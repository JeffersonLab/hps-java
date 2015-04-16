package org.hps.monitoring.drivers.svt;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map; 

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogramFactory;
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.ITree;
import hep.aida.jfree.plotter.Plotter;
import hep.aida.ref.rootwriter.RootFileStore;

import org.lcsim.util.Driver; 
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.ShapeFitParameters;

/**
 *  Monitoring driver that will be used when 'timing in' the SVT.
 * 
 *  @author Sho Uemura <meeg@slac.stanford.edu>
 *  @author Omar Moreno <omoreno1@ucsc.edu>
 */
public class SvtTimingInPlots extends Driver {

    // TODO: Add documentation
    // TODO: Set plot styles

    static {
        hep.aida.jfree.AnalysisFactory.register();
    } 
    
    ITree tree; 
    IHistogramFactory histogramFactory; 
    IPlotterFactory plotterFactory = IAnalysisFactory.create().createPlotterFactory();
    protected Map<String, IPlotter> plotters = new HashMap<String, IPlotter>(); 
    protected Map<SiSensor, IHistogram1D> t0Plots = new HashMap<SiSensor, IHistogram1D>(); 
    protected Map<SiSensor, IHistogram1D> amplitudePlots = new HashMap<SiSensor, IHistogram1D>(); 
    protected Map<SiSensor, IHistogram1D> chi2Plots = new HashMap<SiSensor, IHistogram1D>(); 
    protected Map<SiSensor, IHistogram1D> maxSampleNumberPerSensorPlots = new HashMap<SiSensor, IHistogram1D>(); 
    protected Map<SiSensor, IHistogram1D> maxSampleNumberPerSensorOppPlots = new HashMap<SiSensor, IHistogram1D>(); 
    protected Map<String, IHistogram1D>   maxSampleNumberPerVolumePlots = new HashMap<String, IHistogram1D>();
    protected IHistogram1D maxSampleNumberPlot = null; 
    protected Map<SiSensor, IHistogram2D> t0vAmpPlots = new HashMap<SiSensor, IHistogram2D>(); 
    protected Map<SiSensor, IHistogram2D> t0vChi2Plots = new HashMap<SiSensor, IHistogram2D>(); 
    protected Map<SiSensor, IHistogram2D> chi2vAmpPlots = new HashMap<SiSensor, IHistogram2D>();
    
    String rootFile = "default.root";
   
    boolean batchMode = false; 
    
    public void setBatchMode(boolean batchMode) { 
        this.batchMode = batchMode; 
    }
    
    private int computePlotterRegion(HpsSiSensor sensor) {

        if (sensor.getLayerNumber() < 7) {
            if (sensor.isTopLayer()) {
                return 2*(sensor.getLayerNumber() - 1); 
            } else { 
                return 2*(sensor.getLayerNumber() - 1) + 1;
            }
        } else { 
        
            if (sensor.isTopLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 4*(sensor.getLayerNumber() - 7);
                } else { 
                    return 4*(sensor.getLayerNumber() - 7) + 1;
                }
            } else if (sensor.isBottomLayer()) {
                if (sensor.getSide() == HpsSiSensor.POSITRON_SIDE) {
                    return 4*(sensor.getLayerNumber() - 7) + 2;
                } else {
                    return 4*(sensor.getLayerNumber() - 7) + 3;
                }
                }
        }
        
        return -1; 
    }
    
    protected void detectorChanged(Detector detector) {
      
        tree = IAnalysisFactory.create().createTreeFactory().create();
        histogramFactory = IAnalysisFactory.create().createHistogramFactory(tree);
        
        List<HpsSiSensor> sensors 
            = detector.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);
    
        plotters.put("L1-L3 t0", plotterFactory.create("L1-L3 t0"));
        plotters.get("L1-L3 t0").createRegions(6,2);

        plotters.put("L4-L6 t0", plotterFactory.create("L4-L6 t0"));
        plotters.get("L4-L6 t0").createRegions(6,4);
        
        plotters.put("L1-L3 Amplitude", plotterFactory.create("L1-L3 Amplitude"));
        plotters.get("L1-L3 Amplitude").createRegions(6,2);

        plotters.put("L4-L6 Amplitude", plotterFactory.create("L4-L6 Amplitude"));
        plotters.get("L4-L6 Amplitude").createRegions(6,4);
        
        plotters.put("L1-L3 Chi^2 Probability", plotterFactory.create("L1-L3 Chi^2 Probability"));
        plotters.get("L1-L3 Chi^2 Probability").createRegions(6,2);

        plotters.put("L4-L6 Chi^2 Probability", plotterFactory.create("L1-L3 Chi^2 Probability"));
        plotters.get("L4-L6 Chi^2 Probability").createRegions(6,4);
        
        plotters.put("L1-L3 Max Sample Number", plotterFactory.create("L1-L3 Max Sample Number"));
        plotters.get("L1-L3 Max Sample Number").createRegions(6,2);

        plotters.put("L4-L6 Max Sample Number", plotterFactory.create("L4-L6 Max Sample Number"));
        plotters.get("L4-L6 Max Sample Number").createRegions(6,4);

        plotters.put("L1-L3 Max Sample Number - Opposite", plotterFactory.create("L1-L3 Max Sample Number - Opposite"));
        plotters.get("L1-L3 Max Sample Number - Opposite").createRegions(6,2);

        plotters.put("L4-L6 Max Sample Number - Opposite", plotterFactory.create("L4-L6 Max Sample Number - Opposite"));
        plotters.get("L4-L6 Max Sample Number - Opposite").createRegions(6,4);
       
        plotters.put("Max Sample Per Volume", plotterFactory.create("Max Sample Per Volume"));
        plotters.get("Max Sample Per Volume").createRegions(1,2);
        
        plotters.put("L1-L3 t0 vs Amplitude", plotterFactory.create("L1-L3 t0 vs Amplitude"));
        plotters.get("L1-L3 t0 vs Amplitude").createRegions(6, 2);
        
        plotters.put("L4-L6 t0 vs Amplitude", plotterFactory.create("L4-L6 t0 vs Amplitude"));
        plotters.get("L4-L6 t0 vs Amplitude").createRegions(6, 4);

        plotters.put("L1-L3 t0 vs Chi^2 Prob.", plotterFactory.create("L1-L3 t0 vs Chi^2 Prob."));
        plotters.get("L1-L3 t0 vs Chi^2 Prob.").createRegions(6, 2);
        
        plotters.put("L4-L6 t0 vs Chi^2 Prob.", plotterFactory.create("L4-L6 t0 vs Chi^2 Prob."));
        plotters.get("L4-L6 t0 vs Chi^2 Prob.").createRegions(6, 4);

        plotters.put("L1-L3 Chi^2 Prob. vs Amplitude", plotterFactory.create("L1-L3 Chi^2 Prob. vs Amplitude"));
        plotters.get("L1-L3 Chi^2 Prob. vs Amplitude").createRegions(6, 2);
        
        plotters.put("L4-L6 Chi^2 Prob. vs Amplitude", plotterFactory.create("L4-L6 Chi^2 Prob. vs Amplitude"));
        plotters.get("L4-L6 Chi^2 Prob. vs Amplitude").createRegions(6, 4);
       
        for (HpsSiSensor sensor : sensors) {

            
            t0Plots.put(sensor,histogramFactory.createHistogram1D(sensor.getName() + " - t0",75, -50, 100.0));
            amplitudePlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Amplitude", 200, 0, 2000));
            chi2Plots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Chi^2 Probability", 20, 0, 1));
            t0vAmpPlots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - t0 v Amplitude", 75, -50, 100.0, 200, 0, 2000));
            t0vChi2Plots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - t0 v Chi^2 Probability", 75, -50, 100.0, 20, 0, 1));
            chi2vAmpPlots.put(sensor, histogramFactory.createHistogram2D(sensor.getName() + " - Chi2 v Amplitude", 20, 0, 1, 200, 0, 2000));
            maxSampleNumberPerSensorPlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Max Sample Number", 6, 0, 6));
            maxSampleNumberPerSensorOppPlots.put(sensor, histogramFactory.createHistogram1D(sensor.getName() + " - Max Sample Number - Opposite", 6, 0, 6));
            
            if (sensor.getLayerNumber() < 7) {
                plotters.get("L1-L3 t0").region(this.computePlotterRegion(sensor))
                                        .plot(t0Plots.get(sensor), this.createStyle("t0 [ns]", ""));
                plotters.get("L1-L3 Amplitude").region(this.computePlotterRegion(sensor))
                                               .plot(amplitudePlots.get(sensor), this.createStyle("Amplitude [ADC Counts] ", ""));
                plotters.get("L1-L3 Chi^2 Probability").region(this.computePlotterRegion(sensor))
                                               .plot(chi2Plots.get(sensor), this.createStyle("#chi^{2} Probability", ""));
                plotters.get("L1-L3 t0 vs Amplitude").region(this.computePlotterRegion(sensor))
                                                     .plot(t0vAmpPlots.get(sensor), this.createStyle("t0 [ns]", "Amplitude [ADC Counts]"));
                plotters.get("L1-L3 t0 vs Chi^2 Prob.").region(this.computePlotterRegion(sensor))
                                                     .plot(t0vChi2Plots.get(sensor), this.createStyle("t0 [ns]", "#chi^{2} Probability"));
                plotters.get("L1-L3 Chi^2 Prob. vs Amplitude").region(this.computePlotterRegion(sensor))
                                                     .plot(chi2vAmpPlots.get(sensor), this.createStyle("#chi^{2} Probability","Amplitude [ADC Counts]"));
               plotters.get("L1-L3 Max Sample Number").region(this.computePlotterRegion(sensor))
                                                      .plot(maxSampleNumberPerSensorPlots.get(sensor), this.createStyle("Max Sample Number", ""));
               plotters.get("L1-L3 Max Sample Number - Opposite").region(this.computePlotterRegion(sensor))
                                                      .plot(maxSampleNumberPerSensorOppPlots.get(sensor),this.createStyle("Max Sample Number", ""));
            } else {
                plotters.get("L4-L6 t0").region(this.computePlotterRegion(sensor))
                                        .plot(t0Plots.get(sensor), this.createStyle("t0 [ns]", ""));
                plotters.get("L4-L6 Amplitude").region(this.computePlotterRegion(sensor))
                                               .plot(amplitudePlots.get(sensor), this.createStyle("Amplitude [ADC Counts] ", ""));
                plotters.get("L4-L6 Chi^2 Probability").region(this.computePlotterRegion(sensor))
                                                       .plot(chi2Plots.get(sensor),  this.createStyle("#chi^{2} Probability", ""));
                plotters.get("L4-L6 t0 vs Amplitude").region(this.computePlotterRegion(sensor))
                                                     .plot(t0vAmpPlots.get(sensor), this.createStyle("t0 [ns]", "Amplitude [ADC Counts]"));
                plotters.get("L4-L6 t0 vs Chi^2 Prob.").region(this.computePlotterRegion(sensor))
                                                       .plot(t0vChi2Plots.get(sensor), this.createStyle("t0 [ns]", "#chi^{2} Probability"));
                plotters.get("L4-L6 Chi^2 Prob. vs Amplitude").region(this.computePlotterRegion(sensor))
                                                              .plot(chi2vAmpPlots.get(sensor), this.createStyle("#chi^{2} Probability","Amplitude [ADC Counts]"));
                plotters.get("L4-L6 Max Sample Number").region(this.computePlotterRegion(sensor))
                                                       .plot(maxSampleNumberPerSensorPlots.get(sensor), this.createStyle("Max Sample Number", ""));
                plotters.get("L4-L6 Max Sample Number - Opposite").region(this.computePlotterRegion(sensor))
                                                       .plot(maxSampleNumberPerSensorOppPlots.get(sensor), this.createStyle("Max Sample Number", ""));
            }
        }
       
       maxSampleNumberPerVolumePlots.put("Top", histogramFactory.createHistogram1D("SVT Top - Max Sample Number", 6, 0, 6));
       maxSampleNumberPerVolumePlots.put("Bottom", histogramFactory.createHistogram1D("SVT Bottom - Max Sample Number", 6, 0, 6));
       plotters.get("Max Sample Per Volume").region(0).plot(maxSampleNumberPerVolumePlots.get("Top"));
       plotters.get("Max Sample Per Volume").region(1).plot(maxSampleNumberPerVolumePlots.get("Bottom"));
       

       if (batchMode) return;
        
       for (IPlotter plotter : plotters.values()) { 
           plotter.show();
        }
    }
    
    public void process(EventHeader event) { 
        
        if (!event.hasCollection(LCRelation.class, "SVTFittedRawTrackerHits"))
            return;
        
        List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
        
        for (LCRelation fittedHit : fittedHits) { 
            
            RawTrackerHit rawHit = (RawTrackerHit) fittedHit.getFrom();
            
            HpsSiSensor sensor 
                = (HpsSiSensor) rawHit.getDetectorElement();
    
            
            short[] adcValues = rawHit.getADCValues();
           
            int maxAmplitude = 0;
            int maxSampleNumber = -1;
            for (int sampleN = 0; sampleN < 6; sampleN++) { 
                if (adcValues[sampleN] > maxAmplitude) { 
                    maxAmplitude = adcValues[sampleN];
                    maxSampleNumber = sampleN; 
                }
            }

            // WARNING: This is only meant to be used when running with clusters
            //          which are purely top clusters
            if (sensor.isTopLayer()) { 
                maxSampleNumberPerSensorPlots.get(sensor).fill(maxSampleNumber);
                maxSampleNumberPerVolumePlots.get("Top").fill(maxSampleNumber);
            } else { 
                maxSampleNumberPerSensorOppPlots.get(sensor).fill(maxSampleNumber);
                maxSampleNumberPerVolumePlots.get("Bottom").fill(maxSampleNumber);
            }
            
            double t0 = FittedRawTrackerHit.getT0(fittedHit);
            t0Plots.get(sensor).fill(t0);
            
            double amplitude = FittedRawTrackerHit.getAmp(fittedHit);
            amplitudePlots.get(sensor).fill(amplitude);
            
            double chi2Prob = ShapeFitParameters.getChiProb(FittedRawTrackerHit.getShapeFitParameters(fittedHit));
            chi2Plots.get(sensor).fill(chi2Prob);
    
            t0vAmpPlots.get(sensor).fill(t0, amplitude);
            t0vChi2Plots.get(sensor).fill(t0, chi2Prob);
            chi2vAmpPlots.get(sensor).fill(chi2Prob, amplitude);
            
        }   
    }
    
    @Override
    public void endOfData() { 
        RootFileStore store = new RootFileStore(rootFile);
        try {
            //tree.commit();
            store.open();
            store.add(tree);
            store.close(); 
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    IPlotterStyle createStyle(String xAxisTitle, String yAxisTitle) { 
       
        // Create a default style
        IPlotterStyle style = this.plotterFactory.createPlotterStyle();
        
        // Set the style of the X axis
        style.xAxisStyle().setLabel(xAxisTitle);
        style.xAxisStyle().labelStyle().setFontSize(14);
        style.xAxisStyle().setVisible(true);
        
        // Set the style of the Y axis
        style.yAxisStyle().setLabel(yAxisTitle);
        style.yAxisStyle().labelStyle().setFontSize(14);
        style.yAxisStyle().setVisible(true);
        
        // Turn off the histogram grid 
        style.gridStyle().setVisible(false);
        
        // Set the style of the data
        style.dataStyle().lineStyle().setVisible(false);
        style.dataStyle().outlineStyle().setVisible(true);
        style.dataStyle().outlineStyle().setThickness(3);
        style.dataStyle().fillStyle().setVisible(true);
        style.dataStyle().fillStyle().setOpacity(.10);
        style.dataStyle().fillStyle().setColor("31, 137, 229, 1");
        style.dataStyle().outlineStyle().setColor("31, 137, 229, 1");
        style.dataStyle().errorBarStyle().setVisible(false);
        
        // Turn off the legend
        style.legendBoxStyle().setVisible(false);
       
        // Turn off the title
        style.titleStyle().setVisible(false);
        
        return style;
    }
    
    
}
