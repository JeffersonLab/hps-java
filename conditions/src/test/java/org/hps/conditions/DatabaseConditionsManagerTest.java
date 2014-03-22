package org.hps.conditions;

import junit.framework.TestCase;

public class DatabaseConditionsManagerTest extends TestCase {
    
    String detectorName = "HPS-conditions-test";
    int runNumber = 1351;
    DatabaseConditionsManager conditionsManager;
    
    public void setUp() {
        // Create and configure the conditions manager.
        conditionsManager = DatabaseConditionsManager.createInstance();
        conditionsManager.configure("/org/hps/conditions/config/conditions_database_testrun_2012.xml");
        conditionsManager.setDetectorName(detectorName);
        conditionsManager.setRunNumber(runNumber);
        conditionsManager.setup();
    }
    
    @SuppressWarnings("rawtypes")
    public void testLoad() {                       
        // Load data from every table registered with the manager.
        for (TableMetaData metaData : conditionsManager.getTableMetaDataList()) {
            System.out.println(">>>> loading conditions from table: " + metaData.getTableName());
            ConditionsObjectCollection conditionsObjects = 
                    conditionsManager.getConditionsData(metaData.getCollectionClass(), metaData.getTableName());
            System.out.println("  " + conditionsObjects.getObjects().size() + " " + conditionsObjects.get(0).getClass().getSimpleName() + " objects were created.");
        }
    }        
}
