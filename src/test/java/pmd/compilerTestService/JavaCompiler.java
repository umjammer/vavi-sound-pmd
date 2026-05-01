package pmd.compilerTestService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Function;

import pmd.compiler.Compiler;
import vavi.util.compat.Tuple;

import static pmd.common.Common.charset;


public class JavaCompiler extends DosCompiler {

    public static Tuple<CompileResult, String> compile(String mmlFilePath, String[] options) {

        var log = new StringBuilder();
//logger.log(Level.INFO, "[{0,-7}] %d%d", level, msg, Environment.NewLine);

        var fullpath = Path.of(mmlFilePath).toAbsolutePath();
        var dir0 = fullpath.getParent();
        var dir = dir0 != null ? dir0 : Path.of(System.getProperty("user.home"));
        var fname = fullpath.getFileName();

        var compiler = new Compiler();
        compiler.init();

        var includePaths = new ArrayList<Path>();
        includePaths.add(dir);
        var envpmd = System.getenv("PMD");
        if (envpmd != null && !envpmd.isEmpty()) {
            var envpmds = envpmd.split(";");
            for (var item : envpmds) {
                var path = Path.of(item.trim()).toAbsolutePath();
                if (Files.exists(path)) {
                    includePaths.add(path);
                }
            }
        }

        Function<String, InputStream> fnAppendFileReaderCallback = fname_ -> {
            try {
                if (fname_ != null) {
                    for (var item : includePaths) {
                        var path = item.resolve(fname_);
                        if (Files.exists(path)) {
                            return Files.newInputStream(path);
                        }
                    }
                }
            } catch (Exception _) {
            }
            return null;
        };

        var outputFileName = getOutputFileName(compiler, mmlFilePath, fnAppendFileReaderCallback);

        try (var fs = Files.newInputStream(fullpath);
             var ms = new ByteArrayOutputStream()) {
                if (options != null) {
                    var tmp = new ArrayList<String>(options.length + 1);
                    tmp.addAll(Arrays.asList(options));
                    tmp.add(fname.toString());
                    compiler.mcArgs = tmp.toArray(String[]::new);
                } else {
                    compiler.mcArgs = new String[] {fname.toString()};
                }

                compiler.env = new String[] {
                        System.getProperty("pmd.arranger"),
                        System.getProperty("pmd.composer"),
                        System.getProperty("pmd.user"),
                        System.getProperty("pmd.mcopt")
                };

                var r = compiler.compile(fs, ms, fnAppendFileReaderCallback);
                ms.flush();

                return new Tuple<>(new CompileResult(r, ms.toByteArray(), log.toString(), compiler.getMemo_writeAddress()), outputFileName);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String getOutputFileName(Compiler compiler, String mmlFilePath, Function<String, InputStream> fnAppendFileReaderCallback) {
        try (var sourceMML = Files.newInputStream(Path.of(mmlFilePath));
             var sr = new InputStreamReader(sourceMML, charset)) {
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = sr.read()) != -1) {
                sb.append((char) ch);
            }
            var srcText = sb.toString();
            var tags = compiler.getTags(srcText, fnAppendFileReaderCallback);
            if (tags != null) {
                for (var item : tags) {
                    // Because mc is judged up to three characters
                    if (item.getItem1().toUpperCase().indexOf("#FI") == 0) {
                        if (item.getItem2().charAt(0) == '.') {
                            return mmlFilePath.substring(0, mmlFilePath.indexOf('.')) + item.getItem2();
                        } else {
                            return Path.of(item.getItem2()).getFileName().toString();
                        }
                    }
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return mmlFilePath.substring(0, mmlFilePath.indexOf('.')) + ".M";
    }
}
