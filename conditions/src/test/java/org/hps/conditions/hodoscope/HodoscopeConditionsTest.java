package org.hps.conditions.hodoscope;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeCalibration.HodoscopeCalibrationCollection;
import org.hps.conditions.hodoscope.HodoscopeChannel.HodoscopeChannelCollection;
import org.hps.conditions.hodoscope.HodoscopeGain.HodoscopeGainCollection;
import org.hps.conditions.hodoscope.HodoscopeTimeShift.HodoscopeTimeShiftCollection;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

import junit.framework.TestCase;

public class HodoscopeConditionsTest extends TestCase {
    
    private static final String DETECTOR = "HPS-HodoscopeTest-v1";
    private static final int RUN = 1000000;
    
    public void testHodoscopeConditions() {
        
        final DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        try {
            mgr.setDetector(DETECTOR, RUN);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        final HodoscopeChannelCollection channels = 
                mgr.getCachedConditions(HodoscopeChannelCollection.class, "hodo_channels").getCachedData();
        System.out.println("Printing Hodoscope channels ...");
        for (HodoscopeChannel channel : channels) {
            System.out.println(channel);
        }
     
        final HodoscopeCalibrationCollection calibrations =
                mgr.getCachedConditions(HodoscopeCalibrationCollection.class, "hodo_calibrations").getCachedData();
        System.out.println("Printing Hodoscope calibrations ...");
        for (HodoscopeCalibration calibration : calibrations) {
            System.out.println(calibration);
        }
        
        final HodoscopeGainCollection gains = 
                mgr.getCachedConditions(HodoscopeGainCollection.class, "hodo_gains").getCachedData();
        System.out.println("Printing Hodoscope gains ...");
        for (HodoscopeGain gain : gains) {
            System.out.println(gain);
        }
        
        final HodoscopeTimeShiftCollection timeShifts = 
                mgr.getCachedConditions(HodoscopeTimeShiftCollection.class, "hodo_time_shifts").getCachedData();
        for (HodoscopeTimeShift timeShift : timeShifts) {
            System.out.println(timeShift);
        }
        
        System.out.println("Printing Hodoscope channel constants ...");
        final HodoscopeConditions conditions =
                mgr.getCachedConditions(HodoscopeConditions.class, "hodo_conditions").getCachedData();
        for (HodoscopeChannel channel : conditions.getChannels()) {
            HodoscopeChannelConstants channelData = conditions.getChannelConstants(channel);
            System.out.println("Channel constants for ID " + channel.getChannelId());
            System.out.println(channelData.getCalibration());
            System.out.println(channelData.getGain());
            System.out.println(channelData.getTimeShift());
            System.out.println();
        }
    }

}
