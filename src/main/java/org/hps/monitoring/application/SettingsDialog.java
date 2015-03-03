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
public class SettingsDialog extends JDialog {

    final SettingsPanel settingsPanel = new SettingsPanel(this);

    public SettingsDialog() {

        setTitle("Settings");
        setContentPane(settingsPanel);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                setVisible(false);
            }
        });
    }

    public SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }
    
    void addActionListener(ActionListener listener) {
        settingsPanel.addActionListener(listener);
        settingsPanel.getJobSettingsPanel().addActionListener(listener);
        settingsPanel.getDataSourcePanel().addActionListener(listener);
    }
    
    void setConfigurationModel(ConfigurationModel model) {
        settingsPanel.getJobSettingsPanel().setConfigurationModel(model);
        settingsPanel.getConnectionPanel().setConfigurationModel(model);
        settingsPanel.getDataSourcePanel().setConfigurationModel(model);
    }    
}
