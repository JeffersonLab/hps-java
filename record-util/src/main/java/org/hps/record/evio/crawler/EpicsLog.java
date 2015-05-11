package org.hps.record.evio.crawler;

import java.util.HashMap;
import java.util.Map;

import org.hps.record.epics.EpicsEvioProcessor;
import org.hps.record.epics.EpicsScalarData;
import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;

public final class EpicsLog extends EvioEventProcessor {

    private final Map<String, Integer> counts = new HashMap<String, Integer>();

    private EpicsScalarData currentData;

    private final EpicsScalarData logData = new EpicsScalarData();
    private final EpicsEvioProcessor processor = new EpicsEvioProcessor();

    private final RunSummary runSummary;

    EpicsLog(final RunSummary runSummary) {
        this.runSummary = runSummary;
    }

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

    @Override
    public void process(final EvioEvent evioEvent) {
        this.processor.process(evioEvent);
        this.currentData = this.processor.getEpicsScalarData();
        update();
    }

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
                System.out.println(name + " => added " + this.currentData.getValue(name) + "; total = " + value
                        + "; mean = " + value / count);
            }
        }
    }
}
