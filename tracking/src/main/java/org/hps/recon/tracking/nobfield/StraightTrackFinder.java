package org.hps.recon.tracking.nobfield;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.HitCollectionUtilites;
import org.hps.recon.tracking.nobfield.TrackCollectionUtilities;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.line.SlopeInterceptLineFit;
import org.lcsim.fit.line.SlopeInterceptLineFitter;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author mgraham
 */
public class StraightTrackFinder extends Driver {

    // Debug flag.
    private boolean debug = true;
    // Tracks found across all events.
    int ntracks = 0;
    // Number of events processed.
    int nevents = 0;
    // Cache detector object.
    Detector detector = null;
    // Tracking strategies resource path.
    private String strategyResource = "HPS-Test-4pt1.xml";
    // Output track collection.
    private String trackCollectionName = "StraightTracks";
    // HelicalTrackHit input collection.
    private String stInputCollectionName = "HelicalTrackHits";
    // Include MS (done by removing XPlanes from the material manager results)
    private boolean includeMS = true;
    // number of repetitive fits on confirmed/extended tracks
    private int _iterativeConfirmed = 3;
    // use HPS implementation of material manager
    private boolean _useHPSMaterialManager = true;

    private TrackChecker checkerTrack = new TrackChecker();
    private HitOnTrackChecker checkerHOT = new HitOnTrackChecker();

    private SlopeInterceptLineFitter _lfitter = new SlopeInterceptLineFitter();

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

    @Override
    public void detectorChanged(Detector detector) {
        // Cache Detector object.
        this.detector = detector;
//        initialize();
        super.detectorChanged(detector);
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(HelicalTrackHit.class, stInputCollectionName))
            return;

        List<HelicalTrackHit> allHits = event.get(HelicalTrackHit.class, stInputCollectionName);

        List<List<HelicalTrackHit>> splitTopBot = HitCollectionUtilites.SplitTopBottomHits(allHits);
        // will always have top(=0) and bottom(=1) lists (though they may be empty)
        List<HelicalTrackHit> topHits = splitTopBot.get(0);
        List<HelicalTrackHit> bottomHits = splitTopBot.get(1);
        //a simple strategy...eventually implement SeedTracker strategies?
        int nTotLayers = 6;
        int nSeed = 3;
        int nExtra = nTotLayers - nSeed;
        int[] seedStrategy = {1, 3, 5};
        int[] extendStrategy = {7, 9, 11};
        int minHits = 4;

        TrackChecker checkerTrack = new TrackChecker();
        HitOnTrackChecker checkerHOT = new HitOnTrackChecker();

//        List<StraightTrack> seeds = getSeeds(seedStrategy, topHits);
        List<StraightTrack> seeds = getSeeds(seedStrategy, allHits);
        System.out.println("Found " + seeds.size() + " seeds");

        List<StraightTrack> extendedSeeds = new ArrayList<>();
        for (StraightTrack seed : seeds)
            extendTrack(extendStrategy, 0, seed, allHits, extendedSeeds);
//            extendTrack(extendStrategy, 0, seed, topHits, extendedSeeds);

        System.out.println("Prepruning  :Found " + extendedSeeds.size() + " extended seeds");

        //remove tracks with more than m overlaping hits...pick best chi2
        //...
        List<StraightTrack> finalTracks = new ArrayList<>();
        for (StraightTrack track : extendedSeeds) {
            boolean isbest = TrackCollectionUtilities.pruneTrackList((ArrayList<Track>) (ArrayList) extendedSeeds, track, 1);
            if (isbest)
                finalTracks.add(track);
        }

        System.out.println("Postpruning  :Found " + finalTracks.size() + " extended seeds");
        event.put(trackCollectionName, finalTracks);
    }

    public SlopeInterceptLineFit FitToLine(List<HelicalTrackHit> hits, int projection) {
        SlopeInterceptLineFit _lfit;
        int npix = hits.size();
        double[] s = new double[npix];
        double[] q = new double[npix];
        double[] dq = new double[npix];

        //  Store the coordinates and errors for the line fit
        for (int i = 0; i < npix; i++) {
            HelicalTrackHit hit = hits.get(i);
            s[i] = hit.z();//probably isn't quite right...track length is not z
            if (projection == 0) { //do x vs z;
                q[i] = hit.x();
                dq[i] = Math.sqrt(hit.getCorrectedCovMatrix().e(0, 0));
            } else {
                q[i] = hit.y();
                dq[i] = Math.sqrt(hit.getCorrectedCovMatrix().e(1, 1));
            }
        }

        //  Call the line fitter and check for success
        boolean success = _lfitter.fit(s, q, dq, npix);

        if (!success)
            System.out.println("Something is broken in the line fit");
        //  Save the line fit, chi^2, and DOF
        _lfit = _lfitter.getFit();
        return _lfit;

    }

    private StraightTrack makeTrack(List<HelicalTrackHit> hits, SlopeInterceptLineFit xfit, SlopeInterceptLineFit yfit) {
        StraightTrack track = new StraightTrack();
        double[] pars = {-99, -99, -99, -99, -99};//this needs to have 5 fields to implement Track
        pars[0] = xfit.intercept();
        pars[1] = xfit.slope();
        pars[2] = yfit.intercept();
        pars[3] = yfit.slope();
        track.setTrackParameters(pars);
        track.setChi2(xfit.chisquared(), yfit.chisquared());
        track.setNDF(xfit.ndf()+yfit.ndf());
        for (TrackerHit hit : hits)
            track.addHit(hit);        
        // TODO:  set convariance, 
        return track;
    }

    private StraightTrack makeTrack(List<HelicalTrackHit> hits) {
        SlopeInterceptLineFit xfit = FitToLine(hits, 0);
        SlopeInterceptLineFit yfit = FitToLine(hits, 1);
        if (debug)
            System.out.println("xfit = " + xfit.toString());
        if (debug)
            System.out.println("yfit = " + yfit.toString());        
        return makeTrack(hits, xfit, yfit);
    }

    /*
     *   Get all seed combinations that make sense (pass checkSeed)
     *   currently, just assume there are 3 seed layers (don't have to be first 3 though.  
     */
    private List<StraightTrack> getSeeds(int[] seedLayers, List<HelicalTrackHit> hits) {
        List<StraightTrack> seeds = new ArrayList<>();
        int nseeds = seedLayers.length;
        if (nseeds == 3)//TODO ... set this up so that this works for arbitrary nseeds...use recursion
            for (HelicalTrackHit h1 : HitCollectionUtilites.GetSortedHits(hits, seedLayers[0])) {
                if (debug)
                    System.out.println(h1.toString());
                for (HelicalTrackHit h2 : HitCollectionUtilites.GetSortedHits(hits, seedLayers[1])) {
                    if (debug)
                        System.out.println(h2.toString());
                    for (HelicalTrackHit h3 : HitCollectionUtilites.GetSortedHits(hits, seedLayers[2])) {
                        if (debug)
                            System.out.println(h3.toString());
                        //make a 3-hit test track...see if it passes CheckTrack 
                        List<HelicalTrackHit> testTrack = new ArrayList<HelicalTrackHit>();
                        testTrack.add(h1);
                        testTrack.add(h2);
                        testTrack.add(h3);                       
                        StraightTrack trk = makeTrack(testTrack);
                        if (!checkerTrack.checkSeed(trk))
                            break;
                        seeds.add(trk);
                    }
                }
            }
        return seeds;
    }

    /*
     * recursively extend the seeds through all of the extend layers..
     * ...I think this should work...
     */
    private void extendTrack(int[] extendLayers, int n, StraightTrack origTrack, List<HelicalTrackHit> hits, List<StraightTrack> trackList) {
        if (n >= extendLayers.length) {
            if (debug)
                System.out.println("Done finding this track through all " + n + " extra layers");
            trackList.add(origTrack);
            return;
        }

        boolean cannotExtendThisLayer = true;
        if (debug)
            System.out.println("Extending to layer " + extendLayers[n]);
        for (HelicalTrackHit h : HitCollectionUtilites.GetSortedHits(hits, extendLayers[n])) {
            //let's see if this hit makes sense to add to original track
            if (!checkerHOT.checkNewHit(origTrack, h))
                continue;

            List<TrackerHit> origHits = origTrack.getTrackerHits();
            //make a new list and cast them as HelicalTrackHits (Track only stores TrackerHits)
            List<HelicalTrackHit> newHits = new ArrayList<>();
            for (TrackerHit oh : origHits) {
                HelicalTrackHit hoh = (HelicalTrackHit) oh;
                System.out.println(hoh.getPosition()[0]);
                newHits.add(hoh);
            }
            //add the new hit to the list & make new track
            newHits.add(h);
            StraightTrack newTrack = makeTrack(newHits);
            //check the new track after we've added this hit
            if (!checkerTrack.checkTrack(newTrack))
                continue;
            cannotExtendThisLayer = false;
            //extend again to the next layer
            extendTrack(extendLayers, n + 1, newTrack, hits, trackList);
        }

        //didn't find any hits in this layer that match the track...but let's try the next one
        if (cannotExtendThisLayer)
            extendTrack(extendLayers, n + 1, origTrack, hits, trackList);

        return;
    }

}
