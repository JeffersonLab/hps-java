package org.hps.analysis.alignment;

import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A Graf
 */
public class Strip2019FeeForAlignment extends Driver {

    int _numberOfEventsSelected;
    private AIDA aida = AIDA.defaultInstance();

    int maxNClusters = 1;
    double _minClusterEnergy = 3.5;
    double _minSeedHitEnergy = 0.;

    private int _minNumberOfTracks = 1;
    private int _maxNumberOfTracks = 1;
    private int _minNumberOfHitsOnTrack = 6;

    private boolean _skipMonsterEvents = false;
    private int _maxSvtRawTrackerHits = 250;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        if (event.get(RawTrackerHit.class, "SVTRawTrackerHits").size() < _maxSvtRawTrackerHits) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
            //System.out.println(clusters.size()+ "clusters");
            if (clusters.size() > 0 && clusters.size() <= maxNClusters) {
                for (Cluster cluster : clusters) {
                    //System.out.println("cluster energy "+cluster.getEnergy());
                    if (cluster.getEnergy() > _minClusterEnergy) {
                        double seedEnergy = ClusterUtilities.findSeedHit(cluster).getCorrectedEnergy();
                        if (seedEnergy > _minSeedHitEnergy) {
                            List<Track> tracks = event.get(Track.class, "GBLTracks");
                            int nTracks = tracks.size();
                            //System.out.println(nTracks+" GBL tracks");
                            if (nTracks >= _minNumberOfTracks && nTracks <= _maxNumberOfTracks) {
                                for (Track t : tracks) {
                                    int nhits = t.getTrackerHits().size();
                                    //System.out.println("with "+nhits+" hits");
                                    if (nhits > _minNumberOfHitsOnTrack) {
                                        skipEvent = false;
                                        //System.out.println("good");
                                        aida.histogram2D("Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cluster.getPosition()[0], cluster.getPosition()[1]);
                                        aida.histogram1D("Cluster energy", 100, 0., 7.).fill(cluster.getEnergy());
                                        aida.histogram2D("Cluster energy vs seed energy", 50, 0.5, 4.0, 50, 3.0, 5.0).fill(seedEnergy, cluster.getEnergy());
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            _numberOfEventsSelected++;
        }
    }

    protected void endOfData() {
        System.out.println("Selected " + _numberOfEventsSelected + " events");
    }
}
