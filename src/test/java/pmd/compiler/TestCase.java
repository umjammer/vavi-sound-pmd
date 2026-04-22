
package pmd.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pmd.compilerTestService.PMDCompileTestService;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertTrue;


class TestCase {

//    String mml = "src/test/resources/BRICK.MML";
    String mml = "tmp/PC-98 Eternal Shrine Maiden.mml";

    @Test
    void test1() throws Exception {
        var compiler = new Compiler();
        compiler.init();

        compiler.mcArgs = new String[] {"/v", mml};

        compiler.env = new String[] {
                System.getProperty("pmd.arranger"),
                System.getProperty("pmd.composer"),
                System.getProperty("pmd.user"),
                System.getProperty("pmd.opt")
        };

        var ms = new MemoryStream();
        var fs = new FileStream(mml, FileMode.Open, FileAccess.Read, FileShare.Read);
        var r = compiler.compile(fs, ms, f -> {
Debug.println(f);
            return new FileStream("tmp/" + f, FileMode.Open, FileAccess.Read, FileShare.Read);
        });
        ms.flush();

Debug.println(r);
Debug.println(compiler.getMemo_writeAddress());

        assertTrue(r);
    }

    @Test
    @Disabled("dos tool not exists")
    @DisplayName("Multiple MML compile tests_V available")
    void test2() throws Exception {
        testMain(new String[] {"/v"});
    }

    @Test
    @Disabled
    @DisplayName("Multiple MML compile test_V None")
    void test3() throws Exception {
        testMain(null);
    }

    private static void testMain(String[] options) throws Exception {
        var service = new PMDCompileTestService();

        var mmlFilesDir = getMMLDir();
        assertTrue(service.multiTest(mmlFilesDir, options, getToolDir()));

        var mmlListFile = mmlFilesDir.resolve("MMLFiles.txt");
        if (Files.exists(mmlListFile)) {
            var lines = Files.readAllLines(mmlListFile);

            for (var item : lines) {
                Path path = Path.of(item);
                if (Files.exists(path)) {
                    assertTrue(service.multiTest(path, options, getToolDir()));
                }
            }
        }
    }

    private static Path getToolDir() {
        return Path.of("/opt/homebrew/bin");
    }

    private static Path getMMLDir() {
        return Path.of("src/test/resources");
    }
}

