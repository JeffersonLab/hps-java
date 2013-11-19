package org.lcsim.hps.conditions.svt;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;
import org.lcsim.hps.conditions.ConditionsRecord;
import org.lcsim.hps.conditions.ConnectionManager;
import org.lcsim.hps.conditions.DatabaseConditionsConverter;

/**
 * This class creates a {@link SvtGainCollection} from the conditions database.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SvtTimeShiftConverter extends DatabaseConditionsConverter<SvtTimeShiftCollection> {
    
    /**
     * Class constructor.
     */
    public SvtTimeShiftConverter() {
    }

    /**
     * Get the SVT channel constants for this run by named set.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The channel constants data.
     */
    public SvtTimeShiftCollection getData(ConditionsManager manager, String name) {
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        ConditionsRecord record = ConditionsRecord.find(manager, name).get(0);
               
        // Get the table name, field name, and field value defining the applicable conditions.
        String tableName = record.getTableName();
        String fieldName = record.getFieldName();
        int fieldValue = record.getFieldValue();
                
        // Collection that will be returned. 
        SvtTimeShiftCollection collection = new SvtTimeShiftCollection();
        
        // Get the connection manager.
        ConnectionManager connectionManager = ConnectionManager.getConnectionManager();
                                                                                            
        // Construct the query to find matching records.
        String query = "SELECT fpga, hybrid, time_shift FROM "
                + tableName + " WHERE " + fieldName + " = " + fieldValue;
            
        // Execute the query and get the results.
        ResultSet resultSet = connectionManager.query(query);
               
        try {
            // Loop over the records.            
            while(resultSet.next()) {                                 
                // Create the object with the sensor time shift.
                int fpga = resultSet.getInt(1);
                int hybrid = resultSet.getInt(2);
                double timeShift = resultSet.getDouble(3);
                collection.add(new SvtTimeShift(fpga, hybrid, timeShift));
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error.", x);
        }
        
        // Return collection to caller.
        return collection;
    }

    /**
     * Get the type handled by this converter.     
     * @return The type handled by this converte.
     */
    public Class<SvtTimeShiftCollection> getType() {
        return SvtTimeShiftCollection.class;
    }        
}