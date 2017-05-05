package org.hps.users.omoreno;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Point3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Analysis driver used to calculate the hit efficiency of the SVT.
 */
public class SvtHitEfficiency extends Driver {

    //--------------//
    //   Plotting   //
    //--------------//
    private ITree tree = null; 
    private IHistogramFactory histogramFactory = null; 
    
    //--------------------//
    //   Histogram maps   //
    //--------------------//
    
    private Map<Integer, IHistogram1D> trackMomentumAllTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> trackMomentumWithinAcceptanceTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> trackMomentumAllLayersHitTopPlots = new HashMap<Integer, IHistogram1D>(); 
    private Map<Integer, IHistogram1D> trackMomentumAllBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> trackMomentumWithinAcceptanceBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> trackMomentumAllLayersHitBotPlots = new HashMap<Integer, IHistogram1D>();
    
    private Map<Integer, IHistogram2D> trackPositionWithinAcceptanceTopPlots = new HashMap<Integer, IHistogram2D>();
    private Map<Integer, IHistogram2D> trackPositionAllLayersHitTopPlots = new HashMap<Integer, IHistogram2D>(); 
    private Map<Integer, IHistogram2D> trackPositionWithinAcceptanceBotPlots = new HashMap<Integer, IHistogram2D>();
    private Map<Integer, IHistogram2D> trackPositionAllLayersHitBotPlots = new HashMap<Integer, IHistogram2D>(); 
    
    private Map<Integer, IHistogram1D> electronMomentumAllTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> electronMomentumWithinAcceptanceTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> electronMomentumAllLayersHitTopPlots = new HashMap<Integer, IHistogram1D>(); 
    private Map<Integer, IHistogram1D> electronMomentumAllBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> electronMomentumWithinAcceptanceBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> electronMomentumAllLayersHitBotPlots = new HashMap<Integer, IHistogram1D>();
    
    private Map<Integer, IHistogram1D> positronMomentumAllTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> positronMomentumWithinAcceptanceTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> positronMomentumAllLayersHitTopPlots = new HashMap<Integer, IHistogram1D>(); 
    private Map<Integer, IHistogram1D> positronMomentumAllBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> positronMomentumWithinAcceptanceBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> positronMomentumAllLayersHitBotPlots = new HashMap<Integer, IHistogram1D>(); 

    private Map<Integer, IHistogram1D> unbiasedXResTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> unbiasedXResBotPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> unbiasedYResTopPlots = new HashMap<Integer, IHistogram1D>();
    private Map<Integer, IHistogram1D> unbiasedYResBotPlots = new HashMap<Integer, IHistogram1D>();
    
    //private Map<SiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<SiSensor, Map<Integer, Hep3Vector>>(); 
    private List<HpsSiSensor> sensors = null;
    private Map<Integer, List<SvtStereoLayer>> topStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    private Map<Integer, List<SvtStereoLayer>> bottomStereoLayers = new HashMap<Integer, List<SvtStereoLayer>>();
    
    // Mappings between a collection of hits and the associated layer.
    private Map<Integer, List<TrackerHit>> topHitMap = new HashMap<Integer, List<TrackerHit>>();
    private Map<Integer, List<TrackerHit>> botHitMap = new HashMap<Integer, List<TrackerHit>>();

    TrackerHitUtils trackerHitUtils = new TrackerHitUtils();

    boolean debug = false;
    boolean ecalClusterTrackMatch = false;

    // Plot flags
    boolean enableMomentumPlots = true;
    boolean enableChiSquaredPlots = true;
    boolean enableTrackPositionPlots = true;
    boolean maskBadChannels = false;

    double numberOfTopTracks = 0;
    double numberOfBottomTracks = 0;
    double numberOfTopTracksWithHitOnMissingLayer = 0; 
    double numberOfBottomTracksWithHitOnMissingLayer = 0;
    double[] topTracksPerMissingLayer = new double[5];
    double[] topTracksWithHitOnMissingLayer = new double[5];
    double[] bottomTracksPerMissingLayer = new double[5];
    double[] bottomTracksWithHitOnMissingLayer = new double[5];

    // Collections
    private String stereoHitCollectionName = "HelicalTrackHits";
    private String ecalClustersCollectionName = "EcalClusters";

    // Constants
    public static final double SENSOR_LENGTH = 98.33; // mm
    public static final double SENSOR_WIDTH = 38.3399; // mm
    private static final String SUBDETECTOR_NAME = "Tracker";


    private double[] topXUnbiasedResidualMean = {
            0.136630076605, 
            -0.0372865518579, 
            0.0721381727508, 
            0.0509403980777, 
            0.0686801299522, 
            -0.252740627354
    };
    private double[] topXUnbiasedResidualSigma = {
            0.757111057485,
            0.445695560721,
            0.471524764148,
            0.691259310815,
            0.653186050157,
            1.67592292474
    };
    
    private double[] botXUnbiasedResidualMean = {
            -0.050062437621, 
            -0.0284269138592,
            -0.0185082041728,
            0.0187179831998,
            0.0718635793646,
            0.0785929625563
    };
    
    private double[] botXUnbiasedResidualSigma = {
            0.721833719938,
            0.459258383079,
            0.461651708305,
            0.669558021294,
            0.646307506985,
            1.70833919409
    };
    
    private double[] topYUnbiasedResidualMean = {
            0.0154282282709,
            0.0186347388904,
            0.0156961379185,
            -0.00238475652854,
            -0.0174100106177,
            -0.0440662300552
    };
    private double[] topYUnbiasedResidualSigma = {
            0.325752777676,
            0.24950521257,
            0.242532547574,
            0.420941281086,
            0.781453174046,
            1.16834668832
    };
    
    private double[] botYUnbiasedResidualMean = {
            -0.0277169063966,
            -0.0108234881129,
            -0.00852130805205,
            -0.0104129564111,
            0.00547904118153,
            0.00547904118153
    };
    
    private double[] botYUnbiasedResidualSigma = {
            0.389851962915,
            0.275056692408,
            0.24870417303,
            0.402831487607,
            0.694039536013,
            0.995938084198
    };
    
    /**
     * Default Constructor
     */
    public SvtHitEfficiency(){
    }

    /**
     *  Enable/Disable debug 
     */
    public void  setDebug(boolean debug){
        this.debug = debug;
    }

    /**
     * Enable/Disable masking of bad channels
     */
    public void setMaskBadChannels(boolean maskBadChannels){
        this.maskBadChannels = maskBadChannels; 
    }

    protected void detectorChanged(Detector detector){

        // Instantiate the tree and histogram factory
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

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
            
            HpsSiSensor axialSensor = stereoLayer.getAxialSensor();
            HpsSiSensor stereoSensor = stereoLayer.getStereoSensor();
            double axialZ = axialSensor.getGeometry().getPosition().z();
            double stereoZ = stereoSensor.getGeometry().getPosition().z(); 
            
            if (stereoLayer.getAxialSensor().isTopLayer()) {
            
                System.out.println("Top Z Midpoint: " + (stereoZ + axialZ)/2);
                
                if (!topStereoLayers.containsKey(stereoLayer.getLayerNumber())) { 
                    topStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                }
                
                topStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
                
            } else { 

                System.out.println("Bot Z Midpoint: " + (axialZ + stereoZ)/2);
                
                if (!bottomStereoLayers.containsKey(stereoLayer.getLayerNumber())) { 
                    bottomStereoLayers.put(stereoLayer.getLayerNumber(), new ArrayList<SvtStereoLayer>());
                }
                
                bottomStereoLayers.get(stereoLayer.getLayerNumber()).add(stereoLayer);
            }
        }

        for (int layer = 1; layer < 7; layer++) { 
            
            this.trackMomentumAllTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Track Momentum - All Tracks", 50, 0, 1.5));
            this.trackMomentumWithinAcceptanceTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Track Momentum - Within Acceptance", 50, 0, 1.5));
            this.trackMomentumAllLayersHitTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Track Momentum - All Layers Hit", 50, 0, 1.5));
            
            this.trackMomentumAllBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Track Momentum - All Tracks", 50, 0, 1.5));
            this.trackMomentumWithinAcceptanceBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Track Momentum - Within Acceptance", 50, 0, 1.5));
            this.trackMomentumAllLayersHitBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Track Momentum - All Layers Hit", 50, 0, 1.5));
            
            this.trackPositionWithinAcceptanceTopPlots.put(layer, histogramFactory.createHistogram2D("Layer " + layer + " - Top Track Position - Within Acceptance", 260, -100, 160, 80, -5, 70));
            this.trackPositionAllLayersHitTopPlots.put(layer, histogramFactory.createHistogram2D("Layer " + layer + " - Top Track Position - All Layers Hit", 260, -100, 160, 80, -5, 70));
            
            this.trackPositionWithinAcceptanceBotPlots.put(layer, histogramFactory.createHistogram2D("Layer " + layer + " - Bot Track Position - Within Acceptance", 260, -100, 160, 80, -70, 5));
            this.trackPositionAllLayersHitBotPlots.put(layer, histogramFactory.createHistogram2D("Layer " + layer + " - Bot Track Position - All Layers Hit", 260, -100, 160, 80, -70, 5));
            
            this.electronMomentumAllTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Electron Momentum - All Tracks", 50, 0, 1.5));
            this.electronMomentumWithinAcceptanceTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Electron Momentum - Within Acceptance", 50, 0, 1.5));
            this.electronMomentumAllLayersHitTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Electron Momentum - All Layers Hit", 50, 0, 1.5));
            
            this.electronMomentumAllBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Electron Momentum - All Tracks", 50, 0, 1.5));
            this.electronMomentumWithinAcceptanceBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Electron Momentum - Within Acceptance", 50, 0, 1.5));
            this.electronMomentumAllLayersHitBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Electron Momentum - All Layers Hit", 50, 0, 1.5));
            
            this.positronMomentumAllTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Positron Momentum - All Tracks", 50, 0, 1.5));
            this.positronMomentumWithinAcceptanceTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Positron Momentum - Within Acceptance", 50, 0, 1.5));
            this.positronMomentumAllLayersHitTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top Positron Momentum - All Layers Hit", 50, 0, 1.5));
            
            this.positronMomentumAllBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Positron Momentum - All Tracks", 50, 0, 1.5));
            this.positronMomentumWithinAcceptanceBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Positron Momentum - Within Acceptance", 50, 0, 1.5));
            this.positronMomentumAllLayersHitBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot Positron Momentum - All Layers Hit", 50, 0, 1.5)); 
        
            this.unbiasedXResTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top - Unbiased Residual x", 200, -10, 10));
            this.unbiasedYResTopPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Top - Unbiased Residual y", 200, -10, 10));

            this.unbiasedXResBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot - Unbiased Residual x", 200, -10, 10));
            this.unbiasedYResBotPlots.put(layer, histogramFactory.createHistogram1D("Layer " + layer + " - Bot - Unbiased Residual y", 200, -10, 10));
        }

        // Create a Map from sensor to bad channels and from bad channels to
        // strip position
        /*
           for(ChargeCarrier carrier : ChargeCarrier.values()){
           for(SiSensor sensor : sensors){ 
           if(sensor.hasElectrodesOnSide(carrier)){ 
           stripPositions.put(sensor, new HashMap<Integer, Hep3Vector>());
           SiStrips strips = (SiStrips) sensor.getReadoutElectrodes(carrier);     
           ITransform3D parentToLocal = sensor.getReadoutElectrodes(carrier).getParentToLocal();
           ITransform3D localToGlobal = sensor.getReadoutElectrodes(carrier).getLocalToGlobal();
           for(int physicalChannel = 0; physicalChannel < 640; physicalChannel++){
           Hep3Vector localStripPosition = strips.getCellPosition(physicalChannel);
           Hep3Vector stripPosition = parentToLocal.transformed(localStripPosition);
           Hep3Vector globalStripPosition = localToGlobal.transformed(stripPosition);
        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Local Strip Position " + localStripPosition.toString());
        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Strip Position " + stripPosition.toString());
        //this.printDebug(SvtUtils.getInstance().getDescription(sensor) + " : Channel " + physicalChannel + " : Strip Position " + globalStripPosition.toString());
        stripPositions.get(sensor).put(physicalChannel, stripPosition);
           }
           }
           }
           }*/


        //--- Track Fit Chi Squared ---//
        //-----------------------------//
        /*if(enableChiSquaredPlots){
          plotters.add(PlotUtils.setupPlotter("Track Chi Squared", 0, 0));
          title = "Chi Squared - All Tracks";
          histo1D = aida.histogram1D(title, 50, 0, 50);
          PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 0, "Chi Squared", histo1D);
          title = "Chi Squared - Tracks Within Acceptance";
          histo1D = aida.histogram1D(title, 50, 0, 50);
          plotters.get(plotterIndex).region(0).plot(histo1D);
          title = "Chi Squared - Tracks With All Layers Hit";
          histo1D = aida.histogram1D(title, 50, 0, 50);
          plotters.get(plotterIndex).region(0).plot(histo1D);
          plotterIndex++;
          }*/

        //--- Track Position Plots ---//
        //----------------------------//
        /*if(enableTrackPositionPlots){
          int layerNumber = 1; 
          SiSensor sensor = null;
          IPlotterStyle style = aida.analysisFactory().createPlotterFactory().createPlotterStyle();
          for(int index = 1; index < 6; index++){
          plotters.add(PlotUtils.setupPlotter("Track Position - Layer " + index, 2, 3));
          title = "Track Position - Layer " + index + " - Tracks Within Acceptance";
          cloud2D = aida.cloud2D(title);
          PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 0, "x [mm]", "y [mm]", cloud2D, style);
          title = "Track Position - Layer " + index + " - Tracks With All Layers Hit";
          cloud2D = aida.cloud2D(title);
          plotters.get(plotterIndex).region(0).plot(cloud2D, style);
          title = "Track Position - Layer " + index + " - Difference";
          cloud2D = aida.cloud2D(title);
          PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 1, "x [mm]", "y [mm]", cloud2D, style);
        //sensor = SvtUtils.getInstance().getBottomSensor(layerNumber, 0);
        //title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
        histo1D = aida.histogram1D(title, 640, 0, 639);

        histos1D.add(histo1D);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 2, "Channel #", histo1D);
        //sensor = SvtUtils.getInstance().getTopSensor(layerNumber, 0);
        //title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
        histo1D = aida.histogram1D(title, 640, 0, 639);
        histos1D.add(histo1D);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 4, "Channel #", histo1D);
        layerNumber++;
        //sensor = SvtUtils.getInstance().getBottomSensor(layerNumber, 0);
        //title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
        histo1D = aida.histogram1D(title, 640, 0, 639);
        histos1D.add(histo1D);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 3, "Channel #", histo1D);
        //sensor = SvtUtils.getInstance().getTopSensor(layerNumber, 0);
        //title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
        histo1D = aida.histogram1D(title, 640, 0, 639);
        histos1D.add(histo1D);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 5, "Channel #", histo1D);
        layerNumber++;
        plotterIndex++;
          }
          }*/

    }

    /**
     * .
     */
    /* private Hep3Vector getStripPosition(SiSensor sensor, int physicalChannel){ 
       return stripPositions.get(sensor).get(physicalChannel);
       }*/

    public void process(EventHeader event) {

        // If the event does not contain a collection of tracks, skip it.
        if(!event.hasCollection(Track.class)) return;

        // Get all collections of tracks from the event.
        List<List<Track>> track_collections = event.get(Track.class);

        // Get the collection of 3D hits from the event. This collection
        // contains all 3D hits in the event and not just those associated
        // with a track.
        List<TrackerHit> hits = event.get(TrackerHit.class, stereoHitCollectionName);
       
        // Map all 3D hits in the event to their corresponding layer.
        this.mapHitsToLayers(hits);

        // Loop over all available track collections.  Each track collection 
        // should have tracks where one of the layers is missing.
        for (List<Track> tracks : track_collections) { 

            // Check that the event has a good pair of tracks
            if (!this.hasGoodTrackPair(tracks)) continue; 

            // Loop over all tracks in the event
            for (Track track : tracks) {

                // Get one of the sensors associated with the track.  This will be used
                // to determine which hit map to use.
                HpsSiSensor sensor = (HpsSiSensor) ( (RawTrackerHit) track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
                
                // Calculate the track momentum
                double p = this.getTrackMomentum(track, -.24);
                 
                // Find which of the layers isn't being used in the track fit.  
                int unusedLayer = this.getUnusedSvtLayer(track.getTrackerHits());

                if (sensor.isTopLayer()) {
                    this.trackMomentumAllTopPlots.get(unusedLayer).fill(p);
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumAllTopPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumAllTopPlots.get(unusedLayer).fill(p);
                    }
                } else { 
                    this.trackMomentumAllBotPlots.get(unusedLayer).fill(p);
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumAllBotPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumAllBotPlots.get(unusedLayer).fill(p);
                    }
                }
                
                // Extrapolate the track to the unused layer and check that it lies
                // within the acceptance of that layer.  If it doesn't, move on
                // to the next track.
                double z = this.isWithinAcceptance(track, unusedLayer);
                if (z <= 0) continue;
                
                Hep3Vector trackPos = TrackUtils.extrapolateTrack(track, z);
           
                if (sensor.isTopLayer()) {
                    this.trackMomentumWithinAcceptanceTopPlots.get(unusedLayer).fill(p);
                    this.trackPositionWithinAcceptanceTopPlots.get(unusedLayer).fill(trackPos.x(), trackPos.y());
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumWithinAcceptanceTopPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumWithinAcceptanceTopPlots.get(unusedLayer).fill(p);
                    }
                } else { 
                    this.trackMomentumWithinAcceptanceBotPlots.get(unusedLayer).fill(p);
                    this.trackPositionWithinAcceptanceBotPlots.get(unusedLayer).fill(trackPos.x(), trackPos.y());
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumWithinAcceptanceBotPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumWithinAcceptanceBotPlots.get(unusedLayer).fill(p);
                    }
                }

                // Check if the unused layer has a 3D hit associated with a 
                // track
                if (!this.hasGood3DHit(track, unusedLayer)) continue;

                if (sensor.isTopLayer()) {
                    this.trackMomentumAllLayersHitTopPlots.get(unusedLayer).fill(p);
                    this.trackPositionAllLayersHitTopPlots.get(unusedLayer).fill(trackPos.x(), trackPos.y());
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumAllLayersHitTopPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumAllLayersHitTopPlots.get(unusedLayer).fill(p);
                    }
                } else { 
                    this.trackMomentumAllLayersHitBotPlots.get(unusedLayer).fill(p);
                    this.trackPositionAllLayersHitBotPlots.get(unusedLayer).fill(trackPos.x(), trackPos.y());
                    if (track.getTrackStates().get(0).getOmega() > 0) { 
                        this.electronMomentumAllLayersHitBotPlots.get(unusedLayer).fill(p);
                    } else { 
                        this.positronMomentumAllLayersHitBotPlots.get(unusedLayer).fill(p);
                    }
                }
            }
        }        
    }

    /**
     *
     *
     */
    private boolean hasGood3DHit(Track track, int unusedLayer) {
    
        // Get one of the sensors associated with the track.  This will be used
        // to determine which hit map to use.
        HpsSiSensor sensor = (HpsSiSensor) ( (RawTrackerHit) track.getTrackerHits().get(0).getRawHits().get(0)).getDetectorElement();
        
        // Get the hit map that will be used to search for hits in the
        // unused layer.
        Map<Integer, List<TrackerHit>> hitMap = null;
        hitMap = (sensor.isTopLayer()) ? this.topHitMap : this.botHitMap;
        
        // Get the hits associated with the layer not used to fit the track
        List<TrackerHit> hits = hitMap.get(unusedLayer);
        
        // Check if there are any 3D hits in the unused layer
        if ((hits == null) || (hits.size() == 0)) return false;
        
        for (TrackerHit hit : hits) { 
           
            Hep3Vector hitPosition = new BasicHep3Vector(hit.getPosition());
            Hep3Vector trackPosition = TrackUtils.extrapolateTrack(track, hitPosition.z());
            double xResidual = trackPosition.x() - hitPosition.x();
            double yResidual = trackPosition.y() - hitPosition.y();

            if (sensor.isTopLayer()) { 
                this.unbiasedXResTopPlots.get(unusedLayer).fill(xResidual);
                this.unbiasedYResTopPlots.get(unusedLayer).fill(yResidual);
                if ((xResidual > this.topXUnbiasedResidualMean[unusedLayer - 1] - 5*this.topXUnbiasedResidualSigma[unusedLayer - 1]) &&
                        (xResidual < this.topXUnbiasedResidualMean[unusedLayer - 1] + 5*this.topXUnbiasedResidualSigma[unusedLayer - 1]) && 
                        (yResidual > this.topYUnbiasedResidualMean[unusedLayer - 1] - 5*this.topYUnbiasedResidualSigma[unusedLayer - 1]) &&
                        (yResidual < this.topYUnbiasedResidualMean[unusedLayer - 1] + 5*this.topYUnbiasedResidualSigma[unusedLayer - 1])) { 
                    return true;
                }
            } else {
                this.unbiasedXResBotPlots.get(unusedLayer).fill(xResidual);
                this.unbiasedYResBotPlots.get(unusedLayer).fill(yResidual);
                if ((xResidual > this.botXUnbiasedResidualMean[unusedLayer - 1] - 5*this.botXUnbiasedResidualSigma[unusedLayer - 1]) &&
                        (xResidual < this.botXUnbiasedResidualMean[unusedLayer - 1] + 5*this.botXUnbiasedResidualSigma[unusedLayer - 1]) && 
                        (yResidual > this.botYUnbiasedResidualMean[unusedLayer - 1] - 5*this.botYUnbiasedResidualSigma[unusedLayer - 1]) &&
                        (yResidual < this.botYUnbiasedResidualMean[unusedLayer - 1] + 5*this.botYUnbiasedResidualSigma[unusedLayer - 1])) { 
                    return true;
                }
            }
        }
        return true;
    }
    
    /**
     * Method to check if an event has:
     * <p><ul>
     *  <li> At most two tracks
     *  <li> The two tracks are in opposite volumes
     *  <li> The two tracks are oppositely charged
     * </ul>
     *
     * @param tracks Collection of tracks in an event
     * @return True if an event satisfies the above criteria, false otherwise.
     */
    private boolean hasGoodTrackPair(List<Track> tracks) {
        
        // Require an event to have exactly two tracks
        if (tracks.size() != 2) return false;

        // Require the two tracks to be in opposite volumes
        if (tracks.get(0).getTrackStates().get(0).getTanLambda()*tracks.get(1).getTrackStates().get(0).getTanLambda() >= 0) return false;

        // Require the two tracks to be oppositely charged
        if (tracks.get(0).getTrackStates().get(0).getOmega()*tracks.get(1).getTrackStates().get(0).getOmega() >= 0) return false;

        // If all criteria are satisfied, return true.
        return true;
    }
    
    /**
     * 
     * @param track
     * @return
     */
    private double getTrackMomentum(Track track, double b_field) { 
       
        double param = 2.99792458e-04; 
        double pt = Math.abs((1/track.getTrackStates().get(0).getOmega())*b_field*param);
        double px = pt*Math.cos(track.getTrackStates().get(0).getPhi());
        double py = pt*Math.sin(track.getTrackStates().get(0).getPhi());
        double pz = pt*track.getTrackStates().get(0).getTanLambda();
        
        return (new BasicHep3Vector(px, py, pz)).magnitude();  
    }
    
    /**
     * Create a mapping between a layer number and a collection of 3D hits
     * associated with the layer.  The mappings are split by SVT volume.
     *
     * @param hits Collection of all 3D hits in the event.
     */
    private void mapHitsToLayers(List<TrackerHit> hits) { 
        
        // Clear all hit maps of all previous mappings.
        this.topHitMap.clear(); this.botHitMap.clear();
        
        // Loop over the collection of 3D hits in the event and map them to 
        // their corresponding layer.
        for (TrackerHit hit : hits) {
            
            // Retrieve the sensor associated with one of the hits.  This will
            // be used to retrieve the layer number
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) hit.getRawHits().get(0)).getDetectorElement();

            // Retrieve the layer number by using the sensor
            int layer = (sensor.getLayerNumber() + 1)/2;

            // Get the hit map corresponding to this layer
            Map<Integer, List<TrackerHit>> hitMap = null; 
            hitMap = (sensor.isTopLayer()) ? this.topHitMap : this.botHitMap; 
            
            // If a container of hits associated with a layer does not exist, 
            // create it.
            if (hitMap.get(layer) == null) { 
                hitMap.put(layer, new ArrayList<TrackerHit>()); 
            }
            
            hitMap.get(layer).add(hit);
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
    private double isWithinAcceptance(Track track, int layer) {

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

        if (this.sensorContainsTrack(axialTrackPos, stereoLayer.getAxialSensor()) 
                && this.sensorContainsTrack(stereoTrackPos, stereoLayer.getStereoSensor())) {
            
            double z = (axialTrackPos.z() + stereoTrackPos.z())/2;
            
            return z;        
        }
    }

    return -9999;
    
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
        System.out.println("%===================================================================%");
        System.out.println("%======================  Hit Efficiencies ==========================%");
        System.out.println("%===================================================================% \n%");
        if(numberOfTopTracks > 0){
            double topEfficiency = numberOfTopTracksWithHitOnMissingLayer/numberOfTopTracks;
            System.out.println("% Top Hit Efficiency: " + numberOfTopTracksWithHitOnMissingLayer + "/" + 
                    numberOfTopTracks + " = " + topEfficiency*100 + "%");
            System.out.println("% Top Hit Efficiency Error: sigma poisson = " 
                    + topEfficiency*Math.sqrt((1/numberOfTopTracksWithHitOnMissingLayer) + (1/numberOfTopTracks))*100 + "%");
            System.out.println("% Top Hit Efficiency Error: sigma binomial = " 
                    + (1/numberOfTopTracks)*Math.sqrt(numberOfTopTracksWithHitOnMissingLayer*(1-topEfficiency))*100 + "%");
        }
        if(numberOfBottomTracks > 0){
            double bottomEfficiency = numberOfBottomTracksWithHitOnMissingLayer/numberOfBottomTracks;
            System.out.println("% Bottom Hit Efficiency: " + numberOfBottomTracksWithHitOnMissingLayer + "/" 
                    + numberOfBottomTracks + " = " + bottomEfficiency*100 + "%");
            System.out.println("% Bottom Hit Efficiency Error: sigma poisson= " 
                    + bottomEfficiency*Math.sqrt((1/numberOfBottomTracksWithHitOnMissingLayer) + (1/numberOfBottomTracks))*100 + "%");
            System.out.println("% Top Hit Efficiency Error: sigma binomial = " 
                    + (1/numberOfBottomTracks)*Math.sqrt(numberOfBottomTracksWithHitOnMissingLayer*(1-bottomEfficiency))*100 + "%");
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


        String rootFile = "run" + "_cluster_analysis.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }


    }    
}
