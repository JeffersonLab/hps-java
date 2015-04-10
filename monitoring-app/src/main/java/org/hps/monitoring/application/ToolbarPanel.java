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
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class ToolbarPanel extends JPanel {

    private final JPanel buttonsPanel;
    private final DataSourceComboBox dataSourceComboBox;

    ToolbarPanel(final ConfigurationModel configurationModel, final ConnectionStatusModel connectionModel,
            final ActionListener listener) {

        setLayout(new FlowLayout(FlowLayout.LEFT));

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

        add(containerPanel);
    }

    DataSourceComboBox getDataSourceComboBox() {
        return this.dataSourceComboBox;
    }

}
