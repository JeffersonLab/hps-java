package org.hps.recon.tracking;

import java.util.Collection;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.lcsim.event.RawTrackerHit;

/**
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class ShaperPileupFitAlgorithm implements ShaperFitAlgorithm {

    ShaperLinearFitAlgorithm onePulseFitter = new ShaperLinearFitAlgorithm(1);
    ShaperLinearFitAlgorithm twoPulseFitter = new ShaperLinearFitAlgorithm(2);
    private boolean debug = false;
    private double refitThreshold = 0.1;

    public ShaperPileupFitAlgorithm() {
    }

    public ShaperPileupFitAlgorithm(double threshold) {
        refitThreshold = threshold;
    }

    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, HPSSVTCalibrationConstants.ChannelConstants constants) {
        Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, constants);
        double singlePulseChiProb = fittedPulses.iterator().next().getChiProb();
        if (singlePulseChiProb < refitThreshold) {
            Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, constants);
            double doublePulseChiProb = doublePulse.iterator().next().getChiProb();
            if (doublePulseChiProb > singlePulseChiProb) {
                fittedPulses = doublePulse;
            }
        }
        return fittedPulses;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

}
