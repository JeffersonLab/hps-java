package org.hps.users.meeg;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotterFactory;
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
    private long previousTimestamp = 0;
    private boolean printADCValues = false;
    private boolean printEvents = false;
    Map<HpsSiSensor, Set<Integer>> hitMap;
    double dtSumWith = 0;
    double dtSumWithout = 0;
    double nHitsWith = 0;
    double nHitsWithout = 0;
    int nWith = 0;
    int nWithout = 0;

    IHistogram1D trigDt, trigDtWithSmallHits, smallHitFracDt, smallHitCount1D;
    IHistogram2D smallHitsDt2D;

    public void setPrintADCValues(boolean printADCValues) {
        this.printADCValues = printADCValues;
    }

    public void setPrintEvents(boolean printEvents) {
        this.printEvents = printEvents;
    }

    protected void detectorChanged(Detector detector) {
        aida.tree().cd("/");

        trigDt = aida.histogram1D("trigger dt", 1000, 0, 1e5);
        smallHitCount1D = aida.histogram1D("small hit count", 100, 0, 100);
        trigDtWithSmallHits = aida.histogram1D("trigger dt, events with small hits", 1000, 0, 1e5);
        smallHitsDt2D = aida.histogram2D("small hit count vs. trigger dt", 1000, 0, 1e5, 100, 0, 100);
        smallHitFracDt = aida.histogram1D("fraction of events with small hits vs. dt", 1000, 0, 1e5);
    }

    @Override
    protected void process(EventHeader event) {
        int dt = (int) (event.getTimeStamp() - previousTimestamp);

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
            smallHitCount1D.fill(smallHitCount);
            smallHitsDt2D.fill(dt, smallHitCount);
            if (smallHitCount > 3) {
                trigDtWithSmallHits.fill(dt);
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
                System.out.format("%d %d %d %d ", event.getEventNumber(), event.getTimeStamp(), event.getTimeStamp() - previousTimestamp, smallHitCount);
            }

            previousTimestamp = event.getTimeStamp();
            if (printEvents) {
                for (String sensorName : smallHitCounts.keySet()) {
                    System.out.format("%s:%d ", sensorName, smallHitCounts.get(sensorName));
                }
                System.out.println();
            }
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

    public void endOfData() {
        if (trigDt != null) {
            smallHitFracDt.reset();
            for (int i = 0; i < trigDt.axis().bins(); i++) {
                smallHitFracDt.fill(trigDt.axis().binCenter(i), trigDtWithSmallHits.binHeight(i) / trigDt.binHeight(i));
            }
        }
    }
}
