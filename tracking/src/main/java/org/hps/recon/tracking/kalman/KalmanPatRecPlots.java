package org.hps.recon.tracking.kalman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.recon.tracking.TrackUtils;
import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.MatrixFeatures_DDRM;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.matrix.EigenvalueDecomposition;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.hps.util.Pair;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.physics.vec.Hep3Vector;

/**
 * Histograms and plots for Kalman Filter pattern recognition development
 */
class KalmanPatRecPlots {
    private KalmanInterface KI;
    private AIDA aida;
    private int nPlotted;
    private int nEvents;
    private boolean verbose;
    private String trackCollectionName = "GBLTracks";
    private org.lcsim.geometry.FieldMap fm;
    private IDDecoder decoder;
    private String outputFileName = "KalmanTestPlots.root";
    private String outputGnuPlotDir = "./";
    private RelationalTable hitToStrips;
    private RelationalTable hitToRotated;
    private int numEvtPlots;
    private IHistogram1D hp, hnh;
    private IHistogram1D hpf, hnhf;
    private IHistogramFactory hf;
    int nMCtracks;
    int nMCtracksFound;
    private Efficiency pEff;
    private Logger logger;
    private int numBadCov;
    private String ecalClusterCollectionName = "EcalClusters";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    KalmanPatRecPlots(boolean verbose, KalmanInterface KI, IDDecoder decoder, int numEvtPlots, org.lcsim.geometry.FieldMap fm) {
        this.verbose = verbose;
        this.KI = KI;
        this.decoder = decoder;
        this.fm = fm;
        this.numEvtPlots = numEvtPlots;
        logger = Logger.getLogger(KalmanPatRecPlots.class.getName());
        
        if (aida == null) aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        nPlotted = 0;
        nEvents = 0;
        nMCtracks = 0;
        nMCtracksFound = 0;
        numBadCov = 0;
        
        // arguments to histogram1D: name, nbins, min, max
        aida.histogram1D("Kalman number of tracks", 10, 0., 10.);
        aida.histogram1D("Kalman Track Chi2", 100, 0., 200.);
        aida.histogram1D("Kalman Track Chi2, >=12 hits", 100, 0., 200.);
        aida.histogram1D("Kalman Track simple Chi2, >=12 hits", 50, 0., 100.);
        aida.histogram2D("number tracks Kalman vs GBL", 20, 0., 5., 20, 0., 5.);
        aida.histogram1D("helix chi-squared at origin", 100, 0., 25.);
        aida.histogram1D("GBL track chi^2", 50, 0., 100.);
        aida.histogram1D("GBL >=12-hit track chi^2", 50, 0., 100.);
        aida.histogram1D("Kalman Track Number Hits", 20, 0., 20.);
        aida.histogram1D("GBL number tracks", 10, 0., 10.);
        aida.histogram1D("Kalman missed hit residual", 100, -1.0, 1.0);
        aida.histogram1D("Kalman track hit residual, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track hit residual >= 10 hits, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track hit residual", 100, -0.1, 0.1);
        aida.histogram1D("Kalman hit true error", 100, -0.2, 0.2);
        aida.histogram1D("Kalman hit true error over uncertainty", 100, -5., 5.);
        aida.histogram1D("Kalman track Momentum 11-hit", 120, 0., 6.);
        aida.histogram1D("Kalman track Momentum 12-hit", 120, 0., 6.);
        aida.histogram1D("Kalman track Momentum 13-hit", 120, 0., 6.);
        aida.histogram1D("Kalman track Momentum 14-hit", 120, 0., 6.);
        aida.histogram1D("Vertex constrained Kalman track Momentum 14-hit", 120, 0., 6.);
        aida.histogram1D("GBL momentum, >= 12 hits", 100, 0., 5.);
        aida.histogram1D("dRho", 100, -5., 5.);
        aida.histogram1D("dRho error, sigmas", 100, -5., 5.);
        aida.histogram1D("z0", 100, -2., 2.);
        aida.histogram1D("z0 error, sigmas", 100, -5., 5.);
        aida.histogram1D("pt inverse", 200, -1.5, 1.5);
        aida.histogram1D("pt inverse True", 200, -1.5, 1.5);
        aida.histogram1D("pt inverse error, percent", 100, -50., 50.);
        aida.histogram1D("pt inverse error, sigmas", 100, -5., 5.);
        aida.histogram1D("tanLambda", 100, -0.3, 0.3);
        aida.histogram1D("GBL tanLambda", 100, -0.3, 0.3);
        aida.histogram1D("tanLambda true", 100, -0.3, 0.3);
        aida.histogram1D("tanLambda error, sigmas", 100, -5., 5.);
        aida.histogram1D("phi0 true", 100, -0.3, 0.3);
        aida.histogram1D("phi0", 100, -0.3, 0.3);
        aida.histogram1D("phi0 error, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track drho",100,-5.,5.);
        aida.histogram1D("Kalman track dz",100,-2.,2.);
        aida.histogram1D("Kalman track drho, 14-hit",100,-5.,5.);
        aida.histogram1D("Kalman track dz, 14-hit",100,-2.,2.);
        aida.histogram1D("Vertex constrained Kalman track drho, 14-hit",100,-5.,5.);
        aida.histogram1D("Vertex constrained Kalman track dz, 14-hit",100,-2.,2.);
        aida.histogram1D("Kalman track number MC particles",10,0.,10.);
        aida.histogram1D("Kalman number of wrong hits on track",12,0.,12.);
        aida.histogram1D("Kalman number of wrong hits on track, >= 10 hits", 12, 0., 12.);
        aida.histogram1D("GBL track number MC particles",10,0.,10.);
        aida.histogram1D("Kalman arc length to first measurement",100,0.,200.);
        aida.histogram1D("GBL number of wrong hits on track",12,0.,12.);
        aida.histogram1D("MC hit z in local system (should be zero)", 50, -2., 2.);
        aida.histogram1D("Kalman track number of shared hits", 10, 0., 10.);
        aida.histogram1D("GBL d0", 100, -5., 5.);
        aida.histogram1D("GBL z0", 100, -2., 2.);
        aida.histogram1D("GBL pt inverse", 200, -1.5, 1.5);
        aida.histogram1D("GBL pt inverse, sigmas", 100, -5., 5.);
        aida.histogram1D("Kalman track time range (ns)", 100, 0., 100.);
        aida.histogram1D("GBL number of hits",20,0.,20.);
        aida.histogram1D("Kalman layer hit",20,0.,20.);
        aida.histogram1D("drho error estimate",50,0.,1.);
        aida.histogram1D("phi0 error estimate",50,0.,.01);
        aida.histogram1D("ptInv error estimate",50,0.,0.2);
        aida.histogram1D("ptInv relative error estimate",50,0.,0.5);
        aida.histogram1D("dz error estimate",50,0.,0.2);
        aida.histogram1D("tanl error estimate",50,0.,.005);
        for (int lyr=0; lyr<14; ++lyr) {
            aida.histogram1D(String.format("Layers/Kalman missed hit residual in layer %d",lyr), 100, -1.0, 1.0);
            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d",lyr), 100, -0.1, 0.1);
            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d, sigmas",lyr), 100, -5., 5.);
            aida.histogram1D(String.format("Layers/Kalman track unbiased hit residual in layer %d",lyr), 100, -0.1, 0.1);
            aida.histogram1D(String.format("Layers/Kalman track unbiased hit residual in layer %d, sigmas",lyr), 100, -5., 5.);
            aida.histogram1D(String.format("Layers/Kalman true error in layer %d",lyr), 100, -0.2, 0.2);
            aida.histogram1D(String.format("Layers/Kalman layer %d chi^2 contribution", lyr), 100, 0., 20.);
            if (lyr<13) {
                aida.histogram1D(String.format("Layers/Kalman kink in xy, layer %d", lyr),100, -0.001, .001);
                aida.histogram1D(String.format("Layers/Kalman kink in zy, layer %d", lyr),100, -0.0025, .0025);
            }
        }
        aida.histogram1D("projected track-state x error", 100, -25., 25.);
        aida.histogram1D("projected track-state y error", 100, -25., 25.);
        aida.histogram1D("projected track-state x uncertainty",100,0.,5.);
        aida.histogram1D("projected track-state y uncertainty",100,0.,5.);
        aida.histogram1D("projected track-state x error, sigmas", 100, -10., 10.);
        aida.histogram1D("projected track-state y error, sigmas", 100, -10., 10.);
        aida.histogram1D("Kalman projected track-state x error", 100, -25., 25.);
        aida.histogram1D("Kalman projected track-state z error", 100, -25., 25.);
        hf = aida.histogramFactory();
        hp = aida.histogram1D("MC particle momentum",40,0.,4.);
        hpf = aida.histogram1D("MC particle momentum, found",40,0.,4.);
        hnh = aida.histogram1D("MC number hits",15,0.,15.);
        hnhf = aida.histogram1D("MC number hits, found",15,0.,15.);
        pEff = new Efficiency(40,0.,0.1,"Track efficency vs momentum","momentum (GeV)","efficiency");
        aida.histogram1D("Bad/Number of hits on bad tracks", 20, 0., 20.);
        aida.histogram1D("Bad/Chi-squared of bad tracks", 100, 0., 200.);
        aida.histogram1D("Bad/drho of bad tracks", 50, -8., 8.);
        aida.histogram1D("Bad/dz of bad tracks", 50, -4., 4.);
        aida.histogram1D("Bad/momentum of bad tracks", 60, 0., 6.);
        aida.histogram1D("Bad/Number of MC particles associated", 10, 0., 10.);
        aida.histogram1D("Bad/Number of wrong hits on track", 20, 0., 20.);
        aida.histogram1D("seed slope",100,0.,0.3);
        aida.histogram1D("seed z intercept",100,0.,10.);
        aida.histogram1D("seed y intercept",100,-100.,100.);
    }
    
    void process(EventHeader event, List<HpsSiSensor> sensors, ArrayList<KalTrack>[] kPatList, 
            List<Track> outputFullTracks) {
        
        nEvents++;
        
        setupSensors(event);
        
        // Set up relatations between raw hits and MC hits
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
        }
        
        // Analysis of tracker hits (noise hits versus real hits)
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);

        // Map the fitted hits to their corresponding raw hits
        Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();

        List<RawTrackerHit> rawHitsList = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
        Map<String, Integer> hits = new HashMap<String, Integer>();
        for (HpsSiSensor sensor : sensors) {
            hits.put(sensor.getName(), 0);
        }
        
        // Map the raw hits to the Kalman tracks and to TrackerHit
        Map<TrackerHit, KalTrack> hitToTk = new HashMap<TrackerHit, KalTrack>();
        for (int topBottom=0; topBottom<2; ++topBottom) {
            for (KalTrack kTk : kPatList[topBottom]) {
                for (MeasurementSite site : kTk.SiteList) {
                    if (site.hitID < 0) continue;
                    StateVector aS = site.aS;
                    SiModule mod = site.m;
                    if (aS != null && mod != null) {
                        TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                        hitToTk.put(hpsHit, kTk);
                        List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                        double totalPh = 0.;
                        HpsSiSensor sensor = null;
                        int Layer = -1;
                        int nstrip = 0;
                        double chan = 0.;
                        for (RawTrackerHit rawHit : rawHits) { 
                            double amplitude = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rawHit));
                            totalPh += amplitude;
                            nstrip++;
                            chan += rawHit.getIdentifierFieldValue("strip");
                            if (sensor == null) {
                                sensor = (HpsSiSensor) rawHit.getDetectorElement();
                                Layer = sensor.getLayerNumber();
                            }
                        }
                        chan = chan/(double)nstrip;
                        aida.histogram1D("hits/" + sensor.getName() + " average channel with track", 640, 0, 640).fill(chan);
                        aida.histogram1D("hits/dEdx of hits on tracks",250,0.,20.).fill(hpsHit.getdEdx()*1000000.);
                        aida.histogram1D("hits/Layer " + Integer.toString(Layer) + " cluster amplitude of hit on track", 250, 0., 5000.).fill(totalPh);
                        aida.histogram1D("hits/cluster amplitude of hit on track", 250, 0., 5000.).fill(totalPh);
                    }
                }
            }
        }
        Map<RawTrackerHit, TrackerHit> hitToTkHit = new HashMap<RawTrackerHit, TrackerHit>();
        List<TrackerHit> stripHits = event.get(TrackerHit.class, stripHitInputCollectionName);
        if (stripHits != null) {
            for (TrackerHit tkHit : stripHits) {
                KalTrack kTk = hitToTk.get(tkHit);
                List<RawTrackerHit> rawHits = tkHit.getRawHits();
                double totalPh = 0.;
                HpsSiSensor sensor = null;
                int Layer = -1;
                int nstrip = 0;
                double chan = 0.;
                for (RawTrackerHit rawHit : rawHits) { 
                    hitToTkHit.put(rawHit, tkHit);
                    double amplitude = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rawHit));
                    totalPh += amplitude;
                    nstrip++;
                    chan += rawHit.getIdentifierFieldValue("strip");
                    if (sensor == null) {
                        sensor = (HpsSiSensor) rawHit.getDetectorElement();
                        Layer = sensor.getLayerNumber();
                    }
                }
                if (kTk == null) {
                    chan = chan/(double)nstrip;
                    aida.histogram1D("hits/" + sensor.getName() + " average channel with no track", 640, 0, 640).fill(chan);
                    aida.histogram1D("hits/Layer " + Integer.toString(Layer) + " cluster amplitude with no track", 250, 0., 5000.).fill(totalPh);
                    aida.histogram1D("hits/" + sensor.getName() + " cluster amplitude with no track", 250, 0., 5000.).fill(totalPh);
                    aida.histogram1D("hits/cluster amplitude with no track", 250, 0., 5000.).fill(totalPh);
                }
            }
        }
        // Look for raw strip hits that were not associated with a tracker hit
        for (RawTrackerHit rawHit : rawHitsList) {
            // Access the sensor associated with the raw hit
            HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
            int Layer = sensor.getLayerNumber();
            aida.histogram1D("hits/layer number", 20,0.,20.).fill(Layer);
            Integer nHits = hits.get(sensor.getName());
            if (nHits == null) {
                nHits = 0;
            }
            nHits++;
            hits.put(rawHit.getDetectorElement().getName(), nHits);
            TrackerHit hpsHit = hitToTkHit.get(rawHit);
            if (hpsHit != null) continue;
            
            double amplitude = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rawHit));
            aida.histogram1D("hits/" + sensor.getName() + " strip amplitude with no tracker hit", 250, 0., 5000.).fill(amplitude);
            aida.histogram1D("hits/strip amplitude with no tracker hit", 250, 0., 5000.).fill(amplitude);
            int channel = rawHit.getIdentifierFieldValue("strip");
            aida.histogram1D("hits/" + sensor.getName() + " channel with no tracker hit", 640, 0, 640).fill(channel);
            double t0 = FittedRawTrackerHit.getT0(fittedRawTrackerHitMap.get(rawHit));
            aida.histogram1D("hits/" + sensor.getName() + " t0 with no tracker hit", 250, -400., 100.).fill(t0);
        }

        
        // Tracker ECAL matching analysis
        if (event.hasCollection(Cluster.class, ecalClusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, ecalClusterCollectionName);
            for (Track tkr : outputFullTracks) {            
                if (tkr.getTrackerHits().size() < 12) continue;
                if (tkr.getChi2()/tkr.getNDF() > 2.) continue;
                List<TrackState> tkStates = tkr.getTrackStates();
                TrackState lastState = null;
                for (TrackState tkState : tkStates) {
                    if (tkState.getLocation() == TrackState.AtLastHit) {
                        lastState = tkState;
                        break;
                    }
                }
                if (lastState != null) {
                    // test propagation of a track state from one end of the track to the ECAL
                    for (Cluster cluster : clusters) {
                        double [] location = cluster.getPosition();
                        double [] direction = new double[3];
                        direction[0] = 0.; direction[1] = 0.; direction[2] = 1.;
                        PropagatedTrackState pts = KI.propagateTrackState(lastState, location, direction);
                        double [] intPnt = pts.getIntersection();
                        double [][] intPntCov = pts.getIntersectionCov();
                        //new Vec(3,location).print("ECAL cluster position");
                        //pts.print("to ECAL");
                        aida.histogram1D("projected track-state x error").fill(intPnt[0] - location[0]);
                        aida.histogram1D("projected track-state x uncertainty").fill(Math.sqrt(intPntCov[0][0]));
                        aida.histogram1D("projected track-state x error, sigmas").fill((intPnt[0] - location[0])/Math.sqrt(intPntCov[0][0]));
                        aida.histogram1D("projected track-state y error").fill(intPnt[1] - location[1]);
                        aida.histogram1D("projected track-state y uncertainty").fill(Math.sqrt(intPntCov[1][1]));
                        aida.histogram1D("projected track-state y error, sigmas").fill((intPnt[1] - location[1])/Math.sqrt(intPntCov[1][1]));                        
                    }
                }
            }
            for (int topBottom=0; topBottom<2; ++topBottom) {
                for (KalTrack tkr : kPatList[topBottom]) {
                    if (tkr.nHits < 12) continue;
                    if (tkr.chi2/tkr.nHits > 2.) continue;
                    MeasurementSite lastSite = tkr.SiteList.get(tkr.SiteList.size()-1);
                    for (Cluster cluster : clusters) {
                        Vec eCalPos = KalmanInterface.vectorGlbToKalman(cluster.getPosition());
                        Plane plnAtEcal = new Plane(eCalPos, new Vec(0.,1.,0.));
                        ArrayList<Double> yScat = new ArrayList<Double>();
                        ArrayList<Double> XLscat = new ArrayList<Double>();
                        //eCalPos.print("ECAL cluster position");
                        //lastSite.aS.helix.print("helix at last layer");
                        double [] arcLength = new double[1];
                        HelixState helixAtEcal = lastSite.aS.helix.propagateRungeKutta(plnAtEcal, yScat, XLscat, fm, arcLength);
                        if (MatrixFeatures_DDRM.hasNaN(helixAtEcal.C)) continue;
                        Vec intPnt = helixAtEcal.getRKintersection();
                        //helixAtEcal.print("helix at ECAL cluster");
                        //intPnt.print("RK intersection point");
                        aida.histogram1D("Kalman projected track-state x error").fill(intPnt.v[0] - eCalPos.v[0]);
                        aida.histogram1D("Kalman projected track-state z error").fill(intPnt.v[2] - eCalPos.v[2]);
                    }
                }
            }
        }
        
        boolean doGBL = true;
        if (doGBL) {
            hitToStrips = TrackUtils.getHitToStripsTable(event);
            hitToRotated = TrackUtils.getHitToRotatedTable(event);
        }
        
        int minHits = 999;
        int nKalTracks = 0;
        boolean sharedHitTrack = false;
        for (int topBottom=0; topBottom<2; ++topBottom) {
            for (KalTrack kTk : kPatList[topBottom]) {
                nKalTracks++;
                aida.histogram1D("Kalman Track Number Hits").fill(kTk.nHits);
                if (kTk.nHits < minHits) minHits = kTk.nHits;
                
                // Vertex constraint
                double [] vtx = {0.1735, -3.168, 0.1687};
                double [][] vtxCov = {{0.686, 0., 0.}, {0., 10.09, 0.}, {0., 0., 0.017}};
                HelixState constrained = kTk.originConstraint(vtx, vtxCov);
                if (constrained == null) continue;
                double pConstrained = constrained.getMom(0.).mag(); 
                if (kTk.nHits >= 12) {
                    aida.histogram1D("Kalman Track Chi2, >=12 hits").fill(kTk.chi2);
                    aida.histogram1D("Kalman Track simple Chi2, >=12 hits").fill(kTk.chi2prime());
                }
                aida.histogram1D("Kalman Track Chi2").fill(kTk.chi2);
                aida.histogram1D("Kalman arc length to first measurement").fill(kTk.originArcLength());
                double[] momentum = kTk.originP();
                double pMag = Math.sqrt(momentum[0]*momentum[0]+momentum[1]*momentum[1]+momentum[2]*momentum[2]);
                switch (kTk.nHits) {
                    case 11:
                        aida.histogram1D("Kalman track Momentum 11-hit").fill(pMag);
                        break;
                    case 12:
                        aida.histogram1D("Kalman track Momentum 12-hit").fill(pMag);
                        break;
                    case 13:
                        aida.histogram1D("Kalman track Momentum 13-hit").fill(pMag);
                        break;
                    case 14:
                        aida.histogram1D("Kalman track Momentum 14-hit").fill(pMag);
                        aida.histogram1D("Vertex constrained Kalman track Momentum 14-hit").fill(pConstrained);
                        aida.histogram1D("Kalman track drho, 14-hit").fill(kTk.originHelixParms()[0]);
                        aida.histogram1D("Kalman track dz, 14-hit").fill(kTk.originHelixParms()[3]);
                        aida.histogram1D("Vertex constrained Kalman track drho, 14-hit").fill(constrained.a.v[0]);
                        aida.histogram1D("Vertex constrained Kalman track dz, 14-hit").fill(constrained.a.v[3]);
                }               
                aida.histogram1D("Kalman track drho").fill(kTk.originHelixParms()[0]);
                aida.histogram1D("Kalman track dz").fill(kTk.originHelixParms()[3]);
                aida.histogram1D("Kalman track time range (ns)").fill(kTk.tMax - kTk.tMin);
                
                // Use good tracks to analyze seed cuts
                if (kTk.nHits >=10) {
                    if (kTk.chi2 < 15.) {
                        for (MeasurementSite site : kTk.SiteList) {
                            if (site.m.isStereo) continue;
                            if (site.hitID < 0) continue;
                            Measurement hit1 = site.m.hits.get(site.hitID);
                            double z1 = site.m.toGlobal(new Vec(0.,hit1.v,0.)).v[2];
                            double y1 = site.m.p.X().v[1];
                            for (MeasurementSite site2 : kTk.SiteList) {
                                if (site2.m.isStereo) continue;
                                if (site.m.Layer == site2.m.Layer) continue;
                                if (site2.hitID < 0) continue;
                                Measurement hit2 = site2.m.hits.get(site2.hitID);
                                double z2 = site2.m.toGlobal(new Vec(0.,hit2.v,0.)).v[2];
                                double y2 = site2.m.p.X().v[1];   
                                double slope = (z2 - z1) / (y2 - y1);
                                double zIntercept = z1 - slope * y1;
                                double yIntercept = -(zIntercept/slope);
                                aida.histogram1D("seed slope").fill(Math.abs(slope));
                                aida.histogram1D("seed z intercept").fill(Math.abs(zIntercept));
                                aida.histogram1D("seed y intercept").fill(yIntercept);
                            }
                        }
                    }
                }

                // Check the covariance matrix
                boolean badCov = false;
                Matrix C = new Matrix(kTk.originCovariance());
                EigenvalueDecomposition eED= new EigenvalueDecomposition(C);
                double [] e = eED.getRealEigenvalues();
                for (int i=0; i<5; ++i) {
                    if (e[i] < 0.) {
                        logger.warning(String.format("Event %d, eigenvalue %d of covariance is negative!", event.getEventNumber(), i));
                        System.out.format("Event %d, eigenvalue %d of covariance is negative for track %d!", event.getEventNumber(), i, kTk.ID);
                        badCov = true;
                    }
                }
                if (badCov || kTk.bad) {
                    aida.histogram1D("Bad/Number of hits on bad tracks").fill(kTk.nHits);
                    aida.histogram1D("Bad/Chi-squared of bad tracks").fill(kTk.chi2);
                    aida.histogram1D("Bad/drho of bad tracks").fill(kTk.originHelixParms()[0]);
                    aida.histogram1D("Bad/dz of bad tracks").fill(kTk.originHelixParms()[3]);
                    aida.histogram1D("Bad/momentum of bad tracks").fill(pMag);
                }
                if (kTk.nHits >= 10) {
                    DMatrixRMaj thisCov = kTk.SiteList.get(0).aS.helix.C;
                    double ptInv1 = kTk.SiteList.get(0).aS.helix.a.v[2];
                    aida.histogram1D("drho error estimate").fill(Math.sqrt(thisCov.unsafe_get(0, 0)));
                    aida.histogram1D("phi0 error estimate").fill(Math.sqrt(thisCov.unsafe_get(1, 1)));
                    aida.histogram1D("ptInv relative error estimate").fill(Math.sqrt(thisCov.unsafe_get(2, 2))/Math.abs(ptInv1));
                    aida.histogram1D("ptInv error estimate").fill(Math.sqrt(thisCov.unsafe_get(2, 2)));
                    aida.histogram1D("dz error estimate").fill(Math.sqrt(thisCov.unsafe_get(3, 3)));
                    aida.histogram1D("tanl error estimate").fill(Math.sqrt(thisCov.unsafe_get(4, 4)));
                }
                
                // Histogram residuals of hits in layers with no hits on the track and with hits
                ArrayList<MCParticle> mcParts = new ArrayList<MCParticle>();
                ArrayList<Integer> mcCnt= new ArrayList<Integer>();
                int nShared = 0;
                for (MeasurementSite site : kTk.SiteList) {
                    if (verbose) site.print(String.format("track %d", kTk.ID));
                    StateVector aS = site.aS;
                    SiModule mod = site.m;
                    if (aS != null && mod != null) {
                        double [] rGbl = null;
                        double hitV = kTk.moduleIntercept(mod, rGbl)[1];
                        if (site.hitID < 0) {
                            double minResid = 9.9e9;
                            for (Measurement m : mod.hits) {
                                double resid = m.v - hitV;
                                if (resid < minResid) minResid = resid;                                   
                            } 
                            if (kTk.nHits >= 10 && Math.abs(minResid) < 1.0) {
                                aida.histogram1D("Kalman missed hit residual").fill(minResid);
                                aida.histogram1D(String.format("Layers/Kalman missed hit residual in layer %d",mod.Layer)).fill(minResid);
                            }
                        } else {
                            if (site.hitID > mod.hits.size()-1) { // This should never happen!!
                                logger.warning(String.format("Event %d, hit missing in layer %d detector %d\n",event.getEventNumber(),mod.Layer,mod.detector));
                                //site.print("the bad site");
                                //mod.print("the bad module");
                                continue;
                            }
                            if (mod.hits.get(site.hitID).tracks.size() > 1) {
                                nShared++;
                                sharedHitTrack = true;
                            }
                            aida.histogram1D("Kalman layer hit").fill(mod.Layer);
                            double resid = mod.hits.get(site.hitID).v - hitV;
                            if (kTk.nHits >= 10) aida.histogram1D("Kalman track hit residual >= 10 hits, sigmas").fill(resid/Math.sqrt(site.aS.R));
                            aida.histogram1D("Kalman track hit residual").fill(resid);
                            aida.histogram1D("Kalman track hit residual, sigmas").fill(resid/Math.sqrt(site.aS.R));
                            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d",mod.Layer)).fill(resid);
                            aida.histogram1D(String.format("Layers/Kalman track hit residual in layer %d, sigmas",mod.Layer)).fill(resid/Math.sqrt(site.aS.R));
                            aida.histogram1D(String.format("Layers/Kalman layer %d chi^2 contribution", mod.Layer)).fill(site.chi2inc);
                            if (mod.Layer<13) {
                                aida.histogram1D(String.format("Layers/Kalman kink in xy, layer %d", mod.Layer)).fill(kTk.scatX(mod.Layer));
                                aida.histogram1D(String.format("Layers/Kalman kink in zy, layer %d", mod.Layer)).fill(kTk.scatZ(mod.Layer));
                            }      
                            Pair<Double, Double> residPr = kTk.unbiasedResidual(site.m.Layer);
                            if (residPr.getSecondElement() > -999. && kTk.nHits >= 10) {
                                double variance = residPr.getSecondElement();
                                if (variance <= 0.) {
                                    numBadCov++;
                                //    System.out.format("Event %d layer %d, unbiased residual variance < 0: %10.5f, chi2=%9.2f, hits=%d, resid=%9.6f\n", 
                                //                        event.getEventNumber(), site.m.Layer, variance, kTk.chi2, kTk.nHits, unbResid);
                                } else {
                                    double sigma = Math.sqrt(variance);
                                    double unbResid = residPr.getFirstElement();
                                    aida.histogram1D(String.format("Layers/Kalman track unbiased hit residual in layer %d",site.m.Layer)).fill(unbResid);
                                    aida.histogram1D(String.format("Layers/Kalman track unbiased hit residual in layer %d, sigmas",site.m.Layer)).fill(unbResid/sigma);
                                }
                            }
                            TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                            List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                            for (RawTrackerHit rawHit : rawHits) {
                                Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                                for (SimTrackerHit simHit : simHits) {
                                    MCParticle mcp = simHit.getMCParticle();
                                    if (mcParts.contains(mcp)) {
                                        int id = mcParts.indexOf(mcp);
                                        mcCnt.set(id, mcCnt.get(id)+1);
                                    } else {
                                        mcParts.add(mcp);
                                        mcCnt.add(1);
                                    }
                                }
                            }                               
                        }
                    }
                }
                aida.histogram1D("Kalman track number of shared hits").fill(nShared);
                aida.histogram1D("Kalman track number MC particles").fill(mcParts.size());

                // Which MC particle is the best match?
                int idBest = -1;
                int nMatch = 0;
                for (int id=0; id<mcCnt.size(); ++id) {
                    if (mcCnt.get(id) > nMatch) {
                        nMatch = mcCnt.get(id);
                        idBest = id;
                    }
                }
                int nBad = 0;
                for (MeasurementSite site : kTk.SiteList) {
                    if (site.hitID < 0) {
                        continue;
                    }
                    SiModule mod = site.m;
                    TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                    List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                    boolean goodHit = false;
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            int id = mcParts.indexOf(mcp);
                            if (id == idBest) {
                                goodHit = true;
                                break;
                            }
                        }
                    }
                    if (!goodHit) nBad++;
                }
                aida.histogram1D("Kalman number of wrong hits on track").fill(nBad);
                if (badCov || kTk.bad) {
                    aida.histogram1D("Bad/Number of MC particles associated").fill(mcParts.size());
                    aida.histogram1D("Bad/Number of wrong hits on track").fill(nBad);
                }
            
                if (kTk.nHits >= 10) aida.histogram1D("Kalman number of wrong hits on track, >= 10 hits").fill(nBad);
                MCParticle mcBest = null;
                double [] hParams = kTk.originHelixParms();
                double dRho = hParams[0];
                double phi0 = -hParams[1];
                double ptInv = hParams[2];
                double z0 = -hParams[3];
                double tanLambda = -hParams[4];
                aida.histogram1D("dRho").fill(dRho);
                aida.histogram1D("z0").fill(z0);
                aida.histogram1D("phi0").fill(phi0);
                aida.histogram1D("pt inverse").fill(ptInv);
                aida.histogram1D("tanLambda").fill(tanLambda);
                if (idBest > -1) {
                    mcBest = mcParts.get(idBest); 
                    Hep3Vector pVec = mcBest.getMomentum();
                    Hep3Vector rVec = mcBest.getOrigin();
                    double ptTrue = Math.sqrt(pVec.x()*pVec.x() + pVec.z()*pVec.z());
                    double ptInvTrue = mcBest.getCharge()/ptTrue;
                    //double [] pKal = kTk.originP();
                    double tanLambdaTrue = pVec.y()/ptTrue;
                    double phi0True = Math.atan2(pVec.x(), pVec.z());
                    double phi0Err = kTk.helixErr(1);
                    double ptInvErr = kTk.helixErr(2);
                    double tanLambdaErr = kTk.helixErr(4);
                    double z0True = rVec.y();
                    double z0Err = kTk.helixErr(3);
                    double dRhoTrue = -Math.sqrt(rVec.y()*rVec.y()+rVec.x()*rVec.x());  // How to get the sign correct here in general?
                    double dRhoErr = kTk.helixErr(0);
                    Vec apTrue = new Vec(0.,-phi0True,ptInvTrue,-z0True,-tanLambdaTrue);
                    Vec ap = new Vec(5,hParams);
                    SquareMatrix Cov = new SquareMatrix(5,kTk.originCovariance());
                    SquareMatrix CovInv = Cov.invert();
                    Vec helixDiff = ap.dif(apTrue);
                    double chi2Helix = helixDiff.dot(helixDiff.leftMultiply(CovInv));
                    if (kTk.nHits >= 10 && kTk.chi2/(double)kTk.nHits < 2.0) {
                        aida.histogram1D("helix chi-squared at origin").fill(chi2Helix);
                        aida.histogram1D("dRho error, sigmas").fill((dRho-dRhoTrue)/dRhoErr);
                        aida.histogram1D("z0 error, sigmas").fill((z0-z0True)/z0Err);
                        aida.histogram1D("phi0 true").fill(phi0True);
                        aida.histogram1D("phi0 error, sigmas").fill((phi0-phi0True)/phi0Err);
                        aida.histogram1D("pt inverse True").fill(ptInvTrue);                        
                        aida.histogram1D("pt inverse error, percent").fill(100.*(ptInv-ptInvTrue)/ptInvTrue);
                        aida.histogram1D("pt inverse error, sigmas").fill((ptInv-ptInvTrue)/ptInvErr);    
                        aida.histogram1D("tanLambda true").fill(tanLambdaTrue);
                        aida.histogram1D("tanLambda error, sigmas").fill((tanLambda - tanLambdaTrue)/tanLambdaErr);
                    }
                }
            }  // Loop over Kalman tracks
        } // Loop over SVT trackers (top/bottom)
        
        aida.histogram1D("Kalman number of tracks").fill(nKalTracks);
        
        // Tracking efficiency analysis
        // Form MC "tracks" from collections of sim hits
        String MCHitInputCollectionName = "TrackerHits";
        if (event.hasCollection(SimTrackerHit.class, MCHitInputCollectionName)) {
            List<SimTrackerHit> striphits = event.get(SimTrackerHit.class, MCHitInputCollectionName);
            List<TrackerHit> reconHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
            
            // Make a mapping from sim hits to recon hits
            Map<SimTrackerHit, TrackerHit> hitTohitMap = new HashMap<SimTrackerHit, TrackerHit>();
            for (TrackerHit hpsHit : reconHits) {
                List<RawTrackerHit> rawHits = hpsHit.getRawHits();
                for (RawTrackerHit rawHit : rawHits) {
                    Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                    for (SimTrackerHit simHit : simHits) {
                        hitTohitMap.put(simHit, hpsHit);
                    }
                }
            }
            
            // Make a mapping from MCparticle to recon hits (assume 1 sim hit cannot contribute to more than one recon hit)
            Map<MCParticle, Set<TrackerHit>> hitMcpMap = new HashMap<MCParticle, Set<TrackerHit>>();
            Set<MCParticle> mcParticles = new HashSet<MCParticle>();
            for (SimTrackerHit hit1D : striphits) {
                if (!hitTohitMap.containsKey(hit1D)) continue;
                MCParticle mCP = hit1D.getMCParticle();
                mcParticles.add(mCP);
                Set<TrackerHit> hitsOnMcp = null;
                if (hitMcpMap.containsKey(mCP)) {
                    hitsOnMcp = hitMcpMap.get(mCP);
                } else {
                    hitsOnMcp = new HashSet<TrackerHit>();
                }
                hitsOnMcp.add(hitTohitMap.get(hit1D));
                hitMcpMap.put(mCP, hitsOnMcp);
            }
                   
            // Make a list of recon hits for each Kalman track
            Map<KalTrack, Set<TrackerHit>> hitKalMap = new HashMap<KalTrack, Set<TrackerHit>>(nKalTracks);
            for (int topBottom=0; topBottom<2; ++topBottom) {
                for (KalTrack kTk : kPatList[topBottom]) {
                    Set<TrackerHit> hitsOnTk = new HashSet<TrackerHit>();
                    for (MeasurementSite site : kTk.SiteList) {
                        SiModule mod = site.m;
                        if (site.hitID < 0) {
                            continue;
                        }
                        TrackerHit hpsHit = KI.getHpsHit(mod.hits.get(site.hitID));
                        hitsOnTk.add(hpsHit);
                    }
                    hitKalMap.put(kTk, hitsOnTk);
                }
            }
            if (verbose) {
                System.out.format("KalmanPatRecPlots: MC track vs Kaltrack matching for event %d\n", event.getEventNumber());
                for (int topBottom=0; topBottom<2; ++topBottom) {
                    for (KalTrack kTk : kPatList[topBottom]) {
                        System.out.format("  Kaltrack %d with %d hits: [", kTk.ID, kTk.nHits);
                        for (TrackerHit hpsHt : hitKalMap.get(kTk)) {
                            int ID = reconHits.indexOf(hpsHt);
                            System.out.format("%d,", ID);
                        }
                        System.out.format("]\n");
                    }           
                }
            }
            for (MCParticle mCP : mcParticles) {
                Set<TrackerHit> mcHitList = hitMcpMap.get(mCP);
                if (verbose) {
                    System.out.format("  MC particle of type %d, Q=%6.1f, p=%8.2f: [", mCP.getPDGID(), mCP.getCharge(), mCP.getMomentum().magnitude());
                    for (TrackerHit hpsHt : mcHitList) {
                        int ID = reconHits.indexOf(hpsHt);
                        System.out.format("%d,", ID);
                    }
                    System.out.format("]\n");
                }
                int nHits = Math.min(mcHitList.size(), 12);
                if (nHits < 6) continue;          
                KalTrack tkBest = null;
                int nMost = 0;
                for (int topBottom=0; topBottom<2; ++topBottom) {
                    for (KalTrack kTk : kPatList[topBottom]) {
                        Set<TrackerHit> kalHitList = hitKalMap.get(kTk);
                        Set<TrackerHit> intersection = new HashSet<TrackerHit>(mcHitList);
                        intersection.retainAll(kalHitList);
                        if (verbose) {
                            System.out.format("      Intersection with track %d: [", kTk.ID);
                            for (TrackerHit hpsHt : intersection) {
                                int ID = reconHits.indexOf(hpsHt);
                                System.out.format("%d,", ID);
                            }
                            System.out.format("]\n");
                        }
                        if (intersection.size() > nMost) {
                            nMost = intersection.size();
                            tkBest = kTk;
                        }
                    }               
                }
                if (verbose) {
                    System.out.format(" MC match to KalTrack=%b\n",tkBest != null);
                    if (tkBest != null) System.out.format("        The best track is %d with %d matching hits\n",tkBest.ID,nMost);
                }
                double fracFnd = (double)nMost/(double)nHits;
                boolean success = (nMost >= 6 && fracFnd > 0.5 && nMost >= tkBest.nHits-2);
                Hep3Vector p = mCP.getMomentum();
                pEff.entry(p.magnitude(), success);
                hp.fill(p.magnitude());            
                if (success) hpf.fill(p.magnitude());
                if (p.magnitude() > 0.7) {
                    if (nHits >= 10) {
                        nMCtracks++;
                        if (success) nMCtracksFound++;
                    }
                    hnh.fill(nHits);
                    if (success) hnhf.fill(nHits);
                }
            }
        }
        
        // Analysis of helix+GBL tracks, for comparison
        int nGBL = 0;
        if (doGBL && event.hasCollection(Track.class, trackCollectionName)) {
            List<Track> tracksGBL = event.get(Track.class, trackCollectionName);
            nGBL = tracksGBL.size();
            aida.histogram2D("number tracks Kalman vs GBL").fill(nKalTracks, nGBL);
            aida.histogram1D("GBL number tracks").fill(nGBL);
            double c = 2.99793e8; // Speed of light in m/s
            double conFac = 1.0e12 / c;
            Vec Bfield = KalmanInterface.getField(new Vec(0.,505.57,0.), fm); // Field at the instrument center
            double B = Bfield.mag();
            double alpha = conFac / B; // Convert from pt in GeV to curvature in mm
            for (Track tkrGBL : tracksGBL) {
                aida.histogram1D("GBL track chi^2").fill(tkrGBL.getChi2());
                ArrayList<MCParticle> mcParts = new ArrayList<MCParticle>();
                ArrayList<Integer> mcCnt= new ArrayList<Integer>();
                List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(tkrGBL, hitToStrips, hitToRotated);
                int nGBLhits = hitsOnTrack.size();
                if (nGBLhits >= 12) aida.histogram1D("GBL >=12-hit track chi^2").fill(tkrGBL.getChi2());
                aida.histogram1D("GBL number of hits").fill(nGBLhits);
                for (TrackerHit hit1D : hitsOnTrack) {
                    List<RawTrackerHit> rawHits = hit1D.getRawHits();
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            if (mcParts.contains(mcp)) {
                                int id = mcParts.indexOf(mcp);
                                mcCnt.set(id, mcCnt.get(id)+1);
                            } else {
                                mcParts.add(mcp);
                                mcCnt.add(1);
                            }
                        }//simHits
                    }//rawHits               
                }//hitsOnTrack
                aida.histogram1D("GBL track number MC particles").fill(mcParts.size());
                // Which MC particle is the best match?
                int idBest = -1;
                int nMatch = 0;
                for (int id=0; id<mcCnt.size(); ++id) {
                    if (mcCnt.get(id) > nMatch) {
                        nMatch = mcCnt.get(id);
                        idBest = id;
                    }
                }
                int nBad = 0;
                for (TrackerHit hit1D : hitsOnTrack) {
                    List<RawTrackerHit> rawHits = hit1D.getRawHits();
                    boolean goodHit = false;
                    for (RawTrackerHit rawHit : rawHits) {
                        Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                        for (SimTrackerHit simHit : simHits) {
                            MCParticle mcp = simHit.getMCParticle();
                            int id = mcParts.indexOf(mcp);
                            if (id == idBest) {
                                goodHit = true;
                                break;
                            }                          
                        }
                    }  
                    if (!goodHit) nBad++;
                }
                aida.histogram1D("GBL number of wrong hits on track").fill(nBad);
                MCParticle mcBest = null;
                double ptInvTrue = 1.;
                if (idBest > -1) {
                    mcBest = mcParts.get(idBest); 
                    Hep3Vector pVec = mcBest.getMomentum();
                    //Hep3Vector rVec = mcBest.getOrigin();
                    double ptTrue = Math.sqrt(pVec.x()*pVec.x() + pVec.z()*pVec.z());
                    ptInvTrue = mcBest.getCharge()/ptTrue;
                }
                List<TrackState> stLst = tkrGBL.getTrackStates();
                for (TrackState st : stLst) {
                    if (st.getLocation() == TrackState.AtIP) {
                        double d0 = st.getParameter(0);
                        aida.histogram1D("GBL d0").fill(d0);
                        double z0 = st.getParameter(3);
                        aida.histogram1D("GBL z0").fill(z0);
                        double Omega = st.getOmega();
                        double ptInvGBL = -alpha * Omega;
                        aida.histogram1D("GBL pt inverse").fill(ptInvGBL);
                        double [] covGBL = st.getCovMatrix();
                        double ptInvErr = -alpha * Math.sqrt(covGBL[5]);
                        double tanLambdaGBL = st.getTanLambda();
                        aida.histogram1D("GBL tanLambda").fill(tanLambdaGBL);
                        if (mcBest != null) {
                            aida.histogram1D("GBL pt inverse, sigmas").fill((ptInvGBL-ptInvTrue)/ptInvErr);
                        }
                        double pMag = Math.sqrt(1.0+tanLambdaGBL*tanLambdaGBL)/Math.abs(ptInvGBL);
                        if (nGBLhits >= 12) aida.histogram1D("GBL momentum, >= 12 hits").fill(pMag);
                        //System.out.format("d0=%10.5f +- %10.5f\n", d0, Math.sqrt(covGBL[0]));
                        //System.out.format("phi0=%10.5f +- %10.5f\n", st.getParameter(1), Math.sqrt(covGBL[2]));
                        //System.out.format("omega=%10.5f +- %10.5f\n", Omega, omegaErr);
                        //System.out.format("z0=%10.5f +- %10.5f\n", z0, Math.sqrt(covGBL[9]));
                        //System.out.format("tanL=%10.5f +- %10.5f\n", st.getParameter(4), Math.sqrt(covGBL[14]));
                        break;
                    } // Track State at IP
                }//loop on track states
            } //loop on GBL Tracks
        } //check if event has GBLTracks
        
        //int [] badEvents = {51753, 52531, 56183, 57958, 58050, 60199, 80324, 83798, 84933, 86351, 88796, 96749, 97230, 102986, 105578, 106654, 
        //        107191, 108542, 108886, 110453, 120457, 121129, 121311, 121525, 124355, 124910, 127335, 129360, 133951};
        //int [] badEvents = {8788317, 8730045, 8724483, 8716465, 8697779, 8696553, 8565879, 8566151, 8563109, 196192, 155656, 144460, 135200,
        //        110480, 69464, 20665, 9000031, 8882086, 8878414, 8750999};
        int [] badEvents = {};
        if (nPlotted < numEvtPlots) { // && sharedHitTrack) {
            boolean plotIt = false;
            if (badEvents.length > 0) {
                for (int i=0; i<badEvents.length; ++i) {
                    if (event.getEventNumber() == badEvents[i]) {
                        plotIt = true;
                        break;
                    }
                } 
            } else {
                plotIt = true;
            }
            if (plotIt) {
                KI.plotKalmanEvent(outputGnuPlotDir, event, kPatList);
                //KI.plotGBLtracks(outputGnuPlotDir, event);
                nPlotted++;
                KI.printAllTracks("GBLTracks", event);
                //KI.printAllTracks("KalmanFullTracks", event);
                for (int topBottom=0; topBottom<2; ++topBottom) {
                    for (KalTrack kTk : kPatList[topBottom]) {
                        kTk.printRawHits(KI);
                    }
                }
            }
        }
        
        simHitRes(event);
    }
    
    // Make histograms of the MC hit resolution
    private double simHitRes(EventHeader event) {
        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) return 999.;
        List<TrackerHit> stripHits = event.get(TrackerHit.class, stripHitInputCollectionName);
        
        if (stripHits == null) return 999.;

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        for (TrackerHit hit1D : stripHits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement();

            ArrayList<TrackerHit> hitsInSensor = null;
            if (hitSensorMap.containsKey(sensor)) {
                hitsInSensor = hitSensorMap.get(sensor);
            } else {
                hitsInSensor = new ArrayList<TrackerHit>();
            }
            hitsInSensor.add(hit1D);
            hitSensorMap.put(sensor, hitsInSensor);
        }
        
        String MCHitInputCollectionName = "TrackerHits";
        if (!event.hasCollection(SimTrackerHit.class, MCHitInputCollectionName)) return 999.;
        List<SimTrackerHit> MChits = event.get(SimTrackerHit.class, MCHitInputCollectionName);
        
        if (MChits == null) return 999.;

        // Make a map from MC particle to the hit in the layer
        Map<Integer, ArrayList<SimTrackerHit>> hitMCparticleMap = new HashMap<Integer, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : MChits) {
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;  // Kalman layers go from 0 to 13, with 0 and 1 for new 2019 modules
            //int Module = decoder.getValue("module");
            //MCParticle MCpart = hit1D.getMCParticle();
            ArrayList<SimTrackerHit> partInLayer = null;
            if (hitMCparticleMap.containsKey(Layer)) {
                partInLayer = hitMCparticleMap.get(Layer);
            } else {
                partInLayer = new ArrayList<SimTrackerHit>();
            }
            partInLayer.add(hit1D);
            hitMCparticleMap.put(Layer,partInLayer);
        }
        //System.out.format("KalmanPatRecDriver.simHitRes: found both hit collections in event %d\n", event.getEventNumber());
        
        double maxErr = 0.;
        ArrayList<SiModule> SiMlist = KI.getSiModuleList();
        Map<SiModule, SiStripPlane> moduleMap = KI.getModuleMap();
        for (SiModule siMod : SiMlist) {
            int layer = siMod.Layer;
            if (hitMCparticleMap.get(layer) == null) continue;
            SiStripPlane plane = moduleMap.get(siMod);
            if (!hitSensorMap.containsKey(plane.getSensor())) continue;
            ArrayList<TrackerHit> hitsInSensor = hitSensorMap.get(plane.getSensor());
            if (hitsInSensor == null) continue;
            for (TrackerHit hit : hitsInSensor) {
                //System.out.format("simHitRes: Hit in sensor of layer %d, detector %d\n",layer,siMod.detector);
                //Vec tkrHit = new Vec(3,hit.getPosition());
                //tkrHit.print("Tracker hit position");
                //SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
                SiTrackerHitStrip1D localHit = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                double du = Math.sqrt(localHit.getCovarianceAsMatrix().diagonal(0));
                //Vec globalHPS = new Vec(3,global.getPosition());
                //Vec localHPS = new Vec(3,localHit.getPosition());
                //globalHPS.print("simulated hit in HPS global system");
                //localHPS.print("simulated hit in HPS local system");
                
                //Vec hitGlobal = KalmanInterface.vectorGlbToKalman(global.getPosition());
                Vec hitLocal = new Vec(-localHit.getPosition()[1], localHit.getPosition()[0], localHit.getPosition()[2]);
                
                //hitGlobal.print("simulated hit Kal global position");
                //hitLocal.print("simulated hit Kal local position");
                //Vec kalGlobal = siMod.toGlobal(hitLocal);
                //kalGlobal.print("simulated hit global position from Kalman transformation");
                for (SimTrackerHit hitMC : hitMCparticleMap.get(layer)) {
                    Vec hitMCglobal = KalmanInterface.vectorGlbToKalman(hitMC.getPosition());
                    //hitMCglobal.print("true hit Kal global position");
                    Vec kalMClocal = siMod.toLocal(hitMCglobal);
                    //kalMClocal.print("true hit Kal local position");
                    //hitLocal.print("sim hit Kal local position");
                    double hitError = hitLocal.v[1] - kalMClocal.v[1];
                    if (Math.abs(hitError) > maxErr) maxErr = Math.abs(hitError);
                    aida.histogram1D("Kalman hit true error").fill(hitError);
                    aida.histogram1D("Kalman hit true error over uncertainty").fill(hitError/du);
                    aida.histogram1D(String.format("Layers/Kalman true error in layer %d",layer)).fill(hitError);
                    aida.histogram1D("MC hit z in local system (should be zero)").fill(kalMClocal.v[2]);
                }
            }
        }
        return maxErr;
    }
    
    void output() {
        pEff.plot("./effVSp.gp", false, "errors", " ");
        hf.divide("Kalman track efficiency vs momentum", hpf, hp);
        hf.divide("Kalman track efficiency vs number hits", hnhf, hnh);
        double trackEfficiency = (double)nMCtracksFound/(double)nMCtracks;
        int nMiss = nMCtracks - nMCtracksFound;
        double err = Math.sqrt((double)nMiss) / (double)nMCtracks;
        System.out.format("KalmanPatRecPlots: the track efficiency for p>0.7 GeV and >= 10 sim hits is %9.3f+-%9.3f\n", trackEfficiency, err);
        System.out.format("KalmanPatRecPlots: the total number of hits with negative unbiased residual predicted variance=%d\n", numBadCov);
        try {
            System.out.format("Outputting the aida histograms now for %d events to file %s\n", nEvents, outputFileName);
            aida.saveAs(outputFileName);
        } catch (IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }
    
}

