package org.hps.rundb;

import java.io.File;
import java.util.List;

/**
 * Database Access Object (DAO) interface to EVIO files in the run database.
 *
 * @author Jeremy McCormick, SLAC
 */
public interface EvioFilesDao {

    /**
     * Delete the EVIO file records for a run.
     *
     * @param run the run number
     */
    void deleteEvioFiles(int run);

    /**
     * Get all EVIO files from the database.
     *
     * @return all EVIO files from the database
     */
    List<File> getAllEvioFiles();

    /**
     * Get a list of EVIO files by run number.
     *
     * @param run the run number
     * @return the list of EVIO files for the run
     */
    List<File> getEvioFiles(int run);

    /**
     * Insert the list of files for a run.
     *
     * @param fileList the list of files
     * @param run the run number
     */
    void insertEvioFiles(List<File> fileList, int run);

}
