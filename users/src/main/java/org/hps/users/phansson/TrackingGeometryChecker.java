package org.hps.users.phansson;

import java.util.List;

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
	}
}


