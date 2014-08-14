package org.hps.conditions.ecal;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.DefaultTestSetup;

/**
 * Tests that a {@link EcalConditions} objects loads without errors.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalConditionsConverterTest extends TestCase {

    public void setUp() {
        new DefaultTestSetup().configure().setup();
    }

    public void test() {

        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        // Test that the manager gets ECAL conditions.
        EcalConditions conditions = conditionsManager.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        assertNotNull(conditions);
        System.out.println(conditions);
    }
}
