package org.hps.monitoring.application;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;

/**
 * This is the panel for showing the current connection status (connected, disconnected, etc.).
 */
class ConnectionStatusPanel extends JPanel implements PropertyChangeListener {

    JTextField statusField;
    JTextField dateField;

    // Format for date field.
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss");
    
    ConnectionStatusModel model;

    /**
     * Class constructor.
     */
    ConnectionStatusPanel(ConnectionStatusModel model) {
        
        this.model = model;
        this.model.addPropertyChangeListener(this);
        
        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));
                
        statusField = new JTextField("", 10);
        statusField.setHorizontalAlignment(JTextField.LEFT);
        statusField.setEditable(false);
        statusField.setBackground(Color.WHITE);
        statusField.setFont(new Font("Arial", Font.BOLD, 16));
        statusField.setForeground(model.getConnectionStatus().getColor());
        statusField.setText(model.getConnectionStatus().name());
        add(statusField);
        
        add(new JLabel("@"));
        
        dateField = new JTextField("", 21);
        dateField.setEditable(false);
        dateField.setBackground(Color.WHITE);
        dateField.setHorizontalAlignment(JTextField.LEFT);
        dateField.setFont(new Font("Arial", Font.PLAIN, 14));
        add(dateField);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            final ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
            statusField.setForeground(status.getColor());
            statusField.setText(status.name());
            dateField.setText(dateFormat.format(new Date()));
        }        
    }
}