package org.hps.monitoring.application;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.logging.Level;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * This is a combo box used to filter the log table messages by level.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class LogLevelFilterComboBox extends JComboBox<Level> implements ActionListener, PropertyChangeListener {

    /**
     * Available log levels.
     */
    private static final Level[] LOG_LEVELS = new Level[] { Level.ALL, Level.FINEST, Level.FINER, Level.FINE,
            Level.CONFIG, Level.INFO, Level.WARNING, Level.SEVERE };

    /**
     * The {@link org.hps.monitoring.application.model.ConfigurationModel} providing the backing model.
     */
    private final ConfigurationModel configurationModel;

    /**
     * Class constructor.
     *
     * @param configurationModel the {@link org.hps.monitoring.application.model.ConfigurationModel} providing the
     *            backing model
     */
    LogLevelFilterComboBox(final ConfigurationModel configurationModel) {

        configurationModel.addPropertyChangeListener(this);
        this.configurationModel = configurationModel;

        setModel(new DefaultComboBoxModel<Level>(LOG_LEVELS));
        setPrototypeDisplayValue(Level.WARNING);
        setSelectedItem(Level.ALL);
        setActionCommand(Commands.LOG_LEVEL_FILTER_CHANGED);
        addActionListener(this);
        setPreferredSize(new Dimension(100, getPreferredSize().height));
        setSize(new Dimension(100, getPreferredSize().height));
    }

    /**
     * The {@link java.awt.event.ActionEvent} handling, which is used to push changes to the global data model.
     *
     * @param event the {@link java.awt.event.ActionEvent} object
     */
    @Override
    public void actionPerformed(final ActionEvent event) {
        if (event.getActionCommand().equals(Commands.LOG_LEVEL_FILTER_CHANGED)) {
            this.configurationModel.removePropertyChangeListener(this);
            try {
                this.configurationModel.setLogLevelFilter((Level) getSelectedItem());
            } finally {
                this.configurationModel.addPropertyChangeListener(this);
            }
        }
    }

    /**
     * The {@link java.beans.PropertyChangeEvent} handling, which is used to get changes from the model into the GUI.
     *
     * @param event the {@link java.beans.PropertyChangeEvent} object to handle
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (event.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_FILTER_PROPERTY)) {
            final Level newLevel = (Level) event.getNewValue();
            this.configurationModel.removePropertyChangeListener(this);
            try {
                setSelectedItem(newLevel);
            } finally {
                this.configurationModel.addPropertyChangeListener(this);
            }
        }
    }
}
