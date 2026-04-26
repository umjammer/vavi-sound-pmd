package pmd.compiler;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.MemoryStream;
import pmd.compilerTestService.PMDCompileTestService;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "mmlX")
    String mml = "src/test/resources/test.mml";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("pmd.volume", "%4.2f".formatted(volume));

//        System.setProperty("pmd.dir", "");
//        System.setProperty("pmd.opt", "");
    }

    @Test
    @DisplayName("compile mml")
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
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

        Files.write(Path.of("tmp/java.m"), ms.toArray());

        assertTrue(r);

        assertEquals(
                Files.size(Path.of("src/test/resources/dotnet.m")),
                ms.getLength()
        );
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

