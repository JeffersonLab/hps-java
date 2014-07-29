package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.SystemStatusTableModel.ACTIVE_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.CLEARABLE_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.LAST_CHANGED_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.RESET_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.STATUS_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.SYSTEM_COL;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.hps.monitoring.enums.StatusCode;

/**
 * A GUI window for showing changes to {@link org.hps.monitoring.subsys.SystemStatus} objects.
 */
public class SystemStatusFrame extends JFrame implements HasErrorHandler {

    JTable table;
    ErrorHandler errorHandler;

    int WIDTH = 650;
    int HEIGHT = ScreenUtil.getScreenHeight() / 2;
        
    SystemStatusFrame() {
        table = new JTable(new SystemStatusTableModel());
        
        // Rendering of system status cells using different background colors.
        table.getColumnModel().getColumn(SystemStatusTableModel.STATUS_COL).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                // Cells are by default rendered as a JLabel.
                JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                // Color code the cell by its status.
                StatusCode statusCode = StatusCode.valueOf((String) value);
                if (statusCode.ordinal() >= StatusCode.ERROR.ordinal()) {
                    // Any type of error is red.
                    label.setBackground(Color.RED);
                } else if (statusCode.equals(StatusCode.WARNING)) {
                    // Warnings are yellow.
                    label.setBackground(Color.YELLOW);
                } else if (statusCode.equals(StatusCode.OKAY)) {
                    // Okay is green.
                    label.setBackground(Color.GREEN);
                } else if (statusCode.equals(StatusCode.OFFLINE)) {
                    // Offline is orange.
                    label.setBackground(Color.ORANGE);
                } else if (statusCode.equals(StatusCode.UNKNOWN)) {
                    // Unknown is gray.
                    label.setBackground(Color.GRAY);
                } else if (statusCode.equals(StatusCode.CLEARED)) {
                    // Cleared is light gray.
                    label.setBackground(Color.LIGHT_GRAY);
                } else {
                    // Default is white, though this shouldn't happen!
                    label.setBackground(Color.WHITE);
                }
                return label;
            }
        });
        
        // Date formatting for last changed.
        table.getColumnModel().getColumn(LAST_CHANGED_COL).setCellRenderer(new DefaultTableCellRenderer() {

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
        table.getColumnModel().getColumn(RESET_COL).setCellRenderer(new ButtonRenderer("Clear"));
        table.addMouseListener(new JTableButtonMouseListener(table));
        table.getColumn("Clearable").setWidth(0);
        table.getColumn("Clearable").setMinWidth(0);
        table.getColumn("Clearable").setMaxWidth(0);
        
        // Column widths.
        table.getColumnModel().getColumn(ACTIVE_COL).setPreferredWidth(8);
        table.getColumnModel().getColumn(STATUS_COL).setPreferredWidth(10);
        table.getColumnModel().getColumn(SYSTEM_COL).setPreferredWidth(10);
        // TODO: Add widths for every column.
                
        // Scroll pane.
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(true);

        // Additional config.
        setMinimumSize(new Dimension(WIDTH, HEIGHT));
        setTitle("System Status Monitor");
        setContentPane(scrollPane);
        setResizable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        pack();
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    public SystemStatusTableModel getTableModel() {
        return (SystemStatusTableModel) table.getModel();
    }
    
    /**
     * Renders a button if the status is clearable.
     */
    private class ButtonRenderer extends JButton implements TableCellRenderer {
                
        public ButtonRenderer(String label) {
            this.setText(label);
        }

        public Component getTableCellRendererComponent(
                JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {            
            boolean clearable = (Boolean)table.getModel().getValueAt(row, CLEARABLE_COL);
            if (clearable)
                return this;
            else 
                return null;
        }
    }
    
    /**
     * Fires a mouse click event when the clear button is pressed, which in turn
     * will activate the action event for the button.  The <code>ActionListener</code> 
     * then sets the <code>StatusCode</code> to <code>CLEARED</code>.
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
                    ((JButton)value).doClick();
                }
            }
        }
    }          
}