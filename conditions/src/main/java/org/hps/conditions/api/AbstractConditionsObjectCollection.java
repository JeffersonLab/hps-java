package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.LinkedHashSet;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

public class AbstractConditionsObjectCollection<ObjectType extends ConditionsObject> extends LinkedHashSet<ObjectType> {

    protected TableMetaData tableMetaData = null;
    protected int collectionId = -1;
    protected ConditionsRecord conditionsRecord = null;
    
    public AbstractConditionsObjectCollection() {
    }
    
    public AbstractConditionsObjectCollection(ConditionsRecord conditionsRecord, TableMetaData tableMetaData) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = conditionsRecord.getCollectionId();
    }
    
    public AbstractConditionsObjectCollection(ConditionsRecord conditionsRecord, TableMetaData tableMetaData, int collectionID) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionID;
    }
    
    public void setTableMetaData(TableMetaData tableMetaData) {
        /**
         * Setting this more than once is disallowed.
         */
        if (this.tableMetaData != null) {
            throw new RuntimeException("The table meta data cannot be reset.");
        }
        this.tableMetaData = tableMetaData;
    }
    
    public void setConditionsRecord(ConditionsRecord conditionsRecord) {
        /**
         * Setting this more than once is disallowed.
         */
        if (this.conditionsRecord != null) {
            throw new RuntimeException("The table meta data cannot be reset.");
        }
        this.conditionsRecord = conditionsRecord;
    }
    
    public boolean add(ObjectType object) {
        if (contains(object)) {
            throw new IllegalArgumentException("Cannot add duplicate object " + object);
        }
        return super.add(object);
    }

    public TableMetaData getTableMetaData() {
        return tableMetaData;
    }

    public int getCollectionId() {
        return collectionId;
    }
    
    public ConditionsRecord getConditionsRecord() {
        return conditionsRecord;
    }
        
    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (collectionId != -1) {
            throw new ConditionsObjectException("The collectionId already has the value " + collectionId + " and cannot be reset."); 
        }
        this.collectionId = collectionId;
    }
    
    public void insert() throws ConditionsObjectException, SQLException {
        DatabaseConditionsManager.getInstance().insertCollection(this);
    }
    
    // Should select all records into the collection by collection ID.
    public int select() {
        System.out.println("implement me");
        return -1;
    }
    
    // Should delete all records by collection ID in the database and clear the local objects.
    public int delete() {
        System.out.println("implement me");
        return -1;
    }
    
    // Should update objects in the database with their values in the set.
    public int update() {
        System.out.println("implement me");
        return -1;
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (ConditionsObject object : this) {
            buffer.append(object.toString());
            buffer.append('\n');
        }
        return buffer.toString();
    }       
}