package org.hps.digi;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.readout.ReadoutTimestamp;
import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.hps.record.daqconfig2019.FADCConfigEcal2019;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;

/**
 * Class <code>EcalDigitizationWithPulserDataMergingReadoutDriver</code> is an implementation of the
 * {@link org.hps.digi.DigitizationWithPulserDataMergingReadoutDriver} for a subdetector of type {@link
 * org.lcsim.geometry.subdetector.HPSEcal3 HPSEcal3}. It handles all
 * of the calorimeter-specific functions needed by the superclass.
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class EcalDigitizationWithPulserDataMergingReadoutDriver extends DigitizationWithPulserDataMergingReadoutDriver<HPSEcal3> {
    // The DAQ configuration manager for FADC parameters.
    private FADCConfigEcal2019 config = new FADCConfigEcal2019();
    private boolean configStat = false; // Indicates if DAQ configuration is loaded
    
    // The number of nanoseconds in a clock-cycle (sample).
    private static final int nsPerSample = 4;
    
    
    /** Stores the conditions for this subdetector. */
    private EcalConditions ecalConditions = null;
    
    /** Stores the channel collection for this subdetector. */
    private EcalChannelCollection geoMap = new EcalChannelCollection();
    
    public EcalDigitizationWithPulserDataMergingReadoutDriver() {
        // Set the default values for each subdetector-dependent
        // parameter.
        setGeometryName("Ecal");
        
        setInputHitCollectionName("EcalHits");
        setOutputHitCollectionName("EcalRawHits");
        setTruthRelationsCollectionName("EcalTruthRelations");
        setTriggerPathTruthRelationsCollectionName("TriggerPathTruthRelations");
        setReadoutHitCollectionName("EcalReadoutHits");
        
        setPhotoelectronsPerMeV(EcalUtils.photoelectronsPerMeV);
        setPulseTimeParameter(9.6);
    }
    
    /**
     * Sets whether or not the DAQ configuration is applied into the driver
     * the EvIO data stream or whether to read the configuration from data files.
     * 
     * @param state - <code>true</code> indicates that the DAQ configuration is
     * applied into the readout system, and <code>false</code> that it
     * is not applied into the readout system.
     */
    public void setDaqConfigurationAppliedintoReadout(boolean state) {
        // If the DAQ configuration should be read, attach a listener
        // to track when it updates.               
        if (state) {
            ConfigurationManager2019.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Get the DAQ configuration.
                    DAQConfig2019 daq = ConfigurationManager2019.getInstance();

                    // Load the DAQ settings from the configuration manager.
                    numSamplesAfter = daq.getEcalFADCConfig().getNSA() / nsPerSample;
                    numSamplesBefore = daq.getEcalFADCConfig().getNSB() / nsPerSample;
                    readoutWindow = daq.getEcalFADCConfig().getWindowWidth() / nsPerSample;
                    pulserDataWindow = readoutWindow;

                    // Get the FADC configuration.
                    config = daq.getEcalFADCConfig();
                    configStat = true;
                }
            });
        }
    }        
    
    
    @Override
    public void detectorChanged(Detector detector) {
        // Get a copy of the calorimeter conditions for the detector.
        ecalConditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        
        // Store the calorimeter conditions table for converting between
        // geometric IDs and channel objects.
        geoMap = DatabaseConditionsManager.getInstance().getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
        
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
    protected double getGainConditions(long cellID) {
        return findChannel(cellID).getGain().getGain();
    }
    
    @Override
    protected double getNoiseConditions(long channelID) {
        return findChannel(channelID).getCalibration().getNoise();
    }
    
    protected double getPedestalConditions(long cellID) { 
        return findChannel(cellID).getCalibration().getPedestal();
        
    }   
    
    @Override
    protected double getTimeShiftConditions(long cellID) {
        return findChannel(cellID).getTimeShift().getTimeShift();
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