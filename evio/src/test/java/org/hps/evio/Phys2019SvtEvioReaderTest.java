package org.hps.evio;

import java.io.File;
import java.net.URL;
import java.util.Vector;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtConditions;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.cache.FileCache;

/**
 *  Test used to check the EVIO reader that will be used for the 2019 Physics
 *  Run.  
 * 
 *  @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class Phys2019SvtEvioReaderTest extends TestCase {

    // Initialize the logger
    protected static Logger LOGGER = Logger.getLogger(Phys2019SvtEvioReaderTest.class.getPackage().getName());
   
    /**
     * Name of SVT subdetector.
     */
    public static final String SVT_SUBDETECTOR_NAME = "Tracker";

    public void testPhys2019SvtEvioReaderTest() throws Exception { 

        // Get the EVIO file that will be used to test the reader
        File evioFile = new File("hpssvt_009280.evio.00000"); 

        LOGGER.info("Opening file " + evioFile); 

        // Instantiate the EVIO reader and open the file
        EvioReader evioReader = new EvioReader(evioFile);

        // Instantiate the SVT EVIO reader
        Phys2019SvtEvioReader svtReader = new Phys2019SvtEvioReader(); 

        // Setup the database conditions 
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setDetector("HPS-PhysicsRun2019-v1-4pt5", 2000000);

        // Get the detector.
        final Detector detector = conditionsManager.getCachedConditions(Detector.class, "compact.xml").getCachedData();

        // Get all SVT conditions.
        final SvtConditions conditions = conditionsManager.getCachedConditions(SvtConditions.class, "svt_conditions")
                .getCachedData();

        // Load the SVT conditions onto detector.
        final SvtDetectorSetup loader = new SvtDetectorSetup("Tracker");
        loader.loadDefault(detector.getSubdetector(SVT_SUBDETECTOR_NAME), conditions);

        // Instantiate the event builder
        LCSimEventBuilder eventBuilder = new LCSimPhys2019EventBuilder(); 

        // Check that the file contains the expected number of events
        int eventCount = evioReader.getEventCount(); 
        LOGGER.info("File " + evioFile + " contains " + eventCount + " events."); 


        // Loop through the EVIO events and process them.
        EvioEvent evioEvent = null; 
        while ((evioEvent = evioReader.nextEvent()) != null) {

            // Parse the EVIO event to map out the underlying structure.
            evioReader.parseEvent(evioEvent);   

            if (evioEvent.getHeader().getTag() != 0x00fe) continue; 
            
            EventHeader lcsimEvent = eventBuilder.makeLCSimEvent(evioEvent); 
        }
    }
}
