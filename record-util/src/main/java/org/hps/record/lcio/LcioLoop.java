package org.hps.record.lcio;

import org.lcsim.util.loop.LCSimLoop;

/**
 * This class overrides the error handling of <code>LCSimLoop</code>
 * so it does not exit the application when errors occur.
 */
public final class LcioLoop extends LCSimLoop {

    protected void handleClientError(Throwable x) {
        System.out.println("LcioLoop.handleClientError");
        System.out.println("  initial loop state: " + this.getState().toString());
        if (x != null) {            
            this.execute(Command.STOP);
            System.out.println("  loop state after stop: " + this.getState().toString());
            throw new RuntimeException(x);
        }
    }

    protected void handleSourceError(Throwable x) {
        System.out.println("LcioLoop.handleSourceError");
        System.out.println("  initial loop state: " + this.getState().toString());
        if (x != null) {
            this.execute(Command.STOP);
            System.out.println("  loop state after stop: " + this.getState().toString());
            throw new RuntimeException(x);
        }
    }
}
