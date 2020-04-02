package org.hps.monitoring.drivers.hodoscope;

import java.util.Map;

/**
 *
 * @author rafopar
 */
public class HodoUtils {

    HodoUtils() {
        initStaff();
    }

    public final double hodo_t_max = 64.; 
    public final double hodo_t_min = 2.;

    public final double hodotileECut = 200.;
    public final double hodoNoHitCut = 80.;

    double cl_x_coordinates_centers[] = {35.6778, 49.3773, 62.8806, 76.3915, 89.9138, 103.451, 117.008, 130.587, 144.193, 157.83, 171.501, 185.211,
        198.964, 212.763, 226.614, 240.52, 254.485, 268.514, 282.612, 296.783, 311.032, 325.364, 339.784, 354.296};

    double cl_x_edges[] = new double[24];

    void initStaff() {
        for (int i = 0; i < 23; i++) {
            cl_x_edges[i] = 0.5 * (cl_x_coordinates_centers[i] + cl_x_coordinates_centers[i + 1]);
        }
        cl_x_edges[23] = 360.;
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

                    if (E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit
                            && E2_5 < noHit && E2_1 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E1_1 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit
                            && E2_1 + E2_2 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_4 < noHit && E1_5 < noHit && E2_1 < noHit && E2_4 < noHit && E2_5 < noHit
                            && (E2_2 + E2_3) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 3) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_5 < noHit && E2_1 < noHit && E2_2 < noHit && E2_5 < noHit
                            && (E2_3 + E2_4) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 4) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E2_1 < noHit && E2_2 < noHit && E2_3 < noHit
                            && (E2_4 + E2_5) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                }

            } else if (ilayer == 1) {

                if (ix == 0) {

                    if (E2_2 < noHit && E2_3 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit
                            && (E1_1 + E1_2) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E2_1 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_4 < noHit && E1_5 < noHit
                            && (E1_2 + E1_3 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit && E1_5 < noHit
                            && (E1_3 + E1_4 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 3) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit && E1_3 < noHit
                            && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 4) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit && E1_1 < noHit && E1_2 < noHit && E1_3 < noHit
                            && (E1_4 + E1_5 > Hit)) {
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

                    if (E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit
                            && E2_5 < noHit && E2_1 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E1_1 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit
                            && E2_1 + E2_2 > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_4 < noHit && E1_5 < noHit && E2_1 < noHit && E2_4 < noHit && E2_5 < noHit
                            && (E2_2 + E2_3) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 3) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_5 < noHit && E2_1 < noHit && E2_2 < noHit && E2_5 < noHit
                            && (E2_3 + E2_4) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 4) {

                    if (E1_1 < noHit && E1_2 < noHit && E1_3 < noHit && E1_4 < noHit && E2_1 < noHit && E2_2 < noHit && E2_3 < noHit
                            && (E2_4 + E2_5) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                }

            } else if (ilayer == 1) {

                if (ix == 0) {

                    if (E2_2 < noHit && E2_3 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_3 < noHit && E1_4 < noHit && E1_5 < noHit
                            && (E1_1 + E1_2) > Hit) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 1) {

                    if (E2_1 < noHit && E2_3 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_4 < noHit && E1_5 < noHit
                            && (E1_2 + E1_3 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }

                } else if (ix == 2) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_4 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit && E1_5 < noHit
                            && (E1_3 + E1_4 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 3) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_5 < noHit && E1_1 < noHit && E1_2 < noHit && E1_3 < noHit
                            && (E1_4 + E1_5 > Hit)) {
                        goodTileHit = true;
                        return goodTileHit;
                    }
                } else if (ix == 4) {

                    if (E2_1 < noHit && E2_2 < noHit && E2_3 < noHit && E2_4 < noHit && E1_1 < noHit && E1_2 < noHit && E1_3 < noHit
                            && (E1_4 + E1_5 > Hit)) {
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

        HodoTileIdentifier(int ax, int ay, int alayer) {
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
}
