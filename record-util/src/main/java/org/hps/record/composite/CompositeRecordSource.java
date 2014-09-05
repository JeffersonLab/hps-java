package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * A record source providing <code>CompositeRecord</code> objects
 * that can be accessed and/or modified by <code>RecordListener</code>
 * objects on the loop.  This is essentially a minimal implementation
 * that does not support advanced operations like rewind or index.
 */
public final class CompositeRecordSource extends AbstractRecordSource {

    CompositeRecord currentRecord;
    int sequenceNumber = 1;
            
    /**
     * Load the next record which is then accessible using {@link #getCurrentRecord()}.
     */
    public void next() throws IOException, NoSuchRecordException {
        currentRecord = new CompositeRecord();
        currentRecord.setSequenceNumber(sequenceNumber);
        ++sequenceNumber;
    }
            
    /**
     * Get the current {@link CompositeRecord}.
     * @return The current CompositeRecord.
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentRecord;
    }
    
    /**
     * Get whether this source supports the next command (true).
     * @return Whether this source supports the next command.
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
      
    /**
     * Get whether this source has a current record.
     * @return Whether this source has a current record.
     */
    @Override
    public boolean hasCurrent() {
        return currentRecord != null;
    }
}
