package org.hps.users.omoreno;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.TrackUtils;

public class SvtTrackAnalysis extends Driver {

    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private Map<String, IHistogram1D> trackPlots = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram1D> clusterChargePlots = new HashMap<String, IHistogram1D>();
    private Map<String, IHistogram1D> clusterSizePlots = new HashMap<String, IHistogram1D>();

    private List<HpsSiSensor> sensors;
    private Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap 
        = new HashMap<RawTrackerHit, LCRelation>();
    
    // Detector name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    // Collections
    private String trackCollectionName = "MatchedTracks";
    private String stereoHitRelationsColName = "HelicalTrackHitRelations";
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    private String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";

    private int runNumber = -1; 
    
    int npositive = 0;
    int nnegative = 0;
    double ntracks = 0;
    double ntracksTop = 0;
    double ntracksBottom = 0;
    double nTwoTracks = 0;
    double nevents = 0;

    double d0Cut = -9999;
    
    // Flags 
    boolean electronCut = false;
    boolean positronCut = false;
    
    /**
     *  Default Constructor
     */    
    public SvtTrackAnalysis(){
    }
    
    public void setEnableElectronCut(boolean electronCut) {
        this.electronCut = electronCut;
    }

    public void setEnablePositronCut(boolean positronCut) {
        this.positronCut = positronCut;
    }

    public void setD0Cut(double d0Cut) {
       this.d0Cut = d0Cut; 
    }
    
    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 6*(sensor.getLayerNumber() - 1); 
            } else { 
                return 6*(sensor.getLayerNumber() - 1) + 1;
            } 
        } else { 
        
            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6*(sensor.getLayerNumber() - 7) + 2;
                } else { 
                    return 6*(sensor.getLayerNumber() - 7) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6*(sensor.getLayerNumber() - 7) + 4;
                } else {
                    return 6*(sensor.getLayerNumber() - 7) + 5;
                }
            }
        }
        return -1; 
    }
    
    protected void detectorChanged(Detector detector){
    
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

        plotters.put("Event Information", plotterFactory.create("Event information"));
        plotters.get("Event Information").createRegions(2, 3);

        trackPlots.put("Number of tracks", histogramFactory.createHistogram1D("Number of tracks", 10, 0, 10));
        plotters.get("Event Information").region(0).plot(trackPlots.get("Number of tracks"));

        trackPlots.put("Track charge", histogramFactory.createHistogram1D("Track charge", 3, -1, 2));
        plotters.get("Event Information").region(1).plot(trackPlots.get("Track charge"));

        trackPlots.put("chi2", histogramFactory.createHistogram1D("chi2", 40, 0, 40));    
        plotters.get("Event Information").region(2).plot(trackPlots.get("chi2"));

        plotters.put("Track Parameters", plotterFactory.create("Track Parameters"));
        plotters.get("Track Parameters").createRegions(3, 3);

        trackPlots.put("doca", histogramFactory.createHistogram1D("doca", 80, -10, 10));         
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("doca")); 
      
        trackPlots.put("z0", histogramFactory.createHistogram1D("z0", 80, -2, 2));    
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("z0"));

        trackPlots.put("sin(phi0)", histogramFactory.createHistogram1D("sin(phi0)", 40, -0.2, 0.2));    
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("sin(phi0)"));
    
        trackPlots.put("curvature", histogramFactory.createHistogram1D("curvature", 50, -0.001, 0.001));    
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("curvature"));

        trackPlots.put("tan_lambda", histogramFactory.createHistogram1D("tan_lambda", 100, -0.1, 0.1));    
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("tan_lambda"));

        trackPlots.put("cos(theta)", histogramFactory.createHistogram1D("cos(theta)", 40, -0.1, 0.1));
        plotters.get("Track Parameters").region(5).plot(trackPlots.get("cos(theta)"));
        
        trackPlots.put("cluster time dt", histogramFactory.createHistogram1D("cluster time dt", 100, -20, 20));
        plotters.get("Track Parameters").region(6).plot(trackPlots.get("cluster time dt"));
       
        plotters.put("Cluster Amplitude", plotterFactory.create("Cluster Amplitude"));
        plotters.get("Cluster Amplitude").createRegions(6, 6);
        
        plotters.put("Cluster Size", plotterFactory.create("Cluster Size"));
        plotters.get("Cluster Size").createRegions(6, 6);
        
        
        for (HpsSiSensor sensor : sensors) { 
       
            clusterChargePlots.put(sensor.getName(), 
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Charge", 100, 0, 5000));
            plotters.get("Cluster Amplitude").region(this.computePlotterRegion(sensor))
                                             .plot(clusterChargePlots.get(sensor.getName()));
            
            clusterSizePlots.put(sensor.getName(),
                    histogramFactory.createHistogram1D(sensor.getName() + " - Cluster Size", 10, 0, 10));
            plotters.get("Cluster Size").region(this.computePlotterRegion(sensor))
                                                .plot(clusterSizePlots.get(sensor.getName()));
            
        }

        //--- Track Extrapolation ---//
        //---------------------------// 
        /*plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;

        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: curvature < 0",200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: curvature < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: curvature < 0", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: curvature > 0", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: curvature > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: curvature > 0", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Ecal: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Ecal: Two Tracks", 200, -350, 350, 200, -100, 100));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style();
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Track Position at Harp: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("Track Position at Harp: Two Tracks", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        
        //--- Momentum ---//
        //----------------//
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C > 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C > 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Py: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Py: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Pz: C < 0"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Pz: C < 0", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("Px: Two Tracks"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("Px: Two Tracks", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("E over P"));
        plotters.get(nPlotters).region(0).plot(aida.histogram1D("E over P", 100, 0, 5));
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        plotters.get(nPlotters).style().dataStyle().errorBarStyle().setVisible(false);
        nPlotters++;
           
        plotters.add(aida.analysisFactory().createPlotterFactory().create("E versus P"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("E versus P", 100, 0, 1500, 100, 0, 4000));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        
        //--- Cluster Matching ---//
        //------------------------//        
        plotters.add(aida.analysisFactory().createPlotterFactory().create("XY Difference between Ecal Cluster and Track Position"));
        plotters.get(nPlotters).region(0).plot(aida.histogram2D("XY Difference between Ecal Cluster and Track Position", 200, -200, 200, 100, -50, 50));
        plotters.get(nPlotters).region(0).style().setParameter("hist2DStyle", "colorMap");
        plotters.get(nPlotters).region(0).style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotters.get(nPlotters).style().statisticsBoxStyle().setVisible(false);
        nPlotters++;
        */
        for (IPlotter plotter : plotters.values()) { 
            plotter.show();
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void process(EventHeader event){
        nevents++;

        // Get the run number from the event
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        // If the event doesn't have any tracks, skip it    
        if(!event.hasCollection(Track.class, trackCollectionName)) return;
        
        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
        // Get the collection of LCRelations between a stereo hit and the strips making it up
        List<LCRelation> stereoHitRelations = event.get(LCRelation.class, stereoHitRelationsColName);
        BaseRelationalTable stereoHitToClusters = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : stereoHitRelations) { 
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                stereoHitToClusters.add(relation.getFrom(), relation.getTo());
            }
        }
        
        // Get the collection of LCRelations relating RotatedHelicalTrackHits to
        // HelicalTrackHits
        List<LCRelation> rotatedHthToHthRelations = event.get(LCRelation.class, rotatedHthRelationsColName);
        BaseRelationalTable hthToRotatedHth = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_ONE, RelationalTable.Weighting.UNWEIGHTED);
        for (LCRelation relation : rotatedHthToHthRelations) {
            if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                hthToRotatedHth.add(relation.getFrom(), relation.getTo());
            }
        }
        
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
        
        // Map the fitted hits to their corresponding raw hits
        this.mapFittedRawHits(fittedHits);
       
        trackPlots.get("Number of tracks").fill(tracks.size());
        
        // Loop over all of the tracks in the event
        for(Track track : tracks){
            
            if (TrackUtils.getR(track) < 0 && electronCut) continue;
            
            if (TrackUtils.getR(track) > 0 && positronCut) continue;
            
            if (d0Cut != -9999 && Math.abs(TrackUtils.getDoca(track)) < d0Cut) continue;
            
            trackPlots.get("Track charge").fill(TrackUtils.getR(track), 1);
    
            // Fill the track parameter plots
            trackPlots.get("doca").fill(TrackUtils.getDoca(track));
            trackPlots.get("z0").fill(TrackUtils.getZ0(track));
            trackPlots.get("sin(phi0)").fill(TrackUtils.getPhi0(track));
            trackPlots.get("curvature").fill(TrackUtils.getR(track));
            trackPlots.get("tan_lambda").fill(TrackUtils.getTanLambda(track));
            trackPlots.get("cos(theta)").fill(TrackUtils.getCosTheta(track));
            trackPlots.get("chi2").fill(track.getChi2());

            for (TrackerHit rotatedStereoHit : track.getTrackerHits()) { 
             
                // Get the HelicalTrackHit corresponding to the RotatedHelicalTrackHit
                // associated with a track
                Set<TrackerHit> clusters = stereoHitToClusters.allFrom(hthToRotatedHth.from(rotatedStereoHit));
                
                int clusterIndex = 0;
                double clusterTimeDt = 0;
                for (TrackerHit cluster : clusters) { 
                    
                    if (clusterIndex == 0) { 
                        clusterTimeDt += cluster.getTime();
                        clusterIndex++;
                    } else { 
                        clusterTimeDt -= cluster.getTime();
                    }
                    
                    double amplitude = 0;
                    HpsSiSensor sensor = null;
                    for (Object rawHitObject : cluster.getRawHits()) {
                        RawTrackerHit rawHit = (RawTrackerHit) rawHitObject; 
                        
                        sensor = (HpsSiSensor) rawHit.getDetectorElement();
                        
                        // Get the channel of the raw hit
                        //int channel = rawHit.getIdentifierFieldValue("strip");
                
                        // Add the amplitude of that channel to the total amplitude
                        amplitude += FittedRawTrackerHit.getAmp(this.getFittedHit(rawHit));
                    }
                    
                    clusterChargePlots.get(sensor.getName()).fill(amplitude);
                    clusterSizePlots.get(sensor.getName()).fill(cluster.getRawHits().size());
                }
                
                trackPlots.get("cluster time dt").fill(clusterTimeDt);
            }
        }
    }
    
    public void endOfData() { 
        
        String rootFile = "run" + runNumber + "_track_analysis.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     *  Method that creates a map between a fitted raw hit and it's corresponding raw fit
     *  
     * @param fittedHits : List of fitted hits to map
     */
    private void mapFittedRawHits(List<LCRelation> fittedHits) { 
        
        // Clear the fitted raw hit map of old values
        fittedRawTrackerHitMap.clear();
       
        // Loop through all fitted hits and map them to their corresponding raw hits
        for (LCRelation fittedHit : fittedHits) { 
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }
    }
    
    /**
     * 
     * @param rawHit
     * @return
     */
    private LCRelation getFittedHit(RawTrackerHit rawHit) { 
        return fittedRawTrackerHitMap.get(rawHit);
    }
}




            /*
            ntracks++;
            Hep3Vector positionEcal = TrackUtils.getTrackPositionAtEcal(track);
            System.out.println("Position at Ecal: " + positionEcal);
            Hep3Vector positionConverter = TrackUtils.extrapolateTrack(track,-700);
        
            aida.histogram2D("Track Position at Ecal").fill(positionEcal.y(), positionEcal.z());
            aida.histogram2D("Track Position at Harp").fill(positionConverter.y(), positionConverter.z());

            if(positionEcal.z() > 0 ) ntracksTop++;
            else if(positionEcal.z() < 0) ntracksBottom++;
            */
            
        
            /*    
            aida.histogram1D("Px").fill(track.getTrackStates().get(0).getMomentum()[0]);
            aida.histogram1D("Py").fill(track.getTrackStates().get(0).getMomentum()[1]);
            aida.histogram1D("Pz").fill(track.getTrackStates().get(0).getMomentum()[2]);
            aida.histogram1D("ChiSquared").fill(track.getChi2());
            
            if(Math.signum(TrackUtils.getR(track)) < 0){
                aida.histogram2D("Track Position at Ecal: curvature < 0").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: curvature < 0").fill(positionConverter.y(), positionConverter.z());
                aida.histogram1D("Px: C < 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Py: C < 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Pz: C < 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
                nnegative++;
            } else if(Math.signum(TrackUtils.getR(track)) > 0){
                aida.histogram2D("Track Position at Ecal: curvature > 0").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: curvature > 0").fill(positionConverter.y(), positionConverter.z());
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[0]);
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[1]);
                aida.histogram1D("Px: C > 0").fill(track.getTrackStates().get(0).getMomentum()[2]);
                npositive++;
            }
            
            if(tracks.size() > 1){
                aida.histogram2D("Track Position at Ecal: Two Tracks").fill(positionEcal.y(), positionEcal.z());
                aida.histogram2D("Track Position at Harp: Two Tracks").fill(positionConverter.y(), positionConverter.z()); 
                aida.histogram1D("Px: Two Tracks").fill(track.getTrackStates().get(0).getMomentum()[0]);
                if(tracks.size() == 2) nTwoTracks++;
            }
            
            trackToEcalPosition.put(positionEcal, track);
            ecalPos.add(positionEcal);          
        }
        
        if(!event.hasCollection(Cluster.class, "EcalClusters")) return;
        List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
        

        for(Hep3Vector ecalP : ecalPos){
            double xdiff = 1000; 
            double ydiff = 1000;
            for(Cluster cluster : clusters){
                double xd = ecalP.y() - cluster.getPosition()[0];
                double yd = ecalP.z() - cluster.getPosition()[1];  
                if(yd < ydiff){
                    xdiff = xd;
                    ydiff = yd;
                    trackToCluster.put(trackToEcalPosition.get(ecalP),cluster);
                }
            }
            clusters.remove(trackToCluster.get(trackToEcalPosition.get(ecalP)));
            aida.histogram2D("XY Difference between Ecal Cluster and Track Position").fill(xdiff, ydiff);
        }
        
        for(Map.Entry<Track, Cluster> entry : trackToCluster.entrySet()){
            double Energy = entry.getValue().getEnergy();
            Track track = entry.getKey();
            double pTotal = Math.sqrt(track.getTrackStates().get(0).getMomentum()[0]*track.getTrackStates().get(0).getMomentum()[0] + track.getTrackStates().get(0).getMomentum()[1]*track.getTrackStates().get(0).getMomentum()[1] + track.getTrackStates().get(0).getMomentum()[2]*track.getTrackStates().get(0).getMomentum()[2]);
            
            double ep = Energy/(pTotal*1000);
            
            System.out.println("Energy: " + Energy + "P: " + pTotal + " E over P: " + ep);
            
            aida.histogram1D("E over P").fill(ep);
            aida.histogram2D("E versus P").fill(Energy, pTotal*1000);
        }
        
        for(Cluster cluster : clusters){
            double[] clusterPosition = cluster.getPosition();
            
            System.out.println("Cluster Position: [" + clusterPosition[0] + ", " + clusterPosition[1] + ", " + clusterPosition[2]+ "]");
        }
        
        double ratio = nnegative/npositive;
        System.out.println("Ratio of Negative to Position Tracks: " + ratio);
    
        double tracksRatio = ntracks/nevents;
        double tracksTopRatio = ntracksTop/nevents;
        double tracksBottomRatio = ntracksBottom/nevents;
        double twoTrackRatio = nTwoTracks/nevents;
        System.out.println("Number of tracks per event: " + tracksRatio);
        System.out.println("Number of top tracks per event: " + tracksTopRatio);
        System.out.println("Number of bottom tracks per event: " + tracksBottomRatio);
        System.out.println("Number of two track events: " + twoTrackRatio);
    }*/


