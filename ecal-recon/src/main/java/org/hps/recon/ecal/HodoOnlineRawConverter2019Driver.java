package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This class is used to convert between collections of
 * {@link org.lcsim.event.RawTrackerHit}, objects with ADC/sample information,
 * and collections of {@link org.lcsim.event.CalorimeterHit}, objects with
 * energy/time information.
 * 
 * 
 * @author Tongtong Cao <caot@jlab.org>
 *
 */
public class HodoOnlineRawConverter2019Driver extends Driver {
    private HodoOnlineRawConverter2019 converter = null;
    /**
     * The input LCIO collection name. This can be either a
     * {@link org.lcsim.event.RawTrackerHit} or
     * {@link org.lcsim.event.RawCalorimeterHit}. These have ADC and sample time
     * information.
     */
    private String rawCollectionName = "HodoReadoutHits";

    /**
     * The output LCIO collection name. This will always a
     * {@link org.lcsim.event.CalorimeterHit} with energy and ns time information.
     */
    private String hodoCollectionName = "HodoCalHits";

    /**
     * ecalCollectionName "type" (must match detector-data)
     */
    private final String hodoReadoutName = "HodoHits";

    /**
     * Instantiates the <code>HodoOnlineRawConverter2019</code> for this driver.
     */
    public HodoOnlineRawConverter2019Driver() {
        converter = new HodoOnlineRawConverter2019();
    }

    /**
     * Checks that the required LCIO collection names are defined.
     */
    @Override
    public void startOfData() {
        if (hodoCollectionName == null) {
            throw new RuntimeException("The parameter hodoCollectionName was not set!");
        }
    }

    @Override
    public void process(EventHeader event) {
        // Do not process the event if the DAQ configuration is not
        // initialized. All online raw converter parameters are obtained
        // from this class, and this nothing can be done before they
        // are available.
        if (!ConfigurationManager2019.isInitialized()) {
            return;
        }

        // Define the LCIO data flags.
        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; // Store hit time.
        flags += 1 << LCIOConstants.RCHBIT_LONG + 2; // Store hit position; this flag has no effect for
                                                     // RawCalorimeterHits; +2 is to distinguish between HodoCalHits and
                                                     // EcalCalHits.

        // Create a list to store hits.
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

        // Events that contain RawTrackerHit objects use mode-1 data.
        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            // Get the list of mode-1 waveforms.
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

            // Extract hits from each waveform and store them.
            for (RawTrackerHit hit : hits) {
                newHits.addAll(converter.HitDtoA(hit));
            }

            // Add the hits to the data stream.
            event.put(hodoCollectionName, newHits, CalorimeterHit.class, flags, hodoReadoutName);
        }
    }

    /**
     * Sets the output {@link org.lcsim.event.CalorimeterHit} LCIO collection name.
     * 
     * @param ecalCollectionName - The LCIO collection name for output data.
     */
    public void setHodoCollectionName(String hodoCollectionName) {
        this.hodoCollectionName = hodoCollectionName;
    }

    /**
     * Sets the input raw hit data LCIO collection name. It is a collection of
     * {@link org.lcsim.event.RawTrackerHit} objects for mode-1 data.
     * 
     * @param rawCollectionName - The LCIO collection name for raw data.
     */
    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }
}
