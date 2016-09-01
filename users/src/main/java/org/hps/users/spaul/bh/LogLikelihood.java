package org.hps.users.spaul.bh;

import org.apache.commons.math3.fitting.WeightedObservedPoint;

public class LogLikelihood {
    static double getLogLikelihood(Histogram h, double min, double max, IFitResult result){
        GoodnessOfFitAccumulator gofa = new GoodnessOfFitAccumulator(result.paramCount());
        for(int j = 0; j< h.Nbins; j++){
            if(h.minMass[j]>= min && h.maxMass[j]<= max){
                gofa.accumulate(h.h[j], result.get((h.minMass[j]+h.maxMass[j])/2.));
            }
        }
        return gofa.getLogLikeliness();
    }
}
