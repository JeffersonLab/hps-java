package org.    hps.conditions.svt;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.run.RunRange;
import org.hps.conditions.run.RunSpreadsheet;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;

/**
 * Load SVT timing constant data from the run spreadsheet and insert into the conditions database.
 * <p>
 * Be very careful about running this, because it will create many new conditions records that may 
 * already be present in the database.  In fact, don't run this at all without talking to me first. :-) 
 * 
 * @author Jeremy McCormick
 */
public final class SvtTimingConstantsLoader {
    
    /**
     * Setup conditions.     
     */
    private static final DatabaseConditionsManager MANAGER = DatabaseConditionsManager.getInstance();
    
    /**
     * The fields from the run spreadsheet for SVT timing constants.
     */
    private static final Set<String> FIELDS = new HashSet<String>();
    static {
        FIELDS.add("svt_offset_phase");
        FIELDS.add("svt_offset_time");
    }
    
    /**
     * Load the SVT timing constants for time and phase offsets into the conditions database.
     * 
     * @param args the command line arguments (requires one argument which is CSV file name)
     */
    public static void main(String[] args) {
        
        // Load in CSV records from the exported run spreadsheet.
        String path = args[0];
        RunSpreadsheet runSheet = new RunSpreadsheet(new File(path));
        
        // Find the run ranges that have the same fields values.
        List<RunRange> ranges = RunRange.findRunRanges(runSheet, FIELDS);
        
        // Get the unique field values for inserting new conditions collections.
        List<Collection<String>> uniqueValues = RunRange.getUniqueValues(ranges);
        
        /*
        System.out.println("unique values ...");
        for (Collection<String> collection : uniqueValues) {
            for (String value : collection) {
                System.out.print(value + " ");                
            }
            System.out.println();
        } 
        */      
               
        // Create a new collection for each unique combination set of timing constants.
        List<SvtTimingConstantsCollection> collections = createCollections(uniqueValues); 
        
        // Create a new collection for each of the unique combinations of values.
        for (SvtTimingConstantsCollection collection : collections) {
            int collectionId = 0;
            try {
                collectionId = MANAGER.addCollection(
                        "svt_timing_constants",
                        "SVT timing constants added by " + System.getProperty("user.name"),
                        "timing constants from run spreadsheet");
                collection.setCollectionId(collectionId);
                collection.insert();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }            
        } 
                
        // Create the conditions records for the run ranges.
        for (RunRange range : ranges) {
            
            System.out.println(range);
            
            // Get the data values from the run range.
            int offsetPhase = Integer.parseInt(range.getValue("svt_offset_phase"));
            int offsetTime = Integer.parseInt(range.getValue("svt_offset_time"));

            // Find the matching timing constants collection to use.
            SvtTimingConstantsCollection collection = findCollection(collections, offsetPhase, offsetTime);
            if (collection != null) {
                System.out.println("offset_phase : " + collection.get(0).getOffsetPhase() + ", offset_time: " + collection.get(0).getOffsetTime());
            }
                        
            // Create a new conditions record with the run range.
            ConditionsRecord condi = new ConditionsRecord();
            condi.setFieldValue("run_start", range.getRunStart());
            condi.setFieldValue("run_end", range.getRunEnd());
            condi.setFieldValue("name", "svt_timing_constants");
            condi.setFieldValue("table_name", "svt_timing_constants");
            condi.setFieldValue("notes", "timing constants from run spreadsheet");
            condi.setFieldValue("created", new Date());
            condi.setFieldValue("created_by", System.getProperty("user.name"));
            
            condi.setFieldValue("collection_id", collection.getCollectionId());

            try {
                condi.insert();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }            
            
            System.out.print(condi);            
            System.out.println();
        }
    }
    
    /**
     * Create the conditions collections from the unique values found in the spreadsheet.
     * 
     * @param uniqueValues the list of unique raw values
     * @return the new conditions collections
     */
    private static List<SvtTimingConstantsCollection> createCollections(List<Collection<String>> uniqueValues) {
        List<SvtTimingConstantsCollection> collections = new ArrayList<SvtTimingConstantsCollection>();        
        for (Collection<String> values : uniqueValues) {
            
            SvtTimingConstants timing = new SvtTimingConstants();
            Iterator<String> it = values.iterator();            
            Integer offsetPhase = Integer.parseInt(it.next());
            Integer offsetTime = Integer.parseInt(it.next());
            
            timing.setFieldValue("offset_phase", offsetPhase);
            timing.setFieldValue("offset_time", offsetTime);
            
            SvtTimingConstantsCollection collection = new SvtTimingConstantsCollection();
            collection.add(timing);
            collections.add(collection);
        }
        return collections;
    }
     
    /**
     * Find a timing constants collection from offset phase and time. 
     * <p>
     * Each collection has a single object in it.
     * 
     * @param timingConstantsList the list of collections
     * @param offsetPhase the offset phase
     * @param offsetTime the offset time
     * @return the matching collection or <code>null</code> if not found
     */
    private static SvtTimingConstantsCollection findCollection(List<SvtTimingConstantsCollection> timingConstantsList, int offsetPhase, int offsetTime) {
        for (SvtTimingConstantsCollection collection : timingConstantsList) {
            if (collection.find(offsetPhase, offsetTime) != null) {
                return collection;
            }
        }
        return null;
    }
}
