package org.hps.analysis.pass0;

import java.util.List;
import org.hps.recon.tracking.TrackType;
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
    String[] _finalStateParticleCollectionNames = {"FinalStateParticles", "FinalStateParticles_KF"};
    private int _minTrackNhits_GBL = 5;
    private int _minTrackNhits_KF = 10;

    @Override
    protected void process(EventHeader event) {

        int runNumber = event.getRunNumber();
        double feeEmin = 1.75;
        double feeEmax = 3.;

        // 2019 Run 4.55GeV
        String runPeriod = "unknown";
        if (runNumber > 10000 && runNumber < 10750) {
            feeEmin = 4.0;
            feeEmax = 7.0;
            runPeriod = "2019 4.55Gev";
        }
        // 2021 Run 3.74GeV
        if (runNumber > 14000 && runNumber < 15000) {
            feeEmin = 3.5;
            feeEmax = 7.0;
            runPeriod = "2021 3.74Gev";
            // 2021 Run 1.92GeV
            if (runNumber > 14623 && runNumber < 14680) {
                feeEmin = 1.75;
                feeEmax = 3.;
                runPeriod = "2021 1.92Gev";
            }
        }

        _aida.tree().mkdirs(runPeriod);
        _aida.tree().cd(runPeriod);
        for (String s : _finalStateParticleCollectionNames) {
            if (event.hasCollection(ReconstructedParticle.class, s)) {
                List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, s);
                String dir = s + " ReconstructedParticle Analysis";
                _aida.tree().mkdirs(dir);
                _aida.tree().cd(dir);
                // loop over all ReconstructedParticles in the event
                for (ReconstructedParticle rp : rpList) {
                    //select electrons
                    //TODO figure out why this is sometimes null
                    if (rp.getParticleIDUsed() != null) {
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
                                        boolean isGBL = TrackType.isGBL(rp.getType());
                                        int minNhits = isGBL ? _minTrackNhits_GBL : _minTrackNhits_KF;
                                        String trackType = isGBL ? "GBL" : "KF";
                                        // require a reasonable number of hits in the track
                                        if (nHits >= minNhits) {
                                            double chi2Ndf = track.getChi2() / track.getNDF();
                                            for (int i = 0; i < 2; ++i) {
                                                String postFix = i == 0 ? (" " + trackType) : (" " + trackType + " " + runNumber);
                                                _aida.histogram1D("cluster energy" + topOrBottom + postFix, 100, 0., feeEmax).fill(energy);
                                                _aida.histogram1D("track momentum" + topOrBottom + postFix, 100, 0., feeEmax).fill(momentum);
                                                _aida.histogram1D("track chisquared per dof " + topOrBottom + postFix, 100, 0., 50.).fill(chi2Ndf);
                                                _aida.histogram1D("track momentum" + topOrBottom + " " + nHits + " hits " + postFix, 100, 0., feeEmax).fill(momentum);
                                                _aida.histogram1D("track chisquared per dof" + topOrBottom + " " + nHits + " hits " + postFix, 50, 0., 50.).fill(chi2Ndf);
                                                _aida.histogram1D("track number of hits " + topOrBottom + postFix, 14, 0.5, 14.5).fill(nHits);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                _aida.tree().cd("..");
            }
        }
        _aida.tree().cd("..");
    }

    public void setMinTrackNhits_GBL(int i) {
        _minTrackNhits_GBL = i;
    }

    public void setMinTrackNhits_KF(int i) {
        _minTrackNhits_KF = i;
    }
}
