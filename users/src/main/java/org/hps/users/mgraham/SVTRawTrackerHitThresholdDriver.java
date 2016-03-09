package org.hps.users.mgraham;

import java.util.ArrayList;
import java.util.List;


import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * 
 * @author Matt Graham
 */
// TODO: Check that this Driver works as expected after it was updated to use 
//       the database conditions system.
public class SVTRawTrackerHitThresholdDriver extends Driver {

    private String rawTrackerHitCollectionName = "RawTrackerHitMaker_RawTrackerHits";
    private String outputHitCollectionName = "RawTrackerHitsThreshold";
    private String calibFileName = "foobar";
    private String trackerName = "Tracker";
    private Detector detector;
    private double noiseThreshold = 3;
    private int nhitsAboveNoise = 2;

    public SVTRawTrackerHitThresholdDriver() {
    }

    public void setRawTrackerHitCollectionName(String rawTrackerHitCollectionName) {
        this.rawTrackerHitCollectionName = rawTrackerHitCollectionName;
    }

    public void setOutputHitCollectionName(String outputHitCollectionName) {
        this.outputHitCollectionName = outputHitCollectionName;
    }

    public void setCalibFileName(String calibFileName) {
        this.calibFileName = calibFileName;
    }

    public void setNoiseThreshold(double thres) {
        this.noiseThreshold = thres;
    }

    public void setNhitsAboveNoise(int nhits) {
        this.nhitsAboveNoise = nhits;
    }

    protected void detectorChanged(Detector detector) {
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, rawTrackerHitCollectionName)) {
            // Get RawTrackerHit collection from event.
            List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
            List<RawTrackerHit> outputHits = new ArrayList<RawTrackerHit>();

            // Increment strip hit count.
            for (RawTrackerHit hit : rawTrackerHits) {
                HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
                int strip = hit.getIdentifierFieldValue("strip");
                short[] adcVal = hit.getADCValues();
                int nAbove = 0;
                for (int i = 0; i < 6; i++) {
                    double pedSubNorm = (adcVal[i] - sensor.getPedestal(strip, i)) / sensor.getNoise(strip, i);
                    if (pedSubNorm > noiseThreshold)
                        nAbove++;
                }
                if (nAbove >= nhitsAboveNoise)
                    outputHits.add(hit);
            }

            event.put(outputHitCollectionName, outputHits, RawTrackerHit.class, 0);
        }
    }
}