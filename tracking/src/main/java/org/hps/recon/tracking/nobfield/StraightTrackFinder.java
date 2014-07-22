package org.hps.recon.tracking.nobfield;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.tracking.HitCollectionUtilites;
import org.lcsim.event.EventHeader;
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
    private boolean debug = false;
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
    // enable the use of sectoring using sector binning in SeedTracker
    private boolean _applySectorBinning = true;

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

    /**
     * This is used to setup the Drivers after XML config.
     */
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
        if (allHits.size() == 0)
            return;
        List<List<HelicalTrackHit>> splitTopBot = HitCollectionUtilites.SplitTopBottomHits(allHits);
        // will always have top(=0) and bottom(=1) lists (though they may be empty)
        List<HelicalTrackHit> topHits = splitTopBot.get(0);
        List<HelicalTrackHit> bottomHits = splitTopBot.get(1);
        //a simple strategy...eventually implement SeedTracker strategies
        int nTotLayers = 6;
        int[] layerStrategy = {1, 3, 5, 7, 9, 11};
        int minHits = 4;

        List<StraightTrack> trackList = new ArrayList<>();
//sort the hits for some reason
//        List<List<HelicalTrackHit>> sortedTopHits=new ArrayList<>();
//         List<List<HelicalTrackHit>> sortedBottomHits=new ArrayList<>();
//        for(int i = 0;i<nTotLayers;i++){
//            List<HelicalTrackHit> sortedTop=HitCollectionUtilites.GetSortedHits(topHits,layerStrategy[i]);
//            sortedTopHits.add(sortedTop);
//            List<HelicalTrackHit> sortedBot=HitCollectionUtilites.GetSortedHits(bottomHits,layerStrategy[i]);
//            sortedBottomHits.add(sortedBot);                      
//        }
//        
        if (topHits.size() < 4)
            return;
        //first do top...
        for (HelicalTrackHit h1 : HitCollectionUtilites.GetSortedHits(topHits, layerStrategy[0])) {
            if (debug)
                System.out.println(h1.toString());
            for (HelicalTrackHit h2 : HitCollectionUtilites.GetSortedHits(topHits, layerStrategy[1])) {
                if (debug)
                    System.out.println(h2.toString());
                for (HelicalTrackHit h3 : HitCollectionUtilites.GetSortedHits(topHits, layerStrategy[2])) {
                    if (debug)
                        System.out.println(h3.toString());
                    for (HelicalTrackHit h4 : HitCollectionUtilites.GetSortedHits(topHits, layerStrategy[3])) {
                        if (debug)
                            System.out.println(h4.toString());
                        for (HelicalTrackHit h5 : HitCollectionUtilites.GetSortedHits(topHits, layerStrategy[4])) {
                            if (debug)
                                System.out.println(h5.toString());
                            //  Setup for the line fit
                            List<HelicalTrackHit> testTrack = new ArrayList<HelicalTrackHit>();
                            testTrack.add(h1);
                            testTrack.add(h2);
                            testTrack.add(h3);
                            testTrack.add(h4);
                            testTrack.add(h5);
                            SlopeInterceptLineFit xfit = FitToLine(testTrack, 0);
                            SlopeInterceptLineFit yfit = FitToLine(testTrack, 1);
                            if (debug)
                                System.out.println("xfit = " + xfit.toString());
                            if (debug)
                                System.out.println("yfit = " + yfit.toString());
                            StraightTrack trk = makeTrack(xfit, yfit);
                            trackList.add(trk);
                        }
                    }
                }
            }
        }

        event.put(trackCollectionName, trackList);
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

    private StraightTrack makeTrack(SlopeInterceptLineFit xfit, SlopeInterceptLineFit yfit) {
        StraightTrack track = new StraightTrack();
        double[] pars = {-99, -99, -99, -99};
        pars[0] = xfit.intercept();
        pars[1] = xfit.slope();
        pars[2] = yfit.intercept();
        pars[3] = yfit.slope();
        track.setTrackParameters(pars);
        track.setChi2(xfit.chisquared(), yfit.chisquared());
        // TODO:  set convariance, 
        return track;
    }

}
