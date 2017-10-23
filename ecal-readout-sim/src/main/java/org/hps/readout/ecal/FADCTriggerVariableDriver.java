/*
 * To change this template, choose Tools | Templates and open the template in the editor.
 */
package org.hps.readout.ecal;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;

/**
 * Dumps trigger variables to text file
 * 
 * @author phansson <phansson@slac.stanford.edu>
 * @version $id: $
 */
public class FADCTriggerVariableDriver extends FADCTriggerDriver {

    private int _pairs = 0;

    public FADCTriggerVariableDriver() {
    }

    @Override
    public void startOfData() {
        if (!"".equals(outputFileName)) {
            try {
                outputStream = new PrintWriter(outputFileName);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(FADCTriggerVariableDriver.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            throw new RuntimeException("Need to supply a output file!");
        }

        String str = "event/I:beamenergy/F:pairid/I:cl1E/F:cl1posx/F:cl1posy/F:cl2E/F:cl2posx/F:cl2posy/F";

        outputStream.println(str);

    }

    @Override
    public void detectorChanged(Detector detector) {
        setCutsFromBeamEnergy(getBeamEnergyFromDetector(detector));
    }

    @Override
    public void process(EventHeader event) {
        // super.process(event);

        if (event.hasCollection(Cluster.class, clusterCollectionName)) {

            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);

            // System.out.printf("%d ecal clusters in event\n", clusters.size());
            // System.out.printf("%s: %d clusters\n",this.getClass().getSimpleName(),clusters.size());
            // for(Cluster cl : clusters) {
            // System.out.printf("%s: cl E %f x %f y %f \n",this.getClass().getSimpleName(),cl.getEnergy(),cl.getPosition()[0],cl.getPosition()[1]);
            // }
            List<Cluster> unique_clusters = this.getUniqueClusters(clusters);
            // System.out.printf("%s: %d unique clusters\n",this.getClass().getSimpleName(),unique_clusters.size());
            // for(Cluster cl : unique_clusters) {
            // System.out.printf("%s: cl E %f x %f y %f \n",this.getClass().getSimpleName(),cl.getEnergy(),cl.getPosition()[0],cl.getPosition()[1]);
            // }

            updateClusterQueues(unique_clusters);
            List<Cluster[]> clusterPairs = getClusterPairsTopBot();
            boolean foundClusterPairs = !clusterPairs.isEmpty();

            if (foundClusterPairs) {

                int ipair = 0;
                for (Cluster[] pair : clusterPairs) {

                    String evString = String.format("%d %f %d ", event.getEventNumber(), this.beamEnergy, ipair);
                    for (int icluster = 0; icluster != 2; icluster++) {

                        Cluster cluster = pair[icluster];

                        // int quad = ECalUtils.getQuadrant(cluster);
                        double E = cluster.getEnergy();
                        double pos[] = cluster.getCalorimeterHits().get(0).getPosition();
                        // System.out.printf("x %f y %f ix %d iy %d \n", pos[0], pos[1],
                        // cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("ix"),
                        // cluster.getCalorimeterHits().get(0).getIdentifierFieldValue("iy"));

                        evString += String.format("%f %f %f ", E, pos[0], pos[1]);
                    }
                    // System.out.printf("%s\n",evString);
                    outputStream.println(evString);
                    ++ipair;
                    ++_pairs;
                } // pairs
            }

        } // has clusters
        else {
            // System.out.printf("No ecal cluster collection in event %d \n", event.getEventNumber());
        }

    }

    @Override
    public void endOfData() {

        System.out.printf("%s: processed %d pairs\n", this.getClass().getSimpleName(), this._pairs);

        outputStream.close();

    }

    private List<Cluster> getUniqueClusters(List<Cluster> clusters) {
        List<Cluster> unique = new ArrayList<Cluster>();
        for (Cluster loop_cl : clusters) {
            ClusterCmp loop_clCmp = new ClusterCmp(loop_cl);
            boolean found = false;
            for (Cluster cl : unique) {
                if (loop_clCmp.compareTo(cl) == 0) {
                    found = true;
                }
            }
            if (!found) {
                unique.add(loop_cl);
            }
        }
        return unique;
    }

    private static class ClusterCmp implements Comparable<Cluster> {

        private Cluster _cluster;

        public ClusterCmp(Cluster cl) {
            set_cluster(cl);
        }

        @Override
        public int compareTo(Cluster cl) {
            if (cl.getEnergy() == get_cluster().getEnergy() && cl.getPosition()[0] == get_cluster().getPosition()[0]
                    && cl.getPosition()[1] == get_cluster().getPosition()[1]) {
                return 0;
            } else {
                if (cl.getEnergy() > get_cluster().getEnergy()) {
                    return 1;
                } else {
                    return -1;
                }
            }
        }

        public Cluster get_cluster() {
            return _cluster;
        }

        public void set_cluster(Cluster _cluster) {
            this._cluster = _cluster;
        }

    }

}
