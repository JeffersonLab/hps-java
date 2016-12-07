package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.record.daqconfig.ConfigurationManager;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This class is used to convert between collections of {@link org.lcsim.event.RawCalorimeterHit}
 * and {@link org.lcsim.event.RawTrackerHit}, objects with ADC/sample information, and
 * collections of {@link org.lcsim.event.CalorimeterHit}, objects with energy/time information.
 * 
 * org.hps.recon.ecal.EcalRawConverter is called to do most of the lower level work.
 *
 *
*/
public class EcalOnlineRawConverterDriver2 extends Driver {
    private EcalOnlineRawConverter2 converter = null;
    /**
     * The input LCIO collection name. This can be either a
     * {@link org.lcsim.event.RawTrackerHit} or
     * {@link org.lcsim.event.RawCalorimeterHit}. These have ADC and
     * sample time information.
     */
    private String rawCollectionName = "EcalReadoutHits";
    
    /**
     * The output LCIO collection name. This will always a
     * {@link org.lcsim.event.CalorimeterHit} with energy (GeV) and
     * ns time information.
     */
    private String ecalCollectionName = "EcalCalHits";
    
    /**
     * ecalCollectionName "type" (must match detector-data) 
     */
    private final String ecalReadoutName = "EcalHits";
    
    /**
     * Output relation between ecalCollectionName and Mode-7 pedestals.
     */
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";
    
    /**
     * Instantiates the <code>EcalOnlineRawConverter2</code> for this
     * driver.
     */
    public EcalOnlineRawConverterDriver2() { converter = new EcalOnlineRawConverter2(); }
    
    /**
     * Checks that the required LCIO collection names are defined.
     */
    @Override
    public void startOfData() {
        if(ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }
    
    @Override
    public void process(EventHeader event) {
        // Do not process the event if the DAQ configuration is not
        // initialized. All online raw converter parameters are obtained
        // from this class, and this nothing can be done before they
        // are available.
        if(!ConfigurationManager.isInitialized()) {
            return;
        }
        
        double timeOffset = 0.0;
        
        // Define the LCIO data flags.
        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; // Store hit time.
        flags += 1 << LCIOConstants.RCHBIT_LONG; // Store hit position; this flag has no effect for RawCalorimeterHits.
        
        // Create a list to store hits.
        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();
        
        // Events that contain RawTrackerHit objects use mode-1 data.
        if(event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            // Get the list of mode-1 waveforms.
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);
            
            // Extract hits from each waveform and store them.
            for(RawTrackerHit hit : hits) {
                newHits.addAll(converter.HitDtoA(hit));
            }
            
            // Add the hits to the data stream.
            event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }
        
        // Events that contain RawCalorimeterHit objects are either
        // mode-3 or mode-7.
        if(event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
            // Check for extra relations data. This indicates that the
            // hits should be interpreted as mode-7.
            if(event.hasCollection(LCRelation.class, extraDataRelationsName)) { // extra information available from mode 7 readout
                List<LCRelation> extraDataRelations = event.get(LCRelation.class, extraDataRelationsName);
                for(LCRelation rel : extraDataRelations) {
                    RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                    GenericObject extraData = (GenericObject) rel.getTo();
                    newHits.add(converter.HitDtoA(hit, extraData, timeOffset));
                }
            }
            
            // Otherwise, the hits should be treated as mode-3.
            else {
                List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);
                for(RawCalorimeterHit hit : hits) {
                    newHits.add(converter.HitDtoA(hit, timeOffset));
                }
            }
            event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }
    }
    
    /**
     * Sets the output {@link org.lcsim.event.CalorimeterHit} LCIO
     * collection name.
     * @param ecalCollectionName - The LCIO collection name for output
     * data.
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }
    
    /**
     * Sets whether to use a constant integration window for the the
     * purpose of determining the correct pedestal. This should be used
     * in conjunction with Monte Carlo data during the readout cycle.
     * @param state - <code>true</code> ignores the size of the readout
     * window when calculating pedestals, and <code>false</code> accounts
     * for it in the case of pulse-clipping.
     */
    public void setIsReadoutSimulation(boolean state) {
        converter.setUseConstantWindow(state);
    }
    
    /**
     * Sets the input raw hit data LCIO collection name. Depending on
     * the driver configuration, this could be either a collection of
     * {@link org.lcsim.event.RawTrackerHit} objects for mode-1 data or
     * {@link org.lcsim.event.RawCalorimeterHit} objects for mode-3
     * and mode-7 data.
     * @param rawCollectionName - The LCIO collection name for raw data.
     */
    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }
}