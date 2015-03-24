package org.hps.record.epics;

import junit.framework.TestCase;

import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.epics.EpicsEvioProcessor;

public class EpicsScalarDataTest extends TestCase {
    
    public void test() {
        
        CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.add(new EpicsEvioProcessor());
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        configuration.setFilePath("/u1/data/hps/eng_run/hps_004385.evio.0");
        configuration.setProcessingStage(ProcessingStage.EVIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(false);
        
        CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);
        
        loop.loop(-1);        
    }

}
