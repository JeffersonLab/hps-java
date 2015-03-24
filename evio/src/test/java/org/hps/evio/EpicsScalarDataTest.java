package org.hps.evio;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.epics.EpicsScalarData;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;

public class EpicsScalarDataTest extends TestCase {
    
    public void test() throws IOException {
        
        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        manager.addConditionsListener(builder);
        
        CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.add(new EpicsEvioProcessor());
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        //configuration.setFilePath("/u1/data/hps/eng_run/hps_004385.evio.0");
        configuration.add(new EvioDetectorConditionsProcessor("HPS-ECalCommissioning-v2"));
        configuration.setLCSimEventBuilder(builder);
        configuration.setFilePath("/work/data/hps/engrun/hps_004385.evio.0");
        configuration.setProcessingStage(ProcessingStage.LCIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(false);
        configuration.add(new LCIODriver("EpicsScalarDataTest"));        
        CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);        
        loop.loop(100);                
        
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.setLCIORecordSource(new File("EpicsScalarDataTest.slcio"));
        readLoop.add(new Driver() {
            public void process(EventHeader event) {                
                EpicsScalarData data = EpicsScalarData.read(event);
                if (data != null) {
                    System.out.println("read back EPICS data ...");
                    System.out.println(data.toString());
                }                   
            }
        });
        
        readLoop.loop(-1);
    }

}
