package org.hps.recon.tracking.kalman;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.BasicHep3Vector;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.gbl.GBLStripClusterData;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.event.base.BaseLCRelation;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.lcio.LCIOConstants;
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
    private boolean uniformB = false;
    private String outputFullTrackCollectionName = "KalmanFullTracks";
    public AIDA aida;
    private int nTracks;
    private int nEvents;
    private double executionTime;
    private KalmanParams kPar;
    private KalmanPatRecPlots kPlot;
    
    // Parameters for the Kalman pattern recognition that can be set by the user in the steering file:
    private int numKalmanIteration;    // Number of Kalman filter iterations per track in the final fit
    private double maxPtInverse;       // Maximum value of 1/pt for the seed and the final track
    private double maxD0;              // Maximum dRho (or D0) at the target plane for a seed and the final track
    private double maxZ0;              // Maximum dz (or Z0) at the target plane for a seed and the final track
    private double maxChi2;            // Maximum Kalman chi^2 per hit for a track candidate
    private int minHits;               // Minimum number of hits on a track
    private int minStereo;             // Minimum number of stereo hits on a track
    private int maxSharedHits;         // Maximum number of hits on a track that are shared with another track
    private double maxTimeRange;       // Maximum time range in ns spanned by all the hits on a track
    private double maxTanLambda;       // Maximum tan(lambda) for a track seed
    private double maxResidual;        // Maximum residual in units of SSD resolution to add a hit to a track candidate
    private double maxChi2Inc;         // Maximum increment in chi^2 to add a hit to an already completed track
    private double minChi2IncBad;      // Minimum increment in chi^2 to remove a hit from an already completed track
    private double maxResidShare;      // Maximum residual in units of detector resolution for a shared hit
    private double maxChi2IncShare;    // Maximum increment in chi^2 for a hit shared with another track
    private int numEvtPlots;           // Number of event displays to plot (gnuplot files)
    private boolean doDebugPlots;      // Whether to make all the debugging histograms 
    private int siHitsLimit;           // Maximum number of SiClusters in one event allowed for KF pattern reco 
                                       // (protection against monster events) 
    private double seedCompThr;        // Threshold for seedTrack helix parameters compatibility
    
    public String getOutputFullTrackCollectionName() {
        return outputFullTrackCollectionName;
    }

    public void setOutputFullTrackCollectionName(String input) {
        outputFullTrackCollectionName = input;
    }

    public void setVerbose(boolean input) {
        verbose = input;
    }

    public void setUniformB(boolean input) {
        uniformB = input;
        System.out.format("KalmanPatRecDriver: the B field will be assumed uniform.\n");
    }

    public void setMaterialManager(MaterialSupervisor mm) {
        _materialManager = mm;
    }

    public void setTrackCollectionName(String input) {
    }
    
    public void setSiHitsLimit(int input) {
        siHitsLimit = input;
    }
    
    public void setSeedCompThr(double thr) {
        seedCompThr = thr;
    }
    
    @Override
    public void detectorChanged(Detector det) {
        _materialManager = new MaterialSupervisor();
        _materialManager.buildModel(det);

        fm = det.getFieldMap();

        detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (_materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane) (vol));
        }
        
        det.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);

        // Instantiate the interface to the Kalman-Filter code and set up the geometry
        KI = new KalmanInterface(verbose, uniformB);
        KI.setSiHitsLimit(siHitsLimit);
        KI.createSiModules(detPlanes, fm);
        
        decoder = det.getSubdetector("Tracker").getIDDecoder();
        if (doDebugPlots) {
            kPlot = new KalmanPatRecPlots(verbose, KI, decoder, numEvtPlots, fm);
        }
        
        // Change Kalman parameters per settings supplied by the steering file
        // We assume that if not set by the steering file, then the parameters will have the Java default values for the primitives
        // Note that all of the parameters have defaults hard coded in KalmanParams.java
        kPar = KI.getKalmanParams();
        if (numKalmanIteration != 0) kPar.setIterations(numKalmanIteration);
        if (maxPtInverse != 0.0) kPar.setMaxK(maxPtInverse);
        if (maxD0 != 0.0) kPar.setMaxdRho(maxD0);
        if (maxZ0 != 0.0) kPar.setMaxdZ(maxZ0);
        if (maxChi2 != 0.0) kPar.setMaxChi2(maxChi2);
        if (minHits != 0) kPar.setMinHits(minHits);
        if (minStereo != 0) kPar.setMinStereo(minStereo);
        if (maxSharedHits != 0) kPar.setMaxShared(maxSharedHits);
        if (maxTimeRange != 0.0) kPar.setMaxTimeRange(maxTimeRange);
        if (maxTanLambda != 0.0) kPar.setMaxTanL(maxTanLambda);
        if (maxResidual != 0.0) kPar.setMxResid(maxResidual);
        if (maxChi2Inc != 0.0) kPar.setMxChi2Inc(maxChi2Inc);
        if (minChi2IncBad != 0.0) kPar.setMinChi2IncBad(minChi2IncBad);
        if (maxResidShare != 0.0) kPar.setMxResidShare(maxResidShare);
        if (maxChi2IncShare != 0.0) kPar.setMxChi2double(maxChi2IncShare);
        if (seedCompThr != 0.0) kPar.setSeedCompThr(seedCompThr);
        
        // Here we can replace or add search strategies to the pattern recognition (not, as yet, controlled by the steering file)
        // Layers are numbered 0 through 13, and the numbering here corresponds to the bottom tracker. The top-tracker lists are
        // appropriately translated from these. Each seed needs 3 stereo and 2 axial layers
        kPar.clrStrategies();
        int[] list0 = {6, 7, 8, 9, 10};
        int[] list1 = {4, 5, 6, 7, 8};
        int[] list2 = {5, 6, 8, 9, 10};
        int[] list3 = {5, 6, 7, 8, 10};
        int[] list4 = { 3, 6, 8, 9, 10 };
        int[] list5 = { 4, 5, 8, 9, 10 };
        int[] list6 = { 4, 6, 7, 8, 9 };
        int[] list7 = { 4, 6, 7, 9, 10 };
        int[] list8 = { 2, 5, 8, 9, 12};
        int[] list9 = { 8, 10, 11, 12, 13};
        int[] list10 = {6, 9, 10, 11, 12};
        int[] list11 = {6, 7, 9, 10, 12};
        int[] list12 = {2, 3, 4, 5, 6};
        int[] list13 = {2, 4, 5, 6, 7};
        int[] list14 = {6, 7, 8, 10, 11};
        kPar.addStrategy(list0);
        kPar.addStrategy(list1);
        kPar.addStrategy(list2);
        kPar.addStrategy(list3);
        kPar.addStrategy(list4);
        kPar.addStrategy(list5);
        kPar.addStrategy(list6);
        kPar.addStrategy(list7);
        kPar.addStrategy(list8);
        kPar.addStrategy(list9);
        kPar.addStrategy(list10);
        kPar.addStrategy(list11);
        kPar.addStrategy(list12);
        kPar.addStrategy(list13);
        kPar.addStrategy(list14);
        
        System.out.format("KalmanPatRecDriver: the B field is assumed uniform? %b\n", uniformB);
    }

    @Override
    public void process(EventHeader event) {
        
        
        List<Track> outputFullTracks = new ArrayList<Track>();
        List<TrackData> trackDataCollection = new ArrayList<TrackData>();
        List<LCRelation> trackDataRelations = new ArrayList<LCRelation>();
        List<GBLStripClusterData> allClstrs = new ArrayList<GBLStripClusterData>();
        List<LCRelation> gblStripClusterDataRelations  =  new ArrayList<LCRelation>();
        
        prepareTrackCollections(event, outputFullTracks, trackDataCollection, trackDataRelations, allClstrs, gblStripClusterDataRelations);
        
        int flag = 1 << LCIOConstants.TRBIT_HITS;
        event.put(outputFullTrackCollectionName, outputFullTracks, Track.class, flag);
        event.put("GBLStripClusterData", allClstrs, GBLStripClusterData.class, flag);
        event.put("GBLStripClusterDataRelations", gblStripClusterDataRelations, LCRelation.class, flag);
        event.put("KFTrackData",trackDataCollection, TrackData.class,0);
        event.put("KFTrackDataRelations",trackDataRelations,LCRelation.class,0);
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

    private void prepareTrackCollections(EventHeader event, List<Track> outputFullTracks, List<TrackData> trackDataCollection, List<LCRelation> trackDataRelations, List<GBLStripClusterData> allClstrs, List<LCRelation> gblStripClusterDataRelations) {
        
        int evtNumb = event.getEventNumber();
        String stripHitInputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
        if (!event.hasCollection(TrackerHit.class, stripHitInputCollectionName)) {
            System.out.format("KalmanPatRecDriver.process:" + stripHitInputCollectionName + " does not exist; skipping event %d\n", evtNumb);
            return;
        }
        
        long startTime = System.nanoTime();
        ArrayList<KalmanPatRecHPS> kPatList = KI.KalmanPatRec(event, decoder);
        long endTime = System.nanoTime();
        double runTime = (double)(endTime - startTime)/1000000.;
        executionTime += runTime;
        nEvents++;
        Logger.getLogger(KalmanPatRecDriver.class.getName()).log(Level.FINE,
                "KalmanPatRecDriver.process: run time for pattern recognition at event "+evtNumb+" is "+runTime+" milliseconds");
        
        if (kPatList == null) {
            System.out.format("KalmanPatRecDriver.process: null returned by KalmanPatRec. Skipping event %d\n", evtNumb);
            return;
        }
        
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations)
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
        }
        
        List<RawTrackerHit> rawhits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        if (rawhits == null) {
            System.out.format("KalmanPatRecDriver.process: the raw hits collection is missing\n");
            return;
        }
        
        
        int nKalTracks = 0;
        for (KalmanPatRecHPS kPat : kPatList) {
            if (kPat == null) {
                System.out.format("KalmanPatRecDriver.process: pattern recognition failed in the top or bottom tracker for event %d.\n", evtNumb);
                return;
            }
            for (KalTrack kTk : kPat.TkrList) {
                if (verbose) kTk.print(String.format(" PatRec for topBot=%d ",kPat.topBottom));
                double [][] covar = kTk.originCovariance();
                for (int ix=0; ix<5; ++ix) {
                    for (int iy=0; iy<5; ++iy) {
                        if (Double.isNaN(covar[ix][iy])) {
                            System.out.format("KalmanPatRecDriver.process event %d: NaN at %d %d in covariance for track %d\n",evtNumb,ix,iy,kTk.ID);
                        }
                    }
                }                
                nKalTracks++;
                
                //Here is where the tracks to be persisted are formed
                Track KalmanTrackHPS = KI.createTrack(kTk, true);
                if (KalmanTrackHPS == null) continue;
                
                //pT cut 
                //double [] hParams_check = kTk.originHelixParms();
                //double ptInv_check = hParams_check[2];
                //double pt = Math.abs(1./ptInv_check);
                
                outputFullTracks.add(KalmanTrackHPS);
                List<GBLStripClusterData> clstrs = KI.createGBLStripClusterData(kTk);
                if (verbose) {
                    for (GBLStripClusterData clstr : clstrs) {
                        KI.printGBLStripClusterData(clstr);
                    }
                }
                
                //Ecal extrapolation - For the moment done here, but should be moved inside the KalmanInterface (the field map needs to be passed to the KI once)
                BaseTrackState ts_ecal = TrackUtils.getTrackExtrapAtEcalRK(KalmanTrackHPS,fm);
                KalmanTrackHPS.getTrackStates().add(ts_ecal);
                
                allClstrs.addAll(clstrs);
                for (GBLStripClusterData clstr : clstrs) {
                    gblStripClusterDataRelations.add(new BaseLCRelation(KalmanTrackHPS, clstr));
                }
                
                //Set top by default
                int trackerVolume = 0;
                //if tanLamda<0 set bottom
                if (KalmanTrackHPS.getTrackStates().get(0).getTanLambda() < 0) trackerVolume = 1;
                
                //TODO: compute isolations
                double qualityArray[] = new double[1];
                qualityArray[0]= -1;
                
                //Get the track momentum and convert it into detector frame and float values
                Hep3Vector momentum = new BasicHep3Vector(KalmanTrackHPS.getTrackStates().get(0).getMomentum());
                momentum = CoordinateTransformations.transformVectorToDetector(momentum);
                
                float[] momentum_f = new float[3];
                momentum_f[0] = (float) momentum.x();
                momentum_f[1] = (float) momentum.y();
                momentum_f[2] = (float) momentum.z();
                
                //Add the Track Data 
                TrackData KFtrackData = new TrackData(trackerVolume, (float) kTk.getTime(), qualityArray, momentum_f);
                trackDataCollection.add(KFtrackData);
                trackDataRelations.add(new BaseLCRelation(KFtrackData, KalmanTrackHPS));
            } // end of loop on tracks
        } // end of loop on trackers
        
        nTracks += nKalTracks;
        
        if (kPlot != null) kPlot.process(event, runTime, kPatList, rawtomc);
        
        KI.clearInterface();
        if (verbose) System.out.format("\n KalmanPatRecDriver.process: Done with event %d\n", evtNumb);
        

        return;
    }

    @Override
    public void endOfData() {
        System.out.format("KalmanPatRecDrive.endOfData: total pattern recognition execution time=%12.4f ms for %d events and %d tracks.\n", 
                executionTime, nEvents, nTracks);
        if (kPlot != null) kPlot.output();
    }
    
    // Methods to set Kalman parameters from within the steering file
    public void setNumKalmanIteration(int numKalmanIteration) {
        this.numKalmanIteration = numKalmanIteration;
    }
    public void setMaxPtInverse(double maxPtInverse) {
        this.maxPtInverse = maxPtInverse;
    }
    public void setMaxD0(double maxD0) {
        this.maxD0 = maxD0;
    }
    public void setMaxZ0(double maxZ0) {
        this.maxZ0 = maxZ0;
    }
    public void setMaxChi2(double maxChi2) {
        this.maxChi2 = maxChi2;
    }
    public void setMinHits(int minHits) {
        this.minHits = minHits;
    }
    public void setMinStereo(int minStereo) {
        this.minStereo = minStereo;
    }
    public void setMaxSharedHits(int maxSharedHits) {
        this.maxSharedHits = maxSharedHits;
    }
    public void setMaxTimeRange(double maxTimeRange) {
        this.maxTimeRange = maxTimeRange;
    }
    public void setMaxTanLambda(double maxTanLambda) {
        this.maxTanLambda = maxTanLambda;
    }
    public void setMaxResidual(double maxResidual) {
        this.maxResidual = maxResidual;
    }
    public void setMaxChi2Inc(double maxChi2Inc) {
        this.maxChi2Inc = maxChi2Inc;
    }
    public void setMinChi2IncBad(double minChi2IncBad) {
        this.minChi2IncBad = minChi2IncBad;
    }
    public void setMaxResidShare(double maxResidShare) {
        this.maxResidShare = maxResidShare;
    }
    public void setMaxChi2IncShare(double maxChi2IncShare) {
        this.maxChi2IncShare = maxChi2IncShare;
    }
    public void setNumEvtPlots(int numEvtPlots) {
        this.numEvtPlots = numEvtPlots;
    }
    public void setDoDebugPlots(boolean doDebugPlots) {
        this.doDebugPlots = doDebugPlots;
    }
}
