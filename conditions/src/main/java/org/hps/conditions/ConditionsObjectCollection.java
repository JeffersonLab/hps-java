package org.hps.conditions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO: This class should have a reference to its ConditionsRecord.
// TODO: Collections with a mix of different collection IDs on their objects should always be read only.
public class ConditionsObjectCollection<T extends ConditionsObject> {

    protected List<T> _objects = new ArrayList<T>();    
    protected TableMetaData _tableMetaData;
    protected int _collectionId = -1;
    protected boolean _isReadOnly;
    protected boolean _isDirty;
    protected boolean _isNew;
    protected ConditionsRecord _conditionsRecord;
    
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
        return _objects.get(index);
    }
    
    public List<T> getObjects() {
        return Collections.unmodifiableList(_objects);
    }
    
    public boolean contains(Object object) {
        return getObjects().contains(object);
    }
        
    // TODO: Should check here if object has an existing collection ID that is different 
    // from this collection's, in which case this collection becomes "mixed" and it should be
    // flagged as read only.
    public void add(T object) throws ConditionsObjectException {
        if (_objects.contains(object)) {
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
        if (object.getTableMetaData() == null && _tableMetaData != null)
            object.setTableMetaData(_tableMetaData);
        _objects.add(object);
        if (!isNew())
            setIsDirty(true);
    }

    public TableMetaData getTableMetaData() {
        return _tableMetaData;        
    }

    public int getCollectionId() {
        return _collectionId;
    }
    
    // TODO: Should this also insert new records that do not exist?
    // TODO: This operation should lock the table.
    public void update() throws ConditionsObjectException {
        for (ConditionsObject object : _objects) {            
            object.update();
        }        
        setIsDirty(false);
    }

    // TODO: This does not need to loop.  It should just call delete on the collection ID value. 
    public void delete() throws ConditionsObjectException {
        if (isReadOnly()) {
            throw new ConditionsObjectException("Collection is read only.");
        }
        for (ConditionsObject object : _objects) {            
            object.delete();
        }                
    }

    // TODO: This should not loop.  It should select all the objects with a matching collection ID
    // from the database.
    public void select() throws ConditionsObjectException, SQLException {
        for (ConditionsObject object : _objects) {
            object.select();
        }
    }

    // TODO: This method needs to get the next collection ID from the conditions manager.
    // TODO: This operation should lock the table.
    public void insert() throws ConditionsObjectException, SQLException {
        if (!isNew()) {
            throw new ConditionsObjectException("Collection already exists in the database.");
        }
        for (ConditionsObject object : _objects) {
            //if (object.isNew()) {
            object.insert();
            //}
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
    
    // TODO: This can probably just check if collection ID is not valid e.g. equals -1.    
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
    
    public void setConditionsRecord(ConditionsRecord conditionsRecord) throws ConditionsObjectException {
        if (_conditionsRecord != null)
            throw new ConditionsObjectException("The ConditionsRecord is already set on this collection.");
        _conditionsRecord = conditionsRecord;
    }
}
