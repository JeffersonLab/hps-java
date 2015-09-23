package org.hps.run.database;

import java.util.List;

import org.hps.record.scalers.ScalerData;

/**
 * Database Access Object (DAO) for scaler data in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
interface ScalerDataDao {

    /**
     * Delete scaler data for the run.
     *
     * @param run the run number
     */
    void deleteScalerData(int run);

    /**
     * Get scaler data for a run.
     *
     * @param run the run number
     * @return the scaler data for the run
     */
    List<ScalerData> getScalerData(int run);

    /**
     * Insert scaler data for a run.
     *
     * @param scalerData the list of scaler data
     * @param run the run number
     */
    void insertScalerData(List<ScalerData> scalerData, int run);
}
