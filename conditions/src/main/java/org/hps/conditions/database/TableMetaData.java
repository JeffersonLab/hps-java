package org.hps.conditions.database;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;

/**
 * <p>
 * This class provides meta data about a conditions table, including a list of
 * conditions data fields. The list of fields does not include the collection ID
 * or row ID, which are implicitly assumed to exist.
 * <p>
 * It also has references to the implementation classes which are used to map
 * the data onto {@link ConditionsObject} and {@link AbstractConditionsObjectCollection}.
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

    /**
     * The fully qualified constructor.
     * @param tableName The name of the table in the conditions database.
     * @param objectClass The type of object for the data mapping.
     * @param collectionClass The type of collection for the data mapping.
     */
    public TableMetaData(String key, String tableName, Class<? extends ConditionsObject> objectClass, Class<? extends AbstractConditionsObjectCollection<?>> collectionClass) {
        this.key = key;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.collectionClass = collectionClass;
    }
    
    public TableMetaData(String key, String tableName, Class<? extends ConditionsObject> objectClass, Class<? extends AbstractConditionsObjectCollection<?>> collectionClass, Set<String> fieldNames) {
        this.key = key;
        this.tableName = tableName;
        this.objectClass = objectClass;
        this.collectionClass = collectionClass;
        this.fieldNames = fieldNames;
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
     * Add a field.
     * @param name The name of the field.
     */
    void addField(String name) {
        fieldNames.add(name);
    }
    
    void addFields(List<String> names) {
        fieldNames.addAll(names);
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
        System.out.println("findByObjectType - " + objectType.getCanonicalName());
        List<TableMetaData> list = new ArrayList<TableMetaData>();
        for (TableMetaData tableMetaData : tableMetaDataList) {
            System.out.println("comparing to " + tableMetaData.getObjectClass().getCanonicalName());
            if (tableMetaData.getObjectClass().equals(objectType)) {
                System.out.println("found match");
                list.add(tableMetaData);
            } else {
                System.out.println("does not match");
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