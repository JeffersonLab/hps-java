package org.hps.readout.ecal.triggerbank;

import org.lcsim.event.GenericObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the trigger bank information in the SSP trigger bank and
 * converts it into <code>SSPCluster</code> and <code>SSPTrigger</code>
 * objects.
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <celentan@ge.infn.it>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SSPData extends AbstractIntData {
    // The EVIO header tag for SSP trigger banks.
    public static final int BANK_TAG = 0xe10c;
    
    // EVIO 5-bit word identifiers for cluster parameters.
    public static final int TRIG_HEADER = 0x12;
    public static final int TRIG_TIME = 0x13;
    public static final int TRIG_TYPE = 0x15;
    public static final int CLUSTER_TYPE = 0x14;
    
    // EVIO trigger type identifiers.
    public static final int TRIG_TYPE_COSMIC_TOP = 0x0;
    public static final int TRIG_TYPE_COSMIC_BOT = 0x1;
    public static final int TRIG_TYPE_SINGLES0_TOP = 0x2;
    public static final int TRIG_TYPE_SINGLES0_BOT = 0x3;
    public static final int TRIG_TYPE_SINGLES1_TOP = 0x4;
    public static final int TRIG_TYPE_SINGLES1_BOT = 0x5;
    public static final int TRIG_TYPE_PAIR0 = 0x6;
    public static final int TRIG_TYPE_PAIR1 = 0x7;
    
    // Collections for storing the decoded SSP bank data.
    private final List<SSPCluster> clusterList = new ArrayList<SSPCluster>();
    private final List<SSPTrigger> triggerList = new ArrayList<SSPTrigger>();
    
    // Other SSP bank information.
    private int eventNumber = 0;
    private long triggerTime = 0;
    
    /**
     * Instantiates an <code>SSPData</code> object from a bank of
     * integer primitives defining an EVIO-encoded SSP trigger bank.
     * @param bank - The EVIO bank from which to generate the object.
     */
    public SSPData(int[] bank) {
        super(bank);
        decodeData();
    }
    
    /**
     * Instantiates an <code>SSPData</code> object from an LCIO
     * <code>GenericObject</code> object that contains an EVIO-encoded
     * bank of integer primitives which represent an SSP trigger bank.
     * @param sspData - The <code>GenericObject</code> containing the
     * integer bank from which to generate the object.
     */
    public SSPData(GenericObject sspData) {
        super(sspData, BANK_TAG);
        decodeData();
    }
    
    @Override
    public int getTag() {
        return BANK_TAG;
    }
    
    @Override
    protected final void decodeData() {
        // Parse over the integer EVIO words and handle each type. Block
        // headers and block trailers can be ignored because these are
        // disentangled in the secondary CODA readout list.
        for (int ii = 0; ii < bank.length; ii++) {
            // Process the event number.
            if (((bank[ii] >> 27) & (0x1f)) == TRIG_HEADER) {
                eventNumber = bank[ii] & 0x7FFFFFF;
            }
            
            // Process the trigger time.
            else if (((bank[ii] >> 27) & (0x1f)) == TRIG_TIME) {
                triggerTime = (bank[ii + 1] << 24) | (bank[ii] & 0xffffff);
            }
            
            // Process SSP trigger data.
            else if (((bank[ii] >> 27) & (0x1f)) == TRIG_TYPE) {
                // Parse the trigger information. Note that type is
                // the trigger identification bits and ranges from
                // zero to seven.
                int type = (bank[ii] >> 23) & 0xf;
                int data = (bank[ii] >> 16) & 0x7f;
                int time = (bank[ii]) & 0x3ff; 
                
                // Create an SSPTrigger and add it to the list.
                SSPTrigger trigger = SSPTriggerFactory.makeTrigger(type, time * 4, data);
                triggerList.add(trigger);
            }
            
            // Process SSP clusters.
            else if (((bank[ii] >> 27) & (0x1f)) == CLUSTER_TYPE) {
                // Get the number of hits in the cluster and add it
                // to the cluster hits list.
                int hits = (bank[ii] >> 23) & 0xf;
                
                // Get the cluster energy (which is in MeV) and add it
                // to the cluster energy list.
                int energy = (bank[ii] >> 10) & 0x1fff;
                
                // Get the cluster y-index.
                int iy = (bank[ii] >> 6) & 0xf;
                
                // If the first bit of the index is 1, then it is a
                // negative number and needs to be converted using
                // two's complement to get the proper value.
                if(((iy >> 3) & 0x1) == 0x1) {      
                    // Perform the two's complement. ('^' is the bit
                    // wise inversion operator).
                    iy = iy ^ (0xf);
                    iy += 1;
                    iy *=-1;
                }
                
                // Get the x-index of the cluster.
                int ix = (bank[ii]) & 0x3f;
                
                // If the first bit of the index is 1, then it is a
                // negative number and needs to be converted using
                // two's complement to get the proper value.
                if(((ix >> 5) & 0x1) == 0x1) {
                    // Perform the two's complement. ('^' is the bit
                    // wise inversion operator).
                    ix = ix ^ (0x3f);
                    ix += 1;
                    ix *=-1;
                    
                    // Values are encoded from -22 to 23; since LCSIM
                    // defines them from -23 to -1 and 1 to 23, negative
                    // values need to be shifted down by an additional
                    // step to be accurate.
                    ix -= 1;
                }
                
                // Get the cluster time. Time is 4 ns clock-cycles.
                int time = (bank[ii + 1]) & 0x3ff;
                
                // Create an SSPCluster from the parsed information
                // and add it to the cluster list.
                SSPCluster cluster = new SSPCluster(ix, iy, energy, hits, time * 4);
                clusterList.add(cluster);
            }
        }
    }
    
    /**
     * Gets the list of clusters reported by the SSP.
     * @return Returns the clusters as a <code>List</code> collection
     * of <code>SSPCluster</code> objects.
     */
    public List<SSPCluster> getClusters() {
        return clusterList;
    }
    
    /**
     * Gets the list of triggers reported by the SSP.
     * @return Returns the triggers as a <code>List</code> collection
     * of <code>SSPTrigger</code> objects. These can vary in which
     * subclass they are, as appropriate to their type code.
     */
    public List<SSPTrigger> getTriggers() {
        return triggerList;
    }
    
    /**
     * Gets the trigger time reported by the SSP.
     * @return Returns the trigger time as a <code>long</code>.
     */
    public long getTime() { return triggerTime; }
    
    /**
     * Gets the event number reported by the SSP.
     * @return Returns the event number as an <code>int</code>.
     */
    public int getEventNumber() { return eventNumber; }
    
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    /**
     * Gets the first singles trigger that occurred in either crate.
     * If no singles triggered occurred, a value of <code>1025 * 4 =
     * 4100</code> will be returned.
     * @return Returns the time in nanoseconds of the earliest singles
     * trigger that occurred, or <code>4100</code> if no singles trigger
     * has occurred.
     */
    @Deprecated
    public int getOrTrig() {
        // Get the earliest time for both the top and bottom triggers.
        int topTime = getTopTrig();
        int bottomTime = getBotTrig();
        
        // Return whichever crate had the earliest trigger.
        if (topTime <= bottomTime) { return topTime; }
        else { return bottomTime; }
    }
    
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    /**
     * Gets the first singles trigger that occurred in the top crate.
     * If no singles triggered occurred, a value of <code>1025 * 4 =
     * 4100</code> will be returned.
     * @return Returns the time in nanoseconds of the earliest singles
     * trigger that occurred, or <code>4100</code> if no singles trigger
     * has occurred.
     */
    @Deprecated
    public int getTopTrig() {
        // Store the smallest found time. The time is a 10 bit value,
        // so it must always be less than 1024. Multiply by 4 to convert
        // from clock-cycles to nanoseconds.
        int topTime = 1025 * 4;
        
        // Iterate over all triggers.
        for(SSPTrigger trigger : triggerList) {
            // Select only singles triggers from the top crate.
            if(trigger instanceof SSPSinglesTrigger && ((SSPSinglesTrigger) trigger).isTop()) {
                // Store the smallest trigger time found.
                if(trigger.getTime() < topTime) {
                    topTime = trigger.getTime();
                }
            }
        }
        
        // Return the smallest found time.
        return topTime;
    }
    
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    /**
     * Gets the first singles trigger that occurred in the bottom crate.
     * If no singles triggered occurred, a value of <code>1025 * 4 =
     * 4100</code> will be returned.
     * @return Returns the time in nanoseconds of the earliest singles
     * trigger that occurred, or <code>4100</code> if no singles trigger
     * has occurred.
     */
    @Deprecated
    public int getBotTrig() {
        // Store the smallest found time. The time is a 10 bit value,
        // so it must always be less than 1024. Multiply by 4 to convert
        // from clock-cycles to nanoseconds.
        int bottomTime = 1025 * 4;
        
        // Iterate over all triggers.
        for(SSPTrigger trigger : triggerList) {
            // Select only singles triggers from the bottom crate.
            if(trigger instanceof SSPSinglesTrigger && ((SSPSinglesTrigger) trigger).isBottom()) {
                // Store the smallest trigger time found.
                if(trigger.getTime() < bottomTime) {
                    bottomTime = trigger.getTime();
                }
            }
        }
        
        // Return the smallest found time.
        return bottomTime;
    }
    
    // TODO: This does not seem to do anything. Can it be deleted? It
    //        is also not used anywhere.
    @Deprecated
    public int getAndTrig() {
        return 0;
    }
}