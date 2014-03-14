package org.hps.conditions;

import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsDatabaseObject.FieldValueMap;

/**
 * This is an interface for accessing conditions database information by row.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsObject {
    
    /**
     * Get the database table meta data associated to this object.
     * @return The database table meta data associated to this object.
     */
    ConditionsTableMetaData getTableMetaData();
    
    /**
     * Get the row ID of this object.
     * @return The database row ID.
     */
    int getRowId();
    
    /**
     * Get the set ID of this object identifying its collection. 
     * @return The collection ID.
     */
    int getCollectionId();
    
    /**
     * Update this row in the database using a SQL UPDATE statement.    
     */
    void update() throws ConditionsObjectException;
    
    /**
     * Delete this object's row in the database using a SQL DELETE statement.
     */
    void delete() throws ConditionsObjectException;
    
    /**
     * Insert this object into the database using a SQL INSERT statement.     
     */
    void insert() throws ConditionsObjectException, SQLException;
    
    /**
     * Select data into this object from the database using a SQL SELECT statement.     
     */
    void select() throws ConditionsObjectException, SQLException;
    
    /**
     * Return true if this object is read-only.
     * @return True if object is read-only.
     */
    boolean isReadOnly();
    
    /**
     * Return true if this object is new and hasn't been inserted into the database yet.
     * @return True if object is new.
     */
    boolean isNew();
    
    /**
     * Return true if this object's data has been modified without a database update.
     * @return True if object is dirty.
     */
    boolean isDirty();
        
    /**
     * Generic set method.  This will set the object to the 'dirty' state.
     * @param fieldName The name of the field.
     * @param fieldValue The field value.
     */
    void setFieldValue(String field, Object value);
    
    /**
     * Set all of the field values on this object.
     * @param fieldValues The FieldValueMap containing pairs of names and values.
     */
    void setFieldValues(FieldValueMap fieldValues);
    
    /**
     * Get a field value cast to the given type.
     * @param field The field value.
     * @return The field value casted to type T.
     */
    public <T> T getFieldValue(Class<T> klass, String field);
    
    /**
     * Set the ConnectionManager of this object.
     * @param connectionManager The ConnectionManager.
     */
    void setConnectionManager(ConnectionManager connectionManager);

    /**
     * Set the ConditionsTableMetaData of this object.
     * @param tableMetaData The ConditionsTableMetaData.
     */
    void setTableMetaData(ConditionsTableMetaData tableMetaData);
    
    /**
     * Set the collection ID of this object.
     * @param collectionId The collection ID.
     */
    void setCollectionId(int collectionId);
    
    /**
     * Generic Exception type throw by methods in this interface.
     */
    public static final class ConditionsObjectException extends Exception {
        public ConditionsObjectException(String message) {
            super(message);
        }
    }    
}
