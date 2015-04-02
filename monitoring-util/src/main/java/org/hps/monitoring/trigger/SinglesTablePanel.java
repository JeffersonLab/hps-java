package org.hps.monitoring.trigger;

/**
 * Class <code>SinglesTablePanel</code> displays statistical information
 * for trigger singles cuts.
 * 
 * @author Kyle McCarty
 */
public class SinglesTablePanel extends AbstractTriggerTablePanel {
	// Static variables.
	private static final long serialVersionUID = 0L;
	private static final String[] CUT_NAMES = { "    Cluster Energy (Low):",
		"    Cluster Energy (High):", "    Hit Count:"  };
	
	/**
	 * Instantiates a <code>SinglesTablePanel</code>.
	 */
	public SinglesTablePanel() { super(CUT_NAMES, true); }
	
}
