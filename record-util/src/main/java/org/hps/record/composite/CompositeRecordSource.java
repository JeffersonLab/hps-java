package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * A record source providing <code>CompositeRecord</code> objects that can be accessed and/or modified by
 * <code>RecordListener</code> objects on the loop. This is essentially a minimal implementation that does not support
 * advanced operations like rewind or index.
 */
public final class CompositeRecordSource extends AbstractRecordSource {

    /**
     * The current record.
     */
    CompositeRecord currentRecord;

    /**
     * The sequence number.
     */
    int sequenceNumber = 1;

    /**
     * Get the current {@link CompositeRecord}.
     *
     * @return the current CompositeRecord
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return this.currentRecord;
    }

    /**
     * Return <code>true</code> if source has a current record.
     *
     * @return <code>true</code> if this source has a current record
     */
    @Override
    public boolean hasCurrent() {
        return this.currentRecord != null;
    }

    /**
     * Load the next record which is then accessible using {@link #getCurrentRecord()}.
     */
    @Override
    public void next() throws IOException, NoSuchRecordException {
        this.currentRecord = new CompositeRecord();
        this.currentRecord.setSequenceNumber(this.sequenceNumber);
        ++this.sequenceNumber;
    }

    /**
     * Return <code>true</code> to indicate next record capability is supported.
     *
     * @return <code>true</code> to indicate next record capability is supported
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
}
