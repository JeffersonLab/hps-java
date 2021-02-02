package org.hps.readout.trigger2019;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.hps.recon.ecal.EcalUtils;
import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.hps.record.triggerbank.TriggerModule2019;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

/**
 * <code>FEETrigger2019ReadoutDriver</code> simulates an HPS FEE trigger
 * for 2019 MC. It takes in clusters produced by the
 * {@link org.hps.readout.ecal.updated.GTPClusterReadoutDriver
 * GTPClusterReadoutDriver}, and perform the necessary trigger
 * logic on them. If a trigger is detected, it is sent to the readout data
 * manager so that a triggered readout event may be written.
 * 
 * Prescale for various regions are not set. 
 * 
 * @author Tongtong Cao <caot@jlab.org>
 */

public class FEETrigger2019ReadoutDriver extends TriggerDriver{

    // ==================================================================
    // ==== Trigger General Default Parameters ==========================
    // ==================================================================
    private String inputCollectionName = "EcalClustersGTP";       // Name for the LCIO cluster collection.
    private TriggerModule2019 triggerModule = new TriggerModule2019();
    
    // ==================================================================
    // ==== Driver Internal Variables ===================================
    // ==================================================================
    private double localTime = 0.0;                                // Stores the internal time clock for the driver.
        
    /**
     * Sets whether or not the DAQ configuration is applied into the driver
     * the EvIO data stream or whether to read the configuration from data files.
     * 
     * @param state - <code>true</code> indicates that the DAQ configuration is
     * applied into the readout system, and <code>false</code> that it
     * is not applied into the readout system.
     */
    public void setDaqConfigurationAppliedintoReadout(boolean state) {
        // If the DAQ configuration should be read, attach a listener
        // to track when it updates.   
        if (state) {
            ConfigurationManager2019.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    // Get the DAQ configuration.
                    DAQConfig2019 daq = ConfigurationManager2019.getInstance(); 
                    triggerModule.loadDAQConfiguration(daq.getVTPConfig().getFEEConfig()); 
                }
            });
        }         
    }
    
    @Override
    public void startOfData() {
        // Define the driver collection dependencies.
        addDependency(inputCollectionName);
        
        // Register the trigger.
        ReadoutDataManager.registerTrigger(this);
        
        // Make sure that a valid cluster collection name has been
        // defined. If it has not, throw an exception.
        if(inputCollectionName == null) {
            throw new RuntimeException("The parameter inputCollectionName was not set!");
        }       
        
        // Run the superclass method.
        super.startOfData();
    }
    
    @Override
    public void process(EventHeader event) {
        // If there is no data ready, then nothing can be done/
        if(!ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime)) {
            return;
        }
        
        // Otherwise, get the input clusters from the present time.
        Collection<Cluster> clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, Cluster.class); 
        
        // Check that if a trigger exists, if the trigger is not in
        // dead time. If it is, no trigger may be issued, so this is
        // not necessary.
        if(!isInDeadTime() && testTrigger(clusters)) { sendTrigger(); }
        
        // Increment the local time.
        localTime += 4.0;
    }
    
    @Override
    protected double getTimeDisplacement() {
        return 0;
    }
    
    @Override
    protected double getTimeNeededForLocalOutput() {
        return 0;
    }
    
    /**
     * Sets the name of the LCIO collection that contains the clusters.
     * @param clusterCollectionName - The cluster LCIO collection name.
     */
    public void setInputCollectionName(String clusterCollectionName) {
        inputCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets the minimum number of hits needed for a cluster to pass
     * the hit count single cluster cut.
     * @param minHitCount - The parameter value.
     */
    public void setMinHitCount(int minHitCount) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_HIT_COUNT_LOW, minHitCount);
    }
    
    /**
     * Sets the highest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyHigh - The parameter value.
     */
    public void setClusterEnergyHigh(double clusterEnergyHigh) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH, clusterEnergyHigh * EcalUtils.GeV);
    }
    
    /**
     * Sets the lowest allowed energy a cluster may have and still
     * pass the cluster total energy single cluster cut. Value uses
     * units of GeV.
     * @param clusterEnergyLow - The parameter value.
     */
    public void setClusterEnergyLow(double clusterEnergyLow) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW, clusterEnergyLow * EcalUtils.GeV);
    }
    
    private boolean testTrigger(Collection<Cluster> clusters) {  
        // Track whether a trigger has occurred.
        boolean triggered = false;
        
        // Sort through the cluster list and add clusters that pass
        // the single cluster cuts to the good list.
        clusterLoop:
        for(Cluster cluster : clusters) {
            // ==== Cluster Hit Count Cut ==================================
            // =============================================================
            // If the cluster fails the cut, skip to the next cluster.
            if(!triggerModule.clusterHitCountCut(cluster)) {
                continue clusterLoop;
            }
            
            // ==== Cluster Total Energy Cut ===============================
            // =============================================================
            // If the cluster fails the cut, skip to the next cluster.
            if(!triggerModule.clusterTotalEnergyCut(cluster)) {
                continue clusterLoop;
            }
            
            // Clusters that pass all of the pair cuts produce a trigger.
            triggered = true;
        }
        
        // Return the good clusters.
        return triggered;
    }
    
}
