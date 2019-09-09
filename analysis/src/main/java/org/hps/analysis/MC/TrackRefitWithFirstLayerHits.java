package org.hps.analysis.MC;


import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.hps.util.Pair;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
//import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.fit.helicaltrack.HitIdentifier;
import org.lcsim.fit.helicaltrack.StereoHitMaker;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.seedtracker.SeedCandidate;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.recon.tracking.seedtracker.StrategyXMLUtils;
import org.lcsim.util.Driver;

/**
 *
 * @author mrsolt
 * This driver keeps all truth hits that correspond to a track's 1D strip hits
 */
public class TrackRefitWithFirstLayerHits extends Driver {

    //Collection Names
    //private String V0CollectionName = "UnconstrainedV0Candidates";
    private String trackCollectionName = "GBLTracks";
    private String helicalTrackHitCollectionName = "HelicalTrackHits";
    private String helicalTrackHitRefitCollectionName = "HelicalTrackHits_refit";
    private String trackToFirstLayHitsRelationCollectionName = "TrackToFirstLayerHitsRelations";
    private String trackToTrackRefitListRelationCollectionName = "TrackToTrackRefitRelations";
    //private String v0ToV0RefitListRelationCollectionName = "V0ToV0RefitRelations";
    private String helicalTrackHitToStripHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String helicalTrackHitRefitToStripHitRelationsCollectionName = "HelicalTrackHitRelations_refit";
    private String refitTracksCollectionName = "Tracks_refit";
    
    protected String _outname = "HelicalTrackHits_refit";
    protected String _hitrelname = "HelicalTrackHitRelations_refit";
    protected StereoHitMaker _crosser = new StereoHitMaker(2., 10.);
    
    protected Hep3Vector _orgloc = new BasicHep3Vector(0., 0., 0.);
    protected HitIdentifier _ID = new HitIdentifier();
    //Beam Energy
    double bfield;
    
    public void detectorChanged(Detector detector){
        //Set B Field
        bfield = TrackUtils.getBField(detector).magnitude();
    }
    
    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    public void setHelicalTrackHitCollectionName(String helicalTrackHitCollectionName) {
        this.helicalTrackHitCollectionName = helicalTrackHitCollectionName;
    }
    
    public void setTrackToFirstLayHitsRelationCollectionName(String trackToFirstLayHitsRelationCollectionName) {
        this.trackToFirstLayHitsRelationCollectionName = trackToFirstLayHitsRelationCollectionName;
    }
    
    protected void process(EventHeader event) {
        //List<ReconstructedParticle> uncV0s = event.get(ReconstructedParticle.class, V0CollectionName);
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        List<TrackerHit> hits = event.get(TrackerHit.class,helicalTrackHitCollectionName);
        List<LCRelation> helicalTrackHitToStripHitRelations = event.get(LCRelation.class,helicalTrackHitToStripHitRelationsCollectionName);
        List<LCRelation> trackToFirstLayHitsRelationList = new ArrayList<LCRelation>();
        List<Track> refitTracks = new ArrayList<Track>();
        List<LCRelation> trackToTrackRefitList = new ArrayList<LCRelation>();
        //List<LCRelation> v0ToV0RefitList = new ArrayList<LCRelation>();
        List<LCRelation> hthToStripHitRefitList = new ArrayList<LCRelation>();
        List<HelicalTrackHit> helicalTrackHits_refit = new ArrayList<HelicalTrackHit>();
        
        for(Track track:tracks){
            int firstLayHit = (((HpsSiSensor) ((RawTrackerHit) track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement()).getLayerNumber() + 1) / 2;
            List<TrackerHit> newHits = new ArrayList<TrackerHit>();
            for(TrackerHit hit:hits){
                int layer = (((HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement()).getLayerNumber() + 1) / 2;
                if(layer != firstLayHit)
                    continue;
                if(hit.getPosition()[1]*track.getTrackStates().get(0).getTanLambda() < 0)
                    continue;
                boolean sameHit = checkSameHit(hit,track.getTrackerHits().get(0));
                if(sameHit)
                    continue;
                newHits.add(hit);
                trackToFirstLayHitsRelationList.add(new BaseLCRelation(track, hit));
            }
            for(TrackerHit hit:newHits){
                Pair<Track,Pair<List<LCRelation>,List<HelicalTrackHit>>> newtrackPair = fitNewTrack(track,hit,helicalTrackHitToStripHitRelations); //track with new hits
                Track newtrack = newtrackPair.getFirstElement();
                refitTracks.add(newtrack);
                hthToStripHitRefitList.addAll(newtrackPair.getSecondElement().getFirstElement());
                helicalTrackHits_refit.addAll(newtrackPair.getSecondElement().getSecondElement());
                trackToTrackRefitList.add(new BaseLCRelation(track,newtrack));
            }
            
        }
        event.put(trackToFirstLayHitsRelationCollectionName, trackToFirstLayHitsRelationList, LCRelation.class, 0);
        event.put(refitTracksCollectionName, refitTracks, Track.class, 0);
        event.put(trackToTrackRefitListRelationCollectionName, trackToTrackRefitList, LCRelation.class, 0);
        event.put(helicalTrackHitRefitCollectionName, helicalTrackHits_refit, HelicalTrackHit.class, 0);
        event.put(helicalTrackHitRefitToStripHitRelationsCollectionName, hthToStripHitRefitList, LCRelation.class, 0);
        addRotatedHitsToEvent(event, helicalTrackHits_refit); 
        
        /*for(ReconstructedParticle v0:uncV0s){
            List<Track> v0Tracks = v0.getTracks();
            for(Track track:v0Tracks){
                for(LCRelation rel:trackToTrackRefitList){
                    List<Track> newTracks = new ArrayList<Track>();
                    if(rel.getFrom().equals(track)){
                        Track newTrack = (Track) rel.getTo();
                        newTracks.add(newTrack);
                    }
                }
            }
            ReconstructedParticle newV0 = null; //V0 with new tracks;
            v0ToV0RefitList.add(new BaseLCRelation(v0,newV0));
        }
        event.put(v0ToV0RefitListRelationCollectionName, v0ToV0RefitList, LCRelation.class, 0);*/
    }

    private boolean checkSameHit(TrackerHit hit, TrackerHit trackerHit) {
        List<RawTrackerHit> rawhits = hit.getRawHits();
        List<RawTrackerHit> rawtrackerhits = trackerHit.getRawHits();
        if(rawhits.size() != rawtrackerhits.size())
            return false;
        
        //return rawhits.get(0).equals(rawtrackerhits.get(0));
        boolean[] isSameRawHit = new boolean[rawhits.size()];
        boolean[] isSameRawTrackerHit = new boolean[rawtrackerhits.size()];
        
        for(int i = 0; i < rawhits.size(); i++){
            isSameRawHit[i] = false;
        }
        for(int i = 0; i < rawtrackerhits.size(); i++){
            isSameRawTrackerHit[i] = false;
        }
        
        int i = 0;
        for(RawTrackerHit rawhit:rawhits){
            int j = 0;
            for(RawTrackerHit rawtrackerhit:rawtrackerhits){
                if(rawhit.equals(rawtrackerhit)){
                    isSameRawHit[i] = true;
                    isSameRawTrackerHit[j] = true;
                    break;
                }
                j++;
            }
            i++;
        }
        for(int j = 0; j < isSameRawHit.length; j++){
            if(!isSameRawHit[j])
                return false;
        }
        for(int j = 0; j < isSameRawTrackerHit.length; j++){
            if(!isSameRawTrackerHit[j])
                return false;
        }
        return true;
    }
    
    private Pair<Track,Pair<List<LCRelation>,List<HelicalTrackHit>>> fitNewTrack(Track track, TrackerHit hitL1, List<LCRelation> helicalTrackHitToStripHitRelations){
        String[] strategyResources = new String[4];
        strategyResources[0] = "/org/hps/recon/tracking/strategies/HPS_s345_c2_e16.xml";
        strategyResources[1] = "/org/hps/recon/tracking/strategies/HPS_s456_c3_e21.xml";
        strategyResources[2] = "/org/hps/recon/tracking/strategies/HPS_s123_c4_e56.xml";
        strategyResources[3] = "/org/hps/recon/tracking/strategies/HPS_s123_c5_e46.xml";
        
        List<SeedStrategy> sFinallist = StrategyXMLUtils.getStrategyListFromInputStream(this.getClass().getResourceAsStream(strategyResources[0]));
        List<LCRelation> hthToStripHitRefitList = new ArrayList<LCRelation>();
        List<HelicalTrackHit> helicalHits = new ArrayList<HelicalTrackHit>();

        HelicalTrackStrip strip1 = null;
        HelicalTrackStrip strip2 = null;
        TrackerHit hit1 = null;
        TrackerHit hit2 = null;
        HelicalTrackHit hth = null;
        
        for(LCRelation rel:helicalTrackHitToStripHitRelations){
            if(((TrackerHit) rel.getFrom()).equals(hitL1)){
                HelicalTrackStrip strip = makeDigiStrip(new SiTrackerHitStrip1D((TrackerHit) rel.getTo()));
                if(strip1 == null){
                    strip1 = strip;
                    hit1 = (TrackerHit) rel.getTo();
                }
                else{
                    strip2 = strip;
                    hit2 = (TrackerHit) rel.getTo();
                    break;
                }
            }
        }
        
        if(strip1 != null && strip2 != null){
            HelicalTrackCross trackCross = new HelicalTrackCross(strip1,strip2);
            hth = (HelicalTrackHit) trackCross;
            helicalHits.add(hth);
        }
        
        List<TrackerHit> hits = track.getTrackerHits();
        
        for(TrackerHit hit:hits){
            if(hit.equals(hits.get(0))){
                hthToStripHitRefitList.add(new BaseLCRelation(hth,hit1));
                hthToStripHitRefitList.add(new BaseLCRelation(hth,hit2));
                continue;
            }
            strip1 = null;
            strip2 = null;
            for(LCRelation rel:helicalTrackHitToStripHitRelations){
                if(checkSameHit((TrackerHit) rel.getFrom(),hit)){
                    HelicalTrackStrip strip = makeDigiStrip(new SiTrackerHitStrip1D((TrackerHit) rel.getTo()));
                    if(strip1 == null){
                        strip1 = strip;
                        hit1 = (TrackerHit) rel.getTo();
                    }
                    else{
                        strip2 = strip;
                        hit2 = (TrackerHit) rel.getTo();
                        break;
                    }
                }
            }
            if(strip1 != null && strip2 != null){
                HelicalTrackCross trackCross = new HelicalTrackCross(strip1,strip2);
                hth = (HelicalTrackHit) trackCross;
                helicalHits.add(hth);
                hthToStripHitRefitList.add(new BaseLCRelation(hth,hit1));
                hthToStripHitRefitList.add(new BaseLCRelation(hth,hit2));
            }
        }
        
        SeedCandidate trackseed = new SeedCandidate(helicalHits, sFinallist.get(0), TrackUtils.getHTF(track.getTrackStates().get(0)), bfield);
        
        //  Initialize the reference point to the origin
        double[] ref = new double[] {0., 0., 0.};
        
            //  Create a new SeedTrack (SeedTrack extends BaseTrack)
        SeedTrack trk = new SeedTrack();
            
            //  Add the hits to the track
        for (HelicalTrackHit hit : trackseed.getHits()) {
            trk.addHit((TrackerHit) hit);
        }
            
            //  Retrieve the helix and save the relevant bits of helix info
        HelicalTrackFit helix = trackseed.getHelix();
        trk.setTrackParameters(helix.parameters(), bfield); // Sets first TrackState.
        trk.setCovarianceMatrix(helix.covariance()); // Modifies first TrackState.
        trk.setChisq(helix.chisqtot());
        trk.setNDF(helix.ndf()[0]+helix.ndf()[1]);
            
            //  Flag that the fit was successful and set the reference point
        trk.setFitSuccess(true);
        trk.setReferencePoint(ref); // Modifies first TrackState.
        trk.setRefPointIsDCA(true);
            
            //  Set the strategy used to find this track
        trk.setStratetgy(trackseed.getSeedStrategy());
            
            //  Set the SeedCandidate this track is based on
        trk.setSeedCandidate(trackseed);
        
        return new Pair<>((Track) trk,new Pair<>(hthToStripHitRefitList,helicalHits));
    }
    
    private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {

        SiTrackerHitStrip1D local = h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(_orgloc);
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();

        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

        IDetectorElement de = h.getSensor();
        String det = _ID.getName(de);
        int lyr = _ID.getLayer(de);
        BarrelEndcapFlag be = _ID.getBarrelEndcapFlag(de);

        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        HelicalTrackStrip strip = new HelicalTrackStrip(org, u, v, umeas, du, vmin, vmax, dEdx, time, rawhits, det, lyr, be);

        return strip;
    }
    
    private void addRotatedHitsToEvent(EventHeader event, List<HelicalTrackHit> helicalHits) {

        List<HelicalTrackHit> rotatedhits = new ArrayList<HelicalTrackHit>();
        List<LCRelation> hthrelations = new ArrayList<LCRelation>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        List<HelicalTrackCross> stereohits = new ArrayList<HelicalTrackCross>();
        for(HelicalTrackHit hit:helicalHits){
            stereohits.add((HelicalTrackCross) hit);
        }
        
        for (HelicalTrackCross cross : stereohits) {
            List<HelicalTrackStrip> rotatedstriphits = new ArrayList<HelicalTrackStrip>();
            for (HelicalTrackStrip strip : cross.getStrips()) {

                Hep3Vector origin = strip.origin();
                Hep3Vector u = strip.u();
                Hep3Vector v = strip.v();
                double umeas = strip.umeas();
                double du = strip.du();
                double vmin = strip.vmin();
                double vmax = strip.vmax();
                double dedx = strip.dEdx();
                double time = strip.time();
                List<RawTrackerHit> rthList = strip.rawhits();
                String detname = strip.detector();
                int layer = strip.layer();
                BarrelEndcapFlag bec = strip.BarrelEndcapFlag();
                Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(origin);
                Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
                Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);
                HelicalTrackStrip newstrip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dedx, time, rthList, detname, layer, bec);
                rotatedstriphits.add(newstrip);
            }
            List<HelicalTrackStrip> strip1 = new ArrayList<HelicalTrackStrip>();
            List<HelicalTrackStrip> strip2 = new ArrayList<HelicalTrackStrip>();
            strip1.add(rotatedstriphits.get(0));
            strip2.add(rotatedstriphits.get(1));
            HelicalTrackCross newhit = new HelicalTrackCross(strip1.get(0),strip2.get(0));

            rotatedhits.add(newhit);
            hthrelations.add(new MyLCRelation(cross, newhit));
        }
        event.put("Rotated" + _outname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated" + _hitrelname, hthrelations, LCRelation.class, 0);
    }
}