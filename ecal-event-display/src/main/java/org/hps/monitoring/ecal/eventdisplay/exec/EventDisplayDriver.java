package org.hps.monitoring.ecal.eventdisplay.exec;

import java.util.List;

import org.hps.monitoring.ecal.eventdisplay.event.EcalHit;
import org.hps.monitoring.ecal.eventdisplay.ui.PEventViewer;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.geometry.Detector;
import org.lcsim.util.Driver;

/**
 * This is a <code>Driver</code> for running the ECAL Event Display.
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

        org.hps.monitoring.ecal.eventdisplay.event.Cluster eventDisplayCluster;

        if (event.hasCollection(CalorimeterHit.class, inputCollection)) {
            List<CalorimeterHit> hits = event.get(CalorimeterHit.class, inputCollection);
            for (CalorimeterHit hit : hits) {
                viewer.addHit(new EcalHit(hit.getIdentifierFieldValue("ix"), hit.getIdentifierFieldValue("iy"), hit.getRawEnergy()));
            }
        }
        if (event.hasCollection(Cluster.class, clusterCollection)) {
            List<Cluster> clusters = event.get(Cluster.class, clusterCollection);
            for (Cluster cluster : clusters) {
                CalorimeterHit seedHit = cluster.getCalorimeterHits().get(0);
                eventDisplayCluster = new org.hps.monitoring.ecal.eventdisplay.event.Cluster(seedHit.getIdentifierFieldValue("ix"), seedHit.getIdentifierFieldValue("iy"), cluster.getEnergy());
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
