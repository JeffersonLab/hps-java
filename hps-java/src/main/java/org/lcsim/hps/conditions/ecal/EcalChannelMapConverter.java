package org.lcsim.hps.conditions.ecal;

import static org.lcsim.hps.conditions.ConditionsConstants.ECAL_CHANNELS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.conditions.ConnectionManager;
import org.lcsim.hps.conditions.DatabaseConditionsConverter;

/**
 * This class creates the {@link EcalChannelMap} from the conditions table
 * containing the channel data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalChannelMapConverter extends DatabaseConditionsConverter<EcalChannelMap> {

    /**
     * Load the data from the conditions database.
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public EcalChannelMap getData(ConditionsManager manager, String name) {

        // Collection to be returned to caller.
        EcalChannelMap channels = new EcalChannelMap();

        // References to database objects.
        ResultSet resultSet = null;
        ConnectionManager connectionManager = getConnectionManager();

        // Assign default key name if none was given.
        if (name == null)
            name = ECAL_CHANNELS;

        // Query to retrieve channel data.
        String query = "SELECT id, x, y, crate, slot, channel FROM " + name;

        // Execute the query and get the results.
        resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {
                // Create channel data object from database record.
                int id = resultSet.getInt(1);
                int x = resultSet.getInt(2);
                int y = resultSet.getInt(3);
                int crate = resultSet.getInt(4);
                int slot = resultSet.getInt(5);
                int channel = resultSet.getInt(6);
                EcalChannel channelData = new EcalChannel(id, crate, slot, channel, x, y);
                channels.put(channelData.getId(), channelData);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } 
        
        return channels;
    }

    /**
     * Get the type that this converter handles.
     * @return The type handled by this converter.
     */
    public Class<EcalChannelMap> getType() {
        return EcalChannelMap.class;
    }
}
