package org.srs.datacat.server;

import java.io.File;
import java.util.Map;

import junit.framework.TestCase;


public class EvioContentCheckerTest extends TestCase {
    
    static String filePath = "/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0";
    
    public void testEvioContentChecker() throws Exception {        
        ContentChecker checker = new EvioContentCheckerCreator().create();
        checker.setLocation(-1, new File(filePath).toURI().toURL());
        System.out.println("event count: " + checker.getEventCount());
        System.out.println("run max: " + checker.getRunMax());
        System.out.println("run min: " + checker.getRunMin());
        System.out.println("status: " + checker.getStatus());        
        System.out.println("meta data ...");
        Map<String, Object> metaData = checker.getMetaData();
        for (String key : metaData.keySet()) {
            System.out.println("  " + key + "=" + metaData.get(key));
        }
    }
}
