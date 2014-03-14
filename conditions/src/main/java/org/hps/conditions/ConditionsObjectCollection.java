package org.hps.conditions;

import java.sql.SQLException;
import java.util.List;

import org.hps.conditions.ConditionsObject.ConditionsObjectException;

// TODO: This should be a generic with <T extends ConditionsDatabaseObject> 
//       but of course this is a pain in the ass!!!
public interface ConditionsObjectCollection {

    ConditionsObject get(int index);
            
    void add(ConditionsObject object);
    
    List<ConditionsObject> getObjects();
    
    ConditionsTableMetaData getTableMetaData();
    
    int getCollectionId();    
    
    void updateAll() throws ConditionsObjectException;
    
    void deleteAll() throws ConditionsObjectException;
    
    void selectAll() throws ConditionsObjectException, SQLException;
    
    void insertAll() throws ConditionsObjectException, SQLException;
    
    boolean isDirty();
    
    boolean isReadOnly();
    
    boolean isNew();
}
