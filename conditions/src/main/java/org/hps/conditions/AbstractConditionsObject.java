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

    private TableMetaData _tableMetaData = null;
    protected int _rowId = -1;
    protected int _collectionId = -1;
    protected boolean _isDirty = false;
    protected boolean _isReadOnly = false;
    protected FieldValueMap _fieldValues;
        
    /**
     * Constructor for sub-classing.
     */
    protected AbstractConditionsObject() {     
        _fieldValues = new FieldValueMap();
    }
            
    public TableMetaData getTableMetaData() {
        return _tableMetaData;
    }
    
    public int getRowId() {
        return _rowId;
    }

    public int getCollectionId() {
        return _collectionId;
    }
    
    public boolean isReadOnly() {
        return _isReadOnly;
    }

    public boolean isNew() {
        return _rowId == -1;
    }

    public boolean isDirty() {
        return _isDirty;
    }
    
    public void delete() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("This object is set to read only.");
        }
        if (isNew()) {
            throw new ConditionsObjectException("This object is not in the database and so cannot be deleted.");
        }
        String query = QueryBuilder.buildDelete(_tableMetaData.getTableName(), _rowId);
        DatabaseConditionsManager.getInstance().update(query);
        _rowId = -1;
    }
    
    public void insert() throws ConditionsObjectException {
        if (!isNew())
            throw new ConditionsObjectException("Record already exists in database and cannot be inserted.");
        if (isReadOnly())
            throw new ConditionsObjectException("This object is set to read only mode.");
        if (_fieldValues.size() == 0) 
            throw new ConditionsObjectException("There are no field values to insert.");
        if (!hasValidCollection())
            throw new ConditionsObjectException("The object's collection ID is not valid.");
        String query = QueryBuilder.buildInsert(getTableMetaData().getTableName(), 
                getCollectionId(),
                getTableMetaData().getFieldNames(),
                _fieldValues.valuesToArray());
        List<Integer> keys = DatabaseConditionsManager.getInstance().update(query);
        if (keys.size() == 0 || keys.size() > 1) {
            throw new ConditionsObjectException("SQL insert returned wrong number of keys: " + keys.size());
        }
        _rowId = keys.get(0);
    }

    public void select() throws ConditionsObjectException {
        if (isNew()) {
            throw new ConditionsObjectException("Record has not been inserted into the database yet.");
        } 
        String query = QueryBuilder.buildSelect(
                getTableMetaData().getTableName(), _collectionId, _fieldValues.fieldsToArray(), "id ASC");
        DatabaseConditionsManager manager = DatabaseConditionsManager.getInstance();
        ResultSet resultSet = manager.query(query);  
        try {
            ResultSetMetaData metadata = resultSet.getMetaData();
            int ncolumns = metadata.getColumnCount();
            if (resultSet.next()) {        
                for (int i=1; i<=ncolumns; i++) {
                    _fieldValues.put(metadata.getColumnName(i), resultSet.getObject(i));
                }
            }    
        } catch (SQLException e) {
            throw new ConditionsObjectException(e.getMessage(), this);
        }
        DatabaseConditionsManager.close(resultSet);
    }
        
    public void update() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("This object is set to read only.", this);
        }
        if (isNew()) {
            throw new ConditionsObjectException("Cannot call update on a new record.", this);
        }
        if (_fieldValues.size() == 0) {
            throw new ConditionsObjectException("No field values to update.", this);
        }
        String query = QueryBuilder.buildUpdate(
                _tableMetaData.getTableName(), 
                _rowId, 
                _fieldValues.fieldsToArray(), 
                _fieldValues.valuesToArray());
        DatabaseConditionsManager.getInstance().update(query);
        setIsDirty(false);
    }
    
    public void setFieldValue(String key, Object value) {
        _fieldValues.put(key, value);
        setIsDirty(true);
    }
    
    public void setFieldValues(FieldValueMap fieldValues) {
        _fieldValues = fieldValues;
        if (!isNew()) {
            setIsDirty(true);
        }        
    }
    
    public <T> T getFieldValue(Class<T> klass, String field) {
        return klass.cast(_fieldValues.get(field)); 
    }
    
    @SuppressWarnings("unchecked")
    public <T> T getFieldValue(String field) {
        return (T)_fieldValues.get(field);
    }

    public void setTableMetaData(TableMetaData tableMetaData) throws ConditionsObjectException {
        if (_tableMetaData != null) 
            throw new ConditionsObjectException("The table meta data cannot be reset on an object.", this);
        _tableMetaData = tableMetaData;
    }

    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (_collectionId != -1)
            throw new ConditionsObjectException("The collection ID cannot be reassigned once set.", this);
        _collectionId = collectionId;
    }
        
    public void setIsDirty(boolean isDirty) {
        _isDirty = isDirty;
    }
    
    public void setIsReadOnly() {
        _isReadOnly = true;
    }
    
    public void setRowId(int rowId) throws ConditionsObjectException {
        if (_rowId != -1)
            throw new ConditionsObjectException("The row ID cannot be reassigned on an existing object.");
        _rowId = rowId;
    }
    
    private boolean hasValidCollection() {
        return _collectionId != -1;
    }    
}