package org.hps.users.spaul;

import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.ref.histogram.Histogram1D;

public class AidaHistogramUtil {
    public static IHistogram1D shift(IHistogram1D h, final int nbins){
        return new Histogram1D(){
            public double binHeight(int i){
                return super.binHeight(i-nbins);
            }
            public double binError(int i){
                return super.binError(i-nbins);
            }
        };
    }
    public static IHistogram1D shift(IHistogramFactory hf, IHistogram1D h, final int nbins){
        IHistogram1D copy = hf.createCopy("copy", h);
        copy.reset();
        if(nbins > 0)
            for(int i = 0; i< copy.axis().bins()-nbins; i++){
                copy.fill(h.binMean(i), h.binHeight(i+nbins));
            }
        else 
            for(int i = -nbins; i< copy.axis().bins(); i++){
                copy.fill(h.binMean(i), h.binHeight(i+nbins));
            }
        return copy;
    }
}
