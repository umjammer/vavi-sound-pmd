/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package pmd.common;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * CommonTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-07-13 nsano initial version <br>
 */
class CommonTest {

    @Test
    @DisplayName("plain shift_jis")
    void test() {
        // コナミ矩形波倶楽部
        byte[] b = {
                (byte) 0x83, 0x52, (byte) 0x83, 0x69, (byte) 0x83, 0x7e,
                (byte) 0x8b, (byte) 0xe9, (byte) 0x8c, 0x60, (byte) 0x94, 0x67,
                (byte) 0x8b, (byte) 0xe4, (byte) 0x8a, 0x79, (byte) 0x95, (byte) 0x94
        };
        assertEquals("コナミ矩形波倶楽部", Common.decode(b));
    }

    @Test
    @DisplayName("half width characters written as double byte JIS row 9, as PMD titles do")
    void test2() {
        // 「Twin」 - the letters are 0x85xx, which ms932 alone decodes to U+FFFD
        byte[] b = {
                (byte) 0x81, 0x75,
                (byte) 0x85, 0x73, (byte) 0x85, (byte) 0x97, (byte) 0x85, (byte) 0x89, (byte) 0x85, (byte) 0x8e,
                (byte) 0x81, 0x76
        };
        assertEquals("「Twin」", Common.decode(b));
    }

    @Test
    @DisplayName("half width katakana written as double byte JIS row 10")
    void test3() {
        // ｱｲ
        byte[] b = {(byte) 0x85, (byte) 0xaf, (byte) 0x85, (byte) 0xb0};
        assertEquals("ｱｲ", Common.decode(b));
    }
}
