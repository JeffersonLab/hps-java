package org.hps.monitoring.application;

/**
 * These strings are used to identify ActionEvents in the MonitoringApplication. A few commands
 * handled only by sub-components are not listed here.
 */
public final class Commands {

    static final String SETTINGS_LOAD = "settingsLoad";
    static final String SETTINGS_LOAD_DEFAULT = "settingsLoadDefault";
    static final String SETTINGS_SAVE = "settingsSave";
    static final String SETTINGS_SHOW = "settingsShow";
    static final String OPEN_FILE = "openFile";
    

    ////////////////////////////////////////////
    
    static final String AIDA_AUTO_SAVE = "aidaAutoSave";
    static final String AIDA_AUTO_SAVE_CHANGED = "aidaAutoSaveChanged";
    static final String BLOCKING_CHANGED = "blockingChanged";
    static final String CHOOSE_COMPACT_FILE = "chooseCompactFile";
    static final String CHOOSE_LOG_FILE = "chooseLogFile";
    
    static final String CHOOSE_STEERING_FILE = "chooseSteeringFile";
    static final String CONNECT = "connect";
    static final String CLEAR_LOG_TABLE = "clearLogTable";
    static final String DATA_SOURCE_TYPE_CHANGED = "dataSourceTypeChanged";
    static final String DETECTOR_NAME_CHANGED = "detectorNameChanged";
    static final String DETECTOR_ALIAS_CHANGED = "detectorAliasChanged";
    static final String DISCONNECT = "disconnect";
    static final String DISCONNECT_ON_ERROR_CHANGED = "disconnectOnErrorChanged";
    static final String DISCONNECT_ON_END_RUN_CHANGED = "disconnectOnEndRunChanged";
    static final String EVENT_BUILDER_CHANGED = "eventBuilderChanged";
    static final String EXIT = "exit";
    static final String FREEZE_CONDITIONS_CHANGED = "freezeConditionsChanged";
    
    static final String LOG_LEVEL_CHANGED = "logLevelChanged";
    static final String LOG_TO_FILE = "logToFile";
    static final String LOG_TO_FILE_CHANGED = "logToFileChanged";
    static final String LOG_TO_TERMINAL = "logToTerminal";
    static final String NEXT = "next";
    static final String PAUSE = "pause";
    static final String PROCESSING_STAGE_CHANGED = "processingStageChanged";
    static final String PLOTS_CLEAR = "resetPlots";
    static final String RESTORE_DEFAULT_GUI_LAYOUT = "restoreDefaultGuiLayout";
    static final String RESUME = "resume";
    
    static final String SAVE_LAYOUT = "saveLayout";
    static final String SAVE_LOG_TABLE = "saveLogTable";
    static final String PLOTS_SAVE = "savePlots";
    static final String SCREENSHOT = "screenshot";
    
    static final String SELECT_LOG_FILE = "logToFile";
    static final String SET_STEERING_RESOURCE = "setSteeringResource";
    
    static final String STEERING_TYPE_CHANGED = "steeringTypeChanged";
    static final String STEERING_RESOURCE_CHANGED = "steeringResourceChanged";
    static final String USER_RUN_NUMBER_CHANGED = "userRunNumberChanged";
    static final String VERBOSE_CHANGED = "verboseChanged";
    static final String VALIDATE_DATA_FILE = "validateDataFile";
    static final String WAIT_MODE_CHANGED = "waitModeChanged";
}