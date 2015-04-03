package org.hps.conditions.database;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectUtilities;

/**
 * This is a registry providing a map between tables and their meta-data.
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class TableRegistry extends HashMap<String, TableMetaData> {

    /**
     * Maps types to table meta data.     
     */
    static class ObjectTypeMap extends HashMap<Class<? extends ConditionsObject>, List<TableMetaData>> {
        /**
         * Add a connection between an object type and table meta data.
         * @param type The object type.
         * @param metaData The table meta data.
         */
        void add(Class<? extends ConditionsObject> type, TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());
            }
            this.get(type).add(metaData);
        }
    }

    /**
     * Maps collection types to table meta data.
     */    
    static class CollectionTypeMap extends HashMap<Class<? extends BaseConditionsObjectCollection<?>>, List<TableMetaData>> {
        /**
         * Add a connection between a collection type and table meta data.
         * @param type The collection type.
         * @param metaData The table meta data.
         */
        void add(Class<? extends BaseConditionsObjectCollection<?>> type, TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());                                    
            }
            this.get(type).add(metaData);
        }
    }

    /**
     * Map between object types and meta data.
     */
    private ObjectTypeMap objectTypeMap = new ObjectTypeMap();
    
    /**
     * Map between collection types and meta data.
     */
    private CollectionTypeMap collectionTypeMap = new CollectionTypeMap();

    /**
     * Class should not be directly instantiated.
     * <p>
     * Use the {@link #create()} method instead.
     */
    private TableRegistry() {
    }

    /**
     * Create a new table meta data registry.
     * @return The meta data registry.
     */
    static TableRegistry create() {
        TableRegistry registry = new TableRegistry();
        for (Class<? extends ConditionsObject> objectType : ConditionsObjectUtilities.findConditionsObjectTypes()) {

            // Get the collection type.
            Class<? extends BaseConditionsObjectCollection<?>> collectionType =
                    ConditionsObjectUtilities.getCollectionType(objectType);

            // Get the list of field names.
            Set<String> fieldNames = ConditionsObjectUtilities.getFieldNames(objectType);

            // Create map of fields to their types.
            Map<String, Class<?>> fieldTypes = new HashMap<String, Class<?>>();
            for (Method method : objectType.getMethods()) {
                if (!method.getReturnType().equals(Void.TYPE)) {
                    for (Annotation annotation : method.getAnnotations()) {
                        if (annotation.annotationType().equals(Field.class)) {
                            Field field = (Field) annotation;
                            for (String fieldName : field.names()) {
                                fieldTypes.put(fieldName, method.getReturnType());
                            }
                        }
                    }
                }
            }

            for (String name : ConditionsObjectUtilities.getTableNames(objectType)) {
                // Create a meta data mapping for each table name in the class description.
                TableMetaData data = new TableMetaData(name, name, objectType, collectionType, fieldNames, fieldTypes);
                registry.put(name, data);
                registry.objectTypeMap.add(objectType, data);
                registry.collectionTypeMap.add(collectionType, data);
            }            
        }
        return registry;
    }

    /**
     * Find meta data by object type.
     * @param objectType The object type.
     * @return The meta data or null if none exists.
     */
    List<TableMetaData> findByObjectType(Class<? extends ConditionsObject> objectType) {
        return objectTypeMap.get(objectType);
    }

    /**
     * Find meta data by collection type.
     * @param collectionType The collection type.
     * @return The meta data or null if none exists.
     */
    List<TableMetaData> findByCollectionType(Class<?> collectionType) {
        return collectionTypeMap.get(collectionType);
    }

    /**
     * Find meta data by table name.
     * @param name The table name.
     * @return The meta data or null if none exists.
     */
    TableMetaData findByTableName(String name) {
        return this.get(name);
    }

    /**
     * Convert this object to a string.
     * @return This object converted to a string.
     */
    public String toString() {  
        StringBuffer buff = new StringBuffer();
        for (TableMetaData tableMetaData : this.values()) {
            buff.append(tableMetaData.toString());
        }
        return buff.toString();
    }
}
