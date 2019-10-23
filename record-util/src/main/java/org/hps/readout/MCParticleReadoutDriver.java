package org.hps.readout;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.lcio.SIOMCParticle;

/**
 * <code>MCParticleReadoutDriver</code> handles SLIC objects in input
 * Monte Carlo files of type {@link org.lcsim.event.MCParticle
 * MCParticle}.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see org.hps.readout.SLICDataReadoutDriver
 */
public class MCParticleReadoutDriver extends SLICDataReadoutDriver<MCParticle> {
    /**
     * Instantiate an instance of {@link
     * org.hps.readout.SLICDataReadoutDriver SLICDataReadoutDriver}
     * for objects of type {@link org.lcsim.event.MCParticle
     * MCParticle}. These do not require any special LCIO flags.
     */
    public MCParticleReadoutDriver() {
        super(MCParticle.class);
        setCollectionName("MCParticle");
    }
    
    @Override
    public void process(EventHeader event) {
        // Get the MCParticles.
        List<MCParticle> particles = null;
        if(event.hasCollection(MCParticle.class, "MCParticle")) {
            particles = event.get(MCParticle.class, "MCParticle");
        } else {
            particles = new ArrayList<MCParticle>(0);
        }
        
        // Modify the particle times.
        if(!particles.isEmpty()) {
            double currentTime = ReadoutDataManager.getCurrentTime();
            for(MCParticle particle : particles) {
                if(SIOMCParticle.class.isAssignableFrom(particle.getClass())) {
                    SIOMCParticle baseParticle = SIOMCParticle.class.cast(particle);
                    baseParticle.setTime(particle.getProductionTime() + currentTime);
                } else {
                    throw new RuntimeException("Error: Saw unexpected MC particle class \"" + particle.getClass().getSimpleName() + "\".");
                }
            }
        }
        
        // Run the superclass method to store the particle collection
        // in the data manager.
        super.process(event);
    }
}