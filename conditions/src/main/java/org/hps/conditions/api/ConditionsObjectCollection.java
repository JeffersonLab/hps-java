package org.hps.conditions.api;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableMetaData;

public class ConditionsObjectCollection<ObjectType extends ConditionsObject> implements Iterable<ObjectType> {

    protected Set<ObjectType> objects = new LinkedHashSet<ObjectType>();
    protected TableMetaData tableMetaData;
    protected int collectionId = -1;
    protected ConditionsRecord conditionsRecord;

    protected ConditionsObjectCollection() {
    }

    public ConditionsObjectCollection(TableMetaData tableMetaData, int collectionId) {
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionId;
    }

    public ObjectType get(int index) {
        Iterator<ObjectType> iterator = objects.iterator();
        ObjectType current = iterator.next();
        for (int i = 0; i < index; i++) {
            current = iterator.next();
        }
        return current;
    }

    public Set<ObjectType> getObjects() {
        return Collections.unmodifiableSet(objects);
    }

    public boolean contains(Object object) {
        return getObjects().contains(object);
    }

    // TODO: Should check here if object has an existing collection ID that is
    // different from this collection's, in which case this collection becomes "mixed" and
    // it should be flagged as read only.
    public void add(ObjectType object) throws ConditionsObjectException {
        if (objects.contains(object)) {
            throw new IllegalArgumentException("Collection already contains this object.");
        }
        try {
            // Only assign a collection ID to the object if this collection has a valid ID
            // and the object does not have one already.
            if (getCollectionId() != -1)
                object.setCollectionId(getCollectionId());
        } catch (ConditionsObjectException x) {
            throw new IllegalArgumentException("Error assigning collection ID to object.", x);
        }
        // Set the table meta data on the object if it does not have any.
        if (object.getTableMetaData() == null && tableMetaData != null)
            object.setTableMetaData(tableMetaData);
        objects.add(object);
    }

    public TableMetaData getTableMetaData() {
        return tableMetaData;
    }

    public int getCollectionId() {
        return collectionId;
    }

    // TODO: Should this also insert new records that do not exist?
    // TODO: This operation should lock the table.
    public void update() throws ConditionsObjectException {
        for (ConditionsObject object : objects) {
            object.update();
        }
    }

    // TODO: This does not need to loop. It should just call delete on the collection ID value.
    public void delete() throws ConditionsObjectException {
        // TODO: Replace with call to a deleteCollection DatabaseConditionsManager method.
        for (ConditionsObject object : objects) {
            object.delete();
        }
    }

    // TODO: This should not loop. It should select all the objects with a matching collection ID 
    //       from the database replacing the current contents of the collection (if any).
    public void select() throws ConditionsObjectException, SQLException {
        // TODO: Replace with call to a selectCollection DatabaseConditionsManager method.
        for (ConditionsObject object : objects) {
            object.select();
        }
    }

    public void insert() throws ConditionsObjectException, SQLException {
        DatabaseConditionsManager.getInstance().insertCollection(this);
    }

    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (this.collectionId != -1)
            throw new ConditionsObjectException("The collection ID is already set.");
        this.collectionId = collectionId;
        for (ConditionsObject object : this.objects) {
            object.setCollectionId(this.collectionId);
        }
    }

    public void setTableMetaData(TableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    @Override
    public Iterator<ObjectType> iterator() {
        return objects.iterator();
    }       
    
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        for (ConditionsObject object : this.getObjects()) {
            buffer.append(object.toString());
            buffer.append('\n');
        }
        return buffer.toString();
    }
}