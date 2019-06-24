package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
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
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.IDDecoder;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.swim.Trajectory;

/**
 * This is the tuple template driver
 * Use this to add your code and variables to make a tuple
 * Run the GeneralTupleDriver to output info into a text file
 * Change the steering file to include this driver
 * Run "makeTree.py" on text file to create a root tuple
 *
 * @author mrsolt on Aug 31, 2017
 */

public class MCFullDetectorTruth{

    private TrackTruthMatching _pTruth =  null;
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
    
    private final String trackHitMCRelationsCollectionName = "RotatedHelicalTrackMCRelations";
    private String trackerHitsCollectionName = "TrackerHits";
    private String inactiveTrackerHitsCollectionName = "TrackerHits_Inactive";
    private String ecalHitsCollectionName = "EcalHits";
    private final String mcParticleCollectionName = "MCParticle";
    private String detectorFrameHitRelationsCollectionName = "HelicalTrackHitRelations";
    private String trackHitRelationsCollectionName = "RotatedHelicalTrackHitRelations";
    
    

    public MCFullDetectorTruth(EventHeader event, Track trk, FieldMap fieldMap, List<HpsSiSensor> sensors, Subdetector trackerSubdet) {
        doTruth(event, trk, fieldMap, sensors, trackerSubdet);
    }
    
    private void doTruth(EventHeader event, Track trk, FieldMap bFieldMap, List<HpsSiSensor> sensors, Subdetector trackerSubdet){
    
        List<SimTrackerHit> trackerHits = event.get(SimTrackerHit.class, trackerHitsCollectionName);
        List<SimTrackerHit> trackerHits_Inactive = null;
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
        
        List<SimTrackerHit> truthActHits = trackerHitMap.get(truthp);
        List<SimTrackerHit> truthInActHits = trackerInHitMap.get(truthp);
        
        ComputeSVTVars(truthp, truthActHits, truthInActHits, bFieldMap, sensors, trackerSubdet);
        
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
        /*if(hits_act != null && hits_in != null){
            if(hits_in.size() > 9999){
                for (SimTrackerHit hit : hits_act) {
                    System.out.println("Active Hits " + hit.getLayer());
                }
            }
        }
        if(hits_act != null && hits_in != null){
            if(hits_in.size() > 9999){
                for (SimTrackerHit hit : hits_in) {
                    IDetectorElement de = trackerSubdet.getDetectorElement().findDetectorElement(hit.getPositionVec());
                    int layer = trackHitLayerNum(de, sensors, true);
                    System.out.println("InActive Hits " + layer);
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
        }*/
        
        if(hits_act == null) 
            return null;
        List<Pair<SimTrackerHit,String>> hitlist = new ArrayList<Pair<SimTrackerHit,String>>();
        if(hits_in == null){
            for(SimTrackerHit hit : hits_act){
                hitlist.add(new Pair<SimTrackerHit,String>(hit,"active"));
            }
            /*for(Pair<SimTrackerHit,String> pair : hitlist){
                System.out.println(pair.getFirst().getLayer() + " " + pair.getSecond());
            }*/
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
        /*if(hits_act != null && hits_in != null){
            if(hits_in.size() > 999){
                for(Pair<SimTrackerHit,String> pair : hitlist){
                    SimTrackerHit hit = pair.getFirst();
                    IDetectorElement de = trackerSubdet.getDetectorElement().findDetectorElement(hit.getPositionVec());
                    int layer = 0;
                    if(pair.getSecond() == "inactive")
                        layer = trackHitLayerNum(de, sensors, true);
                    else
                        layer = hit.getLayerNumber();
                    System.out.println(layer + " " + pair.getSecond());
                }
                System.out.println("");
                System.out.println("");
                System.out.println("");
                System.out.println("");
            }
        }*/
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
    
    /*public static String trackHitLayer(SimTrackerHit hit) {
        String layer = Integer.toString( (int) hit.getLayer());
        double y = hit.getPositionVec().y();
        String volume = "";
        //boolean isTop = ((HpsSiSensor) ((SimTrackerHit) hit.getDetectorElement()).getDetectorElement()).isTopLayer();
        //if(isTop) volume = "t";
        if(y > 0) volume = "t";
        else volume = "b";
        String prefix = "L" + layer + volume;
        return prefix;
    }*/
    
    /*public static String trackHitLayer(IDetectorElement de, List<HpsSiSensor> sensors, boolean inactive) {
        if(!inactive){
            HpsSiSensor sensor = ((HpsSiSensor) de);
            String layer = Integer.toString(sensor.getLayerNumber());
            String volume = "";
            if(sensor.isTopLayer()) volume = "t";
            else volume = "b";
            return "L" + layer + volume;
        }
        else{
            HpsSiSensor sensor = null;
            for (HpsSiSensor s : sensors){
                String name = de.getName() + "_sensor0";
                if(s.getName().equals(name)){
                    sensor = s;
                    break;
                }
            }
            String layer = Integer.toString(sensor.getLayerNumber());
            String volume = "";
            if(sensor.isTopLayer()) volume = "t";
            else volume = "b";
            return "L" + layer + volume;
        }
        //System.out.println(sensor.getModuleId());
        //String layer = "";
        //System.out.println(sensor.getName());
        //HpsSiSensor sensor = null;
        /*for (HpsSiSensor s : sensors){
            System.out.println(s.getName() + "  " + de.getName() + "  ");
            String name = de.getName();
            if(de.isSensitive()) name = name + "_sensor0";
            if(s.getName() == name){
                System.out.println(s.getName());
                sensor = s;
                break;
            }
        }
        //String layer = Integer.toString(sensor.getLayerNumber());;
        //String volume = "";
        //if(sensor.isTopLayer()) volume = "t";
        //else volume = "b";
        //return "L" + layer + volume;
    }*/
    
    public static int trackHitLayerNum(IDetectorElement de, List<HpsSiSensor> sensors, boolean inactive) {
        if(!inactive){
            try{
                //System.out.println(de.getName() + " " + de.getParent().getName() + " " + de.getAncestry());
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
            return sensor.getLayerNumber();
        }
    }
    
    /*public static String trackHitLayer_1(SimTrackerHit hit) {
        String layer = Integer.toString( (int) hit.getLayer() - 1);
        double y = hit.getPositionVec().y();
        String volume = "";
        //boolean isTop = ((HpsSiSensor) ((SimTrackerHit) hit.getDetectorElement()).getDetectorElement()).isTopLayer();
        //if(isTop) volume = "t";
        if(y > 0) volume = "t";
        else volume = "b";
        String prefix = "L" + layer + volume;
        return prefix;
    }*/
    
    /*public static String MCParticleType(MCParticle p, List<MCParticle> particles, double ebeam) {
        boolean isEle = false;
        boolean isPos = false;
        
        // particle PDG ID
        int pdgid = p.getPDGID();
        
        // particle energy
        double energy = p.getEnergy();
        
        if(pdgid == 11){
            isEle = true;
        }
        
        if(pdgid == -11){
            isPos = true;
        }
        
        // parent index in particle list
        Integer parIdx = null;
        
        // find parent's index in the particle collection
        if (p.getParents().size() > 0) {
            MCParticle parent = p.getParents().get(0);
            for (int i = 0; i < particles.size(); i++) {
                if (particles.get(i) == parent) {
                    parIdx = i;
                    break;
                }
            }
        }
        else{
            parIdx = -1;
        }
        if(isEle && parIdx == 0){
            for (MCParticle particle : particles) {
                if(particle.getParents().size() == 0) continue;
                if(particle.getPDGID() == 11 && particle.getParents().get(0).getPDGID() == 622 && p != particle){
                    if(particle.getEnergy() < energy){
                        return "triEle1";
                    }
                    else{
                        return "triEle2";
                    }
                }
            }
        }
        
        if(isPos && parIdx == 0){
            return "triPos";
        }     
        

        MCParticle wab = null;

        MCParticle ele1 = null;// conversion electron daughter
        MCParticle ele2 = null;// recoil wab electron
        MCParticle pos = null;// conversion positron daughter

        List<MCParticle> wabParticles = null;

        for (MCParticle particle : particles) {
            if (particle.getPDGID() == 22) {
                if (particle.getDaughters().size() != 2) continue;
                double wabEnergy = particle.getEnergy();
                for(MCParticle part : particles){
                    if(part.getPDGID() != 11 || !part.getParents().isEmpty()) continue;
                    double eleEnergy = part.getEnergy();
                    double esum = wabEnergy + eleEnergy;
                    if(esum < 0.98 * ebeam || esum > 1.02 * ebeam) continue;
                    ele2 = part;
                    wab = particle;
                    wabParticles = wab.getDaughters();
                    break;
                }
            }
        }
        if (wab == null) {
            return "";
        }


        for (MCParticle particle : wabParticles) {
            if(particle.getPDGID() == 11){
                pos = particle;
            }
            if(particle.getPDGID() == -11){
                ele1 = particle;
            }
        }

        if (ele1 == p) {
            return "wabEle1";
        }
        if (ele2 == p) {
            return "wabEle2";
        }
        if (pos == p) {
            return "wabPos";
        }
        
        return "";
    }
    */
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
    
        //System.out.println(currentPosition.x() + "  " + currentPosition.y() + "  " + currentPosition.z() + "  ");

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
