package org.hps.users.jeremym;

import hep.aida.IAnalysisFactory;
import hep.aida.IProfile1D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
 * in a row that are above a sigma threshold.  For events with number
 * of hits greater than a minimum (5 by default), it will convert
 * the raw data into CalorimeterHits and write them to an output
 * collection.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalADCSignalPlotsDriver extends Driver {

    EcalConditions conditions = null;
    EcalChannelCollection channels = null;
    
    Map<EcalChannel, IProfile1D> signalProfiles = new HashMap<EcalChannel, IProfile1D>();
    Map<EcalChannel, IProfile1D> backgroundProfiles = new HashMap<EcalChannel, IProfile1D>();
    
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    double sigmaThreshold = 2.5;
    
    int minimumSelectedSamples = 3;
    int minimumNumberOfHits = 3;
    int minNeighbors = 2;
    String outputHitsCollectionName = null;
    String inputHitsCollectionName = "EcalReadoutHits";
    HPSEcal3 ecal = null;
    static String ecalName = "Ecal";

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
    public void setMinimumSelectedSamples(int minimumSelectedSamples) {
        this.minimumSelectedSamples = minimumSelectedSamples;
    }
    
    public void setMinimumNumberOfHits(int minimumNumberOfHits) {
        this.minimumNumberOfHits = minimumNumberOfHits;
    }
    
    public void setOutputHitsCollectionName(String outputHitsCollectionName) {
        this.outputHitsCollectionName = outputHitsCollectionName;
    }

    public void detectorChanged(Detector detector) {
        ecal = (HPSEcal3)detector.getSubdetector(ecalName);
        conditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();
        channels = conditions.getChannelCollection();
        for (EcalChannel channel : conditions.getChannelCollection()) {            
            signalProfiles.put(channel, aida.profile1D("Average Signal ADC Values : " + String.format("%03d", channel.getChannelId()), 100, 0, 100));
            backgroundProfiles.put(channel, aida.profile1D("Average Background ADC Values : " + String.format("%03d", channel.getChannelId()), 100, 0, 100));
        }
    }

    public void process(EventHeader event) {
        if (event.hasCollection(RawTrackerHit.class, inputHitsCollectionName)) {
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputHitsCollectionName);
            List<RawTrackerHit> selectedHitsList = new ArrayList<RawTrackerHit>();
            for (RawTrackerHit hit : hits) {
                EcalChannel channel = channels.findGeometric(hit.getCellID());
                if (channel != null) {
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
                            if (nSelectedHits >= minimumSelectedSamples) {
                                saveHit = true;
                                break adcThresholdLoop;
                            }
                        } else {
                            nSelectedHits = 0;
                        }                        
                    }
                    
                    // Pick the signal or background Profile1D based on the selection.
                    IProfile1D profile = null;
                    if (saveHit) {
                        profile = signalProfiles.get(channel);
                        selectedHitsList.add(hit);
                    } else {
                        profile = backgroundProfiles.get(channel);
                    }
                    
                    // Fill the Profile1D.
                    for (int adcIndex = 0; adcIndex < hit.getADCValues().length; adcIndex++) {
                        profile.fill(adcIndex, hit.getADCValues()[adcIndex]);
                    }                    
                } else {
                    System.err.println("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
                        
            if (outputHitsCollectionName != null) {     
                // Is number of hits above minimum?
                if (selectedHitsList.size() >= this.minimumNumberOfHits) {
                    // Save selected hits list to event.
                    getLogger().info("writing " + selectedHitsList.size() + " hits into " + outputHitsCollectionName);
                    event.put(outputHitsCollectionName, selectedHitsList, RawTrackerHit.class, event.getMetaData(hits).getFlags(), ecal.getReadout().getName());
                } else {
                    // Skip this event so LCIODriver does not write this event.
                    throw new NextEventException();
                }
            }                        
        }
    }

}
