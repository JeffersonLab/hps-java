package org.hps.users.jeremym;

import org.hps.conditions.deprecated.CalibrationDriver;
import org.hps.recon.ecal.EcalClusterer;
import org.hps.recon.ecal.EcalRawConverterDriver;
import org.hps.recon.tracking.DataTrackerHitDriver;
import org.hps.recon.tracking.HelicalTrackHitDriver;
import org.hps.recon.tracking.RawTrackerHitFitterDriver;
import org.hps.recon.tracking.TrackerReconDriver;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;

/**
 * <p>
 * This is a Driver that does the same thing as this steering file:
 * <p>
 * steering-files/src/main/resources/org/hps/steering/recon/TestRunOfflineRecon.lcsim
 * <p>
 * It should only be used for testing purposes or for convenience, as it is not configurable.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class TestRunReconDriver extends Driver {
    
    public TestRunReconDriver() {
        
        //CalibrationDriver calibrationDriver = new CalibrationDriver();
        //calibrationDriver.setRunNumber(975);
        //this.add(calibrationDriver);
        
        RawTrackerHitSensorSetup rawTrackerHitDriver = new RawTrackerHitSensorSetup();
        this.add(rawTrackerHitDriver);

        EcalRawConverterDriver ecalRawConverter = new EcalRawConverterDriver();
        ecalRawConverter.setEcalCollectionName("EcalCalHits");
        ecalRawConverter.setUse2014Gain(false);
        this.add(ecalRawConverter);
        
        EcalClusterer ecalClusterer = new EcalClusterer();
        ecalClusterer.setEcalName("Ecal");
        ecalClusterer.setEcalCollectionName("EcalCalHits");
        this.add(ecalClusterer);
        
        RawTrackerHitFitterDriver fitterDriver = new RawTrackerHitFitterDriver();
        fitterDriver.setFitAlgorithm("Analytic");
        this.add(fitterDriver);
                
        DataTrackerHitDriver trackerHitDriver = new DataTrackerHitDriver();
        this.add(trackerHitDriver);

        HelicalTrackHitDriver helicalTrackHitDriver = new HelicalTrackHitDriver();
        helicalTrackHitDriver.setDebug(false);
        helicalTrackHitDriver.setMaxSeperation(20.0);
        helicalTrackHitDriver.setTolerance(1.0);
        this.add(helicalTrackHitDriver);
        
        TrackerReconDriver trackerReconDriver = new TrackerReconDriver();
        trackerReconDriver.setDebug(false);
        this.add(trackerReconDriver);

        ReadoutCleanupDriver cleanupDriver = new ReadoutCleanupDriver();
        cleanupDriver.setCollection("TrackerHits");
        this.add(cleanupDriver);        
    }
    
    public void detectorChanged(Detector detector) {
        super.detectorChanged(detector);
    }
    
    public void endOfData() {
        super.endOfData();
    }
    
    public void process(EventHeader event) {
        super.process(event);
    }
    
    public void startOfData() {
        super.startOfData();
    }
}