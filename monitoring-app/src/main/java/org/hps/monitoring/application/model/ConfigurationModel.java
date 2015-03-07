package org.hps.monitoring.application.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.jlab.coda.et.enums.Mode;

/**
 * A model of the global configuration parameters that can be used to automatically update the GUI
 * from a configuration or push changes from GUI components into the current configuration.
 */
public final class ConfigurationModel extends AbstractModel {

    Configuration configuration;    
    
    List<ConfigurationListener> listeners = new ArrayList<ConfigurationListener>();
    
    // Job setting properties.
    public static final String DETECTOR_NAME_PROPERTY = "DetectorName";
    public static final String DETECTOR_ALIAS_PROPERTY = "DetectorAlias";
    public static final String DISCONNECT_ON_ERROR_PROPERTY = "DisconnectOnError";
    public static final String DISCONNECT_ON_END_RUN_PROPERTY = "DisconnectOnEndRun";
    public static final String EVENT_BUILDER_PROPERTY = "EventBuilderClassName";
    public static final String FREEZE_CONDITIONS_PROPERTY = "FreezeConditions";
    public static final String LOG_FILE_NAME_PROPERTY = "LogFileName";
    public static final String LOG_LEVEL_PROPERTY = "LogLevel";
    public static final String LOG_LEVEL_FILTER_PROPERTY = "LogLevelFilter";
    public static final String LOG_TO_FILE_PROPERTY = "LogToFile";
    public static final String MAX_EVENTS_PROPERTY = "MaxEvents";
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
    
    // Action command to get notified after configuration change is performed.
    public static final String CONFIGURATION_CHANGED = "configurationChanged";

    static final String[] CONFIG_PROPERTIES = AbstractModel.getPropertyNames(ConfigurationModel.class);

    public ConfigurationModel() {
        this.configuration = new Configuration();
    }

    public ConfigurationModel(Configuration configuration) {
        this.configuration = configuration;
        fireModelChanged();
    }
    
    public void addConfigurationListener(ConfigurationListener listener) {
        listeners.add(listener);
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
        fireModelChanged();
        fireConfigurationChanged();
    }
    
    void fireConfigurationChanged() {        
        for (ConfigurationListener listener : listeners) {
            listener.configurationChanged(this);
        }
    }
    
    public Configuration getConfiguration() {
        return this.configuration;
    }

    public Level getLogLevel() {
        return Level.parse(configuration.get(LOG_LEVEL_PROPERTY));
    }

    public void setLogLevel(Level level) {
        Level oldValue = getLogLevel();
        configuration.set(LOG_LEVEL_PROPERTY, level.getName());
        firePropertyChange(LOG_LEVEL_PROPERTY, oldValue, getLogLevel());
    }
    
    public Level getLogLevelFilter() {
        return Level.parse(configuration.get(LOG_LEVEL_FILTER_PROPERTY));
    }

    public void setLogLevelFilter(Level level) {
        Level oldValue = getLogLevelFilter();
        configuration.set(LOG_LEVEL_FILTER_PROPERTY, level.getName());
        firePropertyChange(LOG_LEVEL_FILTER_PROPERTY, oldValue, getLogLevelFilter());
    }

    public SteeringType getSteeringType() {
        return SteeringType.valueOf(configuration.get(STEERING_TYPE_PROPERTY));
    }

    public void setSteeringType(SteeringType steeringType) {
        SteeringType oldValue = getSteeringType();
        configuration.set(STEERING_TYPE_PROPERTY, steeringType.name());
        firePropertyChange(STEERING_TYPE_PROPERTY, oldValue, getSteeringType());
    }

    public File getSteeringFile() {
        if (configuration.hasKey(STEERING_FILE_PROPERTY)) {
            return new File(configuration.get(STEERING_FILE_PROPERTY));
        } else {
            return null;
        }
    }

    public void setSteeringFile(String steeringFile) {
        File oldValue = getSteeringFile();
        configuration.set(STEERING_FILE_PROPERTY, steeringFile);
        firePropertyChange(STEERING_FILE_PROPERTY, oldValue, getSteeringFile().getPath());
    }

    public String getSteeringResource() {
        return configuration.get(STEERING_RESOURCE_PROPERTY);
    }

    public void setSteeringResource(String steeringResource) {
        String oldValue = getSteeringResource();
        configuration.set(STEERING_RESOURCE_PROPERTY, steeringResource);
        firePropertyChange(STEERING_RESOURCE_PROPERTY, oldValue, steeringResource);
    }

    public String getDetectorName() {
        return configuration.get(DETECTOR_NAME_PROPERTY);
    }

    public void setDetectorName(String detectorName) {
        String oldValue = getDetectorName();
        configuration.set(DETECTOR_NAME_PROPERTY, detectorName);
        firePropertyChange(DETECTOR_NAME_PROPERTY, oldValue, getDetectorName());
    }

    public String getDetectorAlias() {
        return configuration.get(DETECTOR_ALIAS_PROPERTY);
    }

    public void setDetectorAlias(String detectorAlias) {
        String oldValue = null;
        if (hasPropertyKey(DETECTOR_ALIAS_PROPERTY)) {
            oldValue = getDetectorAlias();
        }
        configuration.set(DETECTOR_ALIAS_PROPERTY, detectorAlias);
        firePropertyChange(DETECTOR_ALIAS_PROPERTY, oldValue, getDetectorAlias());
    }

    public String getEventBuilderClassName() {
        return configuration.get(EVENT_BUILDER_PROPERTY);
    }

    public void setEventBuilderClassName(String eventBuilderClassName) {
        String oldValue = getEventBuilderClassName();
        configuration.set(EVENT_BUILDER_PROPERTY, eventBuilderClassName);
        firePropertyChange(EVENT_BUILDER_PROPERTY, oldValue, getEventBuilderClassName());
    }

    public Boolean getLogToFile() {
        return configuration.getBoolean(LOG_TO_FILE_PROPERTY);
    }

    public void setLogToFile(Boolean logToFile) {
        Boolean oldValue = getLogToFile();
        configuration.set(LOG_TO_FILE_PROPERTY, logToFile);
        firePropertyChange(LOG_TO_FILE_PROPERTY, oldValue, getLogToFile());
    }

    public String getLogFileName() {
        return configuration.get(LOG_FILE_NAME_PROPERTY);
    }

    public void setLogFileName(String logFileName) {
        String oldValue = getLogFileName();
        configuration.set(LOG_FILE_NAME_PROPERTY, logFileName);
        firePropertyChange(LOG_FILE_NAME_PROPERTY, oldValue, getLogFileName());
    }

    public Boolean getDisconnectOnError() {
        return configuration.getBoolean(DISCONNECT_ON_ERROR_PROPERTY);
    }

    public void setDisconnectOnError(Boolean disconnectOnError) {
        Boolean oldValue = getDisconnectOnError();
        configuration.set(DISCONNECT_ON_ERROR_PROPERTY, disconnectOnError);
        firePropertyChange(DISCONNECT_ON_ERROR_PROPERTY, oldValue, getDisconnectOnError());
    }

    public Boolean getDisconnectOnEndRun() {
        return configuration.getBoolean(DISCONNECT_ON_END_RUN_PROPERTY);
    }

    public void setDisconnectOnEndRun(Boolean disconnectOnEndRun) {
        Boolean oldValue = getDisconnectOnEndRun();
        configuration.set(DISCONNECT_ON_END_RUN_PROPERTY, disconnectOnEndRun);
        firePropertyChange(DISCONNECT_ON_END_RUN_PROPERTY, oldValue, getDisconnectOnEndRun());
    }

    public DataSourceType getDataSourceType() {
        return DataSourceType.valueOf(configuration.get(DATA_SOURCE_TYPE_PROPERTY));
    }

    public void setDataSourceType(DataSourceType dataSourceType) {
        DataSourceType oldValue = getDataSourceType();
        configuration.set(DATA_SOURCE_TYPE_PROPERTY, dataSourceType);
        firePropertyChange(DATA_SOURCE_TYPE_PROPERTY, oldValue, getDataSourceType());
    }

    public String getDataSourcePath() {
        return configuration.get(DATA_SOURCE_PATH_PROPERTY);
    }

    public void setDataSourcePath(String dataSourcePath) {
        String oldValue = getDataSourcePath();
        configuration.set(DATA_SOURCE_PATH_PROPERTY, dataSourcePath);
        firePropertyChange(DATA_SOURCE_PATH_PROPERTY, oldValue, getDataSourcePath());
    }

    public ProcessingStage getProcessingStage() {
        if (configuration.get(PROCESSING_STAGE_PROPERTY) == null)
            throw new RuntimeException(PROCESSING_STAGE_PROPERTY + " is null!!!");
        return ProcessingStage.valueOf(configuration.get(PROCESSING_STAGE_PROPERTY));
    }

    public void setProcessingStage(ProcessingStage processingStage) {
        ProcessingStage oldValue = getProcessingStage();
        configuration.set(PROCESSING_STAGE_PROPERTY, processingStage);
        firePropertyChange(PROCESSING_STAGE_PROPERTY, oldValue, getProcessingStage());
    }

    public String getEtName() {
        return configuration.get(ET_NAME_PROPERTY);
    }

    public void setEtName(String etName) {
        String oldValue = getEtName();
        configuration.set(ET_NAME_PROPERTY, etName);
        firePropertyChange(ET_NAME_PROPERTY, oldValue, getEtName());
    }

    public String getHost() {
        return configuration.get(HOST_PROPERTY);
    }

    public void setHost(String host) {
        String oldValue = getHost();
        configuration.set(HOST_PROPERTY, host);
        firePropertyChange(HOST_PROPERTY, oldValue, getHost());
    }

    public Integer getPort() {
        return configuration.getInteger(PORT_PROPERTY);
    }

    public void setPort(Integer port) {
        Integer oldValue = getPort();
        configuration.set(PORT_PROPERTY, port);
        firePropertyChange(PORT_PROPERTY, oldValue, getPort());
    }

    public Boolean getBlocking() {
        return configuration.getBoolean(BLOCKING_PROPERTY);
    }

    public void setBlocking(Boolean blocking) {
        Boolean oldValue = getBlocking();
        configuration.set(BLOCKING_PROPERTY, blocking);
        firePropertyChange(BLOCKING_PROPERTY, oldValue, getBlocking());
    }

    public Boolean getVerbose() {
        return configuration.getBoolean(VERBOSE_PROPERTY);
    }

    public void setVerbose(Boolean verbose) {
        Boolean oldValue = getVerbose();
        configuration.set(VERBOSE_PROPERTY, verbose);
        firePropertyChange(VERBOSE_PROPERTY, oldValue, getVerbose());
    }

    public String getStationName() {
        return configuration.get(STATION_NAME_PROPERTY);
    }

    public void setStationName(String stationName) {
        String oldValue = getStationName();
        configuration.set(STATION_NAME_PROPERTY, stationName);
        firePropertyChange(STATION_NAME_PROPERTY, oldValue, getStationName());
    }

    public Integer getChunkSize() {
        return configuration.getInteger(CHUNK_SIZE_PROPERTY);
    }

    public void setChunkSize(Integer chunkSize) {
        Integer oldValue = getChunkSize();
        configuration.set(CHUNK_SIZE_PROPERTY, chunkSize);
        firePropertyChange(CHUNK_SIZE_PROPERTY, oldValue, getChunkSize());
    }

    public Integer getQueueSize() {
        return configuration.getInteger(QUEUE_SIZE_PROPERTY);
    }

    public void setQueueSize(Integer queueSize) {
        Integer oldValue = getQueueSize();
        configuration.set(QUEUE_SIZE_PROPERTY, queueSize);
        firePropertyChange(QUEUE_SIZE_PROPERTY, oldValue, getQueueSize());
    }

    public Integer getStationPosition() {
        return configuration.getInteger(STATION_POSITION_PROPERTY);
    }

    public void setStationPosition(Integer stationPosition) {
        Integer oldValue = getStationPosition();
        configuration.set(STATION_POSITION_PROPERTY, stationPosition);
        firePropertyChange(STATION_POSITION_PROPERTY, oldValue, getStationPosition());
    }

    public Mode getWaitMode() {
        return Mode.valueOf(configuration.get(WAIT_MODE_PROPERTY));
    }

    public void setWaitMode(Mode waitMode) {
        Mode oldValue = getWaitMode();
        configuration.set(WAIT_MODE_PROPERTY, waitMode.name());
        firePropertyChange(WAIT_MODE_PROPERTY, oldValue, getWaitMode());
    }

    public Integer getWaitTime() {
        return configuration.getInteger(WAIT_TIME_PROPERTY);
    }

    public void setWaitTime(Integer waitTime) {
        Integer oldValue = getWaitTime();
        configuration.set(WAIT_TIME_PROPERTY, waitTime);
        firePropertyChange(WAIT_TIME_PROPERTY, oldValue, getWaitTime());
    }

    public Integer getPrescale() {
        return configuration.getInteger(PRESCALE_PROPERTY);
    }

    public void setPrescale(Integer prescale) {
        Integer oldValue = getPrescale();
        configuration.set(PRESCALE_PROPERTY, prescale);
        firePropertyChange(PRESCALE_PROPERTY, oldValue, getPrescale());
    }

    public void setUserRunNumber(Integer userRunNumber) {
        Integer oldValue = null;
        if (hasPropertyKey(USER_RUN_NUMBER_PROPERTY)) {
            oldValue = getUserRunNumber();
        }
        configuration.set(USER_RUN_NUMBER_PROPERTY, userRunNumber);
        firePropertyChange(USER_RUN_NUMBER_PROPERTY, oldValue, getUserRunNumber());
    }

    public Integer getUserRunNumber() {
        return configuration.getInteger(USER_RUN_NUMBER_PROPERTY);
    }

    public void setFreezeConditions(Boolean freezeConditions) {
        Boolean oldValue = null;
        if (hasPropertyKey(FREEZE_CONDITIONS_PROPERTY)) {
            oldValue = getFreezeConditions();
        }
        configuration.set(FREEZE_CONDITIONS_PROPERTY, freezeConditions);
        firePropertyChange(FREEZE_CONDITIONS_PROPERTY, oldValue, freezeConditions);
    }

    public Boolean getFreezeConditions() {
        return configuration.getBoolean(FREEZE_CONDITIONS_PROPERTY);
    }

    public void setMaxEvents(Long maxEvents) {
        Long oldValue = getMaxEvents();
        configuration.set(MAX_EVENTS_PROPERTY, maxEvents);
        firePropertyChange(MAX_EVENTS_PROPERTY, oldValue, getMaxEvents());
    }

    public Long getMaxEvents() {
        return configuration.getLong(MAX_EVENTS_PROPERTY);
    }
       
    public String getEtPath() {
        return getEtName() + "@" + getHost() + ":" + getPort();
    }

    public void remove(String property) {
        if (hasPropertyKey(property)) {
            Object oldValue = configuration.get(property);
            if (oldValue != null) {
                configuration.remove(property);
                firePropertyChange(property, oldValue, null);
            }
        }
    }

    public boolean hasPropertyKey(String key) {
        return configuration.hasKey(key);
    }

    public boolean hasValidProperty(String key) {
        return configuration.hasKey(key) && configuration.get(key) != null;
    }

    @Override
    public String[] getPropertyNames() {
        return CONFIG_PROPERTIES;
    }    
}
