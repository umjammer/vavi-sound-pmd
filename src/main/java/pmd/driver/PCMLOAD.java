package pmd.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import musicDriverInterface.ChipDatum;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;
import static pmd.common.Common.charset;


public class PCMLOAD {

    private static final Logger logger = getLogger(PCMLOAD.class.getName());

    private final X86Register r;
    private final PW pw;
    private final PMD pmd;
    private final Pc98 pc98;
    private final Function<ChipDatum, Integer> ppz8em;
    private final Function<ChipDatum, Integer> ppsdrv;
    private final Function<ChipDatum, Integer> p86em;
    private Function<String, Stream> appendFileReaderCallback = null;
    public byte[][] ppzPcmData = new byte[2][];
    public byte[][] p86PcmData = new byte[2][];

    public PCMLOAD(PMD pmd, PW pw, X86Register r, Pc98 pc98,
                   Function<ChipDatum, Integer> ppz8em,
                   Function<ChipDatum, Integer> ppsdrv,
                   Function<ChipDatum, Integer> p86em,
                   Function<String, Stream> appendFileReaderCallback) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.ppz8em = ppz8em;
        this.ppsdrv = ppsdrv;
        this.p86em = p86em;
        this.appendFileReaderCallback = appendFileReaderCallback;
    }

    private byte[] getPCMDataFromFile(String fnPcm) {
logger.log(Level.DEBUG, "pcm: " + fnPcm);
        try (Stream pd = appendFileReaderCallback != null ? appendFileReaderCallback.apply(fnPcm) : null) {
            return readAllBytes(pd);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Read binary from a stream in bulk
     */
    private byte[] readAllBytes(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        try (var ms = new MemoryStream()) {
            while (true) {
                var r = stream.read(buf, 0, buf.length);
                if (r < 1) {
                    break;
                }
                ms.write(buf, 0, r);
            }
            return ms.toArray();
        }
    }

    /**
     * Importing PPZ (PVI/PZI) files
     *
     *  input DS:AX filename(128byte)
     * CL Bank to read (1=first, 2=second, 3=both)
     * output cy = 1    Not Loaded
     * AX=1 File read failure
     *    AX=2 The data format is different
     *    AX=3 Cannot allocate memory
     *    AX=4 EMS handle mapping not possible
     *    AX=5 PPZ8 is not resident
     *    CL PCM number where the error occurred (0 or 1)
     *  break ax,cx
     */
    public void ppz_load(String ppz1File, String ppz2File) {
        r.stack.push(r.getBx());
        r.stack.push(r.getDx());
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        r.stack.push(r.bp);
        //r.stack.push(r.ds);
        //r.stack.push(r.es);

        pw.ppz_bank = r.cl;
        pw.filename_ofs = ppz1File;
        pw.filename_seg = 0;
        ppz_load_main(ppz1File, ppz2File);

        //r.es = r.stack.pop();
        //r.ds = r.stack.pop();
        r.bp = r.stack.pop();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
    }

    /**
     * PPZ8 loading main
     */
    private void ppz_load_main(String ppz1File, String ppz2File) {
        ppz8_check();
        pw.ppz_bank = (byte) ((ppz1File == null || ppz1File.isEmpty() ? 0 : 1) | (ppz2File == null || ppz2File.isEmpty() ? 0 : 2));
        r.cl = 0;
        r.setAx((short) 4);
        if (r.carry) {
            ppz_load_error();
            return;
        }
        // Additional discrimination process for reading two PCMs
        read_ppz8();
        if (!r.carry) { // break plm_exit;
            if (ppz2File == null || ppz2File.isEmpty()) {
//                break plm_exit2; // Only one PCM
//plm_exit2:
                r.setAx((short) 0);
                return;
            }
            pw.filename_ofs = ppz2File;
            r.cl = 1;
            read_ppz8();
        }
//plm_exit:
    }

    private void read_ppz8() {
        // File extension identification (PVI/PZI)
        String ext = Path.getExtension(pw.filename_ofs).toUpperCase().trim();
        if (ext.isEmpty()) pw.filename_ofs = Path.changeExtension(pw.filename_ofs, ".PZI");
        if (ext.equals(".PZI")) r.ch = 1;
        else if (ext.equals(".PVI")) r.ch = 0;

        r.carry = (pw.ppz_bank & 1) != 0;
        pw.ppz_bank = (byte) ((pw.ppz_bank & 0xff) >>> 1);
        if (r.carry) { // break p8_load_skip; // load skip

            // PVI/PZI Import
//p8_load_main:
            byte[] pcmData = getPCMDataFromFile(pw.filename_ofs);
            ppzPcmData[r.cl & 0xff] = pcmData;
            ChipDatum cd = new ChipDatum(0x03, r.cl & 0xff, r.ch & 0xff, 0, ppzPcmData); // pcmData); // LoadPCM
            int ret = ppz8em.apply(cd);

            if (ret != 0) { // break p8_load_exit; // KUMA: Loaded
                if (ret == 2) { // break p8_load_exit; // file not found or If the format is different

                    r.ch ^= 1; // The other form
                    cd = new ChipDatum(0x03, r.cl & 0xff, r.ch & 0xff, 0, ppzPcmData); // pcmData); // LoadPCM
                    ret = ppz8em.apply(cd); // Try pcm load
                }
            }
//p8_load_exit:
            r.carry = false;
            if (ret != 0) {
                r.setAx((short) ret);
                ppz_load_error();
                r.carry = true;
            }
        }
//p8_load_skip:
        r.setAx((short) 0);
    }

    /** Error Handling */
    private void ppz_load_error() {
        r.incAx();
        if (pw.message != 0) {
            r.setBx(r.getAx());
            r.setDx((short) 0); // offset exit1z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(PW.exit1z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit2z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(PW.exit2z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit3z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(PW.exit3z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit4z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(PW.exit4z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit5z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(PW.exit5z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit6z_mes
            ppz_error_main2(PW.exit6z_mes);
        }

        r.carry = true;
    }

    private void ppz_error_main(String msg) {
        r.al = r.cl;
        r.al += (byte) '1';
        //pw.banknum = r.al.toString(); // Bank
        r.stack.push(r.getDx());
        r.setDx((short) 0); // offset ppzbank_mes
        //ppz_error_main2(String.format(pw.ppzbank_mes, r.al));
        r.setDx(r.stack.pop());
        ppz_error_main2(String.format(PW.ppzbank_mes, (char) (r.al & 0xff)) + msg);
    }

    private void ppz_error_main2(String msg) {
        logger.log(Level.ERROR, msg);
    }

    /** PPZ8 resident check */
    private void ppz8_check() {
        r.carry = pw.ppz == 0;
    }

    /**
     * Importing PCM (PPC/P86) files
     *  If P86DRV.COM is resident, then read .P86; otherwise, read .PPC.
     *  If PMDPPZE is resident, PVI will be unconditionally loaded into PPZ8.
     * <pre>
     *  input DS:AX filename(128byte)
     * ES:DI pcm_work(32KB, Not necessary for P86)
     * output cy = 1    Not Loaded
     * For PMDB2/PMD86
     *    AX=1 SPB/ADPCM-RAM or PMDB2 is missing
     *     No 86B/P86DRV or PMD86
     * AX=2 File Missing
     *    AX=3 The file is not PMD PCM data.
     *    AX=4 SPB/It was the same as what was already loaded
     *     86B/Capacity OVER
     *    AX=5 Unable to read file
     *    AX=6 PCM memory was being accessed
     *   For PMDPPZE
     *    AX=1 File read failure
     *    AX=2 The data format is different
     *    AX=3 Cannot allocate memory
     *    AX=4 EMS handle mapping not possible
     *    AX=5 PPZ8 is not resident
     *
     * .PPC format:
     * WORK=PCMWORK in PMD, DATA=WORK at the beginning of PCMRAM, FILE=PCMFILE
     *      123456789012345678901234567890
     *  DATA/FILE only "ADPCM DATA for PMD ver.4.4- "30bytes
     *  WORK/DATA/FILE  1Word Next START Address
     *     2Word*256 START/STOP
     *  WORK/DATA only 128bytes FILENAME
     * DATA only  32 bytes reserved
     *
     *  PCMRAM_Work  =00000H～00025H
     *  PCMRAM_Main_data =00026H～01FFFH
     *
     * .P86 format:
     *  "PCM86 DATA",0ah,0 12 byte
     *  P86DRV version 1  byte
     *  Overall size 3  byte
     *  Tone table start(3),size(3) * 256 (1536) bytes
     *  Tone data variable
     *  </pre>
     */
    public void pcm_all_load(String ppcFile) {
        //cld
        //r.stack.push(r.ds);
        //r.stack.push(r.es);
        r.stack.push(r.getBx());
        r.stack.push(r.getCx());
        r.stack.push(r.getDx());
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        r.stack.push(r.bp);

        pw.filename_ofs = ppcFile; // r.ax;
        pw.filename_seg = 0; // r.ds;
        pw.pcmdata_ofs = r.di;
        pw.pcmdata_seg = 0; // r.es;
        r.ah = 0xe; // GET_PCM_ADR
        pmd.int60_main(r.getAx());// int 60h  ;DS:DX=PCM Work
        pw.pcmwork_ofs = r.getDx();
        pw.pcmwork_seg = 0; // r.ds;

        all_load();

        r.bp = r.stack.pop();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        r.setDx(r.stack.pop());
        r.setCx(r.stack.pop());
        r.setBx(r.stack.pop());
        //r.es = r.stack.pop();
        //r.ds = r.stack.pop();
    }

    public void pps_load(String ppsFile) {
        //cld
        //r.stack.push(r.ds);
        //r.stack.push(r.es);
        r.stack.push(r.getBx());
        r.stack.push(r.getCx());
        r.stack.push(r.getDx());
        r.stack.push(r.getSi());
        r.stack.push(r.di);
        r.stack.push(r.bp);

        pw.filename_ofs = ppsFile; // r.ax;
        pw.filename_seg = 0; // r.ds;
        pw.pcmdata_ofs = r.di;
        pw.pcmdata_seg = 0; // r.es;

        pmd.ppsdrv_check();
        if (!r.carry) { // break not_load;

            r.ah = 0x4;
            ChipDatum cd = new ChipDatum(0x04, 0, 0);
            ppsdrv.apply(cd); // .int04();

            pps_load_main();
        }
//not_load:
        r.bp = r.stack.pop();
        r.di = r.stack.pop();
        r.setSi(r.stack.pop());
        r.setDx(r.stack.pop());
        r.setCx(r.stack.pop());
        r.setBx(r.stack.pop());
        //r.es = r.stack.pop();
        //r.ds = r.stack.pop();
    }

    /**
     * pps load
     *  in
     *   cs:[filename_ofs/seg] Filename
     *   cs:[pcmdata_ofs/seg] PPSData location
     *   cs:[pcmdata_size] PPSData Capacity
     */
    private void pps_load_main() {
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".PPS"); // Change the extension to "PPS"
        pcmData = getPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {
            fn = pw.filename_ofs; // specification
            pcmData = getPCMDataFromFile(fn); // Try reading by specifying MML

            if (pcmData == null || pcmData.length < 1) {
                logger.log(Level.ERROR, "Failed to load PPS file [%s].".formatted(pw.filename_ofs));
                pw.usePPSDRV = false;
                pw.ppsdrv_flag = 0;
            }
        } else {
            ChipDatum cd = new ChipDatum(0x05, 0, 0, 0, pcmData);
            ppsdrv.apply(cd); // .Load(pcmData);
        }
    }

    /**
     * .PPC/.P86 Bulk load
     *  in
     *   cs:[filename_ofs/seg] Filename
     *   cs:[pcmdata_ofs/seg]PCMData loadarea
     *   cs:[pcmwork_ofs/seg] PCMwork in PMD
     */
    private void all_load() {
        //
        // Determine whether .P86 or .PPC is being read
        //
        check_p86drv();
        if (!r.carry) {
            p86_load();
            return;
        }
        r.ah = 9;
        pmd.int60_main(r.getAx()); // board check
        if (r.al != 1) { // pmdb2 // break allload_main1;
            if (r.al != 4) { // pmdppz // break allload_main1;
                if (r.al == 5) { // pmdppze
                    //break allload_ppze;
                    throw new UnsupportedOperationException();
                }
                allload_exit1();
                return;
            }
        }
        //
        // .PPC read Main
        //
//allload_main1:
        check_pmdb2();
        if (r.carry) {
            allload_exit1();
            return;
        }

        filename_set();

        //
        // Read only the header of the file into the PMD work // KUMA: Read the whole thing!
        //
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".PPC"); // Change the extension to "PPC"
        pcmData = getPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {
            fn = pw.filename_ofs; // specification
            pcmData = getPCMDataFromFile(fn); // Try reading by specifying MML

            if (pcmData == null || pcmData.length < 1) {
                fn = Path.changeExtension(pw.filename_ofs, ".PVI"); // Change the extension to "PVI"
                pcmData = getPCMDataFromFile(fn);
                if (pcmData == null || pcmData.length < 1) {
                    fn = Path.changeExtension(pw.filename_ofs, ".P86"); // Change the extension to "P86"
                    pcmData = getPCMDataFromFile(fn);
                    if (pcmData == null || pcmData.length < 1) {
                        allload_exit2();
                        return;
                    }
                }
            }
        }

//exec_ppcload:
        if (pcmData.length < 30) {
            allload_exit3_close();
            return;
        }

        if (pcmData[0] == 'P' && pcmData[1] == 'V' && pcmData[2] == 'I' && pcmData[3] == '2') {
            if (pcmData[10] == 2) { // RAM Type 8bit
                pvi_load(pcmData);
                return;
            }
        }
//not_pvi:
        if (!(pcmData[0] == 'A' && pcmData[1] == 'D'
                && pcmData[2] == 'P' && pcmData[3] == 'C'
                && pcmData[4] == 'M' && pcmData[5] == ' '
        )) {
            allload_exit3_close(); // Not PMD PCM data
            return;
        }

        if (pcmData.length < 4 * 256 + 2 + 30) { // KUMA: Less than 0x420
            allload_exit3_close();
            return;
        }

        ppc_load_main(pcmData);
    }

    /**
     * Write Filename to PMD work
     */
    private void ppc_load_main(byte[] pcmData) {
        write_filename_to_pmdwork();

        //
        // Read the PCMRAM header
        //
        if (pw.retry_flag == 0) { // break write_pcm_main; // unconditional

            //TBD

            //
            // Comparing PMD work and PCMRAM headers
            //

            //TBD
        }
        //
        // Write the PMD work to the PCMRAM head
        //
//write_pcm_main:
        //r.ds = r.cs;
        r.setSi((short) 0); // offset adpcm_header
        r.di = 0; // pcmdata_ofs
        r.setCx((short) (30 / 2)); // Write "ADPCM..." header

        pw.pcmDt = new byte[4 * 256 + 128 + 2 + 30];
        for (int i = 0; i < (4 * 256 + 128 + 2 + 30); i++) {
            pw.pcmDt[i] = pcmData[i];
        }
        for (int i = 0; i < (4 * 256 + 128); i++) {
            pw.pcmWk[i] = pcmData[i + 32];
        }

        pw.pcmload_pcmstart = 0;
        pw.pcmload_pcmstop = 0x25;
        pcmstore();

        //
        // Write PCMDATA to PCMRAM
        // Define while reading 8000h each time
        //
        if (pw.message != 0) {
            //r.ds = r.cs;
            logger.log(Level.INFO, PW.allload_mes); // "PCM definition in progress" display
        }

        r.setBx((short) 30); // pw.pcmwork_ofs; // cs:[pcmwork_ofs]
        r.setAx((short) ((pcmData[r.getBx() & 0xffff] & 0xff) + ((pcmData[(r.getBx() & 0xffff) + 1] & 0xff) * 0x100))); // ds:[bx] ;AX=PCM Next Start Address
        r.subAx((short) 0x26); // Converted into the amount of data to be actually transferred

        pw.pcmload_pcmstart = 0x26;
        pw.pcmload_pcmstop = 0x426; // 400h*32=8000h Bulk

        int pcmdata_ofs = 4 * 256 + 2 + 30;
//allload_loop:
        while ((r.getAx() & 0xffff) >= 0x401) { // break allload_last;
            r.subAx((short) 0x400);
            r.bp = r.getAx(); // Push
            r.setCx((short) 0x8000);

            pw.pcmDt = new byte[r.getCx() & 0xffff];
            for (int i = 0; i < (r.getCx() & 0xffff); i++) {
                pw.pcmDt[i] = pcmData[pcmdata_ofs++];
            }
            // jc allload_exit5_close
            // jnz allload_exit3_close

            pcmstore(); // PCM Store

            pw.pcmload_pcmstart += 0x400;
            pw.pcmload_pcmstop += 0x400;
            r.setAx(r.bp); // Pop
//            break allload_loop;
        }
//allload_last:
        if (r.getAx() != 0) { // break allload_justend;
            r.bp = r.getAx(); // Push
            r.addAx(pw.pcmload_pcmstart);
            pw.pcmload_pcmstop = r.getAx();
            r.setDx(pw.pcmdata_ofs); // cs:[pcmdata_ofs]
            r.setCx((short) 0x8000);
            pw.pcmDt = new byte[r.getCx() & 0xffff];
            for (int i = 0; i < (r.getCx() & 0xffff); i++) {
                if (pcmdata_ofs < pcmData.length)
                    pw.pcmDt[i] = pcmData[pcmdata_ofs++];
                else
                    pw.pcmDt[i] = 0;
            }
            // jc allload_exit5_close
            r.setBx(r.bp); // Pop
            r.addBx(r.getBx());
            r.addBx(r.getBx());
            r.addBx(r.getBx());
            r.addBx(r.getBx());
            r.addBx(r.getBx());
            r.carry = (r.getAx() & 0xffff) < (r.getBx() & 0xffff);
            //pushf
            pcmstore(); // PCM Store
            // popf
            // jc  allload_exit3_close
        }
//allload_justend:
        // FILE Close

        //
        // end
        //
        r.setAx((short) 0);
    }

    /**
     * .PVI loading
     */
    private void pvi_load(byte[] pcmData) {
        // 
        // Read the rest of the header/tone table
        // 
        // KUMA: Not necessary as it is all included in pcmData

        List<Byte> o = new ArrayList<>();
        for (int i = 0; i < 30; i++) o.add((byte) 0);

        o.add((byte) 0);
        o.add((byte) 0); // Endpoint?

        short max = 0;
        for (int i = 0; i < 128; i++) {
            short st = (short) (((pcmData[i * 4 + 0x10] & 0xff) + (pcmData[i * 4 + 0x11] & 0xff) * 0x100) + 0x26);
            short ed = (short) (((pcmData[i * 4 + 0x12] & 0xff) + (pcmData[i * 4 + 0x13] & 0xff) * 0x100) + 0x26);
            if ((max & 0xffff) < (st & 0xffff)) max = st;
            if ((max & 0xffff) < (ed & 0xffff)) max = ed;
            o.add((byte) st);
            o.add((byte) (st >>> 8));
            o.add((byte) ed);
            o.add((byte) (ed >>> 8));
        }
        max++;
        o.set(0x1e, (byte) max);
        o.set(0x1f, (byte) (max >>> 8));
        for (int i = 0; i < 128 * 4; i++) o.add((byte) 0);
        for (int i = 0x210; i < pcmData.length; i++) {
            o.add(pcmData[i]);
        }

        ppc_load_main(ByteUtil.toByteArray(o));
    }

    /**
     * P86 data bulk load
     *  in cs:[filename_ofs/seg] Filename
     */
    private void p86_load() {
        //
        // P86drv check
        //
        if (false) {
            // Persistent check and
            // version check
        }

        filename_set();

        //
        // Check P86Data,Size
        //
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".P86"); // Change the extension to "P86"
        pcmData = getPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {

            fn = pw.filename_ofs; // specification
            pcmData = getPCMDataFromFile(fn); // Try reading by specifying MML

            if (pcmData == null || pcmData.length < 1) {
                fn = Path.changeExtension(pw.filename_ofs, ".PPC"); // Change the extension to "PPC"
                pcmData = getPCMDataFromFile(fn);
                if (pcmData == null || pcmData.length < 1) {
//                    break p86load_error;
//p86load_error:
                    allload_exit2(); // File not found
                    return;
                }
            }
        }
//p86load_complete:
        r.setAx((short) 0); // normal termination
        p86PcmData[0] = pcmData; // Fixed to bank 0
        ChipDatum cd = new ChipDatum(0x00, 0, 0, 0, pcmData); // LoadPCM
        p86em.apply(cd);
    }

    //
    // Error Return
    //

    private void allload_exit1() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit1_mes
        }
        r.setAx((short) 1); // PCM cannot be defined.
        error_exec(PW.exit1_mes);
    }

    private void allload_exit2() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit2_mes
        }
        r.setAx((short) 2); // No PCM file
        error_exec(PW.exit2_mes);
    }

    private void allload_exit3_close() {
        allload_exit3();
    }

    private void allload_exit3() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit3_mes
        }
        r.setAx((short) 3); // File is not a PMD PCM
        error_exec(PW.exit3_mes);
    }

    private void allload_exit6_close() {
        allload_exit6();
    }

    private void allload_exit6() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit6_mes
        }
        r.setAx((short) 6); // PCM memory access
        error_exec(PW.exit6_mes);
    }

    private void error_exec(String msg) {
        if (pw.message != 0) {
            r.stack.push(r.getAx());
            r.setAx((short) 0); // r.cs;
            //r.ds = r.ax;
            r.ah = 0x09;
            //int 21h
            logger.log(Level.ERROR, msg);
            r.setAx(r.stack.pop());
        }

        r.carry = true;
    }

    /**
     * PMDB2 & ADPCM Check
     *  output cy PMDB2 or ADPCM not available
     */
    private void check_pmdb2() {
        //
        // PMDB2 & ADPCM installation check
        //
        r.ah = 0x10;
        pmd.int60_main(r.getAx()); // get_workadr in DS:DX
        r.setBx(r.getDx());
        r.setBx((short) 0); // mov bx,-2[bx] ;ds:bx = open_work
        if (pw.pcm_gs_flag == 0) { // break cpb_stc_ret; // ; ERROR Return
            r.setAx((short) pw.fm2_port1);
            pw.port46 = r.getAx();
            r.setAx((short) pw.fm2_port2);
            pw.port47 = r.getAx();
            r.carry = false;
            return;
        }
//cpb_stc_ret:
        r.carry = true;
    }

    /**
     * P86DRV resident check
     *  output cy  P86DRV is missing
     */
    private void check_p86drv() {
        r.carry = !pw.useP86DRV;
    }

    /**
     * Filename capitalization and pathname avoidance processing
     */
    private void filename_set() {
        //
        // Convert filename from lowercase to uppercase (with SHIFTJIS avoidance)
        //
        pw.filename_ofs = pw.filename_ofs.toUpperCase().trim();

        //
        // Set filename_ofs2 to the filename without the pathname (for file name comparison)
        //
        pw.filename_ofs2 = Path.getFileName(pw.filename_ofs);
    }

    /**
     * Write Filename to PMD work
     */
    private void write_filename_to_pmdwork() {
        r.setSi((short) 0);
        byte[] fnba = pw.filename_ofs2.getBytes(charset);
        r.di = 4 * 256 + 2; // ES:DI = Filename storage location of PCM_WORK in PMD
        r.setCx((short) 128); // Number of bytes

        while (r.getCx() != 0) {
            if ((r.getSi() & 0xffff) < fnba.length) {
                pw.pcmWk[r.di & 0xffff] = fnba[r.getSi() & 0xffff];
                r.incSi();
            } else {
                pw.pcmWk[r.di & 0xffff] = 0; // Fill the rest with 0
            }
            r.di++;
            r.decCx();
        }
    }

    /**
     * Send data from main memory to PCM memory (x8, high/low speed selectable version)
     *
     * INPUTS
     *  cs:[pcmstart] to Start Address
     *  cs:[pcmstop] to Stop  Address
     *  cs:[pcmdata_ofs/seg] to PCMData_Buffer
     */
    private void pcmstore() {
        key_check_reset();

        r.setDx((short) 0x0001);
        out46();
        r.setDx((short) 0x1017); // Mask all except brdy (=no timer interrupt)
        out46();
        r.setDx((short) 0x1080);
        out46();
        r.setDx((short) 0x0060);
        out46();
        r.setDx((short) 0x0102); // x8
        out46();
        r.setDx((short) 0x0cff);
        out46();
        r.dh++;
        out46();

        r.setBx(pw.pcmload_pcmstart);
        r.dh = 0x02;
        r.dl = r.bl;
        out46();
        r.dh++;
        r.dl = r.bh;
        out46();
        r.setDx((short) 0x04ff);
        out46();
        r.dh++;
        out46();

        r.setSi((short) 0); // [pcmdata_ofs]
        r.setCx(pw.pcmload_pcmstop);
        r.subCx(pw.pcmload_pcmstart);
        r.addCx(r.getCx());
        r.addCx(r.getCx());
        r.addCx(r.getCx());
        r.addCx(r.getCx());
        r.addCx(r.getCx());

        r.setDx(pw.port46);
        r.setBx(pw.port47);

        if (pw.adpcm_wait != 0) { // break fast_store;
            if (pw.adpcm_wait != 1) { // break middle_store;
                //
                // Low Speed Definition
                //
//slow_store:
                // cli
                //o4600z: in al,dx
                //    or  al,al
                //    js  o4600z
                //    mov al,8 ;PCMDAT reg.
                //    out dx, al
                //    push    cx
                //    mov cx, cs:[wait_clock]
                //    loop    $
                //    pop cx
                //    xchg bx, dx
                //    lodsb
                //    out dx, al   ; OUT data
                //    sti
                //    xchg    dx,bx
                //o4601z:
                // in al,dx
                //    test    al,8 ;BRDY check
                //    jz o4601z
                //o4601zb:
                // test al, al; BUSY check
                //    jns o4601zc
                // in al,dx
                //    jmp o4601zb
                //o4601zc:
                //    mov al,10h
                //    cli
                // out dx,al
                //    push    cx
                //    mov cx,cs:[wait_clock]
                //        loop $
                // pop cx
                //    xchg dx, bx
                //    mov al,80h
                // out dx,al ;BRDY reset
                //    sti
                //    xchg    dx,bx
                //    loop    slow_store
                //    jmp pcmst_exit
            }
            //
            // Medium Speed Definition
            //
//middle_store:
            // call cli_sub
            //o4600y: in al,dx
            //    or  al,al
            //    js  o4600y
            //    mov al,8 ;PCMDAT reg.
            //    out dx, al
            //middle_store_loop:
            //    push cx
            //    mov cx, cs:[wait_clock]
            //    loop    $
            //    pop cx
            //    xchg bx, dx
            //    lodsb
            //    out dx, al   ; OUT data
            //    xchg bx, dx
            //o4601y:
            // in al,dx
            //    test    al,8 ;BRDY check
            //    jz o4601y
            //    loop middle_store_loop
            //    call sti_sub
            //    jmp pcmst_exit
        }

        //
        // Fast definition
        //
//fast_store:
        cli_sub();

//o4600x:
        do {
            r.al = pc98.inPort(r.getDx() & 0xffff);
        } while ((r.al & 0x80) != 0); // break o4600x;
        r.al = 8; // PCMDAT reg.
        pc98.outPort(r.getDx(), r.al);
        r.stack.push(r.getCx());
        r.setCx(pw.pcmload_wait_clock);
        //do {
        //r.cx--;
        //} while (r.cx != 0);
        r.setCx(r.stack.pop());
        short b = r.getBx();
        r.setBx(r.getDx());
        r.setDx(b);

//fast_store_loop:
        do {
            r.al = pw.pcmDt[r.incSi() & 0xffff];
            pc98.outPort(r.getDx(), r.al); // OUT data
            b = r.getBx();
            r.setBx(r.getDx());
            r.setDx(b);

//o4601x:
            r.al = pc98.inPort(r.getDx() & 0xffff);
            //if ((r.al & 8) == 0) // BRDY check
            //break o4601x;

            b = r.getBx();
            r.setBx(r.getDx());
            r.setDx(b);

            r.decCx();
        } while (r.getCx() != 0); // break fast_store_loop;

        sti_sub();

//pcmst_exit:
        r.setDx((short) 0x1000);
        out46();
        r.setDx((short) 0x1080);
        out46();
        r.setDx((short) 0x0001);
        out46();
        key_check_set();
    }

    /**
     * Disable interrupts for anything other than RS-232C
     * ((To prevent the address of the FM sound source LSI from being changed)
     */
    private void cli_sub() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());
        //cli
        r.setDx(pw.mmask_port);
        r.al = pc98.inPort(r.getDx() & 0xffff);
        pw.mmask_push = r.al;
        r.al |= (byte) 0b1110_1111; // Only RS remains unchanged
        pc98.outPort(r.getDx(), r.al);
        //sti
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    /**
     * Revert the interrupts disabled in the subroutine above
     */
    private void sti_sub() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());
        //cli
        r.setDx(pw.mmask_port);
        r.al = pw.mmask_push;
        pc98.outPort(r.getDx(), r.al);
        //sti
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    /**
     * Write data to OPNA back port
     *
     * Inputs..dh to Register
     *  .. dl to Data
     */
    private void out46() {
        r.stack.push(r.getDx());
        r.stack.push(r.getBx());
        r.setBx(r.getDx());
        r.setDx(pw.port46);
//o4600:
        do {
            r.al = pc98.inPort(r.getDx() & 0xffff);
            r.al |= r.al;
        } while ((r.al & 0x80) != 0); // break o4600;
        r.al = r.bh;
        //cli
        pc98.outPort(r.getDx(), r.al);
        r.stack.push(r.getCx());
        //r.cx = (short)pw.pcmload_wait_clock;
        //do {
        //r.cx--;
        //} while (r.cx != 0);
        r.setCx(r.stack.pop());
        r.setDx(pw.port47);
        r.al = r.bl;
        pc98.outPort(r.getDx(), r.al);
        //sti
        r.setBx(r.stack.pop());
        r.setDx(r.stack.pop());
    }

    /**
     * Disable PMD ESC/GRPH input
     * Read other necessary data from pmd segment
     *  out cy acccess flag on
     */
    private void key_check_reset() {
        //r.stack.push(r.ds);
        r.stack.push(r.getAx());
        r.stack.push(r.getBx());
        r.stack.push(r.getDx());
        r.stack.push(r.di);

        r.ah = 0x10;
        pmd.int60_main(r.getAx());
        r.setBx(r.getDx());
        r.di = (short) pw.part10; // 18[bx]
        //mov bx,-2[bx] // KUMA:open_work
        r.setAx((short) pw.wait_clock); // _wait_clock[bx]
        pw.pcmload_wait_clock = r.getAx(); // get wait_clock
        r.al = pw.adpcm_wait; // [bx]
        pw.pcmload_adpcm_wait = r.al; // get adpcm_wait
        pw.mmask_port = 0x02; // master_mask(98)
        if (pw.va != 0) { // cmp word ptr ds:[84h],"AV"
            pw.mmask_port = 0x18a; // master_mask(VA)
        }

        r.carry = false;
        if (pw.pcm_access == 0) { // cf=0 // break kcr_exit;
            r.al = pw.key_check;
            pw.key_check_push = r.al;
            pw.key_check = 0;
            pw.pcm_access = 1;
            pw.pcmflag = 0; // Addition (sound effect measures)
            pw.pcm_effec_num = (byte) 255;
            pw.partWk[r.di & 0xffff].partmask &= (byte) 0xfd; // clear bit1
            r.carry = true; // cf=1
        }
//kcr_exit:
        r.carry = !r.carry;

        r.di = r.stack.pop();
        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.setAx(r.stack.pop());
        //r.ds = r.stack.pop();
    }

    /**
     * Resetting the PMD ESC/GRPH input
     * PCM memory access flag off
     */
    private void key_check_set() {
        //r.stack.push(r.ds);
        r.stack.push(r.getAx());
        r.stack.push(r.getBx());
        r.stack.push(r.getDx());

        r.ah = 0x10;
        pmd.int60_main(r.getAx());
        //    mov bx,dx
        //mov bx,-2[bx] // KUMA: open_work
        r.al = pw.key_check_push;
        pw.key_check = r.al;
        pw.pcm_access = 0;

        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.setAx(r.stack.pop());
        //r.ds = r.stack.pop();
    }
}
