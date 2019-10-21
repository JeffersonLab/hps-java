package org.hps.recon.tracking.kalman;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

// This class provides an interface between hps-java and the Kalman Filter fitting and pattern recognition code.
// It can be used to refit the hits on an existing hps track, or it can be used to drive the pattern recognition.
// However, both cannot be done at the same time. The interface must be reset between doing one and the other. 

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
import org.lcsim.event.EventHeader;
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
    public boolean verbose;

    public HpsSiSensor getHpsSensor(SiModule kalmanSiMod) {
        if (moduleMap == null) return null;
        else {
            SiStripPlane temp = moduleMap.get(kalmanSiMod);
            if (temp == null) return null;
            else return (HpsSiSensor) (temp.getSensor());
        }
    }

    static Vec getField(Vec kalPos, org.lcsim.geometry.FieldMap hpsFm) {
        if (FieldMap.class.isInstance(hpsFm)) { return ((FieldMap) (hpsFm)).getField(kalPos); }

        double[] hpsPos = { kalPos.v[0], -1.0 * kalPos.v[2], kalPos.v[1] };
        double[] hpsField = hpsFm.getField(hpsPos);
        return new Vec(hpsField[0], hpsField[2], -1.0 * hpsField[1]);
    }

    static Vec convertMomentumToHps(Vec kalMom, double bfield) {
        return kalMom.scale(fieldConversion * bfield);
    }

    public void setSeedTrackLayers(List<Integer> input) {
        SeedTrackLayers = input;
    }

    public KalmanInterface() {
        this(false);
    }

    public KalmanInterface(boolean verbose) {
        hitMap = new HashMap<Measurement, TrackerHit>();
        moduleMap = new HashMap<SiModule, SiStripPlane>();
        trackHitsKalman = new ArrayList<int[]>();
        SiMlist = new ArrayList<SiModule>();
        SeedTrackLayers = new ArrayList<Integer>();
        // SeedTrackLayers.add(2);
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
        this.verbose = verbose;
    }

    static Vec vectorGlbToKalman(double[] HPSvec) { // Convert a vector from global coordinates to Kalman coordinates
        Vec kalVec = new Vec(HPSvec[0], HPSvec[2], -HPSvec[1]);
        return kalVec;
    }

    public ArrayList<SiModule> getSiModuleList() {
        return SiMlist;
    }

    public ArrayList<Measurement> getMeasurements() {
        ArrayList<Measurement> measList = new ArrayList<Measurement>();
        int modIndex = 0;
        for (SiModule SiM : SiMlist) {
            for (int[] hitPair : trackHitsKalman) {
                if (hitPair[0] == modIndex) measList.add(SiM.hits.get(hitPair[1]));
            }
            modIndex++;
        }
        return measList;
    }

    public void clearInterface() {
        if (verbose) System.out.println("Clearing the Kalman interface\n");
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
            if (!ms.smoothed) return null;
            sv = ms.aS;
        } else {
            if (!ms.filtered) return null;
            sv = ms.aF;
        }

        // local params, rotated
        Vec localParams = sv.a;
        SquareMatrix fRot = new SquareMatrix(5);
        double[] globalParams = StateVector.rotateHelix(localParams, sv.Rot.invert(), fRot).v;
        double[] newParams = getLCSimParams(globalParams, sv.alpha);
        SquareMatrix localCov = sv.C;
        SquareMatrix globalCov = localCov.similarity(fRot);
        double[] newCov = getLCSimCov(globalCov.M, sv.alpha).asPackedArray(true);

        return new BaseTrackState(newParams, newCov, new double[] { 0, 0, 0 }, loc);
    }

    public BaseTrack createTrack(KalTrack kT, boolean storeTrackStates) {
        if (kT.SiteList == null) return null;
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
            // System.out.printf("ssp id %d \n", hssd.getMillepedeId());

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
            } else if (i == kT.SiteList.size() - 1) loc = TrackState.AtLastHit;

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
                if (ts != null) { newTrack.getTrackStates().add(ts); }
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
        // convert helix parameters from Kalman to LCSim
        double[] params = new double[5];
        params[ParameterName.d0.ordinal()] = oldParams[0];
        params[ParameterName.phi0.ordinal()] = -1.0 * oldParams[1];
        params[ParameterName.omega.ordinal()] = oldParams[2] / alpha * -1.0;
        params[ParameterName.z0.ordinal()] = oldParams[3] * -1.0;
        params[ParameterName.tanLambda.ordinal()] = oldParams[4] * -1.0;
        // System.out.printf("d0 ordinal = %d\n", ParameterName.d0.ordinal());
        // System.out.printf("phi0 ordinal = %d\n", ParameterName.phi0.ordinal());
        // System.out.printf("omega ordinal = %d\n", ParameterName.omega.ordinal());
        // System.out.printf("z0 ordinal = %d\n", ParameterName.z0.ordinal());
        // System.out.printf("tanLambda ordinal = %d\n",
        // ParameterName.tanLambda.ordinal());

        return params;
    }

    static double[] unGetLCSimParams(double[] oldParams, double alpha) {
        // convert helix parameters from LCSim to Kalman
        double[] params = new double[5];
        params[0] = oldParams[ParameterName.d0.ordinal()];
        params[1] = -1.0 * oldParams[ParameterName.phi0.ordinal()];
        params[2] = oldParams[ParameterName.omega.ordinal()] * alpha * -1.0;
        params[3] = oldParams[ParameterName.z0.ordinal()] * -1.0;
        params[4] = oldParams[ParameterName.tanLambda.ordinal()] * -1.0;
        return params;
    }

    static SymmetricMatrix getLCSimCov(double[][] oldCov, double alpha) {
        double[] d = { 1.0, -1.0, -1.0 / alpha, -1.0, -1.0 };
        SymmetricMatrix cov = new SymmetricMatrix(5);
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                cov.setElement(i, j, d[i] * d[j] * oldCov[i][j]);
            }
        }
        /*
         * for (int i = 0; i <= 2; i++) { for (int j = 0; j <= 2; j++) {
         * cov.setElement(i, j, oldCov.M[i][j]); } } for (int i = 3; i <= 4; i++) { for
         * (int j = 0; j <= 4; j++) { cov.setElement(i, j, oldCov.M[j][i]);
         * cov.setElement(j, i, oldCov.M[i][j]); } }
         */
        return cov;
    }

    static double[][] ungetLCSimCov(double[] oldCov, double alpha) {
        double[] d = { 1.0, -1.0, -1.0 * alpha, -1.0, -1.0 };
        double[][] cov = new double[5][5];
        cov[0][0] = oldCov[0] * d[0] * d[0];
        cov[1][0] = oldCov[1] * d[1] * d[0];
        cov[1][1] = oldCov[2] * d[1] * d[1];
        cov[2][0] = oldCov[3] * d[2] * d[0];
        cov[2][1] = oldCov[4] * d[2] * d[1];
        cov[2][2] = oldCov[5] * d[2] * d[2];
        cov[3][0] = oldCov[6] * d[3] * d[0];
        cov[3][1] = oldCov[7] * d[3] * d[1];
        cov[3][2] = oldCov[8] * d[3] * d[2];
        cov[3][3] = oldCov[9] * d[3] * d[3];
        cov[4][0] = oldCov[10] * d[4] * d[0];
        cov[4][1] = oldCov[11] * d[4] * d[1];
        cov[4][2] = oldCov[12] * d[4] * d[2];
        cov[4][3] = oldCov[13] * d[4] * d[3];
        cov[4][4] = oldCov[14] * d[4] * d[4];
        for (int i = 0; i < 5; ++i) {
            for (int j = i + 1; j < 5; ++j) {
                cov[i][j] = cov[j][i];
            }
        }
        return cov;
    }

    private void addHitsToTrack(BaseTrack newTrack) {
        List<Measurement> measList = getMeasurements();

        for (Measurement meas : measList) {
            TrackerHit hit = hitMap.get(meas);
            if (hit != null) { 
                if (!newTrack.getTrackerHits().contains(hit)) newTrack.addHit(hit); 
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

    // Method to create one SiModule object for each silicon-strip detector
    public void createSiModules(List<SiStripPlane> inputPlanes, org.lcsim.geometry.FieldMap fm) {
        SiMlist.clear();

        for (SiStripPlane inputPlane : inputPlanes) {

            HpsSiSensor temp = (HpsSiSensor) (inputPlane.getSensor());

            // u and v are reversed in hps compared to kalman
            Hep3Vector v = inputPlane.getUnmeasuredCoordinate();
            Hep3Vector w = inputPlane.normal();
            double stereo = Math.signum(w.x()) * (Math.asin(v.z()));
            if (temp.getName().contains("slot")) stereo *= -1.0;

            Vec pointOnPlane = new Vec(3, inputPlane.origin().v());
            Vec normalToPlane = new Vec(3, w.v());

            Vec normalToPlaneTransformed = normalToPlane.leftMultiply(HpsToKalman);
            Vec pointOnPlaneTransformed = pointOnPlane.leftMultiply(HpsToKalman);

            if (verbose) {
                System.out.printf("HPSsensor raw info: %s : v %s w %s, origin %s, stereo %f \n", temp.getName(), v.toString(), w.toString(),
                        inputPlane.origin().toString(), stereo);
                System.out.printf("    Building with Kalman plane: point %s normal %s \n",
                        new BasicHep3Vector(pointOnPlaneTransformed.v).toString(), new BasicHep3Vector(normalToPlaneTransformed.v).toString());
            }
            Plane p = new Plane(pointOnPlaneTransformed, normalToPlaneTransformed);
            int kalLayer = temp.getLayerNumber()+1;  // Not sure this transformation will work when lyr0 is implemented!!
            if (kalLayer > 13) {
                System.out.format("***KalmanInterface.createSiModules Warning: Kalman layer %d out of range.***\n", kalLayer);
            }
            SiModule newMod = new SiModule(kalLayer, p, temp.isStereo(), stereo, inputPlane.getWidth(), inputPlane.getLength(),
                    inputPlane.getThickness(), fm);
            moduleMap.put(newMod, inputPlane);
            SiMlist.add(newMod);
        }
        Collections.sort(SiMlist, new SortByLayer());
    }

    // Method to fill all Si hits into the SiModule objects, to feed to the pattern recognition.
    private boolean fillAllMeasurements(EventHeader event) {
        boolean success = false;

        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        List<TrackerHit> striphits = event.get(TrackerHit.class, stripHitInputCollectionName);

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        for (TrackerHit hit1D : striphits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement();

            ArrayList<TrackerHit> hitsInSensor = null;
            if (hitSensorMap.containsKey(sensor)) {
                hitsInSensor = hitSensorMap.get(sensor);
            } else {
                hitsInSensor = new ArrayList<TrackerHit>();
            }
            hitsInSensor.add(hit1D);
            hitSensorMap.put(sensor, hitsInSensor);
        }

        int hitsFilled = 0;
        for (int modIndex = 0; modIndex < SiMlist.size(); ++modIndex) {
            SiModule module = SiMlist.get(modIndex);

            SiStripPlane plane = moduleMap.get(module);
            if (!hitSensorMap.containsKey(plane.getSensor())) continue;
            ArrayList<TrackerHit> hitsInSensor = hitSensorMap.get(plane.getSensor());
            if (hitsInSensor == null) continue;

            Hep3Vector planeMeasuredVec = VecOp.mult(HpsToKalmanMatrix, plane.getMeasuredCoordinate());

            for (int i = 0; i < hitsInSensor.size(); i++) {
                TrackerHit hit = hitsInSensor.get(i);

                SiTrackerHitStrip1D localHit = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                // SiTrackerHitStrip1D global = (new
                // SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

                double umeas = localHit.getPosition()[0];
                double du = Math.sqrt(localHit.getCovarianceAsMatrix().diagonal(0));

                // If HPS measured coordinate axis is opposite to kalman measured coordinate axis
                if (planeMeasuredVec.z() * module.p.V().v[2] < 0) umeas *= -1.0;

                if (verbose) {
                    System.out.format("\nKalmanInterface:fillAllMeasurements Measurement %d, the measurement uncertainty is set to %10.7f\n", i,
                            du);
                    System.out.printf("Filling SiMod: %s \n", plane.getName());
                    System.out.printf("HPSplane MeasuredCoord %s UnmeasuredCoord %s Normal %s umeas %f\n",
                            plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), plane.normal().toString(),
                            umeas);
                    System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(),
                            VecOp.mult(HpsToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                    module.p.print("Corresponding KalmanPlane");
                    Vec globalX = module.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = module.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                }
                Measurement m = new Measurement(umeas, du);

                int[] hitPair = { modIndex, module.hits.size() };
                if (verbose) System.out.format("    adding hitPair %d %d \n", hitPair[0], hitPair[1]);
                trackHitsKalman.add(hitPair);
                module.addMeasurement(m);
                hitMap.put(m, hit);
                hitsFilled++;
            }
            if (verbose) { module.print("SiModule-filled"); }
        }
        if (hitsFilled > 0) success = true;
        if (verbose) System.out.format("KalmanInterface.fillAllMeasurements: %d hits were filled into Si Modules\n", hitsFilled);

        return success;
    }

    // Method to fill the Si hits into the SiModule objects for a given track, in order to refit the track
    private double fillMeasurements(List<TrackerHit> hits1D, int addMode) {
        double firstZ = 10000;
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitsMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();

        for (TrackerHit hit1D : hits1D) {
            HpsSiSensor temp = ((HpsSiSensor) ((RawTrackerHit) hit1D.getRawHits().get(0)).getDetectorElement());
            int lay = temp.getLayerNumber();
            if (addMode == 0 && !SeedTrackLayers.contains((lay + 1) / 2)) continue;
            else if (addMode == 1 && SeedTrackLayers.contains((lay + 1) / 2)) continue;

            ArrayList<TrackerHit> hitsInLayer = null;
            if (hitsMap.containsKey(temp)) {
                hitsInLayer = hitsMap.get(temp);
            } else {
                hitsInLayer = new ArrayList<TrackerHit>();
            }
            hitsInLayer.add(hit1D);
            if (hit1D.getPosition()[2] < firstZ) firstZ = hit1D.getPosition()[2];
            hitsMap.put(temp, hitsInLayer);
        }

        int modIndex = -1;
        for (SiModule mod : SiMlist) {
            modIndex++;
            SiStripPlane plane = moduleMap.get(mod);
            if (!hitsMap.containsKey(plane.getSensor())) { continue; }
            ArrayList<TrackerHit> temp = hitsMap.get(plane.getSensor());
            if (temp == null) { continue; }

            Hep3Vector planeMeasuredVec = VecOp.mult(HpsToKalmanMatrix, plane.getMeasuredCoordinate());

            for (int i = 0; i < temp.size(); i++) {
                TrackerHit hit = temp.get(i);

                SiTrackerHitStrip1D local = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                // SiTrackerHitStrip1D global = (new
                // SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

                double umeas = local.getPosition()[0];
                double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

                // if hps measured coord axis is opposite to kalman measured coord axis
                if (planeMeasuredVec.z() * mod.p.V().v[2] < 0) { umeas *= -1.0; }

                if (verbose) {
                    System.out.format("\nKalmanInterface:fillMeasurements Measurement %d, the measurement uncertainty is set to %10.7f\n", i,
                            du);
                    System.out.printf("Filling SiMod: %s \n", plane.getName());
                    System.out.printf("HPSplane MeasuredCoord %s UnmeasuredCoord %s Normal %s umeas %f\n",
                            plane.getMeasuredCoordinate().toString(), plane.getUnmeasuredCoordinate().toString(), plane.normal().toString(),
                            umeas);
                    System.out.printf(" converted to Kalman Coords  Measured %s Unmeasured %s umeas %f \n", planeMeasuredVec.toString(),
                            VecOp.mult(HpsToKalmanMatrix, plane.getUnmeasuredCoordinate()).toString(), umeas);
                    mod.p.print("Corresponding KalmanPlane");
                    Vec globalX = mod.R.rotate(new Vec(1, 0, 0));
                    Vec globalY = mod.R.rotate(new Vec(0, 1, 0));
                    globalX.print("globalX");
                    globalY.print("globalY");
                }
                Measurement m = new Measurement(umeas, du);

                int[] hitPair = { modIndex, mod.hits.size() };
                if (verbose) { System.out.printf("    adding hitPair %d %d \n", hitPair[0], hitPair[1]); }
                trackHitsKalman.add(hitPair);
                mod.addMeasurement(m);
                hitMap.put(m, hit);

            }
            if (verbose) { mod.print("SiModule-filled"); }
        }
        return firstZ;
    }

    public SeedTrack createKalmanSeedTrack(Track track, RelationalTable hitToStrips, RelationalTable hitToRotated) {

        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        double firstHitZ = fillMeasurements(hitsOnTrack, 0);
        if (verbose) { System.out.printf("firstHitZ %f \n", firstHitZ); }
        return new SeedTrack(SiMlist, firstHitZ, trackHitsKalman, verbose);
    }

    // Method to refit an existing track's hits, using the Kalman seed-track to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, SeedTrack seed, Track track, RelationalTable hitToStrips,
            RelationalTable hitToRotated, org.lcsim.geometry.FieldMap fm, int nIt) {
        double firstHitZ = 10000.;
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (verbose) { System.out.format("createKalmanTrackFit: number of hits on track = %d\n", hitsOnTrack.size()); }
        for (TrackerHit hit1D : hitsOnTrack) {
            if (hit1D.getPosition()[2] < firstHitZ) firstHitZ = hit1D.getPosition()[2];
        }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
        int startIndex = 0;
        fillMeasurements(hitsOnTrack, 1);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (SeedTrackLayers.contains((SiM.Layer + 1) / 2) && (i > startIndex)) { startIndex = i; }
            if (verbose) { SiM.print(String.format("SiMoccupied%d", i)); }
        }
        // startIndex++;

        if (verbose) { System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); }

        SquareMatrix cov = seed.covariance();
        cov.scale(1000.0);

        return new KalmanTrackFit2(evtNumb, SiMoccupied, startIndex, nIt, new Vec(0., seed.yOrigin, 0.), seed.helixParams(), cov, fm, verbose);
    }

    // Method to refit an existing track, using the track's helix parameters and covariance to initialize the Kalman Filter.
    public KalmanTrackFit2 createKalmanTrackFit(int evtNumb, Vec helixParams, Vec pivot, SquareMatrix cov, Track track,
            RelationalTable hitToStrips, RelationalTable hitToRotated, org.lcsim.geometry.FieldMap fm, int nIt) {
        List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(track, hitToStrips, hitToRotated);
        if (verbose) { System.out.format("createKalmanTrackFit: using GBL fit as start; number of hits on track = %d\n", hitsOnTrack.size()); }

        ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();

        fillMeasurements(hitsOnTrack, 2);
        for (SiModule SiM : SiMlist) {
            if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
        }
        Collections.sort(SiMoccupied, new SortByLayer());

        for (int i = 0; i < SiMoccupied.size(); i++) {
            SiModule SiM = SiMoccupied.get(i);
            if (verbose) SiM.print(String.format("SiMoccupied%d", i));
        }

        int startIndex = 0;
        if (verbose) { System.out.printf("createKTF: using %d SiModules, startIndex %d \n", SiMoccupied.size(), startIndex); }
        cov.scale(1000.0);
        return new KalmanTrackFit2(evtNumb, SiMoccupied, startIndex, nIt, pivot, helixParams, cov, fm, verbose);
    }

    // public KalTrack createKalmanTrack(KalmanTrackFit2 ktf, int trackID) {
    // return new KalTrack(trackID, ktf.sites.size(), ktf.sites, ktf.chi2s);
    // }

    class SortByLayer implements Comparator<SiModule> {

        @Override
        public int compare(SiModule o1, SiModule o2) {
            return o1.Layer - o2.Layer;
        }
    }

    // Method to drive the Kalman-Filter based pattern recognition
    public ArrayList<KalmanPatRecHPS> KalmanPatRec(EventHeader event) {
        if (!fillAllMeasurements(event)) {
            System.out.format("KalmanInterface.KalmanPatRec: failed to fill measurements in detector.\n");
            return null;
        }

        int evtNum = event.getEventNumber();
        
        ArrayList<KalmanPatRecHPS> outList = new ArrayList<KalmanPatRecHPS>(2);
        for (int topBottom=0; topBottom<2; ++topBottom) {
            ArrayList<SiModule> SiMoccupied = new ArrayList<SiModule>();
            for (SiModule SiM : SiMlist) {
                if (topBottom == 0) {
                    if (SiM.p.X().v[2] < 0) continue;
                } else {
                    if (SiM.p.X().v[2] > 0) continue;
                }
                if (!SiM.hits.isEmpty()) SiMoccupied.add(SiM);
            }
            Collections.sort(SiMoccupied, new SortByLayer());

            if (verbose) {
                for (int i = 0; i < SiMoccupied.size(); i++) {
                    SiModule SiM = SiMoccupied.get(i);
                    SiM.print(String.format("SiMoccupied Number %d for topBottom=%d", i, topBottom));
                }
            }
            KalmanPatRecHPS kPat = new KalmanPatRecHPS(SiMoccupied, topBottom, evtNum, true);
            outList.add(kPat);
        }
        return outList;
    }
    
    // This method makes a Gnuplot file to display the Kalman tracks and hits in 3D.
    public void plotKalmanEvent(String path, EventHeader event, ArrayList<KalmanPatRecHPS> patRecList) {
        PrintWriter printWriter3 = null;
        int eventNumber = event.getEventNumber();
        String fn = String.format("%shelix3_%d.gp", path, eventNumber);
        System.out.format("KalmanPatRecDriver.plotKalmanEvent: Outputting single event plot to file %s\n", fn);
        File file3 = new File(fn);
        file3.getParentFile().mkdirs();
        try {
            printWriter3 = new PrintWriter(file3);
        } catch (FileNotFoundException e1) {
            System.out.format("KalmanPatRecDriver.plotKalmanEvent: could not create the gnuplot output file %s", fn);
            e1.printStackTrace();
            return;
        }
        // printWriter3.format("set xrange [-500.:1500]\n");
        // printWriter3.format("set yrange [-1000.:1000.]\n");
        printWriter3.format("set title 'Event Number %d'\n", eventNumber);
        printWriter3.format("set xlabel 'X'\n");
        printWriter3.format("set ylabel 'Y'\n");
        for (KalmanPatRecHPS patRec : patRecList) {
            for (KalTrack tkr : patRec.TkrList) {
                printWriter3.format("$tkr%d_%d << EOD\n", tkr.ID, patRec.topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    StateVector aS = site.aS;
                    SiModule module = site.m;
                    if (aS == null) {
                        System.out.println("KalmanInterface.plotKalmanEvent: missing track state pointer.");
                        site.print(" bad site ");
                        continue;
                    }
                    if (module == null) {
                        System.out.println("KalmanInterface.plotKalmanEvent: missing module pointer.");
                        site.print(" bad site ");
                        continue;
                    }
                    double phiS = aS.planeIntersect(module.p);
                    if (Double.isNaN(phiS)) continue;
                    Vec rLocal = aS.atPhi(phiS);
                    Vec rGlobal = aS.toGlobal(rLocal);
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rGlobal.v[0], rGlobal.v[1], rGlobal.v[2]);
                    // Vec rDetector = m.toLocal(rGlobal);
                    // double vPred = rDetector.v[1];
                    // if (site.hitID >= 0) {
                    // System.out.format("vPredPrime=%10.6f, vPred=%10.6f, v=%10.6f\n", vPred, aS.mPred, m.hits.get(site.hitID).v);
                    // }
                }
                printWriter3.format("EOD\n");
            }

            for (KalTrack tkr : patRec.TkrList) {
                printWriter3.format("$tkp%d_%d << EOD\n", tkr.ID, patRec.topBottom);
                for (MeasurementSite site : tkr.SiteList) {
                    SiModule module = site.m;
                    int hitID = site.hitID;
                    if (hitID < 0) continue;
                    Measurement mm = module.hits.get(hitID);
                    Vec rLoc = null;
                    if (mm.rGlobal == null) {         // If there is no MC truth, use the track intersection for x and z
                        StateVector aS = site.aS;
                        double phiS = aS.planeIntersect(module.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.toGlobal(rLocal);  // Position in the global frame                 
                            rLoc = module.toLocal(rGlobal);     // Position in the detector frame
                        } else {
                            rLoc = new Vec(0.,0.,0.);
                        }
                    } else {
                        rLoc = module.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                    }
                    Vec rmG = module.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                    printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
                }
                printWriter3.format("EOD\n");
            }
        }
        printWriter3.format("$pnts << EOD\n");
        for (SiModule si : SiMlist) {
            for (Measurement mm : si.hits) {
                if (mm.tracks.size() > 0) continue;
                Vec rLoc = null;
                if (mm.rGlobal == null) {
                    rLoc = new Vec(0.,0.,0.);      // Use the center of the detector if there is no MC truth info
                } else {
                    rLoc = si.toLocal(mm.rGlobal); // Use MC truth for the x and z coordinates in the detector frame
                }
                Vec rmG = si.toGlobal(new Vec(rLoc.v[0], mm.v, rLoc.v[2]));
                printWriter3.format(" %10.6f %10.6f %10.6f\n", rmG.v[0], rmG.v[1], rmG.v[2]);
            }
        }
        printWriter3.format("EOD\n");
        printWriter3.format("splot $pnts u 1:2:3 with points pt 6 ps 2");
        for (KalmanPatRecHPS patRec : patRecList) {
            for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkr%d_%d u 1:2:3 with lines lw 3", tkr.ID, patRec.topBottom); }
            for (KalTrack tkr : patRec.TkrList) { printWriter3.format(", $tkp%d_%d u 1:2:3 with points pt 7 ps 2", tkr.ID, patRec.topBottom); }
        }
        printWriter3.format("\n");
        printWriter3.close();
    }
}
