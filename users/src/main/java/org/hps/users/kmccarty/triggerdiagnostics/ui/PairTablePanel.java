package org.hps.users.kmccarty.triggerdiagnostics.ui;

import org.hps.users.kmccarty.triggerdiagnostics.DiagSnapshot;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerStatModule;

/**
 * Class <code>PairTablePanel</code> displays statistical information
 * for trigger pair cuts.
 * 
 * @author Kyle McCarty
 */
public class PairTablePanel extends AbstractTriggerTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	private static final String[] CUT_NAMES = { "        Energy Sum:",
		"        Energy Difference:", "        Energy Slope:", "        Coplanarity:" };
	
	/**
	 * Instantiates a <code>PairTablePanel</code>.
	 */
	public PairTablePanel() { super(CUT_NAMES); }

	@Override
	protected TriggerStatModule getLocalModule(DiagSnapshot snapshot) {
		return snapshot.pairLocalStatistics;
	}

	@Override
	protected TriggerStatModule getRunModule(DiagSnapshot snapshot) {
		return snapshot.pairRunStatistics;
	}
	
}
