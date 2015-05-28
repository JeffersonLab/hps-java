package org.hps.users.meeg;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author uemura
 */
public class SVTSmallHitsDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private static final int SYNC_INTERVAL = 35 * 24;
    private static final int READOUT_LENGTH = 6 * 140 * 24;
    private long[] previousTriggerTimesPhased = new long[5];
    private long[] previousTriggerTimes = new long[5];
    private long[] previousReadoutTimes = new long[5];
    private boolean printADCValues = false;
    private boolean printEvents = false;
    Map<HpsSiSensor, Set<Integer>> hitMap;
    double dtSumWith = 0;
    double dtSumWithout = 0;
    double nHitsWith = 0;
    double nHitsWithout = 0;
    int nWith = 0;
    int nWithout = 0;
    private int syncPhaseOffset = 12;
    private int daqDelay = 6200;

    IHistogram1D trigDt, trigDtWithSmallHits, smallHitFracDt, smallHitCount1D;
    IHistogram2D smallHitsDt2D;
    IHistogram1D trigDtPhased, trigDtWithSmallHitsPhased, smallHitFracDtPhased;
    IHistogram2D smallHitsDt2DPhased;
    IHistogram1D trigDtReadout, trigDtWithSmallHitsReadout, smallHitFracDtReadout;
    IHistogram2D smallHitsDt2DReadout;
    IHistogram2D trigDt2DWithSmallHits;

    IHistogram2D trigDtPhase, trigDtWithSmallHitsPhase, trigDtReadoutWithSmallHitsPhase;

    public void setPrintADCValues(boolean printADCValues) {
        this.printADCValues = printADCValues;
    }

    public void setPrintEvents(boolean printEvents) {
        this.printEvents = printEvents;
    }

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        smallHitCount1D = aida.histogram1D("small hit count", 100, 0, 100);

        trigDt2DWithSmallHits = aida.histogram2D("trigger dt vs. previous trigger dt, events with small hits", 1000, 0, 9.6e4, 1000, 0, 9.6e4);

        trigDt = aida.histogram1D("trigger dt", 4000, 0, 9.6e4);
        trigDtWithSmallHits = aida.histogram1D("trigger dt, events with small hits", 4000, 0, 9.6e4);
        smallHitsDt2D = aida.histogram2D("small hit count vs. trigger dt", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracDt = aida.histogram1D("fraction of events with small hits vs. dt", 4000, 0, 9.6e4);

        trigDtPhased = aida.histogram1D("trigger dt, sync phase subtracted", 4000, 0, 9.6e4);
        trigDtWithSmallHitsPhased = aida.histogram1D("trigger dt, events with small hits, sync phase subtracted", 4000, 0, 9.6e4);
        smallHitsDt2DPhased = aida.histogram2D("small hit count vs. trigger dt, sync phase subtracted", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracDtPhased = aida.histogram1D("fraction of events with small hits vs. dt, sync phase subtracted", 4000, 0, 9.6e4);

        trigDtReadout = aida.histogram1D("trigger dt, using previous readout time", 4000, 0, 9.6e4);
        trigDtWithSmallHitsReadout = aida.histogram1D("trigger dt, events with small hits, using previous readout time", 4000, 0, 9.6e4);
        smallHitsDt2DReadout = aida.histogram2D("small hit count vs. trigger dt, using previous readout time", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracDtReadout = aida.histogram1D("fraction of events with small hits vs. dt, using previous readout time", 4000, 0, 9.6e4);

        trigDtPhase = aida.histogram2D("sync phase vs. trigger dt", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
        trigDtWithSmallHitsPhase = aida.histogram2D("sync phase vs. trigger dt, events with small hits", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
        trigDtReadoutWithSmallHitsPhase = aida.histogram2D("sync phase vs. trigger dt, using previous readout time, events with small hits", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
    }

//    @Override
    protected void process(EventHeader event) {
        for (int i = previousTriggerTimes.length - 1; i > 0; i--) {
            previousTriggerTimes[i] = previousTriggerTimes[i - 1];
        }
        previousTriggerTimes[0] = event.getTimeStamp();

        for (int i = previousTriggerTimesPhased.length - 1; i > 0; i--) {
            previousTriggerTimesPhased[i] = previousTriggerTimesPhased[i - 1];
        }
        previousTriggerTimesPhased[0] = getNextSyncTime(event.getTimeStamp(), syncPhaseOffset);

        for (int i = previousReadoutTimes.length - 1; i > 0; i--) {
            previousReadoutTimes[i] = previousReadoutTimes[i - 1];
        }
        previousReadoutTimes[0] = Math.max(getNextSyncTime(event.getTimeStamp() + daqDelay, syncPhaseOffset + daqDelay), previousReadoutTimes[1] + READOUT_LENGTH);

        int dt = (int) (previousTriggerTimes[0] - previousTriggerTimes[1]);
        int dtReadout = (int) (previousTriggerTimes[0] - previousReadoutTimes[1]);
        long previousTimestampPhased = previousTriggerTimesPhased[1];
        int dtPhased = (int) (event.getTimeStamp() - previousTimestampPhased);

        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            Map<String, Integer> smallHitCounts = new HashMap<String, Integer>();
            int smallHitCount = 0;
            hitMap = new HashMap<HpsSiSensor, Set<Integer>>();

            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                Set<Integer> hitStrips = hitMap.get(sensor);
                if (hitStrips == null) {
                    hitStrips = new HashSet<Integer>();
                    hitMap.put(sensor, hitStrips);
                }
                int strip = hit.getIdentifierFieldValue("strip");
                hitStrips.add(strip);
            }

            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                int strip = hit.getIdentifierFieldValue("strip");
                double pedestal = sensor.getPedestal(strip, 0);

                if (isSmallHit(hit)) {
                    if (printADCValues) {
                        System.out.format("%s %d %f %f %f %f %f %f\n", sensor.getName(), strip, hit.getADCValues()[0] - pedestal, hit.getADCValues()[1] - pedestal, hit.getADCValues()[2] - pedestal, hit.getADCValues()[3] - pedestal, hit.getADCValues()[4] - pedestal, hit.getADCValues()[5] - pedestal);
                    }
                    smallHitCount++;
                    Integer count = smallHitCounts.get(sensor.getName());
                    if (count == null) {
                        count = 0;
                    }
                    smallHitCounts.put(sensor.getName(), count + 1);
                }
            }
            trigDt.fill(dt);
            trigDtPhased.fill(dtPhased);
            trigDtReadout.fill(dtReadout);
            smallHitCount1D.fill(smallHitCount);
            smallHitsDt2D.fill(dt, smallHitCount);
            smallHitsDt2DPhased.fill(dtPhased, smallHitCount);
            smallHitsDt2DReadout.fill(dtReadout, smallHitCount);
            trigDtPhase.fill(dt, previousTriggerTimes[1] % SYNC_INTERVAL);
            if (smallHitCount > 3) {
                trigDtWithSmallHits.fill(dt);
                trigDt2DWithSmallHits.fill(previousTriggerTimes[0] - previousTriggerTimesPhased[1], previousTriggerTimes[0] - previousTriggerTimesPhased[2]);
                trigDtWithSmallHitsPhased.fill(dtPhased);
                trigDtWithSmallHitsReadout.fill(dtReadout);
                trigDtWithSmallHitsPhase.fill(dt, previousTriggerTimes[1] % SYNC_INTERVAL);
                trigDtReadoutWithSmallHitsPhase.fill(dtReadout, previousTriggerTimes[1] % SYNC_INTERVAL);
                dtSumWith += dt;
                nHitsWith += smallHitCount;
                nWith++;
            } else {
                dtSumWithout += dt;
                nHitsWithout += smallHitCount;
                nWithout++;
            }
            if (printEvents) {
                System.out.format("%f ns with small hits (%d events with %f small hits),\t%f ns without small hits (%d events with %f small hits)\n", dtSumWith / nWith, nWith, nHitsWith / nWith, dtSumWithout / nWithout, nWithout, nHitsWithout / nWithout);
                System.out.format("%d %d %d %d ", event.getEventNumber(), event.getTimeStamp(), event.getTimeStamp() - previousTriggerTimes[1], smallHitCount);
            }

            if (printEvents) {
                for (String sensorName : smallHitCounts.keySet()) {
                    System.out.format("%s:%d ", sensorName, smallHitCounts.get(sensorName));
                }
                System.out.println();
            }
        }
    }

    private static long getNextSyncTime(long triggerTime, int phaseOffset) {
        return triggerTime + SYNC_INTERVAL - ((triggerTime - phaseOffset) % SYNC_INTERVAL);
    }

    private boolean isSmallHit(RawTrackerHit hit) {
        HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
        int strip = hit.getIdentifierFieldValue("strip");
        double pedestal = sensor.getPedestal(strip, 0);
        double noise = sensor.getNoise(strip, 0);

        if (hitMap.get(sensor) != null && (hitMap.get(sensor).contains(strip - 1) || hitMap.get(sensor).contains(strip + 1))) {
            return false;
        }
        for (int i = 0; i < 6; i++) {
            if (hit.getADCValues()[i] > pedestal + 4.0 * noise) {
                return false;
            }
        }
        return true;
    }

    public void endOfData() {
        if (trigDt != null) {
            smallHitFracDt.reset();
            smallHitFracDtPhased.reset();
            smallHitFracDtReadout.reset();
            for (int i = 0; i < trigDt.axis().bins(); i++) {
                smallHitFracDt.fill(trigDt.axis().binCenter(i), trigDtWithSmallHits.binHeight(i) / trigDt.binHeight(i));
                smallHitFracDtPhased.fill(trigDt.axis().binCenter(i), trigDtWithSmallHitsPhased.binHeight(i) / trigDtPhased.binHeight(i));
                smallHitFracDtReadout.fill(trigDt.axis().binCenter(i), trigDtWithSmallHitsReadout.binHeight(i) / trigDtReadout.binHeight(i));
            }
        }
    }
}
