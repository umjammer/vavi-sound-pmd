package pmd.compilerTestService;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import dotnet4j.io.Directory;
import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.Path;
import pmd.compilerTestService.CompileResult.CompareResult;
import pmd.compilerTestService.CompileResult.CompileStatus;

import static java.lang.System.getLogger;


public class PMDCompileTestService {

    private static final Logger logger = getLogger(PMDCompileTestService.class.getName());

    public static class TestResult {

        private String MMLFilePath;

        public String getMMLFilePath() {
            return MMLFilePath;
        }

        private CompareResult CompareResult;

        public CompareResult getCompareResult() {
            return CompareResult;
        }

        private CompileStatus DotNetResult;

        public CompileStatus getDotNetResult() {
            return DotNetResult;
        }

        private CompileStatus DosResult;

        public CompileStatus getDosResult() {
            return DosResult;
        }

        private int DosExitCode;

        public int getDosExitCode() {
            return DosExitCode;
        }

        private String CompiledFilesDir;

        public String getCompiledFilesDir() {
            return CompiledFilesDir;
        }

        public TestResult(String mmlFilePath, CompareResult compareResult, CompileStatus dotnetResult, int dosExitCode, CompileStatus dosResult, String compiledFilesdir) {
            MMLFilePath = mmlFilePath;
            CompareResult = compareResult;
            DotNetResult = dotnetResult;
            DosResult = dosResult;
            DosExitCode = dosExitCode;
            CompiledFilesDir = compiledFilesdir;
        }

        public boolean IsPerfect() {
            return
                    CompareResult == CompareResult.Match &&
                            DotNetResult == CompileStatus.Succeeded &&
                            DosResult == CompileStatus.Succeeded;
        }

        public boolean IsAllowed() {
            return
                    (CompareResult == CompareResult.Match || CompareResult == CompareResult.Match_NotEqualLength || CompareResult == CompareResult.Match_WithoutMemo) &&
                            DotNetResult == CompileStatus.Succeeded &&
                            DosResult == CompileStatus.Succeeded;
        }

        public boolean IsWarning() {
            return
                    (CompareResult == CompareResult.Match || CompareResult == CompareResult.Match_NotEqualLength || CompareResult == CompareResult.Match_WithoutMemo) &&
                            (DotNetResult == CompileStatus.Succeeded || DotNetResult == CompileStatus.Warning &&
                                    (DosResult == CompileStatus.Succeeded || DosResult == CompileStatus.Warning));
        }
    }

    public PMDCompileTestService.TestResult SingleTest(String mmlFilePath, String[] options, String tooldir, String logdir) {
        logger.log(Level.INFO, "---- Test Start - %d", mmlFilePath);
        if (options != null && options.length > 0) {
            var tmp = new StringBuilder();
            for (var item : options) {
                tmp.append(" %s".formatted(item));
            }

            logger.log(Level.INFO, "Compile Option:%d", tmp.toString());
        }
        try {
            var dotnet = DotnetCompiler.Compile(mmlFilePath, options);
            logger.log(Level.INFO, ".NET Compile Result: %d", dotnet.getItem1().Status);
            dotnet.getItem1().WriteLog(logger);

            var dos = DosCompiler.Compile(mmlFilePath, options, dotnet.getItem2(), tooldir);
            logger.log(Level.INFO, "DOS Compile Result: %d", dos.Status);
            dos.WriteLog(logger);

            var compareResult = dotnet.getItem1().Compare(dos);
            logger.log(Level.INFO, "Compare Result: %d", Optional.ofNullable(compareResult));

            String compiledFilesDir = null;
            if (logdir != null && compareResult == CompareResult.Unmatch) {
                var basename = Path.getFileNameWithoutExtension(mmlFilePath);
                var dir = Path.combine(logdir, basename);
                for (int i = 2; i < 100; i++) {
                    if (!File.exists(dir)) {
                        compiledFilesDir = dir;
                        break;
                    }
                    dir = Path.combine(logdir, String.format("%d%d", basename, i));
                }
                if (compiledFilesDir != null) {
                    Directory.createDirectory(compiledFilesDir);

                    WriteFile(Path.combine(compiledFilesDir, "dotnet.m"), dotnet.getItem1().getCompiledBinary());
                    WriteFile(Path.combine(compiledFilesDir, "dos.m"), dos.getCompiledBinary());
                }
            }

            return new PMDCompileTestService.TestResult(
                    mmlFilePath,
                    compareResult,
                    dotnet.getItem1().Status,
                    dos.getExitCode(),
                    dos.Status,
                    compiledFilesDir
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
            logger.log(Level.INFO, "---- Test End - %d", mmlFilePath);
        }
    }

    static void WriteFile(String path, byte[] bin) {
        if (bin != null && bin.length > 0) {
            try (var fs = new FileStream(path, FileMode.Create, FileAccess.ReadWrite, FileShare.ReadWrite)) {
                fs.write(bin, 0, bin.length);
            }
        }
    }

    public boolean MultiTest(String mmlFileDir, String[] options, String tooldir, String logdir) {
        try (var mmls = Files.list(java.nio.file.Path.of(mmlFileDir))
                    .filter(Files::isDirectory)
                    .filter(p -> p.getFileName().toString().endsWith(".mml"))) {

            var count = new AtomicInteger();
            var allowfiles = new ArrayList<TestResult>();
            var warningfiles = new ArrayList<TestResult>();
            var doserrorfiles = new ArrayList<TestResult>();
            var errorfiles = new ArrayList<TestResult>();
            mmls.forEach(mml -> {
                var r = SingleTest(mml.toString(), options, tooldir, logdir);

                if (!r.IsPerfect()) {
                    if (r.IsAllowed()) {
                        allowfiles.add(r);
                    } else if (r.IsWarning()) {
                        warningfiles.add(r);
                    } else if (r.DosResult == CompileStatus.Failed) {
                        doserrorfiles.add(r);
                    } else {
                        errorfiles.add(r);
                    }
                }
                count.getAndIncrement();
            });
            logger.log(Level.INFO, "Test Files: %d files", count.get());
            logger.log(Level.INFO, "Allowed Files: %d files", allowfiles.size());
            logger.log(Level.INFO, "Warning Files: %d files", warningfiles.size());
            logger.log(Level.INFO, "DOS Compiler Error Files: %d files", doserrorfiles.size());
            logger.log(Level.INFO, "Error Files: %d files", errorfiles.size());

            if (!allowfiles.isEmpty()) {
                logger.log(Level.INFO, "Allowed Files List:");
                LogFiles(allowfiles);
            }

            if (!warningfiles.isEmpty()) {
                logger.log(Level.INFO, "Warning Files List:");
                LogFiles(warningfiles);
            }

            if (!doserrorfiles.isEmpty()) {
                logger.log(Level.INFO, "DOS Compiler Error Files List:");
                for (var item : doserrorfiles) {
                    logger.log(Level.INFO, "%d, Compare = %d, .NET = %d, DOS = %d (exitcode = %d)", item.MMLFilePath, item.CompareResult, item.DotNetResult, item.DosResult, item.DosExitCode);
                }
            }

            if (!errorfiles.isEmpty()) {
                logger.log(Level.INFO, "Error Files List:");
                LogFiles(errorfiles);
            }

            return errorfiles.isEmpty();
        } catch (java.io.IOException e) {
            throw new IOException(e);
        }
    }

    static void LogFiles(List<TestResult> list) {
        for (var item : list) {
            if (item.CompiledFilesDir != null) {
                logger.log(Level.INFO, "%d, Compare = %d, .NET = %d, DOS = %d, Binary = %d", item.MMLFilePath, item.CompareResult, item.DotNetResult, item.DosResult, item.CompiledFilesDir);
            } else {
                logger.log(Level.INFO, "%d, Compare = %d, .NET = %d, DOS = %d", item.MMLFilePath, item.CompareResult, item.DotNetResult, item.DosResult);
            }
        }
    }
}
