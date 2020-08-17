package org.hps.analysis.pi0;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.List;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;
import org.lcsim.util.fourvec.Momentum4Vector;

/**
 *
 * @author ngraf
 */
public class Pi0AnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    public void process(EventHeader event) {
//        if (event.hasCollection(Cluster.class, "EcalClusters")) {
//            List<Cluster> clusters = event.get(Cluster.class, "EcalClusters");
//            int nclus = clusters.size();
//            aida.histogram1D("Number of Clusters in Event", 10, 0., 10.).fill(clusters.size());
////            System.out.println("nclus " + nclus);
//            if (nclus > 1) {
//                List<ReconstructedParticle> rps = new ArrayList<ReconstructedParticle>();
//                for (Cluster c : clusters) {
//                    // is this cluster in the fiducial region of the calorimeter?
//                    if (TriggerModule.inFiducialRegion(c)) {
//                        ReconstructedParticle rp = new BaseReconstructedParticle();
//                        // Add the cluster to the particle.
//                        rp.addCluster(c);
//                        double clusterEnergy = c.getEnergy();
//                        Hep3Vector momentum = new BasicHep3Vector(c.getPosition());
//                        momentum = VecOp.mult(clusterEnergy, VecOp.unit(momentum));
//                        HepLorentzVector fourVector = new BasicHepLorentzVector(clusterEnergy, momentum);
//                        ((BaseReconstructedParticle) rp).set4Vector(fourVector);
////                        System.out.println("c " + c);
////                        System.out.println("rp " + rp);
//                        // add the RP to the list
//                        rps.add(rp);
//                    }
//                }
//                if (rps.size() > 1) {
//                    int nrps = rps.size();
//                    ReconstructedParticle[] rpArray = rps.toArray(new ReconstructedParticle[0]);
////                    System.out.println("nrps " + nrps);
//                    for (int i = 0; i < nrps - 1; ++i) {
//                        for (int j = i + 1; j <= nrps - 1; ++j) {
////                            System.out.println("i " + i + " j " + j);
//                            Momentum4Vector vec = fourVector(rpArray[i]);
////                            System.out.println("vec[i] " + vec);
//                            vec.plusEquals(fourVector(rpArray[j]));
////                            System.out.println("vec[i+j] " + vec);
////                            System.out.println("mass " + vec.mass());
//                            aida.histogram1D("two-cluster mass", 500, 0., 1.).fill(vec.mass());
//                            aida.histogram1D("two-cluster mass fine", 200, 0.04, 0.28).fill(vec.mass());
//
//                        }
//                    }
//                }
//            }
//        }

        List<ReconstructedParticle> fspList = event.get(ReconstructedParticle.class, "FinalStateParticles");
        if (fspList.size() > 1) {
            List<ReconstructedParticle> photons = new ArrayList<>();
            for (ReconstructedParticle rp : fspList) {
                if (rp.getParticleIDUsed().getPDG() == 22) {
                    photons.add(rp);
                }
            }
            aida.histogram1D("number of photons in event", 10, 0., 10.).fill(photons.size());
            if (photons.size() > 1) {
                int nrps = photons.size();
                ReconstructedParticle[] rpArray = photons.toArray(new ReconstructedParticle[0]);
                for (int i = 0; i < nrps - 1; ++i) {
                    for (int j = i + 1; j <= nrps - 1; ++j) {
                        ReconstructedParticle rp1 = rpArray[i];
                        Cluster cl1 = rp1.getClusters().get(0);
                        ReconstructedParticle rp2 = rpArray[j];
                        Cluster cl2 = rp2.getClusters().get(0);
                        Momentum4Vector vec = fourVector(rp1);
                        vec.plusEquals(fourVector(rp2));
                        aida.histogram1D("two-photon mass", 500, 0., 1.).fill(vec.mass());
                        aida.histogram1D("two-photon mass fine", 200, 0.0, 0.5).fill(vec.mass());
                        if (TriggerModule.inFiducialRegion(cl1) && TriggerModule.inFiducialRegion(cl2)) {
                            aida.histogram1D("two fiducial photon fine", 200, 0.0, 0.5).fill(vec.mass());
                            double e1 = cl1.getEnergy();
                            Hep3Vector pos1 = new BasicHep3Vector(cl1.getPosition());
                            double e2 = cl2.getEnergy();
                            Hep3Vector pos2 = new BasicHep3Vector(cl2.getPosition());
                            double esum = e1 + e2;
                            //opposite hemispheres
                            if (pos1.x() * pos2.x() < 0. && pos1.y() * pos2.y() < 0.) {
                                aida.histogram1D("two fiducial photon mass opposite", 200, 0.0, 0.5).fill(vec.mass());
                                aida.histogram1D("two fiducial photon esum opposite", 100, 0.0, 5.0).fill(esum);
                            }
                        }
                    }
                }
            }
        }
    }

    private Momentum4Vector fourVector(ReconstructedParticle rp) {
        Hep3Vector p = rp.getMomentum();
        return new Momentum4Vector(p.x(), p.y(), p.z(), rp.getEnergy());
    }
}
