package org.hps.monitoring.application;

import javax.swing.table.DefaultTableModel;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This is a table model for a collection of conditions objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
final class ConditionsCollectionTableModel extends DefaultTableModel {

    /**
     * The {@link org.hps.conditions.api.ConditionsObjectCollection} for the model.
     */
    private final ConditionsObjectCollection<?> collection;

    /**
     * The number of columns.
     */
    private int columnCount;

    /**
     * The column names.
     */
    private String[] columnNames;

    /**
     * The column classes.
     */
    private Class<?>[] columnTypes;

    /**
     * The row count.
     */
    private final int rowCount;

    /**
     * Class constructor.
     *
     * @param manager the global conditions manager instance
     * @param collection the {@link org.hps.conditions.api.ConditionsObjectCollection} providing data for the model
     */
    ConditionsCollectionTableModel(final DatabaseConditionsManager manager,
            final ConditionsObjectCollection<?> collection) {

        // Set collection data.
        this.collection = collection;
        this.rowCount = this.collection.size();

        final String tableName = collection.getConditionsRecord().getTableName();
        final TableMetaData tableInfo = manager.findTableMetaData(tableName);

        // Set column names and count from table meta data.
        this.setupColumns(tableInfo);
    }

    /**
     * Get the class of a column.
     *
     * @param columnIndex the index of the column
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        final Class<?> columnClass = this.columnTypes[columnIndex];
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

    /**
     * Get the number of columns.
     *
     * @return the number of columns
     */
    @Override
    public int getColumnCount() {
        return this.columnCount;
    }

    /**
     * Get the name of the column.
     *
     * @return the name of the column
     */
    @Override
    public String getColumnName(final int columnIndex) {
        return this.columnNames[columnIndex];
    }

    /**
     * Get the row count.
     *
     * @return the row count
     */
    @Override
    public int getRowCount() {
        return this.rowCount;
    }

    /**
     * Get a cell value.
     *
     * @param rowIndex the row index
     * @param columnIndex the column index
     */
    @Override
    public Object getValueAt(final int rowIndex, final int columnIndex) {
        final ConditionsObject object = this.collection.get(rowIndex);
        if (columnIndex == 0) {
            return object.getRowId();
        } else {
            return object.getFieldValue(this.columnNames[columnIndex]);
        }
    }

    /**
     * Setup the columns from table meta data.
     *
     * @param tableInfo the {@link org.hps.conditions.database.TableMetaData} with table info
     */
    private void setupColumns(final TableMetaData tableInfo) {

        final int fieldNameCount = tableInfo.getFieldNames().length;
        this.columnCount = fieldNameCount + 1;

        this.columnTypes = new Class<?>[this.columnCount];
        this.columnNames = new String[this.columnCount];

        this.columnNames[0] = "id";
        this.columnTypes[0] = int.class;

        for (int i = 0; i < fieldNameCount; i++) {
            final String fieldName = tableInfo.getFieldNames()[i];
            this.columnNames[i + 1] = fieldName;
            this.columnTypes[i + 1] = tableInfo.getFieldType(fieldName);
        }
    }
}
