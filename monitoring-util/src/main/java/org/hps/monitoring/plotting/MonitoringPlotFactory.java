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
import org.jfree.data.time.RegularTimePeriod;

/**
 * This class implements an AIDA <code>IPlotterFactory</code> for the monitoring application. It
 * extends the JFree plotter by putting plots into tabs. Each plotter factory is given its own
 * top-level tab in a root tabbed pane, under which are separate tabs for each plotter. The root
 * pane is static and shared across all plotter factories. The top level component is set externally
 * by the MonitoringApplication before any calls to AIDA are made from Drivers.
 */
public class MonitoringPlotFactory extends PlotterFactory {
    
    // Global plotter registry.
    static PlotterRegistry plotters = new PlotterRegistry();
    
    // The name of the factory which will be used in naming tabs in the monitoring app.
    String name = null;

    // The GUI tabs for this factory's plots.
    private JTabbedPane tabs = new JTabbedPane();

    // Root pane where this factory's top-level tab will be inserted.
    private static JTabbedPane rootPane = null;

    // The region listener for handling mouse clicks in a region.
    private static PlotterRegionListener regionListener;
    
    // The current tab index.
    int tabIndex;

    /**
     * Set the plot region listener.
     * @param regionListener The plot region listener.
     */
    public static void setPlotterRegionListener(PlotterRegionListener regionListener) {
        MonitoringPlotFactory.regionListener = regionListener;
    }

    /**
     * Class constructor for unnamed factory.
     */
    MonitoringPlotFactory() {
        super();
        setIsEmbedded(true);
        setupRootPane("  ");
        if (regionListener != null)
            addPlotterRegionListener(regionListener);
    }

    /**
     * Class constructor for named factory.
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

    /**
     * Setup the root GUI pane of this factory for display of plots in tabs.
     * @param name The tab's label.
     */
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

    /**
     * Setup a plotter tab.
     * @param plotterName The name of the plotter.
     * @param plotter The IPlotter which will plot into the tab.
     */
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

    /**
     * Add a <code>JFreeChart</code> to one of the tabs.
     * @param chart The JFreeChart object to add.
     */
    private void addChart(JFreeChart chart) {
        ChartPanel panel = new ChartPanel(chart);
        tabs.addTab(chart.getTitle().getText(), panel);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, new JLabel(chart.getTitle().getText()));
    }
    
    /**
     * This creates a strip chart with full parameter settings, which will automatically
     * update at a certain time interval.
     * @param name The title of the chart.
     * @param rangeLabel The range axis label text.
     * @param seriesCount The number of series in the data set.
     * @param seriesNames The names of the series (if non-null the length must match seriesCount).
     * @param itemCount The maximum number of items in the series.
     * @param timeBase The time unit for updates.
     * @param valueProvider The interface for providing the series values.
     * @param rangeView The view in the domain axis around the current data point (applied to plus and minus).
     * @return The StripChartUpdater for the chart.
     */
    public StripChartUpdater createStripChart(
            String name, 
            String rangeLabel,
            int seriesCount, 
            String[] seriesNames,
            int itemCount,
            RegularTimePeriod timeBase,
            ValueProvider valueProvider,
            long rangeView) {
        StripChartUpdater updater = StripChartBuilder.createStripChart(
                name, 
                rangeLabel, 
                seriesCount, 
                seriesNames,
                itemCount, 
                timeBase, 
                valueProvider, 
                rangeView);
        addChart(updater.getChart());
        return updater;
    }
    
    /**
     * Create a strip chart with simple parameter settings.
     * @param name The title of the strip chart.
     * @param seriesCount The number of series in the data set.
     * @param timeBase The time interval for updating.
     * @param valueProvider The interface for providing values.
     * @return The StripChartUpdater for the chart.
     */
    public StripChartUpdater createStripChart(
            String name, 
            int seriesCount, 
            RegularTimePeriod timeBase,
            ValueProvider valueProvider) {
        return createStripChart(
                name, "Values", 
                seriesCount, 
                null, 
                9999, 
                timeBase, 
                valueProvider, 
                10000L);
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
     * Get the global registry of plotters.
     * @return The global plotter registry.
     */
    public static PlotterRegistry getPlotterRegistry() {
        return plotters;
    }
}