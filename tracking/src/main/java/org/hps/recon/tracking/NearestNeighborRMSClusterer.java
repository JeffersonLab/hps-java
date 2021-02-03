package org.hps.recon.tracking;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.special.Gamma;

import org.hps.readout.svt.HPSSVTConstants;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;

public class NearestNeighborRMSClusterer implements ClusteringAlgorithm {

    private static String _NAME = "NearestNeighborRMS";
    private double _seed_threshold;
    private double _neighbor_threshold;
    private double _cluster_threshold;
    private double _meanTime = 24;
    private double _timeWindow = 48;
    private double _neighborDeltaT = Double.POSITIVE_INFINITY;
    private final double _minChiProb = Gamma.regularizedGammaQ(4, 20);

    /**
     * Instantiate NearestNeighborRMS with specified thresholds. Seed threshold
     * is the minimum charge to initiate a cluster. Neighbor threshold is the
     * minimum charge to add a neighboring cell to a cluster. Cluster threshold
     * is minimum charge of the entire cluster. All thresholds are in units of
     * RMS noise of the channel(s).
     *
     * @param seed_threshold seed threshold
     * @param neighbor_threshold neighbor threshold
     * @param cluster_threshold cluster threshold
     */
    public NearestNeighborRMSClusterer(double seed_threshold, double neighbor_threshold, double cluster_threshold) {
        _seed_threshold = seed_threshold;
        _neighbor_threshold = neighbor_threshold;
        _cluster_threshold = cluster_threshold;
    }

    public void setMeanTime(double _meanTime) {
        this._meanTime = _meanTime;
    }

    public void setTimeWindow(double _timeWindow) {
        this._timeWindow = _timeWindow;
    }

    public void setNeighborDeltaT(double _neighborDeltaT) {
        this._neighborDeltaT = _neighborDeltaT;
    }

    /**
     * Instantiate NearestNeighborRMS with default thresholds:
     *
     * seed_threshold = 4*RMS noise neighbor_threshold = 3*RMS noise
     * cluster_threshold = 4*RMS noise
     */
    public NearestNeighborRMSClusterer() {
        this(4.0, 3.0, 4.0);
    }

    /**
     * Set the seed threshold. Units are RMS noise.
     *
     * @param seed_threshold seed threshold
     */
    public void setSeedThreshold(double seed_threshold) {
        _seed_threshold = seed_threshold;
    }

    /**
     * Set the neighbor threshold. Units are RMS noise.
     *
     * @param neighbor_threshold neighbor threshold
     */
    public void setNeighborThreshold(double neighbor_threshold) {
        _neighbor_threshold = neighbor_threshold;
    }

    /**
     * Set the cluster threshold. Units are RMS noise.
     *
     * @param cluster_threshold cluster threshold
     */
    public void setClusterThreshold(double cluster_threshold) {
        _cluster_threshold = cluster_threshold;
    }

    /**
     * Use a nearest neighbor algorithm to create clusters. 
     *
     * @param fittedHits Collection of fitted raw tracker hits to cluster.
     * @return Collection of fitted hits
     */
    @Override
    public List<List<LCRelation>> findClusters(List<LCRelation> fittedHits) {

        // Check that the seed threshold is at least as large as the neighbor 
        // threshold
        if (_seed_threshold < _neighbor_threshold) {
            throw new RuntimeException("Tracker hit clustering error: seed threshold below neighbor threshold");
        }

        // Create maps that show the channel status and relate the channel 
        // number to the raw hit and vice versa
        int mapsize = 2 * fittedHits.size();
        Set<Integer> clusterableSet = new HashSet<Integer>(mapsize);

        Map<Integer, LCRelation> channel_to_hit = new HashMap<Integer, LCRelation>(mapsize);

        // Create list of channel numbers to be used as cluster seeds
        List<Integer> cluster_seeds = new ArrayList<Integer>();

        // Loop over the raw hits and construct the maps used to relate cells and hits, initialize
        // the
        // clustering status map, and create a list of possible cluster seeds
        for (LCRelation fittedHit : fittedHits) {

            RawTrackerHit rawHit = FittedRawTrackerHit.getRawTrackerHit(fittedHit); 
            
            // get the channel number for this hit
            SiTrackerIdentifierHelper sid_helper = (SiTrackerIdentifierHelper) rawHit.getIdentifierHelper();
            IIdentifier id = rawHit.getIdentifier();
            int channel_number = sid_helper.getElectrodeValue(id);

            // Check for duplicate RawTrackerHits or channel numbers
            if (channel_to_hit.containsKey(channel_number)) {
                //TODO: be smarter about this
                if (Math.abs(FittedRawTrackerHit.getT0(channel_to_hit.get(channel_number))) 
                        < Math.abs(FittedRawTrackerHit.getT0(fittedHit))) {
                    continue;
                }
            }

            // Add this hit to the maps that relate channels and hits
            channel_to_hit.put(channel_number, fittedHit);

            // Get the signal from the readout chip
            double signal = FittedRawTrackerHit.getAmp(fittedHit); 
            double noiseRMS = 0; 
            for(int sampleN = 0; sampleN < HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES; sampleN++){
                noiseRMS += ((HpsSiSensor) rawHit.getDetectorElement()).getNoise(channel_number, sampleN);
            }
            noiseRMS = noiseRMS/HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;
            
            // Mark this hit as available for clustering if it is above the neighbor threshold
            if (signal / noiseRMS >= _neighbor_threshold && passChisqCut(fittedHit)) {
                clusterableSet.add(channel_number);
            }

            // Add this hit to the list of seeds if it is above the seed threshold
            if (signal / noiseRMS >= _seed_threshold && passTimingCut(fittedHit) && passChisqCut(fittedHit)) {
                cluster_seeds.add(channel_number);
            }
        }

        // Create a list of clusters
        // TODO: Create a cluster class instead 
        List<List<LCRelation>> cluster_list = new ArrayList<List<LCRelation>>();

        // Now loop over the cluster seeds to form clusters
        for (int seed_channel : cluster_seeds) {

            if (!clusterableSet.contains(seed_channel)) {
                continue;
            }

            // Create a new cluster
            List<LCRelation> cluster = new ArrayList<LCRelation>();
            double cluster_signal = 0.;
            double cluster_noise_squared = 0.;
            double cluster_weighted_time = 0.;

            // Create a queue to hold channels whose neighbors need to be checked for inclusion
            LinkedList<Integer> unchecked = new LinkedList<Integer>();

            // Add the seed channel to the unchecked list and mark it as unavailable for clustering
            unchecked.addLast(seed_channel);
            clusterableSet.remove(seed_channel);

            // Check the neighbors of channels added to the cluster
            while (unchecked.size() > 0) {

                // Pull the next channel off the queue and add it's hit to the cluster
                int clustered_cell = unchecked.removeFirst();
                cluster.add(channel_to_hit.get(clustered_cell));
                LCRelation hit = channel_to_hit.get(clustered_cell);
                cluster_signal += FittedRawTrackerHit.getAmp(hit);
                double strip_noise = 0; 
                for(int sampleN = 0; sampleN < HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES; sampleN++){
                    strip_noise += ((HpsSiSensor) FittedRawTrackerHit.getRawTrackerHit(hit).getDetectorElement()).getNoise(clustered_cell, sampleN);
                }
                strip_noise = strip_noise/HPSSVTConstants.TOTAL_NUMBER_OF_SAMPLES;
                cluster_noise_squared += Math.pow(strip_noise, 2); 
                cluster_weighted_time += FittedRawTrackerHit.getT0(hit) * FittedRawTrackerHit.getAmp(hit);
                Collection<Integer> neighbor_channels = getNearestNeighborCells(clustered_cell);

                // Now loop over the neighbors and see if we can add them to the cluster
                for (int channel : neighbor_channels) {

                    // Check if this neighbor channel is still available for clustering
                    if (!clusterableSet.contains(channel)) {
                        continue;
                    }

                    LCRelation neighbor_hit = channel_to_hit.get(channel);
                    if (Math.abs(FittedRawTrackerHit.getT0(neighbor_hit) - cluster_weighted_time / cluster_signal) > _neighborDeltaT) {
                        continue;
                    }

                    // Add channel to the list of unchecked clustered channels
                    // and mark it unavailable for clustering
                    unchecked.addLast(channel);
                    clusterableSet.remove(channel);

                } // end of loop over neighbor cells
            } // end of loop over unchecked cells

            // Finished with this cluster, check cluster threshold and add it to the list of
            // clusters
            if (cluster.size() > 0 && cluster_signal / Math.sqrt(cluster_noise_squared) > _cluster_threshold) {
                cluster_list.add(cluster);
            }

        } // End of loop over seeds

        // Finished finding clusters
        return cluster_list;
    }

    private boolean passTimingCut(LCRelation fittedHit) {
        double time = FittedRawTrackerHit.getT0(fittedHit);
        return (Math.abs(time - _meanTime) < _timeWindow);
    }

    private boolean passChisqCut(LCRelation fittedHit) {
        return FittedRawTrackerHit.getChi2Prob(fittedHit) > _minChiProb; 
    }

    public Collection<Integer> getNearestNeighborCells(int cell) {
        Collection<Integer> neighbors = new ArrayList<Integer>(2);
        for (int ineigh = -1; ineigh <= 1; ineigh = ineigh + 2) {
            int neighbor_cell = cell + ineigh;
            if (isValidCell(neighbor_cell)) {
                neighbors.add(neighbor_cell);
            }
        }
        return neighbors;
    }

    public boolean isValidCell(int cell) {
        return (cell >= 0 && cell < HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR);
    }
}
