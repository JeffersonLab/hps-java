package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Set;

import org.hps.conditions.database.TableMetaData;

/**
 * An interface representing a collection of conditions objects.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @param <ObjectType> The type of the conditions object contained in the collection.
 */
public interface ConditionsObjectCollection<ObjectType extends ConditionsObject> extends Set<ObjectType> {

    /**
     * Get the table meta data.
     *
     * @return The table meta data.
     */
    TableMetaData getTableMetaData();

    /**
     * Get the collection ID.
     *
     * @return The collection ID.
     */
    int getCollectionId();

    /**
     * Get the conditions record.
     *
     * @return The conditions record.
     */
    ConditionsRecord getConditionsRecord();

    /**
     * Set the collection ID. Once set it cannot be assigned again, which will cause an exception.
     *
     * @param collectionId The collection ID.
     * @throws ConditionsObjectException If reassignment is attempted.
     */
    void setCollectionId(int collectionId) throws ConditionsObjectException;

    /**
     * Insert all objects from the collection into the database.
     *
     * @throws ConditionsObjectException If there is a conditions object error.
     * @throws SQLException If there is a SQL syntax or execution error.
     */
    void insert() throws ConditionsObjectException, SQLException;

    /**
     * Select objects into this collection by collection ID.
     *
     * @return The number of rows selected.
     */
    int select();

    /**
     * Delete objects in this from the database.
     *
     * @return The number of rows deleted.
     */
    int delete();

    /**
     * Update rows in the database from these objects.
     *
     * @return The number of rows updated.
     */
    int update();

    /**
     * Get an object by its index.
     *
     * @param index The index in the set.
     * @return The object at the index.
     * @throws IndexOutOfBoundsException If the index value is invalid.
     */
    ObjectType get(int index);

    /**
     * Sort the collection in place.
     *
     * @param comparator The comparator to use for sorting.
     */
    void sort(Comparator<ObjectType> comparator);

    /**
     * Get a sorted list of the objects, leaving original collection in place.
     *
     * @param comparator The comparator to use for the sort.
     * @return A sorted list of the objects.
     */
    BaseConditionsObjectCollection<ObjectType> sorted(Comparator<ObjectType> comparator);

    /**
     * Sort the collection in place.
     */
    void sort();

    /**
     * Get a new sorted collection.
     *
     * @return The new sorted collection.
     */
    BaseConditionsObjectCollection<ObjectType> sorted();
}
