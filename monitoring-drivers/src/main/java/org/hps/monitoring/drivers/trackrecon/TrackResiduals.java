package org.hps.monitoring.drivers.trackrecon;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterStyle;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

    private String plotDir = "TrackResiduals/";
    String[] trackingQuantNames = {};
    int nmodules = 6;
    private String posresDir = "PostionResiduals/";
    private String uresDir = "UResiduals/";
    private String timeresDir = "TimeResiduals/";
  
    IPlotter plotterResX;
    IPlotter plotterResY;

    String outputPlots;

    void setupPlotter(IPlotter plotter, String title) {
        plotter.setTitle(title);
        IPlotterStyle style = plotter.style();
        style.dataStyle().fillStyle().setColor("yellow");
        style.dataStyle().errorBarStyle().setVisible(false);
    }

    private int computePlotterRegion(int i, boolean istop) {
    
        int region =-99;
        if (i < 3)
            if (istop)
                region= i*4;
            else
                region= i*4+1;
        else
            if (istop)
                region= (i-3)*4+2 ;
            else
                region= (i-3)*4+3;
//     System.out.println("Setting region to "+region);
        return region;
    }

    @Override
    protected void detectorChanged(Detector detector) {

        aida.tree().cd("/");
//        resetOccupancyMap(); // this is for calculating averages         
        IAnalysisFactory fac = aida.analysisFactory();

        plotterResX = fac.createPlotterFactory().create("HPS Tracking Plots");
        setupPlotter(plotterResX, "X-Residuals");
        plotterResX.createRegions(3, 4);

        plotterResY = fac.createPlotterFactory().create("HPS Tracking Plots");
        setupPlotter(plotterResY, "Y-Residuals");
        plotterResY.createRegions(3, 4);

        for (int i = 1; i <= nmodules; i++) {
            IHistogram1D xresid = aida.histogram1D("Module " + i + " Top x Residual", 50, -getRange(i, true), getRange(i, true));
            IHistogram1D yresid = aida.histogram1D("Module " + i + " Top y Residual", 50, -getRange(i, false), getRange(i, false));
            IHistogram1D xresidbot = aida.histogram1D("Module " + i + " Bot x Residual", 50, -getRange(i, true), getRange(i, true));
            IHistogram1D yresidbot = aida.histogram1D("Module " + i + " Bot y Residual", 50, -getRange(i, false), getRange(i, false));
            plotterResX.region(computePlotterRegion(i - 1, true)).plot(xresid);
            plotterResX.region(computePlotterRegion(i - 1, false)).plot(xresidbot);
            plotterResY.region(computePlotterRegion(i - 1, true)).plot(yresid);
            plotterResY.region(computePlotterRegion(i - 1, false)).plot(yresidbot);
        }

        /*
         for (int i = 1; i <= nmodules * 2; i++) {
         IHistogram1D tresid = aida.histogram1D(plotDir + timeresDir + "HalfModule " + i + " t Residual", 50, -20, 20);
         IHistogram1D utopresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual", 50, -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
         IHistogram1D ubotresid = aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual", 50, -getRange((i + 1) / 2, false), getRange((i + 1) / 2, false));
         }
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
            int isBot = trd.getIntVal(trd.getNInt() - 1);//last Int is the top/bottom flag
            for (int i = 1; i <= nResid; i++)

                if (isBot == 1) {
                    aida.histogram1D("Module " + i + " Bot x Residual").fill(trd.getDoubleVal(i - 1));//x is the double value in the generic object
                    aida.histogram1D("Module " + i + " Bot y Residual").fill(trd.getFloatVal(i - 1));//y is the float value in the generic object
                } else {
                    aida.histogram1D("Module " + i + " Top x Residual").fill(trd.getDoubleVal(i - 1));//x is the double value in the generic object
                    aida.histogram1D("Module " + i + " Top y Residual").fill(trd.getFloatVal(i - 1));//y is the float value in the generic object                    
                }
        }
        /*
         List<GenericObject> ttdList = event.get(GenericObject.class, trackTimeDataCollectionName);
         for (GenericObject ttd : ttdList) {
         int nResid = ttd.getNDouble();
         for (int i = 1; i <= nResid; i++)
         aida.histogram1D( "HalfModule " + i + " t Residual").fill(ttd.getDoubleVal(i - 1));//x is the double value in the generic object               
         }
         */
        /*
         if (!event.hasCollection(GenericObject.class, gblStripClusterDataCollectionName))
         return;
         List<GenericObject> gblSCDList = event.get(GenericObject.class, gblStripClusterDataCollectionName);
         for (GenericObject gblSCD : gblSCDList) {
         double umeas = gblSCD.getDoubleVal(15);//TODO:  implement generic methods into GBLStripClusterData so this isn't hard coded
         double utrk = gblSCD.getDoubleVal(16);//implement generic methods into GBLStripClusterData so this isn't hard coded
         double resid = umeas - utrk;
         double tanlambda = gblSCD.getDoubleVal(21);//use the slope as a proxy for the top/bottom half of tracker

         int i = gblSCD.getIntVal(0);//implement generic methods into GBLStripClusterData so this isn't hard coded
         if (tanlambda > 0)
         aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Top u Residual").fill(resid);//x is the double value in the generic object                 
         else
         aida.histogram1D(plotDir + uresDir + "HalfModule " + i + " Bot u Residual").fill(resid);//x is the double value in the generic object                 

         }
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

    IFitResult fitGaussian(IHistogram1D h1d, IFitter fitter, String range) {
        double[] init = {20.0, 0.0, 0.2};
        return fitter.fit(h1d, "g", init, range);
//        double[] init = {20.0, 0.0, 1.0, 20, -1};
//        return fitter.fit(h1d, "g+p1", init, range);
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
