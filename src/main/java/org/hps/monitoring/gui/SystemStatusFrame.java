package org.hps.monitoring.gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatus.StatusCode;
import org.hps.monitoring.subsys.SystemStatusListener;

/**
 * A GUI window for showing changes to {@link org.hps.monitoring.subsys.SystemStatus} objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add custom table model.
public class SystemStatusFrame extends JFrame implements SystemStatusListener, TableModelListener, HasErrorHandler {

    private static final int ACTIVE_COL = 0;
    private static final int STATUS_COL = 1;
    private static final int SYSTEM_COL = 2;
    private static final int DESCRIPTION_COL = 3;
    private static final int MESSAGE_COL = 4;
    private static final int LAST_CHANGED_COL = 5;
            
    String[] columnNames = {
            "Active",
            "Status",
            "System",       
            "Description",
            "Message", 
            "Last Changed"
    };
           
    JTable table;
    DefaultTableModel tableModel;
    List<SystemStatus> systemStatuses = new ArrayList<SystemStatus>();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");
    ErrorHandler errorHandler;
    
    int WIDTH = 650;
    int HEIGHT = ScreenUtil.getScreenHeight() / 2;
    
    SystemStatusFrame() {
         
        String data[][] = new String[0][0];
        tableModel = new DefaultTableModel(data, columnNames) {
                public Class getColumnClass(int column) {
                    switch (column) {
                        case ACTIVE_COL:
                            return Boolean.class;
                        default:
                            return String.class;
                    }                    
                }
                
                public boolean isCellEditable(int row, int col) {
                    if (col == ACTIVE_COL)
                        return true;
                    else 
                        return false;
                }
        };        
        tableModel.addTableModelListener(this);
        table = new JTable(tableModel);
        table.getColumnModel().getColumn(STATUS_COL).setCellRenderer(
                new DefaultTableCellRenderer() {                    
                    @Override
                    public Component getTableCellRendererComponent(
                            JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int col) {

                        // Cells are by default rendered as a JLabel.
                        JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);

                        // Color code the cell by its status.
                        SystemStatus.StatusCode statusCode = SystemStatus.StatusCode.valueOf((String) value);
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
                }
        );
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
    
    void addSystemStatus(SystemStatus status) {        
        
        // Insert row data into table.
        Object[] record = new Object[] {
                (Boolean)status.isActive(),
                status.getStatusCode().name(),
                status.getSystemName(),                
                status.getStatusCode().description(),
                status.getMessage(),
                new Date(status.getLastChangedMillis())
        };
        tableModel.insertRow(table.getRowCount(), record);
        
        // Add to list for easily finding its row number later.
        systemStatuses.add(status);
        
        // Add this class as a listener to get notification of status changes.
        status.addListener(this);
    }

    @Override
    public void statusChanged(SystemStatus status) {
        int rowNumber = systemStatuses.indexOf(status);
        tableModel.setValueAt(status.isActive(), rowNumber, ACTIVE_COL);
        tableModel.setValueAt(status.getStatusCode().name(), rowNumber, STATUS_COL);
        tableModel.setValueAt(status.getSystemName(), rowNumber, SYSTEM_COL);        
        tableModel.setValueAt(status.getStatusCode().description(), rowNumber, DESCRIPTION_COL);
        tableModel.setValueAt(status.getMessage(), rowNumber, MESSAGE_COL);
        tableModel.setValueAt(dateFormat.format(new Date(status.getLastChangedMillis())), rowNumber, LAST_CHANGED_COL);
    }
    
    public void clear() {
        tableModel.setNumRows(0);
        systemStatuses.clear();
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        int col = e.getColumn();
        if (col == ACTIVE_COL) {
            int row = e.getFirstRow();
            SystemStatus status = this.systemStatuses.get(row);
            boolean active = (Boolean)tableModel.getValueAt(row, col);
            status.setActive(active);
        }
    }

    @Override
    public void setErrorHandler(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
    }

    @Override
    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }
}
