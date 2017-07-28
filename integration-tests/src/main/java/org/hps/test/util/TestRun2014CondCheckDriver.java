package org.hps.test.util;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalPulseWidth.EcalPulseWidthCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.TestRunSvtChannel.TestRunSvtChannelCollection;
import org.hps.conditions.svt.TestRunSvtConditions;
import org.hps.conditions.svt.TestRunSvtDaqMapping.TestRunSvtDaqMappingCollection;
import org.hps.conditions.svt.TestRunSvtT0Shift.TestRunSvtT0ShiftCollection;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Checks that the correct conditions are loaded for Test Run 2015 reconstruction
 * by verifying the collection IDs.
 */
public class TestRun2014CondCheckDriver extends Driver {
    
    private static Logger LOG = Logger.getLogger(TestRun2014CondCheckDriver.class.getName());
    
    private boolean executed = false;
    
    /**
     * Checks that expected conditions sets are loaded for engineering run 2015 reconstruction
     * by looking at their collection IDs.
     */
    public void detectorChanged(Detector detector) {
                       
        LOG.info("Checking collection IDs of TestRun2014 conditions...");
        
        TestCase.assertFalse("The detectorChanged method was executed more than once!", executed);
        
        ConditionsManager mgr = DatabaseConditionsManager.getInstance();

        TestRunSvtChannelCollection svtChannels = mgr.getCachedConditions(TestRunSvtChannelCollection.class, "test_run_svt_channels").getCachedData();
        TestCase.assertEquals("SvtChannelCollection has wrong collection ID.", 1, svtChannels.getCollectionId());

        TestRunSvtDaqMappingCollection svtDaqMap = mgr.getCachedConditions(TestRunSvtDaqMappingCollection.class, "test_run_svt_daq_map").getCachedData();
        TestCase.assertEquals("SvtDaqMappingCollection has wrong collection ID.", 1, svtDaqMap.getCollectionId());
        
        TestRunSvtT0ShiftCollection svtT0Shifts = mgr.getCachedConditions(TestRunSvtT0ShiftCollection.class, "test_run_svt_t0_shifts").getCachedData();
        TestCase.assertEquals("SvtT0ShiftCollection has wrong collection ID.", 1, svtT0Shifts.getCollectionId());
        
        SvtCalibrationCollection svtCalib = mgr.getCachedConditions(SvtCalibrationCollection.class, "test_run_svt_calibrations").getCachedData();
        TestCase.assertEquals("SvtCalibrationCollection has wrong collection ID.", 1, svtCalib.getCollectionId());
        
        SvtShapeFitParametersCollection svtShapeFit = mgr.getCachedConditions(SvtShapeFitParametersCollection.class, "test_run_svt_shape_fit_parameters").getCachedData();
        TestCase.assertEquals("SvtShapeFitParametersCollection has wrong collection ID.", 1, svtShapeFit.getCollectionId());
        
        SvtGainCollection svtGains = mgr.getCachedConditions(SvtGainCollection.class, "test_run_svt_gains").getCachedData();
        TestCase.assertEquals("SvtGainCollection has wrong collection ID.", 1, svtGains.getCollectionId());

        EcalChannelCollection ecalChannels = mgr.getCachedConditions(EcalChannelCollection.class, "test_run_ecal_channels").getCachedData();
        TestCase.assertEquals("EcalChannelCollection has wrong collection ID.", 1, ecalChannels.getCollectionId());
                
        EcalGainCollection ecalGains = mgr.getCachedConditions(EcalGainCollection.class, "test_run_ecal_gains").getCachedData();
        TestCase.assertEquals("EcalGainCollection has wrong collection ID.", 1, ecalGains.getCollectionId());
        
        EcalCalibrationCollection ecalCalib = mgr.getCachedConditions(EcalCalibrationCollection.class, "test_run_ecal_calibrations").getCachedData();
        TestCase.assertEquals("EcalCalibrationCollection has wrong collection ID.", 1, ecalCalib.getCollectionId());
                
        EcalPulseWidthCollection ecalPulseWidths = mgr.getCachedConditions(EcalPulseWidthCollection.class, "ecal_pulse_widths").getCachedData();
        TestCase.assertEquals("EcalPulseWidthCollection has wrong collection ID.", 1034, ecalPulseWidths.getCollectionId());
                        
        TestRunSvtConditions svtConditions = mgr.getCachedConditions(TestRunSvtConditions.class, "svt_conditions").getCachedData();
        TestCase.assertNotNull("SvtConditions is null!", svtConditions);
        System.out.println(svtConditions.toString());
        
        EcalConditions ecalConditions = mgr.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        TestCase.assertNotNull("EcalConditions is null!", ecalConditions);
        System.out.println(ecalConditions.toString());
        
        executed = true;
        
        LOG.info("Done checking collection IDs for TestRun2014 conditions!");
    }

}
