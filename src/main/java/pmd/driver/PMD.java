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
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;
import pmd.common.Common;
import pmd.common.PmdDosExitException;
import pmd.common.PmdDosExitException.PmdErrorExitException;
import pmd.common.PmdException;

import static java.lang.System.getLogger;


//==============================================================================
// Professional Music Driver[P.M.D.] version 4.8
//     FOR PC98(+ Speak Board)
// By M.Kajihara
//==============================================================================
public class PMD {

    private static final Logger logger = getLogger(PMD.class.getName());

    public PW pw = null;
    private X86Register r = null;
    private Pc98 pc98 = null;
    private PPZDRV ppzdrv = null;
    private PCMDRV pcmdrv = null;
    private PCMDRV86 pcmdrv86 = null;
    private EFCDRV efcdrv = null;
    private Function<ChipDatum, Integer> ppz8em = null;
    private Function<ChipDatum, Integer> ppsdrv = null;
    private Function<ChipDatum, Integer> p86em = null;
    public PCMLOAD pcmload = null;
    public Consumer<ChipDatum> WriteOPNARegister = null;


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

        pw.board = 1; // 音源あり
        //ポート番号の指定
        pw.fm1_port1 = 0x188; // レジスタ
        pw.fm1_port2 = 0x18a; // データ
        pw.fm2_port1 = 0x18c; // レジスタ(拡張)
        pw.fm2_port2 = 0x18e; // データ(拡張)

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

        Set_int60_jumptable();
        Set_n_int60_jumptable();
        SetupCmdtbl();
        SetupCmdtblp();
        SetupCmdtblr();
        SetupComtbl0c0h();

        comstart();
    }

    public void Rendering() {
        if (pw.getStatus() == 0) return;

        synchronized (pw.SystemInterrupt) {
            do {
                pw.timer.timer();
                pw.timeCounter++;
                if ((pw.timer.getStatReg() & 3) != 0) {
                    synchronized (r.lockobj) {
                        FM_Timer_main();
                    }
                }
            } while (pw.jumpIndex != -1 && pw.nowLoopCounter < 1);

            //Work.SystemInterrupt = false;
        }
    }


    //PMD.ASM 127-259
    //==============================================================================
    // ＭＳ−ＤＯＳコールのマクロ
    //==============================================================================

    private void resident_exit() {
        //特に何もしない
    }

    private void resident_cut() {
        //特に何もしない
    }

    private void get_psp() {
        //特に何もしない
    }

    public void msdos_exit() {
        //プログラム終了(エラーコード0)
        throw new PmdDosExitException("msdos_exit");
    }

    public void error_exit(int qq) {
        //プログラム終了(エラーコードqq)
        throw new PmdErrorExitException(String.format("error code:%d", qq));
    }

    public void print_mes(String qq) {
        //コンソールへメッセージ表示
        String[] a = qq.split("" + (char) 13 + (char) 10);
        for (String s : a)
            logger.log(Level.INFO, s);
    }

    public void print_chr(String qq) {
        //コンソールへ文字表示
        logger.log(Level.INFO, qq);
    }

    public void print_line(String bx) {
        //コンソールへメッセージ表示(bx位置から0まで)
        logger.log(Level.INFO, bx);
    }

//#if DEBUG
//        private PMDDotNET.Common.AutoExtendList<byte> debugBuff = new Common.AutoExtendList<byte>();
//        private PMDDotNET.Common.AutoExtendList<byte> debug2Buff = new Common.AutoExtendList<byte>();
//#endif

    public void debug(int adr) {
//#if DEBUG
//            debugBuff.Set(adr, (byte)(debugBuff.Get(adr) + 1));
//#endif
    }

    public void debug2(int adr, byte dat) {
//#if DEBUG
//            debug2Buff.Set(adr * 2, dat);
//#endif
    }

    public void debug_pcm(int adr) {
//#if DEBUG
//            r.al = pc98.InPort(0xa468); // 86音源FIFO
//            if ((r.al & 0x10) != 0)
//            {
//                debug(adr);
//            }
//#endif
    }

    public void _wait() {
        //r.cx = (short)pw.wait_clock;
        //do
        //{
        //    r.cx--;
        //} while (r.cx > 0);
    }

    public void _waitP() {
        //short p = r.cx;
        //r.cx = (short)pw.wait_clock;
        //do
        //{
        //    r.cx--;
        //}
        //while (r.cx > 0);
        //r.cx = p;
    }

    public void _rwait() // リズム連続出力用wait
    {
        //short p = r.cx;
        //r.cx = (short)(pw.wait_clock * 32);
        //do
        //{
        //    r.cx--;
        //}
        //while (r.cx > 0);
        //r.cx = p;
    }

    public void rdychk() // Address out時用 break:ax
    {
        r.al = pc98.InPort(r.getDx()); // 無駄読み
        do {
            r.al = pc98.InPort(r.getDx());
        } while ((r.al & 0x80) != 0);
    }

    public void _ppz() {
        //local exit
        if (pw.ppz != 0) {
            if (pw.ppz_call_seg >= 2) {
                //  call dword ptr[ppz_call_ofs]
                throw new UnsupportedOperationException();
            }
        }
    }


    public void int60_main(short ax) {
        synchronized (r.lockobj) {
            r.setAx(ax);

            pw.int60flag++;
            if (r.ah >= int60_max + 1) {
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


    //377-380
    private void getss() {
        r.setAx(pw.syousetu);
    }


    //381-385
    private void getst() {
        r.ah = pw.status;
        r.al = pw.status2; // KUMA: 0xff : 演奏終了?
    }


    //386-388
    private void fout() {
        pw.fadeout_speed = r.al;
    }

    public void resetOption(String[] pmdOption) {
        set_option(pmdOption);
    }


    //389-429
    //==============================================================================
    // FM効果音演奏メイン
    //==============================================================================
    private void fm_efcplay() {
        r.setBx((short) pw.efcdat);
        r.setAx((short) (pw.md[r.getBx() + 254].dat + pw.md[r.getBx() + 254 + 1].dat * 0x100));
        r.addAx(r.getBx());
        pw.prgdat_adr2 = r.getAx();
        r.di = (short) pw.part_e; // offset part_e

        if (pw.board2 != 0) {
            r.stack.push(pw.fm_port1); // mmainでsel44状態でTimerA割り込みが来た時用の
            r.stack.push(pw.fm_port2); // 対策
            r.ah = pw.partb;
            r.al = pw.fmsel;
            r.stack.push(r.getAx());
            pw.partb = 3;
            sel46(); // ; ここでmmainが来てもsel46のまま
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

        if (pw.md[r.getSi()].dat == 0x80) { // break not_end_fmefc;
            if (pw.partWk[r.di].leng == 0) { // break not_end_fmefc;

                fm_effect_off();
            }
        }
//not_end_fmefc:
    }


    //430-501
    //==============================================================================
    // 演奏開始
    //==============================================================================
    private void mstart_f() {
        r.al = pw.TimerAflag;
        r.al |= pw.TimerBflag;
        if (r.al == 0) {
            mstart();
            return;
        }

        pw.music_flag |= 1; // TA/TB処理中は 実行しない
        pw.ah_push = (byte) 0xff; // -1;
    }

    private void mstart() {
        //------------------------------------------------------------------------------
        // 演奏停止
        //------------------------------------------------------------------------------
        // pushf
        //  cli
        pw.music_flag &= 0xfe;
        mstop();
        //  popf

        //------------------------------------------------------------------------------
        // 演奏準備
        //------------------------------------------------------------------------------
        data_init();
        play_init();

        pw.fadeout_volume = 0;

        if ((pw.board2 | pw.adpcm) != 0) {
            if (pw.ademu != 0) {
                r.setAx((short) 0x1800);
                pw.adpcm_emulate = r.al;
                ChipDatum cd = new ChipDatum(0x18, r.al, 0);
                ppz8em.apply(cd); // .SetAdpcmEmu(r.al); // ADPCMEmulate OFF
                r.setBx((short) pw.part10); // offset part10 //PCMを
                pw.partWk[r.getBx()].partmask |= 0x10; // Mask(bit4)
            } else {
                //------------------------------------------------------------------------------
                // NECなYM2608なら PCMパートをMASK
                //------------------------------------------------------------------------------
                if (pw.pcm_gs_flag != 0) {
                    r.setBx((short) pw.part10); // offset part10 ;PCMを
                    pw.partWk[r.getBx()].partmask |= 4; // Mask(bit2)
                }
                //not_mask_pcm:
            }
        }

        if (pw.ppz != 0) {
            //------------------------------------------------------------------------------
            // PPZ8初期化
            //------------------------------------------------------------------------------
            if (pw.ppz_call_seg != 0) {
                r.setAx((short) 0x1901);
                ChipDatum cd = new ChipDatum(0x19, 0, 0x01);
                ppz8em.apply(cd); // .SetReleaseFlag(0x01); // 常駐解除禁止
                r.ah = 0;
                cd = new ChipDatum(0x00, 0, 0);
                ppz8em.apply(cd); // .Initialize();
                //r.ah = 6;
                //ppz8em.Reserve();
            }
            //not_init_ppz8:
        }

        //------------------------------------------------------------------------------
        // OPN初期化
        //------------------------------------------------------------------------------
        opn_init();

        //------------------------------------------------------------------------------
        // 音楽の演奏を開始
        //------------------------------------------------------------------------------
        setint();

        pw.play_flag = 1;
        pw.mstart_flag++;
    }

    //==============================================================================
    // 各パートのスタートアドレス及び初期値をセット
    //==============================================================================
    private void play_init() {
        r.setSi((short) pw.mmlbuf);

        r.al = (byte) pw.md[r.getSi() - 1].dat;
        pw.x68_flg = r.al;

        // 2.6 追加分
        pw.prg_flg = 0;
        if (pw.md[r.getSi()].dat != (pw.max_part2 + 1) * 2) {
            r.setBx(Common.GetLe16(pw.md, r.getSi() + (2 * (pw.max_part2 + 1))));
            r.addBx(r.getSi());
            pw.prgdat_adr = r.getBx();
            pw.prg_flg = 1;
        }

        //not_prg:
        //prg:

        r.setCx((short) pw.max_part2);
        r.dl = 0;
        r.setBx((short) 0); // offset part_data_table
        pw.part_data_table = new int[22];
        for (int i = 0; i < pw.part_data_table.length; i++) pw.part_data_table[i] = i; // KUMA:順に並んだ配列なので。

        // din0:
        do {
            r.di = (short) pw.part_data_table[r.getBx()]; // di = part workarea // KUMA: 各partのIndexです
            r.incBx();
            r.setAx(Common.GetLe16(pw.md, r.getSi())); // ax = part start addr
            r.addSi((short) 2);

            r.addAx((short) pw.mmlbuf);
            if (pw.md[r.getAx()].dat == 0x80) { // 先頭が80hなら演奏しない
                r.setAx((short) 0);
            }
            //din1:

            pw.partWk[r.di].address = r.getAx();

            pw.partWk[r.di].leng = 1; // あと１カウントで演奏開始
            r.al = (byte) 0xff; // -1
            pw.partWk[r.di].keyoff_flag = r.al; // 現在keyoff中
            pw.partWk[r.di].mdc = r.al; // MDepth Counter(無限)
            pw.partWk[r.di].mdc2 = r.al;//
            pw.partWk[r.di]._mdc = r.al;//
            pw.partWk[r.di]._mdc2 = r.al;//
            pw.partWk[r.di].onkai = r.al; // rest
            pw.partWk[r.di].onkai_def = r.al; // rest
            if (r.dl < 6) { // break din_not_fm;

                // Part 0,1,2,3,4,5(FM1～6)の時
                pw.partWk[r.di].volume = 108; // FM VOLUME DEFAULT= 108
                pw.partWk[r.di].fmpan = (byte) 0xc0; // FM PAN = Middle
                if (pw.board2 != 0) {
                    pw.partWk[r.di].slotmask = (byte) 0xf0; // FM SLOT MASK
                    pw.partWk[r.di].neiromask = (byte) 0xff; // FM Neiro MASK
                } else {
                    if (r.dl < 3) { // break din_fm_mask; // OPN 3,4,5 はneiro/slotmaskは0のまま
                        pw.partWk[r.di].slotmask = (byte) 0xf0; // FM SLOT MASK
                        pw.partWk[r.di].neiromask = (byte) 0xff; // FM Neiro MASK
//                        break init_exit;
                    } else {
//din_fm_mask:
                        pw.partWk[r.di].partmask |= 0x20; // s0の時FMマスク
                    }
                }

//                break init_exit;
            } else {
//din_not_fm:
                if (r.dl < 9) { // break din_not_psg;
                    // Part 6,7,8(PSG1～3)の時
                    pw.partWk[r.di].volume = 8; // PSG VOLUME DEFAULT= 8
                    pw.partWk[r.di].psgpat = 7; // PSG = TONE
                    pw.partWk[r.di].envf = 3; // PSG ENV = NONE / normal
//                    break init_exit;
                } else {
//din_not_psg:
                    if (r.dl == 9) { // break din_not_pcm;
                        if (pw.board2 != 0) {
                            if (pw.adpcm != 0) {
                                // Part 9(OPNA/ADPCM)の時
                                pw.partWk[r.di].volume = (byte) 128; // PCM VOLUME DEFAULT= 128
                                pw.partWk[r.di].fmpan = (byte) 0xc0; // PCM PAN = Middle
                            }
                            if (pw.pcm != 0) {
                                // Part 9(OPNA/PCM)の時
                                pw.partWk[r.di].volume = (byte) 128; // PCM VOLUME DEFAULT= 128
                                pw.partWk[r.di].fmpan = 0x00;
                                pw.pcm86_pan_flag = 0; // Mid
                                pw.revpan = 0; // 逆相off
                            }
                        }
//                        break init_exit;
                    } else {
//din_not_pcm:
                        if (r.dl == 10) { // break not_rhythm;
                            // Part 10(Rhythm) の時
                            pw.partWk[r.di].volume = 15; // PPSDRV volume
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

        } while (r.getCx() > 0);

        //------------------------------------------------------------------------------
        // Rhythm のアドレステーブルをセット
        //------------------------------------------------------------------------------
        r.setAx(Common.GetLe16(pw.md, r.getSi())); // ax = part start addr
        r.addSi((short) 2);
        r.addAx((short) pw.mmlbuf);
        pw.radtbl = r.getAx();

        pw.rhyadr = 0; // offset rhydmy
        pw.rd = pw.rdDmy; // rhyadrはrdDmy(ダミー向け演奏データ配列)を参照する
    }

    //==============================================================================
    // DATA AREA の イニシャライズ
    //==============================================================================
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

        // di_loop:
        do {
            r.stack.push(r.getCx());
            r.setBx(r.di++); // KUMA:多分、インクリメントしないとだめ。。。
            r.dh = pw.partWk[r.getBx()].partmask;
            r.dl = pw.partWk[r.getBx()].keyon_flag;
            r.setCx((short) 0); // type qq // KUMA:partworkの大きさが入る?

            //pushf
            //cli

            r.al = 0;
            pw.partWk[r.getBx()].Clear();

            r.dh &= 0xf; // 0dh;一時,s,m,ADE以外の
            pw.partWk[r.getBx()].partmask = r.dh; // partmaskのみ保存
            pw.partWk[r.getBx()].keyon_flag = r.dl; // keyon_flag保存
            pw.partWk[r.getBx()].onkai = (byte) 0xff; // -1; // onkaiを休符設定
            pw.partWk[r.getBx()].onkai_def = (byte) 0xff; // -1; // onkaiを休符設定

            //    popf

            r.setCx(r.stack.pop());
            r.decCx();
        } while (r.getCx() > 0);

        r.setAx((short) 0);
        pw.tieflag = r.al;
        pw.status = r.al;
        pw.status2 = r.al;
        pw.syousetu = r.getAx();
        pw.opncount = r.al;
        pw.TimerAtime = r.al;
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
        pw.pcmstart = r.getAx();
        pw.pcmstop = r.getAx();
        pw.pcmrepeat1 = r.getAx();
        pw.pcmrepeat2 = r.getAx();
        pw.pcmrelease = (short) 0x8000;
        pw.kshot_dat = r.getAx();
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

    //==============================================================================
    // OPN INIT
    //==============================================================================
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

        //==============================================================================
        // SSG - EG RESET(4.8s)
        //==============================================================================
        if (pw.board2 != 0) {
            r.setBx((short) 2);
            sel44();
        }

//sr01:
        while (true) {

            r.setCx((short) 15); // 効果音とか気にしない、でいいや…仕様で。
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
            } while (r.getCx() > 0);

            if (pw.board2 != 0) {
                sel46();
                r.decBx();
                if (r.getBx() != 0) continue; // break sr01;
                sel44();
            }
            break;
        }

        //==============================================================================
        // YM2203ならここでおしまい
        //==============================================================================
        if (pw.board2 == 0) {
            if (pw.ongen == 0) // 2203 ?
            {
                return;
            }
        }

        //==============================================================================
        // 以下YM2608用
        // PAN / HARDLFO DEFAULT
        //==============================================================================
        //init_2608:
        //endif

        if (pw.board2 != 0) {
            r.setBx((short) 2);
            //; call sel44; mmainには飛ばない状況下なので大丈夫
        }
//pd01:
        while (true) {

            r.setDx((short) 0xb4c0); // PAN = MID / HARDLFO = OFF
            r.setCx((short) 3);

            //pd00:
            do {
pd03:
                // ↑
                {
                    if (pw.fm_effec_flag != 0) {
                        // ここbugってたわ…(4.8s) //KUMA:ややこしかったので整理。。。
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
                sel46(); // mmainには飛ばない状況下なので大丈夫
                r.decBx();
                if (r.getBx() != 0) continue; // break pd01;
                sel44(); // mmainには飛ばない状況下なので大丈夫
            }
            break;
        }

        r.setDx((short) 0x2200); // HARDLFO = OFF
        pw.port22h = r.dl;
        opnset44();

        if (pw.board2 != 0) {
            //==============================================================================
            // Rhythm Default = Pan : Mid , Vol: 15
            //==============================================================================
            r.di = 0; // offset rdat
            r.setCx((short) 6);
            r.al = (byte) 0b1100_1111;
            do {
                pw.rdat[r.di++] = r.al;
                r.decCx();
            } while (r.getCx() != 0);

            r.setDx((short) 0x10ff);
            opnset44(); // ; Rhythm All Dump

            //==============================================================================
            // リズムトータルレベル セット
            //==============================================================================
            //rtlset:
            r.dl = 48;
            r.al = pw.rhythm_voldown;
            if (r.al != 0) { // break rtlset2r;

                r.dl <<= 2; // 0 - 63 > 0 - 255
                r.al = (byte) -r.al;
                r.setAx((short) (r.al * r.dl));
                r.dl = r.ah;
                r.dl >>= 2; // 0 - 255 > 0 - 63
            }
//rtlset2r:
            pw.rhyvol = r.dl;
            r.dh = 0x11;
            opnset44();

            //==============================================================================
            // PCM reset &ＬＩＭＩＴ ＳＥＴ
            //==============================================================================
            if (pw.ademu == 0) {
                if (pw.pcm_gs_flag != 1) {
                    r.setDx((short) 0xcff);
                    opnset46();
                    r.setDx((short) 0xdff);
                    opnset46();
                }
                //pr_non_pcm:;
            }

            //==============================================================================
            // PPZ Pan Init.
            //==============================================================================
            if (pw.ppz + pw.ademu != 0) {
                r.setDx((short) 5);
                r.setAx((short) 0x1300);
                r.setCx((short) 8);
                //ppz_pan_init_loop:
                do {
                    r.al = r.cl;
                    r.al--;
                    ChipDatum cd = new ChipDatum(0x13, r.al, r.getDx());
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
            pc98.OutPort(r.getDx(), r.al);
            r.setDx((short) pw.fm2_port2);
            _wait();
            r.al = (byte) 0x80;
            pc98.OutPort(r.getDx(), r.al);
            _wait();
            r.al = 0x18;
            pc98.OutPort(r.getDx(), r.al);
            //    popf
        }
    }


    //871-896
    //==============================================================================
    // ＭＵＳＩＣ ＳＴＯＰ
    //==============================================================================
    private void mstop_f() {
        r.al = pw.TimerAflag;
        r.al |= pw.TimerBflag;
        if (r.al != 0) {
            pw.music_flag |= 2; // TA/TB処理中は 実行しない
            pw.ah_push = (byte) 0xff; // -1
            return;
        }
        //_mstop:
        pw.fadeout_flag = 0; // 外部からmstopさせた場合は0にする

        //mstop(); // KUMA:外部からの場合も同じ処理をさせる(別スレッドから音源を操作させない)
        pw.music_flag |= 2;
        pw.ah_push = (byte) 0xff;
    }

    private void mstop() {
        //    pushf
        //    cli
        pw.music_flag &= 0xfd;
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


    //897-1024
    //==============================================================================
    // MUSIC PLAYER MAIN[FROM TIMER - B]
    //==============================================================================
    private void mmain() {
        int w = 0; // kuma: added

        pw.loop_work = 3;
        pw.nowLoopCounter = Integer.MAX_VALUE;

        if (pw.x68_flg == 0) { // break mmain_fm;

            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;

            r.di = (short) pw.part7; // offset part7
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 1;
            psgmain(); // SSG1
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part8; // offset part8
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 2;
            psgmain(); // SSG2
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part9; // offset part9
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 3;
            psgmain(); // SSG3
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        }
//mmain_fm:
        if (pw.board2 != 0) {
            sel46();

            r.di = (short) pw.part4; // offset part4
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 1;
            fmmain(); // FM4 OPNA
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part5; // offset part5
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 2;
            fmmain(); // FM5 OPNA
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

            r.di = (short) pw.part6; // offset part6
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            pw.partb = 3;
            fmmain(); // FM6 OPNA
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

            sel44();
        }

        r.di = (short) pw.part1; // offset part1
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        pw.partb = 1;
        fmmain(); // FM1
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part2; // offset part2
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        pw.partb = 2;
        fmmain(); // FM2
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3; // offset part3
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        pw.partb = 3;
        fmmain(); // FM3
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3b; // offset part3b
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 拡張１
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3c; // offset part3c
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 拡張２
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        r.di = (short) pw.part3d; // offset part3d
        w = 0;
        if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
        fmmain(); // FM3 拡張３
        if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
            pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

        if (pw.x68_flg == 0) { // break mmain_exit;


            r.di = (short) pw.part11; // offset part11
            w = 0;
            if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
            rhythmmain(); // RHYTHM
            if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added


            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;

            if (pw.board2 != 0) {
                r.di = (short) pw.part10; // offset part10
                w = 0;
                if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                if (pw.useP86DRV) {
                    pcmdrv86.pcmmain(); // ADPCM/PCM(IN "pcmdrv.asm"/"pcmdrv86.asm")
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added
                } else {
                    pcmdrv.pcmmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added
                }

                if (pw.ppz != 0) {
                    r.di = (short) pw.part10a; // offset part10a
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 0;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10b; // offset part10b
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 1;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10c; // offset part10c
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 2;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10d; // offset part10d
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 3;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10e; // offset part10e
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 4;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10f; // offset part10f
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 5;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10g; // offset part10g
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 6;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added

                    r.di = (short) pw.part10h; // offset part10h
                    w = 0;
                    if (pw.partWk[pw.part_data_table[r.di]].address == 0) w = 1; // kuma: added
                    pw.partb = 7;
                    ppzdrv.ppzmain();
                    if (w == 0 && !(pw.partWk[r.di].loopcheck == 3 && pw.partWk[r.di].partloop == 0))
                        pw.nowLoopCounter = Math.min(pw.partWk[r.di].loopCounter, pw.nowLoopCounter); // kuma: added
                }

            }

        }
//mmain_exit:

        //Console.WriteLine("loop counter:%d", pw.nowLoopCounter);

        if (pw.loop_work == 0) { // break mmain_loop;
            return;
        }
//mmain_loop:

        r.setCx((short) pw.max_part1);
        r.setBx((short) 0); // offset part_data_table

        //mm_din0:;
        do {
            r.di = (short) pw.part_data_table[r.getBx()]; // [bx]; di = part workarea
            r.incBx();

            if (pw.partWk[r.di].loopcheck != 3) { // break mm_notset;
                pw.partWk[r.di].loopcheck = 0;
            }
//mm_notset:
            r.decCx();
        } while (r.getCx() != 0);

        if (pw.loop_work != 3) { // break mml_fin;

            pw.status2++;
            if (pw.status2 == 0xff) { // -1にはさせない // break mml_ret;

                pw.status2 = 1;
            }
//mml_ret:
            return;
        }
//mml_fin:
        pw.status2 = (byte) 0xff; // -1;
    }

    //==============================================================================
    // 裏FMセレクト
    //==============================================================================
    private void sel46() {
        r.setAx((short) pw.fm2_port1);
        pw.fm_port1 = r.getAx();
        r.setAx((short) pw.fm2_port2);
        pw.fm_port2 = r.getAx();
        pw.fmsel = 1;
    }

    //==============================================================================
    // 表に戻す
    //==============================================================================
    private void sel44() {
        r.setAx((short) pw.fm1_port1);
        pw.fm_port1 = r.getAx();
        r.setAx((short) pw.fm1_port2);
        pw.fm_port2 = r.getAx();
        pw.fmsel = 0;
    }


    //1047-1210
    //==============================================================================
    // FM音源演奏メイン
    //==============================================================================
    //private void fmmain_ret() {
    // ret
    //}

    private void fmmain() {
        r.setSi(pw.partWk[pw.part_data_table[r.di]].address); // si = PART DATA ADDRESS
        if (r.getSi()  == 0) return;

        Supplier<Object> ret = null;
        if (pw.partWk[r.di].partmask != 0)
            ret = this::fmmain_nonplay;
        else
            ret = this::fmmain_c_1;

        if (ret != null) {
            do {
                ret = (Supplier<Object>) ret.get();
            } while (ret != null);
        }
    }

    private Supplier<Object> fmmain_c_1() {
        // 音長 -1
        pw.partWk[r.di].leng--;
        r.al = pw.partWk[r.di].leng;

        // KEYOFF CHECK & Keyoff
        if ((pw.partWk[r.di].keyoff_flag & 3) == 0) { // 既にkeyoffしたか？ // break mp0;

            if (r.al <= pw.partWk[r.di].qdat) { // Q値 => 残りLength値時 keyoff // break mp0;

                keyoff(); // ALは壊さない
                pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
            }
        }
//mp0:
        // LENGTH CHECK
        if (r.al != 0) return this::mpexit;
        return this::mp10;
    }

    private Supplier<Object> mp10() {
        pw.partWk[r.di].lfoswi &= 0xf7; // Porta off
        return this::mp1;
    }

    private Supplier<Object> mp1() { // DATA READ
mp2: // ↑
        {
mp15: // ↑
            {
                do {
                    pw.cmd = pw.md[r.getSi()];
                    //if (r.si == pw.jumpIndex)
                    //pw.jumpIndex = -1; // KUMA:Added

                    r.al = (byte) pw.md[r.incSi()].dat;
                    if (r.al >= 0x80)
                        break mp2;
                    if (r.al == 0x80) break mp15;

                    // ELSE COMMANDS
                    Object o = commands();
                    Supplier<Object> mp1_ = this::mp1;
                    Supplier<Object> mnp_ret_ = this::mnp_ret;
                    Supplier<Object> porta_return_ = this::porta_return;
                    while (o != null && o != mp1_) {
                        o = ((Supplier<Object>) o).get();
                        if (o == mnp_ret_)
                            return this::mnp_ret;
                        if (o == porta_return_)
                            return this::porta_return;
                    }
                } while (true);
            }
            // END OF MUSIC["L"があった時はそこに戻る]
//mp15:
            FlashMacroList();

            r.decSi();
            pw.partWk[r.di].address = r.getSi(); // mov[di],si
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di].partloop);
            if (r.getBx() == 0) return this::mpexit;

            // "L"があった時
            r.setSi(r.getBx());
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
            return this::mp1;
        }

//mp2:
        // F-NUMBER SET

        FlashMacroList();

        lfoinit();
        oshift();
        fnumset();

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = pw.cmd;
        WriteOPNARegister.accept(cd);

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].leng = r.al;
        calc_q();
        return porta_return();
    }

    public void FlashMacroList() {
        if (pw.cmd.args != null && pw.cmd.args.size() > 2) {
            Object obj = pw.cmd.args.get(2);
            if (obj != null && obj instanceof MmlDatum[])
            {
                MmlDatum[] mds = (MmlDatum[]) obj;
                for (MmlDatum md : mds) ExecIDESpecialCommand(md);
            }
        }
    }

    private Supplier<Object> porta_return() {
        if (pw.partWk[r.di].volpush != 0) { // break mp_new;
            if (pw.partWk[r.di].onkai != 0xff) { // break mp_new;
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) { // break mp_new;
                    pw.volpush_flag = 0;
                    pw.partWk[r.di].volpush = 0;
                }
            }
        }
//mp_new:
        volset();
        otodasi();
        keyon();
        pw.partWk[r.di].keyon_flag++;
        pw.partWk[r.di].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di].keyoff_flag = r.al;
        if (pw.md[r.getSi()].dat != 0xfb) // '&'が直後にあったらkeyoffしない
            return this::mnp_ret;
        pw.partWk[r.di].keyoff_flag = 2;
        return this::mnp_ret;
    }

    private Supplier<Object> mpexit() { // LFO & Portament & Fadeout 処理 をして終了
        if (pw.board2 != 0) {
            if (pw.partWk[r.di].hldelay_c != 0) {
                pw.partWk[r.di].hldelay_c--;
                if (pw.partWk[r.di].hldelay_c == 0) {
                    r.dh = pw.partb;
                    r.dh += 0xb4 - 1;
                    r.dl = pw.partWk[r.di].fmpan;
                    opnset();
                }
            }
            //not_hldelay:;
        }
        if (pw.partWk[r.di].sdelay_c != 0) {
            pw.partWk[r.di].sdelay_c--;
            if (pw.partWk[r.di].sdelay_c == 0) {
                if ((pw.partWk[r.di].keyoff_flag & 1) == 0) { // 既にkeyoffしたか？
                    keyon();
                }
            }
        }
        //not_sdelay:;
        r.cl = pw.partWk[r.di].lfoswi;
        if ((r.cl & r.cl) == 0) {
            //break nolfosw; // 飛ばずに処理
            if (pw.fadeout_speed != 0) {
                volset();
            }
            return this::mnp_ret;
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
                r.al = pw.partWk[r.di].lfoswi;
                r.al &= 0x30;
                pw.lfo_switch |= r.al;
            } else {
                //not_lfo1:
                lfo_change();
                //    popf
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
            if (pw.fadeout_speed == 0) return this::mnp_ret;
        }
        //vol_set:
        volset();
        return this::mnp_ret;
    }

    public Supplier<Object> mnp_ret() {
        r.al = pw.loop_work;
        r.al &= pw.partWk[r.di].loopcheck;
        pw.loop_work = r.al;
        _ppz();
        return null;
    }

    //==============================================================================
    // Q値の計算
    //  break dx
    //==============================================================================
    public void calc_q() {
        if (pw.md[r.getSi()].dat != 0xc1) { //&& // break cq_sular;

            r.dl = pw.partWk[r.di].qdata;
            if (pw.partWk[r.di].qdatb != 0) { // break cq_set;

                r.stack.push(r.getAx());
                r.al = pw.partWk[r.di].leng;
                r.setAx((short) (r.al * pw.partWk[r.di].qdatb));
                r.dl += r.ah;
                r.setAx(r.stack.pop());
            }
//cq_set:
            if (pw.partWk[r.di].qdat3 != 0) { // break cq_set2;

                // Random-Q
                r.stack.push(r.getAx());
                r.stack.push(r.getCx());
                r.al = pw.partWk[r.di].qdat3;
                r.al &= 0x7f;
                r.setAx(r.al); // cbw
                r.incAx();

                r.stack.push(r.getDx());
                rnd();
                r.setDx(r.stack.pop());

                if ((pw.partWk[r.di].qdat3 & 0x80) == 0) { // break cqr_minus;

                    r.dl += r.al;
//                    break cqr_exit;
                } else {
//cqr_minus:
                    r.carry = (r.dl - r.al) < 0;
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
            if (pw.partWk[r.di].qdat2 != 0) { // break cq_sete;

                r.dh = pw.partWk[r.di].leng;
                r.carry = (r.dh - pw.partWk[r.di].qdat2) < 0;
                r.dh -= pw.partWk[r.di].qdat2;
                if (r.carry) {
//                    break cq_zero;
                    pw.partWk[r.di].qdat = 0; // <<
                    return; // <<
                }
                if (r.dl - r.dh >= 0) { // break cq_sete;
                    r.dl = r.dh; // 最低保証gate値設定
                }
            }
//cq_sete:
            pw.partWk[r.di].qdat = r.dl;
            return;
        }
//cq_sular:
        r.incSi(); // スラー命令
//cq_zero:
        pw.partWk[r.di].qdat = 0;
    }

    //==============================================================================
    // FM音源演奏メイン：パートマスクされている時
    //==============================================================================
    // false : break mnp_ret
    // true : break mp10
    private Supplier<Object> fmmain_nonplay() {
        pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) return this::mnp_ret;

        if ((pw.partWk[r.di].partmask & 2) != 0) { // bit1(FM効果音中？)をcheck
            if (pw.fm_effec_flag == 0) { // ; 効果音終了したか？
                pw.partWk[r.di].partmask &= 0xfd; // bit1をclear
                if (pw.partWk[r.di].partmask == 0) return this::mp10; // partmaskが0なら復活させる
            }
        }
        return this::fmmnp_1;
    }

    private Supplier<Object> fmmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.getSi()];
                r.al = (byte) pw.md[r.incSi()].dat;
                if (r.al == 0x80) break;
                if (r.al < 0x80) return this::fmmnp_3;

                Object o = commands();
                Supplier<Object> fmmnp_1_ = this::fmmnp_1;
                Supplier<Object> mnp_ret_ = this::mnp_ret;
                while (o != null && o !=fmmnp_1_) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_ret_) return this::mnp_ret;
                }

            } while (true);

            FlashMacroList();

            //fmmnp_2:
            // ; END OF MUSIC["L"があった時はそこに戻る]
            r.decSi();
            pw.partWk[r.di].address = r.getSi();
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.subBx(pw.partWk[r.di].partloop);
            if ((r.getBx() & r.getBx()) == 0) return this::fmmnp_4;
            //    ; "L"があった時
            r.setSi(r.getBx());
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
        } while (true);
    }

    public Supplier<Object> fmmnp_3() {
        FlashMacroList();

        pw.partWk[r.di].fnum = 0; // 休符に設定
        pw.partWk[r.di].onkai = (byte) 0xff; // -1
        pw.partWk[r.di].onkai_def = (byte) 0xff; // -1

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].leng = r.al; // 音長設定
        pw.partWk[r.di].keyon_flag++;
        pw.partWk[r.di].address = r.getSi();

        pw.volpush_flag--;
        if (pw.volpush_flag != 0) {
            pw.partWk[r.di].volpush = 0;
        }

        return this::fmmnp_4;
    }

    public Supplier<Object> fmmnp_4() {
        pw.tieflag = 0;
        pw.volpush_flag = 0;
        return this::mnp_ret;
    }

    //==============================================================================
    // ＳＳＧ音源 演奏 メイン
    //==============================================================================
    //psgmain_ret:
    // ret

    private void psgmain() {
        r.setSi(pw.partWk[pw.part_data_table[r.di]].address); // si = PART DATA ADDRESS
        if (r.getSi()  == 0) return;

        //if (r.si == pw.jumpIndex) pw.jumpIndex = -1; // KUMA:Added

        Supplier<Object> ret = null;
        if (pw.partWk[r.di].partmask != 0)
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
        // 音長 -1
        pw.partWk[r.di].leng--;
        r.al = pw.partWk[r.di].leng;

        // KEYOFF CHECK & Keyoff
        if ((pw.partWk[r.di].keyoff_flag & 3) != 0) // 既にkeyoffしたか？
            return this::mp0p;

        if (r.al > pw.partWk[r.di].qdat) // Q値 => 残りLength値時 keyoff
            return this::mp0p;

        keyoffp(); // ALは壊さない
        pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1

        return this::mp0p;
    }

    private Supplier<Object> mp0p() // LENGTH CHECK
    {
        if (r.al != 0) return this::mpexitp;

        pw.partWk[r.di].lfoswi &= 0xf7; // Porta off

        return this::mp1p;
    }

    private Supplier<Object> mp1p() // DATA READ
    {
        pw.cmd = pw.md[r.getSi()];

        //if (r.si == pw.jumpIndex)
        //pw.jumpIndex = -1; // KUMA:Added

        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al < 0x80) return this::mp2p;
        if (r.al == 0x80) return this::mp15p;
        return this::mp1cp;
    }

    // ELSE COMMANDS
    private Supplier<Object> mp1cp() {
        Object o = commandsp();
        Supplier<Object> mp1cp_ = this::mp1cp;
        Supplier<Object> mp1p_ = this::mp1p;
        Supplier<Object> mnp_ret_ = this::mnp_ret;
        while (o != null && o != mp1cp_ && o != mp1p_) {
            o = ((Supplier<Object>) o).get();
            if (o == mnp_ret_) return this::mnp_ret;
        }

        return this::mp1p;
    }

    // END OF MUSIC["L"があった時はそこに戻る]
    private Supplier<Object> mp15p() {
        FlashMacroList();

        r.decSi();
        pw.partWk[r.di].address = r.getSi(); // mov[di],si
        pw.partWk[r.di].loopcheck = 3;
        pw.partWk[r.di].onkai = (byte) 0xff; // -1
        r.setBx(pw.partWk[r.di].partloop);
        if (r.getBx() == 0) return this::mpexitp;

        // "L"があった時
        r.setSi(r.getBx());
        pw.partWk[r.di].loopcheck = 1;
        pw.partWk[r.di].loopCounter++;
        return this::mp1p;
    }

    private Supplier<Object> mp2p() { // TONE SET
        FlashMacroList();

        lfoinitp();
        oshiftp();
        fnumsetp();

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = pw.cmd;
        WriteOPNARegister.accept(cd);

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].leng = r.al;
        calc_q();
        return porta_returnp();
    }

    private Supplier<Object> porta_returnp() {
        if (pw.partWk[r.di].volpush != 0) {
            if (pw.partWk[r.di].onkai != (byte) 0xff) {
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) {
                    pw.volpush_flag = 0;
                    pw.partWk[r.di].volpush = 0;
                }
            }
        }
        //mp_newp:;
        volsetp();
        otodasip();
        keyonp();
        pw.partWk[r.di].keyon_flag++;
        pw.partWk[r.di].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di].keyoff_flag = r.al;
        if (pw.md[r.getSi()].dat != 0xfb) // '&'が直後にあったらkeyoffしない
        {
            return this::mnp_ret;
        }
        pw.partWk[r.di].keyoff_flag = 2;
        return this::mnp_ret;
    }

    private Supplier<Object> mpexitp() {
        r.cl = pw.partWk[r.di].lfoswi;
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
                    r.al = pw.partWk[r.di].lfoswi;
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
        //volsp2:;
        volsetp();
        return mnp_ret();
    }

    //==============================================================================
    // ＳＳＧ音源演奏メイン：パートマスクされている時
    //==============================================================================
    private Supplier<Object> psgmain_nonplay() {
        pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) return this::mnp_ret;

        pw.partWk[r.di].lfoswi &= 0xf7; // Porta off
        return this::psgmnp_1;
    }

    private Supplier<Object> psgmnp_1() {
psgmnp_4:
        do {
            do {
                pw.cmd = pw.md[r.getSi()];
                r.al = (byte) pw.md[r.incSi()].dat;
                if (r.al == (byte) 0x80) break;
                if ((r.al & 0xff) < 0x80) break psgmnp_4;

                if (r.al == (byte) 0xda) { // break psgmnp_3; // Portament?
                    ssgdrum_check(); // の場合だけSSG復活Check
                    if (r.carry) return this::mp1cp; // 復活の場合はメインの処理へ
                }
//psgmnp_3:
                Object o = commandsp();
                Supplier<Object> psgmnp_1_ = this::psgmnp_1;
                Supplier<Object> mnp_ret_ = this::mnp_ret;
                Supplier<Object> porta_returnp_ = this::porta_returnp;
                while (o != null && o != psgmnp_1_) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_ret_)
                        return this::mnp_ret;
                    if (o == porta_returnp_)
                        return this::porta_returnp;
                }

            } while (true);

            //    ; END OF MUSIC["L"があった時はそこに戻る]
            //psgmnp_2:

            FlashMacroList();

            r.decSi();
            pw.partWk[r.di].address = r.getSi();
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di].partloop);

            if ((r.getBx() & r.getBx()) == 0) return this::fmmnp_4;

            //    ; "L"があった時
            r.setSi(r.getBx());
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
        } while (true);

//psgmnp_4:
        ssgdrum_check();
        if (!r.carry) return this::fmmnp_3;

        return this::mp2p; // SSG復活
    }

    //==============================================================================
    // SSGドラムを消してSSGを復活させるかどうかcheck
    //  input AL<- Command
    // output cy = 1 : 復活させる
    //==============================================================================
    private void ssgdrum_check() {
        if ((pw.partWk[r.di].partmask & 1) == 0) { // bit0(SSGマスク中？)をcheck // break sdrchk_2; //SSGマスク中はドラムを止めない
            if ((pw.partWk[r.di].partmask & 2) != 0) { // bit1(SSG効果音中？)をcheck // break sdrchk_2; //SSGドラムは鳴ってない
                if (pw.effon < 2) { // SSGドラム以外の効果音が鳴っているか？ // break sdrchk_2; //普通の効果音は消さない

                    r.ah = r.al;       //ALは壊さない
                    r.ah &= 0xf; // 0DAH(portament)の時は0AHなので大丈夫
                    if (r.ah != 0xf) { // 休符？ // break sdrchk_2; // ; 休符の時はドラムは止めない
                        if (pw.effon == 1) { // SSGドラムはまだ再生中か？ // break sdrchk_1; // 既に消されている
                            r.stack.push(r.getAx());
                            efcdrv.effend(); // SSGドラムを消す
                            r.setAx(r.stack.pop());
                        }
//sdrchk_1:
                        pw.partWk[r.di].partmask &= 0xfd; // bit1をclear
                        if (pw.partWk[r.di].partmask == 0) { // break sdrchk_2; // まだ何かでマスクされている
                            r.carry = true;
                            return; // partmaskが0なら復活させる
                        }
                    }
                }
            }
        }
//sdrchk_2:
        r.carry = false;
    }

    //==============================================================================
    // リズムパート 演奏 メイン
    //==============================================================================
//    private void rhythmmain_ret() {
//    }

    private void rhythmmain() {
        pw.checkJumpIndexSI = true;
        pw.checkJumpIndexBX = false;
        r.setSi(pw.partWk[pw.part_data_table[r.di]].address); // si = PART DATA ADDRESS
        if (r.getSi()  == 0) return;

        //if (r.si == pw.jumpIndex) pw.jumpIndex = -1; // KUMA:Added

        // 音長 -1
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) {
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
            pw.cmd = pw.rd[r.getBx()];
            r.al = (byte) pw.rd[r.getBx()].dat; // rdにはmd(正規の演奏データ)或いはrdDmy(ダミーの演奏データ)のどちらかがセットされている
            r.decBx();

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
        FlashMacroList();

        pw.kshot_dat = 0; // rest
        rlnset();
    }

    /**
     * 音長を読み取り、セット
     */
    private void rlnset() {
        r.al = (byte) pw.rd[r.getBx()].dat; // mov al,[bx]

        if (r.getBx() == pw.jumpIndex)
            pw.jumpIndex = -1; // KUMA:Added

        r.incBx();

        pw.rhyadr = r.getBx();
        pw.partWk[r.di].leng = r.al;
        pw.partWk[r.di].keyon_flag++;

        fmmnp_4();
        mnp_ret();
    }

    //private void mnp_ret()
    //{
    //    r.al = pw.loop_work;
    //    r.al &= pw.partWk[r.di].loopcheck;
    //    pw.loop_work = r.al;
    //    _ppz();
    //    return;
    //}

    private void reom() {
        //KUMA: K part の解析
//reom:
        while (true) {
rfin: // ↑
            {
                pw.checkJumpIndexSI = true;
                pw.checkJumpIndexBX = false;
                do {
                    pw.cmd = pw.md[r.getSi()];
                    r.al = (byte) pw.md[r.getSi()].dat;

                    if (r.getSi() == pw.jumpIndex)
                        pw.jumpIndex = -1; // KUMA:Added スキップ再生向け

                    r.incSi();

                    if (r.al == (byte) 0x80) break rfin;
                    if ((r.al & 0xff) < 0x80) break; // K part に Rn コマンドが指定されていた場合

                    Object o = commandsr();
                    while (o != null) {
                        o = ((Supplier<Object>) o).get();
                    }
                } while (true);

                //KUMA: R part に処理を切り替える準備

                //Console.WriteLine("%d", pw.cmd);

                FlashMacroList();

                MmlDatum md = new MmlDatum(MMLType.TraceLocate, null, LinePos.Copy(pw.cmd.linePos), 0xff);
                md = new MmlDatum(MMLType.TraceLocate, List.of(0, 1, md), LinePos.Copy(pw.cmd.linePos), 0xff);
                ChipDatum cd = new ChipDatum(-1, -1, -1);
                cd.additionalData = md;
                WriteDummy(cd);

                //re00:
                pw.partWk[r.di].address = r.getSi();
                pw.checkJumpIndexSI = false;
                r.ah = 0;
                r.addAx(r.getAx());
                r.addAx((short) pw.radtbl); // KUMA: R part　のアドレステーブル0x00～最大0x7f分存在しうる
                r.setBx(r.getAx());
                r.setAx((short) (pw.md[r.getBx()].dat + pw.md[r.getBx() + 1].dat * 0x100)); // mov ax,[bx]

                r.addAx((short) pw.mmlbuf);
                pw.rhyadr = r.getAx();

                //KUMA: R part に処理を切り替え

                r.setBx(r.getAx());
                pw.rd = pw.md;

                pw.checkJumpIndexBX = true;

//rhyms00:
                while (true) {
                    pw.cmd = pw.rd[r.getBx()];// mov al,[bx]
                    r.al = (byte) pw.rd[r.getBx()].dat;// mov al,[bx]
                    r.incBx();

                    if (r.al == (byte) 0xff) { //KUMA: R part 終端の場合は K part 解析に戻る
                        pw.checkJumpIndexBX = false;
                        continue; // break reom;
                    }

                    //0x00 - 0x7f : 休符
                    //0x80 - 0xbf : 音符(発音)
                    //0xc0 - 0xff : コマンド
                    if ((r.al & 0x80) != 0) { //KUMA: 最上位bitが1かどうかチェック(mmlコマンド/発音かどうかチェック)
                        int r = rhythmon();
                        if (r == 1) continue; // break rhyms00; // KUMA: 1の(連続でコマンドを実行したい)場合はループ
                        pw.checkJumpIndexBX = false;
                        return;
                    }
                    break;
                }

                //KUMA: alが0～0x7fの場合は休符処理

                FlashMacroList();

                pw.kshot_dat = 0; // rest
                rlnset();
                pw.checkJumpIndexBX = false;
                return;
            }
//rfin:
            //KUMA: K part終端処理

            FlashMacroList();

            r.decSi();
            pw.partWk[r.di].address = r.getSi(); // mov[di],si
            pw.partWk[r.di].loopcheck = 3;
            r.setBx(pw.partWk[r.di].partloop);
            if (r.getBx() != 0) { // break rf00;

                //    ; "L"があった時
                r.setSi(r.getBx());
                pw.partWk[r.di].loopcheck = 1;
                pw.partWk[r.di].loopCounter++;
                continue; // break reom;
            }
            break;
        }
//rf00:
        r.setBx((short) 0); // offset rhydmy
        pw.rhyadr = r.getBx();
        pw.rd = pw.rdDmy;

        fmmnp_4();
        mnp_ret();
    }

    //==============================================================================
    // PSGリズム ON
    //==============================================================================
    private int rhythmon() {
        if ((r.al & 0b0100_0000) != 0) { // KUMA: bit6が0の場合はリズム音の発音処理へ break rhy_shot;

            //KUMA: 各コマンド処理はr.siをインデックスとして使うのでbxとsiを入れ替える

            pw.checkJumpIndexSI = true;
            pw.checkJumpIndexBX = false;
            short a = r.getSi();
            r.setSi(r.getBx());
            r.setBx(a);
            r.stack.push(r.getBx());

            Object o = commandsr(); //KUMA: alが示す、コマンド処理をもらってくる
            while (o != null) {
                o = ((Supplier<Object>) o).get(); // KUMA: コマンド実施
            }

            //KUMA: 元に戻す

            pw.checkJumpIndexSI = false;
            pw.checkJumpIndexBX = true;
            r.setBx(r.stack.pop());
            a = r.getSi();
            r.setSi(r.getBx());
            r.setBx(a);
            //rhyms00();
            return 1;
        }
//rhy_shot:
        if (pw.partWk[r.di].partmask != 0) { // break r_nonmask;
            pw.kshot_dat = 0;
            r.incBx();
            rlnset(); // maskされている場合
            return 0;
        }
//r_nonmask:
        r.ah = r.al;
        pw.cmd = pw.rd[r.getBx()];
        r.al = (byte) pw.rd[r.getBx()].dat; // 本来の音符コマンド
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
                //KUMA:SB2でkp_rhythm_flagなら、リズム音源もならす
                r.stack.push(r.getAx());
                r.setBx((short) 0); // offset rhydat
                r.setCx((short) 11);
                //rsb2lp:;
                do {
                    r.carry = ((r.getAx() & 1) != 0);
                    r.setAx((short) ((r.getAx() >> 1) | (r.carry ? 0x8000 : 0)));
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
                    break roret; // fadeout時ppsdrvでなら発音しない
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
//        break rolop;
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
            } // break rolop; // PPSDRVなら２音目以上も鳴らしてみる
        }
//roret:
        r.setBx((short) pw.rhyadr);
        rlnset();
        return 0;
    }

    private void rshot() {
        if (pw.board2 != 0) {
            //rshot:;
            r.setDx((short) (pw.rhydat[r.getBx()] + pw.rhydat[r.getBx() + 1] * 0x100));
            byte x = r.dh;
            r.dh = r.dl;
            r.dl = x;
            r.incBx();
            r.incBx();
            opnset44();
            r.dh = 0x10;
            r.dl = pw.rhydat[r.getBx()];
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
            return; // break rsb200;
        }
    }

    //==============================================================================
    // 各種特殊コマンド処理
    //==============================================================================
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
                ExecIDESpecialCommand(md);
            }
        }

        //if (r.si - 1 < pw.md.length && pw.md[r.si - 1].type == MMLType.IDE) { // KUMA: Added
        //    //alレジスタは無関係でtypeがIDEならばIDE向け特殊コマンドとして処理する
        //    //(コンパイラ側はIDEからのコンパイル要求時のみこの状態を作り出すように調整が必要。)
        //    ExecIDESpecialCommand(pw.md[r.si - 1]);
        //    return null;
        //}

        if (r.al < pw.com_end) {
            return out_of_commands();
        }

        r.setBx((byte) ~r.al);
        if (pw.ppz != 0) {
            r.stack.push(r.getAx());
            _ppz();
            r.setAx(r.stack.pop());
        }

        logger.log(Level.TRACE, String.format("bx:%d di:%d", r.getBx(), r.di));

        Supplier<Object> o = pw.currentCommandTable[r.getBx()];

        if (o == null)
            logger.log(Level.ERROR, String.format("bx:%d di:%d", r.getBx(), r.di));

        return o;
    }

    private Supplier<Object> out_of_commands() {
        r.decSi();
        pw.md[r.getSi()].dat = 0x80; //Part END
        return null;
    }

    //1745-2033
    private Supplier<Object>[] cmdtbl;

    private void SetupCmdtbl() {
        cmdtbl = new Supplier[] {
                this::comAt                     // 0xff(0)
                , this::comq                     // 0xfe(1)
                , this::comv                     // 0xfd(2)
                , this::comt                     // 0xfc(3)
                , this::comtie                   // 0xfb(4)
                , this::comd                     // 0xfa(5)
                , this::comstloop                // 0xf9(6)
                , this::comedloop                // 0xf8(7)
                , this::comexloop                // 0xf7(8)
                , this::comlopset                // 0xf6(9)
                , this::comshift                 // 0xf5(10)
                , this::comvolup                 // 0xf4(11)
                , this::comvoldown               // 0xf3(12)
                , this::lfoset                   // 0xf2(13)
                , this::lfoswitch_f              // 0xf1(14)
                , this::jump4                    // 0xf0(15)
                , this::comy                     // 0xef(16)
                , this::jump1                    // 0xee(17)
                , this::jump1                    // 0xed(18)
                //FOR SB2
                , this::panset                   // 0xec(19)
                , this::rhykey                   // 0xeb(20)
                , this::rhyvs                    // 0xea(21)
                , this::rpnset                   // 0xe9(22)
                , this::rmsvs                    // 0xe8(23)
                //追加 for V2.0
                , this::comshift2                // 0xe7(24)
                , this::rmsvs_sft                // 0xe6(25)
                , this::rhyvs_sft                // 0xe5(26)
                //
                , this::hlfo_delay               // 0xe4(27)
                //追加 for V2.3
                , this::comvolup2                // 0xe3(28)
                , this::comvoldown2              // 0xe2(29)
                //追加 for V2.4
                , this::hlfo_set                 // 0xe1(30)
                , this::hlfo_onoff               // 0xe0(31)
                //
                , this::syousetu_lng_set         // 0xdf(32)
                //
                , this::vol_one_up_fm            // 0xde(33)
                , this::vol_one_down             // 0xdd(34)
                //
                , this::status_write             // 0xdc(35)
                , this::status_add               // 0xdb(36)
                //
                , this::porta                    // 0xda(37)
                //
                , this::jump1                    // 0xd9(38)
                , this::jump1                    // 0xd8(39)
                , this::jump1                    // 0xd7(40)
                //
                , this::mdepth_set               // 0xd6(41)
                //
                , this::comdd                    // 0xd5(42)
                //
                , this::ssg_efct_set             // 0xd4(43)
                , this::fm_efct_set              // 0xd3(44)
                , this::fade_set                 // 0xd2(45)
                //
                , this::jump1                    // 0xd1(46)
                //
                , this::jump1                    // 0xd0(47)
                //
                , this::slotmask_set             // 0xcf(48)
                , this::jump6                    // 0xce(49)
                , this::jump5                    // 0xcd(50)
                , this::jump1                    // 0xcc(51)
                , this::lfowave_set              // 0xcb(52)
                , this::lfo_extend               // 0xca(53)
                , this::jump1                    // 0xc9(54)
                , this::slotdetune_set           // 0xc8(55)
                , this::slotdetune_set2          // 0xc7(56)
                , this::fm3_extpartset           // 0xc6(57)
                , this::volmask_set              // 0xc5(58)
                , this::comq2                    // 0xc4(59)
                , this::panset_ex                // 0xc3(60)
                , this::lfoset_delay             // 0xc2(61)
                , this::jump0                    // 0xc1(62) ,sular
                , this::fm_mml_part_mask         // 0xc0(63)
                , this::_lfoset                  // 0xbf(64)
                , this::_lfoswitch_f              // 0xbe(65)
                , this::_mdepth_set              // 0xbd(66)
                , this::_lfowave_set              // 0xbc(67)
                , this::_lfo_extend              // 0xbb(68)
                , this::_volmask_set          // 0xba(69)
                , this::_lfoset_delay          // 0xb9(70)
                , this::tl_set                  // 0xb8(71)
                , this::mdepth_count              // 0xb7(72)
                , this::fb_set                      // 0xb6(73)
                , this::slot_delay              // 0xb5(74)
                , this::jump16                      // 0xb4(75)
                , this::comq3                      // 0xb3(76)
                , this::comshift_master          // 0xb2(77)
                , this::comq4                  // 0xb1(78)
        };
    }

    //com_end equ 0b1h

    private Supplier<Object>[] cmdtblp;

    private void SetupCmdtblp() {
        cmdtblp = new Supplier[] {
                this::jump1                       //(0xff)0
                , this::comq                       //(0xfe)1
                , this::comv                       //(0xfd)2
                , this::comt                       //(0xfc)3
                , this::comtie                     //(0xfb)4
                , this::comd                       //(0xfa)5
                , this::comstloop                  //(0xf9)6
                , this::comedloop                  //(0xf8)7
                , this::comexloop                  //(0xf7)8
                , this::comlopset                  //(0xf6)9
                , this::comshift                   //(0xf5)10
                , this::comvolupp                  //(0xf4)11
                , this::comvoldownp                //(0xf3)12
                , this::lfoset                     //(0xf2)13
                , this::lfoswitch                  //(0xf1)14
                , this::psgenvset                  //(0xf0)15
                , this::comy                       //(0xef)16
                , this::psgnoise                   //(0xee)17
                , this::psgsel                     //(0xed)18
                ////
                , this::jump1                      //(0xec)19
                , this::rhykey                     //(0xeb)20
                , this::rhyvs                      //(0xea)21
                , this::rpnset                     //(0xe9)22
                , this::rmsvs                      //(0xe8)23
                ////
                , this::comshift2                  //(0xe7)24
                , this::rmsvs_sft                  //(0xe6)25
                , this::rhyvs_sft                  //(0xe5)26
                ////
                , this::jump1                      //(0xe4)27
                ////追加 for V2.3
                , this::comvolupp2                 //0E3H 28
                , this::comvoldownp2                //0E2H 29
                ////
                , this::jump1                      //0E1H 30
                , this::jump1                        //0E0H 31
                ////
                , this::syousetu_lng_set           //0DFH 32
                ////
                , this::vol_one_up_psg             //0DEH 33
                , this::vol_one_down                //0DDH 34
                ////
                , this::status_write               //0DCH 35
                , this::status_add                 //0DBH 36
                ////
                , this::portap                     //0DAH 37
                ////
                , this::jump1                      //0D9H 38
                , this::jump1                        //0D8H 39
                , this::jump1                        //0D7H 40
                ////
                , this::mdepth_set                 //0D6H 41
                ////
                , this::comdd                      //0d5h 42
                ////
                , this::ssg_efct_set               //0d4h 43
                , this::fm_efct_set                //0d3h 44
                , this::fade_set                //0d2h 45
                ////
                , this::jump1                      //(0xd1)46
                , this::psgnoise_move              //0d0h 47
                ////
                , this::jump1                      //(0xcf) 48
                , this::jump6                      //0ceh 49
                , this::extend_psgenvset           //0cdh 50
                , this::detune_extend                //0cch 51
                , this::lfowave_set                //0cbh 52
                , this::lfo_extend                    //0cah 53
                , this::envelope_extend            //0c9h 54
                , this::jump3                        //0c8h 55
                , this::jump3                        //0c7h 56
                , this::jump6                        //0c6h 57
                , this::jump1                        //0c5h 58
                , this::comq2                    //0c4h 59
                , this::jump2                        //0c3h 60
                , this::lfoset_delay                //0c2h 61
                , this::jump0                    //0c1h,sular 62
                , this::ssg_mml_part_mask            //0c0h 63
                , this::_lfoset                    //0bfh 64
                , this::_lfoswitch                //0beh 65
                , this::_mdepth_set                //0bdh 66
                , this::_lfowave_set            //0bch 67
                , this::_lfo_extend                //0bbh 68
                , this::jump1                    //0bah 69
                , this::_lfoset_delay                //0b9h 70
                , this::jump2                      //0b8h 71
                , this::mdepth_count                //0b7h 72
                , this::jump1
                , this::jump2
                , this::jump16                        //0b4h
                , this::comq3                        //0b3h
                , this::comshift_master            //0b2h
                , this::comq4                    //0b1h
        };
    }

    private Supplier<Object>[] cmdtblr;

    private void SetupCmdtblr() {
        cmdtblr = new Supplier[] {
                this::jump1                      //0xff 0
                , this::jump1                      //0xfe 1
                , this::comv                       //0xfd 2
                , this::comt                       //0xfc 3
                , this::comtie                     //0xfb 4
                , this::comd                       //0xfa 5
                , this::comstloop                  //0xf9 6
                , this::comedloop                  //0xf8 7
                , this::comexloop                  //0xf7 8
                , this::comlopset                  //0xf6 9
                , this::jump1                      //0xf5 10
                , this::comvolupp                  //0xf4 11
                , this::comvoldownp                //0xf3 12
                , this::jump4                      //0xf2 13
                , this::pdrswitch                  //0xf1 14
                , this::jump4                      //0xf0 15
                , this::comy                       //0xef 16
                , this::jump1                      //0xee 17
                , this::jump1                      //0xed 18
                //
                , this::jump1                      //0xec 19
                , this::rhykey                     //0xeb 20
                , this::rhyvs                      //0xea 21
                , this::rpnset                     //0xe9 22
                , this::rmsvs                      //0xe8 23
                //
                , this::jump1                      //0xe7 24
                , this::rmsvs_sft                  //0xe6 25
                , this::rhyvs_sft                  //0xe5 26
                //
                , this::jump1                      //0E4H 27
                //
                , this::comvolupp2                 //0E3H 28
                , this::comvoldownp2                //0E2H 29
                //
                , this::jump1                      //0E1H 30
                , this::jump1                        //0E0H 31
                //
                , this::syousetu_lng_set           //0DFH 32
                //
                , this::vol_one_up_psg             //0DEH 33
                , this::vol_one_down               //0DDH 34
                //
                , this::status_write               //0DCH 35
                , this::status_add                    //0DBH 36
                //
                , this::jump1                      // ポルタメント＝通常音程コマンドに 0xda 37
                //
                , this::jump1                      //0D9H 38
                , this::jump1                        //0D8H 39
                , this::jump1                        //0D7H 40
                //
                , this::jump2                      //0D6H 41
                //
                , this::comdd                      //0d5h 42
                //
                , this::ssg_efct_set               //0d4h 43
                , this::fm_efct_set                //0d3h 44
                , this::fade_set                    //0d2h 45
                //
                , this::jump1                      //0xd1 46
                , this::jump1                      //0d0h 47
                //
                , this::jump1                      //0xcf 48
                , this::jump6                      //0ceh 49
                , this::jump5                        //0cdh 50
                , this::jump1                        //0cch 51
                , this::jump1                      //0xcb 52
                , this::jump1                      //0xca 53
                , this::jump1                      //0xc9 54
                , this::jump3                      //0xc8 55
                , this::jump3                      //0xc7 56
                , this::jump6                      //0xc6 57
                , this::jump1                        //0c5h 58
                , this::jump1                      //0xc4 59
                , this::jump2                        //0c3h 60
                , this::jump1                      //0xc2 61
                , this::jump0                        //0c1h,sular 62
                , this::rhythm_mml_part_mask       //0c0h 63
                , this::jump4                        //0bfh 64
                , this::jump1                        //0beh 65
                , this::jump2                        //0bdh 66
                , this::jump1                        //0bch 67
                , this::jump1                        //0bbh 68
                , this::jump1                        //0bah 69
                , this::jump1                        //0b9h 70
                , this::jump2                      //0xb8 71
                , this::jump1                      //0xb7 72
                , this::jump1                      //0xb6 73
                , this::jump2                      //0xb5 74
                , this::jump16                        //0b4h 75
                , this::jump1                      //0xb3 76
                , this::jump1                        //0b2h 77
                , this::jump1                        //0b1h 78
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

    //==============================================================================
    // 0c0hの追加special命令
    //==============================================================================
    public Supplier<Object> special_0c0h() {
        if (r.al < pw.com_end_0c0h) {
            return this::out_of_commands;
        }

        r.al = (byte) ~r.al;
        r.al += r.al;
        r.ah = 0;
        r.setBx(r.getAx());

        return comtbl0c0h[r.getBx() / 2];
    }

    private Supplier<Object>[] comtbl0c0h;

    private void SetupComtbl0c0h() {
        comtbl0c0h = new Supplier[] {
                this::vd_fm // 0ffh
                , this::_vd_fm
                , this::vd_ssg
                , this::_vd_ssg
                , this::vd_pcm
                , this::_vd_pcm
                , this::vd_rhythm
                , this::_vd_rhythm    //0f8h
                , this::pmd86_s
                , this::vd_ppz
                , this::_vd_ppz //0f5h
        };
    }

    //==============================================================================
    // /s option制御
    //==============================================================================
    private Supplier<Object> pmd86_s() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 1;
        pw.pcm86_vol = r.al;
        return null;
    }

    //==============================================================================
    // 各種Voldown
    //==============================================================================
    private Supplier<Object> vd_fm() {
        r.setBx((short) 0); // offset fm_voldown

        //vd_main:;
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.fm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_ssg() {
        r.setBx((short) 0); // offset ssg_voldown
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.ssg_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_pcm() {
        r.setBx((short) 0); // offset pcm_voldown
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.pcm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_rhythm() {
        r.setBx((short) 0); // offset rhythm_voldown
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.rhythm_voldown = r.al;
        return null;
    }

    private Supplier<Object> vd_ppz() {
        r.setBx((short) 0); // offset ppz_voldown
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.ppz_voldown = r.al;
        return null;
    }

    private Supplier<Object> _vd_fm() {
        _vd_main(/* ref */ pw.fm_voldown, pw._fm_voldown);
        return null;
    }

    private void _vd_main(/* ref */ byte a, byte b) {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al != 0) { // break _vd_reset;
            if ((r.al & 0x80) == 0) { // break _vd_sign;

                r.carry = a + r.al > 0xff;
                a += r.al;
                if (r.carry) { // break _vd_ret;
                    a = (byte) 255;
                }
//_vd_ret:
                return;
            }
//_vd_sign:
            r.carry = a + r.al > 0xff;
            a += r.al;
            if (!r.carry) { // break _vd_ret;
                return;
            }
            a = 0;
            return;
        }
//_vd_reset:
        a = b;
    }

    private Supplier<Object> _vd_ssg() {
        _vd_main(/* ref */ pw.ssg_voldown, pw._ssg_voldown);
        return null;
    }

    private Supplier<Object> _vd_pcm() {
        _vd_main(/* ref */ pw.pcm_voldown, pw._pcm_voldown);
        return null;
    }

    private Supplier<Object> _vd_rhythm() {
        _vd_main(/* ref */ pw.rhythm_voldown, pw._rhythm_voldown);
        return null;
    }

    private Supplier<Object> _vd_ppz() {
        _vd_main(/* ref */ pw.ppz_voldown, pw._ppz_voldown);
        return null;
    }

    //==============================================================================
    // slot keyon delay
    //==============================================================================
    private Supplier<Object> slot_delay() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0xf;
        r.al ^= 0xf;
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        pw.partWk[r.di].sdelay_m = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].sdelay = r.al;
        pw.partWk[r.di].sdelay_c = r.al;
        return null;
    }

    //==============================================================================
    // FB変化
    //==============================================================================
    private Supplier<Object> fb_set() {
        r.dh = (byte) (0xb0 - 1);
        r.dh += pw.partb; // dh=ALG/FB port address
        r.al = (byte) pw.md[r.incSi()].dat;
        if ((r.al & 0x80) == 0) { // break _fb_set;
//fb_set2:
            //  ;in al 00000xxx 設定するFB
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
        }
//fb_set3:
        while (true) {
            //  ;in al 00xxx000 設定するFB
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
                    if ((pw.partWk[r.di].slotmask & 0x10) == 0) { // slot1を使用していなければ
//                    break fb_ret; // 出力しない
                        return null;
                    }
                    r.dl = pw.fm3_alg_fb;
                    r.dl &= 7;
                    r.dl |= r.al;
                    pw.fm3_alg_fb = r.dl;
//                break fb_exit;
                } else {
//fb_notfm3:
                    r.dl = pw.partWk[r.di].alg_fb;
                    r.dl &= 0x7;
                    r.dl |= r.al;
                }
//fb_exit:
                opnset();
                pw.partWk[r.di].alg_fb = r.dl;
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
                r.dl = pw.partWk[r.di].alg_fb;
            }
//_fb_next:
            r.dl = r.rol(r.dl, 1);
            r.dl = r.rol(r.dl, 1);
            r.dl = r.rol(r.dl, 1);
            r.dl &= 7;
            r.al += r.dl;
            if ((r.al & 0x80) == 0) { // break _fb_zero;
                if (r.al < 8) {
//                    break fb_set2;
//fb_set2:
                    //  ;in al 00000xxx 設定するFB
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

    //==============================================================================
    // TL変化
    //==============================================================================
    private Supplier<Object> tl_set() {
        r.dh = 0x40 - 1;
        r.dh += pw.partb; // dh=TL FM Port Address
        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.ah &= 0xf;
        r.ch = pw.partWk[r.di].slotmask; // ch=slotmask 43210000
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ch = r.ror(r.ch, 1);
        r.ah &= r.ch; // ah=変化させるslot 00004321
        r.dl = (byte) pw.md[r.getSi()].dat; // dl=変化値
        r.incSi();
        r.setBx((short) 0); // offset opnset
        if (pw.partWk[r.di].partmask != 0) { // パートマスクされているか？ // break ts_00;
            r.setBx((short) 1); // offset dummy_ret
        }
//ts_00:
        if ((r.al & 0x80) == 0) { // break tl_slide;
            r.dl &= 127;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_01;
                pw.partWk[r.di].slot1 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_01:
            r.dh += 8;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_02;
                pw.partWk[r.di].slot2 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_02:
            r.dh -= 4;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_03;
                pw.partWk[r.di].slot3 = r.dl;
                if (r.getBx() == 0) opnset();
            }
//ts_03:
            r.dh += 8;
            r.ah = r.ror(r.ah, 1);
            if (r.carry) { // break ts_04;
                pw.partWk[r.di].slot4 = r.dl;
                if (r.getBx() == 0) opnset();
                //dummy_ret:;
            }
//ts_04:
            return null;
        }
        // 相対変化
//tl_slide:
        r.al = r.dl;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_01;
            r.dl = pw.partWk[r.di].slot1;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_0b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_0b;
                    r.dl = 127;
                }
            }
//tls_0b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di].slot1 = r.dl;
        }
//tls_01:
        r.dh += 8;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_02;
            r.dl = pw.partWk[r.di].slot2;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_1b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_1b;
                    r.dl = 127;
                }
            }
//tls_1b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di].slot2 = r.dl;
        }
//tls_02:
        r.dh -= 4;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_03;
            r.dl = pw.partWk[r.di].slot3;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_2b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { // break tls_2b;
                    r.dl = 127;
                }
            }
//tls_2b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di].slot3 = r.dl;
        }
//tls_03:
        r.dh += 8;
        r.ah = r.ror(r.ah, 1);
        if (r.carry) { // break tls_04;
            r.dl = pw.partWk[r.di].slot4;
            r.dl += r.al;
            if ((r.dl & 0x80) != 0) { // break tls_3b;
                r.dl = 0;
                if ((r.al & 0x80) == 0) { //break tls_3b;
                    r.dl = 127;
                }
            }
//tls_3b:
            if (r.getBx() == 0) opnset();
            pw.partWk[r.di].slot4 = r.dl;
        }
//tls_04:
        return null;
    }

    //==============================================================================
    // 演奏中パートのマスクon/off
    //==============================================================================
    private Supplier<Object> fm_mml_part_mask() {
        logger.log(Level.TRACE, "fm_mml_part_mask");

        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break fm_mml_part_maskoff;

            pw.partWk[r.di].partmask |= 0x40;
            if (pw.partWk[r.di].partmask == 0x40) { // break fmpm_ret;

                silence_fmpart(); // 音消去
            }
//fmpm_ret:
            //r.ax = r.stack.pop(); // commands
            return this::fmmnp_1; // パートマスク時の処理に移行
        }
//fm_mml_part_maskoff:

        pw.partWk[r.di].partmask &= 0xbf;
        if (pw.partWk[r.di].partmask != 0) {
//            break fmpm_ret;
            return this::fmmnp_1; // パートマスク時の処理に移行 // <<
        }
        neiro_reset(); // 音色再設定
        //r.ax = r.stack.pop(); // commands
        return this::mp1; // パート復活
    }

    private Supplier<Object> ssg_mml_part_mask() {
        logger.log(Level.TRACE, "ssg_mml_part_mask");

        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break ssg_part_maskoff_ret;

            pw.partWk[r.di].partmask |= 0x40;
            if (pw.partWk[r.di].partmask == 0x40) { // break smpm_ret;

                psgmsk(); // AL=07h AH = Maskdata
                r.dh = 7;
                r.dl = r.al;
                r.dl |= r.ah;
                opnset44(); // PSG keyoff
            }
//smpm_ret:

            //r.ax = r.stack.pop(); // commandsp
            return this::psgmnp_1;
        }
//ssg_part_maskoff_ret:

        pw.partWk[r.di].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di].partmask != 0) {
//            break smpm_ret;
            return this::psgmnp_1; // <<
        }
        //r.ax = r.stack.pop(); // commandsp
        return this::mp1p; // パート復活
    }

    private Supplier<Object> rhythm_mml_part_mask() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al >= 2)
            return this::special_0c0h;

        if (r.al != 0) { // break rhythm_part_maskoff_ret;

            pw.partWk[r.di].partmask |= 0x40;
            return null;
        }
//rhythm_part_maskoff_ret:
        pw.partWk[r.di].partmask &= (byte) 0xbf;
        return null;
    }

    //==============================================================================
    // FM音源の音色を再設定
    //==============================================================================
    private void neiro_reset() {
        if (pw.partWk[r.di].neiromask != 0) { // break nr_ret;

            r.dl = pw.partWk[r.di].voicenum;
            r.bl = pw.partWk[r.di].slot1; //    mov bx, word ptr slot1[di]; bh=s3 bl = s1
            r.bh = pw.partWk[r.di].slot3;
            r.cl = pw.partWk[r.di].slot2; //    mov cx, word ptr slot2[di]; ch=s4 cl = s2
            r.ch = pw.partWk[r.di].slot4;
            r.stack.push(r.getBx());
            r.stack.push(r.getCx());
            pw.af_check = 1;
            neiroset(); // 音色復帰
            pw.af_check = 0;
            r.setCx(r.stack.pop());
            r.setBx(r.stack.pop());
            pw.partWk[r.di].slot1 = r.bl;
            pw.partWk[r.di].slot3 = r.bh;
            pw.partWk[r.di].slot2 = r.cl;
            pw.partWk[r.di].slot4 = r.ch;
            r.al = pw.partWk[r.di].carrier;
            r.al = (byte) ~r.al;
            r.al &= pw.partWk[r.di].slotmask; // al<- TLを再設定していいslot 4321xxxx
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
                r.dh += 0xb4 - 1;
                calc_panout();
                opnset(); // パン復帰
            }
        }
//nr_ret:
    }

    //==============================================================================
    // PDRのswitch
    //==============================================================================
    private Supplier<Object> pdrswitch() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.ppsdrv_flag != 0) { // break pdrsw_ret;

            r.dl = r.al;
            r.dl &= 1;
            r.al >>= 1;
            r.ah = 5;
            ChipDatum cd = new ChipDatum(0x03, r.al, r.dl);
            ppsdrv.apply(cd); // .SetParam(r.al, r.dl); // int ppsdrv
        }
//pdrsw_ret:
        return null;
    }

    //==============================================================================
    // 音量マスクslotの設定
    //==============================================================================
    private Supplier<Object> volmask_set() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0xf;
        if (r.al != 0) { // break vms_zero;

            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1); // 上位4BITに移動

            r.al |= 0xf; // ０以外を指定した=下位4BITを１にする
            pw.partWk[r.di].volmask = r.al;
            return this::ch3_setting;
        }
//vms_zero:
        r.al = pw.partWk[r.di].carrier;
        pw.partWk[r.di].volmask = r.al; // キャリア位置を設定

        return this::ch3_setting;
    }

    public Supplier<Object> _volmask_set() {
        logger.log(Level.TRACE, "_volmask_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0xf;
        if (r.al != 0) { // break _vms_zero;

            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1);
            r.al = r.rol(r.al, 1); // 上位4BITに移動
            r.al |= 0xf; // ０以外を指定した=下位4BITを１にする
            pw.partWk[r.di]._volmask = r.al;
            return this::ch3_setting;
        }
//_vms_zero:
        r.al = pw.partWk[r.di].carrier;
        pw.partWk[r.di]._volmask = r.al; // キャリア位置を設定

        return this::ch3_setting;
    }

    //==============================================================================
    // パートを判別してch3ならmode設定
    //==============================================================================
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

            ch3mode_set(); // FM3chの場合のみ ch3modeの変更処理

            r.carry = true;
            return null;
        }
//vms_not_p3:
        r.carry = false;
        return null;
    }

    //==============================================================================
    // FM3ch 拡張パートセット
    //==============================================================================
    private Supplier<Object> fm3_extpartset() {
        logger.log(Level.TRACE, "fm3_extpartset");

        r.stack.push(r.di);

        r.setAx((short) ((byte) pw.md[r.getSi()].dat + (byte) pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        if (r.getAx() != 0) { // break fm3ext_part3c;
            r.addAx((short) pw.mmlbuf);
            r.di = (short) pw.part3b; // offset part3b
            fm3_partinit();
        }
//fm3ext_part3c:

        r.setAx((short) ((byte) pw.md[r.getSi()].dat + (byte) pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        if (r.getAx() != 0) { // break fm3ext_part3d;
            r.addAx((short) pw.mmlbuf);
            r.di = (short) pw.part3c; // offset part3c
            fm3_partinit();
        }
//fm3ext_part3d:

        r.setAx((short) ((byte) pw.md[r.getSi()].dat + (byte) pw.md[r.getSi() + 1].dat * 0x100));
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
        pw.partWk[r.di].address = r.getAx();
        pw.partWk[r.di].leng = 1; // アト 1カウント デ エンソウ カイシ
        r.al = (byte) 0xff; // -1
        pw.partWk[r.di].keyoff_flag = r.al; // 現在keyoff中
        pw.partWk[r.di].mdc = r.al; // MDepth Counter(無限)
        pw.partWk[r.di].mdc2 = r.al;
        pw.partWk[r.di]._mdc = r.al;
        pw.partWk[r.di]._mdc2 = r.al;
        pw.partWk[r.di].onkai = r.al; // rest
        pw.partWk[r.di].onkai_def = r.al; // rest
        pw.partWk[r.di].volume = 108; // FM VOLUME DEFAULT= 108
        r.setBx((short) pw.part3); // offset part3
        r.al = pw.partWk[r.getBx()].fmpan;
        pw.partWk[r.di].fmpan = r.al; // FM PAN = CH3と同じ
        pw.partWk[r.di].partmask |= 0x20; // s0用 partmask
        return;
    }

    //==============================================================================
    // Detune Extend Set
    //==============================================================================
    private Supplier<Object> detune_extend() {
        logger.log(Level.TRACE, "detune_extend");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 1;
        pw.partWk[r.di].extendmode &= 0xfe;
        pw.partWk[r.di].extendmode |= r.al;
        return null;
    }

    //==============================================================================
    // LFO Extend Set
    //==============================================================================
    public Supplier<Object> lfo_extend() {
        logger.log(Level.TRACE, "lfo_extend");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 1;
        r.al <<= 1;

        pw.partWk[r.di].extendmode &= 0xfd;
        pw.partWk[r.di].extendmode |= r.al;
        return null;
    }

    //==============================================================================
    // Envelope Extend Set
    //==============================================================================
    public Supplier<Object> envelope_extend() {
        logger.log(Level.TRACE, "envelope_extend");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 1;
        r.al <<= 1;
        r.al <<= 1;

        pw.partWk[r.di].extendmode &= 0xfb;
        pw.partWk[r.di].extendmode |= r.al;
        return null;
    }

    //==============================================================================
    // LFOのWave選択
    //==============================================================================
    public Supplier<Object> lfowave_set() {
        logger.log(Level.TRACE, "lfowave_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].lfo_wave = r.al;

        return null;
    }

    //==============================================================================
    // PSG Envelope set(Extend)
    //==============================================================================
    public Supplier<Object> extend_psgenvset() {
        logger.log(Level.TRACE, "extend_psgenvset");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0x1f;
        pw.partWk[r.di].eenv_ar = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0x1f;
        pw.partWk[r.di].eenv_dr = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0x1f;
        pw.partWk[r.di].eenv_sr = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;

        r.al &= 0x0f;
        pw.partWk[r.di].eenv_rr = r.al;

        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);
        r.ah = r.rol(r.ah, 1);

        r.ah &= 0xf;
        r.ah ^= 0xf;
        pw.partWk[r.di].eenv_sl = r.ah;

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 0x0f;
        pw.partWk[r.di].eenv_al = r.al;

        if (pw.partWk[r.di].envf != 0xff) { // break not_set_count; // ノーマル＞拡張に移行したか？

            pw.partWk[r.di].envf = (byte) 0xff;

            pw.partWk[r.di].eenv_count = 4; // RR
            pw.partWk[r.di].eenv_volume = 0; // Volume
        }
//not_set_count:

        return null;
    }

    //==============================================================================
    // Slot Detune Set(相対)
    //==============================================================================
    private Supplier<Object> slotdetune_set2() {
        logger.log(Level.TRACE, "slotdetune_set2");

        if (pw.partb != 3) // FM3CH目しか指定出来ない
            return this::jump3;
        if (pw.board2 != 0) {
            if (pw.fmsel == 1) // 裏では指定出来ない
                return this::jump3;
        }

        r.al = (byte) pw.md[r.incSi()].dat;
        r.bl = r.al;
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
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

    //==============================================================================
    // Slot Detune Set
    //==============================================================================
    private Supplier<Object> slotdetune_set() {
        logger.log(Level.TRACE, "slotdetune_set");

        if (pw.partb != 3) // FM3CH目しか指定出来ない
            return this::jump3;
        if (pw.board2 != 0) {
            if (pw.fmsel == 1) // 裏では指定出来ない
                return this::jump3;
        } else {
            if (r.di == pw.part_e)
                return this::jump3;
        }

        r.al = (byte) pw.md[r.incSi()].dat;
        r.bl = r.al;
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);

        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) (((r.bl >> 1) & 0x7f) | (r.bl << 7));
        if (r.carry) { // break sds_slot2;
            pw.slot_detune1 = r.getAx();
        }
//sds_slot2:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) (((r.bl >> 1) & 0x7f) | (r.bl << 7));
        if (r.carry) { // break sds_slot3;
            pw.slot_detune2 = r.getAx();
        }
//sds_slot3:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) (((r.bl >> 1) & 0x7f) | (r.bl << 7));
        if (r.carry) { // break sds_slot4;
            pw.slot_detune3 = r.getAx();
        }
//sds_slot4:
        r.carry = ((r.bl & 0x01) != 0);
        r.bl = (byte) (((r.bl >> 1) & 0x7f) | (r.bl << 7));
        if (!r.carry) return this::sds_check;
        pw.slot_detune4 = r.getAx();
        return this::sds_check;
    }

    private Supplier<Object> sds_check() {
        r.setAx(pw.slot_detune1);
        r.orAx(pw.slot_detune2);
        r.orAx(pw.slot_detune3);
        r.orAx(pw.slot_detune4); // 全部０か？
        if (r.getAx() != 0) { // break sdf_set;
            r.al = 1;
        }
//sdf_set:
        pw.slotdetune_flag = r.al;
        ch3mode_set();
        return null;
    }

    //==============================================================================
    // FM3のmodeを設定する
    //==============================================================================
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
                        if ((pw.partWk[r.di].slotmask & 0xf0) != 0) { //s0 // break cm_clear;
                            if (pw.partWk[r.di].slotmask != (byte) 0xf0)
                                break cm_set;
                            if ((pw.partWk[r.di].volmask & 0x0f) == 0)
                                break cm_clear;
                            if ((pw.partWk[r.di].lfoswi & 0x1) != 0)
                                break cm_set;

                            //cm_noset1:;
                            if ((pw.partWk[r.di]._volmask & 0x0f) == 0)
                                break cm_clear;
                            if ((pw.partWk[r.di].lfoswi & 0x10) != 0)
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
            if ((pw.partWk[r.di].partmask & 2) != 0) // Effect/パートマスクされているか？
            {
                cm_nowefcplaying();
                return;
            }
        }

        if (r.ah != pw.ch3mode) { // break cm_exit; // ; 以前と変更無しなら何もしない

            pw.ch3mode = r.ah;
            r.dh = 0x27;
            r.dl = r.ah;
            r.dl &= 0b1100_1111; // Resetはしない
            opnset44();

            // 効果音モードに移った場合はそれ以前のFM3パートで音程書き換え
            if (r.ah != 0x3f) { // break cm_exit;
                if (r.di != pw.part3) { // break cm_exit;

                    //cm_otodasi:;

                    r.stack.push(r.bp);
                    r.bp = r.di;
                    r.stack.push(r.di);
                    r.di = (short) pw.part3; // offset part3
                    otodasi_cm();

                    //cm_3bchk:;
                    if (r.bp != pw.part3b) { // break cm_exit2;
                        r.di = (short) pw.part3; // offset part3b
                        otodasi_cm();

                        //cm_3cchk:;
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
        if (pw.partWk[r.di].partmask == 0) { // break ocm_ret;
            otodasi();
        }
//ocm_ret:
    }

    private void cm_nowefcplaying() {
        if (pw.board2 == 0) {
            pw.ch3mode_push = r.ah;
        }
    }

    //==============================================================================
    // FM slotmask set
    //==============================================================================
    private Supplier<Object> slotmask_set() {
        logger.log(Level.TRACE, "slotmask_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.al &= 0xf;
        if (r.al != 0) { // break sm_not_car;

            r.al = (byte) ((r.al << 4) | ((r.al >> 4) & 0x0f));
            pw.partWk[r.di].carrier = r.al;
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
                //r.dl = pw.partWk[r.di].voicenum;
                //r.stack.push(r.ax);
                //toneadr_calc();
                //r.ax = r.stack.pop();
                //r.bl = (byte)pw.inst[r.bx + 24].dat;
                r.bl = pw.partWk[r.di].alg_fb;
            }
//sm_car_set:
            r.bh = 0;
            r.bl &= 7;
            r.addBx((short) 0); // offset carrier_table
            r.al = (byte) pw.carrier_table[r.getBx()];
            pw.partWk[r.di].carrier = r.al;
        }
//sm_set:
        r.ah &= 0xf0;
        if (pw.partWk[r.di].slotmask != r.ah) { // break sm_no_change;
            pw.partWk[r.di].slotmask = r.ah;
            if ((r.ah & 0xf0) == 0) { // break sm_noset_pm;
                pw.partWk[r.di].partmask |= 0x20; // s0の時パートマスク
//                break sms_ns;
            } else {
//sm_noset_pm:
                pw.partWk[r.di].partmask &= 0xdf; // s0以外の時パートマスク解除
            }
//sms_ns:
            ch3_setting(); // FM3chの場合のみ ch3modeの変更処理
            if (r.carry) { // break sms_nms;
                // ch3なら、それ以前のFM3パートでkeyon処理
                if (r.di != pw.part3) { // break sm_exit;

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
            r.al = pw.partWk[r.di].slotmask;
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
                r.ah |= 0b1000_1000;
            }
//sms_n1:
            pw.partWk[r.di].neiromask = r.ah;
            //r.bx = r.stack.pop(); // commands
            if (pw.partWk[r.di].partmask == 0)
                return this::mp1; // パート復活
            return this::fmmnp_1;
        }
//sm_no_change:
        return null;
    }

    private void keyon_sm() {
        if (pw.partWk[r.di].partmask == 0) { // break ksm_ret;
            if ((pw.partWk[r.di].keyoff_flag & 1) == 0) { // keyon中か？ // break ksm_ret; // keyoff中
                keyon();
            }
        }
//ksm_ret:
    }

    //==============================================================================
    // ssg effect
    //==============================================================================
    public Supplier<Object> ssg_efct_set() {
        logger.log(Level.TRACE, "ssg_efct_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.partWk[r.di].partmask != 0)
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

    //==============================================================================
    // fm effect
    //==============================================================================
    public Supplier<Object> fm_efct_set() {
        logger.log(Level.TRACE, "fm_efct_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.partWk[r.di].partmask != 0)
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

    //==============================================================================
    // fadeout
    //==============================================================================
    public Supplier<Object> fade_set() {
        logger.log(Level.TRACE, "fade_set");

        pw.fadeout_flag = 1;
        r.al = (byte) pw.md[r.incSi()].dat;
        //KUMA:fout の処理をここでやってしまう
        pw.fadeout_speed = r.al;
        return null;
    }

    //==============================================================================
    // LFO depth +- set
    //==============================================================================
    public Supplier<Object> mdepth_set() {
        logger.log(Level.TRACE, "mdepth_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].mdspd = r.al;
        pw.partWk[r.di].mdspd2 = r.al;
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].mdepth = r.al;

        return null;
    }

    //3045-3063
    public Supplier<Object> mdepth_count() {
        logger.log(Level.TRACE, "mdepth_count");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al |= r.al;
        if ((r.al & 0x80) == 0) { // break mdc_lfo2;
            if (r.al == 0) { // break mdc_no_deca;
                r.al--; // 255
            }
//mdc_no_deca:
            pw.partWk[r.di].mdc = r.al;
            pw.partWk[r.di].mdc2 = r.al;

            return null;
        }
//mdc_lfo2:
        r.al &= 0x7f;
        if (r.al == 0) { // break mdc_no_decb;
            r.al--; // 255
        }
//mdc_no_decb:
        pw.partWk[r.di]._mdc = r.al;
        pw.partWk[r.di]._mdc2 = r.al;

        return null;
    }

    //3064-3081
    //==============================================================================
    // ポルタメント計算なのね
    //==============================================================================
    public void porta_calc() {
        r.setAx(pw.partWk[r.di].porta_num2);
        pw.partWk[r.di].porta_num += r.getAx();
        if (pw.partWk[r.di].porta_num3 != 0) { // break pc_ret;
            if ((pw.partWk[r.di].porta_num3 & 0x8000) == 0) { // break pc_minus;

                pw.partWk[r.di].porta_num3--;
                pw.partWk[r.di].porta_num++;
            } else {
//pc_minus:
            pw.partWk[r.di].porta_num3++;
            pw.partWk[r.di].porta_num--;
            }
        }
//pc_ret:
    }

    //3082-3151
    //==============================================================================
    // ポルタメント(FM)
    //==============================================================================
    private Supplier<Object> porta() {
        if (pw.partWk[r.di].partmask == 0) { // break porta_notset;

            ChipDatum cd = new ChipDatum(-1, -1, -1);
            cd.additionalData = pw.cmd;
            WriteOPNARegister.accept(cd);

            r.al = (byte) pw.md[r.incSi()].dat;
            lfoinit();
            oshift();
            fnumset();
            r.setAx(pw.partWk[r.di].fnum);
            r.stack.push(r.getAx());
            r.al = pw.partWk[r.di].onkai;
            r.stack.push(r.getAx());
            r.al = (byte) pw.md[r.incSi()].dat;
            oshift();
            fnumset();
            r.setBx(pw.partWk[r.di].fnum); // bx=ポルタメント先のfnum値
            r.setCx(r.stack.pop());
            pw.partWk[r.di].onkai = r.cl;
            r.setCx(r.stack.pop());
            pw.partWk[r.di].fnum = r.getCx(); // cx=ポルタメント元のfnum値
            r.setAx((short) 0);
            r.stack.push(r.getCx());
            r.stack.push(r.getBx());
            r.ch &= 0x38;
            r.bh &= 0x38;
            r.bh -= r.ch; // 先のoctarb - 元のoctarb
            if (r.bh != 0) { // break not_octarb;
                r.bh = (byte) ((r.bh & 0x80) | ((r.bh >> 1) & 0x7f));
                r.bh = (byte) ((r.bh & 0x80) | ((r.bh >> 1) & 0x7f));
                r.bh = (byte) ((r.bh & 0x80) | ((r.bh >> 1) & 0x7f));
                r.al = r.bh;
                r.setAx(r.al); // ax=octarb差
                r.setBx((short) 0x26a);
                int ans = r.getAx() * r.getBx(); // (dx) ax = 26ah* octarb差
                r.setDx((short) (ans >> 16));
                r.setAx((short) ans);
            }
//not_octarb:
            r.setBx(r.stack.pop());
            r.setCx(r.stack.pop());
            r.andCx((short) 0x7ff);
            r.andBx((short) 0x7ff);
            r.subBx(r.getCx());
            r.addAx(r.getBx()); // ax=26ah* octarb差 + 音程差
            r.bl = (byte) pw.md[r.getSi()].dat;
            r.incSi();
            pw.partWk[r.di].leng = r.bl;
            calc_q();
            r.bh = 0;
            int src = r.getAx();
            r.setDx((short) (src % r.getBx())); // ax=(26ah* ovtarb差 + 音程差) / 音長
            r.setAx((short) (src / r.getBx()));
            pw.partWk[r.di].porta_num2 = r.getAx(); // 商
            pw.partWk[r.di].porta_num3 = r.getDx(); // 余り
            pw.partWk[r.di].lfoswi |= 8; // Porta ON
            //r.ax = r.stack.pop(); // commands
            return this::porta_return;
        }
//porta_notset:
        r.al = (byte) pw.md[r.incSi()].dat; // 最初の音程を読み飛ばす(Mask時)
        return null;
    }

    //==============================================================================
    // ポルタメント(PSG)
    //==============================================================================
    private Supplier<Object> portap() {
        if (pw.partWk[r.di].partmask != 0) {
            r.al = (byte) pw.md[r.incSi()].dat;
            return null;
            //return porta_notset;
        }

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = pw.cmd;
        WriteOPNARegister.accept(cd);

        r.al = (byte) pw.md[r.incSi()].dat;
        lfoinitp();
        oshiftp();
        fnumsetp();
        r.setAx(pw.partWk[r.di].fnum);
        r.stack.push(r.getAx());
        r.al = pw.partWk[r.di].onkai;
        r.stack.push(r.getAx());
        r.al = (byte) pw.md[r.incSi()].dat;
        oshiftp();
        fnumsetp();
        r.setAx(pw.partWk[r.di].fnum); // ax = ポルタメント先のpsg_tune値
        r.setBx(r.stack.pop());
        pw.partWk[r.di].onkai = r.bl;
        r.setBx(r.stack.pop()); // bx = ポルタメント元のpsg_tune値
        pw.partWk[r.di].fnum = r.getBx();
        r.subAx(r.getBx()); // ax = psg_tune差
        r.bl = (byte) pw.md[r.getSi()].dat;
        r.incSi();
        pw.partWk[r.di].leng = r.bl;
        calc_q();
        r.bh = 0;
        int src = r.getAx();
        r.setDx((short) (src % r.getBx())); // ax = psg_tune差 / 音長
        r.setAx((short) (src / r.getBx()));
        pw.partWk[r.di].porta_num2 = r.getAx(); // 商
        pw.partWk[r.di].porta_num3 = r.getDx(); // 余り
        pw.partWk[r.di].lfoswi |= 8; // Porta ON
        //r.ax = r.stack.pop(); // commandsp
        return this::porta_returnp;
    }

    //==============================================================================
    // STATUSに値を出力
    //==============================================================================
    public Supplier<Object> status_write() {
        logger.log(Level.TRACE, "status_write");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.status = r.al;
        return null;
    }

    //3205-3214
    //==============================================================================
    // STATUSに値を加算
    //==============================================================================
    public Supplier<Object> status_add() {
        logger.log(Level.TRACE, "status_add");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.setBx((short) 0); // offset status
        r.al += pw.status; // add al,[bx]
        pw.status = r.al; // mov[bx],al
        return null;
    }

    //3215-3256
    //==============================================================================
    // ボリュームを次の一個だけ変更（V2.7拡張分）
    //==============================================================================
    private Supplier<Object> vol_one_up_fm() {
        logger.log(Level.TRACE, "vol_one_up_fm");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.partWk[r.di].volume;
        if (r.al < 128)
            return this::vo_vset;
        r.al = 127;
        return this::vo_vset;
    }

    private Supplier<Object> vo_vset() {
        r.al++;
        pw.partWk[r.di].volpush = r.al;
        pw.volpush_flag = 1;
        return null;
    }

    private Supplier<Object> vol_one_up_psg() {
        logger.log(Level.TRACE, "vol_one_up_psg");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.partWk[r.di].volume;
        if (r.al < 16)
            return this::vo_vset;
        r.al = 15;
        return this::vo_vset;
    }

    public Supplier<Object> vol_one_up_pcm() {
        logger.log(Level.TRACE, "vol_one_up_pcm");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.carry = (r.al + pw.partWk[r.di].volume > 0xff);
        r.al += pw.partWk[r.di].volume;
        if (r.carry) return this::voup_over;
        return this::vmax_check;
    }

    private Supplier<Object> vmax_check() {
        if (r.al < 255)
            return this::vo_vset;
        return this::voup_over;
    }

    private Supplier<Object> voup_over() {
        r.al = (byte) 254;
        return this::vo_vset;
    }

    public Supplier<Object> vol_one_down() {
        logger.log(Level.TRACE, "vol_one_down");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di].volume;
        r.carry = r.al < r.ah;
        r.al -= r.ah;
        if (!r.carry) return this::vmax_check;
        r.al = 0;
        return this::vo_vset;
    }

    //3257-3300
    //==============================================================================
    // FM音源ハードLFOの設定（Ｖ２．４拡張分）
    //==============================================================================
    private Supplier<Object> hlfo_set() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.board2 != 0) {
            r.ah = r.al;
            r.al = pw.partWk[r.di].fmpan;
            r.al &= (byte) 0b1100_0000;
            r.al |= r.ah;
            pw.partWk[r.di].fmpan = r.al;
            if (pw.partb == 3) { // break hlfoset_notfm3;
                if (pw.fmsel == 0) { // break hlfoset_notfm3;
                    //2608の時のみなので part_eはありえない
                    // FM3の場合は 4つのパート総て設定
                    r.stack.push(r.di);
                    r.di = (short) pw.part3; // offset part3
                    pw.partWk[r.di].fmpan = r.al;
                    r.di = (short) pw.part3b; // offset part3b
                    pw.partWk[r.di].fmpan = r.al;
                    r.di = (short) pw.part3c; // offset part3c
                    pw.partWk[r.di].fmpan = r.al;
                    r.di = (short) pw.part3d; // offset part3d
                    pw.partWk[r.di].fmpan = r.al;
                    r.di = r.stack.pop();
                }
            }
//hlfoset_notfm3:
            if (pw.partWk[r.di].partmask == 0) { // パートマスクされているか？ // break hlfo_exit;
                r.dh = pw.partb;
                r.dh += (byte) (0xb4 - 1);
                calc_panout();
                opnset();
            }
        }
//hlfo_exit:
        return null;
    }

    //3301-3314
    //==============================================================================
    // FM音源ハードLFOのスイッチ（Ｖ２．４拡張分）
    //==============================================================================
    private Supplier<Object> hlfo_onoff() {
        r.al = (byte) pw.md[r.incSi()].dat;
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

    //3315-3324
    //==============================================================================
    // FM音源ハードLFOのディレイ設定
    //==============================================================================
    private Supplier<Object> hlfo_delay() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.board2 != 0) {
            pw.partWk[r.di].hldelay = r.al;
        }
        return null;
    }

    //3325-3332
    //==============================================================================
    // COMMAND 'Z' （小節の長さの変更）
    //==============================================================================
    public Supplier<Object> syousetu_lng_set() {
        logger.log(Level.TRACE, "syousetu_lng_set");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.syousetu_lng = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Tempo, null, (int) pw.tempo_d, (int) pw.syousetu_lng);
        cd.additionalData = md;
        WriteOPNARegister.accept(cd);

        return null;
    }

    //3333-3377
    //==============================================================================
    // COMMAND '@' [PROGRAM CHANGE]
    //==============================================================================
    private Supplier<Object> comAt() {
        logger.log(Level.TRACE, "com@");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].voicenum = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Instrument, pw.cmd.linePos
                , 0xff
                , (int) pw.partWk[r.di].voicenum
        );
        WriteDummy(cd);

        r.dl = r.al;
        if (pw.partWk[r.di].partmask == 0) { // パートマスクされているか？ // break comAt_mask;

            neiroset();
            return null;
        }
//comAt_mask:
        toneadr_calc();

        r.dl = (byte) pw.inst[r.getBx() + 24].dat; //    mov dl,24[bx]
        pw.partWk[r.di].alg_fb = r.dl; // alg/fb設定
        r.andBx((short) 4);

        neiroset_tl(); // tl設定(NO break dl)

comAt_afset:
        {
            // FM3chで、マスクされていた場合、fm3_alg_fbを設定
            if (pw.partb == 3) { // break comAt_exit;

                if (pw.partWk[r.di].neiromask != 0) { // break comAt_exit;

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
        if ((pw.partWk[r.di].slotmask & 0x10) == 0) { // slot1を使用していなければ // break comAt_notslot1;

            r.al = pw.fm3_alg_fb;
            r.al &= 0b0011_1000; // fbは前の値を使用
            r.dl &= 0b0000_0111;
            r.dl |= r.al;
        }
//comAt_notslot1:
        pw.fm3_alg_fb = r.dl;
        pw.partWk[r.di].alg_fb = r.al;
        return null;
    }

    //3378-3394
    //==============================================================================
    // COMMAND 'q' [STEP-GATE CHANGE]
    //==============================================================================
    public Supplier<Object> comq() {
        logger.log(Level.TRACE, "comq");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].qdata = r.al;
        pw.partWk[r.di].qdat3 = 0;
        comq_dmy();

        return null;
    }

    public Supplier<Object> comq3() {
        logger.log(Level.TRACE, "comq3");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].qdat2 = r.al;
        comq_dmy();

        return null;
    }

    public Supplier<Object> comq4() {
        logger.log(Level.TRACE, "comq4");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].qdat3 = r.al;
        comq_dmy();

        return null;
    }

    //==============================================================================
    // COMMAND 'Q' [STEP-GATE CHANGE 2]
    //==============================================================================
    public Supplier<Object> comq2() {
        logger.log(Level.TRACE, "comq2");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].qdatb = r.al;
        comq_dmy();

        return null;
    }

    private void comq_dmy() {
        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Gatetime, pw.cmd.linePos
                , (int) pw.partWk[r.di].qdatb // Q%
                , (int) pw.partWk[r.di].qdata // q [X] -x  ,  x    :数値1
                , (int) pw.partWk[r.di].qdat2 // q  x  -x  , [X]   :数値3
                , (int) pw.partWk[r.di].qdat3 // q  x [-X] ,  x    :数値2
        );
        cd.additionalData = md;
        WriteDummy(cd);
    }

    //==============================================================================
    // COMMAND 'V' [VOLUME CHANGE]
    //==============================================================================
    public Supplier<Object> comv() {
        logger.log(Level.TRACE, "comv");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].volume = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, (int) r.al);
        cd.additionalData = md;
        WriteDummy(cd);

        return null;
    }

    //==============================================================================
    // COMMAND 't' [TEMPO CHANGE1]
    // COMMAND 'T' [TEMPO CHANGE2]
    // COMMAND 't±' [TEMPO CHANGE 相対1]
    // COMMAND 'T±' [TEMPO CHANGE 相対2]
    //==============================================================================
    public Supplier<Object> comt() {
        logger.log(Level.TRACE, "comt");

        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al < (byte) 251) { // break comt_sp0;
//comt_exit1:
            pw.tempo_d = r.al; // T(FC)
            pw.tempo_d_push = r.al;
            return this::calc_tb_tempo;
        }
//comt_sp0:
        r.al++;
        if (r.al == 0) { // break comt_sp1;

            r.al = (byte) pw.md[r.incSi()].dat; //t(FC FF)

//comt_exit2c:
            if (r.al < 18) { // break comt_exit2;
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
        r.al = (byte) pw.md[r.incSi()].dat;
        if (zero) { // break comt_sp2;

            r.ah = pw.tempo_d_push; // T± (FC FE)
            if ((r.al & 0x80) == 0) { // break comt_sp1_minus;
                r.carry = (r.al + r.ah) > 0xff;
                r.al += r.ah;
                if (r.carry) { // break comt_sp1_exitc;
                    r.al = (byte) 250;
//                    break comt_exit1;
                    pw.tempo_d = r.al; // T(FC) // <<
                    pw.tempo_d_push = r.al; // <<
                    return this::calc_tb_tempo; // <<
                }
            } else {
//comt_sp1_minus:
                r.carry = (r.al + r.ah) > 0xff;
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
            pw.tempo_d_push = r.al; // <<
            return this::calc_tb_tempo; // <<
        }
//comt_sp2:
        r.ah = pw.tempo_48_push; // t± (FC FD)
        if ((r.al & 0x80) == 0) { // break comt_sp2_minus;
            r.carry = (r.al + r.ah) > 0xff;
            r.al += r.ah;
            if (r.carry) { // break comt_exit2;
                r.al = (byte) 255;
            }
//            break comt_exit2;
        } else {
//comt_sp2_minus:
            r.carry = (r.al + r.ah) > 0xff;
            r.al += r.ah;
            if (!r.carry) {
//                break comt_2c_over;
//comt_2c_over: // <<
                r.al = 18; // <<
//comt_exit2: // <<
                pw.tempo_48 = r.al; // <<
                pw.tempo_48_push = r.al; // <<
                return this::calc_tempo_tb; // <<
            }
        }
//        break comt_exit2c;
//comt_exit2c: // <<
        if (r.al < 18) { // break comt_exit2; // <<
//comt_2c_over: // <<
            r.al = 18; // <<
        } // <<
//comt_exit2: // <<
        pw.tempo_48 = r.al; // <<
        pw.tempo_48_push = r.al; // <<
        return this::calc_tempo_tb; // <<
    }

    //3475-3496
    //==============================================================================
    // T->t 変換
    // input[tempo_d]
    //  output[tempo_48]
    //==============================================================================
    private Supplier<Object> calc_tb_tempo() {
        // TEMPO = 112CH / [ 256 - TB] timerB -> tempo
        r.bl = 0;
        r.bl -= pw.tempo_d; // tempo_d レジスタ地
        r.al = (byte) 255;

        if (r.bl >= 18) { // break ctbt_exit;

            r.setAx((short) 0x112c);
            r.ah = (byte) (0x112c % r.bl);
            r.al = (byte) (0x112c / r.bl);
            if ((r.ah & 0x80) != 0) { // break ctbt_exit;
                r.al++; // 四捨五入
            }
        }
//ctbt_exit:
        pw.tempo_48 = r.al;
        pw.tempo_48_push = r.al;

        return null;
    }

    //3497-3520
    //==============================================================================
    // t->T 変換
    // input[tempo_48]
    //  output[tempo_d]
    //==============================================================================
    private Supplier<Object> calc_tempo_tb() {
        // TB = 256 - [ 112CH / TEMPO] tempo -> timerB
        r.bl = pw.tempo_48;
        r.al = 0;

        if (r.bl >= 18) { // break cttb_exit;

            r.setAx((short) 0x112c);

            r.al = (byte) (0x112c / r.bl);
            r.ah = (byte) (0x112c % r.bl);

            r.dl = 0;
            r.dl -= r.al;
            r.al = r.dl;
            if ((r.ah & 0x80) != 0) { // break cttb_exit;
                r.al--; // 四捨五入
            }
        }
//cttb_exit:
        pw.tempo_d = r.al;
        pw.tempo_d_push = r.al;

        return null;
    }

    //==============================================================================
    // COMMAND '&' [タイ]
    //==============================================================================
    public Supplier<Object> comtie() {
        logger.log(Level.TRACE, "comtie");

        pw.tieflag |= 1;
        return null;
    }

    //==============================================================================
    // COMMAND 'D' [デチューン]
    //==============================================================================
    public Supplier<Object> comd() {
        logger.log(Level.TRACE, "comd");

        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        pw.partWk[r.di].detune = r.getAx();

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Detune, pw.cmd.linePos
                , (int) pw.partWk[r.di].detune
        );
        WriteDummy(cd);

        return null;
    }

    //3535-3541
    //==============================================================================
    // COMMAND 'DD' [相対デチューン]
    //==============================================================================
    public Supplier<Object> comdd() {
//#if DEBUG
//            logger.log(Level.TRACE, "comdd");
//#endif

        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        pw.partWk[r.di].detune += r.getAx();

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Detune, pw.cmd.linePos
                , (int) pw.partWk[r.di].detune
        );
        WriteDummy(cd);

        return null;
    }

    //3542-3557
    //==============================================================================
    // COMMAND '[' [ループ スタート]
    //==============================================================================
    public Supplier<Object> comstloop() {
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        r.setBx(r.getAx());
        r.setAx((short) pw.mmlbuf);
        if (r.di == pw.part_e) { // break comst_nonefc;
            r.setAx((short) pw.efcdat);
        }
//comst_nonefc:
        r.addBx(r.getAx());
        r.incBx();
        pw.md[r.getBx()].dat = 0;

        return null;
    }

    //3558-3586
    //==============================================================================
    // COMMAND ']' [ループ エンド]
    //==============================================================================
    public Supplier<Object> comedloop() {
reloop: // ↑
        {
            r.al = (byte) (pw.md[r.incSi()].dat);
            if (r.al != 0) { // break muloop; // 0 ナラ ムジョウケン ループ
                r.ah = r.al;
                pw.md[r.getSi()].dat++;

                r.al = (byte) (pw.md[r.incSi()].dat);
                if (r.ah != r.al)
                    break reloop;
                r.addSi((short) 2);
                return null;
            }
//muloop:
            r.incSi();
            pw.partWk[r.di].loopcheck = 1;
        }
//reloop:
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        r.addAx((short) 2);

        r.setBx((short) pw.mmlbuf);
        if (r.di == pw.part_e) { // break comed_nonefc;
            r.setBx((short) pw.efcdat);
        }
//comed_nonefc:
        r.addAx(r.getBx());

        r.setSi(r.getAx());

        return null;
    }

    //3587-3609
    //==============================================================================
    // COMMAND ':' [ループ ダッシュツ]
    //==============================================================================
    public Supplier<Object> comexloop() {
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);
        r.setBx(r.getAx());

        r.setAx((short) pw.mmlbuf);
        if (r.di == pw.part_e) { // break comex_nonefc;
            r.setAx((short) pw.efcdat);
        }
//comex_nonefc:
        r.addBx(r.getAx());

        r.dl = (byte) pw.md[r.getBx()].dat;
        r.dl--;
        r.incBx();
        if (r.dl != pw.md[r.getBx()].dat) { // break loopexit;
            return null;
        }
//loopexit:
        r.addBx((short) 3);
        r.setSi(r.getBx());
        return null;
    }

    //3610-3616
    //==============================================================================
    // COMMAND 'L' [クリカエシ ループ セット]
    //==============================================================================
    public Supplier<Object> comlopset() {
        logger.log(Level.TRACE, "comlopset");

        pw.partWk[r.di].partloop = r.getSi();

        return null;
    }

    //3617-3624
    //==============================================================================
    // COMMAND '_' [オンカイ シフト]
    //==============================================================================
    public Supplier<Object> comshift() {
        logger.log(Level.TRACE, "comshift");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].shift = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.KeyShift, pw.cmd.linePos
                , (int) pw.partWk[r.di].shift
        );
        WriteDummy(cd);

        return null;
    }

    //3625-3633
    //==============================================================================
    // COMMAND '__' [相対転調]
    //==============================================================================
    public Supplier<Object> comshift2() {
        logger.log(Level.TRACE, "comshift2");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.partWk[r.di].shift;
        pw.partWk[r.di].shift = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.KeyShift, pw.cmd.linePos
                , (int) pw.partWk[r.di].shift
        );
        WriteDummy(cd);

        return null;
    }

    //3634-3641
    //==============================================================================
    // COMMAND '_M' [Master転調値]
    //==============================================================================
    public Supplier<Object> comshift_master() {
        logger.log(Level.TRACE, "comshift_master");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].shift_def = r.al;
        return null;
    }

    //3642-3654
    //==============================================================================
    // COMMAND ')' [VOLUME UP]
    //==============================================================================
    // ; ＦＯＲ FM
    private Supplier<Object> comvolup() {
        logger.log(Level.TRACE, "comvolup");

        r.al = pw.partWk[r.di].volume;
        r.al += 4;
        return this::volupck;
    }

    private Supplier<Object> volupck() {
        logger.log(Level.TRACE, "volupck");

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.min(r.al, 127));
        cd.additionalData = md;
        WriteDummy(cd);

        if (r.al < 128)
            return this::vset;
        r.al = 127;
        return this::vset;
    }

    private Supplier<Object> vset() {
        logger.log(Level.TRACE, "vset");

        pw.partWk[r.di].volume = r.al;
        return null;
    }

    //3656-3661
    // 数字付き
    private Supplier<Object> comvolup2() {
        logger.log(Level.TRACE, "comvolup2");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.partWk[r.di].volume;
        return this::volupck;
    }

    //3662-3671
    // for PSG
    private Supplier<Object> comvolupp() {
        logger.log(Level.TRACE, "comvolupp");

        r.al = pw.partWk[r.di].volume;
        r.al++;
        return this::volupckp;
    }

    private Supplier<Object> volupckp() {
        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.min(r.al, 15));
        cd.additionalData = md;
        WriteDummy(cd);

        if (r.al < 16)
            return this::vset;
        r.al = 15;
        return this::vset;
    }

    //    ; 数字付き
    private Supplier<Object> comvolupp2() {
        logger.log(Level.TRACE, "comvolupp2");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.partWk[r.di].volume;
        return this::volupckp;
    }

    //3678-3716
    //==============================================================================
    // COMMAND '(' [VOLUME DOWN]
    //==============================================================================
    // ; for FM
    private Supplier<Object> comvoldown() {
        logger.log(Level.TRACE, "comvoldown");

        r.al = pw.partWk[r.di].volume;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.max(r.al - 4, 0));
        cd.additionalData = md;
        WriteDummy(cd);

        r.carry = (r.al < 4);
        r.al -= 4;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    //    ; 数字付き
    private Supplier<Object> comvoldown2() {
        logger.log(Level.TRACE, "comvoldown2");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di].volume;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.max(r.al - r.ah, 0));
        cd.additionalData = md;
        WriteDummy(cd);

        r.carry = (r.al < r.ah);
        r.al -= r.ah;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    // ; for PSG
    private Supplier<Object> comvoldownp() {
        logger.log(Level.TRACE, "comvoldownp");

        r.al = pw.partWk[r.di].volume;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.max(r.al - 1, 0));
        cd.additionalData = md;
        WriteDummy(cd);

        if (r.al == 0) return this::vset;
        r.al--;
        return this::vset;
    }

    //    ; 数字付き
    private Supplier<Object> comvoldownp2() {
        logger.log(Level.TRACE, "comvoldownp2");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di].volume;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, Math.max(r.al - r.ah, 0));
        cd.additionalData = md;
        WriteDummy(cd);

        r.carry = (r.al < r.ah);
        r.al -= r.ah;
        if (!r.carry) return this::vset;
        r.al = 0;
        return this::vset;
    }

    //3717-3721
    //==============================================================================
    // LFO２用処理
    //==============================================================================
    public Supplier<Object> _lfoset() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfoset);
    }

    //3722-3732
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

    //3733-3736
    public Supplier<Object> _mdepth_set() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::mdepth_set);
    }

    //3737-3740
    public Supplier<Object> _lfowave_set() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfowave_set);
    }

    //3741-3744
    public Supplier<Object> _lfo_extend() {
//#if DEBUG
        logger.log(Level.TRACE, "_lfo_extend");
//#endif

        r.setAx((short) 0); // offset lfo_extend
        return _lfo_main(this::lfo_extend);
    }

    //3745-3748
    public Supplier<Object> _lfoset_delay() {
        r.setAx((short) 0); // offset lfoset
        return _lfo_main(this::lfoset_delay);
    }

    //3749-3761
    public Supplier<Object> _lfoswitch() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.al &= 7;
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        r.al = r.rol(r.al, 1);
        pw.partWk[r.di].lfoswi &= 0x8f;
        pw.partWk[r.di].lfoswi |= r.al;
        lfo_change();
        lfoinit_main();
        lfo_change();
        return null;
    }

    //3762-3765
    private Supplier<Object> _lfoswitch_f() {
        _lfoswitch();
        return this::ch3_setting;
    }

    //3766-3809
    //==============================================================================
    // LFO1<->LFO2 change
    //==============================================================================
    public void lfo_change() {
        r.setAx(pw.partWk[r.di].lfodat);
        pw.partWk[r.di].lfodat = pw.partWk[r.di]._lfodat;
        pw.partWk[r.di]._lfodat = r.getAx();

        r.cl = 4;
        pw.partWk[r.di].lfoswi = (byte) ((pw.partWk[r.di].lfoswi << 4) | ((pw.partWk[r.di].lfoswi & 0xf0) >> 4));
        pw.partWk[r.di].extendmode = (byte) ((pw.partWk[r.di].extendmode << 4) | ((pw.partWk[r.di].extendmode & 0xf0) >> 4));

        r.al = pw.partWk[r.di].delay;
        pw.partWk[r.di].delay = pw.partWk[r.di]._delay;
        pw.partWk[r.di]._delay = r.al;

        r.al = pw.partWk[r.di].speed;
        pw.partWk[r.di].speed = pw.partWk[r.di]._speed;
        pw.partWk[r.di]._speed = r.al;

        r.al = pw.partWk[r.di].step;
        pw.partWk[r.di].step = pw.partWk[r.di]._step;
        pw.partWk[r.di]._step = r.al;

        r.al = pw.partWk[r.di].time;
        pw.partWk[r.di].time = pw.partWk[r.di]._time;
        pw.partWk[r.di]._time = r.al;

        r.al = pw.partWk[r.di].delay2;
        pw.partWk[r.di].delay2 = pw.partWk[r.di]._delay2;
        pw.partWk[r.di]._delay2 = r.al;

        r.al = pw.partWk[r.di].speed2;
        pw.partWk[r.di].speed2 = pw.partWk[r.di]._speed2;
        pw.partWk[r.di]._speed2 = r.al;

        r.al = pw.partWk[r.di].step2;
        pw.partWk[r.di].step2 = pw.partWk[r.di]._step2;
        pw.partWk[r.di]._step2 = r.al;

        r.al = pw.partWk[r.di].time2;
        pw.partWk[r.di].time2 = pw.partWk[r.di]._time2;
        pw.partWk[r.di]._time2 = r.al;

        r.al = pw.partWk[r.di].mdepth;
        pw.partWk[r.di].mdepth = pw.partWk[r.di]._mdepth;
        pw.partWk[r.di]._mdepth = r.al;

        r.al = pw.partWk[r.di].mdspd;
        pw.partWk[r.di].mdspd = pw.partWk[r.di]._mdspd;
        pw.partWk[r.di]._mdspd = r.al;

        r.al = pw.partWk[r.di].mdspd2;
        pw.partWk[r.di].mdspd2 = pw.partWk[r.di]._mdspd2;
        pw.partWk[r.di]._mdspd2 = r.al;
        r.ah = pw.partWk[r.di].lfo_wave;
        pw.partWk[r.di].lfo_wave = pw.partWk[r.di]._lfo_wave;
        pw.partWk[r.di]._lfo_wave = r.ah;

        r.al = pw.partWk[r.di].mdc;
        pw.partWk[r.di].mdc = pw.partWk[r.di]._mdc;
        pw.partWk[r.di]._mdc = r.al;
        r.al = pw.partWk[r.di].mdc2;
        pw.partWk[r.di].mdc2 = pw.partWk[r.di]._mdc2;
        pw.partWk[r.di]._mdc2 = r.al;

        return;
    }

    //3810-3826
    //==============================================================================
    // LFO パラメータ セット
    //==============================================================================
    public Supplier<Object> lfoset() {
        logger.log(Level.TRACE, "lfoset");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].delay = r.al;
        pw.partWk[r.di].delay2 = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].speed = r.al;
        pw.partWk[r.di].speed2 = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].step = r.al;
        pw.partWk[r.di].step2 = r.al;

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].time = r.al;
        pw.partWk[r.di].time2 = r.al;

        return this::lfoinit_main;
    }

    public Supplier<Object> lfoset_delay() {
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].delay = r.al;
        pw.partWk[r.di].delay2 = r.al;

        return this::lfoinit_main;
    }

    //==============================================================================
    // LFO SWITCH
    //==============================================================================
    public Supplier<Object> lfoswitch() {
        logger.log(Level.TRACE, "lfoswitch");

        r.al = (byte) pw.md[r.incSi()].dat;
        if ((r.al & 0b1111_1000) != 0) { // break ls_00;
            r.al = 1;
        }
//ls_00:
        r.al &= 7;
        pw.partWk[r.di].lfoswi &= 0xf8;
        pw.partWk[r.di].lfoswi |= r.al;
        return this::lfoinit_main;
    }

    private Supplier<Object> lfoswitch_f() {
        logger.log(Level.TRACE, "lfoswitch_f");

        Object o = lfoswitch();
        while (o != null) o = ((Supplier<Object>) o).get();
        return this::ch3_setting;
    }

    //==============================================================================
    // PSG ENVELOPE SET
    //==============================================================================
    public Supplier<Object> psgenvset() {
        logger.log(Level.TRACE, "psgenvset");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].eenv_ar = r.al; // pat
        pw.partWk[r.di].eenv_arc = r.al; // patb
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].eenv_dr = r.al; // pv2
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].eenv_sr = r.al; // pr1
        pw.partWk[r.di].eenv_src = r.al; // pr1b
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].eenv_rr = r.al; // pr2
        pw.partWk[r.di].eenv_rrc = r.al; // pr2b

        if (pw.partWk[r.di].envf == (byte) 0xff) { // break not_set_count2; // 拡張＞ノーマルに移行したか？

            pw.partWk[r.di].envf = 2; // RR
            pw.partWk[r.di].eenv_volume = (byte) 0xf1; // -15; // Volume // .penv
        }
//not_set_count2:
        return null;
    }

    //==============================================================================
    // 'y' COMMAND[コイツガ イチバン カンタン]
    //==============================================================================
    public Supplier<Object> comy() {
        logger.log(Level.TRACE, "comy");

        r.dh = (byte) pw.md[r.incSi()].dat;
        r.dl = (byte) pw.md[r.incSi()].dat;
        opnset();
        return null;
    }

    //==============================================================================
    // 'w' COMMAND[PSG NOISE ヘイキン シュウハスウ]
    //==============================================================================
    private Supplier<Object> psgnoise() {
        logger.log(Level.TRACE, "psgnoise");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.psnoi = r.al;
        return null;
    }

    private Supplier<Object> psgnoise_move() {
        logger.log(Level.TRACE, "psgnoise_move");

        r.al = (byte) pw.md[r.incSi()].dat;
        r.al += pw.psnoi;
        if ((r.al & 0x80) != 0) { // break pnm_nminus;
            r.al = 0;
        }
//pnm_nminus:
        if (r.al >= 32) { // break pnm_set;
            r.al = 31;
        }
//pnm_set:
        pw.psnoi = r.al;
        return null;
    }

    //3903-3910
    //==============================================================================
    // 'P' COMMAND[PSG TONE / NOISE / MIX SET]
    //==============================================================================
    private Supplier<Object> psgsel() {
        logger.log(Level.TRACE, "psgsel");

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].psgpat = r.al;
        return null;
    }

    //3911-3956
    //==============================================================================
    // 'p' COMMAND[FM PANNING SET]
    //==============================================================================
    private Supplier<Object> panset() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (pw.board2 != 0) {
            return panset_main();
        }
        return null;
    }

    private Supplier<Object> panset_main() {
        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Pan, pw.cmd.linePos, (int) r.al);
        WriteDummy(cd);

        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al &= 0b1100_0000;
        r.ah = r.al; // ah<- pan data
        r.al = pw.partWk[r.di].fmpan;
        r.al &= 0b0011_1111;
        r.al |= r.ah;
        pw.partWk[r.di].fmpan = r.al;
        if (pw.partb == 3) { // break panset_notfm3;
            if (pw.fmsel == 0) { // break panset_notfm3;
                // FM3の場合は 4つのパート総て設定
                r.stack.push(r.di);
                r.di = (short) pw.part3;
                pw.partWk[r.di].fmpan = r.al;
                r.di = (short) pw.part3b;
                pw.partWk[r.di].fmpan = r.al;
                r.di = (short) pw.part3c;
                pw.partWk[r.di].fmpan = r.al;
                r.di = (short) pw.part3d;
                pw.partWk[r.di].fmpan = r.al;
                r.di = r.stack.pop();
            }
        }
//panset_notfm3:
        if (pw.partWk[r.di].partmask == 0) { // パートマスクされているか？ break panset_exit;
            r.dl = r.al;
            r.dh = pw.partb;
            r.dh += 0xb4 - 1;
            calc_panout();
            opnset();
        }
//panset_exit:
        return null;
    }

    //==============================================================================
    // 0b4h～に設定するデータを取得 out.dl
    //==============================================================================
    private void calc_panout() {
        if (pw.board2 != 0) {
            r.dl = pw.partWk[r.di].fmpan;
            if (pw.partWk[r.di].hldelay_c != 0) { // break cpo_ret;
                r.dl &= 0xc0; // HLFO Delayが残ってる場合はパンのみ設定
            }
//cpo_ret:
        }
    }

    //==============================================================================
    // Pan setting Extend
    //==============================================================================
    private Supplier<Object> panset_ex() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.incSi(); // 逆走flagは読み飛ばす
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

    //3991-4060
    //==============================================================================
    // "\?" COMMAND[OPNA Rhythm Keyon / Dump]
    //==============================================================================
    public Supplier<Object> rhykey() {
        if (pw.board2 != 0) {
            r.dh = 0x10;
            r.al = (byte) pw.md[r.incSi()].dat;
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
                    //rklp:;
                    do {
                        r.al = r.ror(r.al, 1);
                        if (r.carry) { // break rk00;
                            r.dl = pw.rdat[r.getBx()];
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
                    r.addBx((short) 0); // offset rshot_bd
                    rflag_inc(true);
                    pw.rshot_dat |= r.dl;
                }
//rhst_ret2:
                if (pw.md[r.getSi()].dat == 0xeb) { // break rhst_ret;
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
        //ri_loop:;
        do {
            r.al = r.ror(r.al, 1);
            if (r.carry) { // break ri_not;
                if (isShot) pw.rshot[r.getBx()]++;
                else pw.rdump[r.getBx()]++;
            }
//ri_not:
            r.incBx();
            r.decCx();
        } while (r.getCx() != 0);
    }

    //4061-4132
    //==============================================================================
    // "\v?n" COMMAND
    //==============================================================================
    public Supplier<Object> rhyvs() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi()].dat;
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
        r.al = pw.rdat[r.getBx()];
        r.al &= r.cl;
        r.dl |= r.al;
        pw.rdat[r.getBx()] = r.dl;
        opnset44();
        return null;
    }

    public Supplier<Object> rhyvs_sft() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi()].dat;
            r.setBx((short) 0xffff); // offset rdat-1
            r.dh = r.al;
            r.ah = 0;
            r.addBx(r.getAx());

            r.dh += 0x18 - 1;
            r.al = pw.rdat[r.getBx()];
            r.al &= 0b0001_1111;
            r.dl = r.al;
            r.al = (byte) pw.md[r.incSi()].dat;
            r.al += r.dl;
            if (r.al >= 32) { // break rvss00;
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
            r.al = pw.rdat[r.getBx()];
            r.al &= 0b1110_0000;
            r.dl |= r.al;
            pw.rdat[r.getBx()] = r.dl;
            opnset44();
            return null;
        } else {
            r.addSi((short) 2);
            return null;
        }
    }

    //==============================================================================
    // "\p?" COMMAND
    //==============================================================================
    public Supplier<Object> rpnset() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi()].dat;
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

    //==============================================================================
    // "\Vn" COMMAND
    //==============================================================================
    public Supplier<Object> rmsvs() {
        if (pw.board2 != 0) {
            r.al = (byte) pw.md[r.incSi()].dat;
            r.dl = r.al;
            r.al = pw.rhythm_voldown;
            if (r.al != 0) {
                r.al = (byte) -r.al;
                r.setAx((short) (r.al * r.dl));
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

    //4169-4181
    private void volset2rf() {
        if (pw.board2 != 0) {
            r.dh = 0x11;
            r.al = pw.fadeout_volume;
            if (r.al != 0) { // break vs2r_000;
                r.al = (byte) ~r.al;
                int ans = r.al * r.dl;
                r.dl = (byte) (ans >> 8);
            }
//vs2r_000:
            opnset44();
        }
    }


    //4184-4204
    public Supplier<Object> rmsvs_sft() {
        if (pw.board2 != 0) {
            r.dh = 0x11;
            r.al = (byte) pw.md[r.incSi()].dat;
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

    //==============================================================================
    // SHIFT[di] 分移調する
    //==============================================================================
    public void oshift() {
        //oshiftp:
        if (r.al == 0xf) // 休符
            return;
        r.dl = pw.partWk[r.di].shift;
        r.dl += pw.partWk[r.di].shift_def;
        if ((r.dl & r.dl) == 0)
            return;

        r.bl = r.al;
        r.bl &= 0xf;
        r.al &= 0xf0;
        r.al >>= 4; // KUMA:ホントはror x4
        r.bh = r.al; // bh=OCT bl = ONKAI

        if ((r.dl & 0x80) != 0) { // break shiftplus;

            //
            // - ホウコウ シフト
            //
            //shiftminus:
            r.carry = false;
            if (r.bl + r.dl > 0xff) r.carry = true;
            r.bl += r.dl;
            if (!r.carry) { // break sfm2;

                //sfm1:
                do {
                    r.bh--;
                    r.carry = false;
                    if (r.bl + 12 > 0xff) r.carry = true;
                    r.bl += 12;
                } while (!r.carry);
            }
//sfm2:
            r.al = r.bh;
            r.al = (byte) ((r.al >> 4) | (byte) (r.al << 4)); // ror x4
            r.al |= r.bl;
            return;
        }
        //
        // + ホウコウ シフト
        //
//shiftplus:
        r.bl += r.dl;
        //spm1:;
        do {
            if (r.bl < 0xc)
                break; // spm2;
            r.bh++;
            r.bl -= 12;
        } while (true);
//spm2:
        r.al = r.bh;
        r.al = (byte) ((r.al >> 4) | (byte) (r.al << 4)); // ror x4
        r.al |= r.bl;

        //osret: ret
    }

    private void oshiftp() {
        oshift();
    }

    //4262-4331
    //==============================================================================
    // FM BLOCK, F-NUMBER SET
    // ; INPUTS -- AL[KEY#,0-7F]
    //==============================================================================
    private void fnumset() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrest(); // 休符の場合
            return;
        }
        pw.partWk[r.di].onkai = r.al;

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
        r.setAx((short) pw.fnum_data[r.getBx()]);

        //
        // BLOCK SET
        //
        r.ah |= r.ch;
        pw.partWk[r.di].fnum = r.getAx();
        return;
    }

    public void fnrest() {
        pw.partWk[r.di].onkai = (byte) 0xff;
        if ((pw.partWk[r.di].lfoswi & 0x11) == 0) { // break fnr_ret;
            pw.partWk[r.di].fnum = 0; // 音程LFO未使用
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
            fnrest(); // キュウフ ナラ FNUM ニ 0 ヲ セット
            return;
        }
        pw.partWk[r.di].onkai = r.al;

        r.cl = r.al;
        r.cl = (byte) ((r.cl >> 4) | (byte) (r.cl << 4)); // ror x4
        r.cl &= 0xf; // cl=oct
        r.bl = r.al;
        r.bl &= 0xf;
        r.bh = 0; // bx=onkai
        //r.bx += r.bx;
        r.setAx((short) pw.psg_tune_data[r.getBx()]);

        r.carry = r.cl == 0 ? false : ((r.getAx() & (1 << (r.cl - 1))) != 0);
        r.setAx((short) (r.getAx() >> r.cl)); //    shr ax,cl

        if (r.carry) { // break pt_non_inc;
            r.incAx();
        }
//pt_non_inc:
        pw.partWk[r.di].fnum = r.getAx();
    }

    //==============================================================================
    // Set[FNUM / BLOCK + DETUNE + LFO]
    //==============================================================================
    private void otodasi() {
        r.setAx(pw.partWk[r.di].fnum);
        if (r.getAx() == 0) { // break od_00;
            return;
        }
//od_00:
od_non_ch3: // ↑
        if (pw.partWk[r.di].slotmask != 0) { // break od_exit;
            r.setCx(r.getAx());
            r.andCx((short) 0x3800); // cx=BLOCK
            r.ah &= 7; // ax=FNUM
            //
            // Portament/LFO/Detune SET
            //
            r.addAx(pw.partWk[r.di].porta_num);
            r.addAx(pw.partWk[r.di].detune);
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
            if ((pw.partWk[r.di].lfoswi & 1) != 0) { // break od_not_lfo1;
                r.addAx(pw.partWk[r.di].lfodat);
            }
//od_not_lfo1:
            if ((pw.partWk[r.di].lfoswi & 0x10) != 0) { // break od_not_lfo2;
                r.addAx(pw.partWk[r.di]._lfodat);
            }
//od_not_lfo2:
            fm_block_calc();

//#if DEBUG
            //logger.log(Level.TRACE, String.format("cx:{0:X4}  ax:{1:X4}  lfodat:%d  step:%d"
            //    , r.cx, r.ax, (short)pw.partWk[r.di].lfodat, (sbyte)pw.partWk[r.di].step));
//#endif

            //
            // SET BLOCK/FNUM TO OPN
            // input CX:AX
            r.orAx(r.getCx()); // AX=block/Fnum
            r.dh += 0xa4 - 1;
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

    //==============================================================================
    // ch3=効果音モード を使用する場合の音程設定
    // input CX:block AX:fnum
    //==============================================================================
    private void ch3_special() {
        r.stack.push(r.getSi());
        r.setSi(r.getCx()); // si=block
        r.bl = pw.partWk[r.di].slotmask; // bl=slot mask 4321xxxx
        r.cl = pw.partWk[r.di].lfoswi; // cl=lfoswitch
        r.bh = pw.partWk[r.di].volmask; // bh=lfo1 mask 4321xxxx
        if ((r.bh & 0xf) == 0) { // break c3s_00;
            r.bh = (byte) 0xf0; // all
        }
//c3s_00:
        r.ch = pw.partWk[r.di]._volmask; // ch=lfo2 mask 4321xxxx
        if ((r.ch & 0xf) == 0) { // break ns_sl4;
            r.ch = (byte) 0xf0; // all
        }

        // slot 4
//ns_sl4:
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) ((r.bl << 1) | (r.bl >> 7));
        if (r.carry) { // break ns_sl3;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune4);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) ((r.bh << 1) | (r.bh >> 7));
            if (r.carry) { // break ns_sl4b;
                if ((r.cl & 1) != 0) { // break ns_sl4b;
                    r.addAx(pw.partWk[r.di].lfodat);
                }
            }
//ns_sl4b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) ((r.ch << 1) | (r.ch >> 7));
            if (r.carry) { // break ns_sl4c;
                if ((r.cl & 0x10) != 0) { // break ns_sl4c;
                    r.addAx(pw.partWk[r.di]._lfodat);
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
ns_sl3:
        ;
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) ((r.bl << 1) | (r.bl >> 7));
        if (r.carry) { // break ns_sl2;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune3);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) ((r.bh << 1) | (r.bh >> 7));
            if (r.carry) { // break ns_sl3b;
                if ((r.cl & 1) != 0) { // break ns_sl3b;
                    r.addAx(pw.partWk[r.di].lfodat);
                }
            }
//ns_sl3b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) ((r.ch << 1) | (r.ch >> 7));
            if (r.carry) { // break ns_sl3c;
                if ((r.cl & 0x10) != 0) { // break ns_sl3c;
                    r.addAx(pw.partWk[r.di]._lfodat);
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
ns_sl2:
        ;
        r.carry = (r.bl & 0x80) != 0;
        r.bl = (byte) ((r.bl << 1) | (r.bl >> 7));
        if (r.carry) { // break ns_sl1;

            r.stack.push(r.getAx());
            r.addAx(pw.slot_detune2);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) ((r.bh << 1) | (r.bh >> 7));
            if (r.carry) { // break ns_sl2b;
                if ((r.cl & 1) != 0) { // break ns_sl2b;
                    r.addAx(pw.partWk[r.di].lfodat);
                }
            }
//ns_sl2b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) ((r.ch << 1) | (r.ch >> 7));
            if (r.carry) { // break ns_sl2c;
                if ((r.cl & 0x10) != 0) { // break ns_sl2c;
                    r.addAx(pw.partWk[r.di]._lfodat);
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
        r.bl = (byte) ((r.bl << 1) | (r.bl >> 7));
        if (r.carry) { // break ns_exit;

            r.addAx(pw.slot_detune1);
            r.carry = (r.bh & 0x80) != 0;
            r.bh = (byte) ((r.bh << 1) | (r.bh >> 7));
            if (r.carry) { // break ns_sl1b;
                if ((r.cl & 1) != 0) { // break ns_sl1b;
                    r.addAx(pw.partWk[r.di].lfodat);
                }
            }
//ns_sl1b:
            r.carry = (r.ch & 0x80) != 0;
            r.ch = (byte) ((r.ch << 1) | (r.ch >> 7));
            if (r.carry) { // break ns_sl1c;
                if ((r.cl & 0x10) != 0) { // break ns_sl1c;
                    r.addAx(pw.partWk[r.di]._lfodat);
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

    //==============================================================================
    // FM音源のdetuneでオクターブが変わる時の修正
    //  input CX:block / AX:fnum+detune
    //  output CX:block / AX:fnum
    //==============================================================================
    private void fm_block_calc() {
        r.sign = (r.getAx() & 0x8000) != 0;
//od0:
        while (true) {
            if (!r.sign) { // break od1;

                if (r.getAx() >= 0x26a) { // break od1;
                    //
                    if (r.getAx() < 0x26a * 2) { // 04d2h
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
                    // モウ コレイジョウ アガンナイヨン
                    r.setCx((short) 0x3800);
                    if (r.getAx() >= 0x800) { // break od_ret;
                        r.setAx((short) 0x7ff); // 04d2h
                    }
//od_ret:
                    return;
                }
            }
            // ;
//od1:
            r.carry = r.getCx() < 0x800;
            r.subCx((short) 0x800); // oct.down
            if (r.carry) break; // od15;
            r.addAx((short) 0x26a); // 4d2h-26ah
            r.sign = (r.getAx() & 0x8000) != 0;
        } // break od0;
//od15:
        // モウ コレイジョウ サガンナイヨン
        r.setCx((short) 0);
        r.sign = (r.getAx() & 0x8000) != 0;
        if (!r.sign) { // break od16;
            if (r.getAx() >= 8) { // 4
//                break od2;
                return;
            }
        }
//od16:
        r.setAx((short) 8); // 4
        // ;
//od2:
    }

    //==============================================================================
    // ＰＳＧ 音程設定
    //==============================================================================
    private void otodasip() {
        r.setAx(pw.partWk[r.di].fnum);
        if (r.getAx() == 0) { // break od_00p;
            return;
        }
//od_00p:
        //
        // PSG Portament set
        //
        r.setAx((short) (r.getAx() + pw.partWk[r.di].porta_num));
        //
        // PSG Detune/LFO set
        //
        if ((pw.partWk[r.di].extendmode & 1) == 0) { // break od_ext_detune;
            r.subAx(pw.partWk[r.di].detune);
            if ((pw.partWk[r.di].lfoswi & 1) != 0) { // break od_notlfo1;
                r.subAx(pw.partWk[r.di].lfodat);
            }
//od_notlfo1:
            if ((pw.partWk[r.di].lfoswi & 0x10) != 0) { // break tonesetp;
                r.subAx(pw.partWk[r.di]._lfodat);
            }
//            break tonesetp;
        } else {
//od_ext_detune:
            // 拡張DETUNE(DETUNE)の計算
            r.stack.push(r.getAx());
            r.setBx(pw.partWk[r.di].detune);
            if (r.getBx() != 0) { // break od_ext_lfo; // LFOへ

                int ans = r.getAx() * r.getBx(); //  imul    bx
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
                r.subAx(r.getDx()); // Detuneをずらす
                r.stack.push(r.getAx());
            }
//od_ext_lfo:
            // 拡張DETUNE(LFO)の計算
            r.setDx((short) 0);
            if ((pw.partWk[r.di].lfoswi & 0x11) != 0) { // break extlfo_set;
                r.setDx((short) 0);
                if ((pw.partWk[r.di].lfoswi & 0x1) != 0) { // break od_ext_notlfo1;
                    r.setDx(pw.partWk[r.di].lfodat);
                }
//od_ext_notlfo1:
                if ((pw.partWk[r.di].lfoswi & 0x10) != 0) { // break od_ext_notlfo2;
                    r.addDx(pw.partWk[r.di]._lfodat);
                }
//od_ext_notlfo2:
                if (r.getDx() != 0) { // break extlfo_set;
                    int ans1 = r.getAx() * r.getDx(); //  imul    dx
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
            r.subAx(r.getDx()); // LFOをずらす
        }
        //
        // TONE SET
        //
//tonesetp:
        if (true) { // 突撃mixでは0 //KUMA:false
            if (r.getAx() >= 0x1000) { // break tsp_01;
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

    //==============================================================================
    // FM ＶＯＬＵＭＥ ＳＥＴ
    //==============================================================================
    //------------------------------------------------------------------------------
    // スロット毎の計算 & 出力 マクロ
    //   in. dl 元のTL値
    // dh Outするレジスタ
    // al 音量変動値 中心=80h
    //------------------------------------------------------------------------------
    private void volset_slot() {
        r.carry = (r.al + r.dl > 0xff);
        r.al += r.dl;
        if (r.carry) { // break vsl_noover1;
            r.al = (byte) 255;
        }
//vsl_noover1:
        r.carry = (r.al < 0x80);
        r.al -= 0x80;
        if (r.carry) { // break vsl_noover2;
            r.al = 0;
        }
//vsl_noover2:
        r.dl = r.al;
        opnset();
    }

    //------------------------------------------------------------------------------
    // FM音量設定メイン
    //------------------------------------------------------------------------------
    private void volset() {
        r.bl = pw.partWk[r.di].slotmask; // bl<- slotmask
        if (r.bl == 0) { // break vs_exec;
            return; // SlotMaskが0の時
        }
//vs_exec:
        r.al = pw.partWk[r.di].volpush;
        if (r.al != 0) { // break vs_00a;

            r.al--;
//            break vs_00;
        } else {
//vs_00a:
            r.al = pw.partWk[r.di].volume;
        }
//vs_00:
        r.cl = r.al;
        if (r.di == pw.part_e) {
            fmvs(); // 効果音の場合はvoldown/fadeout影響無し
            return;
        }

        pmdAsm_4743_voldown();
    }

    //------------------------------------------------------------------------------
    // 音量down計算
    //------------------------------------------------------------------------------
    private void pmdAsm_4743_voldown() {
        r.al = pw.fm_voldown;
        if (r.al == 0) {
            fm_fade_calc();
            return;
        }

        r.al = (byte) -r.al;
        r.setAx((short) (r.al * r.cl));
        r.cl = r.ah;

        fm_fade_calc();
    }

    //------------------------------------------------------------------------------
    // Fadeout計算
    //------------------------------------------------------------------------------
    private void fm_fade_calc() {
        r.al = pw.fadeout_volume;
        if (r.al >= 2) {
            r.al >>= 1; //50%下げれば充分
            r.al = (byte) -r.al;
            r.setAx((short) (r.al * r.cl));
            r.cl = r.ah;
        }

        fmvs();
    }

    //------------------------------------------------------------------------------
    // 音量をcarrierに設定 & 音量LFO処理
    //  input cl to Volume[0 - 127]
    // bl to SlotMask
    //------------------------------------------------------------------------------
    private void fmvs() {
        r.bh = 0; // Vol Slot Mask
        r.ch = r.bl; // ch=SlotMask Push

        r.stack.push(r.getSi());
        r.setSi((short) 0); // offset vol_tbl
        pw.vol_tbl[r.getSi()] = 0x80;
        pw.vol_tbl[r.getSi() + 1] = 0x80;
        pw.vol_tbl[r.getSi() + 2] = 0x80;
        pw.vol_tbl[r.getSi() + 3] = 0x80;

        r.cl = (byte) ~r.cl; // cl=carrierに設定する音量+80H(add)
        r.bl &= pw.partWk[r.di].carrier; // bl=音量 を設定するSLOT xxxx0000b
        r.bh |= r.bl;
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_01;

            pw.vol_tbl[r.getSi()] = r.cl;
        }
//fmvs_01:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_02;

            pw.vol_tbl[r.getSi()] = r.cl;
        }
//fmvs_02:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_03;

            pw.vol_tbl[r.getSi()] = r.cl;
        }
//fmvs_03:
        r.incSi();
        r.bl = r.rol(r.bl, 1);
        if (r.carry) { // break fmvs_04;

            pw.vol_tbl[r.getSi()] = r.cl;
        }
//fmvs_04:
        r.subSi((short) 3);
        if (r.cl != 255) { // 音量0? // break fmvs_no_lfo;

            if ((pw.partWk[r.di].lfoswi & 2) != 0) { // break fmvs_not_vollfo1;

                r.bl = pw.partWk[r.di].volmask;
                r.bl &= r.ch; // bl=音量LFOを設定するSLOT xxxx0000b
                r.bh |= r.bl;
                r.setAx(pw.partWk[r.di].lfodat); // ax=音量LFO変動値(sub)
                fmlfo_sub();
            }
//fmvs_not_vollfo1:
            if ((pw.partWk[r.di].lfoswi & 0x20) != 0) { // break fmvs_no_lfo;

                r.bl = pw.partWk[r.di]._volmask; // mov bl,_volmask[di]
                r.bl &= r.ch; // bh=音量LFOを設定するSLOT xxxx0000b
                r.bh |= r.bl;
                r.setAx(pw.partWk[r.di]._lfodat); // ax=音量LFO変動値(sub)
                fmlfo_sub();
            }
        }
//fmvs_no_lfo:
        r.dh = 0x4c - 1;
        r.dh += pw.partb; // dh=FM Port Address
        r.al = (byte) pw.vol_tbl[r.incSi()]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_01;
            r.dl = pw.partWk[r.di].slot4;
            volset_slot();
        }
//fmvm_01:
        r.dh -= 8;
        r.al = (byte) pw.vol_tbl[r.incSi()]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_02;
            r.dl = pw.partWk[r.di].slot3;
            volset_slot();
        }
//fmvm_02:
        r.dh += 4;
        r.al = (byte) pw.vol_tbl[r.incSi()]; // lodsb
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_03;
            r.dl = pw.partWk[r.di].slot2;
            volset_slot();
        }
//fmvm_03:
        r.bh = r.rol(r.bh, 1);
        if (r.carry) { // break fmvm_04;
            r.dh -= 8;
            r.al = (byte) pw.vol_tbl[r.incSi()]; // lodsb
            r.dl = pw.partWk[r.di].slot1;
            volset_slot();
        }
//fmvm_04:
        r.setSi(r.stack.pop());
    }

    //------------------------------------------------------------------------------
    // 音量LFO用サブ
    //------------------------------------------------------------------------------
    private void fmlfo_sub() {
        r.stack.push(r.getCx());
        r.setCx((short) 4);
//fmlfo_loop:
        do {
            r.carry = (r.bl & 0x80) != 0;
            r.bl = (byte) ((r.bl << 1) | (r.bl >> 7));
            if (r.carry) { // break fml_exit;
                if ((r.al & 0x80) == 0) { // break fmls_minus;
                    r.carry = (pw.vol_tbl[r.getSi()] < r.al);
                    pw.vol_tbl[r.getSi()] -= r.al;
                    if (r.carry) { // break fml_exit;
                        pw.vol_tbl[r.getSi()] = 0;
                    }
//                    break fml_exit;
                } else {
//fmls_minus:
                    r.carry = (pw.vol_tbl[r.getSi()] < r.al);
                    pw.vol_tbl[r.getSi()] -= r.al;
                    if (!r.carry) { // break fml_exit;
                        pw.vol_tbl[r.getSi()] = 0xff;
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


    //4887-4994
    //==============================================================================
    // ＰＳＧ ＶＯＬＵＭＥ ＳＥＴ
    //==============================================================================
    private void volsetp() {
        if (pw.partWk[r.di].envf == 3) {
//            break volsetp_ret;
            return;
        }
        if (pw.partWk[r.di].envf == (byte) 0xff) { // -1 // break vsp_00;
            if (pw.partWk[r.di].eenv_count == 0) { // break vsp_00;
//volsetp_ret:
                return;
            }
        }
//vsp_00:
        r.al = pw.partWk[r.di].volpush;
        if (r.al != 0) { // break vsp_01a;
            r.al--;
//            break vsp_01;
        } else {
//vsp_01a:
            r.al = pw.partWk[r.di].volume;
        }
//vsp_01:
        r.dl = r.al;
        //------------------------------------------------------------------------------
        // 音量down計算
        //------------------------------------------------------------------------------
        r.al = pw.ssg_voldown;
        if (r.al != 0) { // break psg_fade_calc;
            r.al = (byte) -r.al;
            r.setAx((short) (r.al * r.dl));
            r.dl = r.ah;
        }
        //------------------------------------------------------------------------------
        // Fadeout計算
        //------------------------------------------------------------------------------
//psg_fade_calc:
        r.al = pw.fadeout_volume;
        if (r.al != 0) { // break psg_env_calc;
            r.al = (byte) -r.al;
            r.setAx((short) (r.al * r.dl));
            r.dl = r.ah;
        }
        //------------------------------------------------------------------------------
        // ENVELOPE 計算
        //------------------------------------------------------------------------------
//psg_env_calc:
pv_out: // ↑
        if (r.dl != 0) { // 音量0? // break pv_out;
            if (pw.partWk[r.di].envf == (byte) 0xff) { // -1 // break normal_pvset;
                r.al = r.dl; // 拡張版 音量 = dl * (eenv_vol + 1) / 16
                r.dl = pw.partWk[r.di].eenv_volume;
                if (r.dl == 0) {
//                    break pv_min;
//pv_min:
                    r.dl = 0;

                    if (r.dl == 0) break pv_out; // 0になったら音量LFOは掛けない
                    if (r.dl >= 16) { // break pv1;
                        r.dl = 15;
                    }
                } else {
                    r.dl++;
                    r.setAx((short) (r.al * r.dl));
                    r.dl = r.al;
                    r.dl >>= 3;
                    r.carry = ((r.dl % 2) != 0);
                    r.dl >>= 1;
                    if (r.carry) { // break pv1;
                        r.dl++;
                    }
//                    break pv1;
                }
            } else {
//normal_pvset:
                r.dl += pw.partWk[r.di].eenv_volume; // .penv;
                if ((r.dl & 0x80) != 0) { // break pv0;
pv_min:
                    r.dl = 0;
                }
//pv0:
                if (r.dl == 0) break pv_out; // 0になったら音量LFOは掛けない
                if (r.dl >= 16) { // break pv1;
                    r.dl = 15;
                }
            }
            //------------------------------------------------------------------------------
            // 音量LFO計算
            //------------------------------------------------------------------------------
//pv1:
            if ((pw.partWk[r.di].lfoswi & 0x22) != 0) { // break pv_out;
                r.setAx((short) 0);
                if ((pw.partWk[r.di].lfoswi & 0x2) != 0) { // break pv_nolfo1;
                    r.setAx(pw.partWk[r.di].lfodat);
                }
//pv_nolfo1:
                if ((pw.partWk[r.di].lfoswi & 0x20) != 0) { // break pv_nolfo2;
                    r.andAx(pw.partWk[r.di]._lfodat);
                }
//pv_nolfo2:
                r.dh = 0;
                r.addDx(r.getAx());
                if ((r.getDx() & 0x8000) != 0) { // break pv10;
                    r.dl = 0;
//                    break pv_out;
                } else {
//pv10:
                    if (r.getDx() >= 16) { // break pv_out;
                        r.dl = 15;
                    }
                }
            }
        }
        //------------------------------------------------------------------------------
        // 出力
        //------------------------------------------------------------------------------
//pv_out:
        r.dh = pw.partb;
        r.dh += 8 - 1;
        //Console.WriteLine("%d %d", r.dh, r.dl);
        opnset44();
    }

    //==============================================================================
    // FM ＫＥＹＯＮ
    //==============================================================================
    private void keyon() {
        if (pw.partWk[r.di].onkai == (byte) 0xff) { //-1 // break ko1;
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
            r.al = pw.fmKeyOnDataTbl[r.getBx()];
            r.al |= pw.partWk[r.di].slotmask;
            if (pw.partWk[r.di].sdelay_c != 0) { // break no_sdm;
                r.al &= pw.partWk[r.di].sdelay_m;
            }
//no_sdm:
            pw.fmKeyOnDataTbl[r.getBx()] = r.al;
            r.dl |= r.al;
            opnset44();
            return;
        }
//ura_keyon:
        if (pw.board2 != 0) {
            r.addBx((short) 3); // offset ura_key1
            r.al = pw.fmKeyOnDataTbl[r.getBx()];
            r.al |= pw.partWk[r.di].slotmask;
            if (pw.partWk[r.di].sdelay_c != 0) { // break no_sdm2;
                r.al &= pw.partWk[r.di].sdelay_m;
            }
//no_sdm2:
            pw.fmKeyOnDataTbl[r.getBx()] = r.al;
            r.dl |= r.al;
            r.dl |= 0b0000_0100; // Ura Port
            opnset44();
            return;
        }
    }

    //==============================================================================
    // ＰＳＧ ＫＥＹＯＮ
    //==============================================================================
    private void keyonp() {
        if (pw.partWk[r.di].onkai == (byte) 0xff) { // -1 // break ko1p;
            return; // when a rest
        }
//ko1p:
        //    pushf
        //    cli
        psgmsk(); // AL=07h AH = Maskdata
        r.al |= r.ah;
        r.ah &= pw.partWk[r.di].psgpat;
        r.ah = (byte) ~r.ah;
        r.al &= r.ah;
        r.dh = 7;
        r.dl = r.al;
        opnset44();
        //    popf
        // ;
        // ; PSG ノイズ シュウハスウ ノ セット
        // ;
        r.dl = pw.psnoi;
        if (r.dl != pw.psnoi_last) { // break psnoi_ret; // ; 同じなら定義しない
            if ((pw.psgefcnum & 0x80) != 0) { // break psnoi_ret; // PSG効果音発音中は変更しない
                r.dh = 6;
                opnset44();
                pw.psnoi_last = r.dl;
            }
        }
//psnoi_ret:
    }

    //==============================================================================
    // ＰＳＧ07hポートのKEYON/OFF準備(07Hを読み、マスクする値を算出)
    // OUTPUT...al<- 07h Read Data
    //      ah<- Mask Data
    //==============================================================================
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

    //==============================================================================
    // KEY OFF
    // don't Break AL
    //==============================================================================
    private void keyoff() {
        if (pw.partWk[r.di].onkai != (byte) 0xff) {
            kof1();
            return;
        }
        return; // when a rest
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

            r.addBx((short) 0); // offset omote_key1 KUMA: fmKeyOnDataTblへの位置

            r.cl = pw.partWk[r.di].slotmask;
            r.cl = (byte) ~r.cl;
            r.cl &= pw.fmKeyOnDataTbl[r.getBx()];

            pw.fmKeyOnDataTbl[r.getBx()] = r.cl;
            r.dl |= r.cl;
            opnset44();
            return;
        }
//ura_keyoff:
        if (pw.board2 != 0) {
            r.addBx((short) 3); // offset ura_key1 KUMA: fmKeyOnDataTblへの位置(裏は+3)

            r.cl = pw.partWk[r.di].slotmask;
            r.cl = (byte) ~r.cl;
            r.cl &= pw.fmKeyOnDataTbl[r.getBx()];

            pw.fmKeyOnDataTbl[r.getBx()] = r.cl;
            r.dl |= r.cl;
            r.dl |= 0b0100; // FM Ura Port
            opnset44();
        }
    }

    public void keyoffp() {
        if (pw.partWk[r.di].onkai != (byte) 0xff) {
            kofp1();
        }
        // when a rest
    }

    private void kofp1() {
        if (pw.partWk[r.di].envf != (byte) 0xff) { //  break kofp1_ext;
            pw.partWk[r.di].envf = 2;
            return;
        }
//kofp1_ext:
        pw.partWk[r.di].eenv_count = 4;
    }


    //5138-5152
    //==============================================================================
    // 音色の設定
    //  INPUTS -- [PARTB]
    //   -- dl[TONE_NUMBER]
    //   -- di[PART_DATA_ADDRESS]
    //==============================================================================
    private void neiroset() {
        toneadr_calc();
        silence_fmpart();
        if (!r.carry) {
            neiroset_main();
            return;
        }
        // neiromask=0の時(TLのworkのみ設定)
        r.addBx((short) 4);
        neiroset_tl();
    }


    //5153-5196
    //==============================================================================
    // 音色設定メイン
    //==============================================================================
    //------------------------------------------------------------------------------
    // AL/FBを設定
    //------------------------------------------------------------------------------
    private void neiroset_main() {
        r.dh = (byte) (0xb0 - 1);
        r.dh += pw.partb;
        if (pw.inst != null && r.getBx() + 24 < pw.inst.length) r.dl = (byte) pw.inst[r.getBx() + 24].dat;
        else r.dl = 0;

        if (pw.af_check != 0) { // ALG/FBは設定しないmodeか？ // break no_af;

            r.dl = pw.partWk[r.di].alg_fb;
        }
//no_af:
nss_notfm3: // ↑
        if (pw.partb == 3) { // break nss_notfm3;

            if (pw.board2 != 0) {
                if (pw.fmsel != 0)
                    break nss_notfm3;
            } else {
                if (r.di == pw.part_e)
                    break nss_notfm3;
            }

            if (pw.af_check != 0) { // ALG/FBは設定しないmodeか？ // break set_fm3_alg_fb;

                r.dl = pw.fm3_alg_fb;
//                break nss_notfm3;
            } else {
//set_fm3_alg_fb:
                if ((pw.partWk[r.di].slotmask & 0x10) == 0) { // slot1を使用していなければ // break nss_notslot1;

                    r.al = pw.fm3_alg_fb;
                    r.al &= 0b0011_1000; // fbは前の値を使用
                    r.dl &= 0b0000_0111;
                    r.dl |= r.al;
                }
//nss_notslot1:
                pw.fm3_alg_fb = r.dl;
            }
        }
//nss_notfm3:
        opnset();
        pw.partWk[r.di].alg_fb = r.dl;
        r.dl &= 7; // dl=algo

        check_carrier();
    }


    //5197-5220
    //------------------------------------------------------------------------------
    // Carrierの位置を調べる(VolMaskにも設定)
    //------------------------------------------------------------------------------
    private void check_carrier() {
        r.stack.push(r.getBx());
        r.bh = 0;
        r.bl = r.dl;
        r.addBx((short) 0); // offset carrier_table
        r.al = (byte) pw.carrier_table[r.getBx()];
        if ((pw.partWk[r.di].volmask & 0xf) == 0) { // break not_set_volmask; // Volmask値が0以外の場合は設定しない
            pw.partWk[r.di].volmask = r.al;
        }
//not_set_volmask:
        if ((pw.partWk[r.di]._volmask & 0xf) == 0) { // break not_set_volmask2;
            pw.partWk[r.di]._volmask = r.al;
        }
//not_set_volmask2:
        pw.partWk[r.di].carrier = r.al;
        r.ah = (byte) pw.carrier_table[r.getBx() + 8]; // slot2/3の逆転データ(not済み)
        r.setBx(r.stack.pop());
        r.al = pw.partWk[r.di].neiromask;
        r.ah &= r.al; // AH=TL用のmask / AL=その他用のmask

        // ------------------------------------------------------------------------------
        // 各音色パラメータを設定(TLはモジュレータのみ)
        // ------------------------------------------------------------------------------
        r.dh = 0x30 - 1;
        r.dh += pw.partb;
        r.setCx((short) 4); // DT / ML
//ns01:
        do {
            if (pw.inst != null && r.getBx() < pw.inst.length) r.dl = (byte) pw.inst[r.getBx()].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) ((r.al << 1) | ((r.al & 0x80) >> 7));
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
            if (pw.inst != null && r.getBx() < pw.inst.length) r.dl = (byte) pw.inst[r.getBx()].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) ((r.al << 1) | ((r.al & 0x80) >> 7));
            if (r.carry) { // break ns_nsb;
                opnset();
            }
//ns_nsb:
            r.dh += 4;
            r.decCx();
        } while (r.getCx() != 0); // break ns01b;

        r.setCx((short) 16); // 残り
ns01c:
        do {
            if (pw.inst != null && r.getBx() < pw.inst.length) r.dl = (byte) pw.inst[r.getBx()].dat;
            else r.dl = 0;
            r.incBx();
            r.carry = ((r.al & 0x80) != 0);
            r.al = (byte) ((r.al << 1) | ((r.al & 0x80) >> 7));
            if (r.carry) { // break ns_nsc;
                opnset();
            }
//ns_nsc:
            r.dh += 4;
            r.decCx();
        } while (r.getCx() != 0); // break ns01c;

        // ------------------------------------------------------------------------------
        // SLOT毎のTLをワークに保存
        // ------------------------------------------------------------------------------
        r.subBx((short) 20);
        neiroset_tl();
    }

    private void neiroset_tl() {
        r.stack.push(r.getSi());
        r.stack.push(r.di);

        r.setSi(r.getBx());
        //r.di += pw.slot1;

        if (pw.inst != null && r.getSi() + 3 < pw.inst.length) {
            pw.partWk[r.di].slot1 = (byte) pw.inst[r.getSi() + 0].dat;
            pw.partWk[r.di].slot3 = (byte) pw.inst[r.getSi() + 1].dat;
            pw.partWk[r.di].slot2 = (byte) pw.inst[r.getSi() + 2].dat;
            pw.partWk[r.di].slot4 = (byte) pw.inst[r.getSi() + 3].dat;
        }

        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        return;
    }

    //==============================================================================
    // TONE DATA START ADDRESS を計算
    //  input dl  tone_number
    //  output bx  address
    //==============================================================================
    private void toneadr_calc() {
        if (pw.prg_flg == 0) { // break prgdat_get;

            if (r.di != pw.part_e) { // break prgdat_get;

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
        if (r.di == pw.part_e) { // break gpd_loop;

            r.setBx(pw.prgdat_adr2); // FM効果音の場合
        }
//gpd_loop:
        while (true) {
            pw.inst = pw.md;
            if (r.getBx() >= pw.inst.length) {
                throw new PmdException("お探しの音色番号は見つかりませんでした。");
            }
            if (pw.inst[r.getBx()].dat == r.dl)
                break; // gpd_exit;
            r.andBx((short) 26);
//            break gpd_loop;
        }
//gpd_exit:
        r.incBx();
    }

    //==============================================================================
    // [PartB]
    //        のパートの音を完璧に消す(TL= 127 and RR = 15 and KEY-OFF)
    // cy=1 ・・・ 全スロットneiromaskされている
    //==============================================================================
    private void silence_fmpart() {
        r.al = pw.partWk[r.di].neiromask;
        if (r.al != 0) { // break sfm_exit;

            r.stack.push(r.getDx());
            r.dh = pw.partb;
            r.dh += 0x40 - 1;
            r.setCx((short) 4);
            r.dl = 127; // TL = 127 / RR=15
ns00c:
            do {
                r.carry = ((r.al & 0x80) != 0);
                r.al = (byte) ((r.al << 1) | ((r.al & 0x80) >> 7));
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

    //==============================================================================
    // LFO処理
    //  Don't Break cl
    //  output cy = 1    変化があった
    //==============================================================================
    public void lfo() {
        //lfop:;
        if (pw.partWk[r.di].delay != 0) { // break lfo1;
            pw.partWk[r.di].delay--; // cy=0
lfo_ret:
            return;
        }
//lfo1:
        if ((pw.partWk[r.di].extendmode & 2) != 0) { // TimerAと合わせるか？ // break lfo_normal; // そうじゃないなら無条件にlfo処理
            r.ch = pw.TimerAtime;
            r.ch -= pw.lastTimerAtime;
            if (r.ch == 0) {
//                break lfo_ret; // 前回の値と同じなら何もしない cy = 0
                return; // <<
            }

            r.setAx(pw.partWk[r.di].lfodat);
            r.stack.push(r.getAx());

//lfo_loop:
            do {
                lfo_main();
                r.ch--;
            } while (r.ch != 0); // break lfo_loop;

//            break lfo_check;
        } else {
//lfo_normal:
            r.setAx(pw.partWk[r.di].lfodat);
            r.stack.push(r.getAx());
            lfo_main();
        }
//lfo_check:
        r.setAx(r.stack.pop());

        if (r.getAx() == pw.partWk[r.di].lfodat) { // break lfo_stc_ret;
            return; // c=0
        }
//lfo_stc_ret:
        r.carry = true;
    }

    private void lfop() {
        lfo();
    }

    private void lfo_main() {
        if (pw.partWk[r.di].speed != 1) { // break lfo2;
            if (pw.partWk[r.di].speed != (byte) 0xff) { // -1 // break lfom_ret;
                pw.partWk[r.di].speed--;
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
                        r.al = pw.partWk[r.di].speed2;
                        pw.partWk[r.di].speed = r.al;
                        r.bl = pw.partWk[r.di].lfo_wave;
                        if (r.bl != 0) { // break lfo_sankaku;
                            if (r.bl != 4) { // break lfo_sankaku;
                                if (r.bl != 2) { // break lfo_kukei;
                                    if (r.bl == 6)
                                        break lfo_oneshot;
                                    if (r.bl != 5)
                                        break not_sankaku;
                                    // 三角波 lfowave = 0,4,5
                                    r.al = pw.partWk[r.di].step;
                                    r.ah = r.al;
                                    if ((r.ah & 0x80) != 0) { // break lfo2ns;
                                        r.ah = (byte) -r.ah;
                                    }
//lfo2ns:
                                    r.setAx((short) (r.al * r.ah)); // lfowave=5の場合 1step = step×｜step｜
                                    break lfo20;
                                }
                            }
                        }
//lfo_sankaku:
                        r.al = pw.partWk[r.di].step;
                        r.setAx(r.al); // cbw
                    }
//lfo20:
                    pw.partWk[r.di].lfodat += r.getAx();
                    if (pw.partWk[r.di].lfodat == 0) { // break lfo21;
                        md_inc();
                    }
//lfo21:
                    r.al = pw.partWk[r.di].time;
                    if (r.al != (byte) 255) { // break lfo3;
                        r.al--;
                        if (r.al == 0) { // break lfo3;
                            r.al = pw.partWk[r.di].time2;
                            if (r.bl != 4) { // break lfo22;
                                r.al += r.al; // lfowave=0,5の場合 timeを反転時２倍にする
                            }
//lfo22:
                            pw.partWk[r.di].time = r.al;
                            r.al = pw.partWk[r.di].step;
                            r.al = (byte) -r.al;
                            pw.partWk[r.di].step = r.al;
                            return;
                        }
                    }
//lfo3:
                    pw.partWk[r.di].time = r.al;
                    return;
                }
//not_sankaku:
                r.bl--;
                if (r.bl != 0)
                    break not_nokogiri;
                // ノコギリ波 lfowave = 1,6
                r.al = pw.partWk[r.di].step;
                r.setAx(r.al); // cbw
                pw.partWk[r.di].lfodat += r.getAx();
                r.al = pw.partWk[r.di].time;
                if (r.al != 0xff) { // -1 // break nk_lfo3;
                    r.al--;
                    if (r.al == 0) { // break nk_lfo3;
                        pw.partWk[r.di].lfodat = (short) -pw.partWk[r.di].lfodat;
                        md_inc();

                        r.al = pw.partWk[r.di].time2;
                        r.al += r.al;
                    }
                }
//nk_lfo3:
                pw.partWk[r.di].time = r.al;
                return;
            }
//lfo_oneshot:
            // ワンショット lfowave = 6
            r.al = pw.partWk[r.di].time;
            if (r.al != 0) { // break lfoone_ret;
                if (r.al != 0xff) { // -1 // break lfoone_nodec;
                    r.al--;
                    pw.partWk[r.di].time = r.al;
                }
//lfoone_nodec:
                r.al = pw.partWk[r.di].step;
                r.setAx(r.al); // cbw
                pw.partWk[r.di].lfodat += r.getAx();
            }
//lfoone_ret:
            return;

//lfo_kukei:
            // 矩形波 lfowave = 2
//            r.al = pw.partWk[r.di].step; // TODO unreachable
//            r.setAx((short) ((byte) r.al * (byte) pw.partWk[r.di].time));
//            pw.partWk[r.di].lfodat = r.getAx();
//            md_inc();
//            pw.partWk[r.di].step = (byte) -pw.partWk[r.di].step;
//            return;
        }
//not_nokogiri:
        // ランダム波 lfowave = 3
        r.al = pw.partWk[r.di].step;
        if ((r.al & 0x80) != 0) { // break ns_plus;
            r.al = (byte) -r.al;
        }
//ns_plus:
        r.setAx((short) (r.al * pw.partWk[r.di].time));
        r.stack.push(r.getAx());
        r.stack.push(r.getCx());
        r.addAx(r.getAx());
        rnd();
        r.setCx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.subAx(r.getBx());
        pw.partWk[r.di].lfodat = r.getAx();

        md_inc();
    }

    //==============================================================================
    // MDコマンドの値によってSTEP値を変更
    //==============================================================================
    private void md_inc() {
        pw.partWk[r.di].mdspd--;
        if (pw.partWk[r.di].mdspd != 0) {
//            break md_exit;
            return;
        }
        r.al = pw.partWk[r.di].mdspd2;
        pw.partWk[r.di].mdspd = r.al;
        r.al = pw.partWk[r.di].mdc;
        if (r.al == 0) {
//            break md_exit; // count =0
            return;
        }
        if ((r.al & 0x80) == 0) { // break mdi21; // count > 127 (255)
            r.al--;
            pw.partWk[r.di].mdc = r.al;
        }
//mdi21:
        r.al = pw.partWk[r.di].step;
        if ((r.al & 0x80) != 0) { // break mdi22;
            r.al = (byte) -r.al;
            r.al += pw.partWk[r.di].mdepth;
            if ((r.al & 0x80) == 0) { // break mdi21_ov;
                r.al = (byte) -r.al;
//mdi21_s:
                pw.partWk[r.di].step = r.al;

//md_exit:
                return;
            }
//mdi21_ov:
            r.al = 0;
            if ((pw.partWk[r.di].mdepth & 0x80) == 0) { // break mdi21_s;
                r.al = (byte) 0x81; // -127;
            }
//            break mdi21_s;
            pw.partWk[r.di].step = r.al; // <<
            return; // <<
        }
//mdi22:
        r.al += pw.partWk[r.di].mdepth;
        if ((r.al & 0x80) != 0) { // break mdi22_ov;
//mdi22_ov:
            r.al = 0;
            if ((pw.partWk[r.di].mdepth & 0x80) == 0) { // break mdi22_s;
                r.al = 0x7f;
            }
//            break mdi22_s;
        }
//mdi22_s:
        pw.partWk[r.di].step = r.al; // ↓
    }

    //==============================================================================
    // 乱数発生ルーチン INPUT : AX=MAX_RANDOM
    //    OUTPUT: AX=RANDOM_NUMBER
    //==============================================================================
    private void rnd() {
        r.setCx(r.getAx());
        r.setAx((short) 259);

        r.setAx((short) (r.getAx() * pw.seed));
        r.andAx((short) 3);
        r.andAx((short) 32767); // 0x7fff

        pw.seed = r.getAx();
        int ans = (r.getAx() * r.getCx());
        r.setCx((short) 32767);
        r.setAx((short) (ans / r.getCx()));
        r.setDx((short) (ans % r.getCx()));
    }

    //==============================================================================
    // LFOとＰＳＧ／PCMのソフトウエアエンベロープの初期化
    //==============================================================================
    //==============================================================================
    // ＰＳＧ／PCM音源用 Entry
    //==============================================================================
    public void lfoinitp() {
        r.ah = r.al; // キューフ ノ トキ ハ INIT シナイヨ
        r.ah &= 0xf;
        if (r.ah == 0xc) { // break lip_00;
            r.al = pw.partWk[r.di].onkai_def;
            r.ah = r.al;
            r.ah &= 0xf;
        }
//lip_00:
        pw.partWk[r.di].onkai_def = r.al;

        if (r.ah != 0xf) { // 4.8r 修正 // break lfo_exitp;
            pw.partWk[r.di].porta_num = 0; // ポルタメントは初期化

            if ((pw.tieflag & 1) == 0) { // マエ ガ & ノ トキ モ INIT シナイ。
                seinit();
                return;
            }
        }
//lfo_exitp:
        r.stack.push(r.getAx());
        soft_env(); // 前が & の場合 -> 1回 SoftEnv処理
        r.setAx(r.stack.pop());

        lfo_exit();
        // ここまで
    }

    //==============================================================================
    // ソフトウエアエンベロープ初期化
    //==============================================================================
    private void seinit() {
        if (pw.partWk[r.di].envf != (byte) 0xff) { // break extenv_init;

            pw.partWk[r.di].envf = 0;
            pw.partWk[r.di].eenv_volume = 0; // .penv

            r.ah = pw.partWk[r.di].eenv_arc; // .patb
            pw.partWk[r.di].eenv_ar = r.ah; // .pat
            if (r.ah == 0) { // break lfin2;
                pw.partWk[r.di].envf = 1; // ATTACK=0 ... スグ Decay ニ
                r.ah = pw.partWk[r.di].eenv_dr; // .pv2
                pw.partWk[r.di].eenv_volume = r.ah; // .penv
            }
//lfin2:
            r.ah = pw.partWk[r.di].eenv_src; // .pr1b
            pw.partWk[r.di].eenv_sr = r.ah; // .pr1
            r.ah = pw.partWk[r.di].eenv_rrc; // .pr2b
            pw.partWk[r.di].eenv_rr = r.ah; // .pr2
            lfin1();
            return;
        }
        // 拡張ssg_envelope用
//extenv_init:
        r.ah = pw.partWk[r.di].eenv_ar;
        r.ah -= 16;
        pw.partWk[r.di].eenv_arc = r.ah;
        r.ah = pw.partWk[r.di].eenv_dr;
        r.ah -= 16;
        if ((r.ah & 0x80) != 0) { // break eei_dr_notx;
            r.ah += r.ah;
        }
//eei_dr_notx:
        pw.partWk[r.di].eenv_drc = r.ah;

        r.ah = pw.partWk[r.di].eenv_sr;
        r.ah -= 16;
        if ((r.ah & 0x80) != 0) { // break eei_sr_notx;
            r.ah += r.ah;
        }
//eei_sr_notx:
        pw.partWk[r.di].eenv_src = r.ah;

        r.ah = pw.partWk[r.di].eenv_rr;
        r.ah += r.ah;
        r.ah -= 16;
        pw.partWk[r.di].eenv_rrc = r.ah;

        r.ah = pw.partWk[r.di].eenv_al;
        pw.partWk[r.di].eenv_volume = r.ah;
        pw.partWk[r.di].eenv_count = 1;

        r.stack.push(r.getAx());
        ext_ssgenv_main(); // 最初の１回
        r.setAx(r.stack.pop());

        lfin1();
    }

    //==============================================================================
    // FM音源用 Entry
    //==============================================================================
    private void lfoinit() {
        r.ah = r.al; // キューフ ノ トキ ハ INIT シナイヨ
        r.ah &= 0xf;
        if (r.ah == 0xc) { // break li_00;
            r.al = pw.partWk[r.di].onkai_def;
            r.ah = r.al;
            r.ah &= 0xf;
        }
//li_00:
        pw.partWk[r.di].onkai_def = r.al;

        if (r.ah == 0xf) {
            lfo_exit();
            return;
        }
        pw.partWk[r.di].porta_num = 0; // ポルタメントは初期化

        if ((pw.tieflag & 1) == 0) { // マエ ガ & ノ トキ モ INIT シナイ。
            lfin1();
            return;
        }
        lfo_exit();
    }

    private void lfo_exit() {
        if ((pw.partWk[r.di].lfoswi & 3) != 0) { // LFO使用中か？ // break le_no_one_lfo1; // ; 前が & の場合 -> 1回 LFO処理

            r.stack.push(r.getAx());
            lfo();
            r.setAx(r.stack.pop());
        }
//le_no_one_lfo1:
        if ((pw.partWk[r.di].lfoswi & 0x30) != 0) { // LFO使用中か？ // break le_no_one_lfo2; // 前が & の場合 -> 1回 LFO処理

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


    //5681-5757
    //==============================================================================
    // LFO初期化
    //==============================================================================
    private void lfin1() {
        if (pw.board2 != 0) {
            r.ah = pw.partWk[r.di].hldelay;
            pw.partWk[r.di].hldelay_c = r.ah;
            if (r.ah != 0) { // break non_hldelay;
                r.dh = pw.partb; //    mov dh,[partb]
                r.dh += 0xb4 - 1;
                r.dl = pw.partWk[r.di].fmpan;
                r.dl &= 0xc0; // HLFO = OFF
                opnset();
            }
//non_hldelay:
        }

        r.ah = pw.partWk[r.di].sdelay;
        pw.partWk[r.di].sdelay_c = r.ah;
        r.cl = pw.partWk[r.di].lfoswi;
        if ((r.cl & 3) != 0) { // break li_lfo1_exit; // LFOは未使用
            if ((r.cl & 4) == 0) { // keyon非同期か? break li_lfo1_next;
                lfoinit_main();
            }
//li_lfo1_next:
            r.stack.push(r.getAx());
            lfo();
            r.setAx(r.stack.pop());
        }
//li_lfo1_exit:
        if ((r.cl & 0x30) != 0) { // break li_lfo2_exit; // LFOは未使用
            if ((r.cl & 0x40) == 0) { // keyon非同期か? // break li_lfo2_next;

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
        pw.partWk[r.di].lfodat = 0;
        r.dl = pw.partWk[r.di].delay2; //    mov dx, word ptr delay2[di]
        r.dh = pw.partWk[r.di].speed2;
        pw.partWk[r.di].delay = r.dl;
        pw.partWk[r.di].speed = r.dh;
        r.dl = pw.partWk[r.di].step2; //    mov dx, word ptr step2[di]
        r.dh = pw.partWk[r.di].time2;
        pw.partWk[r.di].step = r.dl;
        pw.partWk[r.di].time = r.dh;

        r.dl = pw.partWk[r.di].mdc2;
        pw.partWk[r.di].mdc = r.dl;

        if (pw.partWk[r.di].lfo_wave == 2) { // 矩形波または
//            break lim_first;
//lim_first:
            pw.partWk[r.di].speed = 1; // delay直後にLFOが掛かるようにする
            return null;
        }
        if (pw.partWk[r.di].lfo_wave != 3) { // ランダム波の場合は
//            break lim_nofirst;
//lim_nofirst:
            pw.partWk[r.di].speed++; // それ以外の場合はdelay直後のspeed値を +1
            return null;
        }
        pw.partWk[r.di].speed = 1; // delay直後にLFOが掛かるようにする
        return null;
    }

    //==============================================================================
    // ＰＳＧ／PCMのソフトウエアエンベロープ
    //==============================================================================
    public void soft_env() {
        if ((pw.partWk[r.di].extendmode & 4) == 0) // TimerAと合わせるか？
        {
            soft_env_main(); // ; そうじゃないなら無条件にsenv処理
            return;
        }

        r.ch = pw.TimerAtime;
        r.ch -= pw.lastTimerAtime;
        if (r.ch != 0) { // break senv_ret; // 前回の値と同じなら何もしない cy = 0
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
        if (pw.partWk[r.di].envf == (byte) 0xff) // -1
        {
            ext_ssgenv_main();
            return;
        }

        r.dl = pw.partWk[r.di].eenv_volume; // .penv;
        soft_env_sub();
        r.carry = false;
        if (r.dl != pw.partWk[r.di].eenv_volume) { // .penv // break sem_ret; // cy=0
            r.carry = true;
        }
//sem_ret:
    }

    private void soft_env_sub() {
        if (pw.partWk[r.di].envf == 0) { // -1 // break se1;

            //
            // Attack
            //
            pw.partWk[r.di].eenv_ar--; // .pat--;
            if (pw.partWk[r.di].eenv_ar != 0) {
//                break se2;
                return;
            }

            pw.partWk[r.di].envf = 1;
            r.al = pw.partWk[r.di].eenv_dr; // pv2[di]
            pw.partWk[r.di].eenv_volume = r.al; // penv[di]
            r.carry = true;
            return;
        }
//se1:
        if (pw.partWk[r.di].envf != 2) { // break se3;

            //
            // Decay
            //
            if (pw.partWk[r.di].eenv_sr == 0) {
//                break se2; //ＤＲ＝０の時は減衰しない
                return;
            }
            pw.partWk[r.di].eenv_sr--;
            if (pw.partWk[r.di].eenv_sr != 0) {
//                break se2;
                return;
            }

            r.al = pw.partWk[r.di].eenv_src; // pr1b[di]
            pw.partWk[r.di].eenv_sr = r.al; // pr1[di]
            pw.partWk[r.di].eenv_volume--; // penv[di]

//se4:
            if (pw.partWk[r.di].eenv_volume < (byte) 0xf1) { // -15 // break se2;
                if (pw.partWk[r.di].eenv_volume >= 15) { // break se2;
//se5:
                    pw.partWk[r.di].eenv_volume = (byte) 0xf1; // mov penv[di],-15
                }
            }
//se2:
            return;
        }
        //
        // Release
        //
//se3:
        if (pw.partWk[r.di].eenv_rr == 0) { // pr2
//            break se5; // ＲＲ＝０の時はすぐに音消し
            pw.partWk[r.di].eenv_volume = (byte) 0xf1; // mov penv[di],-15
            return;
        }
        pw.partWk[r.di].eenv_rr--; // pr2[di]
        if (pw.partWk[r.di].eenv_rr != 0) {
//            break se2;
            return;
        }
        r.al = pw.partWk[r.di].eenv_rrc; // pr2b[di]
        pw.partWk[r.di].eenv_rr = r.al; // pr2[di]
        pw.partWk[r.di].eenv_volume--; // penv[di]
//        break se4;
        if (pw.partWk[r.di].eenv_volume < (byte) 0xf1) { // -15 // break se2;
            if (pw.partWk[r.di].eenv_volume >= 15) { // break se2;
                pw.partWk[r.di].eenv_volume = (byte) 0xf1; // mov penv[di],-15
            }
        }
    }

    // 拡張版
    private void ext_ssgenv_main() {
        r.ah = pw.partWk[r.di].eenv_count;
        if (r.ah == 0) { // break esm_main2;
//esm_ret:
            r.carry = false;
            return; // cy=0
        }
//esm_main2:
        r.dl = pw.partWk[r.di].eenv_volume;
        esm_sub();
        if (r.dl == pw.partWk[r.di].eenv_volume) {
            r.carry = false;
//            break esm_ret; // cy=0
            return;
        }
        r.carry = true;
        return;
    }

    private void esm_sub() {
        //esm_ar_check:;
        r.ah--;
        if (r.ah == 0) { // break esm_dr_check;
            //
            // [[[Attack Rate]]]
            //
            r.al = pw.partWk[r.di].eenv_arc;
            r.al--;
            if ((r.al & 0x80) == 0) { // break arc_count_check; // 0以下の場合はカウントCHECK
                r.al++;
                pw.partWk[r.di].eenv_volume += r.al;
                if (pw.partWk[r.di].eenv_volume < 15) { // break esm_ar_next;
                    r.ah = pw.partWk[r.di].eenv_ar;
                    r.ah -= 16;
                    pw.partWk[r.di].eenv_arc = r.ah;
                    return;
                }
//esm_ar_next:
                pw.partWk[r.di].eenv_volume = 15;
                pw.partWk[r.di].eenv_count++;
                if (pw.partWk[r.di].eenv_sl != 15) // SL=0の場合はすぐSRに
                    return; // break esm_ret;
                pw.partWk[r.di].eenv_count++;
                return;
            }
//arc_count_check:
            if (pw.partWk[r.di].eenv_ar == 0) // AR=0?
                return; // break esm_ret;
            pw.partWk[r.di].eenv_arc++;
            return;
        }
//esm_dr_check:
        r.ah--;
        if (r.ah == 0) { // break esm_sr_check;
            //
            // [[[Decay Rate]]]
            //
            r.al = pw.partWk[r.di].eenv_drc;
            r.al--;
            if ((r.al & 0x80) == 0) { // break drc_count_check; // 0以下の場合はカウントCHECK
                r.al++;
                r.carry = pw.partWk[r.di].eenv_volume < r.al;
                pw.partWk[r.di].eenv_volume -= r.al;
                r.al = pw.partWk[r.di].eenv_sl;
                if (!r.carry) { // break dr_slset;
                    if (pw.partWk[r.di].eenv_volume >= r.al) { // break dr_slset;
                        r.ah = pw.partWk[r.di].eenv_dr;
                        r.ah -= 16;
                        if ((r.ah & 0x80) != 0) { // break esm_dr_notx;
                            r.ah += r.ah;
                        }
//esm_dr_notx:
                        pw.partWk[r.di].eenv_drc = r.ah;
                        return;
                    }
                }
//dr_slset:
                pw.partWk[r.di].eenv_volume = r.al;
                pw.partWk[r.di].eenv_count++;
                return;
            }
//drc_count_check:
            if (pw.partWk[r.di].eenv_dr == 0) // DR=0?
                return; // break esm_ret;
            pw.partWk[r.di].eenv_drc++;
            return;
        }
//esm_sr_check:
        r.ah--;
        if (r.ah == 0) { // break esm_rr;
            //
            // [[[Sustain Rate]]]
            //
            r.al = pw.partWk[r.di].eenv_src;
            r.al--;
            if ((r.al & 0x80) == 0) { // break src_count_check; // 0以下の場合はカウントCHECK
                r.al++;
                r.carry = pw.partWk[r.di].eenv_volume < r.al;
                pw.partWk[r.di].eenv_volume -= r.al;
                if (r.carry) { // break esm_sr_exit;
                    pw.partWk[r.di].eenv_volume = 0;
                }
//esm_sr_exit:
                r.ah = pw.partWk[r.di].eenv_sr;
                r.ah -= 16;
                if ((r.ah & 0x80) != 0) { // break esm_sr_notx;
                    r.ah += r.ah;
                }
//esm_sr_notx:
                pw.partWk[r.di].eenv_src = r.ah;
                return;
            }
//src_count_check:
            if (pw.partWk[r.di].eenv_sr == 0) // SR=0?
                return; // break esm_ret;
            pw.partWk[r.di].eenv_src++;
            return;
        }
//esm_rr:
        //
        // [[[Release Rate]]]
        //
        r.al = pw.partWk[r.di].eenv_rrc;
        r.al--;
        if ((r.al & 0x80) == 0) { // break rrc_count_check; // 0以下の場合はカウントCHECK
            r.al++;
            r.carry = pw.partWk[r.di].eenv_volume < r.al;
            pw.partWk[r.di].eenv_volume -= r.al;
            if (r.carry) { // break esm_rr_exit;
                pw.partWk[r.di].eenv_volume = 0;
            }
//esm_rr_exit:
            r.ah = pw.partWk[r.di].eenv_rr;
            r.ah += r.ah;
            r.ah -= 16;
            pw.partWk[r.di].eenv_rrc = r.ah;
            return;
        }
//rrc_count_check:
        if (pw.partWk[r.di].eenv_rr == 0) // RR=0?
            return; // break esm_ret;
        pw.partWk[r.di].eenv_rrc++;
    }

    //==============================================================================
    // FADE IN / OUT ROUTINE
    //
    //  FROM Timer-A
    //==============================================================================
    private void fadeout() {
        if (pw.pause_flag == 1) { // pause中はfadeoutしない
//            break fade_exit;
            return; // <<
        }
        r.al = pw.fadeout_speed;
        if (r.al == 0) {
//            break fade_exit;
            return; // <<
        }
        if ((r.al & 0x80) == 0) { // break fade_in;

            r.carry = (r.al + pw.fadeout_volume > 0xff);
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
        r.carry = (r.al + pw.fadeout_volume > 0xff);
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

    //==============================================================================
    // インタラプト 設定
    // FM音源専用
    //==============================================================================
    private void setint() {
        //pushf
        //cli; 割り込み禁止
        //
        // ＯＰＮ割り込み初期設定
        //
        pw.tempo_d = (byte) 200; // TIMER B SET
        pw.tempo_d_push = (byte) 200;

        calc_tb_tempo();
        settempo_b();

        r.setDx((short) 0x2500);
        opnset44();
        r.setDx((short) 0x2400); // TIMER A SET(9216μs固定)

        opnset44(); // 一番遅くて丁度いい

        r.dh = 0x27;
        r.dl = 0b0011_1111; // TIMER ENABLE

        opnset44();

        //    popf

        //
        //　小節カウンタリセット
        //
        r.setAx((short) 0);
        pw.opncount = r.al;
        pw.syousetu = r.getAx();
        pw.syousetu_lng = 96;
    }

    //==============================================================================
    // ALL SILENCE
    //==============================================================================
    private void silence() {
        if (pw.board2 != 0) {
            sel44(); // mmainには飛ばない状況下なので大丈夫
            r.ah = 2;
        }
        oploop();
    }

    private void oploop() {
        byte[] bxTbl = null;
        if (pw.fm_effec_flag == 1) { // break opi_nef;
            if (pw.board2 != 0) {
                if (r.ah != 1) {
//                    break opi_nef;
                    bxTbl = pw.fmoff_nef; // <<
                    r.setBx((short) 0); //offset fmoff_nef // <<
                }
            } else {

                bxTbl = pw.fmoff_ef;
                r.setBx((short) 0); // offset fmoff_ef
//            break opi_ef;
            }
        } else {
//opi_nef:
            bxTbl = pw.fmoff_nef;
            r.setBx((short) 0); //offset fmoff_nef
        }
//opi_ef:

//opi0:
        while (true) {
            r.dh = bxTbl[r.getBx()];
            r.incBx();
            if (r.dh == (byte) 0xff) break; // opi1b;

            r.dh += 0x80;
            r.dl = (byte) 0xff; // FM Release = 15
            opnset();
//            break opi0;
        }
//opi1b:
        if (pw.board2 != 0) {
            r.stack.push(r.getAx());
            sel46(); // mmainには飛ばない状況下なので大丈夫
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
        } while (r.getCx() > 0);

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
            } while (r.getCx() > 0);
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
            r.dl |= 0b1001_1011;
            r.dh = 0x7;
            opnset44();
            //    popf
        }
//s_pcm:
pcm_ef: // ↑
        if (pw.board2 != 0) {
            if (pw.pcmflag == 0) { // break pcm_ef; // PCM効果音発声中か？
                if (pw.adpcm != 0) {
                    if (pw.ademu == 0) {
                        if (pw.pcm_gs_flag == 1) break pcm_ef;
                        r.setDx((short) 0x0102); // PAN=0 / x8 bit mode
                        opnset46();
                        r.setDx((short) 0x0001); // PCM RESET
                        opnset46();
                    }
                }
                r.setDx((short) 0x1080); // TA/TB/EOS を RESET
                opnset46();
                r.setDx((short) 0x1018); // TIMERB/A/EOSのみbit変化あり
                opnset46(); // (NEC音源でも一応実行)
                if (pw.pcm != 0) {
                    pcmdrv86.stop_86pcm();
                }
            }
//pcm_ef:
            if (pw.ppz != 0) {
                if (pw.ppz_call_seg != 0) {
                    r.ah = 0x12;
                    ChipDatum cd = new ChipDatum(0x12, 0, 0);
                    ppz8em.apply(cd); // .StopInterrupt(); // FIFO割り込み停止
                    r.setAx((short) 0x0200);
//ppz_off_loop:
                    do {
                        r.stack.push(r.getAx());
                        cd = new ChipDatum(0x02, r.al, 0);
                        ppz8em.apply(cd); // .StopPCM(r.al); // ppz keyoff
                        r.setAx(r.stack.pop());
                        r.al++;
                    } while (r.al < 8); // break ppz_off_loop;
                }
//_not_ppz8:
            }
        }
    }


    //6166-6248
    //==============================================================================
    // SET DATA TO OPN
    // INPUTS ---- D,E
    //==============================================================================
    //
    // 表
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
        pc98.OutPort(r.getDx(), r.al);
        _waitP();
        r.setDx((short) pw.fm1_port2);
        r.al = r.bl;
        pc98.OutPort(r.getDx(), r.al);

        //    popf

        r.setBx(r.stack.pop());
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //
    // 裏
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
            pc98.OutPort(r.getDx(), r.al);
            _waitP();
            r.setDx((short) pw.fm2_port2);
            r.al = r.bl;
            pc98.OutPort(r.getDx(), r.al);

            //    popf

            r.setDx(r.stack.pop());
            r.setBx(r.stack.pop());
            r.setAx(r.stack.pop());
        }
    }

    //
    // 表／裏
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
        pc98.OutPort(r.getDx(), r.al);
        _waitP();
        r.setDx(pw.fm_port2);
        r.al = r.bl;
        pc98.OutPort(r.getDx(), r.al);

        //    popf

        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //==============================================================================
    // READ PSG 07H Port
    // cliしてから来ること
    //==============================================================================
    public void get07() {
        r.stack.push(r.getDx());
        r.setDx((short) pw.fm1_port1);
        rdychk();
        r.al = 7;
        pc98.OutPort(r.getDx(), r.al);
        _waitP(); // ; PSG Read Wait
        r.setDx((short) pw.fm1_port2);
        r.al = pc98.InPort(r.getDx());
        r.setDx(r.stack.pop());
    }

    //==============================================================================
    // ＩＮＴ６０Ｈのメイン
    //==============================================================================
    private void int60_start() {
        // TimerA/B 再入check

        if ((reint_chk[r.ah] & 1) != 0) {
            if (pw.TimerBflag != 0) {
                int60_error();
                return;
            }
        }
        if ((reint_chk[r.ah] & 2) != 0) {
            if (pw.TimerAflag != 0) {
                int60_error();
                return;
            }
        }
        if ((reint_chk[r.ah] & 4) != 0) {
            if (pw.int60flag != 1) {
                int60_error();
                return;
            }
        }

        if (r.ah != 0xf) int60_jumptable[r.ah].run();
        else {
            //KUMA: 注意)外部スレッドから音源をアクセスしないようにする必要があります
            //KUMA:      どうしても必要な場合は本スレッドを止めてからにしてください。
            if (pw.board2 != 0) int60_jumptable[r.ah].run();
                else nothing();
        }

        r.al = pw.al_push;
        r.ah = pw.ah_push;
        r.setDx(pw.dx_push);
        int60_exit();
        return;
    }

    // 再入check用code / bit0=TimerBint 1=TimerAint 2=INT60
    private byte[] reint_chk = {
            4, 4, 0, 6, 6, 0, 0, 0, 0, 0, 0, 0, 7, 7, 0, 5,
            0, 0, 0, 0, 0, 0, 0, 0, 7, 0, 5, 5, 7, 0, 7, 0,
            0, 0
    };

    private Runnable[] int60_jumptable;

    private void Set_int60_jumptable() {
        int60_jumptable = new Runnable[] {
                this::mstart_f, // 0
                this::mstop_f, // 1
                this::fout, // 2  in al:fadeout_speed
                null, // eff_on, // 3
                null, // effoff, // 4
                this::get_ss, // 5 out ax:小節数
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
                //this::get_fmefc_num, // 11
                //this::get_pcmefc_num, // 12
                //this::set_fm_int, // 13
                //this::set_efc_int, // 14
                //this::get_psgefcnum, // 15
                //this::get_joy, // 16
                //this::get_ppsdrv_flag, // 17
                //this::set_ppsdrv_flag, // 18
                //this::set_fv, // 19
                //this::pause_on, // 1A
                //this::pause_off, // 1B
                //this::ff_music, // 1C
                //this::get_memo, // 1D
                //this::part_mask, // 1E
                //this::get_fm_int, // 1F
                //this::get_efc_int, // 20
                //this::get_mus_name, // 21
                //this::get_size // 22
        };
    }

    private int int60_max = 0x22;

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
        r.ah = (byte) pw.vers;
        r.al = (byte) pw.verc;
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


    //6488-6493
    private void get_workadr() {
        r.setAx((short) 0); // r.cs;
        pw.ds_push = r.getAx();
        pw.dx_push = 0; // offset part_data_table
    }


    //6738-6789
    //==============================================================================
    // メモ文字列の取り出し
    //==============================================================================
    public short get_memo(int al) {
        try {
getmemo_errret: // ↑
            {
                r.al = (byte) al;
                r.setSi((short) pw.mmlbuf);
                if (pw.md[r.getSi()].dat != 0x1a)
                    break getmemo_errret; // 音色がないfile=メモのアドレス取得不能
                r.addSi((short) 0x18);
                r.setSi((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
                r.addSi((short) pw.mmlbuf);
                r.subSi((short) 4);
                r.setBx((short) (pw.md[r.getSi() + 2].dat + pw.md[r.getSi() + 3].dat * 0x100)); // bh=0feh,bl=ver
                if (r.bl != 0x40) { // Ver4.0 & 00Hの場合 // break getmemo_exec;
                    if (r.bh != (byte) 0xfe)
                        break getmemo_errret; // Ver.4.1以降は 0feh
                    if (r.bl < 0x41)
                        break getmemo_errret; // MC version 4.1以前だったらError
                }
//getmemo_exec:
                if ((r.bl & 0xff) >= 0x42) { // Ver.4.2以降か？ // break getmemo_oldver41;
                    r.al++; // ならalを +1 (0FFHで#PPSFile)
                }
//getmemo_oldver41:
                if (r.bl >= 0x48) { // Ver.4.8以降か？ // break getmemo_oldver47;
                    r.al++; // ならalを +1 (0FEHで#PPZFile)
                }
//getmemo_oldver47:
                r.setSi((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
                r.addSi((short) pw.mmlbuf);
                r.al++;
//getmemo_loop:
                do {
                    r.setDx((short) (pw.md[r.getSi() + 0].dat + pw.md[r.getSi() + 1].dat * 0x100));
                    if (r.getDx() == 0)
                        break getmemo_errret;
                    r.addSi((short) 2);
                    r.al--;
                } while (r.al != 0); // break getmemo_loop;
//getmemo_exit:
                r.addDx((short) pw.mmlbuf);
                pw.ds_push = 0; // r.cs; セグメントなし
                pw.dx_push = r.getDx();
                return r.getDx();
            }
//getmemo_errret:
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "メモのアドレスが範囲外を指していることを検出しました。無視します。");
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        }
    }

    //==============================================================================
    // 曲の頭だし
    //  input DX<- 小節番号
    // output AL<- return code   0:正常終了
    //      1:その小節まで曲がない
    //      2:曲が演奏されていない
    //==============================================================================
    private void ff_music() {
        r.stack.push(r.getDx());
        r.setDx(pw.mask_adr);
        //pushf
        //cli
        r.al = pc98.InPort(r.getDx());
        r.al |= pw.mask_data;
        pc98.OutPort(r.getDx(), r.al); // FM割り込みを禁止
        //popf
        r.setDx(r.stack.pop());
        ff_music_main();
        pw.al_push = r.al;
        r.setDx(pw.mask_adr);
        //pushf
        //cli
        r.al = pc98.InPort(r.getDx());
        r.al &= pw.mask_data2;
        pc98.OutPort(r.getDx(), r.al); // FM割り込みを許可
        //    popf
        return;
    }

    private void ff_music_main() {
        if (pw.status2 != (byte) 255) { // break ffm_exit2;
            pw.skip_flag = 1;
            if (r.getDx() < pw.syousetu) { // cmp dx,[syousetu] // break ffm_main;
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
                    r.dl--; // ffより1少ないtempo
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
                opnset44(); // Rhythm音源を全部Dump
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
            } while (r.bp >= pw.syousetu); // break ffm_loop;
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
            r.dl--; // ffより1少ないtempo
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

    //==============================================================================
    // 全パート一時マスク
    //==============================================================================
    private void maskon_all() {
        r.setSi((short) 0); // offset part_table
        r.setCx((short) pw.max_part1);
        r.di = (short) pw.part1;

//maskon_loop:
        do {
            if (pw.ppz != 0) {
                r.al = (byte) pw.part_table[r.incSi()];
                if (r.al == (byte) 0xff) { // -1 // break monl_main;
                    r.addSi((short) 6); // skip Rhythm & Effects(for PPZ parts)
                }
//monl_main:
            } else {
                r.incSi();
            }

            r.al = (byte) pw.part_table[r.incSi()]; //    lodsw; ah=音源 al = partb
            r.ah = (byte) pw.part_table[r.incSi()];

            pw.partWk[r.di].partmask |= (byte) 0x80;
            if (pw.partWk[r.di].partmask == 0x80) { // break maskon_next; // 既に他でマスクされてた

                r.stack.push(r.getCx());
                r.stack.push(r.di);
                r.stack.push(r.getSi());
                maskon_main(); // 1パートマスク
                r.setSi(r.stack.pop());
                r.di = r.stack.pop();
                r.setCx(r.stack.pop());
            }
//maskon_next:
            r.di += 1; // qq
            r.decCx();
        } while (r.getCx() != 0); // break maskon_loop;
    }

    //==============================================================================
    // 全パート一時マスク解除
    //==============================================================================
    private void maskoff_all() {
        r.setSi((short) 0); // offset part_table
        r.setCx((short) pw.max_part1);
        r.di = (short) pw.part1;
//maskoff_loop:
        do {
            if (pw.ppz != 0) {
                r.al = (byte) pw.part_table[r.incSi()];
                if (r.al == (byte) 0xff) { // -1 // break moffl_main;
                    r.addSi((short) 6); // skip Rhythm & Effects(for PPZ parts)
                }
//moffl_main:
            } else {
                r.incSi();
            }

            r.al = (byte) pw.part_table[r.incSi()]; //    lodsw; ah=音源 al = partb
            r.ah = (byte) pw.part_table[r.incSi()];

            pw.partWk[r.di].partmask &= 0x7f;
            if (pw.partWk[r.di].partmask == 0) { // break maskoff_next; // まだ他でマスクされてる

                r.stack.push(r.getCx());
                r.stack.push(r.di);
                r.stack.push(r.getSi());
                maskoff_main(); // 1パート復帰
                r.setSi(r.stack.pop());
                r.di = r.stack.pop();
                r.setCx(r.stack.pop());
            }
//maskoff_next:
            r.di += 1; // qq
            r.decCx();
        } while (r.getCx() != 0); // break maskoff_loop;
    }

    //==============================================================================
    // パートのマスク & Keyoff
    //==============================================================================
    private void part_mask() {
        r.ah = r.al;
        r.ah &= 0x7f;
        if (pw.ppz != 0) {
            r.carry = (r.ah < 16 + 8);
        } else {
            r.carry = (r.ah < 16);
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
        r.dl = (byte) pw.part_table[r.getBx()]; // dl<- Part番号
        if ((r.dl & 0x80) != 0) {
            rhythm_mask();
            return;
        }
        r.incBx();
        r.al = (byte) pw.part_table[r.getBx() + 0]; // AH=音源 AL = partb
        r.ah = (byte) pw.part_table[r.getBx() + 1];
        r.bh = 0;
        r.bl = r.dl;
        //r.bx += r.bx;
        r.setBx((short) 0); // offset part_data_table
        r.di = (short) pw.part_data_table[r.getBx()];
        r.dl = pw.partWk[r.di].partmask;
        pw.partWk[r.di].partmask |= 1;
        if (r.dl != 0)
            return; // break pm_ret; // ; 既にマスクされていた
        if (pw.play_flag == 0)
            return; // break pm_ret; // ; 曲が止まっている

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
                                //pm_ret:;
                                return;
                            }
//pm_fm1:
                            //pushf
                            //cli
                            pw.partb = r.al;
                            if (pw.board2 != 0) {
                                sel44();
                            }
                            silence_fmpart(); // 音を完璧に消す
                            //popf
                            return;
                        }
//pm_fm2:
                        if (pw.board2 != 0) {
                            //pushf
                            //cli
                            pw.partb = r.al;
                            sel46();
                            silence_fmpart(); // 音を完璧に消す
                            //popf
                            return;
                        }
                    }
//pm_drums:
                    if (pw.psgefcnum >= 11) {
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
                            ChipDatum cd = new ChipDatum(0x02, r.al, 0);
                            ppz8em.apply(cd); // .StopPCM(r.al); // PPZ8 ch7 発音停止
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
            ChipDatum cd = new ChipDatum(0x02, r.al, 0);
            ppz8em.apply(cd); // .StopPCM(r.al); // ; ppz stop(al= partb)
//pmppz_noexec:
        }
    }

    private void rhythm_mask() {
        pw.rhythmmask = 0; // Rhythm音源をMask
        if (pw.board2 != 0) {
            r.setDx((short) 0x10ff);
            opnset44(); // Rhythm音源を全部Dump
        }
    }

    //==============================================================================
    // パートのマスク解除 & FM音源音色設定 in.AH=part番号
    //==============================================================================
    private void part_on() {
        r.bh = 0;
        r.bl = r.ah;
        r.bl += r.bl;
        r.bl += r.ah;
        r.addBx((short) 0); // offset part_table
        r.dl = (byte) pw.part_table[r.getBx()]; // dl<- Part番号
        if ((r.dl & 0x80) != 0) {
            rhythm_on();
            return;
        }
        r.incBx();
        r.al = (byte) pw.part_table[r.getBx() + 0]; // AH=音源 AL = partb
        r.ah = (byte) pw.part_table[r.getBx() + 1];
        r.bh = 0;
        r.bl = r.dl;
        //r.bx += r.bx;
        r.addBx((short) 0); // offset part_data_table
        r.di = (short) pw.part_data_table[r.getBx()];
        if (pw.partWk[r.di].partmask == 0)
            return; // break po_ret; // ; マスクされてない
        pw.partWk[r.di].partmask &= 0xfe;
        if (pw.partWk[r.di].partmask != 0)
            return; // break po_ret; // 効果音でまだマスクされている
        if (pw.play_flag == 0)
            return; // break po_ret; // ; 曲が止まっている
        maskoff_main();
    }

    private void maskoff_main() {
        if (r.ah != 0) { // break po_fm1; // FM音源の場合は
            if (pw.board2 != 0) {
                r.ah--;
                if (r.ah != 0) {
                    // break po_fm2; // 音色設定処理
                } else {
                    return; // <<
                }
            } else {
//po_ret:
                return;
            }
        } else {
//po_fm1:
            r.dl = pw.partWk[r.di].voicenum;
            //pushf
            //cli
            pw.partb = r.al;
            if (pw.board2 != 0) {
                sel44();
            }
            if (pw.partWk[r.di].address != 0) { //                 break pof1_not_set;
                neiro_reset();
            }
//pof1_not_set:
            //popf
            return;
        }
//po_fm2:
        if (pw.board2 != 0) {
            r.dl = pw.partWk[r.di].voicenum;
            //pushf
            //cli
            pw.partb = r.al;
            sel46();
            if (pw.partWk[r.di].address != 0) { // break pof2_not_set;
                neiro_reset();
            }
pof2_not_set:
            //popf
            return;
        }
    }

    private void rhythm_on() {
        pw.rhythmmask = (byte) 0xff; // Rhythm音源をMask解除
        return;
    }

    //==============================================================================
    // ボードがない時
    //==============================================================================
    private void int60_start_not_board() {
        n_int60_jumptable[r.ah].run();

        r.al = pw.al_push;
        r.ah = pw.ah_push;
        r.setDx(pw.dx_push);
        int60_exit();
        return;
    }

    private Runnable[] n_int60_jumptable;

    private void Set_n_int60_jumptable() {
        n_int60_jumptable = new Runnable[] {
                // nothing // 0
                //,nothing // 1
                //,nothing // 2
                //,nothing // 3
                //,nothing // 4
                //,get_255 // 5
                //,get_musdat_adr // 6
                //,get_tondat_adr // 7
                //,get_255 // 8
                //,drv_chk2 // 9
                //,get_65535 // A
                //,get_efcdat_adr // B
                //,nothing  //C
                //,nothing  //D
                //,get_pcm_adr //E
                //,nothing  //F
                //,get_workadr //10
                //,get_255 // 11
                //,get_255 // 12
                //,nothing // 13
                //,nothing // 14
                //,get_65535 // 15
                //,get_65535 // 16
                //,get_255 // 17
                //,nothing // 18
                //,nothing // 19
                //,nothing // 1A
                //,nothing  //1B
                //,nothing  //1C
                //,get_memo //1D
                //,nothing // 1E
                //,get_fm_int // 1F
                //,get_efc_int // 20
                //,get_mus_name // 21
                //,get_size // 22
        };
    }

    private void get_255() {
        pw.al_push = (byte) 255;
    }

    //7333-7334
    private void nothing() {
    }

    private void get_65535() {
        pw.ah_push = (byte) 255;
        get_255();
    }

    //==============================================================================
    // FM効果音ルーチン
    //==============================================================================
    //==============================================================================
    // 発音
    //  input AL to number_of_data
    //==============================================================================
    private void fm_effect_on() {
        if (pw.efcdat == -1) return; // KUMA: 将来効果音使うときまで封印

        //pushf
        //cli
        if (pw.fm_effec_flag != 0) { // break not_e_flag;

            r.stack.push(r.getAx());
            fm_effect_off();
            r.setAx(r.stack.pop());
        }
//not_e_flag:
        pw.fm_effec_num = r.al;
        pw.fm_effec_flag = 1; // 効果音発声してね
        pw.partb = 3;
        if (pw.board2 == 0) {
            r.di = (short) pw.part3; // offset part3
            pw.partWk[r.di].partmask |= 2; // Part Mask
            r.di = (short) pw.part3b; // offset part3b
            pw.partWk[r.di].partmask |= 2; // Part Mask
            r.di = (short) pw.part3c; // offset part3c
            pw.partWk[r.di].partmask |= 2; // Part Mask
            r.di = (short) pw.part3d; // offset part3d
            pw.partWk[r.di].partmask |= 2; // Part Mask
        } else {
            r.di = (short) pw.part6; // offset part6
            pw.partWk[r.di].partmask |= 2; // Part Mask
        }
        r.bh = 0;
        r.bl = pw.fm_effec_num; // bx = effect no.
        r.di = (short) pw.part_e; // offset part_e
        r.al = 0;
        pw.partWk[r.di].Clear(); // PartData 初期化
        r.addBx(r.getBx());
        r.addBx((short) pw.efcdat);
        r.setAx((short) (pw.md[r.getBx()].dat + pw.md[r.getBx() + 1].dat * 0x100));
        r.addAx((short) pw.efcdat);
        r.di = (short) pw.part_e;
        pw.partWk[r.di].address = r.getAx(); // アドレスのセット
        pw.partWk[r.di].leng = 1; // あと1カウントで演奏開始
        pw.partWk[r.di].volume = 108; // FM VOLUME DEFAULT= 108
        pw.partWk[r.di].slotmask = (byte) 0xf0; // FM SLOTMASK
        pw.partWk[r.di].neiromask = (byte) 0xff; // FM Neiro MASK
        if (pw.board2 != 0) {
            r.dl = (byte) 0xc0;
            pw.partWk[r.di].fmpan = r.dl; // FM PAN = Middle
            r.dh = (byte) 0xb6;
            sel46(); // ここでmmainが来てもsel46のまま
            opnset();
        } else {
            r.al = pw.ch3mode;
            pw.ch3mode_push = r.al;
            pw.ch3mode = 0x3f;
        }

        //popf
        //ret
    }

    //==============================================================================
    // 消音
    //==============================================================================
    private void fm_effect_off() {
        //pushf
        //cli
        if (pw.fm_effec_flag != 0) { // break feo_ret;

            pw.fm_effec_num = (byte) 0xff; // -1;
            pw.fm_effec_flag = 0; // 効果音止めてね
            if (pw.board2 != 0) {
                sel46(); // ここでmmainが来てもsel46のまま
            }
            r.di = (short) pw.part_e;
            pw.partb = 3;
            silence_fmpart();
            if (pw.play_flag != 0) { // break feo_ret; // 曲が止まっている

                if (pw.board2 != 0) {
                    r.di = (short) pw.part6;
                    r.dl = pw.partWk[r.di].voicenum;
                    neiro_reset();
                } else {
                    r.di = (short) pw.part3;
                    r.dl = pw.partWk[r.di].voicenum;
                    neiro_reset();

                    r.di = (short) pw.part3b;
                    r.dl = pw.partWk[r.di].voicenum;
                    neiro_reset();

                    r.di = (short) pw.part3c;
                    r.dl = pw.partWk[r.di].voicenum;
                    neiro_reset();


                    r.di = (short) pw.part3d;
                    r.dl = pw.partWk[r.di].voicenum;
                    neiro_reset();

                    r.al = pw.ch3mode_push;
                    pw.ch3mode = r.al;
                    r.dh = 0x27;
                    r.dl = r.al;
                    r.dl &= 0b1100_1111; // Resetはしない
                    opnset44();
                }
            }
        }
//feo_ret:
        // popf
    }

    //==============================================================================
    // FM TimerA/B 処理 Main
    //  *Timerが来ている事を確認してから飛んで来ること。
    //   pushしてあるレジスタは ax/dx/ds のみ。
    //==============================================================================
    private void FM_Timer_main() {
        //push cx
        //------------------------------------------------------------------------------
        // Timer Reset
        // 同時にFM割り込み Timer AorB どちらが来たかを読み取る
        //------------------------------------------------------------------------------
        r.setDx((short) pw.fm1_port1);
        rdychk();
        r.al = 0x27;
        pc98.OutPort(r.getDx(), r.al);
        _wait();
        r.ah = pw.ch3mode; // ah = 27hに出力する値
        r.al = (byte) pw.timer.getStatReg(); // pc98.InPort(r.dx); // rdychk ;al = status
        byte a = r.ah;
        r.ah = r.al;
        r.al = a; // ah = status / al=27hに出力する値

        r.setDx((short) pw.fm1_port2);
        pc98.OutPort(r.getDx(), r.al); // Timer Reset

        //r.ah = (byte)(pw.timer.StatReg & 3); // ah = TimerA/B flag

        //------------------------------------------------------------------------------
        // 割り込み許可
        //------------------------------------------------------------------------------
        if (pw.disint != 1) { // break not_sti;
            //sti
        }
//not_sti:

        //------------------------------------------------------------------------------
        // どちらが来たかで場合分け処理
        //------------------------------------------------------------------------------

        r.ah--; // Timer Aか？
        if (r.ah == 0) { // break TimerA_int; // Timer Aの方を処理
//TimerA_int:
            TimerA_main();
//exit_Timer:
            return;
        }

        r.ah--; // Timer Bか？
        if (r.ah == 0) { // break TimerB_int; // Timer Bの方を処理
//TimerB_int:
            TimerB_main();
//exit_Timer:
            return;
        }

        TimerB_main(); // 同時
        TimerA_main();

        //    cli
    }

    //==============================================================================
    // TimerBの処理[メイン]
    //==============================================================================
    private void TimerB_main() {
        if (pw.sync != 0) return;
        opnint_sub();
    }

    private void opnint_sub() {
        pw.TimerBflag = 1;
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
            r.al = pw.TimerAtime;
            pw.lastTimerAtime = r.al;
        }
not_play:
        pw.TimerBflag = 0;
        if ((pw.intfook_flag & 1) != 0) { // break TimerB_nojump;
            //    call dword ptr[fmint_ofs]
        }
//TimerB_nojump:
    }

    //==============================================================================
    // TimerAの処理[メイン]
    //==============================================================================
    private void TimerA_main() {
        pw.TimerAflag = 1;
        pw.TimerAtime++;
        r.al = pw.TimerAtime;
        r.al &= 7;
        if (r.al == 0) { // break not_fade;
            fadeout(); // Fadeout処理
            rew(); // Rew処理
        }
//not_fade:
        if (pw.effon != 0) { // break not_psgeffec;
            if (pw.ppsdrv_flag != 0) { // break ta_not_ppsdrv;
                if ((pw.psgefcnum & 0x80) == 0) { // break not_psgeffec; // ppsdrvの方で鳴らしている
                } else {
                    efcdrv.effplay(); // SSG効果音処理 // <<
                }
            } else {
//ta_not_ppsdrv:
                efcdrv.effplay(); // SSG効果音処理
            }
        }
//not_psgeffec:
        if (pw.fm_effec_flag != 0) { // break not_fmeffec;
            fm_efcplay(); // FM効果音処理
        }
//not_fmeffec:
vtc000: // ↑
        {
            if (pw.key_check == 0)
                break vtc000;
            if (pw.play_flag == 0)
                break vtc000;
            if (pw.va != 0) {
                r.al = pc98.InPort(8);
                r.ah = pw.esc_sp_key;
                if ((r.ah & r.al) != 0)
                    break vtc000;
                r.al = pc98.InPort(9);
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

            pw.music_flag |= 2; // 次のTimerBでMSTOP
            pw.fadeout_flag = 0; // CTRL+ESCで止めた=外部扱い
        }
//vtc000:
        pw.TimerAflag = 0;
        if ((pw.intfook_flag & 2) != 0) { // break TimerA_nojump;

            //TBD
            //pw.efcint_ofs(); //dword ptr[efcint_ofs]
        }
//TimerA_nojump:
    }

    //==============================================================================
    // 小節のカウント
    //==============================================================================
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

    //==============================================================================
    // テンポ設定
    //==============================================================================
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
        if (r.dl == pw.TimerB_speed) return;

        pw.TimerB_speed = r.dl;
        r.dh = 0x26;
        opnset44();
        return;
        //stb_ret:
    }

    //==============================================================================
    // 巻き戻し処理
    //==============================================================================
    private void rew() {
        r.ah = pw.rew_sp_key;
        check_grph();
        if (r.carry) { // break rew_ret;
            r.setDx(pw.syousetu);
            r.al = pw.syousetu_lng;
            r.al = (byte) (r.al >> 1);
            r.al = (byte) (r.al >> 1);
            if (pw.opncount >= r.al) {
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
                return;
            }
        }
//rew_ret:
    }

    //==============================================================================
    // GRPH key check
    //  in AH sp_key
    //  out CY 1で押されている
    //==============================================================================
    private void check_grph() {
        if (pw.key_check == 0) // cy=0
            return;
        //cgr_main:
        r.carry = pc98.GetGraphKey();
    }

    private void comstart() {
        //==============================================================================
        // ＰＭＤコマンドスタート
        //==============================================================================

        print_mes(pw.mes_title); // タイトル表示

        //==============================================================================
        // ＰＭＤ常駐CHECK
        //==============================================================================
        //略

        //==============================================================================
        // 常駐処理
        //==============================================================================
        //resident_main:
        //==============================================================================
        // オプション初期設定
        //==============================================================================
        r.setAx((short) 0);

        pw.mmldat_lng = (byte) pw.mdata_def; // Default 16K
        pw.voicedat_lng = (byte) pw.voice_def; // Default 8K
        pw.effecdat_lng = (byte) pw.effect_def; // Default 4K
        pw.key_check = (byte) pw.key_def; // Keycheck ON

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
        pw.kp_rhythm_flag = (byte) 0xff; // SSGDRUMでRHYTHM音源を鳴らすか FLAG

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
            pw.partWk[r.di++].Clear();
            r.decCx();
        } while (r.getCx() != 0);

        pw.disint = r.al; // INT Disable FLAG
        pw.rescut_cant = r.al; // 常駐解除禁止 FLAG
        pw.adpcm_wait = r.al; // ADPCM定義速度
        pw.pcm86_vol = r.al; // PCM音量合わせ
        pw._pcm86_vol = r.al; // PCM音量合わせ
        pw.fade_stop_flag = 1; // FADEOUT後MSTOPするか FLAG
        pw.ppsdrv_flag = (byte) 0xff; // PPSDRV FLAG

        if (pw.va != 0) {
            pw.grph_sp_key = (byte) 0x80; // GRPH + CTRL key code
            pw.rew_sp_key = 0x40; // GPPH + SHIFTkey code
            pw.esc_sp_key = (byte) 0x80; // ESC + CTRL key code
        } else {
            pw.grph_sp_key = 0x10; // GRPH + CTRL key code
            pw.rew_sp_key = 0x1; // GPPH + SHIFTkey code
            pw.esc_sp_key = 0x10; // ESC + CTRL key code
            pw.port_sel = (byte) 0xff; // ポート選択 = 自動
        }
        pw.ff_tempo = (byte) 250;
        pw.music_flag = r.al;
        pw.message_flag = 1;

        //==============================================================================
        // FM音源のcheck(INT / PORT選択)
        //==============================================================================

        //TBD

        //==============================================================================
        // オプションを取り込む
        //==============================================================================

        //TBD "PMDOPT=" 検索
        set_option(pw.pmdOption);

        //==============================================================================
        // vmapエリアに"PMD"文字列書込み
        //==============================================================================

        //TBD


        //==============================================================================
        // Memory Check &Init
        //==============================================================================

        //TBD

        //==============================================================================
        // 曲データ，音色データ格納番地を設定
        //==============================================================================

        r.setAx((short) 1); // offset dataarea+1
        pw.mmlbuf = r.getAx();
        r.decAx();

        r.bh = pw.mmldat_lng;
        r.bl = 0;
        r.slBx(2);
        r.addAx(r.getBx());
        pw.tondat = r.getAx();
        r.bh = pw.voicedat_lng;
        r.bl = 0;
        r.slBx(2);
        r.addAx(r.getBx());
        pw.efcdat = r.getAx();
        pw.efcdat = -1; // 効果音は未使用

        Random rnd = new Random();
        pw.seed = (short) rnd.nextInt(0, 0xffff);

        //==============================================================================
        // 効果音 / FMINT / EFCINTを初期化
        //==============================================================================
        r.setAx((short) 0);
        pw.fmint_seg = r.getAx();
        pw.fmint_ofs = r.getAx();
        pw.efcint_seg = r.getAx();
        pw.efcint_ofs = r.getAx();
        pw.intfook_flag = r.al;
        pw.skip_flag = r.al;
        pw.effon = r.al;
        pw.fm_effec_flag = r.al;
        pw.pcmflag = r.al;

        r.al--;

        pw.psgefcnum = r.al;
        pw.fm_effec_num = r.al;
        pw.pcm_effec_num = r.al;

        //==============================================================================
        // 割り込み設定
        //==============================================================================
        if (pw.board != 0) { // break not_set_opnvec;

            //==============================================================================
            // OPN 初期化
            //==============================================================================
            int_init();

            // ------------------------------------------------------------------------------
            // 088 / 188 / 288 / 388(同INT番号のみ) を初期設定
            // ------------------------------------------------------------------------------
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

                r.setCx((short) 1); // KUMA:0x188のみ
                r.setDx((short) 0x188); // KUMA:0x188のみ

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
                                r.al = pc98.InPort(r.getDx());
                                r.ah &= r.al;
                                if ((r.ah & 0x80) == 0)
                                    break opninit_exec;
                                r.decCx();
                            } while (r.getCx() != 0);
                            break opninit_next; // ; 音源無し
                        }
//opninit_exec:
                        //pushf
                        //cli

                        rdychk();
                        r.al = 0xe;
                        pc98.OutPort(r.getDx(), r.al);
                        r.setCx((short) 256);
                        do {
                            r.decCx();
                        } while (r.getCx() != 0);
                        r.addDx((short) 2);
                        r.al = pc98.InPort(r.getDx());

                        //popf

                        r.subDx((short) 2);
                        r.al &= 0xc0;
                        if (r.al == pw.opn_0eh) { // int番号を比較 // break opninit_next; // ; 非一致なら初期化しない

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

            //==============================================================================
            // ＯＰＮ 割り込みベクトル 退避
            //==============================================================================
            //  cli
            r.setAx((short) 0);
            //r.es = r.ax;
            //r.bx = pw.vector;
            r.setBx((short) 0); // les bx, es:[bx]
            pw.int5ofs = r.getBx();
            pw.int5seg = 0; // r.es;

            //==============================================================================
            // ＯＰＮ 割り込みベクトル 設定
            //==============================================================================
            //r.es = r.ax;
            //r.bx = pw.vector;
            //es:[bx] = 0; // offset opnint
            //es:[bx+2] = r.cs;
        }
//not_set_opnvec:

        //==============================================================================
        // INT60 割り込みベクトル 退避
        //==============================================================================
        //cli
        r.setAx((short) 0);
        //r.es = r.ax;
        //r.bx = es:[pmdvector*4];
        pw.int60ofs = r.getBx();
        pw.int60seg = 0; // r.es;

        //==============================================================================
        // INT60 割り込みベクトル 設定
        //==============================================================================
        //r.es = r.ax;
        //es:[pmdvector*4] = 0; // offset int60_head
        //es:[pmdvector*4 + 2] = r.cs;

        //==============================================================================
        // ＯＰＮ割り込み開始
        //==============================================================================
        opnint_start();
        //sti
    }

    //8896-
    private void int_init() {
        //不要?

        pps_chk();
    }

    //8970-9029
    //------------------------------------------------------------------------------
    // ppsdrv/ppz8常駐CHECK
    //------------------------------------------------------------------------------
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
                if (pw.kp_rhythm_flag != 0xff)
                    break ppschk_exit;

                pw.kp_rhythm_flag = 0;
                break ppschk_exit;
            }
//ppschk_01:
            pw.ppsdrv_flag = 0;
            if (pw.kp_rhythm_flag != 0xff)
                break ppschk_exit;

            pw.kp_rhythm_flag = 1;
            break ppschk_exit;
        }
//ppschk_exit:
        if (pw.message_flag != 0) { // break ppschk_end;
            if (pw.ppsdrv_flag == 1) { // break ppschk_end;
                print_mes(pw.mes_ppsdrv);
            }
        }
//ppschk_end:

        if (pw.ppz != 0) {
            ppz8_check();
            if (!r.carry) { // break ppzchk_end;
                pw.ppz_call_seg = 1;
                r.setAx((short) 0x410);
                ChipDatum cd = new ChipDatum(0x04, r.al, 0);
                ppz8em.apply(cd); // .ReadStatus(r.al); // int ppz_vec
                r.ah = (byte) pw.int_level;
                r.ah += 8;
                if (r.al == r.ah) { // break ppzchk_next;
                    //push es
                    r.setAx((short) 0x409);
                    cd = new ChipDatum(0x04, r.al, 0);
                    ppz8em.apply(cd); // .ReadStatus(r.al); // int ppz_vec
                    r.setAx((short) 0); // r.es;
                    // pop es
                    pw.ppz_call_ofs = r.getBx();
                    pw.ppz_call_seg = r.getAx();
                }
//ppzchk_next:
                r.setAx((short) 0x1901);
                cd = new ChipDatum(0x19, 0, r.al);
                ppz8em.apply(cd); // .SetReleaseFlag(r.al); // int ppz_vec; 常駐解除禁止
                if (pw.message_flag == 0) {
                    mask_eoi_set();
                    return;
                }
                print_mes(pw.mes_ppz8);
            }
//ppzchk_end:
        }
    }

    //9030-9079
    //------------------------------------------------------------------------------
    // MASK/EOIの出力先の設定
    //------------------------------------------------------------------------------
    private void mask_eoi_set() {
        //なにもしない
    }

    //9080-9105
    //==============================================================================
    // ppsdrv常駐CHECK
    //==============================================================================
    public void ppsdrv_check() {
        r.carry = !pw.usePPSDRV; // PPSDRV常駐しています！
    }


    //9106-9132
    //==============================================================================
    // ppz8常駐CHECK
    //==============================================================================
    private void ppz8_check() {
        if (pw.ppz != 0) {
            r.carry = false; // PPZ8常駐しています！(TBD)
        }
    }

    //9133-9166
    //==============================================================================
    // ＯＰＮ割り込み許可処理
    //==============================================================================
    private void opnint_start() {
        if (pw.board != 0) { // break not_opnint_start; // ; ボードがない

            //r.ax = r.cs;
            //r.es = r.ax;
            r.di = (short) pw.part1;
            r.setCx((short) pw.max_part1); // max_part1*type qq
            r.al = 0;
            do {
                pw.partWk[r.di++].Clear();
                r.decCx();
            } while (r.getCx() != 0); // Partwork All Reset

            r.al--;
            pw.rhythmmask = (byte) 255; // Rhythm Mask解除
            pw.rhydmy = r.al;// ;R part Dummy用
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
                r.al = pc98.InPort(0x32);
                //jmp $+2
                r.al &= 0x7f;
                pc98.OutPort((short) 0x32, r.al);
            }
            r.setDx((short) 0x2983);
            opnset44();
        }
//not_opnint_start:
    }

    //9167-9189
    //==============================================================================
    // OPN out for 088/188/288/388 INIT用
    //  input ah  reg
    //   al data
    // dx port
    //==============================================================================
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
            pc98.OutPort(r.getDx(), r.al);

            r.setCx((short) 256);
            do {
                r.decCx();
            } while (r.getCx() != 0);

            r.addDx((short) 2);
            a = r.ah;
            r.ah = r.al;
            r.al = a;
            pc98.OutPort(r.getDx(), r.al);

            r.subDx((short) 2);
            //popf
            //ret
        }
    }

    //9856-9894
    private void intset() {
        //不要?
    }

    //9982-10003
    //==============================================================================
    // /D? option
    //==============================================================================
    private void fmvd_set(String op) {
        char c = op.charAt(0);
        int n = 0;
        try {
            n = Integer.parseInt(op.substring(1));
        } catch (NumberFormatException e) {
            logger.log(Level.ERROR, "/D オプションの解析に失敗しました");
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
                logger.log(Level.ERROR, "/D オプションの解析に失敗しました");
                break;
        }
    }

    //10043-10105
    private void keycheck(String op) {
        char c = op.charAt(0);
        int n = 0;
        try {
            n = Integer.parseInt(op.substring(1));
        } catch (NumberFormatException e) {
            logger.log(Level.ERROR, "/K オプションの解析に失敗しました");
        }
        switch (c) {
            case 'G':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= 0b1110_0000;
                }
                pw.grph_sp_key = r.al;
                break;
            case 'R':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= 0b1110_0000;
                }
                pw.rew_sp_key = r.al;
                break;
            case 'E':
                r.al = (byte) n;
                if (pw.va != 0) {
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al = r.ror(r.al, 1);
                    r.al &= 0b1110_0000;
                }
                pw.esc_sp_key = r.al;
                break;
            default:
                logger.log(Level.ERROR, "/K オプションの解析に失敗しました");
                break;
        }
    }

    //==============================================================================
    // オプション処理
    //  input cs:bx option_data
    // ds:si command_line
    // es pmd_segment
    //==============================================================================
    private void set_option(String[] pmdOption) {
        if (pmdOption == null) return;
        for (int i = 0; i < pmdOption.length; i++) {
            String op = pmdOption[i].toUpperCase();
            if (op == null || op.isEmpty()) continue;
            if (op.isEmpty() || (op.charAt(0) != '/' && op.charAt(0) != '-')) continue;

            char c = op.charAt(1); // 1文字目
            switch (c) {
                case 'D': // 音量
                    if (op.length() > 2) fmvd_set(op.substring(2));
                    break;
                case 'N': // ssgドラム
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
                    } catch (NumberFormatException e) { pw.ff_tempo = (byte) 250; }
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
                    logger.log(Level.WARNING, String.format("PMDDotNETは指定のオプションをサポートしません。無視します。(%d)", op));
                    break;
                default:
                    logger.log(Level.ERROR, String.format("オプションの解析に失敗しました。無視します。(%d)", op));
                    break;
            }
        }
    }

    public void WriteDummy(ChipDatum cd) {
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

    public void ExecIDESpecialCommand(MmlDatum md) {
        //Console.WriteLine("%d", md);

        List<Object> obj = md.args;
        MmlDatum mmd = (MmlDatum) obj.get(0);

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = mmd;
        WriteDummy(cd);
    }
}
