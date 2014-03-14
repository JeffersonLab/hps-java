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
public abstract class AbstractConditionsDatabaseObject implements ConditionsObject {

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
    protected AbstractConditionsDatabaseObject() {}
    
    /**
     * Constructor for a new object which cannot initially be read only as it must be inserted.
     * @param tableMetaData
     * @param fieldValues
     * @param setId
     * @param isReadOnly
     */
    public AbstractConditionsDatabaseObject(
            ConnectionManager connectionManager,
            ConditionsTableMetaData tableMetaData,  
            int setId,
            FieldValueMap fieldValues) {
        
        if (connectionManager == null) {
            throw new IllegalArgumentException("The connectionManager is null.");
        }
        if (tableMetaData == null) {
            throw new IllegalArgumentException("The tableMetaData is null");
        }
        if (setId <= 0) {
            throw new IllegalArgumentException("The set ID value is invalid: " + setId);
        }        
        _connectionManager = connectionManager;
        _tableMetaData = tableMetaData;
        _collectionId = setId;
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
    public AbstractConditionsDatabaseObject(
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
            throw new ConditionsObjectException("This object cannot be deleted in read only mode.");
        }
        String query = "DELETE FROM " + _tableMetaData.getTableName() + " WHERE id = " + _rowId;
        _connectionManager.update(query);
        _rowId = -1;
    }
    
    public void insert() throws ConditionsObjectException, SQLException {
        if (!isNew()) {
            throw new ConditionsObjectException("Record already exists in database.");
        }
        if (isReadOnly()) {
            throw new ConditionsObjectException("Cannot insert in read only mode.");
        }
        if (_fieldValues.size() == 0) {
            throw new ConditionsObjectException("There are no field values to insert.");
        }        
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
            throw new ConditionsObjectException("Cannot update in read only mode.");
        }
        if (isNew()) {
            throw new ConditionsObjectException("Cannot update a new record.");
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
        if (_fieldValues == null)
            _fieldValues = new FieldValueMap();
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

    public void setConnectionManager(ConnectionManager connectionManager) {
        _connectionManager = connectionManager;        
    }

    public void setTableMetaData(ConditionsTableMetaData tableMetaData) {
        _tableMetaData = tableMetaData;
    }

    public void setCollectionId(int collectionId) {
        _collectionId = collectionId;
        if (!isNew())
            setIsDirty(true);
    }
        
    public void setIsDirty(boolean isDirty) {
        _isDirty = isDirty;
    }
    
    protected void setIsReadOnly(boolean isReadOnly) {
        _isReadOnly = isReadOnly;
    }
}