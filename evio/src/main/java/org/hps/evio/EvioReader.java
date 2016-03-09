package org.hps.evio;

import org.jlab.coda.jevio.EvioEvent;

import org.lcsim.event.EventHeader;

/**
 *  Abstract class containing shared methods used by EVIO readers.
 *
 *  @author Sho Uemura <meeg@slac.stanford.edu>
 */
public abstract class EvioReader {

    // Debug flag
    protected boolean debug = false;
    
    // Name of the hit collection that will be created
    protected String hitCollectionName = null;

    /**
     *  Make a LCIO hit collection (e.g. {@link RawTrackerHit}, 
     *  {@link CalorimeterHit} from raw EVIO data.
     * 
     *  @param event : The EVIO event to read the raw data from
     *  @param lcsimEvent : The LCSim event to write the collections to
     *  @return True if the appropriate EVIO bank is found, false otherwise 
     * @throws Exception 
     * 
     */
    abstract boolean makeHits(EvioEvent event, EventHeader lcsimEvent) throws Exception;

    /**
     *  Set the hit collection name.
     * 
     *  @param hitCollectionName : Name of the hit collection
     */
    public void setHitCollectionName(String hitCollectionName) {
        this.hitCollectionName = hitCollectionName;
    }

    /**
     *  Enable/disable debug output.
     * 
     *  @param debug : Set to true to enable, false to disable.
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
}
