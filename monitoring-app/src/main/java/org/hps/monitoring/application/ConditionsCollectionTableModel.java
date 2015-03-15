package org.hps.monitoring.application;

import javax.swing.table.DefaultTableModel;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This is a table model for a collection of conditions objects.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
class ConditionsCollectionTableModel extends DefaultTableModel {

    ConditionsObjectCollection<?> collection;
    int columnCount;
    int rowCount;
    String[] columnNames;
    Class<?>[] columnTypes;
    DatabaseConditionsManager manager;
    
    ConditionsCollectionTableModel(DatabaseConditionsManager manager, ConditionsObjectCollection<?> collection) {
        
        // Set collection data.
        this.collection = collection;
        rowCount = this.collection.size();
        
        String tableName = collection.getConditionsRecord().getTableName();                
        TableMetaData tableInfo = manager.findTableMetaData(tableName);

        // Set column names and count from table meta data.
        setupColumns(tableInfo);        
    }

    private void setupColumns(TableMetaData tableInfo) {

        int fieldNameCount = tableInfo.getFieldNames().length;
        columnCount = fieldNameCount + 1;
        
        columnTypes = new Class<?>[columnCount];                
        columnNames = new String[columnCount];
        
        columnNames[0] = "id";
        columnTypes[0] = int.class;
        
        for (int i = 0; i < fieldNameCount; i++) {
            String fieldName = tableInfo.getFieldNames()[i];
            columnNames[i + 1] = fieldName;
            columnTypes[i + 1] = tableInfo.getFieldType(fieldName);
        }
    }

    @Override
    public int getRowCount() {
        return rowCount;
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames[columnIndex];
    }
    
    @Override
    public Class<?> getColumnClass(int columnIndex) {
        Class<?> columnClass = columnTypes[columnIndex];
        if (columnClass.equals(int.class)) {
            return Integer.class;
        } else if (columnClass.equals(float.class)) {
            return Float.class;
        } else if (columnClass.equals(double.class)) {
            return Double.class;
        } else {
            return columnClass;
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        ConditionsObject object = collection.get(rowIndex);
        if (columnIndex == 0) {
            return object.getRowId();
        } else {
            return object.getFieldValue(columnNames[columnIndex]);
        }
    }
}