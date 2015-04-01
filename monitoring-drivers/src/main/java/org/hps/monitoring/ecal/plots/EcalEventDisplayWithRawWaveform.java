package org.hps.monitoring.ecal.plots;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.aida.IPlotterFactory;
import hep.aida.IPlotterStyle;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.ui.PDataEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.Viewer;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalEvent;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalListener;
import org.hps.monitoring.ecal.plots.EcalMonitoringUtilities;
import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver <code>EcalEventDisplay</code> generates the histograms shown
 * to the user in the fifth tab of the monitoring application. An
 * instance of the single event display is employed to allow for the
 * selection of specific crystal channels.<br/>
 * <br/>
 * The implementation is as follows:
 * <ul><li>The event display is opened in a separate window</li>
 * <li>It is updated regularly, according to the event refresh rate</li>
 * <li>If the user clicks on a crystal, the corresponding energy and time
 * distributions (both of type <code>IHistogram1D</code>) are shown in
 * the last panel of the monitoring application, as well as a 2D histogram
 * (hit time vs. hit energy). Finally, if available, the raw waveshape (in
 * mV) is displayed.</li>
 * 
 * @author Andrea Celentano
 */
public class EcalEventDisplayWithRawWaveform extends Driver implements CrystalListener, ActionListener {
	// Class variables.
	private static final int NUM_CHANNELS = 11 * 47;
	
	// Plotter objects and variables.
	private IPlotter plotter;
	private IPlotterFactory plotterFactory;
	private AIDA aida = AIDA.defaultInstance();	
	
	// LCIO Collection names.
	private String inputCollection = "EcalCalHits";
	private String clusterCollection = "EcalClusters";
	private String inputCollectionRaw = "EcalReadoutHits";
	
	// Channel plot lists.
	private ArrayList<IHistogram1D> channelEnergyPlot;
	private ArrayList<IHistogram1D> clusterEnergyPlot;
	private ArrayList<IHistogram1D> channelTimePlot;
	private ArrayList<IHistogram1D> channelRawWaveform;
	private ArrayList<IHistogram2D> channelTimeVsEnergyPlot;
	
	// Internal variables.
	private PEventViewer viewer;								 // Single event display.
	private int pedSamples = 10;								 // 
	private IPlotterStyle pstyle;								 // The plotter style for all plots.
	private long lastEventTime = 0;								 // Tracks the time at which the last event occurred.
	private int eventRefreshRate = 1;							 // The number of seconds before an update occurs.
	private boolean resetOnUpdate = true;						 // Clears the event display on each update.
	private double minEch = 10 * EcalUtils.MeV;					 // The energy scale minimum.
	private double maxEch = 3500 * EcalUtils.MeV;				 // The energy scale maximum.
	private int[] windowRaw = new int[NUM_CHANNELS];			 // The number of samples in a waveform for each channel.
	private boolean[] isFirstRaw = new boolean[NUM_CHANNELS];	 // Whether a waveform plot was initiated for each channel.
	
	// Plot style and title variables.
	private static final String NO_TITLE = "";
	private static final String SIGNAL_TIME_TITLE = "Time (ns)";
	private static final String HIT_TIME_TITLE = "Hit Time (ns)";
	private static final String SIGNAL_DATA_STYLE_COLOR = "orange";
	private static final String RAW_WAVEFORM_TITLE = "Raw Waveform";
	private static final String HIT_ENERGY_TITLE = "Hit Energy (GeV)";
	private static final String CLUSTER_ENERGY_TITLE = "Cluster Energy (GeV)";
	private static final String SIGNAL_AMPLITUDE_TITLE = "Signal Amplitude (mV)";
	private String detectorName;
	
	/**
	 * Sets the upper bound of the energy scales used by the driver.
	 * Energy units are in GeV.
	 * @param maxEch - The energy scale upper bound.
	 */
	public void setMaxEch(double maxEch) {
		this.maxEch = maxEch;
	}
	
	/**
	 * Sets the lower bound of the energy scales used by the driver.
	 * Energy units are in GeV.
	 * @param minEch - The lower energy scale bound.
	 */
	public void setMinEch(double minEch) {
		this.minEch = minEch;
	}
	
	public void setPedSamples(int pedSamples) {
		this.pedSamples = pedSamples;
	}
	/**
	 * Sets the LCIO collection name for the processed calorimeter hits.
	 * @param inputCollection - The LCIO collection name.
	 */
	public void setInputCollection(String inputCollection) {
		this.inputCollection = inputCollection;
	}
	
	/**
	 * Sets the LCIO collection name for the raw waveform hits.
	 * @param inputCollectionRaw - The LCIO collection name.
	 */
	public void setInputCollectionRaw(String inputCollectionRaw) {
		this.inputCollectionRaw = inputCollectionRaw;
	}
	
	/**
	 * Sets the LCIO collection name for calorimeter clusters.
	 * @param inputClusterCollection - The LCIO collection name.
	 */
	public void setInputClusterCollection(String inputClusterCollection) {
		this.clusterCollection = inputClusterCollection;
	}
	
	/**
	 * Sets the rate at which the GUI updates its elements,
	 * @param eventRefreshRate - The rate at which the GUI should be
	 * updated, in seconds.
	 */
	public void setEventRefreshRate(int eventRefreshRate) {
		this.eventRefreshRate = eventRefreshRate;
	}
	
	/**
	 * Sets whether the event display should be cleared after event
	 * or whether it should retain the previously displayed results.
	 * @param resetOnUpdate - <code>true</code> means that the event
	 * display should be cleared on each update and <code>false</code>
	 * that it should not.
	 */
	public void setResetOnUpdate(boolean resetOnUpdate) {
		this.resetOnUpdate = resetOnUpdate;
	}
	
	/**
	 * Initializes the single channel monitoring plots for all crystal
	 * channels and defines the plotter region that contains them.
	 */
	@Override
	public void detectorChanged(Detector detector) {
	    detectorName=detector.getName();
		// Reset the AIDA tree directory.
		aida.tree().cd("/");
		
		// Store histograms for the crystals.
		channelEnergyPlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		channelTimePlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		channelRawWaveform = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		clusterEnergyPlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
		channelTimeVsEnergyPlot = new ArrayList<IHistogram2D>(NUM_CHANNELS);
		
		// Create the histograms for single channel energy and time
		// distribution.
		for(int ii = 0; ii < NUM_CHANNELS; ii++) {
			// The above instruction is a terrible hack, just to fill
			// the arrayList with all the elements. They'll be initialized
			// properly during the event readout, Since we want to account
			// for possibly different raw waveform dimensions!
			
			//Get the x and y indices for the current channel.
			int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
			int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
			
			// Initialize the histograms for the current crystal channel.
			channelEnergyPlot.add(aida.histogram1D(detectorName + " : "
						+ inputCollection + " : Hit Energy : " + column + " " + row
						+ ": " + ii, 100, -0.2, maxEch));
			channelTimePlot.add(aida.histogram1D(detectorName + " : "
						+ inputCollection + " : Hit Time : " + column + " " + row + ": "
						+ ii, 100, 0, 400));     
			channelTimeVsEnergyPlot.add(aida.histogram2D(detectorName 
					+ " : " + inputCollection + " : Hit Time Vs Energy : " + column
					+ " " + row + ": " + ii, 100, 0, 400, 100, -0.2, maxEch));              
			channelRawWaveform.add(aida.histogram1D(detectorName  + " : "
					+ inputCollection + " : Hit Energy : " + column + " " + row + ": " + ii));
			clusterEnergyPlot.add(aida.histogram1D(detectorName  + " : "
					+ inputCollection + " : Cluster Energy : " + column + " " + row
					+ ": " + ii, 100, -0.2, maxEch));
			
			// Note that no raw waveform has yet been read for this
			// crystal/channel.
			windowRaw[ii] = 1;
			isFirstRaw[ii] = true;
		}
		
		// Define the plot region that will display the single channel
		// plots in the monitoring application.
		plotterFactory = aida.analysisFactory().createPlotterFactory("Single Channel");
		plotter = plotterFactory.create("Single Channel");
		pstyle = this.createDefaultStyle();
		plotter.setTitle("");
		plotter.createRegions(2,2);
		
		// Define the first plot region.
		pstyle.xAxisStyle().setLabel(HIT_ENERGY_TITLE);
		pstyle.yAxisStyle().setLabel(NO_TITLE);
		plotter.region(0).plot(channelEnergyPlot.get(0), pstyle);
		
		// Define the second plot region.
		pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
		pstyle.yAxisStyle().setLabel(NO_TITLE);
		plotter.region(1).plot(channelTimePlot.get(0), pstyle);
		
		// Define the third plot region; this encompasses the time vs.
		// energy plots.
		pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
		pstyle.yAxisStyle().setLabel(HIT_ENERGY_TITLE);
		plotter.region(2).plot(channelTimeVsEnergyPlot.get(0), pstyle);
		
		
		
		
		// Define the fourth plot region; this encompasses the raw
		// wave form plots.
		pstyle.xAxisStyle().setLabel(RAW_WAVEFORM_TITLE);
		pstyle.yAxisStyle().setLabel(NO_TITLE);
		pstyle.dataStyle().fillStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
		pstyle.dataStyle().markerStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
		pstyle.dataStyle().errorBarStyle().setVisible(false);
		plotter.region(3).plot(channelRawWaveform.get(0), pstyle);
		
		
		// Display the plot region.
		plotter.show();
		
		// Set the time tracker variables.
		lastEventTime = 0;
	}
	
	/**
	 * Initializes the <code>Viewer</code> for the single event display.
	 * If a configuration file is available, then it is used by the
	 * <code>Viewer</code> to display hardware configuration mappings.
	 * Otherwise, this is excluded.
	 */
	@Override
	public void startOfData() {
		// Check if the configuration mapping file exists.
		File config = new File("ecal-mapping-config.csv");
		
		// If the file exists, load the viewer that will display it.
		if(config.exists() && config.canRead()) {
			// Account for IO read errors. Only load this version if
			// the data file can be read successfully.
			try { viewer = new PDataEventViewer(config.getAbsolutePath()); }
			
			// Otherwise, open the regular version.
			catch (IOException e) { viewer = new PEventViewer(); }
		}
		
		// If the file is not present, then just load the normal version.
		else { viewer = new PEventViewer(); }
		
		// Set the viewer properties.
		viewer.setScaleMinimum(minEch);
		viewer.setScaleMaximum(maxEch);
		viewer.addCrystalListener(this);
		
		// Make the Viewer object visible.
		viewer.setVisible(true);
	}
	
	/**
	 * Hides the single event display and disposes it from memory.
	 */
	@Override
	public void endOfData() {
		viewer.setVisible(false);
		viewer.dispose();
		
		

        int row,column;
        String hName;
        //System.out.println("EcalEventDisplay endOfData clear histograms");
        for(int ii = 0; ii < NUM_CHANNELS; ii++) {
            // The above instruction is a terrible hack, just to fill
            // the arrayList with all the elements. They'll be initialized
            // properly during the event readout, Since we want to account
            // for possibly different raw waveform dimensions!
            
            //Get the x and y indices for the current channel.
            row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
            hName=detectorName + " : "
                    + inputCollection + " : Hit Energy : " + column + " " + row
                    + ": " + ii;
            aida.tree().rm(hName);
           
            hName=detectorName + " : "
                    + inputCollection + " : Hit Time : " + column + " " + row + ": "
                    + ii;
            aida.tree().rm(hName);
            
            hName=detectorName+ " : " + inputCollection + " : Hit Time Vs Energy : " + column
                    + " " + row + ": " + ii;
            aida.tree().rm(hName);
            
            hName=detectorName + " : "
                    + inputCollection + " : Cluster Energy : " + column + " " + row
                    + ": " + ii;
            aida.tree().rm(hName);
        
            hName=detectorName + " : "
                    + inputCollection + " : Cluster Energy : " + column + " " + row
                    + ": " + ii;
            aida.tree().rm(hName);
        
        
        }
      //System.out.println("EcalEventDisplay endOfData clear histograms done");
		
		
	}
	
	@Override
	public void process(EventHeader event){
		// Check whether enough time has passed to perform an update
		// on the event display.
		boolean update = false;
		long currentTime = System.currentTimeMillis() / 1000;
		if((currentTime - lastEventTime) > eventRefreshRate){
			lastEventTime = currentTime;
			update = true;
		}
		
		// If an update should be made, perform the update.
		if(update && resetOnUpdate) { viewer.resetDisplay(); }
		
		// If the event has calorimeter hit objects...
		if(event.hasCollection(CalorimeterHit.class, inputCollection)) {
			// Get the list of calorimeter hits.
			List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
			
			// For each of the calorimeter hits...
			for (CalorimeterHit hit : hits) {
				// Get the x and y indices for the current hit.
				int ix = hit.getIdentifierFieldValue("ix");
				int iy = hit.getIdentifierFieldValue("iy");
				
				if (iy != 0 && ix != 0) {
					// Get the histogram index for the hit.
					int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(iy, ix);
					
					// If the hit has energy, populate the plots.
					if(hit.getCorrectedEnergy() > 0) {
						channelEnergyPlot.get(id).fill(hit.getCorrectedEnergy());
						channelTimePlot.get(id).fill(hit.getTime());
						channelTimeVsEnergyPlot.get(id).fill(hit.getTime(), hit.getCorrectedEnergy());
					}
					
					// If an update to the event display should be
					// performed, give it the hits.
					if(update) { viewer.addHit(hit); }
				}
			}
		}
		
		// If there are clusters in the event...
		if (event.hasCollection(Cluster.class, clusterCollection)) {
			// Get the list of clusters.
			List<Cluster> clusters = event.get(Cluster.class, clusterCollection);
			
			// Iterate over the clusters and add them to the event
			// display if appropriate.
			for (Cluster cluster : clusters) {
				// Get the ix and iy indices for the seed.
				int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
				int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");
				
				// Get the histogram index for the hit.
				int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(iy, ix);
				
				// Add the cluster energy to the plot.
				if(cluster.getEnergy() > 0.0) {
					clusterEnergyPlot.get(id).fill(cluster.getEnergy());
				}
				
				// If an update is needed, add the cluster to the viewer.
				if(update) { viewer.addCluster(cluster); }
			}
		}
		
	
		// Plot the raw waveform only if raw tracker hit exist in the
		// event.
		if (event.hasCollection(RawTrackerHit.class, inputCollectionRaw)){
		    // Get the list of raw tracker hits.
		    List<RawTrackerHit> hits = event.get(RawTrackerHit.class, inputCollectionRaw);
		    
		    // Process each raw tracker hit.
			for (RawTrackerHit hit : hits) {
				// Get the x and y indices for the hit.
				int ix = hit.getIdentifierFieldValue("ix");
				int iy = hit.getIdentifierFieldValue("iy");
				
				if(iy != 0 && ix != 0) {
					if(!EcalMonitoringUtilities.isInHole(iy, ix)) {
						// Get the crystal ID for the current hit.
						int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(iy, ix);
						
						// The window is length is not known by default.
						// If this is the first hit, read the window
						// length and initialize the plot.
						if(isFirstRaw[id]) {
							// Note that this plot is initialized.
							isFirstRaw[id] = false;
							
							// Set the waveform array.
							windowRaw[id] = hit.getADCValues().length;
							
							// Initialize the waveform plot.
							channelRawWaveform.set(id,aida.histogram1D(event.getDetectorName()
									+ " : " + inputCollectionRaw + " : Raw Waveform : " + ix + " "
									+ iy + ": " + id, windowRaw[id], -0.5 * EcalUtils.ecalReadoutPeriod,
									(-0.5 + windowRaw[id]) * EcalUtils.ecalReadoutPeriod));
						}
						
						// If the plot should be updated, do so.
						if(update) {
							channelRawWaveform.get(id).reset();
							for (int jj = 0; jj < windowRaw[id]; jj++) {
								channelRawWaveform.get(id).fill(jj * EcalUtils.ecalReadoutPeriod,
										hit.getADCValues()[jj] * EcalUtils.adcResolution * 1000);
							}
							double[] result = EcalUtils.computeAmplitude(hit.getADCValues(), windowRaw[id], pedSamples);
							channelRawWaveform.get(id).setTitle("Ampl: " + String.format("%.2f", result[0])
									+ " mV , ped : " + String.format("%.2f", result[1]) + " "
									+ String.format("%.2f", result[2]) + " ADC counts");
							plotter.region(3).refresh();
						}
					}
				}
			}
		}
		
		
		// Update the single event display.
		if(update) { viewer.updateDisplay(); }
	}
	
	@Override
	public void actionPerformed(ActionEvent ae) { }
	
	@Override
	public void crystalActivated(CrystalEvent e) { }
	
	@Override
	public void crystalDeactivated(CrystalEvent e) { }
	
	/**
	 * Updates the monitoring plots for the crystal that was clicked.
	 */
	@Override
	public void crystalClicked(CrystalEvent e) {
		// Get the crystal that was clicked in the LCSim coordinate system.
		Point ecalPoint = Viewer.toEcalPoint(e.getCrystalID());
		
		// Make sure that the clicked crystal is valid. Necessary??
		if((ecalPoint.x != 0) && (ecalPoint.y != 0))
			if (!EcalMonitoringUtilities.isInHole(ecalPoint.y, ecalPoint.x)) {
				// Get the crystal ID.
				int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(ecalPoint.y, ecalPoint.x);
				
				// Clear and replot region 0 for the new crystal.
				plotter.region(0).clear();
				pstyle.xAxisStyle().setLabel(HIT_ENERGY_TITLE);
				pstyle.yAxisStyle().setLabel(NO_TITLE);
				plotter.region(0).plot(channelEnergyPlot.get(id), pstyle);
				
				// Clear and replot region 1 for the new crystal.
				plotter.region(1).clear();
				pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
				pstyle.yAxisStyle().setLabel(NO_TITLE);
				plotter.region(1).plot(channelTimePlot.get(id), pstyle);
				
				// Clear and replot region 2 for the new crystal.
				plotter.region(2).clear();
				pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
				pstyle.yAxisStyle().setLabel(HIT_ENERGY_TITLE);
				plotter.region(2).plot(channelTimeVsEnergyPlot.get(id), pstyle);
				
		
				// Process and plot the region 3 plot.
				plotter.region(3).clear();
				if(!isFirstRaw[id]) {
					pstyle.yAxisStyle().setLabel(SIGNAL_AMPLITUDE_TITLE);
					pstyle.xAxisStyle().setLabel(SIGNAL_TIME_TITLE);
					pstyle.dataStyle().fillStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
					pstyle.dataStyle().markerStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
					pstyle.dataStyle().errorBarStyle().setVisible(false);
				}
				else {
					pstyle.xAxisStyle().setLabel(HIT_ENERGY_TITLE);
					pstyle.yAxisStyle().setLabel("");
				}
				plotter.region(3).plot(channelRawWaveform.get(id), pstyle);
		}
	}
	
	/**
	 * Initializes the default style for plots.
	 * @return Returns an <code>IPlotterStyle</code> object that
	 * represents the default style for plots.
	 */
	public IPlotterStyle createDefaultStyle() {
		IPlotterStyle pstyle = plotterFactory.createPlotterStyle();
		// Set the appearance of the axes.
		pstyle.xAxisStyle().labelStyle().setBold(true);
		pstyle.yAxisStyle().labelStyle().setBold(true);
		pstyle.xAxisStyle().tickLabelStyle().setBold(true);
		pstyle.yAxisStyle().tickLabelStyle().setBold(true);
		pstyle.xAxisStyle().lineStyle().setColor("black");
		pstyle.yAxisStyle().lineStyle().setColor("black");
		pstyle.xAxisStyle().lineStyle().setThickness(2);
		pstyle.yAxisStyle().lineStyle().setThickness(2);
		
		// Set color settings.
		pstyle.dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
		pstyle.dataStyle().fillStyle().setParameter("showZeroHeightBins", Boolean.FALSE.toString());
		pstyle.dataStyle().errorBarStyle().setVisible(false);
		pstyle.setParameter("hist2DStyle", "colorMap");
		
		// Force auto range to zero.
		pstyle.yAxisStyle().setParameter("allowZeroSuppression", "false");
		pstyle.xAxisStyle().setParameter("allowZeroSuppression", "false");
		
		// Set the title style.
		pstyle.titleStyle().textStyle().setFontSize(20);
		
		// Draw caps on error bars.
		pstyle.dataStyle().errorBarStyle().setParameter("errorBarDecoration", (new Float(1.0f)).toString());
		
		// Turn off grid lines until explicitly enabled.
		pstyle.gridStyle().setVisible(false);
		
		// Return the style.
		return pstyle;
	}
}
