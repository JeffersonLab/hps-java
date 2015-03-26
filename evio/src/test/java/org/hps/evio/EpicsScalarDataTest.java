package org.hps.evio;

import java.io.File;
import java.io.IOException;
import java.net.URL;

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
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test of reading EPICs scalar data from EVIO and writing it out to LCIO.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EpicsScalarDataTest extends TestCase {
    
    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/ScalarsTest/hpsecal_004469_1000_events.evio.0";
        
    public void test() throws IOException {
        
        // Cache input data file.
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(TEST_FILE_URL));
        
        // Setup conditions and event building.
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        manager.addConditionsListener(builder);
        
        // Setup and run the loop to write out LCIO events with the EPICS scalar data.
        CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.add(new EpicsEvioProcessor());
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        configuration.add(new EvioDetectorConditionsProcessor("HPS-ECalCommissioning-v2"));
        configuration.setLCSimEventBuilder(builder);
        configuration.setFilePath(inputFile.getPath());
        configuration.setProcessingStage(ProcessingStage.LCIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(false);
        File outputFile = new TestOutputFile("EpicsScalarDataTest.slcio");
        configuration.add(new LCIODriver(outputFile.getPath()));        
        CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);        
        loop.loop(100);
        
        // Read back and print out the scalar data.
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.setLCIORecordSource(outputFile);
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
