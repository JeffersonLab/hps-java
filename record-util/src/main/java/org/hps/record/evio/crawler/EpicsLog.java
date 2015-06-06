package org.hps.record.evio.crawler;

import java.util.HashMap;
import java.util.Map;

import org.hps.record.epics.EpicsData;
import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * Create a summary log of EPICS information found in EVIO events.
 *
 * @author Jeremy McCormick, SLAC
 */
final class EpicsLog extends EvioEventProcessor {

    /**
     * A count of how many times a given EPICS variable is found in the input, e.g. for computing the mean value across the run.
     */
    private final Map<String, Integer> counts = new HashMap<String, Integer>();

    /**
     * The current EPICS data block from the EVIO events (last one that was found).
     */
    private EpicsData currentData;

    /**
     * The summary information for the variables from computing the mean across the whole run.
     */
    private final EpicsData logData = new EpicsData();

    /**
     * The processor for extracting the EPICS information from EVIO events.
     */
    private final EpicsEvioProcessor processor = new EpicsEvioProcessor();

    /**
     * Reference to the run summary which will contain the EPICs information.
     */
    private final RunSummary runSummary;

    /**
     * Create an EPICs log pointing to a run summary.
     * 
     * @param runSummary the run summary
     */
    EpicsLog(final RunSummary runSummary) {
        this.runSummary = runSummary;
    }

    /**
     * End of job hook which computes the mean values for all EPICS variables found in the run.
     */
    @Override
    public void endJob() {
        System.out.println(this.logData);

        // Compute means for all EPICS variables.
        for (final String name : this.logData.getUsedNames()) {
            final double total = this.logData.getValue(name);
            final double mean = total / this.counts.get(name);
            this.logData.setValue(name, mean);
        }

        // Set the EPICS data on the run summary.
        this.runSummary.setEpicsData(this.logData);
    }

    /**
     * Process a single EVIO event, setting the current EPICS data and updating the variable counts.
     */
    @Override
    public void process(final EvioEvent evioEvent) {
        this.processor.process(evioEvent);
        this.currentData = this.processor.getEpicsData();
        this.update();
    }

    /**
     * Update state from the current EPICS data.
     * <p>
     * If the current data is null, this method does nothing.
     */
    private void update() {
        if (this.currentData != null) {
            for (final String name : this.currentData.getUsedNames()) {
                if (!this.logData.getUsedNames().contains(name)) {
                    this.logData.setValue(name, 0.);
                }
                if (!this.counts.keySet().contains(name)) {
                    this.counts.put(name, 0);
                }
                int count = this.counts.get(name);
                count += 1;
                this.counts.put(name, count);
                final double value = this.logData.getValue(name) + this.currentData.getValue(name);
                this.logData.setValue(name, value);
                //System.out.println(name + " => added " + this.currentData.getValue(name) + "; total = " + value + "; mean = " + value / count);
            }
        }
    }
}
