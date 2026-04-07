package pmd.compilerTestService;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;


public class DosCompiler {

    public static CompileResult compile(String mmlFilePath, String[] options, String outputFileName, Path tooldir) {
        var tooldirFull = tooldir.toAbsolutePath();
        var currentDir = System.getProperty("user.home");
        var fullpath = Path.of(mmlFilePath).toAbsolutePath();
        var dir = fullpath.getParent();
        var fname = fullpath.getFileName();

        if (dir != null) {
//                Environment.CurrentDirectory = dir;
        }

        var psi = new ProcessBuilder().command(tooldirFull.resolve("msdos.exe").toString());
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
        psi.command("%s%s %s".formatted(tooldirFull.resolve("pmdmini"), option, fname));

        try {
            var p = psi.start();
            var stdout = p.getInputStream().readAllBytes();
            var stderr = p.getErrorStream().readAllBytes();
            p.waitFor();

            String outputFileName2 = null;
            Path outputFile = Path.of(outputFileName);
            if (Files.exists(outputFile)) {
                outputFileName2 = outputFileName;
            } else {
                // Is there a pattern where only the extension is generated?
                var ext = outputFileName.substring(outputFileName.lastIndexOf('.') + 1);
                if (Files.exists(Path.of(ext))) {
                    outputFileName2 = ext;
                }
            }

            try {
                byte[] compiledBinary = null;
                if (p.exitValue() == 0 && outputFileName2 != null) {
                    Path outputFile2 = Path.of(outputFileName2);
                    if (Files.exists(outputFile2)) {
                        byte[] buffer;
                        try (var fs = Files.newInputStream(outputFile)) {
                            buffer = fs.readAllBytes();
                        }

                        compiledBinary = buffer;
                    }
                }

                var log = Arrays.equals(stdout, stderr) ?
                        stdout :
                        "stdout:%s%sstderr:%s".formatted(System.lineSeparator(), stdout, stderr);

                return new CompileResult(p.exitValue(), compiledBinary, log != null ? log.toString() : "");
            } finally {
                if (outputFileName2 != null) {
                    Path outputFile2 = Path.of(outputFileName2);
                    if (Files.exists(outputFile2)) {
                        Files.delete(outputFile2);
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            throw new dotnet4j.io.IOException(e);
        }
    }
}
