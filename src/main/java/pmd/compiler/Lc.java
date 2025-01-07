package pmd.compiler;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;

import musicDriverInterface.MmlDatum;

import static java.lang.System.getLogger;


public class Lc {

    private static final Logger logger = getLogger(Lc.class.getName());

    private Work work;
    private MSeg m_seg;
    private Mc mc;

    public Lc(Mc mc, Work work, MSeg m_seg) {
        this.mc = mc;
        this.work = work;
        this.m_seg = m_seg;
        setJumpTable();
    }

    /**
    //	音長計算用 include file
    //		in.al print_flag(0で非表示)
     */
    //_print_mes macro   ofs
    //local   exit
    //   cmp[print_flag],0
    //    jz exit
    //    print_mes ofs
    //exit:
    //	endm
    public void lc_proc(byte al) {
        enmPart_ends ret;
        ret = enmPart_ends.calc_start;

        //List<byte> dst = new List<byte>();
        //dst.add(0);
        //for (int i = 0; i < MSeg.m_buf.size(); i++) {
        //    MmlDatum o = MSeg.m_buf.Get(i);
        //    dst.add((byte)(o == null ? 0xff : o.dat));
        //}
        //Files.write(Path.of("c:\\temp\\debug"), dst.ToArray());

        do {
            switch (ret) {
                case calc_start:
                    ret = calc_start(al);
                    break;
                case part_loop:
                    ret = part_loop();
                    break;
                case part_loop2:
                    ret = part_loop2();
                    break;
                case check_j:
                    ret = check_j();
                    break;
                case com_loop:
                    ret = com_loop();
                    break;
                case part_ends:
                    ret = part_ends();
                    break;
                case partk_start:
                    ret = partk_start();
                    break;
                case kcom_loop:
                    ret = kcom_loop();
                    break;
                case kpart_end:
                    ret = kpart_end();
                    break;
                case kl_00:
                    ret = kl_00();
                    break;
            }
        } while (ret != enmPart_ends.exit);
    }

    /**
    //	計算開始
     */
    private enmPart_ends calc_start(byte al) {
        print_flag = al;
        part_chr = 'A';
        work.bp = 0; // offset m_buf

        return enmPart_ends.part_loop;
    }

    /**
    //	パート毎のループ
     */
    private enmPart_ends part_loop() {
        work.si = (m_seg.m_buf.get(work.bp).dat & 0xff) + (m_seg.m_buf.get(work.bp + 1).dat & 0xff) * 0x100;
        work.si += 0; // offset m_buf
        work.bp += 2;

        return enmPart_ends.part_loop2;
    }

    private enmPart_ends part_loop2() {
        all_length = 0;
        loop_length = -1;
        loop_flag = 0;

        /**
        //	(Part Aの場合) 拡張のFM3ch目があるか調べる
         */
        if (part_chr != 'A') return enmPart_ends.check_j;
        if (m_seg.m_buf.get(work.si).dat != 0xc6) return enmPart_ends.check_j;

        work.si++;

        for (int i = 0; i < 3; i++) {
            byte l = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            byte h = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            fm3_adr[i] = (h & 0xff) * 0x100 + (l & 0xff);
        }

        return enmPart_ends.check_j;
    }

    /**
    //	(Part Jの場合) 拡張のPCMパートがあるか調べる
     */
    private enmPart_ends check_j() {
        if (part_chr != 'J') return enmPart_ends.com_loop; //jnz com_loop
        if (m_seg.m_buf.get(work.si).dat != 0xb4) return enmPart_ends.com_loop;

        work.si++;

        for (int i = 0; i < 8; i++) {
            byte l = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            byte h = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            pcm_adr[i] = h * 0x100 + l;
        }

        return enmPart_ends.com_loop;
    }

    /**
    //	コマンド毎のループ
     */
    private enmPart_ends com_loop() {

        logger.log(Level.DEBUG, "partType:%s partCh:%c partNum:%d".formatted(part_type, part_chr, part_num));

        do {
            MmlDatum al;
            do {
                logger.log(Level.TRACE, String.format("si:%d", work.si));

                al = (work.si < m_seg.m_buf.size() ? m_seg.m_buf.get(work.si++) : new MmlDatum(0x80));
                if (al.dat == 0x80) return enmPart_ends.part_ends;
                if (al.dat >= 0x80) break;

                al = m_seg.m_buf.get(work.si++);
                //byte ah = 0;

                all_length += al.dat;
            } while (true);

            //cl_00:;
            command_exec((byte) (al.dat & 0xff));
            if (loop_flag != 0) return enmPart_ends.part_ends;

        } while (true);

        //return enmPart_ends.part_ends;
    }

    /**
    //	パート終了
     */
    private enmPart_ends part_ends() {
        print_length();

        part_chr++;
        part_num++;
        if (part_chr == 'G') part_type = "SSG";
        if (part_chr == 'H') part_type = "SSG";
        if (part_chr == 'I') part_type = "SSG";
        if (part_chr < 'K') return enmPart_ends.part_loop;

        int di = work.di;
        work.di = 0; // offset fm3_adr1
        int bx = 0; // offset _fm3_partchr1;in cs
        int cx = 3 + 8;
//extend_check_loop:;
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
                return enmPart_ends.part_loop2;
            }
//extend_check_next:
            work.di++;
            bx++;
            cx--;
        } while (cx > 0);

        work.di = di;
        return enmPart_ends.partk_start;
    }

    /**
    //	Part K
     */
    private enmPart_ends partk_start() {
        part_chr = 'K';
        all_length = 0;
        loop_length = -1;
        loop_flag = 0;

        //logger.log(Level.TRACE, "bp:%d", Work.bp);
        work.si = m_seg.m_buf.get(work.bp).dat + (m_seg.m_buf.get(work.bp + 1).dat * 0x100);
        work.si += 0; // offset m_buf
        work.bp += 2;
        work.bx = m_seg.m_buf.get(work.bp).dat + (m_seg.m_buf.get(work.bp + 1).dat * 0x100);
        work.bx += 0; // offset m_buf	; bx= R table 先頭番地

        return enmPart_ends.kcom_loop;
    }

    /**
    //	Kpart/コマンド毎のループ
     */
    private enmPart_ends kcom_loop() {
        do {
            MmlDatum ald;
            byte al;
            ald = (work.si < m_seg.m_buf.size()) ? m_seg.m_buf.get(work.si++) : (new MmlDatum(0x80));
            al = (byte) (ald.dat & 0xff);
            if (al == (byte) 0x80) return enmPart_ends.kpart_end;
            if ((al & 0xff) >= 0x80) {
                work.al = al;
                return enmPart_ends.kl_00;
            }
            al *= 2;

            //    push    si
            //    push    bx
            int si = work.si;
            int bx = work.bx;
            work.bx += al;
            work.si = m_seg.m_buf.get(work.bx).dat + (m_seg.m_buf.get(work.bx + 1).dat * 0x100);
            //logger.log(Level.TRACE, "bx:%d si:%d", Work.bx, Work.si);
            work.si += 0; // offset m_buf
            rcom_loop();
            work.bx = bx;
            work.si = si;

            if (loop_flag != 0) return enmPart_ends.kpart_end;
        } while (true);
    }

    /**
    //	Kpart/各種特殊コマンド
     */
    private enmPart_ends kl_00() {
        int bx = work.bx;
        command_exec(work.al);
        work.bx = bx;

        if (loop_flag != 0) return enmPart_ends.kpart_end;
        return enmPart_ends.kcom_loop;
    }

    /**
    //	Kpart/計算終了
     */
    private enmPart_ends kpart_end() {
        print_length();
        return enmPart_ends.exit;
    }

    private enum enmPart_ends {
        calc_start,
        part_loop,
        part_loop2,
        check_j,
        com_loop,
        part_ends,
        partk_start,
        kcom_loop,
        kpart_end,
        kl_00,
        exit
    }

    /**
    //	Rpart/コマンド毎のループ
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
                //rl_01:;
                al = m_seg.m_buf.get(work.si++);
                all_length += al.dat;

            } while (true);
            /**
            // Rpart / 各種特殊コマンド処理
             */
            // rl_00:
            command_exec((byte) (al.dat & 0xff));
            if (loop_flag != 0) break rpart_end;
        } while (true);
        /**
        // Rpart / 計算終了
         */
    }

    /**
    //	各種コマンド
     */
    private void command_exec(byte al) {
        al = (byte) ~al;
        int ax = al;
        ax += 0; // offset jumptable
        jumptable[ax].run();
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
    //	tempo
     */
    private void _tempo() {
        byte al = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        if (al >= (byte) 251) {
            work.si++; // 相対
        }
        //tempo_ret:;
    }

    /**
    //	ポルタメント
     */
    private void porta() {
        work.si += 2;

        byte al = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        all_length += al;
    }

    /**
    //	L command
     */
    private void loop_set() {
        loop_length = all_length;
    }

    /**
    //	[command
     */
    private void loop_start() {
        int ax = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        ax += m_seg.m_buf.get(work.si++).dat * 0x100;

        work.bx = ax;
        work.bx += 1; // offset m_buf+1
        m_seg.m_buf.set(work.bx, new musicDriverInterface.MmlDatum(0));
    }

    /**
    //	] command
     */
    private void loop_end() {
        byte al = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        if (al != 0) { // break loop_fset; // 無条件loopがあった
            byte ah = al;
            m_seg.m_buf.set(work.si, new musicDriverInterface.MmlDatum(m_seg.m_buf.get(work.si).dat + 1));
            al = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            if (ah == al) { // break reloop;
                work.si++;
                work.si++;
                return;
            }
//reloop:

            int ax = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
            ax += m_seg.m_buf.get(work.si++).dat * 0x100;
            ax += 2; // offset m_buf+2
            work.si = ax;
            return;
        }
//loop_fset:
        loop_flag = 1;
    }

    /**
    //	: command
     */
    private void loop_exit() {
        int ax = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        ax += m_seg.m_buf.get(work.si++).dat * 0x100;
        work.bx = ax;
        work.bx += 0; // offset m_buf
        byte dl = (byte) (m_seg.m_buf.get(work.bx).dat & 0xff);
        dl--;
        work.bx++;
        if (dl != (byte) m_seg.m_buf.get(work.bx).dat) { // break loopexit;
            return;
        }
//loopexit:
        work.bx += 3;
        work.si = work.bx;
    }

    /**
    //	0c0h + ?? special control
     */
    private void special_0c0h() {
        byte al = (byte) (m_seg.m_buf.get(work.si++).dat & 0xff);
        if (al >= 2) { // break spc0_ret;
            al = (byte) ~al;
            al += 0; // offset jumptable_0c0h
            jumptable_0c0h[al].run();
        }
//spc0_ret:
    }

    /**
    //	長さを表示
     */
    private void print_length() {
        int tc = 0;
        int lc = 0;

        if (all_length == 0) return; // データ無し
        String msg = part_mes + part_chr + part_chr_n;

        int ax = all_length & 0xffff;
        int dx = (all_length >> 16) & 0xffff;
        int bx = max_all & 0xffff;
        int cx = (max_all >> 16) & 0xffff;
        if (max_all - all_length < 0) {
            max_all = all_length; // ax | dx * 0x10000;
        }
        //not_over_all:;
        msg += String.format("%d", ax);
        tc = ax;

        if (loop_flag == 1) { // break pe_loop;
            if (print_flag != 0) {
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
            //not_over_loop:;
            msg += String.format("%d", n);
            lc = n;
        }
//pe_00:
        if (print_flag != 0) {
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
        //pe_01:;
    }

    private Runnable[] jumptable;
    private Runnable[] jumptable_0c0h;

    private void setJumpTable() {
        jumptable = new Runnable[] {
                this::jump1 // 0ffh
                 , this::jump1
                 , this::jump1
                 , this::_tempo
                 , this::jump0
                 , this::jump2
                 , this::loop_start
                 , this::loop_end // 0f8h
                 , this::loop_exit
                 , this::loop_set
                 , this::jump1
                 , this::jump0
                 , this::jump0
                 , this::jump4
                 , this::jump1
                 , this::jump4 // 0f0h
                 , this::jump2
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1 // 0e8h
                 , this::jump1
                 , this::jump1
                 , this::jump2
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1 // 0e0h
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::porta
                 , this::jump1
                 , this::jump1 // 0d8h
                 , this::jump1
                 , this::jump2
                 , this::jump2
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1 // 0d0h
                 , this::jump1
                 , this::jump6
                 , this::jump5
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump3 // 0c8h
                 , this::jump3
                 , this::jump6
                 , this::jump1
                 , this::jump1
                 , this::jump2
                 , this::jump1
                 , this::jump0
                 , this::special_0c0h // 0c0h
                 , this::jump4
                 , this::jump1
                 , this::jump2
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump2 // 0b8h
                 , this::jump1
                 , this::jump1
                 , this::jump2
                 , this::jump16 // 0b4h
                 , this::jump1 // 0b3h
                 , this::jump1 // 0b2h
                 , this::jump1 // 0b1h
        };

        jumptable_0c0h = new Runnable[] {
                this::jump1  // 0ffh
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1
                 , this::jump1 // 0f8h
                 , this::jump1
                 , this::jump1
                 , this::jump1
        };
    }


    public static final String part_mes = "Part ";
    public String part_type = "FMOPN";
    public char part_chr = ' ';
    public int part_num = 0;
    private static final String part_chr_n = "\tLength : ";
    private static final String loop_mes = "\t/ Loop : ";
    private static final String loop_mes2 = "\t/ Found Infinite Local Loop!";
    //private String _crlf_mes = "\r\n$";

    public byte print_flag = 0;
    public int all_length = 0; // new int[2] { 0, 0 };
    public int loop_length = 0; // new int[2] { 0, 0 };
    public int max_all = 0; // new int[2] { 0, 0 };
    public int max_loop = 0; // new int[2] { 0, 0 };

    public final int[] fm3_adr = {0, 0, 0};
    public final int[] pcm_adr = {0, 0, 0, 0, 0, 0, 0, 0};

    public byte loop_flag = 0;

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
