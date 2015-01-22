package org.hps.readout.ecal.triggerbank;

import org.lcsim.event.GenericObject;

//import org.hps.record.evio.EvioEventConstants; // doesn't work
/**
 * TI Trigger Data
 *
 * @author Nathan Baltzell <baltzell@jlab.org>
 */
public class TIData extends AbstractIntData {

    public static final int BANK_TAG = 0xe10a; // EvioEventConstants.TI_TRIGGER_BANK_TAG;
    public static final int BANK_SIZE = 4;

    private long time = 0;
    private boolean singles0 = false;
    private boolean singles1 = false;
    private boolean pairs0 = false;
    private boolean pairs1 = false;
    private boolean calib = false;
    private boolean pulser = false;

    public TIData(int[] bank) {
        super(bank);
        decodeData();
    }

    public TIData(GenericObject tiData) {
        super(tiData, BANK_TAG);
        decodeData();
    }

    @Override
    protected final void decodeData() {
        if (this.bank.length != BANK_SIZE) {
            throw new RuntimeException("Invalid Data Length:  " + bank.length);
        }

        singles0 = ((bank[0] >> 24) & 1) == 1;
        singles1 = ((bank[0] >> 25) & 1) == 1;
        pairs0 = ((bank[0] >> 26) & 1) == 1;
        pairs1 = ((bank[0] >> 27) & 1) == 1;
        calib = ((bank[0] >> 28) & 1) == 1;
        pulser = ((bank[0] >> 29) & 1) == 1;

        long w1 = bank[2] & 0xffffffffL;
        long w2 = bank[3] & 0xffffffffL;

        final long timelo = w1;
        final long timehi = (w2 & 0xffff) << 32;

        time = 4 * (timelo + timehi); // units ns
    }

    @Override
    public int getTag() {
        return BANK_TAG;
    }

    public long getTime() {
        return time;
    }

    public boolean isSingle0Trigger() {
        return singles0;
    }

    public boolean isSingle1Trigger() {
        return singles1;
    }

    public boolean isPair0Trigger() {
        return pairs0;
    }

    public boolean isPair1Trigger() {
        return pairs1;
    }

    public boolean isCalibTrigger() {
        return calib;
    }

    public boolean isPulserTrigger() {
        return pulser;
    }
}
