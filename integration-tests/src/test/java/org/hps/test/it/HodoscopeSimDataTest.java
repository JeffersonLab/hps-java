package org.hps.test.it;

import java.io.File;
import java.util.List;

import org.hps.analysis.hodoscope.SimpleHodoscopeAnalysisDriver;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.hodoscope.HodoscopeDetectorElement;
import org.hps.util.test.TestUtil;
import org.hps.util.test.TestOutputFile;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.job.AidaSaveDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCSimLoop;

import hep.physics.vec.Hep3Vector;
import junit.framework.TestCase;

public class HodoscopeSimDataTest extends TestCase {

    public void testHodoscopeData() throws Exception {

        File testFile = TestUtil.downloadTestFile("slicHodoTestEvents.slcio");

        LCSimLoop loop = new LCSimLoop();
        DatabaseConditionsManager.getInstance();

        loop.setLCIORecordSource(testFile);

        loop.add(new SimpleHodoscopeAnalysisDriver());
        HodoscopeTestDriver hodoTestDriver = new HodoscopeTestDriver();
        hodoTestDriver.setDebug(false);
        loop.add(hodoTestDriver);

        File aidaPlotFile = new TestOutputFile(HodoscopeSimDataTest.class, "plots.aida");
        AidaSaveDriver aidaSaveDriver = new AidaSaveDriver();
        aidaSaveDriver.setOutputFileName(aidaPlotFile.getPath());
        loop.add(aidaSaveDriver);

        File rootPlotFile  = new TestOutputFile(HodoscopeSimDataTest.class, "plots.root");
        AidaSaveDriver rootSaveDriver = new AidaSaveDriver();
        rootSaveDriver.setOutputFileName(rootPlotFile.getPath());
        loop.add(rootSaveDriver);

        loop.loop(-1);
    }

    public class HodoscopeTestDriver extends Driver {

        private HodoscopeDetectorElement hodoDetElem;
        private String hitCollName = "HodoscopeHits";

        private boolean debug = true;

        protected void detectorChanged(Detector det) {
            hodoDetElem = (HodoscopeDetectorElement) det.getSubdetector("Hodoscope").getDetectorElement();
        }

        protected void process(EventHeader event) {
            List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, hitCollName);
            for (SimTrackerHit simHit : simHits) {
                Hep3Vector posVec = simHit.getPositionVec();

                IIdentifier hitId = simHit.getIdentifier();
                IDetectorElement idDetElem = hodoDetElem.findDetectorElement(hitId).get(0);
                if (debug)
                    System.out.println("found DE from ID: " + idDetElem.getName());

                IDetectorElement posDetElem = hodoDetElem.findDetectorElement(posVec);
                if (debug) {
                    System.out.println("found DE from pos: " + posDetElem.getName());
                }

                TestCase.assertEquals("Detector elements different from ID and position!", idDetElem, posDetElem);

                //IGeometryInfo geom = idDetElem.getGeometry();

                double[] scintDims = hodoDetElem.getScintillatorHalfDimensions(simHit);
                if (debug)
                    System.out.println("scintDims: " + scintDims[0] + " " + scintDims[1] + " " + scintDims[2]);

                if (debug)
                    System.out.println();
            }
        }

        void setDebug(boolean debug) {
            this.debug = debug;
        }
    }
}
