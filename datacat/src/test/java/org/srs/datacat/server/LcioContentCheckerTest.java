package org.srs.datacat.server;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Map;

import org.lcsim.util.cache.FileCache;

public class LcioContentCheckerTest {
        
    private static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps/conditions_test.slcio";
    
    public void testLcioContentChecker() throws IOException {
        
        FileCache cache = new FileCache();
        File file = cache.getCachedFile(new URL(TEST_FILE_URL));
        
        ContentChecker checker = new LcioContentCheckerCreator().create();
        checker.setLocation(-1, file.toURI().toURL());
        System.out.println("event count: " + checker.getEventCount());
        System.out.println("run max: " + checker.getRunMax());
        System.out.println("run min: " + checker.getRunMin());
        System.out.println("status: " + checker.getStatus());        
        System.out.println("meta data ...");
        Map<String, Object> metaData = checker.getMetaData();
        for (String key : metaData.keySet()) {
            System.out.println("  " + key + ": " + metaData.get(key));
        }
    }

}
