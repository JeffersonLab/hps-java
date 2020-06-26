package org.hps.analysis.examples;

import java.util.ArrayList;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class EcalClusterAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    public void process(EventHeader event) {
        if (event.hasCollection(Cluster.class, "EcalClustersCorr")) {
            List<Cluster> clusters = event.get(Cluster.class, "EcalClustersCorr");
            aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(clusters.size());
            double maxEnergy = 0;
            List<Double> times = new ArrayList<Double>();
            List<Double> energies = new ArrayList<Double>();
            List<Boolean> isFiducial = new ArrayList<Boolean>();
            List<Boolean> isTop = new ArrayList<Boolean>();
            for (Cluster cluster : clusters) {
                if (cluster.getEnergy() > maxEnergy) {
                    maxEnergy = cluster.getEnergy();
                }
                double[] cPos = cluster.getPosition();
                times.add(ClusterUtilities.findSeedHit(cluster).getTime());
                energies.add(cluster.getEnergy());
                isFiducial.add(TriggerModule.inFiducialRegion(cluster));
                isTop.add(cluster.getPosition()[1] > 0.);
                aida.histogram2D("cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(cPos[0], cPos[1]);
                String half = cluster.getPosition()[1] > 0. ? "Top ": "Bottom ";
                aida.histogram1D(half+"Cluster Energy", 100, 0., 6.).fill(cluster.getEnergy());
            }
            if (clusters.size() == 2) {
                double e0 = energies.get(0);
                double e1 = energies.get(1);
                double esum = e0 + e1;
                aida.histogram1D("Two-Cluster Energy sum", 100, 0., 6.).fill(energies.get(0) + energies.get(1));
                aida.histogram1D("Two-Cluster time difference", 100, -5, 5.).fill(times.get(0) - times.get(1));
                boolean isTopBottom = false;
                if (isTop.get(0) && !isTop.get(1)) {
                    isTopBottom = true;
                    aida.histogram1D("Two-Cluster Energy sum top-bottom", 100, 0., 6.).fill(energies.get(0) + energies.get(1));
                    aida.histogram1D("Two-Cluster time difference top-bottom", 100, -5, 5.).fill(times.get(0) - times.get(1));
                }
                if (!isTop.get(0) && isTop.get(1)) {
                    isTopBottom = true;
                    aida.histogram1D("Two-Cluster time difference top-bottom", 100, -5, 5.).fill(times.get(1) - times.get(0));
                    aida.histogram1D("Two-Cluster Energy sum top-bottom", 100, 0., 6.).fill(energies.get(0) + energies.get(1));

                }
                if (isTop.get(0) && isTop.get(1)) {
                    aida.histogram1D("Two-Cluster time difference top-top", 100, -5, 5.).fill(times.get(0) - times.get(1));
                }
                if (!isTop.get(0) && !isTop.get(1)) {
                    aida.histogram1D("Two-Cluster time difference bottom-bottom", 100, -5, 5.).fill(times.get(0) - times.get(1));
                }
                if (isFiducial.get(0) && isFiducial.get(1)) {
                    double dt = -9999.;
                    aida.histogram1D("Two-Cluster Energy sum both fiducial", 100, 0., 6.).fill(energies.get(0) + energies.get(1));
                    aida.histogram1D("Two-Cluster time difference both fiducial", 100, -5, 5.).fill(times.get(0) - times.get(1));
                    if (isTop.get(0) && !isTop.get(1)) {
                        aida.histogram1D("Two-Cluster Energy sum both fiducial top-bottom", 100, 0., 6.).fill(energies.get(0) + energies.get(1));
                        aida.histogram1D("Two-Cluster time difference both fiducial top-bottom", 100, -5, 5.).fill(times.get(0) - times.get(1));
                        dt = times.get(0) - times.get(1);
                    }
                    if (!isTop.get(0) && isTop.get(1)) {
                        aida.histogram1D("Two-Cluster time difference both fiducial top-bottom", 100, -5, 5.).fill(times.get(1) - times.get(0));
                        aida.histogram1D("Two-Cluster Energy sum both fiducial top-bottom", 100, 0., 6.).fill(energies.get(0) + energies.get(1));
                        dt = times.get(1) - times.get(0);
                    }
                    if (isTopBottom) {
                        aida.histogram2D("Two-Cluster both fiducial top-bottom E0 vs E1", 100, 0., 5., 100, 0., 5.).fill(energies.get(0), energies.get(1));
                        aida.histogram2D("Two-Cluster both fiducial top-bottom Esum vs E0", 100, 0., 5., 100, 0., 5.).fill(energies.get(0), energies.get(0) + energies.get(1));
                        if (e0 > 1.8 && e0 < 2.8) {
                            aida.histogram1D("Two-Cluster both fiducial Energy sum top-bottom 1.8<e0<2.8", 100, 0., 6.).fill((esum));
                            aida.histogram1D("Two-Cluster both fiducial time difference top-bottom 1.8<e0<2.8", 100, -5., 5.).fill((dt));
                            if (dt > 0. && dt < 1.) {
                                aida.histogram1D("Two-Cluster both fiducial Energy sum top-bottom 1.8<e0<2.8 0.<dt<1.", 100, 0., 6.).fill((esum));
                            }
                        }
                    }
                }
            }
            aida.histogram1D("Max Cluster Energy", 100, 0., 6.).fill(maxEnergy);
        }
    }
}
