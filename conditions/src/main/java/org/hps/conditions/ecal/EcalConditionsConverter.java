package org.hps.conditions.ecal;

import static org.hps.conditions.TableConstants.ECAL_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_CALIBRATIONS;
import static org.hps.conditions.TableConstants.ECAL_CHANNELS;
import static org.hps.conditions.TableConstants.ECAL_GAINS;
import static org.hps.conditions.TableConstants.ECAL_TIME_SHIFTS;

import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class loads all ecal conditions into an {@link EcalConditions} object from the
 * database, based on the current run number known by the conditions manager.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {

    /**
     * Create ECAL conditions object containing all data for the current run.
     */
    public EcalConditions getData(ConditionsManager manager, String name) {

        // Create new, empty conditions object to fill with data.
        EcalConditions conditions = new EcalConditions();

        // Get the channel information from the database.
        EcalChannelCollection channels = manager.getCachedConditions(EcalChannelCollection.class, ECAL_CHANNELS).getCachedData();

        // Set the channel map.
        conditions.setChannelCollection(channels);

        System.out.println("channel collection size = " + channels.getObjects().size());

        // Add gains.
        EcalGainCollection gains = manager.getCachedConditions(EcalGainCollection.class, ECAL_GAINS).getCachedData();
        for (EcalGain gain : gains.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {gain.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }

        // Add bad channels.
        EcalBadChannelCollection badChannels = manager.getCachedConditions(EcalBadChannelCollection.class, ECAL_BAD_CHANNELS).getCachedData();
        for (EcalBadChannel badChannel : badChannels.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {badChannel.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setBadChannel(true);
        }

        // Add calibrations including pedestal and noise values.
        EcalCalibrationCollection calibrations = manager.getCachedConditions(EcalCalibrationCollection.class, ECAL_CALIBRATIONS).getCachedData();
        for (EcalCalibration calibration : calibrations.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {calibration.getChannelId()});
            EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Add time shifts.
        EcalTimeShiftCollection timeShifts = manager.getCachedConditions(EcalTimeShiftCollection.class, ECAL_TIME_SHIFTS).getCachedData();
        for (EcalTimeShift timeShift : timeShifts.getObjects()) {
            ChannelId channelId = new ChannelId(new int[] {timeShift.getChannelId()});
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
