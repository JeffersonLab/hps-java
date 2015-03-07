package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * This is a simple GUI component for the log table and its controls.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LogPanel extends JPanel {

    LogTable logTable;
        
    LogPanel(ConfigurationModel configurationModel, ActionListener listener) {
        
        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        logTable = new LogTable(configurationModel);
                        
        JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));
        
        JLabel label = new JLabel("Log Level Filter");
        LogLevelFilterComboBox logFilterComboBox = new LogLevelFilterComboBox(configurationModel);
        logFilterComboBox.setToolTipText("Messages below this level will be filtered out.");              
        controlsPanel.add(label);        
        controlsPanel.add(logFilterComboBox);
        
        JButton exportButton = new JButton("Export ...");
        exportButton.setActionCommand(Commands.SAVE_LOG_TABLE);
        exportButton.addActionListener(listener);
        controlsPanel.add(exportButton);
        
        JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand(Commands.CLEAR_LOG_TABLE);
        clearButton.addActionListener(listener);
        controlsPanel.add(clearButton);
                              
        JScrollPane tablePane = new JScrollPane(logTable);        
        controlsPanel.setMaximumSize(new Dimension(tablePane.getPreferredSize().width, 200));
                
        add(controlsPanel, BorderLayout.PAGE_START);
        add(tablePane, BorderLayout.PAGE_END);
    }    
}
