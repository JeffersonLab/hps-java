package org.hps.recon.filtering;

import java.util.ArrayList;
import java.util.List;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;

/**
 * This is a Driver for selecting/rejecting recon events using simple criteria on the output collections.
 * 
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class SimpleEventFilterDriver extends Driver {
        
    int minTracks = 0;
    int minReconParticles = 0;
    int minClusters = 0;
    
    int maxTracks = 999;
    int maxReconParticles = 999;
    int maxClusters = 999;
    
    int nRejected = 0;
    int nAccepted = 0;
    
     String trackCollectionName = "MatchedTracks";
     String reconParticleCollectionName = "FinalStateParticles";
     String clusterCollectionName = "EcalClusters";
    
    List<EventFilter> filters = new ArrayList<EventFilter>();
    
    public void startOfData() {
        filters.add(new ClusterFilter());
        filters.add(new TrackFilter());        
        filters.add(new ReconstructedParticleFilter());
    }
    
    public void process(EventHeader event) {
        for (EventFilter filter : filters) {
            boolean accept = filter.accept(event);
            if (!accept) {  
                System.out.println(filter.name() + " rejected event #" + event.getEventNumber());
                skipEvent();
            }
        }               
        ++nAccepted;
    }
    
    public void endOfData() {
        System.out.println(this.getClass().getName() + " - run summary");
        System.out.println("  nRejected: " + nRejected);
        System.out.println("  nAccepted: " + nAccepted);
    } 
    
    public void skipEvent() {
        ++nRejected;
        throw new Driver.NextEventException();
    }
        
    public void setMinTracks(int minTracks) {
        this.minTracks = minTracks;
    }
    public void setMaxTracks(int maxTracks) {
        this.maxTracks = maxTracks;
    }
    
    public void setMinClusters(int minClusters) {
        this.minClusters = minClusters;
    }
    public void setMaxClusters(int maxClusters) {
        this.maxClusters = maxClusters;
    }
    
    public void setMinReconParticles(int minReconParticles) {
        this.minReconParticles = minReconParticles;
    }
    public void setMaxReconParticles(int maxReconParticles) {
        this.maxReconParticles = maxReconParticles;
    }
    
    public void setTrackCollectionName(String s)
    {
        trackCollectionName = s;
    }
    public void setReconParticleCollectionName(String s)
    {
        reconParticleCollectionName = s;
    }
    
    public void setClusterCollectionName(String s)
    {
        clusterCollectionName = s;
    }
    
    
    static interface EventFilter {
        
        String name();
        
        boolean accept(EventHeader event);        
    }
        
    class TrackFilter implements EventFilter {
        
        public String name() {
            return "TrackFilter";
        }
        
        public boolean accept(EventHeader event) {
            if (event.hasCollection(Track.class, trackCollectionName)) {
                int n = event.get(Track.class, trackCollectionName).size() ;
                if ( n >= minTracks && n <= maxTracks) {
                    return true;
                }
            }
            return false;
        }        
    }
    
    class ClusterFilter implements EventFilter {
        
        public String name() {
            return "ClusterFilter";
        }
        
        public boolean accept(EventHeader event) {
            if (event.hasCollection(Cluster.class, clusterCollectionName)) {
                int n = event.get(Cluster.class, clusterCollectionName).size();
                if (n >= minClusters && n <= maxClusters) {
                    return true;
                }
            }
            return false;
        }        
    }
    
    class ReconstructedParticleFilter implements EventFilter {
        
        public String name() {
            return "ReconstructedParticleFilter";
        }
        
        public boolean accept(EventHeader event) {
            if (event.hasCollection(ReconstructedParticle.class, reconParticleCollectionName)) {
                int n = event.get(ReconstructedParticle.class, reconParticleCollectionName).size();
                if ( n >= minReconParticles && n <= maxReconParticles) {
                    return true;
                }
            }
            return false;
        }
    }    
}
