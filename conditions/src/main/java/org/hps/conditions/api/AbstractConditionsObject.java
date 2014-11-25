package org.hps.conditions.api;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.DatabaseUtilities;
import org.hps.conditions.database.QueryBuilder;
import org.hps.conditions.database.TableMetaData;

/**
 * The abstract implementation of {@link ConditionsObject}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractConditionsObject implements ConditionsObject {

    private TableMetaData tableMetaData = null;
    protected int rowId = -1;
    protected int collectionId = -1;
    protected FieldValueMap fieldValues;

    /**
     * Constructor for sub-classing.
     */
    protected AbstractConditionsObject() {
        fieldValues = new FieldValueMap();
    }

    public TableMetaData getTableMetaData() {
        return tableMetaData;
    }

    public int getRowId() {
        return rowId;
    }

    public int getCollectionId() {
        return collectionId;
    }

    public boolean isNew() {
        return rowId == -1;
    }

    public void delete() throws ConditionsObjectException {
        String query = QueryBuilder.buildDelete(tableMetaData.getTableName(), rowId);
        // TODO: Replace this with a method that takes a conditions object.
        DatabaseConditionsManager.getInstance().updateQuery(query);
        rowId = -1;
    }

    public void insert() throws ConditionsObjectException {
        if (fieldValues.size() == 0)
            throw new ConditionsObjectException("There are no field values to insert.");
        if (collectionId == -1)
            throw new ConditionsObjectException("The object's collection ID is not valid.");
        // TODO: Replace this with a method that takes a conditions object.
        String query = QueryBuilder.buildInsert(getTableMetaData().getTableName(), getCollectionId(), getTableMetaData().getFieldNames(), fieldValues.valuesToArray());
        System.out.println(query);
        List<Integer> keys = DatabaseConditionsManager.getInstance().updateQuery(query);
        if (keys.size() != 1) {
            throw new ConditionsObjectException("SQL insert returned wrong number of keys: " + keys.size());
        }
        rowId = keys.get(0);
    }

    public void select() throws ConditionsObjectException {
        if (isNew()) {
            throw new ConditionsObjectException("Record has not been inserted into the database yet.");
        }
        // TODO: Replace this with method that takes a conditions object.
        String query = QueryBuilder.buildSelect(getTableMetaData().getTableName(), collectionId, fieldValues.fieldsToArray(), "id ASC");
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        ResultSet resultSet = manager.selectQuery(query);
        try {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int ncolumns = metadata.getColumnCount();
            if (resultSet.next()) {
                for (int i = 1; i <= ncolumns; i++) {
                    fieldValues.put(metadata.getColumnName(i), resultSet.getObject(i));
                }
            }
        } catch (SQLException e) {
            throw new ConditionsObjectException(e.getMessage(), this);
        }
        DatabaseUtilities.close(resultSet);
        resultSet = null;
    }

    public void update() throws ConditionsObjectException {
        if (fieldValues.size() == 0) {
            throw new ConditionsObjectException("No field values to update.", this);
        }
        // TODO: Replace this with method that takes a conditions object.
        String query = QueryBuilder.buildUpdate(tableMetaData.getTableName(), rowId, fieldValues.fieldsToArray(), fieldValues.valuesToArray());
        DatabaseConditionsManager.getInstance().updateQuery(query);
    }

    public void setFieldValue(String key, Object value) {
        fieldValues.put(key, value);
    }

    public void setFieldValues(FieldValueMap fieldValues) {
        this.fieldValues = fieldValues;
    }

    public <T> T getFieldValue(Class<T> klass, String field) {
        return klass.cast(fieldValues.get(field));
    }
    
    public FieldValueMap getFieldValues() {
        return this.fieldValues;
    }

    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(String field) {
        return (T) fieldValues.get(field);
    }

    public void setTableMetaData(TableMetaData tableMetaData) throws ConditionsObjectException {
        if (this.tableMetaData != null)
            throw new ConditionsObjectException("The table meta data cannot be reset on an object.", this);
        this.tableMetaData = tableMetaData;
    }

    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (this.collectionId != -1)
            throw new ConditionsObjectException("The collection ID cannot be reassigned once set.", this);
        this.collectionId = collectionId;
    }

    public void setRowId(int rowId) throws ConditionsObjectException {
        if (!isNew()) {
            throw new ConditionsObjectException("The row ID cannot be reassigned on an existing object.");
        }
        this.rowId = rowId;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getRowId());
        sb.append('\t');
        for (String fieldName : getTableMetaData().getFieldNames()) {
            sb.append(getFieldValue(fieldName));
            sb.append('\t');
        }
        return sb.toString();
    }
}