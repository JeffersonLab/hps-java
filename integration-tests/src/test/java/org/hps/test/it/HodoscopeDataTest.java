package org.hps.test.it;

import java.io.File;
import java.net.URL;

import org.hps.analysis.hodoscope.SimpleHodoscopeAnalysisDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.test.util.TestOutputFile;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

public class HodoscopeDataTest extends TestCase {

    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/slicHodoTestEvents.slcio";
    
    public void testHodoscopeData() throws Exception {
        
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));
        
        // FIXME: Local file
        //File testFile = new File("/work/slac/hps-projects/hodoscope-dev/slicHodoTestEvents.slcio");
        
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        
        loop.setLCIORecordSource(testFile);
        
        SimpleHodoscopeAnalysisDriver anal = new SimpleHodoscopeAnalysisDriver();       
        loop.add(anal);
        
        File aidaPlotFile = new TestOutputFile(HodoscopeDataTest.class, "HodoscopeDataTest.aida");
        AidaSaveDriver aidaSaveDriver = new AidaSaveDriver();
        aidaSaveDriver.setOutputFileName(aidaPlotFile.getPath());
        loop.add(aidaSaveDriver);
        
        File rootPlotFile  = new TestOutputFile(HodoscopeDataTest.class, "HodoscopeDataTest.root");
        AidaSaveDriver rootSaveDriver = new AidaSaveDriver();
        rootSaveDriver.setOutputFileName(rootPlotFile.getPath());
        loop.add(rootSaveDriver);
        
        loop.loop(-1);
    }
}
