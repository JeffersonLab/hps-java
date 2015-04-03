package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.hps.conditions.api.ConditionsObject.DefaultConditionsObjectComparator;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

/**
 * This class implements a collection API for ConditionsObjects, using a <code>LinkedHashSet</code>.
 *
 * @author <a href="mailto:jeremym@slac.stanford.edu">Jeremy McCormick</a>
 * @param <ObjectType> The concrete type of the collection class.
 */
@SuppressWarnings("serial")
public class BaseConditionsObjectCollection<ObjectType extends ConditionsObject> extends LinkedHashSet<ObjectType>
        implements ConditionsObjectCollection<ObjectType> {

    /**
     * The associated table meta data.
     */
    private TableMetaData tableMetaData = null;

    /**
     * The collection ID which is -1 if the collection is not in the database.
     */
    private int collectionId = -1;

    /**
     * The associated conditions record information including run validity.
     */
    private ConditionsRecord conditionsRecord = null;

    /**
     * This is the no argument constructor that would be used when creating a new collection that is not in the
     * database.
     */
    public BaseConditionsObjectCollection() {
    }

    /**
     * This constructor uses the given conditions record and table meta data objects and will assign the collection ID
     * from the conditions record.
     *
     * @param tableMetaData The table meta data.
     * @param conditionsRecord The conditions record.
     */
    public BaseConditionsObjectCollection(final ConditionsRecord conditionsRecord, final TableMetaData tableMetaData) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = conditionsRecord.getCollectionId();
    }

    /**
     * This constructor is used to explicitly assign all class variable values.
     *
     * @param conditionsRecord The conditions record.
     * @param tableMetaData The table meta data.
     * @param collectionID The new collection ID.
     */
    public BaseConditionsObjectCollection(final ConditionsRecord conditionsRecord, final TableMetaData tableMetaData,
            final int collectionID) {
        this.conditionsRecord = conditionsRecord;
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionID;
    }

    /**
     * Set the associated table meta data for this collection. Once set it cannot be reassigned, which will cause an
     * exception to be thrown.
     *
     * @param tableMetaData The table meta data for this collection.
     */
    public final void setTableMetaData(final TableMetaData tableMetaData) {
        if (this.tableMetaData != null) {
            throw new RuntimeException("The table meta data cannot be reset once assigned.");
        }
        this.tableMetaData = tableMetaData;
    }

    /**
     * Set the associated conditions record this collection. Once set it cannot be reassigned, which will cause an
     * exception to be thrown.
     *
     * @param conditionsRecord The conditions record for the collection.
     */
    public final void setConditionsRecord(final ConditionsRecord conditionsRecord) {
        if (this.conditionsRecord != null) {
            throw new RuntimeException("The conditions record cannot be reset once assigned.");
        }
        this.conditionsRecord = conditionsRecord;
    }

    /**
     * Add an object to the collection.
     * <p>
     * Implements {@link ConditionsObjectCollection#add(Object)}.
     *
     * @param object The object do add to the collection.
     * @return True if the add operation succeeded.
     */
    public boolean add(final ObjectType object) {
        if (contains(object)) {
            throw new IllegalArgumentException("Cannot add duplicate object " + object + " to collection.");
        }
        return super.add(object);
    }

    /**
     * Get the table meta data.
     * <p>
     * Implements {@link ConditionsObjectCollection#getTableMetaData()}.
     *
     * @return The table meta data for the collection.
     */
    @Override
    public final TableMetaData getTableMetaData() {
        return tableMetaData;
    }

    /**
     * Get the collection ID.
     * <p>
     * Implements {@link ConditionsObjectCollection#getCollectionId()}.
     *
     * @return The collection ID.
     */
    @Override
    public final int getCollectionId() {
        if (conditionsRecord != null) {
            return conditionsRecord.getCollectionId();
        } else {
            return collectionId;
        }
    }

    /**
     * Get the conditions record.
     * <p>
     * Implements {@link ConditionsObjectCollection#getConditionsRecord()}.
     *
     * @return The conditions record for the collection.
     */
    @Override
    public final ConditionsRecord getConditionsRecord() {
        return conditionsRecord;
    }

    /**
     * Set the collection ID. Once set it cannot be assigned again.
     * <p>
     * Implements {@link ConditionsObjectCollection#setCollectionId(int)}.
     *
     * @param collectionId The new collection ID.
     * @throws ConditionsObjectException If the ID was already assigned.
     */
    @Override
    public final void setCollectionId(final int collectionId) throws ConditionsObjectException {
        if (this.collectionId != -1) {
            throw new ConditionsObjectException("The collectionId already has the value " + collectionId
                    + " and cannot be reset.");
        }
        this.collectionId = collectionId;
    }

    /**
     * Insert the collection into the database.
     * <p>
     * Implements {@link ConditionsObjectCollection#insert()}.
     *
     * @throws ConditionsObjectException If there was a problem inserting the object.
     * @throws SQLException If there was a SQL syntax error while executing the operation.
     */
    @Override
    public final void insert() throws ConditionsObjectException, SQLException {
        DatabaseConditionsManager.getInstance().insertCollection(this);
    }

    /**
     * Select objects into this collection from the database.
     * <p>
     * Implements {@link ConditionsObjectCollection#select()}.
     *
     * @return The number of records selected.
     */
    @Override
    public final int select() {
        return 0;
    }

    /**
     * Delete the collection's object's from the database.
     * <p>
     * Implements {@link ConditionsObjectCollection#delete()}.
     *
     * @return The number of objects deleted.
     */
    @Override
    public int delete() {
        return 0;
    }

    /**
     * Update the collection's objects in the database.
     * <p>
     * Implements {@link ConditionsObjectCollection#update()}.
     *
     * @return The number of records updated.
     */
    @Override
    public final int update() {
        return 0;
    }

    /**
     * Convert this object to a string.
     * @return The object converted to a string.
     */
    public String toString() {
        final StringBuffer buffer = new StringBuffer();
        for (ConditionsObject object : this) {
            buffer.append(object.toString());
            buffer.append('\n');
        }
        return buffer.toString();
    }

    /**
     * Get an object by index.
     *
     * @param index The index in the set.
     * @return The object at the index.
     * @throws IndexOutOfBoundsException If the index value is invalid.
     */
    public final ObjectType get(final int index) {
        if (index + 1 > this.size() || index < 0) {
            throw new IndexOutOfBoundsException("The index is out of bounds: " + index);
        }
        int current = 0;
        final Iterator<ObjectType> iterator = this.iterator();
        ObjectType object = iterator.next();
        while (current != index && iterator.hasNext()) {
            object = iterator.next();
            current++;
        }
        return object;
    }

    /**
     * Sort the collection in place.
     *
     * @param comparator The comparator to use for sorting.
     */
    public void sort(final Comparator<ObjectType> comparator) {
        final List<ObjectType> objects = new ArrayList<ObjectType>(this);
        Collections.sort(objects, comparator);
        clear();
        addAll(objects);
    }

    /**
     * Get a sorted list of the objects, leaving original collection in place.
     *
     * @param comparator The comparator to use for the sort.
     * @return A sorted list of the objects.
     */
    @SuppressWarnings("unchecked")
    public BaseConditionsObjectCollection<ObjectType> sorted(final Comparator<ObjectType> comparator) {
        final List<ObjectType> objects = new ArrayList<ObjectType>(this);
        Collections.sort(objects, comparator);
        BaseConditionsObjectCollection<ObjectType> collection = null;
        try {
            collection = (BaseConditionsObjectCollection<ObjectType>) getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        collection.addAll(objects);
        return collection;
    }

    /**
     * Sort this collection in place.
     */
    @Override
    public void sort() {
        final BaseConditionsObjectCollection<ObjectType> sortedCollection = sorted();
        this.clear();
        this.addAll(sortedCollection);
    }

    /**
     * Create and return a sorted collection, leaving the original collection unsorted.
     * @return The sorted collection.
     */
    @SuppressWarnings("unchecked")
    @Override
    public BaseConditionsObjectCollection<ObjectType> sorted() {
        final List<ObjectType> objects = new ArrayList<ObjectType>(this);
        Collections.sort(objects, new DefaultConditionsObjectComparator());
        BaseConditionsObjectCollection<ObjectType> collection = null;
        try {
            collection = ((BaseConditionsObjectCollection<ObjectType>) getClass().newInstance());
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        collection.addAll(objects);
        return collection;
    }
}
