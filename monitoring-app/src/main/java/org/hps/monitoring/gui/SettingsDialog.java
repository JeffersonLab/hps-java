package org.hps.monitoring.gui;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JDialog;

/**
 * The modal dialog for entering settings.  It contains a <code>JPanel</code>
 * with the different settings sub-tabs.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SettingsDialog extends JDialog {
    
    final SettingsPanel settingsPanel = new SettingsPanel(this);

    SettingsDialog() {

        setTitle("Settings");
        setContentPane(settingsPanel);
        setResizable(false);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setModalityType(ModalityType.APPLICATION_MODAL);
        pack();
        
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                settingsPanel.revert();
                setVisible(false);
            }
        });
    }
    
    SettingsPanel getSettingsPanel() {
        return settingsPanel;
    }
}
