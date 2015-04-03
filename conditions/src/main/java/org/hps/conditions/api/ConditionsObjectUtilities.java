package org.hps.conditions.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javassist.Modifier;

import org.hps.conditions.database.Field;
import org.hps.conditions.database.Table;
import org.reflections.Reflections;

/**
 * This is a collection of utility methods for {@link ConditionsObject}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public final class ConditionsObjectUtilities {

    /**
     * Do not allow class to be instantiated.
     */
    private ConditionsObjectUtilities() {
    }

    /**
     * Get the list of table names for the class.
     *
     * @param type The class.
     * @return The list of table names.
     */
    public static String[] getTableNames(final Class<? extends ConditionsObject> type) {
        final Table tableAnnotation = type.getAnnotation(Table.class);
        if (tableAnnotation != null) {
            return tableAnnotation.names();
        } else {
            return new String[] {};
        }
    }

    /**
     * Get the list of database field names for the class.
     *
     * @param type The class.
     * @return The list of field names.
     */
    public static Set<String> getFieldNames(final Class<? extends ConditionsObject> type) {
        final Set<String> fieldNames = new HashSet<String>();
        for (Method method : type.getMethods()) {
            if (!method.getReturnType().equals(Void.TYPE)) {
                for (Annotation annotation : method.getAnnotations()) {
                    if (annotation.annotationType().equals(Field.class)) {
                        if (!Modifier.isPublic(method.getModifiers())) {
                            throw new RuntimeException("The method " + type.getName() + "." + method.getName()
                                    + " has a Field annotation but is not public.");
                        }
                        final Field field = (Field) annotation;
                        for (String fieldName : field.names()) {
                            if (fieldName != null && !("".equals(fieldName))) {
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
     * Get the class for the collection of the ConditionsObject type.
     *
     * @param type The class of the ConditionsObject.
     * @return The class of the collection.
     */
    @SuppressWarnings("unchecked")
    public static Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>> getCollectionType(
            final Class<? extends ConditionsObject> type) {
        final String collectionClassName = type.getCanonicalName() + "$" + type.getSimpleName() + "Collection";
        Class<?> rawCollectionClass;
        try {
            rawCollectionClass = Class.forName(collectionClassName);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("The type does not define a nested collection class.", e);
        }
        if (!BaseConditionsObjectCollection.class.isAssignableFrom(rawCollectionClass)) {
            throw new RuntimeException("The class " + rawCollectionClass.getSimpleName()
                    + " does not extend ConditionsObjectCollection.");
        }
        return (Class<? extends BaseConditionsObjectCollection<? extends ConditionsObject>>) rawCollectionClass;
    }

    /**
     * Find all available classes that extend ConditionsObject.
     *
     * @return The set of all available classes that extend ConditionsObject.
     */
    public static Set<Class<? extends ConditionsObject>> findConditionsObjectTypes() {
        final Reflections reflections = new Reflections("org.hps.conditions");
        final Set<Class<? extends ConditionsObject>> objectTypes = new HashSet<Class<? extends ConditionsObject>>();
        for (Class<? extends ConditionsObject> objectType : reflections.getSubTypesOf(ConditionsObject.class)) {
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
}
