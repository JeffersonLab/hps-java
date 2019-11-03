package org.hps.recon.tracking.kalman;

//import java.io.File;
//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackingReconstructionPlots;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

// $ java -jar ./distribution/target/hps-distribution-4.0-SNAPSHOT-bin.jar -b -DoutputFile=output -d HPS-EngRun2015-Nominal-v4-4-fieldmap -i tracking/tst_4-1.slcio -n 1 -R 5772 steering-files/src/main/resources/org/hps/steering/recon/KalmanTest.lcsim

public class KalmanPatRecDriver extends Driver {

    private ArrayList<SiStripPlane> detPlanes;
    private MaterialSupervisor _materialManager;
    private org.lcsim.geometry.FieldMap fm;
    private KalmanInterface KI;
    private boolean verbose = false;
    private String outputSeedTrackCollectionName = "KalmanSeedTracks";
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private String outputPlots = "KalmanTestPlots.root";
    private int nPlotted;

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
    }

    private void setupPlots() {
        if (aida == null) aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        nPlotted = 0;

        // arguments to histogram1D: name, nbins, min, max
        aida.histogram1D("Kalman Track Chi2", 50, 0., 400.);
        aida.histogram1D("Kalman Track Number Hits", 20, 0., 20.);
        aida.histogram1D("Kalman missed hit residual", 100, -1.0, 1.0);
        aida.histogram1D("Kalman track hit residual", 100, -0.2, 0.2);
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

        TrackUtils.getBField(det).magnitude();
        det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        KI = new KalmanInterface(verbose);
        KI.createSiModules(detPlanes, fm);
    }

    @Override
    public void process(EventHeader event) {
        List<Track> outputFullTracks = new ArrayList<Track>();
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.println("KalmanPatRecDriver.process:" + stripHitInputCollectionName + " does not exist; skipping event");
            return;
        }
        int evtNumb = event.getEventNumber();

        ArrayList<KalmanPatRecHPS> kPatList = KI.KalmanPatRec(event);

        for (KalmanPatRecHPS kPat : kPatList) {
            if (kPat == null) {
                System.out.println("KalmanPatRecDriver.process: pattern recognition failed in the bottom tracker.\n");
                return;
            }
            for (KalTrack kTk : kPat.TkrList) {
                if (verbose) kTk.print(String.format(" PatRec for topBot=%d ",kPat.topBottom));
                aida.histogram1D("Kalman Track Chi2").fill(kTk.chi2);
                aida.histogram1D("Kalman Track Number Hits").fill(kTk.nHits);
                Track KalmanTrackHPS = KI.createTrack(kTk, true);
                outputFullTracks.add(KalmanTrackHPS);
                
                // Histogram residuals of hits in layers with no hits on the track and with hits
                for (MeasurementSite site : kTk.SiteList) {
                    StateVector aS = site.aS;
                    SiModule mod = site.m;
                    if (aS != null && mod != null) {
                        double phiS = aS.planeIntersect(mod.p);
                        if (!Double.isNaN(phiS)) {
                            Vec rLocal = aS.atPhi(phiS);        // Position in the Bfield frame
                            Vec rGlobal = aS.toGlobal(rLocal);  // Position in the global frame                 
                            Vec rLoc = mod.toLocal(rGlobal);    // Position in the detector frame
                            if (site.hitID < 0) {
                                for (Measurement m : mod.hits) {
                                    double resid = m.v - rLoc.v[1];
                                    aida.histogram1D("Kalman missed hit residual").fill(resid);
                                }                       
                            } else {
                                double resid = mod.hits.get(site.hitID).v - rLoc.v[1];
                                aida.histogram1D("Kalman track hit residual").fill(resid);
                            }
                        }
                    }
                }
            }
        }
        
        String path = "C:\\Users\\Robert\\Desktop\\Kalman\\";
        if (nPlotted < 20) {
            KI.plotKalmanEvent(path, event, kPatList);
            nPlotted++;
        }
        
        KI.clearInterface();
        if (verbose) System.out.format("\n KalmanPatRecDriver.process: Done with event %d\n", evtNumb);
        
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
    }

    void printKalmanKinks(KalTrack tkr) {
        //for (MeasurementSite site : tkr.SiteList) {
        //    if (verbose) {
        //        System.out.format("Layer %d Kalman filter lambda-kink= %10.8f  phi-kink=  %10.8f\n", site.m.Layer, site.scatZ(), site.scatX());
        //    }
        //}
        for (int layer = 1; layer < 12; layer++) {
            aida.histogram1D(String.format("Kalman lambda kinks for layer %d", layer)).fill(tkr.scatZ(layer));
            aida.histogram1D(String.format("Kalman phi kinks for layer %d", layer)).fill(tkr.scatX(layer));
            if (verbose) {
                System.out.format("Layer %d Kalman smoother lambda-kink= %10.8f  phi-kink= %10.8f\n", layer, tkr.scatZ(layer),
                        tkr.scatX(layer));
            }
        }
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

    @Override
    public void endOfData() {
        if (outputPlots != null) {
            try {
                System.out.println("Outputting the plots now.");
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
