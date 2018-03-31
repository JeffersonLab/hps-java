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
    private StringBuffer outputBuffer = null;
    
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
            outputBuffer = new StringBuffer();
            outputBuffer.append("Processing MC Particles...\n");
            double currentTime = ReadoutDataManager.getCurrentTime();
            outputBuffer.append("Time Adjustment: " + currentTime + "\n");
            for(MCParticle particle : particles) {
                if(SIOMCParticle.class.isAssignableFrom(particle.getClass())) {
                    outputBuffer.append(String.format("\tParticle of type %d and charge %.0f created at time t = %5.3f with momentum p = <%5.3f, %5.3f, %5.3f>.%n",
                            particle.getPDGID(), particle.getCharge(), particle.getProductionTime(),
                            particle.getMomentum().x(), particle.getMomentum().y(), particle.getMomentum().z()));
                    outputBuffer.append(String.format("\t\tTime Shift: t = %5.3f >>>> ", particle.getProductionTime()));
                    SIOMCParticle baseParticle = SIOMCParticle.class.cast(particle);
                    baseParticle.setTime(particle.getProductionTime() + currentTime);
                    outputBuffer.append(String.format("t = %5.3f.%n", baseParticle.getProductionTime()));
                } else {
                    throw new RuntimeException("Error: Saw unexpected MC particle class \"" + particle.getClass().getSimpleName() + "\".");
                }
            }
        }
        
        // Run the superclass method to store the particle collection
        // in the data manager.
        super.process(event);
    }
    
    @Override
    protected void writeData(List<MCParticle> data) {
        if(outputBuffer != null) {
            writer.write(outputBuffer.toString());
            outputBuffer = null;
        }
    }
}