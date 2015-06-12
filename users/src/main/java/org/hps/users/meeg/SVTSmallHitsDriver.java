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
    private long[] triggerSyncTimes = new long[5];
    private long[] triggerTimes = new long[5];
    private long[] readoutTimes = new long[5];
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

    IHistogram1D trigDt, trigDtWithSmallHits, smallHitFracVsTrigDt, smallHitCount1D;
    IHistogram2D smallHitsVsTrigDt;
    IHistogram1D syncDt, syncDtWithSmallHits, smallHitFracVsSyncDt;
    IHistogram2D smallHitsVsSyncDt;
    IHistogram1D readoutDt, readoutDtWithSmallHits, smallHitFracVsReadoutDt;
    IHistogram2D smallHitsVsReadoutDt;
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

        trigDt2DWithSmallHits = aida.histogram2D("trigger-to-trigger dt vs. trigger-to-trigger dt, events with small hits", 1000, 0, 9.6e4, 1000, 0, 9.6e4);

        trigDt = aida.histogram1D("trigger-to-trigger dt", 4000, 0, 9.6e4);
        trigDtWithSmallHits = aida.histogram1D("trigger-to-trigger dt, events with small hits", 4000, 0, 9.6e4);
        smallHitsVsTrigDt = aida.histogram2D("small hit count vs. trigger-to-trigger dt", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracVsTrigDt = aida.histogram1D("fraction of events with small hits vs. trigger-to-trigger dt", 4000, 0, 9.6e4);

        syncDt = aida.histogram1D("trigger-to-trigger dt, sync phase subtracted", 4000, 0, 9.6e4);
        syncDtWithSmallHits = aida.histogram1D("trigger-to-trigger dt, events with small hits, sync phase subtracted", 4000, 0, 9.6e4);
        smallHitsVsSyncDt = aida.histogram2D("small hit count vs. trigger-to-trigger dt, sync phase subtracted", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracVsSyncDt = aida.histogram1D("fraction of events with small hits vs. trigger-to-trigger dt, sync phase subtracted", 4000, 0, 9.6e4);

        readoutDt = aida.histogram1D("readout-to-trigger dt", 4000, 0, 9.6e4);
        readoutDtWithSmallHits = aida.histogram1D("readout-to-trigger dt, events with small hits", 4000, 0, 9.6e4);
        smallHitsVsReadoutDt = aida.histogram2D("small hit count vs. readout-to-trigger dt", 4000, 0, 9.6e4, 100, 0, 100);
        smallHitFracVsReadoutDt = aida.histogram1D("fraction of events with small hits vs. readout-to-trigger dt", 4000, 0, 9.6e4);

        trigDtPhase = aida.histogram2D("sync phase vs. trigger-to-trigger dt", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
        trigDtWithSmallHitsPhase = aida.histogram2D("sync phase vs. trigger-to-trigger dt, events with small hits", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
        trigDtReadoutWithSmallHitsPhase = aida.histogram2D("sync phase vs. readout-to-trigger dt, events with small hits", 4000, 0, 9.6e4, 210, 0, SYNC_INTERVAL);
    }

//    @Override
    protected void process(EventHeader event) {
        for (int i = triggerTimes.length - 1; i > 0; i--) {
            triggerTimes[i] = triggerTimes[i - 1];
        }
        triggerTimes[0] = event.getTimeStamp();

        for (int i = triggerSyncTimes.length - 1; i > 0; i--) {
            triggerSyncTimes[i] = triggerSyncTimes[i - 1];
        }
        triggerSyncTimes[0] = getNextSyncTime(event.getTimeStamp(), syncPhaseOffset);

        for (int i = readoutTimes.length - 1; i > 0; i--) {
            readoutTimes[i] = readoutTimes[i - 1];
        }
        readoutTimes[0] = Math.max(getNextSyncTime(event.getTimeStamp() + daqDelay, syncPhaseOffset + daqDelay), readoutTimes[1] + READOUT_LENGTH);

        int dt = (int) (triggerTimes[0] - triggerTimes[1]);
        int dtReadout = (int) (triggerTimes[0] - readoutTimes[1]);
        long previousTimestampPhased = triggerSyncTimes[1];
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
            syncDt.fill(dtPhased);
            readoutDt.fill(dtReadout);
            smallHitCount1D.fill(smallHitCount);
            smallHitsVsTrigDt.fill(dt, smallHitCount);
            smallHitsVsSyncDt.fill(dtPhased, smallHitCount);
            smallHitsVsReadoutDt.fill(dtReadout, smallHitCount);
            trigDtPhase.fill(dt, triggerTimes[1] % SYNC_INTERVAL);
            if (smallHitCount > 3) {
                trigDtWithSmallHits.fill(dt);
                trigDt2DWithSmallHits.fill(triggerTimes[0] - triggerSyncTimes[1], triggerTimes[0] - triggerSyncTimes[2]);
                syncDtWithSmallHits.fill(dtPhased);
                readoutDtWithSmallHits.fill(dtReadout);
                trigDtWithSmallHitsPhase.fill(dt, triggerTimes[1] % SYNC_INTERVAL);
                trigDtReadoutWithSmallHitsPhase.fill(dtReadout, triggerTimes[1] % SYNC_INTERVAL);
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
                System.out.format("%d %d %d %d ", event.getEventNumber(), event.getTimeStamp(), event.getTimeStamp() - triggerTimes[1], smallHitCount);
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
            smallHitFracVsTrigDt.reset();
            smallHitFracVsSyncDt.reset();
            smallHitFracVsReadoutDt.reset();
            for (int i = 0; i < trigDt.axis().bins(); i++) {
                smallHitFracVsTrigDt.fill(trigDt.axis().binCenter(i), trigDtWithSmallHits.binHeight(i) / trigDt.binHeight(i));
                smallHitFracVsSyncDt.fill(trigDt.axis().binCenter(i), syncDtWithSmallHits.binHeight(i) / syncDt.binHeight(i));
                smallHitFracVsReadoutDt.fill(trigDt.axis().binCenter(i), readoutDtWithSmallHits.binHeight(i) / readoutDt.binHeight(i));
            }
        }
    }
}
