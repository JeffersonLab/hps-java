package org.hps.conditions.api;

/**
 * This is an interface for connecting conditions information to a database.
 * <p>
 * Most of the functionality is derived from the {@link DatabaseObject} interface.
 *
 * @author Jeremy McCormick, SLAC
 */
public interface ConditionsObject extends DatabaseObject {

    /**
     * Return the collection ID of the object.
     *
     * @return the collection ID of the object
     */
    Integer getCollectionId();

    /**
     * Get the value of a field by casting to an explicit type.
     *
     * @param type the return type
     * @param name the name of the field
     * @return the field value
     */
    <T> T getFieldValue(final Class<T> type, final String name);

    /**
     * Get the value of a field by casting to an implicit type.
     *
     * @param name the name of the field
     * @return the field value
     */
    <T> T getFieldValue(final String name);

    /**
     * Get the field values for the object.
     *
     * @return the field values for the object
     */
    FieldValues getFieldValues();

    /**
     * Get the row ID of the object in the database.
     *
     * @return the row ID of the object in the database
     */
    int getRowId();

    /**
     * Return <code>true</code> if object has a valid collection ID.
     *
     * @return <code>true</code> if object has a valid collection ID
     */
    boolean hasValidCollectionId();

    /**
     * Set the value of a field.
     *
     * @param name the name of the field
     * @param value the new value of the field
     */
    void setFieldValue(String name, Object value);

    /**
     * Set all of the field values.
     *
     * @param fieldValues the new field values
     */
    void setFieldValues(FieldValues fieldValues);
}
