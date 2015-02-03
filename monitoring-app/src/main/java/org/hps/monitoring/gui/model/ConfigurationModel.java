package org.hps.monitoring.gui.model;

import java.io.File;
import java.lang.reflect.Field;
import java.util.logging.Level;

import javassist.Modifier;

import org.hps.monitoring.enums.SteeringType;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.jlab.coda.et.enums.Mode;

/**
 * A model of the global configuration parameters that can be used to automatically update the GUI
 * from a configuration or push changes from GUI components into the current configuration.
 */
// FIXME: When the set methods are called, e.g. from GUI updates, this triggers
//        a property change event that pushes the values back to the GUI again.
// FIXME: Should check if property exists in set methods before retrieving old value for all set methods.
public final class ConfigurationModel extends AbstractModel {

    Configuration config;

    // Job setting properties.
    public static final String AIDA_AUTO_SAVE_PROPERTY = "AidaAutoSave";
    public static final String AIDA_FILE_NAME_PROPERTY = "AidaFileName";
    public static final String DETECTOR_NAME_PROPERTY = "DetectorName";
    public static final String DETECTOR_ALIAS_PROPERTY = "DetectorAlias";
    public static final String DISCONNECT_ON_ERROR_PROPERTY = "DisconnectOnError";
    public static final String DISCONNECT_ON_END_RUN_PROPERTY = "DisconnectOnEndRun";
    public static final String EVENT_BUILDER_PROPERTY = "EventBuilderClassName";
    public static final String FREEZE_CONDITIONS_PROPERTY = "FreezeConditions";
    public static final String LOG_FILE_NAME_PROPERTY = "LogFileName";
    public static final String LOG_LEVEL_PROPERTY = "LogLevel";
    public static final String LOG_TO_FILE_PROPERTY = "LogToFile";
    public static final String MAX_EVENTS_PROPERTY = "MaxEvents";
    public static final String MONITORING_APPLICATION_LAYOUT_PROPERTY = "MonitoringApplicationLayout";
    public static final String PLOT_FRAME_LAYOUT_PROPERTY = "PlotFrameLayout";
    public static final String SAVE_LAYOUT_PROPERTY = "SaveLayout";
    public static final String SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY = "SystemStatusFrameLayout";
    public static final String STEERING_TYPE_PROPERTY = "SteeringType";
    public static final String STEERING_FILE_PROPERTY = "SteeringFile";
    public static final String STEERING_RESOURCE_PROPERTY = "SteeringResource";
    public static final String USER_RUN_NUMBER_PROPERTY = "UserRunNumber";

    // Data source properties.
    public static final String DATA_SOURCE_TYPE_PROPERTY = "DataSourceType";
    public static final String DATA_SOURCE_PATH_PROPERTY = "DataSourcePath";
    public static final String PROCESSING_STAGE_PROPERTY = "ProcessingStage";

    // ET connection parameters.
    public static final String ET_NAME_PROPERTY = "EtName";
    public static final String HOST_PROPERTY = "Host";
    public static final String PORT_PROPERTY = "Port";
    public static final String BLOCKING_PROPERTY = "Blocking";
    public static final String VERBOSE_PROPERTY = "Verbose";
    public static final String STATION_NAME_PROPERTY = "StationName";
    public static final String CHUNK_SIZE_PROPERTY = "ChunkSize";
    public static final String QUEUE_SIZE_PROPERTY = "QueueSize";
    public static final String STATION_POSITION_PROPERTY = "StationPosition";
    public static final String WAIT_MODE_PROPERTY = "WaitMode";
    public static final String WAIT_TIME_PROPERTY = "WaitTime";
    public static final String PRESCALE_PROPERTY = "Prescale";
   
    static final String[] CONFIG_PROPERTIES = AbstractModel.getPropertyNames(ConfigurationModel.class);
        
    public ConfigurationModel() {
        this.config = new Configuration();
    }

    public ConfigurationModel(Configuration config) {
        this.config = config;
    }

    public void setConfiguration(Configuration config) {
        this.config = config;
        fireAllChanged();
    }

    public Configuration getConfiguration() {
        return this.config;
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
    
    public String getDetectorAlias() {
        return config.get(DETECTOR_ALIAS_PROPERTY);
    }
    
    public void setDetectorAlias(String detectorAlias) {
        String oldValue = null;
        if (hasPropertyKey(DETECTOR_ALIAS_PROPERTY)) {
            oldValue = getDetectorAlias();
        }
        config.set(DETECTOR_ALIAS_PROPERTY, detectorAlias);
        firePropertyChange(DETECTOR_ALIAS_PROPERTY, oldValue, getDetectorAlias());
    }
    

    public String getEventBuilderClassName() {
        return config.get(EVENT_BUILDER_PROPERTY);
    }

    public void setEventBuilderClassName(String eventBuilderClassName) {
        String oldValue = getEventBuilderClassName();
        config.set(EVENT_BUILDER_PROPERTY, eventBuilderClassName);
        firePropertyChange(EVENT_BUILDER_PROPERTY, oldValue, getEventBuilderClassName());
    }

    public Boolean getLogToFile() {
        return config.getBoolean(LOG_TO_FILE_PROPERTY);
    }

    public void setLogToFile(boolean logToFile) {
        Boolean oldValue = getLogToFile();
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

    public Boolean getAidaAutoSave() {
        return config.equals(AIDA_AUTO_SAVE_PROPERTY);
    }

    public void setAidaAutoSave(boolean aidaAutoSave) {
        Boolean oldValue = getAidaAutoSave();
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

    public Boolean getDisconnectOnError() {
        return config.getBoolean(DISCONNECT_ON_ERROR_PROPERTY);
    }

    public void setDisconnectOnError(boolean disconnectOnError) {
        Boolean oldValue = getDisconnectOnError();
        config.set(DISCONNECT_ON_ERROR_PROPERTY, disconnectOnError);
        firePropertyChange(DISCONNECT_ON_ERROR_PROPERTY, oldValue, getDisconnectOnError());
    }

    public Boolean getDisconnectOnEndRun() {
        return config.getBoolean(DISCONNECT_ON_END_RUN_PROPERTY);
    }

    public void setDisconnectOnEndRun(boolean disconnectOnEndRun) {
        Boolean oldValue = getDisconnectOnEndRun();
        config.set(DISCONNECT_ON_END_RUN_PROPERTY, disconnectOnEndRun);
        firePropertyChange(DISCONNECT_ON_END_RUN_PROPERTY, oldValue, getDisconnectOnEndRun());
    }

    public DataSourceType getDataSourceType() {
        return DataSourceType.valueOf(config.get(DATA_SOURCE_TYPE_PROPERTY));
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        DataSourceType oldValue = getDataSourceType();
        config.set(DATA_SOURCE_TYPE_PROPERTY, dataSourceType);
        firePropertyChange(DATA_SOURCE_TYPE_PROPERTY, oldValue, getDataSourceType());
    }

    public String getDataSourcePath() {
        return config.get(DATA_SOURCE_PATH_PROPERTY);
    }

    public void setDataSourcePath(String dataSourcePath) {
        String oldValue = getDataSourcePath();
        config.set(DATA_SOURCE_PATH_PROPERTY, dataSourcePath);
        firePropertyChange(DATA_SOURCE_PATH_PROPERTY, oldValue, getDataSourcePath());
    }

    public ProcessingStage getProcessingStage() {
        if (config.get(PROCESSING_STAGE_PROPERTY) == null)
            throw new RuntimeException(PROCESSING_STAGE_PROPERTY + " is null!!!");
        return ProcessingStage.valueOf(config.get(PROCESSING_STAGE_PROPERTY));
    }

    public void setProcessingStage(ProcessingStage processingStage) {
        ProcessingStage oldValue = getProcessingStage();
        config.set(PROCESSING_STAGE_PROPERTY, processingStage);
        firePropertyChange(PROCESSING_STAGE_PROPERTY, oldValue, getProcessingStage());
    }

    public String getEtName() {
        return config.get(ET_NAME_PROPERTY);
    }

    public void setEtName(String etName) {
        String oldValue = getEtName();
        config.set(ET_NAME_PROPERTY, etName);
        firePropertyChange(ET_NAME_PROPERTY, oldValue, getEtName());
    }

    public String getHost() {
        return config.get(HOST_PROPERTY);
    }

    public void setHost(String host) {
        String oldValue = getHost();
        config.set(HOST_PROPERTY, host);
        firePropertyChange(HOST_PROPERTY, oldValue, getHost());
    }

    public Integer getPort() {
        return config.getInteger(PORT_PROPERTY);
    }

    public void setPort(int port) {
        Integer oldValue = getPort();
        config.set(PORT_PROPERTY, port);
        firePropertyChange(PORT_PROPERTY, oldValue, getPort());
    }

    public Boolean getBlocking() {
        return config.getBoolean(BLOCKING_PROPERTY);
    }

    public void setBlocking(boolean blocking) {
        Boolean oldValue = getBlocking();
        config.set(BLOCKING_PROPERTY, blocking);
        firePropertyChange(BLOCKING_PROPERTY, oldValue, getBlocking());
    }

    public Boolean getVerbose() {
        return config.getBoolean(VERBOSE_PROPERTY);
    }

    public void setVerbose(boolean verbose) {
        Boolean oldValue = getVerbose();
        config.set(VERBOSE_PROPERTY, verbose);
        firePropertyChange(VERBOSE_PROPERTY, oldValue, getVerbose());
    }

    public String getStationName() {
        return config.get(STATION_NAME_PROPERTY);
    }

    public void setStationName(String stationName) {
        String oldValue = getStationName();
        config.set(STATION_NAME_PROPERTY, stationName);
        firePropertyChange(STATION_NAME_PROPERTY, oldValue, getStationName());
    }

    public Integer getChunkSize() {
        return config.getInteger(CHUNK_SIZE_PROPERTY);
    }

    public void setChunkSize(int chunkSize) {
        Integer oldValue = getChunkSize();
        config.set(CHUNK_SIZE_PROPERTY, chunkSize);
        firePropertyChange(CHUNK_SIZE_PROPERTY, oldValue, getChunkSize());
    }

    public Integer getQueueSize() {
        return config.getInteger(QUEUE_SIZE_PROPERTY);
    }

    public void setQueueSize(int queueSize) {
        Integer oldValue = getQueueSize();
        config.set(QUEUE_SIZE_PROPERTY, queueSize);
        firePropertyChange(QUEUE_SIZE_PROPERTY, oldValue, getQueueSize());
    }

    public Integer getStationPosition() {
        return config.getInteger(STATION_POSITION_PROPERTY);
    }

    public void setStationPosition(int stationPosition) {
        Integer oldValue = getStationPosition();
        config.set(STATION_POSITION_PROPERTY, stationPosition);
        firePropertyChange(STATION_POSITION_PROPERTY, oldValue, getStationPosition());
    }

    public Mode getWaitMode() {
        return Mode.valueOf(config.get(WAIT_MODE_PROPERTY));
    }

    public void setWaitMode(Mode waitMode) {
        Mode oldValue = getWaitMode();
        config.set(WAIT_MODE_PROPERTY, waitMode.name());
        firePropertyChange(WAIT_MODE_PROPERTY, oldValue, getWaitMode());
    }

    public Integer getWaitTime() {
        return config.getInteger(WAIT_TIME_PROPERTY);
    }

    public void setWaitTime(int waitTime) {
        Integer oldValue = getWaitTime();
        config.set(WAIT_TIME_PROPERTY, waitTime);
        firePropertyChange(WAIT_TIME_PROPERTY, oldValue, getWaitTime());
    }

    public Integer getPrescale() {
        return config.getInteger(PRESCALE_PROPERTY);
    }

    public void setPrescale(int prescale) {
        Integer oldValue = getPrescale();
        config.set(PRESCALE_PROPERTY, prescale);
        firePropertyChange(PRESCALE_PROPERTY, oldValue, getPrescale());
    }
    
    public void setUserRunNumber(Integer userRunNumber) {
        Integer oldValue = null;
        if (hasPropertyKey(USER_RUN_NUMBER_PROPERTY)) {
            oldValue = getUserRunNumber();
        }
        config.set(USER_RUN_NUMBER_PROPERTY, userRunNumber);
        firePropertyChange(USER_RUN_NUMBER_PROPERTY, oldValue, getUserRunNumber());
    }
    
    public Integer getUserRunNumber() {
        return config.getInteger(USER_RUN_NUMBER_PROPERTY);
    }
    
    public void setFreezeConditions(boolean freezeConditions) {
        Boolean oldValue = null;
        if (hasPropertyKey(FREEZE_CONDITIONS_PROPERTY)) {
            oldValue = getFreezeConditions();
        }
        config.set(FREEZE_CONDITIONS_PROPERTY, freezeConditions);
        firePropertyChange(FREEZE_CONDITIONS_PROPERTY, oldValue, freezeConditions);
    }
    
    public Boolean getFreezeConditions() {
        return config.getBoolean(FREEZE_CONDITIONS_PROPERTY);
    }

    public Boolean getSaveLayout() {
        return config.getBoolean(SAVE_LAYOUT_PROPERTY);
    }

    public void setSaveLayout(boolean saveLayout) {
        Boolean oldValue = getSaveLayout();
        config.set(SAVE_LAYOUT_PROPERTY, saveLayout);
        firePropertyChange(SAVE_LAYOUT_PROPERTY, oldValue, getSaveLayout());
    }

    public String getMonitoringApplicationLayout() {
        return config.get(MONITORING_APPLICATION_LAYOUT_PROPERTY);
    }

    public void setMonitoringApplicationLayout(String layout) {
        String oldValue = getMonitoringApplicationLayout();
        config.set(MONITORING_APPLICATION_LAYOUT_PROPERTY, layout);
        firePropertyChange(MONITORING_APPLICATION_LAYOUT_PROPERTY, oldValue, getMonitoringApplicationLayout());
    }

    public String getSystemStatusFrameLayout() {
        return config.get(SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY);
    }

    public void setSystemStatusFrameLayout(String layout) {
        String oldValue = getSystemStatusFrameLayout();
        config.set(SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY, layout);
        firePropertyChange(SYSTEM_STATUS_FRAME_LAYOUT_PROPERTY, oldValue, getSystemStatusFrameLayout());
    }

    public String getPlotFrameLayout() {
        return config.get(PLOT_FRAME_LAYOUT_PROPERTY);
    }

    public void setPlotFrameLayout(String layout) {
        String oldValue = getPlotFrameLayout();
        config.set(PLOT_FRAME_LAYOUT_PROPERTY, layout);
        firePropertyChange(PLOT_FRAME_LAYOUT_PROPERTY, oldValue, getPlotFrameLayout());
    }
    
    public void setMaxEvents(long maxEvents) {
        //System.out.println("ConfigurationModel.setMaxEvents - " + maxEvents);
        Long oldValue = getMaxEvents();
        config.set(MAX_EVENTS_PROPERTY, maxEvents);
        firePropertyChange(MAX_EVENTS_PROPERTY, oldValue, getMaxEvents());
    }
    
    public Long getMaxEvents() {
        return config.getLong(MAX_EVENTS_PROPERTY);
    }

    public void remove(String property) {
        if (hasPropertyKey(property)) {
            Object oldValue = config.get(property);
            if (oldValue != null) {
                config.remove(property);
                firePropertyChange(property, oldValue, null);
            }
        }
    }
    
    public boolean hasPropertyKey(String key) {
        return config.hasKey(key);
    }
    
    public boolean hasValidProperty(String key) {
        return config.hasKey(key) && config.get(key) != null;
    }
        
    @Override
    public String[] getPropertyNames() {
        return CONFIG_PROPERTIES;
    }
}