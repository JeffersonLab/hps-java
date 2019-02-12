package org.hps.analysis.vertex;

import hep.physics.vec.Hep3Vector;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class UnconstrainedVcVerticesAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    @Override
    protected void process(EventHeader event) {
        List<Vertex> vertices = event.get(Vertex.class, "UnconstrainedVcVertices");
        for (Vertex v : vertices) {
            Hep3Vector pos = v.getPosition();
            ReconstructedParticle rp = v.getAssociatedParticle();
            double mass = rp.getMass();
            List<ReconstructedParticle> parts = rp.getParticles();
            ReconstructedParticle rp1 = parts.get(0); // electron
            ReconstructedParticle rp2 = parts.get(1); // positron
            Cluster c1 = rp1.getClusters().get(0);
            Cluster c2 = rp2.getClusters().get(0);
            double deltaT = ClusterUtilities.getSeedHitTime(c1) - ClusterUtilities.getSeedHitTime(c2);
            aida.histogram1D("cluster dT", 100, -2.5, 2.5).fill(deltaT);
            aida.histogram2D("VcVertex x vs y", 100, -10., 20., 100, -10., 10.).fill(pos.x(), pos.y());

            if (c1.getPosition()[1] > 0) {
                aida.histogram1D("VcVertex z top", 100, -100., 300.).fill(pos.z());
                aida.histogram1D("VcVertex z top", 100, -100., 300.).fill(pos.z());
                aida.histogram2D("VcVertex z vs x top", 100, -100., 300., 100, -20., 20.).fill(pos.z(), pos.x());
                aida.histogram1D("vertex mass top", 100, 0., 0.2).fill(mass);
                aida.histogram2D("vertex z vs mass top", 100, -100., 300., 100, 0., 0.2).fill(pos.z(), mass);
            } else {
                aida.histogram1D("VcVertex z bottom", 100, -100., 300.).fill(pos.z());
                aida.histogram1D("VcVertex z bottom", 100, -100., 300.).fill(pos.z());
                aida.histogram2D("VcVertex z vs x bottom", 100, -100., 300., 100, -20., 20.).fill(pos.z(), pos.x());
                aida.histogram1D("vertex mass bottom", 100, 0., 0.2).fill(mass);
                aida.histogram2D("vertex z vs mass bottom", 100, -100., 300., 100, 0., 0.2).fill(pos.z(), mass);
            }

            aida.histogram1D("electron momentum", 100, 0., 2.3).fill(rp1.getMomentum().magnitude());
            aida.histogram1D("positron momentum", 100, 0., 2.3).fill(rp2.getMomentum().magnitude());

            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles");
            if (rpList.size() == 3) {
                // most likely e- e- e+
                double eventE = 0.;
                for (ReconstructedParticle rp3 : rpList) {
                    eventE += rp3.getEnergy();
                }
                aida.histogram1D("event energy for 3 particle events", 100, 1.0, 3.0).fill(eventE);
                // should I cut here?
                if (c1.getPosition()[1] > 0) {
                    aida.histogram1D("Clean VcVertex z top", 100, -100., 300.).fill(pos.z());
                    aida.histogram1D("Clean VcVertex z top", 100, -100., 300.).fill(pos.z());
                    aida.histogram2D("Clean VcVertex z vs x top", 100, -100., 300., 100, -20., 20.).fill(pos.z(), pos.x());
                    aida.histogram1D("Clean VcVertex mass top", 100, 0., 0.2).fill(mass);
                    aida.histogram2D("Clean VcVertex z vs mass top", 100, -100., 300., 100, 0., 0.2).fill(pos.z(), mass);
                } else {
                    aida.histogram1D("Clean VcVertex z bottom", 100, -100., 300.).fill(pos.z());
                    aida.histogram1D("Clean VcVertex z bottom", 100, -100., 300.).fill(pos.z());
                    aida.histogram2D("Clean VcVertex z vs x bottom", 100, -100., 300., 100, -20., 20.).fill(pos.z(), pos.x());
                    aida.histogram1D("Clean VcVertex mass bottom", 100, 0., 0.2).fill(mass);
                    aida.histogram2D("Clean VcVertex z vs mass bottom", 100, -100., 300., 100, 0., 0.2).fill(pos.z(), mass);
                }

                aida.histogram1D("Clean VcVertex electron momentum", 100, 0., 2.3).fill(rp1.getMomentum().magnitude());
                aida.histogram1D("Clean VcVertex positron momentum", 100, 0., 2.3).fill(rp2.getMomentum().magnitude());
            }
        }
    }

}
