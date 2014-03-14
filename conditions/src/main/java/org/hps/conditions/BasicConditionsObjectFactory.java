package org.hps.conditions;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;

/**
 * The basic implementation of the {@link ConditionsObjectFactory} interface.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class BasicConditionsObjectFactory implements ConditionsObjectFactory {
    
    private ConnectionManager _connectionManager;
    private ConditionsTableRegistry _tableRegistry;
    
    protected BasicConditionsObjectFactory(ConnectionManager connectionManager, ConditionsTableRegistry tableRegistry) {
        _connectionManager = connectionManager;
        _tableRegistry = tableRegistry;
    }
    
    /**
     * This method is the primary one in the API for creating new conditions objects.
     */
    @SuppressWarnings("unchecked")
    public <T> T createObject(
            Class<? extends ConditionsObject> klass, 
            String tableName,  
            int rowId,
            FieldValueMap fieldValues,
            boolean isReadOnly) throws ConditionsObjectException {                
        ConditionsObject newObject = null;
        try {
            newObject = klass.newInstance();
        } catch (InstantiationException x) { 
            throw new RuntimeException(x);
        } catch (IllegalAccessException x) {
            throw new RuntimeException(x);
        }        
        if (rowId != -1)
            newObject.setRowId(rowId);
        newObject.setFieldValues(fieldValues);
        newObject.setConnectionManager(_connectionManager);
        ConditionsTableMetaData tableMetaData = _tableRegistry.getTableMetaData(tableName);
        if (tableMetaData == null) {
            throw new ConditionsObjectException("No meta data found for table: " + tableName);
        }
        newObject.setTableMetaData(tableMetaData);
        if (isReadOnly)
            newObject.setIsReadOnly();
        return (T)newObject;
    }   
    
    public ConditionsTableRegistry getTableRegistry() {
        return _tableRegistry;
    }
    
    public ConditionsTableMetaData getTableMetaData(String name) {
        return _tableRegistry.getTableMetaData(name);
    }
}
