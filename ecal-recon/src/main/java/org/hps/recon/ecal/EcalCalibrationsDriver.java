package org.hps.recon.ecal;

import hep.aida.IFitFactory;
import hep.aida.IFitResult;
import hep.aida.IFitter;
import hep.aida.IFunction;
import hep.aida.IFunctionFactory;
import hep.aida.IHistogram1D;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
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
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalCalibrationsDriver extends Driver {
    
    EcalConditions ecalConditions = null;
    DatabaseConditionsManager conditionsManager = null;
    AIDA aida = AIDA.defaultInstance();
    boolean loadCalibrations = false;
    Set<Integer> runs = new HashSet<Integer>();
    
    public void setLoadCalibrations(boolean loadCalibrations) {
        this.loadCalibrations = loadCalibrations;
    }
    
    public void detectorChanged(Detector detector) {
        conditionsManager = DatabaseConditionsManager.getInstance();
        ecalConditions = conditionsManager.getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        for (EcalChannel channel : ecalConditions.getChannelCollection().getObjects()) {
            aida.histogram1D("ECAL Channel " + channel.getChannelId(), 300, 0, 300.);
        }
    }
    
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
    
    public void endOfData() {
        
        if (runs.size() == 0) {
            throw new RuntimeException("There was no data processed.");
        }
        List<Integer> runList = new ArrayList<Integer>(runs);
        Collections.sort(runList);
        
        IFunctionFactory functionFactory = aida.analysisFactory().createFunctionFactory(null);
        IFitFactory fitFactory = aida.analysisFactory().createFitFactory();
        
        EcalCalibrationCollection calibrations = new EcalCalibrationCollection();
        TableMetaData tableMetaData = conditionsManager.findTableMetaData(TableConstants.ECAL_CALIBRATIONS);
        calibrations.setTableMetaData(tableMetaData);
        
        for (EcalChannel channel : ecalConditions.getChannelCollection().getObjects()) {
            IHistogram1D histogram = aida.histogram1D("ECAL Channel " + channel.getChannelId());
                                 
            IFunction function = functionFactory.createFunctionByName("Gaussian", "G");        
            IFitter fitter = fitFactory.createFitter("chi2", "jminuit");
            double[] parameters = new double[3];
            parameters[0] = histogram.maxBinHeight();
            parameters[1] = histogram.mean();
            parameters[2] = histogram.rms();
            function.setParameters(parameters);
            IFitResult fitResult = fitter.fit(histogram, function);
            int channelId = channel.getChannelId();
                        
            double mean = fitResult.fittedParameter("mean");
            double sigma = fitResult.fittedParameter("sigma");
            EcalCalibration calibration = new EcalCalibration(channelId, mean, sigma);
            try {
                calibrations.add(calibration);
            } catch (ConditionsObjectException e) {
                throw new RuntimeException(e);
            }
        } 
        System.out.println(calibrations.toString());
        if (loadCalibrations) {
            int collectionId = conditionsManager.getNextCollectionID(TableConstants.ECAL_CALIBRATIONS);
            try {
                calibrations.setCollectionId(collectionId);
                calibrations.insert();
                int runStart = runList.get(0);
                int runEnd = runList.get(runList.size() - 1);
                ConditionsRecord conditionsRecord = new ConditionsRecord(
                        calibrations.getCollectionId(), 
                        runStart, 
                        runEnd, 
                        TableConstants.ECAL_CALIBRATIONS,
                        TableConstants.ECAL_CALIBRATIONS, 
                        "Auto generated by EcalCalibrationsDriver.", 
                        "eng_run");
                conditionsRecord.setTableMetaData(conditionsManager.findTableMetaData(TableConstants.CONDITIONS_RECORD));
                conditionsRecord.insert();
            } catch (ConditionsObjectException | SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}