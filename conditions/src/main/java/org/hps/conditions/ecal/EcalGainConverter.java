package org.hps.conditions.ecal;

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
 * This class creates an {@link EcalGainCollection} from the appropriate
 * conditions database information.
 */
public class EcalGainConverter extends DatabaseConditionsConverter<EcalGainCollection> {

    
    public EcalGainConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create the collection from the conditions database.
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public EcalGainCollection getData(ConditionsManager manager, String name) {

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int collectionId = record.getFieldValue();
        
        // Objects for building the return value.
        ConditionsTableMetaData tableMetaData = _objectFactory.getTableRegistry().getTableMetaData(tableName);
        EcalGainCollection collection = 
                new EcalGainCollection(tableMetaData, collectionId, true); 

        // References to database objects.
        ConnectionManager connectionManager = getConnectionManager();

        // Database query on ecal gain table.
        String query = "SELECT id, ecal_channel_id, gain FROM " 
                + tableName + " WHERE " 
                + fieldName + " = " + collectionId + " ORDER BY id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {                
                int rowId = resultSet.getInt(1);                 
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("ecal_channel_id", resultSet.getInt(2));
                fieldValues.put("gain", resultSet.getDouble(3));
                EcalGain newObject = _objectFactory.createObject(EcalGain.class, tableName, rowId, fieldValues, true);                
                collection.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to " + getType().getSimpleName() + " type.", x);
        }
        
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalGainCollection> getType() {
        return EcalGainCollection.class;
    }

}
