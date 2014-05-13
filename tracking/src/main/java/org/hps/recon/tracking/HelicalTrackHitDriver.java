package org.hps.recon.tracking;

import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hps.conditions.deprecated.StereoPair;
import org.hps.conditions.deprecated.StereoPair.detectorVolume;
import org.hps.conditions.deprecated.SvtUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 * @version $Id: HelicalTrackHitDriver.java,v 1.10 2013/10/17 22:08:33 omoreno
 * Exp $
 */
// TODO: Add class documentation.
public class HelicalTrackHitDriver extends org.lcsim.fit.helicaltrack.HelicalTrackHitDriver {

    private boolean _debug = false;
    private double _clusterTimeCut = -99; // if negative, don't cut..otherwise,
    // dt cut time in ns
    private String _subdetectorName = "Tracker";
    private final Map<String, String> _stereomap = new HashMap<String, String>();
    private final List<String> _colnames = new ArrayList<String>();
    private boolean _doTransformToTracking = true;

    public enum LayerGeometryType {

        Split, Common
    }
    private LayerGeometryType _layerGeometryType = LayerGeometryType.Split;

    /**
     * Default Ctor
     */
    public HelicalTrackHitDriver() {
        _colnames.add("StripClusterer_SiTrackerHitStrip1D");
    }

    // --- Setters ---//
    // ---------------//
    /**
     *
     * @param geomType
     */
    public void setLayerGeometryType(String geomType) {
        this._layerGeometryType = LayerGeometryType.valueOf(geomType);
    }

    /**
     *
     * @param dtCut
     */
    public void setClusterTimeCut(double dtCut) {
        this._clusterTimeCut = dtCut;
    }

    /**
     *
     * @param subdetectorName
     */
    public void setSubdetectorName(String subdetectorName) {
        this._subdetectorName = subdetectorName;
    }

    /**
     *
     * @param debug
     */
    public void setDebug(boolean debug) {
        this._debug = debug;
    }

    /**
     *
     * @param trans
     */
    public void setTransformToTracking(boolean trans) {
        this._doTransformToTracking = trans;
    }

    /**
     *
     * @param stripHitsCollectionName
     */
    public void setStripHitsCollectionName(String stripHitsCollectionName) {
        HitRelationName(stripHitsCollectionName);
    }

    /**
     *
     * @param helicalTrackHitRelationsCollectionName
     */
    public void setHelicalTrackHitRelationsCollectionName(String helicalTrackHitRelationsCollectionName) {
        HitRelationName(helicalTrackHitRelationsCollectionName);
    }

    /**
     *
     * @param helicalTrackMCRelationsCollectionName
     */
    public void setHelicalTrackMCRelationsCollectionName(String helicalTrackMCRelationsCollectionName) {
        MCRelationName(helicalTrackMCRelationsCollectionName);
    }

    /**
     *
     * @param outputHitCollectionName
     */
    public void setOutputHitCollectionName(String outputHitCollectionName) {
        OutputCollection(outputHitCollectionName);
    }

    @Override
    public void process(EventHeader event) {

        // Instantiate the list of HelicalTrackCrosses and HelicalTrackHits
        List<HelicalTrackCross> stereoCrosses = new ArrayList<HelicalTrackCross>();
        List<HelicalTrackHit> helhits = new ArrayList<HelicalTrackHit>();

        // Create an LCRelation from a HelicalTrackHit to
        List<LCRelation> hitrelations = new ArrayList<LCRelation>();

        // Create an LCRelation from a HelicalTrackHit to an MC particle used to
        // create it
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();

        RelationalTable hittomc = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    hittomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        for (String _colname : this._colnames) {
            if (!event.hasCollection(SiTrackerHit.class, _colname)) {
                if (_debug) {
                    System.out.println("Event: " + event.getRunNumber() + " does not contain the collection " + _colname);
                }
                continue;
            }

            // Get the list of SiTrackerHits for this collection
            List<SiTrackerHit> hitlist = event.get(SiTrackerHit.class, _colname);

            if (_debug) {
                System.out.printf("%s: found %d SiTrackerHits\n", this.getClass().getSimpleName(), hitlist.size());
            }

            Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap = new HashMap<HelicalTrackStrip, SiTrackerHitStrip1D>();

            for (SiTrackerHit hit : hitlist) {
                if (hit instanceof SiTrackerHitStrip1D) {

                    // Cast the hit as a 1D strip hit and find the
                    // identifier for the detector/layer combo
                    SiTrackerHitStrip1D h = (SiTrackerHitStrip1D) hit;

                    if ((_clusterTimeCut > 0 && Math.abs(h.getTime()) < _clusterTimeCut) || _clusterTimeCut < 0) {
                        // Create a HelicalTrackStrip for this hit
                        HelicalTrackStrip strip = makeDigiStrip(h);
                        for (RawTrackerHit rth : h.getRawHits()) {
                            for (Object simHit : hittomc.allFrom(rth)) {
                                strip.addMCParticle(((SimTrackerHit) simHit).getMCParticle());
                            }
                        }

                        // Map a reference back to the hit needed to create
                        // the stereo hit LC relations
                        stripmap.put(strip, h);

                        if (_debug) {
                            System.out.printf("%s: added strip org %s layer %d\n", this.getClass().getSimpleName(), strip.origin().toString(), strip.layer());
                        }
                    }
                } else {
                    // If not a 1D strip hit, make a pixel hit
                    // This should be removed as it is never used.
                    HelicalTrackHit hit3d = this.makeDigi3DHit(hit);
                    helhits.add(hit3d);
                    hitrelations.add(new MyLCRelation(hit3d, hit));
                }
            }

            List<HelicalTrackCross> helicalTrackCrosses = new ArrayList<HelicalTrackCross>();

            if (LayerGeometryType.Common == _layerGeometryType) {

                // Create collections for strip hits by layer and hit cross
                // references
                Map<String, List<HelicalTrackStrip>> striplistmap = new HashMap<String, List<HelicalTrackStrip>>();

                for (HelicalTrackStrip strip : stripmap.keySet()) {
                    IDetectorElement de = stripmap.get(strip).getSensor();
                    String id = this.makeID(_ID.getName(de), _ID.getLayer(de));

                    // This hit should be a on a stereo pair!
                    // With our detector setup, when is this not true?
                    if (!_stereomap.containsKey(id) && !_stereomap.containsValue(id)) {
                        throw new RuntimeException(this.getClass().getSimpleName() + ": this " + id + " was not among the stereo modules!");
                    }

                    // Get the list of strips for this layer - create a new
                    // list if one doesn't already exist
                    List<HelicalTrackStrip> lyrhits = striplistmap.get(id);
                    if (lyrhits == null) {
                        lyrhits = new ArrayList<HelicalTrackStrip>();
                        striplistmap.put(id, lyrhits);
                    }

                    // Add the strip to the list of strips on this
                    // sensor
                    lyrhits.add(strip);
                }

                if (_debug) {
                    System.out.printf("%s: Create stereo hits from %d strips \n", this.getClass().getSimpleName(), striplistmap.size());
                }

                // Loop over the stereo layer pairs
                // TODO: Change this so that it makes use of StereoPairs
                for (String id1 : _stereomap.keySet()) {
                    // Get the second layer
                    String id2 = _stereomap.get(id1);

                    if (_debug) {
                        System.out.printf("%s: Form stereo hits from sensor id %s with %d hits and %s with %d hits\n", this.getClass().getSimpleName(), id1, striplistmap.get(id1) == null ? 0 : striplistmap.get(id1).size(), id2, striplistmap.get(id2) == null ? 0 : striplistmap.get(id2).size());
                    }

                    // Form the stereo hits and add them to our hit list
                    helicalTrackCrosses.addAll(_crosser.MakeHits(striplistmap.get(id1), striplistmap.get(id2)));
                } // End of loop over stereo pairs
            } else {

                Map<SiSensor, List<HelicalTrackStrip>> striplistmap = new HashMap<SiSensor, List<HelicalTrackStrip>>();

                for (HelicalTrackStrip strip : stripmap.keySet()) {
                    SiSensor sensor = stripmap.get(strip).getSensor();

                    List<HelicalTrackStrip> hitsOnSensor = striplistmap.get(sensor);

                    // If no hits on that sensor yet -> create the list
                    if (hitsOnSensor == null) {
                        hitsOnSensor = new ArrayList<HelicalTrackStrip>();
                        striplistmap.put(sensor, hitsOnSensor);
                    }

                    // Add the strip to the list of strips on this sensor
                    hitsOnSensor.add(strip);
                }

                for (StereoPair stereoPair : SvtUtils.getInstance().getStereoPairs()) {

                    // Form the stereo hits and add them to our hit list
                    List<HelicalTrackCross> newCrosses;

                    if (stereoPair.getDetectorVolume() == detectorVolume.Top) {
                        newCrosses = _crosser.MakeHits(striplistmap.get(stereoPair.getAxialSensor()), striplistmap.get(stereoPair.getStereoSensor()));
                    } else if (stereoPair.getDetectorVolume() == detectorVolume.Bottom) {
                        newCrosses = _crosser.MakeHits(striplistmap.get(stereoPair.getStereoSensor()), striplistmap.get(stereoPair.getAxialSensor()));
                    } else {
                        throw new RuntimeException("stereo pair is neither top nor bottom");
                    }

                    if (_debug) {
                        System.out.printf("%s: Found %d stereo hits from sensors\n%s: %s : %d hits\n%s: %s with %d hits\n", this.getClass().getSimpleName(), newCrosses.size(), this.getClass().getSimpleName(), stereoPair.getAxialSensor().getName(), striplistmap.get(stereoPair.getAxialSensor()) == null ? 0 : striplistmap.get(stereoPair.getAxialSensor()).size(), this.getClass().getSimpleName(), stereoPair.getStereoSensor().getName(), striplistmap.get(stereoPair.getStereoSensor()) == null ? 0 : striplistmap.get(stereoPair.getStereoSensor()).size());
                    }

                    helicalTrackCrosses.addAll(newCrosses);
                } // Loop over stereo pairs
            }

            for (HelicalTrackCross cross : helicalTrackCrosses) {
                if (cross.getMCParticles() != null) {
                    for (MCParticle mcp : cross.getMCParticles()) {
                        mcrelations.add(new MyLCRelation((HelicalTrackHit) cross, mcp));
                    }
                }
                for (HelicalTrackStrip strip : cross.getStrips()) {
                    hitrelations.add(new MyLCRelation(cross, stripmap.get(strip)));
                }
                if (_debug) {
                    System.out.printf("%s: cross at %.2f,%.2f,%.2f \n", this.getClass().getSimpleName(), cross.getPosition()[0], cross.getPosition()[1], cross.getPosition()[2]);
                }
            }

            stereoCrosses.addAll(helicalTrackCrosses);

            if (_debug) {
                System.out.printf("%s: added %d stereo hits from %s collection \n", this.getClass().getSimpleName(), helicalTrackCrosses.size(), _colname);
            }
        } // End of loop over collection names

        if (_debug) {
            System.out.printf("%s: totally added %d stereo hits:\n", this.getClass().getSimpleName(), stereoCrosses.size());
            for (HelicalTrackCross cross : stereoCrosses) {
                System.out.printf("%s: %.2f,%.2f,%.2f \n", this.getClass().getSimpleName(), cross.getPosition()[0], cross.getPosition()[1], cross.getPosition()[2]);
            }
        }

        // Add things to the event
        // Cast crosses to HTH
        helhits.addAll(stereoCrosses);
        event.put(_outname, helhits, HelicalTrackHit.class, 0);
        event.put(_hitrelname, hitrelations, LCRelation.class, 0);
        event.put(_mcrelname, mcrelations, LCRelation.class, 0);
        if (_doTransformToTracking) {
            addRotatedHitsToEvent(event, stereoCrosses);
        }

    } // Process()

    public void addCollection(String colname) {
        _colnames.add(colname);
    }

    public void setCollection(String colname) {
        _colnames.clear();
        this.addCollection(colname);
    }

    private String makeID(String detname, int lyr) {
        return detname + lyr;
    }

    public void setStereoPair(String detname, int lyr1, int lyr2) {
        this._stereomap.put(this.makeID(detname, lyr1), this.makeID(detname, lyr2));
    }

    @Override
    protected void detectorChanged(Detector detector) {

        /*
         * Setup default pairing
         */
        if (_debug) {
            System.out.printf("%s: Setup stereo hit pair modules \n", this.getClass().getSimpleName());
        }

        List<SiTrackerModule> modules = detector.getSubdetector(this._subdetectorName).getDetectorElement().findDescendants(SiTrackerModule.class);

        if (modules.isEmpty()) {
            throw new RuntimeException(this.getClass().getName() + ": No SiTrackerModules found in detector.");
        }

        if (LayerGeometryType.Common == this._layerGeometryType) {

            int nLayersTotal = detector.getSubdetector(_subdetectorName).getLayering().getLayers().getNumberOfLayers();
            if (_debug) {
                System.out.printf("%s: %d layers \n", this.getClass().getSimpleName(), nLayersTotal);
            }
            if (nLayersTotal % 2 != 0) {
                throw new RuntimeException(this.getClass().getName() + ": Don't know how to do stereo pairing for odd number of modules.");
            }
            for (int i = 1; i <= (nLayersTotal) - 1; i += 2) {
                if (_debug) {
                    System.out.printf("%s: Adding stereo pair: %d,%d\n", this.getClass().getSimpleName(), i, i + 1);
                }
                setStereoPair(_subdetectorName, i, i + 1);
            }
        }

        if (_debug) {
            System.out.printf("%s: %d stereo modules added", this.getClass().getSimpleName(), this._stereomap.size());
        }

    }

    private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {

        SiTrackerHitStrip1D local = h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        SiTrackerHitStrip1D global = h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(_orgloc);
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();

        double umeas = local.getPosition()[0];
        double vmin = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getStartPoint());
        double vmax = VecOp.dot(local.getUnmeasuredCoordinate(), local.getHitSegment().getEndPoint());
        double du = Math.sqrt(local.getCovarianceAsMatrix().diagonal(0));

        IDetectorElement de = h.getSensor();
        String det = _ID.getName(de);
        int lyr = _ID.getLayer(de);
        BarrelEndcapFlag be = _ID.getBarrelEndcapFlag(de);

        double dEdx = h.getdEdx();
        double time = h.getTime();
        List<RawTrackerHit> rawhits = h.getRawHits();
        HelicalTrackStrip strip = new HelicalTrackStrip(org, u, v, umeas, du, vmin, vmax, dEdx, time, rawhits, det, lyr, be);

        try {
            if (h.getMCParticles() != null) {
                for (MCParticle p : h.getMCParticles()) {
                    strip.addMCParticle(p);
                }
            }
        } catch (RuntimeException e) {
            // Okay when MC info not present.
        }

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": produced HelicalTrackStrip with origin " + strip.origin().toString());
        }

        return strip;
    }

    private void addRotatedHitsToEvent(EventHeader event, List<HelicalTrackCross> stereohits) {

        List<HelicalTrackHit> rotatedhits = new ArrayList<HelicalTrackHit>();
        List<LCRelation> hthrelations = new ArrayList<LCRelation>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        for (HelicalTrackCross cross : stereohits) {
            List<HelicalTrackStrip> rotatedstriphits = new ArrayList<HelicalTrackStrip>();
            for (HelicalTrackStrip strip : cross.getStrips()) {

                Hep3Vector origin = strip.origin();
                Hep3Vector u = strip.u();
                Hep3Vector v = strip.v();
                double umeas = strip.umeas();
                double du = strip.du();
                double vmin = strip.vmin();
                double vmax = strip.vmax();
                double dedx = strip.dEdx();
                double time = strip.time();
                List<RawTrackerHit> rthList = strip.rawhits();
                String detname = strip.detector();
                int layer = strip.layer();
                BarrelEndcapFlag bec = strip.BarrelEndcapFlag();
                Hep3Vector neworigin = CoordinateTransformations.transformVectorToTracking(origin);
                Hep3Vector newu = CoordinateTransformations.transformVectorToTracking(u);
                Hep3Vector newv = CoordinateTransformations.transformVectorToTracking(v);
                HelicalTrackStrip newstrip = new HelicalTrackStrip(neworigin, newu, newv, umeas, du, vmin, vmax, dedx, time, rthList, detname, layer, bec);
                for (MCParticle p : strip.MCParticles()) {
                    newstrip.addMCParticle(p);
                }
                rotatedstriphits.add(newstrip);
            }
            HelicalTrackCross newhit = new HelicalTrackCross(rotatedstriphits.get(0), rotatedstriphits.get(1));
            for (MCParticle mcp : cross.getMCParticles()) {
                newhit.addMCParticle(mcp);
            }
            rotatedhits.add(newhit);
            hthrelations.add(new MyLCRelation(cross, newhit));
            for (MCParticle mcp : newhit.getMCParticles()) {
                mcrelations.add(new MyLCRelation(newhit, mcp));
            }
        }

        event.put("Rotated" + _outname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated" + _hitrelname, hthrelations, LCRelation.class, 0);
        event.put("Rotated" + _mcrelname, mcrelations, LCRelation.class, 0);
    }
}
