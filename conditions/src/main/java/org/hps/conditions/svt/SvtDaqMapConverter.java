package org.hps.conditions.svt;

import static org.hps.conditions.ConditionsTableConstants.SVT_DAQ_MAP;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.ConditionsObjectFactory;
import org.hps.conditions.ConditionsRecord;
import org.hps.conditions.ConnectionManager;
import org.hps.conditions.DatabaseConditionsConverter;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.util.Pair;

/**
 * This class creates a {@link SvtDaqMap} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtDaqMapConverter extends DatabaseConditionsConverter<SvtDaqMap> {

    public SvtDaqMapConverter(ConditionsObjectFactory objectFactory) {
        super(objectFactory);
    }
    
    /**
     * Create an {@link SvtDaqMap} object from the database.
     */
    public SvtDaqMap getData(ConditionsManager manager, String name) {
        
        // Use default key name if not set.
        if (name == null) {
            name = SVT_DAQ_MAP;
        }
        
        // The object to be returned to caller.
        SvtDaqMap daqMap = new SvtDaqMap();
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();
                        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching calibration records using the ID field.
        String query = "SELECT half, layer, hybrid, fpga FROM "
                + tableName + " WHERE " + fieldName + " = " + fieldValue
                + " ORDER BY half ASC, layer ASC";
                   
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the database records.
            while(resultSet.next()) {          
                
                // Get record data.
                int half = resultSet.getInt(1);
                int layer = resultSet.getInt(2);
                int hybrid = resultSet.getInt(3);
                int fpga = resultSet.getInt(4);
                                
                // Add data to DAQ map: half => layer => DAQ pair               
                daqMap.add(half, layer, new Pair<Integer,Integer>(fpga, hybrid));
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        } 
        
        // Return DAQ map to caller.
        return daqMap;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter.
     */
    public Class<SvtDaqMap> getType() {
        return SvtDaqMap.class;
    }

}
