package org.hps.users.holly;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hps.recon.ecal.EcalUtils;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.base.BaseCluster;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 *
 * The clustering algorithm is from pages 83 and 84 of the HPS Proposal.
 *
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 * @author Tim Nelson <tknelson@slac.stanford.edu>
 *
 * @version $Id: EcalClusterer.java,v 1.1 2013/02/25 22:39:24 meeg Exp $
 */
public class EcalClustererCosmics extends Driver {

    HPSEcal3 ecal;
    String ecalCollectionName;
    String ecalName = "Ecal";
    String clusterCollectionName = "EcalClusters";
    // Minimum E for cluster seed.
    double seedEMin = .05 * EcalUtils.GeV;
    // Minimum E to add hit to cluster.
    double addEMin = .03 * EcalUtils.GeV;
    // Odd or even number of crystals in X.
    boolean oddX;
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;

    public EcalClustererCosmics() {
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setSeedEMin(double seedEMin) {
        this.seedEMin = seedEMin;
    }

    public void setAddEMin(double addEMin) {
        this.addEMin = addEMin;
        if (seedEMin < addEMin) {
            seedEMin = addEMin;
        }
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setEcalName(String ecalName) {
        this.ecalName = ecalName;
    }

    public void startOfData() {
        if (ecalCollectionName == null) {
            throw new RuntimeException("The parameter ecalCollectionName was not set!");
        }

        if (ecalName == null) {
            throw new RuntimeException("The parameter ecalName was not set!");
        }
    }

    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);

        // Cache ref to neighbor map.
        neighborMap = ecal.getNeighborMap();

        //System.out.println(ecal.getName());
        //System.out.println("  nx="+ecal.nx());
        //System.out.println("  ny="+ecal.ny());
        //System.out.println("  beamgap="+ecal.beamGap());
        //System.out.println("  dface="+ecal.distanceToFace());

        //System.out.println(neighborMap.toString());
    }

    public void process(EventHeader event) {
        //System.out.println(this.getClass().getCanonicalName() + " - process");

        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();
            
            System.out.println("Number of ECal hits: "+hitMap.size());
            
            for (CalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }

            // Put Cluster collection into event.
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            event.put(clusterCollectionName, createClusters(hitMap), Cluster.class, flag);
        }
    }

    public List<Cluster> createClusters(Map<Long, CalorimeterHit> map) {

        // New Cluster list to be added to event.
        List<Cluster> clusters = new ArrayList<Cluster>();

        // Loop over ECal hits to find cluster seeds.
        for (CalorimeterHit hit : map.values()) {
        //  int ix = hit.getIdentifierFieldValue("ix");
         //   int iy = hit.getIdentifierFieldValue("iy");
      //      System.out.println("ix = "+ix);
      //      System.out.println("iy = "+iy);
            
                
            // Cut on min seed E.
            if (hit.getRawEnergy() < seedEMin) {
                continue;
            }

            // Get neighbor crystal IDs.
            Set<Long> neighbors = neighborMap.get(hit.getCellID());

            if (neighbors == null) {
                throw new RuntimeException("Oops!  Set of neighbors is null!");
            }

            // List for neighboring hits.
            List<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();

            // Loop over neighbors to make hit list for cluster.
            boolean isSeed = true;
            for (Long neighborId : neighbors) {
                // Find the neighbor hit in the event if it exists.
                CalorimeterHit neighborHit = map.get(neighborId);

                // Was this neighbor cell hit?
                if (neighborHit != null) {
                    // Check if neighbor cell has more energy.
                    if (neighborHit.getRawEnergy() > hit.getRawEnergy()) {
                        // Neighbor has more energy, so cell is not a seed.
                        isSeed = false;
                        break;
                    }

                    // Add to cluster if above min E.
                    if (neighborHit.getRawEnergy() >= addEMin) {
                        neighborHits.add(neighborHit);
                    }
                }
            }

            // Did we find a seed?
            if (isSeed) {
                // Make a cluster from the hit list.
                BaseCluster cluster = new BaseCluster();
                cluster.addHit(hit);
                for (CalorimeterHit clusHit : neighborHits) {
                    cluster.addHit(clusHit);
                }
                clusters.add(cluster);
            }
        }
        return clusters;
    }
}