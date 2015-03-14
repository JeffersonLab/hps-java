package org.hps.monitoring.plotting;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.PlotterFactory;
import hep.aida.jfree.plotter.PlotterRegionListener;
import hep.aida.ref.plotter.PlotterUtilities;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * This class implements an AIDA <code>IPlotterFactory</code> for the monitoring application. It
 * extends the JFree plotter by putting plots into tabs. Each plotter factory is given its own
 * top-level tab in a root tabbed pane, under which are separate tabs for each plotter. The root
 * pane is static and shared across all plotter factories. The top level component is set externally
 * by the MonitoringApplication before any calls to AIDA are made from Drivers.
 */
public class MonitoringPlotFactory extends PlotterFactory {
    
    static PlotterRegistry plotters = new PlotterRegistry();
    
    // The name of the factory which will be used in naming tabs in the monitoring app.
    String name = null;

    // The GUI tabs for this factory's plots.
    private JTabbedPane tabs = new JTabbedPane();

    // Root pane where this factory's top-level tab will be inserted.
    private static JTabbedPane rootPane = null;

    private static PlotterRegionListener regionListener;
    
    int tabIndex;

    public static void setPlotterRegionListener(PlotterRegionListener regionListener) {
        MonitoringPlotFactory.regionListener = regionListener;
    }

    /**
     * Class constructor.
     */
    MonitoringPlotFactory() {
        super();
        setIsEmbedded(true);
        setupRootPane("  ");
        if (regionListener != null)
            addPlotterRegionListener(regionListener);
    }

    /**
     * Class constructor.
     * @param name The name of the factory.
     */
    MonitoringPlotFactory(String name) {
        super();
        this.name = name;
        setIsEmbedded(true);
        setupRootPane(name);
        if (regionListener != null)
            addPlotterRegionListener(regionListener);
    }

    private void setupRootPane(String name) {
        // FIXME: Hack to disregard call from an AIDA related class.
        if (!(new RuntimeException()).getStackTrace()[2].getClassName().equals("hep.aida.ref.plotter.style.registry.StyleStoreXMLReader")) {
            rootPane.addTab(name, tabs);
            tabIndex = rootPane.getTabCount() - 1;
            rootPane.setTabComponentAt(tabIndex, new JLabel(name));
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
    public static void setRootPane(JTabbedPane rootPane) {
        MonitoringPlotFactory.rootPane = rootPane;
    }

    private void setupPlotterTab(String plotterName, IPlotter plotter) {
        
        // Setup the plotter's GUI pane and tab.
        JPanel plotterPanel = new JPanel(new BorderLayout());
        plotterPanel.add(PlotterUtilities.componentForPlotter(plotter), BorderLayout.CENTER);
        tabs.addTab(plotterName, plotterPanel);
        int plotterIndex = tabs.getTabCount() - 1;
        tabs.setTabComponentAt(plotterIndex, new JLabel(plotterName));
        
        // Register plotter globally with its tab indices.
        plotters.register(plotter, tabIndex, plotterIndex);
    }

    private void addChart(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        tabs.addTab(chart.getTitle().getText(), panel);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, new JLabel(chart.getTitle().getText()));
    }

    /**
     * Create a strip chart using a JFreeChart implementation. It will be automatically updated from
     * a {@link StripChartUpdater}. Similar to AIDA plots, the chart will be given a sub-tab in the
     * tab of this factory.
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

    /**
     * Create a strip chart which must be updated manually.
     * @param title The title of the chart.
     * @param yAxisLabel The y axis label.
     * @param maxAge The maximum age of items to keep.
     * @param maxCount The maximum count of items.
     * @param rangeSize The size of the data range.
     * @return The strip chart as a <code>JFreeChart</code>.
     */
    public JFreeChart createStripChart(String title, String yAxisLabel, int maxAge, int maxCount, int rangeSize) {
        JFreeChart stripChart = StripChartBuilder.createTimeSeriesChart(title, yAxisLabel, maxAge, maxCount, rangeSize);
        stripChart.getLegend().setVisible(false); /* Legend turned off for now. */
        addChart(stripChart);
        return stripChart;
    }       
    
    /**
     * 
     * @return
     */
    public static PlotterRegistry getPlotterRegistry() {
        return plotters;
    }
}