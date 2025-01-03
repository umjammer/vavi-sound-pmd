package pmd.driver;

import java.util.Stack;


public class X86Register {

    public PW pw = null;

    public byte al;
    public byte ah;

    public short getAx() {
        return (short) (ah * 0x100 + al);
    }

    public void setAx(short value) {
        ah = (byte) (value >> 8);
        al = (byte) value;
    }

    public byte bl;
    public byte bh;

    public short getBx() {
        return (short) (bh * 0x100 + bl);
    }

    public void setBx(short value) {
        if (pw != null && pw.checkJumpIndexBX) {
            if (value == pw.jumpIndex)
                pw.jumpIndex = -1;
        }

        bh = (byte) (value >> 8);
        bl = (byte) value;
    }

    public byte cl;
    public byte ch;

    public short getCx() {
        return (short) (ch * 0x100 + cl);
    }

    public void setCx(short value) {
        ch = (byte) (value >> 8);
        cl = (byte) value;
    }

    public byte dl;
    public byte dh;

    public short getDx() {
        return (short) (dh * 0x100 + dl);
    }

    public void setDx(short value) {
        dh = (byte) (value >> 8);
        dl = (byte) value;
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
            if (value == pw.jumpIndex)
                pw.jumpIndex = -1;
        }
        _si = value;
    }

    public short incSi() {
        setSi((short) (_si + 1));
        return _si;
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

    public Stack<Short> stack = new Stack<>();

    public Object lockobj = new Object();

    private int[] bitMask = new int[] {0x00, 0x01, 0x03, 0x07, 0x0f, 0x1f, 0x3f, 0x7f, 0xff};

    public byte rol(byte r, int n) {
        n &= 7;
        byte ans = (byte) ((r << n) | ((r >> (8 - n)))); // & bitMask[n]));
        carry = ((ans & 0x01) != 0);
        return ans;
    }

    public byte ror(byte r, int n) {
        n &= 7;
        byte ans = (byte) ((r << (8 - n)) | ((r >> n))); // & bitMask[8 - n]));
        carry = ((ans & 0x80) != 0);
        return ans;
    }

    public byte rcl(byte r, int n) {
        n &= 7;
        byte ans = (byte) (
                (r << n)
                        | ((carry ? 1 : 0) << n)
                        | (n < 2 ? 0 : (r >> (9 - n)))
        ); // & bitMask[n]));
        carry = ((r & (0x100 >> n)) != 0);
        return ans;
    }

    public byte rcr(byte r, int n) {
        n &= 7;
        byte ans = (byte) (
                (n < 2 ? 0 : (r << (9 - n)))
                        | ((carry ? 0x100 : 0) >> n)
                        | (r >> n)
        ); // & bitMask[n]));
        carry = ((r & (0x100 >> n)) != 0);
        return ans;
    }
}
