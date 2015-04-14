package org.hps.record.composite;

import java.util.ArrayList;
import java.util.List;

import org.freehep.record.source.RecordSource;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.et.EtConnection;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.evio.EvioEventProcessor;
import org.lcsim.util.Driver;

/**
 * A configuration object for the {@link CompositeLoop}. The instance variables are readable within the package, e.g. by
 * {@link CompositeLoop}, but their values must be set through the public set methods when the class is used outside its
 * package. Depending on how this object is setup, some arguments may end up being ignored (e.g. setting a file path
 * when actually using an ET server, etc.).
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
// TODO: Add lcsim steering setting that uses JobControlManager to create Driver list.
public final class CompositeLoopConfiguration {

    /**
     * List of composite record processors.
     */
    private final List<CompositeRecordProcessor> compositeProcessors = new ArrayList<CompositeRecordProcessor>();

    /**
     * The ET connection which will be null if not using an ET system.
     */
    private EtConnection connection = null;

    /**
     * The list of LCSim Drivers.
     */
    private final List<Driver> drivers = new ArrayList<Driver>();

    /**
     * The list of ET processors.
     */
    private final List<EtEventProcessor> etProcessors = new ArrayList<EtEventProcessor>();

    /**
     * The LCSim event builder for creating LCIO events from EVIO (can be null).
     */
    private LCSimEventBuilder eventBuilder = null;

    /**
     * The list of EVIO processors.
     */
    private final List<EvioEventProcessor> evioProcessors = new ArrayList<EvioEventProcessor>();

    /**
     * Path to a file source.
     */
    private String filePath = null;

    /**
     * The maximum queue size if using an LCIO record queue.
     */
    private int maxQueueSize = -1;

    /**
     * The maximum number of records to process (-1 for unlimited).
     */
    private long maxRecords = -1;

    /**
     * The processing stage to execute (all prior stages will be included).
     */
    private ProcessingStage processingStage = ProcessingStage.LCIO;

    /**
     * The record source for the loop.
     */
    private RecordSource recordSource = null;

    /**
     * The data source type which defaults to an ET system.
     */
    private DataSourceType sourceType = DataSourceType.ET_SERVER;

    /**
     * Flag for stopping the event processing on end of run.
     */
    private boolean stopOnEndRun = true;

    /**
     * Flag for stopping the event processing on errors.
     */
    private boolean stopOnErrors = true;

    /**
     * Flag to enable supplying events to the JAS3 record source lookup system.
     */
    private boolean supplyLcioEvents;

    /**
     * The timeout on the LCIO record queue in milliseconds, if applicable.
     */
    private long timeout = -1L;

    /**
     * Add a {@link org.hps.record.composite.CompositeRecordProcessor} to the loop.
     *
     * @param processor The CompositeRecordProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(final CompositeRecordProcessor processor) {
        this.compositeProcessors.add(processor);
        return this;
    }

    /**
     * Add an org.lcsim <code>Driver</code> to the loop.
     *
     * @param processor The Driver.
     * @return This object.
     */
    public CompositeLoopConfiguration add(final Driver processor) {
        this.drivers.add(processor);
        return this;
    }

    /**
     * Add an {@link org.hps.record.et.EtEventProcessor} to the loop.
     *
     * @param processor The EtEventProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(final EtEventProcessor processor) {
        this.etProcessors.add(processor);
        return this;
    }

    /**
     * Add an {@link org.hps.record.evio.EvioEventProcessor} to the loop.
     *
     * @param processor The EvioEventProcessor.
     * @return This object.
     */
    public CompositeLoopConfiguration add(final EvioEventProcessor processor) {
        this.evioProcessors.add(processor);
        return this;
    }

    /**
     * Get the list of composite record processors.
     *
     * @return the list of composite record processors
     */
    List<CompositeRecordProcessor> getCompositeProcessors() {
        return this.compositeProcessors;
    }

    /**
     * Get the data source type (EVIO, LCIO or ET).
     *
     * @return the data source type
     */
    DataSourceType getDataSourceType() {
        return this.sourceType;
    }

    /**
     * Get the list of Drivers to add to the loop.
     *
     * @return the list of Drivers
     */
    List<Driver> getDrivers() {
        return this.drivers;
    }

    /**
     * Get the ET connection (can be <code>null</code>).
     *
     * @return the ET connection
     */
    EtConnection getEtConnection() {
        return this.connection;
    }

    /**
     * Get the list of ET event processors.
     *
     * @return the list of ET event processors
     */
    List<EtEventProcessor> getEtProcessors() {
        return this.etProcessors;
    }

    /**
     * Get the list of EVIO event processors.
     *
     * @return the list of EVIO event processors
     */
    List<EvioEventProcessor> getEvioProcessors() {
        return this.evioProcessors;
    }

    /**
     * Get the file path of a data source if using a file based source (can be null).
     *
     * @return the file path of a data source
     */
    String getFilePath() {
        return this.filePath;
    }

    /**
     * Get the builder for creating LCIO events from EVIO.
     *
     * @return the event builder
     */
    LCSimEventBuilder getLCSimEventBuilder() {
        return this.eventBuilder;
    }

    /**
     * Get the maximum size of the LCIO record queue.
     *
     * @return the maximum size of the LCIO record queue
     */
    int getMaxQueueSize() {
        return this.maxQueueSize;
    }

    /**
     * Get the maximum number of records to process.
     *
     * @return the maximum number of records to process
     */
    long getMaxRecords() {
        return this.maxRecords;
    }

    /**
     * Get the processing stage to execute (all prior stages will be executed as well).
     *
     * @return the processing stage to execute including prior stages
     */
    ProcessingStage getProcessingStage() {
        return this.processingStage;
    }

    /**
     * Get the record source.
     *
     * @return the record source
     */
    RecordSource getRecordSource() {
        return this.recordSource;
    }

    /**
     * Returns <code>true</code> if stop on end run is enabled.
     *
     * @return <code>true</code> if stop on end run is enabled
     */
    boolean getStopOnEndRun() {
        return this.stopOnEndRun;
    }

    /**
     * Returns <code>true</code> if stop on errors is enabled.
     *
     * @return <code>true</code> if stop on errors is enabled
     */
    boolean getStopOnErrors() {
        return this.stopOnErrors;
    }

    /**
     * Returns <code>true</code> if supplying LCIO events to JAS3 is enabled.
     *
     * @return <code>true</code> if supplying LCIO events to JAS3 is enabled
     */
    boolean getSupplyLcioEvents() {
        return this.supplyLcioEvents;
    }

    /**
     * Get the LCIO event queue timeout in milliseconds.
     *
     * @return the LCIO event queue timeout in milliseconds
     */
    long getTimeout() {
        return this.timeout;
    }

    /**
     * Set the data source type e.g. ET server, EVIO file or LCIO file.
     *
     * @param sourceType the data source type
     * @return this object
     */
    public CompositeLoopConfiguration setDataSourceType(final DataSourceType sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    /**
     * Set the ET connection parameters. This is ignored if using direct file streaming.
     *
     * @param connection the ET connection parameters
     * @return this object
     */
    public CompositeLoopConfiguration setEtConnection(final EtConnection connection) {
        this.connection = connection;
        return this;
    }

    /**
     * Set the full path to a file being used as an event source. This is ignored if the ET system is being used.
     *
     * @param filePath the full path to a file
     * @return this object
     */
    public CompositeLoopConfiguration setFilePath(final String filePath) {
        this.filePath = filePath;
        return this;
    }

    /**
     * Set the <code>LCSimEventBuilder</code> that will be used to translate from raw EVIO events to LCIO. The detector
     * name will be set on this object from within {@link CompositeLoop}.
     *
     * @param eventBuilder the LCSimEventBuilder object
     * @return this object
     */
    public CompositeLoopConfiguration setLCSimEventBuilder(final LCSimEventBuilder eventBuilder) {
        this.eventBuilder = eventBuilder;
        return this;
    }

    /**
     * Set the max queue size for the LCIO DataSource that hooks into JAS3.
     *
     * @param maxQueueSize the maximum queue size or -1 for unlimited
     * @return this object
     */
    public CompositeLoopConfiguration setMaxQueueSize(final int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    /**
     * Set the maximum number of records.
     *
     * @param maxRecords the maximum number of records
     * @return the maximum number of records
     */
    public CompositeLoopConfiguration setMaxRecords(final long maxRecords) {
        if (maxRecords < 1) {
            throw new IllegalArgumentException("Invalid maxRecords value: " + maxRecords);
        }
        this.maxRecords = maxRecords;
        return this;
    }

    /**
     * Set the processing stage to include i.e. ET only, ET to EVIO, or all three.
     * <p>
     * This may be ignored if the argument does not make sense given the {@link org.hps.record.enums.DataSourceType} of
     * this configuration.
     *
     * @param processingStage the processing stages to execute
     * @return this object
     */
    public CompositeLoopConfiguration setProcessingStage(final ProcessingStage processingStage) {
        this.processingStage = processingStage;
        return this;
    }

    /**
     * Set directly the <code>RecordSource</code> that will supply records.
     *
     * @param recordSource the <code>RecordSource</code> that will supply records
     * @return this object
     */
    public CompositeLoopConfiguration setRecordSource(final RecordSource recordSource) {
        this.recordSource = recordSource;
        return this;
    }

    /**
     * Set whether loop will stop when an end of run record is encountered e.g. from an EvioEvent.
     *
     * @param stopOnEndRun <code>true</code> to stop on end of run
     * @return this object
     */
    public CompositeLoopConfiguration setStopOnEndRun(final boolean stopOnEndRun) {
        this.stopOnEndRun = stopOnEndRun;
        return this;
    }

    /**
     * Set whether the loop will stop when event processing errors occur. Certain types of errors are considered fatal
     * or are used to control the loop and will never be ignored (e.g. ET system errors, etc.).
     *
     * @param stopOnErrors <code>true</code> to stop the loop when errors occur
     * @return this object
     */
    public CompositeLoopConfiguration setStopOnErrors(final boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
        return this;
    }

    /**
     * Set whether to supply LCIO events to a DataSource that will automatically register itself to JAS3 in order to
     * activate Wired, the LCSim Event Browser, etc.
     *
     * @param supplyLcioEvents <code>true</code> to supply LCIO events to JAS3
     * @return this object
     */
    public CompositeLoopConfiguration setSupplyLcioEvents(final boolean supplyLcioEvents) {
        this.supplyLcioEvents = supplyLcioEvents;
        return this;
    }

    /**
     * Set the timeout when calling <code>next</code> on the LCIO record queue.
     *
     * @param timeout the timeout in milliseconds
     * @return this object
     */
    public CompositeLoopConfiguration setTimeout(final long timeout) {
        this.timeout = timeout;
        return this;
    }
}
