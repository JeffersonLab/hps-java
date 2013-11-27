package org.lcsim.hps.conditions;

import org.lcsim.conditions.ConditionsEvent;
import org.lcsim.conditions.ConditionsListener;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.conditions.ConditionsSet;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.hps.recon.ecal.EcalConditions;
import org.lcsim.hps.recon.tracking.FieldMap;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants;
import org.lcsim.hps.recon.tracking.HPSSVTSensorSetup;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: CalibrationDriver.java,v 1.1 2013/10/25 19:39:46 jeremy Exp $
 */
public class CalibrationDriver extends Driver implements ConditionsListener {

    // The test run number of interest.  If it equals -1, the default calibrations
    // are loaded
    private static int runNumber = -1;
    private boolean fixRunNumber = false;
	private String gainFilename = "default.gain";

    public CalibrationDriver() {
        add(new EcalConditions());
        add(new HPSSVTSensorSetup());
    }

    public void setRunNumber(int runNumber) {
        CalibrationDriver.runNumber = runNumber;
        fixRunNumber = true;
    }

    public void setGainFilename(String gainFileName) {
        this.gainFilename = gainFileName;
    }

    public static int runNumber() {
        return runNumber;
    }

    @Override
    protected void process(EventHeader event) {
        super.process(event);
        if (!fixRunNumber && runNumber != event.getRunNumber()) {
            runNumber = event.getRunNumber();
        }
    }

    @Override
    protected void detectorChanged(Detector detector) {
        super.detectorChanged(detector);

        if (!EcalConditions.calibrationLoaded()) {
        	EcalConditions.setGainFilename(gainFilename);
        	EcalConditions.loadCalibration();
        }
        if (fixRunNumber && (!HPSSVTCalibrationConstants.pedestalLoaded() || !HPSSVTCalibrationConstants.tpLoaded())) {
            System.out.println("Loading calibration for set run: " + runNumber);
            loadCalibsByRun(runNumber);
        }
    }

    @Override
    protected void startOfData() {
        ConditionsManager.defaultInstance().addConditionsListener(this);
    }

    @Override
    public void conditionsChanged(ConditionsEvent ce) {
        if (!fixRunNumber) {
            System.out.println("Got ConditionsEvent with run: " + ce.getConditionsManager().getRun());
            runNumber = ce.getConditionsManager().getRun();
            loadCalibsByRun(runNumber);
        }
    }

    private void loadCalibsByRun(int run) {
        HPSSVTCalibrationConstants.loadCalibration(run);
        FieldMap.loadFieldMap(run);
    }

    /**
     * get specified conditions list, parse as a map of run numbers to calibration file paths; get the appropriate file 
     * @param calibName 
     * @param run
     * @return
     */
    public static String getCalibForRun(String calibName, int run) {
        System.out.println("Reading calibrations " + calibName + " for run: " + run);

        ConditionsSet calibSet = ConditionsManager.defaultInstance().getConditions(calibName);

        int mostRecentValid = Integer.MIN_VALUE;
        String filePath = null;
        for (Object key : calibSet.keySet()) {
            int keyRun = Integer.parseInt((String) key);
//            System.out.println("Available calibration for run: " + keyRun);
            if (keyRun > mostRecentValid && keyRun <= run) {
                mostRecentValid = keyRun;
                filePath = calibSet.getString((String) key);
            }
        }
        System.out.println("Use this calibration from run " + mostRecentValid + ": " + filePath);
        return filePath;
    }
}
