package org.hps.conditions;

import java.util.Set;

public class ConditionsTableMetaData {
    
    String _tableName;
    Set<String> _fieldNames = null;
    
    ConditionsTableMetaData(String tableName, Set<String> fieldNames) {
        _tableName = tableName;
        _fieldNames = fieldNames;
    }
    
    Set<String> getFieldNames() {
        return _fieldNames;
    }
    
    String getTableName() {
        return _tableName;
    }    
    
    // TODO: Add method for getting next set ID.
}
