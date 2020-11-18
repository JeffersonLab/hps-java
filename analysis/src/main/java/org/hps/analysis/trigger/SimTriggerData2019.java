package org.hps.analysis.trigger;

import org.hps.record.triggerbank.VTPCluster;
import org.lcsim.event.Cluster;
import org.lcsim.event.CalorimeterHit;

/**
 * Class <code>SimTriggerData2019</code> is a container class that holds
 * simulated trigger data modules. It is intended to be placed in the
 * LCIO data stream by the <code>DataTriggerSimDriver2019</code> to allow
 * other classes to access triggers simulated from hardware and software
 * cluster data.
 * 
 * Class <code>SimTriggerData2019</code>  is developed based on Class <code>SimTriggerData</code> 
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */
public class SimTriggerData2019 {
    private final SimTriggerModule2019<Cluster, CalorimeterHit> softwareClusterTriggers;
    private final SimTriggerModule2019<VTPCluster, CalorimeterHit> hardwareClusterTriggers;
    
    /**
     * Instantiates a new <code>SimTriggerData</code> object with empty
     * trigger results modules.
     */
    SimTriggerData2019() {
        softwareClusterTriggers = new SimTriggerModule2019<Cluster, CalorimeterHit>();
        hardwareClusterTriggers = new SimTriggerModule2019<VTPCluster, CalorimeterHit>();
    }
    
    /**
     * Instantiates a new <code>SimTriggerData2019</code> object that will
     * contain the argument trigger modules.
     * @param softwareClusterTriggers - The module containing triggers
     * simulated from software simulated clusters.
     * @param hardwareClusterTriggers - The module containing triggers
     * simulated from hardware reported clusters.
     */
    SimTriggerData2019(SimTriggerModule2019<Cluster, CalorimeterHit> softwareClusterTriggers, SimTriggerModule2019<VTPCluster, CalorimeterHit> hardwareClusterTriggers) {
        this.softwareClusterTriggers = softwareClusterTriggers;
        this.hardwareClusterTriggers = hardwareClusterTriggers;
    }    
    
    /**
     * Gets the module containing all triggers simulated from hardware
     * reported clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule2019</code>
     * object.
     */
    public SimTriggerModule2019<VTPCluster, CalorimeterHit> getSimHardwareClusterTriggers() {
        return hardwareClusterTriggers;
    }
    
    /**
     * Gets the module containing all triggers simulated from software
     * simulated clusters for each of the four production triggers.
     * @return Returns the trigger data in a <code>SimTriggerModule2019</code>
     * object.
     */
    public SimTriggerModule2019<Cluster, CalorimeterHit> getSimSoftwareClusterTriggers() {
        return softwareClusterTriggers;
    }
}