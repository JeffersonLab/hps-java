package org.hps.recon.tracking.nobfield;

import hep.physics.vec.BasicHep3Vector;
import java.util.List;
import org.hps.recon.tracking.CoordinateTransformations;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.StrategyXMLUtils;
import org.lcsim.recon.tracking.seedtracker.diagnostic.SeedTrackerDiagnostics;
import org.lcsim.util.Driver;

/**
 * This class runs the Track Reconstruction 0 B-Field  events.
 * The tracker digitization must be run in front of it. It is intended to work 
 *
 * @author Matt Graham <mgraham@slac.stanford.edu>
 */
public final class StraightTrackReconDriver extends Driver {

    // Debug flag.
    private boolean debug = false;
    // Tracks found across all events.
    int ntracks = 0;
    // Number of events processed.
    int nevents = 0;
    // Cache detector object.
    Detector detector = null;
    // Default B-field value.
    private double bfield = 0.5;
    // Tracking strategies resource path.
    private String strategyResource = "HPS-Full-StraightTracks.xml";
    // Output track collection.
    private String trackCollectionName = "StraightTracks";
    // HelicalTrackHit input collection.
    private String stInputCollectionName = "RotatedHelicalTrackHits";
    // Include MS (done by removing XPlanes from the material manager results)
    private boolean includeMS = true;
    // number of repetitive fits on confirmed/extended tracks
    private int _iterativeConfirmed = 3;
    // use HPS implementation of material manager
    private boolean _useHPSMaterialManager = true;
    // enable the use of sectoring using sector binning in SeedTracker
    private boolean _applySectorBinning = true;
    private double rmsTimeCut = -1;

    public StraightTrackReconDriver() {
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Set the tracking strategy resource.
     *
     * @param strategyResource The absolute path to the strategy resource in the
     * hps-java jar.
     */
    public void setStrategyResource(String strategyResource) {
        this.strategyResource = strategyResource;
    }

    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.stInputCollectionName = inputHitCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setIncludeMS(boolean incMS) {
        this.includeMS = incMS;
    }

    /**
     * Set to enable the use of the HPS material manager implementation
     *
     * @param useHPSMaterialManager switch
     */
    public void setUseHPSMaterialManager(boolean useHPSMaterialManager) {
        this._useHPSMaterialManager = useHPSMaterialManager;
    }

    public void setIterativeFits(int val) {
        this._iterativeConfirmed = val;
    }

    /**
     * Set to enable the sectoring to use the sector bins in checking for
     * consistent hits.
     *
     * @param applySectorBinning apply sector binning switch
     */
    public void setApplySectorBinning(boolean applySectorBinning) {
        this._applySectorBinning = applySectorBinning;
    }

    /**
     * Set time cut.
     * @param rmsTimeCut
     */
    public void setRmsTimeCut(double rmsTimeCut) {
        this.rmsTimeCut = rmsTimeCut;
    }

    /**
     * This is used to setup the Drivers after XML config.
     */
    @Override
    public void detectorChanged(Detector detector) {
        // Cache Detector object.
        this.detector = detector;

        // Get B-field Y with no sign. Seed Tracker doesn't like signed B-field components.
        // FIXME Is this always right?
        this.bfield = Math.abs((detector.getFieldMap().getField(new BasicHep3Vector(0, 0, 0)).y()));
        if (debug) {
            System.out.printf("%s: Set B-field to %.3f\n", this.getClass().getSimpleName(), this.bfield);
        }

        initialize();

        super.detectorChanged(detector);
    }

    /**
     * Setup all the child Drivers necessary for track reconstruction.
     */
    private void initialize() {

        // 
        // 1) Driver to run Seed Tracker.
        //
        if (!strategyResource.startsWith("/")) {
            strategyResource = "/org/hps/recon/tracking/strategies/" + strategyResource;
        }
        List<SeedStrategy> sFinallist = StrategyXMLUtils.getStrategyListFromInputStream(this.getClass().getResourceAsStream(strategyResource));
        StraightTracker stFinal = new StraightTracker(sFinallist, this._useHPSMaterialManager, this.includeMS);
//        stFinal.setApplySectorBinning(_applySectorBinning);
        stFinal.setUseDefaultXPlane(false);
        stFinal.setDebug(this.debug);
        stFinal.setIterativeConfirmed(_iterativeConfirmed);
        stFinal.setMaterialManagerTransform(CoordinateTransformations.getTransform());
        stFinal.setInputCollectionName(stInputCollectionName);
        stFinal.setTrkCollectionName(trackCollectionName);
        stFinal.setBField(bfield);
        if (debug) {
            stFinal.setDiagnostics(new SeedTrackerDiagnostics());
        }
        // stFinal.setSectorParams(false); //this doesn't actually seem to do anything
        stFinal.setSectorParams(1, 10000);
        add(stFinal);
//
//        if (rmsTimeCut > 0) {
//            stFinal.setTrackCheck(new HitTimeTrackCheck(rmsTimeCut));
//        }
    }

    /**
     * This method is used to run the reconstruction and print debug
     * information.
     */
    @Override
    public void process(EventHeader event) {
        // This call runs the track reconstruction using the sub-Drivers.
        super.process(event);

        // Debug printouts.
        if (debug) {
            if (event.hasCollection(HelicalTrackHit.class, stInputCollectionName)) {
                System.out.println(this.getClass().getSimpleName() + ": The HelicalTrackHit collection " + stInputCollectionName + " has " + event.get(HelicalTrackHit.class, stInputCollectionName).size() + " hits.");
            } else {
                System.out.println(this.getClass().getSimpleName() + ": No HelicalTrackHit collection for this event");
            }
            // Check for Tracks.
            List<Track> tracks = event.get(Track.class, trackCollectionName);
            System.out.println(this.getClass().getSimpleName() + ": The Track collection " + trackCollectionName + " has " + tracks.size() + " tracks.");

            // Print out track info.
            for (Track track : tracks) {
                System.out.println(this.getClass().getSimpleName() + ": " + track.toString());
                System.out.println(this.getClass().getSimpleName() + ": number of layers = " + track.getTrackerHits().size());
                System.out.println(this.getClass().getSimpleName() + ": chi2 = " + track.getChi2());
            }
        }

        // Set the type of track to indicate B-field in Y e.g. for swimming in Wired.
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        setTrackType(tracks);

        // Increment number of events.
        ++nevents;

        // Add to tracks found.
        ntracks += event.get(Track.class, trackCollectionName).size();
    }

    /**
     * Set the track type to Y_FIELD so swimming is done correctly in Wired.
     *
     * @param tracks The list of <code>Track</code> objects.
     */
    private void setTrackType(List<Track> tracks) {
        for (Track track : tracks) {
            ((BaseTrack) track).setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        }
    }

    @Override
    public void endOfData() {
        if (debug) {
            System.out.println("-------------------------------------------");
            System.out.println(this.getName() + " found " + ntracks + " tracks in " + nevents + " events which is " + ((double) ntracks / (double) nevents) + " tracks per event.");
        }
    }
}
