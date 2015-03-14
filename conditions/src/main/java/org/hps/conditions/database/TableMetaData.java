package org.hps.conditions.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;

/**
 * <p>
 * This class provides meta data about a conditions table, including a list of
 * conditions data fields. The list of fields does not include the collection ID
 * or row ID, which are implicitly assumed to exist.
 * <p>
 * It also has references to the implementation classes which are used for the ORM
 * onto {@link ConditionsObject} and {@link ConditionsObjectCollection}.
 * 
 * @see org.hps.conditions.api.ConditionsObject
 * @see org.hps.conditions.api.AbstractConditionsObjectCollection
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 */
public final class TableMetaData {

    protected String tableName;
    protected String key;
    protected Class<? extends ConditionsObject> objectClass;
    protected Class<? extends AbstractConditionsObjectCollection<?>> collectionClass;
    protected Set<String> fieldNames = new LinkedHashSet<String>();
    protected Map<String, Class<?>> fieldTypes;

    /**
     * The fully qualified constructor.
     * @param tableName The name of the table in the conditions database.
     * @param objectClass The type of object for the data mapping.
     * @param collectionClass The type of collection for the data mapping.
     */
    /*
    public TableMetaData(
            String key, 
            String tableName, 
            Class<? extends ConditionsObject> objectClass, 
            Class<? extends AbstractConditionsObjectCollection<?>> collectionClass,
            Map<String, Class<?>> fieldTypes) {
       
        this.key = key;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.collectionClass = collectionClass;
        this.fieldTypes = fieldTypes;
    }
    */
    
    public TableMetaData(
            String key, 
            String tableName, 
            Class<? extends ConditionsObject> objectClass, 
            Class<? extends AbstractConditionsObjectCollection<?>> collectionClass, 
            Set<String> fieldNames,
            Map<String, Class<?>> fieldTypes) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (tableName == null) {
            throw new IllegalArgumentException("tableName is null");
        }
        if (objectClass == null) {
            throw new IllegalArgumentException("objectClass is null");
        }
        if (fieldNames == null) {
            throw new IllegalArgumentException("fieldNames is null");
        }
        if (collectionClass == null) {
            throw new IllegalArgumentException("collectionClass is null");
        }
        if (fieldTypes == null) {
            throw new IllegalArgumentException("fieldTypes is null");
        }
        this.key = key;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.collectionClass = collectionClass;
        this.fieldNames = fieldNames;
        this.fieldTypes = fieldTypes;
    }
    
    /**
     * Get the type of object this table maps onto.
     * @return The type of object.
     */
    public Class<? extends ConditionsObject> getObjectClass() {
        return objectClass;
    }

    /**
     * Get the type of collection this table maps onto.
     * @return
     */
    public Class<? extends AbstractConditionsObjectCollection<?>> getCollectionClass() {
        return collectionClass;
    }

    /**
     * Get the names of the fields. Types are implied from the database tables.
     * @return The names of the fields.
     */
    public String[] getFieldNames() {
        return fieldNames.toArray(new String[] {});
    }

    /**
     * Get the type of the field called <code>fieldName</code>.
     * @return The type of the field.
     */
    public Class<?> getFieldType(String fieldName) {
        return fieldTypes.get(fieldName);
    }

    /**
     * Get the name of the table.
     * @return The name of the table.
     */
    public String getTableName() {
        return tableName;
    }
    
    /**
     * Get the key of this conditions type. May be different from table name but
     * is usually the same.
     * @return The key name of the conditions type.
     */
    public String getKey() {
        return key;
    }
    
    static public List<TableMetaData> findByObjectType(List<TableMetaData> tableMetaDataList, Class<? extends ConditionsObject> objectType) {
        List<TableMetaData> list = new ArrayList<TableMetaData>();
        for (TableMetaData tableMetaData : tableMetaDataList) {
            if (tableMetaData.getObjectClass().equals(objectType)) {

                list.add(tableMetaData);
            }
        }
        return list;
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("tableMetaData: tableName = " + this.getTableName());  
        buff.append(", objectClass = " + this.getObjectClass().getCanonicalName());
        buff.append(", collectionClass = " + this.getCollectionClass().getCanonicalName());
        buff.append(", fieldNames = ");
        for (String field : this.getFieldNames()) {
            buff.append(field + " ");
        }
        buff.setLength(buff.length() - 1);
        buff.append('\n');
        return buff.toString();
    }
}