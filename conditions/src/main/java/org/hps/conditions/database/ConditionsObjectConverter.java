package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.hps.conditions.api.AbstractConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.FieldValueMap;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * <p>
 * Implementation of default conversion from database tables to a
 * {@link ConditionsObject} class.
 * <p>
 * This class actually returns collections and not individual objects.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 * @param <T> The type of the returned data which should be a class extending {@link AbstractConditionsObjectCollection}.
 */
public abstract class ConditionsObjectConverter<T> implements ConditionsConverter<T> {

    // This is the strategy used for disambiguating multiple overlapping conditions sets.
    enum MultipleRecordsStrategy {
        LAST_UPDATED,
        LAST_CREATED,
        LATEST_RUN_START,
        COMBINE,
        ERROR
    }
    
    MultipleRecordsStrategy multiStrat = MultipleRecordsStrategy.LAST_UPDATED;
    
    public ConditionsObjectConverter() {
    }
    
    public void setMultipleRecordsStrategy(MultipleRecordsStrategy multiStrat) {
        this.multiStrat = multiStrat;
    }

    /**
     * Child classes must extend this method to specify what type the converter handles.
     * @return The Class that this converter handles.
     */
    public abstract Class<T> getType();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T getData(ConditionsManager conditionsManager, String name) {
        
        // Get the DatabaseConditionsManager which is required for using this converter.
        DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.getInstance();
        
        // Get the table meta data from the key given by the caller.
        TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);
        if (tableMetaData == null)
            throw new RuntimeException("Table meta data for " + name + " was not found.");

        // Get the ConditionsRecordCollection with the run number assignments.
        ConditionsRecordCollection conditionsRecords = databaseConditionsManager.findConditionsRecords(name);
        
        // By default use all conditions records, which works if there is a single one, or if the COMBINE strategy is being used.
        ConditionsRecordCollection filteredConditionsRecords = new ConditionsRecordCollection();
        filteredConditionsRecords.addAll(conditionsRecords);
        
        // Now we need to determine which ConditionsRecord objects to use according to configuration.
        if (conditionsRecords.size() == 0) {
            // No conditions records were found for the key.
            throw new RuntimeException("No conditions were found with key " + name);
        } else {           
            if (multiStrat.equals(MultipleRecordsStrategy.LAST_UPDATED)) {
                // Use the conditions set with the latest updated date.
                filteredConditionsRecords = conditionsRecords.sortedByUpdated();
            } else if (multiStrat.equals(MultipleRecordsStrategy.LAST_CREATED)){
                // Use the conditions set with the latest created date.
                filteredConditionsRecords = conditionsRecords.sortedByCreated();                
            } else if (multiStrat.equals(MultipleRecordsStrategy.LATEST_RUN_START)) {
                // Use the conditions set with the greatest run start value.
                filteredConditionsRecords = conditionsRecords.sortedByRunStart();
            } else if (multiStrat.equals(MultipleRecordsStrategy.ERROR)) {            
                // The converter has been configured to throw an error when this happens!
                throw new RuntimeException("Multiple ConditionsRecord object found for conditions key " + name);
            }           
        }             
        
        // Create a collection of objects to to return.        
        AbstractConditionsObjectCollection collection = null;
        
        try {
            ConditionsRecord conditionsRecord = null;
            if (filteredConditionsRecords.size() == 1) {
                // If there is a single ConditionsRecord, then it can be assigned to the collection.
                conditionsRecord = filteredConditionsRecords.get(0);
            }
            collection = createCollection(conditionsRecord, tableMetaData);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
   
        // Open a database connection.
        databaseConditionsManager.openConnection();
        
        // Loop over all records, which could just be a single one.
        for (ConditionsRecord conditionsRecord : filteredConditionsRecords) {
        
            // Get the table name.
            String tableName = conditionsRecord.getTableName();

            // Get the collection ID.
            int collectionId = conditionsRecord.getCollectionId();

            // Build a select query.
            String query = QueryBuilder.buildSelect(tableName, collectionId, tableMetaData.getFieldNames(), "id ASC");

            // Query the database to get the conditions collection's rows.
            ResultSet resultSet = databaseConditionsManager.selectQuery(query);

            try {
                // Loop over the rows.
                while (resultSet.next()) {
                    // Create a new ConditionsObject from this row.
                    ConditionsObject newObject = createConditionsObject(resultSet, tableMetaData);

                    // Add the object to the collection.
                    collection.add(newObject);
                }
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            
            // Close the Statement and the ResultSet.
            DatabaseUtilities.cleanup(resultSet);
        }
                
        // Close the database connection.
        databaseConditionsManager.closeConnection();
        
        return (T) collection;
    }
    
    static final ConditionsObject createConditionsObject(ResultSet resultSet, TableMetaData tableMetaData) throws SQLException {
        ResultSetMetaData metaData = resultSet.getMetaData();
        int rowId = resultSet.getInt(1);
        int ncols = metaData.getColumnCount();
        FieldValueMap fieldValues = new FieldValueMap();
        for (int i = 2; i <= ncols; i++) {
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
        newObject.setFieldValues(fieldValues);
        return newObject;
    }
    
    static final AbstractConditionsObjectCollection<?> createCollection(ConditionsRecord conditionsRecord, TableMetaData tableMetaData) throws ConditionsObjectException {
        AbstractConditionsObjectCollection<?> collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
            if (conditionsRecord != null) {
                collection.setConditionsRecord(conditionsRecord);
                collection.setTableMetaData(tableMetaData);
                collection.setCollectionId(conditionsRecord.getCollectionId());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConditionsObjectException("Error creating conditions object collection.", e);
        }
        return collection;
    }  
}
