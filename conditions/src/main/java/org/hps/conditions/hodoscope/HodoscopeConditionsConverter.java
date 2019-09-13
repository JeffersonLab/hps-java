package org.hps.conditions.hodoscope;

import org.hps.conditions.hodoscope.HodoscopeCalibration.HodoscopeCalibrationCollection;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.conditions.hodoscope.HodoscopeGain.HodoscopeGainCollection;
import org.hps.conditions.hodoscope.HodoscopeTimeShift.HodoscopeTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

public class HodoscopeConditionsConverter implements ConditionsConverter<HodoscopeConditions> {
              
    public final HodoscopeConditions getData(final ConditionsManager manager, final String name) {
       
        final HodoscopeChannelCollection channels = 
                manager.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        
        if (channels == null) {
            throw new IllegalStateException("The Hodoscope channels collection is null.");
        }
                      
        final HodoscopeConditions conditions = new HodoscopeConditions();

        conditions.setChannelCollection(channels);

        final HodoscopeGainCollection gains = 
                manager.getCachedConditions(HodoscopeGainCollection.class, "hodo_gains").getCachedData();
        for (final HodoscopeGain gain : gains) {
            final HodoscopeChannel channel = channels.findChannel(gain.getChannelId());
            conditions.getChannelConstants(channel).setGain(gain);
        }

        final HodoscopeCalibrationCollection calibrations = 
                manager.getCachedConditions(HodoscopeCalibrationCollection.class, "hodo_calibrations").getCachedData();
        for (final HodoscopeCalibration calibration : calibrations) {
            final HodoscopeChannel channel = channels.findChannel(calibration.getChannelId());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        final HodoscopeTimeShiftCollection timeShifts = 
                manager.getCachedConditions(HodoscopeTimeShiftCollection.class, "hodo_time_shifts").getCachedData();
        for (final HodoscopeTimeShift timeShift : timeShifts) {
            final HodoscopeChannel channel = channels.findChannel(timeShift.getChannelId());
            conditions.getChannelConstants(channel).setTimeShift(timeShift);
        }
        
        return conditions;
    }

    public Class<HodoscopeConditions> getType() {
        return HodoscopeConditions.class;
    }
}
