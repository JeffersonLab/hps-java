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
     * @return the table meta data
     */
    TableMetaData getTableMetaData();

    /**
     * Get the collection ID.
     *
     * @return the collection ID
     */
    int getCollectionId();

    /**
     * Get the conditions record.
     *
     * @return the conditions record
     */
    ConditionsRecord getConditionsRecord();

    /**
     * Set the collection ID. Once set it cannot be assigned again, which will cause an exception.
     *
     * @param collectionId the collection ID
     * @throws ConditionsObjectException if reassignment of the ID is attempted
     */
    void setCollectionId(int collectionId) throws ConditionsObjectException;

    /**
     * Insert all objects from the collection into the database.
     *
     * @throws ConditionsObjectException if there is a conditions object error
     * @throws SQLException if there is a SQL syntax or execution error
     */
    void insert() throws ConditionsObjectException, SQLException;

    /**
     * Select objects into this collection by collection ID.
     *
     * @return the number of rows selected
     */
    int select();

    /**
     * Delete objects in this from the database.
     *
     * @return the number of rows deleted
     */
    int delete();

    /**
     * Update rows in the database from these objects.
     *
     * @return the number of rows updated
     */
    int update();

    /**
     * Get an object by its index.
     *
     * @param index the index in the set
     * @return the object at the index
     * @throws IndexOutOfBoundsException if the index value is out of bounds
     */
    ObjectType get(int index);

    /**
     * Sort the collection in place.
     *
     * @param comparator the comparator to use for sorting
     */
    void sort(Comparator<ObjectType> comparator);

    /**
     * Get a sorted list of the objects, leaving original collection in place.
     *
     * @param comparator the comparator to use for the sort
     * @return a sorted list of the objects
     */
    BaseConditionsObjectCollection<ObjectType> sorted(Comparator<ObjectType> comparator);

    /**
     * Sort the collection in place.
     */
    void sort();

    /**
     * Get a new, sorted collection.
     *
     * @return the new sorted collection
     */
    BaseConditionsObjectCollection<ObjectType> sorted();
}
