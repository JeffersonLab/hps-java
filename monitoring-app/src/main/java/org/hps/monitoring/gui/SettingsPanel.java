package org.hps.monitoring.gui;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;

/**
 * The container component with the tabs which have job and connection settings.
 */
class SettingsPanel extends JPanel implements ActionListener {

    JTabbedPane tabs;
    JobSettingsPanel jobPanel = new JobSettingsPanel();
    ConnectionSettingsPanel connectionPanel = new ConnectionSettingsPanel();
    DataSourcePanel dataSourcePanel = new DataSourcePanel();
    static final String OKAY_COMMAND = "settingsOkay";
    static final String CANCEL_COMMAND = "settingsCancel";
    
    JButton defaultsButton;
    
    JDialog parent;
    ErrorHandler errorHandler;
        
    SettingsPanel(JDialog parent) {

        this.parent = parent;

        this.setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
        
        tabs = new JTabbedPane();
        tabs.addTab("Connection Settings", connectionPanel);
        tabs.addTab("Job Settings", jobPanel);
        tabs.addTab("Data Source", dataSourcePanel);
        add(tabs);
                
        JButton okayButton = new JButton("Okay");
        okayButton.setActionCommand(OKAY_COMMAND);
        okayButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(CANCEL_COMMAND);
        cancelButton.addActionListener(this);
        
        defaultsButton = new JButton("Defaults");
        defaultsButton.setActionCommand(MonitoringCommands.LOAD_DEFAULT_CONFIG_FILE);
        defaultsButton.addActionListener(this);
                
        add(Box.createRigidArea(new Dimension(1,5)));
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okayButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(defaultsButton);
        buttonsPanel.setLayout(new FlowLayout());        
        add(buttonsPanel);
        add(Box.createRigidArea(new Dimension(1,5)));
    }
            
    ConnectionSettingsPanel getConnectionPanel() {
        return connectionPanel;
    }
    
    JobSettingsPanel getJobSettingsPanel() {
        return jobPanel;
    }
    
    DataSourcePanel getDataSourcePanel() {
        return dataSourcePanel;
    }
    
    /**
     * Pushes the GUI settings into the current configuration object.
     */
    void save() {
        connectionPanel.save();
        //jobPanel.save();
        dataSourcePanel.save();
    }
    
    /**
     * Resets the settings to the last saved configuration.
     */
    void reset() {
        connectionPanel.reset();
        //jobPanel.reset();
        dataSourcePanel.reset();
    }
    
    // FIXME: This isn't the right way to do this!
    String getError() {
        try {
            this.dataSourcePanel.checkFile();
            return null;
        } catch (IOException e) {
            return e.getMessage();
        }
    }
        
    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getActionCommand().equals(OKAY_COMMAND)) {
            final String errorMessage = getError();
            if (errorMessage != null) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        JOptionPane.showMessageDialog(
                                SettingsPanel.this, 
                                errorMessage, 
                                "Settings Error", 
                                JOptionPane.ERROR_MESSAGE);
                    }
                });
            } else {
                save();
                parent.setVisible(false);
            }                
        } else if (e.getActionCommand().equals(CANCEL_COMMAND)) {
            reset();
            parent.setVisible(false);
        }
    }    
    
    /**
     * This method is used to register a listener so that the Monitoring Application 
     * can reset to the default configuration when the "Defaults" button is pushed from
     * the settings panel. 
     * @param listener
     */
    void addActionListener(ActionListener listener) {
        defaultsButton.addActionListener(listener);
    }
}
