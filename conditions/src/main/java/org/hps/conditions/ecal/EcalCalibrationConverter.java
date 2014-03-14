package org.hps.conditions.ecal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a list of {@link EcalCalibrationCollection} from the
 * conditions database.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibrationConverter extends DatabaseConditionsConverter<EcalCalibrationCollection> {
    
    public EcalCalibrationConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create the calibration collection from the conditions database.
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public EcalCalibrationCollection getData(ConditionsManager manager, String name) {

        // Collection to be returned to caller.
        EcalCalibrationCollection calibrations = new EcalCalibrationCollection();

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();

        // References to database objects.
        ResultSet resultSet = null;
        ConnectionManager connectionManager = getConnectionManager();

        // The query to get conditions.
        String query = "SELECT ecal_channel_id, pedestal, noise FROM " 
                + tableName + " WHERE " 
                + fieldName + " = " + fieldValue + " ORDER BY ecal_channel_id ASC";

        // Execute the query.
        resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {
                // Create calibration object from record.
                int channelId = resultSet.getInt(1);
                double pedestal = resultSet.getDouble(2);
                double noise = resultSet.getDouble(3);
                calibrations.put(channelId, new EcalCalibration(pedestal, noise));
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } 
        
        return calibrations;
    }

    /**
     * Get the type handled by this converter.
     * 
     * @return The type handled by this converter.
     */
    public Class<EcalCalibrationCollection> getType() {
        return EcalCalibrationCollection.class;
    }
}
