package org.hps.users.mrsolt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.ITree;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.hps.conditions.beam.BeamEnergy.BeamEnergyCollection;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;

public class TrackHitEfficiency extends Driver {

    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    protected AIDA aida = AIDA.defaultInstance();
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
    private Map<String, IHistogram1D> trackMomentumPlots = new HashMap<String, IHistogram1D>(); 
    private Map<String, IHistogram1D> trackPlots = new HashMap<String, IHistogram1D>();

    //private Map<SiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<SiSensor, Map<Integer, Hep3Vector>>(); 
    private List<HpsSiSensor> sensors = null;
    private Map<Track, ReconstructedParticle> reconParticleMap = new HashMap<Track, ReconstructedParticle>();
    private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    
    IHistogram1D trackMomentumPlots_top;
    IHistogram1D trackMomentumPlots_bot;
    IHistogram1D HitEfficiency_top;
    IHistogram1D HitEfficiency_bot;
    IHistogram1D HitEfficiency_topElec;
    IHistogram1D HitEfficiency_botElec;
    IHistogram1D HitEfficiency_topPosi;
    IHistogram1D HitEfficiency_botPosi;
    IHistogram1D HitEfficiencyError_topElec;
    IHistogram1D HitEfficiencyError_botElec;
    IHistogram1D HitEfficiencyError_topPosi;
    IHistogram1D HitEfficiencyError_botPosi;
    IHistogram1D HitEfficiencyError_top;
    IHistogram1D HitEfficiencyError_bot;
    IHistogram1D HitEfficiency_Momentum_top;
    IHistogram1D HitEfficiency_Momentum_bot;
    IHistogram1D HitEfficiencyError_Momentum_top;
    IHistogram1D HitEfficiencyError_Momentum_bot;
    
    IHistogram1D HitEfficiencyElec_top;
    IHistogram1D HitEfficiencyPosi_top;
    IHistogram1D HitEfficiencyElec_bot;
    IHistogram1D HitEfficiencyPosi_bot;
    IHistogram1D HitEfficiencyErrorElec_top;
    IHistogram1D HitEfficiencyErrorPosi_top;
    IHistogram1D HitEfficiencyErrorElec_bot;
    IHistogram1D HitEfficiencyErrorPosi_bot;
    
    IHistogram1D HitEfficiencyElecStereo_top;
    IHistogram1D HitEfficiencyElecAxial_top;
    IHistogram1D HitEfficiencyPosiStereo_top;
    IHistogram1D HitEfficiencyPosiAxial_top;
    IHistogram1D HitEfficiencyElecStereo_bot;
    IHistogram1D HitEfficiencyElecAxial_bot;
    IHistogram1D HitEfficiencyPosiStereo_bot;
    IHistogram1D HitEfficiencyPosiAxial_bot;
    IHistogram1D HitEfficiencyErrorElecStereo_top;
    IHistogram1D HitEfficiencyErrorElecAxial_top;
    IHistogram1D HitEfficiencyErrorPosiStereo_top;
    IHistogram1D HitEfficiencyErrorPosiAxial_top;
    IHistogram1D HitEfficiencyErrorElecStereo_bot;
    IHistogram1D HitEfficiencyErrorElecAxial_bot;
    IHistogram1D HitEfficiencyErrorPosiStereo_bot;
    IHistogram1D HitEfficiencyErrorPosiAxial_bot;
	
    IHistogram1D TrackXTop;
    IHistogram1D TrackYTop;
    IHistogram1D TrackXBot;
    IHistogram1D TrackYBot;
    
    IHistogram2D TrackResidualXvsMomentumTop;
    IHistogram2D TrackResidualYvsMomentumTop;
    IHistogram2D TrackResidualXvsMomentumBot;
    IHistogram2D TrackResidualYvsMomentumBot;
    
    IHistogram2D TrackResidualXvsTrackResdidualYTop;
    IHistogram2D TrackResidualXvsTrackResdidualYBot;  
    
    Map<Integer, IHistogram1D> trackMomentum = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_accepted = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_accepted_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_accepted_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_final = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_final_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> trackMomentum_final_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram2D> HitEfficiencyPos_top = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> HitEfficiencyPos_bot = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> HitEfficiencyErrorPos_top = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> HitEfficiencyErrorPos_bot = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram1D> HitPosition_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitPosition_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> UnbiasedResidualX_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> UnbiasedResidualX_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> UnbiasedResidualY_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> UnbiasedResidualY_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram2D> Momentum_UnbiasedResidualX_top = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> Momentum_UnbiasedResidualX_bot = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> Momentum_UnbiasedResidualY_top = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram2D> Momentum_UnbiasedResidualY_bot = new HashMap<Integer,IHistogram2D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_top = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_bot = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_topElec = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_botElec = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_topElec = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_botElec = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_topPosi = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLay_botPosi = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_topPosi = new HashMap<Integer,IHistogram1D>();
    Map<Integer, IHistogram1D> HitEfficiency_MomentumLayError_botPosi = new HashMap<Integer,IHistogram1D>();
    
    int num_lay = 6;
    
    /*double topXResidualOffset1 = -0.0259575; 
    double topYResidualOffset1 = 0.00255140; 
    double botXResidualOffset1 = -.00154694; 
    double botYResidualOffset1 = -0.0103718; 
    
    double topXResidualCut1 = 0.438542;
    double topYResidualCut1 = 0.167872;
    double botXResidualCut1 = 0.434835;
    double botYResidualCut1 = 0.169351;*/
    
    double[] topXResidualOffset = new double[num_lay];
    double[] topYResidualOffset = new double[num_lay]; 
    double[] botXResidualOffset = new double[num_lay]; 
    double[] botYResidualOffset = new double[num_lay]; 
    
    double[] topXResidualCut = new double[num_lay];
    double[] topYResidualCut = new double[num_lay];
    double[] botXResidualCut = new double[num_lay];
    double[] botYResidualCut = new double[num_lay];
    
    double topXResidualOffset1; 
    double topYResidualOffset1; 
    double botXResidualOffset1; 
    double botYResidualOffset1; 
    
    double topXResidualCut1;
    double topYResidualCut1;
    double botXResidualCut1;
    double botYResidualCut1;
    
    double topXResidualOffset2; 
    double topYResidualOffset2; 
    double botXResidualOffset2; 
    double botYResidualOffset2; 
    
    double topXResidualCut2;
    double topYResidualCut2;
    double botXResidualCut2;
    double botYResidualCut2;
    
    double topXResidualOffset3; 
    double topYResidualOffset3; 
    double botXResidualOffset3; 
    double botYResidualOffset3; 
    
    double topXResidualCut3;
    double topYResidualCut3;
    double botXResidualCut3;
    double botYResidualCut3;
    
    double topXResidualOffset4; 
    double topYResidualOffset4; 
    double botXResidualOffset4; 
    double botYResidualOffset4; 
    
    double topXResidualCut4;
    double topYResidualCut4;
    double botXResidualCut4;
    double botYResidualCut4;
    
    double topXResidualOffset5; 
    double topYResidualOffset5; 
    double botXResidualOffset5; 
    double botYResidualOffset5; 
    
    double topXResidualCut5;
    double topYResidualCut5;
    double botXResidualCut5;
    double botYResidualCut5;
    
    double topXResidualOffset6; 
    double topYResidualOffset6; 
    double botXResidualOffset6; 
    double botYResidualOffset6; 
    
    double topXResidualCut6;
    double topYResidualCut6;
    double botXResidualCut6;
    double botYResidualCut6;
    
    
    
    /*double topXResidualOffset1 = 0; 
    double topYResidualOffset1 = 0; 
    double botXResidualOffset1 = 0; 
    double botYResidualOffset1 = 0; 
    
    double topXResidualCut1 = 4;
    double topYResidualCut1 = 3;
    double botXResidualCut1 = 4;
    double botYResidualCut1 = 3;
    
    double topXResidualOffset2 = 0; 
    double topYResidualOffset2 = 0; 
    double botXResidualOffset2 = 0; 
    double botYResidualOffset2 = 0; 
    
    double topXResidualCut2 = 2;
    double topYResidualCut2 = 2;
    double botXResidualCut2 = 2;
    double botYResidualCut2 = 2;
    
    double topXResidualOffset3 = 0; 
    double topYResidualOffset3 = 0; 
    double botXResidualOffset3 = 0; 
    double botYResidualOffset3 = 0; 
    
    double topXResidualCut3 = 2;
    double topYResidualCut3 = 2;
    double botXResidualCut3 = 2;
    double botYResidualCut3 = 2;
    
    double topXResidualOffset4 = 0; 
    double topYResidualOffset4 = 0; 
    double botXResidualOffset4 = 0; 
    double botYResidualOffset4 = 0; 
    
    double topXResidualCut4 = 3;
    double topYResidualCut4 = 2;
    double botXResidualCut4 = 3;
    double botYResidualCut4 = 2;
    
    double topXResidualOffset5 = 0; 
    double topYResidualOffset5 = 0; 
    double botXResidualOffset5 = 0; 
    double botYResidualOffset5 = 0; 
    
    double topXResidualCut5 = 3;
    double topYResidualCut5 = 4;
    double botXResidualCut5 = 3;
    double botYResidualCut5 = 4;
    
    double topXResidualOffset6 = 0; 
    double topYResidualOffset6 = 0; 
    double botXResidualOffset6 = 0; 
    double botYResidualOffset6 = 0; 
    
    double topXResidualCut6 = 6;
    double topYResidualCut6 = 6;
    double botXResidualCut6 = 6;
    double botYResidualCut6 = 6;*/
    
    int nSigCut;
    
    double[] gapXtop = new double[num_lay];
    double[] gapXbot = new double[num_lay];
    double gapXtop4 = 15;
    double gapXtop5 = 21;
    double gapXtop6 = 27;
    double gapXbot4 = 15;
    double gapXbot5 = 21;
    double gapXbot6 = 27;
    

    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    
    boolean debug = false;
    boolean ecalClusterTrackMatch = false;
    boolean cleanTridents = false;
    
    // Plot flags
    boolean enableMomentumPlots = true;
    boolean enableChiSquaredPlots = true;
    boolean enableTrackPositionPlots = true;
    boolean maskBadChannels = false;
    
    double numberOfTopTracksTot = 0;
    double numberOfBotTracksTot = 0;
    double numberOfTopTracksWithHitOnMissingLayerTot = 0; 
    double numberOfBotTracksWithHitOnMissingLayerTot = 0;
    double[] topTracksPerMissingLayer = new double[5];
    double[] topTracksWithHitOnMissingLayer = new double[5];
    double[] bottomTracksPerMissingLayer = new double[5];
    double[] bottomTracksWithHitOnMissingLayer = new double[5];

    int nBinx = 500;
    int nBiny = 700;
    int nP = 20;
    int[] numberOfTopTracks = new int[num_lay];
    int[] numberOfBotTracks = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayer = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayer = new int[num_lay];
    double[] hitEfficiencyTop = new double[num_lay];
    double[] hitEfficiencyBot = new double[num_lay];
    double[] errorTop = new double[num_lay];
    double[] errorBot = new double[num_lay];
    
    int[] numberOfTopTracksElectron = new int[num_lay];
    int[] numberOfBotTracksElectron = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerElectron = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerElectron = new int[num_lay];
    double[] hitEfficiencyTopElectron = new double[num_lay];
    double[] hitEfficiencyBotElectron = new double[num_lay];
    double[] errorTopElectron = new double[num_lay];
    double[] errorBotElectron = new double[num_lay];
    
    int[] numberOfTopTracksPositron = new int[num_lay];
    int[] numberOfBotTracksPositron = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerPositron = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerPositron = new int[num_lay];
    double[] hitEfficiencyTopPositron = new double[num_lay];
    double[] hitEfficiencyBotPositron = new double[num_lay];
    double[] errorTopPositron = new double[num_lay];
    double[] errorBotPositron = new double[num_lay];
    
    int[] numberOfTopTracksElec = new int[num_lay];
    int[] numberOfTopTracksPosi = new int[num_lay];
    int[] numberOfBotTracksElec = new int[num_lay];
    int[] numberOfBotTracksPosi = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerElec = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerPosi = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerElec = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerPosi = new int[num_lay];
    double[] hitEfficiencyTopElec = new double[num_lay];
    double[] hitEfficiencyTopPosi = new double[num_lay];
    double[] hitEfficiencyBotElec = new double[num_lay];
    double[] hitEfficiencyBotPosi = new double[num_lay];
    double[] errorTopElec = new double[num_lay];
    double[] errorTopPosi = new double[num_lay];
    double[] errorBotElec = new double[num_lay];
    double[] errorBotPosi = new double[num_lay];
    
    int[] numberOfTopTracksElecStereo = new int[num_lay];
    int[] numberOfTopTracksElecAxial = new int[num_lay];
    int[] numberOfTopTracksPosiStereo = new int[num_lay];
    int[] numberOfTopTracksPosiAxial = new int[num_lay];
    int[] numberOfBotTracksElecStereo = new int[num_lay];
    int[] numberOfBotTracksElecAxial = new int[num_lay];
    int[] numberOfBotTracksPosiStereo = new int[num_lay];
    int[] numberOfBotTracksPosiAxial = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerElecStereo = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerElecAxial = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerPosiStereo = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayerPosiAxial = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerElecStereo = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerElecAxial = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerPosiStereo = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayerPosiAxial = new int[num_lay];
    double[] hitEfficiencyTopElecStereo = new double[num_lay];
    double[] hitEfficiencyTopElecAxial = new double[num_lay];
    double[] hitEfficiencyTopPosiStereo = new double[num_lay];
    double[] hitEfficiencyTopPosiAxial = new double[num_lay];
    double[] hitEfficiencyBotElecStereo = new double[num_lay];
    double[] hitEfficiencyBotElecAxial = new double[num_lay];
    double[] hitEfficiencyBotPosiStereo = new double[num_lay];
    double[] hitEfficiencyBotPosiAxial = new double[num_lay];
    double[] errorTopElecStereo = new double[num_lay];
    double[] errorTopElecAxial = new double[num_lay];
    double[] errorTopPosiStereo = new double[num_lay];
    double[] errorTopPosiAxial = new double[num_lay];
    double[] errorBotElecStereo = new double[num_lay];
    double[] errorBotElecAxial = new double[num_lay];
    double[] errorBotPosiStereo = new double[num_lay];
    double[] errorBotPosiAxial = new double[num_lay];
    
    int[][][] numberOfTopTracksPos = new int[num_lay][nBinx][nBiny];
    int[][][] numberOfBotTracksPos = new int[num_lay][nBinx][nBiny];
    int[][][] numberOfTopTracksWithHitOnMissingLayerPos = new int[num_lay][nBinx][nBiny];
    int[][][] numberOfBotTracksWithHitOnMissingLayerPos = new int[num_lay][nBinx][nBiny];
    double[][][] hitEfficiencyTopPos = new double[num_lay][nBinx][nBiny];
    double[][][] hitEfficiencyBotPos = new double[num_lay][nBinx][nBiny];
    double[][][] hitEfficiencyErrorTopPos = new double[num_lay][nBinx][nBiny];
    double[][][] hitEfficiencyErrorBotPos = new double[num_lay][nBinx][nBiny];
    int[] numberOfTopTracksMomentum = new int[nP];
    int[] numberOfBotTracksMomentum = new int[nP];
    int[] numberOfTopTracksWithHitOnMissingLayerMomentum = new int[nP];
    int[] numberOfBotTracksWithHitOnMissingLayerMomentum = new int[nP];
    double[] hitEfficiencyMomentumTop = new double[nP];
    double[] hitEfficiencyMomentumBot = new double[nP];
    double[] hitEfficiencyErrorMomentumTop = new double[nP];
    double[] hitEfficiencyErrorMomentumBot = new double[nP];  
    Map<Integer, int[]> numberOfTopTracksMomentumLay = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksMomentumLay = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfTopTracksWithHitOnMissingLayerMomentumLay = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksWithHitOnMissingLayerMomentumLay = new HashMap<Integer,int[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayTop = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayBot = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayTop = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayBot = new HashMap<Integer,double[]>();
    Map<Integer, int[]> numberOfTopTracksWithHitOnMissingLayerMomentumLayElec = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksWithHitOnMissingLayerMomentumLayElec = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfTopTracksMomentumLayElec = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksMomentumLayElec = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfTopTracksMomentumLayPosi = new HashMap<Integer,int[]>();
    Map<Integer, int[]> numberOfBotTracksMomentumLayPosi = new HashMap<Integer,int[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayTopElec = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayBotElec = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayTopElec = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayBotElec = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayTopPosi = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyMomentumLayBotPosi = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayTopPosi = new HashMap<Integer,double[]>();
    Map<Integer, double[]> hitEfficiencyErrorMomentumLayBotPosi = new HashMap<Integer,double[]>();
    
    double lowerLimX = -100;
    double upperLimX = 130;
    double lowerLimY = -70;
    double upperLimY = 70;
    /*double lowerLimX = -10;
    double upperLimX = 20;
    double lowerLimY = -10;
    double upperLimY = 10;*/
    double lowerLimP;
    double upperLimP;
   
    double angle = 0.0305;
    double xComp = Math.sin(angle);
    double yComp = Math.cos(angle);
    double zComp = 0.0;
    Hep3Vector unitNormal = new BasicHep3Vector(xComp,yComp,zComp);
    
    Hep3Vector zPointonPlaneLay1Top = new BasicHep3Vector(0,0,92.09);
    Hep3Vector zPointonPlaneLay2Top = new BasicHep3Vector(0,0,192.1);
    Hep3Vector zPointonPlaneLay3Top = new BasicHep3Vector(0,0,292.2);
    Hep3Vector zPointonPlaneLay4Top = new BasicHep3Vector(0,0,492.4);
    Hep3Vector zPointonPlaneLay5Top = new BasicHep3Vector(0,0,692.7);
    Hep3Vector zPointonPlaneLay6Top = new BasicHep3Vector(0,0,893.2);
    Hep3Vector[] zPointonPlaneTop = new Hep3Vector[num_lay];
    double[] zTop = new double[num_lay];
    
    Hep3Vector zPointonPlaneLay1Bot = new BasicHep3Vector(0,0,107.7);
    Hep3Vector zPointonPlaneLay2Bot = new BasicHep3Vector(0,0,207.9);
    Hep3Vector zPointonPlaneLay3Bot = new BasicHep3Vector(0,0,308.8);
    Hep3Vector zPointonPlaneLay4Bot = new BasicHep3Vector(0,0,508.4);
    Hep3Vector zPointonPlaneLay5Bot = new BasicHep3Vector(0,0,708.7);
    Hep3Vector zPointonPlaneLay6Bot = new BasicHep3Vector(0,0,909.0);
    Hep3Vector[] zPointonPlaneBot = new Hep3Vector[num_lay];
    double[] zBot = new double[num_lay];
    
    double BadRegionXLow = 0;
    double BadRegionYLow = 0;
    double BadRegionXHigh = 0;
    double BadRegionYHigh = 0;
    
    Hep3Vector trackPos = null;
    Hep3Vector frontTrackPos = null;
    Hep3Vector rearTrackPos = null;
    
    // Collections
    private String fsParticlesCollectionName = "FinalStateParticles";
    private String stereoHitCollectionName = "HelicalTrackHits";
    private String trackCollectionName = "MatchedTracks";
    private String clusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";
   
    // Constants
    public static final double SENSOR_LENGTH = 98.33; // mm
    public static final double SENSOR_WIDTH = 38.3399; // mm
    private static final String SUBDETECTOR_NAME = "Tracker";

    // By default, require that all tracks have 5 hits
    int hitsOnTrack = 5;

    /**
     * Default Constructor
     */
    public TrackHitEfficiency(){
    }

    /**
     *  Set the number of stereo hits associated with a track fit.
     *
     *  @param hitsOnTrack : Number of stereo hits associated with a track fit.
     */
    public void setHitsOnTrack(int hitsOnTrack) { 
        this.hitsOnTrack = hitsOnTrack;
    }
    
    public void setTridentsOnly(boolean cleanTridents) { 
        this.cleanTridents = cleanTridents;
    }
    
    public void setTopXResidualOffset1(double topXResidualOffset1) { 
        this.topXResidualOffset1 = topXResidualOffset1;
    }
    
    public void setTopXResidualOffset2(double topXResidualOffset2) { 
        this.topXResidualOffset2 = topXResidualOffset2;
    }
    
    public void setTopXResidualOffset3(double topXResidualOffset3) { 
        this.topXResidualOffset3 = topXResidualOffset3;
    }
    
    public void setTopXResidualOffset4(double topXResidualOffset4) { 
        this.topXResidualOffset4 = topXResidualOffset4;
    }
    
    public void setTopXResidualOffset5(double topXResidualOffset5) { 
        this.topXResidualOffset5 = topXResidualOffset5;
    }
    
    public void setTopXResidualOffset6(double topXResidualOffset6) { 
        this.topXResidualOffset6 = topXResidualOffset6;
    }
    
    public void setBotXResidualOffset1(double botXResidualOffset1) { 
        this.botXResidualOffset1 = botXResidualOffset1;
    }
    
    public void setBotXResidualOffset2(double botXResidualOffset2) { 
        this.botXResidualOffset2 = botXResidualOffset2;
    }
    
    public void setBotXResidualOffset3(double botXResidualOffset3) { 
        this.botXResidualOffset3 = botXResidualOffset3;
    }
    
    public void setBotXResidualOffset4(double botXResidualOffset4) { 
        this.botXResidualOffset4 = botXResidualOffset4;
    }
    
    public void setBotXResidualOffset5(double botXResidualOffset5) { 
        this.botXResidualOffset5 = botXResidualOffset5;
    }
    
    public void setBotXResidualOffset6(double botXResidualOffset6) { 
        this.botXResidualOffset6 = botXResidualOffset6;
    }
    
    public void setTopYResidualOffset1(double topYResidualOffset1) { 
        this.topYResidualOffset1 = topYResidualOffset1;
    }
    
    public void setTopYResidualOffset2(double topYResidualOffset2) { 
        this.topYResidualOffset2 = topYResidualOffset2;
    }
    
    public void setTopYResidualOffset3(double topYResidualOffset3) { 
        this.topYResidualOffset3 = topYResidualOffset3;
    }
    
    public void setTopYResidualOffset4(double topYResidualOffset4) { 
        this.topYResidualOffset4 = topYResidualOffset4;
    }
    
    public void setTopYResidualOffset5(double topYResidualOffset5) { 
        this.topYResidualOffset5 = topYResidualOffset5;
    }
    
    public void setTopYResidualOffset6(double topYResidualOffset6) { 
        this.topYResidualOffset6 = topYResidualOffset6;
    }
    
    public void setBotYResidualOffset1(double botYResidualOffset1) { 
        this.botYResidualOffset1 = botYResidualOffset1;
    }
    
    public void setBotYResidualOffset2(double botYResidualOffset2) { 
        this.botYResidualOffset2 = botYResidualOffset2;
    }
    
    public void setBotYResidualOffset3(double botYResidualOffset3) { 
        this.botYResidualOffset3 = botYResidualOffset3;
    }
    
    public void setBotYResidualOffset4(double botYResidualOffset4) { 
        this.botYResidualOffset4 = botYResidualOffset4;
    }
    
    public void setBotYResidualOffset5(double botYResidualOffset5) { 
        this.botYResidualOffset5 = botYResidualOffset5;
    }
    
    public void setBotYResidualOffset6(double botYResidualOffset6) { 
        this.botYResidualOffset6 = botYResidualOffset6;
    }
    
    public void setTopXResidualCut1(double topXResidualCut1) { 
        this.topXResidualCut1 = topXResidualCut1;
    }
    
    public void setTopXResidualCut2(double topXResidualCut2) { 
        this.topXResidualCut2 = topXResidualCut2;
    }
    
    public void setTopXResidualCut3(double topXResidualCut3) { 
        this.topXResidualCut3 = topXResidualCut3;
    }
    
    public void setTopXResidualCut4(double topXResidualCut4) { 
        this.topXResidualCut4 = topXResidualCut4;
    }
    
    public void setTopXResidualCut5(double topXResidualCut5) { 
        this.topXResidualCut5 = topXResidualCut5;
    }
    
    public void setTopXResidualCut6(double topXResidualCut6) { 
        this.topXResidualCut6 = topXResidualCut6;
    }
    
    public void setBotXResidualCut1(double botXResidualCut1) { 
        this.botXResidualCut1 = botXResidualCut1;
    }
    
    public void setBotXResidualCut2(double botXResidualCut2) { 
        this.botXResidualCut2 = botXResidualCut2;
    }
    
    public void setBotXResidualCut3(double botXResidualCut3) { 
        this.botXResidualCut3 = botXResidualCut3;
    }
    
    public void setBotXResidualCut4(double botXResidualCut4) { 
        this.botXResidualCut4 = botXResidualCut4;
    }
    
    public void setBotXResidualCut5(double botXResidualCut5) { 
        this.botXResidualCut5 = botXResidualCut5;
    }
    
    public void setBotXResidualCut6(double botXResidualCut6) { 
        this.botXResidualCut6 = botXResidualCut6;
    }
    
    public void setTopYResidualCut1(double topYResidualCut1) { 
        this.topYResidualCut1 = topYResidualCut1;
    }
    
    public void setTopYResidualCut2(double topYResidualCut2) { 
        this.topYResidualCut2 = topYResidualCut2;
    }
    
    public void setTopYResidualCut3(double topYResidualCut3) { 
        this.topYResidualCut3 = topYResidualCut3;
    }
    
    public void setTopYResidualCut4(double topYResidualCut4) { 
        this.topYResidualCut4 = topYResidualCut4;
    }
    
    public void setTopYResidualCut5(double topYResidualCut5) { 
        this.topYResidualCut5 = topYResidualCut5;
    }
    
    public void setTopYResidualCut6(double topYResidualCut6) { 
        this.topYResidualCut6 = topYResidualCut6;
    }
    
    public void setBotYResidualCut1(double botYResidualCut1) { 
        this.botYResidualCut1 = botYResidualCut1;
    }
    
    public void setBotYResidualCut2(double botYResidualCut2) { 
        this.botYResidualCut2 = botYResidualCut2;
    }
    
    public void setBotYResidualCut3(double botYResidualCut3) { 
        this.botYResidualCut3 = botYResidualCut3;
    }
    
    public void setBotYResidualCut4(double botYResidualCut4) { 
        this.botYResidualCut4 = botYResidualCut4;
    }
    
    public void setBotYResidualCut5(double botYResidualCut5) { 
        this.botYResidualCut5 = botYResidualCut5;
    }
    
    public void setBotYResidualCut6(double botYResidualCut6) { 
        this.botYResidualCut6 = botYResidualCut6;
    }

    public void setNSigCut(int nSigCut) { 
        this.nSigCut = nSigCut;
    }
    /**
     *  Enable/Disable debug 
     */
    public void  setDebug(boolean debug){
        this.debug = debug;
    }
    
    public void setTrackCollectionName(String trackCollectionName) {
    	this.trackCollectionName = trackCollectionName; 
    }
    
    /**
     * Enable/Disable masking of bad channels
     */
    public void setMaskBadChannels(boolean maskBadChannels){
        this.maskBadChannels = maskBadChannels; 
    }
    
    double ebeam;
    
    public void detectorChanged(Detector detector){
    	
    	aida.tree().cd("/");
    	tree = aida.tree();
        //tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
    
        
        BeamEnergyCollection beamEnergyCollection = 
                this.getConditionsManager().getCachedConditions(BeamEnergyCollection.class, "beam_energies").getCachedData();        
        ebeam = beamEnergyCollection.get(0).getBeamEnergy();
        lowerLimP = 0.0 * ebeam;
        upperLimP = 1.3 * ebeam;
        
        zTop[0] = zPointonPlaneLay1Top.z();
        zTop[1] = zPointonPlaneLay2Top.z();
        zTop[2] = zPointonPlaneLay3Top.z();
        zTop[3] = zPointonPlaneLay4Top.z();
        zTop[4] = zPointonPlaneLay5Top.z();
        zTop[5] = zPointonPlaneLay6Top.z();
        
        zBot[0] = zPointonPlaneLay1Bot.z();
        zBot[1] = zPointonPlaneLay2Bot.z();
        zBot[2] = zPointonPlaneLay3Bot.z();
        zBot[3] = zPointonPlaneLay4Bot.z();
        zBot[4] = zPointonPlaneLay5Bot.z();
        zBot[5] = zPointonPlaneLay6Bot.z();
        
        zPointonPlaneTop[0] = zPointonPlaneLay1Top;
        zPointonPlaneTop[1] = zPointonPlaneLay2Top;
        zPointonPlaneTop[2] = zPointonPlaneLay3Top;
        zPointonPlaneTop[3] = zPointonPlaneLay4Top;
        zPointonPlaneTop[4] = zPointonPlaneLay5Top;
        zPointonPlaneTop[5] = zPointonPlaneLay6Top;
        
        zPointonPlaneBot[0] = zPointonPlaneLay1Top;
        zPointonPlaneBot[1] = zPointonPlaneLay2Top;
        zPointonPlaneBot[2] = zPointonPlaneLay3Top;
        zPointonPlaneBot[3] = zPointonPlaneLay4Top;
        zPointonPlaneBot[4] = zPointonPlaneLay5Top;
        zPointonPlaneBot[5] = zPointonPlaneLay6Top;
        
        topXResidualOffset[0] = this.topXResidualOffset1;
        topYResidualOffset[0] = this.topYResidualOffset1; 
        botXResidualOffset[0] = this.botXResidualOffset1; 
        botYResidualOffset[0] = this.botYResidualOffset1; 
        
        topXResidualCut[0] = this.topXResidualCut1;
        topYResidualCut[0] = this.topYResidualCut1;
        botXResidualCut[0] = this.botXResidualCut1;
        botYResidualCut[0] = this.botYResidualCut1;
        
        topXResidualOffset[1] = this.topXResidualOffset2;
        topYResidualOffset[1] = this.topYResidualOffset2; 
        botXResidualOffset[1] = this.botXResidualOffset2; 
        botYResidualOffset[1] = this.botYResidualOffset2; 
        
        topXResidualCut[1] = this.topXResidualCut2;
        topYResidualCut[1] = this.topYResidualCut2;
        botXResidualCut[1] = this.botXResidualCut2;
        botYResidualCut[1] = this.botYResidualCut2;
        
        topXResidualOffset[2] = this.topXResidualOffset3;
        topYResidualOffset[2] = this.topYResidualOffset3; 
        botXResidualOffset[2] = this.botXResidualOffset3; 
        botYResidualOffset[2] = this.botYResidualOffset3; 
        
        topXResidualCut[2] = this.topXResidualCut3;
        topYResidualCut[2] = this.topYResidualCut3;
        botXResidualCut[2] = this.botXResidualCut3;
        botYResidualCut[2] = this.botYResidualCut3;
        
        topXResidualOffset[3] = this.topXResidualOffset4;
        topYResidualOffset[3] = this.topYResidualOffset4; 
        botXResidualOffset[3] = this.botXResidualOffset4; 
        botYResidualOffset[3] = this.botYResidualOffset4; 
        
        topXResidualCut[3] = this.topXResidualCut4;
        topYResidualCut[3] = this.topYResidualCut4;
        botXResidualCut[3] = this.botXResidualCut4;
        botYResidualCut[3] = this.botYResidualCut4;
        
        topXResidualOffset[4] = this.topXResidualOffset5;
        topYResidualOffset[4] = this.topYResidualOffset5; 
        botXResidualOffset[4] = this.botXResidualOffset5; 
        botYResidualOffset[4] = this.botYResidualOffset5; 
        
        topXResidualCut[4] = this.topXResidualCut5;
        topYResidualCut[4] = this.topYResidualCut5;
        botXResidualCut[4] = this.botXResidualCut5;
        botYResidualCut[4] = this.botYResidualCut5;
        
        topXResidualOffset[5] = this.topXResidualOffset6;
        topYResidualOffset[5] = this.topYResidualOffset6; 
        botXResidualOffset[5] = this.botXResidualOffset6; 
        botYResidualOffset[5] = this.botYResidualOffset6; 
        
        topXResidualCut[5] = this.topXResidualCut6;
        topYResidualCut[5] = this.topYResidualCut6;
        botXResidualCut[5] = this.botXResidualCut6;
        botYResidualCut[5] = this.botYResidualCut6;
        
        gapXtop[0] = 0;
        gapXtop[1] = 0;
        gapXtop[2] = 0;
        gapXtop[3] = gapXtop4;
        gapXtop[4] = gapXtop5;
        gapXtop[5] = gapXtop6;
        
        gapXbot[0] = 0;
        gapXbot[1] = 0;
        gapXbot[2] = 0;
        gapXbot[3] = gapXbot4;
        gapXbot[4] = gapXbot5;
        gapXbot[5] = gapXbot6;
        
        
        // Get the HpsSiSensor objects from the tracker detector element
        sensors = detector.getSubdetector(SUBDETECTOR_NAME)
                          .getDetectorElement().findDescendants(HpsSiSensor.class);
   
        // If the detector element had no sensors associated with it, throw
        // an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("No sensors were found in this detector.");
        }

        // Get the stereo layers from the geometry and build the stereo
        // layer maps
        List<SvtStereoLayer> stereoLayers 
            = ((HpsTracker2) detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement()).getStereoPairs();
        for (SvtStereoLayer stereoLayer : stereoLayers) { 
            if (stereoLayer.getAxialSensor().isTopLayer()) {
                //System.out.println("Adding stereo layer " + stereoLayer.getLayerNumber());
                if (!topStereoLayers.containsKey(stereoLayer.getLayerNumber())) { 
                    topStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                } 
                topStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
            } else { 
                if (!bottomStereoLayers.containsKey(stereoLayer.getLayerNumber())) { 
                    bottomStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                } 
                bottomStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
            }
        }
    
        plotters.put("Event Information", plotterFactory.create("Event information"));
        plotters.get("Event Information").createRegions(2, 3);

        trackPlots.put("Number of tracks", histogramFactory.createHistogram1D("Number of tracks", 10, 0, 10));
        plotters.get("Event Information").region(0).plot(trackPlots.get("Number of tracks"));

        trackPlots.put("Unused Layer", histogramFactory.createHistogram1D("Unused Layer", 6, 1, 7));
        plotters.get("Event Information").region(1).plot(trackPlots.get("Unused Layer"));
        
        trackPlots.put("Unbiased Residual x - Top", histogramFactory.createHistogram1D("Unbiased Residual x - Top", 100, -10, 10));
        plotters.get("Event Information").region(2).plot(trackPlots.get("Unbiased Residual x - Top"));

        trackPlots.put("Unbiased Residual x - Bottom", histogramFactory.createHistogram1D("Unbiased Residual x - Bottom", 100, -10, 10));
        plotters.get("Event Information").region(3).plot(trackPlots.get("Unbiased Residual x - Bottom"));

        trackPlots.put("Unbiased Residual y - Top", histogramFactory.createHistogram1D("Unbiased Residual y - Top", 100, -10, 10));
        plotters.get("Event Information").region(4).plot(trackPlots.get("Unbiased Residual y - Top"));

        trackPlots.put("Unbiased Residual y - Bottom", histogramFactory.createHistogram1D("Unbiased Residual y - Bottom", 100, -10, 10));
        plotters.get("Event Information").region(5).plot(trackPlots.get("Unbiased Residual y - Bottom"));

        plotters.put("Track Momentum", plotterFactory.create("Track Momentum"));
        plotters.get("Track Momentum").createRegions(2, 2);

        trackMomentumPlots.put("Track Momentum", histogramFactory.createHistogram1D("Track Momentum", 50, 0, ebeam*1.3));
        plotters.get("Track Momentum").region(0).plot(trackMomentumPlots.get("Track Momentum"));
           
        trackMomentumPlots.put("Track Momentum - Tracks Within Acceptance",
                histogramFactory.createHistogram1D("Track Momentum - Tracks Within Acceptance", 50, 0, ebeam*1.3));
        plotters.get("Track Momentum").region(1)
                                      .plot(trackMomentumPlots.get("Track Momentum - Tracks Within Acceptance"));

        trackMomentumPlots.put("Track Momentum - All Layers Hit", 
                histogramFactory.createHistogram1D("Track Momentum - All Layers Hit", 50, 0, ebeam*1.3));
        plotters.get("Track Momentum").region(2)
                                      .plot(trackMomentumPlots.get("Track Momentum - All Layers Hit"));
        
        trackMomentumPlots_top = aida.histogram1D("Track Momentum Top", 50, 0, ebeam*1.3);
        trackMomentumPlots_bot = aida.histogram1D("Track Momentum Bot", 50, 0, ebeam*1.3);
        
        HitEfficiency_top = aida.histogram1D("Hit Efficiency Top", num_lay, 0, num_lay);
        HitEfficiency_bot = aida.histogram1D("Hit Efficiency Bot", num_lay, 0, num_lay);
        HitEfficiencyError_top = aida.histogram1D("Hit Efficiency Error Top", num_lay, 0, num_lay);
        HitEfficiencyError_bot = aida.histogram1D("Hit Efficiency Error Bot", num_lay, 0, num_lay);
        HitEfficiency_topElec = aida.histogram1D("Hit Efficiency Top Electrons", num_lay, 0, num_lay);
        HitEfficiency_botElec = aida.histogram1D("Hit Efficiency Bot Electrons", num_lay, 0, num_lay);
        HitEfficiencyError_topElec = aida.histogram1D("Hit Efficiency Error Top Electrons", num_lay, 0, num_lay);
        HitEfficiencyError_botElec = aida.histogram1D("Hit Efficiency Error Bot Electrons", num_lay, 0, num_lay);
        HitEfficiency_topPosi = aida.histogram1D("Hit Efficiency Top Positrons", num_lay, 0, num_lay);
        HitEfficiency_botPosi = aida.histogram1D("Hit Efficiency Bot Positrons", num_lay, 0, num_lay);
        HitEfficiencyError_topPosi = aida.histogram1D("Hit Efficiency Error Top Positrons", num_lay, 0, num_lay);
        HitEfficiencyError_botPosi = aida.histogram1D("Hit Efficiency Error Bot Positrons", num_lay, 0, num_lay);
        HitEfficiencyElec_top = aida.histogram1D("Hit Efficiency Top Electron Side", num_lay, 0, num_lay);
        HitEfficiencyPosi_top = aida.histogram1D("Hit Efficiency Top Positron Side", num_lay, 0, num_lay);
        HitEfficiencyElec_bot = aida.histogram1D("Hit Efficiency Bot Electron Side", num_lay, 0, num_lay);
        HitEfficiencyPosi_bot = aida.histogram1D("Hit Efficiency Bot Positron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorElec_top = aida.histogram1D("Hit Efficiency Error Top Electron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorPosi_top = aida.histogram1D("Hit Efficiency Error Top Positron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorElec_bot = aida.histogram1D("Hit Efficiency Error Bot Electron", num_lay, 0, num_lay);
        HitEfficiencyErrorPosi_bot = aida.histogram1D("Hit Efficiency Error Bot Positron", num_lay, 0, num_lay);
        
        HitEfficiencyElecStereo_top = aida.histogram1D("Hit Efficiency Top Stereo Electron Side", num_lay, 0, num_lay);
        HitEfficiencyElecAxial_top = aida.histogram1D("Hit Efficiency Top Axial Electron Side", num_lay, 0, num_lay);
        HitEfficiencyPosiStereo_top = aida.histogram1D("Hit Efficiency Top Stereo Positron Side", num_lay, 0, num_lay);
        HitEfficiencyPosiAxial_top = aida.histogram1D("Hit Efficiency Top Axial Positron Side", num_lay, 0, num_lay);
        HitEfficiencyElecStereo_bot = aida.histogram1D("Hit Efficiency Bot Stereo Electron Side", num_lay, 0, num_lay);
        HitEfficiencyElecAxial_bot = aida.histogram1D("Hit Efficiency Bot Axial Electron Side", num_lay, 0, num_lay);
        HitEfficiencyPosiStereo_bot = aida.histogram1D("Hit Efficiency Bot Stereo Positron Side", num_lay, 0, num_lay);
        HitEfficiencyPosiAxial_bot = aida.histogram1D("Hit Efficiency Bot Axial Positron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorElecStereo_top = aida.histogram1D("Hit Efficiency Error Top Stereo Electron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorElecAxial_top = aida.histogram1D("Hit Efficiency Error Top Axial Electron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorPosiStereo_top = aida.histogram1D("Hit Efficiency Error Top Stereo Positron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorPosiAxial_top = aida.histogram1D("Hit Efficiency Error Top Axial Positron Side", num_lay, 0, num_lay);
        HitEfficiencyErrorElecStereo_bot = aida.histogram1D("Hit Efficiency Error Bot Stereo Electron", num_lay, 0, num_lay);
        HitEfficiencyErrorElecAxial_bot = aida.histogram1D("Hit Efficiency Error Bot Axial Electron", num_lay, 0, num_lay);
        HitEfficiencyErrorPosiStereo_bot = aida.histogram1D("Hit Efficiency Error Bot Stereo Positron", num_lay, 0, num_lay);
        HitEfficiencyErrorPosiAxial_bot = aida.histogram1D("Hit Efficiency Error Bot Axial Positron", num_lay, 0, num_lay);
        
        HitEfficiency_Momentum_top = aida.histogram1D("Hit Efficiency Momentum Top", nP, lowerLimP, upperLimP);
        HitEfficiency_Momentum_bot = aida.histogram1D("Hit Efficiency Momentum Bot", nP, lowerLimP, upperLimP);
        HitEfficiencyError_Momentum_top = aida.histogram1D("Hit Efficiency Error Momentum Top", nP, lowerLimP, upperLimP);
        HitEfficiencyError_Momentum_bot = aida.histogram1D("Hit Efficiency Error Momentum Bot", nP, lowerLimP, upperLimP);
        
        TrackXTop = aida.histogram1D("Extrapolated Track X Top", 50, lowerLimX, upperLimX);
        TrackYTop = aida.histogram1D("Extrapolated Track Y Top", 50, lowerLimY, upperLimY);
        TrackXBot = aida.histogram1D("Extrapolated Track X Bot", 50, lowerLimX, upperLimX);
        TrackYBot = aida.histogram1D("Extrapolated Track Y Bot", 50, lowerLimY, upperLimY);
        
        TrackResidualXvsMomentumTop = aida.histogram2D("Track Residual X vs Momentum Top", nP, lowerLimP, upperLimP, 50, -10, 10);
        TrackResidualYvsMomentumTop = aida.histogram2D("Track Residual Y vs Momentum Top", nP, lowerLimP, upperLimP, 50, -10, 10);
        TrackResidualXvsMomentumBot = aida.histogram2D("Track Residual X vs Momentum Top", nP, lowerLimP, upperLimP, 50, -10, 10);
        TrackResidualYvsMomentumBot = aida.histogram2D("Track Residual Y vs Momentum Bot", nP, lowerLimP, upperLimP, 50, -10, 10);
        
        TrackResidualXvsTrackResdidualYTop = aida.histogram2D("Track Residuals X vs Y Top",50,-10, 10, 50, -10, 10);
        TrackResidualXvsTrackResdidualYBot = aida.histogram2D("Track Residuals X vs Y Bot",50,-10, 10, 50, -10, 10);
        
        for(int i = 0; i < num_lay; i++){
        	trackMomentum.put((i+1),histogramFactory.createHistogram1D("Track Momentum Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_top.put((i+1),histogramFactory.createHistogram1D("Track Momentum Top Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_bot.put((i+1),histogramFactory.createHistogram1D("Track Momentum Bot Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_accepted.put((i+1),histogramFactory.createHistogram1D("Track Momentum Accepted Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_accepted_top.put((i+1),histogramFactory.createHistogram1D("Track Momentum Accepted Top Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_accepted_bot.put((i+1),histogramFactory.createHistogram1D("Track Momentum Accepted Bot Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_final.put((i+1),histogramFactory.createHistogram1D("Track Momentum Final Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_final_top.put((i+1),histogramFactory.createHistogram1D("Track Momentum Final Top Layer #" + (i+1), 50, 0, ebeam*1.3));
        	trackMomentum_final_bot.put((i+1),histogramFactory.createHistogram1D("Track Momentum Final Bot Layer #" + (i+1), 50, 0, ebeam*1.3));
        	HitEfficiencyPos_top.put((i+1),histogramFactory.createHistogram2D("Hit Efficiency for Hit Positions Top Layer #" + (i+1), nBinx, lowerLimX, upperLimX, nBiny, lowerLimY, upperLimY));
        	HitEfficiencyPos_bot.put((i+1),histogramFactory.createHistogram2D("Hit Efficiency for Hit Positions Bot Layer #" + (i+1), nBinx, lowerLimX, upperLimX, nBiny, lowerLimY, upperLimY));
        	HitEfficiencyErrorPos_top.put((i+1),histogramFactory.createHistogram2D("Hit Efficiency Error for Hit Positions Top Layer #" + (i+1), nBinx, lowerLimX, upperLimX, nBiny, lowerLimY, upperLimY));
        	HitEfficiencyErrorPos_bot.put((i+1),histogramFactory.createHistogram2D("Hit Efficiency Error for Hit Positions Bot Layer #" + (i+1), nBinx, lowerLimX, upperLimX, nBiny, lowerLimY, upperLimY));
        	HitPosition_top.put((i+1),histogramFactory.createHistogram1D("Hit Position Top Layer #" + (i+1), 1100, 0, 1100));
        	HitPosition_bot.put((i+1),histogramFactory.createHistogram1D("Hit Position Bot Layer #" + (i+1), 1100, 0, 1100));
        	UnbiasedResidualX_top.put((i+1),histogramFactory.createHistogram1D("Unbiased Residual X Top Layer #" + (i+1),100,-10,10));
        	UnbiasedResidualX_bot.put((i+1),histogramFactory.createHistogram1D("Unbiased Residual X Bot Layer #" + (i+1),100,-10,10));
        	UnbiasedResidualY_top.put((i+1),histogramFactory.createHistogram1D("Unbiased Residual Y Top Layer #" + (i+1),100,-10,10));
        	UnbiasedResidualY_bot.put((i+1),histogramFactory.createHistogram1D("Unbiased Residual Y Bot Layer #" + (i+1),100,-10,10));
        	HitEfficiency_MomentumLay_top.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Top Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLay_bot.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Bot Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_top.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Top Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_bot.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Bot Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLay_topElec.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Top Electrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLay_botElec.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Bot Electrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_topElec.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Top Electrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_botElec.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Bot Electrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLay_topPosi.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Top Positrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLay_botPosi.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Momentum Bot Positrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_topPosi.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Top Positrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	HitEfficiency_MomentumLayError_botPosi.put((i+1), histogramFactory.createHistogram1D("Hit Efficiency Error Momentum Bot Positrons Layer #"+ (i+1),nP, lowerLimP, upperLimP));
        	numberOfTopTracksMomentumLay.put((i+1),new int[nP]);
        	numberOfBotTracksMomentumLay.put((i+1),new int[nP]);
        	numberOfTopTracksWithHitOnMissingLayerMomentumLay.put((i+1),new int[nP]);
        	numberOfBotTracksWithHitOnMissingLayerMomentumLay.put((i+1),new int[nP]);
        	numberOfTopTracksMomentumLayElec.put((i+1),new int[nP]);
        	numberOfBotTracksMomentumLayElec.put((i+1),new int[nP]);
        	numberOfTopTracksWithHitOnMissingLayerMomentumLayElec.put((i+1),new int[nP]);
        	numberOfBotTracksWithHitOnMissingLayerMomentumLayElec.put((i+1),new int[nP]);
        	numberOfTopTracksMomentumLayPosi.put((i+1),new int[nP]);
        	numberOfBotTracksMomentumLayPosi.put((i+1),new int[nP]);
        	numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi.put((i+1),new int[nP]);
        	numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi.put((i+1),new int[nP]);
        	hitEfficiencyMomentumLayTop.put((i+1),new double[nP]);
        	hitEfficiencyMomentumLayBot.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayTop.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayBot.put((i+1),new double[nP]);
        	hitEfficiencyMomentumLayTopElec.put((i+1),new double[nP]);
        	hitEfficiencyMomentumLayBotElec.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayTopElec.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayBotElec.put((i+1),new double[nP]);
        	hitEfficiencyMomentumLayTopPosi.put((i+1),new double[nP]);
        	hitEfficiencyMomentumLayBotPosi.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayTopPosi.put((i+1),new double[nP]);
        	hitEfficiencyErrorMomentumLayBotPosi.put((i+1),new double[nP]);
        	Momentum_UnbiasedResidualX_top.put((i+1), histogramFactory.createHistogram2D("Track Residuals X vs Momentum Top Layer #" + (i+1),  nP, lowerLimP, upperLimP, 50, -10, 10));
        	Momentum_UnbiasedResidualX_bot.put((i+1), histogramFactory.createHistogram2D("Track Residuals X vs Momentum Bot Layer #" + (i+1),  nP, lowerLimP, upperLimP, 50, -10, 10));
        	Momentum_UnbiasedResidualY_top.put((i+1), histogramFactory.createHistogram2D("Track Residuals Y vs Momentum Top Layer #" + (i+1),  nP, lowerLimP, upperLimP, 50, -10, 10));
        	Momentum_UnbiasedResidualY_bot.put((i+1), histogramFactory.createHistogram2D("Track Residuals Y vs Momentum Bot Layer #" + (i+1),  nP, lowerLimP, upperLimP, 50, -10, 10));
        }
        
        for (IPlotter plotter : plotters.values()) { 
            //plotter.show();
        }
    }
    @Override
public void process(EventHeader event){
		aida.tree().cd("/");
		
        // If the event does not have tracks, skip it
        //if(!event.hasCollection(Track.class, trackCollectionName)) return;

        // Get the list of tracks from the event
        List<List<Track>> trackCollections = event.get(Track.class);
       
        // Get the list of final state particles from the event.  These will
        // be used to obtain the track momentum.
        //List<ReconstructedParticle> fsParticles = event.get(ReconstructedParticle.class, fsParticlesCollectionName);
      
        //this.mapReconstructedParticlesToTracks(tracks, fsParticles);
        
        //trackPlots.get("Number of tracks").fill(tracks.size());
        
        //  Get all of the stereo hits in the event
        List<TrackerHit> stereoHits = event.get(TrackerHit.class, stereoHitCollectionName);

        for (List<Track> tracks : trackCollections) {
        	
    		if(cleanTridents){
    			// Require an event to have exactly two tracks
    			if (tracks.size() != 2) continue;

    			// Require the two tracks to be in opposite volumes
    			if (tracks.get(0).getTrackStates().get(0).getTanLambda()*tracks.get(1).getTrackStates().get(0).getTanLambda() >= 0) continue;

    			// Require the two tracks to be oppositely charged
    			if (tracks.get(0).getTrackStates().get(0).getOmega()*tracks.get(1).getTrackStates().get(0).getOmega() >= 0) continue;
    		}
    			
        	for(Track track : tracks){
        		// Check that the track has the required number of hits.  The number of hits
        		// required to make a track is set in the tracking strategy.
        		if(track.getTrackerHits().size() != this.hitsOnTrack){
        			//System.out.println("This track doesn't have the required number of hits.");
        			continue;
        		}          
        		//HpsSiSensor trackSensor = (HpsSiSensor) ((RawTrackerHit)((HelicalTrackCross) track.getTrackerHits().get(0)).getStrips().get(0).rawhits().get(0)).getDetectorElement();
        			
        		HpsSiSensor trackSensor = (HpsSiSensor) ((RawTrackerHit)track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
        		//HpsSiSensor hitSensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
        		
        		// Calculate the momentum of the track
        		HelicalTrackFit hlc_trk_fit = TrackUtils.getHTF(track.getTrackStates().get(0));
        		
        		double B_field = Math.abs(TrackUtils.getBField(event.getDetector()).y());

                double p = hlc_trk_fit.p(B_field);
                
        		trackMomentumPlots.get("Track Momentum").fill(p);
        		if (trackSensor.isTopLayer()) {
        			trackMomentumPlots_top.fill(p);
        		} else { 
        			trackMomentumPlots_bot.fill(p);
        		}
        		// Find which of the layers isn't being used in the track fit
        		int unusedLayer = this.getUnusedSvtLayer(track.getTrackerHits());
            
        		trackPlots.get("Unused Layer").fill(unusedLayer);
        		trackMomentum.get(unusedLayer).fill(p);
        		if (trackSensor.isTopLayer()) {   			
        			trackMomentum_top.get(unusedLayer).fill(p);
        		} else{
        			trackMomentum_bot.get(unusedLayer).fill(p);
        		}
        		
        		if (trackSensor.isTopLayer()) {   
        			Hep3Vector extrapTrackPos = TrackUtils.extrapolateTrack(track,  zTop[unusedLayer-1]);                
           	 		double extrapTrackX = extrapTrackPos.x();
        			if(extrapTrackX < gapXtop[unusedLayer-1]){        				
           	 			if(trackSensor.isAxial()){
           	        		if(isWithinSensorAcceptance(track,unusedLayer,true)){
           	        			numberOfTopTracksElecAxial[unusedLayer-1]++;
           	        		}
           	 			}
           	 			else{
           	        		if(isWithinSensorAcceptance(track,unusedLayer,false)){
           	        			numberOfTopTracksElecStereo[unusedLayer-1]++;
           	        		}
           	 			}
           	 		}
           	 		else{
           	 			if(trackSensor.isAxial()){
           	        		if(isWithinSensorAcceptance(track,unusedLayer,true)){
           	        			numberOfTopTracksPosiAxial[unusedLayer-1]++;
           	        		}
           	 			}
           	 			else{
           	        		if(isWithinSensorAcceptance(track,unusedLayer,false)){
           	        			numberOfTopTracksPosiStereo[unusedLayer-1]++;
           	        		}
           	 			}
           	 		}
        		}
        		
        		if (trackSensor.isBottomLayer()) {   
        			Hep3Vector extrapTrackPos = TrackUtils.extrapolateTrack(track,  zBot[unusedLayer-1]);                
           	 		double extrapTrackX = extrapTrackPos.x();
        			if(extrapTrackX < gapXbot[unusedLayer-1]){        				
           	 			if(trackSensor.isAxial()){
           	        		if(isWithinSensorAcceptance(track,unusedLayer,true)){
           	        			numberOfBotTracksElecAxial[unusedLayer-1]++;
           	        		}
           	 			}
           	 			else{
           	        		if(isWithinSensorAcceptance(track,unusedLayer,false)){
           	        			numberOfBotTracksElecStereo[unusedLayer-1]++;
           	        		}
           	 			}
           	 		}
           	 		else{
           	 			if(trackSensor.isAxial()){
           	        		if(isWithinSensorAcceptance(track,unusedLayer,true)){
           	        			numberOfBotTracksPosiAxial[unusedLayer-1]++;
           	        		}
           	 			}
           	 			else{
           	        		if(isWithinSensorAcceptance(track,unusedLayer,false)){
           	        			numberOfBotTracksPosiStereo[unusedLayer-1]++;
           	        		}
           	 			}
           	 		}
        		}
        		
        		/*for (TrackerHit stereoHit : stereoHits) {
        			HpsSiSensor hitSensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
            	          
        			if ((trackSensor.isTopLayer() && hitSensor.isBottomLayer()) 
        					|| (trackSensor.isBottomLayer() && hitSensor.isTopLayer())) continue;
        			
        			// Retrieve the layer number by using the sensor
               	 	int layer = (hitSensor.getLayerNumber() + 1)/2;
               	 	
               	 	if (unusedLayer != layer) continue;              	 	
               	 	Hep3Vector stereoHitPosition = new BasicHep3Vector(stereoHit.getPosition());
               	 	Hep3Vector trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.z());
               	 	double xResidual = trackPosition.x() - stereoHitPosition.x();
               	 	double yResidual = trackPosition.y() - stereoHitPosition.y();                    
               	 	if (hitSensor.isTopLayer()) {
               	 		if (Math.abs(xResidual+topXResidualOffset[unusedLayer-1]) > this.nSigCut*topXResidualCut[unusedLayer-1] 
       	 					|| Math.abs(yResidual + topYResidualOffset[unusedLayer-1]) > this.nSigCut*topYResidualCut[unusedLayer-1]) continue;
               	 		//if (countTop > 0){
               	 			//continue;
               	 		//}
               	 		if(trackPosition.x() < gapXtop[unusedLayer-1]){ 
               	 			if(trackSensor.isAxial()){
               	 				if(isWithinSensorAcceptance(track,unusedLayer,true)){
               	 					List<TrackerHit> clusters = event.get(TrackerHit.class, clusterCollectionName);
               	 					for (TrackerHit cluster : clusters) {
               	 						for (Object rawHitObject : cluster.getRawHits()) {
               	 							numberOfTopTracksWithHitOnMissingLayerPosiStereo[unusedLayer-1]++;
               	 						}
               	 					}
               	 				}
               	            }
               	 		}
               	 	}
               	 		//countTop++;               	                	 		
               	 } */
        		
        		if(!isWithinAcceptance(track, unusedLayer)) continue;
        		
           	 	trackMomentum_accepted.get(unusedLayer).fill(p);
           	 	if (trackSensor.isTopLayer()) {           	 		
           	 		Hep3Vector extrapTrackPos = TrackUtils.extrapolateTrack(track,  zTop[unusedLayer-1]);                
           	 		double extrapTrackX = extrapTrackPos.x();
           	 		double extrapTrackY = extrapTrackPos.y();
           	 		TrackXTop.fill(extrapTrackX);
           	 		TrackYTop.fill(extrapTrackY);
           	 		trackMomentum_accepted_top.get(unusedLayer).fill(p);
           	 		numberOfTopTracks[unusedLayer-1]++;
           	 		if(track.getTrackStates().get(0).getOmega() > 0){
           	 			numberOfTopTracksElectron[unusedLayer-1]++;
           	 		}
           	 		else{
           	 			numberOfTopTracksPositron[unusedLayer-1]++;
           	 		}
           	 		for(int i = 0; i<nP;i++){
           	 			double mP = (upperLimP - lowerLimP)/((double) nP);
           	 			double lowerP = mP * i + lowerLimP;
           	 			double upperP = lowerP + mP;
           	 			if(lowerP < p && p < upperP){
           	 				numberOfTopTracksMomentum[i]++;
           	 				for(int j = 0; j<num_lay;j++){
           	 					if(unusedLayer == j + 1){
           	 						numberOfTopTracksMomentumLay.get(j+1)[i]++;
           	 						if(track.getTrackStates().get(0).getOmega() > 0){
           	 							numberOfTopTracksMomentumLayElec.get(j+1)[i]++;
           	 						}
           	 						else{
           	 							numberOfTopTracksMomentumLayPosi.get(j+1)[i]++;
           	 						}
           	 					}
           	 				}
           	 			}
           	 		}
           	 		for(int i = 0; i<nBinx; i++){
           	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
           	 			double lowerX = mX * i + lowerLimX;
           	 			double upperX = lowerX + mX;
           	 			if(lowerX < extrapTrackX && extrapTrackX < upperX){
           	 				for(int j = 0; j<nBiny; j++){
           	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
           	 					double lowerY = mY * j + lowerLimY;
           	 					double upperY = lowerY + mY;
           	 					if(lowerY < extrapTrackY && extrapTrackY < upperY){
           	 						numberOfTopTracksPos[unusedLayer-1][i][j]++;
           	 					}     
           	 				}
           	 			}          	 			
           	 		}
           	 		if(extrapTrackX < gapXtop[unusedLayer-1]){
           	 			numberOfTopTracksElec[unusedLayer-1]++;
           	 			if(trackSensor.isAxial()){
           	 				numberOfTopTracksElecAxial[unusedLayer-1]++;
           	 			}
           	 			else{
           	 				numberOfTopTracksElecStereo[unusedLayer-1]++;
           	 			}
           	 		}
           	 		else{
           	 			numberOfTopTracksPosi[unusedLayer-1]++;
           	 			if(trackSensor.isAxial()){
           	 				numberOfTopTracksPosiAxial[unusedLayer-1]++;
           	 			}
           	 			else{
           	 				numberOfTopTracksPosiStereo[unusedLayer-1]++;
           	 			}
           	 		}
           	 	} else{
           	 		Hep3Vector extrapTrackPos = TrackUtils.extrapolateTrack(track,  zBot[unusedLayer-1]);                
           	 		double extrapTrackX = extrapTrackPos.x();
           	 		double extrapTrackY = extrapTrackPos.y();
           	 		TrackXBot.fill(extrapTrackX);
           	 		TrackYBot.fill(extrapTrackY);
           	 		trackMomentum_accepted_bot.get(unusedLayer).fill(p);
           	 		numberOfBotTracks[unusedLayer-1]++;
           	 		if(track.getTrackStates().get(0).getOmega() > 0){
           	 			numberOfBotTracksElectron[unusedLayer-1]++;
           	 		}
           	 		else{
           	 			numberOfBotTracksPositron[unusedLayer-1]++;
           	 		}
           	 		for(int i = 0; i<nP;i++){
           	 			double mP = (upperLimP - lowerLimP)/((double) nP);
           	 			double lowerP = mP * i + lowerLimP;
           	 			double upperP = lowerP + mP;
           	 			if(lowerP < p && p < upperP){
           	 				numberOfBotTracksMomentum[i]++;
           	 				for(int j = 0; j<num_lay;j++){
           	 					if(unusedLayer == j + 1){
           	 						numberOfBotTracksMomentumLay.get(j+1)[i]++;
           	 						if(track.getTrackStates().get(0).getOmega() > 0){
           	 							numberOfBotTracksMomentumLayElec.get(j+1)[i]++;
           	 						}
           	 						else{
           	 							numberOfBotTracksMomentumLayPosi.get(j+1)[i]++;
           	 						}
           	 					}
           	 				}
           	 			}
           	 		}
           	 		for(int i = 0; i<nBinx; i++){
           	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
           	 			double lowerX = mX * i + lowerLimX;
           	 			double upperX = lowerX + mX;
           	 			if(lowerX < extrapTrackX && extrapTrackX < upperX){
           	 				for(int j = 0; j<nBiny; j++){
           	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
           	 					double lowerY = mY * j + lowerLimY;
           	 					double upperY = lowerY + mY;
           	 					if(lowerY < extrapTrackY && extrapTrackY < upperY){
           	 						numberOfBotTracksPos[unusedLayer-1][i][j]++;
           	 					}          	 			
           	 				}
           	 			}
           	 		}
           	 		if(extrapTrackX < gapXtop[unusedLayer-1]){
           	 			numberOfBotTracksElec[unusedLayer-1]++;
           	 			if(trackSensor.isAxial()){
           	 				numberOfBotTracksElecAxial[unusedLayer-1]++;
           	 			}
           	 			else{
           	 				numberOfBotTracksElecStereo[unusedLayer-1]++;
           	 			}
           	 		}
           	 		else{
           	 			numberOfBotTracksPosi[unusedLayer-1]++;
           	 			if(trackSensor.isAxial()){
           	 				numberOfBotTracksPosiAxial[unusedLayer-1]++;
           	 			}
           	 			else{
       	 					numberOfBotTracksPosiStereo[unusedLayer-1]++;
           	 			}
           	 		}
           	 	}
        		int countTop = 0;
        		int countBot = 0;
        		for (TrackerHit stereoHit : stereoHits) {
        			HpsSiSensor hitSensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
            	          
        			if ((trackSensor.isTopLayer() && hitSensor.isBottomLayer()) 
        					|| (trackSensor.isBottomLayer() && hitSensor.isTopLayer())) continue;
        			
        			// Retrieve the layer number by using the sensor
               	 	int layer = (hitSensor.getLayerNumber() + 1)/2;
               	 	
               	 	if (unusedLayer != layer) continue;              	 	
                        
               	 	Hep3Vector stereoHitPosition = new BasicHep3Vector(stereoHit.getPosition());
               	 	Hep3Vector trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.z());
               	 	double xResidual = trackPosition.x() - stereoHitPosition.x();
               	 	double yResidual = trackPosition.y() - stereoHitPosition.y();
             
               	 	trackMomentum_final.get(unusedLayer).fill(p);                        
               	 	if (hitSensor.isTopLayer()) {
               	 		if (countTop > 0){
               	 			//System.out.println("Top " + unusedLayer);
               	 			continue;
               	 		}
               	 		UnbiasedResidualX_top.get(unusedLayer).fill(xResidual);
               	 		UnbiasedResidualY_top.get(unusedLayer).fill(yResidual);
               	 		HitPosition_top.get(unusedLayer).fill(stereoHitPosition.z());
               	 		trackPlots.get("Unbiased Residual x - Top").fill(xResidual);
               	 		trackPlots.get("Unbiased Residual y - Top").fill(yResidual);
               	 		TrackResidualXvsMomentumTop.fill(p,xResidual);
               	 		TrackResidualYvsMomentumTop.fill(p,yResidual);
               	 		TrackResidualXvsTrackResdidualYTop.fill(xResidual,yResidual);
               	 		Momentum_UnbiasedResidualX_top.get(unusedLayer).fill(p,xResidual);
               	 		Momentum_UnbiasedResidualY_top.get(unusedLayer).fill(p,yResidual);
               	 		if (Math.abs(xResidual+topXResidualOffset[unusedLayer-1]) > this.nSigCut*topXResidualCut[unusedLayer-1] 
       	 					|| Math.abs(yResidual + topYResidualOffset[unusedLayer-1]) > this.nSigCut*topYResidualCut[unusedLayer-1]) continue;
               	 		//trackPlots.get("Unbiased Residual x - Top").fill(xResidual);
               	 		//trackPlots.get("Unbiased Residual y - Top").fill(yResidual);
               	 		//TrackResidualXvsMomentumTop.fill(p,xResidual);
               	 		//TrackResidualYvsMomentumTop.fill(p,yResidual);
               	 		//TrackResidualXvsTrackResdidualYTop.fill(xResidual,yResidual);
               	 		trackMomentumPlots.get("Track Momentum - All Layers Hit").fill(p);
               	 		trackMomentum_final_top.get(unusedLayer).fill(p);
               	 		numberOfTopTracksWithHitOnMissingLayer[unusedLayer-1]++;
               	 		if(track.getTrackStates().get(0).getOmega() > 0){
               	 			numberOfTopTracksWithHitOnMissingLayerElectron[unusedLayer-1]++;
               	 		}
               	 		else{
               	 			numberOfTopTracksWithHitOnMissingLayerPositron[unusedLayer-1]++;
               	 		}
               	 		for(int i = 0; i<nP;i++){
               	 			double mP = (upperLimP - lowerLimP)/((double) nP);
               	 			double lowerP = mP * i + lowerLimP;
               	 			double upperP = lowerP + mP;
               	 			if(lowerP < p && p < upperP){
               	 				numberOfTopTracksWithHitOnMissingLayerMomentum[i]++;
               	 				for(int j = 0; j<num_lay;j++){
               	 					if(unusedLayer == j + 1){
               	 						numberOfTopTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i]++;
               	 						if(track.getTrackStates().get(0).getOmega() > 0){
               	 							numberOfTopTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i]++;
               	 						}
               	 						else{
               	 							numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i]++;
               	 						}
               	 					}
               	 				}
               	 			}
               	 		}
               	 		for(int i = 0; i<nBinx; i++){
               	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
               	 			double lowerX = mX * i + lowerLimX;
               	 			double upperX = lowerX + mX;
               	 			if(lowerX < trackPosition.x() && trackPosition.x() < upperX){
               	 				for(int j = 0; j<nBiny; j++){
               	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
               	 					double lowerY = mY * j + lowerLimY;
               	 					double upperY = lowerY + mY;
               	 					if(lowerY < trackPosition.y() && trackPosition.y() < upperY){
               	 						numberOfTopTracksWithHitOnMissingLayerPos[unusedLayer-1][i][j]++;
               	 					}     
               	 				}
               	 			}             	 			
               	 		}
               	 		if(trackPosition.x() < gapXtop[unusedLayer-1]){
               	 			numberOfTopTracksWithHitOnMissingLayerElec[unusedLayer-1]++;
               	 		}
               	 		else{
               	 			numberOfTopTracksWithHitOnMissingLayerPosi[unusedLayer-1]++;	
               	 		}
               	 		countTop++;               	                	 		
               	 	} else {
               	 		if (countBot > 0){
               	 			continue;
               	 		}
               	 		UnbiasedResidualX_bot.get(unusedLayer).fill(xResidual);
               	 		UnbiasedResidualY_bot.get(unusedLayer).fill(yResidual);
               	 		HitPosition_bot.get(unusedLayer).fill(stereoHitPosition.z());
               	 		trackPlots.get("Unbiased Residual x - Bottom").fill(xResidual);
               	 		trackPlots.get("Unbiased Residual y - Bottom").fill(yResidual);
               	 		TrackResidualXvsMomentumBot.fill(p,xResidual);
               	 		TrackResidualYvsMomentumBot.fill(p,yResidual);
               	 		TrackResidualXvsTrackResdidualYBot.fill(xResidual,yResidual);
               	 		Momentum_UnbiasedResidualX_bot.get(unusedLayer).fill(p,xResidual);
               	 		Momentum_UnbiasedResidualY_bot.get(unusedLayer).fill(p,yResidual);
               	 		if (Math.abs(xResidual+botXResidualOffset[unusedLayer-1]) > this.nSigCut*botXResidualCut[unusedLayer-1] 
       	 					|| Math.abs(yResidual + botYResidualOffset[unusedLayer-1]) > this.nSigCut*botYResidualCut[unusedLayer-1]) continue;
               	 		//trackPlots.get("Unbiased Residual x - Bottom").fill(xResidual);
               	 		//trackPlots.get("Unbiased Residual y - Bottom").fill(yResidual);
               	 		//TrackResidualXvsMomentumBot.fill(p,xResidual);
               	 		//TrackResidualYvsMomentumBot.fill(p,yResidual);
               	 		//TrackResidualXvsTrackResdidualYBot.fill(xResidual,yResidual);
               	 		trackMomentumPlots.get("Track Momentum - All Layers Hit").fill(p);
               	 		trackMomentum_final_bot.get(unusedLayer).fill(p);
               	 		numberOfBotTracksWithHitOnMissingLayer[unusedLayer-1]++;
               	 		if(track.getTrackStates().get(0).getOmega() > 0){
               	 			numberOfBotTracksWithHitOnMissingLayerElectron[unusedLayer-1]++;
               	 		}
               	 		else{
               	 			numberOfBotTracksWithHitOnMissingLayerPositron[unusedLayer-1]++;
               	 		}
               	 		for(int i = 0; i<nP;i++){
               	 			double mP = (upperLimP - lowerLimP)/((double) nP);
               	 			double lowerP = mP * i + lowerLimP;
               	 			double upperP = lowerP + mP;
               	 			if(lowerP < p && p < upperP){
               	 				numberOfBotTracksWithHitOnMissingLayerMomentum[i]++;
               	 				for(int j = 0; j<num_lay;j++){
               	 					if(unusedLayer == j + 1){
               	 						numberOfBotTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i]++;
               	 						if(track.getTrackStates().get(0).getOmega() > 0){
               	 							numberOfBotTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i]++;
               	 						}
               	 						else{
               	 							numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i]++;
               	 						}
               	 					}
               	 				}
               	 			}
               	 		}
               	 		for(int i = 0; i<nBinx; i++){
               	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
               	 			double lowerX = mX * i + lowerLimX;
               	 			double upperX = lowerX + mX;
               	 			if(lowerX < trackPosition.x() && trackPosition.x() < upperX){
               	 				for(int j = 0; j<nBiny; j++){
               	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
               	 					double lowerY = mY * j + lowerLimY;
               	 					double upperY = lowerY + mY;
               	 					if(lowerY < trackPosition.y() && trackPosition.y() < upperY){
               	 						numberOfBotTracksWithHitOnMissingLayerPos[unusedLayer-1][i][j]++;
               	 					}     
               	 				}
               	 			}             	 			
               	 		}
               	 		if(trackPosition.x() < gapXtop[unusedLayer-1]){
               	 			numberOfBotTracksWithHitOnMissingLayerElec[unusedLayer-1]++;
               	 		}
               	 		else{
               	 			numberOfBotTracksWithHitOnMissingLayerPosi[unusedLayer-1]++;	
               	 		}
               	 		countBot++;            	 	
        			}
        		}          
        	}
        }
    }



/**
 *  Find which of the layers is not being used in the track fit
 *
 *  @param hits : List of stereo hits associated with a track
 *  @return Layer not used in the track fit
 */
private int getUnusedSvtLayer(List<TrackerHit> stereoHits) {
    
    int[] svtLayer = new int[6];
    
    // Loop over all of the stereo hits associated with the track
    for (TrackerHit stereoHit : stereoHits) {
        
        // Retrieve the sensor associated with one of the hits.  This will
        // be used to retrieve the layer number
        HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
        
        // Retrieve the layer number by using the sensor
        int layer = (sensor.getLayerNumber() + 1)/2;
       
        // If a hit is associated with that layer, increment its 
        // corresponding counter
        svtLayer[layer - 1]++;
    }
    
    // Loop through the layer counters and find which layer has not been
    // incremented i.e. is unused by the track
    for(int layer = 0; layer < svtLayer.length; layer++){
        if(svtLayer[layer] == 0) { 
            //System.out.println("Layer number " + (layer+1) + " is not used");
            return (layer + 1);
        }
    }
  
    // If all of the layers are being used, this track can't be used to 
    // in the single hit efficiency calculation.  This means that something
    // is wrong with the file
    // TODO: This should probably throw an exception
    return -1;
}

private boolean isWithinSensorAcceptance(Track track, int layer,boolean axial) {
	   
	//Axial is true if the sensor is axial
    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit)track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
    
    // Get the sensors associated with the layer that the track
    // will be extrapolated to
    List<SvtStereoLayer> stereoLayers = null;
    
    if (sensor.isTopLayer()) {
        stereoLayers = this.topStereoLayers.get(layer);
    } else {
        stereoLayers = this.bottomStereoLayers.get(layer);
    }
    
    for (SvtStereoLayer stereoLayer : stereoLayers) { 
        Hep3Vector axialSensorPosition = stereoLayer.getAxialSensor().getGeometry().getPosition();
        Hep3Vector stereoSensorPosition = stereoLayer.getStereoSensor().getGeometry().getPosition();
        
        Hep3Vector axialTrackPos = TrackUtils.extrapolateTrack(track,  axialSensorPosition.z());
        Hep3Vector stereoTrackPos = TrackUtils.extrapolateTrack(track, stereoSensorPosition.z());
        
        if(this.sensorContainsTrack(axialTrackPos, stereoLayer.getAxialSensor()) && axial){
        	return true;
        }
       		
        if(this.sensorContainsTrack(stereoTrackPos, stereoLayer.getStereoSensor()) && !axial){
            return true;
        }
    }
    
    return false;
    
    /*int layerNumber = (layer - 1)/2 + 1;
    String title = "Track Position - Layer " + layerNumber + " - Tracks Within Acceptance";
    //aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
    //aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z()); */
    
}

/**
 * Extrapolate a track to a layer and check that it lies within its 
 * acceptance.
 *  
 * @param track The track that will be extrapolated to the layer of interest
 * @param layer The layer number to extrapolate to
 * @return true if the track lies within the sensor acceptance, false otherwise
 */
private boolean isWithinAcceptance(Track track, int layer) {
   
    // TODO: Move this to a utility class 
   
    //System.out.println("Retrieving sensors for layer: " + layer);
    
    // Since TrackUtils.isTop/BottomTrack does not work when running off 
    // a recon file, get the detector volume that a track is associated 
    // with by using the sensor.  This assumes that a track is always
    // composed by stereo hits that lie within a single detector volume
    HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit)track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
    
    // Get the sensors associated with the layer that the track
    // will be extrapolated to
    List<SvtStereoLayer> stereoLayers = null;
    
    // if (TrackUtils.isTopTrack(track, track.getTrackerHits().size())) {
    if (sensor.isTopLayer()) {
        //System.out.println("Top track found.");
        stereoLayers = this.topStereoLayers.get(layer);
    //} else if (TrackUtils.isBottomTrack(track, track.getTrackerHits().size())) {
    } else {
        //System.out.println("Bottom track found.");
        stereoLayers = this.bottomStereoLayers.get(layer);
    }
    
    for (SvtStereoLayer stereoLayer : stereoLayers) { 
        Hep3Vector axialSensorPosition = stereoLayer.getAxialSensor().getGeometry().getPosition();
        Hep3Vector stereoSensorPosition = stereoLayer.getStereoSensor().getGeometry().getPosition();
   
        //System.out.println("Axial sensor position: " + axialSensorPosition.toString());
        //System.out.println("Stereo sensor position: " + stereoSensorPosition.toString());
        
        Hep3Vector axialTrackPos = TrackUtils.extrapolateTrack(track,  axialSensorPosition.z());
        Hep3Vector stereoTrackPos = TrackUtils.extrapolateTrack(track, stereoSensorPosition.z());
  
        //System.out.println("Track position at axial sensor: " + axialTrackPos.toString());
        //System.out.println("Track position at stereo sensor: " + stereoTrackPos.toString());
        
        if(this.sensorContainsTrack(axialTrackPos, stereoLayer.getAxialSensor()) 
                && this.sensorContainsTrack(stereoTrackPos, stereoLayer.getStereoSensor())){
            //System.out.println("Track lies within layer acceptance.");
            return true;
        }
    }
    
    return false;
    
    /*int layerNumber = (layer - 1)/2 + 1;
    String title = "Track Position - Layer " + layerNumber + " - Tracks Within Acceptance";
    //aida.histogram2D(title).fill(trackPos.y(), trackPos.z());
    //aida.cloud2D(title).fill(frontTrackPos.y(), frontTrackPos.z()); */
    
}

/**
 * 
 */
public int findIntersectingChannel(Hep3Vector trackPosition, SiSensor sensor){
    
    //--- Check that the track doesn't pass through a region of bad channels ---//
    //--------------------------------------------------------------------------//

    //Rotate the track position to the JLab coordinate system
    //this.printDebug("Track position in tracking frame: " + trackPosition.toString());
    Hep3Vector trackPositionDet = VecOp.mult(VecOp.inverse(this.trackerHitUtils.detToTrackRotationMatrix()), trackPosition);
    //this.printDebug("Track position in JLab frame " + trackPositionDet.toString());
    // Rotate the track to the sensor coordinates
    ITransform3D globalToLocal = sensor.getReadoutElectrodes(ChargeCarrier.HOLE).getGlobalToLocal();
    globalToLocal.transform(trackPositionDet);
    //this.printDebug("Track position in sensor electrodes frame " + trackPositionDet.toString());

    // Find the closest channel to the track position
    double deltaY = Double.MAX_VALUE;
    int intersectingChannel = 0;
    for(int physicalChannel= 0; physicalChannel < 639; physicalChannel++){ 
        /*this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel 
                + " : Strip Position " + stripPositions.get(sensor).get(physicalChannel));
        this.printDebug(SvtUtils.getInstance().getDescription(sensor) + ": Channel " + physicalChannel
                + " : Delta Y: " + Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()));*/
        /*if(Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()) < deltaY ){
            deltaY = Math.abs(trackPositionDet.x() - stripPositions.get(sensor).get(physicalChannel).x()); 
            intersectingChannel = physicalChannel;
        }*/
    }

    
    return intersectingChannel;
}

/**
 *
 */
public boolean sensorContainsTrack(Hep3Vector trackPosition, HpsSiSensor sensor){

    
    if(maskBadChannels){
        int intersectingChannel = this.findIntersectingChannel(trackPosition, sensor);
        if(intersectingChannel == 0 || intersectingChannel == 638) return false;
        
        if(sensor.isBadChannel(intersectingChannel) 
                || sensor.isBadChannel(intersectingChannel+1) 
                || sensor.isBadChannel(intersectingChannel-1)){
            //this.printDebug("Track intersects a bad channel!");
            return false;
        }
    }
    
    ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();
    
    Hep3Vector sensorPos = sensor.getGeometry().getPosition();   
    Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
    Polygon3D sensorFace = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
    
    List<Point3D> vertices = new ArrayList<Point3D>();
    for(int index = 0; index < 4; index++){
        vertices.add(new Point3D());
    }
    for(Point3D vertex : sensorFace.getVertices()){
        if(vertex.y() < 0 && vertex.x() > 0){
            localToGlobal.transform(vertex);
            //vertices.set(0, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
            vertices.set(0, new Point3D(vertex.x(), vertex.y(), vertex.z()));
           //System.out.println(this.getClass().getSimpleName() + ": Vertex 1 Position: " + vertices.get(0).toString());
           //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 1 Position: " + localToGlobal.transformed(vertex).toString());
        } 
        else if(vertex.y() > 0 && vertex.x() > 0){
            localToGlobal.transform(vertex);
            //vertices.set(1, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
            vertices.set(1, new Point3D(vertex.x(), vertex.y(), vertex.z()));
            //System.out.println(this.getClass().getSimpleName() + ": Vertex 2 Position: " + vertices.get(1).toString());
            //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 2 Position: " + localToGlobal.transformed(vertex).toString());
        } 
        else if(vertex.y() > 0 && vertex.x() < 0){
            localToGlobal.transform(vertex);
            //vertices.set(2, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
            vertices.set(2, new Point3D(vertex.x(), vertex.y(), vertex.z()));
            //System.out.println(this.getClass().getSimpleName() + ": Vertex 3 Position: " + vertices.get(2).toString());
            //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 3 Position: " + localToGlobal.transformed(vertex).toString());
        }             
        else if(vertex.y() < 0 && vertex.x() < 0){
            localToGlobal.transform(vertex);
            //vertices.set(3, new Point3D(vertex.y() + sensorPos.x(), vertex.x() + sensorPos.y(), vertex.z() + sensorPos.z()));
            vertices.set(3, new Point3D(vertex.x(), vertex.y(), vertex.z()));
            //System.out.println(this.getClass().getSimpleName() + ": Vertex 4 Position: " + vertices.get(3).toString());
            //System.out.println(this.getClass().getSimpleName() + ": Transformed Vertex 4 Position: " + localToGlobal.transformed(vertex).toString());
        } 
    }

    /*
    double area1 = this.findTriangleArea(vertices.get(0).x(), vertices.get(0).y(), vertices.get(1).x(), vertices.get(1).y(), trackPosition.y(), trackPosition.z()); 
    double area2 = this.findTriangleArea(vertices.get(1).x(), vertices.get(1).y(), vertices.get(2).x(), vertices.get(2).y(), trackPosition.y(), trackPosition.z()); 
    double area3 = this.findTriangleArea(vertices.get(2).x(), vertices.get(2).y(), vertices.get(3).x(), vertices.get(3).y(), trackPosition.y(), trackPosition.z()); 
    double area4 = this.findTriangleArea(vertices.get(3).x(), vertices.get(3).y(), vertices.get(0).x(), vertices.get(0).y(), trackPosition.y(), trackPosition.z()); 
    */
    
    double area1 = this.findTriangleArea(vertices.get(0).x(), vertices.get(0).y(), vertices.get(1).x(), vertices.get(1).y(), trackPosition.x(), trackPosition.y()); 
    double area2 = this.findTriangleArea(vertices.get(1).x(), vertices.get(1).y(), vertices.get(2).x(), vertices.get(2).y(), trackPosition.x(), trackPosition.y()); 
    double area3 = this.findTriangleArea(vertices.get(2).x(), vertices.get(2).y(), vertices.get(3).x(), vertices.get(3).y(), trackPosition.x(), trackPosition.y()); 
    double area4 = this.findTriangleArea(vertices.get(3).x(), vertices.get(3).y(), vertices.get(0).x(), vertices.get(0).y(), trackPosition.x(), trackPosition.y()); 

    if((area1 > 0 && area2 > 0 && area3 > 0 && area4 > 0) || (area1 < 0 && area2 < 0 && area3 < 0 && area4 < 0)) return true;
    return false;
} 

/**
*
*/
public double findTriangleArea(double x0, double y0, double x1, double y1, double x2, double y2){
  return .5*(x1*y2 - y1*x2 -x0*y2 + y0*x2 + x0*y1 - y0*x1); 
}

@Override
public void endOfData(){
    
    for(int i = 0; i<num_lay; i++){
    	hitEfficiencyTop[i] = numberOfTopTracksWithHitOnMissingLayer[i]/(double) numberOfTopTracks[i];
    	hitEfficiencyBot[i] = numberOfBotTracksWithHitOnMissingLayer[i]/(double) numberOfBotTracks[i];
    	hitEfficiencyTopElectron[i] = numberOfTopTracksWithHitOnMissingLayerElectron[i]/(double) numberOfTopTracksElectron[i];
    	hitEfficiencyBotElectron[i] = numberOfBotTracksWithHitOnMissingLayerElectron[i]/(double) numberOfBotTracksElectron[i];
    	hitEfficiencyTopPositron[i] = numberOfTopTracksWithHitOnMissingLayerPositron[i]/(double) numberOfTopTracksPositron[i];
    	hitEfficiencyBotPositron[i] = numberOfBotTracksWithHitOnMissingLayerPositron[i]/(double) numberOfBotTracksPositron[i];
    	numberOfTopTracksTot = numberOfTopTracksTot + numberOfTopTracks[i];
    	numberOfBotTracksTot = numberOfBotTracksTot + numberOfBotTracks[i];
    	numberOfTopTracksWithHitOnMissingLayerTot = numberOfTopTracksWithHitOnMissingLayerTot + numberOfTopTracksWithHitOnMissingLayer[i];
    	numberOfBotTracksWithHitOnMissingLayerTot = numberOfBotTracksWithHitOnMissingLayerTot + numberOfBotTracksWithHitOnMissingLayer[i];
    	errorTop[i] = Math.sqrt(1/(double) numberOfTopTracks[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayer[i]);
    	errorBot[i] = Math.sqrt(1/(double) numberOfBotTracks[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayer[i]);   	
    	errorTopElectron[i] = Math.sqrt(1/(double) numberOfTopTracksElectron[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerElectron[i]);
    	errorBotElectron[i] = Math.sqrt(1/(double) numberOfBotTracksElectron[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerElectron[i]);
    	errorTopPositron[i] = Math.sqrt(1/(double) numberOfTopTracksPositron[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerPositron[i]);
    	errorBotPositron[i] = Math.sqrt(1/(double) numberOfBotTracksPositron[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerPositron[i]); 
    	HitEfficiency_top.fill(i,hitEfficiencyTop[i]);
    	HitEfficiency_bot.fill(i,hitEfficiencyBot[i]);
    	HitEfficiencyError_top.fill(i,errorTop[i]);
    	HitEfficiencyError_bot.fill(i,errorBot[i]);
    	HitEfficiency_topElec.fill(i,hitEfficiencyTopElectron[i]);
    	HitEfficiency_botElec.fill(i,hitEfficiencyBotElectron[i]);
    	HitEfficiencyError_topElec.fill(i,errorTopElectron[i]);
    	HitEfficiencyError_botElec.fill(i,errorBotElectron[i]);
    	HitEfficiency_topPosi.fill(i,hitEfficiencyTopPositron[i]);
    	HitEfficiency_botPosi.fill(i,hitEfficiencyBotPositron[i]);
    	HitEfficiencyError_topPosi.fill(i,errorTopPositron[i]);
    	HitEfficiencyError_botPosi.fill(i,errorBotPositron[i]);
    	hitEfficiencyTopElecStereo[i] = numberOfTopTracksWithHitOnMissingLayerElecStereo[i]/(double) numberOfTopTracksElecStereo[i];
    	hitEfficiencyTopElecAxial[i] = numberOfTopTracksWithHitOnMissingLayerElecAxial[i]/(double) numberOfTopTracksElecAxial[i];
		hitEfficiencyTopPosiStereo[i] = numberOfTopTracksWithHitOnMissingLayerPosiStereo[i]/(double) numberOfTopTracksPosiStereo[i];
		hitEfficiencyTopPosiAxial[i] = numberOfTopTracksWithHitOnMissingLayerPosiAxial[i]/(double) numberOfTopTracksPosiAxial[i];
    	hitEfficiencyBotElecStereo[i] = numberOfBotTracksWithHitOnMissingLayerElecStereo[i]/(double) numberOfBotTracksElecStereo[i];
    	hitEfficiencyBotElecAxial[i] = numberOfBotTracksWithHitOnMissingLayerElecAxial[i]/(double) numberOfBotTracksElecAxial[i];
    	hitEfficiencyBotPosiStereo[i] = numberOfBotTracksWithHitOnMissingLayerPosiStereo[i]/(double) numberOfBotTracksPosiStereo[i];
    	hitEfficiencyBotPosiAxial[i] = numberOfBotTracksWithHitOnMissingLayerPosiAxial[i]/(double) numberOfBotTracksPosiAxial[i];
    	errorTopElecStereo[i] = Math.sqrt(1/(double) numberOfTopTracksElecStereo[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerElecStereo[i]);
    	errorTopElecAxial[i] = Math.sqrt(1/(double) numberOfTopTracksElecAxial[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerElecAxial[i]);
    	errorTopPosiStereo[i] = Math.sqrt(1/(double) numberOfTopTracksPosiStereo[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerPosiStereo[i]);
    	errorTopPosiAxial[i] = Math.sqrt(1/(double) numberOfTopTracksPosiAxial[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerPosiAxial[i]);
    	errorBotElecStereo[i] = Math.sqrt(1/(double) numberOfBotTracksElecStereo[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerElecStereo[i]);
    	errorBotElecAxial[i] = Math.sqrt(1/(double) numberOfBotTracksElecAxial[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerElecAxial[i]);
    	errorBotPosiStereo[i] = Math.sqrt(1/(double) numberOfBotTracksPosiStereo[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerPosiStereo[i]);
    	errorBotPosiAxial[i] = Math.sqrt(1/(double) numberOfBotTracksPosiAxial[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerPosiAxial[i]);
    	if(i+1>num_lay/2){
    		hitEfficiencyTopElec[i] = numberOfTopTracksWithHitOnMissingLayerElec[i]/(double) numberOfTopTracksElec[i];
    		hitEfficiencyTopPosi[i] = numberOfTopTracksWithHitOnMissingLayerPosi[i]/(double) numberOfTopTracksPosi[i];
        	hitEfficiencyBotElec[i] = numberOfBotTracksWithHitOnMissingLayerElec[i]/(double) numberOfBotTracksElec[i];
        	hitEfficiencyBotPosi[i] = numberOfBotTracksWithHitOnMissingLayerPosi[i]/(double) numberOfBotTracksPosi[i];
        	errorTopElec[i] = Math.sqrt(1/(double) numberOfTopTracksElec[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerElec[i]);
        	errorTopPosi[i] = Math.sqrt(1/(double) numberOfTopTracksPosi[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerPosi[i]);
        	errorBotElec[i] = Math.sqrt(1/(double) numberOfBotTracksElec[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerElec[i]);
        	errorBotPosi[i] = Math.sqrt(1/(double) numberOfBotTracksPosi[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerPosi[i]);
        	HitEfficiencyElec_top.fill(i,hitEfficiencyTopElec[i]);
        	HitEfficiencyPosi_top.fill(i,hitEfficiencyTopPosi[i]);
        	HitEfficiencyElec_bot.fill(i,hitEfficiencyBotElec[i]);
        	HitEfficiencyPosi_bot.fill(i,hitEfficiencyBotPosi[i]);
        	HitEfficiencyErrorElec_top.fill(i,errorTopElec[i]);
        	HitEfficiencyErrorPosi_top.fill(i,errorTopPosi[i]);
        	HitEfficiencyErrorElec_bot.fill(i,errorBotElec[i]);
        	HitEfficiencyErrorPosi_bot.fill(i,errorBotPosi[i]);
    	}
    	double mX = (upperLimX - lowerLimX)/((double) nBinx);
    	double mY = (upperLimY - lowerLimY)/((double) nBiny);
    	for(int j = 0; j<nBinx; j++){
    		double x = mX * (j+0.5) + lowerLimX;
    		for(int k = 0; k<nBiny; k++){
    			double y = mY * (k+0.5) + lowerLimY;
    			if(numberOfTopTracksWithHitOnMissingLayerPos[i][j][k] != 0){
    				hitEfficiencyTopPos[i][j][k] = numberOfTopTracksWithHitOnMissingLayerPos[i][j][k]/(double) numberOfTopTracksPos[i][j][k];
    				hitEfficiencyErrorTopPos[i][j][k] = Math.sqrt(1/(double) numberOfTopTracksPos[i][j][k]+1/(double) numberOfTopTracksWithHitOnMissingLayerPos[i][j][k]);
    			}
    			else{
    				hitEfficiencyTopPos[i][j][k] = 0;
    				hitEfficiencyErrorTopPos[i][j][k] = 0;
    			}
    			if(numberOfBotTracksWithHitOnMissingLayerPos[i][j][k] != 0){
    				hitEfficiencyBotPos[i][j][k] = numberOfBotTracksWithHitOnMissingLayerPos[i][j][k]/(double) numberOfBotTracksPos[i][j][k];
    				hitEfficiencyErrorBotPos[i][j][k] = Math.sqrt(1/(double) numberOfBotTracksPos[i][j][k]+1/(double) numberOfBotTracksWithHitOnMissingLayerPos[i][j][k]);
    			}
    			else{
    				hitEfficiencyBotPos[i][j][k] = 0;
    				hitEfficiencyErrorBotPos[i][j][k] = 0;
    			}
    			if(hitEfficiencyTopPos[i][j][k]>1){
    				hitEfficiencyTopPos[i][j][k] = 1.0;
    			}
    			if(hitEfficiencyBotPos[i][j][k]>1){
    				hitEfficiencyBotPos[i][j][k] = 1.0;
    			}
    			HitEfficiencyPos_top.get(i+1).fill(x,y,hitEfficiencyTopPos[i][j][k]);
    			HitEfficiencyPos_bot.get(i+1).fill(x,y,hitEfficiencyBotPos[i][j][k]);
    			HitEfficiencyErrorPos_top.get(i+1).fill(x,y,hitEfficiencyErrorTopPos[i][j][k]);
    			HitEfficiencyErrorPos_bot.get(i+1).fill(x,y,hitEfficiencyErrorBotPos[i][j][k]);
    		}
    	}
    }
    double mP = (upperLimP - lowerLimP)/((double) nP);
    
    for(int i = 0; i<nP; i++){
    	hitEfficiencyMomentumTop[i] = numberOfTopTracksWithHitOnMissingLayerMomentum[i]/(double) numberOfTopTracksMomentum[i];
    	hitEfficiencyMomentumBot[i] = numberOfBotTracksWithHitOnMissingLayerMomentum[i]/(double) numberOfBotTracksMomentum[i];
    	if(numberOfTopTracksWithHitOnMissingLayerMomentum[i] != 0 && numberOfTopTracksMomentum[i] != 0){
    		hitEfficiencyErrorMomentumTop[i] = Math.sqrt(1/(double) numberOfTopTracksMomentum[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerMomentum[i]);
    	}
    	else{
    		hitEfficiencyErrorMomentumTop[i] = 0;
    	}
    	if(numberOfBotTracksWithHitOnMissingLayerMomentum[i] != 0 && numberOfBotTracksMomentum[i] != 0){
    		hitEfficiencyErrorMomentumBot[i] = Math.sqrt(1/(double) numberOfBotTracksMomentum[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerMomentum[i]);
    	}
    	else{
    		hitEfficiencyErrorMomentumBot[i] = 0;
    	}
    	double p = mP * (i+0.5) + lowerLimP;
    	HitEfficiency_Momentum_top.fill(p,hitEfficiencyMomentumTop[i]);
    	HitEfficiency_Momentum_bot.fill(p,hitEfficiencyMomentumBot[i]);
    	HitEfficiencyError_Momentum_top.fill(p,hitEfficiencyErrorMomentumTop[i]);
    	HitEfficiencyError_Momentum_bot.fill(p,hitEfficiencyErrorMomentumBot[i]);
    	for(int j = 0; j<num_lay; j++){
    		hitEfficiencyMomentumLayTop.get(j+1)[i] = numberOfTopTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i]/(double) numberOfTopTracksMomentumLay.get(j+1)[i];
    		hitEfficiencyMomentumLayBot.get(j+1)[i] = numberOfBotTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i]/(double) numberOfBotTracksMomentumLay.get(j+1)[i];
    		hitEfficiencyMomentumLayTopElec.get(j+1)[i] = numberOfTopTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i]/(double) numberOfTopTracksMomentumLayElec.get(j+1)[i];
    		hitEfficiencyMomentumLayTopPosi.get(j+1)[i] = numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i]/(double) numberOfTopTracksMomentumLayPosi.get(j+1)[i];
    		hitEfficiencyMomentumLayBotElec.get(j+1)[i] = numberOfBotTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i]/(double) numberOfBotTracksMomentumLayElec.get(j+1)[i];
    		hitEfficiencyMomentumLayBotPosi.get(j+1)[i] = numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i]/(double) numberOfBotTracksMomentumLayPosi.get(j+1)[i];
    		if(numberOfTopTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i] != 0 && numberOfTopTracksMomentumLay.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayTop.get(j+1)[i] = Math.sqrt(1/(double) numberOfTopTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i] + 1/(double) numberOfTopTracksMomentumLay.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayTop.get(j+1)[i] = 0;
    		}
    		if(numberOfBotTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i] != 0 && numberOfBotTracksMomentumLay.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayBot.get(j+1)[i] = Math.sqrt(1/(double) numberOfBotTracksWithHitOnMissingLayerMomentumLay.get(j+1)[i] + 1/(double) numberOfBotTracksMomentumLay.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayBot.get(j+1)[i] = 0;
    		}
    		if(numberOfTopTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i] != 0 && numberOfTopTracksMomentumLayElec.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayTopElec.get(j+1)[i] = Math.sqrt(1/(double) numberOfTopTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i] + 1/(double) numberOfTopTracksMomentumLayElec.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayTopElec.get(j+1)[i] = 0;
    		}   		
    		if(numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i] != 0 && numberOfTopTracksMomentumLayPosi.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayTopPosi.get(j+1)[i] = Math.sqrt(1/(double) numberOfTopTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i] + 1/(double) numberOfTopTracksMomentumLayPosi.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayTopPosi.get(j+1)[i] = 0;
    		}
    		if(numberOfBotTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i] != 0 && numberOfBotTracksMomentumLayElec.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayBotElec.get(j+1)[i] = Math.sqrt(1/(double) numberOfBotTracksWithHitOnMissingLayerMomentumLayElec.get(j+1)[i] + 1/(double) numberOfBotTracksMomentumLayElec.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayBotElec.get(j+1)[i] = 0;
    		}   		
    		if(numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i] != 0 && numberOfBotTracksMomentumLayPosi.get(j+1)[i] != 0){
    			hitEfficiencyErrorMomentumLayBotPosi.get(j+1)[i] = Math.sqrt(1/(double) numberOfBotTracksWithHitOnMissingLayerMomentumLayPosi.get(j+1)[i] + 1/(double) numberOfBotTracksMomentumLayPosi.get(j+1)[i]);
    		}
    		else{
    			hitEfficiencyErrorMomentumLayBotPosi.get(j+1)[i] = 0;
    		}
    		HitEfficiency_MomentumLay_top.get(j+1).fill(p,hitEfficiencyMomentumLayTop.get(j+1)[i]);
    		HitEfficiency_MomentumLay_bot.get(j+1).fill(p,hitEfficiencyMomentumLayBot.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_top.get(j+1).fill(p,hitEfficiencyErrorMomentumLayTop.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_bot.get(j+1).fill(p,hitEfficiencyErrorMomentumLayBot.get(j+1)[i]);
    		HitEfficiency_MomentumLay_topElec.get(j+1).fill(p,hitEfficiencyMomentumLayTopElec.get(j+1)[i]);
    		HitEfficiency_MomentumLay_botElec.get(j+1).fill(p,hitEfficiencyMomentumLayBotElec.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_topElec.get(j+1).fill(p,hitEfficiencyErrorMomentumLayTopElec.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_botElec.get(j+1).fill(p,hitEfficiencyErrorMomentumLayBotElec.get(j+1)[i]);
    		HitEfficiency_MomentumLay_topPosi.get(j+1).fill(p,hitEfficiencyMomentumLayTopPosi.get(j+1)[i]);
    		HitEfficiency_MomentumLay_botPosi.get(j+1).fill(p,hitEfficiencyMomentumLayBotPosi.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_topPosi.get(j+1).fill(p,hitEfficiencyErrorMomentumLayTopPosi.get(j+1)[i]);
    		HitEfficiency_MomentumLayError_botPosi.get(j+1).fill(p,hitEfficiencyErrorMomentumLayBotPosi.get(j+1)[i]);
    	}
    }
   System.out.println("%===================================================================%");
   System.out.println("%======================  Hit Efficiencies ==========================%");
   System.out.println("%===================================================================% \n%");
   if(numberOfTopTracksTot > 0){
	   for(int i = 0; i<num_lay; i++){
		   System.out.println("Top Layer #"+(i+1)+ "   "+numberOfTopTracksWithHitOnMissingLayer[i] +" / "+ numberOfTopTracks[i]+ " = " +hitEfficiencyTop[i]);
	   }
       double topEfficiency = numberOfTopTracksWithHitOnMissingLayerTot/numberOfTopTracksTot;
       System.out.println("% Top Hit Efficiency: " + numberOfTopTracksWithHitOnMissingLayerTot + "/" + 
                           numberOfTopTracksTot + " = " + topEfficiency*100 + "%");
       System.out.println("% Top Hit Efficiency Error: sigma poisson = " 
                           + topEfficiency*Math.sqrt((1/numberOfTopTracksWithHitOnMissingLayerTot) + (1/numberOfTopTracksTot))*100 + "%");
       System.out.println("% Top Hit Efficiency Error: sigma binomial = " 
                           + (1/numberOfTopTracksTot)*Math.sqrt(numberOfTopTracksWithHitOnMissingLayerTot*(1-topEfficiency))*100 + "%");
   }
   if(numberOfBotTracksTot > 0){
	   for(int i = 0; i<num_lay; i++){
		   System.out.println("Bot Layer #"+(i+1)+ "   "+numberOfBotTracksWithHitOnMissingLayer[i] +" / "+ numberOfBotTracks[i]+ " = " +hitEfficiencyBot[i]); 
	   }
       double bottomEfficiency = numberOfBotTracksWithHitOnMissingLayerTot/numberOfBotTracksTot;
       System.out.println("% Bottom Hit Efficiency: " + numberOfBotTracksWithHitOnMissingLayerTot + "/" 
                           + numberOfBotTracksTot + " = " + bottomEfficiency*100 + "%");
       System.out.println("% Bottom Hit Efficiency Error: sigma poisson= " 
                           + bottomEfficiency*Math.sqrt((1/numberOfBotTracksWithHitOnMissingLayerTot) + (1/numberOfBotTracksTot))*100 + "%");
       System.out.println("% Bottom Hit Efficiency Error: sigma binomial = " 
                           + (1/numberOfBotTracksTot)*Math.sqrt(numberOfBotTracksWithHitOnMissingLayerTot*(1-bottomEfficiency))*100 + "%");
   }
/*        for(int index = 0; index < topTracksWithHitOnMissingLayer.length; index++){
       if(topTracksPerMissingLayer[index] > 0)
           System.out.println("% Top Layer " + (index+1) + ": " + (topTracksWithHitOnMissingLayer[index]/topTracksPerMissingLayer[index])*100 + "%");
   }
   for(int index = 0; index < bottomTracksWithHitOnMissingLayer.length; index++){
       if(bottomTracksPerMissingLayer[index] > 0)
           System.out.println("% Bottom Layer " + (index+1) + ": " + (bottomTracksWithHitOnMissingLayer[index]/bottomTracksPerMissingLayer[index])*100 + "%");
   }*/
   System.out.println("% \n%===================================================================%");
}

/**
* 
* @param tracks
* @param particles
*/
private void mapReconstructedParticlesToTracks(List<Track> tracks, List<ReconstructedParticle> particles) {
   
  reconParticleMap.clear();
  for (ReconstructedParticle particle : particles) {
      for (Track track : tracks) {
          if (!particle.getTracks().isEmpty() && particle.getTracks().get(0) == track) {
              reconParticleMap.put(track, particle);
          }
      }
  }
}

/**
* 
* @param track
* @return
*/
private ReconstructedParticle getReconstructedParticle(Track track) {
   return reconParticleMap.get(track);
}

}
