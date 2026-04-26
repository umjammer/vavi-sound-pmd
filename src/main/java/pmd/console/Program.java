package pmd.console;

import java.io.OutputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import dotnet4j.io.BufferedStream;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.util.compat.Tuple;
import musicDriverInterface.MmlDatum;
import org.apache.tools.ant.types.Environment;
import pmd.compiler.Compiler;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;
import static pmd.common.Common.charset;


/**
 * system properties
 * <li>{@code pmd.dir} ... separated by {@code ;}</li>
 */
public class Program {

    private static final Logger logger = getLogger(Program.class.getName());

    private static final ResourceBundle rb = ResourceBundle.getBundle("pmd/message");

    private static String srcFile;
    private static String ffFile;
    private static String desFile;
    private static boolean isXml = false;
    public static boolean isTest = false;

    public static void main(String[] args) {
        int fnIndex = AnalyzeOption(args);

        if (args == null || args.length - fnIndex < 1) {
            logger.log(Level.ERROR, rb.getString("E0600"));
            return;
        }

        try {

            compile(args, fnIndex);

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            if (isTest) throw ex;
        }
    }

    private static int compile(String[] args, int argIndex) {
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

            byte[] ffFileBuf = null;
            if (ffFile != null && !ffFile.isEmpty() && File.exists(ffFile)) {
                ffFileBuf = File.readAllBytes(ffFile);
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
                    destFileName = Path.combine(Path.getDirectoryName(Path.getFullPath(srcFile)), "%s.M".formatted(Path.getFileNameWithoutExtension(srcFile))).replace('\\', java.io.File.separatorChar);
                }

                // Get Filename from Tag
                String srcText;
                try (FileStream sourceMML = new FileStream(srcFile, FileMode.Open, FileAccess.Read, FileShare.Read)) {
                    try (StreamReader sr = new StreamReader(sourceMML, charset)) {
                        StringBuilder sb = new StringBuilder();
                        int ch;
                        while ((ch = sr.read()) != -1) {
                            sb.append((char) ch);
                        }
                        srcText = sb.toString();
                    }
                }
                String outFileName = "";
                Tuple<String, String>[] tags = compiler.getTags(srcText, Program::appendFileReaderCallback);
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
                        destFileName = Path.combine(Path.getDirectoryName(Path.getFullPath(srcFile)), outFileName);
                    } else {
                        // When specifying the extension only
                        destFileName = Path.combine(
                                Path.getDirectoryName(Path.getFullPath(srcFile)), "%s%s".formatted(
                                        Path.getFileNameWithoutExtension(srcFile), outFileName));
                    }
                }

                // If desFile is specified finally, it takes precedence.
                if (desFile != null) {
                    destFileName = desFile;
                }

                boolean isSuccess = false;
                try (FileStream sourceMML = new FileStream(srcFile, FileMode.Open, FileAccess.Read, FileShare.Read)) {
                    //try (FileStream destCompiledBin = new FileStream(destFileName, FileMode.Create, FileAccess.Write))
                    try (MemoryStream destCompiledBin = new MemoryStream()) {
                        try (Stream bufferedDestStream = new BufferedStream(destCompiledBin)) {
                            isSuccess = compiler.compile(sourceMML, bufferedDestStream, Program::appendFileReaderCallback);

                            if (isSuccess) {
                                bufferedDestStream.flush();
                                byte[] destbuf = destCompiledBin.toArray();
                                File.writeAllBytes(destFileName, destbuf);
                                if (compiler.getOutFFFileBuf() != null) {
                                    String outfn = Path.combine(Path.getDirectoryName(destFileName), compiler.getOutFFFileName());
                                    File.writeAllBytes(outfn, compiler.getOutFFFileBuf());
                                }
                            } else return 1;
                        }
                    }
                }
            } else {

                String destFileName = Path.combine(Path.getDirectoryName(Path.getFullPath(srcFile)), "%s.xml".formatted(Path.getFileNameWithoutExtension(srcFile)));
                if (desFile != null) {
                    destFileName = desFile;
                }
                MmlDatum[] dest = null;

                // When using xml, compile in IDE mode
                compiler.setCompileSwitch("IDE");

                try (FileStream sourceMML = new FileStream(srcFile, FileMode.Open, FileAccess.Read, FileShare.Read)) {
                    dest = compiler.compile(sourceMML, Program::appendFileReaderCallback);
                }

                try (OutputStream sw = Files.newOutputStream(java.nio.file.Path.of(destFileName))) {
                    Serdes.Util.serialize(sw, dest);
                }
            }

        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
            return 1;
        } finally {
        }
        return 0;
    }

    private static Stream appendFileReaderCallback(String arg) {

        String fn;
        fn = Path.combine(Path.getDirectoryName(srcFile), arg);

        String[] envPaths = System.getProperty("pmd.dir", "").split(";");
        if (envPaths.length > 0 && envPaths[0] != null) {
            int i = 0;
            while (!File.exists(fn) && i < envPaths.length) {
                fn = Path.combine(envPaths[i++], arg);
            }
        }

        FileStream strm;
        try {
            strm = new FileStream(fn, FileMode.Open, FileAccess.Read, FileShare.Read);
        } catch (IOException e) {
            strm = null;
        }

        return strm;
    }

    private static int AnalyzeOption(String[] args) {
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
