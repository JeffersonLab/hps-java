package org.hps.datacat;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.srs.datacat.server.ContentChecker;
import org.srs.datacat.server.EvioContentChecker;

public class EvioMetaDataExtractor implements MetaDataExtractor {

    static String extension = "evio";
    ContentChecker contentChecker = new EvioContentChecker();
    
    public Map<String,Object> getMetaData(String location) {        
        File file = new File(location);
        try {
            contentChecker.setLocation(0, file.toURI().toURL());
            return contentChecker.getMetaData();
        } catch (IOException e) {
            throw new RuntimeException(e);
        } 
    }

    public String handlesExtension() {
        return extension;
    }
}
