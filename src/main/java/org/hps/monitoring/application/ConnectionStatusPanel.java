package org.hps.monitoring.application;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.hps.monitoring.application.model.ConnectionStatusModel;

/**
 * This is the panel for showing the current connection status (connected, disconnected, etc.).
 */
class ConnectionStatusPanel extends JPanel implements PropertyChangeListener {

    JTextField statusField;
    JTextField dateField;

    // Format for date field.
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss");

    private static final int PANEL_HEIGHT = 50;
    private static final int PANEL_WIDTH = 400;
    
    ConnectionStatusModel model;

    /**
     * Class constructor.
     */
    ConnectionStatusPanel(ConnectionStatusModel model) {
        
        this.model = model;
        this.model.addPropertyChangeListener(this);

        setMinimumSize(new Dimension(PANEL_WIDTH, PANEL_HEIGHT));

        setLayout(new GridBagLayout());
        // setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
        Font font = new Font("Arial", Font.PLAIN, 14);

        GridBagConstraints c = new GridBagConstraints();
        c.weightx = c.weighty = 1.0;

        // Connection status label.
        c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(0, 10, 0, 10);
        JLabel statusLabel = new JLabel("Connection Status:");
        statusLabel.setHorizontalAlignment(JLabel.LEFT);
        add(statusLabel, c);

        // Connection status field.
        c.gridx = 1;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 10);
        statusField = new JTextField("", 15);
        statusField.setHorizontalAlignment(JTextField.LEFT);
        statusField.setEditable(false);
        statusField.setBackground(Color.WHITE);
        statusField.setFont(font);
        statusField.setMinimumSize(new Dimension(300, 50));
        add(statusField, c);

        // The "@" label.
        c.gridx = 2;
        c.gridy = 1;
        c.anchor = GridBagConstraints.CENTER;
        c.fill = GridBagConstraints.NONE;
        c.insets = new Insets(0, 0, 0, 10);
        JLabel atLabel = new JLabel("@");
        add(atLabel, c);

        // The date field.
        c = new GridBagConstraints();
        c.gridx = 3;
        c.gridy = 1;
        c.anchor = GridBagConstraints.WEST;
        dateField = new JTextField("", 20);
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setHorizontalAlignment(JTextField.LEFT);
        dateField.setFont(font);
        dateField.setMinimumSize(new Dimension(200, 50));
        add(dateField, c);
    }

    void setConnectionStatus(final ConnectionStatus status) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                statusField.setText(status.name());
                dateField.setText(dateFormat.format(new Date()));
            }
        });
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            final ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    statusField.setText(status.name());
                    dateField.setText(dateFormat.format(new Date()));
                }
            }); 
        }        
    }
}