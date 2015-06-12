/**
 *
 */
package org.hps.monitoring.application;

import java.awt.BorderLayout;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.hps.monitoring.application.SystemStatusEventsTable.SystemStatusEventsTableModel;
import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;

/**
 * This is a panel showing the two tables for viewing the system statuses, one
 * showing the current state of all system status monitors and the other with
 * all system status change events.
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
     * The list of statuses to check.
     */
    private final List<SystemStatus> statuses = new ArrayList<SystemStatus>();

    private final Timer timer = new Timer("System Status Beeper");

    /**
     * Class constructor.
     */
    SystemStatusPanel() {
        super(new BorderLayout());
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(this.statusTable),
                new JScrollPane(this.eventsTable));
        splitPane.setDividerLocation(150);
        this.add(splitPane, BorderLayout.CENTER);
        timer.scheduleAtFixedRate(new SystemStatusBeeper(), 0, 1000);
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
        this.statuses.add(status);
    }

    /**
     * Clear all the table records.
     */
    void clear() {
        // Clear the system status monitor table.
        this.statusTable.getTableModel().clear();

        // Clear the system status events table.
        ((SystemStatusEventsTableModel) this.eventsTable.getModel()).clear();
        
        this.statuses.clear();
    }

    private class SystemStatusBeeper extends TimerTask {

        @Override
        public void run() {
            boolean isAlarming = false;
            for (SystemStatus status : statuses) {
                if (status.isActive() && status.getStatusCode() == StatusCode.ALARM) {
                    isAlarming = true;
                }
            }
            if (isAlarming) {
                System.out.println("beep\007");
            }
        }
    }
}
