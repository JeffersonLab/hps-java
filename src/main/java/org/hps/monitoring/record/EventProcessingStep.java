package org.hps.monitoring.record;

import java.io.IOException;

import org.freehep.record.source.NoSuchRecordException;

/**
 * Interface for a single processing step which handles one type of record.
 */
// FIXME: This could be done by registering record listener's on CompositeLoop
// which receive and alter the current CompositeRecord.
public interface EventProcessingStep {    
    void execute() throws IOException, NoSuchRecordException;    
}