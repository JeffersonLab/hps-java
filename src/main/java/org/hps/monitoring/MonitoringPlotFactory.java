package org.hps.monitoring;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.PlotterFactory;
import hep.aida.ref.plotter.PlotterUtilities;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.hps.monitoring.plotting.StripChartBuilder;
import org.hps.monitoring.plotting.StripChartUpdater;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * This class implements an AIDA IPlotterFactory for the monitoring application. 
 * It extends the JFree plotter by putting plots into tabs. Each plotter factory 
 * is given its own top-level tab in a root tabbed pane, under which are separate tabs 
 * for each plotter. The root pane is static and shared across all plotter factories. 
 * It is set externally by the MonitoringApplication before any calls to AIDA are made 
 * from Drivers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringPlotFactory.java,v 1.6 2013/12/10 07:36:40 jeremy Exp $
 */
// FIXME: Move to plotting package.
public class MonitoringPlotFactory extends PlotterFactory {

    /* The name of the factory which will be used in naming tabs in the monitoring app. */
    String name = null;

    /* The GUI tabs for this factory's plots. */
    private JTabbedPane tabs = new JTabbedPane();

    /* Root pane where this factory's top-level tab will be inserted. */
    private static JTabbedPane rootPane = null;

    /**
     * Class constructor.
     */
    MonitoringPlotFactory() {
        super();
        
        /* Enable embedded mode. */
        setEmbedded(true);

        /* Setup the root pane by adding a tab for this factory. */
        setupRootPane("  ");
    }

    /**
     * Class constructor.
     * @param name The name of the factory.
     */
    MonitoringPlotFactory(String name) {
        super();
        this.name = name;
        
        setEmbedded(true);
        
        setupRootPane(name);
    }

    private void setupRootPane(String name) {
        // FIXME: hack
        if (!(new RuntimeException()).getStackTrace()[2].getClassName()
                .equals("hep.aida.ref.plotter.style.registry.StyleStoreXMLReader")) {
            rootPane.addTab(name, tabs);
            rootPane.setTabComponentAt(rootPane.getTabCount() - 1, new JLabel(name));
        }
    }

    /**
     * Create a named plotter.
     * @param plotterName The name of the plotter.
     * @return The plotter.
     */
    public IPlotter create(String plotterName) {
        IPlotter plotter = super.create(plotterName);
        setupPlotterTab(plotterName, plotter);
        return plotter;
    }

    /**
     * Create an unnamed plotter.
     * @return The plotter.
     */
    public IPlotter create() {
        return create((String) null);
    }

    /**
     * Set the reference to the root tab pane where this factory's GUI tabs will be inserted.
     * @param rootPane The root tabbed pane.
     */
    static void setRootPane(JTabbedPane rootPane) {
        MonitoringPlotFactory.rootPane = rootPane;
    }
    
    private void setupPlotterTab(String plotterName, IPlotter plotter) {
        JPanel plotterPanel = new JPanel(new BorderLayout());
        plotterPanel.add(PlotterUtilities.componentForPlotter(plotter), BorderLayout.CENTER);
        tabs.addTab(plotterName, plotterPanel);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, new JLabel(plotterName));
    }    
    
    private void addChart(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        tabs.addTab(chart.getTitle().getText(), panel);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, new JLabel(chart.getTitle().getText()));
    }
    
    /**
     * Create a strip chart with a pure JFreeChart implementation.     
     * It will be automatically updated from a {@link StripChartUpdater}.    
     * Similar to AIDA plots, the chart will be given a sub-tab in the tab 
     * of this factory.
     * 
     * @param title The title of the chart.
     * @param yAxisLabel The y axis label.
     * @param size The buffer size of the series which determines how much data displays.
     * @param updater The updater which will update the chart in real time.
     * @return The modified <tt>StripChartUpdater</tt> which points to the new chart.
     */
    public StripChartUpdater createStripChart(String title, String yAxisLabel, int size, StripChartUpdater updater) {
        JFreeChart stripChart = StripChartBuilder.createDynamicTimeSeriesChart(title, yAxisLabel, size);
        stripChart.getLegend().setVisible(false); /* Legend turned off for now. */
        addChart(stripChart);
        updater.setChart(stripChart);
        return updater;
    }      
}
