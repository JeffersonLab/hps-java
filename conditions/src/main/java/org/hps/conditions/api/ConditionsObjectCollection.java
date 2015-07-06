package org.hps.conditions.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Comparator;

/**
 * Interface representing a collection of conditions objects.
 *
 * @author Jeremy McCormick, SLAC
 * @param <ObjectType> the type of the objects
 */
public interface ConditionsObjectCollection<ObjectType extends ConditionsObject> extends Iterable<ObjectType>,
        DatabaseObject {

    /**
     * Add an object to the collection.
     *
     * @param object the object to add to the collection
     * @return <code>true</code> if object was added successfully
     * @throws ConditionsObjectException if there was an error adding the object
     */
    boolean add(final ObjectType object) throws ConditionsObjectException;

    /**
     * Add all objects to the collection.
     *
     * @param collection the source collection with objects to add
     */
    void addAll(ConditionsObjectCollection<ObjectType> collection);

    /**
     * Clear the objects from this collection and reset its ID.
     * <p>
     * This has no effect on the underlying database values.
     */
    void clear();

    /**
     * Return <code>true</code> if collection contains this object.
     *
     * @param object the object to check
     * @return <code>true</code> if the collection contains the object
     */
    boolean contains(Object object);

    /**
     * Get an object by index.
     *
     * @param index the index of the object
     * @return the object
     */
    ObjectType get(final int index);

    /**
     * Get the collection ID.
     *
     * @return the collection ID
     */
    int getCollectionId();

    /**
     * Load collection from a CSV file.
     *
     * @param file the input CSV file
     * @param delimiter the field delimiter (leave blank for default which is comma-delimited)
     * @throws IOException if there is an error closing the reader
     * @throws FileNotFoundException if the input file does not exist
     * @throws ConditionsObjectException if there is an error creating a conditions object
     */
    void load(final File file, Character delimiter) throws IOException, FileNotFoundException,
            ConditionsObjectException;

    /**
     * Set the collection ID.
     *
     * @param collectionId the new collection ID
     */
    void setCollectionId(int collectionId);

    /**
     * Get the size of the collection.
     *
     * @return the size of the collection
     */
    int size();

    /**
     * Sort the collection in place.
     *
     * @param comparator the comparison operator to use for sorting
     */
    void sort(final Comparator<ObjectType> comparator);

    /**
     * Get a sorted copy of the collection, leaving the original in place.
     *
     * @param comparator the comparison operator to use
     * @return the sorted copy of the collection
     */
    ConditionsObjectCollection<ObjectType> sorted(final Comparator<ObjectType> comparator);

    /**
     * Write the collection contents to a text file.
     *
     * @param file the output text file
     * @param delimiter the field delimiter (leave blank for default which is comma-delimited)
     */
    void write(File file, Character delimiter) throws IOException;
}