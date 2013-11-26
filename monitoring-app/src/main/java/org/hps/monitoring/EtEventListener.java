package org.hps.monitoring;

/**
 * Interface for notifying listeners of ET ring events.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: EtEventListener.java,v 1.1 2012/05/03 16:59:28 jeremy Exp $
 */
// FIXME: Should all the callback methods get an EtEvent or event number?
interface EtEventListener {

    /**
     * Called at beginning of event processing session.
     */
    void begin();

    /**
     * Called at start of single EtEvent.
     */
    void startOfEvent();
    
    /**
     * Called at end of processing single EtEvent.
     */
    void endOfEvent();

    /**
     * Called when an error occurs processing current EtEvent.
     */
    void errorOnEvent();

    /**
     * Called when event processing session finishes.
     */
    void finish();
    
    /** 
     * Called when a Pre Start event is received from the ET ring,
     * indicating start of run.
     * @param seconds Unix time in seconds.
     * @param runNumber The run number.
     */
    void prestart(int seconds, int runNumber);
    
    /**
     * Called when an End Event is received from the ET ring,
     * indicating end of run.
     * @param seconds Unix time in seconds.
     * @param nevents Number of events in run.
     */
    void endRun(int seconds, int nevents);
}