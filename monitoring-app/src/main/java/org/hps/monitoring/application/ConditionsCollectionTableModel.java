package org.hps.monitoring.application;

import javax.swing.table.DefaultTableModel;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.TableMetaData;
import org.hps.conditions.database.DatabaseConditionsManager;

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

        // Set column names and count from table meta data.
        this.setupColumns(collection.getTableMetaData());
    }

    /**
     * Get the class of a column.
     *
     * @param columnIndex the index of the column
     */
    @Override
    public Class<?> getColumnClass(final int columnIndex) {
        final Class<?> columnClass = this.columnTypes[columnIndex];
        if (int.class.equals(columnClass)) {
            return Integer.class;
        } else if (float.class.equals(columnClass)) {
            return Float.class;
        } else if (double.class.equals(columnClass)) {
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
     * @param columnIndex the column index
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
     * Get a table cell value.
     *
     * @param rowIndex the row index
     * @param columnIndex the column index
     * @return the value of the cell
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
     * @param tableInfo the {@link org.hps.conditions.database.TableMetaData} with the table info
     */
    private void setupColumns(final TableMetaData tableInfo) {

        final int fieldNameCount = tableInfo.getFieldNames().size();
        this.columnCount = fieldNameCount + 1;

        this.columnTypes = new Class<?>[this.columnCount];
        this.columnNames = new String[this.columnCount];

        this.columnNames[0] = "id";
        this.columnTypes[0] = int.class;

        int columnNumber = 1;
        for (String fieldName : tableInfo.getFieldNames()) {
            this.columnNames[columnNumber + 1] = fieldName;
            this.columnTypes[columnNumber + 1] = tableInfo.getFieldType(fieldName);
        }
    }
}
