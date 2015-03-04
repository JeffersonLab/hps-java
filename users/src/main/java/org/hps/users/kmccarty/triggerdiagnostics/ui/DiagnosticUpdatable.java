package org.hps.users.kmccarty.triggerdiagnostics.ui;

import org.hps.users.kmccarty.triggerdiagnostics.DiagSnapshot;

/**
 * Interface <code>DiagnosticUpdatable</code> defines a class of objects
 * that can be updated with information from a trigger diagnostic driver.
 * They can take snapshots of the driver results and use this in order to
 * alter their displayed or constituent values.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see DiagSnapshot
 */
public interface DiagnosticUpdatable {
	/**
	 * Updates the object with information from the trigger diagnostic
	 * snapshot in the argument.
	 * @param snapshot - The snapshot containing information with which
	 * to update the object.
	 */
	public void updatePanel(DiagSnapshot snapshot);
}
