package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.DevReadOnlyConfiguration;

/**
 * Tests that a {@link EcalConditions} objects loads without errors.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {                
        new DevReadOnlyConfiguration().setup().load("HPS-Proposal2014-v7-2pt2", 0);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }

    public void test() {       
        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        assertNotNull(conditions);
        System.out.println(conditions);
    }
}
