package org.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConditionsTableMetaData;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a {@link SvtGainCollection} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtGainConverter extends DatabaseConditionsConverter<SvtGainCollection> {
    
    /**
     * Class constructor.
     */
    public SvtGainConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }

    /**
     * Get the SVT channel constants for this run by named set.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtGainCollection getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int collectionId = record.getFieldValue();
                
        // Objects for building the return value.
        ConditionsTableMetaData tableMetaData = _objectFactory.getTableRegistry().getTableMetaData(tableName);
        SvtGainCollection collection = 
                new SvtGainCollection(tableMetaData, collectionId, true); 
        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching calibration records using the ID field.
        String query = "SELECT id, svt_channel_id, gain, offset FROM "
                + tableName + " WHERE " + fieldName + " = " + collectionId
                + " ORDER BY svt_channel_id ASC";
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the gain records.            
            while(resultSet.next()) {         
                
                // Get the object parameters from the ResultSet.
                int rowId = resultSet.getInt(1);
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("svt_channel_id", resultSet.getInt(2));
                fieldValues.put("gain", resultSet.getDouble(3));
                fieldValues.put("offset", resultSet.getDouble(4));
                
                // Create the object using the factory.
                SvtGain newObject = _objectFactory.createObject(
                        SvtGain.class,
                        tableName,
                        rowId,
                        fieldValues,
                        true);
                
                // Add the object to the collection. 
                collection.add(newObject);
            }            
        } catch (SQLException x1) {
            throw new RuntimeException("Database error.", x1);
        } catch (ConditionsObjectException x2) {
            throw new RuntimeException("Error converting to SvtGain object.", x2);
        }
        
        // Return collection of gain objects to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converter.
     */
    public Class<SvtGainCollection> getType() {
        return SvtGainCollection.class;
    }        
}