package org.hps.readout.ecal;

import org.lcsim.event.GenericObject;
import java.util.Arrays;

//import org.hps.record.evio.EvioEventConstants; // doesn't work

/**
 * TI Trigger Data 
 * @author Nathan Baltzell <baltzell@jlab.org>
 */
public class TIData extends TriggerData {
                       
   
    private int tag=0;
    private long time=0;
    private boolean singles0=false;
    private boolean singles1=false;
    private boolean pairs0=false;
    private boolean pairs1=false;
    private boolean calib=false;
    private boolean pulser=false;

    public TIData(int[] bank) {
        this.bank = Arrays.copyOf(bank, bank.length);
        this.decodeTriggerBank();
    }

    public TIData(GenericObject tiData) {
        this.bank = getBank(tiData);
        this.decodeTriggerBank();
    }

    private void decodeTriggerBank() {
        
        if (bank.length != 4)
        {
            System.err.println("TIData:: Invalid Data Length:  "+bank.length);
            return;
        }

        tag = 0xe10a; // EvioEventConstants.TI_TRIGGER_BANK_TAG;

        singles0 = ((bank[0]>>24)&1)==1;
        singles1 = ((bank[0]>>25)&1)==1;
        pairs0   = ((bank[0]>>26)&1)==1;
        pairs1   = ((bank[0]>>27)&1)==1;
        calib    = ((bank[0]>>28)&1)==1;
        pulser   = ((bank[0]>>29)&1)==1;

        long w1 = bank[2];
        long w2 = bank[3];
        if (w1<0) w1 += 2*(long)Integer.MAX_VALUE+2;
        if (w2<0) w2 += 2*(long)Integer.MAX_VALUE+2;

        final long timelo = w1;
        final long timehi = (w2 & 0xffff) << 32;

        time = 4*(timelo+timehi); // units ns
    }

    public int getTag() {
        return tag;
    }
    @Override
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
    public int[] getBank() {
        return bank;
    }
    public static int[] getBank(GenericObject object) {
        int N = object.getNInt();
        int[] bank = new int[N];
        for (int i = 0; i < N; i++) {
            bank[i] = object.getIntVal(i);
        }
        return bank;
    }
    @Override
    public int getNInt() {
        return bank.length;
    }

    @Override 
    public int getAndTrig() {
        throw new UnsupportedOperationException("No getAndTrig in " + this.getClass().getSimpleName());
    }
    @Override 
    public int getOrTrig() {
        throw new UnsupportedOperationException("No getOrTrig in " + this.getClass().getSimpleName());
    }
    @Override 
    public int getTopTrig() {
        throw new UnsupportedOperationException("No getTopTrig in " + this.getClass().getSimpleName());
    }
    @Override 
    public int getBotTrig() {
        throw new UnsupportedOperationException("No getBotTrig in " + this.getClass().getSimpleName());
    }
    @Override
    public boolean isFixedSize() {
        return true;
    }
}
