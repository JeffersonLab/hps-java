package org.hps.conditions.database;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectUtilities;

/**
 * This is a registry providing a map between tables and their meta-data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class TableRegistry extends HashMap<String, TableMetaData> {
    
    protected Map<Class<? extends ConditionsObject>, TableMetaData> objectTypeMap =
            new HashMap<Class<? extends ConditionsObject>, TableMetaData>();
    
    protected Map<Class<? extends AbstractConditionsObjectCollection<?>>, TableMetaData> collectionTypeMap =
            new HashMap<Class<? extends AbstractConditionsObjectCollection<?>>, TableMetaData>();
    
    private TableRegistry() {
    }
    
    public static TableRegistry create() {
        TableRegistry registry = new TableRegistry();
        for (Class<? extends ConditionsObject> objectType : ConditionsObjectUtilities.findConditionsObjectTypes()) {
            String name = ConditionsObjectUtilities.getTableNames(objectType)[0];
            Class<? extends AbstractConditionsObjectCollection<?>> collectionType = 
                    ConditionsObjectUtilities.getCollectionType(objectType);
            Set<String> fieldNames = ConditionsObjectUtilities.getFieldNames(objectType);
            TableMetaData data = new TableMetaData(name, name, objectType, collectionType, fieldNames); 
            registry.put(name, data);
            registry.objectTypeMap.put(objectType, data);
            registry.collectionTypeMap.put(collectionType, data);
        }
        return registry;
    }
    
    public TableMetaData findByObjectType(Class<? extends ConditionsObject> objectType) {
        return objectTypeMap.get(objectType);
    }
    
    public TableMetaData findByCollectionType(Class<?> collectionType) {
        return collectionTypeMap.get(collectionType);
    }
    
    public TableMetaData findByTableName(String name) {
        return this.get(name);
    }
    
    public String toString() {  
        StringBuffer buff = new StringBuffer();
        for (TableMetaData tableMetaData : this.values()) {
            buff.append(tableMetaData.toString());
        }
        return buff.toString();
    }
}
