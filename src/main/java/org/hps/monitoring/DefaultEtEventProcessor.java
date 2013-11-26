package org.hps.monitoring;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jlab.coda.et.EtEvent;
import org.jlab.coda.et.enums.Modify;
import org.jlab.coda.et.exception.EtTimeoutException;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.event.EventHeader;
import org.lcsim.hps.evio.EventConstants;
import org.lcsim.hps.evio.LCSimEventBuilder;
import org.lcsim.job.JobControlManager;

/**
 * This class executes the default event processing chain for HPS Test Run monitoring: ET -> EVIO -> LCIO.
 * {@link EtEventListener} objects can be registered with this class to receive notifications 
 * as processing occurs.  This is how the {@link MonitoringApplication} updates the GUI without the processor
 * having a direct reference to the actual application.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: DefaultEtEventProcessor.java,v 1.13 2013/10/25 23:13:53 jeremy Exp $
 */
public class DefaultEtEventProcessor implements EtEventProcessor {

    private int maxEvents;
    private int eventsProcessed;
    private int errorEvents;
    private LCSimEventBuilder eventBuilder;
    private JobControlManager jobManager;
    private EtConnection et;
    private List<EtEventListener> listeners = new ArrayList<EtEventListener>();
    private int status;
    private volatile boolean stopProcessing;
    private volatile boolean nextEvents;
    private volatile boolean stopOnError;
    private volatile boolean pauseMode = false;
    private volatile boolean done = false;
    private volatile boolean blocked = false;
    private static Logger logger;

    /**
     * Create an instance of the default EtEvent processor.
     * @param et The ET connection.
     * @param eventBuilder The builder for converting from EVIO to LCIO.
     * @param jobManager The LCSim job manager.
     * @param maxEvents Maximum number of events to process in the session.
     * @param stopOnError True to stop on first error; false to continue processing.
     * @param logHandler The log handler for redirecting log messages.
     */
    DefaultEtEventProcessor(
            EtConnection et,
            LCSimEventBuilder eventBuilder,
            JobControlManager jobManager,
            int maxEvents,
            boolean stopOnError,
            Handler logHandler) {

        // Set class parameters.
        this.et = et;
        this.eventBuilder = eventBuilder;
        this.jobManager = jobManager;
        this.maxEvents = maxEvents;
        this.stopOnError = stopOnError;

        // Setup the logger if needed.  This will only happen once.
        if (logger == null) {
            logger = Logger.getLogger(this.getClass().getSimpleName());
            logger.setUseParentHandlers(false);
            logger.addHandler(logHandler);
            logger.setLevel(Level.ALL);
        }

        // Log an initialization message.
        logger.log(Level.CONFIG, "Event processor initialized successfully.");
    }

    /**
     * Enable pause mode to pause between single events.
     * @param p True to pause after each set of events; false to do real-time processing.
     */
    void setPauseMode(boolean p) {
        this.pauseMode = p;
    }

    /**
     * Add an <code>EtEventListener</code> that will receive a callback for each event.
     * @param callme The EtEventListener to add.
     */
    public void addListener(EtEventListener callme) {
        listeners.add(callme);
    }

    /**
     * Set the maximum number of events before processing should stop.
     * @param maxEvents The maximum number of events before processing should be stopped.
     */
    public void setMaxEvents(int maxEvents) {
        this.maxEvents = maxEvents;
        logger.log(Level.INFO, "Set maxEvents to <{0}>.", maxEvents);
    }

    /**
     * Notify listeners of start of event processing. 
     */
    private void begin() {
        for (EtEventListener listener : listeners) {
            listener.begin();
        }
    }

    /**
     * Notify listeners of start of an event.
     */
    private void startOfEvent() {
        for (EtEventListener listener : listeners) {
            listener.startOfEvent();
        }
    }

    /**
     * Notify listeners of end of an event.
     */
    private void endOfEvent() {
        for (EtEventListener listener : listeners) {
            listener.endOfEvent();
        }
    }

    /**
     * Notify listeners that there was an error processing an event.
     */
    private void errorOnEvent() {
        ++errorEvents;
        for (EtEventListener listener : listeners) {
            listener.errorOnEvent();
        }
    }

    /**
     * Notify listeners of end of job.
     */
    private void finish() {
        logger.log(Level.FINER, "Calling finish() methods of listeners.");
        for (EtEventListener listener : listeners) {
            listener.finish();
        }
    }

    /**
     * Set the flag to stop processing events, which will cause the 
     * event processing loop to stop the next time it checks this value.
     */
    public void stop() {
        this.stopProcessing = true;
        logger.log(Level.FINEST, "Received stop request.");
    }

    /**
     * Get the total number of events processed thusfar.
     * @return The number of events processed.
     */
    public int getNumberOfEventsProcessed() {
        return eventsProcessed;
    }

    /**
     * Get the total number of errors that have occurred.
     * @return The number of errors that have happened.
     */
    public int getNumberOfErrors() {
        return errorEvents;
    }

    /**
     * Get the maximum number of events that should be processed before stopping.
     * @return The maximum number of events to process.
     */
    public int getMaxEvents() {
        return maxEvents;
    }

    /**
     * Reset the event counter.
     */
    public void resetNumberOfEventsProcessed() {
        eventsProcessed = 0;
        errorEvents = 0;
    }

    /**
     * Get the status of the processor.
     * @return The status of the processor.
     */
    public int getStatus() {
        return status;
    }

    /**
     * Get EtEvents and process them.
     * If in wait mode, continue after waitTime in microseconds if there are no events.
     * If in async mode, expects non-empty event list immediately or an error occurs.
     * If in sleep mode, the call to getEvents() will block, including requests to wake-up, until events arrive.
     */
    public void processEtEvents() throws EtTimeoutException, MaxEventsException, Exception {

        /* 
         * Get events from the ET system.  
         * WARNING: This can potentially block forever until it receives events. 
         */
        blocked = true;
        EtEvent[] mevs = et.sys.getEvents(et.att, et.param.waitMode, Modify.NOTHING, et.param.waitTime, et.param.chunk);
        blocked = false;

        // Loop over retrieved EtEvents.
        for (EtEvent mev : mevs) {

            // If running in pause mode, then hold here until the "Next Event" button is pressed.
            if (pauseMode && this.status == ConnectionStatus.CONNECTED) {
                logger.log(Level.FINEST, "Pausing until next events requested.");
                while (true) {
                    if (nextEvents) {
                        logger.log(Level.FINEST, "User requested next set of events.");
                        nextEvents = false;
                        break;
                    } else if (stopProcessing) {
                        logger.log(Level.FINEST, "Stop was requested in inner event processing loop.");
                        break;
                    }
                }
            }

            // Got an external request to stop processing.  Break out of loop.
            if (stopProcessing) {
                break;
            }

            // Process a single EtEvent using the default processing chain.
            try {
                processEtEvent(mev);
            // Catch event processing errors, including ET -> EVIO, EVIO -> LCIO, and LCSim Driver execution.
            } catch (EventProcessingException e) {       
                /* Stack trace will show up log file. */
                e.getCause().printStackTrace();
                /* Print error message to log table. */
                logger.log(Level.SEVERE, e.getMessage());
                /* Print event number to log table. */
                logger.log(Level.SEVERE, "Error processing event <" + this.eventsProcessed + ">.");
                errorOnEvent();
                if (stopOnError) {
                    this.status = ConnectionStatus.ERROR;
                    logger.log(Level.INFO, "Exiting on first error.");
                    break;
                }
            }
        }
    }

    /**
     * Process a single <code>EtEvent</code>.
     * The exceptions throw by this method will be caught and handled from within {@link #processEtEvents()}.
     * @param mev The EtEvent to process.
     */
    public void processEtEvent(EtEvent mev) throws EventProcessingException, MaxEventsException {
                
        /* Check if max events was reached or exceeded. */
        if (maxEvents != -1 && eventsProcessed >= maxEvents) {
            logger.log(Level.INFO, "Reached max events.");
            throw new MaxEventsException();
        }
        
        /* Check that the supplied EtEvent was not null. An exception is thrown if it is null. */
    	if (mev == null) {
    	    logger.log(Level.SEVERE, "Supplied EtEvent is null.");
    	    throw new EventProcessingException("The supplied EtEvent is null");
    	}

        /* Notify listeners of start of event. */
        startOfEvent();
        
        /* Increment the number of events processed. */
        ++eventsProcessed;
        
        /* 
         * Create the EVIO event. 
         */
        EvioEvent evioEvent = null;
        boolean isPhysicsEvent = false;
        try {
            evioEvent = createEvioEvent(mev);
        } catch (Exception e) {
            throw new EventProcessingException("Failed to create EVIO event.", e);
        } 
        
        /*
         * Process the EVIO event if the EvioReader created a non-null object.
         */
        if (evioEvent != null) {
            
            /* Check for physics event. */
            isPhysicsEvent = eventBuilder.isPhysicsEvent(evioEvent);
            
            /* 
             * This is basically just redundantly printing information about the event to System.out, 
             * but it also sets some internal state within the event builder so leave it for now.
             */
            eventBuilder.readEvioEvent(evioEvent);
    
            /* Log non-physics event information. */
            logEvioEvent(evioEvent);
            
            /* 
             * Notify listeners of non-physics events.
             */
            if (isPreStartEvent(evioEvent)) {
                preStartEvent(evioEvent);
            } else if (isEndEvent(evioEvent)) {
                endRun(evioEvent);
            }
            
            /* Check that the event has physics type. */
            if (isPhysicsEvent) {
                
                /*
                 * Use the event builder to create the lcsim event.
                 */
                EventHeader lcsimEvent = null;
                try {
                    lcsimEvent = eventBuilder.makeLCSimEvent(evioEvent);
                } catch (Exception e) {
                    throw new EventProcessingException("Failed to create LCSim event.", e);
                }

                /*
                 * Process the event using the lcsim JobManager.
                 */
                if (lcsimEvent != null) {
                    try {
                        jobManager.processEvent(lcsimEvent);
                    } catch (Exception e) {
                        throw new EventProcessingException("Error processing the LCSim event.", e);
                    }
                } else {
                    throw new EventProcessingException("The builder returned a null lcsim event.");
                }
            }
        } else {
            /*
             * The EVIO event was null, so print an error message to the log.  This happens enough 
             * that an exception is NOT thrown.  Ideally an error should be thrown but this
             * means that the "disconnect on error" setting becomes completely useless, as this problem 
             * basically happens at least a few times in every test run EVIO file.
             */
            logger.log(Level.SEVERE, "Error converting event <" + this.eventsProcessed + "> to EVIO.");
            throw new EventProcessingException("The EVIO reader returned a null event.");
        }

        /* Notify listeners of end event. */
        endOfEvent();
    }

    /**
     * Static utility method for creating an EVIO event from an EtEvent
     * @param etEvent The EtEvent.
     * @return The EvioEvent.
     * @throws IOException
     * @throws EvioException
     * @throws BufferUnderflowException
     */
    private static final EvioEvent createEvioEvent(EtEvent etEvent) throws IOException, EvioException, BufferUnderflowException {
        return (new EvioReader(etEvent.getDataBuffer())).parseNextEvent();
    }

    /**
     * Run the event processing from the ET connection until the job finishes.
     */
    public void process() {

        // Set current status to connected.
        this.status = ConnectionStatus.CONNECTED;

        // Clear any leftover stop request before starting, just in case.
        stopProcessing = false;

        // A new job is starting so this flag gets reset.        
        done = false;

        // Notify listeners of job start.
        begin();

        // Loop until fatal error or stop flag is changed to true.
        while (true) {

            // Got a request to stop processing.
            if (stopProcessing) {
                logger.log(Level.FINEST, "Outer event processing loop got stop request.");
                this.status = ConnectionStatus.DISCONNECT_REQUESTED;
                break;
            }

            /* Try to process the next set of ET events. */
            try {
                processEtEvents();
            /* The ET system timed out in the getEvents() method. */
            } catch (EtTimeoutException e) {
                logger.log(Level.WARNING, "ET connection timed out.");
                this.status = ConnectionStatus.TIMED_OUT;
            /* Event processing reached the maximum number of events. */
            } catch (MaxEventsException e) {
                logger.log(Level.INFO, "Reached max events <{0}>.", this.maxEvents);
                this.status = ConnectionStatus.DISCONNECTING;
            /* 
             * Catch other types of errors.  These are most likely ET system exceptions
             * or event processing errors.
             */
            } catch (Exception e) {
                logger.log(Level.SEVERE, e.getMessage());
                e.printStackTrace();
                this.status = ConnectionStatus.ERROR;
            /* Break out of processing loop if not still in connected state. */
            } finally {
                if (this.status != ConnectionStatus.CONNECTED) {
                    break;
                }
            }
            /* Force messages to show in the log table. */
            logger.getHandlers()[0].flush();
        }

        logger.log(Level.FINEST, "End of processing loop.  About to cleanup.");
        logger.getHandlers()[0].flush();

        // Notify listeners of job end.
        finish();

        done = true;
    }

    /**
     * Check if this is a Pre Start event.
     * @param event
     * @return True if event is of type Pre Start; false if not.
     */
    private static boolean isPreStartEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.PRESTART_EVENT_TAG;
    }

    /**
     * Check if this is an End Event.
     * @param event
     * @return True if event is of type End Event; false if not.
     */
    private static boolean isEndEvent(EvioEvent event) {
        return event.getHeader().getTag() == EventConstants.END_EVENT_TAG;
    }

    /**
     * Notify listeners of Pre Start event being received by the ET system.
     * @param event The EvioEvent for the Pre Start.
     */
    private void preStartEvent(EvioEvent event) {
        int[] data = event.getIntData();
        int seconds = data[0];
        int runNumber = data[1];
        for (EtEventListener listener : this.listeners) {
            listener.prestart(seconds, runNumber);
        }
    }

    /**
     * Notify listeners of end of run being received by ET system.
     * @param event The EvioEvent for the End Event record, which indicates end of run.
     */
    private void endRun(EvioEvent event) {
        int[] data = event.getIntData();
        int seconds = data[0];
        int nevents = data[2];
        for (EtEventListener listener : this.listeners) {
            listener.endRun(seconds, nevents);
        }
    }

    /**
     * Set pause mode.
     * @param p Set the pause mode; true to pause; false to unpause.
     */
    public void pauseMode(boolean p) {
        pauseMode = p;
    }

    /**
     * This is called externally to get the next event if running in pause mode.
     */
    public void nextEvents() {
        nextEvents = true;
    }

    /**
     * Set the application's log level.
     * @param level The log level.
     */
    public void setLogLevel(Level level) {
        logger.setLevel(level);
    }

    /**
     * Get whether the event processing is blocked.
     * @return True if blocked; false if not.
     */
    public boolean blocked() {
        return blocked;
    }

    /**
     * Get whether the event processing has been completed.
     * @return True if event processing is complete; false if not.
     */
    public boolean done() {
        return done;
    }
 
    /**
     * Print information from non-physics event.  
     * Copied and modified from {@link org.lcsim.hps.evio.LCSimTestRunEventBuilder#readEvioEvent(EvioEvent)} 
     * in order to route messages to the log table instead of <code>System.out</code>.
     * @param evioEvent The EVIO event.
     */
    private void logEvioEvent(EvioEvent evioEvent) {
        if (EventConstants.isSyncEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            logger.log(Level.INFO, "Sync event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count since last sync " + data[1] + ", event count so far " + data[2] + ", status " + data[3]);
        } else if (EventConstants.isPreStartEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            logger.log(Level.INFO, "Prestart event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", run type " + data[2]);
        } else if (EventConstants.isGoEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            logger.log(Level.INFO, "Go event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EventConstants.isPauseEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            logger.log(Level.INFO, "Pause event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count so far " + data[2]);
        } else if (EventConstants.isEndEvent(evioEvent)) {
            int[] data = evioEvent.getIntData();
            int seconds = data[0];
            logger.log(Level.INFO, "End event: time " + seconds + " - " + new Date(((long) seconds) * 1000) + ", event count " + data[2]);
        }
    }
}