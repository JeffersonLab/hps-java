package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.List;

import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.SSPData;
import org.hps.record.triggerbank.TIData;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>HPSEcalDataPlotsDriver</code> creates plots from run
 * data for each trigger based on the active TI bits. Analogous plots
 * are also created for both the edge and fiducial regions of the
 * calorimeter face. Plots include all trigger distributions.<br/>
 * <br/>
 * Note that this driver does not function for Monte Carlo; it only
 * works on input that contains a TI bank collection.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class HPSEcalDataPlotsDriver extends Driver {
	private boolean useGoodSVT = false;
	private String plotsGroupName= "Data Plots";
	private String bankCollectionName = "TriggerBank";
	private String clusterCollectionName = "EcalClusters";
	
	private static final int PULSER   = 0;
	private static final int SINGLES0 = 1;
	private static final int SINGLES1 = 2;
	private static final int PAIR0    = 3;
	private static final int PAIR1    = 4;
	
	private static final int ALL       = 0;
	private static final int EDGE      = 1;
	private static final int FIDUCIAL  = 2;
	
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[][] clusterTotalEnergy = new IHistogram1D[5][3];
    private IHistogram1D[][] clusterTime = new IHistogram1D[5][3];
    private IHistogram1D[][] clusterHitCount = new IHistogram1D[5][3];
    private IHistogram1D[][] clusterSeedEnergy = new IHistogram1D[5][3];
    private IHistogram1D[][] pairEnergySum = new IHistogram1D[5][3];
    private IHistogram1D[][] pairEnergyDifference = new IHistogram1D[5][3];
    private IHistogram1D[][] pairEnergySlope = new IHistogram1D[5][3];
    private IHistogram1D[][] pairCoplanarity = new IHistogram1D[5][3];
    private IHistogram1D[][] pairTimeCoincidence = new IHistogram1D[5][3];

    private IHistogram2D[][] pairEnergySum2D = new IHistogram2D[5][3];
    private IHistogram2D[][] clusterSeedPosition = new IHistogram2D[5][3];
    private IHistogram2D[][] pairEnergySlope2D = new IHistogram2D[5][3];
    private IHistogram2D[][] pairCoplanarityEnergySum = new IHistogram2D[5][3];
    
    /**
     * Initializes the plots.
     */
    @Override
    public void startOfData() {
        // Define trigger names.
        String[] triggerNames = {
            "Pulser", "Singles 0", "Singles 1", "Pair 0", "Pair 1"  
        };
        
        // Define the positional names.
        String[] positionNames = {
                "All", "Edge", "Fiducial"
        };
        
        // Instantiate the plots.
        for(int i = 0; i < 5; i++) {
            for(int j = 0; j < 3; j++) {
                clusterTotalEnergy[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Cluster Total Energy", 150, 0.000, 1.500);
                clusterTime[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Cluster Hit Time", 100, 0.0, 100.0);
                clusterHitCount[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Cluster Hit Count", 10, -0.5, 9.5);
                clusterSeedEnergy[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Cluster Seed Energy", 150, 0.000, 1.500);
                pairEnergySum[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Energy Sum", 150, 0.000, 1.500);
                pairEnergyDifference[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Energy Difference", 150, 0.000, 1.500);
                pairEnergySlope[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Energy Slope", 100, 0.000, 4.000);
                pairCoplanarity[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Coplanarity", 180, 0.000, 180);
                pairTimeCoincidence[i][j] = aida.histogram1D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Time Coincidence", 80, 0, 20);
                
                clusterSeedPosition[i][j] = aida.histogram2D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Cluster Seed Position", 46, -23, 23, 11, -5.5, 5.5);
                pairEnergySum2D[i][j] = aida.histogram2D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Energy Sum 2D", 150, 0.000, 1.500, 150, 0.000, 1.500);
                pairCoplanarityEnergySum[i][j] = aida.histogram2D(plotsGroupName + "/" + triggerNames[i] + "/"
                        + positionNames[j] + "/Pair Energy Sum vs. Coplanarity", 150, 0.000, 1.500, 180, 0, 180);
                pairEnergySlope2D[i][j] = aida.histogram2D(plotsGroupName + "/" + triggerNames[i] + "/"
                		+ positionNames[j] + "/Pair Energy Slope 2D", 75, 0.000, 1.500, 100, 0.0, 400.0);
			}
		}
	}
	
	/**
	 * Processes the event clusters and populates distribution charts
	 * from them for each trigger. Also creates separate plots for the
	 * edge and fiducial regions.
	 * @param event - The event containing LCIO collections to be used
	 * for plot population.
	 */
	@Override
	public void process(EventHeader event) {
		// Check whether the SVT was active in this event.
		final String[] flagNames = { "svt_bias_good", "svt_burstmode_noise_good", "svt_position_good" };
		boolean svtGood = true;
        for(int i = 0; i < flagNames.length; i++) {
            int[] flag = event.getIntegerParameters().get(flagNames[i]);
            if(flag == null || flag[0] == 0) {
                svtGood = false;
            }
        }
		
        // If the SVT is not properly running, skip the event.
        if(!svtGood && useGoodSVT) { return; }
        
		// Get the TI and SSP banks.
		TIData tiBank = null;
		SSPData sspBank = null;
		if(event.hasCollection(GenericObject.class, bankCollectionName)) {
			// Get the bank list.
			List<GenericObject> bankList = event.get(GenericObject.class, bankCollectionName);
			
			// Search through the banks and get the SSP and TI banks.
			for(GenericObject obj : bankList) {
				// If this is an SSP bank, parse it.
				if(AbstractIntData.getTag(obj) == SSPData.BANK_TAG) {
					sspBank = new SSPData(obj);
				}
				
				// Otherwise, if this is a TI bank, parse it.
				else if(AbstractIntData.getTag(obj) == TIData.BANK_TAG) {
					tiBank = new TIData(obj);
				}
			}
		}
		
		// Get the list of clusters.
		List<Cluster> clusters = null;
		if(event.hasCollection(Cluster.class, clusterCollectionName)) {
			clusters = event.get(Cluster.class, clusterCollectionName);
		}
		
		// Require that all collections be initialized.
		if(sspBank == null || tiBank == null || clusters == null) {
			return;
		}
		
		// Track which triggers are active.
		boolean[] activeTrigger = new boolean[5];
		activeTrigger[PULSER] = tiBank.isPulserTrigger();
		activeTrigger[SINGLES0] = tiBank.isSingle0Trigger();
		activeTrigger[SINGLES1] = tiBank.isSingle1Trigger();
		activeTrigger[PAIR0] = tiBank.isPair0Trigger();
		activeTrigger[PAIR1] = tiBank.isPair1Trigger();
		
		// Plot all cluster properties for each trigger.
		for(Cluster cluster : clusters) {
			// Check whether the cluster is a fiducial or edge cluster.
			int positional = inFiducialRegion(cluster) ? FIDUCIAL : EDGE;
			
			// Fill the appropriate plots for each trigger with an
			// active trigger bit for single clusters.
			for(int i = 0; i < 5; i++) {
				if(activeTrigger[i]) {
					// Populate the ALL plots.
					clusterSeedEnergy[i][ALL].fill(TriggerModule.getValueClusterSeedEnergy(cluster));
					clusterTotalEnergy[i][ALL].fill(cluster.getEnergy());
					clusterHitCount[i][ALL].fill(TriggerModule.getClusterHitCount(cluster));
					clusterTime[i][ALL].fill(TriggerModule.getClusterTime(cluster));
					clusterSeedPosition[i][ALL].fill(TriggerModule.getClusterXIndex(cluster),
							TriggerModule.getClusterYIndex(cluster));
					
					// Populate the positional plots.
					clusterSeedEnergy[i][positional].fill(TriggerModule.getValueClusterSeedEnergy(cluster));
					clusterTotalEnergy[i][positional].fill(cluster.getEnergy());
					clusterHitCount[i][positional].fill(TriggerModule.getClusterHitCount(cluster));
					clusterTime[i][positional].fill(TriggerModule.getClusterTime(cluster));
					clusterSeedPosition[i][positional].fill(TriggerModule.getClusterXIndex(cluster),
							TriggerModule.getClusterYIndex(cluster));
				}
			}
		}
		
		// Plot all pair properties for each trigger.
		List<Cluster[]> pairs = TriggerModule.getTopBottomPairs(clusters, Cluster.class);
		for(Cluster[] pair : pairs) {
			// Check whether the cluster is a fiducial or edge cluster.
			boolean[] isFiducial = {
					inFiducialRegion(pair[0]),
					inFiducialRegion(pair[1])
			};
			int positional = (isFiducial[0] && isFiducial[1]) ? FIDUCIAL : EDGE;
			
			// Fill the appropriate plots for each trigger with an
			// active trigger bit for single clusters.
			for(int i = 0; i < 5; i++) {
				if(activeTrigger[i]) {
					// Calculate the values.
					double energySum = TriggerModule.getValueEnergySum(pair);
					double energyDiff = TriggerModule.getValueEnergyDifference(pair);
					double energySlope = TriggerModule.getValueEnergySlope(pair, 0.00550);
					double coplanarity = TriggerModule.getValueCoplanarity(pair);
					double timeCoincidence = TriggerModule.getValueTimeCoincidence(pair);
					
					// Get the energy slope values.
					Cluster lowCluster = pair[0].getEnergy() < pair[1].getEnergy() ? pair[0] : pair[1];
					double clusterDistance = TriggerModule.getClusterDistance(lowCluster);
					
					// Populate the ALL plots.
					pairEnergySum[i][ALL].fill(energySum);
					pairEnergyDifference[i][ALL].fill(energyDiff);
					pairEnergySlope[i][ALL].fill(energySlope);
					pairCoplanarity[i][ALL].fill(coplanarity);
					pairTimeCoincidence[i][ALL].fill(timeCoincidence);
					pairEnergySum2D[i][ALL].fill(pair[0].getEnergy(), pair[1].getEnergy());
					pairCoplanarityEnergySum[i][ALL].fill(energySum, coplanarity);
					pairEnergySlope2D[i][ALL].fill(lowCluster.getEnergy(), clusterDistance);
					
					// Populate the positional plots.
					pairEnergySum[i][positional].fill(energySum);
					pairEnergyDifference[i][positional].fill(energyDiff);
					pairEnergySlope[i][positional].fill(energySlope);
					pairCoplanarity[i][positional].fill(coplanarity);
					pairTimeCoincidence[i][positional].fill(timeCoincidence);
					pairEnergySum2D[i][positional].fill(pair[0].getEnergy(), pair[1].getEnergy());
					pairCoplanarityEnergySum[i][positional].fill(energySum, coplanarity);
					pairEnergySlope2D[i][positional].fill(lowCluster.getEnergy(), clusterDistance);
				}
			}
		}
	}
	
	/**
	 * Indicates whether the argument cluster is located in the fiducial
	 * region or not.
	 * @param cluster - The cluster to check.
	 * @return Returns <code>true</code> if the cluster is located in
	 * the fiducial region and <code>false</code> otherwise.
	 */
	private static final boolean inFiducialRegion(Cluster cluster) {
		// Get the x and y indices for the cluster.
		int ix   = TriggerModule.getClusterXIndex(cluster);
		int absx = Math.abs(TriggerModule.getClusterXIndex(cluster));
		int absy = Math.abs(TriggerModule.getClusterYIndex(cluster));
		
		// Check if the cluster is on the top or the bottom of the
		// calorimeter, as defined by |y| == 5. This is an edge cluster
		// and is not in the fiducial region.
		if(absy == 5) {
			return false;
		}
		
		// Check if the cluster is on the extreme left or right side
		// of the calorimeter, as defined by |x| == 23. This is also
		// and edge cluster is not in the fiducial region.
		if(absx == 23) {
			return false;
		}
		
		// Check if the cluster is along the beam gap, as defined by
		// |y| == 1. This is an internal edge cluster and is not in the
		// fiducial region.
		if(absy == 1) {
			return false;
		}
		
		// Lastly, check if the cluster falls along the beam hole, as
		// defined by clusters with -11 <= x <= -1 and |y| == 2. This
		// is not the fiducial region.
		if(absy == 2 && ix <= -1 && ix >= -11) {
			return false;
		}
		
		// If all checks fail, the cluster is in the fiducial region.
		return true;
	}
	
	/**
	 * Sets the name of the LCIO collection containing the clusters
	 * that are to be plotted.
	 * @param collection - The LCIO collection name.
	 */
	public void setClusterCollectionName(String collection) {
		clusterCollectionName = collection;
	}
	
	/**
	 * Defines the name of the LCIO collection containing the TI bank.
	 * @param collection - The LCIO collection name.
	 */
	public void setBankCollectionName(String collection) {
		bankCollectionName = collection;
	}
	
	/**
	 * Sets the name of the super-group folder containing all plots.
	 * @param name - The name of the plots folder.
	 */
	public void setPlotsGroupName(String name) {
		plotsGroupName = name;
	}
	
	/**
	 * Sets whether or not to skip events where the SVT was not in
	 * position and active.
	 * @param state - <code>true</code> indicates that only events
	 * with an active, properly positioned SVT should be analyzed, and
	 * <code>false</code> that all events will be included.
	 */
	public void setUseGoodSVT(boolean state) {
		useGoodSVT = state;
	}
}
