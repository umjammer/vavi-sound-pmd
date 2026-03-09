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

    private final PMD pmd;
    private final PW pw;
    private final X86Register r;
    private final Pc98 pc98;
    private final Function<ChipDatum, Integer> ppz8em;
    private final byte[][] pcmData;
    public PCMDRV pcmdrv = null;
    private int bank = 0;
    private int ptr = 0;

    private final Supplier<Object> mp1z_ = this::mp1z;
    private final Supplier<Object> mnp_ret_;
    private final Supplier<Object> porta_returnz_ = this::porta_returnz;
    private final Supplier<Object> _ppzmnp_1 = this::ppzmnp_1;

    public PPZDRV(PMD pmd, PW pw, X86Register r, Pc98 pc98, Function<ChipDatum, Integer> ppz8em, byte[][] pcmData) {
        this.pmd = pmd;
        mnp_ret_ = pmd::mnp_ret;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.ppz8em = ppz8em;
        this.pcmData = pcmData;
    }

    public void init() {
        SetupCmdtbl();
    }

    /**
     * PCM sound source performance main [PPZ8]
     */
    public void ppz8_call() {
        // Please call ppz8em directly if possible.
        throw new UnsupportedOperationException();
    }

    public void ppzmain() {
        r.setSi(pw.partWk[r.di & 0xffff].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0)
            return; // break pcmmain_ret;

        //if (r.si == pw.jumpIndex)
        //    pw.jumpIndex = -1; // KUMA:Added
        //logger.log(Level.TRACE, "%d", r.si);

        Supplier<Object> ret;
        if (pw.partWk[r.di & 0xffff].partmask != 0)
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
        // Duration - 1
        pw.partWk[r.di & 0xffff].leng--;
        r.al = pw.partWk[r.di & 0xffff].leng;

        // KEYOFF CHECK
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 3) == 0) { // break mp0z; // Have you already keyed off?
            if ((r.al & 0xff) <= (pw.partWk[r.di & 0xffff].qdat & 0xff)) { // break mp0z; // Q value => keyoff when remaining Length value
                pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
                keyoffz(); // AL will not break
            }
        }
//mp0z:
        // LENGTH CHECK
        if (r.al != 0) return this::mpexitz;
        return this::mp1z0;
    }

    private Supplier<Object> mp1z0() {
        pw.partWk[r.di & 0xffff].lfoswi &= (byte) 0xf7; // Porta off
        return mp1z_;
    }

    private Supplier<Object> mp1z() // DATA READ
    {
mp2z: // ↑
        {
            do {
                pw.cmd = pw.md[r.getSi() & 0xffff];

                //if (r.si == pw.jumpIndex)
                //pw.jumpIndex = -1; // KUMA:Added

                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                if ((r.al & 0xff) < 0x80) break mp2z;
                if (r.al == (byte) 0x80) break; // mp15z;

                // ELSE COMMANDS
                Object o = commandsz();
                while (o != null && o != mp1z_) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_ret_)
                        return mnp_ret_;
                    if (o == porta_returnz_)
                        return porta_returnz_;
                }
            } while (true);

            // END OF MUSIC[If there is an 'L', go back to it]
//mp15z:
            pmd.flashMacroList();

            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi(); // mov[di],si
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di & 0xffff].partloop);
            if (r.getBx() == 0) return this::mpexitz;

            // When there was an 'L'
            r.setSi(r.getBx());
            pw.partWk[r.di & 0xffff].loopcheck = 1;
            pw.partWk[r.di & 0xffff].loopCounter++;
            return mp1z_;
        }
//mp2z:
        // F - NUMBER SET
        pmd.flashMacroList();
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetz();

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = pw.cmd;
        ppz8em.apply(cd);

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].leng = r.al;
        pmd.calc_q();
        return porta_returnz_;
    }

    private Supplier<Object> porta_returnz() {
        if (pw.partWk[r.di & 0xffff].volpush != 0) { // break mp_newz;
            if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) { // break mp_newz;
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) { // break mp_newz;
                    pw.volpush_flag = 0;
                    pw.partWk[r.di & 0xffff].volpush = 0;
                }
            }
        }
//mp_newz:
        volsetz();
        otodasiz();
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 1) != 0) { // break mp3z;
            keyonz();
        }
//mp3z:
        pw.partWk[r.di & 0xffff].keyon_flag++;
        pw.partWk[r.di & 0xffff].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di & 0xffff].keyoff_flag = r.al;
        if (pw.md[r.getSi() & 0xffff].dat != 0xfb) // If there is an '&' immediately after, keyoff will not occur.
            return mnp_ret_;
        pw.partWk[r.di & 0xffff].keyoff_flag = 2;
        return mnp_ret_;
    }

    private Supplier<Object> mpexitz() {
        r.cl = pw.partWk[r.di & 0xffff].lfoswi;
        r.al = r.cl;
        r.al &= 8;
        pw.lfo_switch = r.al;
        if (r.cl != 0) { // break volsz;
            if ((r.cl & 3) != 0) { // break not_lfoz;

                pmd.lfo();
                if (r.carry) { // break not_lfoz;
                    r.al = r.cl;
                    r.al &= 3;
                    pw.lfo_switch |= r.al;
                }
            }
//not_lfoz:
            if ((r.cl & 0x30) != 0) { // break not_lfoz2;
                //pushf
                //cli
                pmd.lfo_change();
                pmd.lfo();
                if (r.carry) { // break not_lfoz1;
                    pmd.lfo_change();
                    //popf
                    r.al = pw.partWk[r.di & 0xffff].lfoswi;
                    r.al &= 0x30;
                    pw.lfo_switch |= r.al;
//                    break not_lfoz2;
                } else {
//not_lfoz1:
                    pmd.lfo_change();
                    //popf
                }
            }
//not_lfoz2:
            if ((pw.lfo_switch & 0x19) != 0) { // break volsz;
                if ((pw.lfo_switch & 8) != 0) { // break not_portaz;
                    pmd.porta_calc();
                }
//not_portaz:
                otodasiz();
            }
        }
//volsz:
        pmd.soft_env();
        if (!r.carry) { // break volsz2;
            if ((pw.lfo_switch & 0x22) == 0) { // break volsz2;
                if (pw.fadeout_speed == 0)
                    return mnp_ret_;
            }
        }
//volsz2:
        volsetz();
        return mnp_ret_;
    }

    /**
     * PCM sound source playback: When parts are masked
     */
    private Supplier<Object> ppzmain_nonplay() {
        pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
        pw.partWk[r.di & 0xffff].leng--;
        if (pw.partWk[r.di & 0xffff].leng != 0) return mnp_ret_;

        return _ppzmnp_1;
    }

    private Supplier<Object> ppzmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.incSi() & 0xffff];
                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                if (r.al == (byte) 0x80) break;
                if ((r.al & 0xff) < 0x80) return this::ppzmnp_3;

                Object o = commandsz();
                while (o != null && o != _ppzmnp_1) {
                    o = ((Supplier<Object>) o).get();
                    if (o == mnp_ret_)
                        return mnp_ret_;
                }
            } while (true);

            pmd.flashMacroList();

//pcmmnp_2:
            // END OF MUSIC[When there was an "L" I went back there]
            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi();
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di & 0xffff].partloop);

            if ((r.getBx() & r.getBx()) == 0) return pmd::fmmnp_4;

            // When there was an "L"
            r.setSi(r.getBx());
            pw.partWk[r.di & 0xffff].loopcheck = 1;
            pw.partWk[r.di & 0xffff].loopCounter++;
        } while (true);
    }

    private Supplier<Object> ppzmnp_3() {
        pw.partWk[r.di & 0xffff].fnum2 = 0;
        return pmd::fmmnp_3;
    }

    /**
     * PCM sound source special command processing
     */
    private Supplier<Object> commandsz() {
        pw.currentCommandTable = cmdtblz;
        pw.currentWriter = 3;
        r.setBx((short) 0); // offset cmdtblp
        return pmd.command00();
    }

    private Supplier<Object>[] cmdtblz;

    private void SetupCmdtbl() {
        cmdtblz = new Supplier[] {
                this::comAtz,            // 0xff(0)
                pmd::comq,               // 0xfe(1)
                pmd::comv,               // 0xfd(2)
                pmd::comt,               // 0xfc(3)
                pmd::comtie,             // 0xfb(4)
                pmd::comd,               // 0xfa(5)
                pmd::comstloop,          // 0xf9(6)
                pmd::comedloop,          // 0xf8(7)
                pmd::comexloop,          // 0xf7(8)
                pmd::comlopset,          // 0xf6(9)
                pmd::comshift,           // 0xf5(10)
                pcmdrv::comvolupm,       // 0xf4(11)
                pcmdrv::comvoldownm,     // 0xf3(12)
                pmd::lfoset,             // 0xf2(13)
                pmd::lfoswitch,          // 0xf1(14)
                pmd::psgenvset,          // 0xf0(15)
                pmd::comy,               // 0xef(16)
                pmd::jump1,              // 0xee(17)
                pmd::jump1,              // 0xed(18)
                //
                this::pansetz,           // 0xec(19)
                pmd::rhykey,             // 0xeb(20)
                pmd::rhyvs,              // 0xea(21)
                pmd::rpnset,             // 0xe9(22)
                pmd::rmsvs,              // 0xe8(23)
                //
                pmd::comshift2,          // 0xe7(24)
                pmd::rmsvs_sft,          // 0xe6(25)
                pmd::rhyvs_sft,          // 0xe5(26)
                //
                pmd::jump1,              // 0xe4(27)
                //
                pcmdrv::comvolupm2,      // 0xe3(28)
                pcmdrv::comvoldownm2,    // 0xe2(29)
                //
                pmd::jump1,              // 0xe1(30)
                pmd::jump1,              // 0xe0(31)
                //
                pmd::syousetu_lng_set,   // 0DFH(32)
                //
                pmd::vol_one_up_pcm,     // 0deH(33)
                pmd::vol_one_down,       // 0DDH(34)
                //
                pmd::status_write,       // 0DCH(35)
                pmd::status_add,         // 0DBH(36)
                //
                this::portaz,            // 0DAH(37)
                //
                pmd::jump1,              // 0D9H(38)
                pmd::jump1,              // 0D8H(39)
                pmd::jump1,              // 0D7H(40)
                //
                pmd::mdepth_set,         // 0D6H(41)
                //
                pmd::comdd,              // 0d5h(42)
                //
                pmd::ssg_efct_set,       // 0d4h(43)
                pmd::fm_efct_set,        // 0d3h(44)
                pmd::fade_set,           // 0d2h(45)
                //
                pmd::jump1,              // 0xd1(46)
                pmd::jump1,              // 0d0h(47)
                //
                pmd::jump1,              // 0cfh(48)
                this::ppzrepeat_set,     // 0ceh(49)
                pmd::extend_psgenvset,   // 0cdh(50)
                pmd::jump1,              // 0cch(51)
                pmd::lfowave_set,        // 0cbh(52)
                pmd::lfo_extend,         // 0cah(53)
                pmd::envelope_extend,    // 0c9h(54)
                pmd::jump3,              // 0c8h(55)
                pmd::jump3,              // 0c7h(56)
                pmd::jump6,              // 0c6h(57)
                pmd::jump1,              // 0c5h(58)
                pmd::comq2,              // 0c4h(59)
                this::pansetz_ex,        // 0c3h(60)
                pmd::lfoset_delay,       // 0c2h(61)
                pmd::jump0,              // 0c1h, sular(62)
                this::ppz_mml_part_mask, // 0c0h(63)
                pmd::_lfoset,            // 0bfh(64)
                pmd::_lfoswitch,         // 0beh(65)
                pmd::_mdepth_set,        // 0bdh(66)
                pmd::_lfowave_set,       // 0bch(67)
                pmd::_lfo_extend,        // 0bbh(68)
                pmd::_volmask_set,       // 0bah(69)
                pmd::_lfoset_delay,      // 0b9h(70)
                pmd::jump2,              // 0xb8(71)
                pmd::mdepth_count,       // 0b7h(72)
                pmd::jump1,              // 0xb6(73)
                pmd::jump2,              // 0xb5(74)
                pmd::jump16,             // 0b4h(75)
                pmd::comq3,              // 0b3h(76)
                pmd::comshift_master,    // 0b2h(77)
                pmd::comq4               // 0b1h(78)
        };
    }

    /**
     * ppz Extended Part Set
     */
    public Supplier<Object> ppz_extpartset() {
        r.stack.push(r.di);
        r.di = (short) pw.part10a; // offset part10a
        r.setCx((short) 8);
//ppz_ex_loop:
        do {
            r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
            r.addSi((short) 2);
            if (r.getAx() != 0) { // break no_init_ppz;
                r.addAx((short) pw.mmlbuf);
                pw.partWk[r.di & 0xffff].address = r.getAx();

                pw.partWk[r.di & 0xffff].leng = 1; // Play begins in 1 count
                r.al = (byte) 0xff; // -1
                pw.partWk[r.di & 0xffff].keyoff_flag = r.al; // Currently being keyed off
                pw.partWk[r.di & 0xffff].mdc = r.al; // MDepth Counter(Infinite)
                pw.partWk[r.di & 0xffff].mdc2 = r.al;
                pw.partWk[r.di & 0xffff]._mdc = r.al;
                pw.partWk[r.di & 0xffff]._mdc2 = r.al;
                pw.partWk[r.di & 0xffff].onkai = r.al; // rest
                pw.partWk[r.di & 0xffff].volume = (byte) 128; // PCM VOLUME DEFAULT = 128
                pw.partWk[r.di & 0xffff].fmpan = 5; // PAN = Middle
            }
//no_init_ppz:
            r.di++; // type qq
            r.decCx();
        } while (r.getCx() != 0); // break ppz_ex_loop;
//ppzext_exit:
        r.di = r.stack.pop();
        return null;
    }

    private Supplier<Object> ppz_mml_part_mask() {
//#if DEBUG
        logger.log(Level.TRACE, "ppz_mml_part_mask");
//#endif

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) >= 2)
            return pmd::special_0c0h;

        if (r.al != 0) { // break ppz_part_maskoff_ret;

            pw.partWk[r.di & 0xffff].partmask |= 0x40;
pmpz_ret: // ↑
            if (pw.partWk[r.di & 0xffff].partmask == 0x40) { // break pmpz_ret;

                r.al = pw.partb;
                if (pw.ademu != 0) {
                    if (r.al == 7) { // break pmpz_exec;
                        if (pw.adpcm_emulate == 1)
                            break pmpz_ret;
                    }
//pmpz_exec:
                }
                r.ah = 2;
                ChipDatum cd = new ChipDatum(0x02, r.al & 0xff, 0);
                cd.additionalData = pw.cmd;
                ppz8em.apply(cd); // .StopPCM(r.al);
            }
//pmpz_ret:
            //r.ax = r.stack.pop(); // commandsm
            return _ppzmnp_1;
        }
//ppz_part_maskoff_ret:
        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
//            break pmpz_ret;
            return _ppzmnp_1;
        }
        //r.ax = r.stack.pop(); // commandsm
        return mp1z_; // restore the part
    }

    /**
     * Repeat Settings
     */
    private Supplier<Object> ppzrepeat_set() {
        ppz_voicetable_calc();

        r.setDx((short) (
                pcmData[bank] == null ? 0
                        : ((pcmData[bank][ptr + 6] & 0xff) + (pcmData[bank][ptr + 7] & 0xff) * 0x100)));
        r.setCx((short) (
                pcmData[bank] == null ? 0
                        : ((pcmData[bank][ptr + 4] & 0xff) + (pcmData[bank][ptr + 5] & 0xff) * 0x100))); // dx: cx = Data volume

        r.stack.push(r.getSi());
        r.stack.push(r.di);

        get_loop_ppz8();
        r.stack.push(r.getAx());
        r.stack.push(r.getBx());
        get_loop_ppz8();
        r.di = r.getBx();
        r.setSi(r.getAx());
        r.setDx(r.stack.pop());
        r.setCx(r.stack.pop());

        r.ah = 0xe;
        r.al = pw.partb;
        ChipDatum cd = new ChipDatum(((r.al & 0xff) << 8) | 0x0e, (((r.getDx() & 0xffff) << 16) | (r.getCx() & 0xffff)), (((r.di & 0xffff) << 16) | (r.getSi() & 0xffff)));
        ppz8em.apply(cd); // .SetLoopPoint(r.al, r.dx, r.cx, r.di, r.si);
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        r.addSi((short) 6);
        return null;
    }

    private void get_loop_ppz8() {
        r.setBx((short) 0);
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
        r.addSi((short) 2);
        if ((r.getAx() & 0x8000) != 0) { // break glp_ret;
            r.decBx();
            r.carry = ((r.getAx() & 0xffff) + (r.getCx() & 0xffff)) > 0xffff;
            r.addAx(r.getCx());
            r.addBx((short) ((r.getDx() & 0xffff) + (r.carry ? 1 : 0)));
        }
//glp_ret:
    }

    private void ppz_voicetable_calc() {
        r.setDx((short) 0);
        r.dl = pw.partWk[r.di & 0xffff].voicenum;

        r.setAx((short) 0x040d);
        if ((r.dl & 0x80) != 0) { // break pvc_a;
            r.dl &= 0x7f;
            r.al++;
        }
//pvc_a:
        ChipDatum cd = new ChipDatum(0x04, r.al & 0xff, 0);
        ppz8em.apply(cd); // .ReadStatus(r.al); // in. ES: BX
        bank = r.al == 0xd ? 0 : 1; // ppz8em.bank;
        ptr = 0; // ppz8em.ptr;

        ptr += 0x20; // PZI Header Skip
        r.addDx(r.getDx());
        r.setCx(r.getDx());
        r.addDx(r.getDx());
        r.addDx(r.getDx());
        r.addDx(r.getDx());
        r.addDx(r.getCx()); // x 12h
        ptr += r.getDx() & 0xffff;
    }

    /**
     * Portamento (PCM)
     */
    private Supplier<Object> portaz() {
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
            //return pmd::porta_notset;
            r.al = (byte) pw.md[r.incSi() & 0xffff].dat; // Skip the first note (when masked)
            return null;
        }

        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = pw.cmd;
        ppz8em.apply(cd);

        //pop ax; commandsp
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetz();

        r.setAx(pw.partWk[r.di & 0xffff].fnum);
        r.stack.push(r.getAx());
        r.setAx(pw.partWk[r.di & 0xffff].fnum2);
        r.stack.push(r.getAx());
        r.al = pw.partWk[r.di & 0xffff].onkai;
        r.stack.push(r.getAx());

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pmd.oshift();
        fnumsetz();
        r.setDx(pw.partWk[r.di & 0xffff].fnum2);
        r.setAx(pw.partWk[r.di & 0xffff].fnum); // ax = delta_n value of portamento destination

        r.setBx(r.stack.pop());
        pw.partWk[r.di & 0xffff].onkai = r.bl;
        r.setCx(r.stack.pop());
        pw.partWk[r.di & 0xffff].fnum2 = r.getCx();
        r.setBx(r.stack.pop()); // bx = delta_n value of the portamento source
        pw.partWk[r.di & 0xffff].fnum = r.getBx();

        r.carry = (r.getAx() & 0xffff) < (r.getBx() & 0xffff);
        r.subAx(r.getBx());
        r.subDx((short) ((r.getCx() & 0xffff) + (r.carry ? 1 : 0))); // dx:ax = delta_n difference

        for (int i = 0; i < 4; i++) {
            r.carry = (r.getDx() & 1) != 0;
            r.srDx(1);
            //boolean c = (r.ax & 1) != 0;
            r.setAx((short) ((r.carry ? 0x8000 : 0) | ((r.getAx() & 0xffff) >> 1))); // /16
            //r.carry = c;
        }

        r.bl = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].leng = r.bl;
        pmd.calc_q();

        r.bh = 0;
        int src = /* signed */ r.getAx();
        r.setDx((short) (src % /* signed */ r.getBx())); // ax = delta_n difference / note length
        r.setAx((short) (src / /* signed */ r.getBx()));
        pw.partWk[r.di & 0xffff].porta_num2 = r.getAx(); // quotient
        pw.partWk[r.di & 0xffff].porta_num3 = r.getDx(); // remainder
        pw.partWk[r.di & 0xffff].lfoswi |= 8; // Porta ON
        return porta_returnz_;
    }

    /**
     * COMMAND 'p' [Panning Set]
     *  0=0 Silence
     *  1=9 right
     *  2=1 left
     *  3=5 Center
     */
    private Supplier<Object> pansetz() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.bh = 0;
        r.bl = r.al;
        r.addBx((short) 0); // offset ppzpandata
        r.al = (byte) pw.ppzpandata[r.getBx() & 0xffff];
        return this::pansetz_main;
    }

    private Supplier<Object> pansetz_main() {
        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0xff, MMLType.Pan, pw.cmd.linePos, r.al & 0xff);
        pmd.writeDummy(cd);

        pw.partWk[r.di & 0xffff].fmpan = r.al;
        r.setDx((short) 0);
        r.dl = r.al;
        r.ah = 0x13;
        r.al = pw.partb;
        cd = new ChipDatum(0x13, r.al & 0xff, r.getDx() & 0xffff);
        ppz8em.apply(cd); // .SetPan(r.al, r.dx);
        return null;
    }

    /**
     * Pan setting Extend
     *  px -4～+4
     */
    private Supplier<Object> pansetz_ex() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.incSi(); // The reverse phase flag is skipped.
        if ((r.al & 0x80) == 0) { // break pzex_minus;
            if ((r.al & 0xff) >= 5) { // break pzex_set;
                r.al = 4;
            }
//            break pzex_set;
        } else {
//pzex_minus:
            if ((r.al & 0xff) < 0xfc) { // break pzex_set;
                r.al = (byte) 0xfc;
            }
        }
//pzex_set:
        r.al += 5;
        return this::pansetz_main;
    }

    /**
     * COMMAND '@' [NEIRO Change]
     */
    private Supplier<Object> comAtz() {
        Supplier<Object> ret = null;
        ChipDatum cd;

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if (pw.ademu != 0) {
            if (pw.adpcm_emulate == 1) { // break cAtz_adchk_exit;
                if ((r.al & 0x80) != 0) { // break cAtz_partchk;
                    r.al = 127; // During ADPCMEmulate, @128~ is forcibly changed to @127
                }
//cAtz_partchk:
                if (pw.partb == 7) { // break cAtz_adchk_exit;
                    r.setBx((short) pw.part10); // PPZADEmuPart
                    pw.partWk[r.getBx() & 0xffff].partmask |= 0x10; // Mask
                    pw.partWk[r.getBx() & 0xffff].partmask &= (byte) 0xef; // Mask off
                    if (pw.partWk[r.getBx() & 0xffff].partmask == 0) { // break cAtz_emuoff;
                        //r.bx = r.stack.pop();
                        ret = mp1z_; // Part revival preparation
                        //r.stack.push(r.bx);
                    }
//cAtz_emuoff:
                    r.stack.push(r.getAx());
                    r.setAx((short) 0x1800);
                    pw.adpcm_emulate = r.al;
                    cd = new ChipDatum(0x18, r.al & 0xff, 0);
                    ppz8em.apply(cd); // .SetAdpcmEmu(r.al); // ADPCMEmulate OFF
                    r.setAx(r.stack.pop());
                }
            }
//cAtz_adchk_exit:
        }
        pw.partWk[r.di & 0xffff].voicenum = r.al;

        // For IDEs
        cd = new ChipDatum(-1, 0xff, 0xff);
        cd.additionalData = new MmlDatum(0xff, MMLType.Instrument, pw.cmd.linePos, 0xff, pw.partWk[r.di & 0xffff].voicenum & 0xff);
        pmd.writeDummy(cd);

//ppz_neiro_reset:
        //    push es
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        ppz_voicetable_calc();
        if (pcmData[bank] != null) {
            r.setDx((short) ((pcmData[bank][ptr + 0xa] & 0xff) + (pcmData[bank][ptr + 0xb] & 0xff) * 0x100));
            r.setCx((short) ((pcmData[bank][ptr + 0x8] & 0xff) + (pcmData[bank][ptr + 0x9] & 0xff) * 0x100)); // dx: cx = Loop Start
            r.di = (short) ((pcmData[bank][ptr + 0xe] & 0xff) + (pcmData[bank][ptr + 0xf] & 0xff) * 0x100);
            r.setSi((short) ((pcmData[bank][ptr + 0xc] & 0xff) + (pcmData[bank][ptr + 0xd] & 0xff) * 0x100)); // dx: cx = Loop End
            r.ah = 0xe;
            r.al = pw.partb;
            //push es
            r.stack.push(r.getBx());
            cd = new ChipDatum(((r.al & 0xff) << 8) | 0x0e, (((r.getDx() & 0xffff) << 16) | (r.getCx() & 0xffff)), (((r.di & 0xffff) << 16) | (r.getSi() & 0xffff)));
            ppz8em.apply(cd); // .SetLoopPoint(r.al, r.dx, r.cx, r.di, r.si);
            r.setBx(r.stack.pop());
            //pop es
            r.setDx((short) ((pcmData[bank][ptr + 0x10] & 0xff) + (pcmData[bank][ptr + 0x11] & 0xff) * 0x100)); // dx = Frequency
            r.ah = 0x15;
            r.al = pw.partb;
            cd = new ChipDatum(0x15, r.al & 0xff, r.getDx() & 0xffff);
            ppz8em.apply(cd); // .SetSrcFrequency(r.al, r.dx);
        }
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        //    pop es
//cAtz_exit:
        return ret;
    }

    /**
     * PPZ VOLUME SET
     */
    private void volsetz() {
        r.al = pw.partWk[r.di & 0xffff].volpush;
        if (r.al == 0) { // break vsz_01;
            r.al = pw.partWk[r.di & 0xffff].volume;
        }
//vsz_01:
        r.dl = r.al;
        //
        // Volume down calculation
        //
        r.al = pw.ppz_voldown;
        if (r.al != 0) { // break ppz_fade_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // Fadeout calculation
        //
//ppz_fade_calc:
        r.al = pw.fadeout_volume;
        if (r.al != 0) { // break ppz_env_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // ENVELOPE Calculation
        //
//ppz_env_calc:
zv_out: // ↑
        {
            r.al = r.dl;
            if (r.al != 0) { // Volume 0? // break zv_out;
                if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // -1 // break normal_zvset;
                    // Extended volume = al * (eenv_vol + 1) / 16
                    r.dl = pw.partWk[r.di & 0xffff].eenv_volume;
                    if (r.dl == 0) {
//                    break zv_min;
                        r.al = 0; // <<
                        break zv_out; // <<
                    }
                    r.dl++;
                    r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
                    r.srAx(3);
                    r.carry = ((r.getAx() & 1) != 0);
                    r.srAx(1);
                    if (r.carry) { // break zvset;
                        r.incAx();
                    }
//                break zvset;
                } else {
//normal_zvset:
                    r.ah = pw.partWk[r.di & 0xffff].eenv_volume; // .penv;
                    if ((r.ah & 0x80) != 0) { // break zvplus;
                        // -
                        r.ah = (byte) -r.ah;
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.carry = (r.al & 0xff) - (r.ah & 0xff) < 0;
                        r.al -= r.ah;
                        if (r.carry) { // break zvset;
//zv_min:
                            r.al = 0;
                            break zv_out;
                        }
                    } else {
                        // +
//zvplus:
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.ah += r.ah;
                        r.carry = (r.al & 0xff) + (r.ah & 0xff) > 0xff;
                        r.al += r.ah;
                        if (r.carry) { // break zvset;
                            r.al = (byte) 255;
                        }
                    }
                }
                //
                // Volume LFO Calculation
                //
//zvset:
                if ((pw.partWk[r.di & 0xffff].lfoswi & 0x22) != 0) { // break zv_out;
                    r.setDx((short) 0);
                    r.ah = r.dl;
                    if ((pw.partWk[r.di & 0xffff].lfoswi & 0x2) != 0) { // break zv_nolfo1;
                        r.setDx(pw.partWk[r.di & 0xffff].lfodat);
                    }
//zv_nolfo1:
                    if ((pw.partWk[r.di & 0xffff].lfoswi & 0x20) != 0) { // break zv_nolfo2;
                        r.addDx(pw.partWk[r.di & 0xffff]._lfodat);
                    }
//zv_nolfo2:
                    if ((r.getDx() & 0x8000) == 0) { // break zvlfo_minus;
                        r.addAx(r.getDx());
                        if (r.ah != 0) { // break zv_out;
                            r.al = (byte) 255;
                        }
//                break zv_out;
                    } else {
//zvlfo_minus:
                        r.carry = (r.getAx() & 0xffff) + (r.getDx() & 0xffff) > 0xffff;
                        r.addAx(r.getDx());
                        if (!r.carry) { // break zv_out;
                            r.al = 0;
                        }
                    }
                }
            }
        }
        //
        // output
        //
//zv_out:
        if (r.al != 0) { // break zv_cut;
            r.dh = 0;
            r.dl = r.al;
            r.srDx(1);
            r.srDx(1);
            r.srDx(1);
            r.srDx(1);    // dx = volume(0～15)
            r.ah = 0x07;
            r.al = pw.partb;
            ChipDatum cd = new ChipDatum(0x07, r.al & 0xff, r.getDx() & 0xffff);
            ppz8em.apply(cd); // .SetVolume(r.al, r.dx);
            return;
        }
//zv_cut:
        r.ah = 0x02;
        r.al = pw.partb;
        ChipDatum cd = new ChipDatum(0x02, r.al & 0xff, 0);
        cd.additionalData = pw.cmd;
        ppz8em.apply(cd); // .StopPCM(r.al); // ; volume = 0... keyoff
    }

    /**
     * PPZ KEYON
     */
    private void keyonz() {
        if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) { //-1 // break keyonz_ret;

            //; xor dx, dx
            //; mov dl, fmpan[di]
            //; mov ah,13h
            //; mov al,[partb]
            //; call ppz8_call

            r.ah = 1;
            r.al = pw.partb;
            r.dl = pw.partWk[r.di & 0xffff].voicenum;
            r.dh = r.dl;
            r.andDx((short) 0x807f); // dx=voicenum
            ChipDatum cd = new ChipDatum(0x01, r.al & 0xff, r.getDx() & 0xffff);
            ppz8em.apply(cd); // .PlayPCM(r.al, r.dx); // ppz keyon
        }
//keyonz_ret:
    }

    /**
     * ppz KEYOFF
     */
    private void keyoffz() {
        if (pw.partWk[r.di & 0xffff].envf != (byte) 0xff) { // -1 // break kofz1_ext;
            if (pw.partWk[r.di & 0xffff].envf != 2) {
                pmd.keyoffp();
//                return;
            }
//kofz_ret:
            return;
        }
//kofz1_ext:
        if (pw.partWk[r.di & 0xffff].eenv_count == 4) {
//            break kofz_ret;
            return;
        }
        pmd.keyoffp();
    }

    /**
     * PPZ OTODASI
     */
    private void otodasiz() {
        r.setCx(pw.partWk[r.di & 0xffff].fnum);
        r.setBx(pw.partWk[r.di & 0xffff].fnum2); // bx:cx=fnum
        r.setAx((short) (r.getCx() | r.getBx()));
        if (r.getAx() == 0) { // break odz_00;
            return;
        }
//odz_00:
        //
        // Portament/LFO/Detune SET
        //
        r.setAx(pw.partWk[r.di & 0xffff].porta_num);
        if (r.getAx() != 0) { // break odz_not_porta;
            int a = /* signed */ r.getAx();
            a += a;
            a += a;
            a += a;
            a += a; // x16
            r.carry = (r.getCx() & 0xffff) + (a & 0xffff) > 0xffff;
            r.addCx((short) a);
            r.addBx((short) ((a >> 16) + (r.carry ? 1 : 0)));
        }
//odz_not_porta:
        r.setAx((short) 0);
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x11) != 0) { // break odz_not_lfo;
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x1) != 0) { // break odz_not_lfo1;
                r.addAx(pw.partWk[r.di & 0xffff].lfodat);
            }
//odz_not_lfo1:
            if ((pw.partWk[r.di & 0xffff].lfoswi & 0x10) != 0) { // break odz_not_lfo;
                r.addAx(pw.partWk[r.di & 0xffff]._lfodat);
            }
        }
//odz_not_lfo:
        r.addAx(pw.partWk[r.di & 0xffff].detune);
        r.dl = r.ch;
        r.dh = r.bl;
        int a = /* signed */ r.getAx() * (r.getDx() & 0xffff);
        r.setDx((short) (a >> 16));
        r.setAx((short) a);
        if ((r.getDx() & 0x8000) == 0) { // break odz_minus;

            boolean c = (r.getCx() & 0xffff) + (r.getAx() & 0xffff) > 0xffff;
            r.addCx(r.getAx());
            r.carry = (r.getBx() & 0xffff) + (r.getDx() & 0xffff) + (c ? 1 : 0) > 0xffff;
            r.addBx((short) ((r.getDx() & 0xffff) + (c ? 1 : 0)));
            if (r.carry) { // break odz_main;
                r.setCx((short) 0xffff); // -1
                r.setBx((short) 0xffff);
            }
//            break odz_main;
        } else {
//odz_minus:
            r.carry = !((r.getBx() & 0xffff) * 0x1_0000 + (r.getCx() & 0xffff) + a < 0);
            a = (r.getBx() & 0xffff) * 0x10000 + (r.getCx() & 0xffff) + a;
            r.setBx((short) (a >>> 16));
            r.setCx((short) a);
            if (!r.carry) { // break odz_main;
                r.setCx((short) 0);
                r.setBx((short) 0);
            }
        }
        //
        // TONE SET
        //
//odz_main:
        r.ah = 0x0b;
        r.al = pw.partb;
        r.setDx(r.getBx());
        ChipDatum cd = new ChipDatum(0x0b, r.al & 0xff, ((r.getDx() & 0xffff) << 16) | (r.getCx() & 0xffff));
        ppz8em.apply(cd); // .SetFrequency(r.al, r.dx, r.cx);
    }

    /**
     * PPZ FNUM SET
     */
    private void fnumsetz() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            fnrestz(); // Rests
            return;
        }
        pw.partWk[r.di & 0xffff].onkai = r.al;
        r.bh = 0;
        r.bl = r.ah; // bx = onkai
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al = r.ror(r.al, 1);
        r.al &= 0xf;
        r.cl = r.al; // cl=octarb
        //r.bx += r.bx;
        r.setAx((short) pw.ppz_tune_data[r.getBx() & 0xffff]); // o5 standard
        r.setDx((short) 0);
        r.cl -= 4;
        if ((r.cl & 0x80) != 0) { // break ppz_over_o5;
            r.cl = (byte) -r.cl;
            r.setAx((short) ((r.getAx() & 0xffff) >> (r.cl & 0xff)));
//            break ppz_fnumset;
        } else {
//ppz_over_o5:
            if (r.cl != 0) { // break ppz_fnumset;
                r.ch = 0;
//ppz_over_o5_loop:
                do {
                    r.carry = (r.getAx() & 0xffff) + (r.getAx() & 0xffff) > 0xffff;
                    r.addAx(r.getAx());
                    r.setDx((short) ((r.getDx() & 0xffff) + (r.getDx() & 0xffff) + (r.carry ? 1 : 0)));
                    r.decCx();
                } while (r.getCx() != 0); // break ppz_over_o5_loop;
            }
        }
//ppz_fnumset:
        pw.partWk[r.di & 0xffff].fnum = r.getAx();
        pw.partWk[r.di & 0xffff].fnum2 = r.getDx();
    }

    private void fnrestz() {
        pw.partWk[r.di & 0xffff].onkai = (byte) 0xff;
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x11) == 0) { // break fnrz_ret;
            pw.partWk[r.di & 0xffff].fnum = 0;
            pw.partWk[r.di & 0xffff].fnum2 = 0;
        }
//fnrz_ret:
    }
}
