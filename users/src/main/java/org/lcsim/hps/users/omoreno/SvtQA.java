package org.lcsim.hps.users.omoreno;

//--- HEP ---//
import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;

import java.io.BufferedWriter;
import java.io.FileWriter;
//--- Java ---//
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

//--- lcsim ---//
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.seedtracker.SeedTrack;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

//--- hps-java ---//
import org.lcsim.hps.monitoring.deprecated.AIDAFrame;
import org.lcsim.hps.recon.tracking.HPSFittedRawTrackerHit;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants;
import org.lcsim.hps.recon.tracking.HPSSVTCalibrationConstants.ChannelConstants;
import org.lcsim.hps.recon.tracking.HPSShapeFitParameters;
import org.lcsim.hps.recon.tracking.HPSShaperAnalyticFitAlgorithm;
import org.lcsim.hps.recon.tracking.SvtUtils;
import org.lcsim.hps.recon.tracking.TrackUtils;
import org.lcsim.hps.util.Pair;
import org.lcsim.hps.recon.tracking.apv25.SvtReadout;


//--- Constants ---//
import static org.lcsim.hps.recon.tracking.HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR;

/**
 * SVT Quality Assurance Driver
 *  
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SvtQA.java,v 1.7 2013/10/25 19:45:01 jeremy Exp $
 */
public class SvtQA extends Driver {

    private AIDA aida;
    private HPSShaperAnalyticFitAlgorithm shaperFitter = new HPSShaperAnalyticFitAlgorithm();
    private List<AIDAFrame>    frames   = new ArrayList<AIDAFrame>();
    private List<IHistogram1D> histos1D = new ArrayList<IHistogram1D>();
    private List<IHistogram2D> histos2D = new ArrayList<IHistogram2D>();
    private List<IPlotter>     plotters = new ArrayList<IPlotter>();
    private Map<String, double[]> sensorToOccupancy = new HashMap<String, double[]>();
    private Map<String, double[]> sensorToStereoOccupancy = new HashMap<String, double[]>();
    BufferedWriter output = null;

    String outputFile = null;
    String sensorName = null;

    int channelNumber = 0;
    int plotterIndex = 0;
    int apvNumber = 0;	
    double totalNumberEvents = 0;
    double totalNumberOfRawHitEvents = 0;
    double[] totalTopSamples = new double[6];
    double[] totalBottomSamples = new double[6];
    double[] topSamples = new double[6];
    double[] bottomSamples = new double[6];
    double totalNumberOfHits = 0;
    
    double maxOccupancy = 1.0;
    double maxOccupancyVariation = 1000; // %

    boolean verbose = false;
    boolean simulation = false;

    // Plot flags
    boolean enableADCvsChannel = false;
    boolean enableOccupancy    = false;
    boolean enableStereoHitOccupancy = false;
    boolean enableChannelPlots = false;
    boolean enableAPVPlots     = false;
    boolean enableChiSquaredvsChannel = false;
    boolean enableSamples = false;
    boolean enableT0Plots = false;
    boolean enableTotalNumberOfHitsPlots = false;

    // Collection Names
    private String trackCollectionName = "MatchedTracks";
    private String rawHitCollectionName = "SVTRawTrackerHits";
    private String stereoHitCollectionName = "RotatedHelicalTrackHits";
    private String fittedHitCollectionName = "SVTFittedRawTrackerHits";

    /**
     * Default Ctor
     */
    public SvtQA(){
    }

    //--- Setters ---//
    //---------------//
    /**
     * Enable/disable occupancy plots
     */
    public void setEnableOccupancyPlots(boolean enableOccupancy){
        this.enableOccupancy = enableOccupancy;
    }

    /**
     * Enable/disable stereo hit occupancy plots
     */
    public void setEnableStereoHitOccupancyPlots(boolean enableStereoHitOccupancy){
        this.enableStereoHitOccupancy = enableStereoHitOccupancy;
    }

    /**
     * Enable/disable ADC counts vs Channel plots 
     */
    public void setEnableADCvsChannelPlots(boolean enableADCvsChannel){
        this.enableADCvsChannel = enableADCvsChannel;
    }

    /**
     * Enable/disable Channel Plots
     */
    public void setEnableChannelPlots(boolean enableChannelPlots){
        this.enableChannelPlots = enableChannelPlots;
    }

    /**
     * Enable/disable plots associated with individual APVs 
     */
    public void setEnableAPVPlots(boolean enableAPVPlots){
        this.enableAPVPlots = enableAPVPlots;
    }

    /**
     * Enable/disable Chi Squared of fit to samples vs Channel plots 
     */
    public void setEnableChiSquaredvsChannelPlots(boolean enableChiSquaredvsChannel){
        this.enableChiSquaredvsChannel = enableChiSquaredvsChannel;
    }

    /**
     * Enable/disable plots of the APV samples
     */
    public void setEnableSamplesPlots(boolean enableSamples){
        this.enableSamples = enableSamples;
    }

    /**
     * Enable/disable t0 plots 
     */
    public void setEnableT0Plots(boolean enableT0Plots){
        this.enableT0Plots = enableT0Plots;
    }

    /**
     * Set the channel number of interest 
     */
    public void setChannelNumber(int channelNumber){
        this.channelNumber = channelNumber;
    }

    /**
     * Set the sensor of interest 
     */
    public void setSensorName(String sensorName){
        this.sensorName = sensorName;
    }

    /**
     * Set the APV number of interest
     */
    public void setApvNumber(int apvNumber){
        this.apvNumber = apvNumber;
    }

    /**
     * Set the maximum occupancy a channel may have    
     */
    public void setMaxOccupancy(double maxOccupancy){
        this.maxOccupancy = maxOccupancy;
    }

    /**
     * Set the maximum channel to channel occupancy variation
     */
    public void setMaxOccupancyVariation(double maxOccupancyVariation){
        this.maxOccupancyVariation = maxOccupancyVariation;
    }

    /**
     * 
     */
    public void setOutputFileName(String outputFile){
        this.outputFile = outputFile;
    }

    /**
     *
     */
    public void setVerbose(boolean verbose){
        this.verbose = verbose;
    }

    /**
     *
     */
    public void setSimulation(boolean simulation){
        this.simulation = simulation;
    }
    
    /**
     * 
     */
    public void setEnableTotalNumberOfHitsPlots(boolean enableTotalNumberOfHitsPlots){
    	this.enableTotalNumberOfHitsPlots = enableTotalNumberOfHitsPlots;
    }

    /**
     * 
     */
    private int getAPVNumber(int physicalChannel){
        int apv = (int) Math.floor((physicalChannel - TOTAL_STRIPS_PER_SENSOR)/-128);
        if(apv > 4 || apv < 0) throw new RuntimeException("Invalid APV Number: " + apv );
        return apv;
    }  

    protected void detectorChanged(Detector detector){
        super.detectorChanged(detector);

        // setup AIDA
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");

        // Create AIDA Frames
        for(int index = 0; index < 2; index++) frames.add(new AIDAFrame());

        // Set frame titles
        frames.get(0).setTitle("Occupancy");
        frames.get(1).setTitle("ADC Counts");

        //
        Set<SiSensor> sensors = SvtUtils.getInstance().getSensors();
        int plotterIndex = 0;
        IHistogram1D histo1D = null;
        IHistogram2D histo2D = null;
        String title = null;

        //--- Occupancy ---//
        //-----------------//
        if(enableOccupancy){
            plotters.add(PlotUtils.setupPlotter("Occupancy", 5, 4));
            for(SiSensor sensor : sensors){
                sensorToOccupancy.put(SvtUtils.getInstance().getDescription(sensor), new double[640]);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, PlotUtils.getPlotterRegion(sensor), "Channel #", histo1D);
            }
            frames.get(0).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;
        }

        //--- Stereo Hit Occupancy ---//
        //----------------------------//
        if(enableStereoHitOccupancy){
            plotters.add(PlotUtils.setupPlotter("Stereo Hit Occupancy", 5, 4));
            for(SiSensor sensor : sensors){
                sensorToStereoOccupancy.put(SvtUtils.getInstance().getDescription(sensor), new double[640]);
                title = SvtUtils.getInstance().getDescription(sensor) + " - Stereo Hit Occupancy";
                histo1D = aida.histogram1D(title, 640, 0, 639);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, PlotUtils.getPlotterRegion(sensor), "Channel #", histo1D);
            }
            frames.get(0).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;
        }

        //--- ADC Counts vs Channel ---//
        //-----------------------------//
        if(enableADCvsChannel){
            if(sensorName.equals("all")){
                plotters.add(PlotUtils.setupPlotter("ADC Counts vs Channel #", 5, 4));
                for(SiSensor sensor : sensors){
                    title = SvtUtils.getInstance().getDescription(sensor) + " - ADC Counts vs Channel #";
                    histo2D = aida.histogram2D(title, 640, 0, 639, 300, 0, 10000);
                    histos2D.add(histo2D);
                    PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, PlotUtils.getPlotterRegion(sensor), "Channel #", "ADC Counts", histo2D);
                }
                frames.get(1).addPlotter(plotters.get(plotterIndex));
                plotterIndex++;
            } else if(sensorName != null){
                title = sensorName + " - ADC Counts vs Channel #";
                plotters.add(PlotUtils.setupPlotter(title, 0, 0));
                histo2D = aida.histogram2D(title, 640, 0, 639, 300, 0, 10000);
                histos2D.add(histo2D);
                PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 0, "Channel #", "ADC Counts", histo2D);
                frames.get(1).addPlotter(plotters.get(plotterIndex));
                plotterIndex++;
            } else {
                throw new RuntimeException("Sensor of interest is not set!");
            }
        }

        //--- Chi Squared vs Channel ---//
        //------------------------------//
        if(enableChiSquaredvsChannel){
            title = sensorName + " - Chi Squared vs Channel #";
            plotters.add(PlotUtils.setupPlotter(title, 0, 0));
            histo2D = aida.histogram2D(title, 640, 0, 639, 300, 0, 100);
            histos2D.add(histo2D);
            PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 0, "Channel #", "Chi Squared", histo2D);
            frames.get(1).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;
        }


        //--- Single Channel Plots ---//
        //----------------------------//
        if(enableChannelPlots){
            if(sensorName == null) throw new RuntimeException("Sensor of interest is not set!");
            title = sensorName + " - Channel: " + channelNumber;
            plotters.add(PlotUtils.setupPlotter(title, 2, 2));
            title = "ADC Counts";
            histo1D = aida.histogram1D(title, 300, 4000, 7000);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex),title , 0, "ADC Counts", histo1D);
            title = "Shaper Signal Amplitude";
            histo1D = aida.histogram1D(title, 300, 0, 3000);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 1, "Amplitude [ADC Counts]", histo1D);
            title = "t0";
            histo1D = aida.histogram1D(title, 100, -150, 100);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 2, "t0 [ns]", histo1D);
            frames.get(1).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;
        }

        //--- APV Plots ---//
        //-----------------//
        if(enableAPVPlots){
            if(sensorName == null) throw new RuntimeException("Sensor of interest is not set!");
            title = sensorName + " - APV " + apvNumber;
            plotters.add(PlotUtils.setupPlotter(title, 2, 2));
            title = "APV " + apvNumber + " - ADC Counts";
            histo1D = aida.histogram1D(title, 400, 0, 10000);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex),title , 0, "ADC Counts", histo1D);
            title = "APV " + apvNumber + " - Shaper Signal Amplitude";
            histo1D = aida.histogram1D(title, 300, 0, 6000);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 1, "Amplitude [ADC Counts]", histo1D);
            title = "APV " + apvNumber + " - t0";
            histo1D = aida.histogram1D(title, 100, -100, 100);
            histos1D.add(histo1D);
            PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 2, "t0 [ns]", histo1D);
            title = "APV " + apvNumber + " - Amplitude vs t0";
            histo2D = aida.histogram2D(title, 300, 0, 6000, 100, -100, 100);
            histos2D.add(histo2D);
            PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 3, "Amplitude [ADC Counts]", "t0 [ns]", histo2D);
            frames.get(1).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;   
        }

        //--- Samples Amplitude vs Sample Number ---//
        //------------------------------------------//
        if(enableSamples){
            title = "APV Sample Number vs Sample Amplitude";
            plotters.add(PlotUtils.setupPlotter(title, 1, 2));
            plotters.get(plotterIndex).style().zAxisStyle().setParameter("scale", "log");
            title = "APV Sample Number vs Sample Amplitude - Top";
            histo2D = aida.histogram2D(title, 6, 1, 7, 400, 0, 4000);
            histos2D.add(histo2D);
            PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 0, "Sample Number", "Amplitude [ADC Counts]", histo2D);
            title = "APV Sample Number vs Sample Amplitude - Bottom";
            histo2D = aida.histogram2D(title, 6, 1, 7, 400, 0, 4000);
            histos2D.add(histo2D);
            PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, 1, "Sample Number", "Amplitude [ADC Counts]", histo2D);
            frames.get(1).addPlotter(plotters.get(plotterIndex));
            plotterIndex++;
        }

        //--- t0 Plots ---//
        //----------------//
        if(enableT0Plots){
            if(sensorName.equals("all")){
                plotters.add(PlotUtils.setupPlotter("t0 Resolution vs Channel #", 5, 4));
                plotters.get(plotterIndex).style().zAxisStyle().setParameter("scale", "log");
                for(SiSensor sensor : sensors){
                    title = SvtUtils.getInstance().getDescription(sensor) + " - t0 Resolution vs Channel #";
                    histo2D = aida.histogram2D(title, 640, 0, 639, 40, -20, 20);
                    histos2D.add(histo2D);
                    PlotUtils.setup2DRegion(plotters.get(plotterIndex), title, PlotUtils.getPlotterRegion(sensor), "Channel #", "t0 Resolution [ns]", histo2D);
                }
                frames.get(1).addPlotter(plotters.get(plotterIndex));
                plotterIndex++;
            }
            else if(sensorName != null){
                title = sensorName + " - Hit Time Resolution";
                plotters.add(PlotUtils.setupPlotter(title, 0, 0));
                plotters.get(plotterIndex).style().statisticsBoxStyle().setVisible(true);
                histo1D = aida.histogram1D(title, 40, -20, 20);
                histos1D.add(histo1D);
                PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 0, "<Hit Time> - Hit Time [ns]", histo1D);
                frames.get(1).addPlotter(plotters.get(plotterIndex));
                plotterIndex++;
            }
            else throw new RuntimeException("Sensor of interest not set!");

        }
        
        if(enableTotalNumberOfHitsPlots){
        	title = "Total Number of RawTrackerHits";
        	plotters.add(PlotUtils.setupPlotter(title, 0, 0));
        	plotters.get(plotterIndex).style().statisticsBoxStyle().setVisible(true);
        	histo1D = aida.histogram1D(title, 100, 0, 75);
        	histos1D.add(histo1D);
        	PlotUtils.setup1DRegion(plotters.get(plotterIndex), title, 0, "Number of RawTrackerHits", histo1D);
        	frames.get(1).addPlotter(plotters.get(plotterIndex));
        	plotterIndex++;
        }

        for(AIDAFrame frame : frames){
            frame.pack();
            frame.setVisible(true);
        }
    }

    /**
     * 
     */
    public int findPeakSamples(short[] samples){
        int maxSample = 0;
        int maxSampleIndex = 0;
        for(int index = 0; index < samples.length; index++){
            if(maxSample < samples[index]){
                maxSample = samples[index];
                maxSampleIndex = index;
            }
        }
        return maxSampleIndex;
    }



    public void process(EventHeader event){

        totalNumberEvents++;
        String title;

        // If the event doesn't contain RawTrackerHits then skip it
        if(!event.hasCollection(RawTrackerHit.class, rawHitCollectionName )){
            if(verbose) System.out.println("Event doesn't contain RawTrackerHits! Skipping event ...");
            return;
        }

        // Get the RawTrackerHits from the event
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawHitCollectionName);
        
        // Get the total number of RawTrackerHits in the event
        if(enableTotalNumberOfHitsPlots && rawHits.size() != 0){
            aida.histogram1D("Total Number of RawTrackerHits").fill(rawHits.size());
        }

        SiSensor sensor = null;
        ChannelConstants constants = null;
        HPSShapeFitParameters fit = null;
        for(RawTrackerHit rawHit : rawHits){

            // Get the sensor on which this hit occurred
            sensor = (SiSensor) rawHit.getDetectorElement();

            // Get the shaper signal samples
            short[] samples = rawHit.getADCValues();

            // Get the channel number
            int channel = rawHit.getIdentifierFieldValue("strip");

            // Get the APV number
            int apv = this.getAPVNumber(channel);

            // Get the constants associated with this channel
            constants = HPSSVTCalibrationConstants.getChannelConstants(sensor, channel);

            // Fit the samples associated with the RawTrackerHit
            fit = shaperFitter.fitShape(rawHit, constants);

            // Fill the occupancy plots
            if(enableOccupancy){
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                aida.histogram1D(title).fill(channel, 1);
            }

            // Fill ADC vs Channel # plots
            if(enableADCvsChannel && sensorName.equals("all")){
                title = SvtUtils.getInstance().getDescription(sensor) + " - ADC Counts vs Channel #";
                for(short sample : samples){
                    aida.histogram2D(title).fill(channel, sample);
                }
            } else if(enableADCvsChannel && SvtUtils.getInstance().getDescription(sensor).equals(sensorName)){
                title = sensorName + " - ADC Counts vs Channel #";
                for(short sample : samples){
                    aida.histogram2D(title).fill(channel, sample);
                }
            }

            // 
            if(enableChannelPlots && SvtUtils.getInstance().getDescription(sensor).equals(sensorName) && channel == channelNumber){
                title = "ADC Counts";
                for(short sample : samples){
                    aida.histogram1D(title).fill(sample);
                }
                title = "Shaper Signal Amplitude";
                aida.histogram1D(title).fill(fit.getAmp());	    
                System.out.println("Amplitude: " + fit.getAmp());
                title="t0";
                aida.histogram1D(title).fill(fit.getT0());
                System.out.println("t0 " + fit.getT0());
            }

            if(enableAPVPlots && SvtUtils.getInstance().getDescription(sensor).equals(sensorName) && apv == apvNumber ){
                title = "APV " + apvNumber + " - ADC Counts";
                for(short sample : samples){
                    aida.histogram1D(title).fill(sample);
                }
                title = "APV " + apvNumber + " - Shaper Signal Amplitude";
                aida.histogram1D(title).fill(fit.getAmp());     
                title = "APV " + apvNumber + " - t0";
                aida.histogram1D(title).fill(fit.getT0());
                title = "APV " + apvNumber + " - Amplitude vs t0";
                aida.histogram2D(title).fill(fit.getAmp(), fit.getT0());
            }
        }

        /*
        if(enableOccupancy){
            for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
                title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
                aida.histogram1D(title).reset();
                int nEvents = simulation ? SvtReadout.getNumberOfTriggers()  : totalNumberEvents; 
                for(int index = 0; index < 640; index++){
                    aida.histogram1D(title).fill(index, sensorToOccupancy.get(SvtUtils.getInstance().getDescription(sensor))[index]/nEvents);
                }
            }
        }*/

        // If the event doesn't contain FittedRawTrackerHits then skip it
        if(!event.hasCollection(HPSFittedRawTrackerHit.class, fittedHitCollectionName)){
            if(verbose) System.out.println("Event doesn't contain FittedRawTrackerHits! Skipping event ...");
            return;
        }

        // Get the RawTrackerHits from the event
        List<HPSFittedRawTrackerHit> fittedHits = event.get(HPSFittedRawTrackerHit.class, fittedHitCollectionName);
        
        //System.out.println(this.getClass().getSimpleName() + ": Number of FittedRawTrackerHits " + fittedHits.size());

        for(HPSFittedRawTrackerHit fittedHit : fittedHits){


            // Get the channel number
            int channel = fittedHit.getRawTrackerHit().getIdentifierFieldValue("strip");

            // Get the sensor number 
            sensor = (SiSensor) fittedHit.getRawTrackerHit().getDetectorElement();

            // Fill Chi Squared vs Channel # plots
            if(enableChiSquaredvsChannel && SvtUtils.getInstance().getDescription(sensor).equals(sensorName)){
                title = sensorName + " - Chi Squared vs Channel #";
                aida.histogram2D(title).fill(channel, fittedHit.getShapeFitParameters().getChiSq());
            }
        }

        // If the event does not contain stereo hits, skip the event
        if(!event.hasCollection(HelicalTrackHit.class, stereoHitCollectionName)){
            if(verbose) System.out.println("Event doesn't contain HelicalTrackHits! Skipping event ...");
            return;
        }

        // Get the list of HelicalTrackHits
        List<HelicalTrackHit> stereoHits =event.get(HelicalTrackHit.class, stereoHitCollectionName); 

        for(HelicalTrackHit stereoHit : stereoHits){

            double totalT0 = 0;

            for(Object hit : stereoHit.getRawHits()){

                RawTrackerHit rawHit = (RawTrackerHit) hit;

                sensor = (SiSensor) rawHit.getDetectorElement();

                // Get the channel number
                int channel = rawHit.getIdentifierFieldValue("strip");

                // Get the constants associated with this channel
                constants = HPSSVTCalibrationConstants.getChannelConstants(sensor, channel);

                // Fit the samples associated with the RawTrackerHit
                fit = shaperFitter.fitShape(rawHit, constants);

                // Get the shaper signal samples
                short[] samples = rawHit.getADCValues();

                if(enableStereoHitOccupancy){
                    title = SvtUtils.getInstance().getDescription(sensor) + " - Stereo Hit Occupancy";
                    sensorToStereoOccupancy.get(SvtUtils.getInstance().getDescription(sensor))[channel] += 1;
                }

                // Fill Sample Plots
                if(enableSamples){
                    if(fit.getAmp() > 2000 && fit.getAmp() < 6000){
                        for(int sampleN = 1; sampleN <= samples.length; sampleN++){
                            if((sampleN == 1 && totalNumberEvents%5 != 0) || (sampleN == 2 && totalNumberEvents%5 != 0)/* || (sampleN == 3 && totalNumberEvents%3 != 0) */) continue;
                            if(SvtUtils.getInstance().isTopLayer(sensor)){
                                aida.histogram2D("APV Sample Number vs Sample Amplitude - Top").fill(sampleN, samples[sampleN-1] - constants.getPedestal());
                                totalTopSamples[sampleN-1]++;
                                topSamples[sampleN-1] += samples[sampleN-1] - constants.getPedestal();
                            }
                            else{
                            	aida.histogram2D("APV Sample Number vs Sample Amplitude - Bottom").fill(sampleN, samples[sampleN-1] - constants.getPedestal());
                            	totalBottomSamples[sampleN-1]++;
                            	bottomSamples[sampleN-1] += samples[sampleN - 1] - constants.getPedestal();
                            }
                        }
                    }
                }
            }
        }

        /*
        if(enableStereoHitOccupancy){
            for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
                title = SvtUtils.getInstance().getDescription(sensor) + " - Stereo Hit Occupancy";
                aida.histogram1D(title).reset();
                for(int index = 0; index < 640; index++){
                    aida.histogram1D(title).fill(index, sensorToStereoOccupancy.get(SvtUtils.getInstance().getDescription(sensor))[index]/event.getEventNumber());
                }
            }
        }*/
        if(!event.hasCollection(Track.class, trackCollectionName)) return;
        
        // Get the list of tracks in the event
        List<SeedTrack> tracks = event.get(SeedTrack.class, trackCollectionName);

        if(enableT0Plots){

            // Loop over all tracks in the event
            for(Track track : tracks){


                double totalT0 = 0;
                double totalHits = 0;

                // Loop over all stereo hits comprising a track
                for(TrackerHit crossHit : track.getTrackerHits()){

                    HelicalTrackCross htc = (HelicalTrackCross) crossHit;

                    for(HelicalTrackStrip hts : htc.getStrips()){

                        totalT0 += hts.time();
                        totalHits++;
                    }
                }

                double meanT0 = totalT0/totalHits;

                for(TrackerHit crossHit : track.getTrackerHits()){

                    HelicalTrackCross htc = (HelicalTrackCross) crossHit;

                    for(HelicalTrackStrip hts : htc.getStrips()){

                        //SiSensor sensor = null;
                        if(TrackUtils.getZ0(track) > 0){
                            sensor = SvtUtils.getInstance().getSensor(0, hts.layer() - 1);
                        } else if(TrackUtils.getZ0(track) < 0){
                            sensor = SvtUtils.getInstance().getSensor(1, hts.layer() - 1);
                        }

                        int channel = ((RawTrackerHit) hts.rawhits().get(0)).getIdentifierFieldValue("strip");
                        
                        if(sensorName.equals("all")){
                        	aida.histogram2D(SvtUtils.getInstance().getDescription(sensor) + " - t0 Resolution vs Channel #").fill(channel, meanT0 - hts.time());
                        } else {
                        if(SvtUtils.getInstance().getDescription(sensor).equals(sensorName)){
                            aida.histogram1D(sensorName + " - Hit Time Resolution").fill(meanT0 - hts.time());
                        }
                        
                        } 
                    }
                }
            }
        }

    }

    @Override
        public void endOfData(){
    		String title;
    		

    		
            String plotName;
    		if(enableOccupancy){
    			for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
    				title = SvtUtils.getInstance().getDescription(sensor) + " - Occupancy";
    				// Scale the hits per channel by the number of events
    				aida.histogram1D(title).scale(1/totalNumberEvents);
    				
    				// Write the occupancies to a file
    				if(SvtUtils.getInstance().isTopLayer(sensor)){
    					plotName = outputFile + "_top_";
    				} else { 
    					plotName = outputFile + "_bottom_";
    				}
    				
					if(SvtUtils.getInstance().getLayerNumber(sensor) < 10){
						plotName += "0" + SvtUtils.getInstance().getLayerNumber(sensor) + ".dat";
					} else {
						plotName += SvtUtils.getInstance().getLayerNumber(sensor) + ".dat";
					}
    			
	    			// Open the output files stream
	                if(plotName != null){
	                	try{
	                		output = new BufferedWriter(new FileWriter(plotName)); 
                			for(int channel = 0; channel < 640; channel++){
                				output.write(channel + " " + aida.histogram1D(title).binHeight(channel) + "\n");
                			}
                			output.close();
	                	} catch(Exception e) {
	                		System.out.println(this.getClass().getSimpleName() + " :Error! " + e.getMessage());
	                	}
	                }
    			}
    		}
    		
    		if(enableT0Plots){
    			int bins = aida.histogram1D(sensorName + " - Hit Time Resolution").axis().bins();
    			for(int bin = 0; bin < bins; bin++){
    				System.out.println(bin + "        " + aida.histogram1D(sensorName + " - Hit Time Resolution").binHeight(bin));
    			}
    		}
    	
            System.out.println("Total Bad Channels: " + HPSSVTCalibrationConstants.getTotalBadChannels() + "\n");
            	
            	/*
                for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
                    	if(outputFile != null && sensorName.equals(SvtUtils.getInstance().getDescription(sensor))){
                    		try{
                    			for(int channel = 0; channel < 639; channel++){
    								output.write(channel + " " + this.getOccupancy(sensor, channel) + "\n");
    							}
                    			output.close();
                    		} catch(IOException e){
                    			System.out.println(this.getClass().getSimpleName() + ": Error! " + e.getMessage());
                    		}
                    	}
                	
                	System.out.println("%===================================================================%");
                    System.out.println(SvtUtils.getInstance().getDescription(sensor) + " Bad Channels");
                    System.out.println("%===================================================================%");
                    for(int index = 0; index < 640; index++){

                        // Check is the channel can be considered bad    
                        this.checkChannel(sensor, index);
                    }
                }
                System.out.println("%===================================================================% \n");
            }*/

            if(enableStereoHitOccupancy){
                for(SiSensor sensor : SvtUtils.getInstance().getSensors()){
                    System.out.println("%===================================================================% \n");
                    System.out.println(SvtUtils.getInstance().getDescription(sensor) + " Bad Channels");
                    System.out.println("%===================================================================% \n");
                    for(int index = 0; index < 640; index++){
                        // Check is the channel can be considered bad    
                        this.checkChannel(sensor, index);
                    }
                    System.out.println("%===================================================================% \n");
                }
            }

            if(outputFile != null){
                try{
                    aida.saveAs(outputFile);
                } catch(IOException exeption){
                    System.out.println("File " + outputFile + " was not found!");
                }
            }
            
            if(enableSamples){
            	double sigma = 0;
            	double[] topMean = new double[6];
            	double[] bottomMean = new double[6];
            	
                System.out.println("%===================================================================% \n");
            	for(int index = 0; index < topSamples.length; index++){
            		topMean[index] = topSamples[index]/totalTopSamples[index];
            		System.out.println("Top sample " + index + " mean: " + topMean[index]);
            	}
            	
                System.out.println("\n%===================================================================% \n");
            	for(int index = 0; index < bottomSamples.length; index++){
            		bottomMean[index] = bottomSamples[index]/totalBottomSamples[index];
            		System.out.println("Bottom sample " + index + " mean: " + bottomMean[index]);
            	}
                System.out.println("\n%===================================================================% \n");
            }
        }

    public double getOccupancy(SiSensor sensor, int channel){
        if(!enableOccupancy) throw new RuntimeException("Occupancy calculation was not enabled!"); 
        double nEvents = simulation ? SvtReadout.getNumberOfTriggers()  : totalNumberEvents; 
        return sensorToOccupancy.get(SvtUtils.getInstance().getDescription(sensor))[channel]/nEvents;
    }

    public void checkChannel(SiSensor sensor, int channel){
        if(!enableOccupancy) throw new RuntimeException("Occupancy calculation was not enabled!");

        if(HPSSVTCalibrationConstants.isBadChannel(sensor, channel)) return;

        // Find the occupancy of the current channel
        double currentChannelOccu = this.getOccupancy(sensor, channel);

        // If the channel exceeds the maximum allowable occupancy, then it's a bad channel
        if(currentChannelOccu > maxOccupancy){
            System.out.println("Channel " + channel + ": Occupancy " + currentChannelOccu + " -- Exceeds maximum");
            return;
        }

        // Find the occupancy of the neighboring channels
        if(channel == 0){
            double rNeighborOccu = this.getOccupancy(sensor, channel+1);
            if(rNeighborOccu == 0 || HPSSVTCalibrationConstants.isBadChannel(sensor, channel+1)) return; 
            double rOccuDiff = Math.abs(rNeighborOccu - currentChannelOccu)/rNeighborOccu;
            if(rOccuDiff >= maxOccupancyVariation)
                System.out.println("Channel " + channel + ": Channel Variation exceeds maximum -- RVar: " + rOccuDiff);
        } else if(channel == 638){
            double lNeighborOccu = this.getOccupancy(sensor, channel-1);
            if(lNeighborOccu == 0 || HPSSVTCalibrationConstants.isBadChannel(sensor, channel-1)) return;
            double lOccuDiff = Math.abs(lNeighborOccu - currentChannelOccu)/lNeighborOccu;
            if(lOccuDiff >= maxOccupancyVariation)
                System.out.println("Channel " + channel + ": Channel Variation exceeds maximum -- LVar: " + lOccuDiff);
        } else if (channel == 639){
            return;
        }
        else { 
            double rNeighborOccu = this.getOccupancy(sensor, channel+1);  
            double lNeighborOccu = this.getOccupancy(sensor, channel-1);
            if(rNeighborOccu == 0 || HPSSVTCalibrationConstants.isBadChannel(sensor, channel+1)
                    || lNeighborOccu == 0 || HPSSVTCalibrationConstants.isBadChannel(sensor, channel-1)) return;
            double rOccuDiff = Math.abs(rNeighborOccu - currentChannelOccu)/rNeighborOccu;
            double lOccuDiff = Math.abs(lNeighborOccu - currentChannelOccu)/lNeighborOccu;
            if(rOccuDiff >= maxOccupancyVariation && lOccuDiff >= maxOccupancyVariation){
                System.out.println("Channel " + channel + ": Channel Variation exceeds maximum -- LVar: " + lOccuDiff + " RVar: " + rOccuDiff);
            }
        }
    }
}
