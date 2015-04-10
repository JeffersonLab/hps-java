package org.hps.monitoring.application;

/**
 * These strings are used to identify ActionEvents in the MonitoringApplication. A few commands handled only by
 * sub-components are not listed here.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class Commands {

    /**
     * ET blocking setting changed.
     */
    static final String BLOCKING_CHANGED = "blockingChanged";

    /**
     * Choose a compact file for the detector description.
     */
    static final String CHOOSE_COMPACT_FILE = "chooseCompactFile";

    /**
     * Choose an LCSim job steering file.
     */
    static final String CHOOSE_STEERING_FILE = "chooseSteeringFile";

    /**
     * Clear the log table of all messages.
     */
    static final String CLEAR_LOG_TABLE = "clearLogTable";

    /**
     * Reset the underlying AIDA objects in the plot tree.
     */
    static final String CLEAR_PLOTS = "resetPlots";

    /**
     * Close the current input data file.
     */
    static final String CLOSE_FILE = "closeFile";

    /**
     * Change the current conditions tag.
     */
    static final String CONDITIONS_TAG_CHANGED = "conditionsTagChanged";

    /**
     * Connect to a new session.
     */
    static final String CONNECT = "connect";

    /**
     * Change the current data source.
     */
    static final String DATA_SOURCE_CHANGED = "dataSourceChanged";

    /**
     * Reset the application window to its default settings including scroll pane positions.
     */
    static final String DEFAULT_WINDOW = "defaultWindow";

    /**
     * Detector alias changed.
     */
    static final String DETECTOR_ALIAS_CHANGED = "detectorAliasChanged";

    /**
     * Name of detector changed.
     */
    static final String DETECTOR_NAME_CHANGED = "detectorNameChanged";

    /**
     * Disconnect the current session.
     */
    static final String DISCONNECT = "disconnect";

    /**
     * Disconnect on end run changed.
     */
    static final String DISCONNECT_ON_END_RUN_CHANGED = "disconnectOnEndRunChanged";

    /**
     * Disconnect on error changed.
     */
    static final String DISCONNECT_ON_ERROR_CHANGED = "disconnectOnErrorChanged";

    /**
     * Event builder setting changed.
     */
    static final String EVENT_BUILDER_CHANGED = "eventBuilderChanged";

    /**
     * Exit the application.
     */
    static final String EXIT = "exit";

    /**
     * Freeze conditions system after initialization.
     */
    static final String FREEZE_CONDITIONS_CHANGED = "freezeConditionsChanged";

    /**
     * Load the default properties file from a jar resource.
     */
    static final String LOAD_DEFAULT_SETTINGS = "loadDefaultSettings";

    /**
     * Load a settings properties file.
     */
    static final String LOAD_SETTINGS = "loadSettings";

    /**
     * Global log level changed.
     */
    static final String LOG_LEVEL_CHANGED = "logLevelChanged";

    /**
     * Change the log level filter for showing messages in the log table.
     */
    static final String LOG_LEVEL_FILTER_CHANGED = "logLevelFilterChanged";

    /**
     * Send log messages to an output file.
     */
    static final String LOG_TO_FILE = "logToFile";

    /**
     * Send log messages to the terminal.
     */
    static final String LOG_TO_TERMINAL = "logToTerminal";

    /**
     * Maximize the application window.
     */
    static final String MAXIMIZE_WINDOW = "maximizeWindow";

    /**
     * Minimize the application window.
     */
    static final String MINIMIZE_WINDOW = "minimizeWindow";

    /**
     * Get the next event if paused.
     */
    static final String NEXT = "next";

    /**
     * Open an input data file.
     */
    static final String OPEN_FILE = "openFile";

    /**
     * Pause the event processing.
     */
    static final String PAUSE = "pause";

    /**
     * Processing stage changed.
     */
    static final String PROCESSING_STAGE_CHANGED = "processingStageChanged";

    /**
     * Select one of the items from the recent files list to be the current data source.
     */
    static final String RECENT_FILE_SELECTED = "recentFileSelected";

    /**
     * Resume event processing if paused.
     */
    static final String RESUME = "resume";

    /**
     * Save the log table to a text file.
     */
    static final String SAVE_LOG_TABLE = "saveLogTable";

    /**
     * Save the plots to a ROOT, AIDA or PDF file.
     */
    static final String SAVE_PLOTS = "savePlots";

    /**
     * Save a screenshot from the window graphics.
     */
    static final String SAVE_SCREENSHOT = "saveScreenshot";

    /**
     * Save the currently selected plots tab graphic to a PDF file.
     */
    static final String SAVE_SELECTED_PLOTS = "saveSelectedPlots";

    /**
     * Save settings to a properties file.
     */
    static final String SAVE_SETTINGS = "saveSettings";

    /**
     * Set the steering resource.
     */
    static final String SET_STEERING_RESOURCE = "setSteeringResource";

    /**
     * Show the settings dialog.
     */
    static final String SHOW_SETTINGS = "showSettings";

    /**
     * Start the AIDA server.
     */
    static final String START_AIDA_SERVER = "startAIDAServer";

    /**
     * Steering resource changed.
     */
    static final String STEERING_RESOURCE_CHANGED = "steeringResourceChanged";

    /**
     * Steering type changed (file or resource).
     */
    static final String STEERING_TYPE_CHANGED = "steeringTypeChanged";

    /**
     * Stop the AIDA server.
     */
    static final String STOP_AIDA_SERVER = "stopAIDAServer";

    /**
     * User run number in conditions system changed.
     */
    static final String USER_RUN_NUMBER_CHANGED = "userRunNumberChanged";

    /**
     * Verbose setting changed.
     */
    static final String VERBOSE_CHANGED = "verboseChanged";

    /**
     * ET wait mode changed.
     */
    static final String WAIT_MODE_CHANGED = "waitModeChanged";

    /**
     * Do not allow class instantiation.
     */
    private Commands() {
        throw new UnsupportedOperationException("Do no instantiate this class.");
    }
}
