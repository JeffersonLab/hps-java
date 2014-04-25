package org.hps.conditions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: This class should have a reference to its ConditionsRecord.
// TODO: Collections with a mix of different collection IDs on their objects should always be read only.
public class ConditionsObjectCollection<T extends ConditionsObject> {

    protected List<T> objects = new ArrayList<T>();
    protected TableMetaData tableMetaData;
    protected int collectionId = -1;
    protected boolean isReadOnly;
    protected boolean isDirty;
    protected boolean isNew;
    protected ConditionsRecord conditionsRecord;

    protected ConditionsObjectCollection() {
    }

    public ConditionsObjectCollection(TableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        this.tableMetaData = tableMetaData;
        this.collectionId = collectionId;
        this.isReadOnly = isReadOnly;
        if (collectionId == -1) {
            this.isNew = true;
        }
    }

    public T get(int index) {
        return objects.get(index);
    }

    public List<T> getObjects() {
        return Collections.unmodifiableList(objects);
    }

    public boolean contains(Object object) {
        return getObjects().contains(object);
    }

    // TODO: Should check here if object has an existing collection ID that is different
    // from this collection's, in which case this collection becomes "mixed" and it should
    // be
    // flagged as read only.
    public void add(T object) throws ConditionsObjectException {
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
        if (!isNew())
            setIsDirty(true);
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
        setIsDirty(false);
    }

    // TODO: This does not need to loop. It should just call delete on the collection ID
    // value.
    public void delete() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("Collection is read only.");
        }
        for (ConditionsObject object : objects) {
            object.delete();
        }
    }

    // TODO: This should not loop. It should select all the objects with a matching
    // collection ID
    // from the database.
    public void select() throws ConditionsObjectException, SQLException {
        for (ConditionsObject object : objects) {
            object.select();
        }
    }

    // TODO: This method needs to get the next collection ID from the conditions manager.
    // TODO: This operation should lock the table.
    public void insert() throws ConditionsObjectException, SQLException {
        if (!isNew()) {
            throw new ConditionsObjectException("Collection already exists in the database.");
        }
        for (ConditionsObject object : objects) {
            // if (object.isNew()) {
            object.insert();
            // }
        }
        isNew = false;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public boolean isReadOnly() {
        return isReadOnly;
    }

    void setIsDirty(boolean isDirty) {
        this.isDirty = isDirty;
    }

    // TODO: This can probably just check if collection ID is not valid e.g. equals -1.
    public boolean isNew() {
        return isNew;
    }

    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (this.collectionId != -1)
            throw new ConditionsObjectException("The collection ID is already set.");
        this.collectionId = collectionId;
    }

    public void setTableMetaData(TableMetaData tableMetaData) {
        this.tableMetaData = tableMetaData;
    }

    public void setIsReadOnly(boolean isReadOnly) {
        this.isReadOnly = isReadOnly;
    }

    public void setConditionsRecord(ConditionsRecord conditionsRecord) throws ConditionsObjectException {
        if (this.conditionsRecord != null)
            throw new ConditionsObjectException("The ConditionsRecord is already set on this collection.");
        this.conditionsRecord = conditionsRecord;
    }
}
