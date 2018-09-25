package org.hps.analysis.alignment.straighttrack;

import Jama.Matrix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import static org.hps.analysis.alignment.straighttrack.FitTracks.GET_IMPACT;
import org.hps.analysis.alignment.straighttrack.vertex.StraightLineVertexFitter;
import org.hps.analysis.alignment.straighttrack.vertex.Vertex;
import org.hps.recon.tracking.TrackUtils;
import org.lcsim.detector.DetectorElementStore;
import org.lcsim.detector.IDetectorElement;
import org.lcsim.detector.identifier.IExpandedIdentifier;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.detector.identifier.IIdentifierDictionary;
import org.lcsim.detector.tracker.silicon.SiSensor;
import org.lcsim.event.EventHeader;
import org.lcsim.event.RawTrackerHit;
import org.lcsim.event.RelationalTable;
import org.lcsim.event.Track;
import org.lcsim.event.TrackerHit;
import org.lcsim.geometry.Detector;
import org.lcsim.recon.tracking.digitization.sisim.SiTrackerHitStrip1D;
import org.lcsim.util.Driver;
import org.lcsim.util.aida.AIDA;

/**
 *
 * @author ngraf
 */
public class StraightTrackLeastSquaresDataAlignmentDriver extends Driver {

    boolean debug = false;
    boolean printGeometry = true;

    private AIDA aida = AIDA.defaultInstance();
    RelationalTable hitToStrips;
    RelationalTable hitToRotated;

    private String siClusterCollectionName = "StripClusterer_SiTrackerHitStrip1D";

    DetectorBuilder _db;

    String[] fitNames = {"full fit", "L1-3 fit", "L4-6 fit"};

//    KFParticle p;
//    KFTrack kft1;
//    int nTracksInVertex = 0;
    Map<String, List<org.hps.analysis.alignment.straighttrack.vertex.Track>> topTracksToVertexMap = new HashMap<>();
    Map<String, List<org.hps.analysis.alignment.straighttrack.vertex.Track>> bottomTracksToVertexMap = new HashMap<>();
//    List<org.hps.analysis.alignment.straighttrack.vertex.Track> tracksToVertex = new ArrayList<org.hps.analysis.alignment.straighttrack.vertex.Track>();

    @Override
    protected void detectorChanged(Detector detector) {
        _db = new DetectorBuilder(detector);
        for (String s : fitNames) {
            topTracksToVertexMap.put(s, new ArrayList<org.hps.analysis.alignment.straighttrack.vertex.Track>());
            bottomTracksToVertexMap.put(s, new ArrayList<org.hps.analysis.alignment.straighttrack.vertex.Track>());
        }
    }

    @Override
    protected void process(EventHeader event) {

        hitToStrips = TrackUtils.getHitToStripsTable(event);
        hitToRotated = TrackUtils.getHitToRotatedTable(event);

        setupSensors(event);
        List<TrackerHit> stripClusters = event.get(TrackerHit.class, "StripClusterer_SiTrackerHitStrip1D");

        List<DetectorPlane> planes = new ArrayList<DetectorPlane>();
        List<Hit> hits = new ArrayList<Hit>();

        // some maps so we can slice-and-dice the data, e.g. L13 vs L46
        Map<Integer, DetectorPlane> planeMap = new HashMap<Integer, DetectorPlane>();
        Map<Integer, Hit> hitMap = new HashMap<Integer, Hit>();

        //assign an error based on the size of the strip cluster
        double[] fixedDu = {0., .012, .006};

        List<Track> tracks = event.get(Track.class, "MatchedTracks");
        if (tracks.size() == 1) {
            for (Track t : tracks) {
                List<TrackerHit> trkrHits = t.getTrackerHits(); // 2D HelicalTrackHits
                if (trkrHits.size() == 6) {
                    for (TrackerHit h : trkrHits) {
                        // get the strip cluster hits associated with this 2D axial-stereo pair
                        Set<TrackerHit> stripList = hitToStrips.allFrom(hitToRotated.from(h));
                        for (TrackerHit strip : stripList) {
                            SiTrackerHitStrip1D stripHit = new SiTrackerHitStrip1D(strip);
                            List rthList = strip.getRawHits();
                            int size = rthList.size();
                            double du;
                            if (size < 3) {
                                du = fixedDu[size];
                            } else {
                                du = .04;
                            }
                            RawTrackerHit rth = (RawTrackerHit) rthList.get(0);
                            String moduleName = rth.getDetectorElement().getName();

                            int layer = TrackUtils.getLayer(strip);
                            double[] pos = stripHit.getPosition();
                            DetectorPlane dp = _db.planeMap.get(moduleName);
                            Hit myHit = makeHit(dp, pos, du);
                            planes.add(dp);
                            planeMap.put(layer, dp);
                            hits.add(myHit);
                            hitMap.put(layer, myHit);
                        }
                    }
                } // end of loop over six-hit tracks
            } // end of loop over tracks        

            double[] A0 = {-63., 0., -2338.}; // initial guess for (x,y,z) of track
            double[] B0 = {0., 0., 1.}; // initial guess for the track direction
            // test some things here  

            TrackFit fit = FitTracks.STR_LINFIT(planes, hits, A0, B0);
            // track position parameters x & y are reported at the input z.
            // can change this to an intermediate position between layers 3 and 4 to check
            // agreement of L13 and L46
            // don't (yet) have a way to flag bad fits.
            // for now, simply remove tracks with dydz==0

            double[] pars = fit.pars();
            double[] cov = fit.cov();
            String half = pars[3] > 0. ? "top" : "bottom";
            if (pars[3] != 0.) {
                aida.histogram1D(half + " fit chisq", 100, 0., 100.).fill(fit.chisq());
                double chisqNdf = fit.chisq() / fit.ndf();
                aida.histogram1D(half + " fit chisq per NDF", 100, 0., 100.).fill(chisqNdf);
                //
//                System.out.println(Arrays.toString(pars));
//                System.out.println(Arrays.toString(cov));
//                System.out.println(fit.ndf());
                //
//        double chisqProb = ChisqProb.gammp(fit.ndf(), fit.chisq());
//        aida.cloud1D("fit chisq prob").fill(chisqProb);
                int[] covIndex = {0, 2, 5, 9};
                //       for (int i = 0; i < 4; ++i) {
                aida.histogram1D(half + " X at HARP wire", 100, -100., 0.).fill(pars[0]);
                aida.histogram1D(half + " Y at HARP wire", 100, -20., 20.).fill(pars[1]);
                aida.histogram2D(half + " X vs Y at HARP wire", 100, -90., -45., 100, -10., 10.).fill(pars[0], pars[1]);
                aida.histogram1D(half + " dXdZ at HARP wire", 100, -0.05, 0.05).fill(pars[2]);
                aida.histogram1D(half + " dYdZ at HARP wire", 100, -0.025, 0.025).fill(pars[3]);
                double chisqCut = 25.;
                if (chisqNdf < chisqCut) {
                    scanVertexZ(fit, A0[2]);
                    aida.histogram1D(half + " X at HARP wire chisq < " + chisqCut, 100, -100., 0.).fill(pars[0]);
                    aida.histogram1D(half + " Y at HARP wire chisq < " + chisqCut, 100, -20., 20.).fill(pars[1]);
                    aida.histogram2D(half + " X vs Y at HARP wire chisq < " + chisqCut, 100, -90., -45., 100, -10., 10.).fill(pars[0], pars[1]);
                    aida.histogram1D(half + " dXdZ at HARP wire chisq < " + chisqCut, 100, -0.05, 0.05).fill(pars[2]);
                    aida.histogram1D(half + " dYdZ at HARP wire chisq < " + chisqCut, 100, -0.025, 0.025).fill(pars[3]);
                    // let's look at L13 vs L46
                    List<DetectorPlane> planes13 = new ArrayList<DetectorPlane>();
                    List<Hit> hits13 = new ArrayList<Hit>();
                    List<DetectorPlane> planes46 = new ArrayList<DetectorPlane>();
                    List<Hit> hits46 = new ArrayList<Hit>();
                    for (int i = 1; i < 7; ++i) {
                        planes13.add(planeMap.get(i));
                        hits13.add(hitMap.get(i));
                    }
                    for (int i = 7; i < 13; ++i) {
                        planes46.add(planeMap.get(i));
                        hits46.add(hitMap.get(i));
                    }
                    // define the fit plane halfway between
                    double z34 = 400.;
                    double[] A1346 = {0., 0., z34}; // midway between layer 3 and 4
                    TrackFit fit13 = FitTracks.STR_LINFIT(planes13, hits13, A1346, B0);
                    TrackFit fit46 = FitTracks.STR_LINFIT(planes46, hits46, A1346, B0);
                    double[] pars13 = fit13.pars();
                    double[] pars46 = fit46.pars();
                    aida.cloud2D(" X vs Y at z = " + z34 + " chisq < " + chisqCut).fill(pars13[0], pars13[1]);
                    aida.histogram1D(half + " dX at z = " + z34 + " chisq < " + chisqCut, 100, -2.5, 2.5).fill(pars13[0] - pars46[0]);
                    aida.cloud2D(half + " dY vs X at z = " + z34 + " chisq < " + chisqCut).fill(pars13[0], pars13[1] - pars46[1]);
                    aida.histogram1D(half + " dY at z = " + z34 + " chisq < " + chisqCut, 100, -0.25, 0.25).fill(pars13[1] - pars46[1]);
                    aida.profile1D(half + " dY vs X at z = " + z34 + " chisq < " + chisqCut + " profile", 100, -40., 10.).fill(pars13[0], pars13[1] - pars46[1]);
                    aida.histogram1D(half + " ddXdZ at z = " + z34 + " chisq < " + chisqCut, 100, -0.01, 0.01).fill(pars13[2] - pars46[2]);
                    aida.histogram1D(half + " ddYdZ at z = " + z34 + " chisq < " + chisqCut, 100, -0.001, 0.001).fill(pars13[3] - pars46[3]);

                    // try some vertexing here...
//                    double mass = 0.511;
//                    boolean isElectron = true;
//                    int NDF = fit.ndf();
//                    double chisq = fit.chisq();
//                    double[] tparams = new double[6];
//                    double[] tcov = new double[15];
//                    System.arraycopy(pars, 0, tparams, 0, 4);
//                    tparams[4] = 1.;
//                    tparams[5] = -2338.;
//                    System.arraycopy(cov, 0, tcov, 0, 10);
//                    tcov[14] = 1.;
//                    KFTrack t = null;
//                    if (kft1 == null) {
//                        kft1 = new KFTrack(tparams, tcov, mass, chisq, isElectron, NDF);
//                    } else {
//                        t = new KFTrack(tparams, tcov, mass, chisq, isElectron, NDF);
//                    }
//                    if (p == null) {
//                        if (kft1 != null && t != null) {
//                            p = new KFParticle(new KFParticle(kft1), new KFParticle(t));
//                            nTracksInVertex += 2;
//                        }
//                    } else {
//                        p.AddDaughter(new KFParticle(t));
//                        nTracksInVertex++;
//                    }
//
//                    if (nTracksInVertex == 100) {
//                        System.out.println("pos: " + p.GetX() + " " + p.GetY() + " " + p.GetZ());
//                        p = null;
//                        kft1 = null;
//                        nTracksInVertex = 0;
//                    }
                    // make some Tracks we can vertex...
                    //refit L13 and L14 so we can vertex at target
                    fit13 = FitTracks.STR_LINFIT(planes13, hits13, A0, B0);
                    fit46 = FitTracks.STR_LINFIT(planes46, hits46, A0, B0);
                    addTrackToVertexList("full fit", half, fit, A0);
                    addTrackToVertexList("L1-3 fit", half, fit13, A0);
                    addTrackToVertexList("L4-6 fit", half, fit46, A0);

                    // if we have enogh tracks, try to vertex them
                    for (String s : fitNames) {
                        List<org.hps.analysis.alignment.straighttrack.vertex.Track> trackList = half.equals("top")? topTracksToVertexMap.get(s) : bottomTracksToVertexMap.get(s);
                        if (trackList.size() == 100) {
                            vertexThem(s, half, trackList, A0);
                            trackList.clear();
                        }
                    }
                }
            }
        } //end of one track requirement
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
        Matrix local = R.times(diff);
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

    void scanVertexZ(TrackFit fit, double z0) {
        aida.tree().mkdirs("vertexZscan");
        aida.tree().cd("vertexZscan");
        double[] pars = fit.pars();
        for (int i = 0; i < 100; ++i) {
            double dz = -50. + i;
            double x = pars[0] + dz * pars[2];
            double y = pars[1] + dz * pars[3];
            aida.histogram2D("x vs y at " + (z0 + dz), 100, -75., -50., 100, -10., 10.).fill(x, y);
        }
        aida.tree().cd("/");
    }

    void addTrackToVertexList(String s, String half, TrackFit fit, double[] A0) {
        List<org.hps.analysis.alignment.straighttrack.vertex.Track> tracks = half.equals("top")? topTracksToVertexMap.get(s) : bottomTracksToVertexMap.get(s);;
        double[] tpars = {0., 0., 0., 0., 1., A0[2]};
        double[] tcov = new double[15];
        System.arraycopy(fit.pars(), 0, tpars, 0, 4);
        System.arraycopy(fit.cov(), 0, tcov, 0, 10);
        tcov[14] = 1.;
        tracks.add(new org.hps.analysis.alignment.straighttrack.vertex.Track(tpars, tcov));
    }

    void vertexThem(String s, String half, List<org.hps.analysis.alignment.straighttrack.vertex.Track> tracksToVertex, double[] A0) {
//        List<org.hps.analysis.alignment.straighttrack.vertex.Track> tracks = tracksToVertexMap.get(s);
        aida.tree().mkdirs(s+" "+half);
        aida.tree().cd(s+" "+half);
        Vertex v = new Vertex();
        StraightLineVertexFitter.fitPrimaryVertex(tracksToVertex, A0, v);
        aida.histogram1D("vertex x", 50, -100., -50.).fill(v.x());
        aida.histogram1D("vertex y", 50, -20., 20.).fill(v.y());
        aida.histogram1D("vertex z", 100, -4000., -1500.).fill(v.z());
        aida.histogram2D("vertex x vs y", 100, -100., -50., 100, -20., 20.).fill(v.x(), v.y());
        aida.cloud1D("vertex x cloud").fill(v.x());
        aida.cloud1D("vertex y cloud").fill(v.y());
        aida.cloud1D("vertex z cloud").fill(v.z());
        aida.cloud2D("vertex x vs y cloud").fill(v.x(), v.y());
        aida.cloud1D("vertex nTracks").fill(v.ntracks());
        aida.tree().cd("/");
//        System.out.println(v);
//        System.out.println("*************************************");
//        System.out.println("");
    }
}
