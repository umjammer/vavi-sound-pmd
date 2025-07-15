package pmd.driver;

import java.util.function.Consumer;

import musicDriverInterface.ChipDatum;


public class Pc98 {

    private final Consumer<ChipDatum> writeOPNARegister;
    private final ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
    private byte fm1_reg = 0;
    private byte fm2_reg = 0;
    private final PW pw;

    private final byte[] psgDat = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public Pc98(Consumer<ChipDatum> writeOPNARegister, PW pw) {
        this.writeOPNARegister = writeOPNARegister;
        this.pw = pw;
    }

    public byte inPort(int v) {
        if (v == 0x2) {
            return 0;
        } else if (v == 0xa468) {
            return 0;
        } else if (v == 0x088) { // FM sound source?
            return 0;
        } else if (v == 0x08a) { // FM sound source?
            return 0;
        } else if (v == 0x188) { // Read FM sound source status flag
            return 0;
        } else if (v == 0x18a) { // FM sound data loading
            if ((fm1_reg & 0xff) < 0x10) {
                return psgDat[(fm1_reg & 0xff)];
            }
            return 0;
        } else if (v == 0x18c) { // Read FM sound source status flag(extension)
            return 0;
        } else if (v == 0x18e) { // FM sound data loading(extension)
            return 0;
        }

        throw new UnsupportedOperationException(Integer.toHexString(v));
    }

    public void outPort(short dx, byte al) {
        if (dx == 0x02) {

        } else if (dx == 0x188) {
            fm1_reg = al;
        } else if (dx == 0x18a) {
            cd.port = 0;
            cd.address = fm1_reg;
            cd.data = al & 0xff;
            //cd.additionalData = pw.cmd;

            if ((fm1_reg & 0xff) < 0x10) {
                psgDat[fm1_reg & 0xff] = al;
            }
            writeOPNARegister.accept(cd);
        } else if (dx == 0x18c) {
            fm2_reg = al;
        } else if (dx == 0x18e) {
            cd.port = 1;
            cd.address = fm2_reg;
            cd.data = al & 0xff;
            //cd.additionalData = pw.cmd;
            writeOPNARegister.accept(cd);
        }
    }

    public boolean getGraphKey() {
        // TODO Not implemented
        return false;
    }
}
