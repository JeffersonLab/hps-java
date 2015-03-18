package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
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
            
    EventDashboard dashboardPanel;    
    PlotPanel plotPanel;
    PlotInfoPanel plotInfoPanel;
    LogPanel logPanel;
    JPanel buttonsPanel;
    TriggerDiagnosticsPanel triggerPanel;
    ConditionsPanel conditionsPanel;
    SystemStatusPanel systemStatusPanel;
    MenuBar menu; 
    
    JSplitPane mainSplitPane;
    JSplitPane rightSplitPane;
    JSplitPane leftSplitPane;
    
    DataSourceComboBox dataSourceComboBox;
    
    SettingsDialog settingsDialog;
       
    static final Rectangle BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
    static final int PIXEL_WIDTH_MAX = (int) BOUNDS.getWidth();
    static final int PIXEL_HEIGHT_MAX = (int) BOUNDS.getHeight();
    
    /**
     * 
     * @param listener
     */
    public MonitoringApplicationFrame(
            MonitoringApplication application) {
        
        // Disable interaction until specifically enabled externally after initialization.
        setEnabled(false);
                
        // Create the content panel.
        JPanel contentPanel = new JPanel();
        setContentPane(contentPanel);
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setOpaque(true);
        contentPanel.setPreferredSize(new Dimension(PIXEL_WIDTH_MAX, PIXEL_HEIGHT_MAX));
                
        // Create the top panel.
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 20, 0));
        contentPanel.add(topPanel, BorderLayout.NORTH);
                
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
        topPanel.add(sep);
        
        // Add the data source combo box.
        dataSourceComboBox = new DataSourceComboBox(application.configurationModel, application.connectionModel);
        topPanel.add(dataSourceComboBox);
        
        // Create the bottom panel.
        JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        contentPanel.add(bottomPanel, BorderLayout.CENTER);
                                        
        // Create the left panel.
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
                            
        // Create the run dashboard.
        dashboardPanel = new EventDashboard(application.runModel);

        // Create the tabbed pane for content in bottom of left panel such as log table and system monitor.
        JTabbedPane tableTabbedPane = new JTabbedPane();
        
        // Create the log table and add it to the tabs.
        logPanel = new LogPanel(application.configurationModel, application);
        tableTabbedPane.addTab("Log Messages", logPanel);
        
        // Create the system monitor.
        //systemStatusTable = new SystemStatusTable();
        systemStatusPanel = new SystemStatusPanel();
        tableTabbedPane.addTab("System Status Monitor", systemStatusPanel);
        
        // Add the trigger diagnostics tables.
        triggerPanel = new TriggerDiagnosticsPanel();
        tableTabbedPane.addTab("Trigger Diagnostics", triggerPanel);
        
        // Add the conditions panel.
        conditionsPanel = new ConditionsPanel();
        tableTabbedPane.addTab("Detector Conditions", conditionsPanel);
        
        // Vertical split pane in left panel.
        leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dashboardPanel, tableTabbedPane);
        leftSplitPane.setDividerLocation(250);
        leftPanel.add(leftSplitPane, BorderLayout.CENTER);
                                
        // Create the right panel.
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
                
        // Create the plot info panel.
        plotInfoPanel = new PlotInfoPanel();
                
        // Create the plot panel.
        plotPanel = new PlotPanel();        
        plotInfoPanel.saveButton.addActionListener(plotPanel);
        
        // Create the right panel vertical split pane for displaying plots and their information and statistics.
        rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, plotPanel, plotInfoPanel);
        rightSplitPane.setResizeWeight(0.7);
        rightPanel.add(rightSplitPane, BorderLayout.CENTER);
                       
        // Create the main horizontal split pane for dividing the left and right panels.
        mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        mainSplitPane.setDividerLocation(PIXEL_WIDTH_MAX / 2);
        bottomPanel.add(mainSplitPane, BorderLayout.CENTER);
        
        // Create the menu bar.
        menu = new MenuBar(application.configurationModel, application.connectionModel, application);
        setJMenuBar(menu);
        dataSourceComboBox.addActionListener(menu);
        
        // Setup the settings dialog box (invisible until activated).
        settingsDialog = new SettingsDialog(application.configurationModel, application);        
               
        // Setup the frame now that all components have been added.        
        pack();
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setVisible(true); 
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