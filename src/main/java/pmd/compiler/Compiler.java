package pmd.compiler;

import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

import musicDriverInterface.CompilerInfo;
import musicDriverInterface.ICompiler;
import musicDriverInterface.MetaData;
import musicDriverInterface.MetaData.Tag;
import musicDriverInterface.MmlDatum;
import vavi.util.compat.Tuple;
import vavi.util.compat.Tuple3;

import static java.lang.System.getLogger;
import static pmd.common.Common.charset;


public class Compiler implements ICompiler {

    private static final Logger logger = getLogger(Compiler.class.getName());

    final ResourceBundle rb = ResourceBundle.getBundle("pmd/message");

    // Input data

    public String[] mcArgs = null;
    public String[] env = null;

    // Output Data

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

    public int skipIndex = -1; // Skip Position

    // internal
    private String srcBuf = null;
    private boolean isIDE = false;
    private Point skipPoint = new Point(0, 0);
    private Function<String, InputStream> appendFileReaderCallback;
    private Work work = null;
    private byte[] ffBuf = null;

    public Compiler() {
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
                appendFileReaderCallback = (Function<String, InputStream>) prm;
                continue;
            }

            if (!(prm instanceof String)) continue;

            // IDE Flag On
            if (prm.equals("IDE")) {
                this.isIDE = true;
            }

            // Skip playback specification
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

            // PMD option specification
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
    public MmlDatum[] compile(InputStream sourceMML, Function<String, InputStream> appendFileReaderCallback) {
        try {
            byte[] b = sourceMML.readAllBytes();
            var ms = new ByteArrayInputStream(b);
            int c = 0;
            int offset = 0;
            while ((c = ms.read()) >= 0) {
                if (c == 0x1a) {
                    break;
                }
                offset++;
            }

            var sr = new InputStreamReader(new ByteArrayInputStream(b, 0, offset), charset);
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = sr.read()) != -1) {
                sb.append((char) ch);
            }
            srcBuf = sb.toString();
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(e);
        }

        //logger.log(Level.TRACE, srcBuf);

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
                skipIndex = mc.skipIndex + 1; // Shift one
                work.compilerInfo.jumpClock = skipIndex;
            }

            outFFFileBuf = null;
            if (mc.outVoiceBuf != null) {
                outFFFileBuf = mc.outVoiceBuf;
                outFFFileName = voice_seg.v_filename;
            }

            //for (MmlDatum d : ret) {
            //    if (d.type == MMLType.Note) {
            //        logger.log(Level.TRACE, "%d %d", d.linePos.row, d.linePos.col);
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
            work.compilerInfo.errorList.add(new Tuple3<>(-1, -1, e.getMessage()));
            logger.log(Level.ERROR, e.getMessage(), e);
        }

        return null;
    }

    public boolean compile(InputStream sourceMML, ByteArrayOutputStream destCompiledBin, Function<String, InputStream> appendFileReaderCallback) {
        var dat = compile(sourceMML, appendFileReaderCallback);
        if (dat == null) {
            return false;
        }
        for (MmlDatum md : dat) {
            if (md == null) {
                destCompiledBin.write((byte) 0);
            } else {
                destCompiledBin.write((byte) (md.dat & 0xff));
            }
        }
        return true;
    }

    @Override
    public CompilerInfo getCompilerInfo() {
        return work.compilerInfo;
    }

    public Tuple<String, String>[] getTags(String srcBuf, Function<String, InputStream> appendFileReaderCallback) {
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

                Tuple<String, String> keyVal = new Tuple<>(k, v);
                tags.add(keyVal);
            }
        } catch (Exception e) {
        }

        return tags.toArray(Tuple[]::new);
    }

    public void setFfFileBuf(byte[] ffFileBuf) {
        ffBuf = ffFileBuf;
    }

    byte[] readFile(String filename) throws IOException {
        InputStream strm = appendFileReaderCallback.apply(filename);
        return strm.readAllBytes();
    }

    String readFileText(String mml_filename2) {
        InputStream strm = appendFileReaderCallback.apply(mml_filename2);
        if (strm == null) {
            logger.log(Level.ERROR, String.format(rb.getString("E0201"), mml_filename2));
            throw new IllegalArgumentException(mml_filename2);
            //return "";
        }
        String text;
        try (var sr = new InputStreamReader(strm, charset)) {
            StringBuilder sb = new StringBuilder();
            int ch;
            while ((ch = sr.read()) != -1) {
                sb.append((char) ch);
            }
            text = sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
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

    @Override
    public MetaData getMetaData(byte[] srcBuf) {
        String text = new String(srcBuf, charset);
        Tuple<String, String>[] tags = getTags(text, appendFileReaderCallback);
        MetaData metaData = new MetaData();
        for (Tuple<String, String> ttag : tags) {
            if (ttag.getItem1().toLowerCase().trim().equals("#title")) {
                metaData.set(Tag.Title, ttag.getItem2());
                metaData.set(Tag.TitleJ, ttag.getItem2());
            } else if (ttag.getItem1().toLowerCase().trim().equals("#composer")) {
                metaData.set(Tag.Composer, ttag.getItem2());
                metaData.set(Tag.ComposerJ, ttag.getItem2());
            } else if (ttag.getItem1().toLowerCase().trim().equals("#arranger")) {
                metaData.set(Tag.Arranger, ttag.getItem2());
                metaData.set(Tag.ArrangerJ, ttag.getItem2());
            } else if (ttag.getItem1().toLowerCase().trim().equals("#memo")) {
                metaData.set(Tag.Memo, ttag.getItem2());
            } else if (ttag.getItem1().toLowerCase().trim().contains("#fi")) {
                metaData.set(Tag.SongObjFilename, ttag.getItem2());
            }
        }
        return metaData;
    }
}
