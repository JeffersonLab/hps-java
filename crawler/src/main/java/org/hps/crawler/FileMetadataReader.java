package org.hps.crawler;

import java.io.File;
import java.io.IOException;
import java.util.Map;


public interface FileMetadataReader {   
    
    public Map<String, Object> getMetadata(File file) throws IOException;
}
