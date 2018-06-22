package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.LCIOParameters.ParameterName;
import org.lcsim.event.base.BaseTrack;
import org.lcsim.event.base.BaseTrackState;
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

    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    public KalmanInterface() {
        hitMap = new HashMap<Measurement, TrackerHit>();
        moduleMap = new HashMap<SiModule, SiStripPlane>();
        trackHitsKalman = new ArrayList<int[]>();
        SiMlist = new ArrayList<SiModule>();
        SeedTrackLayers = new ArrayList<Integer>();
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

    public TrackState createTrackState(MeasurementSite ms, int loc, boolean useSmoothed) {
        // public BaseTrackState(double[] trackParameters, double[] covarianceMatrix, double[] position, int location)
        StateVector sv = null;
        if (useSmoothed) {
            if (!ms.smoothed)
                return null;
            sv = ms.aS;
        } else {
            if (!ms.filtered)
                return null;
            sv = ms.aF;
        }

        //local params, rotated
        Vec localParams = sv.a;
        SquareMatrix fRot = new SquareMatrix(5);
        double[] globalParams = sv.rotateHelix(localParams, sv.Rot.invert(), fRot).v;
        double[] newParams = getLCSimParams(globalParams, sv.alpha);
        SquareMatrix localCov = sv.C;
        SquareMatrix globalCov = localCov.similarity(fRot);
        //checkme
        double[] newCov = getLCSimCov(globalCov, sv.alpha).asPackedArray(true);

        Vec localPosition = sv.origin.sum(sv.X0);
        Vec globalPosition = sv.toGlobal(localPosition);
        double[] globalPositionTransformed = globalPosition.leftMultiply(HpsToKalman).v;

        return new BaseTrackState(newParams, newCov, globalPositionTransformed, loc);

    }

    public BaseTrack createTrack(KalmanTrackFit2 kF, boolean useSmoothed) {
        if (kF.sites == null || kF.sites.isEmpty())
            return null;

        BaseTrack newTrack = new BaseTrack();
        // track states at each layer
        for (int i = 0; i < kF.sites.size(); i++) {
            MeasurementSite site = kF.sites.get(i);
            TrackState ts = null;
            int loc = TrackState.AtOther;

            if (i == kF.initialSite)
                loc = TrackState.AtFirstHit;
            else if (i == kF.finalSite)
                loc = TrackState.AtLastHit;

            if (site.smoothed && useSmoothed)
                ts = createTrackState(site, loc, true);
            else if (site.filtered && !useSmoothed)
                ts = createTrackState(site, loc, false);

            if (ts != null) {
                newTrack.getTrackStates().add(ts);
            }
        }

        // TODO: get track params at origin

        // TODO: get track params at ECal

        // other track properties
        newTrack.setChisq(kF.chi2s);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(kF.success);

        // take first TrackState as overall Track params
        newTrack.setTrackParameters(newTrack.getTrackStates().get(0).getParameters(), kF.sites.get(0).B);

        return newTrack;
    }

    static double[] getLCSimParams(double[] oldParams, double alpha) {
        // convert params
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0] * -1.0;
        params[ParameterName.phi0.ordinal()] = oldParams[1]; //- Math.PI / 2.0;
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;

        return params;
    }

    static SymmetricMatrix getLCSimCov(SquareMatrix oldCov, double alpha) {
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
        return cov;
    }

    private void addHitsToTrack(BaseTrack newTrack) {
        List<Measurement> measList = getMeasurements();

        for (Measurement meas : measList) {
            TrackerHit hit = hitMap.get(meas);
            if (hit != null) {
                if (!newTrack.getTrackerHits().contains(hit)) {
                    newTrack.addHit(hit);
                }
            }
        }
        newTrack.setNDF(newTrack.getTrackerHits().size());
    }

    public BaseTrack createTrack(SeedTrack trk) {

        double[] params = getLCSimParams(trk.helixParams().v, trk.getAlpha());
        SymmetricMatrix cov = getLCSimCov(trk.covariance(), trk.getAlpha());
        BaseTrack newTrack = new BaseTrack();
        newTrack.setTrackParameters(params, trk.B());
        newTrack.setCovarianceMatrix(cov);
        addHitsToTrack(newTrack);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(trk.success);

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

            double stereo = Math.abs(Math.asin(v.z()));

            Vec pointOnPlane = new Vec(3, inputPlane.origin().v());
            Vec normalToPlane = new Vec(3, w.v());
            if (normalToPlane.v[0] < 0)
                normalToPlane = normalToPlane.scale(-1.0);
            Vec normalToPlaneTransformed = normalToPlane.leftMultiply(HpsToKalman);
            Vec pointOnPlaneTransformed = pointOnPlane.leftMultiply(HpsToKalman);

            System.out.printf("HPSsensor raw info: %s : v %s w %s, origin %s, stereo %f \n", temp.getName(), v.toString(), w.toString(), inputPlane.origin().toString(), stereo);
            System.out.printf("    Building with Kalman plane: point %s normal %s \n", new BasicHep3Vector(pointOnPlaneTransformed.v).toString(), new BasicHep3Vector(normalToPlaneTransformed.v).toString());
            Plane p = new Plane(pointOnPlaneTransformed, normalToPlaneTransformed);
            SiModule newMod = new SiModule(temp.getLayerNumber(), p, temp.isStereo(), stereo, inputPlane.getWidth(), inputPlane.getLength(), inputPlane.getThickness(), fm);

            moduleMap.put(newMod, inputPlane);
            SiMlist.add(newMod);
        }
        Collections.sort(SiMlist, new SortByLayer());
    }

    private double fillMeasurements(List<TrackerHit> hits1D, int addMode) {
        double firstZ = 10000;
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitsMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();

        for (TrackerHit hit1D : hits1D) {

            HpsSiSensor temp = ((HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement());
            int lay = temp.getLayerNumber();
            //System.out.printf("filling hit1D lay %d \n", lay);
            if (addMode == 0 && !SeedTrackLayers.contains(lay / 2 + 1))
                continue;
            else if (addMode == 1 && SeedTrackLayers.contains(lay / 2 + 1))
                continue;

            ArrayList<TrackerHit> hitsInLayer = null;
            if (hitsMap.containsKey(lay)) {
                hitsInLayer = hitsMap.get(lay);
            } else {
                hitsInLayer = new ArrayList<TrackerHit>();
            }
            hitsInLayer.add(hit1D);
            if (hit1D.getPosition()[2] < firstZ)
                firstZ = hit1D.getPosition()[2];
            hitsMap.put(temp, hitsInLayer);
            //System.out.println("   put hit in map");
        }

        int modIndex = -1;
        for (SiModule mod : SiMlist) {
            modIndex++;
            //System.out.printf("modIndex %d \n", modIndex);
            //mod.hits.clear();
            SiStripPlane plane = moduleMap.get(mod);
            if (!hitsMap.containsKey(plane.getSensor()))
                continue;
            ArrayList<TrackerHit> temp = hitsMap.get(plane.getSensor());
            if (temp == null)
                continue;

            Hep3Vector planeMeasuredVec = VecOp.mult(HpsToKalmanMatrix, plane.getMeasuredCoordinate());

            for (int i = 0; i < temp.size(); i++) {
                TrackerHit hit = temp.get(i);

                SiTrackerHitStrip1D local = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                //SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

                double umeas = local.getPosition()[0];
                double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

                System.out.printf("\n filling SiMod: HPSplane MeasuredCoord %s UnmeasuredCoord %s umeas %f\n", plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), umeas);
                // if hps measured coord axis is opposite to kalman measured coord axis
                if (planeMeasuredVec.z() * mod.p.V().v[2] < 0)
                    umeas *= -1.0;

                System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(), VecOp.mult(HpsToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                mod.p.print("Corresponding KalmanPlane");

                //                
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
                //System.out.printf("    adding hitPair %d %d \n", hitPair[0], hitPair[1]);
                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }
            mod.print("SiModule-filled");
        }
        return firstZ;
    }

    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {

        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        //fillMeasurements(hitsOnTrack, 0);
        double firstHitZ = fillMeasurements(hitsOnTrack, 2);

        System.out.printf("firstHitZ %f \n", firstHitZ);
        return new SeedTrack(SiMlist, 0, trackHitsKalman, true);
        //        return new SeedTrack(SiMlist, firstHitZ, trackHitsKalman, true);
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
