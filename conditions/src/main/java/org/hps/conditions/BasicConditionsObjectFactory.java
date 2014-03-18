package org.hps.conditions;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;

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
    
    /**
     * Get the <code>ConditionsTableRegistry</code> that will be used by the factory to get
     * table meta data.
     * @return The conditions table registry.
     */
    public ConditionsTableRegistry getTableRegistry() {
        return _tableRegistry;
    }
    
    /**
     * Get table meta data by name from the registry.
     * @return The table meta data or null if does not exist.
     */
    public ConditionsTableMetaData getTableMetaData(String name) {
        return _tableRegistry.getTableMetaData(name);
    }
}
