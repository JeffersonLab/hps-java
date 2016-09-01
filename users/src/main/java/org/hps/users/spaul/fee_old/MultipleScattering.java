package org.hps.users.spaul.feecc;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;

import java.util.Random;

public class MultipleScattering {
    
    
    /* 
     V.L. Highland, Nucl. Instrum. Methods 129, 497 (1975); Nucl. Instrum. Methods 161, 171 (1979)
     
     G.R. Lynch and O.I. Dahl, Nucl. Instrum. Methods B58, 6 (1991).
     
     */
    /**
     * @param t number of scattering lengths
     * @param p momentum in GeV/c
     * @return the rms scattering angle of an electron with beta == 1.
     */
    static double getThetaRMS(double t, double p){
        return .0136/p*Math.sqrt(t)*(1+.038*Math.log(t));
    }
    /**
     * Mo Tsai equations A.4-5
     * @param Z
     * @return
     */
    static double b(int Z){
        double xi = Math.log(1440.*Math.pow(Z, -2/3.))/Math.log(183.*Math.pow(Z,-1/3.));
        System.out.println(xi);
        return 4/3.*(1+1/9.*(Z+1.)/(Z+xi)/Math.log(183.*Math.pow(Z,-1/3.)));
        
    
    }
    
    
    public static void main(String arg[]){
        
        System.out.println(b(6));
        System.out.println(b(74));
        
        double bt = 0.03;
        
        IAnalysisFactory af = IAnalysisFactory.create();
        IHistogramFactory hf = af.createHistogramFactory(af.createTreeFactory().create());
        IHistogram1D h = hf.createHistogram1D("x", 100, 0, 1.5);
        IPlotter p = af.createPlotterFactory().create();
        p.region(0).plot(h);
        p.show();
        for(int i = 0; i< 100000; i++){
            h.fill(getRandomEnergyFraction(bt)*getRandomEnergyFraction(bt)*(1+r.nextGaussian()*.045)*1.056);
        }
    }
    
    static double pdf(double x, double bt){
        return bt/(1-x)*(x+3/4.*(1-x)*(1-x))*Math.pow(Math.log(1./x), bt);
    }
    static Random r = new Random();
    /**************************
     * CONFIGURABLE VARIABLES *
     **************************/
    /**
     * step size in x
     */
    static double dx = .001;
    /**
     * starting distance from peak to begin numerical integration
     */
    static double delta0 = .05;
    
    /**
     * fraction of the energy to cut off at.  (all events with less than this energy will be blanked out,
     * and therefore not simulated in slic.  Slic was having problems dealing with low energy events.  
     */
    static double xCutoff = .5;
    
    static double getRandomEnergyFraction(double bt){
        double p = r.nextDouble(); 
        if(p< Math.pow(delta0, bt)){
            return 1-Math.pow(p, 1/bt);
        }
        double sum = Math.pow(delta0,bt);
        if(sum > p){
            return 1;
        }
        double x = 1-delta0;
        while(true){
            double contribution = pdf(x, bt)*dx;
            if(sum + contribution > p)
            {
                return x - (p-sum)/contribution*dx;
            }
            if(x < 0)
            {
                return 0;
            }
            x -= dx;
            sum += contribution;
        }
        //return 1;
    }
}
