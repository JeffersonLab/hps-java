package org.hps.conditions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ConditionsObjectCollection<T extends ConditionsObject> {

    List<T> objects = new ArrayList<T>();    
    TableMetaData _tableMetaData;
    int _collectionId = -1;
    boolean _isReadOnly;
    boolean _isDirty;
    boolean _isNew;
    
    protected ConditionsObjectCollection() {
    }
            
    public ConditionsObjectCollection(TableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        _tableMetaData = tableMetaData;
        _collectionId = collectionId;
        _isReadOnly = isReadOnly;
        if (collectionId == -1) {
            _isNew = true;
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
        
    public void add(T object) throws ConditionsObjectException {
        if (objects.contains(object)) {
            throw new IllegalArgumentException("Collection already contains this object.");
        }
        try {
            // Only assign a collection ID to the object if this collection has a valid ID.
            if (getCollectionId() != -1)
                object.setCollectionId(getCollectionId());
        } catch (ConditionsObjectException x) {
            throw new IllegalArgumentException("Error assigning collection ID to object.", x);
        }
        // Set the table meta data if the object does not have any.
        if (object.getTableMetaData() == null && _tableMetaData != null)
            object.setTableMetaData(_tableMetaData);
        objects.add(object);
        if (!isNew())
            setIsDirty(true);
    }

    public TableMetaData getTableMetaData() {
        return _tableMetaData;        
    }

    public int getCollectionId() {
        return _collectionId;
    }

    public void updateAll() throws ConditionsObjectException {
        for (ConditionsObject object : objects) {
            object.update();
        }        
        setIsDirty(false);
    }

    public void deleteAll() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("Collection is read only.");
        }
        for (ConditionsObject object : objects) {            
            object.delete();
        }                
    }

    public void selectAll() throws ConditionsObjectException, SQLException {
        for (ConditionsObject object : objects) {
            object.select();
        }
    }

    // TODO: This method needs to get the next collection ID if it doesn't have one already.
    public void insertAll() throws ConditionsObjectException, SQLException {
        if (!isNew()) {
            throw new ConditionsObjectException("Collection already exists in the database.");
        }
        // FIXME: Should get the next global collection ID from the database here,
        //        if collection ID is -1 (for new collection being added).
        for (ConditionsObject object : objects) {
            if (object.isNew()) {
                object.insert();
            }
        }
        _isNew = false;
    }

    public boolean isDirty() {
        return _isDirty;
    }

    public boolean isReadOnly() {
        return _isReadOnly;
    }
    
    void setIsDirty(boolean isDirty) {
        _isDirty = isDirty;
    }
    
    public boolean isNew() {
        return _isNew;
    }
    
    public void setCollectionId(int collectionId) throws ConditionsObjectException {
        if (_collectionId != -1)
            throw new ConditionsObjectException("The collection ID is already set.");
        _collectionId = collectionId;
    }
    
    public void setTableMetaData(TableMetaData tableMetaData) {
        _tableMetaData = tableMetaData;
    }
    
    public void setIsReadOnly(boolean isReadOnly) {
        _isReadOnly = isReadOnly;
    }    
}
