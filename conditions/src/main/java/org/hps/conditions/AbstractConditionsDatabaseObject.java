package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

/**
 * The abstract implementation of {@link ConditionsDatabaseObject}.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public abstract class AbstractConditionsDatabaseObject implements ConditionsDatabaseObject {

    ConnectionManager _connectionManager = null;
    ConditionsTableMetaData _tableMetaData = null;
    int _rowId = -1;
    int _setId = -1;
    boolean _isDirty = false;
    boolean _isReadOnly = false;
    FieldValueMap _fieldValues = null;
    
    /**
     * Map of field names to their values.
     */
    public static final class FieldValueMap extends LinkedHashMap<String, Object> {
    }      
    
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
        _setId = setId;
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

    public int getSetId() {
        return _setId;
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
    
    public void delete() throws ConditionsDatabaseObjectException {
        if (isReadOnly()) {
            throw new ConditionsDatabaseObjectException("This object cannot be deleted in read only mode.");
        }
        String query = "DELETE FROM " + _tableMetaData.getTableName() + " WHERE id = " + _rowId;
        _connectionManager.update(query);
        _rowId = -1;
    }
    
    public void insert() throws ConditionsDatabaseObjectException, SQLException {
        if (!isNew()) {
            throw new ConditionsDatabaseObjectException("Record already exists in database.");
        }
        if (isReadOnly()) {
            throw new ConditionsDatabaseObjectException("Cannot insert in read only mode.");
        }
        if (_fieldValues.size() == 0) {
            throw new ConditionsDatabaseObjectException("There are no field values to insert.");
        }        
        StringBuffer buff = new StringBuffer();
        buff.append("INSERT INTO " + _tableMetaData.getTableName() + "( set_id");
        for (Entry<String, Object> entry : _fieldValues.entrySet()) {
            buff.append(", " + entry.getKey());
        }
        buff.append(" ) VALUES ( ");
        buff.append(_setId);
        for (Entry<String, Object> entry : _fieldValues.entrySet()) {
            buff.append(", " + entry.getValue());
        } 
        buff.append(") ");       
        int key = _connectionManager.update(buff.toString());        
        _rowId = key;        
    }

    public void select() throws ConditionsDatabaseObjectException, SQLException {
        if (isNew()) {
            throw new ConditionsDatabaseObjectException("Record has not been inserted into database yet.");
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
        
    public void update() throws ConditionsDatabaseObjectException {
        if (isReadOnly()) {
            throw new ConditionsDatabaseObjectException("Cannot update in read only mode.");
        }
        if (isNew()) {
            throw new ConditionsDatabaseObjectException("Cannot update a new record.");
        }
        if (_fieldValues.size() == 0) {
            throw new ConditionsDatabaseObjectException("No field values to update.");
        }
        StringBuffer buff = new StringBuffer();
        buff.append("UPDATE " + _tableMetaData.getTableName() + " SET ");
        for (Entry entry : _fieldValues.entrySet()) {
            buff.append(entry.getKey() + " = '" + entry.getValue() + "', ");
        }
        buff.delete(buff.length()-2, buff.length()-1);
        buff.append(" WHERE id = " + _rowId); 
        _connectionManager.update(buff.toString());
        _isDirty = false;
    }
    
    public void setFieldValue(String key, Object value) {
        _fieldValues.put(key, value);
        _isDirty = true;
    }        
}
