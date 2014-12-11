package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This class implements a collection API for ConditionsObjects, using a <code>LinkedHashSet</code>.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @param <ObjectType> The concrete type of the collection class.
 */
public class AbstractConditionsObjectCollection<ObjectType extends ConditionsObject> extends LinkedHashSet<ObjectType> {

    protected TableMetaData tableMetaData = null;
    protected int collectionId = -1;
    protected ConditionsRecord conditionsRecord = null;
    
    /**
     * This is the no argument constructor that would be used when creating a new collection
     * that is not in the database.
     */
    public AbstractConditionsObjectCollection() {
    }
    
    /**
     * This constructor uses the given conditions record and table meta data objects and will assign
     * the collection ID from the conditions record.
     * @param conditionsRecord
     * @param tableMetaData
     */
    public AbstractConditionsObjectCollection(ConditionsRecord conditionsRecord, TableMetaData tableMetaData) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = conditionsRecord.getCollectionId();
    }
    
    /**
     * This constructor is used to explicitly assign all class variable values.
     * @param conditionsRecord
     * @param tableMetaData
     * @param collectionID
     */
    public AbstractConditionsObjectCollection(ConditionsRecord conditionsRecord, TableMetaData tableMetaData, int collectionID) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionID;
    }
    
    /**
     * Set the associated table meta data for this collection.  
     * Once set it cannot be reassigned, which will cause an exception to be thrown.
     * @param tableMetaData
     */
    public void setTableMetaData(TableMetaData tableMetaData) {                              
        if (this.tableMetaData != null) {
            throw new RuntimeException("The table meta data cannot be reset once assigned.");
        }
        this.tableMetaData = tableMetaData;
    }
    
    /**
     * Set the associated conditions record this collection.
     * Once set it cannot be reassigned, which will cause an exception to be thrown.
     * @param conditionsRecord
     */
    public void setConditionsRecord(ConditionsRecord conditionsRecord) {
        if (this.conditionsRecord != null) {
            throw new RuntimeException("The conditions record cannot be reset once assigned.");
        }
        this.conditionsRecord = conditionsRecord;
    }
    
    /**
     * Add an object to the collection.
     */
    public boolean add(ObjectType object) {
        if (contains(object)) {
            throw new IllegalArgumentException("Cannot add duplicate object " + object + " to collection.");
        }
        return super.add(object);
    }

    /**
     * Get the table meta data.
     * @return
     */
    public TableMetaData getTableMetaData() {
        return tableMetaData;
    }

    /**
     * Get the collection ID.
     * @return
     */
    public int getCollectionId() {
        return collectionId;
    }
    
    /**
     * Get the conditions record.
     * @return
     */
    public ConditionsRecord getConditionsRecord() {
        return conditionsRecord;
    }
        
    /**
     * Set the collection ID.  
     * Once set it cannot be assign again, which will cause an exception.
     * @param collectionId
     * @throws ConditionsObjectException
     */
    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (this.collectionId != -1) {
            throw new ConditionsObjectException("The collectionId already has the value " + collectionId + " and cannot be reset."); 
        }
        this.collectionId = collectionId;
    }
        
    public void insert() throws ConditionsObjectException, SQLException {
        
        // TODO: First check here if conditions record and/or collection ID is assigned, 
        //       in which case an error should be thrown as this is not a new collection.
        
        DatabaseConditionsManager.getInstance().insertCollection(this);
    }
    
    // Should select records into this collection by collection ID.
    public int select() {
        throw new UnsupportedOperationException("The select operation is not implemented yet.");
    }
    
    // Should delete all records by collection ID in the database and then clear the local objects. 
    public int delete() {
        throw new UnsupportedOperationException("The delete operation is not implemented yet.");
    }
    
    // Should update objects in the database with their values from this collection.
    // All objects would need to have valid row IDs for this to work.
    public int update() {
        throw new UnsupportedOperationException("The update operation is not implemented yet.");
    }
    
    /**
     * Convert object to string.
     */
    public String toString() {
        // TODO: Should print out column headers here.
        StringBuffer buffer = new StringBuffer();
        for (ConditionsObject object : this) {
            buffer.append(object.toString());
            buffer.append('\n');
        }
        return buffer.toString();
    }       
}