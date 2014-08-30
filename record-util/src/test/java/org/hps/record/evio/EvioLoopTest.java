package org.hps.record.evio;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.jlab.coda.jevio.EvioEvent;

/**
 * Test that the {@link EvioLoop} works.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EvioLoopTest extends TestCase {
        
    File file1 = new File("/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001351.evio.0");
    File file2 = new File("/nfs/slac/g/hps3/data/testrun/runs/evio/hps_001353.evio.0");
    
    public void testEvioRecordLoop() {
        
        List<File> files = new ArrayList<File>();
        files.add(file1);
        files.add(file2);
        
        EvioLoop loop = new EvioLoop();
        loop.addEvioEventProcessor(new DummyEvioEventProcessor());
        loop.setRecordSource(new EvioFileSource(files));
        try {
            loop.loop(-1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("loop processed " + loop.getTotalSupplied() + " events");
    }
    
    static class DummyEvioEventProcessor extends EvioProcessor {        
        
        public void process(EvioEvent event) {
            System.out.println(this.getClass().getSimpleName() + " got EVIO event " + event.getEventNumber());
        }
        
        public void startRun(EvioEvent event) {
            int[] data = event.getIntData();
            int runNumber = data[1];
            System.out.println(this.getClass().getSimpleName() + " starting run " + runNumber);
        }
        
        public void endRun(EvioEvent event) {
            int[] data = event.getIntData();
            int runNumber = data[1];
            System.out.println(this.getClass().getSimpleName() + " ending run " + runNumber);
        }
    }    
}
