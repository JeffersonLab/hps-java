package org.lcsim.hps.evio;

import java.io.File;
import java.net.URL;

import junit.framework.TestCase;

import org.jlab.coda.jevio.BaseStructure;
import org.jlab.coda.jevio.CompositeData;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioReader;
import org.jlab.coda.jevio.test.CompositeTester;
import org.lcsim.util.cache.FileCache;

public class DumpRyanEvioFile extends TestCase {
    
    public void testMe() throws Exception {
        
        //FileCache cache = new FileCache();
        //cache.setCacheDirectory(new File(".testdata"));
        //File evioTestData = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps/hps_000246.dat")); // ECal data in pulse integral mode
        
        File evioTestData = new File("/u/ey/rherbst/projects/heavyp/devel/software/data/coda_test_strip.dat");
        
        EvioReader reader = new EvioReader(evioTestData);
        EvioEvent event = reader.parseNextEvent();
        
        int eventCount = 1;
        
        while (true) {
         
            System.out.println("event #" + eventCount);
            System.out.println("event has " + event.getChildCount() + " children");
            /*
            if (event.getChildCount() > 0) {
                for (BaseStructure topBank : event.getChildren()) {
                    System.out.println("top bank has " + topBank.getChildCount() + " children");
                    if (topBank.getChildCount() > 0) {
                        for (BaseStructure subBank : topBank.getChildren()) {
                            System.out.println("subBank has " + subBank.getChildCount() + " children");
                            CompositeData cdata = subBank.getCompositeData();
                            System.out.println("Printing subBank CompositeData...");
                            if (cdata != null)
                                CompositeTester.printCompositeDataObject(cdata);
                        }
                    }
                }               
            }
            */
            
            if (reader.getNumEventsRemaining() == 0) {
                break;
            } 
            else {
                event = reader.parseNextEvent();
            }
            ++eventCount;
            System.out.println("--------------------");
        }
        
    }       
}
