package org.hps.conditions.database;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import org.hps.conditions.api.BaseConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.FieldValueMap;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * <p>
 * Implementation of default conversion from database tables to a {@link ConditionsObject} class.
 * <p>
 * This class actually returns collections and not individual objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @param <T> The type of the returned data which should be a class extending {@link BaseConditionsObjectCollection}.
 */
public abstract class AbstractConditionsObjectConverter<T> implements ConditionsConverter<T> {

    /**
     * The action to take if multiple overlapping conditions sets are found.
     * The default is using the most recently updated one.
     */
    private MultipleCollectionsAction multipleCollections = MultipleCollectionsAction.LAST_UPDATED;

    /**
     * Class constructor.
     */
    public AbstractConditionsObjectConverter() {
    }

    /**
     * Set the action that the converter will use to disambiguate when multiple conditions sets are found.
     *
     * @param multipleCollections the multiple collections action
     */
    final void setMultipleCollectionsAction(MultipleCollectionsAction multipleCollections) {
        this.multipleCollections = multipleCollections;
    }

    /**
     * Get the multiple collections action.
     *
     * @return the multiple collections action
     */
    public final MultipleCollectionsAction getMultipleCollectionsAction() {
        return this.multipleCollections;
    }

    /**
     * Get the specific type converted by this class.
     *
     * @return the class that this converter handles
     */
    public abstract Class<T> getType();

    /**
     * Get the conditions data based on the name, e.g. "ecal_channels". The table information is found using the type
     * handled by the Converter.
     * 
     * @param conditionsManager the current conditions manager
     * @param name the name of the conditions set (maps to table name)
     * @return the conditions data
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public T getData(final ConditionsManager conditionsManager, final String name) {

        // Get the DatabaseConditionsManager which is required for using this converter.
        final DatabaseConditionsManager databaseConditionsManager = (DatabaseConditionsManager) conditionsManager;

        // Setup connection if necessary.
        boolean reopenedConnection = false;
        if (!databaseConditionsManager.isConnected()) {
            // Open a connection to the database.
            databaseConditionsManager.openConnection();
            reopenedConnection = true;
        }

        // Get the TableMetaData from the table name.
        final TableMetaData tableMetaData = databaseConditionsManager.findTableMetaData(name);

        // Throw an exception if the table name does not map to a known type.
        if (tableMetaData == null) {
            throw new RuntimeException(new ConditionsObjectException("No table information found for name: " + name));
        }

        // Get the ConditionsRecordCollection with the run number assignments.
        final ConditionsRecordCollection conditionsRecords = databaseConditionsManager.findConditionsRecords(name);

        // The record with the collection information.
        ConditionsRecord conditionsRecord = null;

        // Now we need to determine which ConditionsRecord object to use.
        if (conditionsRecords.size() == 0) {
            // No conditions records were found for the key.
            // FIXME: This should possibly just return an empty collection instead.
            throw new RuntimeException("No conditions were found with key: " + name);
        } else if (conditionsRecords.size() == 1) {
            // Use the single conditions set that was found.
            conditionsRecord = conditionsRecords.get(0);
        } else if (conditionsRecords.size() > 1) {
            if (multipleCollections.equals(MultipleCollectionsAction.LAST_UPDATED)) {
                // Use the conditions set with the latest updated date.
                conditionsRecord = conditionsRecords.sortedByUpdated().get(conditionsRecords.size() - 1);
            } else if (multipleCollections.equals(MultipleCollectionsAction.LAST_CREATED)) {
                // Use the conditions set with the latest created date.
                conditionsRecord = conditionsRecords.sortedByCreated().get(conditionsRecords.size() - 1);
            } else if (multipleCollections.equals(MultipleCollectionsAction.LATEST_RUN_START)) {
                // Use the conditions set with the greatest run start value.
                conditionsRecord = conditionsRecords.sortedByRunStart().get(conditionsRecords.size() - 1);
            } else if (multipleCollections.equals(MultipleCollectionsAction.ERROR)) {
                // The converter has been configured to throw an error.
                throw new RuntimeException("Multiple ConditionsRecord object found for conditions key " + name);
            }
        }

        // Create a collection of objects to return.
        ConditionsObjectCollection collection = null;
        try {
            collection = createCollection(conditionsRecord, tableMetaData);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }

        DatabaseConditionsManager.getLogger().info("loading conditions set..." + '\n' + conditionsRecord);

        // Get the table name.
        final String tableName = conditionsRecord.getTableName();

        // Get the collection ID.
        final int collectionId = conditionsRecord.getCollectionId();

        // Build a select query.
        final String query = QueryBuilder.buildSelect(tableName, collectionId, tableMetaData.getFieldNames(), "id ASC");

        // Query the database to get the collection's rows.
        final ResultSet resultSet = databaseConditionsManager.selectQuery(query);

        try {
            // Loop over the rows.
            while (resultSet.next()) {
                // Create a new ConditionsObject from this row.
                final ConditionsObject newObject = createConditionsObject(resultSet, tableMetaData);

                // Add the object to the collection.
                collection.add(newObject);
            }
        } catch (SQLException e) {
            // Some kind of database error occurred.
            throw new RuntimeException(e);
        }

        // Close the Statement and the ResultSet.
        DatabaseUtilities.cleanup(resultSet);

        if (reopenedConnection) {
            // Close connection if one was opened.
            databaseConditionsManager.closeConnection();
        }

        return (T) collection;
    }

    /**
     * Create a conditions object.
     * 
     * @param resultSet the database record
     * @param tableMetaData the table data for the object
     * @return the conditions object
     * @throws SQLException if there is a problem using the database
     */
    static final ConditionsObject createConditionsObject(final ResultSet resultSet, final TableMetaData tableMetaData)
            throws SQLException {
        final ResultSetMetaData metaData = resultSet.getMetaData();
        final int rowId = resultSet.getInt(1);
        final int nCols = metaData.getColumnCount();
        FieldValueMap fieldValues = new FieldValueMap();
        for (int i = 2; i <= nCols; i++) {
            fieldValues.put(metaData.getColumnName(i), resultSet.getObject(i));
        }
        ConditionsObject newObject = null;
        try {
            newObject = tableMetaData.getObjectClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        try {
            newObject.setRowID(rowId);
        } catch (ConditionsObjectException e) {
            throw new RuntimeException(e);
        }
        newObject.setFieldValues(fieldValues);
        return newObject;
    }

    /**
     * Create a conditions object collection.
     *
     * @param conditionsRecord the conditions record
     * @param tableMetaData the table data
     * @return the conditions object collection
     * @throws ConditionsObjectException if there is a problem creating the collection
     */
    static final BaseConditionsObjectCollection<?> createCollection(final ConditionsRecord conditionsRecord,
            final TableMetaData tableMetaData) throws ConditionsObjectException {
        BaseConditionsObjectCollection<?> collection;
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

    /**
     * Convert object to string.
     * @return the object converted to string
     */
    public String toString() {
        return "ConditionsObjectConverter: type = " + this.getType() + ", multipleCollectionsAction = "
                + this.getMultipleCollectionsAction().toString();
    }
}
