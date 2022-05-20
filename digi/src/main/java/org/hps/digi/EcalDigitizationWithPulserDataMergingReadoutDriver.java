package org.hps.digi;

import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.ReadoutTimestamp;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;

/**
 * Class <code>EcalDigitizationWithPulserDataMergingReadoutDriver</code> is an implementation of the
 * {@link org.hps.digi.DigitizationWithPulserDataMergingReadoutDriver} for a subdetector of type {@link
 * org.lcsim.geometry.subdetector.HPSEcal3 HPSEcal3}. It handles all
 * of the calorimeter-specific functions needed by the superclass.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class EcalDigitizationWithPulserDataMergingReadoutDriver extends DigitizationWithPulserDataMergingReadoutDriver<HPSEcal3> {
    /** Stores the conditions for this subdetector. */
    private EcalConditions ecalConditions = null;
    
    public EcalDigitizationWithPulserDataMergingReadoutDriver() {
        // Set the default values for each subdetector-dependent
        // parameter.
        setGeometryName("Ecal");
        
        setInputHitCollectionName("EcalHits");
        setOutputHitCollectionName("EcalRawHits");
        setTruthRelationsCollectionName("EcalTruthRelations");
        setTriggerPathTruthRelationsCollectionName("TriggerPathTruthRelations");
        setReadoutHitCollectionName("EcalReadoutHits");
        
        setPhotoelectronsPerMeV(32.8);
        setPulseTimeParameter(9.6);
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        // Get a copy of the calorimeter conditions for the detector.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        
        // Run the superclass method.
        super.detectorChanged(detector);
    }
    
    @Override
    protected Set<Long> getChannelIDs() {
        return getSubdetector().getNeighborMap().keySet();
    }
    
    @Override
    protected Long getID(RawTrackerHit hit) {
        return hit.getCellID();
    }
    
    @Override
    protected double getGainConditions(long channelID) {
        return findChannel(channelID).getGain().getGain();
    }
    
    @Override
    protected double getNoiseConditions(long channelID) {
        return findChannel(channelID).getCalibration().getNoise();
    }
    
    @Override
    protected double getPedestalConditions(long channelID) {
        return findChannel(channelID).getCalibration().getPedestal();
    }
    
    @Override
    protected double getTimeShiftConditions(long channelID) {
        return findChannel(channelID).getTimeShift().getTimeShift();
    }
    
    @Override
    protected int getTimestampFlag() {
        return ReadoutTimestamp.SYSTEM_ECAL;
    }
    
    /**
     * Gets the channel parameters for a given channel ID.
     * @param cellID - The <code>long</code> ID value that represents
     * the channel. This is typically acquired from the method {@link
     * org.lcsim.event.CalorimeterHit#getCellID() getCellID()} in a
     * {@link org.lcsim.event.CalorimeterHit CalorimeterHit} object.
     * @return Returns the channel parameters for the channel as an
     * {@link org.hps.conditions.ecal.EcalChannelConstants
     * EcalChannelConstants} object.
     */
    private EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection().findGeometric(cellID));
    }
}