
package pmd.compiler;

import java.util.ArrayList;
import java.util.List;

import dotnet4j.util.compat.Tuple;
import musicDriverInterface.MmlDatum;
import musicDriverInterface.common.AutoExtendList;


public class MSeg {

    public String m_filename;
    public int file_ext_adr; // w
//#if efc && olddat
//        public byte m_start; // b dummy
//        public AutoExtendList<MmlDatum> m_buf = new AutoExtendList<MmlDatum>(); // [63 * 1024 - 1];
//        public byte mbuf_end;
//#else
    public byte m_start; // b
    public AutoExtendList<MmlDatum> m_buf = new AutoExtendList<>(MmlDatum.class); // [63 * 1024 - 2];
    public byte mbuf_end;
    public List<Tuple<Integer, MmlDatum>> dummy = new ArrayList<>();

    private List<MmlDatum> macroLst = new ArrayList<>();

    public List<MmlDatum> getMacroLst() {
        return macroLst;
    }

    public void setMacroLst(List<MmlDatum> value) {
        macroLst = value;
    }
//#endif
}

