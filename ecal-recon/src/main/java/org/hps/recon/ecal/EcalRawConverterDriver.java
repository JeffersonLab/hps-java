package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.ConditionsDriver;
import org.hps.conditions.TableConstants;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannel.GeometryId;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalConditionsUtil;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 */
public class EcalRawConverterDriver extends Driver {

	// To import database conditions
    static EcalConditions ecalConditions = null;
    static IIdentifierHelper helper = null;
    static EcalChannelCollection channels = null; 
    
    Detector detector = null;
    
    EcalRawConverter converter = null;
    String rawCollectionName = "EcalReadoutHits";
    String ecalReadoutName = "EcalHits";
    String ecalCollectionName = "EcalCalHits";
    int integralWindow = 35;
    boolean debug = false;
    double threshold = Double.NEGATIVE_INFINITY;
    boolean applyBadCrystalMap = true;
    boolean dropBadFADC = false;
    private boolean runBackwards = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;

    public EcalRawConverterDriver() {
    	converter = new EcalRawConverter();    	
    }

    public void setUse2014Gain(boolean use2014Gain) {
        converter.setUse2014Gain(use2014Gain);
    }

    public void setRunBackwards(boolean runBackwards) {
        this.runBackwards = runBackwards;
    }

    public void setDropBadFADC(boolean dropBadFADC) {
        this.dropBadFADC = dropBadFADC;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public void setGain(double gain) {
        converter.setGain(gain);
    }

    public void setIntegralWindow(int integralWindow) {
        this.integralWindow = integralWindow;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    public void setApplyBadCrystalMap(boolean apply) {
        this.applyBadCrystalMap = apply;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setUseTimestamps(boolean useTimestamps) {
        this.useTimestamps = useTimestamps;
    }

    public void setUseTruthTime(boolean useTruthTime) {
        this.useTruthTime = useTruthTime;
    }

    @Override
    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void detectorChanged(Detector detector) {
    	
    	converter.setDetector(detector);
    	
    	// set the detector for the converter
        this.detector = detector;
    	
        // ECAL combined conditions object.
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        
        // List of channels.
        channels = ecalConditions.getChannelCollection();
        
        // ID helper.
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
        
        System.out.println("You are now using the database conditions for EcalRawConverterDriver.");
    }
    /**
     * @return false if the channel is a good one, true if it is a bad one
     * @param CalorimeterHit
     */
    public static boolean isBadCrystal(CalorimeterHit hit) {   	
        // Get the channel data.
        EcalChannelConstants channelData = findChannel(hit.getCellID());
    	
        return channelData.isBadChannel();
    }
    
    /**
     * @return false if the ADC is a good one, true if it is a bad one
     * @param CalorimeterHit
     */
    public boolean isBadFADC(CalorimeterHit hit) {    	
        return (getCrate(hit.getCellID()) == 1 && getSlot(hit.getCellID()) == 3);
    }
    
    private static double getTimestamp(int system, EventHeader event) { //FIXME: copied from org.hps.readout.ecal.ReadoutTimestamp
        if (event.hasCollection(GenericObject.class, "ReadoutTimestamps")) {
            List<GenericObject> timestamps = event.get(GenericObject.class, "ReadoutTimestamps");
            for (GenericObject timestamp : timestamps) {
                if (timestamp.getIntVal(0) == system) {
                    return timestamp.getDoubleVal(0);
                }
            }
            return 0;
        } else {
            return 0;
        }
    }

    @Override
    public void process(EventHeader event) {
        final int SYSTEM_TRIGGER = 0;
        final int SYSTEM_TRACKER = 1;
        final int SYSTEM_ECAL = 2;

        double timeOffset = 0.0;
        if (useTimestamps) {
            double t0ECal = getTimestamp(SYSTEM_ECAL, event);
            double t0Trig = getTimestamp(SYSTEM_TRIGGER, event);
            timeOffset += (t0ECal - t0Trig) + 200.0;
        }
        if (useTruthTime) {
            double t0ECal = getTimestamp(SYSTEM_ECAL, event);
            timeOffset += ((t0ECal + 250.0) % 500.0) - 250.0;
        }


        int flags = 0;
        flags += 1 << LCIOConstants.RCHBIT_TIME; //store hit time
        flags += 1 << LCIOConstants.RCHBIT_LONG; //store hit position; this flag has no effect for RawCalorimeterHits

        if (!runBackwards) {
            ArrayList<CalorimeterHit> newHits = new ArrayList<CalorimeterHit>();

            // Get the list of ECal hits.
            if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
                List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);

                for (RawTrackerHit hit : hits) {
                    CalorimeterHit newHit = converter.HitDtoA(hit);
             
                    // Get the channel data.
                    EcalChannelConstants channelData = findChannel(newHit.getCellID());
                    
                    if (applyBadCrystalMap && channelData.isBadChannel()) {
                        continue;
                    }
                    if (dropBadFADC && isBadFADC(newHit)) {
                        continue;
                    }
                    if (newHit.getRawEnergy() > threshold) {
                        newHits.add(newHit);
                    }
                }
                event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
            }
            if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
                List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);

                for (RawCalorimeterHit hit : hits) {
                    if (debug) {
                        System.out.format("old hit energy %d\n", hit.getAmplitude());
                    }
                    CalorimeterHit newHit = converter.HitDtoA(hit, integralWindow, timeOffset);
                    
                    if (newHit.getRawEnergy() > threshold) {
                        if (applyBadCrystalMap && isBadCrystal(newHit)) {
                            continue;
                        }
                        if (dropBadFADC && isBadFADC(newHit)) {
                            continue;
                        }
                        if (debug) {
                            System.out.format("new hit energy %f\n", newHit.getRawEnergy());
                        }
                        newHits.add(newHit);
                    }
                }
                event.put(ecalCollectionName, newHits, CalorimeterHit.class, flags, ecalReadoutName);
            }
        } else {
            ArrayList<RawCalorimeterHit> newHits = new ArrayList<RawCalorimeterHit>();
            if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
                List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

                for (CalorimeterHit hit : hits) {
                    if (debug) {
                        System.out.format("old hit energy %f\n", hit.getRawEnergy());
                    }
                    RawCalorimeterHit newHit = converter.HitAtoD(hit, integralWindow);
                    if (newHit.getAmplitude() > 0) {
                        if (debug) {
                            System.out.format("new hit energy %d\n", newHit.getAmplitude());
                        }
                        newHits.add(newHit);
                    }
                }
                event.put(rawCollectionName, newHits, RawCalorimeterHit.class, flags, ecalReadoutName);
            }
        }
    }
    
    
    /** 
     * Convert physical ID to gain value.
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private static EcalChannelConstants findChannel(long cellID) {
        // Make an ID object from raw hit ID.
        IIdentifier id = new Identifier(cellID);
        
        // Get physical field values.
        int system = helper.getValue(id, "system");
        int x = helper.getValue(id, "ix");
        int y = helper.getValue(id, "iy");
        
        // Create an ID to search for in channel collection.
        GeometryId geometryId = new GeometryId(helper, new int[] { system, x, y });
                
        // Get the channel data.
        return ecalConditions.getChannelConstants(channels.findChannel(geometryId));    
    }
    
    /**
     * Return crate number from cellID
     * @param cellID (long)
     * @return Crate number (int)
     */
    private int getCrate(long cellID) {
        
        EcalConditionsUtil util = new EcalConditionsUtil();

        // Find the ECAL channel and return the crate number.
        return util.getCrate(helper, cellID);
    }
    
    /**
     * Return slot number from cellID
     * @param cellID (long)
     * @return Slot number (int)
     */
    private int getSlot(long cellID) {
        EcalConditionsUtil util = new EcalConditionsUtil();

        // Find the ECAL channel and return the crate number.
        return util.getSlot(helper, cellID);         
    }
    
}
