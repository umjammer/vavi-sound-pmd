
package pmd.common;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Function;

import musicDriverInterface.MmlDatum;


public class Common {

    public static final Charset charset = Charset.forName("ms932");

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
