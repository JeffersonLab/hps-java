package org.hps.evio;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.CompositeLoopConfiguration;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.hps.record.scalars.ScalarData;
import org.hps.record.scalars.ScalarsEvioProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test of reading scalar data from EVIO files.
 * <p>
 * <a href="https://jira.slac.stanford.edu/browse/HPSJAVA-470">HPSJAVA-470</a>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ScalarsTest extends TestCase {
    
   static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/ScalarsTest/hpsecal_004469_1000_events.evio.0";
    
    public void test() throws Exception {
        
        // Cache the input EVIO file (1000 events).
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(TEST_FILE_URL));
        
        // Setup conditions and event building.
        DatabaseConditionsManager manager = new DatabaseConditionsManager();
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        manager.addConditionsListener(builder);
        
        // Configure and run the job to write out the LCIO from EVIO.
        CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.add(new ScalarsEvioProcessor());
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        configuration.add(new EvioDetectorConditionsProcessor("HPS-ECalCommissioning-v2"));
        configuration.setLCSimEventBuilder(builder);
        configuration.setFilePath(inputFile.getPath());
        configuration.setProcessingStage(ProcessingStage.LCIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(true);        
        TestOutputFile outputFile = new TestOutputFile("ScalarsTest.slcio");        
        configuration.add(new LCIODriver(outputFile.getPath()));
        CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);
        loop.loop(1000);                
        
        // Read back the LCIO data and perform calculations referenced in JIRA item.
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.setLCIORecordSource(outputFile);
        readLoop.add(new Driver() {
            public void process(EventHeader event) {
                ScalarData data = ScalarData.read(event);
                if (data != null) {
                    System.out.println("Driver got ScalarData in LCIO event ...");
                    System.out.println(data.toString());

                    // [03] - gated faraday cup with "TDC" threshold
                    int word03 = data.getValue(3);
                    
                    // [19] - gated faraday cup with "TRG" threshold
                    int word19 = data.getValue(19);

                    // [35] - ungated faraday cup with "TDC" threshold
                    int word35 = data.getValue(35);
                    
                    // [51] - ungated faraday cup with "TRG" threshold
                    int word51 = data.getValue(51);

                    // [67] - gated clock
                    int word67 = data.getValue(67);

                    // [68] - ungated clock
                    int word68 = data.getValue(68);
                    
                    // [03]/[35] = FCUP TDC
                    double fcupTdc = (double) word03 / (double) word35;
                    
                    // [19]/[51] = FCUP TRG
                    double fcupTrg = (double) word19 / (double) word51;
                    
                    // [19]/[51] = FCUP TRG
                    double clock = (double) word67 / (double) word68;
                    
                    System.out.println("word03 = " + word03);
                    System.out.println("word19 = " + word19);
                    System.out.println("word35 = " + word35);
                    System.out.println("word51 = " + word51);
                    System.out.println("word67 = " + word67);
                    System.out.println("word68 = " + word68);
                    
                    System.out.println("fcupTdc = " + fcupTdc);
                    System.out.println("fcupTrg = " + fcupTrg);
                    System.out.println("clock = " + clock);
                }
            }
        });        
        readLoop.loop(-1);
    }

}
