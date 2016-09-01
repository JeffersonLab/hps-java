package org.hps.users.spaul.bh;

import java.util.ArrayList;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;

public class Fitters {
    static Polynomial polyfit(Histogram h, double min, double max, int degree){
        PolynomialCurveFitter pcf = PolynomialCurveFitter.create(degree);
        
        ArrayList<WeightedObservedPoint> wops = new ArrayList(); 
        for(int j = 0; j< h.Nbins; j++){
            if(h.minMass[j]>= min && h.maxMass[j]<= max){
                double w = 1/h.h[j];
                WeightedObservedPoint wop = new WeightedObservedPoint(w, (h.minMass[j]+h.maxMass[j])/2., h.h[j]);
                wops.add(wop);
            }
        }
        double fit[] = pcf.fit(wops);
        
        Polynomial p = new Polynomial(fit.length);
        p.p = fit;
        return p;
    }
    static PolynomialPlusGaussian pplusgfit(Histogram h, Polynomial p, double min, double max, double res, double yield_guess){
        
        double start[] = new double[p.k+3];
        start[start.length-3] = (min+max)/2;
        start[start.length-2] = res;

        start[start.length-1] = yield_guess;
        
        SimpleCurveFitter scf = SimpleCurveFitter.create(new ParametricGpG(), start);
        ArrayList<WeightedObservedPoint> wops = new ArrayList(); 
        for(int j = 0; j< h.Nbins; j++){
            if(h.minMass[j]>= min && h.maxMass[j]<= max){
                double w = 1/h.h[j];
                WeightedObservedPoint wop = new WeightedObservedPoint(w, (h.minMass[j]+h.maxMass[j])/2., h.h[j]);
                wops.add(wop);
            }
        }
        double[] result = scf.fit(wops);
        PolynomialPlusGaussian ppg = new PolynomialPlusGaussian(p.k);
        for(int i = 0; i< p.k; i++)
            ppg.p[i] = result[i];
        ppg.mean = result[p.k];
        ppg.sigma = result[p.k+1];
        ppg.N = result[p.k+2];
        
        return ppg;
    }
    
    static PolynomialPlusGaussian boxFit(Histogram h, Polynomial p, double min, double max, double res){
        
         
        double boxmin = (max+min)/2. - 1.25*res;
        double boxmax = (max+min)/2. + 1.25*res;
        double N = 0;
        for(int j = 0; j< h.Nbins; j++){
            
            if(h.minMass[j]>= boxmin && h.maxMass[j]<= boxmax){
                double binMean = (h.maxMass[j]+ h.minMass[j])/2.;
                N+= h.h[j]-p.get(binMean);
            }
        }
        
        //N/=.6;
        
        
        PolynomialPlusGaussian ppg = new PolynomialPlusGaussian(p.k);
        for(int i = 0; i< p.k; i++)
            ppg.p[i] = p.p[i];
        ppg.mean = (max+min)/2.;
        ppg.sigma = res;
        ppg.N = N;
        
        return ppg;
    }
    
    
    
    
    
    static class ParametricGpG implements
        ParametricUnivariateFunction{

            

            @Override
            public double[] gradient(double arg0, double... arg1) {
                
                double[] ret = new double [arg1.length];
                double powM = 1;
                for(int i = 0; i<arg1.length-3; i++){
                    ret[i] = powM;
                    powM *= arg0;
                    
                }
                double mean = arg1[arg1.length-3];
                double sigma = arg1[arg1.length-2];
                double N = arg1[arg1.length-1];
                
                ret[arg1.length-3] = 0;
                                    //-N*(mean-arg0)/Math.pow(sigma,2)*Math.exp(-Math.pow((arg0-mean)/sigma,2)/2)
                                    //      /(sqrt2pi*sigma);
                
                ret[arg1.length-2] =  0;
                             //N*Math.exp(-Math.pow((arg0-mean)/sigma,2)/2)/(sqrt2pi)
                            //*(-1/(sigma*sigma) + (arg0-mean)*(arg0-mean)/Math.pow(sigma,4));
                
                ret[arg1.length-1] = Math.exp(-Math.pow((arg0-mean)/sigma,2)/2)
                                                    /(sqrt2pi*sigma);
                
                return ret; 
            }

            @Override
            public double value(double arg0, double... arg1) {
                
                double y = 0;
                double powM = 1;
                for(int i = 0; i<arg1.length-3; i++){
                    y += powM*arg1[i];
                    powM *= arg0;
                }
                double mean = arg1[arg1.length-3];
                double sigma = arg1[arg1.length-2];
                double N = arg1[arg1.length-1];
                return y + N*Math.exp(-Math.pow((arg0-mean)/sigma,2)/2)
                                                /(sqrt2pi*sigma);
            }
    };

    static final double sqrt2pi = Math.sqrt(2*Math.PI);
}
