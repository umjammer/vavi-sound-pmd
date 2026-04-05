package pmd.compiler;

import musicDriverInterface.CompilerInfo;
import musicDriverInterface.MmlDatum.MMLType;


public class Work {

    public final CompilerInfo compilerInfo = new CompilerInfo();
    public final byte[] ppzfile_buf = new byte[128 * 8];

    int si;

    public int getSi() {
        return si;
    }

    int di;

    public int getDi() {
        return di;
    }

    int bp;

    public int getBp() {
        return bp;
    }

    byte al;

    public byte getAl() {
        return al;
    }

    byte ah;

    public byte getAh() {
        return ah;
    }

    int bx;

    public int getBx() {
        return bx;
    }

    int dx;

    public int getDx() {
        return dx;
    }

    public boolean isIDE;
    public MMLType ctype;
    public Object[] cargs;
}
