package org.lcsim.hps.users.gcharles;

import hep.aida.IHistogram1D;
import java.util.List;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.hps.readout.ecal.FADCEcalReadoutDriver;
import org.lcsim.hps.util.RingBuffer;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Saves histograms of FADC signal buffers before and after hits.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class FADCSignalAnalysis extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    FADCEcalReadoutDriver readoutDriver = new FADCEcalReadoutDriver();

    @Override
    public void startOfData() {

        add(readoutDriver);
        readoutDriver.setCoincidenceWindow(2);
        readoutDriver.setEcalName("Ecal");
        readoutDriver.setEcalCollectionName("EcalHits");
        readoutDriver.setEcalRawCollectionName("EcalRawHits");
        readoutDriver.setConstantTriggerWindow(true);
        readoutDriver.setScaleFactor(1);
        //readoutDriver.setFixedGain(1.1);
        readoutDriver.setUseCRRCShape(false);

        super.startOfData();
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

            for (CalorimeterHit hit : hits) {
                hit.getCellID();
                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
                String name = String.format("pipeline x=%d, y=%d before hit in event %d, time %f, energy %f", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), event.getEventNumber(), hit.getTime(), hit.getRawEnergy());
                IHistogram1D hist = aida.histogram1D(name, signalBuffer.getLength(), -0.5, signalBuffer.getLength() - 0.5);
                for (int i = 0; i < signalBuffer.getLength(); i++) {
                    hist.fill(i, signalBuffer.getValue(i));
                }
            }
        }


        //now run the subdriver, so we can see the effect of adding the hits
        super.process(event);


        if (event.hasCollection(CalorimeterHit.class, "EcalHits")) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, "EcalHits");

            for (CalorimeterHit hit : hits) {
                hit.getCellID();
                RingBuffer signalBuffer = readoutDriver.getSignalMap().get(hit.getCellID());
                String name = String.format("pipeline x=%d, y=%d after hit in event %d, time %f, energy %f", hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), event.getEventNumber(), hit.getTime(), hit.getRawEnergy());
                IHistogram1D hist = aida.histogram1D(name, signalBuffer.getLength(), -0.5, signalBuffer.getLength() - 0.5);
                for (int i = 0; i < signalBuffer.getLength(); i++) {
                    hist.fill(i, signalBuffer.getValue(i));
                }
            }
        }
    }
}
