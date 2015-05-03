package org.hps.monitoring.trigger;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;

import java.awt.GridLayout;

import javax.swing.JPanel;
/**
 * Class <code>ShifterTrigWindow</code> displays basic efficiency data
 * for clustering and each of the four data triggers for both the entire
 * run and for a local time window. The data displays will change color
 * to either yellow or red if the efficiencies drop too low.
 */
public class ShifterTrigWindow extends JPanel implements DiagnosticUpdatable {
	private static final long serialVersionUID = 1L;
	private ShifterTrigPanel localPanel = new ShifterTrigPanel("Instantaneous");
	private ShifterTrigPanel globalPanel = new ShifterTrigPanel("Run-Integrated");
	
	/**
	 * Instantiates a new panel for displaying basic information
	 * pertaining to trigger diagnostics.
	 */
	public ShifterTrigWindow() {
		setLayout(new GridLayout(1, 2));
		add(localPanel);
		add(globalPanel);
		updatePanel(null, null);
	}
	
	@Override
	public void updatePanel(DiagnosticSnapshot runSnapshot, DiagnosticSnapshot localSnapshot) {
		// Update each panel with the appropriate snapshot.
		localPanel.updatePanel(localSnapshot);
		globalPanel.updatePanel(runSnapshot);
	}
}
