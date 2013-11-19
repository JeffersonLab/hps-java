package org.lcsim.hps.conditions.demo;

import java.io.PrintStream;

import junit.framework.TestCase;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;
import org.lcsim.conditions.ConditionsSet;
import org.lcsim.hps.conditions.demo.CalibrationConverter;
import org.lcsim.hps.conditions.demo.Calibration;

/**
 * This class tests that the example DatabaseConditionsReader authored by Dima
 * actually works with a test detector. 
 * @author jeremym
 */
public class DemoDatabaseConditionsReaderTest extends TestCase {
    
    /* Example detector from hps-detectors. */
    private final String detectorName = "HPS-demo-conditions-test";
    
    /* Run number of conditions set. */
    private final int runNumber = 0;
    
    /* Name of conditions set. */
    private final String conditionsSetName = "calibration";
    
    /* Print output. */
    private final PrintStream ps = System.out;
    
    /**
     * Create the manager, load the detector, and then get the conditions meta-data
     * for the selected conditions set.  Finally, use the sample converter to create 
     * a Calibration object from the database rows.
     */
    public void test() {
	ConditionsManager manager = ConditionsManager.defaultInstance();
	try {
	    manager.setDetector(detectorName, runNumber);	    
	} catch (ConditionsNotFoundException e) {
	    throw new RuntimeException(e);
	}		
	ConditionsSet conditions = manager.getConditions(conditionsSetName);
	ps.println("Got conditions " + conditionsSetName + " of size " + conditions.size());
	ps.println("table: " + conditions.getString("table"));
	ps.println("column: " + conditions.getString("column"));
	ps.println("id: " + conditions.getString("id"));
	CalibrationConverter calibrationConverter = new CalibrationConverter();
	Calibration calibration = calibrationConverter.getData(manager, null);
	ps.println("Fetched calibration conditions: ");
	ps.println(calibration.toString());
    }           
}
