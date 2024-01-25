package org.hps.recon.tracking.kalman;

//import java.io.FileNotFoundException;
//import java.io.PrintWriter;
//import java.util.Collection;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;

import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.gbl.matrix.Matrix;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackState;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.recon.tracking.TrackIntersectData;
import hep.physics.matrix.SymmetricMatrix;
import org.hps.recon.tracking.TrackStateUtils;


import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import org.hps.util.Pair;

//import org.apache.commons.math3.util.Pair;


/**
 * Driver used to compute SVT hit efficiencies at each sensor as a function of
 * strip and y Unbiased Hit Residuals are also computed TODO Cleanup Code, add
 */
public class SvtHitEfficiencyKalman extends Driver {
    
    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree;
    IHistogramFactory histogramFactory;
    boolean debug = false; 
    // List of Sensors
    private List<HpsSiSensor> sensors = null;
    
    Map<String, IHistogram1D> hitTimes = new HashMap<String, IHistogram1D>();

    // Lists of Histograms for efficiencies 
    Map<String, IHistogram1D> numberOfTracksChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksTanL = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerTanL = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPhi = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPhi = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> numberOfTracksTanLVsPhi = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> numberOfTracksWithHitOnMissingLayerTanLVsPhi = new HashMap<String, IHistogram2D>();


    Map<String, IHistogram1D> numberOfTracksChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksTanLEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerTanLEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPhiEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPhiEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> numberOfTracksTanLVsPhiEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> numberOfTracksWithHitOnMissingLayerTanLVsPhiEle = new HashMap<String, IHistogram2D>();


    Map<String, IHistogram1D> numberOfTracksChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksTanLPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerTanLPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPhiPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksWithHitOnMissingLayerPhiPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> numberOfTracksTanLVsPhiPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> numberOfTracksWithHitOnMissingLayerTanLVsPhiPos = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> AllHitsnumberOfTracksChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerChannel = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerP = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksTanL = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerTanL = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksPhi = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerPhi = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksTanLVsPhi = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhi = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> AllHitsnumberOfTracksChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerChannelEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerPEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksTanLEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerTanLEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksPhiEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerPhiEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksTanLVsPhiEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> AllHitsnumberOfTracksChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerChannelPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerPPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksTanLPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerTanLPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksPhiPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> AllHitsnumberOfTracksWithHitOnMissingLayerPhiPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksTanLVsPhiPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiPos = new HashMap<String, IHistogram2D>();


    /*
    Map<String, IHistogram1D> numberOfTracksChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrected = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrected = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> numberOfTracksChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEle = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> numberOfTracksChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksUCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> numberOfTracksPCorrectedPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPos = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> TotalEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPos = new HashMap<String, IHistogram1D>();
   
    Map<String, IHistogram1D> TotalCorrectedEff = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPos = new HashMap<String, IHistogram1D>();
   
    Map<String, IHistogram1D> hitEfficiencyChannelerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPerr = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectederr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectederr = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> hitEfficiencyChannelEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPEleerr = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedEleerr = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> hitEfficiencyChannelPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPPoserr = new HashMap<String, IHistogram1D>();
   
    Map<String, IHistogram1D> hitEfficiencyChannelCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyUCorrectedPoserr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> hitEfficiencyPCorrectedPoserr = new HashMap<String, IHistogram1D>();
   
    Map<String, IHistogram1D> TotalEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalEffPoserr = new HashMap<String, IHistogram1D>();
   
    Map<String, IHistogram1D> TotalCorrectedEfferr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffEleerr = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> TotalCorrectedEffPoserr = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> errorU = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsU = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsV = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsP = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> errorUEle = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsUEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsVEle = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsPEle = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> errorUPos = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram2D> errorUvsUPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsVPos = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> errorUvsPPos = new HashMap<String, IHistogram2D>();
    */


    Map<String, IHistogram2D> tMinus7VsLayer = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> tMinus7VsLayerNHits = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> t0VsLayer = new HashMap<String, IHistogram2D>();
    Map<String, IHistogram2D> t0VsLayerNHits = new HashMap<String, IHistogram2D>();

    Map<String, IHistogram1D> layerHits = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> layerHitsNHits = new HashMap<String, IHistogram1D>();

    Map<String, IHistogram1D> nHoles = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> nHolesNHits = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> layerHoles = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> layerHolesNHits = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> nTrackHits = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> trackTimes = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> D0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Z0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Tanlambda = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Phi0 = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Omega = new HashMap<String, IHistogram1D>();
    
    Map<String, IHistogram1D> D0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Z0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Tanlambda_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Phi0_err = new HashMap<String, IHistogram1D>();
    Map<String, IHistogram1D> Omega_err = new HashMap<String, IHistogram1D>();
 
    Map<String, IHistogram1D> frontLayerPatterns = new HashMap<String, IHistogram1D>();
    
    
    Map<Integer, Integer> hitLayerMap = new HashMap<Integer, Integer>();
    Map<Integer, Integer> holeLayerMap = new HashMap<Integer, Integer>();
    //  the KF code has the number of layers hardcoded at 14 and the ordering is fixed for 12-layer or 14-layer detectors
    //  it uses "KalmanParams.firstLayer" set set the start index of things like the SiModules.
    //  Upside is for 2016 detectors the first two indices (layers 0,1) are empty in lots of things because they correspond to "Layer0"
    //  and layers 2,3 == "Layer1" independent if you have a 2016 or >2019 detector.  
    int maxLayer = 14;           //this is the Kalman layer and is fixed 
    int hpsLayerToKFLayer = 0;   //0 is correct for >2019, set this to 2 for 2016
    int minHOTs = 8;             //minimum number of hits on track required
    int maxHOTs = maxLayer-hpsLayerToKFLayer; //calculate the max number of hits (== n layers) in weird way.  
    int outerLayer = 9;          //the Kalman layer number that the outer detector starts ...is fixed!
    // Histogram Settings
    int nBins = 100;
    int nBins2D = 50;
    double maxPull = 7;
    double minPull = -maxPull;
    double maxRes = 0.5;
    double minRes = -maxRes;
    double maxYerror = 0.1;
    double minTime = -80.0;
    double maxTime = 80.0;
    double maxD0 = 5;
    double minD0 = -maxD0;
    double maxZ0 = 3;
    double minZ0 = -maxZ0;
    double maxTLambda = 0.1;
    double minTLambda = -maxTLambda;
    double maxPhi0 = 0.2;
    double minPhi0 = -maxPhi0;
    double maxOmega = 0.001;
    double minOmega = -maxOmega;
    double maxD0Err = 1.5;
    double minD0Err = 0;
    double maxZ0Err = 1;
    double minZ0Err = 0;
    double maxTLambdaErr = 0.005;
    double minTLambdaErr = 0;
    double maxPhi0Err = 0.01;
    double minPhi0Err = 0;
    double maxOmegaErr = 0.0001;
    double minOmegaErr = 0;
    
    int nChansThin = -666; //fill these when we get sensors
    int nChansThick = -666; 
    String atIP = "IP";

    boolean fillUPlots = false; 
    boolean fillPPlots = false; 
    boolean fillTanLvsPhiPlots = false; 
    boolean fillPatternPlots = false;
    
    // Collection Strings
    private String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String TrackCollectionName = "KalmanFullTracks";
    private String kalIntersectsName = "KFUnbiasInt";
    private String kalIntersectsRelsName = "KFUnbiasIntRelations";
    // Bfield
    protected static double bfield;
    FieldMap bFieldMap = null;
    
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    String outputFileName = "channelEff.txt";
    boolean cleanFEE = false;
    int thinLayCutoff = 4; //thin layers = 0,1,2,3
    double nSig = 5;
    boolean maskBadChannels = false;
    int chanExtd = 1;
    
    boolean useTrkTimeCut = false;
    double trkTimeCut = 10.0; //+/- ns
    double trkTimeMean = 0.0;//true for data...not for MC
    // Daq map
    SvtChannelCollection channelMap;
    SvtDaqMappingCollection daqMap;

    public void setOutputFileName(String outputFileName) {
        this.outputFileName = outputFileName;
    }
    
    public void setSig(double nSig) {
        this.nSig = nSig;
    }

    public void setChanExtd(int chanExtd) {
        this.chanExtd = chanExtd;
    }

    public void setMaskBadChannels(boolean maskBadChannels) {
        this.maskBadChannels = maskBadChannels;
    }

    public void setCleanFEE(boolean cleanFEE) {
        this.cleanFEE = cleanFEE;
    }
    public void setMaxLayer(int nLayers) {
        this.hpsLayerToKFLayer = this.maxLayer-nLayers;
        this.maxHOTs = nLayers; 
        this.maxLayer = nLayers;
    }
    
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    public void setUseTrkTimeCut(boolean useit) {
        this.useTrkTimeCut = useit;
    }

    public void setTrkTimeCut(double val) {
        this.trkTimeCut = val;
    }

    public void setTrkTimeMean(double val) {
        this.trkTimeMean = val;
    }

    // Beam Energy
    double ebeam;

    public void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        tree = aida.tree();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        // Grab channel map and daq map from conditions database
        DatabaseConditionsManager mgr = DatabaseConditionsManager.getInstance();
        SvtConditions svtConditions = mgr.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();

        channelMap = svtConditions.getChannelMap();
        daqMap = svtConditions.getDaqMap();

        // Set Beam Energy
        BeamEnergyCollection beamEnergyCollection = this.getConditionsManager()
                .getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();

        bfield = TrackUtils.getBField(detector).magnitude();
        System.out.println("bfield = "+bfield);
        bFieldMap = detector.getFieldMap();

        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
        
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        List<String> topBot = new ArrayList<String>();
        topBot.add("Top");
        topBot.add("Bottom");
        List<String> elePos = new ArrayList<String>();
        elePos.add("Electron");
        elePos.add("Positron");
        List<String> patStrList = patternStringList(); 
        List<String> modPairList = new ArrayList<String>();
        modPairList.add("hasModPairHit");
        modPairList.add("noModPairHit");

        for(String detHalf: topBot) {
        // Setup Plots
            String ipMapName = atIP + detHalf; 
            String ipHName = detHalf + " " + atIP; 
            trackTimes.put(ipMapName, histogramFactory.createHistogram1D("Track Times " + ipHName, nBins, minTime, maxTime));
            D0.put(ipMapName, histogramFactory.createHistogram1D("D0 " + ipHName, nBins, minD0, maxD0));
            Z0.put(ipMapName, histogramFactory.createHistogram1D("Z0 " + ipHName, nBins, minZ0, maxZ0));
            Tanlambda.put(ipMapName, histogramFactory.createHistogram1D("TanLambda " + ipHName, nBins, minTLambda, maxTLambda));
            Phi0.put(ipMapName, histogramFactory.createHistogram1D("Phi0 " + ipHName, nBins, minPhi0, maxPhi0));
            Omega.put(ipMapName, histogramFactory.createHistogram1D("Momentum " + ipHName, nBins,  0, 1.3 * ebeam));
            
            D0_err.put(ipMapName, histogramFactory.createHistogram1D("D0 Error " + ipHName, nBins, minD0Err, maxD0Err));
            Z0_err.put(ipMapName, histogramFactory.createHistogram1D("Z0 Error " + ipHName, nBins, minZ0Err, maxZ0Err));
            Tanlambda_err.put(ipMapName,
                              histogramFactory.createHistogram1D("TanLambda Error " + ipHName, nBins, minTLambdaErr, maxTLambdaErr));
            Phi0_err.put(ipMapName, histogramFactory.createHistogram1D("Phi0 Error " + ipHName, nBins, minPhi0Err, maxPhi0Err));
            Omega_err.put(ipMapName, histogramFactory.createHistogram1D("Omega Error " + ipHName, nBins, minOmegaErr, maxOmegaErr));

            nTrackHits.put(ipMapName,histogramFactory.createHistogram1D("Number of Track Hits " + detHalf, 15, 0, 15));
            nHoles.put(ipMapName,histogramFactory.createHistogram1D("Number of Holes on Track " + detHalf, 8, 0, 8));
            layerHits.put(ipMapName,histogramFactory.createHistogram1D("Layers Hit " + detHalf, 14, 1, 15));
            layerHoles.put(ipMapName,histogramFactory.createHistogram1D("Hole Layers " + detHalf, 14, 1, 15));
            t0VsLayer.put(ipMapName,histogramFactory.createHistogram2D("t0 vs layer " + detHalf, 14, 1, 15, nBins, minTime, maxTime));
            tMinus7VsLayer.put(ipMapName,histogramFactory.createHistogram2D("t0-t7 vs layer " + detHalf, 14, 1, 15, nBins, minTime, maxTime));

            frontLayerPatterns.put(ipMapName,histogramFactory.createHistogram1D("Front Layer Pattern " + detHalf, 1200, 0, 1200));
            for(String charge: elePos) {
                ipMapName = atIP + detHalf + charge; 
                ipHName = charge + " " + detHalf; 
                String dirCh = charge + "/"; 
                aida.tree().cd("/");
                aida.tree().mkdirs(dirCh);
                D0.put(ipMapName, histogramFactory.createHistogram1D(dirCh + "D0 " + ipHName, nBins, minD0, maxD0));
                Z0.put(ipMapName, histogramFactory.createHistogram1D(dirCh + "Z0 " + ipHName, nBins, minZ0, maxZ0));
                Tanlambda.put(ipMapName, histogramFactory.createHistogram1D(dirCh + "TanLambda " + ipHName, nBins, minTLambda, maxTLambda));
                Phi0.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Phi0 " + ipHName, nBins, minPhi0, maxPhi0));
                Omega.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Momentum " + ipHName,nBins, 0, 1.3 * ebeam ));
                nTrackHits.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Number of Track Hits " + ipHName, 15, 0, 15));
                nHoles.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Number of Holes on Track " + ipHName, 8, 0, 8));
                layerHits.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Layers Hit " + ipHName, 14, 1, 15));
                layerHoles.put(ipMapName, histogramFactory.createHistogram1D(dirCh+"Hole Layers " + ipHName, 14, 1, 15));

                
            }
            for (int hot=minHOTs; hot<=maxHOTs; hot++) {
                String dirHotName = "nHits" + hot+"/";
                String mapHotName = "nHits" + hot + detHalf;
                aida.tree().cd("/");
                aida.tree().mkdirs(dirHotName);
                nHolesNHits.put(mapHotName, histogramFactory.createHistogram1D(dirHotName + "Number of Holes on Track " + detHalf, 8, 0, 8));
                layerHitsNHits.put(mapHotName, histogramFactory.createHistogram1D(dirHotName + "Layers Hit " + detHalf, 14, 1, 15));
                layerHolesNHits.put(mapHotName, histogramFactory.createHistogram1D(dirHotName + "Hole Layers " + detHalf, 14, 1, 15));
                t0VsLayerNHits.put(mapHotName, histogramFactory.createHistogram2D(dirHotName + "t0 vs layer " + detHalf, 14, 1, 15,nBins, minTime, maxTime));
                tMinus7VsLayerNHits.put(mapHotName, histogramFactory.createHistogram2D(dirHotName + "t0-t7 vs layer " + detHalf, 14, 1, 15, nBins, minTime,maxTime));  
            }
            if (fillPatternPlots) {
                for(String pattern: patStrList) {
                    //                System.out.println(pattern); 
                    ipMapName = atIP + detHalf + pattern; 
                    ipHName = detHalf + " " + atIP + " " + pattern; 
                    String dirPat = "Front Layer Pattern " + pattern + "/"; 
                    aida.tree().cd("/");
                    aida.tree().mkdirs(dirPat);
                    D0.put(ipMapName, histogramFactory.createHistogram1D(dirPat + "D0 " + ipHName, nBins, minD0, maxD0));
                    Z0.put(ipMapName, histogramFactory.createHistogram1D(dirPat + "Z0 " + ipHName, nBins, minZ0, maxZ0));
                    Tanlambda.put(ipMapName, histogramFactory.createHistogram1D(dirPat + "TanLambda " + ipHName, nBins, minTLambda, maxTLambda));
                    Phi0.put(ipMapName, histogramFactory.createHistogram1D(dirPat + "Phi0 " + ipHName, nBins, minPhi0, maxPhi0));
                    Omega.put(ipMapName, histogramFactory.createHistogram1D(dirPat + "Momentum " + ipHName,nBins, 0, 1.3 * ebeam ));
                    for(String charge: elePos) {
                        ipMapName = atIP + detHalf + pattern + charge; 
                        ipHName = charge + " " + detHalf + " " + atIP + " " + pattern; 
                        String dirPatCh = "Front Layer Pattern " + pattern + "/" + charge + "/"; 
                        aida.tree().cd("/");
                        aida.tree().mkdirs(dirPatCh);
                        D0.put(ipMapName, histogramFactory.createHistogram1D(dirPatCh + "D0 " + ipHName, nBins, minD0, maxD0));
                        Z0.put(ipMapName, histogramFactory.createHistogram1D(dirPatCh + "Z0 " + ipHName, nBins, minZ0, maxZ0));
                        Tanlambda.put(ipMapName, histogramFactory.createHistogram1D(dirPatCh + "TanLambda " + ipHName, nBins, minTLambda, maxTLambda));
                        Phi0.put(ipMapName, histogramFactory.createHistogram1D(dirPatCh + "Phi0 " + ipHName, nBins, minPhi0, maxPhi0));
                        Omega.put(ipMapName, histogramFactory.createHistogram1D(dirPatCh + "Momentum " + ipHName,nBins, 0, 1.3 * ebeam ));
                    }
                }
            }
        }
        for (HpsSiSensor sensor : sensors) {
            String sensorName = sensor.getName();
            int nChan = sensor.getNumberOfChannels();
            double readoutPitch = sensor.getReadoutStripPitch();
            double maxU = nChan * readoutPitch / 2;
            double width = getSensorLength(sensor);
            double maxV = width / 2;
            double minV = -maxV;            
            int layer = sensor.getLayerNumber();
            if (layer<5) {
                if (debug) System.out.println("Thin layer nChans = " + nChan);
                nChansThin = nChan; 
            } else {
                if (debug) System.out.println("Thick layer nChans = " + nChan);
                nChansThick = nChan;                 
            }
            
            String dirName = "layer" + layer + "/";
            String mapName = sensorName;
            aida.tree().cd("/");
            aida.tree().mkdirs(dirName);
            hitTimes.put(mapName, histogramFactory.createHistogram1D(dirName + "Hit Time " + sensorName, nBins, minTime, maxTime));
            AllHitsnumberOfTracksChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            AllHitsnumberOfTracksWithHitOnMissingLayerChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            if (fillUPlots) {
                AllHitsnumberOfTracksU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U " + sensorName, nBins, -maxU, maxU));
                AllHitsnumberOfTracksWithHitOnMissingLayerU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U " + sensorName, nBins, -maxU, maxU));
            }
            if (fillPPlots) {
                AllHitsnumberOfTracksP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
                AllHitsnumberOfTracksWithHitOnMissingLayerP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
            }
            AllHitsnumberOfTracksTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksWithHitOnMissingLayerPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));


            AllHitsnumberOfTracksChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            AllHitsnumberOfTracksWithHitOnMissingLayerChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            if (fillUPlots) {
                AllHitsnumberOfTracksUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Ele " + sensorName, nBins, -maxU, maxU));
                AllHitsnumberOfTracksWithHitOnMissingLayerUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Ele " + sensorName, nBins, -maxU, maxU));
            }
            if (fillPPlots) {
                AllHitsnumberOfTracksPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                AllHitsnumberOfTracksWithHitOnMissingLayerPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
            }
            AllHitsnumberOfTracksTanLEle.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksWithHitOnMissingLayerPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));


            AllHitsnumberOfTracksChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            AllHitsnumberOfTracksWithHitOnMissingLayerChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Pos " + sensorName,nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
            if (fillUPlots) {
                AllHitsnumberOfTracksUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Pos " + sensorName, nBins, -maxU, maxU));
                AllHitsnumberOfTracksWithHitOnMissingLayerUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Pos " + sensorName, nBins, -maxU, maxU));
            }
            if (fillPPlots) {
                AllHitsnumberOfTracksPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                AllHitsnumberOfTracksWithHitOnMissingLayerPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
            }
            AllHitsnumberOfTracksTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
            AllHitsnumberOfTracksPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksWithHitOnMissingLayerPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
            AllHitsnumberOfTracksTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

            for(String modPair : modPairList) {
                dirName = "layer" + layer + "/" + modPair + "/";
                mapName = sensorName + "-" + modPair;
                aida.tree().cd("/");
                aida.tree().mkdirs(dirName);
                hitTimes.put(mapName, histogramFactory.createHistogram1D(dirName + "Hit Time " + sensorName, nBins, minTime, maxTime));
                AllHitsnumberOfTracksChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                AllHitsnumberOfTracksWithHitOnMissingLayerChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    AllHitsnumberOfTracksU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U " + sensorName, nBins, -maxU, maxU));
                    AllHitsnumberOfTracksWithHitOnMissingLayerU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    AllHitsnumberOfTracksP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
                    AllHitsnumberOfTracksWithHitOnMissingLayerP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                AllHitsnumberOfTracksTanL.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                AllHitsnumberOfTracksPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksWithHitOnMissingLayerPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                
                
                AllHitsnumberOfTracksChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                AllHitsnumberOfTracksWithHitOnMissingLayerChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    AllHitsnumberOfTracksUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Ele " + sensorName, nBins, -maxU, maxU));
                    AllHitsnumberOfTracksWithHitOnMissingLayerUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Ele " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    AllHitsnumberOfTracksPEle.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                    AllHitsnumberOfTracksWithHitOnMissingLayerPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                AllHitsnumberOfTracksTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                AllHitsnumberOfTracksPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksWithHitOnMissingLayerPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                AllHitsnumberOfTracksChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                AllHitsnumberOfTracksWithHitOnMissingLayerChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    AllHitsnumberOfTracksUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Pos " + sensorName, nBins, -maxU, maxU));
                    AllHitsnumberOfTracksWithHitOnMissingLayerUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Pos " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    AllHitsnumberOfTracksPPos.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                    AllHitsnumberOfTracksWithHitOnMissingLayerPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                AllHitsnumberOfTracksTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Pos " + sensorName, nBins,  minTLambda, maxTLambda));
                AllHitsnumberOfTracksPhiPos.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksWithHitOnMissingLayerPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                AllHitsnumberOfTracksTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
            }

            // histogram orginization:  <layer>/<nhits>/<hname> 
            // <nhits> is the number of hits on track -1 if the layer we are looking at has a hit on track!
            for (int hot=minHOTs; hot<maxHOTs; hot++) {//this will count from minHOTs to maxHOTs-1, which is what we want
                dirName = "layer" + layer + "/nHits" + hot + "/";
                mapName = sensorName + "-" + hot; 
                if (debug) System.out.println(" making  sensor-nHOTs name = " + mapName);
                                
                aida.tree().cd("/");
                aida.tree().mkdirs(dirName);
                if (debug) System.out.println("making tree directory with name = " + dirName); 

                numberOfTracksChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    numberOfTracksU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U " + sensorName, nBins, -maxU, maxU));
                    numberOfTracksWithHitOnMissingLayerU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    numberOfTracksP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
                    numberOfTracksWithHitOnMissingLayerP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                numberOfTracksTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                numberOfTracksPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksWithHitOnMissingLayerPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                numberOfTracksChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    numberOfTracksUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Ele " + sensorName, nBins, -maxU, maxU));
                    numberOfTracksWithHitOnMissingLayerUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Ele " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    numberOfTracksPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                    numberOfTracksWithHitOnMissingLayerPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                numberOfTracksTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                numberOfTracksPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksWithHitOnMissingLayerPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                numberOfTracksChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                numberOfTracksWithHitOnMissingLayerChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Pos " + sensorName,nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                if (fillUPlots) {
                    numberOfTracksUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Pos " + sensorName, nBins, -maxU, maxU));
                    numberOfTracksWithHitOnMissingLayerUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Pos " + sensorName, nBins, -maxU, maxU));
                }
                if (fillPPlots) {
                    numberOfTracksPPos.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                    numberOfTracksWithHitOnMissingLayerPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                }
                numberOfTracksTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Pos " + sensorName, nBins,  minTLambda, maxTLambda));
                numberOfTracksPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksWithHitOnMissingLayerPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                numberOfTracksTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                numberOfTracksWithHitOnMissingLayerTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                for(String modPair : modPairList) {
                    dirName = "layer" + layer + "/nHits" + hot + "/" + modPair + "/";
                    mapName = sensorName + "-" + hot + "-" + modPair;
                    aida.tree().cd("/");
                    aida.tree().mkdirs(dirName);
                    numberOfTracksChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    numberOfTracksWithHitOnMissingLayerChannel.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    if (fillUPlots) {
                        numberOfTracksU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U " + sensorName, nBins, -maxU, maxU));
                        numberOfTracksWithHitOnMissingLayerU.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U " + sensorName, nBins, -maxU, maxU));
                    }
                    if (fillPPlots) {
                        numberOfTracksP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P " + sensorName, nBins, 0, 1.3 * ebeam));
                        numberOfTracksWithHitOnMissingLayerP.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P " + sensorName, nBins, 0, 1.3 * ebeam));
                    }
                    numberOfTracksTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanL.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksWithHitOnMissingLayerPhi.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanLVsPhi.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                    numberOfTracksChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    numberOfTracksWithHitOnMissingLayerChannelEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Ele " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    if (fillUPlots) {
                        numberOfTracksUEle.put(mapName,  histogramFactory.createHistogram1D(dirName + "Number of Tracks U Ele " + sensorName, nBins, -maxU, maxU));
                        numberOfTracksWithHitOnMissingLayerUEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Ele " + sensorName, nBins, -maxU, maxU));
                    }
                    if (fillPPlots) {
                        numberOfTracksPEle.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                        numberOfTracksWithHitOnMissingLayerPEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                    }
                    numberOfTracksTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanLEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Ele " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksWithHitOnMissingLayerPhiEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Ele " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanLVsPhiEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Ele " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));

                    numberOfTracksChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    numberOfTracksWithHitOnMissingLayerChannelPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Channel Pos " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                    if (fillUPlots) {
                        numberOfTracksUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks U Pos " + sensorName, nBins, -maxU, maxU));
                        numberOfTracksWithHitOnMissingLayerUPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit U Pos " + sensorName, nBins, -maxU, maxU));
                    }
                    numberOfTracksPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                    numberOfTracksWithHitOnMissingLayerPPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                    numberOfTracksTanLPos.put(mapName,histogramFactory.createHistogram1D(dirName + "Number of Tracks vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanLPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit vs TanLambda Pos " + sensorName, nBins, minTLambda, maxTLambda));
                    numberOfTracksPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksWithHitOnMissingLayerPhiPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Number of Tracks With Hit Phi Pos " + sensorName, nBins, minPhi0, maxPhi0));
                    numberOfTracksTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                    numberOfTracksWithHitOnMissingLayerTanLVsPhiPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Number of Tracks With Hit TanL vs Phi Pos " + sensorName, nBins2D, minPhi0, maxPhi0, nBins2D, minTLambda, maxTLambda));
                }
                
                //hitEfficiencyPPos.put(mapName,histogramFactory.createHistogram1D(dirName + "HitEfficiency P Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                /*
                  numberOfTracksChannelCorrected.put(sensorName,
                    histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Corrected " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrected.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks U Corrected " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrected.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "HitEfficiency U Corrected " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrected.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "HitEfficiency P Corrected " + sensorName, nBins, 0, 1.3 * ebeam));
                            
                            numberOfTracksChannelCorrectedEle.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Corrected Ele " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrectedEle.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected Ele " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks U Corrected Ele " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "HitEfficiency U Corrected Ele " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrectedEle.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "HitEfficiency P Corrected Ele " + sensorName, nBins, 0, 1.3 * ebeam));
                            
                            numberOfTracksChannelCorrectedPos.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "Number of Tracks Channel Corrected Pos " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            hitEfficiencyChannelCorrectedPos.put(sensorName,
                            histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected Pos " + sensorName,
                            nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                            numberOfTracksUCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks U Corrected Pos " + sensorName, nBins, -maxU, maxU));
                            hitEfficiencyUCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "HitEfficiency U Corrected Pos " + sensorName, nBins, -maxU, maxU));
                            numberOfTracksPCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "Number of Tracks P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                            hitEfficiencyPCorrectedPos.put(sensorName, histogramFactory
                            .createHistogram1D(dirName + "HitEfficiency P Corrected Pos " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                TotalEff.put(mapName, histogramFactory.createHistogram1D(dirName + "Total Eff " + sensorName, 1, 0, 1));
                TotalEffEle.put(mapName, histogramFactory.createHistogram1D(dirName + "Total Eff Ele " + sensorName, 1, 0, 1));
                TotalEffPos.put(mapName, histogramFactory.createHistogram1D(dirName + "Total Eff Pos " + sensorName, 1, 0, 1));
                */
                /*
                  TotalCorrectedEff.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff " + sensorName, 1, 0, 1));
                  TotalCorrectedEffEle.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff Ele " + sensorName, 1, 0, 1));
                  TotalCorrectedEffPos.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff Pos " + sensorName, 1, 0, 1));
                */
                /*
                hitEfficiencyChannelerr.put(mapName, histogramFactory.createHistogram1D(
                                                                                           "HitEfficiency Channel Error " + sensorName, nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                hitEfficiencyUerr.put(mapName,
                                      histogramFactory.createHistogram1D(dirName + "HitEfficiency U Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPerr.put(mapName,
                                      histogramFactory.createHistogram1D(dirName + "HitEfficiency P Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectederr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectederr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency U Corrected Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectederr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency P Corrected Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                hitEfficiencyChannelEleerr.put(mapName,
                                               histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Ele Error " + sensorName,
                                                                                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                hitEfficiencyUEleerr.put(mapName,
                                         histogramFactory.createHistogram1D(dirName + "HitEfficiency U Ele Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPEleerr.put(mapName, histogramFactory
                                         .createHistogram1D(dirName + "HitEfficiency P Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectedEleerr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected Ele Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectedEleerr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency U Corrected Ele Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectedEleerr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency P Corrected Ele Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                hitEfficiencyChannelPoserr.put(mapName, histogramFactory.createHistogram1D(
                                                                                              "HitEfficiency Channel Pos Error " + sensorName, nChan, -chanExtd, nChan + chanExtd));
                hitEfficiencyUPoserr.put(mapName,
                                         histogramFactory.createHistogram1D(dirName + "HitEfficiency U Pos Error " + sensorName, nBins, -maxU, maxU));
                hitEfficiencyPPoserr.put(mapName, histogramFactory
                                         .createHistogram1D(dirName + "HitEfficiency P Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                  hitEfficiencyChannelCorrectedPoserr.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "HitEfficiency Channel Corrected Pos Error " + sensorName,
                  nChan + 2 * chanExtd, -chanExtd, nChan + chanExtd));
                  hitEfficiencyUCorrectedPoserr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency U Corrected Pos Error " + sensorName, nBins, -maxU, maxU));
                  hitEfficiencyPCorrectedPoserr.put(sensorName, histogramFactory
                  .createHistogram1D(dirName + "HitEfficiency P Corrected Pos Error " + sensorName, nBins, 0, 1.3 * ebeam));
                */
                /*
                TotalEfferr.put(mapName, histogramFactory.createHistogram1D(dirName + "Total Eff Error " + sensorName, 1, 0, 1));
                TotalEffEleerr.put(mapName,
                                   histogramFactory.createHistogram1D(dirName + "Total Eff Ele Error " + sensorName, 1, 0, 1));
                TotalEffPoserr.put(mapName,
                                   histogramFactory.createHistogram1D(dirName + "Total Eff Pos Error " + sensorName, 1, 0, 1));
                */
                /*
                  TotalCorrectedEfferr.put(mapName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff Error " + sensorName, 1, 0, 1));
                  TotalCorrectedEffEleerr.put(mapName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff Ele Error " + sensorName, 1, 0, 1));
                  TotalCorrectedEffPoserr.put(mapName,
                  histogramFactory.createHistogram1D(dirName + "Total Corrected Eff Pos Error " + sensorName, 1, 0, 1));
                */
                /*
                errorU.put(mapName, histogramFactory.createHistogram1D(dirName + "Error U " + sensorName, nBins, 0, maxYerror));
                errorUvsV.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs V " + sensorName, 2 * nBins, minV,
                                                                             maxV, nBins, 0, maxYerror));
                errorUvsU.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs U " + sensorName, 2 * nBins, -maxU,
                                                                             maxU, nBins, 0, maxYerror));
                errorUvsP.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs P " + sensorName, nBins, 0,
                                                                             1.3 * ebeam, nBins, 0, maxYerror));
                
                errorUEle.put(mapName,
                              histogramFactory.createHistogram1D(dirName + "Error U Electrons " + sensorName, nBins, 0, maxYerror));
                errorUvsVEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs V Electrons " + sensorName,
                                                                                2 * nBins, minV, maxV, nBins, 0, maxYerror));
                errorUvsUEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs U Electrons " + sensorName,
                                                                                2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
                errorUvsPEle.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs P Electrons " + sensorName,
                                                                                nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
                
                errorUPos.put(mapName,
                              histogramFactory.createHistogram1D(dirName + "Error U Electrons " + sensorName, nBins, 0, maxYerror));
                errorUvsVPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs V Positrons " + sensorName,
                                                                                2 * nBins, minV, maxV, nBins, 0, maxYerror));
                errorUvsUPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs U Positrons " + sensorName,
                                                                                2 * nBins, -maxU, maxU, nBins, 0, maxYerror));
                errorUvsPPos.put(mapName, histogramFactory.createHistogram2D(dirName + "Error U vs P Positrons " + sensorName,
                                                                                nBins, 0, 1.3 * ebeam, nBins, 0, maxYerror));
                */
                /*
                  D0.put(sensorName, histogramFactory.createHistogram1D(dirName + "D0 " + sensorName, nBins, minD0, maxD0));
                  Z0.put(sensorName, histogramFactory.createHistogram1D(dirName + "Z0 " + sensorName, nBins, minZ0, maxZ0));
                  Tanlambda.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "TanLambda " + sensorName, nBins, minTLambda, maxTLambda));
                  Phi0.put(sensorName, histogramFactory.createHistogram1D(dirName + "Phi0 " + sensorName, nBins, minPhi00, maxPhi00));
                  Omega.put(sensorName, histogramFactory.createHistogram1D(dirName + "Omega " + sensorName, nBins, minOmega, maxOmega));
                  
                  D0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "D0 Error " + sensorName, nBins, minD0Err, maxD0Err));
                  Z0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Z0 Error " + sensorName, nBins, minZ0Err, maxZ0Err));
                  Tanlambda_err.put(sensorName, histogramFactory.createHistogram1D(dirName + "TanLambda Error " + sensorName, nBins,
                  minTLambdaErr, maxTLambdaErr));
                  Phi0_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Phi0 Error " + sensorName, nBins, minPhi0Err, maxPhi0Err));
                  Omega_err.put(sensorName,
                  histogramFactory.createHistogram1D(dirName + "Omega Error " + sensorName, nBins, minOmegaErr, maxOmegaErr));
                */
            }
        }
    }

    public void process(EventHeader event) {
        aida.tree().cd("/");

        // Grab all Kalman Tracks in the event
        List<Track> tracks = event.get(Track.class, TrackCollectionName);
        List<LCRelation> kalIntersectsRelations = event.get(LCRelation.class, kalIntersectsRelsName);
        // Grab all the clusters in the event
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, stripHitOutputCollectionName);

        for (Track track : tracks) {
            int nHits = track.getTrackerHits().size();
            if (nHits<8)
                continue;
           
            TrackIntersectData trkInts = null;
            for (LCRelation intRel : kalIntersectsRelations) {
                if (intRel.getTo() == track) {
                    trkInts = (TrackIntersectData) intRel.getFrom();
                }
            }
            if (trkInts == null) {
                System.out.println("Couldn't find intersect data to go with the track???");
                continue;
            }
            List<TrackState> TStates = track.getTrackStates();
            if (debug)
                for (TrackState ts: TStates ) {
                    System.out.println("Track State::  location = " + ts.getLocation()
                                       + "; reference x = " + ts.getReferencePoint()[0]
                                       + "; reference y = " + ts.getReferencePoint()[1]
                                       + "; reference z = " + ts.getReferencePoint()[2]);
                }
            
            TrackState tStateIP = TStates.get(0);
            double[] covAtIP = TStates.get(0).getCovMatrix();
            SymmetricMatrix LocCovAtIP = new SymmetricMatrix(5, covAtIP, true);
            double tanLambda = TStates.get(0).getTanLambda(); 
            double phi0 = TStates.get(0).getPhi(); 
            double trackP = toHep3(track.getTrackStates().get(0).getMomentum()).magnitude();
            double q = -track.getCharge(); // HelicalTrackFit flips sign of charge
            String topOrBottom = "Top"; 
            if (tanLambda<0)
                topOrBottom = "Bottom";
            String charge = "Positron";
            if (q<0)
                charge = "Electron";
            String chMapName = atIP + topOrBottom + charge;
                
            String ipMapName = atIP + topOrBottom;
            nTrackHits.get(ipMapName).fill(nHits);
            nTrackHits.get(chMapName).fill(nHits);
            String nhitsmap = "nHits" + nHits + topOrBottom;
            //I'm using sensor layer 7 (starting from 1) as the "reference track time"...only works if we have L7 hit of course...
            boolean hasL7 = false;
            double l7time = -666;

            for (TrackerHit stripCluster : track.getTrackerHits()) {
                int lay=((HpsSiSensor) ((RawTrackerHit) stripCluster.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                if (lay == 7) {
                    hasL7 = true;
                    l7time = stripCluster.getTime();
                }
            }

            // ok, now fill some time histos and get totalT0 for track time 
            double totalT0 = 0; 
            for (TrackerHit stripCluster : track.getTrackerHits()) {
                double t0 = stripCluster.getTime();
                totalT0 += t0;
                int lay = ((HpsSiSensor) ((RawTrackerHit) stripCluster.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
                if (hasL7 && lay!=7) {
                    double tMinus7 = t0 - l7time; 
                    tMinus7VsLayer.get(ipMapName).fill(lay, tMinus7);                    
                    tMinus7VsLayerNHits.get(nhitsmap).fill(lay, tMinus7);                    
                }
                t0VsLayer.get(ipMapName).fill(lay, t0);
                t0VsLayerNHits.get(nhitsmap).fill(lay, t0);
                layerHits.get(ipMapName).fill(lay);
                layerHits.get(chMapName).fill(lay);
                layerHitsNHits.get(nhitsmap).fill(lay);
            }
            //cut on the track times.  
            if (useTrkTimeCut && Math.abs(totalT0/nHits-trkTimeMean)>trkTimeCut) {
                if (debug) System.out.println("Cutting track with time = " + totalT0/nHits);
                continue;
            }
            trackTimes.get(ipMapName).fill(totalT0/nHits); 
            D0.get(ipMapName).fill(TStates.get(0).getD0());            
            Z0.get(ipMapName).fill(TStates.get(0).getZ0());
            Tanlambda.get(ipMapName).fill(TStates.get(0).getTanLambda());
            Phi0.get(ipMapName).fill(TStates.get(0).getPhi());
            Omega.get(ipMapName).fill(trackP);
            
            D0_err.get(ipMapName).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.dcaIndex, HelicalTrackFit.dcaIndex)));
            Z0_err.get(ipMapName).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.z0Index, HelicalTrackFit.z0Index)));
            Tanlambda_err.get(ipMapName).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.slopeIndex, HelicalTrackFit.slopeIndex)));
            Phi0_err.get(ipMapName).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.phi0Index, HelicalTrackFit.phi0Index)));
            Omega_err.get(ipMapName).fill(Math.sqrt(LocCovAtIP.e(HelicalTrackFit.curvatureIndex, HelicalTrackFit.curvatureIndex)));
            
            Hep3Vector p = toHep3(tStateIP.getMomentum());
            int nHitsOnTrack = track.getTrackerHits().size();
            if (debug) System.out.println("Track has " + trkInts.getNInt() + " integers in layers list for #hits = " + nHitsOnTrack);
            //loop over intersects just to get hole information...this is dumb but easy
            int nHolesCnt = 0;
         
            holeLayerMap.clear();
            hitLayerMap.clear();

            for (int l = 0; l < trkInts.getNInt()-1; l++) {
                int layer = trkInts.getLayer(l);  //this is in Kalman layers, so ok
                Double[] interTmp = trkInts.getIntersect(l);
                Double[] intersect = flipInter(interTmp);
                double intersectU = intersect[0];
                float sigmaU = (float)Math.sqrt(trkInts.getSigma(l));
                int hpsLayer = layer-hpsLayerToKFLayer;   //0 is correct for >2019, set this to 2 for 2016
                if (debug)
                    System.out.println("kalman layer = " + layer + "; hps layer = " + hpsLayer);
                if (hpsLayer<0)
                    continue;

                boolean lyrHasHit = layerHasHit(track, hpsLayer);
                boolean modPairLyrHasHit = hasSensorModulePairHit(track, hpsLayer);

                if (debug && intersectU==-999) {
                    System.out.println("layer = " + layer + "   intersect[0] = " + intersect[0] +
                                       "   intersect[1] = " + intersect[1] +
                                       "   intersect[2] = " + intersect[2]);
                }
                if (cleanFEE) {
                    // Require track to be an electron
                    if (q < 0) {
                        continue;
                    }

                    // Select around the FEE momentum peak
                    if (p.magnitude() < 0.75 * ebeam || p.magnitude() > 1.25 * ebeam) {
                        continue;
                    }
                }

                // See if track is within acceptance of unused layer
                Pair<HpsSiSensor,Integer> sensorChanAccPair = getSensorChanInAcceptance(track, intersect, hpsLayer);

                // Set axial and stereo sensors of the missing layer
                HpsSiSensor sensor = sensorChanAccPair.getFirstElement();

                String sensorName = sensor.getName();
                int hot = nHitsOnTrack;
                if (lyrHasHit)
                    hot = hot-1; 
                if (hot<minHOTs)
                    continue; 
                String modPairString = "noModPairHit";
                if (modPairLyrHasHit) {
                    if (debug) System.out.println("Checking layer "+ hpsLayer + "....has modPairHit!");
                    modPairString = "hasModPairHit";
                }
                String mapName = sensorName + "-" + hot; 
                String allMapName = sensorName;
                String allModPairMapName = sensorName + "-" + modPairString;
                String nhotsModPairMapName = sensorName + "-" + hot + "-" + modPairString;
                // Compute the channel where the track extrapolates to in each sensor
                int chan = sensorChanAccPair.getSecondElement();

                ////////////////////////////////////////////////////////////////////////////////////////////
                //  check if track is within sensor acceptance 
                //                
                //  if the track intersect returns -999, that means the intersect code failed (usually 
                //  because track smoothing failed)...I think this happens if track projects way outside of 
                //  sensor acceptance so mark it false
                boolean inAcceptance = intersectU>-999;

                //debug...see if any hits are found in acceptance on first two layers
                if (debug && hpsLayer<2) {
                    System.out.println("layer = " + hpsLayer + " u-intersection = " + intersectU + " +/- " + sigmaU + "  has hit? " + lyrHasHit);
                }
                
                if (hpsLayer==0  && intersectU>-999) {
                    if (debug) System.out.println("layer = " + hpsLayer + " u-intersection = " + intersectU + " +/- " + sigmaU + "  has hit? " + lyrHasHit);
                }
                //  inner, thin layers
                //channel 1 is the default if position is not on sensor 
                //set acceptance to false
                if (maxLayer>12 && (layer<4 && (chan == 1 || chan>=nChansThin))) {
                    if (debug) System.out.println("layer = " + hpsLayer + " chan = " + chan + "  u-intersection = " + intersectU + " +/- " + sigmaU + "  has hit? " + lyrHasHit);
                    inAcceptance = false;
                }
                //  just generally negative channel numbers are bogus...I include >-999 here so I can print out 
                //  when it does compute an intersection with layer but its out of acceptance 
                if (chan<0 && chan > -999) {
                    if (debug) System.out.println("layer = " + hpsLayer + " chan = " + chan + "   u-intersection = " + intersectU + " +/- " + sigmaU + "  has hit? " + lyrHasHit);
                    inAcceptance = false;
                }                
                //  outer layers
                if (maxLayer>12) {///this is detector with small,thin sensors in front
                    if (layer>4 && chan>nChansThick)
                        inAcceptance = false;
                }

                //  outer layers
                if (maxLayer==12) {///this is detector 2016 detector, all thick sensors.
                    if (chan>nChansThick)
                        inAcceptance = false;
                }


                //////////////////////////////////////////////////////////////////////////  done with  acceptance
                             
                double weight = findWeight(intersectU, sigmaU, sensor);
                
                if (lyrHasHit)
                    hitLayerMap.put(hpsLayer, 1);
                else
                    hitLayerMap.put(hpsLayer, 0);
                if (inAcceptance==false)  // can't have hole if track is not on sensor
                    holeLayerMap.put(hpsLayer, 0);
                if (inAcceptance==true && lyrHasHit==false) {
                    //found a hole
                    nHolesCnt++;
                    holeLayerMap.put(hpsLayer, 1);
                } else if (inAcceptance==true && lyrHasHit==true) {
                    holeLayerMap.put(hpsLayer, 0);  // in acceptance and has hit, no hole
                }

                // Fill the denominator of the efficiency histos
                if (inAcceptance) {
                    if (debug) System.out.println("sensor = " + mapName + " channel = " + chan); 

                    numberOfTracksChannel.get(mapName).fill(chan);
                    AllHitsnumberOfTracksChannel.get(allMapName).fill(chan);
                    AllHitsnumberOfTracksChannel.get(allModPairMapName).fill(chan);
                    numberOfTracksChannel.get(nhotsModPairMapName).fill(chan);

                    if (fillUPlots) {
                        numberOfTracksU.get(mapName).fill(intersectU);
                        AllHitsnumberOfTracksU.get(allMapName).fill(intersectU);
                        AllHitsnumberOfTracksU.get(allModPairMapName).fill(intersectU);
                        numberOfTracksU.get(nhotsModPairMapName).fill(intersectU);
                    } 
                    if (fillPPlots) {
                        numberOfTracksP.get(mapName).fill(trackP);
                        AllHitsnumberOfTracksP.get(allMapName).fill(trackP);
                        AllHitsnumberOfTracksP.get(allModPairMapName).fill(trackP);
                        numberOfTracksP.get(nhotsModPairMapName).fill(trackP);
                    }

                    AllHitsnumberOfTracksTanL.get(allMapName).fill(tanLambda);
                    AllHitsnumberOfTracksPhi.get(allMapName).fill(phi0);
                    AllHitsnumberOfTracksTanLVsPhi.get(allMapName).fill(phi0, tanLambda);
                    AllHitsnumberOfTracksTanL.get(allModPairMapName).fill(tanLambda);
                    AllHitsnumberOfTracksPhi.get(allModPairMapName).fill(phi0);
                    AllHitsnumberOfTracksTanLVsPhi.get(allModPairMapName).fill(phi0, tanLambda);
                    numberOfTracksTanL.get(mapName).fill(tanLambda);
                    numberOfTracksPhi.get(mapName).fill(phi0);
                    numberOfTracksTanLVsPhi.get(mapName).fill(phi0,tanLambda);                                        
                    numberOfTracksTanL.get(nhotsModPairMapName).fill(tanLambda);
                    numberOfTracksPhi.get(nhotsModPairMapName).fill(phi0);
                    numberOfTracksTanLVsPhi.get(nhotsModPairMapName).fill(phi0, tanLambda);

                    /*
                      numberOfTracksChannelCorrected.get(mapName).fill(chan, weight);
                      numberOfTracksUCorrected.get(mapName).fill(intersectU, weight);
                      numberOfTracksPCorrected.get(mapName).fill(trackP, weight);
                    */
                    // Fill electron histograms
                    if (q < 0) {
                        numberOfTracksChannelEle.get(mapName).fill(chan);
                        AllHitsnumberOfTracksChannelEle.get(allMapName).fill(chan);
                        numberOfTracksChannelEle.get(nhotsModPairMapName).fill(chan);
                        AllHitsnumberOfTracksChannelEle.get(allModPairMapName).fill(chan);

                        if (fillUPlots) {
                            numberOfTracksUEle.get(mapName).fill(intersectU);
                            AllHitsnumberOfTracksUEle.get(allMapName).fill(intersectU);
                            numberOfTracksUEle.get(nhotsModPairMapName).fill(intersectU);
                            AllHitsnumberOfTracksUEle.get(allModPairMapName).fill(intersectU);
                        }

                        if (fillPPlots) {
                            numberOfTracksPEle.get(mapName).fill(trackP);
                            AllHitsnumberOfTracksPEle.get(allMapName).fill(trackP);
                            numberOfTracksPEle.get(nhotsModPairMapName).fill(trackP);
                            AllHitsnumberOfTracksPEle.get(allModPairMapName).fill(trackP);
                        }

                        numberOfTracksTanLEle.get(mapName).fill(tanLambda);
                        numberOfTracksPhiEle.get(mapName).fill(phi0);
                        numberOfTracksTanLVsPhiEle.get(mapName).fill(phi0, tanLambda);
                        AllHitsnumberOfTracksTanLEle.get(allMapName).fill(tanLambda);
                        AllHitsnumberOfTracksPhiEle.get(allMapName).fill(phi0);
                        AllHitsnumberOfTracksTanLVsPhiEle.get(allMapName).fill(phi0, tanLambda);
                        AllHitsnumberOfTracksTanLEle.get(allModPairMapName).fill(tanLambda);
                        AllHitsnumberOfTracksPhiEle.get(allModPairMapName).fill(phi0);
                        AllHitsnumberOfTracksTanLVsPhiEle.get(allModPairMapName).fill(phi0, tanLambda);
                        numberOfTracksTanLEle.get(nhotsModPairMapName).fill(tanLambda);
                        numberOfTracksPhiEle.get(nhotsModPairMapName).fill(phi0);
                        numberOfTracksTanLVsPhiEle.get(nhotsModPairMapName).fill(phi0, tanLambda);

                    /*
                          numberOfTracksChannelCorrectedEle.get(mapName).fill(chan, weight);
                          numberOfTracksUCorrectedEle.get(mapName).fill(intersectU, weight);
                          numberOfTracksPCorrectedEle.get(mapName).fill(trackP, weight);
                        */
                    } // Fill positron histograms
                    else {
                        numberOfTracksChannelPos.get(mapName).fill(chan);
                        AllHitsnumberOfTracksChannelPos.get(allMapName).fill(chan);
                        numberOfTracksChannelPos.get(nhotsModPairMapName).fill(chan);
                        AllHitsnumberOfTracksChannelPos.get(allModPairMapName).fill(chan);
                        if (fillUPlots) {
                            numberOfTracksUPos.get(mapName).fill(intersectU);
                            AllHitsnumberOfTracksUPos.get(allMapName).fill(intersectU);
                            numberOfTracksUPos.get(nhotsModPairMapName).fill(intersectU);
                            AllHitsnumberOfTracksUPos.get(allModPairMapName).fill(intersectU);
                        }
                        if (fillPPlots) {
                            numberOfTracksPPos.get(mapName).fill(trackP);
                            AllHitsnumberOfTracksPPos.get(allMapName).fill(trackP);
                            numberOfTracksPPos.get(nhotsModPairMapName).fill(trackP);
                            AllHitsnumberOfTracksPPos.get(allModPairMapName).fill(trackP);
                        }

                        numberOfTracksTanLPos.get(mapName).fill(tanLambda);
                        numberOfTracksPhiPos.get(mapName).fill(phi0);
                        numberOfTracksTanLVsPhiPos.get(mapName).fill(phi0, tanLambda);
                        AllHitsnumberOfTracksTanLPos.get(allMapName).fill(tanLambda);
                        AllHitsnumberOfTracksPhiPos.get(allMapName).fill(phi0);
                        AllHitsnumberOfTracksTanLVsPhiPos.get(allMapName).fill(phi0, tanLambda);
                        AllHitsnumberOfTracksTanLPos.get(allModPairMapName).fill(tanLambda);
                        AllHitsnumberOfTracksPhiPos.get(allModPairMapName).fill(phi0);
                        AllHitsnumberOfTracksTanLVsPhiPos.get(allModPairMapName).fill(phi0, tanLambda);
                        numberOfTracksTanLPos.get(nhotsModPairMapName).fill(tanLambda);
                        numberOfTracksPhiPos.get(nhotsModPairMapName).fill(phi0);
                        numberOfTracksTanLVsPhiPos.get(nhotsModPairMapName).fill(phi0, tanLambda);

                        /*
                          numberOfTracksChannelCorrectedPos.get(mapName).fill(chan, weight);
                          numberOfTracksUCorrectedPos.get(mapName).fill(intersectU, weight);
                          numberOfTracksPCorrectedPos.get(mapName).fill(trackP, weight);
                        */
                    }
                    
                    // Fill the error histos
                    /*
                      errorU.get(mapName).fill(sigmaU);
                      errorUvsV.get(mapName).fill(intersect[1], sigmaU);
                      errorUvsU.get(mapName).fill(intersectU, sigmaU);
                      errorUvsP.get(mapName).fill(p.magnitude(), sigmaU);
                      
                      if (q < 0) {
                      errorUEle.get(mapName).fill(sigmaU);
                      errorUvsVEle.get(mapName).fill(intersect[1], sigmaU);
                      errorUvsUEle.get(mapName).fill(intersectU, sigmaU);
                      errorUvsPEle.get(mapName).fill(p.magnitude(), sigmaU);
                      } else {
                      errorUPos.get(mapName).fill(sigmaU);
                      errorUvsVPos.get(mapName).fill(intersect[1], sigmaU);
                      errorUvsUPos.get(mapName).fill(intersectU, sigmaU);
                      errorUvsPPos.get(mapName).fill(p.magnitude(), sigmaU);
                      }
                    */
                    // If layer has a hit included in track, fill the numerator efficiency histograms
                    if (lyrHasHit) {
                        for (TrackerHit stripCluster : track.getTrackerHits()) {
                            if (((HpsSiSensor) ((RawTrackerHit) stripCluster.getRawHits().get(0)).getDetectorElement()).getLayerNumber()==layer+1)
                                hitTimes.get(allMapName).fill(stripCluster.getTime());                                                         
                        }

                        numberOfTracksWithHitOnMissingLayerChannel.get(mapName).fill(chan);
                        numberOfTracksWithHitOnMissingLayerChannel.get(nhotsModPairMapName).fill(chan);
                        AllHitsnumberOfTracksWithHitOnMissingLayerChannel.get(allMapName).fill(chan);
                        AllHitsnumberOfTracksWithHitOnMissingLayerChannel.get(allModPairMapName).fill(chan);

                        if (fillUPlots) {
                            numberOfTracksWithHitOnMissingLayerU.get(mapName).fill(intersectU);
                            numberOfTracksWithHitOnMissingLayerU.get(nhotsModPairMapName).fill(intersectU);
                            AllHitsnumberOfTracksWithHitOnMissingLayerU.get(allMapName).fill(intersectU);
                            AllHitsnumberOfTracksWithHitOnMissingLayerU.get(allModPairMapName).fill(intersectU);
                        }
                        if (fillPPlots) {
                            numberOfTracksWithHitOnMissingLayerP.get(mapName).fill(trackP);
                            numberOfTracksWithHitOnMissingLayerP.get(nhotsModPairMapName).fill(trackP);
                            AllHitsnumberOfTracksWithHitOnMissingLayerP.get(allMapName).fill(trackP);
                            AllHitsnumberOfTracksWithHitOnMissingLayerP.get(allModPairMapName).fill(trackP);
                        }

                        AllHitsnumberOfTracksWithHitOnMissingLayerTanL.get(allMapName).fill(tanLambda);
                        AllHitsnumberOfTracksWithHitOnMissingLayerPhi.get(allMapName).fill(phi0);
                        AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhi.get(allMapName).fill(phi0, tanLambda);
                        AllHitsnumberOfTracksWithHitOnMissingLayerTanL.get(allModPairMapName).fill(tanLambda);
                        AllHitsnumberOfTracksWithHitOnMissingLayerPhi.get(allModPairMapName).fill(phi0);
                        AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhi.get(allModPairMapName).fill(phi0, tanLambda);
                        numberOfTracksWithHitOnMissingLayerTanL.get(mapName).fill(tanLambda);
                        numberOfTracksWithHitOnMissingLayerPhi.get(mapName).fill(phi0);
                        numberOfTracksWithHitOnMissingLayerTanLVsPhi.get(mapName).fill(phi0, tanLambda);                                        
                        numberOfTracksWithHitOnMissingLayerTanL.get(nhotsModPairMapName).fill(tanLambda);
                        numberOfTracksWithHitOnMissingLayerPhi.get(nhotsModPairMapName).fill(phi0);
                        numberOfTracksWithHitOnMissingLayerTanLVsPhi.get(nhotsModPairMapName).fill(phi0, tanLambda);


                        if (q < 0) {
                            numberOfTracksWithHitOnMissingLayerChannelEle.get(mapName).fill(chan);
                            numberOfTracksWithHitOnMissingLayerChannelEle.get(nhotsModPairMapName).fill(chan);
                            AllHitsnumberOfTracksWithHitOnMissingLayerChannelEle.get(allMapName).fill(chan);
                            AllHitsnumberOfTracksWithHitOnMissingLayerChannelEle.get(allModPairMapName).fill(chan);

                            if (fillUPlots) {
                                numberOfTracksWithHitOnMissingLayerUEle.get(mapName).fill(intersectU);
                                numberOfTracksWithHitOnMissingLayerUEle.get(nhotsModPairMapName).fill(intersectU);
                                AllHitsnumberOfTracksWithHitOnMissingLayerUEle.get(allMapName).fill(intersectU);
                                AllHitsnumberOfTracksWithHitOnMissingLayerUEle.get(allModPairMapName).fill(intersectU);
                            }
                            if (fillPPlots) {
                                numberOfTracksWithHitOnMissingLayerPEle.get(mapName).fill(trackP);
                                numberOfTracksWithHitOnMissingLayerPEle.get(nhotsModPairMapName).fill(trackP);
                                AllHitsnumberOfTracksWithHitOnMissingLayerPEle.get(allMapName).fill(trackP);
                                AllHitsnumberOfTracksWithHitOnMissingLayerPEle.get(allModPairMapName).fill(trackP);
                            }

                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLEle.get(allMapName).fill(tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerPhiEle.get(allMapName).fill(phi0);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiEle.get(allMapName).fill(phi0, tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLEle.get(allModPairMapName).fill(tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerPhiEle.get(allModPairMapName).fill(phi0);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiEle.get(allModPairMapName).fill(phi0, tanLambda);
                            numberOfTracksWithHitOnMissingLayerTanLEle.get(mapName).fill(tanLambda);
                            numberOfTracksWithHitOnMissingLayerPhiEle.get(mapName).fill(phi0);
                            numberOfTracksWithHitOnMissingLayerTanLVsPhiEle.get(mapName).fill(phi0, tanLambda);                                        
                            numberOfTracksWithHitOnMissingLayerTanLEle.get(nhotsModPairMapName).fill(tanLambda);
                            numberOfTracksWithHitOnMissingLayerPhiEle.get(nhotsModPairMapName).fill(phi0);
                            numberOfTracksWithHitOnMissingLayerTanLVsPhiEle.get(nhotsModPairMapName).fill(phi0, tanLambda);

                        } else {
                            numberOfTracksWithHitOnMissingLayerChannelPos.get(mapName).fill(chan);
                            numberOfTracksWithHitOnMissingLayerChannelPos.get(nhotsModPairMapName).fill(chan);
                            AllHitsnumberOfTracksWithHitOnMissingLayerChannelPos.get(allMapName).fill(chan);
                            AllHitsnumberOfTracksWithHitOnMissingLayerChannelPos.get(allModPairMapName).fill(chan);

                            if (fillUPlots) {
                                numberOfTracksWithHitOnMissingLayerUPos.get(mapName).fill(intersectU);
                                numberOfTracksWithHitOnMissingLayerUPos.get(nhotsModPairMapName).fill(intersectU);
                                AllHitsnumberOfTracksWithHitOnMissingLayerUPos.get(allMapName).fill(intersectU);
                                AllHitsnumberOfTracksWithHitOnMissingLayerUPos.get(allModPairMapName).fill(intersectU);
                            }
                            if (fillPPlots) {
                                numberOfTracksWithHitOnMissingLayerPPos.get(mapName).fill(trackP);
                                numberOfTracksWithHitOnMissingLayerPPos.get(nhotsModPairMapName).fill(trackP);
                                AllHitsnumberOfTracksWithHitOnMissingLayerPPos.get(allMapName).fill(trackP);
                                AllHitsnumberOfTracksWithHitOnMissingLayerPPos.get(allModPairMapName).fill(trackP);
                            }

                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLPos.get(allMapName).fill(tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerPhiPos.get(allMapName).fill(phi0);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiPos.get(allMapName).fill(phi0, tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLPos.get(allModPairMapName).fill(tanLambda);
                            AllHitsnumberOfTracksWithHitOnMissingLayerPhiPos.get(allModPairMapName).fill(phi0);
                            AllHitsnumberOfTracksWithHitOnMissingLayerTanLVsPhiPos.get(allModPairMapName).fill(phi0, tanLambda);
                            numberOfTracksWithHitOnMissingLayerTanLPos.get(mapName).fill(tanLambda);
                            numberOfTracksWithHitOnMissingLayerPhiPos.get(mapName).fill(phi0);
                            numberOfTracksWithHitOnMissingLayerTanLVsPhiPos.get(mapName).fill(phi0, tanLambda);                                        
                            numberOfTracksWithHitOnMissingLayerTanLPos.get(nhotsModPairMapName).fill(tanLambda);
                            numberOfTracksWithHitOnMissingLayerPhiPos.get(nhotsModPairMapName).fill(phi0);
                            numberOfTracksWithHitOnMissingLayerTanLVsPhiPos.get(nhotsModPairMapName).fill(phi0, tanLambda);
                        }
                    }
                }
            }
            
            for (Map.Entry<Integer, Integer> hole : holeLayerMap.entrySet()) {                
                layerHoles.get(ipMapName).fill(hole.getKey()+1, hole.getValue());
                layerHoles.get(chMapName).fill(hole.getKey()+1, hole.getValue());
                layerHolesNHits.get(nhitsmap).fill(hole.getKey()+1, hole.getValue());
            }          
            nHoles.get(ipMapName).fill(nHolesCnt);
            nHolesNHits.get(nhitsmap).fill(nHolesCnt); 
            if (fillPatternPlots) {                                
                int pat = getFrontLayersPattern(track); 
                String pattern = getPatternString(pat); 
                frontLayerPatterns.get(ipMapName).fill(pat); 
                ipMapName = atIP + topOrBottom+pattern; 
                D0.get(ipMapName).fill(TStates.get(0).getD0());            
                Z0.get(ipMapName).fill(TStates.get(0).getZ0());
                Tanlambda.get(ipMapName).fill(TStates.get(0).getTanLambda());
                Phi0.get(ipMapName).fill(TStates.get(0).getPhi());
                Omega.get(ipMapName).fill(trackP);
                //not fill the charge+front pattern separated plots...
                ipMapName = atIP + topOrBottom + pattern + charge; 
                D0.get(ipMapName).fill(TStates.get(0).getD0());            
                Z0.get(ipMapName).fill(TStates.get(0).getZ0());
                Tanlambda.get(ipMapName).fill(TStates.get(0).getTanLambda());
                Phi0.get(ipMapName).fill(TStates.get(0).getPhi());
                Omega.get(ipMapName).fill(trackP);
                //all tracks just separated by charge
                ipMapName = atIP + topOrBottom + charge; 
                D0.get(ipMapName).fill(TStates.get(0).getD0());            
                Z0.get(ipMapName).fill(TStates.get(0).getZ0());
                Tanlambda.get(ipMapName).fill(TStates.get(0).getTanLambda());
                Phi0.get(ipMapName).fill(TStates.get(0).getPhi());
                Omega.get(ipMapName).fill(trackP);
                nHoles.get(ipMapName).fill(nHolesCnt);
            }
        }
    }

    private Double[] flipInter(Double[] tmp) {
        Double[] thisguy = {tmp[1], tmp[0], tmp[2]};
        return thisguy; 
    }

    // Computes weight based on the number of sigmas (u error) the track
    // extrapolates from the edge of the sensor
    private double findWeight(double y, double yErr, HpsSiSensor sensor) {
        double readoutPitch = sensor.getReadoutStripPitch();
        int nChan = sensor.getNumberOfChannels();
        boolean firstChan = firstChanIsEdge(sensor);
        double height = readoutPitch * nChan;
        double distanceToEdge = 0;
        if (firstChan) {
            distanceToEdge = height / 2 - y;
        } else {
            distanceToEdge = height / 2 + y;
        }
        double nSig = distanceToEdge / yErr;
        return computeGaussInt(nSig, 1000);
    }

    // Computes gaussian integral numerically from -inf to nSig
    private double computeGaussInt(double nSig, int nSteps) {
        double mean = 0;
        double sigma = 1;
        double dx = sigma * nSig / (double) nSteps;
        double integral = 0;
        for (int i = 0; i < nSteps; i++) {
            double x = dx * (i + 0.5) + mean;
            integral += dx * Gauss(x, mean, sigma);
        }
        return integral + 0.5;
    }

    // Gaussian function
    private double Gauss(double x, double mean, double sigma) {
        return 1 / (Math.sqrt(2 * Math.PI * Math.pow(sigma, 2)))
                * Math.exp(-Math.pow(x - mean, 2) / (2 * Math.pow(sigma, 2)));
    }

    // Some sensors have channel 0 closest to the beam
    // Others have channel 640 closest to the beam
    // Use this function to find out which one your sensor is!
    private boolean firstChanIsEdge(HpsSiSensor sensor) {
        int layer = (sensor.getLayerNumber() + 1) / 2;
        if (layer > 0 && layer < 4) {
            if (sensor.isAxial()) {
                return false;
            } else {
                return true;
            }
        } else if (sensor.isAxial()) {
            if (sensor.getSide().matches("ELECTRON")) {
                return false;
            } else {
                return true;
            }
        } else if (!sensor.getSide().matches("ELECTRON")) {
            return false;
        } else {
            return true;
        }
    }

    // Converts position into sensor frame
    private Hep3Vector globalToSensor(Hep3Vector trkpos, HpsSiSensor sensor) {
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (electrodes == null) {
            electrodes = sensor.getReadoutElectrodes(ChargeCarrier.ELECTRON);
            System.out.println("Charge Carrier is NULL");
        }
        if (electrodes == null) {
            System.out.println("Electrodes are null???");
        }
        if (debug)System.out.println("Transforming globalToLocal::  global x =" + trkpos.x() + " y = " + trkpos.y() + " z =" + trkpos.z());
        
        return electrodes.getGlobalToLocal().transformed(trkpos);
    }

    // Returns channel number of a given position in the sensor frame
    private int getChan(Hep3Vector pos, HpsSiSensor sensor) {   
        SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
        if (maxLayer>12 && sensor.getLayerNumber()<5) {//thin sensors
            int row = electrodes.getRowNumber(pos);
            int col = electrodes.getColumnNumber(pos);
            return electrodes.getCellID(row, col);
        }else{
            return electrodes.getCellID(pos);            
        }
        //double readoutPitch = sensor.getReadoutStripPitch();
        //int nChan = sensor.getNumberOfChannels();
        //double height = readoutPitch * nChan;
        //      if (sensor.getLayerNumber()<thinLayCutoff)
        //    return (int)((height / 2 - pos.x()) / readoutPitch);
        //return (int) ((height / 2 - pos.x()) / readoutPitch);
    }

    // Converts double array into Hep3Vector
    private Hep3Vector toHep3(double[] arr) {
        return new BasicHep3Vector(arr[0], arr[1], arr[2]);
    }

    // Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and
    // slot/hole
    private HpsSiSensor getSensor(Track track, int layer) {
        layer = layer + 1;
        if (debug) System.out.println("Getting sensor for layer = " + layer);
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        boolean trkIsTop = tanLambda > 0;
        for (HpsSiSensor sensor : sensors) {
            if (sensor.getLayerNumber() != layer) 
                continue;  // bail...wrong sensor layer

            boolean sensorIsTop = sensor.isTopLayer();
            if (trkIsTop != sensorIsTop)
                continue; // comparing top/bottom...bail

            if (layer < outerLayer && layer > 0) 
                return sensor;//if we are in inner layers, we found sensor!

            //if we got here, must be in outer layers
            boolean isTrackHole = isTrackHole(track,layer);  // this looks for a track state at either hole or slot...not foolproof!
            boolean isSensorHole = sensor.getSide().matches("ELECTRON");
            if (isTrackHole != isSensorHole)
                continue;  /// bail if track hole/slot don't match sensor
            return sensor; //if we get here, we found an outer sensor          
        }
        if (debug) System.out.println("getSensor::  sensor not found!!!!");
        return null;  // we shouldn't ever get here!
    }

    //Return the HpsSiSensor for a given top/bottom track, layer, axial/stereo, and slot/hole
    private HpsSiSensor getSensor(Track track, int layer, boolean isAxial, boolean isHole) {
        double tanLambda = track.getTrackStates().get(0).getTanLambda();
        int outerLayer = 9;
        //TODO unused variable
        boolean trkIsTop = tanLambda > 0;
        for (HpsSiSensor sensor : sensors) {
            int senselayer = sensor.getLayerNumber();  //the "layer" input here has already been incremented so goes 1-14, just like the getLayerNumber() returns
            if (senselayer != layer)
                continue;
            boolean sensorIsTop = sensor.isTopLayer();
            if ((tanLambda > 0 && !sensor.isTopLayer()) || (tanLambda < 0 && sensor.isTopLayer()))
                continue;           
            if (layer < outerLayer && layer > 0)
                return sensor;
            else {
                if ((!sensor.getSide().matches("ELECTRON") && isHole) || (sensor.getSide().matches("ELECTRON") && !isHole))
                    continue;
                return sensor;
            }
        }
        if (debug) System.out.println("getSensor PickEm:: this is just getting the top/bottom/axial/hole sensor...should never happen");
        return null;
    }
    
    // TODO unused function
    private Matrix Hep3ToMatrix(Hep3Matrix mat) {
        int Nrows = mat.getNRows();
        int Ncolumns = mat.getNColumns();
        Matrix matrix = new Matrix(Nrows, Ncolumns);
        for (int i = 0; i < Nrows; i++) {
            for (int j = 0; j < Ncolumns; j++) {
                matrix.set(i, j, mat.e(i, j));
            }
        }
        return matrix;
    }

    private int getUnusedSvtLayer(List<TrackerHit> stereoHits) {
        int[] svtLayer = new int[6];

        // Loop over all of the stereo hits associated with the track
        for (TrackerHit stereoHit : stereoHits) {

            // Retrieve the sensor associated with one of the hits. This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1) / 2;

            // If a hit is associated with that layer, increment its
            // corresponding counter
            svtLayer[layer - 1]++;
        }

        // Loop through the layer counters and find which layer has not been
        // incremented i.e. is unused by the track
        for (int layer = 0; layer < svtLayer.length; layer++) {
            if (svtLayer[layer] == 0) {
                return (layer + 1);
            }
        }
        return -1;
    }

    private Pair<HpsSiSensor,Integer> getSensorChanInAcceptance(Track track, Double[] localPos, int layer) {
        HpsSiSensor sensor = getSensor(track, layer);
        if (debug) System.out.println("getSensorChanInAcceptance: " + sensor.getName());  
        double[] dumbthing = new double[]{localPos[0], localPos[1], localPos[2]};
        Hep3Vector locPosVec = new BasicHep3Vector(dumbthing);
        Integer chanInAcc = new Integer(this.getChan(locPosVec, sensor));

        return new Pair<>(sensor, chanInAcc);

    }

    // Checks to see if track is in acceptance of sensor. Computes within sensor
    // frame
    // Also return channel number of the position
    public Pair<Integer, Boolean> sensorContainsTrack(Hep3Vector localPos, HpsSiSensor sensor) {
        int nChan = sensor.getNumberOfChannels();
        int chan = getChan(localPos, sensor);
        boolean inAcceptance = true; 
        if (localPos.x()<-888)
            inAcceptance = false;              
        return new Pair<>(chan, inAcceptance);
    }

    //Checks to see if track is in acceptance of sensor. Computes within sensor frame
    //Also return channel number of the position
    // TODO unused function
    /*
    public Pair<Boolean, Pair<Integer, Hep3Vector>> sensorContainsTrack(Hep3Vector trackPosition, HpsSiSensor sensor) {
        Hep3Vector pos = globalToSensor(trackPosition, sensor);
        int nChan = sensor.getNumberOfChannels();
        int chan = getChan(pos, sensor);
        double width = getSensorLength(sensor);
        Pair<Integer, Hep3Vector> pair = new Pair<>(chan, pos);
        if (chan < -this.chanExtd || chan > (nChan + this.chanExtd))
            return new Pair<>(false, pair);
        if (Math.abs(pos.y()) > width / 2)
            return new Pair<>(false, pair);
        return new Pair<>(true, pair);
    }
    */

    boolean layerHasHit(Track track, int layer) {
        layer = layer+1;
        for (TrackerHit hit : track.getTrackerHits()) {
            // Retrieve the sensor associated with one of the hits.  This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();
            if (sensor.getLayerNumber() == layer) {
                if (sensor.getLayerNumber()==1 && debug)
                    System.out.println("Yup found hit for layer = " + layer);                
                return true; 
            }
        }
        return false;
    }

    //Check to see if track is hole side
    private boolean isTrackHole(Track track, int unusedLay) {
        boolean isTop = track.getTrackStates().get(0).getTanLambda() > 0;  
        int layer = unusedLay;
        HpsSiSensor sensorHole = getSensor(track, layer, isTop, true);
        HpsSiSensor sensorSlot = getSensor(track, layer, isTop, false);
        HelicalTrackFit htf = TrackUtils.getHTF(track.getTrackStates().get(0));        
        // extrapolate to hole sensor
        Hep3Vector trackHolePosGlobal = TrackStateUtils.getLocationAtSensor(htf, sensorHole, bfield);
        if (trackHolePosGlobal==null) {
            System.out.println("isTrackHole::  trackHolePosGlobal is null ... setting isTrackHole=false");
            return false;
        }
        Hep3Vector trackHolePos = globalToSensor(trackHolePosGlobal, sensorHole);
        if (debug)
            System.out.println("isTrackHole::  global position at Hole:  x =" + trackHolePosGlobal.x() +
                               "; y = " + trackHolePosGlobal.y() +
                               "; z = " + trackHolePosGlobal.z());
        if (debug)
            System.out.println("isTrackHole::  local position at Hole:  x =" + trackHolePos.x() +
                               "; y = " + trackHolePos.y() +
                               "; z = " + trackHolePos.z());
        //    do slot sensor
        Hep3Vector trackSlotPosGlobal = TrackStateUtils.getLocationAtSensor(htf, sensorSlot, bfield);
        if (trackSlotPosGlobal==null) {
            System.out.println("isTrackHole::  trackSlotPosGlobal is null ... setting isTrackSlot=false");
            return true;
        }
        Hep3Vector trackSlotPos = globalToSensor(trackSlotPosGlobal, sensorSlot);
        if (debug)
            System.out.println("isTrackHole::  global position at Slot:  x =" + trackSlotPosGlobal.x() +
                               "; y = " + trackSlotPosGlobal.y() +
                               "; z = " + trackSlotPosGlobal.z());

      
        if (debug)
            System.out.println("isTrackHole::  local position at Slot:  x =" + trackSlotPos.x() +
                               "; y = " + trackSlotPos.y() +
                               "; z = " + trackSlotPos.z()); 
        double yHole = Math.abs(trackHolePos.y());
        double ySlot = Math.abs(trackSlotPos.y());
        
        if (yHole<ySlot)
            return true;
        else
            return false; 

        //           TrackState tState = TrackStateUtils.getTrackStateAtSensor(track, sensorHole.getMillepedeId());
        //if (tState != null)
        //    return true;  // it is indeed hole side
        
        //tState = TrackStateUtils.getTrackStateAtSensor(track, sensorSlot.getMillepedeId());
        //if (tState!=null)
        //    return false; //it is slot side, not hole side 
        //whelp, no track state here...return false anyway
        //           if (debug)System.out.println("isTrackHole::  neither slot nor hole had track state.  This shouldn't happen");
        //        return false; 
    }

    //Checks to see if track is within acceptance of both axial and stereo sensors at a given layer
    //Also returns channel number of the intersection
    // TODO can this be removed?
    /*
    private boolean isTrackHole(Track track, TrackState tState, int layer, boolean axial, Hep3Vector p, FieldMap fieldMap) {

        HpsSiSensor axialSensorHole = getSensor(track, layer, true, true);
        HpsSiSensor axialSensorSlot = getSensor(track, layer, true, false);
        HpsSiSensor stereoSensorHole = getSensor(track, layer, false, true);
        HpsSiSensor stereoSensorSlot = getSensor(track, layer, false, false);

        HelicalTrackFit htf = TrackUtils.getHTF(tState);

        Hep3Vector axialTrackHolePos = TrackStateUtils.getLocationAtSensor(htf, axialSensorHole, bfield);
        Hep3Vector axialTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf, axialSensorSlot, bfield);
        Hep3Vector stereoTrackHolePos = TrackStateUtils.getLocationAtSensor(htf, stereoSensorHole, bfield);
        Hep3Vector stereoTrackSlotPos = TrackStateUtils.getLocationAtSensor(htf, stereoSensorSlot, bfield);

        Pair<Boolean, Pair<Integer, Hep3Vector>> axialHolePair = this.sensorContainsTrack(axialTrackHolePos, axialSensorHole);
        Pair<Boolean, Pair<Integer, Hep3Vector>> axialSlotPair = this.sensorContainsTrack(axialTrackSlotPos, axialSensorSlot);
        Pair<Boolean, Pair<Integer, Hep3Vector>> stereoHolePair = this.sensorContainsTrack(stereoTrackHolePos, stereoSensorHole);
        Pair<Boolean, Pair<Integer, Hep3Vector>> stereoSlotPair = this.sensorContainsTrack(stereoTrackSlotPos, stereoSensorSlot);

        if (axialHolePair.getFirst() && axial)
            return new Pair<>(axialSensorHole, axialHolePair.getSecond());

        if (axialSlotPair.getFirst() && axial)
            return new Pair<>(axialSensorSlot, axialSlotPair.getSecond());

        if (stereoHolePair.getFirst() && !axial)
            return new Pair<>(stereoSensorHole, stereoHolePair.getSecond());

        if (stereoSlotPair.getFirst() && !axial)
            return new Pair<>(stereoSensorSlot, stereoSlotPair.getSecond());

        return null;
    }
    */

    // Returns the horizontal length of the sensor
    protected double getSensorLength(HpsSiSensor sensor) {
        double length = 0;

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {
            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }

 // Returns the horizontal length of the sensor
    protected double getSensorWidth(HpsSiSensor sensor) {
        double length = 0;
        double width = 0; // TODO unused variable

        // Get the faces normal to the sensor
        final List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid())
                .getFacesNormalTo(new BasicHep3Vector(0, 0, 1));
        for (final Polygon3D face : faces) {
            // Loop through the edges of the sensor face and find the longest one
            final List<LineSegment3D> edges = face.getEdges();
            for (final LineSegment3D edge : edges) {
                if (edge.getLength() > length) {
                    length = edge.getLength();
                }
            }
        }
        return length;
    }

    // TODO what is this?
    public void endOfData() {
        System.out.println("End of Data. Computing Hit Efficiencies");

        // skip this...not useful
        if (1==1) {
            return; 
        }

       
    }
    ///
    boolean hasSensorModulePairHit(Track trk, int layer) {
        int otherLayer = getSensorModulePairLayer(layer);
        return layerHasHit(trk,otherLayer);
        
    }
    //  return the layer number that is in the same module 
    //  as the given layer...eg.  0->1, 1->0, 6->7, etc...
    //  this starts with layer = 0
    //
    int getSensorModulePairLayer (int layer) {
        if (layer%2 == 0)
            return layer+1;
        else 
            return layer-1;                
        
    }

    private int getFrontLayersPattern(Track trk) {
        int pat=0;
        for (TrackerHit stripCluster : trk.getTrackerHits()) {
            int lay = ((HpsSiSensor) ((RawTrackerHit) stripCluster.getRawHits().get(0)).getDetectorElement()).getLayerNumber();
            if (lay==1)
                pat+=1000; 
            if (lay==2)
                pat+=100; 
            if (lay==3)
                pat+=10; 
            if (lay==4)
                pat+=1; 
        }
        return pat; 
    }    

    private String getPatternString(int pat) {       
        String patString = String.format("%04d", pat);  
        if (debug) System.out.println(patString); 
        return patString;        
    }

    private List<String> patternStringList() {
        Integer patInt = 0;
        List<String> patterns = new ArrayList<String>(); 
        for(int l1=0; l1<2; l1++) {
            for(int l2=0; l2<2; l2++) {
                for(int l3=0; l3<2; l3++) {
                    for(int l4=0; l4<2; l4++) {
                        patInt = l1*1000 + l2*100 + l3*10 + 1*l4; 
                        patterns.add(String.format("%04d", patInt));
                    }                    
                }
            }
        }
        return patterns; 
    }

}

//  LocalWords:  ebeam
