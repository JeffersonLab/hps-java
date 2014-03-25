package org.hps.conditions;

import junit.framework.TestCase;

public class DatabaseConditionsManagerTest extends TestCase {
    
    DatabaseConditionsManager _conditionsManager;    
    
    public void setUp() {
        _conditionsManager = new DefaultTestSetup().configure().setup();
    }
    
    @SuppressWarnings("rawtypes")
    public void testLoad() {     
        
        // Load data from every table registered with the manager.
        for (TableMetaData metaData : _conditionsManager.getTableMetaDataList()) {
            System.out.println(">>>> loading conditions from table: " + metaData.getTableName());
            ConditionsObjectCollection conditionsObjects = 
                    _conditionsManager.getConditionsData(metaData.getCollectionClass(), metaData.getTableName());
            System.out.println("  " + conditionsObjects.getObjects().size() + " " + conditionsObjects.get(0).getClass().getSimpleName() + " objects were created.");
        }
    }        
}
