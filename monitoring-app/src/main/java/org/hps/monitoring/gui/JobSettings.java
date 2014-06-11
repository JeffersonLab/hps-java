package org.hps.monitoring.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;

/**
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: JobSettings.java,v 1.6 2013/10/30 17:05:17 jeremy Exp $
 */
final class JobSettings {
    
    // Default job settings.
    boolean pauseMode = false;
    boolean disconnectOnError = false;
    boolean warnBeforeDisconnect = true;
    Level logLevel = Level.ALL;
    int steeringType = 0; // resource = 0; file = 1
    String steeringResource = "org/hps/steering/monitoring/TestRunMonitoring.lcsim";
    String steeringFile = "";
    String detectorName = "HPS-TestRun-v5";
    String eventBuilderClassName = "org.hps.evio.LCSimTestRunEventBuilder";
    boolean logToFile = false;
    String logFileName = "";
    boolean autoSaveAida = false;
    String autoSaveAidaFileName = "";
    boolean enableRemoteAida = false;
    String remoteAidaName = "hps";
    
    JobSettings() {
    }
    
    JobSettings(File f) throws IOException {
        load(f);
    }
        
    void save(File file) throws IOException {
        Properties prop = new Properties();
        prop.setProperty("pauseMode", Boolean.toString(pauseMode));
        prop.setProperty("disconnectOnError", Boolean.toString(disconnectOnError));
        prop.setProperty("warnBeforeDisconnect", Boolean.toString(warnBeforeDisconnect));
        prop.setProperty("logLevel", logLevel.toString());
        prop.setProperty("steeringType", Integer.toString(steeringType));
        prop.setProperty("steeringFile", steeringFile);
        prop.setProperty("steeringResource", steeringResource);
        prop.setProperty("detectorName", detectorName);
        prop.setProperty("eventBuilderClassName", eventBuilderClassName);
        prop.setProperty("logToFile", Boolean.toString(logToFile));
        prop.setProperty("logFileName", logFileName);
        prop.setProperty("autoSaveAida", Boolean.toString(autoSaveAida));
        prop.setProperty("autoSaveAidaFileName", autoSaveAidaFileName);
        prop.setProperty("enableRemoteAida", Boolean.toString(enableRemoteAida));
        prop.setProperty("remoteAidaName", remoteAidaName);
        prop.store(new FileOutputStream(file), null);
    }
    
    void load(File file) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(file));
        pauseMode = Boolean.parseBoolean(prop.getProperty("pauseMode"));
        disconnectOnError = Boolean.parseBoolean(prop.getProperty("disconnectOnError"));
        warnBeforeDisconnect = Boolean.parseBoolean(prop.getProperty("warnBeforeDisconnect"));
        logLevel = Level.parse(prop.getProperty("logLevel"));
        steeringType = Integer.parseInt(prop.getProperty("steeringType"));
        steeringFile = prop.getProperty("steeringFile");
        steeringResource = prop.getProperty("steeringResource");
        detectorName = prop.getProperty("detectorName");
        eventBuilderClassName = prop.getProperty("eventBuilderClassName");
        logToFile = Boolean.parseBoolean(prop.getProperty("logToFile"));
        logFileName = prop.getProperty("logFileName");
        autoSaveAida = Boolean.parseBoolean(prop.getProperty("autoSaveAida"));
        autoSaveAidaFileName = prop.getProperty("autoSaveAidaFileName");
        enableRemoteAida = Boolean.parseBoolean(prop.getProperty("enableRemoteAida"));
        remoteAidaName = prop.getProperty("remoteAidaName");
    }
} 