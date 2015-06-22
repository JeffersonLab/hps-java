package org.hps.conditions.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javassist.Modifier;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.reflections.Reflections;

/**
 * This is a registry providing a map between tables and their meta-data.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
@SuppressWarnings("serial")
public final class TableRegistry extends HashMap<String, TableMetaData> {

    /**
     * Class which maps collection types to their table meta data.
     */
    static class CollectionTypeMap extends
            HashMap<Class<? extends BaseConditionsObjectCollection<?>>, List<TableMetaData>> {

        /**
         * Add a mapping between a collection type and table meta data.
         *
         * @param type the collection type
         * @param metaData the table meta data
         */
        void add(final Class<? extends BaseConditionsObjectCollection<?>> type, final TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());
            }
            this.get(type).add(metaData);
        }
    }

    /**
     * Class that maps object types to their table meta data.
     */
    static class ObjectTypeMap extends HashMap<Class<? extends ConditionsObject>, List<TableMetaData>> {
        /**
         * Add a connection between an object type and table meta data.
         *
         * @param type the object type
         * @param metaData the table meta data
         */
        void add(final Class<? extends ConditionsObject> type, final TableMetaData metaData) {
            if (this.get(type) == null) {
                this.put(type, new ArrayList<TableMetaData>());
            }
            this.get(type).add(metaData);
        }
    }

    /**
     * The global, static instance of the registry.
     */
    private static TableRegistry instance = null;

    /**
     * Create a new table meta data registry.
     *
     * @return the meta data registry
     */
    private static TableRegistry create() {
        final TableRegistry registry = new TableRegistry();
        for (final Class<? extends ConditionsObject> objectType : findConditionsObjectTypes()) {

            // Get the collection type.
            final Class<? extends BaseConditionsObjectCollection<?>> collectionType = getCollectionType(objectType);

            // Get the list of field names.
            final Set<String> fieldNames = getFieldNames(objectType);

            // Create map of fields to their types.
            final Map<String, Class<?>> fieldTypes = new HashMap<String, Class<?>>();
            for (final Method method : objectType.getMethods()) {
                if (!method.getReturnType().equals(Void.TYPE)) {
                    for (final Annotation annotation : method.getAnnotations()) {
                        if (annotation.annotationType().equals(Field.class)) {
                            final Field field = (Field) annotation;
                            for (final String fieldName : field.names()) {
                                fieldTypes.put(fieldName, method.getReturnType());
                            }
                        }
                    }
                }
            }

            for (final String name : getTableNames(objectType)) {
                // Create a meta data mapping for each table name in the class description.
                final TableMetaData data = new TableMetaData(name, name, objectType, collectionType, fieldNames,
                        fieldTypes);
                registry.put(name, data);
                registry.objectTypeMap.add(objectType, data);
                registry.collectionTypeMap.add(collectionType, data);
            }
        }
        return registry;
    }
    
    /**
     * Get the class for the collection of the ConditionsObject type.
     *
     * @param type the class of the ConditionsObject
     * @return the class of the collection
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>> getCollectionType(
            final Class<? extends ConditionsObject> type) {
        final String collectionClassName = type.getCanonicalName() + "$" + type.getSimpleName() + "Collection";
        Class<?> rawCollectionClass;
        try {
            rawCollectionClass = Class.forName(collectionClassName);
        } catch (final ClassNotFoundException e) {
            throw new RuntimeException("The type does not define a nested collection class.", e);
        }
        if (!BaseConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass)) {
            throw new RuntimeException("The class " + rawCollectionClass.getSimpleName()
                    + " does not extend ConditionsObjectCollection.");
        }
        return (Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>>) rawCollectionClass;
    }
    

    /**
     * Get the global static instance of the registry.
     *
     * @return the global static instance of the registry
     */
    public synchronized static TableRegistry getTableRegistry() {
        if (instance == null) {
            // Create registry if it does not exist.
            instance = TableRegistry.create();
        }
        return instance;
    }

    /**
     * Map between collection types and meta data.
     */
    private final CollectionTypeMap collectionTypeMap = new CollectionTypeMap();

    /**
     * Map between object types and meta data.
     */
    private final ObjectTypeMap objectTypeMap = new ObjectTypeMap();

    /**
     * Prevent direct class instantiation by users.
     * <p>
     * Use the {@link #create()} method instead.
     */
    private TableRegistry() {
    }
    
    /**
     * Get the list of database field names for the class.
     *
     * @param type the class
     * @return the list of field names
     */
    private static Set<String> getFieldNames(final Class<? extends ConditionsObject> type) {
        final Set<String> fieldNames = new HashSet<String>();
        for (final Method method : type.getMethods()) {
            if (!method.getReturnType().equals(Void.TYPE)) {
                for (final Annotation annotation : method.getAnnotations()) {
                    if (annotation.annotationType().equals(Field.class)) {
                        if (!Modifier.isPublic(method.getModifiers())) {
                            throw new RuntimeException("The method " + type.getName() + "." + method.getName()
                                    + " has a Field annotation, but it is not public.");
                        }
                        final Field field = (Field) annotation;
                        for (final String fieldName : field.names()) {
                            if (fieldName != null && !"".equals(fieldName)) {
                                fieldNames.add(fieldName);
                            }
                        }
                    }
                }
            }
        }
        return fieldNames;
    }
    
    /**
     * Get the list of table names for the class.
     *
     * @param type the class
     * @return the list of table names
     */
    private static String[] getTableNames(final Class<? extends ConditionsObject> type) {
        final Table tableAnnotation = type.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return tableAnnotation.names();
        } else {
            return new String[] {};
        }
    }

    /**
     * Find meta data by collection type.
     *
     * @param collectionType the collection type
     * @return the meta data or <code>null</code> if none exists.
     */
    public List<TableMetaData> findByCollectionType(final Class<?> collectionType) {
        return this.collectionTypeMap.get(collectionType);
    }

    /**
     * Find meta data by object type.
     *
     * @param objectType the object type
     * @return the meta data or <code>null</code> if none exists.
     */
    public List<TableMetaData> findByObjectType(final Class<? extends ConditionsObject> objectType) {
        return this.objectTypeMap.get(objectType);
    }

    /**
     * Find meta data by table name.
     *
     * @param name the table name
     * @return the meta data or <code>null</code> if none exists
     */
    public TableMetaData findByTableName(final String name) {
        return this.get(name);
    }
    
    /**
     * Find all available classes that extend ConditionsObject.
     *
     * @return The set of all available classes that extend ConditionsObject.
     */
    public static Set<Class<? extends ConditionsObject>> findConditionsObjectTypes() {
        final Reflections reflections = new Reflections("org.hps.conditions");
        final Set<Class<? extends ConditionsObject>> objectTypes = new HashSet<Class<? extends ConditionsObject>>();
        for (final Class<? extends ConditionsObject> objectType : reflections.getSubTypesOf(ConditionsObject.class)) {
            if (Modifier.isAbstract(objectType.getModifiers())) {
                continue;
            }
            if (objectType.getAnnotation(Table.class) == null) {
                continue;
            }
            objectTypes.add(objectType);
        }
        return objectTypes;
    }    

    /**
     * Convert this object to a string.
     *
     * @return this object converted to a string
     */
    @Override
    public String toString() {
        final StringBuffer buff = new StringBuffer();
        for (final TableMetaData tableMetaData : this.values()) {
            buff.append(tableMetaData.toString());
        }
        return buff.toString();
    }
}
