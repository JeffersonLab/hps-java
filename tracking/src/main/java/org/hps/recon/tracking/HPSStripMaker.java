/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IReadout;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 *
 * @author mgraham
 */
public class HPSStripMaker {

    private static String _NAME = "HPSStripClusterer";
    // Clustering algorithm
    HPSClusteringAlgorithm _clustering;
    // Number of strips beyond which charge is averaged on center strips
    int _max_noaverage_nstrips = 4;
    // Absolute maximum cluster size
    int _max_cluster_nstrips = 10;
    // Sensor simulation needed to correct for Lorentz drift
    SiSensorSim _simulation;
    // Identifier helper (reset once per sensor)
    SiTrackerIdentifierHelper _sid_helper;    
    // Temporary map connecting hits to strip numbers for sake of speed (reset once per sensor)
    Map<HPSFittedRawTrackerHit, Integer> _strip_map = new HashMap<HPSFittedRawTrackerHit, Integer>();
    double _oneClusterErr = 1 / Math.sqrt(12);
    double _twoClusterErr = 1 / 5;
    double _threeClusterErr = 1 / 3;
    double _fourClusterErr = 1 / 2;
    double _fiveClusterErr = 1;
    
    boolean _debug = false;
    
    public HPSStripMaker(HPSClusteringAlgorithm algo) {
        _clustering = algo;
    }

    public HPSStripMaker(SiSensorSim simulation, HPSClusteringAlgorithm algo) {
        _clustering = algo;
        _simulation = simulation;
    }

    public String getName() {
        return _NAME;
    }

    // Make hits for all sensors within a DetectorElement
    public List<SiTrackerHit> makeHits(IDetectorElement detector) {
        System.out.println("makeHits(IDetectorElement): " + detector.getName());
        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();
        List<SiSensor> sensors = detector.findDescendants(SiSensor.class);

        // Loop over all sensors
        for (SiSensor sensor : sensors) {
            if (sensor.hasStrips()) {
                hits.addAll(makeHits(sensor));
            }
        }

        // Return hit list
        return hits;
    }

    // Make hits for a sensor
    public List<SiTrackerHit> makeHits(SiSensor sensor) {

        //System.out.println("makeHits: " + sensor.getName());

        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();

        // Get SiTrackerIdentifierHelper for this sensor and refresh the strip map used to increase speed
        _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        _strip_map.clear();

        // Get hits for this sensor
        IReadout ro = sensor.getReadout();
        List<HPSFittedRawTrackerHit> hps_hits = ro.getHits(HPSFittedRawTrackerHit.class);

        Map<SiSensorElectrodes, List<HPSFittedRawTrackerHit>> electrode_hits = new HashMap<SiSensorElectrodes, List<HPSFittedRawTrackerHit>>();

        for (HPSFittedRawTrackerHit hps_hit : hps_hits) {

            // get id and create strip map, get electrodes.
            IIdentifier id = hps_hit.getRawTrackerHit().getIdentifier();
            _strip_map.put(hps_hit, _sid_helper.getElectrodeValue(id));

            // Get electrodes and check that they are strips
            //System.out.println("proc raw hit from: " + DetectorElementStore.getInstance().find(raw_hit.getIdentifier()).get(0).getName());
            ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
            SiSensorElectrodes electrodes = ((SiSensor) hps_hit.getRawTrackerHit().getDetectorElement()).getReadoutElectrodes(carrier);
            if (!(electrodes instanceof SiStrips)) {
                continue;
            }

            if (electrode_hits.get(electrodes) == null) {
                electrode_hits.put(electrodes, new ArrayList<HPSFittedRawTrackerHit>());
            }

            electrode_hits.get(electrodes).add(hps_hit);
        }

        for (Map.Entry entry : electrode_hits.entrySet()) {
            hits.addAll(makeHits(sensor, (SiStrips) entry.getKey(), (List<HPSFittedRawTrackerHit>) entry.getValue()));
        }

        return hits;
    }

    public List<SiTrackerHit> makeHits(SiSensor sensor, SiSensorElectrodes electrodes, List<HPSFittedRawTrackerHit> hps_hits) {



        //  Call the clustering algorithm to make clusters
        List<List<HPSFittedRawTrackerHit>> cluster_list = _clustering.findClusters(hps_hits);

        //  Create an empty list for the pixel hits to be formed from clusters
        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();

        //  Make a pixel hit from this cluster
        for (List<HPSFittedRawTrackerHit> cluster : cluster_list) {

            // Make a TrackerHit from the cluster if it meets max cluster size requirement
            if (cluster.size() <= _max_cluster_nstrips) {
                SiTrackerHitStrip1D hit = makeTrackerHit(cluster, electrodes);
                // Add to readout and to list of hits
//                ((SiSensor) electrodes.getDetectorElement()).getReadout().addHit(hit);
                hits.add(hit);
                sensor.getReadout().addHit(hit);
            }
        }

        return hits;
    }

    public void SetOneClusterErr(double err) {
        _oneClusterErr = err;
    }

    public void SetTwoClusterErr(double err) {
        _twoClusterErr = err;
    }

    public void SetThreeClusterErr(double err) {
        _threeClusterErr = err;
    }

    public void SetFourClusterErr(double err) {
        _fourClusterErr = err;
    }

    public void SetFiveClusterErr(double err) {
        _fiveClusterErr = err;
    }

    public void setCentralStripAveragingThreshold(int max_noaverage_nstrips) {
        _max_noaverage_nstrips = max_noaverage_nstrips;
    }

    public void setMaxClusterSize(int max_cluster_nstrips) {
        _max_cluster_nstrips = max_cluster_nstrips;
    }

    private SiTrackerHitStrip1D makeTrackerHit(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        if(_debug) System.out.println(this.getClass().getSimpleName() + " makeTrackerHit ");
        Hep3Vector position = getPosition(cluster, electrodes);
        SymmetricMatrix covariance = getCovariance(cluster, electrodes);
        double time = getTime(cluster);
        double energy = getEnergy(cluster);
        TrackerHitType type = new TrackerHitType(TrackerHitType.CoordinateSystem.GLOBAL, TrackerHitType.MeasurementType.STRIP_1D);
        List<RawTrackerHit> rth_cluster = new ArrayList<RawTrackerHit>();
        for (HPSFittedRawTrackerHit bth : cluster) {
            rth_cluster.add(bth.getRawTrackerHit());
        }
        SiTrackerHitStrip1D hit = new SiTrackerHitStrip1D(position, covariance, energy, time, rth_cluster, type);
        if(_debug) System.out.println(this.getClass().getSimpleName() + " SiTrackerHitStrip1D created at " + position + "(" + hit.getPositionAsVector().toString()+")" + " E " + energy + " time " + time);
        return hit;
    }

    private Hep3Vector getPosition(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        if(_debug) System.out.println(this.getClass().getSimpleName() + " getPosition for cluster size " + cluster.size());
        List<Double> signals = new ArrayList<Double>();
        List<Hep3Vector> positions = new ArrayList<Hep3Vector>();

        if(_debug) System.out.println(this.getClass().getSimpleName() + " Loop of " + cluster.size() + " and add signals and positions to vectors");
        
        for (HPSFittedRawTrackerHit hit : cluster) {
            signals.add(hit.getAmp());
            positions.add(((SiStrips) electrodes).getStripCenter(_strip_map.get(hit)));
            if(_debug) System.out.println(this.getClass().getSimpleName() + " Added hit with signal " + hit.getAmp() + " at strip center posiiton " + (((SiStrips) electrodes).getStripCenter(_strip_map.get(hit))));
        }

        // Average charge on central strips of longer clusters
        if (signals.size() > _max_noaverage_nstrips) {
            int nstrips_center = signals.size() - 2;

            // collect sum of charges on center strips
            double center_charge_sum = 0.0;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++) {
                center_charge_sum += signals.get(istrip);
            }

            // distribute evenly on center strips
            double center_charge_strip = center_charge_sum / nstrips_center;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++) {
                signals.set(istrip, center_charge_strip);
            }
        }

        if(_debug) System.out.println(this.getClass().getSimpleName() + " Calculate charge weighted mean for " + signals.size() + " signals");
        
        double total_charge = 0;
        Hep3Vector position = new BasicHep3Vector(0, 0, 0);

        for (int istrip = 0; istrip < signals.size(); istrip++) {
            double signal = signals.get(istrip);

            total_charge += signal;
            position = VecOp.add(position, VecOp.mult(signal, positions.get(istrip)));
            if(_debug) System.out.println(this.getClass().getSimpleName() + "strip " + istrip+": signal " + signal + " position " + positions.get(istrip) + " -> total_position " + position.toString() + " ( total charge " + total_charge + ")");
            
        }
        position = VecOp.mult(1 / total_charge, position);
        if(_debug) System.out.println(this.getClass().getSimpleName() + " charge weighted position "+position.toString() + " (before trans)");
        electrodes.getParentToLocal().inverse().transform(position);
        if(_debug) System.out.println(this.getClass().getSimpleName() + " charge weighted position "+position.toString() + " (after trans)");
        
        // Swim position back through lorentz drift direction to midpoint between bias surfaces
        if(_simulation!=null) {
            _simulation.setSensor((SiSensor) electrodes.getDetectorElement());
            _simulation.lorentzCorrect(position, electrodes.getChargeCarrier());
            if(_debug) System.out.println(this.getClass().getSimpleName() + ": Position " + position.toString() + " ( after Lorentz)");
        }
        
        // return position in global coordinates
        Hep3Vector newpos = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        if(_debug) System.out.println(this.getClass().getSimpleName() + " final cluster position "+newpos.toString());
        
        return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
//        return electrodes.getLocalToGlobal().transformed(position);
    }

    private double getTime(List<HPSFittedRawTrackerHit> cluster) {
        int time_sum = 0;
        int signal_sum = 0;

        for (HPSFittedRawTrackerHit hit : cluster) {

            double signal = hit.getAmp();
            double time = hit.getT0();

            time_sum += time * signal;
            signal_sum += signal;

        }
        return (double) time_sum / (double) signal_sum;
    }

    private SymmetricMatrix getCovariance(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        SymmetricMatrix covariance = new SymmetricMatrix(3);
        covariance.setElement(0, 0, Math.pow(getMeasuredResolution(cluster, electrodes), 2));
        covariance.setElement(1, 1, Math.pow(getUnmeasuredResolution(cluster, electrodes), 2));
        covariance.setElement(2, 2, 0.0);

        SymmetricMatrix covariance_global = electrodes.getLocalToGlobal().transformed(covariance);

//        System.out.println("Global covariance matrix: \n"+covariance_global);

        return covariance_global;

//        BasicHep3Matrix rotation_matrix = (BasicHep3Matrix)electrodes.getLocalToGlobal().getRotation().getRotationMatrix();
//        BasicHep3Matrix rotation_matrix_transposed = new BasicHep3Matrix(rotation_matrix);
//        rotation_matrix_transposed.transpose();
//
////        System.out.println("Rotation matrix: \n"+rotation_matrix);
////        System.out.println("Rotation matrix transposed: \n"+rotation_matrix_transposed);
////        System.out.println("Local covariance matrix: \n"+covariance);
//
//        BasicHep3Matrix covariance_global = (BasicHep3Matrix)VecOp.mult(rotation_matrix,VecOp.mult(covariance,rotation_matrix_transposed));
//
////        System.out.println("Global covariance matrix: \n"+covariance_global);
//
//        return new SymmetricMatrix((Matrix)covariance_global);
    }

    private double getMeasuredResolution(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) // should replace this by a ResolutionModel class that gives expected resolution.  This could be a big job.
    {
        double measured_resolution;

        double sense_pitch = ((SiSensor) electrodes.getDetectorElement()).getSenseElectrodes(electrodes.getChargeCarrier()).getPitch(0);

//        double readout_pitch = electrodes.getPitch(0);
//        double noise = _readout_chip.getChannel(strip_number).computeNoise(electrodes.getCapacitance(strip_number));
//        double signal_expected = (0.000280/DopedSilicon.ENERGY_EHPAIR) *
//                ((SiSensor)electrodes.getDetectorElement()).getThickness(); // ~280 KeV/mm for thick Si sensors

        if (cluster.size() == 1) {
            measured_resolution = sense_pitch * _oneClusterErr;
        } else if (cluster.size() == 2) {
            measured_resolution = sense_pitch * _twoClusterErr;
        } else if (cluster.size() == 3) {
            measured_resolution = sense_pitch * _threeClusterErr;
        } else if (cluster.size() == 4) {
            measured_resolution = sense_pitch * _fourClusterErr;
        } else {
            measured_resolution = sense_pitch * _fiveClusterErr;
        }

        return measured_resolution;
    }

    private double getUnmeasuredResolution(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        // Get length of longest strip in hit
        double hit_length = 0;
        for (HPSFittedRawTrackerHit hit : cluster) {
            hit_length = Math.max(hit_length, ((SiStrips) electrodes).getStripLength(_strip_map.get(hit)));
        }
        return hit_length / Math.sqrt(12);
    }

    private double getEnergy(List<HPSFittedRawTrackerHit> cluster) {
        double total_charge = 0.0;
        for (HPSFittedRawTrackerHit hit : cluster) {
            double signal = hit.getAmp();
            total_charge += signal;
        }
        return total_charge * DopedSilicon.ENERGY_EHPAIR;
    }
}
