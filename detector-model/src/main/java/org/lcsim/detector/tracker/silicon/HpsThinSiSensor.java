package org.lcsim.detector.tracker.silicon;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;

public class HpsThinSiSensor extends HpsSiSensor {

    private final static double READOUT_STRIP_PITCH = 0.055; // mm
    private final static double SENSE_STRIP_PITCH = 0.055; // mm
    private final static double STRIPS_PER_SENSOR = 255;// mm

    public HpsThinSiSensor(int sensorid, String name, IDetectorElement parent,
            String support, IIdentifier id) {
        super(sensorid, name, parent, support, id);

    }

    @Override
    public double getReadoutStripPitch() {
        return READOUT_STRIP_PITCH;
    }

    @Override
    public double getSenseStripPitch() {
        return SENSE_STRIP_PITCH;
    }
    
    @Override
    public double getStripsPerSensor() {
        return STRIPS_PER_SENSOR;
    }

}
