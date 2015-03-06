package org.hps.monitoring.application;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * The modal dialog for entering settings. It contains a <code>JPanel</code> with the different
 * settings sub-tabs.
 */
class SettingsDialog extends JDialog {

    final SettingsPanel settingsPanel;

    public SettingsDialog(ConfigurationModel configurationModel, ActionListener listener) {

        // Initialize the GUI panel.
        settingsPanel = new SettingsPanel(this, configurationModel, listener);
        
        // Configure the frame.
        setTitle("Settings");
        setContentPane(settingsPanel);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();

        // Add window listener for turning invisible when closing.
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });        
    }
}
