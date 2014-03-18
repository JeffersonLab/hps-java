package org.hps.conditions.ecal;

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

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);

        // Get the table name, field name, and field value defining the
        // applicable conditions.
        String tableName = record.getTableName();
        int collectionId = record.getCollectionId();
        
        // Collection to be returned to caller.
        EcalCalibrationCollection collection = new EcalCalibrationCollection(getTableMetaData(name), collectionId, true);

        // References to database objects.
        ResultSet resultSet = null;
        ConnectionManager connectionManager = getConnectionManager();

        // The query to get conditions.
        String query = "SELECT id, ecal_channel_id, pedestal, noise FROM " 
                + tableName + " WHERE collection_id = " + collectionId 
                + " ORDER BY ecal_channel_id ASC";

        // Execute the query.
        resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {
                int rowId = resultSet.getInt(1);                 
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("ecal_channel_id", resultSet.getInt(2));
                fieldValues.put("pedestal", resultSet.getDouble(3));
                fieldValues.put("noise", resultSet.getDouble(4));
                EcalCalibration newObject = _objectFactory.createObject(EcalCalibration.class, tableName, rowId, fieldValues, true);                
                collection.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to " + getType().getSimpleName() + " object", x);
        }
        
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<EcalCalibrationCollection> getType() {
        return EcalCalibrationCollection.class;
    }
}
