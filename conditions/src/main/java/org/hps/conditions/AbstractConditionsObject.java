package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * The abstract implementation of {@link ConditionsObject}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// FIXME: Database query methods need to be rewritten to use QueryBuilder (which itself needs to be written).
public abstract class AbstractConditionsObject implements ConditionsObject {

    private ConnectionManager _connectionManager = null;
    private ConditionsTableMetaData _tableMetaData = null;
    protected int _rowId = -1;
    protected int _collectionId = -1;
    protected boolean _isDirty = false;
    protected boolean _isReadOnly = false;
    protected FieldValueMap _fieldValues = null;
    
    /**
     * Class that maps field names to values.
     */
    public static final class FieldValueMap extends LinkedHashMap<String, Object> {
    }      
    
    /**
     * Constructor for sub-classing with no arguments.
     */
    protected AbstractConditionsObject() {}
    
    /**
     * Constructor for a new object which cannot initially be read only as it must be inserted.
     * @param tableMetaData
     * @param fieldValues
     * @param collectionId
     * @param isReadOnly
     */
    // FIXME: This can maybe be removed.
    public AbstractConditionsObject(
            ConnectionManager connectionManager,
            ConditionsTableMetaData tableMetaData,  
            int collectionId,
            FieldValueMap fieldValues) {
        
        if (connectionManager == null) {
            throw new IllegalArgumentException("The connectionManager is null.");
        }
        if (tableMetaData == null) {
            throw new IllegalArgumentException("The tableMetaData is null");
        }
        if (collectionId <= 0) {
            throw new IllegalArgumentException("The set ID value is invalid: " + collectionId);
        }        
        _connectionManager = connectionManager;
        _tableMetaData = tableMetaData;
        _collectionId = collectionId;
        _rowId = -1;
        if (fieldValues != null) {
            _fieldValues = fieldValues;
        } else {
            _fieldValues = new FieldValueMap();
        }
    }
    
    /**
     * Constructor for loading data from an existing object with a row ID.
     * @param tableMetaData
     * @param rowId
     * @param isReadOnly
     */
    // FIXME: This can maybe be removed.
    public AbstractConditionsObject(
            ConnectionManager connectionManager,
            ConditionsTableMetaData tableMetaData,
            int rowId,
            boolean isReadOnly) {
        if (connectionManager == null) {
            throw new IllegalArgumentException("The connectionManager cannot be null!");
        }
        if (tableMetaData == null) {
            throw new IllegalArgumentException("The tableMetaData cannot be null");
        }
        if (rowId <= 0) {
            throw new IllegalArgumentException("Invalid row ID: " + rowId);
        }
        _connectionManager = connectionManager;
        _tableMetaData = tableMetaData;
        _rowId = rowId;
        _isReadOnly = isReadOnly;
        _fieldValues = new FieldValueMap();
    }
    
    public ConditionsTableMetaData getTableMetaData() {
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
        String query = "DELETE FROM " + _tableMetaData.getTableName() + " WHERE id = " + _rowId;
        _connectionManager.update(query);
        _rowId = -1;
    }
    
    public void insert() throws ConditionsObjectException, SQLException {
        if (!isNew())
            throw new ConditionsObjectException("Record already exists in database.");        
        if (isReadOnly())
            throw new ConditionsObjectException("This object is set to read only mode.");        
        if (_fieldValues.size() == 0) 
            throw new ConditionsObjectException("There are no field values to insert.");             
        if (!hasValidCollection())
            throw new ConditionsObjectException("The object's collection ID is not valid");
        StringBuffer buff = new StringBuffer();
        buff.append("INSERT INTO " + _tableMetaData.getTableName() + "( set_id");
        for (Entry<String, Object> entry : _fieldValues.entrySet()) {
            buff.append(", " + entry.getKey());
        }
        buff.append(" ) VALUES ( ");
        buff.append(_collectionId);
        for (Entry<String, Object> entry : _fieldValues.entrySet()) {
            buff.append(", " + entry.getValue());
        } 
        buff.append(") ");       
        int key = _connectionManager.update(buff.toString());        
        _rowId = key;        
    }

    public void select() throws ConditionsObjectException, SQLException {
        if (isNew()) {
            throw new ConditionsObjectException("Record has not been inserted into database yet.");
        } 
        StringBuffer buff = new StringBuffer();
        buff.append("SELECT ");
        for (String fieldName : _tableMetaData.getFieldNames()) {
            buff.append(fieldName + ", ");
        }
        buff.delete(buff.length()-2, buff.length()-1);
        buff.append(" FROM " + _tableMetaData.getTableName());        
        buff.append(" WHERE id = " + _rowId);
        ResultSet resultSet = _connectionManager.query(buff.toString());        
        ResultSetMetaData metadata = resultSet.getMetaData();
        int ncolumns = metadata.getColumnCount();
        if (resultSet.next()) {        
            for (int i=1; i<=ncolumns; i++) {
                _fieldValues.put(metadata.getColumnName(i), resultSet.getObject(i));
            }
        }        
    }
        
    public void update() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("This object is set to read only.");
        }
        if (isNew()) {
            throw new ConditionsObjectException("Cannot call update on new record.");
        }
        if (_fieldValues.size() == 0) {
            throw new ConditionsObjectException("No field values to update.");
        }
        StringBuffer buff = new StringBuffer();
        buff.append("UPDATE " + _tableMetaData.getTableName() + " SET ");
        for (Entry<String, Object> entry : _fieldValues.entrySet()) {
            buff.append(entry.getKey() + " = '" + entry.getValue() + "', ");
        }
        buff.delete(buff.length()-2, buff.length()-1);
        buff.append(" WHERE id = " + _rowId); 
        _connectionManager.update(buff.toString());
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

    public void setConnectionManager(ConnectionManager connectionManager) throws ConditionsObjectException {
        if (_connectionManager != null)
            throw new ConditionsObjectException("The connection manager cannot be reset.");
        _connectionManager = connectionManager;        
    }

    public void setTableMetaData(ConditionsTableMetaData tableMetaData) throws ConditionsObjectException {
        if (_tableMetaData != null) 
            throw new ConditionsObjectException("The table meta data cannot be reset.");
        _tableMetaData = tableMetaData;
    }

    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (_collectionId != -1)
            throw new ConditionsObjectException("The collection ID cannot be reassigned.");
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
            throw new ConditionsObjectException("The row ID cannot be reassigned.");
        _rowId = rowId;
    }
    
    private boolean hasValidCollection() {
        return _collectionId != -1;
    }
}