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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SystemStatusEventsTable extends JTable {
    
    SystemStatusEventsTableModel tableModel = new SystemStatusEventsTableModel();
    
    SystemStatusEventsTable() {
        setModel(tableModel);
        
        // Date formatting.
        getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

            final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Date) {
                    value = dateFormat.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });
        
        // Rendering of system status cells using different background colors.
        getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                // Cells are by default rendered as a JLabel.
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                // Color code the cell by its status.
                StatusCode statusCode = (StatusCode) value;
                label.setBackground(statusCode.getColor());
                return label;
            }
        });
    }
    
    void registerListener() {
    }
    
    static class SystemStatusEventsTableModel extends DefaultTableModel implements SystemStatusListener {
        
        List<SystemStatus> statuses = new ArrayList<SystemStatus>();
        
        String[] columnNames = { "Date", "Subsystem", "Status Code", "Description", "Message" };
        Class<?>[] columnClasses = { Date.class, Subsystem.class, SystemStatus.class, String.class, String.class };

        SystemStatusEventsTableModel() {
        }
        
        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
        
        @Override
        public Class<?> getColumnClass(int column) {
            return columnClasses[column];
        }

        /**
         * Update the table with status changes.
         * @param status The system status.
         */
        @Override
        public void statusChanged(SystemStatus status) {
            SystemStatus newStatus = new SystemStatusImpl(status);
            statuses.add(newStatus);
            fireTableDataChanged();
        }
        
        /**
         * Register the listener on this status.
         * @param status The system status.
         */
        void addSystemStatus(SystemStatus status) {
            status.addListener(this);
        }
        
        @Override
        public int getColumnCount() {
            return columnNames.length;
        }
        
        @Override
        public int getRowCount() {
            if (statuses != null) {
                return statuses.size();
            } else {
                return 0;
            }
        }
        
        @Override
        public Object getValueAt(int row, int column) {
            SystemStatus status = statuses.get(row);
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
        
        public void clear() {
            this.statuses.clear();
            this.setRowCount(0);
        }
    }    
}
