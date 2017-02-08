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
import org.hps.record.epics.EpicsData;
import org.hps.record.evio.EvioDetectorConditionsProcessor;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test of reading EPICs scalar data from EVIO and writing it out to LCIO.
 */
public class EpicsDataTest extends TestCase {

    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/ScalersTest/hpsecal_004469_1000_events.evio.0";

    public void test() throws IOException {

        // Cache input data file.
        final FileCache cache = new FileCache();
        final File inputFile = cache.getCachedFile(new URL(TEST_FILE_URL));

        // Setup conditions and event building.
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        final LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        manager.addConditionsListener(builder);

        // Setup and run the loop to write out LCIO events with the EPICS scalar data.
        final CompositeLoopConfiguration configuration = new CompositeLoopConfiguration();
        configuration.setDataSourceType(DataSourceType.EVIO_FILE);
        configuration.add(new EvioDetectorConditionsProcessor("HPS-ECalCommissioning-v2"));
        configuration.setLCSimEventBuilder(builder);
        configuration.setFilePath(inputFile.getPath());
        configuration.setProcessingStage(ProcessingStage.LCIO);
        configuration.setStopOnEndRun(false);
        configuration.setStopOnErrors(false);
        final File outputFile = new TestOutputFile("EpicsDataTest.slcio");
        configuration.add(new LCIODriver(outputFile.getPath()));
        final CompositeLoop loop = new CompositeLoop();
        loop.setConfiguration(configuration);
        loop.loop(100);

        // Read back and print out the scalar data.
        final LCSimLoop readLoop = new LCSimLoop();
        readLoop.setLCIORecordSource(outputFile);
        readLoop.add(new Driver() {
            @Override
            public void process(final EventHeader event) {
                final EpicsData data = EpicsData.read(event);
                if (data != null) {
                    System.out.println("read back EPICS data ...");
                    System.out.println(data.toString());
                }
            }
        });
        readLoop.loop(-1);
    }

}
