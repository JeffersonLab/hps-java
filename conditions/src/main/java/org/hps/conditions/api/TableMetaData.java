package org.hps.conditions.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This class provides meta data about a conditions table, including a list of fields. A row ID field called <i>id</i>
 * is assumed to exist and be a primary key for the table. (Problems will occur if this is not the case!)
 * <p>
 * The class also has references to the implementation classes which are used for mapping database operations to the
 * {@link ConditionsObject} and {@link ConditionsObjectCollection} API.
 *
 * @author Jeremy McCormick, SLAC
 * @see org.hps.conditions.api.ConditionsObject
 * @see org.hps.conditions.api.BaseConditionsObjectCollection
 */
public final class TableMetaData {

    /**
     * Find table meta data by object type.
     *
     * @param tableMetaDataList the list of table meta data e.g. from the registry
     * @param objectType the type of the object
     * @return the list of table meta data that have that object type
     */
    public static List<TableMetaData> findByObjectType(final List<TableMetaData> tableMetaDataList,
            final Class<? extends ConditionsObject> objectType) {
        final List<TableMetaData> list = new ArrayList<TableMetaData>();
        for (final TableMetaData tableMetaData : tableMetaDataList) {
            if (tableMetaData.getObjectClass().equals(objectType)) {

                list.add(tableMetaData);
            }
        }
        return list;
    }

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
    private Map<String, Class<?>> fieldTypes = new HashMap<String, Class<?>>();

    /**
     * The conditions key named (unused???).
     */
    private String key;

    /**
     * The object class.
     */
    private Class<? extends ConditionsObject> objectClass;

    /**
     * The table name.
     */
    private String tableName;

    /**
     * Class constructor.
     * <p>
     * Creates an empty object.
     */
    public TableMetaData() {
    }

    /**
     * Fully qualified constructor.
     *
     * @param key the conditions key
     * @param tableName the table name
     * @param objectClass the object class
     * @param collectionClass the collection class
     * @param fieldNames the field names
     * @param fieldTypes the field types
     */
    public TableMetaData(final String key, final String tableName, final Class<? extends ConditionsObject> objectClass,
            final Class<? extends BaseConditionsObjectCollection<?>> collectionClass, final Set<String> fieldNames,
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
     * Get the type of collection this table maps onto.
     *
     * @return the collection class
     */
    public Class<? extends ConditionsObjectCollection<?>> getCollectionClass() {
        return this.collectionClass;
    }

    /**
     * Get the field names.
     *
     * @return the set of field names
     */
    public Set<String> getFieldNames() {
        return this.fieldNames;
    }

    /**
     * Get the type of the field called <code>fieldName</code>.
     *
     * @return the type of the field
     */
    public Class<?> getFieldType(final String fieldName) {
        return this.fieldTypes.get(fieldName);
    }

    /**
     * Get the key of this conditions type. May be different from table name but is usually the same.
     *
     * @return the key name of the conditions type
     */
    public String getKey() {
        return this.key;
    }

    /**
     * Get the type of object this table maps onto.
     *
     * @return the type of object
     */
    public Class<? extends ConditionsObject> getObjectClass() {
        return this.objectClass;
    }

    /**
     * Get the name of the table.
     *
     * @return the name of the table
     */
    public String getTableName() {
        return this.tableName;
    }

    /**
     * Create a new collection instance from the table meta data's collection type.
     *
     * @return the new object collection
     * @throws ConditionsObjectException if there is an error creating a new collection
     */
    ConditionsObjectCollection<?> newCollection() throws ConditionsObjectException {
        try {
            final ConditionsObjectCollection<?> collection = this.getCollectionClass().newInstance();
            collection.setTableMetaData(this);
            return collection;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConditionsObjectException("Error creating new conditions object collection.", e);
        }
    }

    /**
     * Create a new object instance from the table meta data's object type.
     *
     * @return the new conditions object
     * @throws ConditionsObjectException if there is an error creating a new object
     */
    ConditionsObject newObject() throws ConditionsObjectException {
        try {
            final ConditionsObject object = this.getObjectClass().newInstance();
            object.setTableMetaData(this);
            return object;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConditionsObjectException("Error creating new conditions object collection.", e);
        }
    }

    /**
     * Set all the field names from an array of strings.
     *
     * @param fieldNames the names of the fields
     */
    void setFieldNames(final String[] fieldNames) {
        this.fieldNames = new HashSet<String>();
        for (final String fieldName : fieldNames) {
            this.fieldNames.add(fieldName);
        }
    }

    /**
     * Set the type of a field.
     *
     * @param fieldName the field name
     * @param fieldType the field's type (class)
     */
    void setFieldType(final String fieldName, final Class<?> fieldType) {
        this.fieldTypes.put(fieldName, fieldType);
    }

    /**
     * Set the class that maps to the database.
     *
     * @param objectClass the class that maps to the database
     */
    void setObjectClass(final Class<? extends ConditionsObject> objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * Set the table name.
     *
     * @param tableName the table name
     */
    void setTableName(final String tableName) {
        this.tableName = tableName;
    }

    /**
     * Convert to a string.
     *
     * @return This object converted to a string.
     */
    @Override
    public String toString() {
        final StringBuffer buff = new StringBuffer();
        buff.append("tableMetaData: tableName = " + this.getTableName());
        buff.append(", objectClass = " + this.getObjectClass().getCanonicalName());
        buff.append(", collectionClass = " + this.getCollectionClass().getCanonicalName());
        buff.append(", fieldNames = ");
        for (final String field : this.getFieldNames()) {
            buff.append(field + " ");
        }
        buff.setLength(buff.length() - 1);
        buff.append('\n');
        return buff.toString();
    }
    
    /**
     * Method for removing a field that was found using the automatic introspection methods.
     */
    void removeField(String fieldName) {
        fieldNames.remove(fieldName);
        fieldTypes.remove(fieldName);
    }

    // TODO: add methods for getting SQL strings for PreparedStatements; can be setup once at initialization time for each table
    // String getSelectStatement();
    // String getInsertStatement();
    // String getUpdateStatement();
    // String getDeleteStatement();

    // TODO: add method for getting column class name that maps to JDBC; see similar ConditionsObjectUtilities method
    // String getColumnClassName(String columnName);
}
