package org.hps.conditions.svt;

import static org.hps.conditions.ConditionsTableConstants.SVT_CHANNELS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class converts a table of SVT channel setup data into an {@link SvtChannelCollection}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// FIXME: This converter and the associated classes and tables need to use the collection ID
//        concept so that multiple channel maps are supported.
public class SvtChannelCollectionConverter extends DatabaseConditionsConverter<SvtChannelCollection> {

    public SvtChannelCollectionConverter(ConditionsObjectFactory objectFactory) {
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

        // Assign default key name for channel data if none given.
        if (name == null)
            name = SVT_CHANNELS;
        
        // Objects for building the return value.
        SvtChannelCollection channels = new SvtChannelCollection(
                _objectFactory.getTableRegistry().getTableMetaData(name),
                -1,
                true);
        
        // Construct the query to get the channel data.
        String query = "SELECT id, fpga, hybrid, channel FROM " + name;

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over records.
            while (resultSet.next()) {
                // Add SVT channel data for this record.
                //int id = resultSet.getInt(1);
                //int fpga = resultSet.getInt(2);
                //int hybrid = resultSet.getInt(3);
                //int channel = resultSet.getInt(4);
                //SvtChannel data = new SvtChannel(id, fpga, hybrid, channel);
                //channels.put(data.getId(), data);
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("id", resultSet.getInt(1));
                fieldValues.put("fpga", resultSet.getInt(2));
                fieldValues.put("hybrid", resultSet.getInt(3));
                fieldValues.put("channel", resultSet.getInt(4));
                
                SvtChannel newObject = _objectFactory.createObject(
                        SvtChannel.class, name, resultSet.getInt(1), fieldValues, true);
                
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
