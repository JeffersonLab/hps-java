package org.hps.conditions.ecal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;

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

        // Collection to be returned to caller.
        EcalGainCollection gains = new EcalGainCollection();

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();

        // References to database objects.
        ConnectionManager connectionManager = getConnectionManager();

        // Get the name of the current database being used.
        String database = connectionManager.getConnectionParameters().getDatabase();

        // Database query on ecal gain table.
        String query = "SELECT ecal_channel_id, gain FROM " 
                + tableName + " WHERE " 
                + fieldName + " = " + fieldValue + " ORDER BY id ASC";

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {
                // Create gain object from database record.
                int channelId = resultSet.getInt(1);
                double gain = resultSet.getDouble(2);
                gains.put(channelId, new EcalGain(gain));
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } 
        
        return gains;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalGainCollection> getType() {
        return EcalGainCollection.class;
    }

}
