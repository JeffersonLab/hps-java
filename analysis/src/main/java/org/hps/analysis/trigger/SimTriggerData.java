package org.hps.analysis.trigger;

import org.hps.record.triggerbank.SSPCluster;
import org.lcsim.event.Cluster;

/**
 * Class <code>SimTriggerData</code> is a container class that holds
 * simulated trigger data modules. It is intended to be placed in the
 * LCIO data stream by the <code>DataTriggerSimDriver</code> to allow
 * other classes to access triggers simulated from hardware and software
 * cluster data.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class SimTriggerData {
    private final SimTriggerModule<Cluster> softwareClusterTriggers;
    private final SimTriggerModule<SSPCluster> hardwareClusterTriggers;
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object with empty
     * trigger results modules.
     */
    SimTriggerData() {
        softwareClusterTriggers = new SimTriggerModule<Cluster>();
        hardwareClusterTriggers = new SimTriggerModule<SSPCluster>();
    }
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object that will
     * contain the argument trigger modules.
     * @param softwareClusterTriggers - The module containing triggers
     * simulated from software simulated clusters.
     * @param hardwareClusterTriggers - The module containing triggers
     * simulated from hardware reported clusters.
     */
    SimTriggerData(SimTriggerModule<Cluster> softwareClusterTriggers, SimTriggerModule<SSPCluster> hardwareClusterTriggers) {
        this.softwareClusterTriggers = softwareClusterTriggers;
        this.hardwareClusterTriggers = hardwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all simulated SSP trigger data for
     * each of the four primary triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    @Deprecated
    public SimTriggerModule<SSPCluster> getSimSSPTriggers() {
        return hardwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all simulated LCSim trigger data for
     * each of the four primary triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    @Deprecated
    public SimTriggerModule<Cluster> getSimReconTriggers() {
        return softwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all triggers simulated from hardware
     * reported clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    public SimTriggerModule<SSPCluster> getSimHardwareClusterTriggers() {
        return hardwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all triggers simulated from software
     * simulated clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule</code>
     * object.
     */
    public SimTriggerModule<Cluster> getSimSoftwareClusterTriggers() {
        return softwareClusterTriggers;
    }
}