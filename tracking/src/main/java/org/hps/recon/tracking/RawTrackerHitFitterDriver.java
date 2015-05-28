package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.List;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.svt.SvtTimingConstants;
import org.hps.readout.ecal.ReadoutTimestamp;
import org.hps.readout.svt.HPSSVTConstants;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
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
    private ShaperFitAlgorithm fitter = new DumbShaperFit();
    private PulseShape shape = new PulseShape.FourPole();
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String fitCollectionName = "SVTShapeFitParameters";
    private String fittedHitCollectionName = "SVTFittedRawTrackerHits";
    private SvtTimingConstants timingConstants;
    private int genericObjectFlags = 1 << LCIOConstants.GOBIT_FIXED;
    private int relationFlags = 0;
    private boolean correctTimeOffset = false;
    private boolean correctT0Shift = false;
    private boolean useTimestamps = false;
    private boolean useTruthTime = false;
    private boolean subtractTOF = false;
    private boolean subtractTriggerTime = false;
    private boolean correctChanT0 = true;

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

    public void setCorrectTimeOffset(boolean correctTimeOffset) {
        this.correctTimeOffset = correctTimeOffset;
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

    public void setSubtractTriggerTime(boolean subtractTriggerTime) {
        this.subtractTriggerTime = subtractTriggerTime;
    }

    public void setCorrectChanT0(boolean correctChanT0) {
        this.correctChanT0 = correctChanT0;
    }

    public void setFitAlgorithm(String fitAlgorithm) {
        if (fitAlgorithm.equals("Analytic")) {
            fitter = new ShaperAnalyticFitAlgorithm();
        } else if (fitAlgorithm.equals("Linear")) {
            fitter = new ShaperLinearFitAlgorithm(1);
        } else if (fitAlgorithm.equals("PileupAlways")) {
            fitter = new ShaperPileupFitAlgorithm(1.0);
        } else if (fitAlgorithm.equals("Pileup")) {
            fitter = new ShaperPileupFitAlgorithm();
        } else {
            throw new RuntimeException("Unrecognized fitAlgorithm: " + fitAlgorithm);
        }
    }

    public void setPulseShape(String pulseShape) {
        if (pulseShape.equals("CR-RC")) {
            shape = new PulseShape.CRRC();
        } else if (pulseShape.equals("FourPole")) {
            shape = new PulseShape.FourPole();
        } else {
            throw new RuntimeException("Unrecognized pulseShape: " + pulseShape);
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
        fitter.setDebug(debug);
        if (rawHitCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }
    }

    protected void detectorChanged(Detector detector) {
        timingConstants = DatabaseConditionsManager.getInstance().getCachedConditions(SvtTimingConstants.SvtTimingConstantsCollection.class, "svt_timing_constants").getCachedData().get(0);
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
            HpsSiSensor sensor = (HpsSiSensor) hit.getDetectorElement();
            //===> ChannelConstants constants = HPSSVTCalibrationConstants.getChannelConstants((SiSensor) hit.getDetectorElement(), strip);
            //for (ShapeFitParameters fit : _shaper.fitShape(hit, constants)) {
            for (ShapeFitParameters fit : fitter.fitShape(hit, shape)) {
                if (correctTimeOffset) {
                    fit.setT0(fit.getT0() - timingConstants.getOffsetTime());
                }
                if (subtractTriggerTime) {
                    fit.setT0(fit.getT0() - (((event.getTimeStamp() - 4 * timingConstants.getOffsetPhase()) % 24) - 12));
                }
                if (correctChanT0) {
                    fit.setT0(fit.getT0() - sensor.getShapeFitParameters(strip)[HpsSiSensor.T0_INDEX]);
                }
                if (correctT0Shift) {
                    //===> fit.setT0(fit.getT0() - constants.getT0Shift());
                    fit.setT0(fit.getT0() - sensor.getT0Shift());
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
