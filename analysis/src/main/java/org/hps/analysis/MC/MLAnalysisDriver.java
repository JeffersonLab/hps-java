package org.hps.analysis.MC;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.hps.record.triggerbank.TriggerModule;
import org.hps.util.Pair;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.SimCalorimeterHit;
import org.lcsim.event.Track;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.FieldMap;
import org.lcsim.geometry.compact.Subdetector;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

public class MLAnalysisDriver extends Driver {
    private Subdetector tracker = null;
    private FieldMap magneticFieldMap = null;
    private List<HpsSiSensor> sensors = null;
    private String TRACKER_SUBDETECTOR_NAME = "Tracker";
    
    private FileWriter writer = null;
    
    private final AIDA aida = AIDA.defaultInstance();
    private IHistogram1D deltaR = aida.histogram1D("Dr", 40, 0, 400);
    private IHistogram2D deltaXY = aida.histogram2D("Dy vs. Dx", 40, 0, 400, 40, 0, 40);
    
    private int multiClusterTracks = 0;
    
    @Override
    public void detectorChanged(Detector detector) {
        magneticFieldMap = detector.getFieldMap();
        tracker = detector.getSubdetector(TRACKER_SUBDETECTOR_NAME);
        sensors = detector.getSubdetector(TRACKER_SUBDETECTOR_NAME).getDetectorElement().findDescendants(HpsSiSensor.class);
    }
    
    @Override
    public void startOfData() {
        try {
            writer = new FileWriter("C:\\cygwin64\\home\\Kyle\\data.arff");
            
            writer.write("");
            writer.append("@relation cluster_track_matching\n");
            writer.append("@attribute cluster_rx numeric\n");
            writer.append("@attribute cluster_ry numeric\n");
            writer.append("@attribute cluster_ix numeric\n");
            writer.append("@attribute cluster_iy numeric\n");
            writer.append("@attribute cluster_energy numeric\n");
            writer.append("@attribute cluster_hit_count numeric\n");
            
            writer.append("@attribute track_rx numeric\n");
            writer.append("@attribute track_ry numeric\n");
            writer.append("@attribute track_px numeric\n");
            writer.append("@attribute track_py numeric\n");
            writer.append("@attribute track_momentum numeric\n");
            writer.append("@attribute track_chi2 numeric\n");
            writer.append("@attribute track_charge { positive, negative }\n");
            writer.append("@attribute track_top_bottom { top, bottom }\n");
            
            writer.append("@attribute dx numeric\n");
            writer.append("@attribute dy numeric\n");
            writer.append("@attribute dr numeric\n");
            writer.append("@attribute dE numeric\n");
            writer.append("@attribute matched { true, false }\n");
            
            writer.append("@data\n");
        } catch (IOException e) { throw new RuntimeException(); }
    }
    
    @Override
    public void endOfData() {
        try { writer.close(); }
        catch (IOException e) { throw new RuntimeException(); }
        
        System.out.println("Multi-Cluster Tracks: " + multiClusterTracks);
    }
    
    @Override
    public void process(EventHeader event) {
        
        /*
         * Get the event collection data needed in order to perform
         * the truth analysis. This consists of both the GBL track
         * and calorimeter cluster collections.
         */
        
        // Obtain the calorimeter clusters, and sort them based on
        // the cluster energy.
        List<Cluster> clusters = TruthModule.getCollection(event, "EcalClustersCorr", Cluster.class);
        clusters.sort(new Comparator<Cluster>() {
            @Override
            public int compare(Cluster arg0, Cluster arg1) {
                return Double.compare(arg0.getEnergy(), arg1.getEnergy());
            }
        });
        
        // Obtain the GBL tracks, and sort them based on the track
        // momentum magnitude.
        List<Track> tracks = TruthModule.getCollection(event, "GBLTracks", Track.class);
        tracks.sort(new Comparator<Track>() {
            @Override
            public int compare(Track arg0, Track arg1) {
                return Double.compare(TruthModule.magnitude(arg0.getTrackStates().get(0).getMomentum()), TruthModule.magnitude(arg1.getTrackStates().get(0).getMomentum()));
            }
        });
        
        
        
        /*
         * We only need to worry about "analyzable events." These are
         * events that contain the minimum data necessary to perform
         * trident reconstruction, and is presently defined as having
         * two GBL tracks, one positive and one negative, where one
         * track points upwards and one downwards.
         */
        
        // Consider all possible track permutations and store those
        // that pass the "analyzable" conditions.
        List<Pair<Track, Track>> analyzableTrackPairs = new ArrayList<Pair<Track, Track>>();
        for(int i = 0; i < tracks.size(); i++) {
            Track iTrack = tracks.get(i);
            for(int j = i + 1; j < tracks.size(); j++) {
                // Get the track.
                Track jTrack = tracks.get(j);
                
                // Check whether this is a positron/electron pair.
                double iCharge = TruthModule.getCharge(iTrack);
                double jCharge = TruthModule.getCharge(jTrack);
                if((iCharge > 0 && jCharge < 0) || (iCharge < 0 && jCharge > 0)) {
                    // Check whether one track is a top track and the
                    // other is a bottom track.
                    if((TruthModule.isTopTrack(iTrack) && TruthModule.isBottomTrack(jTrack)) || (TruthModule.isBottomTrack(iTrack) && TruthModule.isTopTrack(jTrack))) {
                        analyzableTrackPairs.add(new Pair<Track, Track>(iTrack, jTrack));
                    }
                }
            }
        }
        
        // If at least one analyzable track exists, then the event is
        // "good" and analysis may continue. Otherwise, skip the event.
        if(analyzableTrackPairs.isEmpty()) { return; }
        
        
        
        /*
         * Print out the initial event data for clusters, tracks, and
         * analyzable track pairs. This is only performed if an event
         * is actually analyzable.
         */
        
        System.out.println("\n\n\nEvent " + event.getEventNumber());
        
        // Print out the cluster and track information to the terminal.
        System.out.println("Energy-Corrected Reconstructed Clusters:");
        for(Cluster cluster : clusters) {
            System.out.println("\t" + TruthModule.getClusterString(cluster));
            for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                if(!(hit instanceof SimCalorimeterHit)) {
                    throw new RuntimeException("Error: Expected cluster truth information, but found none.");
                }
                System.out.println("\t\t" + TruthModule.getEcalHitString(hit));
            }
        }
        
        System.out.println("\nGBL Tracks:");
        for(Track track : tracks) {
            System.out.println("\t" + TruthModule.getTrackString(track));
        }
        
        // Otherwise, print out the analyzable track pairs.
        System.out.println("\nAnalyzable Track Pairs:");
        for(Pair<Track, Track> pair : analyzableTrackPairs) {
            System.out.println("\tAnalyzable Pair:");
            System.out.println("\t\t" + TruthModule.getTrackString(pair.getFirstElement()));
            System.out.println("\t\t" + TruthModule.getTrackString(pair.getSecondElement()));
        }
        
        
        
        /*
         * Get the truth information for each of the analysis object
         * types. GBL tracks are assigned a truth particle by an
         * external algorithm. Clusters are processed and the exact
         * energy contribution (both total and percent) of each truth
         * particle is determined. This is then used to match the
         * track truth particle to best cluster.
         */
        
        // Get the truth data for each track.
        Map<Track, MCParticle> trackTruthMap = new HashMap<Track, MCParticle>(tracks.size());
        Map<MCParticle, Track> reverseTrackTruthMap = new HashMap<MCParticle, Track>(tracks.size());
        for(Track track : tracks) {
            MCFullDetectorTruth trackTruth = new MCFullDetectorTruth(event, track, magneticFieldMap, sensors, tracker);
            trackTruthMap.put(track, trackTruth.getMCParticle());
            reverseTrackTruthMap.put(trackTruth.getMCParticle(), track);
        }
        
        // Get the mappings of cluster to truth energy contribution
        // for each relevant truth particle.
        Map<Cluster, Map<MCParticle, Double>> actualClusterEnergyContributionMap = new HashMap<Cluster, Map<MCParticle, Double>>(clusters.size());
        Map<Cluster, Map<MCParticle, Double>> percentClusterEnergyContributionMap = new HashMap<Cluster, Map<MCParticle, Double>>(clusters.size());
        for(Cluster cluster : clusters) {
            Map<MCParticle, Double> actualMap = TruthModule.getIncidentClusterParticleEnergyContribution(cluster);
            Map<MCParticle, Double> percentMap = TruthModule.getIncidentClusterParticlePercentEnergyContribution(cluster);
            
            actualClusterEnergyContributionMap.put(cluster, actualMap);
            percentClusterEnergyContributionMap.put(cluster, percentMap);
        }
        
        // Print out the cluster energy data.
        System.out.println("\nCluster Truth Data:");
        for(Cluster cluster : clusters) {
            System.out.println("\t" + TruthModule.getClusterString(cluster));
            
            Map<MCParticle, Double> actualMap = actualClusterEnergyContributionMap.get(cluster);
            Map<MCParticle, Double> percentMap = percentClusterEnergyContributionMap.get(cluster);
            List<Pair<MCParticle, Double>> actualMapList = TruthModule.asOrderedList(actualMap);
            
            for(Pair<MCParticle, Double> entry : actualMapList) {
                System.out.printf("\t\t%5.3f (%5.1f%%) :: %s%n", entry.getSecondElement().doubleValue(),
                        percentMap.get(entry.getFirstElement()).doubleValue() * 100.0, TruthModule.getParticleString(entry.getFirstElement()));
            }
        }
        
        // Print out the track truth data.
        System.out.println("\nTrack Truth Data:");
        for(Entry<Track, MCParticle> entry : trackTruthMap.entrySet()) {
            System.out.println("\t" + TruthModule.getTrackString(entry.getKey()) + "\n\t\t" + TruthModule.getParticleString(entry.getValue()));
        }
        
        
        
        /*
         * Each track should be attached to the cluster that it most
         * closely connects to based on its truth particle. This is
         * done by finding all cluster to which a track's truth
         * particle contributes, and selecting the cluster to which
         * it contributes the most energy. This cluster must receive
         * at least 1/3 of its energy from that truth particle for it
         * to be considered matched.
         */
        
        Map<Track, Set<Cluster>> trackClusterMap = new HashMap<Track, Set<Cluster>>();
        for(Track track : tracks) {
            MCParticle truthParticle = trackTruthMap.get(track);
            for(Cluster cluster : clusters) {
                Map<MCParticle, Double> percentMap = percentClusterEnergyContributionMap.get(cluster);
                if(percentMap.containsKey(truthParticle) && percentMap.get(truthParticle).doubleValue() >= 0.200) {
                    addToMap(track, cluster, trackClusterMap);
                }
            }
        }
        
        // Print out the track/cluster matches.
        System.out.println("\nTrack-Cluster Matched Pairs:");
        for(Entry<Track, Set<Cluster>> entry : trackClusterMap.entrySet()) {
            System.out.println("\t" + TruthModule.getTrackString(entry.getKey()));
            
            double[] trackR = TruthModule.getTrackPositionAtCalorimeterFace(entry.getKey());
            for(Cluster cluster : entry.getValue()) {
                double deltaR = delta(trackR, cluster.getPosition());
                System.out.printf("\t\t%6.3f mm :: %s%n", deltaR, TruthModule.getClusterString(cluster));
            }
        }
        if(trackClusterMap.isEmpty()) { System.out.println("\tNone"); }
        
        for(Track track : tracks) {
            double[] trackR = TruthModule.getTrackPositionAtCalorimeterFace(track);
            double[] trackP = track.getTrackStates().get(0).getMomentum();
            double trackPMag = TruthModule.magnitude(trackP);
            double trackCharge = TruthModule.getCharge(track);
            boolean isTop = TruthModule.isTopTrack(track);
            double chi2 = track.getChi2();
            
            MCParticle trackParticle = trackTruthMap.get(track);
            
            int matches = 0;
            
            // PRUNING :: Cut data that is obviously bad.
            if(chi2 > 50) { continue; }
            
            for(Cluster cluster : clusters) {
                double[] clusterR = cluster.getPosition();
                double clusterEnergy = TriggerModule.getValueClusterTotalEnergy(cluster);
                double clusterHitCount = TriggerModule.getClusterHitCount(cluster);
                
                int ix = TriggerModule.getClusterXIndex(cluster);
                int iy = TriggerModule.getClusterYIndex(cluster);
                double dx = Math.abs(clusterR[0] - trackR[0]);
                double dy = Math.abs(clusterR[1] - trackR[1]);
                double dr = delta(clusterR, trackR);
                double dE = Math.abs(clusterEnergy - trackPMag);
                
                // PRUNING :: Cut data that is obviously bad.
                if(dr > 50) { continue; }
                
                boolean isMatch = false;
                Map<MCParticle, Double> percentMap = percentClusterEnergyContributionMap.get(cluster);
                if(percentMap.containsKey(trackParticle) && percentMap.get(trackParticle).doubleValue() >= 0.200) {
                    isMatch = true;
                    deltaR.fill(dr);
                    deltaXY.fill(dx, dy);
                    matches++;
                }
                
                try {
                    //writer.append(String.format("%f,%f,%s,%s,%f,%f,%f,%f,%b%n",
                    //        clusterR[0], chi2, trackCharge > 0 ? "positive" : "negative", isTop ? "top" : "bottom", dx, dy, dr, dE, isMatch));
                    writer.append(String.format("%f,%f,%d,%d,%f,%.0f,%f,%f,%f,%f,%f,%f,%s,%s,%f,%f,%f,%f,%b%n",
                            clusterR[0], clusterR[1], ix, iy, clusterEnergy, clusterHitCount, trackR[0], trackR[1], trackP[0], trackP[1],
                            trackPMag, chi2, trackCharge > 0 ? "positive" : "negative", isTop ? "top" : "bottom", dx, dy, dr, dE, isMatch));
                } catch (IOException e) { throw new RuntimeException(); }
            }
            
            if(matches > 1) { multiClusterTracks++; }
        }
    }
    
    private static double delta(double[] v0, double[] v1) {
        if(v0.length != v1.length) {
            throw new IllegalArgumentException("Error: Vectors must be of the same length.");
        }
        
        double sum = 0.0;
        for(int i = 0; i < v0.length; i++) {
            sum += Math.pow(v0[i] - v1[i], 2);
        }
        return Math.sqrt(sum);
    }
    
    private static final <T, V> void addToMap(T key, V value, Map<T, Set<V>> map) {
        if(map.containsKey(key)) {
            Set<V> newValue = map.get(key);
            newValue.add(value);
        } else {
            Set<V> newValue = new HashSet<V>();
            newValue.add(value);
            map.put(key, newValue);
        }
    }
}