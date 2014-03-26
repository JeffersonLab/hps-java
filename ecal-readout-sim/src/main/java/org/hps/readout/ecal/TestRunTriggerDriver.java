package org.hps.readout.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.hps.recon.ecal.TriggerData;
import org.lcsim.event.EventHeader;

/**
 * Reads clusters and makes trigger decision using opposite quadrant criterion.
 * Prints triggers to file if file path specified.
 *
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: TestRunTriggerDriver.java,v 1.2 2013/03/20 01:21:29 meeg Exp $
 */
public class TestRunTriggerDriver extends TriggerDriver {

    boolean triggerThisCycle = false;
    int cycleCounter = 0;
    private double clusterEnergyLow = 10;    //
    int deadtimelessTriggerCount;
    private int topBits = 0, botBits = 0;
    protected String clusterCollectionName = "EcalClusters";

    public TestRunTriggerDriver() {
    }

    @Override
    protected void makeTriggerData(EventHeader event, String collectionName) {
        int[] trigArray = new int[8];
        trigArray[TriggerData.TOP_TRIG] = topBits;
        trigArray[TriggerData.BOT_TRIG] = botBits;
        trigArray[TriggerData.AND_TRIG] = topBits & botBits;
        trigArray[TriggerData.OR_TRIG] = topBits | botBits;
        TriggerData tData = new TriggerData(trigArray);
        List<TriggerData> triggerList = new ArrayList<TriggerData>();
        triggerList.add(tData);
        event.put(collectionName, triggerList, TriggerData.class, 0);
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    @Override
    public void startOfData() {
        super.startOfData();
        if (clusterCollectionName == null) {
            throw new RuntimeException("The parameter clusterCollectionName was not set!");
        }

        deadtimelessTriggerCount = 0;
    }

    @Override
    protected boolean triggerDecision(EventHeader event) {
        if (event.hasCollection(HPSEcalCluster.class, clusterCollectionName)) {
            cycleCounter++;
            if (testTrigger(event.get(HPSEcalCluster.class, clusterCollectionName))) {
                triggerThisCycle = true;
            }
        }

        if (cycleCounter % 4 == 0) {
            boolean trigger = triggerThisCycle;
            triggerThisCycle = false;
            return trigger;
        } else {
            return false;
        }
    }

    public boolean testTrigger(List<HPSEcalCluster> clusters) {
        boolean trigger = false;

        topBits <<= 1;
        botBits <<= 1;
        for (HPSEcalCluster cluster : clusters) {
            if (cluster.getEnergy() > clusterEnergyLow) {
                if (cluster.getPosition()[1] > 0) {
                    topBits |= 1;
                } else {
                    botBits |= 1;
                }
                trigger = true;
            }
        }
        if (trigger) {
            deadtimelessTriggerCount++;
        }
        return trigger;
    }

    @Override
    public void endOfData() {
        if (outputStream != null) {
            outputStream.printf("Number of cluster pairs after successive trigger conditions:\n");
            outputStream.printf("Trigger count without dead time: %d\n", deadtimelessTriggerCount);
            outputStream.printf("Trigger count: %d\n", numTriggers);
            outputStream.close();
        }
        System.out.printf("Number of cluster pairs after successive trigger conditions:\n");
        System.out.printf("Trigger count without dead time: %d\n", deadtimelessTriggerCount);
        System.out.printf("Trigger count: %d\n", numTriggers);
        super.endOfData();
    }
}