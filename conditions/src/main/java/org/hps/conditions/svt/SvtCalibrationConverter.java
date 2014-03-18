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
 * This class creates a {@link SvtCalibrationCollection} from the conditions
 * database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtCalibrationConverter extends DatabaseConditionsConverter<SvtCalibrationCollection> {

    /**
     * Class constructor.
     */
    public SvtCalibrationConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }

    /**
     * Get the SVT channel constants for this run by named set.     
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtCalibrationCollection getData(ConditionsManager manager, String name) {

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        int collectionId = record.getCollectionId();

        // Objects for building the return value.
        SvtCalibrationCollection collection = 
                new SvtCalibrationCollection(this.getTableMetaData(tableName), collectionId, true); 

        // Get a connection from the manager.
        ConnectionManager connectionManager = getConnectionManager();

        // Construct the query to find matching calibration records.
        String query = "SELECT id, svt_channel_id, noise, pedestal FROM " + tableName 
                + " WHERE collection_id = " + collectionId 
                + " ORDER BY svt_channel_id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over the calibration records.
            while (resultSet.next()) {
                
                int rowId = resultSet.getInt(1);
                
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("svt_channel_id", resultSet.getInt(2));
                fieldValues.put("noise", resultSet.getDouble(3));
                fieldValues.put("pedestal", resultSet.getDouble(4));
                SvtCalibration newObject = _objectFactory.createObject(
                        SvtCalibration.class, tableName, rowId, fieldValues, true);
                
                collection.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to SvtCalibration object");
        }
        
        // Return the collection of channel constants to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * 
     * @return The type handled by this converter, which is
     *         <code>ConditionsRecordCollection</code>.
     */
    public Class<SvtCalibrationCollection> getType() {
        return SvtCalibrationCollection.class;
    }
}