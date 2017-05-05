package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;

/**
 * Create combined SVT conditions for Test Run.
 *
 */
public final class TestRunSvtConditionsConverterTest extends TestCase {

    /**
     * The run number to use for the test.
     */
    private static final int RUN_NUMBER = 1351;

    /**
     * Create combined SVT Test Run conditions.
     * 
     * @throws Exception if there is a conditions system error
     */
    public void test() throws Exception {
        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        conditionsManager.setXmlConfig("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetector("HPS-TestRun-v5", RUN_NUMBER);

        final TestRunSvtConditions svtConditions = conditionsManager.getCachedConditions(TestRunSvtConditions.class,
                "svt_conditions").getCachedData();
        assertNotNull(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Printing test run SVT conditions.");
        System.out.println(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Succussfully loaded test run SVT conditions");
    }

}
