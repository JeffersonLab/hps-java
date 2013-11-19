package org.lcsim.hps.recon.tracking;

import java.util.Iterator;
import java.util.List;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: SVTBadChannelFilterDriver.java,v 1.2 2012/08/29 21:02:46 meeg Exp $
 */
public class SVTBadChannelFilterDriver extends Driver {

    private String rawTrackerHitCollection = "SVTRawTrackerHits";

    @Override
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollection)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawTrackerHitCollection);
            LCMetaData meta = event.getMetaData(hits);
            Iterator<RawTrackerHit> i = hits.iterator();
            while (i.hasNext()) {
                RawTrackerHit hit = i.next();
                hit.setMetaData(meta);
                int strip = hit.getIdentifierFieldValue("strip");
                SiSensor sensor = (SiSensor) hit.getDetectorElement();

//                System.out.format("module %d, layer %d, strip %d\n", hit.getIdentifierFieldValue("module"), hit.getIdentifierFieldValue("layer"), hit.getIdentifierFieldValue("strip"));
                if (HPSSVTCalibrationConstants.isBadChannel(sensor, strip)) {
                    i.remove();
                }

                if (!sensor.getReadout().getHits(RawTrackerHit.class).isEmpty()) {
                    throw new RuntimeException(this.getClass().getSimpleName() + " must be run before any SVT readout drivers.");
                }
            }
        }
    }
}
