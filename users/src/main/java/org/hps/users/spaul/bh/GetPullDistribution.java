package org.hps.users.spaul.bh;

import java.io.FileNotFoundException;

import hep.aida.*;

public class GetPullDistribution {
    public static void main(String arg[]) throws FileNotFoundException{
        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create();
        IHistogramFactory hf = af.createHistogramFactory(tree);

        IHistogram1D h1 = hf.createHistogram1D("pull", 100, -10, 10);
        IPlotter p = af.createPlotterFactory().create();
        p.region(0).plot(h1);
        p.show();

        IHistogram1D h2 = hf.createHistogram1D("yield", 100, -1000, 1000);
        p = af.createPlotterFactory().create();
        p.region(0).plot(h2);
        p.show();

        IHistogram1D h3 = hf.createHistogram1D("p_value", 100, 0, 1);
        p = af.createPlotterFactory().create();
        p.region(0).plot(h3);
        p.show();

        for(int i = 0; i<100000; i++){
            GenerateFakeStuff.main(new String[]{"hist.txt", "0"});
            BumpHunt.main(new String[]{"hist.txt", "windows.txt", "4"});
            for(WindowResult wr : BumpHunt.results){
                //if(wr.p_value > .999){
                    h1.fill(wr.pull);
                    h2.fill(wr.yield);
                //}
                h3.fill(wr.p_value);
            }
        }
    }
}
