package org.hps.monitoring.ecal.exec;

import java.util.List;

import org.hps.monitoring.ecal.event.Cluster;
import org.hps.monitoring.ecal.event.EcalHit;
import org.hps.monitoring.ecal.ui.PEventViewer;
import org.hps.recon.ecal.HPSEcalCluster;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This is a <code>Driver</code> for running the ECAL Event Display.
 * @author Jeremy McCormick <jeremym@slac.stanford.edu>
 */
public class EventDisplayDriver extends Driver {

    String inputCollection = "EcalCalHits";
    String clusterCollection = "EcalClusters";
    int eventRefreshRate = 1;
    int eventn = 0;
    private PEventViewer viewer;

    public EventDisplayDriver() {
    }

    public void setInputCollection(String inputCollection) {
        this.inputCollection = inputCollection;
    }

    public void setEventRefreshRate(int eventRefreshRate) {
        this.eventRefreshRate = eventRefreshRate;
    }

    @Override
    public void detectorChanged(Detector detector) {
        viewer = new PEventViewer();
        viewer.setVisible(true);
    }

    @Override
    public void endOfData() {
        viewer.setVisible(false);
    }

    @Override
    public void process(EventHeader event) {

        if (++eventn % eventRefreshRate != 0) {
            return;
        }

        viewer.resetDisplay();
        viewer.updateDisplay();

        Cluster eventDisplayCluster;

        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                viewer.addHit(new EcalHit(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getRawEnergy()));
            }
        }
        if (event.hasCollection(HPSEcalCluster.class, clusterCollection)) {
            List<HPSEcalCluster> clusters = event.get(HPSEcalCluster.class, clusterCollection);
            for (HPSEcalCluster cluster : clusters) {
                CalorimeterHit seedHit = cluster.getSeedHit();
                eventDisplayCluster = new Cluster(seedHit.getIdentifierFieldValue("ix"), seedHit.getIdentifierFieldValue("iy"), cluster.getEnergy());
                for (CalorimeterHit hit : cluster.getCalorimeterHits()) {
                    if (hit.getRawEnergy() != 0)
                        eventDisplayCluster.addComponentHit(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"));
                }
                viewer.addCluster(eventDisplayCluster);
            }
        }

        viewer.updateDisplay();
    }
}
