package pmd.driver;

import java.util.function.Supplier;

import musicDriverInterface.ChipDatum;
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;


public class PCMDRV {

    private PMD pmd;
    private PW pw;
    private X86Register r;
    private Pc98 pc98;
    private PPZDRV ppzdrv;

    public PCMDRV(PMD pmd, PW pw, X86Register r, Pc98 pc98, PPZDRV ppzdrv) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.ppzdrv = ppzdrv;

        SetupCmdtbl();
    }

    //==============================================================================
    //	ＰＣＭ音源 演奏 メイン
    //==============================================================================
    //pcmmain_ret:
    //	ret
    public void pcmmain() {
        r.setSi(pw.partWk[r.di].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0)
            return; // break pcmmain_ret;

        //if (r.si == pw.jumpIndex) pw.jumpIndex = -1; // KUMA:Added

        Supplier<Object> ret = null;
        if (pw.partWk[r.di].partmask != 0)
            ret = this::pcmmain_nonplay;
        else
            ret = this::pcmmain_c_1;

        if (ret != null) {
            do {
                ret = (Supplier<Object>) ret.get();
            } while (ret != null);
        }
    }

    private Supplier<Object> pcmmain_c_1() {
        // 音長 - 1
        pw.partWk[r.di].leng--;
        r.al = pw.partWk[r.di].leng;

        // KEYOFF CHECK
        if ((pw.partWk[r.di].keyoff_flag & 3) == 0) { // break mp0m; // 既にkeyoffしたか？
            if (r.al <= pw.partWk[r.di].qdat) { // break mp0m; // Q値 => 残りLength値時 keyoff
                pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
                keyoffm(); // ALは壊さない
            }
        }
//mp0m:
        // LENGTH CHECK
        if (r.al != 0) return this::mpexitm;
        return this::mp1m0;
    }

    private Supplier<Object> mp1m0() {
        pw.partWk[r.di].lfoswi &= 0xf7; // Porta off
        return this::mp1m;
    }

    private Supplier<Object> mp1m() { // DATA READ
        do {
            pw.cmd = pw.md[r.getSi()];
            r.al = (byte) pw.md[r.getSi()].dat;

            //if (r.si == pw.jumpIndex)
            //pw.jumpIndex = -1; // KUMA:Added

            r.si++;

            if (r.al < 0x80) break mp2m;
            if (r.al == 0x80) break mp15m;

            // ELSE COMMANDS
            Object o = commandsm();
            while (o != null && (Supplier<Object>) o != this::mp1m) {
                o = ((Supplier<Object>) o).get();
                if ((Supplier<Object>) o == pmd::mnp_ret)
                    return pmd::mnp_ret;
                if ((Supplier<Object>) o == this::porta_returnm)
                    return this::porta_returnm;
            }
        } while (true);

        // END OF MUSIC['L' ガ アッタトキハ ソコヘ モドル]
mp15m:
        ;

        pmd.FlashMacroList();

        r.si--;
        pw.partWk[r.di].address = r.getSi(); // mov[di],si
        pw.partWk[r.di].loopcheck = 3;
        pw.partWk[r.di].onkai = 0xff; // -1
        r.bx = pw.partWk[r.di].partloop;
        if (r.getBx() == 0) return this::mpexitm;

        // 'L' ガ アッタトキ
        r.si = r.getBx();
        pw.partWk[r.di].loopcheck = 1;
        pw.partWk[r.di].loopCounter++;
        return this::mp1m;

mp2m:
        ; // F - NUMBER SET
        pmd.FlashMacroList();
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetm();

        r.al = (byte) pw.md[r.si++].dat;
        pw.partWk[r.di].leng = r.al;
        pmd.calc_q();
        return this::porta_returnm;
    }

    private Supplier<Object> porta_returnm() {
        if (pw.partWk[r.di].volpush == 0) break mp_newm;
        if (pw.partWk[r.di].onkai == 0xff) break mp_newm;
        pw.volpush_flag--;
        if (pw.volpush_flag == 0) break mp_newm;
        pw.volpush_flag = 0;
        pw.partWk[r.di].volpush = 0;
mp_newm:
        ;
        volsetm();
        otodasim();
        if ((pw.partWk[r.di].keyoff_flag & 1) == 0)
            break mp3m;
        keyonm();

mp3m:
        ;
        pw.partWk[r.di].keyon_flag++;
        pw.partWk[r.di].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di].keyoff_flag = r.al;
        if (pw.md[r.getSi()].dat != 0xfb) // '&'が直後にあったらkeyoffしない
            return pmd::mnp_ret;
        pw.partWk[r.di].keyoff_flag = 2;
        return pmd::mnp_ret;
    }

    private Supplier<Object> mpexitm() {
        r.cl = pw.partWk[r.di].lfoswi;
        r.al = r.cl;
        r.al &= 8;
        pw.lfo_switch = r.al;
        if (r.cl == 0)
            break volsm;
        if ((r.cl & 3) == 0)
            break not_lfom;

        pmd.lfo();
        if (!r.carry) break not_lfom;
        r.al = r.cl;
        r.al &= 3;
        pw.lfo_switch |= r.al;
not_lfom:
        ;
        if ((r.cl & 0x30) == 0)
            break not_lfom2;
        //pushf
        //cli
        pmd.lfo_change();
        pmd.lfo();
        if (!r.carry) break not_lfom1;
        pmd.lfo_change();
        //popf
        r.al = pw.partWk[r.di].lfoswi;
        r.al &= 0x30;
        pw.lfo_switch |= r.al;
        break not_lfom2;
not_lfom1:
        ;
        pmd.lfo_change();
        //popf
not_lfom2:
        ;
        if ((pw.lfo_switch & 0x19) == 0)
            break volsm;
        if ((pw.lfo_switch & 8) == 0)
            break not_portam;
        pmd.porta_calc();
not_portam:
        ;
        otodasim();
volsm:
        ;
        pmd.soft_env();
        if (r.carry) break volsm2;
        if ((pw.lfo_switch & 0x22) != 0)
            break volsm2;
        if (pw.fadeout_speed == 0)
            return pmd::mnp_ret;
volsm2:
        ;
        volsetm();
        return pmd::mnp_ret;
    }


    //139-181
    //==============================================================================
    //	ＰＣＭ音源演奏メイン：パートマスクされている時
    //==============================================================================
    private Supplier<Object> pcmmain_nonplay() {
        pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) return pmd::mnp_ret;

        if ((pw.partWk[r.di].partmask & 2) == 0) // bit1(pcm効果音中？)をcheck
            return this::pcmmnp_1;
        r.dx = (short) pw.fm2_port1;
        r.al = pc98.InPort(r.getDx());
        if ((r.al & 0b0000_0100) == 0) // EOS check
            return this::pcmmnp_1; // まだ割り込みPCMが鳴っている
        pw.pcmflag = 0; // PCM効果音終了
        pw.pcm_effec_num = (byte) 255;
        pw.partWk[r.di].partmask &= 0xfd; // bit1をclear
        if (pw.partWk[r.di].partmask == 0)
            return this::mp1m0; // partmaskが0なら復活させる
        return this::pcmmnp_1;
    }

    private Supplier<Object> pcmmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.si];
                r.al = (byte) pw.md[r.si++].dat;
                if (r.al == 0x80) break;
                if (r.al < 0x80) return pmd::fmmnp_3;

                Object o = commandsm();
                while (o != null && (Supplier<Object>) o != this::pcmmnp_1) {
                    o = ((Supplier<Object>) o).get();
                    if ((Supplier<Object>) o == pmd::mnp_ret)
                        return pmd::mnp_ret;
                }
            } while (true);

            pmd.FlashMacroList();

            //pcmmnp_2:
            // END OF MUSIC["L"があった時はそこに戻る]
            r.si--;
            pw.partWk[r.di].address = r.getSi();
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.bx = pw.partWk[r.di].partloop;

            if ((r.getBx() & r.getBx()) == 0) return pmd::fmmnp_4;

            // "L"があった時
            r.si = r.getBx();
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
        } while (true);
    }


    //182-
    //==============================================================================
    //	ＰＣＭ音源特殊コマンド処理
    //==============================================================================

    private Supplier<Object> commandsm() {
        pw.currentCommandTable = cmdtblm;
        r.bx = 0; // offset cmdtblp
        return pmd.command00();
    }

    private Supplier<Object>[] cmdtblm;

    private void SetupCmdtbl() {
        cmdtblm = new Supplier<Object>[] {
                this::comAtm                    //0xff(0)
                , pmd::comq                  //0xfe(1)
                , pmd::comv                  //0xfd(2)
                , pmd::comt                  //0xfc(3)
                , pmd::comtie                //0xfb(4)
                , pmd::comd                  //0xfa(5)
                , pmd::comstloop             //0xf9(6)
                , pmd::comedloop             //0xf8(7)
                , pmd::comexloop             //0xf7(8)
                , pmd::comlopset             //0xf6(9)
                , pmd::comshift              //0xf5(10)
                , this::comvolupm                 //0xf4(11)
                , this::comvoldownm               //0xf3(12)
                , pmd::lfoset                //0xf2(13)
                , pmd::lfoswitch             //0xf1(14)
                , pmd::psgenvset             //0xf0(15)
                , pmd::comy                  //0xef(16)
                , pmd::jump1                 //0xee(17)
                , pmd::jump1                 //0xed(18)
                //
                , this::pansetm                   //0xec(19)
                , pmd::rhykey                //0xeb(20)
                , pmd::rhyvs                 //0xea(21)
                , pmd::rpnset                //0xe9(22)
                , pmd::rmsvs                 //0xe8(23)
                //
                , pmd::comshift2             //0xe7(24)
                , pmd::rmsvs_sft             //0xe6(25)
                , pmd::rhyvs_sft             //0xe5(26)
                //
                , pmd::jump1                 //0xe4(27)
                //Ｖ２．３　ＥＸＴＥＮＤ
                , this::comvolupm2                //0xe3(28)
                , this::comvoldownm2              //0xe2(29)
                //
                , pmd::jump1                 //0xe1(30)
                , pmd::jump1                 //0xe0(31)
                //
                , pmd::syousetu_lng_set    //0DFH(32)
                //
                , pmd::vol_one_up_pcm    //0deH(33)
                , pmd::vol_one_down        //0DDH(34)
                //
                , pmd::status_write        //0DCH(35)
                , pmd::status_add        //0DBH(36)
                //
                , this::portam                    //0DAH(37)
                //
                , pmd::jump1                //0D9H(38)
                , pmd::jump1                    //0D8H(39)
                , pmd::jump1                    //0D7H(40)
                //
                , pmd::mdepth_set            //0D6H(41)
                //
                , pmd::comdd                    //0d5h(42)
                //
                , pmd::ssg_efct_set            //0d4h(43)
                , pmd::fm_efct_set            //0d3h(44)
                , pmd::fade_set                //0d2h(45)
                //
                , pmd::jump1                 //0xd1(46)
                , pmd::jump1                    //0d0h(47)
                //
                , pmd::jump1                //0cfh(48)
                , this::pcmrepeat_set            //0ceh(49)
                , pmd::extend_psgenvset        //0cdh(50)
                , pmd::jump1                    //0cch(51)
                , pmd::lfowave_set            //0cbh(52)
                , pmd::lfo_extend            //0cah(53)
                , pmd::envelope_extend        //0c9h(54)
                , pmd::jump3                //0c8h(55)
                , pmd::jump3                    //0c7h(56)
                , pmd::jump6                    //0c6h(57)
                , pmd::jump1                    //0c5h(58)
                , pmd::comq2                    //0c4h(59)
                , this::pansetm_ex                //0c3h(60)
                , pmd::lfoset_delay            //0c2h(61)
                , pmd::jump0                    //0c1h,sular(62)
                , this::pcm_mml_part_mask        //0c0h(63)
                , pmd::_lfoset                //0bfh(64)
                , pmd::_lfoswitch            //0beh(65)
                , pmd::_mdepth_set            //0bdh(66)
                , pmd::_lfowave_set            //0bch(67)
                , pmd::_lfo_extend            //0bbh(68)
                , pmd::_volmask_set            //0bah(69)
                , pmd::_lfoset_delay            //0b9h(70)
                , pmd::jump2                 //0xb8(71)
                , pmd::mdepth_count            //0b7h(72)
                , pmd::jump1                    //0xb6(73)
                , pmd::jump2                    //0xb5(74)
                , pmd::jump16                //0b4h(75)
                , pmd::comq3                    //0b3h(76)
                , pmd::comshift_master        //0b2h(77)
                , pmd::comq4                    //0b1h(78)
        };

        if (pw.ppz != 0) cmdtblm[75] = ppzdrv::ppz_extpartset; // 0b4h in ppzdrv.asm(75)
    }


    //288-313
    //==============================================================================
    //	演奏中パートのマスクon/off
    //==============================================================================
    private Supplier<Object> pcm_mml_part_mask() {
//#if DEBUG
//        logger.log(Level.TRACE, "pcm_mml_part_mask");
//#endif

        r.al = (byte) pw.md[r.si++].dat;
        if (r.al >= 2)
            return pmd::special_0c0h;

        if (r.al == 0)
            break pcm_part_maskoff_ret;

        pw.partWk[r.di].partmask |= 0x40;
        if (pw.partWk[r.di].partmask != 0x40)
            break pmpm_ret;

        r.dx = 0x0102; // PAN=0 / x8 bit mode
        pmd.opnset46();
        r.dx = 0x0001; // PCM RESET
        pmd.opnset46();

pmpm_ret:
        ;
        //r.ax = r.stack.pop(); // commandsm
        return this::pcmmnp_1;

pcm_part_maskoff_ret:
        ;
        pw.partWk[r.di].partmask &= 0xbf;
        if (pw.partWk[r.di].partmask != 0)
            break pmpm_ret;
        //r.ax = r.stack.pop(); // commandsm
        return this::mp1m; // パート復活
    }


    //314-351
    //==============================================================================
    //	リピート設定
    //==============================================================================
    private Supplier<Object> pcmrepeat_set() {
        r.ax = (short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100);
        r.si += 2;
        if ((r.getAx() & 0x8000) != 0)
            break prs1_minus;
        r.ax += (short) pw.pcmstart;
        break prs1_set;
prs1_minus:
        ;
        r.ax += (short) pw.pcmstop;
prs1_set:
        ;
        pw.pcmrepeat1 = r.getAx();
        r.ax = (short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100);
        r.si += 2;
        if (r.ax == 0)
            break prs2_minus;
        if ((r.ax & 0x8000) != 0)
            break prs2_minus;
        r.ax += (short) pw.pcmstart;
        break prs2_set;
prs2_minus:
        ;
        r.ax += (short) pw.pcmstop;
prs2_set:
        ;
        pw.pcmrepeat2 = r.getAx();
        r.ax = (short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100);
        r.si += 2;
        if (r.getAx() == 0x8000)
            break prs3_set;
        if (r.getAx() >= 0x8000)
            break prs3_minus;
        r.ax += (short) pw.pcmstart;
        break prs3_set;

prs3_minus:
        ;
        r.ax += (short) pw.pcmstop;

prs3_set:
        ;
        pw.pcmrelease = r.getAx();

        return null;
    }


    //352-397
    //==============================================================================
    //	ポルタメント(PCM)
    //==============================================================================
    private Supplier<Object> portam() {
        if (pw.partWk[r.di].partmask != 0) {
            //return pmd.porta_notset;
            r.al = (byte) pw.md[r.si++].dat; // 最初の音程を読み飛ばす(Mask時)
            return null;
        }

        //pop ax; commandsp
        r.al = (byte) pw.md[r.si++].dat;
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetm();
        r.ax = pw.partWk[r.di].fnum;
        r.stack.push(r.getAx());
        r.al = pw.partWk[r.di].onkai;
        r.stack.push(r.getAx());
        r.al = (byte) pw.md[r.si++].dat;
        pmd.oshift();
        fnumsetm();
        r.ax = pw.partWk[r.di].fnum; // ax = ポルタメント先のdelta_n値
        r.bx = r.stack.pop();
        pw.partWk[r.di].onkai = r.bl;
        r.bx = r.stack.pop(); // bx = ポルタメント元のdelta_n値
        pw.partWk[r.di].fnum = r.getBx();
        r.ax -= r.getBx(); // ax = delta_n差
        r.bl = (byte) pw.md[r.si++].dat;
        pw.partWk[r.di].leng = r.bl;
        pmd.calc_q();
        r.bh = 0;
        int src = (short) r.getAx();
        r.dx = (short) (src % (short) r.getBx()); // ax = delta_n差 / 音長
        r.ax = (short) (src / (short) r.getBx());
        pw.partWk[r.di].porta_num2 = r.getAx(); // 商
        pw.partWk[r.di].porta_num3 = r.getDx(); // 余り
        pw.partWk[r.di].lfoswi |= 8; // Porta ON
        return this::porta_returnm;
    }


    //398-409
    //
    //	COMMAND ']' [VOLUME UP]
    //
    public Supplier<Object> comvolupm() {
        r.al = pw.partWk[r.di].volume;
        r.carry = (r.al + 16) > 0xff;
        r.al += 16;
        return vupckm();
    }

    private Supplier<Object> vupckm() {
        if (r.carry) r.al = (byte) 255;
        return vsetm();
    }

    private Supplier<Object> vsetm() {
        pw.partWk[r.di].volume = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, (int) r.al);
        cd.additionalData = md;
        pmd.WriteDummy(cd);

        return null;
    }

    // Ｖ２．３　ＥＸＴＥＮＤ
    public Supplier<Object> comvolupm2() {
        r.al = (byte) pw.md[r.si++].dat;
        r.carry = (r.al + pw.partWk[r.di].volume) > 0xff;
        r.al += pw.partWk[r.di].volume;
        return vupckm();
    }


    //415-433
    //
    //	COMMAND '[' [VOLUME DOWN]
    //
    public Supplier<Object> comvoldownm() {
        r.al = pw.partWk[r.di].volume;
        r.carry = r.al - 16 < 0;
        r.al -= 16;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }

    // Ｖ２．３　ＥＸＴＥＮＤ
    public Supplier<Object> comvoldownm2() {
        r.al = (byte) pw.md[r.si++].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di].volume;
        r.carry = r.al - r.ah < 0;
        r.al -= r.ah;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }


    //434-445
    //==============================================================================
    //	COMMAND 'p' [Panning Set]
    //==============================================================================
    private Supplier<Object> pansetm() {
        r.al = (byte) pw.md[r.si++].dat;
        return this::pansetm_main;
    }

    private Supplier<Object> pansetm_main() {
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al &= 0b1100_0000;
        pw.partWk[r.di].fmpan = r.al;
        return null;
    }


    //446-463
    //==============================================================================
    //	Pan setting Extend
    //==============================================================================
    private Supplier<Object> pansetm_ex() {
        r.al = (byte) pw.md[r.si++].dat;
        r.si++; // 逆走flagは読み飛ばす
        if (r.al == 0)
            break pmex_mid;
        if ((r.al & 0x80) != 0)
            break pmex_left;
        r.al = 2;
        return this::pansetm_main;
pmex_mid:
        ;
        r.al = 3;
        return this::pansetm_main;
pmex_left:
        ;
        r.al = 3;
        return this::pansetm_main;
    }


    //464-485
    //
    //	COMMAND '@' [NEIRO Change]
    //
    private Supplier<Object> comAtm() {
        r.al = (byte) pw.md[r.si++].dat;
        pw.partWk[r.di].voicenum = r.al;

        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Instrument, pw.cmd.linePos
                , (int) 0xff
                , (int) pw.partWk[r.di].voicenum
        );
        pmd.WriteDummy(cd);

        r.ah = 0;
        r.ax += r.getAx();
        r.ax += r.getAx();

        r.bx = 0; // offset pcmadrs
        r.bx += r.getAx();
        r.ax = (short) (pw.pcmWk[r.getBx()] + pw.pcmWk[r.getBx() + 1] * 0x100); // pw.pcmadrs[r.bx];
        r.bx++;
        r.bx++;
        pw.pcmstart = r.getAx();
        r.ax = (short) (pw.pcmWk[r.getBx()] + pw.pcmWk[r.getBx() + 1] * 0x100); // pw.pcmadrs[r.bx];
        pw.pcmstop = r.getAx();
        pw.pcmrepeat1 = 0;
        pw.pcmrepeat2 = 0;
        pw.pcmrelease = (short) 0x8000;

        return null;
    }


    //486-602
    //==============================================================================
    //	PCM VOLUME SET
    //==============================================================================
    private void volsetm() {
        r.al = pw.partWk[r.di].volpush;
        if (r.al != 0)
            break vsm_01;
        r.al = pw.partWk[r.di].volume;
vsm_01:
        ;
        r.dl = r.al;
        //------------------------------------------------------------------------------
        //	音量down計算
        //------------------------------------------------------------------------------
        r.al = pw.pcm_voldown;
        if (r.al == 0)
            break pcm_fade_calc;
        r.al = (byte) -r.al;
        r.ax = (short) (r.al * r.dl);
        r.dl = r.ah;
        //------------------------------------------------------------------------------
        //	Fadeout計算
        //------------------------------------------------------------------------------
pcm_fade_calc:
        ;
        r.al = pw.fadeout_volume;
        if (r.al == 0)
            break pcm_env_calc;
        r.al = (byte) -r.al;
        r.ax = (short) (r.al * r.al); // al=al^2
        r.al = r.ah;
        r.ax = (short) (r.al * r.dl);
        r.dl = r.ah;
        //------------------------------------------------------------------------------
        //	ENVELOPE 計算
        //------------------------------------------------------------------------------
pcm_env_calc:
        ;
        r.al = r.dl;
        if (r.al == 0) // 音量0?
            break mv_out;
        if (pw.partWk[r.di].envf != 0xff) // -1
            break normal_mvset;
        // 拡張版 音量 = al * (eenv_vol + 1) / 16
        r.dl = pw.partWk[r.di].eenv_volume;
        if (r.dl == 0)
            break mv_min;
        r.dl++;
        r.ax = (short) (r.al * r.dl);
        r.ax >>= 3;
        r.carry = ((r.getAx() % 2) != 0);
        r.ax >>= 1;
        if (!r.carry) break mvset;
        r.ax++;
        break mvset;
normal_mvset:
        ;
        r.ah = pw.partWk[r.di].eenv_volume; // .penv;
        if ((r.ah & 0x80) == 0)
            break mvplus;
        // -
        r.ah = (byte) -r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.carry = r.al - r.ah < 0;
        r.al -= r.ah;
        if (!r.carry) break mvset;
mv_min:
        ;
        r.al = 0;
        break mv_out;
        // +
mvplus:
        ;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.carry = r.al + r.ah > 0xff;
        r.al += r.ah;
        if (!r.carry) break mvset;
        r.al = (byte) 255;
        //------------------------------------------------------------------------------
        //	音量LFO計算
        //------------------------------------------------------------------------------
mvset:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x22) == 0)
            break mv_out;
        r.dx = 0;
        r.ah = r.dl;
        if ((pw.partWk[r.di].lfoswi & 0x2) == 0)
            break mv_nolfo1;
        r.dx = pw.partWk[r.di].lfodat;
mv_nolfo1:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x20) == 0)
            break mv_nolfo2;
        r.dx += pw.partWk[r.di]._lfodat;
mv_nolfo2:
        ;
        if ((r.dx & 0x8000) != 0)
            break mvlfo_minus;
        r.ax += r.dx;
        if (r.ah == 0)
            break mv_out;
        r.al = 255;
        break mv_out;
mvlfo_minus:
        ;
        r.carry = r.getAx() + r.getDx() > 0xffff;
        r.ax += r.getDx();
        if (r.carry) break mv_out;
        r.al = 0;

        //------------------------------------------------------------------------------
        //	出力
        //------------------------------------------------------------------------------
mv_out:
        ;
        r.dl = r.al;
        r.dh = 0x0b;
        pmd.opnset46();
    }


    //603-672
    //==============================================================================
    //	PCM KEYON
    //==============================================================================
    private void keyonm() {
        if (pw.partWk[r.di].onkai != 0xff) //-1
            break keyonm_00;
        return; // キュウフ ノ トキ
keyonm_00:
        ;
        r.dx = 0x0102; // PAN=0 / x8 bit mode
        pmd.opnset46();
        r.dx = 0x0021; // PCM RESET
        pmd.opnset46();
        r.bx = (short) pw.pcmstart;
        r.dh = 2;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        r.bx = (short) pw.pcmstop;
        r.dh++;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        r.ax = pw.pcmrepeat1;
        r.ax |= pw.pcmrepeat2;
        if (r.ax != 0) break pcm_repeat_keyon;
        r.dx = 0x00a0; // PCM PLAY(non_repeat)
        pmd.opnset46();
        r.dl = pw.partWk[r.di].fmpan; // PAN SET
        r.dl |= 2; // x8 bit mode
        r.dh = 1;
        pmd.opnset46();
        return;
pcm_repeat_keyon:
        ;
        r.dx = 0x00b0; // PCM PLAY(repeat)
        pmd.opnset46();
        r.dl = pw.partWk[r.di].fmpan; // PAN SET
        r.dl |= 2; // x8 bit mode
        r.dh = 1;
        pmd.opnset46();
        r.bx = pw.pcmrepeat1; // REPEAT ADDRESS set 1
        r.dh = 2;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        r.bx = pw.pcmrepeat2; // REPEAT ADDRESS set 2
        r.dh++;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
    }

    //673-714
    //
    //	PCM KEYOFF
    //
    private void keyoffm() {
        if (pw.partWk[r.di].envf == 0xff) // -1
            break kofm1_ext;
        if (pw.partWk[r.di].envf != 2)
            break keyoffm_main;
kofm_ret:
        ;
        return;
kofm1_ext:
        ;
        if (pw.partWk[r.di].eenv_count == 4)
            break kofm_ret;
keyoffm_main:
        ;
        if (pw.pcmrelease == 0x8000) {
            keyoffp();
            return;
        }
        r.dx = 0x0021; // PCM RESET
        pmd.opnset46();
        r.bx = pw.pcmrelease;
        r.dh = 2;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        r.bx = (short) pw.pcmstop; // Stop ADDRESS for Release
        r.dh++;
        r.dl = r.bl;
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        r.dx = 0x00a0; // PCM PLAY(non_repeat)
        pmd.opnset46();
        keyoffp();
    }

    public void keyoffp() {
        if (pw.partWk[r.di].onkai != 0xff) {
            kofp1();
            return;
        }
        return; // キュウフ ノ トキ
    }

    private void kofp1() {
        if (pw.partWk[r.di].envf == 0xff)
            break kofp1_ext;
        pw.partWk[r.di].envf = 2;
        return;

kofp1_ext:
        ;
        pw.partWk[r.di].eenv_count = 4;
        return;
    }


    //715-767
    //
    //	PCM OTODASI
    //
    private void otodasim() {
        r.bx = pw.partWk[r.di].fnum;
        if (r.getBx() != 0)
            break odm_00;
        return;
odm_00:
        ;
        //
        // Portament/LFO/Detune SET
        //
        r.bx = (short) ((short) r.getBx() + (short) pw.partWk[r.di].porta_num);
        r.dx = 0;
        if ((pw.partWk[r.di].lfoswi & 0x11) == 0)
            break odm_not_lfo;
        if ((pw.partWk[r.di].lfoswi & 0x1) == 0)
            break odm_not_lfo1;
        r.dx = pw.partWk[r.di].lfodat;
odm_not_lfo1:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x10) == 0)
            break odm_not_lfo2;
        r.dx += pw.partWk[r.di]._lfodat;
odm_not_lfo2:
        ;
        r.dx += r.getDx(); // ; PCM ハ LFO ガ カカリニクイ ノデ depth ヲ 4バイ スル
        r.dx += r.getDx();
odm_not_lfo:
        ;
        r.dx += pw.partWk[r.di].detune;
        if ((r.getDx() & 0x8000) != 0)
            break odm_minus;
        r.carry = r.getBx() + r.getDx() > 0xffff;
        r.bx += r.getDx();
        if (!r.carry) break odm_main;
        r.bx = 0xffff; // -1
        break odm_main;
odm_minus:
        ;
        r.carry = r.getBx() + r.getDx() > 0xffff;
        r.bx += r.getDx();
        if (r.carry) break odm_main;
        r.bx = 0;
odm_main:
        ;
        //
        // TONE SET
        //
        r.dh = 9;
        r.dl = r.bl;
        //pushf
        //cli
        pmd.opnset46();
        r.dh++;
        r.dl = r.bh;
        pmd.opnset46();
        //popf
    }


    //768-813
    //
    //	PCM FNUM SET
    //
    private void fnumsetm() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrest(); // 休符の場合
            return;
        }
        pw.partWk[r.di].onkai = r.al;
        r.bh = 0;
        r.bl = r.ah; // bx=onkai
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al &= 0xf;
        r.cl = r.al; // cl=octarb
        r.ch = r.al;
        r.al = 5;
        r.carry = r.al - r.cl < 0;
        r.al -= r.cl;
        if (!r.carry) break fnm00;
        r.al = 0;
fnm00:
        ;
        r.cl = r.al; // cl=5-octarb
        //r.bx += r.bx;
        r.ax = pw.pcm_tune_data[r.getBx()];
        if (r.ch < 6) // o7以上?
            break pts01m;
        r.ch = 0x50;
        if ((r.getAx() & 0x8000) != 0)
            break pts00m;
        r.ax += r.getAx(); // o7以上で2倍できる場合は2倍
        r.ch = 0x60;
pts00m:
        ;
        pw.partWk[r.di].onkai &= 0x0f;
        pw.partWk[r.di].onkai |= r.ch; // onkai値修正
        break fnm01;
pts01m:
        ;
        r.ax = (short) (r.getAx() >> r.cl); // ax=ax/[2^OCTARB]
fnm01:
        ;
        pw.partWk[r.di].fnum = r.getAx();
    }

    private void fnrest() {
        pw.partWk[r.di].onkai = (byte) 0xff;
        if ((pw.partWk[r.di].lfoswi & 0x11) != 0)
            break fnr_ret;
        pw.partWk[r.di].fnum = 0; // 音程LFO未使用
fnr_ret:
        ;
        return;
    }
}
