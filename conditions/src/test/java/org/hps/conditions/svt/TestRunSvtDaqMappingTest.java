package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.TableMetaData;
import org.hps.conditions.config.TestRunReadOnlyConfiguration;
import org.hps.conditions.svt.TestRunSvtDaqMapping.SvtDaqMappingCollection;

/**
 * This test checks if the test run SVT DAQ map was loaded with reasonable 
 * values and is being read correctly from the conditions database.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunSvtDaqMappingTest extends TestCase {

	TableMetaData metaData = null;
	DatabaseConditionsManager conditionsManager = null;
	
	//--- Constants ---//
	//-----------------//
	
	// Total number of SVT sensors
	public static final int TOTAL_NUMBER_OF_SENSORS = 20;
	// Min and max values of front end boad (FEB) hybrid ID's
	public static final int MIN_HYBRID_ID = 0; 
	public static final int MAX_HYBRID_ID = 2; 
	
	
	
	public void setUp(){
        new TestRunReadOnlyConfiguration().setup().load("HPS-TestRun-v5", 0);
        conditionsManager = DatabaseConditionsManager.getInstance();
	}
	
	public void test(){
		
		metaData = conditionsManager.findTableMetaData(SvtDaqMappingCollection.class);
		SvtDaqMappingCollection daqMappingCollection 
			= conditionsManager.getConditionsData(SvtDaqMappingCollection.class, metaData.getTableName());

	
		int totalSensors = 0; 
		int fpgaID;
		int hybridID; 
		this.printDebug("");
		for(SvtDaqMapping daqMapping : daqMappingCollection){
			
			this.printDebug("Sensor: \n" + ((TestRunSvtDaqMapping) daqMapping).toString());
			
			// Check that the Hybrid ID is within the allowable limits
			hybridID = ((TestRunSvtDaqMapping) daqMapping).getHybridID();
			assertTrue("Hybrid ID is out of range!.", hybridID >= MIN_HYBRID_ID && hybridID <= MAX_HYBRID_ID);
			
			totalSensors++;
		}
		
		this.printDebug("Total number of sensors found: " + totalSensors);
		assertTrue(totalSensors == TOTAL_NUMBER_OF_SENSORS);
	}

	private void printDebug(String debugMessage){
		System.out.println("[ " + this.getClass().getSimpleName() + " ]: " + debugMessage);
	}
}
