package org.hps.recon.tracking;

import java.util.Collection;
//===> import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 */
public class ShaperPileupFitAlgorithm implements ShaperFitAlgorithm {

    ShaperLinearFitAlgorithm onePulseFitter = new ShaperLinearFitAlgorithm(1);
    ShaperLinearFitAlgorithm twoPulseFitter = new ShaperLinearFitAlgorithm(2);
    private boolean debug = false;
    private double refitThreshold = 0.5;
    private int totalFits = 0;
    private int refitAttempts = 0;
    private int refitsAccepted = 0;

    public ShaperPileupFitAlgorithm() {
    }

    public ShaperPileupFitAlgorithm(double threshold) {
        refitThreshold = threshold;
    }

    //===> public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, HPSSVTCalibrationConstants.ChannelConstants constants) {
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, PulseShape shape) {
        //===> Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, constants);
        Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, shape);
        double singlePulseChiProb = fittedPulses.iterator().next().getChiProb();
        totalFits++;
        if (singlePulseChiProb < refitThreshold) {
            refitAttempts++;
            //===> Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, constants);
            Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, shape);
            double doublePulseChiProb = doublePulse.iterator().next().getChiProb();
            if (doublePulseChiProb > singlePulseChiProb) {
                refitsAccepted++;
                fittedPulses = doublePulse;
            }
        }
        if (debug && totalFits % 10000 == 0) {
            System.out.format("%d fits, %d refit attempts, %d refits accepted\n", totalFits, refitAttempts, refitsAccepted);
        }
        return fittedPulses;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        onePulseFitter.setDebug(debug);
        twoPulseFitter.setDebug(debug);
    }

}
