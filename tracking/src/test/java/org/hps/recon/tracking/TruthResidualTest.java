/**
 * 
 */
package org.hps.recon.tracking;

import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.recon.tracking.gbl.TruthResiduals;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to check truth particle and propagated position.
 * @author phansson <phansson@slac.stanford.edu>
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 2.0 06/04/17$
 */
public class TruthResidualTest extends TestCase {
    static final String aidaOutputName = "TruthResidualTest.aida";

    public void testTruthResiduals() throws Exception {
        final String testInputFileName = "target/test-output/ap_prompt_new.slcio";
        final String testOutputFileName = new String(testInputFileName.replaceAll(".slcio", "") + "_TruthResidualTrackingTest.slcio");
        File outputFile = new TestOutputFile(testOutputFileName);
        outputFile.getParentFile().mkdirs();
        File inputFile = new File(testInputFileName);
        inputFile.getParentFile().mkdirs();

        final int nEvents = 100;

        LCSimLoop loop2 = new LCSimLoop();
        loop2.setLCIORecordSource(inputFile);

        TestResiduals test = new TestResiduals();
        loop2.add(test);

        loop2.add(new LCIODriver(outputFile));
        loop2.loop(nEvents, null);
        loop2.dispose();

    }

    static class TestResiduals extends Driver {

        private static final double maxResMean = 1e-4; //0.1um 
        private static final double maxResRMS = 5e-4; //0.5um 
        private TruthResiduals truthRes;
        public AIDA aida = null;

        @Override
        public void detectorChanged(Detector detector) {
            if (aida == null)
                aida = AIDA.defaultInstance();
            Hep3Vector bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
            truthRes = new TruthResiduals(bfield);
            truthRes.setHideFrame(true);
        }

        @Override
        protected void endOfData() {
            // TODO Auto-generated method stub
            super.endOfData();

            IHistogram hx = truthRes.getResidual(1, "x");
            IHistogram hy = truthRes.getResidual(1, "y");
            if (hx != null && hx.entries() > 10) {
                IHistogram1D hx1d = (IHistogram1D) hx;
                assertTrue("Mean of layer 1 truth hit residual is not zero " + hx1d.mean(), Math.abs(hx1d.mean()) < maxResMean);
                assertTrue("RMS of layer 1 truth hit residual is not zero" + hx1d.rms(), Math.abs(hx1d.rms()) < maxResRMS);
            }
            if (hy != null && hy.entries() > 10) {
                IHistogram1D hy1d = (IHistogram1D) hy;
                assertTrue("Mean of layer 1 truth hit residual is not zero " + hy1d.mean(), Math.abs(hy1d.mean()) < maxResMean);
                assertTrue("RMS of layer 1 truth hit residual is not zero " + hy1d.mean(), Math.abs(hy1d.rms()) < maxResRMS);
            }

            try {
                aida.saveAs(aidaOutputName);
            } catch (IOException exception) {
                Logger.getLogger(TestRunTrackReconTest.class.getName()).log(Level.SEVERE, null, exception);
            }
        }

        @Override
        protected void process(EventHeader event) {
            // TODO Auto-generated method stub
            super.process(event);

            List<MCParticle> mcParticles = null;
            if (event.hasCollection(MCParticle.class, "MCParticle")) {
                mcParticles = event.get(MCParticle.class, "MCParticle");
            }

            List<SimTrackerHit> simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");

            if (simTrackerHits != null && mcParticles != null) {
                truthRes.processSim(mcParticles, simTrackerHits);
            }

        }

    }

}
