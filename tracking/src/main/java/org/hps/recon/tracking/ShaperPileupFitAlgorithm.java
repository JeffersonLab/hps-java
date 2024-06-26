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
        //System.out.print("Statement in Constructor");
        //System.out.print(threshold);
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
        //System.out.print(refitThreshold);
        //===> Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, constants);
        Collection<ShapeFitParameters> fittedPulses = onePulseFitter.fitShape(rth, shape);
        double singlePulseChiProb = fittedPulses.iterator().next().getChiProb();
        //double time1 = fittedPulses.iterator().next().getT0();
        totalFits++;
        if (singlePulseChiProb < refitThreshold) {
            refitAttempts++;
            //===> Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, constants);
            Collection<ShapeFitParameters> doublePulse = twoPulseFitter.fitShape(rth, shape);
            ShapeFitParameters Hello = doublePulse.iterator().next();
            double doublePulseChiProb = Hello.getChiProb();
            double time1 = Hello.getT0();
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
