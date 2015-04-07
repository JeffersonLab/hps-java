package org.hps.conditions.api;

import java.util.Map.Entry;

/**
 * The basic implementation of {@link ConditionsObject}.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 */
public class BaseConditionsObject implements ConditionsObject {

    /**
     * The database row ID.
     */
    private int rowID = -1;

    /**
     * The map of field-value pairs.
     */
    private FieldValueMap fieldValues;

    /**
     * Constructor for sub-classing.
     */
    protected BaseConditionsObject() {
        fieldValues = new FieldValueMap();
    }

    /**
     * Get the row ID of this object.
     * <p>
     * Implements {@link ConditionsObject#getRowId()}.
     *
     * @return the row ID
     */
    @Override
    public final int getRowId() {
        return rowID;
    }

    /**
     * True if object is new e.g. not in the database.
     * <p>
     * Implements {@link ConditionsObject#isNew()}.
     *
     * @return <code>true</code> if object is new
     */
    @Override
    public final boolean isNew() {
        return rowID == -1;
    }

    /**
     * Set the value of a field.
     * <p>
     * Implements {@link ConditionsObject#setFieldValue(String, Object)}.
     *
     * @param key the name of the field
     * @param value the value of the field
     */
    @Override
    public final void setFieldValue(final String key, final Object value) {
        fieldValues.put(key, value);
    }

    /**
     * Set all field values using a {@link FieldValueMap}.
     * <p>
     * Implements {@link ConditionsObject#setFieldValues(FieldValueMap)}.
     *
     * @param fieldValues the list of key-value pairs
     */
    @Override
    public final void setFieldValues(final FieldValueMap fieldValues) {
        this.fieldValues = fieldValues;
    }

    /**
     * Get the value of a field.
     * <p>
     * Implements {@link ConditionsObject#getFieldValue(Class, String)}.
     *
     * @param klass the inferred return type
     * @param field the name of the field
     * @param <T> the type for inference of return type
     * @return the value of the field
     */
    @Override
    public final <T> T getFieldValue(final Class<T> klass, final String field) {
        return klass.cast(fieldValues.get(field));
    }

    /**
     * Get the field-value map.
     * <p>
     * Implements {@link ConditionsObject#getFieldValues()}.
     *
     * @return the field-value map
     */
    @Override
    public final FieldValueMap getFieldValues() {
        return this.fieldValues;
    }

    /**
     * Get a field value.
     *
     * @param field the field name
     * @param <T> the type inferred from the assigned variable
     * @return the field value
     */
    @SuppressWarnings("unchecked")
    public final <T> T getFieldValue(final String field) {
        return (T) fieldValues.get(field);
    }

    /**
     * Set the database row ID of the object.
     *
     * @param rowId the database row ID
     * @throws ConditionsObjectException if the object already has a row ID
     */
    public final void setRowID(final int rowId) throws ConditionsObjectException {
        if (!isNew()) {
            throw new ConditionsObjectException("The row ID cannot be reassigned on an existing object.");
        }
        this.rowID = rowId;
    }

    /**
     * Convert this object to a string, which is a tab-separated row appropriate
     * for display in a table for console output.
     *
     * @return The object converted to a string.
     */
    public String toString() {
        final StringBuffer sb = new StringBuffer();
        sb.append(this.getRowId());
        sb.append('\t');
        for (Entry<String, Object> entries : this.getFieldValues().entrySet()) {
            sb.append(entries.getValue());
            sb.append('\t');
        }
        return sb.toString();
    }
}
