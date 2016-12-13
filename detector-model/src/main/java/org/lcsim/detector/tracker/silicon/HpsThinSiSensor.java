package org.lcsim.detector.tracker.silicon;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;

public class HpsThinSiSensor extends HpsSiSensor {

    private final double readoutStripPitch = 0.055; // mm
    private final double senseStripPitch = 0.055;   // mm
    private final int numberOfSenseStrips = 256;

    public HpsThinSiSensor(int sensorid, String name, IDetectorElement parent, String support, IIdentifier id) {
        super(sensorid, name, parent, support, id);

    }

    @Override
    public int getNumberOfSenseStrips() {
        return this.numberOfSenseStrips;
    }

    @Override
    public double getReadoutStripPitch() {
        return readoutStripPitch;
    }

    @Override
    public double getSenseStripPitch() {
        return senseStripPitch;
    }
}    

