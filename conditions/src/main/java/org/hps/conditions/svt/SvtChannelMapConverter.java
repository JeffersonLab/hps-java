package org.hps.conditions.svt;

import static org.hps.conditions.ConditionsConstants.SVT_CHANNELS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;

/**
 * This class converts a table of SVT channel setup data into an {@link SvtChannelMap}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtChannelMapConverter extends DatabaseConditionsConverter<SvtChannelMap> {

    /**
     * Create the channel map from the conditions database.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     */
    public SvtChannelMap getData(ConditionsManager manager, String name) {

        // Objects for building the return value.
        SvtChannelMap channels = new SvtChannelMap();

        // Get the connection manager.
        ConnectionManager connectionManager = getConnectionManager();

        // Assign default key name for channel data if none given.
        if (name == null)
            name = SVT_CHANNELS;

        // Construct the query to get the channel data.
        String query = "SELECT id, fpga, hybrid, channel FROM " + name;

        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);

        try {
            // Loop over records.
            while (resultSet.next()) {
                // Add SVT channel data for this record.
                int id = resultSet.getInt(1);
                int fpga = resultSet.getInt(2);
                int hybrid = resultSet.getInt(3);
                int channel = resultSet.getInt(4);
                SvtChannel data = new SvtChannel(id, fpga, hybrid, channel);
                channels.put(data.getId(), data);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } 
        return channels;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtChannelMap> getType() {
        return SvtChannelMap.class;
    }
}
