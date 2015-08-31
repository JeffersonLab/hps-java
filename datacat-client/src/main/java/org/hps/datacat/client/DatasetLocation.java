package org.hps.datacat.client;

/**
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public interface DatasetLocation {
    
    DatasetSite getSite();
    
    String getResource();
    
    ScanStatus getScanStatus();
    
    long getSize();
    
    int getRunMin();
    
    int getRunMax();
    
    int getEventCount();
}
