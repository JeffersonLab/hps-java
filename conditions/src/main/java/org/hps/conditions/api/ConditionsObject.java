package org.hps.conditions.api;

import java.util.Comparator;

/**
 * This is an ORM interface for accessing conditions database information by
 * row. It can handle new or existing records. The row ID values for new records are
 * -1 which indicates they are not in the database yet.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsObject {

    /**
     * Get the row ID of this object.
     * @return The database row ID.
     */
    int getRowId();

    /**
     * Generic set method for field values. This will set the object to the
     * 'dirty' state.
     * @param fieldName The name of the field.
     * @param fieldValue The field value.
     */
    void setFieldValue(String field, Object value);

    /**
     * Set all of the field values on this object.
     * @param fieldValues The FieldValueMap containing pairs of names and
     *            values.
     */
    void setFieldValues(FieldValueMap fieldValues);
    
    /**
     * Get the map of field values.
     * @return The <code>FieldValueMap</code>.
     */
    FieldValueMap getFieldValues();

    /**
     * Get a field value, cast to the given class.
     * @param field The field value.
     * @return The field value casted to type T.
     */
    public <T> T getFieldValue(Class<T> type, String field);

    /**
     * Get a field value with implicit return type.
     * @param field The field's name.
     * @return The field value cast to type.
     */
    public <T> T getFieldValue(String field);

    /**
     * Set the row ID of this object. This cannot be reset once set to a valid
     * ID (e.g. not -1).
     * @param rowId The object's row ID.
     * @throws ConditionsObjectException if already set
     */
    public void setRowId(int rowId) throws ConditionsObjectException;
    
    /**
     * Return true if this object is new, e.g. it does not have a valid row ID.
     * This means that it does not have a database record in its table.
     * @return True if record is new.
     */
    public boolean isNew();
    
    static class DefaultConditionsObjectComparator implements Comparator<ConditionsObject> {
        public int compare(ConditionsObject o1, ConditionsObject o2) {
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
