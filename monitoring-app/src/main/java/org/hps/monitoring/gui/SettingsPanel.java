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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class SettingsPanel extends JPanel implements ActionListener {

    JTabbedPane tabs;
    JobPanel jobPanel = new JobPanel();
    ConnectionPanel connectionPanel = new ConnectionPanel();
    DataSourcePanel dataSourcePanel = new DataSourcePanel();
    static final String okayCommand = "settingsOkay";
    static final String cancelCommand = "settingsCancel";
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
        okayButton.setActionCommand(okayCommand);
        okayButton.addActionListener(this);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setActionCommand(cancelCommand);
        cancelButton.addActionListener(this);
                
        add(Box.createRigidArea(new Dimension(1,5)));
        JPanel buttonsPanel = new JPanel();
        buttonsPanel.add(okayButton);
        buttonsPanel.add(cancelButton);
        buttonsPanel.setLayout(new FlowLayout());        
        add(buttonsPanel);
        add(Box.createRigidArea(new Dimension(1,5)));
    }
        
    ConnectionPanel getConnectionPanel() {
        return connectionPanel;
    }
    
    JobPanel getJobPanel() {
        return jobPanel;
    }
    
    DataSourcePanel getDataSourcePanel() {
        return dataSourcePanel;
    }
    
    /**
     * Caches the object for the settings based on current GUI values.
     */
    void cache() {
        connectionPanel.cache();
        jobPanel.cache();
    }
    
    /**
     * Revert the settings to their unedited values.
     */
    void revert() {
        connectionPanel.revert();
        jobPanel.revert();
    }
    
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
        if (e.getActionCommand().equals(okayCommand)) {
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
                cache();
                parent.setVisible(false);
            }                
        } else if (e.getActionCommand().equals(cancelCommand)) {
            revert();
            parent.setVisible(false);
        }
    }
}
