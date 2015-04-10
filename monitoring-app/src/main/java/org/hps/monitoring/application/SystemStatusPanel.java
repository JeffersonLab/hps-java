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
 * This is a panel showing the two tables for viewing the system statuses, one showing the current state of all system
 * status monitors and the other all system status change events.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class SystemStatusPanel extends JPanel {

    SystemStatusEventsTable eventsTable = new SystemStatusEventsTable();
    SystemStatusTable statusTable = new SystemStatusTable();

    SystemStatusPanel() {
        super(new BorderLayout());
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(this.statusTable),
                new JScrollPane(this.eventsTable));
        splitPane.setDividerLocation(50);
        add(splitPane, BorderLayout.CENTER);
    }

    void addSystemStatus(final SystemStatus status) {
        // Register listeners of table models on this status.
        this.statusTable.getTableModel().addSystemStatus(status);
        this.eventsTable.tableModel.addSystemStatus(status);
    }

    void clear() {
        // Clear the system status monitor table.
        this.statusTable.getTableModel().clear();

        // Clear the system status events table.
        ((SystemStatusEventsTableModel) this.eventsTable.getModel()).clear();
    }
}
