package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

/**
 * The abstract implementation of {@link ConditionsObject}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractConditionsObject implements ConditionsObject {

    private TableMetaData tableMetaData = null;
    protected int rowId = -1;
    protected int collectionId = -1;
    protected boolean isDirty = false;
    protected boolean isReadOnly = false;
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

    public boolean isReadOnly() {
        return isReadOnly;
    }

    public boolean isNew() {
        return rowId == -1;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void delete() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("This object is set to read only.");
        }
        if (isNew()) {
            throw new ConditionsObjectException("This object is not in the database and so cannot be deleted.");
        }
        String query = QueryBuilder.buildDelete(tableMetaData.getTableName(), rowId);
        DatabaseConditionsManager.getInstance().updateQuery(query);
        rowId = -1;
    }

    public void insert() throws ConditionsObjectException {
        if (!isNew())
            throw new ConditionsObjectException("Record already exists in database and cannot be inserted.");
        if (isReadOnly())
            throw new ConditionsObjectException("This object is set to read only mode.");
        if (fieldValues.size() == 0)
            throw new ConditionsObjectException("There are no field values to insert.");
        if (!hasValidCollection())
            throw new ConditionsObjectException("The object's collection ID is not valid.");
        String query = QueryBuilder.buildInsert(getTableMetaData().getTableName(), getCollectionId(), getTableMetaData().getFieldNames(), fieldValues.valuesToArray());
        List<Integer> keys = DatabaseConditionsManager.getInstance().updateQuery(query);
        if (keys.size() == 0 || keys.size() > 1) {
            throw new ConditionsObjectException("SQL insert returned wrong number of keys: " + keys.size());
        }
        rowId = keys.get(0);
    }

    public void select() throws ConditionsObjectException {
        if (isNew()) {
            throw new ConditionsObjectException("Record has not been inserted into the database yet.");
        }
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
        DatabaseConditionsManager.close(resultSet);
        resultSet = null;
    }

    public void update() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("This object is set to read only.", this);
        }
        if (isNew()) {
            throw new ConditionsObjectException("Cannot call update on a new record.", this);
        }
        if (fieldValues.size() == 0) {
            throw new ConditionsObjectException("No field values to update.", this);
        }
        String query = QueryBuilder.buildUpdate(tableMetaData.getTableName(), rowId, fieldValues.fieldsToArray(), fieldValues.valuesToArray());
        DatabaseConditionsManager.getInstance().updateQuery(query);
        setIsDirty(false);
    }

    public void setFieldValue(String key, Object value) {
        fieldValues.put(key, value);
        setIsDirty(true);
    }

    public void setFieldValues(FieldValueMap fieldValues) {
        this.fieldValues = fieldValues;
        if (!isNew()) {
            setIsDirty(true);
        }
    }

    public <T> T getFieldValue(Class<T> klass, String field) {
        return klass.cast(fieldValues.get(field));
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

    public void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    public void setIsReadOnly() {
        isReadOnly = true;
    }

    public void setRowId(int rowId) throws ConditionsObjectException {
        if (this.rowId != -1)
            throw new ConditionsObjectException("The row ID cannot be reassigned on an existing object.");
        this.rowId = rowId;
    }

    private boolean hasValidCollection() {
        return collectionId != -1;
    }

    // protected void finalize() {
    // System.out.println("finalizing ConditionsObject " + System.identityHashCode(this));
    // }
}