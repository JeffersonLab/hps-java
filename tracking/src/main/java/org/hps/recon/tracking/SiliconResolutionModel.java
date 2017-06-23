package org.hps.recon.tracking;

import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;

public interface SiliconResolutionModel {
    public double getMeasuredResolution(List<FittedRawTrackerHit> cluster, SiSensorElectrodes electrodes);
    public double getUnmeasuredResolution(List<FittedRawTrackerHit> cluster, SiSensorElectrodes electrodes, Map<FittedRawTrackerHit, Integer> strip_map);
}
