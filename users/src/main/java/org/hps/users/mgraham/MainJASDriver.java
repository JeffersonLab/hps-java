package org.hps.users.mgraham;

import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.recon.tracking.DataTrackerHitDriver;
import org.hps.recon.tracking.RawTrackerHitFitterDriver;
import org.hps.recon.tracking.HelicalTrackHitDriver;
import org.hps.recon.tracking.TrackerReconDriver;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.util.Driver;

/**
 * Driver for track reconstruction and analysis of HPS detector for execution in JAS.
 *
 * @author M. Graham 
 */
public final class MainJASDriver extends Driver {



    public MainJASDriver() {
//        add(new HPSSVTSensorSetup());
        //add(new CalibrationDriver());
        add(new RawTrackerHitSensorSetup());
        //   Can remove HPSRawTrackerHitFitterDriver and DataTrackerHitDriver for integrated MC
        RawTrackerHitFitterDriver hitfitter=new RawTrackerHitFitterDriver();
        hitfitter.setCorrectT0Shift(true);
        hitfitter.setFitAlgorithm("Analytic");
        add(hitfitter);
        add(new DataTrackerHitDriver());
        
//        add(new TrackerDigiDriver());  //add for integrated MC
        HelicalTrackHitDriver hth = new HelicalTrackHitDriver();
        hth.setClusterTimeCut(8.0);
        
//        SingleSensorHelicalTrackHitDriver hth = new SingleSensorHelicalTrackHitDriver();
        hth.setMaxSeperation(20.0);
        hth.setTolerance(1.0);
        add(hth);
        TrackerReconDriver trd=new TrackerReconDriver();
        
//          trd.setStrategyResource("/org/lcsim/hps/recon/tracking/strategies/HPS-SingleSensors.xml");
        trd.setStrategyResource("/org/lcsim/hps/recon/tracking/strategies/HPS-Full.xml");
//    trd.setStrategyResource("/org/lcsim/hps/recon/tracking/strategies/HPS-Test-Lyr50.xml");
        trd.setDebug(true);
        add(trd);
//
        DetailedAnalysisDriver dad=new DetailedAnalysisDriver(12);
        dad.setRawHitsName("SVTRawTrackerHits");
        add(dad);

//        add(new FastTrackAnalysisDriver());
        
       
    }

  
}
