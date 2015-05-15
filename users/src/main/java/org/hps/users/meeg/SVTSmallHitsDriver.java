package org.hps.users.meeg;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;

/**
 *
 * @author uemura
 */
public class SVTSmallHitsDriver extends Driver {

    private String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    private long previousTimestamp = 0;
    private boolean printADCValues = false;
    Map<HpsSiSensor, Set<Integer>> hitMap;
    double dtSumWith = 0;
    double dtSumWithout = 0;
    double nHitsWith = 0;
    double nHitsWithout = 0;
    int nWith = 0;
    int nWithout = 0;

    public void setPrintADCValues(boolean printADCValues) {
        this.printADCValues = printADCValues;
    }

    @Override
    protected void process(EventHeader event) {
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
            if (smallHitCount > 5) {
                dtSumWith += event.getTimeStamp() - previousTimestamp;
                nHitsWith += smallHitCount;
                nWith++;
            } else {
                dtSumWithout += event.getTimeStamp() - previousTimestamp;
                nHitsWithout += smallHitCount;
                nWithout++;
            }
            System.out.format("%f ns with small hits (%d events with %f small hits),\t%f ns without small hits (%d events with %f small hits)\n", dtSumWith / nWith, nWith, nHitsWith / nWith, dtSumWithout / nWithout, nWithout, nHitsWithout / nWithout);

            System.out.format("%d %d %d %d ", event.getEventNumber(), event.getTimeStamp(), event.getTimeStamp() - previousTimestamp, smallHitCount);

            previousTimestamp = event.getTimeStamp();
            for (String sensorName : smallHitCounts.keySet()) {
                System.out.format("%s:%d ", sensorName, smallHitCounts.get(sensorName));
            }
            System.out.println();
        }
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

}
