package pmd.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dotnet4j.util.compat.Tuple;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;


public class PW {

    public final Object lockObj = new Object();
    public final Object SystemInterrupt = new Object();

    private int _status = 0;

    public int getStatus() {
        synchronized (lockObj) {
            return _status;
        }
    }

    public void setStatus(int value) {
        synchronized (lockObj) {
            _status = value;
        }
    }

    int maxLoopCount = -1;

    public int getmaxLoopCount() {
        return maxLoopCount;
    }

    int nowLoopCounter = -1;

    public int getnowLoopCounter() {
        return nowLoopCounter;
    }

    //DotNET独自
    MmlDatum[] md;

    public MmlDatum[] getMd() {
        return md;
    }

    MmlDatum[] crtEfcDat;

    public MmlDatum[] getCrtEfcDat() {
        return crtEfcDat;
    }

    public byte[] pcmWk = new byte[4 * 256 + 2 + 128];
    public byte[] pcmDt;

    Supplier<Object>[] currentCommandTable;

    public Supplier<Object>[] getCurrentCommandTable() {
        return currentCommandTable;
    }

    public MmlDatum[] inst = null;
    public boolean usePPSDRV = false;
    public boolean useP86DRV = false;
    public OPNATimer timer = null;
    public long timeCounter = 0L;
    public String[] pmdOption = null;
    public String ppsFile = "";
    public String ppcFile = "";
    public String ppz1File = "";
    public String ppz2File = "";

    //PMD.ASM 7-53
    public final String ver = "4.8s";
    public int vers = 0x48;
    public char verc = 's';
    public final String date = "Jan.22nd 2020";

    public int mdata_def = 16;
    public int voice_def = 8;
    public int effect_def = 4;
    public int key_def = 1;

    public String _myname = "PMD     COM";

    public int va = 0; // １の時ＶＡMSDOS用
    public int board2 = 0; // １の時ボード２/音美ちゃん有り
    public int adpcm = 0; // １の時ADPCM使用
    public int ademu = 0; // １の時ADPCM Emulate
    public int pcm = 0; // １の時PCM使用
    public int ppz = 0; // １の時PPZ8使用
    public int sync = 0; // １の時MIDISYNC使用
    public int vsync = 0; // １の時VSyncを止める
    public String resmes = "PMD ver." + ver;
    public int fmvd_init = 16; // ９８は８８よりもFM音源を小さく

    //PMD.ASM 114-116
    public int pmdvector = 0x60; // PMD用の割り込みベクトル
    public int ppsdrv = 0x64; // ppsdrvの割り込みベクトル
    public int ppz_vec = 0x7f; // ppz8の割り込みベクトル

    //PMD.ASM 260-277
    //==============================================================================
    //	定数
    //==============================================================================
    public int ms_cmd = 0x000; // ８２５９マスタポート
    public int ms_msk = 0x002; // ８２５９マスタ／マスク
    public int sl_cmd = 0x008; // ８２５９スレーブポート
    public int sl_msk = 0x00a; // ８２５９スレーブ／マスク

    //PMD.ASM 279-306
    //==============================================================================
    //	Program Start
    //==============================================================================
    //int60_head:	jmp short int60_main
    //db	'PMD'	;+2  常駐チェック用
    //db  vers	;+5
    //db verc;+6
    public int int60ofs;//	?	;+7
    public int int60seg;//	?	;+9
    public int int5ofs;//	?	;+11
    public int int5seg;//	?	;+13
    public int maskpush;//	?	;+15
    public int vector;//	?	;+16
    public int int_level;//	?	;+18

    public int _p = 2;
    public int _m = 3;
    public int _d = 4;
    public int _vers = 5;
    public int _verc = 6;
    public int _int60ofs = 7;
    public int _int60seg = 9;
    public int _int5ofs = 11;
    public int _int5seg = 13;
    public int _maskpush = 15;
    public int _vector = 16;
    public int _int_level = 18;


    //PMD.ASM 2077
    public byte com_end_0c0h = (byte) 0xf7;


    //PMD.ASM 4859
    public int[] vol_tbl = {0, 0, 0, 0};


    //PMD.ASM 5560
    public short seed;

    //7972-8029
    //==============================================================================
    //	WORK AREA
    //==============================================================================
    public short fm_port1; // w	FM音源 I/O port Work(1)
    public short fm_port2; // w FM音源 I/O port Work(2)
    public short ds_push; // w INT60用 ds push
    public short dx_push; // w INT60用 dx push
    public byte ah_push; // b INT60用 ah push
    public byte al_push; // b INT60用 al push
    public byte partb; // b 処理中パート番号
    public byte tieflag; // b &のフラグ
    public byte volpush_flag; // b 次の１音音量down用のflag
    public byte rhydmy; // b R part ダミー演奏データ
    public byte fmsel; // b FM 表か裏か flag
    public byte[] fmKeyOnDataTbl = new byte[6]; // KUMA: 以下６つのパラメータの実体
    //public int[] omote_key = { 0, 0, 0 };
    public byte omote_key1Ptr = 0; // b FM keyondata表1
    public byte omote_key2Ptr = 1; // b  FM keyondata表2
    public byte omote_key3Ptr = 2; // b FM keyondata表3
    //public int[] ura_key = { 0, 0, 0 };
    public byte ura_key1Ptr = 3; // b FM keyondata裏1
    public byte ura_key2Ptr = 4; // b FM keyondata裏2
    public byte ura_key3Ptr = 5; // b FM keyondata裏3
    public byte loop_work; // b Loop Work
    public byte ppsdrv_flag; // b ppsdrv flag
    public short prgdat_adr2; // w 曲データ中音色データ先頭番地(効果音用)
    public short pcmrepeat1; // w PCMのリピートアドレス1
    public short pcmrepeat2; // w PCMのリピートアドレス2
    public short pcmrelease; // w PCMのRelease開始アドレス
    public byte lastTimerAtime; // b 一個前の割り込み時のTimerATime値
    public byte music_flag; // b B0:次でMSTART 1:次でMSTOP のFlag
    public byte slotdetune_flag; // b FM3 Slot Detuneを使っているか
    public byte slot3_flag; // b FM3 Slot毎 要効果音モードフラグ
    public short eoi_adr; // w EOIをsendするI/Oアドレス
    public byte eoi_data; // b EOI用のデータ
    public short mask_adr; // w MaskをするI/Oアドレス
    public byte mask_data; // b Mask用のデータ(OrでMask)
    public byte mask_data2; // b Mask用のデータ(AndでMask解除)
    public short ss_push; // w FMint中 SSのpush
    public short sp_push; // w FMint中 SPのpush
    public byte fm3_alg_fb; // b FM3chの最後に定義した音色のalg/fb
    public byte af_check; // b FM3chのalg/fbを設定するかしないかflag
    public byte ongen; // b 音源 0=無し/2203 1=2608
    public byte lfo_switch; // b	局所LFOスイッチ

    public byte[] rhydat = { // ドラムス用リズムデータ
            //PT PAN/VOLUME  KEYON
            0x18, (byte) 0b1101_1111, 0b0000_0001, // バス
            0x19, (byte) 0b1101_1111, 0b0000_0010, // スネア
            0x1c, 0b0101_1111, 0b0001_0000, // タム[LOW]
            0x1c, (byte) 0b1101_1111, 0b0001_0000, // タム[MID]
            0x1c, (byte) 0b1001_1111, 0b0001_0000, // タム[HIGH]
            0x1d, (byte) 0b1101_0011, 0b0010_0000, // リム
            0x19, (byte) 0b1101_1111, 0b0000_0010, // クラップ
            0x1b, (byte) 0b1001_1100, (byte) 0b1000_1000, // Cハイハット
            0x1a, (byte) 0b1001_1101, 0b0000_0100, // Oハイハット
            0x1a, (byte) 0b1101_1111, 0b0000_0100, // シンバル
            0x1a, 0b0101_1110, 0b0000_0100, // RIDEシンバル
    };

    //PMD.ASM 8030-
    public byte open_work = 0; // label byte
    public int mmlbuf = 0; // Musicdataのaddress+1
    public int tondat = 0; // Voicedataのaddress
    public int efcdat = -1; // FM Effecdataのaddress
    public int fm1_port1 = 0; // FM音源 I/O port(表1)
    public int fm1_port2 = 0; // FM音源 I/O port(表2)
    public int fm2_port1 = 0; // FM音源 I/O port(裏1)
    public int fm2_port2 = 0; // FM音源 I/O port(裏2)
    public int fmint_ofs = 0; // FM割り込みフックアドレス offset
    public int fmint_seg = 0; // FM割り込みフックアドレス address
    public int efcint_ofs = 0; // 効果音割り込みフックアドレス offset
    public int efcint_seg = 0; // 効果音割り込みフックアドレス address
    public int prgdat_adr = 0; // 曲データ中音色データ先頭番地
    public int radtbl = 0; // R part offset table 先頭番地
    public int rhyadr = 0; // R part 演奏中番地
    public byte rhythmmask = 0; // Rhythm音源のマスク x8c/10hのbitに対応
    public byte board = 0; // FM音源ボードあり／なしflag
    public byte key_check = 0; // ESC/GRPH key Check flag
    public byte fm_voldown = 0; // FM voldown 数値
    public byte ssg_voldown = 0; // PSG voldown 数値
    public byte pcm_voldown = 0; // PCM voldown 数値
    public byte rhythm_voldown = 0; // RHYTHM voldown 数値
    public byte prg_flg = 0; // 曲データに音色が含まれているかflag
    public byte x68_flg = 0; // OPM flag
    public byte status = 0; // status1
    public byte status2 = 0; // status2
    public byte tempo_d = 0; // tempo(TIMER-B)
    public byte fadeout_speed = 0; // Fadeout速度
    public byte fadeout_volume = 0; // Fadeout音量
    public byte tempo_d_push = 0; // tempo(TIMER-B) / 保存用
    public byte syousetu_lng = 0; // 小節の長さ
    public byte opncount = 0; // 最短音符カウンタ
    public byte TimerAtime = 0; // TimerAカウンタ
    public byte effflag = 0; // PSG効果音発声on/off flag
    public byte psnoi = 0; // PSG noise周波数
    public byte psnoi_last = 0; // PSG noise周波数(最後に定義した数値)
    public byte fm_effec_num = 0; // 発声中のFM効果音番号
    public byte fm_effec_flag = 0; // FM効果音発声中flag(1)
    public byte disint = 0; // FM割り込み中に割り込みを禁止するかflag
    public byte pcmflag = 0; // PCM効果音発声中flag
    public int pcmstart = 0; // PCM音色のstart値
    public int pcmstop = 0; // PCM音色のstop値
    public byte pcm_effec_num = 0; // 発声中のPCM効果音番号
    public int _pcmstart = 0; // PCM効果音のstart値
    public int _pcmstop = 0; // PCM効果音のstop値
    public int _voice_delta_n = 0; // PCM効果音のdelta_n値
    public byte _pcmpan = 0; // PCM効果音のpan
    public byte _pcm_volume = 0; // PCM効果音のvolume
    public byte rshot_dat = 0; // リズム音源 shot flag
    public byte[] rdat = new byte[6]; // リズム音源 音量/パンデータ
    public byte rhyvol = 0b0011_1100; // リズムトータルレベル
    public int kshot_dat = 0; // ＳＳＧリズム shot flag
    public int ssgefcdat = 0; // efftbl  PSG Effecdataのaddress
    public int ssgefclen = 0; // efftblend-efftbl PSG Effecdataの長さ
    public byte play_flag = 0; // play flag
    public byte pause_flag = 0; // pause flag
    public byte fade_stop_flag = 0; // Fadeout後 MSTOPするかどうかのフラグ
    public byte kp_rhythm_flag = 0; // K/RpartでRhythm音源を鳴らすかflag
    public byte TimerBflag = 0; // TimerB割り込み中？フラグ
    public byte TimerAflag = 0; // TimerA割り込み中？フラグ
    public byte int60flag = 0; // INT60H割り込み中？フラグ
    public byte int60_result = 0; // INT60Hの実行ErrorFlag
    public byte pcm_gs_flag = 0; // ADPCM使用 許可フラグ(0で許可)
    public byte esc_sp_key = 0; // ESC +?? Key Code
    public byte grph_sp_key = 0; // GRPH+?? Key Code
    public byte rescut_cant = 0; // 常駐解除禁止フラグ
    public short slot_detune1 = 0; // FM3 Slot Detune値 slot1
    public short slot_detune2 = 0; // FM3 Slot Detune値 slot2
    public short slot_detune3 = 0; // FM3 Slot Detune値 slot3
    public short slot_detune4 = 0; // FM3 Slot Detune値 slot4
    public int wait_clock = 0; // FM ADDRESS-DATA間 Loop $の回数
    public int wait1_clock = 0; // loop $ １個の速度
    public byte ff_tempo = 0; // 早送り時のTimerB値
    public byte pcm_access = 0; // PCMセット中は 1
    public byte TimerB_speed = 0; // TimerBの現在値(=ff_tempoならff中)
    public byte fadeout_flag = 0; // 内部からfoutを呼び出した時1
    public byte adpcm_wait = 0; // ADPCM定義の速度
    public byte revpan = 0; // PCM86逆走flag
    public byte pcm86_vol = 0; // PCM86の音量をSPBに合わせるか?
    public short syousetu = 0; // 小節カウンタ
    public byte int5_flag = 0; // FM音源割り込み中？フラグ
    public byte port22h = 0; // OPN-PORT 22H に最後に出力した値(hlfo)
    public byte tempo_48 = 0; // 現在のテンポ(clock= 48 tの値)
    public byte tempo_48_push = 0; // 現在のテンポ(同上/保存用)
    public byte rew_sp_key = 0; // GRPH+?? (rew) Key Code
    public byte intfook_flag = 0; // int_fookのflag B0:TB B1:TA
    public byte skip_flag = 0; // normal:0 前方SKIP中:1 後方SKIP中:2
    public byte _fm_voldown = 0; // FM voldown 数値(保存用)
    public byte _ssg_voldown = 0; // PSG voldown 数値(保存用)
    public byte _pcm_voldown = 0; // PCM voldown 数値(保存用)
    public byte _rhythm_voldown = 0; // RHYTHM voldown 数値(保存用)
    public byte _pcm86_vol = 0; // PCM86の音量をSPBに合わせるか? (保存用)
    public byte mstart_flag = 0; // mstartする時に１にするだけのflag
    public byte[] mus_filename = new byte[13]; // 曲のFILE名バッファ
    public byte mmldat_lng = 0; // 曲データバッファサイズ(KB)
    public byte voicedat_lng = 0; // 音色データバッファサイズ(KB)
    public byte effecdat_lng = 0; // 効果音データバッファサイズ(KB)
    public int[] rshot = {0, 0, 0, 0, 0, 0}; // リズム音源 shot inc flags
    //public byte rshot_bd = 0; // リズム音源 shot inc flag(BD)
    //public byte rshot_sd = 0; // リズム音源 shot inc flag(SD)
    //public byte rshot_sym = 0; // リズム音源 shot inc flag(CYM)
    //public byte rshot_hh = 0; // リズム音源 shot inc flag(HH)
    //public byte rshot_tom = 0; // リズム音源 shot inc flag(TOM)
    //public byte rshot_rim = 0; // リズム音源 shot inc flag(RIM)
    public int[] rdump = {0, 0, 0, 0, 0, 0}; // リズム音源 dump inc flags
    //public byte rdump_bd = 0; // リズム音源 dump inc flag(BD)
    //public byte rdump_sd = 0; // リズム音源 dump inc flag(SD)
    //public byte rdump_sym = 0; // リズム音源 dump inc flag(CYM)
    //public byte rdump_hh = 0; // リズム音源 dump inc flag(HH)
    //public byte rdump_tom = 0; // リズム音源 dump inc flag(TOM)
    //public byte rdump_rim = 0; // リズム音源 dump inc flag(RIM)
    public byte ch3mode = 0; // ch3 Mode
    public byte ch3mode_push = 0; // ch3 Mode(効果音発音時用push領域)
    public byte ppz_voldown = 0; // PPZ8 voldown 数値
    public byte _ppz_voldown = 0; // PPZ8 voldown 数値(保存用)
    public int ppz_call_ofs = 0; // PPZ8call用 far call address
    public int ppz_call_seg = 0; // seg値はPPZ8常駐checkを兼ねる,0で非常駐
    public byte p86_freq = 8; // PMD86のPCM再生周波数
    //if	pcm* board2
    public int p86_freqtable = 0; // offset pcm_tune_data
    //else
    //public int p86_freqtable = 0; // PMD86のPCM再生周波数table位置
    //endif
    public byte adpcm_emulate = 0; // PMDPPZEでADPCMエミュレート中か


    public MmlDatum[] rd = null;
    public MmlDatum[] rdDmy = new MmlDatum[] {new MmlDatum(0xff)};

    //8153-8247
    //	演奏中のデータエリア

    public static class partWork { //qq  struc

        public short address; // w?	; 2 エンソウチュウ ノ アドレス
        public short partloop; // w? ; 2 エンソウ ガ オワッタトキ ノ モドリサキ
        public byte leng; // b? ; 1 ノコリ LENGTH
        public byte qdat; // b? ; 1 gatetime(q/Q値を計算した値)
        public short fnum; // w? ; 2 エンソウチュウ ノ BLOCK/FNUM
        public short detune; // w? ; 2 デチューン
        //+10
        public short lfodat; // w? ; 2 LFO DATA
        public short porta_num; // w? ; 2 ポルタメントの加減値（全体）
        public short porta_num2; // w? ; 2 ポルタメントの加減値（一回）
        public short porta_num3; // w? ; 2 ポルタメントの加減値（余り）
        public byte volume; // b? ; 1 VOLUME
        public byte shift; // b? ; 1 オンカイ シフト ノ アタイ
        //+20
        public byte delay; // b? ; 1 LFO[DELAY]
        public byte speed; // b? ; 1 [SPEED]
        public byte step; // b? ; 1 [STEP]
        public byte time; // b? ; 1 [TIME]
        public byte delay2; // b? ; 1 [DELAY_2]
        public byte speed2; // b? ; 1 [SPEED_2]
        public byte step2; // b? ; 1 [STEP_2]
        public byte time2; // b? ; 1 [TIME_2]
        public byte lfoswi; // b? ; 1 LFOSW.B0/tone B1/vol B2/同期 B3/porta
        //    ;          B4/tone B5/vol B6/同期
        public byte volpush; // b? ; 1 Volume PUSHarea
        //+30
        public byte mdepth; // b? ; 1 M depth
        public byte mdspd; // b? ; 1 M speed
        public byte mdspd2; // b? ; 1 M speed_2
        public byte envf; // b? ; 1 PSG ENV. [START_FLAG] / -1でextend
        public byte eenv_count; // b? ; 1 ExtendPSGenv/No=0 AR=1 DR=2 SR=3 RR=4
        public byte eenv_ar; // b? ; 1 /AR /旧pat
        public byte eenv_dr; // b? ; 1 /DR /旧pv2
        public byte eenv_sr; // b? ; 1 /SR /旧pr1
        public byte eenv_rr; // b? ; 1 /RR /旧pr2
        public byte eenv_sl; // b? ; 1 /SL
        //+40
        public byte eenv_al; // b? ; 1 /AL
        public byte eenv_arc; // b? ; 1 /ARのカウンタ /旧patb
        public byte eenv_drc; // b? ; 1 /DRのカウンタ
        public byte eenv_src; // b? ; 1 /SRのカウンタ /旧pr1b
        public byte eenv_rrc; // b? ; 1 /RRのカウンタ /旧pr2b
        public byte eenv_volume; // b? ; 1 /Volume値(0～15)/旧penv
        public byte extendmode; // b? ; 1 B1/Detune B2/LFO B3/Env Normal/Extend
        public byte fmpan; // b? ; 1 FM Panning + AMD + PMD
        public byte psgpat; // b? ; 1 PSG PATTERN[TONE / NOISE / MIX]
        public byte voicenum; // b? ; 1 音色番号
        //+50
        public byte loopcheck; // b? ; 1 ループしたら１ 終了したら３
        public byte carrier; // b? ; 1 FM Carrier
        public byte slot1; // b? ; 1 SLOT 1 ノ TL
        public byte slot3; // b? ; 1 SLOT 3 ノ TL
        public byte slot2; // b? ; 1 SLOT 2 ノ TL
        public byte slot4; // b? ; 1 SLOT 4 ノ TL
        public byte slotmask; // b? ; 1 FM slotmask
        public byte neiromask; // b? ; 1 FM 音色定義用maskdata
        public byte lfo_wave; // b? ; 1 LFOの波形
        public byte partmask; // b 1 PartMask b0:通常 b1:効果音 b2:NECPCM用
        //          ;   b3:none b4:PPZ/ADE用 b5:s0時 b6:m b7:一時
        //+60
        public byte keyoff_flag; // b? ; 1 KeyoffしたかどうかのFlag
        public byte volmask; // b? ; 1 音量LFOのマスク
        public byte qdata; // b? ; 1 qの値
        public byte qdatb; // b?	; 1 Qの値
        public byte hldelay; // b? ; 1 HardLFO delay
        public byte hldelay_c; // b? ; 1 HardLFO delay Counter
        public short _lfodat; // w? ; 2 LFO DATA
        public byte _delay; // b? ; 1 LFO[DELAY]
        public byte _speed; // b? ; 1	[SPEED]
        public byte _step; // b? ; 1	[STEP]
        public byte _time; // b? ; 1	[TIME]
        public byte _delay2; // b? ; 1	[DELAY_2]
        public byte _speed2; // b? ; 1	[SPEED_2]
        public byte _step2; // b? ; 1	[STEP_2]
        public byte _time2; // b? ; 1	[TIME_2]
        public byte _mdepth; // b? ; 1 M depth
        public byte _mdspd; // b? ; 1 M speed
        public byte _mdspd2; // b? ; 1 M speed_2
        public byte _lfo_wave; // b?	; 1 LFOの波形
        public byte _volmask; // b? ; 1 音量LFOのマスク
        public byte mdc; // b? ; 1 M depth Counter(変動値)
        public byte mdc2; // b? ; 1 M depth Counter
        public byte _mdc; // b? ; 1 M depth Counter(変動値)
        public byte _mdc2; // b? ; 1 M depth Counter
        public byte onkai; // b 1 演奏中の音階データ(0ffh:rest)
        public byte sdelay; // b?; 1 Slot delay
        public byte sdelay_c; // b? ; 1 Slot delay counter
        public byte sdelay_m; // b? ; 1 Slot delay Mask
        public byte alg_fb; // b? ; 1 音色のalg/fb
        public byte keyon_flag; // b 1 新音階/休符データを処理したらinc
        public byte qdat2; // b? ; 1 q 最低保証値
        public short fnum2; // w? ; 2 ppz8/pmd86用fnum値上位
        public byte onkai_def; // b 1 演奏中の音階データ(転調処理前 / ?fh:rest)
        public byte shift_def; // b? ; 1 マスター転調値
        public byte qdat3; // b? ; 1 q Random

        public int loopCounter;

        public void Clear() {
            address = 0; // w?	; 2 エンソウチュウ ノ アドレス
            partloop = 0; // w? ; 2 エンソウ ガ オワッタトキ ノ モドリサキ
            leng = 0; // b? ; 1 ノコリ LENGTH
            qdat = 0; // b? ; 1 gatetime(q/Q値を計算した値)
            fnum = 0; // w? ; 2 エンソウチュウ ノ BLOCK/FNUM
            detune = 0; // w? ; 2 デチューン
            lfodat = 0; // w? ; 2 LFO DATA
            porta_num = 0; // w? ; 2 ポルタメントの加減値（全体）
            porta_num2 = 0; // w? ; 2 ポルタメントの加減値（一回）
            porta_num3 = 0; // w? ; 2 ポルタメントの加減値（余り）
            volume = 0; // b? ; 1 VOLUME
            shift = 0; // b? ; 1 オンカイ シフト ノ アタイ
            delay = 0; // b? ; 1 LFO[DELAY]
            speed = 0; // b? ; 1 [SPEED]
            step = 0; // b? ; 1 [STEP]
            time = 0; // b? ; 1 [TIME]
            delay2 = 0; // b? ; 1 [DELAY_2]
            speed2 = 0; // b? ; 1 [SPEED_2]
            step2 = 0; // b? ; 1 [STEP_2]
            time2 = 0; // b? ; 1 [TIME_2]
            lfoswi = 0; // b? ; 1 LFOSW.B0/tone B1/vol B2/同期 B3/porta
            volpush = 0; // b? ; 1 Volume PUSHarea
            mdepth = 0; // b? ; 1 M depth
            mdspd = 0; // b? ; 1 M speed
            mdspd2 = 0; // b? ; 1 M speed_2
            envf = 0; // b? ; 1 PSG ENV. [START_FLAG] / -1でextend
            eenv_count = 0; // b? ; 1 ExtendPSGenv/No=0 AR=1 DR=2 SR=3 RR=4
            eenv_ar = 0; // b? ; 1 /AR /旧pat
            eenv_dr = 0; // b? ; 1 /DR /旧pv2
            eenv_sr = 0; // b? ; 1 /SR /旧pr1
            eenv_rr = 0; // b? ; 1 /RR /旧pr2
            eenv_sl = 0; // b? ; 1 /SL
            eenv_al = 0; // b? ; 1 /AL
            eenv_arc = 0; // b? ; 1 /ARのカウンタ /旧patb
            eenv_drc = 0; // b? ; 1 /DRのカウンタ
            eenv_src = 0; // b? ; 1 /SRのカウンタ /旧pr1b
            eenv_rrc = 0; // b? ; 1 /RRのカウンタ /旧pr2b
            eenv_volume = 0; // b? ; 1 /Volume値(0～15)/旧penv
            extendmode = 0; // b? ; 1 B1/Detune B2/LFO B3/Env Normal/Extend
            fmpan = 0; // b? ; 1 FM Panning + AMD + PMD
            psgpat = 0; // b? ; 1 PSG PATTERN[TONE / NOISE / MIX]
            voicenum = 0; // b? ; 1 音色番号
            loopcheck = 0; // b? ; 1 ループしたら１ 終了したら３
            carrier = 0; // b? ; 1 FM Carrier
            slot1 = 0; // b? ; 1 SLOT 1 ノ TL
            slot3 = 0; // b? ; 1 SLOT 3 ノ TL
            slot2 = 0; // b? ; 1 SLOT 2 ノ TL
            slot4 = 0; // b? ; 1 SLOT 4 ノ TL
            slotmask = 0; // b? ; 1 FM slotmask
            neiromask = 0; // b? ; 1 FM 音色定義用maskdata
            lfo_wave = 0; // b? ; 1 LFOの波形
            partmask = 0; // b 1 PartMask b0:通常 b1:効果音 b2:NECPCM用
            keyoff_flag = 0; // b? ; 1 KeyoffしたかどうかのFlag
            volmask = 0; // b? ; 1 音量LFOのマスク
            qdata = 0; // b? ; 1 qの値
            qdatb = 0; // b?	; 1 Qの値
            hldelay = 0; // b? ; 1 HardLFO delay
            hldelay_c = 0; // b? ; 1 HardLFO delay Counter
            _lfodat = 0; // w? ; 2 LFO DATA
            _delay = 0; // b? ; 1 LFO[DELAY]
            _speed = 0; // b? ; 1	[SPEED]
            _step = 0; // b? ; 1	[STEP]
            _time = 0; // b? ; 1	[TIME]
            _delay2 = 0; // b? ; 1	[DELAY_2]
            _speed2 = 0; // b? ; 1	[SPEED_2]
            _step2 = 0; // b? ; 1	[STEP_2]
            _time2 = 0; // b? ; 1	[TIME_2]
            _mdepth = 0; // b? ; 1 M depth
            _mdspd = 0; // b? ; 1 M speed
            _mdspd2 = 0; // b? ; 1 M speed_2
            _lfo_wave = 0; // b?	; 1 LFOの波形
            _volmask = 0; // b? ; 1 音量LFOのマスク
            mdc = 0; // b? ; 1 M depth Counter(変動値)
            mdc2 = 0; // b? ; 1 M depth Counter
            _mdc = 0; // b? ; 1 M depth Counter(変動値)
            _mdc2 = 0; // b? ; 1 M depth Counter
            onkai = 0; // b 1 演奏中の音階データ(0ffh:rest)
            sdelay = 0; // b?; 1 Slot delay
            sdelay_c = 0; // b? ; 1 Slot delay counter
            sdelay_m = 0; // b? ; 1 Slot delay Mask
            alg_fb = 0; // b? ; 1 音色のalg/fb
            keyon_flag = 0; // b 1 新音階/休符データを処理したらinc
            qdat2 = 0; // b? ; 1 q 最低保証値
            fnum2 = 0; // w? ; 2 ppz8/pmd86用fnum値上位
            onkai_def = 0; // b 1 演奏中の音階データ(転調処理前 / ?fh:rest)
            shift_def = 0; // b? ; 1 マスター転調値
            qdat3 = 0; // b? ; 1 q Random

            loopCounter = 0;
        }
    }

    //qqq struc
    //     db  offset eenv_ar dup(?)
    //pat  db	?	; 1 旧SSGENV	/Normal pat
    //pv2  db	?	; 1		/Normal pv2
    //pr1  db	?	; 1		/Normal pr1
    //pr2  db	?	; 1		/Normal pr2
    //     db?
    //     db	?
    //patb db	?	; 1		/Normal patb
    //     db?
    //pr1b db?	    ; 1		/Normal pr1b
    //pr2b db	?	; 1		/Normal pr2b
    //penv db	?	; 1		/Normal penv
    //qqq ends

    public int max_part1; // ０クリアすべきパート数
    public int max_part2; // 初期化すべきパート数

    //fm     equ 0
    //fm2    equ 1
    //psg    equ 2
    //rhythm equ 3

    //public short open_work; // dw

    public int[] part_data_table;

    //FM1-3
    public int part1;
    public int part2;
    public int part3;

    //FM4-6
    public int part4;
    public int part5;
    public int part6;

    //効果音モード
    public int part3b;
    public int part3c;
    public int part3d;

    //pps?
    public int part7;
    public int part8;
    public int part9;
    public int part10;
    public int part11;

    //ppz
    public int part10a;
    public int part10b;
    public int part10c;
    public int part10d;
    public int part10e;
    public int part10f;
    public int part10g;
    public int part10h;

    //効果音
    public int part_e;

    public partWork[] partWk;
    //ノーマル
    // 1,2,3, 3b,3c,3d, 7,8,9,10,11, e
    //board2
    // 1,2,3,4,5,6, 7,8,9,10,11, 3b,3c,3d, e
    //ppz(ppzはboard2を兼ねる)
    // 1,2,3,4,5,6, 7,8,9,10,11, 3b,3c,3d, 10a,10b,10c,10d,10e,10f,10g,10h ,e


    //    even
    //pcm_table   label word
    //if	board2
    // if	adpcm
    //  ife   ademu
    public short pcmends = 0x26; // 最初のstartは26Hから
    public short[] pcmadrs = new short[2 * 256];
    public byte[] pcmfilename = new byte[128];
    //  endif
    // endif
    // if	pcm
    public short pcmst_ofs = 0;
    public short pcmst_seg = 0;
    public byte[] pcmadrs_86 = new byte[6 * 256];
    // endif
    //endif

    //      db	"ここはＳＴＡＣＫエリアです。いつ"
    //		db	"もＰＭＤをご愛用して下さっている"
    //		db	"方々、どうもありがとうございます"
    //		db	"(^^)。何かバグらしき物が見つかり"
    //		db	"ましたら、些細な事でも構いません"
    //		db	"ので、是非私までご一報、お願いし"
    //		db	"ますね(^^)。→PMDBBS [xx(xxxx)xx"
    //		db	"xx] @PMD ボードまで     by KAJA."

    //_stack:
    //dataarea label   word
    //   db	0		;
    //   dw	12 dup(18h); 初期データ
    //   db	80h		;

    public byte[] fmoff_nef = {0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, (byte) 0xff};
    public byte[] fmoff_ef = {0, 1, 4, 5, 8, 9, 12, 13, (byte) 0xff};


    //10538-
    public String mes_title = "Ｍｕｓｉｃ　Ｄｒｉｖｅｒ　Ｐ.Ｍ.Ｄ. for PC9801/88VA Version " + ver
            + "\r\n"
            + "Copyright (C)1989," + date + " by M.Kajihara(KAJA).\r\n\r\n";

    public String mes_ppsdrv = "PPSDRV(INT64H)に対応します．\r\n";
    public String mes_ppz8 = "PPZ8(INT7FH)に対応します．\r\n";

    public byte port_sel; // b? ; 選択ポート
    public byte opn_0eh; // b?
    public byte message_flag;    // b?
    public short opt_sp_push; // w?
    public short resident_size;  // w?


    //EFCDRV.ASM
    public short effadr; // w effect address
    public short eswthz; // w トーンスゥイープ周波数
    public short eswtst; // w トーンスゥイープ増分
    public byte effcnt; // b effect count
    public byte eswnhz; // b ノイズスゥイープ周波数
    public byte eswnst; // b ノイズスゥイープ増分
    public byte eswnct; // b ノイズスゥイープカウント
    public byte effon; // b 効果音 発音中
    public byte psgefcnum; // b 効果音番号
    public byte hosei_flag; // b ppsdrv 音量/音程補正をするかどうか
    public byte last_shot_data; // b 最後に発音させたPPSDRV音色


    //PCMDRV86.ASM
    //==============================================================================
    //	Datas
    //==============================================================================
    public int trans_size = 256;//	;1回の転送byte数
    public byte play86_flag; // db	0	;発音中? flag
    public byte trans_flag; // db	0	; 転送するdataが残っているか? flag
    public short start_ofs; // dw	0	; 発音中PCMデータ番地(offset下位)
    public short start_ofs2; // dw	0	; 発音中PCMデータ番地(offset上位)
    public short size1; // dw	0	; 残りサイズ(下位word)
    public short size2; // dw	0	; 残りサイズ(上位word)
    public short _start_ofs; // dw	0	; 発音開始PCMデータ番地(offset下位)
    public short _start_ofs2; // dw	0	; 発音開始PCMデータ番地(offset上位)
    public short _size1; // dw	0	; PCMデータサイズ(下位word)
    public short _size2; // dw	0	; PCMデータサイズ(上位word)
    public byte addsize1; // db	0	; PCMアドレス加算値(整数部)
    public short addsize2; // dw	0	; PCMアドレス加算値(小数点部)
    public short addsizew; // dw	0	; PCMアドレス加算値(小数点部, 転送中work)
    public short repeat_ofs; // dw	0	; リピート開始位置(offset下位)
    public short repeat_ofs2; // dw	0	; リピート開始位置(offset上位)
    public short repeat_size1; // dw	0	; リピート後のサイズ(下位word)
    public short repeat_size2; // dw	0	; リピート後のサイズ(上位word)
    public short release_ofs; // dw	0	; リリース開始位置(offset下位)
    public short release_ofs2; // dw	0	; リリース開始位置(offset上位)
    public short release_size1; // dw	0	; リリース後のサイズ(下位word)
    public short release_size2; // dw	0	; リリース後のサイズ(上位word)
    public byte repeat_flag; // db	0	; リピートするかどうかのflag
    public byte release_flag1; //   db	0	;リリースするかどうかのflag
    public byte release_flag2; //   db	0	;リリースしたかどうかのflag
    public byte pcm86_pan_flag = 0; // b 0 ;パンデータ１(bit0= 左 / bit1 = 右 / bit2 = 逆)
    public byte com_end = (byte) 0xb1;
    public byte pcm86_pan_dat; // db	0	; パンデータ２(音量を下げるサイドの音量値)

    // pan_flagによる転送table
    //trans_table dw double_trans, left_trans
    //        dw right_trans, double_trans
    //        dw double_trans_g, left_trans_g
    //        dw right_trans_g, double_trans_g

    // 周波数table Include

    //    include tunedata.inc
    //==============================================================================
    //	周波数table 16.54kHz = o5g
    //==============================================================================
    //fq macro   data1,data2
    //   db  data1
    //   dw  data2
    //   endm

    public Tuple<Integer, Integer>[] pcm_tune_data86 = new Tuple[] {
            // 周波数*32+ 加算値(整数部) , 加算値(小数部)
            new Tuple<Integer, Integer>(0 * 32 + 0, 0x02AB7) // o1  4.13438 C
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x02D41) // o1  4.13438 C#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x02FF2) // o1  4.13438 D
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x032CB) // o1  4.13438 D#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x035D1) // o1  4.13438 E
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x03904) // o1  4.13438 F
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x03C68) // o1  4.13438 F#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x03FFF) // o1  4.13438 G
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x043CE) // o1  4.13438 G#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x047D6) // o1  4.13438 A
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x04C1B) // o1  4.13438 A#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x050A2) // o1  4.13438 B

            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0556E) // o2  4.13438 C
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x05A82) // o2  4.13438 C#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x05FE4) // o2  4.13438 D
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x06597) // o2  4.13438 D#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x06BA2) // o2  4.13438 E
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x07209) // o2  4.13438 F
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x078D0) // o2  4.13438 F#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x07FFF) // o2  4.13438 G
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0879C) // o2  4.13438 G#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x08FAC) // o2  4.13438 A
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x09837) // o2  4.13438 A#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0A145) // o2  4.13438 B

            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0AADC) // o3  4.13438 C
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0B504) // o3  4.13438 C#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0BFC8) // o3  4.13438 D
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0CB2F) // o3  4.13438 D#
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0D744) // o3  4.13438 E
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0E412) // o3  4.13438 F
            , new Tuple<Integer, Integer>(0 * 32 + 0, 0x0F1A1) // o3  4.13438 F#
            , new Tuple<Integer, Integer>(0 * 32 + 1, 0x00000) // o3  4.13438 G
            , new Tuple<Integer, Integer>(1 * 32 + 0, 0x0CB6B) // o3  5.51250 G#
            , new Tuple<Integer, Integer>(1 * 32 + 0, 0x0D783) // o3  5.51250 A
            , new Tuple<Integer, Integer>(1 * 32 + 0, 0x0E454) // o3  5.51250 A#
            , new Tuple<Integer, Integer>(1 * 32 + 0, 0x0F1E7) // o3  5.51250 B

            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0AADC) // o4  8.26875 C
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0B504) // o4  8.26875 C#
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0BFC8) // o4  8.26875 D
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0CB2F) // o4  8.26875 D#
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0D744) // o4  8.26875 E
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0E412) // o4  8.26875 F
            , new Tuple<Integer, Integer>(2 * 32 + 0, 0x0F1A1) // o4  8.26875 F#
            , new Tuple<Integer, Integer>(2 * 32 + 1, 0x00000) // o4  8.26875 G
            , new Tuple<Integer, Integer>(3 * 32 + 0, 0x0CB6B) // o4 11.02500 G#
            , new Tuple<Integer, Integer>(3 * 32 + 0, 0x0D783) // o4 11.02500 A
            , new Tuple<Integer, Integer>(3 * 32 + 0, 0x0E454) // o4 11.02500 A#
            , new Tuple<Integer, Integer>(3 * 32 + 0, 0x0F1E7) // o4 11.02500 B

            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0AADC) // o5 16.53750 C
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0B504) // o5 16.53750 C#
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0BFC8) // o5 16.53750 D
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0CB2F) // o5 16.53750 D#
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0D744) // o5 16.53750 E
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0E412) // o5 16.53750 F
            , new Tuple<Integer, Integer>(4 * 32 + 0, 0x0F1A1) // o5 16.53750 F#
            , new Tuple<Integer, Integer>(4 * 32 + 1, 0x00000) // o5 16.53750 G
            , new Tuple<Integer, Integer>(5 * 32 + 0, 0x0CB6B) // o5 22.05000 G#
            , new Tuple<Integer, Integer>(5 * 32 + 0, 0x0D783) // o5 22.05000 A
            , new Tuple<Integer, Integer>(5 * 32 + 0, 0x0E454) // o5 22.05000 A#
            , new Tuple<Integer, Integer>(5 * 32 + 0, 0x0F1E7) // o5 22.05000 B

            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0AADC) // o6 33.07500 C
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0B504) // o6 33.07500 C#
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0BFC8) // o6 33.07500 D
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0CB2F) // o6 33.07500 D#
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0D744) // o6 33.07500 E
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0E412) // o6 33.07500 F
            , new Tuple<Integer, Integer>(6 * 32 + 0, 0x0F1A1) // o6 33.07500 F#
            , new Tuple<Integer, Integer>(6 * 32 + 1, 0x00000) // o6 33.07500 G
            , new Tuple<Integer, Integer>(7 * 32 + 0, 0x0CB6B) // o6 44.10000 G#
            , new Tuple<Integer, Integer>(7 * 32 + 0, 0x0D783) // o6 44.10000 A
            , new Tuple<Integer, Integer>(7 * 32 + 0, 0x0E454) // o6 44.10000 A#
            , new Tuple<Integer, Integer>(7 * 32 + 0, 0x0F1E7) // o6 44.10000 B

            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x0004A) // o7 44.10000 C
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x00F87) // o7 44.10000 C#
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x01FAC) // o7 44.10000 D
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x030C7) // o7 44.10000 D#
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x042E7) // o7 44.10000 E
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x0561C) // o7 44.10000 F
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x06A72) // o7 44.10000 F#
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x08000) // o7 44.10000 G
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x096D6) // o7 44.10000 G#
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x0AF06) // o7 44.10000 A
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x0C8A8) // o7 44.10000 A#
            , new Tuple<Integer, Integer>(7 * 32 + 1, 0x0E3CF) // o7 44.10000 B

            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x00094) // o8 44.10000 C
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x01F0E) // o8 44.10000 C#
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x03F59) // o8 44.10000 D
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x0618F) // o8 44.10000 D#
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x085CE) // o8 44.10000 E
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x0AC38) // o8 44.10000 F
            , new Tuple<Integer, Integer>(7 * 32 + 2, 0x0D4E5) // o8 44.10000 F#
            , new Tuple<Integer, Integer>(7 * 32 + 3, 0x00000) // o8 44.10000 G
            , new Tuple<Integer, Integer>(7 * 32 + 3, 0x02DAC) // o8 44.10000 G#
            , new Tuple<Integer, Integer>(7 * 32 + 3, 0x05E0D) // o8 44.10000 A
            , new Tuple<Integer, Integer>(7 * 32 + 3, 0x09150) // o8 44.10000 A#
            , new Tuple<Integer, Integer>(7 * 32 + 3, 0x0C79E) // o8 44.10000 B
    };

    //==============================================================================
    //	オンカイ DATA
    //==============================================================================
    public int[] fnum_data = new int[] {
            0x026a // C
            , 0x028f // D-
            , 0x02b6 // D
            , 0x02df // E-
            , 0x030b // E
            , 0x0339 // F
            , 0x036a // G-
            , 0x039e // G
            , 0x03d5 // A-
            , 0x0410 // A
            , 0x044e // B-
            , 0x048f // B
    };

    public int[] psg_tune_data = new int[] {
            0x0ee8 // C
            , 0x0e12 // D-
            , 0x0d48 // D
            , 0x0c89 // E-
            , 0x0bd5 // E
            , 0x0b2b // F
            , 0x0a8a // G-
            , 0x09f3 // G
            , 0x0964 // A-
            , 0x08dd // A
            , 0x085e // B-
            , 0x07e6 // B
    };

    //7177-7242
    public int[] part_table = null;
    //if	board2
    // if	ppz
    //			Part番号,Partb,音源番号
    private int[] part_table_ppz = {
                    00, 1, 0 // A
                    , 01, 2, 0    // B
                    , 02, 3, 0    // C
                    , 03, 1, 1    // D
                    , 04, 2, 1    // E
                    , 05, 3, 1    // F
                    , 06, 1, 2    // G
                    , 07, 2, 2    // H
                    , 8, 3, 2    // I
                    , 9, 1, 3    // J
                    , 10, 3, 4    // K
                    , 11, 3, 0    // c2
                    , 12, 3, 0    // c3
                    , 13, 3, 0    // c4
                    , 0xff, 0, 0xff // Rhythm
                    , 22, 3, 1    // Effect
                    , 14, 0, 5    // PPZ1
                    , 15, 1, 5    // PPZ2
                    , 16, 2, 5    // PPZ3
                    , 17, 3, 5    // PPZ4
                    , 18, 4, 5    // PPZ5
                    , 19, 5, 5    // PPZ6
                    , 20, 6, 5    // PPZ7
                    , 21, 7, 5 // PPZ8
            };
    // else
    //			Part番号,Partb,音源番号
    private int[] part_table_brd2 = {
                    0, 1, 0    //A
                    , 1, 2, 0    //B
                    , 2, 3, 0    //C
                    , 3, 1, 1    //D
                    , 4, 2, 1    //E
                    , 5, 3, 1    //F
                    , 6, 1, 2    //G
                    , 7, 2, 2    //H
                    , 8, 3, 2    //I
                    , 9, 1, 3    //J
                    , 10, 3, 4    //K
                    , 11, 3, 0    //c2
                    , 12, 3, 0    //c3
                    , 13, 3, 0    //c4
                    , 0xff, 0, 0xff    //Rhythm
                    , 14, 3, 1 //Effect
            };
    //else
    // Part番号,Partb,音源番号
    private int[] part_table_nbrd2 =             {
                    0, 1, 0    //A
                    , 1, 2, 0    //B
                    , 2, 3, 0    //C
                    , 3, 3, 0    //c2
                    , 4, 3, 0    //c3
                    , 5, 3, 0    //c4
                    , 6, 1, 2    //G
                    , 7, 2, 2    //H
                    , 8, 3, 2    //I
                    , 9, 1, 3    //J
                    , 10, 3, 4    //K
                    , 3, 3, 0    //c2
                    , 4, 3, 0    //c3
                    , 5, 3, 0    //c4
                    , 0xff, 0, 0xff //Rhythm
                    , 11, 3, 0 //Effect
            };


    //PMD.ASM 7955-7964
    //==============================================================================
    //	FM音色のキャリアのテーブル
    //==============================================================================
    public int[] carrier_table = new int[] {
            0b1000_0000, 0b1000_0000, 0b1000_0000, 0b1000_0000
            , 0b1010_0000, 0b1110_0000, 0b1110_0000, 0b1111_0000
            , 0b1110_1110, 0b1110_1110, 0b1110_1110, 0b1110_1110
            , 0b1100_1100, 0b1000_1000, 0b1000_1000, 0b0000_0000
    };


    //
    //==============================================================================
    //	効果音データ ＩＮＣＬＵＤＥ
    //==============================================================================
    //public byte[] efftbl; // label   word
    //include effect.inc
    public byte efftblend; //   label word


    //PCMLOAD.INC
    //15
    public int message = 1; // equ ;エラーメッセージを表示するか否か

    //1447-1503
    //==============================================================================
    //	DataArea
    //==============================================================================
    //if	message
    public String allload_mes = "PCMを定義中です。しばらくお待ち下さい。";
    public String exit1_mes = "PCMが定義出来る環境ではありません。";
    public String exit1p_mes = "PPSDRVが常駐していません。";
    public String exit2_mes = "PCMFileが見つかりません。";
    public String exit2p_mes = "PPSFileが見つかりません。";
    public String exit3_mes = "PCMFileのFORMATが違います。";
    public String exit3p_mes = "PPSDRVの確保容量が足りません。";
    public String exit4_mes = "PCMDataが一致したので読み込みません。";
    public String exit4pp_mes = "P86DRVの確保容量が足りません。";
    public String exit5_mes = "PCMFileが読み込めません。";
    public String exit5p_mes = "PPSFileが読み込めません。";
    public String exit6_mes = "PCMメモリを他のアプリケーションがアクセス中です。";
    public String exit1z_mes = "PCMFileが見つかりません。";
    public String exit2z_mes = "PCMFileのデータ形式が違います。";
    public String exit3z_mes = "メモリ確保容量が足りません。";
    public String exit4z_mes = "EMSハンドルのマッピングができません。";
    public String exit5z_mes = "PPZ8が常駐していません。";
    public String exit6z_mes = "PVI/PZIFileが見つかりません。"; //KUMA: Added
    public String ppzbank_mes = "PPZ8(%d):";
    //endif
    public String adpcm_header = "ADPCM DATA for  PMD ver.4.4-  "; // ;30 bytes
    public String pps_ext = "PPS";
    public String ppc_ext = "PPC";
    public String p86_ext = "P86";
    public String pvi_ext = "PVI";
    public String pzi_ext = "PZI";

    public byte retry_flag = 0;
    public byte key_check_push = 0;
    public short pcmload_wait_clock = 0;
    public byte pcmload_adpcm_wait = 0;
    public short mmask_port = 0;
    public byte mmask_push = 0;

    public byte[] filename_buf = new byte[128];

    public String filename_ofs; // dw	?
    public short filename_seg; // dw	?
    public String filename_ofs2; // dw	?
    public short filename_seg2; // dw	?
    public short pcmdata_ofs; // dw	?
    public short pcmdata_seg; // dw	?
    public byte pcmdata_size_s; // db	?
    public short pcmdata_size; // dw	?
    public short pcmwork_ofs; // dw	?
    public short pcmwork_seg; // dw	?
    public short port46; // dw	?
    public short port47; // dw	?
    public short pcmload_pcmstop; // dw	?
    public short pcmload_pcmstart; // dw	?
    public short fhand2; // dw	?
    public byte ppz_bank; // db	?


    //924-940
    //==============================================================================
    //	Datas
    //==============================================================================
    public int[] pcm_tune_data = new int[] {
            0x3132 * 2 // C
            , 0x3420 * 2 // C+
            , 0x373a * 2 // D
            , 0x3a83 * 2 // D+
            , 0x3dfe * 2 // E
            , 0x41af * 2 // F
            , 0x4597 * 2 // F+
            , 0x49bb * 2 // G
            , 0x4e1e * 2 // G+
            , 0x52c4 * 2 // A
            , 0x57b1 * 2 // A+
            , 0x5ce8 * 2 // B
    };


    //848-865 PPZDRV.ASM
    //==============================================================================
    //	Datas
    //==============================================================================
    public int[] ppzpandata = new int[] {0, 9, 1, 5};

    public int[] ppz_tune_data = new int[] { // label   word
            0x08000 // 00 c
            , 0x087a6 // 01 d-
            , 0x08fb3 // 02 d
            , 0x09838 // 03 e-
            , 0x0a146 // 04 e
            , 0x0aade // 05 f
            , 0x0b4ff // 06 g-
            , 0x0bfcc // 07 g
            , 0x0cb34 // 08 a-
            , 0x0d747 // 09 a
            , 0x0e418 // 10 b-
            , 0x0f1a5 // 11 b
    };

    //pmdDotNET 独自
    public int jumpIndex = -1;
    public boolean checkJumpIndexSI = false;
    public boolean checkJumpIndexBX = false;


    public PW() {
        efftbl = new ArrayList<Tuple<Integer, MmlDatum[]>>();
        MmlDatum[] ef;

//#region 効果音データ定義
        ef = MakeMmlDatum(D_000);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //BDRM		    ;0
        ef = MakeMmlDatum(D_001);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //SIMONDS    	;1
        ef = MakeMmlDatum(D_002);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //SIMONDSTAML	;2
        ef = MakeMmlDatum(D_003);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //SIMONDSTAMM	;3
        ef = MakeMmlDatum(D_004);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //SIMONDSTAMH	;4
        ef = MakeMmlDatum(D_005);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //RIMSHOTT	    ;5
        ef = MakeMmlDatum(D_006);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //CPSIMONDSSD2	;6
        ef = MakeMmlDatum(D_007);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //CLOSEHT	    ;7
        ef = MakeMmlDatum(D_008);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //OPENHT		    ;8
        ef = MakeMmlDatum(D_009);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //CRUSHCYMBA	    ;9
        ef = MakeMmlDatum(D_010);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(1, ef)); //RDCYN		    ;10

        ef = MakeMmlDatum(DM_001);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 11 syuta
        ef = MakeMmlDatum(DM_002);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 12 Au
        ef = MakeMmlDatum(DM_003);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 13 syuba
        ef = MakeMmlDatum(DM_004);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 14 syu
        ef = MakeMmlDatum(DM_005);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 15 sya-
        ef = MakeMmlDatum(DM_006);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 16 po
        ef = MakeMmlDatum(DM_007);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 17 tattu
        ef = MakeMmlDatum(DM_008);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 18 zusyau
        ef = MakeMmlDatum(DM_009);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 19 piro
        ef = MakeMmlDatum(DM_010);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 20 piron
        ef = MakeMmlDatum(DM_011);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 21 pirorironn
        ef = MakeMmlDatum(DM_012);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 22 buu
        ef = MakeMmlDatum(DM_013);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 23 babon
        ef = MakeMmlDatum(DM_014);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 24 basyu-
        ef = MakeMmlDatum(DM_015);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 25 poun
        ef = MakeMmlDatum(DM_016);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 26 pasyu
        ef = MakeMmlDatum(DM_017);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 27 KON
        ef = MakeMmlDatum(DM_018);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 28 dosun
        ef = MakeMmlDatum(DM_019);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 29 zu
        ef = MakeMmlDatum(DM_020);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 30 go
        ef = MakeMmlDatum(DM_021);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 31 poyon
        ef = MakeMmlDatum(DM_022);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 32 katun
        ef = MakeMmlDatum(DM_023);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 33 syupin
        ef = MakeMmlDatum(DM_024);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 34 1UP
        ef = MakeMmlDatum(DM_025);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 35 PI
        ef = MakeMmlDatum(DM_026);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 36 pikon
        ef = MakeMmlDatum(DM_027);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 37 pyuu
        ef = MakeMmlDatum(DM_028);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 38 PI
        ef = MakeMmlDatum(DM_029);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 39 click

        ef = MakeMmlDatum(RS_006);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 40 batan
        ef = MakeMmlDatum(RS_007);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 41 dodonn
        ef = MakeMmlDatum(RS_009);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 42 kisya-
        ef = MakeMmlDatum(RS_010);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 43 bofu
        ef = MakeMmlDatum(RS_011);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 44 gogogogo--
        ef = MakeMmlDatum(RS_012);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 45 karakara
        ef = MakeMmlDatum(RS_013);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 46 buonn
        ef = MakeMmlDatum(RS_015);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 47 tyattu
        ef = MakeMmlDatum(RS_018);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 48 zu
        ef = MakeMmlDatum(RS_019);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 49 saaaa
        ef = MakeMmlDatum(RS_020);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 50 za
        ef = MakeMmlDatum(RS_021);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 51 TYARIN
        ef = MakeMmlDatum(RS_022);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 52 SYUWAWA
        ef = MakeMmlDatum(RS_024);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 53 PIN
        ef = MakeMmlDatum(RS_026);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 54 KAMINARI
        ef = MakeMmlDatum(RS_027);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 55 PI
        ef = MakeMmlDatum(RS_028);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 56 KEIKOKU
        ef = MakeMmlDatum(RS_029);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 57 ETC 1
        ef = MakeMmlDatum(RS_030);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 58 BUFOFOFO
        ef = MakeMmlDatum(RS_031);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 59 ETC 3
        ef = MakeMmlDatum(RS_032);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 60 ETC 4
        ef = MakeMmlDatum(RS_033);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 61 HADE BOMB
        ef = MakeMmlDatum(RS_035);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 62 JARARAN
        ef = MakeMmlDatum(PO_011);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 63 Rain fall
        ef = MakeMmlDatum(PO_012);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 64 Spinner
        ef = MakeMmlDatum(PO_013);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 65 Kaminari
        ef = MakeMmlDatum(PO_014);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 66 Sairen
        ef = MakeMmlDatum(PO_015);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 67 Door Shut
        ef = MakeMmlDatum(PO_016);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 68 Kiteki
        ef = MakeMmlDatum(PO_017);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 69 Ship Bomb
        ef = MakeMmlDatum(PO_018);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 70 Spinner 2
        ef = MakeMmlDatum(PO_019);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 71 Helli
        ef = MakeMmlDatum(PO_020);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 72 Kinzoku Sagyou
        ef = MakeMmlDatum(PO_021);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 73 Kaze (DAME)
        ef = MakeMmlDatum(PO_022);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 74 Taikushitu Soto
        ef = MakeMmlDatum(PO_023);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 75 Punch
        ef = MakeMmlDatum(PO_024);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 76 Shower
        ef = MakeMmlDatum(PO_025);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 77 Shokki
        ef = MakeMmlDatum(PO_026);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 78 Tobikomi
        ef = MakeMmlDatum(PO_027);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 79 Air Fukidasi
        ef = MakeMmlDatum(PO_028);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 80 Heavy Door Open
        ef = MakeMmlDatum(PO_029);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 81 Car Door Shut
        ef = MakeMmlDatum(PO_030);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 82 Car Come'in
        ef = MakeMmlDatum(PO_031);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 83 Ice Hikkaki
        ef = MakeMmlDatum(PO_032);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 84 Ship Crush Down
        ef = MakeMmlDatum(PO_033);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 85 Toraware
        ef = MakeMmlDatum(PO_034);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 86 Sekizou Break
        ef = MakeMmlDatum(PO_035);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 87 Blaster Shot
        ef = MakeMmlDatum(PO_036);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 88 Seifuku Yabuki
        ef = MakeMmlDatum(PO_037);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 89 Miminari
        ef = MakeMmlDatum(PO_038);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 90 Sekizou Ayasige
        ef = MakeMmlDatum(PO_039);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 91 Voiler & Engine
        ef = MakeMmlDatum(PO_040);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 92 Suimen
        ef = MakeMmlDatum(PO_041);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 93 Kika
        ef = MakeMmlDatum(PO_042);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 94 Change Kakyuu
        ef = MakeMmlDatum(PO_043);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 95 Change Blue
        ef = MakeMmlDatum(PO_044);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 96 Youma Funsyutu
        ef = MakeMmlDatum(PO_045);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 97 Kekkai
        ef = MakeMmlDatum(PO_046);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 98 Gosintou 1
        ef = MakeMmlDatum(PO_047);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 99 Gosintou 2
        ef = MakeMmlDatum(PO_048);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 100 Gosintou 3
        ef = MakeMmlDatum(PO_049);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 101 Hand Blaster
        ef = MakeMmlDatum(PO_050);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 102 Magic
        ef = MakeMmlDatum(PO_051);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 103 Grabiton 1
        ef = MakeMmlDatum(PO_052);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 104 Grabiton 2
        ef = MakeMmlDatum(PO_053);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 105 Attack Kakyuu
        ef = MakeMmlDatum(PO_054);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 106 Attack Blue(TEKITOU)
        ef = MakeMmlDatum(PO_055);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 107 Attack Red
        ef = MakeMmlDatum(PO_056);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 108 Attack White
        ef = MakeMmlDatum(PO_057);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 109 Attack Black
        ef = MakeMmlDatum(PO_058);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 110 Attack Last
        ef = MakeMmlDatum(PO_059);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 111 Damage 1
        ef = MakeMmlDatum(PO_060);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 112 Damage 2
        ef = MakeMmlDatum(PO_061);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 113 Attack
        ef = MakeMmlDatum(ND_000);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 114 MAP
        ef = MakeMmlDatum(ND_001);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 115 SONAR
        ef = MakeMmlDatum(ND_002);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 116 KOUKOU
        ef = MakeMmlDatum(ND_003);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 117 MEGIDO
        ef = MakeMmlDatum(ND_004);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 118 JINARI
        ef = MakeMmlDatum(ND_005);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 119 SWITCH
        ef = MakeMmlDatum(ND_006);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 120 DOSYUUNN
        ef = MakeMmlDatum(ND_007);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 121 GYUOON
        ef = MakeMmlDatum(ND_008);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 122 PIPIPIPI
        ef = MakeMmlDatum(ND_009);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 123 SYUBATTU
        ef = MakeMmlDatum(ND_010);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 124 BEAM UNARI
        ef = MakeMmlDatum(ND_011);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 125 BEAM KAKUSAN
        ef = MakeMmlDatum(ND_012);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 126 ORGAN
        ef = MakeMmlDatum(ND_013);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 127 PANEL
        ef = MakeMmlDatum(ND_014);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 128 DRILL
        ef = MakeMmlDatum(ND_015);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 129 PRAZMA
        ef = MakeMmlDatum(ND_016);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 130 BABEL
        ef = MakeMmlDatum(ND_017);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 131 ELEVETOR
        ef = MakeMmlDatum(ND_018);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 132 MEGIDO HASSYA
        ef = MakeMmlDatum(ND_019);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 133 DAIBAKUHATU
        ef = MakeMmlDatum(ND_020);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 134 NAMI
        ef = MakeMmlDatum(ND_021);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 135 DOOOONN
        ef = MakeMmlDatum(ND_022);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 136 DOGA
        ef = MakeMmlDatum(ND_023);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 137 PISI
        ef = MakeMmlDatum(ND_024);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 138 BLUE WATER
        ef = MakeMmlDatum(ND_025);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 139 HOWAWAN
        ef = MakeMmlDatum(ND_026);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 140 ZUGAN
        ef = MakeMmlDatum(ND_027);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 141 DAAANN
        ef = MakeMmlDatum(ND_028);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 142 DOGOOOONN
        ef = MakeMmlDatum(ND_029);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 143 GASYA
        ef = MakeMmlDatum(ND_030);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 144 BASYUSYUSYU
        ef = MakeMmlDatum(ND_031);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 145 DOSYUSYUSYU
        ef = MakeMmlDatum(ND_032);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 146 SYUSYUUUUNN
        ef = MakeMmlDatum(ND_033);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 147 BASYANN - HYURURURU
        ef = MakeMmlDatum(ND_034);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 148 ZYURUZYURU
        ef = MakeMmlDatum(ND_035);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 149 ZUGOGOGOGO
        ef = MakeMmlDatum(ND_036);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 150 ZUGOOOONN
        ef = MakeMmlDatum(ND_037);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 151 BI--
        ef = MakeMmlDatum(ND_038);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 152 BASYUSYUUU
        ef = MakeMmlDatum(ND_039);
        efftbl.add(new Tuple<Integer, MmlDatum[]>(2, ef)); // 153 BISYU
//#endregion
    }

    public void SetOption(PMDDotNETOption dop, String[] op) {
        pmdOption = op;

        jumpIndex = dop.jumpIndex;
        //Console.WriteLine("%d", jumpIndex);

        if (dop.isAUTO) {
            dop.usePPS = ppsFile != null && !ppsFile.isEmpty();
            dop.usePPZ = ppz1File != null && !ppz1File.isEmpty() || ppz2File != null && !ppz2File.isEmpty();
            dop.isNRM = false;
            dop.isSPB = true;
            if (dop.PPCHeader == "PCM") dop.isSPB = false;
        }

        board = 1;
        board2 = dop.isNRM ? 0 : 1;
        if (board2 != 0) {
            adpcm = dop.isSPB ? 1 : 0;
            pcm = dop.isSPB ? 0 : 1;
        }
        va = dop.isVA ? 1 : 0;
        usePPSDRV = dop.usePPS;
        ppz = dop.usePPZ ? 1 : 0;
        useP86DRV = !dop.isNRM && !dop.isSPB;

        fmvd_init = (va + board2 != 0) ? 0 : 16;

        if (va != 0) {
            ms_cmd = 0x188; // ８２５９マスタポート
            ms_msk = 0x18a; // ８２５９マスタ／マスク
            sl_cmd = 0x184; // ８２５９スレーブポート
            sl_msk = 0x186; // ８２５９スレーブ／マスク
        }

        if (board2 == 0) {
            //ノーマル
            partWk = new partWork[3 + 3 + 5 + 1];
            part3b = 3;
            partWk[part3b] = new partWork();
            part3c = 4;
            partWk[part3c] = new partWork();
            part3d = 5;
            partWk[part3d] = new partWork();
            part7 = 6;
            partWk[part7] = new partWork();
            part8 = 7;
            partWk[part8] = new partWork();
            part9 = 8;
            partWk[part9] = new partWork();
            part10 = 9;
            partWk[part10] = new partWork();
            part11 = 10;
            partWk[part11] = new partWork();
            part_e = 11;
            partWk[part_e] = new partWork();

            max_part1 = 11; // ０クリアすべきパート数
            max_part2 = 11; // 初期化すべきパート数
        } else {
            //board2
            if (ppz == 0) {
                partWk = new partWork[3 + 3 + 5 + 3 + 1];
                part_e = 14;
                partWk[part_e] = new partWork();

                max_part1 = 14; // ０クリアすべきパート数
                max_part2 = 11; // 初期化すべきパート数
            } else {
                partWk = new partWork[3 + 3 + 5 + 3 + 8 + 1];
                part10a = 14;
                partWk[part10a] = new partWork();
                part10b = 15;
                partWk[part10b] = new partWork();
                part10c = 16;
                partWk[part10c] = new partWork();
                part10d = 17;
                partWk[part10d] = new partWork();
                part10e = 18;
                partWk[part10e] = new partWork();
                part10f = 19;
                partWk[part10f] = new partWork();
                part10g = 20;
                partWk[part10g] = new partWork();
                part10h = 21;
                partWk[part10h] = new partWork();
                part_e = 22;
                partWk[part_e] = new partWork();

                max_part1 = 14 + 8; // ０クリアすべきパート数
                max_part2 = 11; // 初期化すべきパート数
            }
            part4 = 3;
            partWk[part4] = new partWork();
            part5 = 4;
            partWk[part5] = new partWork();
            part6 = 5;
            partWk[part6] = new partWork();
            part7 = 6;
            partWk[part7] = new partWork();
            part8 = 7;
            partWk[part8] = new partWork();
            part9 = 8;
            partWk[part9] = new partWork();
            part10 = 9;
            partWk[part10] = new partWork();
            part11 = 10;
            partWk[part11] = new partWork();
            part3b = 11;
            partWk[part3b] = new partWork();
            part3c = 12;
            partWk[part3c] = new partWork();
            part3d = 13;
            partWk[part3d] = new partWork();
        }
        part1 = 0;
        partWk[part1] = new partWork();
        part2 = 1;
        partWk[part2] = new partWork();
        part3 = 2;
        partWk[part3] = new partWork();

        part_table = part_table_nbrd2;
        if (board2 != 0) {
            part_table = part_table_brd2;
            if (ppz != 0) {
                part_table = part_table_ppz;
            }
        }
    }

    private MmlDatum[] MakeMmlDatum(int[] dd) {
        List<MmlDatum> ret = new ArrayList<>();
        for (int b : dd) ret.add(new MmlDatum(b));
        return ret.toArray(MmlDatum[]::new);
    }

    //EFFECT.INC
    public List<Tuple<Integer, MmlDatum[]>> efftbl;

//#region 効果音データ
    private static int[] D_000 = { // Bass Drum               	1990-06-22	05:47:11
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 220, 5, 31, 54, 15, 0, 0, 0, 127, 0
            , 8, 164, 6, 0, 62, 16, 176, 4, 0, 127, 0
            , 0xff // -1
    };
    private static int[] D_001 = new int[] { // Snare Drum              	1990-06-22	05:48:06
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            14, 144, 1, 7, 54, 16, 184, 11, 0, 93, 242
            , 0xff // -1
    };
    private static int[] D_002 = new int[] { // Low Tom                 	1990-06-22	05:49:19
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 188, 2, 0, 54, 15, 0, 0, 0, 100, 0
            , 14, 132, 3, 0, 54, 16, 196, 9, 0, 100, 0
            , 0xff // -1
    };
    private static int[] D_003 = new int[] { // Middle Tom              	1990-06-22	05:50:23
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 244, 1, 5, 54, 15, 0, 0, 0, 60, 0
            , 14, 108, 2, 0, 54, 16, 196, 9, 0, 60, 0
            , 0xff // -1
    };
    private static int[] D_004 = new int[] { // High Tom                	1990-06-22	05:51:13
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 44, 1, 0, 54, 15, 0, 0, 0, 50, 0
            , 14, 144, 1, 0, 54, 16, 196, 9, 0, 50, 0
            , 0xff // -1
    };
    private static int[] D_005 = { // Rim Shot                	1990-06-22	05:51:57
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 55, 0, 0, 62, 16, 44, 1, 0, 100, 0
            , 0xff // -1
    };
    private static int[] D_006 = { // Snare Drum 2            	1990-06-22	05:52:36
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 0, 0, 15, 55, 16, 184, 11, 0, 0, 241
            , 0xff // -1
    };
    private static int[] D_007 = { // Hi-Hat Close            	1990-06-22	05:53:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 39, 0, 0, 54, 16, 244, 1, 0, 0, 0
            , 0xff // -1
    };
    private static int[] D_008 = { // Hi-Hat Open             	1990-06-22	05:53:40
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            32, 39, 0, 0, 54, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    private static int[] D_009 = { // Crush Cymbal            	1990-06-22	05:54:11
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            31, 40, 0, 31, 54, 16, 136, 19, 0, 0, 241
            , 0xff // -1
    };
    private static int[] D_010 = { // Ride Cymbal             	1990-06-22	05:54:38
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            31, 30, 0, 0, 54, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    //
    //	Effect for 電撃MIX
    //

    private static int[] DM_001 = { // syuta                   	1994-05-25	23:13:02
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 15, 55, 16, 232, 3, 0, 0, 113
            , 2, 221, 1, 0, 55, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_002 = { // Au                      	1994-05-25	23:13:07
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 44, 1, 0, 62, 16, 44, 1, 13, 0, 0
            , 6, 44, 1, 0, 62, 16, 16, 39, 0, 80, 0
            , 0xff // -1
    };
    private static int[] DM_003 = { // syuba                   	1994-05-25	23:13:25
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 0, 55, 14, 16, 39, 0, 0, 81
            , 4, 221, 1, 10, 55, 16, 208, 7, 0, 0, 241
            , 0xff // -1
    };
    private static int[] DM_004 = { // syu                     	1994-05-25	23:17:51
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 0, 55, 16, 244, 1, 13, 0, 0
            , 8, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_005 = { // sya-                    	1994-05-25	23:19:01
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 10, 55, 16, 100, 0, 13, 0, 0
            , 16, 221, 1, 5, 55, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_006 = { // po                      	1994-05-25	23:13:32
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 144, 1, 0, 62, 16, 244, 1, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_007 = { // tattu                   	1994-05-25	23:13:37
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 15, 55, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_008 = { // zusyau                  	1994-05-25	23:13:42
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 221, 1, 31, 55, 15, 16, 39, 0, 0, 0
            , 12, 221, 1, 0, 55, 16, 136, 19, 0, 0, 17
            , 0xff // -1
    };
    private static int[] DM_009 = { // piro                    	1994-05-25	23:20:41
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 144, 1, 0, 62, 16, 232, 3, 0, 0, 0
            , 2, 200, 0, 0, 62, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_010 = { // piron                   	1994-05-25	23:20:26
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0
            , 8, 200, 0, 0, 62, 16, 184, 11, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_011 = { // pirorironn              	1994-05-25	23:21:50
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0
            , 3, 100, 0, 0, 62, 16, 208, 7, 0, 0, 0
            , 3, 200, 0, 0, 62, 16, 208, 7, 0, 0, 0
            , 3, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0
            , 8, 100, 0, 0, 62, 16, 184, 11, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_012 = { // buu                     	1994-05-25	23:23:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 208, 7, 0, 62, 15, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_013 = { // babon                   	1994-05-25	23:15:40
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 31, 55, 16, 136, 19, 0, 0, 0
            , 8, 221, 1, 31, 54, 16, 184, 11, 0, 127, 241
            , 0xff // -1
    };
    private static int[] DM_014 = { // basyu-                  	1994-05-25	23:15:44
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 25, 55, 16, 208, 7, 0, 0, 0
            , 32, 221, 1, 20, 55, 16, 112, 23, 0, 0, 19
            , 0xff // -1
    };
    private static int[] DM_015 = { // poun                    	1994-05-25	23:15:27
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 200, 0, 0, 54, 16, 136, 19, 0, 20, 0
            , 0xff // -1
    };
    private static int[] DM_016 = { // pasyu                   	1994-05-25	23:22:59
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 40, 0, 20, 54, 16, 16, 39, 0, 20, 0
            , 16, 20, 0, 5, 54, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_017 = { // KON                     	1994-05-25	23:16:07
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 88, 2, 0, 62, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_018 = { // dosun                   	1994-05-25	23:23:57
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 232, 3, 0, 62, 16, 16, 39, 0, 127, 0
            , 16, 221, 1, 0, 54, 16, 16, 39, 0, 64, 0
            , 0xff // -1
    };
    private static int[] DM_019 = { // zu                      	1994-05-25	23:24:59
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 232, 3, 31, 54, 15, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_020 = { // go                      	1994-05-25	23:24:43
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 255, 15, 31, 54, 15, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_021 = { // poyon                   	1994-05-25	23:26:17
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 0, 62, 16, 232, 3, 0, 206, 0
            , 16, 242, 0, 0, 62, 16, 112, 23, 0, 248, 0
            , 0xff // -1
    };
    private static int[] DM_022 = { // katun                   	1994-05-25	23:27:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 100, 0, 0, 62, 16, 244, 1, 0, 0, 0
            , 4, 10, 0, 0, 54, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_023 = { // syupin                  	1994-05-25	23:28:18
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            8, 221, 1, 5, 55, 16, 244, 1, 13, 0, 0
            , 24, 30, 0, 0, 54, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_024 = { // 1UP                     	1994-05-25	23:16:52
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 44, 1, 0, 62, 16, 136, 19, 0, 0, 0
            , 4, 180, 0, 0, 62, 16, 136, 19, 0, 0, 0
            , 4, 200, 0, 0, 62, 16, 136, 19, 0, 0, 0
            , 24, 150, 0, 0, 62, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_025 = { // PI                      	1994-05-25	23:16:35
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 238, 0, 0, 62, 14, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_026 = { // pikon                   	1994-05-25	23:29:19
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 200, 0, 0, 62, 16, 136, 19, 0, 0, 0
            , 16, 100, 0, 0, 62, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_027 = { // pyuu                    	1994-05-25	23:30:33
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 0, 0, 0, 54, 16, 244, 1, 13, 1, 17
            , 16, 16, 0, 16, 54, 16, 124, 21, 0, 1, 17
            , 0xff // -1
    };
    private static int[] DM_028 = { // PI                      	1994-05-25	23:16:24
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 200, 0, 0, 62, 14, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] DM_029 = { // click                   	1994-05-25	23:14:24
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 200, 0, 0, 62, 16, 32, 3, 0, 0, 0
            , 2, 100, 0, 0, 62, 16, 32, 3, 0, 0, 0
            , 2, 50, 0, 0, 62, 16, 32, 3, 0, 0, 0
            , 2, 25, 0, 0, 62, 16, 32, 3, 0, 0, 0
            , 0xff // -1
    };

    //
//	Effect for Ｒｕｓｔｙ
//
    private static int[] RS_006 = { // batan                   	1993-01-08	01:44:30
            2, 221, 1, 31, 55, 16, 232, 3, 0, 0, 0
            , 6, 221, 1, 10, 55, 16, 208, 7, 0, 0, 17
            , 0xff // -1
    };
    private static int[] RS_007 = { // dodonn                  	1993-01-08	01:39:10
            4, 232, 3, 15, 54, 16, 16, 39, 0, 127, 0
            , 16, 244, 1, 5, 54, 16, 136, 19, 0, 127, 243
            , 0xff // -1
    };
    private static int[] RS_009 = { // kisya-                  	1993-01-08	01:39:47
            4, 40, 0, 20, 54, 16, 16, 39, 0, 20, 0
            , 24, 20, 0, 5, 54, 16, 16, 39, 0, 1, 0
            , 0xff // -1
    };
    private static int[] RS_010 = { // bofu                    	1993-01-08	01:45:38
            4, 232, 3, 0, 54, 15, 16, 39, 0, 127, 0
            , 32, 10, 0, 10, 55, 16, 112, 23, 0, 0, 243
            , 0xff // -1
    };
    private static int[] RS_011 = { // gogogogo--              	1993-06-29	12:27:41
            96, 255, 15, 31, 54, 16, 96, 234, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_012 = { // karakara                	1993-06-29	12:16:36
            64, 10, 0, 0, 54, 16, 32, 78, 0, 0, 129
            , 0xff // -1
    };
    private static int[] RS_013 = { // buonn                   	1993-01-08	01:47:56
            8, 208, 7, 0, 62, 16, 144, 1, 13, 0, 0
            , 8, 208, 7, 0, 62, 16, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_015 = { // tyattu                  	1993-01-08	01:49:27
            4, 20, 0, 8, 54, 16, 184, 11, 0, 0, 225
            , 0xff // -1
    };
    private static int[] RS_018 = { // zu                      	1993-01-08	01:51:05
            4, 208, 7, 30, 54, 16, 160, 15, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_019 = { // saaaa                   	1993-06-29	12:28:05
            60, 221, 1, 4, 55, 10, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_020 = { // za                      	1993-01-08	01:52:35
            6, 221, 1, 16, 55, 16, 136, 19, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_021 = { // TYARIN                  	1993-06-29	12:29:19
            4, 40, 0, 0, 54, 15, 16, 39, 0, 0, 0
            , 8, 30, 0, 0, 62, 16, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_022 = { // SYUWAWA                 	1993-06-29	12:35:38
            48, 100, 0, 0, 55, 16, 136, 19, 13, 255, 33
            , 12, 50, 0, 0, 55, 13, 136, 19, 0, 0, 33
            , 12, 221, 1, 0, 55, 12, 16, 39, 0, 0, 33
            , 12, 221, 1, 0, 55, 11, 16, 39, 0, 0, 33
            , 12, 221, 1, 0, 55, 10, 16, 39, 0, 0, 33
            , 12, 221, 1, 0, 55, 9, 16, 39, 0, 0, 33
            , 0xff // -1
    };
    private static int[] RS_024 = { // PIN                     	1993-06-29	12:36:42
            6, 100, 0, 0, 62, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_026 = { // KAMINARI                	1993-06-29	12:42:57
            4, 23, 0, 31, 55, 16, 208, 7, 0, 0, 0
            , 64, 15, 0, 31, 55, 16, 152, 58, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_027 = { // PI                      	1993-06-29	12:44:03
            3, 238, 0, 0, 62, 14, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_028 = { // KEIKOKU                 	1993-06-29	12:46:13
            7, 44, 1, 0, 62, 16, 160, 15, 0, 0, 0
            , 7, 44, 1, 0, 62, 16, 208, 7, 0, 0, 0
            , 48, 44, 1, 0, 62, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] RS_029 = { // ETC 1                   	1993-06-29	12:46:54
            96, 208, 7, 0, 62, 16, 16, 39, 0, 236, 0
            , 0xff // -1
    };
    private static int[] RS_030 = { // BUFOFOFO                	1993-06-29	12:58:12
            8, 208, 7, 0, 62, 16, 16, 39, 0, 176, 0
            , 8, 8, 7, 0, 62, 16, 16, 39, 0, 176, 0
            , 8, 64, 6, 0, 62, 16, 16, 39, 0, 176, 0
            , 48, 120, 5, 0, 62, 16, 16, 39, 0, 186, 0
            , 0xff // -1
    };
    private static int[] RS_031 = { // ETC 3                   	1993-06-29	12:49:32
            8, 232, 3, 0, 62, 16, 16, 39, 0, 80, 0
            , 8, 176, 4, 0, 62, 16, 16, 39, 0, 80, 0
            , 8, 20, 5, 0, 62, 16, 16, 39, 0, 80, 0
            , 48, 120, 5, 0, 62, 16, 16, 39, 0, 80, 0
            , 0xff // -1
    };
    private static int[] RS_032 = { // ETC 4                   	1993-06-29	12:50:11
            96, 0, 0, 0, 62, 16, 16, 39, 0, 128, 0
            , 0xff // -1
    };
    private static int[] RS_033 = { // HADE BOMB               	1993-06-29	12:52:06
            4, 100, 0, 31, 54, 16, 208, 7, 0, 127, 0
            , 32, 0, 0, 31, 54, 16, 16, 39, 0, 127, 129
            , 0xff // -1
    };
    private static int[] RS_035 = { // JARARAN                 	1993-06-29	13:02:17
            2, 244, 1, 20, 54, 16, 16, 39, 0, 252, 0
            , 2, 144, 1, 15, 54, 16, 16, 39, 0, 252, 65
            , 2, 44, 1, 10, 62, 16, 16, 39, 0, 252, 65
            , 2, 200, 0, 5, 54, 16, 16, 39, 0, 252, 65
            , 16, 150, 0, 0, 62, 16, 184, 11, 0, 0, 0
            , 0xff // -1
    };

    //
    //	Effect for ポゼッショナー
    //

    private static int[] PO_011 = { // Rain fall               	1990-06-22	05:55:43
            254, 221, 1, 3, 55, 10, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_012 = { // Spinner                 	1990-06-22	05:57:18
            24, 140, 0, 0, 62, 16, 88, 27, 0, 14, 0
            , 0xff // -1
    };
    private static int[] PO_013 = { // Kaminari                	1990-06-22	05:59:16
            48, 160, 15, 31, 54, 16, 16, 39, 0, 0, 1
            , 0xff // -1
    };
    private static int[] PO_014 = { // Sairen                  	1990-06-22	06:00:45
            31, 100, 0, 0, 62, 16, 88, 27, 0, 255, 0
            , 0xff // -1
    };
    private static int[] PO_015 = { // Door Shut               	1990-06-22	06:03:28
            6, 221, 1, 8, 55, 16, 184, 11, 0, 0, 241
            , 8, 144, 1, 0, 54, 16, 144, 1, 13, 216, 0
            , 0xff // -1
    };
    private static int[] PO_016 = { // Kiteki                  	1990-06-22	06:05:23
            96, 160, 15, 0, 62, 16, 48, 117, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_017 = { // Ship Bomb               	1990-06-22	06:06:54
            4, 221, 1, 31, 55, 16, 208, 7, 0, 0, 0
            , 64, 221, 1, 20, 55, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_018 = { // Spinner 2               	1990-06-22	06:08:08
            64, 120, 0, 0, 54, 16, 16, 39, 0, 2, 0
            , 0xff // -1
    };
    private static int[] PO_019 = { // Helli                   	1990-06-22	06:09:58
            4, 221, 1, 4, 55, 16, 208, 7, 0, 0, 0
            , 4, 221, 1, 0, 55, 16, 208, 7, 0, 0, 0
            , 4, 221, 1, 4, 55, 16, 232, 3, 0, 0, 0
            , 4, 221, 1, 0, 55, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_020 = { // Kinzoku Sagyou          	1990-06-22	07:23:41
            16, 30, 0, 5, 54, 16, 160, 15, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_021 = { // Kaze (DAME)             	1990-06-22	06:13:46
            16, 220, 5, 0, 62, 15, 16, 39, 0, 0, 0
            , 8, 220, 5, 0, 62, 15, 16, 39, 0, 246, 0
            , 48, 140, 5, 0, 62, 16, 16, 39, 0, 10, 0
            , 0xff // -1
    };
    private static int[] PO_022 = { // Taikushitu Soto         	1990-06-22	06:15:55
            4, 160, 15, 31, 54, 16, 184, 11, 0, 0, 0
            , 24, 184, 11, 8, 54, 16, 136, 19, 0, 40, 20
            , 0xff // -1
    };
    private static int[] PO_023 = { // Punch                   	1990-06-22	06:17:13
            4, 160, 15, 31, 54, 16, 208, 7, 0, 10, 0
            , 8, 221, 1, 28, 54, 16, 208, 7, 0, 127, 0
            , 0xff // -1
    };
    private static int[] PO_024 = { // Shower                  	1990-06-22	06:19:08
            254, 0, 0, 0, 55, 10, 0, 0, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_025 = { // Shokki                  	1990-06-22	06:22:14
            6, 31, 0, 4, 54, 16, 232, 3, 0, 0, 0
            , 8, 30, 0, 0, 54, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_026 = { // Tobikomi                	1990-06-22	06:24:09
            8, 220, 5, 25, 54, 16, 184, 11, 0, 127, 0
            , 48, 221, 1, 10, 55, 16, 64, 31, 0, 0, 18
            , 0xff // -1
    };
    private static int[] PO_027 = { // Air Fukidasi            	1990-06-22	06:25:35
            4, 208, 7, 0, 55, 16, 208, 7, 0, 0, 0
            , 48, 221, 1, 4, 55, 16, 16, 39, 0, 0, 20
            , 0xff // -1
    };
    private static int[] PO_028 = { // Heavy Door Open         	1990-06-22	07:23:33
            48, 208, 7, 31, 54, 16, 152, 58, 0, 251, 0
            , 0xff // -1
    };
    private static int[] PO_029 = { // Car Door Shut           	1990-06-22	07:23:30
            16, 232, 3, 31, 54, 16, 184, 11, 0, 127, 0
            , 0xff // -1
    };
    private static int[] PO_030 = { // Car Come'in             	1990-06-22	06:30:31
            4, 160, 15, 31, 54, 15, 16, 39, 0, 0, 0
            , 96, 160, 15, 28, 54, 16, 32, 78, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_031 = { // Ice Hikkaki             	1990-06-22	06:31:26
            2, 10, 0, 0, 54, 16, 244, 1, 0, 0, 0
            , 2, 20, 0, 0, 54, 16, 244, 1, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_032 = { // Ship Crush Down         	1990-06-22	07:23:23
            64, 160, 15, 20, 54, 16, 48, 117, 0, 1, 22
            , 192, 221, 1, 31, 55, 16, 48, 117, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_033 = { // Toraware                	1990-06-22	06:35:02
            32, 232, 3, 0, 54, 16, 64, 31, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_034 = { // Sekizou Break           	1990-06-22	06:36:14
            4, 221, 1, 31, 55, 15, 16, 39, 0, 0, 0
            , 64, 221, 1, 10, 55, 16, 16, 39, 0, 0, 18
            , 0xff // -1
    };
    private static int[] PO_035 = { // Blaster Shot            	1990-06-22	06:37:55
            4, 221, 1, 31, 55, 16, 184, 11, 0, 0, 0
            , 4, 160, 15, 20, 54, 16, 184, 11, 0, 20, 0
            , 64, 0, 0, 4, 54, 16, 16, 39, 0, 1, 20
            , 0xff // -1
    };
    private static int[] PO_036 = { // Seifuku Yabuki          	1990-06-22	06:39:58
            16, 221, 1, 4, 55, 14, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_037 = { // Miminari                	1990-06-22	06:42:13
            4, 8, 0, 0, 62, 16, 1, 0, 8, 0, 0
            , 64, 0, 0, 0, 62, 16, 64, 31, 0, 1, 0
            , 0xff // -1
    };
    private static int[] PO_038 = { // Sekizou Ayasige         	1990-06-22	06:44:23
            40, 160, 15, 0, 62, 16, 232, 253, 0, 246, 0
            , 50, 16, 14, 0, 62, 16, 32, 78, 0, 10, 0
            , 0xff // -1
    };
    private static int[] PO_039 = { // Voiler & Engine         	1990-06-22	07:23:14
            60, 221, 1, 30, 55, 14, 48, 117, 0, 0, 242
            , 16, 184, 11, 2, 55, 16, 112, 23, 0, 40, 17
            , 0xff // -1
    };
    private static int[] PO_040 = { // Suimen                  	1990-06-22	06:47:58
            4, 10, 0, 4, 54, 16, 232, 3, 0, 0, 0
            , 6, 221, 1, 0, 55, 16, 208, 7, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_041 = { // Kika                    	1990-06-22	06:48:29
            64, 221, 1, 0, 55, 16, 16, 39, 0, 0, 20
            , 0xff // -1
    };
    private static int[] PO_042 = { // Change Kakyuu           	1990-06-22	06:50:00
            48, 232, 3, 0, 62, 16, 16, 39, 0, 10, 0
            , 0xff // -1
    };
    private static int[] PO_043 = { // Change Blue             	1990-06-22	06:51:47
            15, 100, 0, 0, 62, 16, 232, 3, 14, 252, 0
            , 0xff // -1
    };
    private static int[] PO_044 = { // Youma Funsyutu          	1990-06-22	06:54:06
            6, 221, 1, 4, 55, 16, 208, 7, 0, 0, 0
            , 4, 100, 0, 4, 54, 16, 160, 15, 0, 236, 0
            , 64, 221, 1, 8, 55, 16, 64, 31, 0, 0, 246
            , 0xff // -1
    };
    private static int[] PO_045 = { // Kekkai                  	1990-06-22	07:23:06
            128, 232, 3, 31, 54, 16, 48, 117, 0, 1, 242
            , 0xff // -1
    };
    private static int[] PO_046 = { // Gosintou 1              	1990-06-22	06:56:47
            4, 20, 0, 0, 54, 16, 232, 3, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_047 = { // Gosintou 2              	1990-06-22	06:58:25
            8, 208, 7, 0, 62, 16, 232, 3, 13, 246, 0
            , 64, 208, 7, 0, 62, 16, 16, 39, 0, 2, 0
            , 0xff // -1
    };
    private static int[] PO_048 = { // Gosintou 3              	1990-06-22	07:00:22
            8, 221, 1, 0, 55, 16, 32, 3, 13, 0, 17
            , 16, 221, 1, 0, 55, 16, 208, 7, 0, 0, 17
            , 0xff // -1
    };
    private static int[] PO_049 = { // Hand Blaster            	1990-06-22	07:01:53
            4, 160, 15, 31, 54, 16, 184, 11, 0, 0, 0
            , 4, 40, 0, 0, 54, 16, 232, 3, 0, 246, 0
            , 64, 221, 1, 0, 55, 16, 16, 39, 0, 0, 18
            , 0xff // -1
    };
    private static int[] PO_050 = { // Magic                   	1990-06-22	07:04:00
            4, 32, 0, 0, 62, 16, 208, 7, 0, 0, 0
            , 24, 32, 0, 0, 54, 16, 64, 31, 0, 255, 0
            , 90, 160, 15, 31, 54, 16, 48, 117, 0, 216, 244
            , 0xff // -1
    };
    private static int[] PO_051 = { // Grabiton 1              	1990-06-22	07:04:41
            4, 221, 1, 31, 55, 16, 16, 39, 0, 0, 0
            , 31, 221, 1, 0, 55, 16, 16, 39, 0, 0, 17
            , 0xff // -1
    };
    private static int[] PO_052 = { // Grabiton 2              	1990-06-22	07:05:10
            128, 160, 15, 31, 54, 16, 48, 117, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_053 = { // Attack Kakyuu           	1990-06-22	07:06:38
            4, 160, 15, 31, 54, 16, 16, 39, 0, 0, 0
            , 16, 221, 1, 0, 55, 16, 112, 23, 0, 0, 0
            , 0xff // -1
    };
    private static int[] PO_054 = { // Attack Blue(TEKITOU)    	1990-06-22	07:08:33
            6, 100, 0, 0, 54, 16, 244, 1, 13, 251, 0
            , 16, 70, 0, 0, 54, 16, 112, 23, 0, 127, 0
            , 0xff // -1
    };
    private static int[] PO_055 = { // Attack Red              	1990-06-22	07:10:10
            20, 184, 11, 0, 54, 14, 16, 39, 0, 156, 0
            , 16, 232, 3, 0, 54, 16, 112, 23, 0, 100, 0
            , 0xff // -1
    };
    private static int[] PO_056 = { // Attack White            	1990-06-22	07:11:16
            4, 0, 0, 4, 54, 16, 16, 39, 0, 127, 241
            , 16, 0, 0, 0, 54, 16, 112, 23, 0, 10, 17
            , 0xff // -1
    };
    private static int[] PO_057 = { // Attack Black            	1990-06-22	07:22:10
            4, 200, 0, 4, 54, 16, 208, 7, 0, 127, 17
            , 10, 0, 0, 0, 54, 16, 88, 2, 13, 1, 0
            , 24, 10, 0, 0, 54, 16, 112, 23, 0, 5, 17
            , 0xff // -1
    };
    private static int[] PO_058 = { // Attack Last             	1990-06-22	07:22:14
            20, 60, 0, 4, 54, 14, 16, 39, 0, 255, 0
            , 20, 40, 0, 0, 54, 14, 16, 39, 0, 1, 113
            , 20, 60, 0, 10, 54, 16, 112, 23, 0, 1, 20
            , 0xff // -1
    };
    private static int[] PO_059 = { // Damage 1                	1990-06-22	07:17:32
            4, 221, 1, 31, 54, 16, 184, 11, 0, 127, 0
            , 16, 221, 1, 0, 55, 16, 112, 23, 0, 0, 33
            , 0xff // -1
    };
    private static int[] PO_060 = { // Damage 2                	1990-06-22	07:19:18
            8, 232, 3, 31, 54, 14, 16, 39, 0, 100, 0
            , 8, 120, 5, 31, 54, 15, 16, 39, 0, 156, 113
            , 16, 88, 2, 31, 54, 16, 112, 23, 0, 127, 241
            , 0xff // -1
    };
    private static int[] PO_061 = { // Attack                  	1990-06-22	07:22:55
            8, 0, 0, 31, 54, 16, 184, 11, 0, 100, 0
            , 24, 221, 1, 0, 55, 16, 16, 39, 0, 0, 17
            , 0xff // -1
    };

//
//	Effect for ＮＡＤＩＡ
//

    private static int[] ND_000 = { // MAP                     	1992-01-27	17:32:40
            48, 221, 1, 0, 62, 16, 16, 39, 0, 255, 0
            , 0xff // -1
    };
    private static int[] ND_001 = { // SONAR                   	1992-01-27	17:33:23
            192, 200, 0, 0, 62, 16, 64, 156, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_002 = { // KOUKOU                  	1992-01-27	17:57:44
            254, 221, 1, 8, 55, 12, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_003 = { // MEGIDO                  	1992-01-27	17:35:47
            192, 200, 0, 0, 54, 16, 16, 39, 13, 255, 0
            , 6, 221, 1, 0, 54, 16, 16, 39, 0, 127, 0
            , 192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 248
            , 0xff // -1
    };
    private static int[] ND_004 = { // JINARI                  	1992-01-27	17:36:37
            254, 221, 1, 31, 54, 14, 16, 39, 0, 128, 113
            , 0xff // -1
    };
    private static int[] ND_005 = { // SWITCH                  	1992-01-27	17:37:21
            6, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0
            , 6, 20, 0, 0, 54, 16, 160, 15, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_006 = { // DOSYUUNN                	1992-01-27	17:38:01
            6, 221, 1, 0, 54, 16, 16, 39, 0, 127, 0
            , 192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 24
            , 0xff // -1
    };
    private static int[] ND_007 = { // GYUOON                  	1992-01-27	17:39:09
            192, 232, 3, 31, 54, 16, 96, 234, 0, 252, 0
            , 0xff // -1
    };
    private static int[] ND_008 = { // PIPIPIPI                	1992-01-27	17:40:16
            64, 150, 0, 0, 62, 16, 176, 4, 8, 0, 0
            , 0xff // -1
    };
    private static int[] ND_009 = { // SYUBATTU                	1992-01-27	17:41:16
            12, 221, 1, 0, 55, 16, 232, 3, 13, 0, 20
            , 24, 221, 1, 15, 55, 16, 64, 31, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_010 = { // BEAM UNARI              	1992-01-27	17:42:05
            254, 25, 0, 0, 54, 14, 16, 39, 0, 0, 145
            , 0xff // -1
    };
    private static int[] ND_011 = { // BEAM KAKUSAN            	1992-01-27	17:43:07
            6, 221, 1, 15, 55, 16, 160, 15, 0, 0, 0
            , 192, 208, 7, 0, 54, 16, 96, 234, 0, 248, 0
            , 0xff // -1
    };
    private static int[] ND_012 = { // ORGAN                   	1992-01-27	18:01:45
            48, 221, 1, 0, 62, 14, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_013 = { // PANEL                   	1992-01-27	17:57:15
            6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0
            , 6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0
            , 6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0
            , 6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0
            , 24, 20, 0, 10, 54, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_014 = { // DRILL                   	1992-01-27	17:45:25
            254, 160, 15, 31, 54, 15, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_015 = { // PRAZMA                  	1992-01-27	17:45:59
            6, 20, 0, 15, 55, 16, 112, 23, 0, 0, 0
            , 6, 20, 0, 0, 54, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_016 = { // BABEL                   	1992-01-27	17:46:34
            254, 160, 15, 0, 62, 16, 16, 39, 14, 0, 0
            , 0xff // -1
    };
    private static int[] ND_017 = { // ELEVETOR                	1992-01-27	17:47:27
            12, 233, 1, 0, 54, 14, 16, 39, 0, 255, 0
            , 254, 221, 1, 0, 54, 14, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_018 = { // MEGIDO HASSYA           	1992-01-27	17:48:04
            254, 160, 15, 15, 54, 15, 16, 39, 13, 0, 0
            , 0xff // -1
    };
    private static int[] ND_019 = { // DAIBAKUHATU             	1992-01-27	18:28:56
            12, 221, 1, 31, 54, 16, 16, 39, 0, 127, 0
            , 144, 0, 0, 0, 54, 16, 96, 234, 0, 127, 24
            , 192, 160, 15, 31, 54, 16, 80, 70, 14, 0, 0
            , 0xff // -1
    };
    private static int[] ND_020 = { // NAMI                    	1992-01-27	17:50:59
            254, 221, 1, 0, 55, 16, 16, 39, 14, 0, 0
            , 0xff // -1
    };
    private static int[] ND_021 = { // DOOOONN                 	1992-01-27	17:51:39
            96, 208, 7, 0, 54, 16, 16, 39, 0, 40, 0
            , 0xff // -1
    };
    private static int[] ND_022 = { // DOGA                    	1992-01-27	17:52:18
            6, 221, 1, 31, 54, 16, 16, 39, 0, 127, 0
            , 12, 221, 1, 0, 55, 16, 160, 15, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_023 = { // PISI                    	1992-01-27	17:52:53
            6, 20, 0, 31, 54, 16, 16, 39, 0, 0, 0
            , 24, 20, 0, 0, 54, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_024 = { // BLUE WATER              	1992-01-27	17:53:15
            254, 15, 0, 0, 62, 14, 16, 39, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_025 = { // HOWAWAN                 	1992-01-27	17:56:51
            12, 144, 1, 0, 62, 16, 100, 0, 13, 254, 0
            , 12, 134, 1, 0, 62, 16, 100, 0, 13, 254, 0
            , 12, 124, 1, 0, 62, 16, 100, 0, 13, 254, 0
            , 12, 114, 1, 0, 62, 16, 100, 0, 13, 254, 0
            , 48, 90, 1, 0, 62, 16, 16, 39, 0, 254, 0
            , 0xff // -1
    };
    private static int[] ND_026 = { // ZUGAN                   	1992-01-27	17:19:49
            6, 221, 1, 31, 55, 16, 160, 15, 0, 0, 0
            , 64, 221, 1, 24, 55, 16, 32, 78, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_027 = { // DAAANN                  	1992-01-27	17:20:28
            48, 221, 1, 31, 55, 16, 152, 58, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_028 = { // DOGOOOONN               	1992-01-27	17:21:14
            6, 221, 1, 1, 54, 16, 16, 39, 0, 127, 0
            , 192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_029 = { // GASYA                   	1992-01-27	17:22:08
            3, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0
            , 12, 221, 1, 1, 55, 16, 160, 15, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_030 = { // BASYUSYUSYU             	1992-01-27	17:22:52
            3, 221, 1, 15, 55, 15, 16, 39, 0, 0, 0
            , 192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 113
            , 0xff // -1
    };
    private static int[] ND_031 = { // DOSYUSYUSYU             	1992-01-27	17:24:31
            192, 0, 0, 0, 54, 16, 96, 234, 0, 128, 17
            , 0xff // -1
    };
    private static int[] ND_032 = { // SYUSYUUUUNN             	1992-01-27	17:25:34
            12, 221, 1, 0, 55, 15, 16, 39, 0, 0, 113
            , 32, 221, 1, 0, 55, 16, 32, 78, 0, 0, 17
            , 0xff // -1
    };
    private static int[] ND_033 = { // BASYANN - HYURURURU     	1992-01-27	18:00:33
            6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 193
            , 32, 221, 1, 4, 55, 16, 16, 39, 0, 0, 0
            , 192, 0, 0, 0, 54, 16, 96, 234, 0, 1, 0
            , 0xff // -1
    };
    private static int[] ND_034 = { // ZYURUZYURU              	1992-01-27	17:27:38
            192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 113
            , 0xff // -1
    };
    private static int[] ND_035 = { // ZUGOGOGOGO              	1992-01-27	17:29:07
            6, 221, 1, 15, 55, 16, 16, 39, 0, 0, 0
            , 6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241
            , 6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241
            , 6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241
            , 192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 248
            , 0xff // -1
    };
    private static int[] ND_036 = { // ZUGOOOONN               	1992-01-27	17:29:50
            6, 221, 1, 15, 55, 16, 16, 39, 0, 0, 0
            , 192, 221, 1, 31, 55, 16, 48, 117, 0, 0, 0
            , 0xff // -1
    };
    private static int[] ND_037 = { // BI--                    	1992-01-27	17:59:08
            48, 40, 0, 0, 62, 16, 100, 0, 8, 0, 0
            , 0xff // -1
    };
    private static int[] ND_038 = { // BASYUSYUUU              	1992-01-27	17:30:38
            48, 221, 1, 0, 55, 16, 16, 39, 0, 0, 145
            , 0xff // -1
    };
    private static int[] ND_039 = { // BISYU                   	1992-01-27	17:31:52
            6, 232, 3, 15, 54, 16, 16, 39, 0, 127, 0
            , 24, 221, 1, 0, 55, 16, 16, 39, 0, 0, 0
            , 0xff // -1
    };

    public MmlDatum cmd;
    public LinePos clp;
    public int currentWriter;

//#endregion
}
