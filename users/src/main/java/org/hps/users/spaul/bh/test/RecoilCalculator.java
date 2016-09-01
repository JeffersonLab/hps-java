package org.hps.users.spaul.bh.test;

import java.util.Random;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogramFactory;
import hep.aida.IPlotter;
import hep.aida.ITree;

public class RecoilCalculator {
    
    //some ridiculous equations that came out of mathematica, and then translated into java.  
    //might have some mistakes in there
    static public double getProductAB(double a, double b, double sum, double wa, double wb, double wsum){
        return ((a*wsum*wa - b*wsum*wb + sum* wsum* wb + a*wa*wb)*(-a*wsum*wa + sum*wsum*wa + 
                   b*wsum*wb + b*wa*wb))/Math.pow(wsum*(wa + wb) + wa*wb,2);
    }
    static public double getProductError(double a, double b, double sum, double wa, double wb, double wsum){
        return Math.sqrt((2*b*sum*wsum*wb*(2*wsum*wsum*(wa - wb) + wsum*wa*(wa - wb) + wa*wa*wb) + 
                   b*b*(wsum+ wa)*wb*wb*(4*wsum*wsum + wa*wb + wsum*(wa + wb)) + 
                   a*a*wa*wa*(wsum + wb)*(4*wsum*wsum + wa*wb + wsum*(wa + wb)) + 
                   sum*sum*wsum*wsum*(wsum*(wa - wb)*(wa-wb)+ wa*wb*(wa + wb)) - 
                   2*a*wsum*wa*(sum*(2*wsum*wsum*(wa - wb) + wsum*(wa - wb)*wb - wa*wb*wb) + 
                      b*wb*(4*wsum*wsum + 3*wa*wb + 3*wsum*(wa + wb))))
                      /Math.pow(wa*wb + wsum*(wa + wb),3));
        
    }
    
    public static void main(String arg[]){
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create();
        IHistogramFactory hf = af.createHistogramFactory(tree);

        IHistogram1D h1 = hf.createHistogram1D("stuff", 100, .4, 1);
        IPlotter p = af.createPlotterFactory().create();
        p.region(0).plot(h1);
        p.show();
        
        IHistogram1D h2 = hf.createHistogram1D("stuff2", 100, 0, .05);
        IPlotter p2 = af.createPlotterFactory().create();
        p2.region(0).plot(h2);
        p2.show();
        
        double smean = 2;
        double amean = 1.5;
        double bmean = .5;
        
        double ds  = .003;
        double da = .005;
        double db = .07;
        
        System.out.println(getProductAB(amean, bmean, smean, 1/(da*da), 1/(db*db), 1/(ds*ds))
                -getProductAB(bmean, amean, smean, 1/(db*db), 1/(da*da), 1/(ds*ds)))
                ;
        System.out.println(getProductError(amean, bmean, smean, 1/(da*da), 1/(db*db), 1/(ds*ds))
                -getProductError(bmean, amean, smean, 1/(db*db), 1/(da*da), 1/(ds*ds)))
                ;
        Random r =new Random();
        for(int i = 0; i< 10000; i++){
            
            double s = r.nextGaussian()*ds + smean;
            double a = r.nextGaussian()*da + amean;
            double b = r.nextGaussian()*db + bmean;
            
            double product = getProductAB(a, b, s, 1/(da*da), 1/(db*db), 1/(ds*ds));
            double dproduct = getProductError(a,b,s, 1/(da*da), 1/(db*db), 1/(ds*ds));
            
            h1.fill(product);
            h2.fill(dproduct);
        }
        
        
        
    }
    
    
}
