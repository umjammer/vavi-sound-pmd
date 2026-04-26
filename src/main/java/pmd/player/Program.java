package pmd.player;

import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;

import com.github.kwhat.jnativehook.GlobalScreen;
import com.github.kwhat.jnativehook.NativeHookException;
import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent;
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener;
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
import mdsound.instrument.PpsInst;
import mdsound.instrument.Ppz8Inst;
import mdsound.instrument.Ym2608Inst;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import pmd.common.PmdException;
import pmd.driver.Driver;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;
import static vavi.sound.SoundUtil.volume;


/**
 * system properties
 * <li>{@code pmd.dir} ... separated by {@code ;}</li>
 * <li>{@code pmd.opt} ... separated by {@code ;}</li>
 */
public class Program {

    private static final Logger logger = getLogger(Program.class.getName());

    static class KeyboardHook {
        static final AtomicBoolean typed = new AtomicBoolean();
        static {
            try {
                GlobalScreen.registerNativeHook();
                GlobalScreen.addNativeKeyListener(new NativeKeyListener() {
                    @Override
                    public void nativeKeyTyped(NativeKeyEvent nativeEvent) {
                        typed.set(true);
                    }
                });
            } catch (NativeHookException e) {
                logger.log(Level.WARNING, "There was a problem registering the native hook: " + e.getMessage());
            }
        }
        static boolean kbhit() {
            return typed.get();
        }
    }

    private static SourceDataLine audioOutput = null;

    public interface naudioCallBack extends TriFunction<short[], Integer, Integer, Integer> {

    }

    private static Thread trdMain = null;
    private static StopWatch sw = null;
    private static double swFreq = 0;
    public static boolean trdClosed = false;
    private static final Object lockObj = new Object();
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
    private static final short[] frames = new short[samplingBuffer * 4];
    private static MDSound mds = null;
    private static final short[] emuRenderBuf = new short[2];
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
    private static final boolean isGimicOPNA = false;
    private static Ppz8Inst ppz8em = null;
    private static PpsInst ppsdrv = null;
    private static P86Inst p86em = null;
    private static String[] envPmd = null;
    private static String[] envPmdOpt = null;
    private static String srcFile = null;
    private static int userPPSFREQ = -1;
    private static int ppsdrvWait = 1;

    public static void main(String[] args) {
        int fnIndex = analyzeOption(args);
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
            logger.log(Level.INFO, "ot least one argument is needed (.M file)...");
            return;
        }

        srcFile = args[mIndex];

        if (!File.exists(args[mIndex])) {
            logger.log(Level.ERROR, "File [%s] not found".formatted(args[mIndex]));
            return;
        }

//        rsc = checkDevice();

        try {

            int latency = 1000;

            switch (device) {
                case 0:
                    audioOutput = AudioSystem.getSourceDataLine(new AudioFormat(SamplingRate, 16, 2, true, false));
                    audioOutput.open();
                    volume(audioOutput, Double.parseDouble(System.getProperty("pmd.volume", "0.2")));
                    trdMain = new Thread(Program::emuPlayback);
                    trdMain.setPriority(Thread.MAX_PRIORITY);
                    trdMain.setDaemon(true);
                    trdMain.setName("trdEmu");
                    break;
                case 1:
                case 2:
                    trdMain = new Thread(Program::realCallback);
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
            chip.option = new Object[] {getApplicationFolder()};

            MDSound.Chip chipp = new MDSound.Chip();
            chipp.id = 0;
            ppz8em = Instrument.getInstrument(Ppz8Inst.class);
            chipp.instrument = ppz8em;
            chipp.samplingRate = SamplingRate;
            chipp.clock = opnaMasterClock;
            chipp.volume = 0;
            chipp.option = null;

            MDSound.Chip chipps = new MDSound.Chip();
            chipps.id = 0;
            ppsdrv = Instrument.getInstrument(PpsInst.class);
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
            chipps.option = device == 0 ? null : (new Object[] {(BiConsumer<Integer, Integer>) Program::psgPPSDRV});

            MDSound.Chip chip86 = new MDSound.Chip();
            chip86.id = 0;
            p86em = Instrument.getInstrument(P86Inst.class);
            chip86.instrument = p86em;
            chip86.samplingRate = SamplingRate;
            chip86.clock = opnaMasterClock;
            chip86.volume = 0;
            chip86.option = null;

            mds = new MDSound();
            mds.init(SamplingRate, 1024, List.of(chip, chipp, chip86));
//            ppz8em = new PPZ8em(SamplingRate);
//            ppsdrv = new PPSDRV(SamplingRate);

            envPmd = System.getProperty("pmd.pmd", "").split(";");
            envPmdOpt = System.getProperty("pmd.opt", "").split(";");

            List<String> opt = new ArrayList<>(List.of(envPmdOpt));
            opt.addAll(Arrays.asList(args).subList(fnIndex, args.length));
            mIndex += (envPmdOpt == null ? 0 : envPmdOpt.length) - fnIndex;

            drv = new Driver();

            Object[] dop = {
                    false,
                    false,
                    isAUTO,
                    isVA,
                    isNRM,
                    usePPS,
                    usePPZ,
                    isSPB,
                    envPmd,
                    envPmd,
                    srcFile,
                    "",
                    (Function<String, Stream>) Program::appendFileReaderCallback
            };

            List<String> pop = new ArrayList<>();
            boolean pmdvolFound = false;
            for (int i = 0; i < opt.size(); i++) {
                if (i == mIndex) continue;
                String op = opt.get(i).toUpperCase().trim();
                pop.add(op);
                if (op.contains("-D") || op.contains("/D"))
                    pmdvolFound = true;
            }

            if (!Path.getExtension(srcFile).equalsIgnoreCase(".xml")) { // mml
                byte[] srcBuf = File.readAllBytes(srcFile);
logger.log(Level.INFO, "size: " + srcBuf.length);
                List<MmlDatum> buf = new ArrayList<>();
                for (byte b : srcBuf)
                    buf.add(new MmlDatum(b & 0xff));
                drv.init(null,
                        buf.toArray(MmlDatum[]::new),
                        null,
                        dop,
                        pop.toArray(String[]::new),
                        (Function<ChipDatum, Integer>) Program::writePPZ8,
                        (Function<ChipDatum, Integer>) Program::writePPSDRV,
                        (Function<ChipDatum, Integer>) Program::writeP86,
                        (Consumer<ChipDatum>) Program::writeOPNA,
                        (BiConsumer<Long, Integer>) Program::waitSendOPNA);
            } else { // xml
                try (InputStream sr = Files.newInputStream(java.nio.file.Path.of(srcFile))) {
                    MmlDatum[] s = Serdes.Util.deserialize(sr, new MmlDatum[0]); // TODO
                    drv.init(null,
                            s,
                            null,
                            dop,
                            pop.toArray(String[]::new),
                            (Function<ChipDatum, Integer>) Program::writePPZ8,
                            (Function<ChipDatum, Integer>) Program::writePPSDRV,
                            (Function<ChipDatum, Integer>) Program::writeP86,
                            (Consumer<ChipDatum>) Program::writeOPNA,
                            (BiConsumer<Long, Integer>) Program::waitSendOPNA);
                } catch (java.io.IOException e) {
                    throw new dotnet4j.io.IOException(e);
                }
            }

            // When AUTO is specified, the configuration will change, so the volume will be set after receiving the configuration information.
            String[] pmdOptionVol = setVolume();
            // Apply pmdVol if user does not specify D option on command line
            if (!pmdvolFound && pmdOptionVol != null && pmdOptionVol.length > 0) {
                ((Driver) drv).resetOption(pmdOptionVol);//
            }

            List<Tuple<String, String>> tags = drv.getTags();
            if (tags != null) {
                for (Tuple<String, String> tag : tags) {
                    if (Objects.equals(tag.getItem1(), "")) continue;
                    writeLine2(Level.INFO, "%-16s : %s".formatted(tag.getItem1(), tag.getItem2()), 16 + 3);
                }
            }

            logger.log(Level.INFO, "");

            drv.startRendering(SamplingRate, new Tuple<>("YM2608", opnaMasterClock));

            drv.startMusic(0);

            switch (device) {
                case 0:
                case 1:
                case 2:
                    trdMain.start();
                    break;
            }

            logger.log(Level.INFO, "To end the playback, press any key (especially when playing a real chip).");

            while (true) {
                Thread.sleep(1);
                if (KeyboardHook.kbhit()) {
                    break;
                }
                // If the status is 0 (finished) or less than 0 (error), exit the loop and
                if (drv.getStatus() <= 0) {
                    if (drv.getStatus() == 0) {
                        Thread.sleep((int) (latency * 2.0)); // Wait for latency*2 until the actual voice is fully pronounced
                    }
                    break;
                }

                if (loop != 0 && drv.getNowLoopCounter() > loop) {
                    Thread.sleep((int) (latency * 2.0)); // Wait for latency*2 until the actual voice is fully pronounced
                    break;
                }
            }

            drv.stopMusic();
            drv.stopRendering();
            ((Driver) drv).dispStatus();
        } catch (PmdException pe) {
            logger.log(Level.ERROR, pe.getMessage());
        } catch (Exception ex) {
            logger.log(Level.ERROR, "Failed to play");
            logger.log(Level.ERROR, ex.getMessage(), ex);
        } finally {
            if (((Driver) drv).renderingException != null) {
                logger.log(Level.ERROR, "Failed to play");
                logger.log(Level.ERROR, ((Driver) drv).renderingException.getMessage(), ((Driver) drv).renderingException);
            }

            if (audioOutput != null) {
                audioOutput.stop();
                while (audioOutput.isRunning()) {
                    try { Thread.sleep(1); } catch (InterruptedException _) {}
                }
                audioOutput.close();
                audioOutput = null;
            }
            if (trdMain != null) {
                trdClosed = true;
                while (!trdStopped) {
                    try { Thread.sleep(1); } catch (InterruptedException _) {}
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

    private static String[] setVolume() {
        List<String> ret = new ArrayList<>();

        if (device == 0 || device == 3) { //EMU or wav
            // Settings for fmgen
            // Adjusted at fm : ssg = 1 : 0.25
            //
            //  Corrected to 1:(0.45-0.50) within pmd
            //  ・Only in the case of OPN, pass the code to lower the volume of fm on the code of pmd
            //  ・Check the m-file on the GIMIC Pro and Lite terminals
            VolumeV = new int[] {0, 0, 0, 0};
            if (isNRM) {
                // Assumes PC98 OPN
                VolumeV[0] = 12; // FM The 98 has a louder FM volume than the 88.
                VolumeV[1] = -5; // SSG
                VolumeV[2] = -191; // Rhythm
                VolumeV[3] = -191; // Adpcm
            } else {
                // Assume OPNA(-86/SPB)
                VolumeV[0] = 0; // FM
                VolumeV[1] = -5; // SSG
                VolumeV[2] = 0; // Rhythm // Unexplored
                VolumeV[3] = 0; // Adpcm // Unexplored
            }
        } else if (device == 1) { // GIMIC
            if (VolumeR == null) {
                VolumeR = new int[] {0};
                if (isNRM)
                    VolumeR[0] = 31; // Send 31 to GMC-OPNA
                else
                    VolumeR[0] = 66; // Send 66 to GMC-OPNA
            }

            // OPNA-based modules other than GMC-OPNA
            if (!isGimicOPNA) {
                // Adjust with pmd options
                ret.add("/DF12");
                ret.add("/DS0");
            }
        } else if (device == 2) { //SCCI
            // With SCCI, balance adjustment is left to the user
        }

        // The first volume setting is not reflected.
        if (VolumeV != null) {
//            mds.setVolumeYM2608FM(VolumeV[0]);
//            mds.setVolumeYM2608PSG(VolumeV[1]);
//            mds.setVolumeYM2608Rhythm(VolumeV[2]);
//            mds.setVolumeYM2608Adpcm(VolumeV[3]);
        }

        if (VolumeR != null) {
            if (isGimicOPNA) { //GMC-OPNA
//                rsc.setSSGVolume((byte) VolumeR[0]);
                // Take a short break (if you start playing immediately, the sound will skip)
                try { Thread.sleep(500); } catch (InterruptedException _) {}
            }
        }

        return ret.toArray(String[]::new);
    }

    public static String getApplicationFolder() {
        String path = Path.getDirectoryName(System.getProperty("user.home"));
        if (path != null && !path.isEmpty()) {
            path += path.charAt(path.length() - 1) == '\\' ? "" : "\\";
        }
        return path;
    }

    static void writeLine2(Level level, String msg, int wrapPos /* = 0 */) {
        if (wrapPos == 0) {
            logger.log(level, msg);
        } else {
            String[] mes = msg.split("\r\n");
            logger.log(level, mes[0]);
            for (int i = 1; i < mes.length; i++) {
                logger.log(level, "%s%s".formatted(" ".repeat(wrapPos), mes[i]));
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

    private static int analyzeOption(String[] args) {
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
            else if (op.length() > 2 && op.startsWith("L=")) optionSetLoop(op);
            else if (op.equals("H") || op.equals("?")) optionDispHelp();
            else if (op.length() > 2 && op.startsWith("B=")) optionSetBoard(op.substring(2));
            else if (op.length() > 3 && op.startsWith("VV=")) optionSetVolumeV(op.substring(3));
            else if (op.length() > 3 && op.startsWith("VR=")) optionSetVolumeR(op.substring(3));
            else if (op.length() > 4 && op.startsWith("PPS=")) optionSetPPS(op.substring(4));
            else if (op.length() > 4 && op.startsWith("PPZ=")) optionSetPPZ(op.substring(4));
            else if (op.length() > 8 && op.startsWith("PPSFREQ=")) optionSetPPSFREQ(op.substring(8));
            else if (op.length() > 8 && op.startsWith("PPSWAIT=")) optionSetPPSWAIT(op.substring(8));
            else break;

            i++;
        }

        if (device == 3 && loop == 0) loop = 1; // For wave output, change infinite loop to 1
        return i;
    }

    private static void optionSetPPZ(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            usePPZ = n != 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetPPS(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            usePPS = n != 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetPPSFREQ(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            userPPSFREQ = Math.clamp(n, 2000, 192000);
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetPPSWAIT(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            ppsdrvWait = Math.clamp(n, -1, 100);
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetLoop(String op) {
        try {
            loop = Integer.parseInt(op.substring(2));
            loop = 0;
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetVolumeR(String v) {
        if (v == null || v.isEmpty()) return;
        try {
            int n = Integer.parseInt(v);
            VolumeR = new int[] {Math.clamp(n, 0, 127)};
        } catch (NumberFormatException e) {
        }
    }

    private static void optionSetVolumeV(String v) {
        if (v == null || v.isEmpty()) return;
        String[] prm = v.split(",");
        if (prm == null || prm.length < 1) return;
        VolumeV = new int[] {
                0, 0, 0, 0
        };
        for (int i = 0; i < prm.length; i++) {
            try {
                VolumeV[i] = Integer.parseInt(prm[i]);
                VolumeV[i] = Math.clamp(VolumeV[i], -191, 20);
            } catch (NumberFormatException e) {
            }
        }
    }

    private static void optionSetBoard(String v) {
        if (v == null || v.isEmpty()) return;
        switch (v) {
            case "AUTO" -> isAUTO = true;
            case "NRM", "OPN", "2203", "26" -> {
                isAUTO = false;
                isNRM = true;
                isVA = false;
                isSPB = false;
            }
            case "86", "86B" -> {
                isAUTO = false;
                isNRM = false;
                isVA = false;
                isSPB = false;
            }
            case "SPB", "OPNA", "2608" -> {
                isAUTO = false;
                isNRM = false;
                isVA = false;
                isSPB = true;
            }
            case "VA_NRM" -> {
                isAUTO = false;
                isNRM = true;
                isVA = true;
                isSPB = false;
            }
            case "VA_86" -> {
                isAUTO = false;
                isNRM = false;
                isVA = true;
                isSPB = false;
            }
        }
    }

    private static void optionDispHelp() {
        logger.log(Level.INFO, """
                Welcome to PMDDotNET !
                
                 Usage
                   PMDDotNETPlayer.exe  [-[H|?]] [-D=[EMU|GIMIC|SCCI|WAVE]] [-L=n] [-B=[NRM|OPN|2203|26|86|SPB|86B|OPNA|2608|VA_NRM|VA_86]] [-VV=n,n,n,n] [-VR=n] [
                            PMD options] [file.m]
                
                 Options
                  Options are not case sensitive.
                
                   -D=
                     You can change the playback device by specifying the -D= option.
                       -D=EMU
                         This is the default value.
                         Playback using emulation is performed from the Windows audio device.
                       -D=GIMIC
                         Playback is performed using the G.I.M.I.C OPNA module. If the module is not found, it will behave the same as EMU.
                       -D=SCCI
                         Playback is performed using the SCCI OPNA module. If the module is not found, it will behave the same as EMU.
                
                   -L=n
                     Specify the number of loops as a number greater than or equal to 0. (TBD)
                     However, 0 will result in an infinite loop. The default value is 0.
                     This is an option for the number of loops, but it is actually the number of times the sound is played. In other words, if you specify 1, it will end without looping after playing once.
                     If you specify a number that cannot be analyzed, it will become 0 (infinite loop).
                     If you specify 0 when the playback device is WAVE, it will be corrected to 1.
                
                   -B=
                     Specify the assumed sound source board.
                     In addition to changing the behavior of PMD, it also sets the volume value to be set for emulation and the actual chip.
                     The volume value can be changed using -VV=, described below.
                       -B=AUTO
                         This is the default value.
                         -B= -PPS= -PPZ= options are set automatically.
                         The setting is determined from the PCM file specification in the tag in the song data. As follows:
                
                             #    .PPC(header)  .PPS      .PZI       automatic configuration options
                             ------------------------------------------------------------
                             01   Not used      Not used  Not used   -B=SPB -PPS=0 -PPZ=0
                             02   .PPC/.PVI     Not used  Not used   -B=SPB -PPS=0 -PPZ=0
                             03   .P86          Not used  Not used   -B=86B -PPS=0 -PPZ=0
                             04   Not used      Used      Not used   -B=SPB -PPS=1 -PPZ=0
                             05   .PPC/.PVI     Used      Not used   -B=SPB -PPS=1 -PPZ=0
                             06   .P86          Used      Not used   -B=86B -PPS=1 -PPZ=0
                             07   Not used      Not used  Used       -B=SPB -PPS=0 -PPZ=1
                             08   .PPC/.PVI     Not used  Used       -B=SPB -PPS=0 -PPZ=1
                             09   .P86          Not used  Used       -B=86B -PPS=0 -PPZ=1
                             10   Not used      Used      Used       -B=SPB -PPS=1 -PPZ=1
                             11   .PPC/.PVI     Used      Used       -B=SPB -PPS=1 -PPZ=1
                             12   .P86          Used      Used       -B=86B -PPS=1 -PPZ=1
                
                       -B=NRM|OPN|2203|26
                         Specifies the normal sound source (OPN).
                         The following options are implicitly specified.
                           -VV=12,-5,-191,-191
                         For GIMIC GMC-OPNA
                           -VR=31
                         For modules other than GIMIC GMC-OPNA (PMD options)
                           -DF12 -DS0
                         For SCCI (PMD options)
                           -DF1 -DS0
                       -B=86|86B|SPB|OPNA|2608
                         Specifies the extended sound source (OPNA).
                         Specifying SPB/OPNA/2608 uses ADPCM. (Equivalent to PMDB2)
                         If a .PPC/.PVI file is specified, a process to transfer ADPCM data will occur before playback.
                         (Transfer will take time except in the case of emulation.)
                         The following options are implicitly specified.
                           -VV=0,-5,0,0
                         For GIMIC GMC-OPNA
                           -VR=66
                         For modules other than GIMIC GMC-OPNA (PMD options)
                           -DF12 -DS0
                         For SCCI (PMD options) (TBD)
                           -DF1 -DS0
                       -B=VA_NRM
                         Specifies the PC-88VA normal sound source. (TBD)
                         The following options are implicitly specified. (TBD)
                           -VV=0,0,0,0
                         For GIMIC GMC-OPNA (TBD)
                           -VR=31
                         For modules other than GIMIC GMC-OPNA or SCCI (PMD options) (TBD)
                           -DFn -DSn -DRn -DPn -DZn
                       -B=VA_86
                         Specifies the PC-88VA extended sound source. (TBD)
                         The following options are implicitly specified. (TBD)
                           -VV=0,0,0,0
                         For GIMIC GMC-OPNA (TBD)
                           -VR=31
                         For modules other than GIMIC GMC-OPNA or SCCI (PMD option) (TBD)
                           -DFn -DSn -DRn -DPn -DZn
                
                   -VV=n,n,n,n
                     Sets the volume value for emulation.
                     Specify the volumes in the order of FM, SSG, Rhythm, and Adpcm, separated by commas.
                     The range of n that can be specified is -191 to 20.
                
                   -VR=n
                     Sets the volume value for the real chip.
                     This is essentially an option for GIMIC's OPNA module only, and specifies the SSG volume from 0 to 127.
                
                   -PPS=n
                     Specify 1 when using PPSDRV. Specify 0 to disable use.
                     The default value is 0.
                     The possible values of n are 0 or 1.
                
                   -PPZ=n
                     Specify 1 when using PPZ8. If you specify 0, it will not be used.
                     The default value is 0.
                     The value n can be 0 or 1.
                
                   -PPSFREQ=n
                     Specifies the frequency (Hz) of PPSDRV.
                     Only valid for real chips.
                     The default value is 44100 for GIMIC and 16000 for SCCI.
                     The specifiable values for n are 2000 to 192000.
                
                   -PPSWAIT=n
                     Specifies the wait value for synchronization sent to SCCI.
                     Only valid for SCCI.
                     The default value is 1.
                     If -1 is specified, no transmission will occur.
                     The specifiable values for n are -1 to 100.
                
                   [PMD options]
                     Specifies the options to send to the original PMD.
                     If you specify options other than those listed above or a file name, they will all be interpreted as having been specified for the original PMD.
                
                   [file.m]
                     Specifies a .m file. Extensions are not checked.
                """);
    }

    private static void waitSendOPNA(long elapsed, int size) {
        switch (device) {
            case 0: // EMU
                return;
            case 1: // GIMIC

                // Add additional weight based on size and elapsed time.
                int m = Math.max((int) (size / 20 - elapsed), 0); // 20 Threshold (magic number)
                try { Thread.sleep(m); } catch (InterruptedException e) {}

                // Check the port as well
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
//                NScci.NSoundInterfaceManager().sendData();
//                while (!NScci.NSoundInterfaceManager().isBufferEmpty()) {
//                    Thread.sleep(0);
//                }
                break;
        }
    }

//    private static RSoundChip checkDevice() {
//        SChipType ct = null;
//        int iCount = 0;
//
//        switch (device) {
//            case 1: // GIMIC existence check
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
//            case 2: // SCCI Presence Check
//                nScci = new NScci.NScci();
//                iCount = NScci.NSoundInterfaceManager().getInterfaceCount();
//                if (iCount == 0) {
//                    nScci.Dispose();
//                    nScci = null;
//                    logger.log(Level.ERROR, "Not found SCCI.");
//                    device = 0;
//                    break;
//                }
//                for (int i = 0; i < iCount; i++) {
//                    NSoundInterface iIntfc = NScci.NSoundInterfaceManager().getInterface(i);
//                    NSCCI_INTERFACE_INFO iInfo = NScci.NSoundInterfaceManager().getInterfaceInfo(i);
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

    private static int emuCallback(short[] buffer, int offset, int count) {
        try {
            int bufCnt = count / 2;

            for (int i = 0; i < bufCnt; i++) {
                mds.update(emuRenderBuf, 0, 2, Program::oneFrame);
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

    private static void emuPlayback() {
        audioOutput.start();
        short[] buf = new short[samplingBuffer * 2];
        byte[] byteBuf = new byte[buf.length * 2];
        trdStopped = false;
        try {
            while (!trdClosed) {
                emuCallback(buf, 0, buf.length);
                for (int i = 0; i < buf.length; i++) {
                    byteBuf[i * 2] = (byte) (buf[i] & 0xff);
                    byteBuf[i * 2 + 1] = (byte) ((buf[i] >> 8) & 0xff);
                }
                audioOutput.write(byteBuf, 0, byteBuf.length);
            }
        } catch (Exception e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
        trdStopped = true;
    }

    private static void realCallback() {

        double o = sw.getElapsedMilliseconds() / swFreq;
        double oPPS = sw.getElapsedMilliseconds() / swFreq;
        double step = 1 / (double) SamplingRate;
        int PPSSamplingRate = userPPSFREQ == -1
                ? (device == 1 ? SamplingRatePPSGIMIC : SamplingRatePPSSCCI)
                : userPPSFREQ;
        double stepPPS = 1 / (double) PPSSamplingRate;

        trdStopped = false;
        try {
            while (!trdClosed) {
                Thread.sleep(0);

                double el1 = sw.getElapsedMilliseconds() / swFreq;
                if (el1 - o >= step) {
                    if (el1 - o >= step * SamplingRate / 100.0) // Threshold 10ms
                    {
                        do {
                            o += step;
                        } while (el1 - o >= step);
                    } else {
                        o += step;
                    }

                    oneFrame();
                }

                if (el1 - oPPS >= stepPPS) {
                    if (el1 - oPPS >= stepPPS * PPSSamplingRate / 100.0) // Threshold 10ms
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

    private static void oneFrame() {
        drv.render();
    }

    private static void writeOPNA(ChipDatum dat) {
        if (dat != null && dat.additionalData != null) {
            MmlDatum md = (MmlDatum) dat.additionalData;
            if (md.linePos != null) {
                logger.log(Level.TRACE, "! r%d c%d".formatted(md.linePos.row, md.linePos.col));
            }
        }

//#if DEBUG
        //if (dat.address == 0x29)
        //logger.log(Level.INFO, "FM P%d Out:Adr[{0:x02}] val[{1:x02}]".formatted((int)dat.address, (int)dat.data, dat.port));
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

    private static int writePPZ8(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x03) {
            ppz8em.writePcm(0, -1, -1, (byte[][]) arg.additionalData);
            return 0;
        } else {
            return ppz8em.write(0, arg.port, arg.address, arg.data);
        }
    }

    private static int writePPSDRV(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x05) {
            ppsdrv.writePcm(0, (byte[]) arg.additionalData, -1, -1);
            return 0;
        } else {
            return ppsdrv.write(0, arg.port, arg.address, arg.data);
        }
    }

    //static int aold = -1;
    //static int dold = -1;

    private static void psgPPSDRV(int a, int d) {
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

    private static int writeP86(ChipDatum arg) {
        if (arg == null) return 0;

        if (arg.port == 0x00) {
            p86em.writePcm(0, (byte[]) arg.additionalData, (byte) arg.data, (byte) arg.address);
            return 0;
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
