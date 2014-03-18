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
 * This class creates a {@link SvtPulseParametersCollection} object from the
 * conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtPulseParametersConverter extends DatabaseConditionsConverter<SvtPulseParametersCollection> {
    
    public SvtPulseParametersConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Get the pulse parameters by channel for this run by named conditions set.
     * 
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtPulseParametersCollection getData(ConditionsManager manager, String name) {

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        int collectionId = record.getCollectionId();

        // Object for building the return value.
        ConditionsTableMetaData tableMetaData = _objectFactory
                .getTableRegistry().getTableMetaData(tableName);
        SvtPulseParametersCollection collection = new SvtPulseParametersCollection(
                tableMetaData, collectionId, true);

        // Connection objects.
        ConnectionManager connectionManager = getConnectionManager();

        // Construct the query to find matching calibration records.
        String query = "SELECT id, svt_channel_id, amplitude, t0, tp, chisq FROM " + tableName
                + " WHERE collection_id = " + collectionId
                + " ORDER BY id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over the calibration records.
            while (resultSet.next()) {

                // Get row ID from the database.
                int rowId = resultSet.getInt(1);

                // Set the field values for the new object.
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("svt_channel_id", resultSet.getInt(2));
                fieldValues.put("amplitude", resultSet.getDouble(3));
                fieldValues.put("t0", resultSet.getDouble(4));
                fieldValues.put("tp", resultSet.getDouble(5));
                fieldValues.put("chisq", resultSet.getDouble(6));

                // Create the object using the factory.
                SvtPulseParameters newObject;
                newObject = _objectFactory.createObject(SvtPulseParameters.class,
                        tableName, rowId, fieldValues, true);

                // Add the object to the collection.
                collection.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to SvtPulseParameters object.", x);
        }

        // Return the collection of channel constants to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtPulseParametersCollection> getType() {
        return SvtPulseParametersCollection.class;
    }
}
