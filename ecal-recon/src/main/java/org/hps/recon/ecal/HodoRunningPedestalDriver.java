/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.hodoscope.HodoscopeChannel;
import org.hps.conditions.hodoscope.HodoscopeConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;



/**
 *
 * @author rafopar
 */
public class HodoRunningPedestalDriver extends Driver {

    private static final Logger LOGGER = Logger.getLogger(HodoRunningPedestalDriver.class.getPackage().getName());

    // limit array lengths:
    private final int limitLookbackEvents = 1000;

    // minimum number of readouts for running averages:
    // (if not satisfied, use pedestals from database)
    private int minLookbackEvents = 5;

    // maximum number of readouts for running averages:
    // (if too many, discard the oldest ones)
    private int maxLookbackEvents = 40;

    // oldest allowed time for running averages:
    // (discard older readouts ; negative = no time limit)
    private long maxLookbackTime = -1; // units = ms

    private static final String rawCollectionName = "HodoReadoutHits";
    private static final String extraDataRelationsName = "HodoReadoutExtraDataRelations";
    private static final String runningPedestalsName = "HodoRunningPedestals";

    // number of samples from the beginning of the time window used to calculate the pedestal:
    private static final int nSamples = 4;

    // TODO: Get this from somewhere else.
    private final int nChannels = 32;

    // running pedestal averages, one for each channel:
    private Map<HodoscopeChannel, Double> runningPedestals = new HashMap<HodoscopeChannel, Double>(nChannels);

    // recent event-by-event pedestals and timestamps:
    private Map<HodoscopeChannel, List<Double>> eventPedestals = new HashMap<HodoscopeChannel, List<Double>>();
    private Map<HodoscopeChannel, List<Long>> eventTimestamps = new HashMap<HodoscopeChannel, List<Long>>();

    private boolean debug = false;
    private HodoscopeConditions hodoConditions = null;

    public HodoRunningPedestalDriver() {
    }

    
    
    // ===== TEST ===== will remove later following line
    Detector mydet;
    
    @Override
    protected void startOfData() {
        LOGGER.config("minLookbackEvents: " + minLookbackEvents);
        LOGGER.config("maxLookbackEvents: " + maxLookbackEvents);
        LOGGER.config("maxLookbackTime:" + maxLookbackTime);
    }

    @Override
    public void detectorChanged(Detector detector) {
        
        mydet = detector;
        hodoConditions = DatabaseConditionsManager.getInstance().getHodoConditions();
        for (int ii = 0; ii < nChannels; ii++) {
            HodoscopeChannel chan = findChannel(ii + 1);
            runningPedestals.put(chan, getStaticPedestal(chan));
            eventPedestals.put(chan, new ArrayList<Double>());
            eventTimestamps.put(chan, new ArrayList<Long>());
        }
        if (debug) {
            System.out.println("Running and static pedestals better match here:");
            printPedestals();
        }
    }

    public void setMinLookbackEvents(int nev) {
        if (nev < 1) {
            System.err.println("HodoRunningPedestalDriver:  Ignoring minLookbackEvents too small:  " + nev);
            nev = 1;
        }
        minLookbackEvents = nev;
    }

    public void setMaxLookbackEvents(int nev) {
        if (nev > limitLookbackEvents) {
            System.err.println("HodoRunningPedestalDriver:  Ignoring maxLookbackEvents too big:  " + nev);
            nev = limitLookbackEvents;
        }
        maxLookbackEvents = nev;
    }

    private void setMaxLookbackTime(int time) {
        maxLookbackTime = time;
    }

    private double getNSampleMinimum(short samples[]) {
        double min = 99999999;
        for (int ii = 0; ii < samples.length - nSamples; ii++) {
            double tmp = 0;
            for (int jj = ii; jj < ii + nSamples; jj++)
                tmp += samples[jj];
            tmp /= nSamples;
            if (tmp < min)
                min = tmp;
        }
        return min;
    }
    
    
    @Override
    protected void process(EventHeader event) {

        // Mode-7 Input Data:
        if (event.hasCollection(RawCalorimeterHit.class, rawCollectionName)) {
            if (event.hasCollection(LCRelation.class, extraDataRelationsName)) {
                for (LCRelation rel : event.get(LCRelation.class, extraDataRelationsName)) {
                    RawCalorimeterHit hit = (RawCalorimeterHit) rel.getFrom();
                    GenericObject extraData = (GenericObject) rel.getTo();
                    updatePedestal(event, hit, extraData);
                }
            }
        }
        
        
         // Mode-1 Input Data:
        else if (event.hasCollection(RawTrackerHit.class, rawCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, rawCollectionName);
            for (RawTrackerHit hit : hits) {
                short samples[] = hit.getADCValues();
                if (nSamples > samples.length) {
                    throw new IllegalStateException("Not enough samples for Hodo running pedestal.");
                }

                
//                System.out.println(Arrays.toString(samples));
//                System.out.println( "Conditions " + hodoConditions.getChannelConstants(findChannel(hit)).getCalibration().toString());
                
                boolean good = true;
                double ped = 0;
                for (int ii = 0; ii < nSamples; ii++) {
                    // reject pulses from pedestal calculation:

                    if (samples[ii] > getStaticPedestal(findChannel(hit)) + 12) {
                        good = false;
                        break;
                    }
                    ped += samples[ii];
                }
                                
                if (good) {
                    ped /= nSamples;
                    updatePedestal(event, findChannel(hit), ped);
                }
            }
        }

        event.put(runningPedestalsName, runningPedestals);

        if (debug) {
            printPedestals();
        }
       

    }

    public void printPedestals() {
        for (int ii = 1; ii <= nChannels; ii++) {
            HodoscopeChannel chan = findChannel(ii);
            if (!runningPedestals.containsKey(chan)) {
                System.err.println("printPedestals:   Missing Channel:  " + ii);
                continue;
            }
            System.out.printf("(%d,%.2f,%.2f) ", chan.getChannelId(), runningPedestals.get(chan),
                    getStaticPedestal(chan));
        }
        System.out.printf("\n");
    }

    private void updatePedestal(EventHeader event, RawCalorimeterHit hit, GenericObject mode7data) {

        final int min = ((HitExtraData.Mode7Data) mode7data).getAmplLow();
        final int max = ((HitExtraData.Mode7Data) mode7data).getAmplHigh();

        // ignore if pulse at beginning of window:
        if (max <= 0) {
            return;
        }

        HodoscopeChannel chan = findChannel(hit);
        if (chan == null) {
            System.err.println("hit doesn't correspond to hodochannel");
            return;
        }

        updatePedestal(event, chan, (double) min);
    }

    private void updatePedestal(EventHeader event, HodoscopeChannel chan, double min) {

        final long timestamp = event.getTimeStamp();

        List<Double> peds = eventPedestals.get(chan);
        List<Long> times = eventTimestamps.get(chan);

        if (maxLookbackTime > 0) {
            // If new timestamp is older than previous one, restart pedestals.
            // This should never happen unless firmware counter cycles back to zero,
            // in which case it could be dealt with if max timestamp is known.
            if (times.size() > 0 && times.get(0) > timestamp) {
                System.err.println(String.format("Event #%d, Old Timestamp:  %d < %d", event.getEventNumber(),
                        timestamp, times.get(0)));
                peds.clear();
                times.clear();
            }
        }

        // add pedestal to the list:
        peds.add(min);
        times.add(timestamp);

        if (peds.size() > 1) {

            // remove oldest pedestal if surpassed limit on #events:
            if (peds.size() > limitLookbackEvents || (maxLookbackEvents > 0 && peds.size() > maxLookbackEvents)) {
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

    public double getStaticPedestal(HodoscopeChannel chan) {                
        return hodoConditions.getChannelConstants(chan).getCalibration().getPedestal();
    }

    public HodoscopeChannel findChannel(int channel_id) {
        return hodoConditions.getChannels().findChannel(channel_id);
    }

    public HodoscopeChannel findChannel(RawTrackerHit hit) {        
        return hodoConditions.getChannels().findGeometric(hit.getCellID());
    }

    public HodoscopeChannel findChannel(RawCalorimeterHit hit) {
        return hodoConditions.getChannels().findGeometric(hit.getCellID());
    }

}
