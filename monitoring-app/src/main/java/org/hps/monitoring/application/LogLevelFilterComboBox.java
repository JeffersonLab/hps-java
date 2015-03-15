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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class LogLevelFilterComboBox extends JComboBox<Level> implements ActionListener, PropertyChangeListener {
   
    ConfigurationModel configurationModel;
    
    static final Level[] LOG_LEVELS = new Level[] {
        Level.ALL, 
        Level.FINEST, 
        Level.FINER, 
        Level.FINE, 
        Level.CONFIG, 
        Level.INFO, 
        Level.WARNING, 
        Level.SEVERE
    };
    
    LogLevelFilterComboBox(ConfigurationModel configurationModel) {
        
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
     * Push change in log level filtering to the configuration model.
     */
    public void actionPerformed(ActionEvent event) {
        if (event.getActionCommand().equals(Commands.LOG_LEVEL_FILTER_CHANGED)) {
            configurationModel.removePropertyChangeListener(this);
            try {                
                configurationModel.setLogLevelFilter((Level) getSelectedItem());
            } finally {
                configurationModel.addPropertyChangeListener(this);
            }
        }
    }
    
    /**
     * Get change in log level filtering from the configuration model.     
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_FILTER_PROPERTY)) {
            Level newLevel = (Level) event.getNewValue();
            configurationModel.removePropertyChangeListener(this);
            try {
                setSelectedItem(newLevel);
            } finally {
                configurationModel.addPropertyChangeListener(this);
            }
        }
    }
}
