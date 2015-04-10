package org.hps.monitoring.application;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import javax.swing.JTable;
import javax.swing.RowFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;

import org.hps.monitoring.application.model.ConfigurationModel;

/**
 * This is a simple {@link avax.swing.JTable} component for displaying log messages.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class LogTable extends JTable implements PropertyChangeListener {

    /**
     * The table cell renderer for displaying dates.
     */
    static class DateRenderer extends DefaultTableCellRenderer {
        @Override
        public void setValue(final Object value) {
            setText(value == null ? "" : DATE_FORMAT.format(value));
        }
    }

    /**
     * A filter which determines what level of messages to display in the table.
     */
    private class LevelFilter extends RowFilter<LogRecordModel, Integer> {

        /**
         * Return <code>true</code> to display the entry.
         *
         * @param entry the table entry (model with a row ID)
         */
        @Override
        public boolean include(final Entry<? extends LogRecordModel, ? extends Integer> entry) {
            final LogRecordModel model = entry.getModel();
            final LogRecord record = model.get(entry.getIdentifier());
            if (record.getLevel().intValue() >= LogTable.this.filterLevel.intValue()) {
                return true;
            }
            return false;
        }
    }

    /**
     * The table model implementation.
     */
    static class LogRecordModel extends AbstractTableModel {

        /**
         * The list of {@link java.util.logging.LogRecord} objects to display in the table.
         */
        private final List<LogRecord> records = new ArrayList<LogRecord>();

        /**
         * Add a new {@link java.util.logging.LogRecord} object to the table.
         *
         * @param record the new {@link java.util.logging.LogRecord} object
         */
        void add(final LogRecord record) {
            this.records.add(record);
            fireTableDataChanged();
        }

        /**
         * Clear all the records from the table.
         */
        void clear() {
            this.records.clear();
            fireTableDataChanged();
        }

        /**
         * Get a record by its index.
         *
         * @param rowIndex the row index
         * @return the {@link java.util.logging.LogRecord} object
         * @throws IndexOutOfBoundsException if rowIndex is invalid
         */
        private LogRecord get(final Integer rowIndex) {
            return this.records.get(rowIndex);
        }

        /**
         * Get the class of a column.
         *
         * @param columnIndex the column's index
         * @return the column's class
         */
        @Override
        public Class<?> getColumnClass(final int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Date.class;
            case 1:
                return Level.class;
            case 2:
                return String.class;
            default:
                return Object.class;
            }
        }

        /**
         * Get the number of columns.
         *
         * @return the number of columns
         */
        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        /**
         * Get the column name.
         *
         * @param columnIndex the column index
         * @return the name of the column
         */
        @Override
        public String getColumnName(final int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }

        /**
         * Get the number of rows.
         *
         * @return the number of rows
         */
        @Override
        public int getRowCount() {
            return this.records.size();
        }

        /**
         * Get a cell value from the table.
         *
         * @param rowIndex the row index
         * @param column the column index
         * @return the cell value or <code>null</code> if does not exist (e.g. invalid column number)
         */
        @Override
        public Object getValueAt(final int rowIndex, final int columnIndex) {
            final LogRecord record = this.get(rowIndex);
            switch (columnIndex) {
            case 0:
                return new Date(record.getMillis());
            case 1:
                return record.getLevel();
            case 2:
                return record.getMessage();
            default:
                return null;
            }
        }
    }

    /**
     * The column names.
     */
    static final String[] COLUMN_NAMES = { "Date", "Level", "Message" };

    /**
     * Date formatting.
     */
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * The current filtering level.
     */
    private Level filterLevel = Level.ALL;

    /**
     * The backing table model.
     */
    private final LogRecordModel model;

    /**
     * The table sorer.
     */
    private final TableRowSorter<LogRecordModel> sorter;

    /**
     * Class constructor.
     *
     * @param configurationModel the {@link org.hps.monitoring.application.model.ConfigurationModel} for the application
     */
    LogTable(final ConfigurationModel configurationModel) {
        configurationModel.addPropertyChangeListener(this);
        this.model = new LogRecordModel();
        setModel(this.model);
        this.sorter = new TableRowSorter<LogRecordModel>(this.model);
        this.sorter.setRowFilter(new LevelFilter());
        getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
        setRowSorter(this.sorter);
        getColumnModel().getColumn(0).setPreferredWidth(142);
        getColumnModel().getColumn(0).setMaxWidth(142);
        getColumnModel().getColumn(1).setPreferredWidth(60);
        getColumnModel().getColumn(1).setMaxWidth(60);
        setEnabled(false);
    }

    /**
     * Get the table model.
     *
     * @return the table model
     */
    LogRecordModel getLogRecordModel() {
        return this.model;
    }

    /**
     * Get change in log level filtering from the configuration model.
     */
    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        if (event.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_FILTER_PROPERTY)) {
            this.filterLevel = (Level) event.getNewValue();
            this.model.fireTableDataChanged();
        }
    }
}
