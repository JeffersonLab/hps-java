package org.hps.monitoring.trigger;

import java.awt.GridLayout;

import javax.swing.JPanel;

public class ShifterTrigWindow extends JPanel {
	private static final long serialVersionUID = 1L;
	
	public ShifterTrigWindow() {
		setLayout(new GridLayout(1, 2));
		add(new ShifterTrigPanel("Instantaneous"));
		add(new ShifterTrigPanel("Run-Integrated"));
	}
}
