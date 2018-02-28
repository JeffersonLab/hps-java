package org.hps.recon.filtering;

import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.record.triggerbank.AbstractIntData;
import org.hps.record.triggerbank.TIData;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.GenericObject;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.util.aida.AIDA;

/**
 * Class to strip off Wide-Angle Bremsstrahlung (WAB) candidate events.
 *
 * @author Norman A. Graf
 */
public class WabCandidateFilter extends EventReconFilter {

    private String _reconParticleCollectionName = "FinalStateParticles";
    private double _clusterDeltaTimeCut = 2.0;
    private double _beamEnergy = 1.056;
    private double _beamEnergyCut = 0.15;
    private AIDA aida = AIDA.defaultInstance();

    @Override
    protected void detectorChanged(Detector detector) {
    }

    @Override
    protected void process(EventHeader event) {
        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
        }
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
            setupSensors(event);
            if (event.hasCollection(ReconstructedParticle.class, _reconParticleCollectionName)) {
                List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, _reconParticleCollectionName);
                // require two and only two Reconstructed particles (except for old passes we matched both
                // MatchedTracks and GBL Tracks.
                if (rpList.size() == 3) {
                    ReconstructedParticle electron = null;
                    ReconstructedParticle photon = null;

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
                        double eSum = electron.getEnergy() + photon.getEnergy();//electron.getClusters().get(0).getEnergy() + photon.getClusters().get(0).getEnergy();
                        if (eSum > (1. - _beamEnergyCut) * _beamEnergy && eSum < (1. + _beamEnergyCut) * _beamEnergy) {
                            // calorimeter cluster timing cut
                            // first CalorimeterHit in the list is the seed crystal
                            Cluster eClus = electron.getClusters().get(0);
                            Cluster pClus = photon.getClusters().get(0);

                            double eClusTime = ClusterUtilities.getSeedHitTime(eClus);
                            double eClusE = eClus.getEnergy();

                            double pClusTime = ClusterUtilities.getSeedHitTime(pClus);
                            double pClusE = pClus.getEnergy();
                            double dt = eClusTime - pClusTime;
                            if (abs(dt) < _clusterDeltaTimeCut) {
                                goodEvent = true;

                                // make some plots...
                                aida.histogram1D("esum", 100, 0., 1.5).fill(eSum);
                                aida.histogram1D("track momentum", 100, 0., 1.5).fill(electron.getMomentum().magnitude());
                                aida.histogram1D("cluster dT", 100, -3., 3.).fill(dt);
                                aida.histogram1D("e clus", 100, 0., 1.2).fill(eClusE);
                                aida.histogram1D("p clus", 100, 0., 1.2).fill(pClusE);
                                aida.histogram2D("e clus vs p clus", 50, 0., 1.2, 50, 0., 1.2).fill(eClusE, pClusE);
                                Track t = electron.getTracks().get(0);
                                if (isTopTrack(t)) {
                                    aida.histogram1D("top track momentum", 100, 0., 1.5).fill(electron.getMomentum().magnitude());
                                } else {
                                    aida.histogram1D("bottom track momentum", 100, 0., 1.5).fill(electron.getMomentum().magnitude());
                                }
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

    private boolean isTopTrack(Track t) {
        List<TrackerHit> hits = t.getTrackerHits();
        int n[] = {0, 0};
        int nHits = hits.size();
        for (TrackerHit h : hits) {
            HpsSiSensor sensor = ((HpsSiSensor) ((RawTrackerHit) h.getRawHits().get(0)).getDetectorElement());
            if (sensor.isTopLayer()) {
                n[0] += 1;
            } else {
                n[1] += 1;
            }
        }
        if (n[0] == nHits && n[1] == 0) {
            return true;
        }
        if (n[1] == nHits && n[0] == 0) {
            return false;
        }
        throw new RuntimeException("mixed top and bottom hits on same track");

    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        EventHeader.LCMetaData meta = event.getMetaData(rawTrackerHits);
        // Get the ID dictionary and field information.
        IIdentifierDictionary dict = meta.getIDDecoder().getSubdetector().getDetectorElement().getIdentifierHelper().getIdentifierDictionary();
        int fieldIdx = dict.getFieldIndex("side");
        int sideIdx = dict.getFieldIndex("strip");
        for (RawTrackerHit hit : rawTrackerHits) {
            // The "side" and "strip" fields needs to be stripped from the ID for sensor lookup.
            IExpandedIdentifier expId = dict.unpack(hit.getIdentifier());
            expId.setValue(fieldIdx, 0);
            expId.setValue(sideIdx, 0);
            IIdentifier strippedId = dict.pack(expId);
            // Find the sensor DetectorElement.
            List<IDetectorElement> des = DetectorElementStore.getInstance().find(strippedId);
            if (des == null || des.size() == 0) {
                throw new RuntimeException("Failed to find any DetectorElements with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            } else if (des.size() == 1) {
                hit.setDetectorElement((SiSensor) des.get(0));
            } else {
                // Use first sensor found, which should work unless there are sensors with duplicate IDs.
                for (IDetectorElement de : des) {
                    if (de instanceof SiSensor) {
                        hit.setDetectorElement((SiSensor) de);
                        break;
                    }
                }
            }
            // No sensor was found.
            if (hit.getDetectorElement() == null) {
                throw new RuntimeException("No sensor was found for hit with stripped ID <0x" + Long.toHexString(strippedId.getValue()) + ">.");
            }
        }
    }
}
