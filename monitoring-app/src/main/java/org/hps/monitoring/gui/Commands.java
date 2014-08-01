package org.hps.monitoring.gui;

/**
 * These strings are used to identify ActionEvents in the MonitoringApplication.
 * A few commands handled only by sub-components are not listed here.
 */
final class Commands {
           
    static final String DISCONNECT_ON_ERROR_CHANGED = "disconnectOnErrorChanged";
    static final String STEERING_TYPE_CHANGED = "steeringTypeChanged";
    static final String STEERING_RESOURCE_CHANGED = "steeringResourceChanged";
    static final String LOG_TO_FILE_CHANGED = "logToFileChanged";
    static final String AIDA_AUTO_SAVE_CHANGED = "aidaAutoSaveChanged";
    static final String LOG_LEVEL_CHANGED = "logLevelChanged";
    
    static final String BLOCKING_CHANGED = "blockingChanged";
    static final String VERBOSE_CHANGED = "verboseChanged";
    static final String WAIT_MODE_CHANGED = "waitModeChanged";
    
    static final String DATA_SOURCE_TYPE_CHANGED = "dataSourceTypeChanged";
       
    static final String AIDA_AUTO_SAVE = "aidaAutoSave";
    static final String CLEAR_LOG_TABLE = "clearLogTable";
    static final String CHOOSE_LOG_FILE = "chooseLogFile";
    static final String CHOOSE_STEERING_FILE = "chooseSteeringFile";
    static final String CONNECT = "connect";
    static final String DISCONNECT = "disconnect";
    static final String EXIT = "exit";
    static final String LOAD_DEFAULT_CONFIG_FILE = "loadDefaultConfigFile";    
    static final String LOG_TO_FILE = "logToFile";
    static final String LOG_TO_TERMINAL = "logToTerminal";    
    static final String NEXT = "next";
    static final String PAUSE = "pause";
    static final String RESUME = "resume";
    static final String SAVE_LOG_TABLE = "saveLogTable";
    static final String SAVE_PLOTS = "savePlots";
    static final String SCREENSHOT = "screenshot";
    static final String SAVE_CONFIG_FILE = "saveConfigFile";
    static final String SET_EVENT_BUILDER = "setEventBuilder";    

    static final String SET_STEERING_RESOURCE = "setSteeringResource";            
    static final String SELECT_CONFIG_FILE = "selectConfigFile";
    static final String SELECT_LOG_FILE = "logToFile";
    static final String SHOW_SETTINGS = "showSettings";
}