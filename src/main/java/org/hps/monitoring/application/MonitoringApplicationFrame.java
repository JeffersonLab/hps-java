package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

import org.hps.monitoring.application.model.RunModel;

/**
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class MonitoringApplicationFrame extends JFrame {
            
    RunPanel runPanel;    
    PlotPanel plotPanel;
    PlotInfoPanel plotInfoPanel;
    LogTable logTable;
    
    GraphicsConfiguration graphics = this.getGraphicsConfiguration();
    Rectangle bounds = graphics.getBounds();
    
    /**
     * 
     * @param listener
     */
    public MonitoringApplicationFrame(ActionListener listener) {
                
        // Create the content panel.
        JPanel contentPanel = new JPanel();
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setPreferredSize(scaleBounds(1.0, 1.0));
        setContentPane(contentPanel);
        
        // Create the top panel.
        JPanel topPanel = new JPanel();
        topPanel.setPreferredSize(scaleBounds(1.0, 0.1));
                                
        // Create the left panel.
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        leftPanel.setPreferredSize(scaleBounds(0.3, 1.0));
                        
        // Create the run dashboard.
        runPanel = new RunPanel();

        // Create the tabbed pane for content in bottom of left panel such as log table and system monitor.
        JTabbedPane tableTabbedPane = new JTabbedPane();
        
        // Create the log table and add it to the tabs.
        logTable = new LogTable();                       
        tableTabbedPane.addTab("Log", new JScrollPane(logTable));
        
        // Create the system monitor.
        SystemStatusTable systemStatusTable = new SystemStatusTable();
        tableTabbedPane.addTab("System Status Monitor", new JScrollPane(systemStatusTable));
        
        // Vertical split pane in left panel.
        JSplitPane leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, runPanel, tableTabbedPane);
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);
                                
        // Create the right panel.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        
        // Create the plot info panel.
        plotInfoPanel = new PlotInfoPanel();
                
        // Create the plot panel.
        plotPanel = new PlotPanel();
        plotPanel.setPreferredSize(scaleBounds(0.7, 0.8));
        
        // Create the right panel vertical split pane for displaying plots and their information and statistics.
        JSplitPane rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plotPanel, plotInfoPanel);
        rightSplitPane.setPreferredSize(scaleBounds(0.7, 1.0));
        rightSplitPane.setResizeWeight(0.9);
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);
                       
        // Create the main horizontal split pane for dividing the left and right panels.
        JSplitPane mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        contentPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        // Create the menu bar.
        setJMenuBar(new MenuBar(listener));
        
        // Setup the frame now that all components have been added.
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
    }
    
    /**
     * Scale the screen bounds of the <code>JFrame</code> by the given proportions.
     * @param scaleX The X scaling (must be between 0 and 1).
     * @param scaleY The Y scaling (must be between 0 and 1).
     * @return
     */
    Dimension scaleBounds(double scaleX, double scaleY) {                
        if (scaleX < 0 || scaleX > 1) {
            throw new IllegalArgumentException("scaleX must be > 0 and <= 1.");
        }
        if (scaleY < 0 || scaleY > 1) {
            throw new IllegalArgumentException("scaleX must be > 0 and <= 1.");
        }
        return new Dimension((int)(bounds.getX() * scaleX), (int)(bounds.getY() * scaleX));
    }        
    
    /**
     * 
     * @param runModel
     */
    void setRunModel(RunModel runModel) {
        runPanel.setModel(runModel);
    }    
}