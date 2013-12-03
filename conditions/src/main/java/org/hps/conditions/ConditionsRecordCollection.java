package org.hps.conditions;

import java.util.ArrayList;

/**
 * This is a simple container class for objects with the type <code>ConditionsRecord</code>.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class ConditionsRecordCollection extends ArrayList<ConditionsRecord> {
            
    /**
     * Find a ConditionsRecord by its name or key in this collection.  
     * This is the 'name' field from the conditions database table. 
     * @param name The name of the conditions set, e.g. 'svt_calibrations' etc.
     * @return The collection of ConditionsRecords, which can be empty if none were found.
     */
    public ConditionsRecordCollection find(String name) {
        ConditionsRecordCollection records = new ConditionsRecordCollection();
        for (ConditionsRecord rec : this) {
            if (rec.getName().equals(name)) {
                records.add(rec);
            }
        }
        return records;
    }        
}