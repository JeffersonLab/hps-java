package org.hps.analysis.alignment;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Matrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hps.recon.ecal.cluster.ClusterUtilities;
import org.hps.recon.tracking.TrackData;
import org.hps.recon.tracking.TrackType;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.vertexing.BilliorTrack;
import org.hps.recon.vertexing.BilliorVertex;
import org.hps.recon.vertexing.BilliorVertexer;
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
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackState;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseTrackState;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class FeeSvtAlignment2019Driver extends Driver {

    boolean debug = true;
    private AIDA aida = AIDA.defaultInstance();

    private String finalStateParticlesColName = "FinalStateParticles";

    Set<String> finalStateParticleCollections = new HashSet<String>();
    private Double _beamEnergy = 4.55;
    private double _percentFeeCut = 0.8;
    private final BasicHep3Matrix beamAxisRotation = new BasicHep3Matrix();

    //Set min seed energy value, default to 2015 run 
    private double seedCut = 0.0; //= 0.4

    //set min cluster energy value, default to 2015 run
    private double clusterCut = 0.2;

    //minimum number of hits per cluster
    private int minHits = 3;

    double ctMin = 40.;
    double ctMax = 49.;

    double thetaXmin = -0.05;
    double thetaXmax = 0.05;

    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    private boolean _alignit = false;

    protected double bField;
    // flipSign is a kludge...
    // HelicalTrackFitter doesn't deal with B-fields in -ive Z correctly
    // so we set the B-field in +iveZ and flip signs of fitted tracks
    //
    // Note: This should be -1 for test run configurations and +1 for
    // prop-2014 configurations
    private int flipSign = 1;

    List<ReconstructedParticle> _topElectrons = new ArrayList<ReconstructedParticle>();
    List<ReconstructedParticle> _bottomElectrons = new ArrayList<ReconstructedParticle>();

    List<BilliorTrack> _topBTracks = new ArrayList<BilliorTrack>();
    List<BilliorTrack> _bottomBTracks = new ArrayList<BilliorTrack>();

    protected void detectorChanged(Detector detector) {
        beamAxisRotation.setActiveEuler(Math.PI / 2, -0.0305, -Math.PI / 2);
        // Set the magnetic field parameters to the appropriate values.
        Hep3Vector ip = new BasicHep3Vector(0., 0., 500.0);
//        _fieldmap = detector.getFieldMap();
//        double[] pos = new double[3];
//        double[] field = new double[3];
//        for (int i = 0; i < 2000; ++i) {
//            pos[2] = -50. + i;
//            _fieldmap.getField(pos, field);
//            System.out.println(pos[0]+" "+pos[1]+" "+pos[2]+" "+field[0]+" "+field[1]+" "+field[2]);
//            aida.cloud1D("bx").fill(pos[2], field[0]);
//            aida.cloud1D("by").fill(pos[2], field[1]);
//            aida.cloud1D("bz").fill(pos[2], field[2]);
//        }
        bField = detector.getFieldMap().getField(ip).y();
        System.out.println("bfield " + bField);
        System.out.println("bfield @ origin " + detector.getFieldMap().getField(new BasicHep3Vector(0., 0., 0.)).y());
        System.out.println("bfield @ z=-5. " + detector.getFieldMap().getField(new BasicHep3Vector(0., 0., -5.0)).y());
        if (bField < 0) {
            flipSign = -1;
        }
        finalStateParticleCollections.add(finalStateParticlesColName);
    }

    protected void process(EventHeader event) {

        // only keep events with one and only one cluster
        List<Cluster> ecalClusters = event.get(Cluster.class, "EcalClustersCorr");
        int nClusters = ecalClusters.size();
        if (ecalClusters.size() != 1) {
            return;
        }
        for (String s : finalStateParticleCollections) {
            List<ReconstructedParticle> rpList = event.get(ReconstructedParticle.class, s);
            setupSensors(event);
            hitToStrips = TrackUtils.getHitToStripsTable(event);
            hitToRotated = TrackUtils.getHitToRotatedTable(event);
            for (ReconstructedParticle rp : rpList) {
                if (!TrackType.isGBL(rp.getType())) {
                    continue;
                }
                if (rp.getMomentum().magnitude() > 1.5 * _beamEnergy) {
                    continue;
                }
//                // require both track and cluster
//                if (rp.getClusters().size() != 1) {
//                    continue;
//                }

                if (rp.getTracks().size() != 1) {
                    continue;
                }

                Track t = rp.getTracks().get(0);
                Hep3Vector pmom = rp.getMomentum();
                double p = rp.getMomentum().magnitude();
                Cluster c = ecalClusters.get(0);
                if (c.getPosition()[1] > 0.) {
                    aida.histogram1D("Top cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                } else {
                    aida.histogram1D("Bottom cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                }
                aida.histogram2D("Cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);

                CalorimeterHit seedHit = ClusterUtilities.findSeedHit(c);
                boolean isFiducial = isFiducial(seedHit);
                // debug diagnostics to set cuts
                if (debug) {
                    aida.cloud1D("clusterSeedHit energy").fill(ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                    aida.cloud1D("cluster nHits").fill(c.getCalorimeterHits().size());
                    aida.cloud2D("clusterSeedHit energy vs p").fill(p, ClusterUtilities.findSeedHit(c).getCorrectedEnergy());
                    aida.cloud2D("cluster nHits vs p").fill(p, c.getCalorimeterHits().size());
                    aida.cloud2D("cluster time vs p").fill(p, ClusterUtilities.getSeedHitTime(c));
                }
                double ct = ClusterUtilities.getSeedHitTime(c);
                if (c.getEnergy() > clusterCut
                        && ClusterUtilities.findSeedHit(c).getCorrectedEnergy() > seedCut
                        && c.getCalorimeterHits().size() >= minHits
                        && ct > ctMin
                        && ct < ctMax) {
                    double chiSquared = t.getChi2();
                    int ndf = t.getNDF();
                    double chi2Ndf = t.getChi2() / t.getNDF();
                    double chisqProb = ChisqProb.gammp(ndf, chiSquared);
                    int nHits = t.getTrackerHits().size();
                    double dEdx = t.getdEdx();
                    double e1 = rp.getEnergy();
                    double p1 = rp.getMomentum().magnitude();

                    double d0 = t.getTrackStates().get(0).getD0();
                    double z0 = t.getTrackStates().get(0).getZ0();
                    double thetaY = Math.asin(pmom.y() / pmom.magnitude());

                    Hep3Vector p1mom = rp.getMomentum();
                    //rotate into physiscs frame of reference
                    Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
                    double theta = Math.acos(rprot.z() / rprot.magnitude());

                    // debug diagnostics to set cuts
                    String topOrBottom = isTopTrack(t) ? " top " : " bottom ";
                    if (debug) {
                        aida.histogram1D("Track chisq per df" + topOrBottom, 100, 0., 12.).fill(chiSquared / ndf);
                        aida.histogram1D("Track chisq prob" + topOrBottom, 100, 0., 1.).fill(chisqProb);
                        aida.histogram1D("Track nHits" + topOrBottom, 7, 0.5, 7.5).fill(t.getTrackerHits().size());
                        aida.histogram1D("Track momentum" + topOrBottom, 100, 0., 10.0).fill(p);
                        aida.histogram1D("Track deDx" + topOrBottom, 100, 0.00004, 0.00013).fill(t.getdEdx());
                        aida.histogram1D("Track theta" + topOrBottom, 100, 0.010, 0.160).fill(theta);
                        aida.cloud2D("Track theta vs p" + topOrBottom).fill(theta, p);
                        aida.histogram1D("rp x0" + topOrBottom, 100, -0.20, 0.20).fill(TrackUtils.getX0(t));
                        aida.histogram1D("rp y0" + topOrBottom, 100, -2.0, 2.0).fill(TrackUtils.getY0(t));
                        aida.histogram1D("rp z0" + topOrBottom, 100, -0.5, 0.5).fill(TrackUtils.getZ0(t));
                    }
                    double trackDataTime = TrackData.getTrackTime(TrackData.getTrackData(event, t));
                    if (debug) {
                        aida.cloud1D("track data time").fill(trackDataTime);
                    }
                    analyzeHitlayers(rp);
                    if (isTopTrack(t)) {
                        if (nHits == 5) {
                            aida.histogram1D("Fee top 5-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                        } else if (nHits == 6) {

                            _topElectrons.add(rp);
                            aida.histogram1D("Fee top 6-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                            if (nClusters == 1) {
                                aida.histogram1D("Fee top 6-hit track single cluster momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                                aida.histogram1D("Fee top 6-hit track single cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                                if (isFiducial) {
                                    aida.histogram2D("Fee 6-hit track single fiducial cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
                                    aida.histogram1D("Fee top 6-hit track single fiducial cluster momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                                    aida.histogram1D("Fee top 6-hit track single fiducial cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                                }
                                aida.histogram2D("Fee 6-hit track single cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
                            }
                            aida.histogram1D("Fee top 6-hit track d0", 100, -2.0, 2.0).fill(d0);
                            aida.profile1D("Fee top 6-hit track p vs d0 profile", 100, 0.75, 1.25).fill(p1mom.magnitude(), d0);
                            aida.histogram2D("Fee top 6-hit track p vs d0", 100, 0.75, 1.25, 100, -2.0, 2.0).fill(p1mom.magnitude(), d0);
                            aida.histogram1D("Fee top 6-hit track z0", 100, -0.6, 0.6).fill(z0);
                            aida.profile1D("Fee top 6-hit track thetaY vs z0 profile", 10, 0.024, 0.054).fill(thetaY, z0);
                            aida.histogram2D("Fee top 6-hit track thetaY vs z0", 100, 0.015, 0.055, 100, -0.6, 0.6).fill(thetaY, z0);
                            aida.cloud1D("Top Track theta").fill(theta);
                            aida.cloud2D("Top Track theta vs p").fill(theta, p);
                            aida.cloud1D("Top rp x0").fill(TrackUtils.getX0(t));
                            aida.cloud1D("Top rp y0").fill(TrackUtils.getY0(t));
                            aida.cloud1D("Top rp z0").fill(TrackUtils.getZ0(t));
//                            double[] strips = stripClusterSizes(t);
//                            // let's cut real hard here, require 2-strip clusters in all four first layers
                            boolean keepit = true;
//                            for (double d : strips) {
//                                if (d != 2) {
//                                    keepit = false;
//                                }
//                            }
                            if (keepit) {
                                _topBTracks.add(toBilliorTrack(t));
                                aida.profile1D("Fee tight top 6-hit track thetaY vs z0 profile", 10, 0.024, 0.054).fill(thetaY, z0);
                                aida.histogram2D("Fee tight top 6-hit track thetaY vs z0", 100, 0.015, 0.055, 100, -0.6, 0.6).fill(thetaY, z0);
                            }
                        }
                    } else {
                        if (nHits == 5) {
                            aida.histogram1D("Fee bottom 5-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                        } else if (nHits >= 6) {

                            _bottomElectrons.add(rp);
                            aida.histogram1D("Fee bottom " + nHits + "-hit track momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                            if (nClusters == 1) {
                                aida.histogram1D("Fee bottom " + nHits + "-hit track single cluster momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                                aida.histogram1D("Fee bottom " + nHits + "-hit track single cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                                if (isFiducial) {
                                    aida.histogram2D("Fee " + nHits + "-hit track single fiducial cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
                                    aida.histogram1D("Fee bottom " + nHits + "-hit track single fiducial cluster momentum", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(p);
                                    aida.histogram1D("Fee bottom " + nHits + "-hit track single fiducial cluster energy", 100, 0.5 * _beamEnergy, 1.5 * _beamEnergy).fill(c.getEnergy());
                                }
                                aida.histogram2D("Fee " + nHits + "-hit track single cluster x vs y", 200, -200., 200., 100, -100., 100.).fill(c.getPosition()[0], c.getPosition()[1]);
                            }
                            aida.histogram1D("Fee bottom " + nHits + "-hit track d0", 100, -2.0, 2.0).fill(d0);
                            aida.profile1D("Fee bottom " + nHits + "-hit track p vs d0 profile", 100, 0.75, 1.25).fill(p1mom.magnitude(), d0);
                            aida.histogram2D("Fee bottom " + nHits + "-hit track p vs d0", 100, 0.75, 1.25, 100, -2.0, 2.0).fill(p1mom.magnitude(), d0);
                            aida.histogram1D("Fee bottom " + nHits + "-hit track z0", 100, -0.8, 0.2).fill(z0);
                            aida.histogram1D("Fee bottom " + nHits + "-hit track thetaY", 100, -0.06, 0.).fill(thetaY);
                            aida.profile1D("Fee bottom " + nHits + "-hit track thetaY vs z0 profile", 10, -0.054, -0.024).fill(thetaY, z0);
                            aida.profile1D("Fee bottom " + nHits + "-hit track thetaY vs z0 profile zoom", 10, -0.035, -0.024).fill(thetaY, z0);
                            aida.histogram2D("Fee bottom " + nHits + "-hit track thetaY vs z0", 100, -0.055, -0.015, 100, -0.8, 0.8).fill(thetaY, z0);
                            aida.cloud1D("Bottom " + nHits + "-hit Track theta").fill(theta);
                            aida.cloud2D("Bottom " + nHits + "-hit Track theta vs p").fill(theta, p);
                            aida.cloud1D("Bottom " + nHits + "-hit rp x0").fill(TrackUtils.getX0(t));
                            aida.cloud1D("Bottom " + nHits + "-hitrp y0").fill(TrackUtils.getY0(t));
                            aida.cloud1D("Bottom " + nHits + "-hitrp z0").fill(TrackUtils.getZ0(t));
                            double[] strips = stripClusterSizes(t);
                            // let's cut real hard here, require 2-strip clusters in all four first layers
                            // Let's not (at least for now) because the slim layers only have one strip per hit...
                            boolean keepit = true;
//                            for (double d : strips) {
//                                if (d != 2) {
//                                    keepit = false;
//                                }
//                            }
                            if (keepit) {
                                _bottomBTracks.add(toBilliorTrack(t));
                                aida.profile1D("Fee tight bottom " + nHits + "-hit track thetaY vs z0 profile", 10, -0.054, -0.024).fill(thetaY, z0);
                                aida.histogram2D("Fee tight bottom " + nHits + "-hit track thetaY vs z0", 100, -0.055, -0.015, 100, -0.6, 0.6).fill(thetaY, z0);
                            }
                        }
//                        List<TrackerHit> hits = t.getTrackerHits();
//                        for (TrackerHit h : hits) {
//                            RawTrackerHit rth = (RawTrackerHit) h;
//                            if (sensor.isTopLayer()) {
//                                n[0] += 1;
//                            } else {
//                                n[1] += 1;
//                            }
//                        }
                    }
                    if (_alignit) {
                        if (nHits >= 6) {
                            alignit(rp);
                        }
                    }
                }// end of cluster cuts
            }// end of loop over ReconstructedParticles in this event
        }// end of loop over electron collections
        // can't accumulate over all the data as we run out of memory
        // stop every once in a while and process what we have, then release the memory
        if (_topElectrons.size() >= 2) {
            //vertex top electrons
            BilliorVertex vtx = fitVertex(_topElectrons.get(0), _topElectrons.get(1));
            aida.histogram1D("top vertex x position", 100, -3., 3.).fill(vtx.getPosition().x());
            aida.histogram1D("top vertex y position", 100, -3., 3.).fill(vtx.getPosition().y());
            aida.histogram2D("top vertex x vs y position", 100, -3., 3., 100, -3., 3.).fill(vtx.getPosition().x(), vtx.getPosition().y());
            aida.histogram1D("top vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
            _topElectrons.remove(1);
            _topElectrons.remove(0);
        }
        if (_bottomElectrons.size() >= 2) {
            //vertex top electrons
            BilliorVertex vtx = fitVertex(_bottomElectrons.get(0), _bottomElectrons.get(1));
            aida.histogram1D("bottom vertex x position", 100, -3., 3.).fill(vtx.getPosition().x());
            aida.histogram1D("bottom vertex y position", 100, -3., 3.).fill(vtx.getPosition().y());
            aida.histogram2D("bottom vertex x vs y position", 100, -3., 3., 100, -3., 3.).fill(vtx.getPosition().x(), vtx.getPosition().y());
            aida.histogram1D("bottom vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
            _bottomElectrons.remove(1);
            _bottomElectrons.remove(0);
        }

        // can't accumulate over all the data as we run out of memory
        // stop every once in a while and process what we have, then release the memory
        if (_topBTracks.size() >= 2) {
            //vertex top electrons
            BilliorVertex vtx = fitVertex(_topBTracks.get(0), _topBTracks.get(1));
            aida.histogram1D("tight top vertex x position", 100, -3., 3.).fill(vtx.getPosition().x());
            aida.histogram1D("tight top vertex y position", 100, -3., 3.).fill(vtx.getPosition().y());
            aida.histogram2D("tight top vertex x vs y position", 100, -3., 3., 100, -3., 3.).fill(vtx.getPosition().x(), vtx.getPosition().y());
            aida.histogram1D("tight top vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
            _topBTracks.remove(1);
            _topBTracks.remove(0);
        }
        if (_bottomBTracks.size() >= 2) {
            //vertex top electrons
            BilliorVertex vtx = fitVertex(_bottomBTracks.get(0), _bottomBTracks.get(1));
            aida.histogram1D("tight bottom vertex x position", 100, -3., 3.).fill(vtx.getPosition().x());
            aida.histogram1D("tight bottom vertex y position", 100, -3., 3.).fill(vtx.getPosition().y());
            aida.histogram2D("tight bottom vertex x vs y position", 100, -3., 3., 100, -3., 3.).fill(vtx.getPosition().x(), vtx.getPosition().y());
            aida.histogram1D("tight bottom vertex z position", 200, -50., 50.).fill(vtx.getPosition().z());
            _bottomBTracks.remove(1);
            _bottomBTracks.remove(0);
        }
    }

    private void analyzeHitlayers(ReconstructedParticle rp) {
        Track t = rp.getTracks().get(0);
        String topOrBottom = isTopTrack(t) ? " top " : " bottom ";
        double p = rp.getMomentum().magnitude();
        int nHits = t.getTrackerHits().size();
//        System.out.println("Track has " + nHits + " hits");
        for (TrackerHit hit : TrackUtils.getStripHits(t, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            int layerNumber = ((RawTrackerHit) rthList.get(0)).getLayerNumber();
//            System.out.println(" hit in layer " + layerNumber);
            aida.histogram2D(topOrBottom + " " + nHits + "-hit Track hit layer number vs track momentum", 14, 0.5, 14.5, 50, 3.4, 5.4).fill(layerNumber, p);
        }
    }

    private double[] stripClusterSizes(Track t) {
        // return the number of strips in the first two axial and first two stereo layers.
        double[] nStrips = new double[4]; // L1A, L2A, L1S, L2S
        // only analyze 6-hit tracks (for now)
        int t1Nhits = t.getTrackerHits().size();
        if (t1Nhits == 6) {
            // in principle, tracks with multi-strip hits are better measured...
            // 1st axial layer has greatest influence on theta, so require 2 strips in hit
            // TODO should I also require 2 strips in stereo layers?
            int tL1AxialNstrips = 0;
            int tL1StereoNstrips = 0;
            int tL2AxialNstrips = 0;
            int tL2StereoNstrips = 0;

//            int t1L1AxialStripNumber = 0;
//            int t1L1StereoStripNumber = 0;
//            int t1L2AxialStripNumber = 0;
//            int t1L2StereoStripNumber = 0;
            for (TrackerHit hit : TrackUtils.getStripHits(t, hitToStrips, hitToRotated)) {
                List rthList = hit.getRawHits();
                String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
                if (moduleName.contains("module_L1")) {
                    if (moduleName.contains("axial")) {
                        tL1AxialNstrips = rthList.size();
//                        if (rthList.size() == 1) // look at single strip clusters
//                        {
//                            t1L1AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
//                            aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L1AxialStripNumber);
//                        }
                    }
                    if (moduleName.contains("stereo")) {
                        tL1StereoNstrips = rthList.size();
//                        if (rthList.size() == 1) // look at single strip clusters
//                        {
//                            t1L1StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
//                            aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L1StereoStripNumber);
//                        }
                    }
                }
                if (moduleName.contains("module_L2")) {
                    if (moduleName.contains("axial")) {
                        tL2AxialNstrips = rthList.size();
//                        if (rthList.size() == 1) // look at single strip clusters
//                        {
//                            t1L2AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
//                            aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L2AxialStripNumber);
//                        }
                    }
                    if (moduleName.contains("stereo")) {
                        tL2StereoNstrips = rthList.size();
//                        if (rthList.size() == 1) // look at single strip clusters
//                        {
//                            t1L2StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
//                            aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L2StereoStripNumber);
//                        }
                    }
                }
            }
            nStrips[0] = tL1AxialNstrips;
            nStrips[1] = tL2AxialNstrips;
            nStrips[2] = tL1StereoNstrips;
            nStrips[3] = tL2StereoNstrips;
        }
        return nStrips;
    }

    private void alignit(ReconstructedParticle rp) {
        Track t1 = rp.getTracks().get(0);
        // only analyze 6-hit tracks (for now)
        int t1Nhits = t1.getTrackerHits().size();
        if (t1Nhits != 6) {
            return;
        }
        // in principle, tracks with multi-strip hits are better measured...
        // 1st axial layer has greatest influence on theta, so require 2 strips in hit
        // TODO should I also require 2 strips in stereo layers?
        int t1L1AxialNstrips = 0;
        int t1L1StereoNstrips = 0;
        int t1L2AxialNstrips = 0;
        int t1L2StereoNstrips = 0;

        int t1L1AxialStripNumber = 0;
        int t1L1StereoStripNumber = 0;
        int t1L2AxialStripNumber = 0;
        int t1L2StereoStripNumber = 0;

        for (TrackerHit hit : TrackUtils.getStripHits(t1, hitToStrips, hitToRotated)) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    t1L1AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L1AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L1StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L1StereoStripNumber);
                    }
                }
            }
            if (moduleName.contains("module_L2")) {
                if (moduleName.contains("axial")) {
                    t1L2AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 0., 100.).fill(t1L2AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L2StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        aida.histogram1D(moduleName + "single strip cluster strip number", 100, 540., 640.).fill(t1L2StereoStripNumber);
                    }
                }
            }

//            for (Object o : hit.getRawHits()) {
//                RawTrackerHit rth = (RawTrackerHit) o;
//                System.out.printf("name=%s\tside=%d\tstrip=%d\n", rth.getDetectorElement().getName(),
//                        rth.getIdentifierFieldValue("side"), rth.getIdentifierFieldValue("strip"));
//            }
//            System.out.println("Track 1 hit at " + Arrays.toString(hit.getPosition()) + " has " + hit.getRawHits().size() + " strips");
        }

        double e1 = rp.getEnergy();
        double p1 = rp.getMomentum().magnitude();

        double d0 = t1.getTrackStates().get(0).getD0();
        double z0 = t1.getTrackStates().get(0).getZ0();

        Hep3Vector p1mom = rp.getMomentum();

        //rotate into physics frame of reference
        Hep3Vector rprot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        Hep3Vector p1rot = VecOp.mult(beamAxisRotation, rp.getMomentum());
        double theta1 = Math.acos(p1rot.z() / p1rot.magnitude());

        double theta1x = Math.asin(p1rot.x() / p1rot.magnitude());
        double theta1y = Math.asin(p1rot.y() / p1rot.magnitude());

        if (t1L1AxialNstrips < 3) {
            aida.histogram1D("Track thetaY " + t1L1AxialNstrips + " L1 axial strips", 1000, -0.06, 0.06).fill(theta1y);
        }

        if (t1L1AxialNstrips < 3 && t1L2AxialNstrips < 3) {
            aida.histogram1D("Track thetaY " + t1L1AxialNstrips + " L1 " + t1L2AxialNstrips + " L2 axial strips", 1000, -0.06, 0.06).fill(theta1y);
        }

        // look for correlations
        if (t1L1AxialNstrips == 1 && t1L2AxialNstrips == 1) {
            if (theta1y > 0) {
                aida.histogram2D("Top Track L1 axial strip number vs Track thetaY", 100, 0., 100., 500, 0.015, 0.055).fill(t1L1AxialStripNumber, theta1y);
            } else {
                aida.histogram2D("Bottom Track L1 axial strip number vs Track thetaY", 100, 0., 100., 500, 0.015, 0.055).fill(t1L1AxialStripNumber, -theta1y);
            }
            if (t1L1AxialStripNumber > 1 && t1L1AxialStripNumber < 100) // inspect the first few strips more closely...
            {
                if (theta1y > 0) {
                    aida.histogram2D("Top Track thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
                    aida.histogram1D("Top Track L1 axial strip number " + t1L1AxialStripNumber + " thetaY", 400, 0.015, 0.055).fill(theta1y);
                    aida.cloud1D("Top Track thetaX").fill(theta1x);
                    aida.histogram2D("Top Track L1 axial strip number " + t1L1AxialStripNumber + " thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
                } else {
                    aida.histogram2D("Bottom Track thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
                    aida.histogram1D("Bottom Track L1 axial strip number " + t1L1AxialStripNumber + " thetaY", 400, 0.015, 0.055).fill(-theta1y);
                    aida.cloud1D("Bottom Track thetaX").fill(theta1x);
                    aida.histogram2D("Bottom Track L1 axial strip number " + t1L1AxialStripNumber + " thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
                }
            }
        }
        if (t1L1AxialNstrips == 1 && t1L2AxialNstrips == 2) { // should give the bext position resolution for layer 2 wrt 1
            if (theta1y > 0) {
                aida.histogram2D("Top Track 1 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
            } else {
                aida.histogram2D("Bottom Track 1 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
            }
        }
        if (t1L1AxialNstrips == 2 && t1L2AxialNstrips == 2) { // should give the bext position resolution but no structure
            if (theta1y > 0) {
                aida.histogram2D("Top Track 2 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, theta1y);
            } else {
                aida.histogram2D("Bottom Track 2 L1 2 L2 thetaX vs thetaY", 100, thetaXmin, thetaXmax, 400, 0.015, 0.055).fill(theta1x, -theta1y);
            }
        }

//        double mollerTrackTheta1 = acos(1 - 0.511e-3 * (1 / p1 - 1 / _beamEnergy));
//        double mollerTrackTheta2 = acos(1 - 0.511e-3 * (1 / p2 - 1 / _beamEnergy));
//
//        double phi1 = atan2(p1rot.x(), p1rot.y());
//        double phi2 = atan2(p2rot.x(), -p2rot.y()); //TODO figure out why the -ive sign is needed
//
//        // step in momentum
//        for (int i = 0; i < nSteps; ++i) {
//            double pBin = pMin + i * dP;
//            BigDecimal bd = new BigDecimal(Double.toString(pBin));
//            bd = bd.setScale(2, BigDecimal.ROUND_HALF_UP);
//            double binLabel = bd.doubleValue();
//
//            // System.out.println("i " + i + " pBin " + pBin + " p1 " + p1 + " p2 " + p2);
//            if (abs(p1 - pBin) < dP / 2.) {
//                double dTheta = theta1 - mollerTrackTheta1;
//                if (isTopTrack(t1)) {
//                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p1);
//                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Top Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
//                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
//                    if (t1L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
//                    }
//                } else {
//                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p1);
//                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t1Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Bottom Track theta " + t1Nhits + " hits", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta1x, theta1y);
//                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta1);
//                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi1, dTheta);
//                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi1, dTheta);
//                    if (t1L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t1L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi1, dTheta);
//                    }
//                }
//            }
//            if (abs(p2 - pBin) < dP / 2.) {
//                double dTheta = theta2 - mollerTrackTheta2;
//                if (isTopTrack(t2)) {
//                    aida.histogram1D("Top Track Momentum", 100, 0.25, 1.75).fill(p2);
//                    //aida.histogram2D(binLabel + "Top Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Top Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Top Track theta ", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Top Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
//                    aida.profile1D(binLabel + " Top Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
//                    if (t2L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Top Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
//                    }
//                } else {
//                    aida.histogram1D("Bottom Track Momentum", 100, 0.25, 1.75).fill(p2);
//                    //aida.histogram2D(binLabel + "Bottom Track thetaX vs ThetaY " + t2Nhits + " hits", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Bottom Track theta " + t2Nhits + " hits", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Track thetaX vs ThetaY ", 100, -thetaMax, thetaMax, 100, -thetaMax, thetaMax).fill(theta2x, theta2y);
//                    aida.histogram1D(binLabel + " Bottom Track theta ", 100, 0.015, thetaMax).fill(theta2);
//                    aida.histogram2D(binLabel + " Bottom Track phi vs dTheta", 100, -1., 1., 100, -0.01, 0.01).fill(phi2, dTheta);
//                    aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile", 100, -1., 1.).fill(phi2, dTheta);
//                    if (t2L1AxialNstrips < 3) {
//                        aida.profile1D(binLabel + " Bottom Track phi vs dTheta Profile " + t2L1AxialNstrips + " L1 axial strips", 100, -1., 1.).fill(phi2, dTheta);
//                    }
//                }
//            }
//        }
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

    static private String myDate() {
        Calendar cal = new GregorianCalendar();
        Date date = new Date();
        cal.setTime(date);
        DecimalFormat formatter = new DecimalFormat("00");
        String day = formatter.format(cal.get(Calendar.DAY_OF_MONTH));
        String month = formatter.format(cal.get(Calendar.MONTH) + 1);
        return cal.get(Calendar.YEAR) + month + day;
    }
    //
    //    private void sliceAndFit(IHistogram2D hist2D, IDataPointSet dataPointSet, IHistogramFactory hf) {
    //        IAxis xAxis = hist2D.xAxis();
    //        int nBins = xAxis.bins();
    //        IHistogram1D[] bottomSlices = new IHistogram1D[nBins];
    //        IDataPoint dp;
    //        int nDataPoints = 0;
    //        for (int i = 0; i < nBins; ++i) { // stepping through x axis bins
    //            bottomSlices[i] = hf.sliceY("/bottom slice " + i, hist2D, i);
    //            System.out.println("bottom slice " + i + " has " + bottomSlices[i].allEntries() + " entries");
    //            if (bottomSlices[i].entries() > 100.) {
    //                IFitResult fr = performGaussianFit(bottomSlices[i]);
    //                System.out.println(" fit status: " + fr.fitStatus());
    //                double[] frPars = fr.fittedParameters();
    //                double[] frParErrors = fr.errors();
    //                String[] frParNames = fr.fittedParameterNames();
    //                System.out.println(" Energy Resolution Fit: ");
    //                for (int jj = 0; jj < frPars.length; ++jj) {
    //                    System.out.println(frParNames[jj] + " : " + frPars[jj] + " +/- " + frParErrors[jj]);
    //                }
    //                // create a datapoint
    //                dataPointSet.addPoint();
    //                dp = dataPointSet.point(nDataPoints++);
    //                dp.coordinate(0).setValue(xAxis.binCenter(i));
    //                dp.coordinate(1).setValue(frPars[1]); // gaussian mean
    //                dp.coordinate(1).setErrorPlus(frParErrors[1]);
    //                dp.coordinate(1).setErrorMinus(frParErrors[1]);
    //            }
    //        }
    //        try {
    //            aida.tree().commit();
    //        } catch (IOException ex) {
    //            Logger.getLogger(V0SvtAlignmentDriver.class.getName()).log(Level.SEVERE, null, ex);
    //        }
    //    }

    /**
     * Fits a vertex from an electron/positron track pair using the indicated
     * constraint.
     *
     * @param constraint - The constraint type to use.
     * @param electron - The electron track.
     * @param positron - The positron track.
     * @return Returns the reconstructed vertex as a <code>BilliorVertex
     * </code> object. mg--8/14/17--add the displaced vertex refit for the
     * UNCONSTRAINED and BS_CONSTRAINED fits
     */
    private BilliorVertex fitVertex(ReconstructedParticle electron, ReconstructedParticle positron) {

        // Covert the tracks to BilliorTracks.
        BilliorTrack electronBTrack = toBilliorTrack(electron.getTracks().get(0));
        BilliorTrack positronBTrack = toBilliorTrack(positron.getTracks().get(0));

        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.

        // Perform the vertexing
        vtxFitter.doBeamSpotConstraint(false);

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
        billiorTracks.add(electronBTrack);
        billiorTracks.add(positronBTrack);

        // Find a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);

        // mg 8/14/17 
        // if this is an unconstrained or BS constrained vertex, propogate the 
        // tracks to the vertex found in previous fit and do fit again
        //  ...  this is required because the vertex fit assumes trajectories 
        // change linearly about the reference point (which we initially guess to be 
        // (0,0,0) while for long-lived decays there is significant curvature
        List<ReconstructedParticle> recoList = new ArrayList<ReconstructedParticle>();
        recoList.add(electron);
        recoList.add(positron);
        List<BilliorTrack> shiftedTracks = shiftTracksToVertex(recoList, vtx.getPosition());

        BilliorVertex vtxNew = vtxFitter.fitVertex(shiftedTracks);
        Hep3Vector vtxPosNew = VecOp.add(vtx.getPosition(), vtxNew.getPosition());//the refit vertex is measured wrt the original vertex position
        vtxNew.setPosition(vtxPosNew);//just change the position...the errors and momenta are correct in re-fit
        return vtxNew;

    }

    private BilliorVertex fitVertex(BilliorTrack t1, BilliorTrack t2) {

        // Create a vertex fitter from the magnetic field.
        BilliorVertexer vtxFitter = new BilliorVertexer(bField);
        // TODO: The beam size should come from the conditions database.

        // Perform the vertexing
        vtxFitter.doBeamSpotConstraint(false);

        // Add the electron and positron tracks to a track list for
        // the vertex fitter.
        List<BilliorTrack> billiorTracks = new ArrayList<BilliorTrack>();
        billiorTracks.add(t1);
        billiorTracks.add(t2);

        // Find a vertex based on the tracks.
        BilliorVertex vtx = vtxFitter.fitVertex(billiorTracks);

//        // mg 8/14/17 
//        // if this is an unconstrained or BS constrained vertex, propogate the 
//        // tracks to the vertex found in previous fit and do fit again
//        //  ...  this is required because the vertex fit assumes trajectories 
//        // change linearly about the reference point (which we initially guess to be 
//        // (0,0,0) while for long-lived decays there is significant curvature
//        List<ReconstructedParticle> recoList = new ArrayList<ReconstructedParticle>();
//        recoList.add(electron);
//        recoList.add(positron);
//        List<BilliorTrack> shiftedTracks = shiftTracksToVertex(recoList, vtx.getPosition());
//
//        BilliorVertex vtxNew = vtxFitter.fitVertex(shiftedTracks);
//        Hep3Vector vtxPosNew = VecOp.add(vtx.getPosition(), vtxNew.getPosition());//the refit vertex is measured wrt the original vertex position
//        vtxNew.setPosition(vtxPosNew);//just change the position...the errors and momenta are correct in re-fit
//        return vtxNew;
        return vtx;
    }

    private List<BilliorTrack> shiftTracksToVertex(List<ReconstructedParticle> particles, Hep3Vector vtxPos) {
        ///     Ok...shift the reference point....        
        double[] newRef = {vtxPos.z(), vtxPos.x(), 0.0};//the  TrackUtils.getParametersAtNewRefPoint method only shifts in xy tracking frame
        List<BilliorTrack> newTrks = new ArrayList<BilliorTrack>();
        for (ReconstructedParticle part : particles) {
            BaseTrackState oldTS = (BaseTrackState) part.getTracks().get(0).getTrackStates().get(0);
            double[] newParams = TrackUtils.getParametersAtNewRefPoint(newRef, oldTS);
            SymmetricMatrix newCov = TrackUtils.getCovarianceAtNewRefPoint(newRef, oldTS.getReferencePoint(), oldTS.getParameters(), new SymmetricMatrix(5, oldTS.getCovMatrix(), true));
            //mg...I don't like this re-casting, but toBilliorTrack only takes Track as input
            BaseTrackState newTS = new BaseTrackState(newParams, newRef, newCov.asPackedArray(true), TrackState.AtIP, bField);
            BilliorTrack electronBTrackShift = this.toBilliorTrack(newTS);
            newTrks.add(electronBTrackShift);
        }
        return newTrks;
    }

    /**
     * Converts a <code>Track</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track.
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(Track track) {
        // Generate and return the billior track.
        return new BilliorTrack(track);
    }

    /**
     * Converts a <code>TrackState</code> object to a <code>BilliorTrack
     * </code> object.
     *
     * @param track - The original track state
     * @return Returns the original track as a <code>BilliorTrack
     * </code> object.
     */
    private BilliorTrack toBilliorTrack(TrackState trackstate) {
        // Generate and return the billior track.
        return new BilliorTrack(trackstate, 0, 0); // track state doesn't store chi^2 info (stored in the Track object)
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
}
