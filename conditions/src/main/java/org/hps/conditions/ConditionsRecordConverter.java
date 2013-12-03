package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.lcsim.conditions.ConditionsManager;

/**
 * Read ConditionsRecord objects from the conditions database.  
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: ConditionsRecordConverter.java,v 1.5 2013/10/15 23:24:47 jeremy Exp $
 */
public class ConditionsRecordConverter extends DatabaseConditionsConverter<ConditionsRecordCollection> {
           
    /**
     * Class constructor.
     */
    public ConditionsRecordConverter() {
    }
        
    /**
     * Get the ConditionsRecords for a run.  This method ignores the name argument 
     * and will fetch all conditions records for the current run.
     * @param manager The current conditions manager.
     * @param name The name of the conditions set.
     * @return The matching ConditionsRecords.
     */
    public ConditionsRecordCollection getData(ConditionsManager manager, String name) {
                                
        ConditionsRecordCollection records = new ConditionsRecordCollection();
        
        ConnectionManager connectionManager = this.getConnectionManager();
        String tableName = connectionManager.getConnectionParameters().getConditionsTable();
        
        String query = "SELECT * from " 
                + tableName
                + " WHERE "
                + "run_start <= "
                + manager.getRun()
                + " AND run_end >= "
                + manager.getRun();
               
        ResultSet resultSet = connectionManager.query(query);
        
        try {
            while(resultSet.next()) {                  
                ConditionsRecord record = new ConditionsRecord();
                record.load(resultSet);
                records.add(record);
            }            
        } catch (SQLException x) {
            throw new RuntimeException("Database error", x);
        } 
        
        return records;
    }

    /**
     * Get the type handled by this converter.
     * @return The type handled by this converter, which is <code>ConditionsRecordCollection</code>.
     */
    public Class<ConditionsRecordCollection> getType() {
        return ConditionsRecordCollection.class;
    }        
}