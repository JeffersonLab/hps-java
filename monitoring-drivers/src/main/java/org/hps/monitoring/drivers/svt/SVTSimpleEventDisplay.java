package org.hps.monitoring.drivers.svt;

import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
//import hep.aida.jfree.plot.style.DefaultHistogram1DStyle;



import java.util.List;



//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.util.Resettable;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Plots hit counts for all SVT channels at each stage of reconstruction.
 * This class can be configured to reset after each event for use as an
 * event display by calling {@link #setSingleEvent(boolean)}.
 * 
 */
public class SVTSimpleEventDisplay extends Driver implements Resettable {

    /*
     * Names of collections.
     */
    private String inputCollection = "SVTRawTrackerHits";
    private String trackerHitCollection = "StripClusterer_SiTrackerHitStrip1D";
    private String helicalHitCollection = "HelicalTrackHits";
    
    /* 
     * Reference to AIDA utility.
     */
    private AIDA aida = AIDA.defaultInstance();
    
    /*
     * AIDA objects that will be setup during initialization.
     */
    private IPlotter plotter, plotter2, plotter3, plotter4;
    private IHistogram1D[][] rth = new IHistogram1D[2][10];
    private IHistogram1D[][] th = new IHistogram1D[2][10];
    private IHistogram1D[][] hth = new IHistogram1D[2][10];
    private IHistogram1D hitCount[] = new IHistogram1D[2];
    private IPlotterFactory factory;
    
    /*
     * Single event mode setting.
     */
    private boolean singleEvent = true;

    /*
     * Subdetector Name 
     */
    private static final String subdetectorName = "Tracker";

    /**
     * Class constructor.
     */
    public SVTSimpleEventDisplay() {
    }

    /**
     * Set the name of the HelicalTrackHit collection.
     * @param helicalHitCollection The name of the HelicalTrackHit collection.
     */
    public void setHelicalHitCollection(String helicalHitCollection) {
        this.helicalHitCollection = helicalHitCollection;
    }

    /**
     * Set this Driver to reset after each event.
     * @param singleEvent Set to true if Driver should reset after each event.
     */
    public void setSingleEvent(boolean singleEvent) {
        this.singleEvent = singleEvent;
    }

    /**
     * Set the RawTrackerHit collection name.
     * @param inputCollection The name of the RawTrackerHit collection.
     * FIXME: This method should really be called setRawTrackerHitCollection instead.
     */
    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    /**
     * Set the name of the TrackerHit collection.
     * @param trackerHitCollection The name of the TrackerHit collection.
     */
    public void setTrackerHitCollection(String trackerHitCollection) {
        this.trackerHitCollection = trackerHitCollection;
    }

    /**
     * Get the plotter region index from a layer and module number of a sensor.
     * @param layer The sensor's layer number.
     * @param module The sensor's module number.
     * @return The index of the plotter region for the layer and module.
     */
    private int computePlotterRegion(int layer, int module) {
        // Compute the sensor's x and y grid coordinates and then translate to region number.
        int iy = (layer - 1) / 2;
        int ix = 0;
        if (module > 0) {
            ix += 2;
        }
        if (layer % 2 == 0) {
            ix += 1;
        }
        int region = ix * 5 + iy;
        //System.out.println(sensor.getName() + "; lyr=" + layer + "; mod=" + module + " -> xy[" + ix + "][" + iy + "] -> reg="+region);
        return region;
    }

    /**
     * Configure this Driver for a new Detector, e.g. setup the plots and show them.
     */
    protected void detectorChanged(Detector detector) {
        List<HpsSiSensor> sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        createPlotterFactory();
        setupRawTrackerHitPlots();
        setupTrackerHitPlots();
        setupHelicalTrackHitPlots();
        setupHitCountPlots();
        setupOccupancyPlots(sensors);
        showPlots();
    }

    /**
     * Create the PlotterFactory.
     */
    private void createPlotterFactory() {
        factory = aida.analysisFactory().createPlotterFactory("SVT Event Display");
    }

    private void setupHitCountPlots() {
        plotter4 = factory.create("Hit Counts");
        plotter4.setTitle("Hit Counts");
        //plotter4.setStyle(new DefaultHistogram1DStyle());
        plotter4.style().dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(1, 2);
        
        hitCount[0] = aida.histogram1D("Hit layers in top", 6, -0.5, 5.5);
        plotter4.region(0).plot(hitCount[0]);
        hitCount[1] = aida.histogram1D("Hit layers in bottom", 6, -0.5, 5.5);
        plotter4.region(1).plot(hitCount[1]);
    }

    private void setupHelicalTrackHitPlots() {
        plotter3 = factory.create("HelicalTrackHits");
        plotter3.setTitle("HelicalTrackHits");
        //plotter3.setStyle(new DefaultHistogram1DStyle());
        plotter3.style().dataStyle().errorBarStyle().setVisible(false);
        plotter3.style().statisticsBoxStyle().setVisible(false);
        plotter3.createRegions(4, 5);
    }

    private void setupTrackerHitPlots() {
        plotter2 = factory.create("TrackerHits");
        plotter2.setTitle("TrackerHits");
        //plotter2.setStyle(new DefaultHistogram1DStyle());
        plotter2.style().dataStyle().errorBarStyle().setVisible(false);
        plotter2.style().statisticsBoxStyle().setVisible(false);
        plotter2.createRegions(4, 5);
    }

    private void setupRawTrackerHitPlots() {
        plotter = factory.create("RawTrackerHits");
        plotter.setTitle("RawTrackerHits");
        //plotter.setStyle(new DefaultHistogram1DStyle());
        plotter.style().dataStyle().errorBarStyle().setVisible(false);
        plotter.style().statisticsBoxStyle().setVisible(false);
        plotter.createRegions(4, 5);
    }

    private void showPlots() {
        plotter.show();
        plotter2.show();
        plotter3.show();
        plotter4.show();
    }

    private void setupOccupancyPlots(List<HpsSiSensor> sensors) {
        aida.tree().cd("/");
        
        for(HpsSiSensor sensor : sensors){
        	int module = sensor.getModuleNumber();
        	int layer = sensor.getLayerNumber(); 
            int region = computePlotterRegion(layer + 1, module);
            rth[module][layer] = aida.histogram1D(sensor.getName() + " RawTrackerHits", 640, -0.5, 639.5);
            plotter.region(region).plot(rth[module][layer]);
            th[module][layer] = aida.histogram1D(sensor.getName() + " TrackerHits", 640, -0.5, 639.5);
            plotter2.region(region).plot(th[module][layer]);
            hth[module][layer] = aida.histogram1D(sensor.getName() + " HelicalTrackHits", 640, -0.5, 639.5);
            plotter3.region(region).plot(hth[module][layer]);
        }
  
        /* ===> for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                int region = computePlotterRegion(layer + 1, module);
                rth[module][layer] = aida.histogram1D(SvtUtils.getInstance().getSensor(module, layer).getName() + " RawTrackerHits", 640, -0.5, 639.5);
                plotter.region(region).plot(rth[module][layer]);
                th[module][layer] = aida.histogram1D(SvtUtils.getInstance().getSensor(module, layer).getName() + " TrackerHits", 640, -0.5, 639.5);
                plotter2.region(region).plot(th[module][layer]);
                hth[module][layer] = aida.histogram1D(SvtUtils.getInstance().getSensor(module, layer).getName() + " HelicalTrackHits", 640, -0.5, 639.5);
                plotter3.region(region).plot(hth[module][layer]);
            }
        } ===> */
    }

    /**
     * Process a single event by filling histograms with event data.
     * @param event The current event.
     */
    public void process(EventHeader event) {

        // Clear histograms if in single event mode.
        if (singleEvent) {
            resetPlots();
        }

        plotRawTrackerHits(event);

        plotTrackerHits(event);

        plotHelicalTrackHits(event);
    }

    /**
     * Fill HelicalTrackHit plots for one event.
     * @param event The event.
     */
    private void plotHelicalTrackHits(EventHeader event) {
        if (event.hasCollection(HelicalTrackHit.class, helicalHitCollection)) {
            
            List<HelicalTrackHit> helicalTrackerHits = event.get(HelicalTrackHit.class, helicalHitCollection);

            System.out.println(helicalHitCollection + " has " + helicalTrackerHits.size() + " hits");
            
            boolean[][] hasHit = new boolean[2][5];

            // Increment strip hit count.
            for (HelicalTrackHit hit : helicalTrackerHits) {
                for (Object rawHit : hit.getRawHits()) {
                    int layer = ((RawTrackerHit) rawHit).getIdentifierFieldValue("layer"); // 1-10; axial layers are odd layers; stereo layers are even
                    int module = ((RawTrackerHit) rawHit).getIdentifierFieldValue("module"); // 0-1; module number is top or bottom
                    hasHit[module][(layer - 1) / 2] = true;
                    hth[module][layer - 1].fill(((RawTrackerHit) rawHit).getIdentifierFieldValue("strip"));
                }
            }

            for (int module = 0; module < 2; module++) {
                int count = 0;
                for (int i = 0; i < 5; i++) {
                    if (hasHit[module][i]) {
                        count++;
                    }
                }
                hitCount[module].fill(count);
            }
        } else {
            throw new RuntimeException("Collection " + helicalHitCollection + " was not found.");
        }
    }

    /**
     * Fill TrackerHit plots for one event.
     * @param event The event.
     */
    private void plotTrackerHits(EventHeader event) {
        if (event.hasCollection(SiTrackerHit.class, trackerHitCollection)) {
            
            List<SiTrackerHit> trackerHits = event.get(SiTrackerHit.class, trackerHitCollection);

            System.out.println(trackerHitCollection + " has " + trackerHits.size() + " hits");
            
            // Increment strip hit count.
            for (SiTrackerHit hit : trackerHits) {
                for (RawTrackerHit rawHit : hit.getRawHits()) {
                    int layer = rawHit.getIdentifierFieldValue("layer"); // 1-10; axial layers are odd layers; stereo layers are even
                    int module = rawHit.getIdentifierFieldValue("module"); // 0-1; module number is top or bottom

                    th[module][layer - 1].fill(rawHit.getIdentifierFieldValue("strip"));
                }
            }
        } else {
            throw new RuntimeException("Collection " + trackerHitCollection + " was not found.");
        }
    }

    /**
     * Fill RawTrackerHit plots for one event.
     * @param event The event.
     */
    private void plotRawTrackerHits(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputCollection)) {
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, inputCollection);

            System.out.println(inputCollection + " has " + rawTrackerHits.size() + " hits");
            
            // Increment strip hit count.
            for (RawTrackerHit hit : rawTrackerHits) {
                int layer = hit.getIdentifierFieldValue("layer"); // 1-10; axial layers are odd layers; stereo layers are even
                int module = hit.getIdentifierFieldValue("module"); // 0-1; module number is top or bottom

                rth[module][layer - 1].fill(hit.getIdentifierFieldValue("strip"));
            }
        } else {
            throw new RuntimeException("Collection " + inputCollection + " was not found.");
        }
    }

    /**
     * Reset the plots for running in single event mode.
     */
    private void resetPlots() {
        for (int module = 0; module < 2; module++) {
            for (int layer = 1; layer < 11; layer++) {
                rth[module][layer - 1].reset();
                th[module][layer - 1].reset();
                hth[module][layer - 1].reset();
            }
        }
    }

    /**
     * Reset the hit count plot, which is not affected by single event setting. 
     */
    private void resetHitCountPlot() {
        for (int module = 0; module < 2; module++) {
            hitCount[module].reset();
        }
    }

    /**
     * Reset this Driver's plots.
     */
    public void reset() {
        resetPlots();
        resetHitCountPlot();
    }
}
