package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.TestRunReadOnlyConfiguration;

public class TestRunSvtConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
   
	public void setUp(){
        new TestRunReadOnlyConfiguration().setup().load("HPS-TestRun-v5", 1351);
        conditionsManager = DatabaseConditionsManager.getInstance();
	}
	
	public void test(){
        TestRunSvtConditions svtConditions = conditionsManager.getCachedConditions(TestRunSvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Printing test run SVT conditions.");
        System.out.println(svtConditions);
        System.out.println("[ " + this.getClass().getSimpleName() + "]: Succussfully loaded test run SVT conditions");
	}
    
}
