/*
 * Copyright (c) 2025 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package pmd;

import org.junit.jupiter.api.Test;
import pmd.driver.X86Register;
import vavi.util.Debug;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * TestCase.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-01-17 nsano initial version <br>
 */
class TestCase {

    @Test
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
}
