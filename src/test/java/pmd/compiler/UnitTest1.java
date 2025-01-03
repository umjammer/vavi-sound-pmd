
package pmd.compiler;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import dotnet4j.io.Directory;
import dotnet4j.io.File;
import dotnet4j.io.Path;
import org.junit.jupiter.api.Test;
import pmd.compilerTestService.PMDCompileTestService;

import static org.junit.jupiter.api.Assertions.assertTrue;


class UnitTest1 {

    private static String otherLangFilename = Path.combine("lang", "PMDDotNETmessage.%d.txt");
    private static String englishFilename = Path.combine("lang", "message.properties");

    @Test
    public void 複数のMMLコンパイルテスト_Vあり() throws Exception {
        TestMain(new String[] {"/v"});
    }

    @Test
    public void 複数のMMLコンパイルテスト_Vなし() throws Exception {
        TestMain(null);
    }

    private void TestMain(String[] options) throws Exception {
        var logdir = GetLogDir();
        while (Directory.exists(logdir)) {
            Thread.sleep(1000);
            logdir = GetLogDir();
        }
        Directory.createDirectory(logdir);
        var logwriter = File.createText(Path.combine(logdir, "log.txt"));
        try (var listener = new TextWriterTraceListener(logwriter.BaseStream)) {

            try (var loggerFactory = LoggerFactory.Create(builder -> {
                builder.AddConsole(configure ->
                {
                    configure.Format = ConsoleLoggerFormat.Systemd;
                });
                builder.AddTraceSource(new SourceSwitch("TraceSourceLog", SourceLevels.Verbose.toString()), listener);
            })) {
            }
        }
        var logger = loggerFactory.CreateLogger < PMDCompileTestService > ();
        var service = new PMDCompileTestService(logger);

        var mmlfilesDir = GetMMLDir();
        assertTrue(service.MultiTest(mmlfilesDir, options, GetToolDir(), logdir));

        var mmllistfile = Path.combine(mmlfilesDir, "MMLFiles.txt");
        if (File.exists(mmllistfile)) {
            var lines = Files.readAllLines(java.nio.file.Path.of(mmllistfile));

            for (var item : lines) {
                if (Directory.exists(item)) {
                    assertTrue(service.MultiTest(item, options, GetToolDir(), logdir));
                }
            }
        }
    }

    private static String GetToolDir() {
        var dir = Path.getDirectoryName(UnitTest1.class.getName());
        if (dir != null) {
            return Path.getFullPath(Path.combine(dir, "../../../../PMDDotNETCompilerTestService/DOSTOOLS"));
        }
        return System.getProperty("user.home");
    }

    private static String GetMMLDir() {
        var dir = Path.getDirectoryName(UnitTest1.class.getName());
        if (dir != null) {
            return Path.getFullPath(Path.combine(dir, "../../../MMLFILES"));
        }
        return System.getProperty("user.home");
    }

    private static String GetLogDir() {
        var dir = Path.getDirectoryName(UnitTest1.class.getName());
        if (dir != null) {
            return Path.getFullPath(Path.combine(dir, String.format("../../../LOGS/%s", DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss").format(Instant.now()))));
        }
        return System.getProperty("user.home");
    }
}

