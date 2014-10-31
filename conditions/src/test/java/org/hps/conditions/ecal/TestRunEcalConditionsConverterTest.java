package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.TestRunReadOnlyConfiguration;

/**
 * Tests that {@link EcalConditionsConverter} load test run conditions to 
 * an {@link EcalConditions} without errors.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class TestRunEcalConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
	
	public void setUp() {
        new TestRunReadOnlyConfiguration().setup().load("HPS-TestRun-v5", 1351);
        conditionsManager = DatabaseConditionsManager.getInstance();
	}
	
    public void test() {       
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        assertNotNull(conditions);
        System.out.println(conditions);
    }
	
}
