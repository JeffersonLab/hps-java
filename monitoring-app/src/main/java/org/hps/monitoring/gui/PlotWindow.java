package org.hps.monitoring.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/**
 * A <code>JFrame</code> where monitoring plots will show in tabs.
 */
class PlotWindow extends ApplicationWindow {
    
    private JTabbedPane plotPane;
    
    PlotWindow() {
        super("Monitoring Plots");
        plotPane = new JTabbedPane();
        setContentPane(plotPane);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setResizable(true);
        pack();           
    }
    
    void reset() {
        plotPane.removeAll();
    }       
    
    JTabbedPane getPlotPane() {
        return plotPane;
    }
}