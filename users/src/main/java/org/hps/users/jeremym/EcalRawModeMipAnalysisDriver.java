package org.hps.users.jeremym;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
import org.lcsim.detector.converter.compact.EcalCrystal;
import org.lcsim.event.EventHeader;
import org.lcsim.event.EventHeader.LCMetaData;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.Driver;

/**
 * This is an analysis of ECAL cosmics using raw mode (window) data
 * which has 100 samples of 4 ns.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EcalRawModeMipAnalysisDriver extends Driver {
    
    EcalConditions conditions;
    EcalChannelCollection channels;
    double sigmaThreshold = 3;
    String inputHitCollectionName = "EcalReadoutHits";
    String ecalName = "Ecal";
    
    int maxMipCandidates = Integer.MIN_VALUE;
    int nMipEvents = 0;
    HPSEcal3 ecal  = null;
    
    public void detectorChanged(Detector detector) {
        conditions = ConditionsManager.defaultInstance().getCachedConditions(EcalConditions.class, TableConstants.ECAL_CONDITIONS).getCachedData();        
        channels = conditions.getChannelCollection();
        ecal = (HPSEcal3)detector.getSubdetector(ecalName);
        if (ecal == null) {
            throw new RuntimeException("The ECAL subdetector has the wrong type: " + detector.getSubdetector(ecalName).getClass().getCanonicalName());
        }
    }
    
    public void setSigmaThreshold(double sigmaThreshold) {
        this.sigmaThreshold = sigmaThreshold;
    }
    
    public void process(EventHeader event) {
        
        if (event.hasCollection(RawTrackerHit.class, inputHitCollectionName)) {
            
            List<RawTrackerHit> hits = event.get(RawTrackerHit.class, "EcalReadoutHits");
            LCMetaData meta = event.getMetaData(hits);                        
            
            // Make a map of hits with at least one ADC value above threshold.
            Map<Long, RawTrackerHit> mipCandidateHits = createMipCandidateHits(hits);
            System.out.println("found " + mipCandidateHits.size() + " MIP candidate hits");
            
            // Filter the hit list based on whether there are neighboring hits.
            List<RawTrackerHit> mipHits = filterHits(mipCandidateHits);            
            System.out.println("filtered to " + mipHits.size() + " hits");
            
            if (mipCandidateHits.size() > 0) {
                ++nMipEvents;
            }
            if (mipCandidateHits.size() > maxMipCandidates) {
                maxMipCandidates = mipCandidateHits.size();                 
            }
            
            // Write hits to output collection.
            event.put(inputHitCollectionName + "_MipHits", mipHits, RawTrackerHit.class, meta.getFlags(), ecal.getReadout().getName());
        }        
    }
    
    public Map<Long, RawTrackerHit> createMipCandidateHits(List<RawTrackerHit> hitList) {
        Map<Long, RawTrackerHit> mipCandidateHits = new HashMap<Long, RawTrackerHit>();
        for (RawTrackerHit hit : hitList) {
            EcalChannel channel = channels.findGeometric(hit.getCellID());
            if (channel != null) {
                EcalChannelConstants channelConstants = conditions.getChannelConstants(channel);
                double pedestal = channelConstants.getCalibration().getPedestal();
                double noise = channelConstants.getCalibration().getNoise(); 
                adcLoop: for (short adcValue : hit.getADCValues()) {
                    if (adcValue - (pedestal + sigmaThreshold * noise) > 0) {
                        mipCandidateHits.put(hit.getCellID(), hit);
                        break adcLoop;
                    }
                }
            }
        }
        return mipCandidateHits;
    }
    
    public List<RawTrackerHit> filterHits(Map<Long, RawTrackerHit> mipCandidateHits) {
        List<RawTrackerHit> hitList = new ArrayList<RawTrackerHit>();
        for (Entry<Long, RawTrackerHit> entry : mipCandidateHits.entrySet()) {                        
            RawTrackerHit hit = entry.getValue();
            EcalCrystal crystal = (EcalCrystal)hit.getDetectorElement();
            if (crystal == null) {
                throw new RuntimeException("No crystal geometry object is assigned to hit.");
            }            
            Set<Long> neighborHitIDs = findNeighborHitIDs(hit, mipCandidateHits); 
            if (neighborHitIDs.size() > 0) {
                hitList.add(hit);
            }
        }
        return hitList;
    }
    
    Set<Long> findNeighborHitIDs(RawTrackerHit hit, Map<Long, RawTrackerHit> mipCandidateHits) {
        Set<Long> neigbhors = ecal.getNeighborMap().get(hit.getCellID());
        Set<Long> neighborHitIDs = new HashSet<Long>();
        for (long neighborID : neigbhors) {
            if (mipCandidateHits.containsKey(neighborID)) {
                neighborHitIDs.add(neighborID);
            }
        }
        return neighborHitIDs;
    }
    
    public void endOfData() {
        System.out.println("maxMipCandidates = " + maxMipCandidates + " @ " + sigmaThreshold + " sigma in " + nMipEvents + " with at least one MIP candidate");
    }
}
