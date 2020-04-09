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

        // Get the transformation that will be used to place the electrodes on 
        // the face of the sensor. 
        Transform3D electrodesTransform = this.getElectrodesTransform();

        // Column pitch is the length of the sensor divided by 2.
        double stripLength = this.getStripLength()/2; 

        // Set the number of readout and sense electrodes.
        final SiStriplets readoutElectrodes = new SiStriplets(ChargeCarrier.HOLE, 
                           getReadoutStripPitch(), // Strip pitch = 55 um
                           stripLength,            // Column pitch 
                           this, 
                           electrodesTransform     // Parent to local transform
                           );   
       
        // TODO: Investigate whether this needs to be defined or if it can be 
        //       set to null.  
        final SiStriplets senseElectrodes = new SiStriplets(ChargeCarrier.HOLE, 
                           getReadoutStripPitch(), // Strip pitch = 55 um
                           stripLength,            // Column pitch 
                           this, 
                           electrodesTransform     // Parent to local transform
                           );   

        // Set the strip capacitance.
        double readoutCapacitance = this.readoutStripCapacitanceIntercept + readoutStripCapacitanceSlope*stripLength; 
        double senseCapacitance = this.senseStripCapacitanceIntercept + senseStripCapacitanceSlope*stripLength; 
        readoutElectrodes.setCapacitance(readoutCapacitance);

        senseElectrodes.setCapacitance(senseCapacitance);

        // Set sense and readout electrodes.
        this.setSenseElectrodes(senseElectrodes);
        this.setReadoutElectrodes(readoutElectrodes);

        // Set the charge transfer efficiency of both the sense and readout
        // strips.
        final double[][] transferEfficiencies = {{this.getReadoutTransferEfficiency(), this.getSenseTransferEfficiency()}};
        this.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));

    }
}
