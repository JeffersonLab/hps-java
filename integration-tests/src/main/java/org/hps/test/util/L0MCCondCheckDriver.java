package org.hps.test.util;

import java.util.logging.Logger;

import junit.framework.TestCase;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.conditions.ecal.EcalGain.EcalGainCollection;
import org.hps.conditions.ecal.EcalPulseWidth.EcalPulseWidthCollection;
import org.hps.conditions.ecal.EcalTimeShift.EcalTimeShiftCollection;
import org.hps.conditions.svt.SvtCalibration.SvtCalibrationCollection;
import org.hps.conditions.svt.SvtChannel.SvtChannelCollection;
import org.hps.conditions.svt.SvtConditions;
import org.hps.conditions.svt.SvtDaqMapping.SvtDaqMappingCollection;
import org.hps.conditions.svt.SvtGain.SvtGainCollection;
import org.hps.conditions.svt.SvtShapeFitParameters.SvtShapeFitParametersCollection;
import org.hps.conditions.svt.SvtT0Shift.SvtT0ShiftCollection;
import org.hps.conditions.svt.SvtTimingConstants.SvtTimingConstantsCollection;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Checks that the correct conditions are loaded for physics run 2016 reconstruction
 * by verifying the collection IDs.
 */
public class L0MCCondCheckDriver extends Driver {
    
    private static Logger LOG = Logger.getLogger(L0MCCondCheckDriver.class.getName());
    
    private boolean executed = false;
    
    /**
     * Checks that expected conditions sets are loaded for engineering run 2015 reconstruction
     * by looking at their collection IDs.
     */
    public void detectorChanged(Detector detector) {
                       
        LOG.info("Checking collection IDs of Layer0 MC conditions...");
        
        TestCase.assertFalse("The detectorChanged method was executed more than once!", executed);
        
        ConditionsManager mgr = DatabaseConditionsManager.getInstance();

        SvtChannelCollection svtChannels = mgr.getCachedConditions(SvtChannelCollection.class, "svt_channels").getCachedData();
        TestCase.assertEquals("SvtChannelCollection has wrong collection ID.", 3, svtChannels.getCollectionId());
       
        SvtDaqMappingCollection svtDaqMap = mgr.getCachedConditions(SvtDaqMappingCollection.class, "svt_daq_map").getCachedData();
        TestCase.assertEquals("SvtDaqMappingCollection has wrong collection ID.", 3, svtDaqMap.getCollectionId());

        SvtT0ShiftCollection svtT0Shifts = mgr.getCachedConditions(SvtT0ShiftCollection.class, "svt_t0_shifts").getCachedData();
        TestCase.assertEquals("SvtT0ShiftCollection has wrong collection ID.", 3, svtT0Shifts.getCollectionId());

        SvtCalibrationCollection svtCalib = mgr.getCachedConditions(SvtCalibrationCollection.class, "svt_calibrations").getCachedData();
        TestCase.assertEquals("SvtCalibrationCollection has wrong collection ID.", 9, svtCalib.getCollectionId());
        
        SvtShapeFitParametersCollection svtShapeFit = mgr.getCachedConditions(SvtShapeFitParametersCollection.class, "svt_shape_fit_parameters").getCachedData();
        TestCase.assertEquals("SvtShapeFitParametersCollection has wrong collection ID.", 2, svtShapeFit.getCollectionId());

        SvtGainCollection svtGains = mgr.getCachedConditions(SvtGainCollection.class, "svt_gains").getCachedData();
        TestCase.assertEquals("SvtGainCollection has wrong collection ID.", 3, svtGains.getCollectionId());

        EcalChannelCollection ecalChannels = mgr.getCachedConditions(EcalChannelCollection.class, "ecal_channels").getCachedData();
        TestCase.assertEquals("EcalChannelCollection has wrong collection ID.", 2, ecalChannels.getCollectionId());

        EcalGainCollection ecalGains = mgr.getCachedConditions(EcalGainCollection.class, "ecal_gains").getCachedData();
        TestCase.assertEquals("EcalGainCollection has wrong collection ID.", 1898, ecalGains.getCollectionId());

        EcalCalibrationCollection ecalCalib = mgr.getCachedConditions(EcalCalibrationCollection.class, "ecal_calibrations").getCachedData();
        TestCase.assertEquals("EcalCalibrationCollection has wrong collection ID.", 1008, ecalCalib.getCollectionId());

        EcalTimeShiftCollection ecalTimeShifts = mgr.getCachedConditions(EcalTimeShiftCollection.class, "ecal_time_shifts").getCachedData();
        TestCase.assertEquals("EcalTimeShiftCollection has wrong collection ID.", 1899, ecalTimeShifts.getCollectionId());
        
        EcalPulseWidthCollection ecalPulseWidths = mgr.getCachedConditions(EcalPulseWidthCollection.class, "ecal_pulse_widths").getCachedData();
        TestCase.assertEquals("EcalPulseWidthCollection has wrong collection ID.", 1034, ecalPulseWidths.getCollectionId());

        SvtTimingConstantsCollection svtTimingConstants = mgr.getCachedConditions(SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData();
        TestCase.assertEquals("SvtTimingConstantsCollection has wrong collection ID.", 1030, svtTimingConstants.getCollectionId());
                
        SvtConditions svtConditions = mgr.getCachedConditions(SvtConditions.class, "svt_conditions").getCachedData();
        TestCase.assertNotNull("SvtConditions is null!", svtConditions);
        System.out.println(svtConditions.toString());
        
        EcalConditions ecalConditions = mgr.getCachedConditions(EcalConditions.class, "ecal_conditions").getCachedData();
        TestCase.assertNotNull("EcalConditions is null!", ecalConditions);
        System.out.println(ecalConditions.toString());
        
        executed = true;
        
        LOG.info("Done checking collection IDs for Layer0 MC conditions!");
    }

}
