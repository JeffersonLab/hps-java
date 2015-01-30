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
    
    // TODO: These should not be persisted past their use in initial
    //        bank parsing They are now superseded by the SSPCluster
    //        and SSPTrigger objects, which contain the same information
    //        in a more user-friendly fashion. These should be removed
    //        once all functions calling on them have been modified to
    //        use the SSPCluster and SSPTrigger collections instead.
    private final List<Integer> clusterX = new ArrayList<Integer>();
    private final List<Integer> clusterY = new ArrayList<Integer>();
    private final List<Integer> clusterE = new ArrayList<Integer>();
    private final List<Integer> clusterT = new ArrayList<Integer>();
    private final List<Integer> clusterNhits = new ArrayList<Integer>();
    private final List<Integer> trigType = new ArrayList<Integer>();
    private final List<Integer> trigTypeData = new ArrayList<Integer>();
    private final List<Integer> trigTypeTime = new ArrayList<Integer>(); //SSP can report more than 1 trigger type (if the event satisfies more than 1 trigger equation)
    
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
    
    // NOTE :: Cluster time is already corrected to be in ns; trigger
    //         time was left in units of clock-cycles!
    @Override
    protected final void decodeData() {
        /*A. C.: decode here the trigger bank*/
        /*We do not need to handle block header, block trailer since these are disentagled in the secondary CODA readout list*/
        int this_word;
        int this_clusterX, this_clusterY, this_clusterE, this_clusterT, this_clusterNhits;

        for (int ii = 0; ii < bank.length; ii++) {
            this_word = bank[ii];

            //event header
            if (((this_word >> 27) & (0x1f)) == TRIG_HEADER) {
            	eventNumber = this_word & 0x7FFFFFF;
            }
            //trigger time
            else if (((this_word >> 27) & (0x1f)) == TRIG_TIME) {
            	triggerTime = (bank[ii + 1] << 24) | (this_word & 0xffffff);
            } //trigger type
            else if (((this_word >> 27) & (0x1f)) == TRIG_TYPE) {
                trigType.add((this_word >> 23) & 0xf); //this is the trigbit, from 0 to 7
                trigTypeData.add((this_word >> 16) & 0x7f);
                trigTypeTime.add((this_word) & 0x3ff);
            } //cluster 
            else if (((this_word >> 27) & (0x1f)) == CLUSTER_TYPE) {
                this_clusterNhits = (this_word >> 23) & 0xf;
                clusterNhits.add(this_clusterNhits);

                this_clusterE = (this_word >> 10) & 0x1fff;
                clusterE.add(this_clusterE);

                this_clusterY = (this_word >> 6) & 0xf;
                /*Need to do hand-made 2 complement, since this is a 4-bit word (and not a 32-bit integer!)*/
                if (((this_clusterY >> 3) & 0x1) == 0x1){ /*Negative number*/  	
                	this_clusterY = this_clusterY ^ (0xf); /*bit-wise inversion*/
                	this_clusterY += 1;
                	this_clusterY *=-1;
                }
                clusterY.add(this_clusterY);

                this_clusterX = (this_word) & 0x3f;    
                /*Need to do hand-made 2 complement, since this is a 6-bit word (and not a 32-bit integer!)*/
                if (((this_clusterX >> 5) & 0x1) == 0x1){ /*Negative number*/  	
                	this_clusterX = this_clusterX ^ (0x3f); /*bit-wise inversion*/
                	this_clusterX += 1;
                	this_clusterX *=-1;
			this_clusterX -= 1; //A.C. Correction due to X= -23 .. (0  excluded) ... + 23
                }             
                clusterX.add(this_clusterX);

                this_clusterT = (bank[ii + 1]) & 0x3ff;
                clusterT.add(this_clusterT * 4); //*4 since the time is reported in 4 ns ticks
            }
        }
        
        // Convert the cluster lists into a single list of SSPCluster
        // objects and place them into the cluster list.
        int clusters = clusterX.size();
        for(int i = 0; i < clusters; i++) {
        	SSPCluster cluster = new SSPCluster(clusterX.get(i), clusterY.get(i),
        			clusterE.get(i), clusterNhits.get(i), clusterT.get(i));
        	clusterList.add(cluster);
        }
        
        // Convert the trigger lists into a single list of SSPTrigger
        // objects and place them into the trigger list.
        int triggers = trigType.size();
        for(int i = 0; i < triggers; i++) {
        	SSPTrigger trigger = SSPTriggerFactory.makeTrigger(trigType.get(i),
        			trigTypeTime.get(i) * 4, trigTypeData.get(i));
        	triggerList.add(trigger);
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
    
    /*
     * Returns the trigger time, relative to the SSP window, of the FIRST Cluster singles trigger (0/1) (any crate)
     * Returns in ns.
     */
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    public int getOrTrig() {
        int TopTime = this.getTopTrig();
        int BotTime = this.getBotTrig();

        if (TopTime <= BotTime) {
            return TopTime;
        } else {
            return BotTime;
        }

    }
    
    /*
     * Returns the trigger time, relative to the SSP window, of the FIRST Cluster singles trigger (0/1) from TOP crate 
     * Returns in ns.
     */
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    public int getTopTrig() {
        int TopTime = 1025; //time is 10 bits, so is always smaller than 1024.
        for (int ii = 0; ii < trigType.size(); ii++) {
            if (((trigType.get(ii) == TRIG_TYPE_SINGLES0_TOP) || (trigType.get(ii) == TRIG_TYPE_SINGLES1_TOP)) && (trigTypeTime.get(ii) < TopTime)) {
                TopTime = trigTypeTime.get(ii);
            }
        }
        return TopTime * 4;
    }
    
    /*
     * Returns the trigger time, relative to the SSP window, of the FIRST Cluster singles trigger (0/1) from BOT crate 
     * Returns in ns.
     */
    // TODO: Get information from Andrea on what this is for. It seems
    //       to be something specialized. Maybe it should be placed in
    //       the analysis driver in which it is used?
    public int getBotTrig() {
        int BotTime = 1025; //time is 10 bits, so is always smaller than 1024.
        for (int ii = 0; ii < trigType.size(); ii++) {
            if (((trigType.get(ii) == TRIG_TYPE_SINGLES0_BOT) || (trigType.get(ii) == TRIG_TYPE_SINGLES1_BOT)) && (trigTypeTime.get(ii) < BotTime)) {
                BotTime = trigTypeTime.get(ii);
            }
        }
        return BotTime * 4;
    }
    
    // TODO: This does not seem to do anything. Can it be deleted?
    public int getAndTrig() {
        return 0;
    }
}
