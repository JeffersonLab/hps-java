package org.hps.record.triggerbank;

import org.lcsim.event.GenericObject;

/**
 * Class <code>TIData</code> is an implementation of abstract class
 * <code>AbstractIntData</code> that represents a TI trigger bit bank.
 * It contains both a time window length and a set of flags that track
 * whether a trigger of a given type was registered with the event to
 * which this bank is attached.
 *
 * @author Nathan Baltzell <baltzell@jlab.org>
 */
public class TIData extends AbstractIntData {
	/** The EvIO bank header tag for TI data banks. */
    public static final int BANK_TAG = 0xe10a; // EvioEventConstants.TI_TRIGGER_BANK_TAG;
    /** The expected number of entries in the data bank. */
    public static final int BANK_SIZE = 4;
    
    // Store the parsed data bank parameters.
    private long time = 0;
    private boolean singles0 = false;
    private boolean singles1 = false;
    private boolean pairs0 = false;
    private boolean pairs1 = false;
    private boolean calib = false;
    private boolean pulser = false;
    
    /**
     * Creates a <code>TIData</code> bank from a raw EvIO data bank.
     * It is expected that the EvIO reader will verify that the bank
     * tag is of the appropriate type.
     * @param bank - The EvIO data bank.
     */
    public TIData(int[] bank) {
        super(bank);
        decodeData();
    }
    
    /**
     * Creates a <code>TIData</code> object from an existing LCIO
     * <code>GenericObject</code>.
     * @param tiData - The source data bank object.
     */
    public TIData(GenericObject tiData) {
        super(tiData, BANK_TAG);
        decodeData();
    }
    
    @Override
    protected final void decodeData() {
    	// Check that the data bank is the expected size. If not, throw
    	// and exception.
        if(this.bank.length != BANK_SIZE) {
            throw new RuntimeException("Invalid Data Length:  " + bank.length);
        }
        
        // Check each trigger bit to see if it is active. A value of 
        // 1 indicates a trigger of that type occurred, and 0 that it
        // did not.
        singles0 = ((bank[0] >> 24) & 1) == 1;
        singles1 = ((bank[0] >> 25) & 1) == 1;
        pairs0 = ((bank[0] >> 26) & 1) == 1;
        pairs1 = ((bank[0] >> 27) & 1) == 1;
        calib = ((bank[0] >> 28) & 1) == 1;
        pulser = ((bank[0] >> 29) & 1) == 1;
        
        // Get the unprocessed start and end times for the bank.
        long w1 = bank[2] & 0xffffffffL;
        long w2 = bank[3] & 0xffffffffL;
        
        // Process the times into units of clock-cycles.
        final long timelo = w1;
        final long timehi = (w2 & 0xffff) << 32;
        
        // Store the time difference in nanoseconds.
        time = 4 * (timelo + timehi);
    }
    
    @Override
    public int getTag() {
        return BANK_TAG;
    }
    
    /**
     * Gets the time window for the bank.
     * @return Returns the time window length in nanoseconds.
     */
    public long getTime() {
        return time;
    }
    
    /**
     * Indicates whether a singles 0 trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle0Trigger() {
        return singles0;
    }
    
    /**
     * Indicates whether a singles 1 trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle1Trigger() {
        return singles1;
    }
    
    /**
     * Indicates whether a pair 0 trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair0Trigger() {
        return pairs0;
    }
    
    /**
     * Indicates whether a pair 1 trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair1Trigger() {
        return pairs1;
    }
    
    /**
     * Indicates whether a cosmic trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isCalibTrigger() {
        return calib;
    }
    
    /**
     * Indicates whether a random/pulser trigger was registered.
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPulserTrigger() {
        return pulser;
    }
}