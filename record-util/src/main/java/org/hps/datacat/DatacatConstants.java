package org.hps.datacat;

/**
 * Constants for the HPS datacat.
 * 
 * @author Jeremy McCormick, SLAC
 */
final class DatacatConstants {

    /**
     * The root directory in the catalog for HPS folders.
     */
    public static final String ROOT_DIR = "HPS";
        
    /**
     * The base URL of the datacat server.
     */
    // FIXME: This needs to be more easily configurable and not hard-coded.
    public static final String BASE_URL = "http://localhost:8080/datacat-v0.4-SNAPSHOT";            
}
