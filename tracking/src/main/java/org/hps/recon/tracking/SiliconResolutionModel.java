package org.hps.recon.tracking;

import java.util.List;
import java.util.Map;

import org.lcsim.event.LCRelation;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;

import hep.physics.vec.Hep3Vector;

public interface SiliconResolutionModel {
    public double getMeasuredResolution(List< LCRelation > cluster, SiSensorElectrodes electrodes);
    public double getUnmeasuredResolution(List< LCRelation > cluster, SiSensorElectrodes electrodes, Map<LCRelation, Integer> strip_map);
    public Hep3Vector weightedAveragePosition(List<Double> signals, List<Hep3Vector> positions);
}
