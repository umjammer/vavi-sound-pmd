package pmd.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import dotnet4j.io.MemoryStream;
import dotnet4j.io.Path;
import dotnet4j.io.Stream;
import musicDriverInterface.ChipDatum;
import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


public class PCMLOAD {

    private static final Logger logger = getLogger(PCMLOAD.class.getName());

    private X86Register r;
    private PW pw;
    private PMD pmd;
    private Pc98 pc98;
    private Function<ChipDatum, Integer> ppz8em;
    private Function<ChipDatum, Integer> ppsdrv;
    private Function<ChipDatum, Integer> p86em;
    private Function<String, Stream> appendFileReaderCallback = null;
    public byte[][] ppzPcmData = new byte[2][];
    public byte[][] p86PcmData = new byte[2][];

    public PCMLOAD(PMD pmd, PW pw, X86Register r, Pc98 pc98
            , Function<ChipDatum, Integer> ppz8em
            , Function<ChipDatum, Integer> ppsdrv
            , Function<ChipDatum, Integer> p86em
            , Function<String, Stream> appendFileReaderCallback) {
        this.pmd = pmd;
        this.pw = pw;
        this.r = r;
        this.pc98 = pc98;
        this.ppz8em = ppz8em;
        this.ppsdrv = ppsdrv;
        this.p86em = p86em;
        this.appendFileReaderCallback = appendFileReaderCallback;
    }

    private byte[] GetPCMDataFromFile(String fnPcm) {
        try (Stream pd = appendFileReaderCallback != null ? appendFileReaderCallback.apply(fnPcm) : null) {
            return ReadAllBytes(pd);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * ストリームから一括でバイナリを読み込む
     */
    private byte[] ReadAllBytes(Stream stream) {
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

    //20-202
    //
    //
    // PPZ(PVI/PZI)ファイルの読み込み
    //
    //  input DS:AX filename(128byte)
    // CL 読ませるバンク(1=１つ目 2=２つ目 3=両方)
    // output cy = 1    Not Loaded
    // AX=1 ファイルの読み込み失敗
    //    AX=2 データ形式が違う
    //    AX=3 メモリが確保できない
    //    AX=4 EMSハンドルのマッピングができない
    //    AX=5 PPZ8が常駐していない
    //    CL エラーの出たPCM番号(0 or 1)
    //  break ax,cx
    //
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

    //
    // PPZ8 読み込み main
    //
    private void ppz_load_main(String ppz1File, String ppz2File) {
        ppz8_check();
        pw.ppz_bank = (byte) ((ppz1File == null || ppz1File.isEmpty() ? 0 : 1) | (ppz2File == null || ppz2File.isEmpty() ? 0 : 2));
        r.cl = 0;
        r.setAx((short) 4);
        if (r.carry) {
            ppz_load_error();
            return;
        }
        // PCM２つ読み用追加判別処理
        read_ppz8();
        if (!r.carry) { // break plm_exit;
            if (ppz2File == null || ppz2File.isEmpty()) {
//                break plm_exit2; // ; PCMは１つだけ
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
        // 拡張子判別(PVI / PZI)
        String ext = Path.getExtension(pw.filename_ofs).toUpperCase().trim();
        if (ext.isEmpty()) pw.filename_ofs = Path.changeExtension(pw.filename_ofs, ".PZI");
        if (ext.equals(".PZI")) r.ch = 1;
        else if (ext.equals(".PVI")) r.ch = 0;

        r.carry = (pw.ppz_bank & 1) != 0;
        pw.ppz_bank >>= 1;
        if (r.carry) { // break p8_load_skip; // load skip

            // PVI / PZI 読み込み
//p8_load_main:
            byte[] pcmData = GetPCMDataFromFile(pw.filename_ofs);
            ppzPcmData[r.cl] = pcmData;
            ChipDatum cd = new ChipDatum(0x03, r.cl, r.ch, 0, ppzPcmData); // pcmData); // LoadPCM
            int ret = ppz8em.apply(cd);

            if (ret != 0) { // break p8_load_exit; // KUMA:読み込めた
                if (ret == 2) { // break p8_load_exit; // file not found or 形式が違うなら

                    r.ch ^= 1; // もう片方の形式も
                    cd = new ChipDatum(0x03, r.cl, r.ch, 0, ppzPcmData); // pcmData); // LoadPCM
                    ret = ppz8em.apply(cd); // pcm loadを試してみる
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

    // Error処理
    private void ppz_load_error() {
        r.incAx();
        if (pw.message != 0) {
            r.setBx(r.getAx());
            r.setDx((short) 0); // offset exit1z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(pw.exit1z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit2z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(pw.exit2z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit3z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(pw.exit3z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit4z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(pw.exit4z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit5z_mes
            r.decBx();
            if (r.getBx() == 0) {
                ppz_error_main(pw.exit5z_mes);
                return;
            }
            r.setDx((short) 0); // offset exit6z_mes
            ppz_error_main2(pw.exit6z_mes);
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
        ppz_error_main2(String.format(pw.ppzbank_mes, r.al) + msg);
    }

    private void ppz_error_main2(String msg) {
        logger.log(Level.ERROR, msg);
    }

    // PPZ8常駐check
    private void ppz8_check() {
        r.carry = pw.ppz == 0;
    }

    //203-288
    //
    // PCM(PPC/P86)ファイルの読み込み
    //  P86DRV.COMが常駐していれば.P86を、
    //  そうでない場合は.PPCを読む。
    //  PMDPPZEが常駐している場合は無条件にPVIをPPZ8に読み込む。
    //
    //  input DS:AX filename(128byte)
    // ES:DI pcm_work(32KB, P86の場合は必要無し)
    // output cy = 1    Not Loaded
    // PMDB2/PMD86の場合
    //    AX=1 SPB/ADPCM-RAMかPMDB2がない
    //     86B/P86DRV かPMD86がない
    // AX=2 ファイルがない
    //    AX=3 ファイルがPMDのPCMデータではない
    //    AX=4 SPB/既に読み込んであるのと同じだった
    //     86B/容量OVER
    //    AX=5 ファイルが読めない
    //    AX=6 PCMメモリがアクセス中だった
    //   PMDPPZEの場合
    //    AX=1 ファイルの読み込み失敗
    //    AX=2 データ形式が違う
    //    AX=3 メモリが確保できない
    //    AX=4 EMSハンドルのマッピングができない
    //    AX=5 PPZ8が常駐していない
    //
    // .PPC format:
    // WORK=PMD内PCMWORK , DATA=PCMRAM先頭のWORK , FILE=PCMFILE
    //      123456789012345678901234567890
    //  DATA/FILEのみ  "ADPCM DATA for  PMD ver.4.4-  "30bytes
    //  WORK/DATA/FILE  1Word Next START Address
    //     2Word*256 START/STOP
    //  WORK/DATAのみ  128bytes FILENAME
    // DATAのみ  32bytes 予備
    //
    //  PCMRAM_Work  =00000H～00025H
    //  PCMRAM_Main_data =00026H～01FFFH
    //
    // .P86 format:
    //  "PCM86 DATA",0ah,0 12 byte
    //  P86DRVのversion  1  byte
    //  全体のサイズ  3  byte
    //  音色table start(3),size(3) * 256 (1536) bytes
    //  音色データ 可変
    //
    //
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
        pmd.int60_main(r.getAx());// int 60h  ;DS:DX=PCMワーク
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

    //289-
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

    //345-388
    //
    // pps load
    //  in cs:[filename_ofs/seg] Filename
    //   cs:[pcmdata_ofs/seg] PPSData位置
    //   cs:[pcmdata_size] PPSData容量
    //
    private void pps_load_main() {
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".PPS"); // 拡張子 "PPS"に変更
        pcmData = GetPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {
            fn = pw.filename_ofs; // 指定
            pcmData = GetPCMDataFromFile(fn); //MMLの指定で読み込んでみる

            if (pcmData == null || pcmData.length < 1) {
                logger.log(Level.ERROR, "PPSファイル[%s]の読み込みに失敗しました。".formatted(pw.filename_ofs));
                pw.usePPSDRV = false;
                pw.ppsdrv_flag = 0;
            }
        } else {
            ChipDatum cd = new ChipDatum(0x05, 0, 0, 0, pcmData);
            ppsdrv.apply(cd); // .Load(pcmData);
        }
    }

    //
    // .PPC/.P86 一括load
    //  in cs:[filename_ofs/seg] Filename
    //   cs:[pcmdata_ofs/seg]
    //  PCMData loadarea
    // cs:[pcmwork_ofs/seg] PMD内PCMwork
    //
    private void all_load() {
        //-----------------------------------------------------------------------------
        // 読み込むのは.P86か.PPCかどうかを判別
        //-----------------------------------------------------------------------------
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
        //-----------------------------------------------------------------------------
        // .PPC read Main
        //-----------------------------------------------------------------------------
//allload_main1:
        check_pmdb2();
        if (r.carry) {
            allload_exit1();
            return;
        }

        filename_set();

        //-----------------------------------------------------------------------------
        // FileをPMDのワークにヘッダだけ読みこむ //KUMA:全部読み込む！
        //-----------------------------------------------------------------------------
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".PPC"); // 拡張子 "PPC"に変更
        pcmData = GetPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {
            fn = pw.filename_ofs; // 指定
            pcmData = GetPCMDataFromFile(fn); //MMLの指定で読み込んでみる

            if (pcmData == null || pcmData.length < 1) {
                fn = Path.changeExtension(pw.filename_ofs, ".PVI"); // 拡張子 "PVI"に変更
                pcmData = GetPCMDataFromFile(fn);
                if (pcmData == null || pcmData.length < 1) {
                    fn = Path.changeExtension(pw.filename_ofs, ".P86"); // 拡張子 "P86"に変更
                    pcmData = GetPCMDataFromFile(fn);
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
            if (pcmData[10] == 2) // RAM Type 8bit
            {
                pvi_load(pcmData);
                return;
            }
        }
//not_pvi:
        if (!(pcmData[0] == 'A' && pcmData[1] == 'D'
                && pcmData[2] == 'P' && pcmData[3] == 'C'
                && pcmData[4] == 'M' && pcmData[5] == ' '
        )) {
            allload_exit3_close(); // PMDのPCMデータではない
            return;
        }

        if (pcmData.length < 4 * 256 + 2 + 30) // KUMA:0x420未満
        {
            allload_exit3_close();
            return;
        }

        ppc_load_main(pcmData);
    }

    //-----------------------------------------------------------------------------
    // PMDのワークにFilenameを書く
    //-----------------------------------------------------------------------------
    private void ppc_load_main(byte[] pcmData) {
        write_filename_to_pmdwork();

        //-----------------------------------------------------------------------------
        // PCMRAMのヘッダを読む
        //-----------------------------------------------------------------------------
        if (pw.retry_flag == 0) { // break write_pcm_main; // 無条件

            //TBD

            //-----------------------------------------------------------------------------
            // PMDのワークとPCMRAMのヘッダを比較
            //-----------------------------------------------------------------------------
            //TBD

        }
        //-----------------------------------------------------------------------------
        // PMDのワークをPCMRAM頭に書き込む
        //-----------------------------------------------------------------------------
//write_pcm_main:
        //r.ds = r.cs;
        r.setSi((short) 0); // offset adpcm_header
        r.di = 0; // pcmdata_ofs
        r.setCx((short) (30 / 2)); // "ADPCM～"ヘッダを書き込み

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

        //-----------------------------------------------------------------------------
        // PCMDATAをPCMRAMに書き込む
        // 8000hずつ読み込みながら定義
        //-----------------------------------------------------------------------------
        if (pw.message != 0) {
            //r.ds = r.cs;
            logger.log(Level.INFO, pw.allload_mes); // "PCM定義中"の表示
        }

        r.setBx((short) 30); // pw.pcmwork_ofs; // cs:[pcmwork_ofs]
        r.setAx((short) ((pcmData[r.getBx()] & 0xff) + (pcmData[r.getBx() + 1] & 0xff) * 0x100)); // ds:[bx] ;AX=PCM Next Start Address
        r.subAx((short) 0x26); // 実際にこれから転送するデータ量に変換

        pw.pcmload_pcmstart = 0x26;
        pw.pcmload_pcmstop = 0x426; // 400h*32=8000h 一括

        int pcmdata_ofs = 4 * 256 + 2 + 30;
//allload_loop:
        while (r.getAx() >= 0x401) { // break allload_last;
            r.subAx((short) 0x400);
            r.bp = r.getAx(); // Push
            r.setCx((short) 0x8000);

            pw.pcmDt = new byte[r.getCx()];
            for (int i = 0; i < r.getCx(); i++) {
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
            pw.pcmDt = new byte[r.getCx()];
            for (int i = 0; i < r.getCx(); i++) {
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
            r.carry = (r.getAx() < r.getBx());
            //pushf
            pcmstore(); // PCM Store
            // popf
            // jc  allload_exit3_close
        }
//allload_justend:
        // FILE Close

        //-----------------------------------------------------------------------------
        // 終了
        //-----------------------------------------------------------------------------
        r.setAx((short) 0);
    }

    //
    // .PVI loading
    //
    private void pvi_load(byte[] pcmData) {
        // -----------------------------------------------------------------------------
        // ヘッダ / 音色tableの残りを読み込み
        // -----------------------------------------------------------------------------
        //KUMA:pcmDataに一括で入っているため不要

        List<Byte> o = new ArrayList<>();
        for (int i = 0; i < 30; i++) o.add((byte) 0);

        o.add((byte) 0);
        o.add((byte) 0); //Endpoint?

        short max = 0;
        for (int i = 0; i < 128; i++) {
            short st = (short) (((pcmData[i * 4 + 0x10] & 0xff) + (pcmData[i * 4 + 0x11] & 0xff) * 0x100) + 0x26);
            short ed = (short) (((pcmData[i * 4 + 0x12] & 0xff) + (pcmData[i * 4 + 0x13] & 0xff) * 0x100) + 0x26);
            if (max < st) max = st;
            if (max < ed) max = ed;
            o.add((byte) st);
            o.add((byte) (st >> 8));
            o.add((byte) ed);
            o.add((byte) (ed >> 8));
        }
        max++;
        o.set(0x1e, (byte) max);
        o.set(0x1f, (byte) (max >> 8));
        for (int i = 0; i < 128 * 4; i++) o.add((byte) 0);
        for (int i = 0x210; i < pcmData.length; i++) {
            o.add(pcmData[i]);
        }

        ppc_load_main(ByteUtil.toByteArray(o));
    }

    //670-737
    //
    // P86 data 一括load
    //  in cs:[filename_ofs/seg] Filename
    //
    private void p86_load() {
        //-----------------------------------------------------------------------------
        // P86drvのcheck
        //-----------------------------------------------------------------------------
        if (false) {
            //常駐チェックと
            //バージョンチェック
        }

        filename_set();

        //-----------------------------------------------------------------------------
        // P86Data,Size確認
        //-----------------------------------------------------------------------------
        String fn;
        byte[] pcmData;

        fn = Path.changeExtension(pw.filename_ofs, ".P86"); // 拡張子 "P86"に変更
        pcmData = GetPCMDataFromFile(fn);

        if (pcmData == null || pcmData.length < 1) {

            fn = pw.filename_ofs; // 指定
            pcmData = GetPCMDataFromFile(fn); //MMLの指定で読み込んでみる

            if (pcmData == null || pcmData.length < 1) {
                fn = Path.changeExtension(pw.filename_ofs, ".PPC"); // 拡張子 "PPC"に変更
                pcmData = GetPCMDataFromFile(fn);
                if (pcmData == null || pcmData.length < 1) {
//                    break p86load_error;
//p86load_error:
                    allload_exit2(); // File not found
                    return;
                }
            }
        }
//p86load_complete:
        r.setAx((short) 0); // 正常終了
        p86PcmData[0] = pcmData; // bank 0固定
        ChipDatum cd = new ChipDatum(0x00, 0, 0, 0, pcmData); // LoadPCM
        p86em.apply(cd);
    }


    //739-748
    //-----------------------------------------------------------------------------
    // エラーリターン
    //-----------------------------------------------------------------------------
    private void allload_exit1() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit1_mes
        }
        r.setAx((short) 1); // PCMが定義出来ません。
        error_exec(pw.exit1_mes);
    }

    private void allload_exit2() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit2_mes
        }
        r.setAx((short) 2); // PCMファイルがない
        error_exec(pw.exit2_mes);
    }

    private void allload_exit3_close() {
        allload_exit3();
    }

    private void allload_exit3() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit3_mes
        }
        r.setAx((short) 3); // ファイルがPMDのPCMではない
        error_exec(pw.exit3_mes);
    }

    private void allload_exit6_close() {
        allload_exit6();
    }

    private void allload_exit6() {
        if (pw.message != 0) {
            r.setDx((short) 0); //    mov dx,offset exit6_mes
        }
        r.setAx((short) 6); // PCMメモリアクセス中
        error_exec(pw.exit6_mes);
    }

    //828-839
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

    //840-864
    //
    // PMDB2＆ADPCMのCheck
    //  output cy  PMDB2又はADPCMがない
    //
    private void check_pmdb2() {
        //-----------------------------------------------------------------------------
        // PMDB2＆ADPCMの搭載CHECK
        //-----------------------------------------------------------------------------
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

    //
    // P86DRVの常駐Check
    //  output cy  P86DRVがない
    //
    private void check_p86drv() {
        r.carry = !pw.useP86DRV;
    }

    //
    // Filenameの大文字化＆パス名回避処理
    //
    private void filename_set() {
        //-----------------------------------------------------------------------------
        // Filenameを小文字から大文字に変換(SHIFTJIS回避付き)
        //-----------------------------------------------------------------------------
        pw.filename_ofs = pw.filename_ofs.toUpperCase().trim();

        //-----------------------------------------------------------------------------
        // Filename中のパス名を抜いたfilename_ofs2を設定(File名比較用)
        //-----------------------------------------------------------------------------
        pw.filename_ofs2 = Path.getFileName(pw.filename_ofs);
    }

    //
    // PMDのワークにFilenameを書く
    //
    private void write_filename_to_pmdwork() {
        r.setSi((short) 0);
        byte[] fnba = pw.filename_ofs2.getBytes(Charset.forName("shift_jis"));
        r.di = 4 * 256 + 2; // ES:DI = PMD内PCM_WORKのFilename格納位置
        r.setCx((short) 128); // byte数

        while (r.getCx() != 0) {
            if (r.getSi() < fnba.length) {
                pw.pcmWk[r.di] = fnba[r.getSi()];
                r.incSi();
            } else {
                pw.pcmWk[r.di] = 0; // 残りを０で埋める
            }
            r.di++;
            r.decCx();
        }
    }

    //
    // PCMメモリへメインメモリからデータを送る(x8, 高速/低速選択版)
    //
    // INPUTS..cs:[pcmstart] to Start Address
    //  .. cs:[pcmstop] to Stop  Address
    //  .. cs:[pcmdata_ofs/seg]
    //        to PCMData_Buffer
    //
    private void pcmstore() {
        key_check_reset();

        r.setDx((short) 0x0001);
        out46();
        r.setDx((short) 0x1017); // brdy以外はマスク(=timer割り込みは掛からない)
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
                //------------------------------------------------------------------------------
                // 低速定義
                //------------------------------------------------------------------------------
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
            //------------------------------------------------------------------------------
            // 中速定義
            //------------------------------------------------------------------------------
middle_store:
            ;
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

        //------------------------------------------------------------------------------
        // 高速定義
        //------------------------------------------------------------------------------
//fast_store:
        cli_sub();

//o4600x:
        do {
            r.al = pc98.InPort(r.getDx());
        } while ((r.al & 0x80) == 0); // break o4600x;
        r.al = 8; // PCMDAT reg.
        pc98.OutPort(r.getDx(), r.al);
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
            r.al = pw.pcmDt[r.incSi()];
            pc98.OutPort(r.getDx(), r.al); // OUT data
            b = r.getBx();
            r.setBx(r.getDx());
            r.setDx(b);

//o4601x:
            r.al = pc98.InPort(r.getDx());
            //if ((r.al & 8) == 0) // BRDY check
            //break o4601x;

            b = r.getBx();
            r.setBx(r.getDx());
            r.setDx(b);

            r.incCx();
        } while (r.getCx() == 0); // break fast_store_loop;

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

    //------------------------------------------------------------------------------
    // RS-232C以外は割り込みを禁止する
    // (FM音源LSI の ADDRESSの変更をさせない為)
    //------------------------------------------------------------------------------
    private void cli_sub() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());
        //cli
        r.setDx(pw.mmask_port);
        r.al = pc98.InPort(r.getDx());
        pw.mmask_push = r.al;
        r.al |= (byte) 0b1110_1111; // RSのみ変化させない
        pc98.OutPort(r.getDx(), r.al);
        //sti
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //------------------------------------------------------------------------------
    // 上のsubroutineで禁止した割り込みを元に戻す
    //------------------------------------------------------------------------------
    private void sti_sub() {
        r.stack.push(r.getAx());
        r.stack.push(r.getDx());
        //cli
        r.setDx(pw.mmask_port);
        r.al = pw.mmask_push;
        pc98.OutPort(r.getDx(), r.al);
        //sti
        r.setDx(r.stack.pop());
        r.setAx(r.stack.pop());
    }

    //
    // ＯＰＮＡ裏ポートへのデータの書き込み
    //
    // Inputs..dh to Register
    //  .. dl to Data
    //
    private void out46() {
        r.stack.push(r.getDx());
        r.stack.push(r.getBx());
        r.setBx(r.getDx());
        r.setDx(pw.port46);
//o4600:
        do {
            r.al = pc98.InPort(r.getDx());
            r.al |= r.al;
        } while ((r.al & 0x80) == 0); // break o4600;
        r.al = r.bh;
        //cli
        pc98.OutPort(r.getDx(), r.al);
        r.stack.push(r.getCx());
        //r.cx = (short)pw.pcmload_wait_clock;
        //do {
        //r.cx--;
        //} while (r.cx != 0);
        r.setCx(r.stack.pop());
        r.setDx(pw.port47);
        r.al = r.bl;
        pc98.OutPort(r.getDx(), r.al);
        //sti
        r.setBx(r.stack.pop());
        r.setDx(r.stack.pop());
    }

    //
    // PMDの ESC/GRPH入力を効かなくする
    // その他必要なデータをpmdのsegmentから読み取る
    //  out cy acccess flag on
    //
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
        if (pw.va != 0) // cmp word ptr ds:[84h],"AV"
        {
            pw.mmask_port = 0x18a; // master_mask(VA)
        }

        r.carry = false;
        if (pw.pcm_access == 0) { // cf=0 // break kcr_exit;
            r.al = pw.key_check;
            pw.key_check_push = r.al;
            pw.key_check = 0;
            pw.pcm_access = 1;
            pw.pcmflag = 0; // 追加(効果音対策)
            pw.pcm_effec_num = (byte) 255;
            pw.partWk[r.di].partmask &= (byte) 0xfd; // bit1をclear
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

    //1375-1396
    //
    // PMDの ESC/GRPH入力を元に戻す
    // PCMメモリアクセスフラグをoff
    //
    private void key_check_set() {
        //r.stack.push(r.ds);
        r.stack.push(r.getAx());
        r.stack.push(r.getBx());
        r.stack.push(r.getDx());

        r.ah = 0x10;
        pmd.int60_main(r.getAx());
        //    mov bx,dx
        //mov bx,-2[bx] // KUMA:open_work
        r.al = pw.key_check_push;
        pw.key_check = r.al;
        pw.pcm_access = 0;

        r.setDx(r.stack.pop());
        r.setBx(r.stack.pop());
        r.setAx(r.stack.pop());
        //r.ds = r.stack.pop();
    }
}
