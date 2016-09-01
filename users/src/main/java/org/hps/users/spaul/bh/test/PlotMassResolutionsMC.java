package org.hps.users.spaul.bh.test;

import java.io.IOException;

import org.hps.users.spaul.StyleUtil;

import hep.aida.*;

public class PlotMassResolutionsMC {
    public static void main(String[] arg) throws IllegalArgumentException, IOException{
        IAnalysisFactory af = IAnalysisFactory.create();
        ITreeFactory tf  = af.createTreeFactory();
        IPlotterFactory pf = af.createPlotterFactory();
        
        IPlotter p1 = pf.create();
        p1.createRegions(1,2);
        IPlotterRegion r1 = p1.region(0);
        IPlotterRegion r2 = p1.region(1);
        
        IDataPointSet dps = af.createDataPointSetFactory(af.createTreeFactory().create()).create("mass resolutions", 2);
        
        for(int i = 0; i<arg.length/2; i++){
            double mass = Double.parseDouble(arg[2*i]);
            String file = arg[2*i+1];
            ITree tree = tf.create(file);
            IHistogram1D massRecon = (IHistogram1D) tree.find("recon mass");
            
            massRecon.setTitle(mass + " MeV");
            massRecon.scale(1/massRecon.maxBinHeight());
            double rms = massRecon.rms();
            double n = massRecon.entries();
            // http://web.eecs.umich.edu/~fessler/papers/files/tr/stderr.pdf
            double drms = rms *Math.sqrt(Math.sqrt(2/(n-1)));
            r1.plot(massRecon);
            
            IDataPoint dp = dps.addPoint();
            dp.coordinate(0).setValue(mass);
            dp.coordinate(1).setValue(rms/mass);
            dp.coordinate(1).setErrorPlus(drms/mass);
            dp.coordinate(1).setErrorMinus(drms/mass);
        }
        r2.plot(dps);
        
        StyleUtil.stylize(r1, "Mass Resolution", "A' mass (GeV)", "arbitrary units");
        StyleUtil.stylize(r2, "Mass Resolution", "A' mass (GeV)", "dm/m");
        //r2.style().dataStyle().setParameter("showLineBetweenPoints", arg1)
        r2.setYLimits(0, .2);
        r1.setXLimits(0, .300);
        r2.setXLimits(0, .300);
        
        p1.show();
    }
}
