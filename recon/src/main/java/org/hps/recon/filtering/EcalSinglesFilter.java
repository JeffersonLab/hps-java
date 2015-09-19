package org.hps.recon.filtering;

import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;

/**
 * Accept events containing an ECal cluster passing cuts.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: $
 */
public class EcalSinglesFilter extends EventReconFilter {

    private String clusterCollectionName = "EcalClusters";
    private double minX = -500;
    private double maxX = 500;
    private double minY = -500;
    private double maxY = 500;
    private int minSize = 3;
    private int maxSize = 7;
    private double minT = 42.0;
    private double maxT = 49.0;
    private double minE = 0.7;
    private double maxE = 1.05;

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void setMinX(double minX) {
        this.minX = minX;
    }

    public void setMaxX(double maxX) {
        this.maxX = maxX;
    }

    public void setMinY(double minY) {
        this.minY = minY;
    }

    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }

    public void setMinSize(int minSize) {
        this.minSize = minSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public void setMinT(double minT) {
        this.minT = minT;
    }

    public void setMaxT(double maxT) {
        this.maxT = maxT;
    }

    public void setMinE(double minE) {
        this.minE = minE;
    }

    public void setMaxE(double maxE) {
        this.maxE = maxE;
    }

    @Override
    public void process(EventHeader event) {
        incrementEventProcessed();
        if (event.hasCollection(Cluster.class, clusterCollectionName)) {
            List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
            boolean acceptEvent = false;
            for (Cluster cluster : clusters) {
                int clusterSize = cluster.getCalorimeterHits().size();
                double clusterTime = ClusterUtilities.getSeedHitTime(cluster);
                if (clusterSize <= minSize || clusterSize >= maxSize) {
                    continue;
                }
                if (clusterTime < minT || clusterTime > maxT) {
                    continue;
                }
                if (cluster.getEnergy() < minE || cluster.getEnergy() > maxE) {
                    continue;
                }
                if (cluster.getPosition()[0] < minX || cluster.getPosition()[0] > maxX) {
                    continue;
                }
                if (cluster.getPosition()[1] < minY || cluster.getPosition()[1] > maxY) {
                    continue;
                }
                acceptEvent = true;
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
