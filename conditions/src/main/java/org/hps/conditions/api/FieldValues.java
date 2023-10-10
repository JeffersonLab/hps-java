package org.hps.conditions.api;

import java.util.Collection;
import java.util.Set;

/**
 * Representation of fields and values of individual conditions objects.
 */
public interface FieldValues {

    /**
     * Get the field names.
     *
     * @return the field names
     */
    Set<String> getFieldNames();

    /**
     * Get a field value.
     *
     * @param <T> the object type (inferred from the explicit type argument)
     * @param type the class of the object to return
     * @param name the name of the field
     * @return the field value
     */
    // FIXME: This could be simplified by using type inference on the return object and removing the type param.
    <T> T getValue(Class<T> type, String name);

    /**
     * Get a field value as an object.
     *
     * @param name the name of the field
     * @return the field value as an object
     */
    Object getValue(String name);

    /**
     * Get the collection of values.
     *
     * @return the collection of values
     */
    Collection<Object> getValues();

    /**
     * Return <code>true</code> if this field exists.
     *
     * @param name the name of the field
     * @return <code>true</code> if the field exists
     */
    boolean hasField(String name);

    /**
     * Return <code>true</code> if the field's value is not null.
     *
     * @param name the name of the field
     * @return <code>true</code> if the field's value is not null
     */
    boolean isNonNull(String name);

    /**
     * Return <code>true</code> if the field's value is null.
     *
     * @param name the name of the field
     * @return <code>true</code> if the field's value is null
     */
    boolean isNull(String name);

    /**
     * Set the value of a field.
     *
     * @param name the name of the field
     * @param value the new value of the field
     */
    void setValue(String name, Object value);

    /**
     * Get the size which is the field count.
     *
     * @return the size
     */
    int size();
}
