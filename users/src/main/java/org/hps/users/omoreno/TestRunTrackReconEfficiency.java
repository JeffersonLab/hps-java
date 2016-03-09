package org.hps.users.omoreno;

//--- java ---//
//--- hep ---//
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.List;

import org.hps.recon.tracking.TrackUtils;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
//--- lcsim ---//
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
//--- hps-java ---//

/**
 * Class to calculate track reconstruction efficiency using a tag and probe
 * method. 
 * 
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: TestRunTrackReconEfficiency.java,v 1.5 2013/10/14 22:58:04 phansson Exp $
 */
public class TestRunTrackReconEfficiency  extends Driver {
    
    private AIDA aida;
    private List<IPlotter>     plotters = new ArrayList<IPlotter>();
    private List<IHistogram1D> histo1D = new ArrayList<IHistogram1D>();
    List<Track> topTracks; 
    List<Track> botTracks;
    
    int plotterIndex = 0; 

    double eventNumber     = 0;
    double nOppositeVolume = 0;
    double nWithinWindow   = 0;
    double nAboveThreshold = 0;
    double nTrigClusterTrackMatch = 0;
    double findableTracks, findableTopTracks,findableBottomTracks;
    double totalTracks, totalTopTracks, totalBottomTracks; 
    double findableSingleTracks;
    double findableSingleTracksQuad1, findableSingleTracksQuad2;
    double findableSingleTracksQuad3, findableSingleTracksQuad4;
    double foundSingleTracks;
    double foundSingleTracksQuad1, foundSingleTracksQuad2;
    double foundSingleTracksQuad3, foundSingleTracksQuad4;
    double thresholdEnergy  = 0;
    double energyDifference = 0; 
    
    boolean debug = false;  
    boolean topTrackIsFindable, bottomTrackIsFindable;
    boolean topTrigger = false;
     
    // Collection Names
    String stereoHitCollectionName      = "HelicalTrackHits";
    String trackCollectionName          = "MatchedTracks";
    String ecalClustersCollectionName   = "EcalClusters";
    String triggerDataCollectionName    = "TriggerBank";
    
    // Plots
    IHistogram1D findableTrackMomentum; 
    IHistogram1D totalTrackMomentum; 
    IHistogram1D xPositionResidual;
    IHistogram1D yPositionResidual;
    IHistogram1D zPositionResidual;
    IHistogram1D r;

    /**
     * Dflt Ctor
     */
    public TestRunTrackReconEfficiency(){};

    /**
     * Enable/disble debug mode
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }
    
    /**
     * 
     */
    public void setThresholdEnergy(double thresholdEnergy){
        this.thresholdEnergy = thresholdEnergy;
    }

    public void setClusterEnergyDifference(double energyDifference){ 
        this.energyDifference = energyDifference; 
    }
     
    /**
     * 
     */
    protected void detectorChanged(Detector detector){
        super.detectorChanged(detector);

        // setup AIDA
        aida = AIDA.defaultInstance(); 
        aida.tree().cd("/"); 

        // Open the output file stream
    
        // Create plots of track reconstruction effiency vs momentum
        plotters.add(PlotUtils.setupPlotter("Track Momentum", 0, 0));
        findableTrackMomentum = aida.histogram1D("Momentum - Findable Tracks", 14, 0, 5.6); 
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "Momentum - Findable Tracks", 0, "Momentum [GeV]", findableTrackMomentum); 
        totalTrackMomentum = aida.histogram1D("Momentum - Reconstructed Tracks", 14, 0, 5.6); 
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "Momentum - Reconstructed Tracks", 0, "Momentum [GeV]", totalTrackMomentum);
        plotterIndex++;

        // Create plot for diffence in track and cluster position
        plotters.add(PlotUtils.setupPlotter("Track-Cluster Position Residual", 2, 2));
        xPositionResidual = aida.histogram1D("x Residual", 100, -100, 100);
        yPositionResidual = aida.histogram1D("y Residual", 100, -100, 100);
        zPositionResidual = aida.histogram1D("z Residual", 100, -100, 100);
        r = aida.histogram1D("r", 100, -100, 100);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "x Residual", 0, "delta x [mm]", xPositionResidual);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "y Residual", 1, "delta y [mm]", yPositionResidual);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "z Residual", 2, "delta z [mm]", zPositionResidual);
        PlotUtils.setup1DRegion(plotters.get(plotterIndex), "r", 3, "r [mm]", r);
        plotterIndex++; 

        // Show all of the plotters
        //for(IPlotter plotter : plotters) plotter.show(); 

        
    }
    
    /**
     * 
     */
    protected void process(EventHeader event)
    {
        eventNumber++;
        
        // First check if the event contains a collection of Ecal clusters.  If
        // it doesn't skip the event.
        if(!event.hasCollection(Cluster.class, ecalClustersCollectionName)) return;
       
        // Get the list of Ecal clusters in the event
        List<Cluster> ecalClusters = event.get(Cluster.class, ecalClustersCollectionName);
       
        // Get the list of tracks from the event
        List<Track> tracks = event.get(Track.class, trackCollectionName);

        // If the event has a single Ecal cluster satisfying the threshold cut, 
        // check if there is a track that is well matched to the cluster
        if(ecalClusters.size() == 1){
            Cluster ecalCluster = ecalClusters.get(0);
            
            // If the cluster is above the energy threshold, then the track should
            // be findable
            if(!isClusterAboveEnergyThreshold(ecalCluster)) return;
            findableSingleTracks++;
            
            double[] clusterPosition = ecalCluster.getPosition();
            
            if(clusterPosition[0] > 0 && clusterPosition[1] > 0)       findableSingleTracksQuad1++;
            else if(clusterPosition[0] < 0 && clusterPosition[1] > 0) findableSingleTracksQuad2++;
            else if(clusterPosition[0] < 0 && clusterPosition[1] < 0) findableSingleTracksQuad3++;
            else if(clusterPosition[0] > 0 && clusterPosition[1] < 0) findableSingleTracksQuad4++;  
            
            if(!isClusterMatchedToTrack(ecalCluster, tracks)) return;
            foundSingleTracks++;

            if(clusterPosition[0] > 0 && clusterPosition[1] > 0)       foundSingleTracksQuad1++;
            else if(clusterPosition[0] < 0 && clusterPosition[1] > 0) foundSingleTracksQuad2++;
            else if(clusterPosition[0] < 0 && clusterPosition[1] < 0) foundSingleTracksQuad3++;
            else if(clusterPosition[0] > 0 && clusterPosition[1] < 0) foundSingleTracksQuad4++; 
        }
        
        // Only look at events which have two Ecal cluster 
        if(ecalClusters.size() != 2) return;

        // Check that the Ecal clusters are in opposite Ecal volumes. If 
        // they don't, skip the event.
        if(!hasClustersInOppositeVolumes(ecalClusters)){
            this.printDebug("Ecal clusters are not in opposite volumes");
            return;
        }
        nOppositeVolume++;
      
        // Check that the Ecal clusters lie within some pre-defined window. If
        // they don't, skip the event. 
        if(!isClusterWithinWindow(ecalClusters.get(0)) || !isClusterWithinWindow(ecalClusters.get(1))){
                this.printDebug("Ecal cluster falls outside of window.");
                return;         
        }
        nWithinWindow++;
       
        // Check that the Ecal clusters are above the threshold energy.  If 
        // they don't, skip the event.
        if(!isClusterAboveEnergyThreshold(ecalClusters.get(0)) || !isClusterAboveEnergyThreshold(ecalClusters.get(1))){
                this.printDebug("Ecal cluster energies are below threshold.");
                return;                     
        }
        nAboveThreshold++;
 
        // Check that the difference between the Ecal cluster energies is 
        // reasonable
        double energyDiff = Math.abs(ecalClusters.get(0).getEnergy() - ecalClusters.get(1).getEnergy()); 
        if(energyDiff > energyDifference){
            this.printDebug("The energy difference between the two clusters is too great.");
            return;
        }
       
        // Check if the event contains a collection of tracks.  If it doesn't,
        // move on to the next event.
        if(!event.hasCollection(Track.class, trackCollectionName)){
            this.printDebug("Event doesn't contain a collection of tracks!");
            return;
        }


        // If there are no tracks in the collection, move on to the next event. 
        if(tracks.isEmpty()){
            this.printDebug("Event doesn't contain any tracks!");
            return;  
        }
       
        // Sort the tracks by SVT volume
        topTracks = new ArrayList<Track>();
        botTracks = new ArrayList<Track>();
        for(Track track : tracks){
            if(track.getTrackStates().get(0).getZ0() > 0)  topTracks.add(track);
            else if(track.getTrackStates().get(0).getZ0() < 0) botTracks.add(track);
        }
        
        // Get the trigger information from the event
        List<GenericObject> triggerData = event.get(GenericObject.class, triggerDataCollectionName);
        GenericObject triggerDatum = triggerData.get(0);
        if(triggerDatum.getIntVal(4) > 0){
            this.printDebug("Ecal triggered by top cluster");
            topTrigger = true;
        } else if(triggerDatum.getIntVal(5) > 0){
            this.printDebug("Ecal triggered by bottom cluster");            
            topTrigger = false;
        }
        
        // Match a track to the trigger cluster
        Cluster matchedCluster = null; 
        for(Cluster ecalCluster : ecalClusters){
            if(ecalCluster.getPosition()[1] > 0 && topTrigger){
                if(!isClusterMatchedToTrack(ecalCluster, topTracks)){
                    this.printDebug("Trigger cluster-track match was not found.");
                    return;
                }
                matchedCluster = ecalCluster; 
                findableBottomTracks++;
                break;
            } else if( ecalCluster.getPosition()[1] < 0 && !topTrigger){
                if(!isClusterMatchedToTrack(ecalCluster, botTracks)){
                    this.printDebug("Trigger cluster-track match was not found.");
                    return;
                }
                matchedCluster = ecalCluster;
                findableTopTracks++;
                break;
            }
        }
        if(matchedCluster != null) ecalClusters.remove(matchedCluster);
        nTrigClusterTrackMatch++;
        
        // If the cluster passes all requirements, then there is likely a track
        // associated with it
        findableTracks++;
        
        // Now check if a track is associated with the non-trigger cluster
        if(topTrigger){
                if(!isClusterMatchedToTrack(ecalClusters.get(0), botTracks)){
                    this.printDebug("Non trigger cluster-track match was not found.");
                    return;
                }
                totalBottomTracks++;
        } else if(!topTrigger){
                if(!isClusterMatchedToTrack(ecalClusters.get(0), topTracks)){
                    this.printDebug("Non trigger cluster-track match was not found.");
                    return;
                }                  
                totalTopTracks++;
        } 
        ++totalTracks; 
    }    

    /**
     * Print a debug message if they are enabled.
     * 
     * @param debugMessage : message to be printed
     */ 
    private void printDebug(String debugMessage){
        if(debug){
            System.out.println(this.getClass().getSimpleName() + ": " + debugMessage);
        }
    }
    
    /**
     * 
     */
    private boolean isClusterWithinWindow(Cluster clusterPosition){
        return true;
    }
    
    /**
     * 
     */
    private boolean isClusterAboveEnergyThreshold(Cluster ecalCluster){
        if(ecalCluster.getEnergy() > thresholdEnergy) return true;
        return false;
    }
     
    /**
     * 
     */
    private boolean hasClustersInOppositeVolumes(List<Cluster> ecalClusters){
        this.printPosition(ecalClusters.get(0).getPosition());
        this.printPosition(ecalClusters.get(1).getPosition());
        if((ecalClusters.get(0).getPosition()[1] > 0 && ecalClusters.get(1).getPosition()[1] < 0)
                || (ecalClusters.get(0).getPosition()[1] < 0 && ecalClusters.get(1).getPosition()[1] > 0)){
            return true;    
        }
        return false;
    }
    
    /**
     * 
     */
    private boolean isClusterMatchedToTrack(Cluster cluster, List<Track> tracks){
        Hep3Vector clusterPos = new BasicHep3Vector(cluster.getPosition());
        double rMax = Double.MAX_VALUE;
        Track matchedTrack = null; 
        for(Track track : tracks){
            
            Hep3Vector trkPosAtShowerMax = TrackUtils.extrapolateTrack(track,clusterPos.z());
            if(Double.isNaN(trkPosAtShowerMax.x()) || Double.isNaN(trkPosAtShowerMax.y())){
                this.printDebug("Invalid track position");
                return false; 
            }
            this.printDebug("Track position at shower max: " + trkPosAtShowerMax.toString());
 
            // Find the distance between the track position at shower
            // max and the cluster position
            double r = VecOp.sub(trkPosAtShowerMax, clusterPos).magnitude();
            this.printDebug("Distance between Ecal cluster and track position at shower max: " + r + " mm");
            
            // Check if the track is the closest to the cluster.  If it is, then
            // save the track and contineu looping over all other tracks
            if (r < rMax /*&& r <= maxTrackClusterDistance*/) {
                rMax = r;
                matchedTrack = track;
            }
        }
        if(matchedTrack != null) return true;
        return false;
    }
    
    /**
     * 
     */
    private void printPosition(double[] position){
        this.printDebug("[ " + position[0] + ", " + position[1] + ", " + position[2] + " ]");
    }
    
    
    @Override
    public void endOfData(){ 
        System.out.println("%===================================================================% \n");
        if(findableSingleTracks > 0){
            System.out.println("% Total single track efficiency: " + foundSingleTracks + " / " + findableSingleTracks + " = " + (foundSingleTracks/findableSingleTracks)*100 + "%");
        }
        if(findableSingleTracksQuad1 > 0){
            System.out.println("% Total single track efficiency - Quad 1: " + foundSingleTracksQuad1 + " / " + findableSingleTracksQuad1 + " = " + (foundSingleTracksQuad1/findableSingleTracksQuad1)*100 + "%");
        }
        if(findableSingleTracksQuad2 > 0){
            System.out.println("% Total single track efficiency - Quad 2: " + foundSingleTracksQuad2 + " / " + findableSingleTracksQuad2 + " = " + (foundSingleTracksQuad2/findableSingleTracksQuad2)*100 + "%");
        }
        if(findableSingleTracksQuad3 > 0){
            System.out.println("% Total single track efficiency - Quad 3: " + foundSingleTracksQuad3 + " / " + findableSingleTracksQuad3 + " = " + (foundSingleTracksQuad3/findableSingleTracksQuad3)*100 + "%");
        }
        if(findableSingleTracksQuad4 > 0){
            System.out.println("% Total single track efficiency - Quad 4: " + foundSingleTracksQuad4 + " / " + findableSingleTracksQuad4 + " = " + (foundSingleTracksQuad4/findableSingleTracksQuad4)*100 + "%");
        }
        if(nOppositeVolume > 0){
            System.out.println("% Total events passing opposite volume requirement: " + nOppositeVolume + " / " + eventNumber + " = " + (nOppositeVolume/eventNumber)*100 + "%");
        }
        if(nAboveThreshold > 0){
            System.out.println("% Total events with both clusters above energy threshold: " + nAboveThreshold + " / " + eventNumber + " = " + (nAboveThreshold/eventNumber)*100 + "%");
        }
        if(nTrigClusterTrackMatch > 0){
            System.out.println("% Total events with a trigger cluster-track match: " + nTrigClusterTrackMatch + " / " + eventNumber + " = " + (nTrigClusterTrackMatch/eventNumber)*100 + "%");
        }
        if(findableTracks > 0){
            System.out.println("% Total Track Reconstruction Efficiency: " + totalTracks + " / " + findableTracks + " = " + (totalTracks / findableTracks) * 100 + "%");
        }
        if(findableTopTracks > 0){
            System.out.println("% Total Top Track Reconstruction Efficiency: " + totalTopTracks + " / " + findableTopTracks + " = " + (totalTopTracks / findableTopTracks)* 100 + "%");
        }
        if(findableBottomTracks > 0){
            System.out.println("% Total Bottom Track Reconstruction Efficiency: " + totalBottomTracks + " / " + findableBottomTracks + " = " + (totalBottomTracks / findableBottomTracks) * 100 + "%");
        }
        System.out.println("\n%===================================================================% \n");
    }
    
}
