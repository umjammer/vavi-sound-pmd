package pmd.compilerTestService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.util.compat.Tuple;
import pmd.compiler.Compiler;

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

        Function<String, Stream> fnAppendFileReaderCallback = fname_ -> {
            try {
                if (fname_ != null) {
                    for (var item : includePaths) {
                        var path = item.resolve(fname_);
                        if (Files.exists(path)) {
                            return new FileStream(path.toString(), FileMode.Open, FileAccess.Read, FileShare.Read);
                        }
                    }
                }
            } catch (Exception e) {
            }
            return null;
        };

        var outputFileName = getOutputFileName(compiler, mmlFilePath, fnAppendFileReaderCallback);

        try (var fs = new FileStream(fullpath.toString(), FileMode.Open, FileAccess.Read, FileShare.Read)) {
            try (var ms = new MemoryStream()) {
                if (options != null) {
                    var tmp = new ArrayList<String>(options.length + 1);
                    tmp.addAll(Arrays.asList(options));
                    tmp.add(fname.toString());
                    compiler.mcArgs = tmp.toArray(String[]::new);
                } else {
                    compiler.mcArgs = new String[] {fname.toString()};
                }

                var envs = new ArrayList<String>();

                addEnv(envs, "ARRANGER");
                addEnv(envs, "COMPOSER");
                addEnv(envs, "USER");
                addEnv(envs, "MCOPT");
                compiler.env = envs.toArray(String[]::new);

                var r = compiler.compile(fs, ms, fnAppendFileReaderCallback);
                ms.flush();

                return new Tuple<>(new CompileResult(r, ms.toArray(), log.toString(), compiler.getMemo_writeAddress()), outputFileName);
            }
        }
    }

    static void addEnv(List<String> envs, String envname) {
        var env = System.getenv(envname);
        if (env != null && !env.isEmpty()) {
            envs.add(java.lang.String.format("%s=%s", envname, env));
        }
    }

    private static String getOutputFileName(Compiler compiler, String mmlFilePath, Function<String, Stream> fnAppendFileReaderCallback) {
        try (var sourceMML = new FileStream(mmlFilePath, FileMode.Open, FileAccess.Read, FileShare.Read)) {
            try (var sr = new StreamReader(sourceMML, charset)) {
                var srcText = sr.readToEnd();
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
                throw new dotnet4j.io.IOException(e);
            }
        }
        return mmlFilePath.substring(0, mmlFilePath.indexOf('.')) + ".M";
    }
}
