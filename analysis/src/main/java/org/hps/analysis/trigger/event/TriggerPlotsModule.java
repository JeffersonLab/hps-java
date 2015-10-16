package org.hps.analysis.trigger.event;

import org.hps.analysis.trigger.util.Trigger;
import org.hps.record.triggerbank.SSPCluster;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

/**
 * Class <code>TriggerPlotsModule</code> handles the plotting of singles
 * and pair trigger values.
 * 
 * @author Kyle McCarty
 */
public class TriggerPlotsModule {
	// Reference variables.
	private static final int RECON     = 0;
	private static final int SSP       = 1;
	private static final int ALL       = 0;
	private static final int MATCHED   = 1;
	private static final int FAILED    = 2;
	private static final int TRIGGERED = 3;
	private static final int NO_CUTS   = 4;
	
	// Class variables.
	private final double[] energySlopeParamF;
	private static final double MØLLER_SUM_THRESHOLD = 0.750;
	
	// Plots.
	private AIDA aida = AIDA.defaultInstance();
	private IHistogram1D[][][] singlesClusterEnergyPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] singlesHitCountPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] singlesTriggerTimePlot = new IHistogram1D[2][2][5];
	
	private IHistogram1D[][][] pairClusterEnergyPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairHitCountPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairTimePlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairSumPlot = new IHistogram1D[2][2][5];
    private IHistogram2D[][][] pairSumEnergiesPlot = new IHistogram2D[2][2][5];
	private IHistogram1D[][][] pairDiffPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairSlopePlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairCoplanarityPlot = new IHistogram1D[2][2][5];
	private IHistogram1D[][][] pairTriggerTimePlot = new IHistogram1D[2][2][5];
	
	private IHistogram1D[] møllerClusterEnergyPlot = new IHistogram1D[2];
	private IHistogram1D[] møllerHitCountPlot = new IHistogram1D[2];
	private IHistogram1D[] møllerTimePlot = new IHistogram1D[2];
	private IHistogram1D[] møllerSumPlot = new IHistogram1D[2];
    private IHistogram2D[] møllerSumEnergiesPlot = new IHistogram2D[2];
	private IHistogram1D[] møllerDiffPlot = new IHistogram1D[2];
	private IHistogram1D[] møllerSlopePlot = new IHistogram1D[2];
	private IHistogram1D[] møllerCoplanarityPlot = new IHistogram1D[2];
	private IHistogram1D[] møllerTriggerTimePlot = new IHistogram1D[2];
    private IHistogram2D[] møllerPositionPlot = new IHistogram2D[2];
	
	private IHistogram1D[] tridentClusterEnergyPlot = new IHistogram1D[2];
	private IHistogram1D[] tridentHitCountPlot = new IHistogram1D[2];
    private IHistogram2D[] tridentPositionPlot = new IHistogram2D[2];
	
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
		String[] resultType = { "All", "Matched", "Failed", "Triggered", "No Cuts" };
		
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
				for(int result = 0; result < 5; result++) {
					// Instantiate the singles trigger plots.
					singlesClusterEnergyPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Singles Cluster Energy (%s)",
							singlesDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					singlesHitCountPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Singles Hit Count (%s)",
							singlesDir, sourceType[source], resultType[result]), 9, 0.5, 9.5);
					singlesTriggerTimePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Singles Trigger Time (%s)",
							singlesDir, sourceType[source], resultType[result]), 100, 0, 400);
					
					// Instantiate the pair trigger plots.
					pairHitCountPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Hit Count (%s)",
							pairDir, sourceType[source], resultType[result]), 9, 0.5, 9.5);
					pairClusterEnergyPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Cluster Energy (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairTimePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Time Coincidence (%s)",
							pairDir, sourceType[source], resultType[result]), 8, 0, 32);
					pairSumPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Energy Sum (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
                                        pairSumEnergiesPlot[triggerNum][source][result] = aida.histogram2D(String.format("%s/%s/Pair 2D Energy Sum (%s)",
                                                        pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0, 300, 0.0, 3.0);
					pairDiffPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Energy Difference (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairSlopePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Energy Slope (%s)",
							pairDir, sourceType[source], resultType[result]), 300, 0.0, 3.0);
					pairCoplanarityPlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Coplanarity (%s)",
							pairDir, sourceType[source], resultType[result]), 180, 0, 180);
					pairTriggerTimePlot[triggerNum][source][result] = aida.histogram1D(String.format("%s/%s/Pair Trigger Time (%s)",
							pairDir, sourceType[source], resultType[result]), 100, 0, 400);
				}
			}
			
			// Instantiate the Møller plots.
			møllerHitCountPlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Hit Count",
					pairDir), 9, 0.5, 9.5);
			møllerClusterEnergyPlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Cluster Energy",
					pairDir), 300, 0.0, 3.0);
			møllerTimePlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Time Coincidence",
					pairDir), 8, 0, 32);
			møllerSumPlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Energy Sum",
					pairDir), 300, 0.0, 3.0);
			møllerSumEnergiesPlot[triggerNum] = aida.histogram2D(String.format("%s/Møller/Møller-like Pair 2D Energy Sum",
                    pairDir), 300, 0.0, 3.0, 300, 0.0, 3.0);
            møllerDiffPlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Energy Difference",
					pairDir), 300, 0.0, 3.0);
            møllerSlopePlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Energy Slope",
					pairDir), 300, 0.0, 3.0);
            møllerCoplanarityPlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Coplanarity",
					pairDir), 180, 0, 180);
            møllerTriggerTimePlot[triggerNum] = aida.histogram1D(String.format("%s/Møller/Møller-like Pair Trigger Time",
					pairDir), 100, 0, 400);
            møllerPositionPlot[triggerNum] = aida.histogram2D(String.format("%s/Møller/Møller-like Pair Position",
					pairDir), 46, -23, 23, 11, -5.5, 5.5);
            
            // Instantiate the trident plots.
            tridentHitCountPlot[triggerNum] = aida.histogram1D(String.format("%s/Trident/Trident-like Pair Hit Count",
            		singlesDir), 9, 0.5, 9.5);
			tridentClusterEnergyPlot[triggerNum] = aida.histogram1D(String.format("%s/Trident/Trident-like Pair Cluster Energy",
					singlesDir), 300, 0.0, 3.0);
            tridentPositionPlot[triggerNum] = aida.histogram2D(String.format("%s/Trident/Trident-like Pair Position",
            		singlesDir), 46, -23, 23, 11, -5.5, 5.5);
		}
	}
	
	/**
	 * Populates the "failed" plots of the appropriate type with the
	 * cut results from the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 */
	public void failedTrigger(Trigger<?> trigger) {
		processTrigger(trigger, FAILED);
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
	 * Populates the "triggered" plots of the appropriate type with the
	 * cut results from the argument trigger.
	 * @param trigger - The trigger from which to populate the plots.
	 */
	public void passedTrigger(Trigger<?> trigger) {
		processTrigger(trigger, TRIGGERED);
	}
	
	/**
	 * Indicates that a cluster was seen by a trigger and adds it to
	 * the "no cuts" plots.
	 * @param triggerNum - The number of the trigger.
	 * @param cluster - The cluster that was seen.
	 */
	public void sawCluster(int triggerNum, Cluster cluster) {
		processSingles(triggerNum, NO_CUTS, cluster);
	}
	
	/**
	 * Indicates that a cluster was seen by a trigger and adds it to
	 * the "no cuts" plots.
	 * @param triggerNum - The number of the trigger.
	 * @param cluster - The cluster that was seen.
	 */
	public void sawCluster(int triggerNum, SSPCluster cluster) {
		processSingles(triggerNum, NO_CUTS, cluster);
	}
	
	/**
	 * Indicates that a cluster pair was seen by a trigger and adds it
	 * to the "no cuts" plots.
	 * @param triggerNum - The number of the trigger.
	 * @param pair - The cluster pair that was seen.
	 */
	public void sawPair(int triggerNum, Cluster[] pair) {
		processPair(triggerNum, NO_CUTS, pair);
	}
	
	/**
	 * Indicates that a cluster pair was seen by a trigger and adds it
	 * to the "no cuts" plots.
	 * @param triggerNum - The number of the trigger.
	 * @param pair - The cluster pair that was seen.
	 */
	public void sawPair(int triggerNum, SSPCluster[] pair) {
		processPair(triggerNum, NO_CUTS, pair);
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
	 * Sets the energy slope conversion factor to be used to calculate
	 * the energy slope value for plots.
	 * @param triggerNum - The trigger for which the conversion factor
	 * should be used.
	 * @param value - The conversion factor in units of GeV/mm.
	 */
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
		singlesTriggerTimePlot[triggerNum][RECON][plotType].fill(cluster.getCalorimeterHits().get(0).getTime());
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
		singlesTriggerTimePlot[triggerNum][SSP][plotType].fill(cluster.getTime());
		
		// Check if this cluster is "trident-like."
		// TODO: Define "trident-like."
		boolean processTrident = false;
		
		// If this is a trident-like event, add it to the trident plots.
		if(processTrident) {
			tridentHitCountPlot[triggerNum].fill(TriggerModule.getValueClusterHitCount(cluster));
			tridentClusterEnergyPlot[triggerNum].fill(TriggerModule.getValueClusterTotalEnergy(cluster));
			tridentPositionPlot[triggerNum].fill(cluster.getXIndex() > 0 ? cluster.getXIndex() - 1 : cluster.getXIndex(), cluster.getYIndex());
		}
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
		pairTriggerTimePlot[triggerNum][RECON][plotType].fill(pair[0].getCalorimeterHits().get(0).getTime());
		pairTriggerTimePlot[triggerNum][RECON][plotType].fill(pair[1].getCalorimeterHits().get(0).getTime());
		
		// Fill the cluster pair plots.
		pairTimePlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueTimeCoincidence(pair));
		pairSumPlot[triggerNum][RECON][plotType].fill(TriggerModule.getValueEnergySum(pair));
                pairSumEnergiesPlot[triggerNum][RECON][plotType].fill(pair[0].getEnergy(), pair[1].getEnergy());
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
		pairTriggerTimePlot[triggerNum][SSP][plotType].fill(pair[0].getTime());
		pairTriggerTimePlot[triggerNum][SSP][plotType].fill(pair[1].getTime());
		
		// Fill the cluster pair plots.
		pairTimePlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueTimeCoincidence(pair));
		pairSumPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergySum(pair));
        pairSumEnergiesPlot[triggerNum][SSP][plotType].fill(pair[0].getEnergy(), pair[1].getEnergy());
		pairDiffPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergyDifference(pair));
		pairSlopePlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF[triggerNum]));
		pairCoplanarityPlot[triggerNum][SSP][plotType].fill(TriggerModule.getValueCoplanarity(pair));
		
		// Check if this pair is "Møller-like."
		boolean processMøller = TriggerModule.getValueEnergySum(pair) >= MØLLER_SUM_THRESHOLD;
		
		// If the pair is Møller-like, populate the Møller plots.
		if(processMøller) {
			// Fill the cluster singles plots.
			møllerHitCountPlot[triggerNum].fill(TriggerModule.getValueClusterHitCount(pair[0]));
			møllerHitCountPlot[triggerNum].fill(TriggerModule.getValueClusterHitCount(pair[1]));
			møllerClusterEnergyPlot[triggerNum].fill(TriggerModule.getValueClusterTotalEnergy(pair[0]));
			møllerClusterEnergyPlot[triggerNum].fill(TriggerModule.getValueClusterTotalEnergy(pair[1]));
			møllerTriggerTimePlot[triggerNum].fill(pair[0].getTime());
			møllerTriggerTimePlot[triggerNum].fill(pair[1].getTime());
			møllerPositionPlot[triggerNum].fill(pair[0].getXIndex() > 0 ? pair[0].getXIndex() - 1 : pair[0].getXIndex(), pair[0].getYIndex());
			møllerPositionPlot[triggerNum].fill(pair[1].getXIndex() > 0 ? pair[1].getXIndex() - 1 : pair[1].getXIndex(), pair[1].getYIndex());
			
			// Fill the cluster pair plots.
			møllerTimePlot[triggerNum].fill(TriggerModule.getValueTimeCoincidence(pair));
			møllerSumPlot[triggerNum].fill(TriggerModule.getValueEnergySum(pair));
			møllerSumEnergiesPlot[triggerNum].fill(pair[0].getEnergy(), pair[1].getEnergy());
	        møllerDiffPlot[triggerNum].fill(TriggerModule.getValueEnergyDifference(pair));
	        møllerSlopePlot[triggerNum].fill(TriggerModule.getValueEnergySlope(pair, energySlopeParamF[triggerNum]));
	        møllerCoplanarityPlot[triggerNum].fill(TriggerModule.getValueCoplanarity(pair));
		}
	}
}
