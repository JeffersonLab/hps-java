package org.hps.monitoring.record.lcio;

import org.lcsim.util.loop.LCSimLoop;

/**
 * This class overrides the error handling of <code>LCSimLoop</code>
 * so it does not exit the application when errors occur.
 */
public final class LcioLoop extends LCSimLoop {

    protected void handleClientError(Throwable x) {
        if (x != null) {
            throw new RuntimeException(x);
        }
    }

    protected void handleSourceError(Throwable x) {
        if (x != null) {
            throw new RuntimeException(x);
        }
    }
}
