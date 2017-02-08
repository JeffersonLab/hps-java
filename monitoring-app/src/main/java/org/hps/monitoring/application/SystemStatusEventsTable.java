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
 */
@SuppressWarnings("serial")
final class SystemStatusEventsTable extends JTable {

    /**
     * The model for the system status events table.
     */
    static class SystemStatusEventsTableModel extends DefaultTableModel implements SystemStatusListener {

        /**
         * The classes of the table columns.
         */
        private final Class<?>[] columnClasses = {Date.class, Subsystem.class, SystemStatus.class, String.class,
                String.class};

        /**
         * The names of the columns.
         */
        private final String[] columnNames = {"Date", "Subsystem", "Status Code", "Description", "Message"};

        /**
         * The list of statuses shown in the table.
         */
        private final List<SystemStatus> statuses = new ArrayList<SystemStatus>();

        /**
         * Class constructor.
         */
        SystemStatusEventsTableModel() {
        }

        /**
         * Register the listener on this status.
         *
         * @param status the system status
         */
        void addSystemStatus(final SystemStatus status) {
            status.addListener(this);
        }

        /**
         * Clear all the records from the table.
         */
        void clear() {
            this.statuses.clear();
            this.setRowCount(0);
        }

        /**
         * Get the column class.
         *
         * @param columnIndex the column index
         */
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            return this.columnClasses[columnIndex];
        }

        /**
         * Get the column count.
         *
         * @return the column count
         */
        @Override
        public int getColumnCount() {
            return this.columnNames.length;
        }

        /**
         * Get the column name.
         *
         * @param columnIndex the column index
         */
        @Override
        public String getColumnName(final int columnIndex) {
            return this.columnNames[columnIndex];
        }

        /**
         * Get the row count.
         *
         * @return the row count
         */
        @Override
        public int getRowCount() {
            if (this.statuses != null) {
                return this.statuses.size();
            } else {
                return 0;
            }
        }

        /**
         * Get a cell value from the table.
         *
         * @param rowIndex the row index
         * @param columnIndex the column index
         * @return the cell value at the rowIndex and columnIndex
         */
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final SystemStatus status = this.statuses.get(rowIndex);
            switch (columnIndex) {
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
         * @param status the system status
         */
        @Override
        public void statusChanged(final SystemStatus status) {
            final SystemStatus newStatus = new SystemStatusImpl(status);
            this.statuses.add(newStatus);
            this.fireTableDataChanged();
        }
    }

    /**
     * The table model.
     */
    private final SystemStatusEventsTableModel tableModel = new SystemStatusEventsTableModel();

    /**
     * Class constructor.
     */
    SystemStatusEventsTable() {
        this.setModel(this.tableModel);

        // Date formatting.
        this.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

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
        this.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {

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

    /**
     * Get the system status events table model.
     *
     * @return the system status events table model
     */
    SystemStatusEventsTableModel getSystemStatusEventsTableModel() {
        return this.tableModel;
    }
}
