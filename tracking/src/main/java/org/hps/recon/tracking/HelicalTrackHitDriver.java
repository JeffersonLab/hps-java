package org.hps.recon.tracking;

import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;

import org.hps.recon.tracking.axial.HelicalTrack2DHit;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.converter.compact.subdetector.HpsTracker2;
import org.lcsim.detector.converter.compact.subdetector.SvtStereoLayer;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.SiTrackerModule;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.event.base.MyLCRelation;
import org.lcsim.fit.helicaltrack.HelicalTrackCross;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelicalTrackStrip;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;

/**
 * This <code>Driver</code> creates 3D hits from SVT strip clusters of stereo pairs, which by default
 * are read from the <b>StripClusterer_SiTrackerHitStrip1D</b> input collection.
 * <p>
 * The following collections will be added to the output event:
 * <ul>
 * <li>HelicalTrackHits</li>
 * <li>RotatedHelicalTrackHits</li>
 * <li>HelicalTrackHitRelations</li>
 * <li>RotatedHelicalTrackHitRelations</li>
 * <li>HelicalTrackMCRelations</li>
 * <li>RotatedHelicalTrackMCRelations<li>
 * </ul>
 * <p>
 * Class has the following default parameters values in the code (or from <code>EngineeringRun2015FullRecon.lcsim</code>):
 * <ul>
 * <li>{@link #setClusterTimeCut(double)} - 12.0 (ns)</li>
 * <li>{@link #setMaxDt(double)} - 16.0 (ns)</li>
 * <li>{@link #setClusterAmplitudeCut(double)} - 400.0</li>
 * <li>{@link #setRejectGhostHits(boolean)} - <code>false</code></li>
 * <li>{@link #setDebug(boolean)} - <code>false</code></li>
 * <li>{@link #setEpsParallel(double)} - 0.013</li>
 * <li>{@link #setEpsStereo(double)} - 0.01</li>
 * <li>{@link #setSaveAxialHits(boolean)} - <code>false</code></li>
 * <li>{@link #setStripHitsCollectionName(String)} - StripClusterer_SiTrackerHitStrip1D</li>
 * <li>{@link #setHelicalTrackHitRelationsCollectionName(String)} - HelicalTrackHitRelations</li>
 * <li>{@link #setHelicalTrackMCRelationsCollectionName(String)} -  HelicalTrackMCRelations</li>
 * <li>{@link #setOutputHitCollectionName(String)} - HelicalTrackHits</li>
 * </ul>
 *
 * @author Mathew Graham <mgraham@slac.stanford.edu>
 * @author Per Hansson <phansson@slac.stanford.edu>
 * @author Omar Moreno <omoreno1@ucsc.edu>
 */

public class HelicalTrackHitDriver extends org.lcsim.fit.helicaltrack.HelicalTrackHitDriver {

    private boolean _debug = false;
    private double _clusterTimeCut = -99; // if negative, don't cut..otherwise,
    // dt cut time in ns
    private double maxDt = -99; // max time difference between the two hits in a cross
    private double clusterAmplitudeCut = -99; // cluster amplitude cut
    private String _subdetectorName = "Tracker";
    private final Map<String, String> _stereomap = new LinkedHashMap<String, String>();
    private List<SvtStereoLayer> stereoLayers = null;
    private final List<String> _colnames = new ArrayList<String>();
    private boolean _doTransformToTracking = true;
    private boolean _saveAxialHits = false;
    private final String _axialname = "AxialTrackHits";
    private final String _axialmcrelname = "AxialTrackHitsMCRelations";
    private boolean rejectGhostHits = false;
    private boolean allowHoleSlotCombo = false;

    /**
     * Default Ctor
     */
    public HelicalTrackHitDriver() {
        _crosser.setMaxSeparation(20.0);
        _crosser.setTolerance(0.1);
        _crosser.setEpsParallel(0.013);
        _colnames.add("StripClusterer_SiTrackerHitStrip1D");
    }
    
    public void setAllowHoleSlotCombo(boolean input) {
        allowHoleSlotCombo = input;
    }

    public boolean getAllowHoleSlotCombo() {
        return allowHoleSlotCombo;
    }

    /**
     *
     * @param dtCut
     */
    public void setClusterTimeCut(double dtCut) {
        this._clusterTimeCut = dtCut;
    }

    public void setMaxDt(double maxDt) {
        this.maxDt = maxDt;
    }

    public void setClusterAmplitudeCut(double clusterAmplitudeCut) {
        this.clusterAmplitudeCut = clusterAmplitudeCut;
    }

    /**
     * Drop any HelicalTrackHit containing a 1D hit that is also used in another
     * HelicalTrackHit.
     *
     * @param rejectGhostHits
     */
    public void setRejectGhostHits(boolean rejectGhostHits) {
        this.rejectGhostHits = rejectGhostHits;
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

    public void setEpsParallel(double eps) {
        this._crosser.setEpsParallel(eps);
    }

    public void setEpsStereo(double eps) {
        this._crosser.setEpsStereoAngle(eps);
    }

    /**
     *
     * @param trans
     */
    public void setTransformToTracking(boolean trans) {
        this._doTransformToTracking = trans;
    }

    public void setSaveAxialHits(boolean save) {
        _saveAxialHits = save;
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
        RelationalTable hittomc = null;
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            hittomc = new BaseRelationalTable(RelationalTable.Mode.ONE_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    hittomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }

        List<HelicalTrack2DHit> axialhits = new ArrayList<>();
        List<LCRelation> axialmcrelations = new ArrayList<LCRelation>();
        // Loop over the input collection names we want to make hits out of
        // ...for HPS, probably this is just a single one...
        for (String _colname : this._colnames) {
            
            List<SiTrackerHit> hitlist = new ArrayList<>();
            List<TrackerHit> trk_hitlist = null;
            
            //First try to get the SiTracker hits. If that fails get them as TrackerHit and form the SiTrackerHitStrip1D

            //Check if SiTrackerHit collection named _colname is in the event
            if (!event.hasCollection(SiTrackerHit.class, _colname)) {
                if (_debug) {
                    System.out.println("Event: " + event.getRunNumber() + " does not contain the collection " + _colname);
                    System.out.println("Will try to get it as TrackerHit");
                }
                //Check if TrackerHit collection named _colname is in the event
                if (!event.hasCollection(TrackerHit.class, _colname)) {
                    if (_debug) {
                        System.out.println("Collection " + _colname + " doesn't exist in event");
                    }
                    //If not let event go
                    continue;
                }
                //If found the TrackerHit Collection with name _colname, form the SiTrackerHitStrip1D list
                else { 
                    trk_hitlist = event.get(TrackerHit.class,_colname);
                    for (TrackerHit trkhit : trk_hitlist) {
                        SiTrackerHitStrip1D hits1d  = new SiTrackerHitStrip1D(trkhit,TrackerHitType.CoordinateSystem.GLOBAL);
                        if (hits1d == null)  {
                            System.out.println("Failed formation of SiTrackerHitStrip1D");
                            continue;
                        }
                        else {
                            hitlist.add(hits1d);
                        }
                    } //trk_hitlist loop
                }// found TrackerHit Collection
            }//Couldn't find the SiTrackerHit Collection
            
            else {
                // Get the list of SiTrackerHits for this collection
                hitlist = event.get(SiTrackerHit.class, _colname);
            }
            
            if (_debug) {
                System.out.printf("%s: found %d SiTrackerHits\n", this.getClass().getSimpleName(), hitlist.size());
            }
            Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap = new LinkedHashMap<HelicalTrackStrip, SiTrackerHitStrip1D>();
            for (SiTrackerHit hit : hitlist) {
                //if (hit instanceof SiTrackerHitStrip1D) {
                // Cast the hit as a 1D strip hit and find the
                // identifier for the detector/layer combo
                //PF::This fails!!
                //SiTrackerHitStrip1D h = (SiTrackerHitStrip1D) hit;
                
                SiTrackerHitStrip1D h = new SiTrackerHitStrip1D(hit);

                if (clusterAmplitudeCut > 0 && h.getdEdx() / DopedSilicon.ENERGY_EHPAIR < clusterAmplitudeCut) {
                    continue;
                }
                if (_clusterTimeCut > 0 && Math.abs(h.getTime()) > _clusterTimeCut) {
                    continue;
                }
                
                // Create a HelicalTrackStrip for this hit
                HelicalTrackStrip strip = makeDigiStrip(h);
                if (hittomc != null) {
                    for (RawTrackerHit rth : h.getRawHits()) {
                        for (Object simHit : hittomc.allFrom(rth)) {
                            strip.addMCParticle(((SimTrackerHit) simHit).getMCParticle());
                        }
                    }
                }
                
                // Map a reference back to the hit needed to create
                // the stereo hit LC relations
                stripmap.put(strip, h);
                if (_debug) {
                    System.out.printf("%s: added strip org %s layer %d\n", this.getClass().getSimpleName(), strip.origin().toString(), strip.layer());
                }
                
                if (_saveAxialHits)//                           
                {
                    
                    HelicalTrack2DHit haxial = makeDigiAxialHit(h);
                    axialhits.add(haxial);
                    if (hittomc != null) {
                        List<RawTrackerHit> rl = haxial.getRawHits();
                        for (RawTrackerHit rth : rl)
                            for (Object simHit : hittomc.allFrom(rth))
                                haxial.addMCParticle(((SimTrackerHit) simHit).getMCParticle());
                    }
                    axialmcrelations.add(new MyLCRelation(haxial, haxial.getMCParticles()));
                    
                }
                //}//instance 
            }
            
            Map<SiSensor, List<HelicalTrackStrip>> striplistmap = makeStripListMap(stripmap);
            
            if (_debug) {
                System.out.printf("%s: Associate  %d strips to their sensors before pairing\n", this.getClass().getSimpleName(), stripmap.size());
                System.out.printf("%s: Form stereo hits based on %d stereo layers:\n", this.getClass().getSimpleName(), stereoLayers.size());
                System.out.printf("%s: %42s %42s\n", this.getClass().getSimpleName(), "<axial>", "<stereo>");
                for (SvtStereoLayer stereoLayer : stereoLayers) {
                    System.out.printf("%s: %42s %42s\n", this.getClass().getSimpleName(), stereoLayer.getAxialSensor().getName(), stereoLayer.getStereoSensor().getName());
                }
                System.out.printf("%s: Create crosses\n", this.getClass().getSimpleName());
            }
            
            List<HelicalTrackCross> helicalTrackCrosses = findCrosses(striplistmap);
            RelationalTable hittostrip = makeHitToStripTable(helicalTrackCrosses, stripmap);
            // hole-slot combo crosses: only used if allowHoleSlotCombo=true
            List<HelicalTrackCross> helicalTrackCrossesHS = null;
            RelationalTable hittostripHS = null;

            if (allowHoleSlotCombo) {
                helicalTrackCrossesHS = findHoleSlotCrosses(striplistmap);
                hittostripHS = makeHitToStripTable(helicalTrackCrossesHS, stripmap);
                helicalTrackCrosses.addAll(helicalTrackCrossesHS);
            }

            if (rejectGhostHits)
                helicalTrackCrosses = eliminateGhostHits(helicalTrackCrosses, hittostrip, hittostrip);

            for (Iterator<HelicalTrackCross> iter = helicalTrackCrosses.listIterator(); iter.hasNext();) {
                HelicalTrackCross cross = iter.next();

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
        if (hittomc != null) {
            event.put(_mcrelname, mcrelations, LCRelation.class, 0);
        }
        if (_saveAxialHits) {
            event.put(_axialname, axialhits, HelicalTrackHit.class, 0);
            if (hittomc != null) {
                event.put(_axialmcrelname, axialmcrelations, LCRelation.class, 0);
                System.out.println(this.getClass().getSimpleName() + " : number of " + _axialmcrelname + " found = " + axialmcrelations.size());
            }
        }
        if (_doTransformToTracking) {
            addRotatedHitsToEvent(event, stereoCrosses, hittomc != null);
            if (_saveAxialHits) {
                addRotated2DHitsToEvent(event, axialhits);
            }
        }
    } // Process()

    private List<HelicalTrackCross> eliminateGhostHits(List<HelicalTrackCross> crossList, RelationalTable table1, RelationalTable table2) {
        crossLoop: for (Iterator<HelicalTrackCross> iter = crossList.listIterator(); iter.hasNext();) {
            HelicalTrackCross cross = iter.next();
            Collection<TrackerHit> htsList = table1.allFrom(cross);
            for (TrackerHit strip : htsList) {
                Set<HelicalTrackHit> sharedCrosses = table2.allTo(strip);
                if (sharedCrosses != null) {
                    iter.remove();
                    continue crossLoop;
                }
            }
        }
        return crossList;
    }

    private RelationalTable makeHitToStripTable(List<HelicalTrackCross> helicalTrackCrosses, Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap) {
        BaseRelationalTable hittostrip = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (HelicalTrackCross cross : helicalTrackCrosses) {
            for (HelicalTrackStrip strip : cross.getStrips()) {
                hittostrip.add(cross, stripmap.get(strip));
            }
        }
        return hittostrip;
    }

    private Map<SiSensor, List<HelicalTrackStrip>> makeStripListMap(Map<HelicalTrackStrip, SiTrackerHitStrip1D> stripmap) {
        HashMap<SiSensor, List<HelicalTrackStrip>> striplistmap = new HashMap<SiSensor, List<HelicalTrackStrip>>();

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

            if (_debug) {
                System.out.printf("%s: Adding strip hit with origin %s to sensor %s\n", this.getClass().getSimpleName(), strip.origin().toString(), sensor.getName());
            }

        }
        return striplistmap;
    }

    private List<HelicalTrackCross> findCrosses(Map<SiSensor, List<HelicalTrackStrip>> striplistmap) {
        List<HelicalTrackCross> helicalTrackCrosses = new ArrayList<HelicalTrackCross>();

        for (int i = 0; i < stereoLayers.size(); i++) {
            SvtStereoLayer stereoLayer = stereoLayers.get(i);
            if (_debug) {
                System.out.printf("%d: axial %s stereo %s \n", i, stereoLayer.getAxialSensor().getMillepedeId(), stereoLayer.getStereoSensor().getMillepedeId());
            }

            // Form the stereo hits and add them to our hit list
            List<HelicalTrackCross> newCrosses;

            if (stereoLayer.getAxialSensor().isTopLayer()) {
                if (_debug) {
                    System.out.printf("%s: make hits for top\n", this.getClass().getSimpleName());
                    if (striplistmap.get(stereoLayer.getAxialSensor()) != null && striplistmap.get(stereoLayer.getStereoSensor()) != null) 
                        System.out.printf("PF::size striplist map axial: %d  size striplist map stereo: %d \n", striplistmap.get(stereoLayer.getAxialSensor()).size(), striplistmap.get(stereoLayer.getStereoSensor()).size());
                }
                newCrosses = _crosser.MakeHits(striplistmap.get(stereoLayer.getAxialSensor()), striplistmap.get(stereoLayer.getStereoSensor())); //===> } else if (stereoPair.getDetectorVolume() == detectorVolume.Bottom) {
            } else if (stereoLayer.getAxialSensor().isBottomLayer()) {
                if (_debug) {
                    System.out.printf("%s: make hits for bottom\n", this.getClass().getSimpleName());
                    if (striplistmap.get(stereoLayer.getAxialSensor()) != null && striplistmap.get(stereoLayer.getStereoSensor()) != null) 
                        System.out.printf("PF::size striplist map axial: %d  size striplist map stereo: %d \n", striplistmap.get(stereoLayer.getAxialSensor()).size(), striplistmap.get(stereoLayer.getStereoSensor()).size());
                }
                newCrosses = _crosser.MakeHits(striplistmap.get(stereoLayer.getStereoSensor()), striplistmap.get(stereoLayer.getAxialSensor()));
            } else {
                throw new RuntimeException("stereo pair is neither top nor bottom");
            }
            
            if (newCrosses != null) {
                for (Iterator<HelicalTrackCross> iter = newCrosses.listIterator(); iter.hasNext();) {
                    HelicalTrackCross cross = iter.next();
                    if (maxDt > 0 && Math.abs(cross.getStrips().get(0).time() - cross.getStrips().get(1).time()) > maxDt) {
                        iter.remove();
                        continue;
                    }
                }
                helicalTrackCrosses.addAll(newCrosses);
            }
        } // Loop over stereo pairs

        return helicalTrackCrosses;
    }

    private List<HelicalTrackCross> findHoleSlotCrosses(Map<SiSensor, List<HelicalTrackStrip>> striplistmap) {
        List<HelicalTrackCross> helicalTrackCrossesHS = new ArrayList<HelicalTrackCross>();

        Map<Integer, Integer> HSmap = new HashMap<Integer, Integer>();
        HSmap.put(6, 8);
        HSmap.put(8, 6);
        HSmap.put(7, 9);
        HSmap.put(9, 7);
        HSmap.put(10, 12);
        HSmap.put(12, 10);
        HSmap.put(11, 13);
        HSmap.put(13, 11);
        HSmap.put(14, 16);
        HSmap.put(16, 14);
        HSmap.put(15, 17);
        HSmap.put(17, 15);

        for (Integer i = 6; i < 18; i++) {
            List<HelicalTrackCross> newCrosses = null;
            HpsSiSensor stereo = stereoLayers.get(HSmap.get(i)).getStereoSensor();
            HpsSiSensor axial = stereoLayers.get(i).getAxialSensor();
            if (axial.isTopLayer())
                newCrosses = _crosser.MakeHits(striplistmap.get(axial), striplistmap.get(stereo));
            else
                newCrosses = _crosser.MakeHits(striplistmap.get(stereo), striplistmap.get(axial));

            if (newCrosses != null) {
                for (Iterator<HelicalTrackCross> iter = newCrosses.listIterator(); iter.hasNext();) {
                    HelicalTrackCross cross = iter.next();
                    if (maxDt > 0 && Math.abs(cross.getStrips().get(0).time() - cross.getStrips().get(1).time()) > maxDt) {
                        iter.remove();
                        continue;
                    }
                }
                helicalTrackCrossesHS.addAll(newCrosses);
            }
        }

        return helicalTrackCrossesHS;
    }

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

        // Get the collection of stereo layers from the detector
        stereoLayers = ((HpsTracker2) detector.getSubdetector(this._subdetectorName).getDetectorElement()).getStereoPairs();

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

        if (_debug) {
            System.out.printf("%s: %d stereo modules added", this.getClass().getSimpleName(), this._stereomap.size());
        }

    }

    /*
     *  Make  HelicalTrack2DHits from SiTrackerHitStrip1D...note that these HelicalTrack2DHits
     *  are defined in org.hps.recon.tracking.axial (not the lcsim class)
     */
    private HelicalTrack2DHit makeDigiAxialHit(SiTrackerHitStrip1D h) {

        double z1 = h.getHitSegment().getEndPoint().x();
        double z2 = h.getHitSegment().getStartPoint().x();//x is the non-bend direction in the JLAB frame
        double zmin = Math.min(z1, z2);
        double zmax = Math.max(z1, z2);
        IDetectorElement de = h.getSensor();

        HelicalTrack2DHit hit = new HelicalTrack2DHit(h.getPositionAsVector(), h.getCovarianceAsMatrix(), h.getdEdx(), h.getTime(), h.getRawHits(), _ID.getName(de), _ID.getLayer(de), _ID.getBarrelEndcapFlag(de), zmin, zmax, h.getUnmeasuredCoordinate());

        return hit;
    }

    private HelicalTrackStrip makeDigiStrip(SiTrackerHitStrip1D h) {
        
        //PF::this fails
        //SiTrackerHitStrip1D local = (SiTrackerHitStrip1D) h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
        //SiTrackerHitStrip1D global = (SiTrackerHitStrip1D) h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL);

        SiTrackerHitStrip1D local = new SiTrackerHitStrip1D (h.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR));
        SiTrackerHitStrip1D global =  new SiTrackerHitStrip1D (h.getTransformedHit(TrackerHitType.CoordinateSystem.GLOBAL));


        ITransform3D trans = local.getLocalToGlobal();
        Hep3Vector org = trans.transformed(_orgloc);
        Hep3Vector u = global.getMeasuredCoordinate();
        Hep3Vector v = global.getUnmeasuredCoordinate();

        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": makeDigiStrip: org " + org.toString() + " and u " + u.toString() + " v " + v.toString());
        }

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
            System.out.println(this.getClass().getSimpleName() + ": makeDigiStrip: produced HelicalTrackStrip with origin " + strip.origin().toString() + " and u " + strip.u().toString() + " v " + strip.v().toString() + " w " + strip.w().toString());
        }

        return strip;
    }

    private void addRotatedHitsToEvent(EventHeader event, List<HelicalTrackCross> stereohits, boolean isMC) {

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
                if (_debug) {
                    System.out.printf("%s: adding rotated strip with origin %s and u %s v %s w %s \n", getClass().toString(), newstrip.origin().toString(), newstrip.u().toString(), newstrip.v().toString(), newstrip.w().toString());
                }
            }
            List<HelicalTrackStrip> strip1 = new ArrayList<HelicalTrackStrip>();
            List<HelicalTrackStrip> strip2 = new ArrayList<HelicalTrackStrip>();
            strip1.add(rotatedstriphits.get(0));
            strip2.add(rotatedstriphits.get(1));
            List<HelicalTrackCross> newhits = _crosser.MakeHits(strip1, strip2);
            if (newhits.size() != 1) {
                throw new RuntimeException("no rotated cross was created!?");
            }
            HelicalTrackCross newhit = newhits.get(0);
            //HelicalTrackCross newhit = new HelicalTrackCross(rotatedstriphits.get(0), rotatedstriphits.get(1));
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
        if (isMC) {
            event.put("Rotated" + _mcrelname, mcrelations, LCRelation.class, 0);
        }
    }

    /*
     *  Rotate the 2D tracker hits
     */
    private void addRotated2DHitsToEvent(EventHeader event, List<HelicalTrack2DHit> striphits) {
        List<HelicalTrack2DHit> rotatedhits = new ArrayList<HelicalTrack2DHit>();
        List<LCRelation> mcrelations = new ArrayList<LCRelation>();
        for (HelicalTrack2DHit twodhit : striphits) {
            Hep3Vector pos = new BasicHep3Vector(twodhit.getPosition());
            SymmetricMatrix cov = twodhit.getCorrectedCovMatrix();
            double dedx = twodhit.getdEdx();
            double time = twodhit.getTime();
            List<RawTrackerHit> rthList = twodhit.getRawHits();
            String detname = twodhit.Detector();
            int layer = twodhit.Layer();
            BarrelEndcapFlag bec = twodhit.BarrelEndcapFlag();
            double vmin = twodhit.axmin();
            double vmax = twodhit.axmax();
            Hep3Vector axDir = twodhit.axialDirection();
            Hep3Vector newpos = CoordinateTransformations.transformVectorToTracking(pos);
            Hep3Vector newaxdir = CoordinateTransformations.transformVectorToTracking(axDir);
            SymmetricMatrix newcov = CoordinateTransformations.transformCovarianceToTracking(cov);
            HelicalTrack2DHit newhit = new HelicalTrack2DHit(newpos, newcov, dedx, time, rthList, detname, layer, bec, vmin, vmax, newaxdir);
            for (MCParticle mcp : twodhit.getMCParticles()) {
                newhit.addMCParticle(mcp);
            }
            rotatedhits.add(newhit);
            for (MCParticle mcp : newhit.getMCParticles()) {
                mcrelations.add(new MyLCRelation(newhit, mcp));
            }
        }
        if (_debug) {
            System.out.println(this.getClass().getSimpleName() + ": " + _axialname + " size = " + rotatedhits.size());
            System.out.println(this.getClass().getSimpleName() + ": " + _axialmcrelname + " size = " + mcrelations.size());
        }
        event.put("Rotated" + _axialname, rotatedhits, HelicalTrackHit.class, 0);
        event.put("Rotated" + _axialmcrelname, mcrelations, LCRelation.class, 0);
    }

    public void saveAxial2DHits(boolean saveThem) {
        _saveAxialHits = saveThem;
    }
}
