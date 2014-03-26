package org.lcsim.hps.users.omoreno;

//--- java ---//
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.FindableTrack;
import org.hps.recon.tracking.apv25.Apv25Full;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;
//--- lcsim ---//
import org.lcsim.util.Driver;
//--- hps-java ---//

public class SimpleSvtTrigger extends Driver {
    
    FindableTrack findable = null;
    
    int eventNumber = 0;
    int numberOfSvtLayers = 12;
    int numberOfSvtLayersHit = 10;
    
    boolean debug = false;
    
    // Collection Names
    String simTrackerHitCollectionName = "TrackerHits";


    //--- Setters ---//
    //---------------//
    
    /**
     * Enable/disable debug.
     */
    public void setDebug(boolean debug){
        this.debug = debug; 
    }
    
    /**
     * Set the number of SVT Layers.
     */
    public void setNumberOfSvtLayers(int numberOfSvtLayers){
        this.numberOfSvtLayers = numberOfSvtLayers;
    }
    
    /**
     * Set the number of SVT Layers that an MC particle should hit.
     */
    public void setNumberOfSvtLayersHit(int numberOfSvtLayersHit){
        this.numberOfSvtLayersHit = numberOfSvtLayersHit;
    }

    /**
     * Set the SimTrackerHit collection name.
     */
    public void setSimTrackerHitCollectionName(String simTrackerHitCollectionName){
        this.simTrackerHitCollectionName = simTrackerHitCollectionName;
    }
    
    /**
     * Dflt Ctor.
     */
    public SimpleSvtTrigger(){ 
    }
    
    /**
     *
     */
    public SimpleSvtTrigger(int numberOfSvtLayers, int numberOfSvtLayersHit){
        this.numberOfSvtLayers = numberOfSvtLayers;
        this.numberOfSvtLayersHit = numberOfSvtLayersHit;
    }

    /**
     * 
     */
    protected void process(EventHeader event){
        
        eventNumber++;
        
        // If the event doesn't contain SimTrackerHits; skip the event
        if(!event.hasCollection(SimTrackerHit.class, simTrackerHitCollectionName)){
            this.printDebug("The event does not contain the collection " + simTrackerHitCollectionName);
            return;
        }
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, simTrackerHitCollectionName);
        this.printDebug("The collection " + simTrackerHitCollectionName + " contains " + simHits.size() + " SimTrackerHits");
        this.printDebug("The total number of SimTrackerHit collections " + event.get(SimTrackerHit.class).size());

        // Get the MC Particles associated with the SimTrackerHits
        List<MCParticle> mcParticles = event.getMCParticles();
        if(debug){
            String particleList = "[ ";
            for(MCParticle mcParticle : mcParticles){
                particleList += mcParticle.getPDGID() + ", ";
            }
            particleList += "]";
            this.printDebug("MC Particles: " + particleList);
        }
        
        // Check if the MC particle track should be found by the tracking algorithm
        findable = new FindableTrack(event, simHits, numberOfSvtLayers);
        
        // Use an iterator to avoid ConcurrentModificationException
        Iterator<MCParticle> mcParticleIterator = mcParticles.iterator();
        while(mcParticleIterator.hasNext()){
            MCParticle mcParticle = mcParticleIterator.next();
            if(findable.isTrackFindable(mcParticle, numberOfSvtLayersHit)){
                
                // Check that all SimTrackerHits are within the same detector volume
                Set<SimTrackerHit> trackerHits = findable.getSimTrackerHits(mcParticle);
                if(this.isSameSvtVolume(trackerHits)){
                    // If all hits lie within the same detector volume, then trigger the SVT
                    // This is a redundant check but better safe than sorry
                    this.printDebug("The SVT has been triggered on event: " + eventNumber);
                    Apv25Full.readoutBit = true;
                    break;
                }
            } else { 
                mcParticleIterator.remove();
            }
        }
    }
    
    /**
     * Check that all SVT hits are within the same detector volume (either top or bottom)
     * @param simTrackerHits
     * @return true if the hits are within the same detector volume, false otherwise
     */
    private boolean isSameSvtVolume(Set<SimTrackerHit> simTrackerHits){
        int volumeIndex = 0;
        for(SimTrackerHit simTrackerHit : simTrackerHits){
            if(SvtUtils.getInstance().isTopLayer((SiSensor) simTrackerHit.getDetectorElement())) volumeIndex++;
            else volumeIndex--;
        }
        return Math.abs(volumeIndex) == simTrackerHits.size();
    }
    
    /**
     * print debug statements
     */
    public void printDebug(String debugStatement){
        if(!debug) return;
        System.out.println(this.getClass().getSimpleName() + ": " + debugStatement);     
    }
    
}
