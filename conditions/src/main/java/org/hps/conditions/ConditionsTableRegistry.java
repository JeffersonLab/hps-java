package org.hps.conditions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class ConditionsTableRegistry {
    
    Map<String, ConditionsTableMetaData> _tableMetaDataMap = new HashMap<String, ConditionsTableMetaData>();
    
    public ConditionsTableMetaData getTableMetaData(String name) {
        return _tableMetaDataMap.get(name);
    }
    
    void addTableMetaData(ConditionsTableMetaData tableMetaData) {
        if (_tableMetaDataMap.get(tableMetaData.getTableName()) != null) {
            throw new IllegalArgumentException("Table data already exists for " + tableMetaData.getTableName());
        }
        _tableMetaDataMap.put(tableMetaData.getTableName(), tableMetaData);        
    }
    
    void registerDefaultTableMetaData() {
        
        // SVT Gains (as test!!!)
        Set<String> svtGainFields = new HashSet<String>();
        svtGainFields.add("gain");
        svtGainFields.add("offset");
        ConditionsTableMetaData svtGainTable = 
                new ConditionsTableMetaData(ConditionsConstants.SVT_GAINS, svtGainFields);
        addTableMetaData(svtGainTable);
    }
}
