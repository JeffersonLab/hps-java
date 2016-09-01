package org.hps.users.spaul.bh.test;

import java.io.IOException;
import java.util.Random;

import hep.aida.*;

public class GenerateFakeResolutions {
    public static void main(String arg[]) throws IllegalArgumentException, IOException{
        IAnalysisFactory af = IAnalysisFactory.create();
        Random r = new Random();
        int masses[] = {25, 50, 75, 100, 150, 200, 250};
        for(int m : masses){
            String filename = m + "_fake.aida";
            ITree tree = af.createTreeFactory().create(filename,"xml",false,true);
            IHistogramFactory hf = af.createHistogramFactory(tree);
            IHistogram1D h = hf.createHistogram1D("recon mass", 300, 0, 300);
            double dm$m = .05;
            for(int i = 0; i< 10000; i++){
                h.fill(m + m*dm$m*r.nextGaussian());
            }
            tree.commit();
            tree.close();
            System.out.println("wrote file " + filename);
        }
    }
}
