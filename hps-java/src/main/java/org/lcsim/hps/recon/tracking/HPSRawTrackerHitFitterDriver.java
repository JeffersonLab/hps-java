package org.lcsim.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.hps.readout.ecal.ReadoutTimestamp;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.util.Driver;
import org.lcsim.lcio.LCIOConstants;

/**
 *
 * @author mgraham
 */
public class HPSRawTrackerHitFitterDriver extends Driver {

    private boolean debug = false;
    private HPSShaperFitAlgorithm _shaper = new DumbShaperFit();
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String fitCollectionName = "SVTShapeFitParameters";
    private String fittedHitCollectionName = "SVTFittedRawTrackerHits";
    private int genericObjectFlags = 1 << LCIOConstants.GOBIT_FIXED;
    private int relationFlags = 0;
    private boolean correctT0Shift = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setCorrectT0Shift(boolean correctT0Shift) {
        this.correctT0Shift = correctT0Shift;
    }

    public void setUseTimestamps(boolean useTimestamps) {
        this.useTimestamps = useTimestamps;
    }

    /**
     * Report time relative to the nearest expected truth event time.
     *
     * @param useTruthTime
     */
    public void setUseTruthTime(boolean useTruthTime) {
        this.useTruthTime = useTruthTime;
    }

    public void setFitAlgorithm(String fitAlgorithm) {
        if (fitAlgorithm.equals("Analytic")) {
            _shaper = new HPSShaperAnalyticFitAlgorithm();
        } else {
            throw new RuntimeException("Unrecognized fitAlgorithm: " + fitAlgorithm);
        }
    }

    public void setFitCollectionName(String fitCollectionName) {
        this.fitCollectionName = fitCollectionName;
    }

    public void setFittedHitCollectionName(String fittedHitCollectionName) {
        this.fittedHitCollectionName = fittedHitCollectionName;
    }

    public void setRawHitCollectionName(String rawHitCollectionName) {
        this.rawHitCollectionName = rawHitCollectionName;
    }

    @Override
    public void startOfData() {
        if (rawHitCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(RawTrackerHit.class, rawHitCollectionName)) {
            //System.out.println(rawHitCollectionName + " does not exist; skipping event");
            return;
        }

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        if (rawHits == null) {
            throw new RuntimeException("Event is missing SVT hits collection!");
        }
        List<HPSFittedRawTrackerHit> hits = new ArrayList<HPSFittedRawTrackerHit>();
        List<HPSShapeFitParameters> fits = new ArrayList<HPSShapeFitParameters>();

        //  Make a fitted hit from this cluster
        for (RawTrackerHit hit : rawHits) {
            int strip = hit.getIdentifierFieldValue("strip");
            ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) hit.getDetectorElement(), strip);
            HPSShapeFitParameters fit = _shaper.fitShape(hit, constants);
            if (correctT0Shift) {
                fit.setT0(fit.getT0() - constants.getT0Shift());
            }
            if (useTimestamps) {
                double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
                double t0Trig = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRIGGER, event);
                double corMod = (t0Svt - t0Trig);
                fit.setT0(fit.getT0() + corMod);
            }
            if (useTruthTime) {
                double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
                double absoluteHitTime = fit.getT0() + t0Svt;
                double relativeHitTime = ((absoluteHitTime + 250.0) % 500.0) - 250.0;

                fit.setT0(relativeHitTime);
            }
            if (debug) {
                System.out.println(fit);
            }
            fits.add(fit);
            HPSFittedRawTrackerHit hth = new HPSFittedRawTrackerHit(hit, fit);
            hits.add(hth);
            if (strip == HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR) { //drop unbonded channel
                continue;
            }
            hit.getDetectorElement().getReadout().addHit(hth);
        }
        event.put(fitCollectionName, fits, HPSShapeFitParameters.class, genericObjectFlags);
        event.put(fittedHitCollectionName, hits, HPSFittedRawTrackerHit.class, relationFlags);
    }
}
