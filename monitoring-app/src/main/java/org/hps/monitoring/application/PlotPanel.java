package org.hps.monitoring.application;

import hep.aida.IPlotter;

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
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class PlotPanel extends JPanel implements ActionListener {

    /**
     * The tabs containing the plots.
     */
    private final JTabbedPane plotPane;

    /**
     * Class constructor.
     */
    public PlotPanel() {
        this.setLayout(new BorderLayout());
        this.plotPane = new JTabbedPane();
        this.plotPane.setPreferredSize(this.getPreferredSize());
        this.add(this.plotPane, BorderLayout.CENTER);
    }

    /**
     * Handle an {@link java.awt.event.ActionEvent}.
     *
     * @param event the {@link java.awt.event.ActionEvent} to handle
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (event.getActionCommand().equals(Commands.SAVE_SELECTED_PLOTS)) {
            final int[] indices = this.getSelectedTabIndices();
            final IPlotter plotter = MonitoringPlotFactory.getPlotterRegistry().find(indices[0], indices[1]);
            if (plotter != null) {
                this.savePlotter(plotter);
            } else {
                DialogUtil.showErrorDialog(this, "Error Finding Plots", "No plots found in selected tab.");
            }
        }
    }

    /**
     * Get the tabbed pane with the plots.
     *
     * @return the tabbed pane with the plots
     */
    JTabbedPane getPlotPane() {
        return this.plotPane;
    }

    /**
     * Get the currently selected plot tab.
     *
     * @return the currently selected plot tab
     */
    Component getSelectedTab() {
        return ((JTabbedPane) this.plotPane.getSelectedComponent()).getSelectedComponent();
    }

    /**
     * Get the indices of the current selected tabs.
     *
     * @return The indices of the current tabs.
     */
    private int[] getSelectedTabIndices() {
        final int[] indices = new int[2];
        indices[0] = this.plotPane.getSelectedIndex();
        final Component component = this.plotPane.getSelectedComponent();
        if (component instanceof JTabbedPane) {
            indices[1] = ((JTabbedPane) component).getSelectedIndex();
        }
        return indices;
    }

    /**
     * Remove all tabs from the plot pane.
     */
    void reset() {
        this.plotPane.removeAll();
    }

    /**
     * Save the plotter from a tab to a graphics file.
     * 
     * @param plotter the plotter to save
     */
    private void savePlotter(final IPlotter plotter) {
        final JFileChooser fc = new JFileChooser();
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogTitle("Save Plots - " + plotter.title());
        fc.setCurrentDirectory(new File("."));
        fc.setAcceptAllFileFilterUsed(false);
        fc.setFileFilter(new FileNameExtensionFilter("PNG file", "png"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("JPG file", "jpg"));
        fc.addChoosableFileFilter(new FileNameExtensionFilter("GIF file", "gif"));
        final int r = fc.showSaveDialog(this);
        if (r == JFileChooser.APPROVE_OPTION) {
            String path = fc.getSelectedFile().getPath();
            final FileNameExtensionFilter filter = (FileNameExtensionFilter) fc.getFileFilter();
            if (!path.endsWith("." + filter.getExtensions()[0])) {
                path += "." + filter.getExtensions()[0];
            }
            final BufferedImage image = ExportPdf.getImage(this.getSelectedTab());
            try {
                ImageIO.write(image, filter.getExtensions()[0], new File(path));
                DialogUtil.showInfoDialog(this, "Plots Saved", "Plots from panel were saved to" + '\n' + path);
            } catch (final IOException e) {
                e.printStackTrace();
                DialogUtil.showErrorDialog(this, "Error Saving Plots", "There was an error saving the plots.");
            }
        }
    }
}
