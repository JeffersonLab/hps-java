package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;
import hep.aida.IProfile1D;
import hep.aida.ref.fitter.FitResult;
import hep.aida.ref.function.AbstractIFunction;

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
 */
public class EcalCosmicClusterPlotsDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
    IProfile1D combinedSignalProfile;
    Map<EcalChannel, IProfile1D> adcProfiles = new HashMap<EcalChannel, IProfile1D>();
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
    IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
    IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory();
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
        combinedSignalProfile = aida.profile1D(inputClusterCollectionName + "/Combined Signal Profile", 100, 0., 100.);
    }

    public void detectorChanged(Detector detector) {
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {
            IProfile1D profile = aida.profile1D(inputClusterCollectionName + "/ADC Values : Channel " + String.format("%03d", channel.getChannelId()), 100, 0, 100);
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
        File plotDir = new File("fits");
        plotDir.mkdir();
        
        buffer = new StringBuffer();
        buffer.append("ecal_channel_id t0 pulse_width");
        buffer.append('\n');
        
        AbstractIFunction fitFunction = new EcalWindowModeFitFunction();
        functionFactory.catalog().add("ecal_fit_function", fitFunction);
        for (Entry<EcalChannel, IProfile1D> entry : this.adcProfiles.entrySet()) {
            doFit(entry.getKey(), entry.getValue());
        }                
        
        fitCombinedSignalProfile(this.combinedSignalProfile);
    }
    
    public void fitCombinedSignalProfile(IProfile1D combinedSignalProfile) {
        IFunction function = functionFactory.createFunctionByName("ecal_fit_function", "ecal_fit_function");
        function.setParameter("mean", 46);
        function.setParameter("sigma", 2);
        function.setParameter("pedestal", 100);
        function.setParameter("norm", 60.0);
                               
        IFitter fitter = fitFactory.createFitter();                       
        IFitResult fitResult = fitter.fit(combinedSignalProfile, function);        
        
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
        plotter.region(0).plot(combinedSignalProfile, plotStyle);
        plotter.region(0).plot(fitResult.fittedFunction(), functionStyle);
        try {
            plotter.writeToFile("fits" + File.separator + "CombinedSignalProfileFit.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
                
        buffer.append("Combined Signal Profile " + fitResult.fittedFunction().parameter("mean") + " " + fitResult.fittedFunction().parameter("sigma"));
        buffer.append('\n');
    }
    
    public void doFit(EcalChannel channel, IProfile1D profile) {
        
        IFunction function = functionFactory.createFunctionByName("ecal_fit_function", "ecal_fit_function");
        function.setParameter("mean", 48);
        function.setParameter("sigma", 2);
        function.setParameter("pedestal", conditions.getChannelConstants(channel).getCalibration().getPedestal());
        function.setParameter("norm", 60.0);
                               
        IFitter fitter = fitFactory.createFitter();                       
        IFitResult fitResult = fitter.fit(profile, function);        
        
        if (printFitResults) {
            System.out.println();
            System.out.println("Printing fit result for channel " + channel.getChannelId());
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
        plotter.region(0).plot(profile, plotStyle);
        plotter.region(0).plot(fitResult.fittedFunction(), functionStyle);
        try {
            plotter.writeToFile("fits" + File.separator + "EcalChannel" + String.format("%03d", channel.getChannelId()) + "Fit.png");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
                
        buffer.append(channel.getChannelId() + " " + fitResult.fittedFunction().parameter("mean") + " " + fitResult.fittedFunction().parameter("sigma"));
        buffer.append('\n');        
    }
}