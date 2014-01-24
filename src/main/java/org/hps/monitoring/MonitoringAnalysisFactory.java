package org.hps.monitoring;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.renderer.xy.XYBarRenderer;

import hep.aida.IPlotterFactory;
import hep.aida.jfree.chart.DefaultChartTheme;
import hep.aida.ref.AnalysisFactory;

/**
 * This class implements the AIDA <code>IAnalysisFactory</code> for the monitoring application,
 * so that plots are automatically rendered into its tabs.  Its primary function is overriding
 * {@link #createPlotterFactory()} and {@link #createPlotterFactory(String)} to return a custom
 * <code>IPlotterFactory</code> object.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringAnalysisFactory.java,v 1.4 2013/12/10 07:36:40 jeremy Exp $
 */
public class MonitoringAnalysisFactory extends AnalysisFactory {

    //Map<String,IPlotterFactory> plotterFactories = new HashMap<String,IPlotterFactory>();
    
    public MonitoringAnalysisFactory() {
        System.out.println("MonitoringAnalysisFactory - ctor");
    }
    
    /**
     * Register this class as the default AnalysisFactory for AIDA by setting
     * the magic property string.
     */
    final static void register() {
        System.setProperty("hep.aida.IAnalysisFactory", MonitoringAnalysisFactory.class.getName());
    }
    
    public static void configure() {
        ChartFactory.setChartTheme(new DefaultChartTheme());
        XYBarRenderer.setDefaultShadowsVisible(false);
    }

    /**
     * Create a named plotter factory for the monitoring application.
     */
    public IPlotterFactory createPlotterFactory(String name) {
        System.out.println("createPlotterFactory - " + name);
        //if (!plotterFactories.containsKey(name)) {
        //    plotterFactories.put(name, new MonitoringPlotFactory(name));
        //}
        return new MonitoringPlotFactory(name);
        //return plotterFactories.get(name);
    }

    /**
     * Create an unnamed plotter factory for the monitoring application.
     */
    public IPlotterFactory createPlotterFactory() {
        //System.out.println("createPlotterFactory - w/o name");
        return new MonitoringPlotFactory(null);
    }
    
    /*
    public void clearPlotterFactories() {
        if (plotterFactories.size() > 0) {
            System.out.println("clearPlotterFactories - clearing " + plotterFactories.size() + " plotterFactories");
            plotterFactories.clear();
        } else {
            System.out.println("clearPlotterFactories - plotterFactories is empty");
        }
    }
    */
}
