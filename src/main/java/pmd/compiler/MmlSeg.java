package pmd.compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import musicDriverInterface.LinePos;


public class MmlSeg {

    /**
     * //	Work Area
     */
    public static final String warning_mes = "Warning ";
    public static final String not_ff_mes = ": The tone file name is not specified.";
    public final String ff_readerr_mes = ": Cannot load tone file.";
    public final String not_pmd_mes = ": PMD is not resident.";
    public static final String loop_err_mes = " : The loop termination symbol ] is missing.";
    public static final String mcopt_err_mes = ": The environment variable MCOPT is incorrectly written.";

    //#if efc
//		public String usames = "Usage:  EFC [/option] filename[.EML] [filename[.FF]]" + Mc.cr + Mc.lf + Mc.cr + Mc.lf
//		+ "Option: /V  Compile with Tonedatas" + Mc.cr + Mc.lf
//		+ "        /VW Write Voicefile after Compile" + Mc.cr + Mc.lf
//		+ "        /N  Compile on OPN Mode(Default)" + Mc.cr + Mc.lf
//		+ "        /M  Compile on OPM Mode" + Mc.cr + Mc.lf
//		+ "        /L  Compile on OPL Mode" + Mc.cr + Mc.lf + Mc.eof;
//
//		public String titmes = " .EML file --> .EFC file Compiler ver " + Mc.ver
//				+ Mc.cr + Mc.lf
//				+ "		Programmed by M.Kajihara(KAJA) " + Mc.date
//				+ Mc.cr + Mc.lf + Mc.cr + Mc.lf + Mc.eof;
//#else
//#if !hyouka
    public static final String usames = "Usage:  MC" +
            " [/option] filename[.MML] [filename[.FF]]" + Mc.cr + Mc.lf + Mc.cr + Mc.lf +
            "Option: " +
            "/V  Compile with Tonedatas & Messages & Filenames" + Mc.cr + Mc.lf +
            "        /VW Write Tonedata after Compile" + Mc.cr + Mc.lf +
            "        /N  Compile on OPN   Mode(Default)" + Mc.cr + Mc.lf +
            "        /L  (unsupported) Compile on OPL   Mode" + Mc.cr + Mc.lf +
            "        /M  (unsupported) Compile on OPM   Mode" + Mc.cr + Mc.lf +
            "        /T  (unsupported) Compile on TOWNS Mode" + Mc.cr + Mc.lf +
            "        /P  (unsupported) Play after Compile Complete" + Mc.cr + Mc.lf +
            "        /S  (unsupported) Not Write Compiled File & Play" + Mc.cr + Mc.lf +
            "        /A  (unsupported) Not Set ADPCM_File before Play" + Mc.cr + Mc.lf +
            "        /O  (unsupported) Not Put Title Messages after Play" + Mc.cr + Mc.lf +
            "        /C  Calculate & Put Total Length of Parts" + Mc.cr + Mc.lf
            //+ Mc.eof
            ;
    public static final String titmes = " .MML file --> .M file Compiler" +
            " ver " + Mc.ver + Mc.cr + Mc.lf +
            "		Programmed by M.Kajihara(KAJA) " + Mc.date +
            Mc.cr + Mc.lf
            //Mc.cr + Mc.lf +
            //Mc.eof
            ;
//#else
//    public String usames = "Usage:  MCH"
//            + " [/option] filename[.MML] [filename[.FF]]" + Mc.cr + Mc.lf + Mc.cr + Mc.lf
//            + "Option: "
//            + "/N  Compile on OPN   Mode(Default)" + Mc.cr + Mc.lf
//            + "        /L  Compile on OPL   Mode" + Mc.cr + Mc.lf
//            + "        /A  Not Set ADPCM_File before Play" + Mc.cr + Mc.lf
//            + "        /O  Not Put Title Messages after Play" + Mc.cr + Mc.lf
//            + "        /C  Calculate & Put Total Length of Parts" + Mc.cr + Mc.lf
//            + Mc.eof;
//    public String titmes = " .MML file Compiler & Player (MC.EXE evaluation version)"
//            + " ver " + Mc.ver + Mc.cr + Mc.lf
//            + "		Programmed by M.Kajihara(KAJA) " + Mc.date
//            + Mc.cr + Mc.lf
//            //+ Mc.cr + Mc.lf + Mc.eof
//            ;
//#endif
//#endif

    public static final String finmes = "Compile Completed.";
    public String mes_crlf = "" + Mc.cr + Mc.lf + Mc.eof;

    public String mes_title = Mc.cr + Mc.lf + " Start playing." + Mc.cr + Mc.lf + Mc.cr + Mc.lf +
            "Title    : " + Mc.eof;
    public String mes_composer = "Composer : " + Mc.eof;
    public String mes_arranger = "Arranger : " + Mc.eof;
    public String mes_memo = "         : " + Mc.eof;

    public String mes_ppzfile = "PPZFile  : " + Mc.eof;
    public String mes_ppsfile = "PPSFile  : " + Mc.eof;
    public String mes_pcmfile = "PCMFile  : " + Mc.eof;

    public static final String user_txt = "USER=";
    public static final String composer_txt = "COMPOSER=";
    public static final String arranger_txt = "ARRANGER=";
    public static final String mcopt_txt = "MCOPT=";

//#if !tempo_old_flag
    public int tempo = 0;
//#endif
    public int timerb = 0; // b
    public int octave = 4; // b
    public int leng = 0; // b
    public int zenlen = 96; // b
    public int deflng = 24; // b
    public int deflng_k = 24; // b
    public int calflg = 0; // b
    public int hsflag = 0; // b
    public int lopcnt = 0; // b
    public int volss = 0; // b
    public int volss2 = 0; // b
    public int octss = 0; // b
    public int nowvol = 0; // b
    public int line = 0; // w
    public int linehead = 0; // w
    public int length_check1 = 0; // b
    public int length_check2 = 0; // b
    public int allloop_flag = 0; // b
    public int qcommand = 0; // w
    public int acc_adr = 0; // w
    public int jump_flag = 0; // w

    public byte def_a = 0; // b
    public byte def_b = 0; // b
    public byte def_c = 0; // b
    public byte def_d = 0; // b
    public byte def_e = 0; // b
    public byte def_f = 0; // b
    public byte def_g = 0; // b

    public int master_detune = 0; // w
    public int detune = 0; // w
    public int alldet = 0; // w
    public int bend = 0; // b
    public int pitch = 0; // w

    public int bend1 = 0; // b
    public int bend2 = 0; // b
    public int bend3 = 0; // b

    public int transpose = 0; // b

    public int fm_voldown = 0; // b
    public int ssg_voldown = 0; // b
    public int pcm_voldown = 0; // b
    public int rhythm_voldown = 0; // b
    public int ppz_voldown = 0; // b

    public int fm_voldown_flag = 0; // b
    public int ssg_voldown_flag = 0; // b
    public int pcm_voldown_flag = 0; // b
    public int rhythm_voldown_flag = 0; // b
    public int ppz_voldown_flag = 0; // b

    public static final byte[] fmvol = {
            127 - 0x2a,  // VOLUME	00
            127 - 0x28,  // VOLUME	01
            127 - 0x25,  // VOLUME	02
            127 - 0x22,  // VOLUME	03
            127 - 0x20,  // VOLUME	04
            127 - 0x1d,  // VOLUME	05
            127 - 0x1a,  // VOLUME	06
            127 - 0x18,  // VOLUME	07
            127 - 0x15,  // VOLUME	08
            127 - 0x12,  // VOLUME	09
            127 - 0x10,  // VOLUME	10
            127 - 0x0d,  // VOLUME	11
            127 - 0x0a,  // VOLUME	12
            127 - 0x08,  // VOLUME	13
            127 - 0x05,  // VOLUME	14
            127 - 0x02,  // VOLUME	15
            127 - 0x00   // VOLUME	16
    };

//#if !efc

    public int pcm_vol_ext = 0; // b

    // PSG tone patterns
    public final int[][] psgenvdat = {
            {0, 0, 0, 0},     // @0 Standard
            {2, 255, 0, 1},   // @1 Synth 1
            {2, 254, 0, 1},   // @2 Synth 2
            {2, 254, 0, 8},   // @3 Synth 3
            {2, 255, 24, 1},  // @4 E.Piano 1
            {2, 254, 24, 1},  // @5 E.Piano 2
            {2, 254, 4, 1},   // @6 Glocken/Malimba
            {2, 1, 0, 1},     // @7 Strings
            {1, 2, 0, 1},     // @8 Brass 1
            {1, 2, 24, 1}     // @9 Brass 2
    };
    public static final int psgenvdat_max = 9;
    public static final int max_part = 11;
    public static final int fm = 0;
    public int fm2 = 1;
    public static final int psg = 2;
    public static final int pcm = 3;
    public static final int pcm_ex = 4;

//#else

//    public int max_part = 126;

//#endif

    public static final int pcmpart = 10;
    public static final int rhythm2 = 11;
    public static final int rhythm = 18;

    public int part = 0; // b
    public int ongen = 0; // b
    public int pass = 0; // b

    public int maxprg = 0; // b
    public int kpart_maxprg = 0; // b
    public int lastprg = 0; // w

    public int prsok = 0; // b Previous byte
    // bit 1 ・・・ Duration
    // bit 2 ・・・ processing
    // bit 3 ・・・ +tie
    // bit 4 ・・・ Portamento
    // bit 7 ・・・ rhythm

    public int prg_flg = 0; // b
    public int ff_flg = 0; // b
    public int x68_flg = 0; // b
    public int towns_flg = 0; // b
    public int dt2_flg = 0; // b
    public int opl_flg = 0; // b
    public int play_flg = 0; // b
    public int save_flg = 0; // b
    public int pmd_flg = 0; // b
    public int ext_detune = 0; // b
    public int ext_lfo = 0; // b
    public int ext_env = 0; // b
    public int memo_flg = 0; // b
    public int pcm_flg = 0; // b
    public int lc_flag = 0; // b
    public int loop_def = 0; //b

    public int adpcm_flag = 255; // b	-1

    public int sp_push = 0; // w	0

    public int ss_speed = 0; // b	0
    public int ss_depth = 0; // b	0
    public int ss_length = 0; // b	0
    public int ss_tie = 0; // b	0

    public int ge_delay = 0; // b	0
    public int ge_depth = 0; // b	0
    public int ge_depth2 = 0; // b	0
    public int ge_tie = 0; // b	0
    public int ge_flag1 = 0; // b	0
    public int ge_flag2 = 0; // b	0
    public int ge_dep_flag = 0; // b	0

    public int skip_flag = 0; // b	0
    public int tie_flag = 0; // b	0
    public int porta_flag = 0; // b	0

    public int fm3_partchr1 = 0; // b	0
    public int fm3_partchr2 = 0; // b	0
    public int fm3_partchr3 = 0; // b	0
    public int fm3_ofsadr = 0; // w	0
    public final char[] pcm_partchr = {
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0,
            (char) 0
    }; // b	0
    public int pcm_ofsadr = 0; // w	0

    // offset,max,rot
    public static final byte[] oplprg_table = {
            8, 1, 0,   // alg
            8, 7, 1,   // fbl

            4, 15, 4,  // ar
            4, 15, 0,  // dr
            6, 15, 0,  // rr
            6, 15, 4,  // sl
            2, 63, 0,  // tl
            2, 3, 6,   // ksl
            0, 15, 0,  // ml
            0, 1, 4,   // ksr
            0, 1, 5,   // egt
            0, 1, 6,   // vib
            0, 1, 7,   // am

            5, 15, 4,  // ar
            5, 15, 0,  // dr
            7, 15, 0,  // rr
            7, 15, 4,  // sl
            3, 63, 0,  // tl
            3, 3, 6,   // ksl
            1, 15, 0,  // ml
            1, 1, 4,   // ksr
            1, 1, 5,   // egt
            1, 1, 6,   // vib
            1, 1, 7    // am
    };

    public int mml_endadr; // w

    public int loopnest = 32; // MAX 32 NEST
    public final byte[] loptbl = new byte[32 * 2]; // loopnest * 2];
    public final byte[] lextbl = new byte[32 * 2]; // loopnest * 2];

    // Broken Chord Work
    public int bunsan_start; // w Start Offset
    public byte bunsan_count; // b Number of notes
    public final byte[] bunsan_work = new byte[16]; // Scale x16
    public byte bunsan_length; // b Overall length
    public byte bunsan_1cnt; // b The duration of one note
    public byte bunsan_tieflag; // b Tie Flag
    public byte bunsan_1loop; // b Length of one loop
    public byte bunsan_gate; // b Gate
    public byte bunsan_vol; // b Volume ±

    //prgbuf_start label   byte // A structure maybe?

    public int prgbuf_length = 26;
    public byte newprg_num; // b
    public byte alg_fb; // b
    public final byte[][] slot = {new byte[6], new byte[6], new byte[6], new byte[6]}; // b
    public String prg_name = null; // b

    public final byte[] oplbuf = new byte[16]; // b
    public final byte[] prg_num = new byte[256]; // b

    public String mml_filename; // b
    public String mml_filename2 = ""; // For include
    public int ppzfile_adr; // w
    public int ppsfile_adr; // w
    public int pcmfile_adr; // w
    public int title_adr; // w
    public int composer_adr; // w
    public int arranger_adr; // w
    public final int[] memo_adr = new int[129]; // w
    public String composer_seg = null; // w
    public String arranger_seg = null; // w
    //public String composer_adr; // w
    //public String arranger_adr; // w

    public String mml_buf = null; // 61*1024-1 dup(?); max 61k(.mml file)
    public byte mmlbuf_end; // b

    public final Stack<Integer> hscomSI = new Stack<>();
    byte ontei;
    int stPos;
    int chipCh;

    public final List<String> includeFileHistory = new ArrayList<>();
    public int includeFileHistoryPos;
    public Object currentDocument;
    public String currentMMLFile;
    public final Stack<String> includeFileHistoryStack = new Stack<>();

    private Stack<LinePos> macroStack = new Stack<>();

    public Stack<LinePos> getMacroStack() {
        return macroStack;
    }

    public void getMacroStack(Stack<LinePos> value) {
        macroStack = value;
    }

    public String AliesName = "";

//    public Stack<Integer> includeFileLineStack = new Stack<Integer>();

//    MmlSeg ends
}
