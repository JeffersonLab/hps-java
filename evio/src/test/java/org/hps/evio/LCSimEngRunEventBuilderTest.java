package org.hps.evio;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.EventHeader;
import org.lcsim.lcio.LCIOWriter;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test conversion of EVIO to LCIO for Engineering Run EVIO data. This is ECAL
 * data only for now.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LCSimEngRunEventBuilderTest extends TestCase {

    public void testLCSimEngRunEventBuilder() throws Exception {
        
        // Setup database conditions.
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector("HPS-Proposal2014-v8-6pt6", 2000);

        // Configure LCIO writer.
        new TestOutputFile(getClass().getSimpleName()).mkdirs();
        File lcioFile = new TestOutputFile(getClass().getSimpleName() + File.separator + getClass().getSimpleName() + "_output.slcio");
        LCIOWriter writer;
        try {
            writer = new LCIOWriter(lcioFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Create event builder.
        LCSimEventBuilder builder = new LCSimEngRunEventBuilder();
        conditionsManager.addConditionsListener(builder);
        //builder.setDetectorName("HPS-Proposal2014-v8-6pt6");
        conditionsManager.setDetector("HPS-Proposal2014-v8-6pt6", 2744);

        // Get remote test file.
        FileCache cache = new FileCache();
        File evioFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/LCSimEngRunEventBuilderTest/hps_002744.evio.0"));

        // Open the EVIO reader.
        System.out.println("Opening file " + evioFile);
        EvioReader reader = null;
        try {
            reader = new EvioReader(evioFile);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Run the event builder on the EVIO.
        EvioEvent evioEvent = null;
        while ((evioEvent = reader.nextEvent()) != null) {
            reader.parseEvent(evioEvent);
            builder.readEvioEvent(evioEvent);
            if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
                try {
                    EventHeader lcsimEvent = builder.makeLCSimEvent(evioEvent);
                    System.out.println("created LCSim event #" + lcsimEvent.getEventNumber());
                    writer.write(lcsimEvent);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // Close the LCIO writer.
        writer.flush();
        writer.close();
    }
}