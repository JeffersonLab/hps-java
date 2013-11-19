package org.lcsim.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lcsim.detector.*;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Setup driver for the HPSTracker subdetector.
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno   <omoreno@slac.stanford.edu>
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @version $Id: HPSSVTSensorSetup.java,v 1.13 2013/10/29 23:28:53 jeremy Exp $
 */
public class HPSSVTSensorSetup extends Driver {

    boolean debug = false;
    String subdetectorName = "Tracker";
    // Sensor Characteristics
    private double readoutStripPitch = 0.060;    // mm
    private double senseStripPitch = 0.030;    // mm
    private double readoutStripCapacitanceIntercept = 0;
    private double readoutStripCapacitanceSlope = 0.16;  // pf/mm
    private double senseStripCapacitanceIntercept = 0;
    private double senseStripCapacitanceSlope = 0.16;   // pf/mm
    private double readoutTransferEfficiency = 0.986;
    private double senseTransferEfficiency = 0.419;
    /*
     * Adding separate strip capacitance for long detectors following
     * S/N = mip_charge/(270e- + 36*C[pf/cm]*L[cm]
     * e.g. for expected S/N=16 and L=20cm -> C=0.1708pf/mm
     * e.g. for expected S/N=8 and L=20cm -> C=0.39pf/mm
     * This should be taken into account by the noise model -> FIX THIS.
     */
    private double longSensorLengthThreshold = 190.0; //mm
    private double readoutLongStripCapacitanceSlope = 0.39;  // pf/mm
    private double senseLongStripCapacitanceSlope = 0.39;  // pf/mm

    
    // Set of sensors
    Set<SiSensor> sensors = new HashSet<SiSensor>();

    public HPSSVTSensorSetup() {
    }

    public HPSSVTSensorSetup(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    @Override
    public void detectorChanged(Detector detector) {
        if (subdetectorName == null) {
            throw new RuntimeException("The subdetector name was not set!");
        }

        // Get the SVT
        IDetectorElement detectorElement = detector.getDetectorElement().findDetectorElement(subdetectorName);

        // Get all SVT sensors
        sensors.addAll(detectorElement.findDescendants(SiSensor.class));
        if (debug) {
            System.err.println(this.getClass().getName() + ": Added " + sensors.size() + " sensors");
        }

        // Configure the sensors
        configureSensors(sensors);

        // Create DAQ Maps
        //if (!SvtUtils.getInstance().isSetup()) {
        SvtUtils.getInstance().reset(); // Hard reset of SvtUtils to clear previous detector state.
        SvtUtils.getInstance().setup(detector);
        //}
    }

    /**
     * Configure the SVT sensors
     * 
     * @param subdetector  
     */
    private void configureSensors(Set<SiSensor> sensors) {
        // Loop through all the sensors in the set.
        for (SiSensor sensor : sensors) {

            if (debug) {
                System.out.println(this.getClass().getSimpleName() + " - setting up sensor " + sensor.getName());
            }

            Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();

            Polygon3D pSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
            Polygon3D nSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, -1)).get(0);

            // P side collects holes.
            sensor.setBiasSurface(ChargeCarrier.HOLE, pSide);
            // N side collects electrons.
            sensor.setBiasSurface(ChargeCarrier.ELECTRON, nSide);

            // Translate to the outside of the box in order to setup electrodes.
            ITranslation3D electrodesPosition = new Translation3D(VecOp.mult(-pSide.getDistance(), pSide.getNormal()));

            // Align the strips with the edge of the sensor.        
            IRotation3D electrodesRotation = new RotationPassiveXYZ(0, 0, 0);
            Transform3D electrodesTransform = new Transform3D(electrodesPosition, electrodesRotation);

            // Set the number of readout and sense electrodes.
            SiStrips readoutElectrodes = new SiStrips(ChargeCarrier.HOLE, readoutStripPitch, sensor, electrodesTransform);
            SiStrips senseElectrodes = new SiStrips(ChargeCarrier.HOLE, senseStripPitch, (readoutElectrodes.getNCells() * 2 - 1), sensor, electrodesTransform);

            if (debug) {
                System.out.println("The number of readout strips is " + readoutElectrodes.getNCells());
                System.out.println("The number of sense strips is " + senseElectrodes.getNCells());
            }
            
            double roCap = this.getStripLength(sensor) > longSensorLengthThreshold ? readoutLongStripCapacitanceSlope : readoutStripCapacitanceSlope;
            double senseCap = this.getStripLength(sensor) > longSensorLengthThreshold ? senseLongStripCapacitanceSlope : senseStripCapacitanceSlope;
                    
            // Set the strip capacitance.
            readoutElectrodes.setCapacitanceIntercept(readoutStripCapacitanceIntercept);
            readoutElectrodes.setCapacitanceSlope(roCap);
            senseElectrodes.setCapacitanceIntercept(senseStripCapacitanceIntercept);
            senseElectrodes.setCapacitanceSlope(senseCap);

            if(debug) {
                System.out.printf("%s: Sensor %s has strip length %.3f\n",this.getClass().getSimpleName(),sensor.getName(),this.getStripLength(sensor));
                System.out.printf("%s: ro electrodes capacitance %.3f (cell0 %.3f)\n",this.getClass().getSimpleName(),readoutElectrodes.getCapacitance(),readoutElectrodes.getCapacitance(0));
                System.out.printf("%s: ro sense capacitance %.3f (cell0 %.3f)\n",this.getClass().getSimpleName(),senseElectrodes.getCapacitance(),senseElectrodes.getCapacitance(0));
            }
            
            // Set sense and readout electrodes.
            sensor.setSenseElectrodes(senseElectrodes);
            sensor.setReadoutElectrodes(readoutElectrodes);

            // Set the charge transfer efficiency.
            double[][] transferEfficiencies = {{readoutTransferEfficiency, senseTransferEfficiency}};
            sensor.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));

            if (debug) {
                System.out.println("----------------------------");
            }
        }
    }

    double getStripLength(SiSensor sensor) {
        /*
         * Returns the length of the strip
         * This is getting the face of the sensor and then getting the longest edge
         *  VERY DANGEROUS -> FIX THIS!
         */
        double length = 0;
        List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid()).getFacesNormalTo(new BasicHep3Vector(0,0,1));
        for(Polygon3D face : faces) {
            //System.out.printf("%s: Sensor %s polygon3D %s\n",this.getClass().getSimpleName(),sensor.getName(),face.toString());
            List<LineSegment3D> edges = face.getEdges();
            for(LineSegment3D edge : edges) {
                double l = edge.getLength();
                if(l>length) {
                    length = l;
                }
                //System.out.printf("%s: edge %.3f \n",this.getClass().getSimpleName(),edge.getLength());
            }
        }

        return length;
    }
    
    /**
     * Set the readout strip capacitance
     * 
     * @param intercept
     * @param slope 
     */
    public void setReadoutStripCapacitance(double intercept, double slope) {
        readoutStripCapacitanceIntercept = intercept;
        readoutStripCapacitanceSlope = slope;
    }

    /**
     * Set the sense strip capacitance 
     * 
     * @param intercept
     * @param slope 
     */
    public void setSenseStripCapacitance(double intercept, double slope) {
        senseStripCapacitanceIntercept = intercept;
        senseStripCapacitanceSlope = slope;
    }

    /**
     * Set readout strip pitch
     * 
     * @param strip pitch
     */
    public void setReadoutStripPitch(double pitch) {
        readoutStripPitch = pitch;
    }

    /**
     * Set sense strip pitch
     * 
     * @param strip pitch
     */
    public void setSenseStripPitch(double pitch) {
        senseStripPitch = pitch;
    }

    /**
     * Set readout strip transfer efficiency
     * 
     * @param efficiency 
     */
    public void setReadoutTransferEfficiency(double efficiency) {
        readoutTransferEfficiency = efficiency;
    }

    /**
     * Set sense strip transfer efficiency
     * 
     * @param efficiency
     */
    public void setSenseTransferEfficiency(double efficiency) {
        senseTransferEfficiency = efficiency;
    }
}
