package org.hps.conditions;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.hps.conditions.ConditionsObject.ConditionsObjectException;

public class BasicConditionsObjectCollection implements ConditionsObjectCollection {

    List<ConditionsObject> objects = new ArrayList<ConditionsObject>();    
    ConditionsTableMetaData _tableMetaData;
    int _collectionId;
    boolean _isReadOnly;
    boolean _isNew;
    boolean _isDirty;
    
    /**
     * Use for existing collection.
     * @param tableMetaData
     * @param collectionId
     * @param isReadOnly
     */
    public BasicConditionsObjectCollection(ConditionsTableMetaData tableMetaData, int collectionId, boolean isReadOnly) {
        _tableMetaData = tableMetaData;
        _collectionId = collectionId;
        _isReadOnly = isReadOnly;
        if (collectionId == -1) {
            _isNew = true;
        }
    }
    
    /**
     * Use for new collection.
     * @param tableMetaData
     */
    public BasicConditionsObjectCollection(ConditionsTableMetaData tableMetaData) {
        _tableMetaData = tableMetaData;
        _collectionId = -1;
        _isReadOnly = true;
    }
    
    public ConditionsObject get(int index) {
        return objects.get(index);
    }
    
    public List<ConditionsObject> getObjects() {
        return Collections.unmodifiableList(objects);
    }
        
    public void add(ConditionsObject object) {
        if (objects.contains(object)) {
            throw new IllegalArgumentException("Collection already contains this object.");
        }
        object.setCollectionId(getCollectionId());
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
        for (ConditionsObject object : objects) {
            if (object.isNew()) {
                object.insert();
            }
        }
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
