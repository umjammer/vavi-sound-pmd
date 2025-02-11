package pmd.driver;

//
// OPNA timer emulation
//
//
public class OPNATimer {

    private int TimerA;        // Timer A overflow setting
    private double TimerAcounter;  // Timer A counter value
    private int TimerB;            // Timer B overflow setting value
    private double TimerBcounter;  // Timer B counter value
    private int TimerReg;       // Timer control register (lower 4 bits + 7 bits)
    private double step;

    private int StatReg;

    // Status register (lowest 2 bits)
    public int getStatReg() {
        return StatReg;
    }

    public Runnable CsmKeyOn;

    public OPNATimer(int renderingFreq, int opnaMasterClock) {
        setClock(renderingFreq, opnaMasterClock);
    }

    public void setClock(int renderingFreq, int opnaMasterClock) {
        step = opnaMasterClock / 72.0 / 2.0 / (double) renderingFreq;
    }

    public void timer() {
        if ((TimerReg & 0x01) != 0) {   // TimerA is running
            TimerAcounter += step;
            if (TimerAcounter >= (1024 - TimerA)) {
                StatReg |= ((TimerReg >> 2) & 0x01);
                TimerAcounter -= (1024 - TimerA);
                //if ((TimerReg & 0x80) != 0) CsmKeyOn?.Invoke();
            }
        }

        if ((TimerReg & 0x02) != 0) {   // TimerB is running
            TimerBcounter += step;
            if (TimerBcounter >= TimerB) {
                StatReg |= ((TimerReg >> 2) & 0x02);
                TimerBcounter -= TimerB;
            }
        }
    }

    public void WriteReg(byte adr, byte data) {
        switch (adr) {
            // TimerA
            case 0x24:
                TimerA &= 0x3;
                TimerA |= (data << 2);
                break;
            case 0x25:
                TimerA &= 0x3fc;
                TimerA |= (data & 3);
                break;

            case 0x26:
                // TimerB
                TimerB = (256 - data) << 4;
                break;

            case 0x27:
                // Timer Control Register
                TimerReg = data & 0x8F;
                StatReg &= 0xFF - ((data >> 4) & 3);
                break;
        }
    }
}
