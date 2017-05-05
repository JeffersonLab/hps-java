package org.hps.recon.tracking;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.StrategyXMLUtils;
import org.lcsim.recon.tracking.seedtracker.diagnostic.SeedTrackerDiagnostics;
import org.lcsim.util.Driver;

/**
 * This class runs the Track Reconstruction for the HPS Test Proposal detector.
 * The tracker digitization must be run in front of it. It is intended to work
 * with the {@link TrackerDigiDriver} digitization Driver.
 */
public final class TrackerReconDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(TrackerReconDriver.class.getPackage().getName());

    private String subdetectorName = "Tracker";
    
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
    private String strategyResource = "HPS-Test-4pt1.xml";
    
    // Output track collection.
    private String trackCollectionName = "MatchedTracks";
    
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
    
    private boolean rejectUncorrectedHits = true;
    
    private boolean rejectSharedHits = false;

    public TrackerReconDriver() {
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
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
     *
     * @param rmsTimeCut
     */
    public void setRmsTimeCut(double rmsTimeCut) {
        this.rmsTimeCut = rmsTimeCut;
    }

    public void setRejectUncorrectedHits(boolean rejectUncorrectedHits) {
        this.rejectUncorrectedHits = rejectUncorrectedHits;
    }

    public void setRejectSharedHits(boolean rejectSharedHits) {
        this.rejectSharedHits = rejectSharedHits;
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
        Hep3Vector fieldInTracker = TrackUtils.getBField(detector);
        LOGGER.config("fieldInTracker: Bx = " + fieldInTracker.x() + "; By = " + fieldInTracker.y() + "; Bz = " + fieldInTracker.z());
        this.bfield = Math.abs(fieldInTracker.y());
        LOGGER.config(String.format("%s: Set B-field to %.6f\n", this.getClass().getSimpleName(), this.bfield));

        initialize();

        super.detectorChanged(detector);
    }

    /**
     * Setup all the child Drivers necessary for track reconstruction.
     */
    private void initialize() {

        if (!strategyResource.startsWith("/")) {
            strategyResource = "/org/hps/recon/tracking/strategies/" + strategyResource;
        }
        List<SeedStrategy> sFinallist = StrategyXMLUtils.getStrategyListFromInputStream(this.getClass().getResourceAsStream(strategyResource));
        SeedTracker stFinal = new SeedTracker(sFinallist, this._useHPSMaterialManager, this.includeMS);
        stFinal.setSubdetectorName(subdetectorName);
        stFinal.setApplySectorBinning(_applySectorBinning);
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

        if (rmsTimeCut > 0) {
            HitTimeTrackCheck timeCheck = new HitTimeTrackCheck(rmsTimeCut);
            timeCheck.setDebug(debug);
            stFinal.setTrackCheck(timeCheck);
        }        
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

        if (rejectUncorrectedHits) {
            Iterator<Track> iter = tracks.iterator();
            trackLoop:
            while (iter.hasNext()) {
                Track track = iter.next();
                for (TrackerHit hit : track.getTrackerHits()) {
                    HelicalTrackHit hth = (HelicalTrackHit) hit;
                    double correction = VecOp.sub(hth.getCorrectedPosition(), new BasicHep3Vector(hth.getPosition())).magnitude();
                    double chisq = hth.chisq();
                    if (correction < 1e-6) {
                        this.getLogger().warning(String.format("Discarding track with bad HelicalTrackHit (correction distance %f, chisq penalty %f)", correction, chisq));
                        iter.remove();
                        continue trackLoop;
                    }
                }
            }
        }

        if (rejectSharedHits) {
            RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
            RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

            Map<TrackerHit, List<Track>> stripsToTracks = new HashMap<TrackerHit, List<Track>>();
            for (Track track : tracks) {
                for (TrackerHit hit : track.getTrackerHits()) {
                    Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
                    for (TrackerHit strip : htsList) {
                        List<Track> sharedTracks = stripsToTracks.get(strip);
                        if (sharedTracks == null) {
                            sharedTracks = new ArrayList<Track>();
                            stripsToTracks.put(strip, sharedTracks);
                        }
                        sharedTracks.add(track);
                    }
                }
            }
            Iterator<Track> iter = tracks.iterator();
            trackLoop:
            while (iter.hasNext()) {
                Track track = iter.next();
                for (TrackerHit hit : track.getTrackerHits()) {
                    Collection<TrackerHit> htsList = hitToStrips.allFrom(hitToRotated.from(hit));
                    for (TrackerHit strip : htsList) {
                        List<Track> sharedTracks = stripsToTracks.get(strip);
                        if (sharedTracks.size() > 1) {
                            for (Track otherTrack : sharedTracks) {
                                if (otherTrack.getChi2() < track.getChi2()) {
                                    this.getLogger().warning(String.format("removing track with shared hits: chisq %f, d0 %f (other track has chisq %f)", track.getChi2(), track.getTrackStates().get(0).getD0(), otherTrack.getChi2()));
                                    iter.remove();
                                    continue trackLoop;
                                }
                            }
                        }
                    }
                }
            }
        }

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
            System.out.println(this.getName() + " with strategy " + strategyResource + " found " + ntracks + " tracks in " + nevents + " events which is " + ((double) ntracks / (double) nevents) + " tracks per event.");
        }
    }
}
