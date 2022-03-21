package org.hps.analysis.alignment;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman A. Graf
 */
public class TrackAnalysis14Hit2019 extends Driver {

    private AIDA aida = AIDA.defaultInstance();

    public void process(EventHeader event) {
        if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles_KF")) {
            List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles_KF");
            for (ReconstructedParticle rp : rps) {
                int pdgId = rp.getParticleIDUsed().getPDG();
                // only consider charge particles
                if (abs(pdgId) == 11) {
                    // only consider tracks with all sensors hit
                    if (rp.getTracks().get(0).getTrackerHits().size() == 14) {
                        //only consider particles with associated cluster
                        if (!rp.getClusters().isEmpty()) {
                            String type = pdgId == 11 ? " electron" : " positron";
                            Cluster c = rp.getClusters().get(0);
                            Track t = rp.getTracks().get(0);
                            double p = rp.getMomentum().magnitude();
                            double e = rp.getEnergy();
                            if (e > 0.5) {
                                aida.tree().mkdirs(type + " Analysis");
                                aida.tree().cd(type + " Analysis");
                                CalorimeterHit seed = c.getCalorimeterHits().get(0);
                                int ix = seed.getIdentifierFieldValue("ix");
                                int iy = seed.getIdentifierFieldValue("iy");
                                String fid = TriggerModule.inFiducialRegion(c) ? " fiducial " : " non fid ";
                                String topOrBottom = iy > 0 ? " top " : " bottom ";
                                aida.histogram2D("cluster ix vs iy" + type, 47, -23.5, 23.5, 11, -5.5, 5.5).fill(ix, iy);
                                aida.histogram1D("EoverP ix: " + ix + " iy: " + iy + type, 100, 0., 2.0).fill(e / p);
                                aida.histogram1D("Track momentum" + topOrBottom + type, 200, 0., 7.0).fill(p);
                                aida.histogram2D("E vs p" + topOrBottom + type, 100, 0., 7., 100, 0., 7.).fill(e, p);
                                aida.histogram2D("E vs p" + topOrBottom + fid + type, 100, 0., 7., 100, 0., 7.).fill(e, p);
                                aida.histogram1D("EoverP" + topOrBottom + fid + type, 100, 0., 2.0).fill(e / p);
                                aida.tree().cd("..");
                            }
                        }
                    }
                }
            }
        }
    }
}
