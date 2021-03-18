package org.hps.record.lcio;

import org.hps.record.AbstractRecordQueue;
import org.lcsim.event.EventHeader;

/**
 * A record queue for LCIO/LCSim events.
 */
public class LcioEventQueue extends AbstractRecordQueue<EventHeader> {

    /**
     * Class constructor.
     *
     * @param timeoutMillis the queue timeout in milliseconds
     * @param maxQueueSize the maximum queue size
     */
    public LcioEventQueue(final long timeoutMillis, final int maxQueueSize) {
        super(timeoutMillis, maxQueueSize);
    }

    /**
     * Get the record class.
     *
     * @return the record class
     */
    @Override
    public Class<?> getRecordClass() {
        return EventHeader.class;
    }

    /**
     * Return <code>true</code> if there is a current record.
     *
     * @return <code>true</code> if there is a current record
     */
    @Override
    public boolean hasCurrent() {
        return this.size() != 0L;
    }

    /**
     * Return <code>true</code> to indicate there is a next record.
     *
     * @return <code>true</code> to indicate there is a next record
     */
    // FIXME: Should this actually check if the queue has more records?
    @Override
    public boolean hasNext() {
        return true;
    }

    /**
     * Return <code>true</code> to indicate current record capability is supported.
     *
     * @return <code>true</code> to indicate current record capability is supported
     */
    @Override
    public boolean supportsCurrent() {
        return true;
    }

    /**
     * Return <code>false</code> to indicate indexing is not supported.
     *
     * @return <code>false</code> to indicate indexing is not supported.
     */
    @Override
    public boolean supportsIndex() {
        return false;
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

    /**
     * Return <code>false</code> to indicate previous record capability is not supported.
     *
     * @return <code>false</code> to indicate previous record capability is not supported
     */
    @Override
    public boolean supportsPrevious() {
        return false;
    }

    /**
     * Return <code>false</code> to indicate rewind is not supported.
     *
     * @return <code>false</code> to indicate rewind is not supported
     */
    @Override
    public boolean supportsRewind() {
        return false;
    }

    /**
     * Return <code>false</code> to indicate shift operation is not supported.
     *
     * @return <code>false</code> to indicate shift operation is not supported
     */
    @Override
    public boolean supportsShift() {
        return false;
    }
}
