package org.hps.monitoring.application;

import hep.aida.IPlotter;
import hep.aida.jfree.plotter.Plotter;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.hps.monitoring.application.util.DialogUtil;
import org.hps.monitoring.plotting.ExportPdf;
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
    int[] getSelectedTabIndices() {
        int[] indices = new int[2];
        indices[0] = plotPane.getSelectedIndex();
        Component component = plotPane.getSelectedComponent();
        if (component instanceof JTabbedPane) {
            indices[1] = ((JTabbedPane)component).getSelectedIndex();
        } 
        return indices;
    }
    
    Component getSelectedTab() {
        return ((JTabbedPane) plotPane.getSelectedComponent()).getSelectedComponent();
    }
    
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(Commands.SAVE_SELECTED_PLOTS)) {
            int[] indices = getSelectedTabIndices();
            IPlotter plotter = MonitoringPlotFactory.getPlotterRegistry().find(indices[0], indices[1]);
            if (plotter != null) {
                savePlotter(plotter);
            } else {
                DialogUtil.showErrorDialog(this, "Error Finding Plots", "No plots found in selected tab.");
            }
        }
    }
            
    void savePlotter(IPlotter plotter) {
        JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Plots - " + plotter.title());
        fc.setCurrentDirectory(new File("."));
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("PNG file", "png"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JPG file", "jpg"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("GIF file", "gif"));
        int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {                        
            String path = fc.getSelectedFile().getPath();
            FileNameExtensionFilter filter = (FileNameExtensionFilter) fc.getFileFilter();
            if (!path.endsWith("." + filter.getExtensions()[0])) {
                path += "." + filter.getExtensions()[0];
            }
            BufferedImage image = ExportPdf.getImage(getSelectedTab());
            try {
                ImageIO.write(image, filter.getExtensions()[0], new File(path));
                DialogUtil.showInfoDialog(this, "Plots Saved", "Plots from panel were saved to" + '\n' + path);
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