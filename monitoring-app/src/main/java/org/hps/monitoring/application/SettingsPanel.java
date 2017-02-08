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
 */
@SuppressWarnings("serial")
final class SettingsPanel extends JPanel implements ActionListener {

    /**
     * The panel with connection settings.
     */
    private final ConnectionSettingsPanel connectionPanel;

    /**
     * The panel with general job settings.
     */
    private final JobSettingsPanel jobPanel;

    /**
     * The parent dialog window.
     */
    private final JDialog parent;

    /**
     * The tabs with the sub-panels.
     */
    private final JTabbedPane tabs;

    /**
     * Class constructor.
     *
     * @param parent the parent dialog window
     * @param configurationModel the global configuration model
     * @param listener the action listener assigned to certain components
     */
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
        this.add(this.tabs);

        final JButton okayButton = new JButton("Okay");
        okayButton.setActionCommand(Commands.SETTINGS_OKAY_COMMAND);
        okayButton.addActionListener(this);

        this.add(Box.createRigidArea(new Dimension(1, 5)));
        final JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okayButton);
        buttonsPanel.setLayout(new FlowLayout());
        this.add(buttonsPanel);
        this.add(Box.createRigidArea(new Dimension(1, 5)));
    }

    /**
     * Handle action events.
     *
     * @param e the action event to handle
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals(Commands.SETTINGS_OKAY_COMMAND)) {
            this.parent.setVisible(false);
        }
    }
}
