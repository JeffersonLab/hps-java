package org.hps.readout.trigger2019;

import java.util.Collection;
import java.util.List;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import org.hps.readout.ReadoutDataManager;
import org.hps.readout.TriggerDriver;
import org.hps.record.daqconfig2019.ConfigurationManager2019;
import org.hps.record.daqconfig2019.DAQConfig2019;
import org.hps.record.triggerbank.TriggerModule2019;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.aida.AIDA;

import org.hps.readout.util.HodoscopePattern;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * <code>SinglesTrigger2019ReadoutDriver</code> simulates an HPS singles trigger
 * for 2019 MC. It takes in clusters produced by the
 * {@link org.hps.readout.ecal.updated.GTPClusterReadoutDriver
 * GTPClusterReadoutDriver} and hodoscope patterns produced by the
 * {@link HodoscopePatternReadoutDriver}, and perform the necessary trigger
 * logic on them. If a trigger is detected, it is sent to the readout data
 * manager so that a triggered readout event may be written.
 */
public class SinglesTrigger2019ReadoutDriver extends TriggerDriver {     
    // ==============================================================
    // ==== LCIO Collections ========================================
    // ==============================================================
    /**
     * Indicates singles trigger type. Corresponding DAQ configuration is accessed by DAQ
     * configuration system, and applied into readout.
     */
    private String triggerType = "singles3";   
    
    /**
     * Indicates the name of the calorimeter geometry object. This is
     * needed to allow access to the calorimeter channel listings.
     */
    private String ecalGeometryName = "Ecal";
    /**
     * Specifies the name of the LCIO collection containing the input
     * GTP clusters that are used for triggering.
     */
    private String inputCollectionNameEcal = "EcalClustersGTP";
    
    private String inputCollectionNameHodo = "HodoscopePatterns";
    
    // ==============================================================
    // ==== Driver Options ==========================================
    // ==============================================================
    
    /**
     * Specifies the beam energy for the input data. This defines the
     * limits of the energy trigger plots and has no further effect.
     */
    private double beamEnergy = 4.55;
    /**
     * Stores the trigger settings and performs trigger logic.
     */
    private TriggerModule2019 triggerModule = new TriggerModule2019();
    
    // ==============================================================
    // ==== Driver Parameters =======================================
    // ==============================================================
    
    /**
     * Tracks the current local time in nanoseconds for this driver.
     */
    private double localTime = 0.0;
    /**
     * Stores a reference to the calorimeter subdetector model. This
     * is needed to extract the crystal indices from the cell ID.
     */
    private HPSEcal3 ecal = null;
    /**
     * Defines the size of an energy bin for trigger output plots.
     */
    private static final double BIN_SIZE = 0.025;      
    
    
    // ==============================================================
    // ==== AIDA Plots ==============================================
    // ==============================================================
    
    private AIDA aida = AIDA.defaultInstance();
    private static final int NO_CUTS = 0;
    private static final int WITH_CUTS = 1;
    private IHistogram1D[] clusterSeedEnergy = new IHistogram1D[2];
    private IHistogram1D[] clusterHitCount = new IHistogram1D[2];
    private IHistogram1D[] clusterTotalEnergy = new IHistogram1D[2];
    private IHistogram2D[] clusterDistribution = new IHistogram2D[2];
  
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
                    if(triggerType.contentEquals(SINGLES3)) triggerModule.loadDAQConfiguration(daq.getVTPConfig().getSingles3Config());
                    else if(triggerType.equals(SINGLES2)) triggerModule.loadDAQConfiguration(daq.getVTPConfig().getSingles2Config());
                    else if(triggerType.equals(SINGLES1)) triggerModule.loadDAQConfiguration(daq.getVTPConfig().getSingles1Config());
                    else if(triggerType.equals(SINGLES0)) triggerModule.loadDAQConfiguration(daq.getVTPConfig().getSingles0Config());
                }
            });
        }                               
    }
    
    @Override
    public void detectorChanged(Detector detector) {
        // Get the calorimeter sub-detector.
        org.lcsim.geometry.compact.Subdetector ecalSub = detector.getSubdetector(ecalGeometryName);
        if(ecalSub instanceof HPSEcal3) {
            ecal = (HPSEcal3) ecalSub;
        } else {
            throw new IllegalStateException("Error: Unexpected calorimeter sub-detector of type \"" + ecalSub.getClass().getSimpleName() + "; expected HPSEcal3.");
        }        
    }
    
    @Override
    public void process(EventHeader event) {                
        // Check that clusters are available for the trigger.
	//	System.out.println(this.getClass().getName()+"::  starting trigger determination");
        Collection<Cluster> clusters = null;
        Collection<HodoscopePattern> hodoPatterns = null;
        ArrayList<HodoscopePattern> hodoPatternList = null;
	if(doNoSpacing)
	    localTime=ReadoutDataManager.getCurrentTime(); // just overwrite local time on every event

	if(ReadoutDataManager.checkCollectionStatus(inputCollectionNameEcal, localTime) && ReadoutDataManager.checkCollectionStatus(inputCollectionNameHodo, localTime)) {
	    if(debug)	    System.out.println(this.getClass().getName()+":: checkCollectionStatus worked.  Getting collection in time window = ["+localTime+","+(localTime+4.0)+"]");
            clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionNameEcal, Cluster.class);
            hodoPatterns = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionNameHodo, HodoscopePattern.class);
	     if(debug) System.out.println(this.getClass().getName()+":: checkCollectionStatus worked Ecal size = "+clusters.size()+"  Hodo size = "+ hodoPatterns.size());
            
            localTime += 4.0;
            //this is backwards of what I wanted, but whatever...
	    //	    if(doNoSpacing&&(clusters.size() == 0 || hodoPatterns.size() == 0)) return;
	    //  if(!doNoSpacing&&(clusters.size() == 0 && hodoPatterns.size() == 0)) return;  
	    //	    if(doNoSpacing&&(clusters.size() == 0 || hodoPatterns.size() == 0)) return;

	    //just quit if 0 clusters.  
	    //	    if(clusters.size() == 0)
	    //	return; 
	    /*
	     *  I feel like this should be "and" as one of 
	     *  the triggers doesn't require hodo, right?
	     */
	    //this is the cut that's in master
	    if(clusters.size() == 0 || hodoPatterns.size() == 0) return;
            hodoPatternList = new ArrayList<>(hodoPatterns);
            
        } else {
	    if(debug)System.out.println(this.getClass().getName()+":: checkCollectionStatus did not find one of Ecal or Hodo at time = "+localTime);
	    return;
	}
        
        // Track whether or not a trigger was seen.
        boolean triggered = false;
        
        // There is no need to perform the trigger cuts if the
        // trigger is in dead time, as no trigger may be issued
        // regardless of the outcome.
        if(isInDeadTime()) {
	    if(debug)System.out.println(this.getClass().getName()+":: trigger is in dead-time!!!");
	    return;
	}
        
        // Record top/bot status for singles triggers
        List<String> topBot = new ArrayList();
        
        // Plot the trigger distributions before trigger cuts are
        // performed.
        for(Cluster cluster : clusters) {
            // Get the x and y indices. Note that LCSim meta data is
            // not available during readout, so crystal indices must
            // be obtained directly from the calorimeter geometry.
            java.awt.Point ixy = ecal.getCellIndices(cluster.getCalorimeterHits().get(0).getCellID());
	    System.out.println(this.getClass().getName()+
			       "::  looping over clusters; number of hits = "+TriggerModule2019.getClusterHitCount(cluster)
			       +"   seed energy value = " + TriggerModule2019.getValueClusterSeedEnergy(cluster)
			       +"   total energy of cluster = "+ TriggerModule2019.getValueClusterTotalEnergy(cluster));
            // Populate the uncut plots.
            clusterSeedEnergy[NO_CUTS].fill(TriggerModule2019.getValueClusterSeedEnergy(cluster));
            clusterTotalEnergy[NO_CUTS].fill(TriggerModule2019.getValueClusterTotalEnergy(cluster));
            clusterHitCount[NO_CUTS].fill(TriggerModule2019.getClusterHitCount(cluster));
            clusterDistribution[NO_CUTS].fill(ixy.x, ixy.y);           
            
            // Perform the hit count cut.
            if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_HIT_COUNT_LOW_EN) && !triggerModule.clusterHitCountCut(cluster)) {
		if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster hit cout (low)");
                continue;
            }            
            
            // Perform the cluster energy cut.
            if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW_EN) && !triggerModule.clusterTotalEnergyCutLow(cluster)) {
		if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster energy cut (low)");
                continue;
            }
            
            if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH_EN) && !triggerModule.clusterTotalEnergyCutHigh(cluster)) {
		if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster energy cout (high)");
                continue;
            }

	    System.out.println(this.getClass().getName()+":: made it past basic cluster cuts");
            
            // In the setup calorimeter geometry, range of X coordinates is [-23, -1] and [1, 23]. 
            // The hardware uses cluster X coordinates [-22,0] and [1,23].
            int clusterX = ixy.x;
            if(clusterX < 0) clusterX++;
            
            int clusterY = ixy.y;
            
            // XMin is at least 0.
            if(!triggerModule.getCutEn(TriggerModule2019.SINGLES_MOLLERMODE_EN)) {
                if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_XMIN_EN) && !triggerModule.clusterXMinCut(clusterX)) {
		    if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster x cut (low)");
                    continue;
                }
		System.out.println(this.getClass().getName()+":: made it past xMin cut ");
                // XMin cut has been applied.
                if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_PDE_EN) && !triggerModule.clusterPDECut(cluster, clusterX)) {
		    if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster PDE cut");
                    continue;                
                }
		System.out.println(this.getClass().getName()+":: made it past PDE cut ");
				
            }
            
	    //            if(triggerModule.getCutEn(TriggerModule2019.SINGLES_L1L2ECAL_MATCHING_EN) && !triggerModule.geometryMatchingCut(clusterX, ixy.y, hodoPatternList)) {
	    //put in check for hodoscope pattern collection size here
	    if(triggerModule.getCutEn(TriggerModule2019.SINGLES_L1L2ECAL_MATCHING_EN) && hodoPatterns.size()>0){
		if(!triggerModule.geometryMatchingCut(clusterX, ixy.y, hodoPatternList)) {
		    if(debug)System.out.println(this.getClass().getName()+"::  did not satisfy cluster-hodo matching cut");
		    continue;
		}
		System.out.println(this.getClass().getName()+":: made it past cluster-hodo matching cut ");

	    }
	    if(debug)System.out.println(this.getClass().getName()+":: made it through all non-moller cuts");
            //For 2021 update, Moller triggers
            if(triggerModule.getCutEn(TriggerModule2019.SINGLES_MOLLERMODE_EN)) {
                if(triggerModule.getCutEn(TriggerModule2019.SINGLES_XYMINMAX_EN) && !triggerModule.clusterXMinCut(clusterX)) {
                    continue;
                }
                if(triggerModule.getCutEn(TriggerModule2019.SINGLES_XYMINMAX_EN) && !triggerModule.clusterXMaxCut(clusterX)) {
                    continue;
                }            
                if(triggerModule.getCutEn(TriggerModule2019.SINGLES_XYMINMAX_EN) && !triggerModule.clusterYMinCut(clusterY)) {
                    continue;
                }
                if(triggerModule.getCutEn(TriggerModule2019.SINGLES_XYMINMAX_EN) && !triggerModule.clusterYMaxCut(clusterY)) {
                    continue;
                }
                if(triggerModule.getCutEn(TriggerModule2019.CLUSTER_PDE_EN) && !triggerModule.clusterMollerPDECut(cluster, clusterX)) {
                    continue;                
                }  
            }
            // Note that a trigger occurred.
            triggered = true;
	    if(debug)
		if(debug)System.out.println(this.getClass().getName()+"::  found a trigger!");

            if(ixy.y > 0) topBot.add(TOP);
            else topBot.add(BOT);
            
            // Populate the cut plots.
            clusterSeedEnergy[WITH_CUTS].fill(TriggerModule2019.getValueClusterSeedEnergy(cluster));
            clusterTotalEnergy[WITH_CUTS].fill(TriggerModule2019.getValueClusterTotalEnergy(cluster));
            clusterHitCount[WITH_CUTS].fill(TriggerModule2019.getClusterHitCount(cluster));
            clusterDistribution[WITH_CUTS].fill(ixy.x, ixy.y);
        }
        
        if(triggered) {
	    if(debug)System.out.println(this.getClass().getName()+"::   sending trigger!!!");
            boolean topStat = false;
            boolean botStat = false;            
            if(topBot.contains(TOP)) topStat = true;
            if(topBot.contains(BOT)) botStat = true;
            
            if(topStat && botStat) sendTrigger(triggerType, TOPBOT);
            else if(topStat) sendTrigger(triggerType, TOP);
            else sendTrigger(triggerType, BOT);            
        }
    }
    
    @Override
    public void startOfData() {         
        // Define the driver collection dependencies.
        addDependency(inputCollectionNameEcal);
        
        addDependency(inputCollectionNameHodo);
        
        // Register the trigger.
        ReadoutDataManager.registerTrigger(this);
        
        // Set the plot range based on the beam energy.
        int bins = (int) Math.ceil((beamEnergy * 1.1) / BIN_SIZE);
        double xMax = bins * BIN_SIZE;
        
        // Instantiate the trigger plots.
        String[] postscripts = { " (No Cuts)", " (With Cuts)" };
        for(int i = NO_CUTS; i <= WITH_CUTS; i++) {
            clusterSeedEnergy[i] = aida.histogram1D("Trigger Plots\\Cluster Seed Energy Distribution" + postscripts[i], bins, 0.0, xMax);
            clusterHitCount[i] = aida.histogram1D("Trigger Plots\\Cluster Hit Count Distribution" + postscripts[i], 10, -0.5, 9.5);
            clusterTotalEnergy[i] = aida.histogram1D("Trigger Plots\\Cluster Total Energy Distribution" + postscripts[i], bins, 0.0, xMax);
            clusterDistribution[i] = aida.histogram2D("Trigger Plots\\Cluster Seed Distribution" + postscripts[i], 46, -23, 23, 11, -5.5, 5.5);
        }
        
        // Run the superclass method.
        super.startOfData();
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
     * Defines the name of the calorimeter geometry specification. By
     * default, this is <code>"Ecal"</code>.
     * @param ecalName - The calorimeter name.
     */
    public void setEcalGeometryName(String value) {
        ecalGeometryName = value;
    }    
    
    /**
     * Sets the name of the LCIO collection from which clusters are
     * drawn.
     * @param collection - The name of the LCIO collection.
     */
    public void setInputCollectionNameEcal(String collection) {
        inputCollectionNameEcal = collection;
    }
    
    public void setInputCollectionNameHodo(String collection) {
        inputCollectionNameHodo = collection;
    }
    
    public void setTriggerType(String trigger) {
        if(!trigger.equals(SINGLES0) && !trigger.equals(SINGLES1) && !trigger.equals(SINGLES2) && !trigger.equals(SINGLES3))
            throw new IllegalArgumentException("Error: wrong trigger type name \"" + trigger + "\".");
        triggerType = trigger;
    }
    
    /**
     * Sets the beam energy for the trigger. This is only used to
     * determine the range of the x-axis for trigger plots.
     * @param value - The beam energy of the input data, in units of
     * GeV.
     */
    public void setBeamEnergy(double value) {
        beamEnergy = value;
    }
    
    /**
     * Sets the minimum hit count threshold for the trigger. This
     * value is inclusive.
     * @param hitCountThreshold - The value of the threshold.
     */
    public void setHitCountThreshold(int hitCountThreshold) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_HIT_COUNT_LOW, hitCountThreshold);
    }
    
    /**
     * Sets the lower bound for the cluster energy threshold on the
     * trigger. This value is inclusive.
     * @param clusterEnergyLow - The value of the threshold.
     */
    public void setClusterEnergyLowThreshold(double clusterEnergyLow) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_LOW, clusterEnergyLow);
    }
    
    /**
     * Sets the upper bound for the cluster energy threshold on the
     * trigger. This value is inclusive.
     * @param clusterEnergyHigh - The value of the threshold.
     */
    public void setClusterEnergyHighThreshold(double clusterEnergyHigh) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_TOTAL_ENERGY_HIGH, clusterEnergyHigh);
    }
    
    
    public void setClusterXMin(double xMin) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_XMIN, xMin);
    }
    
    public void setClusterPDEC0(double pdeC0) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_PDE_C0, pdeC0);
    }
    
    public void setClusterPDEC1(double pdeC1) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_PDE_C1, pdeC1);
    }
    
    public void setClusterPDEC2(double pdeC2) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_PDE_C2, pdeC2);
    }
    
    public void setClusterPDEC3(double pdeC3) {
        triggerModule.setCutValue(TriggerModule2019.CLUSTER_PDE_C3, pdeC3);
    }
            
}
