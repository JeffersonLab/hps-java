package org.hps.readout.rawconverter;

import java.util.HashMap;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeCalibration;
import org.hps.conditions.hodoscope.HodoscopeGain;
import org.hps.conditions.hodoscope.HodoscopeTimeShift;
import org.hps.conditions.hodoscope.HodoscopeCalibration.HodoscopeCalibrationCollection;
import org.hps.conditions.hodoscope.HodoscopeGain.HodoscopeGainCollection;
import org.hps.conditions.hodoscope.HodoscopeTimeShift.HodoscopeTimeShiftCollection;
import org.lcsim.geometry.Detector;

/**
 * <code>HodoscopeReadoutMode3RawConverter</code> handles the
 * implementation of hodoscope-specific functionality for {@link
 * org.hps.readout.rawconverter.AbstractMode3RawConverter
 * AbstractMode3RawConverter}. Only the behavior needed for readout
 * is implemented. Calibrations specific to reconstruction are not
 * supported.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.rawconverter.AbstractMode3RawConverter
 */
public class HodoscopeReadoutMode3RawConverter extends AbstractMode3RawConverter {
    /**
     * Maps hodoscope channels to the gain for that channel.
     */
    private Map<Long, HodoscopeGain> channelToGainsMap = new HashMap<Long, HodoscopeGain>();
    
    /**
     * Maps hodoscope channels to the time shifts for that channel.
     */
    private Map<Long, HodoscopeTimeShift> channelToTimeShiftsMap = new HashMap<Long, HodoscopeTimeShift>();
    
    /**
     * Maps hodoscope channels to the noise sigma and pedestals for that channel.
     */
    private Map<Long, HodoscopeCalibration> channelToCalibrationsMap = new HashMap<Long, HodoscopeCalibration>();
    
    @Override
    public void updateDetector(Detector detector) {
        // Load the conditions database and get the hodoscope channel
        // collection data.
        final DatabaseConditionsManager conditions = DatabaseConditionsManager.getInstance();
        final HodoscopeGainCollection gains = conditions.getCachedConditions(HodoscopeGainCollection.class, "hodo_gains").getCachedData();
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
    }
    
    @Override
    protected double getGain(long channelID) {
        if(channelToGainsMap.containsKey(Long.valueOf(channelID))) {
            return channelToGainsMap.get(Long.valueOf(channelID)).getGain();
        } else {
            throw new IllegalArgumentException("No gain conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected double getPedestal(long channelID) {
        if(channelToCalibrationsMap.containsKey(Long.valueOf(channelID))) {
            return channelToCalibrationsMap.get(Long.valueOf(channelID)).getPedestal();
        } else {
            throw new IllegalArgumentException("No pedestal conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
    
    @Override
    protected double getTimeShift(long channelID) {
        if(channelToTimeShiftsMap.containsKey(Long.valueOf(channelID))) {
            return channelToTimeShiftsMap.get(Long.valueOf(channelID)).getTimeShift();
        } else {
            throw new IllegalArgumentException("No time shift conditions exist for hodoscope channel ID \"" + channelID + "\".");
        }
    }
}