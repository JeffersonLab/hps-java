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
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
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
    Map<String, IRotation3D> sensorRotations = new ConcurrentSkipListMap<String, IRotation3D>();
    Map<String, ITranslation3D> sensorTranslations = new ConcurrentSkipListMap<String, ITranslation3D>();

    Map<Integer, Double> uLocal = new ConcurrentSkipListMap<Integer, Double>();
    Map<Integer, Double> uSigLocal = new ConcurrentSkipListMap<Integer, Double>();

    Formatter topEvents;
    Formatter bottomEvents;

    @Override
    protected void detectorChanged(Detector detector) {

        try {
            topEvents = new Formatter("topEvents.txt");
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

        for (TrackerHit hit : stripClusters) {

            List rthList = hit.getRawHits();
            int size = rthList.size();
            double sense_pitch = 0;
            List<Double> signals = new ArrayList<Double>();
            List<Hep3Vector> positions = new ArrayList<Hep3Vector>();
            List<FittedRawTrackerHit> cluster = new ArrayList<FittedRawTrackerHit>();
            ITransform3D local_to_global = null;
            ITransform3D global_to_local = null;
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

//        if (isTop) {
//            for (int i = 1; i < 13; ++i) {
//                System.out.println("global " + Arrays.toString(globalPos.get(i)));
//                System.out.println("local " + Arrays.toString(localPos.get(i)));
//            }
//        }

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

}
