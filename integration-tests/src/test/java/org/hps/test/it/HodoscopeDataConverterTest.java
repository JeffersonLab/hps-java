package org.hps.test.it;

import java.io.File;
import java.io.IOException;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.evio.EvioToLcio;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

public class HodoscopeDataConverterTest extends TestCase {
    
    private static String DETECTOR = "HPS-HodoscopeTest-v1";
    private static Integer RUN_NUMBER = 1000000;
    private static String LOCAL_FILE_PATH = "/work/slac/hps-projects/projects/hodoscope-dev/hpshodo_000322_100evts.evio";
    private static String OUTPUT_FILE_NAME = "hodo_cnv_test.slcio";
    
    public void testHodoscopeDataConverter() throws IOException {
        // Run data conversion.
        String args[] = {
                "-d",
                DETECTOR,
                "-R",
                RUN_NUMBER.toString(),
                LOCAL_FILE_PATH,
                "-l",
                OUTPUT_FILE_NAME
        };
        EvioToLcio.main(args);
            
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        loop.setLCIORecordSource(new File(OUTPUT_FILE_NAME));
        loop.add(new HodoscopeDataDriver());
        loop.loop(-1);
    }    
    
    private class HodoscopeDataDriver extends Driver {
        public void process(EventHeader event) {
            System.out.println("Process event " + event.getEventNumber());
        }
    }
    
}
