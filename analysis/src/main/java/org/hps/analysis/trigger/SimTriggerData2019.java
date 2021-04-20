package org.hps.analysis.trigger;

import org.hps.record.triggerbank.VTPCluster;
import org.lcsim.event.Cluster;

/**
 * Class <code>SimTriggerData2019</code> is a container class that holds
 * simulated trigger data modules. It is intended to be placed in the
 * LCIO data stream by the <code>DataTriggerSimDriver2019</code> to allow
 * other classes to access triggers simulated from hardware and software
 * cluster data.
 * 
 * Class <code>SimTriggerData2019</code>  is developed based on Class <code>SimTriggerData</code> 
 * 
 */
public class SimTriggerData2019 {
    private final SimTriggerModule2019<Cluster> softwareClusterTriggers;
    private final SimTriggerModule2019<VTPCluster> hardwareClusterTriggers;
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object with empty
     * trigger results modules.
     */
    SimTriggerData2019() {
        softwareClusterTriggers = new SimTriggerModule2019<Cluster>();
        hardwareClusterTriggers = new SimTriggerModule2019<VTPCluster>();
    }
    
    /**
     * Instantiates a new <code>SimTriggerData2019</code> object that will
     * contain the argument trigger modules.
     * @param softwareClusterTriggers - The module containing triggers
     * simulated from software simulated clusters.
     * @param hardwareClusterTriggers - The module containing triggers
     * simulated from hardware reported clusters.
     */
    SimTriggerData2019(SimTriggerModule2019<Cluster> softwareClusterTriggers, SimTriggerModule2019<VTPCluster> hardwareClusterTriggers) {
        this.softwareClusterTriggers = softwareClusterTriggers;
        this.hardwareClusterTriggers = hardwareClusterTriggers;
    }    
    
    /**
     * Gets the module containing all triggers simulated from hardware
     * reported clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule2019</code>
     * object.
     */
    public SimTriggerModule2019<VTPCluster> getSimHardwareClusterTriggers() {
        return hardwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all triggers simulated from software
     * simulated clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule2019</code>
     * object.
     */
    public SimTriggerModule2019<Cluster> getSimSoftwareClusterTriggers() {
        return softwareClusterTriggers;
    }
}
