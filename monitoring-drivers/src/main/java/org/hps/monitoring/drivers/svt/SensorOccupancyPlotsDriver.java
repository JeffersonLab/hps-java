package org.hps.monitoring.drivers.svt;


import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.rootwriter.RootFileStore;

import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This Driver makes plots of sensor occupancies across a run. It is intended to
 * be used with the monitoring system.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SensorOccupancyPlotsDriver extends Driver {

    // TODO: Add documentation
   static {
        hep.aida.jfree.AnalysisFactory.register();
    } 
   
    ITree tree; 
    IHistogramFactory histogramFactory;
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();

    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    protected Map<HpsSiSensor, IHistogram1D> occupancyPlots = new HashMap<HpsSiSensor, IHistogram1D>();
    protected Map<HpsSiSensor, int[]> occupancyMap = new HashMap<HpsSiSensor, int[]>();
    private List<HpsSiSensor> sensors;

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
    String rootFile = null;

    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1; 
    
    public SensorOccupancyPlotsDriver() {
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
  
    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 6 * (sensor.getLayerNumber() - 1);
            } else {
                return 6 * (sensor.getLayerNumber() - 1) + 1;
            }
        } else {

            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 2;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (sensor.getLayerNumber() - 7) + 4;
                } else {
                    return 6 * (sensor.getLayerNumber() - 7) + 5;
                }
            }
        }

        return -1;
    }
    
    protected void detectorChanged(Detector detector) {

        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);

        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        if (sensors.size() == 0) {
            throw new RuntimeException("There are no sensors associated with this detector");
        }

        plotters.put("Occupancy", plotterFactory.create("Occupancy"));
        plotters.get("Occupancy").createRegions(6, 6);

        for (HpsSiSensor sensor : sensors) {
            occupancyPlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy", 640, 0, 640));
            plotters.get("Occupancy").region(this.computePlotterRegion(sensor))
                                     .plot(occupancyPlots.get(sensor), this.createOccupancyPlotStyle(sensor));
            occupancyMap.put(sensor, new int[640]);
        }

        for (IPlotter plotter : plotters.values()) {
            for (int regionN = 0; regionN < 36; regionN++) { 
                PlotterRegion region = ((PlotterRegion) ((Plotter) plotter).region(regionN));
                region.getPanel().addMouseListener(new PopupPlotterListener(region));
            }
            plotter.show();
        }
    }

    public void process(EventHeader event) {

        if (runNumber == -1) runNumber = event.getRunNumber();
        
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName))
            return;

        eventCount++;

        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        // Increment strip hit count.
        for (RawTrackerHit rawHit : rawHits) {
            occupancyMap.get((HpsSiSensor) rawHit.getDetectorElement())[rawHit.getIdentifierFieldValue("strip")]++;
        }

        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0) {
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(sensor);
                occupancyPlots.get(sensor).reset();
                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;
                    occupancyPlots.get(sensor).fill(channel, stripOccupancy);
                }
            }
        }
    }
    
    public void endOfData() { 
      
        rootFile = "run" + runNumber + "_occupancy.root";
        RootFileStore store = new RootFileStore(rootFile);
        try {
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    IPlotterStyle createOccupancyPlotStyle(HpsSiSensor sensor) {
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel("Channel");
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);
        
        // Set the style of the Y axis
        style.yAxisStyle().setLabel("Occupancy");
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(true);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.10);
        if (sensor.isTopLayer()) { 
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else { 
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        style.regionBoxStyle().backgroundStyle().setOpacity(.10);
        if (sensor.isAxial()) style.regionBoxStyle().backgroundStyle().setColor("229, 114, 31, 1");
        
        return style;
    }
}
