package org.hps.conditions.ecal;

import static org.hps.conditions.ConditionsConstants.ECAL_BAD_CHANNELS;
import static org.hps.conditions.ConditionsConstants.ECAL_CALIBRATIONS;
import static org.hps.conditions.ConditionsConstants.ECAL_CHANNELS;
import static org.hps.conditions.ConditionsConstants.ECAL_GAINS;

import java.util.Map.Entry;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.DatabaseConditionsConverter;

/**
 * This class loads all ecal conditions into an {@link EcalConditions} object
 * from the database, based on the current run number known by the conditions manager.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverter extends DatabaseConditionsConverter<EcalConditions> {
       
    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {
        
        // Create new, empty conditions object to fill with data.
        EcalConditions conditions = new EcalConditions();
                               
        // Get the channel information from the database.                
        EcalChannelMap channelMap = manager.getCachedConditions(EcalChannelMap.class, ECAL_CHANNELS).getCachedData();
        
        // Set the channel map.
        conditions.setChannelMap(channelMap);
                                       
        // Add gains.
        EcalGainCollection gains = manager.getCachedConditions(EcalGainCollection.class, ECAL_GAINS).getCachedData();        
        for (Entry<Integer,EcalGain> entry : gains.entrySet()) {
            EcalChannel channel = channelMap.get(entry.getKey());
            EcalGain gain = entry.getValue();
            conditions.getChannelConstants(channel).setGain(gain);
        }
        
        // Add bad channels.
        EcalBadChannelCollection badChannels = manager.getCachedConditions(
                EcalBadChannelCollection.class, ECAL_BAD_CHANNELS).getCachedData();
        for (Integer badChannel : badChannels) {
            EcalChannel channel = channelMap.get(badChannel);
            conditions.getChannelConstants(channel).setBadChannel(true);
        }
        
        // Add calibrations including pedestal and noise values.
        EcalCalibrationCollection calibrations = 
                manager.getCachedConditions(EcalCalibrationCollection.class, ECAL_CALIBRATIONS).getCachedData();
        for (Entry<Integer,EcalCalibration> entry : calibrations.entrySet()) {
            EcalChannel channel = channelMap.get(entry.getKey());
            EcalCalibration calibration = entry.getValue();
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }       
        
        // Return the conditions object to caller.
        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalConditions> getType() {
        return EcalConditions.class;
    }
    
    
}
