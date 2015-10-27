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
import java.lang.IllegalArgumentException;

import javax.swing.SwingUtilities;

import org.hps.monitoring.ecal.eventdisplay.ui.PDataEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.hps.monitoring.ecal.eventdisplay.ui.Viewer;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalEvent;
import org.hps.monitoring.ecal.eventdisplay.util.CrystalListener;
import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This <code>Driver</code> creates an instance of the ECAL event display viewer and will generate plots in the
 * monitoring app for individual channels that are selected. The event display is updated regularly, according to the
 * event refresh rate. If the user clicks on a crystal, the corresponding energy and time distributions (both of type
 * <code>IHistogram1D</code>) are shown in the last panel of the monitoring application, as well as a 2D histogram (hit
 * time vs. hit energy). The fourth panel reports energy for the crystal.
 *
 * @author Andrea Celentano
 */
public class EcalEventDisplay extends Driver implements CrystalListener, ActionListener {
    private static final String CLUSTER_ENERGY_TITLE = "Cluster Energy (GeV)";

    // private static final String SIGNAL_DATA_STYLE_COLOR = "orange";
    // private static final String RAW_WAVEFORM_TITLE = "Raw Waveform";
    private static final String HIT_ENERGY_TITLE = "Hit Energy (GeV)";
    // private static final String SIGNAL_TIME_TITLE = "Time (ns)";
    private static final String HIT_TIME_TITLE = "Hit Time (ns)";
    // Plot style and title variables.
    private static final String NO_TITLE = "";

    // Class variables.
    private static final int NUM_CHANNELS = 11 * 47;
    private final AIDA aida = AIDA.defaultInstance();
    // Channel plot lists.
    private ArrayList<IHistogram1D> channelEnergyPlot;

    private ArrayList<IHistogram1D> channelTimePlot;
    // private ArrayList<IHistogram1D> channelRawWaveform;
    private ArrayList<IHistogram2D> channelTimeVsEnergyPlot;
    private String clusterCollection = "EcalClusters";
    private ArrayList<IHistogram1D> clusterEnergyPlot;

    private String detectorName;
    // private static final String SIGNAL_AMPLITUDE_TITLE = "Signal Amplitude (mV)";
    private int eventRefreshRate = 1; // The number of seconds before an update occurs.
    // LCIO Collection names.
    private String inputCollection = "EcalCalHits";
    private String inputCollectionRaw = "EcalReadoutHits";
  
    // channel.
    private long lastEventTime = 0; // Tracks the time at which the last event occurred.
    private double maxEch = 3500 * EcalUtils.MeV; // The energy scale maximum.
    private double minEch = 10 * EcalUtils.MeV; // The energy scale minimum.
    private int pedSamples = 10; //
    // Plotter objects and variables.
    private IPlotter plotter;

    private IPlotterFactory plotterFactory;
    private IPlotterStyle pstyle; // The plotter style for all plots.
    private boolean resetOnUpdate = true; // Clears the event display on each update.
    // Internal variables.
    private final PEventViewer viewer; // Single event display.
   
    public EcalEventDisplay() {
        // Check if the configuration mapping file exists.
        final File config = new File("ecal-mapping-config.csv");

        // If the file exists, load the viewer that will display it.
        if (config.exists() && config.canRead()) {
            // Account for IO read errors. Only load this version if
            // the data file can be read successfully.
            try {
                this.viewer = new PDataEventViewer(config.getAbsolutePath());
            }
            // Otherwise, open the regular version.
            catch (final IOException e) {
                // Throw an error if this happens as configuration file should be accessible (checked for it already).
                throw new RuntimeException(e);
                // this.viewer = new PEventViewer();
            }
        } else {
            // If the file is not present, then just load the normal version.
            this.viewer = new PEventViewer();
        }
    }

    @Override
    public void actionPerformed(final ActionEvent ae) {
    }

    /**
     * Initializes the default style for plots.
     *
     * @return Returns an <code>IPlotterStyle</code> object that represents the default style for plots.
     */
    public IPlotterStyle createDefaultStyle() {
        final IPlotterStyle pstyle = this.plotterFactory.createPlotterStyle();
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
        pstyle.dataStyle().errorBarStyle().setParameter("errorBarDecoration", new Float(1.0f).toString());

        // Turn off grid lines until explicitly enabled.
        pstyle.gridStyle().setVisible(false);

        // Return the style.
        return pstyle;
    }

    @Override
    public void crystalActivated(final CrystalEvent e) {
    }

    /**
     * Updates the monitoring plots for the crystal that was clicked.
     */
    @Override
    public void crystalClicked(final CrystalEvent e) {
        // Get the crystal that was clicked in the LCSim coordinate system.
        final Point ecalPoint = Viewer.toEcalPoint(e.getCrystalID());

        // Make sure that the clicked crystal is valid. Necessary??
        if (ecalPoint.x != 0 && ecalPoint.y != 0) {
            if (!EcalMonitoringUtilities.isInHole(ecalPoint.y, ecalPoint.x)) {
                // Get the crystal ID.
                final int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(ecalPoint.y, ecalPoint.x);

                // Clear and replot region 0 for the new crystal.
                this.plotter.region(0).clear();
                this.pstyle.xAxisStyle().setLabel(HIT_ENERGY_TITLE);
                this.pstyle.yAxisStyle().setLabel(NO_TITLE);
                this.plotter.region(0).plot(this.channelEnergyPlot.get(id), this.pstyle);

                // Clear and replot region 1 for the new crystal.
                this.plotter.region(1).clear();
                this.pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
                this.pstyle.yAxisStyle().setLabel(NO_TITLE);
                this.plotter.region(1).plot(this.channelTimePlot.get(id), this.pstyle);

                // Clear and replot region 2 for the new crystal.
                this.plotter.region(2).clear();
                this.pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
                this.pstyle.yAxisStyle().setLabel(HIT_ENERGY_TITLE);
                this.plotter.region(2).plot(this.channelTimeVsEnergyPlot.get(id), this.pstyle);

                // Process and plot the region 3 plot.
                this.plotter.region(3).clear();
                this.pstyle.xAxisStyle().setLabel(CLUSTER_ENERGY_TITLE);
                this.pstyle.yAxisStyle().setLabel(NO_TITLE);
                this.plotter.region(3).plot(this.clusterEnergyPlot.get(id), this.pstyle);

            }
        }
    }

    @Override
    public void crystalDeactivated(final CrystalEvent e) {
    }

    /**
     * Initializes the single channel monitoring plots for all crystal channels and defines the plotter region that
     * contains them.
     */
    @Override
    public void detectorChanged(final Detector detector) {
        // Reset the AIDA tree directory.
        this.aida.tree().cd("/");
        this.detectorName = detector.getName();
        // Store histograms for the crystals.
        this.channelEnergyPlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        this.channelTimePlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        // channelRawWaveform = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        this.clusterEnergyPlot = new ArrayList<IHistogram1D>(NUM_CHANNELS);
        this.channelTimeVsEnergyPlot = new ArrayList<IHistogram2D>(NUM_CHANNELS);

        // Create the histograms for single channel energy and time
        // distribution.
        for (int ii = 0; ii < NUM_CHANNELS; ii++) {
            // The above instruction is a terrible hack, just to fill
            // the arrayList with all the elements. They'll be initialized
            // properly during the event readout, Since we want to account
            // for possibly different raw waveform dimensions!

            // Get the x and y indices for the current channel.
            final int row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            final int column = EcalMonitoringUtilities.getColumnFromHistoID(ii);

            // Initialize the histograms for the current crystal channel.
            this.channelEnergyPlot.add(this.aida.histogram1D(this.detectorName + " : " + this.inputCollection
                    + " : Hit Energy : " + column + " " + row + ": " + ii, 100, -0.2, this.maxEch));
            this.channelTimePlot.add(this.aida.histogram1D(this.detectorName + " : " + this.inputCollection
                    + " : Hit Time : " + column + " " + row + ": " + ii, 100, 0, 400));
            this.channelTimeVsEnergyPlot
            .add(this.aida.histogram2D(this.detectorName + " : " + this.inputCollection
                    + " : Hit Time Vs Energy : " + column + " " + row + ": " + ii, 100, 0, 400, 100, -0.2,
                    this.maxEch));
            // channelRawWaveform.add(aida.histogram1D(detector.getDetectorName() + " : "
            // + inputCollection + " : Hit Energy : " + column + " " + row + ": " + ii));
            this.clusterEnergyPlot.add(this.aida.histogram1D(this.detectorName + " : " + this.inputCollection
                    + " : Cluster Energy : " + column + " " + row + ": " + ii, 100, -0.2, this.maxEch));

   
          
        }

        // Define the plot region that will display the single channel
        // plots in the monitoring application.
        this.plotterFactory = this.aida.analysisFactory().createPlotterFactory("Single Channel");
        this.plotter = this.plotterFactory.create("Single Channel");
        this.pstyle = this.createDefaultStyle();
        this.plotter.setTitle("");
        this.plotter.createRegions(2, 2);

        // Define the first plot region.
        this.pstyle.xAxisStyle().setLabel(HIT_ENERGY_TITLE);
        this.pstyle.yAxisStyle().setLabel(NO_TITLE);
        this.plotter.region(0).plot(this.channelEnergyPlot.get(0), this.pstyle);

        // Define the second plot region.
        this.pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
        this.pstyle.yAxisStyle().setLabel(NO_TITLE);
        this.plotter.region(1).plot(this.channelTimePlot.get(0), this.pstyle);

        // Define the third plot region; this encompasses the time vs.
        // energy plots.
        this.pstyle.xAxisStyle().setLabel(HIT_TIME_TITLE);
        this.pstyle.yAxisStyle().setLabel(HIT_ENERGY_TITLE);
        this.plotter.region(2).plot(this.channelTimeVsEnergyPlot.get(0), this.pstyle);

        // Define the fourth plot region; this encompasses the cluster
        // energy for each channel.
        this.pstyle.xAxisStyle().setLabel(CLUSTER_ENERGY_TITLE);
        this.pstyle.yAxisStyle().setLabel(NO_TITLE);
        this.plotter.region(3).plot(this.clusterEnergyPlot.get(0), this.pstyle);

        /**
         * // Define the fourth plot region; this encompasses the raw // wave form plots.
         * pstyle.xAxisStyle().setLabel(RAW_WAVEFORM_TITLE); pstyle.yAxisStyle().setLabel(NO_TITLE);
         * pstyle.dataStyle().fillStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
         * pstyle.dataStyle().markerStyle().setColor(SIGNAL_DATA_STYLE_COLOR);
         * pstyle.dataStyle().errorBarStyle().setVisible(false); plotter.region(3).plot(channelRawWaveform.get(0),
         * pstyle);
         **/

        // Display the plot region.
        this.plotter.show();

        // Set the time tracker variables.
        this.lastEventTime = 0;
    }

    /**
     * Hides the single event display and disposes it from memory. Also removes histograms from aida tree. We do not
     * want them in the output aida file, if any..
     */
    @Override
    public void endOfData() {
        // Disposing of event display window needs to happen on the Swing EDT.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                System.out.println("turning ECAL event display off");
                EcalEventDisplay.this.viewer.setVisible(false);
                System.out.println("disposing ECAL event display ...");
                EcalEventDisplay.this.viewer.dispose();
                System.out.println("done disposing ECAL event display");
            }
        });

        int row,column; String hName; 
        System.out.println("EcalEventDisplay endOfData clear histograms"); 
        for(int ii = 0; ii < NUM_CHANNELS; ii++) { 
            row = EcalMonitoringUtilities.getRowFromHistoID(ii);
            column = EcalMonitoringUtilities.getColumnFromHistoID(ii);
            hName=detectorName + " : " + inputCollection + " : Hit Energy : " + column + " " + row + ": " + ii;
            try{
                aida.tree().rm(hName);
            }
            catch(IllegalArgumentException e){
                System.out.println("Got exception "+e);
            }



            hName=detectorName + " : " + inputCollection + " : Hit Time : " + column + " " + row + ": " + ii;
            try{
                aida.tree().rm(hName);
            }
            catch(IllegalArgumentException e){
                System.out.println("Got exception "+e);
            }



            hName=detectorName+ " : " + inputCollection + " : Hit Time Vs Energy : " + column + " " + row + ": " + ii; 
            try{
                aida.tree().rm(hName);
            }
            catch(IllegalArgumentException e){
                System.out.println("Got exception "+e);
            }


        }
        System.out.println("EcalEventDisplay endOfData clear histograms done");
    }

    @Override
    public void process(final EventHeader event) {
        // Check whether enough time has passed to perform an update
        // on the event display.
        boolean update = false;
        final long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - this.lastEventTime > this.eventRefreshRate) {
            this.lastEventTime = currentTime;
            update = true;
        }

        // If an update should be made, perform the update.
        if (update && this.resetOnUpdate) {
            this.viewer.resetDisplay();
        }

        // If the event has calorimeter hit objects...
        if (event.hasCollection(CalorimeterHit.class, this.inputCollection)) {
            // Get the list of calorimeter hits.
            final List<CalorimeterHit> hits = event.get(CalorimeterHit.class, this.inputCollection);

            // For each of the calorimeter hits...
            for (final CalorimeterHit hit : hits) {
                // Get the x and y indices for the current hit.
                final int ix = hit.getIdentifierFieldValue("ix");
                final int iy = hit.getIdentifierFieldValue("iy");

                if (iy != 0 && ix != 0) {
                    // Get the histogram index for the hit.
                    final int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(iy, ix);

                    // If the hit has energy, populate the plots.
                    if (hit.getCorrectedEnergy() > 0) {
                        this.channelEnergyPlot.get(id).fill(hit.getCorrectedEnergy());
                        this.channelTimePlot.get(id).fill(hit.getTime());
                        this.channelTimeVsEnergyPlot.get(id).fill(hit.getTime(), hit.getCorrectedEnergy());
                    }

                    // If an update to the event display should be
                    // performed, give it the hits.
                    if (update) {
                        this.viewer.addHit(hit);
                    }
                }
            }
        }

        // If there are clusters in the event...
        if (event.hasCollection(Cluster.class, this.clusterCollection)) {
            // Get the list of clusters.
            final List<Cluster> clusters = event.get(Cluster.class, this.clusterCollection);

            // Iterate over the clusters and add them to the event
            // display if appropriate.
            for (final Cluster cluster : clusters) {
                // Get the ix and iy indices for the seed.
                final int ix = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix");
                final int iy = cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy");

                // Get the histogram index for the hit.
                final int id = EcalMonitoringUtilities.getHistoIDFromRowColumn(iy, ix);

                // Add the cluster energy to the plot.
                if (cluster.getEnergy() > 0.0) {
                    this.clusterEnergyPlot.get(id).fill(cluster.getEnergy());
                }

                // If an update is needed, add the cluster to the viewer.
                if (update) {
                    this.viewer.addCluster(cluster);
                }
            }
        }

   
        // Update the single event display.
        if (update) {
            this.viewer.updateDisplay();
        }
    }

    /**
     * Sets the rate at which the GUI updates its elements,
     *
     * @param eventRefreshRate - The rate at which the GUI should be updated, in seconds.
     */
    public void setEventRefreshRate(final int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    /**
     * Sets the LCIO collection name for calorimeter clusters.
     *
     * @param inputClusterCollection - The LCIO collection name.
     */
    public void setInputClusterCollection(final String inputClusterCollection) {
        this.clusterCollection = inputClusterCollection;
    }

    /**
     * Sets the LCIO collection name for the processed calorimeter hits.
     *
     * @param inputCollection - The LCIO collection name.
     */
    public void setInputCollection(final String inputCollection) {
        this.inputCollection = inputCollection;
    }

    /**
     * Sets the LCIO collection name for the raw waveform hits.
     *
     * @param inputCollectionRaw - The LCIO collection name.
     */
    public void setInputCollectionRaw(final String inputCollectionRaw) {
        this.inputCollectionRaw = inputCollectionRaw;
    }

    /**
     * Sets the upper bound of the energy scales used by the driver. Energy units are in GeV.
     *
     * @param maxEch - The energy scale upper bound.
     */
    public void setMaxEch(final double maxEch) {
        this.maxEch = maxEch;
    }

    /**
     * Sets the lower bound of the energy scales used by the driver. Energy units are in GeV.
     *
     * @param minEch - The lower energy scale bound.
     */
    public void setMinEch(final double minEch) {
        this.minEch = minEch;
    }

    public void setPedSamples(final int pedSamples) {
        this.pedSamples = pedSamples;
    }

    /**
     * Sets whether the event display should be cleared after event or whether it should retain the previously displayed
     * results.
     *
     * @param resetOnUpdate - <code>true</code> means that the event display should be cleared on each update and
     *            <code>false</code> that it should not.
     */
    public void setResetOnUpdate(final boolean resetOnUpdate) {
        this.resetOnUpdate = resetOnUpdate;
    }

    /**
     * Initializes the <code>Viewer</code> for the single event display. If a configuration file is available, then it
     * is used by the <code>Viewer</code> to display hardware configuration mappings. Otherwise, this is excluded.
     */
    @Override
    public void startOfData() {

        // Set the viewer properties from the Driver configuration parameters.
        this.viewer.setScaleMinimum(this.minEch);
        this.viewer.setScaleMaximum(this.maxEch);
        this.viewer.addCrystalListener(this);

        // Make the Viewer object visible. Run on the Swing EDT.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                EcalEventDisplay.this.viewer.setVisible(true);
            }
        });
    }
}
