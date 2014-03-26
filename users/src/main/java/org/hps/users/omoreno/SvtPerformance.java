package org.hps.users.omoreno;


import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hps.conditions.deprecated.HPSSVTCalibrationConstants;
import org.hps.conditions.deprecated.HPSSVTCalibrationConstants.ChannelConstants;
import org.hps.recon.tracking.HPSShapeFitParameters;
import org.hps.recon.tracking.HPSShaperAnalyticFitAlgorithm;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Driver that looks at the performance of the SVT.
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id:$
 */
public class SvtPerformance extends Driver {

    private AIDA aida;
    private List<IPlotter> plotters = new ArrayList<IPlotter>();
    HPSShaperAnalyticFitAlgorithm shaperFitter = new HPSShaperAnalyticFitAlgorithm();
    
    File performanceOutputFile;
    BufferedWriter performanceWriter;
    File samplesOutputFile; 
    BufferedWriter samplesWriter; 

    String performanceOutputFileName = "svt_performance.dat";
    String samplesOutputFileName = "samples.dat";

    int plotterIndex = 0;

    int eventNumber = 0; 
    int runNumber = 0; 
    double totalTracks = 0;
    double totalTwoTrackEvents = 0;

    double[] topLayers;
    double[] bottomLayers;

    boolean debug = false;

    // Collection Names
    private String trackCollectionName = "MatchedTracks";

    // Plot Flags
    boolean plotClustersPerLayer = false;
    boolean plotMIP = false;
    boolean plotSamples = false; 
    boolean batchMode = true; 
    
    public SvtPerformance(){
    }

    // --- Setters ---//
    // ---------------//

    /**
     * Enable/disble debug mode
     * 
     * @param true or false
     * 
     */
    public void setDebug(boolean debug){
        this.debug = debug;
    }

    /**
     * Enable/disable plotting the number of clusters per layer. Only clusters
     * from stereo hits associated with a track are used.
     * 
     * @param true or false
     * 
     */
    public void setPlotClustersPerLayer(boolean plotClustersPerLayer){
        this.plotClustersPerLayer = plotClustersPerLayer;
    }

    /**
     * Enable/disable plotting the cluster charge. Only clusters from stereo
     * hits associated with a track are used.
     * 
     * @param true or false
     * 
     */
    public void setPlotClusterCharge(boolean plotMIP){
        this.plotMIP = plotMIP;
    }

    /**
     * Enable/disable plotting of raw hit samples.  Only raw hits from clusters
     * associated with a track are used.
     * 
     * @param true or false
     * 
     */
    public void setPlotSamples(boolean plotSamples){
        this.plotSamples = plotSamples; 
    }
    
    /**
     * Enable/disable batch mode.  If set to true, plots are not shown.
     *
     * @param true or false
     * 
     */
    public void setBatchMode(boolean batchMode){
    	this.batchMode = batchMode; 
    }
    
    /**
     * Set the name of the file to which performance data will be written.
     * 
     * @param peformanceOutputFileName: Name of the output file
     * 
     */
    public void setPerformanceOutputFileName(String performanceOutputFileName){
        this.performanceOutputFileName = performanceOutputFileName;
    }

    /**
     * Set the name of the file to which raw sample data will be written.
     * 
     * @param samplesOutputFileName : Name of the output file
     * 
     */
    public void setSamplesOutputFileName(String samplesOutputFileName){
        this.samplesOutputFileName = samplesOutputFileName;
    }
    
    /**
     * Set the run number
     * 
     * @param runNumber 
     * 
     */
    public void setRunNumber(int runNumber){
    	this.runNumber = runNumber; 
    }
    
    protected void detectorChanged(Detector detector) {

        this.printDebug("Setting up plots");

        // setup AIDA
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        
        // Get the list of sensors from the detector
        List<HpsSiSensor> sensors = detector.getDetectorElement().findDescendants(HpsSiSensor.class);

        String plotTitle = null;
        IHistogram1D histo1D = null;
        int regionIndex = 0;

        // --- Clusters Per Layer ---//
        // --------------------------//

        if(plotClustersPerLayer){
            
            if(!batchMode){
                plotters.add(PlotUtils.setupPlotter("# Clusters Per Layer", 1, 2));
                for (HpsSiSensor sensor : sensors) {
                    if (sensor.isTopLayer()) {
                        plotTitle = "Top - Layer " + sensor.getLayerNumber();
                        regionIndex = 0;
                    } else {
                        plotTitle = "Bottom - Layer " + sensor.getLayerNumber();
                        regionIndex = 1;
                    }
                    plotTitle += " - # Clusters";
                    histo1D = aida.histogram1D(plotTitle, 9, 1, 10);
                    PlotUtils.setup1DRegion(plotters.get(plotterIndex), plotTitle, regionIndex, "# Clusters", histo1D);
                }
                plotterIndex++;
            }
        }

        // --- MIP Plots ---//
        // -----------------//
        if(plotMIP){

            try {
       
                performanceOutputFile = new File(performanceOutputFileName);
                if (!performanceOutputFile.exists()) performanceOutputFile.createNewFile();
            
                performanceWriter = new BufferedWriter(new FileWriter(performanceOutputFile.getAbsoluteFile()));
        
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            
            try{ 

                performanceWriter.write("! run I\n");
                performanceWriter.write("! event I\n");
                performanceWriter.write("! volume I\n");
                performanceWriter.write("! layer I\n");
                performanceWriter.write("! channel I\n");
                performanceWriter.write("! amplitude D\n");
                performanceWriter.write("! noise D\n");
                performanceWriter.write("! cluster_hits I\n");
                performanceWriter.write("! bad_channel I\n");
                performanceWriter.write("! chi_squared D\n");
                performanceWriter.write("! hit_x D\n");
                performanceWriter.write("! hit_y D\n");
                performanceWriter.write("! trk_chi_squared D\n");
                performanceWriter.write("! hit_time D\n");
                
                
            } catch(IOException exception){
                exception.printStackTrace();
            }
           
            if(!batchMode){
                plotters.add(PlotUtils.setupPlotter("Cluster Charge", 5, 4));
                for (HpsSiSensor sensor : sensors) {
                    if (sensor.isTopLayer()) {
                        plotTitle = "Top - Layer " + sensor.getLayerNumber();
                    } else {
                        plotTitle = "Bottom - Layer " + sensor.getLayerNumber();
                    }
                    plotTitle += " - Cluster Charge";
                    histo1D = aida.histogram1D(plotTitle, 70, 0, 5040);
                    PlotUtils.setup1DRegion(plotters.get(plotterIndex), plotTitle, PlotUtils.getPlotterRegion(sensor), "Cluster Charge [e-]", histo1D);
                }
                plotterIndex++;
            }
        }
       
        //--- Sample Plots ---//
        //--------------------//
        if(plotSamples){

            try {
                
                samplesOutputFile = new File(samplesOutputFileName);
                if (!samplesOutputFile.exists()) samplesOutputFile.createNewFile();
            
                samplesWriter = new BufferedWriter(new FileWriter(samplesOutputFile.getAbsoluteFile()));
                
            } catch (IOException exception) {
                exception.printStackTrace();
            }
            
            
            try{
                samplesWriter.write("! run I\n");
                samplesWriter.write("! event I\n");
                samplesWriter.write("! volume I\n");
                samplesWriter.write("! layer I\n");
                samplesWriter.write("! channel I\n");
                samplesWriter.write("! sample1 I\n");
                samplesWriter.write("! sample2 I\n");
                samplesWriter.write("! sample3 I\n");
                samplesWriter.write("! sample4 I\n");
                samplesWriter.write("! sample5 I\n");
                samplesWriter.write("! sample6 I\n");
                samplesWriter.write("! pedestal D\n");
                
                
            } catch(IOException exception){
                
            }
           
        }
        
        if(batchMode) return;
        
        // Show the plotters        
        for (IPlotter plotter : plotters) plotter.show();

    }

    public void process(EventHeader event) {
    	eventNumber++; 
    	
        if (!event.hasCollection(Track.class, trackCollectionName))
            return;
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        HpsSiSensor sensor = null;
        String plotTitle = null;
        int channel, bad_channel;
        int maxClusterChannel = 0; 
        int hitsPerCluster = 0;  
        ChannelConstants constants = null;
        HPSShapeFitParameters fit = null;
        double clusterAmplitude, maxClusterAmplitude;
        double noise = 0;
        double chiSquared = -1;
        double trkChiSquared = 0;
        double hitTime = 0; 
        double hitX, hitY, pedestal; 
        short[] samples; 

        // Loop over all tracks in an event
        for (Track track : tracks) {
        	trkChiSquared = 0; 
        	trkChiSquared = track.getChi2(); 
        
        	if((new BasicHep3Vector(track.getTrackStates().get(0).getMomentum())).magnitude() <= .500) continue;
            
        	double[] topClusters = new double[10];
            double[] bottomClusters = new double[10];
            // Loop over all stereo hits associated with a track
            hitX = 0; hitY = 0; 
            for (TrackerHit trackerHit : track.getTrackerHits()) {
          	
            	hitX = trackerHit.getPosition()[1];
            	hitY = trackerHit.getPosition()[2];
            	
                // Loop over the strip hits used to crate the stereo hit
            	hitTime = 0; 
                for (HelicalTrackStrip stripHit : ((HelicalTrackCross) trackerHit).getStrips()) {
                	
                	hitTime = stripHit.time(); 
                	
                    sensor = (HpsSiSensor) ((RawTrackerHit) stripHit.rawhits().get(0)).getDetectorElement();
                    if (sensor.isTopLayer())
                        topClusters[sensor.getLayerNumber() - 1] += 1;
                    else
                        bottomClusters[sensor.getLayerNumber() - 1] += 1;

                    maxClusterAmplitude = 0;
                    clusterAmplitude = 0;
                    hitsPerCluster = stripHit.rawhits().size();
                    noise = 0;
                    bad_channel = 0;
                    chiSquared = -1;
                    for (Object rh : stripHit.rawhits()) {

                        RawTrackerHit rawHit = (RawTrackerHit) rh;
                        channel = rawHit.getIdentifierFieldValue("strip");
                        // Check if the channel neighbors a channel that has been tagged as bad
                        if(HPSSVTCalibrationConstants.isBadChannel(sensor, channel+1) 
                        		|| HPSSVTCalibrationConstants.isBadChannel(sensor, channel-1)){
                        	bad_channel = 1; 
                        }
                        
                        if(plotSamples){
                            samples = rawHit.getADCValues();
                            pedestal = sensor.getPedestal(channel);
                            
                            try {
                                if(sensor.isTopLayer()){
                                    samplesWriter.write(runNumber + " " + eventNumber + " 0 " + sensor.getLayerNumber() + " ");
                                } else {
                                    samplesWriter.write(runNumber + " " + eventNumber + " 1 " + sensor.getLayerNumber() + " ");
                                }
                                samplesWriter.write(channel + " " + samples[0] + " " + samples[1] + " " + samples[2] + " "
                                                    + samples[3] + " " + samples[4] + " " + samples[5] +  " " + pedestal + "\n");
                            } catch (IOException exception) {
                                exception.printStackTrace();
                            }
                        }
                        
                        constants = HPSSVTCalibrationConstants.getChannelConstants(sensor, channel);
                        fit = shaperFitter.fitShape(rawHit, constants);
                        if (fit.getAmp() > maxClusterAmplitude) {
                            maxClusterChannel = channel;
                            maxClusterAmplitude = fit.getAmp();
                        }
                        if(stripHit.rawhits().size() == 1){
                        	chiSquared = fit.getChiSq();
                        }
                        noise += Math.pow(sensor.getNoise(channel), 2);
                        clusterAmplitude += fit.getAmp();
                    }

                    noise = Math.sqrt(noise);
                    
                    if (plotMIP) {
                        try {
                            if (sensor.isTopLayer()) {
                                plotTitle = "Top - Layer " + sensor.getLayerNumber() + " - Cluster Charge";
                                performanceWriter.write(runNumber + " " + eventNumber + " 0 " + sensor.getLayerNumber() + " ");
                            } else {
                                plotTitle = "Bottom - Layer " + sensor.getLayerNumber() + " - Cluster Charge";
                                performanceWriter.write(runNumber + " " + eventNumber + " 1 " + sensor.getLayerNumber() + " ");
                            }
                                performanceWriter.write(maxClusterChannel + " " + clusterAmplitude + " " + noise + " " + hitsPerCluster + " " 
                                			 + bad_channel + " " + chiSquared + " " + hitX + " " + hitY + " " + trkChiSquared + " "
                                			 + hitTime + "\n");
                        } catch (IOException exception) {
                        	exception.printStackTrace(); 
                        }
                        if(!batchMode){
                            aida.histogram1D(plotTitle).fill(clusterAmplitude);
                        }
                    }
                }

                // Fill the cluster histograms
                if (plotClustersPerLayer) {
                    for (int layerN = 1; layerN <= 10; layerN++) {
                        if (topClusters[layerN - 1] > 0) {
                            plotTitle = "Top - Layer " + layerN + " - # Clusters";
                            aida.histogram1D(plotTitle).fill(topClusters[layerN - 1]);
                        }
                        if (bottomClusters[layerN - 1] > 0) {
                            plotTitle = "Bottom - Layer " + layerN + " - # Clusters";
                            aida.histogram1D(plotTitle).fill(bottomClusters[layerN - 1]);
                        }
                    }
                }
            }
        }
    }

    /**
     * print debug statements
     */
    public void printDebug(String debugStatement) {
        if (!debug)
            return;
        System.out.println(this.getClass().getSimpleName() + ": " + debugStatement);
    }

    @Override
    public void endOfData() {

        try {
            performanceWriter.close();
            samplesWriter.close(); 
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }
}
