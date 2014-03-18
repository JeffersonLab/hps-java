package org.hps.conditions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A central registry of {@link ConditionsTableMetaData} objects for use by converters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
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
        
        // SVT gains
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("gain");
        fields.add("offset");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_GAINS, fields));
        
        // SVT pulse parameters
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("amplitude");
        fields.add("t0");
        fields.add("tp");
        fields.add("chisq");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_PULSE_PARAMETERS, fields));

        // SVT gains
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        fields.add("noise");
        fields.add("pedestal");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_CALIBRATIONS, fields));
        
        // SVT channels
        fields = new HashSet<String>();
        fields.add("id"); // TODO: Change to svt_channel_id
        fields.add("fpga");
        fields.add("hybrid");
        fields.add("channel");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_CHANNELS, fields));

        // SVT time shift
        fields = new HashSet<String>();
        fields.add("fpga");
        fields.add("hybrid");
        fields.add("time_shift");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_TIME_SHIFTS, fields));
        
        // SVT bad channels
        fields = new HashSet<String>();
        fields.add("svt_channel_id");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.SVT_BAD_CHANNELS, fields));
        
        // ECal bad channels
        fields = new HashSet<String>();
        fields.add("ecal_channel_id");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.ECAL_BAD_CHANNELS, fields));
        
        // ECal gains
        fields = new HashSet<String>();
        fields.add("ecal_channel_id");
        fields.add("gain");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.ECAL_GAINS, fields));
        
        // Ecal calibrations
        fields = new HashSet<String>();
        fields.add("ecal_channel_id");
        fields.add("noise");
        fields.add("pedestal");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.ECAL_CALIBRATIONS, fields));
        
        // Beam current
        fields = new HashSet<String>();
        fields.add("beam_current");
        addTableMetaData(new ConditionsTableMetaData(ConditionsTableConstants.BEAM_CURRENT, fields));
    }
}
