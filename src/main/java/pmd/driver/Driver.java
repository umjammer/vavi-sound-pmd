package pmd.driver;

import java.io.IOException;
import java.io.InputStream;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import dotnet4j.io.File;
import dotnet4j.io.FileAccess;
import dotnet4j.io.FileMode;
import dotnet4j.io.FileShare;
import dotnet4j.io.FileStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import dotnet4j.util.compat.Tuple;
import musicDriverInterface.ChipAction;
import musicDriverInterface.ChipDatum;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.IDriver;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.Tag;
import pmd.common.Common;
import vavi.util.ByteUtil;
import vavi.util.serdes.Serdes;

import static java.lang.System.getLogger;


public class Driver implements IDriver {

    private static final Logger logger = getLogger(Driver.class.getName());

    private PMD pmd = null;
    private PW work = null;
    private int renderingFreq = 44100;
    private int opnaMasterClock = 7987200;
    private Consumer<ChipDatum> writeOPNA;
    private Function<ChipDatum, Integer> writePPZ8;
    private Function<ChipDatum, Integer> writePPSDRV;
    private Function<ChipDatum, Integer> writeP86;
    private BiConsumer<Long, Integer> waitSendOPNA;
    private final Object lockObjWriteReg = new Object();
    MmlDatum[] srcBuf = null;
    public Exception renderingException = null;

    public Driver() {
    }

    @Override
    public void fadeOut() {
        throw new UnsupportedOperationException();
    }

    @Override
    public MmlDatum[] getDATA() {
        throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getPCMFromSrcBuf() {
        throw new UnsupportedOperationException();
    }

    @Override
    public ChipDatum[] getPCMSendData() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Tuple<String, short[]>[] getPCMTable() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getStatus() {
        if (work.getStatus() < 0) return -1;
        pmd.int60_main((short) 0x0a00);
        return pmd.pw.status2 != (byte) 0xff ? 1 : 0;
    }

    @Override
    public int getNowLoopCounter() {
        return pmd.pw.nowLoopCounter;
    }

    /**
     * Get driver specific tag
     */
    @Override
    public List<Tuple<String, String>> getTags() {
        List<Tuple<String, String>> tags = new ArrayList<>();
        X86Register lr = new X86Register();
        PW lpw = new PW();
        lpw.mmlbuf = 1;
        lpw.md = srcBuf;

        StringBuilder str;
        short[] adr = new short[] {get_memo(1, lr, lpw)};
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("title", str.toString()));
        }

        adr[0] = get_memo(2, lr, lpw);
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("composer", str.toString()));
        }

        adr[0] = get_memo(3, lr, lpw);
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("arranger", str.toString()));
        }

        int al = 4;
        str = new StringBuilder();
        do {
            adr[0] = get_memo(al, lr, lpw);
            if (adr[0] != 0) str.append("\r\n").append(getNRDString(/* ref */ adr));
            al++;
        } while (adr[0] != 0);
        str = new StringBuilder((!str.isEmpty()) ? str.substring(2) : "");
        if (!str.isEmpty()) {
            tags.add(new Tuple<>("memo", str.toString()));
        }

        adr[0] = get_memo(0, lr, lpw);
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("PCMFile", str.toString()));
            if (work != null) work.ppcFile = str.toString().trim();
        }

        adr[0] = get_memo(-1, lr, lpw);
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("PPSFile", str.toString()));
            if (work != null) work.ppsFile = str.toString().trim();
        }

        adr[0] = get_memo(-2, lr, lpw);
        if (adr[0] != 0) {
            str = new StringBuilder(getNRDString(/* ref */ adr));
            tags.add(new Tuple<>("PPZFile", str.toString()));
            if (work != null) {
                work.ppz1File = str.toString().trim();
                String[] p = work.ppz1File.split(",");
                if (p.length > 1) {
                    work.ppz1File = p[0];
                    work.ppz2File = p[1];
                }
            }
        }

        return tags;
    }

    public short get_memo(int al, X86Register r, PW pw) {
        try {

getmemo_errret:
            {
                r.al = (byte) al;
                r.setSi((short) pw.mmlbuf);
                if (pw.md[r.getSi() & 0xffff].dat != 0x1a)
                    break getmemo_errret; // File with no tone = Unable to obtain memo address
                r.setSi((short) ((r.getSi() & 0xffff) + 0x18));
                r.setSi((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
                r.setSi((short) ((r.getSi() & 0xffff) + pw.mmlbuf));
                r.setSi((short) ((r.getSi() & 0xffff) - 4));
                r.setBx((short) (pw.md[(r.getSi() & 0xffff) + 2].dat + pw.md[(r.getSi() & 0xffff) + 3].dat * 0x100)); // bh=0feh,bl=ver
                if (r.bl != 0x40) { //Ver4.0 & In the case of 00H
//                    break getmemo_exec;
                    if (r.bh != (byte) 0xfe)
                        break getmemo_errret; // 0feh for version 4.1 and later
                    if ((r.bl & 0xff) < 0x41)
                        break getmemo_errret; // If MC version is 4.1 or earlier, Error
                }
//getmemo_exec:

                if ((r.bl & 0xff) >= 0x42) // Is it version 4.2 or later?
//                    break getmemo_oldver41;
                    r.al++; // Then add +1 to al (0FFH #PPSFile)
//getmemo_oldver41:

                if ((r.bl & 0xff) >= 0x48) // Is it version 4.8 or later?
//                    break getmemo_oldver47;
                    r.al++; // Then add +1 to al (0FEH for #PPZFile)
//getmemo_oldver47:
                r.setSi((short) (pw.md[r.getSi() & 0xffff].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
                r.setSi((short) (r.getSi() + pw.mmlbuf));
                r.al++;
//getmemo_loop:
                do {
                    r.setDx((short) (pw.md[(r.getSi() & 0xffff) + 0].dat + pw.md[(r.getSi() & 0xffff) + 1].dat * 0x100));
                    if (r.getDx() == 0)
                        break getmemo_errret;
                    r.setSi((short) ((r.getSi() & 0xffff) + 2));
                    r.al--;
                } while (r.al != 0);
//                    break getmemo_loop;
//getmemo_exit:
                r.setDx((short) ((r.getDx() & 0xffff) + pw.mmlbuf));
                pw.ds_push = 0; // r.cs; No Segments
                pw.dx_push = r.getDx();
                return r.getDx();
            }
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Detected that the memo address is out of range. Ignored.");
            pw.ds_push = 0;
            pw.dx_push = 0;
            return 0;
        }
    }

    private String getNRDString(/* ref */ short[] index) {
        if (srcBuf == null || srcBuf.length < 1 || (index[0] & 0xffff) >= srcBuf.length) return "";

        try {
            List<Byte> lst = new ArrayList<>();
            for (; srcBuf[index[0] & 0xffff].dat != 0; index[0]++) {
                lst.add((byte) srcBuf[index[0] & 0xffff].dat);
            }

            String n = new String(ByteUtil.toByteArray(lst), Common.charset);
            index[0]++;

            return n;
        } catch (Exception e) {
            logger.log(Level.TRACE, e.getMessage(), e);
        }
        return "";
    }

    /**
     * Get GD3 tag (general song information)
     */
    @Override
    public GD3Tag getGD3TagInfo(byte[] srcBuf) {
        List<MmlDatum> sc = new ArrayList<>();
        for (byte b : srcBuf) sc.add(new MmlDatum(b & 0xff));
        this.srcBuf = sc.toArray(MmlDatum[]::new);
        List<Tuple<String, String>> lstTag = getTags();
        GD3Tag gd3tag = new GD3Tag();
        gd3tag.items.clear();
        for (Tuple<String, String> ttag : lstTag) {
            switch (ttag.getItem1()) {
                case "title" -> {
                    if (gd3tag.items.containsKey(Tag.Title)) gd3tag.items.remove(Tag.Title);
                    gd3tag.items.put(Tag.Title, new String[] {ttag.getItem2()});
                    if (gd3tag.items.containsKey(Tag.TitleJ)) gd3tag.items.remove(Tag.TitleJ);
                    gd3tag.items.put(Tag.TitleJ, new String[] {ttag.getItem2()});
                }
                case "composer" -> {
                    if (gd3tag.items.containsKey(Tag.Composer)) gd3tag.items.remove(Tag.Composer);
                    gd3tag.items.put(Tag.Composer, new String[] {ttag.getItem2()});
                    if (gd3tag.items.containsKey(Tag.ComposerJ)) gd3tag.items.remove(Tag.ComposerJ);
                    gd3tag.items.put(Tag.ComposerJ, new String[] {ttag.getItem2()});
                }
                case "arranger" -> {
                    if (gd3tag.items.containsKey(Tag.Arranger)) gd3tag.items.remove(Tag.Arranger);
                    gd3tag.items.put(Tag.Arranger, new String[] {ttag.getItem2()});
                    if (gd3tag.items.containsKey(Tag.ArrangerJ)) gd3tag.items.remove(Tag.ArrangerJ);
                    gd3tag.items.put(Tag.ArrangerJ, new String[] {ttag.getItem2()});
                }
                case "memo" -> {
                    if (gd3tag.items.containsKey(Tag.Memo)) gd3tag.items.remove(Tag.Memo);
                    gd3tag.items.put(Tag.Memo, new String[] {ttag.getItem2()});
                }
            }
        }
        return gd3tag;
    }

    @Override
    public Object getWork() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void init(List<ChipAction> chipsAction, MmlDatum[] srcBuf, Function<String, Stream> appendFileReaderCallback_, Object... additionalOption) {
//      throw new UnsupportedOperationException();
//    }

//    public void Init(Action<ChipDatum> opnaWrite, Action<long, int> opnaWaitSend, MmlDatum[] srcBuf, Object additionalOption) {
        Consumer<ChipDatum> opnaWrite = chipsAction.getFirst()::writeRegister;
        BiConsumer<Long, Integer> opnaWaitSend = chipsAction.getFirst()::waitSend;

        Object[] option = additionalOption;

        Object[] pdnos = (Object[]) option[0];
        PMDOption pdno = new PMDOption() {{
            isLoadADPCM = (boolean) pdnos[0];
            loadADPCMOnly = (boolean) pdnos[1];
            isAUTO = (boolean) pdnos[2];
            isVA = (boolean) pdnos[3];
            isNRM = (boolean) pdnos[4];
            usePPS = (boolean) pdnos[5];
            usePPZ = (boolean) pdnos[6];
            isSPB = (boolean) pdnos[7];
            envPmd = (String[]) pdnos[8];
            envPmdOpt = (String[]) pdnos[9];
            srcFile = (String) pdnos[10];
            ppcHeader = (String) pdnos[11];
            jumpIndex = -1;
        }};
logger.log(Level.DEBUG, pdno);

        Function<String, Stream> appendFileReaderCallback =
                (pdnos.length < 13 || pdnos[12] == null)
                        ? CreateAppendFileReaderCallback(Path.getDirectoryName(pdno.srcFile))
                        : (Function<String, Stream>) pdnos[12];

        if (pdnos.length == 14) {
            pdno.jumpIndex = (int) pdnos[13];
        }

        String[] po = (String[]) option[1];
        Function<ChipDatum, Integer> ppz8Write = (Function<ChipDatum, Integer>) option[2];
        Function<ChipDatum, Integer> ppsdrvWrite = (Function<ChipDatum, Integer>) option[3];
        Function<ChipDatum, Integer> p86Write = (Function<ChipDatum, Integer>) option[4];
        init(srcBuf,
                opnaWrite, opnaWaitSend,
                pdno, po,
                appendFileReaderCallback,
                ppz8Write,
                ppsdrvWrite,
                p86Write);

        pdnos[2] = pdno.isAUTO;
        pdnos[3] = pdno.isVA;
        pdnos[4] = pdno.isNRM;
        pdnos[5] = pdno.usePPS;
        pdnos[6] = pdno.usePPZ;
        pdnos[7] = pdno.isSPB;
    }

    public void init(
            String fileName,
            Consumer<ChipDatum> opnaWrite, BiConsumer<Long, Integer> opnaWaitSend,
            PMDOption additionalPMDDotNETOption, String[] additionalPMDOption,
            Function<String, Stream> appendFileReaderCallback,
            Function<ChipDatum, Integer> ppz8Write,
            Function<ChipDatum, Integer> ppsdrvWrite,
            Function<ChipDatum, Integer> p86Write) {
        if (!Path.getExtension(fileName).equalsIgnoreCase(".xml")) {
            byte[] srcBuf = File.readAllBytes(fileName);
            if (srcBuf.length < 1) return;
            init(srcBuf, opnaWrite, opnaWaitSend, additionalPMDDotNETOption, additionalPMDOption,
                    appendFileReaderCallback != null ? CreateAppendFileReaderCallback(Path.getDirectoryName(fileName)) : null,
                    ppz8Write,
                    ppsdrvWrite,
                    p86Write);
        } else {
            try (InputStream sr = Files.newInputStream(java.nio.file.Path.of(fileName))) {
                MmlDatum[] s = Serdes.Util.deserialize(sr, new MmlDatum[0]); // TODO
                init(s, opnaWrite, opnaWaitSend, additionalPMDDotNETOption, additionalPMDOption,
                        appendFileReaderCallback != null ? CreateAppendFileReaderCallback(Path.getDirectoryName(fileName)) : null,
                        ppz8Write,
                        ppsdrvWrite,
                        p86Write);
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }
    }

    public void resetOption(String[] pmdOption) {
        pmd.resetOption(pmdOption);
    }

    public void init(
            byte[] srcBuf,
            Consumer<ChipDatum> opnaWrite, BiConsumer<Long, Integer> opnaWaitSend,
            PMDOption additionalPMDDotNETOption, String[] additionalPMDOption,
            Function<String, Stream> appendFileReaderCallback,
            Function<ChipDatum, Integer> ppz8Write,
            Function<ChipDatum, Integer> ppsdrvWrite,
            Function<ChipDatum, Integer> p86Write) {
        if (srcBuf == null || srcBuf.length < 1) return;
        List<MmlDatum> bl = new ArrayList<>();
        for (byte b : srcBuf) bl.add(new MmlDatum(b & 0xff));
        init(bl.toArray(MmlDatum[]::new), opnaWrite, opnaWaitSend, additionalPMDDotNETOption, additionalPMDOption,
                appendFileReaderCallback,
                ppz8Write,
                ppsdrvWrite,
                p86Write
        );
    }

    public void init(
            MmlDatum[] srcBuf,
            Consumer<ChipDatum> opnaWrite, BiConsumer<Long, Integer> opnaWaitSend,
            PMDOption additionalPMDDotNETOption, String[] additionalPMDOption,
            Function<String, Stream> appendFileReaderCallback,
            Function<ChipDatum, Integer> ppz8Write,
            Function<ChipDatum, Integer> ppsdrvWrite,
            Function<ChipDatum, Integer> p86Write) {
        if (srcBuf == null || srcBuf.length < 1) return;

        this.srcBuf = srcBuf;

        writeOPNA = opnaWrite;
        waitSendOPNA = opnaWaitSend;
        writePPZ8 = ppz8Write;
        writePPSDRV = ppsdrvWrite;
        writeP86 = p86Write;

        work = new PW();
        getTags();
        additionalPMDDotNETOption.ppcHeader = checkPPC(appendFileReaderCallback);

        work.setOption(additionalPMDDotNETOption, additionalPMDOption);
        work.timer = new OPNATimer(44100, 7987200);

        //PPZ8em ppz8em = additionalPMDDotNETOption.ppz8em;
        //PPSDRV ppsdrv = additionalPMDDotNETOption.ppsdrv;

        pmd = new PMD(
                srcBuf,
                this::writeRegister,
                work,
                appendFileReaderCallback,
                writePPZ8,
                writePPSDRV,
                writeP86
        );

        if (pmd.pw.ppcFile != null && !pmd.pw.ppcFile.isEmpty()) pmd.pcmload.pcm_all_load(pmd.pw.ppcFile);
        if (pmd.pw.ppz1File != null && !pmd.pw.ppz1File.isEmpty() || pmd.pw.ppz2File != null && !pmd.pw.ppz2File.isEmpty())
            pmd.pcmload.ppz_load(pmd.pw.ppz1File, pmd.pw.ppz2File);
        if (pmd.pw.ppsFile != null && !pmd.pw.ppsFile.isEmpty()) pmd.pcmload.pps_load(pmd.pw.ppsFile);

    }

    private String checkPPC(Function<String, Stream> appendFileReaderCallback) {
        if (work.ppcFile == null || work.ppcFile.isEmpty()) {
            return "";
        }

        byte[] buf = null;
        String ext = Path.getExtension(work.ppcFile);
        String fn = work.ppcFile;
        int extn = 0;
        String[] ppcExtTbl = new String[] {".PPC", ".P86", ".PVI"};
        while (true) {
            buf = Common.getPCMDataFromFile(work.ppcFile, appendFileReaderCallback);
            if (buf != null) break;
            if (extn == 3) break;
            extn++;
            fn = Path.changeExtension(fn, ppcExtTbl[extn - 1]);
        }
        if (buf == null) return "";
        if (buf.length < 3) return "";

        String head = "%c%c%c".formatted((char) (buf[0] & 0xff), (char) (buf[1] & 0xff), (char) (buf[2] & 0xff));
        return head;
    }

    @Override
    public void startMusic(int musicNumber) {
        logger.log(Level.DEBUG, "Start playing");
        pmd.int60_main((short) 0);
        work.setStatus(1);
    }

    @Override
    public void stopMusic() {
        logger.log(Level.DEBUG, "Stop playing");
        pmd.int60_main((short) 0x0100);
        work.setStatus(0);
    }

    public void dispStatus() {
        pmd.int60_main((short) 5);
        int syosetu = (pmd.pw.al_push & 0xff) + (pmd.pw.ah_push & 0xff) * 0x100;

        logger.log(Level.TRACE, "Measure: %d".formatted(syosetu));
    }

    @Override
    public void render() {
        if (work.getStatus() < 0) return;

        try {
            pmd.rendering();
        } catch (Exception e) {
            renderingException = e;
            work.setStatus(-1);
            throw e;
        }
    }

    @Override
    public int setLoopCount(int loopCounter) {
        //throw new UnsupportedOperationException();
        return 0;
    }

    @Override
    public void shotEffect() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void startRendering(int renderingFreq, Tuple<String, Integer>[] chipsMasterClock) {
        synchronized (work.systemInterrupt) {

            work.timeCounter = 0L;
            this.renderingFreq = renderingFreq <= 0 ? 44100 : renderingFreq;
            this.opnaMasterClock = 7987200;
            if (chipsMasterClock != null && chipsMasterClock.length > 0) {
                this.opnaMasterClock = chipsMasterClock[0].getItem2() <= 0 ? 7987200 : chipsMasterClock[0].getItem2();
            }
            work.timer.setClock(renderingFreq, opnaMasterClock);

            logger.log(Level.TRACE, "Start rendering.");
        }
    }

    @Override
    public void stopRendering() {
        synchronized (work.systemInterrupt) {
            if (work.getStatus() > 0) work.setStatus(0);

            logger.log(Level.TRACE, "Stop rendering.");
        }
    }

    @Override
    public void writeRegister(ChipDatum reg) {
        if (work == null) return;
        synchronized (lockObjWriteReg) {
            if (reg.port == 0) {
                if (work != null) work.timer.WriteReg((byte) reg.address, (byte) reg.data);
            }
            writeOPNA.accept(reg);
        }
    }

    //public int getNowLoopCounter() {
    //    //throw new UnsupportedOperationException();
    //    return 0;
    //}

    private static Function<String, Stream> CreateAppendFileReaderCallback(String dir) {
        return fname -> {
            if (dir != null && !dir.isEmpty()) {
                var path = Path.combine(dir, fname);
                if (File.exists(path)) {
                    return new FileStream(path, FileMode.Open, FileAccess.Read, FileShare.Read);
                }
            }
            if (File.exists(fname)) {
                return new FileStream(fname, FileMode.Open, FileAccess.Read, FileShare.Read);
            }
            return null;
        };
    }

    @Override
    public void setDriverSwitch(Object... param) {
        throw new UnsupportedOperationException();
    }
}
