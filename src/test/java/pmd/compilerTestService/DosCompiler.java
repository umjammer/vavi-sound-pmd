package pmd.compilerTestService;

import java.io.IOException;

import dotnet4j.io.File;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileStream;
import dotnet4j.io.Path;


public class DosCompiler {

    public static CompileResult Compile(String mmlFilePath, String[] options, String outputFileName, String tooldir) {
        var tooldirFull = Path.getFullPath(tooldir);
        var currentDir = System.getProperty("user.home");
        try {
            var fullpath = Path.getFullPath(mmlFilePath);
            var dir = Path.getDirectoryName(fullpath);
            var fname = Path.getFileName(fullpath);

            if (dir != null) {
//                Environment.CurrentDirectory = dir;
            }

            var psi = new ProcessBuilder().command(Path.combine(tooldirFull, "msdos.exe"));
//            RedirectStandardOutput = true;
//            RedirectStandardError = true;
//            UseShellExecute = false;
//            CreateNoWindow = true;

            String option;
            if (options != null && options.length > 0) {
                var tmp = new StringBuilder();
                for (var item : options) {
                    tmp.append(" %s".formatted(item));
                }
                option = tmp.toString();
            } else {
                option = " ";
            }
            psi.command(String.format("%d%d %d", Path.combine(tooldirFull, "MC"), option, fname));


            try {
                var p = psi.start();
                var stdout = p.getInputStream();
                var stderr = p.getErrorStream();
                p.waitFor();

                String outputFileName2 = null;
                if (File.exists(outputFileName)) {
                    outputFileName2 = outputFileName;
                } else {
                    //  拡張子のみファイルが生成されるパターンがある?
                    var ext = Path.getExtension(outputFileName);
                    if (File.exists(ext)) {
                        outputFileName2 = ext;
                    }
                }

                try {
                    byte[] compiledBinary = null;
                    if (p.exitValue() == 0 && outputFileName2 != null && File.exists(outputFileName2)) {
                        byte[] buffer;
                        try (var fs = new FileStream(outputFileName, FileMode.Open)) {
                            buffer = new byte[(int) fs.getLength()];
                            fs.read(buffer, 0, buffer.length);
                        }

                        compiledBinary = buffer;
                    }

                    var log = stdout.equals(stderr) ?
                            stdout :
                            String.format("stdout:%s%sstderr:%s", System.lineSeparator(), stdout, stderr);

                    return new CompileResult(p.exitValue(), compiledBinary, log != null ? log.toString() : "");
                } finally {
                    if (outputFileName2 != null && File.exists(outputFileName2)) {
                        File.delete(outputFileName2);
                    }
                }
            } catch (IOException | InterruptedException e) {
                throw new dotnet4j.io.IOException(e);
            }
        } finally {
//            Environment.CurrentDirectory = currentDir;
        }
    }
}
