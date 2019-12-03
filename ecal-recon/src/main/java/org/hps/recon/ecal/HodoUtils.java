package org.hps.recon.ecal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.hps.detector.hodoscope.HodoscopePixelDetectorElement;
import org.lcsim.detector.DetectorElement;
import org.lcsim.detector.identifier.IIdentifier;
import org.lcsim.geometry.Detector;

/**
 *
 * @author mgraham Utility classes for Hodoscope created: 7/20/2019 10/5/19:
 *         moved a bunch of helper methods from monitoring HodoUtils (same name)
 *         and deleted the class in monitoring; also added some maps for
 *         Top/Bottom/L0/L1 and L0/L1 bi-layer clustering
 */
public class HodoUtils {
    public Map<HodoTileIdentifier, List<HodoTileIdentifier>> clusterPairMap = new HashMap<HodoTileIdentifier, List<HodoTileIdentifier>>();
    public List<HodoTileIdentifier> L0TopList = new ArrayList<>();
    public List<HodoTileIdentifier> L1TopList = new ArrayList<>();
    public List<HodoTileIdentifier> L0BotList = new ArrayList<>();
    public List<HodoTileIdentifier> L1BotList = new ArrayList<>();

    public static Map<IIdentifier, DetectorElement> getHodoscopeMap(Detector detector) {
        Map<IIdentifier, DetectorElement> hodoMap = new HashMap<IIdentifier, DetectorElement>();
        List<HodoscopePixelDetectorElement> pixels = detector.getSubdetector("Hodoscope").getDetectorElement()
                .findDescendants(HodoscopePixelDetectorElement.class);
        for (HodoscopePixelDetectorElement pix : pixels)
            hodoMap.put(pix.getIdentifier(), pix);
        return hodoMap;
    }

    public HodoUtils() {
        initStaff();
        makeIdentifierLists();
        makeClusterPairMap();
    }

    public final double hodo_t_max = 64.;
    public final double hodo_t_min = 2.;

    public final double hodotileECut = 200.;
    public final double hodoNoHitCut = 80.;

    public double cl_x_coordinates_centers[] = { 35.6778, 49.3773, 62.8806, 76.3915, 89.9138, 103.451, 117.008, 130.587,
            144.193, 157.83, 171.501, 185.211, 198.964, 212.763, 226.614, 240.52, 254.485, 268.514, 282.612, 296.783,
            311.032, 325.364, 339.784, 354.296 };

    public double cl_x_edges[] = new double[24];

    void initStaff() {
        for (int i = 0; i < 23; i++) {
            cl_x_edges[i] = 0.5 * (cl_x_coordinates_centers[i] + cl_x_coordinates_centers[i + 1]);
        }
        cl_x_edges[23] = 360.;
    }

    void makeIdentifierLists() {
        // top tiles
        HodoTileIdentifier L1_T1 = new HodoTileIdentifier(0, 1, 0);
        HodoTileIdentifier L1_T2 = new HodoTileIdentifier(1, 1, 0);
        HodoTileIdentifier L1_T3 = new HodoTileIdentifier(2, 1, 0);
        HodoTileIdentifier L1_T4 = new HodoTileIdentifier(3, 1, 0);
        HodoTileIdentifier L1_T5 = new HodoTileIdentifier(4, 1, 0);
        L0TopList.add(L1_T1);
        L0TopList.add(L1_T2);
        L0TopList.add(L1_T3);
        L0TopList.add(L1_T4);
        L0TopList.add(L1_T5);
        HodoTileIdentifier L2_T1 = new HodoTileIdentifier(0, 1, 1);
        HodoTileIdentifier L2_T2 = new HodoTileIdentifier(1, 1, 1);
        HodoTileIdentifier L2_T3 = new HodoTileIdentifier(2, 1, 1);
        HodoTileIdentifier L2_T4 = new HodoTileIdentifier(3, 1, 1);
        HodoTileIdentifier L2_T5 = new HodoTileIdentifier(4, 1, 1);
        L1TopList.add(L2_T1);
        L1TopList.add(L2_T2);
        L1TopList.add(L2_T3);
        L1TopList.add(L2_T4);
        L1TopList.add(L2_T5);
        // bottom tiles
        HodoTileIdentifier L1_B1 = new HodoTileIdentifier(0, -1, 0);
        HodoTileIdentifier L1_B2 = new HodoTileIdentifier(1, -1, 0);
        HodoTileIdentifier L1_B3 = new HodoTileIdentifier(2, -1, 0);
        HodoTileIdentifier L1_B4 = new HodoTileIdentifier(3, -1, 0);
        HodoTileIdentifier L1_B5 = new HodoTileIdentifier(4, -1, 0);
        L0TopList.add(L1_B1);
        L0TopList.add(L1_B2);
        L0TopList.add(L1_B3);
        L0TopList.add(L1_B4);
        L0TopList.add(L1_B5);
        HodoTileIdentifier L2_B1 = new HodoTileIdentifier(0, -1, 1);
        HodoTileIdentifier L2_B2 = new HodoTileIdentifier(1, -1, 1);
        HodoTileIdentifier L2_B3 = new HodoTileIdentifier(2, -1, 1);
        HodoTileIdentifier L2_B4 = new HodoTileIdentifier(3, -1, 1);
        HodoTileIdentifier L2_B5 = new HodoTileIdentifier(4, -1, 1);
        L0TopList.add(L2_B1);
        L0TopList.add(L2_B2);
        L0TopList.add(L2_B3);
        L0TopList.add(L2_B4);
        L0TopList.add(L2_B5);
    }

    void makeClusterPairMap() {
        // top tiles
        HodoTileIdentifier L1_T1 = new HodoTileIdentifier(0, 1, 0);
        HodoTileIdentifier L1_T2 = new HodoTileIdentifier(1, 1, 0);
        HodoTileIdentifier L1_T3 = new HodoTileIdentifier(2, 1, 0);
        HodoTileIdentifier L1_T4 = new HodoTileIdentifier(3, 1, 0);
        HodoTileIdentifier L1_T5 = new HodoTileIdentifier(4, 1, 0);
        HodoTileIdentifier L2_T1 = new HodoTileIdentifier(0, 1, 1);
        HodoTileIdentifier L2_T2 = new HodoTileIdentifier(1, 1, 1);
        HodoTileIdentifier L2_T3 = new HodoTileIdentifier(2, 1, 1);
        HodoTileIdentifier L2_T4 = new HodoTileIdentifier(3, 1, 1);
        HodoTileIdentifier L2_T5 = new HodoTileIdentifier(4, 1, 1);
        // bottom tiles
        HodoTileIdentifier L1_B1 = new HodoTileIdentifier(0, -1, 0);
        HodoTileIdentifier L1_B2 = new HodoTileIdentifier(1, -1, 0);
        HodoTileIdentifier L1_B3 = new HodoTileIdentifier(2, -1, 0);
        HodoTileIdentifier L1_B4 = new HodoTileIdentifier(3, -1, 0);
        HodoTileIdentifier L1_B5 = new HodoTileIdentifier(4, -1, 0);
        HodoTileIdentifier L2_B1 = new HodoTileIdentifier(0, -1, 1);
        HodoTileIdentifier L2_B2 = new HodoTileIdentifier(1, -1, 1);
        HodoTileIdentifier L2_B3 = new HodoTileIdentifier(2, -1, 1);
        HodoTileIdentifier L2_B4 = new HodoTileIdentifier(3, -1, 1);
        HodoTileIdentifier L2_B5 = new HodoTileIdentifier(4, -1, 1);
        // top L0/L1 pairs
        List<HodoTileIdentifier> t1t = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_T1));
        clusterPairMap.put(L1_T1, t1t);
        List<HodoTileIdentifier> t2t = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_T1, L2_T2));
        clusterPairMap.put(L1_T2, t2t);
        List<HodoTileIdentifier> t3t = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_T2, L2_T3));
        clusterPairMap.put(L1_T3, t3t);
        List<HodoTileIdentifier> t4t = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_T3, L2_T4));
        clusterPairMap.put(L1_T4, t4t);
        List<HodoTileIdentifier> t5t = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_T4, L2_T5));
        clusterPairMap.put(L1_T5, t5t);
        // bottom L0/L1 pairs
        List<HodoTileIdentifier> t1b = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_B1));
        clusterPairMap.put(L1_B1, t1b);
        List<HodoTileIdentifier> t2b = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_B1,L2_B2));
        clusterPairMap.put(L1_B2, t2b);
        List<HodoTileIdentifier> t3b = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_B2,L2_B3));
        clusterPairMap.put(L1_B3, t3b);
        List<HodoTileIdentifier> t4b = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_B3,L2_B4));
        clusterPairMap.put(L1_B4, t4b);
        List<HodoTileIdentifier> t5b = new ArrayList<HodoTileIdentifier>(Arrays.asList(L2_B4, L2_B5));
        clusterPairMap.put(L1_B5, t5b);
    }

    public boolean GoodTileHit(HodoTileIdentifier tile_id, Map<HodoTileIdentifier, Double> m_tileE) {

        boolean goodTileHit = false;

        int ix = tile_id.ix;
        int iy = tile_id.iy;
        int ilayer = tile_id.ilayer;

        double noHit = hodoNoHitCut;
        double Hit = hodotileECut;

        if (iy > 0) {

            HodoTileIdentifier L1_1 = new HodoTileIdentifier(0, 1, 0);
            HodoTileIdentifier L1_2 = new HodoTileIdentifier(1, 1, 0);
            HodoTileIdentifier L1_3 = new HodoTileIdentifier(2, 1, 0);
            HodoTileIdentifier L1_4 = new HodoTileIdentifier(3, 1, 0);
            HodoTileIdentifier L1_5 = new HodoTileIdentifier(4, 1, 0);
            HodoTileIdentifier L2_1 = new HodoTileIdentifier(0, 1, 1);
            HodoTileIdentifier L2_2 = new HodoTileIdentifier(1, 1, 1);
            HodoTileIdentifier L2_3 = new HodoTileIdentifier(2, 1, 1);
            HodoTileIdentifier L2_4 = new HodoTileIdentifier(3, 1, 1);
            HodoTileIdentifier L2_5 = new HodoTileIdentifier(4, 1, 1);

            double E1_1 = m_tileE.containsKey(L1_1) ? m_tileE.get(L1_1) : 0.;
            double E1_2 = m_tileE.containsKey(L1_2) ? m_tileE.get(L1_2) : 0.;
            double E1_3 = m_tileE.containsKey(L1_3) ? m_tileE.get(L1_3) : 0.;
            double E1_4 = m_tileE.containsKey(L1_4) ? m_tileE.get(L1_4) : 0.;
            double E1_5 = m_tileE.containsKey(L1_5) ? m_tileE.get(L1_5) : 0.;
            double E2_1 = m_tileE.containsKey(L2_1) ? m_tileE.get(L2_1) : 0.;
            double E2_2 = m_tileE.containsKey(L2_2) ? m_tileE.get(L2_2) : 0.;
            double E2_3 = m_tileE.containsKey(L2_3) ? m_tileE.get(L2_3) : 0.;
            double E2_4 = m_tileE.containsKey(L2_4) ? m_tileE.get(L2_4) : 0.;
            double E2_5 = m_tileE.containsKey(L2_5) ? m_tileE.get(L2_5) : 0.;

            if (ilayer == 0) {

                if (ix == 0) {

                    if (E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_2 < noHit && E2_3 < noHit
                            && E2_4 < noHit && E2_5 < noHit && E2_1 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E1_1 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_3 < noHit && E2_4 < noHit
                            && E2_5 < noHit && E2_1 + E2_2 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_4 < noHit && E1_5 < noHit && E2_1 < noHit && E2_4 < noHit
                            && E2_5 < noHit && (E2_2 + E2_3) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 3) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_5 < noHit && E2_1 < noHit && E2_2 < noHit
                            && E2_5 < noHit && (E2_3 + E2_4) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 4) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E2_1 < noHit && E2_2 < noHit
                            && E2_3 < noHit && (E2_4 + E2_5) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                }

            } else if (ilayer == 1) {

                if (ix == 0) {

                    if (E2_2 < noHit && E2_3 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_3 < noHit
                            && E1_4 < noHit && E1_5 < noHit && (E1_1 + E1_2) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E2_1 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_4 < noHit
                            && E1_5 < noHit && (E1_2 + E1_3 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_5 < noHit && (E1_3 + E1_4 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 3) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_3 < noHit && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 4) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_3 < noHit && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                }

            }
        } else {

            HodoTileIdentifier L1_1 = new HodoTileIdentifier(0, -1, 0);
            HodoTileIdentifier L1_2 = new HodoTileIdentifier(1, -1, 0);
            HodoTileIdentifier L1_3 = new HodoTileIdentifier(2, -1, 0);
            HodoTileIdentifier L1_4 = new HodoTileIdentifier(3, -1, 0);
            HodoTileIdentifier L1_5 = new HodoTileIdentifier(4, -1, 0);
            HodoTileIdentifier L2_1 = new HodoTileIdentifier(0, -1, 1);
            HodoTileIdentifier L2_2 = new HodoTileIdentifier(1, -1, 1);
            HodoTileIdentifier L2_3 = new HodoTileIdentifier(2, -1, 1);
            HodoTileIdentifier L2_4 = new HodoTileIdentifier(3, -1, 1);
            HodoTileIdentifier L2_5 = new HodoTileIdentifier(4, -1, 1);

            double E1_1 = m_tileE.containsKey(L1_1) ? m_tileE.get(L1_1) : 0.;
            double E1_2 = m_tileE.containsKey(L1_2) ? m_tileE.get(L1_2) : 0.;
            double E1_3 = m_tileE.containsKey(L1_3) ? m_tileE.get(L1_3) : 0.;
            double E1_4 = m_tileE.containsKey(L1_4) ? m_tileE.get(L1_4) : 0.;
            double E1_5 = m_tileE.containsKey(L1_5) ? m_tileE.get(L1_5) : 0.;
            double E2_1 = m_tileE.containsKey(L2_1) ? m_tileE.get(L2_1) : 0.;
            double E2_2 = m_tileE.containsKey(L2_2) ? m_tileE.get(L2_2) : 0.;
            double E2_3 = m_tileE.containsKey(L2_3) ? m_tileE.get(L2_3) : 0.;
            double E2_4 = m_tileE.containsKey(L2_4) ? m_tileE.get(L2_4) : 0.;
            double E2_5 = m_tileE.containsKey(L2_5) ? m_tileE.get(L2_5) : 0.;

            if (ilayer == 0) {

                if (ix == 0) {

                    if (E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_2 < noHit && E2_3 < noHit
                            && E2_4 < noHit && E2_5 < noHit && E2_1 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E1_1 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_3 < noHit && E2_4 < noHit
                            && E2_5 < noHit && E2_1 + E2_2 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_4 < noHit && E1_5 < noHit && E2_1 < noHit && E2_4 < noHit
                            && E2_5 < noHit && (E2_2 + E2_3) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 3) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_5 < noHit && E2_1 < noHit && E2_2 < noHit
                            && E2_5 < noHit && (E2_3 + E2_4) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 4) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E2_1 < noHit && E2_2 < noHit
                            && E2_3 < noHit && (E2_4 + E2_5) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                }

            } else if (ilayer == 1) {

                if (ix == 0) {

                    if (E2_2 < noHit && E2_3 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_3 < noHit
                            && E1_4 < noHit && E1_5 < noHit && (E1_1 + E1_2) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E2_1 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_4 < noHit
                            && E1_5 < noHit && (E1_2 + E1_3 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_5 < noHit && (E1_3 + E1_4 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 3) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_3 < noHit && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 4) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit && E1_1 < noHit && E1_2 < noHit
                            && E1_3 < noHit && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                }

            }

        }

        return goodTileHit;
    }

    public boolean hasRightClust(HodoTileIdentifier tile_id, double cl_x, double cl_E) {

        boolean rightCluster = false;

        if (tile_id.ilayer == 0) {

            if (tile_id.ix == 0) {

                if (cl_x > 100 && cl_x < 130. && cl_E > 2.5 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 1) {

                if (cl_x > 140 && cl_x < 170. && cl_E > 1.7 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 2) {

                if (cl_x > 180 && cl_x < 260. && cl_E > 1. && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 3) {

                if (cl_x > 250 && cl_x < 340. && cl_E > 0.7 && cl_E < 1.9) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 4) {
                if (cl_x > 320 && cl_E < 1.3) {
                    rightCluster = true;
                    return rightCluster;
                }

            }

        } else if (tile_id.ilayer == 1) {

            if (tile_id.ix == 0) {
                if (cl_x > 100 && cl_x < 140. && cl_E > 2.3 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 1) {
                if (cl_x > 130 && cl_x < 215. && cl_E > 1.4 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 2) {
                if (cl_x > 200 && cl_x < 290. && cl_E > 0.9 && cl_E < 2.4) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 3) {
                if (cl_x > 270 && cl_x < 350. && cl_E < 1.7) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 4) {
                if (cl_x > 320 && cl_E < 1.4) {
                    rightCluster = true;
                    return rightCluster;
                }
            }

        }

        return rightCluster;
    }

    boolean hasRightClust(HodoTileIdentifier tile_id, int cl_ind, double cl_E) {

        boolean rightCluster = false;

        if (tile_id.ilayer == 0) {

            if (tile_id.ix == 0) {

                if (cl_ind >= 5 && cl_ind <= 9 && cl_E > 2.5 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 1) {

                if (cl_ind >= 6 && cl_ind <= 12 && cl_E > 1.7 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 2) {
                if (cl_ind >= 10 && cl_ind <= 17 && cl_E > 1. && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 3) {
                if (cl_ind >= 15 && cl_ind <= 21 && cl_E > 0.7 && cl_E < 1.9) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 4) {
                if (cl_ind >= 18 && cl_ind <= 23 && cl_E < 1.4) {
                    rightCluster = true;
                    return rightCluster;
                }
            }

        } else if (tile_id.ilayer == 1) {

            if (tile_id.ix == 0) {

                if (cl_ind >= 5 && cl_ind <= 9 && cl_E > 2.5 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }

            } else if (tile_id.ix == 1) {

                if (cl_ind >= 6 && cl_ind <= 14 && cl_E > 1.7 && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 2) {
                if (cl_ind >= 12 && cl_ind <= 18 && cl_E > 1. && cl_E < 3.) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 3) {
                if (cl_ind >= 16 && cl_ind <= 22 && cl_E > 0.7 && cl_E < 1.9) {
                    rightCluster = true;
                    return rightCluster;
                }
            } else if (tile_id.ix == 4) {
                if (cl_ind >= 20 && cl_ind <= 23 && cl_E < 1.4) {
                    rightCluster = true;
                    return rightCluster;
                }
            }

        }

        return rightCluster;

    }

    public class HodoTileIdentifier {

        public int ix;
        public int iy;
        public int ilayer;

        public HodoTileIdentifier(int ax, int ay, int alayer) {
            ix = ax;
            iy = ay;
            ilayer = alayer;
        }

        @Override
        public int hashCode() {
            return 1000 * ix + 100 * iy + 10 * ilayer;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final HodoTileIdentifier other = (HodoTileIdentifier) obj;
            if (!(ix == other.ix && iy == other.iy && ilayer == other.ilayer)) {
                return false;
            }

            return true;
        }

    }

// ***  Container class for Hodoscope Clusters == hole combinations  ***//
    public class HodoCluster {
        private HodoTileIdentifier hodoTileID;
        private double clEnergy;
        private double clTime;

        public HodoCluster(int ax, int ay, int alayer, double energy, double time) {
            clEnergy = energy;
            clTime = time;
            hodoTileID = new HodoTileIdentifier(ax, ay, alayer);
        }

        public double getEnergy() {
            return clEnergy;
        }

        public double getTime() {
            return clTime;
        }

        public HodoTileIdentifier getHodoID() {
            return hodoTileID;
        }

        public int getLayer() {
            return hodoTileID.ilayer;
        }

        public int getXId() {
            return hodoTileID.ix;
        }

        public int getYId() {
            return hodoTileID.iy;
        }

    }

    // *** Container class for Hodoscope Cluster Pairs == layer cluster combinations
    // ***//
    public class HodoClusterPair {
        private HodoCluster hodoCl0;
        private HodoCluster hodoCl1;

        public HodoClusterPair(HodoCluster cl0, HodoCluster cl1) {
            if (cl0.getLayer() == cl1.getLayer())
                System.out.println("Pairing two HodoClusters from same layer!  Don't do this!");
            if (cl0.getLayer() == 1) {
                hodoCl0 = cl1;
                hodoCl1 = cl0;
            } else {
                hodoCl0 = cl1;
                hodoCl1 = cl0;
            }
        }

        public HodoClusterPair() {
            // TODO Auto-generated constructor stub
        }

        public HodoCluster getLayer0Cluster() {
            return hodoCl0;
        }

        public HodoCluster getLayer1Cluster() {
            return hodoCl1;
        }

        public double getMeanTime() {
            return (hodoCl0.getTime() + hodoCl1.getTime()) / 2.;
        }

        public double getTimeDiff() {
            return (hodoCl0.getTime() - hodoCl1.getTime());
        }

        public double getMeanEnergy() {
            return (hodoCl0.getEnergy() + hodoCl1.getEnergy()) / 2.;
        }
    }

    public List<HodoCluster> makeHodoClusterList(List<SimpleGenericObject> hodoClusters) {
        List<HodoCluster> clusters = new ArrayList<HodoCluster>();
        int n_hits = hodoClusters.get(0).getNInt();
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
            double Energy = hodoClusters.get(3).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(4).getDoubleVal(ihit);
            clusters.add(new HodoCluster(ix, iy, layer, Energy, hit_time));
        }
        return clusters;
    }

    // given an L0 cluster, find an L1 cluster that matches up
    // with a partner in clusterPairMap whose energy> minE
    // and within time maxdT
    // returns null if no matching hits found!
    // Currently, we make the L0/L1 pair from the highest in-time L1 cluster;
    // ...maybe we should modify this to include possibility of having both
    // neighboring L1 tiles in the L0/L1 "pair"? How probable is it that track
    // hits two adjacent tiles?
    public HodoClusterPair findHodoClusterPair(HodoCluster l0Cluster, List<HodoCluster> allClusters, double minE,
            double maxDt) {
        HodoCluster l1ClusterMatch = null;
        HodoClusterPair pair = null;
        HodoTileIdentifier l0Id = l0Cluster.getHodoID();
        List<HodoTileIdentifier> pairIds = clusterPairMap.get(l0Id);
        System.out.println("findHodoClusterPair:: l0Cluster Energy = " + l0Cluster.getEnergy() + "; time = "
                + l0Cluster.getTime());
        for (HodoCluster cl : allClusters) {
            if (cl.getLayer() == 0)
                continue;
            System.out.println("findHodoClusterPair::  found l1 cluster with energy =" + cl.getEnergy() + " and time = "
                    + cl.getTime());
            if (clusterPairMap.get(l0Id).contains(cl.getHodoID())) { // found a potential pair
                System.out.println("l1 cluster is a neighbor");
                if (cl.clEnergy < minE)
                    continue;
                double dt = l0Cluster.getTime() - cl.getTime();
                if (Math.abs(dt) > maxDt)
                    continue;
                System.out.println("l1 cluster passes cuts !");
                // ok, this neighbor cluster passes cuts...pick out highest energy
                if (l1ClusterMatch != null) {
                    if (cl.getEnergy() < l1ClusterMatch.getEnergy())
                        continue;
                }
                l1ClusterMatch = cl;
            }
        }
        if (l1ClusterMatch != null)
            pair = new HodoClusterPair(l0Cluster, l1ClusterMatch);
        return pair;
    }

    public List<HodoCluster> makeCleanHodoClusterList(List<SimpleGenericObject> hodoClusters, double minE,
            double meanTime, double maxDt) {
        List<HodoCluster> clusters = new ArrayList<HodoCluster>();
        int n_hits = hodoClusters.get(0).getNInt();
        for (int ihit = 0; ihit < n_hits; ihit++) {
            int ix = hodoClusters.get(0).getIntVal(ihit);
            int iy = hodoClusters.get(1).getIntVal(ihit);
            int layer = hodoClusters.get(2).getIntVal(ihit);
            double Energy = hodoClusters.get(3).getDoubleVal(ihit);
            double hit_time = hodoClusters.get(4).getDoubleVal(ihit);
            if (Energy > minE && Math.abs(hit_time - meanTime) < maxDt)
                clusters.add(new HodoCluster(ix, iy, layer, Energy, hit_time));
        }
        return clusters;
    }

}
