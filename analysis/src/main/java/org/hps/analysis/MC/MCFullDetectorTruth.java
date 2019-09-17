package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.swim.Trajectory;

/**
 * This is the driver that takes a TrackTruthMatching object
 * and computes the full truth information including scattering angles
 *
 * @author mrsolt on Aug 31, 2017
 */

public class MCFullDetectorTruth{

    private TrackTruthMatching _pTruth =  null;
    private List<SimTrackerHit> _truthActHits = null;
    private Map<Integer,Set<Track>> _sharedTrk = new HashMap<Integer,Set<Track>>();
    private Map<Integer,Hep3Vector> _actHitPos = new HashMap<Integer,Hep3Vector>();
    private Map<Integer,Hep3Vector> _inactHitPos = new HashMap<Integer,Hep3Vector>();
    private Map<Integer,Hep3Vector> _actHitP = new HashMap<Integer,Hep3Vector>();
    private Map<Integer,Hep3Vector> _inactHitP = new HashMap<Integer,Hep3Vector>();
    private Map<Integer,double[]> _actHitTheta = new HashMap<Integer,double[]>();
    private Map<Integer,double[]> _inactHitTheta = new HashMap<Integer,double[]>();
    private Map<Integer,double[]> _actHitResidual = new HashMap<Integer,double[]>();
    private Map<Integer,double[]> _inactHitResidual = new HashMap<Integer,double[]>();
    private Map<Integer,int[]> _ecalHitIndex = new HashMap<Integer,int[]>();
    private Map<Integer,Hep3Vector> _ecalHitPos = new HashMap<Integer,Hep3Vector>();
    private Map<Integer,Double> _ecalHitE = new HashMap<Integer,Double>();
    private int _ecalNHits = 0;
    private boolean _isTriggered = false;
    
    private String trackerHitsCollectionName = "TrackerHits";
    private String inactiveTrackerHitsCollectionName = "TrackerHits_Inactive";
    private String ecalHitsCollectionName = "EcalHits";
    private String trackCollectionName = "GBLTracks";
    
    public MCFullDetectorTruth(EventHeader event, Track trk, FieldMap fieldMap, List<HpsSiSensor> sensors, Subdetector trackerSubdet) {
        doTruth(event, trk, fieldMap, sensors, trackerSubdet);
    }
    
    private void doTruth(EventHeader event, Track trk, FieldMap bFieldMap, List<HpsSiSensor> sensors, Subdetector trackerSubdet){
    
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, trackerHitsCollectionName);
        List<SimTrackerHit> trackerHits_Inactive = null;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
        if(event.hasCollection(SimTrackerHit.class , inactiveTrackerHitsCollectionName)){
            trackerHits_Inactive = event.get(SimTrackerHit.class, inactiveTrackerHitsCollectionName);
        }
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = BuildTrackerHitMap(trackerHits);
        Map<MCParticle, List<SimTrackerHit>> trackerInHitMap = BuildTrackerHitMap(trackerHits_Inactive);
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null)
                    rawtomc.add(relation.getFrom(), relation.getTo());
        }
        _pTruth = new TrackTruthMatching(trk, rawtomc, trackerHits);
        
        MCParticle truthp = _pTruth.getMCParticle();
        
        if(truthp == null)
            return;
        
        List<TrackerHit> trk_hits = trk.getTrackerHits();
        for(Track track : tracks){
            if(track.equals(trk))
                continue;
            boolean sharedHit = false;
            List<TrackerHit> hits = track.getTrackerHits();
            int sharedLay = 9999;
            for(TrackerHit hit : hits){
                for(TrackerHit trk_hit : trk_hits){
                    int lay = ((RawTrackerHit) trk_hit.getRawHits().get(0)).getLayerNumber();
                    if(!_sharedTrk.containsKey(lay)){
                        _sharedTrk.put(lay,new HashSet<Track>());
                    }
                    if(hit.equals(trk_hit)){
                        sharedHit = true;
                        sharedLay = lay;
                        break;
                    }
                }
                if(sharedHit)
                    break;
            }
            if(sharedHit){
                if(_sharedTrk.containsKey(sharedLay)){
                    _sharedTrk.get(sharedLay).add(track);
                }
                else{
                    _sharedTrk.put(sharedLay,new HashSet<Track>());
                }
            }
        }
        
        _truthActHits = trackerHitMap.get(truthp);
        List<SimTrackerHit> truthInActHits = trackerInHitMap.get(truthp);
        
        ComputeSVTVars(truthp, _truthActHits, truthInActHits, bFieldMap, sensors, trackerSubdet);
        
        List<SimCalorimeterHit> calHits = event.get(SimCalorimeterHit.class, ecalHitsCollectionName);
        Map<MCParticle, List<SimCalorimeterHit>> calHitMap = BuildCalHitMap(calHits);
        
        List<SimCalorimeterHit> truthEcalHits = calHitMap.get(truthp);
        if(truthEcalHits == null)
            return;
        
        IDDecoder calDecoder = event.getMetaData(calHits).getIDDecoder();
        
        ComputeEcalVars(event,truthEcalHits,calDecoder);
    }

    private void ComputeSVTVars(MCParticle p, List<SimTrackerHit> hits_act, List<SimTrackerHit> hits_in, FieldMap bFieldMap, List<HpsSiSensor> sensors, Subdetector trackerSubdet){
        // loop over particle's hits
        boolean inactiveprev = false;
        int layerprev = 0;
        List<Pair<SimTrackerHit,String>> allhits = orderHits(hits_act, hits_in, trackerSubdet, sensors);
        
        Hep3Vector startPosition = allhits.get(0).getFirst().getPositionVec();
        double[] startP = allhits.get(0).getFirst().getMomentum();
        Hep3Vector startMomentum = new BasicHep3Vector(startP[0], startP[1], startP[2]);
        
        for(Pair<SimTrackerHit,String> allhit : allhits){
            boolean inactive = allhit.getSecond().equals("inactive");
            SimTrackerHit hit = allhit.getFirst();
            IDetectorElement de = trackerSubdet.getDetectorElement().findDetectorElement(hit.getPositionVec());
            int layer = trackHitLayerNum(de, sensors, inactive);
            Hep3Vector endPosition = hit.getPositionVec();
            Hep3Vector endMomentum = new BasicHep3Vector(hit.getMomentum()[0], hit.getMomentum()[1], hit.getMomentum()[2]);
            if(layer == layerprev){
                if(inactive != inactiveprev)
                    inactiveprev = false;
                continue;
            }
            
            Pair<Hep3Vector,Hep3Vector> extrapPair = extrapolateTrack(endPosition,endMomentum,startPosition,5,bFieldMap,p.getCharge());
            if(extrapPair == null) continue;
            Hep3Vector extrapPos = extrapPair.getFirst();
            Hep3Vector extrapP = extrapPair.getSecond();

            double thetaX = MCFullDetectorTruth.deltaThetaX(VecOp.neg(extrapP),startMomentum);
            double thetaY = MCFullDetectorTruth.deltaThetaY(VecOp.neg(extrapP),startMomentum);
            
            double[] theta = {thetaX,thetaY};
            double[] residual = {startPosition.x() - extrapPos.x(),startPosition.y() - extrapPos.y()};
            
            if(!inactive)
                _actHitPos.put(layer,endPosition);
            else
                _inactHitPos.put(layer,endPosition);
            if(!inactiveprev){
                _actHitP.put(layerprev,startMomentum);
                _actHitTheta.put(layerprev,theta);
                _actHitResidual.put(layerprev,residual);
            }
            else{
                _inactHitP.put(layerprev,startMomentum);
                _inactHitTheta.put(layerprev,theta);
                _inactHitResidual.put(layerprev,residual);
            }
            startPosition = endPosition;
            startMomentum = endMomentum;
            inactiveprev = inactive;
            layerprev = layer;
        }
    }
    
    private List<Pair<SimTrackerHit,String>> orderHits(List<SimTrackerHit> hits_act, List<SimTrackerHit> hits_in, Subdetector trackerSubdet, List<HpsSiSensor> sensors){
        if(hits_act == null) 
            return null;
        List<Pair<SimTrackerHit,String>> hitlist = new ArrayList<Pair<SimTrackerHit,String>>();
        if(hits_in == null){
            for(SimTrackerHit hit : hits_act){
                hitlist.add(new Pair<SimTrackerHit,String>(hit,"active"));
            }
            return hitlist;
        }
        
        int n_inact_used = 0;

        for (SimTrackerHit hit : hits_act) {
            boolean inactive = false;
            SimTrackerHit hit_act = hit;
            do{
                inactive = false;
                int n_inact = 0;
                for(SimTrackerHit hit_in:hits_in){
                    n_inact++;
                    if(n_inact_used >= n_inact) continue;
                    IDetectorElement de = trackerSubdet.getDetectorElement().findDetectorElement(hit_in.getPositionVec());
                    int layer_in = trackHitLayerNum(de, sensors, true);
                    if(layer_in >= hit_act.getLayer()) continue;
                    hit = hit_in;
                    inactive = true;
                    n_inact_used = n_inact;
                    break;
                }
                
                if(!inactive){
                    hit = hit_act;
                    hitlist.add(new Pair<SimTrackerHit,String>(hit,"active"));
                }
                else
                    hitlist.add(new Pair<SimTrackerHit,String>(hit,"inactive"));
                
            } while(inactive);
        }
        if(n_inact_used > 0){
            int n_inact = 0;
            for(SimTrackerHit hit:hits_in){
                n_inact++;
                if(n_inact_used >= n_inact) continue;
                hitlist.add(new Pair<SimTrackerHit,String>(hit,"inactive"));

            }
        }
        return hitlist;
    }
    
    private void ComputeEcalVars(EventHeader event, List<SimCalorimeterHit> truthEcalHits,IDDecoder calDecoder){
        _ecalNHits = truthEcalHits.size();
        int i = 0;
        for(SimCalorimeterHit hit : truthEcalHits){
            calDecoder.setID(hit.getCellID());
            int[] index = {calDecoder.getValue("ix"),calDecoder.getValue("iy")};
            _ecalHitIndex.put(i,index);
            _ecalHitPos.put(i,hit.getPositionVec());
            _ecalHitE.put(i, hit.getCorrectedEnergy());
            i++;
        }
    }
    
    public static Map<MCParticle, List<SimTrackerHit>> BuildTrackerHitMap(List<SimTrackerHit> trackerHits){
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
        if(trackerHits == null) { return trackerHitMap; }
        for (SimTrackerHit hit : trackerHits) {
            MCParticle p = hit.getMCParticle();
            if (p == null) {
                throw new RuntimeException("Tracker hit points to null MCParticle!");
            }
            if (trackerHitMap.get(p) == null) {
                trackerHitMap.put(p, new ArrayList<SimTrackerHit>());
            }
            trackerHitMap.get(p).add(hit);
        }        
        return trackerHitMap;
    }
    
    public static Map<MCParticle, List<SimCalorimeterHit>> BuildCalHitMap(List<SimCalorimeterHit> calHits){
        // map particle to a list of its sim cal hits
        Map<MCParticle, List<SimCalorimeterHit>> calHitMap = new HashMap<MCParticle, List<SimCalorimeterHit>>();
        for (SimCalorimeterHit hit : calHits) {
            int nmc = hit.getMCParticleCount();
            for (int i = 0; i < nmc; i++) {
                MCParticle p = hit.getMCParticle(i);
                if (p == null) {
                    throw new RuntimeException("Cal hit points to null MCParticle!");
                }
                if (calHitMap.get(p) == null) {
                    calHitMap.put(p, new ArrayList<SimCalorimeterHit>());
                }
                if (!calHitMap.get(p).contains(hit)) {
                    calHitMap.get(p).add(hit);
                }
            }
        }
        return calHitMap;
    }
    
    public static int trackHitLayerNum(IDetectorElement de, List<HpsSiSensor> sensors, boolean inactive) {
        if(!inactive){
            try{
                HpsSiSensor sensor = ((HpsSiSensor) de);
                return sensor.getLayerNumber();
            }
            catch(ClassCastException e){
                System.out.println("Sim tracker hit does not correspond to a sensor! " + e.getMessage());
                return -1;
            }
        }
        else{
            HpsSiSensor sensor = null;
            for (HpsSiSensor s : sensors){
                try{
                    String name = de.getName() + "_sensor0";
                    if(s.getName().equals(name)){
                        sensor = s;
                        break;
                    }
                }
                catch(ClassCastException e){
                    System.out.println("Sim tracker hit does not correspond to a sensor! " + e.getMessage());
                    return -1;
                }
            }
            if(sensor != null){
                return sensor.getLayerNumber();
            }
            else{
                return -1;
            }
        }
    }

    public static double deltaThetaX(Hep3Vector p1, Hep3Vector p2){
        double theta1 = p1.x()/p1.z();
        double theta2 = p2.x()/p2.z();
        return theta2 - theta1;
    }
    
    public static double deltaThetaY(Hep3Vector p1, Hep3Vector p2){
        double theta1 = p1.y()/p1.z();
        double theta2 = p2.y()/p2.z();
        return theta2 - theta1;
    }
    
    public static Pair<Hep3Vector,Hep3Vector> extrapolateTrack(Hep3Vector endPosition, Hep3Vector endMomentum, Hep3Vector startPosition, double stepSize, FieldMap fieldMap, double q) {
        if(endPosition == null || endMomentum == null || startPosition == null) return null;
        // Start by transforming detector vectors into tracking frame
        Hep3Vector currentPosition = CoordinateTransformations.transformVectorToTracking(endPosition);
        Hep3Vector currentMomentum = VecOp.neg(CoordinateTransformations.transformVectorToTracking(endMomentum));
        double startPositionZ = startPosition.z();

        // Retrieve the y component of the bfield in the middle of detector
        double bFieldY = fieldMap.getField(new BasicHep3Vector(0, 0, 500.0)).y();       
    
        // HACK: LCSim doesn't deal well with negative fields so they are
        // turned to positive for tracking purposes. As a result,
        // the charge calculated using the B-field, will be wrong
        // when the field is negative and needs to be flipped.
        if (bFieldY < 0)
            q = q * (-1);

        // Swim the track through the B-field until the end point is reached.
        // The position of the track will be incremented according to the step
        // size up to ~90% of the final position. At this point, a finer
        // track size will be used.
        boolean stepSizeChange = false;
        while (currentPosition.x() > startPositionZ) {      
            // The field map coordinates are in the detector frame so the
            // extrapolated track position needs to be transformed from the
            // track frame to detector.
            Hep3Vector currentPositionDet = CoordinateTransformations.transformVectorToDetector(currentPosition);

            // Get the field at the current position along the track.
            bFieldY = fieldMap.getField(currentPositionDet).y();

            // Get a trajectory (Helix or Line objects) created with the
            // track parameters at the current position.
            Trajectory trajectory = TrackUtils.getTrajectory(currentMomentum, new org.lcsim.spacegeom.SpacePoint(currentPosition), q, bFieldY);

            // Using the new trajectory, extrapolated the track by a step and
            // update the extrapolated position.
            //if(CoordinateTransformations.transformVectorToTracking(startPosition).x() > currentPosition.x()){
            if(currentMomentum.x() > 0){
                //System.out.println("Track is going Forwards!");
                return null;
            }
            
            //If trajectory step crosses starting position, adjust step size to fall on starting position
            if(trajectory.getPointAtDistance(stepSize).x() < startPositionZ){
                stepSize = VecOp.sub(CoordinateTransformations.transformVectorToTracking(startPosition),currentPosition).magnitude();
            }
            
            currentPosition = trajectory.getPointAtDistance(stepSize);

            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
            
            //System.out.println("Position " + CoordinateTransformations.transformVectorToDetector(currentPosition) + "   Momentum " + CoordinateTransformations.transformVectorToDetector(currentMomentum) + "   startPositionZ " + startPositionZ);
        
            //System.out.println(currentPosition.x() + "  " + currentPosition.y() + "  " + currentPosition.z() + "  ");

            // If the position of the track along X (or z in the detector frame)
            // is at 90% of the total distance, reduce the step size.
            if (currentPosition.x() / startPositionZ > .80 && !stepSizeChange) {
                stepSize /= 10;
                //System.out.println("Changing step size: " + stepSize);
                stepSizeChange = true;
            }
        }

        // Transform vector back to detector frame
        Hep3Vector returnPos = CoordinateTransformations.transformVectorToDetector(currentPosition);
        Hep3Vector returnP = CoordinateTransformations.transformVectorToDetector(currentMomentum);
        return new Pair<>(returnPos,returnP);
    }
    
    //Return MCParticle matched to track
    public MCParticle getMCParticle() {
        return _pTruth.getMCParticle();
    }

    //Returns number of tracker hits (10 or 12)
    public int getNHits() {
        return _pTruth.getNHits();
    }

    //Returns the number of MCParticle hits on track
    public int getNGoodHits() {
        return _pTruth.getNGoodHits();
    }
    
    //Returns the number of missing hits
    //_nhits - number of MCParticle hits on track
    public int getNBadHits() {
        return _pTruth.getNBadHits();
    }

    //Returns purity of track truth match
    //_ngoodhits / _nhits
    public double getPurity() {
        return _pTruth.getPurity();
    }
    
    //Returns true if the track is in the top volume
    //false otherwise
    public boolean isTop() {
        return _pTruth.isTop();
    }

    //Returns list of all Tracks associated with a tracker hit
    //on a layer
    public Set<Track> getHitTrackList(int layer) {
        return _sharedTrk.get(layer);
    }
    
    //Returns the number of MCParticles that contribute to the
    //tracker hit at a layer
    public int getNumberOfMCParticles(int layer) {
        return _pTruth.getNumberOfMCParticles(layer);
    }
    
    //Returns list of all MCParticles associated with a tracker hit
    //on a layer
    public Set<MCParticle> getHitMCParticleList(int layer) {
        return _pTruth.getHitMCParticleList(layer);
    }

    //Returns all active hits associated with MC particle
    public List<SimTrackerHit> getActiveHitListMCParticle(){
        return _truthActHits;
    }
    
    //Returns a boolean of which hits of MCParticle contribute
    //to the track
    public Boolean getHitList(int layer) {
        return _pTruth.getHitList(layer);
    }
    
    //Returns a list of SimTrackerHits for the matched MCParticle
    //that does not contribute to the tracker hits on the tracks
    public Set<SimTrackerHit> getHitListNotMatched() {
        return _pTruth.getHitListNotMatched();
    }
    
    //Returns truth hit position at layer in active silicon
    public Hep3Vector getActiveHitPosition(int layer){
        return _actHitPos.get(layer);
    }
    
    //Returns truth hit position at layer in inactive silicon
    public Hep3Vector getInactiveHitPosition(int layer){
        return _inactHitPos.get(layer);
    }
    
    //Returns truth hit momentum at layer in active silicon
    public Hep3Vector getActiveHitMomentum(int layer){
        return _actHitP.get(layer);
    }
    
    //Returns truth hit momentum at layer in inactive silicon
    public Hep3Vector getInactiveHitMomentum(int layer){
        return _inactHitP.get(layer);
    }
    
    //Returns "truth" hit scattering angle {x,y} at layer in active silicon
    //x is horizontal direction, y is vertical direction
    public double[] getActiveHitScatter(int layer){
        return _actHitTheta.get(layer);
    }
    
    //Returns "truth" hit scattering angle {x,y} at layer in inactive silicon
    //x is horizontal direction, y is vertical direction
    public double[] getInactiveHitScatter(int layer){
        return _inactHitTheta.get(layer);
    }
    
    //Returns hit residual {x,y} at layer in active silicon
    //x is horizontal direction, y is vertical direction
    //This is just a check, it should be basically 0
    public double[] getActiveHitResidual(int layer){
        return _actHitResidual.get(layer);
    }
    
    //Returns hit residual {x,y} at layer in inactive silicon
    //x is horizontal direction, y is vertical direction
    //This is just a check, it should be basically 0
    public double[] getInactiveHitResidual(int layer){
        return _inactHitResidual.get(layer);
    }
    
    //Returns ecal hit index {x,y} for a given hit number
    public int[] getEcalHitIndex(int hitnum){
        return _ecalHitIndex.get(hitnum);
    }
    
    //Returns ecal hit position for a given hit number
    public Hep3Vector getEcalHitPosition(int hitnum){
        return _ecalHitPos.get(hitnum);
    }
    
    //Returns ecal hit energy} for a given hit number
    public Double getEcalHitEnergy(int hitnum){
        return _ecalHitE.get(hitnum);
    }
    
    //Returns number of Ecal hits
    public int getEcalNHits(){
        return _ecalNHits;
    }
    
  //Returns whether this MCParticle contributes to a triggered cluster
    public boolean getIsTriggered(){
        return _isTriggered;
    }
}
