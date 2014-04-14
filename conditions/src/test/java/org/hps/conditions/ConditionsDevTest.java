package org.hps.conditions;

import java.util.List;

import org.lcsim.conditions.ConditionsManager.ConditionsNotFoundException;

import junit.framework.TestCase;

public class ConditionsDevTest extends TestCase {
    
    public void testConditionsDev() {

        DatabaseConditionsManager manager = new DatabaseConditionsManager();               
        manager.configure("/org/hps/conditions/config/conditions_dev.xml");
        manager.setConnectionResource("/org/hps/conditions/config/conditions_dev.properties");
        manager.register();
        try {
            manager.setDetector("HPS-TestRun-v5", 1351);
        } catch (ConditionsNotFoundException e) {
            throw new RuntimeException(e);
        }
        
        List<TableMetaData> tableMetaData = manager.getTableMetaDataList();
        for (TableMetaData metaData : tableMetaData) {
            System.out.println("getting conditions of type " + metaData.collectionClass.getCanonicalName() + " with key " + metaData.getKey() + " and table name " + metaData.getTableName());           
            manager.getConditionsData(metaData.collectionClass, metaData.getKey());
        }
    }

}
