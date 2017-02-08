package org.hps.conditions.database;

import java.sql.SQLException;

import org.hps.conditions.api.ConditionsObject;
import org.hps.conditions.api.ConditionsObjectCollection;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.api.ConditionsSeries;
import org.hps.conditions.api.DatabaseObjectException;
import org.hps.conditions.api.TableMetaData;

/**
 * This converter creates a {@link org.hps.conditions.api.ConditionsSeries} which is a list of
 * {@link org.hps.conditions.api.ConditionsObjectCollection} objects having the same type. This can be used to retrieve
 * sets of conditions that may overlap in time validity. The user may then use whichever collections are of interest to
 * them.
 *
 * @see org.hps.conditions.api.ConditionsSeries
 * @see org.hps.conditions.api.ConditionsObjectCollection
 * @see org.hps.conditions.api.ConditionsObject
 * @see DatabaseConditionsManager
 * @param <ObjectType> The type of the ConditionsObject.
 * @param <CollectionType> The type of the collection.
 */
final class ConditionsSeriesConverter<ObjectType extends ConditionsObject, CollectionType extends ConditionsObjectCollection<ObjectType>> {

    /**
     * The type of the collection.
     */
    final Class<CollectionType> collectionType;

    /**
     * The type of the object.
     */
    final Class<ObjectType> objectType;

    /**
     * Class constructor.
     *
     * @param objectType the type of the object
     * @param collectionType the type of the collection
     */
    ConditionsSeriesConverter(final Class<ObjectType> objectType, final Class<CollectionType> collectionType) {
        this.collectionType = collectionType;
        this.objectType = objectType;
    }

    /**
     * Create a new conditions series.
     *
     * @param tableName the name of the data table
     * @return the conditions series
     */
    @SuppressWarnings({"unchecked"})
    final ConditionsSeries<ObjectType, CollectionType> createSeries(final String tableName) {

        if (tableName == null) {
            throw new IllegalArgumentException("The tableName argument is null.");
        }

        final DatabaseConditionsManager conditionsManager = DatabaseConditionsManager.getInstance();

        // Setup connection if necessary.
        boolean reopenedConnection = false;
        if (!conditionsManager.isConnected()) {
            conditionsManager.openConnection();
            reopenedConnection = true;
        }

        // Get the table meta data for the collection type.
        final TableMetaData tableMetaData = conditionsManager.findTableMetaData(tableName);
        if (tableMetaData == null) {
            throw new RuntimeException("Table meta data for " + this.collectionType + " was not found.");
        }

        // Create a new conditions series.
        final ConditionsSeries<ObjectType, CollectionType> series = new ConditionsSeries<ObjectType, CollectionType>();

        // Get the ConditionsRecord with the meta-data, which will use the current run number from the manager.
        final ConditionsRecordCollection conditionsRecords = conditionsManager.findConditionsRecords(tableName);

        for (final ConditionsRecord conditionsRecord : conditionsRecords) {

            ConditionsObjectCollection<?> collection = null;
            try {
                collection = tableMetaData.getCollectionClass().newInstance();
            } catch (InstantiationException | IllegalAccessException e1) {
                throw new RuntimeException(e1);
            }
            try {
                collection.setTableMetaData(tableMetaData);
                collection.setConnection(conditionsManager.getConnection());
                collection.select(conditionsRecord.getCollectionId());
            } catch (final DatabaseObjectException | SQLException e) {
                throw new RuntimeException(e);
            }
            series.add((ConditionsObjectCollection<ObjectType>) collection);
        }

        if (reopenedConnection) {
            conditionsManager.closeConnection();
        }

        // Return new collection.
        return series;
    }
}
