/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.hps.recon.ecal;

import org.lcsim.util.Driver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;

import java.util.List;
import java.util.ArrayList;

/**
 *
 * @author rafopar
 */
public class HodoRawConverterDriver extends Driver {

    // To import database conditions
    private HodoscopeConditions hodoConditions = null;

    private HodoRawConverter converter = null;

    // ===== The Mode1 Hodo hit collection name =====
    private String rawCollectionName = "HodoReadoutHits";

    private String hodoCollectionName = "HodoCalHits";

    // ===== **NOTE** Seems this name can not be arbitrary, it is taken from the detector
    // ===== For example you can find out this by running this method of the detector
    // ===== System.out.println("==== The Detector SubDetector hit collection name " + detector.getSubdetector("Hodoscope").getHitsCollectionName() );
    private final String hodoReadoutName = "HodoscopeHits";

    public HodoRawConverterDriver() {
        converter = new HodoRawConverter();
    }

    @Override
    public void startOfData() {
        if (hodoCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

    }

    @Override
    public void detectorChanged(Detector detector) {

        
//        System.out.println("==== The Detector Name is " + detector.getDetectorName());
//        System.out.println("==== The DetectorElement Name is " + detector.getDetectorElement().getName() );
//        System.out.println("==== The Detector SubDetector hit collection name " + detector.getSubdetector("Hodoscope").getHitsCollectionName() );
        // Hodo conditions object.
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();

        converter.setConditions(hodoConditions);

    }

    @Override
    public void process(EventHeader event) {

        ArrayList<CalorimeterHit> hodoHits = new ArrayList<CalorimeterHit>();

        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {

            int flags = 0;
            flags += 1 << LCIOConstants.RCHBIT_TIME; // store hit time
            flags += 1 << LCIOConstants.RCHBIT_LONG; // store hit position; this flag has no effect for RawCalorimeterHits

            // ======= Getting list of Mode1 hits from the event ======
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            // ======= Loop over hits, and reconstruct energies and times for all hits =====
            for (RawTrackerHit hit : hits) {

                // Getting the cellID of the hit
                final long cellID = hit.getCellID();

                double ped = converter.getPedestal(event, cellID);

                // ========= Now for the given channel we will loop over all threshold crossings, and
                // ========= for each of them will calculate the signal by integrating from NSB to NSA
                ArrayList<Integer> thr_crosings = converter.FindThresholdCrossings(hit, ped);

                //System.out.println("# of thr crossings = " + thr_crosings.size());
                // ===== For now we will calculate coars time, which is the threshold crossing sample time.
                // ===== Later will implement the mode7 time
                hodoHits.addAll(converter.getCaloHits(hit, thr_crosings, ped));

                
                // ===== The following few lines are the equivalent of the cin.ignore() of C
//                try {
//                    System.in.read();
//                } catch (Exception e) {
//                }

            }

            event.put(hodoCollectionName, hodoHits, CalorimeterHit.class, flags, hodoReadoutName);
        }
        //Map<HodoscopeChannel, Double> runningPedMap = (Map<HodoscopeChannel, Double>) event.get("HodoRunningPedestals");

        //System.out.println("runningPedMap = " + runningPedMap);
        // Now 
    }

}
