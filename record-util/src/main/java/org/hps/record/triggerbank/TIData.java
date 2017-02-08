package org.hps.record.triggerbank;

import org.lcsim.event.GenericObject;

/**
 * Class <code>TIData</code> is an implementation of abstract class
 * <code>AbstractIntData</code> that represents a TI trigger bit bank.
 */
public class TIData extends AbstractIntData {

    /**
     * The EvIO bank header tag for TI data banks.
     */
    public static final int BANK_TAG = 0xe10a; // EvioEventConstants.TI_TRIGGER_BANK_TAG;
    /**
     * The expected number of entries in the data bank for the 2015 data.
     */
    private static final int BANK_SIZE_2015 = 4;
    /**
     * The expected number of entries in the data bank for the 2016 data (after
     * unprescaled trigger bits were added).
     */
    private static final int BANK_SIZE_2016 = 5;

    // Store the parsed data bank parameters.
    private long time = 0;
    private boolean singles0 = false;
    private boolean singles1 = false;
    private boolean pairs0 = false;
    private boolean pairs1 = false;
    private boolean calib = false;
    private boolean pulser = false;
    private boolean hasUnprescaledTriggerBits = false;
    private boolean singles0Unprescaled = false;
    private boolean singles1Unprescaled = false;
    private boolean pairs0Unprescaled = false;
    private boolean pairs1Unprescaled = false;
    private boolean calibUnprescaled = false;
    private boolean pulserUnprescaled = false;

    /**
     * Creates a <code>TIData</code> bank from a raw EvIO data bank. It is
     * expected that the EvIO reader will verify that the bank tag is of the
     * appropriate type.
     *
     * @param bank - The EvIO data bank.
     */
    public TIData(int[] bank) {
        super(bank);
        decodeData();
    }

    /**
     * Creates a <code>TIData</code> object from an existing LCIO
     * <code>GenericObject</code>.
     *
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
        switch (this.bank.length) {
            case BANK_SIZE_2015:
//                System.out.println("2015-style TI bank");
                break;
            case BANK_SIZE_2016:
//                System.out.format("2016-style TI bank, first word %x, last word %x\n", bank[0], bank[4]);
                hasUnprescaledTriggerBits = true;
                singles0Unprescaled = ((bank[0]) & 1) == 1;
                singles1Unprescaled = ((bank[0] >> 1) & 1) == 1;
                pairs0Unprescaled = ((bank[0] >> 2) & 1) == 1;
                pairs1Unprescaled = ((bank[0] >> 3) & 1) == 1;
                calibUnprescaled = ((bank[0] >> 4) & 1) == 1;
                pulserUnprescaled = ((bank[0] >> 5) & 1) == 1;
                break;
            default:
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

        // interpret time:
        final long w1 = bank[2] & 0xffffffffL;
        final long w2 = bank[3] & 0xffffffffL;
        final long timelo = w1;
        final long timehi = (w2 & 0xffff) << 32;
        time = 4 * (timelo + timehi);
    }

    @Override
    public int getTag() {
        return BANK_TAG;
    }

    public long getTime() {
        return time;
    }

    /**
     * Indicates whether a singles 0 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle0Trigger() {
        return singles0;
    }

    /**
     * Indicates whether a singles 1 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle1Trigger() {
        return singles1;
    }

    /**
     * Indicates whether a pair 0 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair0Trigger() {
        return pairs0;
    }

    /**
     * Indicates whether a pair 1 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair1Trigger() {
        return pairs1;
    }

    /**
     * Indicates whether a cosmic trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isCalibTrigger() {
        return calib;
    }

    /**
     * Indicates whether a random/pulser trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPulserTrigger() {
        return pulser;
    }

    /**
     * Indicates whether this TI data has unprescaled trigger bits.
     *
     * @return Returns <code>true</code> if the TI data has a fifth int
     * containing unprescaled trigger bits, and <code>false</code> otherwise.
     */
    public boolean hasUnprescaledTriggerBits() {
        return hasUnprescaledTriggerBits;
    }

    /**
     * Indicates whether a singles 0 (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isSingle0UnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return singles0Unprescaled;
    }

    /**
     * Indicates whether a singles 1 (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isSingle1UnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return singles1Unprescaled;
    }

    /**
     * Indicates whether a pairs 0 (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isPair0UnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return pairs0Unprescaled;
    }

    /**
     * Indicates whether a pairs 1 (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isPair1UnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return pairs1Unprescaled;
    }

    /**
     * Indicates whether a cosmic (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isCalibUnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return calibUnprescaled;
    }

    /**
     * Indicates whether a random/pulser (unprescaled) trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise. Throws a RuntimeException if this data does
     * not have unprescaled trigger bits.
     */
    public boolean isPulserUnprescaledTrigger() {
        if (!hasUnprescaledTriggerBits) {
            throw new RuntimeException("This TI data does not have unprescaled trigger bits.");
        }
        return pulserUnprescaled;
    }
}
