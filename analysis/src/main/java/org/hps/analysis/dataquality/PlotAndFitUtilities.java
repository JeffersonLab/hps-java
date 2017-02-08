package org.hps.analysis.dataquality;

import hep.aida.IBaseHistogram;
import hep.aida.ICloud1D;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.IProfile2D;
import hep.aida.ref.plotter.style.registry.IStyleStore;
import hep.aida.ref.plotter.style.registry.StyleRegistry;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;

import org.lcsim.util.aida.AIDA;

public class PlotAndFitUtilities {

    private static Logger LOGGER = Logger.getLogger(PlotAndFitUtilities.class.getPackage().getName());

    static private AIDA aida = AIDA.defaultInstance();

    /*
     *  creates a new plotter with one region and puts the histogram in it
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */
    static IPlotter plot(IPlotterFactory plotterFactory, IBaseHistogram histogram, IPlotterStyle style, boolean show) {
        if (style == null) {
            style = getPlotterStyle(histogram);
        }
        IPlotter plotter = plotterFactory.create(histogram.title());
        plotter.createRegion();
        plotter.region(0).plot(histogram, style);
        if (show) {
            plotter.show();
        }
        return plotter;
    }

    /*
     *  puts a histogram on a plotter region with a  style
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */
    static void plot(IPlotter plotter, IBaseHistogram histogram, IPlotterStyle style, int region) {
        if (style == null) {
            style = getPlotterStyle(histogram);
        }
        LOGGER.info("Putting plot in region " + region);
        plotter.region(region).plot(histogram, style);

    }
    /*
     *  puts a function on a plotter region with a  style
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */

    static void plot(IPlotter plotter, IFunction function, IPlotterStyle style, int region) {
        if (style == null) {
            style = getPlotterStyle(function);
        }
        LOGGER.info("Putting function in region " + region);
        plotter.region(region).plot(function, style);
    }

    /*
     *  gets default plotter style based on histogram type
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */
    static IPlotterStyle getPlotterStyle(IBaseHistogram histogram) {
        StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        IPlotterStyle style = null;
        if ((histogram instanceof IHistogram1D) || (histogram instanceof ICloud1D) || (histogram instanceof IProfile1D)) {
            style = store.getStyle("DefaultHistogram1DStyle");
        } else if ((histogram instanceof IHistogram2D) || (histogram instanceof IProfile2D)) {
            style = store.getStyle("DefaultColorMapStyle");
            style.statisticsBoxStyle().setVisible(false);
            style.setParameter("hist2DStyle", "colorMap");
            style.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        }
        if (style == null) {
            throw new RuntimeException("A default style could not be found for " + histogram.title());
        }

        //custom stuff...mg
        style.dataStyle().errorBarStyle().setVisible(false);
        style.legendBoxStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(false);

        return style;
    }

    /*
     *  gets default plotter style for a function type
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */
    static IPlotterStyle getPlotterStyle(IFunction func) {
        StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        IPlotterStyle style = null;
        style = store.getStyle("DefaultFunctionStyle");
        if (style == null) {
            throw new RuntimeException("A default style could not be found for " + func.title());
        }
        return style;
    }

    static IFitResult performGaussianFit(IHistogram1D histogram) {
        IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
        IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
        IFunction function = functionFactory.createFunctionByName("Example Fit", "G");
        IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
        double[] parameters = new double[3];
        parameters[0] = histogram.maxBinHeight();
        parameters[1] = histogram.mean();
        parameters[2] = histogram.rms();
        function.setParameters(parameters);
        IFitResult fitResult = null;
        Logger minuitLogger = Logger.getLogger("org.freehep.math.minuit");
        minuitLogger.setLevel(Level.OFF);
        minuitLogger.info("minuit logger test");

        try {
            fitResult = fitter.fit(histogram, function);
        } catch (RuntimeException e) {
            LOGGER.info(e.getMessage());
        }
        return fitResult;
    }

    static void fitAndPutParameters(IHistogram1D hist, IFunction function) {
        IFitResult fr = performGaussianFit(hist);
        if (fr != null) {
            IFunction currentFitFunction = fr.fittedFunction();
            function.setParameters(currentFitFunction.parameters());
        }
    }

    private static final String nameStrip = "Tracker_TestRunModule_";

    private static String getNiceSensorName(HpsSiSensor sensor) {
        return sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens");
    }

    public static IHistogram1D getSensorPlot(String prefix, HpsSiSensor sensor) {
        String hname = prefix + getNiceSensorName(sensor);
        return aida.histogram1D(hname);
    }

//    private static IHistogram1D getSensorPlot(String prefix, String sensorName) {
//        return aida.histogram1D(prefix + sensorName);
//    }
    public static IHistogram1D createSensorPlot(String prefix, HpsSiSensor sensor, int nchan, double min, double max) {
        String hname = prefix + getNiceSensorName(sensor);
        IHistogram1D hist = aida.histogram1D(hname, nchan, min, max);
        hist.setTitle(getNiceSensorName(sensor));

        return hist;
    }

    public static IHistogram2D getSensorPlot2D(String prefix, HpsSiSensor sensor) {
        String hname = prefix + getNiceSensorName(sensor);
        return aida.histogram2D(hname);
    }

    public static IHistogram2D createSensorPlot2D(String prefix, HpsSiSensor sensor, int nchanX, double minX, double maxX, int nchanY, double minY, double maxY) {
        String hname = prefix + getNiceSensorName(sensor);
        IHistogram2D hist = aida.histogram2D(hname, nchanX, minX, maxX, nchanY, minY, maxY);
        hist.setTitle(sensor.getName().replaceAll(nameStrip, "")
                .replace("module", "mod")
                .replace("layer", "lyr")
                .replace("sensor", "sens"));

        return hist;
    }
}
