package org.hps.record.epics;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Creates a list of EPICS data found in EVIO events across an entire job.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class EpicsRunProcessor extends EvioEventProcessor {

    /**
     * The current EPICS data block from the EVIO events (last one that was found).
     */
    private EpicsData currentEpicsData;

    /**
     * Collection of the EPICS data accumulated during the job.
     * <p>
     * A set is used here to avoid adding duplicate objects.
     */
    private Set<EpicsData> epicsDataSet;

    /**
     * The processor for extracting the EPICS information from EVIO events.
     */
    private final EpicsEvioProcessor processor = new EpicsEvioProcessor();

    /**
     * Create an EPICs log.
     */
    public EpicsRunProcessor() {
    }

    /**
     * Get the EPICS data from the job.
     *
     * @return the EPICS data from the job
     */
    public List<EpicsData> getEpicsData() {
        return new ArrayList<EpicsData>(this.epicsDataSet);
    }

    /**
     * Process a single EVIO event, setting the current EPICS data and updating the variable counts.
     */
    @Override
    public void process(final EvioEvent evioEvent) {

        // Call the processor that will load EPICS data if it exists in the event.
        this.processor.process(evioEvent);
        this.currentEpicsData = this.processor.getEpicsData();

        // Add EPICS data to the collection.
        if (this.currentEpicsData != null) {
            this.epicsDataSet.add(this.currentEpicsData);
        }
    }

    /**
     * Start of job hook (reset the list of EPICS data).
     */
    @Override
    public void startJob() {
        epicsDataSet = new HashSet<EpicsData>();
    }
}
