package org.hps.analysis.alignment;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Formatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import org.apache.commons.math3.geometry.euclidean.threed.Rotation;
import org.apache.commons.math3.geometry.euclidean.threed.RotationConvention;
import org.apache.commons.math3.geometry.euclidean.threed.RotationOrder;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.hps.recon.tracking.DefaultSiliconResolutionModel;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.SiliconResolutionModel;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationPassiveXYZ;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.Cluster;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class StraightTrackSvtAlignmentDriver extends Driver {

    boolean debug = false;
    boolean printGeometry = false;
    boolean printEvent = false;
    boolean isTop = false;
    private AIDA aida = AIDA.defaultInstance();

    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    private SiliconResolutionModel _res_model = new DefaultSiliconResolutionModel();
    boolean _useWeights = true;
    private double _oneClusterErr = 1 / Math.sqrt(12);
    private double _twoClusterErr = 1 / 5.;
    private double _threeClusterErr = 1 / 3.;
    private double _fourClusterErr = 1 / 2.;
    private double _fiveClusterErr = 1;

    // let's store some geometry here...
    Map<String, double[]> sensorAngles = new ConcurrentSkipListMap<String, double[]>();
    Map<String, double[]> sensorShifts = new ConcurrentSkipListMap<String, double[]>();
    Map<String, ITransform3D> localToGlobalMap = new ConcurrentSkipListMap<String, ITransform3D>();
    Map<String, ITransform3D> globalToLocalMap = new ConcurrentSkipListMap<String, ITransform3D>();

    Map<String, IRotation3D> sensorRotations = new ConcurrentSkipListMap<String, IRotation3D>();
    Map<String, ITranslation3D> sensorTranslations = new ConcurrentSkipListMap<String, ITranslation3D>();

    Map<Integer, Double> uLocal = new ConcurrentSkipListMap<Integer, Double>();
    Map<Integer, Double> uSigLocal = new ConcurrentSkipListMap<Integer, Double>();

    Formatter topEvents;
    Formatter bottomEvents;
    Formatter topExtrap;

    @Override
    protected void detectorChanged(Detector detector) {

        try {
            topEvents = new Formatter("topEvents.txt");
            topExtrap = new Formatter("topEventsExtrap.txt");
        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("Error opening topEvents.txt");
        }

        try {
            bottomEvents = new Formatter("bottomEvents.txt");
        } catch (FileNotFoundException fileNotFoundException) {
            System.err.println("Error opening bottomEvents.txt");
        }

    }

    protected void process(EventHeader event) {

        // only keep events with one and only one cluster
        List<Cluster> ecalClusters = event.get(Cluster.class, "EcalClustersCorr");
        if (ecalClusters.size() != 1) {
            return;
        }
        uLocal.clear();
        uSigLocal.clear();

        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);

        setupSensors(event);
        List<TrackerHit> stripClusters = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
        // Map the fitted hits to their corresponding raw hits
        Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }

        Map<Integer, double[]> globalPos = new ConcurrentSkipListMap<Integer, double[]>();
        Map<Integer, double[]> localPos = new ConcurrentSkipListMap<Integer, double[]>();

        Map<Integer, Hep3Vector> sensorOrigins = new ConcurrentSkipListMap<Integer, Hep3Vector>();
        Map<Integer, Hep3Vector> sensorNormals = new ConcurrentSkipListMap<Integer, Hep3Vector>();
        Map<Integer, String> sensorNames = new ConcurrentSkipListMap<Integer, String>();

        Cluster c = ecalClusters.get(0);
        double[] ecalClusterPos = c.getPosition();

        //histograms
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

        for (TrackerHit hit : stripClusters) {
            List rthList = hit.getRawHits();
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            if (moduleName.contains("module_L1")) {
                if (moduleName.contains("axial")) {
                    t1L1AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        //aida.cloud1D(moduleName + "single strip cluster strip number cloud").fill(t1L1AxialStripNumber);
                        aida.histogram1D(moduleName + "single strip cluster strip number", 300, 350., 650.).fill(t1L1AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L1StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L1StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        //aida.cloud1D(moduleName + "single strip cluster strip number cloud").fill(t1L1StereoStripNumber);
                        aida.histogram1D(moduleName + "single strip cluster strip number", 300, 50., 350.).fill(t1L1StereoStripNumber);
                    }
                }
            }
            if (moduleName.contains("module_L2")) {
                if (moduleName.contains("axial")) {
                    t1L2AxialNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2AxialStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        //aida.cloud1D(moduleName + "single strip cluster strip number cloud").fill(t1L2AxialStripNumber);
                        aida.histogram1D(moduleName + "single strip cluster strip number", 300, 350., 650.).fill(t1L2AxialStripNumber);
                    }
                }
                if (moduleName.contains("stereo")) {
                    t1L2StereoNstrips = rthList.size();
                    if (rthList.size() == 1) // look at single strip clusters
                    {
                        t1L2StereoStripNumber = ((RawTrackerHit) hit.getRawHits().get(0)).getIdentifierFieldValue("strip");
                        //aida.cloud1D(moduleName + "single strip cluster strip number cloud").fill(t1L2StereoStripNumber);
                        aida.histogram1D(moduleName + "single strip cluster strip number", 300, 50., 350.).fill(t1L2StereoStripNumber);
                    }
                }
            }
        }

        if (t1L1AxialStripNumber != 0 && t1L2AxialStripNumber != 0) {
            int dN = t1L1AxialStripNumber - t1L2AxialStripNumber;
            double ecalX = ecalClusterPos[0];
            String hemi = (ecalClusterPos[1] > 0) ? "top" : "bottom";
            aida.cloud1D(hemi + " ecal x position").fill(ecalX);
            aida.histogram2D(hemi + " strip dNumber vs Ecal x position",100,-40., 40., 40, -20., 20.).fill(ecalX, dN);
            aida.profile1D(hemi + " strip dNumber vs Ecal x position profile",100,-30., 60.).fill(ecalX, dN);
            if(-30.<ecalX && ecalX<-20.)
            {
                aida.histogram1D(hemi + " strip dN -30 < ecalX < -20", 100, -20., 20.).fill(dN);
            }
            if(20.<ecalX && ecalX<30.)
            {
                aida.histogram1D(hemi + " strip dN 20 < ecalX < 30", 100, -20., 20.).fill(dN);
            }
        }

        // geometry info
        boolean doGeom = false;
        if (doGeom) {
            for (TrackerHit hit : stripClusters) {

                List rthList = hit.getRawHits();
                int size = rthList.size();
                double sense_pitch = 0;
                List<Double> signals = new ArrayList<Double>();
                List<Hep3Vector> positions = new ArrayList<Hep3Vector>();
                List<FittedRawTrackerHit> cluster = new ArrayList<FittedRawTrackerHit>();
                ITransform3D local_to_global = null;
                ITransform3D global_to_local = null;
                Hep3Vector sensorOrigin;
                Hep3Vector sensorNormal;
                for (int i = 0; i < size; ++i) {
                    RawTrackerHit rth = ((RawTrackerHit) rthList.get(i));
                    IIdentifier id = rth.getIdentifier();
                    SiSensor sensor = (SiSensor) rth.getDetectorElement();
                    local_to_global = sensor.getGeometry().getLocalToGlobal();
                    global_to_local = sensor.getGeometry().getGlobalToLocal();
                    SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
                    ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
                    SiSensorElectrodes electrodes = ((SiSensor) rth.getDetectorElement()).getReadoutElectrodes(carrier);
                    sense_pitch = sensor.getSenseElectrodes(electrodes.getChargeCarrier()).getPitch(0);
                    Hep3Vector stripPosition = ((SiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(id));
                    double stripAmp = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rth));
                    signals.add(stripAmp);
                    positions.add(stripPosition);
                    if (debug) {
                        System.out.println("sensor " + sensor + " sense pitch " + sense_pitch);
                        System.out.println("strip amplitude: " + stripAmp);
                        System.out.println("strip position " + stripPosition);
                    }
                } // loop over strips in cluster
                Hep3Vector weightedPos = weightedAveragePosition(signals, positions);
                if (debug) {
                    System.out.println(size + " hit cluster weighted average position " + weightedPos);
                    System.out.println("hit cov matrix " + Arrays.toString(hit.getCovMatrix()));
                }
                double measured_resolution;
                switch (size) {
                    case 1:
                        measured_resolution = sense_pitch * _oneClusterErr;
                        break;
                    case 2:
                        measured_resolution = sense_pitch * _twoClusterErr;
                        break;
                    case 3:
                        measured_resolution = sense_pitch * _threeClusterErr;
                        break;
                    case 4:
                        measured_resolution = sense_pitch * _fourClusterErr;
                        break;
                    default:
                        measured_resolution = sense_pitch * _fiveClusterErr;
                        break;
                }

                String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
                int layer = TrackUtils.getLayer(hit);
                globalPos.put(layer, hit.getPosition());
                localPos.put(layer, weightedPos.v());

                isTop = moduleName.contains("t_halfmodule") ? true : false;
                uLocal.put(layer, weightedPos.x());
                uSigLocal.put(layer, 1. / (measured_resolution * measured_resolution));
                SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(hit);
                //
                sensorOrigin = getOrigin(stripHit);
                sensorNormal = getNormal(stripHit);

                sensorOrigins.put(layer, sensorOrigin);
                sensorNormals.put(layer, sensorNormal);
                sensorNames.put(layer, moduleName);
                localToGlobalMap.put(moduleName, local_to_global);
                globalToLocalMap.put(moduleName, global_to_local);

                //
                Hep3Vector uMeas = stripHit.getMeasuredCoordinate();
                Hep3Vector vMeas = stripHit.getUnmeasuredCoordinate();
                Hep3Vector pos = stripHit.getPositionAsVector();
                Hep3Vector calcNormal = VecOp.cross(vMeas, uMeas);
                Hep3Vector transformed_ltg = local_to_global.transformed(weightedPos);
                Hep3Vector transformed_gtl = global_to_local.transformed(pos);
                IRotation3D gtl_rot = global_to_local.getRotation();
                ITranslation3D gtl_trans = global_to_local.getTranslation();
                IRotation3D ltg_rot = local_to_global.getRotation();
                ITranslation3D ltg_trans = local_to_global.getTranslation();
                Vector3D vX = Vector3D.PLUS_I;
                Vector3D vY = Vector3D.PLUS_J;
                //Vector3D vZ = Vector3D.PLUS_K;

                Vector3D vXprime = new Vector3D(uMeas.x(), uMeas.y(), uMeas.z());
                Vector3D vYprime = new Vector3D(vMeas.x(), vMeas.y(), vMeas.z());
                // create a rotation matrix from this pair of vectors
                Rotation xyVecRot = new Rotation(vX, vY, vXprime, vYprime);
                double[] hpsAngles = xyVecRot.getAngles(RotationOrder.XYZ, RotationConvention.VECTOR_OPERATOR);
                sensorAngles.put(moduleName, hpsAngles);
                sensorShifts.put(moduleName, ltg_trans.getTranslationVector().v());
                sensorRotations.put(moduleName, ltg_rot);
                sensorTranslations.put(moduleName, ltg_trans);

                if (debug) {
                    System.out.println("measured_resolution " + measured_resolution);
                    System.out.println(moduleName);
                    System.out.println("layer: " + layer);
                    System.out.println("u: " + uMeas);
                    System.out.println("v: " + vMeas);
                    System.out.println("calculated normal v x u " + calcNormal);
                    System.out.println("weighted pos " + weightedPos);
                    System.out.println("transformed ltg" + transformed_ltg);
                    System.out.println("pos: " + pos);
                    System.out.println("transformed gtl" + transformed_gtl);
                    System.out.println("gtl_rot " + gtl_rot);
                    System.out.println("gtl_trans " + gtl_trans);
                    System.out.println("ltg_rot " + ltg_rot);
                    System.out.println("ltg_trans " + ltg_trans);
//do some testing here...
                    Hep3Vector X = new BasicHep3Vector(1., 0., 0.); // this is local u
                    Hep3Vector Y = new BasicHep3Vector(0., 1., 0.); // this is local v
                    Hep3Vector Z = new BasicHep3Vector(0., 0., 1.); // this is local z

                    double[][] xyVecRotMat = xyVecRot.getMatrix();
                    System.out.println("Apache commons rotation:");
                    for (int ii = 0; ii < 3; ++ii) {
                        System.out.println(xyVecRotMat[ii][0] + " " + xyVecRotMat[ii][1] + " " + xyVecRotMat[ii][2]);
                    }
                    System.out.println("Apache commons angles");
                    System.out.println(Arrays.toString(hpsAngles));

                    double alpha = hpsAngles[0];
                    double beta = hpsAngles[1];
                    double gamma = hpsAngles[2];
                    IRotation3D tstRotPassive = new RotationPassiveXYZ(alpha, beta, gamma);
                    // this equals local_to_global
                    System.out.println("rotPassive " + tstRotPassive);
                    IRotation3D tstRotPassiveInv = tstRotPassive.inverse();
                    System.out.println("rotPassiveInv " + tstRotPassiveInv);
                }

            } // loop over strip clusters

            if (printGeometry) {
                for (String s : sensorAngles.keySet()) {
                    System.out.println("module: " + s);
                    System.out.println("angles " + Arrays.toString(sensorAngles.get(s)));
                    System.out.println("shifts " + Arrays.toString(sensorShifts.get(s)));
                    System.out.println("rotation " + sensorRotations.get(s));
                    System.out.println("translation " + sensorTranslations.get(s));
                }
            }

            if (isTop) {
                //try something here...
                // connect beamspot at HARP scan wire and cluster centroid and predict track intercepts with sensors.
                // this should get the y position for axial layers fairly well
                // wire is nominally at (0.,0.,-2337.1810);
                //

                Hep3Vector line = new BasicHep3Vector(ecalClusterPos[0], ecalClusterPos[1], ecalClusterPos[2] + 2337.1810);
                Hep3Vector IP = new BasicHep3Vector(0., 0., -2337.1810);
                double dz = ecalClusterPos[2] + 2337.1810;
                double dxdz = ecalClusterPos[0] / dz;
                double dydz = ecalClusterPos[1] / dz;
                double[] xtrap = new double[12];
                double[] xtrapSig = new double[12];
                for (int i = 1; i < 13; ++i) {
                    System.out.println(" layer " + i + " " + sensorNames.get(i));
                    System.out.println("global " + Arrays.toString(globalPos.get(i)));
                    System.out.println("local " + Arrays.toString(localPos.get(i)));
                    System.out.println("sensor origin " + sensorOrigins.get(i));
                    System.out.println("sensor normal " + sensorNormals.get(i));
                    Hep3Vector xcept = getLinePlaneIntercept(line, IP, sensorOrigins.get(i), sensorNormals.get(i));
                    System.out.println("intercept " + xcept);
                    ITransform3D gtl = globalToLocalMap.get(sensorNames.get(i));
                    Hep3Vector local_xcept = gtl.transformed(xcept);
                    System.out.println("local xcept " + local_xcept);
                    System.out.println(" ");
                    // should I smear these?
                    xtrap[i - 1] = local_xcept.x();
                    xtrapSig[i - 1] = 27777.7778;
                }
                for (int i = 0; i < 12; ++i) {
                    topExtrap.format("%10.4f", xtrap[i]);
                }
                for (int i = 0; i < 12; ++i) {
                    topExtrap.format("%12.4f", xtrapSig[i]);
                }
                // 4 track parameters
                topExtrap.format("%10.4f", 0.); //x
                topExtrap.format("%10.4f", 0.); //y
                topExtrap.format("%10.4f", dxdz); //dxdz
                topExtrap.format("%10.4f", dydz); //dydz

                topExtrap.format("%s", "\n");
            }

            if (printEvent) {
                if (uLocal.size() == 12) {

                    Formatter writer = isTop ? topEvents : bottomEvents;
                    for (Map.Entry<Integer, Double> entry : uLocal.entrySet()) {
                        writer.format("%10.4f", entry.getValue());
                    }
                    for (Map.Entry<Integer, Double> entry : uSigLocal.entrySet()) {
                        writer.format("%12.4f", entry.getValue());
                    }
                    // filler for now, used for 4 track parameters
                    for (int i = 0; i < 4; ++i) {
                        writer.format("%10.4f", 0.);
                    }
                    writer.format("%s", "\n");
                }
            }
        }
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

    public Hep3Vector weightedAveragePosition(List<Double> signals, List<Hep3Vector> positions) {
        double total_weight = 0;
        Hep3Vector position = new BasicHep3Vector(0, 0, 0);
        for (int istrip = 0; istrip < signals.size(); istrip++) {
            double signal = signals.get(istrip);

            double weight = _useWeights ? signal : 1;
            total_weight += weight;
            position = VecOp.add(position, VecOp.mult(weight, positions.get(istrip)));
            /*if (_debug) {
                System.out.println(this.getClass().getSimpleName() + "strip " + istrip + ": signal " + signal + " position " + positions.get(istrip) + " -> total_position " + position.toString() + " ( total charge " + total_charge + ")");
            }*/
        }
        return VecOp.mult(1 / total_weight, position);
    }

    static Hep3Vector getOrigin(SiTrackerHitStrip1D stripCluster) {
        SiTrackerHitStrip1D local = stripCluster.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        ITransform3D trans = local.getLocalToGlobal();
        return trans.transformed(new BasicHep3Vector(0, 0, 0));
    }

    static Hep3Vector getNormal(SiTrackerHitStrip1D s2) {
        Hep3Vector u2 = s2.getMeasuredCoordinate();
        Hep3Vector v2 = s2.getUnmeasuredCoordinate();
        return VecOp.cross(u2, v2);
    }

    /**
     * Finds point of intercept between a generic straight line and a plane.
     *
     * @param l - vector pointing along the line
     * @param l0 - point on the line
     * @param p0 - point on the plane
     * @param n - normal vector of the plane.
     * @return point of intercept.
     */
    private static Hep3Vector getLinePlaneIntercept(Hep3Vector l, Hep3Vector l0, Hep3Vector p0, Hep3Vector n) {
        if (VecOp.dot(l, n) == 0) {
            throw new RuntimeException("This line and plane are parallel!");
        }
        final double d = VecOp.dot(VecOp.sub(p0, l0), n) / VecOp.dot(l, n);
        Hep3Vector p = VecOp.add(VecOp.mult(d, l), l0);
        return p;

    }

}
