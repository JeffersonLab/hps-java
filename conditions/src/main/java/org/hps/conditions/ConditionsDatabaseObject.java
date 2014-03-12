package org.hps.conditions;

import java.sql.SQLException;

/**
 * This is an interface for accessing conditions database information by row.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public interface ConditionsDatabaseObject {
    
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
    int getSetId();
    
    /**
     * Update this row in the database using a SQL UPDATE statement.    
     */
    void update() throws ConditionsDatabaseObjectException;
    
    /**
     * Delete this object's row in the database using a SQL DELETE statement.
     */
    void delete() throws ConditionsDatabaseObjectException;
    
    /**
     * Insert this object into the database using a SQL INSERT statement.     
     */
    void insert() throws ConditionsDatabaseObjectException, SQLException;
    
    /**
     * Select data into this object from the database using a SQL SELECT statement.     
     */
    void select() throws ConditionsDatabaseObjectException, SQLException;
    
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
     * Exception type throw by methods in this interface.
     */
    public static final class ConditionsDatabaseObjectException extends Exception {
        public ConditionsDatabaseObjectException(String message) {
            super(message);
        }
    }    
}
