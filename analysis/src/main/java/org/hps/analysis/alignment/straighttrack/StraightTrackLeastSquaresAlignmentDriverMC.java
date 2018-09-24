package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import hep.physics.matrix.SymmetricMatrix;
import hep.physics.vec.BasicHep3Vector;
import hep.physics.vec.Hep3Vector;
import static java.lang.Math.sqrt;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GET_IMPACT;
import org.hps.analysis.alignment.straighttrack.vertex.StraightLineVertexFitter;
import org.hps.analysis.alignment.straighttrack.vertex.Track;
import org.hps.analysis.alignment.straighttrack.vertex.Vertex;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.IRotation3D;
import org.lcsim.detector.ITransform3D;
import org.lcsim.detector.ITranslation3D;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.LCRelation;
import org.lcsim.event.MCParticle;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.SimTrackerHit;
import org.lcsim.event.TrackerHit;
import org.lcsim.event.base.BaseRelationalTable;
import org.lcsim.geometry.Detector;
import org.lcsim.math.chisq.ChisqProb;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class StraightTrackLeastSquaresAlignmentDriverMC extends Driver {

    boolean debug = false;
    boolean printGeometry = true;

    private AIDA aida = AIDA.defaultInstance();
    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

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

    DetectorBuilder _db;

    List<Track> tracksToVertex = new ArrayList<Track>();

    @Override
    protected void detectorChanged(Detector detector) {
        _db = new DetectorBuilder(detector);
    }

    @Override
    protected void process(EventHeader event) {
//        analyzeMC(event);

        List<MCParticle> mcparts = event.getMCParticles();
        MCParticle mcp = mcparts.get(0);
        Hep3Vector o = mcp.getOrigin();
        Hep3Vector p = mcp.getMomentum();
        double pxpz = p.x() / p.z();
        double pypz = p.y() / p.z();
//        System.out.println("mcp origin " + o);
//        System.out.println("mcp momentum " + p);
//        System.out.println("pxpz " + pxpz + " pypz " + pypz);
        uLocal.clear();
        uSigLocal.clear();

        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);

        setupSensors(event);
        List<TrackerHit> stripClusters = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");
//        // Get the list of fitted hits from the event
//        List<LCRelation> fittedHits = event.get(LCRelation.class, "SVTFittedRawTrackerHits");
//        // Map the fitted hits to their corresponding raw hits
//        Map<RawTrackerHit, LCRelation> fittedRawTrackerHitMap = new HashMap<RawTrackerHit, LCRelation>();
//        for (LCRelation fittedHit : fittedHits) {
//            fittedRawTrackerHitMap.put(FittedRawTrackerHit.getRawTrackerHit(fittedHit), fittedHit);
//        }

//        Map<Integer, double[]> globalPos = new ConcurrentSkipListMap<Integer, double[]>();
//        Map<Integer, double[]> localPos = new ConcurrentSkipListMap<Integer, double[]>();
//
//        Map<Integer, Hep3Vector> sensorOrigins = new ConcurrentSkipListMap<Integer, Hep3Vector>();
//        Map<Integer, Hep3Vector> sensorNormals = new ConcurrentSkipListMap<Integer, Hep3Vector>();
//        Map<Integer, String> sensorNames = new ConcurrentSkipListMap<Integer, String>();
        List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
        List<Hit> hits = new ArrayList<Hit>();
        //assign an error based on the size of the strip cluster
        double[] fixedDu = {0., .012, .006};
        for (TrackerHit hit : stripClusters) {
            SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(hit);
            List rthList = hit.getRawHits();
            int size = rthList.size();
            double du;
            if (size < 3) {
                du = fixedDu[size];
            } else {
                du = .04;
            }
//            ITransform3D local_to_global = null;
//            ITransform3D global_to_local = null;
//            Hep3Vector sensorOrigin;
//            Hep3Vector sensorNormal;
            RawTrackerHit rth = (RawTrackerHit) rthList.get(0);
//            SiSensor sensor = (SiSensor) rth.getDetectorElement();
//            local_to_global = sensor.getGeometry().getLocalToGlobal();
//            global_to_local = sensor.getGeometry().getGlobalToLocal();
            String moduleName = rth.getDetectorElement().getName();

            List<SimTrackerHit> simthList = rth.getSimTrackerHits();
            if (simthList != null) {
                SimTrackerHit sth = simthList.get(0);
                System.out.println("SimTrackerHit position " + sth.getPositionVec());
            }
            int layer = TrackUtils.getLayer(hit);
            double[] pos = stripHit.getPosition();
//            System.out.println(layer + " " + moduleName + " " + Arrays.toString(pos));
            DetectorPlane dp = _db.planeMap.get(moduleName);
//            System.out.println("layer " + layer);
//            System.out.println(" " + dp);
//            System.out.println(" " + Arrays.toString(pos));

            Hit h = makeHit(dp, pos, du);
//            System.out.println(h);
            planes.add(dp);
            hits.add(h);
        } // loop over strip clusters
//        for (Hit h : hits) {
//            System.out.println(h);
//        }
        double[] A0 = {-63., 0., -2338.}; // initial guess for (x,y,z) of track
        double[] B0 = {0., 0., 1.}; // initial guess for the track direction
        // test some things here  
        double[] par = {A0[0], A0[1], pxpz, pypz};
//        List<Hit> myHits = GENER_EVT(planes, par);
//        for (Hit h : myHits) {
//            System.out.println(h);
//        }
        TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
        // track position parameters x & y are reported at the input z.
        // can change this to an intermediate position between layers 3 and 4 to check
        // agreement of L13 and L46
        double[] pars = fit.pars();
        double[] cov = fit.cov();
        aida.cloud1D("fit chisq").fill(fit.chisq());
        aida.cloud1D("fit chisq per NDF").fill(fit.chisq() / fit.ndf());
        double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
        aida.cloud1D("fit chisq prob").fill(chisqProb);
        int[] covIndex = {0, 2, 5, 9};
        for (int i = 0; i < 4; ++i) {
            aida.cloud1D("par " + i + " meas").fill(pars[i]);
            aida.cloud1D("par " + i + " meas-pred").fill(pars[i] - par[i]);
            aida.cloud1D("par " + i + " pull").fill((pars[i] - par[i]) / sqrt(cov[covIndex[i]]));
        }
        // make some Tracks we can vertex...
        addTrackToVertexList(fit, A0);
        // if we have enogh tracks, try to vertex them
        if (tracksToVertex.size() == 1000) {
            vertexThem(tracksToVertex, A0);
            tracksToVertex.clear();
        }

//        if (debug) {
//        System.out.println("fit: " + fit);
//        System.out.println("par " + Arrays.toString(par));
//            System.out.println("parin " + Arrays.toString(parin));
//        System.out.println("fit cov " + Arrays.toString(cov));
//        }
    }

    /**
     * Given a DetectorPlane and a global position, return a hit in local
     * coordinates
     *
     * @param p
     * @param pos
     * @return
     */
    public Hit makeHit(DetectorPlane p, double[] pos, double du) {
        Matrix R = p.rot();
        double[] r0 = p.r0();
        Matrix diff = new Matrix(3, 1);
        for (int i = 0; i < 3; ++i) {
            diff.set(i, 0, pos[i] - r0[i]);
        }
//        diff.print(6, 4);
//        System.out.println("pos " + Arrays.toString(pos));
//        System.out.println("r0  " + Arrays.toString(r0));
        Matrix local = R.times(diff);
//        local.print(6, 4);
        double[] u = new double[2];  // 2dim for u and v measurement 
        double[] wt = new double[3]; // lower diag cov matrix
        double[] sigs = p.sigs();
        u[0] = local.get(0, 0);
        wt[0] = 1 / (du * du); //(sigs[0] * sigs[0]);

        return new Hit(u, wt);
    }

    @Override
    protected void endOfData() {
    }

    private List<Hit> GENER_EVT(List<DetectorPlane> dets, double[] par) {
        List<Hit> hitlist = new ArrayList<Hit>();
        int N = dets.size();
        double[] W = {0., 0., 1.};
        //initial guess is along zhat
        double[] A = {par[0], par[1], -2338.};
        double[] B = {par[2], par[3], 1.0000000};
        Matrix w = new Matrix(W, 3);
        Matrix a = new Matrix(A, 3);
        Matrix b = new Matrix(B, 3);

        for (int i = 0; i < N; ++i) {
            Hit hit;
            DetectorPlane dp = dets.get(i);
            double[] sigs = dp.sigs();
            Matrix ROT = dp.rot();
            double[] R0 = dp.r0();
            if (debug) {
                System.out.println("CALLING VMATR(W, ROT(1," + (i + 1) + "), WG, 3, 3) ");
            }
            Matrix wg = ROT.times(w);
            if (debug) {
                System.out.println(" WG " + wg.get(0, 0) + " " + wg.get(1, 0) + " " + wg.get(2, 0));
            }
            Matrix bwg = b.transpose().times(wg);
            if (debug) {
                System.out.println(" BWG " + bwg.get(0, 0));
            }
            ImpactPoint ip = GET_IMPACT(A, B, ROT, R0, wg, bwg.get(0, 0));
            System.out.println("impact " + dp.id() + " " + Arrays.toString(ip.r()));
            //TODO will have to modify when I restrict code to 1D strips
            double[] u = new double[2];  // 2dim for u and v measurement 
            double[] wt = new double[3]; // lower diag cov matrix
            for (int j = 0; j < 2; ++j) // do fluctuations
            {
                double coor = ip.q()[j]; // local u coordinate of impact point
//                double smear = sigs[j] * ran.nextGaussian(); // fluctuation
//                coor += smear;
                u[j] = coor;
                int k = j * (j + 1); // should be 0 and 2
                wt[1] = 0.; // explicitly list here in case we ever have cov between u and v
                if (sigs[j] > 0.) {
                    wt[k] = 1 / (sigs[j] * sigs[j]);
                }
                if (debug) {
                    System.out.println("MEASUREMENT Q(" + (j + 1) + ") " + ip.q()[j]);
                }
                if (debug) {
                    System.out.println("SMEARED MEASUREMENT " + (j + 1) + ") " + coor);
                }
            }
            hit = new Hit(u, wt);
//            if (use_fixed_hits) {
//                hit = hitlist.get(i);
//            }
            if (debug) {
                System.out.println("SMEARED UVM(JJ," + (i + 1) + ") " + hit.uvm()[0] + " " + hit.uvm()[1]);
            }
            if (debug) {
                System.out.println("WT(JJ, " + (i + 1) + ") " + hit.wt()[0] + " " + hit.wt()[1] + " " + hit.wt()[2]);
            }
            hitlist.add(hit);
        }
        return hitlist;
    }

    private void setupSensors(EventHeader event) {
        List<RawTrackerHit> rawTrackerHits = null;
        if (event.hasCollection(RawTrackerHit.class, "SVTRawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "SVTRawTrackerHits");
        }
        if (event.hasCollection(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits")) {
            rawTrackerHits = event.get(RawTrackerHit.class, "RawTrackerHitMaker_RawTrackerHits");
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

    private void analyzeMC(EventHeader event) {
        //cng
        // make relational table for strip clusters to mc particle
        RelationalTable mcHittomcP = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        RelationalTable rawtomc = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        if (event.hasCollection(LCRelation.class, "SVTTrueHitRelations")) {
            List<LCRelation> trueHitRelations = event.get(LCRelation.class, "SVTTrueHitRelations");
            for (LCRelation relation : trueHitRelations) {
                if (relation != null && relation.getFrom() != null && relation.getTo() != null) {
                    rawtomc.add(relation.getFrom(), relation.getTo());
                }
            }
        }
        List<TrackerHit> siClusters = event.get(TrackerHit.class, siClusterCollectionName);
        RelationalTable clustertosimhit = new BaseRelationalTable(RelationalTable.Mode.MANY_TO_MANY, RelationalTable.Weighting.UNWEIGHTED);
        for (TrackerHit cluster : siClusters) {
            List<RawTrackerHit> rawHits = cluster.getRawHits();
            for (RawTrackerHit rth : rawHits) {
                Set<SimTrackerHit> simTrackerHits = rawtomc.allFrom(rth);
                if (simTrackerHits != null) {
                    for (SimTrackerHit simhit : simTrackerHits) {
                        clustertosimhit.add(cluster, simhit);
                    }
                }
            }
        }
        //cng
        // a map of MC hit positions keyed on sensor name
        Map<String, List<Double>> mcSensorHitPositionMap = new HashMap<String, List<Double>>();
        // a map of Tracker hit positions keyed on sensor name
        Map<String, List<Double>> trackSensorHitPositionMap = new HashMap<String, List<Double>>();

        // First step is to get the SimTrackerHits and determine their location
        // in local coordinates.
        List<SimTrackerHit> simHits = event.get(SimTrackerHit.class, "TrackerHits");
        if (debug) {
            System.out.println("found " + simHits.size() + " SimTrackerHits");
        }
        // loop over each hit
        for (SimTrackerHit hit : simHits) {
            Hep3Vector stripPos = null;
            SymmetricMatrix covG = null;
            // did we correctly map clusters to this simhit?
            Set<TrackerHit> clusters = clustertosimhit.allTo(hit);
            if (debug) {
                System.out.println("found " + clusters.size() + " clusters associated to this SimTrackerHit");
            }
            int clusterSize = 0;
            if (clusters != null) {
                for (TrackerHit clust : clusters) {
                    clusterSize = clust.getRawHits().size();
                    double[] clusPos = clust.getPosition();
                    stripPos = new BasicHep3Vector(clusPos);
                    // now for the uncertainty in u
                    covG = new SymmetricMatrix(3, clust.getCovMatrix(), true);
                }
            }

            // get the hit's position in global coordinates..
            Hep3Vector globalPos = hit.getPositionVec();
            System.out.println(globalPos);
            // get the transformation from global to local
            ITransform3D g2lXform = hit.getDetectorElement().getGeometry().getGlobalToLocal();
            //System.out.println("transform matrix: " + g2lXform);
            IRotation3D rotMat = g2lXform.getRotation();
            //System.out.println("rotation matrix: " + rotMat);
            ITranslation3D transMat = g2lXform.getTranslation();
            //System.out.println("translation vector: " + transMat);
            // check that we can reproduce the local origin
            ITransform3D l2gXform = hit.getDetectorElement().getGeometry().getLocalToGlobal();
            Hep3Vector o = new BasicHep3Vector();
            //System.out.println("origin: " + o);
            // tranform the local origin into global position
            Hep3Vector localOriginInglobal = l2gXform.transformed(o);
            //System.out.println("transformed local to global: " + localOriginInglobal);
            // and now back...
            //System.out.println("and back: " + g2lXform.transformed(localOriginInglobal));
            // hmmm, so why is this not the same as the translation vector of the transform?
            //Note:
            // u is the measurement direction perpendicular to the strip
            // v is along the strip
            // w is normal to the wafer plane

            Hep3Vector localPos = g2lXform.transformed(globalPos);
//            System.out.println("Layer: " + hit.getLayer() + " Layer Number: " + hit.getLayerNumber() + " ID: " + hit.getCellID() + " " + hit.getDetectorElement().getName());
//            System.out.println("global position " + globalPos);
//            System.out.println("local  position " + localPos);
            String sensorName = hit.getDetectorElement().getName();
            double u = localPos.x();
            if (stripPos != null) {
                Hep3Vector clusLocalPos = g2lXform.transformed(stripPos);
                double clusU = clusLocalPos.x();
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u").fill(clusU - u);
                SymmetricMatrix covL = g2lXform.transformed(covG);
                double sigmaU = sqrt(covL.e(0, 0));
                aida.cloud1D(sensorName + " " + clusterSize + " strip cluster u-MC_u pull").fill((clusU - u) / sigmaU);
            }
            if (debug) {
                System.out.println("MC " + hit.getDetectorElement().getName() + " u= " + localPos.x());
            }
            if (mcSensorHitPositionMap.containsKey(sensorName)) {
                List<Double> vals = mcSensorHitPositionMap.get(sensorName);
                vals.add(u);
            } else {
                List<Double> vals = new ArrayList<Double>();
                vals.add(u);
                mcSensorHitPositionMap.put(sensorName, vals);
                System.out.println(sensorName + " " + u);
            }
        } // end of loop over SimTrackerHits        
    }

    void addTrackToVertexList(TrackFit fit, double[] A0) {
        double[] tpars = {0., 0., 0., 0., 1., A0[2]};
        double[] tcov = new double[15];
        System.arraycopy(fit.pars(), 0, tpars, 0, 4);
        System.arraycopy(fit.cov(), 0, tcov, 0, 10);
        tcov[14] = 1.;
        tracksToVertex.add(new Track(tpars, tcov));
    }

    void vertexThem(List<Track> tracksToVertex, double[] A0) {
        Vertex v = new Vertex();
        StraightLineVertexFitter.fitPrimaryVertex(tracksToVertex, A0, v);
        aida.histogram1D("vertex x",100,-90., -40.).fill(v.x());
        aida.histogram1D("vertex y",100,-10., 10.).fill(v.y());
        aida.histogram1D("vertex z",200, -3500., -1500.).fill(v.z());
        aida.histogram2D("vertex x vs y",100,-90.,-40.,100,-10.,10.).fill(v.x(),v.y());
        
//        System.out.println(v);
//        System.out.println("*************************************");
//        System.out.println("");
    }

//    public Hep3Vector weightedAveragePosition(List<Double> signals, List<Hep3Vector> positions) {
//        double total_weight = 0;
//        Hep3Vector position = new BasicHep3Vector(0, 0, 0);
//        for (int istrip = 0; istrip < signals.size(); istrip++) {
//            double signal = signals.get(istrip);
//
//            double weight = _useWeights ? signal : 1;
//            total_weight += weight;
//            position = VecOp.add(position, VecOp.mult(weight, positions.get(istrip)));
//            /*if (_debug) {
//                System.out.println(this.getClass().getSimpleName() + "strip " + istrip + ": signal " + signal + " position " + positions.get(istrip) + " -> total_position " + position.toString() + " ( total charge " + total_charge + ")");
//            }*/
//        }
//        return VecOp.mult(1 / total_weight, position);
//    }
//
//    static Hep3Vector getOrigin(SiTrackerHitStrip1D stripCluster) {
//        SiTrackerHitStrip1D local = stripCluster.getTransformedHit(TrackerHitType.CoordinateSystem.SENSOR);
//        ITransform3D trans = local.getLocalToGlobal();
//        return trans.transformed(new BasicHep3Vector(0, 0, 0));
//    }
//
//    static Hep3Vector getNormal(SiTrackerHitStrip1D s2) {
//        Hep3Vector u2 = s2.getMeasuredCoordinate();
//        Hep3Vector v2 = s2.getUnmeasuredCoordinate();
//        return VecOp.cross(u2, v2);
//    }
}
