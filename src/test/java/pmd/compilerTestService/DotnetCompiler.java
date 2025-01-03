package pmd.compilerTestService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.util.compat.Tuple;
import pmd.compiler.Compiler;


public class DotnetCompiler extends DosCompiler {

    public static Tuple<CompileResult, String> Compile(String mmlFilePath, String[] options) {

        var log = new StringBuilder();
//        Log.writeLine = (level, msg) ->log.AppendFormat("[{0,-7}] %d%d", level, msg, Environment.NewLine);

        var fullpath = Path.getFullPath(mmlFilePath);
        var dir0 = Path.getDirectoryName(fullpath);
        var dir = dir0 != null ? dir0 : System.getProperty("user.home");
        var fname = Path.getFileName(fullpath);

        var compiler = new Compiler();
        compiler.init();

        var includePaths = new ArrayList<String>();
        includePaths.add(dir);
        var envpmd = System.getenv("PMD");
        if (envpmd != null && !envpmd.isEmpty()) {
            var envpmds = envpmd.split(";");
            for(var item : envpmds)
            {
                var path = Path.getFullPath(item.trim());
                if (File.exists(path)) {
                    includePaths.add(path);
                }
            }
        }

        Function<String, Stream> fnAppendFileReaderCallback = fname_ -> {
            try {
                if (fname_ != null) {
                    for (var item : includePaths) {
                        var path = Path.combine(item, fname_);
                        if (File.exists(path)) {
                            return new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                        }
                    }
                }
            } catch (Exception e) {
            }
            return null;
        };

        var outputFileName = GetOutputFileName(compiler, mmlFilePath, fnAppendFileReaderCallback);

        try (var fs = new FileStream(fullpath, FileMode.Open, FileAccess.Read, FileShare.Read)) {
            try (var ms = new MemoryStream()) {
                if (options != null) {
                    var tmp = new ArrayList<String>(options.length + 1);
                    tmp.addAll(Arrays.asList(options));
                    tmp.add(fname);
                    compiler.mcArgs = tmp.toArray(String[]::new);
                } else {
                    compiler.mcArgs = new String[] {fname};
                }

                var envs = new ArrayList<String>();

                AddEnv(envs, "ARRANGER");
                AddEnv(envs, "COMPOSER");
                AddEnv(envs, "USER");
                AddEnv(envs, "MCOPT");
                compiler.env = envs.toArray(String[]::new);

                var r = compiler.compile(fs, ms, fnAppendFileReaderCallback);
                ms.flush();

                return new Tuple<>(new CompileResult(r, ms.toArray(), log.toString(), compiler.getMemo_writeAddress()), outputFileName);
            }
        }
    }

    static void AddEnv(List<String> envs, String envname) {
        var env = System.getenv(envname);
        if (env != null && !env.isEmpty()) {
            envs.add(java.lang.String.format("%d=%d", envname, env));
        }
    }

    private static String GetOutputFileName(Compiler compiler, String mmlFilePath, Function<String, Stream> fnAppendFileReaderCallback) {
        try (var sourceMML = new FileStream(mmlFilePath, FileMode.Open, FileAccess.Read, FileShare.Read)) {
            try (var sr = new StreamReader(sourceMML, Charset.forName("cp932"))) {
                var srcText = sr.readToEnd();
                var tags = compiler.GetTags(srcText, fnAppendFileReaderCallback);
                if (tags != null) {
                    for (var item : tags) {
                        // mcは3文字まで判定している為
                        if (item.getItem1().toUpperCase().indexOf("#FI") == 0) {
                            if (item.getItem2().charAt(0) == '.') {
                                return Path.getFileNameWithoutExtension(mmlFilePath) + item.getItem2();
                            } else {
                                return Path.getFileName(item.getItem2());
                            }
                        }
                    }
                }
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
        return Path.getFileNameWithoutExtension(mmlFilePath) + ".M";
    }
}
