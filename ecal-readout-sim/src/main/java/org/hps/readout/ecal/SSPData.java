package org.hps.readout.ecal;

import org.lcsim.event.GenericObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @author Andrea Celentano <celentan@ge.infn.it>
 * @version $Id: TriggerData.java,v 1.3 2012/08/03 23:14:39 meeg Exp $
 */
public class SSPData extends TriggerData {

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

    public static final String TRIG_COLLECTION = "TriggerBank";
//    private int[] bank;

    private long trigTime;
    private int eventNum;

    private int trigType;
    private int trigTypeData;
    private int trigTypeTime;

    private int nCluster, nClusterTop, nClusterBottom;

    private List<Integer> clusterX, clusterY, clusterE, clusterT, clusterNhits;

    public SSPData(int[] bank) {
        super(bank);

        clusterX = new ArrayList<Integer>();
        clusterY = new ArrayList<Integer>();
        clusterE = new ArrayList<Integer>();
        clusterT = new ArrayList<Integer>();
        clusterNhits = new ArrayList<Integer>();

        trigTime = 0;
        trigType = -1;
        trigTypeData = 0;

        nCluster = nClusterTop = nClusterBottom = 0;

        this.bank = Arrays.copyOf(bank, bank.length);
        this.decodeTriggerBank();

    }

    private void decodeTriggerBank() {
        /*A. Celentano: decode here the trigger bank*/
        /*We do not need to handle block header, block trayler since these are disentagled in the secondary CODA readout list*/
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
                trigType = (this_word >> 23) & 0xf; //this is the trigbit, from 0 to 7
                trigTypeData = (this_word >> 16) & 0x7f;
                trigTypeTime = (this_word >> 16) & 0x3ff;
            } //cluster 
            else if (((this_word >> 27) & (0x1f)) == CLUSTER_TYPE) {
                this_clusterNhits = (this_word >> 23) & 0xf;
                clusterNhits.add(this_clusterNhits);

                this_clusterE = (this_word >> 10) & 0x1fff;
                clusterE.add(this_clusterE);

                this_clusterY = (this_word >> 6) & 0xf;
                clusterY.add(this_clusterY);
                if (this_clusterY > 0) {
                    nClusterTop++;
                } else if (this_clusterY < 0) {
                    nClusterBottom++;
                }

                this_clusterX = (this_word) & 0x3f;
                clusterX.add(this_clusterX);

                this_clusterT = (bank[ii + 1]) & 0x3ff;
                clusterT.add(this_clusterT);

                nCluster++;
            }

        }
    }

    @Override
    public long getTime() {
        return trigTime;
    }

    @Override
    public int getOrTrig() {
        return 0;
    }

    @Override
    public int getTopTrig() {
        return 0;
    }

    @Override
    public int getBotTrig() {
        return 0;
    }

    @Override
    public int getAndTrig() {
        return 0;
    }

    @Override
    public int[] getBank() {
        return bank;
    }

    public static long getTime(GenericObject object) {
        return ((TriggerData) object).getTime();
    }

    public static int getOrTrig(GenericObject object) {
        return ((TriggerData) object).getOrTrig();
    }

    public static int getTopTrig(GenericObject object) {
        return ((TriggerData) object).getTopTrig();
    }

    public static int getBotTrig(GenericObject object) {
        return ((TriggerData) object).getBotTrig();
    }

    public static int getAndTrig(GenericObject object) {
        return ((TriggerData) object).getAndTrig();
    }

    public static int[] getBank(GenericObject object) {
        int N = ((SSPData) object).bank.length;
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
    public int getNFloat() {
        return 0;
    }

    @Override
    public int getNDouble() {
        return 0;
    }

    @Override
    public int getIntVal(int index) {
        return bank[index];
    }

    @Override
    public float getFloatVal(int index) {
        throw new UnsupportedOperationException("No float values in " + this.getClass().getSimpleName());
    }

    @Override
    public double getDoubleVal(int index) {
        throw new UnsupportedOperationException("No double values in " + this.getClass().getSimpleName());
    }

    @Override
    public boolean isFixedSize() {
        return false;
    }
}
