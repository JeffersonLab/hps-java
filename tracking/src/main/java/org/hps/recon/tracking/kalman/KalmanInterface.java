package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

public class KalmanInterface {

    private Map<Measurement, TrackerHit> hitMap;
    private Map<SiModule, SiStripPlane> moduleMap;
    public static SquareMatrix HpsToKalman;
    public static BasicHep3Matrix HpsToKalmanMatrix;
    private ArrayList<int[]> trackHitsKalman;
    private ArrayList<SiModule> SiMlist;
    private List<Integer> SeedTrackLayers = null;

    public Helix getHelix(double yOrig, Vec mom, FieldMap fM) {
        //double yOrig = 0;
        //double p = 1.0;
        //        double Phi = 91. * Math.PI / 180.;
        //        double Theta = 85. * Math.PI / 180.;
        //        Vec initialDirection = new Vec(Math.cos(Phi) * Math.sin(Theta), Math.sin(Phi) * Math.sin(Theta), Math.cos(Theta));
        //        Vec momentum = new Vec(p * initialDirection.v[0], p * initialDirection.v[1], p * initialDirection.v[2]);
        Vec helixOrigin = new Vec(0., yOrig, 0.);
        return new Helix(1.0, helixOrigin, mom, helixOrigin, fM);
    }

    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    public KalmanInterface() {
        hitMap = new HashMap<Measurement, TrackerHit>();
        moduleMap = new HashMap<SiModule, SiStripPlane>();
        trackHitsKalman = new ArrayList<int[]>();
        SiMlist = new ArrayList<SiModule>();
        SeedTrackLayers.add(3);
        SeedTrackLayers.add(4);
        SeedTrackLayers.add(5);
        double[][] HpsToKalmanVals = { { 0, 1, 0 }, { 1, 0, 0 }, { 0, 0, -1 } };
        HpsToKalman = new SquareMatrix(3, HpsToKalmanVals);
        HpsToKalmanMatrix = new BasicHep3Matrix();
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++)
                HpsToKalmanMatrix.setElement(i, j, HpsToKalmanVals[i][j]);
        }

    }

    public ArrayList<SiModule> getSiModuleList() {
        return SiMlist;
    }

    public ArrayList<Measurement> getMeasurements() {
        ArrayList<Measurement> measList = new ArrayList<Measurement>();
        int modIndex = 0;
        for (SiModule SiM : SiMlist) {
            for (int[] hitPair : trackHitsKalman) {
                if (hitPair[0] == modIndex) {
                    measList.add(SiM.hits.get(hitPair[1]));
                }
            }
            modIndex++;
        }
        return measList;
    }

    public void clearTrack() {
        hitMap.clear();
        trackHitsKalman.clear();
        for (SiModule SiM : SiMlist) {
            SiM.hits.clear();
        }
    }

    public BaseTrack createTrack(KalmanTrackFit2 kF, StateVector sv, Helix refHelix) {
        BaseTrack newTrack = new BaseTrack();

        // local params & cov:
        //        Vec afF = sites.get(sites.size() - 1).aF.a;
        //        Vec afC = sites.get(sites.size() - 1).aF.helixErrors();

        // rotated params
        Vec newPivot = sv.toLocal(refHelix.origin.sum(refHelix.X0));
        Vec aF = sv.pivotTransform(newPivot);
        SquareMatrix fRot = new SquareMatrix(5);
        RotMatrix Rcombo = refHelix.R.multiply(sv.Rot.invert());
        aF = sv.rotateHelix(aF, Rcombo, fRot);

        // rotated cov
        SquareMatrix aFC = sv.covariancePivotTransform(aF);
        aFC = aFC.similarity(fRot);

        newTrack = createTrack(aF.v, sv.alpha, sv.B, aFC);

        return newTrack;
    }

    public BaseTrack createTrack(double[] oldParams, double alpha, double B, SquareMatrix oldCov) {
        BaseTrack newTrack = new BaseTrack();

        // convert params
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0]; //*-1.0
        params[ParameterName.phi0.ordinal()] = oldParams[1]; //- Math.PI / 2.0;
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;

        // TODO: fix omega cov
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i <= 2; i++) {
            for (int j = 0; j <= 2; j++) {
                cov.setElement(i, j, oldCov.M[i][j]);
            }
        }
        for (int i = 3; i <= 4; i++) {
            for (int j = 0; j <= 4; j++) {
                cov.setElement(i, j, oldCov.M[j][i]);
                cov.setElement(j, i, oldCov.M[i][j]);
            }
        }

        newTrack.setTrackParameters(params, B);
        newTrack.setCovarianceMatrix(cov);
        return newTrack;
    }

    public BaseTrack createTrack(SeedTrack trk) {
        List<Measurement> measList = getMeasurements();

        BaseTrack newTrack = createTrack(trk.helixParams().v, trk.getAlpha(), trk.B(), trk.covariance());

        newTrack.setFitSuccess(trk.success);
        List<TrackerHit> hitsOnTrack = new ArrayList<TrackerHit>();
        for (Measurement meas : measList) {
            TrackerHit hit = hitMap.get(meas);
            if (hit != null) {
                if (!hitsOnTrack.contains(hit)) {
                    newTrack.addHit(hit);
                    hitsOnTrack.add(hit);
                }
            }
        }
        newTrack.setNDF(measList.size());

        return newTrack;
    }

    public void createSiModules(List<SiStripPlane> inputPlanes, FieldMap fm) {
        // SiModule(int Layer, Plane p, double stereo, double width, double height, double thickness, FieldMap Bfield) {
        SiMlist.clear();
        //double stereo = 0;

        for (SiStripPlane inputPlane : inputPlanes) {

            HpsSiSensor temp = (HpsSiSensor) (inputPlane.getSensor());

            // u and v are reversed in hps compared to kalman
            //Hep3Vector u = inputPlane.getMeasuredCoordinate();
            Hep3Vector v = inputPlane.getUnmeasuredCoordinate();
            if (temp.getName().contains("slot"))
                VecOp.mult(-1.0, v);
            Hep3Vector w = inputPlane.normal();

            double stereo = 0;
            if (temp.isStereo()) {
                stereo = Math.abs(Math.acos(v.y()));
                stereo *= -1.0 * Math.signum(v.z());
            }

            Vec pointOnPlane = new Vec(3, inputPlane.origin().v());
            Vec normalToPlane = new Vec(3, w.v());
            if (normalToPlane.v[0] < 0)
                normalToPlane = normalToPlane.scale(-1.0);
            Vec normalToPlaneTransformed = normalToPlane.leftMultiply(HpsToKalman);
            Vec pointOnPlaneTransformed = pointOnPlane.leftMultiply(HpsToKalman);

            //System.out.printf("sensor %s : u %s v %s, stereo %f \n", temp.getName(), u.toString(), v.toString(), stereo);
            Plane p = new Plane(pointOnPlaneTransformed, normalToPlaneTransformed);
            SiModule newMod = new SiModule(temp.getLayerNumber(), p, stereo, inputPlane.getWidth(), inputPlane.getLength(), inputPlane.getThickness(), fm);
            //p.print("plane");
            moduleMap.put(newMod, inputPlane);
            SiMlist.add(newMod);
        }
        Collections.sort(SiMlist, new SortByLayer());
    }

    public void fillMeasurements(List<TrackerHit> hits1D, int addMode) {

        Map<HpsSiSensor, ArrayList<TrackerHit>> hitsMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();

        for (TrackerHit hit1D : hits1D) {
            HpsSiSensor temp = ((HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement());
            int lay = temp.getLayerNumber();
            if (addMode == 0 && !SeedTrackLayers.contains(lay))
                continue;
            else if (addMode == 1 && SeedTrackLayers.contains(lay))
                continue;

            ArrayList<TrackerHit> hitsInLayer = null;
            if (hitsMap.containsKey(lay)) {
                hitsInLayer = hitsMap.get(lay);
            } else {
                hitsInLayer = new ArrayList<TrackerHit>();
            }
            hitsInLayer.add(hit1D);
            hitsMap.put(temp, hitsInLayer);
        }

        int modIndex = -1;
        for (SiModule mod : SiMlist) {
            modIndex++;
            //mod.hits.clear();
            SiStripPlane plane = moduleMap.get(mod);
            if (!hitsMap.containsKey(plane.getSensor()))
                continue;
            ArrayList<TrackerHit> temp = hitsMap.get(plane.getSensor());
            if (temp == null)
                continue;
            for (int i = 0; i < temp.size(); i++) {
                TrackerHit hit = temp.get(i);

                SiTrackerHitStrip1D local = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                //SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

                double umeas = local.getPosition()[0];
                double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

                // if hps measured coord axis is opposite to flipped kalman measured coord axis
                if (plane.getMeasuredCoordinate().z() * mod.p.V().v[2] > 0)
                    umeas *= -1.0;

                //                System.out.printf("hit local umeas %f du %f \n", umeas, du);
                //                System.out.printf("hit global position: %f %f %f \n", global.getPosition()[0], global.getPosition()[1], global.getPosition()[2]);
                // hit position
                //                System.out.printf("hit raw position: %f %f %f \n", hit.getPosition()[0], hit.getPosition()[1], hit.getPosition()[2]);
                //                Vec hitPos0 = new Vec(3, CoordinateTransformations.transformVectorToTracking(new BasicHep3Vector(hit.getPosition())).v());
                //                System.out.printf("hit transformed position: %f %f %f \n", hitPos0.v[0], hitPos0.v[1], hitPos0.v[2]);
                //                Vec hitPos = hitPos0.leftMultiply(HpsToKalman);
                //                System.out.printf("hit kalman position: %f %f %f \n", hitPos.v[0], hitPos.v[1], hitPos.v[2]);
                //                Vec hitPos2 = hitPos.dif(mod.p.X());
                //                System.out.printf("hit subtracted position: %f %f %f \n", hitPos2.v[0], hitPos2.v[1], hitPos2.v[2]);
                //                Vec hitPosTransformed = mod.Rinv.rotate(hitPos2);
                //Vec hitPosTransformed = mod.toLocal(hitPos);

                // uncertainty on position
                //RotMatrix rm = mod.Rinv;
                //rm.print("modRinv");

                //                double[][] r = rm.M;
                //                Hep3Matrix rotMat = new BasicHep3Matrix(r[0][0], r[0][1], r[0][2], r[1][0], r[1][1], r[1][2], r[2][0], r[2][1], r[2][2]);
                //                rotMat = VecOp.mult(rotMat, HpsToKalmanMatrix);
                //                SymmetricMatrix cov = new SymmetricMatrix(3, hit.getCovMatrix(), true);
                //                double sigma2 = MatrixOp.mult(MatrixOp.mult(rotMat, cov), MatrixOp.inverse(rotMat)).e(1, 1);
                Measurement m = new Measurement(umeas, du, new Vec(0, 0, 0), 0);

                //System.out.printf("rotMat %s \n", rotMat.toString());
                //System.out.printf("cov %s \n", cov.toString());

                int[] hitPair = { modIndex, mod.hits.size() };
                //System.out.printf("adding hitPair %d %d \n", hitPair[0], hitPair[1]);
                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }

        }
    }

    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {
        double firstHitZ = 100000;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        fillMeasurements(hitsOnTrack, 0);

        for (TrackerHit hit1D : hitsOnTrack) {
            HpsSiSensor temp = ((HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement());
            int lay = temp.getLayerNumber();
            if (SeedTrackLayers.contains(lay)) {
                if (hit1D.getPosition()[2] < firstHitZ)
                    firstHitZ = hit1D.getPosition()[2];
            }
        }

        return new SeedTrack(SiMlist, firstHitZ, trackHitsKalman, false);
    }

    public KalmanTrackFit2 createKalmanTrackFit(SeedTrack seed, Track track, RelationalTable hitToStrips, RelationalTable hitToRotated, FieldMap fm, int nIt) {
        double firstHitZ = 10000;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        for (TrackerHit hit1D : hitsOnTrack) {
            if (hit1D.getPosition()[2] < firstHitZ)
                firstHitZ = hit1D.getPosition()[2];
        }
        fillMeasurements(hitsOnTrack, 1);

        SquareMatrix cov = seed.covariance();
        cov.scale(1000.0);

        return new KalmanTrackFit2(SiMlist, 0, nIt, new Vec(0., firstHitZ, 0.), seed.helixParams(), cov, fm, false);
    }

    class SortByLayer implements Comparator<SiModule> {

        @Override
        public int compare(SiModule o1, SiModule o2) {
            return o1.Layer - o2.Layer;
        }
    }

}
