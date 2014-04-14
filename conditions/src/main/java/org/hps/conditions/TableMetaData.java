package org.hps.conditions;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>
 * This class provides meta data about a conditions table, including a
 * list of conditions data fields.  The list of fields does not include
 * the collection ID or row ID, which are implicitly assumed to exist.
 * </p>  
 * <p>
 * It also has references to the implementation classes which are used to 
 * map the data onto {@link ConditionsObject} and {@link ConditionsObjectCollection}s.
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class TableMetaData {
    
    String tableName;
    String key;
    Class<? extends ConditionsObject> objectClass;
    Class<? extends ConditionsObjectCollection<?>> collectionClass;
    Set<String> fieldNames = new LinkedHashSet<String>();
        
    /**
     * The fully qualified constructor.
     * @param tableName The name of the table in the conditions database.
     * @param objectClass The type of object for the data mapping.
     * @param collectionClass The type of collection for the data mapping.
     */
    TableMetaData(
            String key,
            String tableName, 
            Class<? extends ConditionsObject> objectClass, 
            Class<? extends ConditionsObjectCollection<?>> collectionClass) {
        this.key = key;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.collectionClass = collectionClass;
    }
    
    /**
     * Get the type of object this table maps onto.
     * @return The type of object.
     */
    Class<? extends ConditionsObject> getObjectClass() {
        return objectClass;
    }
    
    /**
     * Get the type of collection this table maps onto.
     * @return
     */
    Class<? extends ConditionsObjectCollection<?>> getCollectionClass() {
        return collectionClass;
    }
    
    /**
     * Get the names of the fields.
     * Types are implied from the database tables.
     * @return The names of the fields.
     */
    String[] getFieldNames() {
        return fieldNames.toArray(new String[]{});
    }
     
    /**
     * Add a field.
     * @param name The name of the field.
     */
    void addField(String name) {
        if (fieldNames.contains(name)) {
            throw new RuntimeException("The table meta data already has a field called " + name);
        }
        fieldNames.add(name);
    }
    
    /**
     * Get the name of the table.
     * @return The name of the table.
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Get the key of this conditions type.  May be different from table name 
     * but is usually the same.
     * @return The key name of the conditions type.
     */
    public String getKey() {
        return key;
    }
}