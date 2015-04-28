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
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.rootwriter.RootFileStore;
import hep.physics.vec.Hep3Vector;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.ITransform3D;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This Driver makes plots of SVT sensor occupancies across a run.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SensorOccupancyPlotsDriver extends Driver {

    // TODO: Add documentation
   static {
        hep.aida.jfree.AnalysisFactory.register();
    } 
 
    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = IAnalysisFactory.create();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();
    private IHistogramFactory histogramFactory = null;

    // Histogram maps
    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, IHistogram1D> positionPlots = new HashMap<String, IHistogram1D>();
    private static Map<String, int[]> occupancyMap = new HashMap<String, int[]>();
    private static Map<String, IHistogram1D> maxSamplePositionPlots = new HashMap<String, IHistogram1D>(); 

    private List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, Map<Integer, Hep3Vector>> stripPositions = new HashMap<HpsSiSensor, Map<Integer, Hep3Vector>>(); 

    private static final String SUBDETECTOR_NAME = "Tracker";
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    
    String rootFile = null;

    private int maxSamplePosition = -1;
    private int timeWindowWeight = 1; 
    private int eventCount = 0;
    private int eventRefreshRate = 1;
    private int runNumber = -1;
    
    private boolean enablePositionPlots = false;
    private boolean enableMaxSamplePlots = false;
    
    public SensorOccupancyPlotsDriver() {
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }
 
    public void setEnablePositionPlots(boolean enablePositionPlots) { 
        this.enablePositionPlots = enablePositionPlots; 
    }
    
    public void setEnableMaxSamplePlots(boolean enableMaxSamplePlots) { 
        this.enableMaxSamplePlots = enableMaxSamplePlots;
    }
    
    public void setMaxSamplePosition(int maxSamplePosition) { 
        this.maxSamplePosition = maxSamplePosition;
    }
   
    public void setTimeWindowWeight(int timeWindowWeight) { 
       this.timeWindowWeight = timeWindowWeight;   
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

    /**
     *  Get the global strip position of a physical channel number for a given
     *  sensor.
     *  
     *  @param sensor : HpsSiSensor 
     *  @param physicalChannel : physical channel number 
     *  @return The strip position (mm) in the global coordinate system
     */
    private Hep3Vector getStripPosition(HpsSiSensor sensor, int physicalChannel){ 
        return stripPositions.get(sensor).get(physicalChannel);
    }
   
    /**
     *  For each sensor, create a mapping between a physical channel number and
     *  it's global strip position.
     */
    // TODO: Move this to a utility class
    private void createStripPositionMap() { 

        for(ChargeCarrier carrier : ChargeCarrier.values()){
            for(HpsSiSensor sensor : sensors){ 
                if(sensor.hasElectrodesOnSide(carrier)){ 
                    stripPositions.put(sensor, new HashMap<Integer, Hep3Vector>());
                    SiStrips strips = (SiStrips) sensor.getReadoutElectrodes(carrier);     
                    ITransform3D parentToLocal = sensor.getReadoutElectrodes(carrier).getParentToLocal();
                    ITransform3D localToGlobal = sensor.getReadoutElectrodes(carrier).getLocalToGlobal();
                    for(int physicalChannel = 0; physicalChannel < 640; physicalChannel++){
                        Hep3Vector localStripPosition = strips.getCellPosition(physicalChannel);
                        Hep3Vector stripPosition = parentToLocal.transformed(localStripPosition);
                        Hep3Vector globalStripPosition = localToGlobal.transformed(stripPosition);
                        stripPositions.get(sensor).put(physicalChannel, globalStripPosition);
                    }
                }
            }
        }
    }

    /**
     *  Create a plotter style.
     * 
     * @param xAxisTitle : Title of the x axis
     * @param sensor : HpsSiSensor associated with the plot.  This is used to
     *                 set certain attributes based on the position of the 
     *                 sensor.
     * @return plotter style
     */
    // TODO: Move this to a utilities class
    IPlotterStyle createOccupancyPlotStyle(String xAxisTitle, HpsSiSensor sensor) {
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
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
        style.dataStyle().outlineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.30);
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
       
        style.regionBoxStyle().backgroundStyle().setOpacity(.20);
        if (sensor.isAxial()) style.regionBoxStyle().backgroundStyle().setColor("246, 246, 34, 1");
        
        return style;
    }

    /**
     *  Clear all histograms of it's current data.
     */
    private void resetPlots() { 
      
        // Clear the hit counter map of all previously stored data. 
        occupancyMap.clear();
        
        // Since all plots are mapped to the name of a sensor, loop 
        // through the sensors, get the corresponding plots and clear them.
        for (HpsSiSensor sensor : sensors) { 

            // Clear the occupancy plots.
            occupancyPlots.get(sensor.getName()).reset();
            
            if (enablePositionPlots) { 
                positionPlots.get(sensor.getName()).reset();
            }
            
            if (enableMaxSamplePlots) { 
                maxSamplePositionPlots.get(sensor.getName()).reset();
            }
            
            // Reset the hit counters.
            occupancyMap.put(sensor.getName(), new int[640]);
        }
    }
    
    protected void detectorChanged(Detector detector) {

        // Get the HpsSiSensor objects from the geometry
        sensors = detector.getSubdetector(SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);

        // If there were no sensors found, throw an exception
        if (sensors.size() == 0) {
            throw new RuntimeException("There are no sensors associated with this detector");
        }
        
        // For each sensor, create a mapping between a physical channel number
        // and the global strip position
        this.createStripPositionMap();
        
        // If the tree already exist, clear all existing plots of any old data
        // they might contain.
        if (tree != null) { 
            this.resetPlots();
            return; 
        }
       
        tree = analysisFactory.createTreeFactory().create();
        histogramFactory = analysisFactory.createHistogramFactory(tree);

        // Create the plotter and regions.  A region is created for each
        // sensor for a total of 36.
        plotters.put("Occupancy", plotterFactory.create("Occupancy"));
        plotters.get("Occupancy").createRegions(6, 6);

        if (enablePositionPlots) { 
            plotters.put("Occupancy vs Position", plotterFactory.create("Occupancy vs Position"));
            plotters.get("Occupancy vs Position").createRegions(6, 6);
        }
       
        if (enableMaxSamplePlots) { 
            plotters.put("Max Sample Number", plotterFactory.create("Max Sample Number"));
            plotters.get("Max Sample Number").createRegions(6, 6);
        }
        
        for (HpsSiSensor sensor : sensors) {
            occupancyPlots.put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy", 640, 0, 640));
            plotters.get("Occupancy").region(this.computePlotterRegion(sensor))
                                     .plot(occupancyPlots.get(sensor.getName()), this.createOccupancyPlotStyle("Physical Channel", sensor));
        
            if (enablePositionPlots) {
                if (sensor.isTopLayer()) {
                    positionPlots.put(sensor.getName(), 
                            histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy vs Position", 1000, 0, 60));
                } else { 
                    positionPlots.put(sensor.getName(), 
                            histogramFactory.createHistogram1D(sensor.getName() + " - Occupancy vs Position", 1000, -60, 0));
                }
                
                plotters.get("Occupancy vs Position").region(this.computePlotterRegion(sensor))
                                                     .plot(positionPlots.get(sensor.getName()), this.createOccupancyPlotStyle("Distance from Beam [mm]", sensor));
            }
            occupancyMap.put(sensor.getName(), new int[640]);
        
            if (enableMaxSamplePlots) { 
                maxSamplePositionPlots.put(sensor.getName(), histogramFactory.createHistogram1D(sensor.getName() + " - Max Sample Number", 6, 0, 6));
                plotters.get("Max Sample Number").region(this.computePlotterRegion(sensor))
                                                 .plot(maxSamplePositionPlots.get(sensor.getName()),
                                                         this.createOccupancyPlotStyle("Max Sample Number", sensor));
            }
        }
        
        for (IPlotter plotter : plotters.values()) {
            for (int regionN = 0; regionN < plotter.numberOfRegions(); regionN++) { 
                PlotterRegion region = ((PlotterRegion) ((Plotter) plotter).region(regionN));
			    if (region.getPlottedObjects().size() == 0) continue;
                region.getPanel().addMouseListener(new PopupPlotterListener(region));
            }
            plotter.show();
        }
    }

    public void process(EventHeader event) {

        // Get the run number from the event and store it.  This will be used 
        // when writing the plots out to a ROOT file
        if (runNumber == -1) runNumber = event.getRunNumber();
       
        // If the event doesn't have a collection of RawTrackerHit's, skip it.
        if (!event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) return;
        // Get RawTrackerHit collection from event.
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

        eventCount++;
        // Increment strip hit count.
        for (RawTrackerHit rawHit : rawHits) {
            
            // Obtain the raw ADC samples for each of the six samples readout
            short[] adcValues = rawHit.getADCValues();
            
            // Find the sample that has the largest amplitude.  This should
            // correspond to the peak of the shaper signal if the SVT is timed
            // in correctly.  Otherwise, the maximum sample value will default 
            // to 0.
            int maxAmplitude = 0;
            int maxSamplePositionFound = -1;
            for (int sampleN = 0; sampleN < 6; sampleN++) { 
                if (adcValues[sampleN] > maxAmplitude) { 
                    maxAmplitude = adcValues[sampleN];
                    maxSamplePositionFound = sampleN; 
                }
            }
           
            if (maxSamplePosition == -1 || maxSamplePosition == maxSamplePositionFound) { 
                occupancyMap.get(((HpsSiSensor) rawHit.getDetectorElement()).getName())[rawHit.getIdentifierFieldValue("strip")]++;
            }
            
            if (enableMaxSamplePlots) { 
                maxSamplePositionPlots.get(((HpsSiSensor) rawHit.getDetectorElement()).getName()).fill(maxSamplePositionFound);
            }
        }

        // Plot strip occupancies.
        if (eventCount % eventRefreshRate == 0) {
            for (HpsSiSensor sensor : sensors) {
                int[] strips = occupancyMap.get(sensor.getName());
                occupancyPlots.get(sensor.getName()).reset();
                if (enablePositionPlots) positionPlots.get(sensor.getName()).reset();
                for (int channel = 0; channel < strips.length; channel++) {
                    double stripOccupancy = (double) strips[channel] / (double) eventCount;
                    stripOccupancy /= this.timeWindowWeight;
                    occupancyPlots.get(sensor.getName()).fill(channel, stripOccupancy);
              
                    if (enablePositionPlots) {
                        double stripPosition = this.getStripPosition(sensor, channel).y();
                        positionPlots.get(sensor.getName()).fill(stripPosition, stripOccupancy);
                    }
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
}
