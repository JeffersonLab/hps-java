package org.hps.users.kmccarty;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import org.hps.recon.ecal.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.util.aida.AIDA;

public class TriggerPlotsModule {
	// Define plots.
	private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D singleSeedEnergy;
    private IHistogram1D singleHitCount;
    private IHistogram1D singleTotalEnergy;
    private IHistogram2D singleDistribution;
    
    private IHistogram1D pairSeedEnergy;
    private IHistogram1D pairHitCount;
    private IHistogram1D pairTotalEnergy;
    private IHistogram1D pairEnergySum;
    private IHistogram1D pairEnergyDifference;
    private IHistogram1D pairCoplanarity;
    private IHistogram1D pairEnergySlope;
    private IHistogram2D pairDistribution;
    private IHistogram2D pairEnergySum2D;
    
	public TriggerPlotsModule(String moduleName) {
		singleSeedEnergy     = aida.histogram1D(moduleName + " Trigger Plots/Singles Plots/Cluster Seed Energy",  176,   0.0,   1.1);
		singleHitCount       = aida.histogram1D(moduleName + " Trigger Plots/Singles Plots/Cluster Hit Count",      9,   0.5,   9.5);
		singleTotalEnergy    = aida.histogram1D(moduleName + " Trigger Plots/Singles Plots/Cluster Total Energy", 176,   0.0,   1.1);
		singleDistribution   = aida.histogram2D(moduleName + " Trigger Plots/Singles Plots/Cluster Seed",          46, -23.0,  23.0,  11, -5.5, 5.5);
		pairSeedEnergy       = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Cluster Seed Energy",     176,   0.0,   1.1);
		pairHitCount         = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Cluster Hit Count",         9,   0.5,   9.5);
		pairTotalEnergy      = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Cluster Total Energy",    176,   0.0,   1.1);
		pairEnergySum        = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Pair Energy Sum",         176,   0.0,   2.2);
		pairEnergyDifference = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Pair Energy Difference",  176,   0.0,   1.1);
		pairCoplanarity      = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Pair Coplanarity",        180,   0.0, 180.0);
		pairEnergySlope      = aida.histogram1D(moduleName + " Trigger Plots/Pair Plots/Pair Energy Slope",       200,   0.0,   3.0);
		pairDistribution     = aida.histogram2D(moduleName + " Trigger Plots/Pair Plots/Cluster Seed",             46, -23.0,  23.0,  11, -5.5, 5.5);
		pairEnergySum2D      = aida.histogram2D(moduleName + " Trigger Plots/Pair Plots/Pair Energy Sum 2D",      110,   0.0,   1.1, 110,  0.0, 1.1);
	}
	
	public void addCluster(Cluster cluster) {
		singleSeedEnergy.fill(TriggerModule.getValueClusterSeedEnergy(cluster));
		singleHitCount.fill(TriggerModule.getValueClusterHitCount(cluster));
		singleTotalEnergy.fill(TriggerModule.getValueClusterTotalEnergy(cluster));
		singleDistribution.fill(TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
	}
	
	public void addClusterPair(Cluster[] pair) {
		// Populate the singles plots.
		for(Cluster cluster : pair) {
			pairSeedEnergy.fill(TriggerModule.getValueClusterSeedEnergy(cluster));
			pairHitCount.fill(TriggerModule.getValueClusterHitCount(cluster));
			pairTotalEnergy.fill(TriggerModule.getValueClusterTotalEnergy(cluster));
			pairDistribution.fill(TriggerModule.getClusterXIndex(cluster), TriggerModule.getClusterYIndex(cluster));
		}
		
		// Populate the pair plots.
		pairEnergySum.fill(TriggerModule.getValueEnergySum(pair));
		pairEnergyDifference.fill(TriggerModule.getValueEnergyDifference(pair));
		pairCoplanarity.fill(TriggerModule.getValueCoplanarity(pair));
		pairEnergySlope.fill(TriggerModule.getValueEnergySlope(pair, 0.0055));
		pairEnergySum2D.fill(TriggerModule.getValueClusterTotalEnergy(pair[0]), TriggerModule.getValueClusterTotalEnergy(pair[1]));
	}
}