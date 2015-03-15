/**
 * 
 */
package org.hps.monitoring.application;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.hps.monitoring.application.SystemStatusEventsTable.SystemStatusEventsTableModel;
import org.hps.monitoring.subsys.SystemStatus;

/**
 * This is a panel showing the two tables for viewing the system statuses,
 * one showing the current state of all system status monitors and the other
 * all system status change events.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SystemStatusPanel extends JPanel {
    
    SystemStatusTable statusTable = new SystemStatusTable();
    SystemStatusEventsTable eventsTable = new SystemStatusEventsTable();
        
    SystemStatusPanel() {         
        super(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT, 
                new JScrollPane(statusTable), 
                new JScrollPane(eventsTable));
        splitPane.setDividerLocation(50);
        add(splitPane,
            BorderLayout.CENTER);
    }   
    
    void addSystemStatus(SystemStatus status) {
        // Register listeners of table models on this status.
        statusTable.getTableModel().addSystemStatus(status);        
        eventsTable.tableModel.addSystemStatus(status);
    }
    
    void clear() {
        // Clear the system status monitor table.
        statusTable.getTableModel().clear();    

        // Clear the system status events table.
        ((SystemStatusEventsTableModel)eventsTable.getModel()).clear();
    }
}
