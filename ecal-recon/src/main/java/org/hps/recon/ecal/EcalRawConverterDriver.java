package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 *
 * @version $Id: HPSEcalRawConverterDriver.java,v 1.2 2012/05/03 00:17:54
 * phansson Exp $
 *
 * baltzell: Feb 26, 2015:
 * added firmware emulation for converting from Mode-1 readout (RawTrackerHit)
 * to Mode-3 pulse (CalorimeterHit).  Turn it on with "emulateFirmware", else
 * defaults to previous behavior.  Removed integralWindow in favor of NSA and
 * NSB in EcalRawConverter, so that all conversions can use the same window.
 * March 3, 2015:  Removed integralWindow in favor of NSA/NSB in order to treat
 * all modes uniformly.
 * 
 */
public class EcalRawConverterDriver extends Driver {

    // To import database conditions
    private EcalConditions ecalConditions = null;

    private EcalRawConverter converter = null;
    private String rawCollectionName = "EcalReadoutHits";
    private final String ecalReadoutName = "EcalHits";
    private String ecalCollectionName = "EcalCalHits";

    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";
//    private static final String extraDataCollectionName = "EcalReadoutExtraData";

    private boolean debug = false;
    private double threshold = Double.NEGATIVE_INFINITY;
    private boolean applyBadCrystalMap = true;
    private boolean dropBadFADC = false;
    private boolean runBackwards = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;

    private boolean emulateFirmware = false;
    
    public EcalRawConverterDriver() {
        converter = new EcalRawConverter();
    }

    public void setUse2014Gain(boolean use2014Gain) {
        converter.setUse2014Gain(use2014Gain);
    }

    public void setUseTimeWalkCorrection(boolean useTimeWalkCorrection) {
        converter.setUseTimeWalkCorrection(useTimeWalkCorrection);
    }
    public void setUseRunningPedestal(boolean useRunningPedestal) {
        converter.setUseRunningPedestal(useRunningPedestal);
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

    public void setEmulateFirmware(boolean emulateFirmware) {
        this.emulateFirmware = emulateFirmware;
    }
    public void setLeadingEdgeThreshold(double threshold) {
        converter.setLeadingEdgeThreshold(threshold);
    }
    public void setNsa(int nsa) {
        converter.setNSA(nsa);
    }
    public void setNsb(int nsb) {
        converter.setNSB(nsb);
    }
    
    public void setGain(double gain) {
        converter.setGain(gain);
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

        // set the detector for the converter
        // FIXME: This method doesn't even need the detector object and does not use it.
        converter.setDetector(detector);

        // ECAL combined conditions object.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
    }

    /**
     * @return false if the channel is a good one, true if it is a bad one
     * @param CalorimeterHit
     */
    public boolean isBadCrystal(CalorimeterHit hit) {
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
                    
                    CalorimeterHit newHit = null;
                    if (emulateFirmware) {
                        newHit = converter.firmwareHitDtoA(hit);
                        if (newHit==null) continue;
                    } else {
                        newHit = converter.HitDtoA(hit);
                    }
                

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
            if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) { //A.C. this is the case of the RAW pulse hits
                if (event.hasCollection(LCRelation.class, extraDataRelationsName)) { // extra information available from mode 7 readout
                    List<LCRelation> extraDataRelations = event.get(LCRelation.class, extraDataRelationsName);
                    for (LCRelation rel : extraDataRelations) {
                        RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                        if (debug) {
                            System.out.format("old hit energy %d\n", hit.getAmplitude());
                        }
                        GenericObject extraData = (GenericObject) rel.getTo();
                        CalorimeterHit newHit;
                        newHit = converter.HitDtoA(event,hit, extraData, timeOffset);
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
                } else {
                    List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, rawCollectionName);
                    for (RawCalorimeterHit hit : hits) {
                        if (debug) {
                            System.out.format("old hit energy %d\n", hit.getAmplitude());
                        }
                        CalorimeterHit newHit;
                        newHit = converter.HitDtoA(hit, timeOffset);
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
                    RawCalorimeterHit newHit = converter.HitAtoD(hit);
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
     *
     * @param cellID (long)
     * @return channel constants (EcalChannelConstants)
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
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
