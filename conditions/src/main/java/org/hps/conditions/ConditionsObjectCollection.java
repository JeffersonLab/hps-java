package org.hps.conditions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.conditions.ConditionsObject.ConditionsObjectException;

public class ConditionsObjectCollection<T extends ConditionsObject> {

    List<T> objects = new ArrayList<T>();    
    ConditionsTableMetaData _tableMetaData;
    int _collectionId;
    boolean _isReadOnly;
    boolean _isDirty;
    boolean _isNew;
    
    public ConditionsObjectCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
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
        
    public void add(T object) {
        if (objects.contains(object)) {
            throw new IllegalArgumentException("Collection already contains this object.");
        }
        try {
            object.setCollectionId(getCollectionId());
        } catch (ConditionsObjectException x) {
            throw new IllegalArgumentException("The object has already been assigned to a collection.", x);
        }
        objects.add(object);
        if (!isNew())
            setIsDirty(true);
    }

    public ConditionsTableMetaData getTableMetaData() {
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
}
