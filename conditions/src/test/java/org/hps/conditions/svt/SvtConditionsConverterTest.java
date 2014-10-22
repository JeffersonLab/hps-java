package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.config.DevReadOnlyConfiguration;

/**
 * This test loads and prints {@link SvtConditions} from the dev database, 
 * which internally uses the {@link SvtConditionsConverter}. It does not 
 * perform any assertions.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsConverterTest extends TestCase {

    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {                
        new DevReadOnlyConfiguration().setup().load("HPS-Proposal2014-v7-2pt2", 2000);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }

    /**
     * Load and print all SVT conditions for a certain run number.
     */
    public void test() {        
        // Get conditions and print them out.
        SvtConditions svt = conditionsManager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svt);
        System.out.println(svt);
        System.out.println("Successfully loaded SVT conditions!");
    }
}
