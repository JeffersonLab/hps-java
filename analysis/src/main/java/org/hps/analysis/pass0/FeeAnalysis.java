package org.hps.analysis.pass0;

import java.util.List;
import org.hps.record.triggerbank.TriggerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Analysis of some canonical FEE parameters
 * 
 * @author Norman A. Graf
 */
public class FeeAnalysis extends Driver {

    private AIDA _aida = AIDA.defaultInstance();

    protected void process(EventHeader event) {

        double feeEmin = 1.75;
        double feeEmax = 3.;
        if (event.getRunNumber() > 14673 || event.getRunNumber() < 14626) {
            feeEmin = 3.5;
            feeEmax = 7.0;
        }
        if (event.hasCollection(ReconstructedParticle.class, "FinalStateParticles_KF")) {
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, "FinalStateParticles_KF");
            // loop over all ReconstructedParticles in the event
            for (ReconstructedParticle rp : rpList) {
                //select electrons
                if (rp.getParticleIDUsed().getPDG() == 11) {
                    //require an associated cluster
                    if (!rp.getClusters().isEmpty()) {
                        //require that the energy be consistent with an FEE
                        double energy = rp.getEnergy();
                        if (energy > feeEmin) {
                            //require that the cluster be in the fiducial region of the ECal
                            if (TriggerModule.inFiducialRegion(rp.getClusters().get(0))) {
                                boolean isTop = rp.getClusters().get(0).getPosition()[1] > 0;
                                String topOrBottom = isTop ? " top " : " bottom ";
                                double momentum = rp.getMomentum().magnitude();
                                Track track = rp.getTracks().get(0);
                                int nHits = track.getTrackerHits().size();
                                // require a reasonable number of hits in the track
                                if (nHits > 12) {
                                    double chi2Ndf = track.getChi2() / track.getNDF();
                                    _aida.histogram1D("cluster energy" + topOrBottom, 100, 0., feeEmax).fill(energy);
                                    _aida.histogram1D("track momentum" + topOrBottom, 100, 0., feeEmax).fill(momentum);
                                    _aida.histogram1D("track chisquared per dof" + topOrBottom, 100, 0., 50.).fill(chi2Ndf);
                                    _aida.histogram1D("track momentum" + topOrBottom + " " + nHits + " hits", 100, 0., feeEmax).fill(momentum);
                                    _aida.histogram1D("track chisquared per dof" + topOrBottom + " " + nHits + " hits", 50, 0., 50.).fill(chi2Ndf);
                                    _aida.histogram1D("track number of hits" + topOrBottom, 14, 0.5, 14.5).fill(nHits);
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
