package org.hps.recon.tracking.ztrack;

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
import junit.framework.TestCase;
import org.hps.util.Pair;
import org.hps.util.RK4integrator;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.spacegeom.CartesianPoint;
import org.lcsim.recon.tracking.spacegeom.SpacePointVector;
import org.lcsim.recon.tracking.trfbase.PropDir;
import org.lcsim.recon.tracking.trfbase.PropStat;
import org.lcsim.recon.tracking.trfbase.TrackSurfaceDirection;
import org.lcsim.recon.tracking.trfbase.TrackVector;
import org.lcsim.recon.tracking.trfbase.VTrack;
import org.lcsim.recon.tracking.trfzp.PropZZRK;
import org.lcsim.recon.tracking.trfzp.SurfZPlane;

import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.cache.FileCache;
import org.lcsim.util.loop.LCSimLoop;

/**
 *
 * @author ngraf
 */
public class RKExtrapolatorTest extends TestCase {

    static final String testURLBase = null;

    public void testIt() throws Exception {
        FileCache cache = new FileCache();
        int nEvents = 1; //-1;
        LCSimLoop loop = new LCSimLoop();
        TrackExtrapTestDriver d = new TrackExtrapTestDriver();
        loop.add(d);
        String fileName = "e-_2.3GeV_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2016-Pass2_nomsc_9k_12SimTrackerHits.slcio";
        //"e-_2.3GeV_SLIC-v06-00-00_QGSP_BERT_HPS-PhysicsRun2016-Pass2-Phantom-fieldmap_nomsc.slcio";
        File inputFile = cache.getCachedFile(new URL("http://www.lcsim.org/test/hps-java/" + fileName));
        loop.setLCIORecordSource(inputFile);
        loop.loop(nEvents);
        System.out.println("Loop processed " + loop.getTotalSupplied() + " events.");
        System.out.println("Done!");
    }

    protected class TrackExtrapTestDriver extends Driver {

        private static final String ECAL_POSITION_CONSTANT_NAME = "ecal_dface";
        FieldMap bFieldMap = null;
        private double ecalPosition = 1338.0;
        private double epsilon = 1;
        public AIDA aida = null;
        private String outputPlots = null;
        TrfField _trfField;
        HpsMagField _hpsField;

        @Override
        public void endOfData() {
            if (outputPlots != null) {
                try {
                    aida.saveAs(outputPlots);
                } catch (IOException ex) {
                    Logger.getLogger(RKExtrapolatorTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }

        public void setOutputPlots(String input) {
            outputPlots = input;
        }

        public void setEpsilon(double input) {
            epsilon = input;
        }

        @Override

        protected void detectorChanged(Detector detector) {
            bFieldMap = detector.getFieldMap();
            if (ecalPosition == 0) {
                ecalPosition = detector.getConstants().get(ECAL_POSITION_CONSTANT_NAME).getValue();
            }
            if (aida == null) {
                aida = AIDA.defaultInstance();
            }
            aida.tree().cd("/");
            setupPlots();

            _trfField = new TrfField(detector.getFieldMap());
            _hpsField = new HpsMagField(detector.getFieldMap());
        }

        private void setupPlots() {
            aida.histogram2D("Y vs X extrap-path", 1000, 500, 1500, 600, -300, 300);
            aida.histogram2D("Y vs Z extrap-path", 1000, -50, 50, 600, -300, 300);
            aida.histogram2D("X vs Z extrap-path", 1000, -50, 50, 1000, 500, 1500);
            aida.histogram2D("Bfield Y vs Z Pos", 1000, 890, ecalPosition, 220, -0.22, 0);
            aida.histogram2D("Y vs X Pos Extrapolated to ECal", 300, -300, 300, 300, -300, 300);
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
            List<SimTrackerHit> simTrackerHits = null;
            if (event.hasCollection(SimTrackerHit.class, "TrackerHits")) {
                simTrackerHits = event.get(SimTrackerHit.class, "TrackerHits");
            } else {
                return;
            }
            List<MCParticle> particles = null;
            if (event.hasCollection(MCParticle.class, "MCParticle")) {
                particles = event.get(MCParticle.class, "MCParticle");
            } else {
                return;
            }
            List<SimTrackerHit> scoringHits = null;
            if (event.hasCollection(SimTrackerHit.class, "TrackerHitsECal")) {
                scoringHits = event.get(SimTrackerHit.class, "TrackerHitsECal");
            } else {
                return;
            }
            Map<MCParticle, List<SimTrackerHit>> scoringHitMap = new HashMap<MCParticle, List<SimTrackerHit>>();
            for (SimTrackerHit scoringHit : scoringHits) {
                MCParticle part = scoringHit.getMCParticle();
                if (scoringHitMap.get(part) == null) {
                    scoringHitMap.put(part, new ArrayList<SimTrackerHit>());
                }
                scoringHitMap.get(part).add(scoringHit);
            }

            MCParticle part = particles.get(0);
            Hep3Vector mcPosition = part.getOrigin();
            Hep3Vector mcMomentum = part.getMomentum();
            // let's compare to the trf propagator
            PropZZRK prop = new PropZZRK(_trfField);
            TrackVector vec1 = new TrackVector();
            // NOTE trf uses cm, hps uses mm
            vec1.set(0, mcPosition.x() / 10.);    // x
            vec1.set(1, mcPosition.y() / 10.);    // y
            vec1.set(2, mcMomentum.x() / mcMomentum.z());   // dx/dz
            vec1.set(3, mcMomentum.y() / mcMomentum.z());   // dy/dz
            vec1.set(4, part.getCharge() / mcMomentum.magnitude());   // q/p
            // create a VTrack at the particle's origin.

            SurfZPlane zp0 = new SurfZPlane(mcPosition.z() / 10.);
            TrackSurfaceDirection tdir = TrackSurfaceDirection.TSD_FORWARD;
            VTrack trv0 = new VTrack(zp0.newPureSurface(), vec1, tdir);

            PropDir dir = PropDir.FORWARD;
            ZTrackParam parIn = new ZTrackParam();
            ZTrackParam parOut = new ZTrackParam();

            // ztrack
            double[] pars = new double[5];
            pars[0] = mcPosition.x(); //x
            pars[1] = mcPosition.y(); //y
            pars[2] = mcMomentum.x() / mcMomentum.z(); // x'
            pars[3] = mcMomentum.y() / mcMomentum.z(); // y'
            pars[4] = part.getCharge() / mcMomentum.magnitude(); // q/p
            parIn.SetStateVector(pars);
            parIn.SetZ(mcPosition.z());
            RK4TrackExtrapolator extrap = new RK4TrackExtrapolator(_hpsField);

            for (SimTrackerHit sth : simTrackerHits) {
                // get the z position of this hit...
                double z = sth.getPositionVec().z();
                Hep3Vector extrapPos = extrapolateToZ(mcPosition, mcMomentum, part.getCharge(), z, false);
                System.out.println("SimTrackerHit " + sth.getPositionVec());
                System.out.println("extrapolated  " + extrapPos);
                //trf
                VTrack trv1 = new VTrack(trv0);
                PropStat pstat = prop.vecDirProp(trv1, new SurfZPlane(z / 10.), dir);
                System.out.println("trf vecOut: " + trv1);
                //ztrack
                extrap.Extrapolate(parIn, parOut, z, null);
                System.out.println("extrap to z= " + z + " " + parOut.GetX() + " " + parOut.GetY() + " " + parOut.GetZ());

                //
                Hep3Vector pos = sth.getPositionVec();
                Hep3Vector field = bFieldMap.getField(pos);
                SpacePointVector trfFieldVal = _trfField.field(new CartesianPoint(pos.x() / 10., pos.y() / 10., pos.z() / 10.));
//                System.out.println("bFieldMapVal at " + pos + " is " + field);
//                System.out.println("trfFieldMapVal at " + pos + " is " + trfFieldVal); // mm to cm
            }
            //System.out.printf("   end pos %s \n", extrapPos.toString());

//            Map<MCParticle, List<SimTrackerHit>> trackerHitMap = MCFullDetectorTruth.BuildTrackerHitMap(trackerHits);
//            for (MCParticle part : particles) {
//                
//                if (part.getCharge() == 0) {
//                    continue;
//                }
//                if (part.getGeneratorStatus() != MCParticle.FINAL_STATE) {
//                    continue;
//                }
//                if (!part.getSimulatorStatus().isDecayedInCalorimeter()) {
//                    continue;
//                }
//                if (!trackerHitMap.containsKey(part) || (!scoringHitMap.containsKey(part))) {
//                    continue;
//                }
//                List<SimTrackerHit> hits = trackerHitMap.get(part);
//                //List<SimCalorimeterHit> caloHits = calHitMap.get(part);
//                List<SimTrackerHit> caloHitList = scoringHitMap.get(part);
//                if ((hits == null) || (caloHitList == null) || (caloHitList.isEmpty())) {
//                    continue;
//                }
//                SimTrackerHit lastHit = null;
//                int lay = 0;
//                for (SimTrackerHit hit : hits) {
//                    if (hit.getLayer() > lay) {
//                        lay = hit.getLayer();
//                        lastHit = hit;
//                    }
//                }
//
//                if (lastHit == null || lastHit.getPosition()[2] < 890) {
//                    continue;
//                }
//
//                SimTrackerHit caloHit = caloHitList.get(0);
//                Hep3Vector hitPosition = lastHit.getPositionVec();
//                Hep3Vector hitMomentum = new BasicHep3Vector(lastHit.getMomentum());
//                Hep3Vector extrapPos = doTrackExtrapRK(hitPosition, hitMomentum, part.getCharge(), false);
//                //System.out.printf("   end pos %s \n", extrapPos.toString());
//                caloHit = findClosestCaloHit(caloHitList, extrapPos);
//                Hep3Vector caloHitPos = caloHit.getPositionVec();
//                //System.out.printf("   calo hit pos %s \n", caloHitPos.toString());
//                Hep3Vector residualVec = VecOp.sub(extrapPos, caloHitPos);
//                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
//                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.x());
//                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] X vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.x());
//                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit X").fill(caloHitPos.x(), residualVec.y());
//                aida.histogram2D("[Extrapolated SimTrackerHit - SimCalorimeterHit] Y vs SimCalorimeterHit Y").fill(caloHitPos.y(), residualVec.y());
//                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.y());
//                aida.histogram2D("Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.x());
//                if (lastHit.getPosition()[1] > 0) {
//                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
//                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.y());
//                    aida.histogram2D("Top: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.x());
//                } else {
//                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
//                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.y());
//                    aida.histogram2D("Bot: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.x());
//                }
//                if (part.getCharge() > 0) {
//                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
//                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.y());
//                    aida.histogram2D("e+: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.x());
//                } else {
//                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit: Y vs X").fill(residualVec.x(), residualVec.y());
//                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit Y vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.y());
//                    aida.histogram2D("e-: Extrapolated SimTrackerHit - SimCalorimeterHit X vs MCParticle Pz").fill(abs(part.getPZ()), residualVec.x());
//                }
//            }
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

        public Hep3Vector extrapolateToZ(Hep3Vector currentPosition, Hep3Vector currentMomentum, double q, double z, boolean debug) {
            double distanceZ = z - currentPosition.z();
            double distance = distanceZ / VecOp.cosTheta(currentMomentum);
            RK4integrator RKint = new RK4integrator(q, epsilon, bFieldMap);
            Pair<Hep3Vector, Hep3Vector> RKresults = RKint.integrate(currentPosition, currentMomentum, distance);
            // System.out.printf("RKpos %s \n", RKresults.getFirstElement().toString());
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = z - RKresults.getFirstElement().z();
            Hep3Vector delta = new BasicHep3Vector(dz * mom.x() / mom.z(), dz * mom.y() / mom.z(), dz);
            Hep3Vector finalPos = VecOp.add(delta, RKresults.getFirstElement());
            return finalPos;
        }

        public Hep3Vector doTrackExtrapRK(Hep3Vector currentPosition, Hep3Vector currentMomentum, double q, boolean debug) {
            double distanceZ = ecalPosition - currentPosition.z();
            double distance = distanceZ / VecOp.cosTheta(currentMomentum);
            RK4integrator RKint = new RK4integrator(q, epsilon, bFieldMap);
            Pair<Hep3Vector, Hep3Vector> RKresults = RKint.integrate(currentPosition, currentMomentum, distance);
            // System.out.printf("RKpos %s \n", RKresults.getFirstElement().toString());
            Hep3Vector mom = RKresults.getSecondElement();
            double dz = ecalPosition - RKresults.getFirstElement().z();
            Hep3Vector delta = new BasicHep3Vector(dz * mom.x() / mom.z(), dz * mom.y() / mom.z(), dz);
            Hep3Vector finalPos = VecOp.add(delta, RKresults.getFirstElement());
            return finalPos;
        }
    }
}
