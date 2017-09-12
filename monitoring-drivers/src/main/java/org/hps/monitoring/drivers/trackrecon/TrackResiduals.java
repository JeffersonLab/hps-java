package org.hps.monitoring.drivers.trackrecon;

import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.fitAndPutParameters;
import static org.hps.monitoring.drivers.trackrecon.PlotAndFitUtilities.plot;
import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author mgraham
 */
public class TrackResiduals extends Driver {

    // Collection Names
    String trackTimeDataCollectionName = "TrackTimeData";
    String trackResidualsCollectionName = "TrackResiduals";
    String gblStripClusterDataCollectionName = "GBLStripClusterData";
    private AIDA aida = AIDA.defaultInstance();
    int nEvents = 0;
    int nmodules = 6;

    IPlotter plotterResX;
    IPlotter plotterResY;

    IPlotterFactory plotterFactory;
    IFunctionFactory functionFactory;
    IFitFactory fitFactory;
    IFunction fd0Top;

    String outputPlots;
    IHistogram1D[] xresidTop = new IHistogram1D[nmodules];
    IHistogram1D[] yresidTop = new IHistogram1D[nmodules];
    IHistogram1D[] xresidBot = new IHistogram1D[nmodules];
    IHistogram1D[] yresidBot = new IHistogram1D[nmodules];

    IFunction[] fxresidTop = new IFunction[nmodules];
    IFunction[] fyresidTop = new IFunction[nmodules];
    IFunction[] fxresidBot = new IFunction[nmodules];
    IFunction[] fyresidBot = new IFunction[nmodules];

    private int computePlotterRegion(int i, boolean istop) {

        int region = -99;
        if (i < 3)
            if (istop)
                region = i * 4;
            else
                region = i * 4 + 1;
        else if (istop)
            region = (i - 3) * 4 + 2;
        else
            region = (i - 3) * 4 + 3;
        // System.out.println("Setting region to "+region);
        return region;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");
        // resetOccupancyMap(); // this is for calculating averages
        IAnalysisFactory fac = aida.analysisFactory();
        IPlotterFactory pfac = fac.createPlotterFactory("Track Residuals");
        functionFactory = aida.analysisFactory().createFunctionFactory(null);
        fitFactory = aida.analysisFactory().createFitFactory();
        plotterResX = pfac.create("X Residuals");
        plotterResY = pfac.create("Y Residuals");
        plotterResX.createRegions(3, 4);
        plotterResY.createRegions(3, 4);

        for (int i = 1; i <= nmodules; i++) {
            xresidTop[i - 1] = aida.histogram1D("Module " + i + " Top x Residual", 50, -getRange(i, true),
                    getRange(i, true));
            yresidTop[i - 1] = aida.histogram1D("Module " + i + " Top y Residual", 50, -getRange(i, false),
                    getRange(i, false));
            xresidBot[i - 1] = aida.histogram1D("Module " + i + " Bot x Residual", 50, -getRange(i, true),
                    getRange(i, true));
            yresidBot[i - 1] = aida.histogram1D("Module " + i + " Bot y Residual", 50, -getRange(i, false),
                    getRange(i, false));

            fxresidTop[i - 1] = functionFactory.createFunctionByName("Gaussian", "G");
            fyresidTop[i - 1] = functionFactory.createFunctionByName("Gaussian", "G");
            fxresidBot[i - 1] = functionFactory.createFunctionByName("Gaussian", "G");
            fyresidBot[i - 1] = functionFactory.createFunctionByName("Gaussian", "G");

            plot(plotterResX, xresidTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterResX, xresidBot[i - 1], null, computePlotterRegion(i - 1, false));
            plot(plotterResY, yresidTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterResY, yresidBot[i - 1], null, computePlotterRegion(i - 1, false));

            plot(plotterResX, fxresidTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterResX, fxresidBot[i - 1], null, computePlotterRegion(i - 1, false));
            plot(plotterResY, fyresidTop[i - 1], null, computePlotterRegion(i - 1, true));
            plot(plotterResY, fyresidBot[i - 1], null, computePlotterRegion(i - 1, false));

        }

        plotterResX.show();
        plotterResY.show();

        /*
         * for (int i = 1; i <= nmodules * 2; i++) {
         * IHistogram1D tresid = aida.histogram1D(plotDir + timeresDir + "HalfModule " + i + " t Residual", 50, -20,
         * 20);
         * IHistogram1D utopresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual", 50,
         * -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
         * IHistogram1D ubotresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual", 50,
         * -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
         * }
         */
    }

    @Override
    public void process(EventHeader event) {
        aida.tree().cd("/");
        if (!event.hasCollection(GenericObject.class, trackTimeDataCollectionName))
            return;
        if (!event.hasCollection(GenericObject.class, trackResidualsCollectionName))
            return;
        nEvents++;
        List<GenericObject> trdList = event.get(GenericObject.class, trackResidualsCollectionName);
        for (GenericObject trd : trdList) {
            int nResid = trd.getNDouble();
            int isBot = trd.getIntVal(trd.getNInt() - 1);// last Int is the top/bottom flag
            for (int i = 0; i < nResid; i++)

                if (isBot == 1) {
                    xresidBot[i].fill(trd.getDoubleVal(i));// x is the double value in the generic object
                    yresidBot[i].fill(trd.getFloatVal(i));// y is the float value in the generic object
                } else {
                    xresidTop[i].fill(trd.getDoubleVal(i));// x is the double value in the generic object
                    yresidTop[i].fill(trd.getFloatVal(i));// y is the float value in the generic object
                }
        }
        for (int i = 0; i < nmodules; i++) {
            fitAndPutParameters(xresidTop[i], fxresidTop[i]);
            fitAndPutParameters(yresidTop[i], fyresidTop[i]);
            fitAndPutParameters(xresidBot[i], fxresidBot[i]);
            fitAndPutParameters(yresidBot[i], fyresidBot[i]);
        }

        /*
         * List<GenericObject> ttdList = event.get(GenericObject.class, trackTimeDataCollectionName);
         * for (GenericObject ttd : ttdList) {
         * int nResid = ttd.getNDouble();
         * for (int i = 1; i <= nResid; i++)
         * aida.histogram1D( "HalfModule " + i + " t Residual").fill(ttd.getDoubleVal(i - 1));//x is the double value in
         * the generic object
         * }
         */
        /*
         * if (!event.hasCollection(GenericObject.class, gblStripClusterDataCollectionName))
         * return;
         * List<GenericObject> gblSCDList = event.get(GenericObject.class, gblStripClusterDataCollectionName);
         * for (GenericObject gblSCD : gblSCDList) {
         * double umeas = gblSCD.getDoubleVal(15);//TODO: implement generic methods into GBLStripClusterData so this
         * isn't hard coded
         * double utrk = gblSCD.getDoubleVal(16);//implement generic methods into GBLStripClusterData so this isn't hard
         * coded
         * double resid = umeas - utrk;
         * double tanlambda = gblSCD.getDoubleVal(21);//use the slope as a proxy for the top/bottom half of tracker
         * 
         * int i = gblSCD.getIntVal(0);//implement generic methods into GBLStripClusterData so this isn't hard coded
         * if (tanlambda > 0)
         * aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual").fill(resid);//x is the double
         * value in the generic object
         * else
         * aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual").fill(resid);//x is the double
         * value in the generic object
         * 
         * }
         */
    }

    private String getQuantityName(int itype, int iquant, int top, int nlayer) {
        String typeString = "position_resid";
        String quantString = "mean_";
        if (itype == 1)
            typeString = "time_resid";
        if (iquant == 1)
            quantString = "sigma_";

        String botString = "bot_";
        if (top == 1)
            botString = "top_";
        if (top == 2)
            botString = "";

        String layerString = "module" + nlayer;
        if (itype == 1)
            layerString = "halfmodule" + nlayer;

        return typeString + quantString + botString + layerString;
    }

    private double getRange(int layer, boolean isX) {
        double range = 2.5;
        if (isX) {
            if (layer == 1)
                return 0.5;
            if (layer == 2)
                return 0.5;
            if (layer == 3)
                return 0.5;
            if (layer == 4)
                return 1.0;
            if (layer == 5)
                return 1.0;
            if (layer == 6)
                return 1.0;
        } else {
            if (layer == 1)
                return 0.005;
            if (layer == 2)
                return 0.5;
            if (layer == 3)
                return 0.5;
            if (layer == 4)
                return 1.0;
            if (layer == 5)
                return 1.0;
            if (layer == 6)
                return 1.5;
        }
        return range;

    }

    public void setOutputPlots(String output) {
        this.outputPlots = output;
    }

    @Override
    public void endOfData() {
        if (outputPlots != null)
            try {
                plotterResX.writeToFile(outputPlots + "-X.gif");
                plotterResY.writeToFile(outputPlots + "-Y.gif");
            } catch (IOException ex) {
                Logger.getLogger(TrackingReconPlots.class.getName()).log(Level.SEVERE, null, ex);
            }

    }

}
