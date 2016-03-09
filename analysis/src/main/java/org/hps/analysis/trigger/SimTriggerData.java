package org.hps.analysis.trigger;

import org.hps.record.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

/**
 * Class <code>SimTriggerData</code> is a container class that holds
 * simulated trigger data modules. It is intended to be placed in the
 * LCIO data stream by the <code>DataTriggerSimDriver</code> to allow
 * other classes to access triggers simulated from SSP and reconstructed
 * cluster data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SimTriggerData {
    private final SimTriggerModule<Cluster> reconTriggers;
    private final SimTriggerModule<SSPCluster> sspTriggers;
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object with empty
     * trigger results modules.
     */
    SimTriggerData() {
        reconTriggers = new SimTriggerModule<Cluster>();
        sspTriggers = new SimTriggerModule<SSPCluster>();
    }
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object that will
     * contain the argument trigger modules.
     * @param reconTriggers - The simulated reconstructed cluster
     * triggers module.
     * @param sspTriggers - The simulated SSP cluster triggers module.
     */
    SimTriggerData(SimTriggerModule<Cluster> reconTriggers, SimTriggerModule<SSPCluster> sspTriggers) {
        this.reconTriggers = reconTriggers;
        this.sspTriggers = sspTriggers;
    }
    
    /**
     * Gets the module containing all simulated SSP trigger data for
     * each of the four primary triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    public SimTriggerModule<SSPCluster> getSimSSPTriggers() {
        return sspTriggers;
    }
    
    /**
     * Gets the module containing all simulated LCSim trigger data for
     * each of the four primary triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    public SimTriggerModule<Cluster> getSimReconTriggers() {
        return reconTriggers;
    }
}