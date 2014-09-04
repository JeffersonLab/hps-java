package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;

/**
 * A record source providing <code>CompositeRecord</code> objects.
 */
public final class CompositeRecordSource extends AbstractRecordSource {

    CompositeRecord currentRecord;
    int sequenceNumber = 1;
            
    public void next() throws IOException, NoSuchRecordException {
        //System.out.println("CompositeSource.next");
        //System.out.println("  record #" + sequenceNumber);
        //System.out.flush();
        currentRecord = new CompositeRecord();
        currentRecord.setSequenceNumber(sequenceNumber);
        ++sequenceNumber;
    }
            
    @Override
    public Object getCurrentRecord() throws IOException {
        return currentRecord;
    }
    
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    @Override
    public boolean supportsNext() {
        return true;
    }
  
    @Override
    public boolean supportsPrevious() {
        return false;
    }
  
    @Override
    public boolean supportsIndex() {
        return false;
    }
  
    @Override 
    public boolean supportsShift() {
        return false;
    }
  
    @Override
    public boolean supportsRewind() {
        return false;
    }

    @Override
    public boolean hasCurrent() {
        return currentRecord != null;
    }

    @Override
    public boolean hasNext() {
        // FIXME: Not sure about this one.
        return true;
    }
}
