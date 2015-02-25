package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Changes ECal hit IDs to match what the test run trigger sees.
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalTriggerFilterDriver extends Driver {

    // To import database conditions
    private EcalConditions ecalConditions = null;

    private IIdentifierHelper helper = null;
    private int systemId;

    private final String ecalReadoutName = "EcalHits";
    private String inputCollection = "EcalReadoutHits";
    private String outputCollection = "EcalCalHits";
    private final int topDelay = 0;
    private final int bottomDelay = 5;
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
        // ID helper.
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();

        systemId = detector.getSubdetector("Ecal").getSystemID();

        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();

        System.out.println("You are now using the database conditions for EcalTriggerFilterDriver.");
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
                    if (newHit.getIdentifierFieldValue("iy") > 0) {
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

    /**
     * This method takes input hits and makes new hits with different ix
     *
     * @param CalorimeterHit hit
     * @return new HPSCalorimeterHit
     */
    private CalorimeterHit filterHit(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        int crate = getCrate(hit.getCellID());
        int slot = getSlot(hit.getCellID());

        int delay = iy > 0 ? topDelay : bottomDelay;

        // no triggers from crate 1, slot 3 
        if (crate == 1 && slot == 3) {
            return null;
        }

        // flip quadrant
        if (ix > 0 && iy > 0) {
            ix = 24 - ix;
        }

        int values[] = {systemId, ix, iy};
        GeometryId geomId = new GeometryId(helper, values);
        // Creating the new channel from cell id, ix and iy, then reading its ID       
        long newID = geomId.encode();

        //make new hit; set position to null so it gets recalculated                       
        return CalorimeterHitUtilities.create(hit.getRawEnergy(), hit.getTime() + delay * 4, newID, hit.getType(), hit.getMetaData());
    }

    /**
     * Return crate number from cellID
     *
     * @param cellID (long)
     * @return Crate number (int)
     */
    private int getCrate(long cellID) {
        // Find the ECAL channel and return the crate number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getCrate();
    }

    /**
     * Return slot number from cellID
     *
     * @param cellID (long)
     * @return Slot number (int)
     */
    private int getSlot(long cellID) {
        // Find the ECAL channel and return the slot number.
        return ecalConditions.getChannelCollection().findGeometric(cellID).getSlot();
    }

}
