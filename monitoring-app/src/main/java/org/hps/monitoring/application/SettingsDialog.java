package org.hps.monitoring.application;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * The modal dialog for entering settings. It contains a <code>JPanel</code> with the different settings sub-tabs.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class SettingsDialog extends JDialog {

    final SettingsPanel settingsPanel;

    public SettingsDialog(final ConfigurationModel configurationModel, final ActionListener listener) {

        // Initialize the GUI panel.
        this.settingsPanel = new SettingsPanel(this, configurationModel, listener);

        // Configure the frame.
        setTitle("Settings");
        setContentPane(this.settingsPanel);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();

        // Add window listener for turning invisible when closing.
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent e) {
                setVisible(false);
            }

            @Override
            public void windowOpened(final WindowEvent event) {
                SettingsDialog.this.setLocationRelativeTo(null);
            }
        });
    }
}
