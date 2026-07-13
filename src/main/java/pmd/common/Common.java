
package pmd.common;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

import musicDriverInterface.MmlDatum;


public class Common {

    public static final Charset charset = Charset.forName("ms932");

    /**
     * Decodes text written by a PC-98.
     * <p>
     * Besides Shift_JIS, the PC-98 also writes a <em>half width</em> character as a double byte
     * code in JIS row 9 or 10: {@code 85 73} is a half width {@code T}, and PMD titles are full of
     * them ({@code 「Twinbee Melodies」FROM} is written that way). No Java charset has a mapping for
     * those rows, so decoding such a title as ms932 turns every one of its letters into U+FFFD.
     * They are converted back to ANK and half width katakana here, before the charset sees them.
     */
    public static String decode(byte[] b) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            int c = b[i] & 0xff;
            int next = i + 1 < b.length ? b[i + 1] & 0xff : 0;
            if (isLead(c) && next != 0) {
                int jis = toJis(c, next);
                int row = jis >> 8;
                if (row == 0x29 || row == 0x2a) {
                    // row 9 is ANK, row 10 is half width katakana
                    int ank = row == 0x29 ? jis & 0xff : (jis & 0xff) | 0x80;
                    sb.append(new String(new byte[] {(byte) ank}, charset));
                    i++;
                    continue;
                }
                sb.append(new String(new byte[] {(byte) c, (byte) next}, charset));
                i++;
                continue;
            }
            sb.append(new String(new byte[] {(byte) c}, charset));
        }
        return sb.toString();
    }

    /** whether {@code c} is a Shift_JIS lead byte */
    private static boolean isLead(int c) {
        return (0x81 <= c && c <= 0x9f) || (0xe0 <= c && c <= 0xef);
    }

    /** Shift_JIS to JIS X 0208. */
    private static int toJis(int s1, int s2) {
        if (s1 >= 0xe0) s1 -= 0x40;
        s1 -= 0x81;
        int jis = s1 << 9;
        if (s2 >= 0x80) s2--;
        if (s2 >= 0x9e) {
            jis |= 0x100 | (s2 - 0x9e);
        } else {
            jis |= s2 - 0x40;
        }
        return (jis + 0x2121) & 0xffff;
    }

    public static short getLe16(MmlDatum[] md, int adr) {
        return (short) (md[adr].dat + md[adr + 1].dat * 0x100);
    }

    public static byte[] getPCMDataFromFile(String fnPcm, Function<String, InputStream> appendFileReaderCallback) {
        try (InputStream pd = appendFileReaderCallback.apply(fnPcm)) {
            return pd != null ? pd.readAllBytes() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
