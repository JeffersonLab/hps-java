package org.hps.users.omoreno;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Driver to find the raw hit correlation between layers.
 */
public class SvtHitCorrelations extends Driver {

    // TODO: Add documentation
    static { 
        hep.aida.jfree.AnalysisFactory.register();
    }
   
    // Plotting
    ITree tree; 
    IHistogramFactory histogramFactory;
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
   
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    protected Map<String, IHistogram2D> topAxialAxialPlots = new HashMap<String, IHistogram2D>();
    protected Map<String, IHistogram2D> topAxialStereoPlots = new HashMap<String, IHistogram2D>();
    protected Map<String, IHistogram2D> bottomAxialAxialPlots = new HashMap<String, IHistogram2D>();
    protected Map<String, IHistogram2D> bottomAxialStereoPlots = new HashMap<String, IHistogram2D>();
    
    private List<HpsSiSensor> sensors;
    
    // Collection Names
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";

    // Detector Name
    private static final String SUBDETECTOR_NAME = "Tracker";
    
    private int runNumber = -1; 
    
    boolean enableTopAxialAxial = false;
    boolean enableTopAxialStereo = false;
    boolean enableBottomAxialAxial = false;
    boolean enableBottomAxialStereo = false;
    
    /**
     * 
     */
    public void setEnableTopAxialAxial(boolean enableTopAxialAxial){
        this.enableTopAxialAxial = enableTopAxialAxial;
    }

    /**
     * 
     */
    public void setEnableTopAxialStereo(boolean enableTopAxialStereo){
        this.enableTopAxialStereo = enableTopAxialStereo; 
    }
    
    /**
     * 
     */
    public void setEnableBottomAxialAxial(boolean enableBottomAxialAxial){
        this.enableBottomAxialAxial = enableBottomAxialAxial;
    }
    
    /**
     * 
     */
    public void setEnableBottomAxialStereo(boolean enableBottomAxialStereo){
        this.enableBottomAxialStereo = enableBottomAxialStereo; 
    }
    
    /**
     * 
     */
    private int computePlotterRegion(HpsSiSensor firstSensor, HpsSiSensor secondSensor) {
        return (this.getLayerNumber(firstSensor) - 1) + (this.getLayerNumber(secondSensor) - 1)*6;    
    }
    
    protected void detectorChanged(Detector detector){
       
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
    
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        if (sensors.size() == 0) {
            throw new RuntimeException("There are no sensors associated with this detector");
        }
      
        if (enableTopAxialAxial) { 
            plotters.put("Top Axial vs Axial Channel Correlations", 
                    plotterFactory.create("Top Axial vs Axial Channel Correlation"));
            plotters.get("Top Axial vs Axial Channel Correlations").createRegions(6, 6);
        }
        
        if (enableTopAxialStereo) { 
            plotters.put("Top Axial vs Stereo Channel Correlations", 
                    plotterFactory.create("Top Axial vs Stereo Channel Correlation"));
            plotters.get("Top Axial vs Stereo Channel Correlations").createRegions(6, 6);
        }
        
        if (enableBottomAxialAxial) { 
            plotters.put("Bottom Axial vs Axial Channel Correlations", 
                    plotterFactory.create("Bottom Axial vs Axial Channel Correlation"));
            plotters.get("Bottom Axial vs Axial Channel Correlations").createRegions(6, 6);
        }
        
        if (enableBottomAxialStereo) { 
            plotters.put("Bottom Axial vs Stereo Channel Correlations", 
                    plotterFactory.create("Bottom Axial vs Stereo Channel Correlation"));
            plotters.get("Bottom Axial vs Stereo Channel Correlations").createRegions(6, 6);
        }
       
        String plotName = "";
        for (HpsSiSensor firstSensor : sensors) { 
            for (HpsSiSensor secondSensor : sensors ) { 
                
                if (firstSensor.isTopLayer() && secondSensor.isTopLayer()) {
                    if (enableTopAxialAxial && firstSensor.isAxial() && secondSensor.isAxial()) { 
                        plotName = "Top Axial Layer " + this.getLayerNumber(firstSensor) 
                                + " vs Top Axial Layer " + this.getLayerNumber(secondSensor); 
                        topAxialAxialPlots.put(plotName, histogramFactory.createHistogram2D(plotName, 160, 0, 639, 160, 0, 639));
                        plotters.get("Top Axial vs Axial Channel Correlations")
                                .region(this.computePlotterRegion(firstSensor, secondSensor))
                                .plot(topAxialAxialPlots.get(plotName), 
                                        this.createStyle("Top Axial Layer " + this.getLayerNumber(firstSensor),
                                                "Top Axial Layer " + this.getLayerNumber(secondSensor)));
                    } else if (enableTopAxialStereo && firstSensor.isAxial() && secondSensor.isStereo()) { 
                        plotName = "Top Axial Layer " + this.getLayerNumber(firstSensor) 
                                + " vs Top Stereo Layer " + this.getLayerNumber(secondSensor); 
                        topAxialStereoPlots.put(plotName, histogramFactory.createHistogram2D(plotName, 160, 0, 639, 160, 0, 639));
                        plotters.get("Top Axial vs Stereo Channel Correlations")
                                .region(this.computePlotterRegion(firstSensor, secondSensor))
                                .plot(topAxialStereoPlots.get(plotName), 
                                        this.createStyle("Top Axial Layer " + this.getLayerNumber(firstSensor),
                                                "Top Stereo Layer " + this.getLayerNumber(secondSensor)));
                    }
                } else if (firstSensor.isBottomLayer() && secondSensor.isBottomLayer()) { 
                    if (enableBottomAxialAxial && firstSensor.isAxial() && secondSensor.isAxial()) { 
                        plotName = "Bottom Axial Layer " + this.getLayerNumber(firstSensor) 
                                + " vs Bottom Axial Layer " + this.getLayerNumber(secondSensor); 
                        bottomAxialAxialPlots.put(plotName, histogramFactory.createHistogram2D(plotName, 160, 0, 639, 160, 0, 639));
                        plotters.get("Bottom Axial vs Axial Channel Correlations")
                                .region(this.computePlotterRegion(firstSensor, secondSensor))
                                .plot(bottomAxialAxialPlots.get(plotName), 
                                        this.createStyle("Bottom Axial Layer " + this.getLayerNumber(firstSensor),
                                                "Bottom Axial Layer " + this.getLayerNumber(secondSensor)));
                    } else if (enableBottomAxialStereo && firstSensor.isAxial() && secondSensor.isStereo()) { 
                        plotName = "Bottom Axial Layer " + this.getLayerNumber(firstSensor) 
                                + " vs Bottom Stereo Layer " + this.getLayerNumber(secondSensor); 
                        bottomAxialStereoPlots.put(plotName, histogramFactory.createHistogram2D(plotName, 160, 0, 639, 160, 0, 639));
                        plotters.get("Bottom Axial vs Stereo Channel Correlations")
                                .region(this.computePlotterRegion(firstSensor, secondSensor))
                                .plot(bottomAxialStereoPlots.get(plotName), 
                                        this.createStyle("Bottom Axial Layer " + this.getLayerNumber(firstSensor),
                                                "Bottom Stereo Layer " + this.getLayerNumber(secondSensor)));
                    }
                }
            }
        }

        for (IPlotter plotter : plotters.values()) plotter.show();
    }
    
    public void process(EventHeader event){
    
        if (runNumber == -1) runNumber = event.getRunNumber();
        
        if(!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) return;
    
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        
        String plotName = "";
        for(RawTrackerHit firstRawHit : rawHits){
    
            HpsSiSensor firstSensor = (HpsSiSensor) firstRawHit.getDetectorElement();
            int firstChannel = firstRawHit.getIdentifierFieldValue("strip");
            
            for(RawTrackerHit secondRawHit : rawHits){
            
                HpsSiSensor secondSensor = (HpsSiSensor) secondRawHit.getDetectorElement();
                int secondChannel = secondRawHit.getIdentifierFieldValue("strip");
            
                if(firstSensor.isTopLayer() && secondSensor.isTopLayer()){
                    if(enableTopAxialAxial && firstSensor.isAxial() && secondSensor.isAxial()){
                        
                        plotName = "Top Axial Layer " + this.getLayerNumber(firstSensor) 
                                    + " vs Top Axial Layer " + this.getLayerNumber(secondSensor); 
                        topAxialAxialPlots.get(plotName).fill(firstChannel, secondChannel);
                    } else if (enableTopAxialStereo && firstSensor.isAxial() && secondSensor.isStereo()) { 
                        
                        plotName = "Top Axial Layer " + this.getLayerNumber(firstSensor) 
                                    + " vs Top Stereo Layer " + this.getLayerNumber(secondSensor); 
                        topAxialStereoPlots.get(plotName).fill(firstChannel, secondChannel);
                    }
                } else if (firstSensor.isBottomLayer() && secondSensor.isBottomLayer()) { 
                    if(enableBottomAxialAxial && firstSensor.isAxial() && secondSensor.isAxial()){
                        
                        plotName = "Bottom Axial Layer " + this.getLayerNumber(firstSensor) 
                                    + " vs Bottom Axial Layer " + this.getLayerNumber(secondSensor); 
                        bottomAxialAxialPlots.get(plotName).fill(firstChannel, secondChannel);
                    } else if (enableBottomAxialStereo && firstSensor.isAxial() && secondSensor.isStereo()) { 
                        
                        plotName = "Bottom Axial Layer " + this.getLayerNumber(firstSensor) 
                                    + " vs Bottom Stereo Layer " + this.getLayerNumber(secondSensor); 
                        bottomAxialStereoPlots.get(plotName).fill(firstChannel, secondChannel);
                    }
                }
            }
        }
    }

    public void endOfData() { 
      
        String rootFile = "run" + runNumber + "_svt_hit_correlations.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    private int getLayerNumber(HpsSiSensor sensor) {
       return (int) Math.ceil(((double) sensor.getLayerNumber())/2); 
    }
    
    IPlotterStyle createStyle(String xAxisTitle, String yAxisTitle) { 
       
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
        
        // Set the z axis to log scale
        style.zAxisStyle().setScaling("log");
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(false);
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(true);
       
        return style;
    }
    
}
