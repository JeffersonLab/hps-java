package org.hps.datacat;

import java.util.HashSet;
import java.util.Set;

/**
 * Static constants for use with the Data Catalog.
 */
public final class DatacatConstants {

    /*
     * Default base URL for datacat.
     */
    public static final String DATACAT_URL = "http://hpsweb.jlab.org/datacat/r";
    
    /*
     * Datacat folder where the EVIO files reside.
     */
    public static final String RAW_DATA_FOLDER = "/HPS/data/raw";
    
    /*
     * Datacat folder where the EVIO files reside.
     */
    public static final Site DEFAULT_SITE = Site.JLAB;
    
    /*
     * The set of system metadata which is always set for each file.
     */
    private static final Set<String> SYSTEM_METADATA = new HashSet<String>();
    static {
        SYSTEM_METADATA.add("eventCount");
        SYSTEM_METADATA.add("size");
        SYSTEM_METADATA.add("runMin");
        SYSTEM_METADATA.add("runMax");
        SYSTEM_METADATA.add("checksum");
        SYSTEM_METADATA.add("scanStatus");
    }
    
    /* metadata fields that should be included in search results */
    public static final String[] EVIO_METADATA = {
        "BAD_EVENTS",
        "BLINDED",
        "END_EVENT_COUNT",
        "END_TIMESTAMP",
        "FILE",
        "FIRST_HEAD_TIMESTAMP",
        "FIRST_PHYSICS_EVENT",
        "LAST_HEAD_TIMESTAMP",
        "LAST_PHYSICS_EVENT",
        "LED_COSMIC",
        "PAIRS0",
        "PAIRS1",
        "PHYSICS_EVENTS",
        "PULSER",
        "SINGLES0",
        "SINGLES1",
        "TI_TIME_MAX_OFFSET",
        "TI_TIME_MIN_OFFSET",
        "TI_TIME_N_OUTLIERS",
        "TRIGGER_RATE"
    };
    
    /**
     * Get the set of system metadata field names.
     * 
     * @return the set of system metadata field names
     */
    public static Set<String> getSystemMetadata() {
        return SYSTEM_METADATA;
    }
    
    /**
     * Return <code>true</code> if the metadata field is system metadata.
     * @param name the metadata field name
     * @return <code>true</code> if the metadata field is system metadata
     */
    public static final boolean isSystemMetadata(String name) {
        return SYSTEM_METADATA.contains(name);
    }
}
