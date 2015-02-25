package org.hps.analysis.ecal.cosmic;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.ref.fitter.FitResult;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Create ADC value plots from the cosmic clusters.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
// TODO: Add Driver argument to specify where fit files will go (which can use the outputFileName arg in steering file).
public class CosmicClusterPlotsDriver extends Driver {

    EcalConditions conditions;
    EcalChannelCollection channels;
    
    IProfile1D combinedSignalProfile;
    Map<EcalChannel, IProfile1D> adcProfiles = new HashMap<EcalChannel, IProfile1D>();
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
    IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
    IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory();
    IFitter channelFitter = fitFactory.createFitter();
    IFunction channelFitFunction = new MoyalFitFunction();
    IFitResult combinedFitResult;
    double combinedMpv;
    double combinedWidth;    
    IHistogram1D fitMpvH1D;
    IHistogram1D fitWidthH1D;
    IHistogram1D fitMpvPullH1D;
    IHistogram1D fitWidthPullH1D;
    
    String fitDirectory = "fits";
    String inputClusterCollectionName = "EcalCosmicClusters";
    String rawHitsCollectionName = "EcalCosmicReadoutHits";
    boolean doFits = true;
    boolean writePulseShapeParameters = true;
    boolean printFitResults = false;
    String pulseShapeFileName = "ecal_pulse_shape_parameters.txt";
    StringBuffer buffer;

    public void setDoFits(boolean doFits) {
        this.doFits = doFits;
    }
    
    public void setFitDirectory(String fitDirectory) {
        this.fitDirectory = fitDirectory;
    }
    
    public void setWritePulseShapeParameters(boolean writePulseShapeParameters) {
        this.writePulseShapeParameters = writePulseShapeParameters;
    }
    
    public void setPulseShapeFileName(String calibrationsOutputFileName) {
        this.pulseShapeFileName = calibrationsOutputFileName;
    }
    
    public void setInputHitsCollectionName(String inputClusterCollectionName) {
        this.inputClusterCollectionName = inputClusterCollectionName;
    }
    
    public void setRawHitsCollectionName(String rawHitsCollectionName) {
        this.rawHitsCollectionName = rawHitsCollectionName;
    }
    
    public void setPrintFitResults(boolean printFitResults) {
        this.printFitResults = printFitResults;
    }
    
    public void startOfData() {
        // Setup combined signal fit profile.
        combinedSignalProfile = aida.profile1D(inputClusterCollectionName + "/Combined Signal Profile", 100, -0.5, 99.5);
        
        // Set channel fit global parameters.
        channelFitFunction.setParameter("mpv", 48);
        channelFitFunction.setParameter("width", 2);
        channelFitFunction.setParameter("norm", 60.0);        
        //channelFitter.fitParameterSettings("mpv").setFixed(true);
        //channelFitter.fitParameterSettings("width").setFixed(true);
        
        fitMpvH1D = aida.histogram1D(inputClusterCollectionName + "/Fit MPV", 100, 40., 50.);
        fitWidthH1D = aida.histogram1D(inputClusterCollectionName + "/Fit Width", 100, 1., 3.);
        
        fitMpvPullH1D = aida.histogram1D(inputClusterCollectionName + "/Fit MPV Pull", 200, -10., 10.);
        fitWidthPullH1D = aida.histogram1D(inputClusterCollectionName + "/Fit Width Pull", 200, -10., 10.);
    }

    public void detectorChanged(Detector detector) {
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();        
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {
            IProfile1D profile = aida.profile1D(inputClusterCollectionName + "/ADC Values : Channel " + String.format("%03d", channel.getChannelId()), 100, -0.5, 99.5);
            profile.annotation().addItem("xAxisLabel", "ADC Sample");
            profile.annotation().addItem("yAxisLabel", "Counts");
            adcProfiles.put(channel, profile);
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(Cluster.class, inputClusterCollectionName)) {
            if (event.hasCollection(RawTrackerHit.class, rawHitsCollectionName)) {
                Map<Long, RawTrackerHit> rawHitMap = createRawHitMap(event.get(RawTrackerHit.class, rawHitsCollectionName));
                List<Cluster> clusters = event.get(Cluster.class, inputClusterCollectionName);
                for (Cluster cluster : clusters) {
                    for (CalorimeterHit calHit : cluster.getCalorimeterHits()) {
                        RawTrackerHit rawHit = rawHitMap.get(calHit.getCellID());
                        EcalChannel channel = channels.findGeometric(rawHit.getCellID());
                        if (channel != null) {
                            IProfile1D profile = adcProfiles.get(channel);
                            for (int adcIndex = 0; adcIndex < rawHit.getADCValues().length; adcIndex++) {
                                // Fill the Profile1D with ADC value.
                                profile.fill(adcIndex, rawHit.getADCValues()[adcIndex]);
                                
                                // Fill combined Profile histogram.
                                combinedSignalProfile.fill(adcIndex, rawHit.getADCValues()[adcIndex]);
                            }
                        } else {
                            throw new RuntimeException("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(rawHit.getCellID())));
                        }
                    }
                }
            } else {
                throw new RuntimeException("Missing raw hit collection: " + rawHitsCollectionName);
            }                       
        }
    }
    
    Map<Long, RawTrackerHit> createRawHitMap(List<RawTrackerHit> rawHits) {
        Map<Long, RawTrackerHit> rawHitMap = new HashMap<Long, RawTrackerHit>();
        for (RawTrackerHit hit : rawHits) {
            rawHitMap.put(hit.getCellID(), hit);
        }
        return rawHitMap;
    }
    
    public void endOfData() {
        if (doFits) {            
            doFits();
        }
        
        if (this.writePulseShapeParameters) {
            PrintWriter out = null;
            try {
                out = new PrintWriter(this.pulseShapeFileName);
                out.print(buffer.toString());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            } finally {
                if (out != null) {
                    out.close();
                }
            }            
        } else {
            System.out.println();
            System.out.println("Printing pulse shape parameters ...");
            System.out.println(buffer.toString());
            System.out.println();
        }
    }

    private void doFits() {
        File plotDir = new File(fitDirectory);
        plotDir.mkdir();
        
        buffer = new StringBuffer();
        buffer.append("ecal_channel_id t0 pulse_width");
        buffer.append('\n');
        
        // Do combined fit and set class variables so they are available in channel fit method.
        combinedFitResult = fitCombinedSignalProfile(this.combinedSignalProfile);
        combinedMpv = combinedFitResult.fittedFunction().parameter("mpv");
        combinedWidth = combinedFitResult.fittedFunction().parameter("width");
        
        for (Entry<EcalChannel, IProfile1D> entry : this.adcProfiles.entrySet()) {
            fitChannelProfile(entry.getKey(), entry.getValue());
        }                                
    }
    
    public IFitResult fitCombinedSignalProfile(IProfile1D combinedSignalProfile) {
        
        IFunction combinedFitFunction = new MoyalFitFunction();
        combinedFitFunction.setParameter("mpv", 46);
        combinedFitFunction.setParameter("width", 2);
        combinedFitFunction.setParameter("pedestal", 100);
        combinedFitFunction.setParameter("norm", 60.0);
                               
        IFitter fitter = fitFactory.createFitter();                       
        IFitResult fitResult = fitter.fit(combinedSignalProfile, combinedFitFunction);        
        
        if (printFitResults) {
            System.out.println();
            System.out.println("Printing fit result for channel Combined Signal Profile");
            ((FitResult)fitResult).printResult();
            System.out.println();
        }
                       
        IPlotter plotter = plotterFactory.create();
        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.legendBoxStyle().setVisible(true);
        functionStyle.statisticsBoxStyle().setVisible(true);
        IPlotterStyle plotStyle = plotterFactory.createPlotterStyle();
        plotStyle.dataStyle().fillStyle().setColor("blue");
        plotStyle.legendBoxStyle().setVisible(true);
        plotStyle.statisticsBoxStyle().setVisible(true);
        
        plotter.createRegion();
        plotStyle.dataStyle().errorBarStyle().setVisible(false);
        plotter.region(0).plot(combinedSignalProfile, plotStyle);
        plotter.region(0).plot(fitResult.fittedFunction(), functionStyle);
        try {
            plotter.writeToFile(fitDirectory + File.separator + "CombinedSignalProfileFit.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
                
        buffer.append("Combined Signal Profile " + fitResult.fittedFunction().parameter("mpv") + " " + fitResult.fittedFunction().parameter("width"));
        buffer.append('\n');
        
        return fitResult;
    }
    
    public void fitChannelProfile(EcalChannel channel, IProfile1D profile) {
        
        if (profile.entries() == 0) {
            System.err.println("No data for channel " + channel.getChannelId() + " so fit is skipped!");
            return;
        }
        
        channelFitFunction.setParameter("pedestal", conditions.getChannelConstants(channel).getCalibration().getPedestal());        
                               
        channelFitter = fitFactory.createFitter();                       
        IFitResult fitResult = channelFitter.fit(profile, channelFitFunction);        
        
        if (printFitResults) {
            System.out.println("Printing fit result for channel " + channel.getChannelId());
            ((FitResult)fitResult).printResult();
        }
        
        fitMpvH1D.fill(fitResult.fittedFunction().parameter("mpv"));
        fitWidthH1D.fill(fitResult.fittedFunction().parameter("width"));        
        fitMpvPullH1D.fill((fitResult.fittedFunction().parameter("mpv") - this.combinedMpv) / fitResult.errors()[2]);
        fitWidthPullH1D.fill((fitResult.fittedFunction().parameter("width") - this.combinedWidth) / fitResult.errors()[3]);
                       
        IPlotter plotter = plotterFactory.create();
        IPlotterStyle functionStyle = plotterFactory.createPlotterStyle();
        functionStyle.dataStyle().outlineStyle().setColor("red");
        functionStyle.legendBoxStyle().setVisible(true);
        functionStyle.statisticsBoxStyle().setVisible(true);
        IPlotterStyle plotStyle = plotterFactory.createPlotterStyle();
        plotStyle.dataStyle().fillStyle().setColor("blue");
        plotStyle.legendBoxStyle().setVisible(true);
        plotStyle.statisticsBoxStyle().setVisible(true);
        
        plotter.createRegion();
        plotter.region(0).plot(profile, plotStyle);
        plotter.region(0).plot(fitResult.fittedFunction(), functionStyle);
        try {
            plotter.writeToFile(fitDirectory + File.separator + "EcalChannel" + String.format("%03d", channel.getChannelId()) + "Fit.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
                
        buffer.append(channel.getChannelId() + " " + fitResult.fittedFunction().parameter("mpv") + " " + fitResult.fittedFunction().parameter("width"));
        buffer.append('\n');
    }
}