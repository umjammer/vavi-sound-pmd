package pmd.compiler;

import java.awt.Point;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

import dotnet4j.io.FileNotFoundException;
import dotnet4j.io.MemoryStream;
import dotnet4j.io.SeekOrigin;
import dotnet4j.io.Stream;
import dotnet4j.io.StreamReader;
import dotnet4j.util.compat.Tuple;
import dotnet4j.util.compat.Tuple3;
import musicDriverInterface.CompilerInfo;
import musicDriverInterface.GD3Tag;
import musicDriverInterface.ICompiler;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.Tag;
import pmd.common.iEncoding;
import pmd.common.MyEncoding;

import static java.lang.System.getLogger;


public class Compiler implements ICompiler {

    private static final Logger logger = getLogger(Compiler.class.getName());

    ResourceBundle rb = ResourceBundle.getBundle("message");

    //入力データ

    public iEncoding enc = null;
    public String[] mcArgs = null;
    public String[] env = null;

    //出力データ

    private int memo_writeAddress = -1;

    public int getMemo_writeAddress() {
        return memo_writeAddress;
    }

    private int vdat_setAddress = -1;

    public int getvdat_setAddress() {
        return vdat_setAddress;
    }

    public MmlSeg mml_seg = null;
    public VoiceSeg voice_seg = null;
    private byte[] outFFFileBuf = null;

    public byte[] getOutFFFileBuf() {
        return outFFFileBuf;
    }

    private String outFFFileName = null;
    public String getOutFFFileName() {
        return outFFFileName;
    }

    public int skipIndex = -1; // スキップ位置

    //内部
    private String srcBuf = null;
    private boolean isIDE = false;
    private Point skipPoint = new Point(0, 0);
    private Function<String, Stream> appendFileReaderCallback;
    private Work work = null;
    private byte[] ffBuf = null;

    public Compiler() { this(null); }

    public Compiler(iEncoding enc /* = null */) {
        this.enc = enc == null ? MyEncoding.Default() : enc;
    }

    @Override
    public void init() {
        this.isIDE = false;
        this.skipPoint = new Point(0, 0);
    }

    @Override
    public void setCompileSwitch(Object... param) {

        if (param == null) return;

        for (Object prm : param) {
            if (prm instanceof Function) {
                appendFileReaderCallback = (Function<String, Stream>) prm;
                continue;
            }

            if (!(prm instanceof String)) continue;

            //IDEフラグオン
            if (prm.equals("IDE")) {
                this.isIDE = true;
            }

            //スキップ再生指定
            if (((String) prm).indexOf("SkipPoint=") == 0) {
                try {
                    String[] p = ((String) prm).split("=")[1].split(":");
                    int r = Integer.parseInt(p[0].substring(1));
                    int c = Integer.parseInt(p[1].substring(1));
                    this.skipPoint = new Point(c, r);
                } catch (Exception e) {
                    continue;
                }
            }

            //PMD option 指定
            if (((String) prm).indexOf("PmdOption=") == 0) {
                try {
                    String[] p = ((String) prm).split("=")[1].split(" ");
                    mcArgs = p;
                } catch (Exception e) {
                    continue;
                }
            }
        }
    }

    @Override
    public MmlDatum[] compile(Stream sourceMML, Function<String, Stream> appendFileReaderCallback) {
        try (var ms = readAllBytesToMemoryStream(sourceMML)) {
            ms.seek(0, SeekOrigin.Begin);
            int c = 0;
            int offset = 0;
            while ((c = ms.readByte()) >= 0) {
                if (c == 0x1a) {
                    ms.setLength(offset);
                    break;
                }
                offset++;
            }
            ms.seek(0, SeekOrigin.Begin);

            try (StreamReader sr = new StreamReader(ms, Charset.forName("Shift_JIS"))) {
                srcBuf = sr.readToEnd();
            } catch (IOException e) {
                throw new dotnet4j.io.IOException(e);
            }
        }

        //Console.WriteLine(srcBuf);

        this.appendFileReaderCallback = appendFileReaderCallback;

        try {
            work = new Work();
            work.isIDE = isIDE;
            Mc mc = new Mc(this, mcArgs, srcBuf, ffBuf, work, env);

            mc.skipPoint = this.skipPoint;
            MmlDatum[] ret = mc.compile_start();
            memo_writeAddress = mc.memo_writeAddress;
            vdat_setAddress = mc.vdat_setAddress;
            mml_seg = mc.mml_seg;
            voice_seg = mc.voice_seg;
            work.compilerInfo.jumpClock = -1;
            if (mc.skipSW == 3) {
                skipIndex = mc.skipIndex + 1; // ひとつずらす
                work.compilerInfo.jumpClock = skipIndex;
            }

            outFFFileBuf = null;
            if (mc.outVoiceBuf != null) {
                outFFFileBuf = mc.outVoiceBuf;
                outFFFileName = voice_seg.v_filename;
            }

            //for (MmlDatum d : ret) {
            //    if (d.type == MMLType.Note) {
            //        Console.WriteLine("%d %d", d.linePos.row, d.linePos.col);
            //        ;
            //    }
            //}

            return ret;

//        } catch (PmdDosExitException e) {
//            ;
//        } catch (PmdErrorExitException peee) {
//            Work.compilerInfo.errorList.add(new Tuple3<Integer, Integer, String>(-1, -1, peee.getMessage()));
//            logger.log(Level.ERROR, peee.getMessage());
//        } catch (PmdException pe) {
//            Work.compilerInfo.errorList.add(new Tuple3<Integer, Integer, String>(-1, -1, pe.getMessage()));
//            logger.log(Level.ERROR, pe.getMessage());
        } catch (Exception e) {
            work.compilerInfo.errorList.add(new Tuple3<Integer, Integer, String>(-1, -1, e.getMessage()));
            logger.log(Level.ERROR, String.format(
                    rb.getString("E0000")
                    , e.getMessage()
                    ), e);
        }

        return null;
    }

    public boolean compile(Stream sourceMML, Stream destCompiledBin, Function<String, Stream> appendFileReaderCallback) {
        var dat = compile(sourceMML, appendFileReaderCallback);
        if (dat == null) {
            return false;
        }
        for (MmlDatum md : dat) {
            if (md == null) {
                destCompiledBin.writeByte((byte) 0);
            } else {
                destCompiledBin.writeByte((byte) md.dat);
            }
        }
        return true;
    }

    @Override
    public CompilerInfo getCompilerInfo() {
        return work.compilerInfo;
    }

    public Tuple<String, String>[] getTags(String srcBuf, Function<String, Stream> appendFileReaderCallback) {
        this.appendFileReaderCallback = appendFileReaderCallback;
        List<String> lstTag = new ArrayList<>();
        List<Tuple<String, String>> tags = new ArrayList<>();

        try {
            String[] srcList = srcBuf.split("\r\n");
            for (String lin : srcList) {
                if (lin == null || lin.isEmpty()) continue;
                if (lin.charAt(0) != '#') continue;

                lstTag.add(lin);
                if (lin.toUpperCase().indexOf("#INCLUDE") != 0) continue;

                getTagReca(lstTag, lin);
            }

            for (String tag : lstTag) {
                if (tag == null || tag.isEmpty()) continue;
                int i = 0;
                for (; i < tag.length(); i++) {
                    if (tag.charAt(i) == '\t' || tag.charAt(i) == ' ') break;
                }
                if (i == tag.length()) continue;

                String k = tag.substring(0, i).trim();
                String v = tag.substring(i + 1).trim();
                if (k == null || k.isEmpty()) continue;
                if (v == null || v.isEmpty()) continue;

                Tuple<String, String> keyVal = new Tuple<String, String>(k, v);
                tags.add(keyVal);
            }
        } catch (Exception e) {
        }

        return tags.toArray(Tuple[]::new);
    }

    public void setFfFileBuf(byte[] ffFileBuf) {
        ffBuf = ffFileBuf;
    }

    byte[] readFile(String filename) {
        Stream strm = appendFileReaderCallback.apply(filename);
        return readAllBytes(strm);
    }

    String readFileText(String mml_filename2) {
        Stream strm = appendFileReaderCallback.apply(mml_filename2);
        if (strm == null) {
            logger.log(Level.ERROR, String.format(rb.getString("E0201"), mml_filename2));
            throw new FileNotFoundException(mml_filename2);
            //return "";
        }
        String text;
        try (StreamReader sr = new StreamReader(strm, Charset.forName("Shift_JIS"))) {
            text = sr.readToEnd();
        } catch (IOException e) {
            throw new dotnet4j.io.IOException(e);
        }

        return text;
    }

    private void getTagReca(List<String> lstTag, String lin) {
        if (lin.length() < 9) return;

        String inc = readFileText(lin.substring(8).trim());
        if (inc == null || inc.isEmpty()) return;

        String[] incList = inc.split("\r\n");
        for (String ilin : incList) {
            if (ilin == null || ilin.isEmpty()) continue;
            if (lin.charAt(0) != '#') continue;

            lstTag.add(ilin);
            if (ilin.toUpperCase().indexOf("#INCLUDE") != 0) continue;

            getTagReca(lstTag, ilin);
        }
    }

    /**
     * ストリームから一括でバイナリを読み込む
     */
    private byte[] readAllBytes(Stream stream) {
        try (var ms = readAllBytesToMemoryStream(stream)) {
            return ms != null ? ms.toArray() : null;
        }
    }

    private MemoryStream readAllBytesToMemoryStream(Stream stream) {
        if (stream == null) return null;

        var buf = new byte[8192];
        var ms = new MemoryStream();
        while (true) {
            var r = stream.read(buf, 0, buf.length);
            if (r < 1) {
                break;
            }
            ms.write(buf, 0, r);
        }
        return ms;
    }

    @Override
    public GD3Tag getGD3TagInfo(byte[] srcBuf) {
        String text = new String(srcBuf, Charset.forName("shift_jis"));
        Tuple<String, String>[] tags = getTags(text, appendFileReaderCallback);
        GD3Tag gd3tag = new GD3Tag();
        gd3tag.items.clear();
        for (Tuple<String, String> ttag : tags) {
            if (ttag.getItem1().toLowerCase().trim().equals("#title")) {
                if (gd3tag.items.containsKey(Tag.Title)) gd3tag.items.remove(Tag.Title);
                gd3tag.items.put(Tag.Title, new String[] {ttag.getItem2()});
                if (gd3tag.items.containsKey(Tag.TitleJ)) gd3tag.items.remove(Tag.TitleJ);
                gd3tag.items.put(Tag.TitleJ, new String[] {ttag.getItem2()});
            } else if (ttag.getItem1().toLowerCase().trim().equals("#composer")) {
                if (gd3tag.items.containsKey(Tag.Composer)) gd3tag.items.remove(Tag.Composer);
                gd3tag.items.put(Tag.Composer, new String[] {ttag.getItem2()});
                if (gd3tag.items.containsKey(Tag.ComposerJ)) gd3tag.items.remove(Tag.ComposerJ);
                gd3tag.items.put(Tag.ComposerJ, new String[] {ttag.getItem2()});
            } else if (ttag.getItem1().toLowerCase().trim().equals("#arranger")) {
                if (gd3tag.items.containsKey(Tag.Arranger)) gd3tag.items.remove(Tag.Arranger);
                gd3tag.items.put(Tag.Arranger, new String[] {ttag.getItem2()});
                if (gd3tag.items.containsKey(Tag.ArrangerJ)) gd3tag.items.remove(Tag.ArrangerJ);
                gd3tag.items.put(Tag.ArrangerJ, new String[] {ttag.getItem2()});
            } else if (ttag.getItem1().toLowerCase().trim().equals("#memo")) {
                if (gd3tag.items.containsKey(Tag.Memo)) gd3tag.items.remove(Tag.Memo);
                gd3tag.items.put(Tag.Memo, new String[] {ttag.getItem2()});
            } else if (ttag.getItem1().toLowerCase().trim().contains("#fi")) {
                if (gd3tag.items.containsKey(Tag.SongObjFilename)) gd3tag.items.remove(Tag.SongObjFilename);
                gd3tag.items.put(Tag.SongObjFilename, new String[] {ttag.getItem2()});
            }
        }
        return gd3tag;
    }
}
