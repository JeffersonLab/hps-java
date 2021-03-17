package org.hps.evio;

import java.io.File;

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
import org.hps.util.test.TestUtil;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

import junit.framework.TestCase;

/**
 * Test of reading scaler data from EVIO files.
 */
public class ScalersTest extends TestCase {


    public void test() throws Exception {

        File inputFile = TestUtil.downloadTestFile("hpsecal_004469_1000_events.evio.0");

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
