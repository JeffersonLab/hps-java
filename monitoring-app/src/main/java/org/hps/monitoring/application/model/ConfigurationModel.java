package org.hps.monitoring.application.model;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.jlab.coda.et.enums.Mode;

/**
 * A model of the global configuration parameters that can be used to automatically update the GUI from a configuration
 * or push changes from GUI components into the current configuration.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ConfigurationModel extends AbstractModel {

    // Job setting properties.
    public static final String AIDA_SERVER_NAME_PROPERTY = "AIDAServerName";

    public static final String BLOCKING_PROPERTY = "Blocking";

    public static final String CHUNK_SIZE_PROPERTY = "ChunkSize";
    public static final String CONDITIONS_TAG_PROPERTY = "ConditionsTag";
    static final String[] CONFIG_PROPERTIES = AbstractModel.getPropertyNames(ConfigurationModel.class);
    public static final String CONFIGURATION_CHANGED = "configurationChanged";
    public static final String DATA_SOURCE_PATH_PROPERTY = "DataSourcePath";
    public static final String DATA_SOURCE_TYPE_PROPERTY = "DataSourceType";
    public static final String DETECTOR_ALIAS_PROPERTY = "DetectorAlias";
    public static final String DETECTOR_NAME_PROPERTY = "DetectorName";
    public static final String DISCONNECT_ON_END_RUN_PROPERTY = "DisconnectOnEndRun";
    public static final String DISCONNECT_ON_ERROR_PROPERTY = "DisconnectOnError";
    public static final String ET_NAME_PROPERTY = "EtName";
    public static final String EVENT_BUILDER_PROPERTY = "EventBuilderClassName";
    public static final String FREEZE_CONDITIONS_PROPERTY = "FreezeConditions";
    public static final String HOST_PROPERTY = "Host";
    public static final String LOG_FILE_NAME_PROPERTY = "LogFileName";
    public static final String LOG_LEVEL_FILTER_PROPERTY = "LogLevelFilter";
    public static final String LOG_LEVEL_PROPERTY = "LogLevel";
    public static final String LOG_TO_FILE_PROPERTY = "LogToFile";
    public static final String MAX_EVENTS_PROPERTY = "MaxEvents";
    private static final int MAX_RECENT_FILES = 10;
    public static final String PORT_PROPERTY = "Port";
    public static final String PRESCALE_PROPERTY = "Prescale";
    public static final String PROCESSING_STAGE_PROPERTY = "ProcessingStage";
    public static final String QUEUE_SIZE_PROPERTY = "QueueSize";
    public static final String RECENT_FILES_PROPERTY = "RecentFiles";
    public static final String STATION_NAME_PROPERTY = "StationName";
    public static final String STATION_POSITION_PROPERTY = "StationPosition";
    public static final String STEERING_FILE_PROPERTY = "SteeringFile";
    public static final String STEERING_RESOURCE_PROPERTY = "SteeringResource";
    public static final String STEERING_TYPE_PROPERTY = "SteeringType";
    public static final String USER_RUN_NUMBER_PROPERTY = "UserRunNumber";
    public static final String VERBOSE_PROPERTY = "Verbose";
    public static final String WAIT_MODE_PROPERTY = "WaitMode";
    public static final String WAIT_TIME_PROPERTY = "WaitTime";

    Configuration configuration;

    public ConfigurationModel() {
        this.configuration = new Configuration();
    }

    public ConfigurationModel(final Configuration configuration) {
        this.configuration = configuration;
        fireModelChanged();
    }

    public void addRecentFile(final String recentFile) {
        if (!this.configuration.checkKey(RECENT_FILES_PROPERTY)) {
            this.configuration.set(RECENT_FILES_PROPERTY, recentFile);
            firePropertyChange(RECENT_FILES_PROPERTY, null, recentFile);
        } else {
            final List<String> recentFilesList = getRecentFilesList();
            if (!recentFilesList.contains(recentFile)) {
                if (getRecentFilesList().size() >= MAX_RECENT_FILES) {
                    // Bump the first file from the list if max recent files is exceeded (10 files).
                    recentFilesList.remove(0);
                    setRecentFilesList(recentFilesList);
                }
                final String oldValue = this.configuration.get(RECENT_FILES_PROPERTY);
                final String recentFiles = oldValue + "\n" + recentFile;
                this.configuration.set(RECENT_FILES_PROPERTY, recentFiles);
                firePropertyChange(RECENT_FILES_PROPERTY, oldValue, recentFile);
            }
        }

    }

    @Override
    public void fireModelChanged() {
        firePropertiesChanged(this.configuration.getKeys());
    }

    public String getAIDAServerName() {
        return this.configuration.get(AIDA_SERVER_NAME_PROPERTY);
    }

    public Boolean getBlocking() {
        return this.configuration.getBoolean(BLOCKING_PROPERTY);
    }

    public Integer getChunkSize() {
        return this.configuration.getInteger(CHUNK_SIZE_PROPERTY);
    }

    public String getConditionsTag() {
        return this.configuration.get(CONDITIONS_TAG_PROPERTY);
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    public String getDataSourcePath() {
        return this.configuration.get(DATA_SOURCE_PATH_PROPERTY);
    }

    public DataSourceType getDataSourceType() {
        if (this.configuration.checkKey(DATA_SOURCE_TYPE_PROPERTY)) {
            return DataSourceType.valueOf(this.configuration.get(DATA_SOURCE_TYPE_PROPERTY));
        } else {
            return null;
        }
    }

    public String getDetectorAlias() {
        return this.configuration.get(DETECTOR_ALIAS_PROPERTY);
    }

    public String getDetectorName() {
        return this.configuration.get(DETECTOR_NAME_PROPERTY);
    }

    public Boolean getDisconnectOnEndRun() {
        return this.configuration.getBoolean(DISCONNECT_ON_END_RUN_PROPERTY);
    }

    public Boolean getDisconnectOnError() {
        return this.configuration.getBoolean(DISCONNECT_ON_ERROR_PROPERTY);
    }

    public String getEtName() {
        return this.configuration.get(ET_NAME_PROPERTY);
    }

    public String getEtPath() {
        return getEtName() + "@" + getHost() + ":" + getPort();
    }

    public String getEventBuilderClassName() {
        return this.configuration.get(EVENT_BUILDER_PROPERTY);
    }

    public Boolean getFreezeConditions() {
        return this.configuration.getBoolean(FREEZE_CONDITIONS_PROPERTY);
    }

    public String getHost() {
        return this.configuration.get(HOST_PROPERTY);
    }

    public String getLogFileName() {
        return this.configuration.get(LOG_FILE_NAME_PROPERTY);
    }

    public Level getLogLevel() {
        return Level.parse(this.configuration.get(LOG_LEVEL_PROPERTY));
    }

    public Level getLogLevelFilter() {
        return Level.parse(this.configuration.get(LOG_LEVEL_FILTER_PROPERTY));
    }

    public Boolean getLogToFile() {
        return this.configuration.getBoolean(LOG_TO_FILE_PROPERTY);
    }

    public Long getMaxEvents() {
        return this.configuration.getLong(MAX_EVENTS_PROPERTY);
    }

    public Integer getPort() {
        return this.configuration.getInteger(PORT_PROPERTY);
    }

    public Integer getPrescale() {
        return this.configuration.getInteger(PRESCALE_PROPERTY);
    }

    public ProcessingStage getProcessingStage() {
        if (this.configuration.get(PROCESSING_STAGE_PROPERTY) == null) {
            throw new RuntimeException(PROCESSING_STAGE_PROPERTY + " is null!!!");
        }
        return ProcessingStage.valueOf(this.configuration.get(PROCESSING_STAGE_PROPERTY));
    }

    @Override
    public String[] getPropertyNames() {
        return CONFIG_PROPERTIES;
    }

    public Integer getQueueSize() {
        return this.configuration.getInteger(QUEUE_SIZE_PROPERTY);
    }

    public String getRecentFiles() {
        if (this.configuration.hasKey(RECENT_FILES_PROPERTY)) {
            return this.configuration.get(RECENT_FILES_PROPERTY);
        } else {
            return null;
        }
    }

    public List<String> getRecentFilesList() {
        final List<String> recentFilesList = new ArrayList<String>();
        if (this.configuration.hasKey(RECENT_FILES_PROPERTY)) {
            for (final String recentFile : this.configuration.get(RECENT_FILES_PROPERTY).split("\n")) {
                recentFilesList.add(recentFile);
            }
        }
        return recentFilesList;
    }

    /*
     * public void setDataSource(String dataSource) { setDataSourcePath(dataSource); DataSourceType dst =
     * DataSourceType.getDataSourceType(dataSource); setDataSourceType(dst); }
     */

    public String getStationName() {
        return this.configuration.get(STATION_NAME_PROPERTY);
    }

    public Integer getStationPosition() {
        return this.configuration.getInteger(STATION_POSITION_PROPERTY);
    }

    public String getSteeringFile() {
        return this.configuration.get(STEERING_FILE_PROPERTY);
    }

    public String getSteeringResource() {
        return this.configuration.get(STEERING_RESOURCE_PROPERTY);
    }

    public SteeringType getSteeringType() {
        return SteeringType.valueOf(this.configuration.get(STEERING_TYPE_PROPERTY));
    }

    public Integer getUserRunNumber() {
        return this.configuration.getInteger(USER_RUN_NUMBER_PROPERTY);
    }

    public Boolean getVerbose() {
        return this.configuration.getBoolean(VERBOSE_PROPERTY);
    }

    public Mode getWaitMode() {
        return Mode.valueOf(this.configuration.get(WAIT_MODE_PROPERTY));
    }

    public Integer getWaitTime() {
        return this.configuration.getInteger(WAIT_TIME_PROPERTY);
    }

    public boolean hasPropertyKey(final String key) {
        return this.configuration.hasKey(key);
    }

    public boolean hasValidProperty(final String key) {
        return this.configuration.checkKey(key);
    }

    public void merge(final Configuration configuration) {
        this.configuration.merge(configuration);
        this.firePropertiesChanged(configuration.getKeys());
    }

    public void remove(final String property) {
        if (hasPropertyKey(property)) {
            final Object oldValue = this.configuration.get(property);
            if (oldValue != null) {
                this.configuration.remove(property);
                firePropertyChange(property, oldValue, null);
            }
        }
    }

    public void setAIDAServerName(final String AIDAServerName) {
        final String oldValue = getAIDAServerName();
        this.configuration.set(AIDA_SERVER_NAME_PROPERTY, AIDAServerName);
        firePropertyChange(AIDA_SERVER_NAME_PROPERTY, oldValue, getAIDAServerName());
    }

    public void setBlocking(final Boolean blocking) {
        final Boolean oldValue = getBlocking();
        this.configuration.set(BLOCKING_PROPERTY, blocking);
        firePropertyChange(BLOCKING_PROPERTY, oldValue, getBlocking());
    }

    public void setChunkSize(final Integer chunkSize) {
        final Integer oldValue = getChunkSize();
        this.configuration.set(CHUNK_SIZE_PROPERTY, chunkSize);
        firePropertyChange(CHUNK_SIZE_PROPERTY, oldValue, getChunkSize());
    }

    public void setConditionsTag(final String conditionsTag) {
        final String oldValue = getConditionsTag();
        this.configuration.set(CONDITIONS_TAG_PROPERTY, conditionsTag);
        firePropertyChange(CONDITIONS_TAG_PROPERTY, oldValue, getConditionsTag());
    }

    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        fireModelChanged();
    }

    public void setDataSourcePath(final String dataSourcePath) {
        final String oldValue = getDataSourcePath();
        this.configuration.set(DATA_SOURCE_PATH_PROPERTY, dataSourcePath);
        firePropertyChange(DATA_SOURCE_PATH_PROPERTY, oldValue, getDataSourcePath());
    }

    public void setDataSourceType(final DataSourceType dataSourceType) {
        final DataSourceType oldValue = getDataSourceType();
        this.configuration.set(DATA_SOURCE_TYPE_PROPERTY, dataSourceType);
        firePropertyChange(DATA_SOURCE_TYPE_PROPERTY, oldValue, getDataSourceType());
    }

    public void setDetectorAlias(final String detectorAlias) {
        String oldValue = null;
        if (hasPropertyKey(DETECTOR_ALIAS_PROPERTY)) {
            oldValue = getDetectorAlias();
        }
        this.configuration.set(DETECTOR_ALIAS_PROPERTY, detectorAlias);
        firePropertyChange(DETECTOR_ALIAS_PROPERTY, oldValue, getDetectorAlias());
    }

    public void setDetectorName(final String detectorName) {
        final String oldValue = getDetectorName();
        this.configuration.set(DETECTOR_NAME_PROPERTY, detectorName);
        firePropertyChange(DETECTOR_NAME_PROPERTY, oldValue, getDetectorName());
    }

    public void setDisconnectOnEndRun(final Boolean disconnectOnEndRun) {
        final Boolean oldValue = getDisconnectOnEndRun();
        this.configuration.set(DISCONNECT_ON_END_RUN_PROPERTY, disconnectOnEndRun);
        firePropertyChange(DISCONNECT_ON_END_RUN_PROPERTY, oldValue, getDisconnectOnEndRun());
    }

    public void setDisconnectOnError(final Boolean disconnectOnError) {
        final Boolean oldValue = getDisconnectOnError();
        this.configuration.set(DISCONNECT_ON_ERROR_PROPERTY, disconnectOnError);
        firePropertyChange(DISCONNECT_ON_ERROR_PROPERTY, oldValue, getDisconnectOnError());
    }

    public void setEtName(final String etName) {
        final String oldValue = getEtName();
        this.configuration.set(ET_NAME_PROPERTY, etName);
        firePropertyChange(ET_NAME_PROPERTY, oldValue, getEtName());
    }

    public void setEventBuilderClassName(final String eventBuilderClassName) {
        final String oldValue = getEventBuilderClassName();
        this.configuration.set(EVENT_BUILDER_PROPERTY, eventBuilderClassName);
        firePropertyChange(EVENT_BUILDER_PROPERTY, oldValue, getEventBuilderClassName());
    }

    public void setFreezeConditions(final Boolean freezeConditions) {
        Boolean oldValue = null;
        if (hasPropertyKey(FREEZE_CONDITIONS_PROPERTY)) {
            oldValue = getFreezeConditions();
        }
        this.configuration.set(FREEZE_CONDITIONS_PROPERTY, freezeConditions);
        firePropertyChange(FREEZE_CONDITIONS_PROPERTY, oldValue, freezeConditions);
    }

    public void setHost(final String host) {
        final String oldValue = getHost();
        this.configuration.set(HOST_PROPERTY, host);
        firePropertyChange(HOST_PROPERTY, oldValue, getHost());
    }

    public void setLogFileName(final String logFileName) {
        final String oldValue = getLogFileName();
        this.configuration.set(LOG_FILE_NAME_PROPERTY, logFileName);
        firePropertyChange(LOG_FILE_NAME_PROPERTY, oldValue, getLogFileName());
    }

    public void setLogLevel(final Level level) {
        final Level oldValue = getLogLevel();
        this.configuration.set(LOG_LEVEL_PROPERTY, level.getName());
        firePropertyChange(LOG_LEVEL_PROPERTY, oldValue, getLogLevel());
    }

    public void setLogLevelFilter(final Level level) {
        final Level oldValue = getLogLevelFilter();
        this.configuration.set(LOG_LEVEL_FILTER_PROPERTY, level.getName());
        firePropertyChange(LOG_LEVEL_FILTER_PROPERTY, oldValue, getLogLevelFilter());
    }

    public void setLogToFile(final Boolean logToFile) {
        final Boolean oldValue = getLogToFile();
        this.configuration.set(LOG_TO_FILE_PROPERTY, logToFile);
        firePropertyChange(LOG_TO_FILE_PROPERTY, oldValue, getLogToFile());
    }

    public void setMaxEvents(final Long maxEvents) {
        final Long oldValue = getMaxEvents();
        this.configuration.set(MAX_EVENTS_PROPERTY, maxEvents);
        firePropertyChange(MAX_EVENTS_PROPERTY, oldValue, getMaxEvents());
    }

    public void setPort(final Integer port) {
        final Integer oldValue = getPort();
        this.configuration.set(PORT_PROPERTY, port);
        firePropertyChange(PORT_PROPERTY, oldValue, getPort());
    }

    public void setPrescale(final Integer prescale) {
        final Integer oldValue = getPrescale();
        this.configuration.set(PRESCALE_PROPERTY, prescale);
        firePropertyChange(PRESCALE_PROPERTY, oldValue, getPrescale());
    }

    public void setProcessingStage(final ProcessingStage processingStage) {
        final ProcessingStage oldValue = getProcessingStage();
        this.configuration.set(PROCESSING_STAGE_PROPERTY, processingStage);
        firePropertyChange(PROCESSING_STAGE_PROPERTY, oldValue, getProcessingStage());
    }

    public void setQueueSize(final Integer queueSize) {
        final Integer oldValue = getQueueSize();
        this.configuration.set(QUEUE_SIZE_PROPERTY, queueSize);
        firePropertyChange(QUEUE_SIZE_PROPERTY, oldValue, getQueueSize());
    }

    public void setRecentFiles(final String recentFiles) {
        String oldValue = null;
        if (this.configuration.checkKey(RECENT_FILES_PROPERTY)) {
            oldValue = this.configuration.get(RECENT_FILES_PROPERTY);
        }
        this.configuration.set(RECENT_FILES_PROPERTY, recentFiles);
        firePropertyChange(RECENT_FILES_PROPERTY, oldValue, this.configuration.get(RECENT_FILES_PROPERTY));
    }

    private void setRecentFilesList(final List<String> recentFilesList) {
        final StringBuffer sb = new StringBuffer();
        for (final String recentFile : recentFilesList) {
            sb.append(recentFile + "\n");
        }
        sb.setLength(sb.length() - 2);
        this.configuration.set(RECENT_FILES_PROPERTY, sb.toString());
    }

    public void setStationName(final String stationName) {
        final String oldValue = getStationName();
        this.configuration.set(STATION_NAME_PROPERTY, stationName);
        firePropertyChange(STATION_NAME_PROPERTY, oldValue, getStationName());
    }

    public void setStationPosition(final Integer stationPosition) {
        final Integer oldValue = getStationPosition();
        this.configuration.set(STATION_POSITION_PROPERTY, stationPosition);
        firePropertyChange(STATION_POSITION_PROPERTY, oldValue, getStationPosition());
    }

    public void setSteeringFile(final String steeringFile) {
        final String oldValue = getSteeringFile();
        this.configuration.set(STEERING_FILE_PROPERTY, steeringFile);
        firePropertyChange(STEERING_FILE_PROPERTY, oldValue, getSteeringFile());
    }

    public void setSteeringResource(final String steeringResource) {
        final String oldValue = getSteeringResource();
        this.configuration.set(STEERING_RESOURCE_PROPERTY, steeringResource);
        firePropertyChange(STEERING_RESOURCE_PROPERTY, oldValue, steeringResource);
    }

    public void setSteeringType(final SteeringType steeringType) {
        final SteeringType oldValue = getSteeringType();
        this.configuration.set(STEERING_TYPE_PROPERTY, steeringType.name());
        firePropertyChange(STEERING_TYPE_PROPERTY, oldValue, getSteeringType());
    }

    public void setUserRunNumber(final Integer userRunNumber) {
        Integer oldValue = null;
        if (hasPropertyKey(USER_RUN_NUMBER_PROPERTY)) {
            oldValue = getUserRunNumber();
        }
        this.configuration.set(USER_RUN_NUMBER_PROPERTY, userRunNumber);
        firePropertyChange(USER_RUN_NUMBER_PROPERTY, oldValue, getUserRunNumber());
    }

    public void setVerbose(final Boolean verbose) {
        final Boolean oldValue = getVerbose();
        this.configuration.set(VERBOSE_PROPERTY, verbose);
        firePropertyChange(VERBOSE_PROPERTY, oldValue, getVerbose());
    }

    public void setWaitMode(final Mode waitMode) {
        final Mode oldValue = getWaitMode();
        this.configuration.set(WAIT_MODE_PROPERTY, waitMode.name());
        firePropertyChange(WAIT_MODE_PROPERTY, oldValue, getWaitMode());
    }

    public void setWaitTime(final Integer waitTime) {
        final Integer oldValue = getWaitTime();
        this.configuration.set(WAIT_TIME_PROPERTY, waitTime);
        firePropertyChange(WAIT_TIME_PROPERTY, oldValue, getWaitTime());
    }
}
