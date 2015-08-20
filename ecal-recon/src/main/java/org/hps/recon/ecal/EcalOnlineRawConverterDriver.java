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
public class EcalOnlineRawConverterDriver extends Driver {

    private EcalOnlineRawConverter converter = null;
    /**
     * Input collection name
     * Can be a {@link org.lcsim.event.RawTrackerHit} or {@link org.lcsim.event.RawCalorimeterHit}
     * These have ADC and sample time information.
     */
    private String rawCollectionName = "EcalReadoutHits";
    
    /**
     * Output collection name
     * Always a {@link org.lcsim.event.CalorimeterHit}
     * This has energy (GeV) and ns time information.
     */
    private String ecalCollectionName = "EcalCalHits";

    /**
     * ecalCollectionName "type" (must match detector-data) 
     */
    private final String ecalReadoutName = "EcalHits";

    /*
     * Output relation between ecalCollectionName and Mode-7 pedestals
     */
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";

    public EcalOnlineRawConverterDriver() {
        converter = new EcalOnlineRawConverter();
    }

    /**
     * Set the output {@link org.lcsim.event.CalorimeterHit} collection name,
     * 
     * @param ecalCollectionName The <code>CalorimeterHit</code> collection name.
     */
    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    /**
     * Set the input raw collection name
     * <p>
     * Depending on the Driver configuration, this could be a collection
     * of {@link org.lcsim.event.RawTrackerHit} objects for Mode-1
     * or {@link org.lcsim.event.RawCalorimeterHit} objects for Mode-3
     * or Mode-7.
     * 
     * @param rawCollectionName The raw collection name.
     */
    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void process(EventHeader event) {
    	// Do not process the event if the DAQ configuration should be
    	// used for value, but is not initialized.
    	if(!ConfigurationManager.isInitialized()) {
    		return;
    	}
    	
        double timeOffset = 0.0;
        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store hit time
        flags += 1 << LCIOConstants.RCHBIT_LONG; //store hit position; this flag has no effect for RawCalorimeterHits

        ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

        /*
         * This is for FADC Mode-1 data:    
         */
        if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
        	List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

        	for (RawTrackerHit hit : hits) {
        		newHits.addAll(converter.HitDtoA(event,hit));
        	}
        	event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }

        /*
         * This is for FADC pulse mode data (Mode-3 or Mode-7):
         */
        if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) { 

        	/*
        	 * This is for FADC Mode-7 data:
        	 */
        	if (event.hasCollection(LCRelation.class, extraDataRelationsName)) { // extra information available from mode 7 readout
        		List<LCRelation> extraDataRelations = event.get(LCRelation.class, extraDataRelationsName);
        		for (LCRelation rel : extraDataRelations) {
        			RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
        			GenericObject extraData = (GenericObject) rel.getTo();
        			newHits.add(converter.HitDtoA(event,hit, extraData, timeOffset));
        		}
        	} else {
        		/*
        		 * This is for FADC Mode-3 data:
        		 */
        		List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);
        		for (RawCalorimeterHit hit : hits) {
        			newHits.add(converter.HitDtoA(event, hit, timeOffset));
        		}
        	}
        	event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
        }
    }

}
