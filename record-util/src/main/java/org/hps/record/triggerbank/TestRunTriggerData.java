package org.hps.record.triggerbank;

import org.lcsim.event.GenericObject;

public class TestRunTriggerData extends AbstractIntData {
    
    public static final int BANK_TAG = 0xe106;
    public static final int BANK_SIZE = 8;
    
    public static final int OR_TRIG = 3;
    public static final int TOP_TRIG = 4;
    public static final int BOT_TRIG = 5;
    public static final int AND_TRIG = 6;
    public static final int TIME = 7;
    public static final String TRIG_COLLECTION = "TriggerBank";
    
    public TestRunTriggerData(int[] bank) {
        super(bank);
        decodeData();
    }
    
    public TestRunTriggerData(GenericObject data) {
        super(data, BANK_TAG);
        decodeData();
    }
    
    public long getTime() {
        return bank[TIME] & 0xffffffffL;
    }
    
    public int getOrTrig() {
        return bank[OR_TRIG];
    }
    
    public int getTopTrig() {
        return bank[TOP_TRIG];
    }
    
    public int getBotTrig() {
        return bank[BOT_TRIG];
    }
    
    public int getAndTrig() {
        return bank[AND_TRIG];
    }
    
    public static long getTime(GenericObject object) {
        return AbstractIntData.getBankInt(object, TIME) & 0xffffffffL;
    }
    
    public static int getOrTrig(GenericObject object) {
        return AbstractIntData.getBankInt(object, OR_TRIG);
    }
    
    public static int getTopTrig(GenericObject object) {
        return AbstractIntData.getBankInt(object, TOP_TRIG);
    }
    
    public static int getBotTrig(GenericObject object) {
        return AbstractIntData.getBankInt(object, BOT_TRIG);
    }
    
    public static int getAndTrig(GenericObject object) {
        return AbstractIntData.getBankInt(object, AND_TRIG);
    }
    
    @Override
    public int getTag() {
        return BANK_TAG;
    }
    
    @Override
    protected final void decodeData() { //doesn't actually do anything since there is no decoding done on the ints
        if (this.bank.length != BANK_SIZE) {
            throw new RuntimeException("Invalid Data Length:  " + bank.length);
        }
    }
}
