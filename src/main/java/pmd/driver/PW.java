package pmd.driver;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import dotnet4j.util.compat.Tuple;
import musicDriverInterface.LinePos;
import musicDriverInterface.MmlDatum;


public class PW {

    public final Object lockObj = new Object();
    public final Object systemInterrupt = new Object();

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

    // DotNET-specific
    MmlDatum[] md;

    public MmlDatum[] getMd() {
        return md;
    }

    MmlDatum[] crtEfcDat;

    public MmlDatum[] getCrtEfcDat() {
        return crtEfcDat;
    }

    public final byte[] pcmWk = new byte[4 * 256 + 2 + 128];
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

    public static final String ver = "4.8s";
    public static final int vers = 0x48;
    public static final char verc = 's';
    public static final String date = "Jan.22nd 2020";

    public static final int mdata_def = 16;
    public static final int voice_def = 8;
    public static final int effect_def = 4;
    public static final int key_def = 1;

    public static final String _myname = "PMD     COM";

    public int va = 0; // when 1 for VA-MSDOS
    public int board2 = 0; // when 1 for board2, 2: with Otomi-chan
    public int adpcm = 0; // when 1 use adpcm
    public int ademu = 0; // when 1 emulate ADPCM
    public int pcm = 0; // when 1 use PCM
    public int ppz = 0; // when 1 use PPZ8
    public int sync = 0; // when 1 use MIDISYNC
    public int vsync = 0; // when 1 stop VSync
    public static final String resmes = "PMD ver." + ver;
    public int fmvd_init = 16; // The 98 has a smaller FM sound source than the 88.

    public static final int pmdvector = 0x60; // Interrupt Vector for PMD
    public static final int ppsdrv = 0x64; // ppsdrv interrupt vector
    public static final int ppz_vec = 0x7f; // Interrupt vectors for ppz8

    //
    // constant
    //
    public int ms_cmd = 0x000; // 8259 Master Port
    public int ms_msk = 0x002; // 8259 Master/Mask
    public int sl_cmd = 0x008; // 8259 Slave Port
    public int sl_msk = 0x00a; // 8259 Slave/Mask

    //
    // Program Start
    //

    //int60_head: jmp short int60_main
    //db 'PMD' ;+2  For resident check
    //db  vers ;+5
    //db verc;+6
    public int int60ofs; // ? ;+7
    public int int60seg; // ? ;+9
    public int int5ofs; // ? ;+11
    public int int5seg; // ? ;+13
    public int maskpush; // ? ;+15
    public int vector; // ? ;+16
    public int int_level; // ? ;+18

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

    public static final byte com_end_0c0h = (byte) 0xf7;

    public final int[] vol_tbl = {0, 0, 0, 0};

    public short seed;

    //
    // WORK AREA
    //

    public short fm_port1; // w FM Sound Source I/O port Work(1)
    public short fm_port2; // w FM Sound Source I/O port Work(2)
    public short ds_push; // w For INT60 ds push
    public short dx_push; // w For INT60 dx push
    public byte ah_push; // b For INT60 ah push
    public byte al_push; // b For INT60 al push
    public byte partb; // b Processing Part Number
    public byte tieflag; // b & flag
    public byte volpush_flag; // b Flag for down the volume of the next sound
    public byte rhydmy; // b R part dummy performance data
    public byte fmsel; // b FM Heads or tails flag
    public byte[] fmKeyOnDataTbl = new byte[6]; // KUMA: The following six parameters
    //public int[] omote_key = { 0, 0, 0 };
    public byte omote_key1Ptr = 0; // b FM keyondata Table 1
    public byte omote_key2Ptr = 1; // b  FM keyondata Table 2
    public byte omote_key3Ptr = 2; // b FM keyondata Table 3
    //public int[] ura_key = { 0, 0, 0 };
    public byte ura_key1Ptr = 3; // b FM keyondata tails 1
    public byte ura_key2Ptr = 4; // b FM keyondata tails 2
    public byte ura_key3Ptr = 5; // b FM keyondata tails 3
    public byte loop_work; // b Loop Work
    public byte ppsdrv_flag; // b ppsdrv flag
    public short prgdat_adr2; // w First address of tone data in song data (for sound effects)
    public short pcmrepeat1; // w PCM repeat address 1
    public short pcmrepeat2; // w PCM repeat address 2
    public short pcmrelease; // w PCM Release start address
    public byte lastTimerAtime; // b TimerATime value at the time of the previous interrupt
    public byte music_flag; // b B0: Next MSTART 1: Next MSTOP Flag
    public byte slotdetune_flag; // b Are you using FM3 Slot Detune?
    public byte slot3_flag; // b FM3 Slot Sound Effect Mode Flag
    public short eoi_adr; // w I/O address to send EOI to
    public byte eoi_data; // b Data for EOI
    public short mask_adr; // w I/O address to be masked
    public byte mask_data; // b Data for Masking (Mask with Or)
    public byte mask_data2; // b Data for Mask (remove Mask with And)
    public short ss_push; // w Push SS during FMint
    public short sp_push; // w Push SP during FMint
    public byte fm3_alg_fb; // b Alg/fb of the last defined tone of FM3ch
    public byte af_check; // b Flag to set alg/fb for FM3ch
    public byte ongen; // b Sound source 0=none/2203 1=2608
    public byte lfo_switch; // b Local LFO Switch

    public static final byte[] rhydat = { // Drum rhythm data
            //PT PAN/VOLUME  KEYON
            0x18, (byte) 0b1101_1111, 0b0000_0001, // bass
            0x19, (byte) 0b1101_1111, 0b0000_0010, // Snare
            0x1c, 0b0101_1111, 0b0001_0000, // Tom [LOW]
            0x1c, (byte) 0b1101_1111, 0b0001_0000, // Tom [MID]
            0x1c, (byte) 0b1001_1111, 0b0001_0000, // Tom [HIGH]
            0x1d, (byte) 0b1101_0011, 0b0010_0000, // rim
            0x19, (byte) 0b1101_1111, 0b0000_0010, // Clap
            0x1b, (byte) 0b1001_1100, (byte) 0b1000_1000, // C Hi-Hat
            0x1a, (byte) 0b1001_1101, 0b0000_0100, // O Hi-Hat
            0x1a, (byte) 0b1101_1111, 0b0000_0100, // cymbal
            0x1a, 0b0101_1110, 0b0000_0100, // RIDE Cymbals
    };

    // PMD.ASM 8030-
    public byte open_work = 0; // label byte
    public int mmlbuf = 0; // Musicdata's address+1
    public int tondat = 0; // Voicedata's address
    public int efcdat = -1; // FM Effect data address
    public int fm1_port1 = 0; // FM sound source I/O port (Table 1)
    public int fm1_port2 = 0; // FM sound source I/O port (Table 2)
    public int fm2_port1 = 0; // FM sound source I/O port (Trail 1)
    public int fm2_port2 = 0; // FM sound source I/O port (Trail 2)
    public int fmint_ofs = 0; // FM interrupt hook address offset
    public int fmint_seg = 0; // FM interrupt hook address
    public int efcint_ofs = 0; // Sound effect interrupt hook address offset
    public int efcint_seg = 0; // Sound effect interrupt hook address
    public int prgdat_adr = 0; // First address of tone data in song data
    public int radtbl = 0; // R part offset table start address
    public int rhyadr = 0; // R Part playing address
    public byte rhythmmask = 0; // Rhythm sound source mask x8c/10h bit support
    public byte board = 0; // FM sound board available/not available flag
    public byte key_check = 0; // ESC/GRPH key Check flag
    public byte fm_voldown = 0; // FM voldown number
    public byte ssg_voldown = 0; // PSG voldown number
    public byte pcm_voldown = 0; // PCM voldown number
    public byte rhythm_voldown = 0; // RHYTHM voldown number
    public byte prg_flg = 0; // Does the song data contain a tone?
    public byte x68_flg = 0; // OPM flag
    public byte status = 0; // status1
    public byte status2 = 0; // status2
    public byte tempo_d = 0; // tempo(TIMER-B)
    public byte fadeout_speed = 0; // Fadeout Speed
    public byte fadeout_volume = 0; // Fadeout Volume
    public byte tempo_d_push = 0; // tempo(TIMER-B) / For storage
    public byte syousetu_lng = 0; // Measure length
    public byte opncount = 0; // Shortest note counter
    public byte timerATime = 0; // TimerA Counter
    public byte effflag = 0; // PSG sound effect on/off flag
    public byte psnoi = 0; // PSG noise frequency
    public byte psnoi_last = 0; // PSG noise frequency (last defined value)
    public byte fm_effec_num = 0; // FM sound effect number being played
    public byte fm_effec_flag = 0; // FM sound effect being played flag(1)
    public byte disint = 0; // Whether to disable interrupts during FM interrupt
    public byte pcmflag = 0; // PCM sound effect being played flag
    public int pcmstart = 0; // PCM tone start value
    public int pcmstop = 0; // PCM tone stop value
    public byte pcm_effec_num = 0; // PCM sound effect number being played
    public int _pcmstart = 0; // PCM sound effect start value
    public int _pcmstop = 0; // PCM sound effect stop value
    public int _voice_delta_n = 0; // PCM sound effect delta_n value
    public byte _pcmpan = 0; // PCM sound effect pan
    public byte _pcm_volume = 0; // PCM sound effect volume
    public byte rshot_dat = 0; // Rhythm sound source shot flag
    public final byte[] rdat = new byte[6]; // Rhythm sound source Volume/pan data
    public byte rhyvol = 0b0011_1100; // Rhythm Total Level
    public int kshot_dat = 0; // SSG Rhythm Shot Flag
    public int ssgefcdat = 0; // efftbl  PSG Effect data address
    public int ssgefclen = 0; // efftblend - Length of efftbl PSG Effecdata
    public byte play_flag = 0; // play flag
    public byte pause_flag = 0; // pause flag
    public byte fade_stop_flag = 0; // Flag for whether to MSTOP after Fadeout
    public byte kp_rhythm_flag = 0; // Flag to play Rhythm sound source in K/Rpart
    public byte timerBFlag = 0; // TimerB interrupt in progress? Flag
    public byte timerAFlag = 0; // TimerA interrupt in progress? Flag
    public byte int60flag = 0; // INT60H interrupt in progress? Flag
    public byte int60_result = 0; // INT60H execution ErrorFlag
    public byte pcm_gs_flag = 0; // ADPCM use enable flag (0 for enable)
    public byte esc_sp_key = 0; // ESC +?? Key Code
    public byte grph_sp_key = 0; // GRPH+?? Key Code
    public byte rescut_cant = 0; // Resident release prohibition flag
    public short slot_detune1 = 0; // FM3 Slot Detune value slot1
    public short slot_detune2 = 0; // FM3 Slot Detune value slot2
    public short slot_detune3 = 0; // FM3 Slot Detune value slot3
    public short slot_detune4 = 0; // FM3 Slot Detune value slot4
    public int wait_clock = 0; // FM ADDRESS-DATA Loop $ count
    public int wait1_clock = 0; // loop $ 1 speed
    public byte ff_tempo = 0; // TimerB value during fast forward
    public byte pcm_access = 0; // 1 during PCM set
    public byte timerB_speed = 0; // Current value of TimerB (= ff_tempo if in ff)
    public byte fadeout_flag = 0; // When fout is called from inside 1
    public byte adpcm_wait = 0; // ADPCM defined speed
    public byte revpan = 0; // PCM86 reverse flag
    public byte pcm86_vol = 0; // Should I adjust the volume of the PCM86 to match the SPB?
    public short syousetu = 0; // Bar Counter
    public byte int5_flag = 0; // FM sound source interrupt? Flag
    public byte port22h = 0; // Last output value (hlfo) to OPN-PORT 22H
    public byte tempo_48 = 0; // Current tempo (clock= 48 t value)
    public byte tempo_48_push = 0; // Current tempo (same as above/for saving)
    public byte rew_sp_key = 0; // GRPH+?? (rew) Key Code
    public byte intHook_flag = 0; // int_Hook flag B0:TB B1:TA
    public byte skip_flag = 0; // normal:0 During forward SKIP:1 During backward SKIP:2
    public byte _fm_voldown = 0; // FM voldown Number (for storage)
    public byte _ssg_voldown = 0; // PSG voldown Number (for storage)
    public byte _pcm_voldown = 0; // PCM voldown Number (for storage)
    public byte _rhythm_voldown = 0; // RHYTHM voldown Number (for storage)
    public byte _pcm86_vol = 0; // Should I adjust the volume of the PCM86 to match the SPB? (For storage)
    public byte mstart_flag = 0; // A flag that is set to 1 when starting mstart
    public byte[] mus_filename = new byte[13]; // Song FILE name buffer
    public byte mmldat_lng = 0; // Song data buffer size (KB)
    public byte voicedat_lng = 0; // Tone data buffer size (KB)
    public byte effecdat_lng = 0; // Sound effect data buffer size (KB)
    public final int[] rshot = {0, 0, 0, 0, 0, 0}; // Rhythm Sound Source shot inc flags
    //public byte rshot_bd = 0; // Rhythm Sound Source shot inc flag(BD)
    //public byte rshot_sd = 0; // Rhythm Sound Source shot inc flag(SD)
    //public byte rshot_sym = 0; // Rhythm Sound Source shot inc flag(CYM)
    //public byte rshot_hh = 0; // Rhythm Sound Source shot inc flag(HH)
    //public byte rshot_tom = 0; // Rhythm Sound Source shot inc flag(TOM)
    //public byte rshot_rim = 0; // Rhythm Sound Source shot inc flag(RIM)
    public final int[] rdump = {0, 0, 0, 0, 0, 0}; // Rhythm Sound Source dump inc flags
    //public byte rdump_bd = 0; // Rhythm Sound Source dump inc flag(BD)
    //public byte rdump_sd = 0; // Rhythm Sound Source dump inc flag(SD)
    //public byte rdump_sym = 0; // Rhythm Sound Source dump inc flag(CYM)
    //public byte rdump_hh = 0; // Rhythm Sound Source dump inc flag(HH)
    //public byte rdump_tom = 0; // Rhythm Sound Source dump inc flag(TOM)
    //public byte rdump_rim = 0; // Rhythm Sound Source dump inc flag(RIM)
    public byte ch3mode = 0; // ch3 Mode
    public byte ch3mode_push = 0; // ch3 Mode(Push area for sound effects)
    public byte ppz_voldown = 0; // PPZ8 voldown value
    public byte _ppz_voldown = 0; // PPZ8 voldown Number (for storage)
    public int ppz_call_ofs = 0; // For PPZ8call far call address
    public int ppz_call_seg = 0; // The seg value also serves as a PPZ8 resident check, 0 for non-resident
    public byte p86_freq = 8; // PMD86 PCM playback frequency
//#if pcm* board2
    public int p86_freqtable = 0; // offset pcm_tune_data
//#else
//    public int p86_freqtable = 0; // PMD86 PCM playback frequency table
//#endif
    public byte adpcm_emulate = 0; // Are you emulating ADPCM with PMDPPZE?


    public MmlDatum[] rd = null;
    public final MmlDatum[] rdDmy = new MmlDatum[] {new MmlDatum(0xff)};

    // Playing Data Area

    public static class partWork { // qq  struc

        public short address; // w? ; 2 Address currently playing
        public short partloop; // w? ; 2 Destination to return to when playback ends
        public byte leng; // b? ; 1 Remaining LENGTH
        public byte qdat; // b? ; 1 gatetime(Calculated q/Q value)
        public short fnum; // w? ; 2 BLOCK / FNUM when playing
        public short detune; // w? ; 2 Detune
        // +10
        public short lfodat; // w? ; 2 LFO DATA
        public short porta_num; // w? ; 2 Portamento Adjustment Value (Overall)
        public short porta_num2; // w? ; 2 Portamento Adjustment (Single)
        public short porta_num3; // w? ; 2 Portamento adjustment value (remainder)
        public byte volume; // b? ; 1 VOLUME
        public byte shift; // b? ; 1 Scale shift t value
        // +20
        public byte delay; // b? ; 1 LFO[DELAY]
        public byte speed; // b? ; 1 [SPEED]
        public byte step; // b? ; 1 [STEP]
        public byte time; // b? ; 1 [TIME]
        public byte delay2; // b? ; 1 [DELAY_2]
        public byte speed2; // b? ; 1 [SPEED_2]
        public byte step2; // b? ; 1 [STEP_2]
        public byte time2; // b? ; 1 [TIME_2]
        public byte lfoswi; // b? ; 1 LFOSW.B0/tone B1/vol B2/Synchronization B3/porta
        //    ;          B4/tone B5/vol B6/Synchronization
        public byte volpush; // b? ; 1 Volume PUSHarea
        // +30
        public byte mdepth; // b? ; 1 M depth
        public byte mdspd; // b? ; 1 M speed
        public byte mdspd2; // b? ; 1 M speed_2
        public byte envf; // b? ; 1 PSG ENV. [START_FLAG] / -1: extend
        public byte eenv_count; // b? ; 1 ExtendPSGenv/No=0 AR=1 DR=2 SR=3 RR=4
        public byte eenv_ar; // b? ; 1 /AR /old pat
        public byte eenv_dr; // b? ; 1 /DR /old pv2
        public byte eenv_sr; // b? ; 1 /SR /old pr1
        public byte eenv_rr; // b? ; 1 /RR /old pr2
        public byte eenv_sl; // b? ; 1 /SL
        // +40
        public byte eenv_al; // b? ; 1 /AL
        public byte eenv_arc; // b? ; 1 /AR counter /Former patb
        public byte eenv_drc; // b? ; 1 /DR counter
        public byte eenv_src; // b? ; 1 /SR counter /Former pr1b
        public byte eenv_rrc; // b? ; 1 /RR counter /Former pr2b
        public byte eenv_volume; // b? ; 1 /Volume value(0-15)/Former penv
        public byte extendmode; // b? ; 1 B1/Detune B2/LFO B3/Env Normal/Extend
        public byte fmpan; // b? ; 1 FM Panning + AMD + PMD
        public byte psgpat; // b? ; 1 PSG PATTERN[TONE / NOISE / MIX]
        public byte voicenum; // b? ; 1 Tone Number
        // +50
        public byte loopcheck; // b? ; 1 1 if looped, 3 if finished
        public byte carrier; // b? ; 1 FM Carrier
        public byte slot1; // b? ; 1 SLOT 1 TL
        public byte slot3; // b? ; 1 SLOT 3 TL
        public byte slot2; // b? ; 1 SLOT 2 TL
        public byte slot4; // b? ; 1 SLOT 4 TL
        public byte slotmask; // b? ; 1 FM slotmask
        public byte neiromask; // b? ; 1 FM Mask data for tone definition
        public byte lfo_wave; // b? ; 1 LFO Waveform
        public byte partmask; // b 1 PartMask b0:Normal b1:Sound effect b2:For NECPCM
        //          ;   b3:none b4:For PPZ/ADE b5:at s0 b6:m b7:temporary
        // +60
        public byte keyoff_flag; // b? ; 1 Flag indicating whether keyoff has occurred
        public byte volmask; // b? ;1 Volume LFO Mask
        public byte qdata; // b? ; 1 q value
        public byte qdatb; // b? ; 1 Q value
        public byte hldelay; // b? ; 1 HardLFO delay
        public byte hldelay_c; // b? ; 1 HardLFO delay Counter
        public short _lfodat; // w? ; 2 LFO DATA
        public byte _delay; // b? ; 1 LFO[DELAY]
        public byte _speed; // b? ; 1 [SPEED]
        public byte _step; // b? ; 1 [STEP]
        public byte _time; // b? ; 1 [TIME]
        public byte _delay2; // b? ; 1 [DELAY_2]
        public byte _speed2; // b? ; 1 [SPEED_2]
        public byte _step2; // b? ; 1 [STEP_2]
        public byte _time2; // b? ; 1 [TIME_2]
        public byte _mdepth; // b? ; 1 M depth
        public byte _mdspd; // b? ; 1 M speed
        public byte _mdspd2; // b? ; 1 M speed_2
        public byte _lfo_wave; // b? ; 1 LFO Waveform
        public byte _volmask; // b? ; 1 Volume LFO Mask
        public byte mdc; // b? ; 1 M depth Counter(Variation)
        public byte mdc2; // b? ; 1 M depth Counter
        public byte _mdc; // b? ; 1 M depth Counter(Variation)
        public byte _mdc2; // b? ; 1 M depth Counter
        public byte onkai; // b 1 Scale data being played(0ffh:rest)
        public byte sdelay; // b?; 1 Slot delay
        public byte sdelay_c; // b? ; 1 Slot delay counter
        public byte sdelay_m; // b? ; 1 Slot delay Mask
        public byte alg_fb; // b? ; 1 Tone alg/fb
        public byte keyon_flag; // b 1 After processing the new scale/rest data, inc
        public byte qdat2; // b? ; 1 q Minimum guaranteed value
        public short fnum2; // w? ; 2 Upper fnum value for ppz8/pmd86
        public byte onkai_def; // b 1 Scale data being played(Before Transposition / ?fh:rest)
        public byte shift_def; // b? ; 1 Master Transposition Value
        public byte qdat3; // b? ; 1 q Random

        public int loopCounter;

        public void clear() {
            address = 0; // w? ; 2 Address currently playing
            partloop = 0; // w? ; 2 Destination to return to when playback ends
            leng = 0; // b? ; 1 Remaining LENGTH
            qdat = 0; // b? ; 1 gatetime(Calculated q/Q value)
            fnum = 0; // w? ; 2 BLOCK/FNUM when playing
            detune = 0; // w? ; 2 Detune
            lfodat = 0; // w? ; 2 LFO DATA
            porta_num = 0; // w? ; 2 Portamento Adjustment Value (Overall)
            porta_num2 = 0; // w? ; 2 Portamento Adjustment (single)
            porta_num3 = 0; // w? ; 2 Portamento adjustment value (remainder)
            volume = 0; // b? ; 1 VOLUME
            shift = 0; // b? ; 1 Scale shift value
            delay = 0; // b? ; 1 LFO[DELAY]
            speed = 0; // b? ; 1 [SPEED]
            step = 0; // b? ; 1 [STEP]
            time = 0; // b? ; 1 [TIME]
            delay2 = 0; // b? ; 1 [DELAY_2]
            speed2 = 0; // b? ; 1 [SPEED_2]
            step2 = 0; // b? ; 1 [STEP_2]
            time2 = 0; // b? ; 1 [TIME_2]
            lfoswi = 0; // b? ; 1 LFOSW.B0/tone B1/vol B2/Synchronization B3/porta
            volpush = 0; // b? ; 1 Volume PUSHarea
            mdepth = 0; // b? ; 1 M depth
            mdspd = 0; // b? ; 1 M speed
            mdspd2 = 0; // b? ; 1 M speed_2
            envf = 0; // b? ; 1 PSG ENV. [START_FLAG] / Extend with -1
            eenv_count = 0; // b? ; 1 ExtendPSGenv/No=0 AR=1 DR=2 SR=3 RR=4
            eenv_ar = 0; // b? ; 1 /AR /Former pat
            eenv_dr = 0; // b? ; 1 /DR /Former pv2
            eenv_sr = 0; // b? ; 1 /SR /Former pr1
            eenv_rr = 0; // b? ; 1 /RR /Former pr2
            eenv_sl = 0; // b? ; 1 /SL
            eenv_al = 0; // b? ; 1 /AL
            eenv_arc = 0; // b? ; 1 /AR counter /Former Patb
            eenv_drc = 0; // b? ; 1 /DR counter
            eenv_src = 0; // b? ; 1 /SR counter /Former pr1b
            eenv_rrc = 0; // b? ; 1 /RR counter /Former pr2b
            eenv_volume = 0; // b? ; 1 /Volume value(0 to 15)/Former penv
            extendmode = 0; // b? ; 1 B1/Detune B2/LFO B3/Env Normal/Extend
            fmpan = 0; // b? ; 1 FM Panning + AMD + PMD
            psgpat = 0; // b? ; 1 PSG PATTERN[TONE / NOISE / MIX]
            voicenum = 0; // b? ; 1 Tone Number
            loopcheck = 0; // b? ; 1 1 if looped, 3 if finished
            carrier = 0; // b? ; 1 FM Carrier
            slot1 = 0; // b? ; 1 SLOT 1 TL
            slot3 = 0; // b? ; 1 SLOT 3 TL
            slot2 = 0; // b? ; 1 SLOT 2 TL
            slot4 = 0; // b? ; 1 SLOT 4 TL
            slotmask = 0; // b? ; 1 FM slotmask
            neiromask = 0; // b? ; 1 FM tone definition maskdata
            lfo_wave = 0; // b? ; 1 LFO Waveform
            partmask = 0; // b 1 PartMask b0: Normal b1: Sound effect b2: For NECPCM
            keyoff_flag = 0; // b? ; 1 Flag indicating whether keyoff has occurred
            volmask = 0; // b? ; 1 Volume LFO Mask
            qdata = 0; // b? ; 1 q value
            qdatb = 0; // b? ; 1 Q value
            hldelay = 0; // b? ; 1 HardLFO delay
            hldelay_c = 0; // b? ; 1 HardLFO delay Counter
            _lfodat = 0; // w? ; 2 LFO DATA
            _delay = 0; // b? ; 1 LFO[DELAY]
            _speed = 0; // b? ; 1 [SPEED]
            _step = 0; // b? ; 1 [STEP]
            _time = 0; // b? ; 1 [TIME]
            _delay2 = 0; // b? ; 1 [DELAY_2]
            _speed2 = 0; // b? ; 1 [SPEED_2]
            _step2 = 0; // b? ; 1 [STEP_2]
            _time2 = 0; // b? ; 1 [TIME_2]
            _mdepth = 0; // b? ; 1 M depth
            _mdspd = 0; // b? ; 1 M speed
            _mdspd2 = 0; // b? ; 1 M speed_2
            _lfo_wave = 0; // b? ; 1 LFO Waveform
            _volmask = 0; // b? ; 1 Volume LFO Mask
            mdc = 0; // b? ; 1 M depth Counter(Variation)
            mdc2 = 0; // b? ; 1 M depth Counter
            _mdc = 0; // b? ; 1 M depth Counter(Variation)
            _mdc2 = 0; // b? ; 1 M depth Counter
            onkai = 0; // b 1 Scale data being played(0ffh:rest)
            sdelay = 0; // b?; 1 Slot delay
            sdelay_c = 0; // b? ; 1 Slot delay counter
            sdelay_m = 0; // b? ; 1 Slot delay Mask
            alg_fb = 0; // b? ; 1 Timbre alg/fb
            keyon_flag = 0; // b 1 After processing the new scale/rest data, inc
            qdat2 = 0; // b? ; 1 q Minimum guaranteed value
            fnum2 = 0; // w? ; 2 Upper fnum value for ppz8/pmd86
            onkai_def = 0; // b 1 Scale data being played(Before Transposition / ?fh:rest)
            shift_def = 0; // b? ; 1 Master Transposition Value
            qdat3 = 0; // b? ; 1 q Random

            loopCounter = 0;
        }
    }

    //qqq struc
    //     db  offset eenv_ar dup(?)
    //pat  db ? ; 1 Former SSGENV /Normal pat
    //pv2  db ? ; 1  /Normal pv2
    //pr1  db ? ; 1  /Normal pr1
    //pr2  db ? ; 1  /Normal pr2
    //     db?
    //     db ?
    //patb db ? ; 1  /Normal patb
    //     db?
    //pr1b db?     ; 1  /Normal pr1b
    //pr2b db ? ; 1  /Normal pr2b
    //penv db ? ; 1  /Normal penv
    //qqq ends

    public int max_part1; // 0 Number of parts to be cleared
    public int max_part2; // Number of parts to be initialized

    //fm     equ 0
    //fm2    equ 1
    //psg    equ 2
    //rhythm equ 3

    //public short open_work; // dw

    public int[] part_data_table;

    // FM1-3
    public int part1;
    public int part2;
    public int part3;

    // FM4-6
    public int part4;
    public int part5;
    public int part6;

    // Sound Effects Mode
    public int part3b;
    public int part3c;
    public int part3d;

    // pps?
    public int part7;
    public int part8;
    public int part9;
    public int part10;
    public int part11;

    // ppz
    public int part10a;
    public int part10b;
    public int part10c;
    public int part10d;
    public int part10e;
    public int part10f;
    public int part10g;
    public int part10h;

    // Sound effects
    public int part_e;

    public partWork[] partWk;
    // normal
    // 1,2,3, 3b,3c,3d, 7,8,9,10,11, e
    // board2
    // 1,2,3,4,5,6, 7,8,9,10,11, 3b,3c,3d, e
    // ppz(ppz also serves as board2)
    // 1,2,3,4,5,6, 7,8,9,10,11, 3b,3c,3d, 10a,10b,10c,10d,10e,10f,10g,10h ,e

    //    even
    //pcm_table   label word
//#if board2
//# if adpcm
//#  ife   ademu
    public static final short pcmends = 0x26; // The first start is from 26H
    public static final short[] pcmadrs = new short[2 * 256];
    public static final byte[] pcmfilename = new byte[128];
//#  endif
//# endif
//# if pcm
    public short pcmst_ofs = 0;
    public short pcmst_seg = 0;
    public static final byte[] pcmadrs_86 = new byte[6 * 256];
//# endif
//#endif

    //  db "This is the STACK area.  "
    //  db " Thank you to everyone who always uses PMD (^^)."
    //  db "If you find anything that seems like a bug,"
    //  db "even if it's a small thing, please let me know (^^)."
    //  db "→PMDBBS [xx(xxxx)xx"
    //  db "xx] to @PMD board     by KAJA."

    //_stack:
    //dataarea label   word
    //   db 0  ;
    //   dw 12 dup(18h); Initial Data
    //   db 80h  ;

    public static final byte[] fmoff_nef = {0, 1, 2, 4, 5, 6, 8, 9, 10, 12, 13, 14, (byte) 0xff};
    public static final byte[] fmoff_ef = {0, 1, 4, 5, 8, 9, 12, 13, (byte) 0xff};

    public static final String mes_title = "Music Driver P.M.D. for PC9801/88VA Version " + ver + "\r\n" +
            "Copyright (C)1989," + date + " by M.Kajihara(KAJA).\r\n\r\n";

    public static final String mes_ppsdrv = "Corresponds to PPSDRV(INT64H).\r\n";
    public static final String mes_ppz8 = "Compatible with PPZ8(INT7FH).\r\n";

    public byte port_sel; // b? ; Selected Port
    public byte opn_0eh; // b?
    public byte message_flag;    // b?
    public short opt_sp_push; // w?
    public short resident_size;  // w?

    // EFCDRV.ASM
    public short effadr; // w effect address
    public short eswthz; // w Tone Sweep Frequency
    public short eswtst; // w Tone Sweep Increment
    public byte effcnt; // b effect count
    public byte eswnhz; // b Noise Sweep Frequency
    public byte eswnst; // b Noise Sweep Increment
    public byte eswnct; // b Noise Sweep Count
    public byte effon; // b Sound effect being played
    public byte psgefcnum; // b Sound effect number
    public byte hosei_flag; // b ppsdrv Whether to correct volume/pitch
    public byte last_shot_data; // b The last PPSDRV tone

    // PCMDRV86.ASM

    /**
     * Datas
     */
    public static final int trans_size = 256;// Number of bytes transferred at one time
    public byte play86_flag; // Pronounced? flag
    public byte trans_flag; // db 0 ; Is there any data left to transfer? Flag
    public short start_ofs; // dw 0 ; PCM data address during sounding (lower offset)
    public short start_ofs2; // dw 0 ; PCM data address during sounding (offset upper)
    public short size1; // dw 0 ; Remaining size (lower word)
    public short size2; // dw 0 ; Remaining size (upper word)
    public short _start_ofs; // dw 0 ; Sound start PCM data address (offset lower)
    public short _start_ofs2; // dw 0 ; Sound start PCM data address (offset upper)
    public short _size1; // dw 0 ; PCM data size (lower word)
    public short _size2; // dw 0 ; PCM data size (upper word)
    public byte addsize1; // db 0 ; PCM address addition value (integer part)
    public short addsize2; // dw 0 ; PCM address addition value (decimal part)
    public short addsizew; // dw 0 ; PCM address addition value (decimal part, work being transferred)
    public short repeat_ofs; // dw 0 ; Repeat start position (offset lower)
    public short repeat_ofs2; // dw 0 ; Repeat start position (upper offset)
    public short repeat_size1; // dw 0 ; Size after repeat (lower word)
    public short repeat_size2; // dw 0 ; Size after repeat (upper word)
    public short release_ofs; // dw 0 ; Release start position (offset lower)
    public short release_ofs2; // dw 0 ; Release start position (upper offset)
    public short release_size1; // dw 0 ; Size after release (lower word)
    public short release_size2; // dw 0 ; Size after release (upper word)
    public byte repeat_flag; // db 0 ; Repeat flag
    public byte release_flag1; //   db 0 ; Flag to release or not
    public byte release_flag2; //   db 0 ; Flag of whether it has been released
    public byte pcm86_pan_flag = 0; // b 0 ; Pan data 1 (bit0 = left / bit1 = right / bit2 = reverse)
    public static final byte com_end = (byte) 0xb1;
    public byte pcm86_pan_dat; // db 0 ; Pan data 2 (volume value of the side that lowers the volume)

    // Forwarding table by pan_flag
    //trans_table dw double_trans, left_trans
    //        dw right_trans, double_trans
    //        dw double_trans_g, left_trans_g
    //        dw right_trans_g, double_trans_g

    // Frequency table Include

    //    include tunedata.inc
    /**
     * Frequency table 16.54kHz = o5g
     */
    //fq macro   data1,data2
    //   db  data1
    //   dw  data2
    //   endm

    public static final Tuple<Integer, Integer>[] pcm_tune_data86 = new Tuple[] {
            // Frequency*32 + additional value (integer part), additional value (decimal part)
            new Tuple<>(0 * 32 + 0, 0x02AB7),  // o1  4.13438 C
            new Tuple<>(0 * 32 + 0, 0x02D41),  // o1  4.13438 C#
            new Tuple<>(0 * 32 + 0, 0x02FF2),  // o1  4.13438 D
            new Tuple<>(0 * 32 + 0, 0x032CB),  // o1  4.13438 D#
            new Tuple<>(0 * 32 + 0, 0x035D1),  // o1  4.13438 E
            new Tuple<>(0 * 32 + 0, 0x03904),  // o1  4.13438 F
            new Tuple<>(0 * 32 + 0, 0x03C68),  // o1  4.13438 F#
            new Tuple<>(0 * 32 + 0, 0x03FFF),  // o1  4.13438 G
            new Tuple<>(0 * 32 + 0, 0x043CE),  // o1  4.13438 G#
            new Tuple<>(0 * 32 + 0, 0x047D6),  // o1  4.13438 A
            new Tuple<>(0 * 32 + 0, 0x04C1B),  // o1  4.13438 A#
            new Tuple<>(0 * 32 + 0, 0x050A2),  // o1  4.13438 B

            new Tuple<>(0 * 32 + 0, 0x0556E),  // o2  4.13438 C
            new Tuple<>(0 * 32 + 0, 0x05A82),  // o2  4.13438 C#
            new Tuple<>(0 * 32 + 0, 0x05FE4),  // o2  4.13438 D
            new Tuple<>(0 * 32 + 0, 0x06597),  // o2  4.13438 D#
            new Tuple<>(0 * 32 + 0, 0x06BA2),  // o2  4.13438 E
            new Tuple<>(0 * 32 + 0, 0x07209),  // o2  4.13438 F
            new Tuple<>(0 * 32 + 0, 0x078D0),  // o2  4.13438 F#
            new Tuple<>(0 * 32 + 0, 0x07FFF),  // o2  4.13438 G
            new Tuple<>(0 * 32 + 0, 0x0879C),  // o2  4.13438 G#
            new Tuple<>(0 * 32 + 0, 0x08FAC),  // o2  4.13438 A
            new Tuple<>(0 * 32 + 0, 0x09837),  // o2  4.13438 A#
            new Tuple<>(0 * 32 + 0, 0x0A145),  // o2  4.13438 B

            new Tuple<>(0 * 32 + 0, 0x0AADC),  // o3  4.13438 C
            new Tuple<>(0 * 32 + 0, 0x0B504),  // o3  4.13438 C#
            new Tuple<>(0 * 32 + 0, 0x0BFC8),  // o3  4.13438 D
            new Tuple<>(0 * 32 + 0, 0x0CB2F),  // o3  4.13438 D#
            new Tuple<>(0 * 32 + 0, 0x0D744),  // o3  4.13438 E
            new Tuple<>(0 * 32 + 0, 0x0E412),  // o3  4.13438 F
            new Tuple<>(0 * 32 + 0, 0x0F1A1),  // o3  4.13438 F#
            new Tuple<>(0 * 32 + 1, 0x00000),  // o3  4.13438 G
            new Tuple<>(1 * 32 + 0, 0x0CB6B),  // o3  5.51250 G#
            new Tuple<>(1 * 32 + 0, 0x0D783),  // o3  5.51250 A
            new Tuple<>(1 * 32 + 0, 0x0E454),  // o3  5.51250 A#
            new Tuple<>(1 * 32 + 0, 0x0F1E7),  // o3  5.51250 B

            new Tuple<>(2 * 32 + 0, 0x0AADC),  // o4  8.26875 C
            new Tuple<>(2 * 32 + 0, 0x0B504),  // o4  8.26875 C#
            new Tuple<>(2 * 32 + 0, 0x0BFC8),  // o4  8.26875 D
            new Tuple<>(2 * 32 + 0, 0x0CB2F),  // o4  8.26875 D#
            new Tuple<>(2 * 32 + 0, 0x0D744),  // o4  8.26875 E
            new Tuple<>(2 * 32 + 0, 0x0E412),  // o4  8.26875 F
            new Tuple<>(2 * 32 + 0, 0x0F1A1),  // o4  8.26875 F#
            new Tuple<>(2 * 32 + 1, 0x00000),  // o4  8.26875 G
            new Tuple<>(3 * 32 + 0, 0x0CB6B),  // o4 11.02500 G#
            new Tuple<>(3 * 32 + 0, 0x0D783),  // o4 11.02500 A
            new Tuple<>(3 * 32 + 0, 0x0E454),  // o4 11.02500 A#
            new Tuple<>(3 * 32 + 0, 0x0F1E7),  // o4 11.02500 B

            new Tuple<>(4 * 32 + 0, 0x0AADC),  // o5 16.53750 C
            new Tuple<>(4 * 32 + 0, 0x0B504),  // o5 16.53750 C#
            new Tuple<>(4 * 32 + 0, 0x0BFC8),  // o5 16.53750 D
            new Tuple<>(4 * 32 + 0, 0x0CB2F),  // o5 16.53750 D#
            new Tuple<>(4 * 32 + 0, 0x0D744),  // o5 16.53750 E
            new Tuple<>(4 * 32 + 0, 0x0E412),  // o5 16.53750 F
            new Tuple<>(4 * 32 + 0, 0x0F1A1),  // o5 16.53750 F#
            new Tuple<>(4 * 32 + 1, 0x00000),  // o5 16.53750 G
            new Tuple<>(5 * 32 + 0, 0x0CB6B),  // o5 22.05000 G#
            new Tuple<>(5 * 32 + 0, 0x0D783),  // o5 22.05000 A
            new Tuple<>(5 * 32 + 0, 0x0E454),  // o5 22.05000 A#
            new Tuple<>(5 * 32 + 0, 0x0F1E7),  // o5 22.05000 B

            new Tuple<>(6 * 32 + 0, 0x0AADC),  // o6 33.07500 C
            new Tuple<>(6 * 32 + 0, 0x0B504),  // o6 33.07500 C#
            new Tuple<>(6 * 32 + 0, 0x0BFC8),  // o6 33.07500 D
            new Tuple<>(6 * 32 + 0, 0x0CB2F),  // o6 33.07500 D#
            new Tuple<>(6 * 32 + 0, 0x0D744),  // o6 33.07500 E
            new Tuple<>(6 * 32 + 0, 0x0E412),  // o6 33.07500 F
            new Tuple<>(6 * 32 + 0, 0x0F1A1),  // o6 33.07500 F#
            new Tuple<>(6 * 32 + 1, 0x00000),  // o6 33.07500 G
            new Tuple<>(7 * 32 + 0, 0x0CB6B),  // o6 44.10000 G#
            new Tuple<>(7 * 32 + 0, 0x0D783),  // o6 44.10000 A
            new Tuple<>(7 * 32 + 0, 0x0E454),  // o6 44.10000 A#
            new Tuple<>(7 * 32 + 0, 0x0F1E7),  // o6 44.10000 B

            new Tuple<>(7 * 32 + 1, 0x0004A),  // o7 44.10000 C
            new Tuple<>(7 * 32 + 1, 0x00F87),  // o7 44.10000 C#
            new Tuple<>(7 * 32 + 1, 0x01FAC),  // o7 44.10000 D
            new Tuple<>(7 * 32 + 1, 0x030C7),  // o7 44.10000 D#
            new Tuple<>(7 * 32 + 1, 0x042E7),  // o7 44.10000 E
            new Tuple<>(7 * 32 + 1, 0x0561C),  // o7 44.10000 F
            new Tuple<>(7 * 32 + 1, 0x06A72),  // o7 44.10000 F#
            new Tuple<>(7 * 32 + 1, 0x08000),  // o7 44.10000 G
            new Tuple<>(7 * 32 + 1, 0x096D6),  // o7 44.10000 G#
            new Tuple<>(7 * 32 + 1, 0x0AF06),  // o7 44.10000 A
            new Tuple<>(7 * 32 + 1, 0x0C8A8),  // o7 44.10000 A#
            new Tuple<>(7 * 32 + 1, 0x0E3CF),  // o7 44.10000 B

            new Tuple<>(7 * 32 + 2, 0x00094),  // o8 44.10000 C
            new Tuple<>(7 * 32 + 2, 0x01F0E),  // o8 44.10000 C#
            new Tuple<>(7 * 32 + 2, 0x03F59),  // o8 44.10000 D
            new Tuple<>(7 * 32 + 2, 0x0618F),  // o8 44.10000 D#
            new Tuple<>(7 * 32 + 2, 0x085CE),  // o8 44.10000 E
            new Tuple<>(7 * 32 + 2, 0x0AC38),  // o8 44.10000 F
            new Tuple<>(7 * 32 + 2, 0x0D4E5),  // o8 44.10000 F#
            new Tuple<>(7 * 32 + 3, 0x00000),  // o8 44.10000 G
            new Tuple<>(7 * 32 + 3, 0x02DAC),  // o8 44.10000 G#
            new Tuple<>(7 * 32 + 3, 0x05E0D),  // o8 44.10000 A
            new Tuple<>(7 * 32 + 3, 0x09150),  // o8 44.10000 A#
            new Tuple<>(7 * 32 + 3, 0x0C79E)   // o8 44.10000 B
    };

    /**
     * Scale DATA
     */
    public static final int[] fnum_data = {
            0x026a,  // C
            0x028f,  // D-
            0x02b6,  // D
            0x02df,  // E-
            0x030b,  // E
            0x0339,  // F
            0x036a,  // G-
            0x039e,  // G
            0x03d5,  // A-
            0x0410,  // A
            0x044e,  // B-
            0x048f   // B
    };

    public static final int[] psg_tune_data = {
            0x0ee8,  // C
            0x0e12,  // D-
            0x0d48,  // D
            0x0c89,  // E-
            0x0bd5,  // E
            0x0b2b,  // F
            0x0a8a,  // G-
            0x09f3,  // G
            0x0964,  // A-
            0x08dd,  // A
            0x085e,  // B-
            0x07e6   // B
    };

    public int[] part_table = null;
    //if board2
    // if ppz
    //   Part number, Partb, sound source number
    private static final int[] part_table_ppz = {
            0, 1, 0,  // A
            1, 2, 0,  // B
            2, 3, 0,  // C
            3, 1, 1,  // D
            4, 2, 1,  // E
            5, 3, 1,  // F
            6, 1, 2,  // G
            7, 2, 2,  // H
            8, 3, 2,  // I
            9, 1, 3,  // J
            10, 3, 4,  // K
            11, 3, 0,  // c2
            12, 3, 0,  // c3
            13, 3, 0,  // c4
            0xff, 0, 0xff,  // Rhythm
            22, 3, 1,  // Effect
            14, 0, 5,  // PPZ1
            15, 1, 5,  // PPZ2
            16, 2, 5,  // PPZ3
            17, 3, 5,  // PPZ4
            18, 4, 5,  // PPZ5
            19, 5, 5,  // PPZ6
            20, 6, 5,  // PPZ7
            21, 7, 5  // PPZ8
    };
    // else
    //   Part number, Partb, sound source number
    private static final int[] part_table_brd2 = {
            0, 1, 0,  // A
            1, 2, 0,  // B
            2, 3, 0,  // C
            3, 1, 1,  // D
            4, 2, 1,  // E
            5, 3, 1,  // F
            6, 1, 2,  // G
            7, 2, 2,  // H
            8, 3, 2,  // I
            9, 1, 3,  // J
            10, 3, 4,  // K
            11, 3, 0,  // c2
            12, 3, 0,  // c3
            13, 3, 0,  // c4
            0xff, 0, 0xff,  // Rhythm
            14, 3, 1  // Effect
    };
    //else
    // Part number, Partb, sound source number
    private static final int[] part_table_nbrd2 = {
            0, 1, 0,  // A
            1, 2, 0,  // B
            2, 3, 0,  // C
            3, 3, 0,  // c2
            4, 3, 0,  // c3
            5, 3, 0,  // c4
            6, 1, 2,  // G
            7, 2, 2,  // H
            8, 3, 2,  // I
            9, 1, 3,  // J
            10, 3, 4,  // K
            3, 3, 0,  // c2
            4, 3, 0,  // c3
            5, 3, 0,  // c4
            0xff, 0, 0xff,  // Rhythm
            11, 3, 0  // Effect
    };

    /**
     * FM tone carrier table
     */
    public static final int[] carrier_table = {
            0b1000_0000, 0b1000_0000, 0b1000_0000, 0b1000_0000,
            0b1010_0000, 0b1110_0000, 0b1110_0000, 0b1111_0000,
            0b1110_1110, 0b1110_1110, 0b1110_1110, 0b1110_1110,
            0b1100_1100, 0b1000_1000, 0b1000_1000, 0b0000_0000
    };

    //
    /**
     * Sound effect data INCLUDE
     */
    //public byte[] efftbl; // label   word
    //include effect.inc
    public byte efftblend; //   label word

    //PCMLOAD.INC
    //15
    public int message = 1; // equ ;Whether to display error messages

    /**
     * DataArea
     */
//#if message
    public static final String allload_mes = "PCM is being defined. Please wait.";
    public static final String exit1_mes = "PCM cannot be defined in this environment.";
    public static final String exit1p_mes = "PPSDRV is not resident.";
    public static final String exit2_mes = "PCMFile not found.";
    public static final String exit2p_mes = "PPSFile not found.";
    public static final String exit3_mes = "PCMFile FORMAT is different.";
    public static final String exit3p_mes = "PPSDRV has insufficient capacity.";
    public static final String exit4_mes = "PCMData matches, so will not load.";
    public static final String exit4pp_mes = "P86DRV has insufficient capacity.";
    public static final String exit5_mes = "PCMFile cannot be loaded.";
    public static final String exit5p_mes = "Cannot read PPSFile.";
    public static final String exit6_mes = "PCM memory is being accessed by another application.";
    public static final String exit1z_mes = "PCMFile not found.";
    public static final String exit2z_mes = "PCMFile data format is different.";
    public static final String exit3z_mes = "Insufficient memory allocation.";
    public static final String exit4z_mes = "EMS handle cannot be mapped.";
    public static final String exit5z_mes = "PPZ8 is not resident.";
    public static final String exit6z_mes = "PVI/PZIFile not found."; // KUMA: Added
    public static final String ppzbank_mes = "PPZ8(%d):";
//#endif
    public static final String adpcm_header = "ADPCM DATA for  PMD ver.4.4-  "; // ;30 bytes
    public static final String pps_ext = "PPS";
    public static final String ppc_ext = "PPC";
    public static final String p86_ext = "P86";
    public static final String pvi_ext = "PVI";
    public static final String pzi_ext = "PZI";

    public byte retry_flag = 0;
    public byte key_check_push = 0;
    public short pcmload_wait_clock = 0;
    public byte pcmload_adpcm_wait = 0;
    public short mmask_port = 0;
    public byte mmask_push = 0;

    public byte[] filename_buf = new byte[128];

    public String filename_ofs; // dw ?
    public short filename_seg; // dw ?
    public String filename_ofs2; // dw ?
    public short filename_seg2; // dw ?
    public short pcmdata_ofs; // dw ?
    public short pcmdata_seg; // dw ?
    public byte pcmdata_size_s; // db ?
    public short pcmdata_size; // dw ?
    public short pcmwork_ofs; // dw ?
    public short pcmwork_seg; // dw ?
    public short port46; // dw ?
    public short port47; // dw ?
    public short pcmload_pcmstop; // dw ?
    public short pcmload_pcmstart; // dw ?
    public short fhand2; // dw ?
    public byte ppz_bank; // db ?

    /**
     * Datas
     */
    public static final int[] pcm_tune_data = {
            0x3132 * 2,  // C
            0x3420 * 2,  // C+
            0x373a * 2,  // D
            0x3a83 * 2,  // D+
            0x3dfe * 2,  // E
            0x41af * 2,  // F
            0x4597 * 2,  // F+
            0x49bb * 2,  // G
            0x4e1e * 2,  // G+
            0x52c4 * 2,  // A
            0x57b1 * 2,  // A+
            0x5ce8 * 2   // B
    };

    /**
     * Datas
     */
    public static final int[] ppzpandata = {0, 9, 1, 5};

    public static final int[] ppz_tune_data = { // label   word
            0x08000,  // 00 c
            0x087a6,  // 01 d-
            0x08fb3,  // 02 d
            0x09838,  // 03 e-
            0x0a146,  // 04 e
            0x0aade,  // 05 f
            0x0b4ff,  // 06 g-
            0x0bfcc,  // 07 g
            0x0cb34,  // 08 a-
            0x0d747,  // 09 a
            0x0e418,  // 10 b-
            0x0f1a5   // 11 b
    };

    // pmdDotNET original
    public int jumpIndex = -1;
    public boolean checkJumpIndexSI = false;
    public boolean checkJumpIndexBX = false;

    public PW() {
        efftbl = new ArrayList<>();
        MmlDatum[] ef;

//#region Sound effect data definition
        ef = makeMmlDatum(D_000);
        efftbl.add(new Tuple<>(1, ef)); //BDRM      ;0
        ef = makeMmlDatum(D_001);
        efftbl.add(new Tuple<>(1, ef)); //SIMONDS     ;1
        ef = makeMmlDatum(D_002);
        efftbl.add(new Tuple<>(1, ef)); //SIMONDSTAML ;2
        ef = makeMmlDatum(D_003);
        efftbl.add(new Tuple<>(1, ef)); //SIMONDSTAMM ;3
        ef = makeMmlDatum(D_004);
        efftbl.add(new Tuple<>(1, ef)); //SIMONDSTAMH ;4
        ef = makeMmlDatum(D_005);
        efftbl.add(new Tuple<>(1, ef)); //RIMSHOTT     ;5
        ef = makeMmlDatum(D_006);
        efftbl.add(new Tuple<>(1, ef)); //CPSIMONDSSD2 ;6
        ef = makeMmlDatum(D_007);
        efftbl.add(new Tuple<>(1, ef)); //CLOSEHT     ;7
        ef = makeMmlDatum(D_008);
        efftbl.add(new Tuple<>(1, ef)); //OPENHT      ;8
        ef = makeMmlDatum(D_009);
        efftbl.add(new Tuple<>(1, ef)); //CRUSHCYMBA     ;9
        ef = makeMmlDatum(D_010);
        efftbl.add(new Tuple<>(1, ef)); //RDCYN      ;10

        ef = makeMmlDatum(DM_001);
        efftbl.add(new Tuple<>(2, ef)); // 11 syuta
        ef = makeMmlDatum(DM_002);
        efftbl.add(new Tuple<>(2, ef)); // 12 Au
        ef = makeMmlDatum(DM_003);
        efftbl.add(new Tuple<>(2, ef)); // 13 syuba
        ef = makeMmlDatum(DM_004);
        efftbl.add(new Tuple<>(2, ef)); // 14 syu
        ef = makeMmlDatum(DM_005);
        efftbl.add(new Tuple<>(2, ef)); // 15 sya-
        ef = makeMmlDatum(DM_006);
        efftbl.add(new Tuple<>(2, ef)); // 16 po
        ef = makeMmlDatum(DM_007);
        efftbl.add(new Tuple<>(2, ef)); // 17 tattu
        ef = makeMmlDatum(DM_008);
        efftbl.add(new Tuple<>(2, ef)); // 18 zusyau
        ef = makeMmlDatum(DM_009);
        efftbl.add(new Tuple<>(2, ef)); // 19 piro
        ef = makeMmlDatum(DM_010);
        efftbl.add(new Tuple<>(2, ef)); // 20 piron
        ef = makeMmlDatum(DM_011);
        efftbl.add(new Tuple<>(2, ef)); // 21 pirorironn
        ef = makeMmlDatum(DM_012);
        efftbl.add(new Tuple<>(2, ef)); // 22 buu
        ef = makeMmlDatum(DM_013);
        efftbl.add(new Tuple<>(2, ef)); // 23 babon
        ef = makeMmlDatum(DM_014);
        efftbl.add(new Tuple<>(2, ef)); // 24 basyu-
        ef = makeMmlDatum(DM_015);
        efftbl.add(new Tuple<>(2, ef)); // 25 poun
        ef = makeMmlDatum(DM_016);
        efftbl.add(new Tuple<>(2, ef)); // 26 pasyu
        ef = makeMmlDatum(DM_017);
        efftbl.add(new Tuple<>(2, ef)); // 27 KON
        ef = makeMmlDatum(DM_018);
        efftbl.add(new Tuple<>(2, ef)); // 28 dosun
        ef = makeMmlDatum(DM_019);
        efftbl.add(new Tuple<>(2, ef)); // 29 zu
        ef = makeMmlDatum(DM_020);
        efftbl.add(new Tuple<>(2, ef)); // 30 go
        ef = makeMmlDatum(DM_021);
        efftbl.add(new Tuple<>(2, ef)); // 31 poyon
        ef = makeMmlDatum(DM_022);
        efftbl.add(new Tuple<>(2, ef)); // 32 katun
        ef = makeMmlDatum(DM_023);
        efftbl.add(new Tuple<>(2, ef)); // 33 syupin
        ef = makeMmlDatum(DM_024);
        efftbl.add(new Tuple<>(2, ef)); // 34 1UP
        ef = makeMmlDatum(DM_025);
        efftbl.add(new Tuple<>(2, ef)); // 35 PI
        ef = makeMmlDatum(DM_026);
        efftbl.add(new Tuple<>(2, ef)); // 36 pikon
        ef = makeMmlDatum(DM_027);
        efftbl.add(new Tuple<>(2, ef)); // 37 pyuu
        ef = makeMmlDatum(DM_028);
        efftbl.add(new Tuple<>(2, ef)); // 38 PI
        ef = makeMmlDatum(DM_029);
        efftbl.add(new Tuple<>(2, ef)); // 39 click

        ef = makeMmlDatum(RS_006);
        efftbl.add(new Tuple<>(2, ef)); // 40 batan
        ef = makeMmlDatum(RS_007);
        efftbl.add(new Tuple<>(2, ef)); // 41 dodonn
        ef = makeMmlDatum(RS_009);
        efftbl.add(new Tuple<>(2, ef)); // 42 kisya-
        ef = makeMmlDatum(RS_010);
        efftbl.add(new Tuple<>(2, ef)); // 43 bofu
        ef = makeMmlDatum(RS_011);
        efftbl.add(new Tuple<>(2, ef)); // 44 gogogogo--
        ef = makeMmlDatum(RS_012);
        efftbl.add(new Tuple<>(2, ef)); // 45 karakara
        ef = makeMmlDatum(RS_013);
        efftbl.add(new Tuple<>(2, ef)); // 46 buonn
        ef = makeMmlDatum(RS_015);
        efftbl.add(new Tuple<>(2, ef)); // 47 tyattu
        ef = makeMmlDatum(RS_018);
        efftbl.add(new Tuple<>(2, ef)); // 48 zu
        ef = makeMmlDatum(RS_019);
        efftbl.add(new Tuple<>(2, ef)); // 49 saaaa
        ef = makeMmlDatum(RS_020);
        efftbl.add(new Tuple<>(2, ef)); // 50 za
        ef = makeMmlDatum(RS_021);
        efftbl.add(new Tuple<>(2, ef)); // 51 TYARIN
        ef = makeMmlDatum(RS_022);
        efftbl.add(new Tuple<>(2, ef)); // 52 SYUWAWA
        ef = makeMmlDatum(RS_024);
        efftbl.add(new Tuple<>(2, ef)); // 53 PIN
        ef = makeMmlDatum(RS_026);
        efftbl.add(new Tuple<>(2, ef)); // 54 KAMINARI
        ef = makeMmlDatum(RS_027);
        efftbl.add(new Tuple<>(2, ef)); // 55 PI
        ef = makeMmlDatum(RS_028);
        efftbl.add(new Tuple<>(2, ef)); // 56 KEIKOKU
        ef = makeMmlDatum(RS_029);
        efftbl.add(new Tuple<>(2, ef)); // 57 ETC 1
        ef = makeMmlDatum(RS_030);
        efftbl.add(new Tuple<>(2, ef)); // 58 BUFOFOFO
        ef = makeMmlDatum(RS_031);
        efftbl.add(new Tuple<>(2, ef)); // 59 ETC 3
        ef = makeMmlDatum(RS_032);
        efftbl.add(new Tuple<>(2, ef)); // 60 ETC 4
        ef = makeMmlDatum(RS_033);
        efftbl.add(new Tuple<>(2, ef)); // 61 HADE BOMB
        ef = makeMmlDatum(RS_035);
        efftbl.add(new Tuple<>(2, ef)); // 62 JARARAN
        ef = makeMmlDatum(PO_011);
        efftbl.add(new Tuple<>(2, ef)); // 63 Rain fall
        ef = makeMmlDatum(PO_012);
        efftbl.add(new Tuple<>(2, ef)); // 64 Spinner
        ef = makeMmlDatum(PO_013);
        efftbl.add(new Tuple<>(2, ef)); // 65 Kaminari
        ef = makeMmlDatum(PO_014);
        efftbl.add(new Tuple<>(2, ef)); // 66 Sairen
        ef = makeMmlDatum(PO_015);
        efftbl.add(new Tuple<>(2, ef)); // 67 Door Shut
        ef = makeMmlDatum(PO_016);
        efftbl.add(new Tuple<>(2, ef)); // 68 Kiteki
        ef = makeMmlDatum(PO_017);
        efftbl.add(new Tuple<>(2, ef)); // 69 Ship Bomb
        ef = makeMmlDatum(PO_018);
        efftbl.add(new Tuple<>(2, ef)); // 70 Spinner 2
        ef = makeMmlDatum(PO_019);
        efftbl.add(new Tuple<>(2, ef)); // 71 Helli
        ef = makeMmlDatum(PO_020);
        efftbl.add(new Tuple<>(2, ef)); // 72 Kinzoku Sagyou
        ef = makeMmlDatum(PO_021);
        efftbl.add(new Tuple<>(2, ef)); // 73 Kaze (DAME)
        ef = makeMmlDatum(PO_022);
        efftbl.add(new Tuple<>(2, ef)); // 74 Taikushitu Soto
        ef = makeMmlDatum(PO_023);
        efftbl.add(new Tuple<>(2, ef)); // 75 Punch
        ef = makeMmlDatum(PO_024);
        efftbl.add(new Tuple<>(2, ef)); // 76 Shower
        ef = makeMmlDatum(PO_025);
        efftbl.add(new Tuple<>(2, ef)); // 77 Shokki
        ef = makeMmlDatum(PO_026);
        efftbl.add(new Tuple<>(2, ef)); // 78 Tobikomi
        ef = makeMmlDatum(PO_027);
        efftbl.add(new Tuple<>(2, ef)); // 79 Air Fukidasi
        ef = makeMmlDatum(PO_028);
        efftbl.add(new Tuple<>(2, ef)); // 80 Heavy Door Open
        ef = makeMmlDatum(PO_029);
        efftbl.add(new Tuple<>(2, ef)); // 81 Car Door Shut
        ef = makeMmlDatum(PO_030);
        efftbl.add(new Tuple<>(2, ef)); // 82 Car Come'in
        ef = makeMmlDatum(PO_031);
        efftbl.add(new Tuple<>(2, ef)); // 83 Ice Hikkaki
        ef = makeMmlDatum(PO_032);
        efftbl.add(new Tuple<>(2, ef)); // 84 Ship Crush Down
        ef = makeMmlDatum(PO_033);
        efftbl.add(new Tuple<>(2, ef)); // 85 Toraware
        ef = makeMmlDatum(PO_034);
        efftbl.add(new Tuple<>(2, ef)); // 86 Sekizou Break
        ef = makeMmlDatum(PO_035);
        efftbl.add(new Tuple<>(2, ef)); // 87 Blaster Shot
        ef = makeMmlDatum(PO_036);
        efftbl.add(new Tuple<>(2, ef)); // 88 Seifuku Yabuki
        ef = makeMmlDatum(PO_037);
        efftbl.add(new Tuple<>(2, ef)); // 89 Miminari
        ef = makeMmlDatum(PO_038);
        efftbl.add(new Tuple<>(2, ef)); // 90 Sekizou Ayasige
        ef = makeMmlDatum(PO_039);
        efftbl.add(new Tuple<>(2, ef)); // 91 Voiler & Engine
        ef = makeMmlDatum(PO_040);
        efftbl.add(new Tuple<>(2, ef)); // 92 Suimen
        ef = makeMmlDatum(PO_041);
        efftbl.add(new Tuple<>(2, ef)); // 93 Kika
        ef = makeMmlDatum(PO_042);
        efftbl.add(new Tuple<>(2, ef)); // 94 Change Kakyuu
        ef = makeMmlDatum(PO_043);
        efftbl.add(new Tuple<>(2, ef)); // 95 Change Blue
        ef = makeMmlDatum(PO_044);
        efftbl.add(new Tuple<>(2, ef)); // 96 Youma Funsyutu
        ef = makeMmlDatum(PO_045);
        efftbl.add(new Tuple<>(2, ef)); // 97 Kekkai
        ef = makeMmlDatum(PO_046);
        efftbl.add(new Tuple<>(2, ef)); // 98 Gosintou 1
        ef = makeMmlDatum(PO_047);
        efftbl.add(new Tuple<>(2, ef)); // 99 Gosintou 2
        ef = makeMmlDatum(PO_048);
        efftbl.add(new Tuple<>(2, ef)); // 100 Gosintou 3
        ef = makeMmlDatum(PO_049);
        efftbl.add(new Tuple<>(2, ef)); // 101 Hand Blaster
        ef = makeMmlDatum(PO_050);
        efftbl.add(new Tuple<>(2, ef)); // 102 Magic
        ef = makeMmlDatum(PO_051);
        efftbl.add(new Tuple<>(2, ef)); // 103 Grabiton 1
        ef = makeMmlDatum(PO_052);
        efftbl.add(new Tuple<>(2, ef)); // 104 Grabiton 2
        ef = makeMmlDatum(PO_053);
        efftbl.add(new Tuple<>(2, ef)); // 105 Attack Kakyuu
        ef = makeMmlDatum(PO_054);
        efftbl.add(new Tuple<>(2, ef)); // 106 Attack Blue(TEKITOU)
        ef = makeMmlDatum(PO_055);
        efftbl.add(new Tuple<>(2, ef)); // 107 Attack Red
        ef = makeMmlDatum(PO_056);
        efftbl.add(new Tuple<>(2, ef)); // 108 Attack White
        ef = makeMmlDatum(PO_057);
        efftbl.add(new Tuple<>(2, ef)); // 109 Attack Black
        ef = makeMmlDatum(PO_058);
        efftbl.add(new Tuple<>(2, ef)); // 110 Attack Last
        ef = makeMmlDatum(PO_059);
        efftbl.add(new Tuple<>(2, ef)); // 111 Damage 1
        ef = makeMmlDatum(PO_060);
        efftbl.add(new Tuple<>(2, ef)); // 112 Damage 2
        ef = makeMmlDatum(PO_061);
        efftbl.add(new Tuple<>(2, ef)); // 113 Attack
        ef = makeMmlDatum(ND_000);
        efftbl.add(new Tuple<>(2, ef)); // 114 MAP
        ef = makeMmlDatum(ND_001);
        efftbl.add(new Tuple<>(2, ef)); // 115 SONAR
        ef = makeMmlDatum(ND_002);
        efftbl.add(new Tuple<>(2, ef)); // 116 KOUKOU
        ef = makeMmlDatum(ND_003);
        efftbl.add(new Tuple<>(2, ef)); // 117 MEGIDO
        ef = makeMmlDatum(ND_004);
        efftbl.add(new Tuple<>(2, ef)); // 118 JINARI
        ef = makeMmlDatum(ND_005);
        efftbl.add(new Tuple<>(2, ef)); // 119 SWITCH
        ef = makeMmlDatum(ND_006);
        efftbl.add(new Tuple<>(2, ef)); // 120 DOSYUUNN
        ef = makeMmlDatum(ND_007);
        efftbl.add(new Tuple<>(2, ef)); // 121 GYUOON
        ef = makeMmlDatum(ND_008);
        efftbl.add(new Tuple<>(2, ef)); // 122 PIPIPIPI
        ef = makeMmlDatum(ND_009);
        efftbl.add(new Tuple<>(2, ef)); // 123 SYUBATTU
        ef = makeMmlDatum(ND_010);
        efftbl.add(new Tuple<>(2, ef)); // 124 BEAM UNARI
        ef = makeMmlDatum(ND_011);
        efftbl.add(new Tuple<>(2, ef)); // 125 BEAM KAKUSAN
        ef = makeMmlDatum(ND_012);
        efftbl.add(new Tuple<>(2, ef)); // 126 ORGAN
        ef = makeMmlDatum(ND_013);
        efftbl.add(new Tuple<>(2, ef)); // 127 PANEL
        ef = makeMmlDatum(ND_014);
        efftbl.add(new Tuple<>(2, ef)); // 128 DRILL
        ef = makeMmlDatum(ND_015);
        efftbl.add(new Tuple<>(2, ef)); // 129 PRAZMA
        ef = makeMmlDatum(ND_016);
        efftbl.add(new Tuple<>(2, ef)); // 130 BABEL
        ef = makeMmlDatum(ND_017);
        efftbl.add(new Tuple<>(2, ef)); // 131 ELEVETOR
        ef = makeMmlDatum(ND_018);
        efftbl.add(new Tuple<>(2, ef)); // 132 MEGIDO HASSYA
        ef = makeMmlDatum(ND_019);
        efftbl.add(new Tuple<>(2, ef)); // 133 DAIBAKUHATU
        ef = makeMmlDatum(ND_020);
        efftbl.add(new Tuple<>(2, ef)); // 134 NAMI
        ef = makeMmlDatum(ND_021);
        efftbl.add(new Tuple<>(2, ef)); // 135 DOOOONN
        ef = makeMmlDatum(ND_022);
        efftbl.add(new Tuple<>(2, ef)); // 136 DOGA
        ef = makeMmlDatum(ND_023);
        efftbl.add(new Tuple<>(2, ef)); // 137 PISI
        ef = makeMmlDatum(ND_024);
        efftbl.add(new Tuple<>(2, ef)); // 138 BLUE WATER
        ef = makeMmlDatum(ND_025);
        efftbl.add(new Tuple<>(2, ef)); // 139 HOWAWAN
        ef = makeMmlDatum(ND_026);
        efftbl.add(new Tuple<>(2, ef)); // 140 ZUGAN
        ef = makeMmlDatum(ND_027);
        efftbl.add(new Tuple<>(2, ef)); // 141 DAAANN
        ef = makeMmlDatum(ND_028);
        efftbl.add(new Tuple<>(2, ef)); // 142 DOGOOOONN
        ef = makeMmlDatum(ND_029);
        efftbl.add(new Tuple<>(2, ef)); // 143 GASYA
        ef = makeMmlDatum(ND_030);
        efftbl.add(new Tuple<>(2, ef)); // 144 BASYUSYUSYU
        ef = makeMmlDatum(ND_031);
        efftbl.add(new Tuple<>(2, ef)); // 145 DOSYUSYUSYU
        ef = makeMmlDatum(ND_032);
        efftbl.add(new Tuple<>(2, ef)); // 146 SYUSYUUUUNN
        ef = makeMmlDatum(ND_033);
        efftbl.add(new Tuple<>(2, ef)); // 147 BASYANN - HYURURURU
        ef = makeMmlDatum(ND_034);
        efftbl.add(new Tuple<>(2, ef)); // 148 ZYURUZYURU
        ef = makeMmlDatum(ND_035);
        efftbl.add(new Tuple<>(2, ef)); // 149 ZUGOGOGOGO
        ef = makeMmlDatum(ND_036);
        efftbl.add(new Tuple<>(2, ef)); // 150 ZUGOOOONN
        ef = makeMmlDatum(ND_037);
        efftbl.add(new Tuple<>(2, ef)); // 151 BI--
        ef = makeMmlDatum(ND_038);
        efftbl.add(new Tuple<>(2, ef)); // 152 BASYUSYUUU
        ef = makeMmlDatum(ND_039);
        efftbl.add(new Tuple<>(2, ef)); // 153 BISYU
//#endregion
    }

    public void setOption(PMDOption dop, String[] op) {
        pmdOption = op;

        jumpIndex = dop.jumpIndex;
        //logger.log(Level.TRACE, "%d", jumpIndex);

        if (dop.isAUTO) {
            dop.usePPS = ppsFile != null && !ppsFile.isEmpty();
            dop.usePPZ = ppz1File != null && !ppz1File.isEmpty() || ppz2File != null && !ppz2File.isEmpty();
            dop.isNRM = false;
            dop.isSPB = true;
            if (dop.ppcHeader.equals("PCM")) dop.isSPB = false;
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
            ms_cmd = 0x188; // 8259 Master Port
            ms_msk = 0x18a; // 8259 Master/Mask
            sl_cmd = 0x184; // 8259 slave port
            sl_msk = 0x186; // 8259 Slave/Mask
        }

        if (board2 == 0) {
            // normal
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

            max_part1 = 11; // 0 Number of parts to be cleared
            max_part2 = 11; // Number of parts to be initialized
        } else {
            //board2
            if (ppz == 0) {
                partWk = new partWork[3 + 3 + 5 + 3 + 1];
                part_e = 14;
                partWk[part_e] = new partWork();

                max_part1 = 14; // 0 Number of parts to be cleared
                max_part2 = 11; // Number of parts to be initialized
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

                max_part1 = 14 + 8; // 0 Number of parts to be cleared
                max_part2 = 11; // Number of parts to be initialized
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

    private static MmlDatum[] makeMmlDatum(int[] dd) {
        List<MmlDatum> ret = new ArrayList<>();
        for (int b : dd) ret.add(new MmlDatum(b & 0xff));
        return ret.toArray(MmlDatum[]::new);
    }

    // EFFECT.INC
    public final List<Tuple<Integer, MmlDatum[]>> efftbl;

//#region Sound effect data

    private static final int[] D_000 = { // Bass Drum                1990-06-22 05:47:11
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 220, 5, 31, 54, 15, 0, 0, 0, 127, 0,
            8, 164, 6, 0, 62, 16, 176, 4, 0, 127, 0,
            0xff // -1
    };
    private static final int[] D_001 = { // Snare Drum               1990-06-22 05:48:06
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            14, 144, 1, 7, 54, 16, 184, 11, 0, 93, 242,
            0xff // -1
    };
    private static final int[] D_002 = { // Low Tom                  1990-06-22 05:49:19
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 188, 2, 0, 54, 15, 0, 0, 0, 100, 0,
            14, 132, 3, 0, 54, 16, 196, 9, 0, 100, 0,
            0xff // -1
    };
    private static final int[] D_003 = { // Middle Tom               1990-06-22 05:50:23
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 244, 1, 5, 54, 15, 0, 0, 0, 60, 0,
            14, 108, 2, 0, 54, 16, 196, 9, 0, 60, 0,
            0xff // -1
    };
    private static final int[] D_004 = { // High Tom                 1990-06-22 05:51:13
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 44, 1, 0, 54, 15, 0, 0, 0, 50, 0,
            14, 144, 1, 0, 54, 16, 196, 9, 0, 50, 0,
            0xff // -1
    };
    private static final int[] D_005 = { // Rim Shot                 1990-06-22 05:51:57
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 55, 0, 0, 62, 16, 44, 1, 0, 100, 0,
            0xff // -1
    };
    private static final int[] D_006 = { // Snare Drum 2             1990-06-22 05:52:36
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 0, 0, 15, 55, 16, 184, 11, 0, 0, 241,
            0xff // -1
    };
    private static final int[] D_007 = { // Hi-Hat Close             1990-06-22 05:53:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 39, 0, 0, 54, 16, 244, 1, 0, 0, 0,
            0xff // -1
    };
    private static final int[] D_008 = { // Hi-Hat Open              1990-06-22 05:53:40
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            32, 39, 0, 0, 54, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };
    private static final int[] D_009 = { // Crush Cymbal             1990-06-22 05:54:11
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            31, 40, 0, 31, 54, 16, 136, 19, 0, 0, 241,
            0xff // -1
    };
    private static final int[] D_010 = { // Ride Cymbal              1990-06-22 05:54:38
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            31, 30, 0, 0, 54, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };

    //
    // Effect for "Dengeki MIX"
    //

    private static final int[] DM_001 = { // syuta                    1994-05-25 23:13:02
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 15, 55, 16, 232, 3, 0, 0, 113,
            2, 221, 1, 0, 55, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_002 = { // Au                       1994-05-25 23:13:07
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 44, 1, 0, 62, 16, 44, 1, 13, 0, 0,
            6, 44, 1, 0, 62, 16, 16, 39, 0, 80, 0,
            0xff // -1
    };
    private static final int[] DM_003 = { // syuba                    1994-05-25 23:13:25
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 0, 55, 14, 16, 39, 0, 0, 81,
            4, 221, 1, 10, 55, 16, 208, 7, 0, 0, 241,
            0xff // -1
    };
    private static final int[] DM_004 = { // syu                      1994-05-25 23:17:51
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 0, 55, 16, 244, 1, 13, 0, 0,
            8, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_005 = { // sya-                     1994-05-25 23:19:01
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 221, 1, 10, 55, 16, 100, 0, 13, 0, 0,
            16, 221, 1, 5, 55, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_006 = { // po                       1994-05-25 23:13:32
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 144, 1, 0, 62, 16, 244, 1, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_007 = { // tattu                    1994-05-25 23:13:37
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 15, 55, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_008 = { // zusyau                   1994-05-25 23:13:42
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 221, 1, 31, 55, 15, 16, 39, 0, 0, 0,
            12, 221, 1, 0, 55, 16, 136, 19, 0, 0, 17,
            0xff // -1
    };
    private static final int[] DM_009 = { // piro                     1994-05-25 23:20:41
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 144, 1, 0, 62, 16, 232, 3, 0, 0, 0,
            2, 200, 0, 0, 62, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_010 = { // piron                    1994-05-25 23:20:26
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0,
            8, 200, 0, 0, 62, 16, 184, 11, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_011 = { // pirorironn               1994-05-25 23:21:50
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0,
            3, 100, 0, 0, 62, 16, 208, 7, 0, 0, 0,
            3, 200, 0, 0, 62, 16, 208, 7, 0, 0, 0,
            3, 144, 1, 0, 62, 16, 208, 7, 0, 0, 0,
            8, 100, 0, 0, 62, 16, 184, 11, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_012 = { // buu                      1994-05-25 23:23:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 208, 7, 0, 62, 15, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_013 = { // babon                    1994-05-25 23:15:40
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 31, 55, 16, 136, 19, 0, 0, 0,
            8, 221, 1, 31, 54, 16, 184, 11, 0, 127, 241,
            0xff // -1
    };
    private static final int[] DM_014 = { // basyu-                   1994-05-25 23:15:44
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 25, 55, 16, 208, 7, 0, 0, 0,
            32, 221, 1, 20, 55, 16, 112, 23, 0, 0, 19,
            0xff // -1
    };
    private static final int[] DM_015 = { // poun                     1994-05-25 23:15:27
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 200, 0, 0, 54, 16, 136, 19, 0, 20, 0,
            0xff // -1
    };
    private static final int[] DM_016 = { // pasyu                    1994-05-25 23:22:59
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 40, 0, 20, 54, 16, 16, 39, 0, 20, 0,
            16, 20, 0, 5, 54, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_017 = { // KON                      1994-05-25 23:16:07
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            6, 88, 2, 0, 62, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_018 = { // dosun                    1994-05-25 23:23:57
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 232, 3, 0, 62, 16, 16, 39, 0, 127, 0,
            16, 221, 1, 0, 54, 16, 16, 39, 0, 64, 0,
            0xff // -1
    };
    private static final int[] DM_019 = { // zu                       1994-05-25 23:24:59
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 232, 3, 31, 54, 15, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_020 = { // go                       1994-05-25 23:24:43
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 255, 15, 31, 54, 15, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_021 = { // poyon                    1994-05-25 23:26:17
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 221, 1, 0, 62, 16, 232, 3, 0, 206, 0,
            16, 242, 0, 0, 62, 16, 112, 23, 0, 248, 0,
            0xff // -1
    };
    private static final int[] DM_022 = { // katun                    1994-05-25 23:27:10
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 100, 0, 0, 62, 16, 244, 1, 0, 0, 0,
            4, 10, 0, 0, 54, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_023 = { // syupin                   1994-05-25 23:28:18
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            8, 221, 1, 5, 55, 16, 244, 1, 13, 0, 0,
            24, 30, 0, 0, 54, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_024 = { // 1UP                      1994-05-25 23:16:52
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 44, 1, 0, 62, 16, 136, 19, 0, 0, 0,
            4, 180, 0, 0, 62, 16, 136, 19, 0, 0, 0,
            4, 200, 0, 0, 62, 16, 136, 19, 0, 0, 0,
            24, 150, 0, 0, 62, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_025 = { // PI                       1994-05-25 23:16:35
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            3, 238, 0, 0, 62, 14, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_026 = { // pikon                    1994-05-25 23:29:19
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            4, 200, 0, 0, 62, 16, 136, 19, 0, 0, 0,
            16, 100, 0, 0, 62, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_027 = { // pyuu                     1994-05-25 23:30:33
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            16, 0, 0, 0, 54, 16, 244, 1, 13, 1, 17,
            16, 16, 0, 16, 54, 16, 124, 21, 0, 1, 17,
            0xff // -1
    };
    private static final int[] DM_028 = { // PI                       1994-05-25 23:16:24
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            1, 200, 0, 0, 62, 14, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] DM_029 = { // click                    1994-05-25 23:14:24
            //len freqL freqH noise  mix  Evol envL envH envPtn sweepT sweepN
            2, 200, 0, 0, 62, 16, 32, 3, 0, 0, 0,
            2, 100, 0, 0, 62, 16, 32, 3, 0, 0, 0,
            2, 50, 0, 0, 62, 16, 32, 3, 0, 0, 0,
            2, 25, 0, 0, 62, 16, 32, 3, 0, 0, 0,
            0xff // -1
    };

    //
    // Effect for Rusty
    //
    private static final int[] RS_006 = { // batan                    1993-01-08 01:44:30
            2, 221, 1, 31, 55, 16, 232, 3, 0, 0, 0,
            6, 221, 1, 10, 55, 16, 208, 7, 0, 0, 17,
            0xff // -1
    };
    private static final int[] RS_007 = { // dodonn                   1993-01-08 01:39:10
            4, 232, 3, 15, 54, 16, 16, 39, 0, 127, 0,
            16, 244, 1, 5, 54, 16, 136, 19, 0, 127, 243,
            0xff // -1
    };
    private static final int[] RS_009 = { // kisya-                   1993-01-08 01:39:47
            4, 40, 0, 20, 54, 16, 16, 39, 0, 20, 0,
            24, 20, 0, 5, 54, 16, 16, 39, 0, 1, 0,
            0xff // -1
    };
    private static final int[] RS_010 = { // bofu                     1993-01-08 01:45:38
            4, 232, 3, 0, 54, 15, 16, 39, 0, 127, 0,
            32, 10, 0, 10, 55, 16, 112, 23, 0, 0, 243,
            0xff // -1
    };
    private static final int[] RS_011 = { // gogogogo--               1993-06-29 12:27:41
            96, 255, 15, 31, 54, 16, 96, 234, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_012 = { // karakara                 1993-06-29 12:16:36
            64, 10, 0, 0, 54, 16, 32, 78, 0, 0, 129,
            0xff // -1
    };
    private static final int[] RS_013 = { // buonn                    1993-01-08 01:47:56
            8, 208, 7, 0, 62, 16, 144, 1, 13, 0, 0,
            8, 208, 7, 0, 62, 16, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_015 = { // tyattu                   1993-01-08 01:49:27
            4, 20, 0, 8, 54, 16, 184, 11, 0, 0, 225,
            0xff // -1
    };
    private static final int[] RS_018 = { // zu                       1993-01-08 01:51:05
            4, 208, 7, 30, 54, 16, 160, 15, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_019 = { // saaaa                    1993-06-29 12:28:05
            60, 221, 1, 4, 55, 10, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_020 = { // za                       1993-01-08 01:52:35
            6, 221, 1, 16, 55, 16, 136, 19, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_021 = { // TYARIN                   1993-06-29 12:29:19
            4, 40, 0, 0, 54, 15, 16, 39, 0, 0, 0,
            8, 30, 0, 0, 62, 16, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_022 = { // SYUWAWA                  1993-06-29 12:35:38
            48, 100, 0, 0, 55, 16, 136, 19, 13, 255, 33,
            12, 50, 0, 0, 55, 13, 136, 19, 0, 0, 33,
            12, 221, 1, 0, 55, 12, 16, 39, 0, 0, 33,
            12, 221, 1, 0, 55, 11, 16, 39, 0, 0, 33,
            12, 221, 1, 0, 55, 10, 16, 39, 0, 0, 33,
            12, 221, 1, 0, 55, 9, 16, 39, 0, 0, 33,
            0xff // -1
    };
    private static final int[] RS_024 = { // PIN                      1993-06-29 12:36:42
            6, 100, 0, 0, 62, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_026 = { // KAMINARI                 1993-06-29 12:42:57
            4, 23, 0, 31, 55, 16, 208, 7, 0, 0, 0,
            64, 15, 0, 31, 55, 16, 152, 58, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_027 = { // PI                       1993-06-29 12:44:03
            3, 238, 0, 0, 62, 14, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_028 = { // KEIKOKU                  1993-06-29 12:46:13
            7, 44, 1, 0, 62, 16, 160, 15, 0, 0, 0,
            7, 44, 1, 0, 62, 16, 208, 7, 0, 0, 0,
            48, 44, 1, 0, 62, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] RS_029 = { // ETC 1                    1993-06-29 12:46:54
            96, 208, 7, 0, 62, 16, 16, 39, 0, 236, 0,
            0xff // -1
    };
    private static final int[] RS_030 = { // BUFOFOFO                 1993-06-29 12:58:12
            8, 208, 7, 0, 62, 16, 16, 39, 0, 176, 0,
            8, 8, 7, 0, 62, 16, 16, 39, 0, 176, 0,
            8, 64, 6, 0, 62, 16, 16, 39, 0, 176, 0,
            48, 120, 5, 0, 62, 16, 16, 39, 0, 186, 0,
            0xff // -1
    };
    private static final int[] RS_031 = { // ETC 3                    1993-06-29 12:49:32
            8, 232, 3, 0, 62, 16, 16, 39, 0, 80, 0,
            8, 176, 4, 0, 62, 16, 16, 39, 0, 80, 0,
            8, 20, 5, 0, 62, 16, 16, 39, 0, 80, 0,
            48, 120, 5, 0, 62, 16, 16, 39, 0, 80, 0,
            0xff // -1
    };
    private static final int[] RS_032 = { // ETC 4                    1993-06-29 12:50:11
            96, 0, 0, 0, 62, 16, 16, 39, 0, 128, 0,
            0xff // -1
    };
    private static final int[] RS_033 = { // HADE BOMB                1993-06-29 12:52:06
            4, 100, 0, 31, 54, 16, 208, 7, 0, 127, 0,
            32, 0, 0, 31, 54, 16, 16, 39, 0, 127, 129,
            0xff // -1
    };
    private static final int[] RS_035 = { // JARARAN                  1993-06-29 13:02:17
            2, 244, 1, 20, 54, 16, 16, 39, 0, 252, 0,
            2, 144, 1, 15, 54, 16, 16, 39, 0, 252, 65,
            2, 44, 1, 10, 62, 16, 16, 39, 0, 252, 65,
            2, 200, 0, 5, 54, 16, 16, 39, 0, 252, 65,
            16, 150, 0, 0, 62, 16, 184, 11, 0, 0, 0,
            0xff // -1
    };

    //
    // Effect for "Possessioner"
    //

    private static final int[] PO_011 = { // Rain fall                1990-06-22 05:55:43
            254, 221, 1, 3, 55, 10, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_012 = { // Spinner                  1990-06-22 05:57:18
            24, 140, 0, 0, 62, 16, 88, 27, 0, 14, 0,
            0xff // -1
    };
    private static final int[] PO_013 = { // Kaminari                 1990-06-22 05:59:16
            48, 160, 15, 31, 54, 16, 16, 39, 0, 0, 1,
            0xff // -1
    };
    private static final int[] PO_014 = { // Sairen                   1990-06-22 06:00:45
            31, 100, 0, 0, 62, 16, 88, 27, 0, 255, 0,
            0xff // -1
    };
    private static final int[] PO_015 = { // Door Shut                1990-06-22 06:03:28
            6, 221, 1, 8, 55, 16, 184, 11, 0, 0, 241,
            8, 144, 1, 0, 54, 16, 144, 1, 13, 216, 0,
            0xff // -1
    };
    private static final int[] PO_016 = { // Kiteki                   1990-06-22 06:05:23
            96, 160, 15, 0, 62, 16, 48, 117, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_017 = { // Ship Bomb                1990-06-22 06:06:54
            4, 221, 1, 31, 55, 16, 208, 7, 0, 0, 0,
            64, 221, 1, 20, 55, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_018 = { // Spinner 2                1990-06-22 06:08:08
            64, 120, 0, 0, 54, 16, 16, 39, 0, 2, 0,
            0xff // -1
    };
    private static final int[] PO_019 = { // Helli                    1990-06-22 06:09:58
            4, 221, 1, 4, 55, 16, 208, 7, 0, 0, 0,
            4, 221, 1, 0, 55, 16, 208, 7, 0, 0, 0,
            4, 221, 1, 4, 55, 16, 232, 3, 0, 0, 0,
            4, 221, 1, 0, 55, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_020 = { // Kinzoku Sagyou           1990-06-22 07:23:41
            16, 30, 0, 5, 54, 16, 160, 15, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_021 = { // Kaze (DAME)              1990-06-22 06:13:46
            16, 220, 5, 0, 62, 15, 16, 39, 0, 0, 0,
            8, 220, 5, 0, 62, 15, 16, 39, 0, 246, 0,
            48, 140, 5, 0, 62, 16, 16, 39, 0, 10, 0,
            0xff // -1
    };
    private static final int[] PO_022 = { // Taikushitu Soto          1990-06-22 06:15:55
            4, 160, 15, 31, 54, 16, 184, 11, 0, 0, 0,
            24, 184, 11, 8, 54, 16, 136, 19, 0, 40, 20,
            0xff // -1
    };
    private static final int[] PO_023 = { // Punch                    1990-06-22 06:17:13
            4, 160, 15, 31, 54, 16, 208, 7, 0, 10, 0,
            8, 221, 1, 28, 54, 16, 208, 7, 0, 127, 0,
            0xff // -1
    };
    private static final int[] PO_024 = { // Shower                   1990-06-22 06:19:08
            254, 0, 0, 0, 55, 10, 0, 0, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_025 = { // Shokki                   1990-06-22 06:22:14
            6, 31, 0, 4, 54, 16, 232, 3, 0, 0, 0,
            8, 30, 0, 0, 54, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_026 = { // Tobikomi                 1990-06-22 06:24:09
            8, 220, 5, 25, 54, 16, 184, 11, 0, 127, 0,
            48, 221, 1, 10, 55, 16, 64, 31, 0, 0, 18,
            0xff // -1
    };
    private static final int[] PO_027 = { // Air Fukidasi             1990-06-22 06:25:35
            4, 208, 7, 0, 55, 16, 208, 7, 0, 0, 0,
            48, 221, 1, 4, 55, 16, 16, 39, 0, 0, 20,
            0xff // -1
    };
    private static final int[] PO_028 = { // Heavy Door Open          1990-06-22 07:23:33
            48, 208, 7, 31, 54, 16, 152, 58, 0, 251, 0,
            0xff // -1
    };
    private static final int[] PO_029 = { // Car Door Shut            1990-06-22 07:23:30
            16, 232, 3, 31, 54, 16, 184, 11, 0, 127, 0,
            0xff // -1
    };
    private static final int[] PO_030 = { // Car Come'in              1990-06-22 06:30:31
            4, 160, 15, 31, 54, 15, 16, 39, 0, 0, 0,
            96, 160, 15, 28, 54, 16, 32, 78, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_031 = { // Ice Hikkaki              1990-06-22 06:31:26
            2, 10, 0, 0, 54, 16, 244, 1, 0, 0, 0,
            2, 20, 0, 0, 54, 16, 244, 1, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_032 = { // Ship Crush Down          1990-06-22 07:23:23
            64, 160, 15, 20, 54, 16, 48, 117, 0, 1, 22,
            192, 221, 1, 31, 55, 16, 48, 117, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_033 = { // Toraware                 1990-06-22 06:35:02
            32, 232, 3, 0, 54, 16, 64, 31, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_034 = { // Sekizou Break            1990-06-22 06:36:14
            4, 221, 1, 31, 55, 15, 16, 39, 0, 0, 0,
            64, 221, 1, 10, 55, 16, 16, 39, 0, 0, 18,
            0xff // -1
    };
    private static final int[] PO_035 = { // Blaster Shot             1990-06-22 06:37:55
            4, 221, 1, 31, 55, 16, 184, 11, 0, 0, 0,
            4, 160, 15, 20, 54, 16, 184, 11, 0, 20, 0,
            64, 0, 0, 4, 54, 16, 16, 39, 0, 1, 20,
            0xff // -1
    };
    private static final int[] PO_036 = { // Seifuku Yabuki           1990-06-22 06:39:58
            16, 221, 1, 4, 55, 14, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_037 = { // Miminari                 1990-06-22 06:42:13
            4, 8, 0, 0, 62, 16, 1, 0, 8, 0, 0,
            64, 0, 0, 0, 62, 16, 64, 31, 0, 1, 0,
            0xff // -1
    };
    private static final int[] PO_038 = { // Sekizou Ayasige          1990-06-22 06:44:23
            40, 160, 15, 0, 62, 16, 232, 253, 0, 246, 0,
            50, 16, 14, 0, 62, 16, 32, 78, 0, 10, 0,
            0xff // -1
    };
    private static final int[] PO_039 = { // Voiler & Engine          1990-06-22 07:23:14
            60, 221, 1, 30, 55, 14, 48, 117, 0, 0, 242,
            16, 184, 11, 2, 55, 16, 112, 23, 0, 40, 17,
            0xff // -1
    };
    private static final int[] PO_040 = { // Suimen                   1990-06-22 06:47:58
            4, 10, 0, 4, 54, 16, 232, 3, 0, 0, 0,
            6, 221, 1, 0, 55, 16, 208, 7, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_041 = { // Kika                     1990-06-22 06:48:29
            64, 221, 1, 0, 55, 16, 16, 39, 0, 0, 20,
            0xff // -1
    };
    private static final int[] PO_042 = { // Change Kakyuu            1990-06-22 06:50:00
            48, 232, 3, 0, 62, 16, 16, 39, 0, 10, 0,
            0xff // -1
    };
    private static final int[] PO_043 = { // Change Blue              1990-06-22 06:51:47
            15, 100, 0, 0, 62, 16, 232, 3, 14, 252, 0,
            0xff // -1
    };
    private static final int[] PO_044 = { // Youma Funsyutu           1990-06-22 06:54:06
            6, 221, 1, 4, 55, 16, 208, 7, 0, 0, 0,
            4, 100, 0, 4, 54, 16, 160, 15, 0, 236, 0,
            64, 221, 1, 8, 55, 16, 64, 31, 0, 0, 246,
            0xff // -1
    };
    private static final int[] PO_045 = { // Kekkai                   1990-06-22 07:23:06
            128, 232, 3, 31, 54, 16, 48, 117, 0, 1, 242,
            0xff // -1
    };
    private static final int[] PO_046 = { // Gosintou 1               1990-06-22 06:56:47
            4, 20, 0, 0, 54, 16, 232, 3, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_047 = { // Gosintou 2               1990-06-22 06:58:25
            8, 208, 7, 0, 62, 16, 232, 3, 13, 246, 0,
            64, 208, 7, 0, 62, 16, 16, 39, 0, 2, 0,
            0xff // -1
    };
    private static final int[] PO_048 = { // Gosintou 3               1990-06-22 07:00:22
            8, 221, 1, 0, 55, 16, 32, 3, 13, 0, 17,
            16, 221, 1, 0, 55, 16, 208, 7, 0, 0, 17,
            0xff // -1
    };
    private static final int[] PO_049 = { // Hand Blaster             1990-06-22 07:01:53
            4, 160, 15, 31, 54, 16, 184, 11, 0, 0, 0,
            4, 40, 0, 0, 54, 16, 232, 3, 0, 246, 0,
            64, 221, 1, 0, 55, 16, 16, 39, 0, 0, 18,
            0xff // -1
    };
    private static final int[] PO_050 = { // Magic                    1990-06-22 07:04:00
            4, 32, 0, 0, 62, 16, 208, 7, 0, 0, 0,
            24, 32, 0, 0, 54, 16, 64, 31, 0, 255, 0,
            90, 160, 15, 31, 54, 16, 48, 117, 0, 216, 244,
            0xff // -1
    };
    private static final int[] PO_051 = { // Grabiton 1               1990-06-22 07:04:41
            4, 221, 1, 31, 55, 16, 16, 39, 0, 0, 0,
            31, 221, 1, 0, 55, 16, 16, 39, 0, 0, 17,
            0xff // -1
    };
    private static final int[] PO_052 = { // Grabiton 2               1990-06-22 07:05:10
            128, 160, 15, 31, 54, 16, 48, 117, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_053 = { // Attack Kakyuu            1990-06-22 07:06:38
            4, 160, 15, 31, 54, 16, 16, 39, 0, 0, 0,
            16, 221, 1, 0, 55, 16, 112, 23, 0, 0, 0,
            0xff // -1
    };
    private static final int[] PO_054 = { // Attack Blue(TEKITOU)     1990-06-22 07:08:33
            6, 100, 0, 0, 54, 16, 244, 1, 13, 251, 0,
            16, 70, 0, 0, 54, 16, 112, 23, 0, 127, 0,
            0xff // -1
    };
    private static final int[] PO_055 = { // Attack Red               1990-06-22 07:10:10
            20, 184, 11, 0, 54, 14, 16, 39, 0, 156, 0,
            16, 232, 3, 0, 54, 16, 112, 23, 0, 100, 0,
            0xff // -1
    };
    private static final int[] PO_056 = { // Attack White             1990-06-22 07:11:16
            4, 0, 0, 4, 54, 16, 16, 39, 0, 127, 241,
            16, 0, 0, 0, 54, 16, 112, 23, 0, 10, 17,
            0xff // -1
    };
    private static final int[] PO_057 = { // Attack Black             1990-06-22 07:22:10
            4, 200, 0, 4, 54, 16, 208, 7, 0, 127, 17,
            10, 0, 0, 0, 54, 16, 88, 2, 13, 1, 0,
            24, 10, 0, 0, 54, 16, 112, 23, 0, 5, 17,
            0xff // -1
    };
    private static final int[] PO_058 = { // Attack Last              1990-06-22 07:22:14
            20, 60, 0, 4, 54, 14, 16, 39, 0, 255, 0,
            20, 40, 0, 0, 54, 14, 16, 39, 0, 1, 113,
            20, 60, 0, 10, 54, 16, 112, 23, 0, 1, 20,
            0xff // -1
    };
    private static final int[] PO_059 = { // Damage 1                 1990-06-22 07:17:32
            4, 221, 1, 31, 54, 16, 184, 11, 0, 127, 0,
            16, 221, 1, 0, 55, 16, 112, 23, 0, 0, 33,
            0xff // -1
    };
    private static final int[] PO_060 = { // Damage 2                 1990-06-22 07:19:18
            8, 232, 3, 31, 54, 14, 16, 39, 0, 100, 0,
            8, 120, 5, 31, 54, 15, 16, 39, 0, 156, 113,
            16, 88, 2, 31, 54, 16, 112, 23, 0, 127, 241,
            0xff // -1
    };
    private static final int[] PO_061 = { // Attack                   1990-06-22 07:22:55
            8, 0, 0, 31, 54, 16, 184, 11, 0, 100, 0,
            24, 221, 1, 0, 55, 16, 16, 39, 0, 0, 17,
            0xff // -1
    };

    //
    // Effect for NADIA
    //

    private static final int[] ND_000 = { // MAP                      1992-01-27 17:32:40
            48, 221, 1, 0, 62, 16, 16, 39, 0, 255, 0,
            0xff // -1
    };
    private static final int[] ND_001 = { // SONAR                    1992-01-27 17:33:23
            192, 200, 0, 0, 62, 16, 64, 156, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_002 = { // KOUKOU                   1992-01-27 17:57:44
            254, 221, 1, 8, 55, 12, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_003 = { // MEGIDO                   1992-01-27 17:35:47
            192, 200, 0, 0, 54, 16, 16, 39, 13, 255, 0,
            6, 221, 1, 0, 54, 16, 16, 39, 0, 127, 0,
            192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 248,
            0xff // -1
    };
    private static final int[] ND_004 = { // JINARI                   1992-01-27 17:36:37
            254, 221, 1, 31, 54, 14, 16, 39, 0, 128, 113,
            0xff // -1
    };
    private static final int[] ND_005 = { // SWITCH                   1992-01-27 17:37:21
            6, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0,
            6, 20, 0, 0, 54, 16, 160, 15, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_006 = { // DOSYUUNN                 1992-01-27 17:38:01
            6, 221, 1, 0, 54, 16, 16, 39, 0, 127, 0,
            192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 24,
            0xff // -1
    };
    private static final int[] ND_007 = { // GYUOON                   1992-01-27 17:39:09
            192, 232, 3, 31, 54, 16, 96, 234, 0, 252, 0,
            0xff // -1
    };
    private static final int[] ND_008 = { // PIPIPIPI                 1992-01-27 17:40:16
            64, 150, 0, 0, 62, 16, 176, 4, 8, 0, 0,
            0xff // -1
    };
    private static final int[] ND_009 = { // SYUBATTU                 1992-01-27 17:41:16
            12, 221, 1, 0, 55, 16, 232, 3, 13, 0, 20,
            24, 221, 1, 15, 55, 16, 64, 31, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_010 = { // BEAM UNARI               1992-01-27 17:42:05
            254, 25, 0, 0, 54, 14, 16, 39, 0, 0, 145,
            0xff // -1
    };
    private static final int[] ND_011 = { // BEAM KAKUSAN             1992-01-27 17:43:07
            6, 221, 1, 15, 55, 16, 160, 15, 0, 0, 0,
            192, 208, 7, 0, 54, 16, 96, 234, 0, 248, 0,
            0xff // -1
    };
    private static final int[] ND_012 = { // ORGAN                    1992-01-27 18:01:45
            48, 221, 1, 0, 62, 14, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_013 = { // PANEL                    1992-01-27 17:57:15
            6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0,
            6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0,
            6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0,
            6, 221, 1, 4, 55, 16, 160, 15, 0, 0, 0,
            24, 20, 0, 10, 54, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_014 = { // DRILL                    1992-01-27 17:45:25
            254, 160, 15, 31, 54, 15, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_015 = { // PRAZMA                   1992-01-27 17:45:59
            6, 20, 0, 15, 55, 16, 112, 23, 0, 0, 0,
            6, 20, 0, 0, 54, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_016 = { // BABEL                    1992-01-27 17:46:34
            254, 160, 15, 0, 62, 16, 16, 39, 14, 0, 0,
            0xff // -1
    };
    private static final int[] ND_017 = { // ELEVETOR                 1992-01-27 17:47:27
            12, 233, 1, 0, 54, 14, 16, 39, 0, 255, 0,
            254, 221, 1, 0, 54, 14, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_018 = { // MEGIDO HASSYA            1992-01-27 17:48:04
            254, 160, 15, 15, 54, 15, 16, 39, 13, 0, 0,
            0xff // -1
    };
    private static final int[] ND_019 = { // DAIBAKUHATU              1992-01-27 18:28:56
            12, 221, 1, 31, 54, 16, 16, 39, 0, 127, 0,
            144, 0, 0, 0, 54, 16, 96, 234, 0, 127, 24,
            192, 160, 15, 31, 54, 16, 80, 70, 14, 0, 0,
            0xff // -1
    };
    private static final int[] ND_020 = { // NAMI                     1992-01-27 17:50:59
            254, 221, 1, 0, 55, 16, 16, 39, 14, 0, 0,
            0xff // -1
    };
    private static final int[] ND_021 = { // DOOOONN                  1992-01-27 17:51:39
            96, 208, 7, 0, 54, 16, 16, 39, 0, 40, 0,
            0xff // -1
    };
    private static final int[] ND_022 = { // DOGA                     1992-01-27 17:52:18
            6, 221, 1, 31, 54, 16, 16, 39, 0, 127, 0,
            12, 221, 1, 0, 55, 16, 160, 15, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_023 = { // PISI                     1992-01-27 17:52:53
            6, 20, 0, 31, 54, 16, 16, 39, 0, 0, 0,
            24, 20, 0, 0, 54, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_024 = { // BLUE WATER               1992-01-27 17:53:15
            254, 15, 0, 0, 62, 14, 16, 39, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_025 = { // HOWAWAN                  1992-01-27 17:56:51
            12, 144, 1, 0, 62, 16, 100, 0, 13, 254, 0,
            12, 134, 1, 0, 62, 16, 100, 0, 13, 254, 0,
            12, 124, 1, 0, 62, 16, 100, 0, 13, 254, 0,
            12, 114, 1, 0, 62, 16, 100, 0, 13, 254, 0,
            48, 90, 1, 0, 62, 16, 16, 39, 0, 254, 0,
            0xff // -1
    };
    private static final int[] ND_026 = { // ZUGAN                    1992-01-27 17:19:49
            6, 221, 1, 31, 55, 16, 160, 15, 0, 0, 0,
            64, 221, 1, 24, 55, 16, 32, 78, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_027 = { // DAAANN                   1992-01-27 17:20:28
            48, 221, 1, 31, 55, 16, 152, 58, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_028 = { // DOGOOOONN                1992-01-27 17:21:14
            6, 221, 1, 1, 54, 16, 16, 39, 0, 127, 0,
            192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_029 = { // GASYA                    1992-01-27 17:22:08
            3, 221, 1, 15, 55, 16, 208, 7, 0, 0, 0,
            12, 221, 1, 1, 55, 16, 160, 15, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_030 = { // BASYUSYUSYU              1992-01-27 17:22:52
            3, 221, 1, 15, 55, 15, 16, 39, 0, 0, 0,
            192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 113,
            0xff // -1
    };
    private static final int[] ND_031 = { // DOSYUSYUSYU              1992-01-27 17:24:31
            192, 0, 0, 0, 54, 16, 96, 234, 0, 128, 17,
            0xff // -1
    };
    private static final int[] ND_032 = { // SYUSYUUUUNN              1992-01-27 17:25:34
            12, 221, 1, 0, 55, 15, 16, 39, 0, 0, 113,
            32, 221, 1, 0, 55, 16, 32, 78, 0, 0, 17,
            0xff // -1
    };
    private static final int[] ND_033 = { // BASYANN - HYURURURU      1992-01-27 18:00:33
            6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 193,
            32, 221, 1, 4, 55, 16, 16, 39, 0, 0, 0,
            192, 0, 0, 0, 54, 16, 96, 234, 0, 1, 0,
            0xff // -1
    };
    private static final int[] ND_034 = { // ZYURUZYURU               1992-01-27 17:27:38
            192, 221, 1, 0, 55, 16, 96, 234, 0, 0, 113,
            0xff // -1
    };
    private static final int[] ND_035 = { // ZUGOGOGOGO               1992-01-27 17:29:07
            6, 221, 1, 15, 55, 16, 16, 39, 0, 0, 0,
            6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241,
            6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241,
            6, 221, 1, 31, 55, 16, 16, 39, 0, 0, 241,
            192, 221, 1, 31, 55, 16, 96, 234, 0, 0, 248,
            0xff // -1
    };
    private static final int[] ND_036 = { // ZUGOOOONN                1992-01-27 17:29:50
            6, 221, 1, 15, 55, 16, 16, 39, 0, 0, 0,
            192, 221, 1, 31, 55, 16, 48, 117, 0, 0, 0,
            0xff // -1
    };
    private static final int[] ND_037 = { // BI--                     1992-01-27 17:59:08
            48, 40, 0, 0, 62, 16, 100, 0, 8, 0, 0,
            0xff // -1
    };
    private static final int[] ND_038 = { // BASYUSYUUU               1992-01-27 17:30:38
            48, 221, 1, 0, 55, 16, 16, 39, 0, 0, 145,
            0xff // -1
    };
    private static final int[] ND_039 = { // BISYU                    1992-01-27 17:31:52
            6, 232, 3, 15, 54, 16, 16, 39, 0, 127, 0,
            24, 221, 1, 0, 55, 16, 16, 39, 0, 0, 0,
            0xff // -1
    };

    public MmlDatum cmd;
    public LinePos clp;
    public int currentWriter;

//#endregion
}
