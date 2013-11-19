package org.lcsim.hps.conditions.beam;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.conditions.ConditionsRecord;
import org.lcsim.hps.conditions.ConnectionManager;
import org.lcsim.hps.conditions.DatabaseConditionsConverter;

/**
 * This class creates a {@link BeamCurrent} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BeamCurrentConverter extends DatabaseConditionsConverter<BeamCurrent> {
    
    /**
     * Class constructor.
     */
    public BeamCurrentConverter() {
    }

    /**
     * Get the conditions data.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     */
    public BeamCurrent getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();
                        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching records using the ID field.
        String query = "SELECT beam_current FROM "
                + tableName + " WHERE " + fieldName + " = " + fieldValue;
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
        
        // The object to be returned to caller.
        BeamCurrent beamCurrent = null;
        
        try {
            // Loop over the gain records.            
            while(resultSet.next()) {                              
                beamCurrent = new BeamCurrent(resultSet.getDouble(1));
                break;
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        }
        
        // Return collection of gain objects to caller.
        return beamCurrent;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converter.
     */
    public Class<BeamCurrent> getType() {
        return BeamCurrent.class;
    }        
}