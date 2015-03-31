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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ToolbarPanel extends JPanel {

    DataSourceComboBox dataSourceComboBox;
    JPanel buttonsPanel;

    ToolbarPanel(ConfigurationModel configurationModel, ConnectionStatusModel connectionModel, ActionListener listener) {

        setLayout(new FlowLayout(FlowLayout.LEFT));
        
        JPanel containerPanel = new JPanel();
        containerPanel.setLayout(new GridBagLayout());
        
        // Create the connection status panel.
        GridBagConstraints gbs = new GridBagConstraints();
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 0;
        gbs.gridy = 0;
        gbs.weightx = 0.5;
        gbs.fill = GridBagConstraints.BOTH;
        gbs.insets = new Insets(10, 0, 0, 10);
        JPanel connectionPanel = new ConnectionStatusPanel(connectionModel);
        containerPanel.add(connectionPanel, gbs);

        // Create the buttons panel.
        buttonsPanel = new EventButtonsPanel(connectionModel, listener);
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 1;
        gbs.gridy = 0;
        gbs.weightx = 0.5;
        gbs.fill = GridBagConstraints.BOTH;
        gbs.insets = new Insets(0, 0, 0, 10);
        containerPanel.add(buttonsPanel, gbs);

        // Add the data source combo box.
        dataSourceComboBox = new DataSourceComboBox(configurationModel, connectionModel);
        gbs = new GridBagConstraints();
        gbs.anchor = GridBagConstraints.WEST;
        gbs.gridx = 2;
        gbs.gridy = 0;
        gbs.weightx = 1.0;
        gbs.fill = GridBagConstraints.HORIZONTAL;
        containerPanel.add(dataSourceComboBox, gbs);
        
        add(containerPanel);
    }

}
