package org.lcsim.hps.monitoring.deprecated;

import hep.aida.*;
import hep.aida.ref.plotter.PlotterUtilities;
import java.awt.BorderLayout;
import javax.swing.*;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: AIDAFrame.java,v 1.1 2013/10/25 19:41:01 jeremy Exp $
 * @deprecated
 */
@Deprecated
public class AIDAFrame extends JFrame {

    JPanel controlsPanel;
    JMenuBar menubar;
    JTabbedPane tabbedPane;

    public AIDAFrame() {
        tabbedPane = new JTabbedPane();
        this.getContentPane().setLayout(new BorderLayout());

        menubar = new JMenuBar();
        this.setJMenuBar(menubar);

        this.add(tabbedPane, BorderLayout.CENTER);

        controlsPanel = new JPanel();
        controlsPanel.setLayout(new BoxLayout(controlsPanel, BoxLayout.X_AXIS));
        this.add(controlsPanel, BorderLayout.SOUTH);
    }

    public void addPlotter(IPlotter plotter) {
        JPanel plotterPanel = new JPanel(new BorderLayout());
        // Now embed the plotter
        plotterPanel.add(PlotterUtilities.componentForPlotter(plotter), BorderLayout.CENTER);
        tabbedPane.add(plotter.title(), plotterPanel);
    }

    public JTabbedPane getTabbedPane() {
        return tabbedPane;
    }

    public JPanel getControlsPanel() {
        return controlsPanel;
    }

    public JMenuBar getMenubar() {
        return menubar;
    }
}