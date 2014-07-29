package org.hps.monitoring.gui;

import static org.hps.monitoring.gui.SystemStatusTableModel.ACTIVE_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.LAST_CHANGED_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.STATUS_COL;
import static org.hps.monitoring.gui.SystemStatusTableModel.SYSTEM_COL;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;

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
                } else if (statusCode.ordinal() == StatusCode.WARNING.ordinal()) {
                    // Warnings are yellow.
                    label.setBackground(Color.YELLOW);
                } else if (statusCode.ordinal() == StatusCode.OKAY.ordinal()) {
                    // Okay is green.
                    label.setBackground(Color.GREEN);
                } else if (statusCode.ordinal() == StatusCode.OFFLINE.ordinal()) {
                    // Offline is orange.
                    label.setBackground(Color.ORANGE);
                } else if (statusCode.ordinal() == StatusCode.UNKNOWN.ordinal()) {
                    // Unknown is gray.
                    label.setBackground(Color.GRAY);
                } else {
                    // Default is white, though this shouldn't ever happen!
                    label.setBackground(Color.WHITE);
                }
                return label;
            }
        });
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
        table.getColumnModel().getColumn(ACTIVE_COL).setPreferredWidth(8);
        table.getColumnModel().getColumn(STATUS_COL).setPreferredWidth(10);
        table.getColumnModel().getColumn(SYSTEM_COL).setPreferredWidth(10);
        // TODO: Add widths for every column.

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setOpaque(true);

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
}