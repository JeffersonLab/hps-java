package org.hps.recon.tracking.gbl;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 * Calculate the input needed for Millepede minimization.
 *
 * @author Per Hansson Adrian <phansson@slac.stanford.edu>
 */
public class GBLOutput {

    private int _debug = 0;
    private GBLFileIO textFile = null;
    private Hep3Vector bFieldVector;
    private final TrackerHitUtils trackerHitUtils = new TrackerHitUtils();
    private final MaterialSupervisor materialManager;
    private final MultipleScattering _scattering;
    private final double _beamEnergy = 1.1; // GeV
    private boolean isMC = false;

    /**
     * Constructor
     *
     * @param outputFileName is the filename given to the text-based output file. If empty no output file is written
     * @param bfield magnetic field in Tesla
     */
    GBLOutput(String outputFileName, Hep3Vector bfield) {
        // System.out.printf("name \"%s\" \n", outputFileName);
        if (!outputFileName.equalsIgnoreCase("")) {
            textFile = new GBLFileIO(outputFileName);
        }
        materialManager = new MaterialSupervisor();
        _scattering = new MultipleScattering(materialManager);
        bFieldVector = CoordinateTransformations.transformVectorToTracking(bfield);
        _scattering.setBField(Math.abs(bFieldVector.z())); // only absolute of B is needed as it's used for momentum
                                                           // calculation only
    }

    public void setIsMC(boolean input) {
        isMC = input;
    }

    public void setDebug(int debug) {
        _debug = debug;
        _scattering.setDebug((_debug > 0));
    }

    public void buildModel(Detector detector) {
        materialManager.buildModel(detector);
    }

    void printNewEvent(int eventNumber, double Bz) {
        if (textFile != null) {
            textFile.printEventInfo(eventNumber, Bz);
        }
    }

    void printTrackID(int iTrack) {
        if (textFile != null) {
            textFile.printTrackID(iTrack);
        }
    }

    void close() {
        if (textFile != null) {
            textFile.closeFile();
        }
    }

    public Hep3Vector get_B() {
        return bFieldVector;
    }

    public void set_B(Hep3Vector _B) {
        this.bFieldVector = _B;
    }

    private MCParticle getAprime(List<MCParticle> mcParticles, MCParticle mcp) {
        MCParticle ap = null;

        // check if this is an A' event
        for (MCParticle part : mcParticles) {
            if (Math.abs(part.getPDGID()) == 622) {
                ap = part;
                break;
            }
        }

        if (_debug > 0)
            System.out.printf("%s: truth particle (pdgif %d ) found in event!\n", this.getClass().getSimpleName(), mcp.getPDGID());

        // If this is an A' event, do some more checks
        if (ap != null) {
            // A few MC files have broken links b/w parents-daughters
            // This causes the MC particle to come from the origin even if the decay happen somewhere else
            if (this.getAprimeDecayProducts(mcParticles).size() > 0) {
                // do a full check
                if (!checkAprimeTruth(mcp, mcParticles))
                    ap = null;
            }
        }

        return ap;
    }

    private HelicalTrackFit getHTFtruth(MCParticle mcp, MCParticle ap) {
        // check if we should be using a different origin than the particle tells us
        Hep3Vector mcp_origin;
        if (ap != null) {
            // There is an A' here. Use its origin if different
            if (_debug > 0)
                System.out.printf("%s: A' found with origin  %s compared to particle %s (diff: %s)\n", this.getClass().getSimpleName(), ap.getOrigin().toString(), mcp.getOrigin().toString(), VecOp.sub(ap.getOrigin(), mcp.getOrigin()).toString());
            // TODO: is this really what we want?
            if (VecOp.sub(ap.getOrigin(), mcp.getOrigin()).magnitude() > 0.00001)
                mcp_origin = ap.getOrigin();
            else
                mcp_origin = mcp.getOrigin();
        } else {
            // No A', use particle origin
            mcp_origin = mcp.getOrigin();
        }

        return TrackUtils.getHTF(mcp, mcp_origin, -1.0 * this.bFieldVector.z());
    }

    void doTrackParams(GBLTrackData gtd, HelicalTrackFit htf, HelicalTrackFit htfTruth, GblUtils.PerigeeParams perParTruth) {
        GblUtils.PerigeeParams perPar = new GblUtils.PerigeeParams(htf, bFieldVector.z());
        gtd.setPerigeeTrackParameters(perPar);
        Hep3Matrix perToClPrj = GblUtils.getPerToClPrj(htf);
        for (int row = 0; row < perToClPrj.getNRows(); ++row) {
            for (int col = 0; col < perToClPrj.getNColumns(); ++col) {
                gtd.setPrjPerToCl(row, col, perToClPrj.e(row, col));
            }
        }

        if (textFile != null) {
            textFile.printPerTrackParam(gtd.getPerigeeTrackParameters());
            textFile.printPerTrackParamTruth(perParTruth);
            textFile.printClTrackParam(new GblUtils.ClParams(htf, bFieldVector.z()));
            if (htfTruth != null)
                textFile.printClTrackParamTruth(new GblUtils.ClParams(htfTruth, bFieldVector.z()));
            else
                textFile.printClTrackParamTruth(null);
            textFile.printPerToClPrj(gtd.getPrjPerToCl());
            textFile.printChi2(htf.chisq(), htf.ndf());
            textFile.printPerTrackCov(htf);
        }
    }

    Map<Integer, SimTrackerHit> makeSimHitsLayerMap(List<SimTrackerHit> simTrackerHits, MCParticle mcp) {
        Map<Integer, SimTrackerHit> simHitsLayerMap = new HashMap<Integer, SimTrackerHit>();
        // build map of layer to SimTrackerHits that belongs to the MC particle
        for (SimTrackerHit sh : simTrackerHits) {
            if (sh.getMCParticle() == mcp) {
                int layer = sh.getIdentifierFieldValue("layer");
                if (!simHitsLayerMap.containsKey(layer) || (sh.getPathLength() < simHitsLayerMap.get(layer).getPathLength())) {
                    simHitsLayerMap.put(layer, sh);
                }
            }
        }
        return simHitsLayerMap;
    }

    void printGBL(Track trk, List<SiTrackerHitStrip1D> stripHits, GBLTrackData gtd, List<GBLStripClusterData> stripClusterDataList, List<MCParticle> mcParticles, List<SimTrackerHit> simTrackerHits, boolean mc) {
        this.setIsMC(mc);
        printGBL(trk, stripHits, gtd, stripClusterDataList, mcParticles, simTrackerHits);
    }

    void printGBL(Track trk, List<SiTrackerHitStrip1D> stripHits, GBLTrackData gtd, List<GBLStripClusterData> stripClusterDataList, List<MCParticle> mcParticles, List<SimTrackerHit> simTrackerHits) {

        HelicalTrackFit htf = TrackUtils.getHTF(trk);
        List<TrackerHit> hits = trk.getTrackerHits();

        // Find the truth particle of the track
        MCParticle mcp = null;
        MCParticle ap = null;
        HelicalTrackFit htfTruth = null;
        GblUtils.PerigeeParams perParTruth = null;
        Map<Integer, SimTrackerHit> simHitsLayerMap = null;
        if (isMC) {
            mcp = TrackUtils.getMatchedTruthParticle(trk);
            if (mcp != null) {
                ap = getAprime(mcParticles, mcp);
                if (ap != null) {
                    htfTruth = getHTFtruth(mcp, ap);
                    perParTruth = new GblUtils.PerigeeParams(htfTruth, bFieldVector.z());
                    simHitsLayerMap = makeSimHitsLayerMap(simTrackerHits, mcp);
                }
            } else {
                // TODO: what do we really want here?
                System.out.printf("%s: WARNING!! no truth particle found in event!\n", this.getClass().getSimpleName());
                this.printMCParticles(mcParticles);
                // System.exit(1);
            }
        }
        doTrackParams(gtd, htf, htfTruth, perParTruth);

        int istrip = 0;
        for (TrackerHit thit : hits) {
            HelicalTrackHit hit = (HelicalTrackHit) thit;
            List<HelicalTrackStrip> strips = ((HelicalTrackCross) hit).getStrips();
            Hep3Vector correctedHitPosition = hit.getCorrectedPosition();

            for (HelicalTrackStrip stripOld : strips) {
                IDetectorElement de = ((RawTrackerHit) stripOld.rawhits().get(0)).getDetectorElement();
                if (!(de instanceof HpsSiSensor)) {
                    continue;
                }
                HpsSiSensor sensor = (HpsSiSensor) de;
                HelicalTrackStripGbl strip = new HelicalTrackStripGbl(stripOld, true);
                MultipleScattering.ScatterPoint temp = MakeGblTracks.getScatterPointGbl(sensor, strip, htf, _scattering, bFieldVector.z());
                if (temp == null)
                    continue;
                GBLStripClusterData stripData = MakeGblTracks.makeStripData(sensor, strip, htf, temp);
                if (stripData == null)
                    continue;

                if (textFile != null) {
                    textFile.printStrip(istrip, sensor.getMillepedeId(), sensor.getName());
                    textFile.printOrigin(strip.origin());
                    textFile.printHitPos3D(correctedHitPosition);
                    textFile.printStripTrackPos(temp.getPosition());
                    printStripData(stripData);
                }

                // order: iso, u, ures,  truth ures, simhit ures, scatangle
                doStripIso(stripHits, strip, sensor.getName());
                
                if (textFile != null) {
                    textFile.printStripMeas(stripData.getMeas());
                    textFile.printStripMeasRes(stripData.getMeas() - stripData.getTrackPos().x(), stripData.getMeasErr());
                }

                doResiduals(strip, htfTruth, stripData, simHitsLayerMap, temp.getPosition());

                if (textFile != null) 
                    textFile.printStripScat(stripData.getScatterAngle());
                
                ++istrip;
            }
        }

    }

    private void printStripData(GBLStripClusterData stripData) {
        textFile.printStripPathLen(stripData.getPath());
        textFile.printStripPathLen3D(stripData.getPath3D());
        textFile.printMeasDir(stripData.getU());
        textFile.printNonMeasDir(stripData.getV());
        textFile.printNormalDir(stripData.getW());
        textFile.printStripTrackDir(Math.sin(stripData.getTrackPhi()), Math.sin(stripData.getTrackLambda()));
        textFile.printStripTrackDirFull(stripData.getTrackDirection());
    }

    private void doResiduals(HelicalTrackStripGbl strip, HelicalTrackFit htfTruth, GBLStripClusterData stripData, Map<Integer, SimTrackerHit> simHitsLayerMap, Hep3Vector pos) {
        // truth residuals
        Hep3Matrix trkToStripRot = trackerHitUtils.getTrackToStripRotation(strip.getStrip());
        if (textFile != null) {
            Hep3Vector trkposTruth = htfTruth != null ? TrackUtils.getHelixPlaneIntercept(htfTruth, strip, Math.abs(bFieldVector.z())) : new BasicHep3Vector(-999999.9, -999999.9, -999999.9);
            Hep3Vector vdiffTrkTruth = htfTruth != null ? VecOp.sub(trkposTruth, strip.origin()) : null;
            Hep3Vector trkposTruth_meas = vdiffTrkTruth != null ? VecOp.mult(trkToStripRot, vdiffTrkTruth) : null;
            // residual in measurement frame
            Double resTruth_meas_x = trkposTruth_meas != null ? strip.umeas() - trkposTruth_meas.x() : null;
            textFile.printStripMeasResTruth(resTruth_meas_x != null ? resTruth_meas_x : -9999999.9, strip.du());
        }

        // sim hit residual based on the sim tracker hit for this layer
        double printSimHitPos = -999999.9;
        double printMeasErr = -999999.9;
        if (isMC && simHitsLayerMap != null) {
            SimTrackerHit simHit = simHitsLayerMap.get(strip.layer());
            if (simHit != null) {
                Hep3Vector simHitPos = CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec());
                Hep3Vector vdiffSimHit = VecOp.sub(simHitPos, pos);
                Hep3Vector simHitPos_meas = VecOp.mult(trkToStripRot, vdiffSimHit);
                printSimHitPos = simHitPos_meas.x();
                printMeasErr = stripData.getMeasErr();
            }
        }
        if (textFile != null) {
            textFile.printStripMeasResSimHit(printSimHitPos, printMeasErr);
        }
    }

    // TODO: do we ever need this?
    private void doStripIso(List<SiTrackerHitStrip1D> stripHits, HelicalTrackStripGbl strip, String sensorName) {
        double stripIsoMin = 9999.9;
        if (textFile != null) {
            for (SiTrackerHitStrip1D stripHit : stripHits) {
                if (stripHit.getRawHits().get(0).getDetectorElement().getName().equals(sensorName)) {
                    SiTrackerHitStrip1D local = stripHit.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
                    double d = Math.abs(strip.umeas() - local.getPosition()[0]);
                    if (d < stripIsoMin && d > 0)
                        stripIsoMin = d;
                }
            }
            if (_debug > 0)
                System.out.printf("%s: stripIsoMin = %f \n", this.getClass().getSimpleName(), stripIsoMin);
        }
        if (textFile != null) {
            textFile.printStripIso(stripIsoMin);
        }
    }

    private List<MCParticle> getAprimeDecayProducts(List<MCParticle> mcParticles) {
        List<MCParticle> pair = new ArrayList<MCParticle>();
        for (MCParticle mcp : mcParticles) {
            if (mcp.getGeneratorStatus() != MCParticle.FINAL_STATE) {
                continue;
            }
            boolean hasAprimeParent = false;
            for (MCParticle parent : mcp.getParents()) {
                if (Math.abs(parent.getPDGID()) == 622) {
                    hasAprimeParent = true;
                }
            }
            if (hasAprimeParent) {
                pair.add(mcp);
            }
        }

        return pair;

    }

    private boolean checkAprimeTruth(MCParticle mcp, List<MCParticle> mcParticles) {
        List<MCParticle> mcp_pair = getAprimeDecayProducts(mcParticles);

        if (mcp_pair.size() != 2) {
            System.out.printf("%s: ERROR this event has %d mcp with 622 as parent!!??  \n", this.getClass().getSimpleName(), mcp_pair.size());
            this.printMCParticles(mcParticles);
            return false;
        }
        if (Math.abs(mcp_pair.get(0).getPDGID()) != 11 || Math.abs(mcp_pair.get(1).getPDGID()) != 11) {
            System.out.printf("%s: ERROR decay products are not e+e-? \n", this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            return false;
        }
        if (mcp_pair.get(0).getPDGID() * mcp_pair.get(1).getPDGID() > 0) {
            System.out.printf("%s: ERROR decay products have the same sign? \n", this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            return false;
        }

        // cross-check
        //        if (!mcp_pair.contains(mcp)) {
        //            boolean hasBeamElectronParent = false;
        //            for (MCParticle parent : mcp.getParents()) {
        //                if (parent.getGeneratorStatus() != MCParticle.FINAL_STATE && parent.getPDGID() == 11 && parent.getMomentum().y() == 0.0 && Math.abs(parent.getMomentum().magnitude() - _beamEnergy) < 0.01) {
        //                    hasBeamElectronParent = true;
        //                }
        //            }
        //            if (!hasBeamElectronParent) {
        //                System.out.printf("%s: the matched MC particle is not an A' daughter and not a the recoil electrons!?\n", this.getClass().getSimpleName());
        //                System.out.printf("%s: %s %d p %s org %s\n", this.getClass().getSimpleName(), mcp.getGeneratorStatus() == MCParticle.FINAL_STATE ? "F" : "I", mcp.getPDGID(), mcp.getMomentum().toString(), mcp.getOrigin().toString());
        //                printMCParticles(mcParticles);
        //                return false;
        //            } else if (_debug > 0) {
        //                System.out.printf("%s: the matched MC particle is the recoil electron\n", this.getClass().getSimpleName());
        //            }
        //        }
        return true;
    }

    // TODO: is this needed?
    private void printMCParticles(List<MCParticle> mcParticles) {
        System.out.printf("%s: printMCParticles \n", this.getClass().getSimpleName());
        System.out.printf("%s: %d mc particles \n", this.getClass().getSimpleName(), mcParticles.size());
        for (MCParticle mcp : mcParticles) {
            if (mcp.getGeneratorStatus() != MCParticle.FINAL_STATE) {
                continue;
            }
            System.out.printf("\n%s: (%s) %d  p %s org %s  %s \n", this.getClass().getSimpleName(), mcp.getGeneratorStatus() == MCParticle.FINAL_STATE ? "F" : "I", mcp.getPDGID(), mcp.getMomentum().toString(), mcp.getOrigin().toString(), mcp.getParents().size() > 0 ? "parents:" : "");
            for (MCParticle parent : mcp.getParents()) {
                System.out.printf("%s:       (%s) %d  p %s org %s %s \n", this.getClass().getSimpleName(), parent.getGeneratorStatus() == MCParticle.FINAL_STATE ? "F" : "I", parent.getPDGID(), parent.getMomentum().toString(), parent.getOrigin().toString(), parent.getParents().size() > 0 ? "parents:" : "");
                for (MCParticle grparent : parent.getParents()) {
                    System.out.printf("%s:            (%s) %d  p %s org %s  %s \n", this.getClass().getSimpleName(), grparent.getGeneratorStatus() == MCParticle.FINAL_STATE ? "F" : "I", grparent.getPDGID(), grparent.getMomentum().toString(), grparent.getOrigin().toString(), grparent.getParents().size() > 0 ? "parents:" : "");
                }

            }
        }
    }
}
