package pmd.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.function.Function;
import java.util.function.Supplier;

import musicDriverInterface.ChipDatum;
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;

import static java.lang.System.getLogger;


public class PPZDRV {

    private static final Logger logger = getLogger(PPZDRV.class.getName());

    private PMD pmd = null;
    private PW pw = null;
    private X86Register r = null;
    private Pc98 pc98 = null;
    private Function<ChipDatum, Integer> ppz8em = null;
    private byte[][] pcmData;
    public PCMDRV pcmdrv = null;
    private int bank = 0;
    private int ptr = 0;

    public PPZDRV(PMD pmd, PW pw, X86Register r, Pc98 pc98, Function<ChipDatum, Integer> ppz8em, byte[][] pcmData) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.ppz8em = ppz8em;
        this.pcmData = pcmData;
    }

    public void init() {
        SetupCmdtbl();
    }

    //==============================================================================
    //	ＰＣＭ音源 演奏 メイン[PPZ8]
    //==============================================================================
    public void ppz8_call() {
        //出来るだけppz8emを直接コールしてください
        throw new UnsupportedOperationException();
    }


    public void ppzmain() {
        r.si = pw.partWk[r.di].address; // si = PART DATA ADDRESS
        if (r.getSi() == 0)
            return; // break pcmmain_ret;

        //if (r.si == pw.jumpIndex)
        //pw.jumpIndex = -1; // KUMA:Added
        //Console.WriteLine("%d", r.si);

        Supplier<Object> ret = null;
        if (pw.partWk[r.di].partmask != 0)
            ret = this::ppzmain_nonplay;
        else
            ret = this::ppzmain_c_1;

        if (ret != null) {
            do {
                ret = (Supplier<Object>) ret.get();
            } while (ret != null);
        }
    }

    private Supplier<Object> ppzmain_c_1() {
        // 音長 - 1
        pw.partWk[r.di].leng--;
        r.al = pw.partWk[r.di].leng;

        // KEYOFF CHECK
        if ((pw.partWk[r.di].keyoff_flag & 3) == 0) { // break mp0z; // 既にkeyoffしたか？
            if (r.al <= pw.partWk[r.di].qdat) { // break mp0z; // Q値 => 残りLength値時 keyoff
                pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
                keyoffz(); // ALは壊さない
            }
        }
//mp0z:
        // LENGTH CHECK
        if (r.al != 0) return this::mpexitz;
        return this::mp1z0;
    }

    private Supplier<Object> mp1z0() {
        pw.partWk[r.di].lfoswi &= 0xf7; // Porta off
        return this::mp1z;
    }

    private Supplier<Object> mp1z() // DATA READ
    {
        do {
            pw.cmd = pw.md[r.getSi()];

            //if (r.si == pw.jumpIndex)
            //pw.jumpIndex = -1; // KUMA:Added

            r.al = (byte) pw.md[r.incSi()].dat;
            if (r.al < 0x80) break mp2z;
            if (r.al == 0x80) break mp15z;

            // ELSE COMMANDS
            Object o = commandsz();
            while (o != null && (Supplier<Object>) o != this::mp1z) {
                o = ((Supplier<Object>) o) ();
                if ((Supplier<Object>) o == pmd::mnp_ret)
                    return pmd::mnp_ret;
                if ((Supplier<Object>) o == this::porta_returnz)
                    return this::porta_returnz;
            }
        } while (true);

        // END OF MUSIC['L' ガ アッタトキハ ソコヘ モドル]
mp15z:
        ;

        pmd.FlashMacroList();

        r.si--;
        pw.partWk[r.di].address = r.si; // mov[di],si
        pw.partWk[r.di].loopcheck = 3;
        pw.partWk[r.di].onkai = 0xff; // -1
        r.bx = pw.partWk[r.di].partloop;
        if (r.bx == 0) return this::mpexitz;

        // 'L' ガ アッタトキ
        r.si = r.bx;
        pw.partWk[r.di].loopcheck = 1;
        pw.partWk[r.di].loopCounter++;
        return this::mp1z;

mp2z:
        ; // F - NUMBER SET
        pmd.FlashMacroList();
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetz();

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = pw.cmd;
        ppz8em(cd);

        r.al = (byte) pw.md[r.si++].dat;
        pw.partWk[r.di].leng = r.al;
        pmd.calc_q();
        return this::porta_returnz;
    }

    private Supplier<Object> porta_returnz() {
        if (pw.partWk[r.di].volpush == 0) break mp_newz;
        if (pw.partWk[r.di].onkai == 0xff) break mp_newz;
        pw.volpush_flag--;
        if (pw.volpush_flag == 0) break mp_newz;
        pw.volpush_flag = 0;
        pw.partWk[r.di].volpush = 0;
mp_newz:
        ;
        volsetz();
        otodasiz();
        if ((pw.partWk[r.di].keyoff_flag & 1) == 0)
            break mp3z;
        keyonz();

mp3z:
        ;
        pw.partWk[r.di].keyon_flag++;
        pw.partWk[r.di].address = r.si;
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di].keyoff_flag = r.al;
        if (pw.md[r.si].dat != 0xfb) // '&'が直後にあったらkeyoffしない
            return pmd::mnp_ret;
        pw.partWk[r.di].keyoff_flag = 2;
        return pmd::mnp_ret;
    }

    private Supplier<Object> mpexitz() {
        r.cl = pw.partWk[r.di].lfoswi;
        r.al = r.cl;
        r.al &= 8;
        pw.lfo_switch = r.al;
        if (r.cl == 0)
            break volsz;
        if ((r.cl & 3) == 0)
            break not_lfoz;

        pmd.lfo();
        if (!r.carry) break not_lfoz;
        r.al = r.cl;
        r.al &= 3;
        pw.lfo_switch |= r.al;
not_lfoz:
        ;
        if ((r.cl & 0x30) == 0)
            break not_lfoz2;
        //pushf
        //cli
        pmd.lfo_change();
        pmd.lfo();
        if (!r.carry) break not_lfoz1;
        pmd.lfo_change();
        //popf
        r.al = pw.partWk[r.di].lfoswi;
        r.al &= 0x30;
        pw.lfo_switch |= r.al;
        break not_lfoz2;
not_lfoz1:
        ;
        pmd.lfo_change();
        //popf
not_lfoz2:
        ;
        if ((pw.lfo_switch & 0x19) == 0)
            break volsz;
        if ((pw.lfo_switch & 8) == 0)
            break not_portaz;
        pmd.porta_calc();
not_portaz:
        ;
        otodasiz();
volsz:
        ;
        pmd.soft_env();
        if (r.carry) break volsz2;
        if ((pw.lfo_switch & 0x22) != 0)
            break volsz2;
        if (pw.fadeout_speed == 0)
            return pmd::mnp_ret;
volsz2:
        ;
        volsetz();
        return pmd::mnp_ret;
    }


    //146-153
    //==============================================================================
    //	ＰＣＭ音源演奏メイン：パートマスクされている時
    //==============================================================================
    private Supplier<Object> ppzmain_nonplay() {
        pw.partWk[r.di].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di].leng--;
        if (pw.partWk[r.di].leng != 0) return pmd::mnp_ret;

        return ppzmnp_1;
    }


    //154-181
    private Supplier<Object> ppzmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.si];
                r.al = (byte) pw.md[r.si++].dat;
                if (r.al == 0x80) break;
                if (r.al < 0x80) return ppzmnp_3;

                Object o = commandsz();
                while (o != null && (Supplier<Object>) o != ppzmnp_1) {
                    o = ((Supplier<Object>) o) ();
                    if ((Supplier<Object>) o == pmd::mnp_ret)
                        return pmd::mnp_ret;
                }
            } while (true);

            pmd.FlashMacroList();

            //pcmmnp_2:
            // END OF MUSIC["L"があった時はそこに戻る]
            r.si--;
            pw.partWk[r.di].address = r.si;
            pw.partWk[r.di].loopcheck = 3;
            pw.partWk[r.di].onkai = 0xff; // -1
            r.bx = pw.partWk[r.di].partloop;

            if ((r.bx & r.bx) == 0) return pmd::fmmnp_4;

            // "L"があった時
            r.si = r.bx;
            pw.partWk[r.di].loopcheck = 1;
            pw.partWk[r.di].loopCounter++;
        } while (true);
    }

    private Supplier<Object> ppzmnp_3() {
        pw.partWk[r.di].fnum2 = 0;
        return pmd::fmmnp_3;
    }


    //182-
    //==============================================================================
    //	ＰＣＭ音源特殊コマンド処理
    //==============================================================================

    private Supplier<Object> commandsz() {
        pw.currentCommandTable = cmdtblz;
        pw.currentWriter = 3;
        r.bx = 0; // offset cmdtblp
        return pmd.command00();
    }

    private Supplier<Object>[] cmdtblz;

    private void SetupCmdtbl() {
        cmdtblz = new Supplier<Object>[] {
                this::comAtz                    //0xff(0)
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
                , pcmdrv::comvolupm          //0xf4(11)
                , pcmdrv::comvoldownm        //0xf3(12)
                , pmd::lfoset                //0xf2(13)
                , pmd::lfoswitch             //0xf1(14)
                , pmd::psgenvset             //0xf0(15)
                , pmd::comy                  //0xef(16)
                , pmd::jump1                 //0xee(17)
                , pmd::jump1                 //0xed(18)
                //
                , this::pansetz                   //0xec(19)
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
                //
                , pcmdrv::comvolupm2         //0xe3(28)
                , pcmdrv::comvoldownm2       //0xe2(29)
                //
                , pmd::jump1                 //0xe1(30)
                , pmd::jump1                 //0xe0(31)
                //
                , pmd::syousetu_lng_set      //0DFH(32)
                //
                , pmd::vol_one_up_pcm        //0deH(33)
                , pmd::vol_one_down          //0DDH(34)
                //
                , pmd::status_write          //0DCH(35)
                , pmd::status_add            //0DBH(36)
                //
                , this::portaz                    //0DAH(37)
                //
                , pmd::jump1                 //0D9H(38)
                , pmd::jump1                 //0D8H(39)
                , pmd::jump1                 //0D7H(40)
                //
                , pmd::mdepth_set            //0D6H(41)
                //
                , pmd::comdd                 //0d5h(42)
                //
                , pmd::ssg_efct_set          //0d4h(43)
                , pmd::fm_efct_set           //0d3h(44)
                , pmd::fade_set              //0d2h(45)
                //
                , pmd::jump1                 //0xd1(46)
                , pmd::jump1                 //0d0h(47)
                //
                , pmd::jump1                 //0cfh(48)
                , this::ppzrepeat_set             //0ceh(49)
                , pmd::extend_psgenvset      //0cdh(50)
                , pmd::jump1                 //0cch(51)
                , pmd::lfowave_set           //0cbh(52)
                , pmd::lfo_extend            //0cah(53)
                , pmd::envelope_extend       //0c9h(54)
                , pmd::jump3                 //0c8h(55)
                , pmd::jump3                 //0c7h(56)
                , pmd::jump6                 //0c6h(57)
                , pmd::jump1                 //0c5h(58)
                , pmd::comq2                 //0c4h(59)
                , this::pansetz_ex                //0c3h(60)
                , pmd::lfoset_delay          //0c2h(61)
                , pmd::jump0                 //0c1h,sular(62)
                , this::ppz_mml_part_mask         //0c0h(63)
                , pmd::_lfoset               //0bfh(64)
                , pmd::_lfoswitch            //0beh(65)
                , pmd::_mdepth_set           //0bdh(66)
                , pmd::_lfowave_set          //0bch(67)
                , pmd::_lfo_extend           //0bbh(68)
                , pmd::_volmask_set          //0bah(69)
                , pmd::_lfoset_delay         //0b9h(70)
                , pmd::jump2                 //0xb8(71)
                , pmd::mdepth_count          //0b7h(72)
                , pmd::jump1                 //0xb6(73)
                , pmd::jump2                 //0xb5(74)
                , pmd::jump16                //0b4h(75)
                , pmd::comq3                 //0b3h(76)
                , pmd::comshift_master       //0b2h(77)
                , pmd::comq4                 //0b1h(78)
        };
    }

    //284-316
    //==============================================================================
    //	ppz 拡張パートセット
    //==============================================================================
    public Supplier<Object> ppz_extpartset() {
        r.stack.push(r.di);
        r.di = (short) pw.part10a; // offset part10a
        r.cx = 8;
ppz_ex_loop:
        ;
        r.ax = (short) ((byte) pw.md[r.si].dat + (byte) pw.md[r.si + 1].dat * 0x100);
        r.si += 2;
        if (r.ax == 0)
            break no_init_ppz;
        r.ax += (short) pw.mmlbuf;
        pw.partWk[r.di].address = r.ax;

        pw.partWk[r.di].leng = 1; // アト 1カウント デ エンソウ カイシ
        r.al = 0xff; // -1
        pw.partWk[r.di].keyoff_flag = r.al; // 現在keyoff中
        pw.partWk[r.di].mdc = r.al; // MDepth Counter(無限)
        pw.partWk[r.di].mdc2 = r.al;
        pw.partWk[r.di]._mdc = r.al;
        pw.partWk[r.di]._mdc2 = r.al;
        pw.partWk[r.di].onkai = r.al; // rest
        pw.partWk[r.di].volume = (byte) 128; // PCM VOLUME DEFAULT = 128
        pw.partWk[r.di].fmpan = 5; // PAN = Middle

no_init_ppz:
        ;
        r.di++; // type qq
        r.cx--;
        if (r.cx != 0) break ppz_ex_loop;

ppzext_exit:
        ;
        r.di = r.stack.pop();
        return null;
    }


    //317-348
    private Supplier<Object> ppz_mml_part_mask() {
//#if DEBUG
        logger.log(Level.TRACE, "ppz_mml_part_mask");
//#endif

        r.al = (byte) pw.md[r.si++].dat;
        if (r.al >= 2)
            return pmd::special_0c0h;

        if (r.al == 0)
            break ppz_part_maskoff_ret;

        pw.partWk[r.di].partmask |= 0x40;
        if (pw.partWk[r.di].partmask != 0x40)
            break pmpz_ret;

        r.al = pw.partb;
        if (pw.ademu != 0) {
            if (r.al != 7)
                break pmpz_exec;
            if (pw.adpcm_emulate == 1)
                break pmpz_ret;
pmpz_exec:
            ;
        }
        r.ah = 2;
        ChipDatum cd = new ChipDatum(0x02, r.al, 0);
        cd.additionalData = pw.cmd;
        ppz8em.apply(cd); // .StopPCM(r.al);

pmpz_ret:
        ;
        //r.ax = r.stack.pop(); // commandsm
        return this::ppzmnp_1;

ppz_part_maskoff_ret:
        ;
        pw.partWk[r.di].partmask &= 0xbf;
        if (pw.partWk[r.di].partmask != 0)
            break pmpz_ret;
        //r.ax = r.stack.pop(); // commandsm
        return this::mp1z; // パート復活
    }


    //349-
    //==============================================================================
    //	リピート設定
    //==============================================================================
    private Supplier<Object> ppzrepeat_set() {
        ppz_voicetable_calc();

        r.dx = (short) (
                pcmData[bank] == null ? 0
                        : (pcmData[bank][ptr + 6] + pcmData[bank][ptr + 7] * 0x100));
        r.cx = (short) (
                pcmData[bank] == null ? 0
                        : (pcmData[bank][ptr + 4] + pcmData[bank][ptr + 5] * 0x100)); // dx: cx = データ量

        r.stack.push(r.si);
        r.stack.push(r.di);

        get_loop_ppz8();
        r.stack.push(r.ax);
        r.stack.push(r.bx);
        get_loop_ppz8();
        r.di = r.bx;
        r.si = r.ax;
        r.dx = r.stack.pop();
        r.cx = r.stack.pop();

        r.ah = 0xe;
        r.al = pw.partb;
        ChipDatum cd = new ChipDatum((r.al << 8) | 0x0e, ((r.dx << 16) | r.cx), ((r.di << 16) | r.si));
        ppz8em(cd); // .SetLoopPoint(r.al, r.dx, r.cx, r.di, r.si);
        r.di = r.stack.pop();
        r.si = r.stack.pop();
        r.si += 6;
        return null;
    }

    private void get_loop_ppz8() {
        r.bx = 0;
        r.ax = (short) ((byte) pw.md[r.si].dat + (byte) pw.md[r.si + 1].dat * 0x100);
        r.si += 2;
        if ((r.ax & 0x8000) == 0)
            break glp_ret;
        r.bx--;
        r.carry = (r.ax + r.cx) > 0xffff;
        r.ax += r.cx;
        r.bx += (short) (r.dx + (r.carry ? 1 : 0));
glp_ret:
        ;
        return;
    }

    private void ppz_voicetable_calc() {
        r.dx = 0;
        r.dl = pw.partWk[r.di].voicenum;

        r.ax = 0x040d;
        if ((r.dl & 0x80) == 0)
            break pvc_a;
        r.dl &= 0x7f;
        r.al++;
pvc_a:
        ;
        ChipDatum cd = new ChipDatum(0x04, r.al, 0);
        ppz8em(cd); // .ReadStatus(r.al); // in. ES: BX
        bank = r.al == 0xd ? 0 : 1; // ppz8em.bank;
        ptr = 0; // ppz8em.ptr;

        ptr += 0x20; // PZI Header Skip
        r.dx += r.dx;
        r.cx = r.dx;
        r.dx += r.dx;
        r.dx += r.dx;
        r.dx += r.dx;
        r.dx += r.cx; // x 12h
        ptr += r.dx;
    }


    //412-467
    //352-397
    //==============================================================================
    //	ポルタメント(PCM)
    //==============================================================================
    private Supplier<Object> portaz() {
        if (pw.partWk[r.di].partmask != 0) {
            //return pmd::porta_notset;
            r.al = (byte) pw.md[r.si++].dat; // 最初の音程を読み飛ばす(Mask時)
            return null;
        }

        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = pw.cmd;
        ppz8em.apply(cd);

        //pop ax; commandsp
        r.al = (byte) pw.md[r.si++].dat;
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetz();

        r.ax = pw.partWk[r.di].fnum;
        r.stack.push(r.ax);
        r.ax = pw.partWk[r.di].fnum2;
        r.stack.push(r.ax);
        r.al = pw.partWk[r.di].onkai;
        r.stack.push(r.ax);

        r.al = (byte) pw.md[r.si++].dat;
        pmd.oshift();
        fnumsetz();
        r.dx = pw.partWk[r.di].fnum2;
        r.ax = pw.partWk[r.di].fnum; // ax = ポルタメント先のdelta_n値

        r.bx = r.stack.pop();
        pw.partWk[r.di].onkai = r.bl;
        r.cx = r.stack.pop();
        pw.partWk[r.di].fnum2 = r.cx;
        r.bx = r.stack.pop(); // bx = ポルタメント元のdelta_n値
        pw.partWk[r.di].fnum = r.bx;

        r.carry = r.ax < r.bx;
        r.ax -= r.bx;
        r.dx -= (short) (r.cx + (r.carry ? 1 : 0)); // dx:ax = delta_n差

        for (int i = 0; i < 4; i++) {
            r.carry = (r.dx & 1) != 0;
            r.dx >>= 1;
            //boolean c = (r.ax & 1) != 0;
            r.ax = (short) ((r.carry ? 0x8000 : 0) | (r.ax >> 1)); // /16
            //r.carry = c;
        }

        r.bl = (byte) pw.md[r.si++].dat;
        pw.partWk[r.di].leng = r.bl;
        pmd.calc_q();

        r.bh = 0;
        int src = (short) r.ax;
        r.dx = (short) (src % (short) r.bx); // ax = delta_n差 / 音長
        r.ax = (short) (src / (short) r.bx);
        pw.partWk[r.di].porta_num2 = r.ax; // 商
        pw.partWk[r.di].porta_num3 = r.dx; // 余り
        pw.partWk[r.di].lfoswi |= 8; // Porta ON
        return porta_returnz;
    }


    //468-489
    //==============================================================================
    //	COMMAND 'p' [Panning Set]
    //		0=0	無音
    //		1=9	右
    //		2=1	左
    //		3=5	中央
    //==============================================================================
    private Supplier<Object> pansetz() {
        r.al = (byte) pw.md[r.si++].dat;
        r.bh = 0;
        r.bl = r.al;
        r.bx += 0; // offset ppzpandata
        r.al = pw.ppzpandata[r.bx];
        return this::pansetz_main;
    }

    private Supplier<Object> pansetz_main() {
        //IDE向け
        ChipDatum cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Pan, pw.cmd.linePos
                , (int) r.al
        );
        pmd.WriteDummy(cd);

        pw.partWk[r.di].fmpan = r.al;
        r.dx = 0;
        r.dl = r.al;
        r.ah = 0x13;
        r.al = pw.partb;
        cd = new ChipDatum(0x13, r.al, r.dx);
        ppz8em.apply(cd); // .SetPan(r.al, r.dx);
        return null;
    }


    //490-510
    //==============================================================================
    //	Pan setting Extend
    //		px -4～+4
    //==============================================================================
    private Supplier<Object> pansetz_ex() {
        r.al = (byte) pw.md[r.si++].dat;
        r.si++; // 逆相flagは読み飛ばす
        if ((r.al & 0x80) != 0)
            break pzex_minus;
        if (r.al < 5)
            break pzex_set;
        r.al = 4;
        break pzex_set;
pzex_minus:
        ;
        if (r.al >= 0xfc)
            break pzex_set;
        r.al = 0xfc;

pzex_set:
        ;
        r.al += 5;
        return this::pansetz_main;
    }


    //511-567
    //==============================================================================
    //	COMMAND '@' [NEIRO Change]
    //==============================================================================
    private Supplier<Object> comAtz() {
        Supplier<Object> ret = null;
        ChipDatum cd;

        r.al = (byte) pw.md[r.si++].dat;
        if (pw.ademu != 0) {
            if (pw.adpcm_emulate != 1)
                break cAtz_adchk_exit;
            if ((r.al & 0x80) == 0)
                break cAtz_partchk;
            r.al = 127; // ADPCMEmulate中は @128～なら @127に強制変更
cAtz_partchk:
            ;
            if (pw.partb != 7)
                break cAtz_adchk_exit;
            r.bx = (short) pw.part10; // PPZADEmuPart
            pw.partWk[r.bx].partmask |= 0x10; // Mask
            pw.partWk[r.bx].partmask &= 0xef; // Mask off
            if (pw.partWk[r.bx].partmask != 0)
                break cAtz_emuoff;
            //r.bx = r.stack.pop();
            ret = mp1z; // Part復活準備
            //r.stack.push(r.bx);
cAtz_emuoff:
            ;
            r.stack.push(r.ax);
            r.ax = 0x1800;
            pw.adpcm_emulate = r.al;
            cd = new ChipDatum(0x18, r.al, 0);
            ppz8em(cd); // .SetAdpcmEmu(r.al); // ADPCMEmulate OFF
            r.ax = r.stack.pop();
cAtz_adchk_exit:
            ;
        }
        pw.partWk[r.di].voicenum = r.al;

        //IDE向け
        cd = new ChipDatum(-1, -1, -1);
        cd.additionalData = new MmlDatum(-1, MMLType.Instrument, pw.cmd.linePos
                , (int) 0xff
                , (int) pw.partWk[r.di].voicenum
        );
        pmd.WriteDummy(cd);

ppz_neiro_reset:
        ;
        //    push es
        r.stack.push(r.si);
        r.stack.push(r.di);
        ppz_voicetable_calc();
        if (pcmData[bank] != null) {
            r.dx = (short) (pcmData[bank][ptr + 0xa] + pcmData[bank][ptr + 0xb] * 0x100);
            r.cx = (short) (pcmData[bank][ptr + 0x8] + pcmData[bank][ptr + 0x9] * 0x100); // dx: cx = Loop Start
            r.di = (short) (pcmData[bank][ptr + 0xe] + pcmData[bank][ptr + 0xf] * 0x100);
            r.si = (short) (pcmData[bank][ptr + 0xc] + pcmData[bank][ptr + 0xd] * 0x100); // dx: cx = Loop End
            r.ah = 0xe;
            r.al = pw.partb;
            //push es
            r.stack.push(r.bx);
            cd = new ChipDatum((r.al << 8) | 0x0e, ((r.dx << 16) | r.cx), ((r.di << 16) | r.si));
            ppz8em(cd); // .SetLoopPoint(r.al, r.dx, r.cx, r.di, r.si);
            r.bx = r.stack.pop();
            //pop es
            r.dx = (short) (pcmData[bank][ptr + 0x10] + pcmData[bank][ptr + 0x11] * 0x100); // dx = Frequency
            r.ah = 0x15;
            r.al = pw.partb;
            cd = new ChipDatum(0x15, r.al, r.dx);
            ppz8em(cd); // .SetSrcFrequency(r.al, r.dx);
        }
        r.di = r.stack.pop();
        r.si = r.stack.pop();
        //    pop es
cAtz_exit:
        ;
        return ret;
    }


    //568-
    //486-602
    //==============================================================================
    //	PPZ VOLUME SET
    //==============================================================================
    private void volsetz() {
        r.al = pw.partWk[r.di].volpush;
        if (r.al != 0)
            break vsz_01;
        r.al = pw.partWk[r.di].volume;
vsz_01:
        ;
        r.dl = r.al;
        //------------------------------------------------------------------------------
        //	音量down計算
        //------------------------------------------------------------------------------
        r.al = pw.ppz_voldown;
        if (r.al == 0)
            break ppz_fade_calc;
        r.al = (byte) -r.al;
        r.ax = (short) (r.al * r.dl);
        r.dl = r.ah;
        //------------------------------------------------------------------------------
        //	Fadeout計算
        //------------------------------------------------------------------------------
ppz_fade_calc:
        ;
        r.al = pw.fadeout_volume;
        if (r.al == 0)
            break ppz_env_calc;
        r.al = (byte) -r.al;
        r.ax = (short) (r.al * r.dl);
        r.dl = r.ah;
        //------------------------------------------------------------------------------
        //	ENVELOPE 計算
        //------------------------------------------------------------------------------
ppz_env_calc:
        ;
        r.al = r.dl;
        if (r.al == 0) // 音量0?
            break zv_out;
        if (pw.partWk[r.di].envf != 0xff) // -1
            break normal_zvset;
        // 拡張版 音量 = al * (eenv_vol + 1) / 16
        r.dl = pw.partWk[r.di].eenv_volume;
        if (r.dl == 0)
            break zv_min;
        r.dl++;
        r.ax = (short) (r.al * r.dl);
        r.ax >>= 3;
        r.carry = ((r.ax & 1) != 0);
        r.ax >>= 1;
        if (!r.carry) break zvset;
        r.ax++;
        break zvset;

normal_zvset:
        ;
        r.ah = pw.partWk[r.di].eenv_volume; // .penv;
        if ((r.ah & 0x80) == 0)
            break zvplus;
        // -
        r.ah = (byte) -r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.carry = r.al - r.ah < 0;
        r.al -= r.ah;
        if (!r.carry) break zvset;
zv_min:
        ;
        r.al = 0;
        break zv_out;
        // +
zvplus:
        ;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.ah += r.ah;
        r.carry = r.al + r.ah > 0xff;
        r.al += r.ah;
        if (!r.carry) break zvset;
        r.al = 255;
        //------------------------------------------------------------------------------
        //	音量LFO計算
        //------------------------------------------------------------------------------
zvset:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x22) == 0)
            break zv_out;
        r.dx = 0;
        r.ah = r.dl;
        if ((pw.partWk[r.di].lfoswi & 0x2) == 0)
            break zv_nolfo1;
        r.dx = pw.partWk[r.di].lfodat;
zv_nolfo1:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x20) == 0)
            break zv_nolfo2;
        r.dx += pw.partWk[r.di]._lfodat;
zv_nolfo2:
        ;
        if ((r.dx & 0x8000) != 0)
            break zvlfo_minus;
        r.ax += r.dx;
        if (r.ah == 0)
            break zv_out;
        r.al = 255;
        break zv_out;
zvlfo_minus:
        ;
        r.carry = r.ax + r.dx > 0xffff;
        r.ax += r.dx;
        if (r.carry) break zv_out;
        r.al = 0;

        //------------------------------------------------------------------------------
        //	出力
        //------------------------------------------------------------------------------
zv_out:
        ;
        if (r.al == 0)
            break zv_cut;
        r.dh = 0;
        r.dl = r.al;
        r.dx >>= 1;
        r.dx >>= 1;
        r.dx >>= 1;
        r.dx >>= 1;    // dx = volume(0～15)
        r.ah = 0x07;
        r.al = pw.partb;
        ChipDatum cd = new ChipDatum(0x07, r.al, r.dx);
        ppz8em(cd); // .SetVolume(r.al, r.dx);
        return;
zv_cut:
        ;
        r.ah = 0x02;
        r.al = pw.partb;
        cd = new ChipDatum(0x02, r.al, 0);
        cd.additionalData = pw.cmd;
        ppz8em(cd); // .StopPCM(r.al); // ; volume = 0... keyoff
        return;
    }


    //696-716
    //==============================================================================
    //	PPZ KEYON
    //==============================================================================
    private void keyonz() {
        if (pw.partWk[r.di].onkai == 0xff) //-1
            break keyonz_ret;

        //;	xor dx, dx
        //;	mov dl, fmpan[di]
        //;	mov ah,13h
        //;	mov al,[partb]
        //;	call ppz8_call

        r.ah = 1;
        r.al = pw.partb;
        r.dl = pw.partWk[r.di].voicenum;
        r.dh = r.dl;
        r.dx &= 0x807f; // dx=voicenum
        ChipDatum cd = new ChipDatum(0x01, r.al, r.dx);
        ppz8em(cd); // .PlayPCM(r.al, r.dx); // ppz keyon
keyonz_ret:
        ;
        return;
    }


    //717-731
    //==============================================================================
    //	ppz KEYOFF
    //==============================================================================
    private void keyoffz() {
        if (pw.partWk[r.di].envf == 0xff) // -1
            break kofz1_ext;
        if (pw.partWk[r.di].envf != 2) {
            pmd.keyoffp();
            return;
        }
kofz_ret:
        ;
        return;
kofz1_ext:
        ;
        if (pw.partWk[r.di].eenv_count == 4)
            break kofz_ret;
        pmd.keyoffp();
        return;
    }


    //732-
//==============================================================================
//	PPZ OTODASI
//==============================================================================
    private void otodasiz() {
        r.cx = pw.partWk[r.di].fnum;
        r.bx = pw.partWk[r.di].fnum2; // bx:cx=fnum
        r.ax = (short) (r.cx | r.bx);
        if (r.ax != 0)
            break odz_00;
        return;
odz_00:
        ;
        //
        // Portament/LFO/Detune SET
        //
        r.ax = pw.partWk[r.di].porta_num;
        if (r.ax == 0) break odz_not_porta;
        int a = (short) r.ax;
        a += a;
        a += a;
        a += a;
        a += a; // x16
        r.carry = (r.cx + (short) a) > 0xffff;
        r.cx += (short) a;
        r.bx += (short) ((a >> 16) + (r.carry ? 1 : 0));
odz_not_porta:
        ;
        r.ax = 0;
        if ((pw.partWk[r.di].lfoswi & 0x11) == 0)
            break odz_not_lfo;
        if ((pw.partWk[r.di].lfoswi & 0x1) == 0)
            break odz_not_lfo1;
        r.ax += pw.partWk[r.di].lfodat;
odz_not_lfo1:
        ;
        if ((pw.partWk[r.di].lfoswi & 0x10) == 0)
            break odz_not_lfo;
        r.ax += pw.partWk[r.di]._lfodat;
odz_not_lfo:
        ;
        r.ax += pw.partWk[r.di].detune;
        r.dl = r.ch;
        r.dh = r.bl;
        a = (short) r.ax * (short) r.dx;
        r.dx = (short) (a >> 16);
        r.ax = (short) a;
        if ((r.dx & 0x8000) != 0)
            break odz_minus;

        boolean c = r.cx + r.ax > 0xffff;
        r.cx += r.ax;
        r.carry = (r.bx + r.dx + (c ? 1 : 0)) > 0xffff;
        r.bx += (short) (r.dx + (c ? 1 : 0));
        if (!r.carry) break odz_main;
        r.cx = 0xffff; // -1
        r.bx = 0xffff;
        break odz_main;
odz_minus:
        ;
        r.carry = !((r.bx * 0x10000 + r.cx + a) < 0);
        a = (r.bx * 0x10000 + r.cx) + a;
        r.bx = (short) (a >> 16);
        r.cx = (short) a;
        if (r.carry) break odz_main;
        r.cx = 0;
        r.bx = 0;
        //
        // TONE SET
        //
odz_main:
        ;
        r.ah = 0x0b;
        r.al = pw.partb;
        r.dx = r.bx;
        ChipDatum cd = new ChipDatum(0x0b, r.al, (r.dx << 16) | r.cx);
        ppz8em(cd); // .SetFrequency(r.al, r.dx, r.cx);
    }


    //798-847
    //==============================================================================
    //	PPZ FNUM SET
    //==============================================================================
    private void fnumsetz() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrestz(); // 休符の場合
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
        //r.bx += r.bx;
        r.ax = pw.ppz_tune_data[r.bx]; // o5標準
        r.dx = 0;
        r.cl -= 4;
        if ((r.cl & 0x80) == 0) break ppz_over_o5;
        r.cl = (byte) -r.cl;
        r.ax = (short) (r.ax >> r.cl);
        break ppz_fnumset;
ppz_over_o5:
        ;
        if (r.cl == 0) break ppz_fnumset;
        r.ch = 0;
ppz_over_o5_loop:
        ;
        r.carry = (r.ax + r.ax) > 0xffff;
        r.ax += r.ax;
        r.dx += (short) (r.dx + (r.carry ? 1 : 0));
        r.cx--;
        if (r.cx != 0) break ppz_over_o5_loop;
ppz_fnumset:
        ;
        pw.partWk[r.di].fnum = r.ax;
        pw.partWk[r.di].fnum2 = r.dx;
    }

    private void fnrestz() {
        pw.partWk[r.di].onkai = 0xff;
        if ((pw.partWk[r.di].lfoswi & 0x11) != 0)
            break fnrz_ret;
        pw.partWk[r.di].fnum = 0;
        pw.partWk[r.di].fnum2 = 0;
fnrz_ret:
        ;
        return;
    }
}
