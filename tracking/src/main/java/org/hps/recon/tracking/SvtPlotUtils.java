package org.hps.recon.tracking;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ref.plotter.style.registry.IStyleStore;
import hep.aida.ref.plotter.style.registry.StyleRegistry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.compact.converter.HPSTrackerBuilder;
import org.lcsim.util.aida.AIDA;

public class SvtPlotUtils {

    private static final Logger logger = Logger.getLogger(SvtPlotUtils.class.getSimpleName());
    static private AIDA aida = AIDA.defaultInstance();
    
    public static int computePlotterRegion(HpsSiSensor sensor) {

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

    public static int computePlotterRegionAxialOnly(HpsSiSensor sensor) {
        int l =  HPSTrackerBuilder.getLayerFromVolumeName(sensor.getName());
        if(!sensor.isAxial()) throw new RuntimeException("not axial.");
        if( l < 4 ) {
            if (sensor.isTopLayer()) {
                return 6 * (l - 1);
            } else {
                return 6 * (l - 1) + 1;
            }
        } else {
            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (l - 4) + 2;
                } else {
                    return 6 * (l - 4) + 3;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 6 * (l - 4) + 4;
                } else {
                    return 6 * (l - 4) + 5;
                }
            }
        }

        return -1;
    }

    
    /**
     * Create a plotter style.
     *
     * @param plotterFactory
     * @param xAxisTitle : Title of the x axis
     * @param yAxisTitle : Title of the y axis
     * @return plotter style
     */
    // TODO: Move this to a utilities class
    public static IPlotterStyle createStyle(IPlotterFactory plotterFactory, String xAxisTitle, String yAxisTitle) {

        // Create a default style
        IPlotterStyle style = plotterFactory.createPlotterStyle();

        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);

        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
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
        style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
        style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        style.dataStyle().errorBarStyle().setVisible(false);

        // Turn off the legend
        style.legendBoxStyle().setVisible(false);

        return style;
    }

    public static IPlotterStyle createStyle(IPlotterFactory plotterFactory, HpsSiSensor sensor, String xAxisTitle, String yAxisTitle) {
        IPlotterStyle style = createStyle(plotterFactory, xAxisTitle, yAxisTitle);

        if (sensor.isTopLayer()) {
            style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
            style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        } else {
            style.dataStyle().fillStyle().setColor("93, 228, 47, 1");
            style.dataStyle().outlineStyle().setColor("93, 228, 47, 1");
        }

        return style;
    }

    public static int countSmallHits(List<RawTrackerHit> rawHits) {
        int smallHitCount = 0;
        Map<HpsSiSensor, Set<Integer>> hitMap = new HashMap<HpsSiSensor, Set<Integer>>();

        for (RawTrackerHit hit : rawHits) {
            HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
            Set<Integer> hitStrips = hitMap.get(sensor);
            if (hitStrips == null) {
                hitStrips = new HashSet<Integer>();
                hitMap.put(sensor, hitStrips);
            }
            int strip = hit.getIdentifierFieldValue("strip");
            hitStrips.add(strip);
        }

        for (RawTrackerHit hit : rawHits) {
            if (isSmallHit(hitMap, hit)) {
                smallHitCount++;
            }
        }
        return smallHitCount;
    }

    public static boolean isSmallHit(Map<HpsSiSensor, Set<Integer>> hitMap, RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int strip = hit.getIdentifierFieldValue("strip");
        double pedestal = sensor.getPedestal(strip, 0);
        double noise = sensor.getNoise(strip, 0);

        if (hitMap.get(sensor) != null && (hitMap.get(sensor).contains(strip - 1) || hitMap.get(sensor).contains(strip + 1))) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (hit.getADCValues()[i] > pedestal + 4.0 * noise) {
                return false;
            }
        }
        return true;
    }
    
    public static IFitResult performGaussianFit(IHistogram1D histogram) {
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
           logger.warning("fit failed");
        }
        return fitResult;
    }
    
    /*
     *  puts a function on a plotter region with a  style
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */

    public static void plot(IPlotter plotter, IFunction function, IPlotterStyle style, int region) {
        if (style == null)
            style = getPlotterStyle(function);
        logger.info("Putting function in region " + region);
        if(style != null)
            plotter.region(region).plot(function, style);
        else
            plotter.region(region).plot(function);
    }

    
    /*
     *  gets default plotter style for a function type
     *  copied from org.hps.monitoring.drivers.ecal.EcalMonitoringUtilities.java
     */
    public static IPlotterStyle getPlotterStyle(IFunction func) {
        StyleRegistry styleRegistry = StyleRegistry.getStyleRegistry();
        IStyleStore store = styleRegistry.getStore("DefaultStyleStore");
        if(store == null) {
            int n = styleRegistry.getAvailableStoreNames().length;
            if(n==0) return null;
            else store = styleRegistry.getStore(styleRegistry.getAvailableStoreNames()[0]);
        }
        IPlotterStyle style = null;
        style = store.getStyle("DefaultFunctionStyle");
        if (style == null) {
            int n = store.getAllStyleNames().length;
            if(n==0) return null;
            else style = store.getStyle(store.getAllStyleNames()[0]);
        }
        return style;
    }
}
