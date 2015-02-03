package org.hps.monitoring.drivers.example;

import hep.aida.ICloud1D;
import hep.aida.ICloud2D;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierHelper;
import org.lcsim.detector.identifier.Identifier;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Example monitoring plots, currently only using ECAL data.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
/* 

TODO List 

Add the following example plot types...
 
[X] Histogram1D 

[X] Histogram1D overlay  

[X] Cloud1D

[X] Cloud1D overlay 

[X] Histogram2D color map

[X] Histogram2D box plot
 
[X] Histogram2D overlay

[ ] Cloud2D as scatter plot

[ ] Cloud2D scatter plot overlay

[ ] Cloud2D converted to histogram
 
[ ] Profile1D

[ ] Profile2D

[X] IFunction

[ ] IDataPointSet

See this link for complete list.

https://docs.google.com/spreadsheets/d/1bqKvriNOEaeTrpTrk38kBGXM8oC_F5QIZeLcSSa3JsQ/

*/
public class ExamplePlotDriver extends Driver {
    
    AIDA aida = AIDA.defaultInstance();
    
    IPlotterFactory plotterFactory;
    IFunctionFactory functionFactory;
    IFitFactory fitFactory;
    
    IHistogram1D calRawHitH1D, calClusterH1D, calHitH1D, calClusterEnergyH1D;   
    IHistogram2D calHitMapH2D, calRawHitMapH2D;    
    ICloud1D calRawHitsC1D, calClustersC1D;
    ICloud2D calHitsVsEnergyC2D;
    IFunction fittedFunction;
    
    final static String ECAL_READOUT_HITS = "EcalReadoutHits";
    final static String ECAL_CAL_HITS = "EcalCalHits";
    final static String ECAL_CLUSTERS = "EcalClusters";
    
    IIdentifierHelper helper;
    
    private static final Logger minuitLogger = Logger.getLogger("org.freehep.math.minuit");
    static {
        minuitLogger.setLevel(Level.OFF);
    }
        
    public ExamplePlotDriver() {
    }
    
    public void detectorChanged(Detector detector) {
        helper = detector.getSubdetector("Ecal").getDetectorElement().getIdentifierHelper();
    }
    
    public void startOfData() {                
               
        // AIDA boilerplate stuff.
        plotterFactory = aida.analysisFactory().createPlotterFactory("Example Plots");
        functionFactory = aida.analysisFactory().createFunctionFactory(null);
        fitFactory = aida.analysisFactory().createFitFactory();        
        
        IPlotter plotter = null;
        IPlotterStyle style = null;
        
        // Define all of the AIDA objects to be used for example plotting.
        calClusterEnergyH1D = aida.histogram1D("Cal Cluster Energy", 100, 0., 10.);
        calRawHitH1D = aida.histogram1D("CalRawHit Count H1D", 20, 0., 20.);
        calHitH1D = aida.histogram1D("CalHit Count H1D", 20, 0., 20.);
        calClusterH1D = aida.histogram1D("CalCluster Count H1D", 20, 0., 20.);
        calHitMapH2D = aida.histogram2D("CalHit Map H2D", 47, -23.5, 23.5, 11, -5.5, 5.5);
        calRawHitMapH2D = aida.histogram2D("CalRawHit Map H2D", 47, -23.5, 23.5, 11, -5.5, 5.5);
        calRawHitsC1D = aida.cloud1D("CalRawHit Count C1D", 500);
        calClustersC1D = aida.cloud1D("CalCluster Count C1D", 500);
        calHitsVsEnergyC2D = aida.cloud2D("CalHits vs Energy C2D", 1000000);
        fittedFunction = functionFactory.createFunctionByName("Gaussian", "G");
        fittedFunction.setTitle("Example Fit");
        
        /*
         * Histogram1D 
         */                       
        plotter = plotterFactory.create("IHistogram1D");
        plotter.createRegion();
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setColor("blue");
        plotter.region(0).setTitle("IHistogram1D");
        plotter.region(0).plot(calClusterEnergyH1D, style);
        plotter.show();
        
        /*
         * Fit of Histogram1D
         */
        plotter = plotterFactory.create("IHistogram1D Fit");
        plotter.createRegion();
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setColor("blue");
        plotter.region(0).setTitle("IHistogram1D");
        plotter.region(0).plot(calHitH1D, style);                
        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().lineStyle().setColor("red");
        functionStyle.dataStyle().markerStyle().setVisible(true);
        functionStyle.dataStyle().markerStyle().setColor("black");
        functionStyle.dataStyle().markerStyle().setShape("dot");
        functionStyle.dataStyle().markerStyle().setSize(2);
        plotter.region(0).plot(fittedFunction, functionStyle);
        plotter.show();                
                                                    
        /*
         * Histogram1D overlay
         */                         
        plotter = plotterFactory.create("Overlayed IHistogram1D");
        plotter.createRegion();
        plotter.region(0).setTitle("Overlayed IHistogram1D");
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setVisible(false);
        style.dataStyle().errorBarStyle().setVisible(false);
        style.dataStyle().lineStyle().setColor("blue");
        plotter.region(0).plot(calRawHitH1D, style);
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setVisible(false);
        style.dataStyle().errorBarStyle().setVisible(false);
        style.dataStyle().lineStyle().setColor("red");
        plotter.region(0).plot(calHitH1D, style);        
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setVisible(false);
        style.dataStyle().errorBarStyle().setVisible(false);
        style.dataStyle().lineStyle().setColor("green");
        plotter.region(0).plot(calClusterH1D, style);
        plotter.show();           
        
        /*
         * Histogram2D as color map.
         */                 
        plotter = plotterFactory.create("Histogram2D Color Map");
        style = createDefaultPlotterStyle();
        style.setParameter("hist2DStyle", "colorMap");
        plotter.createRegion();
        plotter.region(0).setTitle("Histogram2D Color Map");
        plotter.region(0).plot(calHitMapH2D, style);
        plotter.show();        
        
        /*
         * Histogram2D as box plot.
         */       
        plotter = plotterFactory.create("Histogram2D Box Plot");
        style = createDefaultPlotterStyle();
        style.dataStyle().fillStyle().setVisible(false);
        style.setParameter("hist2DStyle", "box");
        plotter.createRegion();
        plotter.region(0).setTitle("Histogram2D Box Plot");
        plotter.region(0).plot(calHitMapH2D, style);
        plotter.show();
        
        /*
         * Histogram2D overlay box plots
         */                
        plotter = plotterFactory.create("Overlaid IHistogram2D Box Plots");
        plotter.createRegion();
        plotter.region(0).setTitle("Overlaid Histogram2D Box Plots");
        style = createDefaultPlotterStyle();
        style.setParameter("hist2DStyle", "box");
        style.dataStyle().lineStyle().setColor("green");
        plotter.region(0).plot(calRawHitMapH2D, style);
        style = createDefaultPlotterStyle();
        style.setParameter("hist2DStyle", "box");
        style.dataStyle().lineStyle().setColor("red");
        plotter.region(0).plot(calHitMapH2D, style);
        plotter.show();       
        
        /*
         * Cloud1D which will convert to a histogram on the fly
         */                
        plotter = plotterFactory.create("Cloud1D");
        style = createDefaultPlotterStyle();
        plotter.createRegion();
        plotter.region(0).setTitle("ICloud1D");
        plotter.region(0).plot(calRawHitsC1D, style);
        plotter.show();
        
        /*
         * Cloud1D overlay
         */                
        plotter = plotterFactory.create("Overlayed Cloud1D");        
        plotter.createRegion();
        plotter.region(0).setTitle("Overlayed Cloud1D");
        style = createDefaultPlotterStyle();
        style.dataStyle().lineStyle().setVisible(true);
        style.dataStyle().lineStyle().setColor("green");
        style.dataStyle().fillStyle().setVisible(false);
        plotter.region(0).plot(calRawHitsC1D, style);
        style = createDefaultPlotterStyle();
        style.dataStyle().lineStyle().setVisible(true);
        style.dataStyle().lineStyle().setColor("red");
        style.dataStyle().fillStyle().setVisible(false);
        plotter.region(0).plot(calClustersC1D, style);
        plotter.show();        
        
        /*
         * Cloud2D as scatter plot.
         */        
        plotter = plotterFactory.create("Cloud2D Scatter Plot");
        plotter.createRegion();
        plotter.region(0).setTitle("Cloud2D Scatter Plot");
        style = createDefaultPlotterStyle();
        style.dataStyle().markerStyle().setVisible(true); /* FIXME: This is false by default! */
        style.dataStyle().markerStyle().setColor("purple");
        style.dataStyle().markerStyle().setSize(2);
        style.dataStyle().markerStyle().setShape("diamond");
        plotter.region(0).plot(calHitsVsEnergyC2D, style);        
        plotter.show();                      
    }
    
    public void process(EventHeader event) {
        //printCollectionSummary(event);
               
        if (event.hasCollection(RawCalorimeterHit.class, ECAL_READOUT_HITS)) {
            List<RawCalorimeterHit> hits = event.get(RawCalorimeterHit.class, ECAL_READOUT_HITS);
            int nHits = hits.size();
            calRawHitH1D.fill(nHits);
            calRawHitsC1D.fill(nHits);                      
            for (RawCalorimeterHit hit : hits) {
                IIdentifier id = new Identifier(hit.getCellID());
                int ix = helper.getValue(id, "ix");
                int iy = helper.getValue(id, "iy");
                calRawHitMapH2D.fill(ix, iy);
            }            
        }
        
        if (event.hasCollection(CalorimeterHit.class, ECAL_CAL_HITS)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ECAL_CAL_HITS);
            calHitH1D.fill(hits.size());
            double totalEnergy = 0;
            for (CalorimeterHit hit : hits) {
                int ix = hit.getIdentifierFieldValue("ix");
                int iy = hit.getIdentifierFieldValue("iy");
                calHitMapH2D.fill(ix, iy);
                totalEnergy += hit.getCorrectedEnergy();
            }
            calHitsVsEnergyC2D.fill(hits.size(), totalEnergy);
        }
        
        if (event.hasCollection(Cluster.class, ECAL_CLUSTERS)) {
            List<Cluster> clusters = event.get(Cluster.class, ECAL_CLUSTERS); 
            calClusterH1D.fill(clusters.size());
            calClustersC1D.fill(clusters.size());
            for (Cluster cluster : clusters) {
                calClusterEnergyH1D.fill(cluster.getEnergy());
            }
        }    
        
        // Fit an H1D.
        IFunction currentFitFunction = performGaussianFit(calHitH1D).fittedFunction();
        
        // Copy function parameters into the plotted function which will trigger an update.
        fittedFunction.setParameters(currentFitFunction.parameters());
    }
    
    IPlotterStyle createDefaultPlotterStyle() {
        IPlotterStyle style = plotterFactory.createPlotterStyle();
        style.gridStyle().setVisible(false);      
        style.legendBoxStyle().setVisible(true);
        return style;
    }
    
    IFitResult performGaussianFit(IHistogram1D histogram) {
        IFunction function = functionFactory.createFunctionByName("Example Fit", "G");        
        IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
        double[] parameters = new double[3];        
        parameters[0] = histogram.maxBinHeight();
        parameters[1] = histogram.mean();
        parameters[2] = histogram.rms();
        function.setParameters(parameters);
        IFitResult fitResult = fitter.fit(histogram, function);
        return fitResult;
    }
    
}   