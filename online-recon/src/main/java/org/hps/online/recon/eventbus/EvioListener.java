package org.hps.online.recon.eventbus;

import org.hps.job.JobManager;
import org.hps.online.recon.Station;
import org.hps.record.LCSimEventBuilder;
import org.hps.record.evio.EvioEventUtilities;
import org.jlab.coda.jevio.EvioEvent;
import org.lcsim.event.EventHeader;

import com.google.common.eventbus.Subscribe;

/**
 * Receive EVIO events and convert to raw LCIO
 */
public class EvioListener {

    private OnlineEventBus eventbus;
    private LCSimEventBuilder builder;
    private JobManager mgr;

    EvioListener(OnlineEventBus eventbus) {
        this.eventbus = eventbus;
        this.mgr = eventbus.getStation().getJobManager();
        this.builder = eventbus.getStation().getEventBuilder();
    }

    @Subscribe
    public void receiveEvioAndPostLcio(EvioEvent evioEvent) {
        try {
            /*eventbus.getLogger().info("Station " + eventbus.getStation().getStationName()
                    + " processing EVIO event: "
                    + EvioEventUtilities.getEventIdData(evioEvent)[0]);*/
            builder.readEvioEvent(evioEvent);
            EventHeader lcioEvent = builder.makeLCSimEvent(evioEvent);
            //eventbus.getLogger().info("Built LCIO event: " + lcioEvent.getEventNumber());
            mgr.processEvent(lcioEvent);
            eventbus.getLogger().info("Processed LCIO event: " + lcioEvent.getEventNumber());
            eventbus.post(lcioEvent);
        } catch (Exception e) {
            eventbus.error(e, false);
        }
    }
}
