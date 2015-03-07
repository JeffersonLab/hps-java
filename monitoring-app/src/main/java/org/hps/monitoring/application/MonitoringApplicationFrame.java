package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsConfiguration;
import java.awt.Rectangle;

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

/**
 * This class instantiates the primary GUI components of the monitoring application.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class MonitoringApplicationFrame extends JFrame {
            
    RunPanel runPanel;    
    PlotPanel plotPanel;
    PlotInfoPanel plotInfoPanel;
    LogPanel logPanel;
    SystemStatusTable systemStatusTable;
    JPanel buttonsPanel;
    TriggerDiagnosticsPanel triggerPanel;
    
    JSplitPane mainSplitPane;
    JSplitPane rightSplitPane;
    JSplitPane leftSplitPane;
    
    DataSourceComboBox dataSourceComboBox;
    
    SettingsDialog settingsDialog;
    
    // Proportional layout parameters relative to the screen size.
    static final double FULL_SIZE = 1.0;
    static final double TOP_PANEL_HEIGHT = 0.05;
    static final double BOTTOM_PANEL_HEIGHT = FULL_SIZE - TOP_PANEL_HEIGHT;
    static final double LEFT_PANEL_WIDTH = 0.3;
    static final double RIGHT_PANEL_WIDTH = FULL_SIZE - LEFT_PANEL_WIDTH;
    static final double PLOT_PANEL_HEIGHT = 0.8;
    
    /**
     * 
     * @param listener
     */
    public MonitoringApplicationFrame(
            MonitoringApplication application) {
                
        // Create the content panel.
        JPanel contentPanel = new JPanel();
        setContentPane(contentPanel);
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(true);
        setProportionalSize(contentPanel, FULL_SIZE, FULL_SIZE);
        
        // Create the top panel.
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
        setProportionalSize(topPanel, FULL_SIZE, TOP_PANEL_HEIGHT);
        contentPanel.add(topPanel);
        
        // Create the connection status panel.
        JPanel connectionPanel = new ConnectionStatusPanel(application.connectionModel);
        topPanel.add(connectionPanel);
        
        // Add vertical separator.
        JSeparator sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(5, topPanel.getPreferredSize().height));
        topPanel.add(sep);
        
        // Create the buttons panel.
        buttonsPanel = new EventButtonsPanel(application.connectionModel, application);
        topPanel.add(buttonsPanel);
        
        // Add vertical separator.
        sep = new JSeparator(SwingConstants.VERTICAL);
        sep.setPreferredSize(new Dimension(5, topPanel.getPreferredSize().height));
        topPanel.add(sep);
        
        // Add the data source combo box.
        dataSourceComboBox = new DataSourceComboBox(application.configurationModel, application.connectionModel);
        topPanel.add(dataSourceComboBox);
        
        // Create the bottom panel.
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        setProportionalSize(bottomPanel, FULL_SIZE, BOTTOM_PANEL_HEIGHT);
        contentPanel.add(bottomPanel);
                                
        // Create the left panel.
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        setProportionalSize(leftPanel, LEFT_PANEL_WIDTH, FULL_SIZE);
                        
        // Create the run dashboard.
        runPanel = new RunPanel(application.runModel);

        // Create the tabbed pane for content in bottom of left panel such as log table and system monitor.
        JTabbedPane tableTabbedPane = new JTabbedPane();
        
        // Create the log table and add it to the tabs.
        logPanel = new LogPanel(application.configurationModel, application);
        tableTabbedPane.addTab("Log Messages", logPanel);
        
        // Create the system monitor.
        systemStatusTable = new SystemStatusTable();
        tableTabbedPane.addTab("System Status Monitor", new JScrollPane(systemStatusTable));
        
        // Add the trigger diagnostics tables.
        triggerPanel = new TriggerDiagnosticsPanel();
        tableTabbedPane.addTab("Trigger Diagnostics", triggerPanel);
        
        // Vertical split pane in left panel.
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, runPanel, tableTabbedPane);
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);
                                
        // Create the right panel.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        
        // Create the plot info panel.
        plotInfoPanel = new PlotInfoPanel();
                
        // Create the plot panel.
        plotPanel = new PlotPanel();
        plotPanel.setVisible(true); // DEBUG
        setProportionalSize(plotPanel, RIGHT_PANEL_WIDTH, PLOT_PANEL_HEIGHT);
        
        // Create the right panel vertical split pane for displaying plots and their information and statistics.
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plotPanel, plotInfoPanel);
        setProportionalSize(rightSplitPane, RIGHT_PANEL_WIDTH, FULL_SIZE);
        rightSplitPane.setResizeWeight(0.8);
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);
                       
        // Create the main horizontal split pane for dividing the left and right panels.
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplitPane.setResizeWeight(0.15);
        bottomPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        // Create the menu bar.
        MenuBar menu = new MenuBar(application.configurationModel, application.connectionModel, application);
        setJMenuBar(menu);
        dataSourceComboBox.addActionListener(menu);
                        
        // Setup the frame now that all components have been added.        
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true);
        
        // Setup the settings dialog box.
        settingsDialog = new SettingsDialog(application.configurationModel, application);
    }
    
    /**
     * Set the size of a Swing component using proportions of the current screen bounds.
     * @param component The component to resize.
     * @param scaleX The X scaling (must be between 0 and 1).
     * @param scaleY The Y scaling (must be between 0 and 1).
     * @param setSize Call the setSize method as well as setPreferredSize (which is the default).
     * @return
     */
    void setProportionalSize(JComponent component, double scaleX, double scaleY) {                    
        GraphicsConfiguration graphics = this.getGraphicsConfiguration();        
        Rectangle bounds = graphics.getBounds();        
        if (scaleX < 0 || scaleX > 1) {
            throw new IllegalArgumentException("scaleX must be > 0 and <= 1.");
        }
        if (scaleY < 0 || scaleY > 1) {
            throw new IllegalArgumentException("scaleY must be > 0 and <= 1.");
        }
        Dimension scaledDimension = new Dimension((int)(bounds.getWidth() * scaleX), (int)(bounds.getHeight() * scaleY));
        component.setPreferredSize(scaledDimension);
    }           
    
    /**
     * Restore default window settings.
     */
    void restoreDefaults() {
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        mainSplitPane.resetToPreferredSizes();
        leftSplitPane.resetToPreferredSizes();
        rightSplitPane.resetToPreferredSizes();        
    }    
}