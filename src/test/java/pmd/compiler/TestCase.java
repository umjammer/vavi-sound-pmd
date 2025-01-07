
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
import org.junit.jupiter.api.Test;
import pmd.compilerTestService.PMDCompileTestService;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertTrue;


@Disabled
class TestCase {

    @Test
    void test1() throws Exception {
        var compiler = new Compiler();
        compiler.init();

        compiler.mcArgs = new String[] {"/v", "src/test/resources/BRICK.MML"};

        var envs = new ArrayList<String>();
        envs.add(System.getenv( "ARRANGER"));
        envs.add(System.getenv("COMPOSER"));
        envs.add(System.getenv("USER"));
        envs.add(System.getenv("MCOPT"));
        compiler.env = envs.toArray(String[]::new);

        var ms = new MemoryStream();
        var fs = new FileStream("src/test/resources/BRICK.MML", FileMode.Open, FileAccess.Read, FileShare.Read);
        var r = compiler.compile(fs, ms, f -> {
Debug.println(f);
            return new FileStream("tmp/" + f, FileMode.Open, FileAccess.Read, FileShare.Read);
        });
        ms.flush();

Debug.println(r);
Debug.println(compiler.getMemo_writeAddress());
    }

    @Test
    void 複数のMMLコンパイルテスト_Vあり() throws Exception {
        testMain(new String[] {"/v"});
    }

    @Test
    void 複数のMMLコンパイルテスト_Vなし() throws Exception {
        testMain(null);
    }

    private void testMain(String[] options) throws Exception {
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
        return Path.of("opt/homebrew/Cellar/pmdmini/2.0.0/bin");
    }

    private static Path getMMLDir() {
        return Path.of("src/test/resources");
    }
}

