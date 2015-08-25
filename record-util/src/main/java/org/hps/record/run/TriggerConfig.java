package org.hps.record.run;

/**
 * Trigger information available in the run database.
 * <p>
 * Currently this only has the TI time offset.
 * 
 * @author Jeremy McCormick, SLAC
 */
public final class TriggerConfig {
    
    /**
     * The TI time offset.
     */
    private long tiTimeOffset;
    
    /**
     * Set the TI time offset.
     * 
     * @param tiTimeOffset the TI time offset
     */
    void setTiTimeOffset(long tiTimeOffset) {
        this.tiTimeOffset = tiTimeOffset;
    }
    
    /**
     * Get the TI time offset.
     * 
     * @return the TI time offset
     */
    public long getTiTimeOffset() {
        return tiTimeOffset;
    }
}
