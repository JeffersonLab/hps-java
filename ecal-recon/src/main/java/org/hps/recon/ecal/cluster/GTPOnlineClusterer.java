package org.hps.recon.ecal.cluster;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.util.aida.AIDA;

/**
 * Class <code>GTPOnlineClusterer</code> is an implementation of the
 * abstract class <code>AbstractClusterer</code> that is responsible
 * for producing clusters using the GTP algorithm employed by the
 * hardware.<br/>
 * <br/>
 * The GTP algorithm produces clusters by finding hits representing
 * local spatiotemporal energy maxima and forming a cluster from the
 * hits within the aforementioned spatiotemporal window. A given hit
 * is first checked to see if it exceeds some minimum energy threshold
 * (referred to as the "seed energy threshold"). If this is the case,
 * the algorithm looks at all hits that occurred in the same crystal as
 * the comparison hit, or any crystal directly adjacent to it, within
 * a programmable time window. If the hit exceeds all hits meeting these
 * criteria in energy, the hit is considered the “seed hit” of a cluster.
 * Then, all hits within the 3x3 spatial window which occur in the time
 * window are added to a <code>Cluster</code> object.<br/>
 * <br/>
 * Note that the algorithm employs two distinct temporal windows. The
 * first is the “verification” window. This is used to check that the
 * potential seed hit is a local maximum in energy, and is required to
 * be symmetric (i.e. as long before the seed time as after it) to ensure
 * consistency. The second temporal window is the “inclusion” window,
 * which determines which hits are included in the cluster. The inclusion
 * window can be asymmetric, but can not exceed the verification window
 * in length. As an example, one could choose a 12 ns verification window,
 * meaning that the algorithm would 12 ns before and after the seed hit
 * to check that it has the highest energy, but use a 4 ns/12 ns inclusion
 * window, meaning that the algorithm would only include hits in 3x3
 * spatial window up to 4 ns before and up to 12 ns after the seed hit
 * in the cluster. Due to the way the hardware processes hits, the higher
 * energy parts of a cluster always occur first in time, so it is not
 * necessarily desirable to include hits significantly before the seed.
 * It is however, necessary to verify a hit’s status as a maximum across
 * the full time window to ensure consistency in cluster formation.
 * <code>GTPOnlineClusterer</code> automatically selects the larger of
 * the two inclusion window parts as the verification window length.<br/>
 * <br/>
 * <code>GTPOnlineClusterer</code> requires as input a collection of
 * <code>CalorimeterHit</code> objects representing the event hits. It
 * will then produce a collection of <code>Cluster</code> objects
 * representing the GTP algorithm output. It also produces a series of
 * distribution plots under the “GTP(O) Cluster Plots” header. It is
 * designed to be run on readout events, either from the hardware or
 * Monte Carlo that has been processed through the readout simulation.
 * If the input data is formatted into constant-time beam bunches, the
 * sister class <code>GTPClusterer</code> should be used instead.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 * @see Cluster
 * @see CalorimeterHit
 * @see AbstractClusterer
 * @see GTPClusterer
 */
public class GTPOnlineClusterer extends AbstractClusterer {
    /**
     * The length of the temporal window for inclusing clusters that
     * occur before the seed hit.
     */
    private double timeBefore = 4;
    
    /**
     * The length of the temporal window for including clusters that
     * occur after the seed hit.
     */
    private double timeAfter = 12;
    
    /**
     * The length of the temporal window for verifying that a hit is
     * a local maximum in energy. This length represents both halves
     * of the verification window, so the full length would be defined
     * by <code>timeWindow * 2 + 4</code> ns.
     */
    private double timeWindow = 12;
    
    /**
     * The minimum energy a hit must have to be considered for cluster
     * seed formation. Units are in GeV.
     */
    private double seedThreshold = 0.050;
    
    /**
     * Controls whether or not verbose diagnostic information is output.
     */
    private boolean verbose = false;
    
    // Diagnostic plots.
    private AIDA aida = AIDA.defaultInstance();
    private IHistogram1D hitEnergy = aida.histogram1D("GTP(O) Cluster Plots/Hit Energy Distribution", 256, -1.0, 2.2);
    private IHistogram1D clusterSeedEnergy = aida.histogram1D("GTP(O) Cluster Plots/Cluster Seed Energy Distribution", 176, 0.0, 2.2);
    private IHistogram1D clusterHitCount = aida.histogram1D("GTP(O) Cluster Plots/Cluster Hit Count Distribution", 9, 1, 10);
    private IHistogram1D clusterTotalEnergy = aida.histogram1D("GTP(O) Cluster Plots/Cluster Total Energy Distribution", 176, 0.0, 2.2);
    private IHistogram2D hitDistribution = aida.histogram2D("GTP(O) Cluster Plots/Hit Distribution", 46, -23, 23, 11, -5.5, 5.5);
    private IHistogram2D clusterDistribution = aida.histogram2D("GTP(O) Cluster Plots/Cluster Seed Distribution", 46, -23, 23, 11, -5.5, 5.5);
    
    /**
     * Instantiates a new instance of a readout GTP clustering algorithm.
     * This will use the default seed energy threshold of 50 MeV.
     */
    GTPOnlineClusterer() {
        super(new String[] { "seedThreshold" }, new double[] { 0.050 });
    }
    
    /**
     * Processes the argument <code>CalorimeterHit</code> collection and
     * forms a collection of <code>Cluster</code> objects according to
     * the GTP clustering algorithm.
     * @param event - The object containing event data.
     * @param hitList - A list of <code>CalorimeterHit</code> objects
     * from which clusters should be formed.
     */
    @Override
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitList) {
        // VERBOSE :: Print the driver header.
        if(verbose) {
            System.out.println();
            System.out.println();
            System.out.println("======================================================================");
            System.out.println("=== GTP Readout Clusterer ============================================");
            System.out.println("======================================================================");
            
            // Sort the hits by x-index and then by y-index.
            Collections.sort(hitList, new Comparator<CalorimeterHit>() {
                @Override
                public int compare(CalorimeterHit firstHit, CalorimeterHit secondHit) {
                    int[] ix = { getHitX(firstHit), getHitX(secondHit) };
                    if(ix[0] != ix[1]) { return Integer.compare(ix[0], ix[1]); }
                    else {
                        int iy[] = { getHitY(firstHit), getHitY(secondHit) };
                        return Integer.compare(iy[0], iy[1]);
                    }
                }
            });
            
            // Print the hit collection.
            System.out.println("Event Hit Collection:");
            for(CalorimeterHit hit : hitList) {
                System.out.printf("\t%s%n", getHitText(hit));
            }
            System.out.println();
        }
        
        // Track the valid clusters.
        List<Cluster> clusterList = new ArrayList<Cluster>();
        
        // Sort the hits by time in reverse order.
        Collections.sort(hitList, new Comparator<CalorimeterHit>() {
            @Override
            public int compare(CalorimeterHit firstHit, CalorimeterHit secondHit) {
                return Double.compare(getHitTime(secondHit), getHitTime(firstHit));
            }
        });
        
        // A seed hit is a hit that is the largest both within its
        // spatial range (+/- 1 in the ix and iy direction) and
        // within a certain temporal window. If a hit is a seed, all
        // hits within the 3x3 spatial range around it that are also
        // within the temporal window are considered part of the
        // cluster.
        
        // Iterate over each hit and see if it qualifies as a seed hit.
        seedLoop:
            for(CalorimeterHit seed : hitList) {
                // VERBOSE :: Output the seed that is being considered.
                if(verbose) {
                    System.out.println("\n");
                    System.out.println("Considering seed " + getHitText(seed));
                }
                
                // Put the hit energy into the hit energy distribution.
                hitEnergy.fill(getHitEnergy(seed));
                hitDistribution.fill(getHitX(seed), getHitY(seed));
                
                // Check whether the potential seed passes the seed
                // energy cut.
                if(verbose) { System.out.printf("Checking seed energy threshold %5.3f >= %5.3f... ", getHitEnergy(seed), seedThreshold); }
                if(getHitEnergy(seed) < seedThreshold) {
                    if(verbose) { System.out.println("[fail]"); }
                    continue seedLoop;
                }
                if(verbose) { System.out.println("[pass]"); }
                
                // Create a cluster for the potential seed.
                BaseCluster protoCluster = createBasicCluster();
                protoCluster.addHit(seed);
                protoCluster.setPosition(seed.getDetectorElement().getGeometry().getPosition().v());
                protoCluster.setNeedsPropertyCalculation(false);
                
                // Iterate over the other hits and if they are within
                // the clustering spatiotemporal window, compare their
                // energies.
                hitLoop:
                for(CalorimeterHit hit : hitList) {
                    // Negative energy hits are never valid. Skip them.
                    if(hit.getCorrectedEnergy() < 0) {
                        continue hitLoop;
                    }
                    
                    // Do not compare the potential seed hit to itself.
                    if(hit == seed) {
                        continue hitLoop;
                    }
                    
                    // Check if the hit is within the spatiotemporal
                    // clustering window.
                    if(withinTimeVerificationWindow(seed, hit) && withinSpatialWindow(seed, hit)) {
                        // Check if the hit invalidates the potential
                        // seed.
                        if(isValidSeed(seed, hit)) {
                            // Make sure that the hit is also within
                            // the hit add window; this may not be
                            // the same as the verification window
                            // if the asymmetric window is active.
                            if(withinTimeClusteringWindow(seed, hit)) {
                                protoCluster.addHit(hit);
                            }
                        }
                        
                        // If it is not, then skip the rest of the
                        // loop; the potential seed is not really
                        // a seed.
                        else { continue seedLoop; }
                    }
                }
                
                // If this point is reached, then the seed was not
                // invalidated by any of the other hits and is really
                // a cluster center. Add the cluster to the list.
                clusterList.add(protoCluster);
                
                // Populate the cluster distribution plots.
                clusterSeedEnergy.fill(protoCluster.getCalorimeterHits().get(0).getCorrectedEnergy());
                clusterTotalEnergy.fill(protoCluster.getEnergy());
                clusterHitCount.fill(protoCluster.getCalorimeterHits().size());
                clusterDistribution.fill(protoCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
                        protoCluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));
            }
        
        // VERBOSE :: Print out all the clusters in the event.
        if(verbose) {
            // Print the clusters.
            System.out.println("Event Cluster Collection:");
            for(Cluster cluster : clusterList) {
                // Output basic cluster positional and energy data.
                CalorimeterHit seedHit = cluster.getCalorimeterHits().get(0);
                int ix = seedHit.getIdentifierFieldValue("ix");
                int iy = seedHit.getIdentifierFieldValue("iy");
                double energy = cluster.getEnergy();
                double time = seedHit.getTime();
                System.out.printf("\tCluster --> %6.3f GeV at (%3d, %3d) and at t = %.2f%n", energy, ix, iy, time);
                
                // Output the cluster hit collection.
                for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    int hix = hit.getIdentifierFieldValue("ix");
                    int hiy = hit.getIdentifierFieldValue("iy");
                    double henergy = hit.getCorrectedEnergy();
                    double htime = hit.getTime();
                    System.out.printf("\t\tCompHit --> %.3f GeV at (%3d, %3d) and at t = %.2f%n", henergy, hix, hiy, htime);
                }
            }
            System.out.println();
        }
        
        // VERBOSE :: Print a new line.
        if(verbose) { System.out.println(); }
        
        // Return the list of clusters.
        return clusterList;
    }
    
    /**
     * Gets the type of cluster produced by this clusterer.
     * @return Returns the cluster type as a <code>ClusterType</code>
     * enumerable.
     */
    @Override
    public ClusterType getClusterType() {
        return ClusterType.GTP_ONLINE;
    }
    
    /**
     * Gets the seed energy lower bound threshold in units of GeV.
     * @return Returns the threshold as a <code>double</code>.
     */
    public double getSeedLowThreshold() { return seedThreshold; }
    
    /**
     * Gets the number of nanoseconds before the seed hit time that
     * the clusterer will look to include hits when a cluster is formed.
     * @return Returns the time window as a <code>double</code>.
     */
    public double getWindowBefore() { return timeBefore; }
    
    /**
     * Gets the number of nanoseconds after the seed hit time that
     * the clusterer will look to include hits when a cluster is formed.
     * @return Returns the time window as a <code>double</code>.
     */
    public double getWindowAfter() { return timeAfter; }
    
    /**
     * Sets up the clusterer parameters so that it is ready to be used.
     * This should be run before the cluster formation.
     */
    @Override
    public void initialize() {
        seedThreshold = getCuts().getValue("seedThreshold");
    }
    
    /**
     * Returns whether the clusterer will output verbose diagnostic
     * information.
     * @return Returns <code>true</code> if the clusterer will output
     * diagnostic information and <code>false</code> otherwise.
     */
    boolean isVerbose() { return verbose; }
    
    /**
     * Sets the minimum energy a hit must have before it will be
     * considered for cluster formation.
     * @param seedThreshold - The seed threshold in GeV.
     */
    void setSeedLowThreshold(double seedThreshold) {
        this.seedThreshold = seedThreshold;
    }
    
    /**
     * Sets whether the clusterer should output diagnostic text or not.
     * @param verbose - <code>true</code> indicates that the clusterer
     * should output diagnostic text and <code>false</code> that it
     * should not.
     */
    void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Sets the number of clock-cycles to include in the clustering
     * window before the seed hit. One clock-cycle is four nanoseconds.
     * Note that the larger of this time and the time defined in method
     * <code>setWindowAfter</code> will be the verification window size.
     * @param cyclesBefore - The length of the clustering window before
     * the seed hit in clock cycles.
     */
    void setWindowBefore(int cyclesBefore) {
        // The cluster window can not be negative.
        if(cyclesBefore < 0) { cyclesBefore = 0; }
        
        // Convert the window to nanoseconds and set the two time
        // windows appropriately.
        timeBefore = cyclesBefore * 4;
        timeWindow = Math.max(timeBefore, timeAfter);
    }
    
    /**
     * Sets the number of clock-cycles to include in the clustering
     * window after the seed hit. One clock-cycle is four nanoseconds.
     * Note that the larger of this time and the time defined in method
     * <code>setWindowBefore</code> will be the verification window size.
     * @param cyclesAfter - The length of the clustering window after
     * the seed hit in clock cycles.
     */
    void setWindowAfter(int cyclesAfter) {
        // The cluster window can not be negative.
        if(cyclesAfter < 0) { cyclesAfter = 0; }
        
        // Convert the window to nanoseconds and set the two time
        // windows appropriately.
        timeAfter = cyclesAfter * 4;
        timeWindow = Math.max(timeBefore, timeAfter);
    }
    
    private static final String getHitText(CalorimeterHit hit) {
        return String.format("Hit --> %6.3f GeV at (%3d, %3d) and at t = %.2f", getHitEnergy(hit), getHitX(hit), getHitY(hit), getHitTime(hit));
    }
    
    private static final double getHitEnergy(CalorimeterHit hit) {
        return hit.getCorrectedEnergy();
    }
    
    private static final int getHitX(CalorimeterHit hit) {
        return hit.getIdentifierFieldValue("ix");
    }
    
    private static final int getHitY(CalorimeterHit hit) {
        return hit.getIdentifierFieldValue("iy");
    }
    
    private static final double getHitTime(CalorimeterHit hit) {
        return hit.getTime();
    }
    
    /**
     * Compares the argument <code>CalorimeterHit</code> <code>hit</code>
     * against the <code>CalorimeterHit</code> <code>seed</code> to see
     * if <code>seed</code> meets the criteria for a seed hit given the
     * presence of <code>hit</code>, which is assumed to be located in
     * the appropriate spatiotemporal window.<br/>
     * <br/>
     * Note that it is the responsibility of the calling method to
     * ascertain whether the two <code>CalorimeterHit</code> objects
     * are actually within the proper spatial and temporal windows of
     * one another.
     * @param seed - The potential seed hit.
     * @param hit - The hit with which to compare the seed.
     * @return Returns <code>true</code> if either the two hits are the
     * same hit or if the hit does not invalidate the potential seed.
     * Returns <code>false</code> otherwise.
     */
    private boolean isValidSeed(CalorimeterHit seed, CalorimeterHit hit) {
        // Get the hit and seed energies.
        double henergy = hit.getCorrectedEnergy();
        double senergy = seed.getCorrectedEnergy();
        
        // If the hit energy is less than the seed, the seed is valid.
        if(henergy < senergy) {
            return true;
        }
        
        // If the hit energy is the same as the seed energy, spatial
        // comparisons are used to ensure the uniqueness of the seed.
        if(henergy == senergy) {
            // Get the x-indices of the hits.
            int six = seed.getIdentifierFieldValue("ix");
            int hix = hit.getIdentifierFieldValue("ix");
            
            // The hit closest to the electron-side of the detector
            // is considered the seed.
            if(six < hix) { return true; }
            else if(six > hix) { return false; }
            
            // If both hits are at the same x-index, compare how close
            // they are to the beam gap.
            else {
                // Get the y-indices. The absolute values are used
                // because closeness to iy = 0 represents closeness
                // to the beam gap.
                int siy = Math.abs(seed.getIdentifierFieldValue("iy"));
                int hiy = Math.abs(seed.getIdentifierFieldValue("iy"));
                
                // If the seed is closer, it is valid.
                if(siy < hiy) { return true; }
                else if(siy > hiy) { return false; }
                
                // If the y-index is the same, these are the same hit.
                // This case shouldn't really ever happen, but for the
                // compiler's sake, it returns true. A hit can not render
                // itself invalid for the purpose of being a seed.
                else { return true; }
            }
        }
        
        // Otherwise, the seed is invalid.
        else { return false; }
    }
    
    /**
     * Checks whether the <code>CalorimeterHit</code> <code>hit</code>
     * is within the 3x3 spatial window of <code>CalorimeterHit</code>
     * <code>seed</code>. This is defined as <code>seed</code> having
     * an x-index within +/-1 of the x-index of <code>hit</code> and
     * similarly for the y-index. Allowance is made for the fact that
     * the x-indices go from -1 to 1 and skip zero.
     * @param seed - The seed hit.
     * @param hit - The comparison hit.
     * @return Returns <code>true</code> if either both hits are the
     * the same hit or if the comparison hit is within 1 index of the
     * seed's x-index and within 1 index of the seed's y-index. Returns
     * <code>false</code> otherwise.
     */
    private boolean withinSpatialWindow(CalorimeterHit seed, CalorimeterHit hit) {
        // Get the y-indices of each hit.
        int siy = seed.getIdentifierFieldValue("iy");
        int hiy = hit.getIdentifierFieldValue("iy");
        
        // Ensure that the y-indices are either the same or are within
        // one of one another.
        if((siy == hiy) || (siy + 1 == hiy) || (siy - 1 == hiy)) {
            // Get the x-indices of each hit.
            int six = seed.getIdentifierFieldValue("ix");
            int hix = hit.getIdentifierFieldValue("ix");
            
            // If the x-indices are the same or within one of each other
            // then the crystals are within the spatial window of one
            // another.
            if((six == hix) || (six + 1 == hix) || (six - 1 == hix)) {
                return true;
            }
            
            // Otherwise, check for the special case where ix = 1 is
            // considered to be adjacent to ix = -1 rather than the
            // expected ix = 0. (ix = 0 does not exist.)
            else {
                // ix = -1 is adjacent to ix = 1 and vice versa.
                if((six == -1 && hix == 1) || (six == 1 && hix == -1)) {
                    return true;
                }
                
                // Any other combination that reaches this point is not
                // a valid combination.
                else { return false; }
            }
        }
        
        // If the x-index comparison fails, return false.
        return false;
    }
    
    /**
     * Checks whether <code>CalorimeterHit</code> <code>hit</code> is
     * within the verification temporal window for potential seed hit
     * <code>seed</code>.
     * @param seed - The seed hit.
     * @param hit - The comparison hit.
     * @return Returns <code>true</code> if the comparison hit is within
     * the temporal window of the seed hit and <code>false</code>
     * otherwise.
     */
    private boolean withinTimeVerificationWindow(CalorimeterHit seed, CalorimeterHit hit) {
        // If the hit is within the hit time window, it is valid.
        if(Math.abs(seed.getTime() - hit.getTime()) <= timeWindow) {
            return true;
        }
        
        // Otherwise, they are not.
        else { return false; }
    }
    
    /**
     * Checks whether <code>CalorimeterHit</code> <code>hit</code> is
     * within the inclusion temporal window for potential seed hit
     * <code>seed</code>.
     * @param seed - The seed hit.
     * @param hit - The comparison hit.
     * @return Returns <code>true</code> if the comparison hit is within
     * the temporal window of the seed hit and <code>false</code>
     * otherwise.
     */
    private boolean withinTimeClusteringWindow(CalorimeterHit seed, CalorimeterHit hit) {
        // Get the hit time and seed time.
        double hitTime = hit.getTime();
        double seedTime = seed.getTime();
        
        // If the hit is before the seed, use the before window.
        if(hitTime < seedTime) {
            return (seedTime - hitTime) <= timeBefore;
        }
        
        // If the hit occurs after the seed, use the after window.
        else if(hitTime > seedTime) {
            return (hitTime - seedTime) <= timeAfter;
        }
        
        // If the times are the same, they are within the window.
        if(hitTime == seedTime) { return true; }
        
        // Otherwise, one or both times is undefined and should not be
        // treated as within time.
        else { return false; }
    }
}