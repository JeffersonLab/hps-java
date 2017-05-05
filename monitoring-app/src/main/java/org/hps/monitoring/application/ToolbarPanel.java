package org.hps.monitoring.application;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;

import javax.swing.JPanel;

import org.hps.monitoring.application.model.ConfigurationModel;
import org.hps.monitoring.application.model.ConnectionStatusModel;

/**
 * A GUI component for the top-level toolbar of the monitoring app.
 *
 */
@SuppressWarnings("serial")
final class ToolbarPanel extends JPanel {

    /**
     * The panel with the buttons.
     */
    private final JPanel buttonsPanel;

    /**
     * The combo box with the list of data sources.
     */
    private final DataSourceComboBox dataSourceComboBox;

    /**
     * Class constructor.
     *
     * @param configurationModel the configuration model for the application
     * @param connectionModel the connection status model
     * @param listener the action listener to assign to certain components
     */
    ToolbarPanel(final ConfigurationModel configurationModel, final ConnectionStatusModel connectionModel,
            final ActionListener listener) {

        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        final JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new GridBagLayout());

        // Create the connection status panel.
        GridBagConstraints gbs = new GridBagConstraints();
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 0;
        gbs.gridy = 0;
        gbs.weightx = 0.5;
        gbs.fill = GridBagConstraints.BOTH;
        gbs.insets = new Insets(10, 0, 0, 10);
        final JPanel connectionPanel = new ConnectionStatusPanel(connectionModel);
        containerPanel.add(connectionPanel, gbs);

        // Create the buttons panel.
        this.buttonsPanel = new EventButtonsPanel(connectionModel, listener);
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 1;
        gbs.gridy = 0;
        gbs.weightx = 0.5;
        gbs.fill = GridBagConstraints.BOTH;
        gbs.insets = new Insets(0, 0, 0, 10);
        containerPanel.add(this.buttonsPanel, gbs);

        // Add the data source combo box.
        this.dataSourceComboBox = new DataSourceComboBox(configurationModel, connectionModel);
        gbs = new GridBagConstraints();
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 2;
        gbs.gridy = 0;
        gbs.weightx = 1.0;
        gbs.fill = GridBagConstraints.HORIZONTAL;
        containerPanel.add(this.dataSourceComboBox, gbs);

        this.add(containerPanel);
    }

    /**
     * Get the combo box with the data sources
     *
     * @return the combo box with the data sources
     */
    DataSourceComboBox getDataSourceComboBox() {
        return this.dataSourceComboBox;
    }
}
