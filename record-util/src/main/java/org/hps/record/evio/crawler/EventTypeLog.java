package org.hps.record.evio.crawler;

import java.util.HashMap;
import java.util.Map;

import org.hps.record.evio.EventTagBitMask;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

/**
 * This class makes a log of the number of different event types found in a run by their tag value.
 *
 * @author Jeremy McCormick
 */
final class EventTypeLog extends EvioEventProcessor {

    /**
     * The event tag counts for the run.
     */
    private final Map<Object, Integer> eventTypeCounts = new HashMap<Object, Integer>();

    /**
     * The run summary to update.
     */
    private final RunSummary runSummary;

    /**
     * Create the log pointing to a run summary.
     *
     * @param runSummary the run summary
     */
    EventTypeLog(final RunSummary runSummary) {
        this.runSummary = runSummary;
        for (final EventTagConstant constant : EventTagConstant.values()) {
            this.eventTypeCounts.put(constant, 0);
        }
        for (final EventTagBitMask mask : EventTagBitMask.values()) {
            this.eventTypeCounts.put(mask, 0);
        }
    }

    /**
     * End of job hook which sets the event type counts on the run summary.
     */
    @Override
    public void endJob() {
        this.runSummary.setEventTypeCounts(this.eventTypeCounts);
    }

    /**
     * Get the counts of different event types (physics events, PRESTART, etc.).
     *
     * @return a map of event types to their counts
     */
    Map<Object, Integer> getEventTypeCounts() {
        return this.eventTypeCounts;
    }

    /**
     * Process an EVIO event and add its type to the map.
     *
     * @param event the EVIO event
     */
    @Override
    public void process(final EvioEvent event) {
        for (final EventTagConstant constant : EventTagConstant.values()) {
            if (constant.isEventTag(event)) {
                final int count = this.eventTypeCounts.get(constant) + 1;
                this.eventTypeCounts.put(constant, count);
            }
        }
        for (final EventTagBitMask mask : EventTagBitMask.values()) {
            if (mask.isEventTag(event)) {
                final int count = this.eventTypeCounts.get(mask) + 1;
                this.eventTypeCounts.put(mask, count);
            }
        }
    }
}
