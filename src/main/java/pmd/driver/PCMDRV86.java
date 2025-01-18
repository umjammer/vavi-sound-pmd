package pmd.driver;

import java.util.function.Function;
import java.util.function.Supplier;

import musicDriverInterface.ChipDatum;
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;


public class PCMDRV86 {

    private PMD pmd;
    private PW pw;
    private X86Register r;
    private Pc98 pc98;
    private Function<ChipDatum, Integer> p86drv;
    private Runnable[] trans_table;
    private byte[][] pcmData;

    public PCMDRV86(PMD pmd, PW pw, X86Register r, Pc98 pc98, Function<ChipDatum, Integer> p86drv, byte[][] pcmData) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.p86drv = p86drv;
        this.pcmData = pcmData;

        SetupCmdtbl();
    }

    /**
    //	PCM音源 演奏 メイン(86B PCM)
     */
    //pcmmain_ret:
    //	ret
    public void pcmmain() {
        r.setSi(pw.partWk[r.di].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0)
            return;

//        Supplier<Object> ret = null;
//        if (pw.partWk[r.di].partmask != 0)
//            ret = this::pcmmain_nonplay;
//        else
//            ret = this::pcmmain_c_1;
//
//        if (ret != null) {
//            do {
//                ret = (Supplier<Object>) ret.get();
//            } while (ret != null);
//        }
    }

    private Supplier<Object> pcmmain_c_1() {
        // 音長 -1
        pw.partWk[r.di].leng--;
        r.al = pw.partWk[r.di].leng;

        //	; KEYOFF CHECK
        if ((pw.partWk[r.di].keyoff_flag & 3) != 0) { // 既にkeyoffしたか？ // break mp0m;

            if (r.al <= pw.partWk[r.di].qdat) { // Q値 => 残りLength値時 keyoff // break mp0m;
//mp00m:
                keyoffm(); // ALは壊さない
                pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
            }
        }
//mp0m:
        // LENGTH CHECK
        if (r.al != 0) return this::mpexitm;
        return this::mp1m0;
    }

    private Supplier<Object> mp1m0() {
        return this::mp1m;
    }

    private Supplier<Object> mp1m() { // DATA READ
        do {
            pw.cmd = pw.md[r.getSi()];
            r.al = (byte) pw.md[r.getSi()].dat;

            //if (r.si == pw.jumpIndex)
            //pw.jumpIndex = -1; // KUMA:Added

            r.incSi();
            if ((r.al & 0xff) <= 0x80) break; //mp15m;

            // ELSE COMMANDS
            Object o = commandsm();
            Supplier<Object> mp1m_ = this::mp1m;
            Supplier<Object> mnp_ret_ = pmd::mnp_ret;
            while (o != null && o != mp1m_) {
                o = ((Supplier<Object>) o).get();
                if (o == mnp_ret_)
                    return pmd::mnp_ret;
                //if ((Supplier<Object>)o == porta_returnm)
                //return porta_returnm;
            }
        } while (true);

        // END OF MUSIC['L' ガ アッタトキハ ソコヘ モドル]
//mp15m:
        if ((r.al & 0xff) >= 0x80) { // break mp2m;
            pmd.FlashMacroList();

            r.decSi();
            pw.partWk[r.di].address = r.getSi(); // mov[di],si
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di].partloop);
            if (r.getBx() == 0) return this::mpexitm;

            // 'L' ガ アッタトキ
            r.setSi(r.getBx());

            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
            return this::mp1m;
        }
//mp2m:
        // F - NUMBER SET
        pmd.FlashMacroList();
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetm();

        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].leng = r.al;
        pmd.calc_q();


        if (pw.partWk[r.di].volpush != 0) { // break mp_newm;
            if (pw.partWk[r.di].onkai != (byte) 0xff) { // break mp_newm;
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) { // break mp_newm;
                    pw.volpush_flag = 0;
                    pw.partWk[r.di].volpush = 0;
                }
            }
        }
//mp_newm:
        volsetm();
        otodasim();
        if ((pw.partWk[r.di].keyoff_flag & 1) != 0) { // break mp3m;
            keyonm();
        }
//mp3m:
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
        if ((r.cl & 0x22) != 0) { // break not_lfo3m;

            pw.lfo_switch = 0;
            if ((r.cl & 2) != 0) { // break not_lfom;

                pmd.lfo();
                r.al = r.cl;
                r.al &= 2;
                pw.lfo_switch = r.al;
            }
//not_lfom:
            if ((r.cl & 0x20) != 0) { // break not_lfo2m;
                //pushf
                //cli
                pmd.lfo_change();
                pmd.lfo();
                if (r.carry) { // break not_lfo1m;
                    pmd.lfo_change();
                    //	popf
                    r.al = pw.partWk[r.di].lfoswi;
                    r.al &= 0x20;
                    pw.lfo_switch |= r.al;
//                    break not_lfo2m;
                } else {
//not_lfo1m:
                    pmd.lfo_change();
                    //	popf
                }
            }
//not_lfo2m:
            pmd.soft_env();
            if (!r.carry) { // break volsm2;
                if ((pw.lfo_switch & 0x22) == 0) { // break volsm2;
//volsm1:
                    if (pw.fadeout_speed == 0)
                        return pmd::mnp_ret;
                }
            }
//volsm2:
            volsetm();
            return pmd::mnp_ret;
        }
//not_lfo3m:
        pmd.soft_env();
        if (!r.carry)  { // break volsm2;
//        break volsm1; // <<
            if (pw.fadeout_speed == 0) // <<
                return pmd::mnp_ret; // <<
        }
//volsm2: // <<
        volsetm(); // <<
        return pmd::mnp_ret; // <<
    }

    /**
    //	PCM音源演奏メイン：パートマスクされている時
     */
    private Supplier<Object> pcmmain_nonplay() {
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) return pmd::mnp_ret;

        if ((pw.partWk[r.di].partmask & 2) == 0) // bit1(pcm効果音中？)をcheck
            return this::pcmmnp_1;

        if (pw.play86_flag == 1)
            return this::pcmmnp_1; // まだ割り込みPCMが鳴っている
        pw.pcmflag = 0; // PCM効果音終了
        pw.pcm_effec_num = (byte) 255;
        pw.partWk[r.di].partmask &= 0xfd; // bit1をclear
        if (pw.partWk[r.di].partmask != 0)
            return this::pcmmnp_1;

        r.al = pw.partWk[r.di].voicenum;
        neiro_set();
        r.al = pw.partWk[r.di].fmpan;
        r.ah = pw.revpan;
        set_pcm_pan();

        return this::mp1m0; // partmaskが0なら復活させる
    }

    private Supplier<Object> pcmmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.getSi()];
                r.al = (byte) pw.md[r.incSi()].dat;
                if (r.al == (byte) 0x80) break; // KUMA: 未チェック(TAG050で　==になおした)
                if ((r.al & 0xff) < 0x80) return pmd::fmmnp_3;

                Object o = commandsm();
                Supplier<Object> _pcmmnp_1 = this::pcmmnp_1;
                Supplier<Object> _mnp_ret = pmd::mnp_ret;
                while (o != null && o != _pcmmnp_1) {
                    o = ((Supplier<Object>) o).get();
                    if (o == _mnp_ret)
                        return pmd::mnp_ret;
                }
            } while (true);

            pmd.FlashMacroList();

            //	; END OF MUSIC["L"があった時はそこに戻る]
            r.decSi();
            pw.partWk[r.di].address = r.getSi();
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di].partloop);

            if ((r.getBx() & r.getBx()) == 0) return pmd::fmmnp_4;

            //    ; "L"があった時
            r.setSi(r.getBx());
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
        } while (true);
    }

    /**
    //	PCM音源特殊コマンド処理
     */
    private Supplier<Object> commandsm() {
        pw.currentCommandTable = cmdtblm;
        r.setBx((short) 0); // offset cmdtblp
        return pmd.command00();
    }

    private Supplier<Object>[] cmdtblm;

    private void SetupCmdtbl() {
        cmdtblm = new Supplier[] {
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
                , pmd::jump1                    //0DAH(37)
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
                , pmd::jump4                //0bfh(64)
                , pmd::jump1                //0beh(65)
                , pmd::jump2                //0bdh(66)
                , pmd::jump1                    //0bch(67)
                , pmd::jump1                //0bbh(68)
                , pmd::jump1                    //0bah(69)
                , pmd::jump1                //0b9h(70)
                , pmd::jump2                 //0xb8(71)
                , pmd::mdepth_count            //0b7h(72)
                , pmd::jump1                    //0xb6(73)
                , pmd::jump2                    //0xb5(74)
                , pmd::jump16                //0b4h(75)
                , pmd::comq3                    //0b3h(76)
                , pmd::comshift_master        //0b2h(77)
                //, pmd::comq4				    //0b1h(78)
        };

        trans_table = new Runnable[] {
                this::double_trans
                , this::left_trans
                , this::right_trans
                , this::double_trans
                , this::double_trans_g
                , this::left_trans_g
                , this::right_trans_g
                , this::double_trans_g
        };
    }

    /**
    //	演奏中パートのマスクon/off
     */
    private Supplier<Object> pcm_mml_part_mask() {
        r.al = (byte) pw.md[r.incSi()].dat;
        if (r.al >= 2)
            return pmd::special_0c0h;

        if (r.al != 0) { // break pcm_part_maskoff_ret;

            pw.partWk[r.di].partmask |= 0x40;
            if (pw.partWk[r.di].partmask == 0x40) { // break pmpm_ret;

                stop_86pcm();
            }
//pmpm_ret:
            //    pop ax; commandsm
            return this::pcmmnp_1;
        }
//pcm_part_maskoff_ret:
        pw.partWk[r.di].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di].partmask != 0) {
//            break pmpm_ret;
            return this::pcmmnp_1;
        }
        //    pop ax		;commandsm
        return this::mp1m; // パート復活
    }

    /**
    //	リピート設定
     */
    private Supplier<Object> pcmrepeat_set() {
        r.setAx(pw._start_ofs);
        pw.repeat_ofs = r.getAx();
        r.setAx(pw._start_ofs2);
        pw.repeat_ofs2 = r.getAx(); // repeat開始位置 = start位置に設定
        r.setDx(pw._size1);
        pw.repeat_size1 = r.getDx();
        r.setCx(pw._size2); // cx:dx=全体size
        pw.repeat_size2 = r.getCx(); // repeat_size = 今のsizeに設定
        pw.repeat_flag = 1;

        pw.release_flag1 = 0;

        r.stack.push(r.getDx()); // サイズを保存
        r.stack.push(r.getCx());//

        //	一個目 = リピート開始位置
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);

        if ((r.getAx() & 0x8000) == 0) { // break prs1_minus;

            // 正の場合
            pcm86vol_chk();
            int a = (pw.repeat_size2 & 0xffff) * 0x1_0000 + (pw.repeat_size1 & 0xffff);
            a -= r.getAx() & 0xffff; // リピートサイズ＝全体のサイズ-指定値
            pw.repeat_size1 = (short) a;
            pw.repeat_size2 = (short) (a >> 16);

            a = (pw.repeat_ofs2 & 0xffff) * 0x1_0000 + (pw.repeat_ofs & 0xffff);
            a += r.getAx(); // リピート開始位置から指定値を加算
            pw.repeat_ofs = (short) a;
            pw.repeat_ofs2 = (short) (a >> 16);

//            break prs2_set;
        } else {
            // 負の場合
//prs1_minus:
            r.setAx((short) (-r.getAx()));
            pcm86vol_chk();

            pw.repeat_size1 = r.getAx(); // リピートサイズ＝neg(指定値)
            pw.repeat_size2 = 0;

            int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
            a -= r.getAx() & 0xffff;
            r.setDx((short) a);
            r.setCx((short) (a >> 16));

            a = (pw.repeat_ofs2 & 0xffff) * 0x1_0000 + (pw.repeat_ofs & 0xffff);
            a += r.getDx(); // リピート開始位置に
            a += r.getCx() * 0x1_0000; // (全体サイズ-指定値)を加算
            pw.repeat_ofs = (short) a;
            pw.repeat_ofs2 = (short) (a >> 16);
        }
        //	２個目 = リピート終了位置
//prs2_set:
        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);

        if (r.getAx() != 0) { // break prs3_set;//	;0なら計算しない
            if ((r.getAx() & 0x8000) == 0) { // break prs2_minus;

                //正の場合
                pcm86vol_chk();
                pw._size1 = r.getAx(); // ; 正ならpcmサイズ＝指定値
                pw._size2 = 0;

                int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
                a -= r.getAx(); // リピートサイズから(旧サイズ-新サイズ)を引く
                r.setDx((short) a);
                r.setCx((short) (a >> 16));

                a = (pw.repeat_size2 & 0xffff) * 0x1_0000 + (pw.repeat_size1 & 0xffff);
                a -= r.getAx(); // リピートサイズ＝全体のサイズ-指定値
                a -= (r.getCx() & 0xffff) * 0x1_0000;
                pw.repeat_size1 = (short) (a & 0xffff);
                pw.repeat_size2 = (short) ((a & 0xffff_0000) >> 16);

//                break prs3_set;
            } else {
                // 負の場合
//prs2_minus:
                r.setAx((short) (-r.getAx()));
                pcm86vol_chk();

                int a = pw.repeat_size2 * 0x10000 + pw.repeat_size1;
                a -= r.getAx(); // リピートサイズから
                // neg(指定値)を引く
                pw.repeat_size1 = (short) (a & 0xffff);
                pw.repeat_size2 = (short) ((a & 0xffff_0000) >> 16);

                a = (pw._size2 & 0xffff) * 0x1_0000 + pw._size1;
                a -= r.getAx(); // 本来のサイズから指定値を引く
            }
        }
        //	３個目 = リリース開始位置
//prs3_set:
        r.setCx(r.stack.pop());
        r.setDx(r.stack.pop()); // cx:dx=全体サイズ復帰

        r.setAx((short) (pw.md[r.getSi()].dat + pw.md[r.getSi() + 1].dat * 0x100));
        r.addSi((short) 2);

        if (r.getAx() != (short) 0x8000) { // break prs_exit; // 8000Hなら設定しない
            r.carry = (r.getAx() & 0xffff) < 0x8000;

            r.setBx(pw._start_ofs);
            pw.release_ofs = r.getBx();
            r.setBx(pw._start_ofs2);
            pw.release_ofs2 = r.getBx();
            // release開始位置 = start位置に設定
            pw.release_size1 = r.getDx();
            pw.release_size2 = r.getCx(); // release_size = 今のsizeに設定
            pw.release_flag1 = 1; // リリースするに設定
            if (r.carry) { // break prs3_minus;

                //正の場合
                pcm86vol_chk();
                // リリースサイズ＝全体のサイズ-指定値
                int a = (pw.release_size2 & 0xffff) * 0x1_0000 + (pw.release_size1 & 0xffff);
                a -= r.getAx();
                pw.release_size1 = (short) (a & 0xffff);
                pw.release_size2 = (short) ((a & 0xffff_0000) >> 16);

                //リリース開始位置から指定値を加算
                a = (pw.release_ofs2 & 0xffff) * 0x1_0000 + (pw.release_ofs & 0xffff);
                a += r.getAx();
                pw.release_ofs = (short) (a & 0xffff);
                pw.release_ofs2 = (short) ((a & 0xffff_0000) >> 16);

//                break prs_exit;
            } else {
                // 負の場合
//prs3_minus:
                r.setAx((short) (-r.getAx()));
                pcm86vol_chk();
                pw.release_size1 = r.getAx(); // リリースサイズ＝neg(指定値)
                pw.release_size2 = 0;

                int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
                a -= r.getAx();
                r.setDx((short) (a & 0xffff));
                r.setCx((short) ((a & 0xffff_0000) >> 16));

                a = (pw.release_ofs2 & 0xffff) * 0x1_0000 + (pw.release_ofs & 0xffff);
                a += r.getDx(); // リリース開始位置に
                a += (r.getCx() & 0xffff) * 0x1_0000; // (全体サイズ-指定値)を加算
                pw.release_ofs = (short) (a & 0xffff);
                pw.release_ofs2 = (short) ((a & 0xffff_0000) >> 16);
            }
        }
//prs_exit:
        return null;
    }

    /**
    //	/Sオプション指定時はAXを32倍する
     */
    private void pcm86vol_chk() {
        if (pw.pcm86_vol == 0) return;

        r.addAx(r.getAx());
        r.addAx(r.getAx());
        r.addAx(r.getAx());
        r.addAx(r.getAx());
        r.addAx(r.getAx());
//not_p86chk:
    }

    //423-440
    /**
    //	COMMAND ')' [VOLUME UP]
     */
    public Supplier<Object> comvolupm() {
        r.al = pw.partWk[r.di].volume;
        r.carry = (r.al & 0xff) + 16 > 0xff;
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
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, r.al & 0xff);
        cd.additionalData = md;
        pmd.writeDummy(cd);

        return null;
    }

    // Ｖ２．３　ＥＸＴＥＮＤ
    public Supplier<Object> comvolupm2() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.carry = (r.al & 0xff) + (pw.partWk[r.di].volume & 0xff) > 0xff;
        r.al += pw.partWk[r.di].volume;
        return vupckm();
    }

    //441-459
    /**
    //	COMMAND '(' [VOLUME DOWN]
     */
    public Supplier<Object> comvoldownm() {
        r.al = pw.partWk[r.di].volume;
        r.carry = r.al - 16 < 0;
        r.al -= 16;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }

    //    ; Ｖ２．３　ＥＸＴＥＮＤ
    public Supplier<Object> comvoldownm2() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di].volume;
        r.carry = r.al - r.ah < 0;
        r.al -= r.ah;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }

    //460-486
    /**
    //	COMMAND 'p' [Panning Set]
    //	p0 逆相
    // p1 右
    // p2 左
    // p3 中
     */
    private Supplier<Object> pansetm() {
        r.ah = 0;
        r.al = (byte) pw.md[r.incSi()].dat;
        r.al--;
        if (r.al != 0) { // break psm_right;
            r.al--;
            if (r.al != 0) { // break psm_left;
                r.al--;
                if (r.al != 0) { // break psm_mid;
                    r.ah++; //逆相
                }
//psm_mid:
                r.al = 0;
                return this::set_pcm_pan;
            }
//psm_left:
            r.al = (byte) 0x80; // -128;
            return this::set_pcm_pan;
        }
//psm_right:
        r.al = +127;
        return this::set_pcm_pan;
    }

    /**
    //	COMMAND 'px' [Panning Set Extend]
    //	px-127～+127,0or1
     */
    private Supplier<Object> pansetm_ex() {
        r.al = (byte) pw.md[r.incSi()].dat;
        r.ah = (byte) pw.md[r.incSi()].dat;
        return this::set_pcm_pan;
    }

    private Supplier<Object> set_pcm_pan() {
        pw.partWk[r.di].fmpan = r.al;
        pw.revpan = r.ah;

        return set_pcm_pan2();
    }

    private Supplier<Object> set_pcm_pan2() {
        if ((r.al & 0x80) == 0) { // break psmex_left;
            if (r.al == 0) {
//                break psmex_mid;
                // 真ん中 // ↑
//psmex_mid:
                pw.pcm86_pan_flag = 3; // Middle
                r.al = 0;
            } else {
                // 右寄り
                pw.pcm86_pan_flag = 2; // Right
                r.al = (byte) ~r.al;
                r.al &= 127;
//                break psmex_gs_set;
            }
        } else {
            // 左寄り
//psmex_left:
            pw.pcm86_pan_flag = 1; // Left
            r.al += 128;
            r.al &= 127;
//            break psmex_gs_set;
        }
//psmex_gs_set:
        pw.pcm86_pan_dat = r.al;

        if ((r.ah & 1) != 0) { // break psmex_ret;

            pw.pcm86_pan_flag |= 4; // 逆相
        }
//psmex_ret:
        return null;
    }

    /**
    //	COMMAND '@' [NEIRO Change]
     */
    private Supplier<Object> comAtm() {
        r.al = (byte) pw.md[r.incSi()].dat;
        pw.partWk[r.di].voicenum = r.al;
        return this::neiro_set;
    }

    private Supplier<Object> neiro_set() {
        //r.ah = 0;
        //r.ax += r.ax;
        //r.bx = r.getAx();
        //r.ax += r.ax;
        //r.bx += r.ax; // bx=al*6
        //r.bx += 0; // offset pcmadrs
        //r.ax = (short)(pw.pcmadrs_86[r.bx] + pw.pcmadrs_86[r.bx + 1] * 0x100); // ofs2(w)
        //r.carry = (r.bx + 2) > 0xffff;
        //r.bx += 2;
        //pw._start_ofs = r.getAx();
        //r.ax = 0;
        //r.al += (byte)(pw.pcmadrs_86[r.bx] + (r.carry ? 1 : 0)); // ofs1(b)
        //r.bx++;
        //pw._start_ofs2 = r.getAx();
        //r.ax = (short)(pw.pcmadrs_86[r.bx] + pw.pcmadrs_86[r.bx + 1] * 0x100);
        //r.bx++;
        //r.bx++;
        //pw._size1 = r.getAx();
        //r.ah = 0;
        //r.al = pw.pcmadrs_86[r.bx];
        //pw._size2 = r.getAx();
        //pw.repeat_flag = 0;
        //pw.release_flag1 = 0;

        ChipDatum cd = new ChipDatum(2, 0, r.al);
        p86drv.apply(cd);

        return null;
    }

    /**
    //	PCM VOLUME SET
     */
    private void volsetm() {
        r.al = pw.partWk[r.di].volpush;
        if (r.al == 0) { // break vsm_01;
            r.al = pw.partWk[r.di].volume;
        }
//vsm_01:
        r.dl = r.al;
        //------------------------------------------------------------------------------
        //	音量down計算
        //------------------------------------------------------------------------------
        r.al = pw.pcm_voldown;
        if (r.al != 0) { // break pcm_fade_calc;
            r.al = (byte) -r.al;
            r.setAx((short) (r.al * r.dl));
            r.dl = r.ah;
        }
        //------------------------------------------------------------------------------
        //	Fadeout計算
        //------------------------------------------------------------------------------
//pcm_fade_calc:
        r.al = pw.fadeout_volume;
        if (r.al != 0) { // break pcm_env_calc;
            r.al = (byte) -r.al;
            r.setAx((short) (r.al * r.dl));
            r.dl = r.ah;
        }
        //------------------------------------------------------------------------------
        //	ENVELOPE 計算
        //------------------------------------------------------------------------------
//pcm_env_calc:
        r.al = r.dl;
        if (r.al == 0) { // 音量0?
            mv_out();
            return;
        }
        if (pw.partWk[r.di].envf == (byte) 0xff) { // -1 // break normal_mvset;
            // 拡張版 音量 = al * (eenv_vol + 1) / 16
            r.dl = pw.partWk[r.di].eenv_volume;
            if (r.dl == 0) {
//                break mv_min;
//mv_min: // ↑
                r.al = 0;
                mv_out();
                return;
            }
            r.dl++;
            r.setAx((short) (r.al * r.dl));
            r.srAx(3);
            r.carry = ((r.getAx() % 2) != 0);
            r.srAx(1);
            if (r.carry) { // break mvset;
                r.incAx();
            }
//            break mvset;
        } else {
//normal_mvset:
            r.ah = pw.partWk[r.di].eenv_volume; // .penv;
            if ((r.ah & 0x80) != 0) { // break mvplus;
                // -
                r.ah = (byte) -r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.carry = r.al - r.ah < 0;
                r.al -= r.ah;
                if (r.carry) { // break mvset;
//mv_min:
                    r.al = 0;
                    mv_out();
                    return;
                }
            } else {
                // +
//mvplus:
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
                r.al += r.ah;
                if (r.carry) { // break mvset;
                    r.al = (byte) 255;
                }
            }
        }
        //------------------------------------------------------------------------------
        //	音量LFO計算
        //------------------------------------------------------------------------------
//mvset:
        if ((pw.partWk[r.di].lfoswi & 0x22) == 0) {
            mv_out();
            return;
        }
        r.setDx((short) 0);
        r.ah = r.dl;
        if ((pw.partWk[r.di].lfoswi & 0x2) != 0) { // break mv_nolfo1;
            r.setDx(pw.partWk[r.di].lfodat);
        }
//mv_nolfo1:
        if ((pw.partWk[r.di].lfoswi & 0x20) != 0) { // break mv_nolfo2;
            r.addDx(pw.partWk[r.di]._lfodat);
        }
//mv_nolfo2:
        if ((r.getDx() & 0x8000) == 0) { // break mvlfo_minus;
            r.addAx(r.getDx());
            if (r.ah == 0) {
                mv_out();
                return;
            }
            r.al = (byte) 255;
            mv_out();
            return;
        }
//mvlfo_minus:
        r.carry = (r.getAx() & 0xffff) + (r.getDx() & 0xffff) > 0xffff;
        r.addAx(r.getDx());
        if (r.carry) {
            mv_out();
            return;
        }
        r.al = 0;
        mv_out();
    }

    //------------------------------------------------------------------------------
    //	出力
    //------------------------------------------------------------------------------
    private void mv_out() {
        // 音量設定
        if (pw.pcm86_vol != 0) { // break pcm_normal_set;
            // SPBと同様の音量設定
            // al = sqr(al)
            r.ah = r.al;
            r.al = 0;
            r.carry = true;

//sqr_loop:
            while (true) {
                boolean c = (r.ah - (byte) (r.al + (r.carry ? 1 : 0))) < 0;
                r.ah -= (byte) (r.al + (r.carry ? 1 : 0));
                if (c) break; // pcm_vol_set;

                c = (r.ah - (byte) (r.al + (c ? 1 : 0))) < 0;
                r.ah -= (byte) (r.al + (c ? 1 : 0));
                if (c) break; // pcm_vol_set;

                r.al++;
                if (r.al == 15)
                    break; // pcm_vol_set;
//                break sqr_loop
            }
        } else {
//pcm_normal_set:
            r.al >>= 4;
        }
//pcm_vol_set:
        ChipDatum cd = new ChipDatum(4, 0, r.al);
        p86drv.apply(cd);
        //r.al &= 0b0000_1111;
        //r.al ^= 0b0000_1111;
        //r.al |= 0xa0; // PCM音量
        //r.dx = 0xa466;
        //pc98.OutPort(r.dx, r.al);
    }

    /**
    //	PCM KEYON
     */
    private void keyonm() {
        if (pw.partWk[r.di].onkai == (byte) 0xff) { //-1 // break keyonm_00;
            return; // when a rest
        }
//keyonm_00:
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        play_86pcm();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
    }

    /**
    //	PCM KEYOFF
     */
    private void keyoffm() {
        ChipDatum cd = new ChipDatum(9, 0, 0);
        p86drv.apply(cd);
        //    if (pw.release_flag1 != 1) // リリースが設定されているか?
        //        break kofm_not_release;
        //    r.stack.push(r.ax);
        //    r.ax = pw.release_ofs;
        //    pw.start_ofs = r.getAx();
        //    r.ax = pw.release_ofs2;
        //    pw.start_ofs2 = r.getAx();
        //    r.ax = pw.release_size1;
        //    pw.size1 = r.getAx();
        //    r.ax = pw.release_size2;
        //    pw.size2 = r.getAx();
        //    r.ax = r.stack.pop();
        //    pw.release_flag2 = 1; // リリースした

        //kofm_not_release:;
        //    if (pw.partWk[r.di].envf == 0xff) // -1
        //        break kofm1_ext;
        //    if (pw.partWk[r.di].envf != 2)
        //    {
        //        pmd.keyoffp();
        //        return;
        //    }

        //kofm_ret:;
        //    return;

        //kofm1_ext:;
        //    if (pw.partWk[r.di].eenv_count == 4)
        //        break kofm_ret;

        //    pmd.keyoffp();
    }

    /**
    //	PCM 周波数設定
     */
    private void otodasim() {
        r.setBx(pw.partWk[r.di].fnum);
        if (r.getBx() == 0) { // break tone_set;
            return;
        }
//tone_set:
        r.setAx(pw.partWk[r.di].fnum2);
        if (pw.pcm86_vol == 1) { //	;ADPCMに合わせる場合
            tone_set2(); // DetuneはCut
            return;
        }
        if (pw.partWk[r.di].detune == 0) {
            tone_set2();
            return;
        }

        r.setCx(r.getAx());
        r.setDx(r.getBx());

        int c = (r.getDx() & 0xffff) * 0x10000 + (r.getCx() & 0xffff);
        c >>= 5;
        r.setDx((short) (c >> 16));
        r.setCx((short) c);

        //for (int i = 0; i < 5; i++) { // rept	5
        //    r.carry = (r.dx & 1) != 0;
        //    r.dx >>= 1;
        //    r.cx >>= 1;
        //    if (r.carry) r.cx |= 0x8000;
        //} // endm			;cx=zzzzzxxx xxxxxxxx

        r.setDx(pw.partWk[r.di].detune);
        if ((r.getDx() & 0x8000) == 0) { // break tsdt_minus;
            r.carry = (r.getCx() & 0xffff) + (r.getDx() & 0xffff) > 0xffff;
            r.addCx(r.getDx());
            if (r.carry) { // break tone_set1;
                r.setCx((short) 0xffff); // -1
            }
//            break tone_set1;
        } else {
//tsdt_minus:
            r.carry = (r.getCx() & 0xffff) + (r.getDx() & 0xffff) > 0xffff;
            r.addCx(r.getDx());
            if (r.getCx() == 0 && // break tsdtm0;
                    !r.carry) { // break tone_set1;
//tsdtm0:
                r.setCx((short) 1); // 0にすると加算値0になる危険があるので1にする
            }
        }
//tone_set1:
        r.setDx((short) 0);

        c = (r.getDx() & 0xffff) * 0x1_0000 + (r.getCx() & 0xffff);
        c <<= 5;
        r.setDx((short) (c >> 16));
        r.setCx((short) c);
        //for (int i = 0; i < 5; i++) { // rept	5
        //    r.carry = (r.cx & 0x8000) != 0;
        //    r.cx <<= 1;
        //    r.dx <<= 1;
        //    if (r.carry) r.dx |= 0x0001;
        //} //    endm			;dx:cx=00000000 000zzzzz xxxxxxxx xxx00000

        r.andBx((short) 0b1111_1111_1110_0000);
        r.addAx((short) 0b0000_0000_0001_1111);
        r.orBx(r.getDx());
        r.orAx(r.getCx());

        tone_set2();
    }

    private void tone_set2() {
        pw.addsize2 = r.getAx();
        pw.addsize1 = r.bl;
        //pw.addsize1 &= 0x1f;
        //logger.log(Level.TRACE, "{0:x}  {1:x}", pw.addsize1, pw.addsize2);
        ChipDatum cd = new ChipDatum(5, pw.addsize1, pw.addsize2);
        p86drv.apply(cd);

        //r.carry = false;
        //r.bl = r.rol(r.bl, 1);
        //r.bl = r.rol(r.bl, 1);
        //r.bl = r.rol(r.bl, 1);
        //r.bl &= 7;
        //r.bl ^= 7;
        //// 周波数設定
        //r.dx = 0xa468;
        ////pushf
        ////cli
        //r.al = pc98.InPort(r.dx);
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xf8;
        //r.al |= r.bl;
        //pc98.OutPort(r.dx, r.al);
        ////popf
    }

    /**
    //	PCM FNUM SET
     */
    private void fnumsetm() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            pmd.fnrest(); // 休符の場合
            return;
        }

        if (pw.pcm86_vol == 1) { // break fsm_noad;

            if (r.al >= 0x65) { // o7e? // break fsm_noad;
                r.al = 0x50; // o6
                if (r.ah < 5) { // ah=onkai // break fsm_00;
                    r.al = 0x60; // o7
                }
//fsm_00:
                r.al |= r.ah;
            }
        }
//fsm_noad:
        pw.partWk[r.di].onkai = r.al;

        r.al &= (byte) 0xf0;
        r.al >>= 1;
        r.bl = r.al; // bl=octave*8
        r.al >>= 1; // al=octave*4
        r.bl += r.al;

        r.bl += r.ah; // bl=octave*12 + 音階
        r.bh = 0;
        r.setAx(r.getBx());
        //r.bx += r.bx;
        //r.bx += r.ax;
        //r.bx += 0; // offset pcm_tune_data
        //logger.log(Level.TRACE, "bx:%d",r.bx);
        r.al = (byte) (int) pw.pcm_tune_data86[r.getBx()].getItem1();
        r.orAx(0xff00);
        pw.partWk[r.di].fnum = r.getAx(); // ax=0ff00h + addsize1
        //r.bx++;
        r.setAx((short) (int) pw.pcm_tune_data86[r.getBx()].getItem2()); // ax=addsize2
        pw.partWk[r.di].fnum2 = r.getAx();
        //logger.log(Level.TRACE, "fnum:{0:x} fnum2:{1:x}", pw.partWk[r.di].fnum, pw.partWk[r.di].fnum2);
    }

    /**
    //	FIFO int Subroutine
    //		*FIFOが来ている事を確認してから飛んで来ること。
    //		 pushしてあるレジスタは ax/dx/ds のみ。
     */
    private void fifo_main() {
        //------------------------------------------------------------------------------
        //	割り込み許可
        //------------------------------------------------------------------------------
        if (pw.disint != 1) { // break fifo_not_sti;

            //sti			;早速割り込み許可
        }
//fifo_not_sti:
        //------------------------------------------------------------------------------
        //	PCM処理 main
        //------------------------------------------------------------------------------
        if (pw.play86_flag != 0) { //	;PCM再生中か？ // break not_trans;

            if (pw.trans_flag == 0) { //	;次を転送するか？ // break i5_trans;

                stop_86pcm(); // ; PLAY中で且つ次にはもうデータはない= stop
                return; // FIFOは許可しないで終了
            }
//i5_trans:
            r.stack.push(r.getBx());
            r.stack.push(r.getCx());
            r.stack.push((short) (r.sign ? 1 : 0));
            r.stack.push(r.di);
            r.stack.push(r.bp);

            pcm_trans();

            r.bp = r.stack.pop();
            r.di = r.stack.pop();
            r.setSi(r.stack.pop());
            r.setCx(r.stack.pop());
            r.setBx(r.stack.pop());
        }
        //------------------------------------------------------------------------------
        //	割り込み禁止
        //------------------------------------------------------------------------------
//not_trans:
        //	cli
        //------------------------------------------------------------------------------
        //	FIFO割り込みフラグreset
        //------------------------------------------------------------------------------
        r.setDx((short) 0xa468);
        r.al = pc98.InPort(r.getDx());
        pc98.outPort((short) 0x5f, r.al);
        r.al &= (byte) 0xef;
        pc98.outPort(r.getDx(), r.al); // FIFO割り込みフラグ消去
        pc98.outPort((short) 0x5f, r.al);
        r.al |= 0x10;
        pc98.outPort(r.getDx(), r.al); // FIFO割り込みフラグ消去解除
    }

    /**
    //	PCMdata 転送
    // use ax/bx/cx/dx/si/di/bp
     */
    private void pcm_trans2() {
        r.setCx((short) pw.trans_size); // 転送するbytes
        pcm_trans_main();
    }

    private void pcm_trans() {
        r.setCx((short) (pw.trans_size / 2)); // 転送するbytes
        pcm_trans_main();
    }

    private void pcm_trans_main() {
        //KUMA: P86drv常駐チェック
        //      恐らく不要なため未実装。
        //break zero_trans; // 常駐していない場合

        //KUMA: P86drvのバージョンチェック
        //      恐らく不要なため未実装。
        //r.ah=0xff: // -1
        //int	65h
        //if (r.al < 0x10)
        //    break zero_trans; // ver.1.0以前の場合

        r.ah = 0;
        r.al = pw.pcm86_pan_flag;
        r.addAx(r.getAx());
        r.addAx((short) 0); // offset trans_table
        r.bp = r.getAx(); // bp=転送処理sub offset

        r.setDx((short) 0xa46c);
        r.setAx(pw.size1);
        r.di = r.getAx(); // di=残りsize(下位16bit)
        r.orAx(pw.size2);
        if (r.getAx() == 0) {
            zero_trans();
            return;
        }

        //r.stack.push(r.ds);

        r.ah = (byte) 0xfb; // -5
        ChipDatum cd = new ChipDatum(r.ah, -1, -1);
        p86drv.apply(cd); // p86drv pushems

        r.ah = pw.addsize1;
        r.setBx(pw.addsize2);

        get_data_offset(); // ds:si = data offset
        trans_table[r.bp / 2].run();

        r.ah = (byte) 0xfc; // -4
        cd = new ChipDatum(r.ah, -1, -1);
        p86drv.apply(cd); // p86drv popems

        //    pop ds
    }

    //------------------------------------------------------------------------------
    //	真ん中
    //------------------------------------------------------------------------------
    private void double_trans() {
        r.bp = 0;
//double_trans_loop:
        do {
            //	mov al,[si]
            pc98.outPort(r.getDx(), r.al); // 左
            pc98.outPort(r.getDx(), r.al); // 右
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    private void trans_exit() {
        pw.start_ofs += r.bp; // bp=転送したサイズ
        pw.start_ofs2 += (short) (r.carry ? 1 : 0);
        pw.size1 = r.di;
    }

    //------------------------------------------------------------------------------
    //	真ん中(逆相)
    //------------------------------------------------------------------------------
    private void double_trans_g() {
        r.bp = 0;
//double_trans_g_loop:
        do {
            //	mov al,[si]
            pc98.outPort(r.getDx(), r.al); // 左
            r.al = (byte) -r.al;
            pc98.outPort(r.getDx(), r.al); // 右
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    //------------------------------------------------------------------------------
    //	左寄り
    //------------------------------------------------------------------------------
    private void left_trans() {
        r.bp = 0;
//left_trans_loop:
        do {
            //	mov al,[si]
            pc98.outPort(r.getDx(), r.al); // 左
            r.stack.push(r.getAx());
            r.setAx((short) (r.getAx() * pw.pcm86_pan_dat));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // 右
            r.setAx(r.stack.pop());
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    //------------------------------------------------------------------------------
    //	左寄り(逆相)
    //------------------------------------------------------------------------------
    private void left_trans_g() {
        r.bp = 0;
//left_trans_g_loop:
        do {
            //	mov al,[si]
            pc98.outPort(r.getDx(), r.al); // 左
            r.al = (byte) -r.al;
            r.stack.push(r.getAx());
            r.setAx((short) (r.getAx() * pw.pcm86_pan_dat));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // 右
            r.setAx(r.stack.pop());
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    //------------------------------------------------------------------------------
    //	右寄り
    //------------------------------------------------------------------------------
    private void right_trans() {
        r.bp = 0;
//right_trans_loop:
        do {
            //	mov al,[si]
            r.stack.push(r.getAx());
            r.setAx((short) (r.getAx() * pw.pcm86_pan_dat));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // 左
            r.setAx(r.stack.pop());
            pc98.outPort(r.getDx(), r.al); // 右
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    //------------------------------------------------------------------------------
    //	右寄り(逆相)
    //------------------------------------------------------------------------------
    private void right_trans_g() {
        r.bp = 0;
//right_trans_g_loop:
        do {
            //	mov al,[si]
            r.stack.push(r.getAx());
            r.setAx((short) (r.getAx() * pw.pcm86_pan_dat));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // 左
            r.setAx(r.stack.pop());
            r.al = (byte) -r.al; // 逆相
            pc98.outPort(r.getDx(), r.al); // 右
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    //------------------------------------------------------------------------------
    //	Addressを進める
    //		cy=1 ・・・ 転送終了
    //------------------------------------------------------------------------------
    private void add_address() {
        pw.addsizew += r.getBx(); // bx=addsize2
        //pushf
        r.al = r.ah;
        r.ah = 0; // ax=addsize1
        r.bp += (short) (r.getAx() + (r.carry ? 1 : 0)); // bpをaddsizeに従って加算
        //popf
        //pushf
        r.addSi((short) (r.getAx() + (r.carry ? 1 : 0))); // addressをaddsizeに従って加算
        if (r.getSi() >= 0x4000) { // 16K Over Check(for EMS) break not_add_ofs2;

            //[[[segment over]]]
            r.carry = (pw.start_ofs & 0xffff) + (r.bp & 0xffff) > 0xffff;
            pw.start_ofs += r.bp;
            pw.start_ofs2 += (short) (0 + (r.carry ? 1 : 0));

            r.bp = 0; // 転送サイズのreset
            get_data_offset();
        }
//not_add_ofs2:
        //popf
        boolean c = (r.di - (short) (r.getAx() + (r.carry ? 1 : 0))) < 0;
        r.di -= (short) (r.getAx() + (r.carry ? 1 : 0)); // sizeをaddsizeに従って減算
        r.ah = r.al; // ah=addsize1 に戻す
        if (!c) { // break addadd_sizeseg;
            if (r.di != 0) { // break addadd_justcheck;
                return;
            }
//addadd_justcheck:
            if (pw.size2 != 0) { // ジャスト０ // break addadd_repchk;
                return;
            }
        } else {
//addadd_sizeseg:
            r.carry = (pw.size2 - 1) < 0;
            pw.size2 -= 1;
            if (!r.carry) { // break addadd_repchk;
//                return;
            }
        }
//addadd_repchk:
        if (pw.repeat_flag != 0) { // break addadd_stc_ret;

            if (pw.release_flag2 != 1) { // break addadd_stc_ret;

                // repeat設定
                r.stack.push(r.getAx());
                r.stack.push(r.getDx());
                r.setAx(pw.repeat_size2);
                pw.size2 = r.getAx();
                r.di = pw.repeat_size1;
                r.setAx(pw.repeat_ofs2);
                pw.start_ofs2 = r.getAx();
                r.setDx(pw.repeat_ofs);
                pw.start_ofs = r.getDx();
                r.bp = 0;

                r.ah = (byte) 0xfd;
                ChipDatum cd = new ChipDatum(r.ah, -1, -1);
                p86drv.apply(cd); // get data offset = ds:dx

                r.setSi(r.getDx()); // DS:SI= DATA ADDRESS
                r.setDx(r.stack.pop());
                r.setAx(r.stack.pop());
                r.carry = false;
                return;
            }
        }
//addadd_stc_ret:
        r.carry = true;
    }

    //------------------------------------------------------------------------------
    //	新規にpcmdata offsetを得る
    //------------------------------------------------------------------------------
    private void get_data_offset() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());

        r.setDx(pw.start_ofs); // cs:[start_ofs]
        r.setAx(pw.start_ofs2); // cs:[start_ofs2]

        r.ah = (byte) 0xfd;
        ChipDatum cd = new ChipDatum(r.ah, -1, -1);
        p86drv.apply(cd); // get data offset = ds:dx

        r.setSi(r.getDx()); // DS:SI= DATA ADDRESS

        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //------------------------------------------------------------------------------
    //	転送終了・・・残りを０で埋める
    //------------------------------------------------------------------------------
    private void trans_fin() {
        r.decCx();
        if (r.getCx() != 0) { // break tfin_ret;

            r.al = 0;
//tfin_loop:
            do {
                pc98.outPort(r.getDx(), r.al); // 左
                pc98.outPort(r.getDx(), r.al); // 右
                r.decCx();
            } while (r.getCx() != 0);
        }
//tfin_ret:
        pw.size1 = r.getCx(); // cs:[size1]	;cx=0
        pw.size2 = r.getCx(); // cs:[size2]
    }

    //------------------------------------------------------------------------------
    //	0で埋める
    //------------------------------------------------------------------------------
    private void zero_trans() {
        r.al = 0;
//ztr_loop:
        do {
            pc98.outPort(r.getDx(), r.al); // 左
            pc98.outPort(r.getDx(), r.al); // 右
            r.decCx();
        } while (r.getCx() != 0);
        pw.trans_flag = 0; // もう転送しないでいいよ
    }

    /**
    //	86B play PCM
     */
    private void play_86pcm() {
        ChipDatum cd = new ChipDatum(3, pw.pcm86_pan_flag, pw.pcm86_pan_dat);
        p86drv.apply(cd);

        cd = new ChipDatum(7, 0, 0);
        p86drv.apply(cd);

        ////pushf
        ////cli

        //r.dx = 0xa468;
        //r.al = pc98.InPort(r.dx);
        ////	A468 bit7をreset	（FIFO停止）
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0x7f;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit6をreset	（CPU->FIFO モード）
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xbf;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit3をset		（FIFO リセット設定）
        //pc98.OutPort(0x5f, r.al);
        //r.al |= 8;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit3をreset	（FIFO リセット解除）
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xf7;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit5をreset	（FIFO割り込み禁止/A46A設定準備）
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xdf;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit4をreset	（割り込みフラグ消去）
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xef;
        //pc98.OutPort(r.dx, r.al);

        ////	A46A に PAN を OUT	（8bit L/Rch）
        //r.dx = 0xa46a;
        //r.al = 0xf2;
        //pc98.OutPort(r.dx, r.al);

        ////popf

        //// 最初のdataを転送
        //r.si = 0; // offset _start_ofs
        //r.di = 0; // offset start_ofs
        //r.cx = 4;
        ////rep movsw

        //pw.addsizew = 0;
        //pw.release_flag2 = 0;

        //r.stack.push(r.bp);
        //pcm_trans2();
        //r.bp = r.stack.pop();

        ////pushf
        ////cli
        ////------------------------------------------------------------------------------
        ////	割り込み設定
        ////------------------------------------------------------------------------------
        //r.dx = 0xa468;
        //r.al = pc98.InPort(r.dx);

        ////	A468 bit4をset		（割り込みフラグ消去解除）
        //pc98.OutPort(0x5f, r.al);
        //r.al |= 0x10;
        //pc98.OutPort(r.dx, r.al);

        ////	A468 bit5をset		（FIFO割り込み許可/A46A設定準備）
        //pc98.OutPort(0x5f, r.al);
        //r.al |= 0x20;
        //pc98.OutPort(r.dx, r.al);

        ////	A46AのFIFO割り込みサイズを設定
        //r.dx = 0xa46a;
        //r.al = (byte)(+(pw.trans_size / 128) - 1);
        //pc98.OutPort(r.dx, r.al);

        ////------------------------------------------------------------------------------
        ////	再生開始
        ////------------------------------------------------------------------------------
        //r.dx = 0xa468;
        //r.al = pc98.InPort(r.dx);
        ////	A468 bit7をset		（PCM 再生開始）
        //pc98.OutPort(0x5f, r.al);
        //r.al |= 0x80;
        //pc98.OutPort(r.dx, r.al);

        //pw.play86_flag = 1;
        //pw.trans_flag = 1;

        ////popf
    }

    //1297-1339
    /**
    //	86B PCM stop
     */
    public void stop_86pcm() {
        ChipDatum cd = new ChipDatum(8, 0, 0);
        p86drv.apply(cd);

        //r.stack.push(r.ax);
        //r.stack.push(r.dx);

        ////pushf
        ////cli

        //r.dx = 0xa468;
        //r.al = pc98.InPort(r.dx);

        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0x7f;
        //pc98.OutPort(r.dx, r.al);

        ////	FIFO reset
        //pc98.OutPort(0x5f, r.al);
        //r.al |= 0x08;
        //pc98.OutPort(r.dx, r.al); // Reset処理

        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xf7;
        //pc98.OutPort(r.dx, r.al); // Reset処理おわり

        ////	FIFO 割り込み禁止
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xdf;
        //pc98.OutPort(r.dx, r.al);

        ////	FIFO 割り込みフラグreset
        //pc98.OutPort(0x5f, r.al);
        //r.al &= 0xef;
        //pc98.OutPort(r.dx, r.al);

        //pc98.OutPort(0x5f, r.al);
        //r.al |= 0x10;
        //pc98.OutPort(r.dx, r.al);

        //pw.play86_flag = 0; // cs:[play86_flag]
        //pw.trans_flag = 0; // cs:[trans_flag]

        ////popf

        //r.dx = r.stack.pop();
        //r.ax = r.stack.pop();
    }

    /**
    //	PCM効果音ルーチン
    //		input dx  fnum
    //			ch Pan
    // cl Volume
    // al Number
     */
    private void pcm_effect() {
        r.setBx((short) 0); //offset part10
        pw.partWk[pw.part10].partmask |= 2; // PCM Part Mask
        pw.pcmflag = 1;
        pw.pcm_effec_num = r.al;
        pw._voice_delta_n = r.getDx();
        pw._pcm_volume = r.cl;
        pw._pcmpan = r.ch;

        stop_86pcm();

        //cli

        r.al = pw.pcm_effec_num;
        neiro_set();

        r.al = pw._pcmpan;
        r.ah = 0;
        set_pcm_pan2();

        r.setBx((short) pw._voice_delta_n);
        r.setAx(r.getBx());
        r.bl = r.bh;
        r.andBx((short) 0b0111_0000_0000_1111);
        r.bh <<= 1;
        r.bl |= r.bh;
        r.ah = r.al;
        r.al = 0;
        tone_set2();
        r.al = pw._pcm_volume;
        mv_out();

        //sti

        play_86pcm();
    }
}
