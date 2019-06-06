package org.hps.recon.tracking.lit;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.List;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 *
 * @author Norman A Graf
 *
 * @version $Id:
 */
public class StripHitAnalysisDriver extends Driver {

    @Override
    protected void detectorChanged(Detector detector) {
    }

    @Override
    protected void process(EventHeader event) {
        List<TrackerHit> stripHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
        System.out.println("found " + stripHits.size() + " strip hits");
        for (TrackerHit h : stripHits) {
            List rawHits = h.getRawHits();
            HpsSiSensor sensor = null;
            for (Object o : rawHits) {
                RawTrackerHit rth = (RawTrackerHit) o;
                // TODO figure out why the following collection is always null
                List<SimTrackerHit> stipMCHits = rth.getSimTrackerHits();
                System.out.println(rth.getDetectorElement());
                sensor = (HpsSiSensor) rth.getDetectorElement();
            }
            Hep3Vector globalPos = new BasicHep3Vector(h.getPosition());
            if (sensor != null) {
                Hep3Vector localPos = sensor.getGeometry().getGlobalToLocal().transformed(globalPos);
                // OK, now let's try the explicit rotation and translation...
                long ID = sensor.getIdentifier().getValue();
            }
        }
    }

}
