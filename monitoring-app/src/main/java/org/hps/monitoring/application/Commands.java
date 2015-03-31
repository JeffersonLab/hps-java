package org.hps.monitoring.application;

/**
 * These strings are used to identify ActionEvents in the MonitoringApplication. 
 * A few commands handled only by sub-components are not listed here.
 */
final class Commands {

    // Settings
    static final String LOAD_SETTINGS = "loadSettings";
    static final String LOAD_DEFAULT_SETTINGS = "loadDefaultSettings";
    static final String SAVE_SETTINGS = "saveSettings";
    static final String SHOW_SETTINGS = "showSettings";
    
    // File open and close
    static final String OPEN_FILE = "openFile";
    static final String CLOSE_FILE = "closeFile"; 
    static final String RECENT_FILE_SELECTED = "recentFileSelected";
    
    // Window
    static final String MAXIMIZE_WINDOW = "maximizeWindow";
    static final String MINIMIZE_WINDOW = "minimizeWindow";
    static final String DEFAULT_WINDOW = "defaultWindow";
    
    // Data source
    static final String DATA_SOURCE_CHANGED = "dataSourceChanged";

    // Event commands
    static final String CONNECT = "connect";
    static final String DISCONNECT = "disconnect";
    static final String NEXT = "next";
    static final String PAUSE = "pause";
    static final String RESUME = "resume";
    
    // Save a screenshot
    static final String SAVE_SCREENSHOT = "saveScreenshot";
    
    // Plotting actions
    static final String SAVE_PLOTS = "savePlots";
    static final String CLEAR_PLOTS = "resetPlots";
    static final String SAVE_SELECTED_PLOTS = "saveSelectedPlots";
    
    // Exit the application.
    static final String EXIT = "exit";
           
    // Log to file or standard print stream.
    static final String LOG_TO_FILE = "logToFile";
    static final String LOG_TO_TERMINAL = "logToTerminal";
    
    static final String LOG_LEVEL_FILTER_CHANGED = "logLevelFilterChanged";    
    
    static final String CONDITIONS_TAG_CHANGED = "conditionsTagChanged";
    
    static final String START_AIDA_SERVER = "startAIDAServer";
    static final String STOP_AIDA_SERVER = "stopAIDAServer";
    
    ////////////////////////////////////////////    
    static final String BLOCKING_CHANGED = "blockingChanged";
    static final String CHOOSE_COMPACT_FILE = "chooseCompactFile";
    static final String CHOOSE_LOG_FILE = "chooseLogFile";
    
    static final String CHOOSE_STEERING_FILE = "chooseSteeringFile";
    static final String CLEAR_LOG_TABLE = "clearLogTable";
    static final String DATA_SOURCE_TYPE_CHANGED = "dataSourceTypeChanged";
    static final String DETECTOR_NAME_CHANGED = "detectorNameChanged";
    static final String DETECTOR_ALIAS_CHANGED = "detectorAliasChanged";
    
    static final String DISCONNECT_ON_ERROR_CHANGED = "disconnectOnErrorChanged";
    static final String DISCONNECT_ON_END_RUN_CHANGED = "disconnectOnEndRunChanged";
    static final String EVENT_BUILDER_CHANGED = "eventBuilderChanged";
    static final String FREEZE_CONDITIONS_CHANGED = "freezeConditionsChanged";
    
    static final String LOG_LEVEL_CHANGED = "logLevelChanged";
    
    static final String PROCESSING_STAGE_CHANGED = "processingStageChanged";    
    static final String SAVE_LOG_TABLE = "saveLogTable";            
    static final String SELECT_LOG_FILE = "logToFile";
    static final String SET_STEERING_RESOURCE = "setSteeringResource";    
    static final String STEERING_TYPE_CHANGED = "steeringTypeChanged";
    static final String STEERING_RESOURCE_CHANGED = "steeringResourceChanged";
    static final String USER_RUN_NUMBER_CHANGED = "userRunNumberChanged";
    static final String VERBOSE_CHANGED = "verboseChanged";
    static final String VALIDATE_DATA_FILE = "validateDataFile";
    static final String WAIT_MODE_CHANGED = "waitModeChanged";
}