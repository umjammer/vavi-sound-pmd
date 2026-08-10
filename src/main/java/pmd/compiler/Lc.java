package pmd.compiler;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

import musicDriverInterface.MmlDatum;

import static java.lang.System.getLogger;


public class Lc {

    private static final Logger logger = getLogger(Lc.class.getName());

    private final Work work;
    private final MSeg m_seg;
    private final Mc mc;

    public Lc(Mc mc, Work work, MSeg m_seg) {
        this.mc = mc;
        this.work = work;
        this.m_seg = m_seg;
        setJumpTable();
    }

    /**
     * For calculating sound length include file
     * 	in.al print_flag(0 for hidden)
     */
    public void lc_proc(byte al) {
        //_print_mes macro   ofs
        //local   Exit
        //   cmp[print_flag],0
        //    jz Exit
        //    print_mes ofs
        //Exit:
        //	endm

        PartEnds ret;
        ret = PartEnds.CalcStart;

//List<Byte> dst = new ArrayList<>();
//dst.add((byte) 0);
//for (MmlDatum o : m_seg.m_buf) {
// dst.add(o == null ? (byte) 0xff : (byte) o.dat);
//}
//logger.log(Level.INFO, "\n" + StringUtil.getDump(ByteUtil.toByteArray(dst)));

        do {
            ret = switch (ret) {
                case CalcStart -> calc_start(al);
                case PartLoop -> part_loop();
                case PartLoop2 -> part_loop2();
                case CheckJ -> check_j();
                case ComLoop -> com_loop();
                case PartEnds -> part_ends();
                case PartKStart -> partk_start();
                case KComLoop -> kcom_loop();
                case KPartEnd -> kpart_end();
                case KL00 -> kl_00();
                default -> ret;
            };
        } while (ret != PartEnds.Exit);
    }

    /**
     * Start calculation
     */
    private PartEnds calc_start(byte al) {
        print_flag = al != 0;
        part_chr = 'A';
        work.bp = 0; // offset m_buf

        return PartEnds.PartLoop;
    }

    /**
     * Looping by Part
     */
    private PartEnds part_loop() {
        work.si = m_seg.m_buf.get(work.bp).dat + m_seg.m_buf.get(work.bp + 1).dat * 0x100;
        work.si += 0; // offset m_buf
        work.bp += 2;

        return PartEnds.PartLoop2;
    }

    private PartEnds part_loop2() {
        all_length = 0;
        loop_length = -1;
        loop_flag = false;

        //
        // (For Part A) Check if there is an extended FM3 channel
        //
        if (part_chr != 'A') return PartEnds.CheckJ;
        if (m_seg.m_buf.get(work.si).dat != 0xc6) return PartEnds.CheckJ;

        work.si++;

        for (int i = 0; i < 3; i++) {
            int l = m_seg.m_buf.get(work.si++).dat;
            int h = m_seg.m_buf.get(work.si++).dat;
            fm3_adr[i] = h * 0x100 + l;
        }

        return PartEnds.CheckJ;
    }

    /**
     * (For Part J) Check if there is an extended PCM part
     */
    private PartEnds check_j() {
        if (part_chr != 'J') return PartEnds.ComLoop; //jnz ComLoop
        if (m_seg.m_buf.get(work.si).dat != 0xb4) return PartEnds.ComLoop;

        work.si++;

        for (int i = 0; i < 8; i++) {
            int l = m_seg.m_buf.get(work.si++).dat;
            int h = m_seg.m_buf.get(work.si++).dat;
            pcm_adr[i] = h * 0x100 + l;
        }

        return PartEnds.ComLoop;
    }

    /**
     * Loop through each command
     */
    private PartEnds com_loop() {

        logger.log(Level.INFO, "partType:%s partCh:%c partNum:%d".formatted(part_type, part_chr, part_num));

        do {
            MmlDatum al;
            do {
                logger.log(Level.TRACE, "si:%d, %d".formatted(work.si, m_seg.m_buf.size()));

                al = (work.si < m_seg.m_buf.size() ? m_seg.m_buf.get(work.si++) : new MmlDatum(0x80));
logger.log(Level.TRACE, "si:%d, %02x".formatted(work.si, al.dat));
                if (al.dat == 0x80) return PartEnds.PartEnds;
                if (al.dat >= 0x80) break;

                al = m_seg.m_buf.get(work.si++);
                //byte ah = 0;

                all_length += al.dat;
            } while (true);

//cl_00:
            command_exec((byte) al.dat);
            if (loop_flag) return PartEnds.PartEnds;

        } while (true);

        //return enmPart_ends.PartEnds;
    }

    /**
     * Part-time job finished
     */
    private PartEnds part_ends() {
        print_length();

        part_chr++;
        part_num++;
        if (part_chr == 'G') part_type = "SSG";
        if (part_chr == 'H') part_type = "SSG";
        if (part_chr == 'I') part_type = "SSG";
        if (part_chr < 'K') return PartEnds.PartLoop;

        int di = work.di;
        work.di = 0; // offset fm3_adr1
        int bx = 0; // offset _fm3_partchr1;in cs
        int cx = 3 + 8;
//extend_check_loop:
        do {
            work.si = work.di < 3 ? fm3_adr[work.di] : pcm_adr[work.di - 3];
            if (work.si != 0) { // break extend_check_next;
                work.si += 0; // offset m_buf
                char al_c = bx < 3 ? _fm3_partchr[bx] : _pcm_partchr[bx - 3];
                part_chr = al_c;
                part_num = bx < 3 ? bx : (bx - 3);
                part_type = bx < 3 ? "FMOPNex" : "PPZ8";
                if (work.di < 3) fm3_adr[work.di] = 0;
                else pcm_adr[work.di - 3] = 0;

                work.di = di;
                return PartEnds.PartLoop2;
            }
//extend_check_next:
            work.di++;
            bx++;
            cx--;
        } while (cx > 0);

        work.di = di;
        return PartEnds.PartKStart;
    }

    /**
     * Part K
     */
    private PartEnds partk_start() {
        part_chr = 'K';
        all_length = 0;
        loop_length = -1;
        loop_flag = false;

        //logger.log(Level.TRACE, "bp:%d", Work.bp);
        work.si = m_seg.m_buf.get(work.bp).dat + (m_seg.m_buf.get(work.bp + 1).dat * 0x100);
        work.si += 0; // offset m_buf
        work.bp += 2;
        work.bx = m_seg.m_buf.get(work.bp).dat + (m_seg.m_buf.get(work.bp + 1).dat * 0x100);
        work.bx += 0; // offset m_buf	; bx= R table First Address

        return PartEnds.KComLoop;
    }

    /**
     * Kpart/Loop per command
     */
    private PartEnds kcom_loop() {
        do {
            MmlDatum ald;
            byte al;
            ald = (work.si < m_seg.m_buf.size()) ? m_seg.m_buf.get(work.si++) : (new MmlDatum(0x80));
            al = (byte) ald.dat;
            if (al == (byte) 0x80) return PartEnds.KPartEnd;
            if ((al & 0xff) >= 0x80) {
                work.al = al;
                return PartEnds.KL00;
            }
            al = (byte) (((al & 0xff) * 2) & 0xff);

            //    push    si
            //    push    bx
            int si = work.si;
            int bx = work.bx;
            work.bx += al & 0xff;
            work.si = m_seg.m_buf.get(work.bx).dat + (m_seg.m_buf.get(work.bx + 1).dat * 0x100);
            //logger.log(Level.TRACE, "bx:%d si:%d".formatted(work.bx, work.si));
            work.si += 0; // offset m_buf
            rcom_loop();
            work.bx = bx;
            work.si = si;

            if (loop_flag) return PartEnds.KPartEnd;
        } while (true);
    }

    /**
     * Kpart/Various special commands
     */
    private PartEnds kl_00() {
        int bx = work.bx;
        command_exec(work.al);
        work.bx = bx;

        if (loop_flag) return PartEnds.KPartEnd;
        return PartEnds.KComLoop;
    }

    /**
     * Kpart/Calculation complete
     */
    private PartEnds kpart_end() {
        print_length();
        return PartEnds.Exit;
    }

    private enum PartEnds {
        CalcStart,
        PartLoop,
        PartLoop2,
        CheckJ,
        ComLoop,
        PartEnds,
        PartKStart,
        KComLoop,
        KPartEnd,
        KL00,
        Exit
    }

    /**
     * Loop for each Rpart/command
     */
    private void rcom_loop() {
rpart_end:
        do {
            MmlDatum al;
            do {
                al = (work.si < m_seg.m_buf.size() ? m_seg.m_buf.get(work.si++) : new MmlDatum(0xff));
                if (al.dat == 0xff) break rpart_end;
                if (al.dat >= 0xc0) break;

                if ((al.dat & 0x80) != 0) {
                    work.si++;
                }
//rl_01:
                al = m_seg.m_buf.get(work.si++);
                all_length += al.dat;

            } while (true);
            //
            // Rpart / Various special command processing
            //
//rl_00:
            command_exec((byte) al.dat);
            if (loop_flag) break /* rpart_end */;
        } while (true);
        //
        // Rpart / Calculation complete
        //
    }

    /**
     * Various commands
     */
    private void command_exec(byte al) {
//logger.log(Level.INFO, "%02x, %02x".formatted(al & 0xff, ~al & 0xff));
        al = (byte) ~al;
        int ax = al & 0xff;
        ax += 0; // offset jumpTable
        jumpTable[ax].run();
    }

    private void jump16() {
        work.si += 16;
    }

    private void jump6() {
        work.si += 6;
    }

    private void jump5() {
        work.si += 5;
    }

    private void jump4() {
        work.si += 4;
    }

    private void jump3() {
        work.si += 3;
    }

    private void jump2() {
        work.si += 2;
    }

    private void jump1() {
        work.si += 1;
    }

    private void jump0() {
        work.si += 0;
    }

    /**
     * tempo
     */
    private void _tempo() {
        int al = m_seg.m_buf.get(work.si++).dat;
        if (al >= 251) {
            work.si++; // relative
        }
//tempo_ret:
    }

    /**
     * Portamento
     */
    private void porta() {
        work.si += 2;

        int al = m_seg.m_buf.get(work.si++).dat;
        all_length += al;
    }

    /**
     * L command
     */
    private void loop_set() {
        loop_length = all_length;
    }

    /**
     * '[' command
     */
    private void loop_start() {
        int ax = m_seg.m_buf.get(work.si++).dat;
        ax += m_seg.m_buf.get(work.si++).dat * 0x100;

        work.bx = ax;
        work.bx += 1; // offset m_buf+1
        m_seg.m_buf.set(work.bx, new musicDriverInterface.MmlDatum(0));
    }

    /**
     * ']' command
     */
    private void loop_end() {
        int al = m_seg.m_buf.get(work.si++).dat;
        if (al != 0) { // break loop_fset; // There was an unconditional loop
            int ah = al;
            m_seg.m_buf.set(work.si, new musicDriverInterface.MmlDatum((m_seg.m_buf.get(work.si).dat + 1) & 0xff));
            al = m_seg.m_buf.get(work.si++).dat;
            if (ah == al) { // break reloop;
                work.si++;
                work.si++;
                return;
            }
//reloop:

            int ax = m_seg.m_buf.get(work.si++).dat;
            ax += m_seg.m_buf.get(work.si++).dat * 0x100;
            ax += 2; // offset m_buf+2
            work.si = ax;
            return;
        }
//loop_fset:
        loop_flag = true;
    }

    /**
     * ':' command
     */
    private void loop_exit() {
        int ax = m_seg.m_buf.get(work.si++).dat;
        ax += m_seg.m_buf.get(work.si++).dat * 0x100;
        work.bx = ax;
        work.bx += 0; // offset m_buf
        int dl = m_seg.m_buf.get(work.bx).dat;
        dl--;
        work.bx++;
        if ((dl & 0xff) != m_seg.m_buf.get(work.bx).dat) { // break loopexit;
            return;
        }
//loopexit:
        work.bx += 3;
        work.si = work.bx;
    }

    /**
     * 0c0h + ?? special control
     */
    private void special_0c0h() {
        byte al = (byte) m_seg.m_buf.get(work.si++).dat;
        if ((al & 0xff) >= 2) { // break spc0_ret;
logger.log(Level.INFO, "%02x, %02x".formatted(al & 0xff, ~al & 0xff));
            al = (byte) ~al;
            al += 0; // offset jumpTable_0c0h
            jumpTable_0c0h[al & 0xff].run();
        }
//spc0_ret:
    }

    /**
     * Show Length
     */
    private void print_length() {
        int tc = 0;
        int lc = 0;

        if (all_length == 0) return; // No data
        String msg = part_mes + part_chr + part_chr_n;

        int ax = all_length & 0xffff;
        int dx = (all_length >> 16) & 0xffff;
        int bx = max_all & 0xffff;
        int cx = (max_all >> 16) & 0xffff;
        if (max_all - all_length < 0) {
            max_all = all_length; // ax | dx * 0x10000;
        }
//not_over_all:
        msg += "%d".formatted(ax);
        tc = ax;

        if (loop_flag) { // break pe_loop;
            if (print_flag) {
                mc.print_mes(loop_mes2);
            }

            if (work.compilerInfo.partName == null) work.compilerInfo.partName = new ArrayList<>();
            work.compilerInfo.partName.add(String.valueOf(part_chr));
            if (work.compilerInfo.totalCount == null) work.compilerInfo.totalCount = new ArrayList<>();
            work.compilerInfo.totalCount.add(tc);
            if (work.compilerInfo.loopCount == null) work.compilerInfo.loopCount = new ArrayList<>();
            work.compilerInfo.loopCount.add(lc);

            return;
        }
//pe_loop:

        dx = (loop_length >> 16) & 0xffff;
        ax = loop_length & 0xffff;
        ax++;
        if (loop_length + 1 != 0) { // break pe_00;
            msg += loop_mes;

            //Mc.print_mes(loop_mes);
            int n = all_length - loop_length;
            if (max_loop < n) {
                max_loop = n;
            }
//not_over_loop:
            msg += "%d".formatted(n);
            lc = n;
        }
//pe_00:
        if (print_flag) {
            mc.print_mes(msg);

            if (work.compilerInfo.partType == null) work.compilerInfo.partType = new ArrayList<>();
            work.compilerInfo.partType.add(part_type);

            if (work.compilerInfo.partNumber == null) work.compilerInfo.partNumber = new ArrayList<>();
            work.compilerInfo.partNumber.add(part_num);

            if (work.compilerInfo.partName == null) work.compilerInfo.partName = new ArrayList<>();
            work.compilerInfo.partName.add(String.valueOf(part_chr));

            if (work.compilerInfo.totalCount == null) work.compilerInfo.totalCount = new ArrayList<>();
            work.compilerInfo.totalCount.add(tc);

            if (work.compilerInfo.loopCount == null) work.compilerInfo.loopCount = new ArrayList<>();
            work.compilerInfo.loopCount.add(lc);
        }
        //Mc.print_mes(_crlf_mes);
//pe_01:
    }

    private Runnable[] jumpTable;
    private Runnable[] jumpTable_0c0h;

    private void setJumpTable() {
        jumpTable = new Runnable[] {
                this::jump1,  // 0ffh
                this::jump1,
                this::jump1,
                this::_tempo,
                this::jump0,
                this::jump2,
                this::loop_start,
                this::loop_end,  // 0f8h
                this::loop_exit,
                this::loop_set,
                this::jump1,
                this::jump0,
                this::jump0,
                this::jump4,
                this::jump1,
                this::jump4,  // 0f0h
                this::jump2,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,  // 0e8h
                this::jump1,
                this::jump1,
                this::jump2,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,  // 0e0h
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::porta,
                this::jump1,
                this::jump1,  // 0d8h
                this::jump1,
                this::jump2,
                this::jump2,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,  // 0d0h
                this::jump1,
                this::jump6,
                this::jump5,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump3,  // 0c8h
                this::jump3,
                this::jump6,
                this::jump1,
                this::jump1,
                this::jump2,
                this::jump1,
                this::jump0,
                this::special_0c0h,  // 0c0h
                this::jump4,
                this::jump1,
                this::jump2,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump2,  // 0b8h
                this::jump1,
                this::jump1,
                this::jump2,
                this::jump16,  // 0b4h
                this::jump1,  // 0b3h
                this::jump1,  // 0b2h
                this::jump1  // 0b1h
        };

        jumpTable_0c0h = new Runnable[] {
                this::jump1,  // 0ffh
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,
                this::jump1,  // 0f8h
                this::jump1,
                this::jump1,
                this::jump1
        };
    }


    private static final String part_mes = "Part ";
    private String part_type = "FMOPN";
    private char part_chr = ' ';
    private int part_num = 0;
    private static final String part_chr_n = "\tLength : ";
    private static final String loop_mes = "\t/ Loop : ";
    private static final String loop_mes2 = "\t/ Found Infinite Local Loop!";
    //private String _crlf_mes = "\r\n$";

    private boolean print_flag = false;
    private int all_length = 0; // new int[2] { 0, 0 };
    private int loop_length = 0; // new int[2] { 0, 0 };
    public int max_all = 0; // new int[2] { 0, 0 };
    public int max_loop = 0; // new int[2] { 0, 0 };

    private final int[] fm3_adr = {0, 0, 0};
    private final int[] pcm_adr = {0, 0, 0, 0, 0, 0, 0, 0};

    private boolean loop_flag = false;

    public final char[] _fm3_partchr = {
            (char) 0,
            (char) 0,
            (char) 0
    };

    public final char[] _pcm_partchr = {
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0
    };
}
