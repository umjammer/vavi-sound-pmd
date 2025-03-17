
package pmd.common;

import java.nio.charset.Charset;
import java.util.function.Function;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.Stream;
import musicDriverInterface.MmlDatum;


public class Common {

    public static Charset charset = Charset.forName("cp932");

    public static short GetLe16(MmlDatum[] md, int adr) {
        return (short) (md[adr].dat + md[adr + 1].dat * 0x100);
    }

    public static byte[] GetPCMDataFromFile(String fnPcm, Function<String, Stream> appendFileReaderCallback) {
        try {
            try (Stream pd = appendFileReaderCallback.apply(fnPcm)) {
                return ReadAllBytes(pd);
            }
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read binary from a stream in bulk
     */
    public static byte[] ReadAllBytes(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        try (var ms = new MemoryStream()) {
            while (true) {
                var r = stream.read(buf, 0, buf.length);
                if (r < 1) {
                    break;
                }
                ms.write(buf, 0, r);
            }
            return ms.toArray();
        }
    }
}
