package org.hps.conditions.ecal;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

/**
 * This class loads all ECAL conditions into an {@link EcalConditions} object from the database, based on the current
 * run number known by the conditions manager.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @author <a href="mailto:omoreno1@ucsc.edu">Omar Moreno</a>
 * @see EcalConditions
 * @see EcalChannel
 * @see EcalGain
 * @see EcalCalibration
 * @see EcalBadChannel
 * @see EcalTimeShift
 */
public class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {

    /**
     * Get the default {@link EcalChannel} collection.
     *
     * @param manager the conditions manager
     * @return the default ECAL channel object collection
     */
    protected EcalChannelCollection getEcalChannelCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
    }

    /**
     * Get the default {@link EcalGain} collection.
     *
     * @param manager the conditions manager
     * @return the ECAL channel gain collection
     */
    protected EcalGainCollection getEcalGainCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalGainCollection.class, "ecal_gains").getCachedData();
    }

    /**
     * Get the default collections of {@link EcalBadChannel} objects.
     *
     * @param manager the conditions manager
     * @return the collections of ECAL bad channel objects
     */
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries(
            final DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(EcalBadChannelCollection.class, "ecal_bad_channels");
    }

    /**
     * Get the default {@link EcalCalibration} collection.
     *
     * @param manager the conditions manager
     * @return the collection of ECAL channel calibration objects
     */
    protected EcalCalibrationCollection getEcalCalibrationCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
    }

    /**
     * Get the default {@link EcalTimeShift} collection.
     *
     * @param manager the conditions manager
     * @return the collection of ECAL time shift objects
     */
    protected EcalTimeShiftCollection getEcalTimeShiftCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(EcalTimeShiftCollection.class, "ecal_time_shifts").getCachedData();
    }

    /**
     * Create combined ECAL conditions object containing all data for the current run.
     *
     * @param manager the conditions manager
     * @param name the conditions set name (unused but must satisfy conditions API)
     */
    @Override
    public final EcalConditions getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager databaseConditionsManager = (DatabaseConditionsManager) manager;

        // Get the ECal channel map from the conditions database
        final EcalChannelCollection channels = getEcalChannelCollection(databaseConditionsManager);

        // Create the ECal conditions object that will be used to encapsulate
        // ECal conditions collections
        final Detector detector = databaseConditionsManager.getDetectorObject();
        final EcalConditions conditions = new EcalConditions(detector.getSubdetector(databaseConditionsManager
                .getEcalName()));

        // Set the channel map.
        conditions.setChannelCollection(channels);

        // Get the ECal gains from the conditions database and add them to the
        // conditions set
        final EcalGainCollection gains = getEcalGainCollection(databaseConditionsManager);
        for (final EcalGain gain : gains) {
            final ChannelId channelId = new ChannelId(new int[] {gain.getChannelId()});
            final EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(gain);
        }

        final ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> badChannelSeries = getEcalBadChannelSeries(databaseConditionsManager);
        // FIXME: How to get EcalBadChannelCollection here instead for the collection type?
        // API of ConditionsSeries and ConditionsSeriesConverter needs to be changed!
        for (final ConditionsObjectCollection<EcalBadChannel> badChannels : badChannelSeries) {
            for (final EcalBadChannel badChannel : badChannels) {
                final ChannelId channelId = new ChannelId(new int[] {badChannel.getChannelId()});
                final EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setBadChannel(true);
            }
        }

        // Get the ECal calibrations from the conditions database and add them
        // to the conditions set.
        final EcalCalibrationCollection calibrations = getEcalCalibrationCollection(databaseConditionsManager);
        for (final EcalCalibration calibration : calibrations) {
            final ChannelId channelId = new ChannelId(new int[] {calibration.getChannelId()});
            final EcalChannel channel = channels.findChannel(channelId);
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the ECal time shifts from the conditions database and add them to
        // the conditions set.
        if (databaseConditionsManager.hasConditionsRecord("ecal_time_shifts")) {
            final EcalTimeShiftCollection timeShifts = getEcalTimeShiftCollection(databaseConditionsManager);
            for (final EcalTimeShift timeShift : timeShifts) {
                final ChannelId channelId = new ChannelId(new int[] {timeShift.getChannelId()});
                final EcalChannel channel = channels.findChannel(channelId);
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
     *
     * @return the type handled by this converter
     */
    @Override
    public final Class<EcalConditions> getType() {
        return EcalConditions.class;
    }
}
