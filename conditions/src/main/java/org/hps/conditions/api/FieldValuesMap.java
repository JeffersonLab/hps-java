package org.hps.conditions.api;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of {@link FieldValues} for storing field-value pairs.
 *
 * @author Jeremy McCormick, SLAC
 */
public final class FieldValuesMap implements FieldValues {

    /**
     * Map of field names to values.
     */
    private final Map<String, Object> data = new HashMap<String, Object>();

    /**
     * Class constructor.
     */
    public FieldValuesMap() {
    }

    /**
     * Class constructor with table meta data; all fields in the table will have values set to null in the map.
     *
     * @param tableMetaData the table meta data
     */
    FieldValuesMap(final TableMetaData tableMetaData) {
        for (final String fieldName : tableMetaData.getFieldNames()) {
            this.data.put(fieldName, null);
        }
    }

    /**
     * Get the set of field names.
     *
     * @return the set of field names
     */
    @Override
    public Set<String> getFieldNames() {
        return this.data.keySet();
    }

    /**
     * Get a value and cast to a type.
     *
     * @param type the return type
     * @param name the name of the field
     */
    @Override
    public <T> T getValue(final Class<T> type, final String name) {
        return type.cast(this.data.get(name));
    }

    /**
     * Get a value as an object.
     *
     * @return the name of the field
     */
    @Override
    public Object getValue(final String name) {
        return this.data.get(name);
    }

    /**
     * Get the values as a collection of objects.
     *
     * @return the values as a collection of objects
     */
    @Override
    public Collection<Object> getValues() {
        return this.data.values();
    }

    /**
     * Return <code>true</code> if field exists with any value.
     *
     * @return <code>true</code> if field exists
     */
    @Override
    public boolean hasField(final String name) {
        return this.data.containsKey(name);
    }

    /**
     * Return <code>true</code> if the field has a non-null value.
     *
     * @return <code>true</code> if the field has a non-null value
     */
    @Override
    public boolean isNonNull(final String name) {
        return this.data.get(name) != null;
    }

    /**
     * Return <code>true</code> if the field has a null value.
     *
     * @return <code>true</code> if field is null
     */
    @Override
    public boolean isNull(final String name) {
        return this.data.get(name) == null;
    }

    /**
     * Set the value of a field.
     *
     * @param name the name of the field
     * @param value the value of the field
     */
    @Override
    public void setValue(final String name, final Object value) {
        this.data.put(name, value);
    }

    /**
     * Return the size of the values.
     *
     * @return the size of the values
     */
    @Override
    public int size() {
        return this.data.size();
    }
}
