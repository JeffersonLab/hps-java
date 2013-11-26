package org.hps.monitoring;

/**
 * These strings are used to identify ActionEvents in the MonitoringApplication.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
final class MonitoringCommands {

    private MonitoringCommands() {}

    static final String connectCmd = "connect";
    static final String disconnectCmd = "disconnect";
    static final String saveConnectionCmd = "saveConnection";
    static final String loadConnectionCmd = "loadConnection";
    static final String resetConnectionSettingsCmd = "resetConnectionSettings";
    static final String resetEventsCmd = "resetEvents";
    static final String savePlotsCmd = "savePlots";
    static final String resetDriversCmd = "resetDrivers";
    static final String eventBuilderCmd = "eventBuilder";
    static final String refreshCmd = "eventRefresh";
    static final String exitCmd = "exit";
    static final String logToFileCmd = "logToFile";
    static final String logToTerminalCmd = "logToTerminal";
    static final String screenshotCmd = "screenshot";
    static final String eventRefreshCmd = "eventRefreshEdit";
    static final String updateTimeCmd = "updateTime";    
    static final String setMaxEventsCmd = "setMaxEvents";
    static final String saveLogTableCmd = "saveLogTable";
    static final String clearLogTableCmd = "clearLogTable";    
    static final String pauseCmd = "pause";
    static final String resumeCmd = "resume";
    static final String nextCmd = "next";
    static final String logLevelCmd = "logLevel";
    static final String aidaAutoSaveCmd = "aidaAutoSave";
    static final String saveJobSettingsCmd = "saveJobSettings";
    static final String loadJobSettingsCmd = "loadJobSettings";
    static final String resetJobSettingsCmd = "resetJobSettings";
    static final String steeringResourceCmd = "steeringResource";
    static final String steeringFileCmd = "steeringFile";
}