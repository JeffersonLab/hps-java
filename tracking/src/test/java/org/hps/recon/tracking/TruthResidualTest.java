/**
 * 
 */
package org.hps.recon.tracking;

import hep.aida.IHistogram;
import hep.aida.IHistogram1D;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.File;
import java.net.URL;
import java.util.List;

import junit.framework.TestCase;

import org.hps.recon.tracking.gbl.TruthResiduals;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.util.Driver;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCIODriver;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.test.TestUtil.TestOutputFile;

/**
 * Test class to check truth particle and propagated position.
 * @author phansson <phansson@slac.stanford.edu>
 * @version $id: $
 */
public class TruthResidualTest extends TestCase {

	
	
	private static final String testFileName = "";
	private static final String testURLBase = null;
	private static final long nEvents = 1000;

	public void testTruthResiduals() throws Exception{
		 	File lcioInputFile = null;

	        URL testURL = new URL(testURLBase + "/" + testFileName);
	        FileCache cache = new FileCache();
	        lcioInputFile = cache.getCachedFile(testURL);

	        //Process and write out the file
	        LCSimLoop loop = new LCSimLoop();
	        loop.setLCIORecordSource(lcioInputFile);
	        loop.add(new MainTrackingDriver());
	        File outputFile = new TestOutputFile(testFileName.replaceAll(".slcio", "") + "_hpsTrackTruthResidualTrackingTest.slcio");
	        outputFile.getParentFile().mkdirs(); //make sure the parent directory exists
	        loop.add(new LCIODriver(outputFile));
	        loop.loop(nEvents, null);
	        loop.dispose();

	        //Read LCIO back and test!
	        LCSimLoop readLoop = new LCSimLoop();
	        readLoop.add(new TestResiduals());
	        readLoop.setLCIORecordSource(outputFile);
	        readLoop.loop(nEvents, null);
	        readLoop.dispose();
	}
	
	
	static class TestResiduals extends Driver {

		private static final double maxResMean = 1e-4; //0.1um 
		private static final double maxResRMS = 5e-4; //0.5um 
		private TruthResiduals truthRes;
		
		  @Override
		    public void detectorChanged(Detector detector) {
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
			if (hx != null && hx.entries()>10) {
				IHistogram1D hx1d = (IHistogram1D)hx;
				assertTrue("Mean of layer 1 truth hit residual is not zero " + hx1d.mean(), Math.abs(hx1d.mean()) >maxResMean );
				assertTrue("RMS of layer 1 truth hit residual is not zero" + hx1d.rms(), Math.abs(hx1d.rms()) >maxResRMS );
			}
			if (hy != null && hy.entries()>10) {
				IHistogram1D hy1d = (IHistogram1D)hy;
				assertTrue("Mean of layer 1 truth hit residual is not zero " + hy1d.mean(), Math.abs(hy1d.mean()) >maxResMean );
				assertTrue("RMS of layer 1 truth hit residual is not zero " + hy1d.mean(), Math.abs(hy1d.rms()) >maxResRMS );
			}
		}

		@Override
		protected void process(EventHeader event) {
			// TODO Auto-generated method stub
			super.process(event);
			
			List<MCParticle> mcParticles = null;
			if(event.hasCollection(MCParticle.class,"MCParticle")) {
				mcParticles = event.get(MCParticle.class,"MCParticle");
	        } 
	            
	        List<SimTrackerHit> simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
	        
	        
	        if(simTrackerHits != null && mcParticles != null) {
				truthRes.processSim(mcParticles, simTrackerHits);
	        }
	        
		}
		
	}
	
	private class MainTrackingDriver extends Driver {
        
        public MainTrackingDriver() {

            //Setup the sensors and calibrations
            add(new RawTrackerHitSensorSetup());
            RawTrackerHitFitterDriver hitfitter = new RawTrackerHitFitterDriver();
            hitfitter.setFitAlgorithm("Analytic");
            hitfitter.setCorrectT0Shift(true);
            add(hitfitter);
            add(new DataTrackerHitDriver());
            HelicalTrackHitDriver hth_driver = new HelicalTrackHitDriver();
            hth_driver.setMaxSeperation(20.0);
            hth_driver.setTolerance(1.0);
            add(hth_driver);
            TrackerReconDriver track_recon_driver = new TrackerReconDriver();
            add(track_recon_driver);
        }

    }
	
	
	
}
