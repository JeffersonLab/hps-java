package org.hps.conditions;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This class provides meta data about a conditions table, including a
 * list of conditions data fields (not including collection ID or row ID
 * which are assumed).  It also has references to the classes which are
 * used to map the data onto Java classes via the {@link ConditionsObject}
 * and {@link ConditionsObjectCollection} APIs.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 *
 */
public class TableMetaData {
    
    String _tableName;
    Class<? extends ConditionsObject> _objectClass;
    Class<? extends ConditionsObjectCollection<?>> _collectionClass;
    Set<String> _fieldNames = new LinkedHashSet<String>();
        
    TableMetaData(String tableName, 
            Class<? extends ConditionsObject> objectClass, 
            Class<? extends ConditionsObjectCollection<?>> collectionClass) {
        _tableName = tableName;
        _objectClass = objectClass;
        _collectionClass = collectionClass;
    }
    
    Class<? extends ConditionsObject> getObjectClass() {
        return _objectClass;
    }
    
    Class<? extends ConditionsObjectCollection<?>> getCollectionClass() {
        return _collectionClass;
    }
    
    String[] getFieldNames() {
        return _fieldNames.toArray(new String[]{});
    }
       
    void addField(String name) {
        if (_fieldNames.contains(name)) {
            throw new RuntimeException("The table meta data already has a field called " + name);
        }
        _fieldNames.add(name);
    }
    
    public String getTableName() {
        return _tableName;
    }
}