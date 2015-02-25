package org.hps.analysis.ecal.cosmic;

import hep.aida.IAnalysisFactory;
import hep.aida.IDataPointSet;
import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;
import hep.aida.ref.fitter.FitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * <p>
 * This Driver will perform a function fit on ECAL window mode data
 * to determine the likelihood of a signal being present, e.g. from a cosmic
 * ray MIP signal.  By default, the mean and sigma are fixed in the fit, and the
 * pedestal and normalization are allowed to float.  The pedestal can also be 
 * configured as fixed using the {@link #setFixPedestal(boolean)} method.
 * <p>
 * Those hits with a signal significance greater than a settable
 * threshold (by default set to 4 sigma) will be written into an output collection
 * of selected hits that can be used by other Drivers.   
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim Nelson <tknelson@slac.stanford.edu>
 */
public class DualThresholdSignalFitDriver extends Driver {

    // ECAL conditions data.
    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
        
    // AIDA setup.    
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    IFunctionFactory functionFactory = analysisFactory.createFunctionFactory(null);
    IFitFactory fitFactory = analysisFactory.createFitFactory();
    IFitter fitter = fitFactory.createFitter();
    
    // DPS used to fit a hit's ADC samples.
    IDataPointSet adcDataPointSet;
    
    // Per channel histograms filled when doing the fit.
    Map<EcalChannel, IHistogram1D> signalNormHistograms = new HashMap<EcalChannel, IHistogram1D>();
    Map<EcalChannel, IHistogram1D> pedestalNormHistograms = new HashMap<EcalChannel, IHistogram1D>();
    Map<EcalChannel, IHistogram1D> signalSignificanceHistograms = new HashMap<EcalChannel, IHistogram1D>();
    
    // The function that will be used for the signal fit.
    IFunction fitFunction;
    
    // The output hits collection with the selected hits.
    String outputHitsCollectionName = "EcalCosmicReadoutHits";
    
    // The input hits collection with all the raw data hits.
    String inputHitsCollectionName = "EcalReadoutHits";
        
    HPSEcal3 ecal = null;
    static String ecalName = "Ecal";
    
    // The minimum number of required hits for event processing to continue.  
    int minimumHits = 3;
    
    // This determines whether the pedestal is fixed in the fit parameters.
    boolean fixPedestal = false;
    
    boolean printFits = false;
    
    // This is the required significance for signal hits (4 sigma default).
    //static double signalSignificanceThreshold = 4.0;
    
    double tightSignificanceThreshold = 3.0;
    double looseSignificanceThreshold = 2.0;
    
    String tightOutputHitsCollectionName = "Tight" + outputHitsCollectionName;
    String looseOutputHitsCollectionName = "Loose" + outputHitsCollectionName;
    
    // Global fit parameters for Moyal distribution.
    static double signalMean = 45.698; 
    static double signalSigma = 2.2216;
    
    // The initial value of the function normalization, which is not fixed in the fit.
    static double norm = 60.0;
    
    public void setTightSignificanceThreshold(double tightSignificanceThreshold) {
        this.tightSignificanceThreshold = tightSignificanceThreshold; 
    }
    
    public void setLooseSignificanceThreshold(double looseSignificanceThreshold) {
        this.looseSignificanceThreshold = looseSignificanceThreshold; 
    }
    
    public void printFits(boolean printFits) {
        this.printFits = printFits;
    }
    
    /**
     * Set the output hits collection name for the selected hits.
     * @param outputHitsCollectionName The output hits collection name.
     */
    public void setOutputHitsCollectionName(String outputHitsCollectionName) {
        this.outputHitsCollectionName = outputHitsCollectionName;
    }
    
    /**
     * Set the input RawTrackerHit collection name used for the hit selection.
     * @param inputHitsCollectionName The input hits collection name.
     */
    public void setInputHitsCollectionName(String inputHitsCollectionName) {
        this.inputHitsCollectionName = inputHitsCollectionName;
    }
        
    /**
     * Set the minimum number of required hits to continue processing this event.
     * By default this is 3 hits.
     * @param minimumHits The minimum number of hits.
     */
    public void setMinimumHits(int minimumHits) {
        this.minimumHits = minimumHits;
    }
    
    /**
     * Set whether the pedestal is fixed in the signal fit.  By default this is false.
     * @param fixPedestal True to fix the pedestal in the signal fit.
     */
    public void setFixPedestal(boolean fixPedestal) {
        this.fixPedestal = fixPedestal;
    }
    
    /**
     * Initialize conditions dependent class variables.
     * @param detector The current Detector object.
     */
    public void detectorChanged(Detector detector) {
        ecal = (HPSEcal3)detector.getSubdetector(ecalName);
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        channels = conditions.getChannelCollection();                
        for (EcalChannel channel : conditions.getChannelCollection()) {            
            signalNormHistograms.put(channel, aida.histogram1D(inputHitsCollectionName + "/Signal Norm : Channel " + String.format("%03d", channel.getChannelId()), 500, 0, 500.));
            pedestalNormHistograms.put(channel, aida.histogram1D(inputHitsCollectionName + "/Pedestal Norm : Channel " + String.format("%03d", channel.getChannelId()), 500, 0, 500.));
            signalSignificanceHistograms.put(channel, aida.histogram1D(inputHitsCollectionName + "/Signal Significance : Channel " + String.format("%03d", channel.getChannelId()), 200, -5., 35.));
        }        
    }
    
    /**
     * Perform start of job initialize.
     * The DataPointSet for the ADC values is initialized and global fit parameters are set here.
     */
    public void startOfData() {
        adcDataPointSet = aida.analysisFactory().createDataPointSetFactory(null).create("ADC DataPointSet", 2);
        
        fitFunction = new MoyalFitFunction();        
        fitFunction.setParameter("mpv", signalMean);
        fitFunction.setParameter("width", signalSigma);
        fitFunction.setParameter("norm", norm);                
        
        fitter.fitParameterSettings("mpv").setFixed(true);
        fitter.fitParameterSettings("width").setFixed(true);
        if (fixPedestal) {
            fitter.fitParameterSettings("pedestal").setFixed(true);
        }
    }

    /**
     * Process the event, performing a signal fit for every raw data hit in the input collection.
     * The hits that pass the sigma selection cut are added to a new hits collection, which can be
     * converted to a CalorimeterHit collection and then clustered.
     * @throw NextEventException if there are not enough hits that pass the selection cut.
     */
    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputHitsCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputHitsCollectionName);
            //List<RawTrackerHit> selectedHitsList = new ArrayList<RawTrackerHit>();
            List<RawTrackerHit> looseHitsList = new ArrayList<RawTrackerHit>();
            List<RawTrackerHit> tightHitsList = new ArrayList<RawTrackerHit>();
            
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                if (channel != null) {
                                        
                    EcalChannelConstants channelConstants = conditions.getChannelConstants(channel);
                    double noise = channelConstants.getCalibration().getNoise();                    
                    
                    // Clear the DPS from previous fit.
                    adcDataPointSet.clear();
                    
                    // Loop over all ADC values of the hit.
                    for (int adcSample = 0; adcSample < hit.getADCValues().length; adcSample++) {
                        // Insert a DP into the DPS for each sample. 
                        adcDataPointSet.addPoint();
                        
                        // Coordinate 1 is the ADC sample number.
                        adcDataPointSet.point(adcSample).coordinate(0).setValue(adcSample);
                        
                        // Coordinate 2 is the ADC sample value and its errors, which is set to the 
                        // noise from the EcalCalibration condition object for plus and minus.
                        adcDataPointSet.point(adcSample).coordinate(1).setValue(hit.getADCValues()[adcSample]);
                        adcDataPointSet.point(adcSample).coordinate(1).setErrorMinus(noise);
                        adcDataPointSet.point(adcSample).coordinate(1).setErrorPlus(noise);                                                
                    }
                    
                    // Fit the ADC signal.
                    IFitResult fitResult = fitAdcSamples(channel, adcDataPointSet);
                                     
                    if (printFits) {
                        ((FitResult)fitResult).printResult();
                    }
                     
                    // Calculate the signal significance which is norm over error.
                    double signalSignificance = fitResult.fittedParameter("norm") / fitResult.errors()[1];
                    
                    // Fill signal significance histogram.
                    this.signalSignificanceHistograms.get(channel).fill(signalSignificance);                    
                                        
                    // Is the significance over the threshold?
                    if (signalSignificance >= this.looseSignificanceThreshold) {
                        //System.out.println(fitResult.fittedParameter("norm") + " " + fitResult.errors()[1] + " " + signalSignificance);
                        // Add the hit to the output list.
                        looseHitsList.add(hit);
                        if (signalSignificance >= this.tightSignificanceThreshold) {
                            tightHitsList.add(hit);
                        }
                    }                   
                } else {                    
                    throw new RuntimeException("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
            
            // Is there at least one tight hit and at least 3 loose hits?
            if (!tightHitsList.isEmpty() && looseHitsList.size() >= this.minimumHits) {
                
                // Write loose hits list.
                event.put(tightOutputHitsCollectionName, tightHitsList, RawTrackerHit.class, event.getMetaData(hits).getFlags(), ecal.getReadout().getName());
                event.getMetaData(tightHitsList).setSubset(true);
                    
                // Write tight hits list.
                event.put(looseOutputHitsCollectionName, looseHitsList, RawTrackerHit.class, event.getMetaData(hits).getFlags(), ecal.getReadout().getName());
                event.getMetaData(looseHitsList).setSubset(true);
                
            } else {
                throw new NextEventException();
            }            
        }
    }
    
    /**
     * Fit all of the ADC samples in a hit using a DataPointSet and then return the fit result.
     * @param channel The ECAL channel conditions information.
     * @param adcDataPointSet The DataPointSet to use for the fit containing all 100 ADC samples.
     * @return The significance which is the normalization divided by its error.
     */
    IFitResult fitAdcSamples(EcalChannel channel, IDataPointSet adcDataPointSet) {
        EcalChannelConstants channelConstants = conditions.getChannelConstants(channel);
        fitFunction.setParameter("pedestal", channelConstants.getCalibration().getPedestal());
        IFitResult fitResult = fitter.fit(adcDataPointSet, fitFunction);
        this.signalNormHistograms.get(channel).fill(fitResult.fittedParameter("norm"));
        this.pedestalNormHistograms.get(channel).fill(fitResult.fittedParameter("pedestal"));
        return fitResult;
    }
    
    
}
