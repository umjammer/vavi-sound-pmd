package pmd.driver;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.List;

import vavi.util.ByteUtil;

import static java.lang.System.getLogger;


//
// ppz8l.cpp / ppz8l.h Created with reference to (by C60)
//
public class _PPZ8em {

    private static final Logger logger = getLogger(_PPZ8em.class.getName());

    public byte[][] pcmData = new byte[2][];
    private boolean[] isPVI = new boolean[2];
    private PPZChannelWork[] chWk = {
            new PPZChannelWork(), new PPZChannelWork(), new PPZChannelWork(), new PPZChannelWork(),
            new PPZChannelWork(), new PPZChannelWork(), new PPZChannelWork(), new PPZChannelWork()
    };
    public int bank = 0;
    public int ptr = 0;
    private boolean interrupt = false;
    private byte adpcmEmu;
    private short[][] VolumeTable = {
            new short[256], new short[256], new short[256], new short[256],
            new short[256], new short[256], new short[256], new short[256],
            new short[256], new short[256], new short[256], new short[256],
            new short[256], new short[256], new short[256], new short[256]
    };
    private double SamplingRate = 44100.0;
    private int PCM_VOLUME = 0;
    private int volume = 0;

    public _PPZ8em(int SamplingRate /* = 44100 */) {
        this.SamplingRate = SamplingRate;
    }

    /**
     * 0x00 Initialization
     */
    public void Initialize() {
        bank = 0;
        ptr = 0;
        interrupt = false;
        for (int i = 0; i < 8; i++) {
            chWk[i].srcFrequency = 16000;
            chWk[i].pan = 5;
            chWk[i].panL = 1.0;
            chWk[i].panR = 1.0;
            chWk[i].volume = 8;
            //chWk[i]._frequency = 0;
            chWk[i]._loopStartOffset = -1;
            chWk[i]._loopEndOffset = -1;
        }
        PCM_VOLUME = 0;
        volume = 0;
        SetAllVolume(12);
    }

    private void MakeVolumeTable(int vol) {
        int i, j;
        double temp;

        volume = vol;
        int AVolume = (int) (0x1000 * Math.pow(10.0, vol / 40.0));

        for (i = 0; i < 16; i++) {
            temp = Math.pow(2.0, (i + PCM_VOLUME) / 2.0) * AVolume / 0x18000;
            for (j = 0; j < 256; j++) {
                VolumeTable[i][j] = (short) (Math.max(Math.min((j - 128) * temp, Short.MAX_VALUE), Short.MIN_VALUE));
            }
        }
    }

    /**
     * 0x01 PCM Pronunciation
     *
     * @param al PCM Channel (0-7)
     * @param dx PCM tone number
     */
    public void PlayPCM(byte al, short dx) {
//#if DEBUG
        logger.log(Level.TRACE, String.format("ppz8em: PlayPCM: ch:%d @:%d", al, dx));
//#endif

        int bank = (dx & 0x8000) != 0 ? 1 : 0;
        int num = dx & 0x7fff;
        chWk[al].bank = bank;
        chWk[al].num = num;

        if (pcmData[bank] != null) {
            chWk[al].ptr = (pcmData[bank][num * 0x12 + 32] & 0xff)
                    + (pcmData[bank][num * 0x12 + 1 + 32] & 0xff) * 0x100
                    + (pcmData[bank][num * 0x12 + 2 + 32] & 0xff) * 0x10000
                    + (pcmData[bank][num * 0x12 + 3 + 32] & 0xff) * 0x1000000
                    + 0x20 + 0x12 * 128;
            if (chWk[al].ptr >= pcmData[bank].length) {
                chWk[al].ptr = pcmData[bank].length - 1;
            }
            chWk[al].end = chWk[al].ptr
                    + (pcmData[bank][num * 0x12 + 4 + 32] & 0xff)
                    + (pcmData[bank][num * 0x12 + 5 + 32] & 0xff) * 0x100
                    + (pcmData[bank][num * 0x12 + 6 + 32] & 0xff) * 0x1_0000
                    + (pcmData[bank][num * 0x12 + 7 + 32] & 0xff) * 0x10_00000
            ;
            if (chWk[al].end >= pcmData[bank].length) {
                chWk[al].end = pcmData[bank].length - 1;
            }


            chWk[al].loopStartOffset = chWk[al]._loopStartOffset;
            if (chWk[al]._loopStartOffset == -1) {
                chWk[al].loopStartOffset = 0
                        + (pcmData[bank][num * 0x12 + 8 + 32] & 0xff)
                        + (pcmData[bank][num * 0x12 + 9 + 32] & 0xff) * 0x100
                        + (pcmData[bank][num * 0x12 + 10 + 32] & 0xff) * 0x1_0000
                        + (pcmData[bank][num * 0x12 + 11 + 32] & 0xff) * 0x100_0000
                ;
            }
            chWk[al].loopEndOffset = chWk[al]._loopEndOffset;
            if (chWk[al]._loopEndOffset == -1) {
                chWk[al].loopEndOffset = 0
                        + (pcmData[bank][num * 0x12 + 12 + 32] & 0xff)
                        + (pcmData[bank][num * 0x12 + 13 + 32] & 0xff) * 0x100
                        + (pcmData[bank][num * 0x12 + 14 + 32] & 0xff) * 0x1_0000
                        + (pcmData[bank][num * 0x12 + 15 + 32]  & 0xff)* 0x100_0000
                ;
            }
            if (chWk[al].loopStartOffset == 0xffff && chWk[al].loopEndOffset == 0xffff) {
                chWk[al].loopStartOffset = -1;
                chWk[al].loopEndOffset = -1;
            }

            // Seems unnecessary?
            //chWk[al].srcFrequency = (short)(chWk[al].ptr
            //    + pcmData[bank][num * 0x12 + 16 + 32]
            //    + pcmData[bank][num * 0x12 + 17 + 32] * 0x100
            //    );

            //chWk[al].frequency = chWk[al]._frequency;
            chWk[al].srcFrequency = chWk[al]._srcFrequency;
        }

        interrupt = false;
        chWk[al].playing = true;
    }

    /**
     * 0x02 PCM stopped
     *
     * @param al PCM Channel (0-7)
     */
    public void StopPCM(byte al) {
        logger.log(Level.TRACE, String.format("ppz8em: StopPCM: ch:%d", al));

        chWk[al].playing = false;
    }

    /**
     * 0x03 Load PVI file and convert to PCM
     *
     * @param bank 0: PCM buffer 0 1: PCM buffer 1
     * @param mode 0:.PVI (ADPCM)  1:.PZI(PCM)
     * @param pcmData File Contents
     * <returns></returns>
     */
    public int LoadPcm(byte bank, byte mode, byte[] pcmData) {
        logger.log(Level.TRACE, String.format("ppz8em: LoadPCM: bank:%d mode:%d", bank, mode));

        bank &= 1;
        mode &= 1;
        int ret;
        this.pcmData[bank] = null;

        if (mode == 0) // PVI Format
            ret = CheckPVI(pcmData);
        else // PZI format
            ret = CheckPZI(pcmData);

        if (ret == 0) {
            this.pcmData[bank] = new byte[pcmData.length];
            System.arraycopy(pcmData, 0, this.pcmData[bank], 0, pcmData.length);
            isPVI[bank] = mode == 0;
            if (isPVI[bank]) {
                ret = ConvertPviAdpcmToPziPcm(bank);
            }
        }

        return ret;
    }

    /**
     * 0x04 Loading status
     *
     * @param al 
     */
    public void ReadStatus(byte al) {
        switch (al) {
            case 0xd:
                logger.log(Level.TRACE, "ppz8em: ReadStatus: PCM0 table address");

                bank = 0;
                ptr = 0;
                break;
            case 0xe:
                logger.log(Level.TRACE, "ppz8em: ReadStatus: PCM1 table address");

                bank = 1;
                ptr = 0;
                break;
        }
    }

    /**
     * 0x07 Volume Changes
     *
     * @param al PCM Channel (0-7)
     * @param dx Volume (0-15 / 0-255)
     */
    public void SetVolume(byte al, short dx) {
        logger.log(Level.TRACE, "ppz8em: SetVolume: Ch:%d vol:%d".formatted(al, dx));

        chWk[al].volume = dx;
    }

    /**
     * 0x0B Specifying the PCM pitch frequency
     *
     * @param al PCM Channel (0-7)
     * @param dx PCM Pitch Frequency DX
     * @param cx PCM pitch frequency CX
     */
    public void SetFrequency(byte al, short dx, short cx) {
        logger.log(Level.TRACE, "ppz8em: SetFrequency: 0x%08x".formatted(dx * 0x1_0000 + cx));

        chWk[al].frequency = (dx & 0xffff) * 0x1_0000 + (cx & 0xffff);
    }

    /**
     * 0x0e Setting the Loop Pointer
     *
     * @param al PCM Channel (0-7)
     * @param lpStOfsDX Loop Start Offset DX
     * @param lpStOfsCX Loop Start Offset CX
     * @param lpEdOfsDI Loop End Offset DI
     * @param lpEdOfsSI Loop End Offset SI
     */
    public void SetLoopPoint(byte al, short lpStOfsDX, short lpStOfsCX, short lpEdOfsDI, short lpEdOfsSI) {
        logger.log(Level.TRACE, "ppz8em: SetLoopPoint: St:0x%08x Ed:0x%08x".formatted(
                (lpStOfsDX & 0xffff) * 0x1_0000 + (lpStOfsCX & 0xffff), (lpEdOfsDI & 0xffff) * 0x1_0000 + (lpEdOfsSI & 0xffff)));

        al &= 7;
        chWk[al]._loopStartOffset = (lpStOfsDX & 0xffff) * 0x1_0000 + (lpStOfsCX & 0xffff);
        chWk[al]._loopEndOffset = (lpEdOfsDI & 0xffff) * 0x1_0000 + (lpEdOfsSI & 0xffff);

        if (chWk[al]._loopStartOffset == 0xffff || chWk[al]._loopStartOffset >= chWk[al]._loopEndOffset) {
            chWk[al]._loopStartOffset = -1;
            chWk[al]._loopEndOffset = -1;
        }
    }

    /**
     * 0x12 Stop PCM interrupts
     */
    public void StopInterrupt() {
        logger.log(Level.TRACE, "ppz8em: StopInterrupt");

        interrupt = true;
    }

    /**
     * 0x13 PAN designation
     *
     * @param al PCM Channel (0-7)
     * @param dx PAN(0~9)
     */
    public void SetPan(byte al, short dx) {
        logger.log(Level.TRACE, "ppz8em: SetPan: %d".formatted(dx));

        chWk[al].pan = dx;
        chWk[al].panL = (chWk[al].pan < 6 ? 1.0 : (0.25 * (9 - chWk[al].pan)));
        chWk[al].panR = (chWk[al].pan > 4 ? 1.0 : (0.25 * chWk[al].pan));
    }

    /**
     * 0x15 Original data frequency setting
     *
     * @param al PCM Channel (0-7)
     * @param dx Original Frequency
     */
    public void SetSrcFrequency(byte al, short dx) {
        logger.log(Level.TRACE, "ppz8em: SetSrcFrequency: %d".formatted(dx));

        chWk[al]._srcFrequency = dx;
    }

    /**
     * 0x16 Overall Volume
     */
    public void SetAllVolume(int vol) {
        logger.log(Level.TRACE, "ppz8em: SetAllVolume: %d".formatted(vol));

        if (vol < 16 && vol != PCM_VOLUME) {
            PCM_VOLUME = vol;
            MakeVolumeTable(volume);
        }
    }

    //
    // For volume adjustment
    //
    public void SetVolume(int vol) {
        if (vol != volume) {
            MakeVolumeTable(vol);
        }
    }

    /**
     * 0x18 Channel 7 ADPCM emulation setting
     *
     * @param al 0: Do not emulate ADPCM on channel 7. 1: Emulate.
     */
    public void SetAdpcmEmu(byte al) {
        logger.log(Level.TRACE, "ppz8em: SetAdpcmEmu: %d".formatted(al));

        adpcmEmu = al;
    }

    /**
     * 0x19 Resident disable permission/prohibition setting
     *
     * @param v 0: Permitted to cancel resident mode 1: Prohibited to cancel resident mode
     */
    public void SetReleaseFlag(int v) {
        // Do nothing
    }

    private int CheckPZI(byte[] pcmData) {
        if (pcmData == null)
            return 5;
        if (!(pcmData[0] == 'P' && pcmData[1] == 'Z' && pcmData[2] == 'I'))
            return 2;

        return 0;
    }

    private int CheckPVI(byte[] pcmData) {
        if (pcmData == null)
            return 5;
        if (!(pcmData[0] == 'P' && pcmData[1] == 'V' && pcmData[2] == 'I'))
            return 2;

        return 0;
    }

    public void Update(short[] emuRenderBuf) {
        if (interrupt) return;

        int l = 0, r = 0;
        for (int i = 0; i < 8; i++) {
            if (pcmData[chWk[i].bank] == null) continue;
            if (!chWk[i].playing) continue;
            if (chWk[i].pan == 0) continue;

            if (i == 6) {
                //logger.log(Level.TRACE, VolumeTable[chWk[i].volume][pcmData[chWk[i].bank][chWk[i].ptr]]
                //* chWk[i].panL);
            }

            int n = chWk[i].ptr >= pcmData[chWk[i].bank].length ? 0x80 : pcmData[chWk[i].bank][chWk[i].ptr];
            l += (int) (VolumeTable[chWk[i].volume][n] * chWk[i].panL);
            r += (int) (VolumeTable[chWk[i].volume][n] * chWk[i].panR);
            chWk[i].delta += ((long) chWk[i].srcFrequency * (long) chWk[i].frequency / (long) 0x8000) / SamplingRate;
            chWk[i].ptr += (int) chWk[i].delta;
            chWk[i].delta -= (int) chWk[i].delta;

            if (chWk[i].ptr >= chWk[i].end) {
                if (chWk[i].loopStartOffset != -1) {
                    chWk[i].ptr -= chWk[i].loopEndOffset - chWk[i].loopStartOffset;
                } else {
                    chWk[i].playing = false;
                }
            }
        }

        emuRenderBuf[0] = (short) Math.max(Math.min(emuRenderBuf[0] + l, Short.MAX_VALUE), Short.MIN_VALUE);
        emuRenderBuf[1] = (short) Math.max(Math.min(emuRenderBuf[1] + r, Short.MAX_VALUE), Short.MIN_VALUE);
    }

    private int ConvertPviAdpcmToPziPcm(byte bank) {
        int[] table1 = {
                1, 3, 5, 7, 9, 11, 13, 15,
                -1, -3, -5, -7, -9, -11, -13, -15,
        };
        int[] table2 = {
                57, 57, 57, 57, 77, 102, 128, 153,
                57, 57, 57, 57, 77, 102, 128, 153,
        };

        List<Byte> o = new ArrayList<>();

        // Generating the Header
        o.add((byte) 'P');
        o.add((byte) 'Z');
        o.add((byte) 'I');
        o.add((byte) '1');
        for (int i = 4; i < 0x0b; i++) o.add((byte) 0);
        byte instCount = pcmData[bank][0xb];
        o.add(instCount);
        for (int i = 0xc; i < 0x20; i++) o.add((byte) 0);

        // Tone table conversion
        long size2 = 0;
        for (int i = 0; i < instCount; i++) {
            int startaddress = ((pcmData[bank][i * 4 + 0x10] & 0xff) + (pcmData[bank][i * 4 + 0x11] & 0xff) * 0x100) << (5 + 1);
            int size = (((pcmData[bank][i * 4 + 0x12] & 0xff) + (pcmData[bank][i * 4 + 0x13] & 0xff) * 0x100)
                    - ((pcmData[bank][i * 4 + 0x10] & 0xff) + (pcmData[bank][i * 4 + 0x11] & 0xff) * 0x100) + 1)
                    << (5 + 1); // endAdr - startAdr
            size2 += size;
            short rate = 16000;   // 16kHz

            o.add((byte) startaddress);
            o.add((byte) (startaddress >> 8));
            o.add((byte) (startaddress >> 16));
            o.add((byte) (startaddress >>> 24));
            o.add((byte) size);
            o.add((byte) (size >> 8));
            o.add((byte) (size >> 16));
            o.add((byte) (size >> 24));
            o.add((byte) 0xff);
            o.add((byte) 0xff);
            o.add((byte) 0);
            o.add((byte) 0); // loop_start
            o.add((byte) 0xff);
            o.add((byte) 0xff);
            o.add((byte) 0);
            o.add((byte) 0); // loop_end
            o.add((byte) rate);
            o.add((byte) (rate >> 8)); // rate
        }

        for (int i = instCount; i < 128; i++) {
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0);
            o.add((byte) 0xff);
            o.add((byte) 0xff);
            o.add((byte) 0);
            o.add((byte) 0); // loop_start
            o.add((byte) 0xff);
            o.add((byte) 0xff);
            o.add((byte) 0);
            o.add((byte) 0); // loop_end
            short rate = 16000;   // 16kHz
            o.add((byte) rate);
            o.add((byte) (rate >> 8)); // rate
        }

        // Convert ADPCM to PCM
        int psrcPtr = 0x10 + 4 * 128;
        for (int i = 0; i < instCount; i++) {
            int X_N = 0x80; // Xn (For ADPCM>PCM conversion)
            int DELTA_N = 127; // DELTA_N(For ADPCM>PCM conversion)

            int size = (((pcmData[bank][i * 4 + 0x12] & 0xff) + (pcmData[bank][i * 4 + 0x13] & 0xff) * 0x100)
                    - ((pcmData[bank][i * 4 + 0x10] & 0xff) + (pcmData[bank][i * 4 + 0x11] & 0xff) * 0x100) + 1)
                    << (5 + 1); // endAdr - startAdr

            for (int j = 0; j < size / 2; j++) {
                byte psrc = pcmData[bank][psrcPtr++];

                int n = X_N + table1[(psrc >> 4) & 0x0f] * DELTA_N / 8;
                //logger.log(Level.TRACE, n);
                X_N = Math.max(Math.min(n, 32767), -32768);

                n = DELTA_N * table2[(psrc >> 4) & 0x0f] / 64;
                //logger.log(Level.TRACE, n);
                DELTA_N = Math.max(Math.min(n, 24576), 127);

                o.add((byte) (X_N / (32768 / 128) + 128));


                n = X_N + table1[psrc & 0x0f] * DELTA_N / 8;
                //logger.log(Level.TRACE, n);
                X_N = Math.max(Math.min(n, 32767), -32768);

                n = DELTA_N * table2[psrc & 0x0f] / 64;
                //logger.log(Level.TRACE, n);
                DELTA_N = Math.max(Math.min(n, 24576), 127);

                o.add((byte) (X_N / (32768 / 128) + 128));
            }
        }

        pcmData[bank] = ByteUtil.toByteArray(o);
//        Files.write(Path.of("a.raw"), pcmData[bank]);
        return 0;
    }
}
