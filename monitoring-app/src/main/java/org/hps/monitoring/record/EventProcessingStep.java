package org.hps.monitoring.record;

import java.io.IOException;

import org.freehep.record.source.NoSuchRecordException;

/**
 * Interface for a single processing step which handles one type of record.
 */
public interface EventProcessingStep {    
    void execute() throws IOException, NoSuchRecordException;    
}