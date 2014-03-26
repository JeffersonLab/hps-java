package org.hps.users.meeg;

import java.util.List;

import org.hps.readout.ecal.ReadoutTimestamp;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.event.TrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HitTimePrintDriver.java,v 1.1 2013/03/20 00:09:43 meeg Exp $
 */
public class HitTimePrintDriver extends Driver {

    @Override
    protected void process(EventHeader event) {

        List<TrackerHit> trackerHits = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
        List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class, "EcalCalHits");

        System.out.println("Event with ECal timestamp " + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_ECAL, event) + ", SVT timestamp " + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event));
        for (CalorimeterHit hit : ecalHits) {
            System.out.println("Ecal: " + (hit.getTime() + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_ECAL, event)));
        }
        for (TrackerHit hit : trackerHits) {
            System.out.println("SVT: " + (hit.getTime() + ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event)));
        }
    }
}
