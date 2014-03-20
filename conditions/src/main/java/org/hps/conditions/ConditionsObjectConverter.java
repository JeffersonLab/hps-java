package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hps.conditions.AbstractConditionsObject.FieldValueMap;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

public abstract class ConditionsObjectConverter<T> implements ConditionsConverter<T> {
    
    public ConditionsObjectConverter() {        
    }
    
    /**
     * Classes that extend this must define this method to specify what type the converter
     * is able to handle.
     * @return The Class that this converter handles.
     */
    public abstract Class getType();
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T getData(ConditionsManager conditionsManager, String name) {
        
        // This type of converter only works with the DatabaseConditionsManager class.
        DatabaseConditionsManager databaseConditionsManager = null;
        if (conditionsManager instanceof DatabaseConditionsManager) {
            databaseConditionsManager = (DatabaseConditionsManager)conditionsManager;
        } else {
            throw new RuntimeException("This converter requires a ConditionsManager of type DatabaseConditionsManager.");
        }
        
        // Get the table meta data from the key given by the caller.
        ConditionsTableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);
        
        // Create a collection to return.
        ConditionsObjectCollection collection;
        try {
             collection = tableMetaData.getCollectionClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        
        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.        
        ConditionsRecordCollection conditionsRecords = ConditionsRecord.find(conditionsManager, name);
                
        if (conditionsRecords.size() == 0) {
            // There were no records returned, which is a fatal error.
            throw new RuntimeException("No conditions found with key: " + name);
        } else if (conditionsRecords.size() > 1) {            
            if (!allowMultipleCollections())
                // If there are multiple records returned but this is not allowed by the converter, 
                // then this is a fatal error.
                throw new RuntimeException("Multiple conditions records returned but this is not allowed.");
        } else {
            // The collection ID is only set on the collection object if all rows have the same
            // collection ID.  Otherwise, the collection contains a mix of objects with different 
            // collectionIDs and has no meaningful ID of its own.
            collection.setCollectionId(conditionsRecords.get(0).getCollectionId());
        }

        // Loop over conditions records.  This will usually just be one record.
        for (ConditionsRecord conditionsRecord : conditionsRecords) {
                    
            // Get the table name.
            String tableName = conditionsRecord.getTableName();
            
            // Get the collection ID.
            int collectionId = conditionsRecord.getCollectionId();
            
            // Build a select query.
            String query = QueryBuilder.buildSelect(tableName, collectionId, tableMetaData.getFieldNames(), "id ASC");
        
            // Query the database.
            ResultSet resultSet = ConnectionManager.getConnectionManager().query(query);
            
            try {
                // Loop over rows.
                while (resultSet.next()) {
                    // Create new ConditionsObject.
                    ConditionsObject newObject = createConditionsObject(resultSet, tableMetaData);
                    
                    // Add new object to collection, which will also assign it a collection ID if applicable.
                    collection.add(newObject);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }                 
        }
        
        // Return new collection.
        return (T)collection;
    }

    private ConditionsObject createConditionsObject(ResultSet resultSet, 
            ConditionsTableMetaData tableMetaData) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int rowId = resultSet.getInt(1);
        int ncols = metaData.getColumnCount();
        FieldValueMap fieldValues = new FieldValueMap();
        for (int i=2; i<=ncols; i++) {
            fieldValues.put(metaData.getColumnName(i), resultSet.getObject(i));
        }  
        ConditionsObject newObject = null;
        try {
             newObject = tableMetaData.getObjectClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            newObject.setRowId(rowId);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        try {
            newObject.setTableMetaData(tableMetaData);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        newObject.setFieldValues(fieldValues);
        return newObject;
    }
    
    public boolean allowMultipleCollections() {
        return false;
    }
}
