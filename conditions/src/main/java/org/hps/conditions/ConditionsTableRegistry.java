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
        
        Set<String> fields;
        ConditionsTableMetaData tableMetaData;
        
        // SVT gains
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("gain");
        fields.add("offset");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_GAINS, fields);
        addTableMetaData(tableMetaData);
        
        // SVT pulse parameters
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("amplitude");
        fields.add("t0");
        fields.add("tp");
        fields.add("chisq");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_PULSE_PARAMETERS, fields);
        addTableMetaData(tableMetaData);

        // SVT gains
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("noise");
        fields.add("pedestal");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_CALIBRATIONS, fields);
        addTableMetaData(tableMetaData);
        
        // SVT channels
        fields = new HashSet<String>();
        fields.add("id"); // TODO: Change to svt_channel_id
        fields.add("fpga");
        fields.add("hybrid");
        fields.add("channel");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_CHANNELS, fields);
        addTableMetaData(tableMetaData);

        // SVT time shift
        fields = new HashSet<String>();
        fields.add("fpga");
        fields.add("hybrid");
        fields.add("time_shift");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_TIME_SHIFTS, fields);
        addTableMetaData(tableMetaData);
        
        // SVT bad channels
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        tableMetaData = new ConditionsTableMetaData(ConditionsConstants.SVT_BAD_CHANNELS, fields);
        addTableMetaData(tableMetaData);
        
    }
}
