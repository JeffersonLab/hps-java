package org.hps.monitoring.trigger;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;

/**
 * Interface <code>DiagnosticUpdatable</code> defines a class of objects
 * that can be updated with information from a trigger diagnostic driver.
 * They can take snapshots of the driver results and use this in order to
 * alter their displayed or constituent values.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.analysis.trigger.data.DiagnosticSnapshot
 */
public interface DiagnosticUpdatable {
    /**
     * Updates the object with information from the trigger diagnostic
     * snapshot in the argument.
     * @param runSnapshot the accumulated snapshot
     * @param localSnapshot The snapshot containing information with which
     * to update the object. 
     */
    public void updatePanel(DiagnosticSnapshot runSnapshot, DiagnosticSnapshot localSnapshot);
}
