package org.hps.monitoring.application;

import org.hps.job.JobManager;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.composite.CompositeLoop;
import org.hps.record.composite.EventProcessingThread;
import org.hps.record.et.EtConnection;

/**
 * This is the session state for job processing.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class SessionState {
    JobManager jobManager;
    LCSimEventBuilder eventBuilder;
    CompositeLoop loop;
    EventProcessingThread processingThread;
    Thread sessionWatchdogThread;
    EtConnection connection;
}
