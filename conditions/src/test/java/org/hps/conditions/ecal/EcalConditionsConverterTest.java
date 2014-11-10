package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

/**
 * Tests that a {@link EcalConditions} objects loads without errors.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {                
        conditionsManager = DatabaseConditionsManager.getInstance();
        try {
			conditionsManager.setDetector("HPS-Proposal2014-v7-2pt2", 0);
		} catch (ConditionsNotFoundException e) {
			throw new RuntimeException(e);
		}
    }

    public void test() {       
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        assertNotNull(conditions);
        System.out.println(conditions);
    }
}
