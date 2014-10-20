package org.hps.recon.tracking;

import java.util.Iterator;
import java.util.List;

import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;

/**
 * Driver used to filter out RawTrackerHits that have been identified to come
 * from noisy/dead channels.  
 * 
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id$
 * 
 */
public class SVTBadChannelFilterDriver extends Driver {

	// RawTrackerHit collection name 
    private String rawTrackerHitCollection = "SVTRawTrackerHits";

    @Override
    public void process(EventHeader event) {
    	
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollection)) {
            
        	// Get the list of raw hits from the event
        	List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawTrackerHitCollection);
        	
            // Get the hits meta data from the event
        	LCMetaData meta = event.getMetaData(hits);
            
        	// Iterate over all raw hits in the event.  If the raw hit is 
        	// identified to come from a noisy/bad channel, remove it from
        	// the list of raw hits.
            Iterator<RawTrackerHit> hitsIterator = hits.iterator();
            while (hitsIterator.hasNext()) {
                
            	RawTrackerHit hit = hitsIterator.next();
                hit.setMetaData(meta);
                int strip = hit.getIdentifierFieldValue("strip");
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();

                if(sensor.isBadChannel(strip)){
                	hitsIterator.remove();
                }

                if (!sensor.getReadout().getHits(RawTrackerHit.class).isEmpty()) {
                    throw new RuntimeException(this.getClass().getSimpleName() + " must be run before any SVT readout drivers.");
                }
            }
        }
    }
}
