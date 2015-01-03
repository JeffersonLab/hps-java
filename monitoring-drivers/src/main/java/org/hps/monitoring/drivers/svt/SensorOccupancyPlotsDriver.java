package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.PlotterRegion;
import jas.hist.JASHist;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver makes plots of sensor occupancies across a run. It is intended to
 * be used with the monitoring system. It will currently only run on a test run
 * detector, as the number of sensors, and hence plotter regions, is hardcoded
 * to 20.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: SensorOccupancyPlotsDriver.java,v 1.8 2013/11/06 19:19:55 jeremy Exp $
 *
 */
public class SensorOccupancyPlotsDriver extends Driver {

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String trackerName = "Tracker";
    private AIDA aida = AIDA.defaultInstance();
    private IPlotter plotter;
    private Detector detector;
    private List<SiSensor> sensors;
    private Map<String, int[]> occupancyMap;
    private Map<String, Integer> sensorRegionMap;
    private int eventCount = 0;
    private int eventRefreshRate = 1000;
    private static final String nameStrip = "Tracker_TestRunModule_";
    private static final int maxChannels = 640;

    public SensorOccupancyPlotsDriver() {
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    private int computePlotterRegion(SiSensor sensor) {

        IIdentifierHelper helper = sensor.getIdentifierHelper();
        IIdentifier id = sensor.getIdentifier();

        int layer = helper.getValue(id, "layer"); // 1-10; axial layers are odd layers; stereo layers are even
        int module = helper.getValue(id, "module"); // 0-1; module number is top or bottom

        // Compute the sensor's x and y grid coordinates and then translate to region number.
        int ix = (layer - 1) / 2;
        int iy = 0;
        if (module > 0) {
            iy += 2;
        }
        if (layer % 2 == 0) {
            iy += 1;
        }
        int region = ix * 4 + iy;
        //System.out.println(sensor.getName() + "; lyr=" + layer + "; mod=" + module + " -> xy[" + ix + "][" + iy + "] -> reg="+region);
        return region;
    }

    protected void detectorChanged(Detector detector) {

        // Setup the plotter.
        IAnalysisFactory fac = aida.analysisFactory();
        plotter = fac.createPlotterFactory("SVT").create("Sensor Occupancy Plots");
        IPlotterStyle pstyle = plotter.style();
        pstyle.dataStyle().fillStyle().setColor("green");
        //pstyle.dataStyle().markerStyle().setColor("green");
        pstyle.dataStyle().markerStyle().setVisible(false);
        pstyle.dataStyle().outlineStyle().setVisible(false);
        pstyle.dataStyle().errorBarStyle().setVisible(false);
        pstyle.statisticsBoxStyle().setVisible(false);

        // Create regions.
        plotter.createRegions(5, 4);

        // Cache Detector object.
        this.detector = detector;

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Reset the data structure that keeps track of strip occupancies.
        resetOccupancyMap();

        // For now throw an error if there are "too many" sensors.
        if (sensors.size() > 20) {
            throw new RuntimeException("Can't handle > 20 sensors at a time.");
        }

        // Map a map of sensors to their region numbers in the plotter.
        sensorRegionMap = new HashMap<String, Integer>();
        for (SiSensor sensor : sensors) {
            int region = computePlotterRegion(sensor);
            sensorRegionMap.put(sensor.getName(), region);
        }

        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (SiSensor sensor : sensors) {
        	//IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D occupancyPlot = createSensorPlot(sensor);
            occupancyPlot.reset();
            int region = sensorRegionMap.get(sensor.getName());
            plotter.region(region).plot(occupancyPlot);
            JASHist hist = ((PlotterRegion) plotter.region(region)).getPlot();
            hist.setAllowUserInteraction(false);
            hist.setAllowPopupMenus(false);
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {

            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);

            // Increment strip hit count.
            for (RawTrackerHit hit : rawTrackerHits) {
                int[] strips = occupancyMap.get(hit.getDetectorElement().getName());
                strips[hit.getIdentifierFieldValue("strip")] += 1;
            }

            // Plot strip occupancies.
            if (eventCount % eventRefreshRate == 0) {
                for (SiSensor sensor : sensors) {
                	//IHistogram1D sensorHist = aida.histogram1D(sensor.getName());
                    IHistogram1D sensorHist = getSensorPlot(sensor);
                    sensorHist.reset();
                    int[] strips = occupancyMap.get(sensor.getName());
                    for (int i = 0; i < strips.length; i++) {
                        double stripOccupancy = (double) strips[i] / (double) (eventCount);
                        if (stripOccupancy != 0) {
                            sensorHist.fill(i, stripOccupancy);
                        }
                    }
                }
            }

            // Increment event counter.
            ++eventCount;
        }
    }
    
    private IHistogram1D getSensorPlot(SiSensor sensor) {
    	return aida.histogram1D(sensor.getName());
    }
    
    private IHistogram1D createSensorPlot(SiSensor sensor) {
    	IHistogram1D hist = aida.histogram1D(sensor.getName(), maxChannels, 0, maxChannels-1);
    	hist.setTitle(sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens"));
    	return hist;
    }

    private void resetOccupancyMap() {
        occupancyMap = new HashMap<String, int[]>();
        for (SiSensor sensor : sensors) {
            occupancyMap.put(sensor.getName(), new int[640]);
        }
    }
}
