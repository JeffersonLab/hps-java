package org.hps.analysis.ecal;

import hep.aida.ICloud2D;
import hep.aida.IHistogram1D;

import java.util.List;

import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawCalorimeterHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Diagnostic plots for HPS ECal.
 *
 * @author Sho Uemura <meeg@slac.stanford.edu>
 * @version $Id: HPSEcalFADCPlotsDriver.java,v 1.4 2013/02/25 22:39:26 meeg Exp $
 */
public class HPSEcalFADCPlotsDriver extends Driver {

    String edepCollectionName = "EcalHits";
    String rawCollectionName = null;
    String ecalCollectionName = null;
    String clusterCollectionName = "EcalClusters";
    AIDA aida = AIDA.defaultInstance();
    IHistogram1D edepE;
    IHistogram1D rawE;
    IHistogram1D ecalE;
    IHistogram1D clusterE;
    ICloud2D window_E;
    double edepThreshold = 0.05;

    public void setEdepThreshold(double edepThreshold) {
        this.edepThreshold = edepThreshold;
    }

    public void setRawCollectionName(String rawCollectionName) {
        this.rawCollectionName = rawCollectionName;
    }

    public void setEcalCollectionName(String ecalCollectionName) {
        this.ecalCollectionName = ecalCollectionName;
    }

    public void setClusterCollectionName(String clusterCollectionName) {
        this.clusterCollectionName = clusterCollectionName;
    }

    public void startOfData() {
        edepE = aida.histogram1D(
                "FADC plots: " + edepCollectionName + " : Hits",
                500, 0.0, 5.0);
        if (rawCollectionName != null) {
            rawE = aida.histogram1D(
                    "FADC plots: " + rawCollectionName + " : Hits",
                    500, 0.0, 500.0);
            window_E = aida.cloud2D("FADC plots: " + rawCollectionName + " : Window vs. E");
        }
        if (ecalCollectionName != null) {
            ecalE = aida.histogram1D(
                    "FADC plots: " + ecalCollectionName + " : Hits",
                    500, 0.0, 5.0);
        }
        clusterE = aida.histogram1D(
                "FADC plots: " + clusterCollectionName + " : Clusters",
                500, 0.0, 5.0);
    }

    public void process(EventHeader event) {
        List<Cluster> clusters = event.get(Cluster.class, clusterCollectionName);
        if (clusters == null)
            throw new RuntimeException("Missing cluster collection!");

        List<CalorimeterHit> edepHits = event.get(CalorimeterHit.class, edepCollectionName);
        if (edepHits == null)
            throw new RuntimeException("Missing hit collection!");

        if (rawCollectionName != null) {
            List<RawCalorimeterHit> rawHits = event.get(RawCalorimeterHit.class, rawCollectionName);
            if (rawHits == null)
                throw new RuntimeException("Missing hit collection!");

            for (RawCalorimeterHit hit : rawHits) {
                rawE.fill(hit.getAmplitude());
                //window_E.fill(hit.getAmplitude(),hit.getWindowSize());
            }
        }

        if (ecalCollectionName != null) {
            List<CalorimeterHit> ecalHits = event.get(CalorimeterHit.class, ecalCollectionName);
            if (ecalHits == null)
                throw new RuntimeException("Missing hit collection!");

            for (CalorimeterHit hit : ecalHits) {
                ecalE.fill(hit.getRawEnergy());
            }
        }

        for (CalorimeterHit hit : edepHits) {
            if (hit.getRawEnergy() > edepThreshold)
                edepE.fill(hit.getRawEnergy());
        }
        for (Cluster cluster : clusters) {
            clusterE.fill(cluster.getEnergy());
        }
    }
}
