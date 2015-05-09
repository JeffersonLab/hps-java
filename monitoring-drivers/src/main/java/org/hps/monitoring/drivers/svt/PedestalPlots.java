package org.hps.monitoring.drivers.svt;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPoint;
import hep.aida.IDataPointSet;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.jfree.plotter.PlotterRegion;
import hep.aida.ref.histogram.DataPoint;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
//===> import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class PedestalPlots extends Driver {

    static {
        hep.aida.jfree.AnalysisFactory.register();
    }

    // Plotting
    private static ITree tree = null;
    private IAnalysisFactory analysisFactory = AIDA.defaultInstance().analysisFactory();
    private IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory("SVT Pedestals");
    private IHistogramFactory histogramFactory = null;

    // Histogram maps
    private static Map<String, IPlotter> plotters = new HashMap<String, IPlotter>();
    private static Map<String, IHistogram1D> occupancyPlots = new HashMap<String, IHistogram1D>();

    private AIDA aida = AIDA.defaultInstance();
    List<HpsSiSensor> sensors;
    private Map<HpsSiSensor, IHistogram2D> hists;
    private Map<HpsSiSensor, int[]> counts;
    private Map<HpsSiSensor, double[]> means;
    private Map<HpsSiSensor, double[]> sumsqs;
    private Map<HpsSiSensor, IDataPointSet[]> plots;
    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private String fitFile = null;
    private boolean plotTimeSeries = false;
    private static final String subdetectorName = "Tracker";
    private int eventCount = 0;
    private int eventRefreshRate = 1;

    public void setFitFile(String fitFile) {
        this.fitFile = fitFile;
    }

    public void setPlotTimeSeries(boolean plotTimeSeries) {
        this.plotTimeSeries = plotTimeSeries;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");

        hists = new HashMap<HpsSiSensor, IHistogram2D>();
        counts = new HashMap<HpsSiSensor, int[]>();
        means = new HashMap<HpsSiSensor, double[]>();
        sumsqs = new HashMap<HpsSiSensor, double[]>();
        plots = new HashMap<HpsSiSensor, IDataPointSet[]>();

        sensors = detector.getSubdetector(subdetectorName).getDetectorElement().findDescendants(HpsSiSensor.class);

        plotters.put("Pedestal vs. channel", plotterFactory.create("Pedestal vs. channel"));
        plotters.get("Pedestal vs. channel").createRegions(6, 6);

        //===> for (SiSensor sensor : SvtUtils.getInstance().getSensors()) {
        for (HpsSiSensor sensor : sensors) {
            hists.put(sensor, aida.histogram2D(sensor.getName() + " sample 1 vs. ch", 640, -0.5, 639.5, 100, -500.0, 500.0));
            plotters.get("Pedestal vs. channel").region(SvtPlotUtils.computePlotterRegion(sensor)).plot(hists.get(sensor), this.createStyle(sensor, "Channel", "Sample 1"));

            if (plotTimeSeries) {
                counts.put(sensor, new int[640]);
                means.put(sensor, new double[640]);
                sumsqs.put(sensor, new double[640]);
                IDataPointSet[] plotArray = new IDataPointSet[640];
                plots.put(sensor, plotArray);
                for (int i = 0; i < 640; i++) {
                    plotArray[i] = aida.analysisFactory().createDataPointSetFactory(aida.tree()).create(sensor.getName() + ", channel " + i + " pedestal vs. event", 2);
                }
            }
        }

        for (IPlotter plotter : plotters.values()) {
            for (int regionN = 0; regionN < plotter.numberOfRegions(); regionN++) {
                PlotterRegion region = ((PlotterRegion) ((Plotter) plotter).region(regionN));
                if (region.getPlottedObjects().isEmpty()) {
                    continue;
                }
                region.getPanel().addMouseListener(new PopupPlotterListener(region));
            }
            plotter.show();
        }
    }

    IPlotterStyle createStyle(HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) {
        IPlotterStyle style = SvtPlotUtils.createStyle(plotterFactory, xAxisTitle, yAxisTitle);

        if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }

        return style;
    }

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            eventCount++;

            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                int strip = hit.getIdentifierFieldValue("strip");
                double pedestal = sensor.getPedestal(strip, 0);
                //===> double pedestal = HPSSVTCalibrationConstants.getPedestal(sensor, strip);
                hists.get(sensor).fill(strip, hit.getADCValues()[0] - pedestal);

                if (plotTimeSeries) {

                    counts.get(sensor)[strip]++;
                    double delta = hit.getADCValues()[0] - pedestal - means.get(sensor)[strip];
                    means.get(sensor)[strip] += delta / counts.get(sensor)[strip];
                    sumsqs.get(sensor)[strip] += delta * (hit.getADCValues()[0] - pedestal - means.get(sensor)[strip]);

                    if (counts.get(sensor)[strip] >= 100) {
                        double[] data = {event.getEventNumber(), means.get(sensor)[strip]};
                        double[] errs = {0.0, Math.sqrt(sumsqs.get(sensor)[strip] / counts.get(sensor)[strip])};
                        IDataPoint point = new DataPoint(data, errs);
                        plots.get(sensor)[strip].addPoint(point);
                        counts.get(sensor)[strip] = 0;
                        means.get(sensor)[strip] = 0;
                        sumsqs.get(sensor)[strip] = 0;
                    }
                }

            }
//            if (eventCount % eventRefreshRate == 0) {
//                for (HpsSiSensor sensor : sensors) {
//                    IHistogram2D hist = hists.get(sensor);
////                    hist.
//                }
//            }

        }
    }

//    private void getMean2D(IHistogram2D hist2D) {
//        int nx = hist2D.xAxis().bins();
//        int ny = hist2D.yAxis().bins();
//        double[][] means = new double[nx][ny];
//        for (int ix = 0; ix < nx; ix++) {
//            for (int iy = 0; iy < ny; iy++) {
//                means[ix][iy] = hist2D.binHeight(ix, iy) / hist2D.binEntries(ix, iy);
//            }
//        }
//        hist2D.reset();
//        for (int ix = 0; ix < nx; ix++) {
//            for (int iy = 0; iy < ny; iy++) {
//                double x = hist2D.xAxis().binCenter(ix);
//                double y = hist2D.yAxis().binCenter(iy);
//                hist2D.fill(x, y, means[ix][iy]);
//            }
//        }
//
//        IFitter fitter = AIDA.defaultInstance().analysisFactory().createFitFactory().createFitter("chi2");
//
//    }
//
//    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
//        double[] init = {h1d.maxBinHeight(), h1d.mean(), h1d.rms()};
//        IFitResult ifr = null;
//        try {
//            ifr = fitter.fit(h1d, "g", init, range);
//        } catch (RuntimeException ex) {
//            System.out.println(this.getClass().getSimpleName() + ":  caught exception in fitGaussian");
//        }
//        return ifr;
////        double[] init = {20.0, 0.0, 1.0, 20, -1};
////        return fitter.fit(h1d, "g+p1", init, range);
//    }
    @Override
    public void endOfData() {
        if (fitFile == null) {
            return;
        }

        IFitter fitter = aida.analysisFactory().createFitFactory().createFitter("chi2");
//        fitter.setFitMethod("CleverChiSquared");
//        fitter.setFitMethod("binnedMaximumLikelihood");

        PrintWriter fitWriter = null;
        try {
            fitWriter = new PrintWriter(fitFile);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PedestalPlots.class.getName()).log(Level.SEVERE, null, ex);
        }

        for (SiSensor sensor : hists.keySet()) {
            fitWriter.println(sensor.getName());
            IHistogram2D hist = hists.get(sensor);
            IHistogram1D fit = aida.histogram1D("1D fit", hist.yAxis().bins(), hist.yAxis().lowerEdge(), hist.yAxis().upperEdge());
            for (int i = 0; i < 640; i++) {
                fitWriter.format("%d\t", i);
                for (int y = 0; y < hist.yAxis().bins(); y++) {
                    for (int j = 0; j < hist.binHeight(i, y); j++) {
                        fit.fill(hist.binMeanY(i, y));
                    }
                }
                fitWriter.format("%f\t%f\t%f\t", fit.sumBinHeights(), fit.mean(), fit.rms());
                if (fit.sumBinHeights() > 100) {
                    IFitResult result = fitter.fit(fit, "g");

                    if (result.isValid()) {
                        fitWriter.format("%f\t%f\t", result.fittedParameter("mean"), result.fittedParameter("sigma"));
                    }
                }
                fitWriter.println();
                fit.reset();
            }
            fitWriter.flush();
        }
        fitWriter.close();
        aida.tree().rm("1D fit");
    }
}
