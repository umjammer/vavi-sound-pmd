package pmd.driver;

import java.util.function.Function;
import java.util.function.Supplier;

import musicDriverInterface.ChipDatum;
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;


public class PCMDRV86 {

    private final PMD pmd;
    private final PW pw;
    private final X86Register r;
    private final Pc98 pc98;
    private final Function<ChipDatum, Integer> p86drv;
    private Runnable[] trans_table;
    private final byte[][] pcmData;

    public PCMDRV86(PMD pmd, PW pw, X86Register r, Pc98 pc98, Function<ChipDatum, Integer> p86drv, byte[][] pcmData) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.p86drv = p86drv;
        this.pcmData = pcmData;

        setupCmdtbl();
    }

    /**
     * PCM sound source performance main (86B PCM)
     */
    //pcmmain_ret:
    // ret
    public void pcmmain() {
        r.setSi(pw.partWk[r.di & 0xffff].address); // si = PART DATA ADDRESS
        if (r.getSi() == 0)
            return;

//        Supplier<Object> ret = null;
//        if (pw.partWk[r.di & 0xffff].partmask != 0)
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
        // Duration -1
        pw.partWk[r.di & 0xffff].leng--;
        r.al = pw.partWk[r.di & 0xffff].leng;

        // ; KEYOFF CHECK
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 3) != 0) { // Have you already keyed off? // break mp0m;

            if ((r.al & 0xff) <= (pw.partWk[r.di & 0xffff].qdat & 0xff)) { // Q value => keyoff when remaining Length value // break mp0m;
//mp00m:
                keyoffm(); // AL will not break
                pw.partWk[r.di & 0xffff].keyoff_flag = (byte) 0xff; // -1
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
            pw.cmd = pw.md[r.getSi() & 0xffff];
            r.al = (byte) pw.md[r.getSi() & 0xffff].dat;

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

        // END OF MUSIC[If there is an 'L', go back to it]
//mp15m:
        if ((r.al & 0xff) >= 0x80) { // break mp2m;
            pmd.flashMacroList();

            r.decSi();
            pw.partWk[r.di & 0xffff].address = r.getSi(); // mov[di],si
            pw.partWk[r.di & 0xffff].loopcheck = 3;
            pw.partWk[r.di & 0xffff].onkai = (byte) 0xff; // -1
            r.setBx(pw.partWk[r.di & 0xffff].partloop);
            if (r.getBx() == 0) return this::mpexitm;

            // When there was an 'L'
            r.setSi(r.getBx());

            pw.partWk[r.di & 0xffff].loopcheck = 1;
            pw.partWk[r.di & 0xffff].loopCounter++;
            return this::mp1m;
        }
//mp2m:
        // F - NUMBER SET
        pmd.flashMacroList();
        pmd.lfoinitp();
        pmd.oshift();
        fnumsetm();

        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].leng = r.al;
        pmd.calc_q();


        if (pw.partWk[r.di & 0xffff].volpush != 0) { // break mp_newm;
            if (pw.partWk[r.di & 0xffff].onkai != (byte) 0xff) { // break mp_newm;
                pw.volpush_flag--;
                if (pw.volpush_flag != 0) { // break mp_newm;
                    pw.volpush_flag = 0;
                    pw.partWk[r.di & 0xffff].volpush = 0;
                }
            }
        }
//mp_newm:
        volsetm();
        otodasim();
        if ((pw.partWk[r.di & 0xffff].keyoff_flag & 1) != 0) { // break mp3m;
            keyonm();
        }
//mp3m:
        pw.partWk[r.di & 0xffff].keyon_flag++;
        pw.partWk[r.di & 0xffff].address = r.getSi();
        r.al = 0;
        pw.tieflag = r.al;
        pw.volpush_flag = r.al;
        pw.partWk[r.di & 0xffff].keyoff_flag = r.al;
        if (pw.md[r.getSi() & 0xffff].dat != 0xfb) // If there is an '&' immediately after, keyoff will not occur.
            return pmd::mnp_ret;
        pw.partWk[r.di & 0xffff].keyoff_flag = 2;
        return pmd::mnp_ret;
    }

    private Supplier<Object> mpexitm() {
        r.cl = pw.partWk[r.di & 0xffff].lfoswi;
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
                    // popf
                    r.al = pw.partWk[r.di & 0xffff].lfoswi;
                    r.al &= 0x20;
                    pw.lfo_switch |= r.al;
//                    break not_lfo2m;
                } else {
//not_lfo1m:
                    pmd.lfo_change();
                    // popf
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
     * PCM sound source playback: When parts are masked
     */
    private Supplier<Object> pcmmain_nonplay() {
        pw.partWk[r.di & 0xffff].leng--;
        if (pw.partWk[r.di & 0xffff].leng != 0) return pmd::mnp_ret;

        if ((pw.partWk[r.di & 0xffff].partmask & 2) == 0) // Check bit1 (pcm sound effect?)
            return this::pcmmnp_1;

        if (pw.play86_flag == 1)
            return this::pcmmnp_1; // The interrupt PCM is still ringing
        pw.pcmflag = 0; // PCM sound effect end
        pw.pcm_effec_num = (byte) 255;
        pw.partWk[r.di & 0xffff].partmask &= 0xfd; // clear bit1
        if (pw.partWk[r.di & 0xffff].partmask != 0)
            return this::pcmmnp_1;

        r.al = pw.partWk[r.di & 0xffff].voicenum;
        neiro_set();
        r.al = pw.partWk[r.di & 0xffff].fmpan;
        r.ah = pw.revpan;
        set_pcm_pan();

        return this::mp1m0; // If partmask is 0, restore it.
    }

    private Supplier<Object> pcmmnp_1() {
        do {
            do {
                pw.cmd = pw.md[r.getSi() & 0xffff];
                r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
                if (r.al == (byte) 0x80) break; // KUMA: Not checked (changed to == in TAG050)
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

            pmd.flashMacroList();

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

    /**
     * PCM sound source special command processing
     */
    private Supplier<Object> commandsm() {
        pw.currentCommandTable = cmdtblm;
        r.setBx((short) 0); // offset cmdtblp
        return pmd.command00();
    }

    private Supplier<Object>[] cmdtblm;

    private void setupCmdtbl() {
        cmdtblm = new Supplier[] {
                this::comAtm,            // 0xff (0)
                pmd::comq,                // 0xfe (1)
                pmd::comv,                // 0xfd (2)
                pmd::comt,                // 0xfc (3)
                pmd::comtie,              // 0xfb (4)
                pmd::comd,                // 0xfa (5)
                pmd::comstloop,           // 0xf9 (6)
                pmd::comedloop,           // 0xf8 (7)
                pmd::comexloop,           // 0xf7 (8)
                pmd::comlopset,           // 0xf6 (9)
                pmd::comshift,            // 0xf5 (10)
                this::comvolupm,          // 0xf4 (11)
                this::comvoldownm,        // 0xf3 (12)
                pmd::lfoset,              // 0xf2 (13)
                pmd::lfoswitch,           // 0xf1 (14)
                pmd::psgenvset,           // 0xf0 (15)
                pmd::comy,                // 0xef (16)
                pmd::jump1,               // 0xee (17)
                pmd::jump1,               // 0xed (18)
                //
                this::pansetm,            // 0xec (19)
                pmd::rhykey,              // 0xeb (20)
                pmd::rhyvs,               // 0xea (21)
                pmd::rpnset,              // 0xe9 (22)
                pmd::rmsvs,               // 0xe8 (23)
                //
                pmd::comshift2,           // 0xe7 (24)
                pmd::rmsvs_sft,           // 0xe6 (25)
                pmd::rhyvs_sft,           // 0xe5 (26)
                //
                pmd::jump1,               // 0xe4 (27)
                // V2.3 EXTEND
                this::comvolupm2,         // 0xe3 (28)
                this::comvoldownm2,       // 0xe2 (29)
                //
                pmd::jump1,               // 0xe1 (30)
                pmd::jump1,               // 0xe0 (31)
                //
                pmd::syousetu_lng_set,    // 0DFH (32)
                //
                pmd::vol_one_up_pcm,      // 0deH (33)
                pmd::vol_one_down,        // 0DDH (34)
                //
                pmd::status_write,        // 0DCH (35)
                pmd::status_add,          // 0DBH (36)
                //
                pmd::jump1,               // 0DAH (37)
                //
                pmd::jump1,               // 0D9H (38)
                pmd::jump1,               // 0D8H (39)
                pmd::jump1,               // 0D7H (40)
                //
                pmd::mdepth_set,          // 0D6H (41)
                //
                pmd::comdd,               // 0d5h (42)
                //
                pmd::ssg_efct_set,        // 0d4h (43)
                pmd::fm_efct_set,         // 0d3h (44)
                pmd::fade_set,            // 0d2h (45)
                //
                pmd::jump1,               // 0xd1 (46)
                pmd::jump1,               // 0d0h (47)
                //
                pmd::jump1,               // 0cfh (48)
                this::pcmrepeat_set,      // 0ceh (49)
                pmd::extend_psgenvset,    // 0cdh (50)
                pmd::jump1,               // 0cch (51)
                pmd::lfowave_set,         // 0cbh (52)
                pmd::lfo_extend,          // 0cah (53)
                pmd::envelope_extend,     // 0c9h (54)
                pmd::jump3,               // 0c8h (55)
                pmd::jump3,               // 0c7h (56)
                pmd::jump6,               // 0c6h (57)
                pmd::jump1,               // 0c5h (58)
                pmd::comq2,               // 0c4h (59)
                this::pansetm_ex,         // 0c3h (60)
                pmd::lfoset_delay,        // 0c2h (61)
                pmd::jump0,               // 0c1h, sular(62)
                this::pcm_mml_part_mask,  // 0c0h (63)
                pmd::jump4,               // 0bfh (64)
                pmd::jump1,               // 0beh (65)
                pmd::jump2,               // 0bdh (66)
                pmd::jump1,               // 0bch (67)
                pmd::jump1,               // 0bbh (68)
                pmd::jump1,               // 0bah (69)
                pmd::jump1,               // 0b9h (70)
                pmd::jump2,               // 0xb8 (71)
                pmd::mdepth_count,        // 0b7h (72)
                pmd::jump1,               // 0xb6 (73)
                pmd::jump2,               // 0xb5 (74)
                pmd::jump16,              // 0b4h (75)
                pmd::comq3,               // 0b3h (76)
                pmd::comshift_master,     // 0b2h (77)
                //pmd::comq4              // 0b1h (78)
        };

        trans_table = new Runnable[] {
                this::double_trans,
                this::left_trans,
                this::right_trans,
                this::double_trans,
                this::double_trans_g,
                this::left_trans_g,
                this::right_trans_g,
                this::double_trans_g
        };
    }

    /**
     * Mask on/off for playing part
     */
    private Supplier<Object> pcm_mml_part_mask() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        if ((r.al & 0xff) >= 2)
            return pmd::special_0c0h;

        if (r.al != 0) { // break pcm_part_maskoff_ret;

            pw.partWk[r.di & 0xffff].partmask |= 0x40;
            if (pw.partWk[r.di & 0xffff].partmask == 0x40) { // break pmpm_ret;

                stop_86pcm();
            }
//pmpm_ret:
            //    pop ax; commandsm
            return this::pcmmnp_1;
        }
//pcm_part_maskoff_ret:
        pw.partWk[r.di & 0xffff].partmask &= (byte) 0xbf;
        if (pw.partWk[r.di & 0xffff].partmask != 0) {
//            break pmpm_ret;
            return this::pcmmnp_1;
        }
        //    pop ax  ;commandsm
        return this::mp1m; // Part-time revival
    }

    /**
     * Repeat Settings
     */
    private Supplier<Object> pcmrepeat_set() {
        r.setAx(pw._start_ofs);
        pw.repeat_ofs = r.getAx();
        r.setAx(pw._start_ofs2);
        pw.repeat_ofs2 = r.getAx(); // repeat start position = set to start position
        r.setDx(pw._size1);
        pw.repeat_size1 = r.getDx();
        r.setCx(pw._size2); // cx:dx = overall size
        pw.repeat_size2 = r.getCx(); // repeat_size = set to current size
        pw.repeat_flag = 1;

        pw.release_flag1 = 0;

        r.stack.push(r.getDx()); // Save Size
        r.stack.push(r.getCx()); //

        // First one = Repeat start position
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
        r.addSi((short) 2);

        if ((r.getAx() & 0x8000) == 0) { // break prs1_minus;

            // If positive
            pcm86vol_chk();
            int a = (pw.repeat_size2 & 0xffff) * 0x1_0000 + (pw.repeat_size1 & 0xffff);
            a -= r.getAx() & 0xffff; // Repeat size = total size - specified value
            pw.repeat_size1 = (short) a;
            pw.repeat_size2 = (short) (a >>> 16);

            a = (pw.repeat_ofs2 & 0xffff) * 0x1_0000 + (pw.repeat_ofs & 0xffff);
            a += r.getAx(); // Add the specified value from the repeat start position
            pw.repeat_ofs = (short) a;
            pw.repeat_ofs2 = (short) (a >>> 16);

//            break prs2_set;
        } else {
            // If negative
//prs1_minus:
            r.setAx((short) (-r.getAx()));
            pcm86vol_chk();

            pw.repeat_size1 = r.getAx(); // Repeat size = neg (specified value)
            pw.repeat_size2 = 0;

            int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
            a -= r.getAx() & 0xffff;
            r.setDx((short) a);
            r.setCx((short) (a >>> 16));

            a = (pw.repeat_ofs2 & 0xffff) * 0x1_0000 + (pw.repeat_ofs & 0xffff);
            a += r.getDx(); // Repeat start position
            a += (r.getCx() & 0xffff) * 0x1_0000; // Add (total size - specified value)
            pw.repeat_ofs = (short) a;
            pw.repeat_ofs2 = (short) (a >>> 16);
        }
        // 2nd = Repeat end position
//prs2_set:
        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
        r.addSi((short) 2);

        if (r.getAx() != 0) { // break prs3_set;// If 0, do not calculate
            if ((r.getAx() & 0x8000) == 0) { // break prs2_minus;

                // If positive
                pcm86vol_chk();
                pw._size1 = r.getAx(); // If positive, pcm size = specified value
                pw._size2 = 0;

                int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
                a -= (r.getAx() & 0xffff); // Subtract (old size - new size) from repeat size
                r.setDx((short) a);
                r.setCx((short) (a >>> 16));

                a = (pw.repeat_size2 & 0xffff) * 0x1_0000 + (pw.repeat_size1 & 0xffff);
                a -= (r.getAx() & 0xffff); // Repeat size = total size - specified value
                a -= (r.getCx() & 0xffff) * 0x1_0000;
                pw.repeat_size1 = (short) a;
                pw.repeat_size2 = (short) (a >>> 16);

//                break prs3_set;
            } else {
                // If negative
//prs2_minus:
                r.setAx((short) (-r.getAx()));
                pcm86vol_chk();

                int a = (pw.repeat_size2 & 0xffff) * 0x1_0000 + (pw.repeat_size1 & 0xffff);
                a -= (r.getAx() & 0xffff); // From repeat size
                // Subtract neg(specified value)
                pw.repeat_size1 = (short) a;
                pw.repeat_size2 = (short) (a >>> 16);

                a = (pw._size2 & 0xffff) * 0x1_0000 + pw._size1;
                a -= (r.getAx() & 0xffff); // Subtract the specified value from the original size
            }
        }
        // 3rd = Release start position
//prs3_set:
        r.setCx(r.stack.pop());
        r.setDx(r.stack.pop()); // cx:dx = Overall size return

        r.setAx((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
        r.addSi((short) 2);

        if (r.getAx() != (short) 0x8000) { // break prs_exit; // Do not set if 8000H
            r.carry = (r.getAx() & 0xffff) < 0x8000;

            r.setBx(pw._start_ofs);
            pw.release_ofs = r.getBx();
            r.setBx(pw._start_ofs2);
            pw.release_ofs2 = r.getBx();
            // Set release start position = start position
            pw.release_size1 = r.getDx();
            pw.release_size2 = r.getCx(); // release_size = set to current size
            pw.release_flag1 = 1; // Set to release
            if (r.carry) { // break prs3_minus;

                // If positive
                pcm86vol_chk();
                // Release size = total size - specified value
                int a = (pw.release_size2 & 0xffff) * 0x1_0000 + (pw.release_size1 & 0xffff);
                a -= (r.getAx() & 0xffff);
                pw.release_size1 = (short) a;
                pw.release_size2 = (short) (a >>> 16);

                // Add the specified value from the release start position
                a = (pw.release_ofs2 & 0xffff) * 0x1_0000 + (pw.release_ofs & 0xffff);
                a += (r.getAx() & 0xffff);
                pw.release_ofs = (short) a;
                pw.release_ofs2 = (short) (a >>> 16);

//                break prs_exit;
            } else {
                // If negative
//prs3_minus:
                r.setAx((short) (-r.getAx()));
                pcm86vol_chk();
                pw.release_size1 = r.getAx(); // Release size = neg (specified value)
                pw.release_size2 = 0;

                int a = (r.getCx() & 0xffff) * 0x1_0000 + (r.getDx() & 0xffff);
                a -= (r.getAx() & 0xffff);
                r.setDx((short) a);
                r.setCx((short) (a >>> 16));

                a = (pw.release_ofs2 & 0xffff) * 0x1_0000 + (pw.release_ofs & 0xffff);
                a += (r.getDx() & 0xffff); // At the release start position
                a += (r.getCx() & 0xffff) * 0x1_0000; // Add (total size - specified value)
                pw.release_ofs = (short) a;
                pw.release_ofs2 = (short) (a >>> 16);
            }
        }
//prs_exit:
        return null;
    }

    /**
     * When the '/S' option is specified, AX is multiplied by 32.
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

    /**
     * COMMAND ')' [VOLUME UP]
     */
    public Supplier<Object> comvolupm() {
        r.al = pw.partWk[r.di & 0xffff].volume;
        r.carry = (r.al & 0xff) + 16 > 0xff;
        r.al += 16;
        return vupckm();
    }

    private Supplier<Object> vupckm() {
        if (r.carry) r.al = (byte) 255;
        return vsetm();
    }

    private Supplier<Object> vsetm() {
        pw.partWk[r.di & 0xffff].volume = r.al;

        // For IDEs
        ChipDatum cd = new ChipDatum(-1, 0xff, 0xff);
        MmlDatum md = new MmlDatum(-1, MMLType.Volume, pw.cmd.linePos, r.al & 0xff);
        cd.additionalData = md;
        pmd.writeDummy(cd);

        return null;
    }

    // V2.3 EXTEND
    public Supplier<Object> comvolupm2() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.carry = (r.al & 0xff) + (pw.partWk[r.di & 0xffff].volume & 0xff) > 0xff;
        r.al += pw.partWk[r.di & 0xffff].volume;
        return vupckm();
    }

    /**
     * COMMAND '(' [VOLUME DOWN]
     */
    public Supplier<Object> comvoldownm() {
        r.al = pw.partWk[r.di & 0xffff].volume;
        r.carry = (r.al & 0xff) - 16 < 0;
        r.al -= 16;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }

    // V2.3 EXTEND
    public Supplier<Object> comvoldownm2() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = r.al;
        r.al = pw.partWk[r.di & 0xffff].volume;
        r.carry = (r.al & 0xff) - (r.ah & 0xff) < 0;
        r.al -= r.ah;
        if (r.carry) r.al = 0;
        return this::vsetm;
    }

    /**
     * COMMAND 'p' [Panning Set]
     * p0 Reverse phase
     * p1 right
     * p2 left
     * p3 medium
     */
    private Supplier<Object> pansetm() {
        r.ah = 0;
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.al--;
        if (r.al != 0) { // break psm_right;
            r.al--;
            if (r.al != 0) { // break psm_left;
                r.al--;
                if (r.al != 0) { // break psm_mid;
                    r.ah++; // Reverse phase
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
     * COMMAND 'px' [Panning Set Extend]
     * px-127～+127,0or1
     */
    private Supplier<Object> pansetm_ex() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        r.ah = (byte) pw.md[r.incSi() & 0xffff].dat;
        return this::set_pcm_pan;
    }

    private Supplier<Object> set_pcm_pan() {
        pw.partWk[r.di & 0xffff].fmpan = r.al;
        pw.revpan = r.ah;

        return set_pcm_pan2();
    }

    private Supplier<Object> set_pcm_pan2() {
        if ((r.al & 0x80) == 0) { // break psmex_left;
            if (r.al == 0) {
//                break psmex_mid;
                // middle // ↑
//psmex_mid:
                pw.pcm86_pan_flag = 3; // Middle
                r.al = 0;
            } else {
                // Right leaning
                pw.pcm86_pan_flag = 2; // Right
                r.al = (byte) ~r.al;
                r.al &= 127;
//                break psmex_gs_set;
            }
        } else {
            // Left leaning
//psmex_left:
            pw.pcm86_pan_flag = 1; // Left
            r.al += 128;
            r.al &= 127;
//            break psmex_gs_set;
        }
//psmex_gs_set:
        pw.pcm86_pan_dat = r.al;

        if ((r.ah & 1) != 0) { // break psmex_ret;

            pw.pcm86_pan_flag |= 4; // Reverse phase
        }
//psmex_ret:
        return null;
    }

    /**
     * COMMAND '@' [NEIRO Change]
     */
    private Supplier<Object> comAtm() {
        r.al = (byte) pw.md[r.incSi() & 0xffff].dat;
        pw.partWk[r.di & 0xffff].voicenum = r.al;
        return this::neiro_set;
    }

    private Supplier<Object> neiro_set() {
        //r.ah = 0;
        //r.ax += r.ax;
        //r.bx = r.getAx();
        //r.ax += r.ax;
        //r.bx += r.ax; // bx = al * 6
        //r.bx += 0; // offset pcmadrs
        //r.ax = (short) (pw.pcmadrs_86[r.bx & 0xffff] + pw.pcmadrs_86[(r.bx & 0xffff) + 1] * 0x100); // ofs2(w)
        //r.carry = ((r.bx & 0xffff) + 2) > 0xffff;
        //r.bx += 2;
        //pw._start_ofs = r.getAx();
        //r.ax = 0;
        //r.al += (byte) ((pw.pcmadrs_86[r.bx & 0xffff] & 0xff) + (r.carry ? 1 : 0)); // ofs1(b)
        //r.bx++;
        //pw._start_ofs2 = r.getAx();
        //r.ax = (short) ((pw.pcmadrs_86[r.bx & 0xffff] & 0xff) + (pw.pcmadrs_86[(r.bx & 0xffff) + 1] & 0xff) * 0x100);
        //r.bx++;
        //r.bx++;
        //pw._size1 = r.getAx();
        //r.ah = 0;
        //r.al = pw.pcmadrs_86[r.bx & 0xffff];
        //pw._size2 = r.getAx();
        //pw.repeat_flag = 0;
        //pw.release_flag1 = 0;

        ChipDatum cd = new ChipDatum(2, 0, r.al & 0xff);
        p86drv.apply(cd);

        return null;
    }

    /**
     * PCM VOLUME SET
     */
    private void volsetm() {
        r.al = pw.partWk[r.di & 0xffff].volpush;
        if (r.al == 0) { // break vsm_01;
            r.al = pw.partWk[r.di & 0xffff].volume;
        }
//vsm_01:
        r.dl = r.al;
        //
        // Volume down calculation
        //
        r.al = pw.pcm_voldown;
        if (r.al != 0) { // break pcm_fade_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // Fadeout calculation
        //
//pcm_fade_calc:
        r.al = pw.fadeout_volume;
        if (r.al != 0) { // break pcm_env_calc;
            r.al = (byte) -r.al;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.dl = r.ah;
        }
        //
        // ENVELOPE Calculation
        //
//pcm_env_calc:
        r.al = r.dl;
        if (r.al == 0) { // Volume 0?
            mv_out();
            return;
        }
        if (pw.partWk[r.di & 0xffff].envf == (byte) 0xff) { // -1 // break normal_mvset;
            // Extended volume = al * (eenv_vol + 1) / 16
            r.dl = pw.partWk[r.di & 0xffff].eenv_volume;
            if (r.dl == 0) {
//                break mv_min;
//mv_min: // ↑
                r.al = 0;
                mv_out();
                return;
            }
            r.dl++;
            r.setAx((short) ((r.al & 0xff) * (r.dl & 0xff)));
            r.srAx(3);
            r.carry = (((r.getAx() & 0xffff) % 2) != 0);
            r.srAx(1);
            if (r.carry) { // break mvset;
                r.incAx();
            }
//            break mvset;
        } else {
//normal_mvset:
            r.ah = pw.partWk[r.di & 0xffff].eenv_volume; // .penv;
            if ((r.ah & 0x80) != 0) { // break mvplus;
                // -
                r.ah = (byte) -r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.ah += r.ah;
                r.carry = (r.al & 0xff) - (r.ah & 0xff) < 0;
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
        //
        // Volume LFO Calculation
        //
//mvset:
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x22) == 0) {
            mv_out();
            return;
        }
        r.setDx((short) 0);
        r.ah = r.dl;
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x2) != 0) { // break mv_nolfo1;
            r.setDx(pw.partWk[r.di & 0xffff].lfodat);
        }
//mv_nolfo1:
        if ((pw.partWk[r.di & 0xffff].lfoswi & 0x20) != 0) { // break mv_nolfo2;
            r.addDx(pw.partWk[r.di & 0xffff]._lfodat);
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

    /**
     * output
     */
    private void mv_out() {
        // Volume Settings
        if (pw.pcm86_vol != 0) { // break pcm_normal_set;
            // Same volume settings as SPB
            //al = sqr(al)
            r.ah = r.al;
            r.al = 0;
            r.carry = true;

//sqr_loop:
            while (true) {
                boolean c = ((r.ah & 0xff) - ((r.al & 0xff) + (r.carry ? 1 : 0))) < 0;
                r.ah -= (byte) ((r.al & 0xff) + (r.carry ? 1 : 0));
                if (c) break; // pcm_vol_set;

                c = ((r.ah & 0xff) - ((r.al & 0xff) + (c ? 1 : 0))) < 0;
                r.ah -= (byte) ((r.al & 0xff) + (c ? 1 : 0));
                if (c) break; // pcm_vol_set;

                r.al++;
                if (r.al == 15)
                    break; // pcm_vol_set;
//                break sqr_loop
            }
        } else {
//pcm_normal_set:
            r.al >>>= 4;
        }
//pcm_vol_set:
        ChipDatum cd = new ChipDatum(4, 0, r.al & 0xff);
        p86drv.apply(cd);
        //r.al &= 0b0000_1111;
        //r.al ^= 0b0000_1111;
        //r.al |= 0xa0; // PCM Volume
        //r.dx = 0xa466;
        //pc98.outPort(r.dx, r.al);
    }

    /**
     * PCM KEYON
     */
    private void keyonm() {
        if (pw.partWk[r.di & 0xffff].onkai == (byte) 0xff) { //-1 // break keyonm_00;
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
     * PCM KEYOFF
     */
    private void keyoffm() {
        ChipDatum cd = new ChipDatum(9, 0, 0);
        p86drv.apply(cd);
        //    if (pw.release_flag1 != 1) // Is the release set?
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
        //    pw.release_flag2 = 1; // Released

//kofm_not_release:
        //    if (pw.partWk[r.di & 0xffff].envf == 0xff) // -1
        //        break kofm1_ext;
        //    if (pw.partWk[r.di & 0xffff].envf != 2) {
        //        pmd.keyoffp();
        //        return;
        //    }

//kofm_ret:
        //    return;

//kofm1_ext:
        //    if (pw.partWk[r.di & 0xffff].eenv_count == 4)
        //        break kofm_ret;

        //    pmd.keyoffp();
    }

    /**
     * PCM Frequency Settings
     */
    private void otodasim() {
        r.setBx(pw.partWk[r.di & 0xffff].fnum);
        if (r.getBx() == 0) { // break tone_set;
            return;
        }
//tone_set:
        r.setAx(pw.partWk[r.di & 0xffff].fnum2);
        if (pw.pcm86_vol == 1) { // When matching with ADPCM
            tone_set2(); // Cut Detune
            return;
        }
        if (pw.partWk[r.di & 0xffff].detune == 0) {
            tone_set2();
            return;
        }

        r.setCx(r.getAx());
        r.setDx(r.getBx());

        int c = (r.getDx() & 0xffff) * 0x10000 + (r.getCx() & 0xffff);
        c >>>= 5;
        r.setDx((short) (c >>> 16));
        r.setCx((short) c);

        //for (int i = 0; i < 5; i++) { // rept 5
        //    r.carry = (r.dx & 1) != 0;
        //    r.dx >>>= 1;
        //    r.cx >>>= 1;
        //    if (r.carry) r.cx |= 0x8000;
        //} // endm   ;cx=zzzzzxxx xxxxxxxx

        r.setDx(pw.partWk[r.di & 0xffff].detune);
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
                r.setCx((short) 1); // If you set it to 0, there is a risk that the added value will be 0, so set it to 1.
            }
        }
//tone_set1:
        r.setDx((short) 0);

        c = (r.getDx() & 0xffff) * 0x1_0000 + (r.getCx() & 0xffff);
        c <<= 5;
        r.setDx((short) (c >>> 16));
        r.setCx((short) c);
        //for (int i = 0; i < 5; i++) { // rept 5
        //    r.carry = (r.cx & 0x8000) != 0;
        //    r.cx <<= 1;
        //    r.dx <<= 1;
        //    if (r.carry) r.dx |= 0x0001;
        //} //    endm   ;dx:cx=00000000 000zzzzz xxxxxxxx xxx00000

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
        //logger.log(Level.TRACE, "%x  %x".formatted(pw.addsize1, pw.addsize2));
        ChipDatum cd = new ChipDatum(5, pw.addsize1 & 0xff, pw.addsize2 & 0xff);
        p86drv.apply(cd);

        //r.carry = false;
        //r.bl = r.rol(r.bl, 1);
        //r.bl = r.rol(r.bl, 1);
        //r.bl = r.rol(r.bl, 1);
        //r.bl &= 7;
        //r.bl ^= 7;
        //// Frequency Settings
        //r.dx = 0xa468;
        ////pushf
        ////cli
        //r.al = pc98.inPort(r.dx);
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xf8;
        //r.al |= r.bl;
        //pc98.OutPort(r.dx, r.al);
        ////popf
    }

    /**
     * PCM FNUM SET
     */
    private void fnumsetm() {
        r.ah = r.al;
        r.ah &= 0xf;
        if (r.ah == 0xf) {
            pmd.fnrest(); // Rests
            return;
        }

        if (pw.pcm86_vol == 1) { // break fsm_noad;

            if ((r.al & 0xff) >= 0x65) { // o7e? // break fsm_noad;
                r.al = 0x50; // o6
                if ((r.ah & 0xff) < 5) { // ah=onkai // break fsm_00;
                    r.al = 0x60; // o7
                }
//fsm_00:
                r.al |= r.ah;
            }
        }
//fsm_noad:
        pw.partWk[r.di & 0xffff].onkai = r.al;

        r.al &= (byte) 0xf0;
        r.al >>>= 1;
        r.bl = r.al; // bl = octave * 8
        r.al >>>= 1; // al = octave * 4
        r.bl += r.al;

        r.bl += r.ah; // bl = octave * 12 + Scale
        r.bh = 0;
        r.setAx(r.getBx());
        //r.bx += r.bx;
        //r.bx += r.ax;
        //r.bx += 0; // offset pcm_tune_data
        //logger.log(Level.TRACE, "bx:%d".formatted(r.bx));
        r.al = (byte) (int) pw.pcm_tune_data86[r.getBx() & 0xffff].getItem1();
        r.orAx(0xff00);
        pw.partWk[r.di & 0xffff].fnum = r.getAx(); // ax = 0ff00h + addsize1
        //r.bx++;
        r.setAx((short) (int) pw.pcm_tune_data86[r.getBx() & 0xffff].getItem2()); // ax = addsize2
        pw.partWk[r.di & 0xffff].fnum2 = r.getAx();
        //logger.log(Level.TRACE, "fnum:%x fnum2:%x".formatted(pw.partWk[r.di & 0xffff].fnum, pw.partWk[r.di & 0xffff].fnum2));
    }

    /**
     * FIFO int Subroutine
     *  *Make sure that FIFO is there before flying in.
     *   The only pushed registers are ax/dx/ds.
     */
    private void fifo_main() {
        //
        // Enable interrupts
        //
        if (pw.disint != 1) { // break fifo_not_sti;

            //sti   ; Immediate interrupt permission
        }
//fifo_not_sti:
        //
        // PCM processing main
        //
        if (pw.play86_flag != 0) { // PCM playback? // break not_trans;

            if (pw.trans_flag == 0) { // Transfer next? // break i5_trans;

                stop_86pcm(); // PLAY and no more data next = stop
                return; // FIFO not allowed to terminate
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
        //
        // Disable interrupts
        //
//not_trans:
        // cli
        //
        // FIFO interrupt flag reset
        //
        r.setDx((short) 0xa468);
        r.al = pc98.inPort(r.getDx() & 0xffff);
        pc98.outPort((short) 0x5f, r.al);
        r.al &= (byte) 0xef;
        pc98.outPort(r.getDx(), r.al); // Clear FIFO interrupt flag
        pc98.outPort((short) 0x5f, r.al);
        r.al |= 0x10;
        pc98.outPort(r.getDx(), r.al); // Clear FIFO interrupt flag
    }

    /**
     * PCM data transfer
     * use ax/bx/cx/dx/si/di/bp
     */
    private void pcm_trans2() {
        r.setCx((short) pw.trans_size); // Bytes to transfer
        pcm_trans_main();
    }

    private void pcm_trans() {
        r.setCx((short) (pw.trans_size / 2)); // Bytes to transfer
        pcm_trans_main();
    }

    private void pcm_trans_main() {
        //KUMA: P86drv resident check
        //      Not implemented as it is probably not needed.
        //break zero_trans; // If not resident

        //KUMA: P86drv version check
        //      Not implemented as it is probably not needed.
        //r.ah=0xff: // -1
        //int 65h
        //if (r.al < 0x10)
        //    break zero_trans; // For version 1.0 or earlier

        r.ah = 0;
        r.al = pw.pcm86_pan_flag;
        r.addAx(r.getAx());
        r.addAx((short) 0); // offset trans_table
        r.bp = r.getAx(); // bp = transfer process sub offset

        r.setDx((short) 0xa46c);
        r.setAx(pw.size1);
        r.di = r.getAx(); // di = remaining size (lower 16 bits)
        r.orAx(pw.size2);
        if (r.getAx() == 0) {
            zero_trans();
            return;
        }

        //r.stack.push(r.ds);

        r.ah = (byte) 0xfb; // -5
        ChipDatum cd = new ChipDatum(r.ah & 0xff, 0xff, 0xff);
        p86drv.apply(cd); // p86drv pushems

        r.ah = pw.addsize1;
        r.setBx(pw.addsize2);

        get_data_offset(); // ds:si = data offset
        trans_table[(r.bp & 0xffff) / 2].run();

        r.ah = (byte) 0xfc; // -4
        cd = new ChipDatum(r.ah & 0xff, 0xff, 0xff);
        p86drv.apply(cd); // p86drv popems

        //    pop ds
    }

    /**
     * middle
     */
    private void double_trans() {
        r.bp = 0;
//double_trans_loop:
        do {
            // mov al,[si]
            pc98.outPort(r.getDx(), r.al); // left
            pc98.outPort(r.getDx(), r.al); // right
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
        pw.start_ofs += r.bp; // bp = transferred size
        pw.start_ofs2 += (short) (r.carry ? 1 : 0);
        pw.size1 = r.di;
    }

    /**
     * Middle (reverse phase)
     */
    private void double_trans_g() {
        r.bp = 0;
//double_trans_g_loop:
        do {
            // mov al,[si]
            pc98.outPort(r.getDx(), r.al); // left
            r.al = (byte) -r.al;
            pc98.outPort(r.getDx(), r.al); // right
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    /**
     * Left leaning
     */
    private void left_trans() {
        r.bp = 0;
//left_trans_loop:
        do {
            // mov al,[si]
            pc98.outPort(r.getDx(), r.al); // left
            r.stack.push(r.getAx());
            r.setAx((short) ((r.getAx() & 0xffff) * (pw.pcm86_pan_dat & 0xff)));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // right
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

    /**
     * Left leaning (reverse phase)
     */
    private void left_trans_g() {
        r.bp = 0;
//left_trans_g_loop:
        do {
            // mov al,[si]
            pc98.outPort(r.getDx(), r.al); // left
            r.al = (byte) -r.al;
            r.stack.push(r.getAx());
            r.setAx((short) ((r.getAx() & 0xffff) * (pw.pcm86_pan_dat & 0xff)));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // right
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

    /**
     * Right leaning
     */
    private void right_trans() {
        r.bp = 0;
//right_trans_loop:
        do {
            // mov al,[si]
            r.stack.push(r.getAx());
            r.setAx((short) ((r.getAx() & 0xffff) * (pw.pcm86_pan_dat & 0xff)));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // left
            r.setAx(r.stack.pop());
            pc98.outPort(r.getDx(), r.al); // right
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    /**
     * Rightward (reverse phase)
     */
    private void right_trans_g() {
        r.bp = 0;
//right_trans_g_loop:
        do {
            // mov al,[si]
            r.stack.push(r.getAx());
            r.setAx((short) ((r.getAx() & 0xffff) * (pw.pcm86_pan_dat & 0xff)));
            r.addAx(r.getAx());
            r.al = r.ah;
            pc98.outPort(r.getDx(), r.al); // left
            r.setAx(r.stack.pop());
            r.al = (byte) -r.al; // Reverse phase
            pc98.outPort(r.getDx(), r.al); // right
            add_address();
            if (r.carry) {
                trans_fin();
                return;
            }
            r.decCx();
        } while (r.getCx() != 0);
        trans_exit();
    }

    /**
     * Advance Address
     *  cy=1 ... Transfer end
     */
    private void add_address() {
        pw.addsizew += r.getBx(); // bx=addsize2
        //pushf
        r.al = r.ah;
        r.ah = 0; // ax=addsize1
        r.bp += (short) ((r.getAx() & 0xffff) + (r.carry ? 1 : 0)); // Add bp according to addsize
        //popf
        //pushf
        r.addSi((short) ((r.getAx() & 0xffff) + (r.carry ? 1 : 0))); // Add address according to addsize
        if ((r.getSi() & 0xffff) >= 0x4000) { // 16K Over Check(for EMS) break not_add_ofs2;

            //[[[segment over]]]
            r.carry = (pw.start_ofs & 0xffff) + (r.bp & 0xffff) > 0xffff;
            pw.start_ofs += r.bp;
            pw.start_ofs2 += (short) (0 + (r.carry ? 1 : 0));

            r.bp = 0; // Reset transfer size
            get_data_offset();
        }
//not_add_ofs2:
        //popf
        boolean c = (r.di - (short) ((r.getAx() & 0xffff) + (r.carry ? 1 : 0))) < 0;
        r.di -= (short) ((r.getAx() & 0xffff) + (r.carry ? 1 : 0)); // Subtract size according to addsize
        r.ah = r.al; // Revert to ah=addsize1
        if (!c) { // break addadd_sizeseg;
            if (r.di != 0) { // break addadd_justcheck;
                return;
            }
//addadd_justcheck:
            if (pw.size2 != 0) { // Just 0 // break addadd_repchk;
                return;
            }
        } else {
//addadd_sizeseg:
            r.carry = ((pw.size2 & 0xffff) - 1) < 0;
            pw.size2 -= 1;
            if (!r.carry) { // break addadd_repchk;
//                return;
            }
        }
//addadd_repchk:
        if (pw.repeat_flag != 0) { // break addadd_stc_ret;

            if (pw.release_flag2 != 1) { // break addadd_stc_ret;

                // Repeat settings
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
                ChipDatum cd = new ChipDatum(r.ah, 0xff, 0xff);
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

    /**
     * Get a new pcmdata offset
     */
    private void get_data_offset() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());

        r.setDx(pw.start_ofs); // cs:[start_ofs]
        r.setAx(pw.start_ofs2); // cs:[start_ofs2]

        r.ah = (byte) 0xfd;
        ChipDatum cd = new ChipDatum(r.ah & 0xff, 0xff, 0xff);
        p86drv.apply(cd); // get data offset = ds:dx

        r.setSi(r.getDx()); // DS:SI= DATA ADDRESS

        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    /**
     * Transfer complete...fill the remaining bits with 0
     */
    private void trans_fin() {
        r.decCx();
        if (r.getCx() != 0) { // break tfin_ret;

            r.al = 0;
//tfin_loop:
            do {
                pc98.outPort(r.getDx(), r.al); // left
                pc98.outPort(r.getDx(), r.al); // right
                r.decCx();
            } while (r.getCx() != 0);
        }
//tfin_ret:
        pw.size1 = r.getCx(); // cs:[size1] ;cx=0
        pw.size2 = r.getCx(); // cs:[size2]
    }

    /**
     * Fill with 0
     */
    private void zero_trans() {
        r.al = 0;
//ztr_loop:
        do {
            pc98.outPort(r.getDx(), r.al); // left
            pc98.outPort(r.getDx(), r.al); // right
            r.decCx();
        } while (r.getCx() != 0);
        pw.trans_flag = 0; // You don't need to transfer it anymore
    }

    /**
     * 86B play PCM
     */
    private void play_86pcm() {
        ChipDatum cd = new ChipDatum(3, pw.pcm86_pan_flag & 0xff, pw.pcm86_pan_dat & 0xff);
        p86drv.apply(cd);

        cd = new ChipDatum(7, 0, 0);
        p86drv.apply(cd);

        ////pushf
        ////cli

        //r.dx = 0xa468;
        //r.al = pc98.inPort(r.dx);
        //// A468 reset bit7 （FIFO Stop）
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0x7f;
        //pc98.outPort(r.dx, r.al);

        //// A468 reset bit6 （CPU->FIFO mode）
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xbf;
        //pc98.outPort(r.dx, r.al);

        //// A468 set bit3  （FIFO reset setting）
        //pc98.outPort(0x5f, r.al);
        //r.al |= 8;
        //pc98.outPort(r.dx, r.al);

        //// A468 reset bit3 （FIFO reset release）
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xf7;
        //pc98.outPort(r.dx, r.al);

        //// A468 reset bit5 （Disable FIFO interrupts/Prepare A46A settings）
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xdf;
        //pc98.outPort(r.dx, r.al);

        //// A468 reset bit4 （Clear interrupt flag）
        //pc98.putPort(0x5f, r.al);
        //r.al &= 0xef;
        //pc98.outPort(r.dx, r.al);

        //// PAN OUT to A46A （8bit L/Rch）
        //r.dx = 0xa46a;
        //r.al = 0xf2;
        //pc98.outPort(r.dx, r.al);

        ////popf

        //// Transfer the first data
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
        ////
        //// Interrupt Settings
        ////
        //r.dx = 0xa468;
        //r.al = pc98.inPort(r.dx);

        //// A468 set bit4  （Clear interrupt flag）
        //pc98.outPort(0x5f, r.al);
        //r.al |= 0x10;
        //pc98.outPort(r.dx, r.al);

        //// A468 set bit5  （Enable FIFO interrupt/prepare A46A settings）
        //pc98.outPort(0x5f, r.al);
        //r.al |= 0x20;
        //pc98.outPort(r.dx, r.al);

        //// Set the A46A FIFO interrupt size
        //r.dx = 0xa46a;
        //r.al = (byte)(+(pw.trans_size / 128) - 1);
        //pc98.putPort(r.dx, r.al);

        ////
        //// Start playing
        ////
        //r.dx = 0xa468;
        //r.al = pc98.inPort(r.dx);
        //// A468 set bit7  （PCM Start playing）
        //pc98.outPort(0x5f, r.al);
        //r.al |= 0x80;
        //pc98.outPort(r.dx, r.al);

        //pw.play86_flag = 1;
        //pw.trans_flag = 1;

        ////popf
    }

    /**
     * 86B PCM stop
     */
    public void stop_86pcm() {
        ChipDatum cd = new ChipDatum(8, 0, 0);
        p86drv.apply(cd);

        //r.stack.push(r.ax);
        //r.stack.push(r.dx);

        ////pushf
        ////cli

        //r.dx = 0xa468;
        //r.al = pc98.inPort(r.dx);

        //pc98.outPort(0x5f, r.al);
        //r.al &= 0x7f;
        //pc98.outPort(r.dx, r.al);

        //// FIFO reset
        //pc98.outPort(0x5f, r.al);
        //r.al |= 0x08;
        //pc98.outPort(r.dx, r.al); // Reset process

        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xf7;
        //pc98.outPort(r.dx, r.al); // Reset process completed

        //// Disable FIFO interrupts
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xdf;
        //pc98.outPort(r.dx, r.al);

        //// FIFO interrupt flag reset
        //pc98.outPort(0x5f, r.al);
        //r.al &= 0xef;
        //pc98.outPort(r.dx, r.al);

        //pc98.outPort(0x5f, r.al);
        //r.al |= 0x10;
        //pc98.outPort(r.dx, r.al);

        //pw.play86_flag = 0; // cs:[play86_flag]
        //pw.trans_flag = 0; // cs:[trans_flag]

        ////popf

        //r.dx = r.stack.pop();
        //r.ax = r.stack.pop();
    }

    /**
     * PCM sound effect routine
     *  input dx  fnum
     *   ch Pan
     * cl Volume
     * al Number
     */
    private void pcm_effect() {
        r.setBx((short) 0); //offset part10
        pw.partWk[pw.part10].partmask |= 2; // PCM Part Mask
        pw.pcmflag = 1;
        pw.pcm_effec_num = r.al;
        pw._voice_delta_n = r.getDx() & 0xffff;
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
