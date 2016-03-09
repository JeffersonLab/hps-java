package org.hps.users.omoreno;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.BasicHep3Vector;

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
import org.hps.recon.tracking.TrackUtils;

public class SharedHitAnalysis extends Driver {

    // Use JFreeChart as the default plotting backend
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Collections
    private String trackCollectionName = "MatchedTracks";
    private String stereoHitRelationsColName = "HelicalTrackHitRelations";
    private String rotatedHthRelationsColName = "RotatedHelicalTrackHitRelations";
    private String helicalTrackHitsColName = "HelicalTrackHits";
    
    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private Map<String, IHistogram1D> trackPlots = new HashMap<String, IHistogram1D>();
    
    private Map<Integer, List<TrackerHit>> topLayerToStereoHit = new HashMap<Integer, List<TrackerHit>>();
    private Map<Integer, List<TrackerHit>> bottomLayerToStereoHit = new HashMap<Integer, List<TrackerHit>>();
    
    IPlotterStyle createStyle(int color, String xAxisTitle, String yAxisTitle) { 
       
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);
        
        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(4);
        style.dataStyle().fillStyle().setVisible(true);
        
        if (color == 1) { 
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
            style.dataStyle().fillStyle().setOpacity(.30);
        } else if (color == 2) { 
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
            style.dataStyle().fillStyle().setOpacity(.30);
        } else if (color == 3) { 
            style.dataStyle().fillStyle().setColor("255, 38, 38, 1");
            style.dataStyle().outlineStyle().setColor("255, 38, 38, 1");
            style.dataStyle().fillStyle().setOpacity(.70);
        }
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        return style;
    }
    
    
    protected void detectorChanged(Detector detector){

        for (int layer = 1; layer <= 6; layer++) { 
            
            topLayerToStereoHit.put(layer, new ArrayList<TrackerHit>());
            bottomLayerToStereoHit.put(layer, new ArrayList<TrackerHit>());
        }
        
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        plotters.put("Event Information", plotterFactory.create("Event information"));
        plotters.get("Event Information").createRegions(2, 3);

        trackPlots.put("Number of tracks", histogramFactory.createHistogram1D("Number of tracks", 10, 0, 10));
        plotters.get("Event Information").region(0).plot(trackPlots.get("Number of tracks"));
        
        trackPlots.put("chi2", histogramFactory.createHistogram1D("chi2", 40, 0, 40));    
        trackPlots.put("chi2 - shared strip hit", histogramFactory.createHistogram1D("chi2 - shared strip hit", 40, 0, 40));    
        trackPlots.put("chi2 - l1 Isolation", histogramFactory.createHistogram1D("chi2 - l1 Isolation", 40, 0, 40));    
        plotters.get("Event Information").region(2).plot(trackPlots.get("chi2"), this.createStyle(1, "", ""));
        plotters.get("Event Information").region(2).plot(trackPlots.get("chi2 - shared strip hit"), this.createStyle(2, "", ""));
        plotters.get("Event Information").region(2).plot(trackPlots.get("chi2 - l1 Isolation"), this.createStyle(3, "", ""));

        plotters.put("Track Parameters", plotterFactory.create("Track Parameters"));
        plotters.get("Track Parameters").createRegions(3, 3);

        trackPlots.put("doca", histogramFactory.createHistogram1D("doca", 80, -10, 10));         
        trackPlots.put("doca - shared strip hit", histogramFactory.createHistogram1D("doca - shared strip hit", 80, -10, 10));         
        trackPlots.put("doca - l1 Isolation", histogramFactory.createHistogram1D("doca - l1 Isolation", 80, -10, 10));         
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("doca"),this.createStyle(1, "", "")); 
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("doca - shared strip hit"),  this.createStyle(2, "", "")); 
        plotters.get("Track Parameters").region(0).plot(trackPlots.get("doca - l1 Isolation"),  this.createStyle(3, "", "")); 
      
        trackPlots.put("z0", histogramFactory.createHistogram1D("z0", 80, -2, 2));    
        trackPlots.put("z0 - shared strip hit", histogramFactory.createHistogram1D("z0 - shared strip hit", 80, -2, 2));    
        trackPlots.put("z0 - l1 Isolation", histogramFactory.createHistogram1D("z0 - l1 Isolation", 80, -2, 2));    
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("z0"), this.createStyle(1, "", ""));
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("z0 - shared strip hit"),  this.createStyle(2, "", ""));
        plotters.get("Track Parameters").region(1).plot(trackPlots.get("z0 - l1 Isolation"),  this.createStyle(3, "", ""));

        trackPlots.put("sin(phi0)", histogramFactory.createHistogram1D("sin(phi0)", 40, -0.2, 0.2));    
        trackPlots.put("sin(phi0) - shared strip hit", histogramFactory.createHistogram1D("sin(phi0) - shared strip hit", 40, -0.2, 0.2));    
        trackPlots.put("sin(phi0) - l1 Isolation", histogramFactory.createHistogram1D("sin(phi0) - l1 Isolation", 40, -0.2, 0.2));    
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("sin(phi0)"), this.createStyle(1, "", ""));
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("sin(phi0) - shared strip hit"),  this.createStyle(2, "", ""));
        plotters.get("Track Parameters").region(2).plot(trackPlots.get("sin(phi0) - l1 Isolation"),  this.createStyle(3, "", ""));
    
        trackPlots.put("curvature", histogramFactory.createHistogram1D("curvature", 50, -0.001, 0.001));    
        trackPlots.put("curvature - shared strip hit", histogramFactory.createHistogram1D("curvature - shared strip hit", 50, -0.001, 0.001));    
        trackPlots.put("curvature - l1 Isolation", histogramFactory.createHistogram1D("curvature - l1 Isolation", 50, -0.001, 0.001));    
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("curvature"), this.createStyle(1, "", ""));
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("curvature - shared strip hit"),  this.createStyle(2, "", ""));
        plotters.get("Track Parameters").region(3).plot(trackPlots.get("curvature - l1 Isolation"),  this.createStyle(3, "", ""));

        trackPlots.put("tan_lambda", histogramFactory.createHistogram1D("tan_lambda", 100, -0.1, 0.1));    
        trackPlots.put("tan_lambda - shared strip hit", histogramFactory.createHistogram1D("tan_lambda - shared strip hit", 100, -0.1, 0.1));    
        trackPlots.put("tan_lambda - l1 Isolation", histogramFactory.createHistogram1D("tan_lambda - l1 Isolation", 100, -0.1, 0.1));    
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("tan_lambda"), this.createStyle(1, "", ""));
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("tan_lambda - shared strip hit"),  this.createStyle(2, "", ""));
        plotters.get("Track Parameters").region(4).plot(trackPlots.get("tan_lambda - l1 Isolation"),  this.createStyle(3, "", ""));
    
        for (IPlotter plotter : plotters.values()) { 
            plotter.show();
        }
    }
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void process(EventHeader event){

        // If the event doesn't have any tracks, skip it    
        if(!event.hasCollection(Track.class, trackCollectionName)) return;
        
        // Get the collection of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        
        // Get the collection of LCRelations between a stereo hit and the strips
        // making it up
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

        List<TrackerHit> stereoHits = event.get(TrackerHit.class, helicalTrackHitsColName);
        this.mapStereoHits(stereoHits);
        
        // Loop over all of the tracks in the event
        for(Track track : tracks){
        
            boolean sharedHitTrack = false;
           boolean l1Isolation = true;
            
            // Fill the track parameter plots
            
            // Loop through all of the stereo hits associated with a track
            for (TrackerHit rotatedStereoHit : track.getTrackerHits()) { 
               
                TrackerHit stereoHit = (TrackerHit) hthToRotatedHth.from(rotatedStereoHit);

                // Get the HelicalTrackHit corresponding to the 
                // RotatedHelicalTrackHit associated with a track
                Set<TrackerHit> clusters = stereoHitToClusters.allFrom(stereoHit);
                
                for (TrackerHit cluster : clusters) {
                    
                    Set<TrackerHit> sharedStereoHits = stereoHitToClusters.allTo(cluster);
                    
                    if (sharedStereoHits.size() > 1) {
                        //System.out.println("Multiple stereo hits share a hit");
                        
                        for (TrackerHit sharedStereoHit : sharedStereoHits) { 
                            //System.out.println("Shared stereo hit position: " + (new BasicHep3Vector(sharedStereoHit.getPosition())).toString());
                        }
                        sharedHitTrack = true;
                    }
                }
            
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
                int layer = (sensor.getLayerNumber() + 1)/2;
           
                List<TrackerHit> layerHits; 
                if (sensor.isTopLayer()) { 
                    layerHits = this.getTopLayerStereoHits(layer);
                } else { 
                    layerHits = this.getBottomLayerStereoHits(layer);
                }
                
                if (layerHits.size() > 1 && layer == 1) { 
                   
                    for (TrackerHit layerHit : layerHits) { 
                        if (layerHit == stereoHit) continue;
                        
                        double deltaX = stereoHit.getPosition()[0] - layerHit.getPosition()[0];
                        double deltaY = stereoHit.getPosition()[1] - layerHit.getPosition()[1];
                        
                        if (Math.abs(deltaX) < .5 && Math.abs(deltaY) < .5) {
                           l1Isolation = false; 
                        }
                    }
                }
            }
            
            if (sharedHitTrack) {
                trackPlots.get("doca - shared strip hit").fill(TrackUtils.getDoca(track));
                trackPlots.get("z0 - shared strip hit").fill(TrackUtils.getZ0(track));
                trackPlots.get("sin(phi0) - shared strip hit").fill(TrackUtils.getPhi0(track));
                trackPlots.get("curvature - shared strip hit").fill(TrackUtils.getR(track));
                trackPlots.get("tan_lambda - shared strip hit").fill(TrackUtils.getTanLambda(track));
                trackPlots.get("chi2 - shared strip hit").fill(track.getChi2());
            } else { 
                trackPlots.get("doca").fill(TrackUtils.getDoca(track));
                trackPlots.get("z0").fill(TrackUtils.getZ0(track));
                trackPlots.get("sin(phi0)").fill(TrackUtils.getPhi0(track));
                trackPlots.get("curvature").fill(TrackUtils.getR(track));
                trackPlots.get("tan_lambda").fill(TrackUtils.getTanLambda(track));
                trackPlots.get("chi2").fill(track.getChi2());
            }
            
            if (l1Isolation) { 
                trackPlots.get("doca - l1 Isolation").fill(TrackUtils.getDoca(track));
                trackPlots.get("z0 - l1 Isolation").fill(TrackUtils.getZ0(track));
                trackPlots.get("sin(phi0) - l1 Isolation").fill(TrackUtils.getPhi0(track));
                trackPlots.get("curvature - l1 Isolation").fill(TrackUtils.getR(track));
                trackPlots.get("tan_lambda - l1 Isolation").fill(TrackUtils.getTanLambda(track));
                trackPlots.get("chi2 - l1 Isolation").fill(track.getChi2());
            }
        }
    }
    
    private void mapStereoHits(List<TrackerHit> stereoHits) { 
       
        for (int layer = 1; layer <= 6; layer++) { 
            topLayerToStereoHit.get(layer).clear();
            bottomLayerToStereoHit.get(layer).clear();;
        }
        
        for (TrackerHit stereoHit : stereoHits) {
            HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) stereoHit.getRawHits().get(0)).getDetectorElement();
            int layer = (sensor.getLayerNumber() + 1)/2;
            if (sensor.isTopLayer()) {
                topLayerToStereoHit.get(layer).add(stereoHit);
            } else { 
                bottomLayerToStereoHit.get(layer).add(stereoHit);
            }
        }        
    }
    
    private List<TrackerHit> getTopLayerStereoHits(int layer) { 
        return topLayerToStereoHit.get(layer);
    }
    
    private List<TrackerHit> getBottomLayerStereoHits(int layer) { 
        return bottomLayerToStereoHit.get(layer);
    }
}
