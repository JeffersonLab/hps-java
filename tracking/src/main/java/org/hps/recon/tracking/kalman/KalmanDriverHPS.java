package org.hps.recon.tracking.kalman;

//import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackStateUtils;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackingReconstructionPlots;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.GBLKinkData;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

// $ java -jar ./distribution/target/hps-distribution-4.0-SNAPSHOT-bin.jar -b -DoutputFile=output -d HPS-EngRun2015-Nominal-v4-4-fieldmap -i tracking/tst_4-1.slcio -n 1 -R 5772 steering-files/src/main/resources/org/hps/steering/recon/KalmanTest.lcsim

public class KalmanDriverHPS extends Driver {

    private ArrayList<SiStripPlane> detPlanes;
    private List<HpsSiSensor> sensors;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private String trackCollectionName = "GBLTracks";
    private KalmanInterface KI;
    private double bField;
    private boolean verbose = false;
    private String outputSeedTrackCollectionName = "KalmanSeedTracks";
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private String outputPlots = "KalmanTestPlots.aida";
    private RelationalTable hitToStrips;
    private RelationalTable hitToRotated;

    public void setOutputPlotsFilename(String input) {
        outputPlots = input;
    }

    public String getOutputSeedTrackCollectionName() {
        return outputSeedTrackCollectionName;
    }

    public String getOutputFullTrackCollectionName() {
        return outputFullTrackCollectionName;
    }

    public void setOutputSeedTrackCollectionName(String input) {
        outputSeedTrackCollectionName = input;
    }

    public void setOutputFullTrackCollectionName(String input) {
        outputFullTrackCollectionName = input;
    }

    public void setVerbose(boolean input) {
        verbose = input;
    }

    public void setMaterialManager(MaterialSupervisor mm) {
        _materialManager = mm;
    }

    public void setTrackCollectionName(String input) {
        trackCollectionName = input;
    }

    private void setupPlots() {
        if (aida == null)
            aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        // TODO: example plot. Placeholder for something useful.
        // arguments to histogram1D: name, nbins, min, max
        for (int i = 1; i <= 12; i++) {
            aida.histogram1D(String.format("6hit Kalman Track Chi2 Layer %d", i), 100, 0, 50);
            aida.histogram1D(String.format("6hit Kalman Track Res Layer %d", i), 100, -0.05, 0.05);
            aida.histogram1D(String.format("6hit Kalman Hit Error Layer %d", i), 100, 0, 0.02);

            aida.histogram1D(String.format("5hit Kalman Track Chi2 Layer %d", i), 100, 0, 50);
            aida.histogram1D(String.format("5hit Kalman Track Res Layer %d", i), 100, -0.05, 0.05);
            aida.histogram1D(String.format("5hit Kalman Hit Error Layer %d", i), 100, 0, 0.02);

            //            aida.histogram1D(String.format("5hit Kalman Track ResX Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("5hit Kalman Track ResY Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("5hit GBL Track ResX Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("5hit GBL Track ResY Layer %d", i), 500, -50, 50);
            aida.histogram1D(String.format("5hit GBL-Kalman Track X Layer %d", i), 200, -0.2, 0.2);
            aida.histogram1D(String.format("5hit GBL-Kalman Track Y Layer %d", i), 200, -0.4, 0.4);

            //            aida.histogram1D(String.format("6hit Kalman Track ResX Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("6hit Kalman Track ResY Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("6hit GBL Track ResX Layer %d", i), 500, -50, 50);
            //            aida.histogram1D(String.format("6hit GBL Track ResY Layer %d", i), 500, -50, 50);
            aida.histogram1D(String.format("6hit GBL-Kalman Track X Layer %d", i), 200, -0.2, 0.2);
            aida.histogram1D(String.format("6hit GBL-Kalman Track Y Layer %d", i), 200, -0.4, 0.4);
        }
        aida.histogram1D("6hit Kalman Track Chi2", 100, 0, 200);
        aida.histogram1D("5hit Kalman Track Chi2", 100, 0, 200);
        aida.histogram1D("5hit GBL Track Chi2", 100, 0, 200);
        aida.histogram1D("6hit GBL Track Chi2", 100, 0, 200);

    }

    @Override
    public void detectorChanged(Detector det) {
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();

        setupPlots();

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }

        bField = TrackUtils.getBField(det).magnitude();
        sensors = det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        KI = new KalmanInterface();
        KI.verbose = this.verbose;
        KI.createSiModules(detPlanes, fm);

        // test
        //KalmanInterface.getField(new Vec(0., 500., 0.), det.getFieldMap()).print("new field");
        //fm.getField(new Vec(0., 500., 0.)).print("old field");

    }

    private void printGBLkinks(RelationalTable GBLtoKinks, Track GBLtrack) {
        GenericObject kinks = (GenericObject) GBLtoKinks.from(GBLtrack);
        for (int i = 0; i < kinks.getNDouble(); i++) {
            System.out.printf("sensor %d  lambda-kink %f phi-kink %f \n", i, kinks.getFloatVal(i), kinks.getDoubleVal(i));
        }
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            System.out.println(trackCollectionName + " does not exist; skipping event");
            return;
        }
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        List<Track> outputSeedTracks = new ArrayList<Track>();
        List<Track> outputFullTracks = new ArrayList<Track>();

        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);

        RelationalTable MatchedToGbl = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trkrelations = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
        for (LCRelation relation : trkrelations) {

            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                MatchedToGbl.add(relation.getFrom(), relation.getTo());
            }
            if (relation != null && verbose) {
                System.out.println("trkrelation: " + relation.toString());
                System.out.format("MatchedToGbl size = %d, string=%s\n", MatchedToGbl.size(), MatchedToGbl.toString());
            }
        }

        RelationalTable GBLtoKinks = GBLKinkData.getKinkDataToTrackTable(event);

        for (Track trk : tracks) {

            if (verbose) {
                System.out.println("\nPrinting info for original HPS SeedTrack:");
                printTrackInfo(trk, MatchedToGbl);
                System.out.println("\nPrinting info for original HPS GBLTrack:");
                printExtendedTrackInfo(trk);
                //printGBLkinks(GBLtoKinks, trk);
            }

            boolean createSeed = false;
            KalmanTrackFit2 ktf2 = null;
            if (createSeed) { // Start with the linear helix fit
                if (verbose)
                    System.out.println("Use a linear helix fit to get a starting guess for the Kalman filter\n");
                SeedTrack seedKalmanTrack = KI.createKalmanSeedTrack(trk, hitToStrips, hitToRotated);
                if (verbose) {
                    System.out.println("\nPrinting info for Kalman SeedTrack:");
                    seedKalmanTrack.print("testKalmanTrack");
                }
                if (!seedKalmanTrack.success) {
                    KI.clearInterface();
                    continue;
                }
                Track HPStrk = KI.createTrack(seedKalmanTrack);
                if (verbose) {
                    System.out.println("\nPrinting info for Kalman SeedTrack converted to HPS track:");
                    printTrackInfo(HPStrk, null);
                }
                outputSeedTracks.add(HPStrk);

                //full track
                ktf2 = KI.createKalmanTrackFit(seedKalmanTrack, trk, hitToStrips, hitToRotated, fm, 2);
                if (!ktf2.success) {
                    KI.clearInterface();
                    continue;
                }
            } else { // Or use the GBL fit to start with
                if (verbose)
                    System.out.println("Use the GBL fit results to start the Kalman filter\n");
                double minz = 999.;
                TrackState ts1 = null;
                HpsSiSensor sensor1 = null;
                for (HpsSiSensor sensor : sensors) {
                    //Hep3Vector trackPosition = TrackUtils.extrapolateTrackPositionToSensor(HPStrk, sensor, sensors, bField);
                    TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(trk, sensor.getMillepedeId());
                    if (tsAtSensor == null) {
                        continue;
                    }
                    Hep3Vector loc = TrackStateUtils.getLocationAtSensor(tsAtSensor, sensor, bField);
                    if (loc == null) {
                        continue;
                    }
                    if (verbose) {
                        System.out.format("   location at sensor: %10.7f %10.7f %10.7f\n", loc.v()[0], loc.v()[1], loc.v()[2]);
                    }
                    double zs = loc.v()[2];
                    if (zs < minz) {
                        minz = zs;
                        ts1 = tsAtSensor;
                        sensor1 = sensor;
                    }
                }
                if (ts1 == null) {
                    System.out.format("  No first-sensor track state found.\n");
                    continue;
                }
                double D0 = ts1.getD0();
                double Phi0 = ts1.getPhi();
                double Omega = ts1.getOmega();
                double Z0 = ts1.getZ0();
                double tanl = ts1.getTanLambda();
                // Transform to Kalman parameters     
                double c = 2.99793e8; // Speed of light in m/s
                double alpha = 1000.0 * 1.0e9 / (c * bField);
                double[] params = { D0, Phi0, Omega, Z0, tanl };
                Vec kalParams = new Vec(5, KalmanInterface.unGetLCSimParams(params, alpha));
                // Move the pivot point to the sensor position
                Vec oldPivot = KalmanInterface.vectorGlbToKalman(ts1.getReferencePoint());
                Hep3Vector loc = TrackStateUtils.getLocationAtSensor(ts1, sensor1, bField);
                Vec newPivot = KalmanInterface.vectorGlbToKalman(loc.v());
                kalParams = StateVector.pivotTransform(newPivot, kalParams, oldPivot, alpha, 0.);
                double[] covHPS = ts1.getCovMatrix();
                SquareMatrix cov = new SquareMatrix(5, KalmanInterface.ungetLCSimCov(covHPS, alpha));
                if (verbose) {
                    System.out.format("   1st sensor: D0=%10.7f phi0=%10.7f Omega=%10.7f Z0=%10.7f tanl=%10.7f\n", D0, Phi0, Omega, Z0, tanl);
                    oldPivot.print("Old pivot point for GBL track");
                    newPivot.print("New pivot point, near the first layer");
                    kalParams.print("GBL pivot-transformed helix params for starting Kalman fit");
                    newPivot.print("New pivot for starting Kalman fit");
                    cov.print("GBL covariance for starting Kalman fit");
                }
                //full track
                ktf2 = KI.createKalmanTrackFit(kalParams, newPivot, cov, trk, hitToStrips, hitToRotated, fm, 2);
                if (!ktf2.success) {
                    KI.clearInterface();
                    continue;
                }
                StateVector iniState = ktf2.fittedStateBegin();
                if (iniState != null) {
                    Vec finalPivot = iniState.origin.sum(iniState.X0);
                    Vec kalParamsF = StateVector.pivotTransform(finalPivot, kalParams, newPivot, alpha, 0.);
                    if (verbose) {
                        newPivot.print("Pivot point for the Kalman initial guess:");
                        finalPivot.print("Pivot point for the Kalman fitted helix: ");
                        kalParams.print("Kalman initial guess helix parameters, from GBL fit: ");
                        kalParamsF.print("Kalman initial guess helix parameters at final pivot:");
                        iniState.a.print("Kalman fitted helix parameters, from GBL fit:        ");
                        System.out.format("  >> GBL chi2=%10.4e,  Kalman chi2=%10.4e\n", trk.getChi2(), ktf2.tkr.chi2);
                    }
                }
            }

            KalTrack fullKalmanTrack = ktf2.tkr;

            if (verbose) {
                //ktf2.printFit("fullKalmanTrackFit");
                if (fullKalmanTrack != null) {
                    fullKalmanTrack.print("fullKalmanTrack");
                    printExtendedTrackInfo(fullKalmanTrack);
                }
            }
            if (fullKalmanTrack != null) {

                Track fullKalmanTrackHPS = KI.createTrack(fullKalmanTrack, true);
                outputFullTracks.add(fullKalmanTrackHPS);
                doResiduals(fullKalmanTrack, trk);

                //                double[] hprms = KalmanInterface.getLCSimParams(fullKalmanTrack.originHelix(), fullKalmanTrack.alpha);
                //                SymmetricMatrix hCov = KalmanInterface.getLCSimCov(fullKalmanTrack.originCovariance(), fullKalmanTrack.alpha);
                //                TrackState ts = trk.getTrackStates().get(0);
                //                double[] params = ts.getParameters();

                if (fullKalmanTrack.nHits == 12) {
                    for (MeasurementSite site : ktf2.sites) {
                        aida.histogram1D(String.format("6hit Kalman Track Chi2 Layer %d", site.m.Layer)).fill(site.chi2inc);
                        aida.histogram1D(String.format("6hit Kalman Hit Error Layer %d", site.m.Layer)).fill(site.m.hits.get(0).sigma);
                        aida.histogram1D(String.format("6hit Kalman Track Res Layer %d", site.m.Layer)).fill(site.m.hits.get(0).v - fullKalmanTrack.intercepts.get(site));
                    }
                    aida.histogram1D("6hit Kalman Track Chi2").fill(fullKalmanTrack.chi2);
                    aida.histogram1D("6hit GBL Track Chi2").fill(trk.getChi2());
                } else if (fullKalmanTrack.nHits == 10) {
                    for (MeasurementSite site : ktf2.sites) {
                        aida.histogram1D(String.format("5hit Kalman Track Chi2 Layer %d", site.m.Layer)).fill(site.chi2inc);
                        aida.histogram1D(String.format("5hit Kalman Hit Error Layer %d", site.m.Layer)).fill(site.m.hits.get(0).sigma);
                        aida.histogram1D(String.format("5hit Kalman Track Res Layer %d", site.m.Layer)).fill(site.m.hits.get(0).v - fullKalmanTrack.intercepts.get(site));
                    }
                    aida.histogram1D("5hit Kalman Track Chi2").fill(fullKalmanTrack.chi2);
                    aida.histogram1D("5hit GBL Track Chi2").fill(trk.getChi2());

                }
            }
            // clearing for next track
            KI.clearInterface();
        }
        if (verbose)
            System.out.println("\n DONE event ");

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputSeedTrackCollectionName, outputSeedTracks, Track.class, flag);
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
    }

    private void doResiduals(KalTrack kTrack, Track gblTrack) {

        List<Pair<double[], double[]>> gblMomsLocs = printExtendedTrackInfo(gblTrack);
        List<Pair<double[], double[]>> kalMomsLocs = printExtendedTrackInfo(kTrack);
        //List<TrackerHit> hitsOnTrack = TrackUtils.getStripHits(gblTrack, hitToStrips, hitToRotated);
        //kTrack.sortSites(true);
        //Collections.sort(hitsOnTrack, new SortByZ2());
        for (int i = 0; i < gblMomsLocs.size(); i++) {
            //double[] hitPos = hitsOnTrack.get(i).getPosition();
            double[] kalPos = kalMomsLocs.get(i).getSecondElement();
            double[] gblPos = gblMomsLocs.get(i).getSecondElement();
            int layer = kTrack.SiteList.get(i).m.Layer;
            if (verbose)
                System.out.printf("lay %d kalPos0 %f kalPos1 %f gblPos0 %f gblPos1 %f \n", layer, kalPos[0], kalPos[1], gblPos[0], gblPos[1]);
            aida.histogram1D(String.format("%dhit GBL-Kalman Track X Layer %d", gblTrack.getTrackerHits().size(), layer)).fill(gblPos[0] - kalPos[0]);
            aida.histogram1D(String.format("%dhit GBL-Kalman Track Y Layer %d", gblTrack.getTrackerHits().size(), layer)).fill(gblPos[1] - kalPos[1]);
        }

    }

    private List<Pair<double[], double[]>> printExtendedTrackInfo(Track HPStrk) {
        if (verbose)
            printTrackInfo(HPStrk, null);
        List<Pair<double[], double[]>> MomsLocs = new ArrayList<Pair<double[], double[]>>();
        for (HpsSiSensor sensor : sensors) {
            if (verbose)
                System.out.format("Sensor %d, Layer %d, is axial? %b\n", sensor.getModuleNumber(), sensor.getLayerNumber(), sensor.isAxial());
            //Hep3Vector trackPosition = TrackUtils.extrapolateTrackPositionToSensor(HPStrk, sensor, sensors, bField);
            TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(HPStrk, sensor.getMillepedeId());
            if (tsAtSensor == null) {
                if (verbose)
                    System.out.format("     Null track state at sensor for this sensor\n");
                continue;
            }
            double[] mom = ((BaseTrackState) (tsAtSensor)).computeMomentum(bField);
            double[] momTransformed = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(mom)).v();
            Hep3Vector loc = TrackStateUtils.getLocationAtSensor(tsAtSensor, sensor, bField);
            if (loc == null)
                continue;
            double[] ref = loc.v();
            if (verbose) {
                System.out.format("   Location at sensor= %10.7f %10.7f %10.7f\n", ref[0], ref[1], ref[2]);
                System.out.format("   Momentum at sensor= %10.7f %10.7f %10.7f\n", mom[0], mom[1], mom[2]);
            }
            double D0 = tsAtSensor.getD0();
            double Omega = tsAtSensor.getOmega();
            double Phi0 = tsAtSensor.getPhi();
            double Z0 = tsAtSensor.getZ0();
            double tanl = tsAtSensor.getTanLambda();
            if (verbose)
                System.out.format("   D0=%10.7f phi0=%10.7f Omega=%10.7f Z0=%10.7f tanl=%10.7f\n", D0, Phi0, Omega, Z0, tanl);
            double[] pnt = tsAtSensor.getReferencePoint();
            if (verbose)
                System.out.format("   Reference point = %10.7f, %10.7f, %10.7f\n", pnt[0], pnt[1], pnt[2]);
            MomsLocs.add(new Pair(momTransformed, ref));
        }

        Collections.sort(MomsLocs, new SortByZ());
        if (verbose) {
            for (Pair<double[], double[]> entry : MomsLocs) {
                System.out.printf("   TrackState: intercept %10.6f %10.6f %10.6f \n", entry.getSecondElement()[0], entry.getSecondElement()[1], entry.getSecondElement()[2]);
                System.out.printf("               momentum  %10.6f %10.6f %10.6f \n", entry.getFirstElement()[0], entry.getFirstElement()[1], entry.getFirstElement()[2]);
            }
        }
        return MomsLocs;
    }

    private List<Pair<double[], double[]>> printExtendedTrackInfo(KalTrack trk) {
        List<Pair<double[], double[]>> MomsLocs = new ArrayList<Pair<double[], double[]>>();
        if (verbose)
            System.out.println("KalTrack intercepts and momenta:");
        for (MeasurementSite site : trk.interceptVects.keySet()) {
            Vec mom = trk.interceptMomVects.get(site);
            Vec loc = trk.interceptVects.get(site);
            double[] locTransformed = loc.leftMultiply(KalmanInterface.KalmanToHps).v;
            double[] locTrans = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(locTransformed)).v();
            double[] momTransformed = mom.leftMultiply(KalmanInterface.KalmanToHps).v;
            double[] momTrans = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(momTransformed)).v();
            MomsLocs.add(new Pair<double[], double[]>(momTrans, locTrans));
        }
        Collections.sort(MomsLocs, new SortByZ());
        if (verbose) {
            for (Pair<double[], double[]> entry : MomsLocs) {
                System.out.printf("   TrackState: intercept %f %f %f \n", entry.getSecondElement()[0], entry.getSecondElement()[1], entry.getSecondElement()[2]);
                System.out.printf("       mom %f %f %f \n", entry.getFirstElement()[0], entry.getFirstElement()[1], entry.getFirstElement()[2]);
            }
        }

        return MomsLocs;
    }

    class SortByZ implements Comparator<Pair<double[], double[]>> {

        @Override
        public int compare(Pair<double[], double[]> o1, Pair<double[], double[]> o2) {
            return (int) (o1.getSecondElement()[2] - o2.getSecondElement()[2]);
        }
    }

    class SortByZ2 implements Comparator<TrackerHit> {

        @Override
        public int compare(TrackerHit o1, TrackerHit o2) {
            return (int) (o1.getPosition()[2] - o2.getPosition()[2]);
        }
    }

    private void printTrackInfo(Track HPStrk, RelationalTable MatchedToGbl) {
        TrackState ts = null;
        if (MatchedToGbl != null) {
            Track tmp = (Track) (MatchedToGbl.from(HPStrk));
            if (tmp == null)
                return;
            ts = tmp.getTrackStates().get(0);
        } else
            ts = HPStrk.getTrackStates().get(0);
        double[] params = ts.getParameters();
        System.out.printf("Track 3D hits: %d \n", HPStrk.getTrackerHits().size());
        System.out.printf("params: %f %f %f %f %f \n", params[0], params[1], params[2], params[3], params[4]);
    }

    @Override
    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

}
