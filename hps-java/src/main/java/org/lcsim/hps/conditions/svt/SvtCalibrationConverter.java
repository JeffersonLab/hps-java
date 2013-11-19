package org.lcsim.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.conditions.ConditionsRecord;
import org.lcsim.hps.conditions.ConnectionManager;
import org.lcsim.hps.conditions.DatabaseConditionsConverter;

/**
 * This class creates a {@link SvtCalibrationCollection} from the conditions
 * database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtCalibrationConverter extends DatabaseConditionsConverter<SvtCalibrationCollection> {

    /**
     * Class constructor.
     */
    public SvtCalibrationConverter() {
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
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();

        // Objects for building the return value.
        SvtCalibrationCollection collection = new SvtCalibrationCollection();

        // Get a connection from the manager.
        ConnectionManager connectionManager = getConnectionManager();

        // Construct the query to find matching calibration records using the ID
        // field.
        String query = "SELECT svt_channel_id, noise, pedestal FROM " 
                + tableName + " WHERE " 
                + fieldName + " = " + fieldValue + " ORDER BY svt_channel_id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over the calibration records.
            while (resultSet.next()) {

                // Get the calibration data for a single channel.
                int channelId = resultSet.getInt(1);
                double noise = resultSet.getDouble(2);
                double pedestal = resultSet.getDouble(3);

                collection.put(channelId, new SvtCalibration(noise, pedestal));
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
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