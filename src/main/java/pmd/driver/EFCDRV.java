package pmd.driver;

import java.util.function.Function;

import musicDriverInterface.ChipDatum;
import musicDriverInterface.MmlDatum;


public class EFCDRV {

    private final PMD pmd;
    private final PW pw;
    private final X86Register r;
    private final Function<ChipDatum, Integer> ppsdrv;

    public EFCDRV(PMD pmd, PW pw, X86Register r, Function<ChipDatum, Integer> ppsdrv) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.ppsdrv = ppsdrv;
    }

    public void effgo() {
        if (pw.ppsdrv_flag != 0) { //break effgo2;
            r.al |= (byte) 0x80;
            r.zero = pw.last_shot_data == r.al;
            pw.last_shot_data = r.al;
            if (r.zero) { // break effgo2;
                r.stack.push(r.getAx());
                r.ah = 0;
                ChipDatum cd = new ChipDatum(0x02, 0, 0);
                ppsdrv.apply(cd); // .Stop();
                r.setAx(r.stack.pop());
            }
        }
//effgo2:
        pw.hosei_flag = 3;
        eff_main();
    }

    public void eff_on2() {
        pw.hosei_flag = 1;
        eff_main();
    }

    public void eff_on() {
        pw.hosei_flag = 0;
        eff_main();
    }

    private void eff_main() {
        //r.ds = r.cs;

        if (pw.effflag != 0) { // break eg_00;
            return; // No sound effects mode
        }
//eg_00:
        if (pw.ppsdrv_flag != 0) { // break eg_nonppsdrv;
            r.al |= r.al;
            if ((r.al & 0x80) != 0) { // break eg_nonppsdrv;

                // ppsdrv
                if ((pw.effon & 0xff) >= 2) return; // break effret; // ; Do not vocalize during normal sound effect playback

                r.setBx((short) pw.part9); // PSG 3ch
                pw.partWk[r.getBx() & 0xffff].partmask |= 2; // Part Mask
                pw.effon = 1; // Priority 1 (ppsdrv)
                pw.psgefcnum = r.al; // Tone number setting (80H~)

                r.setBx((short) 15);
                r.ah = pw.hosei_flag;
                r.ah = r.ror(r.ah, 1);
                if (r.carry) { // break not_tone_hosei;
                    r.setBx(pw.partWk[r.di & 0xffff].detune);
                    r.bh = r.bl; // BH = Lower 8 bits of Detune
                    r.bl = 15;
                }
//not_tone_hosei:
                r.ah = r.ror(r.ah, 1);
                if (r.carry) { // break not_volume_hosei;
                    r.ah = pw.partWk[r.di & 0xffff].volume;
                    if ((r.ah & 0xff) < 15) { // break fade_hosei;
                        r.bl = r.ah; // BL = volume value(0 to 15)
                    }
//fade_hosei:
                    r.ah = pw.fadeout_volume;
                    if (r.ah != 0) { // break not_volume_hosei;
                        r.stack.push(r.getAx());
                        r.al = r.bl;
                        r.ah = (byte) -r.ah;
                        r.setAx((short) ((r.al & 0xff) * (r.ah & 0xff)));
                        r.bl = r.ah;
                        r.setAx(r.stack.pop());
                    }
                }
//not_volume_hosei:
                if (r.bl != 0) { // break ppsdrm_ret;
                    r.bl ^= 0b0000_1111; // volume
                    r.ah = 1; // command
                    r.al &= 0x7f; // num?

                    ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
                    cd.additionalData = pw.cmd;
                    pmd.WriteOPNARegister.accept(cd);
                    if (pw.cmd != null && pw.cmd.args != null && pw.cmd.args.size() > 2 && pw.cmd.args.get(2) instanceof MmlDatum[]) {
                        for (MmlDatum md : (MmlDatum[]) pw.cmd.args.get(2)) {
                            pmd.execIDESpecialCommand(md);
                        }
                    }

                    cd = new ChipDatum(0x01, ((r.al & 0xff) << 8) | (r.bh & 0xff), r.bl & 0xff);
                    ppsdrv.apply(cd); // .Play(r.al, r.bh, r.bl); // ppsdrv keyon
                }
//ppsdrm_ret:
                return;
            }
        }
        // TimerA
//eg_nonppsdrv:
        pw.psgefcnum = r.al;
        r.ah = 0;
        r.setBx(r.getAx());
        //r.bx += r.bx;
        //r.bx = r.ax;
        //r.bx += 0; // offset efftbl

        r.al = pw.effon;
        if ((r.al & 0xff) > pw.efftbl.get(r.getBx() & 0xffff).getItem1()) // cmp al,[bx]; Priority
            return; // break eg_ret;

        if (pw.ppsdrv_flag != 0) { // break eok_nonppsdrv;
            r.ah = 0;
            ChipDatum cd = new ChipDatum(0x02, 0, 0);
            ppsdrv.apply(cd); // .Stop(); // ppsdrv Forced keyoff
        }
//eok_nonppsdrv:

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = pw.cmd;
        pmd.WriteOPNARegister.accept(cd);
        if (pw.cmd != null && pw.cmd.args != null && pw.cmd.args.size() > 2 && pw.cmd.args.get(2) instanceof MmlDatum[]) {
            for (MmlDatum md : (MmlDatum[]) pw.cmd.args.get(2)) {
                pmd.execIDESpecialCommand(md);
            }
        }

        r.setSi((short) 0); // pw.efftbl[r.bx].getItem2();
        r.setSi((short) ((r.getSi() & 0xffff) + 0)); // offset efftbl
        pw.crtEfcDat = pw.efftbl.get(r.getBx() & 0xffff).getItem2();
        r.al = (byte) (int) pw.efftbl.get(r.getBx() & 0xffff).getItem1(); // AL = Priority
        r.stack.push(r.getAx());
        r.setBx((short) pw.part9); // PSG 3ch
        pw.partWk[r.getBx() & 0xffff].partmask |= 2; // Part Mask
        efffor(); // Pronounce the first sound
        r.setAx(r.stack.pop());
        pw.effon = r.al; // Set Priority (start of pronunciation)
//eg_ret:
    }

    //
    // Main sound effects
    // from VRTC
    //

    public void effplay() {
        r.dl = pw.effcnt;
        pw.effcnt--;
        if (pw.effcnt != 0) {
            effsweep();
            return;
        }

        r.setSi(pw.effadr);
        efffor();
    }

    private void efffor() {
        r.al = (byte) (pw.crtEfcDat[r.incSi() & 0xffff].dat & 0xff);
        if (r.al == (byte) 0xff) { // -1
            effend();
            return;
        }

        pw.effcnt = r.al; // Count Number

        r.dh = 4; // Frequency Register
        //pushf
        //cli
        efsnd(); // Frequency Set
        r.cl = r.dl;
        efsnd(); // Frequency Set
        //popf
        r.ch = r.dl;
        pw.eswthz = r.getCx();
        r.dl = (byte) (pw.crtEfcDat[r.getSi() & 0xffff].dat & 0xff);
        pw.eswnhz = r.dl;
        r.dh = 6;
        efsnd(); // noise
        pw.psnoi_last = r.dl;

        r.al = (byte) (pw.crtEfcDat[r.incSi() & 0xffff].dat & 0xff); // data
        r.dl = r.al;
        r.dl = r.rol(r.dl, 1);
        r.dl = r.rol(r.dl, 1);
        r.dl &= 0b0010_0100;

        //pushf
        //cli
        pmd.get07();
        r.al &= (byte) 0b1101_1011;
        r.dl |= r.al;
        pmd.opnset44(); // MIX CONTROLL...
        //popf

        r.dh = 10;
        efsnd(); // volume
        efsnd(); // Envelope Frequency
        efsnd();
        efsnd(); // Envelope Pattern

        r.al = (byte) (pw.crtEfcDat[r.incSi() & 0xffff].dat & 0xff);

        r.setAx((short) (r.al & 0xff)); //    cbw
        pw.eswtst = r.getAx(); // Sweep increment (TONE)
        r.al = (byte) (pw.crtEfcDat[r.incSi() & 0xffff].dat & 0xff);
        pw.eswnst = r.al; // Sweep Increment (NOISE)
        r.al &= 15;
        pw.eswnct = r.al; // Sweep Count (NOISE)
        pw.effadr = r.getSi();
//effret:
    }

    private void efsnd() {
        r.al = (byte) (pw.crtEfcDat[r.incSi() & 0xffff].dat & 0xff);
        r.dl = r.al;
        pmd.opnset44();
        r.dh++;
    }

    public void effoff() {
        //r.dx = r.cs;
        //r.ds = r.dx;
        effend();
    }

    public void effend() {
        if (pw.ppsdrv_flag != 0) { // break ee_nonppsdrv;
            r.ah = 0;
            ChipDatum cd = new ChipDatum(0x02, 0, 0);
            ppsdrv.apply(cd); // .Stop(); // ppsdrv keyoff
        }
//ee_nonppsdrv:
        r.setDx((short) 0xa00);
        pmd.opnset44(); // volume min
        r.dh = 7;
        //pushf
        //cli
        pmd.get07();
        r.dl = r.al; // NOISE CUT
        r.dl &= (byte) 0b1101_1011;
        r.dl |= 0b0010_0100;
        pmd.opnset44();
        //popf
        pw.effon = 0;
        pw.psgefcnum = (byte) 0xff; // -1
    }

    // Normal processing
    private void effsweep() {
        r.setAx(pw.eswthz); // Sweep Frequency
        r.setAx((short) ((r.getAx() & 0xffff) + (pw.eswtst & 0xffff)));
        pw.eswthz = r.getAx(); // Sweep Frequency
        r.dh = 4; // REG
        r.dl = r.al; // DATA
        //pushf
        //cli
        pmd.opnset44();
        r.dh++;
        r.dl = r.ah;
        pmd.opnset44();
        pmd.get07();
        r.dl = r.al;
        r.dh = 7;
        pmd.opnset44();
        //popf
        r.dl = pw.eswnst;
        if (r.dl == 0) return; // break effret; // No noise sweep
        pw.eswnct--;
        if (pw.eswnct != 0) return; // break effret;
        r.al = r.dl;
        r.al &= 15;
        pw.eswnct = r.al;
        r.dl >>>= 1;
        r.dl >>>= 1;
        r.dl >>>= 1;
        r.dl >>>= 1;
        pw.eswnhz += r.dl;
        r.dl = pw.eswnhz;
        r.dh = 6;
        pmd.opnset44();
        pw.psnoi_last = r.dl;
    }
}
