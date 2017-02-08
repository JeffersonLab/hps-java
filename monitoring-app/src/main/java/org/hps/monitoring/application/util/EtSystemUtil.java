package org.hps.monitoring.application.util;

import java.util.Arrays;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;

/**
 * ET system utilities.
 */
public final class EtSystemUtil {

    /**
     * Create an {@link org.hps.record.et.EtConnection} from the settings in a
     * {@link org.hps.monitoring.application.model.ConfigurationModel}.
     *
     * @param config the {@link org.hps.monitoring.application.model.ConfigurationModel} with the connection settings
     * @return the new {@link org.hps.record.et.EtConnection}
     */
    public static EtConnection createEtConnection(final ConfigurationModel config) {
        return EtConnection.createConnection(config.getEtName(), config.getHost(), config.getPort(),
                config.getBlocking(), config.getQueueSize(), config.getPrescale(), config.getStationName(),
                config.getStationPosition(), config.getWaitMode(), config.getWaitTime(), config.getChunkSize());
    }

    /**
     * Create an event selection array (with size 6).
     *
     * @return the event selection array
     */
    public static int[] createSelectArray() {
        final int select[] = new int[EtConstants.stationSelectInts];
        Arrays.fill(select, -1);
        return select;
    }

    /**
     * Do not allow class instantiation.
     */
    private EtSystemUtil() {
        throw new UnsupportedOperationException("Do not instantiate this class.");
    }
}
