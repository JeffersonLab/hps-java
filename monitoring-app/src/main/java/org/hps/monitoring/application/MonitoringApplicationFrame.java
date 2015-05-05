package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;

/**
 * This class instantiates the primary GUI components of the monitoring application.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class MonitoringApplicationFrame extends JFrame {

    /**
     * The current graphics bounds.
     */
    private static final Rectangle BOUNDS = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

    /**
     * The maximum height of a window in pixels.
     */
    private static final int PIXEL_HEIGHT_MAX = (int) BOUNDS.getHeight();

    /**
     * The maximum width of a window in pixels.
     */
    private static final int PIXEL_WIDTH_MAX = (int) BOUNDS.getWidth();

    /**
     * The conditions panel.
     */
    private final ConditionsPanel conditionsPanel;

    /**
     * The dashboard panel.
     */
    private final EventDashboard dashboardPanel;

    /**
     * The left split pane that divides the dashboard and the tabs.
     */
    private final JSplitPane leftSplitPane;

    /**
     * The log panel.
     */
    private final LogPanel logPanel;

    /**
     * The primary split pain dividing the left and right panels.
     */
    private final JSplitPane mainSplitPane;

    /**
     * The application's menu bar.
     */
    private final MenuBar menu;

    /**
     * The plot info panel.
     */
    private final PlotInfoPanel plotInfoPanel;

    /**
     * The plot panel.
     */
    private final PlotPanel plotPanel;

    /**
     * The right split pane showing plots and statistics.
     */
    private final JSplitPane rightSplitPane;

    /**
     * The settings dialog window.
     */
    private final SettingsDialog settingsDialog;

    /**
     * The system status panel which shows under the tabs.
     */
    private final SystemStatusPanel systemStatusPanel;

    /**
     * The toolbar panel with the buttons, data source and connection information.
     */
    private final ToolbarPanel toolbarPanel;

    /**
     * The trigger diagnostics panel.
     */
    private final TriggerDiagnosticsPanel triggerPanel;

    /**
     * Class constructor.
     *
     * @param application the associated application object
     */
    public MonitoringApplicationFrame(final MonitoringApplication application) {

        // Disable interaction until specifically enabled externally after initialization.
        this.setEnabled(false);

        // Create the content panel.
        final JPanel contentPanel = new JPanel();
        this.setContentPane(contentPanel);
        contentPanel.setLayout(new BorderLayout());
        contentPanel.setOpaque(true);
        contentPanel.setPreferredSize(new Dimension(PIXEL_WIDTH_MAX, PIXEL_HEIGHT_MAX));

        // Create the top panel.
        this.toolbarPanel = new ToolbarPanel(application.getConfigurationModel(), application.getConnectionModel(),
                application);
        contentPanel.add(this.toolbarPanel, BorderLayout.NORTH);

        // Create the bottom panel.
        final JPanel bottomPanel = new JPanel();
        bottomPanel.setLayout(new BorderLayout());
        contentPanel.add(bottomPanel, BorderLayout.CENTER);

        // Create the left panel.
        final JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        // Create the run dashboard.
        this.dashboardPanel = new EventDashboard(application.getRunModel());

        // Create the tabbed pane for content in bottom of left panel such as log table and system monitor.
        final JTabbedPane tableTabbedPane = new JTabbedPane();

        // Create the log table and add it to the tabs.
        this.logPanel = new LogPanel(application.getConfigurationModel(), application);
        tableTabbedPane.addTab("Log Messages", this.logPanel);

        // Create the system monitor.
        // systemStatusTable = new SystemStatusTable();
        this.systemStatusPanel = new SystemStatusPanel();
        tableTabbedPane.addTab("System Status Monitor", this.systemStatusPanel);

        // Add the trigger diagnostics tables.
        this.triggerPanel = new TriggerDiagnosticsPanel();
        tableTabbedPane.addTab("Trigger Diagnostics", this.triggerPanel);

        // Add the conditions panel.
        this.conditionsPanel = new ConditionsPanel();
        tableTabbedPane.addTab("Detector Conditions", this.conditionsPanel);

        // Vertical split pane in left panel.
        this.leftSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.dashboardPanel, tableTabbedPane);
        this.leftSplitPane.setDividerLocation(250);
        leftPanel.add(this.leftSplitPane, BorderLayout.CENTER);

        // Create the right panel.
        final JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());

        // Create the plot info panel.
        this.plotInfoPanel = new PlotInfoPanel();

        // Create the plot panel.
        this.plotPanel = new PlotPanel();
        this.plotInfoPanel.addActionListener(this.plotPanel);

        // Create the right panel vertical split pane for displaying plots and their information and statistics.
        this.rightSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, this.plotPanel, this.plotInfoPanel);
        this.rightSplitPane.setDividerLocation(680);
        this.rightSplitPane.setOneTouchExpandable(true);
        rightPanel.add(this.rightSplitPane, BorderLayout.CENTER);

        // Create the main horizontal split pane for dividing the left and right panels.
        this.mainSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        this.mainSplitPane.setDividerLocation(600);
        this.mainSplitPane.setOneTouchExpandable(true);
        bottomPanel.add(this.mainSplitPane, BorderLayout.CENTER);

        // Create the menu bar.
        this.menu = new MenuBar(application.getConfigurationModel(), application.getConnectionModel(), application);
        this.setJMenuBar(this.menu);
        this.toolbarPanel.getDataSourceComboBox().addActionListener(this.menu);

        // Setup the settings dialog box (invisible until activated).
        this.settingsDialog = new SettingsDialog(application.getConfigurationModel(), application);

        // Setup the frame now that all components have been added.
        this.pack();
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        this.setVisible(true);
    }

    /**
     * Get the application menu.
     *
     * @return the application menu
     */
    MenuBar getApplicationMenu() {
        return this.menu;
    }

    /**
     * Get the panel showing conditions information.
     *
     * @return the conditions panel
     */
    ConditionsPanel getConditionsPanel() {
        return this.conditionsPanel;
    }

    /**
     * Get the panel for the dashboard.
     *
     * @return the dashboard panel
     */
    EventDashboard getEventDashboard() {
        return this.dashboardPanel;
    }

    /**
     * Get the panel that shows the log table and controls.
     *
     * @return the log panel
     */
    LogPanel getLogPanel() {
        return this.logPanel;
    }

    /**
     * Get the plot info panel that shows plot statistics.
     *
     * @return the plot info panel
     */
    PlotInfoPanel getPlotInfoPanel() {
        return this.plotInfoPanel;
    }

    /**
     * Get the plot panel for displaying AIDA plots.
     *
     * @return the plot panel
     */
    PlotPanel getPlotPanel() {
        return this.plotPanel;
    }

    /**
     * Get the settings dialog window.
     *
     * @return the settings dialog window
     */
    SettingsDialog getSettingsDialog() {
        return this.settingsDialog;
    }

    /**
     * Get the system status panel.
     *
     * @return the system status panel
     */
    SystemStatusPanel getSystemStatusPanel() {
        return this.systemStatusPanel;
    }

    /**
     * Get the toolbar panel with the buttons etc.
     *
     * @return the toolbar panel
     */
    ToolbarPanel getToolbarPanel() {
        return this.toolbarPanel;
    }

    /**
     * Get the trigger diagnostics panel.
     *
     * @return the trigger diagnostics panel
     */
    TriggerDiagnosticsPanel getTriggerPanel() {
        return this.triggerPanel;
    }

    /**
     * Restore default window settings.
     */
    void restoreDefaults() {
        this.setExtendedState(Frame.MAXIMIZED_BOTH);
        this.mainSplitPane.resetToPreferredSizes();
        this.leftSplitPane.resetToPreferredSizes();
        this.rightSplitPane.resetToPreferredSizes();
    }
}
