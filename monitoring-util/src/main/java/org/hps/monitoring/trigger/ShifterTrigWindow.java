package org.hps.monitoring.trigger;

import org.hps.analysis.trigger.data.DiagnosticSnapshot;

import java.awt.GridLayout;

import javax.swing.JPanel;

public class ShifterTrigWindow extends JPanel implements DiagnosticUpdatable {
	private static final long serialVersionUID = 1L;
	private ShifterTrigPanel localPanel = new ShifterTrigPanel("Instantaneous");
	private ShifterTrigPanel globalPanel = new ShifterTrigPanel("Run-Integrated");
	
	public ShifterTrigWindow() {
		setLayout(new GridLayout(1, 2));
		add(localPanel);
		add(globalPanel);
	}
	
	@Override
	public void updatePanel(DiagnosticSnapshot runSnapshot, DiagnosticSnapshot localSnapshot) {
		// Update each panel with the appropriate snapshot.
		localPanel.updatePanel(localSnapshot);
		globalPanel.updatePanel(runSnapshot);
	}
}
