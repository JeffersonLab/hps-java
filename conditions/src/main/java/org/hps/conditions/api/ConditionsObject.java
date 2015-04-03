package org.hps.conditions.api;

import java.util.Comparator;

/**
 * This is an ORM interface for accessing conditions information by row from a database table.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu>Jeremy McCormick</a>
 */
public interface ConditionsObject {

    /**
     * Get the row ID of this object, which will be -1 for records not in the database.
     *
     * @return The database row ID.
     */
    int getRowId();

    /**
     * Set the value of a field.
     *
     * @param field The name of the field.
     * @param value The field value.
     */
    void setFieldValue(String field, Object value);

    /**
     * Set all of the field values on this object.
     *
     * @param fieldValues The map containing pairs of field names and values.
     */
    void setFieldValues(FieldValueMap fieldValues);

    /**
     * Get the map of field values.
     *
     * @return The <code>FieldValueMap</code>.
     */
    FieldValueMap getFieldValues();

    /**
     * Get a field value, cast to the given class.
     *
     * @param field The field value.
     * @param type The class of the field.
     * @param <T> The inferred type of the field.
     * @return The field value casted to type T.
     */
    <T> T getFieldValue(Class<T> type, String field);

    /**
     * Get a field value with implicit return type.
     *
     * @param field The field's name.
     * @param <T> The inferred type of the field.
     * @return The field value cast to type.
     */
    <T> T getFieldValue(String field);

    /**
     * Set the row ID of this object. This cannot be reset once set to a valid ID (e.g. not -1).
     *
     * @param rowId The object's row ID.
     * @throws ConditionsObjectException if already set
     */
    void setRowID(int rowId) throws ConditionsObjectException;

    /**
     * Return true if this object is new, e.g. it does not have a valid row ID. This means that it does not have a
     * database record in its table.
     *
     * @return True if record is new.
     */
    boolean isNew();

    /**
     * Default comparator for this interface which uses row ID.
     */
    static class DefaultConditionsObjectComparator implements Comparator<ConditionsObject> {

        /**
         * Compare objects according to standard Java conventions.
         *
         * @param o1 The first object.
         * @param o2 The second object.
         * @return The result of comparison operation.
         */
        public int compare(final ConditionsObject o1, final ConditionsObject o2) {
            if (o1.getRowId() < o2.getRowId()) {
                return -1;
            } else if (o1.getRowId() > o2.getRowId()) {
                return 1;
            } else {
                return 0;
            }
        }
    }
}
