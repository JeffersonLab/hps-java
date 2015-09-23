package org.hps.run.database;

import java.util.List;

import org.hps.record.epics.EpicsData;

/**
 * Database Access Object (DAO) API for EPICS data from the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
interface EpicsDataDao {

    /**
     * Delete all EPICS data for a run from the database.
     *
     * @param run the run number
     */
    public void deleteEpicsData(EpicsType epicsType, final int run);

    /**
     * Get EPICS data by run.
     *
     * @param run the run number
     * @param epicsType the type of EPICS data (1s or 10s)
     * @return the EPICS data
     */
    List<EpicsData> getEpicsData(EpicsType epicsType, int run);

    /**
     * Insert a list of EPICS data into the database.
     * <p>
     * The run number comes from the header information.
     *
     * @param epicsDataList the list of EPICS data
     */
    void insertEpicsData(List<EpicsData> epicsDataList);   
}
