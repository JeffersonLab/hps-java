package org.hps.recon.tracking.gbl;

import hep.physics.matrix.BasicMatrix;
import hep.physics.matrix.MatrixOp;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math.geometry.Rotation;
import org.apache.commons.math.geometry.Vector3D;
import org.hps.recon.tracking.CoordinateTransformations;
import org.hps.recon.tracking.MaterialSupervisor;
import org.hps.recon.tracking.MultipleScattering;
import org.hps.recon.tracking.MultipleScattering.ScatterPoints;
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
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
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
    private boolean hasXPlanes = false;
    private boolean addBeamspot = false;
    private double beamspotTiltZOverY = 0; // Math.PI/180* 15;
    private double beamspotScatAngle = 0.000001;
    // beam spot, in tracking frame
    private double beamspotWidthZ = 0.05;
    private double beamspotWidthY = 0.150;
    private double beamspotPosition[] = { 0, 0, 0 };
    // human readable ID for beam spot
    private final int iBeamspotHit = -1;

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

    public void setDebug(int debug) {
        _debug = debug;
        _scattering.setDebug((_debug > 0));
    }

    public void buildModel(Detector detector) {
        materialManager.buildModel(detector);
    }

    public void setAddBeamspot(boolean add) {
        this.addBeamspot = add;
    }

    public void setBeamspotScatAngle(double beamspotScatAngle) {
        this.beamspotScatAngle = beamspotScatAngle;
    }

    public void setBeamspotWidthZ(double beamspotWidthZ) {
        this.beamspotWidthZ = beamspotWidthZ;
    }

    public void setBeamspotWidthY(double beamspotWidthY) {
        this.beamspotWidthY = beamspotWidthY;
    }

    public void setBeamspotTiltZOverY(double beamspotTiltZOverY) {
        this.beamspotTiltZOverY = beamspotTiltZOverY;
    }

    public void setBeamspotPosition(double[] beamspotPosition) {
        this.beamspotPosition = beamspotPosition;
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

    void setXPlaneFlag(boolean flag) {
        this.hasXPlanes = flag;
    }

    public Hep3Vector get_B() {
        return bFieldVector;
    }

    public void set_B(Hep3Vector _B) {
        this.bFieldVector = _B;
    }

    private MCParticle doMCprocessing(List<MCParticle> mcParticles, MCParticle mcp) {
        MCParticle ap = null;

        // check if this is an A' event
        for (MCParticle part : mcParticles) {
            if (Math.abs(part.getPDGID()) == 622) {
                ap = part;
                break;
            }
        }

        if (mcp == null) {
            System.out.printf("%s: WARNING!! no truth particle found in event!\n", this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            // System.exit(1);
        } else {
            if (_debug > 0)
                System.out.printf("%s: truth particle (pdgif %d ) found in event!\n", this.getClass().getSimpleName(), mcp.getPDGID());

            // If this is an A' event, do some more checks
            if (ap != null) {
                // A few MC files have broken links b/w parents-daughters
                // This causes the MC particle to come from the origin even if the decay happen somewhere else
                if (this.getAprimeDecayProducts(mcParticles).size() > 0) {
                    // do a full check
                    checkAprimeTruth(mcp, mcParticles);
                }
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
        BasicMatrix clCov = GblUtils.unitMatrix(5, 5);
        clCov = (BasicMatrix) MatrixOp.mult(0.1 * 0.1, clCov);
        if (textFile != null) {
            textFile.printPerTrackParam(gtd.getPerigeeTrackParameters());
            textFile.printPerTrackParamTruth(perParTruth);
            textFile.printClTrackParam(new GblUtils.ClParams(htf, bFieldVector.z()));
            if (htfTruth != null)
                textFile.printClTrackParamTruth(new GblUtils.ClParams(htfTruth, bFieldVector.z()));
            textFile.printPerToClPrj(gtd.getPrjPerToCl());
            textFile.printChi2(htf.chisq(), htf.ndf());
            textFile.printPerTrackCov(htf);
            textFile.printCLTrackCov(clCov);
        }
    }

    void printGBL(Track trk, List<SiTrackerHitStrip1D> stripHits, GBLTrackData gtd, List<GBLStripClusterData> stripClusterDataList, List<MCParticle> mcParticles, List<SimTrackerHit> simTrackerHits, boolean isMC) {

        HelicalTrackFit htf = TrackUtils.getHTF(trk);

        // Find scatter points along the path
        ScatterPoints scatters = _scattering.FindHPSScatterPoints(htf);

        // Hits on track
        List<TrackerHit> hits = trk.getTrackerHits();

        // Find the truth particle of the track
        MCParticle mcp = null;
        MCParticle ap = null;
        HelicalTrackFit htfTruth = null;
        GblUtils.PerigeeParams perParTruth = null;
        Map<Integer, SimTrackerHit> simHitsLayerMap = null;
        if (isMC) {
            mcp = TrackUtils.getMatchedTruthParticle(trk);
            simHitsLayerMap = new HashMap<Integer, SimTrackerHit>();
            if (mcp != null) {
                ap = doMCprocessing(mcParticles, mcp);
                htfTruth = getHTFtruth(mcp, ap);
                perParTruth = new GblUtils.PerigeeParams(htfTruth, bFieldVector.z());

                // build map of layer to SimTrackerHits that belongs to the MC particle
                for (SimTrackerHit sh : simTrackerHits) {
                    if (sh.getMCParticle() == mcp) {
                        int layer = sh.getIdentifierFieldValue("layer");
                        if (!simHitsLayerMap.containsKey(layer) || (sh.getPathLength() < simHitsLayerMap.get(layer).getPathLength())) {
                            simHitsLayerMap.put(layer, sh);
                        }
                    }
                }
            }
        }

        // Get track parameters from MC particle
        doTrackParams(gtd, htf, htfTruth, perParTruth);

        int istrip = 0;
        int beamSpotMillepedeId = 98; // just a random int number that I came up with

        for (int ihit = -1; ihit != hits.size(); ++ihit) {

            HelicalTrackHit hit = null;
            HelicalTrackCross htc = null;
            List<HelicalTrackStrip> strips;
            Hep3Vector correctedHitPosition = null;

            // Add beamspot first
            if (ihit == iBeamspotHit) {
                if (addBeamspot) {
                    strips = this.getBeamSpotHits(TrackUtils.isTopTrack(trk, 4));
                    correctedHitPosition = new BasicHep3Vector(0, 0, 0);
                } else
                    continue;
            } else {
                hit = (HelicalTrackHit) hits.get(ihit);
                htc = (HelicalTrackCross) hit;
                strips = htc.getStrips();
                correctedHitPosition = hit.getCorrectedPosition();
            }

            for (HelicalTrackStrip stripOld : strips) {

                // Use a different class that overrides u- and v- vectors to avoid problems here for the beamspot
                HelicalTrackStripGbl strip;
                if (ihit == iBeamspotHit)
                    strip = new BeamspotHelicalTrackStrip(stripOld, true);
                else
                    strip = new HelicalTrackStripGbl(stripOld, true);

                String sensorName = null;
                int millepedeId = 0;
                HpsSiSensor sensor = null;
                MultipleScattering.ScatterPoint temp = null;
                GBLStripClusterData stripData = null;

                if (ihit == iBeamspotHit) {
                    millepedeId = beamSpotMillepedeId++;
                    sensorName = strip.getStrip().detector();
                    stripData = new GBLStripClusterData(millepedeId);
                    stripData.setScatterAngle(beamspotScatAngle);
                } else {
                    IDetectorElement de = ((RawTrackerHit) strip.getStrip().rawhits().get(0)).getDetectorElement();
                    if (!(de instanceof HpsSiSensor))
                        throw new ClassCastException("Detector element " + de.getName() + " couldn't be casted to " + HpsSiSensor.class.getName());
                    else
                        sensor = (HpsSiSensor) de;
                    sensorName = sensor.getName();
                    millepedeId = sensor.getMillepedeId();
                    temp = MakeGblTracks.getScatterPointGbl(sensor, strip, htf, scatters, _scattering, bFieldVector.z());
                    stripData = MakeGblTracks.makeStripData(sensor, strip, htf, temp);
                }

                if (textFile != null) {
                    textFile.printStrip(istrip, millepedeId, sensorName);
                    textFile.printOrigin(strip.origin());
                    textFile.printHitPos3D(correctedHitPosition);
                    textFile.printStripTrackPos(temp.getPosition());
                    textFile.printStripPathLen(stripData.getPath());
                    textFile.printStripPathLen3D(stripData.getPath3D());
                    textFile.printMeasDir(stripData.getU());
                    textFile.printNonMeasDir(stripData.getV());
                    textFile.printNormalDir(stripData.getW());
                    textFile.printStripTrackDir(Math.sin(stripData.getTrackPhi()), Math.sin(stripData.getTrackLambda()));
                    textFile.printStripTrackDirFull(stripData.getTrackDirection());
                    textFile.printStripMeas(stripData.getMeas());
                    textFile.printStripMeasRes(stripData.getMeas() - stripData.getTrackPos().x(), stripData.getMeasErr());
                    textFile.printStripScat(stripData.getScatterAngle());
                }

                // cross-check intercept point
                if (hasXPlanes) {
                    Hep3Vector trkposXPlaneIter = TrackUtils.getHelixPlanePositionIter(htf, strip.origin(), strip.w(), 1.0e-8);
                    Hep3Vector trkposDiff = VecOp.sub(trkposXPlaneIter, temp.getPosition());
                    if (trkposDiff.magnitude() > 1.0e-7) {
                        System.out.printf("WARNING trkposDiff mag = %.10f [%.10f %.10f %.10f]\n", trkposDiff.magnitude(), trkposDiff.x(), trkposDiff.y(), trkposDiff.z());
                        System.exit(1);
                    }
                    if (_debug > 0) {
                        System.out.printf("trkposXPlaneIter at intercept [%.10f %.10f %.10f]\n", trkposXPlaneIter.x(), trkposXPlaneIter.y(), trkposXPlaneIter.z());
                    }
                }

                //                if (isMC) {
                //                    if (simHit == null && ihit != iBeamspotHit) {
                //                        System.out.printf("%s: no sim hit for strip hit at layer %d\n", this.getClass().getSimpleName(), strip.layer());
                //                        System.out.printf("%s: it as %d mc particles associated with it:\n", this.getClass().getSimpleName(), hitMCParticles.size());
                //                        for (MCParticle particle : hitMCParticles) {
                //                            System.out.printf("%s: %d p %s \n", this.getClass().getSimpleName(), particle.getPDGID(), particle.getMomentum().toString());
                //                        }
                //                        System.out.printf("%s: these are sim hits in the event:\n", this.getClass().getSimpleName());
                //                        for (SimTrackerHit simhit : simTrackerHits) {
                //                            System.out.printf("%s sim hit at %s with MC particle pdgid %d with p %s \n", this.getClass().getSimpleName(), simhit.getPositionVec().toString(), simhit.getMCParticle().getPDGID(), simhit.getMCParticle().getMomentum().toString());
                //                        }
                //                    }
                //
                //                }

                // calculate strip isolation if we are printing a text file or debugging
                // Don't do this for the beam spot strip
                doStripIso(stripHits, strip, sensorName, ihit);

                // truth residuals
                Hep3Matrix trkToStripRot = null;
                if (ihit == iBeamspotHit)
                    trkToStripRot = this.getTrkToStripRot(strip);
                else
                    trkToStripRot = trackerHitUtils.getTrackToStripRotation(strip.getStrip());

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
                if (isMC) {
                    SimTrackerHit simHit = simHitsLayerMap.get(strip.layer());
                    if (simHit != null) {
                        Hep3Vector simHitPos = CoordinateTransformations.transformVectorToTracking(simHit.getPositionVec());
                        if (_debug > 0) {
                            System.out.printf("simHitPos  %s\n", simHitPos.toString());
                        }
                        Hep3Vector vdiffSimHit = VecOp.sub(simHitPos, temp.getPosition());
                        Hep3Vector simHitPos_meas = VecOp.mult(trkToStripRot, vdiffSimHit);
                        printSimHitPos = simHitPos_meas.x();
                        printMeasErr = stripData.getMeasErr();
                    }
                }
                if (textFile != null) {
                    textFile.printStripMeasResSimHit(printSimHitPos, printMeasErr);
                }

                ++istrip;
            }
        }

    }

    private void doStripIso(List<SiTrackerHitStrip1D> stripHits, HelicalTrackStripGbl strip, String sensorName, int ihit) {
        double stripIsoMin = 9999.9;
        if ((_debug > 0 || textFile != null) && ihit != iBeamspotHit) {
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

    /**
     * Make a pair of HelicalTrackStrips from the beam spot.
     */
    private List<HelicalTrackStrip> getBeamSpotHits(boolean isTopTrack) {

        // dummy constants
        final double time = 0;
        final int lyr = 0;
        final BarrelEndcapFlag be = BarrelEndcapFlag.BARREL;

        // width of strip?
        final double vmin = -200;
        final double vmax = 200;

        // measurement in local sensor frame
        final double umeas = 0;

        // calculate stereo angle to give approximately the right horizontal resolution
        final double stereo_angle = Math.asin(beamspotWidthZ / beamspotWidthY);

        // Place stereo sensor origin
        Hep3Vector posStereo = new BasicHep3Vector(beamspotPosition[0], beamspotPosition[1], beamspotPosition[2]);
        // Place axial sensor translated along beam line
        Hep3Vector posAxial = VecOp.add(posStereo, new BasicHep3Vector(0.1, 0, 0));

        // Set the local coordinates of the sensors
        // make these similar orientation as bottom L1-3 modules

        // Start with u pointing in tracking z direction
        Vector3D u_start = new Vector3D(0, 0, 1);

        // AXIAL u
        // flip around tracking y to get correct u
        Rotation r1 = new Rotation(new Vector3D(0, 1, 0), Math.PI);
        // then rotate around X to get correct beamspot tilt
        Rotation r11 = new Rotation(new Vector3D(1, 0, 0), beamspotTiltZOverY);
        Vector3D uAxial_v = r11.applyTo(r1).applyTo(u_start);

        // AXIAL v
        // v is orthogonal to u around x
        Rotation r2 = new Rotation(new Vector3D(1, 0, 0), Math.PI / 2.0);
        Vector3D vAxial_v = r2.applyTo(uAxial_v);

        // STEREO u
        // flip around x w.r.t. axial and then apply stereo angle
        // that is don't do the initial flip
        Rotation r3 = new Rotation(new Vector3D(1, 0, 0), stereo_angle);
        // first apply the beamspot tilt, then the stereo around the same axis
        Vector3D uStereo_v = r3.applyTo(r11).applyTo(u_start);

        // STEREO v
        // v is orthogonal to u, negative compared to u above around new
        Rotation r22 = new Rotation(new Vector3D(1, 0, 0), -Math.PI / 2.0);
        Vector3D vStereo_v = r22.applyTo(uStereo_v);

        // convert to Hep3Vector
        Hep3Vector uStereo = new BasicHep3Vector(uStereo_v.getX(), uStereo_v.getY(), uStereo_v.getZ());
        Hep3Vector vStereo = new BasicHep3Vector(vStereo_v.getX(), vStereo_v.getY(), vStereo_v.getZ());
        Hep3Vector uAxial = new BasicHep3Vector(uAxial_v.getX(), uAxial_v.getY(), uAxial_v.getZ());
        Hep3Vector vAxial = new BasicHep3Vector(vAxial_v.getX(), vAxial_v.getY(), vAxial_v.getZ());

        // Create the actual strip hit objects
        String sensorNameAxial = isTopTrack ? "module_L0t_halfmodule_axial_sensor0" : "module_L0b_halfmodule_axial_sensor0";
        String sensorNameStereo = isTopTrack ? "module_L0t_halfmodule_stereo_sensor0" : "module_L0b_halfmodule_stereo_sensor0";
        NormalHelicalTrackStrip hitAxial = new NormalHelicalTrackStrip(posAxial, uAxial, vAxial, umeas, beamspotWidthZ, vmin, vmax, 0.0, time, null, sensorNameAxial, lyr, be);
        NormalHelicalTrackStrip hitStereo = new NormalHelicalTrackStrip(posStereo, uStereo, vStereo, umeas, beamspotWidthZ, vmin, vmax, 0.0, time, null, sensorNameStereo, lyr, be);

        if (_debug > 0) {
            System.out.printf("%s: created beamspot strip hits\n", this.getClass().getSimpleName());
            System.out.printf("%s: %s\n", this.getClass().getSimpleName(), hitStereo.toString());
            System.out.printf("%s: %s\n", this.getClass().getSimpleName(), hitAxial.toString());
        }

        List<HelicalTrackStrip> htsList = new ArrayList<HelicalTrackStrip>();
        htsList.add(hitStereo);
        htsList.add(hitAxial);

        return htsList;

    }

    private Hep3Matrix getTrkToStripRot(HelicalTrackStripGbl strip) {
        Hep3Matrix prjTrkToStrip = new BasicHep3Matrix(strip.u().x(), strip.u().y(), strip.u().z(), strip.v().x(), strip.v().y(), strip.v().z(), strip.w().x(), strip.w().y(), strip.w().z());
        return prjTrkToStrip;

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

    private void checkAprimeTruth(MCParticle mcp, List<MCParticle> mcParticles) {

        List<MCParticle> mcp_pair = getAprimeDecayProducts(mcParticles);

        if (mcp_pair.size() != 2) {
            System.out.printf("%s: ERROR this event has %d mcp with 622 as parent!!??  \n", this.getClass().getSimpleName(), mcp_pair.size());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }
        if (Math.abs(mcp_pair.get(0).getPDGID()) != 11 || Math.abs(mcp_pair.get(1).getPDGID()) != 11) {
            System.out.printf("%s: ERROR decay products are not e+e-? \n", this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }
        if (mcp_pair.get(0).getPDGID() * mcp_pair.get(1).getPDGID() > 0) {
            System.out.printf("%s: ERROR decay products have the same sign? \n", this.getClass().getSimpleName());
            this.printMCParticles(mcParticles);
            System.exit(1);
        }

        // cross-check
        if (!mcp_pair.contains(mcp)) {
            boolean hasBeamElectronParent = false;
            for (MCParticle parent : mcp.getParents()) {
                if (parent.getGeneratorStatus() != MCParticle.FINAL_STATE && parent.getPDGID() == 11 && parent.getMomentum().y() == 0.0 && Math.abs(parent.getMomentum().magnitude() - _beamEnergy) < 0.01) {
                    hasBeamElectronParent = true;
                }
            }
            if (!hasBeamElectronParent) {
                System.out.printf("%s: the matched MC particle is not an A' daughter and not a the recoil electrons!?\n", this.getClass().getSimpleName());
                System.out.printf("%s: %s %d p %s org %s\n", this.getClass().getSimpleName(), mcp.getGeneratorStatus() == MCParticle.FINAL_STATE ? "F" : "I", mcp.getPDGID(), mcp.getMomentum().toString(), mcp.getOrigin().toString());
                printMCParticles(mcParticles);
                System.exit(1);
            } else if (_debug > 0) {
                System.out.printf("%s: the matched MC particle is the recoil electron\n", this.getClass().getSimpleName());
            }
        }
    }

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

    /**
     * {@link HelicalTrackStripGbl} that explicitly uses the given unit vectors when accessed. This class is used when
     * there is no strip geometry objects assoviated and thus the parent {@link HelicalTrackStripGbl} methods cannot be
     * used to get unit vectors. Make sure this strip uses the given u and v vectors all the time.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    private static class BeamspotHelicalTrackStrip extends HelicalTrackStripGbl {

        public BeamspotHelicalTrackStrip(HelicalTrackStrip strip, boolean useGeomDef) {
            super(strip, useGeomDef);
        }

        @Override
        public Hep3Vector u() {
            return _strip.u();
        }

        @Override
        public Hep3Vector v() {
            return _strip.v();
        }
    }

    /**
     * {@link HelicalTrackStrip} that doesn't flip unit vectors to point along the track.
     * 
     * @author Per Hansson Adrian <phansson@slac.stanford.edu>
     */
    private static class NormalHelicalTrackStrip extends HelicalTrackStrip {

        public NormalHelicalTrackStrip(Hep3Vector origin, Hep3Vector u, Hep3Vector v, double umeas, double du, double vmin, double vmax, double dEdx, double time, List rawhits, String detector, int layer, org.lcsim.geometry.subdetector.BarrelEndcapFlag beflag) {
            super(origin, u, v, umeas, du, vmin, vmax, dEdx, time, rawhits, detector, layer, beflag);

        }

        @Override
        protected void initW() {
            _w = VecOp.cross(_u, _v);
        }
    }

}
