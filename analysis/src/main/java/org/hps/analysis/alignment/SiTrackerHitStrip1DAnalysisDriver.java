package org.hps.analysis.alignment;

import hep.physics.vec.Hep3Vector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.recon.tracking.DefaultSiliconResolutionModel;
import org.hps.recon.tracking.FittedRawTrackerHit;
import org.hps.recon.tracking.SiliconResolutionModel;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.detector.tracker.silicon.ThinSiStrips;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.CDFSiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiSensorSim;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class SiTrackerHitStrip1DAnalysisDriver extends Driver {

    private AIDA aida = AIDA.defaultInstance();
    //Collection Strings
    private String fittedHitsCollectionName = "SVTFittedRawTrackerHits";
    String rawTrackerHitCollectionName = "SVTRawTrackerHits";
    Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();

    protected void process(EventHeader event) {

        // Get the list of fitted hits from the event
        List<LCRelation> fittedHits = event.get(LCRelation.class, fittedHitsCollectionName);
        fittedRawTrackerHitMap.clear();
        // Map the fitted hits to their corresponding raw hits
        for (LCRelation fittedHit : fittedHits) {
            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
        }

//        List<RawTrackerHit> rawHits = event.get(RawTrackerHit.class, rawTrackerHitCollectionName);
        setupSensors(event);
        List<SiTrackerHitStrip1D> stripClusters = event.get(SiTrackerHitStrip1D.class, "StripClusterer_SiTrackerHitStrip1D");
        int nStripHits = stripClusters.size();
        aida.histogram1D("number of strip clusters in the event", 100, 0., 200.).fill(nStripHits);
        // lets partition the strip clusters into each module
        Map<String, List<SiTrackerHitStrip1D>> hitsPerModuleMap = new HashMap<>();
        System.out.println("found " + stripClusters.size() + " strip clusters");
        for (TrackerHit thit : stripClusters) {
            List<RawTrackerHit> hits = thit.getRawHits();
            System.out.println(" Strip Cluster at " + Arrays.toString(thit.getPosition()) + " has " + hits.size() + " strip hits");
            for (RawTrackerHit hit : hits) {
                SiSensor sensor = (SiSensor) hit.getDetectorElement();
                SiSensorElectrodes electrodes = sensor.getReadoutElectrodes(ChargeCarrier.HOLE);
                System.out.println(Arrays.toString(hit.getPosition()));

                System.out.println(hit.getDetectorElement().getGeometry().getPosition());
            }
            Hep3Vector clusterPos = getPosition(hits, ((SiSensor) hits.get(0).getDetectorElement()).getReadoutElectrodes(ChargeCarrier.HOLE));
            System.out.println("recalculated cluster position "+clusterPos);
//            String moduleName = ((RawTrackerHit) rthList.get(0)).getDetectorElement().getName();
//            if (!hitsPerModuleMap.containsKey(moduleName)) {
//                hitsPerModuleMap.put(moduleName, new ArrayList<SiTrackerHitStrip1D>());
//                hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
//            } else {
//                hitsPerModuleMap.get(moduleName).add(new SiTrackerHitStrip1D(hit));
//            }
        }
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;

        if (event.hasCollection(RawTrackerHit.class,
                "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class,
                    "SVTRawTrackerHits");

        }
        if (event.hasCollection(RawTrackerHit.class,
                "RawTrackerHitMaker_RawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class,
                    "RawTrackerHitMaker_RawTrackerHits");
        }
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

    //from StripMaker
    private Hep3Vector getPosition(List<RawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        boolean _debug = true;
        SiTrackerIdentifierHelper _sid_helper;
        //_sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        // Temporary map connecting hits to strip numbers for sake of speed (reset once per sensor)
//        Map<FittedRawTrackerHit, Integer> _strip_map = new HashMap<FittedRawTrackerHit, Integer>();
//// get id and create strip map, get electrodes.
//            IIdentifier id = hps_hit.getRawTrackerHit().getIdentifier();
//            _strip_map.put(hps_hit, _sid_helper.getElectrodeValue(id));

// Number of strips beyond which charge is averaged on center strips
        int _max_noaverage_nstrips = 4;

        SiliconResolutionModel _res_model = new DefaultSiliconResolutionModel();

        // Sensor simulation needed to correct for Lorentz drift
        SiSensorSim _simulation = new CDFSiSensorSim();

        //cng
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " getPosition for cluster size " + cluster.size());
        }
        List<Double> signals = new ArrayList<Double>();
        List<Hep3Vector> positions = new ArrayList<Hep3Vector>();

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " Loop of " + cluster.size() + " and add signals and positions to vectors");
        }

        for (RawTrackerHit hit : cluster) {
            _sid_helper = (SiTrackerIdentifierHelper) hit.getDetectorIdentifierHelper();
            signals.add(FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(hit)));
            if (electrodes instanceof ThinSiStrips) {
                positions.add(((ThinSiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("thinStripCenter is at " + ((ThinSiStrips) electrodes).getStripCenter(_strip_map.get(hit)).toString());
            } else {
                positions.add(((SiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
//                if (hit.getRawTrackerHit().getLayerNumber() < 4)
//                    System.out.println("stripCenter is at " + ((SiStrips) electrodes).getStripCenter(_strip_map.get(hit)).toString());
            }
            if (_debug) {
                System.out.println(this.getClass().getSimpleName() + " Added hit with signal " + FittedRawTrackerHit.getAmp(fittedRawTrackerHitMap.get(hit)) + " at strip center posiiton " + ((SiStrips) electrodes).getStripCenter(_sid_helper.getElectrodeValue(hit.getIdentifier())));
            }
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

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " Calculate charge weighted mean for " + signals.size() + " signals");
        }

        Hep3Vector position = _res_model.weightedAveragePosition(signals, positions);

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (before trans)");
        }
        electrodes.getParentToLocal().inverse().transform(position);
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " charge weighted position " + position.toString() + " (after trans)");
        }

        // Swim position back through lorentz drift direction to midpoint between bias surfaces
        if (_simulation != null) {
            _simulation.setSensor((SiSensor) electrodes.getDetectorElement());
            _simulation.lorentzCorrect(position, electrodes.getChargeCarrier());
            if (_debug) {
                System.out.println(this.getClass().getSimpleName() + ": Position " + position.toString() + " ( after Lorentz)");
            }
        }

        // return position in global coordinates
        Hep3Vector newpos = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + " final cluster position " + newpos.toString());
        }

        return ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);
        // return electrodes.getLocalToGlobal().transformed(position);
    }

}
