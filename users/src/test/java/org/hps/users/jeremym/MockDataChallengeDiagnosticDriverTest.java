package org.hps.users.jeremym;

import java.io.File;
import java.net.URL;

import org.lcsim.job.EventMarkerDriver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;


public class MockDataChallengeDiagnosticDriverTest {
    
    //String mockDataUrl = "http://www.slac.stanford.edu/~meeg/hps2/meeg/mock_data/tritrig-beam-tri_1-10_recon.slcio";        
    static String mockDataUrl = "file:///u1/projects/svn/hps/java/trunk/recon/target/MockDataReconTest.slcio";
    
    public void testSimpleMdcAnalysisDriver() throws Exception {
        
        FileCache cache = new FileCache(new File(new File(".").getCanonicalPath()));
        File file = cache.getCachedFile(new URL(mockDataUrl));
        
        LCSimLoop loop = new LCSimLoop();
        loop.setLCIORecordSource(file);
        EventMarkerDriver eventMarkerDriver = new EventMarkerDriver();
        eventMarkerDriver.setEventInterval(100);
        loop.add(eventMarkerDriver);
        MockDataChallengeDiagnosticDriver analysisDriver = new MockDataChallengeDiagnosticDriver();
        loop.add(analysisDriver);
        loop.loop(-1);
    }

}
