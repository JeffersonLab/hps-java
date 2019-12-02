package org.hps.recon.tracking.kalman;

//import java.io.File;
//import java.io.FileNotFoundException;
import java.io.IOException;
//import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.geometry.IDDecoder;

// $ java -jar ./distribution/target/hps-distribution-4.0-SNAPSHOT-bin.jar -b -DoutputFile=output -d HPS-EngRun2015-Nominal-v4-4-fieldmap -i tracking/tst_4-1.slcio -n 1 -R 5772 steering-files/src/main/resources/org/hps/steering/recon/KalmanTest.lcsim

public class KalmanPatRecDriver extends Driver {

    IDDecoder decoder;
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
    private int nTracks;
    private int[] nHitsOnTracks;

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
        aida.histogram1D("Kalman number of tracks", 10, 0., 10.);
        aida.histogram1D("Kalman Track Chi2", 50, 0., 100.);
        aida.histogram1D("Kalman Track Number Hits", 20, 0., 20.);
        aida.histogram1D("Kalman missed hit residual", 100, -1.0, 1.0);
        aida.histogram1D("Kalman track hit residual", 100, -0.2, 0.2);
        aida.histogram1D("Kalman hit true error", 100, -0.2, 0.2);
        aida.histogram1D("Kalman track Momentum", 100, 0., 5.);
        aida.histogram1D("MC hit z in local system (should be zero)", 50, -2., 2.);
        for (int lyr=2; lyr<14; ++lyr) {
            aida.histogram1D(String.format("Kalman track hit residual in layer %d",lyr), 100, -0.2, 0.2);
            aida.histogram1D(String.format("Kalman true error in layer %d",lyr), 100, -0.2, 0.2);
            aida.histogram1D(String.format("Kalman layer %d chi^2 contribution", lyr), 100, 0., 20.);
            if (lyr<13) {
                aida.histogram1D(String.format("Kalman kink in xy, layer %d", lyr),100, -0.001, .001);
                aida.histogram1D(String.format("Kalman kink in zy, layer %d", lyr),100, -0.0025, .0025);
            }
        }
        nTracks = 0;
        nHitsOnTracks = new int[14];
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
        
        decoder = det.getSubdetector("Tracker").getIDDecoder();
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

        ArrayList<KalmanPatRecHPS> kPatList = KI.KalmanPatRec(event, decoder);
        if (kPatList == null) {
            System.out.println("KalmanPatRecDriver.process: null returned by KalmanPatRec.");
            return;
        }

        nTracks = 0;
        for (KalmanPatRecHPS kPat : kPatList) {
            if (kPat == null) {
                System.out.println("KalmanPatRecDriver.process: pattern recognition failed in the top or bottom tracker.\n");
                return;
            }
            for (KalTrack kTk : kPat.TkrList) {
                if (verbose) kTk.print(String.format(" PatRec for topBot=%d ",kPat.topBottom));
                nTracks++;
                aida.histogram1D("Kalman Track Number Hits").fill(kTk.nHits);
                if (kTk.nHits < 12) continue;
                
                aida.histogram1D("Kalman Track Chi2").fill(kTk.chi2);
                double[] momentum = kTk.originP();
                double pMag = Math.sqrt(momentum[0]*momentum[0]+momentum[1]*momentum[1]+momentum[2]*momentum[2]);
                aida.histogram1D("Kalman track Momentum").fill(pMag);
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
                                nHitsOnTracks[mod.Layer]++;
                                double resid = mod.hits.get(site.hitID).v - rLoc.v[1];
                                aida.histogram1D("Kalman track hit residual").fill(resid);
                                aida.histogram1D(String.format("Kalman track hit residual in layer %d",mod.Layer)).fill(resid);
                                aida.histogram1D(String.format("Kalman layer %d chi^2 contribution", mod.Layer)).fill(site.chi2inc);
                                if (mod.Layer<13) {
                                    aida.histogram1D(String.format("Kalman kink in xy, layer %d", mod.Layer)).fill(kTk.scatX(mod.Layer));
                                    aida.histogram1D(String.format("Kalman kink in zy, layer %d", mod.Layer)).fill(kTk.scatZ(mod.Layer));
                                }  
                            }
                        }
                    }
                }
            }
        }
        aida.histogram1D("Kalman number of tracks").fill(nTracks);
        
        String path = "C:\\Users\\Robert\\Desktop\\Kalman\\";
        if (nPlotted < 40) {
            KI.plotKalmanEvent(path, event, kPatList);
            nPlotted++;
        }
        
        //simScatters(event);
        simHitRes(event);
        
        KI.clearInterface();
        if (verbose) System.out.format("\n KalmanPatRecDriver.process: Done with event %d\n", evtNumb);
        
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
    }

    // Make histograms of the MC hit resolution
    private void simHitRes(EventHeader event) {
        // Get the collection of 1D hits
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        List<TrackerHit> stripHits = event.get(TrackerHit.class, stripHitInputCollectionName);
        
        if (stripHits == null) return;

        // Make a mapping from sensor to hits
        Map<HpsSiSensor, ArrayList<TrackerHit>> hitSensorMap = new HashMap<HpsSiSensor, ArrayList<TrackerHit>>();
        for (TrackerHit hit1D : stripHits) {
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
        
        String MCHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> MChits = event.get(SimTrackerHit.class, MCHitInputCollectionName);
        
        if (MChits == null) return;

        // Make a map from MC particle to the hit in the layer
        Map<Integer, ArrayList<SimTrackerHit>> hitMCparticleMap = new HashMap<Integer, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : MChits) {
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;  // Kalman layers go from 0 to 13, with 0 and 1 for new 2019 modules
            //int Module = decoder.getValue("module");
            //MCParticle MCpart = hit1D.getMCParticle();
            ArrayList<SimTrackerHit> partInLayer = null;
            if (hitMCparticleMap.containsKey(Layer)) {
                partInLayer = hitMCparticleMap.get(Layer);
            } else {
                partInLayer = new ArrayList<SimTrackerHit>();
            }
            partInLayer.add(hit1D);
            hitMCparticleMap.put(Layer,partInLayer);
        }
        //System.out.format("KalmanPatRecDriver.simHitRes: found both hit collections in event %d\n", event.getEventNumber());
        
        ArrayList<SiModule> SiMlist = KI.getSiModuleList();
        Map<SiModule, SiStripPlane> moduleMap = KI.getModuleMap();
        for (SiModule siMod : SiMlist) {
            int layer = siMod.Layer;
            if (hitMCparticleMap.get(layer) == null) continue;
            SiStripPlane plane = moduleMap.get(siMod);
            if (!hitSensorMap.containsKey(plane.getSensor())) continue;
            ArrayList<TrackerHit> hitsInSensor = hitSensorMap.get(plane.getSensor());
            if (hitsInSensor == null) continue;
            for (TrackerHit hit : hitsInSensor) {
                //System.out.format("simHitRes: Hit in sensor of layer %d, detector %d\n",layer,siMod.detector);
                //Vec tkrHit = new Vec(3,hit.getPosition());
                //tkrHit.print("Tracker hit position");
                //SiTrackerHitStrip1D global = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);
                SiTrackerHitStrip1D localHit = (new SiTrackerHitStrip1D(hit)).getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                //Vec globalHPS = new Vec(3,global.getPosition());
                Vec localHPS = new Vec(3,localHit.getPosition());
                //globalHPS.print("simulated hit in HPS global system");
                //localHPS.print("simulated hit in HPS local system");
                
                //Vec hitGlobal = KalmanInterface.vectorGlbToKalman(global.getPosition());
                Vec hitLocal = new Vec(-localHit.getPosition()[1], localHit.getPosition()[0], localHit.getPosition()[2]);
                
                //hitGlobal.print("simulated hit Kal global position");
                //hitLocal.print("simulated hit Kal local position");
                //Vec kalGlobal = siMod.toGlobal(hitLocal);
                //kalGlobal.print("simulated hit global position from Kalman transformation");
                for (SimTrackerHit hitMC : hitMCparticleMap.get(layer)) {
                    Vec hitMCglobal = KalmanInterface.vectorGlbToKalman(hitMC.getPosition());
                    //hitMCglobal.print("true hit Kal global position");
                    Vec kalMClocal = siMod.toLocal(hitMCglobal);
                    //kalMClocal.print("true hit Kal local position");
                    //hitLocal.print("sim hit Kal local position");
                    double hitError = hitLocal.v[1] - kalMClocal.v[1];
                    aida.histogram1D("Kalman hit true error").fill(hitError);
                    aida.histogram1D(String.format("Kalman true error in layer %d",layer)).fill(hitError);
                    aida.histogram1D("MC hit z in local system (should be zero)").fill(kalMClocal.v[2]);
                }
            }
        }
    }
    // Find the MC true scattering angles at tracker layers
    // Note, this doesn't work, because the MC true momentum saved is neither the incoming nor outgoing momentum but rather
    // some average of the two.
    private void simScatters(EventHeader event) {
        String stripHitInputCollectionName = "TrackerHits";
        List<SimTrackerHit> striphits = event.get(SimTrackerHit.class, stripHitInputCollectionName);
        if (striphits == null) return;
        
        // Make a map from MC particle to the hit in the layer
        Map<Integer, ArrayList<SimTrackerHit>> hitMCparticleMap = new HashMap<Integer, ArrayList<SimTrackerHit>>();
        for (SimTrackerHit hit1D : striphits) {
            decoder.setID(hit1D.getCellID());
            int Layer = decoder.getValue("layer") + 1;
            //int Module = decoder.getValue("module");
            //MCParticle MCpart = hit1D.getMCParticle();
            ArrayList<SimTrackerHit> partInLayer = null;
            if (hitMCparticleMap.containsKey(Layer)) {
                partInLayer = hitMCparticleMap.get(Layer);
            } else {
                partInLayer = new ArrayList<SimTrackerHit>();
            }
            partInLayer.add(hit1D);
            hitMCparticleMap.put(Layer,partInLayer);
        }
        
        // Now analyze each MC hit, except those in the first and last layers
        for (int Layer=2; Layer<11; ++Layer) {
            if (hitMCparticleMap.containsKey(Layer) && hitMCparticleMap.containsKey(Layer-1)) {
                hit2Loop :
                for (SimTrackerHit hit2 : hitMCparticleMap.get(Layer)) {
                    MCParticle MCP2 = hit2.getMCParticle();
                    double Q2 = MCP2.getCharge();
                    Vec p2 = KalmanInterface.vectorGlbToKalman(hit2.getMomentum());
                    for (SimTrackerHit hit1 : hitMCparticleMap.get(Layer-1)) {
                        Vec p1 = KalmanInterface.vectorGlbToKalman(hit2.getMomentum());  // reverse direction
                        MCParticle MCP1 = hit1.getMCParticle();
                        double Q1 = MCP1.getCharge();
                        double ratio = p2.mag()/p1.mag();
                        if (ratio > 0.98 && ratio <= 1.02 && Q1*Q2 > 0. && MCP1.equals(MCP2)) {
                            // Integrate through the B field from the previous layer to this layer to get the momentum
                            // before scattering. I'm assuming that the MC hit gives the momentum following the scatter.
                            // The integration distance is approximated to be the straight line distance, which should get
                            // pretty close for momenta of interest.
                            double dx = 1.0;
                            RungeKutta4 rk4 = new RungeKutta4(Q1, dx, fm);
                            Vec xStart = KalmanInterface.vectorGlbToKalman(hit1.getPosition());
                            Vec xEnd = KalmanInterface.vectorGlbToKalman(hit2.getPosition());
                            double straightDistance = xEnd.dif(xStart).mag();
                            double[] r= rk4.integrate(xStart, p1, straightDistance-dx);
                            dx = dx/100.0;
                            rk4 = new RungeKutta4(Q1, dx, fm);
                            straightDistance = xEnd.dif(new Vec(r[0],r[1],r[2])).mag();
                            r = rk4.integrate(new Vec(r[0],r[1],r[2]),  new Vec(r[3],r[4],r[5]), straightDistance-dx);
                            
                            Vec newPos = new Vec(r[0], r[1], r[2]);
                            Vec newMom = new Vec(r[3], r[4], r[5]);
                            System.out.format("KalmanPatRecDriver.simScatters: trial match at layer %d\n", Layer);
                            xEnd.print("MC point on layer");
                            newPos.print("integrated point near layer");
                            p2.print("MC momentum at layer");
                            newMom.print("integrated momentum near layer");
                            double ctScat = p2.dot(newMom)/p2.mag()/newMom.mag();
                            double angle = Math.acos(ctScat);
                            System.out.format("  Angle between incoming and outgoing momenta=%10.6f\n", angle);
                            continue hit2Loop;
                        }
                    }
                }
            }
        }    
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
        System.out.println("Individual layer efficiencies:");
        for (int lyr=0; lyr<14; lyr++) {
            double effic = (double)nHitsOnTracks[lyr]/(double)nTracks;
            System.out.format("   Layer %d, hit efficiency = %9.3f\n", lyr, effic);
        }
        if (outputPlots != null) {
            try {
                System.out.println("Outputting the aida histograms now.");
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}
