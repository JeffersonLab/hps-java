/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package org.hps.recon.ecal;

import org.lcsim.util.Driver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

import java.util.Map;
import java.util.List;

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

        // Hodo conditions object.
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();
        
        converter.setConditions(hodoConditions);

    }

    @Override
    public void process(EventHeader event) {

        System.out.println("===== Starting of Process in the Hodo Recon: The event number is " + event.getEventNumber());
        
        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {

            // ======= Getting list of Mode1 hits from the event ======
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            // ======= Loop over hits, and reconstruct energies and times for all hits =====
            for (RawTrackerHit hit : hits) {

                // Getting the cellID of the hit
                final long cellID = hit.getCellID();
                
                System.out.println("cellID of the hit is " + cellID);
                
                //double ped = converter.getPedestal(event, cellID);                                
            }

        }
        Map<HodoscopeChannel, Double> runningPedMap = (Map<HodoscopeChannel, Double>) event.get("HodoRunningPedestals");

        //System.out.println("runningPedMap = " + runningPedMap);
        // Now 
        
        System.out.println("===== End of Process in the Hodo Recon =====");
    }

}
