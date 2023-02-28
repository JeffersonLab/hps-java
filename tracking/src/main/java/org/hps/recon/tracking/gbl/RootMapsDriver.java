package org.hps.recon.tracking.gbl;

import java.util.ArrayList;
import java.util.List;
//import java.util.Collection;
import java.util.HashMap;

import org.lcsim.event.EventHeader;
//import org.lcsim.event.LCRelation;
//import org.lcsim.event.base.BaseRelationalTable;
//import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
//import org.lcsim.event.base.BaseTrack;

import org.lcsim.util.Driver;
import org.lcsim.geometry.Detector;

import org.lcsim.event.Cluster;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.TrackState;
//Fiducial cuts on the calorimeter cluster
import org.hps.record.triggerbank.TriggerModule;
import hep.physics.vec.BasicHep3Vector;


public class RootMapsDriver extends Driver {

    // This can be used to pass tracks or particles
    private String inputTracks    = "GBLTracks";
    private String inputParticles = "FinalStateParticles";
    private boolean useParticles = true;
    
    private TH3DJna  eop_pos_phi_tanL;
    private TH3DJna  eop_ele_phi_tanL;
    private TFileJna mapfile;


    public void setInputTracks(String val) {
        inputTracks = val;
    }
    
    public void setInputParticles(String val) {
        inputParticles = val;
    }
    
    public void setUseParticles(boolean val) {
        useParticles = val;
    }

    @Override
    protected void startOfData() {
        
        if (useParticles) {

            mapfile = new TFileJna("output_map_file.root","RECREATE");
            
            eop_pos_phi_tanL = new TH3DJna("eop_pos_phi_tanL", "eop_pos_phi_tanL",
                                           100,-0.2,0.2,
                                           100,-0.08,0.08,
                                           100,0.5,1.5);
            
            eop_ele_phi_tanL = new TH3DJna("eop_ele_phi_tanL", "eop_ele_phi_tanL",
                                           100,-0.2,0.2,
                                           100,-0.08,0.08,
                                           100,0.5,1.5);
            
        }
    }

    @Override
    protected void endOfData() {

        if (useParticles) {
            eop_pos_phi_tanL.write(mapfile);
            eop_ele_phi_tanL.write(mapfile);
            mapfile.close();
            mapfile.delete();
        }
    }

    @Override
    protected void detectorChanged(Detector detector) {
        
    }
    

    @Override
    protected void process(EventHeader event) {
    
        //Check if track collection is present
        if (!event.hasCollection(Track.class, inputTracks)) {
            return;
        }

        //Check if particle collection is present
        if (useParticles && !event.hasCollection(ReconstructedParticle.class, inputParticles)) {
            return;
        }

        //Define this in a common driver - TODO
        //setupSensors(event);


        List<Track> tracks = new ArrayList<Track>();
        List<ReconstructedParticle> particles = null;

        // Create a mapping of matched Tracks to corresponding Clusters.
        HashMap<Track,Cluster> TrackClusterPairs = null;
    
        if (!useParticles) 
            tracks = event.get(Track.class, inputTracks);
        else {
            particles = event.get(ReconstructedParticle.class, inputParticles);
            for (ReconstructedParticle particle : particles) {
                if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                    continue;
                tracks.add(particle.getTracks().get(0));
            }
        
            TrackClusterPairs = GetClustersFromParticles(particles);
        }


        //Loop over the tracks
    
        for (Track track : tracks) {

        
            if (useParticles) {

                Cluster em_cluster = TrackClusterPairs.get(track);
            
                //Only select clusters in fiducial region
                if (!TriggerModule.inFiducialRegion(em_cluster))
                    continue;
            
            
                //Should use the trackState at the last instead the one at perigee - TODO
                TrackState trackState = track.getTrackStates().get(0);
                double trackp = new BasicHep3Vector(trackState.getMomentum()).magnitude();
                double e_o_p = em_cluster.getEnergy() / trackp;
            
                //Get the track parameters
                double[] trk_prms = track.getTrackParameters();
                                
                //double phi0 = trk_prms[BaseTrack.PHI];
                //double tanL = trk_prms[BaseTrack.TANLAMBDA];
                double tanL = trackState.getTanLambda();
                double phi0 = trackState.getPhi();

                
                //Positrons - charge is flipped
                if (track.getCharge() < 0) {
                    eop_pos_phi_tanL.fill(phi0,tanL,e_o_p);
                }
                else {
                    eop_ele_phi_tanL.fill(phi0,tanL,e_o_p);
                }
            }//useParticles
        }//tracks loop
    } // process

    private HashMap<Track,Cluster> GetClustersFromParticles(List<ReconstructedParticle> particles) {
        
        HashMap<Track,Cluster> tracksAndclusters = new HashMap<Track,Cluster>();
        
        for (ReconstructedParticle particle : particles) {
            if (particle.getTracks().isEmpty() || particle.getClusters().isEmpty())
                continue;
            Track track = particle.getTracks().get(0);
            Cluster cluster = particle.getClusters().get(0);
            tracksAndclusters.put(track,cluster);
        }
        
        return tracksAndclusters;
    }
    
}
