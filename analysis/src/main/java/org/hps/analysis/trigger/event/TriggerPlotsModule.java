package org.hps.analysis.trigger.event;

import org.hps.analysis.trigger.util.Trigger;
import org.hps.recon.ecal.triggerbank.SSPCluster;
import org.hps.recon.ecal.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;

/**
 * Class <code>TriggerPlotsModule</code> handles the plotting of singles
 * and pair trigger values.
 * 
 * @author Kyle McCarty
 */
public class TriggerPlotsModule {
	// Reference variables.
	private static final int RECON   = 0;
	private static final int SSP     = 1;
	private static final int ALL     = 0;
	private static final int MATCHED = 1;
	private static final int FAILED  = 2;
	
	// Class variables.
	private final double[] energySlopeParamF;
	
	// Plots.
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[][][] singlesClusterEnergyPlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] singlesHitCountPlot = new IHistogram1D[2][2][3];
	
	private IHistogram1D[][][] pairClusterEnergyPlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairHitCountPlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairTimePlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairSumPlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairDiffPlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairSlopePlot = new IHistogram1D[2][2][3];
	private IHistogram1D[][][] pairCoplanarityPlot = new IHistogram1D[2][2][3];
	
	/**
	 * Instantiates a new <code>TriggerPlotsModule</code> that will use
	 * the indicated values for the energy slope conversion factor when
	 * plotting energy slope values. Plots will be attached to the
	 * default AIDA instance.
	 * @param trigger0F - The energy slope conversion factor for the
	 * first trigger.
	 * @param trigger1F - The energy slope conversion factor for the
	 * second trigger.
	 */
	public TriggerPlotsModule(double trigger0F, double trigger1F) {
		// Store the energy slope parameter.
		energySlopeParamF = new double[2];
		energySlopeParamF[0] = trigger0F;
		energySlopeParamF[1] = trigger1F;
		
		// Define type string values.
		String[] sourceType = { "Recon", "SSP" };
		String[] resultType = { "All", "Matched", "Failed" };
		
		// Instantiate the trigger result plots for each trigger.
		for(int triggerNum = 0; triggerNum < 2; triggerNum++) {
			// Get the directory for the current triggers.
			String pairDir = "Pair Trigger " + triggerNum;
			String singlesDir = "Singles Trigger " + triggerNum;
			
			// Instantiate the trigger result plots for each type of
			// trigger source object.
			for(int source = 0; source < 2; source++) {
				// Instantiate the trigger result plots for each type
				// of trigger match result.
				for(int result = 0; result < 3; result++) {
					// Instantiate the singles trigger plots.
					singlesClusterEnergyPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Singles Hit Count (%s)",
							singlesDir, sourceType[source], resultType[result]), 9, 0.5, 9.5);
					singlesHitCountPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Singles Cluster Energy (%s)",
							singlesDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					
					// Instantiate the pair trigger plots.
					pairHitCountPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Hit Count (%s)",
							pairDir, sourceType[source], resultType[result]), 9, 0.5, 9.5);
					pairClusterEnergyPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Cluster Energy (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairTimePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Time Coincidence (%s)",
							pairDir, sourceType[source], resultType[result]), 8, 0, 32);
					pairSumPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Energy Sum (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairDiffPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Energy Difference (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairSlopePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Energy Slope (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairCoplanarityPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s Pair Coplanarity (%s)",
							pairDir, sourceType[source], resultType[result]), 180, 0, 180);
				}
			}
		}
	}
	
	/**
	 * Populates the "all" plots of the appropriate type with the cut
	 * results from the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 */
	public void sawTrigger(Trigger<?> trigger) {
		processTrigger(trigger, ALL);
	}
	
	/**
	 * Populates the "matched" plots of the appropriate type with the
	 * cut results from the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 */
	public void matchedTrigger(Trigger<?> trigger) {
		processTrigger(trigger, MATCHED);
	}
	
	/**
	 * Populates the "failed" plots of the appropriate type with the
	 * cut results from the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 */
	public void failedTrigger(Trigger<?> trigger) {
		processTrigger(trigger, FAILED);
	}
	
	public void setEnergySlopeParamF(int triggerNum, double value) {
		// Make sure that the trigger number is valid.
		if(triggerNum < 0 || triggerNum > 1) {
			throw new IllegalArgumentException(String.format("Trigger number %d is not valid.", triggerNum));
		}
		
		// Set the parameter.
		energySlopeParamF[triggerNum] = value;
	}
	
	/**
	 * Populates the indicated type of plots of the appropriate type
	 * for the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 * @param plotType - The type of plot to populate. This must be one
	 * of <code>ALL</code>, <code>MATCHED</code>, or <code>FAILED</code>.
	 */
	private void processTrigger(Trigger<?> trigger, int plotType) {
		// Get the trigger number and source.
		Object source = trigger.getTriggerSource();
		int triggerNum = trigger.getTriggerNumber();
		
		// Populate the plots using the appropriate method.
		if(source instanceof Cluster) {
			processSingles(triggerNum, plotType, (Cluster) source);
		}
		else if(source instanceof SSPCluster) {
			processSingles(triggerNum, plotType, (SSPCluster) source);
		}
		else if(source instanceof Cluster[]) {
			processPair(triggerNum, plotType, (Cluster[]) source);
		}
		else if(source instanceof SSPCluster[]) {
			processPair(triggerNum, plotType, (SSPCluster[]) source);
		}
		
		// If the trigger source is unsupported, produce an error.
		else {
			throw new IllegalArgumentException(String.format("Trigger source \"%s\" is not supported.", source.getClass().getSimpleName()));
		}
	}
	
	/**
	 * Populates the trigger singles plots for the indicated type for
	 * reconstructed clusters.
	 * @param triggerNum - The trigger number of the source trigger.
	 * @param plotType - The type of plot to populate. This must be one
	 * of <code>ALL</code>, <code>MATCHED</code>, or <code>FAILED</code>.
	 * @param pair - The triggering cluster.
	 */
	private void processSingles(int triggerNum, int plotType, Cluster cluster) {
		// Fill the cluster singles plots.
		singlesHitCountPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterHitCount(cluster));
		singlesClusterEnergyPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterTotalEnergy(cluster));
	}
	
	/**
	 * Populates the trigger singles plots for the indicated type for SSP
	 * clusters.
	 * @param triggerNum - The trigger number of the source trigger.
	 * @param plotType - The type of plot to populate. This must be one
	 * of <code>ALL</code>, <code>MATCHED</code>, or <code>FAILED</code>.
	 * @param pair - The triggering cluster.
	 */
	private void processSingles(int triggerNum, int plotType, SSPCluster cluster) {
		// Fill the cluster singles plots.
		singlesHitCountPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterHitCount(cluster));
		singlesClusterEnergyPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterTotalEnergy(cluster));
	}
	
	/**
	 * Populates the trigger pair plots for the indicated type for
	 * reconstructed cluster pairs.
	 * @param triggerNum - The trigger number of the source trigger.
	 * @param plotType - The type of plot to populate. This must be one
	 * of <code>ALL</code>, <code>MATCHED</code>, or <code>FAILED</code>.
	 * @param pair - The triggering pair.
	 */
	private void processPair(int triggerNum, int plotType, Cluster[] pair) {
		// Fill the cluster singles plots.
		pairHitCountPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterHitCount(pair[0]));
		pairHitCountPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterHitCount(pair[1]));
		pairClusterEnergyPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterTotalEnergy(pair[0]));
		pairClusterEnergyPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueClusterTotalEnergy(pair[1]));
		
		// Fill the cluster pair plots.
		pairTimePlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueTimeCoincidence(pair));
		pairSumPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueEnergySum(pair));
		pairDiffPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueEnergyDifference(pair));
		pairSlopePlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF[triggerNum]));
		pairCoplanarityPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueCoplanarity(pair));
	}
	
	/**
	 * Populates the trigger pair plots for the indicated type for SSP
	 * cluster pairs.
	 * @param triggerNum - The trigger number of the source trigger.
	 * @param plotType - The type of plot to populate. This must be one
	 * of <code>ALL</code>, <code>MATCHED</code>, or <code>FAILED</code>.
	 * @param pair - The triggering pair.
	 */
	private void processPair(int triggerNum, int plotType, SSPCluster[] pair) {
		// Fill the cluster singles plots.
		pairHitCountPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterHitCount(pair[0]));
		pairHitCountPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterHitCount(pair[1]));
		pairClusterEnergyPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterTotalEnergy(pair[0]));
		pairClusterEnergyPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueClusterTotalEnergy(pair[1]));
		
		// Fill the cluster pair plots.
		pairTimePlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueTimeCoincidence(pair));
		pairSumPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergySum(pair));
		pairDiffPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergyDifference(pair));
		pairSlopePlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF[triggerNum]));
		pairCoplanarityPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueCoplanarity(pair));
	}
}
