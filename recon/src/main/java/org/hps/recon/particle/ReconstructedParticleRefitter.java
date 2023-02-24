package org.hps.recon.particle;

import static java.lang.Math.abs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.util.FastMath;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.kalman.KalmanInterface;
import org.hps.recon.tracking.kalman.KalmanParams;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.ReconstructedParticle;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseReconstructedParticle;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
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
    private String outputFileName = "ReconPartRefit.root";
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
     * The interface to the Kalman Filter code 
     */
    private KalmanInterface KI;
    private org.lcsim.geometry.FieldMap fm;
    
    private double eRes0 = -1.0;
    private double eRes1 = -1.0;
    
    private static final boolean debug = false;
    /**
     * ECal energy resolution parameterization, for the resolution as a % of E.
     * @param eRes0    coefficient of the 1/sqrt(E) term
     */
    public void setERes0(double eRes0) {
        this.eRes0 = eRes0;
    }
    /**
     * ECal energy resolution parameterization, for the resolution as a % of E.
     * @param eRes1    the constant term
     */
    public void setERes1(double eRes1) {
        this.eRes1 = eRes1;
    }
    
    /**
     * Set up the geometry and parameters for the Kalman-filter track finding and fitting.
     */
    @Override
    public void detectorChanged(Detector det) {

        MaterialSupervisor materialManager;
        materialManager = new MaterialSupervisor();
        materialManager.buildModel(det);
        
        // Instantiate the interface to the Kalman-Filter code and set up the run parameters
        KalmanParams kPar = new KalmanParams(); 
        // Override the default resolution parameters with numbers from the steering file
        if (eRes0 > 0. || eRes1 > 0.) kPar.setEnergyRes(eRes0, eRes1);
        
        fm = det.getFieldMap();              // The HPS magnetic field map
        KI = new KalmanInterface(kPar, fm);  // Instantiate the Kalman interface
        ArrayList<SiStripPlane> detPlanes = new ArrayList<SiStripPlane>();
        List<ScatteringDetectorVolume> materialVols = ((MaterialSupervisor) (materialManager)).getMaterialVolumes();
        for (ScatteringDetectorVolume vol : materialVols) {
            detPlanes.add((SiStripPlane)(vol));
        }
        KI.createSiModules(detPlanes);   // The detector geometry objects used by the Kalman code
    }
    
    /**
     * The action per event
     * @param event      The hps-java event header
     */
    public void process(EventHeader event) {
        if (debug) {
            System.out.println("Entering process");
            if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
                System.out.println("SVTTrueHitRelations are present");
            }
            if (event.hasCollection(SimTrackerHit.class, "TrackerHits")) {
                System.out.println("Sim TrackerHits are present");
            }
            if (event.hasCollection(MCParticle.class, "MCParticles")) {
                System.out.println("MCParticles are present");
            }
            if (event.hasCollection(LCRelation.class, "KalmanFullTracksToTruthTrackRelations")) {
                System.out.println("KalmanFullTracksToTruthTrackRelations are present");
            }
        }
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
                        aida.histogram1D("e over p before refit", 100, 0., 2.).fill(eOverP);
                        if (debug) System.out.format("ReconstructedParticleRefitter: event %d, E/P=%10.5f, cut=%10.5f\n", event.getEventNumber(), eOverP, _eOverpCut);
                        if (abs(eOverP - 1.0) < _eOverpCut) {
                            // Get the old track info
                            Track oldTrack = rp.getTracks().get(0);
                            TrackState oldTsAtIP = null;
                            for (TrackState ts : oldTrack.getTrackStates()) {
                                if (ts.getLocation() == TrackState.AtIP) {
                                    oldTsAtIP = ts;
                                    break;
                                }
                            }
                            if (oldTsAtIP == null) oldTsAtIP = oldTrack.getTrackStates().get(0);
                            double[] oldParams = oldTsAtIP.getParameters();
                            // create a new ReconstructedParticle here...
                            ReconstructedParticle refitParticle = makeNewReconstructedParticle(event, rp);
                            refitReconstructedParticles.add(refitParticle);
                            Track newTrack = refitParticle.getTracks().get(0);
                            TrackState newTsAtIP = null;
                            for (TrackState ts : newTrack.getTrackStates()) {
                                if (ts.getLocation() == TrackState.AtIP) {
                                    newTsAtIP = ts;
                                    break;
                                }
                            }
                            if (newTsAtIP == null) newTsAtIP = newTrack.getTrackStates().get(0);
                            double[] newParams = newTsAtIP.getParameters();
                            for (int i=0; i<5; ++i) {
                                aida.histogram1D(String.format("Helix parameter %d new minus old over old", i), 100, -0.5, 0.5).fill((newParams[i]-oldParams[i])/oldParams[i]);
                            }
                            aida.histogram1D("old track chi2/dof", 100, 0., 50.).fill(oldTrack.getChi2()/oldTrack.getNDF());
                            aida.histogram1D("new track chi2/dof", 100, 0., 50.).fill(newTrack.getChi2()/newTrack.getNDF());
                            double [] P = newTsAtIP.getMomentum();
                            double pMag = FastMath.sqrt(P[0]*P[0]+P[1]*P[1]+P[2]*P[2]);
                            double changeInP = pMag/rp.getMomentum().magnitude();
                            aida.histogram1D("new momentum over old momentum", 100, 0.5, 1.5).fill(changeInP);
                            aida.histogram1D("new particle E over p", 100, 0., 2.).fill(rp.getEnergy()/pMag);
                            if (debug) System.out.format("ReconstructedParticleRefitter: Event %d, eOverP=%10.4f, changeInP=%10.4f\n",
                                    event.getEventNumber(), eOverP, changeInP);
                            MCParticle theMatch = getMCmatch(event, rp);
                            if (theMatch != null) {
                                double eMC = theMatch.getEnergy();
                                double newPoverEMC = pMag/eMC;
                                if (debug) System.out.format("   MC match: p/E MC = %10.4f\n", newPoverEMC);
                                aida.histogram1D("new momentum over MC energy", 100, 0.5, 1.5).fill(newPoverEMC);
                                aida.histogram1D("old momentum over MC energy", 100, 0.5, 1.5).fill(rp.getMomentum().magnitude()/eMC);
                            }
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
        KI.clearInterface();
    }

    /**
     *
     * The method to create a new ReconstructedParticle by refitting the track
     * along with the cluster energy
     *
     * @param rp the ReconstructedParticle to refit
     * @return
     */
    private ReconstructedParticle makeNewReconstructedParticle(EventHeader event, ReconstructedParticle rp) {
        // Create a reconstructed particle to represent the track.
        ReconstructedParticle particle = new BaseReconstructedParticle();
        Cluster cluster = rp.getClusters().get(0);
        // refit the track with the cluster energy
        Track newTrack = refitTrack(event, rp);
        // Store the track in the particle.
        particle.addTrack(newTrack); 
        if (debug) {
            double [] trackP = newTrack.getTrackStates().get(0).getMomentum();
            double P=FastMath.sqrt(trackP[0]*trackP[0]+trackP[1]*trackP[1]+trackP[2]*trackP[2]);
            System.out.format("makeNewReconstructedParticle:  track p=%10.4f\n", P);
        }

        // Set the type of the particle. This is used to identify the tracking
        // strategy used in finding the track associated with this particle.
        // Modify this to flag that we have refit with the energy.
        // For now, just add 1000
        ((BaseReconstructedParticle) particle).setType(1000 + newTrack.getType());
        ((BaseReconstructedParticle) particle).setParticleIdUsed(new SimpleParticleID(rp.getParticleIDUsed().getPDG(), 0, 0, 0));
        // add cluster to the particle:
        particle.addCluster(cluster);
        // will need to set the RP fourVector, charge, etc.
        // for now, leave as zero.
        // TODO discuss what the best measurement of this is

        return particle;
    }

    private MCParticle getMCmatch(EventHeader event, ReconstructedParticle rp) {
        MCParticle theMatch = null;
        RelationalTable rawtomc = null;
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        } else {
            return theMatch;
        }
        if (debug) System.out.println("getMCmatch: relation table constructed.");
        ArrayList<MCParticle> pMC = new ArrayList<MCParticle>(1);
        ArrayList<Integer> cnt = new ArrayList<Integer>(1);
        Track track = rp.getTracks().get(0);
        List<TrackerHit> hitsOnTrack = track.getTrackerHits();
        for (TrackerHit hit : hitsOnTrack) {
            List<RawTrackerHit> rawHits = hit.getRawHits();
            for (RawTrackerHit rawHit : rawHits) {
                Set<SimTrackerHit> simHits = rawtomc.allFrom(rawHit);
                for (SimTrackerHit simHit : simHits) {
                    MCParticle mcp = simHit.getMCParticle();
                    if (!pMC.contains(mcp)) {
                        pMC.add(mcp);
                        cnt.add(1);
                    } else {
                        int idx = pMC.indexOf(mcp);
                        cnt.set(idx,cnt.get(idx)+1);
                    }
                }
            }
        }
        if (debug) System.out.format("getMCmatch: %d MC matches found\n", pMC.size());
        int maxCnt = 0;
        for (int i=0; i<pMC.size(); ++i) {
            if (cnt.get(i) > maxCnt) {
                maxCnt = cnt.get(i);
                theMatch = pMC.get(i);
            }
        }
        return theMatch;
    }
    /**
     * Method for refitting a track
     *
     * @param rp The input ReconstructedParticle with track and matching cluster
     * @return the newly refit track including the cluster energy
     */
    private Track refitTrack(EventHeader event, ReconstructedParticle rp) {
        Track track = rp.getTracks().get(0);
        Cluster cluster = rp.getClusters().get(0);
        //the energy of the associated cluster
        double energy = cluster.getEnergy();

        // Fit a new track with this list of hits and the cluster energy.
        return KI.refitTrackWithE(event, track, energy);
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
     * Set the cut on E/p from the steering file
     * @param d    Maximum E/p allowed for electron/positron candidates
     */
    public void set_eOverpCut(double d) {
        _eOverpCut = d;
    }
    
    public void endOfData() {
        System.out.format("ReconstructedParticleRefitter: end-of-data reached.\n");
        try {
            System.out.format("Outputting the aida histograms now to file %s\n", outputFileName);
            aida.saveAs(outputFileName);
        } catch (IOException ex) {
            System.out.println("ReconstructedParticleRefitter: exception when writing out histograms");
        }
        KI.summary();
    }
}
