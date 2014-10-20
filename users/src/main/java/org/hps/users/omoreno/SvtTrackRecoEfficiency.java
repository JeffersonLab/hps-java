package org.hps.users.omoreno;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.seedtracker.SeedStrategy;
import org.lcsim.recon.tracking.seedtracker.StrategyXMLUtils;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import org.hps.analysis.examples.TrackAnalysis;
import org.hps.recon.tracking.FindableTrack;

/**
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: SvtTrackRecoEfficiency.java,v 1.11 2013/10/14 22:58:03 phansson Exp $ 
 */
public class SvtTrackRecoEfficiency extends Driver {

    private AIDA aida;
    private List<IPlotter>     plotters = new ArrayList<IPlotter>();
    private List<IHistogram1D> histo1D  = new ArrayList<IHistogram1D>();
    private List<IHistogram2D> histo2D  = new ArrayList<IHistogram2D>();
    private Map<Integer, List<SimTrackerHit>> topSimTrackerHitsList = new HashMap<Integer, List<SimTrackerHit>>();
    private Map<Integer, List<SimTrackerHit>> bottomSimTrackerHitsList = new HashMap<Integer, List<SimTrackerHit>>();
    private List<HpsSiSensor> sensors = null;

    FindableTrack findable = null;
    TrackAnalysis trkAnalysis = null;
    RelationalTable<SimTrackerHit, MCParticle> simHitToMcParticle;
    RelationalTable<SimTrackerHit, RawTrackerHit> simHitToRawHit;
    BufferedWriter efficiencyOutput = null;
    BufferedWriter momentumOutput = null;

    // Collection Names
    String simTrackerHitCollectionName = "TrackerHits";
    String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    String fittedRawTrackerHitCollectionName = "SVTFittedRawTrackerHits";
    String trackCollectionName = "MatchedTracks";
    String stereoHitCollectionName = "RotatedHelicalTrackHits";
    String siTrackerHitCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    int eventNumber = 0;
    int plotterIndex, histo1DIndex, histo2DIndex;
    int[] topSimTrackerHits;
    int[] bottomSimTrackerHits;
    double findableTracks, foundTracks;
    double topPossibleTracks, bottomPossibleTracks, possibleTracks;
    double totalTopTracks, totalBottomTracks, totalTracks;
    int totalLayersHit = 10;
    int totalSvtLayers = 10; 
    
    String efficiencyOutputFile = null;
    String momentumOutputFile = null;
    String strategyResourcePath = null;
    
    boolean debug = false;
    boolean trackingEfficiencyPlots = true;
    boolean trackMatch = false;
    boolean trackIsFindable = false;
    boolean isTopTrack  = false;

    /**
     *  Enable/Disable debug
     */
    public void setDebug(boolean debug){
        this.debug = debug;
    }
    
    /**
     * Set the name of the file to output efficiency data to
     */
    public void setEfficiencyOutputFile(String efficiencyOutputFile){
    	this.efficiencyOutputFile = efficiencyOutputFile;
    }

    /**
     * Set the name of the file to output momentum data to
     */
    public void setMomentumOutputFile(String momentumOutputFile){
        this.momentumOutputFile = momentumOutputFile;
    }
    
    /**
     * Set the required number of layers an MC particle must hit 
     */
    public void setTotalLayersHit(int totalLayersHit){
        if(totalLayersHit%2 == 1) throw new RuntimeException(this.getClass().getSimpleName() + ": Total number of layers hit must be even");
        this.totalLayersHit = totalLayersHit;
    }

    /**
     * Print a debug statement
     * 
     * @param message : debug message
     */
    private void printDebug(String message){
    	if(debug){
    		System.out.println(this.getClass().getSimpleName() + ": " + message);
    	}
    }
    
    /**
     * 
     */
    protected void detectorChanged(Detector detector){
    	super.detectorChanged(detector);
    	
    	sensors = detector.getSubdetector("Tracker").getDetectorElement().findDescendants(HpsSiSensor.class);
    	
        // setup AIDA
        aida = AIDA.defaultInstance();
        aida.tree().cd("/");
        
        // Open the output file stream
        if(efficiencyOutputFile != null && momentumOutputFile != null){
        	try{
        		efficiencyOutput = new BufferedWriter(new FileWriter(efficiencyOutputFile));
                momentumOutput = new BufferedWriter(new FileWriter(momentumOutputFile));
        	} catch(Exception e){
        		System.out.println(this.getClass().getSimpleName() + ": Error! " + e.getMessage());
        	}
        }
        
        // Get the total number of SVT layers
        // TODO: Get the total number of Si planes from the SVT geometry 
        totalSvtLayers = sensors.size()/2; 
        System.out.println("The SVT has a total of " + totalSvtLayers + " layers");
        
        
        // Initialize the Layer to RawTrackerHit maps
        for(int index = 0; index < 10; index++){
            topSimTrackerHitsList.put(index + 1, new ArrayList<SimTrackerHit>());
            bottomSimTrackerHitsList.put(index + 1, new ArrayList<SimTrackerHit>());
        }

        if(trackingEfficiencyPlots){
        	plotters.add(PlotUtils.setupPlotter("Track Momentum", 0, 0));
        	histo1D.add(aida.histogram1D("Momentum - Reconstructed Tracks", 14, 0, 5.6));
        	PlotUtils.setup1DRegion(plotters.get(plotterIndex), "Reconstructed Tracks", 0, "Momentum [GeV]", histo1D.get(histo1DIndex));
            histo1DIndex++;            
            histo1D.add(aida.histogram1D("Momentum - Findable Tracks", 14, 0, 5.6));
        	PlotUtils.setup1DRegion(plotters.get(plotterIndex), "Findable Tracks", 0, "Momentum [GeV]", histo1D.get(histo1DIndex));
        	plotterIndex++;
        	histo1DIndex++;
        }
        
        for(IPlotter plotter : plotters){
        	plotter.show();
        }
    }
    
    private String samplesToString(short[] samples){
    	String sampleList = "[ ";
    	for(short sample : samples){
    		sampleList += Short.toString(sample) + ", ";
    	}
    	sampleList += "]";
    	return sampleList;
    }

    /**
     * Dflt Ctor
     */
    public SvtTrackRecoEfficiency(){}

    @Override
    protected void process(EventHeader event){
        
    	// For now, only look at events with a single track
        if(event.get(Track.class, trackCollectionName).size() > 1) return;
        eventNumber++;

        // If the event doesn't contain SimTrackerHits, skip the event
        if(!event.hasCollection(SimTrackerHit.class, simTrackerHitCollectionName)) return;
        List<SimTrackerHit> simTrackerHits = event.get(SimTrackerHit.class, simTrackerHitCollectionName);
        this.printDebug("\nEvent " + eventNumber + " contains " + simTrackerHits.size() + " SimTrackerHits");
    	// Loop through all SimTrackerHits and confirm that a corresponding RawTrackerHit was created
    	for(SimTrackerHit simTrackHit : simTrackerHits){
    		
    		this.printDebug("SimTrackerHit Layer Number: " + simTrackHit.getLayerNumber());
    	}

        // Get the list of RawTrackerHits and add them to the sensor readout
        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        String volume; 
        for(RawTrackerHit rawHit : rawHits){
        	HpsSiSensor sensor = (HpsSiSensor) rawHit.getDetectorElement();
        	if(sensor.isTopLayer()){
        		volume = "Top Volume ";
        	} else { 
        		volume = "Bottom Volume ";
        	}
    		this.printDebug(volume + "RawTrackerHit Channel #: " + rawHit.getIdentifierFieldValue("strip") + " Layer Number: " + rawHit.getLayerNumber()
    				+ " Samples: " + samplesToString(rawHit.getADCValues()));
            ((HpsSiSensor) rawHit.getDetectorElement()).getReadout().addHit(rawHit);
        }
        
        if(event.hasCollection(SiTrackerHit.class, siTrackerHitCollectionName)){
        	List<SiTrackerHit> hitlist = event.get(SiTrackerHit.class, siTrackerHitCollectionName);
        	for(SiTrackerHit siTrackerHit : hitlist){
    			this.printDebug("Cluster is comprised by the following raw hits:");
        		for(RawTrackerHit rawHit : siTrackerHit.getRawHits()){
            		this.printDebug("RawTrackerHit Channel #: " + rawHit.getIdentifierFieldValue("strip") + " Layer Number: " + rawHit.getLayerNumber());
        		}
        	}
        }
        
        // Get the MC Particles associated with the SimTrackerHits
        List<MCParticle> mcParticles = event.getMCParticles();
        if(debug){
        	String particleList = "[ ";
        	for(MCParticle mcParticle : mcParticles){
        		particleList += mcParticle.getPDGID() + ", ";
        	}
        	particleList += "]";
        	this.printDebug("MC Particles: " + particleList);
        }
        
        // Get the magnetic field
        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        //this.printDebug("BField: " + event.getDetector().getFieldMap().getField(IP).y());
        
        // Check if the MC particle track should be found by the tracking algorithm
        findable = new FindableTrack(event, simTrackerHits, totalSvtLayers);
        
        // Use an iterator to avoid ConcurrentModificationException
        Iterator<MCParticle> mcParticleIterator = mcParticles.iterator();
        trackIsFindable = false;
        while(mcParticleIterator.hasNext()){
            MCParticle mcParticle = mcParticleIterator.next();
            if(findable.isTrackFindable(mcParticle, totalLayersHit)){
                
                // Check that all SimTrackerHits are within the same detector volume
                Set<SimTrackerHit> trackerHits = findable.getSimTrackerHits(mcParticle);
                if(this.isSameSvtVolume(trackerHits)){
                    if(debug){
                    	this.printDebug("Track is findable");
                    	this.printDebug("MC particle momentum: " + mcParticle.getMomentum().toString());
                    }
                    	
                    findableTracks++;
                    trackIsFindable = true;      
                }
            } else {
                mcParticleIterator.remove();
            }
        }
        
        // Check if the event contains a Track collection, otherwise return
        if(!event.hasCollection(Track.class, trackCollectionName)) return;
        List<Track> tracks = event.get(Track.class, trackCollectionName);
        this.printDebug("Event " + eventNumber + " contains " + tracks.size() + " Tracks");
        
        // Relate a stereo hits to a SimTrackerHit; This is a required argument by TrackAnalysis
        List<HelicalTrackHit> stereoHits = event.get(HelicalTrackHit.class, stereoHitCollectionName);
        this.printDebug("Event " + eventNumber + " contains " + stereoHits.size() + " HelicalTrackHits");
        RelationalTable<HelicalTrackHit, MCParticle> hitToMC = stereoHitToMC(stereoHits, simTrackerHits);
        
        // Check if an MC particle is related to a found track
        for(Track track : tracks){
            trkAnalysis = new TrackAnalysis(track, hitToMC);
            if(mcParticles.contains(trkAnalysis.getMCParticle())){
               if(debug)
                    this.printDebug("Track match found");
                foundTracks++;
                if(trackingEfficiencyPlots){
                    aida.histogram1D("Momentum - Reconstructed Tracks").fill(trkAnalysis.getMCParticle().getMomentum().magnitude());
                    aida.histogram1D("Momentum - Findable Tracks").fill(trkAnalysis.getMCParticle().getMomentum().magnitude());
                }
                mcParticles.remove(trkAnalysis.getMCParticle());
            }
        }
        
        if(!mcParticles.isEmpty() && trackingEfficiencyPlots){
            // If the list still contains MC Particles, a matching track wasn't found
        	this.printDebug("No matching track found");
        	
            // Check that all stereoHits were correctly assigned to an MCParticle
            for(MCParticle mcParticle : mcParticles){
                
                // Check if there is a stereo hit associated with every pair of layers hit by the MC particle
                Set<SimTrackerHit> simHits = findable.getSimTrackerHits(mcParticle);
                boolean[] planesHit = new boolean[totalSvtLayers];
                
                // Clear the sensor readout's and then add the SimTrackerHits from  the MC particles to them
                for(HpsSiSensor sensor : sensors) sensor.getReadout().clear();
                for(SimTrackerHit simHitTrackerHit : simHits){
                    ((HpsSiSensor) simHitTrackerHit.getDetectorElement()).getReadout().addHit(simHitTrackerHit);
                }
                
                // Clear all previously stored simTrackerHits
                for(int index = 0; index < 10; index++){
                    topSimTrackerHitsList.get(index+1).clear();
                    bottomSimTrackerHitsList.get(index+1).clear();
                }
                
                // Determine if the MC particle passed through the top or bottom SVT volume
                for(SimTrackerHit simHit : simHits){
                	HpsSiSensor sensor = (HpsSiSensor) simHit.getDetectorElement();
                    if(sensor.isTopLayer()){
                        this.printDebug("MC Particle passed through the top layer");
                        isTopTrack = true;
                        break;
                    } else {
                        this.printDebug("MC Particle passed through the bottom layer");
                        isTopTrack = false;
                        break;
                    }
                }
                
                // Check which layers have SimTrackerHits
                // Arrange them by layers
                for(SimTrackerHit simHit : simHits){
                    planesHit[simHit.getLayer()-1] = true;
                }
                // TODO: Get the number of SVT layers from the geometry
                boolean[] layerHit = new boolean[6];
                int layerN = 0;
                for(int index = 0; index < planesHit.length; index+=2){
                    if(planesHit[index] && planesHit[index+1]){
                        layerHit[layerN] = true;
                        this.printDebug("Layer " + (layerN+1) + " was hit");
                    }
                    layerN++;
                }
               
                // Check which layers have a stereo hit associated with them
                // TODO: Get the number of detector layers from the geometry
                boolean[] topStereoLayerHit = new boolean[6];
                boolean[] bottomStereoLayerHit = new boolean[6];
                for(HelicalTrackHit stereoHit : stereoHits){
                    if(stereoHit.getCorrectedPosition().z() > 0){                    
                        topStereoLayerHit[(stereoHit.Layer() - 1)/2] = true;
                    } else { 
                        bottomStereoLayerHit[(stereoHit.Layer() - 1)/2] = true;
                    }
                }
      
                aida.histogram1D("Momentum - Findable Tracks").fill(mcParticle.getMomentum().magnitude());
                
            }
        }
    }
    
    private boolean isSameSvtVolume(Set<SimTrackerHit> simTrackerHits)
    {
        int volumeIndex = 0;
        for(SimTrackerHit simTrackerHit : simTrackerHits){
        	HpsSiSensor sensor = (HpsSiSensor) simTrackerHit.getDetectorElement();
            if(sensor.isTopLayer()) volumeIndex++;
            else volumeIndex--;
        }
        return Math.abs(volumeIndex) == simTrackerHits.size();
    }

    private RelationalTable<HelicalTrackHit, MCParticle> stereoHitToMC(List<HelicalTrackHit> stereoHits, List<SimTrackerHit> simTrackerHits)
    {
        RelationalTable<HelicalTrackHit, MCParticle> hitToMC 
            = new BaseRelationalTable<HelicalTrackHit, MCParticle>(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        Map<Integer, List<SimTrackerHit>> layerToSimTrackerHit = new HashMap<Integer, List<SimTrackerHit>>();
        
        // Sort the SimTrackerHits by Layer
        for(SimTrackerHit simTrackerHit : simTrackerHits){
            if(!layerToSimTrackerHit.containsKey(simTrackerHit.getLayer()))
                layerToSimTrackerHit.put(simTrackerHit.getLayer(), new ArrayList<SimTrackerHit>());
            layerToSimTrackerHit.get(simTrackerHit.getLayer()).add(simTrackerHit);
        }
        
        // 
        for(HelicalTrackHit stereoHit : stereoHits){
            HelicalTrackCross cross = (HelicalTrackCross) stereoHit;
                for(HelicalTrackStrip strip : cross.getStrips()){
                    // If there is only a single SimTrackerHit on a layer and it's in the same volume as the
                    // stereo hit then they likely match to each other
                    if(layerToSimTrackerHit.get(strip.layer()) != null && layerToSimTrackerHit.get(strip.layer()).size() == 1){
                        Hep3Vector simTrackerHitPosition = layerToSimTrackerHit.get(strip.layer()).get(0).getPositionVec();
                        if(Math.signum(simTrackerHitPosition.y()) == Math.signum(this.getClusterPosition(strip).z())){
                            hitToMC.add(stereoHit, layerToSimTrackerHit.get(strip.layer()).get(0).getMCParticle()); 
                            layerToSimTrackerHit.remove(strip.layer());
                            //System.out.println(this.getClass().getSimpleName() + ": SimTrackerHit position: " + simTrackerHitPosition.toString());
                            //System.out.println(this.getClass().getSimpleName() + ": Cluster position: " + this.getClusterPosition(strip));
                        } else {
                            //System.out.println(this.getClass().getSimpleName() + ": Cluster and SimTrackerHit are on different volumes");
                        }
                    }
                    else if(layerToSimTrackerHit.get(strip.layer()) != null && layerToSimTrackerHit.get(strip.layer()).size() > 1){
                        //System.out.println(this.getClass().getSimpleName() + ": Layer with multiple hits found.");
                        double deltaZ = Double.MAX_VALUE;
                        SimTrackerHit simTrackerHitMatch = null;
                        for(SimTrackerHit simTrackerHit : layerToSimTrackerHit.get(strip.layer())){
                            if(Math.abs(simTrackerHit.getPositionVec().y() - this.getClusterPosition(strip).z()) < deltaZ){
                                deltaZ = Math.abs(simTrackerHit.getPositionVec().y() - this.getClusterPosition(strip).z());
                                simTrackerHitMatch = simTrackerHit;
                            }
                        }
                        hitToMC.add(stereoHit, simTrackerHitMatch.getMCParticle()); 
                        layerToSimTrackerHit.remove(strip.layer()).remove(simTrackerHitMatch);
                        //System.out.println(this.getClass().getSimpleName() + ": SimTrackerHit position: " + simTrackerHitMatch.getPositionVec().toString());
                        //System.out.println(this.getClass().getSimpleName() + ": Cluster position: " + this.getClusterPosition(strip));
                    }
                }
            }

        return hitToMC;
    }
    
    private List<SeedStrategy> getStrategyList(){
        if(!strategyResourcePath.startsWith("/"))
            strategyResourcePath = "/" + strategyResourcePath;
    
        return StrategyXMLUtils.getStrategyListFromInputStream(this.getClass().getResourceAsStream(strategyResourcePath));
    }
    
    private Hep3Vector getClusterPosition(HelicalTrackStrip strip)
    {
        Hep3Vector origin = strip.origin();
        Hep3Vector u = strip.u();
        double umeas = strip.umeas();
        Hep3Vector uvec = VecOp.mult(umeas, u);
        return VecOp.add(origin, uvec);
    }

    
   @Override
   public void endOfData()
   { 
	   
       if(trackingEfficiencyPlots && efficiencyOutputFile != null && momentumOutputFile != null){
	   	   try{ 
	   		   int bins = aida.histogram1D("Momentum - Findable Tracks").axis().bins();
	   		   for(int index = 0; index < bins; index++){
	   			   if(aida.histogram1D("Momentum - Reconstructed Tracks").binEntries(index) == 0) efficiencyOutput.write(index + " " + 0 + "\n");
	   			   else	efficiencyOutput.write(index + " " + aida.histogram1D("Momentum - Reconstructed Tracks").binEntries(index) + "\n");
	   			   
                   if(aida.histogram1D("Momentum - Findable Tracks").binEntries(index) == 0) momentumOutput.write(index + " " + 0 + "\n");
	   			   else momentumOutput.write(index + " " + aida.histogram1D("Momentum - Findable Tracks").binEntries(index) + "\n");
	   		   }
	   		   efficiencyOutput.close();
               momentumOutput.close();
	   	   } catch(IOException e){
	   		   System.out.println(this.getClass().getSimpleName() + ": Error! " + e.getMessage());
	   	   }
       } 
	   
        System.out.println("%===============================================================%");
        System.out.println("%============== Track Reconstruction Efficiencies ==============%");
        System.out.println("%===============================================================%\n%");
        if(findableTracks > 0){
            System.out.println("% Total Track Reconstruction Efficiency: " + foundTracks + "/" + findableTracks + "=" + (foundTracks/findableTracks)*100 + "%");
        }
        System.out.println("%\n%===============================================================%");
    } 
}
