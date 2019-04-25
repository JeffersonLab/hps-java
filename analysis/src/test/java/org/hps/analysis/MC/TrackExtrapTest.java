package org.hps.analysis.MC;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.detector.svt.SvtDetectorSetup;
import org.hps.util.Pair;
import org.hps.util.RK4integrator;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.config.RawTrackerHitSensorSetup;
import org.lcsim.recon.tracking.digitization.sisim.config.ReadoutCleanupDriver;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

import junit.framework.TestCase;

public class TrackExtrapTest extends TestCase {
    
    static final String testURLBase = null;

    public void testIt() throws Exception
    {
        int nEvents = -1;
        String fileName = "ap_0000.slcio";
        File inputFile = null;
        if (testURLBase == null) {
            inputFile = new File(fileName);
        } else {
            URL testURL = new URL(testURLBase + "/" + fileName);
            FileCache cache = new FileCache();
            inputFile = cache.getCachedFile(testURL);
        }
        String aidaOutput = "target/test-output/TrackExtrapPlots_" + fileName.replaceAll("slcio", "aida");
        LCSimLoop loop2 = new LCSimLoop();
        
        final DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
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
        private double epsilon = 1;
        public AIDA aida = null;
        private String outputPlots = null;

        
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
        }

        
        private void setupPlots() {
            aida.histogram2D("Y vs X extrap-path", 1000, 500, 1500, 600,-300,300);
            aida.histogram2D("Y vs Z extrap-path", 1000,-50,50, 600,-300,300);
            aida.histogram2D("X vs Z extrap-path", 1000,-50,50,1000, 500, 1500);
            aida.histogram2D("Bfield Y vs Z Pos", 1000, 890, ecalPosition,220, -0.22, 0);
            
            aida.histogram2D("Y vs X Pos Extrapolated to ECal", 300,-300,300,300,-300,300);
            
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

            List<SimTrackerHit> trackerHits = null;
            if (event.hasCollection(SimTrackerHit.class, "TrackerHits"))
                trackerHits = event.get(SimTrackerHit.class, "TrackerHits");
            else
                return;

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

                Hep3Vector hitPosition = lastHit.getPositionVec();
                Hep3Vector hitMomentum = new BasicHep3Vector(lastHit.getMomentum());   
                Hep3Vector extrapPos = doTrackExtrapRK(hitPosition, hitMomentum, part.getCharge(), false);
                //System.out.printf("   end pos %s \n", extrapPos.toString());
                caloHit = findClosestCaloHit(caloHitList, extrapPos);
                Hep3Vector caloHitPos = caloHit.getPositionVec();
                //System.out.printf("   calo hit pos %s \n", caloHitPos.toString());
                Hep3Vector residualVec = VecOp.sub(extrapPos, caloHitPos);
                

                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.x());
                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.x());
                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.y());
                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.y());

                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());

                if (lastHit.getPosition()[1] > 0) {
                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                else {
                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }

                if (part.getCharge() > 0) {
                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }
                else {
                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.y());
                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(Math.abs(part.getPZ()), residualVec.x());
                }

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

        
        public Hep3Vector doTrackExtrapRK(Hep3Vector currentPosition, Hep3Vector currentMomentum, double q, boolean debug) {
            double distanceZ = ecalPosition - currentPosition.z();
            double distance = distanceZ / VecOp.cosTheta(currentMomentum);
                    
            RK4integrator RKint = new RK4integrator(q, epsilon, bFieldMap);
            Pair<Hep3Vector,Hep3Vector> RKresults = RKint.integrate(currentPosition, currentMomentum, distance);
           // System.out.printf("RKpos %s \n", RKresults.getFirstElement().toString());
          
            Hep3Vector mom = RKresults.getSecondElement();            
            double dz = ecalPosition - RKresults.getFirstElement().z();
            Hep3Vector delta = new BasicHep3Vector(dz * mom.x() / mom.z(), dz * mom.y() / mom.z(), dz);
            Hep3Vector finalPos = VecOp.add(delta, RKresults.getFirstElement());
            
            return finalPos;
        }


    }
    
}
