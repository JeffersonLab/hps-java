package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.hps.conditions.deprecated.HPSSVTConstants;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.recon.cat.util.Const;
import org.lcsim.util.Driver;

/**
 *
 * @author Matt Graham
 */
// TODO: Add class documentation.
public class RawTrackerHitFitterDriver extends Driver {

    private boolean debug = false;
    private ShaperFitAlgorithm _shaper = new DumbShaperFit();
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String fitCollectionName = "SVTShapeFitParameters";
    private String fittedHitCollectionName = "SVTFittedRawTrackerHits";
    private int genericObjectFlags = 1 << LCIOConstants.GOBIT_FIXED;
    private int relationFlags = 0;
    private boolean correctT0Shift = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;
    private boolean subtractTOF = false;

    /**
     * Report time relative to the nearest expected truth event time.
     *
     * @param useTruthTime
     */
    public void setUseTruthTime(boolean useTruthTime) {
        this.useTruthTime = useTruthTime;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setCorrectT0Shift(boolean correctT0Shift) {
        this.correctT0Shift = correctT0Shift;
    }

    public void setUseTimestamps(boolean useTimestamps) {
        this.useTimestamps = useTimestamps;
    }

    public void setSubtractTOF(boolean subtractTOF) {
        this.subtractTOF = subtractTOF;
    }

    public void setFitAlgorithm(String fitAlgorithm) {
        if (fitAlgorithm.equals("Analytic")) {
            _shaper = new ShaperAnalyticFitAlgorithm();
        } else if (fitAlgorithm.equals("Linear")) {
            _shaper = new ShaperLinearFitAlgorithm(1);
        } else if (fitAlgorithm.equals("Pileup")) {
            _shaper = new ShaperLinearFitAlgorithm(2);
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
        _shaper.setDebug(debug);
        if (rawHitCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    @Override
    public void process(EventHeader event) {
        if (!event.hasCollection(RawTrackerHit.class, rawHitCollectionName)) {
            // System.out.println(rawHitCollectionName + " does not exist; skipping event");
            return;
        }

        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        if (rawHits == null) {
            throw new RuntimeException("Event is missing SVT hits collection!");
        }
        List<FittedRawTrackerHit> hits = new ArrayList<FittedRawTrackerHit>();
        List<ShapeFitParameters> fits = new ArrayList<ShapeFitParameters>();

        // Make a fitted hit from this cluster
        for (RawTrackerHit hit : rawHits) {
            int strip = hit.getIdentifierFieldValue("strip");
            ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) hit.getDetectorElement(), strip);
            for (ShapeFitParameters fit : _shaper.fitShape(hit, constants)) {
                if (correctT0Shift) {
                    fit.setT0(fit.getT0() - constants.getT0Shift());
                }
                if (subtractTOF) {
                    double tof = hit.getDetectorElement().getGeometry().getPosition().magnitude() / (Const.SPEED_OF_LIGHT * Const.nanosecond);
                    fit.setT0(fit.getT0() - tof);
                }
                if (useTimestamps) {
                    double t0Svt = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRACKER, event);
                    double t0Trig = ReadoutTimestamp.getTimestamp(ReadoutTimestamp.SYSTEM_TRIGGERBITS, event);
                    double corMod = (t0Svt - t0Trig) + 200.0;
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
                FittedRawTrackerHit hth = new FittedRawTrackerHit(hit, fit);
                hits.add(hth);
                if (strip == HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR) { // drop unbonded channel
                    continue;
                }
                hit.getDetectorElement().getReadout().addHit(hth);
            }
        }
        event.put(fitCollectionName, fits, ShapeFitParameters.class, genericObjectFlags);
        event.put(fittedHitCollectionName, hits, FittedRawTrackerHit.class, relationFlags);
    }
}
