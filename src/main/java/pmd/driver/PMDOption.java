package pmd.driver;

import java.util.Arrays;
import java.util.StringJoiner;


public class PMDOption {

    public boolean isLoadADPCM;
    public boolean loadADPCMOnly;
    public boolean isAUTO;
    public boolean isVA;
    public boolean isNRM;
    public boolean usePPS;
    public boolean usePPZ;
    public boolean isSPB;
    public String[] envPmd;
    public String[] envPmdOpt;
    public String srcFile;
    public String ppcHeader;
    public int jumpIndex;

    @Override
    public String toString() {
        return new StringJoiner(", ", PMDOption.class.getSimpleName() + "[", "]")
                .add("isLoadADPCM=" + isLoadADPCM)
                .add("loadADPCMOnly=" + loadADPCMOnly)
                .add("isAUTO=" + isAUTO)
                .add("isVA=" + isVA)
                .add("isNRM=" + isNRM)
                .add("usePPS=" + usePPS)
                .add("usePPZ=" + usePPZ)
                .add("isSPB=" + isSPB)
                .add("envPmd=" + Arrays.toString(envPmd))
                .add("envPmdOpt=" + Arrays.toString(envPmdOpt))
                .add("srcFile='" + srcFile + "'")
                .add("ppcHeader='" + ppcHeader + "'")
                .add("jumpIndex=" + jumpIndex)
                .toString();
    }
}
