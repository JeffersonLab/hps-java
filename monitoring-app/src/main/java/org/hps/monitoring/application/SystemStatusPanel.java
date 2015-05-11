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
 * status monitors and the other with all system status change events.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class SystemStatusPanel extends JPanel {

    /**
     * The system status events table.
     */
    private final SystemStatusEventsTable eventsTable = new SystemStatusEventsTable();

    /**
     * The system status table.
     */
    private final SystemStatusTable statusTable = new SystemStatusTable();

    /**
     * Class constructor.
     */
    SystemStatusPanel() {
        super(new BorderLayout());
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(this.statusTable),
                new JScrollPane(this.eventsTable));
        splitPane.setDividerLocation(150);
        this.add(splitPane, BorderLayout.CENTER);
    }

    /**
     * Add a system status.
     *
     * @param status the system status to add
     */
    void addSystemStatus(final SystemStatus status) {
        // Register listeners of table models on this status.
        this.statusTable.getTableModel().addSystemStatus(status);
        this.eventsTable.getSystemStatusEventsTableModel().addSystemStatus(status);
    }

    /**
     * Clear all the table records.
     */
    void clear() {
        // Clear the system status monitor table.
        this.statusTable.getTableModel().clear();

        // Clear the system status events table.
        ((SystemStatusEventsTableModel) this.eventsTable.getModel()).clear();
    }
}
