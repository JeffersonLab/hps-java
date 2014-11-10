package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;

public class TestRunSvtConditionsConverterTest extends TestCase {

    public void test() throws Exception {
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", 1351);

        TestRunSvtConditions svtConditions = conditionsManager.getCachedConditions(TestRunSvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Printing test run SVT conditions.");
        System.out.println(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Succussfully loaded test run SVT conditions");
    }

}
