package pmd.console;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import musicDriverInterface.MmlDatum;
import pmd.compiler.Compiler;
import vavi.util.compat.Tuple;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;
import static pmd.common.Common.charset;
import static vavi.util.compat.Util.changeExtension;


/**
 * system properties
 * <li>{@code pmd.dir} ... separated by {@code ;}</li>
 */
public class Program {

    private static final Logger logger = getLogger(Program.class.getName());

    private static final ResourceBundle rb = ResourceBundle.getBundle("pmd/message");

    private String srcFile;
    private String ffFile;
    private String desFile;
    private boolean isXml = false;
    public static boolean isTest = false;

    public static void main(String[] args) {
        Program app = new Program();
        int fnIndex = app.analyzeOption(args);

        if (args == null || args.length - fnIndex < 1) {
            logger.log(Level.ERROR, rb.getString("E0600"));
            return;
        }

        try {

            app.compile(args, fnIndex);

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            if (isTest) throw ex;
        }
    }

    private int compile(String[] args, int argIndex) {
        try {
            // Create a list of arguments for mc
            List<String> lstMcArg = new ArrayList<>(Arrays.asList(args).subList(argIndex, args.length));

            Compiler compiler = new Compiler();
            compiler.init();
            compiler.mcArgs = lstMcArg.toArray(String[]::new);

            compiler.env = new String[] {
                    System.getProperty("pmd.arranger"),
                    System.getProperty("pmd.composer"),
                    System.getProperty("pmd.user"),
                    System.getProperty("pmd.mcopt"),
                    System.getProperty("pmd")
            };

            // Get various file names
            int s = 0;
            for (String arg : compiler.mcArgs) {
                if (arg == null || arg.isEmpty()) continue;
                if (arg.charAt(0) == '-' || arg.charAt(0) == '/') continue;
                if (s == 0) srcFile = arg;
                else if (s == 1) ffFile = arg;
                else if (s == 2) desFile = arg;
                s++;
            }

            if (srcFile == null || srcFile.isEmpty()) {
                logger.log(Level.ERROR, rb.getString("E0601"));
                return 1;
            }

            byte[] ffFileBuf;
            if (ffFile != null && !ffFile.isEmpty() && Files.exists(Path.of(ffFile))) {
                ffFileBuf = Files.readAllBytes(Path.of(ffFile));
                compiler.setFfFileBuf(ffFileBuf);
            }

//#if DEBUG
            compiler.setCompileSwitch("IDE");
//            //compiler.SetCompileSwitch("SkipPoint=R17:C18");
//#endif

            if (!isXml) {
                // The default is the source file name with the extension changed to .M.
                String destFileName = "";
                if (srcFile != null && !srcFile.isEmpty()) {
                    destFileName = changeExtension(srcFile, ".M");
                }

                // Get Filename from Tag
                String srcText;
                try (InputStream sourceMML = Files.newInputStream(Path.of(srcFile));
                    InputStreamReader sr = new InputStreamReader(sourceMML, charset)) {
                    StringBuilder sb = new StringBuilder();
                    int ch;
                    while ((ch = sr.read()) != -1) {
                        sb.append((char) ch);
                    }
                    srcText = sb.toString();
                }
                String outFileName = "";
                Tuple<String, String>[] tags = compiler.getTags(srcText, this::appendFileReaderCallback);
                if (tags != null && tags.length > 0) {
                    for (Tuple<String, String> tag : tags) {
                        logger.log(Level.TRACE, "%s\t: %s".formatted(tag.getItem1(), tag.getItem2()));
                        // Get the output file name
                        if (tag.getItem1().toUpperCase().indexOf("#FI") != 0) continue; // Because mc is judged up to three characters
                        outFileName = tag.getItem2();
                    }
                }

                // If the tag specifies a FileName, that is applied.
                if (outFileName != null && !outFileName.isEmpty()) {
                    if (outFileName.charAt(0) != '.') {
                        // When specifying a file name
                        destFileName = Path.of(srcFile).getParent().resolve(outFileName).toString();
                    } else {
                        // When specifying the extension only
                        destFileName = changeExtension(srcFile, outFileName);
                    }
                }

                // If desFile is specified finally, it takes precedence.
                if (desFile != null) {
                    destFileName = desFile;
                }

                boolean isSuccess = false;
                try (InputStream sourceMML = Files.newInputStream(Path.of(srcFile));
                     ByteArrayOutputStream destCompiledBin = new ByteArrayOutputStream()) {
                    isSuccess = compiler.compile(sourceMML, destCompiledBin, this::appendFileReaderCallback);

                    if (isSuccess) {
                        destCompiledBin.flush();
                        byte[] destbuf = destCompiledBin.toByteArray();
                        Files.write(Path.of(destFileName), destbuf);
                        if (compiler.getOutFFFileBuf() != null) {
                            Path outfn = Path.of(destFileName).getParent().resolve(compiler.getOutFFFileName());
                            Files.write(outfn, compiler.getOutFFFileBuf());
                        }
                    } else return 1;
                }
            } else {

                String destFileName = changeExtension(srcFile, ".xml");
                if (desFile != null) {
                    destFileName = desFile;
                }
                MmlDatum[] dest = null;

                // When using xml, compile in IDE mode
                compiler.setCompileSwitch("IDE");

                try (InputStream sourceMML = Files.newInputStream(Path.of(srcFile))) {
                    dest = compiler.compile(sourceMML, this::appendFileReaderCallback);
                }

                try (OutputStream sw = Files.newOutputStream(Path.of(destFileName))) {
                    Serdes.Util.serialize(sw, dest);
                }
            }

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return 1;
        }
        return 0;
    }

    private InputStream appendFileReaderCallback(String arg) {

        Path fn = Path.of(srcFile).getParent().resolve(arg);

        String[] envPaths = System.getProperty("pmd.dir", "").split(";");
        if (envPaths.length > 0 && envPaths[0] != null) {
            int i = 0;
            while (!Files.exists(fn) && i < envPaths.length) {
                fn = Path.of(envPaths[i++], arg);
            }
        }

        InputStream strm;
        try {
            strm = Files.newInputStream(fn);
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            strm = null;
        }

        return strm;
    }

    private int analyzeOption(String[] args) {
        if (args == null) return 0;
        if (args.length < 1) return 0;

        int i = 0;
        while (args.length > i && args[i] != null && !args[i].isEmpty() && args[i].charAt(0) == '-') {
            String op = args[i].substring(1).toUpperCase();
            if (op.equals("XML")) {
                isXml = true;
            } else {
                break;
            }

            i++;
        }

        return i;
    }
}
