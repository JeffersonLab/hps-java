package org.hps.monitoring.application.model;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.table.AbstractTableModel;

import org.hps.monitoring.subsys.StatusCode;
import org.hps.monitoring.subsys.SystemStatus;
import org.hps.monitoring.subsys.SystemStatusListener;

/**
 * A <code>JTableModel</code> that has a list of {@link org.hps.monitoring.subsys.SystemStatus} objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
public final class SystemStatusTableModel extends AbstractTableModel implements SystemStatusListener {

    /**
     * Active field column index.
     */
    public static final int ACTIVE_COLUMN_INDEX = 1;

    /**
     * Clearable field column index.
     */
    public static final int CLEARABLE_COLUMN_INDEX = 7;

    /**
     * The names of the columns.
     */
    private static final String[] COLUMN_NAMES = {"Reset", "Active", "Status", "System", "Description", "Message",
            "Last Changed", "Clearable"};

    /**
     * Description column index.
     */
    public static final int DESCRIPTION_COLUMN_INDEX = 4;

    /**
     * Last changed field column index.
     */
    public static final int LAST_CHANGED_COLUMN_INDEX = 6;

    /**
     * Message field column index.
     */
    public static final int MESSAGE_COLUMN_INDEX = 5;

    /**
     * Reset field column index.
     */
    public static final int RESET_COLUMN_INDEX = 0;

    /**
     * Status field column index.
     */
    public static final int STATUS_COLUMN_INDEX = 2;

    /**
     * System field column index.
     */
    public static final int SYSTEM_COLUMN_INDEX = 3;

    /**
     * The list of system status objects that back the model.
     */
    private final List<SystemStatus> statuses = new ArrayList<SystemStatus>();

    /**
     * Add a system status to the model.
     *
     * @param status the system status to add to the model
     */
    public void addSystemStatus(final SystemStatus status) {
        this.statuses.add(status);
        status.addListener(this);
        fireTableDataChanged();
    }

    /**
     * Clear all the data in the model.
     */
    public void clear() {
        this.statuses.clear();
        fireTableDataChanged();
    }

    /**
     * Get the class of the column.
     *
     * @param columnIndex the index of the column
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override
    public Class getColumnClass(final int columnIndex) {
        switch (columnIndex) {
        case ACTIVE_COLUMN_INDEX:
            return Boolean.class;
        case LAST_CHANGED_COLUMN_INDEX:
            return Date.class;
        default:
            return String.class;
        }
    }

    /**
     * Get the column count.
     *
     * @return the column count
     */
    @Override
    public int getColumnCount() {
        return COLUMN_NAMES.length;
    }

    /**
     * Get the column name.
     *
     * @param columnIndex the column index
     */
    @Override
    public String getColumnName(final int columnIndex) {
        return COLUMN_NAMES[columnIndex];
    }

    /**
     * Get the row count.
     *
     * @return the row count
     */
    @Override
    public int getRowCount() {
        return this.statuses.size();
    }

    /**
     * Get a table cell value.
     *
     * @param rowIndex the row index
     * @param columnIndex the column index
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final SystemStatus status = this.statuses.get(rowIndex);
        switch (columnIndex) {
        case ACTIVE_COLUMN_INDEX:
            return status.isActive();
        case STATUS_COLUMN_INDEX:
            return status.getStatusCode().name();
        case SYSTEM_COLUMN_INDEX:
            return status.getSubsystem().name();
        case DESCRIPTION_COLUMN_INDEX:
            return status.getDescription();
        case MESSAGE_COLUMN_INDEX:
            return status.getMessage();
        case LAST_CHANGED_COLUMN_INDEX:
            return new Date(status.getLastChangedMillis());
        case RESET_COLUMN_INDEX:
            // If the status is clear-able, then the cell has a button which can be used to
            // manually set the state to CLEARED. If the status cannot be cleared,
            // then nothing is rendered in this cell.
            if (status.isClearable()) {
                final JButton button = new JButton();
                button.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(final ActionEvent e) {
                        final SystemStatus status = SystemStatusTableModel.this.statuses.get(rowIndex);
                        if (status.isClearable()) {
                            final StatusCode oldStatusCode = status.getStatusCode();
                            status.setStatus(StatusCode.CLEARED, "Cleared from " + oldStatusCode.name() + " state.");
                        }
                    }
                });
                return button;
            } else {
                return null;
            }
        case CLEARABLE_COLUMN_INDEX:
            return status.isClearable();
        default:
            return null;
        }
    }

    /**
     * Return <code>true</code> if cell is editable.
     *
     * @return <code>true</code> if cell is editable
     */
    @Override
    public boolean isCellEditable(final int rowIndex, final int columnIndex) {
        return columnIndex == ACTIVE_COLUMN_INDEX;
    }

    /**
     * Set the table cell value.
     *
     * @param value the new value
     * @param rowIndex the row index
     * @param columnIndex the column index
     */
    @Override
    public void setValueAt(final Object value, final int rowIndex, final int columnIndex) {
        if (columnIndex == ACTIVE_COLUMN_INDEX) {
            this.statuses.get(rowIndex).setActive((Boolean) value);
        }
    }

    /**
     * Notify of system status changed.
     *
     * @param status the system status that changed
     */
    @Override
    public void statusChanged(final SystemStatus status) {
        final int rowNumber = this.statuses.indexOf(status);
        fireTableRowsUpdated(rowNumber, rowNumber);
    }
}
