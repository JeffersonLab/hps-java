package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.hps.recon.ecal.HitExtraData;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * Calculate a running pedestal average for every channel from Mode7 FADCs. Uses
 * pedestals from the database if not available from the data.
 * 
 * TODO: Use Logger.
 * 
 * TODO: Timestamps from EVIO for some runs appear to not be monotonically increasing.
 *       This interferes with minLookbackTime, so it defaults to disabled and its setter
 *       is left private for now.
 * 
 * @version $Id: ECalRunningPedestalDriver.java,v 1.0 2015/02/10 00:00:00
 * @author <baltzell@jlab.org>
 */
public class ECalRunningPedestalDriver extends Driver {

    // limit array lengths:
    private final int limitLookbackEvents = 1000;

    // minimum number of readouts for running averages:
    // (if not satisfied, use pedestals from database)
    private int minLookbackEvents = 10;

    // maximum number of readouts for running averages:
    // (if too many, discard the oldest ones)
    private int maxLookbackEvents = 100;

    // oldest allowed time for running averages:
    // (discard older readouts ; negative = no time limit)
    private long maxLookbackTime = -1; // units = ms

    private static final String rawCollectionName = "EcalReadoutHits";
    private static final String extraDataRelationsName = "EcalReadoutExtraDataRelations";
    private static final String runningPedestalsName = "EcalRunningPedestals";

    // TODO:  Get this from somewhere else.
    private final int nChannels = 442;

    // running pedestal averages, one for each channel:
    private Map<EcalChannel, Double> runningPedestals = new HashMap<EcalChannel, Double>(
            nChannels);

    // recent event-by-event pedestals and timestamps:
    private Map<EcalChannel, List<Integer>> eventPedestals = new HashMap<EcalChannel, List<Integer>>();
    private Map<EcalChannel, List<Long>> eventTimestamps = new HashMap<EcalChannel, List<Long>>();

    private boolean debug = false;
    private EcalConditions ecalConditions = null;

    public ECalRunningPedestalDriver() {
    }

    @Override
    protected void startOfData() {
        System.out.println("ECalRunningPedestalDriver.minLookbackEvents = "
                + minLookbackEvents);
        System.out.println("ECalRunningPedestalDriver.maxLookbackEvents = "
                + maxLookbackEvents);
        System.out.println("ECalRunningPedestalDriver.maxLookbackTime   = "
                + maxLookbackTime);
    }

    @Override
    public void detectorChanged(Detector detector) {
        ecalConditions = ConditionsManager
                .defaultInstance()
                .getCachedConditions(EcalConditions.class,
                        TableConstants.ECAL_CONDITIONS).getCachedData();
        for (int ii = 0; ii < nChannels; ii++) {
            EcalChannel chan = findChannel(ii + 1);
            runningPedestals.put(chan,getStaticPedestal(chan));
            eventPedestals.put(chan,new ArrayList<Integer>());
            eventTimestamps.put(chan,new ArrayList<Long>());
        }
        if (debug) {
            System.out.println("Running and static pedestals better match here:");
            printPedestals();
        }
    }

    public void setMinLookbackEvents(int nev) {
        if (nev < 1) {
            System.err
                    .println("ECalRunningPedestalDriver:  Ignoring minLookbackEvents too small:  "
                            + nev);
            nev = 1;
        }
        minLookbackEvents = nev;
    }

    public void setMaxLookbackEvents(int nev) {
        if (nev > limitLookbackEvents) {
            System.err.println("ECalRunningPedestalDriver:  Ignoring maxLookbackEvents too big:  "
                    + nev);
            nev = limitLookbackEvents;
        }
        maxLookbackEvents = nev;
    }

    private void setMaxLookbackTime(int time) {
        maxLookbackTime = time;
    }

    public void printPedestals() {
        for (int ii = 1; ii <= nChannels; ii++) {
            EcalChannel chan = findChannel(ii);
            if (!runningPedestals.containsKey(chan)) {
                System.err.println("printPedestals:   Missing Channel:  " + ii);
                continue;
            }
            System.out.printf("(%d,%.2f,%.2f) ", chan.getChannelId(),
                    runningPedestals.get(chan), getStaticPedestal(chan));
        }
        System.out.printf("\n");
    }

    @Override
    protected void process(EventHeader event) {

        if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
            if (event.hasCollection(LCRelation.class, extraDataRelationsName)) {
                for (LCRelation rel : event.get(LCRelation.class,
                        extraDataRelationsName)) {
                    RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                    GenericObject extraData = (GenericObject) rel.getTo();
                    updatePedestal(event, hit, extraData);
                }
            }
        }
        event.put(runningPedestalsName, runningPedestals);
        if (debug) {
            printPedestals();
        }
    }

    private void updatePedestal(EventHeader event, RawCalorimeterHit hit,
            GenericObject mode7data) {

        final long timestamp = event.getTimeStamp();
        final int min = ((HitExtraData.Mode7Data) mode7data).getAmplLow();
        final int max = ((HitExtraData.Mode7Data) mode7data).getAmplHigh();

        // ignore if pulse at beginning of window:
        if (max <= 0)
            return;

        EcalChannel chan = findChannel(hit);
        if (chan == null) {
            System.err.println("hit doesn't correspond to ecalchannel");
            return;
        }
        List<Integer> peds = eventPedestals.get(chan);
        List<Long> times = eventTimestamps.get(chan);

        if (maxLookbackTime > 0) {
            // If new timestamp is older than previous one, restart pedestals.
            // This should never happen unless firmware counter cycles back to zero,
            // in which case it could be dealt with if max timestamp is known.
            if (times.size() > 0 && times.get(0) > timestamp) {
                System.err.println(String.format(
                        "Event #%d, Old Timestamp:  %d < %d",
                        event.getEventNumber(), timestamp, times.get(0)));
                peds.clear();
                times.clear();
            }
        }

        // add pedestal to the list:
        peds.add(min);
        times.add(timestamp);

        if (peds.size() > 1) {

            // remove oldest pedestal if surpassed limit on #events:
            if (peds.size() > limitLookbackEvents
                    || (maxLookbackEvents > 0 && peds.size() > maxLookbackEvents)) {
                peds.remove(0);
                times.remove(0);
            }

            // remove old pedestals surpassing limit on lookback time:
            if (maxLookbackTime > 0) {
                while (times.size() > 1) {
                    if (times.get(0) < timestamp - maxLookbackTime * 1e6) {
                        times.remove(0);
                        peds.remove(0);
                    } else {
                        break;
                    }
                }
            }
        }

        // Update running pedestal average:
        double ped = 0;
        if (peds.size() >= minLookbackEvents) {
            for (int jj = 0; jj < peds.size(); jj++) {
                ped += peds.get(jj);
            }
            ped /= peds.size();
        } else {
            ped = getStaticPedestal(chan);
        }
        runningPedestals.put(chan, ped);

    }

    public double getStaticPedestal(EcalChannel chan) {
        return ecalConditions.getChannelConstants(chan).getCalibration()
                .getPedestal();
    }

    public EcalChannel findChannel(int channel_id) {
        return ecalConditions.getChannelCollection().findChannel(channel_id);
    }

    public EcalChannel findChannel(RawCalorimeterHit hit) {
        return ecalConditions.getChannelCollection().findGeometric(
                hit.getCellID());
    }

}
