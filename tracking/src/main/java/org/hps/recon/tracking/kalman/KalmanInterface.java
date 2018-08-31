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
import static org.lcsim.constants.Constants.fieldConversion;

public class KalmanInterface {

    private Map<Measurement, TrackerHit> hitMap;
    private Map<SiModule, SiStripPlane> moduleMap;
    public static SquareMatrix HpsToKalman;
    public static SquareMatrix KalmanToHps;
    public static BasicHep3Matrix HpsToKalmanMatrix;
    private ArrayList<int[]> trackHitsKalman;
    private ArrayList<SiModule> SiMlist;
    private List<Integer> SeedTrackLayers = null;
    public boolean verbose = false;

    static Vec convertMomentumToHps(Vec kalMom, double bfield) {
        return kalMom.scale(fieldConversion * bfield);
    }

    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    public KalmanInterface() {
        hitMap = new HashMap<Measurement, TrackerHit>();
        moduleMap = new HashMap<SiModule, SiStripPlane>();
        trackHitsKalman = new ArrayList<int[]>();
        SiMlist = new ArrayList<SiModule>();
        SeedTrackLayers = new ArrayList<Integer>();
        //SeedTrackLayers.add(2);
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
        KalmanToHps = HpsToKalman.invert();

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

    public void clearInterface() {
        hitMap.clear();
        trackHitsKalman.clear();
        for (SiModule SiM : SiMlist) {
            SiM.hits.clear();
        }
    }

    public static TrackState createTrackState(MeasurementSite ms, int loc, boolean useSmoothed) {
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
        double[] newCov = getLCSimCov(globalCov.M, sv.alpha).asPackedArray(true);

        return new BaseTrackState(newParams, newCov, new double[] { 0, 0, 0 }, loc);

    }

    public BaseTrack createTrack(KalTrack kT, boolean storeTrackStates) {
        if (kT.SiteList == null)
            return null;
        kT.sortSites(true);
        int prevID = 0;
        int dummyCounter = -1;

        BaseTrack newTrack = new BaseTrack();
        // track states at each layer
        for (int i = 0; i < kT.SiteList.size(); i++) {
            MeasurementSite site = kT.SiteList.get(i);
            TrackState ts = null;
            int loc = TrackState.AtOther;

            HpsSiSensor hssd = (HpsSiSensor) moduleMap.get(site.m).getSensor();
            int lay = hssd.getMillepedeId();
            //System.out.printf("ssp id %d \n", hssd.getMillepedeId());

            if (i == 0) {
                loc = TrackState.AtFirstHit;
                // add trackstate at IP as first trackstate
                // make this trackstate's params the overall track params
                ts = createTrackState(site, TrackState.AtIP, true);
                if (ts != null) {
                    newTrack.getTrackStates().add(ts);
                    // take first TrackState as overall Track params
                    newTrack.setTrackParameters(ts.getParameters(), kT.Bmag);
                    newTrack.setCovarianceMatrix(new SymmetricMatrix(5, ts.getCovMatrix(), true));
                }
            } else if (i == kT.SiteList.size() - 1)
                loc = TrackState.AtLastHit;

            if (storeTrackStates) {
                for (int k = 1; k < lay - prevID; k++) {
                    // uses new lcsim constructor
                    BaseTrackState dummy = new BaseTrackState(dummyCounter);
                    newTrack.getTrackStates().add(dummy);
                    dummyCounter--;
                }
                prevID = lay;
            }

            if (loc == TrackState.AtFirstHit || loc == TrackState.AtLastHit || storeTrackStates) {
                ts = createTrackState(site, loc, true);
                if (ts != null) {
                    newTrack.getTrackStates().add(ts);
                }
            }
        }

        // TODO: get track params at ECal

        // other track properties
        newTrack.setChisq(kT.chi2);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(true);

        return newTrack;
    }

    static double[] getLCSimParams(double[] oldParams, double alpha) {
        // convert params
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0];
        params[ParameterName.phi0.ordinal()] = -1.0 * oldParams[1];
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;
        //System.out.printf("d0 ordinal = %d\n", ParameterName.d0.ordinal());
        //System.out.printf("phi0 ordinal = %d\n", ParameterName.phi0.ordinal());
        //System.out.printf("omega ordinal = %d\n", ParameterName.omega.ordinal());
        //System.out.printf("z0 ordinal = %d\n", ParameterName.z0.ordinal());
        //System.out.printf("tanLambda ordinal = %d\n", ParameterName.tanLambda.ordinal());

        return params;
    }

    static SymmetricMatrix getLCSimCov(double[][] oldCov, double alpha) {
        double [] d = {1.0, -1.0, -1.0/alpha, -1.0, -1.0};
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i=0; i<5; i++) {
            for (int j=0; j<5; j++) {
                cov.setElement(i, j, d[i]*d[j]*oldCov[i][j]);
            }
        }
        /*
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
        */
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

        double[] newPivot = { 0., 0., 0. };
        double[] params = getLCSimParams(trk.pivotTransform(newPivot), trk.getAlpha());
        SymmetricMatrix cov = getLCSimCov(trk.covariance().M, trk.getAlpha());
        BaseTrack newTrack = new BaseTrack();
        newTrack.setTrackParameters(params, trk.B());
        newTrack.setCovarianceMatrix(cov);
        addHitsToTrack(newTrack);
        newTrack.setTrackType(BaseTrack.TrackType.Y_FIELD.ordinal());
        newTrack.setFitSuccess(trk.success);

        return newTrack;
    }

    public void createSiModules(List<SiStripPlane> inputPlanes, FieldMap fm) {
        SiMlist.clear();

        for (SiStripPlane inputPlane : inputPlanes) {

            HpsSiSensor temp = (HpsSiSensor) (inputPlane.getSensor());

            // u and v are reversed in hps compared to kalman
            Hep3Vector v = inputPlane.getUnmeasuredCoordinate();
            Hep3Vector w = inputPlane.normal();
            double stereo = Math.signum(w.x()) * (Math.asin(v.z()));
            if (temp.getName().contains("slot"))
                stereo *= -1.0;

            Vec pointOnPlane = new Vec(3, inputPlane.origin().v());
            Vec normalToPlane = new Vec(3, w.v());

            Vec normalToPlaneTransformed = normalToPlane.leftMultiply(HpsToKalman);
            Vec pointOnPlaneTransformed = pointOnPlane.leftMultiply(HpsToKalman);

            if (verbose) {
                System.out.printf("HPSsensor raw info: %s : v %s w %s, origin %s, stereo %f \n", temp.getName(), v.toString(), w.toString(), inputPlane.origin().toString(), stereo);
                System.out.printf("    Building with Kalman plane: point %s normal %s \n", new BasicHep3Vector(pointOnPlaneTransformed.v).toString(), new BasicHep3Vector(normalToPlaneTransformed.v).toString());
            }
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
            if (addMode == 0 && !SeedTrackLayers.contains((lay + 1) / 2))
                continue;
            else if (addMode == 1 && SeedTrackLayers.contains((lay + 1) / 2))
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
        }

        int modIndex = -1;
        for (SiModule mod : SiMlist) {
            modIndex++;
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
                // if hps measured coord axis is opposite to kalman measured coord axis
                if (planeMeasuredVec.z() * mod.p.V().v[2] < 0)
                    umeas *= -1.0;

                if (verbose) {
                    System.out.printf("\n filling SiMod: %s \n", plane.getName());
                    System.out.printf("HPSplane MeasuredCoord %s UnmeasuredCoord %s Normal %s umeas %f\n", plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), plane.normal().toString(), umeas);
                    System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(), VecOp.mult(HpsToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                    mod.p.print("Corresponding KalmanPlane");
                    Vec globalX = mod.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = mod.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                }
                Measurement m = new Measurement(umeas, du, new Vec(0, 0, 0), 0);

                int[] hitPair = { modIndex, mod.hits.size() };
                if (verbose)
                    System.out.printf("    adding hitPair %d %d \n", hitPair[0], hitPair[1]);

                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }
            if (verbose)
                mod.print("SiModule-filled");
        }
        return firstZ;
    }

    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {

        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        double firstHitZ = fillMeasurements(hitsOnTrack, 0);
        if (verbose)
            System.out.printf("firstHitZ %f \n", firstHitZ);
        return new SeedTrack(SiMlist, firstHitZ, trackHitsKalman, verbose);
    }

    public KalmanTrackFit2 createKalmanTrackFit(SeedTrack seed, Track track, RelationalTable hitToStrips, RelationalTable hitToRotated, FieldMap fm, int nIt) {
        double firstHitZ = 10000;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        for (TrackerHit hit1D : hitsOnTrack) {
            if (hit1D.getPosition()[2] < firstHitZ)
                firstHitZ = hit1D.getPosition()[2];
        }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
        int startIndex = 0;
        fillMeasurements(hitsOnTrack, 1);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty())
                SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (SeedTrackLayers.contains((SiM.Layer + 1) / 2) && (i > startIndex))
                startIndex = i;
            if (verbose)
                SiM.print(String.format("SiMoccupied%d", i));
        }
        //        startIndex++;

        if (verbose) {
            System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex);
        }

        SquareMatrix cov = seed.covariance();
        cov.scale(1000.0);

        return new KalmanTrackFit2(SiMoccupied, startIndex, nIt, new Vec(0., seed.yOrigin, 0.), seed.helixParams(), cov, fm, verbose);
    }

    //    public KalTrack createKalmanTrack(KalmanTrackFit2 ktf, int trackID) {
    //        return new KalTrack(trackID, ktf.sites.size(), ktf.sites, ktf.chi2s);
    //    }

    class SortByLayer implements Comparator<SiModule> {

        @Override
        public int compare(SiModule o1, SiModule o2) {
            return o1.Layer - o2.Layer;
        }
    }

}
