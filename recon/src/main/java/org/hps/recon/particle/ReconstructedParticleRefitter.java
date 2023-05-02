package org.hps.recon.particle;

import static java.lang.Math.abs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.util.FastMath;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MaterialSupervisor.ScatteringDetectorVolume;
import org.hps.recon.tracking.MaterialSupervisor.SiStripPlane;
import org.hps.recon.tracking.kalman.KalmanInterface;
import org.hps.recon.tracking.kalman.KalmanParams;
import org.hps.recon.tracking.kalman.KalmanPatRecDriver;
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
import org.lcsim.geometry.IDDecoder;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 * This Driver is used to improve the measurement of ReconstructedParticles
 * composed of a Track and an associated Ecal Cluster by including the energy of
 * the cluster in a global refit.
 *
 * A new collection of ReconstructedParticles is added to the event.
 *
 * @authors Norman A. Graf and Robert P. Johnson
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
    private boolean _doEconstraint = true;
    private boolean _cheat = false;
    private Random ran;
    
    /**
     * The interface to the Kalman Filter code 
     */
    private KalmanInterface KI;
    private KalmanParams kPar = null;
    private org.lcsim.geometry.FieldMap fm;
    private static Logger logger;
    private Level _logLevel = Level.WARNING;
    private IDDecoder decoder;
    
    private double _eRes0 = -1.0;
    private double _eRes1 = -1.0;
    
    private static final boolean debug = false;
    /**
     * Feature to allow setting the log level from steering
     * @param logLevel
     */
    public void set_logLevel(String logLevel) {
        System.out.format("ReconstructedParticleRefitter: setting the logger level to %s\n", logLevel);
        _logLevel = Level.parse(logLevel);
        System.out.format("                    logger level = %s\n", _logLevel.getName());
    }
    /**
     * Option to fake the ECAL info with a Gaussian random number, for testing
     * @param b    true to fake the ECAL info using MC truth
     */
    public void set_cheat(boolean b) {
        System.out.format("ReconstructedParticleRefitter: setting control parameter for using MC truth for the energy constraint to %b\n", b);
        _cheat = b;
    }
    /**
     * Control whether the ECAL energy constraint is applied to refitted tracks
     * @param b     true to refit with ECAL constraint
     */
    public void set_doEconstraint(boolean b) {
        System.out.format("ReconstructedParticleRefitter: setting control parameter for doing the ECAL constraint to %b\n", b);
        _doEconstraint = b;
    }
    
    private ArrayList<Integer> layerSkip = null;
    public void set_removeLayer(int lyr) {
        if (layerSkip == null) {
            layerSkip = new ArrayList<Integer>();
        }
        layerSkip.add(lyr);
        System.out.format("ReconstructedParticleRefitter: remove layer %d from the fit.\n", lyr);
    }
    /**
     * ECal energy resolution parameterization, for the resolution as a % of E.
     * @param eRes0    coefficient of the 1/sqrt(E) term
     */
    public void set_eRes0(double eRes0) {
        System.out.format("ReconstructedParticleRefitter: setting the eRes0 ECAL resolution parameter to %9.4f\n", eRes0);
        _eRes0 = eRes0;
    }
    /**
     * ECal energy resolution parameterization, for the resolution as a % of E.
     * @param eRes1    the constant term
     */
    public void set_eRes1(double eRes1) {
        System.out.format("ReconstructedParticleRefitter: setting the eRes1 ECAL resolution parameter to %9.4f\n", eRes1);
        _eRes1 = eRes1;
    }
    
    /**
     * Set up the geometry and parameters for the Kalman-filter track finding and fitting.
     */
    @Override
    public void detectorChanged(Detector det) {
        logger = Logger.getLogger(KalmanPatRecDriver.class.getName());
        if (_logLevel != null) {
            logger.setLevel(_logLevel);
            //LogManager.getLogManager().getLogger(Logger.GLOBAL_LOGGER_NAME).setLevel(_logLevel);
        }
        System.out.format("ReconstructedParticleRefitter: entering detectorChanged, logger level = %s\n", logger.getLevel().getName());
        MaterialSupervisor materialManager;
        materialManager = new MaterialSupervisor();
        materialManager.buildModel(det);
        ran = new Random();
        if (layerSkip != null) {
            for (int lyr : layerSkip) {
                System.out.format("ReconstructedParticleRefitter: layer %d will be removed in track refits\n", lyr);
            }
        }
        decoder = det.getSubdetector("Tracker").getIDDecoder();
        
        // Instantiate the interface to the Kalman-Filter code and set up the run parameters
        kPar = new KalmanParams(); 
        // Override the default resolution parameters with numbers from the steering file
        if (_eRes0 > 0. || _eRes1 > 0.) kPar.setEnergyRes(_eRes0, _eRes1);
        
        fm = det.getFieldMap();              // The HPS magnetic field map
        KI = new KalmanInterface(kPar, det, fm);  // Instantiate the Kalman interface
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
                        // cut on on E/p so we don't try to fit to the energy of MIP tracks
                        double eOverP = rp.getEnergy() / rp.getMomentum().magnitude();
                        aida.histogram1D("e over p before refit", 100, 0., 2.).fill(eOverP);
                        if (debug) System.out.format("ReconstructedParticleRefitter: event %d, E/P=%10.5f, cut=%10.5f, E=%10.5f, p=%10.5f\n", event.getEventNumber(), eOverP, _eOverpCut, rp.getEnergy(), rp.getMomentum().magnitude());
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
                            Track track = rp.getTracks().get(0);
                            int nHits = track.getTrackerHits().size();
                            if (debug) {
                                boolean skipped = false;
                                int lastLyr = -1;
                                for (TrackerHit hit : track.getTrackerHits()) {
                                    List<RawTrackerHit> rawHits = hit.getRawHits();
                                    int Layer = -1;
                                    for (RawTrackerHit rawHit : rawHits) {
                                        long ID = rawHit.getCellID();
                                        decoder.setID(ID);
                                        Layer = decoder.getValue("layer") - 1;
                                    }
                                    if (lastLyr >= 0) {
                                        if (lastLyr != Layer - 1) skipped = true;
                                    }
                                    lastLyr = Layer;
                                }
                                System.out.format("event %d, %d track hits, nDOF=%d, skipped layer=%b\n", event.getEventNumber(), nHits, track.getNDF(), skipped);
                            }
                            int nDOF = nHits-5;
                            
                            // create a new ReconstructedParticle here...
                            ReconstructedParticle refitParticle = makeNewReconstructedParticle(event, rp);
                            if (refitParticle.getTracks().size() == 0) {
                                if (debug) System.out.println("    Track refit failed");
                                return;
                            }
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
                            aida.histogram1D("Helix parameter d0 new minus old over old", 100, -2.5, 2.5).fill((newParams[0]-oldParams[0])/oldParams[0]);
                            aida.histogram1D("Helix parameter phi0 new minus old over old", 100, -0.5, 0.5).fill((newParams[1]-oldParams[1])/oldParams[1]);
                            aida.histogram1D("Helix parameter omega new minus old over old", 100, -0.5, 0.5).fill((newParams[2]-oldParams[2])/oldParams[2]);
                            aida.histogram1D("Helix parameter z0 new minus old over old", 100, -0.5, 0.5).fill((newParams[3]-oldParams[3])/oldParams[3]);
                            aida.histogram1D("Helix parameter tan(lambda) new minus old over old", 100, -0.5, 0.5).fill((newParams[4]-oldParams[4])/oldParams[4]);
                            aida.histogram1D("old track chi2 per dof", 100, 0., 50.).fill(oldTrack.getChi2()/nDOF);
                            aida.histogram1D("old track number degrees of freedom",20,0.,20.).fill(nDOF);
                            aida.histogram1D("new track chi2 per dof", 100, 0., 50.).fill(newTrack.getChi2()/newTrack.getNDF());
                            aida.histogram1D("new track number degrees of freedom",20,0.,20.).fill(newTrack.getNDF());
                            double [] P = newTsAtIP.getMomentum();
                            double pMag = FastMath.sqrt(P[0]*P[0]+P[1]*P[1]+P[2]*P[2]);
                            double changeInP = pMag/rp.getMomentum().magnitude();
                            aida.histogram1D("new momentum over old momentum", 100, 0.5, 1.5).fill(changeInP);
                            if (Math.abs(changeInP-1.0) > 0.04) {
                                aida.histogram1D("Bad new track chi2 per dof", 100, 0., 50.).fill(newTrack.getChi2()/newTrack.getNDF());
                                aida.histogram1D("Bad new track number degrees of freedom",20,0.,20.).fill(newTrack.getNDF());
                                aida.histogram1D("Bad helix parameter d0 new minus old over old", 100, -2.5, 2.5).fill((newParams[0]-oldParams[0])/oldParams[0]);
                                aida.histogram1D("Bad helix parameter phi0 new minus old over old", 100, -0.5, 0.5).fill((newParams[1]-oldParams[1])/oldParams[1]);
                                aida.histogram1D("Bad helix parameter z0 new minus old over old", 100, -0.5, 0.5).fill((newParams[3]-oldParams[3])/oldParams[3]);
     
                            }
                            aida.histogram1D("new particle E over p", 100, 0., 2.).fill(rp.getEnergy()/pMag);
                            if (debug) System.out.format("ReconstructedParticleRefitter: Event %d, eOverP=%10.4f, changeInP=%10.4f\n",
                                    event.getEventNumber(), eOverP, changeInP);
                            
                            // Comparisons for MC events
                            MCParticle theMatch = getMCmatch(event, rp);
                            if (theMatch != null) {
                                double eMC = theMatch.getEnergy();
                                double newPoverEMC = pMag/eMC;
                                if (debug) System.out.format("   MC match: p/E MC = %10.4f\n", newPoverEMC);
                                aida.histogram1D("new momentum over MC energy", 100, 0.5, 1.5).fill(newPoverEMC);
                                aida.histogram1D("old momentum over MC energy", 100, 0.5, 1.5).fill(rp.getMomentum().magnitude()/eMC);
                                aida.histogram1D("ECAL over MC energy", 100, 0.5, 1.5).fill(rp.getEnergy()/eMC);
                                TrackState newTsAtLastHit = null;
                                for (TrackState ts : newTrack.getTrackStates()) {
                                    if (ts == null) {
                                        System.out.format("ReconstructedParticle Refitter: event %d, null Trackstate pointer!\n", event.getEventNumber());
                                        break;
                                    }
                                    if (debug) System.out.format("ReconstructedParticle Refitter: trackstate at %d of %d\n", ts.getLocation(), newTrack.getTrackStates().size());
                                    if (ts.getLocation() == TrackState.AtLastHit) {
                                        newTsAtLastHit = ts;
                                        break;
                                    }
                                }
                                if (newTsAtLastHit != null) {
                                    double [] Plast = newTsAtLastHit.getMomentum();
                                    double pMagLst = FastMath.sqrt(Plast[0]*Plast[0]+Plast[1]*Plast[1]+Plast[2]*Plast[2]);
                                    if (debug) {
                                        double [] refPnt = newTsAtLastHit.getReferencePoint();
                                        double [] helix = newTsAtLastHit.getParameters();
                                        System.out.format("Event %d at last hit, p=%10.5f, ref=%8.3f %8.3f %8.3f\n", event.getEventNumber(), pMagLst, refPnt[0], refPnt[1], refPnt[2]);
                                        System.out.format("   Helix at last hit = %9.5f %9.5f %9.5f %9.5f %9.5f\n", helix[0], helix[1], helix[2], helix[3], helix[4]);
                                    }
                                    aida.histogram1D("p at last hit over MC energy", 100, 0.5, 1.5).fill(pMagLst/eMC);
                                    double pFrstvsPlst = (pMag - pMagLst)/pMagLst;
                                    aida.histogram1D("fractional change of p from smoothing", 100, -0.1, 0.1).fill(pFrstvsPlst);
                                }
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
        if (debug) System.out.format("Entering makeNewReconstructedParticle for event %d\n", event.getEventNumber());
        Cluster cluster = rp.getClusters().get(0);
        // refit the track 
        Track newTrack = refitTrack(event, rp);
        if (newTrack == null) {
            if (debug) System.out.format("makeNewReconstrucedParticle: failed to make new track in event %d\n", event.getEventNumber());
            return particle;
        }
        // Store the track in the particle.
        particle.addTrack(newTrack); 
        if (debug) {
            double [] trackP = newTrack.getTrackStates().get(0).getMomentum();
            double P=FastMath.sqrt(trackP[0]*trackP[0]+trackP[1]*trackP[1]+trackP[2]*trackP[2]);
            int nHits = newTrack.getTrackerHits().size();
            int nDOF = newTrack.getNDF();
            System.out.format("makeNewReconstructedParticle:  track p=%10.4f, %d hits, NDF=%d \n", P, nHits, nDOF);
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
        if (debug) {
            int nHits = track.getTrackerHits().size();
            System.out.format("refitTrack in event %d with %d hits, chi2=%8.3f for %d dof, cluster E=%9.4f\n", event.getEventNumber(), nHits, track.getChi2(), track.getNDF(), energy);
        }
        // Fit a new track with this list of hits and the cluster energy.
        if (_cheat) {
            MCParticle theMatch = getMCmatch(event, rp);
            if (theMatch != null) {
                double energyMC = theMatch.getEnergy();
                double sigmaE = (kPar.getEres(0)/FastMath.sqrt(energy) + kPar.getEres(1))*energy/100.;
                energy = energyMC + sigmaE*ran.nextGaussian();
                if (debug) System.out.format("refitTrack: MC cheat energy = %10.5f+=%9.5f\n", energy, sigmaE);
                aida.histogram1D("Cheat ECAL over MC energy", 100, 0.5, 1.5).fill(energy/energyMC);
            } else {    
                System.out.format("refitTrack: no MC match was found\n");
            }
        }
        if (layerSkip == null) layerSkip = new ArrayList<Integer>();
        return KI.refitTrackWithE(event, track, energy, _doEconstraint, layerSkip);
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
    public void set_finalStateParticleCollectionName(String s) {
        System.out.format("ReconstructedParticleRefitter: setting the final state particle collection name to %s\n", s);
        _finalStateParticleCollectionName = s;
    }

    /**
     * Convenience method to set the name of the output collection of
     * ReconstructedParticles
     *
     * @param s
     */
    public void set_refitParticleCollectionName(String s) {
        System.out.format("ReconstructedParticleRefitter: setting the output collection name to %s\n", s);
        _refitParticleCollectionName = s;
    }

    /**
     * Set the cut on E/p from the steering file
     * @param d    Maximum E/p allowed for electron/positron candidates
     */
    public void set_eOverpCut(double d) {
        System.out.format("ReconstructedParticleRefitter: setting the E/p cut value to %9.4f\n", d);
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
