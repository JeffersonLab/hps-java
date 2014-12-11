package org.hps.conditions.svt;

import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.util.log.LogUtil;

/**
 * Abstract class providing some of the common methods used in creating SVT
 * conditions objects from the database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 *
 * @param <T extends AbstractSvtConditions> SVT conditions object type
 */
public abstract class AbstractSvtConditionsConverter<T extends AbstractSvtConditions> implements ConditionsConverter<T> {

    protected T conditions;
    static Logger logger = LogUtil.create(AbstractSvtConditionsConverter.class);

    /**
     * Create and return the SVT conditions object.
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    public T getData(ConditionsManager manager, String name) {

        DatabaseConditionsManager dbConditionsManager = (DatabaseConditionsManager) manager;

        // Get the SVT calibrations (baseline, noise) from the conditions
        // database
        SvtCalibrationCollection calibrations = dbConditionsManager.getCollection(SvtCalibrationCollection.class);
        for (SvtCalibration calibration : calibrations) {
            AbstractSvtChannel channel = conditions.getChannelMap().findChannel(calibration.getChannelID());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Get the Channel pulse fit parameters from the conditions database
        SvtShapeFitParametersCollection shapeFitParametersCollection = dbConditionsManager.getCollection(SvtShapeFitParametersCollection.class);
        for (SvtShapeFitParameters shapeFitParameters : shapeFitParametersCollection) {
            AbstractSvtChannel channel = conditions.getChannelMap().findChannel(shapeFitParameters.getChannelID());
            conditions.getChannelConstants(channel).setShapeFitParameters(shapeFitParameters);
        }

        // Get the bad channels from the conditions database. If there aren't
        // any bad channels,
        // notify the user and move on.
        try {
            SvtBadChannelCollection badChannels = dbConditionsManager.getCollection(SvtBadChannelCollection.class);
            for (SvtBadChannel badChannel : badChannels) {
                AbstractSvtChannel channel = conditions.getChannelMap().findChannel(badChannel.getChannelId());
                conditions.getChannelConstants(channel).setBadChannel(true);
            }
        } catch (RuntimeException e) {
            logger.warning("A set of SVT bad channels was not found.");
        }

        // Get the gains and offsets from the conditions database
        SvtGainCollection channelGains = dbConditionsManager.getCollection(SvtGainCollection.class);
        for (SvtGain channelGain : channelGains) {
            int channelId = channelGain.getChannelID();
            AbstractSvtChannel channel = conditions.getChannelMap().findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(channelGain);
        }

        return conditions;
    }
}
