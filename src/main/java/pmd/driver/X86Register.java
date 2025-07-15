package pmd.driver;

import java.util.Deque;
import java.util.LinkedList;


public class X86Register {

    public PW pw = null;

    public byte al;
    public byte ah;

    public short getAx() {
        return (short) ((ah & 0xff) * 0x100 + (al & 0xff));
    }

    public void setAx(short value) {
        ah = (byte) ((value & 0xff00) >> 8);
        al = (byte) (value & 0xff);
    }

    public void addAx(short value) {
        setAx((short) (getAx() + value));
    }

    public void subAx(short value) {
        setAx((short) (getAx() - value));
    }

    public void incAx() {
        addAx((short) 1);
    }

    public void decAx() {
        subAx((short) 1);
    }

    // >>=
    public void srAx(int value) {
        setAx((short) (getAx() >>> value));
    }

    // |=
    public void orAx(int value) {
        setAx((short) (getAx() | value));
    }

    // &=
    public void andAx(int value) {
        setAx((short) (getAx() & value));
    }

    public byte bl;
    public byte bh;

    public short getBx() {
        return (short) ((bh & 0xff) * 0x100 + (bl & 0xff));
    }

    public void setBx(short value) {
        if (pw != null && pw.checkJumpIndexBX) {
            if ((value & 0xffff) == pw.jumpIndex)
                pw.jumpIndex = -1;
        }

        bh = (byte) ((value & 0xff00) >> 8);
        bl = (byte) (value & 0xff);
    }

    public void addBx(short value) {
        setBx((short) (getBx() + value));
    }

    public void subBx(short value) {
        setBx((short) (getBx() - value));
    }

    public void incBx() {
        addBx((short) 1);
    }

    public void decBx() {
        subBx((short) 1);
    }

    // >>=
    public void srBx(int value) {
        setBx((short) (getBx() >>> value));
    }

    // <<=
    public void slBx(int value) {
        setBx((short) (getBx() << value));
    }

    public void andBx(short value) {
        setBx((short) (getBx() & value));
    }

    public void orBx(short value) {
        setBx((short) (getBx() | value));
    }

    public byte cl;
    public byte ch;

    public short getCx() {
        return (short) ((ch & 0xff) * 0x100 + (cl & 0xff));
    }

    public void setCx(short value) {
        ch = (byte) ((value & 0xff00) >> 8);
        cl = (byte) (value & 0xff);
    }

    public void addCx(short value) {
        setCx((short) (getCx() + value));
    }

    public void subCx(short value) {
        setCx((short) (getCx() - value));
    }

    public void incCx() {
        addCx((short) 1);
    }

    public void decCx() {
        subCx((short) 1);
    }

    public void andCx(short value) {
        setCx((short) (getCx() & value));
    }

    public byte dl;
    public byte dh;

    public short getDx() {
        return (short) ((dh & 0xff) * 0x100 + (dl & 0xff));
    }

    public void setDx(short value) {
        dh = (byte) ((value & 0xff00) >> 8);
        dl = (byte) (value & 0xff);
    }

    public void addDx(short value) {
        setDx((short) (getDx() + value));
    }

    public void subDx(short value) {
        setDx((short) (getDx() - value));
    }

    public void incDx() {
        addDx((short) 1);
    }

    public void decDx() {
        subDx((short) 1);
    }

    // >>=
    public void srDx(int value) {
        setDx((short) (getDx() >>> value));
    }

    short di;

    public short getDi() {
        return di;
    }

    private short _si;

    public short getSi() {
        return _si;
    }

    public void setSi(short value) {
        if (pw != null && pw.checkJumpIndexSI) {
            if ((value & 0xffff) == pw.jumpIndex)
                pw.jumpIndex = -1;
        }
        _si = value;
    }

    public void addSi(short value) {
        setSi((short) (_si + value));
    }

    public void subSi(short value) {
        setSi((short) (_si - value));
    }

    /** get and inc */
    public short incSi() {
        try {
            return _si;
        } finally {
            addSi((short) 1);
        }
    }

    /** get and dec */
    public short decSi() {
        try {
            return _si;
        } finally {
            subSi((short) 1);
        }
    }

    short bp;

    public short getBp() {
        return bp;
    }

    boolean carry;

    public boolean getCarry() {
        return carry;
    }

    boolean sign;

    public boolean getSign() {
        return sign;
    }

    boolean zero;

    public boolean getZero() {
        return zero;
    }

    public final Deque<Short> stack = new LinkedList<>();

    public final Object lockObj = new Object();

    private static final int[] bitMask = new int[] {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};

    public byte rol(byte r, int n) {
        n &= 7;
        byte ans = (byte) (((r & 0xff) << n) | (r & 0xff) >> 8 - n); // & bitMask[n]));
        carry = ((ans & 0x01) != 0);
        return ans;
    }

    public byte ror(byte r, int n) {
        n &= 7;
        byte ans = (byte) (((r & 0xff) << (8 - n)) | (r & 0xff) >> n); // & bitMask[8 - n]));
        carry = ((ans & 0x80) != 0);
        return ans;
    }

    public byte rcl(byte r, int n) {
        n &= 7;
        byte ans = (byte) (((r & 0xff) << n) |
                        ((carry ? 1 : 0) << n) |
                        (n < 2 ? 0 : ((r & 0xff) >> (9 - n)))
        ); // & bitMask[n]));
        carry = ((r & (0x100 >> n)) != 0);
        return ans;
    }

    public byte rcr(byte r, int n) {
        n &= 7;
        byte ans = (byte) ((n < 2 ? 0 : ((r & 0xff) << (9 - n))) |
                ((carry ? 0x100 : 0) >> n) |
                ((r & 0xff) >> n)
        ); // & bitMask[n]));
        carry = ((r & (0x100 >> n)) != 0);
        return ans;
    }
}
