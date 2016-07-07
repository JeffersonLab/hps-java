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
import org.hps.record.scalers.ScalerData;
import org.hps.record.scalers.ScalerUtilities;
import org.hps.record.scalers.ScalerUtilities.LiveTimeIndex;
import org.hps.rundb.RunManager;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test of reading scaler data from EVIO files.
 * <p>
 * <a href="https://jira.slac.stanford.edu/browse/HPSJAVA-470">HPSJAVA-470</a>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ScalersTest extends TestCase {
    
   static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/ScalersTest/hpsecal_004469_1000_events.evio.0";
    
    public void test() throws Exception {
        
        // Cache the input EVIO file (1000 events).
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL(TEST_FILE_URL));
        
        // Setup conditions and event building.
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        manager.addConditionsListener(builder);
        
        // This needs to be set as the builder depends on it now.
        RunManager.getRunManager().setRun(4469);
                
        // Configure and run the job to write out the LCIO from EVIO.
        CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        configuration.add(new EvioDetectorConditionsProcessor("HPS-ECalCommissioning-v2"));
        configuration.setLCSimEventBuilder(builder);
        configuration.setFilePath(inputFile.getPath());
        configuration.setProcessingStage(ProcessingStage.LCIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(true);        
        TestOutputFile outputFile = new TestOutputFile("ScalersTest.slcio");        
        configuration.add(new LCIODriver(outputFile.getPath()));
        CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);
        loop.loop(1000);                
        
        // Read back the LCIO data and perform calculations referenced in JIRA item.
        LCSimLoop readLoop = new LCSimLoop();
        readLoop.setLCIORecordSource(outputFile);
        readLoop.add(new Driver() {
            public void process(EventHeader event) {
                ScalerData data = ScalerData.read(event);
                if (data != null) {
                    System.out.println("Driver got ScalerData in LCIO event ...");
                    System.out.println(data.toString());
                    
                    double fcupTdc = ScalerUtilities.getLiveTime(data, LiveTimeIndex.FCUP_TDC);
                    double fcupTrg = ScalerUtilities.getLiveTime(data, LiveTimeIndex.FCUP_TRG);
                    double clock = ScalerUtilities.getLiveTime(data, LiveTimeIndex.CLOCK);

                    System.out.println("calculated live times ...");
                    System.out.println(LiveTimeIndex.FCUP_TDC.toString() + " = " + fcupTdc);
                    System.out.println(LiveTimeIndex.FCUP_TRG.toString() + " = " + fcupTrg);
                    System.out.println(LiveTimeIndex.CLOCK.toString() + " = " + clock);
                }
            }
        });        
        readLoop.loop(-1);
    }
}
