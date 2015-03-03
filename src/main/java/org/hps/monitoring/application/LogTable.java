package org.hps.monitoring.application;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.LogRecord;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * This is a simple Swing component to model the log table.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class LogTable extends JTable {
    
    private DefaultTableModel model;
    static final String[] logTableColumns = { "Date", "Level", "Message" };

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMMM-dd-yyyy HH:mm:ss.SSS");
    
    LogTable() {
        String data[][] = new String[0][0];
        model = new DefaultTableModel(data, logTableColumns);
        this.setModel(model);
        setEnabled(false);
        setAutoCreateRowSorter(true);
    }
 
    void clear() {
        model.setRowCount(0);
    }
    
    void insert(LogRecord record) {
        Object[] row = new Object[] { dateFormat.format(new Date(record.getMillis())), record.getLevel(), record.getMessage() };
        model.insertRow(getRowCount(), row);        
    }
}
