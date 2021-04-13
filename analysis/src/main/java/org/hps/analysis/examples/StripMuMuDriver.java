package org.hps.analysis.examples;

import java.util.List;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

public class StripMuMuDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    double clusterMaxEcut = 0.3;
    double clusterMinEcut = 0.1;
    int nMaxSvtRawTrackerHits = 500;
    int _numberOfEventsSelected;

    public void process(EventHeader event) {
        boolean skipEvent = true;
        List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
        int nMuClusters = 0;
        for (Cluster c : clusters) {
            aida.histogram1D("event cluster nHits", 20, 0., 20.).fill(c.getCalorimeterHits().size());
            aida.histogram1D("event cluster energy", 100, 0., 1.5).fill(c.getEnergy());
            aida.histogram2D("event cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);

            if (c.getEnergy() < clusterMaxEcut && c.getEnergy() > clusterMinEcut) {
                nMuClusters++;
                aida.histogram2D("muon cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);

            }
        }
        aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(clusters.size());
        aida.histogram1D("Number of muon Clusters in Event", 10, 0., 10.).fill(nMuClusters);

        if (nMuClusters > 1) {
            skipEvent = false;
        }

        //skip "monster" events
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        int nRawTrackerHits = rawTrackerHits.size();
        aida.histogram1D("SVT number of RawTrackerHits", 100, 0., 1000.).fill(nRawTrackerHits);
        if (nRawTrackerHits > nMaxSvtRawTrackerHits) {
            skipEvent = true;
        }

        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    @Override
    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }

}
