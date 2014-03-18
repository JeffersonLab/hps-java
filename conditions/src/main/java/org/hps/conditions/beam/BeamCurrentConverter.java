package org.hps.conditions.beam;

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
 * This class creates a {@link BeamCurrent} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BeamCurrentConverter extends DatabaseConditionsConverter<BeamCurrentCollection> {
    
    public BeamCurrentConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Get the conditions data.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     */
    public BeamCurrentCollection getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int collectionId = record.getFieldValue();
        
        // Collection to be returned to caller.
        BeamCurrentCollection collection = new BeamCurrentCollection(getTableMetaData(name), collectionId, true);
                        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching records using the ID field.
        String query = "SELECT id, beam_current FROM "
                + tableName + " WHERE " + fieldName + " = " + collectionId;
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
                
        try {
            // Loop over the records.            
            while(resultSet.next()) {                                                             
                int rowId = resultSet.getInt(1);
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("beam_current", resultSet.getDouble(2));
                BeamCurrent newObject = _objectFactory.createObject(BeamCurrent.class, tableName, rowId, fieldValues, true);                
                collection.add(newObject);
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x){
            throw new RuntimeException("Error converting to " + getType().getSimpleName() + "type", x);
        }
        
        // Return collection of gain objects to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converter.
     */
    public Class<BeamCurrentCollection> getType() {
        return BeamCurrentCollection.class;
    }        
}
