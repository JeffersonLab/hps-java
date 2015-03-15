package org.hps.monitoring.application;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.hps.monitoring.application.model.SystemStatusTableModel;
import org.hps.monitoring.subsys.StatusCode;

/**
 * This table shows the current state of {@link org.hps.monitoring.subsys.SystemStatus} objects.
 */
class SystemStatusTable extends JTable {

    SystemStatusTable() {

        setModel(new SystemStatusTableModel());

        // Rendering of system status cells using different background colors.
        getColumnModel().getColumn(SystemStatusTableModel.STATUS_COL).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                // Cells are by default rendered as a JLabel.
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                // Color code the cell by its status.
                StatusCode statusCode = StatusCode.valueOf((String) value);
                label.setBackground(statusCode.getColor());
                return label;
            }
        });

        // Date formatting for last changed.
        getColumnModel().getColumn(SystemStatusTableModel.LAST_CHANGED_COL).setCellRenderer(new DefaultTableCellRenderer() {

            final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                if (value instanceof Date) {
                    value = dateFormat.format(value);
                }
                return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        });

        // Button for clearing system statuses.
        getColumnModel().getColumn(SystemStatusTableModel.RESET_COL).setCellRenderer(new ButtonRenderer("Clear"));
        addMouseListener(new JTableButtonMouseListener(this));
        getColumn("Clearable").setWidth(0);
        getColumn("Clearable").setMinWidth(0);
        getColumn("Clearable").setMaxWidth(0);

        // Column widths.
        getColumnModel().getColumn(SystemStatusTableModel.ACTIVE_COL).setPreferredWidth(8);
        getColumnModel().getColumn(SystemStatusTableModel.STATUS_COL).setPreferredWidth(10);
        getColumnModel().getColumn(SystemStatusTableModel.SYSTEM_COL).setPreferredWidth(10);
        // TODO: Add default width setting for every column.

        setAutoCreateRowSorter(true);
    }

    public SystemStatusTableModel getTableModel() {
        return (SystemStatusTableModel) getModel();
    }

    /**
     * Renders a button if the status is clearable.
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {

        public ButtonRenderer(String label) {
            this.setText(label);
        }

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            boolean clearable = (Boolean) table.getModel().getValueAt(row, SystemStatusTableModel.CLEARABLE_COL);
            if (clearable)
                return this;
            else
                return null;
        }
    }

    /**
     * Fires a mouse click event when the clear button is pressed, which in turn will activate the
     * action event for the button. The <code>ActionListener</code> then sets the
     * <code>StatusCode</code> to <code>CLEARED</code>.
     */
    private static class JTableButtonMouseListener extends MouseAdapter {
        private final JTable table;

        public JTableButtonMouseListener(JTable table) {
            this.table = table;
        }

        public void mouseClicked(MouseEvent e) {
            int column = table.getColumnModel().getColumnIndexAtX(e.getX());
            int row = e.getY() / table.getRowHeight();
            if (row < table.getRowCount() && row >= 0 && column < table.getColumnCount() && column >= 0) {
                Object value = table.getValueAt(row, column);
                if (value instanceof JButton) {
                    ((JButton) value).doClick();
                }
            }
        }
    }
}