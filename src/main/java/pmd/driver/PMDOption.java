package pmd.driver;

import java.util.Arrays;
import java.util.StringJoiner;


class PMDOption {

    boolean isLoadADPCM;
    boolean loadADPCMOnly;
    boolean isAUTO;
    boolean isVA;
    boolean isNRM;
    boolean usePPS;
    boolean usePPZ;
    boolean isSPB;
    String[] envPmd;
    String[] envPmdOpt;
    String srcFile;
    String ppcHeader;
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
