package org.hps.run.database;

import org.hps.record.triggerbank.TriggerConfigData;

/**
 * Database interface for getting raw trigger config data and inserting into run db.
 * 
 * @author Jeremy McCormick, SLAC
 */
interface TriggerConfigDao {
    
    /**
     * Get a trigger config by run number.
     * 
     * @param run the run number
     * @return the trigger config
     */
    TriggerConfigData getTriggerConfig(int run);
    
    /**
     * Insert a trigger config.
     * 
     * @param config the trigger config
     * @param run the run number
     */
    void insertTriggerConfig(TriggerConfigData config, int run);
            
    /**
     * Delete a trigger config by run.
     * 
     * @param run the run number
     */
    void deleteTriggerConfig(int run);
}
