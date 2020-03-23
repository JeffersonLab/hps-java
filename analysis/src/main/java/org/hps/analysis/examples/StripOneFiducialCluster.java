package org.hps.analysis.examples;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class StripOneFiducialCluster extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private int _numberOfEventsWritten = 0;
    private boolean _writeRunAndEventNumbers = false;
    private boolean _fiducialOnly = false;

    public void process(EventHeader event) {
        boolean skipEvent = true;
        int nRawTrackerHits = 0;
        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            nRawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits").size();
        }
        if (event.hasCollection(Cluster.class, "EcalClustersCorr")) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
            aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(clusters.size());
            double maxEnergy = 0;
            List<Double> times = new ArrayList<Double>();
            List<Double> energies = new ArrayList<Double>();
            List<Boolean> isFiducial = new ArrayList<Boolean>();
            List<Boolean> isTop = new ArrayList<Boolean>();
            for (Cluster cluster : clusters) {
                aida.histogram1D("Cluster Energy", 100, 0., 6.).fill(cluster.getEnergy());
                if (cluster.getEnergy() > maxEnergy) {
                    maxEnergy = cluster.getEnergy();
                }
                times.add(ClusterUtilities.findSeedHit(cluster).getTime());
                energies.add(cluster.getEnergy());
                isFiducial.add(TriggerModule.inFiducialRegion(cluster));
                isTop.add(cluster.getPosition()[1] > 0.);
            }
            if (clusters.size() == 1) {
                aida.histogram1D("Number of rawTrackerHits", 100, 0., 1000.).fill(nRawTrackerHits);
                if (energies.get(0) > 3.0 && nRawTrackerHits < 250.) {
                    if (isFiducial.get(0)) {
                        aida.histogram1D("Single Fiducial Cluster Energy nRawTrackerHits<250.", 100, 0., 6.).fill(energies.get(0));
                        skipEvent = false;
                    } else {
                        if (!_fiducialOnly) {
                            skipEvent=false;
                        }
                    }
                }
            }
            aida.histogram1D("Max Cluster Energy", 100, 0., 6.).fill(maxEnergy);
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            if (_writeRunAndEventNumbers) {
                System.out.println(event.getRunNumber() + " " + event.getEventNumber());
            }
            _numberOfEventsWritten++;
        }
    }

    public void setWriteRunAndEventNumbers(boolean b) {
        _writeRunAndEventNumbers = b;
    }

    public void setFiducialOnly(boolean b) {
        _fiducialOnly = b;
    }

    @Override
    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

}
