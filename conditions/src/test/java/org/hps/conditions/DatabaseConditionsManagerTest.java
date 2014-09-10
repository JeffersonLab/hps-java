package org.hps.conditions;

import junit.framework.TestCase;

import org.hps.conditions.config.TestRunReadOnlyConfiguration;

public class DatabaseConditionsManagerTest extends TestCase {

    DatabaseConditionsManager conditionsManager;

    public void setUp() {
        new TestRunReadOnlyConfiguration(true);
        conditionsManager = DatabaseConditionsManager.getInstance();
    }

    @SuppressWarnings("rawtypes")
    public void testLoad() {

        // Load data from every table registered with the manager.
        for (TableMetaData metaData : conditionsManager.getTableMetaDataList()) {
            System.out.println(">>>> loading conditions from table: " + metaData.getKey());
            ConditionsObjectCollection conditionsObjects = conditionsManager.getConditionsData(metaData.getCollectionClass(), metaData.getKey());
            System.out.println("  " + conditionsObjects.getObjects().size() + " " + conditionsObjects.get(0).getClass().getSimpleName() + " objects were created.");
        }
    }
}
