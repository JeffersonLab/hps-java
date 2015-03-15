package org.hps.monitoring.application;

import hep.aida.IPlotter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;

import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.plotting.MonitoringPlotFactory;

/**
 * This is the panel containing the tabs with the monitoring plots.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class PlotPanel extends JPanel implements ActionListener {
    
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

    /**
     * Get the indices of the current selected tabs.
     * @return The indices of the current tabs.
     */
    int[] getSelectedTabs() {
        int[] indices = new int[2];
        indices[0] = plotPane.getSelectedIndex();
        Component component = plotPane.getSelectedComponent();
        if (component instanceof JTabbedPane) {
            indices[1] = ((JTabbedPane)component).getSelectedIndex();
        } 
        return indices;
    }
    
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(Commands.SAVE_SELECTED_PLOTS)) {
            int[] indices = getSelectedTabs();
            IPlotter plotter = MonitoringPlotFactory.getPlotterRegistry().find(indices[0], indices[1]);
            if (plotter != null) {
                savePlotter(plotter);
            } else {
                DialogUtil.showErrorDialog(this, "Error Finding Plots", "No plots found in selected tab.");
            }
        }
    }
            
    static final String DEFAULT_FORMAT = "png";
    void savePlotter(IPlotter plotter) {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Plots - " + plotter.title());
        fc.setCurrentDirectory(new File("."));
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {                        
            String path = fc.getSelectedFile().getPath();
            if (path.lastIndexOf(".") == -1) {
                path += "." + DEFAULT_FORMAT;
            }
            try {
                plotter.writeToFile(path);
            } catch (IOException e) {
                e.printStackTrace();
                DialogUtil.showErrorDialog(this, "Error Saving Plots", "There was an error saving the plots.");
            }
        }        
    }
    
    void reset() {
        plotPane.removeAll();        
    }    
}