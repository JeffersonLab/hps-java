package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math.stat.StatUtils;
import org.hps.readout.ecal.SSPTriggerLogic;
import org.hps.recon.ecal.ECalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * The driver <code>EcalCluster</code> implements the histograms shown
 * to the user in the third tab of the Monitoring Application, when using
 * the Ecal monitoring application.<br/>
 * <br/>
 * These histograms show the single-channel distributions:
 * <ul>
 * <li>The first sub-tab shows the cluster counts per event (Histogram1D),
 * the number of hits in a cluster (Histogram1D), the cluster centers
 * distribution (Histogram2D), and the maximum cluster energy in an
 * event (Histogram1D).</li>
 * <li>The second sub-tab shows the energy distribution of the cluster
 * (Histogram1D) and the maximum cluster energy in each event
 * (Histogram1D)</li>
 * <li>The third sub-tab shows the time distribution of the cluster
 * (Histogram1D), taken from the mean of the times forming the cluster,
 * as well as the RMS (Histogram1D).</li>
 * <li>The fourth sub-tab is a larger version of the the cluster centers
 * distribution.</li>
 * <li>The fifth tab displays the cluster pair distribution for the
 * energy sum, energy difference, energy slope, and coplanarity cuts
 * for all top/bottom pairs received. It also displays the average x-
 * and y-coordinates for the pairs.</li>
 * </ul>
 * All histograms are updated continuously.<br/>
 * <br/>
 * The cluster center is calculated from the average of the positions
 * of the constituent hits, weighted by the hit energies.<br/>
 * <br/>
 * @author Andrea Celentano <andrea.celentano@ge.infn.it>
 * @author Kyle McCarty <mccarty@jlab.org>
 */
public class EcalClusterPlots extends Driver {
	// Internal variables.
    private boolean hide = false;
    private boolean logScale = false;
	private AIDA aida = AIDA.defaultInstance();
    private double maxE = 5000 * ECalUtils.MeV;
	private IPlotter[] plotter = new IPlotter[5];
	private String clusterCollectionName = "EcalClusters";
	
	// Monitoring plot variables.
    private IHistogram1D clusterCountPlot;
    private IHistogram1D clusterSizePlot;
    private IHistogram1D clusterEnergyPlot;
    private IHistogram1D clusterMaxEnergyPlot;
    private IHistogram1D clusterTimes;
    private IHistogram1D clusterTimeSigma;
    private IHistogram2D edgePlot;
    
    private IHistogram1D pairEnergySum;
    private IHistogram1D pairEnergyDifference;
    private IHistogram1D pairCoplanarity;
    private IHistogram1D pairEnergySlope;
    private IHistogram1D pairEnergyPositionMeanX;
    private IHistogram1D pairEnergyPositionMeanY;
    
    // Class variables.
    private static final int TAB_CLUSTER_COUNT = 0;
    private static final int TAB_CLUSTER_ENERGY = 1;
    private static final int TAB_CLUSTER_TIME = 2;
    private static final int TAB_CLUSTER_CENTER = 3;
    private static final int TAB_CLUSTER_PAIR = 4;
    private static final String[] TAB_NAMES = { "Cluster Count Plots", "Cluster Energy Plots",
    	"Cluster Time Plots", "Cluster Center Plot", "Cluster Pair Plots" };
    
    /**
     * Resets all of the plots for the new detector.
     * @param detector - The <code>Detector</code> object representing
     * the new detector.
     */
    @Override
    protected void detectorChanged(Detector detector) {
        // Re-instantiate all of the histograms.
        aida.tree().cd("/");
        clusterCountPlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Cluster Count per Event", 10, -0.5, 9.5);
        clusterSizePlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Cluster Size", 10, -0.5, 9.5);
        clusterEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Cluster Energy", 100, -0.1, maxE);
        clusterMaxEnergyPlot = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Maximum Cluster Energy in Event", 100, -0.1, maxE);
        edgePlot = aida.histogram2D(detector.getDetectorName() + " : " + clusterCollectionName + " : Weighted Cluster", 47, -23.25, 23.25, 11, -5.25, 5.25);
        clusterTimes = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Cluster Time Mean", 400, 0, 4.0 * 100);
        clusterTimeSigma = aida.histogram1D(detector.getDetectorName() + " : " + clusterCollectionName + " : Cluster Time Sigma", 100, 0, 40);
        
        pairEnergySum = aida.histogram1D("Pair Energy Sum Distribution", 176, 0.0, maxE);
        pairEnergyDifference = aida.histogram1D("Pair Energy Difference Distribution", 176, 0.0, 2.2);
        pairCoplanarity = aida.histogram1D("Pair Coplanarity Distribution", 90, 0.0, 180.0);
        pairEnergySlope = aida.histogram1D("Pair Energy Slope Distribution", 150, 0.0, 3.0);
        pairEnergyPositionMeanX = aida.histogram1D("Cluster Pair Weighted Energy Position (x-Index)", 100, -250, 250);
        pairEnergyPositionMeanY = aida.histogram1D("Cluster Pair Weighted Energy Position (y-Index)", 100, -100, 100);
        
        // Setup the plotter factory.
        IPlotterFactory plotterFactory = aida.analysisFactory().createPlotterFactory("Ecal Cluster Plots");
        
        // Apply formatting that is constant across all tabs.
        for(int tabIndex = 0; tabIndex < plotter.length; tabIndex++) {
        	plotter[tabIndex] = plotterFactory.create(TAB_NAMES[tabIndex]);
        	plotter[tabIndex].setTitle(TAB_NAMES[tabIndex]);
        	plotter[tabIndex].style().dataStyle().errorBarStyle().setVisible(false);
        	plotter[tabIndex].style().dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
            if(logScale) { plotter[tabIndex].style().yAxisStyle().setParameter("scale", "log"); }
        }
        
        // Define the Cluster Counts tab.
        plotter[TAB_CLUSTER_COUNT].createRegions(2, 2);
        plotter[TAB_CLUSTER_COUNT].region(0).plot(clusterCountPlot);
        plotter[TAB_CLUSTER_COUNT].region(1).plot(clusterSizePlot);
        plotter[TAB_CLUSTER_COUNT].region(2).plot(edgePlot);
        plotter[TAB_CLUSTER_COUNT].region(3).plot(clusterMaxEnergyPlot);
        
        // Define the Cluster Energy tab.
        plotter[TAB_CLUSTER_ENERGY].createRegions(1, 2);
        plotter[TAB_CLUSTER_ENERGY].region(0).plot(clusterEnergyPlot);
        plotter[TAB_CLUSTER_ENERGY].region(1).plot(clusterMaxEnergyPlot);
        
        // Define the Cluster Time tab.
        plotter[TAB_CLUSTER_TIME].createRegions(1, 2);
        plotter[TAB_CLUSTER_TIME].region(0).plot(clusterTimes);
        plotter[TAB_CLUSTER_TIME].region(1).plot(clusterTimeSigma);
        
        // Define the Cluster Center tab.
        plotter[TAB_CLUSTER_CENTER].createRegion();
        plotter[TAB_CLUSTER_CENTER].region(0).plot(edgePlot);
        plotter[TAB_CLUSTER_CENTER].style().setParameter("hist2DStyle", "colorMap");
        plotter[TAB_CLUSTER_CENTER].style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        
         // Create the Cluster Pair tab.
        plotter[TAB_CLUSTER_PAIR].createRegions(2, 3);
        plotter[TAB_CLUSTER_PAIR].region(0).plot(pairEnergySum);
        plotter[TAB_CLUSTER_PAIR].region(3).plot(pairEnergyDifference);
        plotter[TAB_CLUSTER_PAIR].region(1).plot(pairEnergySlope);
        plotter[TAB_CLUSTER_PAIR].region(4).plot(pairCoplanarity);
        plotter[TAB_CLUSTER_PAIR].region(2).plot(pairEnergyPositionMeanX);
        plotter[TAB_CLUSTER_PAIR].region(5).plot(pairEnergyPositionMeanY);
        
        // If they should not be hidden, display the tabs.
        if(!hide) {
        	for(IPlotter tab : plotter) {
        		tab.show();
        	}
        }
    }
    
    /**
     * Populates all histograms from event clusters, if they are present.
     * @param event - The event from which to draw clusters.
     */
    @Override
    public void process(EventHeader event) {
    	// Check whether the event has clusters or not.
    	if(event.hasCollection(Cluster.class, clusterCollectionName)) {
    		// Get the list of clusters.
    		List<Cluster> clusterList = event.get(Cluster.class, clusterCollectionName);
    		
    		// Create lists to store the clusters from the top of the
    		// calorimeter and the bottom.
    		List<Cluster> topList = new ArrayList<Cluster>();
    		List<Cluster> bottomList = new ArrayList<Cluster>();
    		
    		// Track the highest energy cluster in the event.
    		double maxEnergy = 0.0;
    		
    		// Process each of the clusters.
    		for(Cluster cluster : clusterList) {
    			// If this cluster has a higher energy then was seen
    			// previously, it is now the highest energy cluster.
				if (cluster.getEnergy() > maxEnergy) {
					maxEnergy = cluster.getEnergy();
				}
				
				
				// Get the list of calorimeter hits and its size.
				List<CalorimeterHit> hitList = cluster.getCalorimeterHits();
				int hitCount = hitList.size();
				
				// Track cluster statistics.
				double xEnergyWeight = 0.0;
				double yEnergyWeight = 0.0;
				double[] hitTimes = new double[hitCount];
				double totalHitEnergy = 0.0;
				
				// Iterate over the hits and extract statistics from them.
				for(int hitIndex = 0; hitIndex < hitCount; hitIndex++) {
					hitTimes[hitIndex] = hitList.get(hitIndex).getTime();
					totalHitEnergy += hitList.get(hitIndex).getRawEnergy();
					xEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex).getIdentifierFieldValue("ix"));
					yEnergyWeight += (hitList.get(hitIndex).getRawEnergy() * hitList.get(hitIndex).getIdentifierFieldValue("iy"));
				}
				
				// If the cluster energy exceeds zero, plot the cluster
				// statistics.
				if(cluster.getEnergy() > 0) {
					clusterSizePlot.fill(hitCount);
					clusterTimes.fill(StatUtils.mean(hitTimes, 0, hitCount));
					clusterTimeSigma.fill(Math.sqrt(StatUtils.variance(hitTimes, 0, hitCount)));
					edgePlot.fill(xEnergyWeight / totalHitEnergy, yEnergyWeight / totalHitEnergy);
				}
				
				// Fill the single cluster plots.
				clusterEnergyPlot.fill(cluster.getEnergy());
				
    			// Cluster pairs are formed from all top/bottom cluster
    			// combinations. To create these pairs, separate the
    			// clusters into two lists based on their y-indices.
    			if(cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix") > 0) {
    				topList.add(cluster);
    			} else {
    				bottomList.add(cluster);
    			}
    		}
    		
    		// Populate the event plots.
    		clusterCountPlot.fill(clusterList.size());
    		if(maxEnergy > 0) { clusterMaxEnergyPlot.fill(maxEnergy); }
    		
    		// Create a list to store cluster pairs.
    		List<Cluster[]> pairList = new ArrayList<Cluster[]>(topList.size() * bottomList.size());
    		
    		// Form pairs from all possible combinations of clusters
    		// from the top and bottom lists.
    		for(Cluster topCluster : topList) {
    			for(Cluster bottomCluster : bottomList) {
    				// Make a cluster pair array.
    				Cluster[] pair = new Cluster[2];
    				
    				// The lower energy cluster goes in the second slot.
    				if(topCluster.getEnergy() > bottomCluster.getEnergy()) {
    					pair[0] = topCluster;
    					pair[1] = bottomCluster;
    				} else {
    					pair[0] = bottomCluster;
    					pair[1] = topCluster;
    				}
    				
    				// Add the pair to the pair list.
    				pairList.add(pair);
    			}
    		}
    		
    		// Iterate over each pair and calculate the pair cut values.
    		for(Cluster[] pair : pairList) {
    			// Get the energy slope value.
    			double energySumValue = SSPTriggerLogic.getValueEnergySum(pair);
    			double energyDifferenceValue = SSPTriggerLogic.getValueEnergyDifference(pair);
    			double energySlopeValue = SSPTriggerLogic.getValueEnergySlope(pair, 0.005500);
    			double coplanarityValue = SSPTriggerLogic.getValueCoplanarity(pair);
    			double xMean = ((pair[0].getEnergy() * pair[0].getPosition()[0]) +
    					(pair[1].getEnergy() * pair[1].getPosition()[0])) / energySumValue;
    			double yMean = ((pair[0].getEnergy() * pair[0].getPosition()[1]) +
    					(pair[1].getEnergy() * pair[1].getPosition()[1])) / energySumValue;
    			
    			// Populate the cluster pair plots.
    			pairEnergySum.fill(energySumValue, 1);;
    			pairEnergyDifference.fill(energyDifferenceValue, 1);
    			pairEnergySlope.fill(energySlopeValue, 1);
    			pairCoplanarity.fill(coplanarityValue, 1);
    			pairEnergyPositionMeanX.fill(xMean);
    			pairEnergyPositionMeanY.fill(yMean);
    		}
    	}
    	
    	// If the event does not contain clusters, update the "Event
    	// Clusters" plot accordingly.
    	else { clusterCountPlot.fill(0); }
    }
    
    /**
     * Sets the whether the histograms should be hidden or not.
     * @param hide - <code>true</code> indicates that the histograms
     * will be hidden and <code>false</code> that they will not.
     */
    public void setHide(boolean hide) {
        this.hide = hide;
    }
    
    /**
     * Sets the collection name for the input LCIO cluster collection.
     * @param clusterCollectionName - The LCIO collection name.
     */
    public void setInputCollection(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }
    
    /**
     * Sets whether the histograms are scaled on a logarithmic scale
     * or a linear scale.
     * @param logScale - <code>true</code> indicates that a logarithmic
     * scale will be used and <code>false</code> that a linear scale
     * will be used.
     */
    public void setLogScale(boolean logScale) {
        this.logScale = logScale;
    }
    
    /**
     * Sets the maximum value allowed for the energy scale.
     * @param maxE - The maximum value in GeV.
     */
    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }
}
