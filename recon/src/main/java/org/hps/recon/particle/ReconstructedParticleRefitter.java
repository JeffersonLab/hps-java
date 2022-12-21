package org.hps.recon.particle;

import hep.physics.vec.Hep3Vector;
import static java.lang.Math.abs;
import java.util.ArrayList;
import java.util.List;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver is used to improve the measurement of ReconstructedParticles
 * composed of a Track and an associated Ecal Cluster by including the energy of
 * the cluster in a global refit.
 *
 * A new collection of ReconstructedParticles is added to the event.
 *
 * @author Norman A. Graf
 *
 */
public class ReconstructedParticleRefitter extends Driver {

    /**
     * The histogram handler
     */
    private AIDA aida = AIDA.defaultInstance();

    /**
     * The name of the input ReconstructedParticle collection to process
     */
    private String _finalStateParticleCollectionName = "FinalStateParticles_KF";

    /**
     * The name of the output ReconstructedParticle collection to add to the
     * event
     */
    private String _refitParticleCollectionName = "FinalStateParticles_KF_refit";

    /**
     * The tolerance on E/p for tracks and clusters to make sure we are fitting
     * to a showering electron or positron and not a MIP trace or poorly matched
     * combination
     */
    private double _eOverpCut = 0.1;

    /**
     * The action
     *
     * @param event
     */
    public void process(EventHeader event) {
        if (event.hasCollection(ReconstructedParticle.class, _finalStateParticleCollectionName)) {
            // setup the hit-to-sensor associations
            // should not need to do this if not reading in events from disk
            setupSensors(event);
            // instantiate the output collection of new ReconstructedParticles
            List<ReconstructedParticle> refitReconstructedParticles = new ArrayList<>();
            //fetch the input list of ReconstructedParticles to process
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, _finalStateParticleCollectionName);
            for (ReconstructedParticle rp : rpList) {
                // skip particles without a track, i.e. photons
                if (rp.getParticleIDUsed().getPDG() != 22) {
                    // skip particles without an associated cluster
                    if (!rp.getClusters().isEmpty()) {
                        // quick check on E/p so we don't try to fit to the energy of MIP tracks
                        double eOverP = rp.getEnergy() / rp.getMomentum().magnitude();
                        aida.histogram1D("e over p", 100, 0., 2.).fill(eOverP);
                        if (abs(eOverP - 1) < _eOverpCut) {
                            // create a new ReconstructedParticle here...
                            refitReconstructedParticles.add(makeNewReconstructedParticle(rp));
                        } else {
                            refitReconstructedParticles.add(rp);
                        }
                    } else {
                        refitReconstructedParticles.add(rp);
                    }
                } else {
                    refitReconstructedParticles.add(rp);
                }
            }
            // add the new collection to the event
            event.put(_refitParticleCollectionName, refitReconstructedParticles, ReconstructedParticle.class, 0);
        }
    }

    /**
     *
     * The method to create a new ReconstructedParticle by refitting the track
     * along with the cluster energy
     *
     * @param rp the ReconstructedParticle to refit
     * @return
     */
    private ReconstructedParticle makeNewReconstructedParticle(ReconstructedParticle rp) {
        // Create a reconstructed particle to represent the track.
        ReconstructedParticle particle = new BaseReconstructedParticle();
        Cluster cluster = rp.getClusters().get(0);
        // refit the track with the cluster energy
        Track newTrack = refitTrack(rp);
        // Store the track in the particle.
        particle.addTrack(newTrack);

        // Set the type of the particle. This is used to identify
        // the tracking strategy used in finding the track associated with
        // this particle.
        // Modify this to flag that we have refit with the energy
        // for now, just add 1000
        ((BaseReconstructedParticle) particle).setType(1000 + newTrack.getType());
        ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(rp.getParticleIDUsed().getPDG(), 0, 0, 0));
        // add cluster to the particle:
        particle.addCluster(cluster);
        // will need to set the RP fourVector, charge, etc.
        // for now, leave as zero.
        // TODO discuss what the best measurement of this is

        return particle;
    }

    /**
     * Method stub for refitting a track
     *
     * @param rp The input ReconstructedParticle with track and matching cluster
     * @return the newly refit track including the cluster energy
     */
    private Track refitTrack(ReconstructedParticle rp) {
        Track track = rp.getTracks().get(0);
        Cluster cluster = rp.getClusters().get(0);
        //the energy of the associated cluster
        double energy = cluster.getEnergy();
        //the list of tracker hits
        List<TrackerHit> hits = track.getTrackerHits();
        // initial guess for the track parameters
        Hep3Vector momentum = rp.getMomentum();
        // TODO fit a new track with this list of hits and the cluster energy.
        // for now simply return the existing track
        return track;
    }

    /**
     * Method to associate SVT hits to sensors
     *
     * @param event
     */
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

    /**
     * Convenience method to set the name of the input collection of
     * ReconstructedParticles
     *
     * @param s
     */
    public void setFinalStateParticleCollectionName(String s) {
        _finalStateParticleCollectionName = s;
    }

    /**
     * Convenience method to set the name of the output collection of
     * ReconstructedParticles
     *
     * @param s
     */
    public void setRefitParticleCollectionName(String s) {
        _refitParticleCollectionName = s;
    }

    /**
     *
     * @param d
     */
    public void set_eOverpCut(double d) {
        _eOverpCut = d;
    }
}
