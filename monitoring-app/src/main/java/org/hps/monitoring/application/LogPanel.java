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
 */
@SuppressWarnings("serial")
final class LogPanel extends JPanel {

    /**
     * The combo box for filtering which messages are currently showing in the table.
     */
    private final LogLevelFilterComboBox logFilterComboBox;

    /**
     * The table containing the log messages.
     */
    private final LogTable logTable;

    /**
     * Class constructor.
     *
     * @param configurationModel the {@link org.hps.monitoring.application.model.ConfigurationModel} providing the data
     *            model
     * @param listener an {@link java.awt.event.ActionListener} to register on certain components
     */
    LogPanel(final ConfigurationModel configurationModel, final ActionListener listener) {

        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

        this.logTable = new LogTable(configurationModel);

        final JPanel controlsPanel = new JPanel();
        controlsPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 5));

        final JLabel label = new JLabel("Log Level Filter");
        this.logFilterComboBox = new LogLevelFilterComboBox(configurationModel);
        this.logFilterComboBox.setToolTipText("Messages below this level will be filtered out.");
        controlsPanel.add(label);
        controlsPanel.add(this.logFilterComboBox);

        final JButton exportButton = new JButton("Export ...");
        exportButton.setActionCommand(Commands.SAVE_LOG_TABLE);
        exportButton.addActionListener(listener);
        controlsPanel.add(exportButton);

        final JButton clearButton = new JButton("Clear");
        clearButton.setActionCommand(Commands.CLEAR_LOG_TABLE);
        clearButton.addActionListener(listener);
        controlsPanel.add(clearButton);

        final JScrollPane tablePane = new JScrollPane(this.logTable);
        controlsPanel.setMaximumSize(new Dimension(tablePane.getPreferredSize().width, 200));

        add(controlsPanel, BorderLayout.PAGE_START);
        add(tablePane, BorderLayout.PAGE_END);
    }

    /**
     * Get the log table.
     *
     * @return the log table
     */
    LogTable getLogTable() {
        return this.logTable;
    }
}
