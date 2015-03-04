package org.hps.users.kmccarty.triggerdiagnostics.ui;

import org.hps.users.kmccarty.triggerdiagnostics.DiagSnapshot;
import org.hps.users.kmccarty.triggerdiagnostics.event.TriggerStatModule;

/**
 * Class <code>SinglesTablePanel</code> displays statistical information
 * for trigger singles cuts.
 * 
 * @author Kyle McCarty
 */
public class SinglesTablePanel extends AbstractTriggerTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	private static final String[] CUT_NAMES = { "        Cluster Energy (Low):",
		"        Cluster Energy (High):", "        Hit Count:"  };
	
	/**
	 * Instantiates a <code>SinglesTablePanel</code>.
	 */
	public SinglesTablePanel() { super(CUT_NAMES); }
	
	@Override
	protected TriggerStatModule getLocalModule(DiagSnapshot snapshot) {
		return snapshot.singlesLocalStatistics;
	}
	
	@Override
	protected TriggerStatModule getRunModule(DiagSnapshot snapshot) {
		return snapshot.singlesRunStatistics;
	}
	
}
