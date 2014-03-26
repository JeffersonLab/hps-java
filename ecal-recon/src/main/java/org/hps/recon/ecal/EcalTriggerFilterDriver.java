package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Changes ECal hit IDs to match what the test run trigger sees.
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalTriggerFilterDriver extends Driver {

    private String ecalReadoutName = "EcalHits";
    private String inputCollection = "EcalReadoutHits";
    private String outputCollection = "EcalCalHits";
    private int topDelay = 0;
    private int bottomDelay = 5;
    private Queue<List<CalorimeterHit>> topHitsQueue = null;
    private Queue<List<CalorimeterHit>> bottomHitsQueue = null;

    public EcalTriggerFilterDriver() {
    }

    public void setOutputCollection(String outputCollection) {
        this.outputCollection = outputCollection;
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    @Override
    public void startOfData() {
        if (outputCollection == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        topHitsQueue = new ArrayBlockingQueue<List<CalorimeterHit>>(topDelay + 1);
        for (int i = 0; i < topDelay; i++) {
            topHitsQueue.add(new ArrayList<CalorimeterHit>());
        }
        bottomHitsQueue = new ArrayBlockingQueue<List<CalorimeterHit>>(bottomDelay + 1);
        for (int i = 0; i < bottomDelay; i++) {
            bottomHitsQueue.add(new ArrayList<CalorimeterHit>());
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

            ArrayList<CalorimeterHit> topHits = new ArrayList<CalorimeterHit>();
            ArrayList<CalorimeterHit> bottomHits = new ArrayList<CalorimeterHit>();

            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                CalorimeterHit newHit = filterHit(hit);
                if (newHit != null) {
                    if (hit.getIdentifierFieldValue("iy") > 0) { //should really be checking newHit, but it doesn't have metadata yet
                        topHits.add(newHit);
                    } else {
                        bottomHits.add(newHit);
                    }
                }
            }
            topHitsQueue.add(topHits);
            bottomHitsQueue.add(bottomHits);
            newHits.addAll(topHitsQueue.poll());
            newHits.addAll(bottomHitsQueue.poll());
            int flags = 0;
            event.put(outputCollection, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }
    }

    private CalorimeterHit filterHit(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        long daqID = EcalConditions.physicalToDaqID(hit.getCellID());
        int crate = EcalConditions.getCrate(daqID);
        short slot = EcalConditions.getSlot(daqID);
        short channel = EcalConditions.getChannel(daqID);

        int delay = iy>0?topDelay:bottomDelay;
        // no triggers from crate 1, slot 3 
        if (crate == 1 && slot == 3) {
            return null;
        }

        // flip quadrant
        if (ix > 0 && iy > 0) {
            ix = 24 - ix;
        }
        long newID = EcalConditions.makePhysicalID(ix, iy);
        //make new hit; set position to null so it gets recalculated
        return new HPSCalorimeterHit(hit.getRawEnergy(), hit.getTime()+delay*4, newID, hit.getType());
    }
}
