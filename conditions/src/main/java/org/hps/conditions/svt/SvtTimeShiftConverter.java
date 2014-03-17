package org.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a {@link SvtGainCollection} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShiftConverter extends DatabaseConditionsConverter<SvtTimeShiftCollection> {
    
    public SvtTimeShiftConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Get the SVT channel constants for this run by named set.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtTimeShiftCollection getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int collectionId = record.getFieldValue();
                
        // Collection that will be returned. 
        SvtTimeShiftCollection collection = 
                new SvtTimeShiftCollection(_objectFactory.getTableRegistry().getTableMetaData(tableName), 
                        collectionId, true);
        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching records.
        String query = "SELECT id, fpga, hybrid, time_shift FROM "
                + tableName + " WHERE " + fieldName + " = " + collectionId;
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the records.            
            while(resultSet.next()) {                                 
                int rowId = resultSet.getInt(1);

                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("fpga", resultSet.getInt(2));
                fieldValues.put("hybrid", resultSet.getInt(3));
                fieldValues.put("time_shift", resultSet.getDouble(4));
                
                SvtTimeShift newObject = _objectFactory.createObject(SvtTimeShift.class, tableName, rowId, fieldValues, true);
                
                collection.add(newObject);
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error creating SvtTimeShift object.", x);
        }
        
        // Return collection to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converte.
     */
    public Class<SvtTimeShiftCollection> getType() {
        return SvtTimeShiftCollection.class;
    }        
}