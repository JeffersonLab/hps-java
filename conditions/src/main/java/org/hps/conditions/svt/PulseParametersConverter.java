package org.hps.conditions.svt;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;

/**
 * This class creates a {@link PulseParametersCollection} object from the
 * conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PulseParametersConverter extends DatabaseConditionsConverter<PulseParametersCollection> {

    /**
     * Get the pulse parameters by channel for this run by named conditions set.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public PulseParametersCollection getData(ConditionsManager manager, String name) {

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.s
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();

        // Object for building the return value.
        PulseParametersCollection collection = new PulseParametersCollection();

        // Connection objects.
        ConnectionManager connectionManager = getConnectionManager();

        // Get the name of the current database being used.
        String database = connectionManager.getConnectionParameters().getDatabase();

        // Construct the query to find matching calibration records.
        String query = "SELECT svt_channel_id, amplitude, t0, tp, chisq FROM " 
                + tableName + " WHERE " 
                + fieldName + " = " + fieldValue + " ORDER BY id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
        
        try {
            // Loop over the calibration records.
            while (resultSet.next()) {
                // Create calibration object from database record.
                int channelId = resultSet.getInt(1);
                double amplitude = resultSet.getDouble(2);
                double t0 = resultSet.getDouble(3);
                double tp = resultSet.getDouble(4);
                double chisq = resultSet.getDouble(5);
                collection.put(channelId, new PulseParameters(amplitude, t0, tp, chisq));
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        }
        
        // Return the collection of channel constants to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter, which is <code>ConditionsRecordCollection</code>.
     */
    public Class<PulseParametersCollection> getType() {
        return PulseParametersCollection.class;
    }
}
