package org.hps.test.it;

import java.io.File;
import java.net.URL;
import java.util.List;

import org.hps.analysis.hodoscope.SimpleHodoscopeAnalysisDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.test.util.TestOutputFile;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import hep.physics.vec.Hep3Vector;
import junit.framework.TestCase;

public class HodoscopeSimDataTest extends TestCase {

    static final String TEST_FILE_URL = "http://www.lcsim.org/test/hps-java/slicHodoTestEvents.slcio";
    
    public void testHodoscopeData() throws Exception {
        
        FileCache cache = new FileCache();
        File testFile = cache.getCachedFile(new URL(TEST_FILE_URL));
                
        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();
        
        loop.setLCIORecordSource(testFile);
        
        loop.add(new SimpleHodoscopeAnalysisDriver());
        HodoscopeTestDriver hodoTestDriver = new HodoscopeTestDriver();
        hodoTestDriver.setDebug(true);
        loop.add(hodoTestDriver);
        
        File aidaPlotFile = new TestOutputFile(HodoscopeSimDataTest.class, "HodoscopeDataTest.aida");
        AidaSaveDriver aidaSaveDriver = new AidaSaveDriver();
        aidaSaveDriver.setOutputFileName(aidaPlotFile.getPath());
        loop.add(aidaSaveDriver);
        
        File rootPlotFile  = new TestOutputFile(HodoscopeSimDataTest.class, "HodoscopeDataTest.root");
        AidaSaveDriver rootSaveDriver = new AidaSaveDriver();
        rootSaveDriver.setOutputFileName(rootPlotFile.getPath());
        loop.add(rootSaveDriver);
        
        loop.loop(-1);
    }
    
    public class HodoscopeTestDriver extends Driver {
        
        private HodoscopeDetectorElement hodoDetElem;
        private IIdentifierHelper helper;
        private String hitCollName = "HodoscopeHits";
        
        private boolean debug = true;
        
        protected void detectorChanged(Detector det) {
            hodoDetElem = (HodoscopeDetectorElement) det.getSubdetector("Hodoscope").getDetectorElement();
            helper = hodoDetElem.getIdentifierHelper();
        }
        
        protected void process(EventHeader event) {
            List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, hitCollName);
            LCMetaData meta = event.getMetaData(simHits);
            IDDecoder dec = meta.getIDDecoder();       
            for (SimTrackerHit simHit : simHits) {
                //double[] pos = simHit.getPosition();
                Hep3Vector posVec = simHit.getPositionVec();
                
                dec.setID(simHit.getCellID64());
                //int layer = dec.getValue("layer");
                //int ix = dec.getValue("ix");
                //int iy = dec.getValue("iy");
                                
                IIdentifier hitId = simHit.getIdentifier();
                IDetectorElement idDetElem = hodoDetElem.findDetectorElement(hitId).get(0);
                if (debug)
                    System.out.println("found DE from ID: " + idDetElem.getName());
                
                IDetectorElement posDetElem = hodoDetElem.findDetectorElement(posVec);
                if (debug) {
                    System.out.println("found DE from pos: " + posDetElem.getName());
                    System.out.println();
                }
                
                TestCase.assertEquals("Detector elements different from ID and position!", idDetElem, posDetElem);
                
                //IGeometryInfo geom = idDetElem.getGeometry();

                if (debug)
                    System.out.println("Lookup scint dims for hit: " + helper.unpack(hitId));
                double[] scintDims = hodoDetElem.getScintillatorHalfDimensions(simHit);
                if (debug)
                    System.out.println("scintDims: " + scintDims[0] + " " + scintDims[1] + " " + scintDims[2]);
            }
        }
        
        void setDebug(boolean debug) {
            this.debug = debug;
        }
    }
}
