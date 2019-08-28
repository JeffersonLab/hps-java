package org.hps.analysis.dataquality;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;

/**
 * DQM driver for reconstructed track quantities plots
 * things like occupancy, t0, amplitude, chi^2 (from APV25 sampling fit); each
 * on a per/sensor basis saves to DQM database: <occupancy>
 *
 * @author mgraham on Mar 28, 2014
 */
//TODO:  add some more quantities to DQM database:  <t0> or <sigma>_t0 for intime events;  <chi^2>, <amplitude> etc
public class SvtMonitoring extends DataQualityMonitor {

    private static Logger LOGGER = Logger.getLogger(SvtMonitoring.class.getPackage().getName());

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fittedTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    private String trackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    private Detector detector = null;
    private IPlotter plotter;
    private final String trackerName = "Tracker";
    private List<HpsSiSensor> sensors;
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
    private final String plotDir = "SvtMonitoring/";

    private int maxSamplePosition = 3;
    private int timeWindowWeight = 3;

    boolean makeExtraPlots = false;

    private double minChargeCut = 600.0;
    private double clTime=40; //abs value (ns)

    public void setRawTrackerHitCollectionName(String inputCollection) {
        this.rawTrackerHitCollectionName = inputCollection;
    }

    public void setFittedTrackerHitCollectionName(String inputCollection) {
        this.fittedTrackerHitCollectionName = inputCollection;
    }

    public void setTrackerHitCollectionName(String inputCollection) {
        this.trackerHitCollectionName = inputCollection;
    }

    public void MakeExtraPlots(boolean make) {
        this.makeExtraPlots = make;
    }

    @Override
    protected void detectorChanged(Detector detector) {
        LOGGER.info("Setting up the plotter");
        this.detector = detector;
        aida.tree().cd("/");

        // Make a list of SiSensors in the SVT.
        sensors = this.detector.getSubdetector(trackerName).getDetectorElement().findDescendants(HpsSiSensor.class);

        // Reset the data structure that keeps track of strip occupancies.
        reset();

        // Setup the occupancy plots.
        aida.tree().cd("/");
        for (HpsSiSensor sensor : sensors) {
            int i = sensor.getLayerNumber();
            double maxHTHX = 150.0;
            double maxHTHY = 50.0;
            if (i < 5) {
                maxHTHX = 10;
                maxHTHY = 15;
            } else if (i < 9) {
                maxHTHX = 50;
                maxHTHY = 30;
            }
            //IHistogram1D occupancyPlot = aida.histogram1D(sensor.getName().replaceAll("Tracker_TestRunModule_", ""), 640, 0, 639);
            IHistogram1D occupancyPlot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "occupancy_", sensor, maxChannels, 0, maxChannels - 1, true);
            IHistogram1D t0Plot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "t0Hit_", sensor, 200, -100., 100., true);
            IHistogram1D nHits = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "nHitsPerEvent_", sensor, 100, -0.5, 99.5, true);
            IHistogram1D pileup = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "nFitsPerHit_", sensor, 3, 0.5, 3.5, true);

            IHistogram1D amplitudePlot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "amplitude_", sensor, 50, 0, 3000.0, true);
            if (makeExtraPlots) {
                IHistogram2D t0AmpPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "t0AmpHit_", sensor, 200, -100., 100., 50, 0, 4000.0, true);
                IHistogram2D t0ChanPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "t0ChanBigHit_", sensor, 640, -0.5, 639.5, 200, -100., 100., true);
                IHistogram2D ampChanPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "ampChanHit_", sensor, 640, -0.5, 639.5, 50, 0, 4000, true);
                IHistogram2D chiprobChanPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "chiprobChanBigHit_", sensor, 640, -0.5, 639.5, 50, 0, 1.0, true);
            }
            IHistogram2D t0TrigTimeHitPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "t0BigHitTrigTime_", sensor, 200, -100., 100., 6, 0, 24, true);

            IHistogram1D chiProbPlot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "chiProb_", sensor, 50, 0, 1.0, true);
            IHistogram1D t0ClusterPlot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "t0Cluster_", sensor, 200, -100., 100., true);
            IHistogram2D t0TrigTimePlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "t0ClusterTrigTime_", sensor, 200, -100., 100., 6, 0, 24, true);
            IHistogram2D clusterPositionPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "clusterPositionPlot_", sensor, 100, -maxHTHX, maxHTHX, 100, -maxHTHY, maxHTHY, true);           
            IHistogram1D dedxClusterPlot = PlotAndFitUtilities.createSensorPlot(plotDir + triggerType + "/" + "charge_", sensor, 50, 0., 2500., true);
            IHistogram2D dedxVsYClusterPlot = PlotAndFitUtilities.createSensorPlot2D(plotDir + triggerType + "/" + "chargeVsY_", sensor, 100, 0.0, maxHTHY, 100, minChargeCut, 2500., true);
            occupancyPlot.reset();
        }

    }

    public SvtMonitoring() {
    }

    public void process(EventHeader event) {

        //check to see if this event is from the correct trigger (or "all");
        if (!matchTrigger(event))
            return;

        /*  increment the strip occupancy arrays */
        Map<String, Integer> hitsPerSensor = new HashMap<String, Integer>();

        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
//            LOGGER.info("Found a raw hit collection");
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            for (RawTrackerHit hit : rawTrackerHits) {
                short[] adcValues = hit.getADCValues();

                // Find the sample that has the largest amplitude. This should
                // correspond to the peak of the shaper signal if the SVT is timed
                // in correctly. Otherwise, the maximum sample value will default
                // to 0.
                int maxAmplitude = 0;
                int maxSamplePositionFound = -1;
                for (int sampleN = 0; sampleN < 6; sampleN++)
                    if (adcValues[sampleN] > maxAmplitude) {
                        maxAmplitude = adcValues[sampleN];
                        maxSamplePositionFound = sampleN;
                    }
                if (maxSamplePosition == -1 || maxSamplePosition == maxSamplePositionFound)
                    occupancyMap.get(hit.getDetectorElement().getName())[hit.getIdentifierFieldValue("strip")]++; //                System.out.println("Filling occupancy");

                Integer nHits = hitsPerSensor.get(hit.getDetectorElement().getName());
                if (nHits == null)
                    nHits = 0;
                nHits++;
                hitsPerSensor.put(hit.getDetectorElement().getName(), nHits);
            }
            ++eventCountRaw;
        }
        for (HpsSiSensor sensor : sensors) {
            IHistogram1D sensorHist = PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "nHitsPerEvent_", sensor, true);
            Integer nHits = hitsPerSensor.get(sensor.getName());
            if (nHits == null)
                sensorHist.fill(0);
            else
                sensorHist.fill(nHits);
        }
        /*  fill the FittedTrackerHit related histograms */
        if (event.hasCollection(LCRelation.class, fittedTrackerHitCollectionName)) {
            List<LCRelation> fittedTrackerHits = event.get(LCRelation.class, fittedTrackerHitCollectionName);

            RelationalTable rthtofit = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            for (LCRelation hit : fittedTrackerHits)
                rthtofit.add(FittedRawTrackerHit.getRawTrackerHit(hit), FittedRawTrackerHit.getShapeFitParameters(hit));

            for (LCRelation hit : fittedTrackerHits) {
                RawTrackerHit rth = (RawTrackerHit) hit.getFrom();
                GenericObject pars = (GenericObject) hit.getTo();

                HpsSiSensor sensor = ((HpsSiSensor) rth.getDetectorElement());
                //this is a clever way to get the parameters we want from the generic object
                double t0 = ShapeFitParameters.getT0(pars);
                double amp = ShapeFitParameters.getAmp(pars) ;
                double chiProb = ShapeFitParameters.getChiProb(pars);
                int channel = rth.getIdentifierFieldValue("strip");
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "nFitsPerHit_", sensor, true).fill(rthtofit.allFrom(rth).size());
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "t0Hit_", sensor, true).fill(t0);
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "amplitude_", sensor, true).fill(amp);
                if (makeExtraPlots)
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "t0AmpHit_", sensor, true).fill(t0, amp);
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "chiProb_", sensor, true).fill(chiProb);
                if (makeExtraPlots)
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "ampChanHit_", sensor, true).fill(channel, amp);
                if (amp > minChargeCut && makeExtraPlots) {
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "t0ChanBigHit_", sensor, true).fill(channel, t0);
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "chiprobChanBigHit_", sensor, true).fill(channel, chiProb);
                }
                if (amp > minChargeCut)
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "t0BigHitTrigTime_", sensor, true).fill(t0, event.getTimeStamp() % 24);
            }
            ++eventCountFit;
        }

        if (event.hasItem(trackerHitCollectionName)) {
//            LOGGER.info("Found a Si cluster collection");
            List<TrackerHit> siClusters = (List<TrackerHit>) event.get(trackerHitCollectionName);
            for (TrackerHit cluster : siClusters) {
                HpsSiSensor sensor = (HpsSiSensor) ((RawTrackerHit) cluster.getRawHits().get(0)).getDetectorElement();
                double t0 = cluster.getTime();
                double dedx = cluster.getdEdx() / DopedSilicon.ENERGY_EHPAIR;
//                LOGGER.info("dedx = "+dedx);
                PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "clusterPositionPlot_", sensor, true).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "t0Cluster_", sensor, true).fill(t0);
                PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "t0ClusterTrigTime_", sensor, true).fill(t0, event.getTimeStamp() % 24);
                PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "charge_", sensor, true).fill(dedx);
                if (dedx > minChargeCut && Math.abs(t0)<clTime)
                    PlotAndFitUtilities.getSensorPlot2D(plotDir + triggerType + "/" + "chargeVsY_", sensor, true).fill(Math.abs(cluster.getPosition()[1]), dedx);
            }
        }
    }

    private void resetOccupancyMap() {
        occupancyMap = new HashMap<String, int[]>();
        avgOccupancyMap = new HashMap<String, Double>();
        avgOccupancyNames = new HashMap<String, String>();
        avgt0Names = new HashMap<String, String>();
        sigt0Names = new HashMap<String, String>();
        avgt0Map = new HashMap<String, Double>();
        sigt0Map = new HashMap<String, Double>();
        for (HpsSiSensor sensor : sensors) {
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

    private String getNiceSensorName(HpsSiSensor sensor) {
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
        LOGGER.info("SvtMonitoring::endOfData  filling occupancy plots");
        for (HpsSiSensor sensor : sensors) {
            Double avg = 0.0;
            //IHistogram1D sensorHist = aida.histogram1D(sensor.getName());
            IHistogram1D sensorHist = PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "occupancy_", sensor, true);
            sensorHist.reset();
            int[] strips = occupancyMap.get(sensor.getName());
            for (int i = 0; i < strips.length; i++) {
                double stripOccupancy = (double) strips[i] / (double) (eventCountRaw);
                if (stripOccupancy != 0)
                    sensorHist.fill(i, stripOccupancy / timeWindowWeight);
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
        for (HpsSiSensor sensor : sensors) {
            IHistogram1D sensPlot = PlotAndFitUtilities.getSensorPlot(plotDir + triggerType + "/" + "t0Hit_", sensor, true);
            IFitResult result = fitGaussian(sensPlot, fitter, "range=\"(-8.0,8.0)\"");

            boolean isTop = sensor.isTopLayer();
            if (isTop) {
                plotterTop.region(irTop).plot(sensPlot);
                plotterTop.region(irTop).plot(result.fittedFunction());
                irTop++;
            } else {
                plotterBottom.region(irBot).plot(sensPlot);
                plotterBottom.region(irBot).plot(result.fittedFunction());
                irBot++;
            }
            avgt0Map.put(sensor.getName(), result.fittedParameters()[1]);
            sigt0Map.put(sensor.getName(), result.fittedParameters()[2]);
        }
        if (outputPlots) {
            try {
                plotterTop.writeToFile(outputPlotDir + "t0TopPlots.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
            try {
                plotterBottom.writeToFile(outputPlotDir + "t0BottomPlots.png");
            } catch (IOException ex) {
                Logger.getLogger(SvtMonitoring.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    @Override
    public void dumpDQMData() {
        for (HpsSiSensor sensor : sensors) {
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
        for (HpsSiSensor sensor : sensors) {
            LOGGER.info(avgOccupancyNames.get(sensor.getName()) + "  " + triggerType + " " + avgOccupancyMap.get(sensor.getName()));
            LOGGER.info(avgt0Names.get(sensor.getName()) + "  " + triggerType + " " + avgt0Map.get(sensor.getName()));
            LOGGER.info(sigt0Names.get(sensor.getName()) + "  " + triggerType + " " + sigt0Map.get(sensor.getName()));
        }
    }

    @Override
    public void printDQMStrings() {
        for (HpsSiSensor sensor : sensors) {
            LOGGER.info("ALTER TABLE dqm ADD " + avgOccupancyNames.get(sensor.getName()) + " double;");
            LOGGER.info("ALTER TABLE dqm ADD " + avgt0Names.get(sensor.getName()) + " double;");
            LOGGER.info("ALTER TABLE dqm ADD " + sigt0Names.get(sensor.getName()) + " double;");
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
            LOGGER.info("Not writing because " + name + " is already filled for this entry");
            return; //entry exists and I don't want to overwrite                
        }
        String put = "update dqm SET " + name + " = " + val + " WHERE " + getRunRecoString();
        LOGGER.info(put);
        manager.updateQuery(put);
    }
}
