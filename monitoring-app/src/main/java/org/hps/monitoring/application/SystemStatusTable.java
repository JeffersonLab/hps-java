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
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class SystemStatusTable extends JTable {

    /**
     * Renders a button if the status is clear-able.
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {

        /**
         * Class constructor.
         *
         * @param label the label of the button
         */
        public ButtonRenderer(final String label) {
            this.setText(label);
        }

        /**
         * Get the renderer for the table cell.
         *
         * @param table the table
         * @param value the object from the table cell
         * @param isSelected <code>true</code> if cell is selected
         * @param hasFocus <code>true</code> if cell has focus
         * @param rowIndex the row index
         * @param columnIndex the column index
         */
        @Override
        public Component getTableCellRendererComponent(final JTable table, final Object value,
                final boolean isSelected, final boolean hasFocus, final int rowIndex, final int columnIndex) {
            final boolean clearable = (Boolean) table.getModel().getValueAt(rowIndex,
                    SystemStatusTableModel.CLEARABLE_COLUMN_INDEX);
            if (clearable) {
                return this;
            } else {
                return null;
            }
        }
    }

    /**
     * Fires a mouse click event when the clear button is pressed, which in turn will activate the action event for the
     * button. The <code>ActionListener</code> then sets the <code>StatusCode</code> to <code>CLEARED</code>.
     */
    private static class JTableButtonMouseListener extends MouseAdapter {

        /**
         * The table.
         */
        private final JTable table;

        /**
         * Class constructor.
         *
         * @param table the table for the listener
         */
        public JTableButtonMouseListener(final JTable table) {
            this.table = table;
        }

        /**
         * Implement mouse clicked action.
         *
         * @param e the mouse event
         */
        @Override
        public void mouseClicked(final MouseEvent e) {
            final int column = this.table.getColumnModel().getColumnIndexAtX(e.getX());
            final int row = e.getY() / this.table.getRowHeight();
            if (row < this.table.getRowCount() && row >= 0 && column < this.table.getColumnCount() && column >= 0) {
                final Object value = this.table.getValueAt(row, column);
                if (value instanceof JButton) {
                    ((JButton) value).doClick();
                }
            }
        }
    }

    /**
     * Class constructor.
     */
    SystemStatusTable() {

        this.setModel(new SystemStatusTableModel());

        // Rendering of system status cells using different background colors.
        this.getColumnModel().getColumn(SystemStatusTableModel.STATUS_COLUMN_INDEX)
                .setCellRenderer(new DefaultTableCellRenderer() {

                    @Override
                    public Component getTableCellRendererComponent(final JTable table, final Object value,
                            final boolean isSelected, final boolean hasFocus, final int row, final int col) {

                        // Cells are by default rendered as a JLabel.
                        final JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected,
                                hasFocus, row, col);

                        // Color code the cell by its status.
                        final StatusCode statusCode = StatusCode.valueOf((String) value);
                        label.setBackground(statusCode.getColor());
                        return label;
                    }
                });

        // Date formatting for last changed.
        this.getColumnModel().getColumn(SystemStatusTableModel.LAST_CHANGED_COLUMN_INDEX)
                .setCellRenderer(new DefaultTableCellRenderer() {

                    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");

                    @Override
                    public Component getTableCellRendererComponent(final JTable table, Object value,
                            final boolean isSelected, final boolean hasFocus, final int row, final int column) {
                        if (value instanceof Date) {
                            value = this.dateFormat.format(value);
                        }
                        return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                    }
                });

        // Button for clearing system statuses.
        this.getColumnModel().getColumn(SystemStatusTableModel.RESET_COLUMN_INDEX)
                .setCellRenderer(new ButtonRenderer("Clear"));
        this.addMouseListener(new JTableButtonMouseListener(this));
        this.getColumn("Clearable").setWidth(0);
        this.getColumn("Clearable").setMinWidth(0);
        this.getColumn("Clearable").setMaxWidth(0);

        // Column widths.
        this.getColumnModel().getColumn(SystemStatusTableModel.ACTIVE_COLUMN_INDEX).setPreferredWidth(8);
        this.getColumnModel().getColumn(SystemStatusTableModel.STATUS_COLUMN_INDEX).setPreferredWidth(10);
        this.getColumnModel().getColumn(SystemStatusTableModel.SYSTEM_COLUMN_INDEX).setPreferredWidth(10);
        // TODO: Add default width setting for every column.

        this.setAutoCreateRowSorter(true);
    }

    /**
     * Get the tqble model.
     *
     * @return the table model
     */
    public SystemStatusTableModel getTableModel() {
        return (SystemStatusTableModel) this.getModel();
    }
}
