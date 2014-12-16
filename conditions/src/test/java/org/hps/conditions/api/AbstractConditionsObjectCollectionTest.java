package org.hps.conditions.api;

import java.sql.SQLException;

import junit.framework.TestCase;

import org.hps.conditions.database.ConnectionParameters;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

public class AbstractConditionsObjectCollectionTest extends TestCase {
    
    public void testAbstractConditionsObjectCollection() throws Exception {
        
        ConnectionParameters connection = 
                new ConnectionParameters("hpscalibrations", "heavyphotonsearch", "hps_conditions_dev", "ppa-jeremym-l.slac.stanford.edu", ConnectionParameters.DEFAULT_PORT);
        DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();
        TableMetaData tableMetaData = new TableMetaData("dummy_conditions", "dummy_conditions", DummyObject.class, DummyCollection.class);
        conditionsManager.setConnectionParameters(connection);
        conditionsManager.setDetector(DatabaseConditionsManager.getDefaultEngRunDetectorName(), 2000);
        conditionsManager.addTableMetaData(tableMetaData);
        
        DummyCollection collection = new DummyCollection();
        DummyObject[] objects = new DummyObject[] {
                new DummyObject(24),
                new DummyObject(42),
                new DummyObject(64)
        };
        collection.add(objects[0]);
        collection.add(objects[1]);
        collection.add(objects[2]);
      
        assertEquals("Wrong object from index.", objects[0], collection.get(0));
        assertEquals("Wrong object from index.", objects[1], collection.get(1));
        assertEquals("Wrong object from index.", objects[2], collection.get(2));
        
        // Test inserting the collection into the database.
        try {
            collection.insert();
        } catch (ConditionsObjectException | SQLException e) {
            throw new RuntimeException("Error inserting object into database.", e);
        }
                                
        // TODO: test select into the collection
        collection.setCollectionId(1);
        collection.select();
        
        // TODO: test update records from the collection to db
        
        // TODO: test delete records in the db by previously assigned collection ID
    }
    
    static class DummyObject extends AbstractConditionsObject {
                
        DummyObject(int dummyValue) {
            this.setFieldValue("dummy_value", dummyValue);
        }
        
        public void getDummyValue() {
            this.getFieldValue("dummy_value");
        }        
    }
    
    static class DummyCollection extends AbstractConditionsObjectCollection<DummyObject> {        
    }    
}
