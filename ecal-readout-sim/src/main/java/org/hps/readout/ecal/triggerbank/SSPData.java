package org.hps.readout.ecal.triggerbank;

import org.lcsim.event.GenericObject;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <celentan@ge.infn.it>
 * @version $Id: TriggerData.java,v 1.3 2012/08/03 23:14:39 meeg Exp $
 */
public class SSPData extends AbstractIntData {

    public static final int BANK_TAG = 0xe10c;

//    public static final int OR_TRIG = 3;
//    public static final int TOP_TRIG = 4;
//    public static final int BOT_TRIG = 5;
//    public static final int AND_TRIG = 6;
//    public static final int TIME = 7;
//    public static final int TRIG_BANK_SIZE = 8;
    //Here goes the 5-bit identifiers for the word type
    public static final int TRIG_HEADER = 0x12;
    public static final int TRIG_TIME = 0x13;
    public static final int TRIG_TYPE = 0x15;
    public static final int CLUSTER_TYPE = 0x14;

    public static final int TRIG_TYPE_COSMIC_TOP = 0x0;
    public static final int TRIG_TYPE_COSMIC_BOT = 0x1;
    public static final int TRIG_TYPE_SINGLES0_TOP = 0x2;
    public static final int TRIG_TYPE_SINGLES0_BOT = 0x3;
    public static final int TRIG_TYPE_SINGLES1_TOP = 0x4;
    public static final int TRIG_TYPE_SINGLES1_BOT = 0x5;
    public static final int TRIG_TYPE_PAIR0 = 0x6;
    public static final int TRIG_TYPE_PAIR1 = 0x7;

//    public static final String TRIG_COLLECTION = "TriggerBank";
    private long trigTime = 0;
    private int eventNum;

    private int nCluster, nClusterTop, nClusterBottom = 0;

    private final List<Integer> clusterX = new ArrayList<Integer>();
    private final List<Integer> clusterY = new ArrayList<Integer>();
    private final List<Integer> clusterE = new ArrayList<Integer>();
    private final List<Integer> clusterT = new ArrayList<Integer>();
    private final List<Integer> clusterNhits = new ArrayList<Integer>();
    private final List<Integer> trigType = new ArrayList<Integer>();
    private final List<Integer> trigTypeData = new ArrayList<Integer>();
    private final List<Integer> trigTypeTime = new ArrayList<Integer>(); //SSP can report more than 1 trigger type (if the event satisfies more than 1 trigger equation)

    public SSPData(int[] bank) {
        super(bank);
        decodeData();
    }

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
        /*A. C.: decode here the trigger bank*/
        /*We do not need to handle block header, block trailer since these are disentagled in the secondary CODA readout list*/
        int this_word;
        int this_clusterX, this_clusterY, this_clusterE, this_clusterT, this_clusterNhits;

        for (int ii = 0; ii < bank.length; ii++) {
            this_word = bank[ii];

            //event header
            if (((this_word >> 27) & (0x1f)) == TRIG_HEADER) {
                eventNum = this_word & 0x7FFFFFF;
            } //trigger time
            else if (((this_word >> 27) & (0x1f)) == TRIG_TIME) {
                trigTime = (bank[ii + 1] << 24) | (this_word & 0xffffff);
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
                
                if (this_clusterY > 0) {
                    nClusterTop++;
                } else if (this_clusterY < 0) {
                    nClusterBottom++;
                }

                this_clusterX = (this_word) & 0x3f;    
                /*Need to do hand-made 2 complement, since this is a 6-bit word (and not a 32-bit integer!)*/
                if (((this_clusterX >> 5) & 0x1) == 0x1){ /*Negative number*/  	
                	this_clusterX = this_clusterX ^ (0x3f); /*bit-wise inversion*/
                	this_clusterX += 1;
                	this_clusterX *=-1;
                }             
                clusterX.add(this_clusterX);

                this_clusterT = (bank[ii + 1]) & 0x3ff;
                clusterT.add(this_clusterT * 4); //*4 since the time is reported in 4 ns ticks

                nCluster++;
            }
        }
    }

    public long getTime() {
        return trigTime;
    }

    /*
     * Returns the trigger time, relative to the SSP window, of the FIRST Cluster singles trigger (0/1) (any crate)
     * Returns in ns.
     */
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
    public int getBotTrig() {
        int BotTime = 1025; //time is 10 bits, so is always smaller than 1024.
        for (int ii = 0; ii < trigType.size(); ii++) {
            if (((trigType.get(ii) == TRIG_TYPE_SINGLES0_BOT) || (trigType.get(ii) == TRIG_TYPE_SINGLES1_BOT)) && (trigTypeTime.get(ii) < BotTime)) {
                BotTime = trigTypeTime.get(ii);
            }
        }
        return BotTime * 4;
    }

    public int getAndTrig() {
        return 0;
    }
}
