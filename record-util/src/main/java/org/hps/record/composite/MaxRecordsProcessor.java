package org.hps.record.composite;

import org.hps.record.MaxRecordsException;
import org.hps.record.evio.EvioEventUtilities;

/**
 * A @{link CompositeProcessor} for throwing an error when the maximum number of records is reached or exceeded.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class MaxRecordsProcessor extends CompositeRecordProcessor {

    /**
     * The maximum number of records.
     */
    private final long maxRecords;

    /**
     * The number of records received.
     */
    private long recordsReceived;

    /**
     * Class constructor, with the maximum number of records.
     *
     * @param maxRecords the maximum number of records
     */
    public MaxRecordsProcessor(final long maxRecords) {
        this.maxRecords = maxRecords;
    }

    /**
     * Process a record and check if max number of records was reached.
     * <p>
     * Only records with certain types are considered in this total, which are basically "physics" events when
     * processing LCIO or EVIO files. For an ET system without any other record processing attached, all events count
     * towards the total as it is not easy to tell generically which are physics data events.
     *
     * @param record the composite record to process
     */
    @Override
    public void process(final CompositeRecord record) {
        if (record.getLcioEvent() != null) {
            // All LCSim events count as records.
            ++this.recordsReceived;
        } else if (record.getEvioEvent() != null) {
            if (EvioEventUtilities.isPhysicsEvent(record.getEvioEvent())) {
                // Only EVIO physics events are counted.
                ++this.recordsReceived;
            }
        } else {
            // Otherwise (ET only?) count all records.
            ++this.recordsReceived;
        }
        if (this.recordsReceived >= this.maxRecords) {
            // Throw exception if max records was reached or exceeded.
            throw new MaxRecordsException("Maximum number of records received.", this.maxRecords);
        }
    }
}
