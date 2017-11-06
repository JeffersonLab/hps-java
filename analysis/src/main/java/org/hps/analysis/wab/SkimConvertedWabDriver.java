package org.hps.analysis.wab;

import hep.aida.IHistogram1D;
import hep.aida.IHistogram2D;
import static java.lang.Math.abs;
import java.util.List;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackType;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.Vertex;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * Looking for converted photons in our data stream. Loop over V0 candidates to
 * see if we have any good e+e- candidates Then optionally apply some event
 * cleanup
 *
 * @author Norman A. Graf
 */
public class SkimConvertedWabDriver extends Driver {

    private int _numberOfEventsWritten = 0;
    private int _numberOfEventsRead = 0;

    private double _beamEnergy = 1.056;
    private double _percentFeeCut = 0.8;

    private int _nReconstructedParticles = 3;

    private boolean _writeRunAndEventNumber = false;

    private AIDA aida = AIDA.defaultInstance();

    private IHistogram1D invMassHist_UnconstrainedV0Vertices = aida.histogram1D("V0 Invariant Mass", 2100, 0., 0.3);
    private IHistogram1D esumHist_UnconstrainedV0Vertices = aida.histogram1D("V0 event esum", 200, 0., 4.0);
    private IHistogram1D zposHist_UnconstrainedV0Vertices = aida.histogram1D("V0 vertex z", 250, -50., 200.0);
    private IHistogram1D zposHist_UnconstrainedV0Vertices_top = aida.histogram1D("V0 vertex z top", 250, -50., 200.0);
    private IHistogram1D zposHist_UnconstrainedV0Vertices_bottom = aida.histogram1D("V0 vertex z bottom", 250, -50., 200.0);

    private IHistogram1D zposHist_6hitUnconstrainedV0Vertices = aida.histogram1D("V0 vertex z 66hit", 250, -50., 200.0);
    private IHistogram1D zposHist_6hitUnconstrainedV0Vertices_top = aida.histogram1D("V0 vertex z top 66hit", 250, -50., 200.0);
    private IHistogram1D zposHist_6hitUnconstrainedV0Vertices_bottom = aida.histogram1D("V0 vertex z bottom 66hit", 250, -50., 200.0);

    private IHistogram1D zposHist_5hitUnconstrainedV0Vertices = aida.histogram1D("V0 vertex z 55hit", 250, -50., 200.0);
    private IHistogram1D zposHist_5hitUnconstrainedV0Vertices_top = aida.histogram1D("V0 vertex z top 55hit", 250, -50., 200.0);
    private IHistogram1D zposHist_5hitUnconstrainedV0Vertices_bottom = aida.histogram1D("V0 vertex z bottom 55hit", 250, -50., 200.0);

    private IHistogram1D zposHist_56hitUnconstrainedV0Vertices = aida.histogram1D("V0 vertex z 56hit", 250, -50., 200.0);
    private IHistogram1D zposHist_56hitUnconstrainedV0Vertices_top = aida.histogram1D("V0 vertex z top 56hit", 250, -50., 200.0);
    private IHistogram1D zposHist_56hitUnconstrainedV0Vertices_bottom = aida.histogram1D("V0 vertex z bottom 56hit", 250, -50., 200.0);

    private IHistogram2D psumVsMassHist_UnconstrainedV0Vertices = aida.histogram2D("V0 vertex psum vs mass", 100, 0.8, 1.8, 100, 0., 0.3);
    private IHistogram2D zposVsMassHist_UnconstrainedV0Vertices = aida.histogram2D("V0 vertex zpos vs mass", 250, -50., 200., 100, 0., 0.3);
    private IHistogram2D xVsYHist_UnconstrainedV0Vertices = aida.histogram2D("V0 vertex xpos vs ypos", 100, -20., 20., 100, -10., 10.);

    protected void process(EventHeader event) {
        _numberOfEventsRead++;
        boolean skipEvent = true;
        if (event.getRunNumber() > 7000) {
            _beamEnergy = 2.306;
        }

        int nElectrons = 0;
        int nPositrons = 0;
        double eSum = 0.;
        List<ReconstructedParticle> finalStateParticles = event.get(ReconstructedParticle.class, "FinalStateParticles");
        for (ReconstructedParticle rp : finalStateParticles) {
            //only consider GBL tracks
            if (TrackType.isGBL(rp.getType())) {
                // don't consider FEEs or photons
                eSum += rp.getEnergy();
                if (rp.getEnergy() < _percentFeeCut * _beamEnergy) {
                    if (rp.getCharge() > 0) {
                        nPositrons++;
                    }
                    if (rp.getCharge() < 0) {
                        nElectrons++;
                    }
                }
            }
        }
        // only consider events with one positron and two electrons
        // beam electron which bremmed, and photon decay particles 
        if (nPositrons == 1 && nElectrons == 2) {
            // may also want to require no other activity in the event
            if (finalStateParticles.size() == _nReconstructedParticles) {
                setupSensors(event);
                List<Vertex> vertices = event.get(Vertex.class, "UnconstrainedV0Vertices");
                for (Vertex v : vertices) {
                    ReconstructedParticle rp = v.getAssociatedParticle();
                    int type = rp.getType();
                    boolean isGbl = TrackType.isGBL(type);
                    // require GBL tracks in vertex
                    if (isGbl) {
                        List<ReconstructedParticle> parts = rp.getParticles();
                        ReconstructedParticle rp1 = parts.get(0);
                        ReconstructedParticle rp2 = parts.get(1);
                        // basic sanity check here, remove full energy electrons (fee)
                        if (rp1.getMomentum().magnitude() > 1.5 * _beamEnergy || rp2.getMomentum().magnitude() > 1.5 * _beamEnergy) {
                            continue;
                        }
                        // require both reconstructed particles to have a track and a cluster
                        if (rp1.getClusters().size() != 1) {
                            continue;
                        }
                        if (rp2.getClusters().size() != 1) {
                            continue;
                        }
                        if (rp1.getTracks().size() != 1) {
                            continue;
                        }
                        if (rp2.getTracks().size() != 1) {
                            continue;
                        }
                        Track t1 = rp1.getTracks().get(0);
                        Track t2 = rp2.getTracks().get(0);
                        int t1nhits = t1.getTrackerHits().size();
                        int t2nhits = t2.getTrackerHits().size();

                        Cluster c1 = rp1.getClusters().get(0);
                        Cluster c2 = rp2.getClusters().get(0);
                        double deltaT = ClusterUtilities.getSeedHitTime(c1) - ClusterUtilities.getSeedHitTime(c2);
                        // require cluster times to be coincident within 2 ns
                        if (abs(deltaT) > 2.0) {
                            continue;
                        }
                        // require conversion e+e- to be in same hemisphere
                        //System.out.println(isTopTrack(t1) + " " + isTopTrack(t2));
                        if (isTopTrack(t1) && !isTopTrack(t2)) {
                            continue;
                        }
                        if (isTopTrack(t2) && !isTopTrack(t1)) {
                            continue;
                        }
                        // if we get here, we have a good candidate V0 and event
                        double m = rp.getMass();
                        double p = rp.getMomentum().magnitude();
                        double x = v.getPosition().x();
                        double y = v.getPosition().y();
                        double z = v.getPosition().z();
                        invMassHist_UnconstrainedV0Vertices.fill(m);
                        esumHist_UnconstrainedV0Vertices.fill(eSum);
                        zposHist_UnconstrainedV0Vertices.fill(z);
                        if (t1nhits == 6 && t2nhits == 6) {
                            zposHist_6hitUnconstrainedV0Vertices.fill(z);
                        }
                        if (t1nhits == 5 && t2nhits == 5) {
                            zposHist_5hitUnconstrainedV0Vertices.fill(z);
                        }
                        if ((t1nhits == 5 && t2nhits == 6) || (t1nhits == 6 && t2nhits == 5)) {
                            zposHist_56hitUnconstrainedV0Vertices.fill(z);
                        }

                        if (isTopTrack(t1)) {
                            zposHist_UnconstrainedV0Vertices_top.fill(z);
                            if (t1nhits == 6 && t2nhits == 6) {
                                zposHist_6hitUnconstrainedV0Vertices_top.fill(z);
                            }
                            if (t1nhits == 5 && t2nhits == 5) {
                                zposHist_5hitUnconstrainedV0Vertices_top.fill(z);
                            }
                            if ((t1nhits == 5 && t2nhits == 6) || (t1nhits == 6 && t2nhits == 5)) {
                                zposHist_56hitUnconstrainedV0Vertices_top.fill(z);
                            }

                        } else {
                            zposHist_UnconstrainedV0Vertices_bottom.fill(z);
                            if (t1nhits == 6 && t2nhits == 6) {
                                zposHist_6hitUnconstrainedV0Vertices_bottom.fill(z);
                            }
                            if (t1nhits == 5 && t2nhits == 5) {
                                zposHist_5hitUnconstrainedV0Vertices_bottom.fill(z);
                            }
                            if ((t1nhits == 5 && t2nhits == 6) || (t1nhits == 6 && t2nhits == 5)) {
                                zposHist_56hitUnconstrainedV0Vertices_bottom.fill(z);
                            }

                        }
                        psumVsMassHist_UnconstrainedV0Vertices.fill(p, m);
                        zposVsMassHist_UnconstrainedV0Vertices.fill(z, m);
                        xVsYHist_UnconstrainedV0Vertices.fill(x, y);
                        aida.cloud1D("event esum").fill(eSum);
                        aida.cloud1D("mass").fill(rp.getMass());
                        aida.cloud2D("psum vs mass").fill(rp.getMomentum().magnitude(), rp.getMass());
                        aida.cloud2D("z pos vs mass").fill(v.getPosition().z(), rp.getMass());
                        aida.cloud2D("x pos vs y pos").fill(x, y);

                        skipEvent = false;

                    }
                }
            }
        }
        if (skipEvent) {
            throw new Driver.NextEventException();
        } else {
            if (_writeRunAndEventNumber) {
                System.out.println(event.getRunNumber() + " " + event.getEventNumber());
            }
            _numberOfEventsWritten++;
        }
    }

    protected void endOfData() {
        System.out.println("Wrote " + _numberOfEventsWritten + " events of " + _numberOfEventsRead + " read.");
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

    public void setWriteRunAndEventNumber(boolean b) {
        _writeRunAndEventNumber = b;
    }
    
    public void setNumberOfReconstructedParticles(int n)
    {
        _nReconstructedParticles = n;
    }
}
