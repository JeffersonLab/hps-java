package org.lcsim.hps.monitoring;

import hep.aida.IPlotterFactory;
import hep.aida.ref.AnalysisFactory;

/**
 * This class implements the AIDA <code>IAnalysisFactory</code> for the monitoring application,
 * so that plots are automatically rendered into its tabs.  Its primary function is overriding
 * {@link #createPlotterFactory()} and {@link #createPlotterFactory(String)} to return a custom
 * <code>IPlotterFactory</code> object.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringAnalysisFactory.java,v 1.3 2013/11/06 19:19:56 jeremy Exp $
 */
public class MonitoringAnalysisFactory extends AnalysisFactory {

    /**
     * Register this class as the default AnalysisFactory for AIDA by setting
     * the magic property string.
     */
    final static void register() {
        System.setProperty("hep.aida.IAnalysisFactory", MonitoringAnalysisFactory.class.getName());
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