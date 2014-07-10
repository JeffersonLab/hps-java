package org.hps.monitoring.plotting;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.renderer.xy.XYBarRenderer;

import hep.aida.IPlotterFactory;
import hep.aida.jfree.chart.DefaultChartTheme;
import hep.aida.ref.AnalysisFactory;

/**
 * This class implements the AIDA <code>IAnalysisFactory</code> for the monitoring application,
 * which puts plots into a series of tabs.  Each <code>IPlotter</code> has its own tab where
 * its regions are shown.  This class overrides {@link #createPlotterFactory()} and 
 * {@link #createPlotterFactory(String)} to return a custom <code>IPlotterFactory</code> object
 * that implements this behavior.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringAnalysisFactory.java,v 1.4 2013/12/10 07:36:40 jeremy Exp $
 */
public class MonitoringAnalysisFactory extends AnalysisFactory {
    
    public MonitoringAnalysisFactory() {
    }
    
    /**
     * Register this class as the default AnalysisFactory for AIDA by setting
     * the magic property string.
     */
    public final static void register() {
        System.setProperty("hep.aida.IAnalysisFactory", MonitoringAnalysisFactory.class.getName());
    }
    
    /**
     * Do some JFreeChart related configuration.
     */
    public static void configure() {
        ChartFactory.setChartTheme(new DefaultChartTheme());
        XYBarRenderer.setDefaultShadowsVisible(false);
    }

    /**
     * Create a named plotter factory for the monitoring application.
     */
    public IPlotterFactory createPlotterFactory(String name) {
        return new MonitoringPlotFactory(name);
    }

    /**
     * Create an unnamed plotter factory for the monitoring application.
     */
    public IPlotterFactory createPlotterFactory() {
        return new MonitoringPlotFactory(null);
    }
}
