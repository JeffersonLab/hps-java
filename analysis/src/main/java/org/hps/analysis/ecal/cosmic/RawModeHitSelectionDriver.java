package org.hps.analysis.ecal.cosmic;

import hep.aida.IAnalysisFactory;

import java.util.ArrayList;
import java.util.List;

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
 * This Driver will process ECAL raw mode (window) data and extract hits 
 * that look like signal by using a simple selection cut.  The cut requires 
 * that a certain number of ADC samples in a row  are above a sigma threshold.  
 * Then those events that have at least a minimum number of hits that pass 
 * this cut will be written into a new collection.  If there are not enough
 * hits to pass this last cut, then the event is automatically skipped. 
 */
public class RawModeHitSelectionDriver extends Driver {

    EcalConditions conditions;
    EcalChannelCollection channels;
    
    HPSEcal3 ecal;
    static String ecalName = "Ecal";
    
    AIDA aida = AIDA.defaultInstance();
    IAnalysisFactory analysisFactory = aida.analysisFactory();
    
    double sigmaThreshold = 2.5;
    int minimumSelectedSamples = 3;
    int minimumNumberOfHits = 3;
    String outputHitsCollectionName = "EcalCosmicReadoutHits";
    String inputHitsCollectionName = "EcalReadoutHits";
           
    /**
     * Set the sigma threshold for an ADC value.
     * @param sigmaThreshold The sigma threshold.
     */
    public void setSigmaThreshold(double sigmaThreshold) {
        this.sigmaThreshold = sigmaThreshold;
    }

    /**
     * Set the number of ADC samples in a row which must be above the threshold.
     * @param minimumSelectedSamples The minimum number of samples above threshold.
     */
    public void setMinimumSelectedSamples(int minimumSelectedSamples) {
        this.minimumSelectedSamples = minimumSelectedSamples;
    }
    
    /**
     * Set the minimum number of hits for the event to pass selection cuts.
     * @param minimumNumberOfHits The minimum number of hits.
     */
    public void setMinimumNumberOfHits(int minimumNumberOfHits) {
        this.minimumNumberOfHits = minimumNumberOfHits;
    }
    
    /**
     * Set the name of the output hits collection.
     * @param outputHitsCollectionName The output hits collection name.
     */
    public void setOutputHitsCollectionName(String outputHitsCollectionName) {
        this.outputHitsCollectionName = outputHitsCollectionName;
    }

    /**
     * Initialize conditions dependent class variables.
     * @param detector The current Detector object.
     */
    public void detectorChanged(Detector detector) {
        ecal = (HPSEcal3)detector.getSubdetector(ecalName);
        conditions = DatabaseConditionsManager.getInstance().getEcalConditions();
        channels = conditions.getChannelCollection();
    }

    /**
     * Process the event, performing selection cuts on the collection of RawTrackerHits
     * that has a hit for every crystal.  Those events that don't have enough hits passing
     * the cuts are skipped.
     * @param event The LCIO event.
     */
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
                    
                    if (saveHit) {
                        // Add hit to output list.
                        selectedHitsList.add(hit);
                    }                   
                } else {
                    System.err.println("EcalChannel not found for cell ID 0x" + String.format("%08d", Long.toHexString(hit.getCellID())));
                }
            }
                             
            // Is number of hits above minimum?
            if (selectedHitsList.size() >= this.minimumNumberOfHits) {
                // Save selected hits list to event.
                //getLogger().info("writing " + selectedHitsList.size() + " hits into " + outputHitsCollectionName);
                event.put(outputHitsCollectionName, selectedHitsList, RawTrackerHit.class, event.getMetaData(hits).getFlags(), ecal.getReadout().getName());
            } else {
                // Skip this event so LCIODriver does not write this event.
                throw new NextEventException();
            }                                   
        }
    }
}
