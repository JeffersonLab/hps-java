package org.hps.recon.tracking;

import hep.physics.matrix.BasicMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.VecOp;

import java.util.List;

import org.lcsim.util.Driver;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.LineSegment3D;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.geometry.Detector;

/**
 * Driver to setup SVT SiSensors.
 * 
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Omar Moreno   <omoreno@ucsc.edu>
 * @author Per Hansson <phansson@slac.stanford.edu>
 *
 */
public class SvtSensorSetup extends Driver {

	//-----------------//
	//--- Constants ---//
	//-----------------//
	
	// SVT Subdetector name
	public static final String SVT_SUBDETECTOR_NAME = "Tracker";

    private double readoutStripPitch = 0.060; // mm
    private double senseStripPitch = 0.030; // mm
    private double readoutStripCapacitanceIntercept = 0;
    private double readoutStripCapacitanceSlope = 0.16; // pf/mm
    private double senseStripCapacitanceIntercept = 0;
    private double senseStripCapacitanceSlope = 0.16; // pf/mm
    private double readoutTransferEfficiency = 0.986;
    private double senseTransferEfficiency = 0.419;
	
    /*
     * Adding separate strip capacitance for long detectors following
     * S/N = mip_charge/(270e- + 36*C[pf/cm]*L[cm]
     * e.g. for expected S/N=16 and L=20cm -> C=0.1708pf/mm
     * e.g. for expected S/N=8 and L=20cm -> C=0.39pf/mm
     * FIXME: This should be taken into account by the noise model.
     */
    private double longSensorLengthThreshold = 190.0; //mm
    private double readoutLongStripCapacitanceSlope = 0.39;  // pf/mm
    private double senseLongStripCapacitanceSlope = 0.39;  // pf/mm

    boolean debug = false; 
    
    
	public SvtSensorSetup(){};
	
    /**
     * Set readout strip pitch
     * 
     * @param readoutStripPitch readout strip pitch
     */
    public void setReadoutStripPitch(double readoutStripPitch) {
        this.readoutStripPitch = readoutStripPitch;
    }
	
    /**
     * Set sense strip pitch
     * 
     * @param senseStripPitch sensor strip pitch
     */
    public void setSenseStripPitch(double senseStripPitch) {
        this.senseStripPitch = senseStripPitch;
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
   
    /**
     * Enable/disable debug mode
     * 
     * @param debug True to enable, false otherwise
     */
    public void setDebug(boolean debug){
    	this.debug = debug; 
    }
    
    /**
     * 
     */
	public void detectorChanged(Detector detector){
		
	    if (detector.getSubdetector(SVT_SUBDETECTOR_NAME) != null) {
	    
	        // Get the collection of all SiSensors from the SVT 
	        List<SiSensor> sensors 
        	    = detector.getSubdetector(SVT_SUBDETECTOR_NAME)
        	        .getDetectorElement().findDescendants(SiSensor.class);
		
	        // Loop through all of the sensors and configure them
	        for(SiSensor sensor : sensors){
	            this.setupSensor(sensor);
	        }
	    } else {
	        getLogger().warning("no SVT detector found");
	    }
	}
	
	/**
	 * Setup an SVT SiSensor object
	 * 
	 * @param SiSensor : SVT SiSensor
	 */
	protected void setupSensor(SiSensor sensor){
		
			this.printDebug("Setting up sensor " + sensor.getName());
	
			// Get the solid corresponding to the sensor volume
            Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();

            // Get the faces of the solid corresponding to the n and p sides of the sensor 
            Polygon3D pSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
            Polygon3D nSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, -1)).get(0);

            // p side collects holes.
            sensor.setBiasSurface(ChargeCarrier.HOLE, pSide);
            
            // n side collects electrons.
            sensor.setBiasSurface(ChargeCarrier.ELECTRON, nSide);

            // Translate to the outside of the sensor solid in order to setup electrodes.
            ITranslation3D electrodesPosition = new Translation3D(VecOp.mult(-pSide.getDistance(), pSide.getNormal()));
	
            // Align the strips with the edge of the sensor.        
            IRotation3D electrodesRotation = new RotationPassiveXYZ(0, 0, 0);
            Transform3D electrodesTransform = new Transform3D(electrodesPosition, electrodesRotation);

            // Set the number of readout and sense electrodes.
            SiStrips readoutElectrodes = new SiStrips(ChargeCarrier.HOLE, readoutStripPitch, sensor, electrodesTransform);
            SiStrips senseElectrodes = new SiStrips(ChargeCarrier.HOLE, senseStripPitch, (readoutElectrodes.getNCells() * 2 - 1), sensor, electrodesTransform);

            this.printDebug("Number of readout strips: " + readoutElectrodes.getNCells());
            this.printDebug("Number of sense strips: " + senseElectrodes.getNCells());
            
            double roCap = this.getStripLength(sensor) > longSensorLengthThreshold ? readoutLongStripCapacitanceSlope : readoutStripCapacitanceSlope;
            double senseCap = this.getStripLength(sensor) > longSensorLengthThreshold ? senseLongStripCapacitanceSlope : senseStripCapacitanceSlope;

            
            // Set the strip capacitance.
            readoutElectrodes.setCapacitanceIntercept(readoutStripCapacitanceIntercept);
            readoutElectrodes.setCapacitanceSlope(roCap);
            senseElectrodes.setCapacitanceIntercept(senseStripCapacitanceIntercept);
            senseElectrodes.setCapacitanceSlope(senseCap);
            
            this.printDebug("Sensor " + sensor.getName() + " has a strip length " + this.getStripLength(sensor));
            this.printDebug("Capacitance of readout electrodes: " + readoutElectrodes.getCapacitance() 
            					+ " (Cell 0: " + readoutElectrodes.getCapacitance(0) + ")");
            this.printDebug("Capacitance of sensor electrodes: " + senseElectrodes.getCapacitance() 
            					+ " (Cell 0: " + senseElectrodes.getCapacitance(0) + ")");
            
            // Set sense and readout electrodes.
            sensor.setSenseElectrodes(senseElectrodes);
            sensor.setReadoutElectrodes(readoutElectrodes);

            // Set the charge transfer efficiency.
            double[][] transferEfficiencies = {{readoutTransferEfficiency, senseTransferEfficiency}};
            sensor.setTransferEfficiencies(ChargeCarrier.HOLE, new BasicMatrix(transferEfficiencies));
	
            this.printDebug("----------------------------------------------------------------");
	}
   
	/**
	 * Get the length of a sensor strip.  This is getting the face of the 
	 * sensor and returning the length of the longest edge.
	 * FIXME: Possibly dangerous.
	 * 
	 * @param sensor
	 * @return  The length of the longest sensor edge
	 */
	protected double getStripLength(SiSensor sensor) {
		double length = 0;
        List<Polygon3D> faces = ((Box) sensor.getGeometry().getLogicalVolume().getSolid()).getFacesNormalTo(new BasicHep3Vector(0,0,1));
        for(Polygon3D face : faces) {
            List<LineSegment3D> edges = face.getEdges();
            for(LineSegment3D edge : edges) {
                double l = edge.getLength();
                if(l>length) {
                    length = l;
                }
            }
        }

        return length;
    }
	
	/**
	 * If debug is enabled, prints a debug statement
	 * 
	 * @param message : Debug message
	 */
	private void printDebug(String message){
		if(debug){
			System.out.println(this.getClass().getSimpleName() + ": " + message);
		}
	}
}
