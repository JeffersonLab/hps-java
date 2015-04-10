/**
 *
 */
package org.hps.monitoring.application;

import java.awt.Component;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.Subsystem;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusImpl;
import org.hps.monitoring.subsys.SystemStatusListener;

/**
 * This is a table that shows every system status change in a different row.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
final class SystemStatusEventsTable extends JTable {

    static class SystemStatusEventsTableModel extends DefaultTableModel implements SystemStatusListener {

        Class<?>[] columnClasses = { Date.class, Subsystem.class, SystemStatus.class, String.class, String.class };

        String[] columnNames = { "Date", "Subsystem", "Status Code", "Description", "Message" };
        List<SystemStatus> statuses = new ArrayList<SystemStatus>();

        SystemStatusEventsTableModel() {
        }

        /**
         * Register the listener on this status.
         *
         * @param status The system status.
         */
        void addSystemStatus(final SystemStatus status) {
            status.addListener(this);
        }

        public void clear() {
            this.statuses.clear();
            this.setRowCount(0);
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            return this.columnClasses[column];
        }

        @Override
        public int getColumnCount() {
            return this.columnNames.length;
        }

        @Override
        public String getColumnName(final int column) {
            return this.columnNames[column];
        }

        @Override
        public int getRowCount() {
            if (this.statuses != null) {
                return this.statuses.size();
            } else {
                return 0;
            }
        }

        @Override
        public Object getValueAt(final int row, final int column) {
            final SystemStatus status = this.statuses.get(row);
            switch (column) {
            case 0:
                return new Date(status.getLastChangedMillis());
            case 1:
                return status.getSubsystem();
            case 2:
                return status.getStatusCode();
            case 3:
                return status.getDescription();
            case 4:
                return status.getMessage();
            default:
                return null;
            }
        }

        /**
         * Update the table with status changes.
         *
         * @param status The system status.
         */
        @Override
        public void statusChanged(final SystemStatus status) {
            final SystemStatus newStatus = new SystemStatusImpl(status);
            this.statuses.add(newStatus);
            fireTableDataChanged();
        }
    }

    SystemStatusEventsTableModel tableModel = new SystemStatusEventsTableModel();

    SystemStatusEventsTable() {
        setModel(this.tableModel);

        // Date formatting.
        getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

            final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

            @Override
            public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected,
                    final boolean hasFocus, final int row, final int column) {
                if (value instanceof Date) {
                    value = this.dateFormat.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // Rendering of system status cells using different background colors.
        getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(final JTable table, final Object value,
                    final boolean isSelected, final boolean hasFocus, final int row, final int col) {

                // Cells are by default rendered as a JLabel.
                final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus,
                        row, col);

                // Color code the cell by its status.
                final StatusCode statusCode = (StatusCode) value;
                label.setBackground(statusCode.getColor());
                return label;
            }
        });
    }

    void registerListener() {
    }
}
