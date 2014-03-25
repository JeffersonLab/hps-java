package org.hps.conditions.svt;

import junit.framework.TestCase;

import org.hps.conditions.DatabaseConditionsManager;
import org.hps.conditions.DefaultTestSetup;

/**
 * This test loads and prints {@link SvtConditions}, which internally uses the  
 * {@link SvtConditionsConverter}.  It does not perform any assertions.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtConditionsConverterTest extends TestCase {
    
    
    public void setUp() {
        new DefaultTestSetup().configure().setup();
    }
    
    /**
     * Load and print all SVT conditions for a certain run number.
     */
    public void test() {
        
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
                
        // Get conditions and print them out.
        SvtConditions svt = conditionsManager.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        assertNotNull(svt);
        System.out.println(svt);
        System.out.println("Successfully loaded SVT conditions!");
    }
}
