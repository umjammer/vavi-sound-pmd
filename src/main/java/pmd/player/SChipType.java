package pmd.player;

class SChipType {

    private boolean _UseEmu = true;

    public boolean getUseEmu() {
        return _UseEmu;
    }

    public void setUseEmu(boolean value) {
        _UseEmu = value;
    }

    private boolean _UseEmu2 = false;

    public boolean getUseEmu2() {
        return _UseEmu2;
    }

    public void setUseEmu2(boolean value) {
        _UseEmu2 = value;
    }

    private boolean _UseEmu3 = false;

    public boolean getUseEmu3() {
        return _UseEmu3;
    }

    public void setUseEmu3(boolean value) {

        _UseEmu3 = value;
    }


    private boolean _UseScci = false;

    public boolean getUseScci() {
        return _UseScci;
    }

    public void setUseScci(boolean value) {
        _UseScci = value;
    }

    private String _InterfaceName = "";

    public String getInterfaceName() {
        return _InterfaceName;
    }

    public void setInterfaceName(String value) {
        _InterfaceName = value;
    }

    private int _SoundLocation = -1;

    public int getSoundLocation() {
        return _SoundLocation;
    }

    public void setSoundLocation(int value) {
        _SoundLocation = value;
    }

    private int _BusID = -1;

    public int getBusID() {
        return _BusID;
    }

    public void setBusID(int value) {
        _BusID = value;
    }

    private int _SoundChip = -1;

    public int getSoundChip() {
        return _SoundChip;
    }

    public void setSoundChip(int value) {
        _SoundChip = value;
    }

    private String _ChipName = "";

    public String getChipName() {
        return _ChipName;
    }

    public void setChipName(String value) {
        _ChipName = value;
    }

    private boolean _UseScci2 = false;

    public boolean getUseScci2() {
        return _UseScci2;
    }

    public void setUseScci2(boolean value) {
        _UseScci2 = value;
    }

    private String _InterfaceName2A = "";

    public String getInterfaceName2A() {
        return _InterfaceName2A;
    }

    public void setInterfaceName2A(String value) {
        _InterfaceName2A = value;
    }

    private int _SoundLocation2A = -1;

    public int getSoundLocation2A() {
        return _SoundLocation2A;
    }

    public void setSoundLocation2A(int value) {
        _SoundLocation2A = value;
    }

    private int _BusID2A = -1;

    public int getBusID2A() {
        return _BusID2A;
    }

    public void setBusID2A(int value) {
        _BusID2A = value;
    }

    private int _SoundChip2A = -1;

    public int getSoundChip2A() {
        return _SoundChip2A;
    }

    public void setSoundChip2A(int value) {
        _SoundChip2A = value;
    }

    private String _ChipName2A = "";

    public String getChipName2A() {
        return _ChipName2A;
    }

    public void setChipName2A(String value) {
        _ChipName2A = value;
    }

    private String _InterfaceName2B = "";

    public String getInterfaceName2B() {
        return _InterfaceName2B;
    }

    public void setInterfaceName2B(String value) {
        _InterfaceName2B = value;
    }

    private int _SoundLocation2B = -1;

    public int getSoundLocation2B() {
        return _SoundLocation2B;
    }

    public void setSoundLocation2B(int value) {
        _SoundLocation2B = value;
    }

    private int _BusID2B = -1;

    public int getBusID2B() {
        return _BusID2B;
    }

    public void setBusID2B(int value) {
        _BusID2B = value;
    }

    private int _SoundChip2B = -1;

    public int getSoundChip2B() {
        return _SoundChip2B;
    }

    public void setSoundChip2B(int value) {
        _SoundChip2B = value;
    }

    private String _ChipName2B = "";

    public String getChipName2B() {
        return _ChipName2B;
    }

    public void setChipName2B(String value) {
        _ChipName2B = value;
    }

    private boolean _UseWait = true;

    public boolean getUseWait() {
        return _UseWait;
    }

    public void setUseWait(boolean value) {
        _UseWait = value;
    }

    private boolean _UseWaitBoost = false;

    public boolean getUseWaitBoost() {
        return _UseWaitBoost;
    }

    public void setUseWaitBoost(boolean value) {
        _UseWaitBoost = value;
    }

    private boolean _OnlyPCMEmulation = false;

    public boolean getOnlyPCMEmulation() {
        return _OnlyPCMEmulation;
    }

    public void setOnlyPCMEmulation(boolean value) {
        _OnlyPCMEmulation = value;
    }

    private int _LatencyForEmulation = 0;

    public int getLatencyForEmulation() {
        return _LatencyForEmulation;
    }

    public void setLatencyForEmulation(int value) {
        _LatencyForEmulation = value;
    }

    private int _LatencyForScci = 0;

    public int getLatencyForScci() {
        return _LatencyForScci;
    }

    public void setLatencyForScci(int value) {
        _LatencyForScci = value;
    }

    public SChipType Copy() {
        SChipType ct = new SChipType();
        ct._UseEmu = this._UseEmu;
        ct._UseEmu2 = this._UseEmu2;
        ct._UseEmu3 = this._UseEmu3;
        ct._UseScci = this._UseScci;
        ct._SoundLocation = this._SoundLocation;

        ct._BusID = this._BusID;
        ct._InterfaceName = this._InterfaceName;
        ct._SoundChip = this._SoundChip;
        ct._ChipName = this._ChipName;
        ct._UseScci2 = this._UseScci2;
        ct._SoundLocation2A = this._SoundLocation2A;

        ct._InterfaceName2A = this._InterfaceName2A;
        ct._BusID2A = this._BusID2A;
        ct._SoundChip2A = this._SoundChip2A;
        ct._ChipName2A = this._ChipName2A;
        ct._SoundLocation2B = this._SoundLocation2B;

        ct._InterfaceName2B = this._InterfaceName2B;
        ct._BusID2B = this._BusID2B;
        ct._SoundChip2B = this._SoundChip2B;
        ct._ChipName2B = this._ChipName2B;

        ct._UseWait = this._UseWait;
        ct._UseWaitBoost = this._UseWaitBoost;
        ct._OnlyPCMEmulation = this._OnlyPCMEmulation;
        ct._LatencyForEmulation = this._LatencyForEmulation;
        ct._LatencyForScci = this._LatencyForScci;

        return ct;
    }
}
