package org.hps.analysis.wab;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
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
public class StripWABCandidates extends Driver {

    private boolean _writeRunAndEventNumbers = false;
    private boolean _stripBothFiducial = false;
    private boolean _onlyPhotonFiducial = false;
    private double _energyCut = 0.85;
    private int _nHitsOnTrack = 5;
    private int _nReconstructedParticles = 2;
    private AIDA aida = AIDA.defaultInstance();

    private int _numberOfEventsWritten = 0;

    protected void process(EventHeader event) {
        boolean skipEvent = true;
        // get the ReconstructedParticles in this event
        List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
        // now add in the FEE candidates
        rps.addAll(event.get(ReconstructedParticle.class, "OtherElectrons"));

        // get the electron and photon
        // for now start with only 2 ReconstructedParticles in the event...
        if (rps.size() <= _nReconstructedParticles) {
            ReconstructedParticle electron = null;
            ReconstructedParticle photon = null;
            for (ReconstructedParticle rp : rps) {
                // require the electron to have an associated ECal cluster
                if (rp.getParticleIDUsed().getPDG() == 11 && rp.getClusters().size() == 1) {
                    electron = rp;
                }
                if (rp.getParticleIDUsed().getPDG() == 22) {
                    photon = rp;
                }
            }
            // do we have one (and only one) of each?
            if (electron != null && photon != null) {
                double eEnergy = electron.getEnergy();
                double eMomentum = electron.getMomentum().magnitude();
                Cluster eClus = electron.getClusters().get(0);
                boolean electronIsFiducial = isFiducial(ClusterUtilities.findSeedHit(eClus));
                double eTime = ClusterUtilities.getSeedHitTime(eClus);
                Track t = electron.getTracks().get(0);
                int nHits = t.getTrackerHits().size();
                double pEnergy = photon.getEnergy();
                Cluster pClus = photon.getClusters().get(0);
                boolean photonIsFiducial = isFiducial(ClusterUtilities.findSeedHit(pClus));
                double pTime = ClusterUtilities.getSeedHitTime(pClus);
                double eSum = eEnergy + pEnergy;
                aida.histogram1D("Electron + Photon cluster Esum", 100, 0., 3.0).fill(eSum);
                aida.histogram1D("Electron momentum + photon Energy", 100, 0., 3.0).fill(eMomentum + pEnergy);
                aida.histogram1D("Electron energy", 100, 0., 3.0).fill(eEnergy);
                aida.histogram1D("Electron momentum", 100, 0., 3.0).fill(eMomentum);
                aida.histogram1D("Photon Energy", 100, 0., 3.0).fill(pEnergy);
                aida.histogram2D("Electron energy vs Photon energy", 100, 0., 3.0, 100, 0., 3.0).fill(eEnergy, pEnergy);
                aida.histogram2D("Electron momentum vs Photon energy", 100, 0., 3.0, 100, 0., 3.0).fill(eMomentum, pEnergy);
                aida.histogram2D("Electron Cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(eClus.getPosition()[0], eClus.getPosition()[1]);
                aida.histogram2D("Photon Cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(pClus.getPosition()[0], pClus.getPosition()[1]);
                aida.histogram1D("Cluster delta time", 100, -5., 5.).fill(eTime - pTime);
                aida.histogram2D("Electron Cluster y vs Photon Cluster y", 100, -100., 100., 100, -100., 100.).fill(eClus.getPosition()[1], pClus.getPosition()[1]);
                if (eSum >= _energyCut && electron.getTracks().get(0).getTrackerHits().size() >= _nHitsOnTrack) // electron
                {
                    if (abs(eTime - pTime) < 2.) {
                        if (eClus.getPosition()[1] * pClus.getPosition()[1] < 0.) {
                            aida.histogram1D("Final " + nHits + " hits Electron momentum + photon Energy", 100, 0., 3.0).fill(eMomentum + pEnergy);
                            // Passed all cuts, let's write this event
                            skipEvent = false;
                            //Are we also requiring both clusters to be fiducial?
                            if (_stripBothFiducial) {
                                if (!electronIsFiducial || !photonIsFiducial) {
                                    skipEvent = true;
                                }
                            }
                            // Are we only requiring the photon to be fiducial?
                            if (_onlyPhotonFiducial) {
                                if (!photonIsFiducial) {
                                    skipEvent = true;
                                }
                            }
                            if (electronIsFiducial && photonIsFiducial) {
                                aida.histogram1D("Fiducial " + nHits + " hits Electron momentum + Fiducial photon Energy", 100, 0., 3.0).fill(eMomentum + pEnergy);
                            }
                            if (photonIsFiducial) {
                                aida.histogram1D("Final " + nHits + " hits Electron momentum + Fiducial photon Energy", 100, 0., 3.0).fill(eMomentum + pEnergy);
                            }
                        }
                    }

                }
            }
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

    /**
     * Electrons having energy below the cut will be rejected.
     *
     * @param cut
     */
    public void setEnergyCut(double cut) {
        _energyCut = cut;
    }

    /**
     * Tracks having fewer than the number of hits will be rejected.
     *
     * @param cut
     */
    public void setNumberOfHitsOnTrack(int cut) {
        _nHitsOnTrack = cut;
    }

    /**
     * Events having more than the number of ReconstructedParticles will be
     * rejected.
     *
     * @param cut
     */
    public void setNumberOfReconstructedParticles(int cut) {
        _nReconstructedParticles = cut;
    }

    /**
     * Write out run and event numbers of events passing the cuts if desired
     *
     * @param b
     */
    public void setWriteRunAndEventNumbers(boolean b) {
        _writeRunAndEventNumbers = b;
    }

    public void setStripBothFiducial(boolean b) {
        _stripBothFiducial = b;
    }

    public void setOnlyPhotonFiducial(boolean b) {
        _onlyPhotonFiducial = b;
    }

    public boolean isFiducial(CalorimeterHit hit) {
        int ix = hit.getIdentifierFieldValue("ix");
        int iy = hit.getIdentifierFieldValue("iy");
        // Get the x and y indices for the cluster.
        int absx = Math.abs(ix);
        int absy = Math.abs(iy);

        // Check if the cluster is on the top or the bottom of the
        // calorimeter, as defined by |y| == 5. This is an edge cluster
        // and is not in the fiducial region.
        if (absy == 5) {
            return false;
        }

        // Check if the cluster is on the extreme left or right side
        // of the calorimeter, as defined by |x| == 23. This is also
        // an edge cluster and is not in the fiducial region.
        if (absx == 23) {
            return false;
        }

        // Check if the cluster is along the beam gap, as defined by
        // |y| == 1. This is an internal edge cluster and is not in the
        // fiducial region.
        if (absy == 1) {
            return false;
        }

        // Lastly, check if the cluster falls along the beam hole, as
        // defined by clusters with -11 <= x <= -1 and |y| == 2. This
        // is not the fiducial region.
        if (absy == 2 && ix <= -1 && ix >= -11) {
            return false;
        }

        // If all checks fail, the cluster is in the fiducial region.
        return true;
    }

    @Override
    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

}
