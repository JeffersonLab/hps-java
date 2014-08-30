package org.hps.record.lcio;

import org.hps.record.ErrorState;
import org.hps.record.HasErrorState;
import org.lcsim.util.loop.LCSimLoop;

/**
 * This class overrides the error handling of <code>LCSimLoop</code>
 * so it does not exit the application when errors occur.  It also 
 * adds basic error handling so the caller can determine if an error
 * occurred without exceptions being thrown.
 */
public final class LcioLoop extends LCSimLoop implements HasErrorState {

    ErrorState errorState = new ErrorState();
    
    public ErrorState getErrorState() {
        return errorState;
    }
    
    /**
     * Handle errors from the Drivers. 
     */
    protected void handleClientError(Throwable x) {
        getErrorState().setLastError((Exception) x);
        getErrorState().print();
    }

    /**
     * Handle errors from the RecordSource.
     */
    protected void handleSourceError(Throwable x) {
        getErrorState().setLastError((Exception) x);
        getErrorState().print();
    }
}
