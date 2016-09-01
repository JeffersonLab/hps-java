package org.hps.users.spaul.bh;

import hep.aida.IAnalysisFactory;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.fitting.GaussianCurveFitter;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.SimpleCurveFitter;
import org.apache.commons.math3.distribution.ChiSquaredDistribution;

public class BumpHunt {
    static int degree;
    static int iter;
    public static void main(String arg[]) throws FileNotFoundException{
        
        Histogram h = new Histogram(arg[0]);
        WindowSet ws = new WindowSet(arg[1]);
        degree = Integer.parseInt(arg[2]);
        String outfile = null;
        if(arg.length > 3)
            outfile = arg[3];
        results = new ArrayList<WindowResult>();
        for(int i = 0; i< ws.N; i++){
            try{
                results.add(process(h, ws, i));
            
            } catch(Exception e){
                
            }
        }
        
        if(outfile != null){
            PrintWriter pw = new PrintWriter(outfile);
            for(WindowResult wr : results){
                pw.printf("%f %f %f %f %f", wr.min, wr.max, wr.mean, wr.yield, wr.pull);
            }
        }
    }
    static ArrayList<WindowResult> results;

    public static WindowResult process(Histogram h, WindowSet ws, int i) {
        double min = ws.minMass[i];
        double max = ws.maxMass[i];
        double res = ws.resolution[i];
    
        Polynomial p = Fitters.polyfit(h, min, max, degree);

        //PolynomialPlusGaussian pplusg = Fitters.pplusgfit(h, p, min, max, res);
        PolynomialPlusGaussian pplusg = Fitters.boxFit(h, p, min, max, res);
        
         pplusg = Fitters.pplusgfit(h, p, min, max, res, pplusg.N/.80);
        
        System.out.println(min + " " + max);
        System.out.println(Arrays.toString(p.p));
        
        //test statistic for polynomial-only fit.
        double ll1 = LogLikelihood.getLogLikelihood(h, min, max, p);
        System.out.println("poly LL: " + ll1);
        System.out.println(Arrays.toString(pplusg.p) + " " + pplusg.mean + " " + pplusg.sigma + " " + pplusg.N);
        double ll2 = LogLikelihood.getLogLikelihood(h, min, max, pplusg);
        System.out.println("gauss poly LL: "  + ll2);
        
        double p_value = new ChiSquaredDistribution(1).cumulativeProbability(ll1-ll2);
        
        System.out.println();
        
        
        double yield = pplusg.N;
        double yieldError = getYieldError(p, h, res, pplusg.mean);
        
        double pull = yield/yieldError;
        
        WindowResult wr = new WindowResult();
        wr.pplusg = pplusg;
        wr.poly = p;
        wr.max = max;
        wr.min = min;
        wr.mean = pplusg.mean;
        wr.yield = yield;
        wr.pull = pull;
        wr.p_value = p_value;
        
        return wr;
    }


    final static double twoSqrtPi  = 2*Math.sqrt(Math.PI); 
    private static double getYieldError(Polynomial p, Histogram h, double res,
            double mean) {
        double bw = h.maxMass[0]-h.minMass[0];

        return Math.sqrt(p.get(mean)*res/bw)*.345;
    }
    
    
    
    
    
    
}
