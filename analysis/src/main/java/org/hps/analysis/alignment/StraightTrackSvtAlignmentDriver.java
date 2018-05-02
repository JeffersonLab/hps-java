package org.hps.analysis.alignment;

import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.DefaultSiliconResolutionModel;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.SiliconResolutionModel;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.RotationGeant;
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
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author Norman Graf
 */
public class StraightTrackSvtAlignmentDriver extends Driver {

    boolean debug = false;
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

    protected void process(EventHeader event) {
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
                System.out.println("sensor " + sensor + " sense pitch " + sense_pitch);
                double stripAmp = FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(rth));
                System.out.println("strip amplitude: " + stripAmp);
                System.out.println("strip position " + stripPosition);
                signals.add(stripAmp);
                positions.add(stripPosition);
            } // loop over strips in cluster
            Hep3Vector weightedPos = weightedAveragePosition(signals, positions);
            System.out.println(size + " hit cluster weighted average position " + weightedPos);
            System.out.println("hit cov matrix " + Arrays.toString(hit.getCovMatrix()));
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
            System.out.println("measured_resolution " + measured_resolution);
            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
            System.out.println(moduleName);
            int layer = TrackUtils.getLayer(hit);
            SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(hit);
            Hep3Vector uMeas = stripHit.getMeasuredCoordinate();
            Hep3Vector vMeas = stripHit.getUnmeasuredCoordinate();
            Hep3Vector pos = stripHit.getPositionAsVector();
            System.out.println("layer: " + layer);
            System.out.println("u: " + uMeas);
            System.out.println("v: " + vMeas);
            Hep3Vector calcNormal = VecOp.cross(vMeas, uMeas);
            System.out.println("calculated normal v x u " + calcNormal);

            Hep3Vector transformed_ltg = local_to_global.transformed(weightedPos);
            Hep3Vector transformed_gtl = global_to_local.transformed(pos);
            System.out.println("weighted pos " + weightedPos);
            System.out.println("transformed ltg" + transformed_ltg);
            System.out.println("pos: " + pos);
            System.out.println("transformed gtl" + transformed_gtl);

            IRotation3D gtl_rot = global_to_local.getRotation();
            System.out.println("gtl_rot " + gtl_rot);
            ITranslation3D gtl_trans = global_to_local.getTranslation();
            System.out.println("gtl_trans " + gtl_trans);

            IRotation3D ltg_rot = local_to_global.getRotation();
            System.out.println("ltg_rot " + ltg_rot);
            ITranslation3D ltg_trans = local_to_global.getTranslation();
            System.out.println("ltg_trans " + ltg_trans);

            // local to global is wrt to the Geant4 volume!!!!
            double alpha = 0.; //-PI / 2; // rotation about x
            double beta = .0305; // rotation about y (beam angle)
            double gamma = .050; // rotation about z (stereo angle)
            IRotation3D tstRotPassive = new RotationPassiveXYZ(alpha, beta, gamma);         
            IRotation3D tstRotGeant = new RotationGeant(alpha, beta, gamma);
            System.out.println("rotPassive " + tstRotPassive);
            System.out.println("rotGeant " + tstRotGeant);
            
            IRotation3D tstRotPassive123 = new RotationPassiveXYZ(alpha, beta, gamma);         
            IRotation3D tstRotPassive213 = new RotationPassiveXYZ(beta, alpha, gamma);         
            IRotation3D tstRotPassive231 = new RotationPassiveXYZ(beta, gamma, alpha);         
            IRotation3D tstRotPassive321 = new RotationPassiveXYZ(gamma, beta, alpha);
            IRotation3D tstRotPassive312 = new RotationPassiveXYZ(gamma, alpha, beta);
            System.out.println(tstRotPassive123);
            System.out.println(tstRotPassive213);
            System.out.println(tstRotPassive231);
            System.out.println(tstRotPassive321);
            System.out.println(tstRotPassive312);
            System.out.println(tstRotPassive123);
            
            
            

        } // loop over strip clusters
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
