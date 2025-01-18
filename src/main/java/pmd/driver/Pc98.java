package pmd.driver;

import java.util.function.Consumer;

import musicDriverInterface.ChipDatum;


public class Pc98 {

    private Consumer<ChipDatum> WriteOPNARegister;
    private ChipDatum cd = new ChipDatum(0, 0, 0);
    private byte fm1_reg = 0;
    private byte fm2_reg = 0;
    private PW pw;

    private byte[] psgDat = new byte[] {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public Pc98(Consumer<ChipDatum> WriteOPNARegister, PW pw) {
        this.WriteOPNARegister = WriteOPNARegister;
        this.pw = pw;
    }

    public byte InPort(int v) {
        if (v == 0x2) {
            return 0;
        } else if (v == 0xa468) {
            return 0;
        } else if (v == 0x088) { // FM音源?
            return 0;
        } else if (v == 0x08a) { // FM音源?
            return 0;
        } else if (v == 0x188) { // FM音源のステータスフラグ読み込み
            return 0;
        } else if (v == 0x18a) { // FM音源のデータ読み込み
            if (fm1_reg < 0x10) {
                return psgDat[fm1_reg];
            }
            return 0;
        } else if (v == 0x18c) { // FM音源のステータスフラグ読み込み(拡張)
            return 0;
        } else if (v == 0x18e) { // FM音源のデータ読み込み(拡張)
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
            cd.data = al;
            //cd.additionalData = pw.cmd;

            if ((fm1_reg & 0xff) < 0x10) {
                psgDat[fm1_reg & 0xff] = al;
            }
            WriteOPNARegister.accept(cd);
        } else if (dx == 0x18c) {
            fm2_reg = al;
        } else if (dx == 0x18e) {
            cd.port = 1;
            cd.address = fm2_reg;
            cd.data = al;
            //cd.additionalData = pw.cmd;
            WriteOPNARegister.accept(cd);
        }
    }

    public boolean GetGraphKey() {
        //TODO: 未実装
        return false;
    }
}
