package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.Hep3Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiStriplets;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.ThinSiStrips;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

import org.hps.readout.svt.HPSSVTConstants;

/**
 *
 * @author Matt Graham
 */
// TODO: Add class documentation.
public class StripMaker {

    private static String _NAME = "HPSStripClusterer";
    // Clustering algorithm
    ClusteringAlgorithm _clustering;
    // Number of strips beyond which charge is averaged on center strips
    int _max_noaverage_nstrips = 4;
    // Absolute maximum cluster size
    int _max_cluster_nstrips = 10;
    // Sensor simulation needed to correct for Lorentz drift
    SiSensorSim _simulation;
    // Identifier helper (reset once per sensor)
    SiTrackerIdentifierHelper _sid_helper;
    // Temporary map connecting hits to strip numbers for sake of speed (reset once per sensor)
    //Map<FittedRawTrackerHit, Integer> _strip_map = new HashMap<FittedRawTrackerHit, Integer>();
    Map< LCRelation, Integer > stripMap_ = new HashMap< LCRelation, Integer >(); 

    boolean _debug = false;
    private SiliconResolutionModel _res_model = new DefaultSiliconResolutionModel();

    public void setResolutionModel(SiliconResolutionModel model) {
        _res_model = model;
    }

    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    public StripMaker(ClusteringAlgorithm algo) {
        _clustering = algo;
    }

    public StripMaker(SiSensorSim simulation, ClusteringAlgorithm algo) {
        _clustering = algo;
        _simulation = simulation;
    }

    public String getName() {
        return _NAME;
    }

    // Make hits for all sensors within a DetectorElement
    public List<SiTrackerHit> makeHits(IDetectorElement detector) {
        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();
        List<SiSensor> sensors = detector.findDescendants(SiSensor.class);

        // Loop over all sensors
        for (SiSensor sensor : sensors)
            if (sensor.hasStrips())
                hits.addAll(makeHits(sensor));

        // Return hit list
        return hits;
    }

    // Make hits for a sensor
    public List<SiTrackerHit> makeHits(SiSensor sensor) {

        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();

        // Get SiTrackerIdentifierHelper for this sensor and refresh the 
        // strip map used to increase speed
        _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        stripMap_.clear();

        // Get the collection of LCRelations (RawTrackerHit to fit parameters)
        // from the sensor readout. 
        List< LCRelation > fittedHits = sensor.getReadout().getHits(LCRelation.class);

        // Map out the electrodes to the corresponding hit
        // TODO: Do we need a map for each sensor type? 
        // TODO: Are we still using the thin sensors class? 
        Map<SiSensorElectrodes, List<LCRelation>> electrode_hits = new LinkedHashMap<SiSensorElectrodes, List<LCRelation>>();
        Map<SiSensorElectrodes, List<LCRelation>> thin_hits = new LinkedHashMap<SiSensorElectrodes, List<LCRelation>>();
        Map<SiSensorElectrodes, List<LCRelation>> stripletHits = new LinkedHashMap<SiSensorElectrodes, List<LCRelation>>(); 

        for (LCRelation fittedHit : fittedHits) {

            // Get the RawTrackerHit associated with the fitted hit.
            RawTrackerHit hit = FittedRawTrackerHit.getRawTrackerHit(fittedHit);

            // Get the ID and create strip map, get electrodes.
            IIdentifier id = hit.getIdentifier();
            int strip = _sid_helper.getElectrodeValue(id); 
            
            // Drop the unbonded channel
            if (strip == HPSSVTConstants.TOTAL_STRIPS_PER_SENSOR) continue;

            stripMap_.put(fittedHit, _sid_helper.getElectrodeValue(id));

            // Get electrodes and check that they are strips
            ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
            SiSensorElectrodes electrodes = ((SiSensor) hit.getDetectorElement()).getReadoutElectrodes(carrier);

            if (electrodes instanceof ThinSiStrips) {
                if (thin_hits.get(electrodes) == null)
                    thin_hits.put(electrodes, new ArrayList<LCRelation>());                    
                thin_hits.get(electrodes).add(fittedHit);
            } else if (electrodes instanceof SiStriplets) {

                if (_debug) System.out.println("StripMaker::makeHits : Electrodes are of SiStriplets"); 
                
                if (stripletHits.get(electrodes) == null) stripletHits.put(electrodes, new ArrayList<LCRelation>()); 

                stripletHits.get(electrodes).add(fittedHit);

            } else {
                if (electrode_hits.get(electrodes) == null)
                    electrode_hits.put(electrodes, new ArrayList<LCRelation>());
                electrode_hits.get(electrodes).add(fittedHit);
            }
        }

        for (Map.Entry entry : electrode_hits.entrySet())
            hits.addAll(makeHits(sensor, (SiStrips) entry.getKey(), (List<LCRelation>) entry.getValue()));
        for (Map.Entry entry : thin_hits.entrySet())
            hits.addAll(makeHits(sensor, (ThinSiStrips) entry.getKey(), (List<LCRelation>) entry.getValue()));
        for (Map.Entry entry : stripletHits.entrySet())
            hits.addAll(makeHits(sensor, (SiStriplets) entry.getKey(), (List<LCRelation>) entry.getValue()));


        if (_debug)
            System.out.println(this.getClass().getSimpleName() + "::makeHits returning " + hits.size() + " clusters from sensor "+sensor.getName());
        return hits;
    }

    public List<SiTrackerHit> makeHits(SiSensor sensor, SiSensorElectrodes electrodes, List<LCRelation> fittedHits) {

        // Call the clustering algorithm to make clusters
        List<List<LCRelation>> cluster_list = _clustering.findClusters(fittedHits);
        if (_debug)
            System.out.println(this.getClass().getSimpleName() + "::makeHits : Found clusters = " + cluster_list.size());
        // Create an empty list for the pixel hits to be formed from clusters
        List<SiTrackerHit> hits = new ArrayList<SiTrackerHit>();

        // Make a pixel hit from this cluster
        for (List<LCRelation> cluster : cluster_list)

            // Make a TrackerHit from the cluster if it meets max cluster size requirement
            if (cluster.size() <= _max_cluster_nstrips) {
                SiTrackerHitStrip1D hit = makeTrackerHit(cluster, electrodes);
                // Add to readout and to list of hits
                // ((SiSensor) electrodes.getDetectorElement()).getReadout().addHit(hit);
                hits.add(hit);
                sensor.getReadout().addHit(hit);
            }
        if (_debug)
            System.out.println(this.getClass().getSimpleName() + "::makeHits : Returning " + hits.size() + " hits ");
        return hits;
    }

    public void setCentralStripAveragingThreshold(int max_noaverage_nstrips) {
        _max_noaverage_nstrips = max_noaverage_nstrips;
    }

    public void setMaxClusterSize(int max_cluster_nstrips) {
        _max_cluster_nstrips = max_cluster_nstrips;
    }

    private SiTrackerHitStrip1D makeTrackerHit(List<LCRelation> cluster, SiSensorElectrodes electrodes) {
        
        Hep3Vector position = getPosition(cluster, electrodes);
        if (_debug) System.out.println("StripMaker::makeTrackerHit : Cluster position: " + position.toString()); 
        
        SymmetricMatrix covariance = getCovariance(cluster, electrodes);
        if (_debug) System.out.println("StripMaker::makeTrackerHit : Cluster covariance: " + covariance.toString()); 
        
        double time = getTime(cluster);
        double energy = getEnergy(cluster);
        TrackerHitType type = new TrackerHitType(TrackerHitType.CoordinateSystem.GLOBAL, TrackerHitType.MeasurementType.STRIP_1D);
        List<RawTrackerHit> rth_cluster = new ArrayList<RawTrackerHit>();
        for (LCRelation bth : cluster)
            rth_cluster.add(FittedRawTrackerHit.getRawTrackerHit(bth)); 
        SiTrackerHitStrip1D hit = new SiTrackerHitStrip1D(position, covariance, energy, time, rth_cluster, type);
        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " SiTrackerHitStrip1D created at " + position + "(" + hit.getPositionAsVector().toString() + ")" + " E " + energy + " time " + time);
        return hit;
    }

    private Hep3Vector getPosition(List<LCRelation> cluster, SiSensorElectrodes electrodes) {
        if (_debug) System.out.println("StripMaker::getPosition : Calculating position for cluster size " + cluster.size());
        List<Double> signals = new ArrayList<Double>();
        List<Hep3Vector> positions = new ArrayList<Hep3Vector>();

        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " Loop of " + cluster.size() + " and add signals and positions to vectors");

        for (LCRelation hit : cluster) {
            signals.add(FittedRawTrackerHit.getAmp(hit));
            if (electrodes instanceof ThinSiStrips) {
                positions.add(((ThinSiStrips) electrodes).getStripCenter(stripMap_.get(hit)));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("thinStripCenter is at " + ((ThinSiStrips) electrodes).getStripCenter(stripMap_.get(hit)).toString());
            } else if (electrodes instanceof SiStriplets) {
                Hep3Vector position = ((SiStriplets) electrodes).getStripCenter(stripMap_.get(hit));
                if (_debug) System.out.println("StripMaker::getPosition : Position " + position.toString());
                positions.add(position);
            } else {
                positions.add(((SiStrips) electrodes).getStripCenter(stripMap_.get(hit)));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("stripCenter is at " + ((SiStrips) electrodes).getStripCenter(stripMap_.get(hit)).toString());
            }
        }

        // Average charge on central strips of longer clusters
        if (signals.size() > _max_noaverage_nstrips) {
            int nstrips_center = signals.size() - 2;

            // collect sum of charges on center strips
            double center_charge_sum = 0.0;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++)
                center_charge_sum += signals.get(istrip);

            // distribute evenly on center strips
            double center_charge_strip = center_charge_sum / nstrips_center;
            for (int istrip = 1; istrip < signals.size() - 1; istrip++)
                signals.set(istrip, center_charge_strip);
        }

        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " Calculate charge weighted mean for " + signals.size() + " signals");

        Hep3Vector position = _res_model.weightedAveragePosition(signals, positions);

        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (before trans)");
        electrodes.getParentToLocal().inverse().transform(position);
        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (after trans)");

        // Swim position back through lorentz drift direction to midpoint between bias surfaces
        if (_simulation != null) {
            _simulation.setSensor((SiSensor) electrodes.getDetectorElement());
            _simulation.lorentzCorrect(position, electrodes.getChargeCarrier());
            if (_debug)
                System.out.println(this.getClass().getSimpleName() + ": Position " + position.toString() + " ( after Lorentz)");
        }

        // return position in global coordinates
        Hep3Vector newpos = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        if (_debug)
            System.out.println(this.getClass().getSimpleName() + " final cluster position " + newpos.toString());

        return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
    }

    private double getTime(List<LCRelation> cluster) {
        double time_sum = 0;
        double signal_sum = 0;

        for (LCRelation hit : cluster) {

            double signal = FittedRawTrackerHit.getAmp(hit);
            double time = FittedRawTrackerHit.getT0(hit);

            time_sum += time * signal * signal;
            signal_sum += signal * signal;

        }
        return time_sum / signal_sum;
    }

    private SymmetricMatrix getCovariance(List<LCRelation> cluster, SiSensorElectrodes electrodes) {
        SymmetricMatrix covariance = new SymmetricMatrix(3);
        covariance.setElement(0, 0, Math.pow(_res_model.getMeasuredResolution(cluster, electrodes), 2));
        covariance.setElement(1, 1, Math.pow(_res_model.getUnmeasuredResolution(cluster, electrodes, stripMap_), 2));
        covariance.setElement(2, 2, 0.0);

        SymmetricMatrix covariance_global = electrodes.getLocalToGlobal().transformed(covariance);

        // System.out.println("Global covariance matrix: \n"+covariance_global);
        return covariance_global;

        // BasicHep3Matrix rotation_matrix =
        // (BasicHep3Matrix)electrodes.getLocalToGlobal().getRotation().getRotationMatrix();
        // BasicHep3Matrix rotation_matrix_transposed = new BasicHep3Matrix(rotation_matrix);
        // rotation_matrix_transposed.transpose();
        //
        // // System.out.println("Rotation matrix: \n"+rotation_matrix);
        // // System.out.println("Rotation matrix transposed: \n"+rotation_matrix_transposed);
        // // System.out.println("Local covariance matrix: \n"+covariance);
        //
        // BasicHep3Matrix covariance_global =
        // (BasicHep3Matrix)VecOp.mult(rotation_matrix,VecOp.mult(covariance,rotation_matrix_transposed));
        //
        // // System.out.println("Global covariance matrix: \n"+covariance_global);
        //
        // return new SymmetricMatrix((Matrix)covariance_global);
    }

    private double getEnergy(List<LCRelation> cluster) {
        double total_charge = 0.0;
        for (LCRelation hit : cluster) {
            double signal = FittedRawTrackerHit.getAmp(hit);
            total_charge += signal;
        }
        return total_charge * DopedSilicon.ENERGY_EHPAIR;
    }
    
}
