package org.hps.conditions.api;

import java.util.LinkedHashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.hps.conditions.api.ConditionsRecord.ConditionsRecordCollection;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.hps.conditions.svt.SvtAlignmentConstant.SvtAlignmentConstantCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;

/**
 * Get conditions from the database for a series of runs using a tag.
 * Then check that the correct collection IDs were returned.
 * 
 * @author Jeremy McCormick, SLAC
 *
 */
public class ConditionsTagTest extends TestCase {
   
    /**
     * The table names of the conditions to check.
     */
    private static final String[] CONDITIONS = {
        "ecal_calibrations",
        "ecal_channels",
        "ecal_gains",
        "ecal_time_shifts",
        "svt_alignments",
        "svt_calibrations",
        "svt_channels",
        "svt_daq_map",
        "svt_gains",
        "svt_shape_fit_parameters",
        "svt_t0_shifts",
        "svt_timing_constants"
    };
    
    /**
     * The conditions types.
     */
    private static final Class<?> TYPES[] = {
        EcalCalibrationCollection.class,
        EcalChannelCollection.class,
        EcalGainCollection.class,
        EcalTimeShiftCollection.class,
        SvtAlignmentConstantCollection.class, 
        SvtCalibrationCollection.class,
        SvtChannelCollection.class,
        SvtDaqMappingCollection.class,
        SvtGainCollection.class,
        SvtShapeFitParametersCollection.class,
        SvtT0ShiftCollection.class,
        SvtTimingConstantsCollection.class
    };
    
    /**
     * Build an answer key mapping run numbers to the list of expected collection IDs.
     */
    static Map<Integer, int[]> buildCollectionMap() {
        Map<Integer, int[]> collectionMap = new LinkedHashMap<Integer, int[]>();
        collectionMap.put(4823, new int[] {26, 2, 1022, 2, 1014, 3, 2, 2, 2, 1003, 1009, 1023});
        collectionMap.put(5037, new int[] {26, 2, 1022, 2, 1011, 4, 2, 2, 2, 1003, 1009, 1023});
        collectionMap.put(5076, new int[] {1005, 2, 1022, 2, 1013, 4, 2, 2, 2, 1003, 1009, 1023});
        collectionMap.put(5218, new int[] {1008, 2, 1022, 2, 1014, 4, 2, 2, 2, 1003, 1009, 1024});
        collectionMap.put(5400, new int[] {1008, 2, 1022, 2, 1014, 5, 2, 2, 2, 1003, 1009, 1023});
        collectionMap.put(5550, new int[] {1008, 2, 1022, 2, 1010, 5, 2, 2, 2, 1003, 1009, 1024});
        collectionMap.put(5772, new int[] {1008, 2, 1022, 2, 1012, 7, 2, 2, 2, 1003, 1009, 1029});
        collectionMap.put(5797, new int[] {1008, 2, 1022, 2, 1012, 7, 2, 2, 2, 1003, 1009, 1029});
        return collectionMap;
    }
    
    /**
     * Conditions manager instance.
     */
    private static DatabaseConditionsManager MANAGER;      
   
    /**
     * Perform setup.
     */
    @Override
    public void setUp() {
        // Configure the conditions system.
        MANAGER = new DatabaseConditionsManager();
    }
    
    /**
     * Run test using the 'pass1' tag.
     * 
     * @throws Exception if there is any uncaught error when running the method
     */
    public void testPass1Tag() throws Exception {
                
        MANAGER.addTag("pass1");
        
        Map<Integer, int[]> collectionMap = buildCollectionMap();
        
        for (int run : collectionMap.keySet()) {
            
            MANAGER.setDetector("HPS-conditions-test", run);
            
            ConditionsRecordCollection conditionsRecordCollection = MANAGER.getConditionsRecords();
            System.out.println("run " + run + " has " + conditionsRecordCollection.size() + " conditions records");
            System.out.println(conditionsRecordCollection);
            
            int[] expectedCollectionIds = collectionMap.get(run);
            
            System.out.println("run " + run + " conditions");
            for (int i = 0; i < CONDITIONS.length; i++) {
                // Get the conditions from the db.
                BaseConditionsObjectCollection<?> conditionsObjectCollection = BaseConditionsObjectCollection.class.cast(
                                MANAGER.getCachedConditions(TYPES[i], CONDITIONS[i]).getCachedData());
                    
                // Print collection info.
                System.out.println(conditionsObjectCollection.getTableMetaData().getTableName() + ":" 
                        + conditionsObjectCollection.getCollectionId()
                        + " of type " + conditionsObjectCollection.getTableMetaData().getCollectionClass().getName() 
                        + " and size " + conditionsObjectCollection.size());
                
                // Check that the collection ID is correct according to answer key.
                TestCase.assertEquals("Wrong collection ID found for " + TYPES[i].getName() + " condition.", expectedCollectionIds[i], conditionsObjectCollection.getCollectionId());                
            }
        }
    }
}
