package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.ConditionsRecord.ConditionsRecordCollection;

/**
 * <p>
 * This converter creates a <tt>ConditionsSeries</tt> which is a set of <tt>ConditionsObjectCollection</tt>
 * objects with the same type.  This can be used to retrieve sets of conditions that may overlap in 
 * time validity, such as sets of bad channels .
 * </p>
 * <p>
 * Since type inference from the target variable is used in the {@link #createSeries(String)} method
 * signature, there only needs to be one of these converters per {@link DatabaseConditionsManager}.
 * The creation of the specific types is also done automatically, so each type of conditions object
 * does not need its own converter class.
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsSeriesConverter {

    DatabaseConditionsManager conditionsManager = null;
    
    ConditionsSeriesConverter(DatabaseConditionsManager conditionsManager) {
        if (conditionsManager == null)
            throw new RuntimeException("The conditionsManager is null.");
        this.conditionsManager = conditionsManager;
    }
        
    /**
     * Create a <tt>ConditionsSeries</tt> which is a series of <tt>ConditionsObjectCollections</tt> of the same type,
     * each of which have their own <tt>ConditionsRecord</tt>.  This should be used for overlapping conditions, such as
     * sets of bad channels that are combined together as in the test run.
     * 
     * @param conditionsKey The name of the conditions key to retrieve from the conditions table.
     * @return The <tt>ConditionsSeries</tt> matching <tt>conditionsKey</tt> which type inferred from target variable.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public <CollectionType extends ConditionsObjectCollection> ConditionsSeries<CollectionType> createSeries(String conditionsKey) {

        // Get the table meta data from the key given by the caller.
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(conditionsKey);
        if (tableMetaData == null)
            throw new RuntimeException("Table meta data for " + conditionsKey + " was not found.");

        ConditionsSeries<CollectionType> series = new ConditionsSeries<CollectionType>();

        // Get the ConditionsRecord with the meta-data, which will use the current run
        // number from the manager.
        ConditionsRecordCollection conditionsRecords = conditionsManager.findConditionsRecords(conditionsKey);

        // Loop over conditions records. This will usually just be one record.
        for (ConditionsRecord conditionsRecord : conditionsRecords.getObjects()) {
            
            ConditionsObjectCollection collection = ConditionsObjectUtil.createCollection(tableMetaData);

            try {
                collection.setCollectionId(conditionsRecord.getCollectionId());
                collection.setConditionsRecord(conditionsRecords.get(0));
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
            collection.setTableMetaData(tableMetaData);

            // Get the table name.
            String tableName = conditionsRecord.getTableName();

            // Get the collection ID.
            int collectionId = conditionsRecord.getCollectionId();

            // Build a select query.
            String query = QueryBuilder.buildSelect(tableName, collectionId, tableMetaData.getFieldNames(), "id ASC");

            // Query the database.
            ResultSet resultSet = conditionsManager.selectQuery(query);

            try {
                // Loop over rows.
                while (resultSet.next()) {
                    // Create new ConditionsObject.
                    ConditionsObject newObject = ConditionsObjectUtil.createConditionsObject(resultSet, tableMetaData);

                    // Add new object to collection, which will also assign it a
                    // collection ID if applicable.
                    collection.add(newObject);                                       
                }
            } catch (SQLException | ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
            
            series.addCollection((CollectionType)collection);
        }

        // Return new collection.
        return series;
    }
}
