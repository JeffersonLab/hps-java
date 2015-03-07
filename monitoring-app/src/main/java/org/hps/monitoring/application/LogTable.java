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
 * This is a simple Swing component for the table of log messages.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
class LogTable extends JTable implements PropertyChangeListener {
    
    static final String[] COLUMN_NAMES = { "Date", "Level", "Message" };
    
    LogRecordModel model;
    TableRowSorter<LogRecordModel> sorter;

    Level filterLevel = Level.ALL;
    
    final static SimpleDateFormat formatter = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");
            
    LogTable(ConfigurationModel configurationModel) {
        configurationModel.addPropertyChangeListener(this);
        model = new LogRecordModel();
        setModel(model);
        sorter = new TableRowSorter<LogRecordModel>(model);
        sorter.setRowFilter(new LevelFilter());
        this.getColumnModel().getColumn(0).setCellRenderer(new DateRenderer());
        setRowSorter(sorter);
        setEnabled(false);
    }
        
    static class DateRenderer extends DefaultTableCellRenderer {
        public void setValue(Object value) {
            setText((value == null) ? "" : formatter.format(value));
        }
    }       

    class LevelFilter extends RowFilter<LogRecordModel, Integer> {

        public boolean include(Entry<? extends LogRecordModel, ? extends Integer> entry) {
            LogRecordModel model = entry.getModel();
            LogRecord record = model.get(entry.getIdentifier());
            if (record.getLevel().intValue() >= filterLevel.intValue()) {
                return true;
            }
            return false;
        }
    }
   
    static class LogRecordModel extends AbstractTableModel {        
        
        List<LogRecord> records = new ArrayList<LogRecord>();

        LogRecord get(Integer rowIndex) {
            return records.get(rowIndex);
        }

        void add(LogRecord record) {
            records.add(record);
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return records.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            LogRecord record = records.get(rowIndex);
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

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Date.class;
            case 1:
                return String.class;
            case 2:
                return String.class;
            default:
                return Object.class;
            }
        }
        
        @Override
        public String getColumnName(int columnIndex) {
            return COLUMN_NAMES[columnIndex];
        }
        
        void clear() {
            records.clear();
            fireTableDataChanged();
        }
    }

    /**
     * Get change in log level filtering from the configuration model.
     */
    public void propertyChange(PropertyChangeEvent event) {
        if (event.getPropertyName().equals(ConfigurationModel.LOG_LEVEL_FILTER_PROPERTY)) {
            filterLevel = (Level) event.getNewValue();
            model.fireTableDataChanged();
        }
    }
}
