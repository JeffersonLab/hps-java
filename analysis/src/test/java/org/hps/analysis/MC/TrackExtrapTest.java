package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
//import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.loop.LCSimLoop;
import org.lcsim.util.swim.Trajectory;

import junit.framework.TestCase;

public class TrackExtrapTest extends TestCase {

    public void testIt() throws Exception
    {
        int nEvents = -1;
        String fileName = "RecoCopy_tritrig-wab-beam.slcio";
        File inputFile = new File(fileName);
        String aidaOutput = fileName.replaceAll("slcio", "root");
        LCSimLoop loop2 = new LCSimLoop();
        
        final DatabaseConditionsManager manager = new DatabaseConditionsManager();
        manager.addConditionsListener(new SvtDetectorSetup());

        loop2.setLCIORecordSource(inputFile);

        RawTrackerHitSensorSetup rthss = new RawTrackerHitSensorSetup();
        String[] readoutColl = { "SVTRawTrackerHits" };
        rthss.setReadoutCollections(readoutColl);
        loop2.add(rthss);

        TrackExtrapTestDriver trp = new TrackExtrapTestDriver();
        trp.setOutputPlots(aidaOutput);
        loop2.add(trp);

        ReadoutCleanupDriver rcd = new ReadoutCleanupDriver();
        loop2.add(rcd);
        
        loop2.loop(nEvents);
    }
    
    protected class TrackExtrapTestDriver extends Driver {
        private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";
        FieldMap bFieldMap = null;
        private double ecalPosition = 1338.0;
        private double stepSize = 5.0;
        private double epsilon = 0.05;
        public AIDA aida = null;
        private String outputPlots = null;
        private double lowMomThresh = 0.3;
       // private boolean foundWeird = false;
        
        @Override
        public void endOfData() {
            if (outputPlots != null) {
                try {
                    aida.saveAs(outputPlots);
                } catch (IOException ex) {
                    Logger.getLogger(TrackExtrapTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
        
        public void setOutputPlots(String input) {
            outputPlots = input;
        }
        
        public void setStepSize(double input) {
            stepSize=input;
        }
        public void setEpsilon(double input) {
            epsilon=input;
        }
        
        @Override
        protected void detectorChanged(Detector detector) {            
            bFieldMap = detector.getFieldMap();
            if (ecalPosition == 0)
                ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
            if (aida == null)
                aida = AIDA.defaultInstance();
            aida.tree().cd("/");
            setupPlots();
            //foundWeird = false;
        }

        
        private void setupPlots() {
            aida.histogram2D("Y vs X extrap-path", 1000, 500, 1500, 600,-300,300);
            aida.histogram2D("Y vs Z extrap-path", 1000,-50,50, 600,-300,300);
            aida.histogram2D("X vs Z extrap-path", 1000,-50,50,1000, 500, 1500);
            aida.histogram2D("Bfield Y vs Z Pos", 1000, 890, ecalPosition,220, -0.22, 0);
            
            aida.histogram2D("Y vs X Pos Extrapolated to ECal", 300,-300,300,300,-300,300);
            
            aida.histogram2D("Weird Events: Y vs X Pos at Layer6", 300,-300,300,300,-300,300);
            aida.histogram2D("Weird Events: Y vs X Pos Extrapolated to ECal", 300,-300,300,300,-300,300);
            aida.histogram2D("Weird Events: Y vs X Mom at Layer6", 100,-0.5,0.5,100,-0.5,0.5);
            aida.histogram1D("Weird Events: Z Mom at Layer6", 100,0,1.5);
            aida.histogram2D("Weird Events: Y vs X Mom of MCParticle", 100,-0.5,0.5,100,-0.5,0.5);
            aida.histogram1D("Weird Events: Z Mom of MCParticle", 100,0,1.5);
            
            aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit Y", 300, -300, 300, 200, -5.0, 5.0);
            aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit Y", 300, -300, 300, 200, -5.0, 5.0);
            aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit X", 300, -300, 300, 200, -5.0, 5.0);
            aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit X", 300, -300, 300, 200, -5.0, 5.0);
            
            aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X", 200, -5.0, 5.0, 200, -5.0, 5.0);
            aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            
            aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X", 200, -5.0, 5.0, 200, -5.0, 5.0);
            aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            
            aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X", 200, -5.0, 5.0, 200, -5.0, 5.0);
            aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            
            aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X", 200, -5.0, 5.0, 200, -5.0, 5.0);
            aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            
            aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X", 200, -5.0, 5.0, 200, -5.0, 5.0);
            aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
            aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz", 100, 0, 1.5, 200, -5.0, 5.0);
        }
        
        @Override
        protected void process(EventHeader event) {
            //System.out.printf("processing event %d \n", event.getEventNumber());
            List<SimTrackerHit> trackerHits = null;
            if (event.hasCollection(SimTrackerHit.class, "TrackerHits"))
                trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
            else
                return;
//            List<SimCalorimeterHit> calHits = null;
//            if (event.hasCollection(SimCalorimeterHit.class, "EcalHits"))
//                calHits = event.get(SimCalorimeterHit.class, "EcalHits");
//            else
//                return;
            List<MCParticle> particles = null;
            if (event.hasCollection(MCParticle.class, "MCParticle"))
                particles = event.get(MCParticle.class, "MCParticle");  
            else
                return;
            List<SimTrackerHit> scoringHits = null;
            if (event.hasCollection(SimTrackerHit.class, "TrackerHitsECal"))
                scoringHits = event.get(SimTrackerHit.class, "TrackerHitsECal");
            else
                return;
            
            Map<MCParticle, List<SimTrackerHit>> scoringHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
            for (SimTrackerHit scoringHit : scoringHits) {
                MCParticle part = scoringHit.getMCParticle();
                if (scoringHitMap.get(part) == null) 
                    scoringHitMap.put(part, new ArrayList<SimTrackerHit>());
                scoringHitMap.get(part).add(scoringHit);
            }         
            
            Map<MCParticle, List<SimTrackerHit>> trackerHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits);
            //Map<MCParticle, List<SimCalorimeterHit>> calHitMap = MCFullDetectorTruth.BuildCalHitMap(calHits);
            
            for (MCParticle part : particles) {
                if (part.getCharge() == 0)
                    continue;
                if (part.getGeneratorStatus() != MCParticle.FINAL_STATE)
                    continue;
                if (!part.getSimulatorStatus().isDecayedInCalorimeter())
                    continue;
                if (!trackerHitMap.containsKey(part) || (!scoringHitMap.containsKey(part)))
                    continue;
                List<SimTrackerHit> hits = trackerHitMap.get(part);
                //List<SimCalorimeterHit> caloHits = calHitMap.get(part);
                List<SimTrackerHit> caloHitList = scoringHitMap.get(part);
                if ((hits == null) || (caloHitList == null) || (caloHitList.isEmpty()))
                    continue;
                
                SimTrackerHit lastHit = null;
                int lay=0;
                for (SimTrackerHit hit : hits) {
                    if (hit.getLayer() > lay) {
                        lay=hit.getLayer();
                        lastHit = hit;
                    }
                }
                if (lastHit == null || lastHit.getPosition()[2] < 890)
                    continue;
                    
                SimTrackerHit caloHit = caloHitList.get(0);
//                if (caloHitList.size() > 1) {
//                    for (SimTrackerHit scoringHit : caloHitList) {
//                        if (scoringHit.getTime() < caloHit.getTime())
//                            caloHit = scoringHit;
//                        System.out.printf("Multiple caloHit %s MCPart %s \n", scoringHit.getPositionVec().toString(), scoringHit.getMCParticle().toString());
//                    }
//                }
                
                Hep3Vector hitPosition = lastHit.getPositionVec();
                Hep3Vector hitMomentum = new BasicHep3Vector(lastHit.getMomentum());
                //System.out.printf("starting mom %s \n", CoordinateTransformations.transformVectorToTracking(hitMomentum).toString());
                
                Hep3Vector extrapPos = doTrackExtrap(CoordinateTransformations.transformVectorToTracking(hitPosition), CoordinateTransformations.transformVectorToTracking(hitMomentum), part.getCharge(), false);
                if (extrapPos == null)
                    continue;                
                extrapPos=CoordinateTransformations.transformVectorToDetector(extrapPos);

                caloHit = findClosestCaloHit(caloHitList, extrapPos);
                Hep3Vector caloHitPos = caloHit.getPositionVec();
                Hep3Vector residualVec = VecOp.sub(extrapPos, caloHitPos);
                
                // weird events?
                if (Math.abs(residualVec.x()) > 30) {
                    doWeirdPlots(hitPosition, hitMomentum, part.getMomentum(), extrapPos);
                    //if (!foundWeird) {
                        //System.out.printf("bad residual MCpart %s \n", part.toString());
//                    for (SimTrackerHit hit : hits) {
//                        System.out.printf("hit %s \n", hit.getPositionVec().toString());
//                    }
//                    System.out.printf("momentum %s \n", hitMomentum.toString());
                    //    doTrackExtrap(CoordinateTransformations.transformVectorToTracking(hitPosition), CoordinateTransformations.transformVectorToTracking(hitMomentum), part.getCharge(), true);
                    //}
                    //foundWeird=true;
                }
                else 
                    aida.histogram2D("Y vs X Pos Extrapolated to ECal").fill(extrapPos.x(), extrapPos.y());
                
                // filling plots
                if (Math.abs(part.getPZ()) < lowMomThresh) {
                    aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.x());
                    aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.x());
                    aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.y());
                    aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.y());
                }
                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                
                //System.out.printf("residual %s  caloHitPos %s  partPZ %f  lastHitPos %f \n", residualVec.toString(), caloHitPos.toString(), part.getPZ(), lastHit.getPosition()[2]);
              
                if (lastHit.getPosition()[1] > 0) {
                    if (Math.abs(part.getPZ()) < lowMomThresh)
                        aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                else {
                    if (Math.abs(part.getPZ()) < lowMomThresh)
                        aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                
                if (part.getCharge() > 0) {
                    if (Math.abs(part.getPZ()) < lowMomThresh)
                        aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                else {
                    if (Math.abs(part.getPZ()) < lowMomThresh)
                        aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                
                    //Hep3Vector endPositionrot = VecOp.mult(beamAxisRotation, endPosition);
            }
        }
        
        private SimTrackerHit findClosestCaloHit(List<SimTrackerHit> caloHitList, Hep3Vector extrapPos) {
            SimTrackerHit closestHit = caloHitList.get(0);
            double res = VecOp.sub(extrapPos, closestHit.getPositionVec()).magnitude();
            for (SimTrackerHit caloHit : caloHitList) {
                Hep3Vector residualVec = VecOp.sub(extrapPos, caloHit.getPositionVec());
                if (residualVec.magnitude() < res) {
                    res = residualVec.magnitude();
                    closestHit = caloHit;
                }
            }
            return closestHit;
        }

        private void doWeirdPlots(Hep3Vector lay6Pos, Hep3Vector lay6Mom, Hep3Vector partMom, Hep3Vector extrapPos) {
            aida.histogram2D("Weird Events: Y vs X Pos at Layer6").fill(lay6Pos.x(), lay6Pos.y());
            aida.histogram2D("Weird Events: Y vs X Pos Extrapolated to ECal").fill(extrapPos.x(), extrapPos.y());
            aida.histogram2D("Weird Events: Y vs X Mom at Layer6").fill(lay6Mom.x(), lay6Mom.y());
            aida.histogram1D("Weird Events: Z Mom at Layer6").fill(lay6Mom.z());
            aida.histogram2D("Weird Events: Y vs X Mom of MCParticle").fill(partMom.x(), partMom.y());
            aida.histogram1D("Weird Events: Z Mom of MCParticle").fill(partMom.z());
            
        }

        // everything in tracking frame
        public Hep3Vector doTrackExtrap(Hep3Vector currentPosition, Hep3Vector currentMomentum, double q, boolean debug) {

            double bFieldY = 0;
            Hep3Vector currentPositionDet = null;
            double distance = ecalPosition - currentPosition.x();
            
            if (debug)
                System.out.printf("Track extrap, q %f field %f distance %f pos %s mom %s \n", q, bFieldY, distance, currentPosition.toString(), currentMomentum.toString());
            
            if (stepSize == 0)
                stepSize = distance / 100.0;
            double sign = Math.signum(distance);
            distance = Math.abs(distance);

            while (distance > epsilon) {
                // The field map coordinates are in the detector frame so the
                // extrapolated track position needs to be transformed from the
                // track frame to detector.
                currentPositionDet = CoordinateTransformations.transformVectorToDetector(currentPosition);

                // Get the field at the current position along the track.
                bFieldY = bFieldMap.getField(currentPositionDet).y();
                
                // Get a trajectory (Helix or Line objects) created with the
                // track parameters at the current position.
                Trajectory trajectory = TrackUtils.getTrajectory(currentMomentum, new org.lcsim.spacegeom.SpacePoint(currentPosition), q, bFieldY);

                // Using the new trajectory, extrapolated the track by a step and
                // update the extrapolated position.
                Hep3Vector currentPositionTry = trajectory.getPointAtDistance(stepSize);
                if (debug)
                    System.out.printf("  > currentPositionTry %f %f %f , bfieldY %f \n", currentPositionTry.x(), currentPositionTry.y(), currentPositionTry.z(), bFieldY);

                if (Math.signum(currentPositionTry.x() - currentPosition.x()) != sign) {
                    System.out.println("looper, abort");
                    return null;
                }
                
                if ((Math.abs(ecalPosition - currentPositionTry.x()) > epsilon) && (Math.signum(ecalPosition - currentPositionTry.x()) != sign)) {
                    // went too far, try again with smaller step-size
                    if (Math.abs(stepSize) > 0.01) {
                        stepSize /= 2.0;
                        if (debug)
                            System.out.println(" >>went too far");
                        continue;
                    } else {
                        if (debug)
                            System.out.println(" >>had to break");
                        break;
                    }
                }
                currentPosition = currentPositionTry;
                
                if (debug) {
                    aida.histogram2D("Y vs X extrap-path").fill(currentPosition.x(), currentPosition.y());
                    aida.histogram2D("Y vs Z extrap-path").fill(currentPosition.z(), currentPosition.y());
                    aida.histogram2D("X vs Z extrap-path").fill(currentPosition.z(), currentPosition.x());
                    aida.histogram2D("Bfield Y vs Z Pos").fill(currentPosition.x(), bFieldY);
                }

                distance = Math.abs(ecalPosition - currentPosition.x());
                // Calculate the momentum vector at the new position. This will
                // be used when creating the trajectory that will be used to
                // extrapolate the track in the next iteration.
                currentMomentum = VecOp.mult(currentMomentum.magnitude(), trajectory.getUnitTangentAtLength(stepSize));
                //if (debug)
                //    System.out.printf("  > new distance %f mom %s \n", distance, currentMomentum.toString());
                
               
            }
            return currentPosition;
        }
    }
    
}
