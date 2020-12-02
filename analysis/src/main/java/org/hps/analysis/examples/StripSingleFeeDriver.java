package org.hps.analysis.examples;

import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.CalorimeterHit;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Simple Driver to select events containing a clean Full Energy Electron (FEE).
 * This is defined as one ReconstructedParticle which has been identified as an
 * electron and has an energy equal to or greater than the energyCut (default is
 * 3.0) and a track which has greater than or equal the numberOfHitsOnTrack
 * (default is 5). By default, only one ReconstructedParticle is allowed per
 * event.
 *
 * @author Norman A Graf
 *
 * @todo Move to recon.filtering
 */
public class StripSingleFeeDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    private boolean _writeRunAndEventNumbers = false;
    private double _energyCut = 3.0;
    private int _nHitsOnTrack = 5;
    private int _nReconstructedParticles = 1;

    private int _numberOfEventsWritten = 0;

    private String _holeOrSlot = "";
    private String _topOrBottom = "";

    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
    }

    /**
     *
     * @param event
     */
    @Override
    protected void process(EventHeader event) {
        boolean skipEvent = true;
        // get the ReconstructedParticles in this event
        List<ReconstructedParticle> rps = event.get(ReconstructedParticle.class, "FinalStateParticles");
        // now add in the FEE candidates
        rps.addAll(event.get(ReconstructedParticle.class, "OtherElectrons"));
        ReconstructedParticle fee = null;
        if (rps.size() <= _nReconstructedParticles) {
            for (ReconstructedParticle rp : rps) {
                double energy = rp.getEnergy();
                int pdgId = rp.getParticleIDUsed().getPDG();
                if (pdgId == 11) {
                    //require a cluster associated with the track
                    if (!rp.getClusters().isEmpty()) {
                        if (energy >= _energyCut && rp.getTracks().get(0).getTrackerHits().size() >= _nHitsOnTrack) // electron
                        {
                            double[] cpos = rp.getClusters().get(0).getPosition();
                            boolean isTop = cpos[1] > 0 ? true : false;
                            switch (_topOrBottom) {
                                case "top":
                                    if (isTop) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "Top":
                                    if (isTop) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "bottom":
                                    if (!isTop) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "Bottom":
                                    if (!isTop) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "":
                                    skipEvent = false;
                                    fee = rp;
                                    break;
                            }
                            // now check for hole (electron) or slot (positron)
                            // use cluster position as rough proxy for hole/slot
                            // TODO should check on sensor name for hits in layers 5-7
                            switch (_holeOrSlot) {
                                case "hole":
                                    if (cpos[0] < 0) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "Hole":
                                    if (cpos[0] < 0) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "slot":
                                    if (cpos[0] > 0) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "Slot":
                                    if (cpos[0] > 0) {
                                        skipEvent = false;
                                        fee = rp;
                                    }
                                    break;
                                case "":
                                    skipEvent = false;
                                    fee = rp;
                                    break;
                            } // check on hole/slot
                        } // check on top/bottom
                    }//end of check on cluster
                } // end of check  on electron
            } //loop over RPs
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            setupSensors(event);
            analyzeReconstructedParticle(fee);
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

    public void setSelectHoleOrSlot(String s) {
        _holeOrSlot = s;
    }

    public void setSelectTopOrBottom(String s) {
        _topOrBottom = s;
    }

    @Override
    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events");
    }

    void analyzeReconstructedParticle(ReconstructedParticle rp) {
        boolean isElectron = rp.getParticleIDUsed().getPDG() == 11;
        boolean isPositron = rp.getParticleIDUsed().getPDG() == -11;
        boolean isPhoton = rp.getParticleIDUsed().getPDG() == 22;
        String type = "";
        if (isElectron) {
            type = "electron";
        }
        if (isPositron) {
            type = "positron";
        }
        if (isPhoton) {
            type = "photon";
        }

        aida.tree().mkdirs(type);
        aida.tree().cd(type);

        if (isElectron || isPositron) {
            analyzeTrack(rp);
        }

        if (isPhoton) {
            analyzeCluster(rp);
        }

        aida.tree().cd("..");

    }

    void analyzeCluster(Cluster c) {
        aida.histogram2D("Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
        if (c.getPosition()[1] > 0.) {
            aida.histogram1D("Top cluster energy", 100, 3.5, 5.5).fill(c.getEnergy());
        } else {
            aida.histogram1D("Bottom cluster energy", 100, 3.5, 5.5).fill(c.getEnergy());
        }
    }

    void analyzeCluster(ReconstructedParticle rp) {
        Cluster c = rp.getClusters().get(0);
        double p = rp.getMomentum().magnitude();
        double e = rp.getEnergy();

        CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
        double seedHitEnergy = ClusterUtilities.findSeedHit(c).getCorrectedEnergy();
        boolean isFiducial = isFiducial(seedHit);
        String fid = isFiducial ? "fiducial" : "";
        // debug diagnostics to set cuts

        if (c.getPosition()[1] > 0.) {
            aida.histogram1D("Top cluster energy", 100, 3.5, 5.5).fill(c.getEnergy());
        } else {
            aida.histogram1D("Bottom cluster energy", 100, 3.5, 5.5).fill(c.getEnergy());
        }
        aida.histogram2D("Cluster x vs y", 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
        aida.histogram1D("clusterSeedHit energy", 50, 0.5, 4.5).fill(seedHitEnergy);
        aida.histogram1D("cluster nHits", 20, 0., 20.).fill(c.getCalorimeterHits().size());
        aida.histogram2D("clusterSeedHit energy vs energy", 100, 3.5, 5.5, 50, 0.5, 4.5).fill(e, seedHitEnergy);
        aida.histogram2D("cluster nHits vs energy", 100, 3.5, 5.5, 20, 0., 20.).fill(e, c.getCalorimeterHits().size());
        aida.histogram2D("cluster time vs e", 100, 3.5, 5.5, 30, 30., 60.).fill(p, ClusterUtilities.getSeedHitTime(c));
        if (isFiducial) {
            if (c.getPosition()[1] > 0.) {
                aida.histogram1D("Top cluster energy " + fid, 100, 3.5, 5.5).fill(c.getEnergy());
            } else {
                aida.histogram1D("Bottom cluster energy " + fid, 100, 3.5, 5.5).fill(c.getEnergy());
            }
            aida.histogram2D("Cluster x vs y " + fid, 320, -270.0, 370.0, 90, -90.0, 90.0).fill(c.getPosition()[0], c.getPosition()[1]);
            aida.histogram1D("clusterSeedHit energy " + fid, 50, 0.5, 4.5).fill(seedHitEnergy);
            aida.histogram1D("cluster nHits " + fid, 20, 0., 20.).fill(c.getCalorimeterHits().size());
            aida.histogram2D("clusterSeedHit energy vs energy " + fid, 100, 3.5, 5.5, 50, 0.5, 4.5).fill(e, seedHitEnergy);
            aida.histogram2D("cluster nHits vs energy " + fid, 100, 3.5, 5.5, 20, 0., 20.).fill(e, c.getCalorimeterHits().size());
            aida.histogram2D("cluster time vs e " + fid, 100, 3.5, 5.5, 30, 30., 60.).fill(p, ClusterUtilities.getSeedHitTime(c));

            if (seedHitEnergy > 2.8) {
                if (c.getPosition()[1] > 0.) {
                    aida.histogram1D("Top cluster energy 2.8", 100, 3.5, 5.5).fill(c.getEnergy());
                } else {
                    aida.histogram1D("Bottom cluster energy 2.8", 100, 3.5, 5.5).fill(c.getEnergy());
                }
                if (seedHitEnergy > 3.0) {
                    if (c.getPosition()[1] > 0.) {
                        aida.histogram1D("Top cluster energy 3.0", 100, 3.5, 5.5).fill(c.getEnergy());
                    } else {
                        aida.histogram1D("Bottom cluster energy 3.0", 100, 3.5, 5.5).fill(c.getEnergy());
                    }
                }
            }
        }
    }

    void analyzeTrack(ReconstructedParticle rp) {
        boolean isGBL = TrackType.isGBL(rp.getType());
        String trackDir = isGBL ? "gbl" : "htf";
        if (rp.getType() == 1) {
            trackDir = "kf";
        }
        aida.tree().mkdirs(trackDir);
        aida.tree().cd(trackDir);

        Track t = rp.getTracks().get(0);

//        aida.cloud1D("ReconstructedParticle Type").fill(rp.getType());
//        aida.cloud1D("Track Type").fill(t.getType());
        //rotate into physiscs frame of reference
        Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        double theta = Math.acos(rprot.z() / rprot.magnitude());
        double chiSquared = t.getChi2();
        int ndf = t.getNDF();
        double chi2Ndf = t.getChi2() / t.getNDF();
        double chisqProb = 1.;
        if (ndf != 0) {
            chisqProb = ChisqProb.gammp(ndf, chiSquared);
        }
        int nHits = t.getTrackerHits().size();
        double dEdx = t.getdEdx();
        double e = rp.getEnergy();
        double p = rp.getMomentum().magnitude();

        String topOrBottom = isTopTrack(t) ? " top " : " bottom ";
        aida.histogram1D("Track chisq per df" + topOrBottom, 100, 0., 50.).fill(chiSquared / ndf);
        aida.histogram1D("Track chisq prob" + topOrBottom, 100, 0., 1.).fill(chisqProb);
        aida.histogram1D("Track nHits" + topOrBottom, 7, 0.5, 7.5).fill(t.getTrackerHits().size());
        aida.histogram1D("Track momentum" + topOrBottom, 100, 0., 10.0).fill(p);
        aida.histogram1D("Track deDx" + topOrBottom, 100, 0.00004, 0.00013).fill(t.getdEdx());
        aida.histogram1D("Track theta" + topOrBottom, 100, 0.010, 0.160).fill(theta);
        aida.histogram2D("Track theta vs p" + topOrBottom, 100, 0.010, 0.160, 100, 0., 10.0).fill(theta, p);
        aida.histogram1D("rp x0" + topOrBottom, 100, -0.50, 0.50).fill(TrackUtils.getX0(t));
        aida.histogram1D("rp y0" + topOrBottom, 100, -5.0, 5.0).fill(TrackUtils.getY0(t));
        aida.histogram1D("rp z0" + topOrBottom, 100, -1.0, 1.0).fill(TrackUtils.getZ0(t));

        //
        aida.histogram1D("Track chisq per df" + topOrBottom + " " + nHits + " hits", 100, 0., 50.).fill(chiSquared / ndf);
        aida.histogram1D("Track chisq prob" + topOrBottom + " " + nHits + " hits", 100, 0., 1.).fill(chisqProb);
        aida.histogram1D("Track nHits" + topOrBottom + " " + nHits + " hits", 7, 0.5, 7.5).fill(t.getTrackerHits().size());
        aida.histogram1D("Track momentum" + topOrBottom + " " + nHits + " hits", 100, 0., 10.0).fill(p);
        aida.histogram1D("Track deDx" + topOrBottom + " " + nHits + " hits", 100, 0.00004, 0.00013).fill(t.getdEdx());
        aida.histogram1D("Track theta" + topOrBottom + " " + nHits + " hits", 100, 0.010, 0.160).fill(theta);
        aida.histogram2D("Track theta vs p" + topOrBottom + " " + nHits + " hits", 100, 0.010, 0.160, 100, 0., 10.0).fill(theta, p);
        aida.histogram1D("rp x0" + topOrBottom + " " + nHits + " hits", 100, -0.50, 0.50).fill(TrackUtils.getX0(t));
        aida.histogram1D("rp y0" + topOrBottom + " " + nHits + " hits", 100, -5.0, 5.0).fill(TrackUtils.getY0(t));
        aida.histogram1D("rp z0" + topOrBottom + " " + nHits + " hits", 100, -1.0, 1.0).fill(TrackUtils.getZ0(t));

        boolean hasCluster = rp.getClusters().size() == 1;
        if (hasCluster) {
            analyzeCluster(rp);
        }
        //
        aida.tree().cd("..");
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
