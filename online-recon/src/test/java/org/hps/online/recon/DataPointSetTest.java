package org.hps.online.recon;

import java.util.Random;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IDataPointSetFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterRegion;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.ref.AnalysisFactory;
import hep.aida.ref.plotter.AxisStyle;
import junit.framework.TestCase;

public class DataPointSetTest extends TestCase {

    static {
        System.setProperty("hep.aida.IAnalysisFactory", AnalysisFactory.class.getName());
        System.setProperty("java.awt.headless", "false");
    }

    public void testDataPointSet() {

        IAnalysisFactory af = IAnalysisFactory.create();
        ITree tree = af.createTreeFactory().create();
        IDataPointSetFactory dpsf = af.createDataPointSetFactory(tree);

        // Create a two dimensional IDataPointSet.
        IDataPointSet dps2D = dpsf.create("dps2D", "two dimensional IDataPointSet", 2);

        // Display the results
        IPlotterFactory pf = af.createPlotterFactory();
        IPlotter plotter = pf.create("Plot IDataPointSets");

        IPlotterStyle dateAxisStyle = pf.createPlotterStyle();
        //dateAxisStyle.xAxisStyle().setParameter("type", "date");

        IPlotterRegion region = plotter.createRegion();
        region.plot(dps2D, dateAxisStyle);
        plotter.show();

        int entries = 9999;
        long waitTime = 500;

        Random r = new Random();

        for (int i = 0; i < entries; i++) {

            dps2D.addPoint();
            dps2D.point(i).coordinate(0).setValue(i);
            dps2D.point(i).coordinate(0).setErrorPlus(0);
            dps2D.point(i).coordinate(1).setValue(r.nextGaussian());
            dps2D.point(i).coordinate(1).setErrorPlus(0);
            dps2D.point(i).coordinate(1).setErrorMinus(0);

            if (i > 10) {
                AxisStyle xStyle = (AxisStyle) region.style().xAxisStyle();
                xStyle.setLowerLimit(Integer.toString(i - 10).toString());
                xStyle.setUpperLimit(Integer.toString(i + 10).toString());
                region.refresh();
            }

            try {
                Thread.sleep(waitTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}