package org.hps.monitoring.application;

import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * The modal dialog for entering application settings.
 * <p>
 * It contains a <code>JPanel</code> with the different settings sub-tabs.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class SettingsDialog extends JDialog {

    /**
     * The panel with the settings.
     */
    private final SettingsPanel settingsPanel;

    /**
     * Class constructor.
     *
     * @param configurationModel the configuration model with global settings
     * @param listener the action listener assigned to certain components
     */
    public SettingsDialog(final ConfigurationModel configurationModel, final ActionListener listener) {

        // Initialize the GUI panel.
        this.settingsPanel = new SettingsPanel(this, configurationModel, listener);

        // Configure the frame.
        this.setTitle("Settings");
        this.setContentPane(this.settingsPanel);
        this.setResizable(false);
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        this.setModalityType(ModalityType.APPLICATION_MODAL);
        this.pack();

        // Add window listener for turning invisible when closing.
        this.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(final WindowEvent e) {
                SettingsDialog.this.setVisible(false);
            }

            @Override
            public void windowOpened(final WindowEvent event) {
                SettingsDialog.this.setLocationRelativeTo(null);
            }
        });
    }
}
