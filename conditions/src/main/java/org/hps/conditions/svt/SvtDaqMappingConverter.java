package org.hps.conditions.svt;

import static org.hps.conditions.ConditionsTableConstants.SVT_DAQ_MAP;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a {@link SvtDaqMappingCollection} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtDaqMappingConverter extends DatabaseConditionsConverter<SvtDaqMappingCollection> {

    public SvtDaqMappingConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create an {@link SvtDaqMappingCollection} object from the database.
     */
    public SvtDaqMappingCollection getData(ConditionsManager manager, String name) {
        
        // Use default key name if not set.
        if (name == null) {
            name = SVT_DAQ_MAP;
        }
                
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        int collectionId = record.getCollectionId();
        
        // The object to be returned to caller.
        SvtDaqMappingCollection collection = 
               new SvtDaqMappingCollection(this.getTableMetaData(tableName), collectionId, true); 
                        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching calibration records using the ID field.
        String query = "SELECT id, half, layer, hybrid, fpga FROM " + tableName 
                + " WHERE collection_id = " + collectionId
                + " ORDER BY half ASC, layer ASC";
                   
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the database records.
            while(resultSet.next()) {                          
                int rowId = resultSet.getInt(1);                
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("half", resultSet.getInt(2));
                fieldValues.put("layer", resultSet.getInt(3));
                fieldValues.put("hybrid", resultSet.getInt(4));
                fieldValues.put("fpga", resultSet.getInt(5));
                SvtDaqMapping newObject = _objectFactory.createObject(
                        SvtDaqMapping.class, tableName, rowId, fieldValues, true);
                collection.add(newObject);
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error creating object of " + getType().getSimpleName() + " type.", x);
        }
        
        // Return DAQ map to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtDaqMappingCollection> getType() {
        return SvtDaqMappingCollection.class;
    }

}
