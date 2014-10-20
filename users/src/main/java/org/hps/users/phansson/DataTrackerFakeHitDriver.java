package org.hps.users.phansson;

import hep.aida.IAnalysisFactory;
import hep.aida.IHistogram1D;
import hep.aida.IPlotter;
import hep.aida.IProfile1D;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Matrix;
import hep.physics.vec.Hep3Vector;
import hep.physics.vec.VecOp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//===> import org.hps.conditions.deprecated.SvtUtils;
import org.hps.recon.tracking.HPSTrack;
import org.hps.recon.tracking.TrackUtils;
import org.hps.recon.tracking.TrackerHitUtils;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.Transform3D;
import org.lcsim.detector.Translation3D;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.solids.Box;
import org.lcsim.detector.solids.Inside;
import org.lcsim.detector.solids.Polygon3D;
import org.lcsim.detector.tracker.silicon.ChargeCarrier;
import org.lcsim.detector.tracker.silicon.DopedSilicon;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.detector.tracker.silicon.HpsSiSensor;
import org.lcsim.detector.tracker.silicon.SiSensorElectrodes;
import org.lcsim.detector.tracker.silicon.SiStrips;
import org.lcsim.detector.tracker.silicon.SiTrackerIdentifierHelper;
import org.lcsim.event.EventHeader;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.base.BaseRawTrackerHit;
import org.lcsim.event.base.BaseSimTrackerHit;
import org.lcsim.fit.helicaltrack.HelicalTrackFit;
import org.lcsim.fit.helicaltrack.HelicalTrackHit;
import org.lcsim.fit.helicaltrack.HelixUtils;
import org.lcsim.fit.helicaltrack.HitIdentifier;
import org.lcsim.geometry.Detector;
import org.lcsim.geometry.subdetector.BarrelEndcapFlag;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHit;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType;
import org.lcsim.recon.tracking.digitization.sisim.TrackerHitType.CoordinateSystem;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author phansson
 */
public class DataTrackerFakeHitDriver extends Driver {

    private boolean debug = false;
    TrackerHitUtils trackerhitutils = new TrackerHitUtils();
    Hep3Matrix detToTrk;
    Hep3Vector _bfield;
    TrackerHitType trackerType = new TrackerHitType(TrackerHitType.CoordinateSystem.GLOBAL, TrackerHitType.MeasurementType.STRIP_1D);
    CoordinateSystem coordinate_system = trackerType.getCoordinateSystem();
    private HitIdentifier _ID = new HitIdentifier();
    private EventHeader.LCMetaData metaData = null;
    private boolean _doHth = false;
    boolean createSimTrackerHits = false;
    // Collections
    List<SimTrackerHit> simHits = null;
    List<SiTrackerHit> stripHits1D = null;
    List<HelicalTrackHit> hths = null;
    String trackCollectionName = "MCParticle_HelicalTrackFit";
    String stripHitOutputCollectionName = "StripClusterer_SiTrackerHitStrip1D";
    String hthOutputCollectionName = "RotatedHelicalTrackHits";
    String simTrackerHitCollectionName = "FakeTrackerHits";
    // Subdetector name.
    private String subdetectorName = "Tracker";
    // Various data lists required by digitization.
    private List<String> processPaths = new ArrayList<String>();
    private List<IDetectorElement> processDEs = new ArrayList<IDetectorElement>();
    private Set<SiSensor> processSensors = new HashSet<SiSensor>();
    //Visualization
    private boolean hideFrame = false;
    private AIDA aida = AIDA.defaultInstance();
    private HashMap<SiSensor, IProfile1D> _delta_histos;
    private HashMap<SiSensor, IHistogram1D> _delta_itercount = new HashMap<SiSensor, IHistogram1D>();
    IProfile1D _prf_final_deltas;
    IProfile1D _prf_all_deltas;
    IHistogram1D _h_nstriphits_top;
    IHistogram1D _h_nstriphits_bottom;
    IAnalysisFactory af = aida.analysisFactory();
    IPlotter plotter_iter;
    IPlotter plotter_itercount;
    IPlotter plotter_iter_final;
    IPlotter plotter_iter_all;
    IPlotter plotter_nstripclusters;
    IPlotter plotter_trackposodd;
    int[][] counts = new int[2][10];

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    public void setHideFrame(boolean hide) {
        this.hideFrame = hide;
    }

    public void setDoHth(boolean debug) {
        this._doHth = debug;
    }

    /**
     * Enable/disable the creation of SimTrackerHits
     */
    public void setCreateSimTrackerHits(boolean createSimTrackerHits) {
        this.createSimTrackerHits = createSimTrackerHits;
    }

//    public void setReadoutCollectionName(String readoutCollectionName) {
//        this.readoutCollectionName = readoutCollectionName;
//    }
    public void setSubdetectorName(String subdetectorName) {
        this.subdetectorName = subdetectorName;
    }

    public void setStripHitOutputCollectionName(String stripHitOutputCollectionName) {
        this.stripHitOutputCollectionName = stripHitOutputCollectionName;
    }

    public void setHthOutputCollectionName(String hthOutputCollectionName) {
        this.hthOutputCollectionName = hthOutputCollectionName;
    }

    public void setTrackCollectionName(String trackCollectionName) {
        this.trackCollectionName = trackCollectionName;
    }

    /**
     * Creates a new instance of TrackerHitDriver.
     */
    public DataTrackerFakeHitDriver() {
        this._delta_histos = new HashMap<SiSensor, IProfile1D>();
    }

    /**
     * Do initialization once we get a Detector.
     */
    @Override
    public void detectorChanged(Detector detector) {

        // Call sub-Driver's detectorChanged methods.
        super.detectorChanged(detector);


        Hep3Vector IP = new BasicHep3Vector(0., 0., 1.);
        _bfield = new BasicHep3Vector(0, 0, detector.getFieldMap().getField(IP).y());
        detToTrk = trackerhitutils.detToTrackRotationMatrix();


        // Process detectors specified by path, otherwise process entire
        // detector
        IDetectorElement deDetector = detector.getDetectorElement();

        for (String path : processPaths) {
            processDEs.add(deDetector.findDetectorElement(path));
        }

        if (processDEs.isEmpty()) {
            processDEs.add(deDetector);
        }

        for (IDetectorElement detectorElement : processDEs) {
            processSensors.addAll(detectorElement.findDescendants(SiSensor.class));
        }

        // Set the detector to process.
        processPaths.add(subdetectorName);

        this.makePlots();

    }

    /**
     * Perform the digitization.
     */
    @Override
    public void process(EventHeader event) {


        // Obtain the tracks from the event
        if (!event.hasCollection(HPSTrack.class, trackCollectionName)) {
            this.printDebug("No HPSTracks were found, skipping event");
            simHits = null;
            return;
        }
        List<HPSTrack> tracks = event.get(HPSTrack.class, trackCollectionName);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": found " + tracks.size() + " tracks (" + this.trackCollectionName + ")");
        }

        // Instantiate the list of interest 
        if (this._doHth) {
            this.printDebug("Creating HelicalTrackHits...");
            hths = new ArrayList<HelicalTrackHit>();
        } else if (createSimTrackerHits) {
            this.printDebug("Creating SimTrackerHits...");
            simHits = new ArrayList<SimTrackerHit>();
            metaData = event.getMetaData(event.get(SimTrackerHit.class, "TrackerHits"));
        } else {
            // Create StripHit1Ds by default
            this.printDebug("Creating StripHit1D...");
            stripHits1D = new ArrayList<SiTrackerHit>();
        }

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": Add hits for " + tracks.size() + " tracks (" + this.trackCollectionName + ")");
        }

        for (HPSTrack helix : tracks) {
            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": trying to add hits for this track");
            }

            // Get the MC Particle associated with this track
            MCParticle mcParticle = helix.getMCParticle();

            if (debug) {
                System.out.println(this.getClass().getSimpleName() + helix.toString());
                System.out.println(this.getClass().getSimpleName() + ": htf x0 " + helix.x0() + " y0 " + helix.y0());

                System.out.println(this.getClass().getSimpleName() + ": create a WTrack object");
            }

            WTrack wtrack = new WTrack(helix, Math.abs(_bfield.z()), true); //remove sign from B-field (assumed to go along z-direction)

            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": " + wtrack.toString());

            }



            int n_hits_top = 0;
            int n_hits_bot = 0;
            boolean isTopTrack = false;
            boolean isBotTrack = false;

            // Make hits if the helix passed through the sensor 
            for (SiSensor sensor : processSensors) {

                if (debug) {
                    System.out.println(this.getClass().getSimpleName() + ": add hits to sensor " + sensor.getName() + " at position " + sensor.getGeometry().getPosition().toString());
                }

                // When creating stereo hits, skip the even sensors
                if (this._doHth && _ID.getLayer(sensor) % 2 == 0) {
                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": this was an even sensor -> skip for HTH production");
                    }
                    continue;
                }

                // Get the position where the sensor and track intercept (Maybe this should go 
                // inside the method makeSimTrackerHit since it's the only method that uses it
                Hep3Vector trackPosAtSensor = this.getHelixPlaneIntercept(sensor, wtrack);
                this.printDebug("The track/plane intercept at " + trackPosAtSensor.toString());

                // Check if the track lies within the sensor
                boolean isHit = TrackUtils.sensorContainsTrack(trackPosAtSensor, sensor);

                if (isHit) {
                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": make a tracker hit and add to this sensor");
                    }
                    //===> if (SvtUtils.getInstance().isTopLayer(sensor)) {
                    if (((HpsSiSensor) sensor).isTopLayer()) {
                        n_hits_top++;
                        isTopTrack = true;
                    } else {
                        n_hits_bot++;
                        isBotTrack = true;
                    }
                    if (this._doHth) {
                        hths.add(this.makeHelicalTrackHit(sensor, wtrack));
                    } else if (createSimTrackerHits) {
                        // Create a SimTrackerHit at the intersect between a track and a sensor
                        simHits.add(this.makeSimTrackerHit(metaData, sensor, trackPosAtSensor, mcParticle, wtrack));
                    } else {
                        stripHits1D.add(this.makeTrackerHit(sensor, wtrack));
                    }
                } else {
                    if (debug) {
                        System.out.println(this.getClass().getSimpleName() + ": this helix didn't pass within the sensor so no hit was added");
                    }
                }

            }

            if (isTopTrack) {
                this._h_nstriphits_top.fill(n_hits_top);
            }
            if (isBotTrack) {
                this._h_nstriphits_bottom.fill(n_hits_bot);
            }
            if (isTopTrack && isBotTrack) {
                System.out.println(this.getClass().getSimpleName() + ": tris track is both top and bottom??? \n" + wtrack.toString() + "\nHTF:" + helix.toString());
                System.exit(1);
            }

            /*
             stripHits1D.addAll(stripHits1D_for_track);
             hths.addAll(hths_for_track);
             */
        }



        if (debug) {
            if (stripHits1D != null) {
                System.out.println(this.getClass().getSimpleName() + ": Produced " + stripHits1D.size() + " hits for collection " + this.stripHitOutputCollectionName);
            }
            if (hths != null) {
                System.out.println(this.getClass().getSimpleName() + ": Produced " + hths.size() + " hits for collection " + this.hthOutputCollectionName);
            }
        }


        //int flag = LCIOUtil.bitSet(0, 31, true); // Turn on 64-bit cell ID.
        //event.put(this.rawTrackerHitOutputCollectionName, rawHits, RawTrackerHit.class, flag, toString());
        // Put the collection of interest into the event

        if (_doHth) {
            event.put(hthOutputCollectionName, hths, HelicalTrackHit.class, 0);
        } else if (createSimTrackerHits) {
            event.put(simTrackerHitCollectionName, simHits, SimTrackerHit.class, 0);
            this.printDebug("SimTrackerHits created: " + simHits.size());
        } else {
            event.put(stripHitOutputCollectionName, stripHits1D, SiTrackerHitStrip1D.class, 0, toString());
        }
        //event.put("RotatedHelicalTrackHits", rotatedhits, HelicalTrackHit.class, 0);

        /*
         if (debug) {
         if(event.hasCollection(HelicalTrackHit.class, "RotatedHelicalTrackHits")) {
         System.out.println(this.getClass().getSimpleName() + ": has hths:");
         for(HelicalTrackHit hth : hths) {
         System.out.println(this.getClass().getSimpleName() + ": " + hth.getPosition().toString());
         }
         } else {
         System.out.println(this.getClass().getSimpleName() + ": has not hths!");
         }
         }
         if (debug) {
         for (int mod = 0; mod < 2; mod++) {
         for (int layer = 0; layer < 10; layer++) {
         counts[mod][layer] += SvtUtils.getInstance().getSensor(mod, layer).getReadout().getHits(SiTrackerHit.class).size();
         }
         }
         }*/
    }

    private SiTrackerHitStrip1D makeTrackerHit(SiSensor sensor, WTrack wtrack) {
        //private SiTrackerHitStrip1D makeTrackerHit(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        //create fake raw tracker hit

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": makeTrackerHit");
        }

        List<RawTrackerHit> rth_cluster = this.makeRawTrackerFakeHit(sensor);
        if (rth_cluster.size() != 1) {
            System.out.println(this.getClass().getSimpleName() + ": the fake raw tracker hit cluster is different than one!? " + rth_cluster.size());
            System.exit(1);
        }
        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": created a fake raw tracker hit ");
        }
        RawTrackerHit raw_hit = rth_cluster.get(0);
        IIdentifier id = raw_hit.getIdentifier();

        //Get the electrode objects
        SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
        SiSensorElectrodes electrodes = ((SiSensor) raw_hit.getDetectorElement()).getReadoutElectrodes(carrier);




        ITransform3D local_to_global;
        if (coordinate_system == TrackerHitType.CoordinateSystem.GLOBAL) {
            local_to_global = new Transform3D();
        } else if (coordinate_system == TrackerHitType.CoordinateSystem.SENSOR) {
            local_to_global = sensor.getGeometry().getLocalToGlobal();
        } else {
            throw new RuntimeException(this.getClass().getSimpleName() + " problem with coord system " + coordinate_system.toString());
        }


        ITransform3D electrodes_to_global = electrodes.getLocalToGlobal();
        ITransform3D global_to_hit = local_to_global.inverse();
        ITransform3D electrodes_to_hit = Transform3D.multiply(global_to_hit, electrodes_to_global);

        Hep3Vector u = electrodes_to_hit.rotated(electrodes.getMeasuredCoordinate(0));
        Hep3Vector v = electrodes_to_hit.rotated(electrodes.getUnmeasuredCoordinate(0));
        Hep3Vector w = VecOp.cross(u, v);
        Hep3Vector _orgloc = new BasicHep3Vector(0., 0., 0.);
        electrodes_to_global.transformed(_orgloc);
        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": electrodes  u " + u.toString());
            System.out.println(this.getClass().getSimpleName() + ": electrodes  v " + v.toString());
            System.out.println(this.getClass().getSimpleName() + ": electrodes  w " + w.toString() + "( " + w.magnitude() + ")");
        }

        electrodes_to_global.getTranslation().translate(_orgloc);
        if (debug) {
            System.out.print(this.getClass().getSimpleName() + ": orgloc " + _orgloc.toString() + "  -> ");
        }
        _orgloc = VecOp.mult(detToTrk, _orgloc);
        if (debug) {
            System.out.println(_orgloc.toString());
        }




        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": Try to find the interception point");
        }

        //B-field must go along Z direction
        Hep3Vector h = new BasicHep3Vector(_bfield.x(), _bfield.y(), Math.abs(_bfield.z()));
        h = VecOp.unit(h);
        //Rotate into tracking frame
        Hep3Vector eta = VecOp.mult(detToTrk, w);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": eta  " + eta.toString());
        }

        Hep3Vector position = wtrack.getHelixAndPlaneIntercept(_orgloc, eta, h);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": found interception point at position " + position.toString());
        }


        HelicalTrackFit htf = wtrack._htf;
        List<Double> s = HelixUtils.PathToXPlane(htf, position.x(), 0, 0);
        Hep3Vector posOnHelix = HelixUtils.PointOnHelix(htf, s.get(0));
        Hep3Vector posdiff = VecOp.sub(position, posOnHelix);
        System.out.println(this.getClass().getSimpleName() + ": diffpos " + posdiff.toString() + " L " + position.toString() + " posOnHelix " + posOnHelix.toString() + " R=" + htf.R());

        position = VecOp.mult(VecOp.inverse(detToTrk), position);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": rotate the hit position to the global frame -> " + position.toString());
        }



        // Need to make sure that the position is at the edge of the strip in the global frame
        // 1. Rotate to the local sensor frame
        position = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getGlobalToLocal().transformed(position);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": local (sensor) hit position " + position.toString());
        }

        // 2. Remove the coordinate of the unmeasured direction

        position = new BasicHep3Vector(position.x(), 0, position.z());

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": fixed local (sensor) hit position " + position.toString());
        }

        // 3. Transform back to global coordinates

        position = ((SiSensor) electrodes.getDetectorElement()).getGeometry().getLocalToGlobal().transformed(position);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": fixed global hit position " + position.toString());
        }




        //Fill dummy versions
        SymmetricMatrix covariance = this.getCovariance(rth_cluster, electrodes);
        double time = this.getTime(rth_cluster);
        double energy = this.getEnergy(rth_cluster);


        SiTrackerHitStrip1D hit = new SiTrackerHitStrip1D(position, covariance, energy, time, rth_cluster, trackerType);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": created SiStrip1D at " + position.toString());
        }


        return hit;


    }

    /**
     *
     * Find the unit vector of a sensor
     *
     * @param sensor
     * @return unit vector of the plane
     */
    private Hep3Vector getPlaneUnitVector(SiSensor sensor) {
        /*
         * Weird way of getting the unit vector by creating a fake raw tracker hit...must be simpler way?
         */
        RawTrackerHit raw_hit = this.makeRawTrackerFakeHit(sensor).get(0);
        IIdentifier id = raw_hit.getIdentifier();
        //Get the electrode objects
        SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
        SiSensorElectrodes electrodes = ((SiSensor) raw_hit.getDetectorElement()).getReadoutElectrodes(carrier);
        ITransform3D local_to_global;
        if (coordinate_system == TrackerHitType.CoordinateSystem.GLOBAL) {
            local_to_global = new Transform3D();
        } else if (coordinate_system == TrackerHitType.CoordinateSystem.SENSOR) {
            local_to_global = sensor.getGeometry().getLocalToGlobal();
        } else {
            throw new RuntimeException(this.getClass().getSimpleName() + " problem with coord system " + coordinate_system.toString());
        }
        ITransform3D electrodes_to_global = electrodes.getLocalToGlobal();
        ITransform3D global_to_hit = local_to_global.inverse();
        ITransform3D electrodes_to_hit = Transform3D.multiply(global_to_hit, electrodes_to_global);

        Hep3Vector u = electrodes_to_hit.rotated(electrodes.getMeasuredCoordinate(0));
        Hep3Vector v = electrodes_to_hit.rotated(electrodes.getUnmeasuredCoordinate(0));
        Hep3Vector w = VecOp.cross(u, v);
        Hep3Vector eta = VecOp.mult(detToTrk, w);
        return eta;

    }

    /**
     *
     * Find the origin of the sensor plane
     *
     * @param sensor
     * @return origin position of the plane
     */
    private Hep3Vector getOrgLoc(SiSensor sensor) {
        /*
         * Weird way of getting the org location by creating a fake raw tracker hit...must be simpler way?
         */
        List<RawTrackerHit> rth_cluster = this.makeRawTrackerFakeHit(sensor);
        if (rth_cluster.size() != 1) {
            System.out.println(this.getClass().getSimpleName() + ": the fake raw tracker hit cluster is different than one!? " + rth_cluster.size());
            System.exit(1);
        }
        //Get the electrode objects
        IIdentifier id = rth_cluster.get(0).getIdentifier();
        SiTrackerIdentifierHelper _sid_helper = (SiTrackerIdentifierHelper) sensor.getIdentifierHelper();
        ChargeCarrier carrier = ChargeCarrier.getCarrier(_sid_helper.getSideValue(id));
        SiSensorElectrodes electrodes = ((SiSensor) rth_cluster.get(0).getDetectorElement()).getReadoutElectrodes(carrier);
        ITransform3D local_to_global;
        if (coordinate_system == TrackerHitType.CoordinateSystem.GLOBAL) {
            local_to_global = new Transform3D();
        } else if (coordinate_system == TrackerHitType.CoordinateSystem.SENSOR) {
            local_to_global = sensor.getGeometry().getLocalToGlobal();
        } else {
            throw new RuntimeException(this.getClass().getSimpleName() + " problem with coord system " + coordinate_system.toString());
        }
        ITransform3D electrodes_to_global = electrodes.getLocalToGlobal();
        Hep3Vector _orgloc = new BasicHep3Vector(0., 0., 0.);
        electrodes_to_global.transformed(_orgloc);
        electrodes_to_global.getTranslation().translate(_orgloc);
        if (debug) {
            System.out.print(this.getClass().getSimpleName() + ": orgloc " + _orgloc.toString() + "  -> ");
        }
        _orgloc = VecOp.mult(detToTrk, _orgloc);
        if (debug) {
            System.out.println(_orgloc.toString());
        }

        return _orgloc;

    }

    private Hep3Vector getHelixPlaneIntercept(SiSensor sensor, WTrack wtrack) {
        Hep3Vector eta = this.getPlaneUnitVector(sensor);
        Hep3Vector _orgloc = this.getOrgLoc(sensor);
        Hep3Vector h = new BasicHep3Vector(_bfield.x(), _bfield.y(), Math.abs(_bfield.z()));
        h = VecOp.unit(h);
        Hep3Vector position = wtrack.getHelixAndPlaneIntercept(_orgloc, eta, h);
        if (debug) {
            HelicalTrackFit htf = wtrack._htf;
            List<Double> s = HelixUtils.PathToXPlane(htf, position.x(), 0, 0);
            Hep3Vector posOnHelix = HelixUtils.PointOnHelix(htf, s.get(0));
            Hep3Vector posdiff = VecOp.sub(position, posOnHelix);
            System.out.println(this.getClass().getSimpleName() + ": Path length to position " + position.toString() + ": s = " + s.get(0));
            System.out.println(this.getClass().getSimpleName() + ": Difference between W and helixutils: diffpos " + posdiff.toString() + " ( " + position.toString() + " posOnHelix " + posOnHelix.toString() + " R=" + htf.R());
        }


        return position;
    }

    private HelicalTrackHit makeHelicalTrackHit(SiSensor sensor, WTrack wtrack) {
        //private SiTrackerHitStrip1D makeTrackerHit(List<HPSFittedRawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        //create fake raw tracker hit

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": makeTrackerHit");
        }

        List<RawTrackerHit> rth_cluster = this.makeRawTrackerFakeHit(sensor);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": Try to find the interception point");
        }

        Hep3Vector position = this.getHelixPlaneIntercept(sensor, wtrack);


        //Fill dummy covariance matrix with minimal uncertainties
        //SymmetricMatrix covariance = this.getCovariance(rth_cluster, electrodes);
        SymmetricMatrix covariance = new SymmetricMatrix(3);
        double cov_xx = Math.pow(0.00001, 2); //1um error
        double cov_yy = cov_xx;
        double cov_zz = cov_xx;
        covariance.setElement(0, 0, cov_xx);
        covariance.setElement(1, 1, cov_yy);
        covariance.setElement(2, 2, cov_zz);

        double time = this.getTime(rth_cluster);
        double energy = this.getEnergy(rth_cluster);


        //IDetectorElement de = sensor;
        String det = _ID.getName(sensor);
        int layer = _ID.getLayer(sensor);
        BarrelEndcapFlag beflag = _ID.getBarrelEndcapFlag(sensor);

        if (layer % 2 == 0) {
            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": problem, trying to create a HTH for even layer? " + layer);
            }
            System.exit(1);
        }

        HelicalTrackHit hit = new HelicalTrackHit(position, covariance, 0.0, time, 3, rth_cluster, det, layer, beflag);
        //SiTrackerHitStrip1D hit = new SiTrackerHitStrip1D(position, covariance, energy, time, rth_cluster, trackerType);

        if (debug) {
            System.out.println(this.getClass().getSimpleName() + ": created HelicalTrackHit at " + position.toString() + " and layer " + hit.Layer() + "(" + layer + ")");
        }


        return hit;


    }

    /**
     * Create a SimTrackerHit and add it to the corresponding readout
     *
     * @param metaData : meta data associated with the SimTrackerHit collection
     * @param sensor : sensor on which the hit will be created on
     * @param trkPositionAtSensor : the position of a track at a sensor plane
     * @param particle : MC particle associated with the track containing the
     * hit
     * @return SimTrackerHit
     */
    private SimTrackerHit makeSimTrackerHit(EventHeader.LCMetaData metaData, SiSensor sensor, Hep3Vector trkPositionAtSensor, MCParticle particle, WTrack wtrack) {

        // Transform the position of the SimTrackerHit to the detector coordinates
        // TODO: Fix the extrapolator so that it returns the position in the detector frame
        //Hep3Vector trkPositionAtSensorDet = VecOp.mult(VecOp.inverse(detToTrk),trkPositionAtSensor);
        //this.printDebug("The helix and sensor intercept at: " + trkPositionAtSensorDet.toString());

        // Sensor to tracking frame transformation
        ITransform3D localToGlobal = sensor.getGeometry().getLocalToGlobal();
        // Tracking frame to sensor transformation
        ITransform3D globalToSensor = sensor.getGeometry().getGlobalToLocal();

        // Get the sensor position
        Hep3Vector sensorPosition = sensor.getGeometry().getPosition();
        this.printDebug("Sensor position: " + sensorPosition.toString());
        // Transform the sensor position to the tracking frame
        Hep3Vector transformedSensorPosition = globalToSensor.transformed(sensorPosition);
        this.printDebug("Transformed sensor position: " + transformedSensorPosition.toString());

        // Get the solid representing a sensor
        Box sensorSolid = (Box) sensor.getGeometry().getLogicalVolume().getSolid();
        // Get the solid faces
        Polygon3D pSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, 1)).get(0);
        this.printDebug("p Side: " + pSide.toString());
        Polygon3D nSide = sensorSolid.getFacesNormalTo(new BasicHep3Vector(0, 0, -1)).get(0);
        this.printDebug("n Side: " + pSide.toString());

        // Translate to the sensor face (p side) 
        ITranslation3D translateToPSide = new Translation3D(VecOp.mult(-pSide.getDistance(), pSide.getNormal()));
        this.printDebug("pSide Translation vector: " + translateToPSide.getTranslationVector().toString());

        // Translate to the p side of the sensor
        Hep3Vector pSidePosition = translateToPSide.translated(transformedSensorPosition);
        this.printDebug("Translated sensor position at p side: " + pSidePosition.toString());
        // Transform the sensor position to the tracking coordinates
        localToGlobal.transform(pSidePosition);
        this.printDebug("Translated sensor position at p side in tracking coordinates: " + pSidePosition.toString());
        // Check if the point lies inside of the sensor
        if (sensor.getGeometry().inside(pSidePosition) == Inside.OUTSIDE) {
            throw new RuntimeException("Position of p side face does not lie within the sensor volume!");
        } else {
            this.printDebug("p side position lies within the sensor volume");
        }

        // Find the interception between the p side face and the track
        Hep3Vector h = new BasicHep3Vector(_bfield.x(), _bfield.y(), Math.abs(_bfield.z()));
        h = VecOp.unit(h);
        // Transform the sensor position to the lcsim coordinates
        pSidePosition = VecOp.mult(detToTrk, pSidePosition);
        this.printDebug("p side position in lcsim coordinates: " + pSidePosition.toString());
        //Hep3Vector pSideInter = wutils.getHelixAndPlaneIntercept(wtrack, pSidePosition, VecOp.unit(pSidePosition), h);
        Hep3Vector eta = this.getPlaneUnitVector(sensor);
        Hep3Vector pSideInter = wtrack.getHelixAndPlaneIntercept(pSidePosition, eta, h);
        this.printDebug("Intersection between track and p side: " + pSideInter.toString());
        // Transform back to the JLab coordinates 
        pSideInter = VecOp.mult(VecOp.inverse(detToTrk), pSideInter);
        this.printDebug("Intersection trasnformed to the JLab coordinates: " + pSideInter.toString());
        if (sensor.getGeometry().inside(pSideInter) == Inside.OUTSIDE) {
            throw new RuntimeException("Position of p side/track intercept does not lie within the sensor volume!");
        } else {
            this.printDebug("p side/track intercept lies within the sensor volume");
        }

        // Translate to the sensor n side
        ITranslation3D translateToNSide = new Translation3D(VecOp.mult(-nSide.getDistance(), nSide.getNormal()));
        this.printDebug("n side Translation vector: " + translateToNSide.getTranslationVector().toString());

        // Translate to the n side of the sensor
        Hep3Vector nSidePosition = translateToNSide.translated(transformedSensorPosition);
        this.printDebug("Translated sensor position at n side: " + nSidePosition.toString());
        // Transform the sensor position to the tracking coordinates
        localToGlobal.transform(nSidePosition);
        this.printDebug("Translated sensor position at n side in tracking coordinates: " + nSidePosition.toString());
        // Check if the point lies inside of the sensor
        if (sensor.getGeometry().inside(nSidePosition) == Inside.OUTSIDE) {
            throw new RuntimeException("Position of n side face does not lie within the sensor volume!");
        } else {
            this.printDebug("n side position lies within the sensor volume");
        }

        // Find the interception between the p side face and the track
        // Transform the sensor position to the lcsim coordinates
        nSidePosition = VecOp.mult(detToTrk, nSidePosition);
        this.printDebug("n side position in lcsim coordinates: " + nSidePosition.toString());
        //Hep3Vector pSideInter = wutils.getHelixAndPlaneIntercept(wtrack, pSidePosition, VecOp.unit(pSidePosition), h);
        Hep3Vector nSideInter = wtrack.getHelixAndPlaneIntercept(nSidePosition, eta, h);
        this.printDebug("Intersection between track and n side: " + nSideInter.toString());
        // Transform back to the JLab coordinates 
        nSideInter = VecOp.mult(VecOp.inverse(detToTrk), nSideInter);
        this.printDebug("Intersection trasnfored to the JLab coordinates: " + nSideInter.toString());
        if (sensor.getGeometry().inside(nSideInter) == Inside.OUTSIDE) {
            throw new RuntimeException("Position of n side/track intercept does not lie within the sensor volume!");
        } else {
            this.printDebug("n side/track intercept lies within the sensor volume");
        }

        // Find the midpoint between a straight line connecting the p side and n side intercepts 
        Hep3Vector trkPositionAtSensorDet = VecOp.add(nSideInter, pSideInter);
        trkPositionAtSensorDet = VecOp.mult(.5, trkPositionAtSensorDet);
        this.printDebug("Hit will be placed at position: " + trkPositionAtSensorDet.toString());
        if (sensor.getGeometry().inside(trkPositionAtSensorDet) == Inside.OUTSIDE) {
            throw new RuntimeException("Midpoint does not lie within the sensor volume!");
        } else {
            this.printDebug("midpoint lies within the sensor volume");
        }

        // Find the length of the line. For now, this is the path length
        // Note: The small delta parameter is to avoid ending up outside of the sensor
        double pathLength = VecOp.sub(nSideInter, pSideInter).magnitude() - .01;
        this.printDebug("The path length is: " + pathLength);

        /* DEBUG
         Hep3Vector midpoint = new BasicHep3Vector(trkPositionAtSensorDet.v());
         Hep3Vector direction = VecOp.unit(new BasicHep3Vector(particle.getMomentum().v()));
         Hep3Vector half_length = VecOp.mult(pathLength/2.0,direction);
        
         Hep3Vector endPoint = VecOp.add(midpoint,half_length);
         this.printDebug("The end point is at position: " + endPoint.toString());
        
         Hep3Vector startPoint = VecOp.add(midpoint,VecOp.mult(-1.0,half_length));
         this.printDebug("The start point is at position: " + startPoint.toString());
        
         if(sensor.getGeometry().inside(endPoint) == Inside.OUTSIDE){
         throw new RuntimeException("Position of end point does not lie within the sensor volume!");
         } else { 
         this.printDebug("end point lies within the sensor volume");
         }
        
         if(sensor.getGeometry().inside(startPoint) == Inside.OUTSIDE){
         throw new RuntimeException("Position start point does not lie within the sensor volume!");
         } else { 
         this.printDebug("Start point lies within the sensor volume");
         } */

        double dEdx = 24000/* MIP */ * DopedSilicon.ENERGY_EHPAIR;
        double[] momentum = particle.getMomentum().v();
        this.printDebug("Particle Momentum: " + particle.getMomentum().toString());
        double time = 0;
        int cellID = (int) TrackerHitUtils.makeSimTrackerHitId(sensor).getValue();

        SimTrackerHit simHit = new BaseSimTrackerHit(trkPositionAtSensorDet.v(), dEdx, momentum, pathLength, time, cellID, particle, metaData, sensor);
        // Add it to the sensor readout
        // sensor.getReadout().addHit(simHit);
        return simHit;
    }

    //private SymmetricMatrix getCovariance() {     
    private SymmetricMatrix getCovariance(List<RawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        SymmetricMatrix covariance = new SymmetricMatrix(3);
        covariance.setElement(0, 0, Math.pow(getMeasuredResolution(cluster, electrodes), 2));
        covariance.setElement(1, 1, Math.pow(getUnmeasuredResolution(cluster, electrodes), 2));
        covariance.setElement(2, 2, 0.0);

        SymmetricMatrix covariance_global = electrodes.getLocalToGlobal().transformed(covariance);

//        System.out.println("Global covariance matrix: \n"+covariance_global);

        return covariance_global;

    }

    private double getMeasuredResolution(List<RawTrackerHit> cluster, SiSensorElectrodes electrodes) // should replace this by a ResolutionModel class that gives expected resolution.  This could be a big job.
    {
        double measured_resolution;

        double sense_pitch = ((SiSensor) electrodes.getDetectorElement()).getSenseElectrodes(electrodes.getChargeCarrier()).getPitch(0);

//        double readout_pitch = electrodes.getPitch(0);
//        double noise = _readout_chip.getChannel(strip_number).computeNoise(electrodes.getCapacitance(strip_number));
//        double signal_expected = (0.000280/DopedSilicon.ENERGY_EHPAIR) *
//                ((SiSensor)electrodes.getDetectorElement()).getThickness(); // ~280 KeV/mm for thick Si sensors
        double _oneClusterErr = 1 / Math.sqrt(12);
        double _twoClusterErr = 1 / 5;
        double _threeClusterErr = 1 / 3;
        double _fourClusterErr = 1 / 2;
        double _fiveClusterErr = 1;

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

    private double getUnmeasuredResolution(List<RawTrackerHit> cluster, SiSensorElectrodes electrodes) {
        // Get length of longest strip in hit
        double hit_length = 0;
        for (RawTrackerHit hit : cluster) {
            hit_length = Math.max(hit_length, ((SiStrips) electrodes).getStripLength(1));
        }
        return hit_length / Math.sqrt(12);
    }

    private double getTime(List<RawTrackerHit> cluster) {
        return 0;
    }

    private double getEnergy(List<RawTrackerHit> cluster) {
        double total_charge = 20000;
//        
//         double total_charge = 0.0;
//        for (RawTrackerHit hit : cluster) {
//            double signal = hit.getAmp();
//            total_charge += signal;
//        }
        return total_charge * DopedSilicon.ENERGY_EHPAIR;
    }

    /**
     * Creates a raw tracker hit at a dummy channel
     *
     * @param sensor that will get the hit
     * @return list of a single raw tracker hit
     */
    public List<RawTrackerHit> makeRawTrackerFakeHit(SiSensor sensor) {
    
        //if(debug) System.out.println(this.getClass().getSimpleName() + ": makeRawTrackerFakeHit for sensor " + sensor.getName());
        List<RawTrackerHit> raw_hits = new ArrayList<RawTrackerHit>();

        // Get SimTrackerHits
        //IReadout ro = sensor.getReadout();


        // Loop over electrodes and digitize with readout chip
        for (ChargeCarrier carrier : ChargeCarrier.values()) {
            if (sensor.hasElectrodesOnSide(carrier)) {

                //if(debug) System.out.println(this.getClass().getSimpleName() + ": creating a dummy hit for sensor " + sensor.getName());
                //SortedMap<Integer,List<Integer>> digitized_hits = _readout_chip.readout(electrode_data.get(carrier),sensor.getReadoutElectrodes(carrier));
                //if(debug) System.out.println(this.getClass().getSimpleName() + ": creating a dummy hit for sensor " + sensor.getName());

                int channel = 1;
                int time = 0;
                long cell_id = sensor.makeStripId(channel, carrier.charge()).getValue();
                //List<Integer> readout_data = new ArrayList<Integer>();
                short[] adc_values = new short[6];
                for (Integer i = 0; i < 6; ++i) {
                    Integer adc = 50;
                    adc_values[i] = adc.shortValue(); //ADC counts
                }
                IDetectorElement detector_element = sensor;
                RawTrackerHit raw_hit = new BaseRawTrackerHit(time, cell_id, adc_values, new ArrayList<SimTrackerHit>(), detector_element);
                //ro.addHit(raw_hit);
                raw_hits.add(raw_hit);


            }

        }

        return raw_hits;
    }

    private void makePlots() {

        for (SiSensor sensor : processSensors) {
            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": Making plots for " + sensor.getName());
            }
            IProfile1D h = aida.profile1D("deltas " + sensor.getName(), 7, 0, 7);
            this._delta_histos.put(sensor, h);
            IHistogram1D h1 = aida.histogram1D("Number of iterations " + sensor.getName(), 7, 0, 7);
            this._delta_itercount.put(sensor, h1);
        }

        _prf_all_deltas = aida.profile1D("alldeltas", 10, 0, 10);//,50,-20,20);
        _prf_final_deltas = aida.profile1D("finaldeltas", 10, 0, 10);//,50,-20,20);

        _h_nstriphits_top = aida.histogram1D("NstripClusters top", 11, -0.5, 10.5);
        _h_nstriphits_bottom = aida.histogram1D("NstripClusters bottom", 11, -0.5, 10.5);

        //_h_trkposodd_top = aida.histogram1D("_h_trkposodd_top", 50, -0.5, 10.5);

        plotter_iter_final = af.createPlotterFactory().create();
        plotter_iter_final.createRegions(1, 1);
        plotter_iter_final.region(0).plot(_prf_final_deltas);
        plotter_iter_final.region(0).style().xAxisStyle().setLabel("Final iteration");
        plotter_iter_final.region(0).style().yAxisStyle().setLabel("<Distance to xp>");

        plotter_iter_all = af.createPlotterFactory().create();
        plotter_iter_all.createRegions(1, 1);
        plotter_iter_all.region(0).plot(_prf_all_deltas);
        plotter_iter_all.region(0).style().xAxisStyle().setLabel("Iteration");
        plotter_iter_all.region(0).style().yAxisStyle().setLabel("<Distance to xp>");


        plotter_iter = af.createPlotterFactory().create();
        plotter_iter.createRegions(6, 4);
        plotter_iter.style().dataStyle().fillStyle().setParameter("colorMapScheme", "rainbow");
        plotter_iter.style().statisticsBoxStyle().setVisible(false);
        plotter_itercount = af.createPlotterFactory().create();
        plotter_itercount.createRegions(6, 4);

        plotter_nstripclusters = af.createPlotterFactory().create();
        plotter_nstripclusters.createRegions(2, 2);
        plotter_nstripclusters.region(0).plot(_h_nstriphits_top);
        plotter_nstripclusters.region(1).plot(_h_nstriphits_bottom);

        plotter_trackposodd = af.createPlotterFactory().create();
        plotter_trackposodd.createRegions(2, 2);
        //plotter_trackposodd.region(0).plot(_h_trkposodd_top);

        int i = 0;
        for (SiSensor sensor : this.processSensors) {
            if (debug) {
                System.out.println(this.getClass().getSimpleName() + ": " + i + ": adding plot to plotter for " + sensor.getName());
            }
            plotter_iter.region(i).plot(this._delta_histos.get(sensor));
            plotter_iter.style().setParameter("hist2DStyle", "colorMap");
            plotter_iter.region(i).style().xAxisStyle().setLabel("Iteration");
            plotter_iter.region(i).style().yAxisStyle().setLabel("<Distance to xp>");

            plotter_itercount.region(i).plot(this._delta_itercount.get(sensor));
            plotter_itercount.region(i).style().xAxisStyle().setLabel("# iterations");
            ++i;
        }
        if (!hideFrame) {
            plotter_iter.show();
            plotter_itercount.show();
            plotter_iter_final.show();
            plotter_iter_all.show();
            plotter_nstripclusters.show();
        }
    }

    /**
     * print debug statement
     *
     * @param debugStatement : debug statement
     */
    public void printDebug(String debugStatement) {
        if (!debug) {
            return;
        }
        System.out.println(this.getClass().getSimpleName() + ": " + debugStatement);
    }

    @Override
    public void endOfData() {
        if (debug) {
            for (int mod = 0; mod < 2; mod++) {
                for (int layer = 0; layer < 10; layer++) {
                    System.out.format("mod %d, layer %d, count %d\n", mod, layer, counts[mod][layer]);
                }
            }
        }
    }
}
