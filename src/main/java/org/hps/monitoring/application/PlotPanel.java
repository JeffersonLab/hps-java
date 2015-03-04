package org.hps.monitoring.application;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;

/**
 * This is the panel containing the monitoring plots.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PlotPanel extends JPanel {
    
    private JTabbedPane plotPane;    
    
    public PlotPanel() {
        setLayout(new BorderLayout());
        plotPane = new JTabbedPane();
        plotPane.setPreferredSize(getPreferredSize());
        add(plotPane, BorderLayout.CENTER);
    }
    
    JTabbedPane getPlotPane() {
        return plotPane;
    }
    
    void reset() {
        plotPane.removeAll();
    }
}