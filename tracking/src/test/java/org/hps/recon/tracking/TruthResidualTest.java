/**
 * 
 */
package org.hps.recon.tracking;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
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
public class TruthResidualTest extends ReconTestSkeleton {
    static final String inputFileName = null;
    private AIDA aida;
    private static final double maxResMean = 1e-4; //0.1um 
    private static final double maxResRMS = 5e-4; //0.5um 

    public void testRecon() throws Exception {

        if (inputFileName == null)
            return;
        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();

        String aidaOutputName = "Residuals" + inputFileName.replaceAll("slcio", "root");
        nEvents = -1;
        testTrackingDriver = new TestResiduals();
        ((TestResiduals) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TestResiduals) testTrackingDriver).aida = aida;
        super.testRecon();

        IHistogram1D hx1 = aida.histogram1D("TruthResiduals x Layer 1");
        if (hx1 != null && hx1.entries() > 10) {
            assertTrue("Mean of layer 1 x truth hit residual is not zero " + hx1.mean(), Math.abs(hx1.mean()) < maxResMean);
            assertTrue("RMS of layer 1 x truth hit residual is not zero" + hx1.rms(), Math.abs(hx1.rms()) < maxResRMS);
        }

        IHistogram1D hy1 = aida.histogram1D("TruthResiduals y Layer 1");
        if (hy1 != null && hy1.entries() > 10) {
            assertTrue("Mean of layer 1 y truth hit residual is not zero " + hy1.mean(), Math.abs(hy1.mean()) < maxResMean);
            assertTrue("RMS of layer 1 y truth hit residual is not zero" + hy1.rms(), Math.abs(hy1.rms()) < maxResRMS);
        }

    }

    static class TestResiduals extends Driver {

        private TruthResiduals truthRes;
        public AIDA aida = null;
        private String outputPlots = null;
        IAnalysisFactory af;
        IHistogramFactory hf;

        public void setOutputPlots(String name) {
            outputPlots = name;
        }

        @Override
        public void detectorChanged(Detector detector) {
            if (aida == null)
                aida = AIDA.defaultInstance();
            Hep3Vector bfield = detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 1.));
            truthRes = new TruthResiduals(bfield);
            truthRes.setHideFrame(true);
            af = IAnalysisFactory.create();
            hf = af.createHistogramFactory(af.createTreeFactory().create());
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

        public void endOfData() {
            for (int layer = 1; layer < 7; layer++) {
                IHistogram hx = truthRes.getResidual(layer, "x");
                IHistogram hy = truthRes.getResidual(layer, "y");
                hf.createCopy(String.format("TruthResiduals x Layer %d", layer), (IHistogram1D) hx);
                hf.createCopy(String.format("TruthResiduals y Layer %d", layer), (IHistogram1D) hy);
            }

            if (outputPlots != null) {
                try {
                    aida.saveAs(outputPlots);
                } catch (IOException ex) {
                    Logger.getLogger(TestResiduals.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

    }

}
