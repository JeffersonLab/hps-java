package org.hps.monitoring.gui.model;

import java.io.File;
import java.util.logging.Level;

import org.hps.monitoring.config.Configuration;
import org.hps.monitoring.enums.SteeringType;

/**
 * A model of the global configuration parameters that can be
 * used to automatically update the GUI or push changes in GUI
 * component values into the configuration.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class ConfigurationModel extends AbstractModel {
    
    Configuration config;
    
    public static final String AIDA_AUTO_SAVE_PROPERTY = "AidaAutoSave";
    public static final String AIDA_FILE_NAME_PROPERTY = "AidaFileName";   
    public static final String DETECTOR_NAME_PROPERTY = "DetectorName";
    public static final String DISCONNECT_ON_ERROR_PROPERTY = "DisconnectOnError";
    public static final String EVENT_BUILDER_PROPERTY = "EventBuilderClassName";
    public static final String LOG_FILE_NAME_PROPERTY = "LogFileName";
    public static final String LOG_LEVEL_PROPERTY = "LogLevel";
    public static final String LOG_TO_FILE_PROPERTY = "LogToFile";    
    public static final String STEERING_TYPE_PROPERTY = "SteeringType";
    public static final String STEERING_FILE_PROPERTY = "SteeringFile";
    public static final String STEERING_RESOURCE_PROPERTY = "SteeringResource";    
               
    static final String[] CONFIG_PROPERTIES = new String[] {
            AIDA_AUTO_SAVE_PROPERTY,
            AIDA_FILE_NAME_PROPERTY,
            DETECTOR_NAME_PROPERTY,
            DISCONNECT_ON_ERROR_PROPERTY,
            EVENT_BUILDER_PROPERTY,
            LOG_FILE_NAME_PROPERTY,
            LOG_LEVEL_PROPERTY,
            LOG_TO_FILE_PROPERTY,
            STEERING_FILE_PROPERTY,
            STEERING_RESOURCE_PROPERTY,
            STEERING_TYPE_PROPERTY,
    };        
    
    String detectorName;
            
    public ConfigurationModel(Configuration config) {
        this.config = config;
    }
    
    public void setConfiguration(Configuration config) {
        this.config = config;
    }
           
    public Level getLogLevel() {
        return Level.parse(config.get(LOG_LEVEL_PROPERTY));        
    }
    
    public void setLogLevel(Level level) {
        Level oldValue = getLogLevel();
        config.set(LOG_LEVEL_PROPERTY, level.getName());
        firePropertyChange(LOG_LEVEL_PROPERTY, oldValue, getLogLevel());
    }
    
    public SteeringType getSteeringType() {        
        return SteeringType.valueOf(config.get(STEERING_TYPE_PROPERTY));
    }
    
    public void setSteeringType(SteeringType steeringType) {
        SteeringType oldValue = getSteeringType();
        config.set(STEERING_TYPE_PROPERTY, steeringType.name());
        firePropertyChange(STEERING_TYPE_PROPERTY, oldValue, getSteeringType());
    }
    
    public File getSteeringFile() {
        if (config.hasKey(STEERING_FILE_PROPERTY))
            return new File(config.get(STEERING_FILE_PROPERTY));
        else
            return null;
    }
    
    public void setSteeringFile(String steeringFile) {
        File oldValue = getSteeringFile();
        config.set(STEERING_FILE_PROPERTY, steeringFile);
        firePropertyChange(STEERING_FILE_PROPERTY, oldValue, getSteeringFile().getPath());
    }
    
    public String getSteeringResource() {
        return config.get(STEERING_RESOURCE_PROPERTY);
    }        
    
    public void setSteeringResource(String steeringResource) {
        String oldValue = getSteeringResource();
        config.set(STEERING_RESOURCE_PROPERTY, steeringResource);
        firePropertyChange(STEERING_RESOURCE_PROPERTY, oldValue, steeringResource);
    }
    
    public String getDetectorName() {
        return config.get(DETECTOR_NAME_PROPERTY);
    }
    
    public void setDetectorName(String detectorName) {
        String oldValue = getDetectorName();
        config.set(DETECTOR_NAME_PROPERTY, detectorName);
        firePropertyChange(DETECTOR_NAME_PROPERTY, oldValue, getDetectorName());
    }
    
    public String getEventBuilderClassName() {
        return config.get(EVENT_BUILDER_PROPERTY);    
    }         
    
    public void setEventBuilderClassName(String eventBuilderClassName) {
        String oldValue = getEventBuilderClassName();
        config.set(EVENT_BUILDER_PROPERTY, eventBuilderClassName);
        firePropertyChange(EVENT_BUILDER_PROPERTY, oldValue, getEventBuilderClassName());
    }
    
    public boolean getLogToFile() {
        return config.getBoolean(LOG_TO_FILE_PROPERTY);
    }
    
    public void setLogToFile(boolean logToFile) {
        boolean oldValue = getLogToFile();
        config.set(LOG_TO_FILE_PROPERTY, logToFile);
        firePropertyChange(LOG_TO_FILE_PROPERTY, oldValue, getLogToFile());
    }
    
    public String getLogFileName() {
        return config.get(LOG_FILE_NAME_PROPERTY);
    }
    
    public void setLogFileName(String logFileName) {
        String oldValue = getLogFileName();
        config.set(LOG_FILE_NAME_PROPERTY, logFileName);
        firePropertyChange(LOG_FILE_NAME_PROPERTY, oldValue, getLogFileName());
    }
    
    public boolean getAidaAutoSave() {
        return config.equals(AIDA_AUTO_SAVE_PROPERTY);
    }
    
    public void setAutoSaveAida(boolean aidaAutoSave) {
        boolean oldValue = getAidaAutoSave();
        config.set(AIDA_AUTO_SAVE_PROPERTY, aidaAutoSave);
        firePropertyChange(AIDA_AUTO_SAVE_PROPERTY, oldValue, aidaAutoSave);
    }
    
    public String getAidaFileName() {
        return config.get(AIDA_FILE_NAME_PROPERTY);
    }
    
    public void setAidaFileName(String aidaFileName) {
        String oldValue = getAidaFileName();
        config.set(AIDA_FILE_NAME_PROPERTY, aidaFileName);
        firePropertyChange(AIDA_FILE_NAME_PROPERTY, oldValue, aidaFileName);
    }
    
    public boolean getDisconnectOnError() {
        return config.getBoolean(DISCONNECT_ON_ERROR_PROPERTY);
    }    
    
    public void setDisconnectOnError(boolean disconnectOnError) {
        boolean oldValue = getDisconnectOnError();
        config.set(DISCONNECT_ON_ERROR_PROPERTY, disconnectOnError);
        firePropertyChange(DISCONNECT_ON_ERROR_PROPERTY, oldValue, getDisconnectOnError());
    }        
    
    @Override
    public String[] getProperties() {
        return CONFIG_PROPERTIES;
    }
}