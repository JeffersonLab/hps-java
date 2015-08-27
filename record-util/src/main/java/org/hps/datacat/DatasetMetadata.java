package org.hps.datacat;


/**
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface DatasetMetadata {
        
    float getFloat(String key);
    
    int getInt(String key);
    
    String getString(String key);
    
    boolean hasKey(String key);
}
