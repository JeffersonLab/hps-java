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
import javax.swing.SwingConstants;

import org.hps.monitoring.application.model.ConnectionStatus;
import org.hps.monitoring.application.model.ConnectionStatusModel;

/**
 * This is the panel for showing the current connection status (connected, disconnected, etc.) in the tool bar.
 */
@SuppressWarnings("serial")
final class ConnectionStatusPanel extends JPanel implements PropertyChangeListener {

    /**
     * Date formatting.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss");

    /**
     * Field for date when status changed (read only).
     */
    private final JTextField dateField;

    /**
     * The model for getting connection status changes.
     */
    private final ConnectionStatusModel model;

    /**
     * The field showing the current connection status (read only).
     */
    private final JTextField statusField;

    /**
     * Class constructor.
     *
     * @param model the model which notifies this component of connection status changes
     */
    ConnectionStatusPanel(final ConnectionStatusModel model) {

        this.model = model;
        this.model.addPropertyChangeListener(this);

        setLayout(new FlowLayout(FlowLayout.LEFT, 5, 0));

        this.statusField = new JTextField("", 10);
        this.statusField.setHorizontalAlignment(SwingConstants.LEFT);
        this.statusField.setEditable(false);
        this.statusField.setBackground(Color.WHITE);
        this.statusField.setFont(new Font("Arial", Font.BOLD, 16));
        this.statusField.setForeground(model.getConnectionStatus().getColor());
        this.statusField.setText(model.getConnectionStatus().name());
        add(this.statusField);

        add(new JLabel("@"));

        this.dateField = new JTextField("", 21);
        this.dateField.setEditable(false);
        this.dateField.setBackground(Color.WHITE);
        this.dateField.setHorizontalAlignment(SwingConstants.LEFT);
        this.dateField.setFont(new Font("Arial", Font.PLAIN, 14));
        add(this.dateField);
    }

    /**
     * Handle a property change event coming from the model.
     *
     * @param evt the property change event
     */
    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(ConnectionStatusModel.CONNECTION_STATUS_PROPERTY)) {
            final ConnectionStatus status = (ConnectionStatus) evt.getNewValue();
            this.statusField.setForeground(status.getColor());
            this.statusField.setText(status.name());
            this.dateField.setText(ConnectionStatusPanel.DATE_FORMAT.format(new Date()));
        }
    }
}