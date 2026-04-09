package pmd.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import dotnet4j.io.Stream;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.MmlDatum.MMLType;
import pmd.common.Common;
import pmd.common.PmdDosExitException;
import pmd.common.PmdDosExitException.PmdErrorExitException;
import pmd.common.PmdException;

import static java.lang.System.getLogger;


/**
 * Professional Music Driver [P.M.D.]
 * FOR PC98(+ Speak Board)
 *
 * @author M.Kajihara
 * @version 4.8
 * @see "https://gemini.google.com/app/3cbfc288ae580d61"
 */
public class PMD {

    private static final Logger logger = getLogger(PMD.class.getName());

    public final PW pw;
    private final X86Register r;
    private final Pc98 pc98;
    private final PPZDRV ppzdrv;
    private final PCMDRV pcmdrv;
    private final PCMDRV86 pcmdrv86;
    private final EFCDRV efcdrv;
    private final Function<ChipDatum, Integer> ppz8em;
    private final Function<ChipDatum, Integer> ppsdrv;
    private final Function<ChipDatum, Integer> p86em;
    public final PCMLOAD pcmload;
    public final Consumer<ChipDatum> WriteOPNARegister;

    public PMD(
            MmlDatum[] mmlData,
            Consumer<ChipDatum> WriteOPNARegister,
            PW pw,
            Function<String, Stream> appendFileReaderCallback,
            Function<ChipDatum, Integer> ppz8em,
            Function<ChipDatum, Integer> ppsdrv,
            Function<ChipDatum, Integer> p86em) {
        this.pw = pw;
        pw.md = mmlData;
        this.ppz8em = ppz8em;
        this.ppsdrv = ppsdrv;
        this.p86em = p86em;

        //pw.md = new MmlDatum[mmlData.length + 256];
        //System.arraycopy(mmlData, 0, pw.md, 0, mmlData.length);
        //for (int i = 0; i < 256; i++) pw.md[mmlData.length + i] = new MmlDatum(0);

        pw.board = 1; // With audio source
        // Specifying the port number
        pw.fm1_port1 = 0x188; // Register
        pw.fm1_port2 = 0x18a; // data
        pw.fm2_port1 = 0x18c; // Register (extension)
        pw.fm2_port2 = 0x18e; // Data (extended)

        r = new X86Register();
        r.pw = pw;
        pc98 = new Pc98(WriteOPNARegister, pw);
        pcmload = new PCMLOAD(this, pw, r, pc98, ppz8em, ppsdrv, p86em, appendFileReaderCallback);

        ppzdrv = new PPZDRV(this, pw, r, pc98, ppz8em, pcmload.ppzPcmData);
        pcmdrv = new PCMDRV(this, pw, r, pc98, ppzdrv);
        ppzdrv.pcmdrv = pcmdrv;
        ppzdrv.init();
        pcmdrv86 = new PCMDRV86(this, pw, r, pc98, p86em, pcmload.p86PcmData);
        efcdrv = new EFCDRV(this, pw, r, ppsdrv);
        this.WriteOPNARegister = WriteOPNARegister;

        set_int60_jumptable();
        set_n_int60_jumptable();
        setupCmdtbl();
        setupCmdtblp();
        setupCmdtblr();
        setupComtbl0c0h();

        comstart();
    }

    public void rendering() {
        if (pw.getStatus() == 0) return;

        synchronized (pw.systemInterrupt) {
            do {
                pw.timer.timer();
                pw.timeCounter++;
                if ((pw.timer.getStatReg() & 3) != 0) {
                    synchronized (r.lockObj) {
try {
                        fm_Timer_main();
} catch (Exception e) {
 logger.log(Level.ERROR, e.getMessage(), e);
}
                    }
                }
            } while (pw.jumpIndex != -1 && pw.nowLoopCounter < 1);

            //Work.systemInterrupt = false;
        }
    }

    //
    // MS-DOS Call Macros
    //

    private void resident_exit() {
        // Do nothing in particular
    }

    private void resident_cut() {
        // Do nothing in particular
    }

    private void get_psp() {
        // Do nothing in particular
    }

    public void msdos_exit() {
        // Program terminated (error code 0)
        throw new PmdDosExitException("msdos_exit");
    }

    public void error_exit(int qq) {
        // Program terminated (error code qq)
        throw new PmdErrorExitException("error code:%d".formatted(qq));
    }

    public void print_mes(String qq) {
        // Display messages on the console
        String[] a = qq.split("" + (char) 13 + (char) 10);
        for (String s : a)
            System.out.println(s);
    }

    public void print_chr(String qq) {
        // Display characters on the console
        System.out.print(qq);
    }

    public void print_line(String bx) {
        // Display message on console (from bx position to 0)
        System.out.println(bx);
    }

//#if DEBUG
//        private List<Byte> debugBuff = new ArrayList<>();
//        private List<Byte> debug2Buff = new ArrayList<>();
//#endif

    public void debug(int adr) {
//#if DEBUG
//        debugBuff.set(adr, (byte)(debugBuff.get(adr) + 1));
//#endif
    }

    public void debug2(int adr, byte dat) {
//#if DEBUG
//        debug2Buff.set(adr * 2, dat);
//#endif
    }

    public void debug_pcm(int adr) {
//#if DEBUG
//        r.al = pc98.inPort(0xa468); // 86 Sound Source FIFO
//        if ((r.al & 0x10) != 0) {
//            debug(adr);
//        }
//#endif
    }

    public void _wait() {
//        r.cx = (short) pw.wait_clock;
//        do {
//            r.cx--;
//        } while ((r.cx & 0xffff) > 0);
    }

    public void _waitP() {
//        short p = r.cx;
//        r.cx = (short) pw.wait_clock;
//        do {
//            r.cx--;
//        } while ((r.cx & 0xffff) > 0);
//        r.cx = p;
    }

    /** Rhythm continuous output wait */
    public void _rwait() {
//        short p = r.cx;
//        r.cx = (short) (pw.wait_clock * 32);
//        do {
//            r.cx--;
//        } while ((r.cx & 0xffff) > 0);
//        r.cx = p;
    }

    /** For Address out break:ax */
    public void rdychk() {
        r.al = pc98.inPort(r.getDx() & 0xffff); // Useless reading
        do {
            r.al = pc98.inPort(r.getDx() & 0xffff);
        } while ((r.al & 0x80) != 0);
    }

    public void _ppz() {
        // local exit
        if (pw.ppz != 0) {
            if (pw.ppz_call_seg >= 2) {
                //call dword ptr[ppz_call_ofs]
                throw new UnsupportedOperationException();
            }
        }
    }

    public void int60_main(short ax) {
        synchronized (r.lockObj) {
            r.setAx(ax);

            pw.int60flag++;
            if ((r.ah & 0xff) >= int60_max + 1) {
                int60_error();
                return;
            }

            if (pw.board != 0) {
                int60_start();
                return;
            }

            int60_start_not_board();
            int60_exit();
        }
    }

    private void int60_exit() {
        pw.int60flag--;
        pw.int60_result = 0;
    }

    private void int60_error() {
        if (pw.sync != 0) {
            if (r.ah == (byte) 0xff) { // -1
                opnint_sub();
                pw.int60flag--;
                return;
            }
        }
        pw.int60flag--;
        pw.int60_result = (byte) 0xff; // -1
    }

    private void getss() {
        r.setAx(pw.syousetu);
    }

    private void getst() {
        r.ah = pw.status;
        r.al = pw.status2; // KUMA: 0xff : Finished playing?
    }

    private void fout() {
        pw.fadeout_speed = r.al;
    }

    public void resetOption(String[] pmdOption) {
        set_option(pmdOption);
    }

    /**
     * FM sound effects main
     */
    private void fm_efcplay() {
        r.setBx((short) pw.efcdat);
        r.setAx((short) (pw.md[(r.getBx() + 254) & 0xffff].dat + pw.md[(r.getBx() + 254 + 1) & 0xffff].dat * 0x100));
        r.addAx(r.getBx());
        pw.prgdat_adr2 = r.getAx();
        r.di = (short) pw.part_e; // offset part_e

        if (pw.board2 != 0) {
            r.stack.push(pw.fm_port1); // Countermeasure for when TimerA interrupt occurs in sel44 state in mmain
            r.stack.push(pw.fm_port2); // ditto
            r.ah = pw.partb;
            r.al = pw.fmsel;
            r.stack.push(r.getAx());
            pw.partb = 3;
            sel46(); // Even if mmain comes here, it will remain sel46
            fmmain();
            r.setAx(r.stack.pop());
            pw.partb = r.ah;
            pw.fmsel = r.al;
            pw.fm_port2 = r.stack.pop();
            pw.fm_port1 = r.stack.pop();
        } else {
            r.al = pw.partb;
            r.stack.push(r.getAx());
            pw.partb = 3;
            fmmain();
            r.setAx(r.stack.pop());
            pw.partb = r.ah;
        }

        if (pw.md[r.getSi() & 0xffff].dat == 0x80) { // break not_end_fmefc;
            if (pw.partWk[r.di & 0xffff].leng == 0) { // break not_end_fmefc;

                fm_effect_off();
            }
        }
//not_end_fmefc:
    }

    /**
     * Start playing
     */
    private void mstart_f() {
        r.al = pw.timerAFlag;
        r.al |= pw.timerBFlag;
        if (r.al == 0) {
            mstart();
            return;
        }

        pw.music_flag |= 1; // Do not execute during TA/TB processing
        pw.ah_push = (byte) 0xff; // -1
    }

    private void mstart() {
        //
        // Stop playing
        //
        // pushf
        //  cli
        pw.music_flag &= (byte) 0xfe;
        mstop();
        //  popf

        //
        // Preparing for playing
        //
        data_init();
        play_init();

        pw.fadeout_volume = 0;

        if ((pw.board2 | pw.adpcm) != 0) {
            if (pw.ademu != 0) {
                r.setAx((short) 0x1800);
                pw.adpcm_emulate = r.al;
                ChipDatum cd = new ChipDatum(0x18, r.al & 0xff, 0);
                ppz8em.apply(cd); // .SetAdpcmEmu(r.al); // ADPCMEmulate OFF
                r.setBx((short) pw.part10); // offset part10 //  Mask PCM(bit4)
                pw.partWk[r.getBx() & 0xffff].partmask |= 0x10; // Mask PCM(bit4)
            } else {
                //
                // If you use NEC YM2608, you can mask the PCM part.
                //
                if (pw.pcm_gs_flag != 0) {
                    r.setBx((short) pw.part10); // offset part10 //  Mask PCM(bit4)
                    pw.partWk[r.getBx() & 0xffff].partmask |= 4; // Mask PCM(bit2)
                }
                //not_mask_pcm:
            }
        }

        if (pw.ppz != 0) {
            //
            // PPZ8 Initialization
            //
            if (pw.ppz_call_seg != 0) {
                r.setAx((short) 0x1901);
                ChipDatum cd = new ChipDatum(0x19, 0, 0x01);
                ppz8em.apply(cd); // .SetReleaseFlag(0x01); // Do not release resident
                r.ah = 0;
                cd = new ChipDatum(0x00, 0xff, 0xff);
                ppz8em.apply(cd); // .Initialize();
                //r.ah = 6;
                //ppz8em.Reserve();
            }
            //not_init_ppz8:
        }

        //
        // OPN Initialization
        //
        opn_init();

        //
        // Start playing music
        //
        setint();

        pw.play_flag = 1;
        pw.mstart_flag++;
    }

    /**
     * Set the start address and initial value for each part
     */
    private void play_init() {
        r.setSi((short) pw.mmlbuf);

        r.al = (byte) pw.md[(r.getSi() & 0xffff) - 1].dat;
        pw.x68_flg = r.al;

        // 2.6 Additions
        pw.prg_flg = 0;
        if (pw.md[r.getSi() & 0xffff].dat != (pw.max_part2 + 1) * 2) {
            r.setBx(Common.getLe16(pw.md, (r.getSi() & 0xffff) + (2 * (pw.max_part2 + 1))));
            r.addBx(r.getSi());
            pw.prgdat_adr = r.getBx() & 0xffff;
            pw.prg_flg = 1;
        }

//not_prg:
//prg:

        r.setCx((short) pw.max_part2);
        r.dl = 0;
        r.setBx((short) 0); // offset part_data_table
        pw.part_data_table = new int[22];
        for (int i = 0; i < pw.part_data_table.length; i++) pw.part_data_table[i] = i; // KUMA: Because it's an ordered sequence.

//din0:
        do {
            r.di = (short) pw.part_data_table[r.getBx() & 0xffff]; // di = part workarea // KUMA: Index of each part
            r.incBx();
//logger.log(Level.DEBUG, "si: %d, get: %d, len: %d".formatted(r.getSi(), Common.GetLe16(pw.md, r.getSi() & 0xffff), pw.md.length));
            // usually file starts 00 1a 00 ..
            r.setAx(Common.getLe16(pw.md, r.getSi() & 0xffff)); // ax = part start addr
            r.addSi((short) 2);

            r.addAx((short) pw.mmlbuf);
            if (pw.md[r.getAx() & 0xffff].dat == 0x80) { // Do not play if the first digit is 80h
                r.setAx((short) 0);
            }
//din1:

            pw.partWk[r.di & 0xffff].address = r.getAx();

            pw.partWk[r.di & 0xffff].leng = 1; // One more count to start playing
            r.al = (byte) 0xff; // -1
            pw.partWk[r.di & 0xffff].keyoff_flag = r.al; // Currently being keyed off
            pw.partWk[r.di & 0xffff].mdc = r.al; // MDepth Counter(Infinite)
            pw.partWk[r.di & 0xffff].mdc2 = r.al; //
            pw.partWk[r.di & 0xffff]._mdc = r.al; //
            pw.partWk[r.di & 0xffff]._mdc2 = r.al; //
            pw.partWk[r.di & 0xffff].onkai = r.al; // rest
            pw.partWk[r.di & 0xffff].onkai_def = r.al; // rest
            if ((r.dl & 0xff) < 6) { // break din_not_fm;

                // at Part 0,1,2,3,4,5 (FM1 to 6)
                pw.partWk[r.di & 0xffff].volume = 108; // FM VOLUME DEFAULT= 108
                pw.partWk[r.di & 0xffff].fmpan = (byte) 0xc0; // FM PAN = Middle
                if (pw.board2 != 0) {
                    pw.partWk[r.di & 0xffff].slotmask = (byte) 0xf0; // FM SLOT MASK
                    pw.partWk[r.di & 0xffff].neiromask = (byte) 0xff; // FM Neiro MASK
                } else {
                    if ((r.dl & 0xff) < 3) { // break din_fm_mask; // OPN 3,4,5 neiro/slotmask remains 0
                        pw.partWk[r.di & 0xffff].slotmask = (byte) 0xf0; // FM SLOT MASK
                        pw.partWk[r.di & 0xffff].neiromask = (byte) 0xff; // FM Neiro MASK
//                        break init_exit;
                    } else {
//din_fm_mask:
                        pw.partWk[r.di & 0xffff].partmask |= 0x20; // FM mask at s0
                    }
                }

//                break init_exit;
            } else {
//din_not_fm:
                if ((r.dl & 0xff) < 9) { // break din_not_psg;
                    // at Part 6,7,8 (PSG1 to 3)
                    pw.partWk[r.di & 0xffff].volume = 8; // PSG VOLUME DEFAULT= 8
                    pw.partWk[r.di & 0xffff].psgpat = 7; // PSG = TONE
                    pw.partWk[r.di & 0xffff].envf = 3; // PSG ENV = NONE / normal
//                    break init_exit;
                } else {
//din_not_psg:
                    if (r.dl == 9) { // break din_not_pcm;
                        if (pw.board2 != 0) {
                            if (pw.adpcm != 0) {
                                // at Part 9 (OPNA/ADPCM)
                                pw.partWk[r.di & 0xffff].volume = (byte) 128; // PCM VOLUME DEFAULT= 128
                                pw.partWk[r.di & 0xffff].fmpan = (byte) 0xc0; // PCM PAN = Middle
                            }
                            if (pw.pcm != 0) {
                                // at Part 9 (OPNA/PCM)
                                pw.partWk[r.di & 0xffff].volume = (byte) 128; // PCM VOLUME DEFAULT= 128
                                pw.partWk[r.di & 0xffff].fmpan = 0x00;
                                pw.pcm86_pan_flag = 0; // Mid
                                pw.revpan = 0; // Reverse phase off
                            }
                        }
//                        break init_exit;
                    } else {
//din_not_pcm:
                        if (r.dl == 10) { // break not_rhythm;
                            // at Part 10 (Rhythm)
                            pw.partWk[r.di & 0xffff].volume = 15; // PPSDRV volume
//                            break init_exit;
                        } else {
//not_rhythm:
                        }
                    }
                }
            }
//init_exit:
            r.dl++;
            r.decCx();

        } while ((r.getCx() & 0xffff) > 0);

        //
        // Set Rhythm's address table
        //
        r.setAx(Common.getLe16(pw.md, r.getSi() & 0xffff)); // ax = part start addr
        r.addSi((short) 2);
        r.addAx((short) pw.mmlbuf);
        pw.radtbl = r.getAx() & 0xffff;

        pw.rhyadr = 0; // offset rhydmy
        pw.rd = pw.rdDmy; // rhyadr refers to rdDmy (music data array for dummy)
    }

    /**
     * Initializes DATA AREA
     */
    private void data_init() {
        r.al = 0;
        pw.fadeout_volume = r.al;
        pw.fadeout_speed = r.al;
        pw.fadeout_flag = r.al;
        data_init2();
    }

    private void data_init2() {
        r.setCx((short) pw.max_part1);
        r.di = (short) pw.part1; // offset part1

//di_loop:
        do {
            r.stack.push(r.getCx());
            r.setBx(r.di++); // KUMA: Maybe it should be incremented...
            r.dh = pw.partWk[r.getBx() & 0xffff].partmask;
            r.dl = pw.partWk[r.getBx() & 0xffff].keyon_flag;
            r.setCx((short) 0); // type qq // KUMA: Does it fit the size of the partwork?

            //pushf
            //cli

            r.al = 0;
            pw.partWk[r.getBx() & 0xffff].clear();

            r.dh &= 0xf; // 0dh;temporary,s,m,ade other than
            pw.partWk[r.getBx() & 0xffff].partmask = r.dh; // Save only partmask
            pw.partWk[r.getBx() & 0xffff].keyon_flag = r.dl; // keyon_flag save
            pw.partWk[r.getBx() & 0xffff].onkai = (byte) 0xff; // -1; // Set onkai to rest
            pw.partWk[r.getBx() & 0xffff].onkai_def = (byte) 0xff; // -1; // Set onkai to rest

            //popf

            r.setCx(r.stack.pop());
            r.decCx();
        } while ((r.getCx() & 0xffff) > 0);

        r.setAx((short) 0);
        pw.tieflag = r.al;
        pw.status = r.al;
        pw.status2 = r.al;
        pw.syousetu = r.getAx();
        pw.opncount = r.al;
        pw.timerATime = r.al;
        pw.lastTimerAtime = r.al;
        pw.fmKeyOnDataTbl = new byte[] {
            0, 0, 0, 0, 0, 0
        } ;
        //pw.omote_key = new byte[] { 0, 0, 0 };
        pw.omote_key1Ptr = 0;
        pw.omote_key2Ptr = 1;
        pw.omote_key3Ptr = 2;
        //pw.ura_key = new byte[] { 0, 0, 0 };
        pw.ura_key1Ptr = 3;
        pw.ura_key2Ptr = 4;
        pw.ura_key3Ptr = 5;
        pw.fm3_alg_fb = r.al;
        pw.af_check = r.al;
        pw.pcmstart = r.getAx() & 0xffff;
        pw.pcmstop = r.getAx() & 0xffff;
        pw.pcmrepeat1 = r.getAx();
        pw.pcmrepeat2 = r.getAx();
        pw.pcmrelease = (short) 0x8000;
        pw.kshot_dat = r.getAx() & 0xffff;
        pw.rshot_dat = r.al;
        pw.last_shot_data = r.al;
        pw.slotdetune_flag = r.al;
        pw.slot_detune1 = 0;
        pw.slot_detune2 = 0;
        pw.slot_detune3 = 0;
        pw.slot_detune4 = 0;
        pw.slot3_flag = r.al;
        pw.ch3mode = 0x03f;
        pw.fmsel = r.al;
        pw.syousetu_lng = 96;
        r.setAx((short) pw.fm1_port1);
        pw.fm_port1 = r.getAx();
        r.setAx((short) pw.fm1_port2);
        pw.fm_port2 = r.getAx();
        r.al = pw._fm_voldown;
        pw.fm_voldown = r.al;
        r.al = pw._ssg_voldown;
        pw.ssg_voldown = r.al;
        r.al = pw._pcm_voldown;
        pw.pcm_voldown = r.al;
        if (pw.ppz != 0) {
            r.al = pw._ppz_voldown;
            pw.ppz_voldown = r.al;
        }
        r.al = pw._rhythm_voldown;
        pw.rhythm_voldown = r.al;
        r.al = pw._pcm86_vol;
        pw.pcm86_vol = r.al;
    }

    /**
     * OPN INIT
     */
    private void opn_init() {
        r.setDx((short) 0x2983);
        opnset44();

        pw.psnoi = 0;
        if (pw.effon == 0) { // break no_init_psnoi;

            r.setDx((short) 0x0600); // PSG Noise
            opnset44();

            pw.psnoi_last = 0;
        }
//no_init_psnoi:

        //
        // SSG - EG RESET(4.8s)
        //
        if (pw.board2 != 0) {
            r.setBx((short) 2);
            sel44();
        }

//sr01:
        while (true) {

            r.setCx((short) 15); // I don't care about sound effects, it's just a feature.
            r.setDx((short) 0x9000); // SSG - EG = 0
//sr02:
            do {
                r.al = r.cl;
                r.al &= 3;
                if (r.al != 0) { // break sr03;
                    opnset();
                }
//sr03:
                r.dh++;
                r.decCx();
            } while ((r.getCx() & 0xffff) > 0);

            if (pw.board2 != 0) {
                sel46();
                r.decBx();
                if (r.getBx() != 0) continue; // break sr01;
                sel44();
            }
            break;
        }

        //
        // For YM2203, this is the end
        //
        if (pw.board2 == 0) {
            if (pw.ongen == 0) { // 2203 ?
                return;
            }
        }

        //
        // For YM2608
        // PAN / HARDLFO DEFAULT
        //
//init_2608:
//endif

        if (pw.board2 != 0) {
            r.setBx((short) 2);
            //; call sel44; It's okay because it won't fly to the main
        }
//pd01:
        while (true) {

            r.setDx((short) 0xb4c0); // PAN = MID / HARDLFO = OFF
            r.setCx((short) 3);

//pd00:
            do {
pd03: // ↑
                {
                    if (pw.fm_effec_flag != 0) {
                        // There was a bug here...(4.8s) // KUMA: It was confusing so let me clarify it.
                        if (pw.board2 == 0) {
                            if (r.getCx() == 1) break pd03;
                        } else {
                            if (r.getCx() == 1 && r.getBx() == 2) break pd03;
                        }
                    }
//pd02:;
                    opnset();
                }
//pd03:
                r.dh++;

                r.decCx();
            } while (r.getCx() != 0);

            if (pw.board2 != 0) {
                sel46(); // It's okay because it won't fly to the main
                r.decBx();
                if (r.getBx() != 0) continue; // break pd01;
                sel44(); // It's okay because it won't fly to the main
            }
            break;
        }

        r.setDx((short) 0x2200); // HARDLFO = OFF
        pw.port22h = r.dl;
        opnset44();

        if (pw.board2 != 0) {
            //
            // Rhythm Default = Pan : Mid , Vol: 15
            //
            r.di = 0; // offset rdat
            r.setCx((short) 6);
            r.al = (byte) 0b1100_1111;
            do {
                pw.rdat[r.di & 0xffff] = r.al;
                r.di++;
                r.decCx();
            } while (r.getCx() != 0);

            r.setDx((short) 0x10ff);
            opnset44(); // Rhythm All Dump

            //
            // Rhythm Total Level Set
            //
//rtlset:
            r.dl = 48;
            r.al = pw.rhythm_voldown;
            if (r.al != 0) { // break rtlset2r;

                r.dl <<= 2; // 0 - 63 > 0 - 255
                r.al = (byte) -r.al;
                r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
                r.dl = r.ah;
                r.dl = (byte)((r.dl & 0xff) >>> 2); // 0 - 255 > 0 - 63
            }
//rtlset2r:
            pw.rhyvol = r.dl;
            r.dh = 0x11;
            opnset44();

            //
            // PCM reset & Limit set
            //
            if (pw.ademu == 0) {
                if (pw.pcm_gs_flag != 1) {
                    r.setDx((short) 0xcff);
                    opnset46();
                    r.setDx((short) 0xdff);
                    opnset46();
                }
//pr_non_pcm:
            }

            //
            // PPZ Pan Init.
            //
            if (pw.ppz + pw.ademu != 0) {
                r.setDx((short) 5);
                r.setAx((short) 0x1300);
                r.setCx((short) 8);
//ppz_pan_init_loop:
                do {
                    r.al = r.cl;
                    r.al--;
                    ChipDatum cd = new ChipDatum(0x13, r.al & 0xff, r.getDx() & 0xffff);
                    ppz8em.apply(cd); // .SetPan(r.al, r.dx);
                    r.decCx();
                } while (r.getCx() != 0);
            }
        } else {
            r.setDx((short) pw.fm2_port1);
            //    pushf
            //    cli
            rdychk();
            r.al = 0x10;
            pc98.outPort(r.getDx(), r.al);
            r.setDx((short) pw.fm2_port2);
            _wait();
            r.al = (byte) 0x80;
            pc98.outPort(r.getDx(), r.al);
            _wait();
            r.al = 0x18;
            pc98.outPort(r.getDx(), r.al);
            //    popf
        }
    }

    /**
     * Stops music.
     */
    private void mstop_f() {
        r.al = pw.timerAFlag;
        r.al |= pw.timerBFlag;
        if (r.al != 0) {
            pw.music_flag |= 2; // Do not execute during TA/TB processing
            pw.ah_push = (byte) 0xff; // -1
            return;
        }
//_mstop:
        pw.fadeout_flag = 0; // If mstop is done externally, set it to 0.

        //mstop(); // KUMA: The same process is performed when it is done from outside (do not allow the sound source to be operated from another thread)
        pw.music_flag |= 2;
        pw.ah_push = (byte) 0xff;
    }

    private void mstop() {
        //    pushf
        //    cli
        pw.music_flag &= (byte) 0xfd;
        r.setAx((short) 0);
        pw.play_flag = r.al;
        pw.pause_flag = r.al;
        pw.fadeout_speed = r.al;
        r.al--;
        pw.status2 = r.al;
        pw.fadeout_volume = r.al;
        //    popf
        silence();
    }

    /**
     * MUSIC PLAYER MAIN[FROM TIMER - B]
     */
    private void mmain() {
        int w = 0; // kuma: added

        pw.loop_work = 3;
        pw.nowLoopCounter = Integer.MAX_VALUE;

        if (pw.x68_flg == 0) { // break mmain_fm;

            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;

            r.di = (short) pw.part7; // offset part7
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 1;
            psgmain(); // SSG1
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part8; // offset part8
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 2;
            psgmain(); // SSG2
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part9; // offset part9
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 3;
            psgmain(); // SSG3
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        }
//mmain_fm:
        if (pw.board2 != 0) {
            sel46();

            r.di = (short) pw.part4; // offset part4
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 1;
            fmmain(); // FM4 OPNA
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part5; // offset part5
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 2;
            fmmain(); // FM5 OPNA
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part6; // offset part6
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            pw.partb = 3;
            fmmain(); // FM6 OPNA
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            sel44();
        }

        r.di = (short) pw.part1; // offset part1
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        pw.partb = 1;
        fmmain(); // FM1
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part2; // offset part2
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        pw.partb = 2;
        fmmain(); // FM2
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3; // offset part3
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        pw.partb = 3;
        fmmain(); // FM3
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3b; // offset part3b
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 Expansion 1
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3c; // offset part3c
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 Expansion 2
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3d; // offset part3d
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 Expansion 3
        if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

        if (pw.x68_flg == 0) { // break mmain_exit;

            r.di = (short) pw.part11; // offset part11
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
            rhythmmain(); // RHYTHM
            if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;

            if (pw.board2 != 0) {
                r.di = (short) pw.part10; // offset part10
                w = 0;
                if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                if (pw.useP86DRV) {
                    pcmdrv86.pcmmain(); // ADPCM/PCM(IN "pcmdrv.asm"/"pcmdrv86.asm")
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added
                } else {
                    pcmdrv.pcmmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added
                }

                if (pw.ppz != 0) {
                    r.di = (short) pw.part10a; // offset part10a
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 0;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10b; // offset part10b
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 1;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10c; // offset part10c
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 2;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10d; // offset part10d
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 3;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10e; // offset part10e
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 4;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10f; // offset part10f
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 5;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10g; // offset part10g
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 6;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10h; // offset part10h
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di & 0xffff]].address == 0) w = 1; // kuma: added
                    pw.partb = 7;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di & 0xffff].loopcheck == 3 && pw.partWk[r.di & 0xffff].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di & 0xffff].loopCounter, pw.nowLoopCounter); // kuma: added
                }
            }
        }
//mmain_exit:

        //logger.log(Level.TRACE, "loop counter:%d", pw.nowLoopCounter);

        if (pw.loop_work == 0) { // break mmain_loop;
            return;
        }
//mmain_loop:

        r.setCx((short) pw.max_part1);
        r.setBx((short) 0); // offset part_data_table

//mm_din0:
        do {
            r.di = (short) pw.part_data_table[r.getBx() & 0xffff]; // [bx]; di = part workarea
            r.incBx();

            if (pw.partWk[r.di & 0xffff].loopcheck != 3) { // break mm_notset;
                pw.partWk[r.di & 0xffff].loopcheck = 0;
            }
//mm_notset:
            r.decCx();
        } while (r.getCx() != 0);

        if (pw.loop_work != 3) { // break mml_fin;

            pw.status2++;
            if (pw.status2 == (byte) 0xff) { // Don't let it -1 // break mml_ret;

                pw.status2 = 1;
            }
//mml_ret:
            return;
        }
//mml_fin:
        pw.status2 = (byte) 0xff; // -1;
    }

    /**
     * Back FM Select
     */
    private void sel46() {
        r.setAx((short) pw.fm2_port1);
        pw.fm_port1 = r.getAx();
        r.setAx((short) pw.fm2_port2);
        pw.fm_port2 = r.getAx();
        pw.fmsel = 1;
    }

    /**
     * Back to Fore
     */
    private void sel44() {
        r.setAx((short) pw.fm1_port1);
        pw.fm_port1 = r.getAx();
        r.setAx((short) pw.fm1_port2);
        pw.fm_port2 = r.getAx();
        pw.fmsel = 0;
    }

    //
    // Mainly FM sound source playback
    //

    private final Supplier<Object> mpexitRef = this::mpexit;
    private final Supplier<Object> mpexitpRef = this::mpexitp;
    private final Supplier<Object> mp10Ref = this::mp10;
    private final Supplier<Object> mp1Ref = this::mp1;
    private final Supplier<Object> mnp_retRef = this::mnp_ret;
    private final Supplier<Object> porta_returnRef = this::porta_return;
    private final Supplier<Object> porta_returnpRef = this::porta_returnp;
    private final Supplier<Object> fmmnp_1Ref = this::fmmnp_1;
    private final Supplier<Object> psgmnp_1Ref = this::psgmnp_1;
    private final Supplier<Object> mp1cpRef = this::mp1cp;
    private final Supplier<Object> mp1pRef = this::mp1p;

//    private void fmmain_ret() {
//        ret
//    }

    private void fmmain() {
        r.setSi(pw.partWk[pw.part_data_table[r.di & 0xffff]].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0) return;

        Supplier<Object> ret;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            ret = this::fmmain_nonplay;
        else
            ret = this::fmmain_c_1;

        do {
            ret = (Supplier<Object>) ret.get();
        } while (ret != null);
    }

    private Supplier<Object> fmmain_c_1() {
        // Duration -1
        pw.partWk[r.di & 0xffff].leng--;
        r.al = pw.partWk[r.di & 0xffff].leng;

        // KEYOFF CHECK & Keyoff
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 3) == 0) { // already keyed off? // break mp0;

            if ((r.al & 0xff) <= (pw.partWk[r.di & 0xffff].qdat & 0xff)) { // Q value => keyoff when remaining Length value // break mp0;

                keyoff(); // AL will not break
                pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
            }
        }
//mp0:
        // LENGTH CHECK
        if (r.al != 0) return mpexitRef;
        return mp10Ref;
    }

    private Supplier<Object> mp10() {
        pw.partWk[r.di & 0xffff].lfoswi &= (byte) 0xf7; // Porta off
        return mp1Ref;
    }

    private Supplier<Object> mp1() { // DATA READ
        do {
            pw.cmd = pw.md[r.getSi() & 0xffff];
            //if (r.si == pw.jumpIndex)
            //pw.jumpIndex = -1; // KUMA:Added

            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            if ((r.al & 0xff) < 0x80) { // break mp2;
//mp2:
                // F-NUMBER SET

                flashMacroList();

                lfoinit();
                oshift();
                fnumset();

                ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
                cd.additionalData = pw.cmd;
                WriteOPNARegister.accept(cd);

                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                pw.partWk[r.di & 0xffff].leng = r.al;
                calc_q();
                return porta_return();
            }
            if (r.al == (byte) 0x80) { // break mp15;
                // END OF MUSIC[When there was an "L" I went back there]
//mp15:
                flashMacroList();

                r.decSi();
                pw.partWk[r.di & 0xffff].address = r.getSi(); // mov[di],si
                pw.partWk[r.di & 0xffff].loopcheck = 3;
                pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
                r.setBx(pw.partWk[r.di & 0xffff].partloop);
                if (r.getBx() == 0) return mpexitRef;

                // When there was an "L"
                r.setSi(r.getBx());
                pw.partWk[r.di & 0xffff].loopcheck = 1;
                pw.partWk[r.di & 0xffff].loopCounter++;
                return mp1Ref;
            }

            // ELSE COMMANDS
            Object o = commands();
            while (o != null && o != mp1Ref) {
                o = ((Supplier<Object>) o).get();
                if (o == mnp_retRef)
                    return mnp_retRef;
                if (o == porta_returnRef)
                    return porta_returnRef;
            }
        } while (true);
    }

    public void flashMacroList() {
        if (pw.cmd.args != null && pw.cmd.args.size() > 2) {
            Object obj = pw.cmd.args.get(2);
            if (obj instanceof MmlDatum[] mds) {
                for (MmlDatum md : mds) execIDESpecialCommand(md);
            }
        }
    }

    private Supplier<Object> porta_return() {
        if (pw.partWk[r.di & 0xffff].volpush != 0) { // break mp_new;
            if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) { // break mp_new;
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) { // break mp_new;
                    pw.volpush_flag = 0;
                    pw.partWk[r.di & 0xffff].volpush = 0;
                }
            }
        }
//mp_new:
        volset();
        otodasi();
        keyon();
        pw.partWk[r.di & 0xffff].keyon_flag++;
        pw.partWk[r.di & 0xffff].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di & 0xffff].keyoff_flag = r.al;
        if (pw.md[r.getSi() & 0xffff].dat != 0xfb) // If there is an '&' immediately after, keyoff will not occur.
            return mnp_retRef;
        pw.partWk[r.di & 0xffff].keyoff_flag = 2;
        return mnp_retRef;
    }

    private Supplier<Object> mpexit() { // LFO & Portament & Fadeout processing to finish
        if (pw.board2 != 0) {
            if (pw.partWk[r.di & 0xffff].hldelay_c != 0) {
                pw.partWk[r.di & 0xffff].hldelay_c--;
                if (pw.partWk[r.di & 0xffff].hldelay_c == 0) {
                    r.dh = pw.partb;
                    r.dh += (byte) (0xb4 - 1);
                    r.dl = pw.partWk[r.di & 0xffff].fmpan;
                    opnset();
                }
            }
//not_hldelay:
        }
        if (pw.partWk[r.di & 0xffff].sdelay_c != 0) {
            pw.partWk[r.di & 0xffff].sdelay_c--;
            if (pw.partWk[r.di & 0xffff].sdelay_c == 0) {
                if ((pw.partWk[r.di & 0xffff].keyoff_flag & 1) == 0) { // Have you already keyed off?
                    keyon();
                }
            }
        }
//not_sdelay:
        r.cl = pw.partWk[r.di & 0xffff].lfoswi;
        if ((r.cl & r.cl) == 0) {
            //break nolfosw; // Processing without skipping
            if (pw.fadeout_speed != 0) {
                volset();
            }
            return mnp_retRef;
        }
        r.al = r.cl;
        r.al &= 8;
        pw.lfo_switch = r.al;
        if ((r.cl & 3) != 0) {
            lfo();
            if (r.carry) {
                r.al = r.cl;
                r.al &= 3;
                pw.lfo_switch |= r.al;
            }
        }
//not_lfo:
        if ((r.cl & 0x30) != 0) {
            //pushf
            //cli
            lfo_change();
            lfo();
            if (r.carry) {
                lfo_change();
                //popf
                r.al = pw.partWk[r.di & 0xffff].lfoswi;
                r.al &= 0x30;
                pw.lfo_switch |= r.al;
            } else {
//not_lfo1:
                lfo_change();
                //popf
            }
        }
//not_lfo2:
        if ((pw.lfo_switch & 0x19) != 0) {
            if ((pw.lfo_switch & 8) != 0) {
                porta_calc();
            }
//not_porta:
            otodasi();
        }
//vols:
        if ((pw.lfo_switch & 0x22) == 0) {
//nolfosw:
            if (pw.fadeout_speed == 0) return mnp_retRef;
        }
//vol_set:
        volset();
        return mnp_retRef;
    }

    public Supplier<Object> mnp_ret() {
        r.al = pw.loop_work;
        r.al &= pw.partWk[r.di & 0xffff].loopcheck;
        pw.loop_work = r.al;
        _ppz();
        return null;
    }

    /**
     * Calculating the Q factor
     *  break dx
     */
    public void calc_q() {
        if (pw.md[r.getSi() & 0xffff].dat != 0xc1) { //&& // break cq_sular;

            r.dl = pw.partWk[r.di & 0xffff].qdata;
            if (pw.partWk[r.di & 0xffff].qdatb != 0) { // break cq_set;

                r.stack.push(r.getAx());
                r.al = pw.partWk[r.di & 0xffff].leng;
                r.setAx((short) ((r.al & 0xff) * (pw.partWk[r.di & 0xffff].qdatb & 0xff)));
                r.dl = (byte) ((r.dl & 0xff) + (r.ah & 0xff));
                r.setAx(r.stack.pop());
            }
//cq_set:
            if (pw.partWk[r.di & 0xffff].qdat3 != 0) { // break cq_set2;

                // Random-Q
                r.stack.push(r.getAx());
                r.stack.push(r.getCx());
                r.al = pw.partWk[r.di & 0xffff].qdat3;
                r.al &= 0x7f;
                r.setAx(/* signed */ r.al); // cbw
                r.incAx();

                r.stack.push(r.getDx());
                rnd();
                r.setDx(r.stack.pop());

                if ((pw.partWk[r.di & 0xffff].qdat3 & 0x80) == 0) { // break cqr_minus;

                    r.dl += r.al;
//                    break cqr_exit;
                } else {
//cqr_minus:
                    r.carry = ((r.dl & 0xff) - (r.al & 0xff)) < 0;
                    r.dl -= r.al;
                    if (r.carry) { // break cqr_exit;
                        r.dl = 0;
                    }
                }
//cqr_exit:
                r.setCx(r.stack.pop());
                r.setAx(r.stack.pop());
            }
//cq_set2:
            if (pw.partWk[r.di & 0xffff].qdat2 != 0) { // break cq_sete;

                r.dh = pw.partWk[r.di & 0xffff].leng;
                r.carry = ((r.dh & 0xff) - (pw.partWk[r.di & 0xffff].qdat2 & 0xff)) < 0;
                r.dh -= pw.partWk[r.di & 0xffff].qdat2;
                if (r.carry) {
//                    break cq_zero;
                    pw.partWk[r.di & 0xffff].qdat = 0; // <<
                    return; // <<
                }
                if ((r.dl & 0xff) - (r.dh & 0xff) >= 0) { // break cq_sete;
                    r.dl = r.dh; // Minimum guaranteed gate value setting
                }
            }
//cq_sete:
            pw.partWk[r.di & 0xffff].qdat = r.dl;
            return;
        }
//cq_sular:
        r.incSi(); // Slur Command
//cq_zero:
        pw.partWk[r.di & 0xffff].qdat = 0;
    }

    /**
     * FM sound source performance main: When part is masked
     *
     * false: break mnp_ret
     * true: break mp10
     */
    private Supplier<Object> fmmain_nonplay() {
        pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di & 0xffff].leng--;
        if (pw.partWk[r.di & 0xffff].leng != 0) return mnp_retRef;

        if ((pw.partWk[r.di & 0xffff].partmask & 2) != 0) { // Check bit1 (FM sound effect?)
            if (pw.fm_effec_flag == 0) { // ; Did the sound effect end?
                pw.partWk[r.di & 0xffff].partmask &= (byte) 0xfd; // clear bit1
                if (pw.partWk[r.di & 0xffff].partmask == 0) return mp10Ref; // If partmask is 0, restore it.
            }
        }
        return fmmnp_1Ref;
    }

    private Supplier<Object> fmmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.getSi() & 0xffff];
                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                if (r.al == (byte) 0x80) break;
                if ((r.al & 0xff) < 0x80) return this::fmmnp_3;

                Object o = commands();
                while (o != null && o != fmmnp_1Ref) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_retRef) return mnp_retRef;
                }

            } while (true);

            flashMacroList();

//fmmnp_2:
            // END OF MUSIC[When there was an "L" I went back there]
            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi();
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di & 0xffff].partloop);
            if ((r.getBx() & r.getBx()) == 0) return this::fmmnp_4;
            // When there was an "L"
            r.setSi(r.getBx());
            pw.partWk[r.di & 0xffff].loopcheck = 1;
            pw.partWk[r.di & 0xffff].loopCounter++;
        } while (true);
    }

    public Supplier<Object> fmmnp_3() {
        flashMacroList();

        pw.partWk[r.di & 0xffff].fnum = 0; // Set to Rest
        pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
        pw.partWk[r.di & 0xffff].onkai_def = (byte) 0xff; // -1

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].leng = r.al; // Sound length setting
        pw.partWk[r.di & 0xffff].keyon_flag++;
        pw.partWk[r.di & 0xffff].address = r.getSi();

        pw.volpush_flag--;
        if (pw.volpush_flag != 0) {
            pw.partWk[r.di & 0xffff].volpush = 0;
        }

        return this::fmmnp_4;
    }

    public Supplier<Object> fmmnp_4() {
        pw.tieflag = 0;
        pw.volpush_flag = 0;
        return mnp_retRef;
    }

    //
    // SSG sound source performance main
    //
    //psgmain_ret:
    //ret
    private void psgmain() {
        r.setSi(pw.partWk[pw.part_data_table[r.di & 0xffff]].address); // si = PART DATA ADDRESS
        if (r.getSi()  == 0) return;

        //if (r.si == pw.jumpIndex) pw.jumpIndex = -1; // KUMA:Added

        Supplier<Object> ret = null;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            ret = this::psgmain_nonplay;
        else
            ret = this::psgmain_c_1;

        if (ret != null) {
            do {
                ret = (Supplier<Object>) ret.get();
            } while (ret != null);
        }
    }

    private Supplier<Object> psgmain_c_1() {
        // Duration -1
        pw.partWk[r.di & 0xffff].leng--;
        r.al = pw.partWk[r.di & 0xffff].leng;

        // KEYOFF CHECK & Keyoff
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 3) != 0) // Have you already keyed off?
            return this::mp0p;

        if ((r.al & 0xff) > (pw.partWk[r.di & 0xffff].qdat & 0xff)) // Q value => keyoff when remaining Length value
            return this::mp0p;

        keyoffp(); // AL will not break
        pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1

        return this::mp0p;
    }

    // LENGTH CHECK
    private Supplier<Object> mp0p() {
        if (r.al != 0) return mpexitpRef;

        pw.partWk[r.di & 0xffff].lfoswi &= (byte) 0xf7; // Porta off

        return mp1pRef;
    }

    // DATA READ
    private Supplier<Object> mp1p() {
        pw.cmd = pw.md[r.getSi() & 0xffff];

        //if (r.si == pw.jumpIndex)
        //pw.jumpIndex = -1; // KUMA:Added

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) < 0x80) return this::mp2p;
        if (r.al == (byte) 0x80) return this::mp15p;
        return mp1cpRef;
    }

    // ELSE COMMANDS
    private Supplier<Object> mp1cp() {
        Object o = commandsp();
        while (o != null && o != mp1cpRef && o != mp1pRef) {
            o = ((Supplier<Object>) o).get();
            if (o == mnp_retRef) return mnp_retRef;
        }

        return mp1pRef;
    }

    // END OF MUSIC[When there was an "L" I went back there]
    private Supplier<Object> mp15p() {
        flashMacroList();

        r.decSi();
        pw.partWk[r.di & 0xffff].address = r.getSi(); // mov[di],si
        pw.partWk[r.di & 0xffff].loopcheck = 3;
        pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
        r.setBx(pw.partWk[r.di & 0xffff].partloop);
        if (r.getBx() == 0) return mpexitpRef;

        // When there was an "L"
        r.setSi(r.getBx());
        pw.partWk[r.di & 0xffff].loopcheck = 1;
        pw.partWk[r.di & 0xffff].loopCounter++;
        return mp1pRef;
    }

    // TONE SET
    private Supplier<Object> mp2p() {
        flashMacroList();

        lfoinitp();
        oshiftp();
        fnumsetp();

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = pw.cmd;
        WriteOPNARegister.accept(cd);

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].leng = r.al;
        calc_q();
        return porta_returnp();
    }

    private Supplier<Object> porta_returnp() {
        if (pw.partWk[r.di & 0xffff].volpush != 0) {
            if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) {
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) {
                    pw.volpush_flag = 0;
                    pw.partWk[r.di & 0xffff].volpush = 0;
                }
            }
        }
//mp_newp:
        volsetp();
        otodasip();
        keyonp();
        pw.partWk[r.di & 0xffff].keyon_flag++;
        pw.partWk[r.di & 0xffff].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di & 0xffff].keyoff_flag = r.al;
        if (pw.md[r.getSi() & 0xffff].dat != 0xfb) // If there is an '&' immediately after, keyoff will not occur.
        {
            return mnp_retRef;
        }
        pw.partWk[r.di & 0xffff].keyoff_flag = 2;
        return mnp_retRef;
    }

    private Supplier<Object> mpexitp() {
        r.cl = pw.partWk[r.di & 0xffff].lfoswi;
        r.al = r.cl;
        r.al &= 8;
        pw.lfo_switch = r.al;

        if ((r.cl & r.cl) != 0) { // break volsp;

            if ((r.cl & 3) != 0) {
                lfop();
                if (r.carry) {
                    r.al = r.cl;
                    r.al &= 3;
                    pw.lfo_switch |= r.al;
                }
            }
//not_lfop:
            if ((r.cl & 0x30) != 0) {
                // pushf
                //    cli
                lfo_change();
                lfop();
                if (r.carry) {
                    lfo_change();
                    //    popf
                    r.al = pw.partWk[r.di & 0xffff].lfoswi;
                    r.al &= 0x30;
                    pw.lfo_switch |= r.al;
                } else {
//not_lfo1:
                    lfo_change();
                    //    popf
                }
            }
//not_lfop2:
            if ((pw.lfo_switch & 0x19) != 0) {
                if ((pw.lfo_switch & 8) != 0) {
                    porta_calc();
                }
//not_porta:
                otodasip();
            }
        }
//volsp:
        soft_env();
        if (!r.carry) {
            if ((pw.lfo_switch & 0x22) == 0) {
                if (pw.fadeout_speed == 0) {
                    return mnp_ret();
                }
            }
        }
//volsp2:
        volsetp();
        return mnp_ret();
    }

    /**
     * SSG sound source main: When part is masked
     */
    private Supplier<Object> psgmain_nonplay() {
        pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di & 0xffff].leng--;
        if (pw.partWk[r.di & 0xffff].leng != 0) return mnp_retRef;

        pw.partWk[r.di & 0xffff].lfoswi &= 0xf7; // Porta off
        return psgmnp_1Ref;
    }

    private Supplier<Object> psgmnp_1() {
psgmnp_4:
        do {
            do {
                pw.cmd = pw.md[r.getSi() & 0xffff];
                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                if (r.al == (byte) 0x80) break;
                if ((r.al & 0xff) < 0x80) break psgmnp_4;

                if (r.al == (byte) 0xda) { // break psgmnp_3;
                    ssgdrum_check(); // Check for SSG revival only in the case of Portament?
                    if (r.carry) return mp1cpRef; // In case of revival, proceed to the main process
                }
//psgmnp_3:
                Object o = commandsp();
                while (o != null && o != psgmnp_1Ref) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_retRef)
                        return mnp_retRef;
                    if (o == porta_returnpRef)
                        return porta_returnpRef;
                }

            } while (true);

            // END OF MUSIC[When there was an "L" I went back there]
//psgmnp_2:
            flashMacroList();

            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi();
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di & 0xffff].partloop);

            if ((r.getBx() & r.getBx()) == 0) return this::fmmnp_4;

            // When there was an "L"
            r.setSi(r.getBx());
            pw.partWk[r.di & 0xffff].loopcheck = 1;
            pw.partWk[r.di & 0xffff].loopCounter++;
        } while (true);

//psgmnp_4:
        ssgdrum_check();
        if (!r.carry) return this::fmmnp_3;

        return this::mp2p; // SSG Resurrection
    }

    /**
     * Check whether to turn off SSG drums and revive SSG
     *
     * input AL<- Command
     * output cy = 1: Resurrect
     */
    private void ssgdrum_check() {
        if ((pw.partWk[r.di & 0xffff].partmask & 1) == 0) { // Check bit0 (SSG masked?) // break sdrchk_2; // Do not stop the drum while SSG is masked
            if ((pw.partWk[r.di & 0xffff].partmask & 2) != 0) { // Check bit1 (SSG sound effect in progress?) // break sdrchk_2; //SSG drums are not playing
                if ((pw.effon & 0xff) < 2) { // Are any sound effects other than SSG drums being played? // break sdrchk_2; // Do not turn off normal sound effects

                    r.ah = r.al; // AL will not break
                    r.ah &= 0xf; // When it is 0DAH (portament), it is 0AH, so it is okay.
                    if (r.ah != 0xf) { // Rest? // break sdrchk_2; // ; Drums don't stop during rests
                        if (pw.effon == 1) { // Is the SSG drum still playing? // break sdrchk_1; // It's already been erased
                            r.stack.push(r.getAx());
                            efcdrv.effend(); // Turn off SSG drums
                            r.setAx(r.stack.pop());
                        }
//sdrchk_1:
                        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xfd; // clear bit1
                        if (pw.partWk[r.di & 0xffff].partmask == 0) { // break sdrchk_2; // Still masked by something
                            r.carry = true;
                            return; // If partmask is 0, restore it.
                        }
                    }
                }
            }
        }
//sdrchk_2:
        r.carry = false;
    }

    //
    // Rhythm section performance Main
    //

//    private void rhythmmain_ret() {
//    }

    private void rhythmmain() {
        pw.checkJumpIndexSI = true;
        pw.checkJumpIndexBX = false;
        r.setSi(pw.partWk[pw.part_data_table[r.di & 0xffff]].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0) return;

        //if (r.si == pw.jumpIndex) pw.jumpIndex = -1; // KUMA:Added

        // Duration -1
        pw.partWk[r.di & 0xffff].leng--;
        if (pw.partWk[r.di & 0xffff].leng != 0) {
            mnp_ret();
            return;
        }

//rhyms0:
        r.setBx((short) pw.rhyadr);
        pw.checkJumpIndexSI = false;
        pw.checkJumpIndexBX = true;
        rhyms00();
    }

    private void rhyms00() {
//rhyms00:
        while (true) {
            pw.cmd = pw.rd[r.getBx() & 0xffff];
            r.al = (byte) pw.rd[r.getBx() & 0xffff].dat; // rd is set to either md (regular performance data) or rdDmy (dummy performance data).
            r.incBx();

            if (r.al == (byte) 0xff) {
                reom();
                return;
            }
            if ((r.al & 0x80) != 0) {
                int r = rhythmon();
                if (r == 1) continue; // break rhyms00;
                return;
            }
            break;
        }
        flashMacroList();

        pw.kshot_dat = 0; // rest
        rlnset();
    }

    /**
     * Read and set note length
     */
    private void rlnset() {
        r.al = (byte) pw.rd[r.getBx() & 0xffff].dat; // mov al,[bx]

        if ((r.getBx() & 0xffff) == pw.jumpIndex)
            pw.jumpIndex = -1; // KUMA:Added

        r.incBx();

        pw.rhyadr = r.getBx() & 0xffff;
        pw.partWk[r.di & 0xffff].leng = r.al;
        pw.partWk[r.di & 0xffff].keyon_flag++;

        fmmnp_4();
        mnp_ret();
    }

//    private void mnp_ret() {
//        r.al = pw.loop_work;
//        r.al &= pw.partWk[r.di & 0xffff].loopcheck;
//        pw.loop_work = r.al;
//        _ppz();
//        return;
//    }

    private void reom() {
        // KUMA: Analysis of K part
reom:
        while (true) {
rfin: // ↑
            {
                pw.checkJumpIndexSI = true;
                pw.checkJumpIndexBX = false;
                do {
                    pw.cmd = pw.md[r.getSi() & 0xffff];
                    r.al = (byte) pw.md[r.getSi() & 0xffff].dat;

                    if ((r.getSi() & 0xffff) == pw.jumpIndex)
                        pw.jumpIndex = -1; // KUMA: Added for skip playback

                    r.incSi();

                    if (r.al == (byte) 0x80) break rfin;
                    if ((r.al & 0xff) < 0x80) break; // If the Rn command is specified for the K part

                    Object o = commandsr();
                    while (o != null) {
                        o = ((Supplier<Object>) o).get();
                    }
                } while (true);

                // KUMA: Preparing to switch processing to R part

                //logger.log(Level.TRACE, "%d", pw.cmd);

                flashMacroList();

                MmlDatum md = new MmlDatum(MMLType.TraceLocate, null, LinePos.Copy(pw.cmd.linePos), 0xff);
                md = new MmlDatum(MMLType.TraceLocate, List.of(0, 1, md), LinePos.Copy(pw.cmd.linePos), 0xff);
                ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
                cd.additionalData = md;
                writeDummy(cd);

//re00:
                pw.partWk[r.di & 0xffff].address = r.getSi();
                pw.checkJumpIndexSI = false;
                r.ah = 0;
                r.addAx(r.getAx());
                r.addAx((short) pw.radtbl); // KUMA: R part address table 0x00 to maximum 0x7f can exist
                r.setBx(r.getAx());
                r.setAx((short) (pw.md[r.getBx() & 0xffff].dat + pw.md[(r.getBx() + 1) & 0xffff].dat * 0x100)); // mov ax,[bx]

                r.addAx((short) pw.mmlbuf);
                pw.rhyadr = r.getAx() & 0xffff;

                //KUMA: Switch processing to R part

                r.setBx(r.getAx());
                pw.rd = pw.md;

                pw.checkJumpIndexBX = true;

//rhyms00:
                while (true) {
                    pw.cmd = pw.rd[r.getBx() & 0xffff]; // mov al,[bx]
                    r.al = (byte) pw.rd[r.getBx() & 0xffff].dat; // mov al,[bx]
                    r.incBx();

                    if (r.al == (byte) 0xff) { // KUMA: If R part is terminated, return to K part analysis
                        pw.checkJumpIndexBX = false;
                        continue reom; // break reom;
                    }

                    // 0x00 - 0x7f : rest
                    // 0x80 - 0xbf : note (pronunciation)
                    // 0xc0 - 0xff : command
                    if ((r.al & 0x80) != 0) { // KUMA: Check if the most significant bit is 1 (check if mml command/sound)
                        int r = rhythmon();
                        if (r == 1) continue; // break rhyms00; // KUMA: In case of 1 (to execute commands continuously), loop
                        pw.checkJumpIndexBX = false;
                        return;
                    }
                    break;
                }

                // KUMA: If al is 0 to 0x7f, it processes a rest.

                flashMacroList();

                pw.kshot_dat = 0; // rest
                rlnset();
                pw.checkJumpIndexBX = false;
                return;
            }
//rfin:
            // KUMA: K part termination

            flashMacroList();

            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi(); // mov[di],si
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            r.setBx(pw.partWk[r.di & 0xffff].partloop);
            if (r.getBx() != 0) { // break rf00;

                // When there was an "L"
                r.setSi(r.getBx());
                pw.partWk[r.di & 0xffff].loopcheck = 1;
                pw.partWk[r.di & 0xffff].loopCounter++;
                continue; // break reom;
            }
            break;
        }
//rf00:
        r.setBx((short) 0); // offset rhydmy
        pw.rhyadr = r.getBx() & 0xffff;
        pw.rd = pw.rdDmy;

        fmmnp_4();
        mnp_ret();
    }

    /**
     * PSG Rhythm ON
     */
    private int rhythmon() {
        if ((r.al & 0b0100_0000) != 0) { // KUMA: If bit6 is 0, proceed to rhythm sound generation process break rhy_shot;

            // KUMA: Each command process uses r.si as an index, so bx and si are swapped.

            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;
            short a = r.getSi();
            r.setSi(r.getBx());
            r.setBx(a);
            r.stack.push(r.getBx());

            Object o = commandsr(); // KUMA: al indicates that the command is being processed.
            while (o != null) {
                o = ((Supplier<Object>) o).get(); // KUMA: Command execution
            }

            // KUMA: Undo

            pw.checkJumpIndexSI = false;
            pw.checkJumpIndexBX = true;
            r.setBx(r.stack.pop());
            a = r.getSi();
            r.setSi(r.getBx());
            r.setBx(a);
//            rhyms00();
            return 1;
        }
//rhy_shot:
        if (pw.partWk[r.di & 0xffff].partmask != 0) { // break r_nonmask;
            pw.kshot_dat = 0;
            r.incBx();
            rlnset(); // If masked
            return 0;
        }
//r_nonmask:
        r.ah = r.al;
        pw.cmd = pw.rd[r.getBx() & 0xffff];
        r.al = (byte) pw.rd[r.getBx() & 0xffff].dat; // Original note commands
        r.incBx();
        r.andAx(0x3fff);
        pw.kshot_dat = r.getAx();
        if (r.getAx() == 0) {
            rlnset();
            return 0;
        }
        pw.rhyadr = r.getBx();
        if (pw.board2 != 0) {
            if (pw.kp_rhythm_flag != 0) { // break rsb210;
                // KUMA: If SB2 is set to kp_rhythm_flag, the rhythm sound source will also sound.
                r.stack.push(r.getAx());
                r.setBx((short) 0); // offset rhydat
                r.setCx((short) 11);
//rsb2lp:
                do {
                    r.carry = ((r.getAx() & 1) != 0);
                    r.setAx((short) (((r.getAx() & 0xffff) >> 1) | (r.carry ? 0x8000 : 0)));
                    if (r.carry) {
                        rshot();
//                        break rsb200;
                    } else {
                        r.incBx();
                        r.incBx();
                    }
//rsb200:
                    r.incBx();
                    r.decCx();
                } while (r.getCx() != 0);
                r.setAx(r.stack.pop());
            }
        }
//rsb210:
roret: // ↑
        {
            r.setBx(r.getAx());
            if (pw.fadeout_volume != 0) { // break rpsg;
                if (pw.board2 != 0) {
                    if (pw.kp_rhythm_flag != 0) { // break rpps_check;
                        r.dl = pw.rhyvol;
                        volset2rf();
                    }
//rpps_check:
                }
                if (pw.ppsdrv_flag == 0)
                    break roret; // No sound when using ppsdrv during fadeout
            }
//rpsg:
            r.al = (byte) 0xff; // -1
rolop:
            while (true) {
                do {
                    r.al++;
                    r.carry = ((r.getBx() & 1) != 0);
                    r.srBx(1);
                } while (!r.carry); // break rhygo;
//                break rolop;
//rhygo:
                r.stack.push(r.di);
                r.stack.push(r.getSi());
                r.stack.push(r.getBx());
                r.stack.push(r.getAx());
                efcdrv.effgo();
                r.setAx(r.stack.pop());
                r.setBx(r.stack.pop());
                r.setSi(r.stack.pop());
                r.di = r.stack.pop();

                if (pw.ppsdrv_flag == 0)
                    break; // roret;
                if (r.getBx() == 0)
                    break; // roret;
            } // break rolop; // If you use PPSDRV, try playing more than one note.
        }
//roret:
        r.setBx((short) pw.rhyadr);
        rlnset();
        return 0;
    }

    private void rshot() {
        if (pw.board2 != 0) {
//rshot:
            r.setDx((short) ((PW.rhydat[r.getBx() & 0xffff] & 0xff) + (PW.rhydat[(r.getBx() + 1) & 0xffff] & 0xff) * 0x100));
            byte x = r.dh;
            r.dh = r.dl;
            r.dl = x;
            r.incBx();
            r.incBx();
            opnset44();
            r.dh = 0x10;
            r.dl = PW.rhydat[r.getBx() & 0xffff];
            r.dl &= pw.rhythmmask;
            if (r.dl == 0)
                return; // break rsb200;
            if ((r.dl & 0x80) != 0) { // break rshot00;
                r.dl = (byte) 0b1000_0100;
                opnset44();
                r.dl = 0b0000_1000;
                r.dl &= pw.rhythmmask;
                if (r.dl == 0)
                    return; // break rsb200;
                _rwait();
            }
//rshot00:
            opnset44();
//            return; // break rsb200;
        }
    }

    //
    // Various special command processing
    //

    private Supplier<Object> commands() {
        pw.currentCommandTable = cmdtbl;
        pw.currentWriter = 0;
        r.setBx((short) 0); // offset cmdtbl
        return command00();
    }

    private Supplier<Object> commandsr() {
        pw.currentCommandTable = cmdtblr;
        pw.currentWriter = 1;
        r.setBx((short) 0); // offset cmdtblr
        return command00();
    }

    private Supplier<Object> commandsp() {
        pw.currentCommandTable = cmdtblp;
        pw.currentWriter = 2;
        r.setBx((short) 0); // offset cmdtblp
        return command00();
    }

    public Supplier<Object> command00() {
        if (pw.cmd != null && pw.cmd.args != null && pw.cmd.args.size() > 2 && pw.cmd.args.get(2) instanceof MmlDatum[]) {
            for (MmlDatum md : (MmlDatum[]) pw.cmd.args.get(2)) {
                execIDESpecialCommand(md);
            }
        }

//        if (r.si - 1 < pw.md.length && pw.md[r.si - 1].type == MMLType.IDE) { // KUMA: Added
//            // The al register is irrelevant, and if type is IDE, it is processed as a special command for IDE.
//            // (The compiler needs to be adjusted to create this state only when a compilation request is made from the IDE.)
//            execIDESpecialCommand(pw.md[r.si - 1]);
//            return null;
//        }

        if ((r.al & 0xff) < (PW.com_end & 0xff)) {
            return out_of_commands();
        }

        r.setBx((short) ((~r.al) & 0xff));
        if (pw.ppz != 0) {
            r.stack.push(r.getAx());
            _ppz();
            r.setAx(r.stack.pop());
        }

        logger.log(Level.TRACE, "bx:%04x di:%02x".formatted(r.getBx() & 0xffff, r.di & 0xff));

        Supplier<Object> o = pw.currentCommandTable[r.getBx() & 0xffff];

        if (o == null)
            logger.log(Level.ERROR, "bx:%04x di:%02x".formatted(r.getBx() & 0xffff, r.di & 0xff));

        return o;
    }

    private Supplier<Object> out_of_commands() {
        r.decSi();
        pw.md[r.getSi() & 0xffff].dat = 0x80; // Part END
        return null;
    }

    private Supplier<Object>[] cmdtbl;

    private void setupCmdtbl() {
        cmdtbl = new Supplier[] {
                this::comAt,             // 0xff(0)
                this::comq,              // 0xfe(1)
                this::comv,              // 0xfd(2)
                this::comt,              // 0xfc(3)
                this::comtie,            // 0xfb(4)
                this::comd,              // 0xfa(5)
                this::comstloop,         // 0xf9(6)
                this::comedloop,         // 0xf8(7)
                this::comexloop,         // 0xf7(8)
                this::comlopset,         // 0xf6(9)
                this::comshift,          // 0xf5(10)
                this::comvolup,          // 0xf4(11)
                this::comvoldown,        // 0xf3(12)
                this::lfoset,            // 0xf2(13)
                this::lfoswitch_f,       // 0xf1(14)
                this::jump4,             // 0xf0(15)
                this::comy,              // 0xef(16)
                this::jump1,             // 0xee(17)
                this::jump1,             // 0xed(18)
                // FOR SB2
                this::panset,            // 0xec(19)
                this::rhykey,            // 0xeb(20)
                this::rhyvs,             // 0xea(21)
                this::rpnset,            // 0xe9(22)
                this::rmsvs,             // 0xe8(23)
                // added for V2.0
                this::comshift2,         // 0xe7(24)
                this::rmsvs_sft,         // 0xe6(25)
                this::rhyvs_sft,         // 0xe5(26)
                //
                this::hlfo_delay,        // 0xe4(27)
                // added for V2.3
                this::comvolup2,         // 0xe3(28)
                this::comvoldown2,       // 0xe2(29)
                // added for V2.4
                this::hlfo_set,          // 0xe1(30)
                this::hlfo_onoff,        // 0xe0(31)
                //
                this::syousetu_lng_set,  // 0xdf(32)
                //
                this::vol_one_up_fm,     // 0xde(33)
                this::vol_one_down,      // 0xdd(34)
                //
                this::status_write,      // 0xdc(35)
                this::status_add,        // 0xdb(36)
                //
                this::porta,             // 0xda(37)
                //
                this::jump1,             // 0xd9(38)
                this::jump1,             // 0xd8(39)
                this::jump1,             // 0xd7(40)
                //
                this::mdepth_set,        // 0xd6(41)
                //
                this::comdd,             // 0xd5(42)
                //
                this::ssg_efct_set,      // 0xd4(43)
                this::fm_efct_set,       // 0xd3(44)
                this::fade_set,          // 0xd2(45)
                //
                this::jump1,             // 0xd1(46)
                //
                this::jump1,             // 0xd0(47)
                //
                this::slotmask_set,      // 0xcf(48)
                this::jump6,             // 0xce(49)
                this::jump5,             // 0xcd(50)
                this::jump1,             // 0xcc(51)
                this::lfowave_set,       // 0xcb(52)
                this::lfo_extend,        // 0xca(53)
                this::jump1,             // 0xc9(54)
                this::slotdetune_set,    // 0xc8(55)
                this::slotdetune_set2,   // 0xc7(56)
                this::fm3_extpartset,    // 0xc6(57)
                this::volmask_set,       // 0xc5(58)
                this::comq2,             // 0xc4(59)
                this::panset_ex,         // 0xc3(60)
                this::lfoset_delay,      // 0xc2(61)
                this::jump0,             // 0xc1(62), slurs
                this::fm_mml_part_mask,  // 0xc0(63)
                this::_lfoset,           // 0xbf(64)
                this::_lfoswitch_f,      // 0xbe(65)
                this::_mdepth_set,       // 0xbd(66)
                this::_lfowave_set,      // 0xbc(67)
                this::_lfo_extend,       // 0xbb(68)
                this::_volmask_set,      // 0xba(69)
                this::_lfoset_delay,     // 0xb9(70)
                this::tl_set,            // 0xb8(71)
                this::mdepth_count,      // 0xb7(72)
                this::fb_set,            // 0xb6(73)
                this::slot_delay,        // 0xb5(74)
                this::jump16,            // 0xb4(75)
                this::comq3,             // 0xb3(76)
                this::comshift_master,   // 0xb2(77)
                this::comq4              // 0xb1(78)
        };
    }

    //com_end equ 0b1h

    private Supplier<Object>[] cmdtblp;

    private void setupCmdtblp() {
        cmdtblp = new Supplier[] {
                this::jump1,              // (0xff) 0
                this::comq,               // (0xfe) 1
                this::comv,               // (0xfd) 2
                this::comt,               // (0xfc) 3
                this::comtie,             // (0xfb) 4
                this::comd,               // (0xfa) 5
                this::comstloop,          // (0xf9) 6
                this::comedloop,          // (0xf8) 7
                this::comexloop,          // (0xf7) 8
                this::comlopset,          // (0xf6) 9
                this::comshift,           // (0xf5) 10
                this::comvolupp,          // (0xf4) 11
                this::comvoldownp,        // (0xf3) 12
                this::lfoset,             // (0xf2) 13
                this::lfoswitch,          // (0xf1) 14
                this::psgenvset,          // (0xf0) 15
                this::comy,               // (0xef) 16
                this::psgnoise,           // (0xee) 17
                this::psgsel,             // (0xed) 18
                //
                this::jump1,              // (0xec) 19
                this::rhykey,             // (0xeb) 20
                this::rhyvs,              // (0xea) 21
                this::rpnset,             // (0xe9) 22
                this::rmsvs,              // (0xe8) 23
                //
                this::comshift2,          // (0xe7) 24
                this::rmsvs_sft,          // (0xe6) 25
                this::rhyvs_sft,          // (0xe5) 26
                //
                this::jump1,              // (0xe4) 27
                // added for V2.3
                this::comvolupp2,         // 0E3H 28
                this::comvoldownp2,       // 0E2H 29
                //
                this::jump1,              // 0E1H 30
                this::jump1,              // 0E0H 31
                //
                this::syousetu_lng_set,   // 0DFH 32
                //
                this::vol_one_up_psg,     // 0DEH 33
                this::vol_one_down,       // 0DDH 34
                //
                this::status_write,       // 0DCH 35
                this::status_add,         // 0DBH 36
                //
                this::portap,             // 0DAH 37
                //
                this::jump1,              // 0D9H 38
                this::jump1,              // 0D8H 39
                this::jump1,              // 0D7H 40
                //
                this::mdepth_set,         // 0D6H 41
                //
                this::comdd,              // 0d5h 42
                //
                this::ssg_efct_set,       // 0d4h 43
                this::fm_efct_set,        // 0d3h 44
                this::fade_set,           // 0d2h 45
                //
                this::jump1,              // (0xd1) 46
                this::psgnoise_move,      // 0d0h 47
                //
                this::jump1,              // (0xcf) 48
                this::jump6,              // 0ceh 49
                this::extend_psgenvset,   // 0cdh 50
                this::detune_extend,      // 0cch 51
                this::lfowave_set,        // 0cbh 52
                this::lfo_extend,         // 0cah 53
                this::envelope_extend,    // 0c9h 54
                this::jump3,              // 0c8h 55
                this::jump3,              // 0c7h 56
                this::jump6,              // 0c6h 57
                this::jump1,              // 0c5h 58
                this::comq2,              // 0c4h 59
                this::jump2,              // 0c3h 60
                this::lfoset_delay,       // 0c2h 61
                this::jump0,              // 0c1h, slurs 62
                this::ssg_mml_part_mask,  // 0c0h 63
                this::_lfoset,            // 0bfh 64
                this::_lfoswitch,         // 0beh 65
                this::_mdepth_set,        // 0bdh 66
                this::_lfowave_set,       // 0bch 67
                this::_lfo_extend,        // 0bbh 68
                this::jump1,              // 0bah 69
                this::_lfoset_delay,      // 0b9h 70
                this::jump2,              // 0b8h 71
                this::mdepth_count,       // 0b7h 72
                this::jump1,
                this::jump2,
                this::jump16,             // 0b4h
                this::comq3,              // 0b3h
                this::comshift_master,    // 0b2h
                this::comq4               // 0b1h
        };
    }

    private Supplier<Object>[] cmdtblr;

    private void setupCmdtblr() {
        cmdtblr = new Supplier[] {
                this::jump1,                 // 0xff 0
                this::jump1,                 // 0xfe 1
                this::comv,                  // 0xfd 2
                this::comt,                  // 0xfc 3
                this::comtie,                // 0xfb 4
                this::comd,                  // 0xfa 5
                this::comstloop,             // 0xf9 6
                this::comedloop,             // 0xf8 7
                this::comexloop,             // 0xf7 8
                this::comlopset,             // 0xf6 9
                this::jump1,                 // 0xf5 10
                this::comvolupp,             // 0xf4 11
                this::comvoldownp,           // 0xf3 12
                this::jump4,                 // 0xf2 13
                this::pdrswitch,             // 0xf1 14
                this::jump4,                 // 0xf0 15
                this::comy,                  // 0xef 16
                this::jump1,                 // 0xee 17
                this::jump1,                 // 0xed 18
                //
                this::jump1,                 // 0xec 19
                this::rhykey,                // 0xeb 20
                this::rhyvs,                 // 0xea 21
                this::rpnset,                // 0xe9 22
                this::rmsvs,                 // 0xe8 23
                //
                this::jump1,                 // 0xe7 24
                this::rmsvs_sft,             // 0xe6 25
                this::rhyvs_sft,             // 0xe5 26
                //
                this::jump1,                 // 0E4H 27
                //
                this::comvolupp2,            // 0E3H 28
                this::comvoldownp2,          // 0E2H 29
                //
                this::jump1,                 // 0E1H 30
                this::jump1,                 // 0E0H 31
                //
                this::syousetu_lng_set,      // 0DFH 32
                //
                this::vol_one_up_psg,        // 0DEH 33
                this::vol_one_down,          // 0DDH 34
                //
                this::status_write,          // 0DCH 35
                this::status_add,            // 0DBH 36
                //
                this::jump1,                 // Portamento = Normal pitch command 0xda 37
                //
                this::jump1,                 // 0D9H 38
                this::jump1,                 // 0D8H 39
                this::jump1,                 // 0D7H 40
                //
                this::jump2,                 // 0D6H 41
                //
                this::comdd,                 // 0d5h 42
                //
                this::ssg_efct_set,          // 0d4h 43
                this::fm_efct_set,           // 0d3h 44
                this::fade_set,              // 0d2h 45
                //
                this::jump1,                 // 0xd1 46
                this::jump1,                 // 0d0h 47
                //
                this::jump1,                 // 0xcf 48
                this::jump6,                 // 0ceh 49
                this::jump5,                 // 0cdh 50
                this::jump1,                 // 0cch 51
                this::jump1,                 // 0xcb 52
                this::jump1,                 // 0xca 53
                this::jump1,                 // 0xc9 54
                this::jump3,                 // 0xc8 55
                this::jump3,                 // 0xc7 56
                this::jump6,                 // 0xc6 57
                this::jump1,                 // 0c5h 58
                this::jump1,                 // 0xc4 59
                this::jump2,                 // 0c3h 60
                this::jump1,                 // 0xc2 61
                this::jump0,                 // 0c1h, slurs 62
                this::rhythm_mml_part_mask,  // 0c0h 63
                this::jump4,                 // 0bfh 64
                this::jump1,                 // 0beh 65
                this::jump2,                 // 0bdh 66
                this::jump1,                 // 0bch 67
                this::jump1,                 // 0bbh 68
                this::jump1,                 // 0bah 69
                this::jump1,                 // 0b9h 70
                this::jump2,                 // 0xb8 71
                this::jump1,                 // 0xb7 72
                this::jump1,                 // 0xb6 73
                this::jump2,                 // 0xb5 74
                this::jump16,                // 0b4h 75
                this::jump1,                 // 0xb3 76
                this::jump1,                 // 0b2h 77
                this::jump1,                 // 0b1h 78
        };
    }

    public Supplier<Object> jump16() {
        logger.log(Level.TRACE, "jump16");

        r.addSi((short) 16);
        return null;
    }

    public Supplier<Object> jump6() {
        logger.log(Level.TRACE, "jump6");

        r.addSi((short) 6);
        return null;
    }

    private Supplier<Object> jump5() {
        logger.log(Level.TRACE, "jump5");

        r.addSi((short) 5);
        return null;
    }

    public Supplier<Object> jump4() {
        logger.log(Level.TRACE, "jump4");

        r.addSi((short) 4);
        return null;
    }

    public Supplier<Object> jump3() {
        logger.log(Level.TRACE, "jump3");

        r.addSi((short) 3);
        return null;
    }

    public Supplier<Object> jump2() {
        logger.log(Level.TRACE, "jump2");

        r.addSi((short) 2);
        return null;
    }

    public Supplier<Object> jump1() {
        logger.log(Level.TRACE, "jump1");

        r.incSi();
        return null;
    }

    public Supplier<Object> jump0() {
        logger.log(Level.TRACE, "jump0");

        return null;
    }

    /**
     * Additional special instructions for 0c0h
     */
    public Supplier<Object> special_0c0h() {
        if ((r.al & 0xff) < (PW.com_end_0c0h & 0xff)) {
            return this::out_of_commands;
        }

        r.al = (byte) ~r.al;
        r.al += r.al;
        r.ah = 0;
        r.setBx(r.getAx());

        return comtbl0c0h[(r.getBx() & 0xffff) / 2];
    }

    private Supplier<Object>[] comtbl0c0h;

    private void setupComtbl0c0h() {
        comtbl0c0h = new Supplier[] {
                this::vd_fm,  // 0ffh
                this::_vd_fm,
                this::vd_ssg,
                this::_vd_ssg,
                this::vd_pcm,
                this::_vd_pcm,
                this::vd_rhythm,
                this::_vd_rhythm,  // 0f8h
                this::pmd86_s,
                this::vd_ppz,
                this::_vd_ppz  // 0f5h
        };
    }

    /**
     * /s Option control
     */
    private Supplier<Object> pmd86_s() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 1;
        pw.pcm86_vol = r.al;
        return null;
    }

    /**
     * Various Voldowns
     */
    private Supplier<Object> vd_fm() {
        r.setBx((short) 0); // offset fm_voldown

//vd_main:;
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.fm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_ssg() {
        r.setBx((short) 0); // offset ssg_voldown
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.ssg_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_pcm() {
        r.setBx((short) 0); // offset pcm_voldown
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.pcm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_rhythm() {
        r.setBx((short) 0); // offset rhythm_voldown
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.rhythm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_ppz() {
        r.setBx((short) 0); // offset ppz_voldown
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.ppz_voldown = r.al;
        return null;
    }

    private Supplier<Object> _vd_fm() {
        byte[] tmp = new byte[1];
        _vd_main(/* ref */ tmp, pw._fm_voldown);
        pw.fm_voldown = tmp[0];
        return null;
    }

    private void _vd_main(/* ref */ byte[] a, byte b) {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (r.al != 0) { // break _vd_reset;
            if ((r.al & 0x80) == 0) { // break _vd_sign;

                r.carry = (a[0] & 0xff) + (r.al & 0xff) > 0xff;
                a[0] += r.al;
                if (!r.carry) { // break _vd_ret;
                    return;
                }
                a[0] = (byte) 255;
//_vd_ret:
                return;
            }
//_vd_sign:
            r.carry = (a[0] & 0xff) + (r.al & 0xff) > 0xff;
            a[0] += r.al;
            if (r.carry) { // break _vd_ret;
                return;
            }
            a[0] = 0;
            return;
        }
//_vd_reset:
        a[0] = b;
    }

    private Supplier<Object> _vd_ssg() {
        byte[] tmp = new byte[1];
        _vd_main(/* ref */ tmp, pw._ssg_voldown);
        pw.ssg_voldown = tmp[0];
        return null;
    }

    private Supplier<Object> _vd_pcm() {
        byte[] tmp = new byte[1];
        _vd_main(/* ref */ tmp, pw._pcm_voldown);
        pw.pcm_voldown = tmp[0];
        return null;
    }

    private Supplier<Object> _vd_rhythm() {
        byte[] tmp = new byte[1];
        _vd_main(/* ref */ tmp, pw._rhythm_voldown);
        pw.rhythm_voldown = tmp[0];
        return null;
    }

    private Supplier<Object> _vd_ppz() {
        byte[] tmp = new byte[1];
        _vd_main(/* ref */ tmp, pw._ppz_voldown);
        pw.ppz_voldown = tmp[0];
        return null;
    }

    /**
     * slot keyon delay
     */
    private Supplier<Object> slot_delay() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0xf;
        r.al ^= 0xf;
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        pw.partWk[r.di & 0xffff].sdelay_m = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].sdelay = r.al;
        pw.partWk[r.di & 0xffff].sdelay_c = r.al;
        return null;
    }

    /**
     * FB Change
     */
    private Supplier<Object> fb_set() {
        r.dh = (byte) (0xb0 - 1);
        r.dh += pw.partb; // dh=ALG/FB port address
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0x80) == 0) { // break _fb_set;
//fb_set2:
            //  ;in al 00000xxx Function block to be set
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
        }
//fb_set3:
        while (true) {
            //  ;in al 00xxx000 Function block to be set
            if ((r.al & 0x80) == 0) { // break _fb_set; // <<
fb_notfm3:
                if (pw.partb == 3) { // break fb_notfm3;
                    if (pw.board2 != 0) {
                        if (pw.fmsel != 0)
                            break fb_notfm3;
                    } else {
                        if (r.di == pw.part_e) // offset part_e
                            break fb_notfm3;
                    }
                    if ((pw.partWk[r.di & 0xffff].slotmask & 0x10) == 0) { // If you are not using slot 1
//                    break fb_ret; // No output
                        return null;
                    }
                    r.dl = pw.fm3_alg_fb;
                    r.dl &= 7;
                    r.dl |= r.al;
                    pw.fm3_alg_fb = r.dl;
//                break fb_exit;
                } else {
//fb_notfm3:
                    r.dl = pw.partWk[r.di & 0xffff].alg_fb;
                    r.dl &= 0x7;
                    r.dl |= r.al;
                }
//fb_exit:
                opnset();
                pw.partWk[r.di & 0xffff].alg_fb = r.dl;
//fb_ret:
                return null;
            }
//_fb_set:
            if ((r.al & 0b0100_0000) == 0) { // break _fb_sign;
                r.al &= 7;
            }
//_fb_sign:
_fb_notfm3:
            if (pw.partb == 3) { // break _fb_notfm3;

                if (pw.board2 != 0) {
                    if (pw.fmsel != 0)
                        break _fb_notfm3;
                } else {
                    if (r.di == pw.part_e) // offset part_e
                        break _fb_notfm3;
                }
                r.dl = pw.fm3_alg_fb;
//                break _fb_next;
            } else {
//_fb_notfm3:
                r.dl = pw.partWk[r.di & 0xffff].alg_fb;
            }
//_fb_next:
            r.dl = r.rol(r.dl, 1);
            r.dl = r.rol(r.dl, 1);
            r.dl = r.rol(r.dl, 1);
            r.dl &= 7;
            r.al += r.dl;
            if ((r.al & 0x80) == 0) { // break _fb_zero;
                if ((r.al & 0xff) < 8) {
//                    break fb_set2;
//fb_set2:
                    //  ;in al 00000xxx Function block to be set
                    r.al = r.rol(r.al, 1); // <<
                    r.al = r.rol(r.al, 1); // <<
                    r.al = r.rol(r.al, 1); // <<
                    continue;
                }
                r.al = 0b0011_1000;
                continue; // break fb_set3;
            }
//_fb_zero:
            r.al = 0;
        } // break fb_set3;
    }

    /**
     * TL Change
     */
    private Supplier<Object> tl_set() {
        r.dh = 0x40 - 1;
        r.dh += pw.partb; // dh=TL FM Port Address
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.ah &= 0xf;
        r.ch = pw.partWk[r.di & 0xffff].slotmask; // ch=slotmask 43210000
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ah &= r.ch; // ah = slot to change 00004321
        r.dl = (byte) pw.md[r.getSi() & 0xffff].dat; // dl=change value
        r.incSi();
        r.setBx((short) 0); // offset opnset
        if (pw.partWk[r.di & 0xffff].partmask != 0) { // Are the parts masked? // break ts_00;
            r.setBx((short) 1); // offset dummy_ret
        }
//ts_00:
        if ((r.al & 0x80) == 0) { // break tl_slide;
            r.dl &= 127;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_01;
                pw.partWk[r.di & 0xffff].slot1 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_01:
            r.dh += 8;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_02;
                pw.partWk[r.di & 0xffff].slot2 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_02:
            r.dh -= 4;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_03;
                pw.partWk[r.di & 0xffff].slot3 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_03:
            r.dh += 8;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_04;
                pw.partWk[r.di & 0xffff].slot4 = r.dl;
                if (r.getBx() == 0) opnset();
//dummy_ret:
            }
//ts_04:
            return null;
        }
        // Relative Change
//tl_slide:
        r.al = r.dl;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_01;
            r.dl = pw.partWk[r.di & 0xffff].slot1;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_0b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_0b;
                    r.dl = 127;
                }
            }
//tls_0b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di & 0xffff].slot1 = r.dl;
        }
//tls_01:
        r.dh += 8;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_02;
            r.dl = pw.partWk[r.di & 0xffff].slot2;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_1b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_1b;
                    r.dl = 127;
                }
            }
//tls_1b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di & 0xffff].slot2 = r.dl;
        }
//tls_02:
        r.dh -= 4;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_03;
            r.dl = pw.partWk[r.di & 0xffff].slot3;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_2b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_2b;
                    r.dl = 127;
                }
            }
//tls_2b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di & 0xffff].slot3 = r.dl;
        }
//tls_03:
        r.dh += 8;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_04;
            r.dl = pw.partWk[r.di & 0xffff].slot4;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_3b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { //break tls_3b;
                    r.dl = 127;
                }
            }
//tls_3b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di & 0xffff].slot4 = r.dl;
        }
//tls_04:
        return null;
    }

    /**
     * Mask on/off for playing part
     */
    private Supplier<Object> fm_mml_part_mask() {
        logger.log(Level.TRACE, "fm_mml_part_mask");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break fm_mml_part_maskoff;

            pw.partWk[r.di & 0xffff].partmask |= 0x40;
            if (pw.partWk[r.di & 0xffff].partmask == 0x40) { // break fmpm_ret;

                silence_fmpart(); // Sound Cancellation
            }
//fmpm_ret:
            //r.ax = r.stack.pop(); // commands
            return fmmnp_1Ref; // Move to part mask processing
        }
//fm_mml_part_maskoff:

        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
//            break fmpm_ret;
            return fmmnp_1Ref; // Move to part mask processing // <<
        }
        neiro_reset(); // Resetting the tone
        //r.ax = r.stack.pop(); // commands
        return mp1Ref; // revival the part
    }

    private Supplier<Object> ssg_mml_part_mask() {
        logger.log(Level.TRACE, "ssg_mml_part_mask");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break ssg_part_maskoff_ret;

            pw.partWk[r.di & 0xffff].partmask |= 0x40;
            if (pw.partWk[r.di & 0xffff].partmask == 0x40) { // break smpm_ret;

                psgmsk(); // AL=07h AH = Maskdata
                r.dh = 7;
                r.dl = r.al;
                r.dl |= r.ah;
                opnset44(); // PSG keyoff
            }
//smpm_ret:

            //r.ax = r.stack.pop(); // commandsp
            return psgmnp_1Ref;
        }
//ssg_part_maskoff_ret:

        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
//            break smpm_ret;
            return psgmnp_1Ref; // <<
        }
        //r.ax = r.stack.pop(); // commandsp
        return mp1pRef; // revival the part
    }

    private Supplier<Object> rhythm_mml_part_mask() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break rhythm_part_maskoff_ret;

            pw.partWk[r.di & 0xffff].partmask |= 0x40;
            return null;
        }
//rhythm_part_maskoff_ret:
        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xbf;
        return null;
    }

    /**
     * Reconfiguring the FM sound source
     */
    private void neiro_reset() {
        if (pw.partWk[r.di & 0xffff].neiromask != 0) { // break nr_ret;

            r.dl = pw.partWk[r.di & 0xffff].voicenum;
            r.bl = pw.partWk[r.di & 0xffff].slot1; //    mov bx, word ptr slot1[di]; bh=s3 bl = s1
            r.bh = pw.partWk[r.di & 0xffff].slot3;
            r.cl = pw.partWk[r.di & 0xffff].slot2; //    mov cx, word ptr slot2[di]; ch=s4 cl = s2
            r.ch = pw.partWk[r.di & 0xffff].slot4;
            r.stack.push(r.getBx());
            r.stack.push(r.getCx());
            pw.af_check = 1;
            neiroset(); // Tone recovery
            pw.af_check = 0;
            r.setCx(r.stack.pop());
            r.setBx(r.stack.pop());
            pw.partWk[r.di & 0xffff].slot1 = r.bl;
            pw.partWk[r.di & 0xffff].slot3 = r.bh;
            pw.partWk[r.di & 0xffff].slot2 = r.cl;
            pw.partWk[r.di & 0xffff].slot4 = r.ch;
            r.al = pw.partWk[r.di & 0xffff].carrier;
            r.al = (byte) ~r.al;
            r.al &= pw.partWk[r.di & 0xffff].slotmask; // al<- Slots where you can reset TL 4321xxxx
            if (r.al != 0) { // break nr_exit;
                r.dh = 0x4c - 1;
                r.dh += pw.partb; // dh=TL FM Port Address
                r.al = r.rol(r.al, 1);
                if (r.carry) { // break nr_s3;
                    r.dl = r.ch; // slot 4
                    opnset();
                }
//nr_s3:
                r.dh -= 8;
                r.al = r.rol(r.al, 1);
                if (r.carry) { // break nr_s2;
                    r.dl = r.bh; // slot 3
                    opnset();
                }
//nr_s2:
                r.dh += 4;
                r.al = r.rol(r.al, 1);
                if (r.carry) { // break nr_s1;
                    r.dl = r.cl; // slot 2
                    opnset();
                }
//nr_s1:
                r.dh -= 8;
                r.al = r.rol(r.al, 1);
                if (r.carry) { // break nr_exit;
                    r.dl = r.bl; // slot 1
                    opnset();
                }
            }
//nr_exit:
            if (pw.board2 != 0) {
                r.dh = pw.partb;
                r.dh += (byte) (0xb4 - 1);
                calc_panout();
                opnset(); // restore Pan
            }
        }
//nr_ret:
    }

    /**
     * PDR switch
     */
    private Supplier<Object> pdrswitch() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.ppsdrv_flag != 0) { // break pdrsw_ret;

            r.dl = r.al;
            r.dl &= 1;
            r.al = (byte) ((r.al & 0xff) >>> 1);
            r.ah = 5;
            ChipDatum cd = new ChipDatum(0x03, r.al & 0xff, r.dl & 0xff);
            ppsdrv.apply(cd); // .SetParam(r.al, r.dl); // int ppsdrv
        }
//pdrsw_ret:
        return null;
    }

    /**
     * Setting the volume mask slot
     */
    private Supplier<Object> volmask_set() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0xf;
        if (r.al != 0) { // break vms_zero;

            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1); // Move to the top 4 bits

            r.al |= 0xf; // If a value other than 0 is specified, the lower 4 bits are set to 1.
            pw.partWk[r.di & 0xffff].volmask = r.al;
            return this::ch3_setting;
        }
//vms_zero:
        r.al = pw.partWk[r.di & 0xffff].carrier;
        pw.partWk[r.di & 0xffff].volmask = r.al; // Set carrier position

        return this::ch3_setting;
    }

    public Supplier<Object> _volmask_set() {
        logger.log(Level.TRACE, "_volmask_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0xf;
        if (r.al != 0) { // break _vms_zero;

            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1); // Move to the top 4 bits
            r.al |= 0xf; // If a value other than 0 is specified, the lower 4 bits are set to 1.
            pw.partWk[r.di & 0xffff]._volmask = r.al;
            return this::ch3_setting;
        }
//_vms_zero:
        r.al = pw.partWk[r.di & 0xffff].carrier;
        pw.partWk[r.di & 0xffff]._volmask = r.al; // Set carrier position

        return this::ch3_setting;
    }

    /**
     * Identify the part and set the mode if it is ch3
     */
    private Supplier<Object> ch3_setting() {
vms_not_p3: // ↑
        if (pw.partb == 3) { // break vms_not_p3;

            if (pw.board2 != 0) {
                if (pw.fmsel != 0)
                    break vms_not_p3;
            } else {
                if (r.di == pw.part_e)
                    break vms_not_p3;
            }

            ch3mode_set(); // Change of ch3mode only for FM3ch

            r.carry = true;
            return null;
        }
//vms_not_p3:
        r.carry = false;
        return null;
    }

    /**
     * FM3ch extended part set
     */
    private Supplier<Object> fm3_extpartset() {
        logger.log(Level.TRACE, "fm3_extpartset");

        r.stack.push(r.di);

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        if (r.getAx() != 0) { // break fm3ext_part3c;
            r.addAx((short) pw.mmlbuf);
            r.di = (short) pw.part3b; // offset part3b
            fm3_partinit();
        }
//fm3ext_part3c:

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        if (r.getAx() != 0) { // break fm3ext_part3d;
            r.addAx((short) pw.mmlbuf);
            r.di = (short) pw.part3c; // offset part3c
            fm3_partinit();
        }
//fm3ext_part3d:

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        if (r.getAx() != 0) { // break fm3ext_exit;

            r.addAx((short) pw.mmlbuf);
            r.di = (short) pw.part3d; // offset part3d
            fm3_partinit();
        }
//fm3ext_exit:
        r.di = r.stack.pop();
        return null;
    }

    private void fm3_partinit() {
        pw.partWk[r.di & 0xffff].address = r.getAx();
        pw.partWk[r.di & 0xffff].leng = 1; // Playing starts in 1 count
        r.al = (byte) 0xff; // -1
        pw.partWk[r.di & 0xffff].keyoff_flag = r.al; // Currently being keyed off
        pw.partWk[r.di & 0xffff].mdc = r.al; // MDepth Counter(infinite)
        pw.partWk[r.di & 0xffff].mdc2 = r.al;
        pw.partWk[r.di & 0xffff]._mdc = r.al;
        pw.partWk[r.di & 0xffff]._mdc2 = r.al;
        pw.partWk[r.di & 0xffff].onkai = r.al; // rest
        pw.partWk[r.di & 0xffff].onkai_def = r.al; // rest
        pw.partWk[r.di & 0xffff].volume = 108; // FM VOLUME DEFAULT= 108
        r.setBx((short) pw.part3); // offset part3
        r.al = pw.partWk[r.getBx() & 0xffff].fmpan;
        pw.partWk[r.di & 0xffff].fmpan = r.al; // FM PAN = Same as CH3
        pw.partWk[r.di & 0xffff].partmask |= 0x20; // partmask for s0
    }

    /**
     * Detune Extend Set
     */
    private Supplier<Object> detune_extend() {
        logger.log(Level.TRACE, "detune_extend");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 1;
        pw.partWk[r.di & 0xffff].extendmode &= (byte) 0xfe;
        pw.partWk[r.di & 0xffff].extendmode |= r.al;
        return null;
    }

    /**
     * LFO Extend Set
     */
    public Supplier<Object> lfo_extend() {
        logger.log(Level.TRACE, "lfo_extend");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 1;
        r.al <<= 1;

        pw.partWk[r.di & 0xffff].extendmode &= (byte) 0xfd;
        pw.partWk[r.di & 0xffff].extendmode |= r.al;
        return null;
    }

    /**
     * Envelope Extend Set
     */
    public Supplier<Object> envelope_extend() {
        logger.log(Level.TRACE, "envelope_extend");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 1;
        r.al <<= 1;
        r.al <<= 1;

        pw.partWk[r.di & 0xffff].extendmode &= (byte) 0xfb;
        pw.partWk[r.di & 0xffff].extendmode |= r.al;
        return null;
    }

    /**
     * LFO Wave Selection
     */
    public Supplier<Object> lfowave_set() {
        logger.log(Level.TRACE, "lfowave_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].lfo_wave = r.al;

        return null;
    }

    /**
     * PSG Envelope set(Extend)
     */
    public Supplier<Object> extend_psgenvset() {
        logger.log(Level.TRACE, "extend_psgenvset");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0x1f;
        pw.partWk[r.di & 0xffff].eenv_ar = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0x1f;
        pw.partWk[r.di & 0xffff].eenv_dr = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0x1f;
        pw.partWk[r.di & 0xffff].eenv_sr = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;

        r.al &= 0x0f;
        pw.partWk[r.di & 0xffff].eenv_rr = r.al;

        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);

        r.ah &= 0xf;
        r.ah ^= 0xf;
        pw.partWk[r.di & 0xffff].eenv_sl = r.ah;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 0x0f;
        pw.partWk[r.di & 0xffff].eenv_al = r.al;

        if (pw.partWk[r.di & 0xffff].envf != (byte) 0xff) { // break not_set_count; // Has it gone from Normal to Extended?

            pw.partWk[r.di & 0xffff].envf = (byte) 0xff;

            pw.partWk[r.di & 0xffff].eenv_count = 4; // RR
            pw.partWk[r.di & 0xffff].eenv_volume = 0; // Volume
        }
//not_set_count:

        return null;
    }

    /**
     * Slot Detune Set(relative)
     */
    private Supplier<Object> slotdetune_set2() {
        logger.log(Level.TRACE, "slotdetune_set2");

        if (pw.partb != 3) // Only FM3rd channel can be specified
            return this::jump3;
        if (pw.board2 != 0) {
            if (pw.fmsel == 1) // It cannot be specified back
                return this::jump3;
        }

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.bl = r.al;
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);

        r.bl = r.ror(r.bl, 1);
        if (r.carry) { // break sds2_slot2;
            pw.slot_detune1 += r.getAx();
        }
//sds2_slot2:
        r.bl = r.ror(r.bl, 1);
        if (r.carry) { // break sds2_slot3;
            pw.slot_detune2 += r.getAx();
        }
//sds2_slot3:
        r.bl = r.ror(r.bl, 1);
        if (r.carry) { // break sds2_slot4;
            pw.slot_detune3 += r.getAx();
        }
//sds2_slot4:
        r.bl = r.ror(r.bl, 1);
        if (!r.carry) return this::sds_check;
        pw.slot_detune4 += r.getAx();
        return this::sds_check;
    }

    /**
     * Slot Detune Set
     */
    private Supplier<Object> slotdetune_set() {
        logger.log(Level.TRACE, "slotdetune_set");

        if (pw.partb != 3) // Only FM3rd channel can be specified
            return this::jump3;
        if (pw.board2 != 0) {
            if (pw.fmsel == 1) // It cannot be specified back
                return this::jump3;
        } else {
            if ((r.di & 0xffff) == pw.part_e)
                return this::jump3;
        }

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.bl = r.al;
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
        r.addSi((short) 2);

        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) ((((r.bl & 0xff) >>> 1) & 0x7f) | ((r.bl & 0xff) << 7));
        if (r.carry) { // break sds_slot2;
            pw.slot_detune1 = r.getAx();
        }
//sds_slot2:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) ((((r.bl & 0xff) >>> 1) & 0x7f) | ((r.bl & 0xff) << 7));
        if (r.carry) { // break sds_slot3;
            pw.slot_detune2 = r.getAx();
        }
//sds_slot3:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) ((((r.bl & 0xff) >>> 1) & 0x7f) | ((r.bl & 0xff) << 7));
        if (r.carry) { // break sds_slot4;
            pw.slot_detune3 = r.getAx();
        }
//sds_slot4:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) ((((r.bl & 0xff) >>> 1) & 0x7f) | ((r.bl & 0xff) << 7));
        if (!r.carry) return this::sds_check;
        pw.slot_detune4 = r.getAx();
        return this::sds_check;
    }

    private Supplier<Object> sds_check() {
        r.setAx(pw.slot_detune1);
        r.orAx(pw.slot_detune2);
        r.orAx(pw.slot_detune3);
        r.orAx(pw.slot_detune4); // Are they all 0?
        if (r.getAx() != 0) { // break sdf_set;
            r.al = 1;
        }
//sdf_set:
        pw.slotdetune_flag = r.al;
        ch3mode_set();
        return null;
    }

    /**
     * Set the FM3 mode
     */
    private void ch3mode_set() {
        r.al = 1;
        if (r.di != pw.part3) { // break cmset_00;
            r.al++;
            if (r.di != pw.part3b) { // break cmset_00;
                r.al = 4;
                if (r.di != pw.part3c) { // break cmset_00;
                    r.al = 8;
                }
            }
        }
//cmset_00:
cm_set_main: // ↑
        {
cm_set2: // ↑
            {
cm_set: // ↑
                {
cm_clear: // ↑
                    {
                        if ((pw.partWk[r.di & 0xffff].slotmask & 0xf0) != 0) { //s0 // break cm_clear;
                            if (pw.partWk[r.di & 0xffff].slotmask != (byte) 0xf0)
                                break cm_set;
                            if ((pw.partWk[r.di & 0xffff].volmask & 0x0f) == 0)
                                break cm_clear;
                            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x1) != 0)
                                break cm_set;

                            //cm_noset1:;
                            if ((pw.partWk[r.di & 0xffff]._volmask & 0x0f) == 0)
                                break cm_clear;
                            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x10) != 0)
                                break cm_set;
                        }
                    }
//cm_clear:
                    r.al ^= (byte) 0xff;
                    pw.slot3_flag &= r.al;
                    if (pw.slot3_flag != 0)
                        break cm_set2;

                    //cm_clear2:;
                    if (pw.slotdetune_flag == 1)
                        break cm_set2;
                    r.ah = 0x3f;
                    break cm_set_main;
                }
//cm_set:
                pw.slot3_flag |= r.al;
            }
//cm_set2:
            r.ah = 0x7f;
        }
//cm_set_main:
        if (pw.board2 == 0) {
            if ((pw.partWk[r.di & 0xffff].partmask & 2) != 0) { // Is the effect/part masked?
                cm_nowefcplaying();
                return;
            }
        }

        if (r.ah != pw.ch3mode) { // break cm_exit; // If there is no change from before, do nothing

            pw.ch3mode = r.ah;
            r.dh = 0x27;
            r.dl = r.ah;
            r.dl &= (byte) 0b1100_1111; // Do not reset
            opnset44();

            // When switching to sound effect mode, the pitch is rewritten in the previous FM3 part.
            if (r.ah != 0x3f) { // break cm_exit;
                if (r.di != pw.part3) { // break cm_exit;

//cm_otodasi:
                    r.stack.push(r.bp);
                    r.bp = r.di;
                    r.stack.push(r.di);
                    r.di = (short) pw.part3; // offset part3
                    otodasi_cm();

//cm_3bchk:
                    if (r.bp != pw.part3b) { // break cm_exit2;
                        r.di = (short) pw.part3; // offset part3b
                        otodasi_cm();

//cm_3cchk:
                        if (r.bp != pw.part3c) { // break cm_exit2;
                            r.di = (short) pw.part3c; // offset part3c
                            otodasi_cm();
                        }
                    }
//cm_exit2:
                    r.di = r.stack.pop();
                    r.bp = r.stack.pop();
                }
            }
        }
//cm_exit:
    }

    private void otodasi_cm() {
        if (pw.partWk[r.di & 0xffff].partmask == 0) { // break ocm_ret;
            otodasi();
        }
//ocm_ret:
    }

    private void cm_nowefcplaying() {
        if (pw.board2 == 0) {
            pw.ch3mode_push = r.ah;
        }
    }

    /**
     * FM slotmask set
     */
    private Supplier<Object> slotmask_set() {
        logger.log(Level.TRACE, "slotmask_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.al &= 0xf;
        if (r.al != 0) { // break sm_not_car;

            r.al = (byte) ((r.al << 4) | (((r.al & 0xff) >>> 4) & 0x0f));
            pw.partWk[r.di & 0xffff].carrier = r.al;
//            break sm_set;
        } else {
//sm_not_car:
sm_car_set: // ↑
            {
sm_notfm3: // ↑
                {
                    if (pw.partb != 3)
                        break sm_notfm3;

                    if (pw.board2 != 0) {
                        if (pw.fmsel != 0)
                            break sm_notfm3;
                    } else {
                        if (r.di == pw.part_e)
                            break sm_notfm3;
                    }

                    r.bl = pw.fm3_alg_fb;
                    break sm_car_set;
                }
//sm_notfm3:
                //r.dl = pw.partWk[r.di & 0xffff].voicenum;
                //r.stack.push(r.ax);
                //toneadr_calc();
                //r.ax = r.stack.pop();
                //r.bl = (byte)pw.inst[r.bx + 24].dat;
                r.bl = pw.partWk[r.di & 0xffff].alg_fb;
            }
//sm_car_set:
            r.bh = 0;
            r.bl &= 7;
            r.addBx((short) 0); // offset carrier_table
            r.al = (byte) PW.carrier_table[r.getBx() & 0xffff];
            pw.partWk[r.di & 0xffff].carrier = r.al;
        }
//sm_set:
        r.ah &= (byte) 0xf0;
        if (pw.partWk[r.di & 0xffff].slotmask != r.ah) { // break sm_no_change;
            pw.partWk[r.di & 0xffff].slotmask = r.ah;
            if ((r.ah & 0xf0) == 0) { // break sm_noset_pm;
                pw.partWk[r.di & 0xffff].partmask |= 0x20; // Part mask when s0
//                break sms_ns;
            } else {
//sm_noset_pm:
                pw.partWk[r.di & 0xffff].partmask &= (byte) 0xdf; // Part mask release when not s0
            }
//sms_ns:
            ch3_setting(); // Change of ch3mode only for FM3ch
            if (r.carry) { // break sms_nms;
                // For ch3, keyon is processed in the previous FM3 part.
                if ((r.di & 0xffff) != pw.part3) { // break sm_exit;

                    r.stack.push(r.bp);
                    r.bp = r.di;
                    r.stack.push(r.di);
                    r.di = (short) pw.part3;
                    keyon_sm();

                    //sm_3bchk:;
                    if (r.bp != pw.part3b) { // break sm_exit2;
                        r.di = (short) pw.part3b;
                        keyon_sm();

                        //sm_3cchk:;
                        if (r.bp != pw.part3c) { // break sm_exit2;
                            r.di = (short) pw.part3c;
                            keyon_sm();
                        }
                    }
//sm_exit2:
                    r.di = r.stack.pop();
                    r.bp = r.stack.pop();
                }
//sm_exit:
            }
//sms_nms:
            r.ah = 0;
            r.al = pw.partWk[r.di & 0xffff].slotmask;
            r.al = r.rol(r.al, 1); // slot4
            if (r.carry) { // break sms_n4;
                r.ah |= 0b0001_0001;
            }
//sms_n4:
            r.al = r.rol(r.al, 1); // slot3
            if (r.carry) { // break sms_n3;
                r.ah |= 0b0100_0100;
            }
//sms_n3:
            r.al = r.rol(r.al, 1); // slot2
            if (r.carry) { // break sms_n2;
                r.ah |= 0b0010_0010;
            }
//sms_n2:
            r.al = r.rol(r.al, 1); // slot1
            if (r.carry) { // break sms_n1;
                r.ah |= (byte) 0b1000_1000;
            }
//sms_n1:
            pw.partWk[r.di & 0xffff].neiromask = r.ah;
            //r.bx = r.stack.pop(); // commands
            if (pw.partWk[r.di & 0xffff].partmask == 0)
                return mp1Ref; // restore part
            return fmmnp_1Ref;
        }
//sm_no_change:
        return null;
    }

    private void keyon_sm() {
        if (pw.partWk[r.di & 0xffff].partmask == 0) { // break ksm_ret;
            if ((pw.partWk[r.di & 0xffff].keyoff_flag & 1) == 0) { // Are you in keyon? // break ksm_ret; // During keyoff
                keyon();
            }
        }
//ksm_ret:
    }

    /**
     * ssg effect
     */
    public Supplier<Object> ssg_efct_set() {
        logger.log(Level.TRACE, "ssg_efct_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            return null;

        if (r.al != 0) { // break ses_off;

            r.stack.push(r.getSi());
            r.stack.push(r.di);
            efcdrv.eff_on2();
            r.di = r.stack.pop();
            r.setSi(r.stack.pop());

            //ses_ret:;
            return null;
        }
//ses_off:
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        efcdrv.effoff();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        return null;
    }

    /**
     * fm effect
     */
    public Supplier<Object> fm_efct_set() {
        logger.log(Level.TRACE, "fm_efct_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            return null; // ses_ret;↑

        if (r.al != 0) { // break fes_off;
            if (pw.board2 != 0) {
                r.bh = pw.fmsel;
            }
            r.bl = pw.partb;
            r.stack.push(r.getBx());
            r.stack.push(r.getSi());
            r.stack.push(r.di);
            fm_effect_on();
            r.di = r.stack.pop();
            r.setSi(r.stack.pop());
            r.setAx(r.stack.pop());
            pw.partb = r.al;
            if (pw.board2 != 0) {
                if (r.ah == 0) {
                    sel44();
                    return null;
                }
                sel46();
                return null;
            } else {
                return null;
            }
        }
//fes_off:
        if (pw.board2 != 0) {
            r.bh = pw.fmsel;
        }
        r.bl = pw.partb;
        r.stack.push(r.getBx());
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        fm_effect_off();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        r.setAx(r.stack.pop());
        pw.partb = r.al;
        if (pw.board2 != 0) {
            if (r.ah == 0) {
                sel44();
                return null;
            }
            sel46();
            return null;
        } else {
            return null;
        }
    }

    /**
     * fadeout
     */
    public Supplier<Object> fade_set() {
        logger.log(Level.TRACE, "fade_set");

        pw.fadeout_flag = 1;
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        // KUMA: The fout processing is done here.
        pw.fadeout_speed = r.al;
        return null;
    }

    /**
     * LFO depth +- set
     */
    public Supplier<Object> mdepth_set() {
        logger.log(Level.TRACE, "mdepth_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].mdspd = r.al;
        pw.partWk[r.di & 0xffff].mdspd2 = r.al;
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].mdepth = r.al;

        return null;
    }

    public Supplier<Object> mdepth_count() {
        logger.log(Level.TRACE, "mdepth_count");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al |= r.al;
        if ((r.al & 0x80) == 0) { // break mdc_lfo2;
            if (r.al == 0) { // break mdc_no_deca;
                r.al--; // 255
            }
//mdc_no_deca:
            pw.partWk[r.di & 0xffff].mdc = r.al;
            pw.partWk[r.di & 0xffff].mdc2 = r.al;

            return null;
        }
//mdc_lfo2:
        r.al &= 0x7f;
        if (r.al == 0) { // break mdc_no_decb;
            r.al--; // 255
        }
//mdc_no_decb:
        pw.partWk[r.di & 0xffff]._mdc = r.al;
        pw.partWk[r.di & 0xffff]._mdc2 = r.al;

        return null;
    }

    /**
     * It's a portamento calculation.
     */
    public void porta_calc() {
        r.setAx(pw.partWk[r.di & 0xffff].porta_num2);
        pw.partWk[r.di & 0xffff].porta_num += r.getAx();
        if (pw.partWk[r.di & 0xffff].porta_num3 != 0) { // break pc_ret;
            if ((pw.partWk[r.di & 0xffff].porta_num3 & 0x8000) == 0) { // break pc_minus;

                pw.partWk[r.di & 0xffff].porta_num3--;
                pw.partWk[r.di & 0xffff].porta_num++;
            } else {
//pc_minus:
                pw.partWk[r.di & 0xffff].porta_num3++;
                pw.partWk[r.di & 0xffff].porta_num--;
            }
        }
//pc_ret:
    }

    /**
     * Portamento (FM)
     */
    private Supplier<Object> porta() {
        if (pw.partWk[r.di & 0xffff].partmask == 0) { // break porta_notset;

            ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
            cd.additionalData = pw.cmd;
            WriteOPNARegister.accept(cd);

            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            lfoinit();
            oshift();
            fnumset();
            r.setAx(pw.partWk[r.di & 0xffff].fnum);
            r.stack.push(r.getAx());
            r.al = pw.partWk[r.di & 0xffff].onkai;
            r.stack.push(r.getAx());
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            oshift();
            fnumset();
            r.setBx(pw.partWk[r.di & 0xffff].fnum); // bx = fnum value of portamento destination
            r.setCx(r.stack.pop());
            pw.partWk[r.di & 0xffff].onkai = r.cl;
            r.setCx(r.stack.pop());
            pw.partWk[r.di & 0xffff].fnum = r.getCx(); // cx = fnum value of the portamento source
            r.setAx((short) 0);
            r.stack.push(r.getCx());
            r.stack.push(r.getBx());
            r.ch &= 0x38;
            r.bh &= 0x38;
            r.bh -= r.ch; // Previous octarb - Original octarb
            if (r.bh != 0) { // break not_octarb;
                r.bh = (byte) ((r.bh & 0x80) | (((r.bh & 0xff) >>> 1) & 0x7f));
                r.bh = (byte) ((r.bh & 0x80) | (((r.bh & 0xff) >>> 1) & 0x7f));
                r.bh = (byte) ((r.bh & 0x80) | (((r.bh & 0xff) >>> 1) & 0x7f));
                r.al = r.bh;
                r.setAx(/* signed */ r.al); // ax=octarb difference
                r.setBx((short) 0x26a);
                int ans = (r.getAx() & 0xffff) * (r.getBx() & 0xffff); // (dx) ax = 26ah * octarb difference
                r.setDx((short) (ans >>> 16));
                r.setAx((short) ans);
            }
//not_octarb:
            r.setBx(r.stack.pop());
            r.setCx(r.stack.pop());
            r.andCx((short) 0x7ff);
            r.andBx((short) 0x7ff);
            r.subBx(r.getCx());
            r.addAx(r.getBx()); // ax=26ah * octarb difference + pitch difference
            r.bl = (byte) pw.md[r.getSi() & 0xffff].dat;
            r.incSi();
            pw.partWk[r.di & 0xffff].leng = r.bl;
            calc_q();
            r.bh = 0;
            int src = /* signed */ r.getAx();
            r.setDx((short) (src % /* signed */ r.getBx())); // ax=(26ah * ovtarb difference + pitch difference) / note length
            r.setAx((short) (src / /* signed */ r.getBx()));
            pw.partWk[r.di & 0xffff].porta_num2 = r.getAx(); // quotient
            pw.partWk[r.di & 0xffff].porta_num3 = r.getDx(); // remainder
            pw.partWk[r.di & 0xffff].lfoswi |= 8; // Porta ON
            //r.ax = r.stack.pop(); // commands
            return porta_returnRef;
        }
//porta_notset:
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat; // Skip the first note (when masked)
        return null;
    }

    /**
     * Portamento (PSG)
     */
    private Supplier<Object> portap() {
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            return null;
            //return porta_notset;
        }

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = pw.cmd;
        WriteOPNARegister.accept(cd);

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        lfoinitp();
        oshiftp();
        fnumsetp();
        r.setAx(pw.partWk[r.di & 0xffff].fnum);
        r.stack.push(r.getAx());
        r.al = pw.partWk[r.di & 0xffff].onkai;
        r.stack.push(r.getAx());
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        oshiftp();
        fnumsetp();
        r.setAx(pw.partWk[r.di & 0xffff].fnum); // ax = psg_tune value of portamento destination
        r.setBx(r.stack.pop());
        pw.partWk[r.di & 0xffff].onkai = r.bl;
        r.setBx(r.stack.pop()); // bx = psg_tune value of portamento source
        pw.partWk[r.di & 0xffff].fnum = r.getBx();
        r.subAx(r.getBx()); // ax = psg_tune difference
        r.bl = (byte) pw.md[r.getSi() & 0xffff].dat;
        r.incSi();
        pw.partWk[r.di & 0xffff].leng = r.bl;
        calc_q();
        r.bh = 0;
        int src = /* signed */ r.getAx();
        r.setDx((short) (src % /* signed */ r.getBx())); // ax = psg_tune difference / note length
        r.setAx((short) (src / /* signed */ r.getBx()));
        pw.partWk[r.di & 0xffff].porta_num2 = r.getAx(); // quotient
        pw.partWk[r.di & 0xffff].porta_num3 = r.getDx(); // remainder
        pw.partWk[r.di & 0xffff].lfoswi |= 8; // Porta ON
        //r.ax = r.stack.pop(); // commandsp
        return porta_returnpRef;
    }

    /**
     * Output value to STATUS
     */
    public Supplier<Object> status_write() {
        logger.log(Level.TRACE, "status_write");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.status = r.al;
        return null;
    }

    /**
     * Add a value to STATUS
     */
    public Supplier<Object> status_add() {
        logger.log(Level.TRACE, "status_add");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.setBx((short) 0); // offset status
        r.al += pw.status; // add al,[bx]
        pw.status = r.al; // mov[bx],al
        return null;
    }

    /**
     * Change only one volume (V2.7 expansion)
     */
    private Supplier<Object> vol_one_up_fm() {
        logger.log(Level.TRACE, "vol_one_up_fm");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.partWk[r.di & 0xffff].volume;
        if ((r.al & 0xff) < 128)
            return this::vo_vset;
        r.al = 127;
        return this::vo_vset;
    }

    private Supplier<Object> vo_vset() {
        r.al++;
        pw.partWk[r.di & 0xffff].volpush = r.al;
        pw.volpush_flag = 1;
        return null;
    }

    private Supplier<Object> vol_one_up_psg() {
        logger.log(Level.TRACE, "vol_one_up_psg");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.partWk[r.di & 0xffff].volume;
        if ((r.al & 0xff) < 16)
            return this::vo_vset;
        r.al = 15;
        return this::vo_vset;
    }

    public Supplier<Object> vol_one_up_pcm() {
        logger.log(Level.TRACE, "vol_one_up_pcm");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.carry = (r.al & 0xff) + (pw.partWk[r.di & 0xffff].volume & 0xff) > 0xff;
        r.al += pw.partWk[r.di & 0xffff].volume;
        if (r.carry) return this::voup_over;
        return this::vmax_check;
    }

    private Supplier<Object> vmax_check() {
        if ((r.al & 0xff) < 255)
            return this::vo_vset;
        return this::voup_over;
    }

    private Supplier<Object> voup_over() {
        r.al = (byte) 254;
        return this::vo_vset;
    }

    public Supplier<Object> vol_one_down() {
        logger.log(Level.TRACE, "vol_one_down");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di & 0xffff].volume;
        r.carry = (r.al & 0xff) < (r.ah & 0xff);
        r.al -= r.ah;
        if (!r.carry) return this::vmax_check;
        r.al = 0;
        return this::vo_vset;
    }

    /**
     * FM Sound Generator Hard LFO Settings (v2.4 Extended)
     */
    private Supplier<Object> hlfo_set() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.board2 != 0) {
            r.ah = r.al;
            r.al = pw.partWk[r.di & 0xffff].fmpan;
            r.al &= (byte) 0b1100_0000;
            r.al |= r.ah;
            pw.partWk[r.di & 0xffff].fmpan = r.al;
            if (pw.partb == 3) { // break hlfoset_notfm3;
                if (pw.fmsel == 0) { // break hlfoset_notfm3;
                    // Only in the case of 2608, so part_e is impossible
                    // For FM3, set all four parts
                    r.stack.push(r.di);
                    r.di = (short) pw.part3; // offset part3
                    pw.partWk[r.di & 0xffff].fmpan = r.al;
                    r.di = (short) pw.part3b; // offset part3b
                    pw.partWk[r.di & 0xffff].fmpan = r.al;
                    r.di = (short) pw.part3c; // offset part3c
                    pw.partWk[r.di & 0xffff].fmpan = r.al;
                    r.di = (short) pw.part3d; // offset part3d
                    pw.partWk[r.di & 0xffff].fmpan = r.al;
                    r.di = r.stack.pop();
                }
            }
//hlfoset_notfm3:
            if (pw.partWk[r.di & 0xffff].partmask == 0) { // Are the parts masked? // break hlfo_exit;
                r.dh = pw.partb;
                r.dh += (byte) (0xb4 - 1);
                calc_panout();
                opnset();
            }
        }
//hlfo_exit:
        return null;
    }

    /**
     * FM Sound Source Hard LFO Switch (V2.4 Expansion)
     */
    private Supplier<Object> hlfo_onoff() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.board2 != 0) {
            r.dl = r.al;
            r.dh = 0x22;
            pw.port22h = r.dl;
            opnset44();
            return null;
        } else {
            return null;
        }
    }

    /**
     * FM Sound Source Hardware LFO Delay Settings
     */
    private Supplier<Object> hlfo_delay() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.board2 != 0) {
            pw.partWk[r.di & 0xffff].hldelay = r.al;
        }
        return null;
    }

    /**
     * COMMAND 'Z' (change bar length)
     */
    public Supplier<Object> syousetu_lng_set() {
        logger.log(Level.TRACE, "syousetu_lng_set");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.syousetu_lng = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Tempo, null, pw.tempo_d & 0xff, pw.syousetu_lng & 0xff);
        cd.additionalData = md;
        WriteOPNARegister.accept(cd);

        return null;
    }

    /**
     * COMMAND '@' [PROGRAM CHANGE]
     */
    private Supplier<Object> comAt() {
        logger.log(Level.TRACE, "com@");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].voicenum = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.Instrument, pw.cmd.linePos,
                0xff, pw.partWk[r.di & 0xffff].voicenum & 0xff);
        writeDummy(cd);

        r.dl = r.al;
        if (pw.partWk[r.di & 0xffff].partmask == 0) { // Are the parts masked? // break comAt_mask;

            neiroset();
            return null;
        }
//comAt_mask:
        toneadr_calc();

        r.dl = (byte) pw.inst[(r.getBx() + 24) & 0xffff].dat; //    mov dl,24[bx]
        pw.partWk[r.di & 0xffff].alg_fb = r.dl; // alg/fb settings
        r.andBx((short) 4);

        neiroset_tl(); // tl setting (NO break dl)

comAt_afset:
        {
            // If masked on FM3ch, set fm3_alg_fb
            if (pw.partb == 3) { // break comAt_exit;

                if (pw.partWk[r.di & 0xffff].neiromask != 0) { // break comAt_exit;

                    if (pw.board2 != 0) {
                        if (pw.fmsel == 0)
                            break comAt_afset;
                    } else {
                        if (r.di != pw.part_e)
                            break comAt_afset;
                    }
                }
            }
//comAt_exit:
            return null;
        }
//comAt_afset:
        // ;in. dl = alg/fb
        if ((pw.partWk[r.di & 0xffff].slotmask & 0x10) == 0) { // If you are not using slot 1 // break comAt_notslot1;

            r.al = pw.fm3_alg_fb;
            r.al &= 0b0011_1000; // fb uses previous value
            r.dl &= 0b0000_0111;
            r.dl |= r.al;
        }
//comAt_notslot1:
        pw.fm3_alg_fb = r.dl;
        pw.partWk[r.di & 0xffff].alg_fb = r.al;
        return null;
    }

    /**
     * COMMAND 'q' [STEP-GATE CHANGE]
     */
    public Supplier<Object> comq() {
        logger.log(Level.TRACE, "comq");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].qdata = r.al;
        pw.partWk[r.di & 0xffff].qdat3 = 0;
        comq_dmy();

        return null;
    }

    public Supplier<Object> comq3() {
        logger.log(Level.TRACE, "comq3");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].qdat2 = r.al;
        comq_dmy();

        return null;
    }

    public Supplier<Object> comq4() {
        logger.log(Level.TRACE, "comq4");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].qdat3 = r.al;
        comq_dmy();

        return null;
    }

    /**
     * COMMAND 'Q' [STEP-GATE CHANGE 2]
     */
    public Supplier<Object> comq2() {
        logger.log(Level.TRACE, "comq2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].qdatb = r.al;
        comq_dmy();

        return null;
    }

    private void comq_dmy() {
        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Gatetime, pw.cmd.linePos,
                pw.partWk[r.di & 0xffff].qdatb & 0xff , // Q%
                pw.partWk[r.di & 0xffff].qdata & 0xff, // q [X] -x  ,  x    : Number 1
                pw.partWk[r.di & 0xffff].qdat2 & 0xff, // q  x  -x  , [X]   : Number 3
                pw.partWk[r.di & 0xffff].qdat3 & 0xff  // q  x [-X] ,  x    : Number 2
        );
        cd.additionalData = md;
        writeDummy(cd);
    }

    /**
     * COMMAND 'V' [VOLUME CHANGE]
     */
    public Supplier<Object> comv() {
        logger.log(Level.TRACE, "comv");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].volume = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, r.al & 0xff);
        cd.additionalData = md;
        writeDummy(cd);

        return null;
    }

    /**
     * COMMAND 't' [TEMPO CHANGE1]
     * COMMAND 'T' [TEMPO CHANGE2]
     * COMMAND 't±' [TEMPO CHANGE Relative 1]
     * COMMAND 'T±' [TEMPO CHANGE Relative 2]
     */
    public Supplier<Object> comt() {
        logger.log(Level.TRACE, "comt");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) < 251) { // break comt_sp0;
//comt_exit1:
            pw.tempo_d = r.al; // T(FC)
            pw.tempo_d_push = r.al;
            return this::calc_tb_tempo;
        }
//comt_sp0:
        r.al++;
        if (r.al == 0) { // break comt_sp1;

            r.al = (byte) pw.md[r.incSi() & 0xffff].dat; // t(FC FF)

//comt_exit2c:
            if ((r.al & 0xff) < 18) { // break comt_exit2;
//comt_2c_over:
                r.al = 18;
            }
//comt_exit2:
            pw.tempo_48 = r.al;
            pw.tempo_48_push = r.al;
            return this::calc_tempo_tb;
        }
//comt_sp1:
        r.al++;
        boolean zero = r.al == 0;
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (zero) { // break comt_sp2;

            r.ah = pw.tempo_d_push; // T± (FC FE)
            if ((r.al & 0x80) == 0) { // break comt_sp1_minus;
                r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
                r.al += r.ah;
                if (r.carry) { // break comt_sp1_exitc;
                    r.al = (byte) 250;
//                    break comt_exit1;
                    pw.tempo_d = r.al; // T(FC) // <<
                    pw.tempo_d_push = r.al;     // <<
                    return this::calc_tb_tempo; // <<
                }
            } else {
//comt_sp1_minus:
                r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
                r.al += r.ah;
                if (!r.carry) { // break comt_sp1_exitc;
                    r.al = 0;
                }
            }
//comt_sp1_exitc:
            if ((r.al & 0xff) >= 251) { // break comt_exit1;

                r.al = (byte) 250;
            }
//            break comt_exit1;
            pw.tempo_d = r.al; // T(FC) // <<
            pw.tempo_d_push = r.al;     // <<
            return this::calc_tb_tempo; // <<
        }
//comt_sp2:
        r.ah = pw.tempo_48_push; // t± (FC FD)
        if ((r.al & 0x80) == 0) { // break comt_sp2_minus;
            r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
            r.al += r.ah;
            if (r.carry) { // break comt_exit2;
                r.al = (byte) 255;
            }
//            break comt_exit2;
        } else {
//comt_sp2_minus:
            r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
            r.al += r.ah;
            if (!r.carry) {
//                break comt_2c_over;
//comt_2c_over: // <<
                r.al = 18; // <<
//comt_exit2: // <<
                pw.tempo_48 = r.al;         // <<
                pw.tempo_48_push = r.al;    // <<
                return this::calc_tempo_tb; // <<
            }
        }
//        break comt_exit2c;
//comt_exit2c: // <<
        if ((r.al & 0xff) < 18) { // break comt_exit2; // <<
//comt_2c_over: // <<
            r.al = 18; // <<
        } // <<
//comt_exit2: // <<
        pw.tempo_48 = r.al;         // <<
        pw.tempo_48_push = r.al;    // <<
        return this::calc_tempo_tb; // <<
    }

    /**
     * T->t conversion
     * input[tempo_d]
     * output[tempo_48]
     */
    private Supplier<Object> calc_tb_tempo() {
        // TEMPO = 112CH / [ 256 - TB] timerB -> tempo
        r.bl = 0;
        r.bl -= pw.tempo_d; // tempo_d register value
        r.al = (byte) 255;

        if ((r.bl & 0xff) >= 18) { // break ctbt_exit;

            r.setAx((short) 0x112c);
            r.ah = (byte) (0x112c % (r.bl & 0xff));
            r.al = (byte) (0x112c / (r.bl & 0xff));
            if ((r.ah & 0x80) != 0) { // break ctbt_exit;
                r.al++; // Rounding
            }
        }
//ctbt_exit:
        pw.tempo_48 = r.al;
        pw.tempo_48_push = r.al;

        return null;
    }

    /**
     * t->T transformation
     * input[tempo_48]
     * output[tempo_d]
     */
    private Supplier<Object> calc_tempo_tb() {
        // TB = 256 - [ 112CH / TEMPO] tempo -> timerB
        r.bl = pw.tempo_48;
        r.al = 0;

        if ((r.bl & 0xff) >= 18) { // break cttb_exit;

            r.setAx((short) 0x112c);

            r.al = (byte) (0x112c / (r.bl & 0xff));
            r.ah = (byte) (0x112c % (r.bl & 0xff));

            r.dl = 0;
            r.dl -= r.al;
            r.al = r.dl;
            if ((r.ah & 0x80) != 0) { // break cttb_exit;
                r.al--; // Rounding
            }
        }
//cttb_exit:
        pw.tempo_d = r.al;
        pw.tempo_d_push = r.al;

        return null;
    }

    /**
     * COMMAND '&' [tie]
     */
    public Supplier<Object> comtie() {
        logger.log(Level.TRACE, "comtie");

        pw.tieflag |= 1;
        return null;
    }

    /**
     * COMMAND 'D' [Detune]
     */
    public Supplier<Object> comd() {
        logger.log(Level.TRACE, "comd");

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        pw.partWk[r.di & 0xffff].detune = r.getAx();

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.Detune, pw.cmd.linePos, /* signed */ pw.partWk[r.di & 0xffff].detune);
        writeDummy(cd);

        return null;
    }

    /**
     * COMMAND 'DD' [Relative Detune]
     */
    public Supplier<Object> comdd() {
        logger.log(Level.TRACE, "comdd");

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        pw.partWk[r.di & 0xffff].detune += r.getAx();

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.Detune, pw.cmd.linePos, /* signed */ pw.partWk[r.di & 0xffff].detune);
        writeDummy(cd);

        return null;
    }

    /**
     * COMMAND '[' [Loop Start]
     */
    public Supplier<Object> comstloop() {
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        r.setBx(r.getAx());
        r.setAx((short) pw.mmlbuf);
        if ((r.di & 0xffff) == pw.part_e) { // break comst_nonefc;
            r.setAx((short) pw.efcdat);
        }
//comst_nonefc:
        r.addBx(r.getAx());
        r.incBx();
        pw.md[r.getBx() & 0xffff].dat = 0;

        return null;
    }

    /**
     * COMMAND ']' [Loop End]
     */
    public Supplier<Object> comedloop() {
reloop: // ↑
        {
            r.al = (byte) (pw.md[r.incSi() & 0xffff].dat);
            if (r.al != 0) { // break muloop; // If 0, loop unconditionally
                r.ah = r.al;
                pw.md[r.getSi() & 0xffff].dat++;

                r.al = (byte) (pw.md[r.incSi() & 0xffff].dat);
                if (r.ah != r.al)
                    break reloop;
                r.addSi((short) 2);
                return null;
            }
//muloop:
            r.incSi();
            pw.partWk[r.di & 0xffff].loopcheck = 1;
        }
//reloop:
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        r.addAx((short) 2);

        r.setBx((short) pw.mmlbuf);
        if ((r.di & 0xffff) == pw.part_e) { // break comed_nonefc;
            r.setBx((short) pw.efcdat);
        }
//comed_nonefc:
        r.addAx(r.getBx());

        r.setSi(r.getAx());

        return null;
    }

    /**
     * COMMAND ':' [Escape from the loop]
     */
    public Supplier<Object> comexloop() {
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
        r.addSi((short) 2);
        r.setBx(r.getAx());

        r.setAx((short) pw.mmlbuf);
        if ((r.di & 0xffff) == pw.part_e) { // break comex_nonefc;
            r.setAx((short) pw.efcdat);
        }
//comex_nonefc:
        r.addBx(r.getAx());

        r.dl = (byte) pw.md[r.getBx() & 0xffff].dat;
        r.dl--;
        r.incBx();
        if (r.dl != pw.md[r.getBx() & 0xffff].dat) { // break loopexit;
            return null;
        }
//loopexit:
        r.addBx((short) 3);
        r.setSi(r.getBx());
        return null;
    }

    /**
     * COMMAND 'L' [Set a repeat loop]
     */
    public Supplier<Object> comlopset() {
        logger.log(Level.TRACE, "comlopset");

        pw.partWk[r.di & 0xffff].partloop = r.getSi();

        return null;
    }

    /**
     * COMMAND '_' [Scale Shift]
     */
    public Supplier<Object> comshift() {
        logger.log(Level.TRACE, "comshift");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].shift = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.KeyShift, pw.cmd.linePos, /* signed */ pw.partWk[r.di & 0xffff].shift);
        writeDummy(cd);

        return null;
    }

    /**
     * COMMAND '__' [Relative Transposition]
     */
    public Supplier<Object> comshift2() {
        logger.log(Level.TRACE, "comshift2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.partWk[r.di & 0xffff].shift;
        pw.partWk[r.di & 0xffff].shift = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.KeyShift, pw.cmd.linePos, /* signed */ pw.partWk[r.di & 0xffff].shift);
        writeDummy(cd);

        return null;
    }

    /**
     * COMMAND '_M' [Master Transposition Value]
     */
    public Supplier<Object> comshift_master() {
        logger.log(Level.TRACE, "comshift_master");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].shift_def = r.al;
        return null;
    }

    /**
     * COMMAND ')' [VOLUME UP]
     */
    // for FM
    private Supplier<Object> comvolup() {
        logger.log(Level.TRACE, "comvolup");

        r.al = pw.partWk[r.di & 0xffff].volume;
        r.al += 4;
        return this::volupck;
    }

    private Supplier<Object> volupck() {
        logger.log(Level.TRACE, "volupck");

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.min(r.al & 0xff, 127));
        cd.additionalData = md;
        writeDummy(cd);

        if ((r.al & 0xff) < 128)
            return this::vset;
        r.al = 127;
        return this::vset;
    }

    private Supplier<Object> vset() {
        logger.log(Level.TRACE, "vset");

        pw.partWk[r.di & 0xffff].volume = r.al;
        return null;
    }

    // With numbers
    private Supplier<Object> comvolup2() {
        logger.log(Level.TRACE, "comvolup2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.partWk[r.di & 0xffff].volume;
        return this::volupck;
    }

    // for PSG
    private Supplier<Object> comvolupp() {
        logger.log(Level.TRACE, "comvolupp");

        r.al = pw.partWk[r.di & 0xffff].volume;
        r.al++;
        return this::volupckp;
    }

    private Supplier<Object> volupckp() {
        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.min(r.al & 0xff, 15));
        cd.additionalData = md;
        writeDummy(cd);

        if ((r.al & 0xff) < 16)
            return this::vset;
        r.al = 15;
        return this::vset;
    }

    // With numbers
    private Supplier<Object> comvolupp2() {
        logger.log(Level.TRACE, "comvolupp2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.partWk[r.di & 0xffff].volume;
        return this::volupckp;
    }

    /**
     * COMMAND '(' [VOLUME DOWN]
     */
    // for FM
    private Supplier<Object> comvoldown() {
        logger.log(Level.TRACE, "comvoldown");

        r.al = pw.partWk[r.di & 0xffff].volume;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.max((r.al & 0xff) - 4, 0));
        cd.additionalData = md;
        writeDummy(cd);

        r.carry = (r.al & 0xff) < 4;
        r.al -= 4;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    // With numbers
    private Supplier<Object> comvoldown2() {
        logger.log(Level.TRACE, "comvoldown2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di & 0xffff].volume;

        //For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.max((r.al & 0xff) - (r.ah & 0xff), 0));
        cd.additionalData = md;
        writeDummy(cd);

        r.carry = (r.al & 0xff) < (r.ah & 0xff);
        r.al -= r.ah;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    // for PSG
    private Supplier<Object> comvoldownp() {
        logger.log(Level.TRACE, "comvoldownp");

        r.al = pw.partWk[r.di & 0xffff].volume;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.max((r.al & 0xff) - 1, 0));
        cd.additionalData = md;
        writeDummy(cd);

        if (r.al == 0) return this::vset;
        r.al--;
        return this::vset;
    }

    // With numbers
    private Supplier<Object> comvoldownp2() {
        logger.log(Level.TRACE, "comvoldownp2");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di & 0xffff].volume;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(0, MMLType.Volume, pw.cmd.linePos, Math.max((r.al & 0xff) - (r.ah & 0xff), 0));
        cd.additionalData = md;
        writeDummy(cd);

        r.carry = (r.al & 0xff) < (r.ah & 0xff);
        r.al -= r.ah;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    /**
     * LFO2 Processing
     */
    public Supplier<Object> _lfoset() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfoset);
    }

    private Supplier<Object> _lfo_main(Supplier<Object> fnc) {
        //pushf
        //cli
        r.stack.push(r.getAx());
        lfo_change();
        r.setAx(r.stack.pop());
        Object o = fnc.get();
        while (o != null) o = ((Supplier<Object>) o).get();
        lfo_change();
        //popf
        return null;
    }

    public Supplier<Object> _mdepth_set() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::mdepth_set);
    }

    public Supplier<Object> _lfowave_set() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfowave_set);
    }

    public Supplier<Object> _lfo_extend() {
        logger.log(Level.TRACE, "_lfo_extend");

        r.setAx((short) 0); // offset lfo_extend
        return _lfo_main(this::lfo_extend);
    }

    public Supplier<Object> _lfoset_delay() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfoset_delay);
    }

    public Supplier<Object> _lfoswitch() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al &= 7;
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        pw.partWk[r.di & 0xffff].lfoswi &= (byte) 0x8f;
        pw.partWk[r.di & 0xffff].lfoswi |= r.al;
        lfo_change();
        lfoinit_main();
        lfo_change();
        return null;
    }

    private Supplier<Object> _lfoswitch_f() {
        _lfoswitch();
        return this::ch3_setting;
    }

    /**
     * LFO1<->LFO2 change
     */
    public void lfo_change() {
        r.setAx(pw.partWk[r.di & 0xffff].lfodat);
        pw.partWk[r.di & 0xffff].lfodat = pw.partWk[r.di & 0xffff]._lfodat;
        pw.partWk[r.di & 0xffff]._lfodat = r.getAx();

        r.cl = 4;
        pw.partWk[r.di & 0xffff].lfoswi = (byte) (((pw.partWk[r.di & 0xffff].lfoswi & 0xff) << 4) | ((pw.partWk[r.di & 0xffff].lfoswi & 0xf0) >>> 4));
        pw.partWk[r.di & 0xffff].extendmode = (byte) (((pw.partWk[r.di & 0xffff].extendmode & 0xff) << 4) | ((pw.partWk[r.di & 0xffff].extendmode & 0xf0) >>> 4));

        r.al = pw.partWk[r.di & 0xffff].delay;
        pw.partWk[r.di & 0xffff].delay = pw.partWk[r.di & 0xffff]._delay;
        pw.partWk[r.di & 0xffff]._delay = r.al;

        r.al = pw.partWk[r.di & 0xffff].speed;
        pw.partWk[r.di & 0xffff].speed = pw.partWk[r.di & 0xffff]._speed;
        pw.partWk[r.di & 0xffff]._speed = r.al;

        r.al = pw.partWk[r.di & 0xffff].step;
        pw.partWk[r.di & 0xffff].step = pw.partWk[r.di & 0xffff]._step;
        pw.partWk[r.di & 0xffff]._step = r.al;

        r.al = pw.partWk[r.di & 0xffff].time;
        pw.partWk[r.di & 0xffff].time = pw.partWk[r.di & 0xffff]._time;
        pw.partWk[r.di & 0xffff]._time = r.al;

        r.al = pw.partWk[r.di & 0xffff].delay2;
        pw.partWk[r.di & 0xffff].delay2 = pw.partWk[r.di & 0xffff]._delay2;
        pw.partWk[r.di & 0xffff]._delay2 = r.al;

        r.al = pw.partWk[r.di & 0xffff].speed2;
        pw.partWk[r.di & 0xffff].speed2 = pw.partWk[r.di & 0xffff]._speed2;
        pw.partWk[r.di & 0xffff]._speed2 = r.al;

        r.al = pw.partWk[r.di & 0xffff].step2;
        pw.partWk[r.di & 0xffff].step2 = pw.partWk[r.di & 0xffff]._step2;
        pw.partWk[r.di & 0xffff]._step2 = r.al;

        r.al = pw.partWk[r.di & 0xffff].time2;
        pw.partWk[r.di & 0xffff].time2 = pw.partWk[r.di & 0xffff]._time2;
        pw.partWk[r.di & 0xffff]._time2 = r.al;

        r.al = pw.partWk[r.di & 0xffff].mdepth;
        pw.partWk[r.di & 0xffff].mdepth = pw.partWk[r.di & 0xffff]._mdepth;
        pw.partWk[r.di & 0xffff]._mdepth = r.al;

        r.al = pw.partWk[r.di & 0xffff].mdspd;
        pw.partWk[r.di & 0xffff].mdspd = pw.partWk[r.di & 0xffff]._mdspd;
        pw.partWk[r.di & 0xffff]._mdspd = r.al;

        r.al = pw.partWk[r.di & 0xffff].mdspd2;
        pw.partWk[r.di & 0xffff].mdspd2 = pw.partWk[r.di & 0xffff]._mdspd2;
        pw.partWk[r.di & 0xffff]._mdspd2 = r.al;
        r.ah = pw.partWk[r.di & 0xffff].lfo_wave;
        pw.partWk[r.di & 0xffff].lfo_wave = pw.partWk[r.di & 0xffff]._lfo_wave;
        pw.partWk[r.di & 0xffff]._lfo_wave = r.ah;

        r.al = pw.partWk[r.di & 0xffff].mdc;
        pw.partWk[r.di & 0xffff].mdc = pw.partWk[r.di & 0xffff]._mdc;
        pw.partWk[r.di & 0xffff]._mdc = r.al;
        r.al = pw.partWk[r.di & 0xffff].mdc2;
        pw.partWk[r.di & 0xffff].mdc2 = pw.partWk[r.di & 0xffff]._mdc2;
        pw.partWk[r.di & 0xffff]._mdc2 = r.al;
    }

    /**
     * Set the LFO parameters
     */
    public Supplier<Object> lfoset() {
        logger.log(Level.TRACE, "lfoset");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].delay = r.al;
        pw.partWk[r.di & 0xffff].delay2 = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].speed = r.al;
        pw.partWk[r.di & 0xffff].speed2 = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].step = r.al;
        pw.partWk[r.di & 0xffff].step2 = r.al;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].time = r.al;
        pw.partWk[r.di & 0xffff].time2 = r.al;

        return this::lfoinit_main;
    }

    public Supplier<Object> lfoset_delay() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].delay = r.al;
        pw.partWk[r.di & 0xffff].delay2 = r.al;

        return this::lfoinit_main;
    }

    /**
     * LFO SWITCH
     */
    public Supplier<Object> lfoswitch() {
        logger.log(Level.TRACE, "lfoswitch");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0b1111_1000) != 0) { // break ls_00;
            r.al = 1;
        }
//ls_00:
        r.al &= 7;
        pw.partWk[r.di & 0xffff].lfoswi &= (byte) 0xf8;
        pw.partWk[r.di & 0xffff].lfoswi |= r.al;
        return this::lfoinit_main;
    }

    private Supplier<Object> lfoswitch_f() {
        logger.log(Level.TRACE, "lfoswitch_f");

        Object o = lfoswitch();
        while (o != null) o = ((Supplier<Object>) o).get();
        return this::ch3_setting;
    }

    /**
     * PSG ENVELOPE SET
     */
    public Supplier<Object> psgenvset() {
        logger.log(Level.TRACE, "psgenvset");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].eenv_ar = r.al; // pat
        pw.partWk[r.di & 0xffff].eenv_arc = r.al; // patb
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].eenv_dr = r.al; // pv2
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].eenv_sr = r.al; // pr1
        pw.partWk[r.di & 0xffff].eenv_src = r.al; // pr1b
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].eenv_rr = r.al; // pr2
        pw.partWk[r.di & 0xffff].eenv_rrc = r.al; // pr2b

        if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // break not_set_count2; // Did it go from Expansion to Normal?

            pw.partWk[r.di & 0xffff].envf = 2; // RR
            pw.partWk[r.di & 0xffff].eenv_volume = (byte) 0xf1; // -15; // Volume // .penv
        }
//not_set_count2:
        return null;
    }

    /**
     * 'y' COMMAND[This is the easiest]
     */
    public Supplier<Object> comy() {
        logger.log(Level.TRACE, "comy");

        r.dh = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.dl = (byte) pw.md[r.incSi() & 0xffff].dat;
        opnset();
        return null;
    }

    /**
     * 'w' COMMAND[PSG NOISE Avg. Frequency]
     */
    private Supplier<Object> psgnoise() {
        logger.log(Level.TRACE, "psgnoise");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.psnoi = r.al;
        return null;
    }

    private Supplier<Object> psgnoise_move() {
        logger.log(Level.TRACE, "psgnoise_move");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al += pw.psnoi;
        if ((r.al & 0x80) != 0) { // break pnm_nminus;
            r.al = 0;
        }
//pnm_nminus:
        if ((r.al & 0xff) >= 32) { // break pnm_set;
            r.al = 31;
        }
//pnm_set:
        pw.psnoi = r.al;
        return null;
    }

    /**
     * 'P' COMMAND[PSG TONE / NOISE / MIX SET]
     */
    private Supplier<Object> psgsel() {
        logger.log(Level.TRACE, "psgsel");

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].psgpat = r.al;
        return null;
    }

    /**
     * 'p' COMMAND[FM PANNING SET]
     */
    private Supplier<Object> panset() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.board2 != 0) {
            return panset_main();
        }
        return null;
    }

    private Supplier<Object> panset_main() {
        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0, MMLType.Pan, pw.cmd.linePos, r.al & 0xff);
        writeDummy(cd);

        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al &= 0b1100_0000;
        r.ah = r.al; // ah<- pan data
        r.al = pw.partWk[r.di & 0xffff].fmpan;
        r.al &= 0b0011_1111;
        r.al |= r.ah;
        pw.partWk[r.di & 0xffff].fmpan = r.al;
        if (pw.partb == 3) { // break panset_notfm3;
            if (pw.fmsel == 0) { // break panset_notfm3;
                // For FM3, set all four parts
                r.stack.push(r.di);
                r.di = (short) pw.part3;
                pw.partWk[r.di & 0xffff].fmpan = r.al;
                r.di = (short) pw.part3b;
                pw.partWk[r.di & 0xffff].fmpan = r.al;
                r.di = (short) pw.part3c;
                pw.partWk[r.di & 0xffff].fmpan = r.al;
                r.di = (short) pw.part3d;
                pw.partWk[r.di & 0xffff].fmpan = r.al;
                r.di = r.stack.pop();
            }
        }
//panset_notfm3:
        if (pw.partWk[r.di & 0xffff].partmask == 0) { // Are the parts masked? break panset_exit;
            r.dl = r.al;
            r.dh = pw.partb;
            r.dh += (byte) (0xb4 - 1);
            calc_panout();
            opnset();
        }
//panset_exit:
        return null;
    }

    /**
     * Get data to be set to 0b4h ~
     * out.dl
     */
    private void calc_panout() {
        if (pw.board2 != 0) {
            r.dl = pw.partWk[r.di & 0xffff].fmpan;
            if (pw.partWk[r.di & 0xffff].hldelay_c != 0) { // break cpo_ret;
                r.dl &= 0xc0; // If HLFO Delay remains, set only the pan
            }
//cpo_ret:
        }
    }

    /**
     * Pan setting Extend
     */
    private Supplier<Object> panset_ex() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.incSi(); // Skip the reverse flag
        if (pw.board2 != 0) {
            if (r.al == 0) {
//                break pex_mid;
//pex_mid:
                r.al = 3;
                return panset_main();
            }
            if ((r.al & 0x80) != 0) {
//                break pex_left;
//pex_left:
                r.al = 1;
                return panset_main();
            }

            r.al = 2;
            return panset_main();
        } else {
            return null;
        }
    }

    /**
     * "\?" COMMAND[OPNA Rhythm Keyon / Dump]
     */
    public Supplier<Object> rhykey() {
        if (pw.board2 != 0) {
            r.dh = 0x10;
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.al &= pw.rhythmmask;
            if (r.al != 0) { // break rhst_ret;
                r.dl = r.al;
                if (pw.fadeout_volume != 0) { // break rk_00;
                    r.stack.push(r.getDx());
                    r.dl = pw.rhyvol;
                    volset2rf();
                    r.setDx(r.stack.pop());
                }
//rk_00:
                if ((r.dl & 0x80) == 0) { // break rhyset;
                    r.al = r.dl;
                    r.setBx((short) 0); // offset rdat
                    r.stack.push(r.getDx());
                    r.setCx((short) 6);
                    r.dh = 0x18;
//rklp:
                    do {
                        r.al = r.ror(r.al, 1);
                        if (r.carry) { // break rk00;
                            r.dl = pw.rdat[r.getBx() & 0xffff];
                            opnset44();
                        }
//rk00:
                        r.incBx();
                        r.dh++;
                        r.decCx();
                    } while (r.getCx() != 0);
                    r.setDx(r.stack.pop());
                }
//rhyset:
                opnset44();
                if ((r.dl & 0x80) != 0) { // break rhst_00;

                    r.setBx((short) 0); // offset rdump_bd
                    rflag_inc(false);
                    r.dl = (byte) ~r.dl;
                    pw.rshot_dat &= r.dl;
//                break rhst_ret2;
                } else {
//rhst_00:
                    r.setBx((short) 0); // offset rshot_bd
                    rflag_inc(true);
                    pw.rshot_dat |= r.dl;
                }
//rhst_ret2:
                if (pw.md[r.getSi() & 0xffff].dat == 0xeb) { // break rhst_ret;
                    _rwait();
                }
            }
//rhst_ret:
            return null;
        } else {
            r.incSi();
            return null;
        }
    }

    private void rflag_inc(boolean isShot) {
        r.al = r.dl;
        r.setCx((short) 6);
//ri_loop:
        do {
            r.al = r.ror(r.al, 1);
            if (!r.carry) { // break ri_not;
                if (isShot) pw.rshot[r.getBx() & 0xffff]++;
                else pw.rdump[r.getBx() & 0xffff]++;
            }
//ri_not:
            r.incBx();
            r.decCx();
        } while (r.getCx() != 0);
    }

    /**
     * "\v?n" COMMAND
     */
    public Supplier<Object> rhyvs() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.cl = (byte) 0xc0;
            r.dl = 0x1f;
            r.dl &= r.al;
            return rs002();
        } else {
            r.incSi();
            return null;
        }
    }

    private Supplier<Object> rs002() {
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al &= 0x7;
        r.setBx((short) 0xffff); // offset rdat-1
        r.dh = r.al;
        r.ah = 0;
        r.addBx(r.getAx());
        r.al = 0x18 - 1;
        r.al += r.dh;
        r.dh = r.al;
        r.al = pw.rdat[r.getBx() & 0xffff];
        r.al &= r.cl;
        r.dl |= r.al;
        pw.rdat[r.getBx() & 0xffff] = r.dl;
        opnset44();
        return null;
    }

    public Supplier<Object> rhyvs_sft() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.setBx((short) 0xffff); // offset rdat-1
            r.dh = r.al;
            r.ah = 0;
            r.addBx(r.getAx());

            r.dh += 0x18 - 1;
            r.al = pw.rdat[r.getBx() & 0xffff];
            r.al &= 0b0001_1111;
            r.dl = r.al;
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.al += r.dl;
            if ((r.al & 0xff) >= 32) { // break rvss00;
                if ((r.al & 0x80) == 0) { // break rvss01;
                    r.al = 31;
//                    break rvss00;
                } else {
//rvss01:
                    r.al = 0;
                }
            }
//rvss00:
            r.al &= 0b0001_1111;
            r.dl = r.al;
            r.al = pw.rdat[r.getBx() & 0xffff];
            r.al &= (byte) 0b1110_0000;
            r.dl |= r.al;
            pw.rdat[r.getBx() & 0xffff] = r.dl;
            opnset44();
            return null;
        } else {
            r.addSi((short) 2);
            return null;
        }
    }

    /**
     * "\p?" COMMAND
     */
    public Supplier<Object> rpnset() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.ah = r.al;
            r.cl = 0x1f;
            r.ah &= 3;
            r.ah = r.ror(r.ah, 1);
            r.ah = r.ror(r.ah, 1);
            r.dl = r.ah;
            return rs002();
        } else {
            r.incSi();
            return null;
        }
    }

    /**
     * "\Vn" COMMAND
     */
    public Supplier<Object> rmsvs() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.dl = r.al;
            r.al = pw.rhythm_voldown;
            if (r.al != 0) {
                r.al = (byte) -r.al;
                r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
                r.dl = r.ah;
            }
            volset2r();
        } else {
            r.incSi();
        }

        return null;
    }

    private void volset2r() {
        pw.rhyvol = r.dl;
        volset2rf();
    }

    private void volset2rf() {
        if (pw.board2 != 0) {
            r.dh = 0x11;
            r.al = pw.fadeout_volume;
            if (r.al != 0) { // break vs2r_000;
                r.al = (byte) ~r.al;
                int ans = (r.al & 0xff) * (r.dl & 0xff);
                r.dl = (byte) (ans >>> 8);
            }
//vs2r_000:
            opnset44();
        }
    }

    public Supplier<Object> rmsvs_sft() {
        if (pw.board2 != 0) {
            r.dh = 0x11;
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
            r.dl = r.al;
            r.al = pw.rhyvol;
            r.al += r.dl;
            if ((r.al & 0xff) >= 64) { // break rmss00;
                if ((r.al & 0x80) == 0) { // break rmss01;
                    r.al = 63;
//                    break rmss00;
                } else {
//rmss01:
                    r.al = 0;
                }
            }
//rmss00:
            r.dl = r.al;
            volset2r();
            return null;
        } else {
            r.incSi();
            return null;
        }
    }

    /**
     * SHIFT[di] Transpose by one
     */
    public void oshift() {
//oshiftp:
        if (r.al == 0xf) // rest
            return;
        r.dl = pw.partWk[r.di & 0xffff].shift;
        r.dl += pw.partWk[r.di & 0xffff].shift_def;
        if ((r.dl & r.dl) == 0)
            return;

        r.bl = r.al;
        r.bl &= 0xf;
        r.al &= (byte) 0xf0;
        r.al = (byte) ((r.al & 0xff) >>> 4); // KUMA: Actually, it's ror x4
        r.bh = r.al; // bh=OCT bl = ONKAI

        if ((r.dl & 0x80) != 0) { // break shiftplus;

            //
            // - direction shift
            //
//shiftminus:
            r.carry = false;
            if (((r.bl & 0xff) + (r.dl & 0xff)) > 0xff) r.carry = true;
            r.bl += r.dl;
            if (!r.carry) { // break sfm2;

//sfm1:
                do {
                    r.bh--;
                    r.carry = false;
                    if (((r.bl & 0xff) + 12) > 0xff) r.carry = true;
                    r.bl += 12;
                } while (!r.carry);
            }
//sfm2:
            r.al = r.bh;
            r.al = (byte) (((r.al & 0xff) >>> 4) | ((r.al & 0xff) << 4)); // ror x4
            r.al |= r.bl;
            return;
        }
        //
        // + direction shift
        //
//shiftplus:
        r.bl += r.dl;
//spm1:
        do {
            if ((r.bl & 0xff) < 0xc)
                break; // spm2;
            r.bh++;
            r.bl -= 12;
        } while (true);
//spm2:
        r.al = r.bh;
        r.al = (byte) (((r.al & 0xff) >>> 4) | ((r.al & 0xff) << 4)); // ror x4
        r.al |= r.bl;

//osret: ret
    }

    private void oshiftp() {
        oshift();
    }

    /**
     * FM BLOCK, F-NUMBER SET
     * @input AL KEY#,0-7F
     */
    private void fnumset() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrest(); // Rests
            return;
        }
        pw.partWk[r.di & 0xffff].onkai = r.al;

        //
        // BLOCK/FNUM CALICULATE
        //
        r.ch = r.al;
        r.ch = r.ror(r.ch, 1);
        r.ch &= 0x38; // ch=BLOCK
        r.bl = r.al;
        r.bl &= 0xf; // bl=ONKAI
        r.bh = 0;
        //r.bx += r.bx;
        r.setAx((short) PW.fnum_data[r.getBx() & 0xffff]);

        //
        // BLOCK SET
        //
        r.ah |= r.ch;
        pw.partWk[r.di & 0xffff].fnum = r.getAx();
    }

    public void fnrest() {
        pw.partWk[r.di & 0xffff].onkai = (byte) 0xff;
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x11) == 0) { // break fnr_ret;
            pw.partWk[r.di & 0xffff].fnum = 0; // Pitch LFO not used
        }
//fnr_ret:
    }

    //
    // PSG TUNE SET
    //
    private void fnumsetp() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrest(); // if rest then set FNUM 0
            return;
        }
        pw.partWk[r.di & 0xffff].onkai = r.al;

        r.cl = r.al;
        r.cl = (byte) (((r.cl & 0xff) >> 4) | ((r.cl & 0xff) << 4)); // ror x4
        r.cl &= 0xf; // cl=oct
        r.bl = r.al;
        r.bl &= 0xf;
        r.bh = 0; // bx=onkai
        //r.bx += r.bx;
        r.setAx((short) PW.psg_tune_data[r.getBx() & 0xffff]);

        r.carry = r.cl == 0 ? false : ((r.getAx() & (1 << (r.cl - 1))) != 0);
        r.setAx((short) (/* singed */ r.getAx() >> (r.cl & 0xff))); //    shr ax,cl

        if (r.carry) { // break pt_non_inc;
            r.incAx();
        }
//pt_non_inc:
        pw.partWk[r.di & 0xffff].fnum = r.getAx();
    }

    /**
     * Set[FNUM / BLOCK + DETUNE + LFO]
     */
    private void otodasi() {
        r.setAx(pw.partWk[r.di & 0xffff].fnum);
        if (r.getAx() == 0) { // break od_00;
            return;
        }
//od_00:
od_non_ch3: // ↑
        if (pw.partWk[r.di & 0xffff].slotmask != 0) { // break od_exit;
            r.setCx(r.getAx());
            r.andCx((short) 0x3800); // cx=BLOCK
            r.ah &= 7; // ax=FNUM
            //
            // Portament/LFO/Detune SET
            //
            r.addAx(pw.partWk[r.di & 0xffff].porta_num);
            r.addAx(pw.partWk[r.di & 0xffff].detune);
            r.dh = pw.partb;
            if (r.dh == 3) { // Ch 3 // break od_non_ch3;

                if (pw.board2 != 0) {
                    if (pw.fmsel != 0)
                        break od_non_ch3;
                } else {
                    if (r.di == pw.part_e) //offset part_e
                        break od_non_ch3;
                }

                if (pw.ch3mode != 0x3f) {
                    ch3_special();
                    return;
                }
            }
//od_non_ch3:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 1) != 0) { // break od_not_lfo1;
                r.addAx(pw.partWk[r.di & 0xffff].lfodat);
            }
//od_not_lfo1:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x10) != 0) { // break od_not_lfo2;
                r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
            }
//od_not_lfo2:
            fm_block_calc();

//#if DEBUG
            //logger.log(Level.TRACE, "cx:%4x  ax:%4x  lfodat:%d  step:%d".formatted(
            //    r.cx, r.ax, (short)pw.partWk[r.di & 0xffff].lfodat, (sbyte)pw.partWk[r.di & 0xffff].step));
//#endif

            //
            // SET BLOCK/FNUM TO OPN
            // input CX:AX
            r.orAx(r.getCx()); // AX = block / Fnum
            r.dh += (byte) (0xa4 - 1);
            r.dl = r.ah;
            //    pushf
            //    cli
            opnset();
            r.dh -= 4;
            r.dl = r.al;
            opnset();
            //    popf
        }
//od_exit:
    }

    /**
     * Pitch setting when using ch3=sound effect mode
     * input CX:block AX:fnum
     */
    private void ch3_special() {
        r.stack.push(r.getSi());
        r.setSi(r.getCx()); // si=block
        r.bl = pw.partWk[r.di & 0xffff].slotmask; // bl=slot mask 4321xxxx
        r.cl = pw.partWk[r.di & 0xffff].lfoswi; // cl=lfoswitch
        r.bh = pw.partWk[r.di & 0xffff].volmask; // bh=lfo1 mask 4321xxxx
        if ((r.bh & 0xf) == 0) { // break c3s_00;
            r.bh = (byte) 0xf0; // all
        }
//c3s_00:
        r.ch = pw.partWk[r.di & 0xffff]._volmask; // ch=lfo2 mask 4321xxxx
        if ((r.ch & 0xf) == 0) { // break ns_sl4;
            r.ch = (byte) 0xf0; // all
        }

        // slot 4
//ns_sl4:
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) (((r.bl & 0xff) << 1) | ((r.bl & 0xff) >>> 7));
        if (r.carry) { // break ns_sl3;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune4);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) (((r.bh & 0xff) << 1) | ((r.bh & 0xff) >>> 7));
            if (r.carry) { // break ns_sl4b;
                if ((r.cl & 1) != 0) { // break ns_sl4b;
                    r.addAx(pw.partWk[r.di & 0xffff].lfodat);
                }
            }
//ns_sl4b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) (((r.ch & 0xff) << 1) | ((r.ch & 0xff) >>> 7));
            if (r.carry) { // break ns_sl4c;
                if ((r.cl & 0x10) != 0) { // break ns_sl4c;
                    r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
                }
            }
//ns_sl4c:
            r.stack.push(r.getCx());
            r.setCx(r.getSi());
            fm_block_calc();
            r.orAx(r.getCx());
            r.setCx(r.stack.pop());

            r.dh = (byte) 0xa6;
            r.dl = r.ah;
            //    pushf
            //    cli
            opnset();
            r.dh = (byte) 0xa2;
            r.dl = r.al;
            opnset();
            //    popf
            r.setAx(r.stack.pop());

        }
        // slot 3
//ns_sl3:
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) (((r.bl & 0xff) << 1) | ((r.bl & 0xff) >>> 7));
        if (r.carry) { // break ns_sl2;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune3);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) (((r.bh & 0xff) << 1) | ((r.bh & 0xff) >>> 7));
            if (r.carry) { // break ns_sl3b;
                if ((r.cl & 1) != 0) { // break ns_sl3b;
                    r.addAx(pw.partWk[r.di & 0xffff].lfodat);
                }
            }
//ns_sl3b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) (((r.ch & 0xff) << 1) | ((r.ch & 0xff) >>> 7));
            if (r.carry) { // break ns_sl3c;
                if ((r.cl & 0x10) != 0) { // break ns_sl3c;
                    r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
                }
            }
//ns_sl3c:
            r.stack.push(r.getCx());
            r.setCx(r.getSi());
            fm_block_calc();
            r.orAx(r.getCx());
            r.setCx(r.stack.pop());

            r.dh = (byte) 0xac;
            r.dl = r.ah;
            //    pushf
            //    cli
            opnset();
            r.dh = (byte) 0xa8;
            r.dl = r.al;
            opnset();
            //    popf
            r.setAx(r.stack.pop());

        }
        // slot 2
//ns_sl2:
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) (((r.bl & 0xff) << 1) | ((r.bl & 0xff) >>> 7));
        if (r.carry) { // break ns_sl1;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune2);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) (((r.bh & 0xff) << 1) | ((r.bh & 0xff) >>> 7));
            if (r.carry) { // break ns_sl2b;
                if ((r.cl & 1) != 0) { // break ns_sl2b;
                    r.addAx(pw.partWk[r.di & 0xffff].lfodat);
                }
            }
//ns_sl2b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) (((r.ch & 0xff) << 1) | ((r.ch & 0xff) >>> 7));
            if (r.carry) { // break ns_sl2c;
                if ((r.cl & 0x10) != 0) { // break ns_sl2c;
                    r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
                }
            }
//ns_sl2c:
            r.stack.push(r.getCx());
            r.setCx(r.getSi());
            fm_block_calc();
            r.orAx(r.getCx());
            r.setCx(r.stack.pop());

            r.dh = (byte) 0xae;
            r.dl = r.ah;
            //    pushf
            //    cli
            opnset();
            r.dh = (byte) 0xaa;
            r.dl = r.al;
            opnset();
            //    popf
            r.setAx(r.stack.pop());
        }
        // slot 1
//ns_sl1:
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) (((r.bl & 0xff) << 1) | ((r.bl & 0xff) >>> 7));
        if (r.carry) { // break ns_exit;

            r.addAx(pw.slot_detune1);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) (((r.bh & 0xff) << 1) | ((r.bh & 0xff) >>> 7));
            if (r.carry) { // break ns_sl1b;
                if ((r.cl & 1) != 0) { // break ns_sl1b;
                    r.addAx(pw.partWk[r.di & 0xffff].lfodat);
                }
            }
//ns_sl1b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) (((r.ch & 0xff) << 1) | ((r.ch & 0xff) >>> 7));
            if (r.carry) { // break ns_sl1c;
                if ((r.cl & 0x10) != 0) { // break ns_sl1c;
                    r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
                }
            }
//ns_sl1c:
            r.setCx(r.getSi());
            fm_block_calc();
            r.orAx(r.getCx());

            r.dh = (byte) 0xad;
            r.dl = r.ah;
            //    pushf
            //    cli
            opnset();
            r.dh = (byte) 0xa9;
            r.dl = r.al;
            opnset();
            //    popf
        }
//ns_exit:
        r.setSi(r.stack.pop());
    }

    /**
     * Fixed when the octave changes due to detune of FM sound source
     * @input CX block / AX:fnum+detune
     * @output CX block / AX:fnum
     */
    private void fm_block_calc() {
        r.sign = (r.getAx() & 0x8000) != 0;
//od0:
        while (true) {
            if (!r.sign) { // break od1;

                if ((r.getAx() & 0xffff) >= 0x26a) { // break od1;
                    //
                    if ((r.getAx() & 0xffff) < 0x26a * 2) { // 04d2h
//                        break od2;
                        return; // <<
                    }
                    //

                    r.addCx((short) 0x800); // oct.up
                    if (r.getCx() != 0x4000) { // break od05;
                        r.subAx((short) 0x26a); // 4d2h-26ah
                        r.sign = (r.getAx() & 0x8000) != 0;
                        continue; // break od0;
                    }
//od05:
                    // It won't go any higher
                    r.setCx((short) 0x3800);
                    if ((r.getAx() & 0xffff) >= 0x800) { // break od_ret;
                        r.setAx((short) 0x7ff); // 04d2h
                    }
//od_ret:
                    return;
                }
            }
            //
//od1:
            r.carry = (r.getCx() & 0xffff) < 0x800;
            r.subCx((short) 0x800); // oct.down
            if (r.carry) break; // od15;
            r.addAx((short) 0x26a); // 4d2h-26ah
            r.sign = (r.getAx() & 0x8000) != 0;
        } // break od0;
//od15:
        // It won't go down any further
        r.setCx((short) 0);
        r.sign = (r.getAx() & 0x8000) != 0;
        if (!r.sign) { // break od16;
            if ((r.getAx() & 0xffff) >= 8) { // 4
//                break od2;
                return;
            }
        }
//od16:
        r.setAx((short) 8); // 4
        // ;
//od2:
    }

    /**
     * PSG pitch setting
     */
    private void otodasip() {
        r.setAx(pw.partWk[r.di & 0xffff].fnum);
        if (r.getAx() == 0) { // break od_00p;
            return;
        }
//od_00p:
        //
        // PSG Portament set
        //
        r.setAx((short) (/* signed */ r.getAx() + /* signed */ pw.partWk[r.di & 0xffff].porta_num));
        //
        // PSG Detune/LFO set
        //
        if ((pw.partWk[r.di & 0xffff].extendmode & 1) == 0) { // break od_ext_detune;
            r.subAx(pw.partWk[r.di & 0xffff].detune);
            if ((pw.partWk[r.di & 0xffff].lfoswi & 1) != 0) { // break od_notlfo1;
                r.subAx(pw.partWk[r.di & 0xffff].lfodat);
            }
//od_notlfo1:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x10) != 0) { // break tonesetp;
                r.subAx(pw.partWk[r.di & 0xffff]._lfodat);
            }
//            break tonesetp;
        } else {
//od_ext_detune:
            // Calculation of Extended DETUNE (DETUNE)
            r.stack.push(r.getAx());
            r.setBx(pw.partWk[r.di & 0xffff].detune);
            if (r.getBx() != 0) { // break od_ext_lfo; // To LFO

                int ans = /* signed */ r.getAx() * /* signed */ r.getBx(); //  imul    bx
                ans <<= 4;
                r.setDx((short) (ans >> 16));
                r.setAx((short) (ans >> 0));
                if (ans >= 0) { // break extdet_minus;
                    r.incDx();
//                    break extdet_set;
                } else {
//extdet_minus:
                    r.decDx();
                }
//extdet_set:
                r.setAx(r.stack.pop());
                r.subAx(r.getDx()); // Detune
                r.stack.push(r.getAx());
            }
//od_ext_lfo:
            // Extended DETUNE(LFO) calculation
            r.setDx((short) 0);
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x11) != 0) { // break extlfo_set;
                r.setDx((short) 0);
                if ((pw.partWk[r.di & 0xffff].lfoswi & 0x1) != 0) { // break od_ext_notlfo1;
                    r.setDx(pw.partWk[r.di & 0xffff].lfodat);
                }
//od_ext_notlfo1:
                if ((pw.partWk[r.di & 0xffff].lfoswi & 0x10) != 0) { // break od_ext_notlfo2;
                    r.addDx(pw.partWk[r.di & 0xffff]._lfodat);
                }
//od_ext_notlfo2:
                if (r.getDx() != 0) { // break extlfo_set;
                    int ans1 = /* signed */ r.getAx() * /* signed */ r.getDx(); //  imul    dx
                    ans1 <<= 4;
                    r.setDx((short) (ans1 >> 16));
                    r.setAx((short) (ans1 >> 0));
                    if (ans1 >= 0) { // break extlfo_minus;
                        r.incDx();
//                        break extlfo_set;
                    } else {
//extlfo_minus:
                        r.decDx();
                    }
                }
            }
//extlfo_set:
            r.setAx(r.stack.pop());
            r.subAx(r.getDx()); // Shifting the LFO
        }
        //
        // TONE SET
        //
//tonesetp:
        if (true) { // Assault mix: 0 // KUMA: false
            if ((r.getAx() & 0xffff) >= 0x1000) { // break tsp_01;
                if ((r.getAx() & 0x8000) == 0) { // break tsp_00;
                    r.setAx((short) 0xfff);
//                    break tsp_01;
                } else {
//tsp_00:
                    r.setAx((short) 0);
                }
            }
        }
//tsp_01:
        r.dh = pw.partb;
        r.dh--;
        r.dh += r.dh;
        r.dl = r.al;
        //    pushf
        //    cli
        opnset44();
        r.dh++;
        r.dl = r.ah;
        opnset44();
        //    popf
    }

    /**
     * FM volume set
     *
     * Per-slot calculation & output macros
     * @input dl Original TL value
     * @input dh Register to be output
     * @input al Volume fluctuation value center=80h
     */
    private void volset_slot() {
        r.carry = (r.al & 0xff) + (r.dl & 0xff) > 0xff;
        r.al += r.dl;
        if (r.carry) { // break vsl_noover1;
            r.al = (byte) 255;
        }
//vsl_noover1:
        r.carry = (r.al & 0xff) < 0x80;
        r.al -= (byte) 0x80;
        if (r.carry) { // break vsl_noover2;
            r.al = 0;
        }
//vsl_noover2:
        r.dl = r.al;
        opnset();
    }

    //
    // FM volume setting main
    //
    private void volset() {
        r.bl = pw.partWk[r.di & 0xffff].slotmask; // bl<- slotmask
        if (r.bl == 0) { // break vs_exec;
            return; // When SlotMask is 0
        }
//vs_exec:
        r.al = pw.partWk[r.di & 0xffff].volpush;
        if (r.al != 0) { // break vs_00a;

            r.al--;
//            break vs_00;
        } else {
//vs_00a:
            r.al = pw.partWk[r.di & 0xffff].volume;
        }
//vs_00:
        r.cl = r.al;
        if ((r.di & 0xffff) == pw.part_e) {
            fmvs(); // For sound effects, voldown/fadeout has no effect
            return;
        }

        pmdAsm_4743_voldown();
    }

    //
    // Volume down calculation
    //
    private void pmdAsm_4743_voldown() {
        r.al = pw.fm_voldown;
        if (r.al == 0) {
            fm_fade_calc();
            return;
        }

        r.al = (byte) -r.al;
        r.setAx((short) ((r.al & 0xff) * (r.cl & 0xff)));
        r.cl = r.ah;

        fm_fade_calc();
    }

    //
    // Fadeout calculation
    //
    private void fm_fade_calc() {
        r.al = pw.fadeout_volume;
        if ((r.al & 0xff) >= 2) {
            r.al = (byte) ((r.al & 0xff) >>> 1); // A 50% reduction is enough
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.cl & 0xff)));
            r.cl = r.ah;
        }

        fmvs();
    }

    /**
     * Set volume to carrier & volume LFO processing
     * @input cl to Volume[0 - 127]
     * @input bl to SlotMask
     */
    private void fmvs() {
        r.bh = 0; // Vol Slot Mask
        r.ch = r.bl; // ch=SlotMask Push

        r.stack.push(r.getSi());
        r.setSi((short) 0); // offset vol_tbl
        pw.vol_tbl[r.getSi() & 0xffff] = 0x80;
        pw.vol_tbl[(r.getSi() & 0xffff) + 1] = 0x80;
        pw.vol_tbl[(r.getSi() & 0xffff) + 2] = 0x80;
        pw.vol_tbl[(r.getSi() & 0xffff) + 3] = 0x80;

        r.cl = (byte) ~r.cl; // cl=Volume set to carrier +80H(add)
        r.bl &= pw.partWk[r.di & 0xffff].carrier; // bl=SLOT to set the volume xxxx0000b
        r.bh |= r.bl;
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_01;

            pw.vol_tbl[r.getSi() & 0xffff] = r.cl;
        }
//fmvs_01:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_02;

            pw.vol_tbl[r.getSi() & 0xffff] = r.cl;
        }
//fmvs_02:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_03;

            pw.vol_tbl[r.getSi() & 0xffff] = r.cl;
        }
//fmvs_03:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_04;

            pw.vol_tbl[r.getSi() & 0xffff] = r.cl;
        }
//fmvs_04:
        r.subSi((short) 3);
        if (r.cl != (byte) 255) { // Volume 0? // break fmvs_no_lfo;

            if ((pw.partWk[r.di & 0xffff].lfoswi & 2) != 0) { // break fmvs_not_vollfo1;

                r.bl = pw.partWk[r.di & 0xffff].volmask;
                r.bl &= r.ch; // bl = SLOT to set volume LFO xxxx0000b
                r.bh |= r.bl;
                r.setAx(pw.partWk[r.di & 0xffff].lfodat); // ax = Volume LFO fluctuation value (sub)
                fmlfo_sub();
            }
//fmvs_not_vollfo1:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x20) != 0) { // break fmvs_no_lfo;

                r.bl = pw.partWk[r.di & 0xffff]._volmask; // mov bl,_volmask[di]
                r.bl &= r.ch; // bh=SLOT for setting the volume LFO xxxx0000b
                r.bh |= r.bl;
                r.setAx(pw.partWk[r.di & 0xffff]._lfodat); // ax = Volume LFO fluctuation value (sub)
                fmlfo_sub();
            }
        }
//fmvs_no_lfo:
        r.dh = 0x4c - 1;
        r.dh += pw.partb; // dh=FM Port Address
        r.al = (byte) pw.vol_tbl[r.incSi() & 0xffff]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_01;
            r.dl = pw.partWk[r.di & 0xffff].slot4;
            volset_slot();
        }
//fmvm_01:
        r.dh -= 8;
        r.al = (byte) pw.vol_tbl[r.incSi() & 0xffff]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_02;
            r.dl = pw.partWk[r.di & 0xffff].slot3;
            volset_slot();
        }
//fmvm_02:
        r.dh += 4;
        r.al = (byte) pw.vol_tbl[r.incSi() & 0xffff]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_03;
            r.dl = pw.partWk[r.di & 0xffff].slot2;
            volset_slot();
        }
//fmvm_03:
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_04;
            r.dh -= 8;
            r.al = (byte) pw.vol_tbl[r.incSi() & 0xffff]; // lodsb
            r.dl = pw.partWk[r.di & 0xffff].slot1;
            volset_slot();
        }
//fmvm_04:
        r.setSi(r.stack.pop());
    }

    //
    // Sub for Volume LFO
    //
    private void fmlfo_sub() {
        r.stack.push(r.getCx());
        r.setCx((short) 4);
//fmlfo_loop:
        do {
            r.carry = (r.bl & 0x80) != 0;
            r.bl = (byte) (((r.bl & 0xff) << 1) | ((r.bl & 0xff) >>> 7));
            if (r.carry) { // break fml_exit;
                if ((r.al & 0x80) == 0) { // break fmls_minus;
                    r.carry = pw.vol_tbl[r.getSi() & 0xffff] < (r.al & 0xff);
                    pw.vol_tbl[r.getSi() & 0xffff] -= (r.al & 0xff);
                    if (r.carry) { // break fml_exit;
                        pw.vol_tbl[r.getSi() & 0xffff] = 0;
                    }
//                    break fml_exit;
                } else {
//fmls_minus:
                    r.carry = pw.vol_tbl[r.getSi() & 0xffff] < (r.al & 0xff);
                    pw.vol_tbl[r.getSi() & 0xffff] -= (r.al & 0xff);
                    if (!r.carry) { // break fml_exit;
                        pw.vol_tbl[r.getSi() & 0xffff] = 0xff;
                    }
                }
            }
//fml_exit:
            r.incSi();
            r.decCx();
        } while (r.getCx() != 0); // break fmlfo_loop;

        r.setCx(r.stack.pop());
        r.subSi((short) 4);
    }

    /**
     * PSG volume set
     */
    private void volsetp() {
        if (pw.partWk[r.di & 0xffff].envf == 3) {
//            break volsetp_ret;
            return;
        }
        if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // -1 // break vsp_00;
            if (pw.partWk[r.di & 0xffff].eenv_count == 0) { // break vsp_00;
//volsetp_ret:
                return;
            }
        }
//vsp_00:
        r.al = pw.partWk[r.di & 0xffff].volpush;
        if (r.al != 0) { // break vsp_01a;
            r.al--;
//            break vsp_01;
        } else {
//vsp_01a:
            r.al = pw.partWk[r.di & 0xffff].volume;
        }
//vsp_01:
        r.dl = r.al;
        //
        // Volume down calculation
        //
        r.al = pw.ssg_voldown;
        if (r.al != 0) { // break psg_fade_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // Fadeout calculation
        //
//psg_fade_calc:
        r.al = pw.fadeout_volume;
        if (r.al != 0) { // break psg_env_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // ENVELOPE Calculation
        //
//psg_env_calc:
pv_out: // ↑
        if (r.dl != 0) { // Volume 0? // break pv_out;
            if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // -1 // break normal_pvset;
                r.al = r.dl; // Extended volume = dl * (eenv_vol + 1) / 16
                r.dl = pw.partWk[r.di & 0xffff].eenv_volume;
                if (r.dl == 0) {
//                    break pv_min;
//pv_min:
                    r.dl = 0;

                    if (r.dl == 0) break pv_out; // When it reaches 0, the volume LFO is not applied.
                    if ((r.dl & 0xff) >= 16) { // break pv1;
                        r.dl = 15;
                    }
                } else {
                    r.dl++;
                    r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
                    r.dl = r.al;
                    r.dl = (byte) ((r.dl & 0xff) >>> 3);
                    r.carry = ((r.dl & 0xff) % 2) != 0;
                    r.dl = (byte) ((r.dl & 0xff) >>> 1);
                    if (r.carry) { // break pv1;
                        r.dl++;
                    }
//                    break pv1;
                }
            } else {
//normal_pvset:
                r.dl += pw.partWk[r.di & 0xffff].eenv_volume; // .penv;
                if ((r.dl & 0x80) != 0) { // break pv0;
//pv_min:
                    r.dl = 0;
                }
//pv0:
                if (r.dl == 0) break pv_out; // When it reaches 0, the volume LFO is not applied.
                if ((r.dl & 0xff) >= 16) { // break pv1;
                    r.dl = 15;
                }
            }
            //
            // Volume LFO Calculation
            //
//pv1:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x22) != 0) { // break pv_out;
                r.setAx((short) 0);
                if ((pw.partWk[r.di & 0xffff].lfoswi & 0x2) != 0) { // break pv_nolfo1;
                    r.setAx(pw.partWk[r.di & 0xffff].lfodat);
                }
//pv_nolfo1:
                if ((pw.partWk[r.di & 0xffff].lfoswi & 0x20) != 0) { // break pv_nolfo2;
                    r.andAx(pw.partWk[r.di & 0xffff]._lfodat);
                }
//pv_nolfo2:
                r.dh = 0;
                r.addDx(r.getAx());
                if ((r.getDx() & 0x8000) != 0) { // break pv10;
                    r.dl = 0;
//                    break pv_out;
                } else {
//pv10:
                    if ((r.getDx() & 0xffff) >= 16) { // break pv_out;
                        r.dl = 15;
                    }
                }
            }
        }
        //
        // output
        //
//pv_out:
        r.dh = pw.partb;
        r.dh += 8 - 1;
        //logger.log(Level.TRACE, "%d %d", r.dh, r.dl);
        opnset44();
    }

    /**
     * FM KeyOn
     */
    private void keyon() {
        if (pw.partWk[r.di & 0xffff].onkai == (byte) 0xff) { //-1 // break ko1;
//keyon_ret:;
            return; // when a rest
        }
//ko1:
ura_keyon: // ↑
        {
            r.dh = 0x28;
            r.dl = pw.partb;
            r.dl--;
            r.bh = 0;
            r.bl = r.dl;
            if (pw.board2 != 0) {
                if (pw.fmsel != r.bh) // 0
                    break ura_keyon;
            }

            r.addBx((short) 0); // offset omote_key1
            r.al = pw.fmKeyOnDataTbl[r.getBx() & 0xffff];
            r.al |= pw.partWk[r.di & 0xffff].slotmask;
            if (pw.partWk[r.di & 0xffff].sdelay_c != 0) { // break no_sdm;
                r.al &= pw.partWk[r.di & 0xffff].sdelay_m;
            }
//no_sdm:
            pw.fmKeyOnDataTbl[r.getBx() & 0xffff] = r.al;
            r.dl |= r.al;
            opnset44();
            return;
        }
//ura_keyon:
        if (pw.board2 != 0) {
            r.addBx((short) 3); // offset ura_key1
            r.al = pw.fmKeyOnDataTbl[r.getBx() & 0xffff];
            r.al |= pw.partWk[r.di & 0xffff].slotmask;
            if (pw.partWk[r.di & 0xffff].sdelay_c != 0) { // break no_sdm2;
                r.al &= pw.partWk[r.di & 0xffff].sdelay_m;
            }
//no_sdm2:
            pw.fmKeyOnDataTbl[r.getBx() & 0xffff] = r.al;
            r.dl |= r.al;
            r.dl |= 0b0000_0100; // Ura Port
            opnset44();
        }
    }

    /**
     * PSG Key On
     */
    private void keyonp() {
        if (pw.partWk[r.di & 0xffff].onkai == (byte) 0xff) { // -1 // break ko1p;
            return; // when a rest
        }
//ko1p:
        //    pushf
        //    cli
        psgmsk(); // AL=07h AH = Maskdata
        r.al |= r.ah;
        r.ah &= pw.partWk[r.di & 0xffff].psgpat;
        r.ah = (byte) ~r.ah;
        r.al &= r.ah;
        r.dh = 7;
        r.dl = r.al;
        opnset44();
        //    popf
        //
        // PSG noise frequency set
        //
        r.dl = pw.psnoi;
        if (r.dl != pw.psnoi_last) { // break psnoi_ret; // If they are the same, do not define
            if ((pw.psgefcnum & 0x80) != 0) { // break psnoi_ret; // Do not change while PSG sound effects are being played
                r.dh = 6;
                opnset44();
                pw.psnoi_last = r.dl;
            }
        }
//psnoi_ret:
    }

    /**
     * Prepare KEYON/OFF for PSG 07h port (read 07H and calculate the mask value)
     * @output al <- 07h Read Data
     * @output ah <- Mask Data
     */
    private void psgmsk() {
        r.cl = pw.partb;
        r.al = 0;
        r.carry = true;
        int i = r.cl;
        while (i != 0) {
            boolean bc = (r.al & 0x80) != 0;
            r.al <<= 1;
            r.al |= (byte) ((r.carry) ? 1 : 0);
            r.carry = bc;
            i--;
        }
        r.ah = r.al;
        r.al <<= 3;
        r.ah |= r.al;
        get07();
    }

    /**
     * KEY OFF
     * don't Break AL
     */
    private void keyoff() {
        if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) {
            kof1();
        }
        // when a rest
    }

    private void kof1() {
        r.dh = 0x28;
        r.dl = pw.partb;
        r.dl--;

ura_keyoff: // ↑
        {
            r.bh = 0;
            r.bl = r.dl;
            if (pw.board2 != 0) {
                if (pw.fmsel != 0)
                    break ura_keyoff;
            }

            r.addBx((short) 0); // offset omote_key1 KUMA: Position to fmKeyOnDataTbl

            r.cl = pw.partWk[r.di & 0xffff].slotmask;
            r.cl = (byte) ~r.cl;
            r.cl &= pw.fmKeyOnDataTbl[r.getBx() & 0xffff];

            pw.fmKeyOnDataTbl[r.getBx() & 0xffff] = r.cl;
            r.dl |= r.cl;
            opnset44();
            return;
        }
//ura_keyoff:
        if (pw.board2 != 0) {
            r.addBx((short) 3); // offset ura_key1 KUMA: Position in fmKeyOnDataTbl (reverse is +3)

            r.cl = pw.partWk[r.di & 0xffff].slotmask;
            r.cl = (byte) ~r.cl;
            r.cl &= pw.fmKeyOnDataTbl[r.getBx() & 0xffff];

            pw.fmKeyOnDataTbl[r.getBx() & 0xffff] = r.cl;
            r.dl |= r.cl;
            r.dl |= 0b0100; // FM Ura Port
            opnset44();
        }
    }

    public void keyoffp() {
        if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) {
            kofp1();
        } else {
            // when a rest
        }
    }

    private void kofp1() {
        if (pw.partWk[r.di & 0xffff].envf != (byte) 0xff) { //  break kofp1_ext;
            pw.partWk[r.di & 0xffff].envf = 2;
        } else {
//kofp1_ext:
            pw.partWk[r.di & 0xffff].eenv_count = 4;
        }
    }

    /**
     * Setting the tone
     * @input dl [PARTB] [TONE_NUMBER]
     * @input di [PARTB] [PART_DATA_ADDRESS]
     */
    private void neiroset() {
        toneadr_calc();
        silence_fmpart();
        if (!r.carry) {
            neiroset_main();
        } else {
            // When neiromask=0 (only TL work is set)
            r.addBx((short) 4);
            neiroset_tl();
        }
    }

    //
    // Main tone settings
    //

    /**
     * Set AL/FB
     */
    private void neiroset_main() {
        r.dh = (byte) (0xb0 - 1);
        r.dh += pw.partb;
        if (pw.inst != null && (r.getBx() & 0xffff) + 24 < pw.inst.length) r.dl = (byte) pw.inst[(r.getBx() & 0xffff) + 24].dat;
        else r.dl = 0;

        if (pw.af_check != 0) { // Is ALG/FB not set? // break no_af;

            r.dl = pw.partWk[r.di & 0xffff].alg_fb;
        }
//no_af:
nss_notfm3: // ↑
        if (pw.partb == 3) { // break nss_notfm3;

            if (pw.board2 != 0) {
                if (pw.fmsel != 0)
                    break nss_notfm3;
            } else {
                if ((r.di & 0xffff) == pw.part_e)
                    break nss_notfm3;
            }

            if (pw.af_check != 0) { // Is this a mode where ALG/FB is not set? // break set_fm3_alg_fb;

                r.dl = pw.fm3_alg_fb;
//                break nss_notfm3;
            } else {
//set_fm3_alg_fb:
                if ((pw.partWk[r.di & 0xffff].slotmask & 0x10) == 0) { // If you are not using slot 1 // break nss_notslot1;

                    r.al = pw.fm3_alg_fb;
                    r.al &= 0b0011_1000; // fb uses previous value
                    r.dl &= 0b0000_0111;
                    r.dl |= r.al;
                }
//nss_notslot1:
                pw.fm3_alg_fb = r.dl;
            }
        }
//nss_notfm3:
        opnset();
        pw.partWk[r.di & 0xffff].alg_fb = r.dl;
        r.dl &= 7; // dl=algo

        check_carrier();
    }

    //
    // Check the carrier position (also set in VolMask)
    //
    private void check_carrier() {
        r.stack.push(r.getBx());
        r.bh = 0;
        r.bl = r.dl;
        r.addBx((short) 0); // offset carrier_table
        r.al = (byte) PW.carrier_table[r.getBx() & 0xffff];
        if ((pw.partWk[r.di & 0xffff].volmask & 0xf) == 0) { // break not_set_volmask; // Do not set if Volmask value is non-zero
            pw.partWk[r.di & 0xffff].volmask = r.al;
        }
//not_set_volmask:
        if ((pw.partWk[r.di & 0xffff]._volmask & 0xf) == 0) { // break not_set_volmask2;
            pw.partWk[r.di & 0xffff]._volmask = r.al;
        }
//not_set_volmask2:
        pw.partWk[r.di & 0xffff].carrier = r.al;
        r.ah = (byte) PW.carrier_table[(r.getBx() & 0xffff) + 8]; // Slot 2/3 reversal data (not completed)
        r.setBx(r.stack.pop());
        r.al = pw.partWk[r.di & 0xffff].neiromask;
        r.ah &= r.al; // AH=mask for TL / AL=mask for others

        //
        // Set each tone parameter (TL is modulator only)
        //
        r.dh = 0x30 - 1;
        r.dh += pw.partb;
        r.setCx((short) 4); // DT / ML
//ns01:
        do {
            if (pw.inst != null && (r.getBx() & 0xffff) < pw.inst.length) r.dl = (byte) pw.inst[r.getBx() & 0xffff].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) (((r.al & 0xff) << 1) | ((r.al & 0x80) >>> 7));
            if (r.carry) { // break ns_ns;
                opnset();
            }
//ns_ns:
            r.dh += 4;
            r.decCx();
        } while (r.getCx() != 0); // break ns01;
        r.setCx((short) 4); // TL
//ns01b:
        do {
            if (pw.inst != null && (r.getBx() & 0xffff) < pw.inst.length) r.dl = (byte) pw.inst[r.getBx() & 0xffff].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) (((r.al & 0xff) << 1) | ((r.al & 0x80) >>> 7));
            if (r.carry) { // break ns_nsb;
                opnset();
            }
//ns_nsb:
            r.dh += 4;
            r.decCx();
        } while (r.getCx() != 0); // break ns01b;

        r.setCx((short) 16); // rest
//ns01c:
        do {
            if (pw.inst != null && (r.getBx() & 0xffff) < pw.inst.length) r.dl = (byte) pw.inst[r.getBx() & 0xffff].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) (((r.al & 0xff) << 1) | ((r.al & 0x80) >>> 7));
            if (r.carry) { // break ns_nsc;
                opnset();
            }
//ns_nsc:
            r.dh += 4;
            r.decCx();
        } while (r.getCx() != 0); // break ns01c;

        //
        // Save TL for each slot in work
        //
        r.subBx((short) 20);
        neiroset_tl();
    }

    private void neiroset_tl() {
        r.stack.push(r.getSi());
        r.stack.push(r.di);

        r.setSi(r.getBx());
        //r.di += pw.slot1;

        if (pw.inst != null && (r.getSi() & 0xffff) + 3 < pw.inst.length) {
            pw.partWk[r.di & 0xffff].slot1 = (byte) pw.inst[(r.getSi() & 0xffff) + 0].dat;
            pw.partWk[r.di & 0xffff].slot3 = (byte) pw.inst[(r.getSi() & 0xffff) + 1].dat;
            pw.partWk[r.di & 0xffff].slot2 = (byte) pw.inst[(r.getSi() & 0xffff) + 2].dat;
            pw.partWk[r.di & 0xffff].slot4 = (byte) pw.inst[(r.getSi() & 0xffff) + 3].dat;
        }

        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
    }

    /**
     * Calculate TONE DATA START ADDRESS
     * @input dl tone_number
     * @output bx  address
     */
    private void toneadr_calc() {
        if (pw.prg_flg == 0) { // break prgdat_get;

            if ((r.di & 0xffff) != pw.part_e) { // break prgdat_get;

                r.setBx((short) pw.tondat);

                r.al = r.dl;
                r.ah = 0;
                r.addAx(r.getAx());
                r.addAx(r.getAx());
                r.addAx(r.getAx());
                r.addAx(r.getAx());
                r.addAx(r.getAx());
                r.addBx(r.getAx());
                return;
            }
        }
//prgdat_get:
        r.setBx((short) pw.prgdat_adr);
        if ((r.di & 0xffff) == pw.part_e) { // break gpd_loop;

            r.setBx(pw.prgdat_adr2); // For FM sound effects
        }
//gpd_loop:
        while (true) {
            pw.inst = pw.md;
            if ((r.getBx() & 0xffff) >= pw.inst.length) {
                throw new PmdException("The tone number you are looking for could not be found.");
            }
            if (pw.inst[r.getBx() & 0xffff].dat == (r.dl & 0xff))
                break; // gpd_exit;
            r.addBx((short) 26);
//            break gpd_loop;
        }
//gpd_exit:
        r.incBx();
    }

    /**
     * Completely erase the sound of [Part B](TL= 127 and RR = 15 and KEY-OFF)
     * cy=1 ... All slots are neiromask
     */
    private void silence_fmpart() {
        r.al = pw.partWk[r.di & 0xffff].neiromask;
        if (r.al != 0) { // break sfm_exit;

            r.stack.push(r.getDx());
            r.dh = pw.partb;
            r.dh += 0x40 - 1;
            r.setCx((short) 4);
            r.dl = 127; // TL = 127 / RR=15
//ns00c:
            do {
                r.carry = ((r.al & 0x80) != 0);
                r.al = (byte) (((r.al & 0xff) << 1) | ((r.al & 0x80) >>> 7));
                if (r.carry) { // break ns00d;
                    opnset();
                    r.dh += 0x40;
                    opnset();
                    r.dh -= 0x40;
                }
//ns00d:
                r.dh += 4;
                r.decCx();
            } while (r.getCx() != 0); // break ns00c;

            r.stack.push(r.getBx());
            kof1(); // KEY OFF
            r.setBx(r.stack.pop());

            r.setDx(r.stack.pop()); //    pop dx
            r.carry = false;
            return;
        }
//sfm_exit:
        r.carry = true;
    }

    /**
     * LFO Processing
     * Don't Break cl
     * @output cy = 1 There has been a change
     */
    public void lfo() {
        //lfop:;
        if (pw.partWk[r.di & 0xffff].delay != 0) { // break lfo1;
            pw.partWk[r.di & 0xffff].delay--; // cy=0
//lfo_ret:
            return;
        }
//lfo1:
        if ((pw.partWk[r.di & 0xffff].extendmode & 2) != 0) { // Should I match it with TimerA? // break lfo_normal; // If not, process lfo unconditionally
            r.ch = pw.timerATime;
            r.ch -= pw.lastTimerAtime;
            if (r.ch == 0) {
//                break lfo_ret; // If it is the same as the previous value, do nothing. cy = 0
                return; // <<
            }

            r.setAx(pw.partWk[r.di & 0xffff].lfodat);
            r.stack.push(r.getAx());

//lfo_loop:
            do {
                lfo_main();
                r.ch--;
            } while (r.ch != 0); // break lfo_loop;

//            break lfo_check;
        } else {
//lfo_normal:
            r.setAx(pw.partWk[r.di & 0xffff].lfodat);
            r.stack.push(r.getAx());
            lfo_main();
        }
//lfo_check:
        r.setAx(r.stack.pop());

        if (r.getAx() == pw.partWk[r.di & 0xffff].lfodat) { // break lfo_stc_ret;
            return; // c=0
        }
//lfo_stc_ret:
        r.carry = true;
    }

    private void lfop() {
        lfo();
    }

    private void lfo_main() {
        if (pw.partWk[r.di & 0xffff].speed != 1) { // break lfo2;
            if (pw.partWk[r.di & 0xffff].speed != (byte) 0xff) { // -1 // break lfom_ret;
                pw.partWk[r.di & 0xffff].speed--;
            }
//lfom_ret:
            return;
        }
//lfo2:
not_nokogiri: // ↑
        {
lfo_oneshot: // ↑
            {
not_sankaku: // ↑
                {
lfo20: // ↑
                    {
                        r.al = pw.partWk[r.di & 0xffff].speed2;
                        pw.partWk[r.di & 0xffff].speed = r.al;
                        r.bl = pw.partWk[r.di & 0xffff].lfo_wave;
                        if (r.bl != 0) { // break lfo_sankaku;
                            if (r.bl != 4) { // break lfo_sankaku;
                                if (r.bl != 2) { // break lfo_kukei;
                                    if (r.bl == 6)
                                        break lfo_oneshot;
                                    if (r.bl != 5)
                                        break not_sankaku;
                                    // Triangle wave lfowave = 0,4,5
                                    r.al = pw.partWk[r.di & 0xffff].step;
                                    r.ah = r.al;
                                    if ((r.ah & 0x80) != 0) { // break lfo2ns;
                                        r.ah = (byte) -r.ah;
                                    }
//lfo2ns:
                                    r.setAx((short) (/* signed */ r.al * (r.ah & 0xff))); // When lfowave=5 1step = step×｜step｜
                                    break lfo20;
                                } else {
//lfo_kukei: dup
                                    // Square wave lfowave = 2
                                    r.al = pw.partWk[r.di & 0xffff].step;
                                    r.setAx((short) (/* signed */ r.al * /* signed */ pw.partWk[r.di & 0xffff].time));
                                    pw.partWk[r.di & 0xffff].lfodat = r.getAx();
                                    md_inc();
                                    pw.partWk[r.di & 0xffff].step = (byte) -pw.partWk[r.di & 0xffff].step;
                                    return;
                                }
                            }
                        }
//lfo_sankaku:
                        r.al = pw.partWk[r.di & 0xffff].step;
                        r.setAx(/* signed */ r.al); // cbw
                    }
//lfo20:
                    pw.partWk[r.di & 0xffff].lfodat += r.getAx();
                    if (pw.partWk[r.di & 0xffff].lfodat == 0) { // break lfo21;
                        md_inc();
                    }
//lfo21:
                    r.al = pw.partWk[r.di & 0xffff].time;
                    if (r.al != (byte) 255) { // break lfo3;
                        r.al--;
                        if (r.al == 0) { // break lfo3;
                            r.al = pw.partWk[r.di & 0xffff].time2;
                            if (r.bl != 4) { // break lfo22;
                                r.al += r.al; // lfowave=0,5: Double the time when inverted
                            }
//lfo22:
                            pw.partWk[r.di & 0xffff].time = r.al;
                            r.al = pw.partWk[r.di & 0xffff].step;
                            r.al = (byte) -r.al;
                            pw.partWk[r.di & 0xffff].step = r.al;
                            return;
                        }
                    }
//lfo3:
                    pw.partWk[r.di & 0xffff].time = r.al;
                    return;
                }
//not_sankaku:
                r.bl--;
                if (r.bl != 0)
                    break not_nokogiri;
                // sawtooth lfowave = 1,6
                r.al = pw.partWk[r.di & 0xffff].step;
                r.setAx(/* signed */ r.al); // cbw
                pw.partWk[r.di & 0xffff].lfodat += r.getAx();
                r.al = pw.partWk[r.di & 0xffff].time;
                if (r.al != (byte) 0xff) { // -1 // break nk_lfo3;
                    r.al--;
                    if (r.al == 0) { // break nk_lfo3;
                        pw.partWk[r.di & 0xffff].lfodat = (short) -pw.partWk[r.di & 0xffff].lfodat;
                        md_inc();

                        r.al = pw.partWk[r.di & 0xffff].time2;
                        r.al += r.al;
                    }
                }
//nk_lfo3:
                pw.partWk[r.di & 0xffff].time = r.al;
                return;
            }
//lfo_oneshot:
            // one shot lfowave = 6
            r.al = pw.partWk[r.di & 0xffff].time;
            if (r.al != 0) { // break lfoone_ret;
                if (r.al != (byte) 0xff) { // -1 // break lfoone_nodec;
                    r.al--;
                    pw.partWk[r.di & 0xffff].time = r.al;
                }
//lfoone_nodec:
                r.al = pw.partWk[r.di & 0xffff].step;
                r.setAx(/* signed */ r.al); // cbw
                pw.partWk[r.di & 0xffff].lfodat += r.getAx();
            }
//lfoone_ret:
            return;

//lfo_kukei: // dup ↑
            // Square wave lfowave = 2
//            r.al = pw.partWk[r.di & 0xffff].step;
//            r.setAx((short) ((byte) r.al * (byte) pw.partWk[r.di & 0xffff].time));
//            pw.partWk[r.di & 0xffff].lfodat = r.getAx();
//            md_inc();
//            pw.partWk[r.di & 0xffff].step = (byte) -pw.partWk[r.di & 0xffff].step;
//            return;
        }
//not_nokogiri:
        // Random wave lfowave = 3
        r.al = pw.partWk[r.di & 0xffff].step;
        if ((r.al & 0x80) != 0) { // break ns_plus;
            r.al = (byte) -r.al;
        }
//ns_plus:
        r.setAx((short) ((r.al & 0xff) * (pw.partWk[r.di & 0xffff].time & 0xff)));
        r.stack.push(r.getAx());
        r.stack.push(r.getCx());
        r.addAx(r.getAx());
        rnd();
        r.setCx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.subAx(r.getBx());
        pw.partWk[r.di & 0xffff].lfodat = r.getAx();

        md_inc();
    }

    /**
     * Change STEP value according to MD command value
     */
    private void md_inc() {
        pw.partWk[r.di & 0xffff].mdspd--;
        if (pw.partWk[r.di & 0xffff].mdspd != 0) {
//            break md_exit;
            return;
        }
        r.al = pw.partWk[r.di & 0xffff].mdspd2;
        pw.partWk[r.di & 0xffff].mdspd = r.al;
        r.al = pw.partWk[r.di & 0xffff].mdc;
        if (r.al == 0) {
//            break md_exit; // count =0
            return;
        }
        if ((r.al & 0x80) == 0) { // break mdi21; // count > 127 (255)
            r.al--;
            pw.partWk[r.di & 0xffff].mdc = r.al;
        }
//mdi21:
        r.al = pw.partWk[r.di & 0xffff].step;
        if ((r.al & 0x80) != 0) { // break mdi22;
            r.al = (byte) -r.al;
            r.al += pw.partWk[r.di & 0xffff].mdepth;
            if ((r.al & 0x80) == 0) { // break mdi21_ov;
                r.al = (byte) -r.al;
//mdi21_s:
                pw.partWk[r.di & 0xffff].step = r.al;

//md_exit:
                return;
            }
//mdi21_ov:
            r.al = 0;
            if ((pw.partWk[r.di & 0xffff].mdepth & 0x80) == 0) { // break mdi21_s;
                r.al = (byte) 0x81; // -127;
            }
//            break mdi21_s;
            pw.partWk[r.di & 0xffff].step = r.al; // <<
            return; // <<
        }
//mdi22:
        r.al += pw.partWk[r.di & 0xffff].mdepth;
        if ((r.al & 0x80) != 0) { // break mdi22_ov;
//mdi22_ov:
            r.al = 0;
            if ((pw.partWk[r.di & 0xffff].mdepth & 0x80) == 0) { // break mdi22_s;
                r.al = 0x7f;
            }
//            break mdi22_s;
        }
//mdi22_s:
        pw.partWk[r.di & 0xffff].step = r.al; // ↓
    }

    /**
     * Random number generator
     * INPUT : AX=MAX_RANDOM
     * OUTPUT: AX=RANDOM_NUMBER
     */
    private void rnd() {
        r.setCx(r.getAx());
        r.setAx((short) 259);

        r.setAx((short) ((r.getAx() & 0xffff) * (pw.seed & 0xffff)));
        r.addAx((short) 3);
        r.andAx((short) 32767); // 0x7fff

        pw.seed = r.getAx();
        int ans = (r.getAx() & 0xffff) * (r.getCx() & 0xffff);
        r.setCx((short) 32767);
        r.setAx((short) (ans / (r.getCx() & 0xffff)));
        r.setDx((short) (ans % (r.getCx() & 0xffff)));
    }

    //
    // Initialization of LFO and PSG/PCM software envelopes
    //

    /**
     * Entry for PSG/PCM sound source
     */
    public void lfoinitp() {
        r.ah = r.al; // Do not INIT when it's a rest.
        r.ah &= 0xf;
        if (r.ah == 0xc) { // break lip_00;
            r.al = pw.partWk[r.di & 0xffff].onkai_def;
            r.ah = r.al;
            r.ah &= 0xf;
        }
//lip_00:
        pw.partWk[r.di & 0xffff].onkai_def = r.al;

        if (r.ah != 0xf) { // 4.8r Fixes // break lfo_exitp;
            pw.partWk[r.di & 0xffff].porta_num = 0; // Portamento is initialized

            if ((pw.tieflag & 1) == 0) { // It will not be INITed even if there is an '&' in front.
                seinit();
                return;
            }
        }
//lfo_exitp:
        r.stack.push(r.getAx());
        soft_env(); // If there is '&' in front -> SoftEnv process once
        r.setAx(r.stack.pop());

        lfo_exit();
        // to this point
    }

    /**
     * Software envelope initialization
     */
    private void seinit() {
        if (pw.partWk[r.di & 0xffff].envf != (byte) 0xff) { // break extenv_init;

            pw.partWk[r.di & 0xffff].envf = 0;
            pw.partWk[r.di & 0xffff].eenv_volume = 0; // .penv

            r.ah = pw.partWk[r.di & 0xffff].eenv_arc; // .patb
            pw.partWk[r.di & 0xffff].eenv_ar = r.ah; // .pat
            if (r.ah == 0) { // break lfin2;
                pw.partWk[r.di & 0xffff].envf = 1; // ATTACK=0 ... Immediate Decay
                r.ah = pw.partWk[r.di & 0xffff].eenv_dr; // .pv2
                pw.partWk[r.di & 0xffff].eenv_volume = r.ah; // .penv
            }
//lfin2:
            r.ah = pw.partWk[r.di & 0xffff].eenv_src; // .pr1b
            pw.partWk[r.di & 0xffff].eenv_sr = r.ah; // .pr1
            r.ah = pw.partWk[r.di & 0xffff].eenv_rrc; // .pr2b
            pw.partWk[r.di & 0xffff].eenv_rr = r.ah; // .pr2
            lfin1();
            return;
        }
        // For extended ssg_envelope
//extenv_init:
        r.ah = pw.partWk[r.di & 0xffff].eenv_ar;
        r.ah -= 16;
        pw.partWk[r.di & 0xffff].eenv_arc = r.ah;
        r.ah = pw.partWk[r.di & 0xffff].eenv_dr;
        r.ah -= 16;
        if ((r.ah & 0x80) != 0) { // break eei_dr_notx;
            r.ah += r.ah;
        }
//eei_dr_notx:
        pw.partWk[r.di & 0xffff].eenv_drc = r.ah;

        r.ah = pw.partWk[r.di & 0xffff].eenv_sr;
        r.ah -= 16;
        if ((r.ah & 0x80) != 0) { // break eei_sr_notx;
            r.ah += r.ah;
        }
//eei_sr_notx:
        pw.partWk[r.di & 0xffff].eenv_src = r.ah;

        r.ah = pw.partWk[r.di & 0xffff].eenv_rr;
        r.ah += r.ah;
        r.ah -= 16;
        pw.partWk[r.di & 0xffff].eenv_rrc = r.ah;

        r.ah = pw.partWk[r.di & 0xffff].eenv_al;
        pw.partWk[r.di & 0xffff].eenv_volume = r.ah;
        pw.partWk[r.di & 0xffff].eenv_count = 1;

        r.stack.push(r.getAx());
        ext_ssgenv_main(); // The first time
        r.setAx(r.stack.pop());

        lfin1();
    }

    /**
     * FM Sound Source Entry
     */
    private void lfoinit() {
        r.ah = r.al; // Do not INIT when it's a rest.
        r.ah &= 0xf;
        if (r.ah == 0xc) { // break li_00;
            r.al = pw.partWk[r.di & 0xffff].onkai_def;
            r.ah = r.al;
            r.ah &= 0xf;
        }
//li_00:
        pw.partWk[r.di & 0xffff].onkai_def = r.al;

        if (r.ah == 0xf) {
            lfo_exit();
            return;
        }
        pw.partWk[r.di & 0xffff].porta_num = 0; // Portamento is initialized

        if ((pw.tieflag & 1) == 0) { // It will not be INITed even if there is an '&' in front.
            lfin1();
            return;
        }
        lfo_exit();
    }

    private void lfo_exit() {
        if ((pw.partWk[r.di & 0xffff].lfoswi & 3) != 0) { // Is the LFO in use? // break le_no_one_lfo1; // If preceded by '&' -> LFO process once

            r.stack.push(r.getAx());
            lfo();
            r.setAx(r.stack.pop());
        }
//le_no_one_lfo1:
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x30) != 0) { // Is the LFO in use? // break le_no_one_lfo2; // If preceded by '&' -> LFO process once

            r.stack.push(r.getAx());
            //    pushf
            //    cli
            lfo_change();
            lfo();
            lfo_change();
            //    popf
            r.setAx(r.stack.pop());
        }
//le_no_one_lfo2:
    }

    /**
     * LFO Initialization
     */
    private void lfin1() {
        if (pw.board2 != 0) {
            r.ah = pw.partWk[r.di & 0xffff].hldelay;
            pw.partWk[r.di & 0xffff].hldelay_c = r.ah;
            if (r.ah != 0) { // break non_hldelay;
                r.dh = pw.partb; //    mov dh,[partb]
                r.dh += (byte) (0xb4 - 1);
                r.dl = pw.partWk[r.di & 0xffff].fmpan;
                r.dl &= (byte) 0xc0; // HLFO = OFF
                opnset();
            }
//non_hldelay:
        }

        r.ah = pw.partWk[r.di & 0xffff].sdelay;
        pw.partWk[r.di & 0xffff].sdelay_c = r.ah;
        r.cl = pw.partWk[r.di & 0xffff].lfoswi;
        if ((r.cl & 3) != 0) { // break li_lfo1_exit; // LFO not used
            if ((r.cl & 4) == 0) { // Keyon asynchronous? break li_lfo1_next;
                lfoinit_main();
            }
//li_lfo1_next:
            r.stack.push(r.getAx());
            lfo();
            r.setAx(r.stack.pop());
        }
//li_lfo1_exit:
        if ((r.cl & 0x30) != 0) { // break li_lfo2_exit; // LFO not used
            if ((r.cl & 0x40) == 0) { // Keyon asynchronous? // break li_lfo2_next;

                r.stack.push(r.getAx());
                // pushf
                //    cli
                lfo_change();
                lfoinit_main();
                lfo_change();
                //    popf
                r.setAx(r.stack.pop());
            }
//li_lfo2_next:
            r.stack.push(r.getAx());
            // pushf
            //    cli
            lfo_change();
            lfo();
            lfo_change();
            //    popf
            r.setAx(r.stack.pop());
        }
//li_lfo2_exit:
        //    ret
    }

    private Supplier<Object> lfoinit_main() {
        pw.partWk[r.di & 0xffff].lfodat = 0;
        r.dl = pw.partWk[r.di & 0xffff].delay2; //    mov dx, word ptr delay2[di]
        r.dh = pw.partWk[r.di & 0xffff].speed2;
        pw.partWk[r.di & 0xffff].delay = r.dl;
        pw.partWk[r.di & 0xffff].speed = r.dh;
        r.dl = pw.partWk[r.di & 0xffff].step2; //    mov dx, word ptr step2[di]
        r.dh = pw.partWk[r.di & 0xffff].time2;
        pw.partWk[r.di & 0xffff].step = r.dl;
        pw.partWk[r.di & 0xffff].time = r.dh;

        r.dl = pw.partWk[r.di & 0xffff].mdc2;
        pw.partWk[r.di & 0xffff].mdc = r.dl;

        if (pw.partWk[r.di & 0xffff].lfo_wave == 2) { // Square wave or
//            break lim_first;
//lim_first:
            pw.partWk[r.di & 0xffff].speed = 1; // Apply the LFO immediately after the delay
            return null;
        }
        if (pw.partWk[r.di & 0xffff].lfo_wave != 3) { // In the case of random waves
//            break lim_nofirst;
//lim_nofirst:
            pw.partWk[r.di & 0xffff].speed++; // Otherwise, the speed value immediately after the delay is increased by +1.
            return null;
        }
        pw.partWk[r.di & 0xffff].speed = 1; // Apply the LFO immediately after the delay
        return null;
    }

    /**
     * PSG/PCM software envelope
     */
    public void soft_env() {
        if ((pw.partWk[r.di & 0xffff].extendmode & 4) == 0) // sync it with TimerA?
        {
            soft_env_main(); // If not, unconditionally process senv
            return;
        }

        r.ch = pw.timerATime;
        r.ch -= pw.lastTimerAtime;
        if (r.ch != 0) { // break senv_ret; // If it is the same as the previous value, do nothing. cy = 0
            r.cl = 0;

//senv_loop:
            do {
                soft_env_main();
                if (r.carry) { // break sel00;
                    r.cl = 1;
                }
//sel00:
                r.ch--;
            } while (r.ch != 0); // break senv_loop;
            r.cl = r.ror(r.cl, 1); // cy setting
        }
//senv_ret:
    }

    private void soft_env_main() {
        if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // -1
            ext_ssgenv_main();
            return;
        }

        r.dl = pw.partWk[r.di & 0xffff].eenv_volume; // .penv;
        soft_env_sub();
        r.carry = false;
        if (r.dl != pw.partWk[r.di & 0xffff].eenv_volume) { // .penv // break sem_ret; // cy=0
            r.carry = true;
        }
//sem_ret:
    }

    private void soft_env_sub() {
        if (pw.partWk[r.di & 0xffff].envf == 0) { // -1 // break se1;

            //
            // Attack
            //
            pw.partWk[r.di & 0xffff].eenv_ar--; // .pat--;
            if (pw.partWk[r.di & 0xffff].eenv_ar != 0) {
//                break se2;
                return;
            }

            pw.partWk[r.di & 0xffff].envf = 1;
            r.al = pw.partWk[r.di & 0xffff].eenv_dr; // pv2[di]
            pw.partWk[r.di & 0xffff].eenv_volume = r.al; // penv[di]
            r.carry = true;
            return;
        }
//se1:
        if (pw.partWk[r.di & 0xffff].envf != 2) { // break se3;

            //
            // Decay
            //
            if (pw.partWk[r.di & 0xffff].eenv_sr == 0) {
//                break se2; // When DR=0, there is no attenuation.
                return;
            }
            pw.partWk[r.di & 0xffff].eenv_sr--;
            if (pw.partWk[r.di & 0xffff].eenv_sr != 0) {
//                break se2;
                return;
            }

            r.al = pw.partWk[r.di & 0xffff].eenv_src; // pr1b[di]
            pw.partWk[r.di & 0xffff].eenv_sr = r.al; // pr1[di]
            pw.partWk[r.di & 0xffff].eenv_volume--; // penv[di]

//se4:
            if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < 0xf1) { // -15 // break se2;
                if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) >= 15) { // break se2;
//se5:
                    pw.partWk[r.di & 0xffff].eenv_volume = (byte) 0xf1; // mov penv[di],-15
                }
            }
//se2:
            return;
        }
        //
        // Release
        //
//se3:
        if (pw.partWk[r.di & 0xffff].eenv_rr == 0) { // pr2
//            break se5; // When RR=0, the sound is muted immediately.
            pw.partWk[r.di & 0xffff].eenv_volume = (byte) 0xf1; // mov penv[di],-15
            return;
        }
        pw.partWk[r.di & 0xffff].eenv_rr--; // pr2[di]
        if (pw.partWk[r.di & 0xffff].eenv_rr != 0) {
//            break se2;
            return;
        }
        r.al = pw.partWk[r.di & 0xffff].eenv_rrc; // pr2b[di]
        pw.partWk[r.di & 0xffff].eenv_rr = r.al; // pr2[di]
        pw.partWk[r.di & 0xffff].eenv_volume--; // penv[di]
//        break se4;
        if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < 0xf1) { // -15 // break se2;
            if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) >= 15) { // break se2;
                pw.partWk[r.di & 0xffff].eenv_volume = (byte) 0xf1; // mov penv[di],-15
            }
        }
    }

    // Extended version
    private void ext_ssgenv_main() {
        r.ah = pw.partWk[r.di & 0xffff].eenv_count;
        if (r.ah == 0) { // break esm_main2;
//esm_ret:
            r.carry = false;
            return; // cy=0
        }
//esm_main2:
        r.dl = pw.partWk[r.di & 0xffff].eenv_volume;
        esm_sub();
        if (r.dl == pw.partWk[r.di & 0xffff].eenv_volume) {
            r.carry = false;
//            break esm_ret; // cy=0
            return;
        }
        r.carry = true;
    }

    private void esm_sub() {
//esm_ar_check:
        r.ah--;
        if (r.ah == 0) { // break esm_dr_check;
            //
            // Attack Rate
            //
            r.al = pw.partWk[r.di & 0xffff].eenv_arc;
            r.al--;
            if ((r.al & 0x80) == 0) { // break arc_count_check; // If it is less than 0, check the count.
                r.al++;
                pw.partWk[r.di & 0xffff].eenv_volume += r.al;
                if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < 15) { // break esm_ar_next;
                    r.ah = pw.partWk[r.di & 0xffff].eenv_ar;
                    r.ah -= 16;
                    pw.partWk[r.di & 0xffff].eenv_arc = r.ah;
                    return;
                }
//esm_ar_next:
                pw.partWk[r.di & 0xffff].eenv_volume = 15;
                pw.partWk[r.di & 0xffff].eenv_count++;
                if (pw.partWk[r.di & 0xffff].eenv_sl != 15) // If SL=0, go to SR immediately
                    return; // break esm_ret;
                pw.partWk[r.di & 0xffff].eenv_count++;
                return;
            }
//arc_count_check:
            if (pw.partWk[r.di & 0xffff].eenv_ar == 0) // AR=0?
                return; // break esm_ret;
            pw.partWk[r.di & 0xffff].eenv_arc++;
            return;
        }
//esm_dr_check:
        r.ah--;
        if (r.ah == 0) { // break esm_sr_check;
            //
            // Decay Rate
            //
            r.al = pw.partWk[r.di & 0xffff].eenv_drc;
            r.al--;
            if ((r.al & 0x80) == 0) { // break drc_count_check; // If it is less than 0, check the count.
                r.al++;
                r.carry = (pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < (r.al & 0xff);
                pw.partWk[r.di & 0xffff].eenv_volume -= r.al;
                r.al = pw.partWk[r.di & 0xffff].eenv_sl;
                if (!r.carry) { // break dr_slset;
                    if ((pw.partWk[r.di & 0xffff].eenv_volume & 0xff) >= (r.al & 0xff)) { // break dr_slset;
                        r.ah = pw.partWk[r.di & 0xffff].eenv_dr;
                        r.ah -= 16;
                        if ((r.ah & 0x80) != 0) { // break esm_dr_notx;
                            r.ah += r.ah;
                        }
//esm_dr_notx:
                        pw.partWk[r.di & 0xffff].eenv_drc = r.ah;
                        return;
                    }
                }
//dr_slset:
                pw.partWk[r.di & 0xffff].eenv_volume = r.al;
                pw.partWk[r.di & 0xffff].eenv_count++;
                return;
            }
//drc_count_check:
            if (pw.partWk[r.di & 0xffff].eenv_dr == 0) // DR=0?
                return; // break esm_ret;
            pw.partWk[r.di & 0xffff].eenv_drc++;
            return;
        }
//esm_sr_check:
        r.ah--;
        if (r.ah == 0) { // break esm_rr;
            //
            // Sustain Rate
            //
            r.al = pw.partWk[r.di & 0xffff].eenv_src;
            r.al--;
            if ((r.al & 0x80) == 0) { // break src_count_check; // If it is less than 0, check the count.
                r.al++;
                r.carry = (pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < (r.al & 0xff);
                pw.partWk[r.di & 0xffff].eenv_volume -= r.al;
                if (r.carry) { // break esm_sr_exit;
                    pw.partWk[r.di & 0xffff].eenv_volume = 0;
                }
//esm_sr_exit:
                r.ah = pw.partWk[r.di & 0xffff].eenv_sr;
                r.ah -= 16;
                if ((r.ah & 0x80) != 0) { // break esm_sr_notx;
                    r.ah += r.ah;
                }
//esm_sr_notx:
                pw.partWk[r.di & 0xffff].eenv_src = r.ah;
                return;
            }
//src_count_check:
            if (pw.partWk[r.di & 0xffff].eenv_sr == 0) // SR=0?
                return; // break esm_ret;
            pw.partWk[r.di & 0xffff].eenv_src++;
            return;
        }
//esm_rr:
        //
        // Release Rate
        //
        r.al = pw.partWk[r.di & 0xffff].eenv_rrc;
        r.al--;
        if ((r.al & 0x80) == 0) { // break rrc_count_check; // If it is less than 0, check the count.
            r.al++;
            r.carry = (pw.partWk[r.di & 0xffff].eenv_volume & 0xff) < (r.al & 0xff);
            pw.partWk[r.di & 0xffff].eenv_volume -= r.al;
            if (r.carry) { // break esm_rr_exit;
                pw.partWk[r.di & 0xffff].eenv_volume = 0;
            }
//esm_rr_exit:
            r.ah = pw.partWk[r.di & 0xffff].eenv_rr;
            r.ah += r.ah;
            r.ah -= 16;
            pw.partWk[r.di & 0xffff].eenv_rrc = r.ah;
            return;
        }
//rrc_count_check:
        if (pw.partWk[r.di & 0xffff].eenv_rr == 0) // RR=0?
            return; // break esm_ret;
        pw.partWk[r.di & 0xffff].eenv_rrc++;
    }

    /**
     * FADE IN / OUT ROUTINE
     *
     *  FROM Timer-A
     */
    private void fadeout() {
        if (pw.pause_flag == 1) { // No fadeout during pause
//            break fade_exit;
            return; // <<
        }
        r.al = pw.fadeout_speed;
        if (r.al == 0) {
//            break fade_exit;
            return; // <<
        }
        if ((r.al & 0x80) == 0) { // break fade_in;

            r.carry = (r.al & 0xff) + (pw.fadeout_volume & 0xff) > 0xff;
            r.al += pw.fadeout_volume;
            if (!r.carry) { // break fadeout_end;

                pw.fadeout_volume = r.al;
                return;
            }
//fadeout_end:
            pw.fadeout_volume = (byte) 255;
            pw.fadeout_speed = 0;
            if (pw.fade_stop_flag == 1) { // break fade_exit;

                pw.music_flag |= 2;
            }
//fade_exit:
            return;
        }
//fade_in:
        r.carry = (r.al & 0xff) + (pw.fadeout_volume & 0xff) > 0xff;
        r.al += pw.fadeout_volume;
        if (r.carry) { // break fadein_end;

            pw.fadeout_volume = r.al;
            return;
        }
//fadein_end:
        pw.fadeout_volume = 0;
        pw.fadeout_speed = 0;
        if (pw.board2 != 0) {
            r.dl = pw.rhyvol;
            volset2rf();
        }
    }

    /**
     * Interrupt Settings
     * For FM sound source only
     */
    private void setint() {
        //pushf
        //cli // Disable interrupts
        //
        // OPN interrupt initial setting
        //
        pw.tempo_d = (byte) 200; // TIMER B SET
        pw.tempo_d_push = (byte) 200;

        calc_tb_tempo();
        settempo_b();

        r.setDx((short) 0x2500);
        opnset44();
        r.setDx((short) 0x2400); // TIMER A SET(Fixed at 9216μs)

        opnset44(); // The slowest and best

        r.dh = 0x27;
        r.dl = 0b0011_1111; // TIMER ENABLE

        opnset44();

        //    popf

        //
        // Bar Counter Reset
        //
        r.setAx((short) 0);
        pw.opncount = r.al;
        pw.syousetu = r.getAx();
        pw.syousetu_lng = 96;
    }

    /**
     * ALL SILENCE
     */
    private void silence() {
        if (pw.board2 != 0) {
            sel44(); // It's okay because it won't fly to the main
            r.ah = 2;
        }
        oploop();
    }

    private void oploop() { // TODO vavi re-check
        byte[] bxTbl = null;
        if (pw.fm_effec_flag == 1) { // break opi_nef;
            if (pw.board2 != 0) {
                if (r.ah != 1) {
//                    break opi_nef;
                    bxTbl = PW.fmoff_nef; // <<
                    r.setBx((short) 0); // offset fmoff_nef // <<
                }
            } else {

                bxTbl = PW.fmoff_ef;
                r.setBx((short) 0); // offset fmoff_ef
//            break opi_ef;
            }
        } else {
//opi_nef:
            bxTbl = PW.fmoff_nef;
            r.setBx((short) 0); // offset fmoff_nef
        }
//opi_ef:

//opi0:
        while (true) {
            r.dh = bxTbl[r.getBx() & 0xffff];
            r.incBx();
            if (r.dh == (byte) 0xff) break; // opi1b;

            r.dh += (byte) 0x80;
            r.dl = (byte) 0xff; // FM Release = 15
            opnset();
//            break opi0;
        }
//opi1b:
        if (pw.board2 != 0) {
            r.stack.push(r.getAx());
            sel46(); // It's okay because it won't fly to the main
            r.setAx(r.stack.pop());
            r.ah--;
            if (r.ah != 0) {
                oploop();
                return;
            }
        }

        r.setDx((short) 0x2800); // FM KEYOFF
        r.setCx((short) 3);
        if (pw.board2 == 0) {
            if (pw.fm_effec_flag == 1) { // break opi1;
                r.decCx();
            }
        }

//opi1:
        do {
            opnset44();
            r.dl++;
            r.decCx();
        } while ((r.getCx() & 0xffff) > 0);

        if (pw.board2 != 0) {
            r.setDx((short) 0x2804); // FM KEYOFF[URA]
            r.setCx((short) 3);
            if (pw.fm_effec_flag == 1) { // break opi2;
                r.decCx();
            }
//opi2:
            do {
                opnset44();
                r.dl++;
                r.decCx();
            } while ((r.getCx() & 0xffff) > 0);
        }

        if (pw.effon == 0) { // break psg_ef;
            if (pw.ppsdrv_flag != 0) { // break opi_nonppsdrv;

                r.ah = 0;
                ChipDatum cd = new ChipDatum(0x02, 0, 0);
                ppsdrv.apply(cd); // .Stop(); // ppsdrv keyoff
            }
//opi_nonppsdrv:
            r.setDx((short) 0x07bf); // PSG KEYOFF
            opnset44();
//            break s_pcm;
        } else {
//psg_ef:
            // pushf
            //    cli
            get07();
            r.dl = r.al;
            r.dl &= 0b0011_1111;
            r.dl |= (byte) 0b1001_1011;
            r.dh = 0x7;
            opnset44();
            //    popf
        }
//s_pcm:
pcm_ef: // ↑
        if (pw.board2 != 0) {
            if (pw.pcmflag == 0) { // break pcm_ef; // PCM sound effect being played?
                if (pw.adpcm != 0) {
                    if (pw.ademu == 0) {
                        if (pw.pcm_gs_flag == 1) break pcm_ef;
                        r.setDx((short) 0x0102); // PAN=0 / x8 bit mode
                        opnset46();
                        r.setDx((short) 0x0001); // PCM RESET
                        opnset46();
                    }
                }
                r.setDx((short) 0x1080); // RESET TA/TB/EOS
                opnset46();
                r.setDx((short) 0x1018); // Only TIMERB/A/EOS bits changed
                opnset46(); // (It also works with NEC sound sources)
                if (pw.pcm != 0) {
                    pcmdrv86.stop_86pcm();
                }
            }
//pcm_ef:
            if (pw.ppz != 0) {
                if (pw.ppz_call_seg != 0) {
                    r.ah = 0x12;
                    ChipDatum cd = new ChipDatum(0x12, 0, 0);
                    ppz8em.apply(cd); // .StopInterrupt(); // FIFO interrupt stop
                    r.setAx((short) 0x0200);
//ppz_off_loop:
                    do {
                        r.stack.push(r.getAx());
                        cd = new ChipDatum(0x02, r.al, 0);
                        ppz8em.apply(cd); // .StopPCM(r.al); // ppz keyoff
                        r.setAx(r.stack.pop());
                        r.al++;
                    } while ((r.al & 0xff) < 8); // break ppz_off_loop;
                }
//_not_ppz8:
            }
        }
    }

    /**
     * SET DATA TO OPN
     * INPUTS ---- D,E
     */
    //
    // Fore
    //
    public void opnset44() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());
        r.stack.push(r.getBx());

        r.setBx(r.getDx());
        r.setDx((short) pw.fm1_port1);

        //    pushf
        //    cli

        rdychk();
        r.al = r.bh;
        pc98.outPort(r.getDx(), r.al);
        _waitP();
        r.setDx((short) pw.fm1_port2);
        r.al = r.bl;
        pc98.outPort(r.getDx(), r.al);

        //    popf

        r.setBx(r.stack.pop());
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //
    // Back
    //
    public void opnset46() {
        if (pw.board2 != 0) {
            r.stack.push(r.getAx());
            r.stack.push(r.getBx());
            r.stack.push(r.getDx());

            r.setBx(r.getDx());
            r.setDx((short) pw.fm2_port1);

            //    pushf
            //    cli

            rdychk();
            r.al = r.bh;
            pc98.outPort(r.getDx(), r.al);
            _waitP();
            r.setDx((short) pw.fm2_port2);
            r.al = r.bl;
            pc98.outPort(r.getDx(), r.al);

            //    popf

            r.setDx(r.stack.pop());
            r.setBx(r.stack.pop());
            r.setAx(r.stack.pop());
        }
    }

    //
    // Fore/Back
    //
    private void opnset() {
        r.stack.push(r.getAx());
        r.stack.push(r.getBx());
        r.stack.push(r.getDx());

        r.setBx(r.getDx());
        r.setDx(pw.fm_port1);

        //    pushf
        //    cli

        rdychk();
        r.al = r.bh;
        pc98.outPort(r.getDx(), r.al);
        _waitP();
        r.setDx(pw.fm_port2);
        r.al = r.bl;
        pc98.outPort(r.getDx(), r.al);

        //    popf

        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    /**
     * READ PSG 07H Port
     * cli and then come
     */
    public void get07() {
        r.stack.push(r.getDx());
        r.setDx((short) pw.fm1_port1);
        rdychk();
        r.al = 7;
        pc98.outPort(r.getDx(), r.al);
        _waitP(); // ; PSG Read Wait
        r.setDx((short) pw.fm1_port2);
        r.al = pc98.inPort(r.getDx() & 0xffff);
        r.setDx(r.stack.pop());
    }

    /**
     * INT60H Main
     */
    private void int60_start() {
        // TimerA/B re-entry check

        if ((reint_chk[r.ah & 0xff] & 1) != 0) {
            if (pw.timerBFlag != 0) {
                int60_error();
                return;
            }
        }
        if ((reint_chk[r.ah & 0xff] & 2) != 0) {
            if (pw.timerAFlag != 0) {
                int60_error();
                return;
            }
        }
        if ((reint_chk[r.ah & 0xff] & 4) != 0) {
            if (pw.int60flag != 1) {
                int60_error();
                return;
            }
        }

        if (r.ah != 0xf) int60_jumptable[r.ah & 0xff].run();
        else {
            // KUMA: Note) It is necessary to avoid accessing the sound source from external threads.
            // KUMA: If it is absolutely necessary, please stop this thread first.
            if (pw.board2 != 0) int60_jumptable[r.ah & 0xff].run();
                else nothing();
        }

        r.al = pw.al_push;
        r.ah = pw.ah_push;
        r.setDx(pw.dx_push);
        int60_exit();
    }

    // Code for reentrancy check / bit0=TimerBint 1=TimerAint 2=INT60
    private static final byte[] reint_chk = {
            4, 4, 0, 6, 6, 0, 0, 0, 0, 0, 0, 0, 7, 7, 0, 5,
            0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 5, 5, 7, 0, 7, 0,
            0, 0
    };

    private Runnable[] int60_jumptable;

    private void set_int60_jumptable() {
        int60_jumptable = new Runnable[] {
                this::mstart_f, // 0
                this::mstop_f, // 1
                this::fout, // 2  in al:fadeout_speed
                null, // eff_on, // 3
                null, // effoff, // 4
                this::get_ss, // 5 out ax:Number of measures
                null, // get_musdat_adr, // 6
                null, // get_tondat_adr, // 7
                null, // get_fv, // 8
                this::drv_chk, // 9
                this::get_status, // A
                null, // get_efcdat_adr, // B
                null, // fm_effect_on, // C
                null, // fm_effect_off, // D
                this::get_pcm_adr, // E
                null, // pcm_effect, // F
                this::get_workadr, // 10
//                this::get_fmefc_num, // 11
//                this::get_pcmefc_num, // 12
//                this::set_fm_int, // 13
//                this::set_efc_int, // 14
//                this::get_psgefcnum, // 15
//                this::get_joy, // 16
//                this::get_ppsdrv_flag, // 17
//                this::set_ppsdrv_flag, // 18
//                this::set_fv, // 19
//                this::pause_on, // 1A
//                this::pause_off, // 1B
//                this::ff_music, // 1C
//                this::get_memo, // 1D
//                this::part_mask, // 1E
//                this::get_fm_int, // 1F
//                this::get_efc_int, // 20
//                this::get_mus_name, // 21
//                this::get_size // 22
        };
    }

    private static final int int60_max = 0x22;

    private void get_ss() {
        getss();
        pw.al_push = r.al;
        pw.ah_push = r.ah;
    }

    private void drv_chk() {
        if (pw.board2 != 0) {
            if (pw.ppz != 0) {
                if (pw.ademu != 0) {
                    pw.al_push = 5;
                } else {
                    pw.al_push = 4;
                }
            } else {
                if (pw.pcm != 0) {
                    pw.al_push = 2;
                } else {
                    pw.al_push = 1;
                }
            }
        } else {
            pw.al_push = 0;
        }
        r.ah = (byte) PW.vers;
        r.al = (byte) PW.verc;
        pw.ah_push = r.ah;
        pw.dx_push = r.getAx();
    }

    private void get_status() {
        getst();
        pw.al_push = r.al;
        pw.ah_push = r.ah;
    }

    private void get_pcm_adr() {
        r.setAx((short) 0); // r.cs;
        pw.ds_push = r.getAx();
        pw.dx_push = 0; // offset pcm_table
    }

    private void get_workadr() {
        r.setAx((short) 0); // r.cs;
        pw.ds_push = r.getAx();
        pw.dx_push = 0; // offset part_data_table
    }

    /**
     * Extracting memo strings
     */
    public short get_memo(int al) {
        try {
getmemo_errret: // ↑
            {
                r.al = (byte) al;
                r.setSi((short) pw.mmlbuf);
                if (pw.md[r.getSi() & 0xffff].dat != 0x1a)
                    break getmemo_errret; // File with no tone = Unable to obtain memo address
                r.addSi((short) 0x18);
                r.setSi((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
                r.addSi((short) pw.mmlbuf);
                r.subSi((short) 4);
                r.setBx((short) (pw.md[(r.getSi() + 2) & 0xffff].dat + pw.md[(r.getSi() + 3) & 0xffff].dat * 0x100)); // bh=0feh,bl=ver
                if (r.bl != 0x40) { // For Ver4.0 & 00H // break getmemo_exec;
                    if (r.bh != (byte) 0xfe)
                        break getmemo_errret; // 0feh for version 4.1 and later
                    if ((r.bl & 0xff) < 0x41)
                        break getmemo_errret; // If MC version is 4.1 or earlier, Error
                }
//getmemo_exec:
                if ((r.bl & 0xff) >= 0x42) { // Is it version 4.2 or later? // break getmemo_oldver41;
                    r.al++; // Then add +1 to al (0FFH #PPSFile)
                }
//getmemo_oldver41:
                if ((r.bl & 0xff) >= 0x48) { // Is it version 4.8 or later? // break getmemo_oldver47;
                    r.al++; // Then add +1 to al (0FEH for #PPZFile)
                }
//getmemo_oldver47:
                r.setSi((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
                r.addSi((short) pw.mmlbuf);
                r.al++;
//getmemo_loop:
                do {
                    r.setDx((short) (pw.md[(r.getSi() + 0) & 0xffff].dat + pw.md[(r.getSi() + 1) & 0xffff].dat * 0x100));
                    if (r.getDx() == 0)
                        break getmemo_errret;
                    r.addSi((short) 2);
                    r.al--;
                } while (r.al != 0); // break getmemo_loop;
//getmemo_exit:
                r.addDx((short) pw.mmlbuf);
                pw.ds_push = 0; // r.cs; no segments
                pw.dx_push = r.getDx();
                return r.getDx();
            }
//getmemo_errret:
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Detected that the memo address is out of range. Ignored.");
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        }
    }

    /**
     * The beginning of the song
     *  input DX<- Measure numbers
     * output AL<- return code   0:normal termination
     *      1: There is no song until that measure
     *      2: Song not playing
     */
    private void ff_music() {
        r.stack.push(r.getDx());
        r.setDx(pw.mask_adr);
        //pushf
        //cli
        r.al = pc98.inPort(r.getDx() & 0xffff);
        r.al |= pw.mask_data;
        pc98.outPort(r.getDx(), r.al); // Disable FM interrupt
        //popf
        r.setDx(r.stack.pop());
        ff_music_main();
        pw.al_push = r.al;
        r.setDx(pw.mask_adr);
        //pushf
        //cli
        r.al = pc98.inPort(r.getDx() & 0xffff);
        r.al &= pw.mask_data2;
        pc98.outPort(r.getDx(), r.al); // Enable FM interrupt
        //    popf
    }

    private void ff_music_main() {
        if (pw.status2 != (byte) 255) { // break ffm_exit2;
            pw.skip_flag = 1;
            if ((r.getDx() & 0xffff) < (pw.syousetu & 0xffff)) { // cmp dx,[syousetu] // break ffm_main;
                pw.skip_flag = 2;
                r.stack.push(r.getDx());
                if (pw.effon == 1) { // break ff_no_ssg_dr;
                    efcdrv.effend(); // ssgdrums cut
                }
//ff_no_ssg_dr:
                data_init2();
                play_init();
                opn_init();
                r.setDx(r.stack.pop());
                if (r.getDx() == 0) { // break ffm_main;
                    silence();
//                    break ffm_exit0b;
//ffm_exit0b:
                    r.dl = pw.ff_tempo; // <<
                    r.dl--; // Tempo 1 less than ff
                    stb_ff(); // <<
                    r.al = 0; // <<
//                    break ffm_exit;
                    pw.skip_flag = 0; // <<
                    return;
                }
            }
//ffm_main:
            r.stack.push(r.getDx());
            maskon_all();
            if (pw.board2 != 0) {
                r.setDx((short) 0x10ff);
                opnset44(); // Dump all rhythm sound sources
            }
            r.setDx(r.stack.pop());
            r.ah = pw.fadeout_volume;
            r.al = pw.rhythmmask;
            pw.fadeout_volume = (byte) 255;
            pw.rhythmmask = 0;
            r.stack.push(r.getAx());
            r.stack.push(r.bp);
            r.bp = r.getDx();
//ffm_loop:
            do {
                mmain();
                syousetu_count();
                if (pw.status2 == (byte) 255) {
//                    break ffm_exit1;
//ffm_exit1: // ↑
                    r.bp = r.stack.pop();
                    r.setAx(r.stack.pop());
                    maskoff_all();
                    r.al = 1;
//                    break ffm_exit;
                    pw.skip_flag = 0; // <<
                    return;
                }
            } while ((r.bp & 0xffff) >= (pw.syousetu & 0xffff)); // break ffm_loop;
            r.bp = r.stack.pop();
            r.setAx(r.stack.pop());
            pw.fadeout_volume = r.ah;
            pw.rhythmmask = r.al;
            if (pw.board2 != 0) {
                if (r.ah == 0) { // break ffm_exit0;
                    r.dl = pw.rhyvol;
                    volset2rf();
                }
            }
//ffm_exit0:
            maskoff_all();
//ffm_exit0b:
            r.dl = pw.ff_tempo;
            r.dl--; // Tempo 1 less than ff
            stb_ff();
            r.al = 0;
//            break ffm_exit;
            pw.skip_flag = 0; // <<
            return;
        }
//ffm_exit2:
        r.al = 2;

//ffm_exit:
        pw.skip_flag = 0;
    }

    /**
     * All parts temporary mask
     */
    private void maskon_all() {
        r.setSi((short) 0); // offset part_table
        r.setCx((short) pw.max_part1);
        r.di = (short) pw.part1;

//maskon_loop:
        do {
            if (pw.ppz != 0) {
                r.al = (byte) pw.part_table[r.incSi() & 0xffff];
                if (r.al == (byte) 0xff) { // -1 // break monl_main;
                    r.addSi((short) 6); // skip Rhythm & Effects(for PPZ parts)
                }
//monl_main:
            } else {
                r.incSi();
            }

            r.al = (byte) pw.part_table[r.incSi() & 0xffff]; //    lodsw; ah=sound source al = partb
            r.ah = (byte) pw.part_table[r.incSi() & 0xffff];

            pw.partWk[r.di & 0xffff].partmask |= (byte) 0x80;
            if (pw.partWk[r.di & 0xffff].partmask == (byte) 0x80) { // break maskon_next; // It was already masked elsewhere

                r.stack.push(r.getCx());
                r.stack.push(r.di);
                r.stack.push(r.getSi());
                maskon_main(); // 1 Part Mask
                r.setSi(r.stack.pop());
                r.di = r.stack.pop();
                r.setCx(r.stack.pop());
            }
//maskon_next:
            r.di += 1; // qq
            r.decCx();
        } while (r.getCx() != 0); // break maskon_loop;
    }

    /**
     * Temporarily remove all masks
     */
    private void maskoff_all() {
        r.setSi((short) 0); // offset part_table
        r.setCx((short) pw.max_part1);
        r.di = (short) pw.part1;
//maskoff_loop:
        do {
            if (pw.ppz != 0) {
                r.al = (byte) pw.part_table[r.incSi() & 0xffff];
                if (r.al == (byte) 0xff) { // -1 // break moffl_main;
                    r.addSi((short) 6); // skip Rhythm & Effects(for PPZ parts)
                }
//moffl_main:
            } else {
                r.incSi();
            }

            r.al = (byte) pw.part_table[r.incSi() & 0xffff]; //    lodsw; ah=sound source al = partb
            r.ah = (byte) pw.part_table[r.incSi() & 0xffff];

            pw.partWk[r.di & 0xffff].partmask &= 0x7f;
            if (pw.partWk[r.di & 0xffff].partmask == 0) { // break maskoff_next; // Still masked elsewhere

                r.stack.push(r.getCx());
                r.stack.push(r.di);
                r.stack.push(r.getSi());
                maskoff_main(); // 1 Part Return
                r.setSi(r.stack.pop());
                r.di = r.stack.pop();
                r.setCx(r.stack.pop());
            }
//maskoff_next:
            r.di += 1; // qq
            r.decCx();
        } while (r.getCx() != 0); // break maskoff_loop;
    }

    /**
     * Part Mask & Keyoff
     */
    private void part_mask() {
        r.ah = r.al;
        r.ah &= 0x7f;
        if (pw.ppz != 0) {
            r.carry = ((r.ah & 0xff) < 16 + 8);
        } else {
            r.carry = ((r.ah & 0xff) < 16);
        }
        if (!r.carry) return; // break pm_ret;
        if ((r.al & 0x80) != 0) {
            part_on();
            return;
        }
        r.bh = 0;
        r.bl = r.al;
        r.bl += r.bl;
        r.bl += r.al;
        r.setBx((short) 0); // offset part_table
        r.dl = (byte) pw.part_table[r.getBx() & 0xffff]; // dl<- Part Number
        if ((r.dl & 0x80) != 0) {
            rhythm_mask();
            return;
        }
        r.incBx();
        r.al = (byte) pw.part_table[(r.getBx() & 0xffff) + 0]; // AH=sound source AL = partb
        r.ah = (byte) pw.part_table[(r.getBx() & 0xffff) + 1];
        r.bh = 0;
        r.bl = r.dl;
        //r.bx += r.bx;
        r.setBx((short) 0); // offset part_data_table
        r.di = (short) pw.part_data_table[r.getBx() & 0xffff];
        r.dl = pw.partWk[r.di & 0xffff].partmask;
        pw.partWk[r.di & 0xffff].partmask |= 1;
        if (r.dl != 0)
            return; // break pm_ret; // ; It was already masked
        if (pw.play_flag == 0)
            return; // break pm_ret; // ; The song has stopped

        maskon_main();
    }

    private void maskon_main() {
pm_ppz: // ↑
        {
pm_pcm: // ↑
            {
pm_ssg: // ↑
                {
pm_drums: // ↑
                    {
pm_fm2: // ↑
                        {
                            if (r.ah != 0) { // break pm_fm1;
                                r.ah--;
                                if (pw.board2 != 0) {
                                    if (r.ah == 0) break pm_fm2;
                                }
                                r.ah--;
                                if (r.ah == 0) break pm_ssg;
                                r.ah--;
                                if (pw.board2 != 0) {
                                    if (r.ah == 0) break pm_pcm;
                                }
                                r.ah--;
                                if (r.ah == 0) break pm_drums;
                                if (pw.ppz != 0) {
                                    r.ah--;
                                    if (r.ah == 0) break pm_ppz;
                                }
//pm_ret:
                                return;
                            }
//pm_fm1:
                            //pushf
                            //cli
                            pw.partb = r.al;
                            if (pw.board2 != 0) {
                                sel44();
                            }
                            silence_fmpart(); // Eliminates sound completely
                            //popf
                            return;
                        }
//pm_fm2:
                        if (pw.board2 != 0) {
                            //pushf
                            //cli
                            pw.partb = r.al;
                            sel46();
                            silence_fmpart(); // Eliminates sound completely
                            //popf
                            return;
                        }
                    }
//pm_drums:
                    if ((pw.psgefcnum & 0xff) >= 11) {
//                        break pm_ssg_ret;
                        return;
                    }

                    efcdrv.effend();
                    return;
                }
//pm_ssg:
                //pushf
                //cli
                pw.partb = r.al;
                psgmsk();// ;AL=07h AH = Maskdata
                r.dh = 7;
                r.dl = r.al;
                r.dl |= r.ah;
                opnset44(); // ; PSG keyoff
                //popf
//pm_ssg_ret:
                return;
            }
//pm_pcm:
            if (pw.board2 != 0) {
                if (pw.adpcm != 0) {
                    if (pw.ademu != 0) {
                        if (pw.adpcm_emulate == 1) { // break pmpcm_noadpcm;
                            r.setAx((short) 0x0207);
                            ChipDatum cd = new ChipDatum(0x02, r.al & 0xff, 0);
                            ppz8em.apply(cd); // .StopPCM(r.al); // PPZ8 ch7 stop sound
                        }
//pmpcm_noadpcm:
                    } else {
                        //pushf
                        //cli
                        r.setDx((short) 0x0102); // PAN=0 / x8 bit mode
                        opnset46();
                        r.setDx((short) 0x0001); // PCM RESET
                        opnset46();
                        //popf
                    }
                }
                if (pw.pcm != 0) {
                    pcmdrv86.stop_86pcm();
                }
                return;
            }
        }
//pm_ppz:
        if (pw.ppz != 0) {
            if (pw.ademu != 0) {
                if (r.al == 7) { // break pmppz_exec;
                    if (pw.adpcm_emulate == 1) {
//                        break pmppz_noexec;
                        return;
                    }
                }
//pmppz_exec:
            }
            r.ah = 2;
            ChipDatum cd = new ChipDatum(0x02, r.al & 0xff, 0);
            ppz8em.apply(cd); // .StopPCM(r.al); // ; ppz stop(al= partb)
//pmppz_noexec:
        }
    }

    private void rhythm_mask() {
        pw.rhythmmask = 0; // Mask the rhythm sound source
        if (pw.board2 != 0) {
            r.setDx((short) 0x10ff);
            opnset44(); // Dump all rhythm sound sources
        }
    }

    /**
     * Unmask part and set FM sound source tone in.AH=part number
     */
    private void part_on() {
        r.bh = 0;
        r.bl = r.ah;
        r.bl += r.bl;
        r.bl += r.ah;
        r.addBx((short) 0); // offset part_table
        r.dl = (byte) pw.part_table[r.getBx() & 0xffff]; // dl<- Part number
        if ((r.dl & 0x80) != 0) {
            rhythm_on();
            return;
        }
        r.incBx();
        r.al = (byte) pw.part_table[(r.getBx() + 0) & 0xffff]; // AH=sound source AL = partb
        r.ah = (byte) pw.part_table[(r.getBx() + 1) & 0xffff];
        r.bh = 0;
        r.bl = r.dl;
        //r.bx += r.bx;
        r.addBx((short) 0); // offset part_data_table
        r.di = (short) pw.part_data_table[r.getBx() & 0xffff];
        if (pw.partWk[r.di & 0xffff].partmask == 0)
            return; // break po_ret; // ; Not masked
        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xfe;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            return; // break po_ret; // Still masked by sound effects
        if (pw.play_flag == 0)
            return; // break po_ret; // The song has stopped
        maskoff_main();
    }

    private void maskoff_main() {
        if (r.ah != 0) { // break po_fm1; // In the case of FM sound source
            if (pw.board2 != 0) {
                r.ah--;
                if (r.ah != 0) {
                    // break po_fm2; // Tone setting processing
                } else {
                    return; // <<
                }
            } else {
//po_ret:
                return;
            }
        } else {
//po_fm1:
            r.dl = pw.partWk[r.di & 0xffff].voicenum;
            //pushf
            //cli
            pw.partb = r.al;
            if (pw.board2 != 0) {
                sel44();
            }
            if (pw.partWk[r.di & 0xffff].address != 0) { // break pof1_not_set;
                neiro_reset();
            }
//pof1_not_set:
            //popf
            return;
        }
//po_fm2:
        if (pw.board2 != 0) {
            r.dl = pw.partWk[r.di & 0xffff].voicenum;
            //pushf
            //cli
            pw.partb = r.al;
            sel46();
            if (pw.partWk[r.di & 0xffff].address != 0) { // break pof2_not_set;
                neiro_reset();
            }
//pof2_not_set:
            //popf
        }
    }

    private void rhythm_on() {
        pw.rhythmmask = (byte) 0xff; // Unmask the Rhythm sound source
    }

    /**
     * When there is no board
     */
    private void int60_start_not_board() {
        n_int60_jumptable[r.ah & 0xff].run();

        r.al = pw.al_push;
        r.ah = pw.ah_push;
        r.setDx(pw.dx_push);
        int60_exit();
    }

    private Runnable[] n_int60_jumptable;

    private void set_n_int60_jumptable() {
        n_int60_jumptable = new Runnable[] {
//                 nothing // 0
//                ,nothing // 1
//                ,nothing // 2
//                ,nothing // 3
//                ,nothing // 4
//                ,get_255 // 5
//                ,get_musdat_adr // 6
//                ,get_tondat_adr // 7
//                ,get_255 // 8
//                ,drv_chk2 // 9
//                ,get_65535 // A
//                ,get_efcdat_adr // B
//                ,nothing  //C
//                ,nothing  //D
//                ,get_pcm_adr //E
//                ,nothing  //F
//                ,get_workadr //10
//                ,get_255 // 11
//                ,get_255 // 12
//                ,nothing // 13
//                ,nothing // 14
//                ,get_65535 // 15
//                ,get_65535 // 16
//                ,get_255 // 17
//                ,nothing // 18
//                ,nothing // 19
//                ,nothing // 1A
//                ,nothing  //1B
//                ,nothing  //1C
//                ,get_memo //1D
//                ,nothing // 1E
//                ,get_fm_int // 1F
//                ,get_efc_int // 20
//                ,get_mus_name // 21
//                ,get_size // 22
        };
    }

    private void get_255() {
        pw.al_push = (byte) 255;
    }

    private void nothing() {
    }

    private void get_65535() {
        pw.ah_push = (byte) 255;
        get_255();
    }

    //
    // FM sound effect routine
    //
    /**
     * pronunciation
     *  input AL to number_of_data
     */
    private void fm_effect_on() {
        if (pw.efcdat == -1) return; // KUMA: Sealed until future sound effects are used

        //pushf
        //cli
        if (pw.fm_effec_flag != 0) { // break not_e_flag;

            r.stack.push(r.getAx());
            fm_effect_off();
            r.setAx(r.stack.pop());
        }
//not_e_flag:
        pw.fm_effec_num = r.al;
        pw.fm_effec_flag = 1; // Please play the sound effects
        pw.partb = 3;
        if (pw.board2 == 0) {
            r.di = (short) pw.part3; // offset part3
            pw.partWk[r.di & 0xffff].partmask |= 2; // Part Mask
            r.di = (short) pw.part3b; // offset part3b
            pw.partWk[r.di & 0xffff].partmask |= 2; // Part Mask
            r.di = (short) pw.part3c; // offset part3c
            pw.partWk[r.di & 0xffff].partmask |= 2; // Part Mask
            r.di = (short) pw.part3d; // offset part3d
            pw.partWk[r.di & 0xffff].partmask |= 2; // Part Mask
        } else {
            r.di = (short) pw.part6; // offset part6
            pw.partWk[r.di & 0xffff].partmask |= 2; // Part Mask
        }
        r.bh = 0;
        r.bl = pw.fm_effec_num; // bx = effect no.
        r.di = (short) pw.part_e; // offset part_e
        r.al = 0;
        pw.partWk[r.di & 0xffff].clear(); // PartData Initialization
        r.addBx(r.getBx());
        r.addBx((short) pw.efcdat);
        r.setAx((short) (pw.md[r.getBx() & 0xffff].dat + pw.md[(r.getBx() + 1) & 0xffff].dat * 0x100));
        r.addAx((short) pw.efcdat);
        r.di = (short) pw.part_e;
        pw.partWk[r.di & 0xffff].address = r.getAx(); // Set of addresses
        pw.partWk[r.di & 0xffff].leng = 1; // One more count to start playing
        pw.partWk[r.di & 0xffff].volume = 108; // FM VOLUME DEFAULT= 108
        pw.partWk[r.di & 0xffff].slotmask = (byte) 0xf0; // FM SLOTMASK
        pw.partWk[r.di & 0xffff].neiromask = (byte) 0xff; // FM Neiro MASK
        if (pw.board2 != 0) {
            r.dl = (byte) 0xc0;
            pw.partWk[r.di & 0xffff].fmpan = r.dl; // FM PAN = Middle
            r.dh = (byte) 0xb6;
            sel46(); // Even if mmain comes here, it will remain sel46
            opnset();
        } else {
            r.al = pw.ch3mode;
            pw.ch3mode_push = r.al;
            pw.ch3mode = 0x3f;
        }

        //popf
        //ret
    }

    /**
     * Silence
     */
    private void fm_effect_off() {
        //pushf
        //cli
        if (pw.fm_effec_flag != 0) { // break feo_ret;

            pw.fm_effec_num = (byte) 0xff; // -1;
            pw.fm_effec_flag = 0; // Stop the sound effects
            if (pw.board2 != 0) {
                sel46(); // Even if mmain comes here, it will remain sel46
            }
            r.di = (short) pw.part_e;
            pw.partb = 3;
            silence_fmpart();
            if (pw.play_flag != 0) { // break feo_ret; // The song has stopped

                if (pw.board2 != 0) {
                    r.di = (short) pw.part6;
                    r.dl = pw.partWk[r.di & 0xffff].voicenum;
                    neiro_reset();
                } else {
                    r.di = (short) pw.part3;
                    r.dl = pw.partWk[r.di & 0xffff].voicenum;
                    neiro_reset();

                    r.di = (short) pw.part3b;
                    r.dl = pw.partWk[r.di & 0xffff].voicenum;
                    neiro_reset();

                    r.di = (short) pw.part3c;
                    r.dl = pw.partWk[r.di & 0xffff].voicenum;
                    neiro_reset();


                    r.di = (short) pw.part3d;
                    r.dl = pw.partWk[r.di & 0xffff].voicenum;
                    neiro_reset();

                    r.al = pw.ch3mode_push;
                    pw.ch3mode = r.al;
                    r.dh = 0x27;
                    r.dl = r.al;
                    r.dl &= (byte) 0b1100_1111; // Do not reset
                    opnset44();
                }
            }
        }
//feo_ret:
        // popf
    }

    /**
     * FM TimerA/B Processing Main
     *  *Make sure the timer has gone off before flying.
     *   The only pushed registers are ax/dx/ds.
     */
    private void fm_Timer_main() {
        //push cx
        //
        // Timer Reset
        // At the same time, FM interrupt Timer A or B is read to see which has come.
        //
        r.setDx((short) pw.fm1_port1);
        rdychk();
        r.al = 0x27;
        pc98.outPort(r.getDx(), r.al);
        _wait();
        r.ah = pw.ch3mode; // ah = value to output at 27h
        r.al = (byte) pw.timer.getStatReg(); // pc98.inPort(r.dx); // rdychk ;al = status
        byte a = r.ah;
        r.ah = r.al;
        r.al = a; // ah = status / value to be output to al=27h

        r.setDx((short) pw.fm1_port2);
        pc98.outPort(r.getDx(), r.al); // Timer Reset

        //r.ah = (byte)(pw.timer.StatReg & 3); // ah = TimerA/B flag

        //
        // Enable interrupts
        //
        if (pw.disint != 1) { // break not_sti;
            //sti
        }
//not_sti:

        //
        // Process by case distinction depending on which one came
        //

        r.ah--; // Timer A?
        if (r.ah == 0) { // break TimerA_int; // Process Timer A
//TimerA_int:
            timerA_main();
//exit_Timer:
            return;
        }

        r.ah--; // Timer B?
        if (r.ah == 0) { // break TimerB_int; // Process Timer B
//TimerB_int:
            timerB_main();
//exit_Timer:
            return;
        }

        timerB_main(); // same time
        timerA_main();

        //    cli
    }

    /**
     * TimerB processing [Main]
     */
    private void timerB_main() {
        if (pw.sync != 0) return;
        opnint_sub();
    }

    private void opnint_sub() {
        pw.timerBFlag = 1;
        if (pw.music_flag != 0) { // break not_mstop;
            if ((pw.music_flag & 1) != 0) { // break not_mstart;
                mstart();
            }
//not_mstart:
            if ((pw.music_flag & 2) != 0) { // break not_mstop;
                mstop();
            }
        }
//not_mstop:
        if (pw.play_flag != 0) { // break not_play;
            mmain();
            settempo_b();
            syousetu_count();
            r.al = pw.timerATime;
            pw.lastTimerAtime = r.al;
        }
//not_play:
        pw.timerBFlag = 0;
        if ((pw.intHook_flag & 1) != 0) { // break TimerB_nojump;
            //    call dword ptr[fmint_ofs]
        }
//TimerB_nojump:
    }

    /**
     * TimerA processing [Main]
     */
    private void timerA_main() {
        pw.timerAFlag = 1;
        pw.timerATime++;
        r.al = pw.timerATime;
        r.al &= 7;
        if (r.al == 0) { // break not_fade;
            fadeout(); // Fadeout processing
            rew(); // Rew processing
        }
//not_fade:
        if (pw.effon != 0) { // break not_psgeffec;
            if (pw.ppsdrv_flag != 0) { // break ta_not_ppsdrv;
                if ((pw.psgefcnum & 0x80) == 0) { // break not_psgeffec; // It is played by ppsdrv.
                } else {
                    efcdrv.effplay(); // SSG sound effect processing // <<
                }
            } else {
//ta_not_ppsdrv:
                efcdrv.effplay(); // SSG sound effect processing
            }
        }
//not_psgeffec:
        if (pw.fm_effec_flag != 0) { // break not_fmeffec;
            fm_efcplay(); // FM sound effect processing
        }
//not_fmeffec:
vtc000: // ↑
        {
            if (pw.key_check == 0)
                break vtc000;
            if (pw.play_flag == 0)
                break vtc000;
            if (pw.va != 0) {
                r.al = pc98.inPort(8);
                r.ah = pw.esc_sp_key;
                if ((r.ah & r.al) != 0)
                    break vtc000;
                r.al = pc98.inPort(9);
                if ((r.al & 0b1000_0000) != 0)
                    break vtc000;
            } else {
                //mov es, 0
                r.setBx((short) 0x52a);
                r.al = pw.esc_sp_key;
                r.al &= 0; // byte ptr es:0eh[bx]
                if (r.al != pw.esc_sp_key)
                    break vtc000;
                if ((0 & 0b0000_0001) == 0) //byte ptr es:[bx];esc
                    break vtc000;
                //mov es,cs
            }

            pw.music_flag |= 2; // MSTOP at the next TimerB
            pw.fadeout_flag = 0; // Stopped with CTRL+ESC = Treated as external
        }
//vtc000:
        pw.timerAFlag = 0;
        if ((pw.intHook_flag & 2) != 0) { // break TimerA_nojump;

            //TBD
            //pw.efcint_ofs(); //dword ptr[efcint_ofs]
        }
//TimerA_nojump:
    }

    /**
     * Measure counting
     */
    private void syousetu_count() {
        r.al = pw.opncount;
        r.al++;
        if (r.al == pw.syousetu_lng) { // break sc_ret;
            r.al = 0;
            pw.syousetu++;
        }
//sc_ret:
        pw.opncount = r.al;
    }

    /**
     * Tempo Settings
     */
    private void settempo_b() {
        r.ah = pw.grph_sp_key;
        check_grph();

        if (!r.carry) {
            //stb_n:
            r.dl = pw.tempo_d;
        } else {
            r.dl = pw.ff_tempo;
        }
        stb_ff();
    }

    private void stb_ff() {
        if (r.dl == pw.timerB_speed) return;

        pw.timerB_speed = r.dl;
        r.dh = 0x26;
        opnset44();
//stb_ret:
    }

    /**
     * Rewinding process
     */
    private void rew() {
        r.ah = pw.rew_sp_key;
        check_grph();
        if (r.carry) { // break rew_ret;
            r.setDx(pw.syousetu);
            r.al = pw.syousetu_lng;
            r.al = (byte) (/* signed */ r.al >> 1);
            r.al = (byte) (/* signed */ r.al >> 1);
            if ((pw.opncount & 0xff) >= r.al) {
                ff_music_main();
                return;
            }
            if (r.getDx() == 0) {
                ff_music_main();
                return;
            }
            r.decDx();
            {
                ff_music_main();
            }
        }
//rew_ret:
    }

    /**
     * GRPH key check
     *  in AH sp_key
     *  out CY Pressed with 1
     */
    private void check_grph() {
        if (pw.key_check == 0) // cy=0
            return;
        //cgr_main:
        r.carry = pc98.getGraphKey();
    }

    private void comstart() {
        //
        // PMD Command Start
        //

        print_mes(PW.mes_title); // Title Display

        //
        // PMD Resident CHECK
        //

        // Omitted

        //
        // Resident processing
        //

//resident_main:

        //
        // Option Initial Settings
        //
        r.setAx((short) 0);

        pw.mmldat_lng = (byte) PW.mdata_def; // Default 16K
        pw.voicedat_lng = (byte) PW.voice_def; // Default 8K
        pw.effecdat_lng = (byte) PW.effect_def; // Default 4K
        pw.key_check = (byte) PW.key_def; // Keycheck ON

        pw.fm_voldown = (byte) pw.fmvd_init; // FM_VOLDOWN
        pw._fm_voldown = (byte) pw.fmvd_init; // FM_VOLDOWN
        pw.ssg_voldown = r.al; // SSG_VOLDOWN
        pw._ssg_voldown = r.al; // SSG_VOLDOWN
        pw.pcm_voldown = r.al; // PCM_VOLDOWN
        pw._pcm_voldown = r.al; // PCM_VOLDOWN
        pw.ppz_voldown = r.al; // PPZ_VOLDOWN
        pw._ppz_voldown = r.al; // PPZ_VOLDOWN
        pw.rhythm_voldown = r.al; // RHYTHM_VOLDOWN
        pw._rhythm_voldown = r.al; // RHYTHM_VOLDOWN
        pw.kp_rhythm_flag = (byte) 0xff; // Play RHYTHM sound source with SSGDRUM FLAG

        r.di = 0; // offset rshot_bd
        pw.rshot[0] = 0; // _bd = 0;
        pw.rshot[1] = 0; // _sd = 0;
        pw.rshot[2] = 0; // _sym = 0;
        pw.rshot[3] = 0; // _hh = 0;
        pw.rshot[4] = 0; // _tom = 0;
        pw.rshot[5] = 0; // _rim = 0;

        r.di = (short) pw.part1; // offset part1
        r.setCx((short) pw.max_part1);
        do {
            pw.partWk[r.di & 0xffff].clear();
            r.di++;
            r.decCx();
        } while (r.getCx() != 0);

        pw.disint = r.al; // INT Disable FLAG
        pw.rescut_cant = r.al; // Resident release prohibition FLAG
        pw.adpcm_wait = r.al; // ADPCM defined speed
        pw.pcm86_vol = r.al; // PCM volume adjustment
        pw._pcm86_vol = r.al; // PCM volume adjustment
        pw.fade_stop_flag = 1; // MSTOP after FADEOUT FLAG
        pw.ppsdrv_flag = (byte) 0xff; // PPSDRV FLAG

        if (pw.va != 0) {
            pw.grph_sp_key = (byte) 0x80; // GRPH + CTRL key code
            pw.rew_sp_key = 0x40; // GPPH + SHIFTkey code
            pw.esc_sp_key = (byte) 0x80; // ESC + CTRL key code
        } else {
            pw.grph_sp_key = 0x10; // GRPH + CTRL key code
            pw.rew_sp_key = 0x1; // GPPH + SHIFTkey code
            pw.esc_sp_key = 0x10; // ESC + CTRL key code
            pw.port_sel = (byte) 0xff; // Port Selection = Auto
        }
        pw.ff_tempo = (byte) 250;
        pw.music_flag = r.al;
        pw.message_flag = 1;

        //
        // Check FM sound source (INT/PORT selection)
        //

        // TBD

        //
        // Import options
        //

        // TBD "PMDOPT=" search
        set_option(pw.pmdOption);

        //
        // Write "PMD" string to vmap area
        //

        // TBD

        //
        // Memory Check &Init
        //

        // TBD

        //
        // Set the storage address of the song data and tone color data
        //

        r.setAx((short) 1); // offset dataarea+1
        pw.mmlbuf = r.getAx() & 0xffff;
        r.decAx();

        r.bh = pw.mmldat_lng;
        r.bl = 0;
        r.slBx(2);
        r.addAx(r.getBx());
        pw.tondat = r.getAx() & 0xffff;
        r.bh = pw.voicedat_lng;
        r.bl = 0;
        r.slBx(2);
        r.addAx(r.getBx());
        pw.efcdat = r.getAx() & 0xffff;
        pw.efcdat = -1; // No sound effects used

        Random rnd = new Random();
        pw.seed = (short) rnd.nextInt(0, 0xffff);

        //
        // Initialize sound effects / FMINT / EFCINT
        //
        r.setAx((short) 0);
        pw.fmint_seg = r.getAx() & 0xffff;
        pw.fmint_ofs = r.getAx() & 0xffff;
        pw.efcint_seg = r.getAx() & 0xffff;
        pw.efcint_ofs = r.getAx() & 0xffff;
        pw.intHook_flag = r.al;
        pw.skip_flag = r.al;
        pw.effon = r.al;
        pw.fm_effec_flag = r.al;
        pw.pcmflag = r.al;

        r.al--;

        pw.psgefcnum = r.al;
        pw.fm_effec_num = r.al;
        pw.pcm_effec_num = r.al;

        //
        // Interrupt Settings
        //
        if (pw.board != 0) { // break not_set_opnvec;

            //
            // OPN Initialization
            //
            int_init();

            //
            // 088 / 188 / 288 / 388 (same INT number only) Initial setting
            //
            if (pw.va != 0) {
                r.setAx((short) 0x2900);
                opnset44();
                r.setAx((short) 0x2400);
                opnset44();
                r.setAx((short) 0x2500);
                opnset44();
                r.setAx((short) 0x2600);
                opnset44();
                r.setAx((short) 0x273f);
                opnset44();
            } else {
                r.setCx((short) 4);
                r.setDx((short) 0x88);

                r.setCx((short) 1); // KUMA: Only 0x188
                r.setDx((short) 0x188); // KUMA: Only 0x188

//opninit_loop:
                do {
                    r.stack.push(r.getCx());
                    r.ah = (byte) 0xff; // -1
                    r.setCx((short) 256);

//opninit_loop2:
opninit_next: // ↑
                    {
opninit_exec: // ↑
                        {
                            do {
                                r.al = pc98.inPort(r.getDx() & 0xffff);
                                r.ah &= r.al;
                                if ((r.ah & 0x80) == 0)
                                    break opninit_exec;
                                r.decCx();
                            } while (r.getCx() != 0);
                            break opninit_next; // No sound source
                        }
//opninit_exec:
                        //pushf
                        //cli

                        rdychk();
                        r.al = 0xe;
                        pc98.outPort(r.getDx(), r.al);
                        r.setCx((short) 256);
                        do {
                            r.decCx();
                        } while (r.getCx() != 0);
                        r.addDx((short) 2);
                        r.al = pc98.inPort(r.getDx() & 0xffff);

                        //popf

                        r.subDx((short) 2);
                        r.al &= (byte) 0xc0;
                        if (r.al == pw.opn_0eh) { // Compare int numbers // break opninit_next; // If they don't match, don't initialize

                            r.setAx((short) 0x2900);
                            opnset_fmc();
                            r.setAx((short) 0x2400);
                            opnset_fmc();
                            r.setAx((short) 0x2500);
                            opnset_fmc();
                            r.setAx((short) 0x2600);
                            opnset_fmc();
                            r.setAx((short) 0x273f);
                            opnset_fmc();
                        }
                    }
//opninit_next:
                    r.setCx(r.stack.pop());
                    r.dh++;

                    r.decCx();
                } while (r.getCx() != 0);
            }

            //
            // OPN interrupt vector save
            //
            //  cli
            r.setAx((short) 0);
            //r.es = r.ax;
            //r.bx = pw.vector;
            r.setBx((short) 0); // les bx, es:[bx]
            pw.int5ofs = r.getBx() & 0xffff;
            pw.int5seg = 0; // r.es;

            //
            // OPN interrupt vector setting
            //
            //r.es = r.ax;
            //r.bx = pw.vector;
            //es:[bx] = 0; // offset opnint
            //es:[bx+2] = r.cs;
        }
//not_set_opnvec:

        //
        // INT60 interrupt vector save
        //
        //cli
        r.setAx((short) 0);
        //r.es = r.ax;
        //r.bx = es:[pmdvector*4];
        pw.int60ofs = r.getBx() & 0xffff;
        pw.int60seg = 0; // r.es;

        //
        // INT60 interrupt vector setting
        //
        //r.es = r.ax;
        //es:[pmdvector*4] = 0; // offset int60_head
        //es:[pmdvector*4 + 2] = r.cs;

        //
        // OPN Interrupt Start
        //
        opnint_start();
        //sti
    }

    private void int_init() {
        // Not needed?

        pps_chk();
    }

    //
    // ppsdrv/ppz8 resident CHECK
    //
    private void pps_chk() {
ppschk_exit: // ↑
        {
            if (pw.ppsdrv_flag != (byte) 0xff) { // break pps_check;

                if (pw.kp_rhythm_flag != (byte) 0xff)
                    break ppschk_exit;

                r.al = pw.ppsdrv_flag;
                r.al ^= 1;
                pw.kp_rhythm_flag = r.al;
                break ppschk_exit;
            }
//pps_check:
            ppsdrv_check();
            if (!r.carry) { // break ppschk_01;

                pw.ppsdrv_flag = 1;
                if (pw.kp_rhythm_flag != (byte) 0xff)
                    break ppschk_exit;

                pw.kp_rhythm_flag = 0;
                break ppschk_exit;
            }
//ppschk_01:
            pw.ppsdrv_flag = 0;
            if (pw.kp_rhythm_flag != (byte) 0xff)
                break ppschk_exit;

            pw.kp_rhythm_flag = 1;
            break ppschk_exit;
        }
//ppschk_exit:
        if (pw.message_flag != 0) { // break ppschk_end;
            if (pw.ppsdrv_flag == 1) { // break ppschk_end;
                print_mes(PW.mes_ppsdrv);
            }
        }
//ppschk_end:

        if (pw.ppz != 0) {
            ppz8_check();
            if (!r.carry) { // break ppzchk_end;
                pw.ppz_call_seg = 1;
                r.setAx((short) 0x410);
                ChipDatum cd = new ChipDatum(0x04, r.al & 0xff, 0);
                ppz8em.apply(cd); // .ReadStatus(r.al); // int ppz_vec
                r.ah = (byte) pw.int_level;
                r.ah += 8;
                if (r.al == r.ah) { // break ppzchk_next;
                    //push es
                    r.setAx((short) 0x409);
                    cd = new ChipDatum(0x04, r.al & 0xff, 0);
                    ppz8em.apply(cd); // .ReadStatus(r.al); // int ppz_vec
                    r.setAx((short) 0); // r.es;
                    // pop es
                    pw.ppz_call_ofs = r.getBx() & 0xffff;
                    pw.ppz_call_seg = r.getAx() & 0xffff;
                }
//ppzchk_next:
                r.setAx((short) 0x1901);
                cd = new ChipDatum(0x19, 0, r.al & 0xff);
                ppz8em.apply(cd); // .SetReleaseFlag(r.al); // int ppz_vec; Do not release resident
                if (pw.message_flag == 0) {
                    mask_eoi_set();
                    return;
                }
                print_mes(PW.mes_ppz8);
            }
//ppzchk_end:
        }
    }

    //
    // MASK/EOI output destination settings
    //
    private void mask_eoi_set() {
        // Do nothing
    }

    /**
     * ppsdrv resident CHECK
     */
    public void ppsdrv_check() {
        r.carry = !pw.usePPSDRV; // PPSDRV is always resident!
    }

    /**
     * ppz8 resident CHECK
     */
    private void ppz8_check() {
        if (pw.ppz != 0) {
            r.carry = false; // PPZ8 is always resident! (TBD)
        }
    }

    /**
     * OPN interrupt enable processing
     */
    private void opnint_start() {
        if (pw.board != 0) { // break not_opnint_start; // No board

            //r.ax = r.cs;
            //r.es = r.ax;
            r.di = (short) pw.part1;
            r.setCx((short) pw.max_part1); // max_part1*type qq
            r.al = 0;
            do {
                pw.partWk[r.di & 0xffff].clear();
                r.di++;
                r.decCx();
            } while (r.getCx() != 0); // Partwork All Reset

            r.al--;
            pw.rhythmmask = (byte) 255; // Rhythm Mask Release
            pw.rhydmy = r.al;// R part For Dummy
            pw.rd = pw.rdDmy;
            data_init();
            opn_init();
            r.setDx((short) 0x07bf); // 07hPort Init
            opnset44();
            mstop();
            setint();
            r.al = (byte) pw.int_level;
            intset();
            if (pw.va != 0) {
                r.al = pc98.inPort(0x32);
                //jmp $+2
                r.al &= 0x7f;
                pc98.outPort((short) 0x32, r.al);
            }
            r.setDx((short) 0x2983);
            opnset44();
        }
//not_opnint_start:
    }

    /**
     * OPN out for 088/188/288/388 For INIT
     *  input ah  reg
     *   al data
     * dx port
     */
    private void opnset_fmc() {
        if (pw.va == 0) {
            //pushf
            //cli

            r.setCx((short) 256);
            do {
                r.decCx();
            } while (r.getCx() != 0);

            byte a = r.ah;
            r.ah = r.al;
            r.al = a;
            pc98.outPort(r.getDx(), r.al);

            r.setCx((short) 256);
            do {
                r.decCx();
            } while (r.getCx() != 0);

            r.addDx((short) 2);
            a = r.ah;
            r.ah = r.al;
            r.al = a;
            pc98.outPort(r.getDx(), r.al);

            r.subDx((short) 2);
            //popf
            //ret
        }
    }

    private void intset() {
        // Not needed?
    }

    /**
     * /D? option
     */
    private void fmvd_set(String op) {
        char c = op.charAt(0);
        int n = 0;
        try {
            n = Integer.parseInt(op.substring(1));
        } catch (NumberFormatException e) {
            logger.log(Level.ERROR, "Failed to parse /D option");
        }
        switch (c) {
            case 'S': // DS option
                pw.ssg_voldown = (byte) n;
                pw._ssg_voldown = (byte) n;
                break;
            case 'P':
                pw.pcm_voldown = (byte) n;
                pw._pcm_voldown = (byte) n;
                break;
            case 'R':
                pw.rhythm_voldown = (byte) n;
                pw._rhythm_voldown = (byte) n;
                break;
            case 'Z':
                pw.ppz_voldown = (byte) n;
                pw._ppz_voldown = (byte) n;
                break;
            case 'F': // DF option
                pw.fm_voldown = (byte) n;
                pw._fm_voldown = (byte) n;
                break;
            default:
                logger.log(Level.ERROR, "Failed to parse /D option");
                break;
        }
    }

    private void keycheck(String op) {
        char c = op.charAt(0);
        int n = 0;
        try {
            n = Integer.parseInt(op.substring(1));
        } catch (NumberFormatException e) {
            logger.log(Level.ERROR, "Failed to parse /K option");
        }
        switch (c) {
            case 'G':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= (byte) 0b1110_0000;
                }
                pw.grph_sp_key = r.al;
                break;
            case 'R':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= (byte) 0b1110_0000;
                }
                pw.rew_sp_key = r.al;
                break;
            case 'E':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= (byte) 0b1110_0000;
                }
                pw.esc_sp_key = r.al;
                break;
            default:
                logger.log(Level.ERROR, "Failed to parse /K option");
                break;
        }
    }

    /**
     * Option Processing
     *  input cs:bx option_data
     * ds:si command_line
     * es pmd_segment
     */
    private void set_option(String[] pmdOption) {
        if (pmdOption == null) return;
        for (String s : pmdOption) {
            String op = s.toUpperCase();
            if (op == null || op.isEmpty()) continue;
            if (op.isEmpty() || (op.charAt(0) != '/' && op.charAt(0) != '-')) continue;

            char c = op.charAt(1); // First character
            switch (c) {
                case 'D': // volume
                    if (op.length() > 2) fmvd_set(op.substring(2));
                    break;
                case 'N': // ssg drum
                    if (op.length() > 2 && op.charAt(2) == '-') pw.kp_rhythm_flag = 1;
                    else pw.kp_rhythm_flag = 0;
                    break;
                case 'P': // ppsdrv
                    if (op.length() > 2 && op.charAt(2) == '-') {
                        ppsdrv_check();
                        pw.ppsdrv_flag = 1;
                    } else pw.ppsdrv_flag = 0;
                    break;
                case 'C': // no message
                    pw.message_flag = 0;
                    break;
                case 'G':
                    int n = 0;
                    try {
                        n = Integer.parseInt(op.substring(1));
                    } catch (NumberFormatException e) {
                        pw.ff_tempo = (byte) 250;
                    }
                    pw.ff_tempo = (byte) n;
                    break;
                case 'K':
                    if (op.length() > 2) keycheck(op.substring(2));
                    break;
                case 'H':
                case '?': // help
                    throw new UnsupportedOperationException();
                case 'M':
                case 'V':
                case 'E':
                case 'F':
                case 'I':
                case 'W':
                case 'A':
                case 'S':
                case 'R':
                case 'Z':
                case 'O':
                    logger.log(Level.WARNING, "PMDDotNET does not support the specified option. Ignored. (%s)".formatted(op));
                    break;
                default:
                    logger.log(Level.ERROR, "Failed to parse options. Ignoring. (%s)".formatted(op));
                    break;
            }
        }
    }

    public void writeDummy(ChipDatum cd) {
        switch (pw.currentWriter) {
            case 0:
            case 1:
            case 2:
                WriteOPNARegister.accept(cd);
                break;
            case 3:
                ppz8em.apply(cd);
                break;
        }
    }

    public void execIDESpecialCommand(MmlDatum md) {
        //logger.log(Level.TRACE, "%d", md);

        List<Object> obj = md.args;
        MmlDatum mmd = (MmlDatum) obj.getFirst();

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = mmd;
        writeDummy(cd);
    }
}
