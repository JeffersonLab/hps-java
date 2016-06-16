/**
 * Analysis driver to calculate hit efficiencies in the SVT
 */
/**
 * @author mrsolt
 *
 */
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
    IHistogram1D HitEfficiencyError_top;
    IHistogram1D HitEfficiencyError_bot;
    IHistogram1D HitEfficiency_Momentum_top;
    IHistogram1D HitEfficiency_Momentum_bot;
    IHistogram1D HitEfficiencyError_Momentum_top;
    IHistogram1D HitEfficiencyError_Momentum_bot;
    IHistogram1D TrackXTop;
    IHistogram1D TrackYTop;
    IHistogram1D TrackXBot;
    IHistogram1D TrackYBot;
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
    
    double topXResidualOffset1 = 0; 
    double topYResidualOffset1 = 0; 
    double botXResidualOffset1 = 0; 
    double botYResidualOffset1 = 0; 
    
    double topXResidualCut1 = 3;
    double topYResidualCut1 = 2;
    double botXResidualCut1 = 3;
    double botYResidualCut1 = 2;
    
    double topXResidualOffset2 = 0; 
    double topYResidualOffset2 = 0; 
    double botXResidualOffset2 = 0; 
    double botYResidualOffset2 = 0; 
    
    double topXResidualCut2 = 2;
    double topYResidualCut2 = 1;
    double botXResidualCut2 = 2;
    double botYResidualCut2 = 1;
    
    double topXResidualOffset3 = 0; 
    double topYResidualOffset3 = 0; 
    double botXResidualOffset3 = 0; 
    double botYResidualOffset3 = 0; 
    
    double topXResidualCut3 = 2;
    double topYResidualCut3 = 1;
    double botXResidualCut3 = 2;
    double botYResidualCut3 = 1;
    
    double topXResidualOffset4 = 0; 
    double topYResidualOffset4 = 0; 
    double botXResidualOffset4 = 0; 
    double botYResidualOffset4 = 0; 
    
    double topXResidualCut4 = 2;
    double topYResidualCut4 = 1;
    double botXResidualCut4 = 2;
    double botYResidualCut4 = 1;
    
    double topXResidualOffset5 = 0; 
    double topYResidualOffset5 = 0; 
    double botXResidualOffset5 = 0; 
    double botYResidualOffset5 = 0; 
    
    double topXResidualCut5 = 2;
    double topYResidualCut5 = 2;
    double botXResidualCut5 = 2;
    double botYResidualCut5 = 2;
    
    double topXResidualOffset6 = 0; 
    double topYResidualOffset6 = 0; 
    double botXResidualOffset6 = 0; 
    double botYResidualOffset6 = 0; 
    
    double topXResidualCut6 = 6;
    double topYResidualCut6 = 3;
    double botXResidualCut6 = 6;
    double botYResidualCut6 = 3;
    
    int nSigCut = 1;

    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    
    boolean debug = false;
    boolean ecalClusterTrackMatch = false;
    
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

    int nBinx = 110;
    int nBiny = 120;
    int nP = 20;
    int[] numberOfTopTracks = new int[num_lay];
    int[] numberOfBotTracks = new int[num_lay];
    int[] numberOfTopTracksWithHitOnMissingLayer = new int[num_lay];
    int[] numberOfBotTracksWithHitOnMissingLayer = new int[num_lay];
    double[] hitEfficiencyTop = new double[num_lay];
    double[] hitEfficiencyBot = new double[num_lay];
    double[] errorTop = new double[num_lay];
    double[] errorBot = new double[num_lay];
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
    
    double lowerLimX = -100;
    double upperLimX = 120;
    double lowerLimY = -50;
    double upperLimY = 70;
    double lowerLimP;
    double upperLimP;
   
    double angle = -0.035;
    double xComp = Math.sin(angle);
    double yComp = Math.cos(angle);
    double zComp = 0.0;
    Hep3Vector unitNormal = new BasicHep3Vector(xComp,yComp,zComp);
    
    Hep3Vector zPointonPlaneLay1Top = new BasicHep3Vector(0,92.09,0);
    Hep3Vector zPointonPlaneLay2Top = new BasicHep3Vector(0,192.1,0);
    Hep3Vector zPointonPlaneLay3Top = new BasicHep3Vector(0,292.2,0);
    Hep3Vector zPointonPlaneLay4Top = new BasicHep3Vector(0,492.4,0);
    Hep3Vector zPointonPlaneLay5Top = new BasicHep3Vector(0,692.7,0);
    Hep3Vector zPointonPlaneLay6Top = new BasicHep3Vector(0,893.2,0);
    Hep3Vector[] zPointonPlaneTop = new Hep3Vector[num_lay];
    
    Hep3Vector zPointonPlaneLay1Bot = new BasicHep3Vector(0,107.7,0);
    Hep3Vector zPointonPlaneLay2Bot = new BasicHep3Vector(0,207.9,0);
    Hep3Vector zPointonPlaneLay3Bot = new BasicHep3Vector(0,308.8,0);
    Hep3Vector zPointonPlaneLay4Bot = new BasicHep3Vector(0,508.4,0);
    Hep3Vector zPointonPlaneLay5Bot = new BasicHep3Vector(0,708.7,0);
    Hep3Vector zPointonPlaneLay6Bot = new BasicHep3Vector(0,909.0,0);
    Hep3Vector[] zPointonPlaneBot = new Hep3Vector[num_lay];
    
    Hep3Vector trackPos = null;
    Hep3Vector frontTrackPos = null;
    Hep3Vector rearTrackPos = null;
    
    // Collections
    private String fsParticlesCollectionName = "FinalStateParticles";
    private String stereoHitCollectionName = "HelicalTrackHits";
    private String trackCollectionName = "MatchedTracks";
   
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
        lowerLimP = 0.2 * ebeam;
        upperLimP = 1.3 * ebeam;
        
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
        
        topXResidualOffset[0] = topXResidualOffset1;
        topYResidualOffset[0] = topYResidualOffset1; 
        botXResidualOffset[0] = botXResidualOffset1; 
        botYResidualOffset[0] = botYResidualOffset1; 
        
        topXResidualCut[0] = topXResidualCut1;
        topYResidualCut[0] = topYResidualCut1;
        botXResidualCut[0] = botXResidualCut1;
        botYResidualCut[0] = botYResidualCut1;
        
        topXResidualOffset[1] = topXResidualOffset2;
        topYResidualOffset[1] = topYResidualOffset2; 
        botXResidualOffset[1] = botXResidualOffset2; 
        botYResidualOffset[1] = botYResidualOffset2; 
        
        topXResidualCut[1] = topXResidualCut2;
        topYResidualCut[1] = topYResidualCut2;
        botXResidualCut[1] = botXResidualCut2;
        botYResidualCut[1] = botYResidualCut2;
        
        topXResidualOffset[2] = topXResidualOffset3;
        topYResidualOffset[2] = topYResidualOffset3; 
        botXResidualOffset[2] = botXResidualOffset3; 
        botYResidualOffset[2] = botYResidualOffset3; 
        
        topXResidualCut[2] = topXResidualCut3;
        topYResidualCut[2] = topYResidualCut3;
        botXResidualCut[2] = botXResidualCut3;
        botYResidualCut[2] = botYResidualCut3;
        
        topXResidualOffset[3] = topXResidualOffset4;
        topYResidualOffset[3] = topYResidualOffset4; 
        botXResidualOffset[3] = botXResidualOffset4; 
        botYResidualOffset[3] = botYResidualOffset4; 
        
        topXResidualCut[3] = topXResidualCut4;
        topYResidualCut[3] = topYResidualCut4;
        botXResidualCut[3] = botXResidualCut4;
        botYResidualCut[3] = botYResidualCut4;
        
        topXResidualOffset[4] = topXResidualOffset5;
        topYResidualOffset[4] = topYResidualOffset5; 
        botXResidualOffset[4] = botXResidualOffset5; 
        botYResidualOffset[4] = botYResidualOffset5; 
        
        topXResidualCut[4] = topXResidualCut5;
        topYResidualCut[4] = topYResidualCut5;
        botXResidualCut[4] = botXResidualCut5;
        botYResidualCut[4] = botYResidualCut5;
        
        topXResidualOffset[5] = topXResidualOffset6;
        topYResidualOffset[5] = topYResidualOffset6; 
        botXResidualOffset[5] = botXResidualOffset6; 
        botYResidualOffset[5] = botYResidualOffset6; 
        
        topXResidualCut[5] = topXResidualCut6;
        topYResidualCut[5] = topYResidualCut6;
        botXResidualCut[5] = botXResidualCut6;
        botYResidualCut[5] = botYResidualCut6;
        
        
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
        HitEfficiency_Momentum_top = aida.histogram1D("Hit Efficiency Momentum Top", nP, lowerLimP, upperLimP);
        HitEfficiency_Momentum_bot = aida.histogram1D("Hit Efficiency Momentum Bot", nP, lowerLimP, upperLimP);
        HitEfficiencyError_Momentum_top = aida.histogram1D("Hit Efficiency Error Momentum Top", nP, lowerLimP, upperLimP);
        HitEfficiencyError_Momentum_bot = aida.histogram1D("Hit Efficiency Error Momentum Bot", nP, lowerLimP, upperLimP);
        TrackXTop = aida.histogram1D("Extrapolated Track X Top", 50, lowerLimX, upperLimX);
        TrackYTop = aida.histogram1D("Extrapolated Track Y Top", 50, lowerLimY, upperLimY);
        TrackXBot = aida.histogram1D("Extrapolated Track X Bot", 50, lowerLimX, upperLimX);
        TrackYBot = aida.histogram1D("Extrapolated Track Y Bot", 50, lowerLimY, upperLimY);
        
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
        }
        
        for (IPlotter plotter : plotters.values()) { 
            plotter.show();
        }
    }
    @Override
public void process(EventHeader event){
		aida.tree().cd("/");
        
		System.out.println("Hello");
		
        // If the event does not have tracks, skip it
        //if(!event.hasCollection(Track.class, trackCollectionName)) return;

        // Get the list of tracks from the event
        List<List<Track>> trackCollections = event.get(Track.class);
        
        // For now, only look at events with a single track
        //if(tracks.size() != 1 ) return;
       
        // Get the list of final state particles from the event.  These will
        // be used to obtain the track momentum.
        List<ReconstructedParticle> fsParticles = event.get(ReconstructedParticle.class, fsParticlesCollectionName);
      
        //this.mapReconstructedParticlesToTracks(tracks, fsParticles);
        
        //trackPlots.get("Number of tracks").fill(tracks.size());
        
        //  Get all of the stereo hits in the event
        List<TrackerHit> stereoHits = event.get(TrackerHit.class, stereoHitCollectionName);
        
        // Get the list of Ecal clusters from the event
        //List<Cluster> ecalClusters = event.get(Cluster.class, ecalClustersCollectionName);

        for (List<Track> tracks : trackCollections) {
        	for(Track track : tracks){
        		// Check that the track has the required number of hits.  The number of hits
        		// required to make a track is set in the tracking strategy.
        		if(track.getTrackerHits().size() != this.hitsOnTrack){
        			System.out.println("This track doesn't have the required number of hits.");
        			continue;
        		}          
        		HpsSiSensor trackSensor = (HpsSiSensor) ((RawTrackerHit)track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
        		// Calculate the momentum of the track
        		HelicalTrackFit hlc_trk_fit = TrackUtils.getHTF(track.getTrackStates().get(0));
        		
        		//double p = this.getReconstructedParticle(track).getMomentum().magnitude();
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
        		if(!isWithinAcceptance(track, unusedLayer)) continue;
        		
        		//int nTop = 0;
        		//int nBot = 0;
        		//for (TrackerHit stereoHit : stereoHits) {
        			//HpsSiSensor hitSensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
      	          
        			//if ((trackSensor.isTopLayer() && hitSensor.isBottomLayer()) 
        					//|| (trackSensor.isBottomLayer() && hitSensor.isTopLayer())) continue;
        			
        			//int layer = (hitSensor.getLayerNumber() + 1)/2;
               	 	
               	 	//if (unusedLayer != layer) continue; 
        			
        			//Hep3Vector stereoHitPosition = new BasicHep3Vector(stereoHit.getPosition());
           	 		//Hep3Vector trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.z());
           	 	//Hep3Vector trackPositionExtrap = TrackUtils.getHelixPlaneIntercept(hlc_trk_fit,unitNormal,zPointonPlane[unusedLayer-1],B_field);
           	 	//TrackX.fill(trackPositionExtrap.x());
           	 	//TrackY.fill(trackPositionExtrap.y());
           	 	trackMomentum_accepted.get(unusedLayer).fill(p);
           	 	if (trackSensor.isTopLayer()) {
           	 			//if (nTop > 0){;
           	 				//continue;
           	 			//}
           	 		//Hep3Vector trackPositionExtrap = TrackUtils.getHelixPlaneIntercept(hlc_trk_fit,unitNormal,zPointonPlaneTop[unusedLayer-1],B_field);
           	 		//TrackXTop.fill(trackPositionExtrap.x());
           	 		//TrackYTop.fill(trackPositionExtrap.y());
           	 		trackMomentum_accepted_top.get(unusedLayer).fill(p);
           	 		numberOfTopTracks[unusedLayer-1]++;
           	 		for(int i = 0; i<nP;i++){
           	 			double mP = (upperLimP - lowerLimP)/((double) nP);
           	 			double lowerP = mP * i + lowerLimP;
           	 			double upperP = lowerP + mP;
           	 			if(lowerP < p && p < upperP){
           	 				numberOfTopTracksMomentum[i]++;
           	 			}
           	 		}
           	 		for(int i = 0; i<nBinx; i++){
           	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
           	 			double lowerX = mX * i + lowerLimX;
           	 			double upperX = lowerX + mX;
           	 			/*if(lowerX < trackPositionExtrap.x() && trackPositionExtrap.x() < upperX){
           	 				for(int j = 0; j<nBiny; j++){
           	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
           	 					double lowerY = mY * j + lowerLimY;
           	 					double upperY = lowerY + mY;
           	 					if(lowerY < trackPositionExtrap.y() && trackPositionExtrap.y() < upperY){
           	 						numberOfTopTracksPos[unusedLayer-1][i][j]++;
           	 					}     
           	 				}
           	 			}*/          	 			
           	 		}
           	 		//nTop++;
           	 	} else{
           	 			//if (nBot > 0){;
           	 			//continue;
           	 			//}
           	 		//Hep3Vector trackPositionExtrap = TrackUtils.getHelixPlaneIntercept(hlc_trk_fit,unitNormal,zPointonPlaneBot[unusedLayer-1],B_field);
           	 		//TrackXBot.fill(trackPositionExtrap.x());
           	 		//TrackYBot.fill(trackPositionExtrap.y());
           	 		trackMomentum_accepted_bot.get(unusedLayer).fill(p);
           	 		numberOfBotTracks[unusedLayer-1]++;
           	 		for(int i = 0; i<nP;i++){
           	 			double mP = (upperLimP - lowerLimP)/((double) nP);
           	 			double lowerP = mP * i + lowerLimP;
           	 			double upperP = lowerP + mP;
           	 			if(lowerP < p && p < upperP){
           	 				numberOfBotTracksMomentum[i]++;
           	 			}
           	 		}
           	 		for(int i = 0; i<nBinx; i++){
           	 			double mX = (upperLimX - lowerLimX)/((double) nBinx);
           	 			double lowerX = mX * i + lowerLimX;
           	 			double upperX = lowerX + mX;
           	 			/*if(lowerX < trackPositionExtrap.x() && trackPositionExtrap.x() < upperX){
           	 				for(int j = 0; j<nBiny; j++){
           	 					double mY = (upperLimY - lowerLimY)/((double) nBiny);
           	 					double lowerY = mY * j + lowerLimY;
           	 					double upperY = lowerY + mY;
           	 					if(lowerY < trackPositionExtrap.y() && trackPositionExtrap.y() < upperY){
           	 						numberOfBotTracksPos[unusedLayer-1][i][j]++;
           	 						//}     
           	 				}          	 			
           	 			}
           	 		}*/
           	 			//nBot++;
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
               	 	
               	 	//System.out.println(count);
               	 	
               	 	//System.out.println("Track " + track + "   Collection " + trackCollections);
                        
               	 	Hep3Vector stereoHitPosition = new BasicHep3Vector(stereoHit.getPosition());
               	 	Hep3Vector trackPosition = TrackUtils.extrapolateTrack(track, stereoHitPosition.z());
               	 	double xResidual = trackPosition.x() - stereoHitPosition.x();
               	 	double yResidual = trackPosition.y() - stereoHitPosition.y();
             
               	 	trackMomentum_final.get(unusedLayer).fill(p);                        
               	 	if (hitSensor.isTopLayer()) {
               	 		UnbiasedResidualX_top.get(unusedLayer).fill(xResidual);
               	 		UnbiasedResidualY_top.get(unusedLayer).fill(yResidual);
               	 		HitPosition_top.get(unusedLayer).fill(stereoHitPosition.z());
               	 		trackPlots.get("Unbiased Residual x - Top").fill(xResidual);
               	 		trackPlots.get("Unbiased Residual y - Top").fill(yResidual);
               	 		//if (Math.abs(xResidual+this.topXResidualOffset) > nSigCut*this.topXResidualCut 
               	 				//&& Math.abs(yResidual + this.topYResidualOffset) > nSigCut*this.topYResidualCut) continue;
               	 		if (Math.abs(xResidual+topXResidualOffset[unusedLayer-1]) > nSigCut*topXResidualCut[unusedLayer-1] 
       	 					|| Math.abs(yResidual + topYResidualOffset[unusedLayer-1]) > nSigCut*topYResidualCut[unusedLayer-1]) continue;
               	 		if (countTop > 0){
               	 			System.out.println("Top " + unusedLayer);
               	 			continue;
               	 		}
               	 		trackMomentumPlots.get("Track Momentum - All Layers Hit").fill(p);
               	 		trackMomentum_final_top.get(unusedLayer).fill(p);
               	 		numberOfTopTracksWithHitOnMissingLayer[unusedLayer-1]++;
               	 		for(int i = 0; i<nP;i++){
               	 			double mP = (upperLimP - lowerLimP)/((double) nP);
               	 			double lowerP = mP * i + lowerLimP;
               	 			double upperP = lowerP + mP;
               	 			if(lowerP < p && p < upperP){
               	 				numberOfTopTracksWithHitOnMissingLayerMomentum[i]++;
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
               	 		countTop++;               	                	 		
               	 	} else {
               	 		UnbiasedResidualX_bot.get(unusedLayer).fill(xResidual);
               	 		UnbiasedResidualY_bot.get(unusedLayer).fill(yResidual);
               	 		HitPosition_bot.get(unusedLayer).fill(stereoHitPosition.z());
               	 		trackPlots.get("Unbiased Residual x - Bottom").fill(xResidual);
               	 		trackPlots.get("Unbiased Residual y - Bottom").fill(yResidual);
               	 		//if (Math.abs(xResidual+this.botXResidualOffset) > nSigCut*this.botXResidualCut 
               	 				//&& Math.abs(yResidual + this.botYResidualOffset) > nSigCut*this.botYResidualCut) continue;
               	 		if (Math.abs(xResidual+botXResidualOffset[unusedLayer-1]) > nSigCut*botXResidualCut[unusedLayer-1] 
       	 					|| Math.abs(yResidual + botYResidualOffset[unusedLayer-1]) > nSigCut*botYResidualCut[unusedLayer-1]) continue;
               	 		if (countBot > 0){
           	 				System.out.println("Bot " + unusedLayer);
           	 				continue;
           	 			}
               	 		trackMomentumPlots.get("Track Momentum - All Layers Hit").fill(p);
               	 		trackMomentum_final_bot.get(unusedLayer).fill(p);
               	 		numberOfBotTracksWithHitOnMissingLayer[unusedLayer-1]++;
               	 		for(int i = 0; i<nP;i++){
               	 			double mP = (upperLimP - lowerLimP)/((double) nP);
               	 			double lowerP = mP * i + lowerLimP;
               	 			double upperP = lowerP + mP;
               	 			if(lowerP < p && p < upperP){
               	 				numberOfBotTracksWithHitOnMissingLayerMomentum[i]++;
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
            System.out.println("Layer number " + (layer+1) + " is not used");
            return (layer + 1);
        }
    }
  
    // If all of the layers are being used, this track can't be used to 
    // in the single hit efficiency calculation.  This means that something
    // is wrong with the file
    // TODO: This should probably throw an exception
    return -1;
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
    
    /*for(int i = 0; i<num_lay; i++){
    	hitEfficiencyTop[i] = numberOfTopTracksWithHitOnMissingLayer[i]/ (double) numberOfTopTracks[i];
    	System.out.println(hitEfficiencyTop[i]);
    	hitEfficiencyBot[i] = numberOfBotTracksWithHitOnMissingLayer[i]/(double) numberOfBotTracks[i];
    	numberOfTopTracksTot = numberOfTopTracksTot + numberOfTopTracks[i];
    	numberOfBotTracksTot = numberOfBotTracksTot + numberOfBotTracks[i];
    	numberOfTopTracksWithHitOnMissingLayerTot = numberOfTopTracksWithHitOnMissingLayerTot + numberOfTopTracksWithHitOnMissingLayer[i];
    	numberOfBotTracksWithHitOnMissingLayerTot = numberOfBotTracksWithHitOnMissingLayerTot + numberOfBotTracksWithHitOnMissingLayer[i];
    	errorTop[i] = Math.sqrt(1/(double) numberOfTopTracks[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayer[i]);
    	errorBot[i] = Math.sqrt(1/(double) numberOfBotTracks[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayer[i]);
    	
    	
    	
    	HitEfficiency_top.fill(i,hitEfficiencyTop[i]);
    	HitEfficiency_bot.fill(i,hitEfficiencyBot[i]);
    	HitEfficiencyError_top.fill(i,errorTop[i]);
    	HitEfficiencyError_bot.fill(i,errorBot[i]);
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
    			//System.out.println(i + "  " + x + "  " + y + "  " + numberOfTopTracksWithHitOnMissingLayerPos[i][j][k] + "  " + numberOfTopTracksPos[i][j][k] + "  " + hitEfficiencyTopPos[i][j][k]);
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
    	hitEfficiencyErrorMomentumTop[i] = Math.sqrt(1/(double) numberOfTopTracksMomentum[i] + 1/(double) numberOfTopTracksWithHitOnMissingLayerMomentum[i]);
    	hitEfficiencyErrorMomentumBot[i] = Math.sqrt(1/(double) numberOfBotTracksMomentum[i] + 1/(double) numberOfBotTracksWithHitOnMissingLayerMomentum[i]);
		double p = mP * (i+0.5) + lowerLimP;
    	HitEfficiency_Momentum_top.fill(p,hitEfficiencyMomentumTop[i]);
    	HitEfficiency_Momentum_bot.fill(p,hitEfficiencyMomentumBot[i]);
    	HitEfficiencyError_Momentum_top.fill(p,hitEfficiencyErrorMomentumTop[i]);
    	HitEfficiencyError_Momentum_bot.fill(p,hitEfficiencyErrorMomentumBot[i]);
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
