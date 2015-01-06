package org.hps.recon.ecal.cluster;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;

/**
 * Class <code>GTPOnlineClusterer</code> is an implementation of the
 * GTP clustering algorithm for EVIO readout data for use in either
 * online reconstruction/diagnostics or for general analysis of EVIO
 * readout data.<br/>
 * <br/>
 * The GTP algorithm searches the set of hits in a readout event and
 * compares them to select those that are a maximum in their 3x3 window
 * and across a period of time that can be set. Hits that are maxima
 * are declared "cluster seeds" and a cluster is created from them.
 * All hits within the clustering window of the seed time are then
 * added to the cluster and it is written to the event stream.<br/>
 * <br/>
 * The GTP algorithm uses three time windows. THe verification window
 * is the time around the seed hit in which it is required to have more
 * energy than any other hits in the 3x3 spatial window surrounding it.
 * This is always symmetric. The other two windows combined define the
 * clustering window. The clustering window is composed of a window
 * before and a window after the seed time. These may be defined using
 * different values. The window after should be as long or longer than
 * the window before to make physical sense. The verification window is
 * then defined as the larger of the two constituents of the clustering
 * window. This is required for clustering to be consistent.
 * 
 * @author Kyle McCarty <mccarty@jlab.org>
 */
// TODO: Handle time window arguments.
public class GTPOnlineClusterer extends AbstractClusterer {

    // The size of the temporal window in nanoseconds. By default,
    // this is 1 clock-cycle before and 3 clock-cycles after.
    private double timeBefore = 4;
    private double timeAfter = 12;
    private double timeWindow = 12;

    // Cluster formation energy thresholds. Currently, the hardware
    // only supports a lower bound seed energy. Units are in GeV.
    private double seedThreshold = 0.050;

    // Internal variables.
    private boolean verbose = false;
    
    public GTPOnlineClusterer() {
        super(new String[] { "seedThreshold" }, 
                new double[] { 0.050 });
    }

    public void initialize() {
        seedThreshold = getCuts().getValue("seedThreshold");
    }

    /**
     * Reads in hits and processes them into clusters as per the GTP
     * clustering algorithm implemented in the hardware.
     * @param event - The object containing event data.
     */
    @Override
    public List<Cluster> createClusters(EventHeader event, List<CalorimeterHit> hitList) {

        // Track the valid clusters.
        List<Cluster> clusterList = new ArrayList<Cluster>();

        // VERBOSE :: Indicate whether the event has hits.
        //if(verbose) { System.out.printf("Event %7d :: Has hits [%5b]%n", event.getEventNumber(), hasHits); }

        // Sort the hits by time in reverse order.
        Collections.sort(hitList, new Comparator<CalorimeterHit>() {
            @Override
            public int compare(CalorimeterHit firstHit, CalorimeterHit secondHit) {
                return Double.compare(secondHit.getTime(), firstHit.getTime());
            }
        }); 

        // VERBOSE :: Print the hit information.
        if(verbose) { 
            for(CalorimeterHit hit : hitList) {
                int ix = hit.getIdentifierFieldValue("ix");
                int iy = hit.getIdentifierFieldValue("iy");
                double energy = hit.getCorrectedEnergy();
                double time = hit.getTime();

                System.out.printf("\tHit --> %.3f GeV at (%3d, %3d) and at t = %.2f%n", energy, ix, iy, time);
            }
        }

        // A seed hit is a hit that is the largest both within its
        // spatial range (+/- 1 in the ix and iy direction) and
        // within a certain temporal window. If a hit is a seed, all
        // hits within the 3x3 spatial range around it that are also
        // within the temporal window are considered part of the
        // cluster.

        // Iterate over each hit and see if it qualifies as a seed hit.
        seedLoop:
            for(CalorimeterHit seed : hitList) {
                // Check whether the potential seed passes the seed
                // energy cut.
                if(seed.getCorrectedEnergy() < seedThreshold) {
                    continue seedLoop;
                }

                // Create a cluster for the potential seed.
                BaseCluster protoCluster = new HPSEcalCluster(); // FIXME: Should be changed to BaseCluster but needs prop calculations for now.
                protoCluster.addHit(seed);

                // Iterate over the other hits and if the are within
                // the clustering spatiotemporal window, compare their
                // energies.
                for(CalorimeterHit hit : hitList) {
                    // Do not perform the comparison if the hit is the
                    // current potential seed.
                    if (hit != seed) {
                        // Check if the hit is within the spatiotemporal
                        // clustering window.
                        if (withinTimeVerificationWindow(seed, hit) && withinSpatialWindow(seed, hit)) {
                            // Check if the hit invalidates the potential
                            // seed.
                            if (isValidSeed(seed, hit)) {
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
                }

                // If this point is reached, then the seed was not
                // invalidated by any of the other hits and is really
                // a cluster center. Add the cluster to the list.
                clusterList.add(protoCluster);
            }

        // VERBOSE :: Print out all the clusters in the event.
        if(verbose) { 
            for(Cluster cluster : clusterList) {
                CalorimeterHit seedHit = cluster.getCalorimeterHits().get(0);
                int ix = seedHit.getIdentifierFieldValue("ix");
                int iy = seedHit.getIdentifierFieldValue("iy");
                double energy = cluster.getEnergy();
                double time = seedHit.getTime();

                System.out.printf("\tCluster --> %.3f GeV at (%3d, %3d) and at t = %.2f%n", energy, ix, iy, time);

                for(CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    int hix = hit.getIdentifierFieldValue("ix");
                    int hiy = hit.getIdentifierFieldValue("iy");
                    double henergy = hit.getCorrectedEnergy();
                    double htime = hit.getTime();
                    System.out.printf("\t\tCompHit --> %.3f GeV at (%3d, %3d) and at t = %.2f%n", henergy, hix, hiy, htime);
                }
            }
        }


        // VERBOSE :: Print a new line.
        if(verbose) { System.out.println(); }

        return clusterList;
    }

    /**
     * Checks whether the hit <code>hit</code> keeps the hit <code>seed
     * </code> from meeting the criteria for being a seed hit. Note that
     * this does not check to see if the two hits are within the valid
     * spatiotemporal window of one another.
     * @param seed - The potential seed hit.
     * @param hit - The hit to compare with the seed.
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
     * Checks whether the hit <code>hit</code> falls within the spatial
     * window of the hit <code>Seed</code>. This is defined as within
     * 1 index of the seed's x-index and similarly for the seed's
     * y-index. 
     * @param seed - The seed hit.
     * @param hit - The comparison hit.
     * @return Returns <code>true</code> if either both hits are the
     * the same hit or if the comparison hit is within 1 index of the
     * seed's x-index and within 1 index of the seed's y-index. Returns
     * <code>false</code> otherwise.
     */
    private boolean withinSpatialWindow(CalorimeterHit seed, CalorimeterHit hit) {
        // Get the x-indices of each hit.
        int six = seed.getIdentifierFieldValue("ix");
        int hix = hit.getIdentifierFieldValue("ix");

        // Check that the x indices are either the same or within a
        // range of one of one another.
        if((six == hix) || (six + 1 == hix) || (six - 1 == hix)) {
            // Get the y-indices of each hit.
            int siy = seed.getIdentifierFieldValue("iy");
            int hiy = hit.getIdentifierFieldValue("iy");

            // Ensure that the y-indices are either the same or are
            // within one of one another.
            return (siy == hiy) || (siy + 1 == hiy) || (siy - 1 == hiy);
        }

        // If the x-index comparison fails, return false.
        return false;
    }

    /**
     * Checks whether the hit <code>hit</code> is within the temporal
     * window of the hit <code>seed</code> for the purpose of seed
     * verification.
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
     * Checks whether the hit <code>hit</code> is within the temporal
     * window of the hit <code>seed</code> for the purpose of adding
     * a hit to a cluster.
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

        // If the times are the same, the are within the window.
        if(hitTime == seedTime) { return true; }

        // Otherwise, one or both times is undefined and should not be
        // treated as within time.
        else { return false; }
    }
 
    /**
     * Sets the minimum energy a hit must have before it will be
     * considered for cluster formation.
     * @param seedThreshold - The seed threshold in GeV.
     */
    public void setSeedLowThreshold(double seedThreshold) {
        this.seedThreshold = seedThreshold;
    }

    /**
     * Sets the number of clock-cycles to include in the clustering
     * window before the seed hit. One clock-cycle is four nanoseconds.
     * @param cyclesBefore - The length of the clustering window before
     * the seed hit in clock cycles.
     */
    public void setWindowBefore(int cyclesBefore) {
        timeBefore = cyclesBefore * 4;
        timeWindow = Math.max(timeBefore, timeAfter);
    }

    /**
     * Sets the number of clock-cycles to include in the clustering
     * window after the seed hit. One clock-cycle is four nanoseconds.
     * @param cyclesAfter - The length of the clustering window after
     * the seed hit in clock cycles.
     */
    public void setWindowAfter(int cyclesAfter) {
        timeAfter = cyclesAfter * 4;
        timeWindow = Math.max(timeBefore, timeAfter);
    }
}
