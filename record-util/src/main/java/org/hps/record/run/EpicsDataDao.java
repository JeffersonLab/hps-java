package org.hps.record.run;

import java.util.List;

import org.hps.record.epics.EpicsData;

/**
 * Database Access Object (DAO) API for EPICS data from the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public interface EpicsDataDao {

    /**
     * Delete EPICS data from the database.
     *
     * @param epicsData the EPICS data to delete
     */
    void deleteEpicsData(EpicsData epicsData);

    /**
     * Delete all EPICS data for a run from the database.
     *
     * @param run the run number
     */
    void deleteEpicsData(int run);

    /**
     * Get all the EPICS data in the database.
     *
     * @return the list of EPICS data
     */
    List<EpicsData> getAllEpicsData();

    /**
     * Get EPICS data by run.
     *
     * @param run the run number
     * @return the EPICS data
     */
    List<EpicsData> getEpicsData(int run);

    /**
     * Get the list of unique variables names used in the database records.
     *
     * @return the list of unique variable names
     */
    List<String> getVariableNames();

    /**
     * Insert a list of EPICS data into the database.
     * <p>
     * The run number comes from the header information.
     *
     * @param epicsDataList the list of EPICS data
     */
    void insertEpicsData(List<EpicsData> epicsDataList);

    /**
     * Updates EPICS data in the database.
     *
     * @param epicsData the EPICS data to update
     */
    void updateEpicsData(EpicsData epicsData);
}
