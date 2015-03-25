package org.hps.monitoring.application.util;

import java.util.Arrays;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.record.et.EtConnection;
import org.jlab.coda.et.EtConstants;

public final class EtSystemUtil {

    private EtSystemUtil() {        
    }
    
    public static EtConnection createEtConnection(ConfigurationModel config) {
        return EtConnection.createConnection(config.getEtName(), 
                config.getHost(), 
                config.getPort(), 
                config.getBlocking(), 
                config.getQueueSize(), 
                config.getPrescale(), 
                config.getStationName(), 
                config.getStationPosition(), 
                config.getWaitMode(), 
                config.getWaitTime(), 
                config.getChunkSize());
    }
    
    public static int[] createSelectArray() {
        int select[] = new int[EtConstants.stationSelectInts];
        Arrays.fill(select, -1);
        return select;   
    }    
}
