package org.hps.conditions.ecal;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConditionsRecordCollection;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates a {@link EcalBadChannelCollection} representing bad readout channels.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalBadChannelConverter extends DatabaseConditionsConverter<EcalBadChannelCollection> {

    public EcalBadChannelConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create the collection from the conditions database. 
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public EcalBadChannelCollection getData(ConditionsManager manager, String name) {

        // Collection to be returned to caller.
        EcalBadChannelCollection collection = new EcalBadChannelCollection();

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run number from the manager.
        ConditionsRecordCollection records = ConditionsRecord.find(manager, name);

        // Loop over ConditionsRecords.  For this particular type of condition, multiple
        // sets of bad channels are possible.
        for (ConditionsRecord record : records) {
        
            // Get the table name, field name, and field value defining the
            // applicable conditions.
            String tableName = record.getTableName();
            String fieldName = record.getFieldName();
            int fieldValue = record.getFieldValue();

            // Query for getting back bad channel records.
            String query = "SELECT id, ecal_channel_id FROM " + tableName + " WHERE " 
                    + fieldName + " = " + fieldValue + " ORDER BY id ASC";
            ResultSet resultSet = ConnectionManager.getConnectionManager().query(query);
            
            // Loop over the records.
            try {
                while (resultSet.next()) {
                    
                    int rowId = resultSet.getInt(1);
                                        
                    FieldValueMap fieldValues = new FieldValueMap();
                    fieldValues.put("ecal_channel_id", resultSet.getInt(2));
                    
                    EcalBadChannel newObject = _objectFactory.createObject(EcalBadChannel.class, tableName, rowId, fieldValues, true);
                    
                    collection.add(newObject);
                }
            } catch (SQLException x) {
                throw new RuntimeException(x);
            } catch (ConditionsObjectException x) {
                throw new RuntimeException("Error converting to " + getType().getSimpleName() + " type.");
            }
        }
               
        return collection;
    }

    /**
     * Get the type handled by this converter.
     * 
     * @return The type handled by this converter.
     */
    public Class<EcalBadChannelCollection> getType() {
        return EcalBadChannelCollection.class;
    }
}
