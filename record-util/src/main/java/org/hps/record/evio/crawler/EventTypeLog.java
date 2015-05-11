package org.hps.record.evio.crawler;

import java.util.HashMap;
import java.util.Map;

import org.hps.record.evio.EventTagBitMask;
import org.hps.record.evio.EventTagConstant;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

public class EventTypeLog extends EvioEventProcessor {

    Map<Object, Integer> eventTypeCounts = new HashMap<Object, Integer>();
    RunSummary runSummary;

    EventTypeLog(final RunSummary runSummary) {
        this.runSummary = runSummary;
        for (final EventTagConstant constant : EventTagConstant.values()) {
            this.eventTypeCounts.put(constant, 0);
        }
        for (final EventTagBitMask mask : EventTagBitMask.values()) {
            this.eventTypeCounts.put(mask, 0);
        }
    }

    @Override
    public void endJob() {
        this.runSummary.setEventTypeCounts(this.eventTypeCounts);
    }

    Map<Object, Integer> getEventTypeCounts() {
        return this.eventTypeCounts;
    }

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
