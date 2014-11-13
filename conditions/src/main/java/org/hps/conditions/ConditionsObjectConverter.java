package org.hps.conditions;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.hps.conditions.ConditionsRecord.ConditionsRecordCollection;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * <p>
 * Implementation of default conversion from database tables to a
 * {@link ConditionsObject} class.
 * </p>
 * <p>
 * This class actually returns collections and not individual objects.
 * </p>
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * 
 * @param <T> The type of the returned data which should be a class extending
 *            {@link ConditionsObjectCollection}.
 */
// FIXME: This class should only allow one collection to be returned and not mix
// the database records together.
// TODO: This class can probably be removed in favor of using the
// ConditionsSeriesConverter in all cases.
public abstract class ConditionsObjectConverter<T> implements ConditionsConverter<T> {

    public ConditionsObjectConverter() {
    }

    /**
     * Classes that extend this must define this method to specify what type the
     * converter is able to handle.
     * @return The Class that this converter handles.
     */
    public abstract Class getType();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T getData(ConditionsManager conditionsManager, String name) {

        // System.out.println("finding conditions for key " + name + " ...");

        // Get the DatabaseConditionsManager which is required for using this converter.
        DatabaseConditionsManager databaseConditionsManager = DatabaseConditionsManager.getInstance();

        // Get the table meta data from the key given by the caller.
        TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);
        if (tableMetaData == null)
            throw new RuntimeException("Table meta data for " + name + " was not found.");

        // Create a collection to return.
        ConditionsObjectCollection collection = ConditionsObjectUtil.createCollection(tableMetaData);

        // Get the ConditionsRecord with the meta-data, which will use the
        // current run
        // number from the manager.
        ConditionsRecordCollection conditionsRecords = databaseConditionsManager.findConditionsRecords(name);

        if (conditionsRecords.getObjects().size() == 0) {
            // There were no records returned, which is a fatal error.
            throw new RuntimeException("No conditions found with key: " + name);
        } else if (conditionsRecords.getObjects().size() > 1) {
            // There were multiple records returned.
            if (!allowMultipleCollections())
                // If there are multiple records returned but this is not
                // allowed by the
                // converter, then this is a fatal error.
                throw new RuntimeException("Multiple conditions records returned but this is not allowed.");
        } else {
            // There was a single conditions record so the collection
            // information can be
            // set meaningfully.
            try {
                collection.setCollectionId(conditionsRecords.get(0).getCollectionId());
                collection.setTableMetaData(tableMetaData);
                collection.setConditionsRecord(conditionsRecords.get(0));
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        }

        // Loop over conditions records. This will usually just be one record.
        for (ConditionsRecord conditionsRecord : conditionsRecords.getObjects()) {

            // Get the table name.
            String tableName = conditionsRecord.getTableName();

            // Get the collection ID.
            int collectionId = conditionsRecord.getCollectionId();

            // Build a select query.
            String query = QueryBuilder.buildSelect(tableName, collectionId, tableMetaData.getFieldNames(), "id ASC");

            // Query the database.
            ResultSet resultSet = databaseConditionsManager.selectQuery(query);

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
        }

        // Return new collection.
        return (T) collection;
    }

    public boolean allowMultipleCollections() {
        return true;
    }
}
