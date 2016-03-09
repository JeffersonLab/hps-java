package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.monitoring.drivers.trackrecon.TrackingReconPlots;
//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class SVTHitReconstructionPlots extends Driver {

    //private AIDAFrame plotterFrame;
    private AIDA aida = AIDA.defaultInstance();
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private String trackerName = "Tracker";
    private List<SiSensor> sensors;
    IPlotter plotter1;
    IPlotter plotter2;
    IPlotter plotter3;
    IPlotter plotter4;
    IPlotter plotter5;
    IPlotter plotter6;
    /*IHistogram1D nrawPlot[][] = new IHistogram1D[2][10];
    IHistogram1D nrecoPlot[][] = new IHistogram1D[2][10];
    IHistogram1D nclustPlot[][] = new IHistogram1D[2][10];
    IHistogram1D clusterSizePlot[][] = new IHistogram1D[2][10];
    IHistogram1D clusterAmpPlot[][] = new IHistogram1D[2][10];
    IHistogram2D clposVsStrip[][] = new IHistogram2D[2][10];*/
    private Map<String, Integer> sensorRegionMap;
    private String outputPlots = null;

    protected void detectorChanged(Detector detector) {
        //plotterFrame = new AIDAFrame();
        //plotterFrame.setTitle("HPS SVT Hit Reconstruction Plots");

        aida.tree().cd("/");


        sensors = detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Map a map of sensors to their region numbers in the plotter.
        sensorRegionMap = new HashMap<String, Integer>();
        for (SiSensor sensor : sensors) {
            int region = computePlotterRegion(sensor);
            sensorRegionMap.put(sensor.getName(), region);
        }
        int nregionx = (int) Math.floor(((double)sensorRegionMap.size())/2.0);
        int nregiony = (int) Math.ceil(((double)sensorRegionMap.size())/2.0);
        

        IAnalysisFactory fac = aida.analysisFactory();

        plotter1 = fac.createPlotterFactory().create("HPS SVT Raw Hit Plots");
        plotter1.setTitle("Raw Hits");
        //plotterFrame.addPlotter(plotter1);
        IPlotterStyle style3 = plotter1.style();
        style3.dataStyle().fillStyle().setColor("yellow");
        style3.dataStyle().errorBarStyle().setVisible(false);
        plotter1.createRegions(nregionx, nregiony);

        plotter3 = fac.createPlotterFactory().create("HPS SVT Reco Hit Plots");
        plotter3.setTitle("Reco Hits");
        //plotterFrame.addPlotter(plotter3);
        IPlotterStyle style4 = plotter3.style();
        style4.dataStyle().fillStyle().setColor("yellow");
        style4.dataStyle().errorBarStyle().setVisible(false);
        plotter3.createRegions(nregionx, nregiony);


        plotter2 = fac.createPlotterFactory().create("HPS SVT Cluster Hit Plots");
        plotter2.setTitle("Clusters");
        //plotterFrame.addPlotter(plotter2);
        IPlotterStyle style44 = plotter2.style();
        style44.dataStyle().fillStyle().setColor("yellow");
        style44.dataStyle().errorBarStyle().setVisible(false);
        plotter2.createRegions(nregionx, nregiony);

        plotter4 = fac.createPlotterFactory().create("HPS SVT Cluster Size Plots");
        plotter4.setTitle("Cluster Size");
        //plotterFrame.addPlotter(plotter4);
        IPlotterStyle style6 = plotter4.style();
        style6.dataStyle().fillStyle().setColor("green");
        style6.dataStyle().errorBarStyle().setVisible(false);
        plotter4.createRegions(nregionx, nregiony);


        plotter5 = fac.createPlotterFactory().create("HPS SVT Cluster Amp Plots");
        plotter5.setTitle("Cluster Amplitude");
        //plotterFrame.addPlotter(plotter5);
        IPlotterStyle style7 = plotter5.style();
        style7.dataStyle().fillStyle().setColor("green");
        style7.dataStyle().errorBarStyle().setVisible(false);
        plotter5.createRegions(nregionx, nregiony);
//        plotter5.createRegion();

        plotter6 = fac.createPlotterFactory().create("HPS SVT Cluster Position Vs Channel");
        plotter6.setTitle("Cluster Position (y)");
        //plotterFrame.addPlotter(plotter6);
        IPlotterStyle style8 = plotter6.style();
        style8.dataStyle().fillStyle().setColor("green");
        style8.dataStyle().errorBarStyle().setVisible(false);
        style8.statisticsBoxStyle().setVisible(false);
        plotter6.style().setParameter("hist2DStyle", "colorMap");
        style8.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        style8.zAxisStyle().setParameter("scale", "log");
        plotter6.createRegions(nregionx, nregiony);

       
       // TODO: Check if this block of code is equivalent to the block commented out below
       for(SiSensor sensor : sensors){
            int region = computePlotterRegion(sensor);
            if (region >= plotter1.numberOfRegions()) {
                throw new RuntimeException("not enough regions! (" + region + "/" + plotter1.numberOfRegions() + ")");
            }
            plotter1.region(region).plot(aida.histogram1D(sensor.getName() + "_raw_hits", 10, -0.5, 9.5));
            plotter3.region(region).plot(aida.histogram1D(sensor.getName() + "_reco_hits", 10, -0.5, 9.5));
            plotter2.region(region).plot(aida.histogram1D(sensor.getName() + "_cluster_hits", 10, -0.5, 9.5));
            plotter4.region(region).plot(aida.histogram1D(sensor.getName() + "_cluster_size", 9, 0.5, 9.5));
            plotter5.region(region).plot(aida.histogram1D(sensor.getName() + "_cluster_amp", 50, 0, 4000.0));
            plotter5.style().xAxisStyle().setLabel("Cluster amplitude [ADC counts]");
            plotter6.region(region).plot(
                    aida.histogram2D(sensor.getName() + "_cluster_vs_strip", 128, 0, 640, 100, -50, 50));
       }
        
        /* ===> 
        for (int module = 0; module < 2; module++) {
            for (int layer = 0; layer < 10; layer++) {
                SiSensor sensor = SvtUtils.getInstance().getSensor(module, layer);
                int region = computePlotterRegion(sensor);

                nrawPlot[module][layer] = aida.histogram1D(sensor.getName() + "_raw_hits", 10, -0.5, 9.5);
                nrecoPlot[module][layer] = aida.histogram1D(sensor.getName() + "_reco_hits", 10, -0.5, 9.5);
                nclustPlot[module][layer] = aida.histogram1D(sensor.getName() + "_cluster_hits", 10, -0.5, 9.5);
                clusterSizePlot[module][layer] = aida.histogram1D(sensor.getName() + "_cluster_size", 9, 0.5, 9.5);
                clusterAmpPlot[module][layer] = aida.histogram1D(sensor.getName() + "_cluster_amp", 50, 0, 4000.0);
                clposVsStrip[module][layer] = aida.histogram2D(sensor.getName() + "_cluster_vs_strip", 128, 0, 640, 100, -50, 50);
                plotter1.region(region).plot(nrawPlot[module][layer]);
                plotter3.region(region).plot(nrecoPlot[module][layer]);
                plotter2.region(region).plot(nclustPlot[module][layer]);
                plotter4.region(region).plot(clusterSizePlot[module][layer]);
                plotter5.region(region).plot(clusterAmpPlot[module][layer]);
                ((PlotterRegion) plotter5.region(region)).getPlot().getXAxis().setLabel("Cluster amplitude [ADC counts]");
                plotter6.region(region).plot(clposVsStrip[module][layer]);
            }
        } ===> */

//        plotter5.region(0).plot(aida.histogram1D("Tracker_TestRunModule_layer6_module0_sensor0" + "_cluster_amp"));
//        ((PlotterRegion) plotter5.region(0)).getPlot().getXAxis().setLabel("Cluster amplitude [ADC counts]");

        IProfile1D hitsPerLayerTop = aida.profile1D("Number of Fitted Hits per layer in Top Half", 10, 1, 11);
        IProfile1D hitsPerLayerBot = aida.profile1D("Number of Fitted Hits per layer in Bottom Half", 10, 1, 11);
        //plotterFrame.pack();
        //plotterFrame.setVisible(true);
    }

    public SVTHitReconstructionPlots() {
    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    public void setFittedTrackerHitCollectionName(String fittedTrackerHitCollectionName) {
        this.fittedTrackerHitCollectionName = fittedTrackerHitCollectionName;
    }

    public void setTrackerHitCollectionName(String trackerHitCollectionName) {
        this.trackerHitCollectionName = trackerHitCollectionName;
    }

    public void process(EventHeader event) {
        if (!event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName)) {
            System.out.println(fittedTrackerHitCollectionName + " does not exist; skipping event");
            int ns = sensors.size();
            for (int i = 0; i < ns; i++) {
                int nraw = sensors.get(i).getReadout().getHits(RawTrackerHit.class).size();
                aida.histogram1D(sensors.get(i).getName() + "_raw_hits").fill(nraw);
                aida.histogram1D(sensors.get(i).getName() + "_reco_hits").fill(0);
            }
            return;
        }

        List<FittedRawTrackerHit> fittedrawHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
        List<SiTrackerHitStrip1D> stripHits = event.get(SiTrackerHitStrip1D.class, trackerHitCollectionName);
        int[] layersTop = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        int[] layersBot = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        for (FittedRawTrackerHit hrth : fittedrawHits) {
            SiSensor sensor = (SiSensor) hrth.getRawTrackerHit().getDetectorElement();
            int layer = hrth.getRawTrackerHit().getLayerNumber();
            //===> if (!SvtUtils.getInstance().isTopLayer(sensor)) {
            if (!((HpsSiSensor) sensor).isTopLayer()) {
                layersBot[layer - 1]++;
            } else {
                layersTop[layer - 1]++;
            }
        }

        for (int i = 0; i < 10; i++) {
            aida.profile1D("Number of Fitted Hits per layer in Top Half").fill(i + 1, layersTop[i]);
            aida.profile1D("Number of Fitted Hits per layer in Bottom Half").fill(i + 1, layersBot[i]);
        }
        Map<SiSensor, Integer> clustMap = new HashMap<SiSensor, Integer>();
        for (SiTrackerHitStrip1D cluster : stripHits) {
            SiSensor sensor = cluster.getSensor();
            if (clustMap.containsKey(sensor)) {
                clustMap.put(sensor, clustMap.get(sensor) + 1);
            } else {
                clustMap.put(sensor, 1);
            }
            String sensorName = sensor.getName();
            int clusterSize = cluster.getRawHits().size();
            aida.histogram1D(sensorName + "_cluster_size").fill(clusterSize);
            double cluAmp = cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR;
            aida.histogram1D(sensorName + "_cluster_amp").fill(cluAmp);
            double clpos = cluster.getPositionAsVector().y();
            RawTrackerHit raw = (RawTrackerHit) cluster.getRawHits().get(0);
            SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) raw.getDetectorElement().getIdentifierHelper();
            IIdentifier id = raw.getIdentifier();
            int stripNum = _sid_helper.getElectrodeValue(id);
            aida.histogram2D(sensorName + "_cluster_vs_strip").fill(stripNum, clpos);
        }


        for (SiSensor sensor : sensors) {
            String sensorName = sensor.getName();
            int nraw = sensor.getReadout().getHits(RawTrackerHit.class).size();
            int nreco = sensor.getReadout().getHits(FittedRawTrackerHit.class).size();
            aida.histogram1D(sensorName + "_raw_hits").fill(nraw);
            aida.histogram1D(sensorName + "_reco_hits").fill(nreco);
            if (clustMap.containsKey(sensor)) {
                aida.histogram1D(sensorName + "_cluster_hits").fill(clustMap.get(sensor));
            } else {
                aida.histogram1D(sensorName + "_cluster_hits").fill(0);
            }
        }
    }

    public double getCluAmp(SiTrackerHitStrip1D stripHits, List<FittedRawTrackerHit> hrths) {
        stripHits.getdEdx();
        List<RawTrackerHit> rawHits = stripHits.getRawHits();
        double sum = 0.0;
        for (RawTrackerHit hit : rawHits) {
            //find the fitted hit amplitude
            for (FittedRawTrackerHit fittedHit : hrths) {
                RawTrackerHit fh = fittedHit.getRawTrackerHit();
                if (fh.equals(hit)) {
                    sum += fittedHit.getAmp();
                }
            }
        }
        return sum;
    }

    public void endOfData() {
        if (outputPlots != null) {
            try {
                aida.saveAs(outputPlots);
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        //plotterFrame.dispose();
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
