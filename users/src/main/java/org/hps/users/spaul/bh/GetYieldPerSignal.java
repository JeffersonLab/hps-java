package org.hps.users.spaul.bh;

import java.io.FileNotFoundException;
import java.util.Random;

import org.hps.users.spaul.StyleUtil;

import hep.aida.*;

public class GetYieldPerSignal {
    public static void main(String arg[]) throws FileNotFoundException{
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create();
        IHistogramFactory hf = af.createHistogramFactory(tree);

        IHistogram1D h1 = hf.createHistogram1D("pull", 100, -10, 10);
        IPlotter p = af.createPlotterFactory().create();
        p.region(0).plot(h1);
        p.show();

        IHistogram1D h2 = hf.createHistogram1D("yield", 100, -5000, 5000);
        p = af.createPlotterFactory().create();
        p.region(0).plot(h2);
        p.show();

        IHistogram1D h3 = hf.createHistogram1D("p_value", 100, 0, 1);
        p = af.createPlotterFactory().create();
        p.region(0).plot(h3);
        p.show();
        
        IHistogram2D h4 = hf.createHistogram2D("yield vs signal", 100, -1000, 4000, 100, -1000, 4000);
        
        p = af.createPlotterFactory().create();
        p.region(0).plot(h4);
        StyleUtil.stylize(p.region(0), "yield", "signal");
        p.show();
        
        
        WindowSet ws = new WindowSet("windows.txt");
        BumpHunt.degree = 2;

        Random r = new Random();
        
        //variables for a linear fit
        double sx = 0, sy = 0,sy2 = 0, sx2 = 0, sxy = 0, n = 0;
        for(int i = 0; i<10000; i++){
            GenerateFakeStuff.main(new String[]{"hist.txt", "5"});
            
            Histogram h = new Histogram("hist.txt");
            
            
            WindowResult wr = BumpHunt.process(h, ws, 45);
            h1.fill(wr.pull);
            h2.fill(wr.yield);
            //}
            h3.fill(wr.p_value);
            //}
            double s = r.nextDouble()*10;
            GenerateFakeStuff.main(new String[]{"hist.txt", Double.toString(s)});
            h = new Histogram("hist.txt");
            
            
             wr = BumpHunt.process(h, ws, 45);
            h4.fill(GenerateFakeStuff.pplusg.N, wr.yield);
            double x = GenerateFakeStuff.pplusg.N;
            double y = wr.yield;
            sy += y;
            sy2 += y*y;
            sx +=x;
            sx2+= x*x;
            sxy += x*y;
            n++;
        }
        double m = (sxy*n-sy*sx)/(sx2*n-sx*sx);
        double b = sy/n-m*sx/n;
        System.out.println("m = " + m);
        System.out.println("b = " + b);
        
        
        
    }
}
