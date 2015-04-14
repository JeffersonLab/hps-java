package org.hps.record.et;

import java.io.IOException;
import java.util.Arrays;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

import org.freehep.record.source.AbstractRecordSource;
import org.freehep.record.source.NoSuchRecordException;
import org.jlab.coda.et.EtEvent;

/**
 * Implementation of a record source supplying <tt>EtEvent</tt> objects from an ET server connection to a record loop.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class EtEventSource extends AbstractRecordSource {

    /**
     * Indicates ET system error occurred.
     */
    @SuppressWarnings("serial")
    public static class EtSourceException extends IOException {

        /**
         * Class constructor.
         *
         * @param message the error message
         * @param cause the cause of the error
         */
        public EtSourceException(final String message, final Exception cause) {
            super(message, cause);
        }
    }

    /**
     * The ET connection information.
     */
    private final EtConnection connection;

    /**
     * The current ET record.
     */
    private EtEvent currentRecord;

    /**
     * The ET event queue.
     */
    private final Queue<EtEvent> eventQueue = new LinkedBlockingQueue<EtEvent>();

    /**
     * Constructor that requires the connection parameters.
     *
     * @param connection the <code>EtConnection</code> which should have a valid set of ET connection parameters
     */
    public EtEventSource(final EtConnection connection) {
        this.connection = connection;
    }

    /**
     * Get the current record.
     *
     * @return the current record
     */
    @Override
    public Object getCurrentRecord() throws IOException {
        return this.currentRecord;
    }

    /**
     * Return <code>true</code> if the current record is not <code>null</code>
     *
     * @return <code>true</code> if the current record is not <code>null</code>
     */
    @Override
    public boolean hasCurrent() {
        return this.currentRecord != null;
    }

    /**
     * Load the next <code>EtEvent</code>.
     * <p>
     * A cached record will be read from the queue or more records will be fetched from the ET server if the queue is
     * empty.
     *
     * @throws NoSuchRecordException if the queue is empty and getting more records from the ET server fails
     */
    @Override
    public void next() throws IOException, NoSuchRecordException {

        // Fill the queue if there are no events cached.
        if (this.eventQueue.size() == 0) {
            readEtEvents();
        }

        // Poll the queue.
        this.currentRecord = this.eventQueue.poll();

        if (this.currentRecord == null) {
            throw new NoSuchRecordException("ET record queue is empty.");
        }
    }

    /**
     * Read the next <code>EtEvent</code> array from the ET server.
     *
     * @throws IOException if reading the events fails
     */
    private void readEtEvents() throws IOException {
        try {
            final EtEvent[] mevs = this.connection.readEtEvents();
            this.eventQueue.addAll(Arrays.asList(mevs));
        } catch (final Exception e) {
            throw new EtSourceException("Error while reading ET events.", e);
        }
    }

    /**
     * Get the number of records, which is the size of the current queue.
     *
     * @return the size of the queue
     */
    @Override
    public long size() {
        return this.eventQueue.size();
    }

    /**
     * Return <code>true</code> because this source supports the <code>next</code> method
     *
     * @return <code>true</code> because this source supports the <code>next</code> method
     */
    @Override
    public boolean supportsNext() {
        return true;
    }
}