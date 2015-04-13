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

    /**
     * Name of AIDA server.
     */
    public static final String AIDA_SERVER_NAME_PROPERTY = "AIDAServerName";

    /**
     * ET blocking setting.
     */
    public static final String BLOCKING_PROPERTY = "Blocking";

    /**
     * ET chunk size (number of events per ET <code>getEvents</code>).
     */
    public static final String CHUNK_SIZE_PROPERTY = "ChunkSize";

    /**
     * Conditions tag.
     */
    public static final String CONDITIONS_TAG_PROPERTY = "ConditionsTag";

    /**
     * The list of all property names.
     */
    static final String[] CONFIG_PROPERTIES = AbstractModel.getPropertyNames(ConfigurationModel.class);

    /**
     * The data source path which is a file path if using a file source (EVIO or LCIO file.
     */
    public static final String DATA_SOURCE_PATH_PROPERTY = "DataSourcePath";

    /**
     * The data source type (EVIO, LCIO or ET).
     *
     * @see org.hps.record.enums.DataSourceType
     * @return the data source type
     */
    public static final String DATA_SOURCE_TYPE_PROPERTY = "DataSourceType";

    /**
     * The detector alias which is pointing to a local compact.xml detector file.
     */
    public static final String DETECTOR_ALIAS_PROPERTY = "DetectorAlias";

    /**
     * The name of a detector model to use from the jar file.
     */
    public static final String DETECTOR_NAME_PROPERTY = "DetectorName";

    /**
     * Flag to enable disconnecting when an EVIO END event is received.
     */
    public static final String DISCONNECT_ON_END_RUN_PROPERTY = "DisconnectOnEndRun";

    /**
     * Flag to enable disconnecting if an event processing error occurs.
     */
    public static final String DISCONNECT_ON_ERROR_PROPERTY = "DisconnectOnError";

    /**
     * The name of the ET system which is generally a file on disk.
     */
    public static final String ET_NAME_PROPERTY = "EtName";

    /**
     * The name of the event builder for converting from EVIO to LCIO events.
     */
    public static final String EVENT_BUILDER_PROPERTY = "EventBuilderClassName";

    /**
     * Flag to freeze conditions system after initialization.
     */
    public static final String FREEZE_CONDITIONS_PROPERTY = "FreezeConditions";

    /**
     * The ET host property (TCP/IP host name).
     */
    public static final String HOST_PROPERTY = "Host";

    /**
     * The name of the output log file.
     */
    public static final String LOG_FILE_NAME_PROPERTY = "LogFileName";

    /**
     * The filter level for displaying records in the log table.
     */
    public static final String LOG_LEVEL_FILTER_PROPERTY = "LogLevelFilter";

    /**
     * The global log level.
     */
    public static final String LOG_LEVEL_PROPERTY = "LogLevel";

    /**
     * Flag to log to a file.
     */
    public static final String LOG_TO_FILE_PROPERTY = "LogToFile";

    /**
     * Max events after which session will be automatically ended.
     */
    public static final String MAX_EVENTS_PROPERTY = "MaxEvents";

    /**
     * The maximum number of recent files (hard-coded to 10 to match 0-9 shortcut mnemonics).
     */
    private static final int MAX_RECENT_FILES = 10;

    /**
     * The ET TCP/IP port.
     */
    public static final String PORT_PROPERTY = "Port";

    /**
     * The ET pre-scaling value which throttles event rate.
     */
    public static final String PRESCALE_PROPERTY = "Prescale";

    /**
     * The processing stage(s) to execute (ET, EVIO or LCIO).
     */
    public static final String PROCESSING_STAGE_PROPERTY = "ProcessingStage";

    /**
     * The ET queue size.
     */
    public static final String QUEUE_SIZE_PROPERTY = "QueueSize";

    /**
     * The list of recent files.
     */
    public static final String RECENT_FILES_PROPERTY = "RecentFiles";

    /**
     * The ET station name.
     */
    public static final String STATION_NAME_PROPERTY = "StationName";

    /**
     * The ET station position.
     */
    public static final String STATION_POSITION_PROPERTY = "StationPosition";

    /**
     * The steering file.
     */
    public static final String STEERING_FILE_PROPERTY = "SteeringFile";

    /**
     * The steering resource.
     */
    public static final String STEERING_RESOURCE_PROPERTY = "SteeringResource";

    /**
     * The steering type (file or resource).
     */
    public static final String STEERING_TYPE_PROPERTY = "SteeringType";

    /**
     * A user run number to use for initializing the conditions system.
     */
    public static final String USER_RUN_NUMBER_PROPERTY = "UserRunNumber";

    /**
     * The verbose setting for the ET system.
     */
    public static final String VERBOSE_PROPERTY = "Verbose";

    /**
     * The ET wait mode.
     */
    public static final String WAIT_MODE_PROPERTY = "WaitMode";

    /**
     * The ET wait time (if using timed wait mode).
     */
    public static final String WAIT_TIME_PROPERTY = "WaitTime";

    /**
     * The underlying properties for the model.
     */
    private Configuration configuration;

    /**
     * Class constructor.
     * <p>
     * Create a new model without any initial property settings.
     */
    public ConfigurationModel() {
        this.configuration = new Configuration();
    }

    /**
     * Class constructor.
     * <p>
     * Sets the properties from a configuration (file or resource).
     *
     * @param configuration the configuration containing property settings
     */
    public ConfigurationModel(final Configuration configuration) {
        this.configuration = configuration;
        this.fireModelChanged();
    }

    /**
     * Add a single recent file.
     *
     * @param recentFile the recent file to add
     */
    public void addRecentFile(final String recentFile) {
        if (!this.configuration.checkKey(RECENT_FILES_PROPERTY)) {
            this.configuration.set(RECENT_FILES_PROPERTY, recentFile);
            this.firePropertyChange(RECENT_FILES_PROPERTY, null, recentFile);
        } else {
            final List<String> recentFilesList = this.getRecentFilesList();
            if (!recentFilesList.contains(recentFile)) {
                if (this.getRecentFilesList().size() >= MAX_RECENT_FILES) {
                    // Bump the first file from the list if max recent files is exceeded (10 files).
                    recentFilesList.remove(0);
                    this.setRecentFilesList(recentFilesList);
                }
                final String oldValue = this.configuration.get(RECENT_FILES_PROPERTY);
                final String recentFiles = oldValue + "\n" + recentFile;
                this.configuration.set(RECENT_FILES_PROPERTY, recentFiles);
                this.firePropertyChange(RECENT_FILES_PROPERTY, oldValue, recentFile);
            }
        }

    }

    /**
     * Fire property change for all property keys.
     */
    @Override
    public void fireModelChanged() {
        this.firePropertiesChanged(this.configuration.getKeys());
    }

    /**
     * Get the AIDA server name.
     *
     * @return the AIDA server name
     */
    public String getAIDAServerName() {
        return this.configuration.get(AIDA_SERVER_NAME_PROPERTY);
    }

    /**
     * Get the ET blocking setting.
     *
     * @return the ET blocking setting
     */
    public Boolean getBlocking() {
        return this.configuration.getBoolean(BLOCKING_PROPERTY);
    }

    /**
     * Get the ET chunk size, which is the number of events that will be retrieved at once from the server.
     *
     * @return the ET chunk size
     */
    public Integer getChunkSize() {
        return this.configuration.getInteger(CHUNK_SIZE_PROPERTY);
    }

    /**
     * Get the conditions system tag.
     *
     * @return the conditions system tag
     */
    public String getConditionsTag() {
        return this.configuration.get(CONDITIONS_TAG_PROPERTY);
    }

    /**
     * Get the underlying configuration containing properties.
     *
     * @return the underlying configuration with properties settings
     */
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Get the data source path.
     *
     * @return the data source path
     */
    public String getDataSourcePath() {
        return this.configuration.get(DATA_SOURCE_PATH_PROPERTY);
    }

    /**
     * Get the data source type (EVIO, LCIO or ET).
     *
     * @return the data source type
     */
    public DataSourceType getDataSourceType() {
        if (this.configuration.checkKey(DATA_SOURCE_TYPE_PROPERTY)) {
            return DataSourceType.valueOf(this.configuration.get(DATA_SOURCE_TYPE_PROPERTY));
        } else {
            return null;
        }
    }

    /**
     * Get the detector alias which is a compact.xml file on disk.
     *
     * @return the detector alias
     */
    public String getDetectorAlias() {
        return this.configuration.get(DETECTOR_ALIAS_PROPERTY);
    }

    /**
     * Get the detector name.
     *
     * @return the detector name
     */
    public String getDetectorName() {
        return this.configuration.get(DETECTOR_NAME_PROPERTY);
    }

    /**
     * Get the disconnect on end run flag.
     *
     * @return the disconnect on end run flag
     */
    public Boolean getDisconnectOnEndRun() {
        return this.configuration.getBoolean(DISCONNECT_ON_END_RUN_PROPERTY);
    }

    /**
     * Get the disconnect on error flag.
     *
     * @return the disconnect on error flag
     */
    public Boolean getDisconnectOnError() {
        return this.configuration.getBoolean(DISCONNECT_ON_ERROR_PROPERTY);
    }

    /**
     * Get the ET system name.
     *
     * @return the ET system name
     */
    public String getEtName() {
        return this.configuration.get(ET_NAME_PROPERTY);
    }

    /**
     * Get the ET path from concatenating the ET system name, host and post (which is just used for the GUI).
     *
     * @return the ET path
     */
    public String getEtPath() {
        return this.getEtName() + "@" + this.getHost() + ":" + this.getPort();
    }

    /**
     * Get the event builder class name.
     *
     * @return the event builder class name
     */
    public String getEventBuilderClassName() {
        return this.configuration.get(EVENT_BUILDER_PROPERTY);
    }

    /**
     * Get the freeze conditions setting which will cause conditions system to be frozen after initialization with the
     * currently selected run number and detector name.
     *
     * @return the freeze conditions setting
     */
    public Boolean getFreezeConditions() {
        return this.configuration.getBoolean(FREEZE_CONDITIONS_PROPERTY);
    }

    /**
     * Get the ET host name.
     *
     * @return the ET host name
     */
    public String getHost() {
        return this.configuration.get(HOST_PROPERTY);
    }

    /**
     * Get the log file name.
     *
     * @return the log file name
     */
    public String getLogFileName() {
        return this.configuration.get(LOG_FILE_NAME_PROPERTY);
    }

    /**
     * Get the global log level.
     *
     * @return the global log level
     */
    public Level getLogLevel() {
        return Level.parse(this.configuration.get(LOG_LEVEL_PROPERTY));
    }

    /**
     * Get the log level filter for displaying messages in the log table.
     *
     * @return the log level filter
     */
    public Level getLogLevelFilter() {
        return Level.parse(this.configuration.get(LOG_LEVEL_FILTER_PROPERTY));
    }

    /**
     * Get the log to file setting which redirects monitoring application log messages from the console to a file.
     *
     * @return the log to file setting
     */
    public Boolean getLogToFile() {
        return this.configuration.getBoolean(LOG_TO_FILE_PROPERTY);
    }

    /**
     * Get the maximum number of events before disconnecting.
     *
     * @return the maximum number of events before disconnecting
     */
    public Long getMaxEvents() {
        return this.configuration.getLong(MAX_EVENTS_PROPERTY);
    }

    /**
     * Get the ET TCP/IP port value.
     *
     * @return the ET TCP/IP port value
     */
    public Integer getPort() {
        return this.configuration.getInteger(PORT_PROPERTY);
    }

    /**
     * Get the ET station prescale value.
     *
     * @return the ET station prescale value
     */
    public Integer getPrescale() {
        return this.configuration.getInteger(PRESCALE_PROPERTY);
    }

    /**
     * Get the processing stage.
     * <p>
     * Each level will execute the preceding ones, e.g. LCIO exectures EVIO, ET and LCIO event processing
     *
     * @return the processing stage to execute
     */
    public ProcessingStage getProcessingStage() {
        if (this.configuration.get(PROCESSING_STAGE_PROPERTY) == null) {
            throw new RuntimeException(PROCESSING_STAGE_PROPERTY + " is null!!!");
        }
        return ProcessingStage.valueOf(this.configuration.get(PROCESSING_STAGE_PROPERTY));
    }

    /**
     * Get the property names in the configuration.
     *
     * @return the property names in the configuration
     */
    @Override
    public String[] getPropertyNames() {
        return CONFIG_PROPERTIES;
    }

    public Integer getQueueSize() {
        return this.configuration.getInteger(QUEUE_SIZE_PROPERTY);
    }

    /**
     * Get recent files as a string with paths separated by the '\n' string.
     *
     * @return the recent files as a delimited string
     */
    public String getRecentFiles() {
        if (this.configuration.hasKey(RECENT_FILES_PROPERTY)) {
            return this.configuration.get(RECENT_FILES_PROPERTY);
        } else {
            return null;
        }
    }

    /**
     * Get the recent files list.
     * <p>
     * This is actually just a copy from the property, so to set the recent file list call
     * {@link #setRecentFiles(String)}.
     *
     * @return the recent files list
     */
    public List<String> getRecentFilesList() {
        final List<String> recentFilesList = new ArrayList<String>();
        if (this.configuration.hasKey(RECENT_FILES_PROPERTY)) {
            for (final String recentFile : this.configuration.get(RECENT_FILES_PROPERTY).split("\n")) {
                recentFilesList.add(recentFile);
            }
        }
        return recentFilesList;
    }

    /**
     * Get the ET station name.
     *
     * @return the ET station name
     */
    public String getStationName() {
        return this.configuration.get(STATION_NAME_PROPERTY);
    }

    /**
     * Get the ET station position.
     *
     * @return the ET station position
     */
    public Integer getStationPosition() {
        return this.configuration.getInteger(STATION_POSITION_PROPERTY);
    }

    /**
     * Get the steering file location (if using a file on disk).
     *
     * @return the XML file steering path
     */
    public String getSteeringFile() {
        return this.configuration.get(STEERING_FILE_PROPERTY);
    }

    /**
     * Get the steering resource (if using a jar resource).
     *
     * @return the steering resource location
     */
    public String getSteeringResource() {
        return this.configuration.get(STEERING_RESOURCE_PROPERTY);
    }

    /**
     * Get whether the steering is a file or resource.
     *
     * @return whether the steering is a file or resource
     */
    public SteeringType getSteeringType() {
        return SteeringType.valueOf(this.configuration.get(STEERING_TYPE_PROPERTY));
    }

    /**
     * Get the user run number for configuring the conditions system.
     *
     * @return the user run number for configuring the conditions system
     */
    public Integer getUserRunNumber() {
        return this.configuration.getInteger(USER_RUN_NUMBER_PROPERTY);
    }

    /**
     * Get the ET verbose flag.
     *
     * @return the ET verbose flag
     */
    public Boolean getVerbose() {
        return this.configuration.getBoolean(VERBOSE_PROPERTY);
    }

    /**
     * Get the ET wait mode.
     *
     * @return the ET wait mode
     */
    public Mode getWaitMode() {
        return Mode.valueOf(this.configuration.get(WAIT_MODE_PROPERTY));
    }

    /**
     * Get the ET wait time.
     *
     * @return the ET wait time
     */
    public Integer getWaitTime() {
        return this.configuration.getInteger(WAIT_TIME_PROPERTY);
    }

    /**
     * Return <code>true</code> if the given property key exists.
     *
     * @param key the property key
     * @return <code>true</code> if property key exists (still might be <code>null</code>)
     */
    public boolean hasPropertyKey(final String key) {
        return this.configuration.hasKey(key);
    }

    /**
     * Return <code>true</code> if the given property key exists and is non-null.
     *
     * @param key the property key
     * @return <code>true</code> if the property key exists and is non-null
     */
    public boolean hasValidProperty(final String key) {
        return this.configuration.checkKey(key);
    }

    /**
     * Merge another properties configuration into this one.
     * <p>
     * Settings from the merged properties will override this one.
     *
     * @param configuration the properties configuration to merge in
     */
    public void merge(final Configuration configuration) {
        this.configuration.merge(configuration);
        this.firePropertiesChanged(configuration.getKeys());
    }

    /**
     * Remove the given property which should remove its key and value.
     *
     * @param property the property to remove
     */
    public void remove(final String property) {
        if (this.hasPropertyKey(property)) {
            final Object oldValue = this.configuration.get(property);
            if (oldValue != null) {
                this.configuration.remove(property);
                this.firePropertyChange(property, oldValue, null);
            }
        }
    }

    /**
     * Set the name of the AIDA server.
     *
     * @param aidaServerName the name of the AIDA server
     */
    public void setAIDAServerName(final String aidaServerName) {
        final String oldValue = this.getAIDAServerName();
        this.configuration.set(AIDA_SERVER_NAME_PROPERTY, aidaServerName);
        this.firePropertyChange(AIDA_SERVER_NAME_PROPERTY, oldValue, this.getAIDAServerName());
    }

    /**
     * Set whether the ET station is blocking (generally this should not be set to <code>true</code>!)
     *
     * @param blocking <code>true</code> if station should be blocking
     */
    public void setBlocking(final Boolean blocking) {
        final Boolean oldValue = this.getBlocking();
        this.configuration.set(BLOCKING_PROPERTY, blocking);
        this.firePropertyChange(BLOCKING_PROPERTY, oldValue, this.getBlocking());
    }

    /**
     * The ET chunk size which is how many events should be returned at once in the array.
     *
     * @param chunkSize the ET chunk size
     */
    public void setChunkSize(final Integer chunkSize) {
        final Integer oldValue = this.getChunkSize();
        this.configuration.set(CHUNK_SIZE_PROPERTY, chunkSize);
        this.firePropertyChange(CHUNK_SIZE_PROPERTY, oldValue, this.getChunkSize());
    }

    /**
     * Set the conditions system tag.
     *
     * @param conditionsTag the conditions system tag
     */
    public void setConditionsTag(final String conditionsTag) {
        final String oldValue = this.getConditionsTag();
        this.configuration.set(CONDITIONS_TAG_PROPERTY, conditionsTag);
        this.firePropertyChange(CONDITIONS_TAG_PROPERTY, oldValue, this.getConditionsTag());
    }

    /**
     * Set a new configuration for the model which will fire property change events on all properties.
     *
     * @param configuration the configuration with properties for the model
     */
    public void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
        this.fireModelChanged();
    }

    /**
     * Set the data source path which should be a valid EVIO or LCIO file on an accessible disk.
     *
     * @param dataSourcePath the data source path
     */
    public void setDataSourcePath(final String dataSourcePath) {
        final String oldValue = this.getDataSourcePath();
        this.configuration.set(DATA_SOURCE_PATH_PROPERTY, dataSourcePath);
        this.firePropertyChange(DATA_SOURCE_PATH_PROPERTY, oldValue, this.getDataSourcePath());
    }

    /**
     * Set the data source type (EVIO, LCIO or ET).
     *
     * @param dataSourceType the data source type
     */
    public void setDataSourceType(final DataSourceType dataSourceType) {
        final DataSourceType oldValue = this.getDataSourceType();
        this.configuration.set(DATA_SOURCE_TYPE_PROPERTY, dataSourceType);
        this.firePropertyChange(DATA_SOURCE_TYPE_PROPERTY, oldValue, this.getDataSourceType());
    }

    /**
     * Set the detector alias which is an alternate file path to use for the detector model instead of the one from jar.
     *
     * @param detectorAlias the detector alias
     */
    public void setDetectorAlias(final String detectorAlias) {
        String oldValue = null;
        if (this.hasPropertyKey(DETECTOR_ALIAS_PROPERTY)) {
            oldValue = this.getDetectorAlias();
        }
        this.configuration.set(DETECTOR_ALIAS_PROPERTY, detectorAlias);
        this.firePropertyChange(DETECTOR_ALIAS_PROPERTY, oldValue, this.getDetectorAlias());
    }

    /**
     * Set the detector to load from the detector model resources in the jar.
     * <p>
     * These are present in the jar according to the LCSim convention:
     *
     * <pre>
     * ${DETECTOR_NAME}/detector.properties
     * </pre>
     * <p>
     * where <code>detector.properties</code> has the name of the detector matching the directory name.
     *
     * @param detectorName the name of the detector name
     */
    public void setDetectorName(final String detectorName) {
        final String oldValue = this.getDetectorName();
        this.configuration.set(DETECTOR_NAME_PROPERTY, detectorName);
        this.firePropertyChange(DETECTOR_NAME_PROPERTY, oldValue, this.getDetectorName());
    }

    /**
     * Set to <code>true</code> to disconnect when an EVIO END event is received.
     *
     * @param disconnectOnEndRun <code>true</code> to disconnect when an EVIO END event is received
     */
    public void setDisconnectOnEndRun(final Boolean disconnectOnEndRun) {
        final Boolean oldValue = this.getDisconnectOnEndRun();
        this.configuration.set(DISCONNECT_ON_END_RUN_PROPERTY, disconnectOnEndRun);
        this.firePropertyChange(DISCONNECT_ON_END_RUN_PROPERTY, oldValue, this.getDisconnectOnEndRun());
    }

    /**
     * Set to <code>true<code> to disconnect if event processing errors occur.
     *
     * @param disconnectOnError set to <code>true</code> to disconnect if event processing errors occur
     */
    public void setDisconnectOnError(final Boolean disconnectOnError) {
        final Boolean oldValue = this.getDisconnectOnError();
        this.configuration.set(DISCONNECT_ON_ERROR_PROPERTY, disconnectOnError);
        this.firePropertyChange(DISCONNECT_ON_ERROR_PROPERTY, oldValue, this.getDisconnectOnError());
    }

    /**
     * Set the name of the ET system which is usually a file.
     * <p>
     * Setting this to a valid file on disk being used by the ET server to buffer events will result in event processing
     * speedup.
     *
     * @param etName the name of the ET system (usually a path on disk used as an event buffer by an ET server)
     */
    public void setEtName(final String etName) {
        final String oldValue = this.getEtName();
        this.configuration.set(ET_NAME_PROPERTY, etName);
        this.firePropertyChange(ET_NAME_PROPERTY, oldValue, this.getEtName());
    }

    /**
     * Set the fully qualified class name of the {@link org.hps.record.LCSimEventBuilder} that should be used to build
     * LCIO events from EVIO.
     *
     * @param eventBuilderClassName the fully qualified class name of the event builder
     */
    public void setEventBuilderClassName(final String eventBuilderClassName) {
        final String oldValue = this.getEventBuilderClassName();
        this.configuration.set(EVENT_BUILDER_PROPERTY, eventBuilderClassName);
        this.firePropertyChange(EVENT_BUILDER_PROPERTY, oldValue, this.getEventBuilderClassName());
    }

    /**
     * Set to <code>true</code> to freeze the conditions system after initialization if there is also a valid detector
     * name and run number setting.
     *
     * @param freezeConditions <code>true</code> to freeze the conditions
     */
    public void setFreezeConditions(final Boolean freezeConditions) {
        Boolean oldValue = null;
        if (this.hasPropertyKey(FREEZE_CONDITIONS_PROPERTY)) {
            oldValue = this.getFreezeConditions();
        }
        this.configuration.set(FREEZE_CONDITIONS_PROPERTY, freezeConditions);
        this.firePropertyChange(FREEZE_CONDITIONS_PROPERTY, oldValue, freezeConditions);
    }

    /**
     * Set the ET TCP/IP host name of the server.
     *
     * @param host the host name
     */
    public void setHost(final String host) {
        final String oldValue = this.getHost();
        this.configuration.set(HOST_PROPERTY, host);
        this.firePropertyChange(HOST_PROPERTY, oldValue, this.getHost());
    }

    /**
     * Set the log file name if using file logging.
     *
     * @param logFileName the log file name
     */
    public void setLogFileName(final String logFileName) {
        final String oldValue = this.getLogFileName();
        this.configuration.set(LOG_FILE_NAME_PROPERTY, logFileName);
        this.firePropertyChange(LOG_FILE_NAME_PROPERTY, oldValue, this.getLogFileName());
    }

    /**
     * Set the global log level.
     *
     * @param level the global log level
     */
    public void setLogLevel(final Level level) {
        final Level oldValue = this.getLogLevel();
        this.configuration.set(LOG_LEVEL_PROPERTY, level.getName());
        this.firePropertyChange(LOG_LEVEL_PROPERTY, oldValue, this.getLogLevel());
    }

    /**
     * Set log filter level for the log table.
     *
     * @param level the log filter level
     */
    public void setLogLevelFilter(final Level level) {
        final Level oldValue = this.getLogLevelFilter();
        this.configuration.set(LOG_LEVEL_FILTER_PROPERTY, level.getName());
        this.firePropertyChange(LOG_LEVEL_FILTER_PROPERTY, oldValue, this.getLogLevelFilter());
    }

    /**
     * Set to <code>true</code> to send global messages to a file instead of the console.
     *
     * @param logToFile <code>true</code> to log to file
     */
    public void setLogToFile(final Boolean logToFile) {
        final Boolean oldValue = this.getLogToFile();
        this.configuration.set(LOG_TO_FILE_PROPERTY, logToFile);
        this.firePropertyChange(LOG_TO_FILE_PROPERTY, oldValue, this.getLogToFile());
    }

    /**
     * Set the maximum number of events to process before the session will automatically end.
     *
     * @param maxEvents the maximum number of events
     */
    public void setMaxEvents(final Long maxEvents) {
        final Long oldValue = this.getMaxEvents();
        this.configuration.set(MAX_EVENTS_PROPERTY, maxEvents);
        this.firePropertyChange(MAX_EVENTS_PROPERTY, oldValue, this.getMaxEvents());
    }

    /**
     * Set the TCP/IP port number of the ET server for the client connection.
     *
     * @param port the ET port number
     */
    public void setPort(final Integer port) {
        final Integer oldValue = this.getPort();
        this.configuration.set(PORT_PROPERTY, port);
        this.firePropertyChange(PORT_PROPERTY, oldValue, this.getPort());
    }

    /**
     * Set a prescale value for the ET station which will decrease the event rate.
     * <p>
     * A prescale of 2 would mean every 2nd event is processed, etc.
     *
     * @param prescale the ET station prescale value
     */
    public void setPrescale(final Integer prescale) {
        final Integer oldValue = this.getPrescale();
        this.configuration.set(PRESCALE_PROPERTY, prescale);
        this.firePropertyChange(PRESCALE_PROPERTY, oldValue, this.getPrescale());
    }

    /**
     * Set the processing stage which determines which event processing stages are executed.
     *
     * @param processingStage the processing stage to execute
     */
    public void setProcessingStage(final ProcessingStage processingStage) {
        final ProcessingStage oldValue = this.getProcessingStage();
        this.configuration.set(PROCESSING_STAGE_PROPERTY, processingStage);
        this.firePropertyChange(PROCESSING_STAGE_PROPERTY, oldValue, this.getProcessingStage());
    }

    /**
     * Set the ET queue size.
     *
     * @param queueSize the ET queue size
     */
    public void setQueueSize(final Integer queueSize) {
        final Integer oldValue = this.getQueueSize();
        this.configuration.set(QUEUE_SIZE_PROPERTY, queueSize);
        this.firePropertyChange(QUEUE_SIZE_PROPERTY, oldValue, this.getQueueSize());
    }

    /**
     * Set the recent files list, which is a list of "\n" delimited file paths.
     *
     * @param recentFiles the recent files list as a string
     */
    public void setRecentFiles(final String recentFiles) {
        String oldValue = null;
        if (this.configuration.checkKey(RECENT_FILES_PROPERTY)) {
            oldValue = this.configuration.get(RECENT_FILES_PROPERTY);
        }
        this.configuration.set(RECENT_FILES_PROPERTY, recentFiles);
        this.firePropertyChange(RECENT_FILES_PROPERTY, oldValue, this.configuration.get(RECENT_FILES_PROPERTY));
    }

    /**
     * Set the recent files list.
     * <p>
     * This method is not part of the public API and is only used internally.
     *
     * @param recentFilesList the recent files list
     */
    private void setRecentFilesList(final List<String> recentFilesList) {
        final StringBuffer sb = new StringBuffer();
        for (final String recentFile : recentFilesList) {
            sb.append(recentFile + "\n");
        }
        sb.setLength(sb.length() - 2);
        this.configuration.set(RECENT_FILES_PROPERTY, sb.toString());
    }

    /**
     * Set the ET station name.
     *
     * @param stationName the ET station name
     */
    public void setStationName(final String stationName) {
        final String oldValue = this.getStationName();
        this.configuration.set(STATION_NAME_PROPERTY, stationName);
        this.firePropertyChange(STATION_NAME_PROPERTY, oldValue, this.getStationName());
    }

    /**
     * Set the ET station position.
     *
     * @param stationPosition the ET station position
     */
    public void setStationPosition(final Integer stationPosition) {
        final Integer oldValue = this.getStationPosition();
        this.configuration.set(STATION_POSITION_PROPERTY, stationPosition);
        this.firePropertyChange(STATION_POSITION_PROPERTY, oldValue, this.getStationPosition());
    }

    /**
     * Set the steering file path.
     *
     * @param steeringFile the steering file path
     */
    public void setSteeringFile(final String steeringFile) {
        final String oldValue = this.getSteeringFile();
        this.configuration.set(STEERING_FILE_PROPERTY, steeringFile);
        this.firePropertyChange(STEERING_FILE_PROPERTY, oldValue, this.getSteeringFile());
    }

    /**
     * Set the steering file resource.
     *
     * @param steeringResource the steering file resource
     */
    public void setSteeringResource(final String steeringResource) {
        final String oldValue = this.getSteeringResource();
        this.configuration.set(STEERING_RESOURCE_PROPERTY, steeringResource);
        this.firePropertyChange(STEERING_RESOURCE_PROPERTY, oldValue, steeringResource);
    }

    /**
     * Set the steering type (file or resource).
     *
     * @param steeringType the steering type
     * @see SteeringType
     */
    public void setSteeringType(final SteeringType steeringType) {
        final SteeringType oldValue = this.getSteeringType();
        this.configuration.set(STEERING_TYPE_PROPERTY, steeringType.name());
        this.firePropertyChange(STEERING_TYPE_PROPERTY, oldValue, this.getSteeringType());
    }

    /**
     * Set a user run number which will be used to initialize the conditions system.
     * <p>
     * This should most likely be used with {@link #setFreezeConditions(Boolean)} or it is likely to be later overridden
     * by run numbers from the data.
     *
     * @param userRunNumber the user run number
     */
    public void setUserRunNumber(final Integer userRunNumber) {
        Integer oldValue = null;
        if (this.hasPropertyKey(USER_RUN_NUMBER_PROPERTY)) {
            oldValue = this.getUserRunNumber();
        }
        this.configuration.set(USER_RUN_NUMBER_PROPERTY, userRunNumber);
        this.firePropertyChange(USER_RUN_NUMBER_PROPERTY, oldValue, this.getUserRunNumber());
    }

    /**
     * Set verbose mode for the ET system.
     *
     * @param verbose the ET verbose flag
     */
    public void setVerbose(final Boolean verbose) {
        final Boolean oldValue = this.getVerbose();
        this.configuration.set(VERBOSE_PROPERTY, verbose);
        this.firePropertyChange(VERBOSE_PROPERTY, oldValue, this.getVerbose());
    }

    /**
     * Set the ET wait mode (timed, asynchronous or wait).
     *
     * @param waitMode the ET wait mode
     */
    public void setWaitMode(final Mode waitMode) {
        final Mode oldValue = this.getWaitMode();
        this.configuration.set(WAIT_MODE_PROPERTY, waitMode.name());
        this.firePropertyChange(WAIT_MODE_PROPERTY, oldValue, this.getWaitMode());
    }

    /**
     * Set the ET wait time, which is ignored if wait mode is not timed.
     *
     * @param waitTime the ET wait time
     */
    public void setWaitTime(final Integer waitTime) {
        final Integer oldValue = this.getWaitTime();
        this.configuration.set(WAIT_TIME_PROPERTY, waitTime);
        this.firePropertyChange(WAIT_TIME_PROPERTY, oldValue, this.getWaitTime());
    }
}
