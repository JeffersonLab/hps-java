package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.FieldMap;
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

    public static Map<MCParticle, List<SimTrackerHit>> BuildTrackerHitMap(List<SimTrackerHit> trackerHits){
        Map<MCParticle, List<SimTrackerHit>> trackerHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
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
    
    public static String trackHitLayer(SimTrackerHit hit) {
        String layer = Integer.toString( (int) hit.getLayer());
        double y = hit.getPositionVec().y();
        String volume = "";
        //boolean isTop = ((HpsSiSensor) ((SimTrackerHit) hit.getDetectorElement()).getDetectorElement()).isTopLayer();
        //if(isTop) volume = "t";
        if(y > 0) volume = "t";
        else volume = "b";
        String prefix = "L" + layer + volume;
        return prefix;
    }
    
    public static String trackHitLayer_1(SimTrackerHit hit) {
        String layer = Integer.toString( (int) hit.getLayer() - 1);
        double y = hit.getPositionVec().y();
        String volume = "";
        //boolean isTop = ((HpsSiSensor) ((SimTrackerHit) hit.getDetectorElement()).getDetectorElement()).isTopLayer();
        //if(isTop) volume = "t";
        if(y > 0) volume = "t";
        else volume = "b";
        String prefix = "L" + layer + volume;
        return prefix;
    }
    
    public static String MCParticleType(MCParticle p, List<MCParticle> particles, double ebeam) {
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

    /*public static Hep3Vector extrapolateTrackPosition(Hep3Vector startPosition, Hep3Vector startMomentum, double endPositionZ, double stepSize, FieldMap fieldMap, double q) {

        // Start by transforming detector vectors into tracking frame
        Hep3Vector currentPosition = CoordinateTransformations.transformVectorToTracking(startPosition);
        Hep3Vector currentMomentum = CoordinateTransformations.transformVectorToTracking(startMomentum);
        
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
        while (currentPosition.x() < endPositionZ) {      
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
            if(currentMomentum.x() < 0){
                return null;
            }
            currentPosition = trajectory.getPointAtDistance(stepSize);

            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
            
            //System.out.println(currentPosition.x() + "  " + currentPosition.y() + "  " + currentPosition.z() + "  ");

            // If the position of the track along X (or z in the detector frame)
            // is at 90% of the total distance, reduce the step size.
            if (currentPosition.x() / endPositionZ > .80 && !stepSizeChange) {
                stepSize /= 10;
                // System.out.println("Changing step size: " + stepSize);
                stepSizeChange = true;
            }
        }

        // Transform vector back to detector frame
        return CoordinateTransformations.transformVectorToDetector(currentPosition);
    }
    
    public static Hep3Vector extrapolateTrackMomentum(Hep3Vector startPosition, Hep3Vector startMomentum, double endPositionZ, double stepSize, FieldMap fieldMap, double q) {

        // Start by transforming detector vectors into tracking frame
        Hep3Vector currentPosition = CoordinateTransformations.transformVectorToTracking(startPosition);
        Hep3Vector currentMomentum = CoordinateTransformations.transformVectorToTracking(startMomentum);

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
        while (currentPosition.x() < endPositionZ) {      
            // The field map coordinates are in the detector frame so the
            // extrapolated track position needs to be transformed from the
            // track frame to detector.
            Hep3Vector currentPositionDet = CoordinateTransformations.transformVectorToDetector(currentPosition);

            // Get the field at the current position along the track.
            bFieldY = fieldMap.getField(currentPositionDet).y();

            // Get a trajectory (Helix or Line objects) created with the
            // track parameters at the current position.
            Trajectory trajectory = TrackUtils.getTrajectory(currentMomentum, new org.lcsim.spacegeom.SpacePoint(currentPosition), q, bFieldY);
            
            //if(CoordinateTransformations.transformVectorToTracking(startPosition).x() > currentPosition.x()){
            if(currentMomentum.x() < 0){
                return null;
            }

            // Using the new trajectory, extrapolated the track by a step and
            // update the extrapolated position.
            currentPosition = trajectory.getPointAtDistance(stepSize);

            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));

            // If the position of the track along X (or z in the detector frame)
            // is at 90% of the total distance, reduce the step size.
            if (currentPosition.x() / endPositionZ > .80 && !stepSizeChange) {
                stepSize /= 10;
                // System.out.println("Changing step size: " + stepSize);
                stepSizeChange = true;
            }
        }

        // Transform vector back to detector frame
        return CoordinateTransformations.transformVectorToDetector(currentMomentum);
    }*/
    
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
    
    public static Hep3Vector extrapolateTrackMomentum(Hep3Vector endPosition, Hep3Vector endMomentum, Hep3Vector startPosition, double stepSize, FieldMap fieldMap, double q) {
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
        return CoordinateTransformations.transformVectorToDetector(currentMomentum);
    }
    
    public static Hep3Vector extrapolateTrackPosition(Hep3Vector endPosition, Hep3Vector endMomentum, Hep3Vector startPosition, double stepSize, FieldMap fieldMap, double q) {
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
            currentPosition = trajectory.getPointAtDistance(stepSize);

            // Calculate the momentum vector at the new position. This will
            // be used when creating the trajectory that will be used to
            // extrapolate the track in the next iteration.
            currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
        
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
        return CoordinateTransformations.transformVectorToDetector(currentPosition);
    }
    

}
