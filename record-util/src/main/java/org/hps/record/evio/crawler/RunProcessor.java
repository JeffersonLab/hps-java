package org.hps.record.evio.crawler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.hps.record.evio.EvioEventProcessor;
import org.jlab.coda.jevio.EvioEvent;
import org.jlab.coda.jevio.EvioException;
import org.jlab.coda.jevio.EvioReader;
import org.lcsim.util.log.LogUtil;

public final class RunProcessor {

    private static final Logger LOGGER = LogUtil.create(RunProcessor.class);

    List<EvioEventProcessor> processors = new ArrayList<EvioEventProcessor>();

    RunSummary runSummary;

    RunProcessor(final RunSummary runSummary) {
        this.runSummary = runSummary;
    }

    void addProcessor(final EvioEventProcessor processor) {
        this.processors.add(processor);
        LOGGER.config("added processor: " + processor.getClass().getSimpleName());
    }

    List<EvioEventProcessor> getProcessors() {
        return this.processors;
    }

    void process() throws Exception {
        if (this.processors.isEmpty()) {
            throw new RuntimeException("The processors list is empty.");
        }
        for (final EvioEventProcessor processor : this.processors) {
            processor.startJob();
        }
        for (final File file : this.runSummary.getFiles()) {
            process(file);
        }
        for (final EvioEventProcessor processor : this.processors) {
            processor.endJob();
        }
    }

    private void process(final File file) throws EvioException, IOException, Exception {
        EvioReader reader = null;
        try {
            reader = EvioFileUtilities.open(file);
            this.runSummary.getFiles().computeEventCount(file);
            EvioEvent event = null;
            while ((event = reader.parseNextEvent()) != null) {
                for (final EvioEventProcessor processor : this.processors) {
                    processor.process(event);
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }
}
