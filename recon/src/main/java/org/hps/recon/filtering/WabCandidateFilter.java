package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.ReconstructedParticle;

/**
 * Class to strip off Wide-Angle Bremsstrahlung (WAB) candidate events.
 *
 * @author Norman A. Graf
 */
public class WabCandidateFilter extends EventReconFilter {

    private String _reconParticleCollectionName = "FinalStateParticles";
    private double _clusterDeltaTimeCut = 2.0;

    @Override
    protected void process(EventHeader event) {
        incrementEventProcessed();
        // require pairs1 trigger
        boolean passedTrigger = false;
        boolean goodEvent = false;
        for (GenericObject gob : event.get(GenericObject.class, "TriggerBank")) {
            if (!(AbstractIntData.getTag(gob) == TIData.BANK_TAG)) {
                continue;
            }
            TIData tid = new TIData(gob);
            if (tid.isPair1Trigger()) {
                passedTrigger = true;
            }
        }
        if (passedTrigger) {
            if (event.hasCollection(ReconstructedParticle.class, _reconParticleCollectionName)) {
                List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, _reconParticleCollectionName);
                // require two and only two Reconstructed particles.
                if (rpList.size() == 3) {
                    ReconstructedParticle electron = null;
                    ReconstructedParticle photon = null;
                    double beamEnergy = getBeamEnergy();
                    int nElectrons = 0;
                    int nPhotons = 0;

                    for (ReconstructedParticle rp : rpList) {
                        if (rp.getParticleIDUsed().getPDG() == 11) {
                            // we seem to still be creating ReconstructedParticles with non-GBL tracks...
                            if (TrackType.isGBL(rp.getType())) {
                                // require electron to have an associated cluster
                                if (rp.getClusters().size() == 1) {
                                    electron = rp;
                                    nElectrons++;
                                }
                            }
                        }
                        if (rp.getParticleIDUsed().getPDG() == 22) {
                            photon = rp;
                            nPhotons++;
                        }
                    }
                    //one electron and one photon
                    int npart = 0;
                    if (nElectrons == 1 && nPhotons == 1) {
                        // require energy sum to be approximately the beam energy
                        double eSum = electron.getClusters().get(0).getEnergy() + photon.getClusters().get(0).getEnergy();
                        if (eSum > .85 * beamEnergy && eSum < 1.15 * beamEnergy) {
                            // calorimeter cluster timing cut
                            // first CalorimeterHit in the list is the seed crystal
                            double t1 = electron.getClusters().get(0).getCalorimeterHits().get(0).getTime();
                            double t2 = photon.getClusters().get(0).getCalorimeterHits().get(0).getTime();
                            if (abs(t1 - t2) < _clusterDeltaTimeCut) {
                                goodEvent = true;
                            }
                        }
                    }
                }
            }
        }
        if (!goodEvent) {
            skipEvent();
        }
        incrementEventPassed();
    }
}
