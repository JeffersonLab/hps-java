package org.lcsim.hps.monitoring.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.monitoring.deprecated.Resettable;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTHitPulsePlots extends Driver implements Resettable {

	//private AIDAFrame plotterFrame;
    private List<IPlotter> plotters = new ArrayList<IPlotter>();
    private AIDA aida = AIDA.defaultInstance();
    private String rawTrackerHitCollectionName = "RawTrackerHitMaker_RawTrackerHits";
    private String trackerName = "Tracker";
    private int eventCount;
    private IPlotter plotter3;
    private IPlotter plotter2;
    private List<SiSensor> sensors;
    private Map<String, Integer> sensorRegionMap;
    private String outputPlots = null;

    protected void detectorChanged(Detector detector) {
    	//plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS SVT Pulse Plots");

        aida.tree().cd("/");


        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);


        // Map a map of sensors to their region numbers in the plotter.
        sensorRegionMap = new HashMap<String, Integer>();
        for (SiSensor sensor : sensors) {
            int region = computePlotterRegion(sensor);
            sensorRegionMap.put(sensor.getName(), region);
        }

        IAnalysisFactory fac = aida.analysisFactory();


        plotter3 = fac.createPlotterFactory("SVT Pulse Plots").create("HPS SVT Pulse Plots:  Raw Hits");
        plotter3.setTitle("HPS SVT Pulse Plots:  Raw Hits");
        //plotterFrame.addPlotter(plotter3);
        plotters.add(plotter3);
        IPlotterStyle style3 = plotter3.style();
        style3.statisticsBoxStyle().setVisible(false);
//        style3.statisticsBoxStyle().setVisibileStatistics(trackerName);
        style3.dataStyle().fillStyle().setColor("black");
        style3.dataStyle().errorBarStyle().setVisible(true);
        plotter3.createRegions(5, 4);

        plotter2 = fac.createPlotterFactory("SVT Pulse Plots").create("HPS SVT Hit vs Channel");
        plotter2.setTitle("HPS SVT Hit vs Channel");
        plotter2.style().setParameter("hist2DStyle", "colorMap");
        plotter2.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter2.style().zAxisStyle().setParameter("scale", "log");
        //plotterFrame.addPlotter(plotter2);
        plotters.add(plotter2);
        IPlotterStyle style2 = plotter2.style();
        style2.statisticsBoxStyle().setVisible(false);
        style3.statisticsBoxStyle().setVisibileStatistics("Entries");
        style2.dataStyle().fillStyle().setColor("black");
        style2.dataStyle().errorBarStyle().setVisible(false);
        style3.dataStyle().errorBarStyle().setVisible(false);
        style2.dataStyle().markerStyle().setColor("blue");
        plotter2.createRegions(5, 4);
        for (SiSensor sensor : sensors) {
            IHistogram2D adcVsChanPlot = aida.histogram2D(sensor.getName() + "_AdcVsChan", 100, -100, 2000, 640, 0, 639);
            IProfile pulsePlot = aida.profile1D(sensor.getName() + "_pulse", 6, 0, 24 * 6.0);
            int region = sensorRegionMap.get(sensor.getName());

            plotter3.region(region).plot(pulsePlot);
            plotter2.region(region).plot(adcVsChanPlot);
        }
        //plotterFrame.pack();
        //plotterFrame.setVisible(true);
    }

    public SVTHitPulsePlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            ++eventCount;
            List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            for (RawTrackerHit hrth : rawHits) {
                fillPlots(hrth);
            }
        }
    }

    private void fillPlots(RawTrackerHit hit) {
        String sensorName = hit.getDetectorElement().getName();
        SiSensor sensor = (SiSensor) hit.getDetectorElement();
        int strip = hit.getIdentifierFieldValue("strip");
        short[] adcVal = hit.getADCValues();
        double ped = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
        double noise = HPSSVTCalibrationConstants.getNoise(sensor, strip);
        for (int i = 0; i < 6; i++) {
            double pedSub = (adcVal[i] - ped);
            aida.histogram2D(sensorName + "_AdcVsChan").fill(pedSub, strip);
            //only plot hits above threshold...
//            if (pedSub / noise > 3 && hasAdjacentHit(hit) && noise < 70)
            if (hasAdjacentHit(hit) && noise < 100) {
                aida.profile1D(sensorName + "_pulse").fill(24.0 * i, pedSub);
            }
        }
    }

    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconstructionPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //plotterFrame.dispose();
    }

    private boolean hasAdjacentHit(RawTrackerHit hit) {

        List<RawTrackerHit> hitsOnSensor = ((SiSensor) hit.getDetectorElement()).getReadout().getHits(RawTrackerHit.class);
        int strip = hit.getIdentifierFieldValue("strip");

        for (RawTrackerHit sensorHit : hitsOnSensor) {
            int thisStrip = sensorHit.getIdentifierFieldValue("strip");
//            System.out.println("hit strip = "+strip+"; other strips = "+thisStrip);
            if (Math.abs(thisStrip - strip) == 1) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void reset() {
        int ns = sensors.size();
        for (int i = 0; i < ns; i++) {
            aida.histogram2D(sensors.get(i).getName() + "_AdcVsChan").reset();
            aida.profile1D(sensors.get(i).getName() + "_pulse").reset();
        }
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
}
