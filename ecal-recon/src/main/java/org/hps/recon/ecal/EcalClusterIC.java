package org.hps.recon.ecal;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.HPSEcal3;
import org.lcsim.geometry.subdetector.HPSEcal3.NeighborMap;
import org.lcsim.lcio.LCIOConstants;
import org.lcsim.util.Driver;

/**
 * This Driver creates clusters from the CalorimeterHits of an
 * {@link org.lcsim.geometry.subdetectur.HPSEcal3} detector.
 *
 *
 * @author Holly Szumila-Vance <hszumila@jlab.org>
 *
 */
public class EcalClusterIC extends Driver {

    FileWriter writeHits;
    HPSEcal3 ecal;
    String ecalCollectionName;
    String ecalName = "Ecal";
    String clusterCollectionName = "EcalClusters";
    // Map of crystals to their neighbors.
    NeighborMap neighborMap = null;
    //Minimum energy that counts as hit
    double Emin = 0.001;

    public EcalClusterIC() {
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
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

        try {
            writeHits = new FileWriter("cluster-hit-IC.txt");
            writeHits.write("");
        } catch (IOException e) {
        };
    }

    public void detectorChanged(Detector detector) {
        // Get the Subdetector.
        ecal = (HPSEcal3) detector.getSubdetector(ecalName);

        // Cache ref to neighbor map.
        neighborMap = ecal.getNeighborMap();
    }

    public void process(EventHeader event) {



        if (event.hasCollection(CalorimeterHit.class, ecalCollectionName)) {
            // Get the list of raw ECal hits.
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, ecalCollectionName);

            // Make a hit map for quick lookup by ID.
            Map<Long, CalorimeterHit> hitMap = new HashMap<Long, CalorimeterHit>();

            for (CalorimeterHit hit : hits) {
                hitMap.put(hit.getCellID(), hit);
            }

            //System.out.println("Number of ECal hits: "+hitMap.size());

            // Put Cluster collection into event.
            int flag = 1 << LCIOConstants.CLBIT_HITS;
            try {

                event.put(clusterCollectionName, createClusters(hitMap), HPSEcalCluster.class, flag);
            } catch (IOException e) {
            }
        }
    }

    public List<HPSEcalCluster> createClusters(Map<Long, CalorimeterHit> map) throws IOException {

        // New Cluster list to be added to event.
        List<HPSEcalCluster> clusters = new ArrayList<HPSEcalCluster>();

        //Create a Calorimeter hit list in each event, then sort with highest energy first
        ArrayList<CalorimeterHit> chitList = new ArrayList<CalorimeterHit>(map.size());
        for (CalorimeterHit h : map.values()) {
            if (h.getCorrectedEnergy() > Emin) {
                chitList.add(h);
            }

            Collections.sort(chitList, new EnergyComparator());
        }

        //New Seed list containing each local maximum energy hit
        List<CalorimeterHit> seedHits = new ArrayList<CalorimeterHit>();

        //Create map to contain common hits for evaluation later, key is crystal and values are seed
        Map<CalorimeterHit, List<CalorimeterHit>> commonHits = new HashMap<CalorimeterHit, List<CalorimeterHit>>();

        //Created map to contain seeds with listed hits, key is crystal, and value is seed
        Map<CalorimeterHit, CalorimeterHit> clusterHits = new HashMap<CalorimeterHit, CalorimeterHit>();

        //Create map to contain the total energy of each cluster
        Map<CalorimeterHit, Double> seedEnergy = new HashMap<CalorimeterHit, Double>();

        //Quick Map to access hits from cell IDs
        Map<Long, CalorimeterHit> hitID = new HashMap<Long, CalorimeterHit>();

        //List to contain all hits already put into clusters
        List<CalorimeterHit> clusterHitList = new ArrayList<CalorimeterHit>();

        //Fill Map with cell ID and hit
        for (CalorimeterHit hit : chitList) {
            hitID.put(hit.getCellID(), hit);
        }

        for (CalorimeterHit hit : chitList) {
            Set<Long> neighbors = neighborMap.get(hit.getCellID()); //get all neighbors of hit
            List<CalorimeterHit> neighborHits = new ArrayList<CalorimeterHit>();
            for (Long neighbor : neighbors) {
                if (hitID.containsKey(neighbor)) {//map contains (neighbor's cell id, neighbor hit)
                    neighborHits.add(hitID.get(neighbor));
                }
            }

            Collections.sort(neighborHits, new EnergyComparator());

            boolean highestE = true;
            for (CalorimeterHit neighborHit : neighborHits) {//check for seed hit
                if (hit.getCorrectedEnergy() > neighborHit.getCorrectedEnergy()) {
                    continue;
                } else {
                    highestE = false;
                    break;
                }
            }

            if (highestE == true) {//seed hit, local maximum
                seedHits.add(hit);
                clusterHits.put(hit, hit);
                clusterHitList.add(hit);
            } //not seed hit
            else {
                //builds immediate clusters around seed hits
                for (CalorimeterHit neighborHit : neighborHits) {
                    //if neighbor to seed, add to seed
                    if (seedHits.contains(neighborHit) && !clusterHits.containsKey(hit)) {
                        CalorimeterHit seed = clusterHits.get(neighborHit);
                        clusterHits.put(hit, seed);
                        clusterHitList.add(hit);
                    } //if neighbor to two seeds, add to common hits
                    else if (seedHits.contains(neighborHit) && clusterHits.containsKey(hit)) {
                        CalorimeterHit prevSeed = clusterHits.get(neighborHit);
                        CalorimeterHit currSeed = clusterHits.get(hit);
                        List<CalorimeterHit> commonHitList = new ArrayList<CalorimeterHit>();
                        commonHitList.add(prevSeed);
                        commonHitList.add(currSeed);
                        commonHits.put(hit, commonHitList);
                    }
                }
            }
        }

        //loop over hit list, find neighbors, compare energies, add to list 
        for (CalorimeterHit nHit : chitList) {
            Set<Long> neighbors2 = neighborMap.get(nHit.getCellID()); //get all neighbors of hit
            List<CalorimeterHit> neighborHits2 = new ArrayList<CalorimeterHit>();
            for (Long neighbor : neighbors2) {
                if (hitID.containsKey(neighbor)) {//map contains (neighbor's cell id, neighbor hit)
                    if (!clusterHitList.contains(hitID.get(neighbor))) {
                        neighborHits2.add(hitID.get(neighbor));
                    }
                }
            }

            Collections.sort(neighborHits2, new EnergyComparator());
            for (CalorimeterHit neighbor : neighborHits2) {
                if (clusterHits.containsKey(nHit) && neighbor.getCorrectedEnergy() < nHit.getCorrectedEnergy()) {
                    CalorimeterHit seed = clusterHits.get(nHit);
                    clusterHits.put(neighbor, seed);
                    clusterHitList.add(neighbor);
                } else {
                    continue;
                }
            }
        }
        //loop over cluster hits, compare energies of neighbors with diff seeds, if min->add to commonHits
        for (CalorimeterHit mHit : clusterHitList) {
            //exclude seed hits as possible common hits
            if (clusterHits.get(mHit) != mHit) {

                Set<Long> neighbors3 = neighborMap.get(mHit.getCellID()); //get all neighbors of hit
                List<CalorimeterHit> neighborHits3 = new ArrayList<CalorimeterHit>();
                for (Long neighbor : neighbors3) {
                    if (hitID.containsKey(neighbor)) {//map contains (neighbor's cell id, neighbor hit)
                        neighborHits3.add(hitID.get(neighbor));

                    }
                }

                Collections.sort(neighborHits3, new EnergyComparator());

                CalorimeterHit compSeed = clusterHits.get(mHit);
                for (CalorimeterHit neighbor : neighborHits3) {
                    if (clusterHits.containsKey(neighbor) && clusterHits.get(neighbor) != compSeed) {//borders clusters
                        if (mHit.getCorrectedEnergy() < neighbor.getCorrectedEnergy()) {
                            //add to common hits, 
                            List<CalorimeterHit> commonHitList = new ArrayList<CalorimeterHit>();
                            commonHitList.add(compSeed);
                            commonHitList.add(clusterHits.get(neighbor));
                            commonHits.put(mHit, commonHitList);
                        }
                    }
                }
            }
        }

        //remove common hits from cluster hits list
        for (CalorimeterHit commHit : clusterHitList) {
            if (clusterHitList.contains(commHit) && commonHits.containsKey(commHit)) {
                clusterHits.remove(commHit);
            } else {
                continue;
            }
        }



        //Get energy of each cluster, excluding common hits
        for (CalorimeterHit iSeed : seedHits) {
            seedEnergy.put(iSeed, 0.0);
        }
        //Putting total cluster energies excluding common hit energies into map with seed keys    
        for (Map.Entry<CalorimeterHit, CalorimeterHit> entry : clusterHits.entrySet()) {
            CalorimeterHit eSeed = entry.getValue();
            double eEnergy = seedEnergy.get(eSeed);
            eEnergy += entry.getKey().getRawEnergy();
            seedEnergy.put(eSeed, eEnergy);
        }

        //Distribute common hit energies with clusters
        Map<CalorimeterHit, Double> seedEnergyTot = seedEnergy;
        for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry1 : commonHits.entrySet()) {
            CalorimeterHit commonCell = entry1.getKey();
            List<CalorimeterHit> commSeedList = entry1.getValue();
            CalorimeterHit seedA = commSeedList.get(0);
            CalorimeterHit seedB = commSeedList.get(1);
            double eFractionA = seedEnergy.get(seedA) / (seedEnergy.get(seedA) + seedEnergy.get(seedB));
            double eFractionB = seedEnergy.get(seedB) / (seedEnergy.get(seedA) + seedEnergy.get(seedB));
            double currEnergyA = seedEnergyTot.get(seedA);
            double currEnergyB = seedEnergyTot.get(seedB);
            currEnergyA += eFractionA * commonCell.getCorrectedEnergy();
            currEnergyB += eFractionB * commonCell.getCorrectedEnergy();

            seedEnergyTot.put(seedA, currEnergyA);
            seedEnergyTot.put(seedB, currEnergyB);
        }


        //Do some system.out for number of crystals in each cluster, energy of each cluster
        for (Map.Entry<CalorimeterHit, Double> entryEnergy : seedEnergyTot.entrySet()) {
            //    	System.out.println(entryEnergy.getKey().getCellID()+"\t"+entryEnergy.getKey().getCorrectedEnergy()+
            //    			"\t"+entryEnergy.getValue());
        }

        //     System.out.println("Number of clusters: "+seedHits.size());    


 /*       if (map.size() != 0) {
            writeHits.append("Event" + "\t" + "1" + "\n");
            for (CalorimeterHit n : chitList) {
                writeHits.append("EcalHit" + "\t" + n.getIdentifierFieldValue("ix") + "\t" + n.getIdentifierFieldValue("iy")
                        + "\t" + n.getCorrectedEnergy() + "\n");
            }

            for (Map.Entry<CalorimeterHit, CalorimeterHit> entry2 : clusterHits.entrySet()) {
                if (entry2.getKey() == entry2.getValue()) {//seed
                    writeHits.append("Cluster" + "\t" + entry2.getKey().getIdentifierFieldValue("ix")
                            + "\t" + entry2.getKey().getIdentifierFieldValue("iy") + "\t" + seedEnergyTot.get(entry2.getKey()) + "\n");

                    HPSEcalCluster cluster = new HPSEcalCluster(entry2.getKey());
                    cluster.addHit(entry2.getKey());

                    for (Map.Entry<CalorimeterHit, CalorimeterHit> entry3 : clusterHits.entrySet()) {
                        if (entry3.getValue() == entry2.getValue()) {
                            writeHits.append("CompHit" + "\t" + entry3.getKey().getIdentifierFieldValue("ix") + "\t"
                                    + entry3.getKey().getIdentifierFieldValue("iy") + "\n");

                            cluster.addHit(entry3.getKey());
                        }
                    }
                    for (Map.Entry<CalorimeterHit, List<CalorimeterHit>> entry4 : commonHits.entrySet()) {
                        if (entry4.getValue().contains(entry2.getKey())) {
                            writeHits.append("SharHit" + "\t" + entry4.getKey().getIdentifierFieldValue("ix") + "\t"
                                    + entry4.getKey().getIdentifierFieldValue("iy") + "\n");

                            cluster.addHit(entry4.getKey());

                        }
                    }

                    clusters.add(cluster);
                }
            }
            writeHits.append("EndEvent\n");

        }*/

        //Clear all maps for next event iteration
        hitID.clear();
        clusterHits.clear();
        seedHits.clear();
        commonHits.clear();
        chitList.clear();
        seedEnergy.clear();

        return clusters;




    }

    public void endOfData() {
        try {
            writeHits.close();
        } catch (IOException e) {
        }
    }

    private class EnergyComparator implements Comparator<CalorimeterHit> {

        @Override
        public int compare(CalorimeterHit o1, CalorimeterHit o2) {
            // TODO Auto-generated method stub
            double diff = o1.getCorrectedEnergy() - o2.getCorrectedEnergy();
            if (diff < 0) {
                return 1;
            }
            if (diff > 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }
}
