package org.lcsim.detector.tracker.silicon;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.identifier.IIdentifier;

import hep.physics.matrix.BasicMatrix;

/**
 * Description of the layer 0 sensors used by the SVT. This class extends
 * {@link HpsSiSensor} but overrides several properties such as strip pitch and
 * sense transfer efficiency.  It should be noted that this sensor has no
 * intermediate strips and that is why the sense transfer efficiency is 0.
 *
 * @author Omar Moreno, SLAC National Accelerator Laboratory
 */
public class HpsThinSiSensor extends HpsSiSensor {

    public HpsThinSiSensor(int sensorid, String name, IDetectorElement parent, String support, IIdentifier id) {
        super(sensorid, name, parent, support, id);
    }

    /** @return The total number of sense strips per sensor. */
    @Override
    public int getNumberOfSenseStrips() {
        return 510;
    }

    /** @return The readout strip pitch in mm. */
    @Override
    public double getReadoutStripPitch() {
        return 0.055; // mm
    }

    /** @return The sense strip pitch in mm. */
    @Override
    public double getSenseStripPitch() {
        return 0.055; // mm
    }

    /**
     * Get the charge transfer efficiency of the sense strips. The thin sensors
     * don't have an intermediate strip so the charge transfer efficiency is
     * zero.
     *
     * @return The charge transfer efficiency of the sense strips.
     */
    @Override
    public double getSenseTransferEfficiency() {
        return 0.0;
    }
    
    /**
     * Setup the geometry and electrical characteristics of an {@link HpsThinSiSensor}
     */
    @Override
    public void initialize() {

        Transform3D electrodesTransform = this.getElectrodesTransform();

        // Set the number of readout and sense electrodes.
        final SiStrips readoutElectrodes = new ThinSiStrips(ChargeCarrier.HOLE, getReadoutStripPitch(), this,
                electrodesTransform);
        final SiStrips senseElectrodes = new ThinSiStrips(ChargeCarrier.HOLE, getSenseStripPitch(),
                this.getNumberOfSenseStrips(), this, electrodesTransform);

        final double readoutCapacitance = this.getStripLength() > this.longSensorLengthThreshold ? this.readoutLongStripCapacitanceSlope
                : this.readoutStripCapacitanceSlope;
        final double senseCapacitance = this.getStripLength() > this.longSensorLengthThreshold ? this.senseLongStripCapacitanceSlope
                : this.senseStripCapacitanceSlope;

        // Set the strip capacitance.
        readoutElectrodes.setCapacitanceIntercept(this.readoutStripCapacitanceIntercept);
        readoutElectrodes.setCapacitanceSlope(readoutCapacitance);
        senseElectrodes.setCapacitanceIntercept(this.senseStripCapacitanceIntercept);
        senseElectrodes.setCapacitanceSlope(senseCapacitance);

        // Set sense and readout electrodes.
        this.setSenseElectrodes(senseElectrodes);
        this.setReadoutElectrodes(readoutElectrodes);

        // Set the charge transfer efficiency of both the sense and readout
        // strips.
        final double[][] transferEfficiencies = {{this.getReadoutTransferEfficiency(), this.getSenseTransferEfficiency()}};
        this.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));

    }
}
