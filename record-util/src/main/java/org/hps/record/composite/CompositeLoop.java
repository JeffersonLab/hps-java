package org.hps.record.composite;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.freehep.record.loop.DefaultRecordLoop;
import org.freehep.record.source.NoSuchRecordException;
import org.freehep.record.source.RecordSource;
import org.hps.record.EndRunException;
import org.hps.record.MaxRecordsException;
import org.hps.record.RecordProcessingException;
import org.hps.record.enums.DataSourceType;
import org.hps.record.enums.ProcessingStage;
import org.hps.record.et.EtEventProcessor;
import org.hps.record.et.EtEventSource;
import org.hps.record.evio.EvioEventProcessor;
import org.hps.record.evio.EvioFileSource;
import org.jlab.coda.et.exception.EtBusyException;
import org.jlab.coda.et.exception.EtClosedException;
import org.jlab.coda.et.exception.EtDeadException;
import org.jlab.coda.et.exception.EtEmptyException;
import org.jlab.coda.et.exception.EtException;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.et.exception.EtWakeUpException;
import org.lcsim.util.Driver;
import org.lcsim.util.loop.LCIOEventSource;

/**
 * Implementation of a composite record loop for processing ET, EVIO and LCIO events using a single record source.
 */
public final class CompositeLoop extends DefaultRecordLoop {

    /**
     * Return <code>true</code> if the Throwable is a type that can be thrown by the ET system when it is attempting to
     * read events from the server.
     * <p>
     * The <code>IOException</code> type is not included here but can actually be thrown by the ET system
     *
     * @param e the Exception
     * @return <code>true</code> if the object can be thrown by ET event reading
     */
    private static boolean isEtReadException(final Throwable e) {
        return e instanceof EtException || e instanceof EtDeadException || e instanceof EtClosedException
                || e instanceof EtEmptyException || e instanceof EtBusyException || e instanceof EtTimeoutException
                || e instanceof EtWakeUpException;
    }

    /**
     * The list of record adapters that are activated for every record being processed.
     */
    private final List<CompositeLoopAdapter> adapters = new ArrayList<CompositeLoopAdapter>();

    /**
     * The composite loop configuration.
     */
    private CompositeLoopConfiguration config = null;

    /**
     * Paused flag.
     */
    private boolean paused = false;

    /**
     * The record source.
     */
    private final CompositeRecordSource recordSource = new CompositeRecordSource();

    /**
     * Flag to stop on event processing errors.
     */
    private boolean stopOnErrors = true;

    /**
     * Class constructor.
     * <p>
     * The {@link #setCompositeLoopConfiguration(CompositeLoopConfiguration)} method must be called on the loop manually
     * to configure it for processing.
     */
    public CompositeLoop() {
        setRecordSource(this.recordSource);
    }

    /**
     * Create the loop with the given configuration.
     *
     * @param config the configuration parameters of the loop
     */
    public CompositeLoop(final CompositeLoopConfiguration config) {
        setRecordSource(this.recordSource);
        setCompositeLoopConfiguration(config);
    }

    /**
     * Add a {@link CompositeLoopAdapter} which will process {@link CompositeRecord} objects.
     *
     * @param adapter the CompositeLoopAdapter object
     */
    public void addAdapter(final CompositeLoopAdapter adapter) {
        addLoopListener(adapter);
        addRecordListener(adapter);
        this.adapters.add(adapter);
    }

    /**
     * Get the last error that occurred.
     *
     * @return the last error that occurred
     */
    public Throwable getLastError() {
        return this._exception;
    }

    /**
     * Handle errors from the client including any registered adapters.
     * <p>
     * If the loop is setup to try and continue on errors, only non-fatal record processing exceptions are ignored.
     *
     * @param x the error that occurred
     */
    @Override
    protected void handleClientError(final Throwable x) {

        x.printStackTrace();

        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }

        // Set the exception on the super class.
        this._exception = x;

        // Stop the event processing.
        this.execute(Command.STOP);
    }

    /**
     * Handle errors thrown by the <code>RecordSource</code>.
     *
     * @param x the error that occurred
     */
    @Override
    protected void handleSourceError(final Throwable x) {

        x.printStackTrace();

        // Is the error ignorable?
        if (isIgnorable(x)) {
            // Ignore the error!
            return;
        }

        // Set the exception on the super class.
        this._exception = x;

        // Stop the event processing.
        this.execute(Command.STOP);
    }

    /**
     * <p>
     * Return <code>true</code> if an error is ignore-able.
     * <p>
     * If <code>stopOnErrors</code> is true, then this method always returns <code>false</code>. Otherwise, the error's
     * cause determines whether the loop can continue processing.
     * <p>
     * The assumption here is that errors coming from event processing of the composite records are caught in the
     * adapters and then wrapped in a {@link org.hps.record.RecordProcessingException}. Certain errors which should
     * never be ignored are also wrapped in a similar way, so we need to check for these error types before assuming
     * that event processing can continue.
     *
     * @param x the error that occurred
     * @return <code>true</code> if the error is ignorable
     */
    private boolean isIgnorable(final Throwable x) {
        if (!this.stopOnErrors) {
            if (x instanceof RecordProcessingException) {
                final Throwable cause = x.getCause();
                return !(cause instanceof MaxRecordsException || cause instanceof EndRunException
                        || cause instanceof NoSuchRecordException || isEtReadException(cause));
            }
        }
        return false;
    }

    /**
     * Return <code>true</code> if the loop is paused.
     *
     * @return <code>true</code> if loop is paused
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * Loop over events from the source.
     *
     * @param number the number of events to process or -1L to process until record source is exhausted
     * @return the number of records that were processed
     */
    public long loop(final long number) {
        if (number < 0L) {
            execute(Command.GO, true);
        } else {
            execute(Command.GO_N, number, true);
            execute(Command.STOP);
        }
        return getSupplied();
    }

    /**
     * Pause the event processing.
     */
    public void pause() {
        execute(Command.PAUSE);
        this.paused = true;
    }

    /**
     * Resume event processing from pause mode.
     */
    public void resume() {
        this.paused = false;
    }

    /**
     * Configure the loop using a {@link CompositeLoopConfiguration} object.
     *
     * @param config the <code>CompositeLoopConfiguration</code> object containing the loop configuration
     */
    private void setCompositeLoopConfiguration(final CompositeLoopConfiguration config) {

        if (this.config != null) {
            throw new RuntimeException("CompositeLoop has already been configured.");
        }

        this.config = config;

        EtEventAdapter etAdapter = null;
        EvioEventAdapter evioAdapter = null;
        LcioEventAdapter lcioAdapter = null;
        final CompositeLoopAdapter compositeAdapter = new CompositeLoopAdapter();

        // Was there no RecordSource provided explicitly?
        if (config.getRecordSource() == null) {
            // Using an ET server connection?
            if (config.getDataSourceType().equals(DataSourceType.ET_SERVER)) {
                if (config.getEtConnection() != null) {
                    etAdapter = new EtEventAdapter(new EtEventSource(config.getEtConnection()));
                } else {
                    throw new IllegalArgumentException("Configuration is missing a valid ET connection.");
                    // Using an EVIO file?
                }
            } else if (config.getDataSourceType().equals(DataSourceType.EVIO_FILE)) {
                if (config.getFilePath() != null) {
                    evioAdapter = new EvioEventAdapter(new EvioFileSource(new File(config.getFilePath())));
                } else {
                    throw new IllegalArgumentException("Configuration is missing a file path.");
                }
                // Using an LCIO file?
            } else if (config.getDataSourceType().equals(DataSourceType.LCIO_FILE)) {
                if (config.getFilePath() != null) {
                    try {
                        lcioAdapter = new LcioEventAdapter(new LCIOEventSource(new File(config.getFilePath())));
                    } catch (final IOException e) {
                        throw new RuntimeException("Error configuring LCIOEventSource.", e);
                    }
                } else {
                    throw new IllegalArgumentException("Configuration is missing a file path.");
                }
            }
        }

        // Configure ET system.
        if (config.getDataSourceType() == DataSourceType.ET_SERVER) {
            addAdapter(etAdapter);
        }

        // Configure EVIO processing.
        if (config.getProcessingStage().ordinal() >= ProcessingStage.EVIO.ordinal()) {
            if (config.getDataSourceType().ordinal() <= DataSourceType.EVIO_FILE.ordinal()) {
                if (evioAdapter == null) {
                    evioAdapter = new EvioEventAdapter();
                }
                addAdapter(evioAdapter);
            }
        }

        // Configure LCIO processing.
        if (config.getProcessingStage().ordinal() >= ProcessingStage.LCIO.ordinal()) {
            if (lcioAdapter == null) {
                lcioAdapter = new LcioEventAdapter();
            }
            addAdapter(lcioAdapter);
            if (config.getLCSimEventBuilder() != null) {
                lcioAdapter.setLCSimEventBuilder(config.getLCSimEventBuilder());
            } else {
                throw new IllegalArgumentException("Missing an LCSimEventBuilder in configuration.");
            }
        }

        // Set whether to stop on event processing errors.
        setStopOnErrors(config.getStopOnErrors());

        // Set whether to stop on end run EVIO records.
        if (evioAdapter != null) {
            evioAdapter.setStopOnEndRun(config.getStopOnEndRun());
        }

        // Add EtEventProcessors to loop.
        if (etAdapter != null) {
            for (final EtEventProcessor processor : config.getEtProcessors()) {
                etAdapter.addProcessor(processor);
            }
        }

        // Add EvioEventProcessors to loop.
        if (evioAdapter != null) {
            for (final EvioEventProcessor processor : config.getEvioProcessors()) {
                evioAdapter.addProcessor(processor);
            }
        }

        // Add Drivers to loop.
        if (lcioAdapter != null) {
            for (final Driver driver : config.getDrivers()) {
                lcioAdapter.addDriver(driver);
            }
        }

        // Add CompositeLoopAdapter which should execute last.
        addAdapter(compositeAdapter);

        // Add CompositeRecordProcessors to loop.
        for (final CompositeRecordProcessor processor : config.getCompositeProcessors()) {
            compositeAdapter.addProcessor(processor);
        }

        if (config.getSupplyLcioEvents()) {
            addAdapter(new LcioEventSupplier(config.getTimeout(), config.getMaxQueueSize()));
        }

        // Max records was set?
        if (config.getMaxRecords() != -1) {
            compositeAdapter.addProcessor(new MaxRecordsProcessor(config.getMaxRecords()));
        }
    }

    /**
     * Set the configuration of the loop.
     * <p>
     * For this class, the <code>object</code> should have the type {@link CompositeLoopConfiguration}.
     *
     * @param object the configuration object
     */
    @Override
    public void setConfiguration(final Object object) {
        if (object instanceof CompositeLoopConfiguration) {
            setCompositeLoopConfiguration((CompositeLoopConfiguration) object);
        } else {
            throw new IllegalArgumentException("Wrong type of object to configure CompositeLoop: "
                    + object.getClass().getCanonicalName());
        }
    }

    /**
     * Set the <code>RecordSource</code> which provides <code>CompositeRecord</code> objects.
     *
     * @param source the record source
     */
    @Override
    public void setRecordSource(final RecordSource source) {
        if (!source.getRecordClass().isAssignableFrom(CompositeRecord.class)) {
            throw new IllegalArgumentException("The RecordSource has the wrong class.");
        }
        super.setRecordSource(source);
    }

    /**
     * Set to <code>true</code> in order to have this loop stop on all event processing errors.
     * <p>
     * Certain types of fatal errors will never be ignored regardless of this setting.
     *
     * @param stopOnErrors <code>true</code> for this loop to stop on errors
     */
    public void setStopOnErrors(final boolean stopOnErrors) {
        this.stopOnErrors = stopOnErrors;
    }
}
