package org.hps.record.enums;

/**
 * When set this can be used to limit the number of processing stages that are executed by the
 * {@link org.hps.record.composite.CompositeLoop}.
 * <p>
 * For example, if the <code>ProcessingStage</code> is set to <code>EVIO</code> then the <code>ET</code> and
 * <code>EVIO</code> adapters will be activated but LCIO events will not be created or processed.
 */
public enum ProcessingStage {
    /**
     * Execute only the reading of ET events from the server.
     */
    ET,
    /**
     * Execute reading of ET events and conversion to EVIO.
     */
    EVIO,
    /**
     * Execute full processing chain of ET to EVIO to LCIO.
     */
    LCIO
}