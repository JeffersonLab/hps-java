package org.hps.recon.tracking.kalman;

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
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
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
    private FieldMap fm;
    private String fieldMapFileName = "fieldmap/125acm2_3kg_corrected_unfolded_scaled_0.7992_v3.dat";
    private String trackCollectionName = "GBLTracks";
    private KalmanInterface KI;
    private double bField;
    private boolean verbose = true;
    private String outputSeedTrackCollectionName = "KalmanSeedTracks";
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private String outputPlots = "TrackingRecoPlots.aida";

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

    public void setFieldMapFilename(String input) {
        fieldMapFileName = input;
    }

    private void setupPlots() {
        if (aida == null)
            aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        // TODO: example plot. Placeholder for something useful.
        // arguments to histogram1D: name, nbins, min, max
        aida.histogram1D("Kalman Track Chi2", 50, 0, 100);
    }

    @Override
    public void detectorChanged(Detector det) {
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        setupPlots();

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }
        try {
            //FIXME
            // fetch offsets from compact.xml, *10 for cm->mm
            fm = new FieldMap(fieldMapFileName, "HPS", 21.17, 0., 457.2);
        } catch (IOException e) {
            e.printStackTrace();
        }
        bField = TrackUtils.getBField(det).magnitude();
        sensors = det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        KI = new KalmanInterface();
        KI.createSiModules(detPlanes, fm);
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

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        RelationalTable MatchedToGbl = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        List<LCRelation> trkrelations = event.get(LCRelation.class, "MatchedToGBLTrackRelations");
        for (LCRelation relation : trkrelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                MatchedToGbl.add(relation.getFrom(), relation.getTo());
            }
        }

        for (Track trk : tracks) {
            if (verbose) {
                System.out.println("\nPrinting info for original HPS SeedTrack:");
                printTrackInfo(trk, MatchedToGbl);
                System.out.println("\nPrinting info for original HPS GBLTrack:");
                printExtendedTrackInfo(trk);
            }

            //seedtrack
            SeedTrack seedKalmanTrack = KI.createKalmanSeedTrack(trk, hitToStrips, hitToRotated);
            if (verbose) {
                System.out.println("\nPrinting info for Kalman SeedTrack:");
                seedKalmanTrack.print("testKalmanTrack");
            }
            Track HPStrk = KI.createTrack(seedKalmanTrack);
            if (verbose) {
                System.out.println("\nPrinting info for Kalman SeedTrack converted to HPS track:");
                printTrackInfo(HPStrk, null);
            }
            outputSeedTracks.add(HPStrk);

            //full track
            KalmanTrackFit2 ktf2 = KI.createKalmanTrackFit(seedKalmanTrack, trk, hitToStrips, hitToRotated, fm, 2);
            KalTrack fullKalmanTrack = ktf2.tkr;
            if (verbose) {
                //ktf2.printFit("fullKalmanTrackFit");
                if (fullKalmanTrack != null) {
                    fullKalmanTrack.print("fullKalmanTrack");
                    printExtendedTrackInfo(fullKalmanTrack);
                }
            }

            Track fullKalmanTrackHPS = KI.createTrack(fullKalmanTrack, true);
            outputFullTracks.add(fullKalmanTrackHPS);

            // TODO: placeholder for useful plot-filling
            aida.histogram1D("Kalman Track Chi2").fill(fullKalmanTrackHPS.getChi2());

            // clearing for next track
            KI.clearInterface();

        }
        if (verbose)
            System.out.println("\n DONE event ");

        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputSeedTrackCollectionName, outputSeedTracks, Track.class, flag);
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
    }

    private List<Pair<double[], double[]>> printExtendedTrackInfo(Track HPStrk) {
        printTrackInfo(HPStrk, null);
        List<Pair<double[], double[]>> MomsLocs = new ArrayList<Pair<double[], double[]>>();
        for (HpsSiSensor sensor : sensors) {
            //Hep3Vector trackPosition = TrackUtils.extrapolateTrackPositionToSensor(HPStrk, sensor, sensors, bField);
            TrackState tsAtSensor = TrackStateUtils.getTrackStateAtSensor(HPStrk, sensor.getMillepedeId());
            if (tsAtSensor == null)
                continue;
            double[] mom = ((BaseTrackState) (tsAtSensor)).computeMomentum(bField);
            double[] momTransformed = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(mom)).v();
            Hep3Vector loc = TrackStateUtils.getLocationAtSensor(tsAtSensor, sensor, bField);
            if (loc == null)
                continue;
            double[] ref = loc.v();
            MomsLocs.add(new Pair(momTransformed, ref));
        }

        Collections.sort(MomsLocs, new SortByZ());
        for (Pair<double[], double[]> entry : MomsLocs) {
            System.out.printf("   TrackState: intercept %f %f %f \n", entry.getSecondElement()[0], entry.getSecondElement()[1], entry.getSecondElement()[2]);
            System.out.printf("       mom %f %f %f \n", entry.getFirstElement()[0], entry.getFirstElement()[1], entry.getFirstElement()[2]);
        }

        return MomsLocs;
    }

    private List<Pair<double[], double[]>> printExtendedTrackInfo(KalTrack trk) {
        List<Pair<double[], double[]>> MomsLocs = new ArrayList<Pair<double[], double[]>>();
        System.out.println("KalTrack intercepts and momenta:");
        for (MeasurementSite site : trk.interceptVects.keySet()) {
            Vec mom = trk.interceptMomVects.get(site);
            Vec loc = trk.interceptVects.get(site);
            double[] locTransformed = loc.leftMultiply(KalmanInterface.KalmanToHps).v;
            double[] locTrans = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(locTransformed)).v();
            double[] momTransformed = mom.leftMultiply(KalmanInterface.KalmanToHps).v;
            double[] momTrans = CoordinateTransformations.transformVectorToDetector(new BasicHep3Vector(momTransformed)).v();
            MomsLocs.add(new Pair(momTrans, locTrans));
        }
        Collections.sort(MomsLocs, new SortByZ());
        for (Pair<double[], double[]> entry : MomsLocs) {
            System.out.printf("   TrackState: intercept %f %f %f \n", entry.getSecondElement()[0], entry.getSecondElement()[1], entry.getSecondElement()[2]);
            System.out.printf("       mom %f %f %f \n", entry.getFirstElement()[0], entry.getFirstElement()[1], entry.getFirstElement()[2]);
        }

        return MomsLocs;
    }

    class SortByZ implements Comparator<Pair<double[], double[]>> {

        @Override
        public int compare(Pair<double[], double[]> o1, Pair<double[], double[]> o2) {
            return (int) (o1.getSecondElement()[2] - o2.getSecondElement()[2]);
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
