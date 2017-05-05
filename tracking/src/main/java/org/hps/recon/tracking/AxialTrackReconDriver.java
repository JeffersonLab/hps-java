package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrack2DHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.MultipleScatter;
import org.lcsim.fit.line.SlopeInterceptLineFit;
import org.lcsim.fit.line.SlopeInterceptLineFitter;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.recon.tracking.seedtracker.StrategyXMLUtils;
import org.lcsim.util.Driver;

/**
 * Axial track reconstruction.
 */
public final class AxialTrackReconDriver extends Driver {

    // Debug flag.
    // private final static boolean DEBUG = false;
    private boolean debug = false;
    // Tracks found across all events.
    int ntracks = 0;
    // Number of events processed.
    int nevents = 0;
    // Cache detector object.
    Detector detector = null;
    // Default B-field value.
    private double bfield = 0.5;
    // Name of the SVT subdetector.
    private String subdetectorName = "Tracker";
    // SimTrackerHit input collection for readout cleanup.
    private String simTrackerHitCollectionName = "TrackerHits";
    // Tracking strategies resource path.
    private String strategyResource = "HPS-Test-1pt3.xml";
    // Output track collection.
    private String trackCollectionName = "MatchedTracks";
    // HelicalTrackHit input collection.
    private String stInputCollectionName = "RotatedHelicalTrackHits";
    // Output hit collection for HelicalTrackHits.
    private String hthOutputCollectionName = "HelicalTrackHits";
    // Input strip hits collection from digi.
    private String stripHitsCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    // Hit relations output collection.
    private String helicalTrackHitRelationsCollectionName = "HelicalTrackHitRelations";
    // Track to MC relations output collection.
    private String helicalTrackMCRelationsCollectionName = "HelicalTrackMCRelations";
    // Max strip separation when making HelicalTrackHits.
    private double stripMaxSeparation = 10.01;
    // Tolerance factor when making HelicalTrackHits.
    private double stripTolerance = 0.01;
    private List<SeedStrategy> sFinallist;

    public AxialTrackReconDriver() {
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
     * @param strategyResource The absolute path to the strategy resource in the hps-java jar.
     */
    public void setStrategyResource(String strategyResource) {
        this.strategyResource = strategyResource;
    }

    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        this.helicalTrackHitRelationsCollectionName = helicalTrackHitRelationsCollectionName;
    }

    public void setHelicalTrackMCRelationsCollectionName(String helicalTrackMCRelationsCollectionName) {
        this.helicalTrackMCRelationsCollectionName = helicalTrackMCRelationsCollectionName;
    }

    public void setInputHitCollectionName(String inputHitCollectionName) {
        this.stInputCollectionName = inputHitCollectionName;
    }

    public void setOutputHitCollectionName(String outputHitCollectionName) {
        this.hthOutputCollectionName = outputHitCollectionName;
    }

    public void setStripHitsCollectionName(String stripHitsCollectionName) {
        this.stripHitsCollectionName = stripHitsCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setStripMaxSeparation(double stripMaxSeparation) {
        this.stripMaxSeparation = stripMaxSeparation;
    }

    public void setStripTolerance(double stripTolerance) {
        this.stripTolerance = stripTolerance;
    }

    /**
     * Set the SimTrackerHit collection to be used for tracking.
     * 
     * @param simTrackerHitCollectionName The name of the SimTrackerHit collection in the event.
     */
    public void setSimTrackerHitCollectionName(String simTrackerHitCollectionName) {
        this.simTrackerHitCollectionName = simTrackerHitCollectionName;
    }

    /**
     * This is used to setup the Drivers after XML config.
     */
    public void detectorChanged(Detector detector) {
        // Cache Detector object.
        this.detector = detector;

        // Get B-field Y with no sign. Seed Tracker doesn't like signed B-field components.
        // FIXME Is this always right?
        this.bfield = Math.abs((detector.getFieldMap().getField(new BasicHep3Vector(0, 0, 0)).y()));
        if (debug) {
            System.out.println("Set B-field to " + this.bfield);
        }

        initialize();

        super.detectorChanged(detector);
    }

    /**
     * Setup all the child Drivers necessary for track reconstruction.
     */
    private void initialize() {
        //
        // 1) Driver to create HelicalTrackHits expected by Seedtracker.
        //
        // TODO Make this step its own separate Driver??? (Matt)

        // Setup default stereo pairings, which should work for even number of
        // modules.
        List<SiTrackerModule> modules = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(SiTrackerModule.class);
        if (modules.size() == 0) {
            throw new RuntimeException("No SiTrackerModules found in detector.");
        }

        // Create the Driver.
        HelicalTrackHitDriver hthdriver = new HelicalTrackHitDriver();
        hthdriver.addCollection(stripHitsCollectionName);
        hthdriver.setOutputCollectionName(hthOutputCollectionName);
        // hthdriver.setHitRelationName(helicalTrackHitRelationsCollectionName);
        // hthdriver.setMCRelationName(helicalTrackMCRelationsCollectionName);

        // hthdriver.setStripMaxSeparation(stripMaxSeparation);
        // hthdriver.setStripTolerance(stripTolerance); // user parameter?
        hthdriver.setTransformToTracking(true);
        hthdriver.setDebug(true);
        add(hthdriver);

        //
        // 2) Driver to run Seed Tracker.
        //
        sFinallist = StrategyXMLUtils.getStrategyListFromInputStream(this.getClass().getResourceAsStream(strategyResource));
        // 3) Cleanup the readouts for next event.
        //
        List<String> readoutCleanup = new ArrayList<String>();
        readoutCleanup.add(this.simTrackerHitCollectionName);
        add(new ReadoutCleanupDriver(readoutCleanup));

    }

    /**
     * Call super for child processing at start of data.
     */
    public void startOfData() {
        super.startOfData();
    }

    /**
     * This method is used to run the reconstruction and print debug information.
     */
    public void process(EventHeader event) {
        // This call runs the track reconstruction using the sub-Drivers.
        super.process(event);
        double maxChi2 = 250;
        List<HelicalTrackHit> hth = event.get(HelicalTrackHit.class, stInputCollectionName);

        System.out.println("The HelicalTrackHit collection " + hthOutputCollectionName + " has " + hth.size() + " hits.");
        List<HelicalTrackHit> hitsLayer1 = getLayerHits(hth, 1);
        List<HelicalTrackHit> hitsLayer3 = getLayerHits(hth, 3);
        List<HelicalTrackHit> hitsLayer5 = getLayerHits(hth, 5);
        List<HelicalTrackHit> hitsLayer7 = getLayerHits(hth, 7);
        List<HelicalTrackHit> hitsLayer9 = getLayerHits(hth, 9);

        List<SeedCandidate> seedtracks = new ArrayList<SeedCandidate>();
        for (HelicalTrackHit h1 : hitsLayer1) {
            for (HelicalTrackHit h3 : hitsLayer3) {
                for (HelicalTrackHit h5 : hitsLayer5) {
                    for (HelicalTrackHit h7 : hitsLayer7) {
                        // for (HelicalTrackHit h9 : hitsLayer9) {

                        SeedCandidate seed = new SeedCandidate(sFinallist.get(0), bfield);
                        seed.addHit(h1);
                        seed.addHit(h3);
                        seed.addHit(h5);
                        seed.addHit(h7);
                        // seed.addHit(h9);

                        HelicalTrackFit fitRes = fit(seed);
                        if (fitRes != null) {
                            seed.setHelix(fitRes);
                            if (fitRes.chisq()[1] > maxChi2)
                                continue;

                            seedtracks.add(seed);
                            // System.out.println(fitRes.toString());
                        }
                        // }
                    }
                }
            }
        }

        addTracksToEvent(event, seedtracks, bfield);
        // Debug printouts.
        if (debug) {
            // Check for HelicalTrackHits.

            // Check for Tracks.
            List<Track> tracks = event.get(Track.class, trackCollectionName);
            System.out.println("The Track collection " + trackCollectionName + " has " + tracks.size() + " tracks.");

            // Print out track info.
            for (Track track : tracks) {
                System.out.println(track.toString());
                System.out.println("chi2 = " + track.getChi2());
            }
        }

        // Increment number of events.
        ++nevents;

        // Add to tracks found.
        ntracks += event.get(Track.class, trackCollectionName).size();
    }

    public HelicalTrackFit fit(SeedCandidate seed) {
        List<HelicalTrackHit> hitcol = seed.getHits();
        Map<HelicalTrackHit, MultipleScatter> msmap = new HashMap<HelicalTrackHit, MultipleScatter>();
        SlopeInterceptLineFitter _lfitter = new SlopeInterceptLineFitter();
        SlopeInterceptLineFit _lfit;
        boolean success = false;
        // Check if we have enough pixel hits to do a straight-line fit of s vs z
        int npix = hitcol.size();
        // Calculate the arc lengths from the DCA to each hit and check for backwards hits
        Map<HelicalTrackHit, Double> smap = getPathLengths(hitcol);
        // Create the objects that will hold the fit output
        double[] chisq = new double[2];
        int[] ndof = new int[2];
        ndof[0] = 0;
        chisq[0] = 0;
        double[] par = new double[5];
        SymmetricMatrix cov = new SymmetricMatrix(5);

        // Setup for the line fit
        double[] s = new double[npix];
        double[] z = new double[npix];
        double[] dz = new double[npix];

        // Store the coordinates and errors for the line fit
        for (int i = 0; i < npix; i++) {
            HelicalTrackHit hit = hitcol.get(i);
            z[i] = hit.z();
            System.out.println(hit.getCorrectedCovMatrix().toString());
            dz[i] = Math.sqrt(hit.getCorrectedCovMatrix().e(2, 2));
            s[i] = smap.get(hit);
            System.out.println(z[i] + " " + dz[i] + " " + s[i]);
        }

        // Call the line fitter and check for success
        success = _lfitter.fit(s, z, dz, npix);
        if (!success) {
            return null;
        }

        // Save the line fit, chi^2, and DOF
        _lfit = _lfitter.getFit();
        chisq[1] = _lfit.chisquared();
        ndof[1] = npix - 2;

        // Save the line fit parameters
        par[HelicalTrackFit.z0Index] = _lfit.intercept();
        par[HelicalTrackFit.slopeIndex] = _lfit.slope();
        par[HelicalTrackFit.curvatureIndex] = 0.0001;
        par[HelicalTrackFit.dcaIndex] = 0.0001;
        par[HelicalTrackFit.phi0Index] = 0.000;

        // Save the line fit covariance matrix elements
        cov.setElement(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index, Math.pow(_lfit.interceptUncertainty(), 2));
        cov.setElement(HelicalTrackFit.z0Index, HelicalTrackFit.slopeIndex, _lfit.covariance());
        cov.setElement(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex, Math.pow(_lfit.slopeUncertainty(), 2));
        cov.setElement(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex, 0);
        cov.setElement(HelicalTrackFit.curvatureIndex, HelicalTrackFit.phi0Index, 0);
        cov.setElement(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index, 0);
        cov.setElement(HelicalTrackFit.curvatureIndex, HelicalTrackFit.dcaIndex, 0); // fix d0 sign
                                                                                     // convention
        cov.setElement(HelicalTrackFit.phi0Index, HelicalTrackFit.dcaIndex, 0); // fix d0 sign
                                                                                // convention
        cov.setElement(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex, 0);

        // Create the HelicalTrackFit for this helix
        return new HelicalTrackFit(par, cov, chisq, ndof, smap, msmap);

    }

    private Map<HelicalTrackHit, Double> getPathLengths(List<HelicalTrackHit> hits) {

        // Create a map to store the arc lengths
        Map<HelicalTrackHit, Double> smap = new HashMap<HelicalTrackHit, Double>();

        // Initialize looper tracking and iterate over ordered list of hits
        double slast = 0.;
        int ilast = -1;
        double s;
        for (int i = 0; i < hits.size(); i++) {

            // Retrieve the next hit ordered by z coordinate and check hit type
            HelicalTrackHit hit = hits.get(i);
            if (hit instanceof HelicalTrack2DHit) {

                // Axial hit - measure from the DCA (can't handle loopers)
                // s = HelixUtils.PathLength(_cfit, hit);
                s = hit.getPosition()[0];
                // Save the arc length for this hit
                smap.put(hit, s);
            }
        }
        return smap;
    }

    private List<HelicalTrackHit> getLayerHits(List<HelicalTrackHit> hth, int layer) {
        List<HelicalTrackHit> layerHits = new ArrayList<HelicalTrackHit>();

        for (HelicalTrackHit hit : hth) {
            if (hit.Layer() == layer)
                layerHits.add(hit);
        }
        return layerHits;
    }

    public void endOfData() {
        if (debug) {
            System.out.println("-------------------------------------------");
            System.out.println(this.getName() + " found " + ntracks + " tracks in " + nevents + " events which is " + ((double) ntracks / (double) nevents) + " tracks per event.");
        }
    }

    private void addTracksToEvent(EventHeader event, List<SeedCandidate> seedlist, double bfield) {
        // Create a the track list
        List<Track> tracks = new ArrayList<Track>();

        // Initialize the reference point to the origin
        double[] ref = new double[3];
        ref[0] = 0.;
        ref[1] = 0.;
        ref[2] = 0.;
        // Loop over the SeedCandidates that have survived
        for (SeedCandidate trackseed : seedlist) {

            // Create a new SeedTrack (SeedTrack extends BaseTrack)
            SeedTrack trk = new SeedTrack();

            // Add the hits to the track
            for (HelicalTrackHit hit : trackseed.getHits()) {
                trk.addHit((TrackerHit) hit);
            }

            // Retrieve the helix and save the relevant bits of helix info
            HelicalTrackFit helix = trackseed.getHelix();
            trk.setTrackParameters(helix.parameters(), bfield);
            trk.setCovarianceMatrix(helix.covariance());
            trk.setChisq(helix.chisqtot());
            trk.setNDF(helix.ndf()[0] + helix.ndf()[1]);

            // Flag that the fit was successful and set the reference point
            trk.setFitSuccess(true);
            trk.setReferencePoint(ref);
            trk.setRefPointIsDCA(true);

            // Set the strategy used to find this track
            trk.setStratetgy(trackseed.getSeedStrategy());

            // Set the SeedCandidate this track is based on
            trk.setSeedCandidate(trackseed);

            // Add the track to the list of tracks
            tracks.add((Track) trk);
        }

        // Put the tracks back into the event and exit
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(trackCollectionName, tracks, Track.class, flag);
    }
}
