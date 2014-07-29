package org.hps.monitoring.gui;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;

/**
 * A <code>JTableModel</code> that has a backing list of 
 * {@link org.hps.monitoring.subsys.SystemStatus} objects.
 */
public class SystemStatusTableModel extends AbstractTableModel implements SystemStatusListener {

    static final int ACTIVE_COL = 0;
    static final int STATUS_COL = 1;
    static final int SYSTEM_COL = 2;
    static final int DESCRIPTION_COL = 3;
    static final int MESSAGE_COL = 4;
    static final int LAST_CHANGED_COL = 5;
            
    static final String[] columnNames = {
            "Active",
            "Status",
            "System",       
            "Description",
            "Message", 
            "Last Changed"
    };
    
    List<SystemStatus> statuses = new ArrayList<SystemStatus>();    
    final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");
    
    public void addSystemStatus(SystemStatus status) {
        statuses.add(status);
        status.addListener(this);
        fireTableDataChanged();
    }
        
    @Override
    public int getRowCount() {
        return statuses.size();
    }

    @Override
    public int getColumnCount() {
        return columnNames.length;
    }
    
    @Override
    public String getColumnName(int col) {        
        return columnNames[col];
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        SystemStatus status = statuses.get(rowIndex);
        switch (columnIndex) {
            case ACTIVE_COL:
                return status.isActive();
            case STATUS_COL:
                return status.getStatusCode().name();
            case SYSTEM_COL:
                return status.getSubsystem().name();
            case DESCRIPTION_COL:
                return status.getDescription();
            case MESSAGE_COL:
                return status.getMessage();
            case LAST_CHANGED_COL:
                return new Date(status.getLastChangedMillis());
            default:
                return null;
        }
    }
    
    @Override
    public Class getColumnClass(int column) {
        switch (column) {
            case ACTIVE_COL:
                return Boolean.class;
            case LAST_CHANGED_COL:
                return Date.class;
            default:
                return String.class;
        }                    
    }
    
    @Override
    public boolean isCellEditable(int row, int col) {
        if (col == ACTIVE_COL)
            return true;
        else 
            return false;
    }
    
    @Override
    public void statusChanged(SystemStatus status) {
        int rowNumber = statuses.indexOf(status);
        this.fireTableRowsUpdated(rowNumber, rowNumber);
    }
    
    public void clear() {
        statuses.clear();
        fireTableDataChanged();
    }    
    
    @Override
    public void setValueAt(Object value, int row, int col) {
        if (col == ACTIVE_COL) {
            statuses.get(row).setActive((Boolean) value);
        }
    }
    
}
