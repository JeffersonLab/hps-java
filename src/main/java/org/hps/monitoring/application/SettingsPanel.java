package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * The container component with the tabs that have job and connection settings.
 */
public class SettingsPanel extends JPanel implements ActionListener {

    JTabbedPane tabs;
    JobSettingsPanel jobPanel;
    ConnectionSettingsPanel connectionPanel;
    static final String OKAY_COMMAND = "settingsOkay";

    JDialog parent;

    SettingsPanel(JDialog parent, ConfigurationModel configurationModel, ActionListener listener) {

        this.parent = parent;
        
        connectionPanel = new ConnectionSettingsPanel();        
        jobPanel = new JobSettingsPanel();
        
        // Push configuration to sub-components.
        connectionPanel.setConfigurationModel(configurationModel);
        jobPanel.setConfigurationModel(configurationModel);
        
        // Add ActionListener to sub-components.
        connectionPanel.addActionListener(listener);
        jobPanel.addActionListener(listener);
               
        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        tabs = new JTabbedPane();
        tabs.addTab("Connection Settings", connectionPanel);
        tabs.addTab("Job Settings", jobPanel);
        add(tabs);

        JButton okayButton = new JButton("Okay");
        okayButton.setActionCommand(OKAY_COMMAND);
        okayButton.addActionListener(this);

        add(Box.createRigidArea(new Dimension(1, 5)));
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okayButton);
        //buttonsPanel.add(defaultsButton);
        buttonsPanel.setLayout(new FlowLayout());
        add(buttonsPanel);
        add(Box.createRigidArea(new Dimension(1, 5)));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(OKAY_COMMAND)) {
            parent.setVisible(false);
        }
    }    
}
