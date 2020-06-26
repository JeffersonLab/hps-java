package org.hps.readout.hodoscope;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeCalibration;
import org.hps.conditions.hodoscope.HodoscopeCalibration.HodoscopeCalibrationCollection;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.conditions.hodoscope.HodoscopeGain;
import org.hps.conditions.hodoscope.HodoscopeGain.HodoscopeGainCollection;
import org.hps.conditions.hodoscope.HodoscopeTimeShift;
import org.hps.conditions.hodoscope.HodoscopeTimeShift.HodoscopeTimeShiftCollection;
import org.hps.readout.DigitizationReadoutDriver;
import org.hps.readout.ReadoutTimestamp;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.Hodoscope_v1;

/**
 * Class <code>HodoscopeDigitizationReadoutDriver</code> is an
 * implementation of the {@link
 * org.hps.readout.ecal.updated.DigitizationReadoutDriver
 * DigitizationReadoutDriver} for a subdetector of type {@link
 * org.lcsim.geometry.subdetector.Hodoscope_v1 Hodoscope_v1}. It
 * handles all of the hodoscope-specific functions needed by the
 * superclass.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @author Tongtong Cao <caot@jlab.org>
 */
public class HodoscopeDigitizationReadoutDriver extends DigitizationReadoutDriver<Hodoscope_v1> {
    /** Stores the set of all channel IDs for  the hodoscope. */
    private Set<Long> channelIDSet = new HashSet<Long>();
    /** Maps hodoscope channels to the gain for that channel. */
    private Map<Long, HodoscopeGain> channelToGainsMap = new HashMap<Long, HodoscopeGain>();
    /** Maps hodoscope channels to the time shifts for that channel. */
    private Map<Long, HodoscopeTimeShift> channelToTimeShiftsMap = new HashMap<Long, HodoscopeTimeShift>();
    /** Maps hodoscope channels to the noise sigma and pedestals for that channel. */
    private Map<Long, HodoscopeCalibration> channelToCalibrationsMap = new HashMap<Long, HodoscopeCalibration>();
    /** Factor for gain conversion from self-define-unit/ADC to MeV/ADC. */
    private double factorGainConversion = 0.000833333;
    
    public HodoscopeDigitizationReadoutDriver() {
        // Set the default values for each subdetector-dependent
        // parameter.
        setGeometryName("Hodoscope");
        
        setInputHitCollectionName("HodoscopeHits");
        setOutputHitCollectionName("HodoscopeRawHits");
        setTruthRelationsCollectionName("HodoscopeTruthRelations");
        setTriggerPathTruthRelationsCollectionName("HodoscopeTriggerPathTruthRelations");
        setReadoutHitCollectionName("HodoscopeReadoutHits");
        
        setNumberSamplesAfter(10);
        setNumberSamplesBefore(6);
        setPulseTimeParameter(4.0);
        setPhotoelectronsPerMeV(10.0);
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        // Populate the channel ID collections.
        populateChannelCollections();
        
        // Run the superclass method.
        super.detectorChanged(detector);
    }
    
    @Override
    protected Set<Long> getChannelIDs() {
        return channelIDSet;
    }
    
    @Override
    protected double getGainConditions(long channelID) {
        if(channelToGainsMap.containsKey(Long.valueOf(channelID))) {
            return channelToGainsMap.get(Long.valueOf(channelID)).getGain() * factorGainConversion;
        } else {
            throw new IllegalArgumentException("No gain conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected double getNoiseConditions(long channelID) {
        if(channelToCalibrationsMap.containsKey(Long.valueOf(channelID))) {
            return channelToCalibrationsMap.get(Long.valueOf(channelID)).getNoise();
        } else {
            throw new IllegalArgumentException("No noise conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected double getPedestalConditions(long channelID) {
        if(channelToCalibrationsMap.containsKey(Long.valueOf(channelID))) {
            return channelToCalibrationsMap.get(Long.valueOf(channelID)).getPedestal();
        } else {
            throw new IllegalArgumentException("No pedestal conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected double getTimeShiftConditions(long channelID) {
        if(channelToTimeShiftsMap.containsKey(Long.valueOf(channelID))) {
            return channelToTimeShiftsMap.get(Long.valueOf(channelID)).getTimeShift();
        } else {
            throw new IllegalArgumentException("No time shift conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected int getTimestampFlag() {
        return ReadoutTimestamp.SYSTEM_HODOSCOPE;
    }
    
    /**
     * Populates the channel ID set and maps all existing channels to
     * their respective conditions.
     */
    private void populateChannelCollections() {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeGainCollection gains = conditions.getCachedConditions(HodoscopeGainCollection.class, "hodo_gains").getCachedData();
        final HodoscopeChannelCollection channels = conditions.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        final HodoscopeTimeShiftCollection timeShifts = conditions.getCachedConditions(HodoscopeTimeShiftCollection.class, "hodo_time_shifts").getCachedData();
        final HodoscopeCalibrationCollection calibrations = conditions.getCachedConditions(HodoscopeCalibrationCollection.class, "hodo_calibrations").getCachedData();
        
        // Map the gains to channel IDs.
        for(HodoscopeGain gain : gains) {
            channelToGainsMap.put(Long.valueOf(gain.getChannelId().intValue()), gain);
        }
        
        // Map the pedestals and noise to channel IDs.
        for(HodoscopeCalibration calibration : calibrations) {
            channelToCalibrationsMap.put(Long.valueOf(calibration.getChannelId().intValue()), calibration);
        }
        
        // Map time shifts to channel IDs.
        for(HodoscopeTimeShift timeShift :  timeShifts) {
            channelToTimeShiftsMap.put(Long.valueOf(timeShift.getChannelId().intValue()), timeShift);
        }
        
        // Store the set of all channel IDs.
        for(HodoscopeChannel channel : channels) {
            channelIDSet.add(Long.valueOf(channel.getChannelId().intValue()));
        }
    }
    
    /**
     * Sets factor for gain conversion from self-defined unit/ADC to MeV/ADC
     * @param factor - factor for gain conversion from self-defined-unit/ADC to MeV/ADC.
     */
    public void setFactorGainConversion(double factor) {
        factorGainConversion = factor;
    }
}