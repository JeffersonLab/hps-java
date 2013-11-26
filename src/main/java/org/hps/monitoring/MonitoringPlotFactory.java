package org.hps.monitoring;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.PlotterFactory;
import hep.aida.ref.plotter.PlotterUtilities;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * This class implements an AIDA IPlotterFactory for the monitoring application. 
 * It extends the JFree plotter by putting plots into tabs. Each plotter factory 
 * is given its own top-level tab in a root tabbed pane, under which are separate tabs 
 * for each plotter. The root pane is static and shared across all plotter factories. 
 * It is set externally by the MonitoringApplication before any calls to AIDA are made 
 * from Drivers.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: MonitoringPlotFactory.java,v 1.5 2013/11/06 19:19:56 jeremy Exp $
 */
class MonitoringPlotFactory extends PlotterFactory {

    /*
     * The name of the factory which will be used in naming tabs in the monitoring app.
     */
    String name = null;

    // The GUI tabs for this factory's plots.
    private JTabbedPane tabs = new JTabbedPane();

    // Root pane where this factory's top-level tab will be inserted.
    private static JTabbedPane rootPane = null;

    /**
     * Class constructor.
     */
    MonitoringPlotFactory() {
        super();
        rootPane.addTab("", tabs);
        rootPane.setTabComponentAt(rootPane.getTabCount() - 1, new JLabel("  "));
    }

    /**
     * Class constructor.
     * @param name The name of the factory.
     */
    MonitoringPlotFactory(String name) {
        super();
        this.name = name;
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
        JPanel plotterPanel = new JPanel(new BorderLayout());
        plotterPanel.add(PlotterUtilities.componentForPlotter(plotter), BorderLayout.CENTER);
        tabs.addTab(plotterName, plotterPanel);
        tabs.setTabComponentAt(tabs.getTabCount() - 1, new JLabel(plotterName));
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
}