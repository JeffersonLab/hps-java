package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.logging.Logger;

import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.MultipleCollectionsAction;
import org.lcsim.conditions.ConditionsConverter;
import org.lcsim.conditions.ConditionsManager;

/**
 * Implementation of default conversion from database tables to a {@link ConditionsObject} class.
 * <p>
 * This class actually returns collections and not individual objects.
 *
 * @author Jeremy McCormick, SLAC
 * @param <T> The type of the returned data which should be a class extending {@link BaseConditionsObjectCollection}.
 */
// TODO: Move to conditions.database package (not an API class).
public abstract class AbstractConditionsObjectConverter<T> implements ConditionsConverter<T> {

    /**
     * Initialize the logger.
     */
    private static final Logger LOGGER = Logger.getLogger(AbstractConditionsObjectConverter.class.getPackage().getName());
    
    /**
     * Create a conditions object collection.
     *
     * @param manager the conditions manager
     * @param conditionsRecord the conditions record
     * @param tableMetaData the table data
     * @return the conditions object collection
     * @throws ConditionsObjectException if there is a problem creating the collection
     */
    private static ConditionsObjectCollection<?> createCollection(final DatabaseConditionsManager manager,
            final ConditionsRecord conditionsRecord, final TableMetaData tableMetaData)
            throws ConditionsObjectException {
        ConditionsObjectCollection<?> collection;
        try {
            collection = tableMetaData.getCollectionClass().newInstance();
            if (conditionsRecord != null) {
                collection.setConnection(manager.getConnection());
                collection.setTableMetaData(tableMetaData);
                collection.setCollectionId(conditionsRecord.getCollectionId());
            }
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ConditionsObjectException("Error creating conditions object collection.", e);
        }
        return collection;
    }

    /**
     * The action to take if multiple overlapping conditions sets are found. The default is using the most recently
     * updated one.
     */
    private MultipleCollectionsAction multipleCollections = MultipleCollectionsAction.LAST_CREATED;

    /**
     * Class constructor.
     */
    public AbstractConditionsObjectConverter() {
    }

    /**
     * Get the conditions data based on the name, e.g. "ecal_channels". The table information is found using the type
     * handled by the Converter.
     *
     * @param conditionsManager the current conditions manager
     * @param name the name of the conditions set (maps to table name)
     * @return the conditions data
     */
    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public T getData(final ConditionsManager conditionsManager, final String name) {

        // Get the DatabaseConditionsManager which is required for using this converter.
        final DatabaseConditionsManager databaseConditionsManager = (DatabaseConditionsManager) conditionsManager;

        // Setup connection if necessary.
        final boolean openedConnection = databaseConditionsManager.openConnection();

        // Get the TableMetaData from the table name.
        final TableMetaData tableMetaData = TableRegistry.getTableRegistry().findByTableName(name);

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
            throw new RuntimeException("No conditions were found with key: " + name);
        } else if (conditionsRecords.size() == 1) {
            // Use the single conditions set that was found.
            conditionsRecord = conditionsRecords.get(0);
        } else if (conditionsRecords.size() > 1) {
            if (this.multipleCollections.equals(MultipleCollectionsAction.LAST_UPDATED)) {
                // Use the conditions set with the latest updated date.
                conditionsRecord = conditionsRecords.sortedByUpdated().get(conditionsRecords.size() - 1);
            } else if (this.multipleCollections.equals(MultipleCollectionsAction.LAST_CREATED)) {
                // Use the conditions set with the latest created date.
                conditionsRecord = conditionsRecords.sortedByCreated().get(conditionsRecords.size() - 1);
            } else if (this.multipleCollections.equals(MultipleCollectionsAction.LATEST_RUN_START)) {
                // Use the conditions set with the greatest run start value.
                conditionsRecord = conditionsRecords.sortedByRunStart().get(conditionsRecords.size() - 1);
            } else if (this.multipleCollections.equals(MultipleCollectionsAction.ERROR)) {
                // The converter has been configured to throw an error.
                throw new RuntimeException("Multiple ConditionsRecord object found for conditions key " + name);
            }
        }

        // Create a collection of objects to return.
        ConditionsObjectCollection collection = null;
        try {
            collection = createCollection(databaseConditionsManager, conditionsRecord, tableMetaData);
        } catch (final ConditionsObjectException e) {
            throw new RuntimeException(e);
        }

        LOGGER.info("loading conditions set..." + '\n' + conditionsRecord);

        // Select the objects into the collection by the collection ID.
        try {
            collection.select(conditionsRecord.getCollectionId());
        } catch (DatabaseObjectException | SQLException e) {
            throw new RuntimeException("Error creating conditions collection from table " + name
                    + " with collection ID " + conditionsRecord.getCollectionId(), e);
        }

        if (openedConnection) {
            // Close connection if one was opened.
            databaseConditionsManager.closeConnection();
        }

        return (T) collection;
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
    @Override
    public abstract Class<T> getType();

    /**
     * Set the action that the converter will use to disambiguate when multiple conditions sets are found.
     *
     * @param multipleCollections the multiple collections action
     */
    public final void setMultipleCollectionsAction(final MultipleCollectionsAction multipleCollections) {
        this.multipleCollections = multipleCollections;
    }

    /**
     * Convert object to string.
     *
     * @return the object converted to string
     */
    @Override
    public String toString() {
        return "ConditionsObjectConverter: type = " + this.getType() + ", multipleCollectionsAction = "
                + this.getMultipleCollectionsAction().toString();
    }
}
