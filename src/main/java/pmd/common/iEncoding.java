package pmd.common;


public interface iEncoding {

    String getStringFromSjisArray(byte[] sjisArray);

    String getStringFromSjisArray(byte[] sjisArray, int index, int count);

    byte[] getSjisArrayFromString(String utfString);

    String getStringFromUtfArray(byte[] utfArray);

    byte[] getUtfArrayFromString(String utfString);
}
