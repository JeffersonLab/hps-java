package org.hps.recon.ecal;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hps.conditions.api.ConditionsObjectException;
import org.hps.conditions.api.ConditionsRecord;
import org.hps.conditions.database.DatabaseConditionsManager;
import org.hps.conditions.database.TableConstants;
import org.hps.conditions.database.TableMetaData;
import org.hps.conditions.ecal.EcalCalibration;
import org.hps.conditions.ecal.EcalCalibration.EcalCalibrationCollection;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver will generate a {@link org.hps.conditions.EcalCalibration} collection
 * from the ADC value distributions of raw ECAL data.  It may optionally insert this
 * information into the conditions database using the file's run number.
 * 
 * Currently, it uses every ADC value for the distribution, but filtering should probably
 * be added to exclude hits above a certain threshold.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibrationsDriver extends Driver {
    
    EcalConditions ecalConditions = null;
    DatabaseConditionsManager conditionsManager = null;
    AIDA aida = AIDA.defaultInstance();
    IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
    IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
    boolean loadCalibrations = false;
    boolean performFit = true;
    Integer runStart = null;
    Integer runEnd = null;
    File outputFile = null;
    Set<Integer> runs = new HashSet<Integer>();
    static DecimalFormat decimalFormat = new DecimalFormat("#.####");
    
    /**
     * Set whether to automatically load the conditions into the database.
     * @param loadCalibrations True to load conditions into the database.
     */
    public void setLoadCalibrations(boolean loadCalibrations) {
        this.loadCalibrations = loadCalibrations;
    }    
    
    /**
     * Set the start run number for the conditions record.
     * @param runStart The run start number.
     */
    public void setRunStart(int runStart) {
        if (runStart < 0) {
            throw new IllegalArgumentException("The runStart must be >= 0.");
        }
        this.runStart = runStart;
    }
    
    /**
     * Set the end run number for the conditions record.  
     * It must be >= the runEnd.
     * @param runStart The run start number.
     */
    public void setRunEnd(int runEnd) {
        if (runEnd < 0) {
            throw new IllegalArgumentException("The runEnd must be >= 0.");
        }
        this.runEnd = runEnd;
    }
    
    /**
     * Set whether to perform a function fit to get the mean and sigma values.
     * @param performFit True to perform a function fit of the histogram.
     */
    public void setPerformFit(boolean performFit) {
        this.performFit = performFit;
    }
    
    /**
     * Set an output file path for writing the calibration data.  
     * @param outputFileName The path to the output file.
     */
    public void setOutputFileName(String outputFileName) {
        if (outputFileName == null) {
            throw new IllegalArgumentException("The outputFileName argument is null.");
        }
        outputFile = new File(outputFileName);
    }
    
    /**
     * Start of job hook.
     * Check that the run numbers are valid.
     */
    public void startOfData() {
        if (runEnd < runStart) {
            throw new IllegalArgumentException("The runEnd must be >= runStart.");
        }
    }
        
    /**
     * Initialize this Driver when conditions change is triggered.
     */
    @Override
    public void detectorChanged(Detector detector) {        
        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        
        // Create a histogram for every ECAL channel.
        for (EcalChannel channel : ecalConditions.getChannelCollection()) {
            aida.histogram1D("ECAL Channel " + channel.getChannelId(), 300, 0, 300.);
        }
    }
    
    /**
     * Process the event data, filling the channel histograms from the ADC values.
     */
    @Override
    public void process(EventHeader event) {
        runs.add(event.getRunNumber());
        if (event.hasCollection(RawTrackerHit.class, "EcalReadoutHits")) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = ecalConditions.getChannelCollection().findGeometric(hit.getCellID());
                if (channel != null) {
                    int channelId = channel.getChannelId();
                    for (short adcValue : hit.getADCValues()) {
                        aida.histogram1D("ECAL Channel " + channelId).fill(adcValue);
                    }
                }
            }
        }
    }
    
    /**
     * End of data hook, which will get the mean and sigma for the ADC distributions.
     * It may optionally write these to a file and push the values into the database.
     */
    @Override
    public void endOfData() {
        
        if (runs.size() == 0) {
            throw new RuntimeException("There was no data processed.");
        }
        List<Integer> runList = new ArrayList<Integer>(runs);
        Collections.sort(runList);
        
        if (runStart == null) {
            runStart = runList.get(0);
        }
        
        if (runEnd == null) {
            runEnd = runList.get(runList.size() - 1);
        }
                
        EcalCalibrationCollection calibrations = new EcalCalibrationCollection();
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(TableConstants.ECAL_CALIBRATIONS);
        calibrations.setTableMetaData(tableMetaData);
        
        // Loop over all ECAL channels.
        for (EcalChannel channel : ecalConditions.getChannelCollection()) {
            
            // Get the histogram with ADC distribution for this channel. 
            IHistogram1D histogram = aida.histogram1D("ECAL Channel " + channel.getChannelId());
                                 
            int channelId = channel.getChannelId();
            
            double mean = 0;
            double sigma = 0;
            
            if (performFit) {
                // Perform a Gaussian fit and use its mean and sigma.
                IFitResult fitResult = doGaussianFit(histogram);
                mean = fitResult.fittedParameter("mean");
                sigma = fitResult.fittedParameter("sigma");
            } else {
                // Use the histogram's statistics for mean and sigma. 
                mean = histogram.mean();
                sigma = histogram.rms();
            }
            
            // Truncate to 4 decimal places.
            mean = Double.valueOf(decimalFormat.format(mean));
            sigma = Double.valueOf(decimalFormat.format(sigma));
            
            // Create a new calibration object and add it to the collection, using mean for pedestal
            // and sigma for noise.
            calibrations.add(new EcalCalibration(channelId, mean, sigma));
        } 
        
        // Get the list of field names for the header.
        StringBuffer buffer = new StringBuffer();
        for (String fieldName : tableMetaData.getFieldNames()) {
            buffer.append(fieldName + " ");
        }
        buffer.setLength(buffer.length() - 1);
        String fieldNames = buffer.toString();
        
        // Is there an output file for the calibration data?
        if (outputFile != null) {
            // Write the calibration data to an output text file.
            writeToFile(calibrations, fieldNames);
        } else {
            // Just print out the information to the console.
            System.out.println(fieldNames);
            System.out.println(calibrations.toString());
        }
        
        // Load the calibrations into the conditions database?
        if (loadCalibrations) {
            loadCalibrations(calibrations);
        }
    }

    /**
     * Write the calibration data to an output file.
     * @param calibrations The calibration data which is record delimited by new lines.
     * @param fieldNames The list of field names as a single string.
     */
    private void writeToFile(EcalCalibrationCollection calibrations, String fieldNames) {
        FileWriter writer = null;
        try {
            writer = new FileWriter(outputFile);
            writer.write(fieldNames);
            writer.write(calibrations.toString());
        } catch (IOException e) {
            throw new RuntimeException("There was a problem writing to the output file.", e);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Load the calibration data into the conditions database.
     * @param runList The list of runs processed in the job.
     * @param calibrations The collection of calibration objects.
     */
    private void loadCalibrations(EcalCalibrationCollection calibrations) {
        int collectionId = conditionsManager.getNextCollectionID(TableConstants.ECAL_CALIBRATIONS);
        try {
            calibrations.setCollectionId(collectionId);
            calibrations.insert();
            ConditionsRecord conditionsRecord = new ConditionsRecord(
                    calibrations.getCollectionId(), 
                    runStart, 
                    runEnd, 
                    TableConstants.ECAL_CALIBRATIONS,
                    TableConstants.ECAL_CALIBRATIONS, 
                    "Generated by EcalCalibrationsDriver.", 
                    "eng_run");
            conditionsRecord.setTableMetaData(conditionsManager.findTableMetaData(TableConstants.CONDITIONS_RECORD));
            conditionsRecord.insert();
        } catch (ConditionsObjectException | SQLException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * Do a Gaussian fit to calculate the mean and sigma of a histogram.
     * @param histogram The histogram for the ECAL channel. 
     * @return The fit result.
     */
    private IFitResult doGaussianFit(IHistogram1D histogram) {
        IFunction function = functionFactory.createFunctionByName("Gaussian", "G");        
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