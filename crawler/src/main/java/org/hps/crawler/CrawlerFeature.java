package org.hps.crawler;

/**
 * Enum for enabling or disabling features in the file crawler.
 */
enum CrawlerFeature {
    /**
     * Allow inserts into run database.
     */
    RUNDB_INSERT,
    /**
     * Allow updating the run database if the run exists already.
     */
    RUNDB_UPDATE,
    /**
     * Create list of EPICS data for inserting into run database.
     */
    EPICS,
    /**
     * Create list of scaler data for inserting into run database.
     */
    SCALERS,
    /**
     * Extract trigger config for inserting into run database.
     */
    TRIGGER,
    /**
     * Populate the data catalog with files that are found when crawling.
     */
    DATACAT    
}

