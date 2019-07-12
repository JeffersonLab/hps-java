package org.hps.record.triggerbank;

import org.hps.record.evio.EvioEventConstants;
import org.lcsim.event.GenericObject;
import java.util.BitSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class <code>TSData</code> is an implementation of the abstract class <code>AbstractIntData</code> 
 * In EvIO TS bank, there are 7 words. 
 * Words[2] and words[3] include time information.
 * Bit 15 of words[5] indicates if Faraday Cup trigger was registered. 
 * Words[4] indicates if other triggers were registered. The trigger-bit definition in the trigger configuration file is list:
 *
 * VTP_HPS_PRESCALE               0        0   # Single 0 Top    ( 150-8191) MeV (-31,31)   Low energy cluster
 * VTP_HPS_PRESCALE               1        0   # Single 1 Top    ( 300-3000) MeV (  5,31)   e+
 * VTP_HPS_PRESCALE               2        0   # Single 2 Top    ( 300-3000) MeV (  5,31)   e+ : Position dependent energy cut
 * VTP_HPS_PRESCALE               3        0   # Single 3 Top    ( 300-3000) MeV (  5,31)   e+ : HODO L1*L2  Match with cluster
 * VTP_HPS_PRESCALE               4        0   # Single 0 Bot    ( 150-8191) MeV (-31,31)   Low energy cluster
 * VTP_HPS_PRESCALE               5        0   # Single 1 Bot    ( 300-3000) MeV (  5,31)   e+
 * VTP_HPS_PRESCALE               6        0   # Single 2 Bot    ( 300-3000) MeV (  5,31)   e+ : Position dependent energy cut
 * VTP_HPS_PRESCALE               7        0   # Single 3 Bot    ( 300-3000) MeV (  5,31)   e+ : HODO L1*L2  Match with cluster
 * VTP_HPS_PRESCALE               8        0   # Pair 0          A'
 * VTP_HPS_PRESCALE               9        0   # Pair 1          Moller
 * VTP_HPS_PRESCALE               10       0   # Pair 2          pi0
 * VTP_HPS_PRESCALE               11       0   # Pair 3          -
 * VTP_HPS_PRESCALE               12       0   # LED
 * VTP_HPS_PRESCALE               13       0   # Cosmic
 * VTP_HPS_PRESCALE               14       0   # Hodoscope
 * VTP_HPS_PRESCALE               15       0   # Pulser
 * VTP_HPS_PRESCALE               16       0   # Multiplicity-0 2 Cluster Trigger
 * VTP_HPS_PRESCALE               17       0   # Multiplicity-1 3 Cluster trigger
 * VTP_HPS_PRESCALE               18       0   # FEE Top       ( 2600-5200)
 * VTP_HPS_PRESCALE               19       0   # FEE Bot       ( 2600-5200)
 *
 * @author tongtong
 */
public class TSData2019 extends AbstractIntData {

    /**
     * Setup logger.
     */
    private static final Logger LOGGER = Logger.getLogger(TSData2019.class.getPackage().getName());
    
    /**
     * The EvIO bank header tag for TS data banks.
     */
    public static final int BANK_TAG = EvioEventConstants.TS_BANK_TAG; // TS bank tag

    // Store the parsed data bank parameters.
    private long time = 0;
    private boolean faradayCup = false;
    private BitSet bits = new BitSet(32);


    /**
     * Creates a <code>TSData2019</code> object from a raw EvIO data bank. It is
     * expected that the EvIO reader will verify that the bank tag is of the
     * appropriate type.
     *
     * @param bank - The EvIO data bank.
     */
    public TSData2019(int[] bank) {
        super(bank);
        decodeData();
    }

    /**
     * Creates a <code>TSData2019</code> object from an existing LCIO
     * <code>GenericObject</code>.
     *
     * @param tsBank - The source data bank object.
     */
    public TSData2019(GenericObject tsBank) {
        super(tsBank, BANK_TAG);
        decodeData();
    }

    @Override
    protected final void decodeData() {
        // save 32 bits in words[4] into a set
        bits = BitSet.valueOf(new long [] {bank[4]});
        
        // Check Faraday Cup trigger to see if it is active by bit 15 of words[5] in TS bank of EVIO
        faradayCup = ((bank[5] >> 15) & 1) == 1;

        // interpret time using low 32 bits by words[2] and high 16 bits by bits 15:0 of words[3]; 4 ns steps
        time = 4 * (bank[2] + ((long)(bank[3] & 0xffff) << 32));
    }

    @Override
    public int getTag() {
        return BANK_TAG;
    }

    public long getTime() {
        return time;
    }
        
    /**
     * Indicates whether a Faraday Cup trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isFaradayCupTrigger() {
        return faradayCup;
    }
    
    /**
      * @return Returns trigger bits which are saved in a BitSet class.
     */
    public BitSet getBitSet() {
        return bits;
    }
    
    /**
     * Check if a trigger was registered.
     * 
     * @param bitIndex - index of a trigger bit.
     * 
     * @return Returns <code>true</code> if the corresponding trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean checkTrigger(int bitIndex) {
        if(bitIndex >= 32) throw new RuntimeException("Index " + bitIndex + " is out of range (0 : 31)");
        else return bits.get(bitIndex);
    }
    
    /**
     * List indices of registered triggers.
     */
    public String listIndicesofRegisteredTriggers() {
        return bits.toString();
    }
    
    /**
     * Save indices of registered triggers into an integer array.
     */
    public int[] getIndicesOfRegisteredTriggers() {
        String str = bits.toString();
        String[] items = str.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("\\s", "").split(",");
        int[] arr = new int[items.length];
        for (int i = 0; i < items.length; i++) {
            try {
                arr[i] = Integer.parseInt(items[i]);
            } catch (NumberFormatException e) {
                LOGGER.log(Level.SEVERE, "parsing a string as a signed integer is failed", e);
            };
        }
        return arr;
    }
    
    /**
     * Get number of registered triggers.
     */
    public int getNumberOfRegisteredTriggers() {
        return this.getIndicesOfRegisteredTriggers().length;
    }
    
    /**
     * Indicates whether a single 0 Top trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle0TopTrigger() {
        return bits.get(0); // bit 0 indicates single 0 Top trigger
    }
    
    /**
     * Indicates whether a single 1 Top trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle1TopTrigger() {
        return bits.get(1); // bit 1 indicates single 1 Top trigger
    }
    
    /**
     * Indicates whether a single 2 Top trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle2TopTrigger() {
        return bits.get(2); // bit 2 indicates single 2 Top trigger
    }
    
    /**
     * Indicates whether a single 3 Top trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle3TopTrigger() {
        return bits.get(3); // bit 3 indicates single 3 Top trigger
    }
    
    /**
     * Indicates whether a single 0 Bot trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle0BotTrigger() {
        return bits.get(4); // bit 4 indicates single 0 Bot trigger
    }
    
    /**
     * Indicates whether a single 1 Bot trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle1BotTrigger() {
        return bits.get(5); // bit 5 indicates single 1 Bot trigger
    }
    
    /**
     * Indicates whether a single 2 Bot trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle2BotTrigger() {
        return bits.get(6); // bit 6 indicates single 2 Bot trigger
    }
    
    /**
     * Indicates whether a single 3 Bot trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isSingle3BotTrigger() {
        return bits.get(7); // bit 7 indicates single 3 Bot trigger
    }
    
    /**
     * Indicates whether a pair 0 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair0Trigger() {
        return bits.get(8); // bit 8 indicates pair 0 trigger
    }
    
    /**
     * Indicates whether a pair 1 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair1Trigger() {
        return bits.get(9); // bit 9 indicates pair 1 trigger
    }
    
    /**
     * Indicates whether a pair 2 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair2Trigger() {
        return bits.get(10); // bit 10 indicates pair 2 trigger
    }
    
    /**
     * Indicates whether a pair 3 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPair3Trigger() {
        return bits.get(11); // bit 11 indicates pair 3 trigger
    }
    
    /**
     * Indicates whether a LED trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isLEDTrigger() {
        return bits.get(12); // bit 12 indicates LED trigger
    }
    
    /**
     * Indicates whether a Cosmic trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isCosmicTrigger() {
        return bits.get(13); // bit 13 indicates Cosmic trigger
    }
    
    /**
     * Indicates whether a Hodoscope trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isHodoscopeTrigger() {
        return bits.get(14); // bit 14 indicates Hodoscope trigger
    }
    
    /**
     * Indicates whether a Pulser trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isPulserTrigger() {
        return bits.get(15); // bit 15 indicates Pulser trigger
    }
    
    /**
     * Indicates whether a Multiplicity-0 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isMultiplicity0Trigger() {
        return bits.get(16); // bit 16 indicates Multiplicity-0 trigger
    }
    
    /**
     * Indicates whether a Multiplicity-1 trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isMultiplicity1Trigger() {
        return bits.get(17); // bit 17 indicates Multiplicity-1 trigger
    }
    
    /**
     * Indicates whether a FEE Top trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isFEETopTrigger() {
        return bits.get(18); // bit 18 indicates FEE Top trigger
    }
    
    /**
     * Indicates whether a FEE Bot trigger was registered.
     *
     * @return Returns <code>true</code> if the trigger occurred, and
     * <code>false</code> otherwise.
     */
    public boolean isFEEBotTrigger() {
        return bits.get(19); // bit 19 indicates FEE Bot trigger
    }
}
