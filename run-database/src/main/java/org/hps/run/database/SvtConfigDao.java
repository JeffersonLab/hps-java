package org.hps.run.database;

import java.util.List;

import org.hps.record.svt.SvtConfigData;

/**
 * Database API for accessing SVT configuration in run database.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface SvtConfigDao {
   
    /**
     * Insert SVT configurations.
     * 
     * @param svtConfigs the list of SVT configurations
     * @param run the run number
     */
    void insertSvtConfigs(List<SvtConfigData> svtConfigs, int run);
    
    /**
     * Get the list of SVT configurations for the run.
     * 
     * @param run the run number
     * @return the list of SVT configurations
     */
    List<SvtConfigData> getSvtConfigs(int run);
    
    /**
     * Delete SVT configurations for the run.
     * 
     * @param run the run number
     */
    void deleteSvtConfigs(int run);
}
