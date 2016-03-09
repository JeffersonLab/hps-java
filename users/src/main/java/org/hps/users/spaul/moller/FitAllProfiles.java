package org.hps.users.spaul.moller;

import hep.aida.IAnalysisFactory;
import hep.aida.IBaseHistogram;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;
import hep.aida.ITree;

import java.io.IOException;

import org.hps.users.spaul.StyleUtil;

public class FitAllProfiles {
    static IAnalysisFactory af = IAnalysisFactory.create();
    static IFitFactory ff = af.createFitFactory();
    
    public static void main(String arg[]) throws IllegalArgumentException, IOException{
        
        ITree tree = af.createTreeFactory().create(arg[0]); 
        IPlotter p = af.createPlotterFactory().create();
        p.createRegions(4,3);
        plotAndFit(p, 0, (IHistogram1D)tree.find("pypz"));
        plotAndFit(p, 1, (IHistogram1D)tree.find("pxpz"));
        plotAndFit(p, 3, (IProfile1D)tree.find("pypz vs diff"), -.3,.3);
        plotAndFit(p, 4, (IProfile1D)tree.find("pxpz vs diff"), -.3,.3);
        p.region(5).plot((IBaseHistogram)tree.find("diff"));
        plotAndFit(p, 6, (IProfile1D)tree.find("pypz vs sum"), 1.0, 1.1);
        plotAndFit(p, 7, (IProfile1D)tree.find("pxpz vs sum"), 1.0, 1.1);
        p.region(8).plot((IBaseHistogram)tree.find("sum"));
        plotAndFit(p, 9, (IProfile1D)tree.find("pypz vs mass"), .03, .036);
        plotAndFit(p, 10, (IProfile1D)tree.find("pxpz vs mass"), .03, .036);
        plotAndFit(p,11,(IHistogram1D)tree.find("mass"), .031, .034);
        
        
        for(int i = 0; i< p.numberOfRegions(); i++){

            p.region(i).style().dataStyle().fillStyle().setVisible(false);
        }
        
        p.show();
    }
    public static void plotAndFit(IPlotter p, int r, IHistogram1D h){
        p.region(r).plot(h);
        IFitResult fit = ff.createFitter().fit(h, "g");
        IFunction func = fit.fittedFunction();
        System.out.println("\n" + h.title());
        String names[] = func.parameterNames();
        double params[] = func.parameters();
        for(int i = 0; i< names.length; i++){
            System.out.printf("%s: %f\n", names[i], params[i]);
        }
        p.region(r).plot(func);
        
    }
    public static void plotAndFit(IPlotter p, int r, IHistogram1D h, double min, double max){
        p.region(r).plot(h);
        String range = String.format("range=\"(%f,%f)\"", min, max);
        IFitResult fit = ff.createFitter().fit(h, "g", range);
        IFunction func = fit.fittedFunction();
        System.out.println("\n" + h.title());
        String names[] = func.parameterNames();
        double params[] = func.parameters();
        for(int i = 0; i< names.length; i++){
            System.out.printf("%s: %f\n", names[i], params[i]);
        }
        p.region(r).plot(func);
        
    }
    public static void plotAndFit(IPlotter p, int r, IProfile1D h, double min, double max){
        p.region(r).plot(h);
        String range = String.format("range=\"(%f,%f)\"", min, max);
        
        IFitter fitter = ff.createFitter();
        IFitResult fit = fitter.fit(h, "p1", range);
        IFunction func = fit.fittedFunction();
        System.out.println("\n" + h.title());
        String names[] = func.parameterNames();
        double params[] = func.parameters();
        for(int i = 0; i< names.length; i++){
            System.out.printf("%s: %f\t", names[i], params[i]);
        }
        p.region(r).plot(func);
        
    }
}
