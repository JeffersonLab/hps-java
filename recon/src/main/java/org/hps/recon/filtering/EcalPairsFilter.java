package org.hps.recon.filtering;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

/**
 * Accept events containing a pair of ECal clusters within a set time
 * coincidence.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class EcalPairsFilter extends EventReconFilter {

    private String clusterCollectionName = "EcalClusters";
    private double maxDt = 2.5;
    private boolean strictPairs = false;

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setMaxDt(double maxDt) {
        this.maxDt = maxDt;
    }

    public void setStrictPairs(boolean strictPairs) {
        this.strictPairs = strictPairs;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            if (clusters.size() < 2) {
                skipEvent();
            }
            if (strictPairs && clusters.size() > 2) {
                skipEvent();
            }
            List<Double> clusterTimes = new ArrayList<Double>();
            for (Cluster cluster : clusters) {
                clusterTimes.add(ClusterUtilities.getSeedHitTime(cluster));
            }
            Collections.sort(clusterTimes);
            boolean acceptEvent = false;
            for (int i = 0; i < clusterTimes.size() - 1; i++) {
                double dt = clusterTimes.get(i + 1) - clusterTimes.get(i);
//                System.out.println(dt);
                if (dt < maxDt) {
                    acceptEvent = true;
                }
            }
            if (!acceptEvent) {
                skipEvent();
            }
        } else {
            skipEvent();
        }
        incrementEventPassed();
    }
}
