package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.DevDatabaseReadOnlyConfig;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;


/**
 * This test checks if the SVT DAQ map was loaded with reasonable values and
 * is being read correctly from the conditions database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id$ 
 */
public class SvtDaqMappingTest extends TestCase {

	//--- Constants ---//
	//-----------------//
	
	// DAQ map table name
	public static final String DAQ_MAP_TABLE_NAME = "eng_run_svt_daq_map";
	// Total number of SVT sensors
	public static final int TOTAL_NUMBER_OF_SENSORS = 36;
	// Min and max values of front end boad (FEB) hybrid ID's
	public static final int MIN_FEB_HYBRID_ID = 0; 
	public static final int MAX_FEB_HYBRID_ID = 3; 
	
	//-----------------//

	DevDatabaseReadOnlyConfig dbConfig = new DevDatabaseReadOnlyConfig();
	
	public void setUp(){
		dbConfig.setup();
		dbConfig.load("HPS-Proposal2014-v7-2pt2", 0);
	}
	
	public void test(){
		
		DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
		
		SvtDaqMappingCollection daqMappingCollection 
			= conditionsManager.getConditionsData(SvtDaqMappingCollection.class, DAQ_MAP_TABLE_NAME);
		
		int totalSensors = 0; 
		int febHybridID; 
		this.printDebug("");
		for(SvtDaqMapping daqMapping : daqMappingCollection){
			
			this.printDebug("Sensor: \n" + daqMapping.toString());
			
			// Check that the FEB Hybrid ID is within the allowable limits
			febHybridID = daqMapping.getFebHybridID();
			assertTrue("FEB Hybrid ID is out of range!.", febHybridID >= MIN_FEB_HYBRID_ID && febHybridID <= MAX_FEB_HYBRID_ID);
			
			totalSensors++;
		}
		
		this.printDebug("Total number of sensors found: " + totalSensors);
		assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);
		
	}
	
	private void printDebug(String debugMessage){
		System.out.println(this.getClass().getSimpleName() + ":: " + debugMessage);
	}
	
	
}
