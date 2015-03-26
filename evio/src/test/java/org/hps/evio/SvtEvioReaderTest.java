package org.hps.evio;

import junit.framework.TestCase;

import java.io.File;
import java.net.URL;

import org.jlab.coda.jevio.EvioReader; 
import org.jlab.coda.jevio.EvioEvent; 

import org.lcsim.event.EventHeader; 
import org.lcsim.util.cache.FileCache;

import org.hps.record.evio.EvioEventUtilities; 
import org.hps.record.LCSimEventBuilder; 
import org.hps.conditions.database.DatabaseConditionsManager; 

public class SvtEvioReaderTest extends TestCase {

	public void testSvtEvioReaderTest() throws Exception { 

		// Get the EVIO file that will be used to test the reader
		FileCache fileCache = new FileCache(); 
		File evioFile = fileCache.getCachedFile(
				new URL("http://www.lcsim.org/test/hps-java/svt_evio_reader_test.evio")); 

		System.out.println("[ " + this.getClass().getSimpleName() + " ]: Opening file " + evioFile); 

		// Instantiate the EVIO reader and open the file
		EvioReader evioReader = new EvioReader(evioFile); 
	
		// Instantiate the SVT EVIO reader
		SvtEvioReader svtReader = new SvtEvioReader(); 

		// Setup the database conditions 
		DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
		conditionsManager.setDetector("HPS-Proposal2014-v8-2pt2", 0); 

		// Instantiate the event builder
		LCSimEventBuilder eventBuilder = new LCSimEngRunEventBuilder(); 

		// Check that the file contains the expected number of events
		int eventCount = evioReader.getEventCount(); 
		System.out.println("[ " + this.getClass().getSimpleName() + " ]: File " + evioFile 
				+ " contains " + eventCount + " events."); 


		// Loop through the EVIO events and process them.
		EvioEvent evioEvent = null; 
		while ((evioEvent = evioReader.nextEvent()) != null) { 
			evioReader.parseEvent(evioEvent); 	

			// Only process physics events
			if (!EvioEventUtilities.isPhysicsEvent(evioEvent)) continue;
			System.out.println("[ " + this.getClass().getSimpleName() + " ]: Found physics event."); 	
	
			EventHeader lcsimEvent = eventBuilder.makeLCSimEvent(evioEvent); 
			System.out.println("[ " + this.getClass().getSimpleName() + " ]: Created LCSim event # " + lcsimEvent.getEventNumber()); 	

			// Process the event using the SVT evio reader
			svtReader.processEvent(evioEvent, lcsimEvent);  
		}
	}
}
