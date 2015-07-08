package org.hps.conditions.svt;

import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.log.LogUtil;

/**
 * Abstract class providing some of the common methods used in creating SVT conditions objects from the database.
 *
 * @author Omar Moreno, UCSC
 * @param <T> SVT conditions object type
 */
public abstract class AbstractSvtConditionsConverter<T extends AbstractSvtConditions> implements ConditionsConverter<T> {

    /**
     * Initialize logging.
     */
    static Logger logger = LogUtil.create(AbstractSvtConditionsConverter.class);

    /**
     * The combined detector conditions object.
     */
    // FIXME: Should be private with accessor methods.
    protected T conditions;

    /**
     * Create and return the SVT conditions object.
     *
     * @param manager the current conditions manager
     * @param name the conditions key, which is ignored for now
     * @return the SVT conditions object
     */
    @Override
    public T getData(final ConditionsManager manager, final String name) {

        final DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the SVT calibrations (baseline, noise) from the conditions database
        final SvtCalibrationCollection calibrations = this.getSvtCalibrationCollection(dbConditionsManager);
        for (final SvtCalibration calibration : calibrations) {
            final AbstractSvtChannel channel = this.conditions.getChannelMap().findChannel(calibration.getChannelID());
            this.conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the Channel pulse fit parameters from the conditions database
        final SvtShapeFitParametersCollection shapeFitParametersCollection = this
                .getSvtShapeFitParametersCollection(dbConditionsManager);
        for (final SvtShapeFitParameters shapeFitParameters : shapeFitParametersCollection) {
            final AbstractSvtChannel channel = this.conditions.getChannelMap().findChannel(
                    shapeFitParameters.getChannelID());
            this.conditions.getChannelConstants(channel).setShapeFitParameters(shapeFitParameters);
        }

        // Get the bad channels from the conditions database.
        // If there aren't any bad channels, notify the user and move on.
        try {
            final ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> badChannelSeries = this
                    .getSvtBadChannelSeries(dbConditionsManager);
            for (final ConditionsObjectCollection<SvtBadChannel> badChannelCollection : badChannelSeries) {
                for (final SvtBadChannel badChannel : badChannelCollection) {
                    final AbstractSvtChannel channel = this.conditions.getChannelMap().findChannel(
                            badChannel.getChannelId());
                    this.conditions.getChannelConstants(channel).setBadChannel(true);
                }
            }
        } catch (final RuntimeException e) {
            logger.warning("A set of SVT bad channels was not found.");
        }

        // Get the gains and offsets from the conditions database
        final SvtGainCollection channelGains = this.getSvtGainCollection(dbConditionsManager);
        for (final SvtGain channelGain : channelGains) {
            final int channelId = channelGain.getChannelID();
            final AbstractSvtChannel channel = this.conditions.getChannelMap().findChannel(channelId);
            this.conditions.getChannelConstants(channel).setGain(channelGain);
        }

        return this.conditions;
    }

    /**
     * Get the default {@link SvtBadChannelCollection} collection from the manager.
     *
     * @param manager the current conditions manager
     * @return the default {@link SvtBadChannelCollection}
     */
    protected SvtBadChannelCollection getSvtBadChannelCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtBadChannelCollection.class, "svt_bad_channels").getCachedData();
    }

    /**
     * Get the default series of {@link SvtBadChannelCollection} collections from the manager.
     *
     * @param manager the current conditions manager
     * @return the default series of {@link SvtBadChannelCollection}
     */
    protected ConditionsSeries<SvtBadChannel, SvtBadChannelCollection> getSvtBadChannelSeries(
            final DatabaseConditionsManager manager) {
        return manager.getConditionsSeries(SvtBadChannelCollection.class, "svt_bad_channels");
    }

    /**
     * Get the default {@link SvtCalibrationCollection} collection from the manager.
     *
     * @param manager the current conditions manager
     * @return the default {@link SvtCalibrationCollection}
     */
    protected SvtCalibrationCollection getSvtCalibrationCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtCalibrationCollection.class, "svt_calibrations").getCachedData();
    }

    /**
     * Get the default {@link SvtGainCollection} collection from the manager.
     *
     * @param manager the current conditions manager
     * @return the default {@link SvtGainCollection}
     */
    protected SvtGainCollection getSvtGainCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtGainCollection.class, "svt_gains").getCachedData();
    }

    /**
     * Get the default {@link SvtShapeFitParametersCollection} collection from the manager.
     *
     * @param manager the current conditions manager
     * @return the default {@link SvtShapeFitParametersCollection}
     */
    protected SvtShapeFitParametersCollection getSvtShapeFitParametersCollection(final DatabaseConditionsManager manager) {
        return manager.getCachedConditions(SvtShapeFitParametersCollection.class, "svt_shape_fit_parameters")
                .getCachedData();
    }
}
