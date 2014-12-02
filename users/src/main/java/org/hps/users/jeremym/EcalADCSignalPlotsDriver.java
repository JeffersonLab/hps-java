package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.database.TableConstants;
import org.hps.conditions.ecal.EcalChannel;
import org.hps.conditions.ecal.EcalChannel.EcalChannelCollection;
import org.hps.conditions.ecal.EcalChannelConstants;
import org.hps.conditions.ecal.EcalConditions;
import org.lcsim.conditions.ConditionsManager;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver will process ECAL raw mode (window) data and extract hits 
 * that look like signal, by requiring a certain number of ADC samples
 * in a row that are above a sigma threshold.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalADCSignalPlotsDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;

    Map<EcalChannel, Double[]> channelADCValues = new HashMap<EcalChannel, Double[]>();
    Map<EcalChannel, Integer> channelEventCounts = new HashMap<EcalChannel, Integer>();
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    double sigmaThreshold = 2.5;
    int minimumSelectedHits = 3;
    String outputHitsCollectionName = null;
    String inputHitsCollectionName = "EcalReadoutHits";
    HPSEcal3 ecal = null;
    String ecalName = "Ecal";

    /**
     * Set the sigma threshold for an ADC value.
     * @param sigmaThreshold The sigma threshold.
     */
    public void setSigmaThreshold(double sigmaThreshold) {
        this.sigmaThreshold = sigmaThreshold;
    }

    /**
     * Set the number of hits in a row which must be above threshold for the ADC values to be 
     * saved for the event.
     * @param selectedHits The minimum number of hits above threshold.
     */
    public void setMinimumHits(int selectedHits) {
        this.minimumSelectedHits = selectedHits;
    }
    
    public void setOutputHitsCollectionName(String outputHitsCollectionName) {
        this.outputHitsCollectionName = outputHitsCollectionName;
    }

    public void detectorChanged(Detector detector) {
        ecal = (HPSEcal3)detector.getSubdetector(ecalName);
        conditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {
            channelADCValues.put(channel, new Double[100]);
            channelEventCounts.put(channel, new Integer(0));            
            Double[] adcValues = channelADCValues.get(channel); 
            for (int adcSample = 0; adcSample < adcValues.length; adcSample++) {
                adcValues[adcSample] = new Double(0.0);                
            }
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputHitsCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputHitsCollectionName);
            List<RawTrackerHit> selectedHitsList = new ArrayList<RawTrackerHit>();
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                if (channel != null) {
                    Double[] adcValues = channelADCValues.get(channel);
                    if (adcValues == null) {
                        throw new RuntimeException("The ADC values array is null.");
                    }
                    int nSelectedHits = 0;
                    boolean saveHit = false;
                    EcalChannelConstants channelConstants = conditions.getChannelConstants(channel);
                    double pedestal = channelConstants.getCalibration().getPedestal();
                    double noise = channelConstants.getCalibration().getNoise();
                    double threshold = pedestal + sigmaThreshold * noise;
                    adcThresholdLoop: for (int adcIndex = 0; adcIndex < hit.getADCValues().length; adcIndex++) {
                        short adcValue = hit.getADCValues()[adcIndex];
                        if (adcValue > threshold) {
                            ++nSelectedHits;
                            if (nSelectedHits >= minimumSelectedHits) {
                                saveHit = true;
                                break adcThresholdLoop;
                            }
                        } else {
                            nSelectedHits = 0;
                        }                        
                    }
                    if (saveHit) {
                        for (int adcIndex = 0; adcIndex < hit.getADCValues().length; adcIndex++) {
                            adcValues[adcIndex] += hit.getADCValues()[adcIndex];
                        }
                        Integer nEvents = channelEventCounts.get(channel);
                        nEvents += 1;
                        channelEventCounts.put(channel, nEvents);
                        
                        // Add hit to selected hits list.
                        selectedHitsList.add(hit);
                    }
                } else {
                    System.err.println("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
            
            if (outputHitsCollectionName != null) {
                // Save selected hits list to event.
                event.put(outputHitsCollectionName, selectedHitsList, RawTrackerHit.class, event.getMetaData(hits).getFlags(), ecal.getReadout().getName());
            }
        }
    }

    public void endOfData() {
        for (EcalChannel channel : conditions.getChannelCollection()) {
            int nEvents = channelEventCounts.get(channel);
            IHistogram1D channelHistogram = aida.histogram1D("Average Signal ADC Values : " + channel.getChannelId(), 100, 0, 100);
            Double[] adcValues = channelADCValues.get(channel);
            for (int adcIndex = 0; adcIndex < adcValues.length; adcIndex++) {
                // Calculate the average ADC value across number of events processed for this channel.
                double averageAdcValue = adcValues[adcIndex] / nEvents;
                
                // Fill the ADC sample's bin, scaling by the ADC value.
                channelHistogram.fill(adcIndex, averageAdcValue);
            }
        }
    }
}
