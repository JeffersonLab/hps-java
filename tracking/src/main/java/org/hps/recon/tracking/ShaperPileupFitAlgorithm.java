package org.hps.recon.tracking;

import java.util.Collection;
import org.lcsim.event.RawTrackerHit;

public class ShaperPileupFitAlgorithm implements ShaperFitAlgorithm {

    ShaperLinearFitAlgorithm onePulseFitter = new ShaperLinearFitAlgorithm(1);
    ShaperLinearFitAlgorithm twoPulseFitter = new ShaperLinearFitAlgorithm(2);
    private String fitTimeMinimizer = "Simplex";
    private boolean debug = false;
    private double refitThreshold = .75;
    private int totalFits = 0;
    private int refitAttempts = 0;
    private int refitsAccepted = 0;
    private int doOldDT = 1;
    
    public ShaperPileupFitAlgorithm() {
    }

    public ShaperPileupFitAlgorithm(double threshold,int DT) {
        refitThreshold = threshold;
        doOldDT = DT;
    }

    @Override
    public void setFitTimeMinimizer(String fitTimeMinimizer) {
        this.onePulseFitter.setFitTimeMinimizer(fitTimeMinimizer);
        this.twoPulseFitter.setFitTimeMinimizer(fitTimeMinimizer);
    }

    //===> public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, HPSSVTCalibrationConstants.ChannelConstants constants) {
    public Collection<ShapeFitParameters> fitShape(RawTrackerHit rth, PulseShape shape) {
        Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, shape);
        double singlePulseChiProb = fittedPulses.iterator().next().getChiProb();
        totalFits++;
        if (singlePulseChiProb < refitThreshold) {
            refitAttempts++;
            Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, shape);
            ShapeFitParameters doubleParam = doublePulse.iterator().next();
            double doublePulseChiProb = doubleParam.getChiProb();
            double time1 = doubleParam.getT0();
            double time2 = fittedPulses.iterator().next().getT0();
            if (doublePulseChiProb > singlePulseChiProb) {
                if(((time2-time1)*(time2-time1)>40.0)||(doOldDT==1)){
                    refitsAccepted++;
                    fittedPulses = doublePulse;
                }
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
