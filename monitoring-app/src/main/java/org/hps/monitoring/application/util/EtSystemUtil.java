package org.hps.monitoring.application.util;

import java.util.Arrays;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;

/**
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EtSystemUtil {

    public static EtConnection createEtConnection(final ConfigurationModel config) {
        return EtConnection.createConnection(config.getEtName(), config.getHost(), config.getPort(),
                config.getBlocking(), config.getQueueSize(), config.getPrescale(), config.getStationName(),
                config.getStationPosition(), config.getWaitMode(), config.getWaitTime(), config.getChunkSize());
    }

    public static int[] createSelectArray() {
        final int select[] = new int[EtConstants.stationSelectInts];
        Arrays.fill(select, -1);
        return select;
    }

    private EtSystemUtil() {
    }
}
