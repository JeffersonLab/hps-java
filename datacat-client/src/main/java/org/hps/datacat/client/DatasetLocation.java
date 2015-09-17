package org.hps.datacat.client;

/**
 * Representation of a dataset location in the data catalog.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface DatasetLocation {
    
    /**
     * Get the site of the dataset (JLAB or SLAC).
     * 
     * @return the dataset site
     */
    DatasetSite getSite();
    
    /**
     * Get the resource of the dataset location (file system path).
     * 
     * @return the resource of the dataset location
     */
    String getResource();
    
    /**
     * Get the scan status of the dataset location.
     * 
     * @return the scan status
     */
    ScanStatus getScanStatus();
    
    /**
     * The size of the file in bytes.
     * 
     * @return the size of the file in bytes
     */
    long getSize();
    
    /**
     * Get the minimum run number.
     * 
     * @return the minimum run number
     */
    // FIXME: Belongs in dataset metadata.
    int getRunMin();
    
    /**
     * Get the maximum run number.
     * 
     * @return the maximum run number
     */
    // FIXME: Belongs in dataset metadata.
    int getRunMax();
    
    /**
     * Get the event count.
     * 
     * @return the event count
     */
    // FIXME: Belongs in dataset metadata.
    int getEventCount();
}
