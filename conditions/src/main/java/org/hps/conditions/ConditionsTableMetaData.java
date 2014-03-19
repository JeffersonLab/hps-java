package org.hps.conditions;

import java.util.Set;

/**
 * This class contains basic meta data information about tables in the conditions
 * database, including their name and list of fields.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsTableMetaData {
    
    String _tableName;
    Set<String> _fieldNames = null;
    
    ConditionsTableMetaData(String tableName, Set<String> fieldNames) {
        _tableName = tableName;
        _fieldNames = fieldNames;
    }
        
    /*
    ConditionsTableMetaData(String tableName, String[] fields) {
        _tableName = tableName;
        _fieldNames = new HashSet<String>();
        for (String field : fields) {
            _fieldNames.add(field);
        }
    }
    */
    
    Set<String> getFieldNames() {
        return _fieldNames;
    }
    
    String getTableName() {
        return _tableName;
    }       
}
