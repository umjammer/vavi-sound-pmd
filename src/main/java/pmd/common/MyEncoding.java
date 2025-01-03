
package pmd.common;

import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicReference;


public class MyEncoding implements iEncoding {

    private static AtomicReference<MyEncoding> defaultEncoding;
    private Charset sjis;

    static {
        defaultEncoding = new AtomicReference<>(new MyEncoding());
    }

    public MyEncoding() {
        sjis = Charset.forName("shift_jis");
    }

    public static iEncoding Default() {
        return defaultEncoding.get();
    }

    @Override
    public byte[] getSjisArrayFromString(String utfString) {
        return utfString.getBytes(sjis);
    }

    @Override
    public String getStringFromSjisArray(byte[] sjisArray) {
        return new String(sjisArray, sjis);
    }

    @Override
    public String getStringFromSjisArray(byte[] sjisArray, int index, int count) {
        return new String(sjisArray, index, count, sjis);
    }

    @Override
    public String getStringFromUtfArray(byte[] utfArray) {
        return new String(utfArray);
    }

    @Override
    public byte[] getUtfArrayFromString(String utfString) {
        return utfString.getBytes();
    }
}
