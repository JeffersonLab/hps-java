package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for the monte carlo for reconstructed track quantities
 * plots things like occupancy, t0, amplitude, chi^2 (from APV25 sampling fit);
 * each on a per/sensor basis
 * saves to DQM database: <occupancy>
 *
 * @author mgraham on Mar 28, 2014
 */
//TODO:  add some more quantities to DQM database:  <t0> or <sigma>_t0 for intime events;  <chi^2>, <amplitude> etc
public class SvtMonitoring extends DataQualityMonitor {

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private Detector detector = null;
    private IPlotter plotter;
    private String trackerName = "Tracker";
    private List<SiSensor> sensors;
    private Map<String, int[]> occupancyMap;
    private Map<String, Double> avgOccupancyMap;
    private Map<String, String> avgOccupancyNames;
    private Map<String, Double> avgt0Map;
    private Map<String, Double> sigt0Map;
    private Map<String, String> avgt0Names;
    private Map<String, String> sigt0Names;
    private int eventCountRaw = 0;
    private int eventCountFit = 0;
    private int eventCountCluster = 0;
    private static final String nameStrip = "Tracker_TestRunModule_";
    private static final int maxChannels = 640;
    private String plotDir = "SvtMonitoring/";

    public void setRawTrackerHitCollectionName(String inputCollection) {
        this.rawTrackerHitCollectionName = inputCollection;
    }

    public void setFittedTrackerHitCollectionName(String inputCollection) {
        this.fittedTrackerHitCollectionName = inputCollection;
    }

    public void setTrackerHitCollectionName(String inputCollection) {
        this.trackerHitCollectionName = inputCollection;
    }

    protected void detectorChanged(Detector detector) {
        System.out.println("SvtMonitoring::detectorChanged  Setting up the plotter");
        this.detector = detector;
        aida.tree().cd("/");

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(SiSensor.class);

        // Reset the data structure that keeps track of strip occupancies.
        resetOccupancyMap();

        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (SiSensor sensor : sensors) {
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D occupancyPlot = createSensorPlot(plotDir + "occupancy_", sensor, maxChannels, 0, maxChannels - 1);
            IHistogram1D t0Plot = createSensorPlot(plotDir + "t0_", sensor, 50, -50., 50.);
            IHistogram1D amplitudePlot = createSensorPlot(plotDir + "amplitude_", sensor, 50, 0, 2000);
            IHistogram1D chi2Plot = createSensorPlot(plotDir + "chi2_", sensor, 50, 0, 25);
            occupancyPlot.reset();
        }

    }

    public SvtMonitoring() {
    }

    public void process(EventHeader event) {
        /*  increment the strip occupancy arrays */
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            for (RawTrackerHit hit : rawTrackerHits) {
                int[] strips = occupancyMap.get(hit.getDetectorElement().getName());
                strips[hit.getIdentifierFieldValue("strip")] += 1;
            }
            ++eventCountRaw;
        } else
            return; /* kick out of this if the even has none of these...*/
        /*  fill the FittedTrackerHit related histograms */

        if (event.hasCollection(FittedRawTrackerHit.class, fittedTrackerHitCollectionName)) {
            List<FittedRawTrackerHit> fittedTrackerHits = event.get(FittedRawTrackerHit.class, fittedTrackerHitCollectionName);
            for (FittedRawTrackerHit hit : fittedTrackerHits) {
                String sensorName = hit.getRawTrackerHit().getDetectorElement().getName();
                double t0 = hit.getT0();
                double amp = hit.getAmp();
                double chi2 = hit.getShapeFitParameters().getChiSq();
                getSensorPlot(plotDir + "t0_", sensorName).fill(t0);
                getSensorPlot(plotDir + "amplitude_", sensorName).fill(amp);
                getSensorPlot(plotDir + "chi2_", sensorName).fill(chi2);
            }
            ++eventCountFit;
        } else
            return;
    }

    private IHistogram1D getSensorPlot(String prefix, SiSensor sensor) {
        return aida.histogram1D(prefix + sensor.getName());
    }

    private IHistogram1D getSensorPlot(String prefix, String sensorName) {
        return aida.histogram1D(prefix + sensorName);
    }

    private IHistogram1D createSensorPlot(String prefix, SiSensor sensor, int nchan, double min, double max) {
        IHistogram1D hist = aida.histogram1D(prefix + sensor.getName(), nchan, min, max);
        hist.setTitle(sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens"));
        return hist;
    }

    private void resetOccupancyMap() {
        occupancyMap = new HashMap<>();
        avgOccupancyMap = new HashMap<>();
        avgOccupancyNames = new HashMap<>();
        avgt0Names = new HashMap<>();
        sigt0Names = new HashMap<>();
        avgt0Map = new HashMap<>();
        sigt0Map = new HashMap<>();
        for (SiSensor sensor : sensors) {
            occupancyMap.put(sensor.getName(), new int[640]);
            avgOccupancyMap.put(sensor.getName(), -999.);
            String occName = "avgOcc_" + getNiceSensorName(sensor);
            avgOccupancyNames.put(sensor.getName(), occName);
            String avgt0Name = "avgt0_" + getNiceSensorName(sensor);
            String sigt0Name = "sigmat0_" + getNiceSensorName(sensor);

            avgt0Names.put(sensor.getName(), avgt0Name);
            sigt0Names.put(sensor.getName(), sigt0Name);
        }
    }

    private String getNiceSensorName(SiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

    public void reset() {
        eventCountRaw = 0;
        eventCountFit = 0;
        eventCountCluster = 0;
        resetOccupancyMap();
    }

    @Override
    public void fillEndOfRunPlots() {
        // Plot strip occupancies.
        System.out.println("SvtMonitoring::endOfData  filling occupancy plots");
        for (SiSensor sensor : sensors) {
            Double avg = 0.0;
            //IHistogram1D sensorHist = aida.histogram1D(sensor.getName());
            IHistogram1D sensorHist = getSensorPlot(plotDir + "occupancy_", sensor);
            sensorHist.reset();
            int[] strips = occupancyMap.get(sensor.getName());
            for (int i = 0; i < strips.length; i++) {
                double stripOccupancy = (double) strips[i] / (double) (eventCountRaw);
                if (stripOccupancy != 0)
                    sensorHist.fill(i, stripOccupancy);
                avg += stripOccupancy;
            }
            //do the end-of-run quantities here too since we've already done the loop.  
            avg /= strips.length;
            avgOccupancyMap.put(sensor.getName(), avg);
        }

    }

    @Override
    public void calculateEndOfRunQuantities() {
        IAnalysisFactory analysisFactory = IAnalysisFactory.create();
        IFitFactory fitFactory = analysisFactory.createFitFactory();
        IFitter fitter = fitFactory.createFitter("chi2");
        IPlotter plotterTop = analysisFactory.createPlotterFactory().create("t0 Top");
        IPlotter plotterBottom = analysisFactory.createPlotterFactory().create("t0 Bottom");

        plotterTop.createRegions(4, 5);
        IPlotterStyle pstyle = plotterTop.style();
        pstyle.legendBoxStyle().setVisible(false);
        plotterBottom.createRegions(4, 5);
        IPlotterStyle pstyle2 = plotterBottom.style();
        pstyle2.legendBoxStyle().setVisible(false);

        int irTop = 0;
        int irBot = 0;
        for (SiSensor sensor : sensors) {
            IHistogram1D sensPlot = getSensorPlot(plotDir + "t0_", sensor);
            IFitResult result = fitGaussian(sensPlot, fitter, "range=\"(-10.0,10.0)\"");
            for (int i = 0; i < 5; i++) {
                double par = result.fittedParameters()[i];
                System.out.println("t0_" + sensor.getName() + ":  " + result.fittedParameterNames()[i] + " = " + par);
            }

            boolean isTop = SvtUtils.getInstance().isTopLayer(sensor);

            if (isTop) {
                System.out.println("Plotting into Top region " + irTop);
                plotterTop.region(irTop).plot(sensPlot);
                plotterTop.region(irTop).plot(result.fittedFunction());
                irTop++;
            } else {
                System.out.println("Plotting into Bottom region " + irBot);
                plotterBottom.region(irBot).plot(sensPlot);
                plotterBottom.region(irBot).plot(result.fittedFunction());
                irBot++;
            }
            avgt0Map.put(sensor.getName(), result.fittedParameters()[1]);
            sigt0Map.put(sensor.getName(), result.fittedParameters()[2]);
        }
        try {
            plotterTop.writeToFile("t0TopPlots.png");
        } catch (IOException ex) {
            Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            plotterBottom.writeToFile("t0BottomPlots.png");
        } catch (IOException ex) {
            Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void dumpDQMData() {
        for (SiSensor sensor : sensors) {
            String name = avgOccupancyNames.get(sensor.getName());
            double occ = avgOccupancyMap.get(sensor.getName());
            checkAndUpdate(name, occ);
            String avgt0Name = avgt0Names.get(sensor.getName());
            double avgt0 = avgt0Map.get(sensor.getName());
            checkAndUpdate(avgt0Name, avgt0);
            String sigt0Name = sigt0Names.get(sensor.getName());
            double sigt0 = sigt0Map.get(sensor.getName());
            checkAndUpdate(sigt0Name, sigt0);
        }
    }

    @Override
    public void printDQMData() {
        for (SiSensor sensor : sensors) {
            System.out.println(avgOccupancyNames.get(sensor.getName()) + ":  " + avgOccupancyMap.get(sensor.getName()));
            System.out.println(avgt0Names.get(sensor.getName()) + ":  " + avgt0Map.get(sensor.getName()));
            System.out.println(sigt0Names.get(sensor.getName()) + ":  " + sigt0Map.get(sensor.getName()));
        }
    }

    @Override
    public void printDQMStrings() {
        for (SiSensor sensor : sensors) {
            System.out.println("ALTER TABLE dqm ADD " + avgOccupancyNames.get(sensor.getName()) + " double;");
            System.out.println("ALTER TABLE dqm ADD " + avgt0Names.get(sensor.getName()) + " double;");
            System.out.println("ALTER TABLE dqm ADD " + sigt0Names.get(sensor.getName()) + " double;");
        }
    }

    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
//        return fitter.fit(h1d, "g", range);
        double[] init = {20.0, 0.0, 4.0, 20, -1};
        return fitter.fit(h1d, "g+p1", init, range);

    }

    void checkAndUpdate(String name, double val) {
        boolean isnull = false;
        try {
            isnull = checkSelectionIsNULL(name);
        } catch (SQLException ex) {
            Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (!overwriteDB && !isnull) {
            System.out.println("Not writing because " + name + " is already filled for this entry");
            return; //entry exists and I don't want to overwrite                
        }
        String put = "update dqm SET " + name + " = " + val + " WHERE " + getRunRecoString();
        System.out.println(put);
        manager.updateQuery(put);
    }
}
