package org.hps.recon.tracking;

import java.util.List;
import java.util.Map;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;

public class DefaultSiliconResolutionModel implements SiliconResolutionModel{

    @Override
    public double getMeasuredResolution(List<FittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) 
    
    {
        double measured_resolution;

        double sense_pitch = ((SiSensor) electrodes.getDetectorElement()).getSenseElectrodes(electrodes.getChargeCarrier()).getPitch(0);

        // double readout_pitch = electrodes.getPitch(0);
        // double noise =
        // _readout_chip.getChannel(strip_number).computeNoise(electrodes.getCapacitance(strip_number));
        // double signal_expected = (0.000280/DopedSilicon.ENERGY_EHPAIR) *
        // ((SiSensor)electrodes.getDetectorElement()).getThickness(); // ~280 KeV/mm for thick Si
        // sensors
        if (cluster.size() == 1) {
            measured_resolution = sense_pitch * _oneClusterErr;
        } else if (cluster.size() == 2) {
            measured_resolution = sense_pitch * _twoClusterErr;
        } else if (cluster.size() == 3) {
            measured_resolution = sense_pitch * _threeClusterErr;
        } else if (cluster.size() == 4) {
            measured_resolution = sense_pitch * _fourClusterErr;
        } else {
            measured_resolution = sense_pitch * _fiveClusterErr;
        }

        return measured_resolution;
    }

    public double getUnmeasuredResolution(List<FittedRawTrackerHit> cluster, SiSensorElectrodes electrodes, Map<FittedRawTrackerHit, Integer> strip_map) {
        // Get length of longest strip in hit
        double hit_length = 0;
        for (FittedRawTrackerHit hit : cluster) {
            hit_length = Math.max(hit_length, ((SiStrips) electrodes).getStripLength(strip_map.get(hit)));
        }
        return hit_length / Math.sqrt(12);
    }

    private double _oneClusterErr = 1 / Math.sqrt(12);
    private double _twoClusterErr = 1 / 5.;
    private double _threeClusterErr = 1 / 3.;
    private double _fourClusterErr = 1 / 2.;
    private double _fiveClusterErr = 1;
    
    public void setOneClusterErr(double err) {
        _oneClusterErr = err;
    }

    public void setTwoClusterErr(double err) {
        _twoClusterErr = err;
    }

    public void setThreeClusterErr(double err) {
        _threeClusterErr = err;
    }

    public void setFourClusterErr(double err) {
        _fourClusterErr = err;
    }

    public void setFiveClusterErr(double err) {
        _fiveClusterErr = err;
    }
}
