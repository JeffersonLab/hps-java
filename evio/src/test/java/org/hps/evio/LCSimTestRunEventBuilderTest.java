package org.hps.evio;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;

import org.lcsim.event.EventHeader;
import org.lcsim.util.cache.FileCache;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.record.evio.EvioEventUtilities;

/**
 * 	Integration test to check the conversion of test run EVIO to LCIO 
 * 
 * 	@author Omar Moreno <omoreno1@ucsc.edu>
 *	@date November 20, 2014
 */
public class LCSimTestRunEventBuilderTest extends TestCase {

	//-----------------//
	//--- Constants ---//
	//-----------------//
	private static final String DB_CONFIGURATION
		= "/org/hps/conditions/config/conditions_database_testrun_2012.xml";
	
	public void testLCSimTestRunEventBuilder() throws Exception { 
	
		// Configure the conditions system to retrieve test run conditions fo run 1351.
		DatabaseConditionsManager conditionsManager = new DatabaseConditionsManager();
		conditionsManager.setXmlConfig(DB_CONFIGURATION);
		
		// Create the test run event builder
		LCSimTestRunEventBuilder builder = new LCSimTestRunEventBuilder();
		conditionsManager.addConditionsListener(builder);

		conditionsManager.setDetector("HPS-TestRun-v5", 1351);

		// Retrieve the remote test file.  The file currently being contains a
		// subset of events from run 1351
		FileCache cache = new FileCache();
		File evioFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/hps1351_test.evio"));
	
		// Instantiate the EVIO reader and open the test file.  If the file
		// can't be found, throw a runtime exception
		EvioReader reader = null;
		try {
			reader = new EvioReader(evioFile);
		} catch (Exception e) {
			throw new RuntimeException(
					"[ " + this.getClass().getSimpleName() + " ]: EVIO file couldn't be opened.");
		}
		
		// Loop through all EVIO events in the file and process them using the
		// event builder.  If the event is a physics event, process the event
		// using the subdetector readers.
		EvioEvent evioEvent = null;
		while ((evioEvent = reader.nextEvent()) != null) {
			reader.parseEvent(evioEvent);
			builder.readEvioEvent(evioEvent);
			if (EvioEventUtilities.isPhysicsEvent(evioEvent)) {
				EventHeader lcsimEvent = builder.makeLCSimEvent(evioEvent);
				System.out.println("[ " + this.getClass().getSimpleName() + " ]: Created event number " + lcsimEvent.getEventNumber());
			}
		}
		
		// Close the EVIO reader
		reader.close();
	}
}
