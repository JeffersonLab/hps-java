package org.hps.readout;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.hps.readout.util.collection.LCIOCollection;
import org.hps.readout.util.collection.TriggeredLCIOData;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;

/**
 * <code>SimCalorimeterHitReadoutDriver</code> handles SLIC objects
 * in input Monte Carlo files of type {@link
 * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.SLICDataReadoutDriver
 */
public class SimCalorimeterHitReadoutDriver extends SLICDataReadoutDriver<SimCalorimeterHit> {
    /**
     * Instantiate an instance of {@link
     * org.hps.readout.SLICDataReadoutDriver SLICDataReadoutDriver}
     * for objects of type {@link
     * org.lcsim.event.SimCalorimeterHit SimCalorimeterHit} and set
     * the appropriate LCIO flags.
     */
    public SimCalorimeterHitReadoutDriver() {
        super(SimCalorimeterHit.class, 0xe0000000);
    }
    
    @Override
    protected Collection<TriggeredLCIOData<?>> getOnTriggerData(double triggerTime) {
        // If hodoscope hits are not persisted, truth data doesn't
        // need to be written out.
        if(!isPersistent()) { return null; }
        
        // Get the truth hits in the indicated time range.
        Collection<SimCalorimeterHit> truthHits = ReadoutDataManager.getData(triggerTime - getReadoutWindowBefore(),
                triggerTime + getReadoutWindowAfter(), collectionName, SimCalorimeterHit.class);
        
        // MC particles need to be extracted from the truth hits
        // and included in the readout data to ensure that the
        // full truth chain is available.
        Set<MCParticle> truthParticles = new java.util.HashSet<MCParticle>();
        for(SimCalorimeterHit simHit : truthHits) {
            for(int i = 0; i < simHit.getMCParticleCount(); i++) {
                ReadoutDataManager.addParticleParents(simHit.getMCParticle(i), truthParticles);
            }
        }
        
        // Create the truth MC particle collection.
        LCIOCollection<MCParticle> truthParticleCollection = ReadoutDataManager.getCollectionParameters("MCParticle", MCParticle.class);
        TriggeredLCIOData<MCParticle> truthParticleData = new TriggeredLCIOData<MCParticle>(truthParticleCollection);
        truthParticleData.getData().addAll(truthParticles);
        
        // Create a list to store the output data.
        List<TriggeredLCIOData<?>> output = new java.util.ArrayList<TriggeredLCIOData<?>>(2);
        output.add(truthParticleData);
        
        // Return the result.
        return output;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return isPersistent() ? getReadoutWindowAfter() : 0;
    }
}