package org.hps.conditions.database;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.conditions.api.BaseConditionsObjectCollection;
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
 * @see org.hps.conditions.api.BaseConditionsObjectCollection
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class TableMetaData {

    /**
     * The table name.
     */
    private String tableName;

    /**
     * The conditions key named (unused???).
     */
    private String key;

    /**
     * The object class.
     */
    private Class<? extends ConditionsObject> objectClass;

    /**
     * The collection class.
     */
    private Class<? extends BaseConditionsObjectCollection<?>> collectionClass;

    /**
     * The set of field names.
     */
    private Set<String> fieldNames = new LinkedHashSet<String>();

    /**
     * The map of field names to their types.
     */
    private Map<String, Class<?>> fieldTypes;

    /**
     * Fully qualified constructor.
     * @param key the conditions key
     * @param tableName the table name
     * @param objectClass the object class
     * @param collectionClass the collection class
     * @param fieldNames the field names
     * @param fieldTypes the field types
     */
    public TableMetaData(
            final String key,
            final String tableName,
            final Class<? extends ConditionsObject> objectClass,
            final Class<? extends BaseConditionsObjectCollection<?>> collectionClass,
            final Set<String> fieldNames,
            final Map<String, Class<?>> fieldTypes) {
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
     * @return the type of object
     */
    public Class<? extends ConditionsObject> getObjectClass() {
        return objectClass;
    }

    /**
     * Get the type of collection this table maps onto.
     * @return the collection class
     */
    public Class<? extends BaseConditionsObjectCollection<?>> getCollectionClass() {
        return collectionClass;
    }

    /**
     * Get the names of the fields. Types are implied from the database tables.
     * @return the names of the fields
     */
    public String[] getFieldNames() {
        return fieldNames.toArray(new String[] {});
    }

    /**
     * Get the type of the field called <code>fieldName</code>.
     * @return the type of the field
     */
    public Class<?> getFieldType(final String fieldName) {
        return fieldTypes.get(fieldName);
    }

    /**
     * Get the name of the table.
     * @return the name of the table
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Get the key of this conditions type. May be different from table name but
     * is usually the same.
     * @return the key name of the conditions type
     */
    public String getKey() {
        return key;
    }

    /**
     * Find table meta data by object type.
     * @param tableMetaDataList the list of table meta data e.g. from the registry
     * @param objectType the type of the object
     * @return the list of table meta data that have that object type
     */
    public static List<TableMetaData> findByObjectType(final List<TableMetaData> tableMetaDataList,
            final Class<? extends ConditionsObject> objectType) {
        List<TableMetaData> list = new ArrayList<TableMetaData>();
        for (TableMetaData tableMetaData : tableMetaDataList) {
            if (tableMetaData.getObjectClass().equals(objectType)) {

                list.add(tableMetaData);
            }
        }
        return list;
    }
    
    /**
     * Convert to a string.
     * @return This object converted to a string.
     */
    public String toString() {
        final StringBuffer buff = new StringBuffer();
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