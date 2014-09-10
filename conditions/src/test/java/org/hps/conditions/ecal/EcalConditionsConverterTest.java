package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.TestRunReadOnlyConfiguration;

/**
 * Tests that a {@link EcalConditions} objects loads without errors.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {
        new TestRunReadOnlyConfiguration(true);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }

    public void test() {       
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        assertNotNull(conditions);
        System.out.println(conditions);
    }
}
