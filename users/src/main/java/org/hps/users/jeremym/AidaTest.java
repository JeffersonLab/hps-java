package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import org.lcsim.util.aida.AIDA;


public class AidaTest {
    
    public static void main(String args[]) {
        AIDA aida = AIDA.defaultInstance();
        IAnalysisFactory analysisFactory = aida.analysisFactory();
        System.out.println("analysisFactory: " + analysisFactory.getClass().getCanonicalName());
        IPlotterFactory plotterFactory = analysisFactory.createPlotterFactory();
        System.out.println("plotterFactory: " + plotterFactory.getClass().getCanonicalName());
        IPlotter plotter = plotterFactory.create();
        System.out.println("unnamed plotter: " + plotter.getClass().getCanonicalName());
        plotter.createRegion();
        System.out.println("region: " + plotter.region(0).getClass().getCanonicalName());
        plotterFactory = analysisFactory.createPlotterFactory("dummy");
        System.out.println("named plotterFactory: " + plotterFactory.getClass().getCanonicalName());
        plotter = plotterFactory.create("dummy");
        System.out.println("named plotter: " + plotter.getClass().getCanonicalName());
    }
}
