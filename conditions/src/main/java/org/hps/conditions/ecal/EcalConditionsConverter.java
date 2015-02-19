package org.hps.conditions.ecal;

import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.hps.conditions.svt.AbstractSvtConditionsConverter;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.util.log.LogUtil;

/**
 * This class loads all ECal conditions into an {@link EcalConditions} object
 * from the database, based on the current run number known by the conditions
 * manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {

    static Logger logger = LogUtil.create(AbstractSvtConditionsConverter.class);
    
    protected EcalChannelCollection getEcalChannelCollection(DatabaseConditionsManager manager) {
        return manager.getCollection(EcalChannelCollection.class);
    }
    
    protected EcalGainCollection getEcalGainCollection(DatabaseConditionsManager manager) {
        return manager.getCollection(EcalGainCollection.class);
    }
        
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries(DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(EcalBadChannelCollection.class);
    }
    
    protected EcalCalibrationCollection getEcalCalibrationCollection(DatabaseConditionsManager manager) {
        return manager.getCollection(EcalCalibrationCollection.class);
    }
    
    protected EcalTimeShiftCollection getEcalTimeShiftCollection(DatabaseConditionsManager manager) {
        return manager.getCollection(EcalTimeShiftCollection.class);
    }
        
    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager databaseConditionsManager = (DatabaseConditionsManager) manager;

        // Get the ECal channel map from the conditions database
        EcalChannelCollection channels = getEcalChannelCollection(databaseConditionsManager);

        // Create the ECal conditions object that will be used to encapsulate
        // ECal conditions collections
        Detector detector = databaseConditionsManager.getDetectorObject();
        EcalConditions conditions = new EcalConditions(detector.getSubdetector(databaseConditionsManager.getEcalName()));

        // Set the channel map.
        conditions.setChannelCollection(channels);

        // Get the ECal gains from the conditions database and add them to the
        // conditions set
        EcalGainCollection gains = getEcalGainCollection(databaseConditionsManager);
        for (EcalGain gain : gains) {
            ChannelId channelId = new ChannelId(new int[] { gain.getChannelId() });
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }

        ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> badChannelSeries = 
                getEcalBadChannelSeries(databaseConditionsManager);
        // FIXME: How to get EcalBadChannelCollection here instead for the collection type?
        //        API of ConditionsSeries and ConditionsSeriesConverter needs to be changed!
        for (ConditionsObjectCollection<EcalBadChannel> badChannels : badChannelSeries) {
            for (EcalBadChannel badChannel : badChannels) {
                ChannelId channelId = new ChannelId(new int[] { badChannel.getChannelId() });
                EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setBadChannel(true);
            }
        }

        // Get the ECal calibrations from the conditions database and add them
        // to the conditions set.
        EcalCalibrationCollection calibrations = getEcalCalibrationCollection(databaseConditionsManager);
        for (EcalCalibration calibration : calibrations) {
            ChannelId channelId = new ChannelId(new int[] { calibration.getChannelId() });
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the ECal time shifts from the conditions database and add them to
        // the conditions set.
        if (databaseConditionsManager.hasConditionsRecord("ecal_time_shifts")) {
            EcalTimeShiftCollection timeShifts = getEcalTimeShiftCollection(databaseConditionsManager);
            for (EcalTimeShift timeShift : timeShifts) {
                ChannelId channelId = new ChannelId(new int[] {timeShift.getChannelId()});
                EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setTimeShift(timeShift);
            }   
        } else {
            DatabaseConditionsManager.getLogger().warning("no ecal_time_shifts collection found");
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
