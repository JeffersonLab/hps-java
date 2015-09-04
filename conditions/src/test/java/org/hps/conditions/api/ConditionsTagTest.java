package org.hps.conditions.api;

import java.util.HashSet;
import java.util.Set;

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

public class ConditionsTagTest extends TestCase {

    private static final int[] RUNS = { 5037, 5038, 5066, 5076, 5139, 5149, 5174, 5181, 5200, 5218, 5236, 5251, 5253,
        5263, 5299, 5310, 5375, 5388, 5389, 5400, 5404, 5533, 5538, 5558, 5575, 5596, 5601, 5603, 5610, 5623, 5640,
        5641, 5642, 5673, 5686, 5711, 5712, 5713, 5714, 5722, 5747, 5748, 5779
    };
    
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
    
    private static DatabaseConditionsManager MANAGER;      
    
    @Override
    public void setUp() {
        // Configure the conditions system.
        MANAGER = DatabaseConditionsManager.getInstance();
        MANAGER.setConnectionResource("/org/hps/conditions/config/jeremym_dev_connection.prop");
        MANAGER.setXmlConfig("/org/hps/conditions/config/conditions_database_no_svt.xml");
        //MANAGER.setLogLevel(Level.WARNING);
    }
    
    public void testConditionsTag() throws Exception {
        MANAGER.addTag("pass1");
        for (int run : RUNS) {
            MANAGER.setDetector("HPS-conditions-test", run);
            ConditionsRecordCollection conditionsRecordCollection = MANAGER.getConditionsRecords();
            System.out.println("run " + run + " has " + conditionsRecordCollection.size() + " conditions records");
            System.out.println(conditionsRecordCollection);
            for (int i = 0; i < CONDITIONS.length; i++) {
                try {
                    BaseConditionsObjectCollection<?> conditionsObjectCollection = 
                            BaseConditionsObjectCollection.class.cast(
                                    MANAGER.getCachedConditions(TYPES[i], CONDITIONS[i]).getCachedData());
                System.out.println("got collection " + conditionsObjectCollection.getTableMetaData().getTableName() + ":" 
                        + conditionsObjectCollection.getCollectionId()
                        + " with type " + conditionsObjectCollection.getTableMetaData().getCollectionClass().getName() 
                        + " and " + conditionsObjectCollection.size() + " objects");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void testMultipleConditionsTags() throws Exception {
        Set<String> tags = new HashSet<String>();
        tags.add("pass1");
        tags.add("dev");
        tags.add("eng_run");
        tags.add("derp");
        
        for (int run : RUNS) {
            MANAGER.setDetector("HPS-conditions-test", run);
            ConditionsRecordCollection conditionsRecordCollection = MANAGER.getConditionsRecords();
            System.out.println("run " + run + " has " + conditionsRecordCollection.size());
            System.out.println(conditionsRecordCollection);
            for (int i = 0; i < CONDITIONS.length; i++) {
                try {
                    BaseConditionsObjectCollection<?> conditionsObjectCollection = 
                            BaseConditionsObjectCollection.class.cast(
                                    MANAGER.getCachedConditions(TYPES[i], CONDITIONS[i]).getCachedData());
                System.out.println("got collection " + conditionsObjectCollection.getTableMetaData().getTableName() 
                        + " with type " + conditionsObjectCollection.getTableMetaData().getCollectionClass().getName() 
                        + " and " + conditionsObjectCollection.size() + " objects");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
