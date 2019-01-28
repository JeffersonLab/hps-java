package org.hps.readout.ecal.updated;

import java.util.Collection;

import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class SinglesTriggerReadoutDriver extends ReadoutDriver {
	// ==============================================================
	// ==== LCIO Collections ========================================
	// ==============================================================
	
	/**
	 * Indicates the name of the calorimeter geometry object. This is
	 * needed to allow access to the calorimeter channel listings.
	 */
	private String ecalGeometryName = "Ecal";
	/**
	 * Specifies the name of the LCIO collection containing the input
	 * GTP clusters that are used for triggering.
	 */
	private String inputCollectionName = "EcalClustersGTP";
	
	// ==============================================================
	// ==== Driver Options ==========================================
	// ==============================================================
	
	/**
	 * Specifies the beam energy for the input data. This defines the
	 * limits of the energy trigger plots and has no further effect.
	 */
	private double beamEnergy = 2.2;
	/**
	 * Stores the trigger settings and performs trigger logic.
	 */
	private TriggerModule triggerModule = new TriggerModule();
	
	// ==============================================================
	// ==== Driver Parameters =======================================
	// ==============================================================
	
	/**
	 * Tracks the current local time in nanoseconds for this driver.
	 */
	private double localTime = 2.0;
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
		System.out.println("New Trigger -- Event " + event.getEventNumber() +" -- Current Time is " + localTime);
		System.out.println("\tSaw Clusters:");
		
		// Check that clusters are available for the trigger.
		Collection<Cluster> clusters = null;
		if(ReadoutDataManager.checkCollectionStatus(inputCollectionName, localTime)) {
			clusters = ReadoutDataManager.getData(localTime, localTime + 4.0, inputCollectionName, Cluster.class);
			localTime += 4.0;
		} else {
			System.out.println("\t\tNone!");
			return;
		}
		
		for(Cluster cluster: clusters) {
			System.out.println("\t\tSaw cluster with energy " + cluster.getEnergy() + " at time " + TriggerModule.getClusterTime(cluster) + " with "
					+ TriggerModule.getClusterHitCount(cluster) + " hit on channel " + TriggerModule.getClusterSeedHit(cluster).getCellID() + ".");
		}
		
		// Track whether or not a trigger was seen.
		boolean triggered = false;
		
		// Plot the trigger distributions before trigger cuts are
		// performed.
		for(Cluster cluster : clusters) {
			// Get the x and y indices. Note that LCSim meta data is
			// not available during readout, so crystal indices must
			// be obtained directly from the calorimeter geometry.
			java.awt.Point ixy = ecal.getCellIndices(cluster.getCalorimeterHits().get(0).getCellID());
			
			// Populate the uncut plots.
			clusterSeedEnergy[NO_CUTS].fill(TriggerModule.getValueClusterSeedEnergy(cluster));
			clusterTotalEnergy[NO_CUTS].fill(TriggerModule.getValueClusterTotalEnergy(cluster));
			clusterHitCount[NO_CUTS].fill(TriggerModule.getClusterHitCount(cluster));
			clusterDistribution[NO_CUTS].fill(ixy.x, ixy.y);
			
			// Perform the hit count cut.
			if(!triggerModule.clusterHitCountCut(cluster)) {
				continue;
			}
			
			// Perform the seed hit cut.
			if(!triggerModule.clusterSeedEnergyCut(cluster)) {
				continue;
			}
			
			// Perform the cluster energy cut.
			if(!triggerModule.clusterTotalEnergyCut(cluster)) {
				continue;
			}
			
			// Note that a trigger occurred.
			triggered = true;
			
			// Populate the cut plots.
			clusterSeedEnergy[WITH_CUTS].fill(TriggerModule.getValueClusterSeedEnergy(cluster));
			clusterTotalEnergy[WITH_CUTS].fill(TriggerModule.getValueClusterTotalEnergy(cluster));
			clusterHitCount[WITH_CUTS].fill(TriggerModule.getClusterHitCount(cluster));
			clusterDistribution[WITH_CUTS].fill(ixy.x, ixy.y);
		}
		
		if(triggered) {
			System.out.println("\tTriggered!");
		}
	}
	
	@Override
	public void startOfData() {
		// Define the driver collection dependencies.
		addDependency(inputCollectionName);
		
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
	}
	
	@Override
	protected double getTimeDisplacement() {
		return 0;
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
	 * Sets the lower bound for the cluster energy threshold on the
	 * trigger. This value is inclusive.
	 * @param clusterEnergyLow - The value of the threshold.
	 */
	public void setClusterEnergyLowThreshold(double clusterEnergyLow) {
		triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_LOW, clusterEnergyLow);
	}
	
	/**
	 * Sets the upper bound for the cluster energy threshold on the
	 * trigger. This value is inclusive.
	 * @param clusterEnergyHigh - The value of the threshold.
	 */
	public void setClusterEnergyHighThreshold(double clusterEnergyHigh) {
		triggerModule.setCutValue(TriggerModule.CLUSTER_TOTAL_ENERGY_HIGH, clusterEnergyHigh);
	}
	
	/**
	 * Sets all cut values for the trigger using a string argument
	 * with the format "Emin Emax Nmin".
	 * @param cuts - The cut string.
	 */
	public void setCuts(String cuts) {
		triggerModule.setCutValues(true, cuts);
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
	 * Sets the minimum hit count threshold for the trigger. This
	 * value is inclusive.
	 * @param hitCountThreshold - The value of the threshold.
	 */
	public void setHitCountThreshold(int hitCountThreshold) {
		triggerModule.setCutValue(TriggerModule.CLUSTER_HIT_COUNT_LOW, hitCountThreshold);
	}
	
	/**
	 * Sets the name of the LCIO collection from which clusters are
	 * drawn.
	 * @param collection - The name of the LCIO collection.
	 */
	public void setInputCollectionName(String collection) {
		inputCollectionName = collection;
	}
	
	/**
	 * Sets the lower bound for the seed energy threshold on the
	 * trigger. This value is inclusive.
	 * @param seedEnergyLow - The value of the threshold.
	 */
	public void setSeedEnergyLowThreshold(double seedEnergyLow) {
		triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_LOW, seedEnergyLow);
	}
	
	/**
	 * Sets the upper bound for the seed energy threshold on the
	 * trigger. This value is inclusive.
	 * @param seedEnergyHigh - The value of the threshold.
	 */
	public void setSeedEnergyHighThreshold(double seedEnergyHigh) {
		triggerModule.setCutValue(TriggerModule.CLUSTER_SEED_ENERGY_HIGH, seedEnergyHigh);
	}
}
