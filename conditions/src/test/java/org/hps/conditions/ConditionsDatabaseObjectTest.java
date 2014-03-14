package org.hps.conditions;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import junit.framework.TestCase;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;

/**
 * Test the basic functionality of a {@link ConditionsObject} on a dummy database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDatabaseObjectTest extends TestCase {
    
    String dummyTableName = "dummy_table";
    String dummyFieldName = "dummy_field1";
    float firstValue = 1.234f;
    float secondValue = 5.678f;
    
    public void testDummy() {
    
        // Connect to local test database.
        ConnectionManager connectionManager = new ConnectionManager();
        //ConnectionManager connectionManager = new DummyConnectionManager();
        connectionManager.setupFromProperties(new File("./src/main/config/dummy_db.properties"));
        
        // Setup table meta data information.
        Set<String> dummyFieldNames = new HashSet<String>();
        dummyFieldNames.add(dummyFieldName);               
        ConditionsTableMetaData tableMetaData = new ConditionsTableMetaData(dummyTableName, dummyFieldNames);
        
        // Create a dummy data object with a single field value.
        FieldValueMap fieldValues = new FieldValueMap();
        fieldValues.put(dummyFieldName, firstValue);
        ConditionsObject dummyObject = new DummyConditionsObject(connectionManager, tableMetaData, 1, fieldValues);        
         
        try {
            // Insert the object into the database.
            dummyObject.insert();
            int key = dummyObject.getRowId();

            // Set a new field value and push update to the database.
            dummyObject.setFieldValue(dummyFieldName, secondValue);
            dummyObject.update();
            
            // Load an object in read only mode.
            DummyConditionsObject readOnlyObject = new DummyConditionsObject(connectionManager, tableMetaData, key);            
            readOnlyObject.select();
            try {
                readOnlyObject.delete();
                throw new RuntimeException("Should not get here.");
            } catch (ConditionsObjectException x) {
                System.out.println("Caught error: " + x.getMessage());
            }

            // Delete the object from the database.
            dummyObject.delete();
            
            // Try to select a non-existant object to see that exception is thrown.
            try {
                dummyObject.select();
                throw new RuntimeException("Should not get here.");
            } catch (ConditionsObjectException x) {
                System.out.println("Caught error: " + x.getMessage());
            }
                                    
        } catch (Exception x) {
            throw new RuntimeException(x);
        }        
    }
    
    public static class DummyConditionsObject extends AbstractConditionsObject {
        
        // Create a new object.
        DummyConditionsObject(ConnectionManager connectionManager,
                ConditionsTableMetaData tableMetaData,
                int setId,
                FieldValueMap fieldValues) {       
            super(connectionManager, tableMetaData, setId, fieldValues);
        }
        
        // Load an existing object in read only mode.
        DummyConditionsObject(
                ConnectionManager connectionManager,
                ConditionsTableMetaData tableMetaData,
                int rowId) {
            super(connectionManager, tableMetaData, rowId, true);
        }
        
    }
    
    /*
    public class DummyConnectionManager extends ConnectionManager {
        
        public ResultSet query(String query) {
            System.out.println("Dummy query method ...");
            System.out.println(query);            
            return null;
        }
        
        public int update(String query) {
            System.out.println("Dummy update method ...");
            System.out.println(query);            
            return 1;
        }
    }
    */
}
