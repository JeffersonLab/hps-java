package org.hps.readout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.LCIOCollectionFactory;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimTrackerHit;

/**
 * Class <code>SimTrackerHitReadoutDriver</code> provides a basic
 * framework for reading in <code>SimTrackerHit</code> from SLIC data
 * and feeding it to the <code>ReadoutDataManager</code>. It will
 * also include all truth particles associated with any truth hits
 * that are output, if it is set to persist its data.
 * 
 * @author Kyle McCarty
 */
public class SimTrackerHitReadoutDriver extends SLICDataReadoutDriver<SimTrackerHit> {
    public SimTrackerHitReadoutDriver() {
        super(SimTrackerHit.class, 0xc0000000);
    }
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // If the collection is not persistent, it does not need to
        // output additional data.
        if(!isPersistent()) { return super.getOnTriggerData(triggerTime); }
        
        // Get the truth hits in the indicated time range.
        Collection<SimTrackerHit> truthHits = ReadoutDataManager.getData(triggerTime - getReadoutWindowBefore(), triggerTime + getReadoutWindowAfter(), collectionName, SimTrackerHit.class);
        
        // MC particles need to be extracted from the truth hits
        // and included in the readout data to ensure that the
        // full truth chain is available.
        Set<MCParticle> truthParticles = new java.util.HashSet<MCParticle>();
        for(SimTrackerHit simHit : truthHits) {
            ReadoutDataManager.addParticleParents(simHit.getMCParticle(), truthParticles);
        }
        
        // Create the truth MC particle collection.
        LCIOCollectionFactory.setCollectionName("MCParticle");
        LCIOCollectionFactory.setProductionDriver(this);
        LCIOCollection<MCParticle> truthParticleCollection = LCIOCollectionFactory.produceLCIOCollection(MCParticle.class);
        TriggeredLCIOData<MCParticle> truthParticleData = new TriggeredLCIOData<MCParticle>(truthParticleCollection);
        truthParticleData.getData().addAll(truthParticles);
        
        // Create a general list for the collection.
        List<TriggeredLCIOData<?>> collectionsList = new ArrayList<TriggeredLCIOData<?>>(1);
        if(isPersistent()) { collectionsList.add(truthParticleData); }
        
        // Return the collections list result.
        return collectionsList;
    }
}