package org.hps.conditions.ecal;

import java.util.logging.Logger;

import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.log.LogUtil;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.hps.conditions.svt.AbstractSvtConditionsConverter;

/**
 * This class loads all ECal conditions into an {@link EcalConditions} object
 * from the database, based on the current run number known by the conditions
 * manager.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public final class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {

    static Logger logger = LogUtil.create(AbstractSvtConditionsConverter.class);
    
    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the ECal channel map from the conditions database
        EcalChannelCollection channels = dbConditionsManager.getCollection(EcalChannelCollection.class);

        // Create the ECal conditions object that will be used to encapsulate
        // ECal conditions collections
        EcalConditions conditions = new EcalConditions();

        // Set the channel map.
        conditions.setChannelCollection(channels);

        // Get the ECal gains from the conditions database and add them to the
        // conditions set
        EcalGainCollection gains = dbConditionsManager.getCollection(EcalGainCollection.class);
        for (EcalGain gain : gains.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] { gain.getChannelId() });
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }

        // Get the ECal bad channels and add them to the conditions set
        try {
            EcalBadChannelCollection badChannels = dbConditionsManager.getCollection(EcalBadChannelCollection.class);
            for (EcalBadChannel badChannel : badChannels.getObjects()) {
                ChannelId channelId = new ChannelId(new int[] { badChannel.getChannelId() });
                EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setBadChannel(true);
            }
        } catch (RuntimeException e) {
            logger.warning("A set of bad channels were not found.");
        }

        // Get the ECal calibrations from the conditions database and add them
        // to the conditions set.
        EcalCalibrationCollection calibrations = dbConditionsManager.getCollection(EcalCalibrationCollection.class);
        for (EcalCalibration calibration : calibrations.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] { calibration.getChannelId() });
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the ECal time shifts from the conditions database and add them to
        // the conditions set.
        EcalTimeShiftCollection timeShifts = dbConditionsManager.getCollection(EcalTimeShiftCollection.class);
        for (EcalTimeShift timeShift : timeShifts.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] { timeShift.getChannelId() });
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setTimeShift(timeShift);
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
