package org.hps.datacat;

import java.util.Map;


interface MetaDataExtractor {
    
    /**
     * Extract meta data from a file.
     * @param location
     * @return
     */
    public Map<String,Object> getMetaData(String location);    
    
    /**
     * Get the extension handled by this class e.g. 'slcio', etc.
     * @return The extension handled by the class.
     */
    String handlesExtension();
}
