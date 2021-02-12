package org.hps.evio;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

import org.jlab.coda.jevio.EvioReader;
import org.jlab.coda.jevio.EvioEvent;

import org.lcsim.event.EventHeader;
import org.lcsim.util.cache.FileCache;

import org.hps.record.evio.EvioEventUtilities;
import org.hps.util.test.TestUtil;
import org.hps.record.LCSimEventBuilder;
import org.hps.conditions.database.DatabaseConditionsManager;

public class TestRunSvtEvioReaderTest extends TestCase {

    public void testTestRunSvtEvioReaderTest() throws Exception {

        // Get the EVIO file that will be used to test the reader
        File evioFile = TestUtil.downloadTestFile("hps1351_test.evio");

        System.out.println("[ " + this.getClass().getSimpleName() + " ]: Opening file " + evioFile);

        // Instantiate the EVIO reader and open the file
        EvioReader evioReader = new EvioReader(evioFile);

        // Instantiate the SVT EVIO reader
        // TestRunSvtEvioReader svtReader = new TestRunSvtEvioReader();

        // Configure conditions system
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        // Instantiate the event builder
        LCSimEventBuilder eventBuilder = new LCSimTestRunEventBuilder();
        conditionsManager.addConditionsListener(eventBuilder);

        // Activate conditions which will setup event builder
        conditionsManager.setDetector("HPS-TestRun-v5", 0);

        // Check that the file contains the expected number of events
        int eventCount = evioReader.getEventCount();
        System.out.println("[ " + this.getClass().getSimpleName() + " ]: File " + evioFile + " contains " + eventCount + " events.");

        // Loop through the EVIO events and process them.
        EvioEvent evioEvent = null;
        while ((evioEvent = evioReader.nextEvent()) != null) {
            evioReader.parseEvent(evioEvent);

            // Only process physics events
            if (!EvioEventUtilities.isPhysicsEvent(evioEvent))
                continue;
            System.out.println("[ " + this.getClass().getSimpleName() + " ]: Found physics event.");

            EventHeader lcsimEvent = eventBuilder.makeLCSimEvent(evioEvent);
            System.out.println("[ " + this.getClass().getSimpleName() + " ]: Created LCSim event # " + lcsimEvent.getEventNumber());

            // Process the event using the SVT evio reader
            // svtReader.processEvent(evioEvent, lcsimEvent);
        }

    }
}
