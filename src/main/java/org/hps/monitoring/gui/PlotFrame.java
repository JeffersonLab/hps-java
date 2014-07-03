package org.hps.monitoring.gui;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

/**
 * A <code>JFrame</code> where monitoring plots will show.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class PlotFrame extends JFrame {
    
    private JTabbedPane plotPane;
    
    PlotFrame() {
        plotPane = new JTabbedPane();
        setContentPane(plotPane);
        setTitle("Monitoring Plots");
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