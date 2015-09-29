package org.hps.datacat.client;

/**
 * Dataset metadata which is string keys and values that are double, integer or string.
 * 
 * @author Jeremy McCormick, SLAC
 */
public interface DatasetMetadata {
    
    /**
     * Get a double value.
     * 
     * @param key the key name
     * @return the double value
     */
    Double getDouble(String key);
       
    /**
     * Get a long value. 
     * 
     * @param key the key name
     * @return the long value
     */
    Long getLong(String key);
          
    /**
     * Get a string value.
     * 
     * @param key the key name
     * @return the 
     */
    String getString(String key);
    
    /**
     * Return <code>true</code> if key exists in metadata.
     * 
     * @param key the key name
     * @return <code>true</code> if key exists in metadata
     */
    boolean hasKey(String key);
}
