/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package pmd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

import pmd.driver.X86Register;
import vavi.util.Debug;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-17 nsano initial version <br>
 */
@EnabledIf("localPropertiesExists")
@PropsEntity(url = "file:local.properties")
class TestCase {

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "pmd")
    String file = "src/test/resources/test.m";

    @Property
    String mml = "src/test/resources/test.mml";

    @Property(name = "vavi.test.volume")
    double volume = 0.2;

    @Property
    String pmdDotNet;

    @BeforeAll
    static void setupAll() throws Exception {
        Path tmp = Path.of("tmp");
        if (!Files.exists(tmp)) Files.createDirectory(tmp);
    }

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }

        System.setProperty("pmd.volume", "%4.2f".formatted(volume));

//        System.setProperty("pmd.dir", "");
//        System.setProperty("pmd.opt", "");
Debug.println("volume: " + System.getProperty("pmd.volume"));
    }

    static final ResourceBundle rb = ResourceBundle.getBundle("pmd/message");

    @Test
    @DisplayName("test incSi")
    void test1() throws Exception {
         X86Register r = new X86Register();
         short x;
         r.setAx((short) 0x188);
Debug.printf("%02x, %02x", r.ah & 0xff, r.al & 0xff);
         x = r.getAx();
         assertEquals(392, x);

         r.setSi((short) 1);
         short s2 = r.incSi();
         short s3 = r.getSi();
         assertEquals(1, s2);
         assertEquals(2, s3);
     }

     @Test
     @DisplayName("resource bundle")
     void test2() throws Exception {
Debug.println(rb.getString("E01%02d".formatted(7)));
     }

     @Test
     @DisplayName("play")
     @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
     void test3() throws Exception {
Debug.println(file);
         pmd.player.Program.main(new String[] {file});
     }

    @Test
    @DisplayName("compile & play")
    void test4() throws Exception {
        pmd.console.Program.isTest = true;
Debug.println(mml);
        Path testMML = Path.of("tmp/test_java.mml");
        Path testM = Path.of("tmp/test_java.M");

        Files.copy(Path.of(mml), testMML, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(testM);

        // compile java
        pmd.console.Program.main(new String[] {testMML.toString()});
        assertTrue(Files.exists(testM), "java compile failed");

        // play
        if ("ide".equals(System.getProperty("vavi.test")))
            pmd.player.Program.main(new String[] {testM.toString()});
    }

    @Test
    @DisplayName("PMDDotNet compile & play")
    void test5() throws Exception {
Debug.println(mml);
        Path testMML = Path.of("tmp/test_dotnet.mml");
        Path testM = Path.of("tmp/test_dotnet.M");
        Files.copy(Path.of(mml), testMML, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(testM);
        // compile
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        Process p = pb.command(pmdDotNet, testMML.toString()).start();
        int r = p.waitFor();
        assertEquals(0, r);
        assertTrue(Files.exists(testM), "compile failed");
        // play
        if ("ide".equals(System.getProperty("vavi.test")))
            pmd.player.Program.main(new String[] {testM.toString()});
    }

    @Test
    @DisplayName("compile & compare & play")
    void test6() throws Exception {
        pmd.console.Program.isTest = true;
Debug.println(mml);
        Path testMML = Path.of("tmp/test_java.mml");
        Path testM = Path.of("tmp/test_java.M");
        Path testMML2 = Path.of("tmp/test_dotnet.mml");
        Path testM2 = Path.of("tmp/test_dotnet.M");

        Files.copy(Path.of(mml), testMML, StandardCopyOption.REPLACE_EXISTING);
        Files.copy(Path.of(mml), testMML2, StandardCopyOption.REPLACE_EXISTING);
        Files.deleteIfExists(testM);
        Files.deleteIfExists(testM2);

        // compile c#
Debug.println("compile c# --------");
        ProcessBuilder pb = new ProcessBuilder();
        pb.inheritIO();
        Process p = pb.command(pmdDotNet, testMML2.toString()).start();
        int r = p.waitFor();
        assertEquals(0, r);
        assertTrue(Files.exists(testM2), "c# compile failed");

        // compile java
Debug.println("compile java --------");
        pmd.console.Program.main(new String[] {testMML.toString()});
        assertTrue(Files.exists(testM), "java compile failed");

        // compare
Debug.println("compare --------");
Debug.println("c#  : " + Files.size(testM2));
Debug.println("java: " + Files.size(testM));
        assertEquals(Files.size(testM2), Files.size(testM), "java output is different from the original");

        // play
Debug.println("play --------");
        if ("ide".equals(System.getProperty("vavi.test")))
            pmd.player.Program.main(new String[] {testM.toString()});
    }
}
