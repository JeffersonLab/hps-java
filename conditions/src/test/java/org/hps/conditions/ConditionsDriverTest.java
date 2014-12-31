package org.hps.conditions;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This is a basic test of using ConditionsDriver that doesn't actually check anything at the moment.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsDriverTest extends TestCase {
           
    public void testConditionsDriverTestRun() throws Exception {
        
        FileCache cache = new FileCache();
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/ConditionsTest.slcio"));
                    
        ConditionsDriver conditionsDriver = new ConditionsDriver();
        conditionsDriver.setDetectorName("HPS-TestRun-v5");
        conditionsDriver.setEcalName("Ecal");
        conditionsDriver.setSvtName("Tracker");        
        conditionsDriver.setTag("test_run");
        conditionsDriver.setRunNumber(1351);
        conditionsDriver.setFreeze(true);
        
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(inputFile);
        conditionsDriver.initialize();
        loop.add(new EventMarkerDriver());
        loop.add(new CheckDriver());
        loop.loop(-1);
    }
    
    static class CheckDriver extends Driver {

        public void detectorChanged(Detector detector) {
            System.out.println("detectorChanged - detector " + detector.getDetectorName() 
                    + " and run #" + DatabaseConditionsManager.getInstance().getRun());
        }        
    }
}
