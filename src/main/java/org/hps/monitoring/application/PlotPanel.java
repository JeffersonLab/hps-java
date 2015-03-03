package org.hps.monitoring.application;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * This is the panel containing the monitoring plots.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PlotPanel extends JPanel {
    private JTabbedPane plotPane;    
    
    public PlotPanel() {
        plotPane = new JTabbedPane();
    }
    
    JTabbedPane getPlotPane() {
        return plotPane;
    }
    
    void reset() {
        plotPane.removeAll();
    }
}