package org.hps.recon.tracking.kalman;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

// $ java -jar ./distribution/target/hps-distribution-4.0-SNAPSHOT-bin.jar -b -DoutputFile=output -d HPS-EngRun2015-Nominal-v4-4-fieldmap -i tracking/tst_4-1.slcio -n 1 -R 5772 steering-files/src/main/resources/org/hps/steering/recon/KalmanTest.lcsim

public class KalmanDriverHPS extends Driver {

    private ArrayList<SiModule> testData;
    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private FieldMap fm;
    private String fieldMapFileName = "fieldmap/125acm2_3kg_corrected_unfolded_scaled_0.7992.dat";
    private String trackCollectionName = "MatchedTracks";
    private KalmanInterface KI;
    ArrayList<SiModule> SiMlist;

    public void setMaterialManager(MaterialSupervisor mm) {
        _materialManager = mm;
    }

    public void setTrackCollectionName(String input) {
        trackCollectionName = input;
    }

    public void setFieldMapFilename(String input) {
        fieldMapFileName = input;
    }

    public void setTestData(ArrayList<SiModule> input) {
        testData = input;
    }

    public void constructTestData() {
        // SiModules
        double[] location = { 100., 200., 300., 500., 700., 900. };
        double delta = 5.0;
        double[] heights = { 100., 100., 100., 100., 100., 100. };
        double[] widths = { 150., 150., 150., 300., 300., 300. };
        double[] stereoAngle = { 0.1, 0.1, 0.1, 0.05, 0.05, 0.05 };
        double thickness = 0.3;
        Vec tInt = new Vec(0., 1., 0.);

        testData = new ArrayList<SiModule>(12);
        for (int pln = 0; pln < 6; pln++) {
            Vec rInt1 = new Vec(0., location[pln], 0.);

            Plane pInt1 = new Plane(rInt1, tInt);
            SiModule newModule1 = new SiModule(pln, pInt1, 0., widths[pln], heights[pln], thickness, fm);
            testData.add(newModule1);

            Vec rInt2 = new Vec(0., location[pln] + delta, 0.);
            Plane pInt2 = new Plane(rInt2, tInt);
            SiModule newModule2 = new SiModule(pln, pInt2, stereoAngle[pln], widths[pln], heights[pln], thickness, fm);
            testData.add(newModule2);
        }

        // helix
        double p = 1.0; // momentum
        double Phi = 90. * Math.PI / 180.;
        double Theta = 90. * Math.PI / 180.;
        Vec initialDirection = new Vec(Math.cos(Phi) * Math.sin(Theta), Math.sin(Phi) * Math.sin(Theta), Math.cos(Theta));
        Vec momentum = new Vec(p * initialDirection.v[0], p * initialDirection.v[1], p * initialDirection.v[2]);
        Helix TkInitial = new Helix(1, new Vec(2., 90., 2.), momentum, new Vec(2., 90., 2.), fm);
        TkInitial.print("TestHelix");

        // measurements
        HelixPlaneIntersect hpi = new HelixPlaneIntersect();
        for (int pln = 0; pln < 12; pln++) {
            SiModule thisSi = testData.get(pln);
            double phiInt = TkInitial.planeIntersect(thisSi.p);
            if (Double.isNaN(phiInt))
                break;
            Vec rscat = new Vec(3);
            Vec pInt = new Vec(3);
            rscat = hpi.rkIntersect(thisSi.p, TkInitial.atPhiGlobal(0.), TkInitial.getMomGlobal(0.), 1, fm, pInt);
            Vec rDet = thisSi.toLocal(rscat);
            double resolution = 0.012;
            double m1 = rDet.v[1] + resolution;
            Measurement thisM1 = new Measurement(m1, resolution, rscat, rDet.v[1]);
            thisSi.addMeasurement(thisM1);
        }
    }

    @Override
    public void detectorChanged(Detector det) {
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }
        try {
            fm = new FieldMap(fieldMapFileName, "HPS", 0, 0, 0);
        } catch (IOException e) {
            e.printStackTrace();
        }

        KI = new KalmanInterface();
        SiMlist = KI.createSiModules(detPlanes, fm);
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(Track.class, trackCollectionName)) {
            System.out.println(trackCollectionName + " does not exist; skipping event");
            return;
        }
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        RelationalTable hitToStrips = TrackUtils.getHitToStripsTable(event);
        RelationalTable hitToRotated = TrackUtils.getHitToRotatedTable(event);

        //        constructTestData();
        //        System.out.println("Printing info for test-data SiMods:");
        //        for (SiModule SiM : testData) {
        //            SiM.print("SiModFromTestData");
        //        }

        for (Track trk : tracks) {
            ArrayList<SiModule> SiMlist2 = new ArrayList<SiModule>();
            KI.fillMeasurements(SiMlist, trk, hitToStrips, hitToRotated);
            System.out.println("\nPrinting info for original HPS track:");
            printTrackInfo(trk);

            System.out.println("\nPrinting info for HPS SiMods:\n");
            for (SiModule SiM : SiMlist) {
                if (SiM.hits.size() > 0) {
                    SiMlist2.add(SiM);
                    SiM.print("SiModFilled");
                }
            }
            Collections.sort(SiMlist2, new SortByLayer());

            SeedTrack testKalmanTrack = new SeedTrack(SiMlist2, 0, 0, SiMlist2.size(), false);
            System.out.println("\nPrinting info for Kalman track:");
            testKalmanTrack.print("testKalmanTrack");

            Track HPStrk = KI.createTrack(testKalmanTrack, getMeasurements(SiMlist2));

            System.out.println("\nPrinting info for converted HPS track:");
            printTrackInfo(HPStrk);
            KI.clearHitMap();
        }
        System.out.println("DONE event ");
    }

    private void printTrackInfo(Track HPStrk) {
        TrackState ts = HPStrk.getTrackStates().get(0);
        double[] params = ts.getParameters();
        System.out.printf("Track hits: %d \n", HPStrk.getTrackerHits().size());
        System.out.printf("      params: %f %f %f %f %f \n", params[0], params[1], params[2], params[3], params[4]);
    }

    public ArrayList<Measurement> getMeasurements(List<SiModule> modList) {
        ArrayList<Measurement> measList = new ArrayList<Measurement>();
        for (SiModule SiM : modList) {
            measList.addAll(SiM.hits);
        }
        return measList;
    }

}

class SortByLayer implements Comparator<SiModule> {

    @Override
    public int compare(SiModule o1, SiModule o2) {
        return o1.Layer - o2.Layer;
    }
}
