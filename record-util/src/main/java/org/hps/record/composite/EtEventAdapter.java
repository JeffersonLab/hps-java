package org.hps.record.composite;

import java.io.IOException;

import org.freehep.record.loop.RecordEvent;
import org.freehep.record.source.NoSuchRecordException;
import org.hps.record.RecordProcessingException;
import org.hps.record.et.EtEventSource;
import org.jlab.coda.et.EtEvent;

/**
 * An adapter for directly using the CompositeLoop to supply and process EtEvents.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class EtEventAdapter extends RecordProcessorAdapter<EtEvent> {

    /**
     * The ET event source.
     */
    private final EtEventSource source;

    /**
     * Constructor with an {@link org.hps.record.et.EtEventSource} that supplies <code>EtEvent</code> records through a
     * network ET server.
     *
     * @param source the event source
     */
    public EtEventAdapter(final EtEventSource source) {
        this.source = source;
    }

    /**
     * Process one record which will get the next <code>EtEvent</code> from the source and set a reference to it on the
     * {@link CompositeRecord}.
     *
     * @param record the current composite record
     */
    @Override
    public void recordSupplied(final RecordEvent record) {
        final CompositeRecord compositeRecord = (CompositeRecord) record.getRecord();
        try {
            this.source.next();
            if (this.source.getCurrentRecord() != null) {
                compositeRecord.setEtEvent((EtEvent) this.source.getCurrentRecord());
            } else {
                throw new NoSuchRecordException("No current ET record available from source.");
            }
            process(compositeRecord.getEtEvent());
        } catch (IOException | NoSuchRecordException e) {
            throw new RecordProcessingException("Error processing ET record.", e);
        }
    }
}
