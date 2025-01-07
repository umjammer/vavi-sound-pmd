package pmd.compilerTestService;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import pmd.compilerTestService.CompileResult.CompareResult;
import pmd.compilerTestService.CompileResult.CompileStatus;

import static java.lang.System.getLogger;
import static java.util.function.Predicate.not;
import static pmd.compilerTestService.CompileResult.CompareResult.Match;
import static pmd.compilerTestService.CompileResult.CompareResult.Match_NotEqualLength;
import static pmd.compilerTestService.CompileResult.CompareResult.Match_WithoutMemo;


public class PMDCompileTestService {

    private static final Logger logger = getLogger(PMDCompileTestService.class.getName());

    public static class TestResult {

        private final String mmlFilePath;

        public String getMMLFilePath() {
            return mmlFilePath;
        }

        private final CompareResult compareResult;

        public CompareResult getCompareResult() {
            return compareResult;
        }

        private final CompileStatus dotNetResult;

        public CompileStatus getDotNetResult() {
            return dotNetResult;
        }

        private final CompileStatus dosResult;

        public CompileStatus getDosResult() {
            return dosResult;
        }

        private final int dosExitCode;

        public int getDosExitCode() {
            return dosExitCode;
        }

        private final String compiledFilesDir;

        public String getCompiledFilesDir() {
            return compiledFilesDir;
        }

        public TestResult(String mmlFilePath, CompareResult compareResult, CompileStatus dotnetResult, int dosExitCode, CompileStatus dosResult, String compiledFilesDir) {
            this.mmlFilePath = mmlFilePath;
            this.compareResult = compareResult;
            dotNetResult = dotnetResult;
            this.dosResult = dosResult;
            this.dosExitCode = dosExitCode;
            this.compiledFilesDir = compiledFilesDir;
        }

        public boolean isPerfect() {
            return compareResult == Match &&
                            dotNetResult == CompileStatus.Succeeded &&
                            dosResult == CompileStatus.Succeeded;
        }

        public boolean isAllowed() {
            return (compareResult == Match || compareResult == Match_NotEqualLength || compareResult == Match_WithoutMemo) &&
                            dotNetResult == CompileStatus.Succeeded &&
                            dosResult == CompileStatus.Succeeded;
        }

        public boolean isWarning() {
            return (compareResult == Match || compareResult == Match_NotEqualLength || compareResult == Match_WithoutMemo) &&
                            (dotNetResult == CompileStatus.Succeeded || dotNetResult == CompileStatus.Warning &&
                                    (dosResult == CompileStatus.Succeeded || dosResult == CompileStatus.Warning));
        }
    }

    public PMDCompileTestService.TestResult singleTest(String mmlFilePath, String[] options, Path toolDir) {
        logger.log(Level.INFO, "---- Test Start - %s".formatted(mmlFilePath));
        if (options != null && options.length > 0) {
            var tmp = new StringBuilder();
            for (var item : options) {
                tmp.append(" %s".formatted(item));
            }

            logger.log(Level.INFO, "Compile Option:%s".formatted(tmp.toString()));
        }
        try {
            var dotnet = JavaCompiler.compile(mmlFilePath, options);
            logger.log(Level.INFO, ".NET Compile Result: %s".formatted(dotnet.getItem1().status));
            dotnet.getItem1().writeLog(logger);

            var dos = DosCompiler.compile(mmlFilePath, options, dotnet.getItem2(), toolDir);
            logger.log(Level.INFO, "DOS Compile Result: %s".formatted(dos.status));
            dos.writeLog(logger);

            var compareResult = dotnet.getItem1().compare(dos);
            logger.log(Level.INFO, "Compare Result: %s".formatted(compareResult));

            Path compiledFilesDir = Path.of("tmp");
            if (compareResult == CompareResult.Unmatch) {
                var basename = mmlFilePath.substring(0, mmlFilePath.indexOf('.'));
                if (compiledFilesDir != null) {
                    Files.createDirectory(compiledFilesDir);

                    writeFile(compiledFilesDir.resolve("dotnet.m"), dotnet.getItem1().getCompiledBinary());
                    writeFile(compiledFilesDir.resolve("dos.m"), dos.getCompiledBinary());
                }
            }

            return new PMDCompileTestService.TestResult(
                    mmlFilePath,
                    compareResult,
                    dotnet.getItem1().status,
                    dos.getExitCode(),
                    dos.status,
                    compiledFilesDir.toString()
            );
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
            return new TestResult(
                    mmlFilePath,
                    CompareResult.Unspecified,
                    CompileStatus.Exception,
                    0,
                    CompileStatus.Exception,
                    null);
        } finally {
            logger.log(Level.INFO, "---- Test End - %s".formatted(mmlFilePath));
        }
    }

    static void writeFile(Path path, byte[] bin) throws IOException {
        if (bin != null && bin.length > 0) {
            try (var fs = Files.newOutputStream(path)) {
                fs.write(bin, 0, bin.length);
            }
        }
    }

    public boolean multiTest(Path mmlFileDir, String[] options, Path toolDir) {
        try (var mmls = Files.list(mmlFileDir)
                    .filter(not(Files::isDirectory))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".mml"))) {

            var count = new AtomicInteger();
            var allowFiles = new ArrayList<TestResult>();
            var warningFiles = new ArrayList<TestResult>();
            var dosErrorFiles = new ArrayList<TestResult>();
            var errorFiles = new ArrayList<TestResult>();
            mmls.forEach(mml -> {
                var r = singleTest(mml.toString(), options, toolDir);

                if (!r.isPerfect()) {
                    if (r.isAllowed()) {
                        allowFiles.add(r);
                    } else if (r.isWarning()) {
                        warningFiles.add(r);
                    } else if (r.dosResult == CompileStatus.Failed) {
                        dosErrorFiles.add(r);
                    } else {
                        errorFiles.add(r);
                    }
                }
                count.getAndIncrement();
            });
            logger.log(Level.INFO, "Test Files: %d files".formatted(count.get()));
            logger.log(Level.INFO, "Allowed Files: %d files".formatted(allowFiles.size()));
            logger.log(Level.INFO, "Warning Files: %d files".formatted(warningFiles.size()));
            logger.log(Level.INFO, "DOS Compiler Error Files: %d files".formatted(dosErrorFiles.size()));
            logger.log(Level.INFO, "Error Files: %d files".formatted(errorFiles.size()));

            if (!allowFiles.isEmpty()) {
                logger.log(Level.INFO, "Allowed Files List:");
                LogFiles(allowFiles);
            }

            if (!warningFiles.isEmpty()) {
                logger.log(Level.INFO, "Warning Files List:");
                LogFiles(warningFiles);
            }

            if (!dosErrorFiles.isEmpty()) {
                logger.log(Level.INFO, "DOS Compiler Error Files List:");
                for (var item : dosErrorFiles) {
                    logger.log(Level.INFO, "%s, Compare = %s, .NET = %s, DOS = %s (exitcode = %s)".formatted(item.mmlFilePath, item.compareResult, item.dotNetResult, item.dosResult, item.dosExitCode));
                }
            }

            if (!errorFiles.isEmpty()) {
                logger.log(Level.INFO, "Error Files List:");
                LogFiles(errorFiles);
            }

            return errorFiles.isEmpty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void LogFiles(List<TestResult> list) {
        for (var item : list) {
            if (item.compiledFilesDir != null) {
                logger.log(Level.INFO, "%s, Compare = %s, .NET = %s, DOS = %s, Binary = %s".formatted(item.mmlFilePath, item.compareResult, item.dotNetResult, item.dosResult, item.compiledFilesDir));
            } else {
                logger.log(Level.INFO, "%s, Compare = %s, .NET = %s, DOS = %s".formatted(item.mmlFilePath, item.compareResult, item.dotNetResult, item.dosResult));
            }
        }
    }
}
