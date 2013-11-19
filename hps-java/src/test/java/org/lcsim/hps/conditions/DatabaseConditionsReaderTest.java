package org.lcsim.hps.conditions;

import java.io.PrintStream;

import junit.framework.TestCase;

import org.lcsim.conditions.CachedConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * This class tests the DatabaseConditionsReader on dummy data.
 * 
 * @author jeremym
 */
public class DatabaseConditionsReaderTest extends TestCase {
    
    /** Example detector from hps-detectors. */
    private final String detectorName = "HPS-conditions-test";
    
    /** Run number of conditions set. */
    private final int runNumber = 777;
        
    /** Print output. */
    private final PrintStream ps = System.out;
    
    /**
     * Create the manager, load the detector, and then get the conditions meta-data
     * for the selected conditions set.  Finally, use the sample converter to create 
     * an SvtCalibrationConstants object from the database rows.
     */
    public void test() {
	ConditionsManager manager = ConditionsManager.defaultInstance();
	try {
	    manager.setDetector(detectorName, runNumber);
	} catch (ConditionsNotFoundException e) {
	    throw new RuntimeException(e);
	}
			
	CachedConditions<ConditionsRecordCollection> c2 = manager.getCachedConditions(ConditionsRecordCollection.class, "conditions_records");
	ConditionsRecordCollection rc = c2.getCachedData();
	for (ConditionsRecord r : rc) {
	    ps.println(r.toString());
	}	      
    }    
}
