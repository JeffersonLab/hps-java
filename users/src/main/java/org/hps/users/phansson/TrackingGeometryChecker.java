package org.hps.users.phansson;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**	 
 * Check tracking geometry.
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 *
 */
public class TrackingGeometryChecker extends Driver {

	private int debug = 1;

	/**
	 * Check tracking geometry.
	 */
	public TrackingGeometryChecker() {
	}
	
	protected void detectorChanged(Detector arg0) {
		super.detectorChanged(arg0);
		
		 // Get all of the sensors composing the SVT and add them to the set of all sensors
        IDetectorElement detectorElement = arg0.getDetectorElement().findDetectorElement("Tracker");
        IIdentifierHelper helper = detectorElement.getIdentifierHelper();
        List<SiSensor> sensors = new ArrayList<SiSensor>();
		sensors.addAll(detectorElement.findDescendants(SiSensor.class));
        System.out.printf("%s: Total number of sensors: %d\n",getClass().getSimpleName(), sensors.size());

        IIdentifier sensorIdent;
        SiTrackerIdentifierHelper sensorHelper;
        String description;
        // Loop through all of the sensors and fill the maps
        for (SiSensor sensor : sensors) {

            // Get the sensor identifier
            sensorIdent = sensor.getIdentifier();

            // Get the sensor identifier helper in order to decode the id fields
            sensorHelper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();

            // Get the sensor layer and module id
            int layerNumber = sensorHelper.getLayerValue(sensorIdent);
            int moduleNumber = sensorHelper.getModuleValue(sensorIdent);
            int sideNumber = sensorHelper.getSideValue(sensorIdent);
            int sensorNumber = sensorHelper.getSensorValue(sensorIdent);
            int elecNumber = sensorHelper.getElectrodeValue(sensorIdent);
            
            System.out.printf("%s: Sensor name %s\n",getClass().getSimpleName(), sensor.getName());
            System.out.printf("%s: Sensor position %s\n",getClass().getSimpleName(), sensor.getGeometry().getPosition().toString());
            System.out.printf("%s: Sensor is %s %s \n",getClass().getSimpleName(), SvtUtils.getInstance().isTopLayer(sensor)?"top":"bottom", SvtUtils.getInstance().isAxial(sensor)?"axial":"stereo");
            System.out.printf("%s: layerNumber %d\n",getClass().getSimpleName(), layerNumber);
            System.out.printf("%s: moduleNumber %d\n",getClass().getSimpleName(), moduleNumber);
            System.out.printf("%s: sideNumber %d\n",getClass().getSimpleName(), sideNumber);
            System.out.printf("%s: sensorNumber %d\n",getClass().getSimpleName(), sensorNumber);
            System.out.printf("%s: elecNumber %d\n",getClass().getSimpleName(), elecNumber);
            System.out.printf("%s: DE mother lists:\n",getClass().getSimpleName(), elecNumber);
            IDetectorElement m = sensor;
            int im=0;
            while((m=m.getParent()) != null) {
            	System.out.printf("%s: DE mother %d name %s\n",getClass().getSimpleName(), im, m.getName());
            	if(m.hasGeometryInfo()) {
            		System.out.printf("%s: DE mother %d pos  %s\n",getClass().getSimpleName(), im, m.getGeometry().getPosition().toString());
            	} else {
            		System.out.printf("%s: DE mother - no geom info - \n",getClass().getSimpleName(), im);            		
            	}
                im++;
            }

            
        }
		
	}
	
	protected void startOfData() {
		super.startOfData();
	}
	
	protected void process(EventHeader event) {
		
		List<SimTrackerHit> simTrackerHits = event.getSimTrackerHits("TrackerHits");
        if (simTrackerHits == null) {
            throw new RuntimeException("Missing SimTrackerHit collection");
        }
        
        if(debug>0) System.out.printf("%s: found %d simTrackerHits\n",getClass().getSimpleName(),simTrackerHits.size());
        for(SimTrackerHit simTrackerHit : simTrackerHits) {
        	if(debug>0) printSimTrackerHitInfo(simTrackerHit);
        }
	}
	
	protected void endOfData() {
		super.endOfData();
	}

	protected int getDebug() {
		return debug;
	}

	protected void setDebug(int debug) {
		this.debug = debug;
	}

	private static void printSimTrackerHitInfo(SimTrackerHit simTrackerHit) {
		System.out.printf("\nSimTrackerHit:\n");
		System.out.printf("\t position: %s\n",simTrackerHit.getPositionVec().toString());
		System.out.printf("\t DetectorElement: %s\n",simTrackerHit.getDetectorElement().getName());
		System.out.printf("\t DetectorElement position: %s\n",simTrackerHit.getDetectorElement().getGeometry().getPosition().toString());
		System.out.printf("\t PhysVol name at position: %s\n",simTrackerHit.getDetectorElement().getGeometry().getPhysicalVolume(simTrackerHit.getPositionVec()).getName());
	}
}


