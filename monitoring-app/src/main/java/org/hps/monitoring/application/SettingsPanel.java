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
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class SettingsPanel extends JPanel implements ActionListener {

    static final String OKAY_COMMAND = "settingsOkay";
    ConnectionSettingsPanel connectionPanel;
    JobSettingsPanel jobPanel;
    JDialog parent;

    JTabbedPane tabs;

    SettingsPanel(final JDialog parent, final ConfigurationModel configurationModel, final ActionListener listener) {

        this.parent = parent;

        this.connectionPanel = new ConnectionSettingsPanel();
        this.jobPanel = new JobSettingsPanel(configurationModel);

        // Push configuration to sub-components.
        this.connectionPanel.setConfigurationModel(configurationModel);
        this.jobPanel.setConfigurationModel(configurationModel);

        // Add ActionListener to sub-components.
        this.connectionPanel.addActionListener(listener);
        this.jobPanel.addActionListener(listener);

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.tabs = new JTabbedPane();
        this.tabs.addTab("Connection Settings", this.connectionPanel);
        this.tabs.addTab("Job Settings", this.jobPanel);
        add(this.tabs);

        final JButton okayButton = new JButton("Okay");
        okayButton.setActionCommand(OKAY_COMMAND);
        okayButton.addActionListener(this);

        add(Box.createRigidArea(new Dimension(1, 5)));
        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okayButton);
        buttonsPanel.setLayout(new FlowLayout());
        add(buttonsPanel);
        add(Box.createRigidArea(new Dimension(1, 5)));
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals(OKAY_COMMAND)) {
            this.parent.setVisible(false);
        }
    }
}
