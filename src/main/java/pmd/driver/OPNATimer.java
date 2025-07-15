package pmd.driver;

/**
 * OPNA timer emulation
 */
public class OPNATimer {

    /** Timer A overflow setting */
    private int timerA;
    /** Timer A counter value */
    private double timerACounter;
    /** Timer B overflow setting value */
    private int timerB;
    /** Timer B counter value */
    private double timerBCounter;
    /** Timer control register (lower 4 bits + 7 bits) */
    private int timerReg;
    private double step;

    private int statReg;

    /** Status register (lowest 2 bits) */
    public int getStatReg() {
        return statReg;
    }

    public Runnable csmKeyOn;

    public OPNATimer(int renderingFreq, int opnaMasterClock) {
        setClock(renderingFreq, opnaMasterClock);
    }

    public void setClock(int renderingFreq, int opnaMasterClock) {
        step = opnaMasterClock / 72.0 / 2.0 / (double) renderingFreq;
    }

    public void timer() {
        if ((timerReg & 0x01) != 0) { // timerA is running
            timerACounter += step;
            if (timerACounter >= (1024 - timerA)) {
                statReg |= ((timerReg >> 2) & 0x01);
                timerACounter -= (1024 - timerA);
                //if ((timerReg & 0x80) != 0) csmKeyOn?.Invoke();
            }
        }

        if ((timerReg & 0x02) != 0) { // timerB is running
            timerBCounter += step;
            if (timerBCounter >= timerB) {
                statReg |= ((timerReg >> 2) & 0x02);
                timerBCounter -= timerB;
            }
        }
    }

    public void WriteReg(byte adr, byte data) {
        switch (adr) {
            // timerA
            case 0x24:
                timerA &= 0x3;
                timerA |= ((data & 0xff) << 2);
                break;
            case 0x25:
                timerA &= 0x3fc;
                timerA |= ((data & 0xff) & 3);
                break;

            case 0x26:
                // timerB
                timerB = (256 - (data & 0xff)) << 4;
                break;

            case 0x27:
                // Timer Control Register
                timerReg = data & 0x8f;
                statReg &= 0xff - (((data & 0xff) >> 4) & 3);
                break;
        }
    }
}
