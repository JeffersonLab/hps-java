package org.hps.conditions.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectUtilities;

/**
 * This is a registry providing a map between tables and their meta-data.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public final class TableRegistry extends HashMap<String, TableMetaData> {
    
    static class ObjectTypeMap extends HashMap<Class<? extends ConditionsObject>, List<TableMetaData>> {
        void add(Class<? extends ConditionsObject> type, TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());
            }
            this.get(type).add(metaData);
        }
    }
    
    static class CollectionTypeMap extends HashMap<Class<? extends AbstractConditionsObjectCollection<?>>, List<TableMetaData>> {
        void add(Class<? extends AbstractConditionsObjectCollection<?>> type, TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());                                    
            }
            this.get(type).add(metaData);
        }        
    }
    
    ObjectTypeMap objectTypeMap = new ObjectTypeMap();
    CollectionTypeMap collectionTypeMap = new CollectionTypeMap();
        
    private TableRegistry() {
    }
    
    static TableRegistry create() {
        TableRegistry registry = new TableRegistry();
        for (Class<? extends ConditionsObject> objectType : ConditionsObjectUtilities.findConditionsObjectTypes()) {
            Class<? extends AbstractConditionsObjectCollection<?>> collectionType = 
                    ConditionsObjectUtilities.getCollectionType(objectType);
            Set<String> fieldNames = ConditionsObjectUtilities.getFieldNames(objectType);            
            for (String name : ConditionsObjectUtilities.getTableNames(objectType)) {    
                
                // Create a meta data mapping for each table name in the class description.
                TableMetaData data = new TableMetaData(name, name, objectType, collectionType, fieldNames);
                registry.put(name, data);                               
                registry.objectTypeMap.add(objectType, data);
                registry.collectionTypeMap.add(collectionType, data);                  
            }            
        }
        return registry;
    }
          
    List<TableMetaData> findByObjectType(Class<? extends ConditionsObject> objectType) {
        return objectTypeMap.get(objectType);
    }
    
    List<TableMetaData> findByCollectionType(Class<?> collectionType) {
        return collectionTypeMap.get(collectionType);
    }
    
    TableMetaData findByTableName(String name) {
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
