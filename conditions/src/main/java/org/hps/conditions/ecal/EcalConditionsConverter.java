package org.hps.conditions.ecal;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalBadChannel.EcalBadChannelCollection;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.ChannelId;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalPulseWidth.EcalPulseWidthCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;

/**
 * This class loads all ECAL conditions into an {@link EcalConditions} object from the database, based on the current
 * run number known by the conditions manager.
 *
 * @author Jeremy McCormick, SLAC
 * @author Omar Moreno, UCSC
 * @see EcalConditions
 * @see EcalChannel
 * @see EcalGain
 * @see EcalCalibration
 * @see EcalBadChannel
 * @see EcalTimeShift
 */
public class EcalConditionsConverter implements ConditionsConverter<EcalConditions> {
    
    /**
     * Setup logger.
     */
    private static Logger LOGGER = Logger.getLogger(EcalConditionsConverter.class.getName());
    static {
        LOGGER.setLevel(Level.ALL);
    }
    
    /**
     * Create combined ECAL conditions object containing all data for the current run.
     *
     * @param manager the conditions manager
     * @param name the conditions set name (unused but must satisfy conditions API)
     */
    @Override
    public final EcalConditions getData(final ConditionsManager manager, final String name) {
       
        // Get the ECal channel map from the conditions database.
        final EcalChannelCollection channels = this.getEcalChannelCollection();
        
        if (channels == null) {
            throw new IllegalStateException("The ECal channels collection is null.");
        }
        if (channels.size() == 0) {
            throw new IllegalStateException("The ECal channels collection is empty.");
        }
        
        LOGGER.fine("ECal channel collection has " + channels.size() + " objects");

        // Create the ECal conditions object that will be used to encapsulate ECal conditions collections.
        final Detector detector = getDatabaseConditionsManager().getDetectorObject();
        final EcalConditions conditions = new EcalConditions(detector.getSubdetector(getDatabaseConditionsManager()
                .getEcalName()));

        // Set the channel map.
        conditions.setChannelCollection(channels);

        // Get the ECal gains from the conditions database and add them to the conditions set
        final EcalGainCollection gains = this.getEcalGainCollection();
        LOGGER.fine("ECal gain collction has " + gains.size() + " objects");
        for (final EcalGain gain : gains) {
            final ChannelId channelId = new ChannelId(new int[] {gain.getChannelId()});
            final EcalChannel channel = channels.findChannel(channelId);
            LOGGER.fine("setting channel " + channel.getChannel() + " gain to " + gain.getGain());
            conditions.getChannelConstants(channel).setGain(gain);
        }

        // Get the bad channel collections and merge them together.
        final ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> badChannelSeries = this
                .getEcalBadChannelSeries();
        for (final ConditionsObjectCollection<EcalBadChannel> badChannels : badChannelSeries) {
            for (final EcalBadChannel badChannel : badChannels) {
                final ChannelId channelId = new ChannelId(new int[] {badChannel.getChannelId()});
                final EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setBadChannel(true);
            }
        }

        // Get the ECal calibrations from the conditions database and add them to the conditions set.
        final EcalCalibrationCollection calibrations = this.getEcalCalibrationCollection();
        LOGGER.fine("ECal calibration collction has " + calibrations.size() + " objects");
        for (final EcalCalibration calibration : calibrations) {
            final ChannelId channelId = new ChannelId(new int[] {calibration.getChannelId()});
            final EcalChannel channel = channels.findChannel(channelId);
            LOGGER.fine("setting channel " + channel.getChannel() + " ped, noise to " + calibration.getPedestal() + ", " + calibration.getNoise());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the ECal time shifts from the conditions database and add them to the conditions set.
        if (getDatabaseConditionsManager().hasConditionsRecord("ecal_time_shifts")) {
            final EcalTimeShiftCollection timeShifts = this.getEcalTimeShiftCollection();
            for (final EcalTimeShift timeShift : timeShifts) {
                final ChannelId channelId = new ChannelId(new int[] {timeShift.getChannelId()});
                final EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setTimeShift(timeShift);
            }
        } else {
            // If time shifts do not exist it is not a fatal error.
            DatabaseConditionsManager.getLogger().warning("no conditions found for EcalTimeShiftCollection");
        }
        
        // Set the channel pulse width if it exists in the database.
        if (getDatabaseConditionsManager().hasConditionsRecord("ecal_pulse_widths")) {
            final EcalPulseWidthCollection pulseWidths = this.getEcalPulseWidthCollection();
            for (final EcalPulseWidth pulseWidth : pulseWidths) {
                final ChannelId channelId = new ChannelId(new int[] {pulseWidth.getChannelId()});
                final EcalChannel channel = channels.findChannel(channelId);
                conditions.getChannelConstants(channel).setPulseWidth(pulseWidth);
            }
        } else {
            // If pulse widths do not exist it is not a fatal error.
            DatabaseConditionsManager.getLogger().warning("no conditions found for EcalPulseWidthCollection");
        }

        // Return the conditions object to caller.
        return conditions;
    }

    /**
     * Get the default collections of {@link EcalBadChannel} objects.
     *
     * @param manager the conditions manager
     * @return the collections of ECAL bad channel objects
     */
    protected ConditionsSeries<EcalBadChannel, EcalBadChannelCollection> getEcalBadChannelSeries() {
        return getDatabaseConditionsManager().getConditionsSeries(EcalBadChannelCollection.class, "ecal_bad_channels");
    }

    /**
     * Get the default {@link EcalCalibration} collection.
     *
     * @param manager the conditions manager
     * @return the collection of ECAL channel calibration objects
     */
    protected EcalCalibrationCollection getEcalCalibrationCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
    }

    /**
     * Get the default {@link EcalChannel} collection.
     *
     * @param manager the conditions manager
     * @return the default ECAL channel object collection
     */
    protected EcalChannelCollection getEcalChannelCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
    }

    /**
     * Get the default {@link EcalGain} collection.
     *
     * @param manager the conditions manager
     * @return the ECAL channel gain collection
     */
    protected EcalGainCollection getEcalGainCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalGainCollection.class, "ecal_gains").getCachedData();
    }

    /**
     * Get the default {@link EcalTimeShift} collection.
     *
     * @param manager the conditions manager
     * @return the collection of ECAL time shift objects
     */
    protected EcalTimeShiftCollection getEcalTimeShiftCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalTimeShiftCollection.class, "ecal_time_shifts").getCachedData();
    }
    
    /**
     * Get the default {@link EcalPulseWith} collection.
     *
     * @param manager the conditions manager
     * @return the collection of ECAL pulse widths
     */
    protected EcalPulseWidthCollection getEcalPulseWidthCollection() {
        return getDatabaseConditionsManager().getCachedConditions(EcalPulseWidthCollection.class, "ecal_pulse_widths").getCachedData();
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
    
    /**
     * Get the current instance of the conditions manager.
     * 
     * @return the current instance of the conditions manager
     */
    protected final DatabaseConditionsManager getDatabaseConditionsManager() {
        return DatabaseConditionsManager.getInstance();
    }
}
