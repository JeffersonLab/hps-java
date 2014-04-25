package org.hps.conditions.svt;

import static org.hps.conditions.TableConstants.SVT_BAD_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_CALIBRATIONS;
import static org.hps.conditions.TableConstants.SVT_CHANNELS;
import static org.hps.conditions.TableConstants.SVT_DAQ_MAP;
import static org.hps.conditions.TableConstants.SVT_GAINS;
import static org.hps.conditions.TableConstants.SVT_PULSE_PARAMETERS;
import static org.hps.conditions.TableConstants.SVT_TIME_SHIFTS;

import org.hps.conditions.svt.SvtBadChannel.SvtBadChannelCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtPulseParameters.SvtPulseParametersCollection;
import org.hps.conditions.svt.SvtTimeShift.SvtTimeShiftCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates an {@link SvtConditions} object from the database, based on the
 * current run number known by the conditions manager.
 */
public final class SvtConditionsConverter implements ConditionsConverter<SvtConditions> {

    /**
     * Create and return the SVT conditions object.
     * @param manager The current conditions manager.
     * @param name The conditions key, which is ignored for now.
     */
    public SvtConditions getData(ConditionsManager manager, String name) {

        // Get the SVT channel map.
        SvtChannelCollection channels = manager.getCachedConditions(SvtChannelCollection.class, SVT_CHANNELS).getCachedData();

        // Create the conditions object.
        SvtConditions conditions = new SvtConditions(channels);

        // Create the DAQ map.
        SvtDaqMappingCollection daqMap = manager.getCachedConditions(SvtDaqMappingCollection.class, SVT_DAQ_MAP).getCachedData();
        conditions.setDaqMap(daqMap);

        // Add calibrations by channel.
        SvtCalibrationCollection calibrations = manager.getCachedConditions(SvtCalibrationCollection.class, SVT_CALIBRATIONS).getCachedData();
        for (SvtCalibration calibration : calibrations.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(calibration.getChannelId());
            conditions.getChannelConstants(channel).setCalibration(calibration);
        }

        // Add pulse parameters by channel.
        SvtPulseParametersCollection pulseParametersCollection = manager.getCachedConditions(SvtPulseParametersCollection.class, SVT_PULSE_PARAMETERS).getCachedData();
        for (SvtPulseParameters pulseParameters : pulseParametersCollection.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(pulseParameters.getChannelId());
            conditions.getChannelConstants(channel).setPulseParameters(pulseParameters);
        }

        // Add bad channels.
        SvtBadChannelCollection badChannels = manager.getCachedConditions(SvtBadChannelCollection.class, SVT_BAD_CHANNELS).getCachedData();
        for (SvtBadChannel badChannel : badChannels.getObjects()) {
            SvtChannel channel = conditions.getChannelMap().findChannel(badChannel.getChannelId());
            conditions.getChannelConstants(channel).setBadChannel(true);
        }

        // Add gains by channel.
        SvtGainCollection gains = manager.getCachedConditions(SvtGainCollection.class, SVT_GAINS).getCachedData();
        for (SvtGain object : gains.getObjects()) {
            int channelId = object.getChannelID();
            SvtChannel channel = conditions.getChannelMap().findChannel(channelId);
            conditions.getChannelConstants(channel).setGain(object);
        }

        // Set the time shifts by sensor.
        SvtTimeShiftCollection timeShifts = manager.getCachedConditions(SvtTimeShiftCollection.class, SVT_TIME_SHIFTS).getCachedData();
        conditions.setTimeShifts(timeShifts);

        return conditions;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtConditions> getType() {
        return SvtConditions.class;
    }
}