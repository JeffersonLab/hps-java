package org.hps.users.baltzell;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannelConstants;
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
 * Calculate a running pedestal average for every channel from Mode7 FADCs.
 * @version $Id: ECalRunningPedestalDriver.java,v 0.0 2015/01/31 00:00:00
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

    // FIXME:
    private final int nChannels = 442;

    // running pedestal averages, one for each channel:
    private List<Double> runningPedestals = new ArrayList<Double>(nChannels);
    
    // FIXME:
    // recent event-by-event pedestals and timestamps:
    private List<Integer>[] eventPedestals = (ArrayList<Integer>[]) new ArrayList[nChannels];
    private List<Long>[] eventTimestamps = (ArrayList<Long>[]) new ArrayList[nChannels];

    private EcalConditions ecalConditions = null;

    
    
    public ECalRunningPedestalDriver() {
        for (int ii = 0; ii < nChannels; ii++) {
            eventPedestals[ii] = new ArrayList<>();
            eventTimestamps[ii] = new ArrayList<>();
            runningPedestals.add(-1.); // would like to initialize with DB pedestals, but they're not available yet
        }
    }
    @Override
    protected void startOfData() {
    }

    @Override
    public void detectorChanged(Detector detector) {
        ecalConditions = ConditionsManager.defaultInstance()
                .getCachedConditions(EcalConditions.class,TableConstants.ECAL_CONDITIONS)
                .getCachedData();
    }
    public void setMinLookbackEvents(int nev) {
        minLookbackEvents = nev;
    }
    public void setMaxLookbackEvents(int nev) {
        maxLookbackEvents = nev;
    }
    public void setMaxLookbackTime(int time) {
        maxLookbackTime = time;
    }


    public double getPedestal(int channel_id) {
        final int nped = eventPedestals[channel_id - 1].size();
        if (nped < minLookbackEvents) return getStaticPedestal(channel_id);
        else                          return runningPedestals.get(channel_id - 1);
    }

    public void printPedestals() {
        for (int ii = 0; ii < nChannels; ii++) {
            System.out.printf("(%d,%.2f,%.2f) ",ii,runningPedestals.get(ii),getStaticPedestal(ii + 1));
        }
        System.out.printf("\n");
    }

    @Override
    protected void process(EventHeader event) {
        if (!event.hasCollection(RawCalorimeterHit.class,rawCollectionName))
            return;
        if (!event.hasCollection(LCRelation.class,extraDataRelationsName))
            return;
        for (LCRelation rel : event.get(LCRelation.class,extraDataRelationsName)) {
            RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
            GenericObject extraData = (GenericObject) rel.getTo();
            updatePedestal(event,hit,extraData);
        }

        // quick fix until I know how to read from DB before 'process':
        List<Double> peds = new ArrayList<Double>(nChannels);
        for (int ii=0; ii<nChannels; ii++){
            peds.add(ii,getPedestal(ii+1));
        }
        
        //
        // don't care right now whether this persists in output slcio,
        // just that it is accessible during reconstruction (and it is)
        //
        // Another option would be to put hits' running pedestals into HitExtraData.Mode7Data
        // Or create another LCRelation
        // Either would also remove the need for indexing later.
        //
        event.put(runningPedestalsName,peds,Double.class,1,"dog");
//        event.put(runningPedestalsName,runningPedestals,Double.class,1,"dog");

//        printPedestals();
    }

    public void updatePedestal(EventHeader event, RawCalorimeterHit hit, GenericObject mode7data) {
        final int ii = getChannelID(hit) - 1;
        if (ii < 0 || ii >= nChannels) {
            System.err.println(String.format("Event #%d, Invalid id: %d/%d ",
                    event.getEventNumber(),ii + 1,+hit.getCellID()));
        }

        final long timestamp = event.getTimeStamp();
        final int min = ((HitExtraData.Mode7Data) mode7data).getAmplLow();
        final int max = ((HitExtraData.Mode7Data) mode7data).getAmplHigh();

        // ignore if pulse at beginning of window:
        if (max <= 0) return;

        // If new timestamp is older than previous one, restart pedestals.
        // This should never happen unless firmware counter cycles back to zero,
        // in which case it could be dealt with if max timestamp is known.
        if (eventTimestamps[ii].size() > 0 && eventTimestamps[ii].get(0) > timestamp) {
            System.err.println(String.format("Event #%d, Old Timestamp:  %d < %d",
                    event.getEventNumber(),timestamp,eventTimestamps[ii].get(0)));
            eventPedestals[ii].clear();
            eventTimestamps[ii].clear();
        }

        // add pedestal to the list:
        eventPedestals[ii].add(min);
        eventTimestamps[ii].add(timestamp);

        if (eventPedestals[ii].size() > 1) {

            // remove oldest pedestal if we surpassed limit on #events:
            if (eventPedestals[ii].size() > limitLookbackEvents
                    || (maxLookbackEvents > 0 && eventPedestals[ii].size() > maxLookbackEvents)) {
                eventPedestals[ii].remove(0);
                eventTimestamps[ii].remove(0);
            }

            // remove old pedestals surpassing limit on lookback time:
            if (maxLookbackTime > 0) {
                while (eventTimestamps[ii].size() > 0) {
                    if (eventTimestamps[ii].get(0) < timestamp - maxLookbackTime*1e6) {
                        eventTimestamps[ii].remove(0);
                        eventPedestals[ii].remove(0);
                    } else {
                        break;
                    }
                }
            }
        }

        // Update running pedestal average:
        if (eventPedestals[ii].size() > 0) {
            double avg = 0;
            for (int jj = 0; jj < eventPedestals[ii].size(); jj++) {
                avg += eventPedestals[ii].get(jj);
            }
            runningPedestals.set(ii,avg / eventPedestals[ii].size());
        } else {
            runningPedestals.set(ii,getStaticPedestal(ii+1));
        }
    }

    public double getPedestal(RawCalorimeterHit hit) {
        return getPedestal(getChannelID(hit));
    }
    private double getStaticPedestal(int channel_id) {
        EcalChannel cc = ecalConditions.getChannelCollection().findChannel(channel_id);
        return ecalConditions.getChannelConstants(cc).getCalibration().getPedestal();
    }
    private double getStaticPedestal(RawCalorimeterHit hit) {
        return getStaticPedestal(getChannelID(hit));
    }
    public int getChannelID(RawCalorimeterHit hit) {
        return findChannel(hit.getCellID()).getCalibration().getChannelId();
    }
    public EcalChannelConstants findChannel(long cellID) {
        return ecalConditions.getChannelConstants(ecalConditions.getChannelCollection()
                .findGeometric(cellID));
    }

}
