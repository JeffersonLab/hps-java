package org.hps.conditions;

import org.hps.conditions.AbstractConditionsDatabaseObject.FieldValueMap;
import org.hps.conditions.ConditionsObject.ConditionsObjectException;


public class BasicConditionsObjectFactory implements ConditionsObjectFactory {
    
    private ConnectionManager _connectionManager;
    private ConditionsTableRegistry _tableRegistry;
    
    protected BasicConditionsObjectFactory(ConnectionManager connectionManager, ConditionsTableRegistry tableRegistry) {
        _connectionManager = connectionManager;
        _tableRegistry = tableRegistry;
    }
    
    @SuppressWarnings("unchecked")
    // FIXME: Not sure this is the best way to accomplish the desired generic behavior, especially
    //        the unchecked cast to the T return type.
    public ConditionsObject createObject(
            Class<? extends ConditionsObject> klass, 
            String tableName, 
            int collectionId, // should be -1 if new object
            FieldValueMap fieldValues) throws ConditionsObjectException {                
        ConditionsObject newObject = null;
        try {
            newObject = klass.newInstance();
        } catch (InstantiationException x) { 
            throw new RuntimeException(x);
        } catch (IllegalAccessException x) {
            throw new RuntimeException(x);
        }        
        newObject.setCollectionId(collectionId);
        newObject.setFieldValues(fieldValues);
        newObject.setConnectionManager(_connectionManager);
        ConditionsTableMetaData tableMetaData = _tableRegistry.getTableMetaData(tableName);
        if (tableMetaData == null) {
            throw new ConditionsObjectException("No meta data found for table: " + tableName);
        }
        newObject.setTableMetaData(tableMetaData);
        return newObject;
    }   
    
    public ConditionsTableRegistry getTableRegistry() {
        return _tableRegistry;
    }
}
