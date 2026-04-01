
package pmd.compiler;

import musicDriverInterface.LinePos;


public class HsSeg {

    public static final int hs_length = 32;
    public final byte[] hsbuf2 = new byte[2 * 256];
    public final byte[] hsbuf3 = new byte[32 * 256]; // hs_length * 256];
    public int hsbuf_end; // label   byte
    public byte[] currentBuf;

    public LinePos[] hsLp2 = new LinePos[256];
    public LinePos[] hsLp3 = new LinePos[256];
    public LinePos[] currentLp;
}
