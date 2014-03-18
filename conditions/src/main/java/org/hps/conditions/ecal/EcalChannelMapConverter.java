package org.hps.conditions.ecal;

import static org.hps.conditions.ConditionsTableConstants.ECAL_CHANNELS;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObjectException;
import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * This class creates the {@link EcalChannelMap} from the conditions table
 * containing the channel data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Needs to support different collectionIDs.
public class EcalChannelMapConverter extends DatabaseConditionsConverter<EcalChannelMap> {

    public EcalChannelMapConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Load the data from the conditions database.
     * @param manager The conditions manager.
     * @param name The name of the conditions set.
     */
    public EcalChannelMap getData(ConditionsManager manager, String name) {

        // References to database objects.
        ResultSet resultSet = null;
        ConnectionManager connectionManager = getConnectionManager();
        
        // Collection to be returned to caller.
        EcalChannelMap collection = new EcalChannelMap();

        // Assign default key name if none was given.
        String tableName = name;
        if (tableName == null)
            tableName = ECAL_CHANNELS;

        // Query to retrieve channel data.
        String query = "SELECT id, x, y, crate, slot, channel FROM " + name;

        // Execute the query and get the results.
        resultSet = connectionManager.query(query);

        try {
            // Loop over the records.
            while (resultSet.next()) {
                
                int rowId = resultSet.getInt(1);                                       
                FieldValueMap fieldValues = new FieldValueMap();
                fieldValues.put("x", resultSet.getInt(2));
                fieldValues.put("y", resultSet.getInt(3));
                fieldValues.put("crate", resultSet.getInt(4));
                fieldValues.put("slot", resultSet.getInt(5));
                fieldValues.put("channel", resultSet.getInt(6));                                
                EcalChannel newObject = _objectFactory.createObject(EcalChannel.class, tableName, rowId, fieldValues, true);                    
                collection.add(newObject);
            }
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } catch (ConditionsObjectException x) {
            throw new RuntimeException("Error converting to " + getType().getSimpleName() + " type.", x);
        }
        
        return collection;
    }

    /**
     * Get the type that this converter handles.
     * @return The type handled by this converter.
     */
    public Class<EcalChannelMap> getType() {
        return EcalChannelMap.class;
    }
}
