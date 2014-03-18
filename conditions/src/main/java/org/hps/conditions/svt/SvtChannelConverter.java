package org.hps.conditions.svt;

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
 * This class converts a table of SVT channel setup data into an {@link SvtChannelCollection}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: This needs to support different collectionIDs.
public class SvtChannelConverter extends DatabaseConditionsConverter<SvtChannelCollection> {

    public SvtChannelConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create the channel map from the conditions database.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     */
    public SvtChannelCollection getData(ConditionsManager manager, String name) {

        // Get the connection manager.
        ConnectionManager connectionManager = getConnectionManager();
        
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);        
        int collectionId = record.getCollectionId();
        
        // Objects for building the return value.
        SvtChannelCollection channels = new SvtChannelCollection(
                _objectFactory.getTableRegistry().getTableMetaData(name),
                collectionId,
                true);
        
        // Construct the query to get the channel data.
        String query = "SELECT id, channel_id, fpga, hybrid, channel FROM " + name
                + " WHERE collection_id = " + collectionId;

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over records.
            while (resultSet.next()) {
                int rowId = resultSet.getInt(1);
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("channel_id", resultSet.getInt(2));
                fieldValues.put("fpga", resultSet.getInt(3));
                fieldValues.put("hybrid", resultSet.getInt(4));
                fieldValues.put("channel", resultSet.getInt(5));                
                SvtChannel newObject = _objectFactory.createObject(SvtChannel.class, name, rowId, fieldValues, true);                
                channels.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to SvtChannel object.", x);
        }
        return channels;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtChannelCollection> getType() {
        return SvtChannelCollection.class;
    }
}
