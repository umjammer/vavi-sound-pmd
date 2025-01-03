package pmd.player;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.IOException;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.StopWatch;
import dotnet4j.util.compat.TriFunction;
import dotnet4j.util.compat.Tuple;
import mdsound.Instrument;
import mdsound.MDSound;
import mdsound.instrument.P86Inst;
import mdsound.instrument.PpsDrvInst;
import mdsound.instrument.Ppz8Inst;
import mdsound.instrument.Ym2608Inst;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import pmd.common.Environment;
import pmd.common.PmdException;
import pmd.driver.Driver;
import pmd.driver.PMDDotNETOption;

import static hal8999.emu.ui.Sound80.volume;
import static java.lang.System.getLogger;


class Program {

    private static final Logger logger = getLogger(Program.class.getName());

    private static SourceDataLine audioOutput = null;

    public interface naudioCallBack extends TriFunction<short[], Integer, Integer, Integer> {

    }

    private static naudioCallBack callBack = null;
    private static Thread trdMain = null;
    private static StopWatch sw = null;
    private static double swFreq = 0;
    public static boolean trdClosed = false;
    private static Object lockObj = new Object();
    private static boolean _trdStopped = true;
    static boolean trdStopped;

    public static boolean getTrdStopped() {
        synchronized (lockObj) {
            return _trdStopped;
        }
    }

    public static void setTrdStopped(boolean value) {
        synchronized (lockObj) {
            _trdStopped = value;
        }
    }

    private static final int SamplingRate = 55467; // 44100;
    private static final int SamplingRatePPSGIMIC = 44100;
    private static final int SamplingRatePPSSCCI = 16000;
    private static final int samplingBuffer = 1024;
    private static short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static short[] emuRenderBuf = new short[2];
    private static IDriver drv = null;
    private static final int opnaMasterClock = 7987200;
    private static int device = 0;
    private static int loop = 0;
//    private static NScci.NScci nScci;
//    private static Nc86ctl.Nc86ctl nc86ctl;
//    private static RSoundChip rsc;

    private static boolean isAUTO = true;
    private static boolean isNRM = true;
    private static boolean isSPB = true;
    private static boolean isVA = false;
    private static boolean usePPS = false;
    private static boolean usePPZ = false;
    private static int[] VolumeV = null;
    private static int[] VolumeR = null;
    private static boolean isGimicOPNA = false;
    private static Ppz8Inst ppz8em = null;
    private static PpsDrvInst ppsdrv = null;
    private static P86Inst p86em = null;
    private static String[] envPmd = null;
    private static String[] envPmdOpt = null;
    private static String srcFile = null;
    private static int userPPSFREQ = -1;
    private static int ppsdrvWait = 1;

    public static void main(String[] args) {
        int fnIndex = AnalyzeOption(args);
        int mIndex = -1;

        if (args != null) {
            for (int i = fnIndex; i < args.length; i++) {
                if ((!Path.getExtension(args[i]).toUpperCase().contains(".M"))
                        && (!Path.getExtension(args[i]).toUpperCase().contains(".XML"))
                ) continue;
                mIndex = i;
                break;
            }
        }

        if (mIndex < 0) {
            logger.log(Level.INFO, "引数(.Mファイル)１個欲しいよぉ...");
            return;
        }

        srcFile = args[mIndex];

        if (!File.exists(args[mIndex])) {
            logger.log(Level.ERROR, String.format("ファイル[%d]が見つかりません", args[mIndex]));
            return;
        }

//        rsc = CheckDevice();

        try {

//            SineWaveProvider16 waveProvider;
            int latency = 1000;

            switch (device) {
                case 0:
//                    waveProvider = new SineWaveProvider16();
//                    waveProvider.SetWaveFormat((int) SamplingRate, 2);
                    callBack = Program::EmuCallback;
                    audioOutput = AudioSystem.getSourceDataLine(new AudioFormat(SamplingRate, 16, 2, true, false));;
                    audioOutput.open();
                    volume(audioOutput, Double.parseDouble(System.getProperty("mdm.volume", "0.2")));
                    audioOutput.start();
                    break;
                case 1:
                case 2:
                    trdMain = new Thread(Program::RealCallback);
                    trdMain.setPriority(Thread.MAX_PRIORITY);
                    trdMain.setDaemon(true);
                    trdMain.setName("trdVgmReal");
                    sw = StopWatch.startNew();
                    swFreq = StopWatch.Frequency;
                    break;
            }

            MDSound.Chip chip = new MDSound.Chip();
            chip.id = 0;
            Ym2608Inst ym2608 = Instrument.getInstrument(Ym2608Inst.class);
            chip.instrument = ym2608;
            chip.samplingRate = SamplingRate;
            chip.clock = opnaMasterClock;
            chip.volume = 0;
            chip.option = new Object[] {GetApplicationFolder()};

            MDSound.Chip chipp = new MDSound.Chip();
            //type = MDSound.MDSound.enmInstrumentType.PPZ8,
            chipp.id = 0;
            ppz8em = Instrument.getInstrument(Ppz8Inst.class);
            chipp.instrument = ppz8em;
            chipp.samplingRate = SamplingRate;
            chipp.clock = opnaMasterClock;
            chipp.volume = 0;
            chipp.option = null;

            MDSound.Chip chipps = new MDSound.Chip();
//                type = MDSound.MDSound.enmInstrumentType.PPSDRV,
            chipps.id = 0;
            ppsdrv = Instrument.getInstrument(PpsDrvInst.class);
            chipps.instrument = ppsdrv;
            chipps.samplingRate = device == 0
                    ? SamplingRate
                    : (userPPSFREQ == -1
                    ? device == 1
                    ? SamplingRatePPSGIMIC
                    : SamplingRatePPSSCCI
                    : userPPSFREQ
            );
            chipps.clock = opnaMasterClock;
            chipps.volume = 0;
            chipps.option = device == 0 ? null : (new Object[] {(BiConsumer<Integer, Integer>) Program::PPSDRVpsg});

            MDSound.Chip chip86 = new MDSound.Chip();
            chip86.id = 0;
            p86em = Instrument.getInstrument(P86Inst.class);
            chip86.instrument = p86em;
            chip86.samplingRate = SamplingRate;
            chip86.clock = opnaMasterClock;
            chip86.volume = 0;
            chip86.option = null;

            mds = new MDSound(SamplingRate, samplingBuffer, new MDSound.Chip[] {chip, chipp, chipps, chip86});
            //ppz8em = new PPZ8em(SamplingRate);
            //ppsdrv = new PPSDRV(SamplingRate);

            Environment env = new Environment();
            env.AddEnv("pmd");
            env.AddEnv("pmdopt");
            envPmd = env.GetEnvVal("pmd");
            envPmdOpt = env.GetEnvVal("pmdopt");

            List<String> opt = new ArrayList<>((envPmdOpt == null) ? (new ArrayList<>()) : List.of(envPmdOpt));
            opt.addAll(Arrays.asList(args).subList(fnIndex, args.length));
            mIndex += (envPmdOpt == null ? 0 : envPmdOpt.length) - fnIndex;

//#if NETCOREAPP
//            System.Text.Encoding.RegisterProvider(System.Text.CodePagesEncodingProvider.Instance);
//#endif
            drv = new Driver();
            PMDDotNETOption dop = new PMDDotNETOption();
            dop.isAUTO = isAUTO;
            dop.isNRM = isNRM;
            dop.isSPB = isSPB;
            dop.isVA = isVA;
            dop.usePPS = usePPS;
            dop.usePPZ = usePPZ;
            dop.isLoadADPCM = false;
            dop.loadADPCMOnly = false;
            //dop.ppz8em = ppz8em;
            //dop.ppsdrv = ppsdrv;
            dop.envPmd = envPmd;
            dop.srcFile = srcFile;
            dop.jumpIndex = -1; // -1;
            List<String> pop = new ArrayList<>();
            boolean pmdvolFound = false;
            for (int i = 0; i < opt.size(); i++) {
                if (i == mIndex) continue;
                String op = opt.get(i).toUpperCase().trim();
                pop.add(op);
                if (op.contains("-D") || op.contains("/D"))
                    pmdvolFound = true;
            }

            logger.log(Level.INFO, "");

            ((Driver) drv).init(
                    srcFile
                    , Program::OPNAWrite
                    , Program::OPNAWaitSend
                    , dop
                    , pop.toArray(String[]::new)
                    , Program::appendFileReaderCallback
                    , Program::PPZ8Write
                    , Program::PPSDRVWrite
                    , Program::P86Write
            );


            //AUTO指定の場合に構成が変わるので、構成情報を受け取ってから音量設定を行う
            isNRM = dop.isNRM;
            isSPB = dop.isSPB;
            isVA = dop.isVA;
            usePPS = dop.usePPS;
            usePPZ = dop.usePPZ;
            String[] pmdOptionVol = SetVolume();
            //ユーザーがコマンドラインでDオプションを指定していない場合はpmdVolを適用させる
            if (!pmdvolFound && pmdOptionVol != null && pmdOptionVol.length > 0) {
                ((Driver) drv).resetOption(pmdOptionVol);//
            }


            List<Tuple<String, String>> tags = drv.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (Objects.equals(tag.getItem1(), "")) continue;
                    WriteLine2(Level.INFO, String.format("%-16s : %s", tag.getItem1(), tag.getItem2()), 16 + 3);
                }
            }

            logger.log(Level.INFO, "");

            drv.startRendering(SamplingRate, new Tuple<>("YM2608", opnaMasterClock));

            drv.startMusic(0);

            switch (device) {
                case 0:
                    audioOutput.start();
                    break;
                case 1:
                case 2:
                    trdMain.start();
                    break;
            }

            logger.log(Level.INFO, "演奏を終了する場合は何かキーを押してください(実chip時は特に。)");

            while (true) {
                Thread.sleep(1);
                if (System.in.available() != 0) {
                    break;
                }
                //ステータスが0(終了)又は0未満(エラー)の場合はループを抜けて終了
                if (drv.getStatus() <= 0) {
                    if (drv.getStatus() == 0) {
                        Thread.sleep((int) (latency * 2.0)); // 実際の音声が発音しきるまでlatency*2の分だけ待つ
                    }
                    break;
                }

                if (loop != 0 && drv.getNowLoopCounter() > loop) {
                    Thread.sleep((int) (latency * 2.0)); // 実際の音声が発音しきるまでlatency*2の分だけ待つ
                    break;
                }
            }

            drv.stopMusic();
            drv.stopRendering();
            ((Driver) drv).dispStatus();
        } catch (PmdException pe) {
            logger.log(Level.ERROR, pe.getMessage());
        } catch (Exception ex) {
            logger.log(Level.ERROR, "演奏失敗");
            logger.log(Level.ERROR, ex.getMessage(), ex);
        } finally {
            if (((Driver) drv).renderingException != null) {
                logger.log(Level.ERROR, "演奏失敗");
                logger.log(Level.ERROR, ((Driver) drv).renderingException.getMessage(), ((Driver) drv).renderingException);
            }

            if (audioOutput != null) {
                audioOutput.stop();
                while (audioOutput.isRunning()) {
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                }
                audioOutput.close();
                audioOutput = null;
            }
            if (trdMain != null) {
                trdClosed = true;
                while (!trdStopped) {
                    try { Thread.sleep(1); } catch (InterruptedException e) {}
                }
            }
//            if (nc86ctl != null) {
//                nc86ctl.deinitialize();
//                nc86ctl = null;
//            }
//            if (nScci != null) {
//                nScci.Dispose();
//                nScci = null;
//            }
        }
    }

    private static String[] SetVolume() {
        List<String> ret = new ArrayList<>();

        if (device == 0 || device == 3) { //EMU or wav
            //fmgen向け設定
            //fm:ssg = 1:0.25で調整
            //
            //  pmd内で1:(0.45～0.50)に補正される
            //  ・OPNの場合のみpmdのコード上でfmの音量を下げるコードを通過する
            //  ・GIMIC ProとLiteのターミナルでも mファイルを再生し確認
            VolumeV = new int[] {0, 0, 0, 0};
            if (isNRM) {
                //PC98のOPNを想定
                VolumeV[0] = 12; // FM  98は88よりFMが大きい
                VolumeV[1] = -5; // SSG
                VolumeV[2] = -191; // Rhythm
                VolumeV[3] = -191; // Adpcm
            } else {
                //OPNA(-86/SPB)を想定
                VolumeV[0] = 0; // FM
                VolumeV[1] = -5; // SSG
                VolumeV[2] = 0; // Rhythm //未調査
                VolumeV[3] = 0; // Adpcm //未調査
            }
        } else if (device == 1) { //GIMIC
            if (VolumeR == null) {
                VolumeR = new int[] {0};
                if (isNRM)
                    VolumeR[0] = 31; // GMC-OPNA に31を送信
                else
                    VolumeR[0] = 66; // GMC-OPNA に66を送信
            }

            //GMC-OPNA以外のOPNA系モジュール
            if (!isGimicOPNA) {
                //pmdのオプションで調整
                ret.add("/DF12");
                ret.add("/DS0");
            }
        } else if (device == 2) { //SCCI
            //SCCIの場合はバランス調整はユーザー任せ
        }

        //一度目の音量設定時は反映を行わない
        if (VolumeV != null) {
//            mds.setVolumeYM2608FM(VolumeV[0]);
//            mds.setVolumeYM2608PSG(VolumeV[1]);
//            mds.setVolumeYM2608Rhythm(VolumeV[2]);
//            mds.setVolumeYM2608Adpcm(VolumeV[3]);
        }

        if (VolumeR != null) {
            if (isGimicOPNA) { //GMC-OPNA
//                rsc.setSSGVolume((byte) VolumeR[0]);
                // 少し休む(即再生を始めると音が飛ぶ)
                try { Thread.sleep(500); } catch (InterruptedException e) {}
            }
        }

        return ret.toArray(String[]::new);
    }

    public static String GetApplicationFolder() {
        String path = Path.getDirectoryName(System.getProperty("user.home"));
        if (path != null && !path.isEmpty()) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    static void WriteLine2(Level level, String msg, int wrapPos /* = 0 */) {
        if (wrapPos == 0) {
            logger.log(level, msg);
        } else {
            String[] mes = msg.split("\r\n");
            logger.log(level, mes[0]);
            for (int i = 1; i < mes.length; i++) {
                logger.log(level, String.format("%s%s", " ".repeat(wrapPos), mes[i]));
            }
        }
    }

    private static Stream appendFileReaderCallback(String arg) {
        String fn;
        fn = Path.combine(Path.getDirectoryName(srcFile), arg);

        if (envPmd != null) {
            int i = 0;
            while (!File.exists(fn) && i < envPmd.length) {
                fn = Path.combine(envPmd[i++], arg);
            }
        }

        FileStream strm;
        try {
            strm = new FileStream(fn, FileMode.Open, FileAccess.Read, FileShare.Read);
        } catch (IOException e) {
            strm = null;
        }

        return strm;
    }

    private static int AnalyzeOption(String[] args) {
        if (args == null || args.length < 1) return 0;

        int i = 0;
        device = 0;
        loop = 0;

        while (i < args.length && args[i] != null && !args[i].isEmpty() && (args[i].charAt(0) == '-' || args[i].charAt(0) == '/')) {
            String op = args[i].substring(1).toUpperCase();
            if (op.equals("D=EMU")) device = 0;
            else if (op.equals("D=GIMIC")) device = 1;
            else if (op.equals("D=SCCI")) device = 2;
            else if (op.equals("D=WAVE")) device = 3;
            else if (op.length() > 2 && op.startsWith("L=")) OptionSetLoop(op);
            else if (op.equals("H") || op.equals("?")) OptionDispHelp();
            else if (op.length() > 2 && op.startsWith("B=")) OptionSetBoard(op.substring(2));
            else if (op.length() > 3 && op.startsWith("VV=")) OptionSetVolumeV(op.substring(3));
            else if (op.length() > 3 && op.startsWith("VR=")) OptionSetVolumeR(op.substring(3));
            else if (op.length() > 4 && op.startsWith("PPS=")) OptionSetPPS(op.substring(4));
            else if (op.length() > 4 && op.startsWith("PPZ=")) OptionSetPPZ(op.substring(4));
            else if (op.length() > 8 && op.startsWith("PPSFREQ=")) OptionSetPPSFREQ(op.substring(8));
            else if (op.length() > 8 && op.startsWith("PPSWAIT=")) OptionSetPPSWAIT(op.substring(8));
            else break;

            i++;
        }

        if (device == 3 && loop == 0) loop = 1; // wave出力の場合、無限ループは1に変更
        return i;
    }

    private static void OptionSetPPZ(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            usePPZ = n != 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetPPS(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            usePPS = n != 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetPPSFREQ(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            userPPSFREQ = Math.min(Math.max(n, 2000), 192000);
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetPPSWAIT(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            ppsdrvWait = Math.min(Math.max(n, -1), 100);
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetLoop(String op) {
        try {
            loop = Integer.parseInt(op.substring(2));
            loop = 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetVolumeR(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            VolumeR = new int[] {Math.min(Math.max(n, 0), 127)};
        } catch (NumberFormatException e) {
        }
    }

    private static void OptionSetVolumeV(String v) {
        if (v == null || v.isEmpty()) return;
        String[] prm = v.split(",");
        if (prm == null || prm.length < 1) return;
        VolumeV = new int[] {
                0, 0, 0, 0
        };
        for (int i = 0; i < prm.length; i++) {
            try {
                VolumeV[i] = Integer.parseInt(prm[i]);
                VolumeV[i] = Math.min(Math.max(VolumeV[i], -191), 20);
            } catch (NumberFormatException e) {
            }
        }
    }

    private static void OptionSetBoard(String v) {
        if (v == null || v.isEmpty()) return;
        if (v.equals("AUTO")) {
            isAUTO = true;
        } else if (v.equals("NRM") || v.equals("OPN") || v.equals("2203") || v.equals("26")) {
            isAUTO = false;
            isNRM = true;
            isVA = false;
            isSPB = false;
        } else if (v.equals("86") || v.equals("86B")) {
            isAUTO = false;
            isNRM = false;
            isVA = false;
            isSPB = false;
        } else if (v.equals("SPB") || v.equals("OPNA") || v.equals("2608")) {
            isAUTO = false;
            isNRM = false;
            isVA = false;
            isSPB = true;
        } else if (v.equals("VA_NRM")) {
            isAUTO = false;
            isNRM = true;
            isVA = true;
            isSPB = false;
        } else if (v.equals("VA_86")) {
            isAUTO = false;
            isNRM = false;
            isVA = true;
            isSPB = false;
        }
    }

    private static void OptionDispHelp() {
        logger.log(Level.INFO, """
                Welcome to PMDDotNET !
                
                 Usage
                   PMDDotNETPlayer.exe  [-[H|?]] [-D=[EMU|GIMIC|SCCI|WAVE]] [-L=n] [-B=[NRM|OPN|2203|26|86|SPB|86B|OPNA|2608|VA_NRM|VA_86]] [-VV=n,n,n,n] [-VR=n] [
                            PMD options] [file.m]
                
                 Options
                  オプションは大文字小文字を区別しません。
                
                   -D=
                     -D=オプションを指定することにより再生デバイスを変更できます。
                       -D=EMU
                         デフォルト値です。
                         エミュレーションによる再生をWindowsの音声デバイスから行います。
                       -D=GIMIC
                         G.I.M.I.CのOPNAモジュールによる再生を行います。モジュールが見つからない場合はEMUと同じ動作になります。
                       -D=SCCI
                         SCCIのOPNAモジュールによる再生を行います。モジュールが見つからない場合はEMUと同じ動作になります。
                
                   -L=n
                     ループ回数を0以上の数値で指定します。(TBD)
                     但し0は無限ループになります。デフォルト値は0です。
                     ループ回数というオプションですが実際は演奏回数です。つまり1を指定した場合、一通り演奏するとループせずに終了します。
                     解析できない数値を指定した場合は0(無限ループ)となります。
                     再生デバイスがWAVEの場合に0を指定した場合は1に修正されます。
                
                   -B=
                     想定する音源ボードを指定します。
                     PMDの振る舞いが変わるほか、エミュレーションや実チップに設定するボリューム値も設定します。
                     ボリューム値については後述の-VV=などにて変更可能です。
                       -B=AUTO
                         デフォルト値です。
                         -B= -PPS= -PPZ=のオプションが自動で設定されます。
                         設定は曲データ中タグのPCMファイル指定状況から判断されます。以下の通りです。
                
                             #    .PPC(ヘッダ)  .PPS    .PZI       自動設定オプション
                             ------------------------------------------------------------
                             01   未使用        未使用  未使用     -B=SPB -PPS=0 -PPZ=0
                             02   .PPC/.PVI     未使用  未使用     -B=SPB -PPS=0 -PPZ=0
                             03   .P86          未使用  未使用     -B=86B -PPS=0 -PPZ=0
                             04   未使用          使用  未使用     -B=SPB -PPS=1 -PPZ=0
                             05   .PPC/.PVI       使用  未使用     -B=SPB -PPS=1 -PPZ=0
                             06   .P86            使用  未使用     -B=86B -PPS=1 -PPZ=0
                             07   未使用        未使用    使用     -B=SPB -PPS=0 -PPZ=1
                             08   .PPC/.PVI     未使用    使用     -B=SPB -PPS=0 -PPZ=1
                             09   .P86          未使用    使用     -B=86B -PPS=0 -PPZ=1
                             10   未使用          使用    使用     -B=SPB -PPS=1 -PPZ=1
                             11   .PPC/.PVI       使用    使用     -B=SPB -PPS=1 -PPZ=1
                             12   .P86            使用    使用     -B=86B -PPS=1 -PPZ=1
                
                       -B=NRM|OPN|2203|26
                         ノーマル音源(OPN)を指定します。
                         以下のオプションが暗黙で指定されます。
                           -VV=12,-5,-191,-191
                         GIMIC GMC-OPNAの場合
                           -VR=31
                         GIMIC GMC-OPNA以外のモジュールの場合(PMDのオプション)
                           -DF12 -DS0
                         SCCIの場合(PMDのオプション)
                           -DF1 -DS0
                       -B=86|86B|SPB|OPNA|2608
                         拡張音源(OPNA)を指定します。
                         SPB/OPNA/2608を指定するとADPCMを利用します。(PMDB2相当)
                         .PPC/.PVIファイルが指定されている場合は再生前にADPCMデータを転送する処理が発生します。
                         (エミュレーションの場合以外は転送に時間がかかります。)
                         以下のオプションが暗黙で指定されます。
                           -VV=0,-5,0,0
                         GIMIC GMC-OPNAの場合
                           -VR=66
                         GIMIC GMC-OPNA以外のモジュールの場合(PMDのオプション)
                           -DF12 -DS0
                         SCCIの場合(PMDのオプション)(TBD)
                           -DF1 -DS0
                       -B=VA_NRM
                         PC-88VAノーマル音源を指定します。(TBD)
                         以下のオプションが暗黙で指定されます。(TBD)
                           -VV=0,0,0,0
                         GIMIC GMC-OPNAの場合(TBD)
                           -VR=31
                         GIMIC GMC-OPNA以外のモジュール又はSCCIの場合(PMDのオプション)(TBD)
                           -DFn -DSn -DRn -DPn -DZn
                       -B=VA_86
                         PC-88VA拡張音源を指定します。(TBD)
                         以下のオプションが暗黙で指定されます。(TBD)
                           -VV=0,0,0,0
                         GIMIC GMC-OPNAの場合(TBD)
                           -VR=31
                         GIMIC GMC-OPNA以外のモジュール又はSCCIの場合(PMDのオプション)(TBD)
                           -DFn -DSn -DRn -DPn -DZn
                
                   -VV=n,n,n,n
                     エミュレーション向けボリューム値を設定します。
                     カンマ区切りでFM,SSG,Rhythm,Adpcmの順に音量を指定します。
                     nの指定可能範囲は-191～20です。
                
                   -VR=n
                     実チップ向けボリューム値を設定します。
                     実質、GIMICのOPNAモジュール専用オプションで、SSGの音量を0～127で指定します。
                
                   -PPS=n
                     PPSDRVを使用するときは1を指定します。0を指定すると使用しません。
                     デフォルト値は0です。
                     nの指定可能値は0または1です。
                
                   -PPZ=n
                     PPZ8を使用するときは1を指定します。0を指定すると使用しません。
                     デフォルト値は0です。
                     nの指定可能値は0または1です。
                
                   -PPSFREQ=n
                     PPSDRVの周波数(Hz)を指定します。
                     実Chipのみ有効です。
                     デフォルト値はGIMICは44100、SCCIは16000です。
                     nの指定可能値は2000～192000です。
                
                   -PPSWAIT=n
                     SCCIへ送信する同期の為のウエイト値を指定します。
                     SCCIのみ有効です。
                     デフォルト値は1です。
                     -1の場合は送信しません。
                     nの指定可能値は-1～100です。
                
                   [PMD options]
                     オリジナルのPMDへ送るオプションを指定します。
                     実際には上記以外のオプションや、ファイル名を指定すると全てオリジナルのPMDへ指定したものと解釈されます。
                
                   [file.m]
                     .mファイルを指定します。拡張子のチェックはしません。
                """);
    }

    private static void OPNAWaitSend(long elapsed, int size) {
        switch (device) {
            case 0: // EMU
                return;
            case 1: // GIMIC

                //サイズと経過時間から、追加でウエイトする。
                int m = Math.max((int) (size / 20 - elapsed), 0); // 20 閾値(magic number)
                try { Thread.sleep(m); } catch (InterruptedException e) {}

                //ポートも一応見る
//                int n = nc86ctl.getNumberOfChip();
//                for (int i = 0; i < n; i++) {
//                    NIRealChip rc = nc86ctl.getChipInterface(i);
//                    if (rc != null) {
//                        while ((rc. @in(0x0) &0x83) !=0)
//                        Thread.sleep(0);
//                        while ((rc. @in(0x100) &0xbf) !=0)
//                        Thread.sleep(0);
//                    }
//                }

                break;
            case 2: // SCCI
//                nScci.NSoundInterfaceManager_.sendData();
//                while (!nScci.NSoundInterfaceManager_.isBufferEmpty()) {
//                    Thread.sleep(0);
//                }
                break;
        }
    }

//    private static RSoundChip CheckDevice() {
//        SChipType ct = null;
//        int iCount = 0;
//
//        switch (device) {
//            case 1: // GIMIC存在チェック
//                nc86ctl = new Nc86ctl.Nc86ctl();
//                try {
//                    nc86ctl.initialize();
//                    iCount = nc86ctl.getNumberOfChip();
//                } catch
//            {
//                iCount = 0;
//            }
//            if (iCount == 0) {
//                try {
//                    nc86ctl.deinitialize();
//                } catch {
//                }
//                nc86ctl = null;
//                logger.log(Level.ERROR, "Not found G.I.M.I.C");
//                device = 0;
//                break;
//            }
//            for (int i = 0; i < iCount; i++) {
//                NIRealChip rc = nc86ctl.getChipInterface(i);
//                NIGimic2 gm = rc.QueryInterface();
//                ChipType cct = gm.getModuleType();
//                int o = -1;
//                if (cct == ChipType.CHIP_YM2608 || cct == ChipType.CHIP_YMF288 || cct == ChipType.CHIP_YM2203) {
//                    ct = new SChipType();
//                    ct.SoundLocation = -1;
//                    ct.BusID = i;
//                    String seri = gm.getModuleInfo().Serial;
//                    if (!int.TryParse(seri, out o)) {
//                        o = -1;
//                        ct = null;
//                        continue;
//                    }
//                    ct.SoundChip = o;
//                    ct.ChipName = gm.getModuleInfo().Devname;
//                    ct.InterfaceName = gm.getMBInfo().Devname;
//                    isGimicOPNA = (ct.ChipName == "GMC-OPNA");
//                    break;
//                }
//            }
//            RC86ctlSoundChip rsc = null;
//            if (ct == null) {
//                nc86ctl.deinitialize();
//                nc86ctl = null;
//                logger.log(Level.ERROR, "Not found G.I.M.I.C(OPNA module)");
//                device = 0;
//            } else {
//                rsc = new RC86ctlSoundChip(-1, ct.BusID, ct.SoundChip);
//                rsc.c86ctl = nc86ctl;
//                rsc.init();
//
//                rsc.SetMasterClock(7987200); // SoundBoardII
//                rsc.setSSGVolume(63); // PC-8801
//            }
//            return rsc;
//            case 2: // SCCI存在チェック
//                nScci = new NScci.NScci();
//                iCount = nScci.NSoundInterfaceManager_.getInterfaceCount();
//                if (iCount == 0) {
//                    nScci.Dispose();
//                    nScci = null;
//                    logger.log(Level.ERROR, "Not found SCCI.");
//                    device = 0;
//                    break;
//                }
//                for (int i = 0; i < iCount; i++) {
//                    NSoundInterface iIntfc = nScci.NSoundInterfaceManager_.getInterface(i);
//                    NSCCI_INTERFACE_INFO iInfo = nScci.NSoundInterfaceManager_.getInterfaceInfo(i);
//                    int sCount = iIntfc.getSoundChipCount();
//                    for (int s = 0; s < sCount; s++) {
//                        NSoundChip sc = iIntfc.getSoundChip(s);
//                        int t = sc.getSoundChipType();
//                        if (t == 1) {
//                            ct = new SChipType();
//                            ct.SoundLocation = 0;
//                            ct.BusID = i;
//                            ct.SoundChip = s;
//                            ct.ChipName = sc.getSoundChipInfo().cSoundChipName;
//                            ct.InterfaceName = iInfo.cInterfaceName;
//                            break scciExit;
//                        }
//                    }
//                }
//scciExit:
//                ;
//                RScciSoundChip rssc = null;
//                if (ct == null) {
//                    nScci.Dispose();
//                    nScci = null;
//                    logger.log(Level.ERROR, "Not found SCCI(OPNA module).");
//                    device = 0;
//                } else {
//                    rssc = new RScciSoundChip(0, ct.BusID, ct.SoundChip);
//                    rssc.scci = nScci;
//                    rssc.init();
//                }
//                return rssc;
//        }
//
//        return null;
//    }

    private static int EmuCallback(short[] buffer, int offset, int count) {
        try {
            long bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, Program::OneFrame);
                //ppz8em.Update(emuRenderBuf);
                //ppsdrv.Update(emuRenderBuf);

                buffer[offset + i * 2 + 0] = emuRenderBuf[0];
                buffer[offset + i * 2 + 1] = emuRenderBuf[1];

            }
        } catch (Exception ex) {
            logger.log(Level.ERROR, ex.getMessage(), ex);
        }

        return count;
    }

    private static void RealCallback() {

        double o = sw.getElapsedMilliseconds() / swFreq;
        double oPPS = sw.getElapsedMilliseconds() / swFreq;
        double step = 1 / (double) SamplingRate;
        int PPSSamplingRate = (int) (
                userPPSFREQ == -1
                        ? (device == 1 ? SamplingRatePPSGIMIC : SamplingRatePPSSCCI)
                        : (int) userPPSFREQ);
        double stepPPS = 1 / (double) PPSSamplingRate;

        trdStopped = false;
        try {
            while (!trdClosed) {
                Thread.sleep(0);

                double el1 = sw.getElapsedMilliseconds() / swFreq;
                if (el1 - o >= step) {
                    if (el1 - o >= step * SamplingRate / 100.0) // 閾値10ms
                    {
                        do {
                            o += step;
                        } while (el1 - o >= step);
                    } else {
                        o += step;
                    }

                    OneFrame();
                }

                if (el1 - oPPS >= stepPPS) {
                    if (el1 - oPPS >= stepPPS * PPSSamplingRate / 100.0) // 閾値10ms
                    {
                        do {
                            oPPS += stepPPS;
                        } while (el1 - oPPS >= stepPPS);
                    } else {
                        oPPS += stepPPS;
                    }

                    ppsdrv.update(0, null, 1);
                }

            }
        } catch (Exception e) {
        }
        trdStopped = true;
    }

    private static void OneFrame() {
        drv.render();
    }

    private static void OPNAWrite(ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, String.format("! r%d c%d"
                        , md.linePos.row
                        , md.linePos.col
                ));
            }
        }

//#if DEBUG
        //if (dat.address == 0x29)
        //logger.log(Level.INFO, String.format("FM P%d Out:Adr[{0:x02}] val[{1:x02}]", (int)dat.address, (int)dat.data, dat.port));
//#endif

        switch (device) {
            case 0:
                mds.write(Ym2608Inst.class, 0, (byte) dat.port, (byte) dat.address, (byte) dat.data);
                break;
            case 1:
            case 2:
//                rsc.setRegister(dat.port * 0x100 + dat.address, dat.data);
                break;
        }
    }

    private static int PPZ8Write(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x03) {
            return ppz8em.loadPcm(0, (byte) arg.address, (byte) arg.data, (byte[][]) arg.additionalData);
        } else {
            return ppz8em.write(0, arg.port, arg.address, arg.data);
        }
    }

    private static int PPSDRVWrite(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x05) {
            return ppsdrv.load(0, (byte[]) arg.additionalData);
        } else {
            return ppsdrv.write(0, arg.port, arg.address, arg.data);
        }
    }

    //static int aold = -1;
    //static int dold = -1;

    private static void PPSDRVpsg(int a, int d) {
        switch (device) {
            case 0:
                mds.write(Ym2608Inst.class, 0, 0, a, d);
                break;
            case 1:
            case 2:
                //if (aold != a || dold != d)
            {
//                rsc.setRegister(0 * 0x100 + a, d);
//                if (ppsdrvWait > -1) rsc.setRegister(-1, ppsdrvWait);

                //aold = a;
                //dold = d;
            }
            break;
        }
    }

    private static int P86Write(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x00) {
            return p86em.loadPcm(0, (byte) arg.address, (byte) arg.data, (byte[]) arg.additionalData);
        } else {
            return p86em.write(0, arg.port, arg.address, arg.data);
        }
    }

//    public static class SineWaveProvider16 extends WaveProvider16 {
//
//        public SineWaveProvider16() {
//        }
//
//        @Override
//        public int Read(short[] buffer, int offset, int sampleCount) {
//            return callBack(buffer, offset, sampleCount);
//        }
//    }
}
