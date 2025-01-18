package pmd.compiler;

import java.awt.Point;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Stack;
import java.util.function.Supplier;

import dotnet4j.io.Path;
import dotnet4j.util.compat.Tuple;
import musicDriverInterface.LinePos;
import musicDriverInterface.MMLType;
import musicDriverInterface.MmlDatum;
import pmd.common.PmdDosExitException;
import pmd.common.PmdDosExitException.PmdErrorExitException;
import pmd.common.PmdException;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static pmd.compiler.FNumDatSeg.fnumTbl;


public class Mc {

    private static final Logger logger = getLogger(Mc.class.getName());

    ResourceBundle rb = ResourceBundle.getBundle("lang/message");

    private Compiler compiler;
    private String[] args;
    private Work work;
    private Lc lc;
    private MSeg m_seg;
    private FNumDatSeg fnumdat_seg = null;
    private HsSeg hs_seg = null;
    public MmlSeg mml_seg;
    public VoiceSeg voice_seg;

    //DotNET独自パラメータ
    public byte[] outVoiceBuf = null; // 音色出力用バッファ(ファイル名はv_filename)
    public int memo_writeAddress = -1;
    public int vdat_setAddress = -1;
    public Point skipPoint = new Point(0, 0); // スキップ処理:mml上の行と桁を示す
    public int skipIndex = -1; // スキップ処理:mファイル上の位置を示す
    public int skipSW = 0; // スキップ処理:進捗を示す
    //0 : mml上のスキップ位置の行に達するのを待っている状態
    //1 : mml上のスキップ位置の桁に達するのを待っている状態
    //2 : 行と桁が達成されたので音程コマンドがやってくるのを待っている状態
    //3 : 処理完了
    public int skipPointCol = -1; // スキップ処理:桁の位置をmml上の文字数に置き換えた値

    /**
    //
    // MML Compiler/Effect Compiler FOR PC-9801/88VA
    //       ver 4.8s
    //
     */
    // .186

    public static final String ver = "4.8s"; // version
    public static final int vers = 0x48;
    public static final String date = "2023/09/23"; // date

//#if !hyouka
//        public int hyouka = 0; // 1で評価版(save機能cut)
//#endif

//#if !efc
    public int efc = 0; // FM効果音コンパイラかどうか
//#endif

    /**
    //
    // ＭＭＬコンパイラです．
    // MC filename[.MML](CR)
    // で，コンパイルして，filename.Mというファイルを作成します．
    //
    // また、
    // MC filename[.MML] voice_filename[.FF]
    // で，音色データ付きのfilename.Mを作成します．
    // このデータは，音色データを読まなくても，単体で演奏できます．
    // また、音色定義コマンド(行頭@)が使用可能になります．
    //
    // 下のefcの値を１にすると、FM効果音コンパイラになります。
    // EFC filename[.EML] voice_filename[.FF] (CR)
    // で，コンパイルして，filename.EFCというファイルを作成します．
    //
     */

    public int olddat = 0; // v2.92以前のデータ作成
    public int split = 0; // 音色データがＳＰＬＩＴ形式かどうか
    public int tempo_old_flag = 0; // テンポ処理 新旧flag
    public int pmdvector = 0x60; // VRTC.割り込み
    public static final char cr = (char) 13;
    public static final char lf = (char) 10;
    public static final char eof = '$';

    /**
    // macros
     */

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
        String[] a = qq.split("" + Mc.cr + Mc.lf);
        for (String s : a)
            System.out.println(s);
    }

    public void print_mes_err(String qq) {
        //コンソールへメッセージ表示
        String[] a = qq.split("" + Mc.cr + Mc.lf);
        for (String s : a)
            logger.log(Level.ERROR, s);
    }

    public void print_chr(String qq) {
        //コンソールへ文字表示
        logger.log(Level.INFO, qq);
    }

    public void print_line(String bx) {
        //コンソールへメッセージ表示(bx位置から0まで)
        logger.log(Level.INFO, bx);
    }

    public void pmd(int qq) {
        //mov ah, qq
        //int pmdvector
        //endm
        //ベクターからファンクション呼び出し
    }

    public static final int mstart = 0;
    public static final int mstop = 1;
    public static final int fout = 2;
    public static final int efcon = 3;
    public static final int efcoff = 4;
    public static final int getss = 5;
    public static final int get_music_adr = 6;
    public static final int get_tone_adr = 7;
    public static final int getfv = 8;
    public static final int board_check = 9;
    public static final int get_status = 10;
    public static final int get_efc_adr = 11;
    public static final int fm_efcon = 12;
    public static final int fm_efcoff = 13;
    public static final int get_pcm_adr = 14;
    public static final int pcm_efcon = 15;
    public static final int get_workadr = 16;
    public static final int get_fmefc_num = 17;
    public static final int get_pcmefc_num = 18;
    public static final int set_fm_int = 19;
    public static final int set_efc_int = 20;
    public static final int get_effon = 21;
    public static final int get_joystick = 22;
    public static final int get_pcmdrv_flag = 23;
    public static final int set_pcmdrv_flag = 24;
    public static final int set_fout_vol = 25;
    public static final int pause_on = 26;
    public static final int pause_off = 27;
    public static final int ff_music = 28;
    public static final int get_memo = 29;

    /**
    // main program
     */

    //code segment para public 'code'
    //assume cs:code,ss:stack

    //Mc  proc

    public Mc(Compiler compiler, String[] args, String srcBuf, byte[] ffBuf, Work work, String[] kankyo_seg) {
        this.compiler = compiler;
        this.args = args;
        this.work = work;
        this.kankyo_seg = kankyo_seg;

        voice_seg = new VoiceSeg();
        m_seg = new MSeg();
        lc = new Lc(this, work, m_seg);
        mml_seg = new MmlSeg();
        mml_seg.mml_buf = srcBuf;
        //行末がEOFの場合は改行を追加する
        if (srcBuf.length() > 1 && srcBuf.lastIndexOf("\r\n") != srcBuf.length() - 2) {
            mml_seg.mml_buf += "\r\n";
        }
        voiceTrancer(ffBuf);
        setupComTbl();
        setupRcomtbl();
        fnumdat_seg = new FNumDatSeg();
        hs_seg = new HsSeg();
    }

    private void voiceTrancer(byte[] ffBuf) {
        voice_seg.voice_buf = new byte[8192];
        if (ffBuf == null || ffBuf.length < 1) return;
        for (int i = 0; i < ffBuf.length; i++)
            voice_seg.voice_buf[i] = ffBuf[i];
    }

    /**
    //  compile start
     */
    public MmlDatum[] compile_start() {
        print_mes(mml_seg.titmes);

        if (args == null || args.length == 0) {
            usage();
            return null;
        }

        //コンパイルプロセス開始
        {
            int i = ReadOption();
            ReadMMLFileName(i++);
            //ReadMML(); // 不要
            ChangeMMLToMFileName();
            TransVoiceDataFromPMD();
            get_ff(i);
            clear_voicetable();
            CheckPrgFlgOnEfc();
            InitVariableBuffer();
            SetFromEnvironment();
        }

        enmPass2JumpTable ret = enmPass2JumpTable.Pass1;
        do {
            ret = Jumper(ret);

            if (ret == enmPass2JumpTable.exit)
                break;

        } while (true);

        //コンパイル完了

        //音色データ取得
        outVoiceBuf = write_ff();

        //.Mファイルデータの整形(m_bufが出力データの実態になるよう、m_startをはじめに追加する)
        List<MmlDatum> dst = new ArrayList<>();
        dst.add(new MmlDatum(m_seg.m_start & 0xff));
        for (int i = 0; i < m_seg.m_buf.size(); i++) dst.add(m_seg.m_buf.get(i));
        for (int i = 0; i < dst.size(); i++) m_seg.m_buf.set(i, dst.get(i));

        //コンパイル完了(メッセージを表示するのみ)
        compile_fin();

        return m_seg.m_buf.toArray(MmlDatum[]::new);
    }

    private enmPass2JumpTable Jumper(enmPass2JumpTable ret) {
//#if DEBUG
        logger.log(Level.TRACE, String.format("jp:%s", ret));
//#endif
        switch (ret) {
            //Pass1

            case Pass1:
                ret = Pass1();
                break;

            case mainLoopPass1:
                ret = mainLoopPass1();
                break;

            case InitLoopCount:
                ret = InitLoopCount();
                break;

            //Pass2

            case Pass2CompileStart:
                ret = Pass2CompileStart();
                break;
            case cmloop:
                ret = cmloop();
                break;
            case part_stadr_set:
                ret = part_stadr_set();
                break;
            case cmloop2:
                ret = cmloop2();
                break;
            case cloop:
                ret = cloop();
                break;
            case part_end:
                ret = part_end();
                break;
            case check_lopcnt:
                ret = check_lopcnt();
                break;

            case hsset:
                ret = hsset();
                break;
            case one_line_compile:
                ret = one_line_compile();
                break;
            case rem_set:
                ret = rem_set();
                break;

            case vdat_set:
                ret = vdat_set();
                break;
            case nd_s_loop:
                ret = nd_s_loop();
                break;
            case nd_s_exit:
                ret = nd_s_exit();
                break;

            case memo_write:
                ret = memo_write();
                break;

            case hscom_exit:
                ret = hscom_exit();
                break;

            case forceReturn:
                return ret;
            //throw new Exception("ここでreturnにはならないはず");

            case olc0:
                ret = olc0();
                break;

            case olc00:
                ret = olc00();
                break;

            case olc02:
                ret = olc02();
                break;

            case olc03:
                ret = olc03();
                break;

            case ots002:
                ret = ots002();
                break;

            case olc_skip2:
                ret = olc_skip2();
                break;

            case skip_mml:
                ret = skip_mml();
                break;

            case parset:
                ret = parset();
                break;

            case vset:
                ret = vset();
                break;

            case vsetm:
                ret = vsetm();
                break;

            case vsetm1:
                ret = vsetm1();
                break;

            case vss:
                ret = vss();
                break;

            case vss2:
                ret = vss2();
                break;

            case vss2m:
                ret = vss2m();
                break;

            case lngrew:
                ret = lngrew();
                break;

            case lng_dec:
                ret = lng_dec();
                break;

            case tieset_2:
                ret = tieset_2();
                break;

            case lngmul:
                ret = lngmul();
                break;

            case p1c_fin:
                line_skip();
                ret = enmPass2JumpTable.mainLoopPass1;
                break;

            case rskip:
                ret = rskip();
                break;

            case rtloop:
                ret = rtloop();
                break;

            case rtlp2:
                ret = rtlp2();
                break;

        }

        return ret;
    }

    private enum enmPass2JumpTable {
        Pass1,
        mainLoopPass1,
        InitLoopCount,

        Pass2CompileStart,
        cmloop,
        part_stadr_set,
        cmloop2,
        cloop,
        part_end,
        check_lopcnt,

        hsset,
        one_line_compile,
        rem_set,

        vdat_set,
        memo_write,
        nd_s_loop,
        nd_s_exit,
        forceReturn,
        hscom_exit,
        olc0,
        olc00,
        olc02,
        olc03,
        ots002,
        olc_skip2,
        skip_mml,
        parset,

        vset,
        vsetm,
        vsetm1,
        vss,
        vss2,
        vss2m,

        lngrew,
        lng_dec,
        tieset_2,
        lngmul,

        p1c_fin,
        rskip,
        rtloop,
        rtlp2,

        exit,
        bunsan_end
    }

    /**
    //  コマンドラインから /optionの読みとり
     */
    private int ReadOption() {
        mml_seg.part = 0;
        mml_seg.ff_flg = 0;
        mml_seg.x68_flg = 0;
        mml_seg.dt2_flg = 0;
        mml_seg.opl_flg = 0;

        mml_seg.save_flg = 1;
        mml_seg.memo_flg = 1;
        mml_seg.pcm_flg = 1;

//#if hyouka
//        mml_seg.play_flg = 1;
//        mml_seg.prg_flg = 2;
//#else
        mml_seg.play_flg = 0;
        mml_seg.prg_flg = 0;
//#endif

        String envVal = "";
        int[] index = new int[1], col = new int[1];
        boolean cry = search_env(mml_seg.mcopt_txt, kankyo_seg, /* out */ index, /* out */ col);
        if (cry) {
            envVal = kankyo_seg[index[0]].substring(col[0]);
            if (get_option(envVal)) {
                print_mes(mml_seg.warning_mes);
                print_mes(mml_seg.mcopt_err_mes);
            }
        }

        int i = 0;
        for (i = 0; i < args.length; i++) {
            if (get_option(args[i])) break;
        }

        return i;
    }

    /**
    //  コマンドラインから.mmlのファイル名の取り込み
     */
    private void ReadMMLFileName(int i) {
        mml_seg.mml_filename = args[i];
        if (mml_seg.mml_filename.lastIndexOf('.') == -1) {
//#if efc
//            mml_seg.mml_filename += ".EML";
//#else
            mml_seg.mml_filename += ".MML";
//#endif
        }

        mml_seg.includeFileHistory.clear();
        mml_seg.includeFileHistory.add(mml_seg.mml_filename);
    }

    /**
    //  .mmlファイルの読み込み
     */
    private void ReadMMLFile() {
    }

    /**
    //  .mmlを.mに変更して設定
     */
    private void ChangeMMLToMFileName() {
//#if !hyouka
//#if efc
//        m_seg.m_filename = mml_seg.mml_filename.substring(0, mml_seg.mml_filename.lastIndexOf('.')) + ".EFC";
//#else
        m_seg.m_filename = mml_seg.mml_filename.substring(0, mml_seg.mml_filename.lastIndexOf('.')) + ".M";
//#endif
//#endif
    }

    /**
    // 音色データ領域を転送してくる（PMD常駐時）又はクリア
     */
    private void TransVoiceDataFromPMD() {

        // PMDからもらってくる機能は省略

        mml_seg.pmd_flg = 0;
        if (voice_seg.voice_buf == null) {
            voice_seg.voice_buf = new byte[8192];
            for (int i = 0; i < voice_seg.voice_buf.length; i++) voice_seg.voice_buf[i] = 0;
        }
    }

    /**
    //  コマンドラインから.ffのファイル名を取り込む
     */
    private void get_ff(int i) {
        if (i < args.length) {
            mml_seg.ff_flg = 1;
            voice_seg.v_filename = args[i];
        }
    }

    /**
    //  音色テーブルの初期化
     */
    private void clear_voicetable() {
        for (int i = 0; i < mml_seg.prg_num.length; i++) {
            mml_seg.prg_num[i] = 0;
        }
    }

    /**
    //  compile main
     */
    private void CheckPrgFlgOnEfc() {
//#if efc
//        if (mml_seg.prg_flg == 1) {
//            error(0, 28, 0);
//        }
//#endif
    }

    /**
    // 変数バッファ/文字列offsetバッファ初期化
     */
    private void InitVariableBuffer() {

        for (int i = 0; i < hs_seg.hsbuf2.length; i++) {
            hs_seg.hsbuf2[i] = 0;
        }
        for (int i = 0; i < hs_seg.hsbuf3.length; i++) {
            hs_seg.hsbuf3[i] = 0;
        }

        for (int i = 0; i < 128 * 8; i++) {
            work.ppzfile_buf[mml_seg.ppzfile_adr + i] = 0;
        }
    }

    /**
    // 環境変数 user = , composer = , arranger = を検索して設定
     */
    private void SetFromEnvironment() {
        // "USER=" 検索
        int[] index = new int[1], col = new int[1];
        if (search_env(mml_seg.user_txt, kankyo_seg, /* out */ index, /* out */ col)) {
            mml_seg.composer_adr = 0;
            mml_seg.composer_seg = kankyo_seg[index[0]].substring(col[0]);
            mml_seg.arranger_adr = 0;
            mml_seg.arranger_seg = kankyo_seg[index[0]].substring(col[0]);
        }

        //"COMPOSER=" 検索
        if (search_env(mml_seg.composer_txt, kankyo_seg, /* out */ index, /* out */ col)) {
            mml_seg.composer_adr = 0;
            mml_seg.composer_seg = kankyo_seg[index[0]].substring(col[0]);
        }

        //"ARRANGER=" 検索
        if (search_env(mml_seg.arranger_txt, kankyo_seg, /* out */ index, /* out */ col)) {
            mml_seg.arranger_adr = 0;
            mml_seg.arranger_seg = kankyo_seg[index[0]].substring(col[0]);
        }
    }

    /**
    // Pass1
    /**
    /**
    // Workの初期化(pass1)
     */
    private enmPass2JumpTable Pass1() {
        work.si = 0; // MmlSeg.mml_buf;

        mml_seg.part = 0;
        mml_seg.pass = 0;

        m_seg.mbuf_end = 0x7f; // check code

        mml_seg.skip_flag = 0;

        return enmPass2JumpTable.mainLoopPass1;
    }

    //529-574
    /**
    // Ｍain Ｌoop(pass1)
     */
    private enmPass2JumpTable mainLoopPass1() {
        do {
            //p1cloop:
            mml_seg.linehead = work.si;

//p1c_next:
            while (true) {
                char al = (work.si < mml_seg.mml_buf.length()) ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
                if (al == 0x1a) break;

                if (al <= ' ') break; // p1c_fin;

                if (al == ';') break; // p1c_fin;

                if (al == '`') {
                    mml_seg.skip_flag ^= 2;
                    continue; // break p1c_next;
                }

                //p1c_no_skip:
                if ((mml_seg.skip_flag & 2) != 0) continue; // break p1c_next;

                if (al == '!') {
                    return enmPass2JumpTable.hsset;
                } else if (al == '#') {
                    return macro_set();
                } else if (al == '@') {
                    return new_neiro_set();
                } else {
                    continue; // break p1c_next;
                }
            }
//p1c_fin:
            line_skip();
        } while (work.si < mml_seg.mml_buf.length());

        //p1_end:
        return enmPass2JumpTable.InitLoopCount;
    }

    /**
    // ループカウント初期化
     */
    private enmPass2JumpTable InitLoopCount() {
        mml_seg.lopcnt = 0;
        return enmPass2JumpTable.Pass2CompileStart;
    }

    /**
    // Pass2 Compile Start
     */
    private enmPass2JumpTable Pass2CompileStart() {

//#if !efc || !olddat
        m_seg.m_start = (byte) (mml_seg.opl_flg * 2 | mml_seg.x68_flg); // 音源flag set
//#endif

        work.di = (mml_seg.max_part + 1) * 2; // KUMA: ? -> ver48sで理解w
        work.di += 0; // offset m_buf
        if ((mml_seg.prg_flg & 1) != 0) {
            work.di += 2;
        }

        mml_seg.hsflag = 0;
        mml_seg.prsok = 0;

        mml_seg.part = 1;
        mml_seg.pass = 1;
        mml_seg.chipCh = 0;

        return enmPass2JumpTable.cmloop;
    }

    //607-641
    /**
    // 音源の選択
     */
    private enmPass2JumpTable cmloop() {
//#if !efc

        int al = mml_seg.opl_flg;
        al |= mml_seg.x68_flg;
        if (al != 0) {
            /**
            // OPM/OPLの場合
             */
            if (mml_seg.part != 10) {
                mml_seg.ongen = mml_seg.fm;
            } else {
                mml_seg.ongen = mml_seg.pcm;
            }
        } else {
            /**
            // OPNの場合
             */
            al = mml_seg.part;
            byte ah = 0;
            while (al >= 4) {
                al -= 3;
                ah++;
            }
            mml_seg.ongen = ah;
        }
//#endif
        return enmPass2JumpTable.part_stadr_set;
    }

    /**
    // パートのスタートアドレスのセット
     */
    private enmPass2JumpTable part_stadr_set() {
        int bx = mml_seg.part;
        bx--;
        bx *= 2;
        bx += 0; // offset m_buf

        int dx = work.di;
        dx -= 0; // offset m_buf

        MmlDatum ml = new MmlDatum(dx & 0xff);
        MmlDatum mh = new MmlDatum((dx & 0xff00) >> 8);
        m_seg.m_buf.set(bx++, ml);
        m_seg.m_buf.set(bx, mh);

        return enmPass2JumpTable.cmloop2;
    }

    /**
    // Workの初期化
     */
    private enmPass2JumpTable cmloop2() {
        work.si = 0; // offset mml_buf
        logger.log(Level.DEBUG, "chipCh:%d".formatted(mml_seg.chipCh));
        cm_init();

        byte ah, al;

//#if !efc
        if (mml_seg.part == 1) {
            if (mml_seg.fm3_partchr1 != 0) {

                al = (byte) 0xc6; // FM3 拡張パートの指定(partA)
                m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                mml_seg.fm3_ofsadr = work.di;

                for (int cx = 0; cx < 3; cx++) {
                    m_seg.m_buf.set(work.di++, new MmlDatum(0));
                    m_seg.m_buf.set(work.di++, new MmlDatum(0));
                }
            }
        }

        if (mml_seg.part == mml_seg.pcmpart) {
            if (mml_seg.pcm_partchr[0] != 0) {

                al = (byte) 0xb4; // PCM 拡張パートの指定(partJ)
                m_seg.m_buf.set(work.di++, new MmlDatum(al));
                mml_seg.pcm_ofsadr = work.di;

                for (int cx = 0; cx < 8; cx++) {
                    m_seg.m_buf.set(work.di++, new MmlDatum(0));
                    m_seg.m_buf.set(work.di++, new MmlDatum(0));
                }
            }
        }

        if (mml_seg.part == 7) { // break not_partG;

            if (mml_seg.zenlen != 96) {
                ah = (byte) (mml_seg.zenlen & 0xff);
                al = (byte) 0xdf;
                m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Zenlenが指定されている場合は Zコマンド発行(partG)
                m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
            }

//#if !tempo_old_flag

        if (mml_seg.tempo != 0) {
            ah = (byte) 0xff;
            al = (byte) 0xfc;
            m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Tempoが指定されている場合は tコマンド発行(partG)
            m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
            al = (byte) (mml_seg.tempo & 0xff);
            m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
        }

//#endif
            if (mml_seg.timerb != 0) {
                ah = (byte) (mml_seg.timerb & 0xff);
                al = (byte) 0xfc;
                m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Timerが指定されている場合は Tコマンド発行(partG)
                m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
            }

            if (mml_seg.towns_flg != 1) { // break not_partG; // TOWNSは4.6f @@@@

                if (mml_seg.fm_voldown != 0) {
                    ah = (byte) 0xfe;
                    al = (byte) 0xc0;
                    ah += (byte) (mml_seg.fm_voldown_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.fm_voldown & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Voldownが指定されている場合は DFコマンド発行(partG)
                }

                if (mml_seg.ssg_voldown != 0) {
                    ah = (byte) 0xfc;
                    al = (byte) 0xc0;
                    ah += (byte) (mml_seg.ssg_voldown_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.ssg_voldown & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Voldownが指定されている場合は DSコマンド発行(partG)
                }

                if (mml_seg.pcm_voldown != 0) {
                    ah = (byte) 0xfa;
                    al = (byte) 0xc0;
                    ah += (byte) (mml_seg.pcm_voldown_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.pcm_voldown & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Voldownが指定されている場合は DPコマンド発行(partG)
                }

                if (mml_seg.ppz_voldown != 0) {
                    ah = (byte) 0xf5;
                    al = (byte) 0xc0;
                    ah += (byte) (mml_seg.ppz_voldown_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.ppz_voldown & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Voldownが指定されている場合は DZコマンド発行(partG)
                }

                if (mml_seg.rhythm_voldown != 0) {
                    ah = (byte) 0xf8;
                    al = (byte) 0xc0;
                    ah += (byte) (mml_seg.rhythm_voldown_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.rhythm_voldown & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // #Voldownが指定されている場合は DRコマンド発行(partG)
                }
            }
        }
//not_partG:

        al = (byte) (mml_seg.opl_flg | mml_seg.x68_flg);
        if (al == 0) { // OPM/OPL=DX/EXを却下
            if (mml_seg.ext_detune != 0) {
                if (mml_seg.ongen == mml_seg.psg) {
                    // PSGのみ
                    ah = (byte) 0x01;
                    al = (byte) 0xcc;
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Extend Detune Set(Partの頭)
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
                }
            }

            if (mml_seg.ext_env != 0) {
                if (mml_seg.ongen >= mml_seg.psg) { // FMは却下
                    if (mml_seg.part != mml_seg.rhythm2) { // Rhythmは却下
                        ah = 0x01;
                        al = (byte) 0xc9;
                        m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Extend Envelope Set(Partの頭)
                        m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
                    }
                }
            }
        }

        if (mml_seg.ext_lfo != 0) {
            if (mml_seg.part != mml_seg.rhythm2) { // Rhythmは却下
                ah = 0x01;
                al = (byte) 0xca;
                m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Extend LFO Set(Partの頭)
                m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                if (mml_seg.towns_flg != 1) { // TOWNSは4.6f @@@@
                    ah = 0x01;
                    al = (byte) 0xbb;
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Extend LFO Set(Partの頭)
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
                }
            }
        }

        if (mml_seg.towns_flg != 1) { // TOWNSは4.6f @@@@
            if (mml_seg.adpcm_flag != 255) {
                if (mml_seg.part == mml_seg.pcmpart) { // pcmのみ
                    ah = (byte) 0xf7;
                    al = (byte) 0xc0;
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // ADPCM set(partの頭)
                    m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));

                    al = (byte) (mml_seg.adpcm_flag & 0xff);
                    m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
                }
            }
        }

        if (mml_seg.transpose != 0) {
            if (mml_seg.part != mml_seg.rhythm2) { // Rhythmは却下
                ah = (byte) (mml_seg.transpose & 0xff);
                al = (byte) 0xb2;
                m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Master Transpose(Partの頭)
                m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
            }
        }

//#endif

        logger.log(Level.DEBUG, "Part: %c compile start".formatted((char) ('A' - 1 + mml_seg.part)));

        //MmlSeg.includeFileHistoryPos = 0;
        //MmlSeg.currentMMLFile = MmlSeg.includeFileHistory(0);
        //MmlSeg.includeFileHistoryStack.Clear();
        //MmlSeg.includeFileLineStack.Clear();

        return enmPass2JumpTable.cloop;
    }

    /**
    // Main Loop
     */
    private enmPass2JumpTable cloop() {

//c_next:
        while (true) {
            skipPointCol = work.si + skipPoint.x;

            byte ah_b, al_b;
            char al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == 0x1a)
                return enmPass2JumpTable.part_end;
            if (al == 1) {
                //include file enter
                //MmlSeg.includeFileHistoryPos++;
                //MmlSeg.includeFileHistoryStack.push(MmlSeg.currentMMLFile);
                //MmlSeg.includeFileLineStack.push(MmlSeg.line);
                //MmlSeg.line = 0;
                //MmlSeg.currentMMLFile = MmlSeg.includeFileHistory(MmlSeg.includeFileHistoryPos);
            }
            if (al == 2) {
                //include file exit
                //MmlSeg.currentMMLFile = MmlSeg.includeFileHistoryStack.pop();
                //MmlSeg.line = MmlSeg.includeFileLineStack.pop();
            }
            if (al >= (' ' + 1)) { // break c_fin;
                if (al != ';') { // break c_fin;
                    if (al == '`') { // break c_no_skip;

                        mml_seg.skip_flag ^= 2;
//                break c_next;
                        continue;
                    }
//c_no_skip:
                    if ((mml_seg.skip_flag & 2) == 0) {

                        if (al == '!') {
                            return enmPass2JumpTable.hsset;
                        }
                        if (al == '"') {
                            mml_seg.skip_flag ^= 1;

                            al_b = (byte) 0xc0;
                            m_seg.m_buf.set(work.di++, new MmlDatum(al_b & 0xff));
                            al_b = (byte) (mml_seg.skip_flag & 0xff);
                            al_b &= 1;
                            m_seg.m_buf.set(work.di++, new MmlDatum(al_b & 0xff));
//                            break c_next;
                            continue;
                        }

                        if (al == '\'') {
                            mml_seg.skip_flag &= 0xfe;
                            ah_b = 0x00;
                            al_b = (byte) 0xc0;
                            m_seg.m_buf.set(work.di++, new MmlDatum(al_b & 0xff));
                            m_seg.m_buf.set(work.di++, new MmlDatum(ah_b & 0xff));
//                            break c_next;
                            continue;
                        }
                    }
//#if efc
//        Work.si--;
//        if (lngset(/* out */ int bx, /* out */ al_b))break c_fin;
//        al_b++;
//        if (al_b == MmlSeg.part) break one_line_compile;
//#else
                    al_b = (byte) (al & 0xff);
                    ah_b = (byte) (mml_seg.part & 0xff);
                    ah_b += (byte) (char) ('A' - 1);
                    if (al_b == ah_b) {
                        return enmPass2JumpTable.one_line_compile;
                    }
//#endif
//                    break c_next;
                    continue;
                }
            }
//c_fin:
            line_skip();
//            break c_next;
        }
    }

    /**
    // Error Checks
     */
    private enmPass2JumpTable part_end() {
        work.si = 0; // エラー位置は不定
        return enmPass2JumpTable.check_lopcnt;
    }

    private enmPass2JumpTable check_lopcnt() {
        while (mml_seg.lopcnt != 0) { // break loop_ok;

            print_mes(mml_seg.warning_mes);
            put_part();
            print_mes(mml_seg.loop_err_mes);

            m_seg.m_buf.set(work.di, new MmlDatum(0xf8));
            work.di++;

            work.bx = (work.bx & 0xff00) + 1;
            edl00();
        }
//loop_ok:
        if (mml_seg.porta_flag != 0) {
            error('{', 9, work.si);
        }

        if (mml_seg.allloop_flag != 0) { // break non_allloop_error;
            if (mml_seg.length_check1 == 0) {
                error('L', 10, work.si);
            }
        }
//non_allloop_error:
        /**
        // Part Endmark をセット
         */

        List<Object> args = new ArrayList<>();
        args.add(null);
        args.add(null);
        args.add(!m_seg.getMacroLst().isEmpty() ? m_seg.getMacroLst().toArray(MmlDatum[]::new) : null);
        m_seg.getMacroLst().clear();
        MmlDatum md = new MmlDatum(0x80);
        md.args = args;

        m_seg.m_buf.set(work.di, md);
        work.di++;
        /**
        // PART INC. & LOOP
         */
        mml_seg.part++;
        mml_seg.chipCh++;
        if (mml_seg.chipCh == 6) mml_seg.chipCh += 3;
        if (mml_seg.chipCh == 12) mml_seg.chipCh += 6;
//#if efc
//        if (MmlSeg.part < MmlSeg.max_part + 2) break cmloop;
//#else
        if (mml_seg.part < mml_seg.max_part + 1) return enmPass2JumpTable.cmloop;
//#endif

//#if efc
//        break vdat_set;
//#else
        if (mml_seg.part == mml_seg.max_part + 1) { // break fm3_check;
            byte al = (byte) (mml_seg.maxprg & 0xff);
            if (mml_seg.towns_flg == 1) {
                al = 0; // TOWNSはRパート無し
            }

            //maxprg_towns_chk:;
            mml_seg.kpart_maxprg = al; // K partのmaxprgを保存
            al = (byte) (mml_seg.deflng & 0xff);
            mml_seg.deflng_k = al; // l 値を保存
        }
        /**
        // FM3 拡張パートがあればそれをcompile
         */
fm3_check:
        if (mml_seg.fm3_ofsadr == 0) { // break pcm_check; // 無し

            byte al = (byte) (mml_seg.fm3_partchr1 & 0xff);
            mml_seg.fm3_partchr1 = 0;
            mml_seg.chipCh = 6;
            if (al == 0) { // break fm3c_main;

                al = (byte) (mml_seg.fm3_partchr2 & 0xff);
                mml_seg.fm3_partchr2 = 0;
                mml_seg.chipCh = 7;
                if (al == 0) { // break fm3c_main;

                    al = (byte) (mml_seg.fm3_partchr3 & 0xff);
                    mml_seg.fm3_partchr3 = 0;
                    mml_seg.chipCh = 8;
                    if (al == 0) break fm3_check; // pcm_check;
                }
            }
//fm3c_main:
            int bx = mml_seg.fm3_ofsadr;
            int dx = work.di;
            dx -= 0; // offset m_buf
            m_seg.m_buf.set(bx + 0, new MmlDatum(dx & 0x00ff));
            m_seg.m_buf.set(bx + 1, new MmlDatum((dx & 0xff00) >> 8));
            bx += 2;
            mml_seg.fm3_ofsadr = bx;
            al -= (byte) (char) ('A' - 1);
            mml_seg.part = al;
            mml_seg.ongen = mml_seg.fm;
            return enmPass2JumpTable.cmloop2;
        }
        /**
        // PCM 拡張パートがあればそれをcompile
         */
//pcm_check:
        if (mml_seg.pcm_ofsadr != 0) { // break rt; // 無し

            int bx = 0; // offset pcm_partchr1
            mml_seg.chipCh = -1;

            //pcmc_loop:;
            for (int cx = 0; cx < 8; cx++) {
                byte al = (byte) (mml_seg.pcm_partchr[bx] & 0xff);
                mml_seg.pcm_partchr[bx] = (char) 0;
                bx++;
                mml_seg.chipCh++;
                if (al != 0) {
//                    break pcmc_main;
//pcmc_main: // ↑
                    bx = mml_seg.pcm_ofsadr;
                    int dx = work.di;
                    dx -= 0; // offset m_buf
                    m_seg.m_buf.set(bx + 0, new MmlDatum(dx & 0x00ff));
                    m_seg.m_buf.set(bx + 1, new MmlDatum((dx & 0xff00) >> 8));
                    bx += 2;
                    mml_seg.pcm_ofsadr = bx;
                    al -= (byte) (char) ('A' - 1);
                    mml_seg.part = al;
                    mml_seg.ongen = mml_seg.pcm_ex;
                    return enmPass2JumpTable.cmloop2;
                }
            }
//            break rt;
        }
        /**
        // R part Compile(efc.exeはしない)
         */
//rt:
        /**
        // Ｒパートのスタートアドレスをセット
         */

        int bx = 0; // offset m_buf
        bx += 2 * mml_seg.max_part;
        mml_seg.part = mml_seg.rhythm;
        mml_seg.ongen = mml_seg.pcm;
        int dx = work.di;
        dx -= 0; // offset m_buf
        m_seg.m_buf.set(bx + 0, new MmlDatum(dx & 0x00ff));
        m_seg.m_buf.set(bx + 1, new MmlDatum((dx & 0xff00) >> 8));

        /**
        // リズムデータスタートアドレスを計算してｂｘへ
         */
        work.bx = (byte) (mml_seg.kpart_maxprg & 0xff);
        work.bx += work.bx;
        work.bx += work.di;

        /**
        // Ｒパートコンパイル開始
         */
        mml_seg.pass = 2;
        work.si = 0; // offset mml_buf
        cm_init();
        byte al = (byte) (mml_seg.deflng_k & 0xff);
        mml_seg.deflng = al; // l 値だけKパートから引用

        return enmPass2JumpTable.rtloop;
    }

    /**
    // データスタートアドレスをセット
     */
    private enmPass2JumpTable rtloop() {
        if (mml_seg.kpart_maxprg != 0) {
            work.dx = work.bx;
            work.dx -= 0; // offset m_buf
            m_seg.m_buf.set(work.di + 0, new MmlDatum(work.dx & 0x00ff));
            m_seg.m_buf.set(work.di + 1, new MmlDatum((work.dx & 0xff00) >> 8));
            work.di += 2;
        }

        return enmPass2JumpTable.rtlp2;
    }

    private enmPass2JumpTable rtlp2() {
        char al_c = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (al_c != 0x1a) { // break rend;

            //rtlp3:;
            do {
                al_c = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (al_c < ' ' + 1) return enmPass2JumpTable.rskip;
                if (al_c == ';') return enmPass2JumpTable.rskip;
                if (al_c == '`') {
                    mml_seg.skip_flag ^= 2;
                    continue;
                }

                if ((mml_seg.skip_flag & 2) == 0) {
                    if (al_c == '!') return enmPass2JumpTable.hsset;
                }
                //rt_no_skip2:;
                if (mml_seg.kpart_maxprg == 0) return enmPass2JumpTable.rskip;
                if (al_c == 'R') break;
            } while (true);

            int aa = work.bx;
            work.bx = work.di;
            work.di = aa;

            //push bx
            int bx_p = work.bx;
            mml_seg.hsflag = 1;
            mml_seg.prsok = 0;
            enmPass2JumpTable ret = one_line_compile();
            //if (ret != enmPass2JumpTable.forceReturn)
            {
                do {

                    if (ret == enmPass2JumpTable.exit) break;
                    if (ret == enmPass2JumpTable.hscom_exit) {
                        if (mml_seg.hsflag > 1) {
                            ret = hscom_exit();
                        } else {
                            mml_seg.hsflag--;
                            break;
                        }
                    }

                    ret = Jumper(ret);

                } while (ret != enmPass2JumpTable.forceReturn);

                //throw new Exception("リターンできていない!"); // KUMA:ここでは戻ってくる必要あり
            }
            work.bx = bx_p;
            //pop bx

            m_seg.m_buf.set(work.di, new MmlDatum(0xff));
            work.di++;

            aa = work.bx;
            work.bx = work.di;
            work.di = aa;

            mml_seg.kpart_maxprg--;
            return enmPass2JumpTable.rtloop;
        }
//rend:
        work.di = work.bx;
        //rend2:;
        if (mml_seg.kpart_maxprg == 0) return enmPass2JumpTable.rem_set;

        work.dx = 29;
        work.si = 0;
        error(0, 29, work.si);

        throw new PmdException(); // ダミー：ここに来ることは無い
    }

    private enmPass2JumpTable rskip() {
        line_skip();
        return enmPass2JumpTable.rtlp2;
    }
//#endif

    /**
    // Part Init.
     */
    private void cm_init() {
        mml_seg.maxprg = 0;
        mml_seg.volss = 0;
        mml_seg.volss2 = 0;
        mml_seg.octss = 0;
        mml_seg.skip_flag = 0;
        mml_seg.tie_flag = 0;
        mml_seg.porta_flag = 0;
        mml_seg.ss_speed = 0;
        mml_seg.ss_depth = 0;
        mml_seg.ss_length = 0;
        mml_seg.ge_delay = 0;
        mml_seg.ge_depth = 0;
        mml_seg.ge_depth2 = 0;
        mml_seg.pitch = 0;
        mml_seg.master_detune = 0;
        mml_seg.detune = 0;
        mml_seg.def_a = 0;
        mml_seg.def_b = 0;
        mml_seg.def_c = 0;
        mml_seg.def_d = 0;
        mml_seg.def_e = 0;
        mml_seg.def_f = 0;
        mml_seg.def_g = 0;
        mml_seg.prsok = 0;
        mml_seg.alldet = 0x8000;
        mml_seg.octave = 3;
        mml_seg.deflng = mml_seg.zenlen / 4;
    }

    /**
    // Remark文箇所の設定
     */
    private enmPass2JumpTable rem_set() {
//#if !efc
        mml_seg.part = 0;
        if (mml_seg.towns_flg != 1) { // break tclc_towns_chk;

            byte al = (byte) (mml_seg.lc_flag & 0xff);
            lc.lc_proc(al);

            work.si = 0; // offset max_all;
            m_seg.m_buf.set(work.di++, new MmlDatum(lc.max_all & 0xff)); // TC/LC書き込み(4.8a～)
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_all & 0xff00) >> 8));
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_all & 0xff0000) >> 16));
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_all & 0xff000000) >> 24));

            m_seg.m_buf.set(work.di++, new MmlDatum(lc.max_loop & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_loop & 0xff00) >> 8));
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_loop & 0xff0000) >> 16));
            m_seg.m_buf.set(work.di++, new MmlDatum((lc.max_loop & 0xff000000) >> 24));
        }
//tclc_towns_chk:
        work.bp = work.di;
        work.di += 2;
        byte al = (byte) vers;
        if (mml_seg.towns_flg == 1) { // break vers_towns_chk;
            al = 0x46; // Townsは4.6f @@@@
        }
//vers_towns_chk:
        m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff));
        al = (byte) 0xfe; // -2;
        m_seg.m_buf.set(work.di++, new MmlDatum(al & 0xff)); // Remarks Check Code(0feh)
//#endif
        return enmPass2JumpTable.vdat_set;
    }

//#if!hyouka

    //1190-1215
    /**
    // V2.6以降用／音色データのセット
     */
    private enmPass2JumpTable vdat_set() {
        if ((mml_seg.prg_flg & 1) == 0) return enmPass2JumpTable.memo_write;

        logger.log(Level.DEBUG, String.format("vdat_setAddress:%d", work.di));
        vdat_setAddress = work.di;

        work.si = 0; // offset m_buf
        work.si += 2 * (mml_seg.max_part + 1); // KUMA:? -> v48sで理解w
        int dx = work.di;
        dx -= 0; // offset m_buf
        m_seg.m_buf.set(work.si, new MmlDatum(dx & 0xff));
        m_seg.m_buf.set(work.si + 1, new MmlDatum((dx & 0xff00) >> 8));
        work.bx = 0; // MmlSeg.prg_num;
        work.si = 0; // offset voice_buf
//#if split
        work.si++;
//#endif

        work.al = 0;
        int cx = 256;

        if (mml_seg.opl_flg != 1) return enmPass2JumpTable.nd_s_loop;

        /**
        // OPL用
         */
//nd_s_opl_loop:
        do {
            if (mml_seg.prg_num[work.bx] != 0) { // break nd_s_opl_00;

                m_seg.m_buf.set(work.di, new MmlDatum(work.al & 0xff));
                work.di++;

                for (int rep = 0; rep < 9; rep++) { // １音色 9bytes
                    m_seg.m_buf.set(work.di, new MmlDatum(voice_seg.voice_buf[work.si] & 0xff));
                    work.di++;
                    work.si++;
                }

                work.si += 16 - 9;
//                break nd_s_opl_01;
            } else {
//nd_s_opl_00:
                work.si += 16;
            }
//nd_s_opl_01:
            work.al++;
            work.bx++;

            cx--;
        } while (cx > 0);

        return enmPass2JumpTable.nd_s_exit;
    }

    /**
    // OPN用
     */
    private enmPass2JumpTable nd_s_loop() {
        int cx = 256;
        do {
            if (mml_seg.prg_num[work.bx] != 0) { // break nd_s_00;

                m_seg.m_buf.set(work.di, new MmlDatum(work.al & 0xff));
                work.di++;

                for (int rep = 0; rep < 25; rep++) // １音色 25bytes
                {
                    m_seg.m_buf.set(work.di, new MmlDatum(voice_seg.voice_buf[work.si] & 0xff));
                    work.di++;
                    work.si++;
                }

                work.si += 32 - 25;
//                break nd_s_01;
            } else {
//nd_s_00:
                work.si += 32;
            }
//nd_s_01:
            work.al++;
            work.bx++;

            cx--;
        } while (cx > 0);
        return enmPass2JumpTable.nd_s_exit;
    }

    private enmPass2JumpTable nd_s_exit() {
        int ax = 0xff00;
        m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8)); // 音色終了マーク

        return enmPass2JumpTable.memo_write;
    }
//#endif

    /**
    // その他メモ系文字列の書込み
     */
    private enmPass2JumpTable memo_write() {
        logger.log(Level.DEBUG, "memo_writeAddress:%d".formatted(work.di));
        memo_writeAddress = work.di;

//#if !efc
        work.bx = 0; // MmlSeg.ppzfile_adr;
        int cx = 3; // #PPZFile / #PPSFile / #PCMFile

        if (mml_seg.towns_flg == 1) {
            work.bx = mml_seg.ppsfile_adr;
            cx--; // Townsは4.6f @@@@
        }

        //ppz_towns_chk:;
        //memow_loop0:;
        int ax;
        String ret;
        byte[] bret;
        do {
            ax = work.di;
            ax -= 0; // offset m_buf
            switch (cx) {
                case 3:
                    work.si = mml_seg.ppzfile_adr; // si<文字列先頭番地
                    mml_seg.ppzfile_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
                    break;
                case 2:
                    work.si = mml_seg.ppsfile_adr; // si<文字列先頭番地
                    mml_seg.ppsfile_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
                    break;
                case 1:
                    work.si = mml_seg.pcmfile_adr; // si<文字列先頭番地
                    mml_seg.pcmfile_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
                    break;
            }

            if (work.si == 0) { // break memow_trans0;
                work.al = 0;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
//                break memow_exit0;
            } else {
//memow_trans0:
                ret = set_Strings2(); // 小文字＞大文字変換付き
                bret = compiler.enc.getSjisArrayFromString(ret);
                for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum(0));
            }
//memow_exit0:
            work.bx += 2;
            cx--;
        } while (cx > 0); // loop memow_loop0

        // #Title
        work.si = mml_seg.title_adr; // si<文字列先頭番地
        ax = work.di;
        ax -= 0; // offset m_buf
        mml_seg.title_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
        if (work.si == 0) {
            work.al = 0;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
//            break memow_exit;
        } else {
            //memow_trans:
            ret = set_Strings();
            bret = compiler.enc.getSjisArrayFromString(ret);
            for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum(0));
        }
//memow_exit:
        work.bx += 2;

        // #Composer
        work.si = mml_seg.composer_adr; // si<文字列先頭番地
        ax = work.di;
        ax -= 0; // offset m_buf
        mml_seg.composer_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
        if (work.si == 0) {
            if (mml_seg.composer_seg == null) {
                work.al = 0;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            } else {
                bret = compiler.enc.getSjisArrayFromString(mml_seg.composer_seg);
                for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum(0));
            }
        } else {
            ret = set_Strings();
            bret = compiler.enc.getSjisArrayFromString(ret);
            for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum(0));
        }

        //memow_exit_composer:;
        work.bx += 2;
        // #Arranger
        work.si = mml_seg.arranger_adr; // si<文字列先頭番地
        ax = work.di;
        ax -= 0; // offset m_buf
        mml_seg.arranger_adr = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく
        if (work.si == 0) {
            if (mml_seg.arranger_seg == null) {
                work.al = 0;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            } else {
                bret = compiler.enc.getSjisArrayFromString(mml_seg.arranger_seg);
                for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum(0));
            }
        } else {
            ret = set_Strings();
            bret = compiler.enc.getSjisArrayFromString(ret);
            for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum(0));
        }

        //memow_exit_arranger:;
        work.bx += 2;
        //memow_loop2:;
        // mov si,[bx]; si<文字列先頭番地
        int memoInd = 0;
        while (mml_seg.memo_adr[memoInd] != 0) {
            work.si = mml_seg.memo_adr[memoInd]; // si<文字列先頭番地
            ax = work.di;
            ax -= 0; // offset m_buf
            mml_seg.memo_adr[memoInd] = ax; // [bx] に替わりに転送先のアドレス(ofs)を入れておく

            ret = set_Strings();
            bret = compiler.enc.getSjisArrayFromString(ret);
            for (byte b : bret) m_seg.m_buf.set(work.di++, new MmlDatum(b & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum(0));

            work.bx += 2;
            memoInd++;
        }
        //memow_allexit:;
        ax = work.di;
        ax -= 0; // offset m_buf

        m_seg.m_buf.set(work.bp + 0, new MmlDatum(ax & 0xff)); // KUMA: tagのアドレステーブルへのアドレスをセット
        m_seg.m_buf.set(work.bp + 1, new MmlDatum((ax & 0xff00) >> 8));

        work.si = mml_seg.ppzfile_adr; // offset ppzfile_adr
        if (mml_seg.towns_flg == 1) {
            work.si = mml_seg.ppsfile_adr; //    mov si, offset ppsfile_adr  ;Townsは4.6f @@@@
        }
        //memo_towns_chk:
        //memoofsset_loop:

        if (mml_seg.towns_flg == 0) {
            m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.ppzfile_adr & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.ppzfile_adr & 0xff00) >> 8));
        }
        m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.ppsfile_adr & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.ppsfile_adr & 0xff00) >> 8));
        m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.pcmfile_adr & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.pcmfile_adr & 0xff00) >> 8));
        m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.title_adr & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.title_adr & 0xff00) >> 8));
        m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.composer_adr & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.composer_adr & 0xff00) >> 8));
        m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.arranger_adr & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.arranger_adr & 0xff00) >> 8));
        for (int i = 0; i < mml_seg.memo_adr.length; i++) {
            m_seg.m_buf.set(work.di++, new MmlDatum(mml_seg.memo_adr[i] & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum((mml_seg.memo_adr[i] & 0xff00) >> 8));
            if (mml_seg.memo_adr[i] == 0) break;
        }

//#endif

        /**
        // 容量オーバーcheck
         */
        if (m_seg.mbuf_end != 0x7f) {
            error((char) 0, 19, 0); // 容量オーバー
        }

        return enmPass2JumpTable.exit;
    }

//#if!hyouka

    /**
    // .ffの書き込み
     */
    private byte[] write_ff() {
        List<Byte> vBuf = null;

        if ((mml_seg.prg_flg & 2) == 0) return null; //KUMA: .Mファイルの出力は戻り先で。

        if (mml_seg.ff_flg == 0) {
            //not_ff: //ここに移動
            print_mes(mml_seg.warning_mes
                    + mml_seg.not_ff_mes);
            return null;
        }

        int cx = 8 * 1024;
        if (mml_seg.opl_flg == 1) cx = 4 * 1024;

        //wf_go:;
        try {
            //int ax = 0; // offset v_filename
            work.dx = 0; // offset voice_buf
            vBuf = new ArrayList<>();
            do {
                vBuf.add(voice_seg.voice_buf[work.dx++]);
                cx--;
            } while (cx > 0);
        } catch (Exception e) {
            error((char) 0, 5, 0);
        }

        return ByteUtil.toByteArray(vBuf); //KUMA: .Mファイルの出力は戻り先で。
    }

    /**
    // Disk Write
     */
    //write_disk:
    // KUMA: ここでは不要
    // KUMA: ファイルを出力するときは
    // KUMA:   save_flgをチェック。0の場合は出力不要
    // KUMA:   ファイル名は m_filename
    // KUMA:   ファイルサイズは 1 + MSeg.m_buf.length
    // KUMA:   出力失敗時は error((char)0 , 4 , 0)
//#else
    //
    // 評価版／音色データエリアにコンパイル後の音色データを転送
    //
    // KUMA: 不要
//#endif

    /**
    // Compile 終了
     */
    private void compile_fin() {
        print_mes(mml_seg.finmes);
        //KUMA: コンパイラからPMDを呼び出し再生する機能は省略
    }

    private void line_skip() {
        do {
            work.si++;
        } while (work.si - 1 < mml_seg.mml_buf.length()
                && ((work.si - 1 < mml_seg.mml_buf.length()) ? mml_seg.mml_buf.charAt(work.si - 1) : 0x1a) != 0xa);

    }

    /**
    // FF File read
    //  in.ds:si Strings
     */
    private void read_fffile() {
        mml_seg.ff_flg = 1;
        byte ah = 0;
        //int di = 0; // offset v_filename
        voice_seg.v_filename = "";

//g_vfn_loop:
        do {
            char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
            if (al == ' ') break;
            if (al == 13) break;
            if (al == '\\') ah = 0;
//g_vfn_notyen:
            if (al == '.') ah = 1;
//g_vfn_store:
            voice_seg.v_filename += al;
        } while (true);

//g_vfn_next:
        if (ah == 0) {
            voice_seg.v_filename += ".FF";
            if (mml_seg.opl_flg == 1) {
                voice_seg.v_filename += "L";
            }
        }

        //vfn_ofs_notset:;

        /**
        //  .ffファイルの読み込み
         */

        try {
            voice_seg.voice_buf = compiler.readFile(voice_seg.v_filename);
        } catch (Exception e) {
            voice_seg.voice_buf = null;
        }

        if (voice_seg.voice_buf == null) {
            print_mes(mml_seg.warning_mes + String.format(rb.getString("E0200"), voice_seg.v_filename)); // MmlSeg.ff_readerr_mes);
        } else {
//#if !hyouka
            mml_seg.prg_flg |= 1;
//#endif
        }
        voiceTrancer(voice_seg.voice_buf);
    }

    /**
    // オプション文字列読み取り
     */
    private boolean get_option(String val) {
        for (int[] col = new int[1]; col[0] < val.length(); col[0]++) {
            char a = val.charAt(col[0]);
            if (a == ' ') continue;
            if (a != '/' && a != '-') {
                return true; // -,/で始まらないオプションはエラー
            }
            if (col[0] + 1 == val.length()) return true; // -,/のみのオプションは無いのでエラー

            col[0]++;
            String sw = String.valueOf(Character.toUpperCase(val.charAt(col[0])));
            switch (sw) {
                case "V":
                    prgflg_set(val, /* ref */ col);
                    break;
                case "P":
                    playflg_set(val, /* ref */ col);
                    break;
                case "S":
                    saveflg_reset(val, /* ref */ col);
                    break;
                case "M":
                    x68flg_set(val, /* ref */ col);
                    break;
                case "T":
                    townsflg_set(val, /* ref */ col);
                    break;
                case "N":
                    x68flg_reset(val, /* ref */ col);
                    break;
                case "L":
                    oplflg_set(val, /* ref */ col);
                    break;
                case "O":
                    memoflg_reset(val, /* ref */ col);
                    break;
                case "A":
                    pcmflg_reset(val, /* ref */ col);
                    break;
                case "C":
                    lcflg_set(val, /* ref */ col);
                    break;
                default:
                    return true; // 知らないオプションはエラー
            }
        }

        return false;
    }

    /**
    // /v,/vw option
     */
    private void prgflg_set(String val, /* ref */ int[] col) {
        String c = col[0] + 1 == val.length() ? "" : String.valueOf(Character.toUpperCase(val.charAt(col[0] + 1)));

        if (c.equals("W")) {
            col[0]++;
//#if !hyouka
            mml_seg.prg_flg |= 2;
//#endif
        } else {
//#if !hyouka
            mml_seg.prg_flg |= 1;
//#endif
        }
    }

    /**
    // /p option
     */
    private void playflg_set(String val, /* ref */ int[] col) {
//#if !hyouka
        mml_seg.play_flg = 1;
//#endif
    }

    /**
    // /s option
     */
    private void saveflg_reset(String val, /* ref */ int[] col) {
//#if !hyouka
        mml_seg.save_flg = 0;
        mml_seg.play_flg = 1;
//#endif
    }

    /**
    // /m option
     */
    private void x68flg_set(String val, /* ref */ int[] col) {
        mml_seg.towns_flg = 0;
        mml_seg.x68_flg = 1;
        mml_seg.opl_flg = 0;
        mml_seg.dt2_flg = 1;
    }

    /**
    // /n option
     */
    private void x68flg_reset(String val, /* ref */ int[] col) {
        mml_seg.towns_flg = 0;
        mml_seg.x68_flg = 0;
        mml_seg.opl_flg = 0;
        mml_seg.dt2_flg = 0;
    }

    /**
    // /t option
     */
    private void townsflg_set(String val, /* ref */ int[] col) {
        mml_seg.towns_flg = 1;
        mml_seg.x68_flg = 0;
        mml_seg.opl_flg = 0;
        mml_seg.dt2_flg = 0;
    }

    /**
    // /l option
     */
    private void oplflg_set(String val, /* ref */ int[] col) {
        mml_seg.opl_flg = 1;
    }

    /**
    // /o option
     */
    private void memoflg_reset(String val, /* ref */ int[] col) {
        mml_seg.memo_flg = 0;
    }

    /**
    // /a option
     */
    private void pcmflg_reset(String val, /* ref */ int[] col) {
        mml_seg.pcm_flg = 0;

    }

    /**
    // /c option
     */
    private void lcflg_set(String val, /* ref */ int[] col) {
        mml_seg.lc_flag = 1;
    }

    /**
    // マクロコマンド
     */
    private enmPass2JumpTable macro_set() {
        int bx = work.si; //bxに現在のsiを保存
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換(1文字目)
        String ah = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si))); // 小文字＞大文字変換(2文字目)

        //siを先にパラメータの位置まで進める
        if (move_next_param())
            error('#', 6, work.si); // KUMA:パラメータがみつからなかった

        switch (al) {
//#if !efc
            case "P":
                pcmfile_set(ah, bx);
                break;
            case "T":
                title_set(ah, bx);
                break;
            case "C":
                composer_set();
                break;
            case "A":
                arranger_set(ah);
                break;
            case "M":
                memo_set();
                break;
            case "Z":
                zenlen_set();
                break;
            case "L":
                LFOExtend_set(ah);
                break;
            case "E":
                EnvExtend_set();
                break;
            case "V":
                VolDown_set();
                break;
            case "J":
                JumpFlag_set();
                break;
//#endif
            case "F":
                FM3Extend_set(ah);
                break;
            case "D":
                dt2flag_set(ah);
                break;
            case "O":
                octrev_set(ah);
                break;
            case "B":
                bend_set();
                break;
            case "I":
                include_set();
                break;
            default:
                //ps_error:
                error('#', 7, work.si);
                break;
        }

        //macro_normal_ret:
        bx = mml_seg.linehead;
        mml_seg.mml_buf = (bx != 0 ? mml_seg.mml_buf.substring(0, bx) : "")
                + ";"
                + (bx + 1 < mml_seg.mml_buf.length() ? mml_seg.mml_buf.substring(bx + 1) : "")
        ; // "#"を";"に変換

        return enmPass2JumpTable.p1c_fin;
    }

//#if!efc

    /**
    // #PCMFile
     */
    private void pcmfile_set(String ah, int bx) {
        if (ah.equals("P")) {
            ppsfile_set(bx);
            return;
        } else if (!ah.equals("C")) {
            error('#', 7, work.si);
        }

        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(bx + 2))); // 小文字＞大文字変換(3文字目)
        if (!al.equals("M")) {
            error('#', 7, work.si);
        }

        al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(bx + 3))); // 小文字＞大文字変換(4文字目)
        if (al.equals("V")) {
            pcmvolume_set();
            return;
        } else if (al.equals("E")) {
            pcmextend_set(bx);
            return;
        } else if (!al.equals("F")) {
            error('#', 7, work.si);
        }

        //ppcfile_set:
        mml_seg.pcmfile_adr = work.si;
    }

    /**
    // #PCMVolume Extend/Normal
     */
    private void pcmvolume_set() {
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si))); // 小文字＞大文字変換

        if (al.equals("N")) {
            mml_seg.pcm_vol_ext = 0;
            return;
        } else if (al.equals("E")) {
            mml_seg.pcm_vol_ext = 1;
            return;
        }

        error('#', 7, work.si); // ps_error
    }

    /**
    // #PCMExtend
     */
    private void pcmextend_set(int bx) {
//#if !efc
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(bx + 3))); // 小文字＞大文字変換(4文字目)
        if (al.equals("F")) {
            ppzfile_set();
            return;
        }

        if (mml_seg.mml_buf.charAt(work.si) < ' ') {
            error('#', 6, work.si); // ps_error
        }

        char a = mml_seg.mml_buf.charAt(work.si++);
        if (partcheck(a)) {
            error('#', 6, work.si); // ps_error
        }

        int di = 0; // offset pcm_partchr1
        bx = 0; // offset _pcm_partchr1;in cs
        mml_seg.pcm_partchr[di] = a;
        lc._pcm_partchr[bx] = a;
        di++;
        bx++;

        for (int cx = 0; cx < 7; cx++) // 1+7 = 8 parts
        {
            a = mml_seg.mml_buf.charAt(work.si++);
            if (partcheck(a)) {
                work.si--;
                return;
            }
            mml_seg.pcm_partchr[di] = a;
            lc._pcm_partchr[bx] = a;
            di++;
            bx++;
        }

//#else
//        error((int) '#', 6, Work.si); // ps_error
//#endif
    }

    /**
    // #PPZFile
     */
    private void ppzfile_set() {
        mml_seg.ppzfile_adr = work.si;
    }

    /**
    // #PPSFile
     */
    private void ppsfile_set(int bx) {
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(bx + 2))); // 小文字＞大文字変換(3文字目)

        if (al.equals("C")) {
            mml_seg.pcmfile_adr = work.si; // #PPC
            return;
        } else if (al.equals("Z")) {
            pcmextend_set(bx); // #PPZ
            return;
        } else if (!al.equals("S")) {
            error('#', 6, work.si); // ps_error
        }

        mml_seg.ppsfile_adr = work.si;
    }

    /**
    // #Title
     */
    private void title_set(String ah, int bx) {
        if (ah.equals("E")) { // #TEmpo
            tempo_set();
            return;
        }

        if (ah.equals("R")) { // #TRanspose
            transpose_set();
            return;
        }

        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(bx + 2))); // 小文字＞大文字変換(3文字目)

        if (al.equals("M")) { // #TIMer
            tempo_set2();
            return;
        }

        mml_seg.title_adr = work.si;
    }

    /**
    // #Composer
     */
    private void composer_set() {
        mml_seg.composer_adr = work.si;
        //   mov[composer_seg],0
    }

    private String GetString(String buf, int index) {
        String ret = buf.substring(index);
        if (ret.indexOf((char) 0x1a) >= 0) return ret = ret.substring(0, ret.indexOf((char) 0x1a));
        if (ret.indexOf((char) 0x0a) >= 0) return ret = ret.substring(0, ret.indexOf((char) 0x0a));
        if (ret.indexOf((char) 0x0d) >= 0) return ret = ret.substring(0, ret.indexOf((char) 0x0d));
        return ret;
    }

    /**
    // #Arranger
     */
    private void arranger_set(String ah) {
        if (ah.equals("D")) {
            adpcm_set(ah);
            return;
        }

        mml_seg.arranger_adr = work.si;
    }

    /**
    // #ADPCM on/off
     */
    private void adpcm_set(String ah) {
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換

        if (!al.equals("O")) {
            error('#', 7, work.si);
        }

        al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換
        ah = String.valueOf((char) 1);
        if (al.equals("N")) {
            mml_seg.adpcm_flag = 1;
            return;
        }

        ah = String.valueOf((char) 0);
        if (!al.equals("F")) {
            error('#', 7, work.si);
        }

        mml_seg.adpcm_flag = 1; // kuma:OFFでもflagたてる？
    }

    /**
    // #Memo
     */
    private void memo_set() {
        int bx = -1; // offset memo_adr-2
        do {
            bx++;
        } while (mml_seg.memo_adr[bx] != 0);

        mml_seg.memo_adr[bx] = work.si;
    }

    /**
    // #Transpose
     */
    private void transpose_set() {
        byte[] dl = new byte[1]; int[] dummy = new int[1];
        getnum(/* out */ dummy, /* out */ dl); // 230923 FIXED
        mml_seg.transpose = dl[0];//

        //int bx = 0;
        //byte al = 0;
        //if (lngset(/* out */ bx, /* out */ al)) {
        //    error('#', 7, Work.si);
        //}

        //MmlSeg.transpose = al;
    }

    /**
    // #Detune Normal/Extend
     */
    private void detune_select() {
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換
        if (al.equals("N")) {
            mml_seg.ext_detune = 0;
            return;
        } else if (al.equals("E")) {
            mml_seg.ext_detune = 1;
            return;
        }

        error('#', 7, work.si);
    }

    /**
    // #LFOSpeed Normal/Extend
     */
    private void LFOExtend_set(String ah) {
        if (ah.equals("O")) {
            loopdef_set();
            return;
        }

        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換
        if (al.equals("N")) {
            mml_seg.ext_lfo = 0;
            return;
        } else if (al.equals("E")) {
            mml_seg.ext_lfo = 1;
            return;
        }

        error('#', 7, work.si);
    }

    /**
    // #LoopDefault n
     */
    private void loopdef_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al)) {
            error('#', 7, work.si);
        }

        mml_seg.loop_def = al[0];
    }

    /**
    // #EnvSpeed Normal/Extend
     */
    private void EnvExtend_set() {
        String al = String.valueOf(Character.toUpperCase(mml_seg.mml_buf.charAt(work.si++))); // 小文字＞大文字変換
        if (al.equals("N")) {
            mml_seg.ext_env = 0;
            return;
        } else if (al.equals("E")) {
            mml_seg.ext_env = 1;
            return;
        }

        error('#', 7, work.si);
    }

    /**
    // #VolumeDown
     */
    private void VolDown_set() {
        do {
            int bh = 0; // FSPR select bit clear
            byte bl = 0; // 絶対/相対flag clear
            String al;
            byte al_b;

//voldown_loop:
            do {
                al_b = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (al_b < ((byte) '9' + 1)) break;
                al = String.valueOf(Character.toUpperCase((char) al_b)); // 小文字＞大文字変換

                if (al.equals("F")) bh |= 0x01;
                else if (al.equals("S")) bh |= 0x02;
                else if (al.equals("P")) bh |= 0x04;
                else if (al.equals("R")) bh |= 0x08;
                else if (al.equals("Z")) bh |= 0x10;
                else error('#', 1, work.si);
            } while (true);

//vd_noppz:
//vd_numget:
            work.si--;
            al = String.valueOf((char) al_b);
            if (!al.equals("+") && !al.equals("-"))
                bl++; // 絶対指定

//vd_numget2:
            if (bh == 0) // KUMA:指定なしの場合はエラー
                error('#', 1, work.si);

            int[] bx_dmy = new int[1]; byte[] dl = new byte[1];
            getnum(/* out */ bx_dmy, /* out */ dl);

            if ((bh & 0x01) != 0) {
                mml_seg.fm_voldown = dl[0];
                mml_seg.fm_voldown_flag = bl;
            }

            if ((bh & 0x02) != 0) {
                mml_seg.ssg_voldown = dl[0];
                mml_seg.ssg_voldown_flag = bl;
            }

            if ((bh & 0x04) != 0) {
                mml_seg.pcm_voldown = dl[0];
                mml_seg.pcm_voldown_flag = bl;
            }

            if ((bh & 0x08) != 0) {
                mml_seg.rhythm_voldown = dl[0];
                mml_seg.rhythm_voldown_flag = bl;
            }

            if ((bh & 0x10) != 0) {
                mml_seg.ppz_voldown = dl[0];
                mml_seg.ppz_voldown_flag = bl;
            }

            al_b = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (al_b != ',') {
                return;
            }

            work.si++;
        } while (true);
    }

    /**
    // #Jump
     */
    private void JumpFlag_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al))
        {
            error('#', 6, work.si);
        }

        mml_seg.jump_flag = bx[0];
    }

    /**
    // #Tempo
     */
    private void tempo_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */  bx, /* out */  al))
        {
            error('#', 6, work.si);
        }

        if (tempo_old_flag != 0) {
            byte[] dl = new byte[1];
            timerb_get(al[0], /* out */  dl);
            mml_seg.timerb = dl[0];
        } else {
            mml_seg.tempo = al[0];
        }
    }

    /**
    // #Timer
     */
    private void tempo_set2() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al))
        {
            error('#', 6, work.si);
        }

        mml_seg.timerb = al[0];
    }

    /**
    // #Zenlength
     */
    private void zenlen_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al))
        {
            error('#', 6, work.si);
        }

        mml_seg.zenlen = al[0];
        mml_seg.deflng = al[0] / 4;
    }

//#endif

    /**
    // #FM3Extend
     */
    private void FM3Extend_set(String ah) {
        if (ah.equals("I")) {
            file_name_set();
            return;
        }
        if (ah.equals("F")) {
            fffile_set();
            return;
        }
//#if !efc
        if ((work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a) < ' ') {
            error('#', 6, work.si);
        }

        char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
        if (partcheck(al)) {
            //ps_error
            error('#', 7, work.si);
        }

        mml_seg.fm3_partchr1 = al;
        lc._fm3_partchr[0] = al;

        al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
        if (partcheck(al)) {
            work.si--;
            return;
        }

        mml_seg.fm3_partchr2 = al;
        lc._fm3_partchr[1] = al;

        al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
        if (partcheck(al)) {
            work.si--;
            return;
        }

        mml_seg.fm3_partchr3 = al;
        lc._fm3_partchr[2] = al;

        return;

//#else
        //ps_error
//        error('#', 7, work.si);
//#endif
    }

    /**
    // #Filename
     */
    private void file_name_set() {
//#if !hyouka

        int di = m_seg.file_ext_adr;
        char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;

        if (al != '.') {
            work.si--;
            m_seg.m_filename = "";
        } else {
            m_seg.m_filename = m_seg.m_filename.substring(0, di);
        }

        //file_name_set_main:;
        do {
            m_seg.m_filename += work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;

            if ((work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a) == ';') {
                break;
            }

        } while ((work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a) >= '!');

        //file_name_set_exit:;
        //al = (char)0;
        //    stosb
//#endif
    }

    /**
    // #FFFile
     */
    private void fffile_set() {
        read_fffile();

    }

    /**
    // #DT2flag on/off
     */
    private void dt2flag_set(String ah) {
//#if !efc

        if (ah.equals("E")) {
            detune_select();
            return;
        }
//#endif

        String al = (work.si < mml_seg.mml_buf.length() ? String.valueOf(mml_seg.mml_buf.charAt(work.si++)) : String.valueOf(Character.toUpperCase((char) 0x1a))); // 小文字＞大文字変換
        if (!al.equals("O")) {
            //ps_error
            error('#', 7, work.si);
        }

        al = (work.si < mml_seg.mml_buf.length() ? String.valueOf(mml_seg.mml_buf.charAt(work.si++)) : String.valueOf(Character.toUpperCase((char) 0x1a))); //小文字＞大文字変換
        if (al.equals("N")) {
            mml_seg.dt2_flg = 1;
            return;
        }
        if (!al.equals("F")) {
            //ps_error
            error('#', 7, work.si);
        }

        mml_seg.dt2_flg = 0;
        return;
//dt2flag_norm:
    }

    /**
    // #octave rev/norm
     */
    private void octrev_set(String ah) {
        if (ah.equals("P")) {
            option_set();
            return;
        }

        String al = (work.si < mml_seg.mml_buf.length() ? String.valueOf(mml_seg.mml_buf.charAt(work.si++)) : String.valueOf(Character.toUpperCase((char) 0x1a))); //小文字＞大文字変換
        if (al.equals("R")) {
            comtbl[ou00] = new Tuple<>("<", this::octup);
            comtbl[od00] = new Tuple<>(">", this::octdown);
            return;
        }
        if (!al.equals("N")) {
            //ps_error
            error('#', 7, work.si);
        }

        comtbl[ou00] = new Tuple<>(">", this::octup);
        comtbl[od00] = new Tuple<>("<", this::octdown);
    }

    /**
    // #Option
     */
    private void option_set() {
        String val = "";
        char v = (char) 0;
        do {
            v = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            val += v;
        } while (v >= 0x20);
        if (val.length() > 1) val = val.substring(0, val.length() - 1);

        get_option(val.trim().toUpperCase());
    }

    /**
    // #Bendrange
     */
    private void bend_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al))
        {
            error('#', 6, work.si);
        }

        mml_seg.bend = al[0];
    }

    /**
    // #Include
     */
    private void include_set() {
        // 
        // ファイル名の取り込み
        // 
        //push es
        //push di
        //mov ax, ds
        //mov es, ax
        //assume es:MmlSeg

        mml_seg.mml_filename2 = set_Strings();

        int p_si = work.si; // SI= CR位置 を保存
        work.si += 2; // SI= 次の行の先頭位置(に読み込む予定)

        mml_seg.includeFileHistory.add(mml_seg.mml_filename2);

        // 
        // 現在のMML残りをMMLバッファ末端に移動
        // I---------------I---------------I---------------I
        // mml_buf SI[mml_endadr]    mmlbuf_end
        //   -------CX-------SI DI  にしてstd/movsw
        // 

        String back = mml_seg.mml_buf.substring(work.si); // 移動する代わりにバックアップ
        mml_seg.mml_buf = mml_seg.mml_buf.substring(0, work.si);
        work.si = p_si;

        // 
        // Include開始check codeをMMLに書く
        // 
        p_si = work.si;
        work.si += 2;
        int di = work.si;
        mml_seg.mml_buf += (char) 1; // IncludeFile開始 CheckCode

        // 
        // FileをOpenしてFile名をMMLに書く
        // 
        // 
        // Fileの読み込み
        // 
        String inc = "";
        try {
            inc = compiler.readFileText(mml_seg.mml_filename2);

            while (inc.length() > 1 && inc.charAt(inc.length() - 1) == 0x1a) {
                inc = inc.substring(0, inc.length() - 1);
            }
        } catch(Exception e)
        {
            work.si = p_si;
            error('#', 3, work.si);
        }

        mml_seg.mml_buf += (char) 0x0a; // LFの書込み = Line_skipに引っ掛かるようにする

        // 
        // File終端のEOFを削ってCR/LFが無ければ書き足す
        // 
        if (inc.length() > 1 && (inc.charAt(inc.length() - 2) != 13 || inc.charAt(inc.length() - 1) != 10)) {
            inc += "" + Mc.cr + "" + Mc.lf;
        }

        // 
        // Include->MainのCheckCodeの書込み
        // 
        inc += "" + (char) 2 + (char) 0xa; // IncludeFile終了 CheckCode

        // 
        // 転送した残りMMLを元に戻す
        // 
        mml_seg.mml_buf += inc + back;
        work.si--;

        if (mml_seg.mml_buf.length() > 61 * 1024) // サイズが大き過ぎる
        {
            work.si = p_si;
            error('#', 18, work.si);
        }
    }

    /**
    // alの文字が使用中のパートかどうかcheck
     */
    private boolean partcheck(char al) {
        if (al < 'L') {
            return true;
        }

        if (al == 'R') {
            return true;
        }

        if (al >= (char) 0x7f) {
            return true;
        }

        return false;
    }

    /**
    // 文字列のセット
    //  crlfが来るまで
     */
    private String set_Strings() {
        String ret = "";

        do {
            char al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == 9 || al == 0x1b) // TAB or ESC
            {
                ret += al;
                continue;
            }

            if (al < ' ') break;

            ret += al;
        } while (true);

        //setstr_exit:;
        work.si--;
        return ret;
    }

    private String set_Strings2() {
        String ret = "";
        //小文字＞大文字変換付き

        do {
            char al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == 9 || al == 0x1b) // TAB or ESC
            {
                ret += al;
                continue;
            }
            if (al < ' ') break;

            ret += String.valueOf(Character.toUpperCase(al));
        } while (true);

        //setstr_exit2:
        work.si--;
        return ret;
    }

    /**
    // 次のパラメータに強制移動する
    //  1.space又はtabをsearch
    //  2.文字列をsearch
     */
    private boolean move_next_param() {
        char al;

        do {
            al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == (char) 9) break;
            if (al == ' ') break;
            if (al < ' ') return true;
        } while (work.si < mml_seg.mml_buf.length());

        if (work.si == mml_seg.mml_buf.length()) {
            return true;
        }

        do {
            al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == (char) 0x1b) // ESC
            {
                work.si--;
                return false;
            }
            if (al == (char) 9) continue;
            if (al == ' ') continue;
            if (al < ' ') return true;
            break;
        } while (work.si < mml_seg.mml_buf.length());

        work.si--;
        return false;
    }

    /**
    // MML 変数の設定
     */
    private enmPass2JumpTable hsset() {
        // push es
        int bx_p = work.bx;
        //    mov ax, HsSeg
        //    mov es, ax
        //    assume es:HsSeg

        int[] bx = new int[1]; byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
        work.bx = bx[0];
        work.al = al[0];
        if (!cy) { // break hsset3;

//hsset2:
            int ax = work.al * 2;
            ax += 0; // offset hsbuf2
            hs_seg.currentBuf = hs_seg.hsbuf2;
            work.bx = ax;
//            break hsset_main;
        } else {
//hsset3:
            int si_pp = work.si;
            cy = search_hs3();
            work.si = si_pp;
            if (cy) { // break hsset_main; // 上書き
                work.bx = 0; // offset hsbuf3
                hs_seg.currentBuf = hs_seg.hsbuf3;

                int cx = 256;
hsset3_loop:
                {
                    do {
                        if ((hs_seg.currentBuf[work.bx] | hs_seg.currentBuf[work.bx + 1]) == 0) break hsset3_loop; // hsset3b;
                        work.bx += hs_seg.hs_length;
                        cx--;
                    } while (cx > 0);
                    error('!', 33, work.si);
                }
//hsset3b:
                int bx_pp = work.bx;
                int di_p = work.di;

                work.di = work.bx + 2; //    lea di,2[bx]
                cx = hs_seg.hs_length - 2;
hsset3b_loop:
                {
                    char alc;
                    do {
                        alc = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                        if (alc < '!') break hsset3b_loop; // hsset3c;
                        hs_seg.currentBuf[work.di++] = (byte) alc;
                        cx--;
                    } while (cx > 0);
//hsset3b_loop2:
                    do {
                        alc = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                    } while (alc >= '!');
                }
//hsset3c:
                work.si--;

                work.di = di_p;
                work.bx = bx_pp;
            }
        }
//hsset_main:
        int si_p = work.si;
hsset_loop:
        {
            char alc;
            do {
                alc = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (alc == 0xd) { // cr
                    break hsset_loop; // hsset_fin;
                }
            } while (alc >= (char) (' ' + 1));
            hs_seg.currentBuf[work.bx + 0] = (byte) (work.si & 0xff);
            hs_seg.currentBuf[work.bx + 1] = (byte) ((work.si & 0xff00) >> 8);
        }
//hsset_fin:
        work.si = si_p;

        work.bx = bx_p;
        //    pop es
        //    assume es:MSeg

        work.al = (byte) (mml_seg.pass & 0xff);
        if (work.al == 0) return enmPass2JumpTable.p1c_fin;
//#if !efc
        work.al--;
        if (work.al == 0) {
            line_skip();
            return enmPass2JumpTable.cloop;
        }
        return enmPass2JumpTable.rskip;
//#else
//        line_skip();
//        return enmPass2JumpTable.cloop;
//#endif
    }

    /**
    // 音色の設定
    //  @ num,alg,fb
    //    ar,dr,sr,rr,sl,tl,ks,ml,dt,[dt2,] ams
    //    ar,dr,sr,rr,sl,tl,ks,ml,dt,[dt2,] ams
    //    ar,dr,sr,rr,sl,tl,ks,ml,dt,[dt2,] ams
    //    ar,dr,sr,rr,sl,tl,ks,ml,dt,[dt2,] ams
    // (OPL)
    //  @ num,alg,fb
    //    ar,dr,rr,sl,tl,ksl,ml,ksr,egt,vib,am
    //    ar,dr,rr,sl,tl,ksl,ml,ksr,egt,vib,am
     */
    private enmPass2JumpTable new_neiro_set() {
        int di_p = work.di;
        int bx_p = work.bx;
        nns();
        work.bx = bx_p;
        work.di = di_p;

        return enmPass2JumpTable.p1c_fin;
    }

    private void nns() {
        if (mml_seg.opl_flg == 1) {
            opl_nns();
            return;
        }

        mml_seg.newprg_num = 0;
        for (int i = 0; i < 6; i++) {
            mml_seg.slot[0][i] = 0;
            mml_seg.slot[1][i] = 0;
            mml_seg.slot[2][i] = 0;
            mml_seg.slot[3][i] = 0;
        }

        get_param();
        mml_seg.newprg_num = work.al;

        logger.log(Level.TRACE, String.format("@ num:%d", work.al));

        get_param();
        work.al &= 7;
        byte ch = work.al;
        get_param();
        work.al &= 7;
        work.al <<= 3;
        work.al |= ch;
        mml_seg.alg_fb = work.al;

        mml_seg.prg_name = "";

        work.di = 0; // offset slot_1
        slot_get(0);
        work.di = 0; // offset slot_2
        slot_get(1);
        work.di = 0; // offset slot_3
        slot_get(2);
        work.di = 0; // offset slot_4
        slot_get(3);

        work.bx = 0; // offset voice_buf
//#if split
        work.bx++;
//#endif

        work.dx = mml_seg.newprg_num * 32;
        work.dx += work.bx;

        //    assume es:VoiceSeg

        work.bx = 0; // offset slot_1
        slot_trans(0);
        work.dx++;

        work.bx = 0; // offset slot_3
        slot_trans(2);
        work.dx++;

        work.bx = 0; // offset slot_2
        slot_trans(1);
        work.dx++;

        work.bx = 0; // offset slot_4
        slot_trans(3);

        work.bx = 21 + work.dx;

        voice_seg.voice_buf[work.bx] = mml_seg.alg_fb;
        work.bx++;

        nns_pname_set();
    }

    private void nns_pname_set() {
        work.bp = 0; // offset prg_name
        int cx = 7;

        //nns_loop:
        for (int i = 0; i < 7; i++) voice_seg.voice_buf[work.bx + i] = 0;
        byte[] bret = compiler.enc.getSjisArrayFromString(mml_seg.prg_name);

        while (work.bp < bret.length && cx > 0) {
            voice_seg.voice_buf[work.bx] = bret[work.bp++];
            work.bx++;
            cx--;
        }
    }

    /**
    // OPL版音色設定
     */
    private void opl_nns() {
        //push es
        int si_p = work.si;
        // assume es:MmlSeg
        //Work.di = 0; // offset oplbuf
        //int cx = 8;
        //int ax = 0;
        for (int i = 0; i < 16; i++) mml_seg.oplbuf[i] = 0;

        get_param();
        mml_seg.newprg_num = work.al;
        work.bx = 0; // offset oplprg_table
        work.di = 0; // offset oplbuf
        int cx = 2 + 11 * 2;
//oplset_loop:
        do {
            int bx_p = work.bx;
            get_param();
            work.bx = bx_p;
            work.al &= mml_seg.oplprg_table[work.bx + 1]; // max
            byte cl = mml_seg.oplprg_table[work.bx + 2]; // rot
            for (int i = 0; i < cl; i++) work.al = (byte) (((work.al & 0xff) << 1) | ((work.al & 0x80) != 0 ? 1 : 0));
            mml_seg.oplbuf[work.di + mml_seg.oplprg_table[work.bx]] |= work.al; // 設定

            work.bx += 3;
            cx--;
        } while (cx > 0);

        //    assume es:VoiceSeg
        work.si = 0; // offset oplbuf
        work.di = 0; // offset voice_buf
        work.dx = mml_seg.newprg_num;
        work.dx *= 16;
        work.di += work.dx;
        for (int i = 0; i < 9; i++) voice_seg.voice_buf[work.di++] = mml_seg.oplbuf[work.si++];
        work.si = si_p;
        work.bx = work.di;

        nns_pname_set();
    }

    /**
    // スロット毎のデータを転送
     */
    private void slot_trans(int slot) {
        work.bp = work.dx;

        int cx = 6;
        //st_loop:
        do {
            work.al = mml_seg.slot[slot][work.bx];
            voice_seg.voice_buf[work.bp] = work.al;
            work.bx++;
            work.bp += 4;
            cx--;
        } while (cx > 0);
    }

    /**
    // 各スロットの数値を読む
     */
    private void slot_get(int slot) {
        get_param(); // AR
        work.al &= 0b0001_1111;
        mml_seg.slot[slot][2] = work.al;

        get_param(); // DR
        work.al &= 0b0001_1111;
        mml_seg.slot[slot][3] = work.al;

        get_param(); // SR
        work.al &= 0b0001_1111;
        mml_seg.slot[slot][4] = work.al;

        get_param(); // RR
        work.al &= 0b0000_1111;
        mml_seg.slot[slot][5] = work.al;

        get_param(); // SL
        work.al &= 0b0000_1111;
        mml_seg.slot[slot][5] |= (byte) (work.al << 4);

        get_param(); // TL
        work.al &= 0b0111_1111;
        mml_seg.slot[slot][1] = work.al;

        get_param(); // KS
        work.al &= 0b0000_0011;
        mml_seg.slot[slot][2] |= (byte) (work.al << 6);

        get_param(); // ML
        work.al &= 0b0000_1111;
        mml_seg.slot[slot][0] = work.al;

        get_param(); // DT
        if ((work.al & 0x80) != 0) {
            work.al = (byte) -work.al;
            work.al &= 3;
            work.al |= 4;
        }
        work.al &= 0b0000_0111;
        mml_seg.slot[slot][0] |= (byte) (work.al << 4);

        if (mml_seg.dt2_flg != 0) {
            get_param(); // DT2(for opm)
            work.al &= 0b0000_0011;
            mml_seg.slot[slot][4] |= (byte) (work.al << 6);
        }

        get_param(); // AMS
        work.al &= 0b0000_0001;
        mml_seg.slot[slot][3] |= (byte) (work.al << 7);

    }

    /**
    // 音色設定用パラメータの取り出し
     */
    private void get_param() {

        char al;
        do {
            do {
                al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (al >= (char) 0x80) {
                    continue;
                }

                if (al == 9) continue;
                if (al == ' ') continue;
                if (al == ',') continue;
                if (al < ' ' || al == ';') {
                    //gp_skip
                    line_skip();
                    al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
                    if (al == 0x1a) {
                        error('@', 6, work.si);
                    }
                    continue;
                }
                if (al != '`') {
                    if ((mml_seg.skip_flag & 2) != 0) continue;
                    break;
                }

                mml_seg.skip_flag ^= 2;
            } while (true);

            boolean cy;
            int[] bx = new int[1]; byte[] dl = new byte[1];
            //gp_no_skip:
            if (al != '=') { // break get_vname;
                work.si--;
                if (al == '+' || al == '-') {
                    cy = getnum(/* out */ bx, /* out */ dl);
                    work.al = (byte) (work.dx & 0xff);
                    return;
                }
                byte[] alb = new byte[1];
                cy = numget(/* out */ alb);
                if (cy) {
                    error('@', 1, work.si);
                }
                work.si--;
                //gp_gnm:
                cy = getnum(/* out */ bx, /* out */ dl);
                work.al = (byte) (work.dx & 0xff);
                return;
            }
//get_vname:
            work.si--;
//gsc_loop:
            char ch;
            do {
                work.si++;
                ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
                if (ch == ' ') continue; // break gsc_loop;
            } while (ch != 9); // break gsc_loop;

            work.bp = 0; // offset prg_name
            int cx = 7;
//gvn_loop:;
            do {
                al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (al == 9) break;
                if (al == 13) break;
                mml_seg.prg_name += al;
                cx--;
            } while (cx > 0);
//gv_skip:
            //MmlSeg.prg_name[Work.bp] = 0;
            // jmp gp_skip
            line_skip();
            al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (al == 0x1a) {
                error('@', 6, work.si);
            }
        } while (true);
    }

    /**
    // 一行 Compile
    // INPUTS -- ds:si to MML POINTER
    //   -- es:di to M POINTER
    //   -- [PART]
    //        to PART
     */
    private enmPass2JumpTable one_line_compile() {

//#if DEBUG
        int n = mml_seg.mml_buf.indexOf("\r\n", work.si);
        int r = work.si;
        calc_line(/* ref */ r);
        logger.log(Level.DEBUG, String.format("%d(%d) \t%d"
                , mml_seg.mml_filename
                , mml_seg.line
                , mml_seg.mml_buf.substring(work.si, n - work.si)));

        //KUMA:スキップ処理：指定された行かチェック。もしそうなら、スキップ処理の進捗を一つ上げて桁チェック状態にする。
        if ((skipPoint.x == 0 && skipPoint.y == 0) && skipSW == 0 && mml_seg.line == skipPoint.y + 1) {
            skipSW = 1;
        } else if (skipSW == 1) // KUMA:同一行で、同一桁にならずに次の行に移った(つまり行末を示していたり音程コマンドが見つからなかった)場合は強制的に次のコマンドにスキップポイントを含める
        {
            skipSW = 2;
        }

//#endif

        char al = (char) 0;
        do {
            do {
                mml_seg.lastprg = 0; // Rパート用
                al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (al == 0xd) // cr
                {
                    //olc_fin:;
                    work.si++;
                    //comend:;
                    if (mml_seg.hsflag != 0)
                        return enmPass2JumpTable.hscom_exit;
                    return enmPass2JumpTable.cloop;
                }

                if (al == ';') {
                    //olc_skip:; // KUMA:ここに移動
                    line_skip();
                    if (mml_seg.hsflag != 0)
                        return enmPass2JumpTable.hscom_exit;
                    return enmPass2JumpTable.cloop;
                }
                if (al != '`') break;
                mml_seg.skip_flag ^= 2;
            } while (true);

            //olc_no_skip:;
            if ((mml_seg.skip_flag & 2) != 0) return enmPass2JumpTable.olc_skip2;
        } while (al >= ' ' + 1);

        return enmPass2JumpTable.olc02;
    }

    private enmPass2JumpTable olc0() {
        char al = (char) 0;
        mml_seg.prsok = al;
        return enmPass2JumpTable.olc02;
    }

    private enmPass2JumpTable olc02() {
        if (m_seg.mbuf_end != 0x7f) // check code
        {
            error(0x00, 19, work.si); // 容量オーバー
        }
        return enmPass2JumpTable.olc03;
    }

    private enmPass2JumpTable olc03() {
notend: // ↑
        {
            do {
                mml_seg.stPos = work.si;
                char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
                if (ch == '　') continue;
                work.al = (byte) ch;
                if (work.al == 9) continue; // tab
                if (work.al == (byte) ' ') continue;
                if (work.al < (byte) ' ') break notend;
                if (work.al == (byte) ';') {
                    //olc_skip:; // KUMA:ここに移動
                    line_skip();
                    if (mml_seg.hsflag != 0)
                        return enmPass2JumpTable.hscom_exit;
                    return enmPass2JumpTable.cloop;
                }
                if (work.al != (byte) '`') break;
                mml_seg.skip_flag ^= 2;
            } while (true);

            //olc_no_skip2:;
            if ((mml_seg.skip_flag & 2) != 0) return enmPass2JumpTable.olc_skip2;
            //notskp:;
            if (work.al == (byte) '"') { // break nskp_01;
                mml_seg.skip_flag ^= 1;

                byte dh = (byte) 0xc0;
                byte dl = (byte) (mml_seg.skip_flag & 1);
                work.dx = (dh & 0xff) * 0x100 + (dl & 0xff);
                return enmPass2JumpTable.parset;
            }
//nskp_01:
            if (work.al == (byte) '\'') {
                mml_seg.skip_flag &= 0xfe;

                byte dh = (byte) 0xc0;
                byte dl = 0x00;
                work.dx = (dh & 0xff) * 0x100 + (dl & 0xff);
                return enmPass2JumpTable.parset;
            }
//nskp_02:
//#if !efc
            if (work.al == (byte) '|') return enmPass2JumpTable.skip_mml;
//#endif

            if (work.al == (byte) '/') {
                //part_end2:; // KUMA:ここに移動
                if (mml_seg.hsflag == 0) return enmPass2JumpTable.part_end;
                return enmPass2JumpTable.hscom_exit;
            }
        }
//notend:
        if (work.al != 13) return enmPass2JumpTable.olc00;
//olc_fin:
        work.si++;
//comend:
        if (mml_seg.hsflag != 0)
            return enmPass2JumpTable.hscom_exit;
        return enmPass2JumpTable.cloop;
    }

    private enmPass2JumpTable olc_skip2() {
        char al;
        do {
            al = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (al == 0xa) // lf
            {
                if (mml_seg.hsflag != 0)
                    return enmPass2JumpTable.hscom_exit;
                return enmPass2JumpTable.cloop;
            }
        } while (al != '`');

        mml_seg.skip_flag &= 0xfd;

        return enmPass2JumpTable.olc03;
    }

    /**
    // "|" command(Skip MML except selected Parts)
     */
    private enmPass2JumpTable skip_mml() {
//#if !efc
        byte ah = (byte) (mml_seg.part & 0xff);
        ah += (byte) ('A' - 1);

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);

        if (ch < '!') return enmPass2JumpTable.olc03; // | only = Select All
        if (ch == '!') { // break skm_loop;

            work.si++;

//skm_reverse_loop:;
            do {
                ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (ch == ah) {
//                    break part_not_found;
                    /**
                    // Not Found --- Skip to Next "|" or Next line
                     */
//part_not_found: // ↑
                    do {
                        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                        if (ch == 13) {
                            work.si++;
                            if (mml_seg.hsflag != 0)
                                return enmPass2JumpTable.hscom_exit;
                            return enmPass2JumpTable.cloop;
                            //cr // line end
                        }
                    } while (ch != '|');
                    return enmPass2JumpTable.skip_mml;
                }
                if (ch == 13) {
                    work.si++;
                    if (mml_seg.hsflag != 0)
                        return enmPass2JumpTable.hscom_exit;
                    return enmPass2JumpTable.cloop;
                    //cr // line end
                }
            } while (ch >= (char) (' ' + 1));
            work.si--;
//            break part_found;
        } else {
//skm_loop:
            do {
                ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                if (ch == ah) break; // part_found;
                if (ch == 13) {
                    work.si++;
                    if (mml_seg.hsflag != 0)
                        return enmPass2JumpTable.hscom_exit;
                    return enmPass2JumpTable.cloop;
                    //cr // line end
                }
            } while (ch >= (char) (' ' + 1));
        }

        /**
        // Found --- Compile Next
         */
//part_found:
        do {
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
            if (ch == 13) {
                work.si++;
                if (mml_seg.hsflag != 0)
                    return enmPass2JumpTable.hscom_exit;
                return enmPass2JumpTable.cloop;
                //cr // line end
            }
        } while (ch >= (char) (' ' + 1));
//#endif
        return enmPass2JumpTable.olc03;
    }

    /**
    // Command Jump
     */
    private enmPass2JumpTable olc00() {
        //KUMA:スキップしたい桁まで移動していた場合は、スキップ処理の進捗をひとつあげる
        if (skipSW == 1 && skipPointCol <= work.si - 1) {
            skipSW = 2;
        }

        work.bx = 0; // offset comtbl
        // olc1:
        do {
            if (String.valueOf((char) work.al).equals(comtbl[work.bx].getItem1())) {
                break;
            }
            work.bx++;
            if (comtbl.length == work.bx) {
                error(0, 1, work.si);
            }
        } while (true);

        //KUMA:コマンドのインデックスと格納アドレスを一時保存
        int bbx = work.bx;
        int bdi = work.di;

        byte dh = work.al;
        work.dx = ((dh & 0xff) * 0x100) | (work.dx & 0xff);
        if (comtbl[work.bx].getItem2() != null) {
            logger.log(Level.TRACE, "olc00:command:%c".formatted((char) dh));

            enmPass2JumpTable ret = comtbl[work.bx].getItem2().get();

            //KUMA:スキップ位置を割り出す
            if (skipSW == 2) {
                //音階コマンドのみ対象とする
                if (bbx < 9 || bbx == 0x33) {
                    skipIndex = bdi;
                    skipSW = 3;
                }
            }

            return ret;
        }

        throw new PmdErrorExitException(String.format("まだ移植できてないコマンドを検出しました(%c)", (char) dh));
    }

    /**
    // Command Table
     */

    private int ou00 = 11;
    private int od00 = 12;
    private Tuple<String, Supplier<enmPass2JumpTable>>[] comtbl;

    private void setupComTbl() {
        comtbl = new Tuple[] {
                new Tuple<String, Supplier<enmPass2JumpTable>>("c", this::otoc)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("d", this::otod)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("e", this::otoe)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("f", this::otof)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("g", this::otog)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("a", this::otoa)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("b", this::otob)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("r", this::otor)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("x", this::otox)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("l", this::lengthset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("o", this::octset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>(">", this::octup)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("<", this::octdown)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("C", this::zenlenset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("t", this::tempoa)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("T", this::tempob)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("q", this::qset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("Q", this::qset2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("v", this::vseta)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("V", this::vsetb)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("R", this::neirochg)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("@", this::neirochg)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("&", this::tieset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("D", this::detset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("[", this::stloop)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("]", this::edloop)
                , new Tuple<String, Supplier<enmPass2JumpTable>>(":", this::extloop)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("L", this::lopset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("_", this::oshift)
                , new Tuple<String, Supplier<enmPass2JumpTable>>(")", this::volup)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("(", this::voldown)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("M", this::lfoset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("*", this::lfoswitch)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("E", this::psgenvset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("y", this::ycommand)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("w", this::psgnoise)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("P", this::psgpat)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("!", this::hscom)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("B", this::bendset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("I", this::pitchset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("p", this::panset)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("\\", this::rhycom)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("X", this::octrev)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("^", this::lngmul)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("=", this::lngrew)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("H", this::hardlfo_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("#", this::hardlfo_onoff)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("Z", this::syousetu_lng_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("S", this::sousyoku_onp_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("W", this::giji_echo_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("~", this::status_write)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("{", this::porta_start)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("}", this::porta_end)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("n", this::ssg_efct_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("N", this::fm_efct_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("F", this::fade_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("s", this::slotmask_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("m", this::partmask_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("O", this::tl_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("A", this::adp_set)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("0", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("1", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("2", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("3", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("4", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("5", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("6", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("7", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("8", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("9", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("%", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("$", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>(".", this::lngrew_2)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("-", this::lng_dec)
                , new Tuple<String, Supplier<enmPass2JumpTable>>("+", this::tieset_2)
        };
    }

    /**
    // A command(ADPCM set)
     */
    private enmPass2JumpTable adp_set() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xc0));
        m_seg.m_buf.set(work.di++, new MmlDatum(0xf7));

        boolean cy;
        int[] bx = new int[1]; byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // O command(TL set)
     */
    private enmPass2JumpTable tl_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] dl = new byte[1];

        work.al = (byte) 0xb8;

        if (mml_seg.part == mml_seg.rhythm) {
            error('O', 17, work.si);
        }

        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = (byte) (work.bx & 0xff);
        if (work.al >= 16) {
            error('O', 6, work.si);
        }

        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);

        if (work.al != (byte) ',') {
            error('O', 6, work.si);
        }

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == '+' || ch == '-') { // break tl_slide; // TODO vavi check
//            if (ch == '-') { // break tl_next;
//tl_slide:
            byte d = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
            d |= (byte) 0xf0;
            m_seg.m_buf.set(work.di - 1, new MmlDatum(d & 0xff));
//tl_next:
            cy = getnum(/* out */ bx, /* out */ dl);
            work.al = dl[0];
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        }
        return enmPass2JumpTable.olc0;
    }

    /**
    // m command(part mask)
     */
    private Mc.enmPass2JumpTable partmask_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);

        if (cy) {
            error('m', 6, work.si);
        }

        if (work.al >= 2) {
            error('m', 2, work.si);
        }

        work.dx = 0xc000 + work.al;
        return enmPass2JumpTable.parset;
    }

    /**
    // s command(fm slot mask)
     */
    private enmPass2JumpTable slotmask_set() {
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == 'd') return slotdetune_set();
        if (ch == 'k') return slotkeyondelay_set();
        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);

        m_seg.m_buf.set(work.di++, new MmlDatum(0xcf));
        work.ah = work.al;
        work.al = 0;

        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == ',') { // break not_car_set;
            work.si++;
            cy = lngset(/* out */ bx, /* out */ al);
        }
//not_car_set:
        work.ah <<= 4;
        work.ah &= 0xf0;
        work.al &= 0x0f;
        work.al |= work.ah;

        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // sd command(slot detune) / sdd command(slot detune 相対)
     */
    private enmPass2JumpTable slotdetune_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] dl = new byte[1];

        work.al = (byte) 0xc8;
        work.si++;
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == 'd') { // break sds_set;
            work.al--; // al=0c7h
            work.si++;
        }
//sds_set:
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = dl[0];
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') {
            error('s', 6, work.si);
        }
        work.si++;
        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = (byte) (work.bx & 0xff);
        work.ah = (byte) ((work.bx & 0xff) >> 8);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));

        return enmPass2JumpTable.olc0;
    }

    /**
    // sk command(slot keyon delay)
     */
    private enmPass2JumpTable slotkeyondelay_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] dl = new byte[1];

        work.al = (byte) 0xb5;
        work.si++;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = dl[0];
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == ',') { // break sks_err;

            work.si++;
            work.dx = 's' * 0x100 + (work.dx & 0xff);
            get_clock();

//sks_exit:
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            return enmPass2JumpTable.olc0;
        }
//sks_err:
        if (work.al == 0) {
//            break sks_exit;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff)); // <<
            return enmPass2JumpTable.olc0; // <<
        }
        error('s', 6, work.si);
        return enmPass2JumpTable.exit; // dummy
    }

    /**
    // n command(ssg effect)
     */
    private enmPass2JumpTable ssg_efct_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];

        cy = lngset(/* out */ bx, /* out */ al);

        if (mml_seg.skip_flag != 0) return enmPass2JumpTable.olc03;

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd4));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        return enmPass2JumpTable.olc0;
    }

    /**
    // N command(fm effect)
     */
    private enmPass2JumpTable fm_efct_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];

        cy = lngset(/* out */ bx, /* out */ al);

        if (mml_seg.skip_flag != 0) return enmPass2JumpTable.olc03;

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd3));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        return enmPass2JumpTable.olc0;
    }

    /**
    // F command(fadeout)
     */
    private enmPass2JumpTable fade_set() {
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == 'B') return fb_set();

        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];

        cy = lngset(/* out */ bx, /* out */ al);

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd2));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        return enmPass2JumpTable.olc0;
    }

    /**
    // FB command(FeedBack set)
     */
    private enmPass2JumpTable fb_set() {
        boolean cy;
        int[] bx = new int[1]; byte[] dl = new byte[1];

        work.si++;

        work.al = (byte) 0xb6;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != '+') { // break _fb_set;
            if (ch != '-') { // break _fb_set;

                cy = getnum(/* out */ bx, /* out */ dl);

                if ((byte) work.bx >= 8) {
                    error('F', 2, work.si);
                }

                work.al = (byte) (work.bx & 0xff);

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                return enmPass2JumpTable.olc0;
            }
        }
//_fb_set:
        cy = getnum(/* out */ bx, /* out */ dl);
        dl[0] += 7;
        if (dl[0] >= 15) {
            error('F', 2, work.si);
        }

        work.al = (byte) (work.bx & 0xff);
        work.al |= (byte) 0x80;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // "{" Command [Portament_start] / "{{" Command [分散和音開始]
     */
    private enmPass2JumpTable porta_start() {
        if (mml_seg.skip_flag != 0) return enmPass2JumpTable.olc03;

        if (mml_seg.porta_flag != 0) {
            error('{', 9, work.si);
        }

        MmlDatum md = new MmlDatum(0xda);
        md.linePos = new LinePos();
        md.linePos.col = Math.max(work.si - mml_seg.linehead, 1);
        m_seg.m_buf.set(work.di++, md);
        mml_seg.porta_flag = 1;

        // 分散和音開始アドレスをセット  4.8r
        mml_seg.bunsan_start = 0;
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != '{') return enmPass2JumpTable.olc0;
        work.si++;
        mml_seg.bunsan_start = work.di;

        //pst_end:
        return enmPass2JumpTable.olc0;
    }

    /**
    // "}" Command [Portament_end] / "}}" Command [分散和音終了]
     */
    private enmPass2JumpTable porta_end() {
        boolean cy;
        int[] bx = new int[1]; byte[] al = new byte[1];

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == '}') return bunsan_end();      // 4.8r

        if (mml_seg.skip_flag == 0) { // break pe_skip;
            if (mml_seg.porta_flag != 1) {
                error('}', 13, work.si);
            }

            byte cch = (byte) (m_seg.m_buf.get(work.di - 5).dat & 0xff);
            if (cch != (byte) 0xda) {
                error('}', 14, work.si);
            }
            cch = (byte) (m_seg.m_buf.get(work.di - 4).dat & 0xff);
            if (cch == 0x0f) {
                error('}', 15, work.si);
            }
            cch = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
            if (cch == 0x0f) {
                error('}', 15, work.si);
            }

            MmlDatum srcMd = m_seg.m_buf.get(work.di - 4);
            MmlDatum dstMd = m_seg.m_buf.get(work.di - 5);
            dstMd.args = srcMd.args;
            LinePos lp = dstMd.linePos;
            dstMd.linePos = srcMd.linePos;
            dstMd.linePos.col = lp.col;
            dstMd.linePos.length = Math.max(work.si - mml_seg.linehead + 1, 1) - lp.col;
            dstMd.type = srcMd.type;
            srcMd.linePos = null;

            work.al = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
            m_seg.m_buf.set(work.di - 3, new MmlDatum(work.al & 0xff));

            work.di -= 2;

            mml_seg.porta_flag = 0;
            cy = lngset2(/* out */ bx, /* out */ al);
            if ((work.bx & 0xff00) != 0) {
                error('}', 8, work.si);
            }
            lngcal();
            cy = futen();
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            if (cy) {
                error('}', 8, work.si);
            }

            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch == ',') { // break pe_exit;

                // ディレイ指定
                work.si++;
                cy = lngset2(/* out */ bx, /* out */ al);
                if (cy) {
                    error('}', 6, work.si);
                }
                lngcal();
                cy = futen();
                cch = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
                if (work.al >= cch) // KUMA:ディレイ値が指定音長よりも長い場合はエラー
                {
                    error('}', 8, work.si);
                }
                work.dx = m_seg.m_buf.get(work.di - 2).dat + m_seg.m_buf.get(work.di - 1).dat * 0x100;
                m_seg.m_buf.set(work.di + 1, new MmlDatum(work.dx & 0xff));
                m_seg.m_buf.set(work.di + 2, new MmlDatum((work.dx & 0xff00) >> 8));
                work.dx = m_seg.m_buf.get(work.di - 4).dat + m_seg.m_buf.get(work.di - 3).dat * 0x100; // dh=start ontei
                m_seg.m_buf.set(work.di - 1, new MmlDatum(work.dx & 0xff));
                m_seg.m_buf.set(work.di + 0, new MmlDatum((work.dx & 0xff00) >> 8));
                m_seg.m_buf.set(work.di - 4, new MmlDatum((work.dx & 0xff00) >> 8));
                m_seg.m_buf.set(work.di - 3, new MmlDatum(work.al & 0xff));
                m_seg.m_buf.set(work.di - 2, new MmlDatum(0xfb)); // "&"
                cch = (byte) (m_seg.m_buf.get(work.di + 2).dat & 0xff);
                cch -= work.al;
                m_seg.m_buf.set(work.di + 2, new MmlDatum(cch));
                work.di += 3;
            }
//pe_exit:
            mml_seg.prsok = 9; // ポルタの音長
            return enmPass2JumpTable.olc02;
        }
//pe_skip:
        cy = lngset2(/* out */ bx, /* out */ al);
        if ((work.bx & 0xff00) != 0) {
            error('}', 8, work.si);
        }
        futen_skip();

        // ------ディレイスキップ処理を挿入 2019 / 12 / 28
        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;
        cy = lngset2(/* out */ bx, /* out */ al);
        if (cy) {
            error('}', 6, work.si);
        }
        futen_skip();
        // ------ここまで
        return enmPass2JumpTable.olc03;
    }

    /**
    // "}}" Command[分散和音終了] 4.8r
    // {{cdeg
    //    }
    //}
    //lng[, cnt[, tie[, gate[, vol]]]]
     */
    private enmPass2JumpTable bunsan_end() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        work.si++;

        if (mml_seg.skip_flag != 0) return bunsan_skip();

        // 和音数のチェック
        work.bx = mml_seg.bunsan_start;
        if (work.bx == 0) {
            error('}', 34, work.si);
        }

        int cx = work.di;
        if (cx == work.bx) {
            error('}', 35, work.si); // 音なし
        }
        cx -= work.bx;

        if ((cx & 1) != 0) {
            error('}', 35, work.si); // 間が奇数バイトでエラー
        }
        cx >>= 1; // cx = 分散和音数

        if (cx >= 17) error('}', 35, work.si); // 17音以上でエラー

        mml_seg.bunsan_count = (byte) (cx & 0xff);

        // bunsan_work に音階をセットしていく

        work.bp = 0; // offset bunsan_work

        //bend_loop:;
        do {
            work.al = (byte) (m_seg.m_buf.get(work.bx).dat & 0xff);
            if ((work.al & 0x80) != 0) error('}', 35, work.si); // 音階ではない

            mml_seg.bunsan_work[work.bp] = work.al;
            work.bx += 2;
            work.bp++;

        } while (work.di != work.bx);

        // パラメータ取り込み
        cy = lngset2(/* out */ bx, /* out */ al); // prm1 全体音長
        if ((bx[0] & 0xff00) != 0) error('}', 35, work.si);

        lngcal();
        cy = futen();
        if (cy) error('}', 35, work.si);

        mml_seg.bunsan_length = work.al;
        work.al = 0;
        mml_seg.bunsan_vol = 0; // 音量 def = ±0
        mml_seg.bunsan_gate = 0; // ゲート def = 0
        work.al++;
        mml_seg.bunsan_1cnt = work.al; // 1cnt def = % 1
        mml_seg.bunsan_tieflag = work.al; // Tie def = ON(1)

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == ',') { // break bunsan_main;

            work.si++;

            cy = lngset2(/* out */ bx, /* out */ al); // prm2 1音符長
            if (!cy) { // break bunsan_prm3;
                if ((bx[0] & 0xff00) != 0) error('}', 35, work.si);

                lngcal();
                cy = futen();
                if (cy) error('}', 35, work.si);

                mml_seg.bunsan_1cnt = work.al;
            }
//bunsan_prm3:
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch == ',') { // break bunsan_main;

                work.si++;

                cy = lngset2(/* out */ bx, /* out */ al); // prm3 タイフラグ
                if (!cy) { // break bunsan_prm4;

                    if ((bx[0] & 0xff00) != 0) error('}', 35, work.si);
                    if (work.al >= 2) error('}', 35, work.si);

                    mml_seg.bunsan_tieflag = work.al;
                }
//bunsan_prm4:
                ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
                if (ch == ',') { // break bunsan_main;

                    work.si++;

                    cy = lngset2(/* out */ bx, /* out */ al); // prm4 ゲート長
                    if (!cy) { // break bunsan_prm5;

                        if ((bx[0] & 0xff00) != 0) error('}', 35, work.si);
                        if (work.al >= mml_seg.bunsan_length) error('}', 35, work.si);

                        mml_seg.bunsan_gate = work.al;
                        mml_seg.bunsan_length -= work.al;
                    }
//bunsan_prm5:
                    ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
                    if (ch == ',') { // break bunsan_main;

                        work.si++;

                        cy = getnum(/* out */ bx, /* out */ dl); // prm5 音量±
                        mml_seg.bunsan_vol = dl[0];
                    }
                }
            }
        }
        // 分散和音展開
//bunsan_main:
        work.di = mml_seg.bunsan_start;
        work.di--; // ポルタコマンドをつぶす

        //ループ回数チェック
        work.al = mml_seg.bunsan_1cnt;
        int ax = mml_seg.bunsan_count * work.al; // AX = 音符数 x 一音符の長さ = 1ループの長さ
        work.al = (byte) (ax & 0xff);
        if ((ax & 0xff00) != 0) error('}', 35, work.si);
        mml_seg.bunsan_1loop = work.al;

        cx = 0;
        cx = mml_seg.bunsan_length;

        int tmp = cx; // AX = 全体の長さ / CX = 1ループの長さ
        cx = ax;
        ax = tmp;
        if (cx < ax) { // break bunsan_last; // 1ループに満たない場合

            work.al = (byte) ((ax / cx) & 0xff); // AL = 全体の長さ \ 1ループの長さ = ループ回数
            work.ah = (byte) ((ax % cx) & 0xff);
            if (mml_seg.bunsan_tieflag != 0) { // break bunsan_setloop;

                if (work.ah == 0) { // break bunsan_setloop; // タイありで割り切れた場合は
                    work.al--; // ループ回数 -1
                }
            }
//bunsan_setloop:
            if (work.al != 1) { // break bunsan_nonloop; // ループ1回ならループ不要

                byte al_p = work.al;
                byte ah_p = work.ah;

                work.al = (byte) 0xf9; // "[" Loop Start
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                work.bx = work.di; // BX = 戻り先
                work.di += 2;

                int bx_p = work.bx;
                bunsan_set1loop();
                work.bx = bx_p;

                work.al = (byte) 0xf8; // "]" Loop End

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.ah = ah_p;
                work.al = al_p;

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.al *= mml_seg.bunsan_1loop;
                mml_seg.bunsan_length -= work.al;
                ax = work.bx;
                ax -= 0; // offset m_buf

                m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

                ax = work.di - 4;  //    lea ax,[di-4]
                ax -= 0; // offset m_buf
                m_seg.m_buf.set(work.bx + 0, new MmlDatum(ax & 0xff));
                m_seg.m_buf.set(work.bx + 1, new MmlDatum((ax & 0xff00) >> 8)); // 戻り先セット

                work.ah = (byte) ((ax & 0xff00) >> 8);
                work.al = (byte) (ax & 0xff);

//                break bunsan_last;
            } else {
                // 1ループでいいのでループ不要
//bunsan_nonloop:
                bunsan_set1loop();

                work.al = mml_seg.bunsan_1loop;
                mml_seg.bunsan_length -= work.al;
            }
        }
        // ループ後最終セット
//bunsan_last:
        if (mml_seg.bunsan_length != 0) { // break bunsan_exit; // タイなしの場合ここで0になることがある

            work.bx = 0; // offset bunsan_work

//bunsan_last_loop:
            while (true) {
                work.al = mml_seg.bunsan_work[work.bx];
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                work.bx++;
                work.al = mml_seg.bunsan_1cnt;
                work.ah = mml_seg.bunsan_length;

                if (work.al >= work.ah)
                    break; // bunsan_lastnote;

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                mml_seg.bunsan_length -= work.al;

                if (mml_seg.bunsan_tieflag != 1)
                    continue; // break bunsan_last_loop;

                work.al = (byte) 0xfb;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                continue; // break bunsan_last_loop;
            }
//bunsan_lastnote:
            work.al = work.ah;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        }
        // 分散和音終了処理
//bunsan_exit:
        work.ah = mml_seg.bunsan_gate; // Gateがある場合は休符追加
        if (work.ah != 0) { // break bunsan_exit2;
            work.al = 0xf;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            work.al = work.ah;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        }
//bunsan_exit2:
        work.al = 0;
        work.ah = 0;

        mml_seg.bunsan_start = 0;
        mml_seg.porta_flag = 0;

        return enmPass2JumpTable.olc0;
    }

    // 1ループ分音階をセット
    private void bunsan_set1loop() {
        work.bx = 0; // offset bunsan_work
        int cx = mml_seg.bunsan_count; // CX=音符数

        //bunsan_s1l_loop:;
        do {
            work.al = mml_seg.bunsan_work[work.bx];
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff)); // 音階セット
            work.bx++;
            work.al = mml_seg.bunsan_1cnt;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff)); // 長さセット

            if (mml_seg.bunsan_tieflag == 1) {
                work.al = (byte) 0xfb; // "&"セット
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            }
            //bunsan_s1l_fin:;
            cx--;
        } while (cx > 0);

        work.ah = mml_seg.bunsan_vol;
        if (work.ah != 0) { // break bunsan_s1l_exit;
            work.al = (byte) 0xe3; // )x
            if ((work.ah & 0x80) != 0) { // break bunsan_sl1_volset;
                work.al--; // (x
                work.ah = (byte) (-work.ah & 0xff);
            }
//bunsan_sl1_volset:
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            work.al = work.ah;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        }
//bunsan_s1l_exit:
        return;

        //bunsan_error:   ;分散和音／エラー時の処理
        //各々で処理
    }

    private enmPass2JumpTable bunsan_skip() { // 分散和音／スキップ時の処理
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        lngset2(/* out */ bx, /* out */ al);
        futen_skip();
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;
        lngset2(/* out */ bx, /* out */ al);
        futen_skip();
        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;
        lngset2(/* out */ bx, /* out */ al);
        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;
        lngset2(/* out */ bx, /* out */ al);
        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;
        getnum(/* out */ bx, /* out */ dl);

        //bskip_end:
        return enmPass2JumpTable.olc03;
    }

    /**
    // "~" Command[ＳＴＡＴＵＳの書き込み]
    // ~[+,-] n
     */
    private enmPass2JumpTable status_write() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != '-') { // break sw_sweep;
            if (ch != '+') { // break sw_sweep;

                cy = lngset(/* out */ bx, /* out */ al);
                work.dx = 0xdc00 + work.al;
                return enmPass2JumpTable.parset;
            }
        }
//sw_sweep:
        cy = getnum(/* out */ bx, /* out */ dl);
        work.dx = 0xdb00 + dl[0];
        return enmPass2JumpTable.parset;
    }

    /**
    // "W" Command[擬似エコーの設定]
    // Wdelay[, +-depth][, tie / nextflag]
     */
    private enmPass2JumpTable giji_echo_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        work.dx = 'W' * 0x100 + (work.dx & 0xff);
        get_clock();

        mml_seg.ge_delay = work.al;
        if (work.al != 0) { // break ge_cut;
            mml_seg.ge_tie = 0;
            mml_seg.ge_dep_flag = 0;
            mml_seg.ge_depth = -1;
            mml_seg.ge_depth2 = -1;

            char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch != ',') return enmPass2JumpTable.olc03;

            work.si++;

            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch == '%') { // break ge_no_depf;

                work.si++;

                mml_seg.ge_dep_flag = 1;
            }
//ge_no_depf:
            cy = getnum(/* out */ bx, /* out */ dl);

            mml_seg.ge_depth = dl[0];
            mml_seg.ge_depth2 = dl[0];

            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch != ',') return enmPass2JumpTable.olc03;

            work.si++;

            cy = lngset(/* out */ bx, /* out */ al);

            mml_seg.ge_tie = work.al;
            return enmPass2JumpTable.olc03;
        }
//ge_cut:

        mml_seg.ge_depth = work.al;
        mml_seg.ge_depth2 = work.al;

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;

        cy = getnum(/* out */ bx, /* out */ dl); // dummy

        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;

        cy = getnum(/* out */ bx, /* out */ dl); // dummy

        return enmPass2JumpTable.olc03;
    }

    /**
    // "S" Command[装飾音符の設定]
    // Sspeed[, depth]
     */
    private enmPass2JumpTable sousyoku_onp_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == 'E') return ssgeg_set();

        work.dx = 'S' * 0x100 + (work.dx & 0xff);
        get_clock();

        mml_seg.ss_speed = work.al;
        if (work.al != 0) { // break ss_cut;

            mml_seg.ss_tie = 1;
            mml_seg.ss_depth = -1;

            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch == ',') { // break ss_exit;

                work.si++;

                cy = getnum(/* out */ bx, /* out */ dl);

                mml_seg.ss_depth = dl[0];

                ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
                if (ch == ',') { // break ss_exit;

                    work.si++;

                    cy = lngset(/* out */ bx, /* out */ al);

                    mml_seg.ss_tie = al[0];
                }
            }
//ss_exit:
            work.ah = (byte) (mml_seg.ss_depth & 0xff);
            if ((work.ah & 0x80) != 0) { // break ss_exit_1;
                work.ah = (byte) -work.ah;
            }
//ss_exit_1:
            work.al = (byte) (mml_seg.ss_speed & 0xff);
            if (work.ah != 1) { // break ss_exit_2;

                if (work.al * work.ah > 0xff) {
                    error('S', 2, work.si);
                }
                work.al = (byte) (work.al * work.ah);
            }
//ss_exit_2:
            mml_seg.ss_length = work.al;
            return enmPass2JumpTable.olc03;
        }
//ss_cut:
        work.al = 0;
        mml_seg.ss_depth = 0;
        mml_seg.ss_length = 0;

        ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') return enmPass2JumpTable.olc03;

        work.si++;

        cy = lngset(/* out */ bx, /* out */ al); // dummy
        return enmPass2JumpTable.olc03;
    }

    /**
    // "SE" Command[SSGEG指定] →yコマンド変換
    // SEslot,num
     */
    private enmPass2JumpTable ssgeg_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        // mov dx,"S"*256+17
//#if !efc
        if (mml_seg.ongen >= mml_seg.psg) // FMでなければエラー
        {
            error('S', 17, work.si);
        }
//#endif
        work.al = (byte) mml_seg.opl_flg; // OPL/OPMではエラー
        if ((work.al | mml_seg.x68_flg) != 0) {
            error('S', 17, work.si);
        }

        work.si++;

        cy = lngset2(/* out */ bx, /* out */ al);
        if (cy) {
            error('S', 6, work.si);
        }

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch != ',') {
            error('S', 6, work.si);
        }

        if (work.al == 0) {
            error('S', 2, work.si);
        }
        if (work.al >= 16) {
            error('S', 2, work.si);
        }

        work.si++;

        byte al_p = work.al;
        byte ah_p = work.ah;

        cy = lngset2(/* out */ bx, /* out */ al); // AL=num
        int cx = (ah_p & 0xff) * 0x100 + (al_p & 0xff); // CL=slot

        if (cy) {
            error('S', 6, work.si);
        }

        //    mov dl,2

        if (work.al >= 16) {
            error('S', 2, work.si);
        }

        byte dh = (byte) mml_seg.part; // DHにFMのSSGEG reg取得
        dh--;
        if (dh >= 6) { // break sss_notfm3ex;
            dh = 2; // 6以上はFM3exのみ
        }
//sss_notfm3ex:
        if (dh >= 3) { // break sss_notfm2;
            dh -= 3; // FM2ならpart -3
        }
sss_notfm2:
        dh += 0x90; // SSGEG $90 + [part]
        if ((cx & 1) == 0) {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);                // 指定slotに対応したyコマンド発行
//            break sss_slot2;
        } else {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
            sss_set1slot(dh);
        }
//sss_slot2:
        dh += 8;
        if ((cx & 1) == 0) {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
//            break sss_slot3;
        } else {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
            sss_set1slot(dh);
        }
//sss_slot3:
        dh -= 4;
        if ((cx & 1) == 0) {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
//            break sss_slot4;
        } else {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
            sss_set1slot(dh);
        }
//sss_slot4:
        dh += 8;
        if ((cx & 1) == 0) {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
//            break sss_fin;
        } else {
            cx = (cx & 0xff00) | (((byte) cx) >> 1);
            sss_set1slot(dh);
        }
//sss_fin:
        return enmPass2JumpTable.olc0;
    }

    private void sss_set1slot(byte dh) {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xef));
        m_seg.m_buf.set(work.di++, new MmlDatum(dh & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
    }

    /**
    // "Z" Command[小節の長さ指定]
     */
    private enmPass2JumpTable syousetu_lng_set() {
        int[] bx = new int[1]; byte[] al = new byte[1];
        lngset(/* out */ bx, /* out */ al);
        return syousetu_lng_set_2();
    }

    private enmPass2JumpTable syousetu_lng_set_2() {
        work.dx = 0xdf00 + work.al;
        return enmPass2JumpTable.parset;
    }

    /**
    // "H" Command （ハードLFOの設定）
    //  Hpms[, ams][, dly]
     */
    private enmPass2JumpTable hardlfo_set() {
//#if efc
//        error('H', 11, work.si);
//#else
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al >= 8) {
            error('H', 2, work.si);
        }

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == ',') { // break pmsonly;
            work.si++;
            int ax_p = (work.ah & 0xff) * 0x100 + (work.al & 0xff);
            cy = lngset(/* out */ bx, /* out */ al);
            work.bx = ax_p;

            if (work.al >= 4) {
                error('H', 2, work.si);
            }

//            break bxset00;
        } else {
//pmsonly:
            work.bx = (work.bx & 0xff00) + work.al;
            work.al = 0;
        }
//bxset00:
        work.al <<= 4;
        work.al |= (byte) (work.bx & 0xff);
        work.al &= 0b0011_0111;
        work.ah = work.al;
        work.al = (byte) 0xe1;

        m_seg.m_buf.set(work.di + 0, new MmlDatum(work.al & 0xff));
        m_seg.m_buf.set(work.di + 1, new MmlDatum(work.ah & 0xff));
        work.di += 2;

        work.dx = 'H' * 0x100 + (work.dx & 0xff);
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);

        if (work.al == (byte) ',') return hdelay_set2();
        work.si--;

        return enmPass2JumpTable.olc0;
//#endif
    }

    /**
    // Command "#" （ハードLFOのスイッチ）
    //  #sw[,depth]
    // Command "#w/#p/#a/##" （OPM用）
    //  #w wf
    //  #p pmd
    //  #a amd
    //  ## wf,pmd,amd
    // Command "#D" ハードLFOディレイ
     */
//#if efc

//    private enmPass2JumpTable hardlfo_onoff() {
//        error('#', 11, work.si);
//    }
//#else

    private enmPass2JumpTable hardlfo_onoff() {
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al == (byte) 'D') return hdelay_set();
        if (work.al == (byte) 'f') return opm_hf_set();
        if (work.al == (byte) 'w') return opm_wf_set();
        if (work.al == (byte) 'p') return opm_pmd_set();
        if (work.al == (byte) 'a') return opm_amd_set();
        if (work.al == (byte) '#') return opm_all_set();
        work.si--;
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al == 0) { // break hlon;

            char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (ch == ',') { // break hloff_noparam;
                work.si++;

                cy = lngset(/* out */ bx, /* out */ al); // Dummy
            }
//hloff_noparam:
            work.dx = 0xe000;
            return enmPass2JumpTable.parset;
        }
//hlon:
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != ',') {
            error('#', 6, work.si);
        }
        work.si++;

        cy = lngset(/* out */ bx, /* out */ al);

        if (work.al >= 8) {
            error('#', 2, work.si);
        }
        work.al |= 0b0000_1000;
        work.dx = 0xe000 + work.al;
        return enmPass2JumpTable.parset;
    }

    private Mc.enmPass2JumpTable hdelay_set() {
        work.dx = '#' * 0x100 + (work.dx & 0xff);
        return hdelay_set2();
    }

    private enmPass2JumpTable hdelay_set2() {
        get_clock();

        work.dx = 0xe400 + work.al;
        return enmPass2JumpTable.parset;
    }

    private enmPass2JumpTable opm_hf_set() {
        hf_set();
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable opm_wf_set() {
        wf_set();
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable opm_pmd_set() {
        pmd_set();
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable opm_amd_set() {
        amd_set();
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable opm_all_set() {
        hf_set();
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al != ',') error('#', 6, work.si);
        wf_set();
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al != ',') error('#', 6, work.si);
        pmd_set();
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al != ',') error('#', 6, work.si);
        amd_set();
        return enmPass2JumpTable.olc0;
    }

    private void hf_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd7));
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
    }

    private void wf_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);

        if (al[0] >= 4) {
            error('#', 2, work.si);
        }
        m_seg.m_buf.set(work.di++, new MmlDatum(0xd9));
        m_seg.m_buf.set(work.di++, new MmlDatum(al[0] & 0xff));
    }

    private void pmd_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);

        dl[0] |= 0x80;

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd8));
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
    }

    private void amd_set() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);

        dl[0] &= 0x7f;

        m_seg.m_buf.set(work.di++, new MmlDatum(0xd8));
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
    }

//#endif

    /**
    // "<",">" の反転
     */
    private enmPass2JumpTable octrev() {
        if (comtbl[ou00].getItem1().equals(">")) {
            comtbl[ou00] = new Tuple<>("<", this::octup);
            comtbl[od00] = new Tuple<>(">", this::octdown);
        } else {
            comtbl[ou00] = new Tuple<>(">", this::octup);
            comtbl[od00] = new Tuple<>("<", this::octdown);
        }

        return enmPass2JumpTable.olc03;
    }

    /**
    // "^" ... Length Multiple
     */
    private enmPass2JumpTable lngmul() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al == 0) {
            error('^', 2, work.si);
        }

        if (mml_seg.skip_flag != 0) return enmPass2JumpTable.olc03;

        if ((mml_seg.prsok & 1) == 0) // 直前 = 音長?
        {
            error('^', 16, work.si);
        }

        if ((mml_seg.prsok & 2) != 0) // 加工されているか?
        {
            error('^', 31, work.si);
        }

        work.al--;
        if (work.al == 0) return enmPass2JumpTable.olc03;

        int cx = work.al; //cx = 足す回数

        work.al = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff); // al = 足される数
        work.ah = work.al; // ah = 足す数

        //lnml00:;
        do {
            if ((work.al & 0xff) + (work.ah & 0xff) > 0xff) {
                work.al += work.ah;
                lm_over();
            } else {
                work.al += work.ah;
            }
            //lnml01:;
            cx--;
        } while (cx > 0);

        m_seg.m_buf.set(work.di - 1, new MmlDatum(work.al & 0xff));

        return enmPass2JumpTable.olc03;
    }

    private void lm_over() {
        if ((mml_seg.prsok & 8) != 0) // ポルタ?
        {
            error('^', 8, work.si);
        }

        work.al++;

        m_seg.m_buf.set(work.di - 1, new MmlDatum(0xff));

        if (mml_seg.part == mml_seg.rhythm) // R?
        {
            //lmo_r:;
            m_seg.m_buf.set(work.di + 0, new MmlDatum(0x0f)); // 休符
            m_seg.m_buf.set(work.di + 1, new MmlDatum(0x00));
            work.di += 2;
            mml_seg.prsok |= 2; // 加工フラグ
            return; // break lnml01;
        }

        m_seg.m_buf.set(work.di, new MmlDatum(0xfb));
        byte bl = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
        m_seg.m_buf.set(work.di + 1, new MmlDatum(bl));
        m_seg.m_buf.set(work.di + 2, new MmlDatum(0));
        work.di += 3;

        mml_seg.prsok |= 2; // 加工フラグ
        return; // break lnml01;
    }

    /**
    // "="  ... Length Rewrite
    // 数値 ... Length Rewrite
     */
    private enmPass2JumpTable lngrew() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        if (mml_seg.skip_flag != 0) return lng_skip_ret();

        if ((mml_seg.prsok & 1) == 0) { // 直前=音長?
            error('=', 16, work.si); // = ?
        }

        if ((mml_seg.prsok & 2) != 0) { // 直前=加工音長?
            error('=', 31, work.si); // = ?
        }

        work.di--;

        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == '.') { // break not_futen_rew;

            work.al = (byte) (m_seg.m_buf.get(work.di).dat & 0xff);
            mml_seg.leng = work.al;
//            break futen_rew;
        } else {
//not_futen_rew:
            cy = lngset2(/* out */ bx, /* out */ al);

            if ((bx[0] & 0xff00) != 0) {
                error('=', 8, work.si);
            }

            lngcal();
        }
//futen_rew:
        cy = futen();

        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        if (!cy) return enmPass2JumpTable.olc03;

        if ((mml_seg.prsok & 8) == 0) return enmPass2JumpTable.olc03; // ポルタ?

        error('^', 8, work.si);

        return enmPass2JumpTable.exit; // dummy
    }

    private enmPass2JumpTable lngrew_2() {
        work.si--;
        return lngrew();
    }

    /**
    // "-"  ... Length 減算
     */
    private enmPass2JumpTable lng_dec() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        if (mml_seg.skip_flag != 0) return lng_skip_ret();
        if ((mml_seg.prsok & 1) == 0) // 直前=音長?
        {
            error('-', 16, work.si);
        }
        cy = lngset2(/* out */ bx, /* out */ al);
        if ((work.bx & 0xff00) != 0) {
            error('-', 8, work.si);
        }
        lngcal();
        cy = futen();
        if (cy) {
            error('-', 8, work.si);
        }
        byte d = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
        if (d <= work.al) {
            error('-', 8, work.si);
            // c or z=1
        }
        d -= work.al;
        m_seg.m_buf.set(work.di - 1, new MmlDatum(d & 0xff));
        return enmPass2JumpTable.olc03;
    }

    private enmPass2JumpTable lng_skip_ret() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        cy = lngset2(/* out */ bx, /* out */ al);
        if ((work.bx & 0xff00) != 0) {
            error('-', 8, work.si);
        }
        futen_skip();
        return enmPass2JumpTable.olc03;
    }

    /**
    // c ～ b の時
     */
    private enmPass2JumpTable otoc() {
        work.al = 0;
        work.ah = mml_seg.def_c;

        return otoset();
    }

    private enmPass2JumpTable otod() {
        work.al = 2;
        work.ah = mml_seg.def_d;

        return otoset();
    }

    private enmPass2JumpTable otoe() {
        work.al = 4;
        work.ah = mml_seg.def_e;

        return otoset();
    }

    private enmPass2JumpTable otof() {
        work.al = 5;
        work.ah = mml_seg.def_f;

        return otoset();
    }

    private enmPass2JumpTable otog() {
        work.al = 7;
        work.ah = mml_seg.def_g;

        return otoset();
    }

    private enmPass2JumpTable otoa() {
        work.al = 9;
        work.ah = mml_seg.def_a;

        return otoset();
    }

    private enmPass2JumpTable otob() {
        work.al = 0xb;
        work.ah = mml_seg.def_b;

        return otoset();
    }

    private enmPass2JumpTable otox() {
        work.al = 0xc;

        return otoset();
    }

    private enmPass2JumpTable otor() {
        work.al = 0xf;

        return rest();
    }

    private enmPass2JumpTable otoset() {
//#if !efc
        if (mml_seg.towns_flg != 1) { // break otoset_towns_chk; // TOWNSならKパートcheckは要らない
            if (mml_seg.part == mml_seg.rhythm2) {
                error(work.dx >> 8, 17, work.si); // K part = error
            }
        }
//otoset_towns_chk:
        if (mml_seg.part != mml_seg.rhythm)
            return ots000();

        /**
        // リズム（Ｒ）パートで音程が指定された＝［＠ｎ ｃ］に変換
         */
        int cx = mml_seg.lastprg;
        if (cx == 0) {
            error(work.dx >> 8, 30, work.si);
        }

        if (mml_seg.skip_flag != 0) return bp9();

        cx = (byte) (((cx & 0xff) * 0x100 + ((cx & 0xff00) >> 8)) & 0xff);

        m_seg.m_buf.set(work.di + 0, new MmlDatum(cx & 0xff)); //KUMA: @n
        m_seg.m_buf.set(work.di + 1, new MmlDatum((cx & 0xff00) >> 8)); //KUMA: c
        work.di += 2;

        mml_seg.length_check1 = 1; // 音長データがあったよ
        mml_seg.length_check2 = 1;
        mml_seg.prsok = 0; // prsok RESET
        return bp9();
//#endif
    }

    /**
    // =,+,- 判定
     */
    private enmPass2JumpTable ots000() {
        if (work.al == 0x0c) { // x?
            return otoset_x(); // なら素直にそのまま設定
        }

        byte bh = (byte) (mml_seg.octave & 0xff);
        byte bl;
        bh &= 0xf; // bh<-オクターブ
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        if (ch == '=') {
            //natural:;
            work.si++;
        } else if (work.ah != 0) {
            work.al += work.ah;
            //break bp3; // 範囲check //KUMA: doの中にjumpできない(ちょい無駄だけどそのまま突入する。。。)
        }

        //ots001:;
        do {
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch == '+') {
                work.al++;
                work.si++;
            }
            //bp2:;
            else if (ch == '-') // KUMA:ここもちょい無駄だけどループごとにひとつづつチェックさせる
            {
                work.al--;
                work.si++;
            }

            /**
            // c- は 1oct 下へ, b+ は 1oct 上へ
             */

            //bp3:;
            work.al &= 0xf;
            bl = work.al;
            if (bl == 0xf) {
                bh--;
                if (bh == (byte) 0xff) {
                    error(work.dx >> 8, 26, work.si);
                }
                bl = 0xb;
            }
            //bp4:;
            if (bl == 0xc) {
                bh++;
                if (bh == 0x8) {
                    error(work.dx >> 8, 26, work.si);
                }
                bl = 0;
            }
            //bp5:;
            work.al = bl;
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        } while (ch == '+' || ch == '-');

        /**
        // 音階データをblにセット
         */
        bh = (byte) ((bh & 0x0f) << 4);
        bl |= bh; // bl=音階 DATA //KUMA: 上位4bit:オクターブ  下位4bit:音階
        work.bx = (bh & 0xff) * 0x100 + (bl & 0xff);
        mml_seg.ontei = work.al;

        return enmPass2JumpTable.ots002;
    }

    private enmPass2JumpTable ots002() {
        if (mml_seg.bend == 0) return bp8();

        // PITCH/DETUNE SET
        int bx_p = work.bx;
        work.al = (byte) (work.bx & 0xff);
        work.bx = 0;
        if (mml_seg.pitch != 0) { // break bp6;

//#if !efc
            if (mml_seg.ongen >= mml_seg.psg) { // break fmpt;

                work.bx = mml_seg.pitch >> 7; // PSG/PCMの時はPITCHを128で割る
                if ((work.bx & 0x8000) != 0) { // break bp6;
                    work.bx++;
                }
//            break bp6;
            } else {
//#endif

//fmpt:
                work.al &= 0xf;
                int ax = work.al << 5;
                work.dx = ax; // DX = PITCHを掛けない状態のFnum値のある番地

                //int dx_p = Work.dx; //KUMA:不要(x86ではidiv imulするとdxに影響がある)
                work.dx = 0;
                ax = 32;
                work.bx = (byte) (mml_seg.bend & 0xff);
                ax *= work.bx;
                work.bx = mml_seg.pitch;
                ax *= work.bx;
                work.bx = 8192;
                ax /= work.bx; // AX = PITCHでずらす番地 / 2
                //Work.dx = dx_p; //KUMA:不要

                work.bx = work.dx;
                work.dx = fnumTbl[work.bx]; // DX = PITCHを掛けない状態の Fnum値
                ax *= 2;
                work.bx += ax; // BX = PITCHを掛けた後のFnum値のある番地

//bp50:
                while (true) {
                    if (work.bx < 0) { // break bp51; // オクターブを下回ったか？
                        work.bx += 32 * 12 * 2;
                        work.dx += 0x26a;
                        continue; // break bp50;
                    }
//bp51:
                    if (work.bx < 32 * 12 * 2) break; // bp52; // オクターブを上回ったか？
                    work.bx -= 32 * 12 * 2;
                    work.dx -= 0x26a;
                    // break bp50; // KUMA:bp51のほうが無駄がないような気がする
                }
//bp52:
                work.dx -= fnumTbl[work.bx];
                work.bx = work.dx;
                work.bx = -work.bx; // BX = PITCHをDETUNEに換算した値
            }
        }
//bp6:
        work.bx += mml_seg.detune;
        work.bx += mml_seg.master_detune;
        if (work.bx != mml_seg.alldet) { // break bp7;
            if (mml_seg.porta_flag != 1) { // break porta_pitchset;
                work.al = (byte) 0xfa;

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));

//bp6b:
                mml_seg.alldet = work.bx;
            }
        } else {
//bp7:
            work.bx = bx_p;
            return bp8();
        }
//porta_pitchset:
        if (m_seg.m_buf.get(work.di - 1).dat != 0xda) {
            error(0, 14, work.si);
        }
        work.di--;
        work.al = (byte) 0xfa;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
        m_seg.m_buf.set(work.di++, new MmlDatum(0xda));

//        break bp6b;
        mml_seg.alldet = work.bx; // <<

        work.bx = bx_p; // <<
        return bp8(); // <<
    }

    /**
    // REST 用 entry
     */
    private enmPass2JumpTable rest() {
//#if !efc

        if (mml_seg.towns_flg != 1) { // TOWNSならKパートcheckは要らない
            if (mml_seg.part == mml_seg.rhythm2) {
                error('r', 17, work.si); // K part = error
            }
        }
        //rest_towns_chk:;
//#endif
        return otoset_x();
    }

    private enmPass2JumpTable otoset_x() {
        work.bx = (work.bx & 0xff00) + work.al;

        mml_seg.ontei = work.al;

        return bp8();
    }

    /**
    // 音階 DATA SET
     */
    private enmPass2JumpTable bp8() {
        if (mml_seg.skip_flag != 0) { // break bp8b;
            if (mml_seg.acc_adr == work.di) {
                //直前が(^ )^命令なら
                work.di -= 2; // それを削除
                mml_seg.acc_adr = 0;
            }
            //bp8a:;
            int[] bx = new int[1];
            byte[] al = new byte[1];
            boolean cy = lngset2(/* out */ bx, /* out */ al);
            work.bx = bx[0];
            work.al = al[0];
            if ((bx[0] & 0xff00) != 0) {
                error(0, 8, work.si);
            }

            futen_skip();

            return bp10();
        }
//bp8b:
        mml_seg.length_check1 = 1; // 音長データがあった
        mml_seg.length_check2 = 1;

        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));

        return bp9();
    }

    /**
    // 音長計算
     */
    private enmPass2JumpTable bp9() {
        MMLType mt = MMLType.Note;
        if (work.al == 0xf) {
            mt = MMLType.Rest;
        }

        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset2(/* out */ bx, /* out */ al);
        work.bx = bx[0];
        work.al = al[0];

        if ((bx[0] & 0xff00) != 0) {
            error(0, 8, work.si);
        }

        lngcal();

        mml_seg.prsok &= 0xfd; //加工音長フラグreset
        futen();

        if (!work.isIDE)
            press();

        /**
        // 音長 DATA SET
         */

        work.al = (byte) (mml_seg.leng & 0xff);

        LinePos lp = MakeLinePos();
        MmlDatum dmy = m_seg.m_buf.get(work.di - 1);

        List<Object> args = new ArrayList<>();
        if (!lp.part.equals("Rhythm")) {
            args.add((mml_seg.octave << 4) | mml_seg.ontei);
        } else {
            //if (mt == MMLType.Note)
            args.add(mml_seg.lastprg); // K partの場合は音色番号をセット
            //else
            //args.add(-1);
        }
        args.add(mml_seg.leng);

        if (dmy.args != null && dmy.args.size() > 2 && dmy.args.get(2) != null) {
            args.add(dmy.args.get(2));
        } else {
            args.add(!m_seg.getMacroLst().isEmpty() ? m_seg.getMacroLst().toArray(MmlDatum[]::new) : null);
            m_seg.getMacroLst().clear();
        }

        dmy.type = MMLType.Note;

        dmy.args = args;
        dmy.linePos = lp;


        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        //MSeg.m_buf.set(Work.di++, new MmlDatum(Work.al & 0xff));
        mml_seg.prsok |= 1; // 音長flagをset
        mml_seg.prsok &= 0xf3; // 音長+タイ,ポルタflagをreset

        work.al = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
        work.al &= 0xf;
        if (work.al == 0xf) return bp10(); // 休符
        if (mml_seg.tie_flag != 0) return bp10();
        if (mml_seg.porta_flag != 0) return bp10();

        mml_seg.ge_flag1 = 0;
        mml_seg.ge_flag2 = 0;

        if (mml_seg.ge_delay != 0) {
            ge_set(); // 擬似エコーセット＋装飾音符設定
            return bp10();
        }
        //bp9_2:;

        if (mml_seg.ss_length == 0) return bp10();

        ss_set(); // 装飾音符設定
        return bp10();
    }

    private LinePos MakeLinePos() {
        int p = work.si - 1;
        while (mml_seg.mml_buf.charAt(p) == ' ' || mml_seg.mml_buf.charAt(p) == '\t') p--;
        p++;

        return new LinePos(
                mml_seg.currentDocument,
                mml_seg.currentMMLFile // .mml_filename
                , Math.max(mml_seg.line, 1)
                , Math.max(mml_seg.stPos - mml_seg.linehead + 1, 1)
                , p - mml_seg.stPos
                , mml_seg.ongen == mml_seg.pcm_ex
                ? "PPZ8"
                : (mml_seg.chipCh < 6
                ? "FMOPN"
                : (mml_seg.chipCh >= 6 && mml_seg.chipCh <= 8
                ? "FMOPNex"
                : (mml_seg.chipCh >= 9 && mml_seg.chipCh <= 11
                ? "SSG"
                : (mml_seg.chipCh == 18
                ? "ADPCM"
                : "Rhythm"
        )
        )
        )
        )
                , mml_seg.ongen != mml_seg.pcm_ex ? "YM2608" : "PPZ8"
                , 0
                , 0
                , mml_seg.chipCh - (mml_seg.chipCh < 20 ? 0 : 20)
        );
    }

    private enmPass2JumpTable bp10() {
        mml_seg.tie_flag = 0;
        return enmPass2JumpTable.olc02;
    }

    /**
    // 擬似エコーのセット
     */
    private void ge_set() {
        mml_seg.ge_depth = mml_seg.ge_depth2;
        //ge_loop:;
        do {
            work.al = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
            if (work.al - (mml_seg.ge_delay & 0xff) <= 0) {
                work.al -= (byte) (mml_seg.ge_delay & 0xff);
                break; // 長さが足りない(cf or zf= 1)
            }
            work.al -= (byte) (mml_seg.ge_delay & 0xff);
            byte dh = work.al; // dh=length-delay
            byte dl = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff); // dl=onkai
            work.dx = (dh & 0xff) * 0x100 + (dl & 0xff);

            work.al = (byte) (mml_seg.ge_delay & 0xff);
            m_seg.m_buf.set(work.di - 1, new MmlDatum(work.al & 0xff));

            if (mml_seg.ss_length != 0) { // break ge_ss1;
                work.al = (byte) (mml_seg.ss_length & 0xff); // 長さが足りない
                byte d = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
                if ((work.al & 0xff) - (d & 0xff) >= 0) {
                    work.al -= d;
//                    break ge_ss1;
                } else {
                    work.al -= d;
                    work.al = (byte) (mml_seg.ge_flag1 & 0xff); // (^の重複を避ける
                    if (work.al != 0) { // break no_dec_di;

                        d = (byte) (m_seg.m_buf.get(work.di - 4).dat & 0xff);
                        if (work.al == d) { // break no_dec_di;

                            work.al = (byte) (mml_seg.ge_flag2 & 0xff);
                            d = (byte) (m_seg.m_buf.get(work.di - 3).dat & 0xff);
                            if (work.al == d) { // break no_dec_di;

                                int ax = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
                                ax += (byte) m_seg.m_buf.get(work.di - 1).dat * 0x100;
                                m_seg.m_buf.set(work.di - 4, new MmlDatum(ax & 0xff));
                                m_seg.m_buf.set(work.di - 3, new MmlDatum((ax & 0xff00) >> 8));
                                work.di -= 2;
                            }
                        }
                    }
//no_dec_di:
                    int dx_p = work.dx;
                    ss_set();
                    work.dx = dx_p;
                }
            }
//ge_ss1:
            if ((mml_seg.ge_tie & 1) != 0) { // break ge_not_tie;
                m_seg.m_buf.set(work.di++, new MmlDatum(0xfb)); // "&"
            }
//ge_not_tie:
            ge_set_vol();
            m_seg.m_buf.set(work.di + 0, new MmlDatum(work.dx & 0xff));
            m_seg.m_buf.set(work.di + 1, new MmlDatum((work.dx & 0xff00) >> 8));
            work.di += 2;
            mml_seg.prsok |= 2; // 直前byte = 加工された音長
            if ((mml_seg.ge_tie & 2) != 0) break;
        } while (true);

//ge_set_ret:

        if (mml_seg.ss_length != 0) {
            ss_set();
        }
//ge_ss2:
    }

    private void ge_set_vol() {
        work.al = (byte) mml_seg.ge_depth;
        if ((work.al & 0x80) == 0) { // break ge_minus;
            if (work.al == 0) {
//                break gen_00;
                mml_seg.ge_depth = work.al; // <<
                return; // <<
            }

            /**
            // 音量が上がる
             */
            if (mml_seg.ge_dep_flag != 1) { // break gen_no_sel_vol;
                ongen_sel_vol();
            }
//gen_no_sel_vol:
            if (work.al != 0) { // break gen_not_set; // 0?

                m_seg.m_buf.set(work.di, new MmlDatum(0xde));
                work.di++;
                mml_seg.ge_flag1 = 0xde;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                mml_seg.ge_flag2 = work.al;
            }
//gen_not_set:
            work.al = (byte) mml_seg.ge_depth;
            work.al += (byte) mml_seg.ge_depth2;
            if (mml_seg.ge_dep_flag != 1) { // break gen_01;
                if (work.al >= 16) { // break gen_00;
                    work.al = 15;
                }
//gen_00:
                mml_seg.ge_depth = work.al;
                return;
            }
//gen_01:
            if ((work.al & 0x80) == 0) {
//                break gen_00;
                mml_seg.ge_depth = work.al; // <<
                return; // <<
            }
            mml_seg.ge_depth = 127; //    mov[ge_depth],+127
            return;
        } else {
            /**
            // 音量が下がる
             */
//ge_minus:
            work.al = (byte) -work.al;
            if (mml_seg.ge_dep_flag != 1) { // break gem_no_sel_vol;
                ongen_sel_vol();
            }
//gem_no_sel_vol:
            if (work.al != 0) { // break gem_not_set; // 0?

                m_seg.m_buf.set(work.di, new MmlDatum(0xdd));
                work.di++;
                mml_seg.ge_flag1 = 0xdd;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                mml_seg.ge_flag2 = work.al;
            }
//gem_not_set:
            work.al = (byte) mml_seg.ge_depth;
            work.al += (byte) mml_seg.ge_depth2;
            if (mml_seg.ge_dep_flag != 1) { // break gem_01;

                if (work.al < -15) { // break gem_00;
                    work.al = (byte) (256 - 15);
                }
//gem_00:
                mml_seg.ge_depth = work.al;
                return;
            }
        }
//gem_01:
        if ((work.al & 0x80) != 0) {
//            break gem_00;
            mml_seg.ge_depth = work.al; // <<
            return; // <<
        }
        mml_seg.ge_depth = -127; //    mov[ge_depth],-127
    }

    /**
    // 各音源によって音量の増減を変える
     */
    private void ongen_sel_vol() {
//#if !efc
        if (mml_seg.part != mml_seg.pcmpart) { // break sel_pcm;
            if (mml_seg.ongen != mml_seg.pcm_ex) { // break sel_pcm;
                if (mml_seg.towns_flg != 1) {
                    if (mml_seg.part != mml_seg.rhythm2) { // break sel_pcm;
                    }
                } else {
                    //osv_no_towns:;
                    if (mml_seg.ongen == mml_seg.psg) { // break sel_fm;
                        return;
                    }
//sel_fm:

//#endif
                    work.al *= 4;
                    return;
                }
            }
        }
//#if !efc
//sel_pcm:
        work.al *= 16;
//#endif
    }

    /**
    // 装飾音符のセット
     */
    private void ss_set() {
        work.al = (byte) mml_seg.ss_length;
        byte d = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
        if (work.al - d >= 0) {
            work.al -= d;
            return; // break ss_set_ret; // 長さが足りない
        }
        work.al -= d;

        d = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
        if (d == 0x0c) // x?
        {
            return; // break ss_set_ret; // なら装飾しない
        }

        work.di -= 2;

        work.dx = (byte) (m_seg.m_buf.get(work.di + 0).dat & 0xff);
        work.dx += (byte) m_seg.m_buf.get(work.di + 1).dat * 0x100;

        work.dx = ((byte) (work.dx >> 8) | (work.dx << 8)) & 0xffff; // Dh=Onkai/Dl=Length
        work.al = (byte) mml_seg.ss_depth;
        if ((work.al & 0x80) != 0) { // break ss_plus;

            /**
            // 下から上がる
             */
            work.al = (byte) -work.al;
            int cx = work.al;            // cx = Depth
            work.bx = (work.dx & 0xff00) | (byte) work.bx; // bh = Onkai(for Move)
//ss_minus_loop:;
            do {
                one_down();
                cx--;
            } while (cx > 0);
//ss_minus_loop2:
            do {
                if (mml_seg.ge_flag1 != 0) { // break ssm_non_ge;
                    work.al = (byte) mml_seg.ge_flag1;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                    work.al = (byte) mml_seg.ge_flag2;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                }
//ssm_non_ge:
                work.al = (byte) mml_seg.ss_speed;
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                if (mml_seg.ss_tie != 0) { // break ssm_not_tie;
                    m_seg.m_buf.set(work.di++, new MmlDatum(0xfb)); // "&"
                }
//ssm_not_tie:
                one_up();
            } while ((work.bx & 0xff00) != (work.dx & 0xff00)); // break ss_minus_loop2;
//            break ss_fin;
        } else {
            /**
            // 上から下がる
             */
//ss_plus:
            int cx = work.al; // cx = Depth
            work.bx = (work.dx & 0xff00) | (byte) work.bx; // bh = Onkai(for Move)
            //ss_plus_loop:;
            do {
                one_up();
                cx--;
            } while (cx > 0);
//ss_plus_loop2:
            do {
                if (mml_seg.ge_flag1 != 0) { // break ssp_non_ge;
                    work.al = (byte) mml_seg.ge_flag1;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                    work.al = (byte) mml_seg.ge_flag2;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                }
//ssp_non_ge:
                work.al = (byte) mml_seg.ss_speed;
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                if (mml_seg.ss_tie != 0) { // break ssp_not_tie;
                    m_seg.m_buf.set(work.di++, new MmlDatum(0xfb)); // "&"
                }
//ssp_not_tie:
                one_down();
            } while ((work.bx & 0xff00) != (work.dx & 0xff00)); // break ss_plus_loop2;
        }
        /**
        // 最後の音符を書き込む
         */
//ss_fin:
        if (mml_seg.ge_flag1 != 0) { // break ssf_non_ge;
            work.al = (byte) mml_seg.ge_flag1;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            work.al = (byte) mml_seg.ge_flag2;
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        }
//ssf_non_ge:
        m_seg.m_buf.set(work.di++, new MmlDatum((work.dx & 0xff00) >> 8));
        byte dl = (byte) work.dx;
        dl -= (byte) mml_seg.ss_length;
        m_seg.m_buf.set(work.di++, new MmlDatum(dl & 0xff));
        work.dx = (work.dx & 0xff00) | dl;
        mml_seg.prsok |= 2; // 直前byte = 加工された音長
//ss_set_ret:
    }

    /**
    // 音階を一つ下げる
    //  input/output bh to Onkai
     */
    private void one_down() {
        byte bh = (byte) (work.bx >> 8);
        bh--;
        work.al = bh;
        work.al &= 0xf;
        if (work.al != 0xf) {
            work.bx = (bh << 8) | (byte) work.bx;
            return;
        }
        bh &= 0xf0;
        bh |= 0xb;
        if ((bh & 0x80) != 0) {
            error('S', 26, work.si);
        }
        work.bx = (bh << 8) | (byte) work.bx;
        //one_down_ret:;
    }

    /**
    // 音階を一つ上げる
    //  input/output bh to Onkai
     */
    private void one_up() {
        byte bh = (byte) (work.bx >> 8);
        bh++;
        work.al = bh;
        work.al &= 0xf;
        if (work.al != 0xc) {
            work.bx = (bh << 8) | (byte) work.bx;
            return;
        }
        bh &= 0xf0;
        bh += 0x10;

        if ((bh & 0x80) != 0) {
            error('S', 26, work.si);
        }
        work.bx = (bh << 8) | (byte) work.bx;
        //one_up_ret:;
    }

    /**
    // 前も同じ音符で、しかも"&"で繋がっていた場合は、圧縮する処理
     */
    private void press() {
        byte d;
prs3: // ↑
        {
restprs: // ↑
            {
prs200: // ↑
                {
                    if ((mml_seg.prsok & 0x80) != 0) return; //直前 = Rhythm？
                    if ((mml_seg.prsok & 0x08) != 0) return; //直前 = ポルタ？
                    if (mml_seg.skip_flag != 0) return;
                    if ((mml_seg.prsok & 0x04) == 0) //直前 = +タイ？
                    {
                        d = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
                        if (d != 0xf) return;
                        if ((mml_seg.prsok & 0x01) != 0) break restprs; //直前 = 音長？
                        return;
                    }
//press_main:
//#if !efc
                    if (mml_seg.part == mml_seg.rhythm) break prs3; // リズムパートで圧縮可能＝無条件に圧縮
//#endif

//prs0:
                    byte ah = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
                    d = (byte) (m_seg.m_buf.get(work.di - 4).dat & 0xff);
                    if (ah != d) return;
//prs1:
                    if (work.di - 1 == skipIndex) {
                        skipIndex -= 3;
                    }
                    work.di -= 3;

                    mml_seg.prsok |= 2; // 加工したflag

                    d = (byte) (m_seg.m_buf.get(work.di).dat & 0xff);
                    if ((work.al & 0xff) + d <= 255) {
                        work.al += d;
                        break prs200;
                    }
                    work.al += d;

                    m_seg.m_buf.set(work.di, new MmlDatum(255));
                    work.al++; // 255 over

                    if (work.di - 1 == skipIndex) {
                        skipIndex += 3;
                    }
                    work.di += 3;

                    if (ah != 0xf) break prs200;

                    if (work.di - 1 == skipIndex) {
                        skipIndex--;
                    }
                    work.di--;

                    m_seg.m_buf.set(work.di - 1, new MmlDatum(ah & 0xff)); // r&r -> rr に変更
                }
//prs200:
                mml_seg.leng = work.al;
                return;
            }
//restprs:
//#if !efc
            if (mml_seg.part != mml_seg.rhythm) { // break prs3;
//#endif

                d = (byte) (m_seg.m_buf.get(work.di - 3).dat & 0xff);
                if (d != 0xf) return;
            }
        }
//prs3:

        if (work.di - 1 == skipIndex) {
            skipIndex -= 2;
        }
        work.di -= 2;

        mml_seg.prsok |= 2; // 加工したflag
        d = (byte) (m_seg.m_buf.get(work.di).dat & 0xff);
        if ((work.al & 0xff) + d <= 255) {
            work.al += d;
//            break prs200;
            mml_seg.leng = work.al; // <<
            return; // <<
        }
        work.al += d;

        MmlDatum md = new MmlDatum(255);
        m_seg.m_buf.set(work.di, md);
        work.al++; // 255 over

        if (work.di - 1 == skipIndex) {
            skipIndex += 2;
        }
        work.di += 2;
//        break prs200;
        mml_seg.leng = work.al; // <<
        return; // <<
    }

    /**
    // 数値の読み出し（書かれていない時は１）
    //  output bx/al/[leng]
     */
    private boolean lngset(/* out */ int[] bx, /* out */ byte[] al) {
        if (!lngset2(/* out */ bx, /* out */ al)) return false;

        bx[0] = 1;
        //lnexit相当
        al[0] = (byte) 1;
        work.bx = 1;
        work.al = (byte) 1;
        mml_seg.leng = 1;
        return true;
    }

    /**
    // [si] から数値を読み出す
    // 数字が書かれていない場合は[deflng] の値が返り、cy=1になる
    //  output al/bx/[leng]
     */
    private boolean lngset2(/* out */ int[] bx, /* out */ byte[] al) {
        char ch;
        bx[0] = 0;
        boolean cy = false;

        do {
            ch = mml_seg.mml_buf.charAt(work.si++);
        } while (ch == ' ' || ch == '\t');

        mml_seg.calflg = 0;
        if (ch == '%') {
            mml_seg.calflg = 1;
            ch = mml_seg.mml_buf.charAt(work.si++);
        }

//lgs00:;
        if (ch != '$') {
            // 10進の場合
            work.si--;
            cy = numget(/* out */ al);
//lgs02:;
            bx[0] = al[0] & 0xff;
            if (cy) {
//lgs03:;
                bx[0] = mml_seg.deflng & 0xff;
                mml_seg.calflg = 1;
                al[0] = (byte) bx[0];
                mml_seg.leng = al[0];
                work.al = al[0];
                work.bx = bx[0];
                return true;
            }
//lng1:;
            do {
                cy = numget(/* out */ al); // A=NUMBER
                if (cy) {
//lnexit:;
                    al[0] = (byte) bx[0];
                    mml_seg.leng = al[0];
//lngset_ret:
                    work.al = al[0];
                    work.bx = bx[0];
                    return false;
                }
//lng2:;
                bx[0] *= 10;
                bx[0] += al[0];
            } while (true);
        }

        // 16進の場合
//lgs01:;
        cy = hexget8(/* out */ al);
        bx[0] = (bx[0] & 0xff00) + (int) al[0];
        if (cy) {
            bx[0] = mml_seg.deflng & 0xff;
            mml_seg.calflg = 1;
        }

        al[0] = (byte) bx[0];
        mml_seg.leng = al[0];
        work.al = al[0];
        work.bx = bx[0];
        return cy;
    }

    private boolean hexget8(/* out */ byte[] al_b) {
        al_b[0] = (byte) mml_seg.mml_buf.charAt(work.si++);
        if (!hexcal8(/* ref */ al_b)) return true; // ERROR RETURN

        byte bl = al_b[0];
        al_b[0] = (byte) mml_seg.mml_buf.charAt(work.si++);

        if (hexcal8(/* ref */ al_b)) {
            al_b[0] += (byte) (bl * 16);
            return false;
        }

        work.si--;
        al_b[0] = bl;
        return false;
    }

    private boolean hexcal8(/* ref */ byte[] al_b) {
        try {
            al_b[0] = Byte.parseByte(String.valueOf((char) al_b[0]));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
    // 符点(.)があるかを見て、あれば[leng] を1.5倍する。
    // 符点が２個以上あっても可
    //  output al/bl/[leng]
     */
    private boolean futen() {
        char ch;

        work.al = (byte) mml_seg.leng;
        int ax = work.al;
        work.bx = ax;
//ftloop:
        do {
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
            if (ch != '.') break;

            if (work.bx % 2 != 0) {
                error('.', 21, work.si);
            }
            work.bx /= 2;
            ax += work.bx;
            work.si++;
        } while (true);
//ft0:
        if ((ax & 0xff00) == 0) { // break ft1; // 音長 255 OVER
            work.bx = ax;
            work.al = (byte) ax;
            mml_seg.leng = (byte) ax;
            //    clc
            return false;
        }
//ft1:
        while (true) {
            if (mml_seg.ge_delay != 0) error('.', 20, work.si); // Wコマンド使用中はError
            if (mml_seg.ss_length != 0) error('.', 20, work.si); // Sコマンド使用中もError

//#if !efc
            if (mml_seg.part == mml_seg.rhythm) { // break ft1_r;
//#endif
//ft1_r:
                m_seg.m_buf.set(work.di + 0, new MmlDatum(255)); // 音長255＋休符を設定
                m_seg.m_buf.set(work.di + 1, new MmlDatum(0x0f));
                work.di += 2;
//                break ft2;
            } else {
                m_seg.m_buf.set(work.di + 0, new MmlDatum(255)); // 音長255＋タイを設定
                m_seg.m_buf.set(work.di + 1, new MmlDatum(0xfb));
                work.di += 2;
                byte bl = (byte) (m_seg.m_buf.get(work.di - 3).dat & 0xff);
                m_seg.m_buf.set(work.di, new MmlDatum(bl & 0xff)); // 音符
                work.di++;
            }
//ft2:
            ax -= 255;
            if ((ax & 0xff00) == 0) { // break ft1; // 音長 255 OVER

                work.bx = ax;
                mml_seg.leng = (byte) ax;
                work.al = (byte) ax;
                mml_seg.prsok |= 2; // 直前＝加工された音長
                //  stc
                return true;
            }
        }
    }

    private void futen_skip() {
        char ch;
        do {
            ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        } while (ch == '.');

        work.si--;
    }

    /**
    // 0 ～ 9 の数値を得る
    //  inputs -- ds:si to mml pointer
    // outputs -- al
    //   -- cy[1 = error]
     */
    private boolean numget(/* out */ byte[] al) {
        char ch = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        al[0] = (byte) (ch - '0');
        if (ch < '0') {
            work.si--;
            return true;
        }

        if (al[0] >= 10) {
            work.si--;
            return true;
        }

        return false;
    }

    /**
    // COMMAND "o" オクターブの設定
     */
    private Mc.enmPass2JumpTable octset() {
        char alc = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (alc == '+' || alc == '-') {
            octss_set((byte) alc);
            return enmPass2JumpTable.olc03;
        }

        int[] bx = new int[1];
        byte[] al = new byte[1];
        if (lngset(/* out */ bx, /* out */ al))
        {
            error(work.dx >> 8, 6, work.si);
        }

        al[0]--;
        al[0] += (byte) mml_seg.octss;
        octs0(al[0]);

        return enmPass2JumpTable.olc03;
    }

    private void octs0(byte al) {
        if (al >= 8) {
            error(work.dx >> 8, 26, work.si);
        }
        mml_seg.octave = al;

        //   jmp olc03
    }

    private void octss_set(byte al) {
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        getnum(/* out */ bx, /* out */ dl);
        mml_seg.octss = dl[0];

        al = (byte) mml_seg.octave;
        al += dl[0];
        octs0(al);
    }

    /**
    // COMMAND ">","<" オクターブup/down
     */
    private enmPass2JumpTable octup() {
        byte al = (byte) (mml_seg.octave + 1);
        octs0(al);

        return enmPass2JumpTable.olc03;
    }

    private enmPass2JumpTable octdown() {
        byte al = (byte) (mml_seg.octave - 1);
        octs0(al);
        return enmPass2JumpTable.olc03;
    }

    /**
    // COMMAND "l" デフォルト音長の設定
    // COMMAND "l=" 直前の音長の変更
    // COMMAND "l-" 直前の音長の減算
    // COMMAND "l^" 直前の音長の乗算
     */
    private enmPass2JumpTable lengthset() {
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al == (byte) '=') return enmPass2JumpTable.lngrew;
        if (work.al == (byte) '-') return enmPass2JumpTable.lng_dec;
        if (work.al == (byte) '+') return enmPass2JumpTable.tieset_2;
        if (work.al == (byte) '^') return enmPass2JumpTable.lngmul;

        work.si--;

        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset2(/* out */ bx, /* out */ al);
        work.bx = bx[0];
        work.al = al[0];
        if (cy) {
            error('l', 6, work.si);
        }

        if ((work.bx & 0xff00) != 0) {
            error(work.dx >> 8, 8, work.si);
        }

        lngcal();
        cy = futen();
        if (cy) {
            error('l', 8, work.si);
        }
        mml_seg.deflng = work.al;

        return enmPass2JumpTable.olc03;
    }

    /**
    // COMMAND "C" 全音符の長さを設定
     */
    private enmPass2JumpTable zenlenset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);

        mml_seg.zenlen = work.al;
        mml_seg.deflng = (byte) (work.al >> 2);
        return syousetu_lng_set_2();
    }

    /**
    // 音長から具体的な長さを得る
    //  INPUTS -- [leng]
    //        to 音長
    //   -- [zenlen]
    //        to 全音符の長さ
    // OUTPUTS -- al,[leng]
     */
    private void lngcal() {
        work.al = (byte) mml_seg.leng;
        if (work.al == 0) {
            error(0, 21, work.si); // LENGTH=0 ... ERROR
        }

        if (mml_seg.calflg != 0) return;

        //lcl001:;
        work.al = (byte) mml_seg.zenlen;
        int ax = work.al;

        int d = ax / mml_seg.leng;
        if (ax % mml_seg.leng != 0) {
            error(0, 21, work.si); // 音長が全音符の公約数でない
        }
        mml_seg.leng = (byte) d;
    }

    /**
    // 2byte[dh / dl] の dataをセットして戻る
     */
    private enmPass2JumpTable parset() {
        byte b = (byte) (work.dx & 0xff);
        work.dx = ((work.dx & 0xff00) >> 8) + (b & 0xff) * 0x100;

        MmlDatum cmd;

        if (work.ctype == MMLType.unknown) {
            cmd = new MmlDatum(work.dx & 0xff);
        } else {
            cmd = new MmlDatum(work.dx & 0xff, work.ctype, MakeLinePos(), work.cargs);
            work.ctype = MMLType.unknown;
            work.cargs = null;
        }

        m_seg.m_buf.set(work.di + 0, cmd);
        m_seg.m_buf.set(work.di + 1, new MmlDatum((work.dx & 0xff00) >> 8));
        work.di += 2;

        return enmPass2JumpTable.olc0;
    }

    //5357-5388
    /**
    // COMMAND "t" / "T" テンポ／TimerBセット
     */
    private enmPass2JumpTable tempoa() {
//#if efc
        error('t', 12, work.si); // 効果音emlにはテンポは指定出来ない
//#else
        char alc = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        byte ah = (byte) 0xfd; // t±

        if (alc == '+') return tempo_ss(ah);
        if (alc == '-') return tempo_ss(ah);
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
        if (cy || al[0] < 18) {
            error('t', 2, work.si);
        }

//#if !tempo_old_flag

        m_seg.m_buf.set(work.di++, new MmlDatum(0xfc)); // t
        m_seg.m_buf.set(work.di++, new MmlDatum(0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
        return enmPass2JumpTable.olc0;
//#else
        // call timerb_get
        //    mov al, dl
        //    jmp tset
//#endif
//#endif
    }

    /**
    // "T" Command Entry
     */
    private enmPass2JumpTable tempob() {
//#if efc
        error('T', 12, work.si); // 効果音emlにはテンポは指定出来ない
//#else
        char alc = (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a);
        byte ah = (byte) 0xfe; // T±
        if (alc == '+') return tempo_ss(ah);
        if (alc == '-') return tempo_ss(ah);
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
        //tset:;
        if (cy || (al[0] & 0xff) >= 251) // 251～255はエラー
        {
            error('T', 2, work.si); // KUMA: t -> T
        }

        work.dx = 0xfc00 + al[0];
        return enmPass2JumpTable.parset;
//#endif
    }

    private enmPass2JumpTable tempo_ss(byte ah) {
//#if !efc
        m_seg.m_buf.set(work.di++, new MmlDatum(0xfc));
        m_seg.m_buf.set(work.di++, new MmlDatum(ah & 0xff));
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        boolean cy = getnum(/* out */ bx, /* out */ dl);
        work.al = dl[0];
        if (work.al != 0) {
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            return enmPass2JumpTable.olc0;
        }
        //tss_non:;  ; t±0 T±0の時は無視
        work.di -= 2;
        return enmPass2JumpTable.olc02;
//#endif
    }

    /**
    // タイマＢの数値 を 計算
    //  INPUTS --  AL = TEMPO
    //  OUTPUTS -- DL = タイマＢの数値
    //
    // DL = 256 - [ 112CH / TEMPO]
     */
    private void timerb_get(byte al, /* out */ byte[] dl) {
        dl[0] = 0;
        if (tempo_old_flag != 0) {
            //timerb_get:
            byte bl = al;

            int ax = 0x112c;
            al = (byte) (ax / bl);
            byte ah = (byte) (ax % bl);
            dl[0] = (byte) (0x100 - al);

            if ((ah & 0xff) > 127) {
                dl[0]--; // 四捨五入
            }
        }
    }

    //5450-5473
    /**
    // clock値またはlength値を読み取る
    //  input dh  command name
    // output al  clock
     */
    private void get_clock() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];

        char alc = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (alc == 'l') { // break gcl_no_length;
            work.si++;

            int dx_p = work.dx;

            cy = lngset2(/* out */ bx, /* out */ al);
            if ((work.bx & 0xff00) != 0) {
                error((char) (byte) (work.dx >> 8), 8, work.si);
            }

            lngcal();
            cy = futen();
            if (cy) {
                work.dx = dx_p;
                error((char) (byte) (work.dx >> 8), 8, work.si);
            }

            return;
        }
//gcl_no_length:
        cy = lngset(/* out */ bx, /* out */ al);
    }

    /**
    // COMMAND "q" step-gate time change
     */
    private enmPass2JumpTable qset() {
        char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (al != ',') { // break qsetb;

            work.dx = 'q' * 0x100 + (work.dx & 0xff);
            get_clock();

            //Work.dx = Work.al * 0x100 + (byte)0xfe;
            //MSeg.m_buf.set(Work.di++, new MmlDatum(Work.dx & 0xff));
            //MSeg.m_buf.set(Work.di++, new MmlDatum((Work.dx & 0xff00) >> 8)));
            work.ctype = MMLType.Gatetime;
            work.cargs = new Object[] {(int) work.al};
            work.dx = 0xfe00 + work.al;
            parset();

            al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (al == '-') { // break qseta;

                work.si++;

                int dx_p = work.dx;

                work.dx = 'q' * 0x100 + (work.dx & 0xff);
                get_clock();
                work.dx = (work.al & 0xff) * 0x100 + 0xb1;

                int ax = dx_p;

                byte dh = (byte) (work.dx >> 8);
                byte ah = (byte) (ax >> 8);

                work.dx = ((dh - ah) & 0xff) * 0x100 + (work.dx & 0xff);
                if (dh - ah < 0) { // break qrnd_set;
                    dh = (byte) -(work.dx >> 8);
                    dh |= (byte) 0x80;
                    work.dx = (dh & 0xff) * 0x100 + (work.dx & 0xff);
                }
//qrnd_set:
                if ((work.dx & 0xff00) != 0) { // break qseta;

                    //MSeg.m_buf.set(Work.di++, new MmlDatum(Work.dx & 0xff));
                    //MSeg.m_buf.set(Work.di++, new MmlDatum((Work.dx & 0xff00) >> 8)));

                    byte b = (byte) (work.dx & 0xff);
                    work.dx = ((work.dx & 0xff00) >> 8) + (b & 0xff) * 0x100;
                    work.ctype = MMLType.Gatetime;
                    work.cargs = new Object[] {(int) work.al};
                    parset();
                }
            }
//qseta:
            al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (al != ',') return enmPass2JumpTable.olc0;
        }
//qsetb:
        work.si++;
        work.dx = 'q' * 0x100 + (work.dx & 0xff);
        get_clock();
        work.dx = 0xb300 + work.al;
        work.ctype = MMLType.Gatetime;
        work.cargs = new Object[] {work.al & 0xff};
        return enmPass2JumpTable.parset;
    }

    //5516-5548
    /**
    // COMMAND "Q" step-gate time change 2
     */
    private enmPass2JumpTable qset2() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        char alc = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (alc == '%') return qset3();

        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al >= 9) {
            error('Q', 2, work.si);
        }

        work.al *= 2;
        if (work.al != 0) { // break q2_not_inc;

            work.al *= 16;
            work.al--;
        }
//q2_not_inc:
        work.al = (byte) ~work.al;
        work.dx = 0xc400 + work.al;
        work.ctype = MMLType.Gatetime;
        work.cargs = new Object[] {(int) work.al};
        return enmPass2JumpTable.parset;
    }

    /**
    // COMMAND "Q%" step-gate time change 2
     */
    private enmPass2JumpTable qset3() {
        work.si++;

        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);
        work.al = (byte) ~work.al;
        work.dx = 0xc400 + work.al;
        work.ctype = MMLType.Gatetime;
        work.cargs = new Object[] {(int) work.al};
        return enmPass2JumpTable.parset;
    }

    /**
    // COMMAND "v"/"V" volume_set
     */
    private enmPass2JumpTable vseta() {
        char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (al == '+') return enmPass2JumpTable.vss;
        if (al == '-') return enmPass2JumpTable.vss;
        if (al == ')') return enmPass2JumpTable.vss2;
        if (al == '(') return enmPass2JumpTable.vss2m;

        int[] bx = new int[1];
        byte[] alb = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ alb);
        work.bx = bx[0];

        // md= new MmlDatum(-1, MMLType.Volume, MakeLinePos(), bx);

        work.bx += mml_seg.volss2;
        if ((byte) work.bx >= 17) {
            error('v', 2, work.si);
        }

//#if !efc
        if (mml_seg.part == mml_seg.pcmpart) return enmPass2JumpTable.vsetm;
        if (mml_seg.ongen == mml_seg.pcm_ex) return enmPass2JumpTable.vsetm;
        if (mml_seg.towns_flg == 1) { // break vsa_no_towns;
            if (mml_seg.part == mml_seg.rhythm2) return enmPass2JumpTable.vsetm; // TownsのKパートはPCM相当
        }
//vsa_no_towns:
        if (mml_seg.ongen >= mml_seg.psg) return enmPass2JumpTable.vset;
//#endif
        work.bx = (byte) work.bx;
        work.bx += 0; // offset fmvol
        work.bx = mml_seg.fmvol[work.bx];

        return enmPass2JumpTable.vset;
    }

    private enmPass2JumpTable vset() {
        //Work.dx = (Work.dx & 0xff00) + (byte)Work.bx;
        work.dx = (byte) work.bx;
        return vset2();
    }

    private enmPass2JumpTable vset2() {
        work.dx = 0xfd00 + (byte) work.dx;
        work.al = (byte) mml_seg.volss;
        work.al += (byte) work.dx;
        if ((work.al & 0xff) >= 0x80) { // break vset4;
            if ((mml_seg.volss & 0x80) != 0) { // break vset3;
                work.al = 0;
//                break vset4;
            } else {
//vset3:
//#if !efc
                if (mml_seg.ongen >= mml_seg.psg) { // break vset3f;
                    work.al = 15;
//                    break vset4;
//#endif
                } else {
//vset3f:
                    work.al = 0x7f;
                }
            }
        }
//vset4:
        work.dx = (work.dx & 0xff00) + work.al;
        mml_seg.nowvol = work.al;

        work.ctype = MMLType.Volume;
        work.cargs = new Object[] {(int) work.al};

        return enmPass2JumpTable.parset;
    }

    //5606-5621
    /**
    // command "V" entry
     */
    private enmPass2JumpTable vsetb() {
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
//#if !efc
        work.bx = bx[0];
        work.al = al[0];
        if (mml_seg.part == mml_seg.pcmpart) return enmPass2JumpTable.vsetm1;
        if (mml_seg.ongen == mml_seg.pcm_ex) return enmPass2JumpTable.vsetm1;
        if (mml_seg.towns_flg != 1) return enmPass2JumpTable.vset;
        if (mml_seg.part == mml_seg.rhythm2) return enmPass2JumpTable.vsetm1; // TownsのKパートはPCM相当
//#endif

        return enmPass2JumpTable.vset;
    }


//#if!efc

    //5622-5656
    /**
    // PCM volset patch
     */
    private enmPass2JumpTable vsetm() {
        if (mml_seg.pcm_vol_ext != 1) { // break vsetma;
            if ((byte) work.bx * 16 < 256) work.bx = (work.bx & 0xff00) + (byte) work.bx * 16;
            else work.bx = (work.bx & 0xff00) + 255;
            return enmPass2JumpTable.vsetm1;
        }
//vsetma:
        work.al = (byte) work.bx;
        int ax = work.al * (byte) work.bx; //    mul bl
        work.al = (byte) ax;
        if (ax >= 256) { // break vsetmb;
            work.al = (byte) 255;
        }
//vsetmb:
        work.bx = (work.bx & 0xff00) + work.al;
        return enmPass2JumpTable.vsetm1;
    }

    private enmPass2JumpTable vsetm1() {
        work.dx = 0xfd00 + (work.bx & 0xff);
        work.al = (byte) mml_seg.volss;
        if ((work.al & 0x80) != 0) { // break vsetm0;
            boolean cy = (work.dx & 0xff) + (work.al & 0xff) > 255;
            work.dx = (work.dx & 0xff00) + (((work.dx & 0xff) + (work.al & 0xff)) & 0xff);
            if (!cy) { // break vset4m;
                work.dx = (work.dx & 0xff00);
            }
//            break vset4m;
        } else {
//vsetm0:
            boolean cy = (work.dx & 0xff) + (work.al & 0xff) > 255;
            work.dx = (work.dx & 0xff00) + (((work.dx & 0xff) + (work.al & 0xff)) & 0xff);
            if (cy) { // break vset4m;
                work.dx = (work.dx & 0xff00) + 255;
            }
        }
//vset4m:
        work.al = (byte) work.dx;

        //vset4相当
        work.dx = (work.dx & 0xff00) + (work.al & 0xff);
        mml_seg.nowvol = work.al;

        work.ctype = MMLType.Volume;
        work.cargs = new Object[] {work.al & 0xff};
        return enmPass2JumpTable.parset;
    }
//#endif

    //5657-5674
    /**
    // command "v+"/"v-" entry
     */
    private enmPass2JumpTable vss() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

        cy = getnum(/* out */ bx, /* out */ dl);

        mml_seg.volss = dl[0];
        work.dx = (work.dx & 0xff00) | (byte) mml_seg.nowvol;
//#if !efc
        if (mml_seg.part == mml_seg.pcmpart) return enmPass2JumpTable.vsetm1;
        if (mml_seg.ongen == mml_seg.pcm_ex) return enmPass2JumpTable.vsetm1;
        if (mml_seg.towns_flg != 1) return vset2();
        if (mml_seg.part == mml_seg.rhythm2) return enmPass2JumpTable.vsetm1; // TownsのKパートはPCM相当
//#endif
        return vset2();
    }

    //5675-5688
    /**
    // command "v)"/"v(" entry
     */
    private enmPass2JumpTable vss2() {
        work.si++;

        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);

        mml_seg.volss2 = dl[0];
        return enmPass2JumpTable.olc03;
    }

    private enmPass2JumpTable vss2m() {
        work.si++;

        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);
        dl[0] = (byte) -dl[0];

        mml_seg.volss2 = dl[0];
        return enmPass2JumpTable.olc03;
    }

    //5689-5798
    /**
    // command "@" 音色の変更
     */
    private enmPass2JumpTable neirochg() {
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];
        boolean cy;
        int cx = 0;
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch == '@') {
            work.si++;
            cx = 128;
        }
//nc01:
        cy = lngset(/* out */ bx, /* out */ al);
        work.bx += cx;
        if (mml_seg.prg_flg != 0) set_prg();

//not_sp:
repeat_check: // ↑
        {
psgprg: // ↑
            {
//#if !efc
                if (mml_seg.part != mml_seg.rhythm) { // break rhyprg;
                    if (mml_seg.ongen == mml_seg.psg) break psgprg;
//#endif
                    work.dx = 0xff00 + (work.bx & 0xff);
                    work.bx = (work.bx & 0xff00) | ((work.bx & 0xff) + 1);
                    if (mml_seg.maxprg < (work.bx & 0xff)) { // break nc00;
                        mml_seg.maxprg = (work.bx & 0xff);
//#if efc
//nc00:
//        ;
//        return enmPass2JumpTable.parset;
//#else
                    }
//nc00:
                    if (mml_seg.part == mml_seg.pcmpart) break repeat_check;
                    if (mml_seg.ongen == mml_seg.pcm_ex) break repeat_check;
                    if (mml_seg.part != mml_seg.rhythm2) {
                        work.ctype = MMLType.Instrument;
                        work.cargs = new Object[] {};
                        return enmPass2JumpTable.parset;
                    }
                    if (mml_seg.towns_flg == 1) break repeat_check; // townsの K = PCM part
                    if (mml_seg.skip_flag != 0) return enmPass2JumpTable.olc0;

                    MmlDatum cmd = new MmlDatum(work.dx & 0xff, MMLType.Instrument, MakeLinePos());
                    m_seg.m_buf.set(work.di++, cmd);

                    mml_seg.length_check1 = 1; // 音長データがあったよ
                    mml_seg.length_check2 = 1;
                    return enmPass2JumpTable.olc0;
                }
//rhyprg:
                if (work.bx >= 0x4000) {
                    error('@', 2, work.si);
                }

                work.bx |= (0b1000_0000) * 0x100;
                mml_seg.lastprg = work.bx;
                return enmPass2JumpTable.olc03;
            }
//psgprg:
            work.bx = (byte) work.bx;
            if (work.bx >= mml_seg.psgenvdat_max + 1) work.bx = 0;
            //Work.bx *= 4;
            //Work.bx = (byte)Work.bx;
            work.bx += 0; // offset psgenvdat
            MmlDatum cmd = new MmlDatum(0xf0, MMLType.Instrument, MakeLinePos());
            m_seg.m_buf.set(work.di++, cmd);
            cx = 4;
            //if (Work.bx > 9) {
            //    logger.log(Level.WARNING, String.format(rb.getString("W0100"), Work.bx));
            //    Work.bx = 0;
            //}
            //pplop0:;
            for (int i = 0; i < 4; i++) {
                work.al = (byte) mml_seg.psgenvdat[work.bx][i];
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            }
            //loop    pplop0
            return enmPass2JumpTable.olc0;
        }
//repeat_check:
        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != ',') {
            work.ctype = MMLType.Instrument;
            work.cargs = new Object[] {};
            return enmPass2JumpTable.parset;
        }

        int ax = work.dx;
        ax = (byte) ((ax & 0xff00) >> 8) | ((ax & 0xff) * 0x100);

        m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

        work.si++;

        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = (byte) 0xce;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        ax = work.bx;
        m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
noset_release: // ↑
        {
            if (ch == ',') { // break noset_stop;

                work.si++;
                cy = getnum(/* out */ bx, /* out */ dl);
                ax = work.bx;
                m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

                ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                if (ch != ',') break noset_release;

                work.si++;
                cy = getnum(/* out */ bx, /* out */ dl);
                ax = work.bx;
                m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

                return enmPass2JumpTable.olc0;
            }
//noset_stop:
            ax = 0;
            m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
            m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));
        }
//noset_release:
        ax = 0x8000;
        m_seg.m_buf.set(work.di++, new MmlDatum(ax & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum((ax & 0xff00) >> 8));

        return enmPass2JumpTable.olc0;
//#endif
    }

    /**
    // V2.6用 / FM音源の音色使用フラグセット
     */
    private void set_prg() {
//#if !efc
        if (mml_seg.ongen >= mml_seg.psg) return;
//#endif
        int bx_p = work.bx;
        work.bx = (byte) work.bx;
        work.bx += 0; // offset prg_num
        mml_seg.prg_num[work.bx] = 1;
        work.bx = bx_p;
        //set_prg_ret:;
        return;
    }

    /**
    // COMMAND "&" タイ
    // COMMAND "&&" スラー
    // COMMAND "+"     直前の音長の加算
     */
    private enmPass2JumpTable tieset() {
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch == '&') return sular(); // スラー?

        return tieset_2();
    }

    private enmPass2JumpTable tieset_2() {
        if (mml_seg.skip_flag != 0) return tie_skip();

        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset2(/* out */ bx, /* out */ al);
        if (!cy) { // break tie_norm;

            if ((work.bx & 0xff00) != 0) {
                error('&', 8, work.si);
            }

            if ((mml_seg.prsok & 1) == 0) { // 直前byte = 音長?
                error('&', 22, work.si);
            }

            if ((work.bx & 0xff00) != 0) { // KUMA:２かい調べる?
                error('&', 8, work.si);
            }

            work.di--;
            lngcal();
            cy = futen();
            if (cy) {
                error('&', 8, work.si);
            }

            byte ah = (byte) (m_seg.m_buf.get(work.di).dat & 0xff);
            if ((ah & 0xff) + (work.al & 0xff) > 0xff) {
                ah += work.al;
//                break tie_lng_over;
            } else {
                ah += work.al;
                work.al = ah;

                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                mml_seg.prsok |= 2; // 直前 = 加工音長
                return enmPass2JumpTable.olc02;
            }
//tie_lng_over:
            work.di++;

            if ((mml_seg.prsok & 8) != 0) { // ポルタ?
                error('^', 8, work.si);
            }

//#if !efc
            if (mml_seg.part != mml_seg.rhythm) { // break tlo_r; // R
//#endif

                m_seg.m_buf.set(work.di, new MmlDatum(0xfb));
                ah = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
                work.di++;

                m_seg.m_buf.set(work.di, new MmlDatum(ah & 0xff));
                work.di++;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                mml_seg.prsok = 1; // 直前 = 音長
                return enmPass2JumpTable.olc02;
            }
//tlo_r:
            m_seg.m_buf.set(work.di, new MmlDatum(0x0f)); //休符
            work.di++;

            m_seg.m_buf.set(work.di, new MmlDatum(work.al & 0xff));
            work.di++;
            mml_seg.prsok = 3; // 直前 = 加工音長
            return enmPass2JumpTable.olc02;
        }
//tie_norm:
        mml_seg.tie_flag = 1;
//#if !efc
        if (mml_seg.part == mml_seg.rhythm) // R
        {
            error('&', 32, work.si);
        }
//#endif
        m_seg.m_buf.set(work.di, new MmlDatum(0xfb));
        work.di++;
        if ((mml_seg.prsok & 1) == 0) // 直前byte = 音長?
            return enmPass2JumpTable.olc0; // でなければ prsok = 0クリア
        mml_seg.prsok |= 4; // 音長+タイのフラグをセット
        mml_seg.prsok &= 0xfe; // 音長のフラグをリセット
        return enmPass2JumpTable.olc02;
    }

    private enmPass2JumpTable tie_skip() {
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset2(/* out */ bx, /* out */ al);
        if ((work.bx & 0xff00) != 0) {
            error('&', 8, work.si);
        }
        futen_skip();
        return enmPass2JumpTable.olc03;
    }

    private enmPass2JumpTable sular() {
//#if !efc
        if (mml_seg.part == mml_seg.rhythm) { // R
            error('&', 32, work.si);
        }
//#endif
        work.si++;
        if (mml_seg.skip_flag != 0) return tie_skip();
        work.al = (byte) 0xc1;
        m_seg.m_buf.set(work.di, new MmlDatum(work.al & 0xff));
        work.di++;

        int si_p = work.si;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset2(/* out */ bx, /* out */ al);

        //    pushf
        if ((work.bx & 0xff00) != 0) {
            error('&', 8, work.si);
        }
        //    popf
        work.si = si_p;
        if (cy) return enmPass2JumpTable.olc0;

        byte bl = (byte) (m_seg.m_buf.get(work.di - 3).dat & 0xff); // bl=前の音階
        return enmPass2JumpTable.ots002; // 通常音程コマンド発行
    }

    /**
    // COMMAND "D" デチューンの設定
     */
    private enmPass2JumpTable detset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        switch (work.al) {
            case (byte) 'D' -> {
                // break detset_2;
                /**
                // COMMAND "DD" 相対デチューンの設定
                 */
//detset_2:
                cy = getnum(/* out */ bx, /* out */ dl);
                m_seg.m_buf.set(work.di++, new MmlDatum(0xd5, MMLType.Detune, MakeLinePos(), work.bx));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
                return enmPass2JumpTable.olc0;
            }
            case (byte) 'X' -> {
//            break extdet_set;
                /**
                // COMMAND "DX" 拡張デチューン指定
                 */
//extdet_set:
                work.al = (byte) 0xcc;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                cy = getnum(/* out */ bx, /* out */ dl);
                work.al = dl[0];
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                return enmPass2JumpTable.olc0;
            }
            case (byte) 'M' -> {
//                break mstdet_set;
                /**
                // COMMAND "DM" マスターデチューン指定
                 */
//mstdet_set:
                cy = getnum(/* out */ bx, /* out */ dl);
                mml_seg.master_detune = bx[0];
                work.bx += mml_seg.detune;
//                break detset_exit;
//detset_exit:
                if (mml_seg.bend != 0) return enmPass2JumpTable.olc03;
                work.al = (byte) 0xfa;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff, MMLType.Detune, MakeLinePos(), work.bx));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
                return enmPass2JumpTable.olc0;
            }
            case (byte) 'F' -> {
//                break vd_fm;
                /**
                // COMMAND "DF"/"DS"/"DP"/"DR" 音量ダウン設定
                 */
//vd_fm:
                work.al = (byte) 0xfe;
//                break vd_main;
            }
            case (byte) 'S' -> {
//                break vd_ssg;
//vd_ssg:
                work.al = (byte) 0xfc;
//                break vd_main;
            }
            case (byte) 'P' -> {
//                break vd_pcm;
//vd_pcm:
                work.al = (byte) 0xfa;
//                break vd_main;
            }
            case (byte) 'R' -> {
//                break vd_rhythm;
//vd_rhythm:
                work.al = (byte) 0xf8;
//                break vd_main;
            }
            case (byte) 'Z' -> {
//                break vd_ppz;
//vd_ppz:
                work.al = (byte) 0xf5;
//                break vd_main;
            }
            default -> {
                work.si--;
                cy = getnum(/* out */ bx, /* out */ dl);
                mml_seg.detune = bx[0];
                bx[0] += mml_seg.master_detune;
                work.bx = bx[0];
//detset_exit:
                if (mml_seg.bend != 0) return enmPass2JumpTable.olc03;
                work.al = (byte) 0xfa;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff, MMLType.Detune, MakeLinePos(), work.bx));
                m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
                m_seg.m_buf.set(work.di++, new MmlDatum((work.bx & 0xff00) >> 8));
                return enmPass2JumpTable.olc0;
            }
        }

//vd_main:
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '+') { // break vd_main2;
            if (ch != '-') { // break vd_main2;
                work.al++;
            }
        }
//vd_main2:
        m_seg.m_buf.set(work.di++, new MmlDatum(0xc0));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        cy = getnum(/* out */ bx, /* out */ dl);
        work.al = dl[0];
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // 符号付き数値を読む
    //  OUTPUTS -- bx[word],dl[byte]
     */
    private boolean getnum(/* out */ int[] bx, /* out */ byte[] dl) {
        char al;
        int dh = 0;
        do {
            al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;

        } while (al == ' ' || al == 9);

        if (al == '+') {
            dh = 0;
        } else if (al == '-') {
            dh = 1;
        } else {
            work.si--;
        }

        byte[] al_b = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out byte */ al_b);

        dl[0] = (byte) bx[0];
        if (dh == 0) {
            work.dx = dl[0];
            work.bx = bx[0];
            return cy;
        }

        // dlとbxの符号を反転
        bx[0] = -bx[0];
        dl[0] = (byte) (-(int) dl[0]);

        work.dx = (dh << 8) | dl[0];
        work.bx = bx[0];
        return cy;
    }

    /**
    // COMMAND "[" [LOOP START]
     */
    private enmPass2JumpTable stloop() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xf9));

        work.al = (byte) mml_seg.lopcnt;
        mml_seg.lopcnt++;

        int ax = work.al * 2;
        work.bx = 0; // offset loptbl
        work.bx += ax; // bxにloptblをセット

        // dxに、di - mbufを入れる
        work.dx = work.di;
        work.dx -= 0; // offset m_buf

        // 現在のdi - mbufをloptblに書く
        mml_seg.loptbl[work.bx + 0] = (byte) work.dx;
        mml_seg.loptbl[work.bx + 1] = (byte) (work.dx >> 8);

        // lextblに 0を書いておく
        //Work.bx += MmlSeg.loopnest * 2;
        //MmlSeg.loptbl[Work.bx + 0] = (byte)0;
        //MmlSeg.loptbl[Work.bx + 1] = (byte)0;
        mml_seg.lextbl[work.bx + 0] = (byte) 0;
        mml_seg.lextbl[work.bx + 1] = (byte) 0;

        //2byte 開けておく
        //Work.di += 2;
        m_seg.m_buf.set(work.di++, new MmlDatum(0x00)); // KUMA: オリジナルではメモリの内容が不定のまま？
        m_seg.m_buf.set(work.di++, new MmlDatum(0x00));

        mml_seg.length_check2 = 0; // []0発見対策

        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "]" [LOOP END]
     */
    private enmPass2JumpTable edloop() {
        MmlDatum md = new MmlDatum(0xf8);
        if (!m_seg.getMacroLst().isEmpty()) {
            List<Object> args = new ArrayList<>();
            args.add(null);
            args.add(null);
            args.add(m_seg.getMacroLst().toArray(MmlDatum[]::new));
            m_seg.getMacroLst().clear();
            md.args = args;
        }

        m_seg.m_buf.set(work.di++, md);

        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);

        if (cy) {
            work.bx = mml_seg.loop_def; // bl
            edl00b();
        } else
            edl00();

        return enmPass2JumpTable.olc0;
    }

    private void edl00() {
        byte bh = (byte) (work.bx >> 8);
        if (bh != 0) {
            error(']', 2, work.si);
        }
        edl00b();
    }

    // 繰り返し回数セット
    private void edl00b() {
        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));

        if ((byte) work.bx == 0) { // break edl_nonmuloop;

            if (mml_seg.length_check2 == 0) {
                error('[', 24, work.si);
            }
        }
//edl_nonmuloop:
        // ひとつ開ける（ドライバーで使用する）
        m_seg.m_buf.set(work.di++, new MmlDatum(0));
        mml_seg.lopcnt--;
        work.al = (byte) mml_seg.lopcnt;
        if (work.al == (byte) 0xff) {
            error(']', 23, work.si);
        }
        work.bx = work.al * 2; // offset loptbl
        //loptblに書いておいた値をセット
        work.dx = (mml_seg.loptbl[work.bx + 0] & 0xff) + (mml_seg.loptbl[work.bx + 1] & 0xff) * 0x100;
        m_seg.m_buf.set(work.di + 0, new MmlDatum(work.dx & 0xff));
        m_seg.m_buf.set(work.di + 1, new MmlDatum((work.dx & 0xff00) >> 8));

        /**
        // "[" のあった所に今のアドレスを書く
         */
        int bx_p = work.bx;
        work.dx += 0; // offset m_buf ;dx=[commandで２つ開けておいたアドレス
        work.bx = work.di;
        work.bx -= 2; // bx=繰り返し回数がセットされているアドレス
        work.bx -= 0; // offset m_buf
        int a = work.bx;
        work.bx = work.dx;
        work.dx = a;

        m_seg.m_buf.set(work.bx + 0, new MmlDatum(work.dx & 0xff)); // そこにもdxを書く
        m_seg.m_buf.set(work.bx + 1, new MmlDatum((work.dx & 0xff00) >> 8));
        work.bx = bx_p;

        /**
        // ":" があった時にはそこにも書く
         */
        //Work.bx += MmlSeg.loopnest * 2; // bx＝lextblの位置
        //Work.bx = MSeg.m_buf.get(Work.bx).dat + MSeg.m_buf.get(Work.bx).dat * 0x100; // bx＝lextblの値
        work.bx = (mml_seg.lextbl[work.bx] & 0xff) + (mml_seg.lextbl[work.bx + 1] & 0xff) * 0x100; // bx＝lextblの値
        if (work.bx != 0) { // break nonexit; // ":"はない

            m_seg.m_buf.set(work.bx + 0, new MmlDatum(work.dx & 0xff)); // そこにもdxを書く
            m_seg.m_buf.set(work.bx + 1, new MmlDatum((work.dx & 0xff00) >> 8));
            // DETUNE CANCEL(Bend On / ":"のあった時のみ)
            if (mml_seg.bend != 0) { // break nonexit;
                mml_seg.alldet = 0x8000;
            }
        }
//nonexit:
        work.di += 2;
        //if (Work.si != 0) return enmPass2JumpTable.olc0; // pass2最後のcheck_loopか?
    }

    /**
    // COMMAND ":" ループから脱出
     */
    private enmPass2JumpTable extloop() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xf7));

        work.al = (byte) (mml_seg.lopcnt & 0xff);
        work.al--;

        if (work.al == (byte) 0xff) {
            error(':', 23, work.si);
        }

        work.bx = 0; // offset lextbl
        work.bx += work.al * 2;
        work.dx = (mml_seg.lextbl[work.bx + 0] & 0xff) + (mml_seg.lextbl[work.bx + 1] & 0xff) * 0x100;

        if (work.dx != 0) {
            error(':', 25, work.si); // ":"が２つ以上あった
        }

        mml_seg.lextbl[work.bx + 0] = (byte) (work.di & 0xff); // lextblに開けておくアドレスをセット
        mml_seg.lextbl[work.bx + 1] = (byte) ((work.di & 0xff00) >> 8);

        //Work.di += 2; // ２つ、開けておく
        m_seg.m_buf.set(work.di++, new MmlDatum(0x00)); // KUMA: オリジナルではメモリの内容が不定のまま？
        m_seg.m_buf.set(work.di++, new MmlDatum(0x00));

        return enmPass2JumpTable.olc0;
    }

    //6204-6220
    /**
    // COMMAND "L" [LOOP SET]
     */
    private enmPass2JumpTable lopset() {
//#if efc
        error('L', 17, work.si);
//#else
        if (mml_seg.part == mml_seg.rhythm) {
            error('L', 17, work.si); // R
        }

        m_seg.m_buf.set(work.di++, new MmlDatum(0xf6));
        mml_seg.allloop_flag = 1;

        mml_seg.length_check1 = 0;
        return enmPass2JumpTable.olc0;
//#endif
    }

    //6221-6281
    /**
    // COMMAND "_" [転調] , "__" [相対転調] , "_M" [Master転調]
    // "_{" Command[移調設定]
     */
    private enmPass2JumpTable oshift() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        byte ah;

        char al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;

        switch (al) {
            case '_': // osf00;
//osf00:
                cy = getnum(/* out */ bx, /* out */ dl);
                work.dx = 0xf5 * 0x100 + (dl[0] & 0xff);
                work.ctype = MMLType.KeyShift;
                work.cargs = new Object[] {dl[0] & 0xff};
                return enmPass2JumpTable.parset;            //_ command
            case 'M': // break master_trans_set;
//master_trans_set:
                work.si++;
                cy = getnum(/* out */ bx, /* out */ dl);
                work.dx = 0xb2 * 0x100 + (dl[0] & 0xff);
                return enmPass2JumpTable.parset;            //_M command
            case '{': // break def_onkai_set;
//def_onkai_set:
                work.si++;
                al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;

                ah = 0;
                if (al == '=') {
                    ah = 0;
                } else if (al == '+') {
                    ah = +1;
                } else if (al == '-') {
                    ah = (byte) 0xff; // -1
                } else {
                    error('{', 2, work.si);
                }

                //dos_main:;
                do {
                    al = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
                    if (al == '}') return enmPass2JumpTable.olc03;
                    if (al == ' ') continue;
                    if (al == 9) continue;
                    if (al == ',') continue;

                    if (al < 'a' || al > 'g') {
                        //error((byte)(Work.dx >> 8), (byte)Work.dx, Work.si);
                        error('_', 7, work.si); // KUMA:
                    }

                    switch (al) {
                        case 'a':
                            mml_seg.def_a = ah;
                            break;
                        case 'b':
                            mml_seg.def_b = ah;
                            break;
                        case 'c':
                            mml_seg.def_c = ah;
                            break;
                        case 'd':
                            mml_seg.def_d = ah;
                            break;
                        case 'e':
                            mml_seg.def_e = ah;
                            break;
                        case 'f':
                            mml_seg.def_f = ah;
                            break;
                        case 'g':
                            mml_seg.def_g = ah;
                            break;
                    }
                } while (true);
            default:
                work.si++;
                cy = getnum(/* out */ bx, /* out */ dl);
                work.dx = 0xe7 * 0x100 + (dl[0] & 0xff);
                work.ctype = MMLType.KeyShift;
                work.cargs = new Object[] {dl[0] & 0xff}; // TODO vavi
                return enmPass2JumpTable.parset;            //__ command
        }
    }

    //6282-6336
    /**
    // COMMAND ")" volume up
     */
    private enmPass2JumpTable volup() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        MmlDatum cmd;

        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '%' && ch != '^') { // break volup3;

            cy = lngset(/* out */ bx, /* out */ al);
            if (al[0] == 1) { // break volup2;

                cmd = new MmlDatum(0xf4, MMLType.Volume, MakeLinePos(), (Object[]) null);
                m_seg.m_buf.set(work.di++, cmd);
                return enmPass2JumpTable.olc0;
            }
//volup2:
            cmd = new MmlDatum(0xe3, MMLType.Volume, MakeLinePos(), (Object[]) null);
            m_seg.m_buf.set(work.di++, cmd);
            ongen_sel_vol();
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            return enmPass2JumpTable.olc0;
        }
//volup3:
        if (ch != '^') { // break volup4;
            work.si++;
            cy = lngset(/* out */ bx, /* out */ al);
            cmd = new MmlDatum(0xe3, MMLType.Volume, MakeLinePos(), (Object[]) null);
            m_seg.m_buf.set(work.di++, cmd);
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            return enmPass2JumpTable.olc0;
        }
//volup4:
        work.si++;
        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '%') { // break volup5;

            cy = lngset(/* out */ bx, /* out */ al);
            ongen_sel_vol();
            if (work.al == 0) return enmPass2JumpTable.olc03; // 0なら無視

            cmd = new MmlDatum(0xde, MMLType.Volume, MakeLinePos());
            m_seg.m_buf.set(work.di++, cmd);
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

            if (mml_seg.skip_flag == 0) return enmPass2JumpTable.olc0;
            mml_seg.acc_adr = work.di;
            return enmPass2JumpTable.olc0;
        }
//volup5:
        work.si++;
        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al == 0) return enmPass2JumpTable.olc03; // 0なら無視

        cmd = new MmlDatum(0xde, MMLType.Volume, MakeLinePos());
        m_seg.m_buf.set(work.di++, cmd);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        if (mml_seg.skip_flag == 0) return enmPass2JumpTable.olc0;
        mml_seg.acc_adr = work.di;
        return enmPass2JumpTable.olc0;
    }

    //6337-6391
    /**
    // COMMAND "(" volume down
     */
    private enmPass2JumpTable voldown() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        MmlDatum cmd;

voldown4: // ↑
        {
            char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (ch != '%') { // break voldown3;
                if (ch == '^') break voldown4;

                cy = lngset(/* out */ bx, /* out */ al);
                if (al[0] == 1) { // break voldown2;

                    cmd = new MmlDatum(0xf3, MMLType.Volume, MakeLinePos());
                    m_seg.m_buf.set(work.di++, cmd);
                    return enmPass2JumpTable.olc0;
                }
//voldown2:
                cmd = new MmlDatum(0xe2, MMLType.Volume, MakeLinePos());
                m_seg.m_buf.set(work.di++, cmd);
                ongen_sel_vol();
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                return enmPass2JumpTable.olc0;
            }
//voldown3:
            work.si++;
            cy = lngset(/* out */ bx, /* out */ al);
            cmd = new MmlDatum(0xe2, MMLType.Volume, MakeLinePos());
            m_seg.m_buf.set(work.di++, cmd);
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
            return enmPass2JumpTable.olc0;
        }
//voldown4:
        work.si++;
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '%') { // break voldown5;

            cy = lngset(/* out */ bx, /* out */ al);
            ongen_sel_vol();
            if (work.al == 0) return enmPass2JumpTable.olc03; // 0なら無視

            cmd = new MmlDatum(0xdd, MMLType.Volume, MakeLinePos());
            m_seg.m_buf.set(work.di++, cmd);
            m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

            if (mml_seg.skip_flag == 0) return enmPass2JumpTable.olc0;
            mml_seg.acc_adr = work.di;
            return enmPass2JumpTable.olc0;
        }
//voldown5:
        work.si++;
        cy = lngset(/* out */ bx, /* out */ al);
        if (work.al == 0) return enmPass2JumpTable.olc03; // 0なら無視

        cmd = new MmlDatum(0xdd, MMLType.Volume, MakeLinePos());
        m_seg.m_buf.set(work.di++, cmd);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        if (mml_seg.skip_flag == 0) return enmPass2JumpTable.olc0;
        mml_seg.acc_adr = work.di;
        return enmPass2JumpTable.olc0;
    }

    //6392-6633
    /**
    // COMMAND "M" lfo set
     */
    private enmPass2JumpTable lfoset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        char ch;
//#if !efc
        if (mml_seg.part == mml_seg.rhythm) {
            error('M', 17, work.si); // R
        }
//#endif
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        switch (work.al) {
            case (byte) 'X' -> {
//            break extlfo_set;
                /**
                // COMMAND "MX" LFO Speed Extended Mode Set Ver.4.0m～
                 */
//extlfo_set:
                work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                work.ah = (byte) 0xbb;
                if (work.al != (byte) 'B') { // break extlfo_set_main;
                    work.ah = (byte) 0xca;
                    if (work.al != (byte) 'A') { // break extlfo_set_main;
                        work.si--;
                    }
                }
                //extlfo_set_main:
                m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));
                cy = getnum(/* out */ bx, /* out */ dl);
                work.al = dl[0];
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                return enmPass2JumpTable.olc0;
            }
            case (byte) 'P' -> {
//            break portaset;
                /**
                // COMMAND "MP"[PORTAMENT SET] for PMD V2.3 -
                // MPa[, b][, c] = Mb, c, a, 255 * 1 def.b = 0, c = 1
                 */
//portaset:
                work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                work.ah = (byte) 0xbf;
                if (work.al != 'B') { // break portaset_main;
                    work.ah = (byte) 0xf2;
                    if (work.al != 'A') { // break portaset_main;
                        work.si--;
                    }
                }
//portaset_main:
                int ah_p = work.ah;
                byte al_p = work.al;
                mml_seg.bend2 = 0;
                mml_seg.bend3 = 1;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));
                cy = getnum(/* out */ bx, /* out */ dl);
                mml_seg.bend1 = dl[0];
                ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                if (ch == ',') { // break bset;
                    work.si++;
                    work.dx = 'M' * 0x100 + (work.dx & 0xff);
                    get_clock();
                    mml_seg.bend2 = work.al;

                    ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                    if (ch == ',') { // break bset;

                        work.si++;
                        cy = getnum(/* out */ bx, /* out */ dl);
                        mml_seg.bend3 = dl[0];
                    }
                }
//bset:
                work.al = (byte) mml_seg.bend2;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.al = (byte) mml_seg.bend3;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.al = (byte) mml_seg.bend1;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.al = (byte) 255;
                m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                work.dx = (ah_p - 1) * 0x100 + 1;            //0f1h or 0beh
                return enmPass2JumpTable.parset;
            }
            case (byte) 'D' -> {
//            break depthset;
                /**
                // COMMAND "MD"[DEPTH SET] for PMD V3.3 -
                // MDa, b
                 */
//depthset:
                work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                work.ah = (byte) 0xbd;
                if (work.al != (byte) 'B') { // break depthset_main;
                    work.ah = (byte) 0xd6;
                    if (work.al != (byte) 'A') { // break depthset_main;
                        work.si--;
                    }
                }
//depthset_main:
                m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));
                byte ah_p = work.ah;
                byte al_p = work.al;
                cy = getnum(/* out */ bx, /* out */ dl);
                m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
                if (dl[0] == 0) { // 0の場合は // break dps_nextparam;
                    dl[0] = 0;

                    ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                    if (ch == ',') { // 値２を省略可 // break dps_param2set;
                        work.si++;
                        cy = getnum(/* out */ bx, /* out */ dl);
                    }
//            break dps_param2set;
                } else {
//dps_nextparam:
                    ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                    if (ch != ',') {
                        error('M', 6, work.si);
                    }

                    work.si++;
                    cy = getnum(/* out */ bx, /* out */ dl);

                    m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

                    work.ah = ah_p;
                    work.al = al_p;

                    ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                    if (ch != ',') {
                        return enmPass2JumpTable.olc0;
                    }

                    work.si++;

                    // 値３(counter)   4.7a～
                    work.al = (byte) 0xb7;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

                    ah_p = work.ah;
                    al_p = work.al;

                    cy = getnum(/* out */ bx, /* out */ dl);

                    work.ah = ah_p;
                    work.al = al_p;

                    if ((((byte) work.bx) & 0x80) != 0) {
                        error('M', 2, work.si);
                    }

                    if (work.ah != 0xd6) { // A? // break mdc_noa;
                        work.bx = (work.bx & 0xff00) | ((byte) work.bx | 0x80);
                    }
//mdc_noa:
                    work.al = (byte) work.bx;
                    m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
                    return enmPass2JumpTable.olc0;
                }
//dps_param2set:
                m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
                work.ah = ah_p;
                work.al = al_p;
                return enmPass2JumpTable.olc0;
            }
            case (byte) 'W' -> {
//            break waveset;
                /**
                // COMMAND "MW" [WAVE SET] for PMD V4.0j～
                 */
//waveset:
                work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                work.ah = (byte) 0xbc;
                if (work.al != 'B') { // break waveset_main;
                    work.ah = (byte) 0xcb;
                    if (work.al != 'A') { // break waveset_main;
                        work.si--;
                    }
                }
//waveset_main:
                byte ah_p = work.ah;
                byte al_p = work.al;
                cy = getnum(/* out */ bx, /* out */ dl);
                work.ah = ah_p;
                work.al = al_p;

                work.dx = (work.ah & 0xff) * 0x100 + (work.dx & 0xff);

                return enmPass2JumpTable.parset;
            }
            case (byte) 'M' -> {
                //break lfomask_set;

                /**
                // COMMAND "MM" LFO Mask for PMD v4.2～
                 */
//lfomask_set:
                work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                work.ah = (byte) 0xba;
                if (work.al != 'B') { // break lfomask_set_main;
                    work.ah = (byte) 0xc5;
                    if (work.al != 'A') { // break lfomask_set_main;
                        work.si--;
                    }
                }
//lfomask_set_main:
                byte ah_p = work.ah;
                byte al_p = work.al;
                cy = getnum(/* out */ bx, /* out */ dl);
                work.ah = ah_p;
                work.al = al_p;

                work.dx = (work.ah & 0xff) * 0x100 + (work.dx & 0xff);

                return enmPass2JumpTable.parset;
            }
            case (byte) 'B' -> {
                work.ah = (byte) 0xbf;
                //break lfoset_main;
            }
            case (byte) 'A' -> {
                work.ah = (byte) 0xf2;
//            break lfoset_main;
            }
            default -> {
                work.si--;
            }
        }
//lfoset_main:

        m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));

        int ax_p = (work.ah & 0xff) * 0x100 + (work.al & 0xff); // ah(A/B)保存
        work.dx = 'M' * 0x100 + (work.dx & 0xff);
        get_clock(); //delay
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        work.ah = (byte) (ax_p >> 8);
        work.al = (byte) ax_p;

        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);

        if (work.al == (byte) ',') { // break delay_only;

            cy = getnum(/* out */ bx, /* out */ dl); // speed
            m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

            work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);

            if (work.al != (byte) ',') {
                error('M', 6, work.si);
            }

            cy = getnum(/* out */ bx, /* out */ dl); // depth1
            m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

            work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);

            if (work.al != (byte) ',') {
                error('M', 6, work.si);
            }

            cy = getnum(/* out */ bx, /* out */ dl); // depth2
            m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

            return enmPass2JumpTable.olc0;
        }
//delay_only:

        work.si--;
        if (work.ah == (byte) 0xf2) { // break delay_only2;

            m_seg.m_buf.set(work.di - 2, new MmlDatum(0xc2));
            return enmPass2JumpTable.olc0;
        }
//delay_only2:

        m_seg.m_buf.set(work.di - 2, new MmlDatum(0xb9));
        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "*" lfo switch
     */
    private enmPass2JumpTable lfoswitch() {
        byte ah_p;
        byte al_p;
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        char ch;

        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        work.ah = (byte) 0xf1;
//#if !efc
        if (mml_seg.part != mml_seg.rhythm) { // break lfoswitch_noB; // R part
//#endif
            work.ah = (byte) 0xbe;
        }
//lfoswitch_noB:
        if (work.al != 'B') { // break lfoswitch_main;
            work.ah = (byte) 0xf1;
            if (work.al != 'A') { // break lfoswitch_main;
                work.si--;
            }
        }
//lfoswitch_main:
        ah_p = work.ah;
        al_p = work.al;
        cy = lngset(/* out */ bx, /* out */ al);
        work.bx = (ah_p & 0xff) * 0x100 + (al_p & 0xff);
        if (cy) {
            error('*', 6, work.si);
        }
        work.ah = work.al;
        work.al = (byte) (work.bx >> 8);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));

        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != ',') return enmPass2JumpTable.olc0;

        work.si++;

        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
//#if !efc
        if (mml_seg.part == mml_seg.rhythm) {
            error('*', 17, work.si); // R part
        }
//#endif
        work.ah = (byte) 0xf1;
        if (work.al != 'A') { // break lfoswitch_main2;
            work.ah = (byte) 0xbe;
            if (work.al != 'B') { // break lfoswitch_main2;
                work.si--;
            }
        }
//lfoswitch_main2:
        ah_p = work.ah;
        al_p = work.al;
        cy = lngset(/* out */ bx, /* out */ al);
        work.bx = (ah_p & 0xff) * 0x100 + (al_p & 0xff);
        if (cy) {
            error('*', 6, work.si);
        }

        byte d = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
        if (ah_p != d) { // cmp bh,-2[di] ;対象が同じ? // break lsm2_0;
            work.si -= 2; // 後半が有効
        }
//lsm2_0:
        work.ah = work.al;
        work.al = (byte) ((work.bx & 0xff00) >> 8);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(work.ah & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "E" PSG Software_envelope
     */
    private enmPass2JumpTable psgenvset() {
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch == 'X') return extenv_set();

        m_seg.m_buf.set(work.di++, new MmlDatum(0xf0));
        int cx = 3;
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

//pe0:;
        do {
            cy = getnum(/* out */ bx, /* out */ dl);
            m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

            ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
            if (ch != ',') {
                error('E', 6, work.si);
            }
            cx--;
        } while (cx > 0);

        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != ',') { // break extend_psgenv;
            return enmPass2JumpTable.olc0;
        }
        // 4.0h Extended
//extend_psgenv:
        m_seg.m_buf.set(work.di - 5, new MmlDatum(0xcd));
        work.si++;
        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di - 1, new MmlDatum(m_seg.m_buf.get(work.di - 1).dat & 0xf));
        dl[0] <<= 4;
        dl[0] &= 0xf0;
        m_seg.m_buf.set(work.di - 1, new MmlDatum(m_seg.m_buf.get(work.di - 1).dat | (dl[0] & 0xff)));
        dl[0] = 0;

        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch == ',') { // break not_set_al;
            work.si++;
            cy = getnum(/* out */ bx, /* out */ dl);
        }
//not_set_al:
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        work.dx = (work.dx & 0xff00) | dl[0];
        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "EX" Envelope Speed Extended Mode Set
     */
    private enmPass2JumpTable extenv_set() {
        work.si++;
        m_seg.m_buf.set(work.di++, new MmlDatum(0xc9));

        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];
        cy = getnum(/* out */ bx, /* out */ dl);

        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));

        return enmPass2JumpTable.olc0;
    }

    //6756-6775
    /**
    // COMMAND "y" OPN Register set
     */
    private enmPass2JumpTable ycommand() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xef));

        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1];
        cy = lngset(/* out */ bx, /* out */ al);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));

        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        if (work.al != (byte) ',') {
            error('y', 6, work.si);
        }

        cy = lngset(/* out */ bx, /* out */ al);
        m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));

        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "w" PSG noise 平均周波数設定
     */
    private enmPass2JumpTable psgnoise() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '+') { // break psgnoise_move;
            if (ch != '-') { // break psgnoise_move;

                m_seg.m_buf.set(work.di++, new MmlDatum(0xee));

                cy = lngset(/* out */ bx, /* out */ al);

                m_seg.m_buf.set(work.di++, new MmlDatum(al[0] & 0xff));

                return enmPass2JumpTable.olc0;
            }
        }
//psgnoise_move:
        m_seg.m_buf.set(work.di++, new MmlDatum(0xd0));
        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        return enmPass2JumpTable.olc0;
    }

    /**
    // COMMAND "P" PSG tone/noise/mix Select
     */
    private enmPass2JumpTable psgpat() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xed));
        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
        if (cy) {
            error('P', 6, work.si);
        }

        if (work.al >= 4) {
            error('P', 2, work.si);
        }

        byte ah = work.al;
        ah &= 2;
        work.al &= 1;
        ah <<= 2;
        work.al |= ah;

        ah = work.al;
        work.al <<= 1;
        work.al |= ah;
        ah = work.al;
        work.al <<= 1;
        work.al |= ah;

        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        return enmPass2JumpTable.olc0;
    }

    //6834-6850
    /**
    // COMMAND "B" ベンド幅の設定
     */
    private enmPass2JumpTable bendset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

        cy = getnum(/* out */ bx, /* out */ dl);
        if (dl[0] >= 13) {
            error('B', 2, work.si);
        }

        mml_seg.bend = dl[0];
        if (dl[0] != 0) return enmPass2JumpTable.olc03;

        mml_seg.alldet = 0x8000; // ０が指定されたらalldet値を初期化
        return enmPass2JumpTable.olc03;
    }

    //6851-6858
    /**
    // COMMAND "I" ピッチの設定
     */
    private enmPass2JumpTable pitchset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

        cy = getnum(/* out */ bx, /* out */ dl);
        mml_seg.pitch = work.bx;

        return enmPass2JumpTable.olc03;
    }

    //6859-6891
    /**
    // COMMAND "p" パンの設定
     */
    private enmPass2JumpTable panset() {
        boolean cy;
        int[] bx = new int[1];
        byte[] dl = new byte[1];

        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != 'x') { // break panset_extend;
            cy = getnum(/* out */ bx, /* out */ dl);
            if (work.bx >= 4) {
                error('p', 2, work.si);
            }

            m_seg.m_buf.set(work.di++, new MmlDatum(0xec, MMLType.Pan, MakeLinePos(), work.bx));
            m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));

            return enmPass2JumpTable.olc0;
        }
//panset_extend:
        work.si++;
        cy = getnum(/* out */ bx, /* out */ dl);
        int prm1 = dl[0];
        m_seg.m_buf.set(work.di++, new MmlDatum(0xc3, MMLType.Pan, MakeLinePos(), prm1, 0));
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        m_seg.m_buf.set(work.di++, new MmlDatum(0));

        ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != ',') return enmPass2JumpTable.olc0;
        work.si++;
        cy = getnum(/* out */ bx, /* out */ dl);
        if (dl[0] == 0) return enmPass2JumpTable.olc0;

        m_seg.m_buf.set(work.di - 3, new MmlDatum(0xc3, MMLType.Pan, MakeLinePos(), prm1, 1));
        m_seg.m_buf.set(work.di - 1, new MmlDatum(1));
        return enmPass2JumpTable.olc0;
    }

    //6892-7091
    /**
    // COMMAND "\" リズム音源コントロール
     */
    private Tuple<Character, Supplier<enmPass2JumpTable>>[] rcomtbl;

    private void setupRcomtbl() {
        rcomtbl = new Tuple[] {
                new Tuple<Character, Supplier<enmPass2JumpTable>>('V', this::mstvol)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('v', this::rthvol)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('l', this::panlef)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('m', this::panmid)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('r', this::panrig)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('b', this::bdset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('s', this::snrset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('c', this::cymset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('h', this::hihset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('t', this::tamset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>('i', this::rimset)
                , new Tuple<Character, Supplier<enmPass2JumpTable>>((char) 0, null)
        };
    }

    private enmPass2JumpTable rhycom() {
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        work.bx = 0; // offset rcomtbl
        //rc00:;
        do {
            if (rcomtbl[work.bx].getItem1() == 0) error('\\', 1, work.si);
            if (work.al == rcomtbl[work.bx].getItem1()) break; // rc01;
            work.bx++;
        } while (true);
//rc01:
        return rcomtbl[work.bx].getItem2().get();
        //return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable mstvol() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '+') { // break mstvol_sft;
            if (ch != '-') { // break mstvol_sft;
                m_seg.m_buf.set(work.di++, new MmlDatum(0xe8));

                cy = lngset(/* out */ bx, /* out */ al);
                if (cy) {
                    error('\\', 6, work.si);
                }

                if ((byte) work.bx >= 64) {
                    error('\\', 2, work.si);
                }

                m_seg.m_buf.set(work.di++, new MmlDatum(work.bx & 0xff));
                return enmPass2JumpTable.olc0;
            }
        }
//mstvol_sft:
        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di++, new MmlDatum(0xe6));
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable rthvol() {
        boolean cy;
        int[] bx = new int[1];
        byte[] al = new byte[1], dl = new byte[1];

        rhysel();

        int ax_p = (work.ah & 0xff) * 0x100 + (work.al & 0xff);

        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != '+') { // break rhyvol_sft;
            if (ch != '-') { // break rhyvol_sft;
                cy = lngset(/* out */ bx, /* out */ al);

                work.al = (byte) (ax_p & 0xff);
                work.ah = (byte) ((ax_p & 0xff00) >> 8);

                if (cy) {
                    error('\\', 6, work.si);
                }

                if ((byte) work.bx >= 32) {
                    error('\\', 2, work.si);
                }

                m_seg.m_buf.set(work.di++, new MmlDatum(0xea));

                return rc02();
            }
        }
//rhyvol_sft:
        work.al = (byte) ax_p;
        work.ah = (byte) (ax_p >> 8);

        m_seg.m_buf.set(work.di++, new MmlDatum(0xe5));
        work.al &= (byte) 0b1110_0000;
        work.al >>= 5;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));

        cy = getnum(/* out */ bx, /* out */ dl);
        m_seg.m_buf.set(work.di++, new MmlDatum(dl[0] & 0xff));
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable rc02() {
        work.al &= (byte) 0b1110_0000;
        work.bx &= 0xff1f;
        work.al |= (byte) work.bx;
        m_seg.m_buf.set(work.di++, new MmlDatum(work.al & 0xff));
        return enmPass2JumpTable.olc0;
    }

    private enmPass2JumpTable panlef() {
        work.bx = (work.bx & 0xff00) + 2;
        return rpanset();
    }

    private enmPass2JumpTable panmid() {
        work.bx = (work.bx & 0xff00) + 3;
        return rpanset();
    }

    private enmPass2JumpTable panrig() {
        work.bx = (work.bx & 0xff00) + 1;
        return rpanset();
    }

    private enmPass2JumpTable rpanset() {
        m_seg.m_buf.set(work.di++, new MmlDatum(0xe9));
        rhysel();
        return rc02();
    }

    private void rhysel() {
        work.al = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
        work.bx = 0x0100 + (byte) work.bx;
        if (work.al != 'b') { // break rsel;
            work.bx += 0x100;
            if (work.al != 's') { // break rsel;
                work.bx += 0x100;
                if (work.al != 'c') { // break rsel;
                    work.bx += 0x100;
                    if (work.al != 'h') { // break rsel;
                        work.bx += 0x100;
                        if (work.al != 't') { // break rsel;
                            work.bx += 0x100;
                            if (work.al != 'i') { // break rsel;

                                error('\\', 1, work.si);
                            }
                        }
                    }
                }
            }
        }
//rsel:
        work.al = (byte) (work.bx >> 8);
        work.al <<= 5; //0000_0111 -> 1110_0000
    }

    private enmPass2JumpTable bdset() {
        work.al = 1;
        return rs00();
    }

    private enmPass2JumpTable snrset() {
        work.al = 2;
        return rs00();
    }

    private enmPass2JumpTable cymset() {
        work.al = 4;
        return rs00();
    }

    private enmPass2JumpTable hihset() {
        work.al = 8;
        return rs00();
    }

    private enmPass2JumpTable tamset() {
        work.al = 16;
        return rs00();
    }

    private enmPass2JumpTable rimset() {
        work.al = 32;
        return rs00();
    }

    private enmPass2JumpTable rs00() {
        if (mml_seg.skip_flag == 0) { // break rs_skip;

            boolean flag_rsexit = false; // gross
            char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (ch == 'p') { // break rs01;
                work.si++;
                work.al |= 0x80;
//                break rs02;
            } else {
//rs01:
                var o = m_seg.m_buf.get(work.di - 2);
                if (o != null) { // break rs02;
                    byte cch = (byte) (m_seg.m_buf.get(work.di - 2).dat & 0xff);
                    if (cch == (byte) 0xeb) { // break rs02;
                        cch = (byte) (m_seg.m_buf.get(work.di - 1).dat & 0xff);
                        if ((cch & 0x80) == 0) { // break rs02;
                            if (mml_seg.prsok == 0x80) { // break rs02; // 直前byte = リズム?
                                work.al |= cch;
                                m_seg.m_buf.set(work.di - 1, new MmlDatum(work.al & 0xff));
//                                break rsexit;
                                flag_rsexit = true;
                            }
                        }
                    }
                }
            }
//rs02:
            if (!flag_rsexit) {
                m_seg.m_buf.set(work.di, new MmlDatum(0xeb));
                m_seg.m_buf.set(work.di + 1, new MmlDatum(work.al & 0xff));
                work.di += 2;
                if ((work.al & 0x80) != 0) { // break rsexit;
                    return enmPass2JumpTable.olc0;
                }
            }
//rsexit:
            mml_seg.prsok = 0x80; // 直前byte = リズム
            return enmPass2JumpTable.olc02;
        }
//rs_skip:
        char ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
        if (ch != 'p') return enmPass2JumpTable.olc03;
        work.si++;
        return enmPass2JumpTable.olc03;
    }

    //7092-7163
    /**
    // MML 変数の使用
     */
    private enmPass2JumpTable hscom() {
        char ch;
        int ax;

        int bsi = work.si; // KUMA:Added

        // push es
        //    mov ax, HsSeg
        //    mov es, ax
        //    assume es:HsSeg

        int[] bx = new int[1];
        byte[] al = new byte[1];
        boolean cy = lngset(/* out */ bx, /* out */ al);
        if (!cy) { // break hscom3;
            if ((work.bx & 0xff00) != 0) {
                error('!', 2, work.si);
            }

            //; jnc hscom2
            //; lodsb
            //; cmp al,"!"
            //; jz hscom3
            //; sub al,64
            //; mov dx,"!"*256+7
            //; jc error
            //; cmp al,64
            //; jnc error
            //; xor ah, ah
            //; add ax, ax
            //; mov bx, offset hsbuf
            //; add bx, ax
            //; jmp hscom_main

            //hscom2:;
            ax = work.al * 2;
            work.bx = 0; // offset hsbuf2
            hs_seg.currentBuf = hs_seg.hsbuf2;
            work.bx += ax;
//            break hscom_main;
        } else {
//hscom3:
            ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
            if (ch < '!') {
                error('!', 6, work.si);
            }

            cy = search_hs3();
            if (cy) { // break hscom_main;
                if (work.dx == -1) { // break hscom3_small;

                    error('!', 27, work.si);
                }
            }
//hscom3_small:
            work.bx = work.dx;
        }
//hscom_main:
        //KUMA:Added

        String macroName = "!";
        while (bsi < work.si) macroName += mml_seg.mml_buf.charAt(bsi++);

        if (work.isIDE) {

            //IDE特殊コマンドを出力
            LinePos pos = MakeLinePos();
            List<Object> args = new ArrayList<>();
            MmlDatum cmd = new MmlDatum(MMLType.IDE, args, pos, 0xff);

            m_seg.getMacroLst().add(cmd);

            //public Stack<Tuple<String, Object>> macroStack { get; internal set; }
            //
            LinePos nLp = LinePos.Copy(pos);
            nLp.aliesName = mml_seg.AliesName;
            nLp.aliesNextName = macroName;
            nLp.aliesDepth = mml_seg.getMacroStack().size() + 1;
            nLp.col--;
            nLp.row--;
            mml_seg.getMacroStack().push(nLp);
            mml_seg.AliesName = macroName;

            List<Object> tArgs = new ArrayList<>();
            tArgs.add(mml_seg.getMacroStack().toArray());
            cmd = new MmlDatum(MMLType.TraceUpdateStack, tArgs, pos, 0xff);
            args.add(cmd);
        }

        ax = (hs_seg.currentBuf[work.bx] & 0xff) + (hs_seg.currentBuf[work.bx + 1] & 0xff) * 0x100;

        //    assume es:MSeg

        if (ax == 0) {
            error('!', 27, work.si); // 定義されてないってばさ
        }

        mml_seg.hscomSI.push(work.si);
        mml_seg.hsflag++;
        work.si = ax;
        calc_line(/* ref */ ax);
        return enmPass2JumpTable.olc02; // olc0
    }

    private enmPass2JumpTable hscom_exit() {
        mml_seg.hsflag--;
        work.si = mml_seg.hscomSI.pop();
        int ax = work.si;
        calc_line(/* ref */ ax);

        //KUMA:Added

        if (work.isIDE) {

            //IDE特殊コマンドを出力
            LinePos pos = MakeLinePos();
            List<Object> args = new ArrayList<>();
            MmlDatum cmd = new MmlDatum(MMLType.IDE, args, pos, 0xff);

            m_seg.getMacroLst().add(cmd);

            LinePos nlp = mml_seg.getMacroStack().pop();
            mml_seg.AliesName = nlp.aliesName;

            List<Object> tArgs = new ArrayList<>();
            tArgs.add(mml_seg.getMacroStack().toArray());
            cmd = new MmlDatum(MMLType.TraceUpdateStack, tArgs, pos, 0xff);
            args.add(cmd);

        }

        return enmPass2JumpTable.olc03;
    }

    /**
    // 可変長変数検索
    //  in. ds:si mml_buffer
    //         es HsSeg
    //  out.bx hs3_offset
    //         ds:si next mml_buffer
    //   cy=1 no_match dx = near hs3_offset
     */
    private boolean search_hs3() {
        int di_p = work.di;

        work.al = 0;
        byte ah = 0;
        work.dx = -1;
        work.bx = 0; // offset hsbuf3
        hs_seg.currentBuf = hs_seg.hsbuf3;
        work.di = work.bx + 2;
        int cx = 256;
//hscom3_loop:
hscom3_found: // ↑
        {
            do {
                int cx_p = cx;
                int si_p = work.si;
hscom3_next: // ↑
                if ((hs_seg.currentBuf[work.bx] | hs_seg.currentBuf[work.bx + 1]) != 0) { // break hscom3_next;
                    cx = hs_seg.hs_length - 2;
                    work.al = 0;
//hscom3_loop2:
                    char ch;
hscom3_next2: // ↑
                    {
hscom3_chk: // ↑
                        {
                            do {
                                ch = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si) : (char) 0x1a;
                                if (ch < '!') break hscom3_chk;
                                if (hs_seg.currentBuf[work.di] == 0) break hscom3_next2; // 変数定義側が先に終わった場合は最小確認

                                byte s = (byte) (work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a);
                                byte d = hs_seg.currentBuf[work.di++];
                                if (s != d) break hscom3_next;

                                work.al++;
                                cx--;
                            } while (cx > 0);
//hscom3_loop3:;
                            char alc;
                            do {
                                alc = work.si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(work.si++) : (char) 0x1a;
                            } while (alc >= '!');
                            work.si--;
                            break hscom3_found;
                        }
//hscom3_chk:
                        if (hs_seg.currentBuf[work.di] == 0) break hscom3_found;
                        break hscom3_next;
                    }
//hscom3_next2:
                    if (ah < work.al) { // break hscom3_next;
                        ah = work.al;
                        work.dx = work.bx;
                    }
                }
//hscom3_next:
                work.si = si_p;
                cx = cx_p;

                work.bx += hs_seg.hs_length;
                work.di = work.bx + 2;
                cx--;
            } while (cx > 0);
            work.di = di_p;

            work.al = ah;
            work.si += work.al;

            //    stc
            return true;
        }
//hscom3_found:
        //ax = si_p;
        //cx = cx_p;
        work.di = di_p;
        //    clc
        return false;
    }

    /**
    // ERRORの表示
    //  input dl      ERROR_NUMBER
    //   dh ERROR Command 0なら不定
    //   si ERROR address 0なら不定
    //   [part] part番号 0なら不定
     */
    private void error(int dh, int dl, int si) {
        calc_line(/* ref */ si);

        StringBuilder mes = new StringBuilder();
        //  
        // filename,lineの表示
        //  
        if (si != 0) {
            try {
                mes.append(Path.getFileName(mml_seg.mml_filename));
            } catch (Exception e) {
                mes.append(mml_seg.mml_filename);
            }
            mes.append(String.format("(%d) :", mml_seg.line));
        }

        //  
        // Error番号の表示
        //  
        mes.append(ErrSeg.errmes_1);
        mes.append(dl);

        //  
        // Partの表示
        //  
        put_part();

        //  
        // Commandの表示
        //  
        if (dh != 0) {
            mes.append(ErrSeg.errmes_3);
            mes.append((char) dh);
        }

        if (mes != null && (!mes.isEmpty())) print_mes(mes.toString());

        //  
        // Error Messageの表示
        //  
        print_mes_err(ErrSeg.errmes_4 + rb.getString("E01%02d".formatted(dl))); // ErrSeg.err_table[dl]);

        //  
        // エラー箇所の表示
        //  
        if (si != 0 && mml_seg.line != 0) {
            mes = new StringBuilder();
            int di = mml_seg.linehead;
            while (di < mml_seg.mml_buf.length() && mml_seg.mml_buf.charAt(di) != Mc.cr) {
                mes.append(mml_seg.mml_buf.charAt(di++));
            }
            print_mes(mes.toString());
            int s = si - 1 - mml_seg.linehead;
            if (s >= 0) print_mes(" ".repeat(s) + "^^");
        }

        error_exit(1);
    }

    /**
    // Error,Warning時のパート表示
     */
    private void put_part() {
        if (mml_seg.part == 0) return;

        print_mes(ErrSeg.errmes_2 + (char) ('A' - 1 + mml_seg.part)); // 1文字表示

        if (mml_seg.hsflag == 0) return;

//#if !efc
        if (mml_seg.part != mml_seg.rhythm) {
            print_mes(ErrSeg.errmes_5);
            return;
        }
//#endif
        if (mml_seg.hsflag == 1) return;

        print_mes(ErrSeg.errmes_5);
    }

    /**
    // Error位置のline,lineheadを計算
    //  input DS:SI Error位置
    // output[line] Line
    //[linehead] Lineの頭位置
    //   [mml_filename] MMLのファイル名
     */
    private void calc_line(/* ref */ int si) {
        if (si == 0) {
            mml_seg.line = 0;
            mml_seg.linehead = 0;
            si = 0;
            return;
        }

        mml_seg.includeFileHistoryPos = 0;
        mml_seg.currentMMLFile = mml_seg.includeFileHistory.get(mml_seg.includeFileHistoryPos);

        Stack<Integer> lineStack = new Stack<>();
        int bx = 0;
        int ah = 0; // Main/Include Flag
        int dx = si; // DX=Error位置
        si = 0; // offset mml_buf

        mml_seg.line = 1;

cl_exit:
        do {
            mml_seg.linehead = si;
            if (si == dx) break; //１文字目でerrorの場合

            do {
                char al = si < mml_seg.mml_buf.length() ? mml_seg.mml_buf.charAt(si++) : (char) 0x1a;
                if (si == dx) break cl_exit; // Error位置まで来たか？

                if (al == 0x1a) // EOF
                {
                    mml_seg.line = 0;
                    mml_seg.linehead = 0;
                    si = 0;
                    return;
                }

                if (al >= 0x1a) continue;

                if (al == 13) // CR
                {
                    si++; // LFを飛ばす
                    mml_seg.line++;
                    break;
                }

                if (al == 1) // Main->Include check code
                {
                    lineStack.push(mml_seg.line); // Lineを保存
                    lineStack.push(bx); // MMLのファイル名位置を保存
                    mml_seg.line = 1; // 1行目から
                    ah++; // Include階層を一つ増やす
                    bx = si + 1; // MMLのファイル名位置をBXに保存
                    //do
                    //{
                    //al = si < MmlSeg.mml_buf.length() ? MmlSeg.mml_buf.charAt(si++) : (char)0x1a;
                    //} while (al != 0x0a); // ファイル名部分を飛ばす

                    mml_seg.includeFileHistoryStack.push(mml_seg.currentMMLFile);
                    mml_seg.currentMMLFile = mml_seg.includeFileHistory.get(++mml_seg.includeFileHistoryPos);

                    break;
                } else if (al == 2) // Include->Main check code
                {
                    bx = lineStack.pop(); // MMLのファイル名位置を元に戻す
                    mml_seg.line = lineStack.pop(); // Line位置を元に戻す
                    ah--; // Include階層を一つ減らす

                    si++;
                    mml_seg.currentMMLFile = mml_seg.includeFileHistoryStack.pop();
                    break;
                }

            } while (true);
        } while (true);

        if (ah == 0) {
            si = dx; // Error位置をSIに戻す
            mml_seg.mml_filename = mml_seg.currentMMLFile;
            return;
        }

        si = bx;
        mml_seg.mml_filename = mml_seg.currentMMLFile;

        //while (MmlSeg.mml_buf.charAt(si) != 'e' && MmlSeg.mml_buf.charAt(si) != 'E') { // includeをスキップする
        //    si++;
        //}
        //si++;

        //MmlSeg.mml_filename = ""; //offset mml_filename
        //do {
        //    MmlSeg.mml_filename += MmlSeg.mml_buf.charAt(si++); // Include中にError --> MML Filenameを変更
        //} while (si != MmlSeg.mml_buf.length() && MmlSeg.mml_buf.charAt(si) >= 0x20);
        //si = dx; // Error位置をSIに戻す
        //MmlSeg.mml_filename = MmlSeg.mml_filename.trim();
    }

    /**
    // 環境の検索
    // input si 環境変数名+"="
    //       es 環境segment
    // output es:di 環境のaddress
    // cy 1なら無し
    /**

    /**
     * 環境変数の検索
     *
     * @param siSearchEnv 検索文字(=は不要)
     * @param esKankyoseg 検索対象
     * @param index 見つけた添え字番号
     * @param col 値の始まる位置
     * @return true: 見つけた
     */
    private boolean search_env(String siSearchEnv, String[] esKankyoseg, /* out */ int[] index, /* out */ int[] col) {
        index[0] = -1;
        col[0] = -1;
        if (siSearchEnv == null) return false;
        if (siSearchEnv.isEmpty()) return false;
        if (esKankyoseg == null) return false;
        if (esKankyoseg.length < 1) return false;

        for (index[0] = 0; index[0] < esKankyoseg.length; index[0]++) {
            if (esKankyoseg[index[0]] == null || esKankyoseg[index[0]].isEmpty()) continue;
            if (esKankyoseg[index[0]].indexOf(siSearchEnv) != 0) continue;
            if (esKankyoseg[index[0]].charAt(siSearchEnv.length() - 1) != '=') continue;
            //見つけた
            col[0] = siSearchEnv.length();
            return true;
        }

        //見つからなかった
        index[0] = -1;
        col[0] = -1;
        return false;
    }

    /**
    //  usage put & exit
     */
    private void usage() {
        print_mes(mml_seg.usames);
        error_exit(1);
    }

    private String[] kankyo_seg;
}
