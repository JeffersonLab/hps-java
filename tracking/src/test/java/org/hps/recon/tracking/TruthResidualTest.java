/**
 * 
 */
package org.hps.recon.tracking;

import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.gbl.TruthResiduals;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Test class to check truth particle and propagated position.
 * @author phansson <phansson@slac.stanford.edu>
 * @author mdiamond <mdiamond@slac.stanford.edu>
 * @version $id: 2.0 06/04/17$
 */
public class TruthResidualTest extends ReconTestSkeleton {
    static final String inputFileName = "ap_prompt_raw.slcio";
    private AIDA aida;
    private static final double maxResMean = 1.0; //in mm 
    private static final double maxResRMS = 1.0; //in mm 

    public void testRecon() throws Exception {

        testInputFileName = inputFileName;
        aida = AIDA.defaultInstance();

        String aidaOutputName = "target/test-output/TestResiduals_" + inputFileName.replaceAll("slcio", "aida");
        nEvents = -1;
        testTrackingDriver = new TestResiduals();
        ((TestResiduals) testTrackingDriver).setOutputPlots(aidaOutputName);
        ((TestResiduals) testTrackingDriver).aida = aida;
        super.testRecon();

        IHistogram1D hx1 = aida.histogram1D("dres_truthsimhit_layer1_x");
        if (hx1 != null && hx1.entries() > 10) {
            System.out.printf("Layer 1 x truth hit residual has mean %f , RMS %f \n", hx1.mean(), hx1.rms());
            assertTrue("Mean of layer 1 x truth hit residual is not zero " + hx1.mean(), Math.abs(hx1.mean()) < maxResMean);
            assertTrue("RMS of layer 1 x truth hit residual is not zero" + hx1.rms(), Math.abs(hx1.rms()) < maxResRMS);
        }

        IHistogram1D hy1 = aida.histogram1D("dres_truthsimhit_layer1_y");
        if (hy1 != null && hy1.entries() > 10) {
            System.out.printf("Layer 1 y truth hit residual has mean %f , RMS %f \n", hy1.mean(), hy1.rms());
            assertTrue("Mean of layer 1 y truth hit residual is not zero " + hy1.mean(), Math.abs(hy1.mean()) < maxResMean);
            assertTrue("RMS of layer 1 y truth hit residual is not zero" + hy1.rms(), Math.abs(hy1.rms()) < maxResRMS);
        }

    }

    static class TestResiduals extends Driver {

        private TruthResiduals truthRes;
        public AIDA aida = null;
        private String outputPlots = null;

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
        }

        @Override
        protected void process(EventHeader event) {

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
